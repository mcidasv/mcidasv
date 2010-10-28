/*
 * TLE.java
 *=====================================================================
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
 * Created on July 24, 2007, 3:02 PM
 *
 * Contains a Two Line Element (Norad) for a satellite
 */

package edu.wisc.ssec.mcidasv.data.adde.sgp4;

/**
 *
 * @author ganos
 */
public class TLE implements java.io.Serializable
{
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
