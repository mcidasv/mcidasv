/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2011
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

import static edu.wisc.ssec.mcidasv.Constants.PROP_VERSION_MAJOR;
import static edu.wisc.ssec.mcidasv.Constants.PROP_VERSION_MINOR;
import static edu.wisc.ssec.mcidasv.Constants.PROP_VERSION_RELEASE;
import static edu.wisc.ssec.mcidasv.Constants.PROP_BUILD_DATE;

import java.io.InputStream;
import java.util.Properties;

public class GetVer {

	public InputStream getResourceAsStream(String name) {
		return ClassLoader.getSystemResourceAsStream(name);
	}
	
    /**
     * The main. Get McIDAS-V version and build data and print it out.
     */
    public static void main(String[] args) throws Exception {
    	GetVer nonStaticInstance = new GetVer();
    	String version = "Unknown";
    	String date = "Unknown";
    	
    	try {
    		Properties props = new Properties();
    		InputStream is = nonStaticInstance.getResourceAsStream("edu/wisc/ssec/mcidasv/resources/build.properties");
    		props.load(is);
    		String maj = props.getProperty(PROP_VERSION_MAJOR, "0");
    		String min = props.getProperty(PROP_VERSION_MINOR, "0");
    		String rel = props.getProperty(PROP_VERSION_RELEASE, "");
    		date = props.getProperty(PROP_BUILD_DATE, "");
    		version = maj.concat(".").concat(min).concat(rel);
    	}
    	catch (Exception e) {
    		System.err.println("Error getting McIDAS-V version");
    	}

    	System.out.println("McIDAS-V version " + version + " built " + date);

    }
}