package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import java.awt.Component;
import java.io.File;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import edu.wisc.ssec.mcidasv.servermanager.LocalAddeEntry.AddeFormats;

public class LocalEntryEditor extends javax.swing.JDialog {

    private final TabbedAddeManager managerController;
    private final EntryStore entryStore;

    /** Creates new form LocalEntryEditor */
    public LocalEntryEditor(java.awt.Frame parent, boolean modal, final TabbedAddeManager manager, final EntryStore store) {
        super(manager, modal);
        this.managerController = manager;
        this.entryStore = store;
        initComponents(LocalAddeEntry.INVALID_ENTRY);
    }

    public LocalEntryEditor(java.awt.Frame parent, boolean modal, final TabbedAddeManager manager, final EntryStore store, final LocalAddeEntry entry) {
        super(manager, modal);
        this.managerController = manager;
        this.entryStore = store;
        initComponents(entry);
    }

    @SuppressWarnings("unchecked")
    private void initComponents(final LocalAddeEntry initEntry) {
        java.awt.GridBagConstraints gridBagConstraints;

        mainPanel = new javax.swing.JPanel();
        datasetLabel = new javax.swing.JLabel();
        datasetField = new javax.swing.JTextField();
        typeLabel = new javax.swing.JLabel();
        typeField = new javax.swing.JTextField();
        formatLabel = new javax.swing.JLabel();
        formatComboBox = new javax.swing.JComboBox();
        directoryLabel = new javax.swing.JLabel();
        directoryButton = new javax.swing.JButton();
        buttonPanel = new javax.swing.JPanel();
        addButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Add Local Dataset");

        mainPanel.setLayout(new java.awt.GridBagLayout());

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

        formatComboBox.setModel(new javax.swing.DefaultComboBoxModel(new Object[] { AddeFormats.MCIDAS_AREA, AddeFormats.AMSRE_L1B, AddeFormats.AMSRE_RAIN, AddeFormats.GINI, AddeFormats.LRIT_GOES9, AddeFormats.LRIT_GOES10, AddeFormats.LRIT_GOES11, AddeFormats.LRIT_GOES12, AddeFormats.LRIT_MET5, AddeFormats.LRIT_MET7, AddeFormats.LRIT_MTSAT1R, AddeFormats.METEOSAT_OPENMTP, AddeFormats.METOP_AVHRR, AddeFormats.MODIS_L1B_MOD02, AddeFormats.MODIS_L2_MOD06, AddeFormats.MODIS_L2_MOD07, AddeFormats.MODIS_L2_MOD35, AddeFormats.MODIS_L2_MOD04, AddeFormats.MODIS_L2_MOD28, AddeFormats.MODIS_L2_MODR, AddeFormats.MSG_HRIT_FD, AddeFormats.MSG_HRIT_HRV, AddeFormats.MTSAT_HRIT, AddeFormats.NOAA_AVHRR_L1B, AddeFormats.SSMI, AddeFormats.TRMM }));
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

        addButton.setText("Add Dataset");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
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

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {
        addEntry();
    }

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {
        String selectedPath = getDataDirectory("");
        System.err.println("browseButton: path="+selectedPath);
        if (!selectedPath.equals("")) {
            if (selectedPath.length() > 19) {
                directoryButton.setText(selectedPath.substring(0, 16) + "...");
                directoryButton.setToolTipText(selectedPath);
            } else {
                directoryButton.setText(selectedPath);
            }
        }
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (isDisplayable())
            dispose();
    }

    // TODO: less stupid
    private Set<LocalAddeEntry> pollWidgets() {
        return newLinkedHashSet();
    }

    private void addEntry() {
        Set<LocalAddeEntry> addedEntries = pollWidgets();
        entryStore.addEntries(addedEntries);
        if (isDisplayable())
            dispose();
        managerController.refreshDisplay();
    }

    /**
     * Get a short directory name representation, suitable for a button label
     * 
     * @param longString
     * 
     * @return
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
     * @param
     * 
     * @return
     */
    private String getDataDirectory(final String startDir) {
        JFileChooser fileChooser = new JFileChooser(startDir);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        switch (fileChooser.showOpenDialog(this)) {
            case JFileChooser.APPROVE_OPTION:
                return fileChooser.getSelectedFile().getAbsolutePath();
            case JFileChooser.CANCEL_OPTION:
                return startDir;
            default:
                return startDir;
        }
    }

    private class TooltipComboBoxRenderer extends BasicComboBoxRenderer {
        @Override public Component getListCellRendererComponent(JList list, 
            Object value, int index, boolean isSelected, boolean cellHasFocus) 
        {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                if (value != null && (value instanceof AddeFormats))
                    list.setToolTipText(((AddeFormats)value).getDescription());
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
