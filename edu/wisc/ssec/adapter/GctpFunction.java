/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

//
// GctpFunction.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2017 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

package edu.wisc.ssec.adapter;

import java.lang.Math;
//import org.apache.commons.math3.util.Math;

public class GctpFunction {

    static final int GEO = 0;
    static final int UTM = 1;
    static final int SPCS = 2;
    static final int ALBERS = 3;
    static final int LAMCC = 4;
    static final int MERCAT = 5;
    static final int PS = 6;
    static final int POLYC = 7;
    static final int EQUIDC = 8;
    static final int TM = 9;
    static final int STEREO = 10;
    static final int LAMAZ = 11;
    static final int AZMEQD = 12;
    static final int GNOMON = 13;
    static final int ORTHO = 14;
    static final int GVNSP = 15;
    static final int SNSOID = 16;
    static final int EQRECT = 17;
    static final int MILLER = 18;
    static final int VGRINT = 19;
    static final int HOM = 20;
    static final int ROBIN = 21;
    static final int SOM = 22;
    static final int ALASKA = 23;
    static final int GOOD = 24;
    static final int MOLL = 25;
    static final int IMOLL = 26;
    static final int HAMMER = 27;
    static final int WAGIV = 28;
    static final int WAGVII = 29;
    static final int OBEQA = 30;
    static final int USDEF = 99;

    static final int DATMCT = 20;

    static final double PI = 3.141592653589793238;
    static final double HALF_PI = PI * 0.5;
    static final double TWO_PI = PI * 2.0;
    static final double EPSLNI = 1.0e-10;
    static final double R2D = 57.2957795131;
    static final double D2R = 1.745329251994328e-2;
    static final double S2R = 4.848136811095359e-6;
    static final double EPSLN = 1.0e-10;

    static final int MAX_VAL = 4;
    static final long MAXLONG = 2147483647;
    static final double DBLLONG = 4.61168601e18;


    /*******************************************************************************
     NAME                           SPHDZ

     PURPOSE:        This function assigns values to the semimajor axis, semiminor
     axis, and radius of sphere.  If the datum code is negative,
     the first two values in the parameter array (parm) are used
     to define the values as follows:

     --If parm[0] is a non-zero value and parm[1] is greater than
     one, the semimajor axis and radius are set to parm[0] and
     the semiminor axis is set to parm[1].

     --If parm[0] is nonzero and parm[1] is greater than zero but
     less than or equal to one, the semimajor axis and radius
     are set to parm[0] and the semiminor axis is computed
     from the eccentricity squared value parm[1].  This
     algorithm is given below.

     --If parm[0] is nonzero and parm[1] is equal to zero, the
     semimajor axis, radius, and semiminor axis are set to
     parm[0].

     --If parm[0] equals zero and parm[1] is greater than zero,
     the default Clarke 1866 is used to assign values to the
     semimajor axis, radius and semiminor axis.

     --If parm[0] and parm[1] equals zero, the semimajor axis
     and radius are set to 6370997.0 (This value is represented
     as the last value in the datum code array) and the
     semiminor axis is set to zero.

     if a datum code is zero or greater, the semimajor and
     semiminor axis are defined by the datum code as found
     in Table A and the radius is set to 6370997.0.  If the
     datum code is greater than DATMCT the default datum,
     Clarke 1866, is used to define the semimajor
     and semiminor axis and radius is set to 6370997.0.

     The algorithm to define the semiminor axis using the
     eccentricity squared value is as follows:

     semiminor = sqrt(1.0 - ES) * semimajor   where
     ES = eccentricity squared

     Table A:
     SUPPORTED SPHEROIDS

     0: Clarke 1866 (default)        1: Clarke 1880
     2: Bessel                       3: International 1967
     4: International 1909           5: WGS 72
     6: Everest                      7: WGS 66
     8: GRS 1980                     9: Airy
     10: Modified Everest            11: Modified Airy
     12: WGS 84                      13: Southeast Asia
     14: Australian National         15: Krassovsky
     16: Hough                       17: Mercury 1960
     18: Modified Mercury 1968       19: Sphere of Radius
     6370997 meters

     PROGRAMMER              DATE
     ----------              ----
     T. Mittan             MARCH, 1993

     ALGORITHM REFERENCES

     1.  Snyder, John P., "Map Projections--A Working Manual", U.S. Geological
     Survey Professional Paper 1395 (Supersedes USGS Bulletin 1532), United
     State Government Printing Office, Washington D.C., 1987.

     2.  Snyder, John P. and Voxland, Philip M., "An Album of Map Projections",
     U.S. Geological Survey Professional Paper 1453 , United State Government
     Printing Office, Washington D.C., 1989.
     *******************************************************************************/

    static double major_data[] = {6378206.4, 6378249.145, 6377397.155, 6378157.5,
            6378388.0, 6378135.0, 6377276.3452, 6378145.0,
            6378137.0, 6377563.396, 6377304.063, 6377340.189,
            6378137.0, 6378155.0, 6378160.0, 6378245.0,
            6378270.0, 6378166.0, 6378150.0, 6370997.0};

    static double minor_data[] = {6356583.8, 6356514.86955, 6356078.96284, 6356772.2,
            6356911.94613, 6356750.519915, 6356075.4133,
            6356759.769356, 6356752.31414, 6356256.91,
            6356103.039, 6356034.448, 6356752.314245,
            6356773.3205, 6356774.719, 6356863.0188,
            6356794.343479, 6356784.283666, 6356768.337303,
            6370997.0};


/* Finds the correct ellipsoid axis
---------------------------------*/

    public static int sphdz(long isph,        // spheroid code number
                            double[] parm,    // projection parameters
                            double[] major,     // major axis
                            double[] minor,     // minor axis
                            double[] radius)  // radius
    {

        double r_radius;
        double r_major;
        double t_major;         // temporary major axis
        double r_minor;
        double t_minor;         // temporary minor axis
        long jsph;              // spheroid code number

        if (isph < 0) {
            t_major = Math.abs(parm[0]);
            t_minor = Math.abs(parm[1]);

            if (t_major > 0.0) {
                if (t_minor > 1.0) {
                    r_major = t_major;
                    r_minor = t_minor;
                    r_radius = t_major;
                } else if (t_minor > 0.0) {
                    r_major = t_major;
                    r_radius = t_major;
                    r_minor = (Math.sqrt(1.0 - t_minor)) * t_major;
                } else {
                    r_major = t_major;
                    r_radius = t_major;
                    r_minor = t_major;
                }
            } else if (t_minor > 0.0)   /* t_major = 0 */ {
                r_major = major_data[0];
                r_radius = major_data[0];
                r_minor = minor_data[0];
            } else {
                r_major = major_data[DATMCT - 1];
                r_radius = major_data[DATMCT - 1];
                r_minor = 6370997.0;
            }
        } else            /* isph >= 0 */ {
            jsph = Math.abs(isph);
            if (jsph > 19) {
                // p_error("Invalid spheroid selection","INFORMATIONAL");
                // p_error("Reset to 0","INFORMATIONAL");
                isph = 1;
                jsph = 0;
            }
            r_major = major_data[(int) jsph];
            r_minor = minor_data[(int) jsph];
            r_radius = major_data[DATMCT - 1];
        }

        major[0] = r_major;
        minor[0] = r_minor;
        radius[0] = r_radius;

        return (0);
    }

    /*******************************************************************************
     NAME                            PAKSZ

     PURPOSE:        This function converts a packed DMS angle to seconds.  The
     standard packed DMS format is:

     degrees * 1000000 + minutes * 1000 + seconds

     Example:        ang = 120025045.25 yields
     deg = 120
     min = 25
     sec = 45.25

     The algorithm used for the conversion is as follows:

     1.  The absolute value of the angle is used.

     2.  The degrees are separated out:
     deg = ang/1000000   (fractional portion truncated)

     3.  The minutes are separated out:
     min = (ang - deg * 1000000) / 1000     (fractional
     portion truncated)

     4.  The seconds are then computed:
     sec = ang - deg * 1000000 - min * 1000

     5.  The total angle in seconds is computed:
     sec = deg * 3600.0 + min * 60.0 + sec

     6.  The sign of sec is set to that of the input angle.


     PROGRAMMER              DATE
     ----------              ----
     T. Mittan             MARCH, 1993

     ALGORITHM REFERENCES

     1.  Snyder, John P., "Map Projections--A Working Manual", U.S. Geological
     Survey Proffesional Paper 1395 (Supersedes USGS Bulletin 1532), United
     State Government Printing Office, Washington D.C., 1987.

     2.  Snyder, John P. and Voxland, Philip M., "An Album of Map Projections",
     U.S. Geological Survey Professional Paper 1453 , United State Government
     Printing Office, Washington D.C., 1989.
     *******************************************************************************/

/* Convert DMS packed angle into deg
----------------------------------*/
    static int paksz(double ang,        // angle in DMS
                     double[] Ddeg        // fractional degrees
    ) {

        double fac;             /* sign flag                    */
        double deg;             /* degree variable              */
        double min;             /* minute variable              */
        double sec;             /* seconds variable             */
        double tmp;             /* temporary variable           */
        long i;                 /* temporary variable           */


        if (ang < 0.0)
            fac = -1;
        else
            fac = 1;

/* find degrees
-------------*/
        sec = Math.abs(ang);
        tmp = 1000000.0;
        i = (long) (sec / tmp);
        if (i > 360) {
            //p_error("Illegal DMS field","paksz-deg");
            return (-1);
        } else
            deg = i;

/* find minutes
-------------*/
        sec = sec - deg * tmp;
        tmp = 1000;
        i = (long) (sec / tmp);
        if (i > 60) {
            //p_error("Illegal DMS field","paksz-min");
            return (-1);
        } else
            min = i;

/* find seconds
-------------*/
        sec = sec - min * tmp;
        if (sec > 60) {
            //p_error("Illegal DMS field","paksz-sec");
            return (-1);
        } else
            sec = fac * (deg * 3600.0 + min * 60.0 + sec);
        deg = sec / 3600.0;

        Ddeg[0] = deg;
        return (0);
    }

    /* Function to return the sign of an argument
      ------------------------------------------*/
    public static int sign(double x
    ) {
        if (x < 0.0) {
            return (-1);
        } else {
            return (1);
        }
    }

/* Function to adjust a longitude angle to range from -180 to 180 radians
   added if statments
  -----------------------------------------------------------------------*/

    public static double[] adjust_lon(double[] x     // array of angles in radians
    ) {

        long temp;
        long count = 0;

        int length = x.length;

        for (int ii = 0; ii < length; ii++) {
            for (; ; ) {
                if (Math.abs(x[ii]) <= PI)
                    break;
                else if (((long) Math.abs(x[ii] / PI)) < 2)
                    x[ii] = x[ii] - (sign(x[ii]) * TWO_PI);
                else if (((long) Math.abs(x[ii] / TWO_PI)) < MAXLONG) {
                    x[ii] = x[ii] - (((long) (x[ii] / TWO_PI)) * TWO_PI);
                } else if (((long) Math.abs(x[ii] / (MAXLONG * TWO_PI))) < MAXLONG) {
                    x[ii] = x[ii] - (((long) (x[ii] / (MAXLONG * TWO_PI))) * (TWO_PI * MAXLONG));
                } else if (((long) Math.abs(x[ii] / (DBLLONG * TWO_PI))) < MAXLONG) {
                    x[ii] = x[ii] - (((long) (x[ii] / (DBLLONG * TWO_PI))) * (TWO_PI * DBLLONG));
                } else
                    x[ii] = x[ii] - (sign(x[ii]) * TWO_PI);
                count++;
                if (count > MAX_VAL)
                    break;
            }
        }

        return (x);
    }

    public static double adjust_lon(double x     //  angle in radians
    ) {

        long temp;
        long count = 0;

        for (; ; ) {
            if (Math.abs(x) <= PI)
                break;
            else if (((long) Math.abs(x / PI)) < 2)
                x = x - (sign(x) * TWO_PI);
            else if (((long) Math.abs(x / TWO_PI)) < MAXLONG) {
                x = x - (((long) (x / TWO_PI)) * TWO_PI);
            } else if (((long) Math.abs(x / (MAXLONG * TWO_PI))) < MAXLONG) {
                x = x - (((long) (x / (MAXLONG * TWO_PI))) * (TWO_PI * MAXLONG));
            } else if (((long) Math.abs(x / (DBLLONG * TWO_PI))) < MAXLONG) {
                x = x - (((long) (x / (DBLLONG * TWO_PI))) * (TWO_PI * DBLLONG));
            } else
                x = x - (sign(x) * TWO_PI);
            count++;
            if (count > MAX_VAL)
                break;
        }

        return (x);
    }

    public static void sincos(double[] val,
                              double[] sin_val,
                              double[] cos_val
    ) {

        int length = val.length;

        for (int ii = 0; ii < length; ii++) {
            sin_val[ii] = Math.sin(val[ii]);
            cos_val[ii] = Math.cos(val[ii]);
        }

        return;
    }

    public static void sincos(double val,
                              Double sin_val,
                              Double cos_val
    ) {


        double sin = Math.sin(val);
        double cos = Math.cos(val);

        sin_val = new Double(sin);
        cos_val = new Double(cos);

        return;
    }


    /* Function to eliminate roundoff errors in asin
    ----------------------------------------------*/
    public static double asinz(double con
    ) {
        //if (Math.abs(con) > 1.0)
   /*
 if (con > 1.0 || con < -1.0)
 {
   if (con > 1.0) {
     con = 1.0;
   }
   else {
     con = -1.0;
   }
 }
   */

        if (con > 1.0) {
            con = 1.0;
        } else if (con < -1.0) {
            con = -1.0;
        }

        con = Math.asin(con);

        return con;
    }

    /* Function to compute the constant e4 from the input of the eccentricity
       of the spheroid, x.  This constant is used in the Polar Stereographic
       projection.
    --------------------------------------------------------------------*/
    public static double e4fn(double x         //eccentricity
    ) {
        double con;
        double com;
        con = 1.0 + x;
        com = 1.0 - x;
        return (Math.sqrt((Math.pow(con, con)) * (Math.pow(com, com))));
    }

    /* Function to compute the constant small m which is the radius of
       a parallel of latitude, phi, divided by the semimajor axis.
    ---------------------------------------------------------------*/
    public static double msfnz(double eccent,
                               double sinphi,
                               double cosphi
    ) {
        double con;

        con = eccent * sinphi;
        return ((cosphi / (Math.sqrt(1.0 - con * con))));
    }

    /* Function to compute the constant small t for use in the forward
       computations in the Lambert Conformal Conic and the Polar
       Stereographic projections.
    --------------------------------------------------------------*/
    public static double tsfnz(double eccent,     // Eccentricity of the spheroid
                               double phi,        // Latitude phi
                               double sinphi      // Sine of the latitude
    ) {
        double con;
        double com;

        con = eccent * sinphi;
        com = .5 * eccent;
        con = Math.pow(((1.0 - con) / (1.0 + con)), com);
        return (Math.tan(.5 * (HALF_PI - phi)) / con);
    }

    /* Function to compute the latitude angle, phi2, for the inverse of the
       Lambert Conformal Conic and Polar Stereographic projections.
    ----------------------------------------------------------------*/
    public static double phi2z(double eccent,       // Spheroid eccentricity
                               double ts            // Constant value t
    ) {
        double eccnth;
        double phi;
        double con;
        double dphi;
        double sinpi;
        int i;
        eccnth = .5 * eccent;
        phi = HALF_PI - 2 * Math.atan(ts);
        for (i = 0; i <= 15; i++) {
            sinpi = Math.sin(phi);
            con = eccent * sinpi;
            dphi = HALF_PI - 2 * Math.atan(ts * (Math.pow(((1.0 - con) / (1.0 + con)), eccnth))) -
                    phi;
            phi += dphi;
            if (Math.abs(dphi) <= .0000000001)
                return (phi);
        }

        return Double.NaN;
    }

} /* end class */
