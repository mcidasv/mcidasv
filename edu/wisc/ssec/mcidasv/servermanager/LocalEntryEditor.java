package edu.wisc.ssec.mcidasv.servermanager;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

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

        formatComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "McIDAS AREA", "AMSR-E L1b", "AMSR-E Rain Product", "GINI", "LRIT GOES9", "LRIT GOES10", "LRIT GOES11", "LRIT GOES12", "LRIT MET5", "LRIT MET7", "LRIT MTSAT1R", "Meteosat OpenMTP", "Metop AVHRR L1b", "MODIS L1b MOD02", "MODIS L2 MOD06", "MODIS L2 MOD07", "MODIS L2 MOD35", "MODIS L2 MOD04", "MODIS L2 MOD28", "MODIS_L2 MODR", "MSG HRIT FD", "MSG HRIT HRV", "MTSAT HRIT", "NOAA AVHRR L1b", "SSMI", "TRMM" }));
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
        System.err.println("addButton: TODO");
    }

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {
        System.err.println("browseButton: TODO");
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (isDisplayable())
            dispose();
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
