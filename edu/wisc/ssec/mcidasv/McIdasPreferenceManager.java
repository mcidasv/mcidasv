package edu.wisc.ssec.mcidasv;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataUtil;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.IdvConstants;
import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.JythonManager;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.CheckboxCategoryPanel;
import ucar.unidata.ui.FontSelector;
import ucar.unidata.ui.HelpTipDialog;
import ucar.unidata.ui.XmlUi;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlUtil;
import visad.DateTime;
import visad.Unit;

/**
 * <p>An extension of {@link ucar.unidata.idv.IdvPreferenceManager} that uses
 * a JList instead of tabs to lay out the various PreferenceManagers.</p>
 *
 * @author McIDAS-V Dev Team
 */
public class McIdasPreferenceManager extends IdvPreferenceManager 
implements ListSelectionListener {

	/** 
	 * Maps the "name" of a panel to the actual thing holding the 
	 * PreferenceManager. 
	 */
	private Hashtable<String, Container> prefMap = 
		new Hashtable<String, Container>();

	/** Maps the name of a panel to an icon. */
	private Hashtable<String, URL> iconMap = new Hashtable<String, URL>();

	/** 
	 * A list of the different preference managers that'll wind up in the
	 * list.
	 */
	private List<PreferenceManager> managers = 
		new ArrayList<PreferenceManager>();
	
    /**
     * Each PreferenceManager has associated data contained in this list.
     * TODO: bug Unidata about getting IdvPreferenceManager's dataList protected
     */
	private List<Object> dataList = new ArrayList<Object>();
	
	/** 
	 * The list that'll contain all the names of the different 
	 * PreferenceManagers 
	 */
	private JList labelList;
	
    // TODO: figure out why Unidata has this guy as its own data member
    private PreferenceManager navManager;	
	
	/** The "M" in the MVC for JLists. Contains all the list data. */
	private DefaultListModel listModel;
	
	/** Handle scrolling like a pro. */
	private JScrollPane listScrollPane;
	
	/** I hate JSplitPane, but it seems like the right choice here. */
	private JSplitPane splitPane;
	
	/** Holds splitPane. */
	private JPanel paneHolder;
	
	/** Holds paneHolder. Ugh. */
	private JPanel pane;

    /** Date formats */
    private String[] dateFormats = {
        DEFAULT_DATE_FORMAT, "MM/dd/yy HH:mm z", "dd.MM.yy HH:mm z",
        "yyyy-MM-dd", "EEE, MMM dd yyyy HH:mm z", "HH:mm:ss", "HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ssZ"
    };	

    /** test value for formatting */
    private static double latlonValue = -104.56284;

    /** Decimal format */
    private static DecimalFormat latlonFormat = new DecimalFormat();    
    
    /** Provide some default values for the lat-lon preference drop down. */
    private static List<String> defaultLatLonFormats = new ArrayList<String>();
    static {
    	defaultLatLonFormats.add("##0");
    	defaultLatLonFormats.add("##0.0");
    	defaultLatLonFormats.add("##0.0#");
    	defaultLatLonFormats.add("##0.0##");
    	defaultLatLonFormats.add("0.0");
    	defaultLatLonFormats.add("0.00");
    	defaultLatLonFormats.add("0.000");
    }
    
    /** 
     * Replacing the "incoming" IDV preference tab names with whatever's in
     * this map.
     */
    private static Hashtable<String, String> replaceMap = 
    	new Hashtable<String, String>();    
    static {
    	replaceMap.put("Toolbar", Constants.PREF_LIST_TOOLBAR);
    }    
    
    /** Path to the McV choosers.xml */
    private static final String MCV_CHOOSERS = 
    	"/edu/wisc/ssec/mcidasv/resources/choosers.xml";

    public static final String[][] PREF_PANELS = {
    	{Constants.PREF_LIST_GENERAL, "/edu/wisc/ssec/mcidasv/resources/icons/mcidasv-round32.png"},
    	{Constants.PREF_LIST_VIEW, "/edu/wisc/ssec/mcidasv/resources/icons/tab-new32.png"},
    	{Constants.PREF_LIST_TOOLBAR, "/edu/wisc/ssec/mcidasv/resources/icons/applications-accessories32.png"},
    	{Constants.PREF_LIST_DATA_CHOOSERS, "/edu/wisc/ssec/mcidasv/resources/icons/preferences-desktop-remote-desktop32.png"},
    	{Constants.PREF_LIST_ADDE_SERVERS, "/edu/wisc/ssec/mcidasv/resources/icons/applications-internet32.png"},
    	{Constants.PREF_LIST_AVAILABLE_DISPLAYS, "/edu/wisc/ssec/mcidasv/resources/icons/video-display32.png"},
    	{Constants.PREF_LIST_NAV_CONTROLS, "/edu/wisc/ssec/mcidasv/resources/icons/input-mouse32.png"},
    	{Constants.PREF_LIST_FORMATS_DATA,"/edu/wisc/ssec/mcidasv/resources/icons/preferences-desktop-theme32.png"},
    	{Constants.PREF_LIST_ADVANCED, "/edu/wisc/ssec/mcidasv/resources/icons/applications-internet32.png"},
    };    
    
	/**
	 * Prep as much as possible for displaying the preference window: load up
	 * icons and create some of the window features.
	 * 
	 * @param idv Reference to the supreme IDV object.
	 */
    public McIdasPreferenceManager(IntegratedDataViewer idv) {
        super(idv);
        init();

     	for (int i = 0; i < PREF_PANELS.length; i++)
     		iconMap.put(PREF_PANELS[i][0], getClass().getResource(PREF_PANELS[i][1]));       
    }
    	
	/** Desired rendering hints with their desired values. */
	public static final Object[][] RENDER_HINTS = {
		{RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON},
		{RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY},
		{RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON}
	};    

    /**
	 * @return The rendering hints to use, as determined by RENDER_HINTS.
	 */
	public static RenderingHints getRenderingHints() {
		RenderingHints hints = new RenderingHints(null);
		for (int i = 0; i < RENDER_HINTS.length; i++)
			hints.put(RENDER_HINTS[i][0], RENDER_HINTS[i][1]);
		return hints;
	}	
	
    /**
     * Prepare the JList portion of the preference dialog for display.
     */
    private void initPane() {    	
    	listModel = new DefaultListModel();
    	labelList = new JList(listModel);
    	labelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		labelList.setCellRenderer(new IconCellRenderer());
		labelList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {
					splitPane.setRightComponent(getSelectedPanel());
				}
			}
		});
		listScrollPane = new JScrollPane(labelList);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		
		splitPane.setResizeWeight(0.0);
		splitPane.setLeftComponent(listScrollPane);
		
		// need something more reliable than MAGICAL DIMENSIONS.
		listScrollPane.setMinimumSize(new Dimension(166, 319));
		
		pane = new JPanel(new BorderLayout());
		pane.add(splitPane, BorderLayout.CENTER);
		paneHolder.add(pane, BorderLayout.WEST);
    }
        
    /**
     * Add a PreferenceManager to the list of things that should be shown in
     * the preference dialog.
     * 
     * @param tabLabel The label (or name) of the PreferenceManager.
     * @param description Not used.
     * @param listener The actual PreferenceManager.
     * @param panel The container holding all of the PreferenceManager stuff.
     * @param data Data passed to the preference manager.
     */
    public void add(String tabLabel, String description, 
    	PreferenceManager listener, Container panel, Object data) {    	
    	
    	// if there is an alternate name for tabLabel, find and use it.
    	if (replaceMap.containsKey(tabLabel) == true)
    		tabLabel = replaceMap.get(tabLabel);
    	
    	if (prefMap.containsKey(tabLabel) == true)
    		return;
    	
    	panel.setPreferredSize(null);
    	
    	managers.add(listener);
    	dataList.add(data);
    	
    	prefMap.put(tabLabel, panel);
     	if (pane == null)
     		initPane();
     	     	
     	JLabel label = new JLabel();
     	label.setText(tabLabel);
     	label.setIcon(new ImageIcon(iconMap.get(tabLabel)));
     	listModel.addElement(label);
     	
     	labelList.setSelectedIndex(0);
     	splitPane.setRightComponent(prefMap.get(Constants.PREF_LIST_GENERAL));
     	splitPane.setPreferredSize(new Dimension(900, 600)); //FIXME: MAGIC DIMENSIONS = WHACK WITH CLUESTICK     	
	}
    
    /**
     * Apply the preferences (taken straight from IDV). 
     * TODO: bug Unidata about making managers and dataList protected instead of private
     * 
     * @return Whether or not each of the preference managers applied properly.
     */
    public boolean apply() {
        try {
            for (int i = 0; i < managers.size(); i++) {
                PreferenceManager manager =
                    (PreferenceManager) managers.get(i);
                manager.applyPreference(getStore(), dataList.get(i));
            }        	
            getStore().save();
            return true;
        } catch (Exception exc) {
            LogUtil.logException("Error applying preferences", exc);
            return false;
        }
    }    
    
    /**
     * Select a list item and its corresponding panel that both live within the 
     * preference window JList.
     * 
     * @param labelName The "name" of the JLabel within the JList.
     */
    public void selectListItem(String labelName) {    	
    	show();
    	toFront();
    	
    	if (pane == null)
    		return;
    	
    	for (int i = 0; i < listModel.getSize(); i++) {
    		String labelText = ((JLabel)listModel.get(i)).getText();
    		if (StringUtil.stringMatch(labelText, labelName)) {
    			labelList.setSelectedIndex(i);
    			return;
    		}
    	}
    }
    
    /**
     * Wrapper so that IDV code can still select which preference pane to show.
     * 
     * @param tabNameToShow The name of the pane to be shown. Regular
     * expressions are supported.
     */
    public void showTab(String tabNameToShow) {
    	selectListItem(tabNameToShow);
    }
    
    /**
     * Handle the user clicking around.
     * 
     * @param e The event to be handled! Use your imagination!
     */
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {
			splitPane.setRightComponent(getSelectedPanel());
		}
	}
    
	/**
	 * Returns the container the corresponds to the currently selected label in
	 * the JList.
	 * 
	 * @return The current container.
	 */
	private Container getSelectedPanel() {
		String key = ((JLabel)listModel.getElementAt(labelList.getSelectedIndex())).getText();
		return prefMap.get(key);
	}
        
    /**
     * Perform the GUI initialization for the preference dialog.
     */
    public void init() {
    	paneHolder = new JPanel(new BorderLayout());
        Component buttons = GuiUtils.makeApplyOkHelpCancelButtons(this);
        contents = new JPanel(new BorderLayout());
        contents.add(paneHolder, BorderLayout.CENTER);
        contents.add(buttons, BorderLayout.AFTER_LAST_LINE);
//        contents = GuiUtils.centerBottom(paneHolder, buttons);
    }
        
    /**
     * Initialize the preference dialog. Leave most of the heavy lifting to
     * the IDV, except for creating Gail's server manager.
     */
    protected void initPreferences() {
    	//super.initPreferences();
    	navManager = new PreferenceManager() {
    		public void applyPreference(XmlObjectStore theStore, Object data) {}    		
    	};
    	
    	// 01 General/McIDAS-V
    	addMcVPreferences();
    	
    	// 02 View/Display Window
    	addDisplayWindowPreferences();
        
        // 03 Toolbar/Toolbar Options
        getIdv().getIdvUIManager().addToolbarPreferences(this);
        
        // 04 Available Choosers/Data Sources
        addChooserPreferences();

        // 05 ADDE Servers
        ServerPreferenceManager mspm = new ServerPreferenceManager(getIdv());
        mspm.addServerPreferences(this);
        
        // 06 Available Displays/Display Types
        addDisplayPreferences();    	
    	
        // 07 Navigation/Navigation Controls
        this.add(Constants.PREF_LIST_NAV_CONTROLS, "", navManager, makeEventPanel(),
                 new Hashtable());
                
        // 08 Formats & Data
        addFormatDataPreferences();
        
        // 09 Advanced
        // TODO!
        addAdvancedPreferences();
    }
    
    public void addAdvancedPreferences() {
    	Hashtable<String, Component> widgets = new Hashtable<String, Component>();
    	List<Component> stuff = new ArrayList<Component>();
    	
    	IdvObjectStore store = getStore();
    	      	
    	final JTextField maxHeap = new JTextField(store.get("java.vm.heapsize", ""), 10);
    	final JTextField initHeap = new JTextField(store.get("java.vm.initialheap", ""), 10);
    	final JTextField threadStack = new JTextField(store.get("java.vm.threadstack", ""), 10);
    	final JTextField youngGen = new JTextField(store.get("java.vm.younggeneration", ""), 10);
    	final JTextField mcxMem = new JTextField(store.get("mcx.allocmem", ""), 10);
    	final JTextField mcxDir = new JTextField(store.get("mcx.workingdir", ""), 10);
    	final JTextField collabHost = new JTextField(store.get("idv.collabhost", ""), 10);
    	final JTextField collabPort = new JTextField(store.get("idv.collabport", ""), 10);
    	final JTextField jythonEditorField =
            new JTextField(getStateManager().getPreferenceOrProperty(JythonManager.PROP_JYTHON_EDITOR,""), 10);
    	final JTextField runMcv = new JTextField(store.get("mcv.runpath", ""), 10);
    	
    	final JCheckBox enableSched = new JCheckBox("Enable Scheduler", store.get("mcx.enablescheduler", true));
    	final JCheckBox caseInvert = new JCheckBox("Invert Case", store.get("mcx.caseinvert", false));
    	final JCheckBox enableCollab = new JCheckBox("Act as Collaboration Server", store.get("idv.collabmode", false));
    	final JCheckBox enableDebug = new JCheckBox("Enable Debugging", store.get("idv.enabledebug", false));
    	//final JCheckBox showMsgs = new JCheckBox("Show Debug Messages", store.get("idv.debugmsgs", false));
    	    	
    	widgets.put("java.vm.heapsize", maxHeap);
    	widgets.put("java.vm.initialheap", initHeap);
    	widgets.put("java.vm.threadstack", threadStack);
    	widgets.put("java.vm.younggeneration", youngGen);
    	widgets.put("mcx.allocmem", mcxMem);
    	widgets.put("mcx.workingdir", mcxDir);
    	widgets.put("mcx.enablescheduler", enableSched);
    	widgets.put("mcx.caseinvert", caseInvert);
    	widgets.put("idv.collabmode", enableCollab);
    	widgets.put("idv.collabhost", collabHost);
    	widgets.put("idv.collabport", collabPort);
    	widgets.put("idv.enabledebug", enableDebug);
    	//widgets.put("idv.debugmsgs", showMsgs);
    	widgets.put("mcv.runpath", runMcv);

    	widgets.put(JythonManager.PROP_JYTHON_EDITOR, jythonEditorField);    	
/*
   	JPanel fontPanel =
                GuiUtils.vbox(GuiUtils.lLabel("Layer List Properties:"),
                              GuiUtils.doLayout(new Component[] {
                                  GuiUtils.rLabel("   Font:"),
                                  GuiUtils.left(fontSelector.getComponent()),
                                  GuiUtils.rLabel("  Color:"),
                                  GuiUtils.left(GuiUtils.hbox(dlColorWidget,
                                      dlColorWidget.getSetButton(),
                                      dlColorWidget.getClearButton(), 5)) }, 2,
                                          GuiUtils.WT_N, GuiUtils.WT_N));

    	
    	JPanel javaPanel = GuiUtils.doLayout(GuiUtils.vbox(new Component[] {
    		GuiUtils.lLabel("Java VM:"),
    		GuiUtils.lLabel("  Maximum Heap Size:"), 
    		GuiUtils.right(maxHeap),
    		new JLabel("Initial Heap Size:"), initHeap,
    		new JLabel("Thread Stack Size:"), threadStack,
    		new JLabel("Young Generation Heap Size:"), youngGen,
    	}), 2, GuiUtils.WT_N, GuiUtils.WT_N);*/
    	
    	JPanel javaPanel = GuiUtils.vbox(
    		GuiUtils.lLabel("Java VM:"),
    		GuiUtils.doLayout(new Component[] {
    			GuiUtils.rLabel("  Maximum Heap Size:"),
    			GuiUtils.left(maxHeap),
    			GuiUtils.rLabel("  Initial Heap Size:"),
    			GuiUtils.left(initHeap),
    			GuiUtils.rLabel("  Thread Stack Size:"),
    			GuiUtils.left(threadStack),
    			GuiUtils.rLabel("  Young Generation Size:"),
    			GuiUtils.left(youngGen)
    		}, 2, GuiUtils.WT_N, GuiUtils.WT_N));
    	
    	JPanel mcxPanel = GuiUtils.vbox(
    		GuiUtils.lLabel("McIDAS-X Options:"),
    		GuiUtils.doLayout(new Component[] {
    			GuiUtils.left(enableSched),
    			GuiUtils.left(caseInvert),
    			GuiUtils.rLabel("  Working Directory:"),
    			GuiUtils.left(mcxDir),
    			GuiUtils.rLabel("  Memory Allocation:"),
    			GuiUtils.left(mcxMem),
    		}, 2, GuiUtils.WT_N, GuiUtils.WT_N));
	
    	JPanel idvPanel = GuiUtils.vbox(
    		GuiUtils.lLabel("McIDAS-V Options:"),
    		GuiUtils.doLayout(new Component[] {
    			GuiUtils.rLabel("  External Editor:"),
    			GuiUtils.left(GuiUtils.centerRight(jythonEditorField, GuiUtils.makeFileBrowseButton(jythonEditorField))),
    			GuiUtils.rLabel("  Path to runMcV:"),
    			GuiUtils.left(GuiUtils.centerRight(runMcv, GuiUtils.makeFileBrowseButton(runMcv))),
    			GuiUtils.left(enableDebug),
    			//GuiUtils.left(showMsgs),
    			GuiUtils.left(enableCollab),
    			GuiUtils.rLabel("  Collaboration Host:"),
    			GuiUtils.left(collabHost),
    			GuiUtils.rLabel("  Collaboration Port:"),
    			GuiUtils.left(collabPort)    			
    		}, 2, GuiUtils.WT_N, GuiUtils.WT_N)
    	);
 
    	stuff.add(javaPanel);
    	stuff.add(mcxPanel);
    	stuff.add(idvPanel);
 
    	JPanel advancedPrefs = GuiUtils.inset(GuiUtils.topLeft(GuiUtils.doLayout(stuff, 1, GuiUtils.WT_N, GuiUtils.WT_N)), 5);
 
    	PreferenceManager advancedManager = new PreferenceManager() {
    		public void applyPreference(XmlObjectStore theStore, Object data) {
    			IdvPreferenceManager.applyWidgets((Hashtable)data, theStore);
    			
    			theStore.put("java.vm.heapsize", maxHeap.getText());
    			theStore.put("java.vm.initialheap", initHeap.getText());
    			theStore.put("java.vm.threadstack", threadStack.getText());
    			theStore.put("java.vm.younggeneration", youngGen.getText());
    			theStore.put("mcx.allocmem", mcxMem.getText());
    			theStore.put("mcx.workingdir", mcxDir.getText());
    			theStore.put("mcx.enablescheduler", enableSched.isSelected());
    			theStore.put("mcx.caseinvert", caseInvert.isSelected());
    			theStore.put("idv.collabmode", enableCollab.isSelected());
    			theStore.put("idv.collabhost", collabHost.getText());
    			theStore.put("idv.collabport", collabPort.getText());
    			theStore.put("idv.enabledebug", enableDebug.isSelected());
    			//theStore.put("idv.debugmsgs", showMsgs.isSelected());
    			theStore.put("mcv.runpath", runMcv.getText());
    			theStore.put(JythonManager.PROP_JYTHON_EDITOR, jythonEditorField.getText());
    			
    			alterRunScript();
    		}
    		
    		
    	};
    	
    	this.add("Advanced", "complicated stuff dude", advancedManager, GuiUtils.topCenter(GuiUtils.top(advancedPrefs), new JPanel()), new Hashtable());
    }
    
    private void alterRunScript() {
    	Pattern heapSize = Pattern.compile("^HEAP_SIZE=.+$", Pattern.MULTILINE);
    	Pattern javaOpts = Pattern.compile("^JAVA_FLAGS=.+$", Pattern.MULTILINE);
    	Pattern mcvOpts = Pattern.compile("^MCV_FLAGS.+$", Pattern.MULTILINE);
    	
    	File script = new File(getStore().get("mcv.runpath", ""));
    	
    	StringBuffer buffer = new StringBuffer();
    	String line;
    	
    	IdvObjectStore store = getStore();
    	
    	if (script.getPath().equals(""))
    		return;
    	
    	try {
    		BufferedReader br = new BufferedReader(new FileReader(script));
    		
    		while ((line = br.readLine()) != null)
    			buffer.append(line + "\n");
    		
    		StringBuffer mcvFlags = new StringBuffer("MCV_FLAGS=");
    		StringBuffer javaFlags = new StringBuffer("JAVA_FLAGS=");
    		StringBuffer heapFlag = new StringBuffer("HEAP_SIZE=");
    		
			String maxHeapSize = store.get("java.vm.heapsize", "");
			String initHeap = store.get("java.vm.initialheap", "");
			String threadSize = store.get("java.vm.threadstack", "");
			String youngGen = store.get("java.vm.younggeneration", "");
			String mcxMem = store.get("mcx.allocmem", "");
			String mcxDir = store.get("mcx.workingdir", "");
			boolean enableSched = store.get("mcx.enablescheduler", true);
			boolean invertCase = store.get("mcx.caseinvert", true);
			boolean collabMode = store.get("idv.collabmode", false);
			String collabHost = store.get("idv.collabhost", "");
			String collabPort = store.get("idv.collabport", "");
			boolean enableDebug = store.get("idv.enabledebug", false);
			//boolean enableMsgs = store.get("idv.debugmsgs", false);			
    		
    		if (maxHeapSize.equals("") == false)
    			heapFlag.append(maxHeapSize);
    		
    		if (initHeap.equals("") == false)
    			javaFlags.append("-Xms" + initHeap + " ");
    		if (threadSize.equals("") == false)
    			javaFlags.append("-XX:ThreadStackSize=" + threadSize + " ");
    		if (youngGen.equals("") == false)
    			javaFlags.append("-XX:NewSize=" + youngGen + " ");
    		
    		if (collabMode == true)
    			mcvFlags.append("-server ");
    		
    		if (collabHost.equals("") == false && collabPort.equals("") == false)
    			mcvFlags.append("-connect " + collabHost + " -port " + collabPort + " ");
    		
    		if (enableDebug == true)
    			mcvFlags.append("-debug ");
    		
    		String contents = buffer.toString();
    		
    		Matcher matcher = heapSize.matcher(contents);
    		contents = matcher.replaceAll(heapFlag.toString());
    		
    		matcher = javaOpts.matcher(contents);
    		contents = matcher.replaceAll(javaFlags.toString());
    		
    		matcher = mcvOpts.matcher(contents);
    		contents = matcher.replaceAll(mcvFlags.toString());
    		    		
    		br.close();
    		
            BufferedWriter out = new BufferedWriter(new FileWriter(script));
            out.write(contents);
            out.close();    		
    		
    	} catch (IOException e) {
    		e.printStackTrace();    		
    	}
    }
    
    /**
     * Add in the user preference tab for the controls to show
     */
    protected void addDisplayPreferences() {
        cbxToCdMap = new Hashtable<JCheckBox, ControlDescriptor>();
        List<JPanel> compList = new ArrayList<JPanel>();
        List<ControlDescriptor> controlDescriptors = getIdv().getAllControlDescriptors();
        
        final List<CheckboxCategoryPanel> catPanels = 
        	new ArrayList<CheckboxCategoryPanel>();
        
        Hashtable<String, CheckboxCategoryPanel> catMap = 
        	new Hashtable<String, CheckboxCategoryPanel>();
        
        for (ControlDescriptor cd : controlDescriptors) {
            
            String displayCategory = cd.getDisplayCategory();
            
            CheckboxCategoryPanel catPanel =
                (CheckboxCategoryPanel) catMap.get(displayCategory);
            
            if (catPanel == null) {
                catPanel = new CheckboxCategoryPanel(displayCategory, false);
                catPanels.add(catPanel);
                catMap.put(displayCategory, catPanel);
                compList.add(catPanel.getTopPanel());
                compList.add(catPanel);
            }

            JCheckBox cbx = new JCheckBox(cd.getLabel(),
                                          shouldShowControl(cd, true));
            cbx.setToolTipText(cd.getDescription());
            cbxToCdMap.put(cbx, cd);            
            catPanel.addItem(cbx);
            catPanel.add(GuiUtils.inset(cbx, new Insets(0, 20, 0, 0)));
        }

        for (CheckboxCategoryPanel cbcp : catPanels)
        	cbcp.checkVisCbx();

        final JButton allOn = new JButton("All on");
        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
            	for (CheckboxCategoryPanel cbcp : catPanels)
            		cbcp.toggleAll(true);
            }
        });
        final JButton allOff = new JButton("All off");
        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (CheckboxCategoryPanel cbcp : catPanels) 
                	cbcp.toggleAll(false);
            }
        });

        Boolean controlsAll =
            (Boolean) getIdv().getPreference(PROP_CONTROLDESCRIPTORS_ALL,
                                             Boolean.TRUE);
        final JRadioButton useAllBtn = new JRadioButton("Use all displays",
                                           controlsAll.booleanValue());
        final JRadioButton useTheseBtn =
            new JRadioButton("Use selected displays:",
                             !controlsAll.booleanValue());
        GuiUtils.buttonGroup(useAllBtn, useTheseBtn);

        final JPanel cbPanel = GuiUtils.top(GuiUtils.vbox(compList));

        JScrollPane cbScroller = new JScrollPane(cbPanel);
        cbScroller.getVerticalScrollBar().setUnitIncrement(10);
        cbScroller.setPreferredSize(new Dimension(300, 300));
        
        JComponent exportComp =
            GuiUtils.right(GuiUtils.makeButton("Export to Plugin", this,
                "exportControlsToPlugin"));
        
        JComponent cbComp = GuiUtils.centerBottom(cbScroller, exportComp);
        
        JPanel bottomPanel =
            GuiUtils.leftCenter(
                GuiUtils.inset(
                    GuiUtils.top(GuiUtils.vbox(allOn, allOff)),
                    4), new Msg.SkipPanel(
                        GuiUtils.hgrid(
                            Misc.newList(cbComp, GuiUtils.filler()), 0)));

        JPanel controlsPanel =
            GuiUtils.inset(GuiUtils.topCenter(GuiUtils.hbox(useAllBtn,
                useTheseBtn), bottomPanel), 6);
        
        GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
        useAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
                
                allOn.setEnabled(!useAllBtn.isSelected());
                
                allOff.setEnabled(!useAllBtn.isSelected());
            }
        });
        
        useTheseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
                
                allOn.setEnabled(!useAllBtn.isSelected());
                
                allOff.setEnabled(!useAllBtn.isSelected());
            }
        });

        GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
        
        allOn.setEnabled(!useAllBtn.isSelected());
        
        allOff.setEnabled(!useAllBtn.isSelected());

        PreferenceManager controlsManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
                controlDescriptorsToShow = new Hashtable();
                
                Hashtable<JCheckBox, ControlDescriptor> table = (Hashtable)data;
                
                List<ControlDescriptor> controlDescriptors = getIdv().getAllControlDescriptors();
                
                for (Enumeration keys =
                        table.keys(); keys.hasMoreElements(); ) {
                    JCheckBox         cbx = (JCheckBox) keys.nextElement();
                    
                    ControlDescriptor cd  =
                        (ControlDescriptor) table.get(cbx);
                    
                    controlDescriptorsToShow.put(cd.getControlId(),
                            new Boolean(cbx.isSelected()));
                }
                
                showAllControls = useAllBtn.isSelected();
                
                theStore.put(PROP_CONTROLDESCRIPTORS, controlDescriptorsToShow);
                
                theStore.put(PROP_CONTROLDESCRIPTORS_ALL,
                             new Boolean(showAllControls));
            }
        };
        
        this.add(Constants.PREF_LIST_AVAILABLE_DISPLAYS,
                 "What displays should be available in the user interface?",
                 controlsManager, controlsPanel, cbxToCdMap);
    }    
     
    protected void addDisplayWindowPreferences() {
    	// oh man this seems like a really bad idea.
    	
    	Hashtable<String, JCheckBox> widgets = new Hashtable<String, JCheckBox>();
    	MapViewManager mappy = new MapViewManager(getIdv());
    	
    	final JComponent[] bgComps =
    		GuiUtils.makeColorSwatchWidget(getStore().get(MapViewManager.PREF_BGCOLOR,
    			mappy.getBackground()), "Set Background Color");

    	final JComponent[] fgComps =
    		GuiUtils.makeColorSwatchWidget(getStore().get(MapViewManager.PREF_FGCOLOR,
    			mappy.getForeground()), "Set Foreground Color");
            
    	final JComponent[] border = 
    		GuiUtils.makeColorSwatchWidget(getStore().get(MapViewManager.PREF_BORDERCOLOR, 
    			ViewManager.borderHighlightColor), 
            	"Set Selected Panel Border Color");
            
    	GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);

    	JPanel colorPanel = GuiUtils.left(GuiUtils.doLayout(new Component[] {
    		GuiUtils.rLabel("  Background:"), bgComps[0], bgComps[1],
    		GuiUtils.rLabel("  Foreground:"), fgComps[0], fgComps[1],
    		GuiUtils.rLabel("  Selected Panel:"), border[0], border[1],
    	}, 3, GuiUtils.WT_N, GuiUtils.WT_N));

    	colorPanel = GuiUtils.vbox(new JLabel("Color Scheme:"), colorPanel);
        
    	final FontSelector fontSelector = 
    		new FontSelector(FontSelector.COMBOBOX_UI, false, false);
            
    	Font f = getStore().get(MapViewManager.PREF_DISPLAYLISTFONT, mappy.getDisplayListFont());

    	fontSelector.setFont(f);

    	final GuiUtils.ColorSwatch dlColorWidget =
    		new GuiUtils.ColorSwatch(getStore().get(MapViewManager.PREF_DISPLAYLISTCOLOR,
    			mappy.getDisplayListColor()), "Set Display List Color");

    	GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);

    	JPanel fontPanel =
                GuiUtils.vbox(GuiUtils.lLabel("Layer List Properties:"),
                              GuiUtils.doLayout(new Component[] {
                                  GuiUtils.rLabel("   Font:"),
                                  GuiUtils.left(fontSelector.getComponent()),
                                  GuiUtils.rLabel("  Color:"),
                                  GuiUtils.left(GuiUtils.hbox(dlColorWidget,
                                      dlColorWidget.getSetButton(),
                                      dlColorWidget.getClearButton(), 5)) }, 2,
                                          GuiUtils.WT_N, GuiUtils.WT_N));

    	final JComboBox projBox = new JComboBox();
    	GuiUtils.setListData(projBox, mappy.getProjectionList().toArray());
    	Object defaultProj = mappy.getDefaultProjection();

    	if (defaultProj != null)
    		projBox.setSelectedItem(defaultProj);

    	PreferenceManager miscManager = new PreferenceManager() {
    		public void applyPreference(XmlObjectStore theStore, Object data) {
    			IdvPreferenceManager.applyWidgets((Hashtable) data, theStore);
    			theStore.put(MapViewManager.PREF_PROJ_DFLT, projBox.getSelectedItem());
    			theStore.put(MapViewManager.PREF_BGCOLOR, bgComps[0].getBackground());
    			theStore.put(MapViewManager.PREF_FGCOLOR, fgComps[0].getBackground());
    			theStore.put(MapViewManager.PREF_BORDERCOLOR, border[0].getBackground());
    			theStore.put(MapViewManager.PREF_DISPLAYLISTFONT, fontSelector.getFont());
    			theStore.put(MapViewManager.PREF_DISPLAYLISTCOLOR, dlColorWidget.getSwatchColor());
    			ViewManager.setHighlightBorder(border[0].getBackground());                    
    		}
    	};

    	Object[][] miscObjects = {
    		{ "Panel Configuration:", null, null },
    		{ "Show Wireframe Box", MapViewManager.PREF_WIREFRAME, 
    			new Boolean(mappy.getWireframe()) },
    		{ "Show Cursor Readout", MapViewManager.PREF_SHOWCURSOR,
    			new Boolean(mappy.getShowCursor()) },
    		{ "Clip View At Box", MapViewManager.PREF_3DCLIP, new Boolean(mappy.getClipping()) },
    		{ "Show Layer List in Panel", MapViewManager.PREF_SHOWDISPLAYLIST,
    			new Boolean(mappy.getShowDisplayList()) },
    		{ "Show Times In Panel", MapViewManager.PREF_ANIREADOUT,
    			new Boolean(mappy.getAniReadout()) },
    		{ "Show Map Display Scales", MapViewManager.PREF_SHOWSCALES,
    			new Boolean(mappy.getLabelsVisible()) },
    		{ "Show Transect Display Scales", MapViewManager.PREF_SHOWTRANSECTSCALES,
    			new Boolean(mappy.getTransectLabelsVisible()) },
    		{ "Show \"Please Wait\" Message", MapViewManager.PREF_WAITMSG,
    			new Boolean(mappy.getWaitMessageVisible()) },
    		{ "Reset Projection With New Data", MapViewManager.PREF_PROJ_USEFROMDATA },
    		{ "Use 3D View", MapViewManager.PREF_DIMENSION }
    	};

    	Object[][] legendObjects = {
    		{ "Legends:", null, null },
    		{ "Show Side Legend", MapViewManager.PREF_SHOWSIDELEGEND,
    			new Boolean(mappy.getShowSideLegend()) },
    		{ "Show Bottom Legend", MapViewManager.PREF_SHOWBOTTOMLEGEND,
    			new Boolean(mappy.getShowBottomLegend()) },
    		/*{ "Show Animation Boxes", MapViewManager.PREF_SHOWANIMATIONBOXES,
    			new Boolean(mappy.getShowAnimationBoxes()) },
    		{ "Show Overview Map", MapViewManager.PREF_SHOWPIP,
    			new Boolean(getStore().get(MapViewManager.PREF_SHOWPIP, false)) },*/
    	};

    	Object[][] toolbarObjects = {
    		{ "Navigation Toolbars:", null, null },
    		{ "Show Earth Navigation Panel", MapViewManager.PREF_SHOWEARTHNAVPANEL,
    			new Boolean(mappy.getShowEarthNavPanel()) },
    		{ "Show Viewpoint Toolbar", MapViewManager.PREF_SHOWTOOLBAR + "perspective" },
    		{ "Show Zoom/Pan Toolbar", MapViewManager.PREF_SHOWTOOLBAR + "zoompan" },
    		{ "Show Undo/Redo Toolbar", MapViewManager.PREF_SHOWTOOLBAR + "undoredo" }
    	};

    	JPanel miscPanel = IdvPreferenceManager.makePrefPanel(miscObjects,
                                   widgets, getStore());
    	JPanel legendPanel =
    		IdvPreferenceManager.makePrefPanel(legendObjects, widgets, getStore());

    	JPanel toolbarPanel =
    		IdvPreferenceManager.makePrefPanel(toolbarObjects, widgets, getStore());

    	JPanel projPanel =
    		GuiUtils.vbox(GuiUtils.lLabel("Default Projection: "),
    			GuiUtils.left(GuiUtils.inset(projBox, new Insets(5, 20, 0, 0))));

    	JPanel colorFontPanel = 
    		GuiUtils.vbox(GuiUtils.top(colorPanel),
    			GuiUtils.top(fontPanel), GuiUtils.top(projPanel));

    	GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
    	JPanel miscContents =
    		GuiUtils.doLayout(Misc.newList(GuiUtils.top(legendPanel),
    			GuiUtils.top(toolbarPanel),
    			GuiUtils.top(miscPanel),
    			GuiUtils.top(colorFontPanel)), 2,
    			GuiUtils.WT_N, GuiUtils.WT_N);

    	miscContents = GuiUtils.inset(GuiUtils.topLeft(miscContents), 5);
    	this.add(Constants.PREF_LIST_VIEW, "Display Window Preferences",
    		miscManager, miscContents, widgets);
    }
    
    /**
     * Creates and adds the basic preference panel.
     */
    protected void addMcVPreferences() {

        Hashtable<String, Component> widgets = 
        	new Hashtable<String, Component>();
        
        PreferenceManager basicManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
                //getIdv().getArgsManager().sitePathFromArgs = null;
                applyWidgets((Hashtable) data, theStore);
                getIdv().getIdvUIManager().setDateFormat();
                getIdv().initCacheManager();
                applyEventPreferences(theStore);
            }
        };
        
        /*Object[][] prefs1 = {
            { "General:", null },
            { "Show Help Tip Dialog On Start",
              HelpTipDialog.PREF_HELPTIPSHOW },
            { "Confirm Before Exiting", PREF_SHOWQUITCONFIRM },
            { "Show Data Explorer On Start", PREF_SHOWDASHBOARD, Boolean.TRUE },
            { "Dock in Data Explorer:", null },
            { "Quick Links", PREF_EMBEDQUICKLINKSINDASHBOARD, Boolean.FALSE },
            { "Data Sources", PREF_EMBEDDATACHOOSERINDASHBOARD,
              Boolean.TRUE },
            { "Field Selector", PREF_EMBEDFIELDSELECTORINDASHBOARD,
              Boolean.TRUE },
            { "Layer Controls", PREF_CONTROLSINTABS, Boolean.TRUE },
            { "Legends", PREF_EMBEDLEGENDINDASHBOARD, Boolean.FALSE }
        };*/
        Object[][] prefs1 = {
            { "General:", null },
            { "Show Help Tip dialog on start", HelpTipDialog.PREF_HELPTIPSHOW },
            { "Show Data Explorer on start", PREF_SHOWDASHBOARD, Boolean.TRUE },
            { "Check for new version on start", 
                Constants.PREF_VERSION_CHECK, Boolean.TRUE },
            { "Confirm before exiting", PREF_SHOWQUITCONFIRM },
            { "When Layer Control Window is Closed:", null },
            { "Remove the display", DisplayControl.PREF_REMOVEONWINDOWCLOSE,
              Boolean.FALSE },
            { "Remove standalone displays",
              DisplayControl.PREF_STANDALONE_REMOVEONCLOSE, Boolean.FALSE }
        };

        JPanel panel1 = makePrefPanel(prefs1, widgets, getStore());

        Object[][] prefs2 = {
            { "When Opening a Bundle:", null },
            { "Prompt user to remove displays and data", PREF_OPEN_ASK },
            { "Remove all displays and data sources", PREF_OPEN_REMOVE },
            { "Ask where to put zipped data files", PREF_ZIDV_ASK }
        };
        
        JPanel panel2 = makePrefPanel(prefs2, widgets, getStore());

        Object[][] prefs3 = {
            { "Layer Controls:", null },
            { "Show windows when they are created", PREF_SHOWCONTROLWINDOW },
            { "Use Fast Rendering", PREF_FAST_RENDER, Boolean.FALSE,
              "<html>Turn this on for better performance at<br> the risk of having funky displays</html>" },
            { "Auto-select data when loading a template",
              IdvConstants.PREF_AUTOSELECTDATA, Boolean.FALSE,
              "<html>When loading a display template should the data be automatically selected</html>" },
            /*{ "When Layer Control Window is Closed:", null },
            { "Remove the display", DisplayControl.PREF_REMOVEONWINDOWCLOSE,
              Boolean.FALSE },
            { "Remove standalone displays",
              DisplayControl.PREF_STANDALONE_REMOVEONCLOSE, Boolean.FALSE },*/
        };

        JPanel panel3 = makePrefPanel(prefs3, widgets, getStore());

        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);

        JPanel leftPanel = panel1;
        
        JPanel rightPanel = GuiUtils.inset(GuiUtils.vbox(panel2, panel3),
                                           new Insets(0, 40, 0, 0));
        
        List panelComps = Misc.newList(GuiUtils.top(leftPanel),
                                       GuiUtils.top(rightPanel));
        
        JPanel panels = GuiUtils.doLayout(panelComps, 2, GuiUtils.WT_N,
                                          GuiUtils.WT_N);
        
        panels = GuiUtils.inset(panels, new Insets(6, 0, 0, 0));

        JPanel miscContents =
            GuiUtils.inset(GuiUtils.centerBottom(GuiUtils.left(panels),
                null), 5);

        this.add(Constants.PREF_LIST_GENERAL, "General Preferences", basicManager,
                 GuiUtils.topCenter(miscContents, new JPanel()), widgets);
    }
    
    /**
     * Creates and adds the formats and data preference panel.
     */
    protected void addFormatDataPreferences() {
    	Hashtable<String, Component> widgets = new Hashtable<String, Component>();
    	List<Component> formatComps = new ArrayList<Component>();
    	
        JLabel timeLabel = GuiUtils.rLabel("");
        try {
            timeLabel.setText("ex:  " + new DateTime().toString());
        } catch (Exception ve) {
            timeLabel.setText("Can't format date: " + ve);
        }

        String dateFormat = getStore().get(PREF_DATE_FORMAT,
                                           DEFAULT_DATE_FORMAT);
        List formats = Misc.toList(dateFormats);
        if ( !formats.contains(dateFormat))
            formats.add(dateFormat);
        
        final JComboBox dateFormatBox = 
        	GuiUtils.getEditableBox(formats, dateFormat);
        
        widgets.put(PREF_DATE_FORMAT, dateFormatBox);

        final JComboBox timeZoneBox = new JComboBox();
        String timezoneString = getStore().get(PREF_TIMEZONE,
                                    DEFAULT_TIMEZONE);
        String[] zones = TimeZone.getAvailableIDs();
        Arrays.sort(zones);
        GuiUtils.setListData(timeZoneBox, zones);
        timeZoneBox.setSelectedItem(timezoneString);
        Dimension d = timeZoneBox.getPreferredSize();
        timeZoneBox.setPreferredSize(new Dimension((int) (d.width * .6),
                d.height));

        widgets.put(PREF_TIMEZONE, timeZoneBox);

        ObjectListener timeLabelListener = new ObjectListener(timeLabel) {
            public void actionPerformed(ActionEvent ae) {
                JLabel label  = (JLabel) theObject;
                String format = dateFormatBox.getSelectedItem().toString();
                String zone   = timeZoneBox.getSelectedItem().toString();
                try {
                    TimeZone tz = TimeZone.getTimeZone(zone);
                    // hack to make it the DateTime default
                    if (format.equals(DEFAULT_DATE_FORMAT)) {
                        if (zone.equals(DEFAULT_TIMEZONE)) {
                            format = DateTime.DEFAULT_TIME_FORMAT + "'Z'";
                        }
                    }
                    label.setText("ex:  "
                                  + new DateTime().formattedString(format,
                                      tz));
                } catch (Exception ve) {
                    label.setText("Invalid format or time zone");
                    LogUtil.userMessage("Invalid format or time zone");
                }
            }
        };
        dateFormatBox.addActionListener(timeLabelListener);
        timeZoneBox.addActionListener(timeLabelListener);

        String probeFormat =
            getStore().get(DisplayControl.PREF_PROBEFORMAT,
                           DisplayControl.DEFAULT_PROBEFORMAT);
        
        JComboBox probeFormatFld = GuiUtils.getEditableBox(
        	Misc.newList(DisplayControl.DEFAULT_PROBEFORMAT,
        		"%rawvalue% [%rawunit%]", "%value%", "%rawvalue%",
        		"%value% <i>%unit%</i>"), probeFormat);

        widgets.put(DisplayControl.PREF_PROBEFORMAT, probeFormatFld);

        String defaultMode =
            getStore().get(PREF_SAMPLINGMODE,
                           DisplayControlImpl.WEIGHTED_AVERAGE);
        
        JRadioButton wa = new JRadioButton(
                              DisplayControlImpl.WEIGHTED_AVERAGE,
                              defaultMode.equals(
                                  DisplayControlImpl.WEIGHTED_AVERAGE));
        wa.setToolTipText("Use a weighted average sampling");
        
        JRadioButton nn = new JRadioButton(
                              DisplayControlImpl.NEAREST_NEIGHBOR,
                              defaultMode.equals(
                                  DisplayControlImpl.NEAREST_NEIGHBOR));
        nn.setToolTipText("Use a nearest neighbor sampling");
        
        GuiUtils.buttonGroup(wa, nn);
        widgets.put("WEIGHTED_AVERAGE", wa);
        widgets.put("NEAREST_NEIGHBOR", nn);

        String defaultVertCS = getStore().get(PREF_VERTICALCS,
                                   DataUtil.STD_ATMOSPHERE);

        JRadioButton sa =
            new JRadioButton("Standard Atmosphere",
                             defaultVertCS.equals(DataUtil.STD_ATMOSPHERE));
        sa.setToolTipText("Use a standard atmosphere height approximation");
        JRadioButton v5d =
            new JRadioButton("Vis5D",
                             defaultVertCS.equals(DataUtil.VIS5D_VERTICALCS));
        v5d.setToolTipText("Use the Vis5D vertical transformation");
        widgets.put(DataUtil.STD_ATMOSPHERE, sa);
        widgets.put(DataUtil.VIS5D_VERTICALCS, v5d);
        GuiUtils.buttonGroup(sa, v5d);

        String formatString = getStore().get(PREF_LATLON_FORMAT, "##0.0");
        JComboBox formatBox = GuiUtils.getEditableBox(defaultLatLonFormats,
                                  formatString);
        JLabel formatLabel = new JLabel("");
        try {
            latlonFormat.applyPattern(formatString);
            formatLabel.setText("ex: " + latlonFormat.format(latlonValue));
        } catch (IllegalArgumentException iae) {
            formatLabel.setText("Bad format: " + formatString);
        }
        formatBox.addActionListener(new ObjectListener(formatLabel) {
            public void actionPerformed(ActionEvent ae) {
                JLabel    label   = (JLabel) theObject;
                JComboBox box     = (JComboBox) ae.getSource();
                String    pattern = box.getSelectedItem().toString();
                try {
                    latlonFormat.applyPattern(pattern);
                    label.setText("ex: " + latlonFormat.format(latlonValue));
                } catch (IllegalArgumentException iae) {
                    label.setText("bad pattern: " + pattern);
                    LogUtil.userMessage("Bad format:" + pattern);
                }
            }
        });
        widgets.put(PREF_LATLON_FORMAT, formatBox);

        GuiUtils.tmpInsets = new Insets(0, 5, 0, 5);
        JPanel datePanel = GuiUtils.doLayout(new Component[] {
                               new JLabel("Pattern:"),
                               new JLabel("Time Zone:"), dateFormatBox,
                               GuiUtils.hbox(
                                   timeZoneBox,
                                   getIdv().makeHelpButton(
                                       "idv.tools.preferences.dateformat")) }, 2,
                                           GuiUtils.WT_N, GuiUtils.WT_N);

        formatComps.add(GuiUtils.rLabel("Date Format:"));
        formatComps.add(GuiUtils.left(GuiUtils.hbox(dateFormatBox,
                getIdv().makeHelpButton("idv.tools.preferences.dateformat"),
                timeLabel, 5)));

        formatComps.add(GuiUtils.rLabel("Time Zone:"));
        formatComps.add(GuiUtils.left(timeZoneBox));

        formatComps.add(GuiUtils.rLabel("Lat/Lon Format:"));
        formatComps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    formatBox,
                    getIdv().makeHelpButton(
                        "idv.tools.preferences.latlonformat"), formatLabel,
                            5)));

        formatComps.add(GuiUtils.rLabel("Probe Format:"));
        formatComps.add(GuiUtils.left(GuiUtils.hbox(probeFormatFld,
                getIdv().makeHelpButton("idv.tools.preferences.probeformat"),
                5)));

        Unit distanceUnit = null;
        try {
            distanceUnit =
                ucar.visad.Util.parseUnit(getStore().get(PREF_DISTANCEUNIT,
                    "km"));
        } catch (Exception exc) {}

        JComboBox unitBox =
            getIdv().getDisplayConventions().makeUnitBox(distanceUnit, null);
        widgets.put(PREF_DISTANCEUNIT, unitBox);

        formatComps.add(GuiUtils.rLabel("Distance Unit:"));
        formatComps.add(GuiUtils.left(unitBox));

        formatComps.add(GuiUtils.rLabel("Sampling Mode:"));
        formatComps.add(GuiUtils.left(GuiUtils.hbox(wa, nn)));

        formatComps.add(GuiUtils.rLabel("Pressure to Height:"));
        formatComps.add(GuiUtils.left(GuiUtils.hbox(sa, v5d)));

        formatComps.add(GuiUtils.rLabel("Caching:"));
        JCheckBox cacheCbx = new JCheckBox("Cache Data in Memory",
                                           getStore().get(PREF_DOCACHE,
                                               true));
        widgets.put(PREF_DOCACHE, cacheCbx);

        JTextField cacheSizeFld =
            new JTextField(Misc.format(getStore().get(PREF_CACHESIZE, 20.0)),
                           5);
        List cacheComps = Misc.newList(new JLabel("   Disk Cache Size: "),
                                       cacheSizeFld, new JLabel(" (MB)"));
        widgets.put(PREF_CACHESIZE, cacheSizeFld);
        formatComps.add(GuiUtils.left(cacheCbx));
        formatComps.add(GuiUtils.filler());
        formatComps.add(GuiUtils.left(GuiUtils.hbox(cacheComps)));

        formatComps.add(GuiUtils.rLabel("Max Image Size:"));
        JTextField imageSizeFld =
            new JTextField(Misc.format(getStore().get(PREF_MAXIMAGESIZE,
                -1)), 7);
        widgets.put(PREF_MAXIMAGESIZE, imageSizeFld);
        formatComps.add(GuiUtils.left(GuiUtils.hbox(imageSizeFld,
                new JLabel(" (Pixels, -1=no limit)"))));

        formatComps.add(GuiUtils.rLabel("Grid Threshold:"));
        JTextField thresholdFld = new JTextField(
                                      Misc.format(
                                          getStore().get(
                                              PREF_FIELD_CACHETHRESHOLD,
                                              1000000)), 7);
        widgets.put(PREF_FIELD_CACHETHRESHOLD, thresholdFld);
        formatComps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    thresholdFld,
                    new JLabel(
                        " (Bytes, cache grids larger than this to disk)"))));

        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel formatPrefs =
            GuiUtils.inset(GuiUtils.topLeft(GuiUtils.doLayout(formatComps, 2,
                GuiUtils.WT_N, GuiUtils.WT_N)), 5);    	
        
        this.add(Constants.PREF_LIST_FORMATS_DATA, "", navManager,
                GuiUtils.topCenter(GuiUtils.top(formatPrefs), new JPanel()),
                new Hashtable());        
    }
    
    /**
     * Add in the user preference tab for the choosers to show.
     */
    protected void addChooserPreferences() {
        Hashtable<String, JCheckBox> choosersData = new Hashtable<String, JCheckBox>();
        
        Boolean choosersAll =
            (Boolean) getIdv().getPreference(PROP_CHOOSERS_ALL, Boolean.TRUE);
        
        final List<String[]> choosers = getChooserData();
        
        final List<JCheckBox> choosersList = new ArrayList<JCheckBox>();

        final JRadioButton useAllBtn = new JRadioButton("Use all data sources",
                                           choosersAll.booleanValue());
        final JRadioButton useTheseBtn =
            new JRadioButton("Use selected data sources:",
                             !choosersAll.booleanValue());

        GuiUtils.buttonGroup(useAllBtn, useTheseBtn);

        // handle the user opting to enable all choosers.
        final JButton allOn = new JButton("All on");
        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
            	for (JCheckBox checkbox : choosersList)
            		checkbox.setSelected(true);
            }
        });

        // handle the user opting to disable all choosers.
        final JButton allOff = new JButton("All off");
        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (JCheckBox checkbox : choosersList)
                	checkbox.setSelected(false);
            }
        });

        // create the checkbox + chooser name that'll show up in the preference
        // panel.
        for (String[] data : choosers) {
        	JCheckBox cb = new JCheckBox(data[1], shouldShowChooser(data[0], true));
        	choosersData.put(data[0], cb);
        	choosersList.add(cb);
        }

        final JPanel chooserPanel = GuiUtils.top(GuiUtils.vbox(choosersList));
        GuiUtils.enableTree(chooserPanel, !useAllBtn.isSelected());
        GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
        GuiUtils.enableTree(allOff, !useAllBtn.isSelected());

        JScrollPane chooserScroller = new JScrollPane(chooserPanel);
        chooserScroller.getVerticalScrollBar().setUnitIncrement(10);
        chooserScroller.setPreferredSize(new Dimension(300, 300));
        JPanel widgetPanel =
            GuiUtils.topCenter(
                GuiUtils.hbox(useAllBtn, useTheseBtn),
                GuiUtils.leftCenter(
                    GuiUtils.inset(
                        GuiUtils.top(GuiUtils.vbox(allOn, allOff)),
                        4), chooserScroller));
        JPanel choosersPanel =
            GuiUtils.topCenter(
                GuiUtils.inset(
                    new JLabel("Note: This will take effect the next run"),
                    4), widgetPanel);
        choosersPanel = GuiUtils.inset(GuiUtils.left(choosersPanel), 6);
        useAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(chooserPanel, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOff, !useAllBtn.isSelected());

            }
        });
        useTheseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(chooserPanel, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOff, !useAllBtn.isSelected());
            }
        });

        PreferenceManager choosersManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
                
            	Hashtable<String, Boolean> newToShow = 
                	new Hashtable<String, Boolean>();
                
                Hashtable table = (Hashtable)data;
                for (Enumeration keys = table.keys(); keys.hasMoreElements(); ) {
                    String    chooserId = (String) keys.nextElement();
                    JCheckBox chooserCB = (JCheckBox) table.get(chooserId);
                    newToShow.put(chooserId, new Boolean(chooserCB.isSelected()));
                }
                
                choosersToShow = newToShow;
                theStore.put(PROP_CHOOSERS_ALL, new Boolean(useAllBtn.isSelected()));
                theStore.put(PROP_CHOOSERS, choosersToShow);
            }
        };
        this.add(Constants.PREF_LIST_DATA_CHOOSERS,
                 "What data sources should be shown in the user interface?",
                 choosersManager, choosersPanel, choosersData);
    }    
    
    /**
     * <p>Return a list that contains a bunch of arrays of two strings.</p>
     * 
     * <p>The first item in one of the arrays is the chooser id, and the second
     * item is the "name" of the chooser. The name is formed by working through
     * choosers.xml and concatenating each panel's category and title.</p>
     * 
     * @return A list of chooser ids and names.
     */
    private final List<String[]> getChooserData() {    	
    	List<String[]> choosers = new ArrayList<String[]>();
    	String tempString;
    	
    	try {
    		// get the root element so we can iterate through
    		final String xml = 
    			IOUtil.readContents(MCV_CHOOSERS, McIdasPreferenceManager.class);

    		final Element root = XmlUtil.getRoot(xml);
    		if (root == null)
    			return null;
    		
    		// grab all the children, which should be panels.
    		final NodeList nodeList = XmlUtil.getElements(root);
    		for (int i = 0; i < nodeList.getLength(); i++) {
    			
    			final Element item = (Element)nodeList.item(i);
    			
    			if (item.getTagName().equals(XmlUi.TAG_PANEL)) {

    				// form the name of the chooser.
    				final String title = XmlUtil.getAttribute(item, XmlUi.ATTR_TITLE, "");
    				final String cat = XmlUtil.getAttribute(item, XmlUi.ATTR_CATEGORY, "");

    				if (cat.equals(""))
    					tempString = title;
    				else
    					tempString = cat + ">" + title;
    				
    				final NodeList children = XmlUtil.getElements(item);
    				
    				for (int j = 0; j < children.getLength(); j++) {
    					final Element child = (Element)children.item(j);

    					// form the id of the chooser and add it to the list.
    					if (child.getTagName().equals("chooser")) {
    						final String id = XmlUtil.getAttribute(child, XmlUi.ATTR_ID, "");
    						String[] tmp = {id, tempString};
    						choosers.add(tmp);
    					}
    				}
    			}
    		}
    		
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	return choosers;
    }
    
	public class IconCellRenderer extends DefaultListCellRenderer {
		
		/**
		 * Extends the default list cell renderer to use icons in addition to
		 * the typical text.
		 */
		public Component getListCellRendererComponent(JList list, Object value, 
				int index, boolean isSelected, boolean cellHasFocus) {
			
			super.getListCellRendererComponent(list, value, index, isSelected, 
					cellHasFocus);
			
			if (value instanceof JLabel) {
				setText(((JLabel)value).getText());
				setIcon(((JLabel)value).getIcon());
			}

			return this;
		}

		/** 
		 * I wear some pretty fancy pants, so you'd better believe that I'm
		 * going to enable fancy-pants text antialiasing.
		 * 
		 * @param g The graphics object that we'll use as a base.
		 */
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			
			g2d.setRenderingHints(getRenderingHints());
			
			super.paintComponent(g2d);
		}
	}
}

