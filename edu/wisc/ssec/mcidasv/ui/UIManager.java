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

package edu.wisc.ssec.mcidasv.ui;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.list;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newHashSet;
import static edu.wisc.ssec.mcidasv.util.XPathUtils.elements;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.SavedBundle;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ViewState;
import ucar.unidata.idv.IdvResourceManager.XmlIdvResource;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.ui.DataControlDialog;
import ucar.unidata.idv.ui.DataSelectionWidget;
import ucar.unidata.idv.ui.DataSelector;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.IdvXmlUi;
import ucar.unidata.idv.ui.ViewPanel;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.ui.ComponentGroup;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.ui.RovingProgress;
import ucar.unidata.ui.XmlUi;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.PersistenceManager;
import edu.wisc.ssec.mcidasv.StateManager;
import edu.wisc.ssec.mcidasv.supportform.McvStateCollector;
import edu.wisc.ssec.mcidasv.supportform.SupportForm;
import edu.wisc.ssec.mcidasv.util.CollectionHelpers;
import edu.wisc.ssec.mcidasv.util.Contract;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.MemoryMonitor;

/**
 * <p>Derive our own UI manager to do some specific things:
 * <ul>
 *   <li>Removing displays</li>
 *   <li>Showing the dashboard</li>
 *   <li>Adding toolbar customization options</li>
 *   <li>Implement the McIDAS-V toolbar as a JToolbar.</li>
 *   <li>Deal with bundles without component groups.</li>
 * </ul></p>
 */
// TODO: investigate moving similar unpersisting code to persistence manager.
public class UIManager extends IdvUIManager implements ActionListener {

    private static final Logger logger = LoggerFactory.getLogger(UIManager.class);
    
    /** Id of the "New Display Tab" menu item for the file menu */
    public static final String MENU_NEWDISPLAY_TAB = "file.new.display.tab";

    /** The tag in the xml ui for creating the special example chooser */
    public static final String TAG_EXAMPLECHOOSER = "examplechooser";

    /**
     * Used to keep track of ViewManagers inside a bundle.
     */
    public static final HashMap<String, ViewManager> savedViewManagers =
        new HashMap<String, ViewManager>();

    /** 
     * Property name for whether or not the description field of the support
     * form should perform line wrapping.
     * */
    public static final String PROP_WRAP_SUPPORT_DESC = 
        "mcidasv.supportform.wrap";

    /** Action command for manipulating the size of the toolbar icons. */
    private static final String ACT_ICON_TYPE = "action.toolbar.seticonsize";

    /** Action command for removing all displays */
    private static final String ACT_REMOVE_DISPLAYS = "action.displays.remove";

    /** Action command for showing the dashboard */
    private static final String ACT_SHOW_DASHBOARD = "action.dashboard.show";

    /** Action command for showing the dashboard */
    private static final String ACT_SHOW_DATASELECTOR = "action.dataselector.show";

    /** Action command for showing the dashboard */
    private static final String ACT_SHOW_DISPLAYCONTROLLER = "action.displaycontroller.show";

    /** Action command for displaying the toolbar preference tab. */
    private static final String ACT_SHOW_PREF = "action.toolbar.showprefs";

    /** Message shown when an unknown action is in the toolbar. */
    private static final String BAD_ACTION_MSG = "Unknown action (%s) found in your toolbar. McIDAS-V will continue to load, but there will be no button associated with %s.";

    /** Menu ID for the {@literal "Restore Saved Views"} submenu. */
    public static final String MENU_NEWVIEWS = "menu.tools.projections.restoresavedviews";

    /** Label for the "link" to the toolbar customization preference tab. */
    private static final String LBL_TB_EDITOR = "Customize...";

    /** Current representation of toolbar actions. */
    private ToolbarStyle currentToolbarStyle = 
        getToolbarStylePref(ToolbarStyle.MEDIUM);

    /** The IDV property that reflects the size of the icons. */
    private static final String PROP_ICON_SIZE = "mcv.ui.iconsize";

    /** The URL of the script that processes McIDAS-V support requests. */
    private static final String SUPPORT_REQ_URL = 
        "http://www.ssec.wisc.edu/mcidas/misc/mc-v/supportreq/support.php";

    /** Separator to use between window title components. */
    protected static final String TITLE_SEPARATOR = " - ";

    /**
     * <p>The currently "displayed" actions. Keeping this List allows us to get 
     * away with only reading the XML files upon starting the application and 
     * only writing the XML files upon exiting the application. This will avoid
     * those redrawing delays.</p>
     */
    private List<String> cachedButtons;

    /** Stores all available actions. */
    private final IdvActions idvActions;

    /** Map of skin ids to their skin resource index. */
    private Map<String, Integer> skinIds = readSkinIds();

    /** An easy way to figure out who is holding a given ViewManager. */
    private Map<ViewManager, ComponentHolder> viewManagers = 
        new HashMap<ViewManager, ComponentHolder>();

    private int componentHolderCount;
    
    private int componentGroupCount;
    
    /** Cache for the results of {@link #getWindowTitleFromSkin(int)}. */
    private final Map<Integer, String> skinToTitle = new ConcurrentHashMap<Integer, String>();

    /** Maps menu IDs to {@link JMenu}s. */
//    private Hashtable<String, JMenu> menuIds;
    private Hashtable<String, JMenuItem> menuIds;

    /** The splash screen (minus easter egg). */
    private McvSplash splash;

    /** 
     * A list of the toolbars that the IDV is playing with. Used to apply 
     * changes to *all* the toolbars in the application.
     */
    private List<JToolBar> toolbars;

    /**
     * Keeping the reference to the toolbar menu mouse listener allows us to
     * avoid constantly rebuilding the menu. 
     */
    private MouseListener toolbarMenu;

    /** Keep the dashboard around so we don't have to re-create it each time. */
    protected IdvWindow dashboard;

    /** False until {@link #initDone()}. */
    protected boolean initDone = false;

    /** IDV instantiation--nice to keep around to reduce getIdv() calls. */
    private IntegratedDataViewer idv;

    /**
     * Hands off our IDV instantiation to IdvUiManager.
     *
     * @param idv The idv
     */
    public UIManager(IntegratedDataViewer idv) {
        super(idv);

        this.idv = idv;

        // cache the appropriate data for the toolbar. it'll make updates 
        // much snappier
        idvActions = new IdvActions(getIdv(), IdvResourceManager.RSC_ACTIONS);
        cachedButtons = readToolbar();
    }

    /**
     * Override the IDV method so that we hide component group button.
     */
    @Override public IdvWindow createNewWindow(List viewManagers,
        boolean notifyCollab, String title, String skinPath, Element skinRoot,
        boolean show, WindowInfo windowInfo) 
    {

        if (windowInfo != null) {
            logger.trace("creating window: title='{}' bounds: {}", title, windowInfo.getBounds());
        } else {
            logger.trace("creating window: title='{}' bounds: (no windowinfo)", title);
        }
        
        if (Constants.DATASELECTOR_NAME.equals(title)) {
            show = false;
        }
        if (skinPath.indexOf("dashboard.xml") >= 0) {
            show = false;
        }

        // used to force any new "display" windows to be the same size as the current window.
        IdvWindow previousWindow = IdvWindow.getActiveWindow();

        IdvWindow w = super.createNewWindow(viewManagers, notifyCollab, title, 
            skinPath, skinRoot, show, windowInfo);

        String iconPath = idv.getProperty(Constants.PROP_APP_ICON, (String)null);
        ImageIcon icon = GuiUtils.getImageIcon(iconPath, getClass(), true);
        w.setIconImage(icon.getImage());

        // try to catch the dashboard
        if (Constants.DATASELECTOR_NAME.equals(w.getTitle())) {
            setDashboard(w);
        } else if (!w.getComponentGroups().isEmpty()) {
            // otherwise we need to hide the component group header and explicitly
            // set the size of the window.
            ((ComponentHolder)w.getComponentGroups().get(0)).setShowHeader(false);
            if (previousWindow != null) {
                Rectangle r = previousWindow.getBounds();
                
                w.setBounds(new Rectangle(r.x, r.y, r.width, r.height));
            }
        } else {
            logger.trace("creating window with no component groups");
        }

        initDisplayShortcuts(w);

        RovingProgress progress =
            (RovingProgress)w.getComponent(IdvUIManager.COMP_PROGRESSBAR);

        if (progress != null) {
            progress.start();
        }
        return w;
    }

    /**
     * Create the display window described by McIDAS-V's default display skin
     * 
     * @return {@link IdvWindow} that was created.
     */
    public IdvWindow buildDefaultSkin() {
        return createNewWindow(new ArrayList(), false);
    }

    /**
     * Create a new IdvWindow for the given viewManager. Put the
     * contents of the viewManager into the window
     * 
     * @return The new window
     */
    public IdvWindow buildEmptyWindow() {
        Element root = null;
        String path = null;
        String skinName = null;

        path = getIdv().getProperty("mcv.ui.emptycompgroup", (String)null);
        if (path != null) {
            path = path.trim();
        }

        if ((path != null) && (path.length() > 0)) {
            try {
                root = XmlUtil.getRoot(path, getClass());
                skinName = getStateManager().getTitle();
                String tmp = XmlUtil.getAttribute(root, "name", (String)null);
                if (tmp == null) {
                    tmp = IOUtil.stripExtension(IOUtil.getFileTail(path));
                }
                skinName = skinName + " - " + tmp;
            } catch (Exception exc) {
                logger.error("error building empty window", exc);
            }
        }

        IdvWindow window = createNewWindow(new ArrayList(), false, skinName, path, root, true, null);
        window.setVisible(true);
        return window;
    }

    
    /**
     * Sets {@link #dashboard} to {@code window}. This method also adds some
     * listeners to {@code window} so that the state of the dashboard is 
     * automatically saved.
     * 
     * @param window The dashboard. Nothing happens if {@link #dashboard} has 
     * already been set, or this parameter is {@code null}.
     */
    private void setDashboard(final IdvWindow window) {
        if (window == null || dashboard != null)
            return;

        dashboard = window;
        dashboard.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        final Component comp = dashboard.getComponent();
        final ucar.unidata.idv.StateManager state = getIdv().getStateManager();

        // for some reason the component listener's "componentHidden" method
        // would not fire the *first* time the dashboard is closed/hidden.
        // the window listener catches it.
        dashboard.addWindowListener(new WindowListener() {
            public void windowClosed(final WindowEvent e) {
                Boolean saveViz = (Boolean)state.getPreference(Constants.PREF_SAVE_DASHBOARD_VIZ, Boolean.FALSE);
                if (saveViz)
                    state.putPreference(Constants.PROP_SHOWDASHBOARD, false);
            }

            public void windowActivated(final WindowEvent e) { }
            public void windowClosing(final WindowEvent e) { }
            public void windowDeactivated(final WindowEvent e) { }
            public void windowDeiconified(final WindowEvent e) { }
            public void windowIconified(final WindowEvent e) { }
            public void windowOpened(final WindowEvent e) { }
        });

        dashboard.getComponent().addComponentListener(new ComponentListener() {
            public void componentMoved(final ComponentEvent e) {
                state.putPreference(Constants.PROP_DASHBOARD_BOUNDS, comp.getBounds());
            }

            public void componentResized(final ComponentEvent e) {
                state.putPreference(Constants.PROP_DASHBOARD_BOUNDS, comp.getBounds());
            }

            public void componentShown(final ComponentEvent e) { 
                Boolean saveViz = (Boolean)state.getPreference(Constants.PREF_SAVE_DASHBOARD_VIZ, Boolean.FALSE);
                if (saveViz)
                    state.putPreference(Constants.PROP_SHOWDASHBOARD, true);
            }

            public void componentHidden(final ComponentEvent e) {
                Boolean saveViz = (Boolean)state.getPreference(Constants.PREF_SAVE_DASHBOARD_VIZ, Boolean.FALSE);
                if (saveViz)
                    state.putPreference(Constants.PROP_SHOWDASHBOARD, false);
            }
        });

        Rectangle bounds = (Rectangle)state.getPreferenceOrProperty(Constants.PROP_DASHBOARD_BOUNDS);
        if (bounds != null)
            comp.setBounds(bounds);
    }

    /**
     * <p>
     * Attempts to add all component holders in <code>info</code> to
     * <code>group</code>. Especially useful when unpersisting a bundle and
     * attempting to deal with its component groups.
     * </p>
     * 
     * @param info The window we want to process.
     * @param group Receives the holders in <code>info</code>.
     * 
     * @return True if there were component groups in <code>info</code>.
     */
    public boolean unpersistComponentGroups(final WindowInfo info,
        final McvComponentGroup group) {
        Collection<Object> comps = info.getPersistentComponents().values();

        if (comps.isEmpty())
            return false;

        for (Object comp : comps) {
            // comp is typically always an IdvComponentGroup, but there are
            // no guarantees...
            if (! (comp instanceof IdvComponentGroup)) {
                System.err.println("DEBUG: non IdvComponentGroup found in persistent components: "
                                   + comp.getClass().getName());
                continue;
            }

            IdvComponentGroup bundleGroup = (IdvComponentGroup)comp;

            // need to make a copy of this list to avoid a rogue
            // ConcurrentModificationException
            // TODO: determine which threads are clobbering each other.
            List<IdvComponentHolder> holders = 
                new ArrayList<IdvComponentHolder>(bundleGroup.getDisplayComponents());

            for (IdvComponentHolder holder : holders)
                group.quietAddComponent(holder);

            group.redoLayout();
        }
        return true;
    }

    /**
     * Override IdvUIManager's loadLookAndFeel so that we can force the IDV to
     * load the Aqua look and feel if requested from the command line.
     */
    @Override public void loadLookAndFeel() {
        if (McIDASV.useAquaLookAndFeel) {
            // since we must rely on the IDV to do the actual loading (due to
            // our UIManager's name conflicting with javax.swing.UIManager's
            // name), save the user's preference, replace it temporarily and
            // have the IDV do its thing, then overwrite the temp preference
            // with the saved preference. Blah!
            String previousLF = getStore().get(PREF_LOOKANDFEEL, (String)null);
            getStore().put(PREF_LOOKANDFEEL, "apple.laf.AquaLookAndFeel");
            super.loadLookAndFeel();
            getStore().put(PREF_LOOKANDFEEL, previousLF);

            // locale code was taken out of IDVUIManager's "loadLookAndFeel".
            //
            // if the locale preference is set to "system", there will *NOT*
            // be a PREF_LOCALE -> "Locale.EXAMPLE" key/value pair in main.xml;
            // as you're using the default state of the preference.
            // this also means that if there is a non-null value associated
            // with PREF_LOCALE, the user has selected the US locale.
            String locale = getStore().get(PREF_LOCALE, (String)null);
            if (locale != null) {
                Locale.setDefault(Locale.US);
            }
        } else {
            super.loadLookAndFeel();
        }
    }

    @Override public void handleWindowActivated(final IdvWindow window) {
        List<ViewManager> viewManagers = window.getViewManagers();
        ViewManager newActive = null;
        long lastActivatedTime = -1;
        
        for (ViewManager viewManager : viewManagers) {
            if (viewManager.getContents() == null)
                continue;
            
            if (!viewManager.getContents().isVisible())
                continue;
            
            lastActiveFrame = window;
            
            if (viewManager.getLastTimeActivated() > lastActivatedTime) {
                newActive = viewManager;
                lastActivatedTime = viewManager.getLastTimeActivated();
            }
        }
        
        if (newActive != null)
            getVMManager().setLastActiveViewManager(newActive);
    }
    
    /**
     * <p>
     * Handles the windowing portions of bundle loading: wraps things in
     * component groups (if needed), merges things into existing windows or
     * creates new windows, and removes displays and data if asked nicely.
     * </p>
     * 
     * @param windows WindowInfos from the bundle.
     * @param newViewManagers ViewManagers stored in the bundle.
     * @param okToMerge Put bundled things into an existing window?
     * @param fromCollab Did this come from the collab stuff?
     * @param didRemoveAll Remove all data and displays?
     * 
     * @see IdvUIManager#unpersistWindowInfo(List, List, boolean, boolean,
     *      boolean)
     */
    @Override public void unpersistWindowInfo(List windows,
            List newViewManagers, boolean okToMerge, boolean fromCollab,
            boolean didRemoveAll) 
        {
            if (newViewManagers == null)
                newViewManagers = new ArrayList<ViewManager>();

            // keep track of the "old" state if the user wants to remove things.
            boolean mergeLayers = ((PersistenceManager)getPersistenceManager()).getMergeBundledLayers();
            List<IdvComponentHolder> holdersBefore = new ArrayList<IdvComponentHolder>();
            List<IdvWindow> windowsBefore = new ArrayList<IdvWindow>();
            if (didRemoveAll) {
                holdersBefore.addAll(McVGuiUtils.getAllComponentHolders());
                windowsBefore.addAll(McVGuiUtils.getAllDisplayWindows());
            }

            for (WindowInfo info : (List<WindowInfo>)windows) {
                newViewManagers.removeAll(info.getViewManagers());
                makeBundledDisplays(info, okToMerge, mergeLayers, fromCollab);

                if (mergeLayers)
                    holdersBefore.addAll(McVGuiUtils.getComponentHolders(info));
            }
//            System.err.println("holdersBefore="+holdersBefore);
            // no reason to kill the displays if there aren't any windows in the
            // bundle!
            if ((mergeLayers) || (didRemoveAll && !windows.isEmpty()))
                killOldDisplays(holdersBefore, windowsBefore, (okToMerge || mergeLayers));
        }

    /**
     * <p>
     * Removes data and displays that existed prior to loading a bundle.
     * </p>
     * 
     * @param oldHolders Component holders around before loading.
     * @param oldWindows Windows around before loading.
     * @param merge Were the bundle contents merged into an existing window?
     */
    public void killOldDisplays(final List<IdvComponentHolder> oldHolders,
        final List<IdvWindow> oldWindows, final boolean merge) 
    {
//        System.err.println("killOldDisplays: merge="+merge);
        // if we merged, this will ensure that any old holders in the merged
        // window also get removed.
        if (merge)
            for (IdvComponentHolder holder : oldHolders)
                holder.doRemove();

        // mop up any windows that no longer have component holders.
        for (IdvWindow window : oldWindows) {
            IdvComponentGroup group = McVGuiUtils.getComponentGroup(window);

            List<IdvComponentHolder> holders =
                McVGuiUtils.getComponentHolders(group);

            // if the old set of holders contains all of this window's
            // holders, this window can be deleted:
            // 
            // this works fine for merging because the okToMerge stuff will
            // remove all old holders from the current window, but if the
            // bundle was merged into this window, containsAll() will fail
            // due to there being a new holder.
            // 
            // if the bundle was loaded into its own window, then
            // all the old windows will pass this test.
            if (oldHolders.containsAll(holders)) {
                group.doRemove();
                window.dispose();
            }
        }
    }
    

    /**
     * A hack because Unidata moved the skins (taken from 
     * {@link IdvPersistenceManager}).
     * 
     * @param skinPath original path
     * @return fixed path
     */
    private String fixSkinPath(String skinPath) {
        if (skinPath == null) {
            return null;
        }
        if (StringUtil.stringMatch(
                skinPath, "^/ucar/unidata/idv/resources/[^/]+\\.xml")) {
            skinPath =
                StringUtil.replace(skinPath, "/ucar/unidata/idv/resources/",
                                   "/ucar/unidata/idv/resources/skins/");
        }
        return skinPath;
    }
    
    /**
     * <p>
     * Uses the contents of {@code info} to rebuild a display that has been 
     * bundled. If {@code merge} is true, the displayable parts of the bundle 
     * will be put into the current window. Otherwise a new window is created 
     * and the relevant parts of the bundle will occupy that new window.
     * </p>
     * 
     * @param info WindowInfo to use with creating the new window.
     * @param merge Merge created things into an existing window?
     */
    public void makeBundledDisplays(final WindowInfo info, final boolean merge, final boolean mergeLayers, final boolean fromCollab) {
        // need a way to get the last active view manager (for real)
        IdvWindow window = IdvWindow.getActiveWindow();
        ViewManager last = ((PersistenceManager)getPersistenceManager()).getLastViewManager();
        String skinPath = info.getSkinPath();

        // create a new window if we're not merging (or the active window is 
        // invalid), otherwise sticking with the active window is fine.
        if ((merge || (mergeLayers)) && last != null) {
            List<IdvWindow> windows = IdvWindow.getWindows();
            for (IdvWindow tmpWindow : windows) {
                if (tmpWindow.getComponentGroups().isEmpty())
                    continue;

                List<IdvComponentGroup> groups = tmpWindow.getComponentGroups();
                for (IdvComponentGroup group : groups) {
                    List<IdvComponentHolder> holders = group.getDisplayComponents();
                    for (IdvComponentHolder holder : holders) {
                        List<ViewManager> vms = holder.getViewManagers();
                        if (vms != null && vms.contains(last)) {
                            window = tmpWindow;

                            if (mergeLayers) {
                                mergeLayers(info, window, fromCollab);
                            }
                            break;
                        }
                    }
                }
            }
        }
        else if ((window == null) || (!merge) || (window.getComponentGroups().isEmpty())) {
            try {
                Element skinRoot =
                    XmlUtil.getRoot(Constants.BLANK_COMP_GROUP, getClass());

                window = createNewWindow(null, false, "McIDAS-V",
                    Constants.BLANK_COMP_GROUP, skinRoot, false, null);

                window.setBounds(info.getBounds());
                window.setVisible(true);

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        McvComponentGroup group =
            (McvComponentGroup)window.getComponentGroups().get(0);

        // if the bundle contains only component groups, ensure they get merged
        // into group.
        unpersistComponentGroups(info, group);
    }

    private void mergeLayers(final WindowInfo info, final IdvWindow window, final boolean fromCollab) {
        List<ViewManager> newVms = McVGuiUtils.getViewManagers(info);
        List<ViewManager> oldVms = McVGuiUtils.getViewManagers(window);

        if (oldVms.size() == newVms.size()) {
            List<ViewManager> merged = new ArrayList<ViewManager>();
            for (int vmIdx = 0;
                     (vmIdx < newVms.size())
                     && (vmIdx < oldVms.size());
                     vmIdx++) 
            {
                ViewManager newVm = newVms.get(vmIdx);
                ViewManager oldVm = oldVms.get(vmIdx);
                if (oldVm.canBe(newVm)) {
                    oldVm.initWith(newVm, fromCollab);
                    merged.add(newVm);
                }
            }
            
            Collection<Object> comps = info.getPersistentComponents().values();

            for (Object comp : comps) {
                if (!(comp instanceof IdvComponentGroup))
                    continue;
                
                IdvComponentGroup group = (IdvComponentGroup)comp;
                List<IdvComponentHolder> holders = group.getDisplayComponents();
                List<IdvComponentHolder> emptyHolders = new ArrayList<IdvComponentHolder>();
                for (IdvComponentHolder holder : holders) {
                    List<ViewManager> vms = holder.getViewManagers();
                    for (ViewManager vm : merged) {
                        if (vms.contains(vm)) {
                            vms.remove(vm);
                            getVMManager().removeViewManager(vm);
                            List<DisplayControlImpl> controls = vm.getControlsForLegend();
                            for (DisplayControlImpl dc : controls) {
                                try {
                                    dc.doRemove();
                                } catch (Exception e) { }
                                getViewPanel().removeDisplayControl(dc);
                                getViewPanel().viewManagerDestroyed(vm);
                                
                                vm.clearDisplays();

                            }
                        }
                    }
                    holder.setViewManagers(vms);

                    if (vms.isEmpty()) {
                        emptyHolders.add(holder);
                    }
                }
                
                for (IdvComponentHolder holder : emptyHolders) {
                    holder.doRemove();
                    group.removeComponent(holder);
                }
            }
        }
    }

    /**
     * Make a window title. The format for window titles is:
     * {@literal <window>TITLE_SEPARATOR<document>}
     * 
     * @param win Window title.
     * @param doc Document or window sub-content.
     * @return Formatted window title.
     */
    protected static String makeTitle(final String win, final String doc) {
        if (win == null)
            return "";
        else if (doc == null)
            return win;
        else if (doc.equals("untitled"))
            return win;

        return win.concat(TITLE_SEPARATOR).concat(doc);
    }

    /**
     * Make a window title. The format for window titles is:
     * 
     * <pre>
     * &lt;window&gt;TITLE_SEPARATOR&lt;document&gt;TITLE_SEPARATOR&lt;other&gt;
     * </pre>
     * 
     * @param window Window title.
     * @param document Document or window sub content.
     * @param other Other content to include.
     * @return Formatted window title.
     */
    protected static String makeTitle(final String window,
        final String document, final String other) 
    {
        if (other == null)
            return makeTitle(window, document);

        return window.concat(TITLE_SEPARATOR).concat(document).concat(
            TITLE_SEPARATOR).concat(other);
    }

    /**
     * Split window title using <code>TITLE_SEPARATOR</code>.
     * 
     * @param title The window title to split
     * @return Parts of the title with the white space trimmed.
     */
    protected static String[] splitTitle(final String title) {
        String[] splt = title.split(TITLE_SEPARATOR);
        for (int i = 0; i < splt.length; i++) {
            splt[i] = splt[i].trim();
        }
        return splt;
    }

    /**
     * Overridden to prevent the IDV's {@code StateManager} instantiation of {@link ucar.unidata.idv.mac.MacBridge}.
     * McIDAS-V uses different approaches for OS X compatibility.
     *
     * @return Always returns {@code false}.
     *
     * @deprecated Use {@link edu.wisc.ssec.mcidasv.McIDASV#isMac()} instead.
     */
    // TODO: be sure to bring back the override annotation once we've upgraded our idv.jar.
    public boolean isMac() {
        return false;
    }

    /* (non-Javadoc)
     * @see ucar.unidata.idv.ui.IdvUIManager#about()
     */
    public void about() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AboutFrame((McIDASV)idv).setVisible(true);
            }
        });
    }

    /**
     * Handles all the ActionEvents that occur for widgets contained within
     * this class. It's not so pretty, but it isolates the event handling in
     * one place (and reduces the number of action listeners to one).
     * 
     * @param e The event that triggered the call to this method.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = (String)e.getActionCommand();
        boolean toolbarEditEvent = false;

        // handle selecting large icons
        if (cmd.startsWith(ToolbarStyle.LARGE.getAction())) {
            currentToolbarStyle = ToolbarStyle.LARGE;
            toolbarEditEvent = true;
        }

        // handle selecting medium icons
        else if (cmd.startsWith(ToolbarStyle.MEDIUM.getAction())) {
            currentToolbarStyle = ToolbarStyle.MEDIUM;
            toolbarEditEvent = true;
        }

        // handle selecting small icons
        else if (cmd.startsWith(ToolbarStyle.SMALL.getAction())) {
            currentToolbarStyle = ToolbarStyle.SMALL;
            toolbarEditEvent = true;
        }

        // handle the user selecting the show toolbar preference menu item
        else if (cmd.startsWith(ACT_SHOW_PREF)) {
            IdvPreferenceManager prefs = idv.getPreferenceManager();
            prefs.showTab(Constants.PREF_LIST_TOOLBAR);
            toolbarEditEvent = true;
        }

        // handle the user toggling the size of the icon
        else if (cmd.startsWith(ACT_ICON_TYPE))
            toolbarEditEvent = true;

        // handle the user removing displays
        else if (cmd.startsWith(ACT_REMOVE_DISPLAYS))
            idv.removeAllDisplays();

        // handle popping up the dashboard.
        else if (cmd.startsWith(ACT_SHOW_DASHBOARD))
            showDashboard();

        // handle popping up the data explorer.
        else if (cmd.startsWith(ACT_SHOW_DATASELECTOR))
            showDashboard("Data Sources");

        // handle popping up the display controller.
        else if (cmd.startsWith(ACT_SHOW_DISPLAYCONTROLLER))
            showDashboard("Layer Controls");

        else
            System.err.println("Unsupported action event!");

        // if the user did something to change the toolbar, hide the current
        // toolbar, replace it, and then make the new toolbar visible.
        if (toolbarEditEvent == true) {

            getStateManager().writePreference(PROP_ICON_SIZE, 
                currentToolbarStyle.getSizeAsString());

            // destroy the menu so it can be properly updated during rebuild
            toolbarMenu = null;

            for (JToolBar toolbar : toolbars) {
                toolbar.setVisible(false);
                populateToolbar(toolbar);
                toolbar.setVisible(true);
            }
        }
    }

    public JComponent getDisplaySelectorComponent() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        DefaultTreeModel model = new DefaultTreeModel(root);
        final JTree tree = new JTree(model);
        tree.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        tree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION
        );
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        tree.setCellRenderer(renderer);

        for (IdvWindow w : McVGuiUtils.getAllDisplayWindows()) {
            String title = w.getTitle();
            TwoFacedObject winTFO = new TwoFacedObject(title, w);
            DefaultMutableTreeNode winNode = new DefaultMutableTreeNode(winTFO);
            for (IdvComponentHolder h : McVGuiUtils.getComponentHolders(w)) {
                String hName = h.getName();
                TwoFacedObject tmp = new TwoFacedObject(hName, h);
                DefaultMutableTreeNode holderNode = new DefaultMutableTreeNode(tmp);
                //for (ViewManager v : (List<ViewManager>)h.getViewManagers()) {
                for (int i = 0; i < h.getViewManagers().size(); i++) {
                    ViewManager v = (ViewManager)h.getViewManagers().get(i);
                    String vName = v.getName();
                    TwoFacedObject tfo = null;
                    
                    if (vName != null && vName.length() > 0)
                        tfo = new TwoFacedObject(vName, v);
                    else
                        tfo = new TwoFacedObject(Constants.PANEL_NAME + " " + (i+1), v);
                    
                    holderNode.add(new DefaultMutableTreeNode(tfo));
                }
                winNode.add(holderNode);
            }
            root.add(winNode);
        }

        // select the appropriate view
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent evt) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
                if (node == null || !(node.getUserObject() instanceof TwoFacedObject)) {
                    return;
                }
                TwoFacedObject tfo = (TwoFacedObject) node.getUserObject();

                Object obj = tfo.getId();
                if (obj instanceof ViewManager) {
                    ViewManager viewManager = (ViewManager) tfo.getId();
                    idv.getVMManager().setLastActiveViewManager(viewManager);
                } else if (obj instanceof McvComponentHolder) {
                    McvComponentHolder holder = (McvComponentHolder)obj;
                    holder.setAsActiveTab();
                } else if (obj instanceof IdvWindow) {
                    IdvWindow window = (IdvWindow)obj;
                    window.toFront();
                }
            }
        });

        // expand all the nodes
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandPath(tree.getPathForRow(i));
        }

        return tree;
    }

    /**
     * Builds the JPopupMenu that appears when a user right-clicks in the
     * toolbar.
     * 
     * @return MouseListener that listens for right-clicks in the toolbar.
     */
    private MouseListener constructToolbarMenu() {
        JMenuItem large = ToolbarStyle.LARGE.buildMenuItem(this);
        JMenuItem medium = ToolbarStyle.MEDIUM.buildMenuItem(this);
        JMenuItem small = ToolbarStyle.SMALL.buildMenuItem(this);

        JMenuItem toolbarPrefs = new JMenuItem(LBL_TB_EDITOR);
        toolbarPrefs.setActionCommand(ACT_SHOW_PREF);
        toolbarPrefs.addActionListener(this);

        switch (currentToolbarStyle) {
            case LARGE:  
                large.setSelected(true); 
                break;

            case MEDIUM: 
                medium.setSelected(true); 
                break;

            case SMALL: 
                small.setSelected(true); 
                break;

            default:
                break;
        }

        ButtonGroup group = new ButtonGroup();
        group.add(large);
        group.add(medium);
        group.add(small);

        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(new BevelBorder(BevelBorder.RAISED));
        popup.add(large);
        popup.add(medium);
        popup.add(small);
        popup.addSeparator();
        popup.add(toolbarPrefs);

        return new PopupListener(popup);
    }

    /**
     * Queries the stored preferences to determine the preferred 
     * {@link ToolbarStyle}. If there was no preference, {@code defaultStyle} 
     * is used.
     * 
     * @param defaultStyle {@code ToolbarStyle} to use if there was no value 
     * associated with the toolbar style preference.
     * 
     * @return The preferred {@code ToolbarStyle} or {@code defaultStyle}.
     * 
     * @throws AssertionError if {@code PROP_ICON_SIZE} had returned an integer
     * value that did not correspond to a valid {@code ToolbarStyle}.
     */
    private ToolbarStyle getToolbarStylePref(final ToolbarStyle defaultStyle) {
        assert defaultStyle != null;
        String storedStyle = getStateManager().getPreferenceOrProperty(PROP_ICON_SIZE, (String)null);
        if (storedStyle == null)
            return defaultStyle;

        int intSize = Integer.valueOf(storedStyle);

        // can't switch on intSize using ToolbarStyles as the case...
        if (intSize == ToolbarStyle.LARGE.getSize())
            return ToolbarStyle.LARGE;
        if (intSize == ToolbarStyle.MEDIUM.getSize())
            return ToolbarStyle.MEDIUM;
        if (intSize == ToolbarStyle.SMALL.getSize())
            return ToolbarStyle.SMALL;

        // uh oh
        throw new AssertionError("Invalid preferred icon size: " + intSize);
    }

    /**
     * Given a valid action and icon size, build a JButton for the toolbar.
     * 
     * @param action The action whose corresponding icon we want.
     * 
     * @return A JButton for the given action with an appropriate-sized icon.
     */
    private JButton buildToolbarButton(String action) {
        IdvAction a = idvActions.getAction(action);
        if (a == null)
            return null;

        JButton button = new JButton(idvActions.getStyledIconFor(action, currentToolbarStyle));

        // the IDV will take care of action handling! so nice!
        button.addActionListener(idv);
        button.setActionCommand(a.getAttribute(ActionAttribute.ACTION));
        button.addMouseListener(toolbarMenu);
        button.setToolTipText(a.getAttribute(ActionAttribute.DESCRIPTION));

        return button;
    }

    @Override public JPanel doMakeStatusBar(final IdvWindow window) {
        if (window == null)
            return new JPanel();

        JLabel msgLabel = new JLabel("                         ");
        LogUtil.addMessageLogger(msgLabel);

        window.setComponent(COMP_MESSAGELABEL, msgLabel);

        IdvXmlUi xmlUI = window.getXmlUI();
        if (xmlUI != null)
            xmlUI.addComponent(COMP_MESSAGELABEL, msgLabel);

        JLabel waitLabel = new JLabel(IdvWindow.getNormalIcon());
        waitLabel.addMouseListener(new ObjectListener(null) {
            public void mouseClicked(final MouseEvent e) {
                getIdv().clearWaitCursor();
            }
        });
        window.setComponent(COMP_WAITLABEL, waitLabel);

        RovingProgress progress = doMakeRovingProgressBar();
        window.setComponent(COMP_PROGRESSBAR, progress);

//        Monitoring label = new MemoryPanel();
//        ((McIDASV)getIdv()).getMonitorManager().addListener(label);
//        window.setComponent(Constants.COMP_MONITORPANEL, label);

        boolean isClockShowing = Boolean.getBoolean(getStateManager().getPreferenceOrProperty(PROP_SHOWCLOCK_VIEW, "true"));
        MemoryMonitor mm = new MemoryMonitor(getStateManager(), 75, 95, isClockShowing);
        mm.setBorder(getStatusBorder());

        // MAKE PRETTY NOW!
        progress.setBorder(getStatusBorder());
        waitLabel.setBorder(getStatusBorder());
        msgLabel.setBorder(getStatusBorder());
//        ((JPanel)label).setBorder(getStatusBorder());

//        JPanel msgBar = GuiUtils.leftCenter((JPanel)label, msgLabel);
        JPanel msgBar = GuiUtils.leftCenter(mm, msgLabel);
        JPanel statusBar = GuiUtils.centerRight(msgBar, progress);
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        return statusBar;
    }

    /**
     * Make the roving progress bar
     *
     * @return Roving progress bar
     */
    public RovingProgress doMakeRovingProgressBar() {
    	RovingProgress progress = new RovingProgress(Constants.MCV_BLUE) {
    		private Font labelFont;
//            public boolean drawFilledSquare() {
//                return false;
//            }

    		public void paintInner(Graphics g) {
    			//Catch if we're not in a wait state
    			if ( !IdvWindow.getWaitState() && super.isRunning()) {
    				stop();
    				return;
    			}
    			if ( !super.isRunning()) {
    				super.paintInner(g);
    				return;
    			}
    			super.paintInner(g);
    		}
    		
    		public void paintLabel(Graphics g, Rectangle bounds) {
    			if (labelFont == null) {
    				labelFont = g.getFont();
    				labelFont = labelFont.deriveFont(Font.BOLD);
    			}
    			g.setFont(labelFont);
    			g.setColor(Color.black);
    			if (DataSourceImpl.getOutstandingGetDataCalls() > 0) {
    				g.drawString(" Reading data", 5, bounds.height - 4);
    			}
    			else if (!idv.getAllDisplaysIntialized()){
    				g.drawString(" Creating layers", 5, bounds.height - 4);
    			}

    		}
    		
    	    public synchronized void stop() {
    	    	super.stop();
    	    	super.reset();
    	    }

    	};
    	progress.setPreferredSize(new Dimension(130, 10));
    	return progress;
    }
    
    /**
     * <p>
     * Overrides the IDV's getToolbarUI so that McV can return its own toolbar
     * and not deal with the way the IDV handles toolbars. This method also
     * updates the toolbar data member so that other methods can fool around
     * with whatever the IDV thinks is a toolbar (without having to rely on the
     * IDV window manager code).
     * </p>
     * 
     * <p>
     * Not that the IDV code is bad of course--I just can't handle that pause
     * while the toolbar is rebuilt!
     * </p>
     * 
     * @return A new toolbar based on the contents of toolbar.xml.
     */
    @Override public JComponent getToolbarUI() {
        if (toolbars == null) {
            toolbars = new LinkedList<JToolBar>();
        }
        JToolBar toolbar = new JToolBar();
        populateToolbar(toolbar);
        toolbars.add(toolbar);
        return toolbar;
    }

    /**
     * Return a McV-style toolbar to the IDV.
     * 
     * @return A fancy-pants toolbar.
     */
    @Override protected JComponent doMakeToolbar() {
        return getToolbarUI();
    }

    /**
     * Uses the cached XML to create a toolbar. Any updates to the toolbar 
     * happen almost instantly using this approach. Do note that if there are
     * any components in the given toolbar they will be removed.
     * 
     * @param toolbar A reference to the toolbar that needs buttons and stuff.
     */
    private void populateToolbar(JToolBar toolbar) {
        // clear out the toolbar's current denizens, if any. just a nicety.
        if (toolbar.getComponentCount() > 0) {
            toolbar.removeAll();
        }

        // ensure that the toolbar popup menu appears
        if (toolbarMenu == null) {
            toolbarMenu = constructToolbarMenu();
        }

        toolbar.addMouseListener(toolbarMenu);

        // add the actions that should appear in the toolbar.
        for (String action : cachedButtons) {

            // null actions are considered separators.
            if (action == null) {
                toolbar.addSeparator();
            }
            // otherwise we've got a button to add
            else {
                JButton b = buildToolbarButton(action);
                if (b != null) {
                    toolbar.add(b);
                } else {
                    String err = String.format(BAD_ACTION_MSG, action, action);
                    LogUtil.userErrorMessage(err);
                }
            }
        }

        toolbar.addSeparator();

        BundleTreeNode treeRoot = buildBundleTree();
        if (treeRoot != null) {

            // add the favorite bundles to the toolbar (hello Tom Whittaker!)
            for (BundleTreeNode tmp : treeRoot.getChildren()) {

                // if this node doesn't have a bundle, it's considered a parent
                if (tmp.getBundle() == null) {
                    addBundleTree(toolbar, tmp);
                }
                // otherwise it's just another button to add.
                else {
                    addBundle(toolbar, tmp);
                }
            }
        }
    }

    /**
     * Given a reference to the current toolbar and a bundle tree node, build a
     * button representation of the bundle and add it to the toolbar.
     * 
     * @param toolbar The toolbar to which we add the bundle.
     * @param node The node within the bundle tree that contains our bundle.
     */
    private void addBundle(JToolBar toolbar, BundleTreeNode node) {
        final SavedBundle bundle = node.getBundle();

        ImageIcon fileIcon =
            GuiUtils.getImageIcon("/auxdata/ui/icons/File.gif");

        JButton button = new JButton(node.getName(), fileIcon);
        button.setToolTipText("Click to open favorite: " + node.getName());
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // running in a separate thread is kinda nice!
                Misc.run(UIManager.this, "processBundle", bundle);
            }
        });
        toolbar.add(button);
    }

    /**
     * <p>
     * Builds two things, given a toolbar and a tree node: a JButton that
     * represents a "first-level" parent node and a JPopupMenu that appears
     * upon clicking the JButton. The button is then added to the given
     * toolbar.
     * </p>
     * 
     * <p>
     * "First-level" means the given node is a child of the root node.
     * </p>
     * 
     * @param toolbar The toolbar to which we add the bundle tree.
     * @param node The node we want to add! OMG like duh!
     */
    private void addBundleTree(JToolBar toolbar, BundleTreeNode node) {
        ImageIcon catIcon =
            GuiUtils.getImageIcon("/auxdata/ui/icons/Folder.gif");

        final JButton button = new JButton(node.getName(), catIcon);
        final JPopupMenu popup = new JPopupMenu();

        button.setToolTipText("Show Favorites category: " + node.getName());

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                popup.show(button, 0, button.getHeight());
            }
        });

        toolbar.add(button);

        // recurse through the child nodes
        for (BundleTreeNode kid : node.getChildren())
            buildPopupMenu(kid, popup);
    }

    /**
     * Writes the currently displayed toolbar buttons to the toolbar XML. This
     * has mostly been ripped off from ToolbarEditor. :(
     */
    public void writeToolbar() {
        XmlResourceCollection resources =
            getResourceManager()
                .getXmlResources(IdvResourceManager.RSC_TOOLBAR);

        String actionPrefix = "action:";

        // ensure that the IDV can read the XML we're generating.
        Document doc = resources.getWritableDocument("<panel/>");
        Element root = resources.getWritableRoot("<panel/>");
        root.setAttribute(XmlUi.ATTR_LAYOUT, XmlUi.LAYOUT_FLOW);
        root.setAttribute(XmlUi.ATTR_MARGIN, "4");
        root.setAttribute(XmlUi.ATTR_VSPACE, "0");
        root.setAttribute(XmlUi.ATTR_HSPACE, "2");
        root.setAttribute(XmlUi.inheritName(XmlUi.ATTR_SPACE), "2");
        root.setAttribute(XmlUi.inheritName(XmlUi.ATTR_WIDTH), "5");

        // clear out any pesky kids from previous relationships. XML don't need
        // no baby-mama drama.
        XmlUtil.removeChildren(root);

        // iterate through the actions that have toolbar buttons in use and add
        // 'em to the XML.
        for (String action : cachedButtons) {
            Element e;
            if (action != null) {
                e = doc.createElement(XmlUi.TAG_BUTTON);
                e.setAttribute(XmlUi.ATTR_ACTION, (actionPrefix + action));
            } else {
                e = doc.createElement(XmlUi.TAG_FILLER);
                e.setAttribute(XmlUi.ATTR_WIDTH, "5");
            }
            root.appendChild(e);
        }

        // write the XML
        try {
            resources.writeWritable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read the contents of the toolbar XML into a List. We're essentially just
     * throwing actions into the list.
     * 
     * @return The actions/buttons that live in the toolbar xml. Note that if 
     * an element is {@code null}, this element represents a {@literal "space"}
     * that should appear in both the Toolbar and the Toolbar Preferences.
     */
    public List<String> readToolbar() {
        final Element root = getToolbarRoot();
        if (root == null) {
            return null;
        }

        final NodeList elements = XmlUtil.getElements(root);
        List<String> data = new ArrayList<String>(elements.getLength());
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element)elements.item(i);
            if (child.getTagName().equals(XmlUi.TAG_BUTTON)) {
                data.add(
                    XmlUtil.getAttribute(child, ATTR_ACTION, (String)null)
                        .substring(7));
            } else {
                data.add(null);
            }
        }
        return data;
    }

    /**
     * Returns the icon associated with {@code actionId}. Note that associating
     * the {@literal "missing icon"} icon with an action is allowable.
     * 
     * @param actionId Action ID whose associated icon is to be returned.
     * @param style Returned icon's size will be the size associated with the
     * specified {@code ToolbarStyle}.
     * 
     * @return Either the icon corresponding to {@code actionId} or the default
     * {@literal "missing icon"} icon.
     * 
     * @throws NullPointerException if {@code actionId} is null.
     */
    protected Icon getActionIcon(final String actionId, 
        final ToolbarStyle style) 
    {
        if (actionId == null)
            throw new NullPointerException("Action ID cannot be null");

        Icon actionIcon = idvActions.getStyledIconFor(actionId, style);
        if (actionIcon != null)
            return actionIcon;

        String icon = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/range-bearing%d.png";
        URL tmp = getClass().getResource(String.format(icon, style.getSize()));
        return new ImageIcon(tmp);
    }

    /**
     * Returns the known {@link IdvAction}s in the form of {@link IdvActions}.
     * 
     * @return {@link #idvActions}
     */
    public IdvActions getCachedActions() {
        return idvActions;
    }

    /**
     * Returns the actions that currently make up the McIDAS-V toolbar.
     * 
     * @return {@link List} of {@link ActionAttribute#ID}s that make up the
     * current toolbar buttons.
     */
    public List<String> getCachedButtons() {
        if (cachedButtons == null) {
            cachedButtons = readToolbar();
        }
        return cachedButtons;
    }

    /**
     * Make the menu of actions.
     * 
     * <p>Overridden in McIDAS-V so that we can fool the IDV into working with
     * our icons that allow for multiple {@literal "styles"}.
     * 
     * @param obj Object to call.
     * @param method Method to call.
     * @param makeCall if {@code true}, call 
     * {@link IntegratedDataViewer#handleAction(String)}.
     * 
     * @return List of {@link JMenu}s that represent our action menus.
     */
    @Override public List<JMenu> makeActionMenu(final Object obj, 
        final String method, final boolean makeCall) 
    {
        List<JMenu> menu = arrList();
        IdvActions actions = getCachedActions();
        for (String group : actions.getAllGroups()) {
            List<JMenuItem> items = arrList();
            for (IdvAction action : actions.getActionsForGroup(group)) {
                String cmd = (makeCall) ? action.getCommand() : action.getId();
                String desc = action.getAttribute(ActionAttribute.DESCRIPTION);
//                items.add(GuiUtils.makeMenuItem(desc, obj, method, cmd));
                items.add(makeMenuItem(desc, obj, method, cmd));
            }
//            menu.add(GuiUtils.makeMenu(group, items));
            menu.add(makeMenu(group, items));
        }
        return menu;
    }

    /**
     * @see GuiUtils#makeMenuItem(String, Object, String, Object)
     */
    public static JMenuItem makeMenuItem(String label, Object obj, 
        String method, Object arg) 
    {
        return GuiUtils.makeMenuItem(label, obj, method, arg);
    }

    /**
     * @see GuiUtils#makeMenu(String, List)
     */
    @SuppressWarnings("unchecked")
    public static JMenu makeMenu(String name, List menuItems) {
        return GuiUtils.makeMenu(name, menuItems);
    }

    /**
     * Returns the collection of action identifiers.
     * 
     * <p>Overridden in McIDAS-V so that we can fool the IDV into working with
     * our icons that allow for multiple {@literal "styles"}.
     * 
     * @return {@link List} of {@link String}s that correspond to
     * {@link IdvAction IdvActions}.
     */
    @Override public List<String> getActions() {
        return idvActions.getAttributes(ActionAttribute.ID);
    }

    /**
     * Looks for the XML {@link Element} representation of the action 
     * associated with {@code actionId}.
     * 
     * <p>Overridden in McIDAS-V so that we can fool the IDV into working with
     * our icons that allow for multiple {@literal "styles"}.
     * 
     * @param actionId ID of the action whose {@literal "action node"} is desired. Cannot be {@code null}.
     * 
     * @return {@literal "action node"} associated with {@code actionId}.
     * 
     * @throws NullPointerException if {@code actionId} is {@code null}.
     */
    @Override public Element getActionNode(final String actionId) {
        Contract.notNull(actionId, "Null action id strings are invalid");
        return idvActions.getElementForAction(actionId);
    }

    /**
     * Searches for an action identified by a given {@code actionId}, and 
     * returns the value associated with its {@code attr}.
     * 
     * <p>Overridden in McIDAS-V so that we can fool the IDV into working with
     * our icons that allow for multiple {@literal "styles"}.
     * 
     * @param actionId ID of the action whose attribute value is desired. Cannot be {@code null}.
     * @param attr The attribute whose value is desired. Cannot be {@code null}.
     * 
     * @return Value associated with the given action and given attribute.
     * 
     * @throws NullPointerException if {@code actionId} or {@code attr} is {@code null}.
     */
    @Override public String getActionAttr(final String actionId, 
        final String attr) 
    {
        Contract.notNull(actionId, "Null action id strings are invalid");
        Contract.notNull(attr, "Null attributes are invalid");
        ActionAttribute actionAttr = ActionAttribute.valueOf(attr.toUpperCase());
        return idvActions.getAttributeForAction(stripAction(actionId), actionAttr);
    }

    /**
     * Attempts to verify that {@code element} represents a {@literal "valid"}
     * IDV action.
     * 
     * @param element {@link Element} to check. {@code null} values permitted, 
     * but they return {@code false}.
     * 
     * @return {@code true} if {@code element} had all required 
     * {@link ActionAttribute}s. {@code false} otherwise, or if 
     * {@code element} is {@code null}.
     */
    private static boolean isValidIdvAction(final Element element) {
        if (element == null) {
            return false;
        }
        for (ActionAttribute attribute : ActionAttribute.values()) {
            if (!attribute.isRequired()) {
                continue;
            }
            if (!XmlUtil.hasAttribute(element, attribute.asIdvString())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds a {@link Map} of {@link ActionAttribute}s to values for a given
     * {@link Element}. If {@code element} does not contain an optional attribute,
     * use the attribute's default value.
     * 
     * @param element {@literal "Action node"} of interest. {@code null} 
     * permitted, but results in an empty {@code Map}.
     * 
     * @return Mapping of {@code ActionAttribute}s to values, or an empty 
     * {@code Map} if {@code element} is {@code null}.
     */
    private static Map<ActionAttribute, String> actionElementToMap(
        final Element element) 
    {
        if (element == null) {
            return Collections.emptyMap();
        }
        // loop through set of action attributes; if element contains attribute "A", add it; return results.
        Map<ActionAttribute, String> attrs = 
            new LinkedHashMap<ActionAttribute, String>();
        for (ActionAttribute attribute : ActionAttribute.values()) {
            String idvStr = attribute.asIdvString();
            if (XmlUtil.hasAttribute(element, idvStr)) {
                attrs.put(attribute, XmlUtil.getAttribute(element, idvStr));
            } else {
                attrs.put(attribute, attribute.defaultValue());
            }
        }
        return attrs;
    }
    
    /**
     * <p>
     * Builds a tree out of the bundles that should appear within the McV
     * toolbar. A tree is a nice way to store this data, as the default IDV
     * behavior is to act kinda like a file explorer when it displays these
     * bundles.
     * </p>
     * 
     * <p>
     * The tree makes it REALLY easy to replicate the default IDV
     * functionality.
     * </p>
     * 
     * @return The root BundleTreeNode for the tree containing toolbar bundles.
     */
    public BundleTreeNode buildBundleTree() {
        final String TOOLBAR = "Toolbar";

        int bundleType = IdvPersistenceManager.BUNDLES_FAVORITES;

        final List<SavedBundle> bundles =
            getPersistenceManager().getBundles(bundleType);

        // handy reference to parent nodes; bundle count * 4 seems pretty safe
        final Map<String, BundleTreeNode> mapper =
            new HashMap<String, BundleTreeNode>(bundles.size() * 4);

        // iterate through all toolbar bundles
        for (SavedBundle bundle : bundles) {
            String categoryPath = "";
            String grandParentPath;

            // build the "path" to the bundle. these paths basically look like
            // "Toolbar>category>subcategory>." so "category" is a category of
            // toolbar bundles and subcategory is a subcategory of that. The
            // IDV will build nice JPopupMenus with everything that appears in
            // "category," so McV needs to do the same thing. thus McV needs to
            // figure out the complete path to each toolbar bundle!
            List<String> categories = (List<String>)bundle.getCategories();
            if (categories == null || categories.isEmpty() || !TOOLBAR.equals(categories.get(0))) {
                continue;
            }

            for (String category : categories) {
                grandParentPath = categoryPath;
                categoryPath += category + '>';

                if (!mapper.containsKey(categoryPath)) {
                    BundleTreeNode grandParent = mapper.get(grandParentPath);
                    BundleTreeNode parent = new BundleTreeNode(category);
                    if (grandParent != null) {
                        grandParent.addChild(parent);
                    }
                    mapper.put(categoryPath, parent);
                }
            }

            // so the tree book-keeping (if any) is done and we can just add
            // the current SavedBundle to its parent node within the tree.
            BundleTreeNode parent = mapper.get(categoryPath);
            parent.addChild(new BundleTreeNode(bundle.getName(), bundle));
        }

        // return the root of the tree.
        return mapper.get("Toolbar>");
    }
    
    /**
     * Recursively builds the contents of the (first call) JPopupMenu. This is
     * where that tree annoyance stuff comes in handy. This is basically a
     * simple tree traversal situation.
     * 
     * @param node The node that we're trying to use to build the contents.
     * @param comp The component to which we add node contents.
     */
    private void buildPopupMenu(BundleTreeNode node, JComponent comp) {
        // if the current node has no bundle, it's considered a parent node
        if (node.getBundle() == null) {
            // parent nodes mean that we have to create a JMenu and add it
            JMenu test = new JMenu(node.getName());
            comp.add(test);

            // recurse through children to continue building.
            for (BundleTreeNode kid : node.getChildren()) {
                buildPopupMenu(kid, test);
            }

        } else {
            // nodes with bundles can simply be added to the JMenu
            // (or JPopupMenu)
            JMenuItem mi = new JMenuItem(node.getName());
            final SavedBundle theBundle = node.getBundle();

            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    //Do it in a thread
                    Misc.run(UIManager.this, "processBundle", theBundle);
                }
            });

            comp.add(mi);
        }
    }

    @Override
    public void initDone() {
    	super.initDone();
    	if (getStore().get(Constants.PREF_VERSION_CHECK, true)) {
        	StateManager stateManager = (StateManager) getStateManager();
    		stateManager.checkForNewerVersion(false);
    		stateManager.checkForNotice(false);
    	}
    	
    	// not super excited about how this works.
//    	showBasicWindow(true);
    	
    	initDone = true;
    	
    	showDashboard();
    }

    /**
     * Create the splash screen if needed
     */
    public void initSplash() {
        if (getProperty(PROP_SHOWSPLASH, true)
                && !getArgsManager().getNoGui()
                && !getArgsManager().getIsOffScreen()
                && !getArgsManager().testMode) {
            splash = new McvSplash(idv);
            splashMsg("Loading Programs");
        }
    }
    
    /**
     *  Create (if null)  and show the HelpTipDialog. If checkPrefs is true
     *  then only create the dialog if the PREF_HELPTIPSHOW preference is true.
     *
     * @param checkPrefs Should the user preferences be checked
     */
    /** THe help tip dialog */
    private McvHelpTipDialog helpTipDialog;
    public void initHelpTips(boolean checkPrefs) {
        try {
            if (getIdv().getArgsManager().getIsOffScreen()) {
                return;
            }
            if (checkPrefs) {
                if ( !getStore().get(McvHelpTipDialog.PREF_HELPTIPSHOW, true)) {
                    return;
                }
            }
            if (helpTipDialog == null) {
                IdvResourceManager resourceManager = getResourceManager();
                helpTipDialog = new McvHelpTipDialog(
                    resourceManager.getXmlResources(
                        resourceManager.RSC_HELPTIPS), getIdv(), getStore(),
                            getIdvClass(),
                            getStore().get(
                                McvHelpTipDialog.PREF_HELPTIPSHOW, true));
            }
            helpTipDialog.setVisible(true);
            GuiUtils.toFront(helpTipDialog);
        } catch (Throwable excp) {
            logException("Reading help tips", excp);
        }
    }
    /**
     *  If created, close the HelpTipDialog window.
     */
    public void closeHelpTips() {
        if (helpTipDialog != null) {
            helpTipDialog.setVisible(false);
        }
    }
    /**
     *  Create (if null)  and show the HelpTipDialog
     */
    public void showHelpTips() {
        initHelpTips(false);
    }

    /**
     * Populate a menu with bundles known to the <tt>PersistenceManager</tt>.
     * @param inBundleMenu The menu to populate
     */
    public void makeBundleMenu(JMenu inBundleMenu) {
    	final int bundleType = IdvPersistenceManager.BUNDLES_FAVORITES;

        JMenuItem mi;
        mi = new JMenuItem("Manage...");
        McVGuiUtils.setMenuImage(mi, Constants.ICON_FAVORITEMANAGE_SMALL);
        mi.setMnemonic(GuiUtils.charToKeyCode("M"));
        inBundleMenu.add(mi);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showBundleDialog(bundleType);
            }
        });

        final List bundles = getPersistenceManager().getBundles(bundleType);
        if (bundles.size() == 0) {
            return;
        }
        final String title =
            getPersistenceManager().getBundleTitle(bundleType);
        final String bundleDir =
            getPersistenceManager().getBundleDirectory(bundleType);

        JMenu bundleMenu = new JMenu(title);
        McVGuiUtils.setMenuImage(bundleMenu, Constants.ICON_FAVORITE_SMALL);
        bundleMenu.setMnemonic(GuiUtils.charToKeyCode(title));

//        getPersistenceManager().initBundleMenu(bundleType, bundleMenu);

        Hashtable catMenus = new Hashtable();
        inBundleMenu.addSeparator();
        inBundleMenu.add(bundleMenu);
        for (int i = 0; i < bundles.size(); i++) {
            SavedBundle bundle       = (SavedBundle) bundles.get(i);
            List        categories   = bundle.getCategories();
            JMenu       catMenu      = bundleMenu;
            String      mainCategory = "";
            for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
                String category = (String) categories.get(catIdx);
                mainCategory += "." + category;
                JMenu tmpMenu = (JMenu) catMenus.get(mainCategory);
                if (tmpMenu == null) {
                    tmpMenu = new JMenu(category);
                    catMenu.add(tmpMenu);
                    catMenus.put(mainCategory, tmpMenu);
                }
                catMenu = tmpMenu;

            }

            final SavedBundle theBundle = bundle;
            mi = new JMenuItem(bundle.getName());
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    //Do it in a thread
                    Misc.run(UIManager.this, "processBundle", theBundle);
                }
            });
            catMenu.add(mi);
        }
    }

    /**
     * Overridden to build a custom Window menu.
     * @see ucar.unidata.idv.ui.IdvUIManager#makeWindowsMenu(JMenu, IdvWindow)
     */
    @Override public void makeWindowsMenu(final JMenu windowMenu, final IdvWindow idvWindow) {
        JMenuItem mi;
        boolean first = true;

        mi = new JMenuItem("Show Data Explorer");
        McVGuiUtils.setMenuImage(mi, Constants.ICON_DATAEXPLORER_SMALL);
        mi.addActionListener(this);
        mi.setActionCommand(ACT_SHOW_DASHBOARD);
        windowMenu.add(mi);

        makeTabNavigationMenu(windowMenu);

        @SuppressWarnings("unchecked") // it's how the IDV does it.
        List windows = new ArrayList(IdvWindow.getWindows());
        for (int i = 0; i < windows.size(); i++) {
            final IdvWindow window = ((IdvWindow)windows.get(i));

            // Skip the main window
            if (window.getIsAMainWindow())
                continue;

            String title = window.getTitle();
            String titleParts[] = splitTitle(title);

            if (titleParts.length == 2)
                title = titleParts[1];

            // Skip the data explorer and display controller
            String dataSelectorNameParts[] = splitTitle(Constants.DATASELECTOR_NAME);
            if (title.equals(Constants.DATASELECTOR_NAME) || title.equals(dataSelectorNameParts[1]))
                continue;

            // Add a meaningful name if there is none
            if (title.equals(""))
                title = "<Unnamed>";

            if (window.isVisible()) {
                mi = new JMenuItem(title);
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        window.toFront();
                    }
                });

                if (first) {
                    windowMenu.addSeparator();
                    first = false;
                }

                windowMenu.add(mi);
            }
        }

        
        Msg.translateTree(windowMenu);
    }

    /**
     * 
     * @param menu
     */
    private void makeTabNavigationMenu(final JMenu menu) {
        if (!didInitActions) {
            didInitActions = true;
            initTabNavActions();
        }

        if (McVGuiUtils.getAllComponentHolders().size() <= 1)
            return;

        menu.addSeparator();

        menu.add(new JMenuItem(nextDisplayAction));
        menu.add(new JMenuItem(prevDisplayAction));
        menu.add(new JMenuItem(showDisplayAction));

        if (McVGuiUtils.getAllComponentGroups().size() > 0)
            menu.addSeparator();

        Msg.translateTree(menu);
    }
    
    /**
     * Add in the dynamic menu for displaying formulas
     *
     * @param menu edit menu to add to
     */
    public void makeFormulasMenu(JMenu menu) {
        GuiUtils.makeMenu(menu, getJythonManager().doMakeFormulaDataSourceMenuItems(null));
    }
    
    /** Whether or not the list of available actions has been initialized. */
    private boolean didInitActions = false;

    /** Key combo for the popup with list of displays. */
    private ShowDisplayAction showDisplayAction;

    /** 
     * Key combo for moving to the previous display relative to the current. For
     * key combos the lists of displays in the current window is circular.
     */
    private PrevDisplayAction prevDisplayAction;

    /** 
     * Key combo for moving to the next display relative to the current. For
     * key combos the lists of displays in the current window is circular.
     */
    private NextDisplayAction nextDisplayAction;

    /** Modifier key, like &quot;control&quot; or &quot;shift&quot;. */
    private static final String PROP_KB_MODIFIER = "mcidasv.tabbedui.display.kbmodifier";

    /** Key that pops up the list of displays. Used in conjunction with <code>PROP_KB_MODIFIER</code>. */
    private static final String PROP_KB_SELECT_DISPLAY = "mcidasv.tabbedui.display.kbselect";
    
    /** Key for moving to the previous display. Used in conjunction with <code>PROP_KB_MODIFIER</code>. */
    private static final String PROP_KB_DISPLAY_PREV = "mcidasv.tabbedui.display.kbprev";

    /** Key for moving to the next display. Used in conjunction with <code>PROP_KB_MODIFIER</code>. */
    private static final String PROP_KB_DISPLAY_NEXT = "mcidasv.tabbedui.display.kbnext";

    /** Key for showing the dashboard. Used in conjunction with <code>PROP_KB_MODIFIER</code>. */
    private static final String PROP_KB_SHOW_DASHBOARD = "mcidasv.tabbedui.display.kbdashboard";

    // TODO: make all this stuff static: mod + acc don't need to read the properties file.
    // look at: http://community.livejournal.com/jkff_en/341.html
    // look at: effective java, particularly the stuff about enums
    private void initTabNavActions() {
        String mod = idv.getProperty(PROP_KB_MODIFIER, "control") + " ";
        String acc = idv.getProperty(PROP_KB_SELECT_DISPLAY, "D");

        String stroke = mod + acc;
        showDisplayAction = new ShowDisplayAction(KeyStroke.getKeyStroke(stroke));

        acc = idv.getProperty(PROP_KB_DISPLAY_PREV, "P");
        stroke = mod + acc;
        prevDisplayAction = new PrevDisplayAction(KeyStroke.getKeyStroke(stroke));

        acc = idv.getProperty(PROP_KB_DISPLAY_NEXT, "N");
        stroke = mod + acc;
        nextDisplayAction = new NextDisplayAction(KeyStroke.getKeyStroke(stroke));
    }

    /**
     * Add all the show window keyboard shortcuts. To make keyboard shortcuts
     * global, i.e., available no matter what window is active, the appropriate 
     * actions have to be added the the window contents action and input maps.
     * 
     * FIXME: This can't be the right way to do this!
     * 
     * @param window IdvWindow that requires keyboard shortcut capability.
     */
    private void initDisplayShortcuts(IdvWindow window) {
        JComponent jcomp = window.getContents();
        jcomp.getActionMap().put("show_disp", showDisplayAction);
        jcomp.getActionMap().put("prev_disp", prevDisplayAction);
        jcomp.getActionMap().put("next_disp", nextDisplayAction);
        jcomp.getActionMap().put("show_dashboard", new AbstractAction() {
            private static final long serialVersionUID = -364947940824325949L;
            public void actionPerformed(ActionEvent evt) {
                showDashboard();
            }
        });

        String mod = getIdv().getProperty(PROP_KB_MODIFIER, "control");
        String acc = getIdv().getProperty(PROP_KB_SELECT_DISPLAY, "d");
        jcomp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(mod + " " + acc),
            "show_disp"
        );

        acc = getIdv().getProperty(PROP_KB_SHOW_DASHBOARD, "MINUS");
        jcomp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(mod + " " + acc),
            "show_dashboard"
        );

        acc = getIdv().getProperty(PROP_KB_DISPLAY_NEXT, "N");
        jcomp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(mod + " " + acc),
            "next_disp"
        );

        acc = getIdv().getProperty(PROP_KB_DISPLAY_PREV, "P");
        jcomp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(mod + " " + acc),
            "prev_disp"
        );
    }

    /**
     * Show Bruce's display selector widget.
     */
    protected void showDisplaySelector() {
        IdvWindow mainWindow = IdvWindow.getActiveWindow();
        JPanel contents = new JPanel();
        contents.setLayout(new BorderLayout());
        JComponent comp = getDisplaySelectorComponent();
        final JDialog dialog = new JDialog(mainWindow.getFrame(), "Select Display", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        contents.add(comp, BorderLayout.CENTER);
        JButton button = new JButton("OK");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                final ViewManager vm = getVMManager().getLastActiveViewManager();
                // final DisplayProps disp = getDisplayProps(vm);
                // if (disp != null)
                //    showDisplay(disp);
                final McvComponentHolder holder = (McvComponentHolder)getViewManagerHolder(vm);
                if (holder != null)
                    holder.setAsActiveTab();
                
                // have to do this on the event dispatch thread so we make
                // sure it happens after showDisplay
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        //setActiveDisplay(disp, disp.managers.indexOf(vm));
                        if (holder != null)
                            getVMManager().setLastActiveViewManager(vm);
                    }
                });

                dialog.dispose();
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(button);
        dialog.add(buttonPanel, BorderLayout.AFTER_LAST_LINE);
        JScrollPane scroller = new JScrollPane(contents);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        dialog.add(scroller, BorderLayout.CENTER);
        dialog.setSize(200, 300);
        dialog.setLocationRelativeTo(mainWindow.getFrame());
        dialog.setVisible(true);
    }

    private class ShowDisplayAction extends AbstractAction {
        private static final long serialVersionUID = -4609753725057124244L;
        private static final String ACTION_NAME = "Select Display...";
        public ShowDisplayAction(KeyStroke k) {
            super(ACTION_NAME);
            putValue(Action.ACCELERATOR_KEY, k);
        }

        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd == null)
                return;

            if (ACTION_NAME.equals(cmd)) {
                showDisplaySelector();
            } else {
                List<IdvComponentHolder> holders = McVGuiUtils.getAllComponentHolders();
                McvComponentHolder holder = null;
                int index = 0;
                try {
                    index = Integer.parseInt(cmd) - 1;
                    holder = (McvComponentHolder)holders.get(index);
                } catch (Exception ex) {}

                if (holder != null)
                    holder.setAsActiveTab();
            }
        }
    }

    private class PrevDisplayAction extends AbstractAction {
        private static final long serialVersionUID = -3551890663976755671L;
        private static final String ACTION_NAME = "Previous Display";

        public PrevDisplayAction(KeyStroke k) {
            super(ACTION_NAME);
            putValue(Action.ACCELERATOR_KEY, k);
        }

        public void actionPerformed(ActionEvent e) {
            McvComponentHolder prev = (McvComponentHolder)McVGuiUtils.getBeforeActiveHolder();
            if (prev != null)
                prev.setAsActiveTab();
        }
    }

    private class NextDisplayAction extends AbstractAction {
        private static final long serialVersionUID = 5431901451767117558L;
        private static final String ACTION_NAME = "Next Display";

        public NextDisplayAction(KeyStroke k) {
            super(ACTION_NAME);
            putValue(Action.ACCELERATOR_KEY, k);
        }

        public void actionPerformed(ActionEvent e) {
            McvComponentHolder next = (McvComponentHolder)McVGuiUtils.getAfterActiveHolder();
            if (next != null)
                next.setAsActiveTab();
        }
    }

    /**
     * Populate a "new display" menu from the available skin list. Many thanks
     * to Bruce for doing this in the venerable TabbedUIManager.
     * 
     * @param newDisplayMenu menu to populate.
     * @param inWindow Is the skinned display to be created in a window?
     * 
     * @see ucar.unidata.idv.IdvResourceManager#RSC_SKIN
     * 
     * @return Menu item populated with display skins
     */
    protected JMenuItem doMakeNewDisplayMenu(JMenuItem newDisplayMenu, 
        final boolean inWindow) 
    {
        if (newDisplayMenu != null) {

            String skinFilter = "idv.skin";
            if (!inWindow) {
                skinFilter = "mcv.skin";
            }

            final XmlResourceCollection skins =
                getResourceManager().getXmlResources(
                    IdvResourceManager.RSC_SKIN);

            Map<String, JMenu> menus = new Hashtable<String, JMenu>();
            for (int i = 0; i < skins.size(); i++) {
                final Element root = skins.getRoot(i);
                if (root == null) {
                    continue;
                }

                // filter out mcv or idv skins based on whether or not we're
                // interested in tabs or new windows.
                final String skinid = skins.getProperty("skinid", i);
                if (skinid != null && skinid.startsWith(skinFilter)) {
                    continue;
                }

                final int skinIndex = i;
                List<String> names =
                    StringUtil.split(skins.getShortName(i), ">", true, true);

                JMenuItem theMenu = newDisplayMenu;
                String path = "";
                for (int nameIdx = 0; nameIdx < names.size() - 1; nameIdx++) {
                    String catName = names.get(nameIdx);
                    path = path + ">" + catName;
                    JMenu tmpMenu = menus.get(path);
                    if (tmpMenu == null) {
                        tmpMenu = new JMenu(catName);
                        theMenu.add(tmpMenu);
                        menus.put(path, tmpMenu);
                    }
                    theMenu = tmpMenu;
                }

                final String name = names.get(names.size() - 1);

                IdvWindow window = IdvWindow.getActiveWindow();
                for (final McvComponentGroup group : McVGuiUtils.idvGroupsToMcv(window)) {
                    JMenuItem mi = new JMenuItem(name);

                    mi.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent ae) {
                            if (!inWindow) {
                                group.makeSkin(skinIndex);
                            } else {
                                createNewWindow(null, true,
                                    getStateManager().getTitle(), skins.get(
                                        skinIndex).toString(), skins.getRoot(
                                        skinIndex, false), inWindow, null);
                            }
                        }
                    });
                    theMenu.add(mi);
                }
            }

            // attach the dynamic skin menu item to the tab menu.
//            if (!inWindow) {
//                ((JMenu)newDisplayMenu).addSeparator();
//                IdvWindow window = IdvWindow.getActiveWindow();
//
//                final McvComponentGroup group =
//                    (McvComponentGroup)window.getComponentGroups().get(0);
//
//                JMenuItem mi = new JMenuItem("Choose Your Own Adventure...");
//                mi.addActionListener(new ActionListener() {
//
//                    public void actionPerformed(ActionEvent e) {
//                        makeDynamicSkin(group);
//                    }
//                });
//                newDisplayMenu.add(mi);
//            }
        }
        return newDisplayMenu;
    }

    // for the time being just create some basic viewmanagers.
//    public void makeDynamicSkin(McvComponentGroup group) {
//        // so I have my megastring (which I hate--a class that can generate XML would be cooler) (though it would boil down to the same thing...)
//        try {
//            Document doc = XmlUtil.getDocument(SKIN_TEMPLATE);
//            Element root = doc.getDocumentElement();
//            Element rightChild = doc.createElement("idv.view");
//            rightChild.setAttribute("class", "ucar.unidata.idv.TransectViewManager");
//            rightChild.setAttribute("viewid", "viewright1337");
//            rightChild.setAttribute("id", "viewright");
//            rightChild.setAttribute("properties", "name=Panel 1;clickToFocus=true;showToolBars=true;shareViews=true;showControlLegend=false;initialSplitPaneLocation=0.2;legendOnLeft=true;size=300:400;shareGroup=view%versionuid%;");
//
//            Element leftChild = doc.createElement("idv.view");
//            leftChild.setAttribute("class", "ucar.unidata.idv.MapViewManager");
//            leftChild.setAttribute("viewid", "viewleft1337");
//            leftChild.setAttribute("id", "viewleft");
//            leftChild.setAttribute("properties", "name=Panel 2;clickToFocus=true;showToolBars=true;shareViews=true;showControlLegend=false;size=300:400;shareGroup=view%versionuid%;");
//
//            Element startNode = XmlUtil.findElement(root, "splitpane", "embeddednode", "true");
//            startNode.appendChild(rightChild);
//            startNode.appendChild(leftChild);
//            group.makeDynamicSkin(root);
//        } catch (Exception e) {
//            LogUtil.logException("Error: parsing skin template:", e);
//        }
//    }
//
//    private static final String SKIN_TEMPLATE = 
//        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//        "<skin embedded=\"true\">\n" +
//        "  <ui>\n" +
//        "    <panel layout=\"border\" bgcolor=\"red\">\n" +
//        "      <idv.menubar place=\"North\"/>\n" +
//        "      <panel layout=\"border\" place=\"Center\">\n" +
//        "        <panel layout=\"flow\" place=\"North\">\n" +
//        "          <idv.toolbar id=\"idv.toolbar\" place=\"West\"/>\n" +
//        "          <panel id=\"idv.favoritesbar\" place=\"North\"/>\n" +
//        "        </panel>\n" +
//        "        <splitpane embeddednode=\"true\" resizeweight=\"0.5\" onetouchexpandable=\"true\" orientation=\"h\" bgcolor=\"blue\" layout=\"grid\" cols=\"2\" place=\"Center\">\n" +
//        "        </splitpane>\n" +
//        "      </panel>\n" +
//        "      <component idref=\"bottom_bar\"/>\n" +
//        "    </panel>\n" +
//        "  </ui>\n" +
//        "  <styles>\n" +
//        "    <style class=\"iconbtn\" space=\"2\" mouse_enter=\"ui.setText(idv.messagelabel,prop:tooltip);ui.setBorder(this,etched);\" mouse_exit=\"ui.setText(idv.messagelabel,);ui.setBorder(this,button);\"/>\n" +
//        "    <style class=\"textbtn\" space=\"2\" mouse_enter=\"ui.setText(idv.messagelabel,prop:tooltip)\" mouse_exit=\"ui.setText(idv.messagelabel,)\"/>\n" +
//        "  </styles>\n" +
//        "  <components>\n" +
//        "    <idv.statusbar place=\"South\" id=\"bottom_bar\"/>\n" +
//        "  </components>\n" +
//        "  <properties>\n" +
//        "    <property name=\"icon.wait.wait\" value=\"/ucar/unidata/idv/images/wait.gif\"/>\n" +
//        "  </properties>\n" +
//        "</skin>\n";

    private int holderCount;
    
    /**
     * Associates a given ViewManager with a given ComponentHolder.
     * 
     * @param vm The ViewManager that is inside <tt>holder</tt>.
     * @param holder The ComponentHolder that contains <tt>vm</tt>.
     */
    public void setViewManagerHolder(ViewManager vm, ComponentHolder holder) {
        viewManagers.put(vm, holder);
        holderCount = getComponentHolders().size();
    }

    public Set<ComponentHolder> getComponentHolders() {
        return newHashSet(viewManagers.values());
    }

    public int getComponentHolderCount() {
        return holderCount;
    }

    public int getComponentGroupCount() {
        return getComponentGroups().size();
    }

    /**
     * Returns the ComponentHolder containing the given ViewManager.
     * 
     * @param vm The ViewManager whose ComponentHolder is needed.
     * 
     * @return Either null or the ComponentHolder.
     */
    public ComponentHolder getViewManagerHolder(ViewManager vm) {
        return viewManagers.get(vm);
    }

    /**
     * Disassociate a given ViewManager from its ComponentHolder.
     * 
     * @return The associated ComponentHolder.
     */
    public ComponentHolder removeViewManagerHolder(ViewManager vm) {
        ComponentHolder holder = viewManagers.remove(vm);
        holderCount = getComponentHolders().size();
        return holder;
    }

    /**
     * Overridden to keep the dashboard around after it's initially created.
     * Also give the user the ability to show a particular tab.
     * @see ucar.unidata.idv.ui.IdvUIManager#showDashboard()
     */
    @Override
    public void showDashboard() {
        showDashboard("");
    }

    /**
     * Creates the McVViewPanel component that shows up in the dashboard.
     */
    @Override
    protected ViewPanel doMakeViewPanel() {
        ViewPanel vp = new McIDASVViewPanel(idv);
        vp.getContents();
        return vp;
    }

    /**
     * @return A map of skin ids to their index within the skin resource.
     */
    private Map<String, Integer> readSkinIds() {
        Map<String, Integer> ids = new HashMap<String, Integer>();
        XmlResourceCollection skins = getResourceManager().getXmlResources(IdvResourceManager.RSC_SKIN);
        for (int i = 0; i < skins.size(); i++) {
            String id = skins.getProperty("skinid", i);
            if (id != null)
                ids.put(id, i);
        }
        return ids;
    }

    /**
     * Adds a skinned component holder to the active component group.
     * 
     * @param skinId The value of the skin's skinid attribute.
     */
    public void createNewTab(final String skinId) {
        IdvComponentGroup group = 
            McVGuiUtils.getComponentGroup(IdvWindow.getActiveWindow());

        if (skinIds.containsKey(skinId))
            group.makeSkin(skinIds.get(skinId));
    }

    /**
     * Method to do the work of showing the Data Explorer (nee Dashboard)
     */
    @SuppressWarnings("unchecked") // IdvWindow.getWindows only adds IdvWindows.
    public void showDashboard(String tabName) {
        if (!initDone) {
            return;
        } else if (dashboard == null) {
            showWaitCursor();
            doMakeBasicWindows();
            showNormalCursor();
            String title = makeTitle(getStateManager().getTitle(), Constants.DATASELECTOR_NAME);
            for (IdvWindow window : (List<IdvWindow>)IdvWindow.getWindows()) {
                if (title.equals(window.getTitle())) {
                    dashboard = window;
                    dashboard.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
                }
            }
        } else {
            dashboard.show();
        }

        if (tabName.equals(""))
            return;

        // Dig two panels deep looking for a JTabbedPane
        // If you find one, try to show the requested tab name
        JComponent contents = dashboard.getContents();
        JComponent component = (JComponent)contents.getComponent(0);
        JTabbedPane tPane = null;
        if (component instanceof JTabbedPane) {
            tPane = (JTabbedPane)component;
        }
        else {
            JComponent component2 = (JComponent)component.getComponent(0);
            if (component2 instanceof JTabbedPane) {
                tPane = (JTabbedPane)component2;
            }
        }
        if (tPane != null) {
            for (int i=0; i<tPane.getTabCount(); i++) {
                if (tabName.equals(tPane.getTitleAt(i))) {
                    tPane.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Show the support request form
     *
     * @param description Default value for the description form entry
     * @param stackTrace The stack trace that caused this error.
     * @param dialog The dialog to put the gui in, if non-null.
     */
    public void showSupportForm(final String description, 
        final String stackTrace, final JDialog dialog) 
    {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // TODO: mcvstatecollector should have a way to gather the
                // exception information..
                McIDASV mcv = (McIDASV)getIdv();
                new SupportForm(getStore(), new McvStateCollector(mcv)).setVisible(true);
            }
        });
    }

    /**
     * Attempts to locate and display a dashboard component using an ID.
     * 
     * @param id ID of the desired component.
     * 
     * @return True if <code>id</code> corresponds to a component. False otherwise.
     */
    public boolean showDashboardComponent(String id) {
    	Object comp = findComponent(id);
    	if (comp != null) {
    		GuiUtils.showComponentInTabs((JComponent)comp);
    		return true;
    	} else {
    		super.showDashboard();
    		for (IdvWindow window : (List<IdvWindow>)IdvWindow.getWindows()) {
    			String title = makeTitle(
    				getStateManager().getTitle(),
    				Constants.DATASELECTOR_NAME
    			);
    			if (title.equals(window.getTitle())) {
    				dashboard = window;
    				dashboard.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    			}
    		}
    	}
    	return false;
    }

    /**
     * Close and dispose of the splash window (if it has been created).
     */
    public void splashClose() {
        if (splash != null)
            splash.doClose();
    }

    /**
     * Show a message in the splash screen (if it exists)
     *
     * @param m The message to show
     */
    public void splashMsg(String m) {
        if (splash != null)
            splash.splashMsg(m);
    }

    /**
     * <p>
     * Uses a given toolbar editor to repopulate all toolbars so that they 
     * correspond to the user's choice of actions.
     * </p>
     * 
     * @param tbe The toolbar editor that contains the actions the user wants.
     */
    public void setCurrentToolbars(final McvToolbarEditor tbe) {
        List<TwoFacedObject> tfos = tbe.getTLP().getCurrentEntries();
        List<String> buttonIds = new ArrayList<String>();
        for (TwoFacedObject tfo : tfos) {
            if (McvToolbarEditor.isSpace(tfo))
                buttonIds.add((String)null);
            else
                buttonIds.add(TwoFacedObject.getIdString(tfo));
        }

        cachedButtons = buttonIds;

        for (JToolBar toolbar : toolbars) {
            toolbar.setVisible(false);
            populateToolbar(toolbar);
            toolbar.setVisible(true);
        }
    }

    /**
     * Append a string and object to the buffer
     *
     * @param sb  StringBuffer to append to
     * @param name  Name of the object
     * @param value  the object value
     */
    private void append(StringBuffer sb, String name, Object value) {
        sb.append("<b>").append(name).append("</b>: ").append(value).append("<br>");
    }

    private JMenuItem makeControlDescriptorItem(ControlDescriptor cd) {
        JMenuItem mi = new JMenuItem();
        if (cd != null) {
            mi = new JMenuItem(cd.getLabel());
            mi.addActionListener(new ObjectListener(cd) {
                public void actionPerformed(ActionEvent ev) {
                    idv.doMakeControl(new ArrayList(),
                        (ControlDescriptor)theObject);
                }
            });
        }
        return mi;
    }

    /* (non-javadoc)
     * Overridden so that the toolbar will update upon saving a bundle.
     */
    @Override public void displayTemplatesChanged() {
        super.displayTemplatesChanged();
        for (JToolBar toolbar : toolbars) {
            toolbar.setVisible(false);
            populateToolbar(toolbar);
            toolbar.setVisible(true);
        }
    }

    /**
     * Called when there has been any change to the favorite bundles and is
     * most useful for triggering an update to the {@literal "toolbar bundles"}.
     */
    @Override public void favoriteBundlesChanged() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (JToolBar toolbar : toolbars) {
                    toolbar.setVisible(false);
                    populateToolbar(toolbar);
                    toolbar.setVisible(true);
                }
            }
        });
    }

    /**
     * Show the support request form in a non-swing thread. We do this because we cannot
     * call the HttpFormEntry.showUI from a swing thread
     *
     * @param description Default value for the description form entry
     * @param stackTrace The stack trace that caused this error.
     * @param dialog The dialog to put the gui in, if non-null.
     */

    private void showSupportFormInThread(String description,
                                         String stackTrace, JDialog dialog) {
        List<HttpFormEntry> entries = new ArrayList<HttpFormEntry>();

        StringBuffer extra   = new StringBuffer("<h3>McIDAS-V</h3>\n");
        Hashtable<String, String> table = 
            ((StateManager)getStateManager()).getVersionInfo();
        append(extra, "mcv.version.general", table.get("mcv.version.general"));
        append(extra, "mcv.version.build", table.get("mcv.version.build"));
        append(extra, "idv.version.general", table.get("idv.version.general"));
        append(extra, "idv.version.build", table.get("idv.version.build"));

        extra.append("<h3>OS</h3>\n");
        append(extra, "os.name", System.getProperty("os.name"));
        append(extra, "os.arch", System.getProperty("os.arch"));
        append(extra, "os.version", System.getProperty("os.version"));

        extra.append("<h3>Java</h3>\n");
        append(extra, "java.vendor", System.getProperty("java.vendor"));
        append(extra, "java.version", System.getProperty("java.version"));
        append(extra, "java.home", System.getProperty("java.home"));

        StringBuffer javaInfo = new StringBuffer();
        javaInfo.append("Java: home: " + System.getProperty("java.home"));
        javaInfo.append(" version: " + System.getProperty("java.version"));

        Class c = null;
        try {
            c = Class.forName("javax.media.j3d.VirtualUniverse");
            Method method = Misc.findMethod(c, "getProperties",
                                            new Class[] {});
            if (method == null) {
                javaInfo.append("j3d <1.3");
            } else {
                try {
                    Map m = (Map)method.invoke(c, new Object[] {});
                    javaInfo.append(" j3d:" + m.get("j3d.version"));
                    append(extra, "j3d.version", m.get("j3d.version"));
                    append(extra, "j3d.vendor", m.get("j3d.vendor"));
                    append(extra, "j3d.renderer", m.get("j3d.renderer"));
                } catch (Exception exc) {
                    javaInfo.append(" j3d:" + "unknown");
                }
            }
        } catch (ClassNotFoundException exc) {
            append(extra, "j3d", "none");
        }

        boolean persistCC = getStore().get("mcv.supportreq.cc", true);

        JCheckBox ccMyself = new JCheckBox("Send Copy of Support Request to Me", persistCC);
        ccMyself.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                JCheckBox cb = (JCheckBox)e.getSource();
                getStore().put("mcv.supportreq.cc", cb.isSelected());
            }
        });

        boolean doWrap = idv.getProperty(PROP_WRAP_SUPPORT_DESC, true);

        HttpFormEntry descriptionEntry;
        HttpFormEntry nameEntry;
        HttpFormEntry emailEntry;
        HttpFormEntry orgEntry;

        entries.add(nameEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "form_data[fromName]", "Name:",
                getStore().get(PROP_HELP_NAME, (String) null)));
        entries.add(emailEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "form_data[email]", "Your Email:",
                getStore().get(PROP_HELP_EMAIL, (String) null)));
        entries.add(orgEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "form_data[organization]", "Organization:",
                getStore().get(PROP_HELP_ORG, (String) null)));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                                      "form_data[subject]", "Subject:"));

        entries.add(
            new HttpFormEntry(
                HttpFormEntry.TYPE_LABEL, "",
                "<html>Please provide a <i>thorough</i> description of the problem you encountered:</html>"));
        entries.add(descriptionEntry =
            new FormEntry(doWrap, HttpFormEntry.TYPE_AREA,
                              "form_data[description]", "Description:",
                              description, 5, 30, true));

        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_FILE,
                                      "form_data[att_two]", "Attachment 1:", "",
                                      false));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_FILE,
                                      "form_data[att_three]", "Attachment 2:", "",
                                      false));

        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN,
                                      "form_data[submit]", "", "Send Email"));
        
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN,
                                      "form_data[p_version]", "",
                                      getStateManager().getVersion()
                                      + " build date:"
                                      + getStateManager().getBuildDate()));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN,
                                      "form_data[opsys]", "",
                                      System.getProperty("os.name")));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN,
                                      "form_data[hardware]", "",
                                      javaInfo.toString()));
        
        JLabel topLabel = 
            new JLabel("<html>This form allows you to send a support request to the McIDAS Help Desk.<br></html>");

        JCheckBox includeBundleCbx =
            new JCheckBox("Include Current State as Bundle", false);

        List<JCheckBox> checkboxes = list(includeBundleCbx, ccMyself);

        boolean alreadyHaveDialog = true;
        if (dialog == null) {
            // NOTE: if the dialog is modeless you can leave alreadyHaveDialog
            // alone. If the dialog is modal you need to set alreadyHaveDialog
            // to false.
            // If alreadyHaveDialog is false with a modeless dialog, the later
            // call to HttpFormEntry.showUI will return false and break out of
            // the while loop without talking to the HTTP server.
            dialog = GuiUtils.createDialog(LogUtil.getCurrentWindow(),
                                           "Support Request Form", false);
//            alreadyHaveDialog = false;
        }

        JLabel statusLabel = GuiUtils.cLabel(" ");
        JComponent bottom = GuiUtils.vbox(GuiUtils.leftVbox(checkboxes), statusLabel);

        while (true) {
            //Show form. Check if user pressed cancel.
            statusLabel.setText(" ");
            if ( !HttpFormEntry.showUI(entries, GuiUtils.inset(topLabel, 10),
                                       bottom, dialog, alreadyHaveDialog)) {
                break;
            }
            statusLabel.setText("Posting support request...");

            //Save persistent state
            getStore().put(PROP_HELP_NAME, nameEntry.getValue());
            getStore().put(PROP_HELP_ORG, orgEntry.getValue());
            getStore().put(PROP_HELP_EMAIL, emailEntry.getValue());
            getStore().save();

            List<HttpFormEntry> entriesToPost = 
                new ArrayList<HttpFormEntry>(entries);

            if ((stackTrace != null) && (stackTrace.length() > 0)) {
                entriesToPost.remove(descriptionEntry);
                String newDescription =
                    descriptionEntry.getValue()
                    + "\n\n******************\nStack trace:\n" + stackTrace;
                entriesToPost.add(
                    new HttpFormEntry(
                        HttpFormEntry.TYPE_HIDDEN, "form_data[description]",
                        "Description:", newDescription, 5, 30, true));
            }

            try {
                extra.append(idv.getPluginManager().getPluginHtml());
                extra.append(getResourceManager().getHtmlView());

                entriesToPost.add(new HttpFormEntry("form_data[att_extra]",
                    "extra.html", extra.toString().getBytes()));

                if (includeBundleCbx.isSelected()) {
                    entriesToPost.add(
                        new HttpFormEntry(
                            "form_data[att_state]", "bundle" + Constants.SUFFIX_MCV,
                            idv.getPersistenceManager().getBundleXml(
                                true).getBytes()));
                }
                entriesToPost.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN, 
                    "form_data[cc_user]", "", 
                    Boolean.toString(getStore().get("mcv.supportreq.cc", true))));

                String[] results = 
                    HttpFormEntry.doPost(entriesToPost, SUPPORT_REQ_URL);

                if (results[0] != null) {
                    GuiUtils.showHtmlDialog(
                        results[0], "Support Request Response - Error",
                        "Support Request Response - Error", null, true);
                    continue;
                }
                String html = results[1];
                if (html.toLowerCase().indexOf("your email has been sent")
                        >= 0) {
                    LogUtil.userMessage("Your support request has been sent");
                    break;
                } else if (html.toLowerCase().indexOf("required fields")
                           >= 0) {
                    LogUtil.userErrorMessage(
                        "<html>There was a problem submitting your request. <br>Is your email correct?</html>");
                } else {
                    GuiUtils.showHtmlDialog(
                        html, "Unknown Support Request Response",
                        "Unknown Support Request Response", null, true);
                    System.err.println(html.toLowerCase());
                }
            } catch (Exception exc) {
                LogUtil.logException("Doing support request form", exc);
            }
        }
        dialog.dispose();
    }

    @Override protected IdvXmlUi doMakeIdvXmlUi(IdvWindow window, 
        List viewManagers, Element skinRoot) 
    {
        return new McIDASVXmlUi(window, viewManagers, idv, skinRoot);
    }

    /**
     * DeInitialize the given menu before it is shown
     * @see ucar.unidata.idv.ui.IdvUIManager#historyMenuSelected(JMenu)
     */
    @Override
    protected void handleMenuDeSelected(final String id, final JMenu menu, final IdvWindow idvWindow) {
    	super.handleMenuDeSelected(id, menu, idvWindow);
    }

    /**
     * Initialize the given menu before it is shown
     * @see ucar.unidata.idv.ui.IdvUIManager#historyMenuSelected(JMenu)
     */
    @Override
    protected void handleMenuSelected(final String id, final JMenu menu, final IdvWindow idvWindow) {
        if (id.equals(MENU_NEWVIEWS)) {
            ViewManager last = getVMManager().getLastActiveViewManager();
            menu.removeAll();
            makeViewStateMenu(menu, last);
        } else if (id.equals("bundles")) {
            menu.removeAll();
            makeBundleMenu(menu);
        } else if (id.equals(MENU_NEWDISPLAY_TAB)) {
            menu.removeAll();
            doMakeNewDisplayMenu(menu, false);
        } else if (id.equals(MENU_NEWDISPLAY)) {
            menu.removeAll();
            doMakeNewDisplayMenu(menu, true);
        } else if (id.equals("menu.tools.projections.deletesaved")) {
            menu.removeAll();
            makeDeleteViewsMenu(menu);
        } else if (id.equals("file.default.layout")) {
            makeDefaultLayoutMenu(menu);
        } else if (id.equals("tools.formulas")) {
            menu.removeAll();
            makeFormulasMenu(menu);
        } else {
            super.handleMenuSelected(id, menu, idvWindow);
        }
    }

    private boolean didTabs = false;
    private boolean didNewWindow = false;

    public void makeDefaultLayoutMenu(final JMenu menu) {
        if (menu == null)
            throw new NullPointerException("Must provide a non-null default layout menu");

        menu.removeAll();
        JMenuItem saveLayout = new JMenuItem("Save");
		McVGuiUtils.setMenuImage(saveLayout, Constants.ICON_DEFAULTLAYOUTADD_SMALL);
        saveLayout.setToolTipText("Save as default layout");
        saveLayout.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                ((McIDASV)idv).doSaveAsDefaultLayout();
            }
        });

        JMenuItem removeLayout = new JMenuItem("Remove");
		McVGuiUtils.setMenuImage(removeLayout, Constants.ICON_DEFAULTLAYOUTDELETE_SMALL);
        removeLayout.setToolTipText("Remove saved default layout");
        removeLayout.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                idv.doClearDefaults();
            }
        });

        removeLayout.setEnabled(((McIDASV)idv).hasDefaultLayout());

        menu.add(saveLayout);
        menu.add(removeLayout);
    }

    /**
     * Bundles any compatible {@link ViewManager} states into {@link JMenuItem}s
     * and adds said {@code JMenuItem}s to {@code menu}. Incompatible states are
     * ignored.
     * 
     * <p>Each {@code JMenuItem} (except those under the {@literal "Delete"} menu--apologies)
     * associates a {@literal "view state"} and an {@link ObjectListener}. 
     * The {@code ObjectListener} uses this associated view state to attempt reinitialization
     * of {@code vm}.
     * 
     * <p>Override reasoning:
     * <ul>
     *   <li>terminology ({@literal "views"} rather than {@literal "viewpoints"}).</li>
     *   <li>
     *     use of {@link #filterVMMStatesWithVM(ViewManager, Collection)} to
     *     properly detect the {@literal "no saved views"} case.
     *   </li>
     * </ul>
     * 
     * @param menu Menu to populate. Should not be {@code null}.
     * @param vm {@code ViewManager} that might get reinitialized. Should not be {@code null}. 
     * 
     * @see ViewManager#initWith(ViewManager, boolean)
     * @see ViewManager#initWith(ViewState)
     * @see IdvUIManager#makeViewStateMenu(JMenu, ViewManager)
     */
    @Override public void makeViewStateMenu(final JMenu menu, final ViewManager vm) {
        List<TwoFacedObject> vmStates = filterVMMStatesWithVM(vm, getVMManager().getVMState());
        if (vmStates.isEmpty()) {
            JMenuItem item = new JMenuItem(Msg.msg("No Saved Views"));
            item.setEnabled(false);
            menu.add(item);
        } else {
            JMenu deleteMenu = new JMenu("Delete");
            makeDeleteViewsMenu(deleteMenu);
            menu.add(deleteMenu);
        }

        for (TwoFacedObject tfo : vmStates) {
          JMenuItem mi = new JMenuItem(tfo.getLabel().toString());
          menu.add(mi);
          mi.addActionListener(new ObjectListener(tfo.getId()) {
              public void actionPerformed(final ActionEvent e) {
                  if (vm == null)
                      return;

                  if (theObject instanceof ViewManager) {
                      vm.initWith((ViewManager)theObject, true);
                  } else if (theObject instanceof ViewState) {
                      try {
                          vm.initWith((ViewState)theObject);
                      } catch (Throwable ex) {
                          logException("Initializing view with ViewState", ex);
                      }
                  } else {
                      LogUtil.consoleMessage("UIManager.makeViewStateMenu: Object of unknown type: "+theObject.getClass().getName());
                  }
              }
          });
      }
    }

    /**
     * Returns a list of {@link TwoFacedObject}s that are known to be 
     * compatible with {@code vm}.
     * 
     * <p>This method is currently capable of dealing with {@code TwoFacedObject}s and
     * {@link ViewState}s within {@code states}. Any other types are ignored.
     * 
     * @param vm {@link ViewManager} to use for compatibility tests. {@code null} is allowed.
     * @param states Collection of objects to test against {@code vm}. {@code null} is allowed.
     * 
     * @return Either a {@link List} of compatible {@literal "view states"} or an empty {@code List}.
     * 
     * @see ViewManager#isCompatibleWith(ViewManager)
     * @see ViewManager#isCompatibleWith(ViewState)
     * @see #makeViewStateMenu(JMenu, ViewManager)
     */
    public static List<TwoFacedObject> filterVMMStatesWithVM(final ViewManager vm, final Collection<?> states) {
        if (vm == null || states == null || states.isEmpty())
            return Collections.emptyList();

        List<TwoFacedObject> validStates = new ArrayList<TwoFacedObject>(states.size());
        for (Object obj : states) {
            TwoFacedObject tfo = null;
            if (obj instanceof TwoFacedObject) {
                tfo = (TwoFacedObject)obj;
                if (vm.isCompatibleWith((ViewManager)tfo.getId())) {
                    continue;
                }
            } else if (obj instanceof ViewState) {
                if (!vm.isCompatibleWith((ViewState)obj)) {
                    continue;
                }
                tfo = new TwoFacedObject(((ViewState)obj).getName(), obj);
            } else {
                LogUtil.consoleMessage("UIManager.filterVMMStatesWithVM: Object of unknown type: "+obj.getClass().getName());
                continue;
            }
            validStates.add(tfo);
        }
        return validStates;
    }

    /**
     * Overridden to build a custom Display menu.
     * @see ucar.unidata.idv.ui.IdvUIManager#initializeDisplayMenu(JMenu)
     */
    @Override
    protected void initializeDisplayMenu(JMenu displayMenu) {
        JMenu m;
        JMenuItem mi;

        // Get the list of possible standalone control descriptors
        Hashtable controlsHash = new Hashtable();
        List controlDescriptors = getStandAloneControlDescriptors();
        for (int i = 0; i < controlDescriptors.size(); i++) {
            ControlDescriptor cd = (ControlDescriptor)controlDescriptors.get(i);
            String cdLabel = cd.getLabel();
            if (cdLabel.equals("Range Rings"))
                controlsHash.put(cdLabel, cd);
            else if (cdLabel.equals("Range and Bearing"))
                controlsHash.put(cdLabel, cd);
            else if (cdLabel.equals("Location Indicator"))
                controlsHash.put(cdLabel, cd);
            else if (cdLabel.equals("Drawing Control"))
                controlsHash.put(cdLabel, cd);
            else if (cdLabel.equals("Transect Drawing Control"))
                controlsHash.put(cdLabel, cd);
        }
        
        // Build the menu
        ControlDescriptor cd;
        
        mi = new JMenuItem("Create Layer from Data Source...");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
            	showDashboard("Data Sources");
            }
        });
        displayMenu.add(mi);
        
        mi = new JMenuItem("Layer Controls...");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
            	showDashboard("Layer Controls");
            }
        });
        displayMenu.add(mi);
        
        displayMenu.addSeparator();
        
        cd = (ControlDescriptor)controlsHash.get("Range Rings");
        mi = makeControlDescriptorItem(cd);
        mi.setText("Add Range Rings");
        displayMenu.add(mi);
        
        cd = (ControlDescriptor)controlsHash.get("Range and Bearing");
        mi = makeControlDescriptorItem(cd);
        McVGuiUtils.setMenuImage(mi, Constants.ICON_RANGEANDBEARING_SMALL);
        mi.setText("Add Range and Bearing");
        displayMenu.add(mi);
        
        displayMenu.addSeparator();
        
        cd = (ControlDescriptor)controlsHash.get("Transect Drawing Control");
        mi = makeControlDescriptorItem(cd);
        mi.setText("Draw Transect...");
        displayMenu.add(mi);
        
        cd = (ControlDescriptor)controlsHash.get("Drawing Control");
        mi = makeControlDescriptorItem(cd);
        mi.setText("Draw Freely...");
        displayMenu.add(mi);
        
        displayMenu.addSeparator();
        
        cd = (ControlDescriptor)controlsHash.get("Location Indicator");
        mi = makeControlDescriptorItem(cd);
        McVGuiUtils.setMenuImage(mi, Constants.ICON_LOCATION_SMALL);
        mi.setText("Add Location Indicator");
        displayMenu.add(mi);
        
        ControlDescriptor locationDescriptor =
        	idv.getControlDescriptor("locationcontrol");
        if (locationDescriptor != null) {
        	List stations = idv.getLocationList();
        	ObjectListener listener = new ObjectListener(locationDescriptor) {
        		public void actionPerformed(ActionEvent ae, Object obj) {
        			addStationDisplay((NamedStationTable) obj, (ControlDescriptor) theObject);
        		}
        	};
        	List menuItems = NamedStationTable.makeMenuItems(stations, listener);
        	displayMenu.add(GuiUtils.makeMenu("Plot Location Labels", menuItems));
        }
        
        displayMenu.addSeparator();
        
        mi = new JMenuItem("Add Background Image");
        McVGuiUtils.setMenuImage(mi, Constants.ICON_BACKGROUND_SMALL);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                getIdv().doMakeBackgroundImage();
            }
        });
        displayMenu.add(mi);
        
        mi = new JMenuItem("Reset Map Layer to Defaults");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
            	// TODO: Call IdvUIManager.addDefaultMap()... should be made private
//                addDefaultMap();
                ControlDescriptor mapDescriptor =
                    idv.getControlDescriptor("mapdisplay");
                if (mapDescriptor == null) {
                    return;
                }
                String attrs =
                    "initializeAsDefault=true;displayName=Default Background Maps;";
                idv.doMakeControl(new ArrayList(), mapDescriptor, attrs, null);
            }
        });
        displayMenu.add(mi);
        
        Msg.translateTree(displayMenu);
    }

    /**
     * Get the window title from the skin
     *
     * @param index  the skin index
     *
     * @return  the title
     */
    private String getWindowTitleFromSkin(final int index) {
        if (!skinToTitle.containsKey(index)) {
            IdvResourceManager mngr = getResourceManager();
            XmlResourceCollection skins = mngr.getXmlResources(mngr.RSC_SKIN);
            List<String> names = StringUtil.split(skins.getShortName(index), ">", true, true);
            String title = getStateManager().getTitle();
            if (names.size() > 0)
                title = title + " - " + StringUtil.join(" - ", names);
            skinToTitle.put(index, title);
        }
        return skinToTitle.get(index);
    }

    @SuppressWarnings("unchecked")
    @Override public Hashtable getMenuIds() {
        return menuIds;
    }

    @SuppressWarnings("unchecked")
    @Override public JMenuBar doMakeMenuBar(final IdvWindow idvWindow) {
        Hashtable<String, JMenuItem> menuMap = new Hashtable<String, JMenuItem>();
        JMenuBar menuBar = new JMenuBar();
        final IdvResourceManager mngr = getResourceManager();
        XmlResourceCollection xrc = mngr.getXmlResources(mngr.RSC_MENUBAR);
        Hashtable<String, ImageIcon> actionIcons = new Hashtable<String, ImageIcon>();

        for (int i = 0; i < xrc.size(); i++)
            GuiUtils.processXmlMenuBar(xrc.getRoot(i), menuBar, getIdv(), menuMap, actionIcons);

        menuIds = new Hashtable<String, JMenuItem>(menuMap);

        // Ensure that the "help" menu is the last menu.
        JMenuItem helpMenu = menuMap.get(MENU_HELP);
        if (helpMenu != null) {
            menuBar.remove(helpMenu);
            menuBar.add(helpMenu);
        }

        //TODO: Perhaps we will put the different skins in the menu?
        JMenu newDisplayMenu = (JMenu)menuMap.get(MENU_NEWDISPLAY);
        if (newDisplayMenu != null)
            GuiUtils.makeMenu(newDisplayMenu, makeSkinMenuItems(makeMenuBarActionListener(), true, false));

//        final JMenu publishMenu = menuMap.get(MENU_PUBLISH);
//        if (publishMenu != null) {
//            if (!getPublishManager().isPublishingEnabled())
//                publishMenu.getParent().remove(publishMenu);
//            else
//                getPublishManager().initMenu(publishMenu);
//        }

        for (Entry<String, JMenuItem> e : menuMap.entrySet()) {
            if (!(e.getValue() instanceof JMenu))
                continue;
            String menuId = e.getKey();
            JMenu menu = (JMenu)e.getValue();
            menu.addMenuListener(makeMenuBarListener(menuId, menu, idvWindow));
        }
        return menuBar;
    }

    private final ActionListener makeMenuBarActionListener() {
        final IdvResourceManager mngr = getResourceManager();
        return new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                XmlResourceCollection skins = mngr.getXmlResources(mngr.RSC_SKIN);
                int skinIndex = ((Integer)ae.getSource()).intValue();
                createNewWindow(null, true, getWindowTitleFromSkin(skinIndex),
                    skins.get(skinIndex).toString(), 
                    skins.getRoot(skinIndex, false), true, null);
            }
        };
    }

    private final MenuListener makeMenuBarListener(final String id, final JMenu menu, final IdvWindow idvWindow) {
        return new MenuListener() {
            public void menuCanceled(final MenuEvent e) { }
            public void menuDeselected(final MenuEvent e) { handleMenuDeSelected(id, menu, idvWindow); }
            public void menuSelected(final MenuEvent e) { handleMenuSelected(id, menu, idvWindow); }
        };
    }

    /**
     * Handle mouse clicks that occur within the toolbar.
     */
    private class PopupListener extends MouseAdapter {

        private JPopupMenu popup;

        public PopupListener(JPopupMenu p) {
            popup = p;
        }

        // handle right clicks on os x and linux
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger() == true)
                popup.show(e.getComponent(), e.getX(), e.getY());
        }

        // Windows doesn't seem to trigger mousePressed() for right clicks, but
        // never fear; mouseReleased() does the job.
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger() == true)
                popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * Handle (polymorphically) the {@link ucar.unidata.idv.ui.DataControlDialog}.
     * This dialog is used to either select a display control to create
     * or is used to set the timers used for a {@link ucar.unidata.data.DataSource}.
     *
     * @param dcd The dialog
     */
    public void processDialog(DataControlDialog dcd) {
    	int estimatedMB = getEstimatedMegabytes(dcd);
    	
    	if (estimatedMB > 0) {
            double totalMem = Runtime.getRuntime().maxMemory();
            double highMem = Runtime.getRuntime().totalMemory();
            double freeMem = Runtime.getRuntime().freeMemory();
            double usedMem = (highMem - freeMem);
            int availableMB = Math.round( ((float)totalMem - (float)usedMem) / 1024f / 1024f);
            int percentOfAvailable = Math.round((float)estimatedMB / (float)availableMB * 100f);
            
            if (percentOfAvailable > 95) {
            	String message = "<html>You are attempting to load " + estimatedMB + "MB of data,<br>";
            	message += "which exceeds 95% of total amount available (" + availableMB +"MB).<br>";
            	message += "Data load cancelled.</html>";
            	JComponent msgLabel = new JLabel(message);
    			GuiUtils.showDialog("Data Size", msgLabel);
    			return;
            }
            else if (percentOfAvailable >= 75) {
            	String message = "<html>You are attempting to load " + estimatedMB + "MB of data,<br>";
            	message += percentOfAvailable + "% of the total amount available (" + availableMB + "MB).<br>";
            	message += "Continue loading data?</html>";
            	JComponent msgLabel = new JLabel(message);
    			if (!GuiUtils.askOkCancel("Data Size", msgLabel)) {
    				return;
    			}
            }
    	}
    	
    	super.processDialog(dcd);
    }

    /**
     * Estimate the number of megabytes that will be used by this data selection
     */
    protected int getEstimatedMegabytes(DataControlDialog dcd) {
    	int estimatedMB = 0;
        DataChoice dataChoice = dcd.getDataChoice();
        if (dataChoice != null) {
            Object[] selectedControls = dcd.getSelectedControls();
            for (int i = 0; i < selectedControls.length; i++) {
                ControlDescriptor cd = (ControlDescriptor) selectedControls[i];

                //Check if the data selection is ok
                if(!dcd.getDataSelectionWidget().okToCreateTheDisplay(cd.doesLevels())) {
                    continue;
                }

                DataSelection dataSelection = dcd.getDataSelectionWidget().createDataSelection(cd.doesLevels());
                                
                // Get the size in pixels of the requested image
                Object gotSize = dataSelection.getProperty("SIZE");
                if (gotSize == null) {
                	continue;
                }
                List<String> dims = StringUtil.split((String)gotSize, " ", false, false);
                int myLines = -1;
                int myElements = -1;
                if (dims.size() == 2) {
                	try {
                		myLines = Integer.parseInt(dims.get(0));
                		myElements = Integer.parseInt(dims.get(1));
                	}
                	catch (Exception e) { }
                }

                // Get the count of times requested
                int timeCount = 1;
                DataSelectionWidget dsw = dcd.getDataSelectionWidget();
                List times = dsw.getSelectedDateTimes();
            	List timesAll = dsw.getAllDateTimes();
                if (times != null && times.size() > 0) {
                	timeCount = times.size();
                }
                else if (timesAll != null && timesAll.size() > 0) {
                	timeCount = timesAll.size();
                }
                
                // Total number of pixels
                // Assumed lines x elements x times x 4bytes
                // Empirically seems to be taking *twice* that (64bit fields??)
                float totalPixels = (float)myLines * (float)myElements * (float)timeCount;
                float totalBytes = totalPixels * 4 * 2;
                estimatedMB += Math.round(totalBytes / 1024f / 1024f);
                                
		int additionalMB = 0;
		// Empirical tests show that textures are not affecting
		// required memory... comment out for now
		/*
                int textureDimensions = 2048;
                int mbPerTexture = Math.round((float)textureDimensions * (float)textureDimensions * 4 / 1024f / 1024f);
                int textureCount = (int)Math.ceil((float)myLines / 2048f) * (int)Math.ceil((float)myElements / 2048f);
                int additionalMB = textureCount * mbPerTexture * timeCount;
		*/
                
                estimatedMB += additionalMB;
            }
        }
        
        return estimatedMB;
    }

    /**
     * Represents a SavedBundle as a tree.
     */
    private class BundleTreeNode {

        private String name;

        private SavedBundle bundle;

        private List<BundleTreeNode> kids;

        /**
         * This constructor is used to build a node that is considered a
         * "parent." These nodes only have child nodes, no SavedBundles. This
         * was done so that distinguishing between bundles and bundle
         * subcategories would be easy.
         * 
         * @param name The name of this node. For a parent node with
         *        "Toolbar>cat" as the path, the name parameter would contain
         *        only "cat."
         */
        public BundleTreeNode(String name) {
            this(name, null);
        }

        /**
         * Nodes constructed using this constructor can only ever be child
         * nodes.
         * 
         * @param name The name of the SavedBundle.
         * @param bundle A reference to the SavedBundle.
         */
        public BundleTreeNode(String name, SavedBundle bundle) {
            this.name = name;
            this.bundle = bundle;
            kids = new LinkedList<BundleTreeNode>();
        }

        /**
         * @param child The node to be added to the current node.
         */
        public void addChild(BundleTreeNode child) {
            kids.add(child);
        }

        /**
         * @return Returns all child nodes of this node.
         */
        public List<BundleTreeNode> getChildren() {
            return kids;
        }

        /**
         * @return Return the SavedBundle associated with this node (if any).
         */
        public SavedBundle getBundle() {
            return bundle;
        }

        /**
         * @return The name of this node.
         */
        public String getName() {
            return name;
        }
    }

    /**
     * <p>
     * A type of <code>HttpFormEntry</code> that supports line wrapping for 
     * text area entries.
     * </p>
     * 
     * @see HttpFormEntry
     */
    private static class FormEntry extends HttpFormEntry {
        /** Initial contents of this entry. */
        private String value = "";

        /** Whether or not the JTextArea should wrap lines. */
        private boolean wrap = true;

        /** Entry type. Used to remain compatible with the IDV. */
        private int type = HttpFormEntry.TYPE_AREA;

        /** Number of rows in the JTextArea. */
        private int rows = 5;

        /** Number of columns in the JTextArea. */
        private int cols = 30;

        /** GUI representation of this entry. */
        private JTextArea component = new JTextArea(value, rows, cols);

        /**
         * Required to keep Java happy.
         */
        public FormEntry() {
            super(HttpFormEntry.TYPE_AREA, "form_data[description]", 
                "Description:");
        }

        /**
         * <p>
         * Using this constructor allows McIDAS-V to control whether or not a
         * HttpFormEntry performs line wrapping for JTextArea components.
         * </p>
         * 
         * @see HttpFormEntry#HttpFormEntry(int, String, String, String, int, int, boolean)
         */
        public FormEntry(boolean wrap, int type, String name, String label, String value, int rows, int cols, boolean required) {
            super(type, name, label, value, rows, cols, required);
            this.type = type;
            this.rows = rows;
            this.cols = cols;
            this.wrap = wrap;
        }

        /**
         * <p>
         * Overrides the IDV method so that the McIDAS-V support request form
         * will wrap lines in the "Description" field.
         * </p>
         * 
         * @see HttpFormEntry#addToGui(List)
         */
        @SuppressWarnings("unchecked")
        @Override public void addToGui(List guiComps) {
            if (type == HttpFormEntry.TYPE_AREA) {
                guiComps.add(GuiUtils.top(GuiUtils.rLabel(getLabel())));
                component.setLineWrap(wrap);
                component.setWrapStyleWord(wrap);
                JScrollPane sp = new JScrollPane(component);
                sp.setPreferredSize(new Dimension(500, 200));
                sp.setMinimumSize(new Dimension(500, 200));
                guiComps.add(sp);
            } else {
                super.addToGui(guiComps);
            }
        }

        /**
         * <p>
         * Since the IDV doesn't provide a getComponent for 
         * <code>addToGui</code>, we must make our <code>component</code> field
         * local to this class. 
         * Hijacks any value requests so that the local <code>component</code>
         * field is queried, not the IDV's.
         * </p>
         * 
         * @see HttpFormEntry#getValue()
         */
        @Override public String getValue() {
            if (type != HttpFormEntry.TYPE_AREA)
                return super.getValue();
            return component.getText();
        }

        /**
         * <p>
         * Hijacks any requests to set the <code>component</code> field's text.
         * </p>
         * 
         * @see HttpFormEntry#setValue(String)
         */
        @Override public void setValue(final String newValue) {
            if (type == HttpFormEntry.TYPE_AREA)
                component.setText(newValue);
            else
                super.setValue(newValue);
        }
    }

    /**
     * A {@code ToolbarStyle} is a representation of the way icons associated
     * with current toolbar actions should be displayed. This notion is so far
     * limited to the sizing of icons, but that may change.
     */
    public enum ToolbarStyle {
        /**
         * Represents the current toolbar actions as large icons. Currently,
         * {@literal "large"} is defined as {@code 32 x 32} pixels.
         */
        LARGE("Large Icons", "action.icons.large", 32),

        /**
         * Represents the current toolbar actions as medium icons. Currently,
         * {@literal "medium"} is defined as {@code 22 x 22} pixels.
         */
        MEDIUM("Medium Icons", "action.icons.medium", 22),

        /** 
         * Represents the current toolbar actions as small icons. Currently,
         * {@literal "small"} is defined as {@code 16 x 16} pixels. 
         */
        SMALL("Small Icons", "action.icons.small", 16);

        /** Label to use in the toolbar customization popup menu. */
        private final String label;

        /** Signals that the user selected a specific icon size. */
        private final String action;

        /** Icon dimensions. Each icon should be {@code size * size}. */
        private final int size;

        /**
         * {@link #size} in {@link String} form, merely for use with the IDV's
         * preference functionality.
         */
        private final String sizeAsString;

        /**
         * Initializes a toolbar style.
         * 
         * @param label Label used in the toolbar popup menu.
         * @param action Command that signals the user selected this toolbar 
         * style.
         * @param size Dimensions of the icons.
         * 
         * @throws NullPointerException if {@code label} or {@code action} are
         * null.
         * 
         * @throws IllegalArgumentException if {@code size} is not positive.
         */
        ToolbarStyle(final String label, final String action, final int size) {
            if (label == null)
                throw new NullPointerException("Label cannot be null");
            if (action == null)
                throw new NullPointerException("Action cannot be null");
            if (size <= 0)
                throw new IllegalArgumentException("Size must be a positive integer");

            this.label = label;
            this.action = action;
            this.size = size;
            this.sizeAsString = Integer.toString(size);
        }

        /**
         * Returns the label to use as a brief description of this style.
         */
        public String getLabel() {
            return label;
        }

        /**
         * Returns the action command associated with this style.
         */
        public String getAction() {
            return action;
        }

        /**
         * Returns the dimensions of icons used in this style.
         */
        public int getSize() {
            return size;
        }

        /**
         * Returns {@link #size} as a {@link String} to make cooperating with
         * the IDV preferences code easier.
         */
        public String getSizeAsString() {
            return sizeAsString;
        }

        /**
         * Returns a brief description of this ToolbarStyle. A typical 
         * example:<br/>
         * {@code [ToolbarStyle@1337: label="Large Icons", size=32]}
         * 
         * <p>Note that the format and details provided are subject to change.
         */
        public String toString() {
            return String.format("[ToolbarStyle@%x: label=%s, size=%d]", 
                hashCode(), label, size);
        }

        /**
         * Convenience method for build the toolbar customization popup menu.
         * 
         * @param manager {@link UIManager} that will be listening for action
         * commands.
         * 
         * @return Menu item that has {@code manager} listening for 
         * {@link #action}.
         */
        protected JMenuItem buildMenuItem(final UIManager manager) {
            JMenuItem item = new JRadioButtonMenuItem(label);
            item.setActionCommand(action);
            item.addActionListener(manager);
            return item;
        }
    }

    /**
     * Represents what McIDAS-V {@literal "knows"} about IDV actions.
     */
    protected enum ActionAttribute {

        /**
         * Unique identifier for an IDV action. Required attribute.
         * 
         * @see IdvUIManager#ATTR_ID
         */
        ID(ATTR_ID), 

        /**
         * Path to an icon for this action. Currently required. Note that 
         * McIDAS-V differs from the IDV in that actions must support different
         * icon sizes. This is implemented in McIDAS-V by simply having the value
         * of this path be a valid {@literal "format string"}, 
         * such as {@code image="/edu/wisc/ssec/mcidasv/resources/icons/toolbar/background-image%d.png"}
         * 
         * <p>The upshot is that this value <b>will not be a valid path in 
         * McIDAS-V</b>. Use either {@link IdvAction#getMenuIcon()} or 
         * {@link IdvAction#getIconForStyle}.
         * 
         * @see IdvUIManager#ATTR_IMAGE
         * @see IdvAction#getRawIconPath()
         * @see IdvAction#getMenuIcon()
         * @see IdvAction#getIconForStyle
         */
        ICON(ATTR_IMAGE), 

        /**
         * Brief description of a IDV action. Required attribute.
         * @see IdvUIManager#ATTR_DESCRIPTION
         */
        DESCRIPTION(ATTR_DESCRIPTION), 

        /**
         * Allows actions to be clustered into arbitrary groups. Currently 
         * optional; defaults to {@literal "General"}.
         * @see IdvUIManager#ATTR_GROUP
         */
        GROUP(ATTR_GROUP, "General"), 

        /**
         * Actual method call used to invoke a given IDV action. Required 
         * attribute.
         * @see IdvUIManager#ATTR_ACTION
         */
        ACTION(ATTR_ACTION);

        /**
         * A blank {@link String} if this is a required attribute, or a 
         * {@code String} value to use in case this attribute has not been 
         * specified by a given IDV action.
         */
        private final String defaultValue;

        /**
         * String representation of this attribute as used by the IDV.
         * @see #asIdvString()
         */
        private final String idvString;

        /** Whether or not this attribute is required. */
        private final boolean required;

        /**
         * Creates a constant that represents a required IDV action attribute.
         * 
         * @param idvString Corresponding IDV attribute {@link String}. Cannot be {@code null}.
         * 
         * @throws NullPointerException if {@code idvString} is {@code null}.
         */
        ActionAttribute(final String idvString) {
            Contract.notNull(idvString, "Cannot be associated with a null IDV action attribute String");

            this.idvString = idvString; 
            this.defaultValue = ""; 
            this.required = true; 
        }

        /**
         * Creates a constant that represents an optional IDV action attribute.
         * 
         * @param idvString Corresponding IDV attribute {@link String}. 
         * Cannot be {@code null}.
         * @param defValue Default value for actions that do not have this 
         * attribute. Cannot be {@code null} or an empty {@code String}.
         * 
         * @throws NullPointerException if either {@code idvString} or 
         * {@code defValue} is {@code null}.
         * @throws IllegalArgumentException if {@code defValue} is an empty 
         * {@code String}.
         * 
         */
        ActionAttribute(final String idvString, final String defValue) {
            Contract.notNull(idvString, "Cannot be associated with a null IDV action attribute String");
            Contract.notNull(defValue, "Optional action attribute \"%s\" requires a non-null default value", toString());

            Contract.checkArg(!defValue.equals(""), "Optional action attribute \"%s\" requires something more descriptive than an empty String", toString());

            this.idvString = idvString; 
            this.defaultValue = defValue; 
            this.required = (defaultValue.equals("")); 
        }

        /**
         * @return The {@link String} representation of this attribute, as is 
         * used by the IDV.
         * 
         * @see IdvUIManager#ATTR_ACTION
         * @see IdvUIManager#ATTR_DESCRIPTION
         * @see IdvUIManager#ATTR_GROUP
         * @see IdvUIManager#ATTR_ID
         * @see IdvUIManager#ATTR_IMAGE
         */
        public String asIdvString() { return idvString; }

        /**
         * @return {@literal "Default value"} for this attribute. 
         * Blank {@link String}s imply that the attribute is required (and 
         * thus lacks a true default value).
         */
        public String defaultValue() { return defaultValue; }

        /**
         * @return Whether or not this attribute is a required attribute for 
         * valid {@link IdvAction}s.
         */
        public boolean isRequired() { return required; }
    }

    /**
     * Represents the set of known {@link IdvAction}s in an idiom that can be
     * easily used by both the IDV and McIDAS-V.
     */
    // TODO(jon:101): use Sets instead of maps and whatnot
    // TODO(jon:103): create an invalid IdvAction
    public static final class IdvActions {

        /** Maps {@literal "id"} values to {@link IdvAction}s. */
        private final Map<String, IdvAction> idToAction = new ConcurrentHashMap<String, IdvAction>();

        /** Collects {@link IdvAction}s {@literal "under"} common group values. */
        // TODO(jon:102): this should probably become concurrency-friendly.
        private final Map<String, Set<IdvAction>> groupToActions = new LinkedHashMap<String, Set<IdvAction>>();

        /**
         * 
         * 
         * @param idv Reference to the IDV {@literal "god"} object. Cannot be {@code null}.
         * @param collectionId IDV resource collection that contains our actions. Cannot be {@code null}.
         * 
         * @throws NullPointerException if {@code idv} or {@code collectionId} 
         * is {@code null}. 
         */
        public IdvActions(final IntegratedDataViewer idv, final XmlIdvResource collectionId) {
            Contract.notNull(idv, "Cannot provide a null IDV reference");
            Contract.notNull(collectionId, "Cannot build actions from a null collection id");

            // i lub u xpath (but how much slower is this?)
            String query = "//action[@id and @image and @description and @action]";
            for (Element e : elements(idv, collectionId, query)) {
                IdvAction a = new IdvAction(e);
                String id = a.getAttribute(ActionAttribute.ID);
                idToAction.put(id, a);
                String group = a.getAttribute(ActionAttribute.GROUP);
                if (!groupToActions.containsKey(group)) {
                    groupToActions.put(group, new LinkedHashSet<IdvAction>());
                }
                Set<IdvAction> groupedIds = groupToActions.get(group);
                groupedIds.add(a);
            }
        }

        /**
         * Attempts to return the {@link IdvAction} associated with the given
         * {@code actionId}.
         * 
         * @param actionId Identifier to use in the search. Cannot be 
         * {@code null}.
         * 
         * @return Either the {@code IdvAction} that matches {@code actionId} 
         * or {@code null} if there was no match.
         * 
         * @throws NullPointerException if {@code actionId} is {@code null}.
         */
        // TODO(jon:103) here
        public IdvAction getAction(final String actionId) {
            Contract.notNull(actionId, "Null action identifiers are not allowed");
            return idToAction.get(actionId);
        }

        /**
         * Searches for the action associated with {@code actionId} and 
         * returns the value associated with the given {@link ActionAttribute}.
         * 
         * @param actionId Identifier to search for. Cannot be {@code null}.
         * @param attr Attribute whose value is desired. Cannot be {@code null}.
         * 
         * @return Either the desired attribute value of the desired action, 
         * or {@code null} if {@code actionId} has no associated action.
         * 
         * @throws NullPointerException if either {@code actionId} or 
         * {@code attr} is {@code null}.
         */
        // TODO(jon:103) here
        public String getAttributeForAction(final String actionId, final ActionAttribute attr) {
            Contract.notNull(actionId, "Null action identifiers are not allowed");
            Contract.notNull(attr, "Actions cannot have values associated with a null attribute");
            IdvAction action = idToAction.get(actionId);
            if (action == null) {
                return null;
            }
            return action.getAttribute(attr);
        }

        /**
         * Attempts to return the XML {@link Element} that {@literal "represents"} the
         * action associated with {@code actionId}.
         * 
         * @param actionId Identifier whose XML element is desired. Cannot be {@code null}.
         * 
         * @return Either the XML element associated with {@code actionId} or {@code null}.
         * 
         * @throws NullPointerException if {@code actionId} is {@code null}.
         * 
         * @see IdvAction#originalElement
         */
        // TODO(jon:103) here
        public Element getElementForAction(final String actionId) {
            Contract.notNull(actionId, "Cannot search for a null action identifier");
            IdvAction action = idToAction.get(actionId);
            if (action == null) {
                return null;
            }
            return action.getElement();
        }

        /**
         * Attempts to return an {@link Icon} for a given {@link ActionAttribute#ID} and
         * {@link ToolbarStyle}.
         * 
         * @param actionId ID of the action whose {@literal "styled"} icon is 
         * desired. Cannot be {@code null}.
         * @param style Desired {@code Icon} style. Cannot be {@code null}.
         * 
         * @return Either the {@code Icon} associated with {@code actionId} 
         * and {@code style}, or {@code null}.
         * 
         * @throws NullPointerException if either {@code actionId} or 
         * {@code style} is {@code null}.
         */
        // TODO(jon:103) here
        public Icon getStyledIconFor(final String actionId, final ToolbarStyle style) {
            Contract.notNull(actionId, "Cannot get an icon for a null action identifier");
            Contract.notNull(style, "Cannot get an icon for a null ToolbarStyle");
            IdvAction a = idToAction.get(actionId);
            if (a == null) {
                return null;
            }
            return a.getIconForStyle(style);
        }

        // TODO(jon:105): replace with something better
        public List<String> getAttributes(final ActionAttribute attr) {
            Contract.notNull(attr, "Actions cannot have null attributes");
            List<String> attributeList = arrList();
            for (Map.Entry<String, IdvAction> entry : idToAction.entrySet()) {
                attributeList.add(entry.getValue().getAttribute(attr));
            }
            return attributeList;
        }

        /**
         * @return List of all known {@code IdvAction}s.
         */
        public List<IdvAction> getAllActions() {
            return arrList(idToAction.values());
        }

        /**
         * @return List of all known action groupings.
         * 
         * @see ActionAttribute#GROUP
         * @see #getActionsForGroup(String)
         */
        public List<String> getAllGroups() {
            return arrList(groupToActions.keySet());
        }

        /**
         * Returns the {@link Set} of {@link IdvAction}s associated with the 
         * given {@code group}.
         * 
         * @param group Group whose associated actions you want. Cannot be 
         * {@code null}.
         * 
         * @return Collection of {@code IdvAction}s associated with 
         * {@code group}. A blank collection is returned if there are no actions
         * associated with {@code group}.
         * 
         * @throws NullPointerException if {@code group} is {@code null}.
         * 
         * @see ActionAttribute#GROUP
         * @see #getAllGroups()
         */
        public Set<IdvAction> getActionsForGroup(final String group) {
            Contract.notNull(group, "Actions cannot be associated with a null group");
            if (!groupToActions.containsKey(group)) {
                return Collections.emptySet();
            }
            return groupToActions.get(group);
        }

        /**
         * Returns a summary of the known IDV actions. Please note that this 
         * format is subject to change, and is not intended for serialization.
         * 
         * @return String that looks like 
         * {@code [IdvActions@HASHCODE: actions=...]}.
         */
        @Override public String toString() {
            return String.format("[IdvActions@%x: actions=%s]", hashCode(), idToAction);
        }
    }

    /**
     * Represents an individual IDV action. Should be fairly adaptable to
     * unforeseen changes from Unidata?
     */
    // TODO(jon:106): Implement equals/hashCode so that you can use these in Sets. The only relevant value should be the id, right?
    public static final class IdvAction {

        /** The XML {@link Element} that represents this IDV action. */
        private final Element originalElement;

        /** Mapping of (known) XML attributes to values for this individual action. */
        private final Map<ActionAttribute, String> attributes;

        /** 
         * Simple {@literal "cache"} for the different icons this action has
         * displayed. This is {@literal "lazy"}, so the cache does not contain
         * icons for {@link ToolbarStyle}s that haven't been used. 
         */
        private final Map<ToolbarStyle, Icon> iconCache = new ConcurrentHashMap<ToolbarStyle, Icon>();

        /**
         * Creates a representation of an IDV action using a given {@link Element}.
         * 
         * @param element XML representation of an IDV action. Cannot be {@code null}.
         * 
         * @throws NullPointerException if {@code element} is {@code null}.
         * @throws IllegalArgumentException if {@code element} is not a valid IDV action.
         * 
         * @see UIManager#isValidIdvAction(Element)
         */
        public IdvAction(final Element element) {
            Contract.notNull(element, "Cannot build an action from a null element");
            // TODO(jon:107): need a way to diagnose what's wrong with the action?
            Contract.checkArg(isValidIdvAction(element), "Action lacks required attributes");
            originalElement = element;
            attributes = actionElementToMap(element);
        }

        /**
         * @return Returns the {@literal "raw"} path to the icon associated 
         * with this action. Remember that this is actually a {@literal "format string"}
         * and should not be considered a valid path! 
         * 
         * @see #getIconForStyle
         */
        public String getRawIconPath() {
            return attributes.get(ActionAttribute.ICON);
        }

        /**
         * @return Returns the {@link Icon} associated with {@link ToolbarStyle#SMALL}.
         */
        public Icon getMenuIcon() {
            return getIconForStyle(ToolbarStyle.SMALL);
        }

        /**
         * Returns the {@link Icon} associated with this action and the given
         * {@link ToolbarStyle}.
         * 
         * @param style {@literal "Style"} of the {@code Icon} to be returned.
         * Cannot be {@code null}.
         * 
         * @return This action's {@code Icon} with {@code style} {@literal "applied."}
         * 
         * @see ActionAttribute#ICON
         * @see #iconCache
         */
        public Icon getIconForStyle(final ToolbarStyle style) {
            Contract.notNull(style, "Cannot build an icon for a null ToolbarStyle");

            if (!iconCache.containsKey(style)) {
                String styledPath = String.format(getRawIconPath(), style.getSize());
                URL tmp = getClass().getResource(styledPath);
                iconCache.put(style, new ImageIcon(Toolkit.getDefaultToolkit().getImage(tmp)));
            }
            return iconCache.get(style);
        }

        /**
         * @return Returns the identifier of this {@code IdvAction}.
         */
        public String getId() {
            return getAttribute(ActionAttribute.ID);
        }

        /**
         * Representation of this {@code IdvAction} as an {@literal "IDV action call"}.
         * 
         * @return String that is suitable to hand off to the IDV for execution. 
         */
        public String getCommand() {
            return "idv.handleAction('action:"+getAttribute(ActionAttribute.ID)+"')";
        }

        /**
         * Returns the value associated with a given {@link ActionAttribute} 
         * for this action.
         * 
         * @param attr ActionAttribute whose value you want. Cannot be {@code null}.
         * 
         * @return Value associated with {@code attr}.
         * 
         * @throws NullPointerException if {@code attr} is {@code null}.
         */
        public String getAttribute(final ActionAttribute attr) {
            Contract.notNull(attr, "No values can be associated with a null ActionAttribute");
            return attributes.get(attr);
        }

        /**
         * @return The XML {@link Element} used to create this {@code IdvAction}.
         */
        // TODO(jon:104): any way to copy this element? if so, this can become an immutable class!
        public Element getElement() {
            return originalElement;
        }

        /**
         * Returns a brief description of this action. Please note that the 
         * format is subject to change and is not intended for serialization.
         * 
         * @return String that looks like {@code [IdvAction@HASHCODE: attributes=...]}.
         */
        @Override public String toString() {
            return String.format("[IdvAction@%x: attributes=%s]", hashCode(), attributes);
        }
    }
}
