/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2015
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

package edu.wisc.ssec.mcidasv.data.hydra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

/**
 * Utility class to support Joint Polar Satellite System (JPSS) functionality.
 * Documentation referenced is from Suomi NPP Common Data Format Control Book.
 * See:
 * http://jointmission.gsfc.nasa.gov/science/documents.html
 * 
 * @author tommyj
 *
 */

public abstract class JPSSUtilities {
   
	public static final String JPSS_FIELD_SEPARATOR = "_";
	
	// This regular expression matches a Suomi NPP Data Product as defined by the 
	// NOAA spec in CDFCB-X Volume 1, Page 21
	public static final String SUOMI_NPP_REGEX_NOAA =
    		// Product Id, Multiple (ex: VSSTO-GATMO-VSLTO)
    		"(\\w\\w\\w\\w\\w-)*" + 
    		// Product Id, Single (ex: VSSTO)
    		"\\w\\w\\w\\w\\w" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Spacecraft Id (ex: npp)
    		"\\w\\w\\w" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Start Date (ex: dYYYYMMDD)
    		"d20[0-3]\\d[0-1]\\d[0-3]\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Start Time (ex: tHHMMSSS)
    		"t[0-2]\\d[0-5]\\d[0-6]\\d\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Stop Time (ex: eHHMMSSS)
    		"e[0-2]\\d[0-5]\\d[0-6]\\d\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Orbit Number (ex: b00015)
    		"b\\d\\d\\d\\d\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Creation Date (ex: cYYYYMMDDHHMMSSSSSSSS)
    		"c20[0-3]\\d[0-1]\\d[0-3]\\d[0-2]\\d[0-5]\\d[0-6]\\d\\d\\d\\d\\d\\d\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Origin (ex: navo)
    		"\\w\\w\\w\\w" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Domain (ex: ops)
    		"\\w\\w\\w" + 
    		// HDF5 suffix
    		".h5";
	
	// This regular expression matches a Suomi NPP Data Product as defined by the 
	// NASA spec in TBD
	public static final String SUOMI_NPP_REGEX_NASA =
			// Sensor - right now, only VIIRS
			"viirs" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
			// Always .L1B
    		"l1b-" + 
    		// Always m (M-Band), i (I-Band), or d (DNB)
    		"[dim]" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Platform - always Suomi NPP
    		"snpp" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Start Date (ex: dYYYYMMDD)
    		"d20[0-3]\\d[0-1]\\d[0-3]\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Start Time (ex: tHHMM)
    		"t[0-2]\\d[0-5]\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Creation Date (ex: cYYYYMMDDHHMMSSSSSSSS)
    		"c20[0-3]\\d[0-1]\\d[0-3]\\d\\d\\d\\d\\d\\d\\d" +
    		// NetCDF 4 suffix
    		".nc";
	
	// This regular expression matches a Suomi NPP geolocation file as defined by the 
	// NASA spec in TBD
	public static final String SUOMI_GEO_REGEX_NASA =
			// Sensor - right now, only VIIRS
			"viirs" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
			// Always .L1B
    		"geo-" + 
    		// Always m (M-Band), i (I-Band), or d (DNB)
    		"[dim]" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Platform - always Suomi NPP
    		"snpp" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Start Date (ex: dYYYYMMDD)
    		"d20[0-3]\\d[0-1]\\d[0-3]\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Start Time (ex: tHHMM)
    		"t[0-2]\\d[0-5]\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Creation Date (ex: cYYYYMMDDHHMMSSSSSSSS)
    		"c20[0-3]\\d[0-1]\\d[0-3]\\d\\d\\d\\d\\d\\d\\d" +
    		// NetCDF 4 suffix
    		".nc";
	
	public static String[] validNASAVariableNames = {
		"M01",
		"M02",
		"M03",
		"M04",
		"M05",
		"M06",
		"M07",
		"M08",
		"M09",
		"M10",
		"M11",
		"M12",
		"M13",
		"M14",
		"M15",
		"M16",
		"I01",
		"I02",
		"I03",
		"I04",
		"I05",
		"DNB_observations"
	};
	
	public static float[] ATMSChannelCenterFrequencies = {
		23.8f,
		31.4f,
		50.3f,
		51.76f,
		52.8f,
		53.596f,
		54.40f,
		54.94f,
		55.50f,
		57.29032f,
		57.29033f,
		57.29034f,
		57.29035f,
		57.29036f,
		57.29037f,
		88.20f,
		165.5f,
		183.3101f,
		183.3102f,
		183.3103f,
		183.3104f,
		183.3105f
	};
	
	// the list of valid geolocation product ids
	public static String[] geoProductIDs = {
    	"GATMO",
    	"GCRSO",
    	"GAERO",
    	"GCLDO",
    	"GDNBO",
    	"GNCCO",
    	"GIGTO",
    	"GIMGO",
    	"GITCO",
    	"GMGTO",
    	"GMODO",
    	"GMTCO",
    	"GNHFO",
    	"GOTCO",
    	"GOSCO",
    	"GONPO",
    	"GONCO",
    	"GCRIO",
    	"GATRO",
    	"IVMIM",
    	"VMUGE"
	};  
	
	// This regular expression matches a Suomi NPP geolocation granule, see 
	// NOAA spec in CDFCB-X Volume 1, Page 21
	public static final String SUOMI_GEO_REGEX_NOAA =
    		// Geo Id, Single (ex: GMODO)
			// NOTE: This MUST match the list of product ids in static array above!
    		"(GATMO|GCRSO|GAERO|GCLDO|GDNBO|GNCCO|GIGTO|GIMGO|GITCO|" + 
			"GMGTO|GMODO|GMTCO|GNHFO|GOTCO|GOSCO|GONPO|GONCO|GCRIO|GATRO|IVMIM|VMUGE)" + 
			JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Spacecraft Id (ex: npp)
    		"\\w\\w\\w" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Start Date (ex: dYYYYMMDD)
    		"d20[0-3]\\d[0-1]\\d[0-3]\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Start Time (ex: tHHMMSSS)
    		"t[0-2]\\d[0-5]\\d[0-6]\\d\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Data Stop Time (ex: eHHMMSSS)
    		"e[0-2]\\d[0-5]\\d[0-6]\\d\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Orbit Number (ex: b00015)
    		"b\\d\\d\\d\\d\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Creation Date (ex: cYYYYMMDDHHMMSSSSSSSS)
    		"c20[0-3]\\d[0-1]\\d[0-3]\\d[0-2]\\d[0-5]\\d[0-6]\\d\\d\\d\\d\\d\\d\\d" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Origin (ex: navo)
    		"\\w\\w\\w\\w" + JPSSUtilities.JPSS_FIELD_SEPARATOR +
    		// Domain (ex: ops)
    		"\\w\\w\\w" + 
    		// HDF5 suffix
    		".h5";
	
	/**
	 * Determine if the input variable name is a valid NASA product
	 *
	 * @param varName Variable name to validate.
	 *
	 * @return {@code true} if {@code varName} passes checks.
	 */
	
	public static boolean isValidNASA(String varName) {
		boolean isValid = false;
		if (varName == null) return isValid; 
		for (String s : validNASAVariableNames) {
			if (s.equals(varName)) {
				isValid = true;
				break;
			}
		}
		return isValid;
	}
	
	/**
	 * Determine if the set if filenames constitutes contiguous SNPP granules
	 * of the same geographic coverage.
	 *
	 * @param fileList List of files to validate.
	 *
	 * @return {@code true} if {@code fileList} passes checks.
	 */
	
	public static boolean isValidSet(List fileList) {
		
		// map with filename from start date through orbit will be used for comparisons
        Map<String, List<String>> metadataMap = new HashMap<String, List<String>>();
        
        // Pass 1, populate the list of products selected, and empty maps
        for (Object o : fileList) {
        	String filename = (String) o;
        	// start at last path separator to clip off absolute paths
        	int lastSeparator = filename.lastIndexOf(File.separatorChar);
        	// NOAA style products
        	if (filename.endsWith(".h5")) {
	        	// products go to first underscore, see regex above for more detail
	        	int firstUnderscore = filename.indexOf("_", lastSeparator + 1);
	        	String prodStr = filename.substring(lastSeparator + 1, firstUnderscore);
	        	if (! metadataMap.containsKey(prodStr)) {
					List<String> l = new ArrayList<String>();
					metadataMap.put(prodStr, l);
	        	}
        	}
        	// NASA style products
        	if (filename.endsWith(".nc")) {
	        	// products end at first underscore, see regex above for more detail
	        	int firstUnderscore = filename.indexOf("_", lastSeparator + 1);
	        	int secondUnderscore = filename.indexOf("_", firstUnderscore + 1);
	        	String prodStr = filename.substring(firstUnderscore + 1, secondUnderscore);
	        	if (! metadataMap.containsKey(prodStr)) {
					List<String> l = new ArrayList<String>();
					metadataMap.put(prodStr, l);
	        	}
        	}
        }
        
        // Pass 2, build up the lists of meta data strings and full filenames
        for (Object o : fileList) {
        	String filename = (String) o;
        	// start at last path separator to clip off absolute paths
        	int lastSeparator = filename.lastIndexOf(File.separatorChar);
        	
        	// NOAA style products
        	if (filename.endsWith(".h5")) {
	        	// products go to first underscore, see regex above for more detail
	        	int firstUnderscore = filename.indexOf("_", lastSeparator + 1);
	        	// this is the key for the maps
	        	String prodStr = filename.substring(lastSeparator + 1, firstUnderscore);
	        	// this is the value for the meta data map - start time through orbit 
	        	String metaStr = filename.substring(firstUnderscore + 1, firstUnderscore + 39);
	        	// get the appropriate list, add the new value
	        	List<String> l = (List<String>) metadataMap.get(prodStr);
	        	l.add(metaStr);
	        	metadataMap.put(prodStr, l);
        	}
        	
        	// NASA style products
        	if (filename.endsWith(".nc")) {
	        	// products end at first underscore, see regex above for more detail
	        	int firstUnderscore = filename.indexOf("_", lastSeparator + 1);
	        	int secondUnderscore = filename.indexOf("_", firstUnderscore + 1);
	        	// this is the key for the maps
	        	String prodStr = filename.substring(firstUnderscore + 1, secondUnderscore);
	        	// this is the value for the meta data map - date and time 
	        	int dateIndex = filename.indexOf("_d");
	        	int creationIndex = filename.indexOf("_c");
	        	String metaStr = filename.substring(dateIndex + 1, creationIndex);
	        	// get the appropriate list, add the new value
	        	List<String> l = (List<String>) metadataMap.get(prodStr);
	        	l.add(metaStr);
	        	metadataMap.put(prodStr, l);
        	}
        }
        
        // loop over metadata map, every list much match the one for ALL other products
        Set s = metadataMap.keySet();
        Iterator iterator = s.iterator();
        List prvList = null;
        while (iterator.hasNext()) {
        	String key = (String) iterator.next();
        	List l = (List) metadataMap.get(key);
        	for (int i = 0; i < l.size(); i++) {
        		if (prvList != null) {
        			if (! l.equals(prvList)) return false;
        		}
        	}
        	prvList = l;
        }
        
		return true;
	}

	/**
	 * Determine if a set if filenames which constitutes contiguous SNPP
	 * granules of various products all share the same geolocation data type.
	 *
	 * @param fileList List of files to validate.
	 * @param directory Used when {@literal "GEO"} is not embedded within one
	 *                  of {@code fileList}.
	 *
	 * @return {@code true} if {@code fileList} passes checks.
	 */
	
	public static boolean hasCommonGeo(List fileList, File directory) {
		Set<String> s = new HashSet<String>();
		boolean isCombinedProduct = false;
		
		// loop through all filenames provided
        for (Object o : fileList) {
        	isCombinedProduct = false;
        	String filename = (String) o;
        	
        	// check the case where GEO is embedded in the data granules
        	int lastSeparator = filename.lastIndexOf(File.separatorChar);
        	int firstUnderscore = filename.indexOf("_", lastSeparator + 1);
        	String prodStr = filename.substring(lastSeparator + 1, firstUnderscore);
            StringTokenizer st = new StringTokenizer(prodStr, "-");
            while (st.hasMoreTokens()) {
            	String singleProd = st.nextToken();
            	for (int i = 0; i < JPSSUtilities.geoProductIDs.length; i++) {
            		if (singleProd.equals(JPSSUtilities.geoProductIDs[i])) {
            			s.add(singleProd);
            			isCombinedProduct = true;
            			break;
            		}
            	}
            }
            // GEO not embedded in file, need to see which GEO file is
			// referenced in the global attribute
            if (! isCombinedProduct) {
            	try {
            		String fileFullPath = directory.getAbsolutePath() + File.separator + filename;
					NetcdfFile ncfile = NetcdfFile.open(fileFullPath);
					Attribute a = ncfile.findGlobalAttribute("N_GEO_Ref");
					if (a != null) {
						String geoFromAttr = a.getStringValue().substring(0, 5);
						s.add(geoFromAttr);
					}
					ncfile.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
            }
        }
        
        // if the products chosen utilize multiple GEO types, fail the selection
        if (s.size() > 1) return false;
		return true;
	}
	
}
