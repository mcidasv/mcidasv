/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2012
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.PreferenceList;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.chooser.adde.AddeChooser;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;


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
    private JLabel descLabel;
    List addeList = new ArrayList();

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
		return "TLE";
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
        addePanel =  (JPanel)makeAddePanel();

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
                        .addComponent(helpButton)
                        .addGap(GAP_RELATED)
                        .addComponent(refreshButton)
                        .addGap(GAP_RELATED)
                        .addComponent(cancelButton)
                        .addPreferredGap(RELATED)
                        .addComponent(loadButton))
                        .addGroup(LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(LEADING)
                            .addComponent(addePanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(addePanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(loadButton)
                    .addComponent(cancelButton)
                    .addComponent(refreshButton)
                    .addComponent(helpButton)))
        );

        return outerPanel;
    }

    private JComponent makeAddePanel() {
        JPanel outerPanel = new JPanel();

        addeBtn = new JRadioButton("ADDE", true);
        urlBtn = new JRadioButton("URL", false);
        GuiUtils.buttonGroup(addeBtn, urlBtn);
        addeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i=0; i<5; i++) {
                    JComponent comp = (JComponent)(addeList.get(i));
                    comp.setEnabled(true);
                    enableDescriptors(true);
                }
                for (int i=5; i<7; i++) {
                    JComponent comp = (JComponent)(addeList.get(i));
                    comp.setEnabled(false);
                }
            }
        });
        urlBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i=5; i<7; i++) {
                    JComponent comp = (JComponent)(addeList.get(i));
                    comp.setEnabled(true);
                }
                loadButton.setEnabled(true);
                for (int i=0; i<5; i++) {
                    JComponent comp = (JComponent)(addeList.get(i));
                    comp.setEnabled(false);
                    enableDescriptors(false);
                }
            }
        });
        JLabel serverLabel = new JLabel("     Server:");
        JLabel urlLabel = new JLabel("     URL:");
        descLabel = new JLabel("     Descriptor:");
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
        boxEditor = (JTextField)box.getEditor().getEditorComponent();
        boxEditor.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
            }
            public void keyTyped(KeyEvent e) {}
        });
        urlLabel.setEnabled(false);
        box.setEnabled(false);
        JLabel spaceLab = new JLabel("     ");

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
                            .addPreferredGap(RELATED, DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(connectButton))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(spaceLab)
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
                    .addComponent(spaceLab)
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

        return outerPanel;
    }

    private void enableDescriptors(boolean val) {
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
     * Create the TLE DataSource
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
        return (String) descriptorComboBox.getSelectedItem();
    }

    public void handleConnectFromThread() {
        super.handleConnectFromThread();
        enableDescriptors(true);
    }

    /**
     * Get the default display type
     *
     * @return  the name of the default display
     */
    protected String getDefaultDisplayType() {
        return "tledisplay";
    }
}
