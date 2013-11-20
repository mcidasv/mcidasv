/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2013
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An extension of JFileChooser to handle Two-Line Element (TLE)
 * files, for plotting satellite orbit tracks.
 * 
 * @author tommyj
 *
 */

public class TLEFileChooser extends JFileChooser implements PropertyChangeListener {

	/**
	 * auto-generated default value
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(TLEFileChooser.class);
	
	// the enclosing orbit track chooser
	private PolarOrbitTrackChooser potc = null;
	
	/**
     * Create the file chooser
     *
     * @param path   the initial path
     */
	
    public TLEFileChooser(String path) {
        super(path);
        setControlButtonsAreShown(false);
        setMultiSelectionEnabled(false);
        FileFilter filter = new FileNameExtensionFilter("TLE files", "txt");
        addChoosableFileFilter(filter);
        setAcceptAllFileFilterUsed(false);
        setFileFilter(filter);
        addPropertyChangeListener(this);
        logger.debug("TLEFileChooser constructor...");
    }

    /**
     * Approve the selection
     */
    
    public void approveSelection() {
        potc.doLoad();
    }

	public void setPotc(PolarOrbitTrackChooser potc) {
		this.potc = potc;
	}

	public PolarOrbitTrackChooser getPotc() {
		return potc;
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		String propName = pce.getPropertyName();
		if (propName.equals(SELECTED_FILE_CHANGED_PROPERTY)) {
			// tell the chooser we have a file to load
			if (potc != null) {
				File f = getSelectedFile();
				if ((f != null) && (accept(f))) {
					if (! f.isDirectory()) {
						potc.enableFileLoad(true);
					}
				} else {
					potc.enableFileLoad(false);
				}
			} else {
				logger.debug("null potc, wt?");
			}
		}
	}

}
