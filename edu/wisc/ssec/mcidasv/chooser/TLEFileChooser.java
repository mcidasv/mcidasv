/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2016
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
import javax.swing.JList;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.chooser.IdvChooser;

/**
 * An extension of JFileChooser to handle Two-Line Element (TLE)
 * files, for plotting satellite orbit tracks.
 * 
 * @author Gail Dengel and Tommy Jasmin
 *
 */
public class TLEFileChooser extends JFileChooser implements PropertyChangeListener {

    /**
     * auto-generated default value
     */
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(TLEFileChooser.class);

    /* the enclosing orbit track chooser */
    private PolarOrbitTrackChooser potc = null;

    /**
     * Create the file chooser
     *
     * @param chooser {@code PolarOrbitTrackChooser} to which this {@code TLEFileChooser} belongs.
     * @param directory Initial directory.
     * @param filename Initial filename within {@code directory}.
     */
    public TLEFileChooser(PolarOrbitTrackChooser chooser, String directory, String filename) {
        super(directory);
        potc = chooser;

        logger.debug("TLEFileChooser constructor...");
        setControlButtonsAreShown(false);
        setMultiSelectionEnabled(false);
        FileFilter filter = new FileNameExtensionFilter("TLE files", "txt");
        addChoosableFileFilter(filter);
        setAcceptAllFileFilterUsed(false);
        setFileFilter(filter);
        addPropertyChangeListener(this);

        File tmpFile = new File(directory + File.separatorChar + filename);
//        logger.trace("tmpFile='{}' exists='{}'", tmpFile, tmpFile.exists());
        setSelectedFile(null);
        setSelectedFile(tmpFile);
//        final JList list = McVGuiUtils.getDescendantOfType(JList.class, this, "Enabled", true);
//        list.requestFocus();
    }

    @Override public void setSelectedFile(File file) {
        // i REALLY don't know how to explain this one...but don't remove the
        // following if-else stuff. at least on OSX, it has *something* to do with
        // whether or not the UI actually shows the file selection.
        // what is somewhat weird is that commenting out the current if-else
        // and doing something like:
        // if (file != null) {
        //     boolean weird = file.exists();
        // }
        // does *NOT* work--but maybe HotSpot is optimizing away the unused code, right?
        // wrong! the following also does not work:
        // if (file != null && file.exists()) {
        //    logger.trace("exists!");
        // }
        // i will note that calls to this method appear to be happening on threads
        // other than the EDT...but using SwingUtilities.invokeLater and
        // SwingUtilities.invokeAndWait have not worked so far (and I've tried
        // the obvious places in the code, including POTC.doMakeContents()).
        if (file != null) {
            logger.trace("setting file='{}' exists={}", file, file.exists());
        } else {
            logger.trace("setting file='{}' exists=NULL", file);
        }
        super.setSelectedFile(file);
    }

    /**
     * Approve the selection
     */
    @Override public void approveSelection() {
        logger.trace("firing");
        super.approveSelection();
        potc.doLoad();
    }

    public void setPotc(PolarOrbitTrackChooser potc) {
        this.potc = potc;
    }

    public PolarOrbitTrackChooser getPotc() {
        return potc;
    }

    @Override public void propertyChange(PropertyChangeEvent pce) {
        String propName = pce.getPropertyName();
        if (propName.equals(SELECTED_FILE_CHANGED_PROPERTY)) {
            // tell the chooser we have a file to load
            handleFileChanged();
        }
    }

    protected void handleFileChanged() {
        if (potc != null) {
            File f = getSelectedFile();
            if ((f != null) && accept(f) && potc.localMode()) {
                if (!f.isDirectory()) {
                    // update last visited directory here
                    String potcId = IdvChooser.PREF_DEFAULTDIR + potc.getId();
                    String potcFileId = IdvChooser.PREF_DEFAULTDIR + potc.getId() + ".file";
                    String dir = getSelectedFile().getParent();
                    String file = getSelectedFile().getName();
                    potc.getIdv().getStateManager().writePreference(
                        potcId, dir
                    );
                    potc.getIdv().getStateManager().writePreference(
                        potcFileId, file
                    );

                    logger.trace("potcId='{}' value='{}'", potcId, dir);
                    logger.trace("potcFileId='{}' value='{}'", potcFileId, file);
                    potc.enableLoadFromFile(true);
                }
            } else {
                potc.enableLoadFromFile(false);
            }
        } else {
            logger.warn("null potc, must be set by caller before use.");
        }
    }
}
