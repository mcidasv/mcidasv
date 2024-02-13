/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
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


package ucar.unidata.ui.symbol;


import ucar.unidata.ui.drawing.*;
import ucar.unidata.util.GuiUtils;



import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;

import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.ResourceManager;

import ucar.unidata.xml.*;



import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;

import javax.swing.filechooser.FileFilter;


/**
 *  This manages the set of station models. It provides a wrapper
 *  around a StationModelCan
 */

public class StationModelManager extends ResourceManager {

    /** The file pattern for selecting station model files */
    public static final PatternFileFilter FILTER_ISM =
        new PatternFileFilter(".+\\.ism", "IDV station model file (*.ism)",
                              ".ism");

    /** Name used for default {@literal "blank canvas"} layout model. */
    public static final String NEW_LAYOUT_MODEL = "New Layout Model";

    /** The canvas for drawing. This does most of the work. */
    StationModelCanvas smc;

    /** The window */
    JFrame frame;

    XmlObjectStore store;

    /** This defines where we get the symbol definition xml files */
    protected XmlResourceCollection symbolTypes;



    /**
     * ctor
     *
     */
    public StationModelManager() {}


    /**
     * ctor
     *
     * @param encoder The XML encoder to use
     */
    public StationModelManager(XmlEncoder encoder) {
        super(encoder);
    }


    /**
     * Initialize.
     *
     * @param symbolTypes  This defines where we get the symbol definition xml files
     * @param stationModels Where we get the station model files
     */
    public void init(XmlObjectStore store, XmlResourceCollection symbolTypes,
                     ResourceCollection stationModels) {
        this.store = store;
        this.symbolTypes = symbolTypes;
        init(stationModels);
    }


    /**
     * Add the station model into the list of user SMS
     *
     * @param stationModel Station model to add
     */
    public void addStationModel(StationModel stationModel) {
        addUsers(stationModel);
    }

    /**
     * Get the file filters to use for selecting files.
     * @return List of file filters
     */
    public List getFileFilters() {
        return Misc.newList(FILTER_ISM);
    }


    /**
     * Override the ResourceManager method to get the title to use
     * @return The title
     */
    public String getTitle() {
        return "Layout Model";
    }



    /**
     * Show the editor window
     */
    public void show() {
        show((StationModel) null);
    }


    /**
     * Show the editor dialog for the named station model
     *
     * @param modelName Station model
     */
    public void show(String modelName) {
        show(getStationModel(modelName));
    }


    /**
     *  Check if there has been a change to the currently edited station model.
     *  If there is ask the user if they want to save it. If they cancel then don't close
     *  the window.
     *
     *  @return true if it is ok to close, false if the user hits cancel
     */
    public boolean checkCloseWindow() {
        if ((frame == null) || (smc == null)) {
            return true;
        }
        if ( !smc.okToChange()) {
            return false;
        }
        smc.closeDialogs();
        //Use a tmp so we can set the current frame to null
        JFrame tmp = frame;
        frame = null;
        smc   = null;
        tmp.dispose();
        return true;
    }


    /**
     * Show the editor dialog for the  station model. This may be null.
     *
     * @param initModel Initial model to show. May be null.
     */
    public void show(StationModel initModel) {
        if (frame == null) {
            frame = GuiUtils.createFrame(GuiUtils.getApplicationTitle() +"Layout Model Editor");
            frame.setDefaultCloseOperation(
                WindowConstants.DO_NOTHING_ON_CLOSE);
            smc = new StationModelCanvas(this, frame) {
                protected void doClose() {
                    checkCloseWindow();
                }
            };

            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    checkCloseWindow();
                }
            });
            smc.setPreferredSize(new Dimension(250, 250));
	    //	    GuiUtils.decorateFrame(frame, smc.getMenuBar());
	    frame.setJMenuBar(smc.getMenuBar());
            GuiUtils.packWindow(frame, smc.getContents(), true);
        }
        if (initModel != null) {
            smc.setStationModel(initModel);
        }
        frame.setVisible(true);
    }

    /**
     * Get the frame.
     * @return The frame
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Get the default station model.
     * @return Default station model.
     */
    public StationModel getDefaultStationModel() {
        StationModel dflt = (StationModel) getDefault();
        return dflt;
    }

    /**
     *  Return the currently station model being edited.
     *
     *  @return the station model being edited
     */
    public StationModel getStationModel() {
        if (smc == null) {
            return null;
        }
        return smc.getStationModel();
    }


    /**
     *  Return the name of currently station model being edited.
     *
     *  @return the station model name being edited
     */
    public String getStationModelName() {
        StationModel sm = smc.getStationModel();
        return ((sm != null)
                ? sm.getName()
                : null);
    }


    /**
     * Get the station model to use for selected locations
     *
     * @return selected station model
     */
    public StationModel getSelectedStationModel() {
        return getStationModel("Selected");
    }


    /**
     * Get the named station model
     *
     * @param name station model name
     * @return The station model or null if none found.
     */
    public StationModel getStationModel(String name) {
        if (name == null || name.equals("")) {
            return null;
        }
        StationModel stationModel = (StationModel) getObject(name);
        //Check for old station models
        if ((stationModel == null) && (name.indexOf(">") < 0)) {
            name = ">" + name;
            for (int i = 0; i < resources.size(); i++) {
                String modelName = resources.get(i).toString();
                if (modelName.endsWith(name)) {
                    return (StationModel) resources.get(i);
                }
            }
        }
        return stationModel;
    }

    /**
     * Get the list of all station models.
     * @return All station models
     */
    public List getStationModels() {
        return getResources();
    }

    /**
     * test
     *
     * @param args cmd line args
     */
    public static void main(String[] args) {
        List files = new ArrayList();
        for (int i = 0; i < args.length; i++) {
            files.add(args[i]);
        }
        ResourceCollection rc = new ResourceCollection("station models",
                                    files);
        StationModelManager smm = new StationModelManager();
        smm.init(rc);
        smm.show();
    }



}

