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
 *
 * @author Shawn
 */
public class Sidereal 
{
    
    /**
     * Calculates the Greenwich mean sidereal time (GMST) on julDate (doesn't have to be 0h).
     * Used calculations from Meesus 2nd ed. 
     * @param mjd Modified Julian Date
     * @return Greenwich mean sidereal time in degrees (0-360)
     */
    public static double Greenwich_Mean_Sidereal_Deg(double mjd)
    {
        // calculate T
        double T = (mjd-51544.5)/36525.0;  
        
        // do calculation
        double gmst = ( (280.46061837 + 360.98564736629*(mjd-51544.5)) + 0.000387933*T*T - T*T*T/38710000.0) % 360.0;
        
        // make positive
        if(gmst < 0)
        {
            gmst += 360.0;
        }
        
        return gmst;
    } //Greenwich_Mean_Sidereal_Deg
    
    /**
     * Calculates the mean sidereal time (MST) on julDate (doesn't have to be 0h) for a given longitiude.
     * @param mjd Modified Julian Date
     * @param longitude longitude in degrees
     * @return mean sidereal time in degrees (0-360)
     */
    public static double Mean_Sidereal_Deg(double mjd, double longitudeDeg)
    {
        return (Greenwich_Mean_Sidereal_Deg(mjd) + longitudeDeg) % 360.0;
    } // Mean_Sidereal_Deg

    
    // main testing function
    public static void main(String[] args)
    {
        double mjd = 2449991.875 - AstroConst.JDminusMJD;
        double longitude = -75; // degrees
        double latitude = 40;
        
        double mst = Sidereal.Mean_Sidereal_Deg(mjd, longitude);
        
        System.out.println("For Phily MST: " + mst);
        
        // assuming a spherical Earth - calculate ECI vector
        double [] eciVec = new double[3];
        eciVec[2] = AstroConst.R_Earth * Math.sin( latitude*Math.PI/180.0 );
        double r = AstroConst.R_Earth * Math.cos( latitude*Math.PI/180.0 );
        eciVec[0] = r * Math.cos(mst*Math.PI/180.0);
        eciVec[1] = r * Math.sin(mst*Math.PI/180.0);
        
        System.out.println("ECI: " + eciVec[0] + ", " + eciVec[1] + ", " + eciVec[2]);
    }
    
}
