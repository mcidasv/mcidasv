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

package edu.wisc.ssec.mcidasv;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;

import java.awt.CardLayout;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
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
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataUtil;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.IdvConstants;
import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.CheckboxCategoryPanel;
import ucar.unidata.ui.FontSelector;
import ucar.unidata.ui.HelpTipDialog;
import ucar.unidata.ui.XmlUi;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlUtil;
import ucar.visad.UtcDate;

import visad.DateTime;
import visad.Unit;

import edu.wisc.ssec.mcidasv.servermanager.EntryStore;
import edu.wisc.ssec.mcidasv.servermanager.AddePreferences;
import edu.wisc.ssec.mcidasv.servermanager.AddePreferences.AddePrefConglomeration;
import edu.wisc.ssec.mcidasv.startupmanager.Platform;
import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.ui.McvToolbarEditor;
import edu.wisc.ssec.mcidasv.ui.UIManager;
import edu.wisc.ssec.mcidasv.util.CollectionHelpers;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

/**
 * <p>An extension of {@link ucar.unidata.idv.IdvPreferenceManager} that uses
 * a JList instead of tabs to lay out the various PreferenceManagers.</p>
 *
 * @author McIDAS-V Dev Team
 */
public class McIdasPreferenceManager extends IdvPreferenceManager implements ListSelectionListener, Constants {
    
    /** Logger object. */
    private static final Logger logger = LoggerFactory.getLogger(McIdasPreferenceManager.class);
    
    /** 
     * <p>Controls how the preference panel list is displayed. Want to modify 
     * the preferences UI in some way? PREF_PANELS is your friend. Think of 
     * it like a really brain-dead SQLite.</p>
     * 
     * <p>Each row is a panel, and <b>must</b> consist of three columns:
     * <ol start="0">
     * <li>Name of the panel.</li>
     * <li>Path to the icon associated with the panel.</li>
     * <li>The panel's {@literal "help ID."}</li>
     * </ol>
     * The {@link JList} in the preferences window will order the panels based
     * upon {@code PREF_PANELS}.
     * </p>
     */
    public static final String[][] PREF_PANELS = {
        { Constants.PREF_LIST_GENERAL, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/mcidasv-round32.png", "idv.tools.preferences.generalpreferences" },
        { Constants.PREF_LIST_VIEW, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/tab-new32.png", "idv.tools.preferences.displaywindowpreferences" },
        { Constants.PREF_LIST_TOOLBAR, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/application-x-executable32.png", "idv.tools.preferences.toolbarpreferences" },
        { Constants.PREF_LIST_DATA_CHOOSERS, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/preferences-desktop-remote-desktop32.png", "idv.tools.preferences.datapreferences" },
        { Constants.PREF_LIST_ADDE_SERVERS, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/applications-internet32.png", "idv.tools.preferences.serverpreferences" },
        { Constants.PREF_LIST_AVAILABLE_DISPLAYS, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/video-display32.png", "idv.tools.preferences.availabledisplayspreferences" },
        { Constants.PREF_LIST_NAV_CONTROLS, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/input-mouse32.png", "idv.tools.preferences.navigationpreferences" },
        { Constants.PREF_LIST_FORMATS_DATA,"/edu/wisc/ssec/mcidasv/resources/icons/prefs/preferences-desktop-theme32.png", "idv.tools.preferences.formatpreferences" },
        { Constants.PREF_LIST_ADVANCED, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/applications-internet32.png", "idv.tools.preferences.advancedpreferences" }
    };
    
    /** Desired rendering hints with their desired values. */
    public static final Object[][] RENDER_HINTS = {
        { RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON },
        { RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY },
        { RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON }
    };
    
    /** Options for bundle loading */
    public static final String[] loadComboOptions = {
        "Create new window(s)",
        "Merge with active tab(s)",
        "Add new tab(s) to current window",
        "Replace session"
    };
    
    /**
     * @return The rendering hints to use, as determined by RENDER_HINTS.
     */
    public static RenderingHints getRenderingHints() {
        RenderingHints hints = new RenderingHints(null);
        for (int i = 0; i < RENDER_HINTS.length; i++) {
            hints.put(RENDER_HINTS[i][0], RENDER_HINTS[i][1]);
        }
        return hints;
    }
    
    /** Help McV remember the last preference panel the user selected. */
    private static final String LAST_PREF_PANEL = "mcv.prefs.lastpanel";
    
    private static final String LEGEND_TEMPLATE_DATA = "%datasourcename% - %displayname%";
    private static final String DISPLAY_LIST_TEMPLATE_DATA = "%datasourcename% - %displayname% " + UtcDate.MACRO_TIMESTAMP;
    private static final String TEMPLATE_IMAGEDISPLAY = "%longname% " + UtcDate.MACRO_TIMESTAMP;
    
    private static final String TEMPLATE_NO_DATA = "%displayname%";
    
    /** test value for formatting */
    private static double latlonValue = -104.56284;
    
    /** Decimal format */
    private static DecimalFormat latlonFormat = new DecimalFormat();
    
    /** Provide some default values for the lat-lon preference drop down. */
    private static final Set<String> defaultLatLonFormats = CollectionHelpers.set("##0","##0.0","##0.0#","##0.0##","0.0","0.00","0.000");
    
    private static final Set<String> probeFormatsList = CollectionHelpers.set(DisplayControl.DEFAULT_PROBEFORMAT, "%rawvalue% [%rawunit%]", "%value%", "%rawvalue%", "%value% <i>%unit%</i>");
    
    /** 
     * Replacing the "incoming" IDV preference tab names with whatever's in
     * this map.
     */
    private static final Map<String, String> replaceMap = 
        CollectionHelpers.zipMap(
            CollectionHelpers.arr("Toolbar", "View"), 
            CollectionHelpers.arr(Constants.PREF_LIST_TOOLBAR, Constants.PREF_LIST_VIEW));
            
    /** Path to the McV choosers.xml */
    private static final String MCV_CHOOSERS = "/edu/wisc/ssec/mcidasv/resources/choosers.xml";
    
    /** 
     * Maps the {@literal "name"} of a panel to the actual thing holding the 
     * PreferenceManager. 
     */
    private final Map<String, Container> prefMap = CollectionHelpers.concurrentMap();
    
    /** Maps the name of a panel to an icon. */
    private final Map<String, ImageIcon> iconCache = CollectionHelpers.concurrentMap();
    
    /** 
     * A table of the different preference managers that'll wind up in the
     * list.
     */
    private final Map<String, PreferenceManager> managerMap = CollectionHelpers.concurrentMap();
    
    /**
     * Each PreferenceManager has associated data contained in this table.
     * TODO: bug Unidata about getting IdvPreferenceManager's dataList protected
     */
    private final Map<String, Object> dataMap = CollectionHelpers.concurrentMap();
    
    private final Set<String> labelSet = new LinkedHashSet<String>();
    
    /** 
     * The list that'll contain all the names of the different 
     * PreferenceManagers 
     */
    private JList labelList;
    
    /** The "M" in the MVC for JLists. Contains all the list data. */
    private DefaultListModel listModel;
    
    /** Handle scrolling like a pro. */
    private JScrollPane listScrollPane;
    
    /** Holds the main preference pane */
    private JPanel mainPane;
    
    /** Holds the buttons at the bottom */
    private JPanel buttonPane;
    
    /** Date formats */
    private final Set<String> dateFormats = CollectionHelpers.set(
        DEFAULT_DATE_FORMAT, "MM/dd/yy HH:mm z", "dd.MM.yy HH:mm z", 
        "yyyy-MM-dd", "EEE, MMM dd yyyy HH:mm z", "HH:mm:ss", "HH:mm", 
        "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ssZ");
    
    /** The toolbar editor */
    private McvToolbarEditor toolbarEditor;
    
    /**
     * Prep as much as possible for displaying the preference window: load up
     * icons and create some of the window features.
     * 
     * @param idv Reference to the supreme IDV object.
     */
    public McIdasPreferenceManager(IntegratedDataViewer idv) {
        super(idv);
        AnnotationProcessor.process(this);
        init();
        
        for (int i = 0; i < PREF_PANELS.length; i++) {
            URL url = getClass().getResource(PREF_PANELS[i][1]);
            iconCache.put(PREF_PANELS[i][0], new ImageIcon(url));
        }
        
        setEmptyPref("idv.displaylist.template.data", DISPLAY_LIST_TEMPLATE_DATA);
        setEmptyPref("idv.displaylist.template.nodata", TEMPLATE_NO_DATA);
        setEmptyPref("idv.displaylist.template.imagedisplay", TEMPLATE_IMAGEDISPLAY);
        setEmptyPref("idv.legendlabel.template.data", LEGEND_TEMPLATE_DATA);
        setEmptyPref("idv.legendlabel.template.nodata", TEMPLATE_NO_DATA);
    }
    
    private boolean setEmptyPref(final String id, final String val) {
        IdvObjectStore store = getIdv().getStore();
        if (store.get(id, (String)null) == null) {
            store.put(id, val);
            return true;
        }
        return false;
    }
    
    /**
     * Overridden so McIDAS-V can direct users to specific help sections for
     * each preference panel.
     */
    @Override public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();
        if (!GuiUtils.CMD_HELP.equals(cmd) || labelList == null) {
            super.actionPerformed(event);
            return;
        }
        
        int selectedIndex = labelList.getSelectedIndex();
        getIdvUIManager().showHelp(PREF_PANELS[selectedIndex][2]);
    }
    
    public void replaceServerPrefPanel(final JPanel panel) {
        String name = "SERVER MANAGER";
        mainPane.add(name, panel);
        ((CardLayout)mainPane.getLayout()).show(mainPane, name);
    }
    
    @EventSubscriber(eventClass=EntryStore.Event.class)
    public void replaceServerPreferences(EntryStore.Event evt) {
        EntryStore remoteAddeStore = ((McIDASV)getIdv()).getServerManager();
        AddePreferences prefs = new AddePreferences(remoteAddeStore);
        AddePrefConglomeration eww = prefs.buildPanel((McIDASV)getIdv());
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
    @Override public void add(String tabLabel, String description, 
        PreferenceManager listener, Container panel, Object data) {
        
        // if there is an alternate name for tabLabel, find and use it.
        if (replaceMap.containsKey(tabLabel) == true) {
            tabLabel = replaceMap.get(tabLabel);
        }
        
        if (prefMap.containsKey(tabLabel) == true) {
            return;
        }
        
        // figure out the last panel that was selected.
        int selected = getIdv().getObjectStore().get(LAST_PREF_PANEL, 0);
        if (selected < 0 || selected >= PREF_PANELS.length) {
            logger.warn("attempted to select an invalid preference panel: {}", selected);
            selected = 0;
        }
        String selectedPanel = PREF_PANELS[selected][0];
        
        panel.setPreferredSize(null);
        
        Msg.translateTree(panel);
        
        managerMap.put(tabLabel, listener);
        if (data == null) {
            dataMap.put(tabLabel, new Hashtable());
        } else {
            dataMap.put(tabLabel, data);
        }
        prefMap.put(tabLabel, panel);
        
        if (labelSet.add(tabLabel)) {
            JLabel label = new JLabel();
            label.setText(tabLabel);
            label.setIcon(iconCache.get(tabLabel));
            listModel.addElement(label);
            
            labelList.setSelectedIndex(selected);
            mainPane.add(tabLabel, panel);
            if (selectedPanel.equals(tabLabel)) {
                ((CardLayout)mainPane.getLayout()).show(mainPane, tabLabel);
            }
        }
        
        mainPane.repaint();
    }
    
    /**
     * Apply the preferences (taken straight from IDV). 
     * TODO: bug Unidata about making managers and dataList protected instead of private
     * 
     * @return Whether or not each of the preference managers applied properly.
     */
    @Override public boolean apply() {
        try {
            for (String id : labelSet) {
                PreferenceManager manager = managerMap.get(id);
                manager.applyPreference(getStore(), dataMap.get(id));
            }
            fixDisplayListFont();
            getStore().save();
            return true;
        } catch (Exception exc) {
            LogUtil.logException("Error applying preferences", exc);
            return false;
        }
    }
    
    // For some reason the display list font can have a size of zero if your
    // new font size didn't change after starting the prefs panel. 
    private void fixDisplayListFont() {
        IdvObjectStore s = getStore();
        Font f = s.get(ViewManager.PREF_DISPLAYLISTFONT, FontSelector.DEFAULT_FONT);
        if (f.getSize() == 0) {
            f = f.deriveFont(8f);
            s.put(ViewManager.PREF_DISPLAYLISTFONT, f);
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
        
        for (int i = 0; i < listModel.getSize(); i++) {
            String labelText = ((JLabel)listModel.get(i)).getText();
            if (StringUtil.stringMatch(labelText, labelName)) {
                // persist across restarts
                getIdv().getObjectStore().put(LAST_PREF_PANEL, i);
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
            String name = getSelectedName();
            ((CardLayout)mainPane.getLayout()).show(mainPane, name);
        }
    }
    
    /**
     * Returns the container the corresponds to the currently selected label in
     * the JList. Also stores the selected panel so that the next time a user
     * tries to open the preferences they will start off in the panel they last
     * selected.
     * 
     * @return The current container.
     */
    private String getSelectedName() {
        // make sure the selected panel persists across restarts
        getIdv().getObjectStore().put(LAST_PREF_PANEL, labelList.getSelectedIndex());
        String key = ((JLabel)listModel.getElementAt(labelList.getSelectedIndex())).getText();
        return key;
    }
    
    /**
     * Perform the GUI initialization for the preference dialog.
     */
    public void init() {
        listModel = new DefaultListModel();
        labelList = new JList(listModel);
        
        labelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        labelList.setCellRenderer(new IconCellRenderer());
        labelList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    String name = getSelectedName();
                    if (Constants.PREF_LIST_NAV_CONTROLS.equals(name)) {
                        mainPane.add(name, makeEventPanel());
                        mainPane.validate();
                    }
                    ((CardLayout)mainPane.getLayout()).show(mainPane, name);
                }
            }
        });
        
        listScrollPane = new JScrollPane(labelList);
        listScrollPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        
        mainPane = new JPanel(new CardLayout());
        mainPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        mainPane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
//                System.err.println("prop change: prop="+e.getPropertyName()+" old="+e.getOldValue()+" new="+e.getNewValue());
                String p = e.getPropertyName();
                if (!"Frame.active".equals(p) && !"ancestor".equals(p)) {
                    return;
                }
                
                Object v = e.getNewValue();
                boolean okay = false;
                if (v instanceof Boolean) {
                    okay = ((Boolean)v).booleanValue();
                } else if (v instanceof JPanel) {
                    okay = true;
                } else {
                    okay = false;
                }
                
                if (okay) {
                    if (getSelectedName().equals(Constants.PREF_LIST_NAV_CONTROLS)) {
                        mainPane.add(Constants.PREF_LIST_NAV_CONTROLS, makeEventPanel());
                        mainPane.validate();
                        ((CardLayout)mainPane.getLayout()).show(mainPane, Constants.PREF_LIST_NAV_CONTROLS);
                    }
                }
            }
        });
        
        JPanel buttons = GuiUtils.makeApplyOkHelpCancelButtons(this);
        buttonPane = McVGuiUtils.makePrettyButtons(buttons);
        
        contents = new JPanel();
        GroupLayout layout = new GroupLayout(contents);
        contents.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(listScrollPane, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                .addPreferredGap(RELATED)
                .addComponent(mainPane, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addComponent(buttonPane, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(TRAILING)
                    .addComponent(mainPane, LEADING, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(listScrollPane, LEADING, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                    .addPreferredGap(RELATED)
                    .addComponent(buttonPane, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
        );
    }
    
    /**
     * Initialize the preference dialog. Leave most of the heavy lifting to
     * the IDV, except for creating the server manager.
     */
    protected void initPreferences() {
        // General/McIDAS-V
        addMcVPreferences();
        
        // View/Display Window
        addDisplayWindowPreferences();
        
        // Toolbar/Toolbar Options
        addToolbarPreferences();
        
        // Available Choosers/Data Sources
        addChooserPreferences();
        
        // ADDE Servers
        addServerPreferences();
        
        // Available Displays/Display Types
        addDisplayPreferences();
        
        // Navigation/Navigation Controls
        addNavigationPreferences();
        
        // Formats & Data
        addFormatDataPreferences();
        
        // Advanced
        if (!labelSet.contains(Constants.PREF_LIST_ADVANCED)) {
            // due to issue with MemoryOption.getTextComponent, we don't
            // want to do this again if Advanced tab is already built.
            // (the heap size text field will disappear on second opening
            //  of McV preferences window!)
            addAdvancedPreferences();
        }
    }
    
    /**
     * Build a {@link AddePreferences} panel {@literal "around"} the
     * server manager {@link EntryStore}.
     * 
     * @see McIDASV#getServerManager()
     */
    public void addServerPreferences() {
        EntryStore remoteAddeStore = ((McIDASV)getIdv()).getServerManager();
        AddePreferences prefs = new AddePreferences(remoteAddeStore);
        prefs.addPanel(this);
    }
    
    /**
     * Create the navigation preference panel
     */
    public void addNavigationPreferences() {
        PreferenceManager navigationManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore, Object data) {
//                System.err.println("applying nav prefs");
            }
        };
        this.add(Constants.PREF_LIST_NAV_CONTROLS, "", navigationManager, makeEventPanel(), new Hashtable());
    }
    
    /**
     * Create the toolbar preference panel
     */
    public void addToolbarPreferences() {
        if (toolbarEditor == null) {
            toolbarEditor = 
                new McvToolbarEditor((UIManager)getIdv().getIdvUIManager());
        }
        
        PreferenceManager toolbarManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore s, Object d) {
                if (toolbarEditor.anyChanges() == true) {
                    toolbarEditor.doApply();
                    UIManager mngr = (UIManager)getIdv().getIdvUIManager();
                    mngr.setCurrentToolbars(toolbarEditor);
                }
            }
        };
        this.add("Toolbar", "Toolbar icons", toolbarManager,
                              toolbarEditor.getContents(), toolbarEditor);
    }
    
    /**
     * Make a checkbox preference panel
     *
     * @param objects Holds (Label, preference id, Boolean default value).
     * If preference id is null then just show the label. If the entry is only length
     * 2 (i.e., no value) then default to true.
     * @param widgets The map to store the id to widget
     * @param store  Where to look up the preference value
     *
     * @return The created panel
     */
    @SuppressWarnings("unchecked") // idv-style.
    public static JPanel makePrefPanel(final Object[][] objects, final Hashtable widgets, final XmlObjectStore store) {
        List<JComponent> comps = CollectionHelpers.arrList();
        for (int i = 0; i < objects.length; i++) {
            final String name = (String)objects[i][0];
            final String id = (String)objects[i][1];
            final boolean value = ((objects[i].length > 2) ? ((Boolean) objects[i][2]).booleanValue() : true);
            
            if (id == null) {
                if (i > 0) {
                    comps.add(new JLabel(" "));
                }
                comps.add(new JLabel(name));
                continue;
            }
            
            final JCheckBox cb = new JCheckBox(name, store.get(id, value));
            cb.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(final PropertyChangeEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            boolean internalSel = store.get(id, value);
                            
                            cb.setSelected(store.get(id, value));
                        }
                    });
                }
            });
            if (objects[i].length > 3) {
                cb.setToolTipText(objects[i][3].toString());
            }
            widgets.put(id, cb);
            comps.add(cb);
        }
        return GuiUtils.top(GuiUtils.vbox(comps));
    }
    
    public void addAdvancedPreferences() {
        Hashtable<String, Component> widgets = new Hashtable<String, Component>();
        
        McIDASV mcv = (McIDASV)getIdv();
        
        // Build the startup options panel
        final StartupManager startup = StartupManager.getInstance();
        Platform platform = startup.getPlatform();
        platform.setUserDirectory(
                mcv.getObjectStore().getUserDirectory().toString());
        platform.setAvailableMemory(
               mcv.getStateManager().getProperty(Constants.PROP_SYSMEM, "0"));
        JPanel smPanel = startup.getAdvancedPanel(true);
        List<JPanel> stuff = Collections.singletonList(smPanel);
        
        PreferenceManager advancedManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore, Object data) {
                IdvPreferenceManager.applyWidgets((Hashtable)data, theStore);
                startup.handleApply();
            }
        };
        
        JPanel outerPanel = new JPanel();
        
        // Outer panel layout
        GroupLayout layout = new GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(smPanel, TRAILING, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(smPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );
        
        this.add(Constants.PREF_LIST_ADVANCED, "complicated stuff dude", 
            advancedManager, outerPanel, widgets);
    }
    
    /**
     * Add in the user preference tab for the controls to show
     */
    protected void addDisplayPreferences() {
        McIDASV mcv = (McIDASV)getIdv();
        cbxToCdMap = new Hashtable<JCheckBox, ControlDescriptor>();
        List<JPanel> compList = new ArrayList<JPanel>();
        List<ControlDescriptor> controlDescriptors = 
            getIdv().getAllControlDescriptors();
            
        final List<CheckboxCategoryPanel> catPanels = 
            new ArrayList<CheckboxCategoryPanel>();
            
        final Hashtable<String, CheckboxCategoryPanel> catMap = 
            new Hashtable<String, CheckboxCategoryPanel>();
            
        for (ControlDescriptor cd : controlDescriptors) {
            
            final String displayCategory = cd.getDisplayCategory();
            
            CheckboxCategoryPanel catPanel =
                (CheckboxCategoryPanel) catMap.get(displayCategory);
                
            if (catPanel == null) {
                catPanel = new CheckboxCategoryPanel(displayCategory, false);
                catPanels.add(catPanel);
                catMap.put(displayCategory, catPanel);
                compList.add(catPanel.getTopPanel());
                compList.add(catPanel);
            }
            
            JCheckBox cbx = 
                new JCheckBox(cd.getLabel(), shouldShowControl(cd, true));
            cbx.setToolTipText(cd.getDescription());
            cbxToCdMap.put(cbx, cd);
            catPanel.addItem(cbx);
            catPanel.add(GuiUtils.inset(cbx, new Insets(0, 20, 0, 0)));
        }
        
        for (CheckboxCategoryPanel cbcp : catPanels) {
            cbcp.checkVisCbx();
        }
        
        final JButton allOn = new JButton("All on");
        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (CheckboxCategoryPanel cbcp : catPanels) {
                    cbcp.toggleAll(true);
                }
            }
        });
        final JButton allOff = new JButton("All off");
        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (CheckboxCategoryPanel cbcp : catPanels) {
                    cbcp.toggleAll(false);
                }
            }
        });
        
        Boolean controlsAll =
            (Boolean)mcv.getPreference(PROP_CONTROLDESCRIPTORS_ALL, Boolean.TRUE);
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
            public void applyPreference(XmlObjectStore theStore, Object data) {
                controlDescriptorsToShow = new Hashtable();
                
                Hashtable<JCheckBox, ControlDescriptor> table = (Hashtable)data;
                
                List<ControlDescriptor> controlDescriptors = getIdv().getAllControlDescriptors();
                
                for (Enumeration keys = table.keys(); keys.hasMoreElements(); ) {
                    JCheckBox cbx = (JCheckBox) keys.nextElement();
                    ControlDescriptor cd = (ControlDescriptor)table.get(cbx);
                    controlDescriptorsToShow.put(cd.getControlId(), Boolean.valueOf(cbx.isSelected()));
                }
                
                showAllControls = useAllBtn.isSelected();
                
                theStore.put(PROP_CONTROLDESCRIPTORS, controlDescriptorsToShow);
                theStore.put(PROP_CONTROLDESCRIPTORS_ALL, Boolean.valueOf(showAllControls));
            }
        };
        
        this.add(Constants.PREF_LIST_AVAILABLE_DISPLAYS,
                 "What displays should be available in the user interface?",
                 controlsManager, controlsPanel, cbxToCdMap);
    }
    
    protected void addDisplayWindowPreferences() {
        
        Hashtable<String, JCheckBox> widgets = new Hashtable<String, JCheckBox>();
        MapViewManager mappy = new MapViewManager(getIdv());
        
        Object[][] legendObjects = {
            { "Show Side Legend", MapViewManager.PREF_SHOWSIDELEGEND, Boolean.valueOf(mappy.getShowSideLegend()) },
            { "Show Bottom Legend", MapViewManager.PREF_SHOWBOTTOMLEGEND, Boolean.valueOf(mappy.getShowBottomLegend()) }
        };
        JPanel legendPanel = makePrefPanel(legendObjects, widgets, getStore());
        legendPanel.setBorder(BorderFactory.createTitledBorder("Legends"));
        
        Object[][] navigationObjects = {
            { "Show Earth Navigation Panel", MapViewManager.PREF_SHOWEARTHNAVPANEL, Boolean.valueOf(mappy.getShowEarthNavPanel()) },
            { "Show Viewpoint Toolbar", MapViewManager.PREF_SHOWTOOLBAR + "perspective" },
            { "Show Zoom/Pan Toolbar", MapViewManager.PREF_SHOWTOOLBAR + "zoompan" },
            { "Show Undo/Redo Toolbar", MapViewManager.PREF_SHOWTOOLBAR + "undoredo" }
        };
        JPanel navigationPanel = makePrefPanel(navigationObjects, widgets, getStore());
        navigationPanel.setBorder(BorderFactory.createTitledBorder("Navigation Toolbars"));
        
        Object[][] panelObjects = {
            { "Show Globe Background", MapViewManager.PREF_SHOWGLOBEBACKGROUND, Boolean.valueOf(getStore().get(MapViewManager.PREF_SHOWGLOBEBACKGROUND, false)) },
            { "Show Wireframe Box", MapViewManager.PREF_WIREFRAME, Boolean.valueOf(mappy.getWireframe()) },
            { "Show Cursor Readout", MapViewManager.PREF_SHOWCURSOR, Boolean.valueOf(mappy.getShowCursor()) },
            { "Clip View At Box", MapViewManager.PREF_3DCLIP, Boolean.valueOf(mappy.getClipping()) },
            { "Show Layer List in Panel", MapViewManager.PREF_SHOWDISPLAYLIST, Boolean.valueOf(mappy.getShowDisplayList()) },
            { "Show Times In Panel", MapViewManager.PREF_ANIREADOUT, Boolean.valueOf(mappy.getAniReadout()) },
            { "Show Map Display Scales", MapViewManager.PREF_SHOWSCALES, Boolean.valueOf(mappy.getLabelsVisible()) },
            { "Show Transect Display Scales", MapViewManager.PREF_SHOWTRANSECTSCALES, Boolean.valueOf(mappy.getTransectLabelsVisible()) },
            { "Show \"Please Wait\" Message", MapViewManager.PREF_WAITMSG, Boolean.valueOf(mappy.getWaitMessageVisible()) },
            { "Reset Projection With New Data", MapViewManager.PREF_PROJ_USEFROMDATA }
        };
        JPanel panelPanel = makePrefPanel(panelObjects, widgets, getStore());
        panelPanel.setBorder(BorderFactory.createTitledBorder("Panel Configuration"));
        
        final JComponent[] globeBg = 
          GuiUtils.makeColorSwatchWidget(mappy.getGlobeBackgroundColorToUse(), 
              "Set Globe Background Color");
        final JComponent[] bgComps =
            GuiUtils.makeColorSwatchWidget(getStore().get(MapViewManager.PREF_BGCOLOR,
                mappy.getBackground()), "Set Background Color");
        final JComponent[] fgComps =
            GuiUtils.makeColorSwatchWidget(getStore().get(MapViewManager.PREF_FGCOLOR,
                mappy.getForeground()), "Set Foreground Color"); 
        final JComponent[] border = 
            GuiUtils.makeColorSwatchWidget(getStore().get(MapViewManager.PREF_BORDERCOLOR, 
                Constants.MCV_BLUE_DARK), "Set Selected Panel Border Color");
        
        JPanel colorPanel = GuiUtils.vbox(
                GuiUtils.hbox(
                        McVGuiUtils.makeLabelRight("Globe Background:", Width.ONEHALF),
                        GuiUtils.left(globeBg[0]),
                        GAP_RELATED
                ),
                GuiUtils.hbox(
                        McVGuiUtils.makeLabelRight("Background:", Width.ONEHALF),
                        GuiUtils.left(bgComps[0]),
                        GAP_RELATED
                ),
                GuiUtils.hbox(
                        McVGuiUtils.makeLabelRight("Foreground:", Width.ONEHALF),
                        GuiUtils.left(fgComps[0]),
                        GAP_RELATED
                ),
                GuiUtils.hbox(
                        McVGuiUtils.makeLabelRight("Selected Panel:", Width.ONEHALF),
                        GuiUtils.left(border[0]),
                        GAP_RELATED
                )
        );
        
        colorPanel.setBorder(BorderFactory.createTitledBorder("Color Scheme"));
        
        final FontSelector fontSelector = new FontSelector(FontSelector.COMBOBOX_UI, false, false);
        Font f = getStore().get(MapViewManager.PREF_DISPLAYLISTFONT, mappy.getDisplayListFont());
        fontSelector.setFont(f);
        final GuiUtils.ColorSwatch dlColorWidget =
            new GuiUtils.ColorSwatch(getStore().get(MapViewManager.PREF_DISPLAYLISTCOLOR,
                mappy.getDisplayListColor()), "Set Display List Color");
                
        JPanel fontPanel = GuiUtils.vbox(
            GuiUtils.hbox(
                McVGuiUtils.makeLabelRight("Font:", Width.ONEHALF),
                GuiUtils.left(fontSelector.getComponent()),
                GAP_RELATED
            ),
            GuiUtils.hbox(
                McVGuiUtils.makeLabelRight("Color:", Width.ONEHALF),
                GuiUtils.left(GuiUtils.hbox(dlColorWidget, dlColorWidget.getClearButton(), GAP_RELATED)),
                GAP_RELATED
            )
        );
        fontPanel.setBorder(BorderFactory.createTitledBorder("Layer List Properties"));
        
        final JComboBox projBox = new JComboBox();
        GuiUtils.setListData(projBox, mappy.getProjectionList().toArray());
        Object defaultProj = mappy.getDefaultProjection();
        if (defaultProj != null) projBox.setSelectedItem(defaultProj);
        JPanel projPanel = GuiUtils.left(projBox);
        projPanel.setBorder(BorderFactory.createTitledBorder("Default Projection"));
        
        McIDASV mcv = (McIDASV) getIdv();
        
        final JCheckBox logoVizBox = new JCheckBox(
        		"Show Logo in View",
        		mcv.getStateManager().getPreferenceOrProperty(
        				ViewManager.PREF_LOGO_VISIBILITY, false));
        final JTextField logoField =
        		new JTextField(mcv.getStateManager().getPreferenceOrProperty(ViewManager.PREF_LOGO,
        				""));
        logoField.setToolTipText("Enter a file or URL");
        // top panel
        JButton browseButton = new JButton("Browse..");
        browseButton.setToolTipText("Choose a logo from disk");
        browseButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent ae) {
        		String filename =
        				FileManager.getReadFile(FileManager.FILTER_IMAGE);
        		if (filename == null) {
        			return;
        		}
        		logoField.setText(filename);
        	}
        });

        String[] logos = ViewManager.parseLogoPosition(
        		mcv.getStateManager().getPreferenceOrProperty(
        				ViewManager.PREF_LOGO_POSITION_OFFSET, ""));
        final JComboBox logoPosBox = new JComboBox(ViewManager.logoPoses);
        logoPosBox.setToolTipText("Set the logo position on the screen");
        logoPosBox.setSelectedItem(ViewManager.findLoc(logos[0]));

        final JTextField logoOffsetField = new JTextField(logos[1]);
        // provide enough space for 12 characters
        logoOffsetField.setColumns(12);
        logoOffsetField.setToolTipText(
        		"Set an offset from the position (x,y)");

        float logoScaleFactor =
        		(float) mcv.getStateManager().getPreferenceOrProperty(ViewManager.PREF_LOGO_SCALE,
        				1.0);
        final JLabel logoSizeLab = new JLabel("" + logoScaleFactor);
        JComponent[] sliderComps = GuiUtils.makeSliderPopup(0, 20,
        		(int) (logoScaleFactor * 10), null);
        final JSlider  logoScaleSlider = (JSlider) sliderComps[1];
        ChangeListener listener        = new ChangeListener() {
        	public void stateChanged(ChangeEvent e) {
        		logoSizeLab.setText("" + logoScaleSlider.getValue() / 10.f);
        	}
        };
        logoScaleSlider.addChangeListener(listener);
        sliderComps[0].setToolTipText("Change Logo Scale Value");

        JPanel logoPanel =
        		GuiUtils.vbox(
        				GuiUtils.left(logoVizBox),
        				GuiUtils.centerRight(logoField, browseButton),
        				GuiUtils.hbox(
        						GuiUtils.leftCenter(
        								GuiUtils.rLabel("Screen Position: "),
        								logoPosBox), GuiUtils.leftCenter(
        										GuiUtils.rLabel("Offset: "),
        										logoOffsetField), GuiUtils.leftCenter(
        												GuiUtils.rLabel("Scale: "),
        												GuiUtils.leftRight(
        														logoSizeLab, sliderComps[0]))));
        logoPanel = GuiUtils.vbox(GuiUtils.lLabel(""),
        		GuiUtils.left(GuiUtils.inset(logoPanel,
        				new Insets(5, 5, 0, 0))));
        
        logoPanel.setBorder(BorderFactory.createTitledBorder("Logo"));
        
        JPanel outerPanel = new JPanel();
        
        // Outer panel layout
        GroupLayout layout = new GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(navigationPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(GAP_RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(colorPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(legendPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(fontPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(logoPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(projPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING, false)
                    .addComponent(navigationPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(legendPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(colorPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                        .addPreferredGap(RELATED)
                        .addComponent(fontPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                        .addPreferredGap(RELATED)
                        .addComponent(logoPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                        .addPreferredGap(RELATED)
                        .addComponent(projPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                    .addComponent(panelPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );
        
        PreferenceManager miscManager = new PreferenceManager() {
            // applyWidgets called the same way the IDV does it.
            public void applyPreference(XmlObjectStore theStore, Object data) {
                IdvPreferenceManager.applyWidgets((Hashtable)data, theStore);
                theStore.put(MapViewManager.PREF_PROJ_DFLT, projBox.getSelectedItem());
                theStore.put(MapViewManager.PREF_BGCOLOR, bgComps[0].getBackground());
                theStore.put(MapViewManager.PREF_FGCOLOR, fgComps[0].getBackground());
                theStore.put(MapViewManager.PREF_BORDERCOLOR, border[0].getBackground());
                theStore.put(MapViewManager.PREF_DISPLAYLISTFONT, fontSelector.getFont());
                theStore.put(MapViewManager.PREF_LOGO, logoField.getText());
                String lpos =
                    ((TwoFacedObject) logoPosBox.getSelectedItem()).getId()
                        .toString();
                String loff = logoOffsetField.getText().trim();
                theStore.put(MapViewManager.PREF_LOGO_POSITION_OFFSET,
                             ViewManager.makeLogoPosition(lpos, loff));
                theStore.put(MapViewManager.PREF_LOGO_VISIBILITY, logoVizBox.isSelected());
                theStore.put(MapViewManager.PREF_LOGO_SCALE,
                             logoScaleSlider.getValue() / 10f);
                theStore.put(MapViewManager.PREF_DISPLAYLISTCOLOR, dlColorWidget.getSwatchColor());
                theStore.put(MapViewManager.PREF_GLOBEBACKGROUND, globeBg[0].getBackground());
                ViewManager.setHighlightBorder(border[0].getBackground());
            }
        };
        
        this.add(Constants.PREF_LIST_VIEW, "Display Window Preferences", miscManager, outerPanel, widgets);
    }
    
    /**
     * Creates and adds the basic preference panel.
     */
    protected void addMcVPreferences() {
        
        Hashtable<String, Component> widgets = new Hashtable<String, Component>();
        McIDASV mcv = (McIDASV)getIdv();
        StateManager sm = (edu.wisc.ssec.mcidasv.StateManager)mcv.getStateManager();
        
        PreferenceManager basicManager = new PreferenceManager() {
            // IDV-style call to applyWidgets.
            public void applyPreference(XmlObjectStore theStore, Object data) {
                applyWidgets((Hashtable)data, theStore);
                getIdv().getIdvUIManager().setDateFormat();
                getIdv().initCacheManager();
                applyEventPreferences(theStore);
            }
        };
        
        boolean isPrerelease = sm.getIsPrerelease();
        Object[][] generalObjects = {
            { "Show Help Tips on start", HelpTipDialog.PREF_HELPTIPSHOW },
            { "Show Data Explorer on start", PREF_SHOWDASHBOARD, Boolean.TRUE },
            { "Check for new version and notice on start", Constants.PREF_VERSION_CHECK, Boolean.TRUE },
            { "Include prereleases in version check", Constants.PREF_PRERELEASE_CHECK, isPrerelease },
            { "Confirm before exiting", PREF_SHOWQUITCONFIRM },
            { "Automatically save default layout at exit", Constants.PREF_AUTO_SAVE_DEFAULT_LAYOUT, Boolean.FALSE },
            { "Save visibility of Data Explorer", Constants.PREF_SAVE_DASHBOARD_VIZ, Boolean.FALSE },
            { "Confirm removal of all data sources", PREF_CONFIRM_REMOVE_DATA, Boolean.TRUE },
            { "Confirm removal of all layers", PREF_CONFIRM_REMOVE_LAYERS, Boolean.TRUE },
            { "Confirm removal of all layers and data sources", PREF_CONFIRM_REMOVE_BOTH, Boolean.TRUE },
        };
        final IdvObjectStore store = getStore();
        JPanel generalPanel = makePrefPanel(generalObjects, widgets, store);
        generalPanel.setBorder(BorderFactory.createTitledBorder("General"));
        
        // Turn what used to be a set of checkboxes into a corresponding menu selection
        // The options have to be checkboxes in the widget collection
        // That way "applyWidgets" will work as expected
        boolean shouldRemove = store.get(PREF_OPEN_REMOVE, false);
        boolean shouldMerge  = store.get(PREF_OPEN_MERGE, false);
        final JCheckBox shouldRemoveCbx = new JCheckBox("You shouldn't see this", shouldRemove);
        final JCheckBox shouldMergeCbx  = new JCheckBox("You shouldn't see this", shouldMerge);
        widgets.put(PREF_OPEN_REMOVE, shouldRemoveCbx);
        widgets.put(PREF_OPEN_MERGE, shouldMergeCbx);
        
        final JComboBox loadComboBox = new JComboBox(loadComboOptions);
        loadComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switch (((JComboBox)e.getSource()).getSelectedIndex()) {
                case 0:
                    shouldRemoveCbx.setSelected(false);
                    shouldMergeCbx.setSelected(false);
                    break;
                case 1:
                    shouldRemoveCbx.setSelected(true);
                    shouldMergeCbx.setSelected(false);
                    break;
                case 2:
                    shouldRemoveCbx.setSelected(false);
                    shouldMergeCbx.setSelected(true);
                    break;
                case 3:
                    shouldRemoveCbx.setSelected(true);
                    shouldMergeCbx.setSelected(true);
                    break;
                }
            }
        });
        
        // update the bundle loading options upon visibility changes.
        loadComboBox.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent e) {
                String prop = e.getPropertyName();
                if (!"ancestor".equals(prop) && !"Frame.active".equals(prop)) {
                    return;
                }
                
                boolean remove = store.get(PREF_OPEN_REMOVE, false);
                boolean merge  = store.get(PREF_OPEN_MERGE, false);
                
                if (!remove) {
                    if (!merge) { 
                        loadComboBox.setSelectedIndex(0);
                    } else {
                        loadComboBox.setSelectedIndex(2);
                    }
                }
                else {
                    if (!merge) {
                        loadComboBox.setSelectedIndex(1);
                    } else {
                        loadComboBox.setSelectedIndex(3);
                    }
                }
            }
        });
        
        if (!shouldRemove) {
            if (!shouldMerge) {
                loadComboBox.setSelectedIndex(0);
            } else {
                loadComboBox.setSelectedIndex(2);
            }
        }
        else {
            if (!shouldMerge) {
                loadComboBox.setSelectedIndex(1);
            } else { 
                loadComboBox.setSelectedIndex(3);
            }
        }
        
        Object[][] bundleObjects = {
            { "Prompt when opening bundles", PREF_OPEN_ASK },
            { "Prompt for location for zipped data", PREF_ZIDV_ASK }
        };
        JPanel bundlePanelInner = makePrefPanel(bundleObjects, widgets, getStore());
        JPanel bundlePanel = GuiUtils.topCenter(loadComboBox, bundlePanelInner);
        bundlePanel.setBorder(BorderFactory.createTitledBorder("When Opening a Bundle"));
        
        Object[][] layerObjects = {
            { "Show windows when they are created", PREF_SHOWCONTROLWINDOW },
            { "Use fast rendering", PREF_FAST_RENDER, Boolean.FALSE, "<html>Turn this on for better performance at the risk of having funky displays</html>" },
            { "Auto-select data when loading a template", IdvConstants.PREF_AUTOSELECTDATA, Boolean.FALSE, "<html>When loading a display template should the data be automatically selected</html>" },
        };
        JPanel layerPanel = makePrefPanel(layerObjects, widgets, getStore());
        layerPanel.setBorder(BorderFactory.createTitledBorder("Layer Controls"));
        
        Object[][] layerclosedObjects = {
            { "Remove the display", DisplayControl.PREF_REMOVEONWINDOWCLOSE, Boolean.FALSE },
            { "Remove standalone displays", DisplayControl.PREF_STANDALONE_REMOVEONCLOSE, Boolean.FALSE }
        };
        JPanel layerclosedPanel = makePrefPanel(layerclosedObjects, widgets, getStore());
        layerclosedPanel.setBorder(BorderFactory.createTitledBorder("When Layer Control Window is Closed"));
        
        JPanel outerPanel = new JPanel();
        
        // Outer panel layout
        GroupLayout layout = new GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(TRAILING)
                    .addComponent(generalPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(layerPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(GAP_RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(bundlePanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(layerclosedPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING, false)
                    .addComponent(bundlePanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(generalPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING, false)
                    .addComponent(layerclosedPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(layerPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );
        
        this.add(Constants.PREF_LIST_GENERAL, "General Preferences", basicManager, outerPanel, widgets);
    }
    
    /**
     * <p>This determines whether the IDV should do a remove display and data 
     * before a bundle is loaded. It returns a 2 element boolean array. The 
     * first element is whether the open should take place at all. The second 
     * element determines whether displays and data should be removed before 
     * the load.</p>
     *
     * <p>Overridden by McIDAS-V so that we can ask the user whether or not we
     * should limit the number of new windows a bundle can create.</p>
     *
     * @param name Bundle name - may be null.
     *
     * @return Element 0: did user hit cancel; Element 1: Should remove data 
     *         and displays; Element 2: limit new windows.
     * 
     * @see IdvPreferenceManager#getDoRemoveBeforeOpening(String)
     */
    @Override public boolean[] getDoRemoveBeforeOpening(String name) {
        IdvObjectStore store = getStore();
        boolean shouldAsk    = store.get(PREF_OPEN_ASK, true);
        boolean shouldRemove = store.get(PREF_OPEN_REMOVE, false);
        boolean shouldMerge  = store.get(PREF_OPEN_MERGE, false);
        
        if (shouldAsk) {
            JComboBox loadComboBox = new JComboBox(loadComboOptions);
            JCheckBox preferenceCbx = new JCheckBox("Save as default preference", true);
            JCheckBox askCbx = new JCheckBox("Don't show this window again", false);
            
            if (!shouldRemove) {
                if (!shouldMerge) {
                    loadComboBox.setSelectedIndex(0);
                } else { 
                    loadComboBox.setSelectedIndex(2);
                }
            }
            else {
                if (!shouldMerge) {
                    loadComboBox.setSelectedIndex(1);
                } else {
                    loadComboBox.setSelectedIndex(3);
                }
            }
            
            JPanel inner = new JPanel();
            GroupLayout layout = new GroupLayout(inner);
            inner.setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(LEADING)
                        .addComponent(loadComboBox, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(preferenceCbx)
                        .addComponent(askCbx))
                    .addContainerGap())
            );
            layout.setVerticalGroup(
                layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(loadComboBox, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                    .addPreferredGap(RELATED)
                    .addComponent(preferenceCbx)
                    .addPreferredGap(RELATED)
                    .addComponent(askCbx)
                    .addContainerGap())
            );
            
            if (!GuiUtils.showOkCancelDialog(null, "Open bundle", inner, null)) {
                return new boolean[] { false, false, false };
            }
            
            switch (loadComboBox.getSelectedIndex()) {
                case 0: // new windows
                    shouldRemove = false;
                    shouldMerge = false;
                    break;
                case 1: // merge with existing tabs
                    shouldRemove = true;
                    shouldMerge = false;
                    break;
                case 2: // add new tab(s) to current
                    shouldRemove = false;
                    shouldMerge = true;
                    break;
                case 3: // replace session
                    shouldRemove = true;
                    shouldMerge = true;
                    break;
            }
            
            // Save these as default preference if the user wants to
            if (preferenceCbx.isSelected()) {
                store.put(PREF_OPEN_REMOVE, shouldRemove);
                store.put(PREF_OPEN_MERGE, shouldMerge);
            }
            store.put(PREF_OPEN_ASK, !askCbx.isSelected());
        }
        return new boolean[] { true, shouldRemove, shouldMerge };
    }
    
    /**
     * Creates and adds the formats and data preference panel.
     */
    protected void addFormatDataPreferences() {
        Hashtable<String, Component> widgets = new Hashtable<String, Component>();
        
        JPanel formatPanel = new JPanel();
        formatPanel.setBorder(BorderFactory.createTitledBorder("Formats"));
        
        // Date stuff
        JLabel dateLabel = McVGuiUtils.makeLabelRight("Date Format:", Width.ONEHALF);
        
        String dateFormat = getStore().get(PREF_DATE_FORMAT, DEFAULT_DATE_FORMAT);
        
        if (!dateFormats.contains(dateFormat)) {
            dateFormats.add(dateFormat);
        }
        
        final JComboBox dateComboBox = McVGuiUtils.makeComboBox(dateFormats, dateFormat, Width.DOUBLE);
        widgets.put(PREF_DATE_FORMAT, dateComboBox);
        
        JComponent dateHelpButton = getIdv().makeHelpButton("idv.tools.preferences.dateformat");
        
        JLabel dateExLabel = new JLabel("");
        
        // Time stuff
        JLabel timeLabel = McVGuiUtils.makeLabelRight("Time Zone:", Width.ONEHALF);
        
        String timeString = getStore().get(PREF_TIMEZONE, DEFAULT_TIMEZONE);
        String[] zoneStrings = TimeZone.getAvailableIDs();
        Arrays.sort(zoneStrings);
        
        final JComboBox timeComboBox = McVGuiUtils.makeComboBox(zoneStrings, timeString, Width.DOUBLE);
        widgets.put(PREF_TIMEZONE, timeComboBox);
        
        JComponent timeHelpButton = getIdv().makeHelpButton("idv.tools.preferences.dateformat");
        
        try {
            dateExLabel.setText("ex:  " + new DateTime().toString());
        } catch (Exception ve) {
            dateExLabel.setText("Can't format date: " + ve);
        }
        
        ObjectListener timeLabelListener = new ObjectListener(dateExLabel) {
            public void actionPerformed(ActionEvent ae) {
                JLabel label  = (JLabel) theObject;
                String format = dateComboBox.getSelectedItem().toString();
                String zone = timeComboBox.getSelectedItem().toString();
                try {
                    TimeZone tz = TimeZone.getTimeZone(zone);
                    // hack to make it the DateTime default
                    if (format.equals(DEFAULT_DATE_FORMAT)) {
                        if (zone.equals(DEFAULT_TIMEZONE)) {
                            format = DateTime.DEFAULT_TIME_FORMAT + "'Z'";
                        }
                    }
                    label.setText("ex:  " + new DateTime().formattedString(format, tz));
                } catch (Exception ve) {
                    label.setText("Invalid format or time zone");
                    LogUtil.userMessage("Invalid format or time zone");
                }
            }
        };
        dateComboBox.addActionListener(timeLabelListener);
        timeComboBox.addActionListener(timeLabelListener);
        
        // Lat/Lon stuff
        JLabel latlonLabel = McVGuiUtils.makeLabelRight("Lat/Lon Format:", Width.ONEHALF);
        
        String latlonFormatString = getStore().get(PREF_LATLON_FORMAT, "##0.0");
        JComboBox latlonComboBox = McVGuiUtils.makeComboBox(defaultLatLonFormats, latlonFormatString, Width.DOUBLE);
        widgets.put(PREF_LATLON_FORMAT, latlonComboBox);
        
        JComponent latlonHelpButton = getIdv().makeHelpButton("idv.tools.preferences.latlonformat");
        
        JLabel latlonExLabel = new JLabel("");
        
        try {
            latlonFormat.applyPattern(latlonFormatString);
            latlonExLabel.setText("ex: " + latlonFormat.format(latlonValue));
        } catch (IllegalArgumentException iae) {
            latlonExLabel.setText("Bad format: " + latlonFormatString);
        }
        latlonComboBox.addActionListener(new ObjectListener(latlonExLabel) {
            public void actionPerformed(final ActionEvent ae) {
                JLabel label = (JLabel)theObject;
                JComboBox box = (JComboBox)ae.getSource();
                String pattern = box.getSelectedItem().toString();
                try {
                    latlonFormat.applyPattern(pattern);
                    label.setText("ex: " + latlonFormat.format(latlonValue));
                } catch (IllegalArgumentException iae) {
                    label.setText("bad pattern: " + pattern);
                    LogUtil.userMessage("Bad format:" + pattern);
                }
            }
        });
        
        // Probe stuff
        JLabel probeLabel = McVGuiUtils.makeLabelRight("Probe Format:", Width.ONEHALF);
        
        String probeFormat = getStore().get(DisplayControl.PREF_PROBEFORMAT, DisplayControl.DEFAULT_PROBEFORMAT);
//        List probeFormatsList = Misc.newList(DisplayControl.DEFAULT_PROBEFORMAT,
//              "%rawvalue% [%rawunit%]", "%value%", "%rawvalue%", "%value% <i>%unit%</i>");
        JComboBox probeComboBox = McVGuiUtils.makeComboBox(probeFormatsList, probeFormat, Width.DOUBLE);
        widgets.put(DisplayControl.PREF_PROBEFORMAT, probeComboBox);
        
        JComponent probeHelpButton = getIdv().makeHelpButton("idv.tools.preferences.probeformat");
        
        // Distance stuff
        JLabel distanceLabel = McVGuiUtils.makeLabelRight("Distance Unit:", Width.ONEHALF);
        
        Unit distanceUnit = null;
        try {
            distanceUnit = ucar.visad.Util.parseUnit(getStore().get(PREF_DISTANCEUNIT, "km"));
        } catch (Exception exc) {}
        JComboBox distanceComboBox = getIdv().getDisplayConventions().makeUnitBox(distanceUnit, null);
        McVGuiUtils.setComponentWidth(distanceComboBox, Width.DOUBLE);
        widgets.put(PREF_DISTANCEUNIT, distanceComboBox);

        // Locale stuff (largely ripped out of IDV prefs)
        JLabel localeLabel = McVGuiUtils.makeLabelRight("Number Style:", Width.ONEHALF);
        String defaultLocale = getStore().get(PREF_LOCALE, "SYSTEM_LOCALE");
        JRadioButton sysLocale = new JRadioButton("System Default",
            defaultLocale.equals("SYSTEM_LOCALE"));

        sysLocale.setToolTipText(
            "Use the system default locale for number formatting");

        JRadioButton usLocale = new JRadioButton("English/US",
            !defaultLocale.equals("SYSTEM_LOCALE"));

        usLocale.setToolTipText("Use the US number formatting");
        GuiUtils.buttonGroup(sysLocale, usLocale);
        widgets.put("SYSTEM_LOCALE", sysLocale);
        widgets.put("US_LOCALE", usLocale);

        // Format panel layout
        GroupLayout formatLayout = new GroupLayout(formatPanel);
        formatPanel.setLayout(formatLayout);
        formatLayout.setHorizontalGroup(
            formatLayout.createParallelGroup(LEADING)
            .addGroup(formatLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(formatLayout.createParallelGroup(LEADING)
                    .addGroup(formatLayout.createSequentialGroup()
                        .addComponent(dateLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(dateComboBox)
                        .addGap(GAP_RELATED)
                        .addComponent(dateHelpButton)
                        .addGap(GAP_RELATED)
                        .addComponent(dateExLabel))
                    .addGroup(formatLayout.createSequentialGroup()
                        .addComponent(timeLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(timeComboBox))
                    .addGroup(formatLayout.createSequentialGroup()
                        .addComponent(latlonLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(latlonComboBox)
                        .addGap(GAP_RELATED)
                        .addComponent(latlonHelpButton)
                        .addGap(GAP_RELATED)
                        .addComponent(latlonExLabel))
                    .addGroup(formatLayout.createSequentialGroup()
                        .addComponent(probeLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(probeComboBox)
                        .addGap(GAP_RELATED)
                        .addComponent(probeHelpButton))
                    .addGroup(formatLayout.createSequentialGroup()
                        .addComponent(distanceLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(distanceComboBox))
                    .addGroup(formatLayout.createSequentialGroup()
                        .addComponent(localeLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(sysLocale)
                        .addGap(GAP_RELATED)
                        .addComponent(usLocale)))
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );
        formatLayout.setVerticalGroup(
            formatLayout.createParallelGroup(LEADING)
            .addGroup(formatLayout.createSequentialGroup()
                .addGroup(formatLayout.createParallelGroup(BASELINE)
                    .addComponent(dateComboBox)
                    .addComponent(dateLabel)
                    .addComponent(dateHelpButton)
                    .addComponent(dateExLabel))
                .addPreferredGap(RELATED)
                .addGroup(formatLayout.createParallelGroup(BASELINE)
                    .addComponent(timeComboBox)
                    .addComponent(timeLabel))
                .addPreferredGap(RELATED)
                .addGroup(formatLayout.createParallelGroup(BASELINE)
                    .addComponent(latlonComboBox)
                    .addComponent(latlonLabel)
                    .addComponent(latlonHelpButton)
                    .addComponent(latlonExLabel))
                .addPreferredGap(RELATED)
                .addGroup(formatLayout.createParallelGroup(BASELINE)
                    .addComponent(probeComboBox)
                    .addComponent(probeLabel)
                    .addComponent(probeHelpButton))
                .addPreferredGap(RELATED)
                .addGroup(formatLayout.createParallelGroup(BASELINE)
                    .addComponent(distanceComboBox)
                    .addComponent(distanceLabel))
                .addPreferredGap(RELATED)
                .addGroup(formatLayout.createParallelGroup(BASELINE)
                    .addComponent(localeLabel)
                    .addComponent(sysLocale)
                    .addComponent(usLocale))
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );
        
        JPanel dataPanel = new JPanel();
        dataPanel.setBorder(BorderFactory.createTitledBorder("Data"));

        // Sampling stuff
        JLabel sampleLabel = McVGuiUtils.makeLabelRight("Sampling Mode:", Width.ONEHALF);
        
        String sampleValue = getStore().get(PREF_SAMPLINGMODE, DisplayControlImpl.WEIGHTED_AVERAGE);
        JRadioButton sampleWA = new JRadioButton(DisplayControlImpl.WEIGHTED_AVERAGE,
            sampleValue.equals(DisplayControlImpl.WEIGHTED_AVERAGE));
            
        sampleWA.setToolTipText("Use a weighted average sampling");
        JRadioButton sampleNN = new JRadioButton(DisplayControlImpl.NEAREST_NEIGHBOR,
            sampleValue.equals(DisplayControlImpl.NEAREST_NEIGHBOR));
            
        sampleNN.setToolTipText("Use a nearest neighbor sampling");
        GuiUtils.buttonGroup(sampleWA, sampleNN);
        widgets.put("WEIGHTED_AVERAGE", sampleWA);
        widgets.put("NEAREST_NEIGHBOR", sampleNN);
        
        // Pressure stuff
        JLabel verticalLabel = McVGuiUtils.makeLabelRight("Pressure to Height:", Width.ONEHALF);
        
        String verticalValue = getStore().get(PREF_VERTICALCS, DataUtil.STD_ATMOSPHERE);
        JRadioButton verticalSA = new JRadioButton("Standard Atmosphere", verticalValue.equals(DataUtil.STD_ATMOSPHERE));
        verticalSA.setToolTipText("Use a standard atmosphere height approximation");
        JRadioButton verticalV5D = new JRadioButton("Vis5D", verticalValue.equals(DataUtil.VIS5D_VERTICALCS));
        verticalV5D.setToolTipText("Use the Vis5D vertical transformation");
        GuiUtils.buttonGroup(verticalSA, verticalV5D);
        widgets.put(DataUtil.STD_ATMOSPHERE, verticalSA);
        widgets.put(DataUtil.VIS5D_VERTICALCS, verticalV5D);
        
        // Caching stuff
        JLabel cacheLabel = McVGuiUtils.makeLabelRight("Caching:", Width.ONEHALF);
        
        JCheckBox cacheCheckBox = new JCheckBox("Cache Data in Memory", getStore().get(PREF_DOCACHE, true));
        widgets.put(PREF_DOCACHE, cacheCheckBox);
        
        JLabel cacheEmptyLabel = McVGuiUtils.makeLabelRight("", Width.ONEHALF);
        
        JTextField cacheTextField = McVGuiUtils.makeTextField(Misc.format(getStore().get(PREF_CACHESIZE, 20.0)));
        JComponent cacheTextFieldComponent = GuiUtils.hbox(new JLabel("Disk Cache Size: "), cacheTextField, new JLabel(" megabytes"));
        widgets.put(PREF_CACHESIZE, cacheTextField);
        
        // Image stuff
        JLabel imageLabel = McVGuiUtils.makeLabelRight("Max Image Size:", Width.ONEHALF);
        
        JTextField imageField = McVGuiUtils.makeTextField(Misc.format(getStore().get(PREF_MAXIMAGESIZE, -1)));
        JComponent imageFieldComponent = GuiUtils.hbox(imageField, new JLabel(" pixels (-1 = no limit)"));
        widgets.put(PREF_MAXIMAGESIZE, imageField);
        
        // Grid stuff
        JLabel gridLabel = McVGuiUtils.makeLabelRight("Grid Threshold:", Width.ONEHALF);
        
        JTextField gridField = McVGuiUtils.makeTextField(Misc.format(getStore().get(PREF_FIELD_CACHETHRESHOLD, 1000000.)));
        JComponent gridFieldComponent = GuiUtils.hbox(gridField, new JLabel(" bytes (Cache grids larger than this to disk)"));
        widgets.put(PREF_FIELD_CACHETHRESHOLD, gridField);
        
        // Data panel layout
        GroupLayout dataLayout = new GroupLayout(dataPanel);
        dataPanel.setLayout(dataLayout);
        dataLayout.setHorizontalGroup(
            dataLayout.createParallelGroup(LEADING)
            .addGroup(dataLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dataLayout.createParallelGroup(LEADING)
                    .addGroup(dataLayout.createSequentialGroup()
                        .addComponent(sampleLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(sampleWA)
                        .addGap(GAP_RELATED)
                        .addComponent(sampleNN))
                    .addGroup(dataLayout.createSequentialGroup()
                        .addComponent(verticalLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(verticalSA)
                        .addGap(GAP_RELATED)
                        .addComponent(verticalV5D))
                    .addGroup(dataLayout.createSequentialGroup()
                        .addComponent(cacheLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(cacheCheckBox))
                    .addGroup(dataLayout.createSequentialGroup()
                        .addComponent(cacheEmptyLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(cacheTextFieldComponent))
                    .addGroup(dataLayout.createSequentialGroup()
                        .addComponent(imageLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(imageFieldComponent))
                    .addGroup(dataLayout.createSequentialGroup()
                        .addComponent(gridLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(gridFieldComponent)))
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );
        dataLayout.setVerticalGroup(
            dataLayout.createParallelGroup(LEADING)
            .addGroup(dataLayout.createSequentialGroup()
                .addGroup(dataLayout.createParallelGroup(BASELINE)
                    .addComponent(sampleLabel)
                    .addComponent(sampleWA)
                    .addComponent(sampleNN))
                .addPreferredGap(RELATED)
                .addGroup(dataLayout.createParallelGroup(BASELINE)
                    .addComponent(verticalLabel)
                    .addComponent(verticalSA)
                    .addComponent(verticalV5D))
                .addPreferredGap(RELATED)
                .addGroup(dataLayout.createParallelGroup(BASELINE)
                    .addComponent(cacheLabel)
                    .addComponent(cacheCheckBox))
                .addPreferredGap(RELATED)
                .addGroup(dataLayout.createParallelGroup(BASELINE)
                    .addComponent(cacheEmptyLabel)
                    .addComponent(cacheTextFieldComponent))
                .addPreferredGap(RELATED)
                .addGroup(dataLayout.createParallelGroup(BASELINE)
                    .addComponent(imageLabel)
                    .addComponent(imageFieldComponent))
                .addPreferredGap(RELATED)
                .addGroup(dataLayout.createParallelGroup(BASELINE)
                    .addComponent(gridLabel)
                    .addComponent(gridFieldComponent))
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        ); 
        
        JPanel outerPanel = new JPanel();
        
        // Outer panel layout
        GroupLayout layout = new GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(formatPanel, TRAILING, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(dataPanel, TRAILING, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(formatPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                .addGap(GAP_UNRELATED)
                .addComponent(dataPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );
        
        PreferenceManager formatsManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore, Object data) {
                IdvPreferenceManager.applyWidgets((Hashtable)data, theStore);
                
                // if we ever need to add formats and data prefs, here's where
                // they get saved off (unless we override applyWidgets).
            }
        };
        
        this.add(Constants.PREF_LIST_FORMATS_DATA, "", formatsManager, outerPanel, widgets);
    }
    
    /**
     * Add in the user preference tab for the choosers to show.
     */
    protected void addChooserPreferences() {
        Hashtable<String, JCheckBox> choosersData = new Hashtable<String, JCheckBox>();
        List<JPanel> compList = new ArrayList<JPanel>();
        
        Boolean choosersAll =
            (Boolean) getIdv().getPreference(PROP_CHOOSERS_ALL, Boolean.TRUE);
            
        final List<String[]> choosers = getChooserData();
        
        final JRadioButton useAllBtn = new JRadioButton("Use all data sources",
                                           choosersAll.booleanValue());
        final JRadioButton useTheseBtn =
            new JRadioButton("Use selected data sources:",
                             !choosersAll.booleanValue());
                             
        GuiUtils.buttonGroup(useAllBtn, useTheseBtn);
        
        final List<CheckboxCategoryPanel> chooserPanels = 
            new ArrayList<CheckboxCategoryPanel>();
            
        final Hashtable<String, CheckboxCategoryPanel> chooserMap = 
            new Hashtable<String, CheckboxCategoryPanel>();
            
        // create the checkbox + chooser name that'll show up in the preference
        // panel.
        for (String[] cs : choosers) {
            final String chooserCategory = getChooserCategory(cs[1]);
            String chooserShortName = getChooserShortName(cs[1]);
            
            CheckboxCategoryPanel chooserPanel =
                (CheckboxCategoryPanel) chooserMap.get(chooserCategory);
                
            if (chooserPanel == null) {
                chooserPanel = new CheckboxCategoryPanel(chooserCategory, false);
                chooserPanels.add(chooserPanel);
                chooserMap.put(chooserCategory, chooserPanel);
                compList.add(chooserPanel.getTopPanel());
                compList.add(chooserPanel);
            }
            
            JCheckBox cbx = new JCheckBox(chooserShortName, shouldShowChooser(cs[0], true));
            choosersData.put(cs[0], cbx);
            chooserPanel.addItem(cbx);
            chooserPanel.add(GuiUtils.inset(cbx, new Insets(0, 20, 0, 0)));
        }
        
        for (CheckboxCategoryPanel cbcp : chooserPanels) {
            cbcp.checkVisCbx();
        }
        
        // handle the user opting to enable all choosers.
        final JButton allOn = new JButton("All on");
        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (CheckboxCategoryPanel cbcp : chooserPanels) {
                    cbcp.toggleAll(true);
                }
            }
        });
        
        // handle the user opting to disable all choosers.
        final JButton allOff = new JButton("All off");
        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (CheckboxCategoryPanel cbcp : chooserPanels) {
                    cbcp.toggleAll(false);
                }
            }
        });
        
        final JPanel cbPanel = GuiUtils.top(GuiUtils.vbox(compList));
        
        JScrollPane cbScroller = new JScrollPane(cbPanel);
        cbScroller.getVerticalScrollBar().setUnitIncrement(10);
        cbScroller.setPreferredSize(new Dimension(300, 300));
        
        GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
        GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
        GuiUtils.enableTree(allOff, !useAllBtn.isSelected());
        
        JPanel widgetPanel =
            GuiUtils.topCenter(
                GuiUtils.hbox(useAllBtn, useTheseBtn),
                GuiUtils.leftCenter(
                    GuiUtils.inset(
                        GuiUtils.top(GuiUtils.vbox(allOn, allOff)),
                        4), cbScroller));
        JPanel choosersPanel =
            GuiUtils.topCenter(
                GuiUtils.inset(
                    new JLabel("Note: This will take effect the next run"),
                    4), widgetPanel);
        choosersPanel = GuiUtils.inset(GuiUtils.left(choosersPanel), 6);
        useAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOff, !useAllBtn.isSelected());
                
            }
        });
        useTheseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOff, !useAllBtn.isSelected());
            }
        });
        
        PreferenceManager choosersManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore, Object data) {
                
                Hashtable<String, Boolean> newToShow = new Hashtable<String, Boolean>();
                
                Hashtable table = (Hashtable)data;
                for (Enumeration keys = table.keys(); keys.hasMoreElements(); ) {
                    String chooserId = (String) keys.nextElement();
                    JCheckBox chooserCB = (JCheckBox) table.get(chooserId);
                    newToShow.put(chooserId, Boolean.valueOf(chooserCB.isSelected()));
                }
                
                choosersToShow = newToShow;
                theStore.put(PROP_CHOOSERS_ALL, Boolean.valueOf(useAllBtn.isSelected()));
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
            if (root == null) {
                return null;
            }
            // grab all the children, which should be panels.
            final NodeList nodeList = XmlUtil.getElements(root);
            for (int i = 0; i < nodeList.getLength(); i++) {
                
                final Element item = (Element)nodeList.item(i);
                
                if (item.getTagName().equals(XmlUi.TAG_PANEL) || item.getTagName().equals("chooser")) {
                    
                    // form the name of the chooser.
                    final String title = 
                        XmlUtil.getAttribute(item, XmlUi.ATTR_TITLE, "");
                    
                    final String cat = 
                        XmlUtil.getAttribute(item, XmlUi.ATTR_CATEGORY, "");
                        
                    if (cat.equals("")) {
                        tempString = title;
                    } else {
                        tempString = cat + ">" + title;
                    }
                    
                    final NodeList children = XmlUtil.getElements(item);
                    
                    if (item.getTagName().equals("chooser")) {
                        final String id = 
                            XmlUtil.getAttribute(item, XmlUi.ATTR_ID, "");
                        String[] tmp = {id, tempString};
                        choosers.add(tmp);
                    }
                    else {
                        for (int j = 0; j < children.getLength(); j++) {
                            final Element child = (Element)children.item(j);

                            // form the id of the chooser and add it to the list.
                            if (child.getTagName().equals("chooser")) {
                                final String id = 
                                    XmlUtil.getAttribute(child, XmlUi.ATTR_ID, "");
                                String[] tmp = {id, tempString};
                                choosers.add(tmp);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return choosers;
    }
    
    /**
     * Parse the full chooser name for a category.
     * 
     * @param chooserName Name of a chooser. Cannot be {@code null}.
     * 
     * @return {@literal "Category"} associated with {@code chooserName} or 
     * {@literal "Other"} if no category is available.
     */
    private String getChooserCategory(String chooserName) {
        String chooserCategory = "Other";
        int indexSep = chooserName.indexOf('>');
        if (indexSep >= 0) {
            chooserCategory = chooserName.substring(0, indexSep);
        }
        return chooserCategory;
    }
    
    /**
     * Parse the full chooser name for a short name.
     * 
     * @param chooserName Name of a chooser. Cannot be {@code null}.
     * 
     * @return The {@literal "short name"} of {@code chooserName}.
     */
    private String getChooserShortName(String chooserName) {
        String chooserShortName = chooserName;
        int indexSep = chooserName.indexOf('>');
        if (indexSep >= 0 && chooserName.length() > indexSep + 1) {
            chooserShortName = 
                chooserName.substring(indexSep + 1, chooserName.length());
        }
        return chooserShortName;
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

