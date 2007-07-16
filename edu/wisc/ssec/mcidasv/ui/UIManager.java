package edu.wisc.ssec.mcidasv.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.util.Msg;

/**
 * Derive our own UI manager to do some specific things:
 * <ul>
 *   <li>Removing displays
 *   <li>Showing the dashboard
 *   <li>Adding toolbar customization options
 * </ul>
 */
public class UIManager extends IdvUIManager implements ActionListener {

    /** The tag in the xml ui for creating the special example chooser */
    public static final String TAG_EXAMPLECHOOSER = "examplechooser";

    /** Label for the icons-only radio menu item. */
    private final String LBL_TB_ICON_ONLY = "Display Only Icons";
    
    /** Label for the icons and labels radio menu item. */
    private final String LBL_TB_ICON_TEXT = "Display Icons with Labels";
    
    /** Label for the labels-only radio menu item. */
    private final String LBL_TB_TEXT_ONLY = "Display Only Labels";
    
    /** Label for the icon size check box menu item. */
    private final String LBL_TB_ICON_TYPE = "Enable Large Icons";

    /** Label for the "link" to the toolbar customization preference tab. */
    private final String LBL_TB_EDITOR = "Customize...";
    
    /** Action command for displaying only icons in the toolbar. */
    private final String ACT_ICON_ONLY = "action.toolbar.onlyicons";
    
    /** Action command for displaying both icons and labels in the toolbar. */
    private final String ACT_ICON_TEXT = "action.toolbar.iconsandtext";
    
    /** Action command for displaying only labels within the toolbar. */
    private final String ACT_TEXT_ONLY = "action.toolbar.onlytext";
    
    /** Action command for manipulating the size of the toolbar icons. */
    private final String ACT_ICON_TYPE = "action.toolbar.seticonsize";
    
    /** Action command for displaying the toolbar preference tab. */
    private final String ACT_SHOW_PREF = "action.toolbar.showprefs";
    
    /** Action command for removing all displays */
    private final String ACT_REMOVE_DISPLAYS = "action.displays.remove";
    
    /** Action command for showing the dashboard */
    private final String ACT_SHOW_DASHBOARD = "action.dashboard.show";
        
    /** Handy reference to the name of the IDV toolbar customization tab. */
    private final String TOOLBAR_TAB_NAME = "Toolbar";

    /** Reference to the icon size checkbox for easy enabling/disabling. */
    private JCheckBoxMenuItem largeIconsEnabled;
    
    private boolean iconsEnabled = true;
    
    private boolean labelsEnabled = false;
    
    // TODO: determine if this is needed.
    private JComponent toolbarUI;
    
    /** */
    private final int MENU_RADIO = 31337;
    
    /** */
    private final int MENU_CHECKBOX = 31338;
    
    /** */
    private final int MENU_NORMAL = 31339;

    private final String ICON_LOCATION = "/edu/wisc/ssec/mcidasv/resources/icons/";
    
    private final String LARGE_ICON_PREFIX = "large/";
    private final String SMALL_ICON_PREFIX = "small/";
    
    private static final String PROP_TOOLBAR_MENU = "mcidasv.toolbarMenu";
    
    private String[] iconMap = {
    	       "show-dashboard.png",
    	        "new-display.png",
    	        "open-bundle.png",
    	        "save-bundle.png",
    	        "fave-bundle.png",
    	        "manage-faves.png",
    	        "show-help.png",
    	        "show-tips.png",
    	        "support-request.png",
    	        "remove-displays.png",
    	        "remove-displays-data.png",
    	        "drawing-control.png",
    	        "location-indicator.png",
    	        "range-bearing.png",
    	        "background-image.png",
    	        "stop-loads.png",
    	        "exit.png"
    };
    
    private String[] labelMap = {
    	"Show Dashboard",
    	"New Display",
    	"Open Bundle",
    	"Save Bundle",
    	"Favorite Bundle",
    	"Manage Favorites",
    	"Help",
    	"Tips",
    	"Support",
    	"Remove Displays",
    	"Remove Displays and Data",
    	"Drawing Control",
    	"Location Indicator",
    	"Range Bearing?",
    	"Background Image",
    	"Stop Loading",
    	"Exit McIDAS-V"
    };
    
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
    private Object[][] types = {
    		{ACT_ICON_ONLY, MENU_RADIO, LBL_TB_ICON_ONLY},
    		{ACT_ICON_TEXT, MENU_RADIO, LBL_TB_ICON_TEXT},
    		{ACT_TEXT_ONLY, MENU_RADIO, LBL_TB_TEXT_ONLY},
    		{null, null, null},
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
     * Add in the menu items for the given display menu
     *
     * @param displayMenu The display menu
     */
    protected void initializeDisplayMenu(JMenu displayMenu) {
        JMenuItem mi;
        
        mi = new JMenuItem("Remove All Displays");
        mi.addActionListener(this);
        mi.setActionCommand(ACT_REMOVE_DISPLAYS);
        displayMenu.add(mi);
        displayMenu.addSeparator();                                                                                 
    	
        processBundleMenu(displayMenu,
                          IdvPersistenceManager.BUNDLES_FAVORITES);
        processBundleMenu(displayMenu, IdvPersistenceManager.BUNDLES_DISPLAY);

        processMapMenu(displayMenu, true);
        processStationMenu(displayMenu, true);
        processStandAloneMenu(displayMenu, true);
        
        mi = new JMenuItem("Show Dashboard");
        mi.addActionListener(this);
        mi.setActionCommand(ACT_SHOW_DASHBOARD);
        displayMenu.addSeparator();
        displayMenu.add(mi);
        
        Msg.translateTree(displayMenu);
    }
    
    /**
     *  
     * 
     * @param bigIcons Whether or not the icons in the toolbar should be made
     *                 "big" or "small".
     */
    private void resizeToolbarIcons(boolean bigIcons) {
    	if (bigIcons)
    		System.err.println("ui manager: event: enabled large icons");
    	else
    		System.err.println("ui manager: event: disabled large icons");    	
    }
    
    /**
     * 
     * @param enabled Whether or not icons should be displayed in the toolbar.
     */
    private void displayToolbarIcons(boolean enabled) {
    	if (enabled) {
    		System.err.println("ui manager: event: enabled toolbar icons");
    		largeIconsEnabled.setEnabled(true);
    	}
    	else {
    		System.err.println("ui manager: event: disabled toolbar icons");
    		largeIconsEnabled.setEnabled(false);
    	}
    	
    	iconsEnabled = enabled;
    	
    }
    
    /**
     * 
     * 
     * @param enabled Whether or not to display the labels associated with 
     *                toolbar icons.
     */
    private void displayToolbarLabels(boolean enabled) {
    	if (enabled) {
    		System.err.println("ui manager: event: enabled toolbar labels");    		
    	} 
    	else {
    		System.err.println("ui manager: event: disabled toolbar labels");
    	}

    	labelsEnabled = enabled;
    }

    private JPanel createToolbar(boolean icons, boolean labels) {
    	String path;

    	if (largeIconsEnabled.getState())
    		path = ICON_LOCATION + LARGE_ICON_PREFIX;
    	else
    		path = ICON_LOCATION + SMALL_ICON_PREFIX;
    	
    	JPanel newPanel = new JPanel();
    	    	
    	for (int i = 0; i < iconMap.length; i++) {
    		JButton button;
    		    		
    		if (icons == true) {
        		URL iconUrl = this.getClass().getResource(path + iconMap[i]);
        		Icon icon = new ImageIcon(iconUrl);
        		
        		if (labels == true)
        			button = new JButton(labelMap[i], icon);
        		else
        			button = new JButton(icon);
    		} else {
    			button = new JButton(labelMap[i]);
    		}
    		
    		button.setContentAreaFilled(true);
    		button.setBorder(BorderFactory.createEtchedBorder());
    		
    		newPanel.add(button);
    	}
    	    	
    	return newPanel; 
    }
    
    /** 
     * Override to add some toolbar customization options to the JComponent 
     * returned by the IDV getToolbarUI method. The layout and menu items that 
     * appear within the customization menu are determined by the contents of 
     * <code>types</code> field.
     * 
     * @see ucar.unidata.idv.ui.IdvUIManager#getToolbarUI()
     * 
     * FIXME: doesn't trigger when a user right clicks over an icon!
	 * TODO: determine whether or not popup menu hides correctly.
	 * 
	 * @return The modified version of the IDV toolbar.
     */
    public JComponent getToolbarUI() {
    	toolbarUI = super.getToolbarUI();
    	
    	if (!getIdv().getProperty(PROP_TOOLBAR_MENU, false))
    		return toolbarUI;
    	
    	System.err.println(toolbarUI);
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
    					if (action.startsWith(ACT_ICON_TYPE))
    						largeIconsEnabled = (JCheckBoxMenuItem)item;
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
 
    	toolbarUI.addMouseListener(popupListener);
    	return toolbarUI;
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
    	
    	if (cmd.startsWith(ACT_ICON_ONLY)) {
    		displayToolbarIcons(true);
    		displayToolbarLabels(false);
    		toolbarEditEvent = true;
    	} 
    	else if (cmd.startsWith(ACT_ICON_TEXT)) {
    		displayToolbarIcons(true);
    		displayToolbarLabels(true);
    		toolbarEditEvent = true;
    	} 
    	else if (cmd.startsWith(ACT_TEXT_ONLY)) {
    		displayToolbarIcons(false);
    		displayToolbarLabels(false);
    		toolbarEditEvent = true;
    	} 
    	else if (cmd.startsWith(ACT_SHOW_PREF)) {
    		IdvPreferenceManager prefs = getIdv().getPreferenceManager();
    		prefs.showTab(TOOLBAR_TAB_NAME);
    		toolbarEditEvent = true;
    	} 
    	else if (cmd.startsWith(ACT_ICON_TYPE)) {
    		resizeToolbarIcons(!largeIconsEnabled.getState());
    		toolbarEditEvent = true;
    	}
    	
    	else if (cmd.startsWith(ACT_REMOVE_DISPLAYS))
    		getIdv().removeAllDisplays();
    	
    	else if (cmd.startsWith(ACT_SHOW_DASHBOARD))
    		showDashboard();
    	
    	else
    		System.err.println("Unsupported action event!");
    	
    	if (toolbarEditEvent == true) {
    		JPanel newPanel = createToolbar(iconsEnabled, labelsEnabled);
    		toolbarUI.setVisible(false);
    		toolbarUI.remove(0);
    		
    		toolbarUI.setLayout(new BorderLayout());
    		
    		toolbarUI.add(newPanel, BorderLayout.WEST);
    		toolbarUI.setVisible(true);
    		toolbarUI.validate();
    	}
    }
	
	/**
	 * Handle mouse clicks that occur within the toolbar.  
	 */
    private class PopupListener extends MouseAdapter {
    	private JPopupMenu popup;
    	
    	public PopupListener(JPopupMenu p) {
    		popup = p;
    	}
    	
    	public void mousePressed(MouseEvent e) {
    		// isPopupTrigger is very nice. It varies depending upon whatever
    		// the norm is for the current platform.
    		if (e.isPopupTrigger()) {
    			if (!popup.isVisible()) {
    				popup.show(e.getComponent(), e.getX(), e.getY());
    			}
    		}
    	}    	
    }
    // end PopupListener. So many brackets!    
}