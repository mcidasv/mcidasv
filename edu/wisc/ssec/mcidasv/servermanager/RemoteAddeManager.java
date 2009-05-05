package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;

import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.table.AbstractTableModel;

import edu.wisc.ssec.mcidasv.McIDASV;

// this is accessible via the tools menu or some such thing and allows complete
// control over the available entries.
public class RemoteAddeManager extends javax.swing.JPanel {

    /** Reference back to the {@literal "main"} McIDAS-V object. */
    private final McIDASV mcv;

    /** 
     * The {@link EntryStore} that contains the set of servers being 
     * managed. 
     */
    private final EntryStore entryStore;

    /** Creates new form RemoteAddeManager */
    public RemoteAddeManager(final McIDASV mcv, final EntryStore entryStore) {
        this.mcv = mcv;
        this.entryStore = entryStore;
        initComponents();
    }

    /**
     * Creates a new window and displays the contents of the server manager 
     * within it.
     */
    public void showManager() {
        JFrame frame = new JFrame("Remote ADDE Data Sources");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(this);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Creates a new dialog and displays the server editor within it.
     */
    private void showAddEntryDialog() {
        JDialog dialog = new JDialog((JFrame)null, "Add New ADDE Server", true);
        AddRemoteAddeEntry entryPanel = new AddRemoteAddeEntry();
        dialog.setContentPane(entryPanel);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {

        entryTable.setModel(new AddeManagerTableModel(entryStore));
        entryTablePane.setViewportView(entryTable);

        newServerButton.setText("Add New Server");
        newServerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newServerButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(newServerButton);

        editServerButton.setText("Edit Server");
        editServerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editServerButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(editServerButton);

        removeServerButton.setText("Remove Servers");
        removeServerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeServerButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(removeServerButton);

        importButton.setText("Import MCTABLE");
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(importButton);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(cancelButton);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, entryTablePane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 691, Short.MAX_VALUE)
                    .add(buttonPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 691, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(26, 26, 26)
                .add(entryTablePane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE)
                .add(18, 18, 18)
                .add(buttonPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }

    /**
     * Handles the user clicking on the {@literal "Add Server"} button.
     * 
     * @param evt
     */
    private void newServerButtonActionPerformed(java.awt.event.ActionEvent evt) {
        showAddEntryDialog();
    }

    /**
     * Handles the user clicking on the {@literal "Edit Server"} button.
     * 
     * @param evt
     */
    private void editServerButtonActionPerformed(java.awt.event.ActionEvent evt) {
    }

    /**
     * Handles the user clicking on the {@literal "Remove Server"} button.
     * 
     * @param evt
     */
    private void removeServerButtonActionPerformed(java.awt.event.ActionEvent evt) {
    }

    /**
     * Handles the user clicking on the {@literal "Import from MCTABLE"}
     * button.
     * 
     * @param evt
     */
    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {
    }

    /**
     * Handles the user clicking on the {@literal "Cancel"} button.
     * 
     * @param evt Event to handle.
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private static class AddeManagerTableModel extends AbstractTableModel {
        /** Labels that appear as the column headers. */
        private final String[] columnNames = { 
            "Server", "Group", "Username", "Project #", "Type", "Source", 
            "Validity" 
        };

        /** Entries that currently populate the server manager. */
        private final List<RemoteAddeEntry> entries = arrList();

        /** {@link EntryStore} used to query and apply changes. */
        private final EntryStore entryStore;

        /**
         * Builds a table model that can be used to represent the contents of 
         * a {@link EntryStore}.
         * 
         * @param entryStore {@code EntryStore} to use as the backed 
         * {@literal "data store"}. Cannot be {@code null}.
         * 
         * @throws NullPointerException if {@code entryStore} is {@code null}.
         */
        public AddeManagerTableModel(final EntryStore entryStore) {
            if (entryStore == null)
                throw new NullPointerException("Cannot query a null EntryStore");
            this.entryStore = entryStore;
            entries.addAll(entryStore.getEntrySet());
        }

        /**
         * Returns the length of {@link #columnNames}.
         * 
         * @return The number of columns.
         */
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Returns the number of entries being managed.
         */
        public int getRowCount() {
            return entries.size();
        }

        /**
         * Returns the column name associated with {@code column}.
         * 
         * @return One of {@link #columnNames}.
         */
        public String getColumnName(int column) {
            return columnNames[column];
        }

        /**
         * Finds the value at the given coordinates.
         */
        public Object getValueAt(int row, int column) {
            RemoteAddeEntry entry = entries.get(row);
            if (entry == null)
                throw new IndexOutOfBoundsException(); // questionable...

            switch (column) {
                case 0: return entry.getAddress();
                case 1: return entry.getGroup();
                case 2: return entry.getAccount().getUsername();
                case 3: return entry.getAccount().getProject();
                case 4: return entry.getEntryType();
                case 5: return entry.getEntrySource();
                case 6: return entry.getEntryValidity();
                default: throw new IndexOutOfBoundsException();
            }
        }
    }

    /* boring gui stuff */
    private final javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
    private final javax.swing.JButton cancelButton = new javax.swing.JButton();
    private final javax.swing.JButton editServerButton = new javax.swing.JButton();
    private final javax.swing.JTable entryTable = new javax.swing.JTable();
    private final javax.swing.JScrollPane entryTablePane = new javax.swing.JScrollPane();
    private final javax.swing.JButton importButton = new javax.swing.JButton();
    private final javax.swing.JButton newServerButton = new javax.swing.JButton();
    private final javax.swing.JButton removeServerButton = new javax.swing.JButton();
}
