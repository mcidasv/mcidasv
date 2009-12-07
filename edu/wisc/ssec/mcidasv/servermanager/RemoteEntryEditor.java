package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.set;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import edu.wisc.ssec.mcidasv.ServerPreferenceManager.AddeStatus;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.util.CollectionHelpers;

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
        initComponents();
    }

   /**
     * Populates the applicable components with values dictated by the entries
     * within {@link #currentEntries}. Primarily useful for editing entries.
     */
    private void fillComponents() {
        if (currentEntries.isEmpty())
            return;

        List<RemoteAddeEntry> entries = new ArrayList<RemoteAddeEntry>(currentEntries);
        RemoteAddeEntry entry = entries.get(0); // currently only allowing single selection. this'll have to change.
        serverField.setText(entry.getAddress());
//        groupField.setText(entry.getGroup());

        if (entry.getAccount() != RemoteAddeEntry.DEFAULT_ACCOUNT) {
            acctBox.setSelected(true);
            userField.setText(entry.getAccount().getUsername());
            projField.setText(entry.getAccount().getProject());
        }

        // ugh
        if (entry.getEntryType() == EntryType.IMAGE)
            imageBox.setSelected(true);
        else if (entry.getEntryType() == EntryType.POINT)
            pointBox.setSelected(true);
        else if (entry.getEntryType() == EntryType.GRID)
            gridBox.setSelected(true);
        else if (entry.getEntryType() == EntryType.TEXT)
            textBox.setSelected(true);
        else if (entry.getEntryType() == EntryType.NAV)
            navBox.setSelected(true);
        else if (entry.getEntryType() == EntryType.RADAR)
            radarBox.setSelected(true);
    }

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

    /**
     * Displays a short status message in {@link #statusLabel}.
     *
     * @param msg Status message. Shouldn't be {@code null}.
     */
    private void setStatus(final String msg) {
        assert msg != null;
        statusLabel.setText(msg);
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
    private void setBadField(javax.swing.JTextField field, final boolean isBad) {
        assert field != null;
        assert field == serverField || field == datasetField || field == userField || field == projField;

        Color foreground = NORMAL_TEXT_COLOR;
        Color background = NORMAL_FIELD_COLOR;

        if (isBad) {
            foreground = ERROR_TEXT_COLOR;
            background = ERROR_FIELD_COLOR;
            badFields.add(field);
        } else {
            badFields.remove(field);
        }

        field.setForeground(foreground);
        field.setBackground(background);
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

//    private static void setForceMcxCaps(final boolean value) {
//        McIDASV mcv = McIDASV.getStaticMcv();
//        if (mcv == null)
//            return;
//
//        mcv.getStore().put(PREF_FORCE_CAPS, value);
//    }

//    private static boolean getForceMcxCaps() {
//        McIDASV mcv = McIDASV.getStaticMcv();
//        if (mcv == null)
//            return false;
//
//        return mcv.getStore().get(PREF_FORCE_CAPS, false);
//    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

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

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
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

        projLabel.setText("Project #:");

        capBox.setText("Automatically capitalize dataset and username?");

        org.jdesktop.layout.GroupLayout entryPanelLayout = new org.jdesktop.layout.GroupLayout(entryPanel);
        entryPanel.setLayout(entryPanelLayout);
        entryPanelLayout.setHorizontalGroup(
            entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(entryPanelLayout.createSequentialGroup()
                .add(entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, serverLabel)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, datasetLabel)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, userLabel)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, projLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(serverField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .add(capBox)
                    .add(acctBox)
                    .add(datasetField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .add(userField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .add(projField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE))
                .addContainerGap())
        );
        entryPanelLayout.setVerticalGroup(
            entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(entryPanelLayout.createSequentialGroup()
                .add(entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(serverLabel)
                    .add(serverField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(datasetLabel)
                    .add(datasetField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(16, 16, 16)
                .add(acctBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(userLabel)
                    .add(userField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(projLabel)
                    .add(projField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(capBox)
                .add(0, 0, Short.MAX_VALUE))
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

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusLabel)
                .addContainerGap(154, Short.MAX_VALUE))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .add(statusLabel)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(statusPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(typePanel, 0, 0, Short.MAX_VALUE)
                    .add(entryPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(verifyAddButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(verifyServer)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(addServer)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(entryPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(typePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 57, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(statusPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(verifyServer)
                    .add(addServer)
                    .add(cancelButton)
                    .add(verifyAddButton))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>

    private void acctBoxActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void verifyAddButtonActionPerformed(java.awt.event.ActionEvent evt) {
//        verifyInput();
//        if (!anyBadFields())
//            addEntry();
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        dispose();
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {
        dispose();
    }

    private void verifyServerActionPerformed(java.awt.event.ActionEvent evt) {
//        verifyInput();
    }

    private void addServerActionPerformed(java.awt.event.ActionEvent evt) {
//        addEntry();
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
