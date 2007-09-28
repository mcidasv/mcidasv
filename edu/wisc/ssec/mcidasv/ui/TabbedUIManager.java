package edu.wisc.ssec.mcidasv.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
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
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Element;

import ucar.unidata.idv.IdvConstants;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Msg;
import edu.wisc.ssec.mcidasv.Constants;

/**
 * A tabbed user interface for McIDAS-V.
 * 
 * <p>There is only a single tabbed window to contain all displays. When 
 * <tt>createNewWindow</tt> is called a new tab is added to the
 * display window. A tab may be 'ejected' from the main window and 
 * placed in a stand-alone window by double clicking on the tab. To
 * re-dock the display click the minimize window button and click the
 * close window button to close it.
 * </p>
 * 
 * @see <a href="http://java3d.j3d.org/tutorials/quick_fix/swing.html">
 *          Integrating Java 3D and Swing
 *      </a>
 *      
 * @author Bruce Flynn, SSEC
 * @version $Id$
 */
public class TabbedUIManager extends UIManager {
	
	/**
	 * Convenience class to associate a display with various helpful properties.
	 */
	private class DisplayProps implements Comparable {
		final Component contents;
		final ViewDescriptor desc;
		final List<ViewManager> managers;
		final int number; // 1-based display number, monotonic increasing
		String title = "";
		IdvWindow window = null; // may be null if in a tab
		DisplayProps(
			final int number, 
			final List<ViewManager> vms,
			final Component contents) {
			
			this.number = number;
			this.managers = vms;
			this.contents = contents;
			this.title = makeTabTitle(this);
			if (managers.size() > 1) {
				this.desc = new ViewDescriptor();
			} else {
				this.desc = managers.get(0).getViewDescriptor();
			}
		}
		/**
		 * Implement comparisons relative to display number.
		 */
		public int compareTo(Object obj) {
			DisplayProps that = (DisplayProps) obj;
			if (this.number == that.number) {
				return 0;
			} else if (this.number < that.number) {
				return -1;
			} 
			//else if (this.number > that.number) {
			return 1;
		}
		@Override
		public String toString() {
			return "DisplayProps" + 
				" number:"+number+" managers:"+ managers.size() + 
				" window:" + window.getFrame().isVisible();
		}
	}
	
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
				setActiveViewManager(disp);
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
				if (multiDisplays.containsKey(dn)) {
					multiDisplays.remove(dn);
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
	
	private class MainWindowListener extends WindowAdapter {
		@Override
		public void windowActivated(WindowEvent evt) {
			int idx = tabPane.getSelectedIndex();
			String desc = evt.getWindow().getName();
			DisplayProps disp = displays.get(desc);
			if (disp != null) {
				setActiveViewManager(disp);
			}
		}
		
		@Override
		public void windowClosing(WindowEvent evt) {
			mainWindow.show();
			// defer to the default idv quitter to show dialog or whatever
			getIdv().handleAction("jython:idv.quit();", null);
		}
	}
	
	private class NextDisplayAction extends AbstractAction {
		public NextDisplayAction() {
			super("Next Display");
		}
		public void actionPerformed(ActionEvent evt) {
			List<DisplayProps> disps = new ArrayList<DisplayProps>(displays.values());
			Collections.sort(disps);
			int curIdx = disps.indexOf(currentDisplay);
			if (curIdx == NOT_FOUND) {
				return;
			}
			int nextIdx = 0;
			if (curIdx != disps.size() - 1) {
				nextIdx = ++curIdx;
			}
			showDisplay(disps.get(nextIdx));
		}
	}
	
	private class PrevDisplayAction extends AbstractAction {
		public PrevDisplayAction() {
			super("Prev. Display");
		}
		public void actionPerformed(ActionEvent evt) {
			List<DisplayProps> disps = new ArrayList<DisplayProps>(displays.values());
			Collections.sort(disps);
			int curIdx = disps.indexOf(currentDisplay);
			if (curIdx == NOT_FOUND) {
				return;
			}
			int prevIdx = disps.size() - 1;
			if (curIdx != 0) {
				prevIdx = --curIdx;
			}
			showDisplay(disps.get(prevIdx));
		}
	}
	/**
	 * Take action to show a display.
	 */
	private class ShowDisplayAction extends AbstractAction {
		public void actionPerformed(ActionEvent evt) {
			String key = evt.getActionCommand().trim();
			DisplayProps disp = null;
			int idx = 0;
			try {
				idx = Integer.parseInt(key) - 1;
				disp = kbDisplayShortcuts.get(idx);
			} catch (Exception e) {
				logException("Could not show display", e);
			}
			if (disp != null) {
				showDisplay(disp);
			}
		}
	}
	
	private class TabChangeListener implements ChangeListener {
		public void stateChanged(ChangeEvent evt) {
			String desc = tabPane.getSelectedComponent().getName();
			DisplayProps disp = displays.get(desc);
			currentDisplay = disp;
			setActiveViewManager(currentDisplay);
		}
	}
	
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
	/** Property name for the initialization skin separator. */
	public static final String PROP_INITSKIN_SEP 
		= "idv.ui.initskins.propdelimiter";
	/** Property name for the initialization skins. */
	public static final String PROP_INITSKINS = "idv.ui.initskins";
	
	/** Action command for detroying a display. */
	private static final String DESTROY_DISPLAY_CMD = "DESTROY_DISPLAY_TAB";
	
	/** Action command for ejecting a display from a tab. */
	private static final String EJECT_DISPLAY_CMD = "EJECT_TAB";
	
	private static final String ICO_CLOSE = "/edu/wisc/ssec/mcidasv/resources/icons/stop-loads16.png";
	private static final String ICO_RENAME = "/edu/wisc/ssec/mcidasv/resources/icons/accessories-text-editor16.png";
	private static final String ICO_UNDOCK = "/edu/wisc/ssec/mcidasv/resources/icons/media-eject16.png";
	
	private static final String KEY_NEXT_DISPLAY = "RIGHT";
	private static final String KEY_PREV_DISPLAY = "LEFT";
	private static final String KEY_SHOW_DASHBOARD = "-";
	private static final String KEY_SHOW_MAINWINDOW = "0";
	
	private static final int NOT_FOUND = -1;
	
	/** Action command for renaming a display. */
	private static final String RENAME_DISPLAY_CMD = "RENAME_DISPLAY";

	/** Tooltip text for tabs. */
	private static final String TABS_TOOLTIP = "Right-click for options";

	/**
	 * Set the size of the main application window. If there is no 
	 * <tt>PROP_WINDOW_SIZEWIDTH</tt> or <tt>PROP_WINDOW_SIZEHEIGHT</tt>
	 * properties or their values are &lt;= 0 their values are set to
	 * the values of the corresponding <tt>DFLT_WINDOW_SIZE</tt> constant.
	 * 
	 * @param ui Manager that needs it's main window size set.
	 */
	private static void setSize(final TabbedUIManager ui) {
		// set the size from property or defualt
		int width = ui.getStateManager().getProperty(
			IdvConstants.PROP_WINDOW_SIZEWIDTH, 
			DFLT_WINDOW_SIZEWIDTH
		);
		if (width <= 0) {
			width = DFLT_WINDOW_SIZEWIDTH;
		}
		int height = ui.getStateManager().getProperty(
			IdvConstants.PROP_WINDOW_SIZEHEIGHT,
			DFLT_WINDOW_SIZEHEIGHT
		);
		if (height <= 0) {
			height = DFLT_WINDOW_SIZEHEIGHT;
		}
		ui.getFrame().setSize(width, height);
	}
	
	/** The number of the currently selected display. */
	private DisplayProps currentDisplay;
	
	/** Mapping of view descriptor names to their <tt>DisplayProps</tt>. */
	private Map<String, DisplayProps> displays;

	protected String displayKeyboardModifier;
	
	/**
	 * Displays assigned to shortcut keys in order of key assignment.
	 */
	private List<DisplayProps> kbDisplayShortcuts;
	private JPanel mainContents;
	
	/** The only display window. */
	private IdvWindow mainWindow;
	/**
	 * Mapping of <tt>ViewManager</tt> <tt>ViewDescriptors</tt> to a 
	 * <tt>DisplayProps</tt>. This provides the ability to locate the  
	 * <tt>DisplayProps</tt> to which a <tt>ViewManager</tt> belongs.
	 */
	private Map<String, String> multiDisplays = new Hashtable<String, String>();
	private NextDisplayAction nextDisplayAction;
	/** Number to assign to the next display added. */
	private int nextDisplayNum = 1;
	private JPopupMenu popup;
	private PrevDisplayAction prevDisplayAction;
	private ShowDisplayAction showWindowAction;
	
	/**
	 * Mapping of tab index to display view descriptor name. Provides constant 
	 * time access to a <tt>DisplayProps</tt> using a tab index.
	 */
//	private Map<Integer, String> tabDisplays = new Hashtable<Integer, String>();
	/** Main display container. */
	private JTabbedPane tabPane;

	private DisplayWindowListener windowActivationListener;
	/**
	 * The ctor. Just pass along the reference to the parent.
	 * 
	 * @param idv The idv
	 */
	public TabbedUIManager(IntegratedDataViewer idv) {
		super(idv);
	}
	
	/**
	 * Create a new display tab.
	 * 
	 * <p>An <tt>IdvWindow</tt> and tab is created for each new display. To 
	 * start with the views are added to the tab component. If the tab is
	 * 'ejected' the view components are removed from the tab, the tab is 
	 * removed and the components are added to the window and the window is
	 * shown.
	 * </p>
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
			String title, String skinPath, Element skinRoot) {
		
		IdvWindow window = super.createNewWindow(
			viewManagers,
			notifyCollab,
			makeWindowTitle(title),
			skinPath,
			skinRoot,
			false
		);
		
		Component contents = window.getContents();
		List<ViewManager> winViewMgrs = window.getViewManagers();
		
		// only the main window should be a main window, duhh!
		window.setIsAMainWindow(false);
		
		// view managers indicates it's a display window
		if (winViewMgrs.size() > 0) {
			
			window.addWindowListener(windowActivationListener);
			
			DisplayProps disp = new DisplayProps(
				nextDisplayNum++,
				winViewMgrs,
				contents
			);
			
			// setup multi-view mapping
			if (winViewMgrs.size() > 1) {
				for (ViewManager vm : winViewMgrs) {
					multiDisplays.put(
						vm.getViewDescriptor().getName(), 
						disp.desc.getName()
					);
				}
			}
			
			// set the component name to the view descriptor
			// so we can locate it later
			String descName = disp.desc.getName();
			window.getFrame().setName(descName);
			displays.put(descName, disp);
			
			// add key listeners shortcut keys
			if (kbDisplayShortcuts.size() <= 10) {
				kbDisplayShortcuts.add(disp);
			}
			disp.window = window;

			// make heavy use of component names for associating 
			// components with DisplayProps
			disp.window.getFrame().setName(descName);
			disp.contents.setName(descName);
			
			showDisplayInTab(disp);

			window.setTitle(makeWindowTitle(makeTabTitle(disp)));

		// not a display window, e.g., dashboard
		} else {
			String appTitle = getIdv().getStateManager().getTitle();
			String winTitle = window.getFrame().getTitle();
			if (!winTitle.contains(appTitle)) {
				window.getFrame().setTitle(makeWindowTitle(winTitle));
			}
			window.show();
		}
		
		// register keyboard shortcuts with all windows
		registerShortcutKeys(window.getContents());
		
		return window;
	}
	
	/**
     * Override to enable default bundle initialization. If the skin path is <tt>null</tt>
     * we assume the window to create is the main application window.  This should be ok because all
     * the other display types are created using skins.
     * @see ucar.unidata.idv.ui.IdvUIManager#createNewWindow(java.util.List, java.lang.String, java.lang.String)
     */
    @Override
	public IdvWindow createNewWindow(List viewManagers, String skinPath, String windowTitle) {
    	if (skinPath == null) {
    		makeApplicationWindow(windowTitle);
    		return mainWindow;
    	}
    	return super.createNewWindow(viewManagers, skinPath, windowTitle);
    }

    /** 
	 * Create the main application window and add a single display.
	 * 
	 * @see ucar.unidata.idv.ui.IdvUIManager#doMakeInitialGui()
	 */
	@Override
	public void doMakeInitialGui() {
		makeApplicationWindow(getStateManager().getTitle());
		createNewWindow(new ArrayList<ViewManager>(), true);
	}
	
	@Override
	public void init() {
		super.init();
		displayKeyboardModifier = getIdv().getProperty(Constants.PROP_DISPLAY_KB_MODIFIER, "control");
		popup = doMakeTabMenu();
		windowActivationListener = new DisplayWindowListener();
		kbDisplayShortcuts = new ArrayList<DisplayProps>();
		showWindowAction = new ShowDisplayAction();
		prevDisplayAction = new PrevDisplayAction();
		nextDisplayAction = new NextDisplayAction();
		displays = new Hashtable<String, DisplayProps>();
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
    		vm.destroy();
    	}
    }
    
    private DisplayProps getDisplayProps(ViewManager vm) {
    	String desc = vm.getViewDescriptor().getName();
    	if (displays.containsKey(desc)) {
    		return displays.get(desc);
    	}
    	String realDesc = multiDisplays.get(desc);
    	return displays.get(realDesc);
    }
    
	/**
	 * Make the main tabbed window. Tab component is initialized, listeners 
	 * added and added to window.
	 * @param title Window title to use.
	 */
	private void makeApplicationWindow(final String title) {
		
		mainWindow = new IdvWindow(
			title,
			getIdv(), 
			true
		);
		// setup the closing behavior
		mainWindow.setDefaultCloseOperation(
			WindowConstants.DO_NOTHING_ON_CLOSE
		);
		mainWindow.addWindowListener(new MainWindowListener());
		mainWindow.setSize(new Dimension(800, 600));
		
		JMenuBar menuBar = doMakeMenuBar();
		if (menuBar != null) {
			mainWindow.setJMenuBar(menuBar);
		}
		JComponent toolbar = getToolbarUI();
		tabPane = new HeavyTabbedPane();
		registerShortcutKeys(tabPane);
		
		// compensate for Java3D/Swing issue
		tabPane.addChangeListener(new TabChangeListener());
		
		// listener for showing popup
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
		
		setSize(this);
	}

	private String makeTabTitle(DisplayProps disp) {
    	return Constants.DISPLAY_NAME + " " + disp.number;
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
	 * Add all the show window keyboard shortcuts to the given components
	 * input and action maps.
	 * @param jcomp
	 */
	private void registerShortcutKeys(JComponent jcomp) {
		jcomp.getActionMap().put("show_disp", showWindowAction);
		jcomp.getActionMap().put("prev_disp", prevDisplayAction);
		jcomp.getActionMap().put("next_disp", nextDisplayAction);
		jcomp.getActionMap().put("show_dashboard", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				showDashboard();
			}
		});
		jcomp.getActionMap().put("show_main", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				if (getIdv().okToShowWindows()) {
					mainWindow.toFront();
				}
			}
		});
		for (int key = 1; key < 10; key++) {
			jcomp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(displayKeyboardModifier + " " + key),
				"show_disp"
			);
			
		}
		jcomp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(displayKeyboardModifier + " " + KEY_SHOW_MAINWINDOW),
			"show_main"
		);
		jcomp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(displayKeyboardModifier + " " + KEY_SHOW_DASHBOARD),
			"show_dashboard"
		);
		jcomp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(displayKeyboardModifier + " " + KEY_NEXT_DISPLAY),
			"next_disp"
		);
		jcomp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(displayKeyboardModifier + " " + KEY_PREV_DISPLAY),
			"prev_disp"
		);
	}
	
	/**
     * Remove a tab from that tab container.
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
	 * Show the display in a main window tab.
	 * @param disp properties for the new tab.
	 */
	private void showDisplayInTab(final DisplayProps disp) {
		
		disp.window.getContentPane().removeAll();
		tabPane.add(disp.title, disp.contents);
		
		String ttip = TABS_TOOLTIP;
		tabPane.setToolTipTextAt(tabPane.getTabCount() - 1, ttip);
		tabPane.setSelectedIndex(tabPane.getTabCount() - 1);
		currentDisplay = disp;
		
		mainWindow.show();
	}
	
	/**
	 * Show the window component for a display.  Window modifications
	 * are done on the event dispatch thread.
	 * @param idx Component index of the tab to extract.
	 */
	private void showDisplayInWindow(final int idx) {

		Component comp = tabPane.getComponentAt(idx);
		DisplayProps disp = displays.get(comp.getName());
		
		// must remove tab _before_ adding to window
		removeTab(idx);
		
		disp.window.getContentPane().removeAll();
		disp.window.getContentPane().add(disp.contents);
		disp.window.pack();
		disp.window.show();
		
		tabPane.setSelectedIndex(0);
		currentDisplay = disp;
	}
    
    /**
     * Create the <tt>JPopupMenu</tt> that will be displayed for a tab.
     * @return
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
//					String desc = tabDisplays.get(idx);
					Component comp = tabPane.getComponentAt(idx);
					DisplayProps props = displays.get(comp.getName());
					props.title = title;
					props.window.getFrame().setTitle(makeWindowTitle(title));
					tabPane.setTitleAt(idx, title);
					
				} else if (DESTROY_DISPLAY_CMD.equals(evt.getActionCommand())) {
					destroyDisplay(idx);
					
				} else if ("TEST".equals(evt.getActionCommand())) {
					JComponent comp = TabbedUIManager.this.getDisplaySelectorComponent();
			        JButton button = new JButton("Select " + Constants.PANEL_NAME);
			        final ComponentPopup cp = new ComponentPopup(button);
			        cp.add(comp, BorderLayout.CENTER);
			        cp.pack();
			        button.addActionListener(new ActionListener() {
			        	public void actionPerformed(ActionEvent evt) {
			        		cp.showPopup();
			        	}
			        });
			        
			        JFrame frame = new JFrame("Select a " + Constants.PANEL_NAME);
			        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			        frame.add(button);
			        frame.pack();
			        frame.setVisible(true);
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
		
//		// FIXME: remove before release
//		item = new JMenuItem("Test");
//		item.setActionCommand("TEST");
//		item.addActionListener(menuListener);
//		popup.add(item);
		
		popup.setBorder(new BevelBorder(BevelBorder.RAISED));
		
		Msg.translateTree(popup);
		return popup;
	}
    
    @Override
    protected void initializeDisplayMenu(JMenu displayMenu) {
    	super.initializeDisplayMenu(displayMenu);
    	displayMenu.addSeparator();
    	JMenuItem item = new JMenuItem(nextDisplayAction);
    	displayMenu.add(item);
    	item = new JMenuItem(prevDisplayAction);
    	displayMenu.add(item);
    	item = new JMenuItem("Select Display");
    	item.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent evt) {
    	    	showDisplaySelector();
    		}
    	});
    	displayMenu.add(item);
    	Msg.translateTree(displayMenu);
    }
    
    /**
     * Set the active view manager.
     * @param disp contains the view manager collection.
     */
    protected void setActiveViewManager(DisplayProps disp) {
    	setActiveViewManager(disp, 0);
    }
    
    protected void setActiveViewManager(DisplayProps disp, int idx) {
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
		String desc = disp.desc.getName();
		int tabIdx = tabPane.indexOfComponent(disp.contents);
		if (tabIdx != -1) {
			mainWindow.toFront();
			try {
				tabPane.setSelectedComponent(disp.contents);
				return;
			} catch (Exception e) {}
		}
		
		if (disp.window != null) {
			disp.window.toFront();
		}
		
		currentDisplay = disp;
	}

    protected void showDisplaySelector() {
    	JComponent comp = getDisplaySelectorComponent();
    	final JDialog dialog = new JDialog(mainWindow.getFrame(), "Select Display", true);
    	dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    	dialog.add(comp, BorderLayout.CENTER);
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
    					setActiveViewManager(disp, disp.managers.indexOf(vm));
    				}
    			});
    			
    			dialog.dispose();
    		}
    	});
    	JPanel buttonPanel = new JPanel();
    	buttonPanel.add(button);
    	dialog.add(buttonPanel, BorderLayout.AFTER_LAST_LINE);
    	dialog.pack();
    	dialog.setLocationRelativeTo(mainWindow.getFrame());
    	dialog.setVisible(true);
    }
}
