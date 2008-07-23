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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;

import ucar.unidata.data.DataManager;
import ucar.unidata.idv.ArgsManager;
import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.PluginManager;
import ucar.unidata.idv.VMManager;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import visad.VisADException;
import edu.wisc.ssec.mcidasv.addemanager.AddeManager;
import edu.wisc.ssec.mcidasv.chooser.McIdasChooserManager;
import edu.wisc.ssec.mcidasv.data.McvDataManager;
import edu.wisc.ssec.mcidasv.ui.McIdasColorTableManager;
import edu.wisc.ssec.mcidasv.ui.UIManager;

@SuppressWarnings("unchecked")
public class McIDASV extends IntegratedDataViewer{

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

    /** Accessory in file save dialog */
    private JCheckBox overwriteDataCbx = 
        new JCheckBox("Change data paths", false);

    /** The chooser manager */
    protected McIdasChooserManager chooserManager;

    /** The ADDE manager */
    protected static AddeManager addeManager;
    
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

    /**
     * @see ucar.unidata.idv.IdvBase#setIdv(ucar.unidata.idv.IntegratedDataViewer)
     */
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
     * <p>
     * Overridden so that the support form becomes non-modal if launched from
     * an exception dialog.
     * </p>
     * 
     * @see ucar.unidata.idv.IntegratedDataViewer#addErrorButtons(JDialog, List, String, Throwable)
     */
    @Override public void addErrorButtons(final JDialog dialog, 
        List buttonList, final String msg, final Throwable exc) 
    {
        JButton supportBtn = new JButton("Support Form");
        supportBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                getIdvUIManager().showSupportForm(msg,
                    LogUtil.getStackTrace(exc), null);
            }
        });
        buttonList.add(supportBtn);
    }

    /**
     * @see IntegratedDataViewer#doOpen(String, boolean, boolean)
     */
    @Override public void doOpen(final String filename,
        final boolean checkUserPreference, final boolean andRemove) 
    {
        doOpenInThread(filename, checkUserPreference, andRemove);
    }

    /**
     * Have the user select an xidv file. If andRemove is true then we remove
     * all data sources and displays. Then we open the unpersist the bundle in
     * the xidv file
     * 
     * @param filename The filename to open
     * @param checkUserPreference Should we show, if needed, the Open dialog
     * @param andRemove If true then first remove all data sources and displays
     */
    private void doOpenInThread(String filename, boolean checkUserPreference,
        boolean andRemove) 
    {
        boolean overwriteData = false;
        if (filename == null) {
            if (overwriteDataCbx.getToolTipText() == null)
                overwriteDataCbx.setToolTipText("Change the file paths that the data sources use");

            filename = FileManager.getReadFile("Open File",
                ((ArgumentManager)getArgsManager()).getBundleFilters(true), 
                GuiUtils.top(overwriteDataCbx));

            if (filename == null)
                return;

            overwriteData = overwriteDataCbx.isSelected();
        }

        if (ArgumentManager.isXmlBundle(filename)) {
            getPersistenceManager().decodeXmlFile(filename,
                checkUserPreference, overwriteData);
            return;
        }
        handleAction(filename, null);
    }


    /**
     * Factory method to create the {@link IdvUIManager}. Here we create our 
     * own UI manager so it can do McV specific things.
     *
     * @return The UI manager indicated by the startup properties.
     * 
     * @see ucar.unidata.idv.IdvBase#doMakeIdvUIManager()
     */
    @Override
    protected IdvUIManager doMakeIdvUIManager() {
        return new UIManager(idv);
    }

    /**
     * Create our own VMManager so that we can make the tabs play nice.
     * @see ucar.unidata.idv.IdvBase#doMakeVMManager()
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
        return new McIdasPreferenceManager(idv);
    }

    /**
     * <p>McIDAS-V (alpha 10+) needs to handle both IDV bundles without 
     * component groups and all bundles from prior McV alphas. You better 
     * believe we need to extend the persistence manager functionality!</p>
     * 
     * @see ucar.unidata.idv.IdvBase#doMakePersistenceManager()
     */
    @Override protected IdvPersistenceManager doMakePersistenceManager() {
    	return new PersistenceManager(idv);
    }

    /**
     * Make the {@link edu.wisc.ssec.mcidasv.chooser.McIdasChooserManager}.
     * @see ucar.unidata.idv.IdvBase#doMakePreferenceManager()
     */
    protected IdvChooserManager doMakeChooserManager() {
        return new McIdasChooserManager(idv);
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
     * Make the {@link edu.wisc.ssec.mcidasv.addemanager.AddeManager}.
     * @see ucar.unidata.idv.IdvBase#doMakePreferenceManager()
     */
    protected AddeManager doMakeAddeManager() {
        return new AddeManager();
    }
    
    /**
     *  Create, if needed,  and return the
     * {@link AddeManager}
     *
     * @return The Chooser manager
     */
    public AddeManager getAddeManager() {
        if (addeManager == null) {
            addeManager = (AddeManager)doMakeAddeManager();
        }
        return addeManager;
    }

    /**
     * Get McIDASV. 
     * @see ucar.unidata.idv.IdvBase#getIdv()
     */
    @Override
    public IntegratedDataViewer getIdv() {
    	return idv;
    }

    /**
     * Creates a McIDAS-V argument manager so that McV can handle some non-IDV
     * command line things.
     * 
     * @param The arguments from the command line.
     * 
     * @see ucar.unidata.idv.IdvBase#doMakeArgsManager(java.lang.String[])
     */
    @Override
    protected ArgsManager doMakeArgsManager(String[] args) {
    	return new ArgumentManager(idv, args);
    }

    /**
     * Factory method to create the
     * {@link edu.wisc.ssec.mcidasv.data.McvDataManager}
     * 
     * @return The data manager
     * 
     * @see ucar.unidata.idv.IdvBase#doMakeDataManager()
     */
    @Override
    protected DataManager doMakeDataManager() {
    	return new McvDataManager(idv);
    }

    /**
     * Make the McIDAS-V {@link StateManager}.
     * @see ucar.unidata.idv.IdvBase#doMakeStateManager()
     */
    @Override
    protected StateManager doMakeStateManager() {
    	return new StateManager(idv);
    }
    
    /**
     * Make the McIDAS-V {@link ResourceManager}.
     * @see ucar.unidata.idv.IdvBase#doMakeResourceManager()
     */
    @Override
    protected IdvResourceManager doMakeResourceManager() {
    	return new ResourceManager(idv);
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
     * Factory method to create the {@link McIDASVPluginManager}.
     *
     * @return The McV plugin manager.
     * 
     * @see ucar.unidata.idv.IdvBase#doMakePluginManager()
     */
    @Override
    protected PluginManager doMakePluginManager() {
    	return new McIDASVPluginManager(idv);
    }

//    /**
//     * Make the {@link edu.wisc.ssec.mcidasv.data.McIDASVProjectionManager}.
//     * @see ucar.unidata.idv.IdvBase#doMakeIdvProjectionManager()
//     */
//    @Override
//    protected IdvProjectionManager doMakeIdvProjectionManager() {
//    	return new McIDASVProjectionManager(idv);
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
        addeManager = new AddeManager();
        addeManager.startLocalServer();
        new McIDASV(args);
    }
    
    /**
     * Try to stop the ADDE local data server
     * This is called after IntegratedDataViewer quit() has done its thing
     * 
     * @param exitCode System exit code to use
     */
    protected void exit(int exitCode) {
    	addeManager.stopLocalServer();
    	System.exit(exitCode);
    }
}