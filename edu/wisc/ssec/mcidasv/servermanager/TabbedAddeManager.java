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

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.Contract.notNull;
import static edu.wisc.ssec.mcidasv.util.McVGuiUtils.runOnEDT;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;

import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntrySource;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryValidity;
import edu.wisc.ssec.mcidasv.servermanager.AddeThread.McservEvent;
import edu.wisc.ssec.mcidasv.servermanager.EntryStore.Event;
import edu.wisc.ssec.mcidasv.servermanager.RemoteEntryEditor.AddeStatus;
import edu.wisc.ssec.mcidasv.ui.BetterJTable;
import edu.wisc.ssec.mcidasv.util.McVTextField.Prompt;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This class is the GUI frontend to {@link EntryStore} (the server manager).
 * It allows users to manipulate their local and remote ADDE data.
 */
// TODO(jon): don't forget to persist tab choice and window position. maybe also the "positions" of the scrollpanes (if possible).
// TODO(jon): GUI could look much better.
// TODO(jon): finish up the javadocs.
@SuppressWarnings({"serial", "AssignmentToStaticFieldFromInstanceMethod", "FieldCanBeLocal"})
public class TabbedAddeManager extends JFrame {

    /** Pretty typical logger object. */
    private final static Logger logger = LoggerFactory.getLogger(TabbedAddeManager.class);

    /** Path to the help resources. */
    private static final String HELP_TOP_DIR = "/docs/userguide";

    /** Help target for the remote servers. */
    private static final String REMOTE_HELP_TARGET = "idv.tools.remotedata";

    /** Help target for the local servers. */
    private static final String LOCAL_HELP_TARGET = "idv.tools.localdata";

    /** ID used to save/restore the last visible tab between sessions. */
    private static final String LAST_TAB = "mcv.adde.lasttab";

    /** ID used to save/restore the last directory that contained a MCTABLE.TXT. */
    private static final String LAST_IMPORTED = "mcv.adde.lastmctabledir";

    /** Size of the ADDE entry verification thread pool. */
    private static final int POOL = 2;

    /** Static reference to an instance of this class. Bad idea! */
    private static TabbedAddeManager staticTabbedManager;

    /**
     * These are the various {@literal "events"} that the server manager GUI
     * supports. These are published via the wonderful {@link EventBus#publish(Object)} method.
     */
    public enum Event { 
        /** The GUI was created. */
        OPENED,
        /** The GUI was hidden/minimized/etc. */
        HIDDEN,
        /** GUI was unhidden or some such thing. */
        SHOWN,
        /** The GUI was closed. */
        CLOSED
    }

    /** Reference to the actual server manager. */
    private final EntryStore serverManager;

    /** 
     * Entries stored within the server manager GUI. This may differ from the
     * contents of the server manager itself. 
     */
//    private final Set<AddeEntry> entrySet;

    /** */
    private final List<RemoteAddeEntry> selectedRemoteEntries;

    /** */
    private final List<LocalAddeEntry> selectedLocalEntries;

    /** */
    private JTextField importUser;

    /** */
    private JTextField importProject;

    /** Whether or not {@link #initComponents()} has been called. */
    private boolean guiInitialized = false;

    /**
     * Creates a standalone server manager GUI.
     */
    public TabbedAddeManager() {
        //noinspection AssignmentToNull
        AnnotationProcessor.process(this);
        this.serverManager = null;
//        this.entrySet = newLinkedHashSet();
        this.selectedLocalEntries = arrList();
        this.selectedRemoteEntries = arrList();

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                initComponents();
            }
        });
    }

    /**
     * Creates a server manager GUI that's linked back to the rest of McIDAS-V.
     * 
     * @param entryStore Server manager reference.
     * 
     * @throws NullPointerException if {@code entryStore} is {@code null}.
     */
    public TabbedAddeManager(final EntryStore entryStore) {
        notNull(entryStore, "Cannot pass a null server manager");
        AnnotationProcessor.process(this);
        this.serverManager = entryStore; 
        this.selectedLocalEntries = arrList();
        this.selectedRemoteEntries = arrList();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                initComponents();
            }
        });
    }

    /** 
     * Returns an instance of this class. The instance <i>should</i> correspond
     * to the one being used by the {@literal "rest"} of McIDAS-V.
     * 
     * @return Either an instance of this class or {@code null}.
     */
    public static TabbedAddeManager getTabbedManager() {
        return staticTabbedManager;
    }

//    public void addEntries(final Collection<? extends AddeEntry> entries) {
//        logger.trace("entries={}", entries);
//        entrySet.addAll(entries);
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                refreshDisplay();
//            }
//        });
//    }
//
//    public void replaceEntries(final Collection<? extends AddeEntry> currentEntries, final Collection<? extends AddeEntry> newEntries) {
//        logger.trace("currentEntries={} newEntries={}", currentEntries, newEntries);
//        entrySet.removeAll(currentEntries);
//        entrySet.addAll(newEntries);
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                refreshDisplay();
//            }
//        });
//    }
//
//    public void removeEntries(final Collection<? extends AddeEntry> entries) {
//        logger.trace("entries={}", entries);
//        entrySet.removeAll(entries);
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                refreshDisplay();
//            }
//        });
//    }

    /**
     * If the GUI isn't shown, this method will display things. If the GUI <i>is 
     * shown</i>, bring it to the front.
     * 
     * <p>This method publishes {@link Event#SHOWN}.
     */
    public void showManager() {
        if (!isVisible()) {
            setVisible(true);
        } else {
            toFront();
        }
        staticTabbedManager = this;
        EventBus.publish(Event.SHOWN);
    }

    /**
     * Closes and disposes (if needed) the GUI.
     */
    public void closeManager() {
        //noinspection AssignmentToNull
        staticTabbedManager = null;
        EventBus.publish(Event.CLOSED);
        if (isDisplayable()) {
            dispose();
        }
    }

    /**
     * Attempts to refresh the contents of both the local and remote dataset
     * tables. 
     */
    public void refreshDisplay() {
        if (guiInitialized) {
            ((RemoteAddeTableModel)remoteTable.getModel()).refreshEntries();
            ((LocalAddeTableModel)localTable.getModel()).refreshEntries();
        }
    }

    /**
     * Create and show the GUI the remote ADDE dataset GUI. Since no 
     * {@link RemoteAddeEntry RemoteAddeEntries} have been provided, none of 
     * the fields will be prefilled (user is creating a new dataset). 
     */
    // TODO(jon): differentiate between showRemoteEditor() and showRemoteEditor(entries)
    public void showRemoteEditor() {
        if (tabbedPane.getSelectedIndex() != 0) {
            tabbedPane.setSelectedIndex(0);
        }
        RemoteEntryEditor editor = new RemoteEntryEditor(this, true, this, serverManager);
        editor.setVisible(true);
    }

    /**
     * Create and show the GUI the remote ADDE dataset GUI. Since some 
     * {@link RemoteAddeEntry RemoteAddeEntries} have been provided, all of the
     * applicable fields will be filled (user is editing an existing dataset).
     * 
     * @param entries Selection to edit. Should not be {@code null}.
     */
    // TODO(jon): differentiate between showRemoteEditor() and showRemoteEditor(entries)
    public void showRemoteEditor(final List<RemoteAddeEntry> entries) {
        if (tabbedPane.getSelectedIndex() != 0) {
            tabbedPane.setSelectedIndex(0);
        }
        RemoteEntryEditor editor = new RemoteEntryEditor(this, true, this, serverManager, entries);
        editor.setVisible(true);
    }

    /**
     * Removes the given remote ADDE entries from the server manager GUI.
     * 
     * @param entries Entries to remove. {@code null} is permissible, but is a {@literal "no-op"}.
     */
    public void removeRemoteEntries(final Collection<RemoteAddeEntry> entries) {
        if (entries == null) {
            return;
        }
        List<RemoteAddeEntry> removable = arrList(entries.size());
        for (RemoteAddeEntry entry : entries) {
            if (entry.getEntrySource() != EntrySource.SYSTEM) {
                removable.add(entry);
            }
        }
//        if (entrySet.removeAll(removable)) {
        if (serverManager.removeEntries(removable)) {
            RemoteAddeTableModel tableModel = (RemoteAddeTableModel)remoteTable.getModel();
            int first = Integer.MAX_VALUE;
            int last = Integer.MIN_VALUE;
            for (RemoteAddeEntry entry : removable) {
                int index = tableModel.getRowForEntry(entry);
                if (index >= 0) {
                    if (index < first) {
                        first = index;
                    }
                    if (index > last) {
                        last = index;
                    }
                }
            }
            tableModel.fireTableDataChanged();
            refreshDisplay();
            remoteTable.revalidate();
            if (first < remoteTable.getRowCount()) {
                remoteTable.setRowSelectionInterval(first, first);
            }
        } else {
            logger.debug("could not remove entries={}", removable);
        }
    }

    /**
     * Shows a local ADDE entry editor <b>without</b> anything pre-populated 
     * (creating a new local ADDE dataset).
     */
    public void showLocalEditor() {
        // TODO(jon): differentiate between showLocalEditor() and showLocalEditor(entry)
        if (tabbedPane.getSelectedIndex() != 1) {
            tabbedPane.setSelectedIndex(1);
        }
        LocalEntryEditor editor = new LocalEntryEditor(this, true, this, serverManager);
        editor.setVisible(true);
    }

    /**
     * Shows a local ADDE entry editor <b>with</b> the appropriate fields
     * pre-populated, using the values from {@code entry}. This is intended to 
     * handle {@literal "editing"} a local ADDE dataset.
     * 
     * @param entry Entry to edit; should not be {@code null}.
     */
    public void showLocalEditor(final LocalAddeEntry entry) {
        // TODO(jon): differentiate between showLocalEditor() and showLocalEditor(entry)
        if (tabbedPane.getSelectedIndex() != 1) {
            tabbedPane.setSelectedIndex(1);
        }
        LocalEntryEditor editor = new LocalEntryEditor(this, true, this, serverManager, entry);
        editor.setVisible(true);
    }

    /**
     * Removes the given local ADDE entries from the server manager GUI.
     * 
     * @param entries Entries to remove. {@code null} is permissible, but is a {@literal "no-op"}.
     */
    public void removeLocalEntries(final Collection<LocalAddeEntry> entries) {
        if (entries == null) {
            return;
        }
//        if (entrySet.removeAll(entries)) {
        if (serverManager.removeEntries(entries)) {
            logger.trace("successful removal of entries={}",entries);
            LocalAddeTableModel tableModel = (LocalAddeTableModel)localTable.getModel();
            int first = Integer.MAX_VALUE;
            int last = Integer.MIN_VALUE;
            for (LocalAddeEntry entry : entries) {
                int index = tableModel.getRowForEntry(entry);
                if (index >= 0) {
                    if (index < first) {
                        first = index;
                    }
                    if (index > last) {
                        last = index;
                    }
                }
            }
            tableModel.fireTableDataChanged();
            refreshDisplay();
            localTable.revalidate();
            if (first < localTable.getRowCount()) {
                localTable.setRowSelectionInterval(first, first);
            }
        } else {
            logger.debug("could not remove entries={}", entries);
        }
    }

    /**
     * Extracts datasets from a given MCTABLE.TXT and adds them to the server
     * manager.
     * 
     * @param path Path to the MCTABLE.TXT. Cannot be {@code null}.
     * @param username ADDE username to use for verifying extracted datasets. Cannot be {@code null}.
     * @param project ADDE project number to use for verifying extracted datasets. Cannot be {@code null}.
     */
    public void importMctable(final String path, final String username, final String project) {
        logger.trace("extracting path={} username={}, project={}", new Object[] { path, username, project });
        final Set<RemoteAddeEntry> imported = EntryTransforms.extractMctableEntries(path, username, project);
        logger.trace("extracted entries={}", imported);
        if (imported.equals(Collections.emptySet())) {
            LogUtil.userErrorMessage("Selection does not appear to a valid MCTABLE.TXT file:\n"+path);
        } else {
            logger.trace("adding extracted entries...");
            // verify entries first!
            serverManager.addEntries(imported);
            refreshDisplay();
            repaint();
            Runnable r = new Runnable() {
                public void run() {
                    checkDatasets(imported);
                }
            };
            Thread t = new Thread(r);
            t.start();
        }
    }

    /**
     * Attempts to start the local servers. 
     * 
     * @see EntryStore#startLocalServer()
     */
    public void startLocalServers() {
        logger.trace("starting local servers...?");
        serverManager.startLocalServer();
    }

    /**
     * Attempts to stop the local servers.
     * 
     * @see EntryStore#stopLocalServer()
     */
    public void stopLocalServers() {
        logger.trace("stopping local servers...?");
        serverManager.stopLocalServer();
    }

    /**
     * Responds to local server events and attempts to update the GUI status
     * message.
     * 
     * @param event Local server event. Should not be {@code null}.
     */
    @EventSubscriber(eventClass=AddeThread.McservEvent.class)
    public void mcservUpdated(final AddeThread.McservEvent event) {
        logger.trace("eventbus evt={}", event.toString());
        final String msg;
        switch (event) {
            case ACTIVE: case DIED: case STOPPED:
                msg = event.getMessage();
                break;
            case STARTED:
//                msg = "Local servers are listening on port "+EntryStore.getLocalPort();
                msg = String.format(event.getMessage(),EntryStore.getLocalPort());
                break;
            default:
                msg = "Unknown local servers status: "+event.toString();
                break;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (statusLabel != null) {
                    statusLabel.setText(msg);
                }
            }
        });
    }

    /**
     * Builds the server manager GUI.
     */
    @SuppressWarnings({"unchecked", "FeatureEnvy", "MagicNumber"})
    public void initComponents() {
        Dimension frameSize = new Dimension(730, 460);
        ucar.unidata.ui.Help.setTopDir(HELP_TOP_DIR);
        system = icon("/edu/wisc/ssec/mcidasv/resources/icons/servermanager/padlock_closed.png");
        mctable = icon("/edu/wisc/ssec/mcidasv/resources/icons/servermanager/bug.png");
        user = icon("/edu/wisc/ssec/mcidasv/resources/icons/servermanager/hand_pro.png");
        invalid = icon("/edu/wisc/ssec/mcidasv/resources/icons/servermanager/emotion_sad.png");
        unverified = icon("/edu/wisc/ssec/mcidasv/resources/icons/servermanager/eye_inv.png");
        setTitle("ADDE Data Manager");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(frameSize);
        setMinimumSize(frameSize);
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem remoteNewMenuItem = new JMenuItem("New Remote Dataset");
        remoteNewMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                showRemoteEditor();
            }
        });
        fileMenu.add(remoteNewMenuItem);

        JMenuItem localNewMenuItem = new JMenuItem("New Local Dataset");
        localNewMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                showLocalEditor();
            }
        });
        fileMenu.add(localNewMenuItem);

        fileMenu.add(new JSeparator());

        JMenuItem importMctableMenuItem = new JMenuItem("Import MCTABLE...");
        importMctableMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                importButtonActionPerformed(e);
            }
        });
        fileMenu.add(importMctableMenuItem);

        JMenuItem importUrlMenuItem = new JMenuItem("Import from URL...");
        final TabbedAddeManager myRef = this;
        importUrlMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                   public void run() { 
                       try {
                           ImportUrl dialog = new ImportUrl(serverManager, myRef);
                           dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                           dialog.setVisible(true);
                       } catch (Exception e) {
                           e.printStackTrace();
                       }
                   }
                });
            }
        });
        fileMenu.add(importUrlMenuItem);

//        JMenuItem exportMenuItem = new JMenuItem("Export...");
//        exportMenuItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                logger.trace("exporting datasets...");
//            }
//        });
//        fileMenu.add(exportMenuItem);
//
        fileMenu.add(new JSeparator());

        JMenuItem closeMenuItem = new JMenuItem("Close");
        closeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logger.debug("evt={}", evt.toString());
                closeManager();
            }
        });
        fileMenu.add(closeMenuItem);

        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);

        editMenuItem = new JMenuItem("Edit Entry...");
        editMenuItem.setEnabled(false);
        editMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (tabbedPane.getSelectedIndex() == 0) {
                    showRemoteEditor(getSelectedRemoteEntries());
                } else {
                    showLocalEditor(getSingleLocalSelection());
                }
            }
        });
        editMenu.add(editMenuItem);

        removeMenuItem = new JMenuItem("Remove Selection");
        removeMenuItem.setEnabled(false);
        removeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (tabbedPane.getSelectedIndex() == 0) {
                    removeRemoteEntries(getSelectedRemoteEntries());
                } else {
                    removeLocalEntries(getSelectedLocalEntries());
                }
            }
        });
        editMenu.add(removeMenuItem);

        JMenu localServersMenu = new JMenu("Local Servers");
        menuBar.add(localServersMenu);

        JMenuItem startLocalMenuItem = new JMenuItem("Start Local Servers");
        startLocalMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startLocalServers();
            }
        });
        localServersMenu.add(startLocalMenuItem);

        JMenuItem stopLocalMenuItem = new JMenuItem("Stop Local Servers");
        stopLocalMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopLocalServers();
            }
        });
        localServersMenu.add(stopLocalMenuItem);

        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        JMenuItem remoteHelpMenuItem = new JMenuItem("Show Remote Data Help");
        remoteHelpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(REMOTE_HELP_TARGET);
            }
        });
        helpMenu.add(remoteHelpMenuItem);

        JMenuItem localHelpMenuItem = new JMenuItem("Show Local Data Help");
        localHelpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(LOCAL_HELP_TARGET);
            }
        });
        helpMenu.add(localHelpMenuItem);

        contentPane = new JPanel();
        contentPane.setBorder(null);
        setContentPane(contentPane);
        contentPane.setLayout(new MigLayout("", "[grow]", "[grow][grow][grow]"));

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                handleTabStateChanged(event);
            }
        });
        contentPane.add(tabbedPane, "cell 0 0 1 3,grow");

        JPanel remoteTab = new JPanel();
        remoteTab.setBorder(new EmptyBorder(0, 4, 4, 4));
        tabbedPane.addTab("Remote Data", null, remoteTab, null);
        remoteTab.setLayout(new BoxLayout(remoteTab, BoxLayout.Y_AXIS));

        remoteTable = new BetterJTable();
        JScrollPane remoteScroller = BetterJTable.createStripedJScrollPane(remoteTable);

        remoteTable.setModel(new RemoteAddeTableModel(serverManager));
        remoteTable.setColumnSelectionAllowed(false);
        remoteTable.setRowSelectionAllowed(true);
        remoteTable.getTableHeader().setReorderingAllowed(false);
        remoteTable.setFont(UIManager.getFont("Table.font").deriveFont(11.0f));
        remoteTable.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        remoteTable.setDefaultRenderer(String.class, new TextRenderer());
        remoteTable.getColumnModel().getColumn(0).setPreferredWidth(10);
        remoteTable.getColumnModel().getColumn(1).setPreferredWidth(10);
        remoteTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        remoteTable.getColumnModel().getColumn(4).setPreferredWidth(50);
        remoteTable.getColumnModel().getColumn(0).setCellRenderer(new EntryValidityRenderer());
        remoteTable.getColumnModel().getColumn(1).setCellRenderer(new EntrySourceRenderer());
        remoteTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                remoteSelectionModelChanged(e);
            }
        });
        remoteTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(final java.awt.event.MouseEvent e) {
                if ((e.getClickCount() == 2) && (hasSingleRemoteSelection())) {
                    showRemoteEditor(getSelectedRemoteEntries());
                }
            }
        });
        remoteScroller.setViewportView(remoteTable);
        remoteTab.add(remoteScroller);

        JPanel remoteActionPanel = new JPanel();
        remoteTab.add(remoteActionPanel);
        remoteActionPanel.setLayout(new BoxLayout(remoteActionPanel, BoxLayout.X_AXIS));

        newRemoteButton = new JButton("Add New Dataset");
        newRemoteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.trace("new remote dataset");
                showRemoteEditor();
            }
        });
        newRemoteButton.setToolTipText("Create a new remote ADDE dataset.");
        remoteActionPanel.add(newRemoteButton);

        editRemoteButton = new JButton("Edit Dataset");
        editRemoteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.trace("edit remote dataset");
                showRemoteEditor(getSelectedRemoteEntries());
            }
        });
        editRemoteButton.setToolTipText("Edit an existing remote ADDE dataset.");
        remoteActionPanel.add(editRemoteButton);

        removeRemoteButton = new JButton("Remove Selection");
        removeRemoteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.trace("remove remote dataset");
                removeRemoteEntries(getSelectedRemoteEntries());
            }
        });
        removeRemoteButton.setToolTipText("Remove the selected remote ADDE datasets.");
        remoteActionPanel.add(removeRemoteButton);

        importRemoteButton = new JButton("Import MCTABLE...");
        importRemoteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.trace("import from mctable...");
                importButtonActionPerformed(e);
            }
        });
        remoteActionPanel.add(importRemoteButton);

        JPanel localTab = new JPanel();
        localTab.setBorder(new EmptyBorder(0, 4, 4, 4));
        tabbedPane.addTab("Local Data", null, localTab, null);
        localTab.setLayout(new BoxLayout(localTab, BoxLayout.Y_AXIS));

        localTable = new BetterJTable();
        JScrollPane localScroller = BetterJTable.createStripedJScrollPane(localTable);
        localTable.setModel(new LocalAddeTableModel(serverManager));
        localTable.setColumnSelectionAllowed(false);
        localTable.setRowSelectionAllowed(true);
        localTable.getTableHeader().setReorderingAllowed(false);
        localTable.setFont(UIManager.getFont("Table.font").deriveFont(11.0f));
        localTable.setDefaultRenderer(String.class, new TextRenderer());
        localTable.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        localTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                localSelectionModelChanged(e);
            }
        });
        localTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(final java.awt.event.MouseEvent e) {
                if ((e.getClickCount() == 2) && (hasSingleLocalSelection())) {
                    showLocalEditor(getSingleLocalSelection());
                }
            }
        });
        localScroller.setViewportView(localTable);
        localTab.add(localScroller);

        JPanel localActionPanel = new JPanel();
        localTab.add(localActionPanel);
        localActionPanel.setLayout(new BoxLayout(localActionPanel, BoxLayout.X_AXIS));

        newLocalButton = new JButton("Add New Dataset");
        newLocalButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.trace("new local dataset");
                showLocalEditor();
            }
        });
        newLocalButton.setToolTipText("Create a new local ADDE dataset.");
        localActionPanel.add(newLocalButton);

        editLocalButton = new JButton("Edit Dataset");
        editLocalButton.setEnabled(false);
        editLocalButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.trace("edit local dataset");
                showLocalEditor(getSingleLocalSelection());
            }
        });
        editLocalButton.setToolTipText("Edit an existing local ADDE dataset.");
        localActionPanel.add(editLocalButton);

        removeLocalButton = new JButton("Remove Selection");
        removeLocalButton.setEnabled(false);
        removeLocalButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.trace("remove local dataset");
                removeLocalEntries(getSelectedLocalEntries());
            }
        });
        removeLocalButton.setToolTipText("Remove the selected local ADDE datasets.");
        localActionPanel.add(removeLocalButton);

        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(new EmptyBorder(0, 6, 0, 6));
        contentPane.add(statusPanel, "cell 0 3,grow");
        statusPanel.setLayout(new BorderLayout(0, 0));

        Box statusMessageBox = Box.createHorizontalBox();
        statusPanel.add(statusMessageBox, BorderLayout.WEST);

        String statusMessage = McservEvent.STOPPED.getMessage();
        if (serverManager.checkLocalServer()) {
            statusMessage = McservEvent.ACTIVE.getMessage();
        }
        statusLabel = new JLabel(statusMessage);
        statusMessageBox.add(statusLabel);
        statusLabel.setEnabled(false);

        Box frameControlBox = Box.createHorizontalBox();
        statusPanel.add(frameControlBox, BorderLayout.EAST);

//        cancelButton = new JButton("Cancel");
//        cancelButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                handleCancellingChanges();
//            }
//        });
//        frameControlBox.add(cancelButton);

//        applyButton = new JButton("Apply");
//        applyButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                logger.trace("apply");
//                handleSavingChanges();
//            }
//        });
//        frameControlBox.add(applyButton);

        okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//                handleSavingChanges();
                closeManager();
            }
        });
        frameControlBox.add(okButton);
        tabbedPane.setSelectedIndex(getLastTab());
        guiInitialized = true;
    }

//    /**
//     * Determines whether or not the user has changed anything (where {@literal "changed"} means added, modified, or removed entries).
//     * 
//     * @return {@code true} if the user has changed any entries; {@code false} otherwise.
//     */
//    public boolean hasUserChanges() {
//        return !entrySet.equals(serverManager.getEntrySet());
//    }

//    /**
//     * Respond to the user clicking the {@literal "cancel"} button.
//     */
//    public void handleCancellingChanges() {
//        logger.trace("cancel changes. anything to do={}", hasUserChanges());
//        closeManager();
//    }
//
//    /**
//     * Respond to the user clicking the {@literal "save changes"} button.
//     */
//    public void handleSavingChanges() {
//        boolean userChanges = hasUserChanges();
//        logger.trace("save changes. anything to do={}", userChanges);
//        if (userChanges) {
//            serverManager.addEntries(entrySet);
//        }
//    }

    /**
     * Respond to changes in {@link #tabbedPane}; primarily switching tabs.
     * 
     * @param event Event being handled. Ignored for now.
     */
    private void handleTabStateChanged(final ChangeEvent event) {
        assert SwingUtilities.isEventDispatchThread();
        boolean hasSelection = false;
        int index = 0;
        if (guiInitialized) {
            index = tabbedPane.getSelectedIndex();
            if (index == 0) {
                hasSelection = hasRemoteSelection();
                editRemoteButton.setEnabled(hasSelection);
                removeRemoteButton.setEnabled(hasSelection);
            } else {
                hasSelection = hasLocalSelection();
                editLocalButton.setEnabled(hasSelection);
                removeLocalButton.setEnabled(hasSelection);
            }
            editMenuItem.setEnabled(hasSelection);
            removeMenuItem.setEnabled(hasSelection);
            setLastTab(index);
        }
        logger.trace("index={} hasRemote={} hasLocal={} guiInit={}", new Object[] {index, hasRemoteSelection(), hasLocalSelection(), guiInitialized});
    }

    /**
     * Respond to events.
     * 
     * @param e {@link ListSelectionEvent} that necessitated this call.
     */
    private void remoteSelectionModelChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        int selectedRowCount = 0;
        ListSelectionModel selModel = (ListSelectionModel)e.getSource();
        Set<RemoteAddeEntry> selectedEntries;
        if (selModel.isSelectionEmpty()) {
            selectedEntries = Collections.emptySet();
        } else {
            int min = selModel.getMinSelectionIndex();
            int max = selModel.getMaxSelectionIndex();
            RemoteAddeTableModel tableModel = (RemoteAddeTableModel)remoteTable.getModel();
            selectedEntries = newLinkedHashSet((max - min) * AddeEntry.EntryType.values().length);
            for (int i = min; i <= max; i++) {
                if (selModel.isSelectedIndex(i)) {
                    List<RemoteAddeEntry> entries = tableModel.getEntriesAtRow(i);
                    selectedEntries.addAll(entries);
                    selectedRowCount++;
                }
            }
        }

        boolean onlyDefaultEntries = true;
        for (RemoteAddeEntry entry : selectedEntries) {
            if (entry.getEntrySource() != EntrySource.SYSTEM) {
                onlyDefaultEntries = false;
                break;
            }
        }
        setSelectedRemoteEntries(selectedEntries);

        // the current "edit" dialog doesn't work so well with multiple 
        // servers/datasets, so only allow the user to edit entries one at a time.
        boolean singleSelection = selectedRowCount == 1;
        editRemoteButton.setEnabled(singleSelection);
        editMenuItem.setEnabled(singleSelection);

        boolean hasSelection = (selectedRowCount >= 1) && !onlyDefaultEntries;
        removeRemoteButton.setEnabled(hasSelection);
        removeMenuItem.setEnabled(hasSelection);
    }

    /**
     * Respond to events from the local dataset table.
     * 
     * @param e {@link ListSelectionEvent} that necessitated this call.
     */
    private void localSelectionModelChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        ListSelectionModel selModel = (ListSelectionModel)e.getSource();
        Set<LocalAddeEntry> selectedEntries;
        if (selModel.isSelectionEmpty()) {
            selectedEntries = Collections.emptySet();
        } else {
            int min = selModel.getMinSelectionIndex();
            int max = selModel.getMaxSelectionIndex();
            LocalAddeTableModel tableModel = (LocalAddeTableModel)localTable.getModel();
            selectedEntries = newLinkedHashSet(max - min);
            for (int i = min; i <= max; i++) {
                if (selModel.isSelectedIndex(i)) {
                    selectedEntries.add(tableModel.getEntryAtRow(i));
                }
            }
        }

        setSelectedLocalEntries(selectedEntries);

        // the current "edit" dialog doesn't work so well with multiple 
        // servers/datasets, so only allow the user to edit entries one at a time.
        boolean singleSelection = selectedEntries.size() == 1;
        this.editRemoteButton.setEnabled(singleSelection);
        this.editMenuItem.setEnabled(singleSelection);

        boolean hasSelection = !selectedEntries.isEmpty();
        removeRemoteButton.setEnabled(hasSelection);
        removeMenuItem.setEnabled(hasSelection);
    }

    /**
     * Checks to see if {@link #selectedRemoteEntries} contains any 
     * {@link RemoteAddeEntry}s.
     *
     * @return Whether or not any {@code RemoteAddeEntry} values are selected.
     */
    private boolean hasRemoteSelection() {
        return !selectedRemoteEntries.isEmpty();
    }

    /**
     * Checks to see if {@link #selectedLocalEntries} contains any
     * {@link LocalAddeEntry}s.
     *
     * @return Whether or not any {@code LocalAddeEntry} values are selected.
     */
    private boolean hasLocalSelection() {
        return !selectedLocalEntries.isEmpty();
    }

    /**
     * Checks to see if the user has select a <b>single</b> remote dataset.
     * 
     * @return {@code true} if there is a single remote dataset selected. {@code false} otherwise.
     */
    private boolean hasSingleRemoteSelection() {
        String entryText = null;
        for (RemoteAddeEntry entry : selectedRemoteEntries) {
            if (entryText == null) {
                entryText = entry.getEntryText();
            }
            if (!entry.getEntryText().equals(entryText)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks to see if the user has select a <b>single</b> local dataset.
     * 
     * @return {@code true} if there is a single local dataset selected. {@code false} otherwise.
     */
    private boolean hasSingleLocalSelection() {
        return selectedLocalEntries.size() == 1;
    }

    /**
     * If there is a single local dataset selected, this method will return that
     * dataset.
     * 
     * @return Either the single selected local dataset, or {@link LocalAddeEntry#INVALID_ENTRY}.
     */
    private LocalAddeEntry getSingleLocalSelection() {
        LocalAddeEntry entry = LocalAddeEntry.INVALID_ENTRY;
        if (selectedLocalEntries.size() == 1) {
            entry = selectedLocalEntries.get(0);
        }
        return entry;
    }

    /**
     * Corresponds to the selected remote ADDE entries in the GUI.
     * 
     * @param entries Should not be {@code null}.
     */
    private void setSelectedRemoteEntries(final Collection<RemoteAddeEntry> entries) {
        selectedRemoteEntries.clear();
        selectedRemoteEntries.addAll(entries);
        this.editRemoteButton.setEnabled(entries.size() == 1);
        this.removeRemoteButton.setEnabled(!entries.isEmpty());
        logger.trace("remote entries={}", entries);
    }

    /**
     * Gets the selected remote ADDE entries.
     * 
     * @return Either an empty list or the remote entries selected in the GUI.
     */
    private List<RemoteAddeEntry> getSelectedRemoteEntries() {
        if (selectedRemoteEntries.isEmpty()) {
            return Collections.emptyList();
        } else {
            return arrList(selectedRemoteEntries);
        }
    }

    /**
     * Corresponds to the selected local ADDE entries in the GUI.
     * 
     * @param entries Should not be {@code null}.
     */
    private void setSelectedLocalEntries(final Collection<LocalAddeEntry> entries) {
        selectedLocalEntries.clear();
        selectedLocalEntries.addAll(entries);
        this.editLocalButton.setEnabled(entries.size() == 1);
        this.removeLocalButton.setEnabled(!entries.isEmpty());
        logger.trace("local entries={}", entries);
    }

    /**
     * Gets the selected local ADDE entries.
     * 
     * @return Either an empty list or the local entries selected in the GUI.
     */
    private List<LocalAddeEntry> getSelectedLocalEntries() {
        if (selectedLocalEntries.isEmpty()) {
            return Collections.emptyList();
        } else {
            return arrList(selectedLocalEntries);
        }
    }

    /**
     * Handles the user closing the server manager GUI.
     * 
     * @param evt Event that triggered this method call.
     * 
     * @see #closeManager()
     */
    private void formWindowClosed(java.awt.event.WindowEvent evt) {
        logger.debug("evt={}", evt.toString());
        closeManager();
    }

    @SuppressWarnings({"MagicNumber"})
    private JPanel makeFileChooserAccessory() {
        assert SwingUtilities.isEventDispatchThread();
        JPanel accessory = new JPanel();
        accessory.setLayout(new BoxLayout(accessory, BoxLayout.PAGE_AXIS));
        importAccountBox = new JCheckBox("Use ADDE Accounting?");
        importAccountBox.setSelected(false);
        importAccountBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boolean selected = importAccountBox.isSelected();
                importUser.setEnabled(selected);
                importProject.setEnabled(selected);
            }
        });
        String clientProp = "JComponent.sizeVariant";
        String propVal = "mini";

        importUser = new JTextField();
        importUser.putClientProperty(clientProp, propVal);
        Prompt userPrompt = new Prompt(importUser, "Username");
        userPrompt.putClientProperty(clientProp, propVal);
        importUser.setEnabled(importAccountBox.isSelected());

        importProject = new JTextField();
        Prompt projPrompt = new Prompt(importProject, "Project Number");
        projPrompt.putClientProperty(clientProp, propVal);
        importProject.putClientProperty(clientProp, propVal);
        importProject.setEnabled(importAccountBox.isSelected());

        GroupLayout layout = new GroupLayout(accessory);
        accessory.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(importAccountBox)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                    .addComponent(importProject, GroupLayout.Alignment.LEADING)
                    .addComponent(importUser, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(importAccountBox)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(importUser, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(importProject, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(55, (int)Short.MAX_VALUE))
        );
        return accessory;
    }

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {
        assert SwingUtilities.isEventDispatchThread();
        JFileChooser fc = new JFileChooser(getLastImportPath());
        fc.setAccessory(makeFileChooserAccessory());
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int ret = fc.showOpenDialog(TabbedAddeManager.this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            String path = f.getPath();

            boolean defaultUser = false;
            String forceUser = importUser.getText();
            if (forceUser.length() == 0) {
                forceUser = AddeEntry.DEFAULT_ACCOUNT.getUsername();
                defaultUser = true;
            }

            boolean defaultProj = false;
            String forceProj = importProject.getText();
            if (forceProj.length() == 0) {
                forceProj = AddeEntry.DEFAULT_ACCOUNT.getProject();
                defaultProj = true;
            }

            
            if ((importAccountBox.isSelected()) && (defaultUser || defaultProj)) {
                logger.warn("bad acct dialog: forceUser={} forceProj={}", forceUser, forceProj);
            } else {
                logger.warn("acct appears valid: forceUser={} forceProj={}", forceUser, forceProj);
                importMctable(path, forceUser, forceProj);
                // don't worry about file validity; i'll just assume the user clicked
                // on the wrong entry by accident.
                setLastImportPath(f.getParent());
            }
        }
    }

    /**
     * Returns the directory that contained the most recently imported MCTABLE.TXT.
     *
     * @return Either the path to the most recently imported MCTABLE.TXT file,
     * or an empty {@code String}.
     */
    private String getLastImportPath() {
        String lastPath = serverManager.getIdvStore().get(LAST_IMPORTED, "");
        logger.trace("last path='{}'", lastPath);
        return lastPath;
//        return serverManager.getIdvStore().get(LAST_IMPORTED, "");
    }

    /**
     * Saves the directory that contained the most recently imported MCTABLE.TXT.
     *
     * @param path Path to the most recently imported MCTABLE.TXT file.
     * {@code null} values are replaced with an empty {@code String}.
     */
    private void setLastImportPath(final String path) {
        String okayPath = (path == null) ? "" : path;
        logger.trace("saving path='{}'", okayPath);
        serverManager.getIdvStore().put(LAST_IMPORTED, okayPath);
    }

    /**
     * Returns the index of the user's last server manager tab.
     *
     * @return Index of the user's most recently viewed server manager tab, or {@code 0}.
     */
    private int getLastTab() {
        int index = serverManager.getIdvStore().get(LAST_TAB, 0);
        logger.trace("last tab={}", index);
        return index;
//        return serverManager.getIdvStore().get(LAST_TAB, 0);
    }

    /**
     * Saves the index of the last server manager tab the user was looking at.
     * 
     * @param index Index of the user's most recently viewed server manager tab.
     */
    private void setLastTab(final int index) {
        int okayIndex = ((index >= 0) && (index < 2)) ? index : 0;
        IdvObjectStore store = serverManager.getIdvStore();
        logger.trace("storing tab={}", okayIndex);
        store.put(LAST_TAB, okayIndex);
    }

    // stupid adde.ucar.edu entries never seem to time out! great! making the gui hang is just so awesome!
    @SuppressWarnings({"ObjectAllocationInLoop"})
    public Set<RemoteAddeEntry> checkDatasets(final Collection<RemoteAddeEntry> entries) {
        notNull(entries, "can't check a null collection of entries");
        if (entries.isEmpty()) {
            return Collections.emptySet();
        }

        Set<RemoteAddeEntry> valid = newLinkedHashSet();
        ExecutorService exec = Executors.newFixedThreadPool(POOL);
        CompletionService<List<RemoteAddeEntry>> ecs = new ExecutorCompletionService<List<RemoteAddeEntry>>(exec);
        final RemoteAddeTableModel tableModel = (RemoteAddeTableModel)remoteTable.getModel();

        // place entries
        for (RemoteAddeEntry entry : entries) {
            ecs.submit(new BetterCheckTask(entry));
            logger.trace("submitting entry={}", entry);
            final int row = tableModel.getRowForEntry(entry);
            runOnEDT(new Runnable() {
                public void run() {
                    tableModel.fireTableRowsUpdated(row, row);
                }
            });
        }

        // work through the entries
        try {
            for (int i = 0; i < entries.size(); i++) {
                final List<RemoteAddeEntry> checkedEntries = ecs.take().get();
                if (!checkedEntries.isEmpty()) {
                    final int row = tableModel.getRowForEntry(checkedEntries.get(0));
                    runOnEDT(new Runnable() {
                        public void run() {
                            List<RemoteAddeEntry> oldEntries = tableModel.getEntriesAtRow(row);
                            serverManager.replaceEntries(oldEntries, checkedEntries);
//                            entrySet.removeAll(oldEntries);
//                            entrySet.addAll(checkedEntries);
                            tableModel.fireTableRowsUpdated(row, row);
                        }
                    });
                }
                valid.addAll(checkedEntries);
            }
        } catch (InterruptedException e) {
            LogUtil.logException("Interrupted while validating entries", e);
        } catch (ExecutionException e) {
            LogUtil.logException("ADDE validation execution error", e);
        } finally {
            exec.shutdown();
        }
        return valid;
    }

    private static class BetterCheckTask implements Callable<List<RemoteAddeEntry>> {
        private final RemoteAddeEntry entry;
        public BetterCheckTask(final RemoteAddeEntry entry) {
            this.entry = entry;
            this.entry.setEntryValidity(EntryValidity.VALIDATING);
        }
        @SuppressWarnings({"FeatureEnvy"})
        public List<RemoteAddeEntry> call() {
            List<RemoteAddeEntry> valid = arrList();
            if (RemoteAddeEntry.checkHost(entry)) {
                for (RemoteAddeEntry tmp : EntryTransforms.createEntriesFrom(entry)) {
                    if (RemoteAddeEntry.checkEntry(false, tmp) == AddeStatus.OK) {
                        tmp.setEntryValidity(EntryValidity.VERIFIED);
                        valid.add(tmp);
                    }
                }
            }
            if (!valid.isEmpty()) {
                entry.setEntryValidity(EntryValidity.VERIFIED);
            } else {
                entry.setEntryValidity(EntryValidity.INVALID);
            }
            return valid;
        }
    }

    private class CheckEntryTask implements Callable<RemoteAddeEntry> {
        private final RemoteAddeEntry entry;
        public CheckEntryTask(final RemoteAddeEntry entry) {
            notNull(entry);
            this.entry = entry;
            this.entry.setEntryValidity(EntryValidity.VALIDATING);
        }
        @SuppressWarnings({"FeatureEnvy"})
        public RemoteAddeEntry call() {
            AddeStatus status = RemoteAddeEntry.checkEntry(entry);
            switch (status) {
                case OK: entry.setEntryValidity(EntryValidity.VERIFIED); break;
                default: entry.setEntryValidity(EntryValidity.INVALID); break;
            }
            return entry;
        }
    }

    private static class RemoteAddeTableModel extends AbstractTableModel {

        // TODO(jon): these constants can go once things calm down
        private static final int VALID = 0;
        private static final int SOURCE = 1;
        private static final int DATASET = 2;
        private static final int ACCT = 3;
        private static final int TYPES = 4;
        private static final Pattern ENTRY_ID_SPLITTER = Pattern.compile("!");

        /** Labels that appear as the column headers. */
        private final String[] columnNames = {
            "Valid", "Source", "Dataset", "Accounting", "Data Types"
        };

        private final List<String> servers;

        /** {@link EntryStore} used to query and apply changes. */
        private final EntryStore entryStore;

        /**
         * Builds an {@link javax.swing.table.AbstractTableModel} with some extensions that
         * facilitate working with {@link RemoteAddeEntry RemoteAddeEntrys}.
         * 
         * @param entryStore Server manager object.
         */
        public RemoteAddeTableModel(final EntryStore entryStore) {
            notNull(entryStore, "Cannot query a null EntryStore");
            this.entryStore = entryStore;
            this.servers = arrList(entryStore.getRemoteEntryTexts());
        }

        /**
         * Returns the {@link RemoteAddeEntry} at the given index.
         * 
         * @param row Index of the entry.
         * 
         * @return The {@code RemoteAddeEntry} at the index specified by {@code row}.
         */
        protected List<RemoteAddeEntry> getEntriesAtRow(final int row) {
            String server = servers.get(row).replace('/', '!');
            List<RemoteAddeEntry> matches = arrList();
            for (AddeEntry entry : entryStore.searchWithPrefix(server)) {
                if (entry instanceof RemoteAddeEntry) {
                    matches.add((RemoteAddeEntry)entry);
                }
            }
            return matches;
        }

        /**
         * Returns the index of the given {@code entry}.
         *
         * @param entry {@link RemoteAddeEntry} whose row is desired.
         *
         * @return Index of the desired {@code entry}, or {@code -1} if the
         * entry wasn't found.
         */
        protected int getRowForEntry(final RemoteAddeEntry entry) {
            return getRowForEntry(entry.getEntryText());
        }

        /**
         * Returns the index of the given entry text within the table.
         *
         * @param entryText String representation of the desired entry.
         *
         * @return Index of the desired entry, or {@code -1} if the entry was
         * not found.
         *
         * @see edu.wisc.ssec.mcidasv.servermanager.AddeEntry#getEntryText()
         */
        protected int getRowForEntry(final String entryText) {
            return servers.indexOf(entryText);
        }

        /**
         * Clears and re-adds all {@link RemoteAddeEntry}s within {@link #entryStore}.
         */
        public void refreshEntries() {
            servers.clear();
            servers.addAll(entryStore.getRemoteEntryTexts());
            this.fireTableDataChanged();
        }

        /**
         * Returns the length of {@link #columnNames}.
         * 
         * @return The number of columns.
         */
        @Override public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Returns the number of entries being managed.
         */
        @Override public int getRowCount() {
            return servers.size();
        }

        /**
         * Finds the value at the given coordinates.
         * 
         * @param row Table row.
         * @param column Table column.
         * 
         * @return Value stored at the given {@code row} and {@code column}
         * coordinates
         * 
         * @throws IndexOutOfBoundsException if {@code row} or {@code column}
         * refer to an invalid table cell.
         */
        @Override public Object getValueAt(int row, int column) {
            String serverText = servers.get(row);
            String prefix = serverText.replace('/', '!');
            switch (column) {
                case VALID: return formattedValidity(prefix, entryStore);
                case SOURCE: return formattedSource(prefix, entryStore);
                case DATASET: return serverText;
                case ACCT: return formattedAccounting(prefix, entryStore);
                case TYPES: return formattedTypes(prefix, entryStore);
                default: throw new IndexOutOfBoundsException();
            }
        }

        private static String formattedSource(final String serv, final EntryStore manager) {
            List<AddeEntry> matches = manager.searchWithPrefix(serv);
            EntrySource source = EntrySource.INVALID;
            if (!matches.isEmpty()) {
                for (AddeEntry entry : matches) {
                    if (entry.getEntrySource() == EntrySource.USER) {
                        return EntrySource.USER.toString();
                    }
                }
              source = matches.get(0).getEntrySource();
            }
            return source.toString();
        }

        private static String formattedValidity(final String serv, final EntryStore manager) {
            List<AddeEntry> matches = manager.searchWithPrefix(serv);
            EntryValidity validity = EntryValidity.INVALID;
            if (!matches.isEmpty()) {
                validity = matches.get(0).getEntryValidity();
            }
            return validity.toString();
        }

        private static String formattedAccounting(final String serv, final EntryStore manager) {
            List<AddeEntry> matches = manager.searchWithPrefix(serv);
            AddeAccount acct = AddeEntry.DEFAULT_ACCOUNT;
            if (!matches.isEmpty()) {
                acct = matches.get(0).getAccount();
            }
            if (AddeEntry.DEFAULT_ACCOUNT.equals(acct)) {
                return "public dataset";
            }
            return acct.friendlyString();
        }

        private static boolean hasType(final String serv, final EntryStore manager, final EntryType type) {
            String[] chunks = ENTRY_ID_SPLITTER.split(serv);
            Set<EntryType> types = Collections.emptySet();
            if (chunks.length == 2) {
                types = manager.getTypes(chunks[0], chunks[1]);
            }
            return types.contains(type);
        }

        private static String formattedTypes(final String serv, final EntryStore manager) {
            String[] chunks = ENTRY_ID_SPLITTER.split(serv);
            Set<EntryType> types = Collections.emptySet();
            if (chunks.length == 2) {
                types = manager.getTypes(chunks[0], chunks[1]);
            }

            @SuppressWarnings({"MagicNumber"})
            StringBuilder sb = new StringBuilder(30);
            for (EntryType type : EnumSet.of(EntryType.IMAGE, EntryType.GRID, EntryType.NAV, EntryType.POINT, EntryType.RADAR, EntryType.TEXT)) {
                if (types.contains(type)) {
                    sb.append(type.toString()).append(' ');
                }
            }
            return sb.toString().toLowerCase();
        }

        /**
         * Returns the column name associated with {@code column}.
         * 
         * @return One of {@link #columnNames}.
         */
        @Override public String getColumnName(final int column) {
            return columnNames[column];
        }

        @Override public Class<?> getColumnClass(final int column) {
            return String.class;
        }

        @Override public boolean isCellEditable(final int row, final int column) {
            return false;
        }
    }

    private static class LocalAddeTableModel extends AbstractTableModel {

        /** Labels that appear as the column headers. */
        private final String[] columnNames = {
            "Dataset (e.g. MYDATA)", "Image Type (e.g. JAN 07 GOES)", "Format", "Directory"
        };

        /** Entries that currently populate the server manager. */
        private final List<LocalAddeEntry> entries;

        /** {@link EntryStore} used to query and apply changes. */
        private final EntryStore entryStore;

        public LocalAddeTableModel(final EntryStore entryStore) {
            notNull(entryStore, "Cannot query a null EntryStore");
            this.entryStore = entryStore;
            this.entries = arrList(entryStore.getLocalEntries());
        }

        /**
         * Returns the {@link LocalAddeEntry} at the given index.
         * 
         * @param row Index of the entry.
         * 
         * @return The {@code LocalAddeEntry} at the index specified by {@code row}.
         */
        protected LocalAddeEntry getEntryAtRow(final int row) {
            return entries.get(row);
        }

        protected int getRowForEntry(final LocalAddeEntry entry) {
            return entries.indexOf(entry);
        }

        protected List<LocalAddeEntry> getSelectedEntries(final int[] rows) {
            List<LocalAddeEntry> selected = arrList(rows.length);
            int rowCount = entries.size();
            for (int i = 0; i < rows.length; i++) {
                int tmpIdx = rows[i];
                if ((tmpIdx >= 0) && (tmpIdx < rowCount)) {
                    selected.add(entries.get(tmpIdx));
                } else {
                    throw new IndexOutOfBoundsException();
                }
            }
            return selected;
        }

        public void refreshEntries() {
            entries.clear();
            entries.addAll(entryStore.getLocalEntries());
            this.fireTableDataChanged();
        }

        /**
         * Returns the length of {@link #columnNames}.
         * 
         * @return The number of columns.
         */
        @Override public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Returns the number of entries being managed.
         */
        @Override public int getRowCount() {
            return entries.size();
        }

        /**
         * Finds the value at the given coordinates.
         * 
         * @param row Table row.
         * @param column Table column.
         * 
         * @return Value stored at the given {@code row} and {@code column}
         * coordinates
         *
         * @throws IndexOutOfBoundsException if {@code row} or {@code column}
         * refer to an invalid table cell.
         */
        @Override public Object getValueAt(int row, int column) {
            LocalAddeEntry entry = entries.get(row);
            if (entry == null) {
                throw new IndexOutOfBoundsException(); // still questionable...
            }

            switch (column) {
                case 0: return entry.getGroup();
                case 1: return entry.getName();
                case 2: return entry.getFormat();
                case 3: return entry.getMask();
                default: throw new IndexOutOfBoundsException();
            }
        }

        /**
         * Returns the column name associated with {@code column}.
         * 
         * @return One of {@link #columnNames}.
         */
        @Override public String getColumnName(final int column) {
            return columnNames[column];
        }
    }

    // i need the following icons:
    // something to convey entry validity: invalid, verified, unverified
    // a "system" entry icon (thinking of something with prominent "V")
    // a "mctable" entry icon (similar to above, but with a prominent "X")
    // a "user" entry icon (no idea yet!)
    public class EntrySourceRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            EntrySource source = EntrySource.valueOf((String)value);
            EntrySourceRenderer renderer = (EntrySourceRenderer)comp;
            Icon icon = null;
            String tooltip = null;
            switch (source) {
                case SYSTEM:
                    icon = system;
                    tooltip = "Default dataset and cannot be removed, only disabled.";
                    break;
                case MCTABLE:
                    icon = mctable;
                    tooltip = "Dataset imported from a MCTABLE.TXT.";
                    break;
                case USER:
                    icon = user;
                    tooltip = "Dataset created or altered by you!";
                    break;
            }
            renderer.setIcon(icon);
            renderer.setToolTipText(tooltip);
            renderer.setText(null);
            return comp;
        }
    }

    public class EntryValidityRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            EntryValidity validity = EntryValidity.valueOf((String)value);
            EntryValidityRenderer renderer = (EntryValidityRenderer)comp;
            Icon icon = null;
            String msg = null;
            String tooltip = null;
            switch (validity) {
                case INVALID:
                    icon = invalid;
                    tooltip = "Dataset verification failed.";
                    break;
                case VERIFIED:
                    break;
                case UNVERIFIED:
                    icon = unverified;
                    tooltip = "Dataset has not been verified.";
                    break;
                case VALIDATING:
                    msg = "Checking...";
                    break;
            }
            renderer.setIcon(icon);
            renderer.setToolTipText(tooltip);
            renderer.setText(msg);
            return comp;
        }
    }

    public static class TextRenderer extends DefaultTableCellRenderer {

        /** */
        private Font bold;

        /** */
        private Font boldItalic;

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Font currentFont = comp.getFont();
            if (bold == null) {
                bold = currentFont.deriveFont(Font.BOLD); 
            }
            if (boldItalic == null) {
                boldItalic = currentFont.deriveFont(Font.BOLD | Font.ITALIC);
            }
            if (column == 2) {
                comp.setFont(bold);
            } else if (column == 3) {
                // why can't i set the color for just a single column!?
            } else if (column == 4) {
                comp.setFont(boldItalic);
            }
            return comp;
        }
    }

    /**
     * Construct an {@link Icon} object using the image at the specified
     * {@code path}.
     * 
     * @param path Path to image to use as an icon. Should not be {@code null}.
     * 
     * @return Icon object with the desired image.
     */
    private static Icon icon(final String path) {
        return GuiUtils.getImageIcon(path, TabbedAddeManager.class, true);
    }

    /**
     * Launch the application. Makes for a simplistic test.
     * 
     * @param args Command line arguments. These are currently ignored.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    TabbedAddeManager frame = new TabbedAddeManager();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private JPanel contentPane;
    private JTable remoteTable;
    private JTable localTable;
    private JTabbedPane tabbedPane;
    private JLabel statusLabel;
    private JButton newRemoteButton;
    private JButton editRemoteButton;
    private JButton removeRemoteButton;
    private JButton importRemoteButton;
    private JButton newLocalButton;
    private JButton editLocalButton;
    private JButton removeLocalButton;
//    private JButton applyButton;
    private JButton okButton;
//    private JButton cancelButton;
    private JMenuItem editMenuItem;
    private JMenuItem removeMenuItem;
    private JCheckBox importAccountBox;

    /** Icon for datasets that are part of a default McIDAS-V install. */
    private Icon system;

    /** Icon for datasets that originate from a MCTABLE.TXT. */
    private Icon mctable;

    /** Icon for datasets that the user has provided. */
    private Icon user;

    /** Icon for invalid datasets. */
    private Icon invalid;

    /** Icon for datasets that have not been verified. */
    private Icon unverified;
}
