package edu.wisc.ssec.mcidasv.servermanager;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.set;
import static edu.wisc.ssec.mcidasv.util.McVGuiUtils.runOnEDT;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.SwingWorker;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeVerification.AddeStatus;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.util.CollectionHelpers;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

public class RemoteEntryEditor extends javax.swing.JDialog {

    private static final String PREF_ENTERED_USER = "mcv.servers.defaultuser";
    private static final String PREF_ENTERED_PROJ = "mcv.servers.defaultproj";

    private static final String PREF_FORCE_CAPS = "mcv.servers.forcecaps";

    /** Background {@link Color} of an {@literal "invalid"} {@link javax.swing.JTextField}. */
    private static final Color ERROR_FIELD_COLOR = Color.PINK;

    /** Text {@link Color} of an {@literal "invalid"} {@link javax.swing.JTextField}. */
    private static final Color ERROR_TEXT_COLOR = Color.white;

    /** Background {@link Color} of a {@literal "valid"} {@link javax.swing.JTextField}. */
    private static final Color NORMAL_FIELD_COLOR = Color.WHITE;

    /** Text {@link Color} of a {@literal "valid"} {@link java.swing.JTextField}. */
    private static final Color NORMAL_TEXT_COLOR = Color.BLACK;

    /**
     * Contains any {@code JTextField}s that may be in an invalid
     * (to McIDAS-V) state.
     */
    private final Set<javax.swing.JTextField> badFields = CollectionHelpers.newLinkedHashSet();

    private final TabbedAddeManager managerController;

    /** Reference back to the server manager. */
    private final EntryStore entryStore;

    /** Current contents of the editor. */
    private final Set<RemoteAddeEntry> currentEntries = CollectionHelpers.newLinkedHashSet();

    private RemoteAddeEntry entry;

    /** Creates new form RemoteEntryEditor */
    public RemoteEntryEditor(java.awt.Frame parent, boolean modal, final TabbedAddeManager manager, final EntryStore store) {
        super(manager, modal);
        this.entryStore = store;
        this.managerController = manager;
        initComponents();
    }

    public RemoteEntryEditor(java.awt.Frame parent, boolean modal, final TabbedAddeManager manager, final EntryStore store, final RemoteAddeEntry entry) {
        super(manager, modal);
        this.entryStore = store;
        this.managerController = manager;
        this.entry = entry;
        currentEntries.add(entry);
        initComponents(entry);
    }

//   /**
//     * Populates the applicable components with values dictated by the entries
//     * within {@link #currentEntries}. Primarily useful for editing entries.
//     */
//    private void fillComponentsOld() {
//        if (currentEntries.isEmpty())
//            return;
//
//        List<RemoteAddeEntry> entries = new ArrayList<RemoteAddeEntry>(currentEntries);
//        RemoteAddeEntry entry = entries.get(0); // currently only allowing single selection. this'll have to change.
//        serverField.setText(entry.getAddress());
////        groupField.setText(entry.getGroup());
//
//        if (entry.getAccount() != RemoteAddeEntry.DEFAULT_ACCOUNT) {
//            acctBox.setSelected(true);
//            userField.setText(entry.getAccount().getUsername());
//            projField.setText(entry.getAccount().getProject());
//        }
//
//        // ugh
//        if (entry.getEntryType() == EntryType.IMAGE)
//            imageBox.setSelected(true);
//        else if (entry.getEntryType() == EntryType.POINT)
//            pointBox.setSelected(true);
//        else if (entry.getEntryType() == EntryType.GRID)
//            gridBox.setSelected(true);
//        else if (entry.getEntryType() == EntryType.TEXT)
//            textBox.setSelected(true);
//        else if (entry.getEntryType() == EntryType.NAV)
//            navBox.setSelected(true);
//        else if (entry.getEntryType() == EntryType.RADAR)
//            radarBox.setSelected(true);
//    }

    /**
     * Poll the various UI components and attempt to construct valid ADDE
     * entries based upon the information provided by the user.
     *
     * @param ignoreCheckboxes Whether or not the {@literal "type"} checkboxes
     * should get ignored. Setting this to {@code true} means that <i>all</i>
     * types are considered valid--which is useful when attempting to verify
     * the user's input.
     *
     * @return {@link Set} of entries that represent the user's input, or an
     * empty {@code Set} if the input was invalid somehow.
     */
    private Set<RemoteAddeEntry> pollWidgets(final boolean ignoreCheckboxes) {
        String host = serverField.getText().trim();
        String dataset = datasetField.getText().trim();
        String username = RemoteAddeEntry.DEFAULT_ACCOUNT.getUsername();
        String project = RemoteAddeEntry.DEFAULT_ACCOUNT.getProject();
        if (acctBox.isSelected()) {
            username = userField.getText().trim();
            project = projField.getText().trim();
        }

        // determine the "valid" types
        Set<EntryType> enabledTypes = CollectionHelpers.newLinkedHashSet();
        if (!ignoreCheckboxes) {
            if (imageBox.isSelected())
                enabledTypes.add(EntryType.IMAGE);
            if (pointBox.isSelected())
                enabledTypes.add(EntryType.POINT);
            if (gridBox.isSelected())
                enabledTypes.add(EntryType.GRID);
            if (textBox.isSelected())
                enabledTypes.add(EntryType.TEXT);
            if (navBox.isSelected())
                enabledTypes.add(EntryType.NAV);
            if (radarBox.isSelected())
                enabledTypes.add(EntryType.RADAR);
        } else {
            enabledTypes.addAll(set(EntryType.IMAGE, EntryType.POINT, EntryType.GRID, EntryType.TEXT, EntryType.NAV, EntryType.RADAR));
        }

        if (enabledTypes.isEmpty())
            enabledTypes.add(EntryType.UNKNOWN);

        // deal with the user trying to add multiple groups at once (even though this UI doesn't work right with it)
        StringTokenizer tok = new StringTokenizer(dataset, ",");
        Set<String> newDatasets = CollectionHelpers.newLinkedHashSet();
        while (tok.hasMoreTokens()) {
            newDatasets.add(tok.nextToken().trim());
        }

        // create a new entry for each group and its valid types.
        Set<RemoteAddeEntry> entries = CollectionHelpers.newLinkedHashSet();
        for (String newGroup : newDatasets) {
            for (EntryType type : enabledTypes) {


                RemoteAddeEntry.Builder builder = new RemoteAddeEntry.Builder(host, newGroup).type(type);
                if (acctBox.isSelected()) {
                    builder = builder.account(username, project);
                }
//                if (!currentEntries.isEmpty()) {
//
//                }
                entries.add(builder.build());
            }
        }
        return entries;
    }

    private void addEntry() {
        Set<RemoteAddeEntry> addedEntries = pollWidgets(false);
        entryStore.addEntries(addedEntries);
        if (isDisplayable())
            dispose();
        managerController.refreshDisplay();
    }

    /**
     * Attempts to verify that the current contents of the GUI are
     * {@literal "valid"}.
     */
    private void verifyInput() {
        Set<RemoteAddeEntry> entries = pollWidgets(true);
        Set<EntryType> validTypes = CollectionHelpers.newLinkedHashSet();
        for (RemoteAddeEntry entry : entries) {
            EntryType type = entry.getEntryType();
            if (validTypes.contains(type))
                continue;

            String server = entry.getAddress();
            String dataset = entry.getGroup();

            setStatus("Checking "+server+"/"+dataset+" for accessible "+type+" data...");
            AddeStatus status = RemoteAddeVerification.checkEntry(entry);
            if (status == AddeStatus.OK) {
                setStatus("Verified that "+server+"/"+dataset+" has accessible "+type+" data.");
                validTypes.add(type);
            } else if (status == AddeStatus.BAD_SERVER) {
                setStatus("Could not connect to "+server);
                setBadField(serverField, true);
                return;
            } else if (status == AddeStatus.BAD_ACCOUNTING) {
                setStatus("Could not access "+server+"/"+dataset+" with current accounting information...");
                setBadField(userField, true);
                setBadField(projField, true);
                return;
            } else if (status == AddeStatus.BAD_GROUP) {
                // err...
            } else {
                setStatus("Unknown status returned: "+status);
                return;
            }
        }

        if (validTypes.isEmpty()) {
            setStatus("Could not verify any types of data...");
            setBadField(datasetField, true);
        } else {
            setStatus("Server verification complete.");
            imageBox.setSelected(validTypes.contains(EntryType.IMAGE));
            pointBox.setSelected(validTypes.contains(EntryType.POINT));
            gridBox.setSelected(validTypes.contains(EntryType.GRID));
            textBox.setSelected(validTypes.contains(EntryType.TEXT));
            navBox.setSelected(validTypes.contains(EntryType.NAV));
            radarBox.setSelected(validTypes.contains(EntryType.RADAR));
        }
    }

//    private class Verifier extends SwingWorker<Set<EntryType>, AddeStatus> {
//        protected Set<EntryType> doInBackground() throws Exception {
//            
//        }
//        
//        
//    }
    
    /**
     * Displays a short status message in {@link #statusLabel}.
     *
     * @param msg Status message. Shouldn't be {@code null}.
     */
    private void setStatus(final String msg) {
        assert msg != null;
        runOnEDT(new Runnable() {
            public void run() {
                statusLabel.setText(msg);
            }
        });
        statusLabel.revalidate();
    }

    /**
     * Marks a {@code JTextField} as {@literal "valid"} or {@literal "invalid"}.
     * Mostly this just means that the field is highlighted in order to provide
     * to the user a sense of {@literal "what do I fix"} when something goes
     * wrong.
     *
     * @param field {@code JTextField} to mark.
     * @param isBad {@code true} means that the field is {@literal "invalid"},
     * {@code false} means that the field is {@literal "valid"}.
     */
    private void setBadField(final javax.swing.JTextField field, final boolean isBad) {
        assert field != null;
        assert field == serverField || field == datasetField || field == userField || field == projField;

        if (isBad) {
            badFields.add(field);
        } else {
            badFields.remove(field);
        }

        runOnEDT(new Runnable() {
            public void run() {
                if (isBad) {
                    field.setForeground(ERROR_TEXT_COLOR);
                    field.setBackground(ERROR_FIELD_COLOR);
                } else {
                    field.setForeground(NORMAL_TEXT_COLOR);
                    field.setBackground(NORMAL_FIELD_COLOR);
                }
            }
        });
        field.revalidate();
    }

   /**
     * Determines whether or not any fields are in an invalid state. Useful
     * for disallowing the user to add invalid entries to the server manager.
     *
     * @return Whether or not any fields are invalid.
     */
    private boolean anyBadFields() {
        assert badFields != null;
        return !badFields.isEmpty();
    }

    /**
     * Clear out {@link #badFields} and {@literal "set"} the field's status to
     * valid.
     */
    private void resetBadFields() {
        Set<javax.swing.JTextField> fields = new LinkedHashSet<javax.swing.JTextField>(badFields);
        for (javax.swing.JTextField field : fields)
            setBadField(field, false);
    }

    private static void setForceMcxCaps(final boolean value) {
        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv == null)
            return;

        mcv.getStore().put(PREF_FORCE_CAPS, value);
    }

    private static boolean getForceMcxCaps() {
        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv == null)
            return false;

        return mcv.getStore().get(PREF_FORCE_CAPS, false);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {
        initComponents(RemoteAddeEntry.INVALID_ENTRY);
    }

    private void initComponents(final RemoteAddeEntry initEntry) {

        entryPanel = new javax.swing.JPanel();
        serverLabel = new javax.swing.JLabel();
        serverField = new javax.swing.JTextField();
        datasetLabel = new javax.swing.JLabel();
        datasetField = new javax.swing.JTextField();
        acctBox = new javax.swing.JCheckBox();
        userLabel = new javax.swing.JLabel();
        userField = new javax.swing.JTextField();
        projLabel = new javax.swing.JLabel();
        projField = new javax.swing.JTextField();
        capBox = new javax.swing.JCheckBox();
        typePanel = new javax.swing.JPanel();
        imageBox = new javax.swing.JCheckBox();
        pointBox = new javax.swing.JCheckBox();
        gridBox = new javax.swing.JCheckBox();
        textBox = new javax.swing.JCheckBox();
        navBox = new javax.swing.JCheckBox();
        radarBox = new javax.swing.JCheckBox();
        statusPanel = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        verifyAddButton = new javax.swing.JButton();
        verifyServer = new javax.swing.JButton();
        addServer = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Define New Remote Dataset");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        serverLabel.setText("Server:");

        datasetLabel.setText("Dataset:");

        acctBox.setText("Specify accounting informaton:");
        acctBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acctBoxActionPerformed(evt);
            }
        });

        userLabel.setText("Username:");
        userField.setEnabled(acctBox.isSelected());

        projLabel.setText("Project #:");
        projField.setEnabled(acctBox.isSelected());

        capBox.setText("Automatically capitalize dataset and username?");

        javax.swing.GroupLayout entryPanelLayout = new javax.swing.GroupLayout(entryPanel);
        entryPanel.setLayout(entryPanelLayout);
        entryPanelLayout.setHorizontalGroup(
            entryPanelLayout.createParallelGroup(LEADING)
            .addGroup(entryPanelLayout.createSequentialGroup()
                .addGroup(entryPanelLayout.createParallelGroup(LEADING)
                    .addComponent(serverLabel, TRAILING)
                    .addComponent(datasetLabel, TRAILING)
                    .addComponent(userLabel, TRAILING)
                    .addComponent(projLabel, TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(entryPanelLayout.createParallelGroup(LEADING)
                    .addComponent(serverField, DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .addComponent(capBox)
                    .addComponent(acctBox)
                    .addComponent(datasetField, DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .addComponent(userField, DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .addComponent(projField, DEFAULT_SIZE, 419, Short.MAX_VALUE))
                .addContainerGap())
        );
        entryPanelLayout.setVerticalGroup(
            entryPanelLayout.createParallelGroup(LEADING)
            .addGroup(entryPanelLayout.createSequentialGroup()
                .addGroup(entryPanelLayout.createParallelGroup(BASELINE)
                    .addComponent(serverLabel)
                    .addComponent(serverField, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(entryPanelLayout.createParallelGroup(BASELINE)
                    .addComponent(datasetLabel)
                    .addComponent(datasetField, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                .addGap(16, 16, 16)
                .addComponent(acctBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(entryPanelLayout.createParallelGroup(BASELINE)
                    .addComponent(userLabel)
                    .addComponent(userField, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(entryPanelLayout.createParallelGroup(BASELINE)
                    .addComponent(projLabel)
                    .addComponent(projField, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(capBox)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        typePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Dataset Types"));

        imageBox.setText("Image");
        typePanel.add(imageBox);

        pointBox.setText("Point");
        typePanel.add(pointBox);

        gridBox.setText("Grid");
        typePanel.add(gridBox);

        textBox.setText("Text");
        typePanel.add(textBox);

        navBox.setText("Navigation");
        typePanel.add(navBox);

        radarBox.setText("Radar");
        typePanel.add(radarBox);

        statusPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Status"));

        statusLabel.setText("Please provide the address of a remote ADDE server.");

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusLabel)
                .addContainerGap(154, Short.MAX_VALUE))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusLabel)
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );

        verifyAddButton.setText("Verify and Add Server");
        verifyAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verifyAddButtonActionPerformed(evt);
            }
        });

        verifyServer.setText("Verify Server");
        verifyServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verifyServerActionPerformed(evt);
            }
        });

        addServer.setText("Add Server");
        addServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addServerActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(statusPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(typePanel, 0, 0, Short.MAX_VALUE)
                    .addComponent(entryPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(verifyAddButton)
                        .addPreferredGap(RELATED)
                        .addComponent(verifyServer)
                        .addPreferredGap(RELATED)
                        .addComponent(addServer)
                        .addPreferredGap(RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(entryPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                .addPreferredGap(UNRELATED)
                .addComponent(typePanel, PREFERRED_SIZE, 57, PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(statusPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(verifyServer)
                    .addComponent(addServer)
                    .addComponent(cancelButton)
                    .addComponent(verifyAddButton))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        if (initEntry != null && !initEntry.equals(RemoteAddeEntry.INVALID_ENTRY)) {
            serverField.setText(initEntry.getAddress());
            datasetField.setText(initEntry.getGroup());

            if (!initEntry.getAccount().equals(RemoteAddeEntry.DEFAULT_ACCOUNT)) {
                acctBox.setSelected(true);
                userField.setEnabled(true);
                userField.setText(initEntry.getAccount().getUsername());
                projField.setEnabled(true);
                projField.setText(initEntry.getAccount().getProject());
                
            }

            switch (initEntry.getEntryType()) {
                case IMAGE:
                    imageBox.setSelected(true);
                    break;
                case POINT:
                    pointBox.setSelected(true);
                    break;
                case GRID:
                    gridBox.setSelected(true);
                    break;
                case TEXT:
                    textBox.setSelected(true);
                    break;
                case NAV:
                    navBox.setSelected(true);
                    break;
                case RADAR:
                    radarBox.setSelected(true);
                    break;
            }
        }

        pack();
    }// </editor-fold>

    private void acctBoxActionPerformed(java.awt.event.ActionEvent evt) {
        McVGuiUtils.runOnEDT(new Runnable() {
            public void run() {
                boolean enabled = acctBox.isSelected();
                userField.setEnabled(enabled);
                projField.setEnabled(enabled);
            }
        });
    }

    private void verifyAddButtonActionPerformed(java.awt.event.ActionEvent evt) {
        System.err.println("remote entry editor: Verify+Add");
//        runOnEDT(new Runnable() {
//            public void run() {
                verifyInput();
                if (!anyBadFields())
                    addEntry();
//            }
//        });
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (isDisplayable())
            dispose();
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {
        if (isDisplayable())
            dispose();
    }

    private void verifyServerActionPerformed(java.awt.event.ActionEvent evt) {
        System.err.println("verify button!");
//        runOnEDT(new Runnable() {
//            public void run() {
                verifyInput();
//            }
//        });
    }

    private void addServerActionPerformed(java.awt.event.ActionEvent evt) {
        runOnEDT(new Runnable() {
            public void run() {
                addEntry();
            }
        });
    }

    /**
    * @param args the command line arguments
    */
//    public static void main(String args[]) {
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                RemoteEntryEditor dialog = new RemoteEntryEditor(new javax.swing.JFrame(), true);
//                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
//                    public void windowClosing(java.awt.event.WindowEvent e) {
//                        System.exit(0);
//                    }
//                });
//                dialog.setVisible(true);
//            }
//        });
//    }

    // Variables declaration - do not modify
    private javax.swing.JCheckBox acctBox;
    private javax.swing.JButton addServer;
    private javax.swing.JButton cancelButton;
    private javax.swing.JCheckBox capBox;
    private javax.swing.JTextField datasetField;
    private javax.swing.JLabel datasetLabel;
    private javax.swing.JPanel entryPanel;
    private javax.swing.JCheckBox gridBox;
    private javax.swing.JCheckBox imageBox;
    private javax.swing.JCheckBox navBox;
    private javax.swing.JCheckBox pointBox;
    private javax.swing.JTextField projField;
    private javax.swing.JLabel projLabel;
    private javax.swing.JCheckBox radarBox;
    private javax.swing.JTextField serverField;
    private javax.swing.JLabel serverLabel;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JCheckBox textBox;
    private javax.swing.JPanel typePanel;
    private javax.swing.JTextField userField;
    private javax.swing.JLabel userLabel;
    private javax.swing.JButton verifyAddButton;
    private javax.swing.JButton verifyServer;
    // End of variables declaration
}
