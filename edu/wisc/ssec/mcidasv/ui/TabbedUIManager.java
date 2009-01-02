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

package edu.wisc.ssec.mcidasv.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Element;

import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.IdvConstants;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlResourceCollection;
import edu.wisc.ssec.mcidasv.Constants;

/**
 * A tabbed user interface for McIDAS-V.
 * 
 * <p>There is a single main application window. Displays may be in the 
 * main window as a tab or in a display window. If a display is tabbed
 * it will always be in the main application window. There is no duplication
 * of the application menubar, it is only found in the main window.  A 
 * display that is created in a tab can be un-docked using the popup menu
 * for each individual tab. Once un-docked it may be re-docked by minimizing.
 * </p>
 * <p>Compatibility with bundles created in IDV cannot be guaranteed.
 * </p>
 * <p>
 * There are issues involved with using a <tt>JTabbedPane</tt>, which is a 
 * Swing light weight component, to display the heavy weight Java3D
 * {@link javax.media.j3d.Canvas3D} which is used by VisAD. These issues are
 * circumvented by using the <tt>HeavyTabbedPane</tt>. So far this has worked
 * well, but this issue may have to be re-visited if issues arise.
 * </p>
 * @see <a href="http://java3d.j3d.org/tutorials/quick_fix/swing.html">
 *          Integrating Java 3D and Swing
 *      </a>
 * @see HeavyTabbedPane
 *      
 * @author <a href="http://www.ssec.wisc.edu/cgi-bin/email_form.cgi?name=Flynn,%20Bruce">Bruce Flynn, SSEC</a>
 * @version $Id$
 */
public class TabbedUIManager extends UIManager implements Constants {
	
	/**
	 * Associates a display with various helpful properties.
	 */
	private class DisplayProps implements Comparable<DisplayProps> {
		final Component contents;
		final ViewDescriptor desc;
		boolean intab = false;
		final List<ViewManager> managers;
		final int number; // 1-based display number, monotonic increasing
		String title = "";
		final IdvWindow window;
		DisplayProps(IdvWindow window) {
			this.number = nextDisplayNum++;
			this.managers = window.getViewManagers();
			this.contents = window.getContents();
			this.window = window;
			this.window.addWindowListener(windowActivationListener);
			this.title = makeTabTitle(this);

			if (managers.size() > 1) {
				this.desc = new ViewDescriptor();
			} else {
				this.desc = managers.get(0).getViewDescriptor();
			}
			
			// set the component name to the view descriptor
			// so we can locate it later
			this.window.getFrame().setName(desc.getName());
			this.window.getFrame().setName(desc.getName());
			
			this.contents.setName(desc.getName());
			
			window.setTitle(makeWindowTitle(makeTabTitle(this)));
		}
		
		/**
		 * Implement comparisons relative to display number.
		 */
		public int compareTo(DisplayProps that) {
			if (this.number == that.number) {
				return 0;
			} else if (this.number < that.number) {
				return -1;
			} 
			//else if (this.number > that.number) {
			return 1;
		}
		
		public boolean isInTab() {
			return intab;
		}
		
		public void setInTab(boolean b) {
			intab = b;
		}
	}
	
	/**
	 * Handle window events for display windows.
	 */
	private class DisplayWindowListener extends WindowAdapter {
		@Override
		public void windowActivated(WindowEvent evt) {
			JFrame frame = (JFrame) evt.getWindow();
			String desc = frame.getName();
			DisplayProps disp = null;
			
			// we're a tab window
			if (displays.containsKey(desc)) {
				disp = displays.get(desc);
			}
			
			if (disp != null) {
				setActiveDisplay(disp);
			}
		}
		
		// unmanage/remove the view
		@Override
		public void windowClosing(WindowEvent evt) {
			JFrame frame = (JFrame)evt.getSource();
			String descName = frame.getName();
			DisplayProps disp = displays.remove(descName);
			for (ViewManager vm : disp.managers) {
				String dn = vm.getViewDescriptor().getName();
				if (vmToDisp.containsKey(dn)) {
					vmToDisp.remove(dn);
				}
			}
		}
		
		// put the window back in a tab
		@Override
		public void windowIconified(WindowEvent evt) {
			JFrame window = (JFrame) evt.getSource();
			// FIXME: can't seem to set the visible state of
			// the window when it's iconified so we have to
			// change the state first
			window.setExtendedState(Frame.NORMAL);
			window.setVisible(false);
			DisplayProps disp = displays.get(window.getName());
			showDisplayInTab(disp);
		}
	}
	
	/**
	 * Handle window events for the main display window.
	 */
	private class MainWindowListener extends WindowAdapter {
		@Override
		public void windowActivated(WindowEvent evt) {
			String desc = evt.getWindow().getName();
			DisplayProps disp = displays.get(desc);
			if (disp != null) {
				setActiveDisplay(disp);
			}
		}
		
		@Override
		public void windowClosing(WindowEvent evt) {
			// FIXME: need a better way to do this
			mainWindow.show();
			// defer to the default idv quitter to show dialog or whatever
			getIdv().handleAction("jython:idv.quit();", null);
		}
	}
	
	/**
	 * Make the next display the current display. Next means the first display 
	 * created after the currently selected. This will wrap if the current display
	 * is the last display created.
	 */
	private class NextDisplayAction extends AbstractAction {
		private static final String ACTION_NAME = "Next Display";
		private static final long serialVersionUID = 8006303227143024646L;
		public NextDisplayAction(KeyStroke acc) {
			super(ACTION_NAME);
			putValue(Action.ACCELERATOR_KEY, acc);
		}
		public void actionPerformed(ActionEvent evt) {
			List<DisplayProps> disps = new ArrayList<DisplayProps>(displays.values());
			Collections.sort(disps); // put displays in display number order
			int curIdx = disps.indexOf(currentDisplay);
			if (curIdx != NOT_FOUND) {
				int nextIdx = 0;
				if (curIdx != disps.size() - 1) {
					nextIdx = ++curIdx;
				}
				showDisplay(disps.get(nextIdx));
			}
		}
	}
	
	/**
	 * Make the previous display current. Previous means the last display created
	 * before the currently selected. This will wrap if the current display is
	 * the first display created.
	 */
	private class PrevDisplayAction extends AbstractAction {
		private static final long serialVersionUID = 8337365883743005438L;
		static final String ACTION_NAME = "Previous Display";
		public PrevDisplayAction(KeyStroke acc) {
			super(ACTION_NAME);
			putValue(Action.ACCELERATOR_KEY, acc);
		}
		public void actionPerformed(ActionEvent evt) {
			List<DisplayProps> disps = new ArrayList<DisplayProps>(displays.values());
			Collections.sort(disps); // put displays in display number order
			int curIdx = disps.indexOf(currentDisplay);
			if (curIdx != NOT_FOUND) {
				int prevIdx = disps.size() - 1;
				if (curIdx != 0) {
					prevIdx = --curIdx;
				}
				showDisplay(disps.get(prevIdx));
			}
		}
	}
	
	/**
	 * Show the select display widget.
	 */
	private class ShowDisplayAction extends AbstractAction {
		private static final String ACTION_NAME = "Select Display ...";
		private static final long serialVersionUID = 988512881445984887L;
		public ShowDisplayAction(KeyStroke acc) {
			super(ACTION_NAME);
			putValue(Action.ACCELERATOR_KEY, acc);
		}
		public void actionPerformed(ActionEvent evt) {
			// relies on the action command being the key
			// pressed for the display, i.e., 1-9.
			String cmd = evt.getActionCommand();
			if (cmd == null) {
				return;
			}
			
			if (ACTION_NAME.equals(cmd)) {
				showDisplaySelector();
				
			} else {
				DisplayProps disp = null;
				int idx = 0;
				try {
					idx = Integer.parseInt(cmd) - 1;
					disp = kbDisplayShortcuts.get(idx);
				} catch (Exception e) {
				}
				if (disp != null) {
					showDisplay(disp);
				}
			}
		}
	}
	
	/**
	 * Handle tab state change events.
	 */
	private class TabChangeListener implements ChangeListener {
		public void stateChanged(ChangeEvent evt) {
			String desc = tabPane.getSelectedComponent().getName();
			DisplayProps disp = displays.get(desc);
			currentDisplay = disp;
			setActiveDisplay(currentDisplay);
			tabPane.requestFocus();
		}
	}
	
	/**
	 * Handle pop-up events for tabs.
	 */
	private class TabPopupListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent evt) {
			checkPopup(evt);
		}
		@Override
		public void mousePressed(MouseEvent evt) {
			checkPopup(evt);
		}
		@Override
		public void mouseReleased(MouseEvent evt) {
			checkPopup(evt);
		}
		private void checkPopup(MouseEvent evt) {
			if (evt.isPopupTrigger()) {
				// can't close or eject last tab
				Component[] comps = popup.getComponents();
				for (Component comp : comps) {
					if (comp instanceof JMenuItem) {
						String cmd = ((JMenuItem) comp).getActionCommand();
						if ((DESTROY_DISPLAY_CMD.equals(cmd) 
								|| EJECT_DISPLAY_CMD.equals(cmd))
								&& tabPane.getTabCount() == 1) {
							comp.setEnabled(false);
						} else {
							comp.setEnabled(true);
						}
					}
				}
				popup.show(tabPane, evt.getX(), evt.getY());
			}
		}
	}
	
	/** Default screen height to use if not specified in the properties file. */
	public static final int DFLT_WINDOW_SIZEHEIGHT = 768;
	
	/** Default screen width to use if not specified in the properties file. */
	public static final int DFLT_WINDOW_SIZEWIDTH = 1024;
	
	/** Id of the "New Display Tab" menu item for the file menu */
    public static final String MENU_NEWDISPLAY_TAB = "file.new.display.tab";
	/** Property name for the initialization skin separator. */
	public static final String PROP_INITSKIN_SEP 
		= "idv.ui.initskins.propdelimiter";
	/** Property name for the initialization skins. */
	public static final String PROP_INITSKINS = "idv.ui.initskins";
	
	/** Action command for detroying a display. */
	private static final String DESTROY_DISPLAY_CMD = "DESTROY_DISPLAY_TAB";
	
	/** Action command for ejecting a display from a tab. */
	private static final String EJECT_DISPLAY_CMD = "EJECT_TAB";
	
	private static final int NOT_FOUND = -1;
	/** Action command for renaming a display. */
	private static final String RENAME_DISPLAY_CMD = "RENAME_DISPLAY";
	protected static final String ICO_CLOSE = "/edu/wisc/ssec/mcidasv/resources/icons/tabmenu/stop-loads16.png";
	
	protected static final String ICO_RENAME = "/edu/wisc/ssec/mcidasv/resources/icons/tabmenu/accessories-text-editor16.png";
	
	protected static final String ICO_UNDOCK = "/edu/wisc/ssec/mcidasv/resources/icons/tabmenu/media-eject16.png";

    /** Property name for the keyboard accelerator for the previous display. */
	protected static final String PROP_KB_DISPLAY_NEXT = "mcidasv.tabbedui.display.kbnext";
	
	/** Property name for the keyboard accelerator for the next display. */
	protected static final String PROP_KB_DISPLAY_PREV = "mcidasv.tabbedui.display.kbprev";

	/** Property name for the keyboard shorcut modifier. */
	protected static final String PROP_KB_MODIFIER = "mcidasv.tabbedui.display.kbmodifier";
	/** Property name for the keyboard accelerator for showing the selecte display widget. */
	protected static final String PROP_KB_SELECT_DISPLAY = "mcidasv.tabbedui.display.kbselect";
	/** Property name for the keyboard accelerator for showing the dashboard. */
	protected static final String PROP_KB_SHOW_DASHBOARD = "mcidasv.tabbedui.display.kbdashboard";
	/** Property name for the keyboard accelerator for showing the main window. */
	protected static final String PROP_KB_SHOW_MAIN = "mcidasv.tabbedui.display.kbmain";
	/** Tooltip text for tabs. */
	protected static final String TABS_TOOLTIP = "Right-click for options";
	static {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
	}

	/** The number of the currently selected display. */
	private DisplayProps currentDisplay;
	
	/** Mapping of view descriptor names to their <tt>DisplayProps</tt>. */
	private Map<String, DisplayProps> displays;
	
	/**
	 * Displays assigned to shortcut keys in order of key assignment.
	 */
	private List<DisplayProps> kbDisplayShortcuts;

	private JPanel mainContents;
	
	/** The only display window. */
	private IdvWindow mainWindow;
	/** Number to assign to the next display added. */
	private int nextDisplayNum = 1;
	
	private JPopupMenu popup;
	/** Main display container. */
	private JTabbedPane tabPane;
	
//	private McIdasViewPanel viewPanel;
	
	/**
	 * Mapping of <tt>ViewManager</tt> <tt>ViewDescriptors</tt> to a 
	 * <tt>DisplayProps</tt>. This provides the ability to locate the  
	 * <tt>DisplayProps</tt> to which a <tt>ViewManager</tt> belongs.
	 */
	private Map<String, String> vmToDisp = new Hashtable<String, String>();
	
	/** 
	 * Listens for display window activations so the current display can be set.
	 * @see #setActiveDisplay(edu.wisc.ssec.mcidasv.ui.TabbedUIManager.DisplayProps)
	 */
	private DisplayWindowListener windowActivationListener;
	/** 
	 * Sets the next display to be the current. Next means the display immediately 
	 * following the current display according to the order they were created.
	 */
	protected NextDisplayAction nextDisplayAction;
	/** 
	 * Sets the previous display to be the current. Previous means the display immediately 
	 * before the current display according to the order they were created.
	 */
	protected PrevDisplayAction prevDisplayAction;
	/** 
	 * For setting the current display or showing the display selection widget.
	 * @see #showDisplay(edu.wisc.ssec.mcidasv.ui.TabbedUIManager.DisplayProps)
	 * @see #showDisplaySelector()
	 */
	protected ShowDisplayAction showDisplayAction;
	/**
	 * Just pass along the reference to the parent.
	 * 
	 * @param idv The idv
	 */
	public TabbedUIManager(IntegratedDataViewer idv) {
		super(idv);
		displays = new Hashtable<String, DisplayProps>();
		initActions();
	}
	
//    /**
//     * Notify the data source holders of a new display control.
//     * Redo any menus
//     *
//     * @param control The new control
//     */
//    public void addDisplayControl(DisplayControl control) {
//    	ViewManager vm = control.getViewManager();
//    	if (getDisplayProps(vm) == null) {
//    		return;
//    	}
//        if (getStateManager().getShowControlsInTree()) {
//            DataChoice choice = control.getDataChoice();
//            for (int i = 0; i < getDataSourceHolders().size(); i++) {
//            	DataSourceHolder dsh = (DataSourceHolder) getDataSourceHolders().get(i);
//                dsh.addDisplayControl(control, choice);
//            }
//        }
//        if ((getViewPanel() != null) && !getIdv().getArgsManager().getIsOffScreen()) {
//        	DisplayProps disp = getDisplayProps(vm);
//        	String path = disp.title + ">" + ("Panel " + disp.managers.indexOf(vm));
//            getViewPanel().addDisplayControl(path, control);
//        }
//        displayControlsChanged();
//    }
	
    /**
     * Gets called by the display controls. We add the 'embed in tabs' menu items
     *
     * @param control The control
     * @param items List of menu items
     */
    @Override
	public void addViewMenuItems(DisplayControl control, List items) {
        if (getViewPanel() != null) {
            getViewPanel().addViewMenuItems(control, items);
        }
    }

	/**
     * Create a new IdvWindow for the given viewManager. Put the
     * contents of the viewManager into the window
     *
     * @param viewManagers The view managers
     * @param notifyCollab Should the {@link ucar.unidata.idv.collab.CollabManager}
     * be notified
     * @param show Do we show the window we just created
     *
     * @return The new window
     */
    @Override
	public IdvWindow createNewWindow(List viewManagers,
                                     boolean notifyCollab) {

        XmlResourceCollection skins = getResourceManager().getXmlResources(
                                          IdvResourceManager.RSC_SKIN);

        Element root     = null;
        String  path     = null;
        String  skinName = null;

        //First try to find the the default skin
        for (int i = 0; (i < skins.size()) && (root == null); i++) {
            if (Misc.equals(skins.getProperty("default", i), "true")) {
                root     = skins.getRoot(i);
                path     = skins.get(i).toString();
                skinName = getWindowTitleFromSkin(i);
            }
        }

        if (root == null) {
            for (int i = 0; (i < skins.size()) && (root == null); i++) {
                root     = skins.getRoot(i);
                path     = skins.get(i).toString();
                skinName = getWindowTitleFromSkin(i);
            }
        }
        return createNewWindow(viewManagers, notifyCollab, skinName, path, root, false, null);
    }
	
	/**
	 * Create a new display.
	 * 
	 * <p>An <tt>IdvWindow</tt> and tab are created for each new display. 
     * Initially the views are added to the tab component. If the tab is
	 * 'ejected' the view components are removed from the tab, the tab is 
	 * removed and the components are added to the window and the window is
	 * shown.
	 * </p>
	 * 
	 * @param inWindow If true create the display in a separate display window,
	 *  if false create the display in a tab.
	 * 
	 * @see ucar.unidata.idv.ui.IdvUIManager#createNewWindow(
	 *     java.util.List, 
	 *     boolean, 
	 *     java.lang.String, 
	 *     java.lang.String, 
	 *     org.w3c.dom.Element
	 * )
	 */
	@Override
	public IdvWindow createNewWindow(List viewManagers, boolean notifyCollab,
			String title, String skinPath, Element skinRoot, boolean inWindow, WindowInfo wi) {
		
//		// always make sure we have a main window
//		if (mainWindow == null) {
//			makeApplicationWindow(getStateManager().getTitle());
//		}

		// the window to use when actually displaying in a window
		IdvWindow window = super.createNewWindow(
			viewManagers,
			notifyCollab,
			makeWindowTitle(title),
			skinPath,
			skinRoot,
			false,
			wi
		);

		// register keyboard shortcuts with all windows to simulate globalness
		initDisplayShortcuts(window);
		
		// only the main window should be a main window which is different from 
		// the typical IDV behavior where every window with a display is a main
		// window
		window.setIsAMainWindow(false);
		
		// view managers indicates it's a display window
		if (window.getViewManagers().size() > 0) {

			// incoming list of ViewManagers can be null, so be sure to 
			// populate the list if that's the case.
			// TODO: determine why it can be null?
			if (viewManagers == null)
				viewManagers = window.getViewManagers();			
			
			DisplayProps disp = new DisplayProps(window);
			displays.put(disp.desc.getName(), disp);
			
			List<DisplayControl> controls = new ArrayList<DisplayControl>();

			for (ViewManager vm : (List<ViewManager>) viewManagers) {
				
				for (DisplayControl control : (List<DisplayControl>)vm.getControls()) {
					addDisplayControl(control);
				}
			}
			
			// setup multi-viewmanager to display mapping
			if (disp.managers.size() > 1) {
				for (ViewManager vm : disp.managers) {
					vmToDisp.put(
						vm.getViewDescriptor().getName(), 
						disp.desc.getName()
					);
				}
			}
			
			if (inWindow) {
				showDisplayInWindow(disp);
			} else {
				showDisplayInTab(disp);
			}

		// not a display window, e.g., dashboard
		} else {
			String appTitle = getIdv().getStateManager().getTitle();
			String winTitle = window.getFrame().getTitle();
			if (!winTitle.contains(appTitle)) {
				window.getFrame().setTitle(makeWindowTitle(winTitle));
			}
			if (getIdv().okToShowWindows()) {
				window.show();
			}
		}
		
		return window;
	}

    public IdvWindow createNewWindow(List viewManagers, boolean notifyCollab,
			String title, String skinPath, Element skinRoot, WindowInfo wi) {
		return createNewWindow(viewManagers, notifyCollab, title, skinPath, skinRoot, false, wi);
	}

    /* (non-Javadoc)
     * @see ucar.unidata.idv.ui.IdvUIManager#createNewWindow(java.util.List, java.lang.String, java.lang.String)
     */
    @Override
	public IdvWindow createNewWindow(List viewManagers, String skinPath, String windowTitle, WindowInfo wi) {
    	if (skinPath == null) {
    		makeApplicationWindow(windowTitle);
    		return mainWindow;
    	}
    	return super.createNewWindow(viewManagers, skinPath, windowTitle, wi);
    }
    
    @Override
	public void doBasicInitialization() {
		
		setDateFormat();
	}

	/**
     * Handle GUI initialization if -nogui argument was not given.
     * 
     * @see ucar.unidata.idv.ui.IdvUIManager#doMakeInitialGui()
     */
    @Override
    public void doMakeInitialGui() {
		makeApplicationWindow(getStateManager().getTitle());
		
		// moved from doMakeInitialGui because it wasn't getting called
		createNewWindow(new ArrayList<ViewManager>(), false);
    }
	
    /** 
	 * Have to re-configure the new display menu to have the capability
	 * to make a new display in a tab or window.
	 * @see ucar.unidata.idv.ui.IdvUIManager#doMakeMenuBar()
	 */
	@Override
	public JMenuBar doMakeMenuBar() {
        JMenuBar menuBar = super.doMakeMenuBar();
		
        JMenuItem menu = (JMenuItem) getMenuIds().get(MENU_NEWDISPLAY_TAB);
        if (menu != null) {
        	doMakeNewDisplayMenu(menu, false);
        }
        
        // re-configure add new display menu
        menu = (JMenuItem) getMenuIds().get(MENU_NEWDISPLAY);
        if (menu != null) {
        	menu.removeAll();
        	doMakeNewDisplayMenu(menu, true);
        }
        
		return menuBar;
	}
    
//    /**
//     * get the view panel, the one that holds the displays.
//     * If it doesn't exist then create it
//     *
//     * @return ViewPanel
//     */
//    public ViewPanel getViewPanel() {
//        if (viewPanel == null) {
//            viewPanel = new McIdasViewPanel(getIdv());
//            viewPanel.getContents();
//        }
//        return viewPanel;
//    }
    
    @Override
	public boolean getShowControlsInTab() {
    	return super.getShowControlsInTab();
    }
	
	/**
     * Get the list of IdvWindows that should be saved in a bundle. In this
     * <tt>UIManager</tt> a display has both a tab and a display associated
     * with it. All <u>displays</u> are persisted whether they're in a tab 
     * or in a window.
     *
     * @return List of windows to persist
     */
    @Override
	public List getWindowsToPersist() {
        List<WindowInfo> windows    = new ArrayList<WindowInfo>();
        for (DisplayProps disp : displays.values()) {
        	if (disp.window != null) {
        		windows.add(new WindowInfo(disp.window));
        	}
        }
        return windows;
    }
	
	/**
	 * Initialize the super then init our listeners and actions.
	 */
	@Override
	public void init() {
		super.init();
		popup = doMakeTabMenu();
		windowActivationListener = new DisplayWindowListener();
		initActions();
	}
	
	/* (non-Javadoc)
	 * @see ucar.unidata.idv.ui.IdvUIManager#initDone()
	 */
	@Override
	public void initDone() {
		super.initDone();
		
		// rely on the fact that dashboard cannot be done unless
		// initDone() is called in super
		showBasicWindow(true);
	}

    /**
     * Overridden to add display navigation items to the Window menu.
     * @see UIManager#makeWindowsMenu(JMenu)
     */
    @Override
    public void makeWindowsMenu(JMenu windowMenu) {
    	super.makeWindowsMenu(windowMenu);

    	if (displays.size() > 1) {
        	int winCnt = 0;
        	for (DisplayProps disp : displays.values()) {
        		if (!disp.isInTab()) {
        			winCnt++;
        		}
        	}
    		
    		// insert above the windows but below the rest of the items
    		int idx = windowMenu.getItemCount();
	    	if (winCnt == 0) {
	    		windowMenu.insertSeparator(idx++);
	    	} else {
	    		idx = windowMenu.getItemCount() - winCnt;
	    	}
	    	JMenuItem item = new JMenuItem(nextDisplayAction);
	    	windowMenu.add(item, idx);
	    	item = new JMenuItem(prevDisplayAction);
	    	windowMenu.add(item, ++idx);
	    	item = new JMenuItem(showDisplayAction);
	    	windowMenu.add(item, ++idx);
	    	if (winCnt > 0) {
	    		windowMenu.insertSeparator(++idx);
	    	}
	        Msg.translateTree(windowMenu);
    	}
    }
    
    @Override
    public boolean showBasicWindow(boolean createThemIfNotThere) {
    	if (!initDone) {
//    		System.err.println("init not done, no basic window");
    		return false;
    	}
    	return super.showBasicWindow(createThemIfNotThere);
    }
	
	@Override
	public void unpersistWindowInfo(List windows, List newViewManagers,
            boolean okToMerge, boolean fromCollab,
            boolean didRemoveAll) {
    	
//    	TODO: hack to make IDV bundles compatible
//    	Simply change the skin in the bundle to a compatible one
//
//    	if (windows != null) {
//	    	for (WindowInfo window : new ArrayList<WindowInfo>(windows)) {
//	    		if (window.getSkinPath() != null && window != null) {
//	    			// add more skins to translate here
//	    			// TODO: this should probably be read from a file
//		    		if (window.getSkinPath().equals("/ucar/unidata/idv/resources/skins/skin.xml")) {
//		    			window.setSkinPath("/edu/wisc/ssec/mcidasv/resources/tabbed/oneviewskin.xml");
//		    		}
//	    		}
//	    	}
//    	}
    	
    	super.unpersistWindowInfo(windows, newViewManagers, okToMerge, fromCollab, didRemoveAll);
    }

	@Override
	public void viewManagerActiveChanged(ViewManager vm) {
        if (getViewPanel() != null) {
            getViewPanel().viewManagerChanged(vm);
        }
    }
    
    /**
     * Be notified of the addition of a VM
     *
     * @param viewManager The VM
     */
    @Override
	public void viewManagerAdded(ViewManager viewManager) {
        if (getArgsManager().getIsOffScreen()) {
            return;
        }
        if (getViewPanel() != null) {
            getViewPanel().viewManagerAdded(viewManager);
        }
    }
    
    /**
     * Called when the ViewManager is changed. If we are showing legends in a
     * window then we update the tab label
     *
     * @param viewManager The ViewManager that was changed
     */
    @Override
	public void viewManagerChanged(ViewManager viewManager) {
        if (getArgsManager().getIsOffScreen()) {
            return;
        }
        if (getViewPanel() != null) {
            getViewPanel().viewManagerChanged(viewManager);
        }
    }
    
	/**
     * Called when the ViewManager is removed. If we are showing legends in a
     * window then we remove the tab
     *
     * @param viewManager The ViewManager that was destroyed
     */
    @Override
	public void viewManagerDestroyed(ViewManager viewManager) {
        if (getArgsManager().getIsOffScreen()) {
            return;
        }
        if (getViewPanel() != null) {
            getViewPanel().viewManagerDestroyed(viewManager);
        }
    }

	/**
     * Remove the tab, destroy the <tt>IdvWindow</tt> and all the associated 
     * <tt>ViewManagers</tt>
     * @param idx tab index
     */
    private void destroyDisplay(final int idx) {
    	String desc = removeTab(idx);
    	final DisplayProps disp = displays.remove(desc);
    	if (disp.window != null) {
	    	disp.window.dispose();
    	}
    	for (ViewManager vm : disp.managers) {
    		try { 
    			vm.destroy();
    		} catch (Exception e) {
    			// FIXME: after weird Java3D issues are resolved this should be removed
    			LogUtil.consoleMessage(LogUtil.getStackTrace(e));
    		}
    	}
    }

	/**
     * Get the <tt>DisplayProps</tt> which contains the specified
     * <tt>ViewManager</tt>. 
     * @param vm The <tt>ViewManager</tt> in question.
     * @return The <tt>ViewManager</tt>, null if not found.
     */
    private DisplayProps getDisplayProps(ViewManager vm) {
    	String desc = vm.getViewDescriptor().getName();
    	if (displays.containsKey(desc)) {
    		return displays.get(desc);
    	}
    	String realDesc = vmToDisp.get(desc);
    	if (realDesc != null && displays.containsKey(realDesc)) {
    		return displays.get(realDesc);
    	}
    	return null;
    }
	
	/**
     * Get the window title from the skin
     *
     * @param index  the skin index
     *
     * @return  the title
     */
    private String getWindowTitleFromSkin(int index) {
        XmlResourceCollection skins = getResourceManager().getXmlResources(
                                          IdvResourceManager.RSC_SKIN);
        List names = StringUtil.split(skins.getShortName(index), ">", true,
                                      true);

        String title = getStateManager().getTitle();

        if (names.size() > 0) {
            title = title + " - " + StringUtil.join(" - ", names);
        }
        return title;
    }
	
	/**
	 * Initialize my actions.
	 */
	private void initActions() {
		kbDisplayShortcuts = new ArrayList<DisplayProps>();
		
		String mod = getIdv().getProperty(PROP_KB_MODIFIER, "control");
		
		String acc = getIdv().getProperty(PROP_KB_SELECT_DISPLAY, "D");
		String stroke = mod + " " + acc;
		showDisplayAction = new ShowDisplayAction(KeyStroke.getKeyStroke(stroke));
		
		acc = getIdv().getProperty(PROP_KB_DISPLAY_PREV, "P");
		stroke = mod + " " + acc;
		prevDisplayAction = new PrevDisplayAction(KeyStroke.getKeyStroke(stroke));
		
		acc = getIdv().getProperty(PROP_KB_DISPLAY_NEXT, "N");
		stroke = mod + " " + acc;
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
		jcomp.getActionMap().put("show_main", new AbstractAction() {
			private static final long serialVersionUID = -4170757094665809808L;
			public void actionPerformed(ActionEvent evt) {
				if (getIdv().okToShowWindows()) {
					mainWindow.toFront();
				}
			}
		});

		String mod = getIdv().getProperty(PROP_KB_MODIFIER, "control");
		String acc = getIdv().getProperty(PROP_KB_SELECT_DISPLAY, "d");
		jcomp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(mod + " " + acc),
			"show_disp"
		);
		
		acc = getIdv().getProperty(PROP_KB_SHOW_MAIN, "0");
		jcomp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(mod + " " + acc),
			"show_main"
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
	 * Initialize the keyboard shortcuts for moving between display tabs.
	 * On Mac OSX the keyboard modifier will be the META(Apple) key and
	 * ALT on all others.
	 */
	private void initTabbedDisplayShortcuts() {
		tabPane.getActionMap().put("show_display", showDisplayAction);
		for (int key = 1; key <= 9; key++) {
			// macs don't play well with the ALT key
			if (OS_OSX.equals(System.getProperty("os.name"))) {
				tabPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
					KeyStroke.getKeyStroke("meta " + key),
					"show_display"
				);
				
			} else {
				tabPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
					KeyStroke.getKeyStroke("alt " + key),
					"show_display"
				);
			}
		}
	}
	
	/**
	 * Make the main tabbed window. Tab component is initialized, listeners 
	 * added and added to window.
	 * @param title Window title to use.
	 */
	private void makeApplicationWindow(final String title) {
		
		// only create the main window once
		if (mainWindow != null) {
			return;
		}

		mainWindow = new IdvWindow(
			title,
			getIdv(), 
			true
		);
		
		// let app decide what to do on close
		mainWindow.setDefaultCloseOperation(
			WindowConstants.DO_NOTHING_ON_CLOSE
		);
		
		mainWindow.addWindowListener(new MainWindowListener());
		
		JMenuBar menuBar = doMakeMenuBar();
		if (menuBar != null) {
			mainWindow.setJMenuBar(menuBar);
		}
		JComponent toolbar = getToolbarUI();
		tabPane = new HeavyTabbedPane();
		
		// add actions for tabbed display navigation
		initTabbedDisplayShortcuts();
		
		tabPane.addChangeListener(new TabChangeListener());
		tabPane.addMouseListener(new TabPopupListener());
		
        ImageIcon icon = GuiUtils.getImageIcon(
    		getIdv().getProperty(
    			IdvConstants.PROP_SPLASHICON, 
    			IdvConstants.NULL_STRING
    		)
        );
        if (icon != null) {
            mainWindow.setIconImage(icon.getImage());
        }
		
		JPanel statusBar = doMakeStatusBar(mainWindow);
		mainContents = new JPanel();
		mainContents.setLayout(new BorderLayout());
		mainContents.add(tabPane, BorderLayout.CENTER);
		JComponent contents = GuiUtils.topCenterBottom(
			toolbar, 
			mainContents, 
			statusBar
		);
		mainWindow.setContents(contents);
		setSize(mainWindow);
	}
	
	/**
	 * Create a window title suitable for an application window.
	 * @param title window title
	 * @return Application title plus the window title.
	 */
	private String makeWindowTitle(final String title) {
		return makeTitle(getIdv().getStateManager().getTitle(), title);
	}
	
	/**
     * Remove a tab from the tab container.
     * @param idx tab index
     * @return descriptor of the <tt>DisplayProps</tt> associated with the tab
     * 	idx.
     */
    private String removeTab(final int idx) {
    	Component comp = tabPane.getComponentAt(idx);
		tabPane.removeTabAt(idx);
		
		String desc = tabPane.getSelectedComponent().getName();
		showDisplay(displays.get(desc));
		
		return comp.getName();
    }
	
    /**
	 * Set the size of the window. If there is no 
	 * <tt>PROP_WINDOW_SIZEWIDTH</tt> or <tt>PROP_WINDOW_SIZEHEIGHT</tt>
	 * properties or their values are &lt;= 0 their values are set to
	 * the values of the corresponding <tt>DFLT_WINDOW_SIZE</tt> constant.
	 * 
	 * @param window For whom to set the size.
	 */
	private  void setSize(IdvWindow window) {
		// set the size from property or defualt
		int width = getStateManager().getProperty(
			IdvConstants.PROP_WINDOW_SIZEWIDTH, 
			DFLT_WINDOW_SIZEWIDTH
		);
		if (width <= 0) {
			width = DFLT_WINDOW_SIZEWIDTH;
		}
		int height = getStateManager().getProperty(
			IdvConstants.PROP_WINDOW_SIZEHEIGHT,
			DFLT_WINDOW_SIZEHEIGHT
		);
		if (height <= 0) {
			height = DFLT_WINDOW_SIZEHEIGHT;
		}
		
		window.getFrame().setSize(width, height);
	}

    /**
	 * Show the display in a main window tab.
	 * @param disp properties for the new tab.
	 */
	private void showDisplayInTab(final DisplayProps disp) {
		
		// order of operations matters here
		disp.window.getContentPane().removeAll();
		tabPane.add(disp.title, disp.contents);
		
		String ttip = TABS_TOOLTIP;
		tabPane.setToolTipTextAt(tabPane.getTabCount() - 1, ttip);
		tabPane.setSelectedIndex(tabPane.getTabCount() - 1);
		currentDisplay = disp;
		
		disp.setInTab(true);
		if (kbDisplayShortcuts.size() < 9) {
			kbDisplayShortcuts.add(disp);
		}
	}
    
    /**
	 * Show the window component for a display.
	 * @param disp
	 */
	private void showDisplayInWindow(DisplayProps disp) {
		disp.window.getContentPane().removeAll();
		disp.window.getContentPane().add(disp.contents);
		setSize(disp.window);
		
		if (getIdv().okToShowWindows()) {
			disp.window.show();
		}
		
		disp.setInTab(false);
		kbDisplayShortcuts.remove(disp);
		
		currentDisplay = disp;
	}
    
    /**
	 * Show the window component for a display that is currently in a tab.
	 * @param idx Component index of the tab to extract.
	 */
	private void showDisplayInWindow(final int idx) {

		Component comp = tabPane.getComponentAt(idx);
		DisplayProps disp = displays.get(comp.getName());
		
		// must remove tab BEFORE adding to window
		removeTab(idx);
		
		showDisplayInWindow(disp);

	}
    
    /**
	 * Populate a "new display" menu from the available skin list.
	 * @param newDisplayMenu menu to populate.
	 * @param inWindow Is the skinned display to be created in a window?
	 * @see ucar.unidata.idv.IdvResourceManager#RSC_SKIN
	 * @return Menu item populated with display skins
	 */
	protected JMenuItem doMakeNewDisplayMenu(JMenuItem newDisplayMenu, final boolean inWindow) {
        if (newDisplayMenu != null) {
            final XmlResourceCollection skins =
                getResourceManager().getXmlResources(
                    IdvResourceManager.RSC_SKIN);

            Map<String, JMenu> menus = new Hashtable<String, JMenu>();
            for (int i = 0; i < skins.size(); i++) {
                final Element root = skins.getRoot(i);
                if (root == null) {
                    continue;
                }
                final int skinIndex = i;
                List<String> names = StringUtil.split(skins.getShortName(i), ">", true, true);
                JMenuItem theMenu = newDisplayMenu;
                String    path    = "";
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
                JMenuItem    mi   = new JMenuItem(name);
                theMenu.add(mi);
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        createNewWindow(null, true,
                                        getStateManager().getTitle(),
                                        skins.get(skinIndex).toString(),
                                        skins.getRoot(skinIndex, false),
                                        inWindow,
                                        null);
                    }
                });
            }
        }
        return newDisplayMenu;
	}

    /**
     * Create the <tt>JPopupMenu</tt> that will be displayed for a tab.
     * @return Menu initialized with tab options
     */
    protected JPopupMenu doMakeTabMenu() {
		ActionListener menuListener = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				final int idx = tabPane.getSelectedIndex();
				if (EJECT_DISPLAY_CMD.equals(evt.getActionCommand())) {
					if (idx >= 0 && tabPane.getTabCount() > 1) {
						showDisplayInWindow(idx);
					}
				} else if (RENAME_DISPLAY_CMD.equals(evt.getActionCommand())) {
					final String title = JOptionPane.showInputDialog(
						mainWindow.getFrame(),
						"Enter new name",
						makeWindowTitle("Rename Tab"),
						JOptionPane.PLAIN_MESSAGE
					);
					if (title == null) {
						return;
					}
					Component comp = tabPane.getComponentAt(idx);
					DisplayProps props = displays.get(comp.getName());
					props.title = title;
					props.window.getFrame().setTitle(makeWindowTitle(title));
					tabPane.setTitleAt(idx, title);
					
				} else if (DESTROY_DISPLAY_CMD.equals(evt.getActionCommand())) {
					destroyDisplay(idx);
					
				}
			}
		};
		
		final JPopupMenu popup = new JPopupMenu();
		JMenuItem item;
		
		URL img = getClass().getResource(ICO_UNDOCK);
		item = new JMenuItem("Undock", new ImageIcon(img));
		item.setActionCommand(EJECT_DISPLAY_CMD);
		item.addActionListener(menuListener);
		popup.add(item);
		
		img = getClass().getResource(ICO_RENAME);
		item = new JMenuItem("Rename", new ImageIcon(img));
		item.setActionCommand(RENAME_DISPLAY_CMD);
		item.addActionListener(menuListener);
		popup.add(item);
	
		popup.addSeparator();
		
		img = getClass().getResource(ICO_CLOSE);
		item = new JMenuItem("Close", new ImageIcon(img));
		item.setActionCommand(DESTROY_DISPLAY_CMD);
		item.addActionListener(menuListener);
		popup.add(item);
		
		popup.setBorder(new BevelBorder(BevelBorder.RAISED));
		
		Msg.translateTree(popup);
		return popup;
	}
    
    protected String makeTabTitle(DisplayProps disp) {
    	return DISPLAY_NAME + " " + disp.number;
    }
    
    /**
     * Set the active display. If the display contains more than one
     * view manager make the first one active.
     * @param disp display to activate
     */
    protected void setActiveDisplay(DisplayProps disp) {
    	setActiveDisplay(disp, 0);
    }
    
    /**
     * Set the active display making the view manager at the give index
     * current.
     * @param disp display to activate
     * @param idx Index of the view manager to make active.
     */
    protected void setActiveDisplay(DisplayProps disp, int idx) {
		if (disp.managers.size() > 0) {
			getVMManager().setLastActiveViewManager(disp.managers.get(idx));
		}
    }
    
    /**
     * Bring the display for <tt>disp</tt> to the front whether it's
     * in a tab or window.
     * @param disp
     */
    protected void showDisplay(final DisplayProps disp) {
		int tabIdx = tabPane.indexOfComponent(disp.contents);
		
		// are we in a tab?
		if (tabIdx != NOT_FOUND) {
			mainWindow.toFront();
			try {
				tabPane.setSelectedComponent(disp.contents);
				tabPane.requestFocus();
			} catch (Exception e) {}
			
		// not in tab, must be in window
		} else if (disp.window != null) {
			disp.window.toFront();
		}
		
		currentDisplay = disp;
	}
    
    /**
     * Show the display selector widget.
     */
    protected void showDisplaySelector() {
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
    			final DisplayProps disp = getDisplayProps(vm);
    			if (disp != null) {
    				showDisplay(disp);
    			}
    			// have to do this on the event dispatch thread so we make
    			// sure it happens after showDisplay
    			SwingUtilities.invokeLater(new Runnable() {
    				public void run() {
    					setActiveDisplay(disp, disp.managers.indexOf(vm));
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
}
