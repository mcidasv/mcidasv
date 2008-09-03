/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.chooser;

import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

import org.w3c.dom.Element;

import ucar.unidata.data.DataSourceResults;

import ucar.unidata.idv.*;

import ucar.unidata.idv.chooser.FileChooser;
import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.ui.ChooserPanel;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

public class HRITChooser extends IdvChooser {

	private static final long serialVersionUID = 1L;

	private Element chooserNode;

    private JFileChooser fileChooser;
    
    private HRITFilter hf = null;

    private IntegratedDataViewer idv = getIdv();

    /**
     *  The chooser xml can specify a datasourceid attribute.
     */
    private String dfltDataSourceId = "HRIT";

    /**
     * Create the chooser with the given manager and xml
     *
     * @param mgr The manager
     * @param root The xml
     *
     */
    public HRITChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        this.chooserNode = root;
    }


    /**
     * Get the tooltip for the load button
     *
     * @return The tooltip for the load button
     */
    protected String getLoadToolTip() {
        return "";
    }


    /**
     * Get the tooltip for the update button
     *
     * @return The tooltip for the update button
     */
    protected String getUpdateToolTip() {
        return "Rescan the directory";
    }


    /**
     * Make the GUI
     *
     * @return The GUI for HRIT Chooser
     */
    protected JComponent doMakeContents() {
        String path = (String) idv.getPreference(PREF_DEFAULTDIR + getId());
        if (path == null) {
            path = XmlUtil.getAttribute(this.chooserNode, FileChooser.ATTR_PATH,
                                        (String) null);
        }

        if (path == null) {
        	path = ".";
        }
        
        fileChooser = doMakeFileChooser(path);
        fileChooser.setPreferredSize(new Dimension(300, 300));
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setApproveButtonText(ChooserPanel.CMD_LOAD);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileView(new HRITFileView());
        
        // now we need to see what HRIT data is available in this directory
        Vector availableTypes = getAvailableHRITTypes(path);
        
        hf = new HRITFilter((String) availableTypes.get(0));
        fileChooser.setFileFilter(hf);
        ImageTypeChooser itc = new ImageTypeChooser(fileChooser, path);

        JPanel filePanel = GuiUtils.vbox(itc, getDefaultButtons());
        return filePanel;
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
    
    protected Vector getAvailableHRITTypes(String path) {
    	ArrayList al = new ArrayList();
    	Vector v = new Vector();
    	File f = new File(path);
    	File [] files = null;
    	if (f.isDirectory()) {
    		files = f.listFiles(
    			new java.io.FileFilter() {
    				public boolean accept(File f) {
    		            if ((f.getName().endsWith("__")) && (f.getName().contains("MSG2"))) {
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

    public class ImageTypeChooser extends JPanel implements ActionListener {
    	
        JFileChooser jfc = null;
        
    	public ImageTypeChooser(JFileChooser fc, String path) {
    		jfc = fc;
        	setLayout(new BorderLayout());
        	JPanel topPanel = new JPanel(new FlowLayout());
        	topPanel.add(new JLabel("Select a data channel: "));
            Vector availableTypes = getAvailableHRITTypes(path);
            JComboBox jcb = new JComboBox(availableTypes);
            jcb.addActionListener(this);
            topPanel.add(jcb);
            add(topPanel, BorderLayout.NORTH);
            add(fc, BorderLayout.CENTER);           
        }
        
        /** Listens to the combo box. */
        public void actionPerformed(ActionEvent e) {
            JComboBox cb = (JComboBox) e.getSource();
            String newFilter = (String) cb.getSelectedItem();
            HRITFilter hf = (HRITFilter) jfc.getFileFilter();
            hf.setExtraFilter(newFilter);
            jfc.rescanCurrentDirectory();
        }
        
    }
    
    /* HRITFileView aggregates user's view of segments */
    public class HRITFileView extends FileView {

        ArrayList foundFiles =  new ArrayList();
        
    	// show only the image time as file name for selection purposes
        public String getName(File f) {
        	if ((f.getName().contains("MSG2")) && (f.getName().length() >= 58)) {        	
        		return f.getName().substring(46, 58);
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
            return "HRIT Image";
        }

        public Icon getIcon(File f) {
            Icon icon = null;
            return icon;
        }
    }
    
    /* HRITFilter */
    public class HRITFilter extends FileFilter {

        // maintain an array of "seen" patterns, so we only identify data
    	// once for a particular type and time (instead of for each segment).
    	ArrayList seenPatterns = new ArrayList();
    	
    	String extraFilter = "";
    	
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

            // XXX TJJ - at present, we are ONLY allowing MSG2 segment data files
            // through the filter which have already bee Wavelet decompressed
            // (i.e., they end with __ and not C_ )
            String fileName = f.getName();
            if ((fileName.endsWith("__")) && (fileName.contains("MSG2")) && (fileName.contains(extraFilter))) {
            	String patternCheck = fileName.substring(46, 58);
            	if (seenPatterns.contains(patternCheck)) {
            		return false;
            	} else {
            		seenPatterns.add(patternCheck);
            		return true;
            	}
            } else {
            	return false;
            }

        }

        //The description of this filter
        public String getDescription() {
            return "HRIT Data";
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
        	for (int i = 0; i < selectedFiles.length; i++) {
                channelStr = selectedFiles[i].getName().substring(26, 32);
                timeStr = selectedFiles[i].getName().substring(46, 58);
        	}
        	File curDir = getCurrentDirectory();
        	File [] fileList = curDir.listFiles();
        	String tmpChannel = null;
        	String tmpTime = null;
        	ArrayList matches = new ArrayList();
        	for (int i = 0; i < fileList.length; i++) {
        		if ((fileList[i].getName().endsWith("__")) && 
        			(fileList[i].getName().contains("MSG2")) && 
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
            //super.setSelectedFiles(selectedFiles);
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
     */
    protected final void selectFiles(File[] files, File directory) {
        try {
            if (selectFilesInner(files, directory)) {
                idv.getStateManager().writePreference(PREF_DEFAULTDIR
                        + getId(), directory.getPath());
            }
        } catch (Exception excp) {
            logException("File selection", excp);
        }
    }

    /**
     * Get the file chooser
     *
     * @return  the chooser for this instance
     */
    protected JFileChooser getFileChooser() {
        return fileChooser;
    }

    /**
     * Override the base class method to catch the do load
     */
    public void doLoadInThread() {
        selectFiles(fileChooser.getSelectedFiles(),
                    fileChooser.getCurrentDirectory());
    }


    /**
     * Override the base class method to catch the do update
     */
    public void doUpdate() {
        fileChooser.rescanCurrentDirectory();
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
        
        FileManager.addToHistory(files[0]);
        List    selectedFiles      = new ArrayList();
        String  fileNotExistsError = "";
        boolean didXidv            = false;

        for (int i = 0; i < files.length; i++) {
            if ( !files[i].exists()) {
                fileNotExistsError += "File does not exist: " + files[i] + "\n";
            } else {
                String filename = files[i].toString();
                //Check for the bundle or jnlp file
                if (idv.getArgsManager().isXidvFile(filename)
                        || idv.getArgsManager().isZidvFile(filename)
                        || idv.getArgsManager().isJnlpFile(filename)) {
                    didXidv = idv.handleAction(filename, null);
                } else {
                    selectedFiles.add(filename);
                }
            }
        }

        if (didXidv) {
            closeChooser();
            return true;
        }

        if (selectedFiles.size() == 0) {
            return false;
        }

        if (fileNotExistsError.length() > 0) {
            userMessage(fileNotExistsError);
            return false;
        }

        Object definingObject = selectedFiles;
        if (selectedFiles.size() == 1) {
            definingObject = selectedFiles.get(0);
        }

        String dataSourceId = getDataSourceId();
        if (dataSourceId == null) {
            dataSourceId = dfltDataSourceId;
        }

        //If the user specifically selected a data source type then pass all files to that data source and be done.
        DataSourceResults results;
        if (dataSourceId == null) {
            //If they selected one directory then ask if they want to load all the files
            if (selectedFiles.size() == 1) {
                File file = new File(selectedFiles.get(0).toString());
                if (file.isDirectory()) {
                    if ( !GuiUtils.showYesNoDialog(null,
                            "Do you want to load all of the files in the selected directory: "
                            + file, "Directory Load")) {
                        return false;
                    }
                    selectedFiles  = new ArrayList();
                    definingObject = selectedFiles;
                    File[] subFiles = file.listFiles();
                    for (int i = 0; i < subFiles.length; i++) {
                        if ( !subFiles[i].isDirectory()) {
                            selectedFiles.add(subFiles[i].toString());
                        }
                    }
                }
            }
        }

        Hashtable   properties  = new Hashtable();
        boolean result = makeDataSource(definingObject, dataSourceId, properties);
        // System.err.println("makeDataSource returns: " + result + ", selected files: " + selectedFiles);
        return result;
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
        Vector v = new Vector();
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
    
}