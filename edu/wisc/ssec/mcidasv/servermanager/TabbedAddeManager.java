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

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.Contract.notNull;
import static edu.wisc.ssec.mcidasv.util.McVGuiUtils.runOnEDT;

import java.awt.Component;
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

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.bushe.swing.event.EventBus;
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
import edu.wisc.ssec.mcidasv.servermanager.RemoteEntryEditor.AddeStatus;
import edu.wisc.ssec.mcidasv.ui.BetterJTable;
import edu.wisc.ssec.mcidasv.util.McVTextField.Prompt;

/**
 * This class is the GUI frontend to {@link EntryStore} (the server manager).
 * It allows users to manipulate their local and remote ADDE data.
 */
// TODO(jon): don't forget to persist tab choice and window position. maybe also the "positions" of the scrollpanes (if possible).
// TODO(jon): GUI could look much better.
// TODO(jon): finish up the javadocs.
@SuppressWarnings("serial")
public class TabbedAddeManager extends JFrame {

    /** Pretty typical logger object. */
    private final static Logger logger = LoggerFactory.getLogger(TabbedAddeManager.class);

    private static final Icon system = icon("/edu/wisc/ssec/mcidasv/resources/icons/servermanager/padlock_closed.png");
    private static final Icon mctable = icon("/edu/wisc/ssec/mcidasv/resources/icons/servermanager/bug.png");
    private static final Icon user = icon("/edu/wisc/ssec/mcidasv/resources/icons/servermanager/hand_pro.png");
    private static final Icon invalid = icon("/edu/wisc/ssec/mcidasv/resources/icons/servermanager/emotion_sad.png");
//    private static final Icon verified = icon("/edu/wisc/ssec/mcidasv/resources/icons/servermanager/emotion_smile.png");
    private static final Icon unverified = icon("/edu/wisc/ssec/mcidasv/resources/icons/servermanager/eye_inv.png");

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
    };

    /** Reference back to the McV god object. */
//    private final McIDASV mcv;

    /** Reference to the actual server manager. */
    private final EntryStore serverManager;

    /** The currently selected {@link RemoteAddeEntry} or {@code null} if nothing is selected. */
//    private RemoteAddeEntry selectedRemoteEntry = null;

    private final List<RemoteAddeEntry> selectedRemoteEntries;

    /** The currently selected {@link LocalAddeEntry} or {@code null} if nothing is selected. */
//    private LocalAddeEntry selectedLocalEntry = null;

    private final List<LocalAddeEntry> selectedLocalEntries;

    /**
     * Creates a standalone server manager GUI.
     */
    public TabbedAddeManager() {
        this.serverManager = null;
        this.selectedLocalEntries = arrList();
        this.selectedRemoteEntries = arrList();
        initComponents();
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
        this.serverManager = entryStore;
        this.selectedLocalEntries = arrList();
        this.selectedRemoteEntries = arrList();
        initComponents();
    }

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
        EventBus.publish(Event.SHOWN);
    }

    /**
     * Closes and disposes (if needed) the GUI.
     */
    public void closeManager() {
        EventBus.publish(Event.CLOSED);
        if (isDisplayable()) {
            dispose();
        }
    }

    // TODO(jon): still needs to refresh the local table.
    protected void refreshDisplay() {
        ((RemoteAddeTableModel)remoteTable.getModel()).refreshEntries();
        ((LocalAddeTableModel)localTable.getModel()).refreshEntries();
    }

    public void showRemoteEditor() {
        if (tabbedPane.getSelectedIndex() != 0) {
            tabbedPane.setSelectedIndex(0);
        }
        RemoteEntryEditor editor = new RemoteEntryEditor(this, true, this, serverManager);
        editor.setVisible(true);
    }

    public void showRemoteEditor(final List<RemoteAddeEntry> entries) {
        if (tabbedPane.getSelectedIndex() != 0) {
            tabbedPane.setSelectedIndex(0);
        }
        RemoteEntryEditor editor = new RemoteEntryEditor(this, true, this, serverManager, entries);
        editor.setVisible(true);
    }

    public void removeRemoteEntries(final List<RemoteAddeEntry> entries) {
        if (entries == null) {
            return;
        }
        if (serverManager.removeEntries(entries)) {
            RemoteAddeTableModel tableModel = ((RemoteAddeTableModel)remoteTable.getModel());
            int first = Integer.MAX_VALUE;
            int last = Integer.MIN_VALUE;
            for (RemoteAddeEntry entry : entries) {
                int index = tableModel.getRowForEntry(entry);
                if (index < 0) {
                    continue;
                } else {
                    if (index < first) {
                        first = index;
                    }
                    if (index > last) {
                        last = index;
                    }
                }
            }
            tableModel.fireTableRowsDeleted(first, last);
            refreshDisplay();
            remoteTable.revalidate();
            if (first < remoteTable.getRowCount()) {
                remoteTable.setRowSelectionInterval(first, first);
            }
        } else {
            logger.debug("could not remove entries={}", entries);
        }
    }

    public void showLocalEditor() {
        if (tabbedPane.getSelectedIndex() != 1) {
            tabbedPane.setSelectedIndex(1);
        }
        LocalEntryEditor editor = new LocalEntryEditor(this, true, this, serverManager);
        editor.setVisible(true);
    }

    public void showLocalEditor(final LocalAddeEntry entry) {
        if (tabbedPane.getSelectedIndex() != 1) {
            tabbedPane.setSelectedIndex(1);
        }
        LocalEntryEditor editor = new LocalEntryEditor(this, true, this, serverManager, entry);
        editor.setVisible(true);
    }

    public void removeLocalEntries(final List<LocalAddeEntry> entries) {
        if (entries == null) {
            return;
        }
        if (serverManager.removeEntries(entries)) {
            LocalAddeTableModel tableModel = ((LocalAddeTableModel)localTable.getModel());
            int first = Integer.MAX_VALUE;
            int last = Integer.MIN_VALUE;
            for (LocalAddeEntry entry : entries) {
                int index = tableModel.getRowForEntry(entry);
                if (index < 0) {
                    continue;
                } else {
                    if (index < first) {
                        first = index;
                    }
                    if (index > last) {
                        last = index;
                    }
                }
            }
            tableModel.fireTableRowsDeleted(first, last);
            refreshDisplay();
            localTable.revalidate();
            localTable.setRowSelectionInterval(first, first);
        } else {
            logger.debug("could not remove entries={}", entries);
        }
    }

    public void importMctable(final String path, final String username, final String project) {
        final Set<RemoteAddeEntry> imported = EntryTransforms.extractMctableEntries(path, username, project);
        if (imported == Collections.EMPTY_SET) {
            LogUtil.userErrorMessage("Selection does not appear to a valid MCTABLE.TXT file:\n"+path);
        } else {
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

    public void restartLocalServer() {
        serverManager.restartLocalServer();
    }

    @EventSubscriber(eventClass=McservEvent.class)
    public void mcservUpdated(final McservEvent event) {
        final String msg;
        switch (event) {
            case ACTIVE:
                msg = "Local servers are already running.";
                break;
            case DIED:
                msg = "Local servers quit unexpectedly...";
                break;
            case STARTED:
                msg = "Local servers are listening on port "+EntryStore.getLocalPort();
                break;
            case STOPPED:
                msg = "Local servers have been stopped.";
                break;
            default:
                msg = "Unknown local servers status: "+event.toString();
                break;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                statusLabel.setText(msg);
            }
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {
        assert SwingUtilities.isEventDispatchThread();
        ucar.unidata.ui.Help.setTopDir(HELP_TOP_DIR);

        tabbedPane = new JTabbedPane();
        remoteTab = new JPanel();
        remoteTable = new BetterJTable();
        remoteScroller = BetterJTable.createStripedJScrollPane(remoteTable);

        actionPanel = new JPanel();
        newEntryButton = new JButton();
        editEntryButton = new JButton();
        removeEntryButton = new JButton();
        importButton = new JButton();
        localTab = new JPanel();
        localTable = new BetterJTable();
        localScroller = BetterJTable.createStripedJScrollPane(localTable);
        statusPanel = new JPanel();
        statusLabel = new JLabel();
        restartButton = new JButton();
        menuBar = new JMenuBar();
        fileMenu = new JMenu();
        newRemoteItem = new JMenuItem();
        newLocalItem = new JMenuItem();
        fileSeparator1 = new JPopupMenu.Separator();
        closeItem = new JMenuItem();
        editMenu = new JMenu();
        editEntryItem = new JMenuItem();
        removeEntryItem = new JMenuItem();
        helpMenu = new JMenu();
        remoteHelpItem = new JMenuItem();
        localHelpItem = new JMenuItem();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("ADDE Data Manager");
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        remoteTable.setModel(new RemoteAddeTableModel(serverManager));
        remoteTable.setColumnSelectionAllowed(false);
        remoteTable.setRowSelectionAllowed(true);
        remoteTable.getTableHeader().setReorderingAllowed(false);
        remoteTable.setFont(UIManager.getFont("Table.font").deriveFont(11.0f));
        remoteTable.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        remoteTable.setDefaultRenderer(String.class, new TextRenderer());
        remoteTable.getColumnModel().getColumn(0).setPreferredWidth(10);
        remoteTable.getColumnModel().getColumn(1).setPreferredWidth(10);
//        remoteTable.getColumnModel().getColumn(2).setPreferredWidth(10);
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

        newEntryButton.setText("Add New Dataset");
        newEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showRemoteEditor();
            }
        });

        editEntryButton.setText("Edit Dataset");
        editEntryButton.setEnabled(false);
        editEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (tabbedPane.getSelectedIndex() == 0) {
                    showRemoteEditor(getSelectedRemoteEntries());
                } else {
                    showLocalEditor(getSingleLocalSelection());
                }
            }
        });

        removeEntryButton.setText("Remove Selection");
        removeEntryButton.setEnabled(false);
        removeEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (tabbedPane.getSelectedIndex() == 0) {
                    removeRemoteEntries(getSelectedRemoteEntries());
                } else {
                    removeLocalEntries(getSelectedLocalEntries());
                }
            }
        });

        importButton.setText("Import MCTABLE...");
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });

        GroupLayout actionPanelLayout = new GroupLayout(actionPanel);
        actionPanel.setLayout(actionPanelLayout);
        actionPanelLayout.setHorizontalGroup(
            actionPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(actionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(newEntryButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editEntryButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(removeEntryButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(importButton)
                .addContainerGap(77, Short.MAX_VALUE))
        );
        actionPanelLayout.setVerticalGroup(
            actionPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(actionPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(newEntryButton)
                .addComponent(editEntryButton)
                .addComponent(removeEntryButton)
                .addComponent(importButton))
        );

        GroupLayout remoteTabLayout = new GroupLayout(remoteTab);
        remoteTab.setLayout(remoteTabLayout);
        remoteTabLayout.setHorizontalGroup(
            remoteTabLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, remoteTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(remoteTabLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(remoteScroller, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
                    .addComponent(actionPanel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        remoteTabLayout.setVerticalGroup(
            remoteTabLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(remoteTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(remoteScroller, GroupLayout.PREFERRED_SIZE, 291, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(actionPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabbedPane.addTab("Remote Data", remoteTab);

        localTable.setModel(new LocalAddeTableModel(serverManager));
        localTable.setColumnSelectionAllowed(false);
        localTable.setRowSelectionAllowed(true);
        localTable.getTableHeader().setReorderingAllowed(false);
        localScroller.setViewportView(localTable);
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

        if (!serverManager.checkLocalServer()) {
            statusLabel.setText("Local server is not running.");
            restartButton.setText("Start Me!");
        }
        else {
            statusLabel.setText("Local server is running.");
            restartButton.setText("Restart Me!");
        }
        restartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restartLocalServer();
            }
        });

        GroupLayout statusPanelLayout = new GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
                .addComponent(statusLabel)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 314, Short.MAX_VALUE)
                .addComponent(restartButton))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(restartButton)
                .addComponent(statusLabel))
        );

        GroupLayout localTabLayout = new GroupLayout(localTab);
        localTab.setLayout(localTabLayout);
        localTabLayout.setHorizontalGroup(
            localTabLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, localTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(localTabLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(localScroller, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
                    .addComponent(statusPanel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        localTabLayout.setVerticalGroup(
            localTabLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(localTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(localScroller, GroupLayout.PREFERRED_SIZE, 289, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        tabbedPane.addTab("Local Data", localTab);

        fileMenu.setText("File");

        newRemoteItem.setText("New Remote Dataset");
        newRemoteItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showRemoteEditor();
            }
        });
        fileMenu.add(newRemoteItem);

        newLocalItem.setText("New Local Dataset");
        newLocalItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showLocalEditor();
            }
        });
        fileMenu.add(newLocalItem);
        fileMenu.add(fileSeparator1);

        closeItem.setText("Close");
        closeItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logger.debug("evt={}", evt.toString());
                closeManager();
            }
        });
        fileMenu.add(closeItem);

        menuBar.add(fileMenu);

        editMenu.setText("Edit");
        editEntryItem.setText("Edit Entry...");
        editEntryItem.setEnabled(false);
        editEntryItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (tabbedPane.getSelectedIndex() == 0) {
                    showRemoteEditor(getSelectedRemoteEntries());
                } else {
                    showLocalEditor(getSingleLocalSelection());
                }
            }
        });

        removeEntryItem.setText("Remove Selection");
        removeEntryItem.setEnabled(false);
        removeEntryItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (tabbedPane.getSelectedIndex() == 0) {
                    removeRemoteEntries(getSelectedRemoteEntries());
                } else {
                    removeLocalEntries(getSelectedLocalEntries());
                }
            }
        });
        editMenu.add(editEntryItem);
        editMenu.add(removeEntryItem);
        menuBar.add(editMenu);

        helpMenu.setText("Help");

        remoteHelpItem.setText("Show Remote Data Help");
        remoteHelpItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(REMOTE_HELP_TARGET);
            }
        });
        helpMenu.add(remoteHelpItem);

        localHelpItem.setText("Show Local Data Help");
        localHelpItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(LOCAL_HELP_TARGET);
            }
        });
        helpMenu.add(localHelpItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 558, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE))
        );

        tabbedPane.setSelectedIndex(getLastTab());
        tabbedPane.getAccessibleContext().setAccessibleName("Remote Data");
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                boolean hasSelection = false;
                int index = tabbedPane.getSelectedIndex();
                if (index == 0) {
                    hasSelection = hasRemoteSelection();
                } else {
                    hasSelection = hasLocalSelection();
                }

                editEntryButton.setEnabled(hasSelection);
                editEntryItem.setEnabled(hasSelection);
                removeEntryButton.setEnabled(hasSelection);
                removeEntryItem.setEnabled(hasSelection);
                setLastTab(index);
            }
        });
        pack();
    }// </editor-fold>

    /**
     * I respond to events! Yyyyaaaaaaayyyyyy!!!!
     * 
     * @param e
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
            RemoteAddeTableModel tableModel = ((RemoteAddeTableModel)remoteTable.getModel());
            selectedEntries = newLinkedHashSet();
            for (int i = min; i <= max; i++) {
                if (selModel.isSelectedIndex(i)) {
                    selectedEntries.addAll(tableModel.getEntriesAtRow(i));
                    selectedRowCount++;
                }
            }
        }

        setSelectedRemoteEntries(selectedEntries);

        // the current "edit" dialog doesn't work so well with multiple 
        // servers/datasets, so only allow the user to edit entries one at a time.
        boolean singleSelection = selectedRowCount == 1;
        editEntryButton.setEnabled(singleSelection);
        editEntryItem.setEnabled(singleSelection);

        boolean hasSelection = selectedRowCount >= 1;
        removeEntryButton.setEnabled(hasSelection);
        removeEntryItem.setEnabled(hasSelection);
    }

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
            LocalAddeTableModel tableModel = ((LocalAddeTableModel)localTable.getModel());
            selectedEntries = newLinkedHashSet();
            for (int i = min; i <= max; i++) {
                if (selModel.isSelectedIndex(i)) {
                    selectedEntries.add(tableModel.getEntryAtRow(i));
                }
            }
        }

        setSelectedLocalEntries(selectedEntries);

        // the current "edit" dialog doesn't work so well with multiple 
        // servers/datasets, so only allow the user to edit entries one at a time.
        boolean singleSelection = (selectedEntries.size() == 1);
        editEntryButton.setEnabled(singleSelection);
        editEntryItem.setEnabled(singleSelection);

        boolean hasSelection = !selectedEntries.isEmpty();
        removeEntryButton.setEnabled(hasSelection);
        removeEntryItem.setEnabled(hasSelection);
    }

    /**
     * Checks to see if {@link #selectedRemoteEntries} contains any 
     * {@link RemoteAddeEntry}s.
     */
    private boolean hasRemoteSelection() {
        return !selectedRemoteEntries.isEmpty();
    }

    /**
     * Checks to see if {@link {@link #selectedLocalEntries} contains any 
     * {@link LocalAddeEntry}s.
     */
    private boolean hasLocalSelection() {
        return !selectedLocalEntries.isEmpty();
    }

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

    private boolean hasSingleLocalSelection() {
        return (selectedLocalEntries.size() == 1);
    }

    private LocalAddeEntry getSingleLocalSelection() {
        LocalAddeEntry entry = LocalAddeEntry.INVALID_ENTRY;
        if (selectedLocalEntries.size() == 1) {
            entry = selectedLocalEntries.get(0);
        }
        return entry;
    }

    private void setSelectedRemoteEntries(final Collection<RemoteAddeEntry> entries) {
        selectedRemoteEntries.clear();
        selectedRemoteEntries.addAll(entries);
        logger.trace("remote entries={}", entries);
    }

    private List<RemoteAddeEntry> getSelectedRemoteEntries() {
        if (selectedRemoteEntries.isEmpty()) {
            return Collections.emptyList();
        } else {
            return arrList(selectedRemoteEntries);
        }
    }

    private void setSelectedLocalEntries(final Collection<LocalAddeEntry> entries) {
        selectedLocalEntries.clear();
        selectedLocalEntries.addAll(entries);
        logger.trace("local entries={}", entries);
    }

    private List<LocalAddeEntry> getSelectedLocalEntries() {
        if (selectedLocalEntries.isEmpty()) {
            return Collections.emptyList();
        } else {
            return arrList(selectedLocalEntries);
        }
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {
        logger.debug("evt={}", evt.toString());
        closeManager();
    }

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
                .addContainerGap(55, Short.MAX_VALUE))
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

            String forceUser = importUser.getText();
            if (forceUser.length() == 0) {
                forceUser = AddeEntry.DEFAULT_ACCOUNT.getUsername();
            }

            String forceProj = importProject.getText();
            if (forceProj.length() == 0) {
                forceProj = AddeEntry.DEFAULT_ACCOUNT.getProject();
            }

            importMctable(path, forceUser, forceProj);
            // don't worry about file validity; i'll just assume the user clicked
            // on the wrong entry by accident.
            setLastImportPath(f.getParent());
        }
    }

    /**
     * Returns the directory that contained the most recently imported MCTABLE.TXT.
     */
    private String getLastImportPath() {
        return serverManager.getIdvStore().get(LAST_IMPORTED, "");
    }

    /**
     * Saves the directory that contained the most recently imported MCTABLE.TXT.
     */
    private void setLastImportPath(final String path) {
        String okayPath = (path == null) ? "" : path;
        serverManager.getIdvStore().put(LAST_IMPORTED, okayPath);
    }

    /**
     * Returns the index of the user's last server manager tab.
     */
    private int getLastTab() {
        return serverManager.getIdvStore().get(LAST_TAB, 0);
    }

    /**
     * Saves the index of the last server manager tab the user was looking at.
     */
    private void setLastTab(final int index) {
        int okayIndex = ((index >= 0) && (index < 2)) ? index : 0;
        IdvObjectStore store = serverManager.getIdvStore();
        store.put(LAST_TAB, okayIndex);
    }

    // stupid adde.ucar.edu entries never seem to time out! great! making the gui hang is just so awesome!
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


    private class BetterCheckTask implements Callable<List<RemoteAddeEntry>> {
        private final RemoteAddeEntry entry;
        public BetterCheckTask(final RemoteAddeEntry entry) {
            this.entry = entry;
            this.entry.setEntryValidity(EntryValidity.VALIDATING);
        }
        public List<RemoteAddeEntry> call() {
            List<RemoteAddeEntry> valid = arrList();
            if (RemoteEntryEditor.checkHost(entry)) {
                for (RemoteAddeEntry tmp : EntryTransforms.createEntriesFrom(entry)) {
                    if (RemoteEntryEditor.checkEntry(false, tmp) == AddeStatus.OK) {
                        tmp.setEntryValidity(EntryValidity.VERIFIED);
                        valid.add(tmp);
                    }
                }
            }
            if (!valid.isEmpty()) {
                entry.setEntryValidity(EntryValidity.VERIFIED);
//                serverManager.replaceEntries(Collections.singletonList(entry), valid);
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
        public RemoteAddeEntry call() {
            AddeStatus status = RemoteEntryEditor.checkEntry(entry);
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

        /** Labels that appear as the column headers. */
        private final String[] columnNames = {
            "Valid", "Source", "Dataset", "Accounting", "Data Types"
        };

        private final List<String> servers;

        /** {@link EntryStore} used to query and apply changes. */
        private final EntryStore entryStore;

        /**
         * 
         * 
         * @param entryStore
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
         * @see List#indexOf(Object)
         */
        protected int getRowForEntry(final RemoteAddeEntry entry) {
            return getRowForEntry(entry.getEntryText());
        }

        protected int getRowForEntry(final String entryText) {
            return servers.indexOf(entryText);
        }

        /**
         * Clears and re-adds all {@link RemoteAddeEntry}s within {@link #entries}. 
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
         * @param row
         * @param column
         * 
         * @return
         * 
         * @throws IndexOutOfBoundsException
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
            String[] chunks = serv.split("!");
            Set<EntryType> types = Collections.emptySet();
            if (chunks.length == 2) {
                types = manager.getTypes(chunks[0], chunks[1]);
            }
            return types.contains(type);
        }

        private static String formattedTypes(final String serv, final EntryStore manager) {
            String[] chunks = serv.split("!");
            Set<EntryType> types = Collections.emptySet();
            if (chunks.length == 2) {
                types = manager.getTypes(chunks[0], chunks[1]);
            }

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
            List<LocalAddeEntry> selected = arrList();
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
         * @param row
         * @param column
         * 
         * @return
         * 
         * @throws IndexOutOfBoundsException
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

    public class TextRenderer extends DefaultTableCellRenderer {

        private Font bold;
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

    private static Icon icon(final String path) {
        return GuiUtils.getImageIcon(path, TabbedAddeManager.class, true);
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TabbedAddeManager().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify
    private JPanel actionPanel;
    private JMenuItem closeItem;
    private JButton editEntryButton;
    private JMenu fileMenu;
    private JMenu editMenu;
    private JPopupMenu.Separator fileSeparator1;
    private JMenu helpMenu;
    private JButton importButton;
    private JTable localTable;
    private JScrollPane localScroller;
    private JMenuItem localHelpItem;
    private JPanel localTab;
    private JMenuBar menuBar;
    private JButton newEntryButton;
    private JMenuItem newLocalItem;
    private JMenuItem newRemoteItem;
    private JMenuItem remoteHelpItem;
    private JMenuItem editEntryItem;
    private JMenuItem removeEntryItem;
    private JScrollPane remoteScroller;
    private JPanel remoteTab;
    private JTable remoteTable;
    private JButton removeEntryButton;
    private JButton restartButton;
    private JLabel statusLabel;
    private JPanel statusPanel;
    private JTabbedPane tabbedPane;
    private JCheckBox importAccountBox;
    private JTextField importUser;
    private JTextField importProject;
    // End of variables declaration
    
}
