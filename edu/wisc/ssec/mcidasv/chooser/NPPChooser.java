/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

import edu.wisc.ssec.mcidasv.data.hydra.JPSSUtilities;

import java.awt.Component;
import java.awt.Container;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.nc2.NetcdfFile;

import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.StringUtil;

public class NPPChooser extends FileChooser {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(NPPChooser.class);
    
    private NPPFilter nppf = null;

    /**
     * Create the chooser with the given manager and xml
     *
     * @param mgr The manager
     * @param root The xml
     *
     */
    
    public NPPChooser(IdvChooserManager mgr, org.w3c.dom.Element root) {
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
        return new NPPFileChooser(path);
    }
    
    /**
     * Is this an NPP Product Data file?
     * 
     * @param f name of file to test
     * @return
     */
    
    private boolean isNPPFile(File f) {
    	 	
    	String nppRegex1 = "\\w\\w\\w\\w\\w_npp_d\\d\\d\\d\\d\\d\\d\\d\\d_t\\d\\d\\d\\d\\d\\d_e\\d\\d\\d\\d\\d\\d_b\\d\\d\\d\\d\\d_c\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d_\\w\\w\\w_OPS_SEG.h5";
    	String nppRegex2 = "\\w\\w\\w\\w\\w_npp_d\\d\\d\\d\\d\\d\\d\\d\\d_t\\d\\d\\d\\d\\d\\d\\d_e\\d\\d\\d\\d\\d\\d\\d_b\\d\\d\\d\\d\\d_c\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d_noaa_ada.h5";

    	boolean isNPP = false;
    	
    	String fileNameRelative = f.getName();
    	String fileNameAbsolute = f.getParent() + File.separatorChar + f.getName();
    	
    	// null or empty filename
    	if ((fileNameRelative == null) || (fileNameRelative.equals(""))) return isNPP;
    	
    	// just see if it matches the regular expression for now
    	// XXX at some point we should check all valid product ids
    	
    	if (	(fileNameRelative.matches(nppRegex1)) ||
    			(fileNameRelative.matches(nppRegex2)) )
    	{
    		isNPP = true;
    	// don't go any further if file does not match NPP data product regex
    	} else {
    		return isNPP;
    	}
    	
    	// before we leave, disqualify geolocation files
    	if (fileNameRelative.startsWith("GATMO")) isNPP = false;
    	if (fileNameRelative.startsWith("GCRIO")) isNPP = false;
    	if (fileNameRelative.startsWith("GCRSO")) isNPP = false;
    	if (fileNameRelative.startsWith("GMGTO")) isNPP = false;
    	if (fileNameRelative.startsWith("GMODO")) isNPP = false;
    	if (fileNameRelative.startsWith("GMTCO")) isNPP = false;
    	if (fileNameRelative.startsWith("IVMIM")) isNPP = false;
    	if (fileNameRelative.startsWith("VMUGE")) isNPP = false;
    	if (fileNameRelative.startsWith("GNCCO")) isNPP = false;
    	if (fileNameRelative.startsWith("GDNBO")) isNPP = false;
    	if (fileNameRelative.startsWith("GIGTO")) isNPP = false;
    	if (fileNameRelative.startsWith("GIMGO")) isNPP = false;
    	if (fileNameRelative.startsWith("GITCO")) isNPP = false;
    	if (fileNameRelative.startsWith("GCLDO")) isNPP = false;
    	
    	// but make sure a geolocation file is present if it IS a valid NPP data file!
		// first, create the corresponding GEO loc file name
		String geoProductID = null;
		
		boolean noGeo = false;
		NetcdfFile ncfile = null;
		try {
			logger.info("Trying to open file: " + fileNameAbsolute);
			ncfile = NetcdfFile.open(fileNameAbsolute);
			ucar.nc2.Attribute a = ncfile.findGlobalAttribute("N_GEO_Ref");
			// if no GEO attribute, we can't visualize this NPP data file, don't include it
			if (a == null) {
				noGeo = true;
			} else {
    			logger.info("Value of GEO global attribute: " + a.getStringValue());
    			geoProductID = JPSSUtilities.mapGeoRefToProductID(a.getStringValue());
    			logger.info("Value of corresponding Product ID: " + geoProductID);
			}
		} catch (Exception e) {
			logger.error("Exception during open file: " + fileNameAbsolute);
			e.printStackTrace();
		} finally {
			try {
				ncfile.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
		// if no geolocation global attribute found, skip this file
		if (noGeo) {
			isNPP = false;
		} else {
		
			// ok, we know what the geo file is supposed to be, but is it present in this directory?
			String geoFilename = fileNameAbsolute.substring(0, fileNameAbsolute.lastIndexOf(File.separatorChar) + 1);
			geoFilename += geoProductID;
			geoFilename += fileNameAbsolute.substring(fileNameAbsolute.lastIndexOf(File.separatorChar) + 6);
			File geoFile = new File(geoFilename);
			
			if (geoFile.exists()) {
				logger.info("GEO file FOUND: " + geoFilename);
			    isNPP = true;
			} else {
				logger.info("GEO file NOT found: " + geoFilename);
				isNPP = false;
			}    
			
		}
    	
    	return isNPP;
    }
    
   
    /* NPPFileView aggregates user's view of NPP data products */
    public class NPPFileView extends FileView {
  
        public String getName(File f) {
        	if (isNPPFile(f)) {  
        		return f.getName();
        	} else {
        		return null;
        	}
        }

        public String getDescription(File f) {
            return null; //let the L&F FileView figure this out
        }

        public Boolean isTraversable(File f) {
            return null; //let the L&F FileView figure this out
        }

        public String getTypeDescription(File f) {
            return "NPP Data Product";
        }

        public Icon getIcon(File f) {
            Icon icon = null;
            return icon;
        }
        
    }
    
    /* NPPFilter */
    public class NPPFilter extends FileFilter {

        // maintain an array of "seen" patterns, so we only identify data
    	// once for a particular type and time (instead of for each segment).
    	ArrayList<String> seenPatterns = new ArrayList<String>();
    	
    	String extraFilter = "";
    	
    	public NPPFilter(String extraFilter) {
    		super();
    		if (extraFilter != null) {
    			this.extraFilter = extraFilter;
    		}
    	}
    	
    	// Accept all directories and all NPP files.
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            if (isNPPFile(f)) {
            	return true;
            } else {
            	return false;
            }

        }

        // The description of this filter
        public String getDescription() {
            return "NPP Data";
        }
        
        // change the additional filter string
        public void setExtraFilter(String newFilter) {
        	if (newFilter != null) {
        		extraFilter = newFilter;
        		seenPatterns.clear();
        	}
        }
        
    }
        
    /**
     * An extension of JFileChooser
     *
     * @author Tommy Jasmin
     */
    
    public class NPPFileChooser extends JFileChooser {

        /**
		 * default for serializable class
		 */
		private static final long serialVersionUID = 1L;

		/**
         * Create the file chooser
         *
         * @param path   the initial path
         */
        public NPPFileChooser(String path) {
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
            NPPChooser.this.doLoad();
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

        return super.selectFilesInner(files, directory);
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
        
        String extraFilter = "";
        nppf = new NPPFilter(extraFilter);
        fileChooser.setFileFilter(nppf);

        return centerPanel;
    }
    
}