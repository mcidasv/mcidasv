/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.w3c.dom.Element;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.data.imagery.BandInfo;
import ucar.unidata.data.imagery.ImageDataSource;
import ucar.unidata.data.imagery.ImageDataset;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.idv.chooser.adde.AddeServer.Group;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.ui.ChooserList;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlNodeList;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;
import ucar.visad.UtcDate;
import visad.DateTime;
import visad.Gridded1DSet;
import visad.VisADException;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaDirectoryList;
import edu.wisc.ssec.mcidas.McIDASException;
import edu.wisc.ssec.mcidas.adde.AddeSatBands;
import edu.wisc.ssec.mcidas.adde.AddeURL;
import edu.wisc.ssec.mcidas.adde.DataSetInfo;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.chooser.adde.AddeChooser;
import edu.wisc.ssec.mcidasv.servermanager.EntryTransforms;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.EntryStore;
import edu.wisc.ssec.mcidasv.util.CollectionHelpers;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.TextColor;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;


/**
 * Widget to select images from a remote ADDE server
 * Displays a list of the descriptors (names) of the image datasets
 * available for a particular ADDE group on the remote server.
 *
 * @author Don Murray
 */
public class PolarOrbitTrackChooser extends AddeChooser implements Constants {

    /** Connect button--we need to be able to disable this */
    JButton connectButton = McVGuiUtils.makeImageTextButton(ICON_CONNECT_SMALL, "Connect");

    /** Manage button */
    JButton manageButton =
        McVGuiUtils.makeImageButton("/edu/wisc/ssec/mcidasv/resources/icons/toolbar/preferences-system22.png",
            this, "doManager", null, "Manage servers");

    /** Public button--we need to draw a menu from this */
    JButton publicButton =
        McVGuiUtils.makeImageButton("/edu/wisc/ssec/mcidasv/resources/icons/toolbar/show-layer-controls22.png",
            this, "showGroups", null, "List public datasets");

    private JComboBox serverSelector;
    private JRadioButton addeBtn;
    private JRadioButton urlBtn;
    List addeList = new ArrayList();
    List urlList = new ArrayList();

    /** Manages the pull down list of urls */
    private PreferenceList prefList;

    /** The list of urls */
    private JComboBox box;
    private JTextField boxEditor;

    /** text type */
    private static final String TLE_TYPE = "text";

    /** Property name to get the list or urls */
    public static final String PREF_URLLIST = "idv.urllist";

    /**
     * Property for the tle server name key.
     * @see #getServer()
     */
    public static String TLE_SERVER_NAME_KEY = "tle_server";
    public static String URL_NAME_KEY = "url_name";

    /**
     * Property for the tle group name key.
     * @see #getGroup()
     */
    public static String TLE_GROUP_NAME_KEY = "tle_group";

    /**
     * Property for the tle user id
     */
    public static String TLE_USER_ID_KEY = "tle_user";

    /**
     * Property for the tle project number
     */
    public static String TLE_PROJECT_NUMBER_KEY = "tle_proj";

    /**
     * Construct an Adde image selection widget
     *
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
	 * Return the data source ID.  Used by extending classes.
	 */
    @Override
	protected String getDataSourceId() {
		return "ADDE.TLE.V";
	}
    
    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    @Override
    public JComponent doMakeContents() {
        JPanel outerPanel = new JPanel();
        JPanel addePanel = new JPanel();
        JPanel urlPanel = new JPanel();

        addeBtn = new JRadioButton("ADDE", true);
        urlBtn = new JRadioButton("URL", false);
        GuiUtils.buttonGroup(addeBtn, urlBtn);
        addeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i=0; i<7; i++) {
                    JComponent comp = (JComponent)(addeList.get(i));
                    comp.setEnabled(true);
                }
                for (int i=0; i<2; i++) {
                    JComponent comp = (JComponent)(urlList.get(i));
                    comp.setEnabled(false);
                }
            }
        });
        urlBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i=0; i<2; i++) {
                    JComponent comp = (JComponent)(urlList.get(i));
                    comp.setEnabled(true);
                }
                loadButton.setEnabled(true);
                for (int i=0; i<7; i++) {
                    JComponent comp = (JComponent)(addeList.get(i));
                    comp.setEnabled(false);
                }
            }
        });

        addePanel =  (JPanel)makeAddePanel();
        urlPanel =  (JPanel)makeUrlPanel();

        JButton helpButton = McVGuiUtils.makeImageButton(ICON_HELP, "Show help");
        helpButton.setActionCommand(GuiUtils.CMD_HELP);
        helpButton.addActionListener(this);

        JButton refreshButton = McVGuiUtils.makeImageButton(ICON_REFRESH, "Refresh");
        refreshButton.setActionCommand(GuiUtils.CMD_UPDATE);
        refreshButton.addActionListener(this);

        McVGuiUtils.setComponentWidth(loadButton, Width.DOUBLE);

        GroupLayout layout = new GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(helpButton)
                        .addGap(GAP_RELATED)
                        .addComponent(refreshButton)
                        .addGap(GAP_RELATED)
                        .addComponent(cancelButton)
                        .addPreferredGap(RELATED)
                        .addComponent(loadButton))
                        .addGroup(LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(LEADING)
                            .addComponent(addeBtn)
                            .addComponent(addePanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(urlBtn)
                            .addComponent(urlPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()))) 
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(addeBtn)
                .addComponent(addePanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(UNRELATED)
                .addComponent(urlBtn)
                .addComponent(urlPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(UNRELATED)
                .addPreferredGap(UNRELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(loadButton)
                    .addComponent(cancelButton)
                    .addComponent(refreshButton)
                    .addComponent(helpButton))
                .addContainerGap())
        );

        return outerPanel;
    }

    private JComponent makeAddePanel() {
        JPanel outerPanel = new JPanel();

        JLabel serverLabel = new JLabel("Server:");

        clearOnChange(serverSelector);
        McVGuiUtils.setComponentWidth(serverSelector, Width.DOUBLE);

        JLabel groupLabel = McVGuiUtils.makeLabelRight("Dataset:");

        groupSelector.setEditable(isGroupEditable());
        clearOnChange(groupSelector);
        McVGuiUtils.setComponentWidth(groupSelector, Width.DOUBLE);

        McVGuiUtils.setComponentWidth(connectButton, Width.DOUBLE);
        connectButton.setActionCommand(CMD_CONNECT);
        connectButton.addActionListener(this);

        GroupLayout layout = new GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(TRAILING)
                        .addGroup(LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(LEADING)
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
                            .addPreferredGap(RELATED, DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(connectButton))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(descriptorLabel)
                            .addGap(GAP_RELATED)
                            .addComponent(descriptorComboBox)))))
                    .addContainerGap())
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(serverLabel)
                    .addComponent(serverSelector)
                    .addComponent(manageButton)
                    .addComponent(groupLabel)
                    .addComponent(groupSelector)
                    .addComponent(publicButton)
                    .addComponent(connectButton))
                .addPreferredGap(UNRELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(descriptorLabel)
                    .addComponent(descriptorComboBox))
                .addContainerGap())
        );

        addeList.add(serverLabel);
        addeList.add(serverSelector);
        addeList.add(groupLabel);
        addeList.add(groupSelector);
        addeList.add(connectButton);
        addeList.add(descriptorLabel);
        addeList.add(descriptorComboBox);

        return outerPanel;
    }

    private JComponent makeUrlPanel() {
        JPanel urlPanel = new JPanel();
        JLabel urlLabel = new JLabel("URL:");
        McVGuiUtils.setLabelPosition(urlLabel, Position.RIGHT);

        prefList = getPreferenceList(PREF_URLLIST);
        box = prefList.createComboBox(CMD_LOAD, this);
        boxEditor = (JTextField)box.getEditor().getEditorComponent();
        boxEditor.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
            }
            public void keyTyped(KeyEvent e) {}
        });
        urlLabel.setEnabled(false);
        box.setEnabled(false);
        urlList.add(urlLabel);
        urlList.add(box);
        urlPanel = GuiUtils.top(box);
        JPanel retPanel = McVGuiUtils.makeLabeledComponent(urlLabel, urlPanel);

        return retPanel;
    }


    /**
     * Update labels, enable widgets, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        enableWidgets();
    }

    /**
     * Get the data type ID
     *
     * @return  the data type
     */

    public String getDataType() {
        return "TEXT";
    }

    /**
      * get the adde server grup type to use
      *
      * @return group type
      */
    @Override
    protected String getGroupType() {
            return TLE_TYPE;
    }

    /**
     * User said go, we go. 
     * Create the ADDE.TLE.V DataSource
     * 
     */
    public void doLoadInThread() {
        String obj = "TLE";
        prefList.saveState(box);
        Hashtable ht = new Hashtable();
        getDataSourceProperties(ht);
        makeDataSource(obj, getDataSourceId(), ht);
        saveServerState();
    }

    /**
     * Get the DataSource properties
     * 
     * @param ht
     *            Hashtable of properties
     */
    protected void getDataSourceProperties(Hashtable ht) {
        if (addeBtn.isSelected()) {
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
        } else {
            ht.put(URL_NAME_KEY, box.getSelectedItem());
        }
    }

    private String getDatasetName() {
        return "TLE";
    }
}
