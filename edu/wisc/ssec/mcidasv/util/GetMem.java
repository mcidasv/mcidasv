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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

/**
 * Wrapper for OperatingSystemMXBean
 */
public class GetMem {

    /**
     * Call a method belonging to {@link OperatingSystemMXBean} and return 
     * the result. 
     * 
     * @param <T> Type of the expected return and {@code defaultValue}.
     * @param methodName Name of the {@code OperatingSystemMXBean} method to call. Cannot be {@code null} or empty.
     * @param defaultValue Value to return if the call to {@code methodName} fails.
     * 
     * @return Either the value returned by the {@code methodName} call, or {@code defaultValue}.
     */
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
     * Get total system memory and print it out--accounts for 32bit JRE 
     * limitation of 1.5GB.
     * 
     * @return {@code String} representation of the total amount of system 
     * memory. 
     * 
     * @throws Exception
     */
    public static String getMemory() throws Exception {
        GetMem nonStaticInstance = new GetMem();
        Object totalMemoryObject = nonStaticInstance.callMXBeanMethod("getTotalPhysicalMemorySize", 0);
        long totalMemory = ((Number)totalMemoryObject).longValue();
        boolean is64 = (System.getProperty("os.arch").indexOf("64") >= 0);
        int megabytes = (int)(Math.round(totalMemory/1024/1024));
        if (!is64 && megabytes > 1536) {
            megabytes = 1536;
        }
        return String.valueOf(megabytes);
    }

    /**
     * The main. Get total system memory and print it out.
     * 
     * @param args Ignored.
     */
    public static void main(String[] args) {
        try {
            System.out.println(getMemory());
        } catch (Exception e) {
            System.err.println("Error getting total physical memory size");
            System.out.println("0");
        }
    }
}
