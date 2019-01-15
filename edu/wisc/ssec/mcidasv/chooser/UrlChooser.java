/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2019
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Hashtable;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.w3c.dom.Element;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.TextColor;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

import ucar.unidata.data.DataManager;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;

/**
 * Allows the user to select a url as a data source.
 */
public class UrlChooser extends ucar.unidata.idv.chooser.UrlChooser implements Constants {

    /** Property ID for the contents of {@link #textArea}. */
    public static final String PROP_MULTI_URLS =
        "mcidasv.chooser.url.multiurlcontents";

    /**
     * Property ID for the {@literal "URL type"} selection.
     * Values will be either {@link #MULTIPLE_URL_VALUE} or
     * {@link #SINGLE_URL_VALUE}.
     */
    public static final String PROP_URL_TYPE = "mcidasv.chooser.url.urltype";

    /** Multiple URL selection value for {@link #PROP_URL_TYPE}. */
    public static final String MULTIPLE_URL_VALUE = "multiple";

    /** Single URL selection value for {@link #PROP_URL_TYPE}. */
    public static final String SINGLE_URL_VALUE = "single";

    /** Manages the pull down list of URLs. */
    private PreferenceList prefList;

    /** List of URLs. */
    private JComboBox box;

    private JTextField boxEditor;

    /** Text area for multi-line URLs. */
    private JTextArea textArea;

    /** Text scroller. */
    private JScrollPane textScroller;

    /** Holds the combo box. */
    private JPanel urlPanel;

    /** Holds the text area. */
    private JPanel textPanel;

    /** Are we showing {@link #textPanel}? */
    private boolean showBox = true;

    /**
     * Panel that allows switching between {@link #urlPanel} and
     * {@link #textPanel}.
     */
    private GuiUtils.CardLayoutPanel cardLayoutPanel;

    private JLabel statusLabel = new JLabel("Status");

    /**
     * Create the {@code UrlChooser}.
     *
     * @param mgr {@code IdvChooserManager}.
     * @param root XML root that defines this chooser.
     */
    public UrlChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);

        String loadCommand = getLoadCommandName();
        loadButton =
            McVGuiUtils.makeImageTextButton(ICON_ACCEPT_SMALL, loadCommand);
        loadButton.setActionCommand(getLoadCommandName());
        loadButton.addActionListener(this);

    }

    /**
     * Show {@link #urlPanel} or {@link #textPanel}, depending on the value of
     * {@link #showBox}.
     */
    @Override public void switchFields() {
        if (showBox) {
            cardLayoutPanel.show(urlPanel);
        } else {
            cardLayoutPanel.show(textPanel);
        }
        updateStatus();
    }

    /**
     * Disable/enable any components that depend on the server.
     * Try to update the status label with what we know here.
     */
    @Override protected void updateStatus() {
        if ((boxEditor != null) && (textArea != null)) {
            if (showBox) {
                setHaveData(!boxEditor.getText().trim().isEmpty());
            } else {
                setHaveData(!textArea.getText().trim().isEmpty());
            }
            super.updateStatus();
            if (!getHaveData()) {
                if (showBox) {
                    setStatus("Enter a URL.");
                } else {
                    setStatus("Enter one or more URLs.");
                }
            }
        }
    }

    /**
     * Handle the action event from the GUI.
     */
    @Override public void doLoadInThread() {
        loadURL();
    }

    /**
     * Wrapper around {@link #loadURLInner()}, showing the wait cursor.
     */
    private void loadURL() {
        showWaitCursor();
        loadURLInner();
        showNormalCursor();
    }

    /**
     * Load the URL.
     */
    private void loadURLInner() {

        String url = "";
        String dataSourceId = getDataSourceId();
        if (showBox) {
            Object selectedItem = box.getSelectedItem();
            if (selectedItem != null) {
                url = selectedItem.toString().trim();
            }
            if (url.isEmpty() && (dataSourceId == null)) {
                userMessage("Please specify a URL.");
                return;
            }
        }

        Hashtable props = new Hashtable();
        if (dataSourceId != null) {
            props.put(DataManager.DATATYPE_ID, dataSourceId);
        }

        if (showBox) {
            if (getIdv().handleAction(url, props)) {
                closeChooser();
                prefList.saveState(box);
            }
        } else {
            List<String> urls =
                StringUtil.split(textArea.getText(), "\n", true, true);

            if (!urls.isEmpty() && makeDataSource(urls, dataSourceId, props)) {
                closeChooser();
                getIdv().getStore().put(PROP_MULTI_URLS, textArea.getText());
            }
        }
    }

    /**
     * Make the contents.
     *
     * @return  the contents
     */
    protected JPanel doMakeInnerPanel() {

        String urlType =
            getIdv().getStore().get(PROP_URL_TYPE, SINGLE_URL_VALUE);
        if (MULTIPLE_URL_VALUE.equals(urlType)) {
            showBox = false;
        }

        final JToggleButton singleBtn = new JRadioButton("Single", showBox);
        singleBtn.addActionListener(e -> {
            getIdv().getStore().put(PROP_URL_TYPE, SINGLE_URL_VALUE);
            showBox = true;
            switchFields();
        });

        final JToggleButton multipleBtn =
            new JRadioButton("Multiple", !showBox);
        multipleBtn.addActionListener(e -> {
            getIdv().getStore().put(PROP_URL_TYPE, MULTIPLE_URL_VALUE);
            showBox = false;
            switchFields();
        });
        GuiUtils.buttonGroup(singleBtn, multipleBtn);
        JPanel radioPanel = LayoutUtil.hbox(singleBtn, multipleBtn);
        
        prefList = getPreferenceList(PREF_URLLIST);
        box = prefList.createComboBox(CMD_LOAD, this);
        boxEditor = (JTextField)box.getEditor().getEditorComponent();
        boxEditor.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                updateStatus();
            }
        });
        
        textArea = new JTextArea(5, 30);
        textScroller = new JScrollPane(textArea);
        textArea.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                updateStatus();
            }
        });
        
        urlPanel = LayoutUtil.top(box);
        textPanel = LayoutUtil.top(textScroller);
        
        cardLayoutPanel = new GuiUtils.CardLayoutPanel();
        cardLayoutPanel.addCard(urlPanel);
        cardLayoutPanel.addCard(textPanel);
        
        JPanel showPanel =
            McVGuiUtils.topBottom(radioPanel, cardLayoutPanel, null);
        
        setHaveData(false);
        updateStatus();
        if (!showBox) {
            textArea.setText(getIdv().getStore().get(PROP_MULTI_URLS, ""));
            switchFields();
        }
        return McVGuiUtils.makeLabeledComponent("URL:", showPanel);
    }

    @Override public void setStatus(String statusString, String foo) {
        if (statusString == null) {
            statusString = "";
        }
        statusLabel.setText(statusString);
    }

    /**
     * Create a more McIDAS-V-like GUI layout
     */
    protected JComponent doMakeContents() {
        JComponent typeComponent = getDataSourcesComponent();
        if (typeComponent == null) {
            typeComponent = new JLabel("No data type specified");
        }

        JPanel innerPanel = doMakeInnerPanel();
        
        // Start building the whole thing here
        JPanel outerPanel = new JPanel();

        JLabel typeLabel = McVGuiUtils.makeLabelRight("Data Type:");
                
        JLabel statusLabelLabel = McVGuiUtils.makeLabelRight("");
        
        statusLabel.setText("Status");
        McVGuiUtils.setLabelPosition(statusLabel, Position.RIGHT);
        McVGuiUtils.setComponentColor(statusLabel, TextColor.STATUS);
        
        JButton helpButton =
            McVGuiUtils.makeImageButton(ICON_HELP, "Show help");
        helpButton.setActionCommand(GuiUtils.CMD_HELP);
        helpButton.addActionListener(this);
        
        JButton refreshButton =
            McVGuiUtils.makeImageButton(ICON_REFRESH, "Refresh");
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
                        .addPreferredGap(RELATED)
                        .addComponent(loadButton))
                        .addGroup(LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(LEADING)
                            .addComponent(innerPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(typeLabel)
                                .addGap(GAP_RELATED)
                                .addComponent(typeComponent))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(statusLabelLabel)
                                .addGap(GAP_RELATED)
                                .addComponent(statusLabel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(typeLabel)
                    .addComponent(typeComponent))
                .addPreferredGap(UNRELATED)
                .addComponent(innerPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(UNRELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(statusLabelLabel)
                    .addComponent(statusLabel))
                .addPreferredGap(UNRELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(loadButton)
                    .addComponent(refreshButton)
                    .addComponent(helpButton))
                .addContainerGap())
        );
        return outerPanel;
    }
}

