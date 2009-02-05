/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

// Wrapper for OperatingSystemMXBean
public class GetMem {

    private <T> T callMXBeanMethod(final String methodName, final T defaultValue) {
        assert methodName != null : "Cannot invoke a null method name";
        assert methodName.length() > 0: "Cannot invoke an empty method name";
        OperatingSystemMXBean osBean = 
            ManagementFactory.getOperatingSystemMXBean();
        T result = defaultValue;
        try {
            Method m = osBean.getClass().getMethod(methodName);
            m.setAccessible(true);
            // don't suppress warnings because we cannot guarantee that this
            // cast is correct.
            result = (T)m.invoke(osBean);
        } catch (Exception e) {
        	System.err.println("Error invoking OperatingSystemMXBean method: " + methodName);
            // do nothing for right now
        }
        return result;
    }
	
    /**
     * The main. Get total system memory and print it out.
     * Account for 32bit JRE limitation of 1.5GB.
     * Print value in megabytes.
     */
    public static void main(String[] args) throws Exception {
    	GetMem nonStaticInstance = new GetMem();

    	try {
	        Object totalMemoryObject = nonStaticInstance.callMXBeanMethod("getTotalPhysicalMemorySize", 0);
	        long totalMemory = ((Number)totalMemoryObject).longValue();
    		boolean is64 = (System.getProperty("os.arch").indexOf("64") >= 0);
    		int megabytes = (int)(Math.round(totalMemory/1024/1024));
    		if (!is64 && megabytes > 1536) megabytes=1536;
	        String memoryString = String.valueOf(megabytes);
	        System.out.println(memoryString);
    	}
    	catch (Exception e) {
    		System.err.println("Error getting total physical memory size");
    		System.out.println("0");
    	}
    }
}
