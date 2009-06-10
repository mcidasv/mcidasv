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

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.list;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import javax.swing.JEditorPane;
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
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.SavedBundle;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.IdvXmlUi;
import ucar.unidata.idv.ui.ViewPanel;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.ui.RovingProgress;
import ucar.unidata.ui.XmlUi;
import ucar.unidata.util.GuiUtils;
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
import edu.wisc.ssec.mcidasv.supportform.SimpleStateCollector;
import edu.wisc.ssec.mcidasv.supportform.SupportForm;
import edu.wisc.ssec.mcidasv.util.CompGroups;
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

    /** Id of the "New Display Tab" menu item for the file menu */
    public static final String MENU_NEWDISPLAY_TAB = "file.new.display.tab";

    /** The tag in the xml ui for creating the special example chooser */
    public static final String TAG_EXAMPLECHOOSER = "examplechooser";

    /**
     * Used to keep track of ViewManagers inside a bundle.
     * @see McIDASVXmlUi#createViewManager(Element)
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

    /** Stores all available actions. */
    private Map<String, String[]> cachedActions;

    /**
     * <p>The currently "displayed" actions. Keeping this List allows us to get 
     * away with only reading the XML files upon starting the application and 
     * only writing the XML files upon exiting the application. This will avoid
     * those redrawing delays.</p>
     */
    private List<String> cachedButtons;

    /** Map of skin ids to their skin resource index. */
    private Map<String, Integer> skinIds = readSkinIds();

    /** An easy way to figure out who is holding a given ViewManager. */
    private Hashtable<ViewManager, ComponentHolder> viewManagers = 
        new Hashtable<ViewManager, ComponentHolder>();

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
     * Hands off our IDV instantiation to IdvUiManager.
     *
     * @param idv The idv
     */
    public UIManager(IntegratedDataViewer idv) {
        super(idv);

        this.idv = idv;

        // cache the appropriate data for the toolbar. it'll make updates 
        // much snappier
        cachedActions = readActions();
        cachedButtons = readToolbar();
    }

    /**
     * Override the IDV method so that we hide component group button.
     */
    @Override public IdvWindow createNewWindow(List viewManagers,
        boolean notifyCollab, String title, String skinPath, Element skinRoot,
        boolean show, WindowInfo windowInfo) 
    {
        if (title != null && title.equals(Constants.DATASELECTOR_NAME))
            show = false;
        if (skinPath.indexOf("dashboard.xml") >= 0)
            show = false;

        IdvWindow w = super.createNewWindow(viewManagers, notifyCollab, title, 
            skinPath, skinRoot, show, windowInfo);

        String iconPath = idv.getProperty(Constants.PROP_APP_ICON, (String)null);
        ImageIcon icon = 
            GuiUtils.getImageIcon(iconPath, getClass(), true);
        w.setIconImage(icon.getImage());

        // need to catch the dashboard so that the showDashboard method has
        // something to do.
        if (w.getTitle().equals(Constants.DATASELECTOR_NAME)) {
            w.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            dashboard = w;
        } else {
            ((ComponentHolder)w.getComponentGroups().get(0)).setShowHeader(false);
        }

        initDisplayShortcuts(w);

        RovingProgress progress =
            (RovingProgress)w.getComponent(IdvUIManager.COMP_PROGRESSBAR);

        if (progress != null)
            progress.start();
        return w;
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
                holdersBefore.addAll(CompGroups.getAllComponentHolders());
                windowsBefore.addAll(CompGroups.getAllDisplayWindows());
            }

            for (WindowInfo info : (List<WindowInfo>)windows) {
                newViewManagers.removeAll(info.getViewManagers());
                makeBundledDisplays(info, okToMerge, mergeLayers, fromCollab);

                if (mergeLayers)
                    holdersBefore.addAll(CompGroups.getComponentHolders(info));
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
            IdvComponentGroup group = CompGroups.getComponentGroup(window);

            List<IdvComponentHolder> holders =
                CompGroups.getComponentHolders(group);

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
        List<ViewManager> newVms = CompGroups.getViewManagers(info);
        List<ViewManager> oldVms = CompGroups.getViewManagers(window);

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

    /* (non-Javadoc)
     * @see ucar.unidata.idv.ui.IdvUIManager#about()
     */
    public void about() {

        StateManager stateManager = (StateManager) getStateManager();

        JEditorPane editor = new JEditorPane();
        editor.setEditable(false);
        editor.setContentType("text/html");
        String html = stateManager.getMcIdasVersionAbout();
        editor.setText(html);
        editor.setBackground(new JPanel().getBackground());
        editor.addHyperlinkListener(idv);

        final JLabel iconLbl = new JLabel(
            GuiUtils.getImageIcon(idv.getProperty(PROP_SPLASHICON, ""))
        );
        iconLbl.setToolTipText("McIDAS-V homepage");
        iconLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        iconLbl.addMouseListener(new MouseAdapter() {
        	public void mouseClicked(MouseEvent evt) {
        		HyperlinkEvent link = null;
				try {
					link = new HyperlinkEvent(
						iconLbl,
						HyperlinkEvent.EventType.ACTIVATED,
						new URL(idv.getProperty(Constants.PROP_HOMEPAGE, ""))
					);
				} catch (MalformedURLException e) {}
        		idv.hyperlinkUpdate(link);
        	}
        });
        JPanel contents = GuiUtils.topCenter(
        	GuiUtils.inset(iconLbl, 5),
            GuiUtils.inset(editor, 5)
        );
        contents.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,
                Color.gray, Color.gray));

        final JDialog dialog = GuiUtils.createDialog(
        	getFrame(),
            "About " + getStateManager().getTitle(),
            false
        );
        dialog.add(contents);
        JButton close = McVGuiUtils.makePrettyButton("Close");
        close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				dialog.setVisible(false);
				dialog.dispose();
			}
        });
        JPanel bottom = new JPanel();
        bottom.add(close);
        dialog.add(GuiUtils.centerBottom(contents, bottom));
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(getFrame());

        Msg.translateTree(dialog);

        dialog.setVisible(true);
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

        for (IdvWindow w : CompGroups.getAllDisplayWindows()) {
            String title = w.getTitle();
            TwoFacedObject winTFO = new TwoFacedObject(title, w);
            DefaultMutableTreeNode winNode = new DefaultMutableTreeNode(winTFO);
            for (IdvComponentHolder h : CompGroups.getComponentHolders(w)) {
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
     * @param size
     * @param style
     * 
     * @return A JButton for the given action with an appropriate-sized icon.
     */
    private JButton buildToolbarButton(String action) {
        // grab the xml action attributes: 0 = icon path, 1 = tool tip,
        // 2 = action
        String[] data = cachedActions.get(action);

        if (data == null)
            return null;

        // handle missing mcv icons. the return of Amigo Mono!
        if (data[0] == null)
            data[0] =
                "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/range-bearing%d.png";

        // take advantage of sprintf-style functionality for creating the path
        // to the appropriate icon (given the user-specified icon dimensions).
        String str = String.format(data[0], currentToolbarStyle.getSize());
        URL tmp = getClass().getResource(str);

        JButton button = new JButton(new ImageIcon(tmp));

        // the IDV will take care of action handling! so nice!
        button.addActionListener(idv);
        button.setActionCommand(data[2]);
        button.addMouseListener(toolbarMenu);
        button.setToolTipText(data[1]);

        return button;
    }

    @Override public JPanel doMakeStatusBar(final IdvWindow window) {
//        System.err.println("caught doMakeStatusBar");
//        JPanel panel = super.doMakeStatusBar(window);
//        return panel;
        if (window == null)
            return new JPanel();

        JLabel msgLabel = new JLabel("                         ");
        LogUtil.addMessageLogger(msgLabel);

//        if (window != null) {
        window.setComponent(COMP_MESSAGELABEL, msgLabel);
//        }
//        if (window != null) {
        IdvXmlUi xmlUI = window.getXmlUI();
        if (xmlUI != null)
            xmlUI.addComponent(COMP_MESSAGELABEL, msgLabel);
//        }
        
        JLabel waitLabel = new JLabel(IdvWindow.getNormalIcon());
        waitLabel.addMouseListener(new ObjectListener(null) {
            public void mouseClicked(final MouseEvent e) {
                getIdv().clearWaitCursor();
            }
        });
        window.setComponent(COMP_WAITLABEL, waitLabel);

        RovingProgress progress = doMakeRovingProgressBar();
        window.setComponent(COMP_PROGRESSBAR, progress);

        MemoryMonitor mm = new MemoryMonitor(idv);
//        Border paddedBorder =
//            BorderFactory.createCompoundBorder(getStatusBorder(),
//                BorderFactory.createEmptyBorder(0, 2, 0, 2));
        mm.setBorder(getStatusBorder());
        progress.setBorder(getStatusBorder());
        waitLabel.setBorder(getStatusBorder());
        msgLabel.setBorder(getStatusBorder());

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
        if (toolbars == null)
            toolbars = new LinkedList<JToolBar>();

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
        if (toolbar.getComponentCount() > 0)
            toolbar.removeAll();

        // ensure that the toolbar popup menu appears
        if (toolbarMenu == null)
            toolbarMenu = constructToolbarMenu();

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
                if (tmp.getBundle() == null)
                    addBundleTree(toolbar, tmp);
                // otherwise it's just another button to add.
                else
                    addBundle(toolbar, tmp);
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
            getResourceManager().getXmlResources(
                IdvResourceManager.RSC_TOOLBAR);

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
     * @return The actions/buttons that live in the toolbar xml.
     */
    public List<String> readToolbar() {
        List<String> data = new ArrayList<String>();

        final Element root = getToolbarRoot();
        if (root == null)
            return null;

        final NodeList elements = XmlUtil.getElements(root);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element)elements.item(i);

            if (child.getTagName().equals(XmlUi.TAG_BUTTON))
                data.add(
                    XmlUtil.getAttribute(child, ATTR_ACTION, (String)null)
                        .substring(7));
            else
                data.add(null);
        }

        return data;
    }

    /**
     * <p>
     * Read the files that contain our available actions and build a nice hash
     * table that keeps them in memory. The format of the table matches the XML
     * pretty closely.
     * </p>
     * 
     * <p>
     * XML example:
     * <pre>
     * &lt;action id=&quot;show.dashboard&quot;
     *  image=&quot;/edu/wisc/ssec/mcidasv/resources/icons/show-dashboard16.png&quot;
     *  description=&quot;Show data explorer&quot;
     *  action=&quot;jython:idv.getIdvUIManager().showDashboard();&quot;/&gt;
     * </pre>
     * </p>
     * 
     * <p>
     * This would result in a hash table item with the key
     * &quot;show.dashboard" and an array of three Strings which contains the
     * "image", "description", and "action" attributes.
     * </p>
     * 
     * @return A hash table containing all available actions.
     */
    public Map<String, String[]> readActions() {
        Map<String, String[]> actionMap = new HashMap<String, String[]>();

        // grab the files that store our actions
        XmlResourceCollection xrc =
            getResourceManager().getXmlResources(
                IdvResourceManager.RSC_ACTIONS);

        // iterate through the set of files
        for (int i = 0; i < xrc.size(); i++) {
            Element root = xrc.getRoot(i);
            if (root == null)
                continue;

            // iterate through the set of actions in the current file.
            List<Element> kids = XmlUtil.findChildren(root, TAG_ACTION);
            for (Element node : kids) {
                String id = XmlUtil.getAttribute(node, ATTR_ID);

                String[] attributes = {
                     XmlUtil.getAttribute(node, ATTR_IMAGE, (String)null),
                     XmlUtil.getAttribute(node, ATTR_DESCRIPTION, (String)null),
                     XmlUtil.getAttribute(node, ATTR_ACTION, (String)null), 
                };

                // throw the action into the table and move on.
                actionMap.put(id, attributes);
            }
        }
        return actionMap;
    }

    /**
     * Returns the icon associated with {@code actionId}. Note that associating
     * the {@literal "missing icon"} icon with an action is allowable.
     * 
     * @param actionId Action ID whose associated icon is to be returned.
     * 
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

        String[] data = cachedActions.get(actionId);
        String icon = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/range-bearing%d.png";
        if (data != null && data[0] != null)
            icon = data[0];
        String str = String.format(icon, style.getSize());
        URL tmp = getClass().getResource(str);
        return new ImageIcon(tmp);
    }

    public Map<String, String[]> getCachedActions() {
        if (cachedActions == null)
            cachedActions = readActions();
        return cachedActions;
    }

    public List<String> getCachedButtons() {
        if (cachedButtons == null)
            cachedButtons = readToolbar();
        return cachedButtons;
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
        // handy reference to parent nodes
        Hashtable<String, BundleTreeNode> mapper =
            new Hashtable<String, BundleTreeNode>();

        final String TOOLBAR = "Toolbar";

        int bundleType = IdvPersistenceManager.BUNDLES_FAVORITES;

        final List<SavedBundle> bundles =
            getPersistenceManager().getBundles(bundleType);

        // iterate through all toolbar bundles
        for (SavedBundle bundle : bundles) {
            String categoryPath = "";
            String lastCategory = "";
            String grandParentPath = "";

            // build the "path" to the bundle. these paths basically look like
            // "Toolbar>category>subcategory>." so "category" is a category of
            // toolbar bundles and subcategory is a subcategory of that. The
            // IDV will build nice JPopupMenus with everything that appears in
            // "category," so McV needs to do the same thing. thus McV needs to
            // figure out the complete path to each toolbar bundle!
            List<String> categories = bundle.getCategories();
            if (categories != null && categories.size() > 0
                && categories.get(0).equals(TOOLBAR) == false)
                continue;

            for (String category : categories) {
                grandParentPath = categoryPath;
                categoryPath += category + ">";
                lastCategory = category;
            }

            // if the current path hasn't been encountered yet there is some
            // work to do.
            if (mapper.containsKey(categoryPath) == false) {
                // create the "parent" node for this bundle. note that no
                // SavedBundle is stored for parent nodes!
                BundleTreeNode newParent = new BundleTreeNode(lastCategory);

                // make sure that we store the fact that we've seen this path
                mapper.put(categoryPath, newParent);

                // also need to add newParent to grandparent's kids!
                if (lastCategory.equals(TOOLBAR) == false) {
                    BundleTreeNode grandParent = mapper.get(grandParentPath);
                    grandParent.addChild(newParent);
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
            for (BundleTreeNode kid : node.getChildren())
                buildPopupMenu(kid, test);

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
     * @see ucar.unidata.idv.ui.IdvUIManager#makeWindowsMenu(JMenu)
     */
    @Override public void makeWindowsMenu(JMenu windowMenu) {
        JMenuItem mi;
        boolean first = true;

        mi = new JMenuItem("Show Data Explorer");
        McVGuiUtils.setMenuImage(mi, Constants.ICON_DATAEXPLORER_SMALL);
        mi.addActionListener(this);
        mi.setActionCommand(ACT_SHOW_DASHBOARD);
        windowMenu.add(mi);

        makeTabNavigationMenu(windowMenu);
        
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
            if (title.equals(Constants.DATASELECTOR_NAME))
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

        if (CompGroups.getAllComponentHolders().size() <= 1)
            return;

        menu.addSeparator();

        menu.add(new JMenuItem(nextDisplayAction));
        menu.add(new JMenuItem(prevDisplayAction));
        menu.add(new JMenuItem(showDisplayAction));

        if (CompGroups.getAllComponentGroups().size() > 0)
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
                List<IdvComponentHolder> holders = CompGroups.getAllComponentHolders();
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
            McvComponentHolder prev = (McvComponentHolder)CompGroups.getBeforeActiveHolder();
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
            McvComponentHolder next = (McvComponentHolder)CompGroups.getAfterActiveHolder();
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
            if (!inWindow)
                skinFilter = "mcv.skin";

            // TODO: isn't there some static skin collection that I can use?
            final XmlResourceCollection skins =
                getResourceManager().getXmlResources(
                    IdvResourceManager.RSC_SKIN);

            Map<String, JMenu> menus = new Hashtable<String, JMenu>();
            for (int i = 0; i < skins.size(); i++) {
                final Element root = skins.getRoot(i);
                if (root == null)
                    continue;

                // filter out mcv or idv skins based on whether or not we're
                // interested in tabs or new windows.
                final String skinid = skins.getProperty("skinid", i);
                if (skinid != null && skinid.startsWith(skinFilter))
                    continue;

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
                List<McvComponentGroup> groups = window.getComponentGroups();
                for (final McvComponentGroup group : groups) {
                    JMenuItem mi = new JMenuItem(name);

                    mi.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent ae) {

                            if (!inWindow)
                                group.makeSkin(skinIndex);
                            else
                                createNewWindow(null, true,
                                    getStateManager().getTitle(), skins.get(
                                        skinIndex).toString(), skins.getRoot(
                                        skinIndex, false), inWindow, null);
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

    /**
     * Associates a given ViewManager with a given ComponentHolder.
     * 
     * @param vm The ViewManager that is inside <tt>holder</tt>.
     * @param holder The ComponentHolder that contains <tt>vm</tt>.
     */
    public void setViewManagerHolder(ViewManager vm, ComponentHolder holder) {
        viewManagers.put(vm, holder);
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
        return viewManagers.remove(vm);
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
            CompGroups.getComponentGroup(IdvWindow.getActiveWindow());

        if (skinIds.containsKey(skinId))
            group.makeSkin(skinIds.get(skinId));
    }

    /**
     * Method to do the work of showing the Data Explorer (nee Dashboard)
     */
    public void showDashboard(String tabName) {
        if (!initDone) {
            return;
        } else if (dashboard == null) {
            showWaitCursor();
            doMakeBasicWindows();
            showNormalCursor();
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
        sb.append("<b>" + name + "</b>: " + value + "<br>");
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
    protected void handleMenuDeSelected(String id, JMenu menu) {
    	super.handleMenuDeSelected(id, menu);
    }

    /**
     * Initialize the given menu before it is shown
     * @see ucar.unidata.idv.ui.IdvUIManager#historyMenuSelected(JMenu)
     */
    @Override
    protected void handleMenuSelected(String id, JMenu menu) {
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
        	super.handleMenuSelected(id, menu);
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
     *  This adds to the given menu a set of MenuItems, one for each saved viewmanager
     *  in the vmState list. If the ViewManager parameter vm is non-null
     *  then  the result of the selection will be to apply the selected ViewManager
     *  state to the given vm. Else a new tab will be created with a new ViewManager.
     *
     * @param menu The menu
     * @param vm The view manager
     */
    @Override
    public void makeViewStateMenu(JMenu menu, final ViewManager vm) {
        List<TwoFacedObject> vms = getVMManager().getVMState();
        if (vms.size() == 0) 
            menu.add(new JMenuItem(Msg.msg("No Saved Views")));

        final IdvUIManager uiManager = getIdv().getIdvUIManager();

        for (TwoFacedObject tfo : vms) {
            JMenuItem mi  = new JMenuItem(tfo.getLabel().toString());
            menu.add(mi);
            mi.addActionListener(new ObjectListener(tfo.getId()) {
                public void actionPerformed(ActionEvent ae) {
                    if (vm == null) {
                        ViewManager otherView = (ViewManager)theObject;
                    } else {
                        vm.initWith((ViewManager)theObject, true);
                    }
                }
            });
        }
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
     * 
     * Add icons to the IDV menubar
     * @return The menu bar we just created
     */
    public JMenuBar doMakeMenuBar() {
    	JMenuBar menuBar = super.doMakeMenuBar();
    	Hashtable menuMap = super.getMenuIds();
    	
    	// Add the icons to the file menu
    	JMenu fileMenu = (JMenu)menuMap.get("file");
    	if (fileMenu!=null) {
    		for (int i=0; i<fileMenu.getItemCount(); i++) {
    			JMenuItem menuItem = fileMenu.getItem(i);
    			if (menuItem==null) continue;
    			String menuText = menuItem.getText();
    			if (menuText.equals("New Display Window"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_NEWWINDOW_SMALL);
    			else if (menuText.equals("New Display Tab"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_NEWTAB_SMALL);
    			else if (menuText.equals("Open File..."))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_OPEN_SMALL);
    			else if (menuText.equals("Save Bundle..."))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_FAVORITESAVE_SMALL);
    			else if (menuText.equals("Save As..."))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_SAVEAS_SMALL);
    			else if (menuText.equals("Default Layout"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_DEFAULTLAYOUT_SMALL);
    			else if (menuText.equals("Exit"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_CANCEL_SMALL);
    		}
    	}

    	// Add the icons to the edit menu
    	JMenu editMenu = (JMenu)menuMap.get("edit");
    	if (editMenu!=null) {
    		for (int i=0; i<editMenu.getItemCount(); i++) {
    			JMenuItem menuItem = editMenu.getItem(i);
    			if (menuItem==null) continue;
    			String menuText = menuItem.getText();
    			if (menuText.equals("Remove")) {
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_REMOVE_SMALL);

    				// Remove submenu
    				if (menuItem instanceof JMenu) {
    					JMenu thisMenu = (JMenu)menuItem;
    					for (int j=0; j<thisMenu.getItemCount(); j++) {
    						JMenuItem thisItem = thisMenu.getItem(j);
    						if (thisItem==null) continue;
    						String thisText = thisItem.getText();
    						if (thisText.equals("All Layers and Data Sources"))
    							McVGuiUtils.setMenuImage(thisItem, Constants.ICON_REMOVELAYERSDATA_SMALL);
    						else if (thisText.equals("All Layers"))
    							McVGuiUtils.setMenuImage(thisItem, Constants.ICON_REMOVELAYERS_SMALL);
    						else if (thisText.equals("All Data Sources"))
    							McVGuiUtils.setMenuImage(thisItem, Constants.ICON_REMOVEDATA_SMALL);
    					}
    				}

    			}
    			else if (menuText.equals("Preferences..."))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_PREFERENCES_SMALL);
    		}
    	}
    	
    	// Add the icons to the tools menu
    	JMenu toolsMenu = (JMenu)menuMap.get("menu.tools");
    	if (toolsMenu!=null) {
    		for (int i=0; i<toolsMenu.getItemCount(); i++) {
    			JMenuItem menuItem = toolsMenu.getItem(i);
    			if (menuItem==null) continue;
    			String menuText = menuItem.getText();
    			if (menuText.equals("Local ADDE Data"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_LOCALDATA_SMALL);
    			else if (menuText.equals("Color Tables"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_COLORTABLE_SMALL);
    			else if (menuText.equals("Station Model Template"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_LAYOUTEDIT_SMALL);
    		}
    	}
    	
    	// Add the icons to the help menu
    	JMenu helpMenu = (JMenu)menuMap.get("help");
    	if (helpMenu!=null) {
    		for (int i=0; i<helpMenu.getItemCount(); i++) {
    			JMenuItem menuItem = helpMenu.getItem(i);
    			if (menuItem==null) continue;
    			String menuText = menuItem.getText();
    			if (menuText.equals("User's Guide"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_USERSGUIDE_SMALL);
    			if (menuText.equals("Getting Started"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_GETTINGSTARTED_SMALL);
    			else if (menuText.equals("Show Help Tips"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_HELPTIPS_SMALL);
    			else if (menuText.equals("Show Console"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_CONSOLE_SMALL);
    			else if (menuText.equals("Show Support Request Form"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_SUPPORT_SMALL);
    			else if (menuText.equals("Visit Online Forums"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_FORUMS_SMALL);
    			else if (menuText.equals("Check for new version"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_CHECKVERSION_SMALL);
    			else if (menuText.equals("Release Notes"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_NOTE_SMALL);
    			else if (menuText.equals("About McIDAS-V"))
    				McVGuiUtils.setMenuImage(menuItem, Constants.ICON_MCIDASV_SMALL);
    		}
    	}
    	
        return menuBar;
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
}