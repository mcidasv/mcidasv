/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */
package edu.wisc.ssec.mcidasv.chooser;

import java.awt.BorderLayout;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.data.hydra.JPSSUtilities;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.StringUtil;

public class SuomiNPPChooser extends FileChooser {
	
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(SuomiNPPChooser.class);
	// Our consecutive granule "slop"
	// No granule of any type should be shorter than this
	// And therefore no gap between consecutive granules could ever be greater. 5 seconds feels safe
	private static final long CONSECUTIVE_GRANULE_MAX_GAP_MS = 5000;
	private static final long CONSECUTIVE_GRANULE_MAX_GAP_MS_NASA = 360000;
	
	// date formatters for converting Suomi NPP day/time from file name for consecutive granule check
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssS");
    private static final SimpleDateFormat sdfNASA = new SimpleDateFormat("yyyyMMddHHmm");

    /**
     * Create the chooser with the given manager and xml
     *
     * @param mgr The manager
     * @param root The xml
     *
     */
    
    public SuomiNPPChooser(IdvChooserManager mgr, org.w3c.dom.Element root) {
        super(mgr, root);
    }
   
    /**
     * Make the file chooser
     *
     * @param path   the initial path
     *
     * @return  the file chooser
     */
    
    protected JFileChooser doMakeFileChooser(String path) {
    	if (fileChooser == null) {
    		logger.debug("Creating Suomi NPP File Chooser...");
    		fileChooser = new SuomiNPPFileChooser(path, this);
    	} else {
    		logger.debug("2nd call to doMakeFileChooser, why?");
    	}
        return fileChooser;
    }

    /**
     * Handle the selection of the set of files
     *
     * @param files The files the user chose
     * @param directory The directory they chose them from
     * @return True if the file was successful
     * @throws Exception
     */
    
    protected boolean selectFilesInner(File[] files, File directory)
            throws Exception {
        if ((files == null) || (files.length == 0)) {
            userMessage("Please select a file");
            return false;
        }

        // make a list of just the file names
        ArrayList<String> fileNames = new ArrayList<String>();
        for (int i = 0; i < files.length; i++) {
        	fileNames.add(files[i].getName());
        }

        // Kick out attempts to load "GCRSO-SCRIF-SCRIS" products, which for some
        // reason CLASS will provide, but scientists have told us we do not need
        // to support.
        if (JPSSUtilities.isInvalidCris(fileNames)) {
            JOptionPane.showMessageDialog(null,
                    "CrIS files containing both Full Spectral Science SDR (SCRIF)\n" +
                    "and Science SDR (SCRIS) are not supported.");
            return false;
        }

        // ensure these files make sense as a set to create a single SNPP data source
    	if (! JPSSUtilities.isValidSet(fileNames)) {
        	JOptionPane.showMessageDialog(null, 
			"Unable to group selected data as a single data source.");
        	return false;
    	}
    	
        // ensure these files make sense as a set to create a single SNPP data source
    	if (! JPSSUtilities.hasCommonGeo(fileNames, directory)) {
        	JOptionPane.showMessageDialog(null, 
			"Unable to group selected data as a single data source.");
        	return false;
    	}
    	
        // At present, Suomi NPP chooser only allows selecting sets of consecutive granules
    	int granulesAreConsecutive = -1;
        // Consecutive granule check - can only aggregate a contiguous set
    	if (files.length > 1) {
           granulesAreConsecutive = testConsecutiveGranules(files);
    	}
    	
    	// Need to reverse file list, so granules are increasing time order
    	if (granulesAreConsecutive == 1) {
    	   Collections.reverse(Arrays.asList(files));
    	}
    	
        if ((granulesAreConsecutive >= 0) || (files.length == 1)) {
        	return super.selectFilesInner(files, directory);
        } else {
        	// throw up a dialog to tell user the problem
            JOptionPane.showMessageDialog(this,
                "When selecting multiple granules, they must be consecutive and from the same satellite.");
        }
        return false;
    }

    /**
     * Test whether a set of files are consecutive Suomi NPP granules,
     * any sensor. NOTE: This method works when the file list contains
     * multiple products ONLY because once we've validate one product,
     * the time check will be a negative number when comparing the FIRST
     * granule of product 2 with the LAST granule of product 1. A better
     * implementation would be to pass in the filename map like the 
     * one generated in SuomiNPPDataSource constructor.
     * 
     * @param files
     * @return 0 if consecutive tests pass for all files
     *        -1 if tests fail
     *         1 if tests pass but file order is backward 
     *           (decreasing time order)
     */
    
    private int testConsecutiveGranules(File[] files) {
    	int testResult = -1;
    	if (files == null) return testResult;
    	
    	// TJJ Jan 2016 - different checks for NASA data, 6 minutes per granule
    	File f = files[0];

    	if (f.getName().matches(JPSSUtilities.SUOMI_NPP_REGEX_NASA)) {
			// compare start time of current granule with end time of previous
	    	// difference should be very small - under a second
	    	long prvTime = -1;
	    	testResult = 0;
	        for (int i = 0; i < files.length; i++) {
	            if ((files[i] != null) && !files[i].isDirectory()) {
	                if (files[i].exists()) {
	                	String fileName = files[i].getName();
	                	int dateIndex = fileName.lastIndexOf("_d2") + 2;
	                	int timeIndex = fileName.lastIndexOf("_t") + 2;
	                	String dateStr = fileName.substring(dateIndex, dateIndex + 8);
	                	String timeStr = fileName.substring(timeIndex, timeIndex + 4);
	                    // pull start and end time out of file name
	                    Date dS = null;
	                    try {
							dS = sdfNASA.parse(dateStr + timeStr);
	                    } catch (ParseException pe) {
							logger.error("Not recognized as valid Suomi NPP file name: " + fileName);
							testResult = -1;
							break;
	                    }
						long curTime = dS.getTime();
						// only check current with previous
						if (prvTime > 0) {
							// make sure time diff does not exceed allowed threshold
							// consecutive granules should be less than 1 minute apart
							if ((curTime - prvTime) > CONSECUTIVE_GRANULE_MAX_GAP_MS_NASA) {
								testResult = -1;
								break;
							}
                            // TJJ Inq #2265, #2370. Granules need to be increasing time order 
                            // to properly georeference. If they are reverse order but pass
							// all consecutive tests, we just reverse the list before returning
                            if (curTime < prvTime) {
                                testResult = 1;
                                break;
                            }
						}
						prvTime = curTime;
	                }
	            }
	        }

	    // consecutive granule check for NOAA data
    	} else {
			// compare start time of current granule with end time of previous
	    	// difference should be very small - under a second
	    	long prvTime = -1;
            long prvStartTime = -1;
            long prvEndTime = -1;
	    	testResult = 0;
            int lastSeparator = -1;
            int firstUnderscore = -1;
            String prodStr = "";
            String prevPrd = "";
            String dateIdx = "_d2";
            String startTimeIdx = "_t";
            String endTimeIdx = "_e";
            String curPlatformStr = null;
            String prvPlatformStr = null;
            int firstSeparator = -1;
            int timeFieldStart = 2;
            if (f.getName().matches(JPSSUtilities.JPSS_REGEX_ENTERPRISE_EDR)) {
                dateIdx = "_s";
                startTimeIdx = "_s";
                endTimeIdx = "_e";
                timeFieldStart = 10;
            }
	        for (int i = 0; i < files.length; i++) {
	            if ((files[i] != null) && !files[i].isDirectory()) {
	                if (files[i].exists()) {
                        String fileName = files[i].getName();

                        // get platform - 3 chars after first separator char
                        firstSeparator = fileName.indexOf(JPSSUtilities.JPSS_FIELD_SEPARATOR);
                        curPlatformStr = fileName.substring(firstSeparator + 1, firstSeparator + 4);
                        logger.debug("platform: " + curPlatformStr);
                        if ((prvPlatformStr != null) && (! curPlatformStr.equals(prvPlatformStr))) {
                            logger.warn("Mixed platforms in filelist: " +
                                curPlatformStr + ", and: " + prvPlatformStr);
                            testResult = -1;
                            break;
                        }
                        prvPlatformStr = curPlatformStr;

                        lastSeparator = fileName.lastIndexOf(File.separatorChar);
                        firstUnderscore = fileName.indexOf("_", lastSeparator + 1);
                        prodStr = fileName.substring(lastSeparator + 1, firstUnderscore);
                        // reset check if product changes
                        if (! prodStr.equals(prevPrd)) prvTime = -1;
                        int dateIndex = fileName.lastIndexOf(dateIdx) + 2;
                        int timeIndexStart = fileName.lastIndexOf(startTimeIdx) + timeFieldStart;
                        int timeIndexEnd = fileName.lastIndexOf(endTimeIdx) + timeFieldStart;
                        String dateStr = fileName.substring(dateIndex, dateIndex + 8);
                        String timeStrStart = fileName.substring(timeIndexStart, timeIndexStart + 7);
                        String timeStrEnd = fileName.substring(timeIndexEnd, timeIndexEnd + 7);
	                	// sanity check on file name lengths
	                	int fnLen = fileName.length();
	                	if ((dateIndex > fnLen) || (timeIndexStart > fnLen) || (timeIndexEnd > fnLen)) {
	                		logger.warn("unexpected file name length for: " + fileName);
	                		testResult = -1;
	                		break;
	                	}
	                    // pull start and end time out of file name
	                    Date dS = null;
	                    Date dE = null;

	                    try {
							dS = sdf.parse(dateStr + timeStrStart);
							// due to nature of Suomi NPP file name encoding, we need a special
							// check here - end time CAN roll over to next day, while day part 
							// does not change.  if this happens, we tweak the date string
							String endDateStr = dateStr;
							String startHour = timeStrStart.substring(0, 2);
							String endHour = timeStrEnd.substring(0, 2);
							if ((startHour.equals("23")) && (endHour.equals("00"))) {
								// temporarily convert date to integer, increment, convert back
								int tmpDate = Integer.parseInt(dateStr);
								tmpDate++;
								endDateStr = "" + tmpDate;
								logger.info("Granule time spanning days case handled ok...");
							}
							dE = sdf.parse(endDateStr + timeStrEnd);
						} catch (ParseException e) {
							logger.error("Not recognized as valid Suomi NPP file name: " + fileName);
							testResult = -1;
							break;
						}
						long curTime = dS.getTime();
						long endTime = dE.getTime();

						// only check current with previous
						if (prvTime > 0) {

							// Make sure time diff does not exceed allowed threshold for the sensor
							// Whatever the granule size, the time gap cannot exceed our defined "slop"
							logger.debug("curTime (ms): " + curTime);
							logger.debug("prvTime (ms): " + prvTime);
							logger.debug("curTime - prvEndTime (ms): " + Math.abs(curTime - prvEndTime));
							if (Math.abs(curTime - prvEndTime) > CONSECUTIVE_GRANULE_MAX_GAP_MS) {
								// Make sure there really is a gap, and not granule overlap
								if (prvEndTime < curTime) {
									testResult = -1;
									break;
								}
							}

                            // TJJ Inq #2265, #2370. Granules need to be increasing time order 
                            // to properly georeference. If they are reverse order but pass
                            // all consecutive tests, we just reverse the list before returning
							if (curTime < prvStartTime) {
							    testResult = 1;
							    break;
							}

						}
						prvTime = curTime;
						prvStartTime = curTime;
						prvEndTime = endTime;
						prevPrd = prodStr;
	                }
	            }
	        }
    	}
		return testResult;
	}

	/**
     * Convert the given array of File objects
     * to an array of String file names. Only
     * include the files that actually exist.
     *
     * @param files Selected files
     * @return Selected files as Strings
     */
    
    protected String[] getFileNames(File[] files) {
        if (files == null) {
            return (String[]) null;
        }
        Vector<String> v = new Vector<String>();
        String fileNotExistsError = "";

        // NOTE:  If multiple files are selected, then missing files
        // are not in the files array.  If one file is selected and
        // it is not there, then it is in the array and file.exists()
        // is false
        for (int i = 0; i < files.length; i++) {
            if ((files[i] != null) && !files[i].isDirectory()) {
                if ( !files[i].exists()) {
                    fileNotExistsError += "File does not exist: " + files[i] + "\n";
                } else {
                    v.add(files[i].toString());
                }
            }
        }

        if (fileNotExistsError.length() > 0) {
            userMessage(fileNotExistsError);
            return null;
        }

        return v.isEmpty()
               ? null
               : StringUtil.listToStringArray(v);
    }
    
    /**
     * Get the bottom panel for the chooser
     * @return the bottom panel
     */
    
    protected JPanel getBottomPanel() {
    	// No bottom panel at present
    	return null;
    }
    
    /**
     * Get the center panel for the chooser
     * @return the center panel
     */
    
    protected JPanel getCenterPanel() {
    	JPanel centerPanel = super.getCenterPanel();

    	JPanel jp = new JPanel(new BorderLayout()) {
    		public void paint(java.awt.Graphics g) {
    			FileFilter ff = fileChooser.getFileFilter();
    			if (! (ff instanceof SuomiNPPFilter)) {
    				fileChooser.setAcceptAllFileFilterUsed(false);
    				fileChooser.setFileFilter(new SuomiNPPFilter());
    			}
    			super.paint(g);
    		}
    	};
    	jp.add(centerPanel);

    	return jp; 
    }
    
}
