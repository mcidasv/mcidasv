/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.mcidasv;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.cast;
import static ucar.unidata.xml.XmlUtil.getAttribute;

import java.awt.Insets;
import java.awt.geom.Rectangle2D;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import java.security.Security;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import com.formdev.flatlaf.FlatDarkLaf;
import edu.wisc.ssec.hydra.DataBrowser;
import edu.wisc.ssec.mcidas.adde.AddeURL;
import edu.wisc.ssec.mcidas.adde.AddeURLStreamHandler;

import com.google.common.base.Strings;
import edu.wisc.ssec.mcidasv.collaboration.CollaborationManager;
import edu.wisc.ssec.mcidasv.ui.McIDASVXmlUi;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.OptionPaneClicker;
import edu.wisc.ssec.mcidasv.util.SystemState;
import edu.wisc.ssec.mcidasv.util.WebBrowser;
import edu.wisc.ssec.mcidasv.util.WelcomeWindow;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.joda.time.DateTime;
import org.python.util.PythonInterpreter;
import org.w3c.dom.Element;

import ucar.nc2.NetcdfFile;
import visad.VisADException;

import ucar.unidata.data.DataManager;
import ucar.unidata.idv.ArgsManager;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.PluginManager;
import ucar.unidata.idv.VMManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.collab.CollabManager;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.ui.InteractiveShell.ShellHistoryEntry;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlDelegateImpl;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;

import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.lidalia.sysoutslf4j.context.LogLevel;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import edu.wisc.ssec.mcidasv.data.GpmIosp;
import edu.wisc.ssec.mcidasv.data.TropomiIOSP;
import edu.wisc.ssec.mcidasv.chooser.McIdasChooserManager;
import edu.wisc.ssec.mcidasv.control.LambertAEA;
import edu.wisc.ssec.mcidasv.data.McvDataManager;
import edu.wisc.ssec.mcidasv.monitors.MonitorManager;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntrySource;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryStatus;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryValidity;
import edu.wisc.ssec.mcidasv.servermanager.AddePreferences;
import edu.wisc.ssec.mcidasv.servermanager.EntryStore;
import edu.wisc.ssec.mcidasv.servermanager.EntryTransforms;
import edu.wisc.ssec.mcidasv.servermanager.LocalAddeEntry;
import edu.wisc.ssec.mcidasv.servermanager.LocalAddeEntry.AddeFormat;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry;
import edu.wisc.ssec.mcidasv.servermanager.TabbedAddeManager;
import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.ui.LayerAnimationWindow;
import edu.wisc.ssec.mcidasv.ui.McIdasColorTableManager;
import edu.wisc.ssec.mcidasv.ui.UIManager;
import edu.wisc.ssec.mcidasv.util.gui.EventDispatchThreadHangMonitor;
import edu.wisc.ssec.mcidasv.util.pathwatcher.DirectoryWatchService;
import edu.wisc.ssec.mcidasv.util.pathwatcher.OnFileChangeListener;
import edu.wisc.ssec.mcidasv.util.pathwatcher.SimpleDirectoryWatchService;

/**
 * Class used as the {@literal "gateway"} to a running McIDAS-V session.
 *
 * <p>This is where the startup and shutdown processes are handled, as well as
 * the initialization of the application's various {@literal "managers"}.</p>
 */
@SuppressWarnings("unchecked")
public class McIDASV extends IntegratedDataViewer {
    
    /** Logging object. */
    private static final Logger logger =
        LoggerFactory.getLogger(McIDASV.class);
        
    /**
     * Initialization start time. This value is set at the beginning of
     * {@link #main(String[])}, and is used to estimate the duration of the
     * session's initialization phase.
     */
    private static long startTime;
    
    /**
     * Initialization duration. Set at the end of {@link #initDone()}.
     */
    private static long estimate;
    
    /** 
     * Path to a {@literal "session"} file--it's created upon McIDAS-V 
     * starting and removed when McIDAS-V exits cleanly. This allows us to
     * perform a primitive check to see if the current session has happened
     * after a crash. 
     */
    private static String SESSION_FILE = getSessionFilePath();
    
    /**
     * Whether or not the previous session was able to exit as it should.
     * If {@code false}, the previous session likely crashed.
     */
    private static boolean cleanExit = true;
    
    /** Date the previous session was started. May be {@code null}. */
    private static Date previousStart = null;
    
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
    
    /** Chooser manager */
    protected McIdasChooserManager chooserManager;
    
    /** HTTP based monitor to dump stack traces and shutdown McIDAS-V. */
    private McIDASVMonitor mcvMonitor;
    
    /**
     * {@link MonitorManager} allows for relatively easy and efficient
     * monitoring of various resources.
     */
    private final MonitorManager monitorManager = new MonitorManager();
    
    /**
     * Actions passed into {@link #handleAction(String, Hashtable, boolean)}.
     */
    private final List<String> actions = new LinkedList<>();
    
    private enum WarningResult { OK, CANCEL, SHOW, HIDE };
    
    /** Reference to the ADDE server manager. */
    private EntryStore addeEntries;
    
    /**
     * GUI wrapper for ADDE server management. Reference is kept due to some
     * of the trickery used by {@link AddePreferences}.
     *
     * <p>Value may be {@code null}.</p>
     */
    private TabbedAddeManager tabbedAddeManager = null;
    
    /** Directory monitoring service. */
    private final DirectoryWatchService watchService;
    
    /**
     * Create the McIDASV with the given command line arguments.
     * This constructor calls {@link IntegratedDataViewer#init()}
     * 
     * @param args Command line arguments
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     */
    public McIDASV(String[] args) throws IOException, VisADException {
        super(args);
        
        AnnotationProcessor.process(this);
        
        staticMcv = this;
        
        // Set up our application to respond to the Mac OS X application menu
        registerForMacOSXEvents();
        
        // we're tired of the IDV's default missing image, so reset it
        GuiUtils.MISSING_IMAGE =
            "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/mcidasv-round22.png";
        
        watchService = new SimpleDirectoryWatchService();
        
        this.init();
        
        // ensure jython init only happens once per application session
        String cacheDir = getStore().getJythonCacheDir();
        Properties pyProps = new Properties();
        if (cacheDir != null) {
            pyProps.put("python.home", cacheDir);
        }
        String[] blank = new String[] { "" };
        PythonInterpreter.initialize(System.getProperties(), pyProps, blank);
    }
    
    /**
     * Generic registration with the macOS application menu.
     *
     * <p>Checks the platform, then attempts to register with Apple's
     * {@literal "EAWT"} stuff.</p>
     *
     * <p>See {@code OSXAdapter.java} to learn how this is done without
     * directly referencing any Apple APIs.</p>
     */
    public void registerForMacOSXEvents() {
        // TODO(jon): remove?
        if (isMac()) {
            try {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
                // use as delegates for various com.apple.eawt.ApplicationListener methods
                Class<?> thisClass = getClass();
                Class<?>[] args = null;
                OSXAdapter.setQuitHandler(this, thisClass.getDeclaredMethod("MacOSXQuit", args));
                OSXAdapter.setAboutHandler(this, thisClass.getDeclaredMethod("MacOSXAbout", args));
                OSXAdapter.setPreferencesHandler(this, thisClass.getDeclaredMethod("MacOSXPreferences", args));
            } catch (Exception e) {
                logger.error("Error while loading the OSXAdapter", e);
            }
        }
    }
    
    public boolean MacOSXQuit() {
        // TODO(jon): remove?
        return quit();
    }
    
    public void MacOSXAbout() {
        // TODO(jon): remove?
        getIdvUIManager().about();
    }
    
    public void MacOSXPreferences() {
        // TODO(jon): remove?
        showPreferenceManager();
    }
    
    /**
     * Get the maximum number of threads to be used when rendering in VisAD.
     *
     * @return Number of threads for rendering. Default value is the same as
     * {@link Runtime#availableProcessors()}.
     */
    @Override public int getMaxRenderThreadCount() {
        StateManager stateManager = (StateManager)getStateManager();
        return stateManager.getPropertyOrPreference(PREF_THREADS_RENDER,
            Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Get the maximum number of threads to be used when reading data.
     *
     * @return Number of threads for reading data. Default value is {@code 4}.
     */
    @Override public int getMaxDataThreadCount() {
        StateManager stateManager = (StateManager)getStateManager();
        return stateManager.getPropertyOrPreference(PREF_THREADS_DATA, 4);
    }
    
    /**
     * Start up the McIDAS-V monitor server.
     *
     * <p>This is an HTTP server on the port defined by the property
     * {@code idv.monitorport}. Default value is 8788.</p>
     *
     * <p>It is only accessible to 127.0.0.1 (localhost).</p>
     */
    @Override protected void startMonitor() {
        if (mcvMonitor != null) {
            return;
        }
        final String monitorPort = getProperty(PROP_MONITORPORT, "");
        if (monitorPort!=null && monitorPort.trim().length()>0 && !"none".equals(monitorPort.trim())) {
            Misc.run(() -> {
                try {
                    mcvMonitor =
                        new McIDASVMonitor(McIDASV.this,
                                           Integer.parseInt(monitorPort));
                    mcvMonitor.init();
                } catch (Exception exc) {
                    LogUtil.consoleMessage("Unable to start McIDAS-V monitor on port:" + monitorPort);
                    LogUtil.consoleMessage("Error:" + exc);
                }
            });
        }
    }
    
    /**
     * Initializes a XML encoder with McIDAS-V specific XML delegates.
     * 
     * @param encoder XML encoder that'll be dealing with persistence.
     * @param forRead Not used as yet.
     */
    // TODO: if we ever get up past three or so XML delegates, I vote that we
    // make our own version of VisADPersistence.
    @Override protected void initEncoder(XmlEncoder encoder, boolean forRead) {
        
        encoder.addDelegateForClass(LambertAEA.class, new XmlDelegateImpl() {
            @Override public Element createElement(XmlEncoder e, Object o) {
                LambertAEA projection = (LambertAEA)o;
                Rectangle2D rect = projection.getDefaultMapArea();
                List args = Misc.newList(rect);
                List types = Misc.newList(rect.getClass());
                return e.createObjectConstructorElement(o, args, types);
            }
        });
        
        encoder.addDelegateForClass(ShellHistoryEntry.class, new XmlDelegateImpl() {
            @Override public Element createElement(XmlEncoder e, Object o) {
                ShellHistoryEntry entry = (ShellHistoryEntry)o;
                List args = Misc.newList(entry.getEntryBytes(), entry.getInputMode().toString());
                return e.createObjectConstructorElement(o, args);
            }
        });
        
        // TODO(jon): ultra fashion makeover!!
        encoder.addDelegateForClass(RemoteAddeEntry.class, new XmlDelegateImpl() {
            @Override public Element createElement(XmlEncoder e, Object o) {
                RemoteAddeEntry entry = (RemoteAddeEntry)o;
                Element element = e.createObjectElement(o.getClass());
                element.setAttribute("address", entry.getAddress());
                element.setAttribute("group", entry.getGroup());
                element.setAttribute("username", entry.getAccount().getUsername());
                element.setAttribute("project", entry.getAccount().getProject());
                element.setAttribute("source", entry.getEntrySource().toString());
                element.setAttribute("type", entry.getEntryType().toString());
                element.setAttribute("validity", entry.getEntryValidity().toString());
                element.setAttribute("status", entry.getEntryStatus().toString());
                element.setAttribute("temporary", Boolean.toString(entry.isEntryTemporary()));
                element.setAttribute("alias", entry.getEntryAlias());
                return element;
            }
            
            @Override public Object createObject(XmlEncoder e, Element element) {
                String address = getAttribute(element, "address");
                String group = getAttribute(element, "group");
                String username = getAttribute(element, "username");
                String project = getAttribute(element, "project");
                String source = getAttribute(element, "source");
                String type = getAttribute(element, "type");
                String validity = getAttribute(element, "validity");
                String status = getAttribute(element, "status");
                boolean temporary = getAttribute(element, "temporary", false);
                String alias = getAttribute(element, "alias", "");
                
                EntrySource entrySource = EntryTransforms.strToEntrySource(source);
                EntryType entryType = EntryTransforms.strToEntryType(type);
                EntryValidity entryValidity = EntryTransforms.strToEntryValidity(validity);
                EntryStatus entryStatus = EntryTransforms.strToEntryStatus(status);
                
                return new RemoteAddeEntry.Builder(address, group)
                                          .account(username, project)
                                          .source(entrySource)
                                          .type(entryType)
                                          .validity(entryValidity)
                                          .temporary(temporary)
                                          .alias(alias)
                                          .status(entryStatus).build();
            }
        });
        
        encoder.addDelegateForClass(LocalAddeEntry.class, new XmlDelegateImpl() {
            @Override public Element createElement(XmlEncoder e, Object o) {
                LocalAddeEntry entry = (LocalAddeEntry)o;
                Element element = e.createObjectElement(o.getClass());
                element.setAttribute("group", entry.getGroup());
                element.setAttribute("descriptor", entry.getDescriptor());
                element.setAttribute("realtime", entry.getRealtimeAsString());
                element.setAttribute("format", entry.getFormat().name());
                element.setAttribute("start", entry.getStart());
                element.setAttribute("end", entry.getEnd());
                element.setAttribute("fileMask", entry.getMask());
                element.setAttribute("name", entry.getName());
                element.setAttribute("status", entry.getEntryStatus().name());
                element.setAttribute("temporary", Boolean.toString(entry.isEntryTemporary()));
                element.setAttribute("alias", entry.getEntryAlias());
                return element;
            }
            
            @Override public Object createObject(XmlEncoder e, Element element) {
                String group = getAttribute(element, "group");
                String descriptor = getAttribute(element, "descriptor");
                String realtime = getAttribute(element, "realtime", "");
                AddeFormat format = EntryTransforms.strToAddeFormat(XmlUtil.getAttribute(element, "format"));
                String start = getAttribute(element, "start", "1");
                String end = getAttribute(element, "end", "999999");
                String fileMask = getAttribute(element, "fileMask");
                String name = getAttribute(element, "name");
                String status = getAttribute(element, "status", "ENABLED");
                boolean temporary = getAttribute(element, "temporary", false);
                String alias = getAttribute(element, "alias", "");
                
                return new LocalAddeEntry.Builder(name, group, fileMask, format)
                                         .range(start, end)
                                         .descriptor(descriptor)
                                         .realtime(realtime)
                                         .status(status)
                                         .temporary(temporary)
                                         .alias(alias).build();
            }
        });
        
        // Move legacy classes to a new location
        encoder.registerNewClassName(
            "edu.wisc.ssec.mcidasv.data.Test2ImageDataSource",
            "edu.wisc.ssec.mcidasv.data.adde.AddeImageParameterDataSource");
        encoder.registerNewClassName(
            "edu.wisc.ssec.mcidasv.data.Test2AddeImageDataSource",
            "edu.wisc.ssec.mcidasv.data.adde.AddeImageParameterDataSource");
        encoder.registerNewClassName(
            "edu.wisc.ssec.mcidasv.data.AddePointDataSource",
            "edu.wisc.ssec.mcidasv.data.adde.AddePointDataSource");
        encoder.registerNewClassName(
            "edu.wisc.ssec.mcidasv.data.AddeSoundingAdapter",
            "edu.wisc.ssec.mcidasv.data.adde.AddeSoundingAdapter");
    }
    
    /**
     * Returns <i>all</i> of the actions used in this McIDAS-V session. This is
     * possibly TMI and might be removed...
     * 
     * @return Actions executed thus far.
     */
    public List<String> getActionHistory() {
        return actions;
    }
    
    /**
     * Converts {@link ArgsManager#getOriginalArgs()} to a {@link List} and
     * returns.
     * 
     * @return The command-line arguments used to start McIDAS-V, as an 
     * {@code ArrayList}.
     */
    public List<String> getCommandLineArgs() {
        String[] originalArgs = getArgsManager().getOriginalArgs();
        List<String> args = arrList(originalArgs.length);
        Collections.addAll(args, originalArgs);
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
        
        boolean result = false;
        DateTime start = DateTime.now();
        logger.trace("started: action='{}', checkForAlias={}, properties='{}'", action, checkForAlias, properties);
        if (IOUtil.isHtmlFile(action)) {
            WebBrowser.browse(action);
            result = true;
        } else {
            if (action.toLowerCase().contains("showsupportform")) {
                logger.trace("showing support form 'manually'...");
                getIdvUIManager().showSupportForm();
                result = true;
            } else {
                result = super.handleAction(action, properties, checkForAlias);
            }
        }
        long duration = new DateTime().minus(start.getMillis()).getMillis();
        logger.trace("finished: action='{}', duration: {} (ms), checkForAlias={}, properties='{}'", action, duration, checkForAlias, properties);
        
        return result;
    }
    
    /**
     * This method checks if the given action is one of the following.
     * <ul>
     *   <li>Jython code: starts with {@literal "jython:"}.</li>
     *   <li>Help link: starts with {@literal "help:"}.</li>
     *   <li>Resource bundle file: ends with {@literal ".rbi"}.</li>
     *   <li>Bundle file: ends with {@literal ".xidv"}.</li>
     *   <li>JNLP file: ends with {@literal ".jnlp"}.</li>
     * </ul>
     *
     * <p>It returns {@code true} if the action is one of these. {@code false}
     * otherwise.</p>
     *
     * @param action The string action
     * @param properties any properties
     *
     * @return {@code true} if the action was {@literal "handled"};
     * {@code false} otherwise.
     */
    @Override protected boolean handleFileOrUrlAction(String action,
                                                      Hashtable properties) {
        boolean result = false;
        boolean idvAction = action.startsWith("idv:");
        boolean jythonAction = action.startsWith("jython:");
        
        if (!idvAction && !jythonAction) {
            return super.handleFileOrUrlAction(action, properties);
        }
        
        Map<String, Object> hashProps;
        if (properties != null) {
            hashProps = new HashMap<>(properties);
        } else {
            //noinspection CollectionWithoutInitialCapacity
            hashProps = new HashMap<>();
        }
        
        ucar.unidata.idv.JythonManager jyManager = getJythonManager();
        if (idvAction) {
            action = action.replace("&", "&amp;").substring(4);
            jyManager.evaluateUntrusted(action, hashProps);
            result = true;
        } else if (jythonAction) {
            action = action.substring(7);
            jyManager.evaluateAction(action, hashProps);
            result = true;
        } else {
            result = super.handleFileOrUrlAction(action, properties);
        }
        return result;
    }
    
    /**
     * Add a new {@link ControlDescriptor} into the {@code controlDescriptor}
     * list and {@code controlDescriptorMap}.
     *
     * <p>This method differs from the IDV's in that McIDAS-V <b>overwrites</b>
     * existing {@code ControlDescriptor ControlDescriptors} if
     * {@link ControlDescriptor#getControlId()} matches.
     *
     * @param cd The ControlDescriptor to add.
     *
     * @throws NullPointerException if {@code cd} is {@code null}.
     */
    @Override protected void addControlDescriptor(ControlDescriptor cd) {
        cd = Objects.requireNonNull(cd, "Cannot add a null control descriptor to the list of control descriptors.");
        String id = cd.getControlId();
        if (controlDescriptorMap.get(id) == null) {
            controlDescriptors.add(cd);
            controlDescriptorMap.put(id, cd);
        } else {
            for (int i = 0; i < controlDescriptors.size(); i++) {
                ControlDescriptor tmp = (ControlDescriptor)controlDescriptors.get(i);
                if (tmp.getControlId().equals(id)) {
                    controlDescriptors.set(i, cd);
                    controlDescriptorMap.put(id, cd);
                    break;
                }
            }
        }
    }
    
    /**
     * Get the {@code Hashtable} that maps {@link ControlDescriptor} IDs to the
     * {@code ControlDescriptor} instance.
     * 
     * <p>Mostly used as a convenient way to check if a given 
     * {@code ControlDescriptor} has already been processed.</p>
     * 
     * @return {@code Hashtable} that maps {@code String} IDs to the 
     *         corresponding {@code ControlDescriptor}. 
     */
    public Hashtable<String, ControlDescriptor> getControlDescriptorMap() {
        return controlDescriptorMap;
    }
    
    // pop up an incredibly rudimentary window that controls layer viz animation.
    public void showLayerVisibilityAnimator() {
        logger.trace("probably should try to do something here.");
        SwingUtilities.invokeLater(() -> {
            try {
                LayerAnimationWindow window = new LayerAnimationWindow();
                window.setVisible(true);
            } catch (Exception e) {
                logger.error("oh no! something happened!", e);
            }
        });
    }
    
    /**
     * Handles removing all loaded data sources.
     * 
     * <p>If {@link ArgsManager#getIsOffScreen()} is {@code true}, this method
     * will ignore the user's preferences and remove all data sources.
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
    public boolean removeAllData(final boolean showWarning) {
        boolean reallyRemove = false;
        boolean continueWarning = true;
        
        if (getArgsManager().getIsOffScreen()) {
            super.removeAllDataSources();
            return continueWarning;
        }
        
        if (showWarning) {
            Set<WarningResult> result = showWarningDialog(
                "Confirm Data Removal",
                "This action will remove all of the data currently loaded in McIDAS-V.<br>Is this what you want to do?",
                Constants.PREF_CONFIRM_REMOVE_DATA,
                "Always ask?",
                "Remove all data",
                "Do not remove any data");
            reallyRemove = result.contains(WarningResult.OK);
            continueWarning = result.contains(WarningResult.SHOW);
        } else {
            // user doesn't want to see warning messages.
            reallyRemove = true;
            continueWarning = false;
        }
        
        if (reallyRemove) {
            super.removeAllDataSources();
        }
        
        return continueWarning;
    }
    
    /**
     * Handles removing all loaded layers ({@literal "displays"} in IDV-land).
     * 
     * <p>If {@link ArgsManager#getIsOffScreen()} is {@code true}, this method
     * will ignore the user's preferences and remove all layers.
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
    public boolean removeAllLayers(final boolean showWarning) {
        boolean reallyRemove = false;
        boolean continueWarning = true;
        
        if (getArgsManager().getIsOffScreen()) {
            super.removeAllDisplays();
            ((ViewManagerManager)getVMManager()).disableAllLayerVizAnimations();
            return continueWarning;
        }
        
        if (showWarning) {
            Set<WarningResult> result = showWarningDialog(
                "Confirm Layer Removal",
                "This action will remove every layer currently loaded in McIDAS-V.<br>Is this what you want to do?",
                Constants.PREF_CONFIRM_REMOVE_LAYERS,
                "Always ask?",
                "Remove all layers",
                "Do not remove any layers");
            reallyRemove = result.contains(WarningResult.OK);
            continueWarning = result.contains(WarningResult.SHOW);
        } else {
            // user doesn't want to see warning messages.
            reallyRemove = true;
            continueWarning = false;
        }
        
        if (reallyRemove) {
            super.removeAllDisplays();
            ((ViewManagerManager)getVMManager()).disableAllLayerVizAnimations();
        }
        
        return continueWarning;
    }
    
    /**
     * Overridden so that McIDAS-V can prompt the user before removing, if 
     * necessary.
     */
    @Override public void removeAllDataSources() {
        IdvObjectStore store = getStore();
        boolean showWarning =
            store.get(Constants.PREF_CONFIRM_REMOVE_DATA, true);
        showWarning = removeAllData(showWarning);
        store.put(Constants.PREF_CONFIRM_REMOVE_DATA, showWarning);
    }
    
    /**
     * Overridden so that McIDAS-V can prompt the user before removing, if 
     * necessary.
     */
    @Override public void removeAllDisplays() {
        IdvObjectStore store = getStore();
        boolean showWarning =
            store.get(Constants.PREF_CONFIRM_REMOVE_LAYERS, true);
        showWarning = removeAllLayers(showWarning);
        store.put(Constants.PREF_CONFIRM_REMOVE_LAYERS, showWarning);
    }
    
    /**
     * Handles removing all loaded layers ({@literal "displays"} in IDV-land)
     * and data sources. 
     * 
     * <p>If {@link ArgsManager#getIsOffScreen()} is {@code true}, this method
     * will ignore the user's preferences and remove all layers and data.
     * 
     * @see #removeAllData(boolean)
     * @see #removeAllLayers(boolean)
     */
    public void removeAllLayersAndData() {
        boolean reallyRemove = false;
        boolean continueWarning = true;
        
        if (getArgsManager().getIsOffScreen()) {
            removeAllData(false);
            removeAllLayers(false);
        }
        
        IdvObjectStore store = getStore();
        boolean showWarning = store.get(Constants.PREF_CONFIRM_REMOVE_BOTH, true);
        if (showWarning) {
            Set<WarningResult> result = showWarningDialog(
                "Confirm Removal",
                "This action will remove all of your currently loaded layers and data.<br>Is this what you want to do?",
                Constants.PREF_CONFIRM_REMOVE_BOTH,
                "Always ask?",
                "Remove all layers and data",
                "Do not remove anything");
            reallyRemove = result.contains(WarningResult.OK);
            continueWarning = result.contains(WarningResult.SHOW);
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
        
        store.put(Constants.PREF_CONFIRM_REMOVE_BOTH, continueWarning);
    }
    
    /**
     * Helper method for showing the removal warning dialog. Note that none of
     * these parameters should be {@code null} or empty.
     * 
     * @param title Title of the warning dialog.
     * @param message Contents of the warning. May contain HTML, but you do 
     * not need to provide opening and closing {@literal "html"} tags.
     * @param prefId ID of the preference that controls whether or not the 
     * dialog should be displayed.
     * @param prefLabel Brief description of the preference.
     * @param okLabel Text of button that signals removal.
     * @param cancelLabel Text of button that signals cancelling removal.
     * 
     * @return A {@code Set} of {@link WarningResult WarningResults} that
     * describes what the user opted to do. Should always contain only
     * <b>two</b> elements. One for whether or not {@literal "ok"} or
     * {@literal "cancel"} was clicked, and one for whether or not the warning
     * should continue to be displayed.
     */
    private Set<WarningResult> showWarningDialog(final String title, 
        final String message, final String prefId, final String prefLabel, 
        final String okLabel, final String cancelLabel) 
    {
        JCheckBox box = new JCheckBox(prefLabel, true);
        JComponent comp = GuiUtils.vbox(
            new JLabel("<html>"+message+"</html>"), 
            GuiUtils.inset(box, new Insets(4, 15, 0, 10)));
            
        Object[] options = { okLabel, cancelLabel };
        int result = JOptionPane.showOptionDialog(
            LogUtil.getCurrentWindow(),  // parent
            comp,                        // msg
            title,                       // title
            JOptionPane.YES_NO_OPTION,   // option type
            JOptionPane.WARNING_MESSAGE, // message type
            (Icon)null,                  // icon?
            options,                     // selection values
            options[1]);                 // initial?
            
        WarningResult button = WarningResult.CANCEL;
        if (result == JOptionPane.YES_OPTION) {
            button = WarningResult.OK;
        }
        
        WarningResult show = WarningResult.HIDE;
        if (box.isSelected()) {
            show = WarningResult.SHOW;
        }
        
        return EnumSet.of(button, show);
    }
    
    public void removeTabData() {
    }
    
    public void removeTabLayers() {
        
    }
    
    public void removeTabLayersAndData() {
    }
    
    /**
     * Overridden so that McIDAS-V doesn't have to create an entire new
     * {@link ucar.unidata.idv.ui.IdvWindow} if
     * {@link VMManager#findViewManager(ViewDescriptor)} can't find an
     * appropriate ViewManager for {@code viewDescriptor}.
     * 
     * <p>Not doing the above causes McIDAS-V to get stuck in a window creation
     * loop.</p>
     */
    @Override public ViewManager getViewManager(ViewDescriptor viewDescriptor,
        boolean newWindow, String properties) 
    {
        ViewManager vm = 
            getVMManager().findOrCreateViewManager(viewDescriptor, properties);
        if (vm == null) {
            vm = super.getViewManager(viewDescriptor, newWindow, properties);
        }
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
    @Override public void setIdv(IntegratedDataViewer idv) {
        this.idv = idv;
    }
    
    /**
     * Load the McV properties. All other property files are disregarded.
     * 
     * @see ucar.unidata.idv.IntegratedDataViewer#initPropertyFiles(java.util.List)
     */
    @Override public void initPropertyFiles(List files) {
        files.clear();
        files.add(Constants.PROPERTIES_FILE);
    }
    
    /**
     * Makes {@link PersistenceManager} save off a default {@literal "layout"}
     * bundle.
     */
    public void doSaveAsDefaultLayout() {
        Misc.run(() -> ((PersistenceManager)getPersistenceManager()).doSaveAsDefaultLayout());
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
                "Delete confirmation")) {
            resourceManager.clearDefaultBundles();
        }
    }
    
    /**
     * Returns the time it took for McIDAS-V to start up.
     *
     * @return These results are created from subtracting the results of two
     * {@link System#nanoTime()} calls against one another.
     */
    public long getStartupDuration() {
        return estimate;
    }
    
    /**
     * <p>
     * Overridden so that the support form becomes non-modal if launched from
     * an exception dialog.
     * </p>
     * 
     * @see IntegratedDataViewer#addErrorButtons(JDialog, List, String, Throwable)
     */
    @Override public void addErrorButtons(final JDialog dialog, 
        List buttonList, final String msg, final Throwable exc) 
    {
        JButton supportBtn = new JButton("Support Form");
        supportBtn.addActionListener(ae ->
            getIdvUIManager().showSupportForm(msg,
                                              LogUtil.getStackTrace(exc),
                                              null));
        buttonList.add(supportBtn);
    }
    
    /**
     * This method is useful for storing commandline {@literal "properties"}
     * with the user's preferences.
     */
    private void overridePreferences() {
        StateManager stateManager = (StateManager)getStateManager();
        int renderThreads = getMaxRenderThreadCount();
        stateManager.putPreference(PREF_THREADS_RENDER, renderThreads);
        stateManager.putPreference(PREF_THREADS_DATA, getMaxDataThreadCount());
        visad.util.ThreadManager.setGlobalMaxThreads(renderThreads);
    }
    
    /**
     * Determine if the last {@literal "exit"} was clean--whether or not
     * {@code SESSION_FILE} was removed before the McIDAS-V process terminated.
     *
     * <p>If the exit was not clean, the user is prompted to submit a support
     * request.</p>
     */
    private void detectAndHandleCrash() {
        GuiUtils.setApplicationTitle("");
        if (cleanExit || getArgsManager().getIsOffScreen()) {
            return;
        }
        
        String msg = "The previous McIDAS-V session did not exit cleanly.<br>"+
            "Do you want to send the log file to the McIDAS Help Desk?";
        if (previousStart != null) {
            msg = "The previous McIDAS-V session (start time: %s) did not exit cleanly.<br>"+
                "Do you want to send the log file to the McIDAS Help Desk?";
            msg = String.format(msg, previousStart);
        }
        
        boolean continueAsking = getStore().get("mcv.crash.boom.send.report", true);
        if (!continueAsking) {
            return;
        }
        
        Set<WarningResult> result = showWarningDialog(
            "Report Crash",
            msg,
            "mcv.crash.boom.send.report",
            "Always ask?",
            "Open support form",
            "Do not report");
            
        getStore().put("mcv.crash.boom.send.report", result.contains(WarningResult.SHOW));
        if (!result.contains(WarningResult.OK)) {
            return;
        }
        
        getIdvUIManager().showSupportForm();
    }
    
    /**
     * Called after the IDV has finished setting everything up after starting.
     * McIDAS-V is currently only using this method to determine if the last
     * {@literal "exit"} was clean--whether or not {@code SESSION_FILE} was 
     * removed before the McIDAS-V process terminated.
     *
     * Called after the IDV has finished setting everything up. McIDAS-V uses
     * this method to handle:
     *
     * <ul>
     *   <li>Clearing out the automatic display creation arguments.</li>
     *   <li>Presence of certain properties on the commandline.</li>
     *   <li>Detection and handling of a crashed McIDAS-V session.</li>
     *   <li>Run action specified by {@code -doaction} flag (if any).</li>
     *   <li>Allowing tooltips to remain visible for more than 4 seconds.</li>
     *   <li>Apply preferences</li>
     * </ul>
     *
     * @see ArgumentManager#clearAutomaticDisplayArgs()
     * @see #overridePreferences()
     * @see #detectAndHandleCrash()
     */
    @Override public void initDone() {
        ((ArgumentManager)argsManager).clearAutomaticDisplayArgs();
        
        overridePreferences();
        
        detectAndHandleCrash();
        
        if (addeEntries == null) {
            getServerManager();
        }
        addeEntries.startLocalServer();
        
        estimate = System.nanoTime() - startTime;
        logger.info("estimated startup duration: {} ms", estimate / 1.0e6);
        System.setProperty("mcv.start.duration", Long.toString(estimate));
        
        // handle the -doAction <action id> startup option.
        ((ArgumentManager)getArgsManager()).runStartupAction();
        
        // disable idiotic tooltip dismissal (seriously, 4 seconds!?)
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        
        // turn on directory monitoring in the file choosers.
        startWatchService();
        EventBus.publish(Constants.EVENT_FILECHOOSER_START, "init finished");
        
        EventDispatchThreadHangMonitor.initMonitoring();

        // McIDAS Inquiry #983-3141 -> I really don't like how this is implemented
//        SwingUtilities.invokeLater(() -> {
//            try {
//                ((McIdasPreferenceManager) doMakePreferenceManager()).initAtStartup();
//            } catch (Exception e) {
//                logger.error("oh no! something happened!", e);
//            }
//        });
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
     * Have the user select a bundle. If andRemove is true then we remove all
     * data sources and displays.
     *
     * Then we open the bundle and start doing unpersistence things.
     *
     * @param filename The filename to open
     * @param checkUserPreference Should we show, if needed, the
     * {@literal "open"} dialog
     * @param andRemove If true then first remove all data sources and displays
     */
    private void doOpenInThread(String filename, boolean checkUserPreference,
        boolean andRemove) 
    {
        boolean overwriteData = false;
        if (filename == null) {
            if (overwriteDataCbx.getToolTipText() == null) {
                overwriteDataCbx.setToolTipText("Change the file paths that the data sources use");
            }
            
            filename = FileManager.getReadFile("Open File",
                ((ArgumentManager)getArgsManager()).getBundleFilters(true), 
                GuiUtils.top(overwriteDataCbx));
                
            if (filename == null) {
                return;
            }
            
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
     * Factory method to create the McIDAS-V @link JythonManager}.
     *
     * @return New {@code JythonManager}.
     */
    @Override protected JythonManager doMakeJythonManager() {
        logger.debug("returning a new JythonManager");
        return new JythonManager(this);
    }
    
    /**
     * Factory method to create a McIDAS-V {@link CollaborationManager}.
     *
     * @return New {@code CollaborationManager}.
     */
    @Override protected CollabManager doMakeCollabManager() {
        return new CollaborationManager(this);
    }
    
    /**
     * Factory method to create a McIDAS-V {@link McIdasChooserManager}.
     * Here we create our own manager so it can do things specific to McIDAS-V.
     *
     * @return {@code McIdasChooserManager} indicated by the startup properties.
     * 
     * @see ucar.unidata.idv.IdvBase#doMakeIdvChooserManager()
     */
    @Override
    protected IdvChooserManager doMakeIdvChooserManager() {
        chooserManager = (McIdasChooserManager)makeManager(
            McIdasChooserManager.class, new Object[] { this });
        chooserManager.init();
        return chooserManager;
    }
    
    /**
     * Factory method to create the {@link IdvUIManager}. Here we create our
     * own UI manager so it can do things specific to McIDAS-V.
     *
     * @return {@link UIManager} indicated by the startup properties.
     * 
     * @see ucar.unidata.idv.IdvBase#doMakeIdvUIManager()
     */
    @Override
    protected IdvUIManager doMakeIdvUIManager() {
        return new UIManager(this);
    }
    
    /**
     * Create our own VMManager so that we can make the tabs play nice.
     * @see ucar.unidata.idv.IdvBase#doMakeVMManager()
     */
    @Override
    protected VMManager doMakeVMManager() {
        // what an ugly class name :(
        return new ViewManagerManager(this);
    }
    
    /**
     * Make the {@link McIdasPreferenceManager}.
     * @see ucar.unidata.idv.IdvBase#doMakePreferenceManager()
     */
    @Override
    protected IdvPreferenceManager doMakePreferenceManager() {
        return new McIdasPreferenceManager(this);
    }
    
    /**
     * <p>McIDAS-V (alpha 10+) needs to handle both IDV bundles without 
     * component groups and all bundles from prior McV alphas. You better 
     * believe we need to extend the persistence manager functionality!</p>
     * 
     * @see ucar.unidata.idv.IdvBase#doMakePersistenceManager()
     */
    @Override protected IdvPersistenceManager doMakePersistenceManager() {
        return new PersistenceManager(this);
    }
    
    /**
     * Create, if needed, and return the {@link McIdasChooserManager}.
     * 
     * @return The Chooser manager
     */
    public McIdasChooserManager getMcIdasChooserManager() {
        return (McIdasChooserManager)getIdvChooserManager();
    }
    
    /**
     * Returns the {@link MonitorManager}.
     *
     * @return McIDAS-V {@literal "monitor manager"}.
     */
    public MonitorManager getMonitorManager() {
        return monitorManager;
    }
    
    /**
     * Responds to events generated by the server manager's GUI. Currently
     * limited to {@link edu.wisc.ssec.mcidasv.servermanager.TabbedAddeManager.Event#CLOSED TabbedAddeManager.Event#CLOSED}.
     *
     * @param evt {@code TabbedAddeManager} event to respond to.
     */
    @EventSubscriber(eventClass=TabbedAddeManager.Event.class)
    public void onServerManagerWindowEvent(TabbedAddeManager.Event evt) {
        if (evt == TabbedAddeManager.Event.CLOSED) {
            tabbedAddeManager = null;
        }
    }
    
    /**
     * Creates (if needed) the server manager GUI and displays it.
     */
    public void showServerManager() {
        if (tabbedAddeManager == null) {
            tabbedAddeManager = new TabbedAddeManager(getServerManager());
        }
        tabbedAddeManager.showManager();
    }
    
    /**
     * Creates a new server manager (if needed) and returns it.
     *
     * @return The McIDAS-V ADDE server manager.
     */
    public EntryStore getServerManager() {
        if (addeEntries == null) {
            addeEntries = new EntryStore(getStore(), getResourceManager());
        }
        return addeEntries;
    }
    
    public McvDataManager getMcvDataManager() {
        return (McvDataManager)getDataManager();
    }
    
    /**
     * Get McIDASV. 
     * @see ucar.unidata.idv.IdvBase#getIdv()
     */
    @Override public IntegratedDataViewer getIdv() {
        return this;
    }
    
    /**
     * Creates a McIDAS-V argument manager so that McV can handle some non-IDV
     * command line things.
     * 
     * @param args The arguments from the command line.
     * 
     * @see ucar.unidata.idv.IdvBase#doMakeArgsManager(java.lang.String[])
     */
    @Override protected ArgsManager doMakeArgsManager(String[] args) {
        return new ArgumentManager(this, args);
    }
    
    /**
     * Factory method to create the {@link McvDataManager}.
     * 
     * @return The data manager
     * 
     * @see ucar.unidata.idv.IdvBase#doMakeDataManager()
     */
    @Override protected DataManager doMakeDataManager() {
        return new McvDataManager(this);
    }
    
    /**
     * Make the McIDAS-V {@link StateManager}.
     * @see ucar.unidata.idv.IdvBase#doMakeStateManager()
     */
    @Override protected StateManager doMakeStateManager() {
        return new StateManager(this);
    }
    
    /**
     * Make the McIDAS-V {@link ResourceManager}.
     * @see ucar.unidata.idv.IdvBase#doMakeResourceManager()
     */
    @Override protected IdvResourceManager doMakeResourceManager() {
        return new ResourceManager(this);
    }
    
    /**
     * Make the {@link McIdasColorTableManager}.
     * @see ucar.unidata.idv.IdvBase#doMakeColorTableManager()
     */
    @Override protected ColorTableManager doMakeColorTableManager() {
        return new McIdasColorTableManager();
    }
    
    /**
     * Factory method to create the {@link McvPluginManager}.
     *
     * @return The McV plugin manager.
     * 
     * @see ucar.unidata.idv.IdvBase#doMakePluginManager()
     */
    @Override protected PluginManager doMakePluginManager() {
        return new McvPluginManager(this);
    }
    
//    /**
//     * Make the {@link edu.wisc.ssec.mcidasv.data.McIDASVProjectionManager}.
//     * @see ucar.unidata.idv.IdvBase#doMakeIdvProjectionManager()
//     */
//    @Override
//    protected IdvProjectionManager doMakeIdvProjectionManager() {
//    	return new McIDASVProjectionManager(this);
//    }
    
    /**
     * Make a help button for a particular help topic
     *
     * @param helpId  the topic id
     * @param toolTip  the tooltip
     *
     * @return  the button
     */
    @Override public JComponent makeHelpButton(String helpId, String toolTip) {
        JButton btn = McVGuiUtils.makeImageButton(Constants.ICON_HELP,
            getIdvUIManager(), "showHelp", helpId, "Show help");
            
        if (toolTip != null) {
            btn.setToolTipText(toolTip);
        }
        return btn;
    }
    
    /**
     * Return the current {@literal "userpath"}.
     * 
     * @return Path to the user's {@literal "McIDAS-V directory"}.
     */
    public String getUserDirectory() {
        return StartupManager.getInstance().getPlatform().getUserDirectory();
    }
    
    /**
     * Return the path to a file within {@literal "userpath"}.
     * 
     * @param filename File within the userpath.
     * 
     * @return Path to a file within the user's {@literal "McIDAS-V directory"}.
     * No path validation is performed, so please be aware that the returned
     * path may not exist.
     */
    public String getUserFile(String filename) {
        return StartupManager.getInstance().getPlatform().getUserFile(filename);
    }
    
    /**
     * Invokes the main method for a given class. 
     * 
     * <p>Note: this is rather limited so far as it doesn't pass in any
     * arguments.</p>
     * 
     * @param className Class whose main method is to be invoked. Cannot be
     * {@code null}.
     */
    public void runPluginMainMethod(final String className) {
        final String[] dummyArgs = { "" };
        try {
            Class<?> clazz = Misc.findClass(className);
            Class[] args = new Class[] { dummyArgs.getClass() };
            Method mainMethod = Misc.findMethod(clazz, "main", args);
            if (mainMethod != null) {
                mainMethod.invoke(null, new Object[] { dummyArgs });
            }
        } catch (Exception e) {
            logger.error("problem with plugin class", e);
            LogUtil.logException("problem running main method for class: "+className, e);
        }
    }
    
    /**
     * Attempts to determine if a given string is a 
     * {@literal "loopback address"} (aka localhost).
     * 
     * <p>Strings are <b>trimmed and converted to lowercase</b>, and currently
     * checked against:
     * <ul>
     * <li>{@code 127.0.0.1}</li>
     * <li>{@code ::1} (for IPv6)</li>
     * <li>Strings starting with {@code localhost}.</li>
     * </ul>
     * 
     * @param host {@code String} to check. Should not be {@code null}.
     * 
     * @return {@code true} if {@code host} is a recognized loopback address.
     * {@code false} otherwise.
     * 
     * @throws NullPointerException if {@code host} is {@code null}.
     */
    public static boolean isLoopback(final String host) {
        String cleaned = Objects.requireNonNull(host.trim().toLowerCase());
        return "127.0.0.1".startsWith(cleaned) 
            || "::1".startsWith(cleaned) 
            || cleaned.startsWith("localhost");
    }
    
    /**
     * Are we on a Mac? Used to build the MRJ handlers, taken from TN2110.
     * 
     * @return {@code true} if this session is running on top of OS X,
     * {@code false} otherwise.
     * 
     * @see <a href="http://developer.apple.com/technotes/tn2002/tn2110.html">TN2110</a>
     */
    public static boolean isMac() {
        String osName = System.getProperty("os.name");
        return osName.contains("OS X");
    }
    
    /**
     * Queries the {@code os.name} system property and if the result does not 
     * start with {@literal "Windows"}, the platform is assumed to be 
     * {@literal "unix-like"}.
     * 
     * <p>Given the McIDAS-V supported platforms (Windows, {@literal "Unix"}, 
     * and OS X), the above logic is safe.
     * 
     * @return {@code true} if we're not running on Windows, {@code false} 
     * otherwise.
     * 
     * @throws RuntimeException if there is no property associated with 
     * {@code os.name}.
     */
    public static boolean isUnixLike() {
        String osName = System.getProperty("os.name");
        if (osName == null) {
            throw new RuntimeException("no os.name system property!");
        }
        
        if (System.getProperty("os.name").startsWith("Windows")) {
            return false;
        }
        return true;
    }
    
    /**
     * Queries the {@code os.name} system property and if the result starts 
     * with {@literal "Windows"}, the platform is assumed to be Windows. Duh.
     * 
     * @return {@code true} if we're running on Windows, {@code false} 
     * otherwise.
     * 
     * @throws RuntimeException if there is no property associated with 
     * {@code os.name}.
     */
    public static boolean isWindows() {
        String osName = System.getProperty("os.name");
        if (osName == null) {
            throw new RuntimeException("no os.name system property!");
        }
        
        return osName.startsWith("Windows");
    }
    
    /**
     * If McIDAS-V is running on Windows, this method will return a 
     * {@code String} that looks like {@literal "C:"} or {@literal "D:"}, etc.
     * 
     * <p>If McIDAS-V is not running on Windows, this method will return an
     * empty {@code String}.
     * 
     * @return Either the {@literal "drive letter"} of the {@code java.home} 
     * property or an empty {@code String} if McIDAS-V isn't running on Windows.
     * 
     * @throws RuntimeException if there is no property associated with 
     * {@code java.home}.
     */
    public static String getJavaDriveLetter() {
        if (!isWindows()) {
            return "";
        }
        
        String home = System.getProperty("java.home");
        if (home == null) {
            throw new RuntimeException("no java.home system property!");
        }
        
        return home.substring(0, 2);
    }
    
    /**
     * Attempts to create a {@literal "session"} file. This method will create
     * a {@literal "userpath"} if it does not already exist. 
     * 
     * @param path Path of the session file that should get created. 
     * {@code null} values are not allowed, and sufficient priviledges are 
     * assumed.
     * 
     * @throws AssertionError if McIDAS-V couldn't write to {@code path}.
     * 
     * @see #SESSION_FILE
     * @see #hadCleanExit(String)
     * @see #removeSessionFile(String)
     */
    private static void createSessionFile(final String path) {
        assert path != null : "Cannot create a null path";
        try (
            FileOutputStream out = new FileOutputStream(path);
            PrintStream p = new PrintStream(out)
        ) {
            p.println(System.currentTimeMillis());
        } catch (Exception e) {
            throw new AssertionError("Could not write to "+path+". Error message: "+e.getMessage(), e);
        }
    }
    
    /**
     * Attempts to extract a timestamp from {@code path}. {@code path} is 
     * expected to <b>only</b> contain a single line consisting of a 
     * {@link Long} integer.
     * 
     * @param path Path to the file of interest.
     * 
     * @return Either a {@link Date} of the timestamp contained in 
     * {@code path} or {@code null} if the extraction failed.
     */
    private static Date extractDate(final String path) {
        assert path != null;
        Date savedDate = null;
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String line = r.readLine();
            if (line != null) {
                savedDate = new Date(Long.parseLong(line.trim()));
            }
        } catch (Exception e) {
            logger.warn("Could not extract date from session file", e);
        }
        return savedDate;
    }
    
    /**
     * Attempts to remove the file accessible via {@code path}.
     * 
     * @param path Path of the file that'll get removed. This should be 
     * non-null and point to an existing and writable filename (not a 
     * directory).
     * 
     * @throws AssertionError if the file at {@code path} could not be 
     * removed.
     * 
     * @see #SESSION_FILE
     * @see #createSessionFile(String)
     * @see #hadCleanExit(String)
     */
    private static void removeSessionFile(final String path) {
        if (path == null) {
            return;
        }
        
        File f = new File(path);
        
        if (!f.exists() || !f.canWrite() || f.isDirectory()) {
            return;
        }
        
        if (!f.delete()) {
            throw new AssertionError("Could not delete session file");
        }
    }
    
    /**
     * Tries to determine whether or not the last McIDAS-V session ended 
     * {@literal "cleanly"}. Currently a simple check for a 
     * {@literal "session"} file that is created upon starting and removed upon
     * ending.
     * 
     * @param path Path to the session file to check. Can't be {@code null}.
     * 
     * @return Either {@code true} if the file pointed at by {@code path} does
     * <b><i>NOT</i></b> exist, {@code false} if it does exist.
     * 
     * @see #SESSION_FILE
     * @see #createSessionFile(String)
     * @see #removeSessionFile(String)
     */
    private static boolean hadCleanExit(final String path) {
        assert path != null : "Cannot test for a null path";
        return !(new File(path).exists());
    }
    
    /**
     * Returns the (<i>current</i>) path to the session file. Note that the
     * location of the file may change arbitrarily.
     * 
     * @return {@code String} pointing to the session file.
     * 
     * @see #SESSION_FILE
     */
    public static String getSessionFilePath() {
        return StartupManager.getInstance().getPlatform().getUserFile("session.tmp");
    }
    
    /**
     * Useful for providing the startup manager with values other than the 
     * defaults... Note that this method attempts to update the value of 
     * {@link #SESSION_FILE}.
     * 
     * @param args Likely the argument array coming from the main method.
     */
    private static void applyArgs(final String[] args) {
        assert args != null : "Cannot use a null argument array";
        StartupManager.applyArgs(true, false, args);
        SESSION_FILE = getSessionFilePath();
    }
    
    /**
     * This returns the set of {@link ControlDescriptor ControlDescriptors}
     * that can be shown. The ordering of this list determines the
     * "default" controls shown in the Field Selector, so we override
     * here for control over the ordering.
     *
     * @param includeTemplates If true then include the display templates
     * @return re-ordered List of shown control descriptors
     */
    @Override public List getControlDescriptors(boolean includeTemplates) {
        List<ControlDescriptor> l = 
            cast(super.getControlDescriptors(includeTemplates));
        for (int i = 0; i < l.size(); i++) {
            ControlDescriptor cd = l.get(i);
            if (cd.getControlId().equals("omni")) {
                // move the omni control to the end of the list
                // so it will never be "default" in Field Selector
                l.remove(i);
                l.add(cd);
            }
        }
        return l;
    }
    
    /**
     * Show the McIDAS-V {@literal "Welcome Window"} for the first start up.
     *
     * @param args Commandline arguments, used to handle autoquit stress testing.
     */
    private static void handleWelcomeWindow(String... args) {
        boolean showWelcome = false;
        boolean welcomeAutoQuit = false;
        long welcomeAutoQuitDelay = WelcomeWindow.DEFAULT_QUIT_DELAY;
        for (int i = 0; i < args.length; i++) {
            if ("-welcomewindow".equals(args[i])) {
                showWelcome = true;
            } else if ("-autoquit".equals(args[i])) {
                welcomeAutoQuit = true;
                int delayIdx = i + 1;
                if (delayIdx < args.length) {
                    welcomeAutoQuitDelay = Long.parseLong(args[delayIdx]);
                }
            }
        }
        
        // if user elects to quit, System.exit(1) will be called.
        // if the user decides to start, the welcome window will be simply be
        // closed, allowing McV to continue starting up.
        if (showWelcome) {
            WelcomeWindow ww;
            if (welcomeAutoQuit) {
                ww = new WelcomeWindow(true, welcomeAutoQuitDelay);
            } else {
                ww = new WelcomeWindow();
            }
            ww.setVisible(true);
        }
    }
    
    /**
     * Register {@literal "adde"} and {@literal "idvresource"} URL protocols.
     *
     * <p>This needs to be called pretty early in the McIDAS-V initialization
     * process. They're currently being registered immediately after the
     * session file is created.</p>
     */
    private static void registerProtocolHandlers() {
        try {
            URL.setURLStreamHandlerFactory(protocol -> {
                switch (protocol.toLowerCase()) {
                    case AddeURL.ADDE_PROTOCOL:
                        return new AddeURLStreamHandler();
                    case PluginManager.PLUGIN_PROTOCOL:
                        return new IdvResourceStreamHandler();
                    default:
                        return null;
                }
            });
        } catch (Throwable e) {
            logger.error("Could not register protocol handlers!", e);
        }
    }
    
    /**
     * Responsible for handling {@literal "idvresource"} URLs.
     *
     * <p>Really just a redirect to {@link IOUtil#getURL(String, Class)}.</p>
     */
    private static class IdvResourceStreamHandler extends URLStreamHandler {
        @Override protected URLConnection openConnection(URL u)
            throws IOException
        {
            return IOUtil.getURL(u.getPath(), McIDASV.class).openConnection();
        }
    }

    /**
     * Show the new HYDRA DataBrowser GUI.
     *
     * <p>Mostly for ease of use via a toolbar action.</p>
     */
    public void showNewHydra() {
        try {
            DataBrowser.main(new String[]{});
        } catch (Exception e) {
            logger.warn("Exception launching HYDRA data browser", e);
        }
    }

    /**
     * Check the value of the {@code mcidasv.darkmode} system property.
     *
     * @return {@code true} if dark mode should be enabled, {@code false} otherwise.
     */
    public static boolean isDarkMode() {
        return Boolean.parseBoolean(System.getProperty("mcidasv.darkmode", "false"));
    }

    /**
     * Configure the logging and create the McIDAS-V object responsible for
     * initializing the application session.
     *
     * @param args Command line arguments.
     *
     * @throws Exception When something untoward happens.
     */
    public static void main(String... args) throws Exception {
        // show the welcome window if needed.
        // since the welcome window is intended to be a one time thing,
        // it probably shouldn't count for the startTime stuff.
        handleWelcomeWindow(args);
        
        startTime = System.nanoTime();
        
        // allow use of the "unlimited strength" crypto policy.
        // this becomes the default in 1.8.0_162, but we need to ship with
        // 1.8.0_152.
        Security.setProperty("crypto.policy", "unlimited");

        // Configure GUI look and feel before any UI stuff
        if (isDarkMode()) {
            FlatDarkLaf.setup();
        }

        // the following two lines are required if we want to embed JavaFX
        // widgets into McV (which is Swing). The first line initializes the
        // JavaFX runtime, and the second line allows the JavaFX runtime to
        // hang around even if there are no JavaFX windows.
         JFXPanel dummy = new JFXPanel();
         Platform.setImplicitExit(false);
        
        try {
            applyArgs(args);
            
            SysOutOverSLF4J.sendSystemOutAndErrToSLF4J(LogLevel.INFO, LogLevel.WARN);
            
            // Optionally remove existing handlers attached to j.u.l root logger
            SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
            
            // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
            // the initialization phase of your application
            SLF4JBridgeHandler.install();
            
//            Properties pythonProps = new Properties();
//            logger.trace("calling PythonInterpreter.initialize...");
//            PythonInterpreter.initialize(System.getProperties(), pythonProps, new String[] {""});
            
            LogUtil.configure();
            
            // Register IOSPs here
            
            // TJJ Jan 2019 - new IOSP for TropOMI data
            NetcdfFile.registerIOProvider(TropomiIOSP.class);
            // Experimental - this was 2016ish for GPM data, should still work
            NetcdfFile.registerIOProvider(GpmIosp.class);
            
            long sysMem = Long.valueOf(SystemState.queryOpSysProps().get("opsys.memory.physical.total"));
            logger.info("=============================================================================");
            logger.info("Starting McIDAS-V @ {}", new Date());
            logger.info("Versions:");
            logger.info("{}", SystemState.getMcvVersionString());
            logger.info("{}", SystemState.getIdvVersionString());
            logger.info("{}", SystemState.getVisadVersionString());
            logger.info("{}", SystemState.getNcidvVersionString());
            logger.info("{} MB system memory", Math.round(sysMem/1024/1024));
            
            if (!hadCleanExit(SESSION_FILE)) {
                previousStart = extractDate(SESSION_FILE);
            }
            
            createSessionFile(SESSION_FILE);
            registerProtocolHandlers();
            
            McIDASV myself = new McIDASV(args);
        } catch (IllegalArgumentException e) {
            String msg = "McIDAS-V could not initialize itself. ";
            String osName = System.getProperty("os.name");
            if (osName.contains("Windows")) {
                LogUtil.userErrorMessage(msg+'\n'+e.getMessage());
            } else {
                System.err.println(msg+e.getMessage());
            }
        }
    }
    
    /**
     * Log (TRACE level) the time spent in various discrete parts of
     * McIDAS-V's startup process.
     *
     * <p>Currently tracking time spent:
     * <ul>
     *   <li>creating GUI components with {@link McIDASVXmlUi}</li>
     *   <li>initialing Jython interpreters.</li>
     * </ul></p>
     * 
     * <p><b>The elapsed times are merely a quick estimate.</b> The only way
     * to obtain accurate timing information with the JVM is using 
     * <a href="https://openjdk.java.net/projects/code-tools/jmh/">JMH</a>.</p>
     *
     * @see McIDASVXmlUi#getElapsedGuiTime()
     * @see JythonManager#getElapsedInterpreterInit()
     */
    public static void elapsedStartup() {
        long totalXmlUi = 0L;
        List<IdvWindow> windows = cast(IdvWindow.getWindows());
        for (IdvWindow window : windows) {
            McIDASVXmlUi xmlUi = (McIDASVXmlUi)window.getXmlUI();
            if (xmlUi != null) {
                long winElapsed = xmlUi.getElapsedGuiTime();
                totalXmlUi += winElapsed;
                logger.trace("title '{}' xmlui '{}' spent {} ms creating components",
                             window.getTitle(),
                             Integer.toHexString(xmlUi.hashCode()),
                             String.format("%.2f", winElapsed / 1.0e6));
            } else {
                logger.trace("no xmlui for '{}'", window.getTitle());
            }
        }
        
        McIDASV m = getStaticMcv();
        long elapsedJython = m.getJythonManager().getElapsedInterpreterInit();
        
        String xmlui = String.format("%6.2f", totalXmlUi / 1.0e6);
        String jython = String.format("%6.2f", elapsedJython / 1.0e6);
        logger.trace("estimated time spent creating xmlui GUI components   : {} ms", Strings.padStart(xmlui, 9, ' '));
        logger.trace("estimated time spent initializing jython interpreters: {} ms", Strings.padStart(jython, 9, ' '));
    }
    
    /**
     * Attempts a clean shutdown of McIDAS-V.
     *
     * <p>Currently, this entails suppressing any error dialogs, explicitly
     * killing the {@link #addeEntries}, removing {@link #SESSION_FILE},
     * and disabling the directory monitors found in the file choosers.
     * 
     * @param exitCode System exit code to use.
     * 
     * @see IntegratedDataViewer#quit()
     */
    @Override protected void exit(int exitCode) {
        LogUtil.setShowErrorsInGui(false);
        
        // turn off the directory monitors in the file choosers.
        EventBus.publish(Constants.EVENT_FILECHOOSER_STOP, "shutting down");
        stopWatchService();
        
        if (addeEntries != null) {
            addeEntries.saveForShutdown();
            addeEntries.stopLocalServer();
        }
        
        removeSessionFile(SESSION_FILE);
        
        // shut down javafx runtime
        Platform.exit();
        
        // dump some information about how much time was spent doing certain
        // things during startup. 
        elapsedStartup();
        
        logger.info("Exiting McIDAS-V @ {}", new Date());
        
        System.exit(exitCode);
    }
    
    /**
     * This method is largely a copy of {@link IntegratedDataViewer#quit()},
     * but allows for some GUI testing.
     */
    public boolean autoQuit() {
        IdvObjectStore store = getStore();
        
        boolean showQuitConfirm = store.get(PREF_SHOWQUITCONFIRM, true);
        long quitDelay = store.get("mcidasv.autoexit.delay", 3000);
        
        if (showQuitConfirm) {
            JCheckBox cbx = new JCheckBox("Always ask", true);
            JComponent comp =
                GuiUtils.vbox(
                    new JLabel("<html><b>Do you really want to exit?</b></html>"),
                    GuiUtils.inset(cbx, new Insets(4, 15, 0, 10)));
                    
            JOptionPane pane = new JOptionPane(comp,
                                               JOptionPane.QUESTION_MESSAGE,
                                               JOptionPane.YES_NO_OPTION);
                                               
            new OptionPaneClicker(pane, "Exit Confirmation", quitDelay, "Yes");
            getStore().put(PREF_SHOWQUITCONFIRM, cbx.isSelected());
        }
        
        if (!getStationModelManager().checkCloseWindow()) {
            return false;
        }
        
        if (!getJythonManager().saveOnExit()) {
            return false;
        }
        
        store.saveIfNeeded();
        store.cleanupTmpFiles();
        getPluginManager().handleApplicationExit();
        getJythonManager().handleApplicationExit();
        
        if (getInteractiveMode()) {
            exit(0);
        }
        return true;
    }
    
    /**
     * Register the given {@code listener} so that changes to files matching
     * {@code glob} in {@code path} can be handled.
     *
     * @param path Directory to watch.
     * @param glob Only respond to files matching this file mask.
     * @param listener Listener that can handle file changes.
     *
     * @throws IOException if there was a problem registering {@code listener}.
     */
    public void watchDirectory(final String path,
                               final String glob,
                               final OnFileChangeListener listener)
        throws IOException
    {
        watchService.register(listener, path, glob);
    }
    
    /**
     * Returns McIDAS-V's {@link DirectoryWatchService}.
     *
     * @return {@code DirectoryWatchService} responsible for handling all of
     * McIDAS-V's directory monitoring.
     */
    public DirectoryWatchService getWatchService() {
        return watchService;
    }
    
    /**
     * Enable directory monitoring.
     */
    public void startWatchService() {
        watchService.start();
    }
    
    /**
     * Disable directory monitoring.
     */
    public void stopWatchService() {
        if (watchService != null) {
            watchService.stop();
        }
    }
    
    /**
     * Exposes {@link #exit(int)} to other classes.
     * 
     * @param exitCode System exit code to use.
     * 
     * @see #exit(int)
     */
    public void exitMcIDASV(int exitCode) {
        exit(exitCode);
    }
}
