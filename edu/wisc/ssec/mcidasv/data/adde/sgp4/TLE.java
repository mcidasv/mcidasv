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
 * @author ganos
 */

public class TLE implements java.io.Serializable
{

	private static final long serialVersionUID = 1L;
	String line0 = ""; // name line
    String line1 = ""; // first line
    String line2 = ""; // second line
    
    /** Creates a new instance of TLE */
    public TLE()
    {
    }
    
    public TLE(String name, String l1, String l2)
    {
        line0 = name;
        line1 = l1;
        line2 = l2;
    }
    
    public String getSatName()
    {
        return line0;
    }
    
    public String getLine1()
    {
        return line1;
    }
    
    public String getLine2()
    {
        return line2;
    }
             
    
} // TLE
