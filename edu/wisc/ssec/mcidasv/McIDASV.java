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

package edu.wisc.ssec.mcidasv;

import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ucar.unidata.data.DataManager;
import ucar.unidata.idv.ArgsManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.PluginManager;
import ucar.unidata.idv.VMManager;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import visad.VisADException;
import edu.wisc.ssec.mcidasv.chooser.McIdasChooserManager;
import edu.wisc.ssec.mcidasv.data.McvDataManager;
import edu.wisc.ssec.mcidasv.ui.McIdasColorTableManager;
import edu.wisc.ssec.mcidasv.ui.UIManager;

@SuppressWarnings("unchecked")
public class McIDASV extends IntegratedDataViewer {

	static {
        // FIXME: there may be a better place to do this
        try {
        	Properties scrubStrings = new Properties();
        	InputStream in = IOUtil.getInputStream(Constants.SCRUB_STRINGS_FILE, McIDASV.class);
        	if (in != null) {
        		scrubStrings.loadFromXML(in);
				LogUtil.setApplicationScrubStrings((Map)scrubStrings);
        	}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Set to true only if "-forceaqua" was found in the command line. */
	public static boolean useAquaLookAndFeel = false;

    /** Points to the adde image defaults. */
    public static final IdvResourceManager.XmlIdvResource RSC_FRAMEDEFAULTS =
        new IdvResourceManager.XmlIdvResource("idv.resource.framedefaults",
                           "McIDAS-X Frame Defaults");

    /** Points to the server definitions. */
    public static final IdvResourceManager.XmlIdvResource RSC_SERVERS =
        new IdvResourceManager.XmlIdvResource("idv.resource.servers",
                           "Servers", "servers\\.xml$");

    /** The chooser manager */
    protected McIdasChooserManager chooserManager;
	
    /**
     * Create the McIdasV with the given command line arguments.
     * This constructor calls {@link IntegratedDataViewer#init()}
     *
     * @param args Command line arguments
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     */
    public McIDASV(String[] args) throws VisADException, RemoteException {
        super(args);

        // we're tired of the IDV's default missing image, so reset it
        GuiUtils.MISSING_IMAGE = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/mcidasv-round22.png";

        this.init();
    }

    @Override
    public void setIdv(IntegratedDataViewer idv) {
    	this.idv = idv;
    }

    /**
     * Load the McV properties. All other property files are disregarded.
     * 
     * @see ucar.unidata.idv.IntegratedDataViewer#initPropertyFiles(java.util.List)
     */
    @Override
	public void initPropertyFiles(List files) {
        files.clear();
        files.add(Constants.PROPERTIES_FILE);
    }

    /**
     * Factory method to create the {@link IdvUIManager}. Here we create our 
     * own UI manager so it can do McV specific things.
     *
     * @return The UI manager indicated by the startup properties.
     */
    @Override
    protected IdvUIManager doMakeIdvUIManager() {
        return new UIManager(getIdv());
    }

    /**
     * Create our own VMManager so that we can make the tabs play nice.
     */
    @Override
    protected VMManager doMakeVMManager() {
    	// what an ugly class name :(
    	return new ViewManagerManager(idv);
    }

    /**
     * Make the {@link McIdasPreferenceManager}.
     * @see ucar.unidata.idv.IdvBase#doMakePreferenceManager()
     */
    @Override
    protected IdvPreferenceManager doMakePreferenceManager() {
        return new McIdasPreferenceManager(getIdv());
    }

    /**
     * Make the {@link edu.wisc.ssec.mcidasv.chooser.McIdasChooserManager}.
     * @see ucar.unidata.idv.IdvBase#doMakePreferenceManager()
     */
    protected IdvChooserManager doMakeChooserManager() {
        return new McIdasChooserManager(getIdv());
    }

    /**
     *  Create, if needed,  and return the
     * {@link McIdasChooserManager}
     *
     * @return The Chooser manager
     */
    public McIdasChooserManager getMcIdasChooserManager() {
        if (chooserManager == null) {
            chooserManager = (McIdasChooserManager)doMakeChooserManager();
        }
        return chooserManager;
    }

    /**
     * Get McIDASV. 
     * @see ucar.unidata.idv.IdvBase#getIdv()
     */
    @Override
    public IntegratedDataViewer getIdv() {
    	return this;
    }

    protected ArgsManager doMakeArgsManager() {
    	return new ArgumentManager(getIdv(), args);
    }

    /**
     * Factory method to create the
     * {@link edu.wisc.ssec.mcidasv.data.McvDataManager}
     *
     * @return The data manager
     */
    @Override
    protected DataManager doMakeDataManager() {
    	return new McvDataManager(this);
    }

    /**
     * Make the McIDAS-V {@link StateManager}.
     * @see ucar.unidata.idv.IdvBase#doMakeStateManager()
     */
    @Override
    protected StateManager doMakeStateManager() {
    	return new StateManager(getIdv());
    }
    
    /**
     * Make the McIDAS-V {@link ResourceManager}.
     * @see ucar.unidata.idv.IdvBase#doMakeResourceManager()
     */
    @Override
    protected IdvResourceManager doMakeResourceManager() {
    	return new ResourceManager(getIdv());
    }

    /**
     * Make the {@link edu.wisc.ssec.mcidasv.ui.McIdasColorTableManager}.
     * @see ucar.unidata.idv.IdvBase#doMakeColorTableManager()
     */
    @Override
    protected ColorTableManager doMakeColorTableManager() {
        return new McIdasColorTableManager();
    }

    /**
     * Factory method to create the {@link McIDASVPluginManager}
     *
     * @return The McV plugin manager
     */
    @Override
    protected PluginManager doMakePluginManager() {
    	return new McIDASVPluginManager(getIdv());
    }

    @Override
    public ArgsManager getArgsManager() {
    	if (argsManager == null)
    		argsManager = doMakeArgsManager();
    	
    	return argsManager;
    }

    @Override
    public ucar.unidata.idv.StateManager getStateManager() {
    	if (stateManager == null)
    		stateManager = doMakeStateManager();
    	
    	return stateManager;
    }

    /**
     * Create, if needed, and return the {@link McIDASVPluginManager}
     *
     * @return The McV plugin manager
     */
    @Override
    public PluginManager getPluginManager() {
    	if (pluginManager == null) 
    		pluginManager = doMakePluginManager();

        return pluginManager;
    }

//    /**
//     * Make the {@link edu.wisc.ssec.mcidasv.data.McIDASVProjectionManager}.
//     * @see ucar.unidata.idv.IdvBase#doMakeIdvProjectionManager()
//     */
//    @Override
//    protected IdvProjectionManager doMakeIdvProjectionManager() {
//    	return new McIDASVProjectionManager(getIdv());
//    }

    /**
     * The main. Configure the logging and create the McIdasV
     *
     * @param args Command line arguments
     *
     * @throws Exception When something untoward happens
     */
    public static void main(String[] args) throws Exception {
        LogUtil.configure();
        new McIDASV(args);
    }
}