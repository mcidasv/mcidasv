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

package edu.wisc.ssec.mcidasv.util;

/**
 * Simple wrapper around {@link GetVer} and {@link GetMem}. Used to reduce number
 * of Java invocations at McIDAS-V runtime.
 */
public class GetSystemInformation {

    /**
     * Determines the amount of system memory and the current McIDAS-V version and 
     * prints out the result. The string is formatted like so:
     * 
     * <pre>
     * MEMORYHERE McIDAS-V version VERSIONHERE built BUILDDATEHERE
     * </pre>
     * 
     * @param args Ignored.
     */
    public static void main(String[] args) {
        String systemMemory = "0";
        String mcvVersion = "unknown";
        try {
            systemMemory = GetMem.getMemory();
            mcvVersion = GetVer.getVersion();
        } catch (Exception e) {
            System.err.println("error assembling system information: "+e);
        } finally {
            System.out.println(systemMemory + " "+mcvVersion);
        }
    }
}
