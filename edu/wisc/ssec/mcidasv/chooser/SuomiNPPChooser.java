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
package edu.wisc.ssec.mcidasv.chooser;

import java.awt.BorderLayout;
import java.io.File;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.StringUtil;

public class SuomiNPPChooser extends FileChooser {
	
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(SuomiNPPChooser.class);
	private static final long CONSECUTIVE_GRANULE_MAX_GAP_MS = 60000;
	
	// date formatter for converting Suomi NPP day/time from file name for consecutive granule check
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmSSS");

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

        // At present, Suomi NPP chooser only allows selecting sets of consecutive granules
        boolean granulesAreConsecutive = testConsecutiveGranules(files);
        if (granulesAreConsecutive) {
        	return super.selectFilesInner(files, directory);
        } else {
        	// throw up a dialog to tell user the problem
        	JOptionPane.showMessageDialog(this, "When selecting multiple granules, they must be consecutive.");
        }
        return false;
    }

    /**
     * Test whether a set of files are consecutive Suomi NPP granules,
     * any sensor.
     * @param files
     * @return true if consecutive tests pass for all files
     */
    
    private boolean testConsecutiveGranules(File[] files) {
    	boolean testResult = false;
    	if (files == null) return testResult;
		// compare start time of current granule with end time of previous
    	// difference should be very small - under a second
    	long prvTime = -1;
    	testResult = true;
        for (int i = 0; i < files.length; i++) {
            if ((files[i] != null) && !files[i].isDirectory()) {
                if (files[i].exists()) {
                	String fileName = files[i].getName(); 
                	int dateIndex = fileName.lastIndexOf("_d2") + 2;
                	int timeIndexStart = fileName.lastIndexOf("_t") + 2;
                	int timeIndexEnd = fileName.lastIndexOf("_e") + 2;
                	String dateStr = fileName.substring(dateIndex, dateIndex + 8);
                	String timeStrStart = fileName.substring(timeIndexStart, timeIndexStart + 7);
                	String timeStrEnd = fileName.substring(timeIndexEnd, timeIndexEnd + 7);
                	// sanity check on file name lengths
                	int fnLen = fileName.length();
                	if ((dateIndex > fnLen) || (timeIndexStart > fnLen) || (timeIndexEnd > fnLen)) {
                		logger.warn("unexpected file name length for: " + fileName);
                		testResult = false;
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
						testResult = false;
						break;
					}
					long curTime = dS.getTime();
					long endTime = dE.getTime();
					// only check current with previous
					if (prvTime > 0) {
						// make sure time diff does not exceed allowed threshold
						// consecutive granules should be less than 1 minute apart
						if ((curTime - prvTime) > CONSECUTIVE_GRANULE_MAX_GAP_MS) {
							testResult = false;
							break;
						}
					}
					prvTime = endTime;
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