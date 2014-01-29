/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package ucar.unidata.geoloc.projection.sat;

import java.io.*;
import java.lang.Math;
import java.lang.String;
import java.lang.*;
import java.util.*;


public class FGFNavigation {

   static int n_elems;
   static int n_lines;
   double DEG_TO_RAD = Math.PI/180.0;
   double RAD_TO_DEG = 180.0/Math.PI;

   // quadrant extents
   double elemHalfAngle = 8.88*DEG_TO_RAD;          // radians
   double lineHalfAngle = 8.88*DEG_TO_RAD;          // radians
//   double elemHalfAngle = 1.5*DEG_TO_RAD;          // radians -- for testing
//   double lineHalfAngle = 1.5*DEG_TO_RAD;          // radians -- for testing

  
   double lamdaStart = -8.88*DEG_TO_RAD;
   double thetaStart = 8.88*DEG_TO_RAD;
   double lamdaEnd = 8.88*DEG_TO_RAD;
   double thetaEnd = -8.88*DEG_TO_RAD;

   double lamdaWidth;
   double thetaHeight;


   double r_pol = 6356.7523;
   double r_eq  = 6378.1380;
   double f = 1.0/298.257222;
   double fp = 1.0/((1.0-f)*(1.0-f));
   double geo_alt = 35786.000;
   double h = geo_alt + r_eq;
   double d = h*h - r_eq*r_eq;

//   double sub_lon = -75*DEG_TO_RAD;
   double sub_lon;

   // increment resolution = 28E-6 * spatial resolution of satellite
   // offset resolution = increment resolution / 2.0
   static double inc_res = 28.00000E-06;

// double inc_2km = 56.00000E-06;
// double inc_1km = 28.00000E-06;
// double inc_hkm = 14.00000E-06;

// double off_2km = 28.00000E-06;
// double off_1km = 14.00000E-06;
// double off_hkm =  7.00000E-06;

   static double delLamda;
   static double delTheta;
   static double offset;

   double lamdaOffset;
   double thetaOffset;

   static double sat_chan_resKM;
   static double sub_lon_degrees;

   static double latlon[][][];

   static double MISSING = -999.0;

   public FGFNavigation() {
     this(-75.0, 56E-06, 56E-06, 2.0);
   }

   public FGFNavigation(double subLonDegrees, double delLamdaRadians) {
     this.sub_lon_degrees = subLonDegrees;
     this.sub_lon = sub_lon_degrees*DEG_TO_RAD;
     this.delLamda = delLamdaRadians;
     this.delTheta = delLamda;

     init();
   }

   public FGFNavigation(double subLonDegrees, double delLamdaRadians, double delThetaRadians, double subPointRes) {
     this.sub_lon_degrees = subLonDegrees;
     this.sub_lon  = sub_lon_degrees*DEG_TO_RAD;
     this.delLamda = delLamdaRadians;
     this.delTheta = delThetaRadians;
     this.sat_chan_resKM = subPointRes;

     init();
   }

   public FGFNavigation(String SatelliteSeriesString, int SatelliteSeriesNumber, int SatelliteChannel) {

     int SatelliteSeriesID = getSatelliteSeriesNumber(SatelliteSeriesString);
         System.out.println("SatID: "+SatelliteSeriesID);

     sub_lon_degrees = getSatelliteSubPoint(SatelliteSeriesID,SatelliteSeriesNumber);
     sub_lon = sub_lon_degrees*DEG_TO_RAD; 
         System.out.println("sub_lon_degrees: "+sub_lon_degrees+"  sub_lon:"+sub_lon);

     sat_chan_resKM = getSatelliteChannelResolution(SatelliteSeriesID,SatelliteSeriesNumber,SatelliteChannel);
         System.out.println("sat_chan_resKM: "+sat_chan_resKM);

     delLamda = getGridRadiansFromRes(sat_chan_resKM);
     delTheta = getGridRadiansFromRes(sat_chan_resKM);

     init();
  }

  private void init() {

     offset = delLamda*0.5;

     n_elems = (int) (lamdaStart/delLamda);
     n_lines = (int) (thetaStart/delTheta);
     lamdaStart = n_elems*delLamda;
     thetaStart = n_lines*delTheta;

     n_elems = (int) (lamdaEnd/delLamda);
     n_lines = (int) (thetaEnd/delTheta);
     lamdaEnd = n_elems*delLamda;
     thetaEnd = n_lines*delTheta;

     lamdaWidth  = lamdaEnd - lamdaStart;
     thetaHeight = thetaEnd - thetaStart;

     // use an integral number of steps
     n_elems = (int) ((lamdaEnd - lamdaStart)/delLamda);
     n_lines = (int) ((thetaEnd - thetaStart)/delTheta);

     lamdaOffset = offset;
     thetaOffset = offset;

     if (n_elems < 0) {
       n_elems     = -n_elems;
       delLamda    = -delLamda;
       lamdaOffset = -offset;
     }

     if (n_lines < 0) {
       n_lines     = -n_lines;
       delTheta    = -delTheta;
       thetaOffset = -offset;
     }
  }


  public static double getGridRadiansFromRes(double satHorizResolution) {
    return (satHorizResolution*inc_res);
  }

  public static int getSatelliteSeriesNumber(String satSeries) {
    int SatNumber;

    if ( satSeries.equals("GOES") ) {
      SatNumber=0;     
    } else if ( satSeries.equals("MTSAT") || satSeries.equals("Himawari") ) {
      SatNumber=1;     
    } else if ( satSeries.equals("Meteosat") ) {
      SatNumber=2;     
    } else if ( satSeries.equals("MSG") ) {
      SatNumber=3;     
    } else if ( satSeries.equals("FY-2") ) {
      SatNumber=4;     
    } else if ( satSeries.equals("Kalpana") ) {
      SatNumber=5;     
    } else if ( satSeries.equals("INSAT") ) {
      SatNumber=6;     
    } else {
      SatNumber=99;    // throw exception???
    }
   
    return SatNumber;
  }

  public static double getSatelliteSubPoint(int satSeriesNum, int satNumber) { 

  double subpoint=-99.0;

  switch(satSeriesNum) {
    case 0: 
     // GOES
     if(satNumber<10) {
       // non-operational GOES satellites
       subpoint=MISSING;
     } else if (satNumber==10) {         // GOES-10
       subpoint=-60.0;
     } else if (satNumber==11) {         // GOES-11
       subpoint=-135.0;
     } else if (satNumber==12) {         // GOES-12
       subpoint=-75.0;
     } else if (satNumber==13) {         // GOES-13
       subpoint=-105.0;
     } else if (satNumber==14) {         // GOES-14
       subpoint=-89.5;
     } else if (satNumber==15) {         // GOES-15
       subpoint=-105.0;
     } else if (satNumber==16) {         // GOES-R
       subpoint=-75.0;
     } else if (satNumber==17) {         // GOES-S
       subpoint=-135.0;
     } else {
       // not a valid input value
       subpoint=-888.;
     }
     break;
    case 1: 
     // MTSAT/Himawari
     if(satNumber<1) {
       // non-operational Himawari satellites
       subpoint=MISSING;
     } else if (satNumber==1) {          // Himawari-6/MTSAT-1R
       subpoint=140.0;
     } else if (satNumber==2) {          // Himawari-7/MTSAT-2
       subpoint=145.0;
     } else if (satNumber==6) {          // Himawari-6/MTSAT-1R
       subpoint=140.0;
     } else if (satNumber==7) {          // Himawari-7/MTSAT-2
       subpoint=145.0;
     } else {
       // not a valid input value
       subpoint=-888.;
     }
     break;
    case 2: 
     // Meteosat
     if(satNumber<6) {
       // non-operational Meteosat satellites
       subpoint=MISSING;
     } else if (satNumber==6) {          // Meteosat-6
       subpoint=-67.5;
     } else if (satNumber==7) {          // Meteosat-7
       subpoint=-57.5;
     } else if (satNumber==8) {          // Meteosat-8/MSG
       // Meteosat-8 is MSG
       subpoint=9.5;
     } else if (satNumber==9) {          // Meteosat-9/MSG
       // Meteosat-8 is MSG
       subpoint=0.0;
     } else {
       // not a valid input value
       subpoint=-888.;
     }
     break;
    case 3: 
     // MSG
     if(satNumber<8) {
       // non-operational Meteosat satellites
       subpoint=MISSING;
     } else if (satNumber==8) {          // Meteosat-8/MSG
       subpoint=9.5;
     } else if (satNumber==9) {          // Meteosat-9/MSG
       subpoint=0.0;
     } else {
       // not a valid input value
       subpoint=-888.;
     }
     break;
    case 4: 
     // FY-2
     if(satNumber<3) {
       // non-operational FY-2 satellites
       subpoint=MISSING;
     } else if (satNumber==3) {          // FY-2C
       subpoint=123.5;
     } else if (satNumber==4) {          // FY-2D
       subpoint=86.5;
     } else if (satNumber==5) {          // FY-2E
       subpoint=105.0;
     } else {
       // not a valid input value
       subpoint=-888.;
     }
     break;
    case 5: 
     // Kalpana
     if(satNumber<1) {
       subpoint=MISSING;
     } else if (satNumber==1) {          // Kalpana-1
       subpoint=123.5;
     } else {
       // not a valid input value
       subpoint=-888.;
     }
     break;
    case 6: 
     // INSAT-3
     if(satNumber<1) {
       subpoint=MISSING;
     } else if (satNumber==1) {          // INSAT-3A
       subpoint=93.5;
     } else {
       // not a valid input value
       subpoint=-888.;
     }
     break;
    default:
     // unknown satellite
     // throw exception?   print error
     break;
    }

    return subpoint;

  }

  public static double getSatelliteChannelResolution(int satSeriesNum, int satNumber, int satChannel) {
  double satRes=-88.0;

  switch(satSeriesNum) {
    case 0:
     // GOES
     if(satNumber<8) {
       // non-operational GOES satellites
       satRes=MISSING;
     } else if (satNumber<=11) {         // GOES-8 to 11
       satRes=4.0;                         // IR channels
       if(satChannel==1) satRes=1.0;       // Visible channel
       if(satChannel==3) satRes=8.0;       // WV channel
     } else if (satNumber<=13) {         // GOES-12 to 15
       satRes=4.0;                         // IR and WV channels
       if(satChannel==1) satRes=1.0;       // Visible channel
       if(satChannel==5) satRes=8.0;       // CO2 channel
     } else if (satNumber<=15) {         // GOES-12 to 15
       satRes=4.0;                         // IR and WV channels
       if(satChannel==1) satRes=1.0;       // Visible channel
     } else if (satNumber<=17) {         // GOES-R and S
       satRes=2.0;                         // IR and WV channels
       if(satChannel<=2) satRes=0.5;       // Visible channel
     } else if ((satNumber>=21)&&
                (satNumber<=39)) {       // GOES Sounder
       satRes=10.0;                        // All channels
     } else {
       // not a valid input value
       satRes=-888.;
     }
     break;
    case 1: 
     // MTSAT/Himawari
     satRes=4.0;                           // IR and WV channels
     if(satChannel==1) satRes=1.0;         // Visible channel
     break;
    case 2: 
     // Meteosat
     if(satNumber<6) {
       // non-operational Meteosat satellites
       satRes=MISSING;
     } else if (satNumber<=7) {          // Meteosat-6 and 7
       satRes=5.0;                         // IR and WV channels
       if(satChannel==1) satRes=2.5;       // Visible channel
     } else if (satNumber<=9) {          // Meteosat-8 and 9
       satRes=3.0;                         // IR and WV channels
       if(satChannel==12) satRes=1.0;      // Visible channel
     } else {
       // not a valid input value
       satRes=-888.;
     }
     break;
    case 3: 
     // MSG
     if(satNumber<8) {
       // non-operational Meteosat satellites
       satRes=MISSING;
     } else if (satNumber<=9) {          // Meteosat-8 and 9
       satRes=3.0;                         // IR and WV channels
       if(satChannel==12) satRes=1.0;      // Visible channel
     } else {
       // not a valid input value
       satRes=-888.;
     }
     break;
    case 4: 
     // FY-2                             // FY-2A to 2D
     satRes=5.0;                           // IR and WV channels
     if(satChannel==1) satRes=1.25;        // Visible channel
     break;
    case 5: 
     // Kalpana                          // Kalpana-1
     satRes=8.0;                           // IR and WV channels
     if(satChannel==1) satRes=2.0;         // Visible channel
     break;
    case 6: 
     // INSAT-3 
     if(satNumber<4) {                   // INSAT 3A to 3C
       satRes=8.0;                         // IR and WV channels
       if(satChannel==1) satRes=2.0;       // Visible
       if((satChannel>=5)&&
          (satChannel<=7)) satRes=1.0;     // Visible, near IR, SWIR CCD camera (bands are a guess)
     } else if ((satNumber>=21)&&
                (satNumber<=39)) {       // INSAT Sounder
       satRes=10.0;                        // All channels
     } else {                            // INSAT 3D
       satRes=4.0;                         // IR channels
       if(satChannel<=2) satRes=1.0;       // Visible and SWIR
       if(satChannel==3) satRes=8.0;       // WV
     }
     break;
    default:
     // unknown satellite
     // throw exception?   print error
     break;
    }

    return satRes;

  }


  public void makeEarthLocations(double[] longitude, double[] latitude) {
    for (int j=0; j<n_lines; j++) {
      for (int i=0; i<n_elems; i++) {
        double[] lonlat = elemLineToEarth(i, j);
        int k = j*n_elems + i;
        longitude[k] = lonlat[0];
        latitude[k] = lonlat[1];
      }
    }
  }

  public int[] earthToElemLine(double longitude, double latitude) {
    double[] satCoords = earthToSat(longitude, latitude);
    return satToElemLine(satCoords[0], satCoords[1]);
  }

  public double[] elemLineToEarth(int elem, int line) {
    double[] satCoords = elemLineToSat(elem, line);
    return satToEarth(satCoords[0]+lamdaOffset, satCoords[1]+thetaOffset);
  }

  public double[] elemLineToSat(int elem, int line) {
    double lamda = elem*delLamda + lamdaStart;
    double theta = line*delTheta + thetaStart;

    return new double[] {lamda, theta};
  }

  public int[] satToElemLine(double lamda, double theta) {
    int elem = (int) ((lamda - lamdaStart)/delLamda);
    int line = (int) ((theta - thetaStart)/delTheta);

    return new int[] {elem, line};
  }

  public double[] earthToSat(double geographic_lon, double geographic_lat) {

     geographic_lat = geographic_lat*DEG_TO_RAD;
     geographic_lon = geographic_lon*DEG_TO_RAD;

     double geocentric_lat = Math.atan(((r_pol*r_pol)/(r_eq*r_eq))*Math.tan(geographic_lat));

     double r_earth = r_pol/Math.sqrt(1 -((r_eq*r_eq - r_pol*r_pol)/(r_eq*r_eq))*Math.cos(geocentric_lat)*Math.cos(geocentric_lat));

     double r_1 = h - r_earth*Math.cos(geocentric_lat)*Math.cos(geographic_lon - sub_lon);
     double r_2 = -r_earth*Math.cos(geocentric_lat)*Math.sin(geographic_lon - sub_lon);
     double r_3 = r_earth*Math.sin(geocentric_lat);

     if (r_1 > h) {
       return new double[] {Double.NaN, Double.NaN};
     }

     /** reverse sign of argument from that in cgms to account for
         opposite lamda rotation in the earth and satellite frames.
      */
     double lamda_sat = Math.atan(-r_2/r_1);

     double theta_sat = Math.asin(r_3/Math.sqrt(r_1*r_1 + r_2*r_2 + r_3*r_3));

     return new double[] {lamda_sat, theta_sat};
  }


  //- x is lamda, y is theta from the cgms document, output Longitude, Latitude
  public double[] satToEarth(double x, double y) {

     double c1=(h*Math.cos(x)*Math.cos(y))*(h*Math.cos(x)*Math.cos(y));
     double c2=(Math.cos(y)*Math.cos(y)+fp*Math.sin(y)*Math.sin(y))*d;
     if(c1<c2) {
       return new double[] {Double.NaN, Double.NaN};
     }
     double s_d = Math.sqrt(c1 - c2);
//     double s_d = Math.sqrt((h*Math.cos(x)*Math.cos(y))*(h*Math.cos(x)*Math.cos(y)) -
//                 (Math.cos(y)*Math.cos(y)+fp*Math.sin(y)*Math.sin(y))*d);

     double s_n = (h*Math.cos(x)*Math.cos(y) - s_d)/(Math.cos(y)*Math.cos(y) + fp*Math.sin(y)*Math.sin(y));

     double s_1 = h - s_n*Math.cos(x)*Math.cos(y);
     double s_2 = s_n*Math.sin(x)*Math.cos(y);
     double s_3 = -s_n*Math.sin(y);

     double s_xy = Math.sqrt(s_1*s_1 + s_2*s_2);
     double geographic_lon = Math.atan(s_2/s_1) + sub_lon;

     /** reverse sign of argument from that in cgms to account for
         opposite theta rotation in the earth and satellite frames.
      */
     double geographic_lat = Math.atan(-fp*(s_3/s_xy));

     return new double[] {RAD_TO_DEG*geographic_lon, RAD_TO_DEG*geographic_lat};
  }

}
