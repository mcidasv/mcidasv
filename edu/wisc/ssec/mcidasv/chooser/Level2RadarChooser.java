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


import org.w3c.dom.Element;

import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

import ucar.unidata.data.DataSource;

import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.radar.Level2RadarDataSource;

import ucar.unidata.idv.*;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.control.DisplayControlBase;

import ucar.unidata.metdata.*;

import ucar.unidata.ui.ChooserPanel;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlResourceCollection;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;

import javax.swing.filechooser.FileFilter;



/**
 * A chooser for Level II NEXRAD data. This loads in
 * files from the file system. Since (right now) the
 * data does not contain the station we rely on
 * the heuristic  of looking at the directory path
 * name to see if it contains a station name.
 * The user can also specify the station from the GUI
 *
 *
 * @author IDV development team
 * @version $Revision$Date: 2011/03/24 16:06:31 $
 */
public class Level2RadarChooser extends FileChooser {

    /** Holds the predefined list of nexrad stations */
    private JComboBox stationsCbx;

    /** List of predefined nexrad stations */
    private List nexradStations;

    /** Label used in the widgets to show an unknown station */
    private static String UNKNOWN_STATION = "I'm Feeling Lucky";

    /**
     * The data source id we pass the files to.
     * This is the oone defined in idv/resources/datasources.xml
     */
    private static String DATA_TYPE = "FILE.LEVEL2RADAR";

    /** the type for the CDM radar */
    private static String CDM_DATA_TYPE = "FILE.RADAR";

    /** checkbox for switching data types */
    private JCheckBox typeCbx;

    /**
     * Create the chooser with the given chooser manager
     * and xml root (from the xml that defines this chooser).
     *
     * @param mgr The manager
     * @param root The xml
     *
     */
    public Level2RadarChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }

    /**
     * Label for {@link #getDataSourcesComponent()} selector.
     *
     * @return {@code String} to use as the label for data sources selector.
     */
    protected String getDataSourcesLabel() {
    	return "Station:";
    }

    /**
     * Overridden so that McIDAS-V can attempt auto-selecting the default data
     * source type.
     */
    @Override protected JComboBox getDataSourcesComponent() {
    	stationsCbx = new JComboBox();
        List stations = Misc.newList(UNKNOWN_STATION);
        stations.addAll(nexradStations = getStations());
        DisplayControlBase.setStations(stations, stationsCbx, false);
        return stationsCbx;
    }
    
    /**
     * Get the tooltip for the load button
     *
     * @return The tooltip for the load button
     */
    protected String getLoadToolTip() {
        return "Load the selected Level II radar files";
    }

    /**
     * Make the file chooser
     *
     * @param path  the initial path
     *
     * @return the JFileChooser
     */
    protected JFileChooser doMakeFileChooser(String path) {
        MyFileChooser fileChooser = new Level2RadarFileChooser(this, path);
        fileChooser.addChoosableFileFilter(new PatternFileFilter(".*\\.raw$", "Raw files"));
//        fileChooser.setApproveButtonText(ChooserPanel.CMD_LOAD);
        fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
        if (path != null) {
            fileChooser.setCurrentDirectory(new File(path));
        }
        return fileChooser;
    }
    
    /**
     * Process the set of selected files
     *
     * @param files Array of files
     * @param directory The last directory  chosen
     *
     * @return true if successful
     */
    protected boolean selectFilesInner(File[] files, final File directory) {
        final Object selected =
            ((TwoFacedObject) stationsCbx.getSelectedItem()).getId();

        if (selected.equals(UNKNOWN_STATION)
                && ((typeCbx != null) && !typeCbx.isSelected())) {
            userMessage("Unknown location of selected files, "
                        + "please select from list");
            return false;
        }

        int recentCnt = getFileCount();
        if (recentCnt <= 0) {
            if ((files == null) || (files.length == 0)) {
                userMessage("Please select one or more files");
                return false;
            }
        }
        if ((files != null) && (files.length > 0)) {
            FileManager.addToHistory(files[0]);
        }

        String[] tmpDataLoc = getFileNames(((recentCnt <= 0)
                                            ? files
                                            : null));
        if (recentCnt <= 0) {
            if (tmpDataLoc == null) {
                return false;
            }
        }

        final Hashtable properties =
            Misc.newHashtable(Level2RadarDataSource.STATION_LOCATION,
                              selected);
        String pattern = getFilePattern();
        if ((pattern != null) && (pattern.length() > 0)) {
            properties.put(DataSource.PROP_FILEPATTERN,
                           pattern.toLowerCase());
        } else {
            pattern = null;
        }

        if (recentCnt > 0) {
            properties.put(DataSource.MOST_RECENT, new Integer(recentCnt));
            tmpDataLoc = new String[] { directory.toString() };
            PollingInfo pollingInfo = new PollingInfo(directory.toString(),
                                          60000, pattern, false, false);
            pollingInfo.setMode(PollingInfo.MODE_COUNT);
            pollingInfo.setFileCount(recentCnt);
            properties.put(DataSource.PROP_POLLINFO, pollingInfo);
        }

        String dataType = ((typeCbx != null) && !typeCbx.isSelected())
                          ? DATA_TYPE
                          : CDM_DATA_TYPE;
        // System.out.println("dataType = " + dataType);
        makeDataSource(tmpDataLoc, dataType, properties);
        return true;
    }

    /**
     * Read in the nexrad stations from the
     * idv/resources/nexradstns.xml resource
     *
     * @return List of of {@link ucar.unidata.metdata.NamedStation}-s
     */
    private List getStations() {
        if (nexradStations == null) {
            nexradStations = new Vector();
            List radarLocations =
                getIdv().getResourceManager().findLocationsByType("radar");
            for (int i = 0; i < radarLocations.size(); i++) {
                NamedStationTable nexrTable =
                    (NamedStationTable) radarLocations.get(i);
                nexradStations.addAll(nexrTable.values());
            }
            Collections.sort(nexradStations);
        }
        return nexradStations;
    }

    /**
     * Try to guess at the station of the selected
     * file based on directory name.
     *
     * @param file The selected file
     */
    protected void guessAtStation(File file) {

        if ((file == null) || !file.isDirectory()) {
            return;
        }
        if ((nexradStations == null) || nexradStations.isEmpty()) {
            return;
        }
        File tmpFile = file;

        //Walk up the directory tree, looking at the names of each file

        //Use the  dirLevel so we only do the println on the first check.
        //Though  we could use it to only check one or two directory levels
        int     dirLevel = 0;
        boolean found    = false;
        while ((tmpFile != null) && (found == false)) {
            String name = tmpFile.getName().toLowerCase();
            for (Iterator iter =
                    nexradStations.iterator(); iter.hasNext(); ) {
                NamedStation station = (NamedStation) iter.next();
                if (station == null) {
                    continue;
                }

                String id = station.getIdentifier();
                //Do a .equals - perhaps we do want to do the .indexOf check??
                //Though that might mean some odd matches.
                if (name.indexOf(id.toLowerCase()) >= 0) {
                    stationsCbx.setSelectedItem(
                        DisplayControlBase.createStationTfo(station));
                    found = true;
                    break;
                }
            }
            dirLevel++;
            tmpFile = tmpFile.getParentFile();
        }
        if ( !found) {
            stationsCbx.setSelectedItem(UNKNOWN_STATION);
        }
    }

    /**
     * This class allows us to add in our own functionality
     * to the file chooser. It has a hook to support the guessing
     * of the station from the directory name and passes through
     * to the chooser the select and cancel events
     *
     * @author IDV development team
     */
    public class Level2RadarFileChooser extends FileChooser.MyFileChooser {

        /** my chooser */
        Level2RadarChooser myChooser;

        /** Keeps track of the last directory the user chose */
        File lastDirectory = null;

        /**
         * Create the special file chooser
         *
         *
         * @param chooser the chooser to relate to
         * @param path  path to start with
         */
        public Level2RadarFileChooser(Level2RadarChooser chooser,
                                      String path) {
            super(path);
            myChooser = chooser;
        }

        /**
         * Try to guess at the  station name
         *
         * @param file The currently selected dir
         */
        public void setCurrentDirectory(File file) {
            super.setCurrentDirectory(file);
            if ( !Misc.equals(file, lastDirectory)) {
                if (myChooser != null) {
                    myChooser.guessAtStation(file);
                }
                lastDirectory = file;
            }
        }
    }

    /**
     * Get the bottom panel for the chooser
     * @return the bottom panel
     */
    protected JPanel getBottomPanel() {       
        // do this because the original check is made before the list is inited
        if (getFileChooser() != null) {
            guessAtStation(getFileChooser().getCurrentDirectory());
        }
        JComponent recentComponent = getRecentFilesComponent();
    	Component [] components = recentComponent.getComponents();
    	if (components != null) {
    		for (int i = 0; i < components.length; i++) {
    			if (components[i] instanceof JLabel) {
    				McVGuiUtils.setComponentWidth((JLabel)components[i], McVGuiUtils.Width.SINGLE);
    				McVGuiUtils.setLabelPosition((JLabel)components[i], McVGuiUtils.Position.RIGHT);
    			}
    			else if (components[i] instanceof JComboBox) {
    				McVGuiUtils.setComponentWidth((JComboBox)components[i], McVGuiUtils.Width.DOUBLE);
    			}
    			else if (components[i] instanceof JTextField) {
    				McVGuiUtils.setComponentWidth((JTextField)components[i], McVGuiUtils.Width.SINGLE);
    			}
    		}
    		recentComponent = GuiUtils.left(GuiUtils.hbox(components));
    	}
    	return McVGuiUtils.makeLabeledComponent("Times:", recentComponent);
    }
    
}

