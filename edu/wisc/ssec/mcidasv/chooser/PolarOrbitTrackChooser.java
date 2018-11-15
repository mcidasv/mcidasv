/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
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

package edu.wisc.ssec.mcidasv.chooser;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.plaf.FileChooserUI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.PreferenceList;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.chooser.adde.AddeChooser;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;
import ucar.unidata.xml.XmlObjectStore;

/**
 * Polar Orbit Track Chooser
 * Allows user to load TLE files for display in McIDAS-V via three methods:
 * Remote: ADDE or URL
 * Local: file
 *
 * @author Gail Dengel and Tommy Jasmin
 */

public class PolarOrbitTrackChooser extends AddeChooser implements Constants {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(PolarOrbitTrackChooser.class);

    // chooser for local files
    TLEFileChooser tlefc = null;

    /** Connect button--we need to be able to disable this. */
    JButton connectButton = McVGuiUtils.makeImageTextButton(ICON_CONNECT_SMALL, "Connect");

    /** Manage button. */
    JButton manageButton =
        McVGuiUtils.makeImageButton("/edu/wisc/ssec/mcidasv/resources/icons/toolbar/preferences-system22.png",
            this, "doManager", null, "Manage servers");

    private JComboBox serverSelector;
    private JRadioButton localBtn;
    private JRadioButton addeBtn;
    private JRadioButton urlBtn;
    private JLabel descLabel;
    List<JComponent> addeList = new ArrayList<JComponent>();

    /** Manages the pull down list of URLs. */
    private PreferenceList prefList;

    /** List of URLs. */
    private JComboBox box;
    private JTextField boxEditor;

    private boolean propsOk = false;

    /** Text type. */
    private static final String TLE_TYPE = "text";

    /** Property ID used to get the list or URLs. */
    public static final String PREF_URLLIST = "idv.urllist";

    /** Property ID used to determine the last {@literal "source"}. */
    public static final String PROP_LAST_SOURCE = "mcidasv.chooser.tle.lastsource";

    /** Property value that represents the {@literal "local"} button. */
    public static final String FILE_SOURCE = "FILE";

    /** Property value that represents the {@literal "ADDE"} button. */
    public static final String ADDE_SOURCE = "ADDE";

    /** Property value that represents the {@literal "URL"} button. */
    public static final String URL_SOURCE = "URL";

    /**
     * Property for the tle server name key.
     * @see #getServer()
     */
    public static String TLE_SERVER_NAME_KEY = "tle_server";
    public static String URL_NAME_KEY = "url_name";
    public static String LOCAL_FILE_KEY = "file_object";

    /**
     * Property for the TLE group name key.
     * @see #getGroup()
     */
    public static String TLE_GROUP_NAME_KEY = "tle_group";

    /** Property for the TLE user ID. */
    public static String TLE_USER_ID_KEY = "tle_user";

    /** Property for the TLE project number. */
    public static String TLE_PROJECT_NUMBER_KEY = "tle_proj";

    /** TLE data source identifier. */
    public static final String TLE_DATA_SOURCE_ID = "TLE";

    /** TLE display type. */
    public static final String TLE_DISPLAY_TYPE = "tledisplay";

    /** TLE data source type. */
    public static final String TLE_DATA_TYPE = "TEXT";

    /**
     * Construct an Adde image selection widget
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public PolarOrbitTrackChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        serverSelector = getServerSelector();
        showServers();
    }
    
    /**
     * Return the data source ID.
     *
     * @return {@link #TLE_DATA_SOURCE_ID}
     */
    @Override protected String getDataSourceId() {
        return TLE_DATA_SOURCE_ID;
    }

    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    @Override public JComponent doMakeContents() {
        logger.debug("doMakeContents() in...");
        JPanel outerPanel = new JPanel();
        JPanel addePanel = new JPanel();
        addePanel = (JPanel)makeAddePanel();

        // retrieve our last visited directory
        String path = (String)getIdv().getStateManager().getPreference(IdvChooser.PREF_DEFAULTDIR + getId());
        String file = (String)getIdv().getStateManager().getPreference(IdvChooser.PREF_DEFAULTDIR + getId() + ".file");
        tlefc = new TLEFileChooser(this, path, file);
//        tlefc.setPotc(this);

        JButton helpButton = McVGuiUtils.makeImageButton(ICON_HELP, "Show help");
        helpButton.setActionCommand(GuiUtils.CMD_HELP);
        helpButton.addActionListener(this);

        JButton refreshButton = McVGuiUtils.makeImageButton(ICON_REFRESH, "Refresh");
        refreshButton.setActionCommand(GuiUtils.CMD_UPDATE);
        refreshButton.addActionListener(this);

        McVGuiUtils.setComponentWidth(loadButton, Width.DOUBLE);
        
        outerPanel.setLayout(new BorderLayout());
        JPanel choicePanel = new JPanel(new BorderLayout());
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        
        // load the local and remote choices in a Box in center panel
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
        JPanel localPanel = new JPanel(new BorderLayout());
        
        // create border like the remote panel (titled) but include the
        // gap that remote panel creates with various GroupLayout effects
        localPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Local"),
            BorderFactory.createEmptyBorder(GAP_RELATED, GAP_RELATED, GAP_RELATED, GAP_RELATED)));
        
        JPanel remotePanel = new JPanel();
        remotePanel.setBorder(BorderFactory.createTitledBorder("Remote"));
        
        // populate the local access panel
        localPanel.add(localBtn, BorderLayout.NORTH);
        localPanel.add(tlefc, BorderLayout.CENTER);
        
        // populate the remote access panel
        remotePanel.add(addePanel);
        
        centerPanel.add(localPanel);
        centerPanel.add(remotePanel);
        choicePanel.add(centerPanel, BorderLayout.CENTER);
        
        outerPanel.add(choicePanel, BorderLayout.CENTER);
        
        // populate and add the control panel
        controlPanel.add(helpButton);
        controlPanel.add(Box.createHorizontalStrut(5));
        controlPanel.add(refreshButton);
        controlPanel.add(Box.createHorizontalStrut(5));
        controlPanel.add(cancelButton);
        controlPanel.add(Box.createHorizontalStrut(5));
        controlPanel.add(loadButton);
        outerPanel.add(controlPanel, BorderLayout.PAGE_END);

        final XmlObjectStore store = getIdv().getStore();
        String lastSource = store.get(PROP_LAST_SOURCE, FILE_SOURCE);
        if (FILE_SOURCE.equals(lastSource)) {
            localBtn.setSelected(true);
            for (ActionListener a: localBtn.getActionListeners()) {
                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {
                    // nothing to do in here
                });
            }
        } else if (ADDE_SOURCE.equals(lastSource)) {
            addeBtn.setSelected(true);
            for (ActionListener a: addeBtn.getActionListeners()) {
                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {
                    // nothing to do in here
                });
            }
        } else if (URL_SOURCE.equals(lastSource)) {
            urlBtn.setSelected(true);
            for (ActionListener a: urlBtn.getActionListeners()) {
                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {
                    // nothing to do in here
                });
            }
        } else {
            logger.trace("should not be able to arrive here; defaulting to file. (lastSource={})", lastSource);
            localBtn.setSelected(true);
        }

        if (localBtn.isSelected()) {
            File tmp = new File(path + File.separatorChar + file);
//            logger.trace("attempting to select '{}'", tmp.getAbsolutePath());
//            tlefc.setSelectedFile(tmp);
            try {
                FileChooserUI fcUi = tlefc.getUI();
                tlefc.setSelectedFile(tmp);
                Class<? extends FileChooserUI> fcClass = fcUi.getClass();
//                logger.trace("classname={}", fcClass.getCanonicalName());
                Method setFileName = fcClass.getMethod("setFileName", String.class);
                setFileName.invoke(fcUi, tmp.getName());
//                final JList list = McVGuiUtils.getDescendantOfType(JList.class, this, "Enabled", true);
//                list.requestFocus();
            } catch (Exception e) {
                logger.warn("Could not dynamically invoke setFileName", e);
            }
//            logger.trace("selected='{}'", tlefc.getSelectedFile());


        }
        return outerPanel;
    }
    
    /**
     * Used by the local file chooser to make sure we are in Local Mode
     * (the radio button is selected).  Helps to retain cleaner state.
     *
     * @return true if Local File Mode radio button is selected
     */
    public boolean localMode() {
        if (localBtn.isSelected()) {
            return true;
        } else {
            return false;
        }
    }

    private JComponent makeAddePanel() {
        JPanel outerPanel = new JPanel();

        localBtn = new JRadioButton("File", false);
        addeBtn = new JRadioButton("ADDE", true);
        urlBtn = new JRadioButton("URL", false);
        GuiUtils.buttonGroup(localBtn, addeBtn, urlBtn);

        final XmlObjectStore store = getIdv().getStore();

        localBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableLoadFromFile(false);
                // enable the file chooser
                if (tlefc != null) {
                    tlefc.setEnabled(true);
                    tlefc.handleFileChanged();
                }

                // disable everything else? Just following pattern below
                for (int i = 0; i < 5; i++) {
                    JComponent comp = addeList.get(i);
                    comp.setEnabled(false);
                    enableDescriptors(false);
                }
                for (int i = 5; i < 7; i++) {
                    JComponent comp = addeList.get(i);
                    comp.setEnabled(false);
                }
                store.put(PROP_LAST_SOURCE, FILE_SOURCE);
                store.save();
            }
        });
        
        // TJJ Nov 2013, I need to figure out what these 
        // hardcoded component ids are!
        addeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // disable the file chooser
                if (tlefc != null) {
                    tlefc.setEnabled(false);
                }
                enableLoadFromAdde(true);
                for (int i = 0; i < 5; i++) {
                    JComponent comp = addeList.get(i);
                    comp.setEnabled(true);
                    enableDescriptors(true);
                }
                for (int i = 5; i < 7; i++) {
                    JComponent comp = addeList.get(i);
                    comp.setEnabled(false);
                }
                store.put(PROP_LAST_SOURCE, ADDE_SOURCE);
                store.save();
            }
        });

        final JLabel urlLabel = new JLabel("URL:");
        // TJJ Nov 2013, I need to figure out what these 
        // hardcoded component ids are!
        urlBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//                logger.trace("url button");
                // disable the file chooser
                if (tlefc != null) {
                    tlefc.setEnabled(false);
                }
                for (int i = 5; i < 7; i++) {
                    JComponent comp = addeList.get(i);
                    comp.setEnabled(true);
                }
                enableLoadFromUrl(true);
                for (int i = 0; i < 5; i++) {
                    JComponent comp = addeList.get(i);
                    comp.setEnabled(false);
                    enableDescriptors(false);
                }
                urlLabel.setEnabled(true);
                box.setEnabled(true);
                store.put(PROP_LAST_SOURCE, URL_SOURCE);
                store.save();
            }
        });
        JLabel serverLabel = new JLabel("Server:");

        descLabel = new JLabel("Descriptor:");
        descLabel.setEnabled(false);
        descriptorComboBox.setEnabled(false);

        clearOnChange(serverSelector);
        McVGuiUtils.setComponentWidth(serverSelector, Width.DOUBLE);

        JLabel groupLabel = McVGuiUtils.makeLabelRight("Dataset:");

        groupSelector.setEditable(isGroupEditable());
        clearOnChange(groupSelector);
        McVGuiUtils.setComponentWidth(groupSelector, Width.DOUBLE);

        McVGuiUtils.setComponentWidth(connectButton, Width.DOUBLE);
        connectButton.setActionCommand(CMD_CONNECT);
        connectButton.addActionListener(this);

        prefList = getPreferenceList(PREF_URLLIST);
        box = prefList.createComboBox(CMD_LOAD, this);
        boxEditor = (JTextField) box.getEditor().getEditorComponent();
        boxEditor.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
            public void keyTyped(KeyEvent e) {}
        });
        urlLabel.setEnabled(false);
        box.setEnabled(false);

        GroupLayout layout = new GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(TRAILING)
                        .addGroup(LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(LEADING)
                        .addComponent(addeBtn)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(serverLabel)
                            .addGap(GAP_RELATED)
                            .addComponent(serverSelector)
                            .addGap(GAP_RELATED)
                            .addComponent(manageButton)
                            .addGap(GAP_RELATED)
                            .addComponent(groupLabel)
                            .addGap(GAP_RELATED)
                            .addComponent(groupSelector)
                            .addGap(GAP_RELATED)
                            .addComponent(publicButton)
                            .addPreferredGap(RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(connectButton))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(descLabel)
                            .addGap(GAP_RELATED)
                            .addComponent(descriptorComboBox))
                            .addGap(GAP_RELATED)
                        .addComponent(urlBtn)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(urlLabel)
                            .addGap(GAP_RELATED)
                            .addComponent(box))))))
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(addeBtn)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(serverLabel)
                    .addComponent(serverSelector)
                    .addComponent(manageButton)
                    .addComponent(groupLabel)
                    .addComponent(groupSelector)
                    .addComponent(publicButton)
                    .addComponent(connectButton))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(descLabel)
                    .addComponent(descriptorComboBox))
                .addPreferredGap(UNRELATED)
                .addComponent(urlBtn)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(urlLabel)
                    .addComponent(box)))
        );

        addeList.add(serverLabel);
        addeList.add(serverSelector);
        addeList.add(groupLabel);
        addeList.add(groupSelector);
        addeList.add(connectButton);
        McVGuiUtils.setComponentWidth(descriptorComboBox, Width.DOUBLEDOUBLE);
        addeList.add(urlLabel);
        addeList.add(box);

//        String lastSource = store.get(PROP_LAST_SOURCE, FILE_SOURCE);
//        if (FILE_SOURCE.equals(lastSource)) {
//            localBtn.setSelected(true);
//            for (ActionListener a: localBtn.getActionListeners()) {
//                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {
//                    // nothing to do in here
//                });
//            }
//        } else if (ADDE_SOURCE.equals(lastSource)) {
//            addeBtn.setSelected(true);
//            for (ActionListener a: addeBtn.getActionListeners()) {
//                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {
//                    // nothing to do in here
//                });
//            }
//        } else if (URL_SOURCE.equals(lastSource)) {
//            urlBtn.setSelected(true);
//            for (ActionListener a: urlBtn.getActionListeners()) {
//                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {
//                    // nothing to do in here
//                });
//            }
//        } else {
//            logger.trace("should not be able to arrive here; defaulting to file. (lastSource={})", lastSource);
//            localBtn.setSelected(true);
//        }

        return outerPanel;
    }

    public void enableLoadFromFile(boolean val) {
//        logger.trace("val={}", val);
        loadButton.setEnabled(val);
        if (tlefc != null && tlefc.getSelectedFile() != null) {
            setHaveData(val);
        }
    }

    public void enableLoadFromAdde(boolean val) {
//        logger.trace("val={}", val);
        if (val && descriptorComboBox.isEnabled() && getSelectedDescriptor() != null) {
            loadButton.setEnabled(val);
        } else {
            loadButton.setEnabled(val);
        }
    }

    public void enableLoadFromUrl(boolean val) {
//        logger.trace("val={}", val);
        loadButton.setEnabled(val);
        String url = (String)box.getSelectedItem();
        if (val && url != null && !url.isEmpty()) {
            setHaveData(true);
        } else {
            setHaveData(val);
        }
    }

    public void enableFileLoad(boolean val) {
//        logger.trace("loadButton={}", val);
        loadButton.setEnabled(val);
    }

    private void enableDescriptors(boolean val) {
//        logger.trace("descriptors={}", val);
        if (val) {
            boolean connected;
            if (getState() == STATE_CONNECTED) {
                connected = true;
            } else {
                connected = false;
            }
            if (connected) {
                descLabel.setEnabled(true);
                descriptorComboBox.setEnabled(true);
            } else {
                descLabel.setEnabled(false);
                descriptorComboBox.setEnabled(false);
            }
        } else {
            descLabel.setEnabled(false);
            descriptorComboBox.setEnabled(false);
        }
    }

    @Override protected boolean getGoodToGo() {
        final XmlObjectStore store = getIdv().getStore();
        String lastSource = store.get(PROP_LAST_SOURCE, FILE_SOURCE);
        boolean goodToGo = false;
        if (URL_SOURCE.equals(lastSource)) {
            goodToGo = true;
        }
        return goodToGo;
    }

    /**
     * Update labels, enable widgets, etc.
     */
    @Override protected void updateStatus() {
//        if (localBtn != null && addeBtn != null && urlBtn != null) {
//            logger.trace("updating status fromFile={} fromAdde={} fromUrl={}", localBtn.isSelected(), addeBtn.isSelected(), urlBtn.isSelected());
//        }
        super.updateStatus();
        enableWidgets();
        if ((addeBtn != null) && addeBtn.isSelected()) {
            enableLoadFromAdde(true);
        }
    }

    /**
     * Get the data type ID.
     *
     * @return {@link #TLE_DATA_TYPE}
     */
    @Override public String getDataType() {
        return TLE_DATA_TYPE;
    }

    /**
     * Get the adde server group type to use.
     *
     * @return {@link #TLE_TYPE}
     */
    @Override protected String getGroupType() {
        return TLE_TYPE;
    }

    /**
     * User said go, we go. 
     * Create the TLE DataSource
     */
    @Override public void doLoadInThread() {
        prefList.saveState(box);
        String dsName = TLE_DATA_SOURCE_ID;
        if (tlefc.getSelectedFile() != null) {
            dsName = tlefc.getSelectedFile().getName();
        }
        Hashtable ht = new Hashtable();
        getDataSourceProperties(ht);
        if (propsOk) {
            makeDataSource(dsName, TLE_DATA_SOURCE_ID, ht);
            saveServerState();
        }
    }

    /**
     * Get the DataSource properties
     * 
     * @param ht Hashtable of properties
     */
    @Override protected void getDataSourceProperties(Hashtable ht) {

        // Local data
        if (localBtn.isSelected()) {
            if (tlefc.getSelectedFile() != null) {
                // local file, set a new key...
                ht.put(LOCAL_FILE_KEY, tlefc.getSelectedFile());
                propsOk = true;
            } else {
                JOptionPane.showMessageDialog(this, "No file selected.");
                propsOk = false;
            }
        }

        // Remote data, ADDE
        if (addeBtn.isSelected()) {
            if (getState() == STATE_CONNECTED) {
                super.getDataSourceProperties(ht);
                ht.put(DATASET_NAME_KEY, getDatasetName());
                String server = getServer();
                ht.put(TLE_SERVER_NAME_KEY, server);
                String group = getGroup();
                ht.put(TLE_GROUP_NAME_KEY, group);
                Map<String, String> acct = getAccounting(server, group);
                String user = acct.get("user");
                String proj = acct.get("proj");
                ht.put(TLE_USER_ID_KEY, user);
                ht.put(TLE_PROJECT_NUMBER_KEY, proj);
                propsOk = true;
            } else {
                JOptionPane.showMessageDialog(this, "No ADDE server connection.");
                propsOk = false;
            }
        }

        // Remote or Local, URL
        if (urlBtn.isSelected()) {
            String s = (String) box.getSelectedItem();
            if ((s != null) && !s.isEmpty()) {
                // TJJ Nov 2018
                // Until we can update Java 8 distributed with McV, Celestrak cert authority
                // isn't recognized, so revert to HTTP whenever HTTPS is used
                s = s.replaceAll("(?i)https", "http");
                ht.put(URL_NAME_KEY, s);
                propsOk = true;
            } else {
                JOptionPane.showMessageDialog(this, "Please provide a valid URL.");
                propsOk = false;
            }
        }
    }

    /* (non-Javadoc)
     * @see edu.wisc.ssec.mcidasv.chooser.adde.AddeChooser#showGroups()
     */
    @Override
    public void showGroups() {
        super.showGroups();
    }

    private String getDatasetName() {
        String dsName = (String) descriptorComboBox.getSelectedItem();
        // strip out description part
        dsName = dsName.substring(0, dsName.indexOf(" "));
        return dsName;
    }

    @Override public void handleConnectFromThread() {
        super.handleConnectFromThread();
        enableDescriptors(true);
    }

    /**
     * Get the default display type.
     *
     * @return {@link #TLE_DISPLAY_TYPE}
     */
    @Override protected String getDefaultDisplayType() {
        return TLE_DISPLAY_TYPE;
    }
}
