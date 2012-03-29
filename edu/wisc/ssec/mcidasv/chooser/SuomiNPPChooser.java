/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2012
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

import java.awt.Component;
import java.awt.Container;

import java.io.File;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.StringUtil;

public class SuomiNPPChooser extends FileChooser {
	
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(SuomiNPPChooser.class);
	private static final long CONSECUTIVE_GRANULE_TIME_GAP_MS = 1000;
	
	// date formatter for converting Suomi NPP day/time from file name for consecutive granule check
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmSSS");

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
    		logger.trace("Creating Suomi NPP File Chooser...");
    		fileChooser = new SuomiNPPFileChooser(path);
    	} else {
    		logger.trace("2nd call to doMakeFileChooser, why?");
    	}
        return fileChooser;
    }
        
    /**
     * An extension of JFileChooser
     *
     * @author Tommy Jasmin
     */
    
    public class SuomiNPPFileChooser extends JFileChooser {

        /**
		 * default for serializable class
		 */
		private static final long serialVersionUID = 1L;

		/**
         * Create the file chooser
         *
         * @param path   the initial path
         */
        public SuomiNPPFileChooser(String path) {
            super(path);
            setControlButtonsAreShown(false);
            setMultiSelectionEnabled(true);
            setAcceptAllFileFilterUsed(false);
            processChildren(this);
        }

        private void processChildren(Container c) {
        	Component [] components = c.getComponents();
        	if (components != null) {
        		// loop through all components, looking for the JLabel children of 
        		// components we want to remove
        		for (int i = 0; i < components.length; i++) {
        			if (components[i] instanceof JLabel) {
        				String text = ((JLabel) components[i]).getText();
        				if (text.equals("File Name:")) {
        					hideChildren((Container) components[i].getParent());
        					continue;
        				}
        				if (text.equals("Files of Type:")) {
        					hideChildren((Container) components[i].getParent());
        					continue;
        				}
        			}
        			// now check this component for any children
        			processChildren((Container) components[i]);
        		}
        	}
        }
        
        private void hideChildren(Container c) {
        	Component [] components = c.getComponents();
        	for (int i = 0; i < components.length; i++) {
        		components[i].setVisible(false);
        	}
        	c.setVisible(false);
        }

        /**
         * Approve the selection
         */
        public void approveSelection() {
            SuomiNPPChooser.this.doLoad();
        }

        /**
         * Cancel the selection
         */
        public void cancelSelection() {
            closeChooser();
        }

        /**
         * Set the selected files
         *
         * @param selectedFiles  the selected files
         */
        public void setSelectedFiles(File[] selectedFiles) {
        	super.setSelectedFiles(selectedFiles);
            setHaveData( !((selectedFiles == null) || (selectedFiles.length == 0)));
        }
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
						dE = sdf.parse(dateStr + timeStrEnd);
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
						// consecutive granules should be less than 1 second apart
						if ((curTime - prvTime) > CONSECUTIVE_GRANULE_TIME_GAP_MS) {
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
    	
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new SuomiNPPFilter());

        return centerPanel;
    }
    
}