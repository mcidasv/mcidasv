/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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
package edu.wisc.ssec.mcidasv;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIDefaults;

import org.w3c.dom.Element;

import ucar.unidata.data.DataManager;
import ucar.unidata.idv.ArgsManager;
import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.PluginManager;
import ucar.unidata.idv.VMManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlDelegateImpl;
import ucar.unidata.xml.XmlEncoder;
import visad.VisADException;
import edu.wisc.ssec.mcidasv.addemanager.AddeManager;
import edu.wisc.ssec.mcidasv.chooser.McIdasChooserManager;
import edu.wisc.ssec.mcidasv.control.LambertAEA;
import edu.wisc.ssec.mcidasv.data.McvDataManager;
import edu.wisc.ssec.mcidasv.ui.McIdasColorTableManager;
import edu.wisc.ssec.mcidasv.ui.UIManager;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.WebBrowser;

@SuppressWarnings("unchecked")
public class McIDASV extends IntegratedDataViewer {

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

    /** Used to access McIDAS-V state in a static context. */
    private static McIDASV staticMcv;

    /** Accessory in file save dialog */
    private JCheckBox overwriteDataCbx = 
        new JCheckBox("Change data paths", false);

    /** The chooser manager */
    protected McIdasChooserManager chooserManager;

    /** The ADDE manager */
    protected static AddeManager addeManager;

    /** The http based monitor to dump stack traces and shutdown the IDV */
    private McIDASVMonitor mcvMonitor;

    /** Actions passed into {@link #handleAction(String, Hashtable, boolean)}. */
    private final List<String> actions = new ArrayList<String>();

    /**
     * Create the McIDASV with the given command line arguments.
     * This constructor calls {@link IntegratedDataViewer#init()}
     * 
     * @param args Command line arguments
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     */
    public McIDASV(String[] args) throws VisADException, RemoteException {
        super(args);

        staticMcv = this;

        // Keep this code around for reference--this requires MacMenuManager.java and MRJToolkit.
        // We use OSXAdapter instead now, but it is unclear which is the preferred method.
        // Let's use the one that works.
//    	if (isMac()) {
//    		try {
//    			Object[] constructor_args = { this };
//    			Class[] arglist = { McIDASV.class };
//    			Class mac_class = Class.forName("edu.wisc.ssec.mcidasv.MacMenuManager");
//    			Constructor new_one = mac_class.getConstructor(arglist);
//    			new_one.newInstance(constructor_args);
//    		}
//    		catch(Exception e) {
//    			System.out.println(e);
//    		}
//    	}
    	
        // Set up our application to respond to the Mac OS X application menu
        registerForMacOSXEvents();
    	        
        // This doesn't always look good... but keep it here for future reference
        if (false) {
        	UIDefaults def = javax.swing.UIManager.getLookAndFeelDefaults();
        	Enumeration defkeys = def.keys();
        	while (defkeys.hasMoreElements()) {
        		Object item = defkeys.nextElement();
        		if (item.toString().indexOf("selectionBackground") > 0)
        			def.put(item, Constants.MCV_BLUE);
        	}
        }
        
        // we're tired of the IDV's default missing image, so reset it
        GuiUtils.MISSING_IMAGE = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/mcidasv-round22.png";
        
        this.init();
    }
    
    // Generic registration with the Mac OS X application menu
    // Checks the platform, then attempts to register with the Apple EAWT
    // See OSXAdapter.java to see how this is done without directly referencing any Apple APIs
    public void registerForMacOSXEvents() {
        if (isMac()) {
            try {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
                // use as delegates for various com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("MacOSXQuit", (Class[])null));
                OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("MacOSXAbout", (Class[])null));
                OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("MacOSXPreferences", (Class[])null));
            } catch (Exception e) {
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
        }
    }
    public boolean MacOSXQuit() {
    	return quit();
    }
    public void MacOSXAbout() {
    	getIdvUIManager().about();
    }
    public void MacOSXPreferences() {
    	showPreferenceManager();
    }

    /**
     * Start up the McIDAS-V monitor server. This is an http server on the port defined
     * by the property idv.monitorport (8788).  It is only accessible to 127.0.0.1 (localhost)
     */
    // TODO: we probably don't want our own copy of this in the long run...
    // all we did was change "IDV" to "McIDAS-V"
    protected void startMonitor() {
        if(mcvMonitor!=null) return;
        final String monitorPort = getProperty(PROP_MONITORPORT,"");
        if(monitorPort!=null && monitorPort.trim().length()>0 && !monitorPort.trim().equals("none")) {
            Misc.run(new Runnable() {
                    public void run() {
                        try {
                        	mcvMonitor = new McIDASVMonitor(McIDASV.this, new Integer(monitorPort).intValue());
                        	mcvMonitor.init();
                        }    catch (Exception exc) {
                            LogUtil.consoleMessage("Unable to start McIDAS-V monitor on port:" + monitorPort);
                            LogUtil.consoleMessage("Error:" + exc);
                        }
                    }});
        }
    }
    
    /**
     * Initializes a XML encoder with McIDAS-V specific XML delegates.
     * 
     * @param encoder XML encoder that'll be dealing with persistence.
     * @param forRead Not used as of yet.
     */
    // TODO: if we ever get up past three or so XML delegates, I vote that we
    // make our own version of VisADPersistence.
    @Override protected void initEncoder(XmlEncoder encoder, boolean forRead) {

        encoder.addDelegateForClass(LambertAEA.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                LambertAEA projection = (LambertAEA)o;
                Rectangle2D rect = projection.getDefaultMapArea();
                List args = Misc.newList(rect);
                List types = Misc.newList(rect.getClass());
                return e.createObjectConstructorElement(o, args, types);
            }
        });

    }

    /**
     * Returns <i>all</i> of the actions used in this McIDAS-V session. This is
     * possibly TMI and might be removed...
     * 
     * @return
     */
    public List<String> getActionHistory() {
        return actions;
    }

    /**
     * Converts {@link ArgsManager#getOriginalArgs()} to a {@link ArrayList} and
     * returns.
     * 
     * @return The command-line arguments used to start McIDAS-V, as an 
     * {@code ArrayList}.
     */
    public List<String> getCommandLineArgs() {
        List<String> args = new ArrayList<String>();
        for (String arg : getArgsManager().getOriginalArgs())
            args.add(arg);
        return args;
    }

    /**
     * Captures the action passed to {@code handleAction}. The action is logged
     * and additionally, if the action is a HTML link, we attempt to visit the
     * link in the user's preferred browser.
     */
    @Override public boolean handleAction(String action, Hashtable properties, 
        boolean checkForAlias) 
    {
        actions.add(action);

        if (IOUtil.isHtmlFile(action)) {
            WebBrowser.browse(action);
            return true;
        }

        return super.handleAction(action, properties, checkForAlias);
    }

    /**
     * Handles removing all loaded data sources.
     * 
     * @param showWarning Whether or not to display a warning message before
     * removing <i>all</i> data sources. See the return details for more.
     * 
     * @return Either {@code true} if the user wants to continue showing the
     * warning dialog, or {@code false} if they've elected to stop showing the
     * warning. If {@code showWarning} is {@code false}, this method will 
     * always return {@code false}, as the user isn't interested in seeing the
     * warning.
     */
    private boolean removeAllData(final boolean showWarning) {
        boolean reallyRemove = false;
        boolean continueWarning = true;
        if (showWarning) {
            JCheckBox box = new JCheckBox("Continue to ask me if I want to remove all of my data?", true);
            JComponent comp = GuiUtils.vbox(
                new JLabel("This action will remove all of the data currently loaded in McIDAS-V. You cannot undo this as of yet. Is this what you want to do?"), 
                GuiUtils.inset(box, new Insets(4, 15, 0, 10)));

            Object[] options = { "Remove all data", "Do not remove any data" };
            int result = JOptionPane.showOptionDialog(
                LogUtil.getCurrentWindow(), // parent
                comp,                        // msg
                "Confirm data removal",      // title
                JOptionPane.YES_NO_OPTION,   // option type
                JOptionPane.WARNING_MESSAGE, // message type
                (Icon)null,                  // icon?
                options,                     // selection values
                options[1]);                 // initial?

            if (result == JOptionPane.YES_OPTION)
                reallyRemove = true;

            continueWarning = box.isSelected();
        } else {
            // user doesn't want to see warning messages.
            reallyRemove = true;
            continueWarning = false;
        }

        if (reallyRemove)
            super.removeAllDataSources();

        return continueWarning;
    }

    /**
     * Handles removing all loaded layers ({@literal "displays"} in IDV-land).
     * 
     * @param showWarning Whether or not to display a warning message before
     * removing <i>all</i> layers. See the return details for more.
     * 
     * @return Either {@code true} if the user wants to continue showing the
     * warning dialog, or {@code false} if they've elected to stop showing the
     * warning. If {@code showWarning} is {@code false}, this method will 
     * always return {@code false}, as the user isn't interested in seeing the
     * warning.
     */
    private boolean removeAllLayers(final boolean showWarning) {
        boolean reallyRemove = false;
        boolean continueWarning = true;
        if (showWarning) {
            JCheckBox box = new JCheckBox("Continue to ask me if I want to remove all of my layers?", true);
            JComponent comp = GuiUtils.vbox(
                new JLabel("This action will remove every layer currently loaded in McIDAS-V. You cannot undo this as of yet. Is this what you want to do?"), 
                GuiUtils.inset(box, new Insets(4, 15, 0, 10)));

            Object[] options = { "Remove all layers", "Do not remove any layers" };
            int result = JOptionPane.showOptionDialog(
                LogUtil.getCurrentWindow(),  // parent
                comp,                        // msg
                "Confirm layer removal",     // title
                JOptionPane.YES_NO_OPTION,   // option type
                JOptionPane.WARNING_MESSAGE, // message type
                (Icon)null,                  // icon?
                options,                     // selection values
                options[1]);                 // initial?

            if (result == JOptionPane.YES_OPTION)
                reallyRemove = true;

            continueWarning = box.isSelected();
        } else {
            // user doesn't want to see warning messages.
            reallyRemove = true;
            continueWarning = false;
        }

        if (reallyRemove)
            super.removeAllDisplays();

        return continueWarning;
    }

    /**
     * Overridden so that McIDAS-V can prompt the user before removing, if 
     * necessary.
     */
    @Override public void removeAllDataSources() {
        boolean showWarning = getStore().get(Constants.PREF_CONFIRM_REMOVE_DATA, true);
        showWarning = removeAllData(showWarning);
        getStore().put(Constants.PREF_CONFIRM_REMOVE_DATA, showWarning);
    }

    /**
     * Overridden so that McIDAS-V can prompt the user before removing, if 
     * necessary.
     */
    @Override public void removeAllDisplays() {
        boolean showWarning = getStore().get(Constants.PREF_CONFIRM_REMOVE_LAYERS, true);
        showWarning = removeAllLayers(showWarning);
        getStore().put(Constants.PREF_CONFIRM_REMOVE_LAYERS, showWarning);
    }

    /**
     * Handles removing all loaded layers ({@literal "displays"} in IDV-land)
     * and data sources.
     * 
     * @param showWarning Whether or not to display a warning message before
     * removing <i>all</i> layers and data sources.
     * 
     * @see #removeAllData(boolean)
     * @see #removeAllLayers(boolean)
     */
    public void removeAllLayersAndData() {
        boolean reallyRemove = false;
        boolean continueWarning = true;
        boolean showWarning = getStore().get(Constants.PREF_CONFIRM_REMOVE_BOTH, true);
        if (showWarning) {
            JCheckBox box = new JCheckBox("Continue asking me if I want to remove all of my layers and data?", true);
            JComponent comp = GuiUtils.vbox(
                new JLabel("This action will remove all of your currently loaded layers and data. You cannot undo this as of yet. Is this what you want to do?"), 
                GuiUtils.inset(box, new Insets(4, 15, 0, 10)));

            Object[] options = { "Remove all layers and data", "Do not remove anything" };
            int result = JOptionPane.showOptionDialog(
                LogUtil.getCurrentWindow(),  // parent
                comp,                        // msg
                "Confirm removal",           // title
                JOptionPane.YES_NO_OPTION,   // option type
                JOptionPane.WARNING_MESSAGE, // message type
                (Icon)null,                  // icon?
                options,                     // selection values
                options[1]);                 // initial?

            if (result == JOptionPane.YES_OPTION)
                reallyRemove = true;

            continueWarning = box.isSelected();
        } else {
            // user doesn't want to see warning messages.
            reallyRemove = true;
            continueWarning = false;
        }

        // don't show the individual warning messages as the user has attempted
        // to remove *both*
        if (reallyRemove) {
            removeAllData(false);
            removeAllLayers(false);
        }

        getStore().put(Constants.PREF_CONFIRM_REMOVE_BOTH, continueWarning);
    }

    /**
     * Overridden so that McIDAS-V doesn't have to create an entire new {@link ucar.unidata.idv.ui.IdvWindow}
     * if {@link VMManager#findViewManager(ViewDescriptor)} can't find an appropriate
     * ViewManager for {@code viewDescriptor}.
     * 
     * <p>Not doing the above causes McIDAS-V to get stuck in a window creation
     * loop.
     */
    @Override public ViewManager getViewManager(ViewDescriptor viewDescriptor,
        boolean newWindow, String properties) 
    {
        ViewManager vm = 
            getVMManager().findOrCreateViewManager(viewDescriptor, properties);
        if (vm == null)
            vm = super.getViewManager(viewDescriptor, newWindow, properties);
        return vm;
    }

    /**
     * Returns a reference to the current McIDAS-V object. Useful for working 
     * inside static methods. <b>Always check for null when using this 
     * method</b>.
     * 
     * @return Either the current McIDAS-V "god object" or {@code null}.
     */
    public static McIDASV getStaticMcv() {
        return staticMcv;
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
     * Makes {@link PersistenceManager} save off a default {@literal "layout"}
     * bundle.
     */
    public void doSaveAsDefaultLayout() {
        Misc.run(new Runnable() {
            public void run() {
                ((PersistenceManager)getPersistenceManager()).doSaveAsDefaultLayout();
            }
        });
    }

    /**
     * Determines whether or not a default layout exists.
     * 
     * @return {@code true} if there is a default layout, {@code false} 
     * otherwise.
     */
    public boolean hasDefaultLayout() {
        String path = 
            getResourceManager().getResources(IdvResourceManager.RSC_BUNDLES)
                .getWritable();
        return new File(path).exists();
    }

    /**
     * Called from the menu command to clear the default bundle. Overridden
     * in McIDAS-V so that we reference the <i>layout</i> rather than the
     * bundle.
     */
    @Override public void doClearDefaults() {
        if (GuiUtils.showYesNoDialog(null, 
                "Are you sure you want to delete your default layout?",
                "Delete confirmation")) 
            resourceManager.clearDefaultBundles();
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
    protected IdvChooserManager doMakeIdvChooserManager() {
        chooserManager =
            (McIdasChooserManager) makeManager(McIdasChooserManager.class,
                                            new Object[] { idv });
        chooserManager.init();
        return chooserManager;
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
     *  Create, if needed,  and return the
     * {@link McIdasChooserManager}
     *
     * @return The Chooser manager
     */
    public McIdasChooserManager getMcIdasChooserManager() {
        return (McIdasChooserManager)getIdvChooserManager();
    }

    /**
     *  Create, if needed,  and return the
     * {@link AddeManager}
     *
     * @return The Chooser manager
     */
    public AddeManager getAddeManager() {
        return addeManager;
    }

    public void showAddeManager() {
    	getAddeManager().show();
    }

    public void showServerManager() {
        getServerManager().show();
    }

    public ServerPreferenceManager getServerManager() {
        if (getPreferenceManager() == null)
            preferenceManager = doMakePreferenceManager();
        return ((McIdasPreferenceManager)getPreferenceManager()).getServerManager();
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
     * Make the {@link McIdasColorTableManager}.
     * @see ucar.unidata.idv.IdvBase#doMakeColorTableManager()
     */
    @Override
    protected ColorTableManager doMakeColorTableManager() {
        return new McIdasColorTableManager();
    }

    /**
     * Factory method to create the {@link McvPluginManager}.
     *
     * @return The McV plugin manager.
     * 
     * @see ucar.unidata.idv.IdvBase#doMakePluginManager()
     */
    @Override
    protected PluginManager doMakePluginManager() {
        return new McvPluginManager(idv);
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
     * Make a help button for a particular help topic
     *
     * @param helpId  the topic id
     * @param toolTip  the tooltip
     *
     * @return  the button
     */
    public JComponent makeHelpButton(String helpId, String toolTip) {
    	JButton btn =
    		McVGuiUtils.makeImageButton(Constants.ICON_HELP,
    				getIdvUIManager(), "showHelp", helpId, "Show help");

    	if (toolTip != null) {
    		btn.setToolTipText(toolTip);
    	}
    	return btn;
    }

    /**
     * Are we on a Mac?  Used to build the MRJ handlers
     * Take from TN2042
     * @return
     */
	public boolean isMac() {
		return System.getProperty("mrj.version") != null;
	}

    /**
     * The main. Configure the logging and create the McIdasV
     * 
     * @param args Command line arguments
     * 
     * @throws Exception When something untoward happens
     */
    public static void main(String[] args) throws Exception {
        LogUtil.configure();
        McIDASV myself = new McIDASV(args);
        addeManager = new AddeManager(myself);
        addeManager.startLocalServer();
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