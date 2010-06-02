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
package edu.wisc.ssec.mcidasv.servermanager;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import java.awt.Component;
import java.io.File;
import java.util.Collections;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.servermanager.LocalAddeEntry.AddeFormat;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EditorAction;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryStatus;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVTextField;

/**
 * A dialog that allows the user to define or modify {@link LocalAddeEntry}s.
 */
@SuppressWarnings("serial")
public class LocalEntryEditor extends javax.swing.JDialog {

    private static final Logger logger = LoggerFactory.getLogger(LocalEntryEditor.class);

    /** Property ID for the last directory selected. */
    private static final String PROP_LAST_PATH = "mcv.localdata.lastpath";

    /** The server manager GUI. Be aware that this can be {@code null}. */
    private final TabbedAddeManager managerController;

    /** Reference back to the server manager. */
    private final EntryStore entryStore;

    private final LocalAddeEntry currentEntry;

    /** Either the path to an ADDE directory as selected by the user or an empty {@link String}. */
    private String selectedPath = "";

    /** The last dialog action performed by the user. */
    private EditorAction editorAction = EditorAction.INVALID;

    private final String datasetText;

    /**
     * Creates a modal local ADDE data editor. It's pretty useful when adding
     * from a chooser.
     * 
     * @param entryStore The server manager. Should not be {@code null}.
     * @param group Name of the group/dataset containing the desired data. Be aware that {@code null} is okay.
     */
    public LocalEntryEditor(final EntryStore entryStore, final String group) {
        super((javax.swing.JDialog)null, true);
        this.managerController = null;
        this.entryStore = entryStore;
        this.datasetText = group;
        this.currentEntry = null;
        initComponents(LocalAddeEntry.INVALID_ENTRY);
    }

    // TODO(jon): hold back on javadocs, this is likely to change
    public LocalEntryEditor(java.awt.Frame parent, boolean modal, final TabbedAddeManager manager, final EntryStore store) {
        super(manager, modal);
        this.managerController = manager;
        this.entryStore = store;
        this.datasetText = null;
        this.currentEntry = null;
        initComponents(LocalAddeEntry.INVALID_ENTRY);
    }

    // TODO(jon): hold back on javadocs, this is likely to change
    public LocalEntryEditor(java.awt.Frame parent, boolean modal, final TabbedAddeManager manager, final EntryStore store, final LocalAddeEntry entry) {
        super(manager, modal);
        this.managerController = manager;
        this.entryStore = store;
        this.datasetText = null;
        this.currentEntry = entry;
        initComponents(entry);
    }

    @SuppressWarnings("unchecked")
    private void initComponents(final LocalAddeEntry initEntry) {
        java.awt.GridBagConstraints gridBagConstraints;

        mainPanel = new javax.swing.JPanel();
        datasetLabel = new javax.swing.JLabel();
        datasetField = McVGuiUtils.makeTextFieldDeny("", 8, true, McVTextField.mcidasDeny);
        typeLabel = new javax.swing.JLabel();
        typeField = new javax.swing.JTextField();
        formatLabel = new javax.swing.JLabel();
        formatComboBox = new javax.swing.JComboBox();
        directoryLabel = new javax.swing.JLabel();
        directoryButton = new javax.swing.JButton();
        buttonPanel = new javax.swing.JPanel();
        addButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        if (initEntry == LocalAddeEntry.INVALID_ENTRY) {
            setTitle("Add Local Dataset");
        } else {
            setTitle("Edit Local Dataset");
        }
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        mainPanel.setLayout(new java.awt.GridBagLayout());

        if (datasetText != null)
            datasetField.setText(datasetText);

        datasetLabel.setText("Dataset (e.g. MYDATA):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        mainPanel.add(datasetLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(datasetField, gridBagConstraints);

        typeLabel.setText("Image Type (e.g. JAN 07 GOES):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        mainPanel.add(typeLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(typeField, gridBagConstraints);

        formatLabel.setText("Format:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        mainPanel.add(formatLabel, gridBagConstraints);

        formatComboBox.setModel(new javax.swing.DefaultComboBoxModel(new Object[] { AddeFormat.MCIDAS_AREA, AddeFormat.AMSRE_L1B, AddeFormat.AMSRE_RAIN_PRODUCT, AddeFormat.GINI, AddeFormat.LRIT_GOES9, AddeFormat.LRIT_GOES10, AddeFormat.LRIT_GOES11, AddeFormat.LRIT_GOES12, AddeFormat.LRIT_MET5, AddeFormat.LRIT_MET7, AddeFormat.LRIT_MTSAT1R, AddeFormat.METEOSAT_OPENMTP, AddeFormat.METOP_AVHRR_L1B, AddeFormat.MODIS_L1B_MOD02, AddeFormat.MODIS_L2_MOD06, AddeFormat.MODIS_L2_MOD07, AddeFormat.MODIS_L2_MOD35, AddeFormat.MODIS_L2_MOD04, AddeFormat.MODIS_L2_MOD28, AddeFormat.MODIS_L2_MODR, AddeFormat.MSG_HRIT_FD, AddeFormat.MSG_HRIT_HRV, AddeFormat.MTSAT_HRIT, AddeFormat.NOAA_AVHRR_L1B, AddeFormat.SSMI, AddeFormat.TRMM }));
        formatComboBox.setRenderer(new TooltipComboBoxRenderer());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(formatComboBox, gridBagConstraints);

        directoryLabel.setText("Directory:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        mainPanel.add(directoryLabel, gridBagConstraints);

        directoryButton.setText("Browse...");
        directoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(directoryButton, gridBagConstraints);

        buttonPanel.setLayout(new java.awt.GridBagLayout());

        if (initEntry == LocalAddeEntry.INVALID_ENTRY) {
            addButton.setText("Add Dataset");
        } else {
            addButton.setText("Save Changes");
            datasetField.setText(initEntry.getGroup());
            typeField.setText(initEntry.getName());
            directoryButton.setText(getShortString(EntryTransforms.demungeFileMask(initEntry.getFileMask())));
            formatComboBox.setSelectedItem(initEntry.getFormat());
        }
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (initEntry == LocalAddeEntry.INVALID_ENTRY)
                    addButtonActionPerformed(evt);
                else
                    editButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(addButton, new java.awt.GridBagConstraints());

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(cancelButton, new java.awt.GridBagConstraints());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(mainPanel, TRAILING)
                    .addComponent(buttonPanel, DEFAULT_SIZE, 384, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                .addPreferredGap(UNRELATED)
                .addComponent(buttonPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pack();
    }// </editor-fold>

    /**
     * Triggered when the {@literal "add"} button is clicked.
     */
    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {
        addEntry();
    }

    private void editButtonActionPerformed(java.awt.event.ActionEvent evt) {
        editEntry();
    }

    /**
     * Triggered when the {@literal "file picker"} button is clicked.
     */
    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {
        selectedPath = getDataDirectory(getLastPath());
        if (selectedPath.length() != 0) {
            if (selectedPath.length() > 19) {
                directoryButton.setText(selectedPath.substring(0, 16) + "...");
                directoryButton.setToolTipText(selectedPath);
            } else {
                directoryButton.setText(selectedPath);
            }
        }
        setLastPath(selectedPath);
    }

    private String getLastPath() {
        McIDASV mcv = McIDASV.getStaticMcv();
        String path = "";
        if (mcv != null) {
            return mcv.getObjectStore().get(PROP_LAST_PATH, "");
        }
        return path;
    }

    public void setLastPath(final String path) {
        String okayPath = (path != null) ? path : "";
        logger.debug("parent={}", new File(path).getParent());
        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv != null) {
            mcv.getObjectStore().put(PROP_LAST_PATH, okayPath);
            mcv.getObjectStore().saveIfNeeded();
        }
    }

    /**
     * Calls {@link #dispose} if the dialog is visible.
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (isDisplayable()) {
            dispose();
        }
    }

    /**
     * Poll the various UI components and attempt to construct valid ADDE
     * entries based upon the information provided by the user.
     * 
     * @return {@link Set} of entries that represent the user's input, or an
     * empty {@code Set} if the input was somehow invalid.
     */
    private Set<LocalAddeEntry> pollWidgets() {
        String group = datasetField.getText();
        String name = typeField.getText();
        String mask = getLastPath();
        AddeFormat format = (AddeFormat)formatComboBox.getSelectedItem();
        LocalAddeEntry entry = new LocalAddeEntry.Builder(name, group, mask, format).status(EntryStatus.ENABLED).build();
        return Collections.singleton(entry);
    }

    /**
     * Creates new {@link LocalAddeEntry}s based upon the contents of the dialog
     * and adds {@literal "them"} to the managed servers. If the dialog is
     * displayed, we call {@link #dispose()} and attempt to refresh the
     * server manager GUI if it is available.
     */
    private void addEntry() {
        Set<LocalAddeEntry> addedEntries = pollWidgets();
        entryStore.addEntries(addedEntries);
        if (isDisplayable()) {
            dispose();
        }
        if (managerController != null) {
            managerController.refreshDisplay();
        }
    }

    private void editEntry() {
        Set<LocalAddeEntry> newEntries = pollWidgets();
        Set<LocalAddeEntry> currentEntries = Collections.singleton(currentEntry);
        entryStore.replaceEntries(currentEntries, newEntries);
        if (isDisplayable())
            dispose();
        if (managerController != null)
            managerController.refreshDisplay();
    }

    /**
     * Get a short directory name representation, suitable for a button label.
     * 
     * @param longString Initial {@link String}. {@code null is bad}!
     * 
     * @return If {@code longString} is longer than 19 characters, the 
     * first 16 characters of {@code longString} (and {@literal "..."}) are 
     * returned. Otherwise an unmodified {@code longString} is returned.
     */
    private String getShortString(final String longString) {
        String shortString = longString;
        if (longString.length() > 19)
            shortString = longString.subSequence(0, 16) + "...";
        return shortString;
    }

    /**
     * Ask the user for a data directory from which to create a MASK=
     * 
     * @param startDir If this is a valid path, then the file picker will (presumably)
     * use that as its initial location. Should not be {@code null}?
     * 
     * @return Either a path to a data directory or {@code startDir}.
     */
    private String getDataDirectory(final String startDir) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setSelectedFile(new File(startDir));
        switch (fileChooser.showOpenDialog(this)) {
            case JFileChooser.APPROVE_OPTION:
                return fileChooser.getSelectedFile().getAbsolutePath();
            case JFileChooser.CANCEL_OPTION:
                return startDir;
            default:
                return startDir;
        }
    }

    /**
     * @see #editorAction
     */
    public EditorAction getEditorAction() {
        return editorAction;
    }

    /**
     * @see #editorAction
     */
    private void setEditorAction(final EditorAction editorAction) {
        this.editorAction = editorAction;
    }

    /**
     * Dave's nice combobox tooltip renderer!
     */
    private class TooltipComboBoxRenderer extends BasicComboBoxRenderer {
        @Override public Component getListCellRendererComponent(JList list, 
            Object value, int index, boolean isSelected, boolean cellHasFocus) 
        {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                if (value != null && (value instanceof AddeFormat))
                    list.setToolTipText(((AddeFormat)value).getTooltip());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setFont(list.getFont());
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    // Variables declaration - do not modify
    private javax.swing.JButton addButton;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField datasetField;
    private javax.swing.JLabel datasetLabel;
    private javax.swing.JButton directoryButton;
    private javax.swing.JLabel directoryLabel;
    private javax.swing.JComboBox formatComboBox;
    private javax.swing.JLabel formatLabel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JTextField typeField;
    private javax.swing.JLabel typeLabel;
    // End of variables declaration
}
