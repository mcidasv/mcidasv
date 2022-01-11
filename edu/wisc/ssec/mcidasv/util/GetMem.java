/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2022
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

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Wrapper for OperatingSystemMXBean.
 *
 * <p>Note: this class is likely to be used in contexts where we don't
 * have logging set up, so {@link System#err} and {@link System#out} are used.
 */
public class GetMem {

    /**
     * Query an {@link OperatingSystemMXBean} attribute and return the result.
     * 
     * @param <T> Type of the expected return value and {@code defaultValue}.
     * @param attrName Name of the {@code OperatingSystemMXBean} attribute to 
     *                 query. Cannot be {@code null} or empty.
     * @param defaultValue Value returned if {@code attrName} could not be 
     *                     queried.
     * 
     * @return Either the value corresponding to {@code attrName} or 
     *         {@code defaultValue}.
     */
    private static <T> T queryPlatformBean(final String attrName,
                                           final T defaultValue)
    {
        assert attrName != null : "Cannot query a null attribute name";
        assert !attrName.isEmpty() : "Cannot query an empty attribute name";
        T result = defaultValue;
        try {
            final ObjectName objName =
                new ObjectName("java.lang", "type", "OperatingSystem");
            final MBeanServer beanServer =
                ManagementFactory.getPlatformMBeanServer();
            final Object attr = beanServer.getAttribute(objName, attrName);
            if (attr != null) {
                // don't suppress warnings because we cannot guarantee that 
                // this cast is correct.
                result = (T)attr;
            }
        } catch (Exception e) {
            System.err.println("Couldn't query attribute: " + attrName);
        }
        return result;
    }

    /**
     * Get total system memory and print it out--accounts for 32bit JRE 
     * limitation of 1.5GB.
     * 
     * @return {@code String} representation of the total amount of system 
     * memory.
     */
    public static String getMemory() {
        GetMem nonStaticInstance = new GetMem();
        Object totalMemoryObject = queryPlatformBean("TotalPhysicalMemorySize", Long.MIN_VALUE);
        long totalMemory = ((Number)totalMemoryObject).longValue();
        boolean is64 = System.getProperty("os.arch").contains("64");
        int megabytes = Math.round(totalMemory / 1024 / 1024);
        if (!is64 && (megabytes > 1536)) {
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
