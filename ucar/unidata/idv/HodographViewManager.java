/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2021
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

package ucar.unidata.idv;


import ucar.unidata.view.sounding.Hodograph3DDisplay;
import ucar.visad.display.AnimationInfo;
import ucar.visad.display.DisplayMaster;
import visad.VisADException;

import java.rmi.RemoteException;

import javax.swing.JMenu;



/**
 * A wrapper around a hodograph display
 * Provides an interface for managing user interactions, gui creation, etc.
 *
 * @author IDV development team
 */

public class HodographViewManager extends ViewManager {

    /** Prefix for preferences. */
    public static final String PREF_PREFIX = ViewManager.PREF_PREFIX
                                             + "HODOGRAPH";

    /**
     *  A paramterless ctor for XmlEncoder  based decoding.
     */
    public HodographViewManager() {}

    /**
     * Create a HodographViewManager with the given context,
     * descriptor, object store and properties string.
     *
     * @param viewContext  Provides a context for the VM to be in.
     * @param desc         The ViewDescriptor that identifies this VM
     * @param properties   A set of ";" delimited name-value pairs.
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    public HodographViewManager(ViewContext viewContext, ViewDescriptor desc,
                                String properties)
            throws VisADException, RemoteException {
        this(viewContext, desc, properties, null);
    }


    /**
     * Create a HodographViewManager with the given context, descriptor,
     * object store, properties string and animation state.
     *
     * @param viewContext Provides a context for the VM to be in.
     * @param desc The ViewDescriptor that identifies this VM
     * @param properties A set of ";" delimited name-value pairs.
     * @param animationInfo Initial animation properties
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    public HodographViewManager(ViewContext viewContext, ViewDescriptor desc,
                                String properties,
                                AnimationInfo animationInfo)
            throws VisADException, RemoteException {
        super(viewContext, desc, properties, animationInfo);
    }


    /**
     *  Create a HodographViewManager with the given context, display,
     *  descriptor, properties string.
     *
     * @param viewContext Provides a context for the VM to be in.
     * @param master  display master
     * @param viewDescriptor The ViewDescriptor that identifies this VM
     * @param properties A set of ";" delimited name-value pairs.
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    public HodographViewManager(ViewContext viewContext,
                                DisplayMaster master,
                                ViewDescriptor viewDescriptor,
                                String properties)
            throws VisADException, RemoteException {
        this(viewContext, viewDescriptor, properties, null);
        setDisplayMaster(master);
    }

    /**
     * Initialize the view menu
     *
     * @param viewMenu the view menu
     */
    public void initializeViewMenu(JMenu viewMenu) {
        showControlMenu = false;
        super.initializeViewMenu(viewMenu);
        viewMenu.add(makeColorMenu());
    }
    
    /**
     * Factory method for creating the display master.
     *
     * @return The Display Master
     * @throws VisADException On badness
     * @throws RemoteException On badness
     */
    protected DisplayMaster doMakeDisplayMaster()
            throws VisADException, RemoteException {
        Hodograph3DDisplay display = new Hodograph3DDisplay();
        return display;
    }

    /**
     * Set the hodograph display.
     *
     * @param hd  the hodograph display
     */
    public void setHodographDisplay(Hodograph3DDisplay hd) {
        setDisplayMaster(hd);
    }


    /**
     * Don't show the side legend.
     *
     * @return false
     */
    public boolean getShowSideLegend() {
        return false;
    }

    /**
     * What type of view is this.
     *
     * @return The type of view
     */
    public String getTypeName() {
        return "Hodograph View";
    }

    /**
     * Do we support animation?
     *
     * @return false
     */
    public boolean animationOk() {
        return false;
    }

}
