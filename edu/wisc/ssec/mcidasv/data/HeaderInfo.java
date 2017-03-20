/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2017
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

package edu.wisc.ssec.mcidasv.data;

import java.io.File;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * {@literal "Base"} class that represents several different types of header
 * files.
 */
public class HeaderInfo {

    // Enumerate some constants
    public static final int kFormatUnknown = -1;
    public static final int kFormatASCII = 0;
    public static final int kFormat1ByteUInt = 1;
    public static final int kFormat2ByteSInt = 2;
    public static final int kFormat4ByteSInt = 3;
    public static final int kFormat4ByteFloat = 4;
    public static final int kFormat8ByteDouble = 5;
    public static final int kFormat2x8Byte = 9;
    public static final int kFormat2ByteUInt = 12;
    public static final int kFormatImage = 8080;

    public static final int kNavigationUnknown = -1;
    public static final int kNavigationBounds = 1;
    public static final int kNavigationFiles = 2;

    public static final String kInterleaveSequential = "BSQ";
    public static final String kInterleaveByLine = "BIL";
    public static final String kInterleaveByPixel = "BIP";

    public static final String DESCRIPTION = "description";
    public static final String ELEMENTS = "elements";
    public static final String LINES = "lines";
    public static final String UNIT = "unit";
    public static final String OFFSET = "offset";
    public static final String DATATYPE = "dataType";
    public static final String MISSINGVALUE = "missingValue";
    public static final String BANDNAMES = "bandNames";
    public static final String BANDFILES = "bandFiles";
    public static final String INTERLEAVE = "interleave";
    public static final String BYTEORDER = "byteOrder";
    public static final String BIGENDIAN = "bigEndian";
    public static final String NAVBOUNDS = "navBounds";
    public static final String NAVFILES = "navFiles";

    /** Path to header file. */
    private String headerFile = "";
    
    /** Map containing relevant parameters and values in {@code headerFile}. */
    private Hashtable parameters = new Hashtable();

    /**
     * Ctor for xml encoding
     */
    public HeaderInfo() {}

    /**
     * CTOR
     *
     * @param thisFile File to use. Cannot be {@code null}.
     */
    public HeaderInfo(File thisFile) {
        this(thisFile.getAbsolutePath());
    }

    /**
     * CTOR
     *
     * @param filename File containing header information.
     */
    public HeaderInfo(String filename) {
        setFilename(filename);
    }

    /**
     * Set the file name to be used.
     *
     * @param filename File containing header information.
     */
    public void setFilename(String filename) {
        parameters = new Hashtable();
        headerFile = filename;
    }

    /**
     * Get the file being used.
     * 
     * @return File in use.
     */
    public String getFilename() {
        return headerFile;
    }

    /**
     * Get the current header's band count.
     * 
     * @return Number of bands.
     */
    public int getBandCount() {
        if (!haveParsed()) {
            parseHeader();
        }
        List bandNames = getParameter(BANDNAMES, new ArrayList());
        return bandNames.size();
    }

    /**
     * Return {@code String} value associated with {@code parameter}.
     * 
     * @param parameter Parameter name.
     * @param defaultValue Value to return if {@code parameter} does not exist.
     *
     * @return Either the {@code List} associated with {@code parameter}, or 
     *         {@code defaultValue}.
     */
    public String getParameter(String parameter, String defaultValue) {
        parseHeader();
        Object hashedValue = parameters.get(parameter);
        if (hashedValue == null || !(hashedValue instanceof String)) {
            return defaultValue;
        }
        return (String)hashedValue;
    }

    /**
     * Return {@code Integer} value associated with {@code parameter}.
     *
     * @param parameter Parameter name.
     * @param defaultValue Value to return if {@code parameter} does not exist.
     *
     * @return Either the {@code Integer} associated with {@code parameter}, or 
     *         {@code defaultValue}.
     */
    public Integer getParameter(String parameter, Integer defaultValue) {
        parseHeader();
        Object hashedValue = parameters.get(parameter);
        if (hashedValue == null || !(hashedValue instanceof Integer)) {
            return defaultValue;
        }
        return (Integer)hashedValue;
    }

    /**
     * Return {@code Float} value associated with {@code parameter}.
     *
     * @param parameter Parameter name.
     * @param defaultValue Value to return if {@code parameter} does not exist.
     *
     * @return Either the {@code Float} associated with {@code parameter}, or 
     *         {@code defaultValue}.
     */
    public Float getParameter(String parameter, Float defaultValue) {
        parseHeader();
        Object hashedValue = parameters.get(parameter);
        if (hashedValue == null || !(hashedValue instanceof Float)) {
            return defaultValue;
        }
        return (Float)hashedValue;
    }

    /**
     * Return {@code Boolean} value associated with {@code parameter}.
     *
     * @param parameter Parameter name.
     * @param defaultValue Value to return if {@code parameter} does not exist.
     *
     * @return Either the {@code Boolean} associated with {@code parameter}, or 
     *         {@code defaultValue}.
     */
    public Boolean getParameter(String parameter, Boolean defaultValue) {
        parseHeader();
        Object hashedValue = parameters.get(parameter);
        if (hashedValue == null || !(hashedValue instanceof Boolean)) {
            return defaultValue;
        }
        return (Boolean)hashedValue;
    }

    /**
     * Return {@code List} associated with {@code parameter}.
     * 
     * @param parameter Parameter name.
     * @param defaultValue Value to return if {@code parameter} does not exist.
     * 
     * @return Either the {@code List} associated with {@code parameter}, or 
     *         {@code defaultValue}.
     */
    public List getParameter(String parameter, List defaultValue) {
        parseHeader();
        Object hashedValue = parameters.get(parameter);
        if (hashedValue == null || !(hashedValue instanceof List)) {
            return defaultValue;
        }
        return (List)hashedValue;
    }

    /**
     * Set a parsed parameter value.
     * 
     * @param parameter Parameter name.
     * @param value Value associated with {@code parameter}.
     */
    public void setParameter(String parameter, Object value) {
        parameters.put(parameter, value);
    }

    /**
     * Have we already parsed once?
     * 
     * @return {@code true} if {@link #parameters} has at least entry.
     */
    public boolean haveParsed() {
        return parameters.size() > 0;
    }

    /**
     * Does the file we are pointing to even exist?
     * 
     * @return Whether or not {@link #headerFile} exists.
     */
    public boolean doesExist() {
        File checkFile = new File(headerFile);
        return checkFile.exists();
    }

    /**
     * Override this when extending for a specific header type
     */
    protected void parseHeader() {}
}
