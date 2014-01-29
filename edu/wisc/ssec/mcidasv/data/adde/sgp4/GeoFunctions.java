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
package edu.wisc.ssec.mcidasv.data.adde.sgp4;

/**
 * Earth computation functions
 */
public class GeoFunctions
{

    // same as below except the function takes Julian Date
    /**
     * Compute Geodetic Latatude/Longitude/Altitude from Mean of Date position vector and Date 
     *
     * @param modPos Mean of date position vector
     * @param mjd modified julian date  (is this UTC or TT?) guessing UTC
     * @return vector of geodetic [latitude,longitude,altitude]
     */
    public static double[] GeodeticLLA(double[] modPos, double mjd)
    {
        // calculate days since Y2k
        // MJD = JD - 2400000.5
        // passed in MJD - 51544.5
        //double daysSinceY2k = (julDate - 2400000.5)-51544.5;
        double daysSinceY2k = mjd - 51544.5;

        return calculateGeodeticLLA(modPos, daysSinceY2k);
    } // GeodeticLLA

    // returned as Lat, Long, Alt
    // LLA = corrected for time (geographical coordinates)
    // for handling geodetic coordinates
    // r = TEME positions, d = days since Y2K
    private static double[] calculateGeodeticLLA(double[] r, double d)
    {
        double R_equ= AstroConst.R_Earth; // Equator radius [m]
        double f    = AstroConst.f_Earth; // Flattening

        double eps_mach = 2.22E-16; // machine precision (double?)

        final double  eps     = 1.0e3*eps_mach;   // Convergence criterion
        final double  epsRequ = eps*R_equ;
        final double  e2      = f*(2.0-f);        // Square of eccentricity

        final double  X = r[0];                   // Cartesian coordinates
        final double  Y = r[1];
        final double  Z = r[2];
        final double  rho2 = X*X + Y*Y;           // Square of distance from z-axis

        // original class vars
        // double lon;
        // double lat;
        //double h;
        double[] LLA = new double[3];


        // Check validity of input data
        if (MathUtils.norm(r)==0.0)
        {
            System.out.println(" invalid input in Geodetic constructor");
            LLA[1]=0.0;
            LLA[0]=0.0;
            LLA[2]=-AstroConst.R_Earth;
            return LLA;
        }

        // Iteration
        double  dZ, dZ_new, SinPhi;
        double  ZdZ, Nh, N;

        dZ = e2*Z;
        for(;;)
        {
            ZdZ    =  Z + dZ;
            Nh     =  Math.sqrt( rho2 + ZdZ*ZdZ );
            SinPhi =  ZdZ / Nh;                    // Sine of geodetic latitude
            N      =  R_equ / Math.sqrt(1.0-e2*SinPhi*SinPhi);
            dZ_new =  N*e2*SinPhi;
            if ( Math.abs(dZ-dZ_new) < epsRequ ) break;
            dZ = dZ_new;
        }

        // Longitude, latitude, altitude
        //double[] LLA = new double[3];
        LLA[1] = Math.atan2( Y, X );  // longitude,  lon
        LLA[0] = Math.atan2( ZdZ, Math.sqrt(rho2) ); // latitude, lat
        LLA[2] = Nh - N; // altitute, h

        //System.out.println("LLA[1]: "+ LLA[1]);
        //LLA[1] = LLA[1] -(280.4606 +360.9856473*d)*Math.PI/180.0; // shift based on time
        // add fidelity to the line above
        LLA[1] = LLA[1] - earthRotationDeg(d)*Math.PI/180.0; // shift based on time
        double div = Math.floor(LLA[1]/(2*Math.PI));
        LLA[1] = LLA[1] - div*2*Math.PI;
        if(LLA[1] > Math.PI)
        {
            LLA[1] = LLA[1]- 2.0*Math.PI;
        }

        //System.out.println("LLA[1]a: "+ LLA[1]);

        return LLA; //h

    } // calculateGeodeticLLA

    // SEG 10 June 2009 - help standardize earth Rotations
    private static double earthRotationDeg(double d) // days since y2K
    {
        // LLA[1] = LLA[1] -(280.4606 +360.9856473*d)*Math.PI/180.0; // shift based on time

        // calculate T
        double T = (d)/36525.0;

        // do calculation
        return ( (280.46061837 + 360.98564736629*(d)) + 0.000387933*T*T - T*T*T/38710000.0) % 360.0;

    } // earthRotationRad

     /**
     * calculate the pointing information Azumuth, Elevation, and Range (AER) to 
     * a satellite from a location on Earth (given Lat, Long, Alt)
     * if elevation >=0 then sat is above local horizon
      * @param currentJulianDate Julian Date for AER calculation (corresponds to ECI position)
      * @param lla_deg_m lat long and alt of station in deg/deg/meters (Geodetic)
      * @param eci_pos ECI position of object in meters (sat)
     * @return Azumuth [deg], Elevation [deg], and Range vector [m]
     */
    public static double[] calculate_AER(double currentJulianDate,double[] lla_deg_m, double[] eci_pos)
    {        double[] aer = new double[3];

        // 0th step get local mean Sidereal time
        // first get mean sidereal time for this station - since we use it twice
        double thetaDeg = Sidereal.Mean_Sidereal_Deg(currentJulianDate-AstroConst.JDminusMJD, lla_deg_m[1]);
        // first calculate ECI position of Station
        double[] eciGS = calculateECIposition(lla_deg_m,thetaDeg);

        // find the vector between pos and GS
        double[] rECI = MathUtils.sub(eci_pos, eciGS);

        // calculate range
        aer[2] = MathUtils.norm(rECI);

        // now transform ECI to topocentric-horizon system (SEZ)  (use Geodetic Lat, not geocentric)
        double[] rSEZ = eci2sez(rECI,thetaDeg,lla_deg_m[0]); // ECI vec, sidereal in Deg, latitude in deg

        // compute azimuth [radians] -> Deg
        //aer[0] = Math.atan(-rSEZ[1]/rSEZ[0]) * 180.0/Math.PI;
        aer[0] = Math.atan2(-rSEZ[0], rSEZ[1]) * 180.0/Math.PI;

        //System.out.println("aer[0]_0=" + aer[0] + ", rSEZ[-0,1]=" + (-rSEZ[0]) + ", " +rSEZ[1]);

        // do conversions so N=0, S=180, NW=270
        if(aer[0] <= 0)
        {
            aer[0] = Math.abs(aer[0]) + 90;
        }
        else
        {
            if(aer[0]<= 90)  //(between 0 and 90)
            {
                aer[0] = -1.0*aer[0] + 90.0;
            }
            else // between 90 and 180
            {
                aer[0] = -1.0*aer[0] + 450.0;
            }
        }

        // compute elevation [radians]
        aer[1] = Math.asin(rSEZ[2] / aer[2]) * 180.0/Math.PI;

        //System.out.println("SEZ: " + rSEZ[0] + ", " + rSEZ[1] + ", " + rSEZ[2]);

        return aer;
    } // calculate_AER

    /**
     * Calculate ECI position from local mean sidereal time and geodetic lat long alt
     * @param lla_deg_m lat long and alt of station in deg/deg/meters (Geodetic)
     * @param theta local mean sidereal time (Degrees)
     * @return ECI position (meters)
     */
    public static double[] calculateECIposition(double[] lla_deg_m, double theta)
    {
        // calculate the ECI j2k position vector of the ground station at the current time
        double [] eciVec = new double[3];

//        // calculate geocentric latitude - using non spherical earth (in radians)
//        // http://celestrak.com/columns/v02n03/
//        double  geocentricLat = Math.atan( Math.pow(1.0-AstroConst.f_Earth, 2.0) * Math.tan( lla_deg_m[0]*Math.PI/180.0 )  ); // (1-f)^2 tan(?).
//        
//        eciVec[2] = AstroConst.R_Earth * Math.sin( geocentricLat ); //lla_deg_m[0]*Math.PI/180.0 );
//        double r = AstroConst.R_Earth * Math.cos( geocentricLat ); //lla_deg_m[0]*Math.PI/180.0 );
//        eciVec[0] = r * Math.cos(theta*Math.PI/180.0);
//        eciVec[1] = r * Math.sin(theta*Math.PI/180.0);

        // alternate way to calcuate ECI position - using earth flattening
        // http://celestrak.com/columns/v02n03/
        double C = 1.0 / Math.sqrt( 1.0+AstroConst.f_Earth*(AstroConst.f_Earth-2.0)*Math.pow(Math.sin(lla_deg_m[0]*Math.PI/180.0 ),2.0) );
        double S = Math.pow(1.0-AstroConst.f_Earth, 2.0) * C;

        eciVec[0] = AstroConst.R_Earth * C * Math.cos(lla_deg_m[0]*Math.PI/180.0)*Math.cos(theta*Math.PI/180.0);
        eciVec[1] = AstroConst.R_Earth * C * Math.cos(lla_deg_m[0]*Math.PI/180.0)*Math.sin(theta*Math.PI/180.0);
        eciVec[2] = AstroConst.R_Earth * S * Math.sin(lla_deg_m[0]*Math.PI/180.0);

        return eciVec;

    } //calculateECIposition

    /**
     * transform ECI to topocentric-horizon system (SEZ) (south-East-Zenith)
     * @param rECI position in ECI coordinates (meters)
     * @param thetaDeg local sidereal time (degrees)
     * @param latDeg observer's latitude (degrees)
     * @return topocentric-horizon system (SEZ) (south-East-Zenith)
     */
    public static double[] eci2sez(double[] rECI,double thetaDeg,double latDeg)
    {
        double[] rSEZ = new double[3]; // new postion in SEZ coorinates

        //? (the local sidereal time) -> (thetaDeg*Math.PI)
        //? (the observer's latitude) - > (latDeg*Math.PI)
        rSEZ[0] = Math.sin(latDeg*Math.PI/180.0) * Math.cos(thetaDeg*Math.PI/180.0) * rECI[0] + Math.sin(latDeg*Math.PI/180.0) * Math.sin(thetaDeg*Math.PI/180.0) * rECI[1] - Math.cos(latDeg*Math.PI/180.0) * rECI[2];
        rSEZ[1] = -Math.sin(thetaDeg*Math.PI/180.0) * rECI[0] + Math.cos(thetaDeg*Math.PI/180.0) * rECI[1];        rSEZ[2] = Math.cos(latDeg*Math.PI/180.0) * Math.cos(thetaDeg*Math.PI/180.0) * rECI[0] + 
Math.cos(latDeg*Math.PI/180.0) * Math.sin(thetaDeg*Math.PI/180.0) * rECI[1] + Math.sin(latDeg*Math.PI/180.0) * rECI[2];

        return rSEZ;
    }
}
