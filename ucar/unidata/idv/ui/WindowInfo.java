/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.idv.ui;

import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds information about an IdvWindow so we can persist it.
 */
public class WindowInfo {

    /** The view managers in the window */
    @SuppressWarnings("rawtypes")
    private List viewManagers;

    /** The xml skin path */
    private String skinPath;

    /** The window bounds */
    private Rectangle bounds;

    /** Is this window one of the main windows */
    private boolean isAMainWindow;

    /** Window title to save */
    private String title;

    /** _more_ */
    @SuppressWarnings("rawtypes")
    private Map persistentComponents;

    /**
     * Ctor
     */
    public WindowInfo() {}

    /**
     * Create me and instantiate my state form the given window
     *
     * @param window The window to get state from
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public WindowInfo(final IdvWindow window) {
        if (window.getViewManagers() != null) {
            this.viewManagers = new ArrayList(window.getViewManagers());
        }
        this.persistentComponents = new HashMap(window.getPersistentComponents());
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
        return String.format("[Window%x: title='%s', skinPath='%s', isAMainWindow=%s, bounds=%s, viewManagers=%s, persistentComponents=%s]", 
            hashCode(), title, skinPath, isAMainWindow, bounds, viewManagers, persistentComponents);
    }

    /**
     * Get the list of view managers in the window
     *
     * @return The viewmanagers
     */
    @SuppressWarnings("rawtypes")
    public List getViewManagers() {
        return viewManagers;
    }

    /**
     * Set the list of view managers in the window
     *
     * @param vms The view managers
     */
    @SuppressWarnings("rawtypes")
    public void setViewManagers(final List vms) {
        viewManagers = vms;
    }

    /**
     * Get the window bounds
     *
     * @return Window bounds
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Set the window bounds
     *
     * @param b The window bounds
     */
    public void setBounds(Rectangle b) {
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
    public void setSkinPath(final String b) {
        skinPath = b;
    }

    /**
     * Set the IsAMainWindow property.
     *
     * @param value The new value for IsAMainWindow
     */
    public void setIsAMainWindow(final boolean value) {
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
    public void setTitle(final String value) {
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
    @SuppressWarnings("rawtypes")
    public void setPersistentComponents(final Map value) {
        persistentComponents = value;
    }

    /**
     * Get the PersistentComponents property.
     *
     * @return The PersistentComponents
     */
    @SuppressWarnings("rawtypes")
    public Map getPersistentComponents() {
        return persistentComponents;
    }
}

