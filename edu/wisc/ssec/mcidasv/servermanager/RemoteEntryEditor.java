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
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import static edu.wisc.ssec.mcidasv.util.Contract.checkArg;
import static edu.wisc.ssec.mcidasv.util.Contract.notNull;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newMap;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.set;
import static edu.wisc.ssec.mcidasv.util.McVGuiUtils.runOnEDT;

import java.awt.Color;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
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

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.util.LogUtil;

import edu.wisc.ssec.mcidas.adde.AddeServerInfo;
import edu.wisc.ssec.mcidas.adde.AddeTextReader;
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
public class RemoteEntryEditor extends javax.swing.JDialog {

    private static final Logger logger = LoggerFactory.getLogger(RemoteEntryEditor.class);

    /** Default port for remote ADDE servers. */
    public static final int ADDE_PORT = 112;

    /** Possible entry verification states. */
    public enum AddeStatus { PREFLIGHT, BAD_SERVER, BAD_ACCOUNTING, NO_METADATA, OK, BAD_GROUP };

    /** Number of threads in the thread pool. */
    private static final int POOL = 5;

    /** 
     * {@link String#format(String, Object...)}-friendly string for building a
     * request to read a server's PUBLIC.SRV.
     */
    private static final String publicSrvFormat = "adde://%s/text?compress=gzip&port=112&debug=false&version=1&user=%s&proj=%s&file=PUBLIC.SRV";

    /** Whether or not to input in the dataset, username, and project fields should be uppercased. */
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
    private final Set<javax.swing.JTextField> badFields = newLinkedHashSet();

    /** The server manager GUI. Be aware that this can be {@code null}. */
    private final TabbedAddeManager managerController;

    /** Reference back to the server manager. */
    private final EntryStore entryStore;

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
        super((javax.swing.JDialog)null, true);
        this.entryStore = entryStore;
        this.managerController = null;
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
        this.managerController = manager;
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
                    // err... wtf?
                }
//                entries.add(builder.build());
            }
        }
        return entries;
    }

    private void disposeDisplayable(final boolean refreshManager) {
        if (isDisplayable()) {
            dispose();
        }
        if (refreshManager && managerController != null) {
            managerController.refreshDisplay();
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
        logger.trace("currentEntries={}", currentEntries);
        disposeDisplayable(true);
    }

    /**
     * Attempts to verify that the current contents of the GUI are
     * {@literal "valid"}.
     */
    private void verifyInput() {
//        Set<RemoteAddeEntry> unverifiedEntries = pollWidgets(true);
//        Set<EntryType> validTypes = CollectionHelpers.newLinkedHashSet();
//        for (RemoteAddeEntry entry : entries) {
//            EntryType type = entry.getEntryType();
//            if (validTypes.contains(type))
//                continue;
//
//            String server = entry.getAddress();
//            String dataset = entry.getGroup();
//
//            setStatus("Checking "+server+'/'+dataset+" for accessible "+type+" data...");
//            AddeStatus status = RemoteAddeVerification.checkEntry(entry);
//            if (status == AddeStatus.OK) {
//                setStatus("Verified that "+server+'/'+dataset+" has accessible "+type+" data.");
//                validTypes.add(type);
//            } else if (status == AddeStatus.BAD_SERVER) {
//                setStatus("Could not connect to "+server);
//                setBadField(serverField, true);
//                return;
//            } else if (status == AddeStatus.BAD_ACCOUNTING) {
//                setStatus("Could not access "+server+'/'+dataset+" with current accounting information...");
//                setBadField(userField, true);
//                setBadField(projField, true);
//                return;
//            } else if (status == AddeStatus.BAD_GROUP) {
//                // err...
//            } else {
//                setStatus("Unknown status returned: "+status);
//                return;
//            }
//        }
        resetBadFields();
        Set<RemoteAddeEntry> unverifiedEntries = pollWidgets(true);
        
        // the editor GUI only works with one server address at a time. so 
        // although there may be several RemoteAddeEntry objs, they'll all have
        // the same address and the follow *isn't* as dumb as it looks!
        if (!unverifiedEntries.isEmpty()) {
            if (!checkHost(unverifiedEntries.toArray(new RemoteAddeEntry[0])[0])) {
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
        for (javax.swing.JTextField field : fields) {
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
        entryPanel = new javax.swing.JPanel();
        serverLabel = new javax.swing.JLabel();
        serverField = new javax.swing.JTextField();
        datasetLabel = new javax.swing.JLabel();
        datasetField = new McVTextField();
        acctBox = new javax.swing.JCheckBox();
        userLabel = new javax.swing.JLabel();
        userField = new McVTextField();
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

        boolean forceCaps = getForceMcxCaps();
        datasetField.setUppercase(forceCaps);
        userField.setUppercase(forceCaps);

        if (initEntries == RemoteAddeEntry.INVALID_ENTRIES) {
            setTitle("Define New Remote Dataset");
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
        capBox.setSelected(forceCaps);
        capBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
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

        typePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Dataset Types"));

        java.awt.event.ActionListener typeInputListener = new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
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

        if (initEntries == RemoteAddeEntry.INVALID_ENTRIES) {
            verifyAddButton.setText("Verify and Add Server");
        } else {
            verifyAddButton.setText("Verify and Save Changes");
        }
        verifyAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
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
        verifyServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verifyServerActionPerformed(evt);
            }
        });

        if (initEntries == RemoteAddeEntry.INVALID_ENTRIES) {
            addServer.setText("Add Server");
        } else {
            addServer.setText("Save Changes");
        }
        addServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (initEntries == RemoteAddeEntry.INVALID_ENTRIES) {
                    addServerActionPerformed(evt);
                } else {
                    editServerActionPerformed(evt);
                }
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

        if (initEntries != null && !RemoteAddeEntry.INVALID_ENTRIES.equals(initEntries)) {
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
    


    private void acctBoxActionPerformed(java.awt.event.ActionEvent evt) {
        assert SwingUtilities.isEventDispatchThread();
        resetBadFields();
        boolean enabled = acctBox.isSelected();
        userField.setEnabled(enabled);
        projField.setEnabled(enabled);
        verifyAddButton.setEnabled(true);
        verifyServer.setEnabled(true);
    }

    private void capBoxActionPerformed(java.awt.event.ActionEvent evt) {
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

    private void verifyAddButtonActionPerformed(java.awt.event.ActionEvent evt) {
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

    private void verifyEditButtonActionPerformed(java.awt.event.ActionEvent evt) {
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

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        setEditorAction(EditorAction.CANCELLED);
        disposeDisplayable(false);
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {
        setEditorAction(EditorAction.CANCELLED);
        disposeDisplayable(false);
    }

    private void verifyServerActionPerformed(java.awt.event.ActionEvent evt) {
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

    private void addServerActionPerformed(java.awt.event.ActionEvent evt) {
        setEditorAction(EditorAction.ADDED);
        addEntry();
    }

    private void editServerActionPerformed(java.awt.event.ActionEvent evt) {
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
     * 
     * @see #checkHost(RemoteAddeEntry)
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
                if (checkHost(entry)) {
                    goodEntries.add(entry);
                    hostStatus.put(host, Boolean.TRUE);
                } else {
                    hostStatus.put(host, Boolean.FALSE);
                }
            }
        }
        return goodEntries;
    }

//    YOU REALLY WANT TO PORT checkGroups
    public Set<RemoteAddeEntry> checkGroups(final Set<RemoteAddeEntry> entries) {
        Contract.notNull(entries, "entries cannot be null");
        if (entries.isEmpty()) {
            return Collections.emptySet();
        }

        Set<RemoteAddeEntry> verified = newLinkedHashSet();
        EnumSet<AddeStatus> statuses = EnumSet.noneOf(AddeStatus.class);
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
//                if (status == AddeStatus.OK) {
//                    verified.add(entry);
//                    setStatus("Found accessible "+entry.getEntryType().toString().toLowerCase()+" data.");
//                } else if (status != AddeStatus.BAD_GROUP) {
//                    setStatus("Could not locate "+entry.getEntryType()+" data within "+entry.getGroup());
//                    if (!statuses.contains(AddeStatus.OK)) {
//                        setBadField(datasetField, true);
//                    }
//                    //                    setBadField(datasetField, true);
//                } else if (status == AddeStatus.BAD_SERVER) {
//                    setStatus("Could not connect to "+entry.getAddress());
//                    if (!statuses.contains(AddeStatus.OK)) {
//                        
//                    }
////                    setBadField(serverField, true);
//                } else if (status == AddeStatus.BAD_ACCOUNTING) {
//                    setStatus("Could not access "+entry.getEntryText()+" with current accounting information...");
////                    setBadField(userField, true);
////                    setBadField(projField, true);
//                } else {
//                    setStatus("Unknown verification status: '"+status+".' Spooky!");
//                }
            }
//            if (!statuses.contains(AddeStatus.OK)) {
//                if (statuses.contains(AddeStatus.BAD_ACCOUNTING)) {
//                    setStatus("Incorrect accounting information.");
//                    setBadField(userField, true);
//                    setBadField(projField, true);
//                } else if (statuses.contains(AddeStatus.BAD_GROUP)) {
//                    setStatus("Dataset does not appear to be valid.");
//                    setBadField(datasetField, true);
//                } else if (statuses.contains(AddeStatus.BAD_SERVER)) {
//                    setStatus("Could not connect to the ADDE server.");
//                    setBadField(serverField, true);
//                } else {
//                    logger.warn("guru meditation error: statuses={}", statuses);
//                }
//            } else {
//                setStatus("Finished verifying.");
//            }
            
            
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

    public static AddeStatus checkEntry(final RemoteAddeEntry entry) {
        return checkEntry(true, entry);
    }

    /**
     * Attempts to verify whether or not the information in a given 
     * {@link RemoteAddeEntry} represents a valid remote ADDE server. If not,
     * the method tries to determine which parts of the entry are invalid.
     * 
     * @param entry The {@code RemoteAddeEntry} to check. Cannot be 
     * {@code null}.
     * 
     * @return The {@link AddeStatus} that represents the verification status
     * of {@code entry}.
     * 
     * @throws NullPointerException if {@code entry} is {@code null}.
     * 
     * @see AddeStatus
     */
    public static AddeStatus checkEntry(final boolean checkHost, final RemoteAddeEntry entry) {
        notNull(entry, "Cannot check a null entry");

        if (checkHost && !checkHost(entry)) {
            return AddeStatus.BAD_SERVER;
        }

        String server = entry.getAddress();
        String type = entry.getEntryType().toString();
        String username = entry.getAccount().getUsername();
        String project = entry.getAccount().getProject();
        String[] servers = { server };
        AddeServerInfo serverInfo = new AddeServerInfo(servers);

        // I just want to go on the record here: 
        // AddeServerInfo#setUserIDAndProjString(String) was not a good API 
        // decision.
        serverInfo.setUserIDandProjString("user="+username+"&proj="+project);
        int status = serverInfo.setSelectedServer(server, type);
        if (status == -2) {
            return AddeStatus.NO_METADATA;
        }
        if (status == -1) {
            return AddeStatus.BAD_ACCOUNTING;
        }

        serverInfo.setSelectedGroup(entry.getGroup());
        String[] datasets = serverInfo.getDatasetList();
        if (datasets != null && datasets.length > 0) {
            return AddeStatus.OK;
        } else {
            return AddeStatus.BAD_GROUP;
        }
        
    }

    private static Map<RemoteAddeEntry, AddeStatus> bulkPut(final Collection<RemoteAddeEntry> entries, final AddeStatus status) {
        Map<RemoteAddeEntry, AddeStatus> map = new LinkedHashMap<RemoteAddeEntry, AddeStatus>(entries.size());
        for (RemoteAddeEntry entry : entries) {
            map.put(entry, status);
        }
        return map;
    }
    
//    public static Map<RemoteAddeEntry, AddeStatus> checkEntries(final boolean checkHost, final List<RemoteAddeEntry> entries) {
//        notNull(entries);
//        
//        RemoteAddeEntry first = entries.get(0);
//        if (checkHost && !checkHost(first)) {
//            return bulkPut(entries, AddeStatus.BAD_SERVER);
//        }
//        
//        
//    }

    /**
     * Tries to connect to a given {@link RemoteAddeEntry} and read the list
     * of ADDE {@literal "groups"} available to the public.
     * 
     * @param entry The {@code RemoteAddeEntry} to query. Cannot be {@code null}.
     * 
     * @return The {@link Set} of public groups on {@code entry}.
     * 
     * @throws NullPointerException if {@code entry} is {@code null}.
     * @throws IllegalArgumentException if the server address is an empty 
     * {@link String}.
     */
    public static Set<String> readPublicGroups(final RemoteAddeEntry entry) {
        notNull(entry, "entry cannot be null");
        notNull(entry.getAddress());
        checkArg((entry.getAddress().length() == 0));

        String user = entry.getAccount().getUsername();
        if (user == null || user.length() == 0) {
            user = RemoteAddeEntry.DEFAULT_ACCOUNT.getUsername();
        }

        String proj = entry.getAccount().getProject();
        if (proj == null || proj.length() == 0) {
            proj = RemoteAddeEntry.DEFAULT_ACCOUNT.getProject();
        }

        String url = String.format(publicSrvFormat, entry.getAddress(), user, proj);

        Set<String> groups = newLinkedHashSet();

        AddeTextReader reader = new AddeTextReader(url);
        if (reader.getStatus().equals("OK")) {
            for (String line : (List<String>)reader.getLinesOfText()) {
                String[] pairs = line.trim().split(",");
                for (String pair : pairs) {
                    if (pair == null || pair.length() == 0 || !pair.startsWith("N1")) {
                        continue;
                    }
                    String[] keyval = pair.split("=");
                    if (keyval.length != 2 || keyval[0].length() == 0 || keyval[1].length() == 0 || !keyval[0].equals("N1")) {
                        continue;
                    }
                    groups.add(keyval[1]);
                }
//                groups.add(new AddeEntry(line).getGroup());
            }
        }

        return groups;
    }

    /**
     * Determines whether or not the server specified in {@code entry} is
     * listening on port 112.
     * 
     * @param entry Descriptor containing the server to check.
     * 
     * @return {@code true} if a connection was opened, {@code false} otherwise.
     * 
     * @throws NullPointerException if {@code entry} is null.
     */
    public static boolean checkHost(final RemoteAddeEntry entry) {
        notNull(entry, "entry cannot be null");
        String host = entry.getAddress();
        Socket socket = null;
        boolean connected = false;
        try { 
            socket = new Socket(host, ADDE_PORT);
            connected = true;
        } catch (UnknownHostException e) {
            logger.debug("can't resolve IP for '{}'", entry.getAddress());
            connected = false;
        } catch (IOException e) {
            logger.debug("IO problem while connecting to '{}': {}", entry.getAddress(), e.getMessage());
            connected = false;
        }
        try {
            socket.close();
        } catch (Exception e) {}
        logger.debug("host={} result={}", entry.getAddress(), connected);
        return connected;
    }
    
    // Variables declaration - do not modify
    private javax.swing.JCheckBox acctBox;
    private javax.swing.JButton addServer;
    private javax.swing.JButton cancelButton;
    private javax.swing.JCheckBox capBox;
    private McVTextField datasetField;
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
    private McVTextField userField;
    private javax.swing.JLabel userLabel;
    private javax.swing.JButton verifyAddButton;
    private javax.swing.JButton verifyServer;
    // End of variables declaration

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

        public StatusWrapper call() throws Exception {
            entryStatus.setStatus(checkEntry(entryStatus.getEntry()));
            return entryStatus;
        }
    }
}
