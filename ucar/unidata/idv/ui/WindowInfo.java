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

package ucar.unidata.idv.ui;


import ucar.unidata.util.ColorTable;
import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;

import visad.Unit;

import java.awt.*;

import java.util.ArrayList;
import java.util.Hashtable;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds information about an IdvWindow so we can persist it.
 */
public class WindowInfo {

    private static final Logger logger = LoggerFactory.getLogger(WindowInfo.class);

    /** The view managers in the window */
    private List viewManagers;

    /** The xml skin path */
    private String skinPath;

    /** The window bounds */
    private Rectangle bounds;

    /** Is this window one of the main windows */
    private boolean isAMainWindow = false;

    /** Window title to save */
    private String title;

    /** _more_          */
    private Hashtable persistentComponents = new Hashtable();

    /**
     * Ctor
     */
    public WindowInfo() {}

    /**
     * Create me and instantiate my state form the given window
     *
     * @param window The window to get state from
     */
    public WindowInfo(IdvWindow window) {
        if (window.getViewManagers() != null) {
            this.viewManagers = new ArrayList(window.getViewManagers());
        }
        this.persistentComponents = window.getPersistentComponents();
        skinPath                  = window.getSkinPath();
        bounds                    = window.getBounds();
        isAMainWindow             = window.getIsAMainWindow();
        this.title                = window.getTitle();
    }



    /**
     * to string
     *
     * @return to string
     */
    public String toString() {
        return "WindowInfo:" + skinPath;
    }

    /**
     * Get the list of view managers in the window
     *
     * @return The viewmanagers
     */
    public List getViewManagers() {
        return viewManagers;
    }

    /**
     * Set the list of view managers in the window
     *
     * @param vms  The view managers
     */
    public void setViewManagers(List vms) {
        viewManagers = vms;
    }




    /**
     * Get the window bounds
     *
     * @return Window bounds
     */
    public synchronized Rectangle getBounds() {
        logger.trace("return bounds={}", bounds);
        return bounds;
    }

    /**
     * Set the window bounds
     *
     * @param b The window bounds
     */
    public synchronized void setBounds(Rectangle b) {
        logger.trace("setting bounds={} old={}", b, bounds);
        bounds = b;
    }

    /**
     * Get the path to the xml skin
     *
     * @return Xml skin path
     */
    public String getSkinPath() {
        return skinPath;
    }

    /**
     * Set the path to the xml skin
     *
     * @param b Xml skin path
     */
    public void setSkinPath(String b) {
        skinPath = b;
    }

    /**
     * Set the IsAMainWindow property.
     *
     * @param value The new value for IsAMainWindow
     */
    public void setIsAMainWindow(boolean value) {
        isAMainWindow = value;
    }

    /**
     * Get the IsAMainWindow property.
     *
     * @return The IsAMainWindow
     */
    public boolean getIsAMainWindow() {
        return isAMainWindow;
    }

    /**
     * Set the Title property.
     *
     * @param value The new value for Title
     */
    public void setTitle(String value) {
        title = value;
    }

    /**
     * Get the Title property.
     *
     * @return The Title
     */
    public String getTitle() {
        return title;
    }


    /**
     * Set the PersistentComponents property.
     *
     * @param value The new value for PersistentComponents
     */
    public void setPersistentComponents(Hashtable value) {
        persistentComponents = value;
    }

    /**
     * Get the PersistentComponents property.
     *
     * @return The PersistentComponents
     */
    public Hashtable getPersistentComponents() {
        return persistentComponents;
    }



}
