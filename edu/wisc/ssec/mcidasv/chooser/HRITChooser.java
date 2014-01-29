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

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.GuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

public class HRITChooser extends FileChooser {

	private static final long serialVersionUID = 1L;
    
    private HRITFilter hf = null;

    private JLabel channelLabel = McVGuiUtils.makeLabelRight("Data Channel:");

    /**
     * Create the chooser with the given manager and xml
     *
     * @param mgr The manager
     * @param root The xml
     *
     */
    
    public HRITChooser(IdvChooserManager mgr, Element root) {
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
        return new HRITFileChooser(path);
    }
    
    protected Vector<String> getAvailableHRITTypes(String path) {
        if (path == null) path = ".";
        
    	ArrayList<String> al = new ArrayList<String>();
    	Vector<String> v = new Vector<String>();
    	File f = new File(path);
    	File [] files = null;
    	if (f.isDirectory()) {
    		files = f.listFiles(
    			new java.io.FileFilter() {
    				public boolean accept(File f) {
    		            if ((f.getName().endsWith("__")) && (f.getName().matches(".*MSG[2-3].*"))) {
    		            	return true;
    		            } else {
    		            	return false;
    		            }
    				}
    			}
    		);
    	}
    	if (files != null) {
    		for (int i = 0; i < files.length; i++) {
    	        String channelStr = files[i].getName().substring(26, 32);
    	        if (channelStr.equals("______")) continue;
    	        channelStr = channelStr.replace("___", "");
    	        if (! al.contains(channelStr)) {
    	        	al.add(channelStr);
    	        }
    		}
    	}
    	Collections.sort(al);
    	for (int i = 0; i < al.size(); i++) {
    		v.add(al.get(i));
    	}
    	return v;
    }
    
    public class ImageTypeChooser extends JComboBox implements ActionListener, PropertyChangeListener {
    	
		private static final long serialVersionUID = 1L;
		JFileChooser jfc = null;
        
    	public ImageTypeChooser(JFileChooser fc, String path) {
    		jfc = fc;
            Vector<String> availableTypes = getAvailableHRITTypes(path);
            if (availableTypes.size() == 1 && availableTypes.get(0) == ".") {
            	availableTypes.removeAllElements();
            }
            reloadComboBox(availableTypes);
            addActionListener(this);
            McVGuiUtils.setComponentWidth(this, McVGuiUtils.Width.DOUBLE);
        }
        
        public void actionPerformed(ActionEvent e) {
            JComboBox cb = (JComboBox) e.getSource();
            String newFilter = (String) cb.getSelectedItem();
            HRITFilter hFilter = (HRITFilter) jfc.getFileFilter();
            hFilter.setExtraFilter(newFilter);
            jfc.rescanCurrentDirectory();
        }
        
        public void reloadComboBox(Vector<String> v) {
        	removeAllItems();
        	if (v != null) {
        		for (int i = 0; i < v.size(); i++) {
        			addItem(v.get(i));
        		}
        	}
        	if (v == null || v.size() == 0) {
        		setEnabled(false);
        		channelLabel.setEnabled(false);
        	}
        	else {
        		setEnabled(true);
        		channelLabel.setEnabled(true);
        	}
        }
        
        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();

            // If the directory changed, reload the combo box with new image type choices.
            if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
            	Vector<String> availableTypes = getAvailableHRITTypes(jfc.getCurrentDirectory().getPath());
            	reloadComboBox(availableTypes);
            }

        }
        
    }
    
    /* HRITFilter */
    public class HRITFilter extends FileFilter {
    	
    	String extraFilter = null;
    	
    	public HRITFilter(String extraFilter) {
    		super();
    		if (extraFilter != null) {
    			this.extraFilter = extraFilter;
    		}
    	}
    	
    	// Accept all directories and all HRIT files.
        public boolean accept(File f) {
        	
            if (f.isDirectory()) {
                return true;
            }

            // XXX TJJ - at present, we are ONLY allowing MSG2 and MSG3 segment data files
            // through the filter which have already been Wavelet decompressed
            // (i.e., they end with __ and not C_ )
            String fileName = f.getName();
            if ((fileName.endsWith("__")) && (fileName.matches(".*MSG[2-3].*")) && (fileName.length() >= 58)) {
            	if (extraFilter != null) {
            		if (fileName.contains(extraFilter)) {
            			return true;
            		} else {
            			return false;
            		}
            	}
            } else {
            	return false;
            }
            return false;

        }

        // The description of this filter
        public String getDescription() {
            return "HRIT Data";
        }
        
        // change the additional filter string
        public void setExtraFilter(String newFilter) {
        	if (newFilter != null) {
        		extraFilter = newFilter;
        		//seenPatterns.clear();
        	}
        }
        
    }
        
    /**
     * HRITFileChooser, an extension of JFileChooser
     *
     * @author Tommy Jasmin
     */
    
    public class HRITFileChooser extends JFileChooser {

        /**
		 * default for serializable class
		 */
		private static final long serialVersionUID = 1L;

		/**
         * Create the file chooser
         *
         * @param path   the initial path
         */
		
        public HRITFileChooser(String path) {
            super(path);
            setControlButtonsAreShown(false);
            setMultiSelectionEnabled(false);
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
            HRITChooser.this.doLoad();
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
        	String channelStr = null;
        	String timeStr = null;
        	if (selectedFiles != null) {
        		for (int i = 0; i < selectedFiles.length; i++) {
        			if (! selectedFiles[i].isDirectory()) {
        				if (selectedFiles[i].getName().length() >= 58) {
        					channelStr = selectedFiles[i].getName().substring(26, 32);
        					timeStr = selectedFiles[i].getName().substring(46, 58);
        				}
        			}
        		}
        	}
        	File curDir = getCurrentDirectory();
        	File [] fileList = curDir.listFiles();
        	String tmpChannel = null;
        	String tmpTime = null;
        	ArrayList<File> matches = new ArrayList<File>();
        	for (int i = 0; i < fileList.length; i++) {
        		if ((fileList[i].getName().endsWith("__")) && 
        			(fileList[i].getName().matches(".*MSG[2-3].*")) && 
        			(fileList[i].getName().length() >= 58)) {
        			tmpChannel = fileList[i].getName().substring(26, 32);
        			tmpTime = fileList[i].getName().substring(46, 58);
        			if ((tmpChannel.equals(channelStr)) && (tmpTime.equals(timeStr))) {
        				matches.add(fileList[i]);
        			}
        		}
        	}
        	Collections.sort(matches);
        	
        	// make new file array from ArrayList matches
        	File [] fileSet = new File[matches.size()];
        	for (int i = 0; i < matches.size(); i++) {
        		fileSet[i] = (File) matches.get(i);
        	}

        	super.setSelectedFiles(fileSet);
            setHaveData( !((selectedFiles == null)
                           || (selectedFiles.length == 0)));
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
        
        // only allow selection of files that make sense as a "set"
        // for now, that means all part of single image for a single channel
        String channelStr = files[0].getName().substring(26, 32);
        String timeStr = files[0].getName().substring(46, 58);
        int prvSegment = -1;
        int curSegment = -1;
        for (int i = 0; i < files.length; i++) {
        	try {
        		curSegment = Integer.parseInt(files[i].getName().substring(40, 42));
        	} catch (NumberFormatException nfe) {
        		userMessage("Problem determining image segment number for file: " + files[i].getName());
        		return false;
        	}
        	if (!files[i].getName().substring(26, 32).equals(channelStr)) {
        		userMessage("Selected data must be for a single channel and time");
        		return false;
        	}
        	if (!files[i].getName().substring(46, 58).equals(timeStr)) {
        		userMessage("Selected data must be for a single channel and time");
        		return false;
        	}
        	if (prvSegment >= 0) {
        		if (curSegment != (prvSegment + 1)) {
            		userMessage("Selected data must be a contiguous set of image segment files");
            		return false;
        		}
        	}
        	prvSegment = curSegment;
        }
        
        // XXX TJJ - until HRITAdapter can handle the fact that HRV data can 
        // alter geographic coverage in mid-transmission, we deal with this by
        // only allowing display of HRV segments one at a time.
        
        // commenting out for now - will deal with it in field selector window
/*        if (files.length > 1) {
        	int hrvCount = 0;
        	for (int i = 0; i < files.length; i++) {
        		if (files[i].getName().contains("HRV")) {
        			hrvCount++;
        		} 
        		if (hrvCount > 1) {
        			userMessage("At present, HRV data can only be displayed one file at a time");
        			return false;
        		}       		
        	}
        }*/

        return super.selectFilesInner(files, directory);
    }
    
    /**
     * Get the bottom panel for the chooser
     * @return the bottom panel
     */
    
    protected JPanel getBottomPanel() {
    	// If we don't have a fileChooser yet, this won't do any good
    	// This happens when Unidata's FileChooser is instantiated
    	// We instantiate ours right after that
    	if (fileChooser == null) {
    		return null;
    	}
    	
        ImageTypeChooser itc = new ImageTypeChooser(fileChooser, path);
        fileChooser.addPropertyChangeListener(itc);
        JPanel bottomPanel = GuiUtils.left(itc);
        bottomPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        
        return McVGuiUtils.makeLabeledComponent(channelLabel, itc);
    }
    
    /**
     * Get the center panel for the chooser
     * @return the center panel
     */
    
    protected JPanel getCenterPanel() {
    	JPanel centerPanel = super.getCenterPanel();
    	
        fileChooser.setAcceptAllFileFilterUsed(false);
      
        // see what HRIT data is available in this directory,
        Vector<String> availableTypes = getAvailableHRITTypes(path);
        String extraFilter = null;
        if ((availableTypes != null) && (availableTypes.size() > 0)) {
        	extraFilter = (String) availableTypes.get(0);
        }
        
        hf = new HRITFilter(extraFilter);
        fileChooser.setFileFilter(hf);

        return centerPanel;
    }
    
}
