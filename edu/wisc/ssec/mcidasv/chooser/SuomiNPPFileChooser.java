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
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JLabel;

/**
 * An extension of JFileChooser, particular to Suomi NPP data.
 * Mostly that means check for presence of separate HDF5 geolocation
 * files when needed to bless granules for selection.
 *
 * @author Tommy Jasmin
 */

public class SuomiNPPFileChooser extends JFileChooser {

    /**
	 * default for serializable class
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * ref to caller
	 */
	private SuomiNPPChooser snppc = null;

	/**
     * Create the file chooser
     *
     * @param path   the initial path
     * @param snppc  the enclosing Suomi Chooser
     */
	
    public SuomiNPPFileChooser(String path, SuomiNPPChooser snppc) {
        super(path);
        this.snppc = snppc;
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
        snppc.doLoad();
    }

    /**
     * Set the selected files
     *
     * @param selectedFiles  the selected files
     */
    public void setSelectedFiles(File[] selectedFiles) {
    	super.setSelectedFiles(selectedFiles);
        snppc.setHaveData( !((selectedFiles == null) || (selectedFiles.length == 0)));
    }
}
