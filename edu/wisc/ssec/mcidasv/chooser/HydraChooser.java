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


import edu.wisc.ssec.mcidasv.*;

import java.awt.*;
import java.awt.event.*;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;

import org.w3c.dom.Element;

import ucar.unidata.data.DataSourceResults;

import ucar.unidata.idv.*;

import ucar.unidata.idv.chooser.FileChooser;
import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.ui.ChooserPanel;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;


public class HydraChooser extends IdvChooser {

    private Element chooserNode;

    private JFileChooser fileChooser;

    private IntegratedDataViewer idv = getIdv();

    /**
     *  The chooser xml can specify a datasourceid attribute.
     */
    private String dfltDataSourceId = "HYDRA";

    /**
     * Create the chooser with the given manager and xml
     *
     * @param mgr The manager
     * @param root The xml
     *
     */
    public HydraChooser(IdvChooserManager mgr, Element root) {
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
     * @return The GUI for HYDRA Chooser
     */
    protected JComponent doMakeContents() {
        String path = (String) idv.getPreference(PREF_DEFAULTDIR + getId());
        if (path == null) {
            path = XmlUtil.getAttribute(this.chooserNode, FileChooser.ATTR_PATH,
                                        (String) null);
        }
        //System.out.println("path=" + path);
        fileChooser = doMakeFileChooser(path);
        fileChooser.setPreferredSize(new Dimension(300, 300));
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setApproveButtonText(ChooserPanel.CMD_LOAD);

        PatternFileFilter ff = new PatternFileFilter(".hdf", "*.hdf files");
        //fileChooser.setFileFilter(ff);

        JComponent chooserPanel = fileChooser;
        JPanel filePanel = GuiUtils.vbox(chooserPanel, getDefaultButtons());
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
        return new MyFileChooser(path);
    }

    /**
     * An extension of JFileChooser
     *
     * @author IDV Development Team
     * @version $Revision$
     */
    public class MyFileChooser extends JFileChooser {

        /**
         * Create the file chooser
         *
         * @param path   the initial path
         */
        public MyFileChooser(String path) {
            super(path);
            setControlButtonsAreShown(false);
            setMultiSelectionEnabled(true);
        }

        /**
         * Approve the selection
         */
        public void approveSelection() {
            HydraChooser.this.doLoad();
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
        FileManager.addToHistory(files[0]);
        List    selectedFiles      = new ArrayList();
        String  fileNotExistsError = "";
        boolean didXidv            = false;

        for (int i = 0; i < files.length; i++) {
            if ( !files[i].exists()) {
                fileNotExistsError += "File does not exist: " + files[i]
                                      + "\n";
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
        return makeDataSource(definingObject, dataSourceId, properties);
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
        Vector v                  = new Vector();
        String fileNotExistsError = "";

        // NOTE:  If multiple files are selected, then missing files
        // are not in the files array.  If one file is selected and
        // it is not there, then it is in the array and file.exists()
        // is false
        for (int i = 0; i < files.length; i++) {
            if ((files[i] != null) && !files[i].isDirectory()) {
                if ( !files[i].exists()) {
                    fileNotExistsError += "File does not exist: " + files[i]
                                          + "\n";
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
