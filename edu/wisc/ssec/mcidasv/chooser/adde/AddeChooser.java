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

package edu.wisc.ssec.mcidasv.chooser.adde;

import static edu.wisc.ssec.mcidasv.servermanager.AddeEntry.DEFAULT_ACCOUNT;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.McIDASV.isLoopback;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.EOFException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import edu.wisc.ssec.mcidas.adde.AddeURLException;
import edu.wisc.ssec.mcidas.adde.DataSetInfo;

import visad.DateTime;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.idv.chooser.adde.AddeServer.Group;
import ucar.unidata.util.DatedThing;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlObjectStore;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.ParameterSet;
import edu.wisc.ssec.mcidasv.PersistenceManager;
import edu.wisc.ssec.mcidasv.servermanager.AddeAccount;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EditorAction;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.EntryStore;
import edu.wisc.ssec.mcidasv.servermanager.EntryTransforms;
import edu.wisc.ssec.mcidasv.servermanager.LocalEntryEditor;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry;
import edu.wisc.ssec.mcidasv.servermanager.RemoteEntryEditor;
import edu.wisc.ssec.mcidasv.servermanager.TabbedAddeManager;
import edu.wisc.ssec.mcidasv.ui.ParameterTree;
import edu.wisc.ssec.mcidasv.ui.UIManager;
import edu.wisc.ssec.mcidasv.util.CollectionHelpers;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.TextColor;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

/**
 *
 * @version $Revision$
 */
public class AddeChooser extends ucar.unidata.idv.chooser.adde.AddeChooser implements Constants {
    
    private static final Logger logger = LoggerFactory.getLogger(AddeChooser.class);
    
    private JComboBox serverSelector;
    
    /** List of descriptors */
    private PreferenceList descList;
    
    /** Descriptor/name hashtable */
    protected Hashtable descriptorTable;
    
    /** List of available descriptors. */
    protected List<String> descriptorList;
    
    /** List of comments associated with list of descriptors. */
    protected List<String> commentList;
    
    /** Property for the descriptor table */
    public static final String DESCRIPTOR_TABLE = "DESCRIPTOR_TABLE";

    /** Connect button--we need to be able to disable this */
    JButton connectButton = McVGuiUtils.makeImageTextButton(ICON_CONNECT_SMALL, "Connect");

    /** Parameter button--we need to be able to disable this */
    JButton parameterButton =
        McVGuiUtils.makeImageButton("/edu/wisc/ssec/mcidasv/resources/icons/toolbar/document-open22.png",
            this, "doParameters", null, "Load parameter set");

    /** Manage button */
    JButton manageButton =
        McVGuiUtils.makeImageButton("/edu/wisc/ssec/mcidasv/resources/icons/toolbar/preferences-system22.png",
            this, "doManager", null, "Manage servers");

    /** Public button--we need to draw a menu from this */
    JButton publicButton =
        McVGuiUtils.makeImageButton("/edu/wisc/ssec/mcidasv/resources/icons/toolbar/show-layer-controls22.png",
            this, "showGroups", null, "List public datasets");

    /** descriptor label */
    protected JLabel descriptorLabel = new JLabel(getDescriptorLabel()+":");

    /** A widget for the list of dataset descriptors */
    protected JComboBox descriptorComboBox = new JComboBox();

    /** The descriptor names */
    protected String[] descriptorNames;

    /** Flag to keep from infinite looping */
    protected boolean ignoreDescriptorChange = false;

    /**
     * List of JComponent-s that depend on a descriptor being selected
     * to be enabled
     */
    protected ArrayList compsThatNeedDescriptor = new ArrayList();

    /** Selection label text */
    protected String LABEL_SELECT = " -- Select -- ";

    /** Separator string */
    protected static String separator = "----------------";

    /** Name separator string */
    protected static String nameSeparator = " - ";

    /** Reference back to the server manager */
    protected EntryStore serverManager;

    public boolean allServersFlag;

    /** Command for opening up the server manager */
    protected static final String CMD_MANAGER = "cmd.manager";

    private String lastBadServer = "";
    private String lastBadGroup = "";

    private String lastServerName = "";
    private String lastServerGroup = "";
    private String lastServerUser = "";
    private String lastServerProj = "";
    private AddeServer lastServer = new AddeServer("");

    private List<AddeServer> addeServers;

    /** Used for parameter set restore */
    private static final String TAG_FOLDER = "folder";
    private static final String TAG_DEFAULT = "default";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SERVER = "server";
    private static final String ATTR_GROUP = "GROUP";
    private static final String ATTR_DESCRIPTOR = "DESCRIPTOR";
    private static final String ATTR_POS = "POS";
    private static final String ATTR_DAY = "DAY";
    private static final String ATTR_TIME = "TIME";
    private List restoreTimes = new ArrayList();
    public Element restoreElement;
    private boolean shouldAddSource = false;
    final JCheckBox cb = new JCheckBox("Add source",shouldAddSource);

    /** Maps favorite type to the BundleTree that shows the Manage window for the type */
    private Hashtable parameterTrees = new Hashtable();

    /**
     * Create an AddeChooser associated with an IdvChooser
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        AnnotationProcessor.process(this);
        descriptorList = new ArrayList<String>();
        commentList = new ArrayList<String>();
        
        simpleMode = !getProperty(IdvChooser.ATTR_SHOWDETAILS, true);

        loadButton = McVGuiUtils.makeImageTextButton(ICON_ACCEPT_SMALL, getLoadCommandName());
        loadButton.setActionCommand(getLoadCommandName());
        loadButton.addActionListener(this);

        cancelButton = McVGuiUtils.makeImageButton(ICON_CANCEL, "Cancel");
        cancelButton.setActionCommand(GuiUtils.CMD_CANCEL);
        cancelButton.addActionListener(this);
        cancelButton.setEnabled(false);

        serverSelector = getServerSelector();

        serverSelector.setToolTipText("Right click to manage servers");
        serverSelector.getEditor().getEditorComponent().addMouseListener(
            new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    if (!SwingUtilities.isRightMouseButton(e)) {
                        return;
                    }

                    AddeServer server = getAddeServer();
                    if (server == null) {
                        return;
                    }
                    List<JMenuItem> items = new ArrayList<JMenuItem>();

                    // Set the right-click behavior
                    if (isLocalServer()) {
                        items.add(GuiUtils.makeMenuItem("Manage local ADDE data",
                            AddeChooser.this,
                            "doManager", null));
                    }
                    else {
                        items.add(GuiUtils.makeMenuItem("Manage ADDE servers",
                            AddeChooser.this,
                            "doManager", null));
                    }
                    JPopupMenu popup = GuiUtils.makePopupMenu(items);
                    popup.show(serverSelector, e.getX(), e.getY());
                }
            });
        serverSelector.setMaximumRowCount(16);

        groupSelector.setToolTipText("Right click to manage servers");
        groupSelector.getEditor().getEditorComponent().addMouseListener(
            new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    if (!SwingUtilities.isRightMouseButton(e)) {
                        return;
                    }

                    AddeServer server = getAddeServer();
                    if (server == null) {
                        return;
                    }
                    List<JMenuItem> items = new ArrayList<JMenuItem>();

                    // Set the right-click behavior
                    if (isLocalServer()) {
                        items.add(GuiUtils.makeMenuItem("Manage local ADDE data",
                            AddeChooser.this, "doManager", null));
                    }
                    else {
                        items.add(GuiUtils.makeMenuItem("Manage ADDE servers",
                            AddeChooser.this, "doManager", null));
                    }
                    JPopupMenu popup = GuiUtils.makePopupMenu(items);
                    popup.show(groupSelector, e.getX(), e.getY());
                }
            });
        groupSelector.setMaximumRowCount(16);

        //        serverManager = ((McIDASV)getIdv()).getServerManager();
        //        serverManager.addManagedChooser(this);
        addServerComp(descriptorLabel);
        //        addServerComp(descriptorComboBox);

        descriptorComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( !ignoreDescriptorChange
                    && (e.getStateChange() == e.SELECTED)) {
                    descriptorChanged();
                }
            }
        });

        // Update the server list and load the saved state
        updateServerList();
        loadServerState();

        // Default to no parameter button unless the overriding class wants one
        hideParameterButton();
    }

    /**
     * Force a reload of the available servers and groups.
     */
    public void updateServerList() {
        updateServers();
        updateGroups();
    }

    /**
     * Returns a {@link java.util.Map Map} containing {@code user} and {@code proj}
     * keys for the given {@code server/group} combination.
     * 
     * <p>The values are either the specific ADDE account details for 
     * {@code server/group} or {@link edu.wisc.ssec.mcidasv.servermanager.AddeEntry#DEFAULT_ACCOUNT DEFAULT_ACCOUNT}
     * values.
     * 
     * @param server Server name. Should not be {@code null}.
     * @param group Group name on {@code name}. Should not be {@code null}.
     * 
     * @return {@code Map} containing the accounting details for {@code server/group}.
     */
    protected Map<String, String> getAccounting(final String server, final String group) {
        Map<String, String> acctInfo = new HashMap<String, String>();
        EntryStore entryStore = ((McIDASV)getIdv()).getServerManager();
        String strType = this.getDataType();
        EntryType type = EntryTransforms.strToEntryType(strType);
        AddeAccount acct = entryStore.getAccountingFor(server, group, type);
        acctInfo.put("user", acct.getUsername());
        acctInfo.put("proj", acct.getProject());
        return acctInfo;
    }

    /**
     * Returns a {@link java.util.Map Map} containing {@code user} and {@code proj}
     * keys for the given {@code server/group} combination.
     * 
     * <p>The values are either the specific ADDE account details for 
     * {@code server/group} or {@link edu.wisc.ssec.mcidasv.servermanager.AddeEntry#DEFAULT_ACCOUNT DEFAULT_ACCOUNT}
     * values.
     * 
     * @param server Server name. Should not be {@code null}.
     * @param group Group name on {@code name}. Should not be {@code null}.
     * 
     * @return {@code Map} containing the accounting details for {@code server/group}.
     */
    protected Map<String, String> getAccounting(final AddeServer server, final String group) {
        return getAccounting(server.getName(), group);
    }

    private List<AddeServer> getManagedServers(final String type) {
        EntryStore entryStore = ((McIDASV)getIdv()).getServerManager();
        return arrList(entryStore.getIdvStyleEntries(type));
    }

    public void updateServers() {
        Object selected = serverSelector.getSelectedItem();

        String type = getGroupType();
        List<AddeServer> managedServers = getManagedServers(type);
        List<AddeServer> localList = arrList();
        List<AddeServer> remoteList = arrList();
        addeServers = CollectionHelpers.arrList();
        for (AddeServer server : managedServers) {
            if (server.getIsLocal())
                localList.add(server);
            else
                remoteList.add(server);
        }

//        logger.debug("{}: updateServers: local size={} contents={}", new Object[] { getDataType(), localList.size(), localList });
//        logger.debug("{}: updateServers: remote size={} contents={}", new Object[] { getDataType(), remoteList.size(), remoteList });

        // server list doesn't need a separator if there's only remote servers
        if (!localList.isEmpty()) {
            addeServers.addAll(localList);
            addeServers.add(new AddeServer(separator));
        }
        Comparator<AddeServer> byServer = new ServerComparator();
        Collections.sort(remoteList, byServer);
        addeServers.addAll(remoteList);

        // always making this call helps to ensure the chooser stays up to date
        // with the server manager.
        GuiUtils.setListData(serverSelector, addeServers);
        if (!addeServers.isEmpty()) {
            if (selected == null || !containsServerName(addeServers, selected)) {
                selected = serverSelector.getItemAt(0);
//                logger.debug("updateServers: selecting item at idx=0, item={} chooser={}", selected, this.getDataType());
            }
            
            int index = getSelectorIndex(selected, serverSelector);
            serverSelector.setSelectedIndex(index);
        }
    }

    /**
     * Searches the given {@link java.util.List List} of {@link ucar.unidata.idv.chooser.adde.AddeServer AddeServers}
     * for {@code server}.
     * 
     * @param servers Servers to search. {@code null} is permitted.
     * @param server Server to search for within {@code servers}. {@code null} is permitted.
     * 
     * @return {@code true} if {@code servers} contains {@code server} or {@code false} otherwise.
     */
    protected static boolean containsServerName(final List<AddeServer> servers, final Object server) {
        if (servers == null || server == null) {
            return false;
        }
        String serverName = (server instanceof AddeServer) ? ((AddeServer)server).getName() : server.toString();
        for (AddeServer tmp : servers) {
            if (tmp.getName().equals(serverName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Searches the given {@link java.util.List List} of {@link ucar.unidata.idv.chooser.adde.AddeServer.Group Groups}
     * for {@code group}.
     * 
     * @param groups Groups to search. {@code null} is permitted.
     * @param group Group to search for within {@code group}. {@code null} is permitted.
     * 
     * @return {@code true} if {@code groups} contains {@code group} or {@code false} otherwise.
     */
    protected static boolean containsGroupName(final List<Group> groups, final Object group) {
        if (groups == null || group == null) {
            return false;
        }
        String groupName = (group instanceof Group) ? ((Group)group).getName() : group.toString();
        for (Group tmp : groups) {
            if (tmp.getName().equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sort the groups alphabetically
     */
    public void updateGroups() {
        if (addingServer || groupSelector == null || getAddeServer() == null)
            return;

        Object selected = groupSelector.getSelectedItem();

        EntryStore servManager = ((McIDASV)getIdv()).getServerManager();

        List<Group> groups = CollectionHelpers.arrList();
        if (isLocalServer()) {
            groups.addAll(servManager.getIdvStyleLocalGroups());
        } else {
            String sel = null;
            Object obj = serverSelector.getSelectedItem();
            if (obj instanceof String) {
                sel = (String)obj;
//                logger.debug("updateGroups: string={} chooser={}", sel, this.getDataType());
            } else if (obj instanceof AddeServer) {
                sel = ((AddeServer)obj).getName();
//                logger.debug("updateGroups: server selection={} chooser={}", sel, this.getDataType());
            } else {
                sel = obj.toString();
//                logger.debug("updateGroups: unknown type={}; toString={}", sel.getClass().getName(), sel);
            }

            EntryType selType = EntryTransforms.strToEntryType(getGroupType());
            groups.addAll(servManager.getIdvStyleRemoteGroups(sel, selType));
        }
//        logger.trace("updateGroups: selected={} (type={}) chooser={} contents={}", new Object[] { serverSelector.getSelectedItem(), serverSelector.getSelectedItem().getClass().getName(), this.getDataType(), groups});
        Comparator<Group> byGroup = new GroupComparator();
        Collections.sort(groups, byGroup);
        GuiUtils.setListData(groupSelector, groups);
        if (!groups.isEmpty()) {
            if (selected == null || !containsGroupName(groups, selected)) {
                selected = groupSelector.getItemAt(0);
            }
            groupSelector.setSelectedItem(selected);
        }
    }

    /**
     * Load any saved server state
     */
    //TODO: Make loadServerState protected in IDV, remove from here
    private void loadServerState() {
        if (addeServers == null) {
//            logger.debug("loadServerState: addeServers == null chooser={}", this.getDataType());
            return;
        }
        String id = getId();
        String[] serverState =
            (String[]) getIdv().getStore().get(Constants.PREF_SERVERSTATE + '.' + id);
        if (serverState == null) {
//            serverState = Constants.DEFAULT_SERVERSTATE;
//            logger.debug("loadServerState: serverState == null chooser={}",this.getDataType());
            return;
        }
        AddeServer server = AddeServer.findServer(addeServers, serverState[0]);
        if (server == null) {
//            logger.debug("loadServerState: server == null chooser={}",this.getDataType());
            return;
        }
//        logger.debug("loadServerState: selecting server={} chooser={}", server, this.getDataType());
        serverSelector.setSelectedItem(server);
        setGroups();
        updateGroups();
        if (serverState[1] != null) {
            Group group = new Group(getDataType(), serverState[1], serverState[1]);
            int index = getSelectorIndex(group, groupSelector);
            if (index >= 0) {
//                logger.debug("loadServerState: selecting index={} group={} chooser={}", new Object[] { index, group, this.getDataType() });
                groupSelector.setSelectedIndex(index);
            } else {
//                logger.debug("loadServerState: group == null chooser={}", this.getDataType());
            }
        } else {
//            logger.debug("loadServerState: serverState[1] == null chooser={}", this.getDataType());
        }
    }

    /**
     * Decide if the server you're asking about is actually a separator
     */
    protected static boolean isSeparator(AddeServer checkServer) {
        if (checkServer != null) {
            if (checkServer.getName().equals(separator)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decide if the server you're asking about is local
     */
    protected boolean isLocalServer() {
        return isLocalServer(getAddeServer());
    }

    protected static boolean isLocalServer(AddeServer checkServer) {
        if (checkServer != null) {
            return checkServer.getIsLocal();
        }
        return false;
    }

    private void setBadServer(String name, String group) {
        if (name == null) {
            name = "";
        }
        if (group == null) {
            group = "";
        }

        lastBadServer = name;
        lastBadGroup = group;
    }

    private boolean isBadServer(String name, String group) {
        assert lastBadServer != null;
        assert lastBadGroup != null;
        return lastBadServer.equals(name) && lastBadGroup.equals(group);
    }

    private void setLastServer(String name, String group, AddeServer server) {
//        logger.trace("name='{}' group='{}' server='{}' old: name='{}' group='{}' server='{}'", new Object[] { name, group, server, lastServerName, lastServerGroup, lastServer });
        if (name == null) {
            name = "";
        }
        if (group == null) {
            group = "";
        }
        if (server == null) {
            server = new AddeServer(name);
            Group addeGroup = new Group(getDataType(), group, group);
            server.addGroup(addeGroup);
        }
        lastServerName = name;
        lastServerGroup = group;
        lastServer = server;
    }

    private boolean isLastServer(String name, String group) {
        assert lastServer != null;
        assert lastServerName != null;
        assert lastServerGroup != null;
        return lastServerName.equals(name) && lastServerGroup.equals(group);
    }

    @EventSubscriber(eventClass=EntryStore.Event.class)
    public void onServerManagerDataEvent(EntryStore.Event evt) {
        EntryStore servManager = ((McIDASV)getIdv()).getServerManager();
//        logger.debug("onServerManagerDataEvent: evt={} server={}", evt, servManager.getLastAdded());
        this.updateServerList();
    }

    @EventSubscriber(eventClass=TabbedAddeManager.Event.class)
    public void onServerManagerWindowEvent(TabbedAddeManager.Event evt) {
//        logger.debug("onServerManagerWindowEvent: caught event bus obj");
    }

    private boolean addingServer = false;

    /**
     * Search a given {@link JComboBox} for the index of a given object. Mostly
     * useful for searching {@link #serverSelector} or {@link #groupSelector}.
     * 
     * @param needle An object. {@code null} values are permitted.
     * @param haystack {@code JComboBox} to search. {@code null} values are 
     * permitted, but return {@code -1}.
     * 
     * @return Either the index of {@code needle} within {@code haystack}, or
     * {@code -1} if {@code needle} could not be found (or {@code haystack} is 
     * {@code null}).
     */
    protected static int getSelectorIndex(final Object needle, 
        final JComboBox haystack) 
    {
        if (haystack == null) {
            return -1;
        }

        String name = null;
        if (needle instanceof AddeServer) {
            name = ((AddeServer)needle).getName();
        } else if (needle instanceof Group) {
            name = ((Group)needle).getName();
        } else if (needle instanceof AddeEntry) {
            name = ((AddeEntry)needle).getAddress();
        } else {
            name = needle.toString();
        }

        if (isLoopback(name)) {
            return 0;
        }

        for (int i = 0; i < haystack.getItemCount(); i++) {
            Object item = haystack.getItemAt(i);
            String tmpName;
            if (item instanceof AddeServer) {
                tmpName = ((AddeServer)item).getName();
            } else {
                tmpName = item.toString();
            }

            if (name.equals(tmpName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the selected AddeServer
     *
     * @return the server or null
     */
    protected AddeServer getAddeServer() {
        if (lastServerName != null && lastServerName.equals("unset")) {
            return null;
        }

        Object selected = serverSelector.getSelectedItem();
        if ((selected != null) && (selected instanceof AddeServer)) {
            AddeServer server = (AddeServer)selected;
            String group = getGroup(true);
            Map<String, String> accounting = getAccounting(server, group);
//            logger.trace("accounting: new: u='{}' p='{}' old: u='{}' p='{}'", new Object[] { accounting.get("user"), accounting.get("proj"), lastServerUser, lastServerProj });
            lastServerUser = accounting.get("user");
            lastServerProj = accounting.get("proj");
            setLastServer(server.getName(), group, server);
            return (AddeServer)selected;
        } else if ((selected != null) && (selected instanceof String)) {

            EntryStore servManager = ((McIDASV)getIdv()).getServerManager();
            String server = (String)selected;
            String group = getGroup(true);

            if (isBadServer(server, group)) {
//                logger.trace("getAddeServer: returning null; known bad server; server={} group={}", server, group);
                return null;
            }

            if (isLastServer(server, group)) {
//                logger.trace("getAddeServer: returning last server name; server={} group={}", server, group);
                return lastServer;
            }

            EditorAction editorAction = EditorAction.INVALID;
            if (!isLoopback(server)) {
                RemoteEntryEditor editor = new RemoteEntryEditor(servManager, server, "");
                editor.setVisible(true);
                editorAction = editor.getEditorAction();
            } else {
                LocalEntryEditor editor = new LocalEntryEditor(servManager, group);
                editor.setVisible(true);
                editorAction = editor.getEditorAction();
            }

            int servIndex = 0;
            int groupIndex = 0;

            if (editorAction != EditorAction.CANCELLED && editorAction != EditorAction.INVALID) {

                List<AddeServer> added = arrList(EntryTransforms.convertMcvServers(servManager.getLastAddedByType(EntryTransforms.strToEntryType(getDataType()))));
                AddeServer first = null;
                if (!added.isEmpty()) {
                    first = added.get(0);
                    servIndex = getSelectorIndex(first, serverSelector);
                    setLastServer(server, group, first);
                } 

                serverSelector.setSelectedIndex(servIndex);
                groupSelector.setSelectedIndex(groupIndex);
//                logger.trace("getAddeServer: serverIdx={} groupIdx={}", servIndex, groupIndex);

                return first;
            } else {
//                logger.trace("getAddeServer: returning null due to cancel request");
                setBadServer(server, group);
                return null;
            }

            
            
        } else if (selected == null) {
//            logger.trace("getAddeServer: null object in selector; returning null");
        } else {
//            logger.debug("getAddeServer: unknown obj type={}; toString={}", selected.getClass().getName(), selected.toString());
        }
        return null;
    }

    /**
     * A utility to add a component to the list of components that
     * need the descriptor
     *
     * @param comp The component
     * @return The component
     */
    protected JComponent addDescComp(JComponent comp) {
        compsThatNeedDescriptor.add(comp);
        return comp;
    }
    
    /**
     * Set LABEL_SELECT from elsewhere
     */
    protected void setSelectString(String string) {
        LABEL_SELECT = string;
    }
    
    /**
     * Reset the descriptor stuff
     */
    protected void resetDescriptorBox() {
        ignoreDescriptorChange = true;
        descriptorComboBox.setSelectedItem(LABEL_SELECT);
        ignoreDescriptorChange = false;
    }
    
    /**
     * Handle when the user presses the connect button
     *
     * @throws Exception On badness
     */
    public void handleConnect() throws Exception {
        AddeServer server = getAddeServer();
        if (server == null) {
            return;
        }
        setState(STATE_CONNECTING);
        connectToServer();
        handleUpdate();
    }
    
    @Override protected void handleConnectionError(Exception e) {
        logger.error("handling connection error", e);
        if (e != null && e.getMessage() != null) {
            String msg = e.getMessage();
            int msgPos = msg.indexOf("AddeURLException:");
            if (msgPos >= 0 && msg.length() > 18) {
                msg = msg.substring(msgPos + 18);
                setState(STATE_UNCONNECTED);
                setHaveData(false);
                resetDescriptorBox();
                GuiUtils.showDialog("ADDE Error", new JLabel(msg));
                return;
            }
            if (msg.indexOf("Connecting to server:localhost:") >= 0) {
                setState(STATE_UNCONNECTED);
                setHaveData(false);
                resetDescriptorBox();
                GuiUtils.showDialog("ADDE Error", new JLabel("Local server is not responding"));
                return;
            }
        }
        super.handleConnectionError(e);
    }

    /**
     * Handle unknown data set error
     */
    @Override protected void handleUnknownDataSetError() {
        String server = getServer();
        String group = getGroup();
        Map<String, String> acct = getAccounting(server, group);
        String user = acct.get("user");
        String proj = acct.get("proj");

        StringBuilder msg = new StringBuilder("Could not connect to dataset \"");
        msg.append(getGroup()).append("\" on server \"").append(getServer()).append("\".");
        if (DEFAULT_ACCOUNT.getUsername().equals(user) && DEFAULT_ACCOUNT.getProject().equals(proj)) {
            msg.append("\n\nDataset may require ADDE accounting information.");
        } else {
            msg.append("\n\nAccounting information:\nusername: \"")
            .append(user).append("\"\nproject: \"").append(proj).append('"');
        }
        LogUtil.userErrorMessage(msg.toString());
        setState(STATE_UNCONNECTED);
    }

    /**
     * Handle the event
     *
     * @param ae The event
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(CMD_MANAGER)) {
            doManager();
        }
        else {
            super.actionPerformed(ae);
        }
    }

    /**
     * Go directly to the Server Manager
     */
    public void doManager() {
//      if (isLocalServer()) {
//          ((McIDASV)getIdv()).showAddeManager();
//          return;
//      }
        getIdv().getPreferenceManager().showTab(Constants.PREF_LIST_ADDE_SERVERS);
    }
    
    /**
     * Show the parameter restore tree
     */
    public void doParameters() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem mi = new JMenuItem("Manage...");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                System.out.println(ae);
                showParameterSetDialog(getParameterSetType());
            }
        });
        popup.add(mi);
        
        // Add the checkbox to automatically create a data source
        cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                shouldAddSource = cb.isSelected();
            }
        });
        popup.addSeparator();
        popup.add(cb);

        final PersistenceManager pm = (PersistenceManager)getIdv().getPersistenceManager();
        List<ParameterSet> parameterSets = pm.getAllParameterSets(getParameterSetType());

        for (int i=0; i<parameterSets.size(); i++) {
            if (i==0) popup.addSeparator();
            final ParameterSet ps = parameterSets.get(i);
            
            // Parameter set at root
            if (ps.getCategories().size() == 0) {
                mi = new JMenuItem(ps.getName());
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        restoreParameterSet(ps.getElement());
                    }
                });
                popup.add(mi);
            }
            
            // Recurse into folders
            else {
                // Find or make the menu for the given parameter set
                JMenu m = getPopupSubMenuForParameterSet(popup, ps);
                // Create parameter set entry
                mi = new JMenuItem(ps.getName());
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        restoreParameterSet(ps.getElement());
                    }
                });
                m.add(mi);
            }
            
        }

        popup.show(parameterButton, 0, (int) parameterButton.getBounds().getHeight());
    }
    
    private JMenu getPopupSubMenuForParameterSet(JPopupMenu popup, final ParameterSet ps) {
        List<String> menuNames = ps.getCategories();
        if (menuNames.size() < 1) return null;

        // Build the complete menu
        String menuName = menuNames.get(0);
        menuNames.remove(0);
        JMenu theMenu = new JMenu();
        
        // Look for the menu in popup
        boolean found = false;
        for (int i=0; i<popup.getComponentCount(); i++) {
            Component thisComponent = popup.getComponent(i);
            if (thisComponent instanceof JMenu && ((JMenu)thisComponent).getText().equals(menuName)) {
                theMenu = mergeMenuNames((JMenu)thisComponent, menuNames);
                found = true;
            }
        }
        
        // Make a new menu, add the root, return the leaf
        if (!found) {
            JMenu theRoot = new JMenu(menuName);
            theMenu = makeMenuRecursive(theRoot, menuNames);
            popup.add(theRoot);
        }
        
        return theMenu;
    }
    
    /**
     * Make a new recursive menu
     * 
     * @param rootMenu The root menu to add items to
     * @param menuNames List of string names for submenus
     * @return A new JMenu representing the leaf
     */
    private JMenu makeMenuRecursive(JMenu rootMenu, List<String> menuNames) {
        if (menuNames.size() < 1) return rootMenu;
        JMenu newMenu = new JMenu(menuNames.get(0));
        rootMenu.add(newMenu);
        menuNames.remove(0);
        return makeMenuRecursive(newMenu, menuNames);
    }
    
    /**
     * Recurse into a menu, returning either a pointer to the designated names path
     *  or a pointer to the leaf menu added by merging new names
     * 
     * @param thisMenu The root menu to merge
     * @param menuNames List of string names to look for
     * @return A new JMenu representing the leaf matched by menuNames
     */
    private JMenu mergeMenuNames(JMenu thisMenu, List<String> menuNames) {
        if (menuNames.size() < 1) return thisMenu;
        boolean found = false;
        String menuName = menuNames.get(0);
        for (int i=0; i<thisMenu.getItemCount(); i++) {
            JMenuItem mi = thisMenu.getItem(i);
            if (!(mi instanceof JMenu)) continue;
            if (mi.getText().equals(menuName)) {
                menuNames.remove(0);
                thisMenu = mergeMenuNames((JMenu)mi, menuNames);
                found = true;
            }
        }
        if (!found) {
            thisMenu = makeMenuRecursive(thisMenu, menuNames);
        }
        return thisMenu;
    }
    
    /**
     * Return the parameter type associated with this chooser.  Override!
     */
    protected String getParameterSetType() {
        return "adde";
    }
    
    /**
     * Show the parameter set manager.
     */
    private void showParameterSetDialog(final String parameterSetType) {
        ParameterTree tree = (ParameterTree) parameterTrees.get(parameterSetType);
        if (tree == null) {
            tree = new ParameterTree((UIManager)getIdv().getIdvUIManager() , parameterSetType);
            parameterTrees.put(parameterSetType, tree);
        }
        else {
            //DAVEP
            System.out.println("Should refresh the parameter tree here");
        }
        tree.setVisible(true);
    }

    /**
     * Clear the selected parameter set.
     */
    protected void clearParameterSet() {
        restoreElement = null;
        restoreTimes = new ArrayList(); 
        shouldAddSource = false;
    }

    /**
     * Restore the selected parameter set using element attributes.
     * 
     * @param restoreElement {@code Element} with the desired attributes.
     * {@code null} values are permitted.
     *
     * @return {@code true} if the parameter set was restored, {@code false}
     * otherwise.
     */
    protected boolean restoreParameterSet(Element restoreElement) {
        if (restoreElement == null) return false;
        if (!restoreElement.getTagName().equals("default")) return false;

        this.restoreElement = restoreElement;

        boolean oldISCE = ignoreStateChangedEvents;
        ignoreStateChangedEvents = true;

        // Restore server
        String server = restoreElement.getAttribute(ATTR_SERVER);
        if (server != null) serverSelector.setSelectedItem(new AddeServer(server));

        // Restore group
        String group = restoreElement.getAttribute(ATTR_GROUP);
        if (group != null) groupSelector.setSelectedItem(group);

        // Act as though the user hit "connect"
        readFromServer();

        // Restore descriptor
        String descriptor = restoreElement.getAttribute(ATTR_DESCRIPTOR);
        if (descriptor != null) {
            Enumeration enumeration = descriptorTable.keys();
            for (int i = 0; enumeration.hasMoreElements(); i++) {
                String key = enumeration.nextElement().toString();
                Object val = descriptorTable.get(key);
                if (descriptor.equals(val)) {
                    descriptorComboBox.setSelectedItem(val + nameSeparator + key);
                    descriptorChanged();
                    break;
                }
            } 
        }

        // Restore date/time
        if (restoreElement.hasAttribute(ATTR_POS)) {
            setDoAbsoluteTimes(false);
            Integer pos = new Integer(restoreElement.getAttribute(ATTR_POS));
            if (pos.intValue() >= 0) {
                getRelativeTimesList().setSelectedIndex(pos);
            }
            restoreTimes = new ArrayList(); 
        }
        else if ((restoreElement.hasAttribute(ATTR_DAY)) && (restoreElement.hasAttribute(ATTR_TIME))) {
            setDoAbsoluteTimes(true);
            String dateStr = restoreElement.getAttribute(ATTR_DAY);
            String timeStr = restoreElement.getAttribute(ATTR_TIME);
            List dateS = StringUtil.split(dateStr, ",");
            List timeS = StringUtil.split(timeStr, ",");
            int numImages = timeS.size();
            restoreTimes = new ArrayList(); 
            try {
                DateTime dt = new DateTime();
                dt.resetFormat();
                String dtformat = dt.getFormatPattern();
                for (int ix=0; ix<numImages; ix++) {
                    DateTime restoreTime = dt.createDateTime((String)dateS.get(ix) + " " + (String)timeS.get(ix));
                    restoreTimes.add(restoreTime);
                }
            } catch (Exception e) {
                System.out.println("Exception e=" + e);
                return false;
            }
        }

        System.out.println("Returning from AddeChooser.restoreParameterSet()");

        ignoreStateChangedEvents = oldISCE;
        return true;
    }

    /**
     * Set the absolute times list. The times list can contain any of the object types
     * that makeDatedObjects knows how to handle, i.e., Date, visad.DateTime, DatedThing, AddeImageDescriptor, etc.
     *
     * @param times List of thinggs to put into absolute times list
     */
    protected void setAbsoluteTimes(List times) {
        super.setAbsoluteTimes(times);
        restoreAbsoluteTimes();
    }

    protected void restoreAbsoluteTimes() {
        List allTimes = makeDatedObjects(super.getAbsoluteTimes());
        if (restoreTimes.size() > 0 && allTimes.size() > 0) {
            int[] indices  = new int[restoreTimes.size()];
            try {
                DateTime rtdt;
                DateTime atdt;
                DatedThing at;
                for (int i = 0; i < restoreTimes.size(); i++) {
                    rtdt = (DateTime)restoreTimes.get(i);
                    for (int j = 0; j < allTimes.size(); j++) {
                        at = (DatedThing)allTimes.get(j);
                        atdt = new DateTime(at.getDate());
                        if (atdt.equals(rtdt)) {
                            indices[i] = j;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception e=" + e);
            }
            setSelectedAbsoluteTimes(indices);
        }
    }

    /**
     * show/hide the parameter restore button
     */
    public void showParameterButton() {
        parameterButton.setVisible(true);
    }

    public void hideParameterButton() {
        parameterButton.setVisible(false);
    }

    /**
     * Override and simulate clicking Add Source if requested
     */
    public void setHaveData(boolean have) {
        super.setHaveData(have);
        if (have && shouldAddSource) {
            // Even though setHaveData should mean we can go, we can't... wait a few jiffies
            Misc.runInABit(100, AddeChooser.this, "doClickLoad", null);
        }
    }

    public void doClickLoad() {
        loadButton.doClick();
    }

    public void showServers() {
        allServersFlag = !allServersFlag;
        XmlObjectStore store = getIdv().getStore();
        store.put(Constants.PREF_SYSTEMSERVERSIMG, allServersFlag);
        store.save();
        updateServers();
        updateGroups();
    }

    protected String getStateString() {
        int state = getState();
        switch (state) {
            case STATE_CONNECTED: return "Connected to server";
            case STATE_UNCONNECTED: return "Not connected to server";
            case STATE_CONNECTING: return "Connecting to server";
            default: return "Unknown state: " + state;
        }
    }

    /**
     * Disable/enable any components that depend on the server.
     * Try to update the status label with what we know here.
     */
    protected void updateStatus() {
        super.updateStatus();
        if (getState() == STATE_CONNECTED) {
            lastServer = new AddeServer("");
            lastServerGroup = "";
            lastServerName = "";
            lastServerProj = "";
            lastServerUser = "";

            if (!haveDescriptorSelected()) {
                if (!usingStations() || haveStationSelected()) {
                    //                String name = getDataName().toLowerCase();
                    String name = getDescriptorLabel().toLowerCase();
                    if (StringUtil.startsWithVowel(name)) {
                        setStatus("Please select an " + name);
                    } else {
                        setStatus("Please select a " + name);
                    }
                }
            }
        }

        GuiUtils.enableTree(connectButton, getState() != STATE_CONNECTING);
    }
    
    /**
     * Get the data type ID
     *
     * @return  the data type
     */
    public String getDataType() {
        return "ANY";
    }

    /**
     * Check if the server is ok
     *
     * @return status code
     */
    protected int checkIfServerIsOk() {
        EntryStore servManager = ((McIDASV)getIdv()).getServerManager();
        if (isLocalServer() && !servManager.checkLocalServer()) {
        	LogUtil.userErrorMessage("Local servers are stopped.\n\nLocal servers can be restarted from the 'Tools' menu:\n  Tools > Manage ADDE Datasets >\nLocal Servers > Start Local Servers");
        	logger.info("Local servers are stopped");
            return STATUS_ERROR;
        }
        try {
            StringBuffer buff = getUrl(REQ_TEXT);
            appendKeyValue(buff, PROP_FILE, FILE_PUBLICSRV);
            URL           url  = new URL(buff.toString());
            URLConnection urlc = url.openConnection();
            InputStream   is   = urlc.getInputStream();
            is.close();
            return STATUS_OK;
        } catch (AddeURLException ae) {
            String aes = ae.toString();
            if (aes.indexOf("Invalid project number") >= 0 ||
                aes.indexOf("Invalid user id") >= 0 ||
                aes.indexOf("Accounting data") >= 0) {
                LogUtil.userErrorMessage("Invalid login.\n\nPlease verify your username and password.");
                logger.info("Invalid login");
                setState(STATE_UNCONNECTED);
                setHaveData(false);
                resetDescriptorBox();
                return STATUS_NEEDSLOGIN;
            }
            if (aes.indexOf("cannot run server 'txtgserv'") >= 0) {
                return STATUS_OK;
            }
            LogUtil.userErrorMessage("Error connecting to server " + getServer() + ":\n" + ae.getMessage());
            logger.info("Error connecting to server");
            setState(STATE_UNCONNECTED);
            setHaveData(false);
            resetDescriptorBox();
            return STATUS_ERROR;
        } catch (ConnectException exc) {
            setState(STATE_UNCONNECTED);
            setHaveData(false);
            resetDescriptorBox();
            String message = "Error connecting to server " + getServer();
            String info = "Error connecting to server";
            if (isLocalServer()) {
            	if (!servManager.checkLocalServer()) {
                    message += "\n\nLocal servers can be restarted from the 'Tools' menu:\n  Tools > Manage ADDE Datasets >\n Local Servers > Start Local Servers";            		
                    info += " (Local servers are stopped)";
                }
            	else {
                    message += "\n\nLocal servers appear to be running.\nYour firewall may be preventing access.";
                    info += " (Local servers are running)";
            	}
            }
            LogUtil.userErrorMessage(message);
            logger.info(info);
            return STATUS_ERROR;
        } catch (EOFException exc) {
            setState(STATE_UNCONNECTED);
            setHaveData(false);
            resetDescriptorBox();
            LogUtil.userErrorMessage("Server " + getServer() + " is not responding");
            logger.info("Server is not responding");
            return STATUS_ERROR;
        } catch (Exception exc) {
            setState(STATE_UNCONNECTED);
            setHaveData(false);
            resetDescriptorBox();
            logException("Connecting to server: " + getServer(), exc);
            logger.info("Error connecting to server");
            return STATUS_ERROR;
        }
    }

    public boolean canAccessServer() {
    	return (checkIfServerIsOk() == STATUS_OK);
    }

    public Map<String, String> getAccountingInfo() {
        AddeServer server = getAddeServer();
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (server != null) {
            List<AddeServer.Group> groups = server.getGroups();
            Map<String, String>acctInfo = getAccounting(server, groups.get(0).toString());
            map.put("user", acctInfo.get("user"));
            map.put("proj", acctInfo.get("proj"));
            map.put("server", server.getName());
            map.put("group", getGroup());
        } else {
            map.put("user", RemoteAddeEntry.DEFAULT_ACCOUNT.getUsername());
            map.put("proj", RemoteAddeEntry.DEFAULT_ACCOUNT.getUsername());
            map.put("server", "");
            map.put("group", "");
        }
        return map;
    }

    /**
     * Saves the currently selected server and group to a chooser-specific 
     * preference. Preference ID is {@code PREF_SERVERSTATE+'.'+getId()}.
     */
    @Override public void saveServerState() {
        String[] serverState = { getServer(), getGroup() };
        getIdv().getStore().put(PREF_SERVERSTATE+'.'+getId(), serverState);
        getIdv().getStore().save();
    }

    /**
     * Connect to the server.
     */
    protected void connectToServer() {
        clearParameterSet();
        setDescriptors(null);
        setDoAbsoluteTimes(false);
        if (!canAccessServer()) {
            return;
        }
        readFromServer();
        saveServerState();
        ignoreStateChangedEvents = true;
        if (descList != null) {
            descList.saveState(groupSelector);
        }
        ignoreStateChangedEvents = false;
    }

    /**
     * Do server connection stuff... override this with type-specific methods
     */
    protected void readFromServer() {
        readDescriptors();
        readTimes();
    }

//    what the request needs to look like:
//    adde://localhost:8112/imagedata?&PORT=112&COMPRES S=gzip&USER=idv&PROJ=0
//        &VERSION=1&DEBUG=false&TRAC E=0&GROUP=MYDATA&DESCRIPTOR=ENTRY4&BAND=1
//        &LATLON= 30.37139 71.74912&PLACE=CENTER&SIZE=1000 1000&UNI T=BRIT
//        &MAG=1 1&SPAC=1&NAV=X&AUX=YES&DOC=X&POS=0

    /**
     *  Generate a list of image descriptors for the descriptor list.
     */
    protected void readDescriptors() {
        try {
            StringBuffer buff = getGroupUrl(REQ_DATASETINFO, getGroup());
            buff.append("&type=").append(getDataType());
            logger.debug("readDesc: buff={}", buff.toString());
            DataSetInfo  dsinfo = new DataSetInfo(buff.toString());
            
            descriptorTable = dsinfo.getDescriptionTable();
            descriptorList.clear();
            commentList.clear();
            descriptorList.addAll(dsinfo.getDescriptorList());
            commentList.addAll(dsinfo.getCommentList());
            int count = commentList.size();
            String[] names = new String[count];
            for (int i = 0; i < count; i++) {
                if (!isLocalServer()) {
                    names[i] = descriptorList.get(i) + nameSeparator + commentList.get(i);
                } else {
                    names[i] = commentList.get(i);
                }
            }
            logger.debug("readDesc: names={}", names);
            Arrays.sort(names);
            setDescriptors(names);
            setState(STATE_CONNECTED);
        } catch (Exception e) {
            handleConnectionError(e);
        }
    }

    /**
     * Initialize the descriptor list from a list of names
     *
     * @param names  list of names
     */
    protected void setDescriptors(String[] names) {
        synchronized (WIDGET_MUTEX) {
            ignoreDescriptorChange = true;
            descriptorComboBox.removeAllItems();
            descriptorNames = names;
            if ((names == null) || (names.length == 0)) {
                return;
            }
            descriptorComboBox.addItem(LABEL_SELECT);
            for (int j = 0; j < names.length; j++) {
                logger.trace("adding names[{}]='{}' to combo box", j, names[j]);
                descriptorComboBox.addItem(names[j]);
            }
            ignoreDescriptorChange = false;
        }
    }

    /**
     * Respond to a change in the descriptor list.
     */
    protected void descriptorChanged() {
        readTimes();
        updateStatus();
    }

    /**
     * Check if a descriptor (image type) has been chosen
     *
     * @return  true if an image type has been chosen
     */
    protected boolean haveDescriptorSelected() {
        if ( !GuiUtils.anySelected(descriptorComboBox)) {
            return false;
        }
        return (getDescriptor() != null);
    }

    /**
     * Get the selected descriptor.
     *
     * @return  the currently selected descriptor.
     */
    protected String getDescriptor() {
        return getDescriptorFromSelection(getSelectedDescriptor());
    }
    
    /**
     * Get the descriptor relating to the selection.
     *
     * @param selection String name from the widget. Can be {@code null}.
     *
     * @return Either the descriptor associated with {@code selection} or {@code null} if {@link #descriptorTable} or 
     * {@code selection} is {@code null}.
     */
    protected String getDescriptorFromSelection(String selection) {
        if (descriptorTable == null) {
            return null;
        }
        if (selection == null) {
            return null;
        }
        
        String descriptor = null;
        if (!selection.contains(nameSeparator)) {
            descriptor = (String)descriptorTable.get(selection);
        } else {
            String[] toks = selection.split(nameSeparator, 2);
            String firstToken = toks[0].trim();
            if (descriptorList.contains(firstToken)) {
                descriptor = firstToken;
            } else {
                String key = toks[1].trim();
                descriptor = (String)descriptorTable.get(key);
            }
        }
        return descriptor;
    }
    
    /**
     * Get the selected descriptor.
     *
     * @return the selected descriptor
     */
    public String getSelectedDescriptor() {
        String selection = (String) descriptorComboBox.getSelectedItem();
        if (selection == null) {
            return null;
        }
        if (selection.equals(LABEL_SELECT)) {
            return null;
        }
        return selection;
    }

    /**
     * Get the descriptor table for this chooser
     *
     * @return a Hashtable of descriptors and names
     */
    public Hashtable getDescriptorTable() {
        return descriptorTable;
    }

    /**
     * Get any extra key=value pairs that are appended to all requests.
     *
     * @param buff The buffer to append onto
     */
    protected void appendMiscKeyValues(StringBuffer buff) {
        appendKeyValue(buff, PROP_COMPRESS, DEFAULT_COMPRESS);
        appendKeyValue(buff, PROP_PORT, DEFAULT_PORT);
        // appendKeyValue(buff, PROP_DEBUG, DEFAULT_DEBUG);
        appendKeyValue(buff, PROP_DEBUG, Boolean.toString(EntryStore.isAddeDebugEnabled(false)));
        appendKeyValue(buff, PROP_VERSION, DEFAULT_VERSION);
        appendKeyValue(buff, PROP_USER, getLastAddedUser());
        appendKeyValue(buff, PROP_PROJ, getLastAddedProj());
    }

    public String getLastAddedUser() {
        if (lastServerUser != null && lastServerUser.length() > 0) {
            logger.debug("getLastAddedUser: using non-default {}", lastServerUser);
            return lastServerUser;
        }
        else {
            logger.debug("getLastAddedUser: using default {}", DEFAULT_USER);
            return DEFAULT_USER;
        }
    }

    public String getLastAddedProj() {
       if (lastServerProj != null && lastServerProj.length() > 0) {
           logger.debug("getLastAddedProj: using non-default {}", lastServerProj);
            return lastServerProj;
        }
        else {
            logger.debug("getLastAddedProj: using default {}", DEFAULT_PROJ);
            return DEFAULT_PROJ;
        }
    }

    /**
     * Show the groups dialog.  This method is not meant to be called
     * but is public by reason of implementation (or insanity).
     */
    public void showGroups() {
        JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem("Reading public datasets..."));
        popup.show(publicButton, 0, (int) publicButton.getBounds().getHeight());

        List groups = readGroups();
        popup.removeAll();
        if ((groups == null) || (groups.size() == 0)) {
            popup.add(new JMenuItem("No public datasets available"));
            popup.setVisible(false);
            popup.setVisible(true);
            return;
        }

        JMenuItem mi;
        for (int i = 0; i < groups.size(); i++) {
            final String group = groups.get(i).toString();
            mi = new JMenuItem(group);
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    groupSelector.setSelectedItem(group);
                    doConnect();
                }
            });
            popup.add(mi);
        }
        popup.setVisible(false);
        popup.setVisible(true);
    }

    /**
     * return the String id of the chosen server name
     *
     * @return  the server name
     */
    public String getServer() {
        AddeServer server = getAddeServer();
        if (server!=null)
            return server.getName();
        else
            return "";
    }

    protected String getGroup() {
        return getGroup(false);
    }

    /**
     * Is the group selector editable?
     *
     * @return Always returns {@code true}.
     */
    protected boolean isGroupEditable() {
        return true;
    }

    /**
     * Get the image group from the GUI.
     *
     * @return The image group.
     */
    protected String getGroup(final boolean fromGetServer) {
        Object selected = groupSelector.getSelectedItem();
        if (selected == null) {
            return null;
        }

        if (selected instanceof AddeServer.Group) {
            AddeServer.Group group = (AddeServer.Group) selected;
        return group.getName();
        }

        if (selected instanceof String) {
            return (String)selected;
        }

        String groupName = selected.toString().trim();
        if (!fromGetServer && (groupName.length() > 0)) {
            //Force the get in case they typed a server name
            getServer();

            AddeServer server = getAddeServer();
            if (server != null) {
                AddeServer.Group group =
                    getIdv().getIdvChooserManager().addAddeServerGroup(
                        server, groupName, getGroupType());
                if (!group.getActive()) {
                    getIdv().getIdvChooserManager().activateAddeServerGroup(
                        server, group);
                }
                //Now put the list of groups back in to the selector
                setGroups();
                groupSelector.setSelectedItem(group);
            }
        }
        return groupName;
    }

    /**
     * Get the server selector
     * @return The server selector
     */
    public JComboBox getServerSelector() {
        if (serverSelector == null)
            serverSelector = super.getServerSelector();

        ItemListener[] ell = serverSelector.getItemListeners();
        for (int i=0; i<ell.length; i++) {
            serverSelector.removeItemListener((ItemListener)ell[i]);
        }
        updateServers();
        updateGroups();
        serverSelector.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( !ignoreStateChangedEvents) {
                    Object selected = serverSelector.getSelectedItem();
                    if (selected instanceof AddeServer) {
                        AddeServer selectedServer = (AddeServer)selected;
                        if (selectedServer != null) {
                            if (isSeparator(selectedServer)) {
                                connectButton.setEnabled(false);
                                return;
                            }
                        }
                    }
                    setState(STATE_UNCONNECTED);
                    connectButton.setEnabled(true);
//                    setGroups();
                    resetDescriptorBox();
                    updateGroups();
//                    System.err.println("itemStateChanged");
                }
//                else {
//                  System.out.println("Ignoring state change here...");
//                }
            }
        });

        serverSelector.getEditor().getEditorComponent().addKeyListener(new KeyListener() {
            public void keyTyped(final KeyEvent e) {}
            public void keyPressed(final KeyEvent e) {}
            public void keyReleased(final KeyEvent e) {
                JTextField field = (JTextField)serverSelector.getEditor().getEditorComponent();
                boolean partialMatch = false;
                for (int i = 0; i < serverSelector.getItemCount(); i++) {
                    String entry = serverSelector.getItemAt(i).toString();
                    if (entry.toLowerCase().startsWith(field.getText().toLowerCase()))
                        partialMatch = true;
                }

                if (!partialMatch && groupSelector != null) {
                    logger.debug("aha! chooser=", getDataType());
                    ((JTextField)groupSelector.getEditor().getEditorComponent()).setText("");
                }
            }
        });

        return serverSelector;
    }

    /**
     * Enable or disable the GUI widgets based on what has been
     * selected.
     */
    protected void enableWidgets() {
        synchronized (WIDGET_MUTEX) {
            boolean newEnabledState = (getState() == STATE_CONNECTED);
            for (int i = 0; i < compsThatNeedDescriptor.size(); i++) {
                JComponent comp = (JComponent) compsThatNeedDescriptor.get(i);
                if (comp.isEnabled() != newEnabledState) {
                    GuiUtils.enableTree(comp, newEnabledState);
                }
            }
        }
    }
    
    /**
     * Add a listener to the given combobox that will set the
     * state to unconnected
     *
     * @param box The box to listen to.
     */
    protected void clearOnChange(final JComboBox box) {
        box.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( !ignoreStateChangedEvents) {
                    setState(STATE_UNCONNECTED);
                    GuiUtils.setListData(descriptorComboBox, new Vector());
//                    System.err.println("clearOnChange");
                }
//                else {
//                  System.out.println("Ignoring state change in clearOnChange for: " + box.toString());
//                }
            }
        });
    }

    /**
     * Get the descriptor widget label
     *
     * @return  label for the descriptor  widget
     */
    public String getDescriptorLabel() {
        return "Descriptor";
    }

    protected int getNumTimesToSelect() {
        return 5;
    }

    /**
     * Get the default selected index for the relative times list.
     *
     * @return default index
     */
    protected int getDefaultRelativeTimeIndex() {
        return 4;
    }

    /**
     * Check the times lists
     */
    protected void checkTimesLists() {
        super.checkTimesLists();
        if (timesCardPanelExtra == null) {
            return;
        }
        if (getDoAbsoluteTimes()) {
            timesCardPanelExtra.show("absolute");
        } else {
            timesCardPanelExtra.show("relative");
        }
    }

    /** Card panel to hold extra relative and absolute time components */
    private GuiUtils.CardLayoutPanel timesCardPanelExtra;

    /**
     * Set the relative and absolute extra components.
     */
    protected JPanel makeTimesPanel(JComponent relativeCard, JComponent absoluteCard) {
        JPanel timesPanel = super.makeTimesPanel(false,true);

        // Make a new timesPanel that has extra components tacked on the bottom, inside the tabs
        Component[] comps = timesPanel.getComponents();

        if (comps.length==1 && comps[0] instanceof JTabbedPane) {
            timesCardPanelExtra = new GuiUtils.CardLayoutPanel();
            if (relativeCard == null) relativeCard = new JPanel();
            if (absoluteCard == null) absoluteCard = new JPanel();
            timesCardPanelExtra.add(relativeCard, "relative");
            timesCardPanelExtra.add(absoluteCard, "absolute");
            timesPanel = GuiUtils.centerBottom(comps[0], timesCardPanelExtra);
        }

        return timesPanel;
    }

    private JPanel innerPanel = new JPanel();

    private JLabel statusLabel = new JLabel("Status");

    /**
     * Super setStatus() takes a second string to enable "simple" mode
     * which highlights the required component.  We don't really care
     * about that feature, and we don't want getStatusLabel() to
     * change the label background color.
     */
    @Override
    public void setStatus(String statusString, String foo) {
        if (statusString == null)
            statusString = "";
        statusLabel.setText(statusString);
    }

    protected void setInnerPanel(JPanel newInnerPanel) {
        innerPanel = newInnerPanel;
    }

    /**
     * Make the UI for this selector.
     *
     * Thank you NetBeans for helping with the layout!
     *
     * @return The GUI.
     */
    protected JComponent doMakeContents() {
        JPanel outerPanel = new JPanel();

        JLabel serverLabelInner = new JLabel("Server:");    
        McVGuiUtils.setLabelPosition(serverLabelInner, Position.RIGHT);
        JPanel serverLabel = GuiUtils.leftRight(parameterButton, serverLabelInner);
        McVGuiUtils.setComponentWidth(serverLabel);

        clearOnChange(serverSelector);
        McVGuiUtils.setComponentWidth(serverSelector, Width.DOUBLE);

        JLabel groupLabel = McVGuiUtils.makeLabelRight("Dataset:");

        groupSelector.setEditable(isGroupEditable());
        clearOnChange(groupSelector);
        McVGuiUtils.setComponentWidth(groupSelector, Width.DOUBLE);

        McVGuiUtils.setComponentWidth(connectButton, Width.DOUBLE);
        connectButton.setActionCommand(CMD_CONNECT);
        connectButton.addActionListener(this);

        /** Set the attributes for the descriptor label and combo box, even though
         * they are not used here.  Extending classes can add them to the panel if
         * necessary.
         */
        McVGuiUtils.setComponentWidth(descriptorLabel);
        McVGuiUtils.setLabelPosition(descriptorLabel, Position.RIGHT);

        McVGuiUtils.setComponentWidth(descriptorComboBox, Width.DOUBLEDOUBLE);

        if (descriptorComboBox.getMinimumSize().getWidth() < ELEMENT_DOUBLE_WIDTH) {
            McVGuiUtils.setComponentWidth(descriptorComboBox, Width.DOUBLE);
        }

        JLabel statusLabelLabel = McVGuiUtils.makeLabelRight("");

        statusLabel.setText("Status");
        McVGuiUtils.setLabelPosition(statusLabel, Position.RIGHT);
        McVGuiUtils.setComponentColor(statusLabel, TextColor.STATUS);

        JButton helpButton = McVGuiUtils.makeImageButton(ICON_HELP, "Show help");
        helpButton.setActionCommand(GuiUtils.CMD_HELP);
        helpButton.addActionListener(this);

        JButton refreshButton = McVGuiUtils.makeImageButton(ICON_REFRESH, "Refresh");
        refreshButton.setActionCommand(GuiUtils.CMD_UPDATE);
        refreshButton.addActionListener(this);

        McVGuiUtils.setComponentWidth(loadButton, Width.DOUBLE);

        GroupLayout layout = new GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(helpButton)
                        .addGap(GAP_RELATED)
                        .addComponent(refreshButton)
                        .addGap(GAP_RELATED)
                        .addComponent(cancelButton)
                        .addPreferredGap(RELATED)
                        .addComponent(loadButton))
                        .addGroup(LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(LEADING)
                            .addComponent(innerPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(serverLabel)
                                .addGap(GAP_RELATED)
                                .addComponent(serverSelector)
                                .addGap(GAP_RELATED)
                                .addComponent(manageButton)
                                .addGap(GAP_RELATED)
                                .addComponent(groupLabel)
                                .addGap(GAP_RELATED)
                                .addComponent(groupSelector)
                                .addGap(GAP_RELATED)
                                .addComponent(publicButton)
                                .addPreferredGap(RELATED, DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(connectButton))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(statusLabelLabel)
                                .addGap(GAP_RELATED)
                                .addComponent(statusLabel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(serverLabel)
                    .addComponent(serverSelector)
                    .addComponent(manageButton)
                    .addComponent(groupLabel)
                    .addComponent(groupSelector)
                    .addComponent(publicButton)
                    .addComponent(connectButton))
                .addPreferredGap(UNRELATED)
                .addComponent(innerPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(UNRELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(statusLabelLabel)
                    .addComponent(statusLabel))
                .addPreferredGap(UNRELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(loadButton)
                    .addComponent(cancelButton)
                    .addComponent(refreshButton)
                    .addComponent(helpButton))
                .addContainerGap())
        );
    
        return outerPanel;

    }

    public class ServerComparator implements Comparator<AddeServer> {
        public int compare(AddeServer server1, AddeServer server2) {
            return server1.getName().compareTo(server2.getName());
        }
    }

    public class GroupComparator implements Comparator<Group> {
        public int compare(Group group1, Group group2) {
            return group1.getName().compareTo(group2.getName());
        }
    }
}

