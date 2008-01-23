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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
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
import javax.swing.JToolBar;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.SavedBundle;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.IdvXmlUi;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.ui.XmlUi;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.StateManager;

/**
 * <p>Derive our own UI manager to do some specific things:
 * <ul>
 *   <li>Removing displays</li>
 *   <li>Showing the dashboard</li>
 *   <li>Adding toolbar customization options</li>
 *   <li>Implement the McIDAS-V toolbar as a JToolbar.</li>
 * </ul></p>
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

    /** Action command for using large toolbar icons. */
    private static final String ACT_LARGE_ICONS = "action.icons.large";

    /** Action command for using medium toolbar icons. */
    private static final String ACT_MEDIUM_ICONS = "action.icons.medium";

    /** Action command for using small toolbar icons. */
    private static final String ACT_SMALL_ICONS = "action.icons.small";

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

    private static final String BAD_ACTION_MSG = "Unknown action (%s) found in your toolbar. McIDAS-V will continue to load, but there will be no button associated with %s.";

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

    /** The label for large icons in the toolbar popup menu. */
    private static final String LARGE_LABEL = "Large Icons";

    /** The label for medium icons in the toolbar popup menu. */
    private static final String MEDIUM_LABEL = "Medium Icons";

    /** The label for small icons in the toolbar popup menu. */
    private static final String SMALL_LABEL = "Small Icons";

    /** The default icon dimension. */
    private static final String DEFAULT_ICON_SIZE = "32";

    /** Large icons are LARGE_DIMENSION x LARGE_DIMENSION. */
    private static final int LARGE_DIMENSION = 32;

    /** Medium icons are MEDIUM_DIMENSION x MEDIUM_DIMENSION. */
    private static final int MEDIUM_DIMENSION = 22;

    /** 1782^12 + 1841^12 = 1922^12 (hand calculator recommended). */
    private static final int SMALL_DIMENSION = 16;

    /** The IDV property that reflects the size of the icons. */
    private static final String PROP_ICON_SIZE = "idv.ui.iconsize";

    /** McV property for what appears in the toolbar: icons, labels, or both */
    private static final String PROP_ICON_LABEL = "mcv.ui.toolbarlabels";    

    // TODO: ought to replace this with an enum
    /** */
    private static final int TOOLBAR_ICONS = 0xDEADBEEF;

    // TODO: ought to replace this with an enum
    /** */
    private static final int TOOLBAR_LABELS = 0xCAFEBABE;

    // TODO: ought to replace this with an enum
    /** */
    private static final int TOOLBAR_BOTH = 0xDECAFBAD;

    /** */
    private static final String DEFAULT_TOOLBAR_STYLE = 
    	Integer.toString(TOOLBAR_ICONS);

    /** */
    private int toolbarStyle;

    /** The URL of the script that processes McIDAS-V support requests. */
    private static final String SUPPORT_REQ_URL = 
    	"http://www.ssec.wisc.edu/mcidas/misc/mc-v/supportreq/support.php";

    /** Separator to use between window title components. */
    protected static final String TITLE_SEPARATOR = " - ";

    /** Whether or not we need to tell IDV about the toolbar. */
    private boolean addToolbarToWindowList = true;

    /** Whether or not icons should be displayed in the toolbar. */
    private boolean iconsEnabled = true;    

    /** Whether or not labels should be displayed in the toolbar. */
    private boolean labelsEnabled = false;

    /** Stores all available actions. */
    private Hashtable<String, String[]> cachedActions;

    /**
     * <p>The currently "displayed" actions. Keeping this List allows us to get 
     * away with only reading the XML files upon starting the application and 
     * only writing the XML files upon exiting the application. This will avoid
     * those redrawing delays.</p>
     */
    private List<String> cachedButtons;

    /** Reference to the icon size checkbox for easy enabling/disabling. */
    private JCheckBoxMenuItem largeIconsEnabled;

    /** The splash screen (minus easter egg). */
    private McvSplash splash;

    /** Reference to the toolbar that the IDV is playing with. */
    private JToolBar toolbar;

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
        cachedActions = readActions();
        cachedButtons = readToolbar();
    }

    /**
     * Override IdvUIManager's loadLookAndFeel so that we can force the IDV to
     * load the Aqua look and feel if requested from the command line.
     */
    @Override
    public void loadLookAndFeel() {
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
    		getStateManager().writePreference(PROP_ICON_LABEL, TOOLBAR_ICONS);
    		iconsEnabled = true;
    		toolbarEditEvent = true;
    	}

    	// handle selecting the icon and label menu item
    	else if (cmd.startsWith(ACT_ICON_TEXT)) {
    		getStateManager().writePreference(PROP_ICON_LABEL, TOOLBAR_BOTH);
    		iconsEnabled = true;
    		toolbarEditEvent = true;
    	}

    	// handle selecting the label-only menu item
    	else if (cmd.startsWith(ACT_TEXT_ONLY)) {
    		getStateManager().writePreference(PROP_ICON_LABEL, TOOLBAR_LABELS);
    		iconsEnabled = false;
    		toolbarEditEvent = true;
    	}

    	// handle selecting large icons
    	else if (cmd.startsWith(ACT_LARGE_ICONS)) {
    		getStateManager().writePreference(PROP_ICON_SIZE, LARGE_DIMENSION);
    		toolbarEditEvent = true;
    	}

    	// handle selecting medium icons
    	else if (cmd.startsWith(ACT_MEDIUM_ICONS)) {
    		getStateManager().writePreference(PROP_ICON_SIZE, MEDIUM_DIMENSION);
    		toolbarEditEvent = true;
    	}

    	// handle selecting small icons
    	else if (cmd.startsWith(ACT_SMALL_ICONS)) {
    		getStateManager().writePreference(PROP_ICON_SIZE, SMALL_DIMENSION);
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
    		// destroy the menu so it can be properly updated during rebuild
    		toolbarMenu = null;

    		toolbar.setVisible(false);
    		populateToolbar(toolbar);
    		toolbar.setVisible(true);
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
				idv.getVMManager().setLastActiveViewManager(viewManager);
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
    	JPopupMenu popup = new JPopupMenu();
    	MouseListener popupListener = new PopupListener(popup);

    	ButtonGroup formatGroup = new ButtonGroup();
    	ButtonGroup sizeGroup = new ButtonGroup();

    	// which icon size radio button should be selected by default?
    	Integer iconSize = 
    		new Integer(getStateManager().getPreferenceOrProperty(PROP_ICON_SIZE, DEFAULT_ICON_SIZE));

    	Integer style = 
    		new Integer(getStateManager().getPreferenceOrProperty(PROP_ICON_LABEL, DEFAULT_TOOLBAR_STYLE));

    	// add in the options that pertain to the format of the toolbar items
    	JMenuItem item = new JRadioButtonMenuItem(LBL_TB_ICON_ONLY);
    	if (style == TOOLBAR_ICONS)
    		item.setSelected(true);
    	item.setActionCommand(ACT_ICON_ONLY);
    	item.addActionListener(this);
    	popup.add(item);
    	formatGroup.add(item);

    	item = new JRadioButtonMenuItem(LBL_TB_ICON_TEXT);
    	if (style == TOOLBAR_BOTH)
    		item.setSelected(true);
    	item.setActionCommand(ACT_ICON_TEXT);
    	item.addActionListener(this);
    	popup.add(item);
    	formatGroup.add(item);

    	item = new JRadioButtonMenuItem(LBL_TB_TEXT_ONLY);
    	if (style == TOOLBAR_LABELS)
    		item.setSelected(true);
    	item.setActionCommand(ACT_TEXT_ONLY);
    	item.addActionListener(this);
    	popup.add(item);
    	formatGroup.add(item);

    	popup.addSeparator();

    	// add in the options that pertain to icon size
    	item = new JRadioButtonMenuItem(LARGE_LABEL);
    	if (iconSize == LARGE_DIMENSION)
    		item.setSelected(true);
    	item.setEnabled(iconsEnabled);
    	item.setActionCommand(ACT_LARGE_ICONS);
    	item.addActionListener(this);
    	popup.add(item);
    	sizeGroup.add(item);

    	item = new JRadioButtonMenuItem(MEDIUM_LABEL);
    	if (iconSize == MEDIUM_DIMENSION)
    		item.setSelected(true);
    	item.setEnabled(iconsEnabled);
    	item.setActionCommand(ACT_MEDIUM_ICONS);
    	item.addActionListener(this);
    	popup.add(item);
    	sizeGroup.add(item);

    	item = new JRadioButtonMenuItem(SMALL_LABEL);
    	if (iconSize == SMALL_DIMENSION)
    		item.setSelected(true);
    	item.setEnabled(iconsEnabled);
    	item.setActionCommand(ACT_SMALL_ICONS);
    	item.addActionListener(this);
    	popup.add(item);
    	sizeGroup.add(item);

    	popup.addSeparator();

    	// easy way to display the toolbar prefs
    	item = new JMenuItem(LBL_TB_EDITOR);
    	item.setActionCommand(ACT_SHOW_PREF);
    	item.addActionListener(this);
    	popup.add(item);

    	return popupListener;
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
    private JButton buildToolbarButton(String action, int size, int style) {
    	// grab the xml action attributes: 0 = icon path, 1 = tool tip, 
    	// 2 = action  
    	String[] data = cachedActions.get(action);

    	if (data == null)
    		return null;
    	
    	// handle missing mcv icons. the return of Amigo Mono!
    	if (data[0] == null)
    		data[0] = "/edu/wisc/ssec/mcidasv/resources/icons/range-bearing%d.png";    	

    	// take advantage of sprintf-style functionality for creating the path
    	// to the appropriate icon (given the user-specified icon dimensions).
    	String str = String.format(data[0], size);
    	URL tmp = getClass().getResource(str);

    	JButton button;

    	if (style == TOOLBAR_BOTH) {
    		button = new JButton(data[1], new ImageIcon(tmp));
    		button.setVerticalTextPosition(AbstractButton.BOTTOM);
    		button.setHorizontalTextPosition(AbstractButton.CENTER);
    	}
    	else if (style == TOOLBAR_LABELS) {
    		button = new JButton(data[1]);
    		button.setVerticalTextPosition(AbstractButton.BOTTOM);
    		button.setHorizontalTextPosition(AbstractButton.CENTER);
    	}
    	else {
    		button = new JButton(new ImageIcon(tmp));
    	}
		//JButton button = new JButton(new ImageIcon(tmp));

		// the IDV will take care of action handling! so nice!
		button.addActionListener(idv);
		button.setActionCommand(data[2]);
		button.addMouseListener(toolbarMenu);
		button.setToolTipText(data[1]);

		return button;
    }
    
    /**
     * <p>Overrides the IDV's getToolbarUI so that McV can return its own toolbar
     * and not deal with the way the IDV handles toolbars. This method also 
     * updates the toolbar data member so that other methods can fool around 
     * with whatever the IDV thinks is a toolbar (without having to rely on the
     * IDV window manager code).</p>
     * 
     * <p>Not that the IDV code is bad of course--I just can't handle that pause
     * while the toolbar is rebuilt!</p>
     * 
     * @return A new toolbar based on the contents of toolbar.xml.
     */
    @Override
    public JComponent getToolbarUI() {
    	JToolBar toolbar = new JToolBar();

    	populateToolbar(toolbar);

    	this.toolbar = toolbar;

        return toolbar;
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

    	Integer iconSize = 
    		new Integer(getStateManager().getPreferenceOrProperty(PROP_ICON_SIZE, DEFAULT_ICON_SIZE));

    	Integer style = 
    		new Integer(getStateManager().getPreferenceOrProperty(PROP_ICON_LABEL, DEFAULT_TOOLBAR_STYLE));    	

    	// add the actions that should appear in the toolbar.
    	for (String action : cachedButtons) {

    		// null actions are considered separators.
    		if (action == null) {
    			toolbar.addSeparator();
    		}
    		
    		// otherwise we've got a button to add
    		else {
    			JButton b = buildToolbarButton(action, iconSize, style);
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
     * <p>Builds two things, given a toolbar and a tree node: a JButton that 
     * represents a "first-level" parent node and a JPopupMenu that appears 
     * upon clicking the JButton. The button is then added to the given 
     * toolbar.</p>
     * 
     * <p>"First-level" means the given node is a child of the root node.</p>
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
     * Writes the currently displayed toolbar buttons to the toolbar XML.
     * This has mostly been ripped off from ToolbarEditor. :(
     */
    public void writeToolbar() {
    	XmlResourceCollection resources = getResourceManager().getXmlResources(
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
    			e.setAttribute(XmlUi.ATTR_ACTION, (actionPrefix+action));
    		}
    		else {
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
    			data.add(XmlUtil.getAttribute(child, ATTR_ACTION, (String)null).substring(7));
    		else
    			data.add(null);
    	}

    	return data;
    }
    
    /**
     * <p>Read the files that contain our available actions and build a nice
     * hash table that keeps them in memory. The format of the table matches 
     * the XML pretty closely.</p>
     * 
     * <p>XML example:<pre>
     * &lt;action id="show.dashboard"
     *  image="/edu/wisc/ssec/mcidasv/resources/icons/show-dashboard16.png"
	 *  description="Show data explorer"
	 *  action="jython:idv.getIdvUIManager().showDashboard();"/&gt;
     * </pre></p>
     * 
     * <p>This would result in a hash table item with the key "show.dashboard"
     * and an array of three Strings which contains the "image", "description",
     * and "action" attributes.</p>
     * 
     * @return A hash table containing all available actions.
     */
    public Hashtable<String, String[]> readActions() {
    	Hashtable<String, String[]> actionMap = 
    		new Hashtable<String, String[]>();

    	// grab the files that store our actions
    	XmlResourceCollection xrc = getResourceManager().getXmlResources(
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
     * <p>Builds a tree out of the bundles that should appear within the McV 
     * toolbar. A tree is a nice way to store this data, as the default IDV 
     * behavior is to act kinda like a file explorer when it displays these
     * bundles.</p> 
     * 
     * <p>The tree makes it REALLY easy to replicate the default IDV 
     * functionality.</p>
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
    		if (categories != null && categories.size() > 0 && categories.get(0).equals(TOOLBAR) == false)
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
     * where that tree annoyance stuff comes in handy. This basically a simple
     * tree traversal situation.
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
    	initDone = true;
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
    	
    	if (!initDone) {
//    		System.err.println("init not done, no dashboard");
    		return;
    		
    	} else if (dashboard == null) {
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
     * Calling this will use the contents of buttonIds to repopulate the data
     * that describes which buttons should appear in the toolbar.
     * 
     * @param buttonIds The actions that need buttons in the toolbar.
     */
    public void setCurrentToolbar(List<String> buttonIds) {
    	// REPLACE!
    	cachedButtons = buttonIds;
    	
    	// HIDE!
    	toolbar.setVisible(false);

    	// TRICKS!
    	populateToolbar(toolbar);
    	
    	// SHOW!
    	toolbar.setVisible(true);
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
	        				(ControlDescriptor) theObject);
	        	}
	        });
        }
        return mi;
    }

    /* (non-javadoc)
     * Overridden so that the toolbar will update upon saving a bundle.
     */
    @Override
    public void displayTemplatesChanged() {
    	super.displayTemplatesChanged();
    	toolbar.setVisible(false);
    	populateToolbar(toolbar);
    	toolbar.setVisible(true);
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
        	new JLabel("<html>This form allows you to send a support request to the McIDAS Help Desk.<br></html>");

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
                extra.append(idv.getPluginManager().getPluginHtml());
                extra.append(getResourceManager().getHtmlView());

                entriesToPost.add(new HttpFormEntry("form_data[att_one]",
                        "extra.html", extra.toString().getBytes()));

                if (includeBundleCbx.isSelected()) {
                    entriesToPost.add(
                        new HttpFormEntry(
                            "form_data[att_two]", "bundle.xidv",
                            idv.getPersistenceManager().getBundleXml(
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
    
    protected IdvXmlUi doMakeIdvXmlUi(IdvWindow window, List viewManagers, Element skinRoot) {
    	return new McIDASVXmlUi(window, viewManagers, idv, skinRoot);
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
    	 * @param name The name of this node. For a parent node with "Toolbar>cat"
    	 * as the path, the name parameter would contain only "cat."
    	 */
    	public BundleTreeNode(String name) {
    		this(name, null);
    	}
    	
    	/**
    	 * Nodes constructed using this constructor can only ever be child nodes.
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
}