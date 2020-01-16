/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2020
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

package edu.wisc.ssec.mcidasv.chooser;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.swing.filechooser.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import edu.wisc.ssec.mcidasv.data.hydra.JPSSUtilities;

/**
 * File filter to pass names which match format for Suomi NPP data.
 * 
 * @author tommyj
 *
 */

public class SuomiNPPFilter extends FileFilter {
	
	private static final Logger logger =
		LoggerFactory.getLogger(SuomiNPPFilter.class);
	
	private static final String PRODUCT_SEPARATOR = "-";
	private static String PREV_DIRECTORY = null;
	private static String DATA_DESCRIPTION = "JPSS Data";
	private HashMap<String, File> seenGranules = new HashMap<String, File>();
	private HashMap<String, File> validGranules = new HashMap<String, File>();
    	
    	public SuomiNPPFilter() {
    		super();
    	}
    	
    	// Accept all directories and all Suomi NPP files.
        public boolean accept(File f) {
        	
            if (f.isDirectory()) {
                return true;
            }
            
        	// avoid constant rescans on window resizing and scrolling
        	String curDirectory = f.getParent();
        	if ((PREV_DIRECTORY != null) && (curDirectory.equals(PREV_DIRECTORY))) {
        		if (seenGranules.containsKey(f.getName())) {
        			// XXX TJJ - Still don't know why accept gets called so often...
        			// Made a non-McV, standalone Swing app with JFileChooser and 
        			// got same behavior - thousands of accept calls with mouse
        			// movement, particularly two-finger scrolling on a Mac.
        			if (validGranules.containsKey(f.getName())) {
        				return true;
        			} else {
        				return false;
        			}
        		} else {
        			seenGranules.put(f.getName(), f);
        		}
        	} else {
        		PREV_DIRECTORY = curDirectory;
        		seenGranules.clear();
        		validGranules.clear();
        	}

            if (isSuomiNPPFile(f)) {
            	validGranules.put(f.getName(), f);
            	return true;
            } else {
            	return false;
            }

        }
        
        /**
         * Is this a Suomi NPP Product Data file?
         * 
         * @param f name of file to test
         * @return	true if conditions met (mostly presence of geolocation)
         */
        
        private boolean isSuomiNPPFile(File f) {
        	
        	boolean isSuomiNPP_NOAA = false;
        	boolean isSuomiNPP_NASA = false;
        	boolean isSuomiNPP = false;
        	
        	String fileNameRelative = f.getName();
        	String fileNameAbsolute = f.getParent() + File.separatorChar + f.getName();
        	
        	// null or empty filename
        	if ((fileNameRelative == null) || (fileNameRelative.equals(""))) return isSuomiNPP;
        	
        	// see if relative filename matches the Suomi NPP NOAA regular expression	
        	if (fileNameRelative.matches(JPSSUtilities.SUOMI_NPP_REGEX_NOAA)) {
        		isSuomiNPP_NOAA = true;
        		logger.debug(fileNameRelative + " matches Suomi NPP NOAA regex");
        	// else see if relative filename matches the Suomi NPP NASA regular expression
        	} else if (fileNameRelative.matches(JPSSUtilities.SUOMI_NPP_REGEX_NASA)) {
        		isSuomiNPP_NASA = true;
        		logger.debug(fileNameRelative + " matches Suomi NPP NASA regex");
        	} else {
        		// don't go any further if file does not match Suomi NPP data product regex
        		logger.debug(fileNameRelative + " does not match any Suomi regex");
        		return isSuomiNPP;
        	}
        	
        	// make sure a geolocation file is present if it does look like a valid Suomi NPP data file!
        	
        	// if a geo dataset is embedded in a multi-product file, we can call it good without
        	// having to open any files.  Just look for a geo product id in the filename.
        	// HOWEVER - if it's a single-product GEO-only file, disqualify that
        	if (isSuomiNPP_NOAA) {
	        	String prodStr = fileNameRelative.substring(0, fileNameRelative.indexOf(JPSSUtilities.JPSS_FIELD_SEPARATOR));
	            StringTokenizer st = new StringTokenizer(prodStr, PRODUCT_SEPARATOR);
	            int numTokens = st.countTokens();
	            logger.trace("check for embedded GEO, tokenizing: " + prodStr);
	            while (st.hasMoreTokens()) {
	            	String singleProd = st.nextToken();
	            	for (int i = 0; i < JPSSUtilities.geoProductIDs.length; i++) {
	            		if (singleProd.equals(JPSSUtilities.geoProductIDs[i])) {
	            			logger.trace("Found embedded GEO: " + singleProd);
	            			// if it's a single-product file, disqualify this as a GEO-only file!
	            			if (numTokens == 1) {
	            				return false;
	            			} else {
	            				if (numTokens > 1) {
	            					return true;
	            				}
	            			}
	            		}
	            	}
	            }
        	}
        	
    		// looks like a standalone product - will have to look for separate geo file
            // first, create the corresponding GEO loc file name
    		String geoProductID = null;
    		
    		boolean noGeo = false;
    		
    		if (isSuomiNPP_NOAA) {
	    		NetcdfFile ncfile = null;
	    		try {
	    			logger.debug("Trying to open file: " + fileNameAbsolute);
	    			ncfile = NetcdfFile.open(fileNameAbsolute);
	    			Attribute a = ncfile.findGlobalAttribute("N_GEO_Ref");
	    			// if no GEO attribute, we can't visualize this Suomi NPP data file, don't include it
	    			if (a == null) {
	    				noGeo = true;
	    			} else {
	        			// in the newest operational data format, attribute is entire file name
	        			// if this is detected, no translation/mapping needed
	        			String geoStr = a.getStringValue();
	        			if (geoStr.matches(JPSSUtilities.SUOMI_GEO_REGEX_NOAA)) {
	        				geoProductID = geoStr;
	        			} else {
	        				noGeo = true;
	        			}
	    			}
	    		} catch (Exception e) {
	    			logger.error("Exception during open file: " + fileNameAbsolute, e);
	    		} finally {
	    			try {
	    				if (ncfile != null) ncfile.close();
	    			} catch (IOException ioe) {
	    				logger.error("Problem closing ncfile", ioe);
	    			}
	    		}
    		}
    		
    		if (isSuomiNPP_NASA) {
    			geoProductID = fileNameRelative.replace("L1B", "GEO");
    			logger.debug("Will be looking for NASA GEO file: " + geoProductID);
    		}
    		
    		// if no geolocation global attribute found, skip this file
    		if (noGeo) {
    			isSuomiNPP = false;
    		} else {
    		
    			if (isSuomiNPP_NOAA) {
	    			// ok, we know what the geo file is supposed to be, but is it present in this directory?
	    			String geoFilename = fileNameAbsolute.substring(0, fileNameAbsolute.lastIndexOf(File.separatorChar) + 1);
	    			geoFilename += geoProductID;
	    			
	    			// first check for the typically referenced ellipsoid geolocation
	    			if (! isSuomiNPP) {
	    				geoFilename = geoFilename.substring(geoFilename.lastIndexOf(File.separatorChar) + 1);
	    				
	    				// now we make a file filter, and see if a matching geo file is present
	    				File fList = new File(fileNameAbsolute.substring(0, fileNameAbsolute.lastIndexOf(File.separatorChar) + 1)); // current directory
	
	    				FilenameFilter geoFilter = new FilenameFilter() {
	    					public boolean accept(File dir, String name) {
	    						if (name.matches(JPSSUtilities.SUOMI_GEO_REGEX_NOAA)) {
	    							return true;
	    						} else {
	    							return false;
	    						}
	    					}
	    				};
	    				
	    				File[] files = fList.listFiles(geoFilter);
	    				for (File file : files) {
	    					if (file.isDirectory()) {
	    						continue;
	    					}
	    					// get the file name for convenience
	    					String fName = file.getName();
	    					// is it one of the geo types we are looking for?
	    					if (fName.substring(0, 5).equals(geoFilename.substring(0, 5))) {
	    						int geoStartIdx = geoFilename.indexOf("_d");
	    						int prdStartIdx = fileNameRelative.indexOf("_d");
	    						String s1 = 
	    							geoFilename.substring(geoStartIdx, geoStartIdx + JPSSUtilities.NOAA_CREATION_DATE_INDEX);
	    						String s2 = 
	    							fileNameRelative.substring(prdStartIdx, prdStartIdx + JPSSUtilities.NOAA_CREATION_DATE_INDEX);
	    						if (s1.equals(s2)) {
	    							isSuomiNPP = true;
	    							break;
	    						}
	    					}
	    				}
	
	    			} 

	    			// one last thing to check, if no luck so far...
	    			// are we using terrain-corrected geolocation?
	    			if (! isSuomiNPP) {
	    				geoFilename = geoFilename.substring(geoFilename.lastIndexOf(File.separatorChar) + 1);
	    				// this one looks for GMTCO instead of GMODO
	    				geoFilename = geoFilename.replace("OD", "TC");
	    				// this one looks for GITCO instead of GIMGO
	    				geoFilename = geoFilename.replace("MG", "TC");
	    				
	    				// now we make a file filter, and see if a matching geo file is present
	    				File fList = new File(fileNameAbsolute.substring(0, fileNameAbsolute.lastIndexOf(File.separatorChar) + 1)); // current directory
	
	    				FilenameFilter geoFilter = new FilenameFilter() {
	    					public boolean accept(File dir, String name) {
	    						if (name.matches(JPSSUtilities.SUOMI_GEO_REGEX_NOAA)) {
	    							return true;
	    						} else {
	    							return false;
	    						}
	    					}
	    				};
	    				
	    				File[] files = fList.listFiles(geoFilter);
	    				for (File file : files) {
	    					if (file.isDirectory()) {
	    						continue;
	    					}
	    					// get the file name for convenience
	    					String fName = file.getName();
	    					// is it one of the geo types we are looking for?
	    					if (fName.substring(0, 5).equals(geoFilename.substring(0, 5))) {
	    						int geoStartIdx = geoFilename.indexOf("_d");
	    						int prdStartIdx = fileNameRelative.indexOf("_d");
	    						String s1 = 
	    							geoFilename.substring(geoStartIdx, geoStartIdx + JPSSUtilities.NOAA_CREATION_DATE_INDEX);
	    						String s2 = 
	    							fileNameRelative.substring(prdStartIdx, prdStartIdx + JPSSUtilities.NOAA_CREATION_DATE_INDEX);
	    						if (s1.equals(s2)) {
	    							isSuomiNPP = true;
	    							break;
	    						}
	    					}
	    				}
	    			}
    			}
    			
    			if (isSuomiNPP_NASA) {
    				logger.debug("Checking for NASA-style GEO...");
    				String geoFilename = geoProductID;
    				logger.debug("Target FileName: " + geoFilename);
    				
    				// now we make a file filter, and see if a matching geo file is present
    				File fList = new File(
    						fileNameAbsolute.substring(0, fileNameAbsolute.lastIndexOf(File.separatorChar) + 1)
    				);

    				FilenameFilter geoFilter = new FilenameFilter() {
    					public boolean accept(File dir, String name) {
    						if (name.matches(JPSSUtilities.SUOMI_GEO_REGEX_NASA)) {
    							return true;
    						} else {
    							return false;
    						}
    					}
    				};
    				
    				File[] files = fList.listFiles(geoFilter);
    				for (File file : files) {
    					if (file.isDirectory()) {
    						continue;
    					}
    					// get the file name for convenience
    					String fName = file.getName();
    					logger.debug("FileName: " + fName);
    					// is it one of the geo types we are looking for?
    					if (fName.substring(0, 5).equals(geoFilename.substring(0, 5))) {
    						int geoStartIdx = geoFilename.indexOf("_d");
    						int prdStartIdx = fileNameRelative.indexOf("_d");
    						String s1 = 
    							geoFilename.substring(geoStartIdx, geoStartIdx + JPSSUtilities.NASA_CREATION_DATE_INDEX);
    						String s2 = 
    							fileNameRelative.substring(prdStartIdx, prdStartIdx + JPSSUtilities.NASA_CREATION_DATE_INDEX);
    						if (s1.equals(s2)) {
    							isSuomiNPP = true;
    							break;
    						}
    					}
    				}
    			}
    		}
        	
        	return isSuomiNPP;
        }
        
        // The description of this filter
        public String getDescription() {
            return DATA_DESCRIPTION;
        }
        
    }
