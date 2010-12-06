/**
 * =====================================================================
 * Copyright (C) 2009 Shawn E. Gano
 * 
 * This file is part of JSatTrak.
 * 
 * JSatTrak is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JSatTrak is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with JSatTrak.  If not, see <http://www.gnu.org/licenses/>.
 * =====================================================================
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
}
