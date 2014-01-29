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

public class Angle 
{

    public final double degrees;
    public final double radians;

    private final static double RADIANS_TO_DEGREES = 180d / Math.PI;

    /**
     * Obtains an angle from a specified number of radians.
     *
     * @param radians the size in radians of the angle to be obtained.
     *
     * @return a new angle, whose size in radians is given by <code>radians</code>.
     */
    public static Angle fromRadians(double radians)
    {
        return new Angle(RADIANS_TO_DEGREES * radians, radians);
    }

    private Angle(double degrees, double radians)
    {
        this.degrees = degrees;
        this.radians = radians;
    }
}
