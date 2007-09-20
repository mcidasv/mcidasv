package edu.wisc.ssec.mcidasv.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
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
	
	/** Default screen height to use if not specified in the properties file. */
	public static final int DFLT_WINDOW_SIZEHEIGHT = 768;
	/** Default screen width to use if not specified in the properties file. */
	public static final int DFLT_WINDOW_SIZEWIDTH = 1024;
	/** Property name for the initialization skin separator. */
	public static final String PROP_INITSKIN_SEP 
		= "idv.ui.initskins.propdelimiter";
	
	/** Property name for the initialization skins. */
	public static final String PROP_INITSKINS = "idv.ui.initskins";
	/** Skin property name indicating the tab title. */
	public static final String PROP_TAB_TITLE = "mcidasv.tab.title";
	
	/** Action command for detroying a display. */
	private static final String DESTROY_DISPLAY_CMD = "DESTROY_DISPLAY_TAB";
	
	/** Action command for ejecting a display from a tab. */
	private static final String EJECT_DISPLAY_CMD = "EJECT_TAB";
	private static final String ICO_CLOSE = "/edu/wisc/ssec/mcidasv/resources/icons/stop-loads16.png";
	private static final String ICO_UNDOCK = "/edu/wisc/ssec/mcidasv/resources/icons/media-eject16.png";
	
	private static final String ICO_RENAME = "/edu/wisc/ssec/mcidasv/resources/icons/accessories-text-editor16.png";
	/** Action command for renaming a display. */
	private static final String RENAME_DISPLAY_CMD = "RENAME_DISPLAY";
	/** Prepended to new tabs. */
	private static final String TAB_TITLE_PFX = "Tab";
	
	private static final String TABS_TOOLTIP = "Right-click for options";
	/**
	 * Make an empty <tt>JPanel</tt> with the component name set to the name
	 * of the view descriptor. 
	 * @param descName
	 * @return the named panel
	 */
	private static JPanel makeBlankTab(final String descName) {
		JPanel panel = new JPanel();
		panel.setName(descName);
		return panel;
	}

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
	
	/**
	 * Convenience class to associate a display with various helpful properties.
	 */
	class DisplayProps {
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
		@Override
		public String toString() {
			return "DisplayProps" + 
				" number:"+number+" managers:"+ managers.size() + 
				" window:" + window.getFrame().isVisible();
		}
	}

	/**
	 * Handles changes to the main tab component.
	 */
	class TabChangeListener implements ChangeListener {
		public void stateChanged(ChangeEvent evt) {
			if (tabPane.getTabCount() == 1) {
				// FIXME: don't display tabs if there's only 1 
			} 
			
			int idx = tabPane.getSelectedIndex();
			if (tabPane.getTabCount() > 0) {
				if (idx == -1) {
					showTab(0);
				} else {
					showTab(idx);
				}
			}
		}
	}

	class TabWindowListener extends WindowAdapter {
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
			windowToTab(window.getName());
		}
	}
	
	class MainWindowListener extends WindowAdapter {
		@Override
		public void windowActivated(WindowEvent evt) {
			int idx = tabPane.getSelectedIndex();
			DisplayProps disp = displays.get(tabDisplays.get(idx));
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

	/** Mapping of view descriptor names to their <tt>DisplayProps</tt>. */
	private Map<String, DisplayProps> displays 
	= new Hashtable<String, DisplayProps>();
	/** The only display window. */
	private IdvWindow mainWindow;
	
	/**
	 * Mapping of <tt>ViewManager</tt> <tt>ViewDescriptors</tt> to a 
	 * <tt>DisplayProps</tt>. This provides the ability to locate the  
	 * <tt>DisplayProps</tt> to which a <tt>ViewManager</tt> belongs.
	 */
	private Map<String, String> multiDisplays = new Hashtable<String, String>();
	/** Number to assign to the next display added. */
	private int nextDisplayNum = 1;
	private JPopupMenu popup;
	/**
	 * Mapping of tab index to display view descriptor name. Provides constant 
	 * time access to a <tt>DisplayProps</tt> using a tab index.
	 */
	private Map<Integer, String> tabDisplays = new Hashtable<Integer, String>();
	
	/** Main display container. */
	private JTabbedPane tabPane;

	private TabWindowListener windowActivationListener;
	
	/**
	 * The ctor. Just pass along the reference to the parent.
	 * 
	 * @param idv The idv
	 */
	public TabbedUIManager(IntegratedDataViewer idv) {
		super(idv);
	}
	
	@Override
	public void init() {
		super.init();
		popup = doMakeTabMenu();
		windowActivationListener = new TabWindowListener();
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
			
			// once the window is registered we can remove the content
			disp.window = window;

			addDisplayTab(disp);

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
		return window;
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
    
	/**
	 * Add a display tab.
	 * @param disp properties for the new tab.
	 */
	private void addDisplayTab(final DisplayProps disp) {
		disp.window.getContentPane().removeAll();
		tabDisplays.put(tabPane.getTabCount(), disp.desc.getName());
		tabPane.add(
			disp.title,
			makeBlankTab(disp.desc.getName())
		);
		
		mainWindow.show();
		
		tabPane.setToolTipTextAt(tabPane.getTabCount() - 1, TABS_TOOLTIP);
		tabPane.setSelectedIndex(tabPane.getTabCount() - 1);
	}

	/**
     * Remove the tab, destroy the <tt>IdvWindow</tt> and all the associated 
     * <tt>ViewManagers</tt>
     * @param idx tab index
     */
    private void destroyDisplay(final int idx) {
    	String desc = removeTab(idx);
    	DisplayProps disp = displays.remove(desc);
    	disp.window.dispose();
    	for (ViewManager vm : disp.managers) {
    		vm.destroy();
    	}
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
		tabPane = new JTabbedPane();

		// compensate for Java3D/Swing issue
		tabPane.addChangeListener(new TabChangeListener());
		
		// listener for showing popup
		tabPane.addMouseListener(new MouseAdapter() {
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
		});
		
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
		JComponent contents = GuiUtils.topCenterBottom(
			toolbar, 
			tabPane, 
			statusBar
		);
		mainWindow.setContents(contents);
		
		setSize(this);
	}
	
	private String makeTabTitle(DisplayProps disp) {
    	return TAB_TITLE_PFX + " " + disp.number;
    }
	
	/**
	 * Create a window title suitable for an application window.
	 * @param title window title
	 * @return Application title plus the window title.
	 */
	private String makeWindowTitle(final String title) {
		return getIdv().getStateManager().getTitle() + " - " + title;
	}
	
	/**
     * Remove a tab from that tab container. Tab to display mapping is corrected
     * to compensate for removed tab.
     * @param idx tab index
     * @return descriptor of the <tt>DisplayProps</tt> associated with the tab
     * 	idx.
     */
    private String removeTab(final int idx) {
    	final String desc = tabDisplays.get(idx);
		final int tabCount = tabPane.getTabCount();
		// update tab indices to reflect change
		for (int i = idx + 1; i < tabCount; i++) {
			String d = tabDisplays.remove(i);
			tabDisplays.put(i - 1, d);
		}
		tabPane.removeTabAt(idx);
		return desc;
    }
	
	/**
	 * Show a particular tab.  This will set the components of all the tabs to 
	 * be an empty <tt>JPanel</tt> except the one to be shown.
	 * @param idx index of tab to show
	 */
	private void showTab(int idx) {

		if (idx < 0 || idx > tabPane.getTabCount() - 1) {
			idx = 0;
		}
		
		for (int i = 0; i < tabPane.getTabCount(); i++) {
			String dn = tabDisplays.get(i);
			tabPane.setComponentAt(idx, makeBlankTab(dn));
		}
		
		// show the selected one
		if (tabDisplays.containsKey(idx)) {
			String descName = tabDisplays.get(idx);
			DisplayProps disp = displays.get(descName);
			if (disp != null) {
				// make sure the VM in the show tab is active
				setActiveViewManager(disp);
				tabPane.setComponentAt(idx, disp.contents);
			}
		} else {
			System.err.println("Cannot find display index " + idx);
		}
	}
    
    /**
	 * Extract a tab to a window.
	 * @param idx Component index of the tab to extract.
	 */
	private void tabToWindow(final int idx) {
		
		// unregister as tab and get view
		String descName = tabDisplays.get(idx);
		DisplayProps disp = displays.get(descName);
		
		// must remove tab _before_ adding to window
		removeTab(idx);
		
		disp.window.getContentPane().removeAll();
		disp.window.getContentPane().add(disp.contents);
		disp.window.pack();
		
		if (getIdv().okToShowWindows()) {
			disp.window.show();
		}
		
		tabPane.setSelectedIndex(0);
	}
    
    /**
	 * Tabify a window. 
	 * @param descName The <tt>ViewDescriptor</tt> name that identifies the 
     *     <tt>ViewProps.component</tt> to convert from window to tab.
	 */
	private void windowToTab(final String descName) {
		DisplayProps disp = displays.get(descName);
		disp.window.getContentPane().removeAll();
		addDisplayTab(disp);
		tabPane.setSelectedIndex(0);
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
						tabToWindow(idx);
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
					String desc = tabDisplays.get(idx);
					DisplayProps props = displays.get(desc);
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
		
		return popup;
	}
    
    /**
     * Set the active view manager.
     * @param disp contains the view manager collection.
     */
    protected void setActiveViewManager(DisplayProps disp) {
		if (disp.managers.size() >= 1) {
			// FIXME: if there was more than one VM we should
			// remember the one that was active
			getVMManager().setLastActiveViewManager(
				disp.managers.get(0)
			);
		}
    }
}
