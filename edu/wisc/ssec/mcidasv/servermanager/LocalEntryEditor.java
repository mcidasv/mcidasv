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
package edu.wisc.ssec.mcidasv.servermanager;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.xml.XmlObjectStore;

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
public class LocalEntryEditor extends JDialog {

    private static final Logger logger = LoggerFactory.getLogger(LocalEntryEditor.class);

    /** Property ID for the last directory selected. */
    private static final String PROP_LAST_PATH = "mcv.localdata.lastpath";

    /** The valid local ADDE formats. */
    private static final DefaultComboBoxModel<AddeFormat> formats = new DefaultComboBoxModel<>(new AddeFormat[] { AddeFormat.MCIDAS_AREA, AddeFormat.AMSRE_L1B, AddeFormat.AMSRE_RAIN_PRODUCT, AddeFormat.GINI, AddeFormat.LRIT_GOES9, AddeFormat.LRIT_GOES10, AddeFormat.LRIT_GOES11, AddeFormat.LRIT_GOES12, AddeFormat.LRIT_MET5, AddeFormat.LRIT_MET7, AddeFormat.LRIT_MTSAT1R, AddeFormat.METEOSAT_OPENMTP, AddeFormat.METOP_AVHRR_L1B, AddeFormat.MODIS_L1B_MOD02, AddeFormat.MODIS_L2_MOD06, AddeFormat.MODIS_L2_MOD07, AddeFormat.MODIS_L2_MOD35, AddeFormat.MODIS_L2_MOD04, AddeFormat.MODIS_L2_MOD28, AddeFormat.MODIS_L2_MODR, AddeFormat.MSG_HRIT_FD, AddeFormat.MSG_HRIT_HRV, AddeFormat.MTSAT_HRIT, AddeFormat.NOAA_AVHRR_L1B, AddeFormat.SSMI, AddeFormat.TRMM, AddeFormat.MCIDAS_MD });

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
        super((JDialog)null, true);
        this.managerController = null;
        this.entryStore = entryStore;
        this.datasetText = group;
        this.currentEntry = null;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                initComponents(LocalAddeEntry.INVALID_ENTRY);
            }
        });
    }

    // TODO(jon): hold back on javadocs, this is likely to change
    public LocalEntryEditor(java.awt.Frame parent, boolean modal, final TabbedAddeManager manager, final EntryStore store) {
        super(manager, modal);
        this.managerController = manager;
        this.entryStore = store;
        this.datasetText = null;
        this.currentEntry = null;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                initComponents(LocalAddeEntry.INVALID_ENTRY);
            }
        });
    }

    // TODO(jon): hold back on javadocs, this is likely to change
    public LocalEntryEditor(java.awt.Frame parent, boolean modal, final TabbedAddeManager manager, final EntryStore store, final LocalAddeEntry entry) {
        super(manager, modal);
        this.managerController = manager;
        this.entryStore = store;
        this.datasetText = null;
        this.currentEntry = entry;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                initComponents(entry);
            }
        });
    }

    /**
     * Creates the editor dialog and initializes the various GUI components.
     * 
     * @param initEntry Use {@link LocalAddeEntry#INVALID_ENTRY} to specify 
     * that the user is creating a new entry; otherwise provide the actual
     * entry that the user is editing.
     */
    private void initComponents(final LocalAddeEntry initEntry) {
        JLabel datasetLabel = new JLabel("Dataset (e.g. MYDATA):");
        datasetField = McVGuiUtils.makeTextFieldDeny("", 8, true, McVTextField.mcidasDeny);
        datasetLabel.setLabelFor(datasetField);
        datasetField.setColumns(20);
        if (datasetText != null) {
            datasetField.setText(datasetText);
        }

        JLabel typeLabel = new JLabel("Image Type (e.g. JAN 07 GOES):");
        typeField = new JTextField();
        typeLabel.setLabelFor(typeField);
        typeField.setColumns(20);

        JLabel formatLabel = new JLabel("Format:");
        formatComboBox = new JComboBox<>();
        formatComboBox.setRenderer(new TooltipComboBoxRenderer());
        formatComboBox.setModel(formats);
        formatComboBox.setSelectedIndex(0);
        formatLabel.setLabelFor(formatComboBox);

        JLabel directoryLabel = new JLabel("Directory:");
        directoryField = new JTextField();
        directoryLabel.setLabelFor(directoryField);
        directoryField.setColumns(20);

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        JButton saveButton = new JButton("Add Dataset");
        saveButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent evt) {
                if (initEntry == LocalAddeEntry.INVALID_ENTRY) {
                    saveButtonActionPerformed(evt);
                } else {
                    editButtonActionPerformed(evt);
                }
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        if (initEntry == LocalAddeEntry.INVALID_ENTRY) {
            setTitle("Add Local Dataset");
        } else {
            setTitle("Edit Local Dataset");
            saveButton.setText("Save Changes");
            datasetField.setText(initEntry.getGroup());
            typeField.setText(initEntry.getName());
            directoryField.setText(EntryTransforms.demungeFileMask(initEntry.getFileMask()));
            formatComboBox.setSelectedItem(initEntry.getFormat());
        }

        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Container c = getContentPane();
        c.setLayout(new MigLayout(
            "",                    // general layout constraints; currently
                                   // none are specified.
            "[align right][fill]", // column constraints; defined two columns
                                   // leftmost aligns the components right;
                                   // rightmost simply fills the remaining space
            "[][][][][][]"));      // row constraints; possibly not needed in
                                   // this particular example?

        // done via WindowBuilder + Eclipse
//        c.add(datasetLabel,   "cell 0 0"); // row: 0; col: 0
//        c.add(datasetField,   "cell 1 0"); // row: 0; col: 1
//        c.add(typeLabel,      "cell 0 1"); // row: 1; col: 0
//        c.add(typeField,      "cell 1 1"); // row: 1; col: 1
//        c.add(formatLabel,    "cell 0 2"); // row: 2; col: 0
//        c.add(formatComboBox, "cell 1 2"); // row: 2; col: 1
//        c.add(directoryLabel, "cell 0 3"); // row: 3; col: 0 ... etc!
//        c.add(directoryField, "flowx,cell 1 3");
//        c.add(browseButton,   "cell 1 3,alignx right");
//        c.add(saveButton,     "flowx,cell 1 5,alignx right,aligny top");
//        c.add(cancelButton,   "cell 1 5,alignx right,aligny top");

        // another way to accomplish the above layout.
        c.add(datasetLabel);
        c.add(datasetField,   "wrap"); // think "newline" or "new row"
        c.add(typeLabel);
        c.add(typeField,      "wrap"); // think "newline" or "new row"
        c.add(formatLabel);
        c.add(formatComboBox, "wrap"); // think "newline" or "new row"
        c.add(directoryLabel);
        c.add(directoryField, "flowx, split 2"); // split this current cell 
                                                 // into two "subcells"; this
                                                 // will cause browseButton to
                                                 // be grouped into the current
                                                 // cell.
        c.add(browseButton,   "alignx right, wrap");

        // skips "cell 0 5" causing this row to start in "cell 1 5"; splits 
        // the cell so that saveButton and cancelButton both occupy cell 1 5.
        c.add(saveButton,     "flowx, split 2, skip 1, alignx right, aligny top");
        c.add(cancelButton,   "alignx right, aligny top");
        pack();
    }// </editor-fold>

    /**
     * Triggered when the {@literal "add"} button is clicked.
     */
    private void saveButtonActionPerformed(final ActionEvent evt) {
        addEntry();
    }

    private void editButtonActionPerformed(final ActionEvent evt) {
        editEntry();
    }

    /**
     * Triggered when the {@literal "file picker"} button is clicked.
     */
    private void browseButtonActionPerformed(final ActionEvent evt) {
        String lastPath = getLastPath();
        selectedPath = getDataDirectory(lastPath);
        // yes, the "!=" is intentional! getDataDirectory(String) will return
        // the exact String it is given if the user cancelled the file picker
        if (selectedPath != lastPath) {
            directoryField.setText(selectedPath);
            setLastPath(selectedPath);
        }
    }

    /**
     * Returns the value of the {@link #PROP_LAST_PATH} McIDAS-V property.
     * 
     * @return Either the {@code String} representation of the last path 
     * selected by the user, or an empty {@code String}.
     */
    private String getLastPath() {
        McIDASV mcv = McIDASV.getStaticMcv();
        String path = "";
        if (mcv != null) {
            path = mcv.getObjectStore().get(PROP_LAST_PATH, "");
        }
        return path;
    }

    /**
     * Sets the value of the {@link #PROP_LAST_PATH} McIDAS-V property to be
     * the contents of {@code path}.
     * 
     * @param path New value for {@link #PROP_LAST_PATH}. {@code null} will be
     * converted to an empty {@code String}.
     */
    public void setLastPath(final String path) {
        String okayPath = (path != null) ? path : "";
        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv != null) {
            XmlObjectStore store = mcv.getObjectStore();
            store.put(PROP_LAST_PATH, okayPath);
            store.saveIfNeeded();
        }
    }

    /**
     * Calls {@link #dispose} if the dialog is visible.
     */
    private void cancelButtonActionPerformed(ActionEvent evt) {
        if (isDisplayable()) {
            dispose();
        }
    }

    /**
     * Poll the various UI components and attempt to construct valid ADDE
     * entries based upon the information provided by the user.
     * 
     * @param newEntry a boolean, {@code true} if we are adding a new entry.
     *
     * @return {@link Set} of entries that represent the user's input, or an
     * empty {@code Set} if the input was somehow invalid.
     */
    private Set<LocalAddeEntry> pollWidgets(boolean newEntry) {
        String group = datasetField.getText();
        String name = typeField.getText();
        String mask = getLastPath();
        
        // consider the UI in error if any field is blank
        if (group.isEmpty() || name.isEmpty() || mask.isEmpty()) {
            JOptionPane.showMessageDialog(this.getContentPane(),
                "Group, Name, or Mask field is empty, please correct this.");
            return Collections.emptySet();
        }

        // if there is something in the directoryField, that's the value we
        // should be using.
        if (!directoryField.getText().isEmpty()) {
            mask = directoryField.getText();
            setLastPath(mask);
        }
        
        AddeFormat format = (AddeFormat)formatComboBox.getSelectedItem();
        LocalAddeEntry entry = new LocalAddeEntry.Builder(name, group, mask, format).status(EntryStatus.ENABLED).build();
        
        // if adding a new entry, make sure dataset is not a duplicate
        if (newEntry) {
            String newGroup = entry.getGroup();
            for (AddeEntry storeEntry : entryStore.getEntrySet()) {
                String storeGroup = storeEntry.getGroup();
                if (newGroup.equals(storeGroup)) {
                    // only apply this restriction to MSG HRIT data
                    if (format.equals(AddeFormat.MSG_HRIT_FD) || (format.equals(AddeFormat.MSG_HRIT_HRV))) {
                        JOptionPane.showMessageDialog(this.getContentPane(),
                            "Dataset specified is a duplicate, not supported with MSG HRIT format.");
                        return Collections.emptySet();
                    }
                }
            }
        }
        return Collections.singleton(entry);
    }

    /**
     * Creates new {@link LocalAddeEntry}s based upon the contents of the dialog
     * and adds {@literal "them"} to the managed servers. If the dialog is
     * displayed, we call {@link #dispose()} and attempt to refresh the
     * server manager GUI if it is available.
     */
    private void addEntry() {
        Set<LocalAddeEntry> addedEntries = pollWidgets(true);
        entryStore.addEntries(addedEntries);
        if (isDisplayable()) {
            dispose();
        }
        if (managerController != null) {
            managerController.refreshDisplay();
        }
    }

    private void editEntry() {
        Set<LocalAddeEntry> newEntries = pollWidgets(false);
        Set<LocalAddeEntry> currentEntries = Collections.singleton(currentEntry);
        entryStore.replaceEntries(currentEntries, newEntries);
        if (isDisplayable()) {
            dispose();
        }
        if (managerController != null) {
            managerController.refreshDisplay();
        }
    }

    /**
     * Ask the user for a data directory from which to create a MASK=
     * 
     * @param startDir If this is a valid path, then the file picker will 
     * (presumably) use that as its initial location. Should not be 
     * {@code null}?
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
    private static class TooltipComboBoxRenderer extends BasicComboBoxRenderer {
        @Override public Component getListCellRendererComponent(JList list, 
            Object value, int index, boolean isSelected, boolean cellHasFocus) 
        {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                if (value instanceof AddeFormat) {
                    list.setToolTipText(((AddeFormat)value).getTooltip());
                }
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
    private JTextField datasetField;
    private JTextField directoryField;
    private JComboBox<AddeFormat> formatComboBox;
    private JTextField typeField;
    // End of variables declaration
}
