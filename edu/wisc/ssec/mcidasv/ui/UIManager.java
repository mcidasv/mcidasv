package edu.wisc.ssec.mcidasv.ui;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.w3c.dom.Element;

import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.SavedBundle;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.IdvXmlUi;
import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.TwoFacedObject;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.StateManager;

/**
 * <p>Derive our own UI manager to do some specific things:
 * <ul>
 *   <li>Removing displays
 *   <li>Showing the dashboard
 *   <li>Adding toolbar customization options
 * </ul></p>
 * 
 * TODO: should probably change out the toolbar jpanels to be actual JToolbars
 */
public class UIManager extends IdvUIManager implements ActionListener {

    /** The tag in the xml ui for creating the special example chooser */
    public static final String TAG_EXAMPLECHOOSER = "examplechooser";

    /** Action command for displaying only icons in the toolbar. */
    private static final String ACT_ICON_ONLY = "action.toolbar.onlyicons";
    
    /** Action command for displaying both icons and labels in the toolbar. */
    private static final String ACT_ICON_TEXT = "action.toolbar.iconsandtext";
    
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
    
    /** Action command for displaying only labels within the toolbar. */
    private static final String ACT_TEXT_ONLY = "action.toolbar.onlytext";
    
    /** Label for the "link" to the toolbar customization preference tab. */
    private static final String LBL_TB_EDITOR = "Customize...";
    
    /** Label for the icons-only radio menu item. */
    private static final String LBL_TB_ICON_ONLY = "Display Only Icons";
    
    /** Label for the icons and labels radio menu item. */
    private static final String LBL_TB_ICON_TEXT = "Display Icons with Labels";
    
    /** Label for the icon size check box menu item. */
    private static final String LBL_TB_ICON_TYPE = "Enable Large Icons";
    
    /** Label for the labels-only radio menu item. */
    private static final String LBL_TB_TEXT_ONLY = "Display Only Labels";
        
    /** Constant signifying a JCheckBoxMenuItem. */
    private static final int MENU_CHECKBOX = 31338;
    
    /** Constant signifying a JMenuItem. */
    private static final int MENU_NORMAL = 31339;
    
    /** Constant that signifies a JRadioButtonMenuItem. */
    private static final int MENU_RADIO = 31337;
    
    /** The IDV property that reflects the size of the icons. */
    private static final String PROP_ICON_SIZE = "idv.ui.iconsize";
    
    /** The URL of the script that processes McIDAS-V support requests. */
    private static final String SUPPORT_REQ_URL = 
    	"http://www.ssec.wisc.edu/mcidas/misc/mc-v/supportreq/support.php";
            
    /** Separator to use between window title components. */
	protected static final String TITLE_SEPARATOR = " - ";
    
	/** Whether or not we need to tell IDV about the toolbar. */
    private boolean addToolbarToWindowList = true;
    
    /** Keep the dashboard arround so we don't have to re-create it each time. */
    protected IdvWindow dashboard;
    
    /** Whether or not icons should be displayed in the toolbar. */
    private boolean iconsEnabled = true;
    
    /** Whether or not labels should be displayed in the toolbar. */
    private boolean labelsEnabled = false;
        
    /** Reference to the icon size checkbox for easy enabling/disabling. */
    private JCheckBoxMenuItem largeIconsEnabled;    
 
    /**
     * Reference to the toolbar container that the IDV is playing with.
     */
    private JComponent toolbar;

    /** The splash screen (minus easter egg). */
    private McvSplash splash;
    
    /** 
     * <p>This array essentially serves as a friendly way to write the contents
     * of the toolbar customization popup menu. The layout of the popup menu
     * will basically look exactly like it does here in the code.</p> 
     * 
     * <p>Each item in the menu must have an action command, the Swing widget 
     * type, and then the String that'll let a user figure out what the widget
     * is supposed to do. The ordering is also important:<br/> 
     * <code>String action, int widgetType, String label.</code></p>
     * 
     * <p>If you'd like a separator to appear, simply make every part of the
     * entry null.</p>
     */
    /*private Object[][] types = {
    		{ACT_ICON_ONLY, MENU_RADIO, LBL_TB_ICON_ONLY},
    		{ACT_ICON_TEXT, MENU_RADIO, LBL_TB_ICON_TEXT},
    		{ACT_TEXT_ONLY, MENU_RADIO, LBL_TB_TEXT_ONLY},
    		{null, null, null},
    		{ACT_ICON_TYPE, MENU_CHECKBOX, LBL_TB_ICON_TYPE},
    		{null, null, null},
    		{ACT_SHOW_PREF, MENU_NORMAL, LBL_TB_EDITOR}
    };*/
    private Object[][] types = {
    		{ACT_ICON_TYPE, MENU_CHECKBOX, LBL_TB_ICON_TYPE},
    		{null, null, null},
    		{ACT_SHOW_PREF, MENU_NORMAL, LBL_TB_EDITOR}
    };

    /**
     * Hands off our IDV instantiation to IdvUiManager.
     *
     * @param idv The idv
     */
    public UIManager(IntegratedDataViewer idv) {
        super(idv);
    }

    /**
     * Make a window title.  The format for window titles is:
     * <pre>
     * &lt;window&gt;TITLE_SEPARATOR&lt;document&gt;
     * </pre>
     * @param window Window title.
     * @param document Document or window sub-content.
     * @return Formatted window title.
     */
    protected static String makeTitle(final String window, final String document) {
    	if (window == null) {
    		return "";
    	} else if (document == null) {
    		return window;
    	}
    	return window.concat(TITLE_SEPARATOR).concat(document);
    }
    
    /**
     * Make a window title.  The format for window titles is:
     * <pre>
     * &lt;window&gt;TITLE_SEPARATOR&lt;document&gt;TITLE_SEPARATOR&lt;other&gt;
     * </pre>
     * @param window Window title.
     * @param document Document or window sub content.
     * @param other Other content to include.
     * @return Formatted window title.
     */
    protected static String makeTitle(final String window, final String document, final String other) {
    	if (other == null) {
    		return makeTitle(window, document);
    	}
    	return window.concat(TITLE_SEPARATOR).concat(document).concat(TITLE_SEPARATOR).concat(other);
    }
        
    /**
     * Split window title using <tt>TITLE_SEPARATOR</tt>.
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
     * Initialize the given menu before it is shown
     * @see ucar.unidata.idv.ui.IdvUIManager#historyMenuSelected(JMenu)
     */
    @Override
    protected void handleMenuSelected(String id, JMenu menu) {
        if (id.equals(MENU_WINDOWS)) {
            menu.removeAll();
            makeWindowsMenu(menu);
        } else if (id.equals("file.newdata") || id.equals("data.newdata")) {
            menu.removeAll();
            GuiUtils.makeMenu(
                menu,
                getIdvChooserManager().makeChooserMenus(new ArrayList()));
        } else if (id.equals(MENU_NEWVIEWS)) {
            menu.removeAll();
            makeViewStateMenu(menu);
        } else if (id.equals(MENU_HISTORY)) {
            historyMenuSelected(menu);
        } else if (id.equals(MENU_EDITFORMULAS)) {
            editFormulasMenuSelected(menu);
        } else if (id.equals(MENU_DELETEHISTORY)) {
            deleteHistoryMenuSelected(menu);
        } else if (id.equals(MENU_DELETEVIEWS)) {
            menu.removeAll();
            makeDeleteViewsMenu(menu);
        } else if (id.equals(MENU_DISPLAYS)) {
            menu.removeAll();
            initializeDisplayMenu(menu);
        } else if (id.equals(MENU_MAPS)) {
            if (menu.getItemCount() == 0) {
                processMapMenu(menu, false);
            }
        } else if (id.equals(MENU_LOCATIONS)) {
            if (menu.getItemCount() == 0) {
                Msg.addDontComponent(menu);
                processStationMenu(menu, false);
            }
        } else if (id.equals("bundles")) {
        	menu.removeAll();
        	makeBundleMenu(menu);
        }
    }

    protected IdvXmlUi doMakeIdvXmlUi(IdvWindow window, List viewManagers, Element skinRoot) {
    	return new McIDASVXmlUi(window, viewManagers, getIdv(), skinRoot);
    }
    
    /**
     * DeInitialize the given menu before it is shown
     * @see ucar.unidata.idv.ui.IdvUIManager#historyMenuSelected(JMenu)
     */
    @Override
    protected void handleMenuDeSelected(String id, JMenu menu) {
        if (id.equals(MENU_DISPLAYS)) {
            menu.removeAll();
        }
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
        editor.addHyperlinkListener(getIdv());

        final JLabel iconLbl = new JLabel(
        	GuiUtils.getImageIcon(getIdv().getProperty(PROP_SPLASHICON, ""))
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
						new URL(getIdv().getProperty(Constants.PROP_HOMEPAGE, ""))
					);
				} catch (MalformedURLException e) {}
        		getIdv().hyperlinkUpdate(link);
        	}
        });
        JPanel contents = GuiUtils.topCenter(
        	GuiUtils.inset(iconLbl, 5),
            GuiUtils.inset(editor, 5)
        );
        contents.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        
        final JDialog dialog = GuiUtils.createDialog(
        	getFrame(),
            "About " + getStateManager().getTitle(),
            false
        );
        dialog.add(contents);
        JButton close = new JButton("Close");
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
     * Create the splash screen if needed
     */
    public void initSplash() {
        if (getProperty(PROP_SHOWSPLASH, true)
                && !getArgsManager().getNoGui()
                && !getArgsManager().getIsOffScreen()
                && !getArgsManager().testMode) {
            splash = new McvSplash(getIdv());
            splashMsg("Loading Programs");
        }
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
     * Close and dispose of the splash window (if it has been created).
     */
    public void splashClose() {
        if (splash != null)
            splash.doClose();
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
    	
    	// handle selecting the icon-only menu item
    	if (cmd.startsWith(ACT_ICON_ONLY)) {
    		iconsEnabled = true;
    		labelsEnabled = false;
    		largeIconsEnabled.setEnabled(true);
    		toolbarEditEvent = true;
    	} 
    	
    	// handle selecting the icon and label menu item
    	else if (cmd.startsWith(ACT_ICON_TEXT)) {
    		iconsEnabled = true;
    		labelsEnabled = true;
    		largeIconsEnabled.setEnabled(true);
    		toolbarEditEvent = true;
    	}
    	
    	// handle selecting the label-only menu item
    	else if (cmd.startsWith(ACT_TEXT_ONLY)) {
    		iconsEnabled = false;
    		labelsEnabled = false;
    		largeIconsEnabled.setEnabled(false);
    		toolbarEditEvent = true;
    	}
    	
    	// handle the user selecting the show toolbar preference menu item
    	else if (cmd.startsWith(ACT_SHOW_PREF)) {
    		IdvPreferenceManager prefs = getIdv().getPreferenceManager();
    		prefs.showTab(Constants.PREF_LIST_TOOLBAR);
    		toolbarEditEvent = true;
    	}
    	
    	// handle the user toggling the size of the icon
    	else if (cmd.startsWith(ACT_ICON_TYPE))
    		toolbarEditEvent = true;

    	// handle the user removing displays
    	else if (cmd.startsWith(ACT_REMOVE_DISPLAYS))
    		getIdv().removeAllDisplays();
    	
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
    		if (largeIconsEnabled.getState() == true)
    			getStateManager().writePreference(PROP_ICON_SIZE, "32");
    		else
    			getStateManager().writePreference(PROP_ICON_SIZE, "16");

    		updateIconBar();
    	}
    }
    
    /**
     * Get the component responsible for selecting the current display. This
     * component is fully contained and requires no further configuration
     * to function properly.
     * @return JComponent that will change the current view according to user
     * 	input.
     */
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
        
    	// create nodes from existing windows
    	for (IdvWindow window : (List<IdvWindow>)IdvWindow.getWindows()) {
    		if (window.getViewManagers().size() == 0) {
    			continue;
    		}
    		String[] titles = splitTitle(window.getTitle());
    		String label = titles.length > 1 ? titles[1] : titles[0];
    		DefaultMutableTreeNode displayNode = new DefaultMutableTreeNode(label);
    		List<ViewManager> views = window.getViewManagers();
    		for (int i = 0; i < views.size(); i++) {
    			ViewManager view = views.get(i);
    			String name = view.getName();
    			TwoFacedObject tfo = null;
    			if (name != null && name.length() > 0) {
    				tfo = new TwoFacedObject(name, view);
    			} else {
    				tfo = new TwoFacedObject(Constants.PANEL_NAME + " " + (i+1), view);
    			}
    			displayNode.add(new DefaultMutableTreeNode(tfo));
    		}
    		root.add(displayNode);
    	}
    	
    	// select the appropriate view
    	tree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent evt) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
				if (node == null || !(node.getUserObject() instanceof TwoFacedObject)) {
					return;
				}
				TwoFacedObject tfo = (TwoFacedObject) node.getUserObject();
				ViewManager viewManager = (ViewManager) tfo.getId();
				getIdv().getVMManager().setLastActiveViewManager(viewManager);
			}
    	});
    	
    	// expand all the nodes
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandPath(tree.getPathForRow(i));
        }

        return tree;
    }    
    
    /** 
     * <p>Override to add some toolbar customization options to the JComponent 
     * returned by the IDV getToolbarUI method. The layout and menu items that 
     * appear within the customization menu are determined by the contents of 
     * <code>types</code> field.</p>
     *
     * <p>FIXME: doesn't trigger when a user right clicks over an icon!
     * 
     * @see ucar.unidata.idv.ui.IdvUIManager#getToolbarUI()
     * 
	 * @return The modified version of the IDV toolbar.
     */
    public JComponent getToolbarUI() {
    	toolbar = super.getToolbarUI();
    	toolbar = GuiUtils.center(toolbar);
    	
    	JPopupMenu popup = new JPopupMenu();
    	ButtonGroup group = new ButtonGroup();
    	MouseListener popupListener = new PopupListener(popup);
    	    	
    	// time to create the toolbar customization menu.
    	for (int i = 0; i < types.length; i++) {
    		Object[] tempArr = types[i];
    		
    		// determine whether or not this entry is a separator. if it isn't,
    		// do some work and create the right types of menu items.
    		if (tempArr[0] != null) {
    			
    			JMenuItem item;
    			String action = (String)tempArr[0];
    			String label = (String)tempArr[2];
    			
    			int type = ((Integer)tempArr[1]).intValue();
    			
    			switch (type) {
    				case MENU_RADIO:
    					item = new JRadioButtonMenuItem(label);
    					group.add(item);
    					break;
    				
    				case MENU_CHECKBOX:
    					item = new JCheckBoxMenuItem(label);
    					if (action.startsWith(ACT_ICON_TYPE)) {
    						largeIconsEnabled = (JCheckBoxMenuItem)item;
    						
    						// make sure the previous selection persists
    						// across restarts.
    						String val = (String)getStateManager()
    							.getPreference(PROP_ICON_SIZE);
    						
    						if (val == null || val.equals("16"))
    							largeIconsEnabled.setState(false);
    						else
    							largeIconsEnabled.setState(true);
    					}
    					break;
    				
    				default:
    					// TODO: rethink this.
    					// this is intended to be the case that catches all the
    					// normal jmenuitems. I should probably rewrite this to
    					// look for a MENU_NORMAL flag or something.
    					item = new JMenuItem(label);
    					break;
    			}

    			item.addActionListener(this);
    			item.setActionCommand(action);
    			popup.add(item);
    		} else {
    			popup.addSeparator();
    		}
    	}
 
    	toolbar.addMouseListener(popupListener);

    	return toolbar;
    }
    
    /**
     * Show the support request form
     *
     * @param description Default value for the description form entry
     * @param stackTrace The stack trace that caused this error.
     * @param dialog The dialog to put the gui in, if non-null.
     */
    public void showSupportForm(final String description,
                                final String stackTrace,
                                final JDialog dialog) {
    	//Must do this in a non-swing thread
        Misc.run(new Runnable() {
            public void run() {            	
                showSupportFormInThread(description, stackTrace, dialog);
            }
        });
    }
        
    /**
     * Need to override the IDV updateIconBar so we can preemptively add the
     * toolbar to the window manager (otherwise the toolbar won't update).
     */
    public void updateIconBar() {
    	if (addToolbarToWindowList == true && IdvWindow.getActiveWindow() != null) {
    		addToolbarToWindowList = false;
    		IdvWindow.getActiveWindow().addToGroup(IdvWindow.GROUP_TOOLBARS, toolbar);
    		IdvWindow.getActiveWindow().addToGroup(COMP_FAVORITESBAR, toolbar);
    	}
    	
    	super.updateIconBar();
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

        StringBuffer extra   = new StringBuffer("<h3>OS</h3>\n");
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
                    Map m = (Map) method.invoke(c, new Object[] {});
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
            new HttpFormEntry(HttpFormEntry.TYPE_AREA,
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
        	new JLabel("<html>This form allows you to send a support request to the MUG support team.<br></html>");

        JCheckBox includeBundleCbx =
            new JCheckBox("Include Current State as Bundle", false);

        boolean alreadyHaveDialog = true;
        if (dialog == null) {
            dialog = GuiUtils.createDialog(LogUtil.getCurrentWindow(),
                                           "Support Request Form", true);
            alreadyHaveDialog = false;
        }

        JLabel statusLabel = GuiUtils.cLabel(" ");
        JComponent bottom = GuiUtils.vbox(GuiUtils.left(includeBundleCbx),
                                          statusLabel);

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
                extra.append(getIdv().getPluginManager().getPluginHtml());
                extra.append(getResourceManager().getHtmlView());

                entriesToPost.add(new HttpFormEntry("form_data[att_one]",
                        "extra.html", extra.toString().getBytes()));

                if (includeBundleCbx.isSelected()) {
                    entriesToPost.add(
                        new HttpFormEntry(
                            "form_data[att_two]", "bundle.xidv",
                            getIdv().getPersistenceManager().getBundleXml(
                                true).getBytes()));
                }

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
     * Method to do the work of showing the Data Explorer (nee Dashboard)
     */
    public void showDashboard(String tabName) {
    	if (dashboard == null) {
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
    	} else {
    		dashboard.show();
    	}
    	
    	if (tabName.equals("")) return;
    	
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
     * Overridden to build a custom Window menu.
     * @see ucar.unidata.idv.ui.IdvUIManager#makeWindowsMenu(JMenu)
     */
    @Override
    public void makeWindowsMenu(JMenu windowMenu) {
        JMenuItem mi;
        boolean first = true;
        
        mi = new JMenuItem("Show Data Explorer");
        mi.addActionListener(this);
        mi.setActionCommand(ACT_SHOW_DASHBOARD);
        windowMenu.add(mi);
        
        List windows = new ArrayList(IdvWindow.getWindows());
    	for (int i = 0; i < windows.size(); i++) {
    		final IdvWindow window = ((IdvWindow)windows.get(i));
    		// Skip the main window
    		if (window.getIsAMainWindow()) continue;
    		String title = window.getTitle();
    		String titleParts[] = splitTitle(title);
    		if (titleParts.length == 2) title = titleParts[1];
    		// Skip the data explorer and display controller
    		if (title.equals(Constants.DATASELECTOR_NAME))
    			continue;
    		// Add a meaningful name if there is none
    		if (title.equals("")) title = "<Unnamed>";
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
     * Populate a menu with bundles known to the <tt>PersistenceManager</tt>.
     * @param inBundleMenu The menu to populate
     */
    public void makeBundleMenu(JMenu inBundleMenu) {
    	final int bundleType = IdvPersistenceManager.BUNDLES_FAVORITES;
    	
        JMenuItem mi;
        mi = new JMenuItem("Manage...");
        mi.setMnemonic(GuiUtils.charToKeyCode("M"));
        inBundleMenu.add(mi);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showBundleDialog(bundleType);
            }
        });
        inBundleMenu.addSeparator();
    	
        final List bundles = getPersistenceManager().getBundles(bundleType);
        if (bundles.size() == 0) {
            return;
        }
        final String title =
            getPersistenceManager().getBundleTitle(bundleType);
        final String bundleDir =
            getPersistenceManager().getBundleDirectory(bundleType);

        JMenu bundleMenu = new JMenu(title);
        bundleMenu.setMnemonic(GuiUtils.charToKeyCode(title));

//        getPersistenceManager().initBundleMenu(bundleType, bundleMenu);

        Hashtable catMenus = new Hashtable();
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
        mi.setText("Add Location Indicator");
        displayMenu.add(mi);
        
        ControlDescriptor locationDescriptor =
        	getIdv().getControlDescriptor("locationcontrol");
        if (locationDescriptor != null) {
        	List stations = getIdv().getLocationList();
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
                    getIdv().getControlDescriptor("mapdisplay");
                if (mapDescriptor == null) {
                    return;
                }
                String attrs =
                    "initializeAsDefault=true;displayName=Default Background Maps;";
                getIdv().doMakeControl(new ArrayList(), mapDescriptor, attrs, null);
            }
        });
        displayMenu.add(mi);
        
        Msg.translateTree(displayMenu);
    }

    private JMenuItem makeControlDescriptorItem(ControlDescriptor cd) {
    	JMenuItem mi = new JMenuItem();
        if (cd != null) {
	        mi = new JMenuItem(cd.getLabel());
	        mi.addActionListener(new ObjectListener(cd) {
	        	public void actionPerformed(ActionEvent ev) {
	        		getIdv().doMakeControl(new ArrayList(),
	        				(ControlDescriptor) theObject);
	        	}
	        });
        }
        return mi;
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
}