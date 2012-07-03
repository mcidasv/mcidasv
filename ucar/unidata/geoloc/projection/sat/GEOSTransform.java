package ucar.unidata.geoloc.projection.sat;

import java.io.*;
import java.lang.Math;
import java.lang.String;
import java.lang.*;
import java.util.*;


public class GEOSTransform {

   double DEG_TO_RAD = Math.PI/180.0;
   double RAD_TO_DEG = 180.0/Math.PI;

   //-  GRS80 parameters (GOES-R) default, can be changed via the ctrs ---------
   double r_pol = 6356.7523;      // semi-minor axis (polar radius km)
   double r_eq  = 6378.1370;      // semi-major axis (equatorial radius km)
   double f = 1.0/298.257222101;  // flattening
   double invf = 1.0/f;
   double fp = 1.0/((1.0-f)*(1.0-f));

   final double h_msg = 42164.0;
   final double h_goesr = 42164.16;
   double h = h_goesr; // Geostationary Orbit Radius (spacecraft to barycenter distance) (km)
   double d;

   double sub_lon;
   double sub_lon_degrees;

   public static final String GOES = "GOES";
   public static final String GEOS = "GEOS";
   public static final String WGS84 = "WGS84";
   public static final String GRS80 = "GRS80";

   public final Geoid wgs84 = new GeoidWGS84();
   public final Geoid grs80 = new GeoidGRS80();

   public String scan_geom = GEOS;

   double scale_x;
   double scale_y;
   double add_offset_x;
   double add_offset_y;

   public GEOSTransform() {
     this(0.0);
   }

   public GEOSTransform(double subLonDegrees) {
     this(subLonDegrees, GEOS);
   }

   public GEOSTransform(double subLonDegrees, String scan_geom) {
     this(subLonDegrees, scan_geom, null);
   }

   public GEOSTransform(double subLonDegrees, String scan_geom, String geoidID)
   {
     Geoid geoid = null;

     if (geoidID == null) {
        if (scan_geom.equals(GEOS)) {
           geoid = wgs84;
        }
        else if (scan_geom.equals(GOES)) {
           geoid = grs80;
        }
     } else if (geoidID.equals(WGS84)) {
        geoid = wgs84;
     } else if (geoidID.equals(GRS80)) {
        geoid = grs80;
     }

     init(subLonDegrees, scan_geom, geoid);
   }

   private void init(double subLonDegrees, String scan_geom, Geoid geoid) {
     this.sub_lon_degrees = subLonDegrees;
     this.sub_lon = sub_lon_degrees*DEG_TO_RAD;
     this.scan_geom = scan_geom;

     r_pol = geoid.r_pol;
     r_eq  = geoid.r_eq;
     f     = geoid.f;
     invf  = geoid.invf;
     fp    = 1.0/((1.0-f)*(1.0-f));

     if (scan_geom.equals(GEOS)) {
       h = h_msg;
     }
     else if (scan_geom.equals(GOES)) {
       h = h_goesr;
     }

     d = h*h - r_eq*r_eq;
   }


   //-- input (Longitude, Latitude) -> output (lamda, theta), ie. (x,y) or (E-W, N-S)
   public double[] earthToSat(double geographic_lon, double geographic_lat) {

     geographic_lat = geographic_lat*DEG_TO_RAD;
     geographic_lon = geographic_lon*DEG_TO_RAD;

     double geocentric_lat = Math.atan(((r_pol*r_pol)/(r_eq*r_eq))*Math.tan(geographic_lat));

     double r_earth = r_pol/Math.sqrt(1.0 -((r_eq*r_eq - r_pol*r_pol)/(r_eq*r_eq))*Math.cos(geocentric_lat)*Math.cos(geocentric_lat));

     double r_1 = h - r_earth*Math.cos(geocentric_lat)*Math.cos(geographic_lon - sub_lon);
     double r_2 = -r_earth*Math.cos(geocentric_lat)*Math.sin(geographic_lon - sub_lon);
     double r_3 = r_earth*Math.sin(geocentric_lat);

     if (r_1 > h) { // often two geoid intersect points, use the closer one.
       return new double[] {Double.NaN, Double.NaN};
     }

     double lamda_sat = Double.NaN;
     double theta_sat = Double.NaN;

     if (scan_geom.equals(GEOS)) { // GEOS (eg. SEVIRI, MSG)  CGMS 03, 4.4.3.2, Normalized Geostationary Projection
        lamda_sat = Math.atan(-r_2/r_1);
        theta_sat = Math.asin(r_3/Math.sqrt(r_1*r_1 + r_2*r_2 + r_3*r_3));
     }
     else if (scan_geom.equals(GOES)) { // GOES (eg. GOES-R ABI) 
        lamda_sat = Math.asin(-r_2/Math.sqrt(r_1*r_1 + r_2*r_2 + r_3*r_3));
        theta_sat = Math.atan(r_3/r_1);
     }

     return new double[] {lamda_sat, theta_sat};
   }


   //- x is lamda (E-W), y is theta (N-S).  Output (Longitude, Latitude)
   public double[] satToEarth(double x, double y) {

     if (scan_geom.equals(GOES)) { // convert from GOES to GEOS for transfrom below
        double[] lambda_theta_geos = new double[2];
        lambda_theta_geos = GOES_to_GEOS(x, y);
        x = lambda_theta_geos[0];
        y = lambda_theta_geos[1];
     }

     double c1=(h*Math.cos(x)*Math.cos(y))*(h*Math.cos(x)*Math.cos(y));
     double c2=(Math.cos(y)*Math.cos(y)+fp*Math.sin(y)*Math.sin(y))*d;

     if (c1<c2) {
       return new double[] {Double.NaN, Double.NaN};
     }

     double s_d = Math.sqrt(c1 - c2);

     double s_n = (h*Math.cos(x)*Math.cos(y) - s_d)/(Math.cos(y)*Math.cos(y) + fp*Math.sin(y)*Math.sin(y));

     double s_1 = h - s_n*Math.cos(x)*Math.cos(y);
     double s_2 = s_n*Math.sin(x)*Math.cos(y);
     double s_3 = -s_n*Math.sin(y);

     double s_xy = Math.sqrt(s_1*s_1 + s_2*s_2);
     double geographic_lon = Math.atan(s_2/s_1) + sub_lon;

     double geographic_lat = Math.atan(-fp*(s_3/s_xy));

     double lonDegrees = RAD_TO_DEG*geographic_lon;
     double latDegrees = RAD_TO_DEG*geographic_lat;
     
     // force output longitude to -180 to 180 range
     if (lonDegrees < -180.0) lonDegrees += 360.0;
     if (lonDegrees > 180.0) lonDegrees -= 360.0;

     return new double[] {lonDegrees, latDegrees};
   }

   public double[] GOES_to_GEOS(double lambda_goes, double theta_goes) {
     double theta_geos = Math.asin( Math.sin(theta_goes)*Math.cos(lambda_goes) );
     double lambda_geos = Math.atan( Math.tan(lambda_goes)/Math.cos(theta_goes) );

     return new double[] {lambda_geos, theta_geos};
   }


  /**
   *  Transform fractional FGF coordinates to (longitude, latitude).
   */
   public double[] FGFtoEarth(double fgf_x, double fgf_y, double scale_x, double offset_x, double scale_y, double offset_y) {
      double[] xy = FGFtoSat(fgf_x, fgf_y, scale_x, offset_x, scale_y, offset_y);
      return satToEarth(xy[0], xy[1]);
   }

  /**
   *  Transform fractional FGF coordinates to (lamda, theta) radians.
   */
   public double[] FGFtoSat(double fgf_x, double fgf_y, double scale_x, double offset_x, double scale_y, double offset_y) {
      double x = fgf_x*scale_x + offset_x;
      double y = fgf_y*scale_y + offset_y;

      return new double[] {x,y};
   }

  /**
   *  Transform integer FGF coordinates to (longitude, latitude).
   */
   public double[] ElemLineToEarth(int elem, int line, double scale_x, double offset_x, double scale_y, double offset_y) {
      return FGFtoEarth((double) elem, (double) line, scale_x, offset_x, scale_y, offset_y);
   }
  
  /**
   *  Transform Earth coordinates (lon,lat) to fractional FGF coordinates.
   */
   public double[] EarthToFGF(double geographic_lon, double geographic_lat, double scale_x, double offset_x, double scale_y, double offset_y) {
     double[] xy = earthToSat(geographic_lon, geographic_lat);
     double[] fgf = SatToFGF(xy[0], xy[1], scale_x, offset_x, scale_y, offset_y);
     return fgf;
   }

  /**
   *  Transform Earth coordinates (lon,lat) to integer FGF coordinates.
   */
   public int[] EarthToElemLine(double geographic_lon, double geographic_lat, double scale_x, double offset_x, double scale_y, double offset_y) {
      double[] fgf = EarthToFGF(geographic_lon, geographic_lat, scale_x, offset_x, scale_y, offset_y);
      int elem = (int) Math.floor(fgf[0] + 0.5);
      int line = (int) Math.floor(fgf[1] + 0.5);
      return new int[] {elem, line};
   }

  /**
   *  Transform (lamda, theta) in radians to fractional FGF coordinates.
   */
   public double[] SatToFGF(double lamda, double theta, double scale_x, double offset_x, double scale_y, double offset_y) {
     double fgf_x = (lamda - offset_x)/scale_x;
     double fgf_y = (theta - offset_y)/scale_y;
     return new double[] {fgf_x, fgf_y};
   }



  /**  Earth Geoid definitions
   *   Note:  CGMS Doc No CGMS 03, Issue 2.6 states the following geoid parameters:
   *   r_pol = 6356.5838 km
   *   r_eq  = 6378.1690 km
   */

   class Geoid {
       double r_pol;  // semi-minor axis (polar radius km)
       double r_eq;   // semi-major axis (equatorial radius km)
       double f;      // flattening
       double invf;   // inverse flattening
       String id;

       public Geoid() {
       }

       public Geoid(double r_pol, double r_eq, double f) {
          this.r_pol = r_pol;
          this.r_eq = r_eq;
          this.f = f;
          this.invf = 1.0/f;
       }
   }

   class GeoidWGS84 extends Geoid {
       //-  WGS84 parameters  ------------------------------------------
       public GeoidWGS84() {
          r_pol = 6356.7523;  // kilometers
          r_eq  = 6378.1370;
          f = 1.0/298.257223563;
          invf = 1.0/f;
          id = WGS84;
       }
   }

   class GeoidGRS80 extends Geoid {
       //-  GRS80 parameters (GOES-R) --------------------------------------
       public GeoidGRS80() {
          r_pol = 6356.7523;  // kilometers
          r_eq  = 6378.1370;
          invf = 298.257222101;
          f = 1.0/298.257222101;
          id = GRS80;
       }
   }
}
