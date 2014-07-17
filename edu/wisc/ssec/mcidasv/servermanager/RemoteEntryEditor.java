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

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import static edu.wisc.ssec.mcidasv.util.Contract.notNull;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newMap;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.set;
import static edu.wisc.ssec.mcidasv.util.McVGuiUtils.runOnEDT;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.util.LogUtil;

import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EditorAction;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntrySource;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryValidity;
import edu.wisc.ssec.mcidasv.util.CollectionHelpers;
import edu.wisc.ssec.mcidasv.util.Contract;
import edu.wisc.ssec.mcidasv.util.McVTextField;

/**
 * Simple dialog that allows the user to define or modify {@link RemoteAddeEntry}s.
 */
@SuppressWarnings("serial")
public class RemoteEntryEditor extends JDialog {

    /** Logger object. */
    private static final Logger logger = LoggerFactory.getLogger(RemoteEntryEditor.class);

    /** Possible entry verification states. */
    public enum AddeStatus { PREFLIGHT, BAD_SERVER, BAD_ACCOUNTING, NO_METADATA, OK, BAD_GROUP };

    /** Number of threads in the thread pool. */
    private static final int POOL = 5;

    /** Whether or not to input in the dataset, username, and project fields should be uppercased. */
    private static final String PREF_FORCE_CAPS = "mcv.servers.forcecaps";

    /** Background {@link java.awt.Color Color} of an {@literal "invalid"} {@link JTextField JTextField}. */
    private static final Color ERROR_FIELD_COLOR = Color.PINK;

    /** Text {@link java.awt.Color Color} of an {@literal "invalid"} {@link JTextField JTextField}. */
    private static final Color ERROR_TEXT_COLOR = Color.WHITE;

    /** Background {@link java.awt.Color Color} of a {@literal "valid"} {@link JTextField JTextField}. */
    private static final Color NORMAL_FIELD_COLOR = Color.WHITE;

    /** Text {@link java.awt.Color Color} of a {@literal "valid"} {@link JTextField JTextField}. */
    private static final Color NORMAL_TEXT_COLOR = Color.BLACK;

    /**
     * Contains any {@code JTextField}s that may be in an invalid
     * (to McIDAS-V) state.
     */
    private final Set<JTextField> badFields = newLinkedHashSet(25);

    /** Reference back to the server manager. */
    private final EntryStore entryStore;

//    private final TabbedAddeManager manager;

    /** Current contents of the editor. */
    private final Set<RemoteAddeEntry> currentEntries = newLinkedHashSet();

    /** The last dialog action performed by the user. */
    private EditorAction editorAction = EditorAction.INVALID;

    /** Initial contents of {@link #serverField}. Be aware that {@code null} is allowed. */
    private final String serverText;

    /** Initial contents of {@link #datasetField}. Be aware that {@code null} is allowed. */
    private final String datasetText;

    /** Whether or not the editor is prompting the user to adjust input. */
    private boolean inErrorState = false;

    // if we decide to restore error overlays for known "bad" values.
//    private Set<RemoteAddeEntry> invalidEntries = CollectionHelpers.newLinkedHashSet();

    /**
     * Populates the server and dataset text fields with given {@link String}s.
     * This only works if the dialog <b>is not yet visible</b>.
     * 
     * <p>This is mostly useful when adding an entry from a chooser.
     * 
     * @param address Should be the address of a server, but empty and 
     * {@code null} values are allowed.
     * @param group Should be the name of a group/dataset on {@code server}, 
     * but empty and {@code null} values are allowed.
     */
    public RemoteEntryEditor(EntryStore entryStore, String address, String group) {
        super((JDialog)null, true);
        this.entryStore = entryStore;
//        this.manager = null;
        this.serverText = address;
        this.datasetText = group;
        initComponents(RemoteAddeEntry.INVALID_ENTRIES);
    }

    // TODO(jon): hold back on javadocs, this is likely to change
    public RemoteEntryEditor(java.awt.Frame parent, boolean modal, final TabbedAddeManager manager, final EntryStore store) {
        this(parent, modal, manager, store, RemoteAddeEntry.INVALID_ENTRIES);
    }

    public RemoteEntryEditor(java.awt.Frame parent, boolean modal, final TabbedAddeManager manager, final EntryStore store, final RemoteAddeEntry entry) {
        this(parent, modal, manager, store, CollectionHelpers.list(entry));
    }

    // TODO(jon): hold back on javadocs, this is likely to change
    public RemoteEntryEditor(java.awt.Frame parent, boolean modal, final TabbedAddeManager manager, final EntryStore store, final List<RemoteAddeEntry> entries) {
        super(manager, modal);
        this.entryStore = store;
//        this.manager = manager;
        this.serverText = null;
        this.datasetText = null;
        if (entries != RemoteAddeEntry.INVALID_ENTRIES) {
            currentEntries.addAll(entries);
        }
        initComponents(entries);
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
        Set<EntryType> selectedTypes = newLinkedHashSet();
        if (!ignoreCheckboxes) {
            if (imageBox.isSelected()) {
                selectedTypes.add(EntryType.IMAGE);
            }
            if (pointBox.isSelected()) {
                selectedTypes.add(EntryType.POINT);
            }
            if (gridBox.isSelected()) {
                selectedTypes.add(EntryType.GRID);
            }
            if (textBox.isSelected()) {
                selectedTypes.add(EntryType.TEXT);
            }
            if (navBox.isSelected()) {
                selectedTypes.add(EntryType.NAV);
            }
            if (radarBox.isSelected()) {
                selectedTypes.add(EntryType.RADAR);
            }
        } else {
            selectedTypes.addAll(set(EntryType.IMAGE, EntryType.POINT, EntryType.GRID, EntryType.TEXT, EntryType.NAV, EntryType.RADAR));
        }

        if (selectedTypes.isEmpty()) {
            selectedTypes.add(EntryType.UNKNOWN);
        }

        // deal with the user trying to add multiple groups at once (even though this UI doesn't work right with it)
        StringTokenizer tok = new StringTokenizer(dataset, ",");
        Set<String> newDatasets = newLinkedHashSet();
        while (tok.hasMoreTokens()) {
            newDatasets.add(tok.nextToken().trim());
        }

        // create a new entry for each group and its valid types.
        Set<RemoteAddeEntry> entries = newLinkedHashSet();
        for (String newGroup : newDatasets) {
            for (EntryType type : selectedTypes) {
                RemoteAddeEntry.Builder builder = new RemoteAddeEntry.Builder(host, newGroup).type(type).validity(EntryValidity.VERIFIED).source(EntrySource.USER);
                if (acctBox.isSelected()) {
                    builder = builder.account(username, project);
                }
                RemoteAddeEntry newEntry = builder.build();
                List<AddeEntry> matches = entryStore.searchWithPrefix(newEntry.asStringId());
                if (matches.isEmpty()) {
                    entries.add(newEntry);
                } else if (matches.size() == 1) {
                    AddeEntry matchedEntry = matches.get(0);
                    if (matchedEntry.getEntrySource() != EntrySource.SYSTEM) {
                        entries.add(newEntry);
                    } else {
                        entries.add((RemoteAddeEntry)matchedEntry);
                    }
                } else {
                    // results should only be empty or a single entry
                    logger.warn("server manager returned unexpected results={}", matches);
                }
            }
        }
        return entries;
    }

    private void disposeDisplayable(final boolean refreshManager) {
        if (isDisplayable()) {
            dispose();
        }
        TabbedAddeManager tmpController = TabbedAddeManager.getTabbedManager();
        if (refreshManager && (tmpController != null)) {
            tmpController.refreshDisplay();
        }
    }

    /**
     * Creates new {@link RemoteAddeEntry}s based upon the contents of the dialog
     * and adds {@literal "them"} to the managed servers. If the dialog is
     * displayed, we call {@link #dispose()} and attempt to refresh the
     * server manager GUI if it is available.
     */
    private void addEntry() {
        Set<RemoteAddeEntry> addedEntries = pollWidgets(false);
        entryStore.addEntries(addedEntries);
//        if (manager != null) {
//            manager.addEntries(addedEntries);
//        }
        disposeDisplayable(true);
    }

    /**
     * Replaces the entries within {@link #currentEntries} with new entries 
     * from {@link #pollWidgets(boolean)}. If the dialog is displayed, we call 
     * {@link #dispose()} and attempt to refresh the server manager GUI if it's 
     * available.
     */
    private void editEntry() {
        Set<RemoteAddeEntry> newEntries = pollWidgets(false);
        entryStore.replaceEntries(currentEntries, newEntries);
//        if (manager != null) {
//            manager.replaceEntries(currentEntries, newEntries);
//        }
        logger.trace("currentEntries={}", currentEntries);
        disposeDisplayable(true);
    }

    /**
     * Attempts to verify that the current contents of the GUI are
     * {@literal "valid"}.
     */
    private void verifyInput() {
        resetBadFields();
        Set<RemoteAddeEntry> unverifiedEntries = pollWidgets(true);

        // the editor GUI only works with one server address at a time. so 
        // although there may be several RemoteAddeEntry objs, they'll all have
        // the same address and the follow *isn't* as dumb as it looks!
        if (!unverifiedEntries.isEmpty()) {
            if (!RemoteAddeEntry.checkHost(unverifiedEntries.toArray(new RemoteAddeEntry[0])[0])) {
                setStatus("Could not connect to the given server.");
                setBadField(serverField, true);
                return;
            }
        } else {
            setStatus("Please specify ");
            setBadField(serverField, true);
            return;
        }

        setStatus("Contacting server...");
        Set<RemoteAddeEntry> verifiedEntries = checkGroups(unverifiedEntries);
        EnumSet<EntryType> presentTypes = EnumSet.noneOf(EntryType.class);
        if (!verifiedEntries.isEmpty()) {
            for (RemoteAddeEntry verifiedEntry : verifiedEntries) {
                presentTypes.add(verifiedEntry.getEntryType());
            }
            imageBox.setSelected(presentTypes.contains(EntryType.IMAGE));
            pointBox.setSelected(presentTypes.contains(EntryType.POINT));
            gridBox.setSelected(presentTypes.contains(EntryType.GRID));
            textBox.setSelected(presentTypes.contains(EntryType.TEXT));
            navBox.setSelected(presentTypes.contains(EntryType.NAV));
            radarBox.setSelected(presentTypes.contains(EntryType.RADAR));
        }
    }

    /**
     * Displays a short status message in {@link #statusLabel}.
     *
     * @param msg Status message. Shouldn't be {@code null}.
     */
    private void setStatus(final String msg) {
        assert msg != null;
        logger.debug("msg={}", msg);
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
    private void setBadField(final JTextField field, final boolean isBad) {
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
        Set<JTextField> fields = new LinkedHashSet<>(badFields);
        for (JTextField field : fields) {
            setBadField(field, false);
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
     * Controls the value associated with the {@link #PREF_FORCE_CAPS} preference.
     * 
     * @param value {@code true} causes user input into the dataset, username, 
     * and project fields to be capitalized.
     * 
     * @see #getForceMcxCaps()
     */
    private void setForceMcxCaps(final boolean value) {
        entryStore.getIdvStore().put(PREF_FORCE_CAPS, value);
    }

    /**
     * Returns the value associated with the {@link #PREF_FORCE_CAPS} preference.
     * 
     * @see #setForceMcxCaps(boolean)
     */
    private boolean getForceMcxCaps() {
        return entryStore.getIdvStore().get(PREF_FORCE_CAPS, true);
    }

    // TODO(jon): oh man clean this junk up
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents(final List<RemoteAddeEntry> initEntries) {
        assert SwingUtilities.isEventDispatchThread();
        entryPanel = new JPanel();
        serverLabel = new JLabel();
        serverField = new JTextField();
        datasetLabel = new JLabel();
        datasetField = new McVTextField();
        acctBox = new JCheckBox();
        userLabel = new JLabel();
        userField = new McVTextField();
        projLabel = new JLabel();
        projField = new JTextField();
        capBox = new JCheckBox();
        typePanel = new JPanel();
        imageBox = new JCheckBox();
        pointBox = new JCheckBox();
        gridBox = new JCheckBox();
        textBox = new JCheckBox();
        navBox = new JCheckBox();
        radarBox = new JCheckBox();
        statusPanel = new JPanel();
        statusLabel = new JLabel();
        verifyAddButton = new JButton();
        verifyServer = new JButton();
        addServer = new JButton();
        cancelButton = new JButton();

        boolean forceCaps = getForceMcxCaps();
        datasetField.setUppercase(forceCaps);
        userField.setUppercase(forceCaps);

        if (initEntries == RemoteAddeEntry.INVALID_ENTRIES) {
            setTitle("Add Remote Dataset");
        } else {
            setTitle("Edit Remote Dataset");
        }
        setResizable(false);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        serverLabel.setText("Server:");
        if (serverText != null) {
            serverField.setText(serverText);
        }

        datasetLabel.setText("Dataset:");
        if (datasetText != null) {
            datasetField.setText(datasetText);
        }

        acctBox.setText("Specify accounting information:");
        acctBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                acctBoxActionPerformed(evt);
            }
        });

        userLabel.setText("Username:");
        userField.setEnabled(acctBox.isSelected());

        projLabel.setText("Project #:");
        projField.setEnabled(acctBox.isSelected());

        capBox.setText("Automatically capitalize dataset and username?");
        capBox.setSelected(forceCaps);
        capBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                capBoxActionPerformed(evt);
            }
        });

        javax.swing.event.DocumentListener inputListener = new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent evt) {
                reactToValueChanges();
            }
            public void insertUpdate(javax.swing.event.DocumentEvent evt) {
                if (inErrorState) {
                    verifyAddButton.setEnabled(true);
                    verifyServer.setEnabled(true);
                    inErrorState = false;
                    resetBadFields();
                }
            }
            public void removeUpdate(javax.swing.event.DocumentEvent evt) {
                if (inErrorState) {
                    verifyAddButton.setEnabled(true);
                    verifyServer.setEnabled(true);
                    inErrorState = false;
                    resetBadFields();
                }
            }
        };

        serverField.getDocument().addDocumentListener(inputListener);
        datasetField.getDocument().addDocumentListener(inputListener);
        userField.getDocument().addDocumentListener(inputListener);
        projField.getDocument().addDocumentListener(inputListener);

        GroupLayout entryPanelLayout = new GroupLayout(entryPanel);
        entryPanel.setLayout(entryPanelLayout);
        entryPanelLayout.setHorizontalGroup(
            entryPanelLayout.createParallelGroup(LEADING)
            .addGroup(entryPanelLayout.createSequentialGroup()
                .addGroup(entryPanelLayout.createParallelGroup(LEADING)
                    .addComponent(serverLabel, TRAILING)
                    .addComponent(datasetLabel, TRAILING)
                    .addComponent(userLabel, TRAILING)
                    .addComponent(projLabel, TRAILING))
                .addPreferredGap(RELATED)
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
                .addPreferredGap(RELATED)
                .addGroup(entryPanelLayout.createParallelGroup(BASELINE)
                    .addComponent(datasetLabel)
                    .addComponent(datasetField, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                .addGap(16, 16, 16)
                .addComponent(acctBox)
                .addPreferredGap(RELATED)
                .addGroup(entryPanelLayout.createParallelGroup(BASELINE)
                    .addComponent(userLabel)
                    .addComponent(userField, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                .addPreferredGap(RELATED)
                .addGroup(entryPanelLayout.createParallelGroup(BASELINE)
                    .addComponent(projLabel)
                    .addComponent(projField, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                .addPreferredGap(RELATED)
                .addComponent(capBox)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        typePanel.setBorder(BorderFactory.createTitledBorder("Dataset Types"));

        ActionListener typeInputListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (inErrorState) {
                    verifyAddButton.setEnabled(true);
                    verifyServer.setEnabled(true);
                    inErrorState = false;
                    resetBadFields();
                }
            }
        };

        imageBox.setText("Image");
        imageBox.addActionListener(typeInputListener);
        typePanel.add(imageBox);

        pointBox.setText("Point");
        pointBox.addActionListener(typeInputListener);
        typePanel.add(pointBox);

        gridBox.setText("Grid");
        gridBox.addActionListener(typeInputListener);
        typePanel.add(gridBox);

        textBox.setText("Text");
        textBox.addActionListener(typeInputListener);
        typePanel.add(textBox);

        navBox.setText("Navigation");
        navBox.addActionListener(typeInputListener);
        typePanel.add(navBox);

        radarBox.setText("Radar");
        radarBox.addActionListener(typeInputListener);
        typePanel.add(radarBox);

        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));

        statusLabel.setText("Please provide the address of a remote ADDE server.");

        GroupLayout statusPanelLayout = new GroupLayout(statusPanel);
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

        if (initEntries == RemoteAddeEntry.INVALID_ENTRIES) {
            verifyAddButton.setText("Verify and Add Server");
        } else {
            verifyAddButton.setText("Verify and Save Changes");
        }
        verifyAddButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (initEntries == RemoteAddeEntry.INVALID_ENTRIES)
                    verifyAddButtonActionPerformed(evt);
                else
                    verifyEditButtonActionPerformed(evt);
            }
        });

        if (initEntries == RemoteAddeEntry.INVALID_ENTRIES) {
            verifyServer.setText("Verify Server");
        } else {
            verifyServer.setText("Verify Changes");
        }
        verifyServer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                verifyServerActionPerformed(evt);
            }
        });

        if (initEntries == RemoteAddeEntry.INVALID_ENTRIES) {
            addServer.setText("Add Server");
        } else {
            addServer.setText("Save Changes");
        }
        addServer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (initEntries == RemoteAddeEntry.INVALID_ENTRIES) {
                    addServerActionPerformed(evt);
                } else {
                    editServerActionPerformed(evt);
                }
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
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

        if ((initEntries != null) && !RemoteAddeEntry.INVALID_ENTRIES.equals(initEntries)) {
            RemoteAddeEntry initEntry = initEntries.get(0);
            boolean hasSystemEntry = false;
            for (RemoteAddeEntry entry : initEntries) {
                if (entry.getEntrySource() == EntrySource.SYSTEM) {
                    initEntry = entry;
                    hasSystemEntry = true;
                    break;
                }
            }
            serverField.setText(initEntry.getAddress());
            datasetField.setText(initEntry.getGroup());

            if (!RemoteAddeEntry.DEFAULT_ACCOUNT.equals(initEntry.getAccount())) {
                acctBox.setSelected(true);
                userField.setEnabled(true);
                userField.setText(initEntry.getAccount().getUsername());
                projField.setEnabled(true);
                projField.setText(initEntry.getAccount().getProject());
            }

            if (hasSystemEntry) {
                serverField.setEnabled(false);
                datasetField.setEnabled(false);
                acctBox.setEnabled(false);
                userField.setEnabled(false);
                projField.setEnabled(false);
                capBox.setEnabled(false);
            }

            for (RemoteAddeEntry entry : initEntries) {
                boolean nonDefaultSource = entry.getEntrySource() != EntrySource.SYSTEM;
                if (entry.getEntryType() == EntryType.IMAGE) {
                    imageBox.setSelected(true);
                    imageBox.setEnabled(nonDefaultSource);
                } else if (entry.getEntryType() == EntryType.POINT) {
                    pointBox.setSelected(true);
                    pointBox.setEnabled(nonDefaultSource);
                } else if (entry.getEntryType() == EntryType.GRID) {
                    gridBox.setSelected(true);
                    gridBox.setEnabled(nonDefaultSource);
                } else if (entry.getEntryType() == EntryType.TEXT) {
                    textBox.setSelected(true);
                    textBox.setEnabled(nonDefaultSource);
                } else if (entry.getEntryType() == EntryType.NAV) {
                    navBox.setSelected(true);
                    navBox.setEnabled(nonDefaultSource);
                } else if (entry.getEntryType() == EntryType.RADAR) {
                    radarBox.setSelected(true);
                    radarBox.setEnabled(nonDefaultSource);
                }
            }
        }
        pack();
    }// </editor-fold>

    private void acctBoxActionPerformed(ActionEvent evt) {
        assert SwingUtilities.isEventDispatchThread();
        resetBadFields();
        boolean enabled = acctBox.isSelected();
        userField.setEnabled(enabled);
        projField.setEnabled(enabled);
        verifyAddButton.setEnabled(true);
        verifyServer.setEnabled(true);
    }

    private void capBoxActionPerformed(ActionEvent evt) {
        assert SwingUtilities.isEventDispatchThread();
        boolean forceCaps = capBox.isSelected();
        datasetField.setUppercase(forceCaps);
        userField.setUppercase(forceCaps);
        setForceMcxCaps(forceCaps);
        if (!forceCaps) {
            return;
        }
        datasetField.setText(datasetField.getText().toUpperCase());
        userField.setText(userField.getText().toUpperCase());
    }

    private void verifyAddButtonActionPerformed(ActionEvent evt) {
        verifyInput();
        if (!anyBadFields()) {
            setEditorAction(EditorAction.ADDED_VERIFIED);
            addEntry();
        } else {
            inErrorState = true;
            verifyAddButton.setEnabled(false);
            verifyServer.setEnabled(false);
        }
    }

    private void verifyEditButtonActionPerformed(ActionEvent evt) {
        verifyInput();
        if (!anyBadFields()) {
            setEditorAction(EditorAction.EDITED_VERIFIED);
            editEntry();
        } else {
            inErrorState = true;
            verifyAddButton.setEnabled(false);
            verifyServer.setEnabled(false);
        }
    }

    private void cancelButtonActionPerformed(ActionEvent evt) {
        setEditorAction(EditorAction.CANCELLED);
        disposeDisplayable(false);
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {
        setEditorAction(EditorAction.CANCELLED);
        disposeDisplayable(false);
    }

    private void verifyServerActionPerformed(ActionEvent evt) {
        verifyInput();
        if (anyBadFields()) {
            // save poll widget state
            // toggle a "listen for *any* input event" switch to on
//            invalidEntries.clear();
//            invalidEntries.addAll(pollWidgets(false));
            inErrorState = true;
            verifyAddButton.setEnabled(false);
            verifyServer.setEnabled(false);
        }
    }

    private void addServerActionPerformed(ActionEvent evt) {
        setEditorAction(EditorAction.ADDED);
        addEntry();
    }

    private void editServerActionPerformed(ActionEvent evt) {
        setEditorAction(EditorAction.EDITED);
        editEntry();
    }

    private void reactToValueChanges() {
        assert SwingUtilities.isEventDispatchThread();
        if (inErrorState) {
            verifyAddButton.setEnabled(true);
            verifyServer.setEnabled(true);
            inErrorState = false;
            resetBadFields();
        }
    }

    /**
     * Attempt to verify a {@link Set} of {@link RemoteAddeEntry}s. Useful for
     * checking a {@literal "MCTABLE.TXT"} after importing.
     * 
     * @param entries {@code Set} of remote ADDE entries to validate. Cannot 
     * be {@code null}.
     * 
     * @return {@code Set} of {@code RemoteAddeEntry}s that McIDAS-V was able
     * to connect to. 
     * 
     * @throws NullPointerException if {@code entries} is {@code null}.
     */
    public Set<RemoteAddeEntry> checkHosts(final Set<RemoteAddeEntry> entries) {
        Contract.notNull(entries, "entries cannot be null");
        Set<RemoteAddeEntry> goodEntries = newLinkedHashSet();
        Set<String> checkedHosts = newLinkedHashSet();
        Map<String, Boolean> hostStatus = newMap();
        for (RemoteAddeEntry entry : entries) {
            String host = entry.getAddress();
            if (hostStatus.get(host) == Boolean.FALSE) {
                continue;
            } else if (hostStatus.get(host) == Boolean.TRUE) {
                goodEntries.add(entry);
            } else {
                checkedHosts.add(host);
                if (RemoteAddeEntry.checkHost(entry)) {
                    goodEntries.add(entry);
                    hostStatus.put(host, Boolean.TRUE);
                } else {
                    hostStatus.put(host, Boolean.FALSE);
                }
            }
        }
        return goodEntries;
    }

    public Set<RemoteAddeEntry> checkHosts2(final Set<RemoteAddeEntry> entries) {
      Contract.notNull(entries, "entries cannot be null");
      if (entries.isEmpty()) {
          return Collections.emptySet();
      }
      
      Set<RemoteAddeEntry> verified = newLinkedHashSet(entries.size());
      Set<String> hosts = newLinkedHashSet(entries.size());
//      Set<String> validHosts = newLinkedHashSet(entries.size());
      
      ExecutorService exec = Executors.newFixedThreadPool(POOL);
      CompletionService<StatusWrapper> ecs = new ExecutorCompletionService<StatusWrapper>(exec);
      for (RemoteAddeEntry entry : entries) {
          ecs.submit(new VerifyHostTask(new StatusWrapper(entry)));
      }

      try {
          for (int i = 0; i < entries.size(); i++) {
              StatusWrapper pairing = ecs.take().get();
              RemoteAddeEntry entry = pairing.getEntry();
              AddeStatus status = pairing.getStatus();
//              setStatus(entry.getAddress()+": attempting to connect...");
//              statuses.add(status);
//              entry2Status.put(entry, status);
              if (status == AddeStatus.OK) {
                  verified.add(entry);
//                  setStatus("Found host name "+entry.getAddress());
              }
          }
      } catch (InterruptedException e) {
          LogUtil.logException("interrupted while checking ADDE entries", e);
      } catch (ExecutionException e) {
          LogUtil.logException("ADDE validation execution error", e);
      } finally {
          exec.shutdown();
      }
      return verified;
    }
//    public Set<RemoteAddeEntry> checkHosts2(final Set<RemoteAddeEntry> entries) {
//        Contract.notNull(entries, "entries cannot be null");
//        if (entries.isEmpty()) {
//            return Collections.emptySet();
//        }
//        
//        Set<RemoteAddeEntry> verified = newLinkedHashSet(entries.size());
//        ExecutorService exec = Executors.newFixedThreadPool(POOL);
//        CompletionService<StatusWrapper> ecs = new ExecutorCompletionService<StatusWrapper>(exec);
////        Map<RemoteAddeEntry, AddeStatus> entry2Status = new LinkedHashMap<RemoteAddeEntry, AddeStatus>(entries.size());
//        
//        for (RemoteAddeEntry entry : entries) {
//            StatusWrapper check = new StatusWrapper(entry);
//            ecs.submit(new VerifyHostTask(check));
//        }
//        
//        try {
//            for (int i = 0; i < entries.size(); i++) {
//                StatusWrapper pairing = ecs.take().get();
//                RemoteAddeEntry entry = pairing.getEntry();
//                AddeStatus status = pairing.getStatus();
//                setStatus(entry.getAddress()+": attempting to connect...");
//                statuses.add(status);
////                entry2Status.put(entry, status);
//                if (status == AddeStatus.OK) {
//                    verified.add(entry);
////                    setStatus("Found host name "+entry.getAddress());
//                }
//            }
//        } catch (InterruptedException e) {
//            
//        } catch (ExecutionException e) {
//            
//        } finally {
//            exec.shutdown();
//        }
//
//        if (statuses.contains(AddeStatus.BAD_SERVER)) {
//            setStatus("Could not connect to the server.");
//            setBadField(serverField, true);
//        } else {
//            setStatus("Connected to server.");
//        }
////        
////        if (!statuses.contains(AddeStatus.OK)) {
////            if (statuses.contains(AddeStatus.BAD_ACCOUNTING)) {
////                setStatus("Incorrect accounting information.");
////                setBadField(userField, true);
////                setBadField(projField, true);
////            } else if (statuses.contains(AddeStatus.BAD_GROUP)) {
////                setStatus("Dataset does not appear to be valid.");
////                setBadField(datasetField, true);
////            } else if (statuses.contains(AddeStatus.BAD_SERVER)) {
////                setStatus("Could not connect to the ADDE server.");
////                setBadField(serverField, true);
////            } else {
////                logger.warn("guru meditation error: statuses={}", statuses);
////            }
////        } else {
////            setStatus("Finished verifying.");
////        }
//
//        return verified;
//    }
    
    public Set<RemoteAddeEntry> checkGroups(final Set<RemoteAddeEntry> entries) {
        Contract.notNull(entries, "entries cannot be null");
        if (entries.isEmpty()) {
            return Collections.emptySet();
        }

        Set<RemoteAddeEntry> verified = newLinkedHashSet(entries.size());
        Collection<AddeStatus> statuses = EnumSet.noneOf(AddeStatus.class);
        ExecutorService exec = Executors.newFixedThreadPool(POOL);
        CompletionService<StatusWrapper> ecs = new ExecutorCompletionService<StatusWrapper>(exec);
        Map<RemoteAddeEntry, AddeStatus> entry2Status = new LinkedHashMap<RemoteAddeEntry, AddeStatus>(entries.size());

        // submit new verification tasks to the pool's queue ... (apologies for the pun?)
        for (RemoteAddeEntry entry : entries) {
            StatusWrapper pairing = new StatusWrapper(entry);
            ecs.submit(new VerifyEntryTask(pairing));
        }

        // use completion service magic to only deal with finished verification tasks
        try {
            for (int i = 0; i < entries.size(); i++) {
                StatusWrapper pairing = ecs.take().get();
                RemoteAddeEntry entry = pairing.getEntry();
                AddeStatus status = pairing.getStatus();
                setStatus(entry.getEntryText()+": attempting verification...");
                statuses.add(status);
                entry2Status.put(entry, status);
                if (status == AddeStatus.OK) {
                    verified.add(entry);
                    setStatus("Found accessible "+entry.getEntryType().toString().toLowerCase()+" data.");
                }
            }
        } catch (InterruptedException e) {
            LogUtil.logException("interrupted while checking ADDE entries", e);
        } catch (ExecutionException e) {
            LogUtil.logException("ADDE validation execution error", e);
        } finally {
            exec.shutdown();
        }

        if (!statuses.contains(AddeStatus.OK)) {
            if (statuses.contains(AddeStatus.BAD_ACCOUNTING)) {
                setStatus("Incorrect accounting information.");
                setBadField(userField, true);
                setBadField(projField, true);
            } else if (statuses.contains(AddeStatus.BAD_GROUP)) {
                setStatus("Dataset does not appear to be valid.");
                setBadField(datasetField, true);
            } else if (statuses.contains(AddeStatus.BAD_SERVER)) {
                setStatus("Could not connect to the ADDE server.");
                setBadField(serverField, true);
            } else {
                logger.warn("guru meditation error: statuses={}", statuses);
            }
        } else {
            setStatus("Finished verifying.");
        }
        return verified;
    }

    private static Map<RemoteAddeEntry, AddeStatus> bulkPut(final Collection<RemoteAddeEntry> entries, final AddeStatus status) {
        Map<RemoteAddeEntry, AddeStatus> map = new LinkedHashMap<>(entries.size());
        for (RemoteAddeEntry entry : entries) {
            map.put(entry, status);
        }
        return map;
    }

    /**
     * Associates a {@link RemoteAddeEntry} with one of the states from 
     * {@link AddeStatus}.
     */
    private static class StatusWrapper {
        /** */
        private final RemoteAddeEntry entry;

        /** Current {@literal "status"} of {@link #entry}. */
        private AddeStatus status;

        /**
         * Builds an entry/status pairing.
         * 
         * @param entry The {@code RemoteAddeEntry} to wrap up.
         * 
         * @throws NullPointerException if {@code entry} is {@code null}.
         */
        public StatusWrapper(final RemoteAddeEntry entry) {
            notNull(entry, "cannot create a entry/status pair with a null descriptor");
            this.entry = entry;
        }

        /**
         * Set the {@literal "status"} of this {@link #entry} to a given 
         * {@link AddeStatus}.
         * 
         * @param status New status of {@code entry}.
         */
        public void setStatus(AddeStatus status) {
            this.status = status;
        }

        /**
         * Returns the current {@literal "status"} of {@link #entry}.
         * 
         * @return One of {@link AddeStatus}.
         */
        public AddeStatus getStatus() {
            return status;
        }

        /**
         * Returns the {@link RemoteAddeEntry} stored in this wrapper.
         * 
         * @return {@link #entry}
         */
        public RemoteAddeEntry getEntry() {
            return entry;
        }
    }

    /**
     * Represents an ADDE entry verification task. These are executed asynchronously 
     * by the completion service within {@link RemoteEntryEditor#checkGroups(Set)}.
     */
    private class VerifyEntryTask implements Callable<StatusWrapper> {
        private final StatusWrapper entryStatus;
        public VerifyEntryTask(final StatusWrapper descStatus) {
            notNull(descStatus, "cannot verify or set status of a null descriptor/status pair");
            this.entryStatus = descStatus;
        }

        @Override public StatusWrapper call() throws Exception {
            entryStatus.setStatus(RemoteAddeEntry.checkEntry(entryStatus.getEntry()));
            return entryStatus;
        }
    }
    
    private class VerifyHostTask implements Callable<StatusWrapper> {
        private final StatusWrapper entryStatus;
        public VerifyHostTask(final StatusWrapper descStatus) {
            entryStatus = notNull(descStatus, "cannot verify or set status of a null descriptor/status pair");
        }
        @Override public StatusWrapper call() throws Exception {
            boolean validHost = RemoteAddeEntry.checkHost(entryStatus.getEntry());
            if (validHost) {
                entryStatus.setStatus(AddeStatus.OK);
            } else {
                entryStatus.setStatus(AddeStatus.BAD_SERVER);
            }
            return entryStatus;
        }
    }

    // Variables declaration - do not modify
    private JCheckBox acctBox;
    private JButton addServer;
    private JButton cancelButton;
    private JCheckBox capBox;
    private McVTextField datasetField;
    private JLabel datasetLabel;
    private JPanel entryPanel;
    private JCheckBox gridBox;
    private JCheckBox imageBox;
    private JCheckBox navBox;
    private JCheckBox pointBox;
    private JTextField projField;
    private JLabel projLabel;
    private JCheckBox radarBox;
    private JTextField serverField;
    private JLabel serverLabel;
    private JLabel statusLabel;
    private JPanel statusPanel;
    private JCheckBox textBox;
    private JPanel typePanel;
    private McVTextField userField;
    private JLabel userLabel;
    private JButton verifyAddButton;
    private JButton verifyServer;
    // End of variables declaration
}
