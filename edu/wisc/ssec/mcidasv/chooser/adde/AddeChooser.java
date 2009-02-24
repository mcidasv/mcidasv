/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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

import java.awt.Component;
import java.awt.Dimension;
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
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.idv.chooser.adde.AddeServer.Group;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlObjectStore;
import edu.wisc.ssec.mcidas.adde.AddeURLException;
import edu.wisc.ssec.mcidas.adde.DataSetInfo;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.McIdasPreferenceManager;
import edu.wisc.ssec.mcidasv.ServerPreferenceManager;
import edu.wisc.ssec.mcidasv.ServerPreferenceManager.DatasetDescriptor;
import edu.wisc.ssec.mcidasv.ServerPreferenceManager.ServerPropertyDialog;
import edu.wisc.ssec.mcidasv.ServerPreferenceManager.ServerPropertyDialog.Types;
import edu.wisc.ssec.mcidasv.util.CollectionHelpers;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.TextColor;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

/**
 *
 * @author Unidata IDV Development Team
 * @version $Revision$
 */
public class AddeChooser extends ucar.unidata.idv.chooser.adde.AddeChooser implements Constants {

	private JComboBox serverSelector;
	
    /** List of descriptors */
    private PreferenceList descList;
    
    /** Descriptor/name hashtable */
    protected Hashtable descriptorTable;
    
    /** Property for the descriptor table */
    public static final String DESCRIPTOR_TABLE = "DESCRIPTOR_TABLE";
	
    /** Connect button--we need to be able to disable this */
    JButton connectButton = McVGuiUtils.makeImageTextButton(ICON_CONNECT_SMALL, "Connect");
    
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
    protected ServerPreferenceManager serverManager;

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
        
    /**
     * Create an AddeChooser associated with an IdvChooser
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeChooser(IdvChooserManager mgr, Element root) {

        super(mgr, root);
        simpleMode = !getProperty(IdvChooser.ATTR_SHOWDETAILS, true);

        loadButton = McVGuiUtils.makeImageTextButton(ICON_ACCEPT_SMALL, getLoadCommandName());
        loadButton.setActionCommand(getLoadCommandName());
        loadButton.addActionListener(this);
        
        cancelButton = McVGuiUtils.makeImageButton(ICON_CANCEL, "Cancel");
        cancelButton.setActionCommand(GuiUtils.CMD_CANCEL);
        cancelButton.addActionListener(this);
        cancelButton.setEnabled(false);
        
        serverSelector = getServerSelector();
        
        serverSelector.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
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
		        connectButton.setEnabled(true);
                if ( !ignoreStateChangedEvents) {
                    setGroups();
                }
            }
        });
        
        serverSelector.setToolTipText("Right click to manage servers");
        serverSelector.getEditor().getEditorComponent().addMouseListener(
        		new MouseAdapter() {
        			public void mouseReleased(MouseEvent e) {
        				if ( !SwingUtilities.isRightMouseButton(e)) {
        					return;
        				}
        				AddeServer server = getAddeServer();
        				if (server == null) {
        					return;
        				}
        				List items = new ArrayList();
        				
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
        				if ( !SwingUtilities.isRightMouseButton(e)) {
        					return;
        				}
        				AddeServer server = getAddeServer();
        				if (server == null) {
        					return;
        				}
        				List items = new ArrayList();
        				
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
        				popup.show(groupSelector, e.getX(), e.getY());        		        
        			}
        		});
        groupSelector.setMaximumRowCount(16);
        
        serverManager = ((McIDASV)getIdv()).getServerManager();
        serverManager.addManagedChooser(this);
        
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
                
        updateServerList();
    }

    public void updateServers() {
        if (serverManager == null)
            serverManager = ((McIDASV)getIdv()).getServerManager();
        String type = getGroupType();
        List<AddeServer> managedServers = serverManager.getAddeServers(type);

        List<AddeServer> localList = CollectionHelpers.arrList();
        List<AddeServer> remoteList = CollectionHelpers.arrList();
        List<AddeServer> servers = CollectionHelpers.arrList();
        for (AddeServer server : managedServers) {
            if (server.getIsLocal())
                localList.add(server);
            else
                remoteList.add(server);
        }

        // server list doesn't need a separator if there's only remote servers
        if (!localList.isEmpty()) {
            servers.addAll(localList);
            servers.add(new AddeServer(separator));
        }
        Comparator<AddeServer> byServer = new ServerComparator();
        Collections.sort(remoteList, byServer);
        servers.addAll(remoteList);

        // always making this call helps to ensure the chooser stays up to date
        // with the server manager.
        GuiUtils.setListData(serverSelector, servers);
        if (!servers.isEmpty()) {
            serverSelector.setSelectedIndex(0);
        }
    }

    /**
     * Reload the list of servers if they have changed
     */
    public void updateServerList() {
        updateServers();
        updateGroups();
    }

    public void updateGroups() {
        if (groupSelector == null || getAddeServer() == null)
            return;

        List<Group> groups = CollectionHelpers.arrList();
        if (isLocalServer())
        	groups.addAll(((McIDASV)getIdv()).getAddeManager().getGroups());
        else
        	groups.addAll(((McIDASV)getIdv()).getServerManager().getGroups(getAddeServer(), getGroupType()));
        Comparator<Group> byGroup = new GroupComparator();
        Collections.sort(groups, byGroup);
        GuiUtils.setListData(groupSelector, groups);
    }
    
    /**
     * Sort the servers alphabetically
     */
    
    /**
     * Sort the groups alphabetically
     */

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
            String serverName = checkServer.getName();
            if (serverName.length() >= 9 && serverName.startsWith("localhost")) {
                return true;
            }
        }
        return false;
    }
    
    private void setBadServer(String name, String group) {
        if (name == null)
            name = "";
        if (group == null)
            group = "";

        lastBadServer = name;
        lastBadGroup = group;
    }

    private boolean isBadServer(String name, String group) {
        assert lastBadServer != null;
        assert lastBadGroup != null;
        return lastBadServer.equals(name) && lastBadGroup.equals(group);
    }

    private void setLastServer(String name, String group, AddeServer server) {
        if (name == null)
            name = "";
        if (group == null)
            group = "";
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
    
    protected AddeServer getAddeServer(String foo) {
    	return getAddeServer();
    }
    
    /**
     * Get the selected AddeServer
     *
     * @return the server or null
     */
    protected AddeServer getAddeServer() {
        if (lastServerName != null && lastServerName.equals("unset")) {
//            System.err.println("* getAddeServer: returning null because we're still waiting on the dialog");
            return null;
        }

        Object selected = serverSelector.getSelectedItem();
        if ((selected != null) && (selected instanceof AddeServer)) {
            AddeServer server = (AddeServer)selected;

            Map<String, String> accounting = serverManager.getAccounting(server);
            lastServerUser = accounting.get("user");
            lastServerProj = accounting.get("proj");
            setLastServer(server.getName(), getGroup(true), server);
            
//            System.err.println("* getAddeServer: returning AddeServer=" + server.getName() + " group=" + server.getGroups()+" user="+lastServerUser+" proj="+lastServerProj + " ugh: " + accounting.get("user") + " " + accounting.get("proj"));
            return (AddeServer) selected;
        } else if ((selected != null) && (selected instanceof String)) {
            String name = (String)selected;
            String group = getGroup(true);
            if (isBadServer(name, group)) {
//                System.err.println("* getAddeServer: returning null due to text entries being known bad values: name=" + name + " group=" + group);
                return null;
            }
            if (isLastServer(name, group)) {
//                System.err.println("* getAddeServer: returning last server: name=" + lastServer.getName() + " group=" + lastServer.getGroups());
                return lastServer;
            }
            lastServerName = "unset";
            lastServerGroup = "unset";
            ServerPreferenceManager serverManager = ((McIdasPreferenceManager)getIdv().getPreferenceManager()).getServerManager();
            ServerPropertyDialog dialog = new ServerPropertyDialog(null, true, serverManager);
            Set<Types> defaultTypes = EnumSet.of(ServerPropertyDialog.convertDataType(getDataType()));
            dialog.setTitle("Add New Server");
            dialog.showDialog(name, group, defaultTypes);
            boolean hitApply = dialog.hitApply(true);
            if (!hitApply) {
//                System.err.println("* getAddeServer: returning null due to cancel request from showDialog");
                setBadServer(name, group);
                return null;
            }

            Set<DatasetDescriptor> added = dialog.getAddedDatasetDescriptors();
            if (added == null) {
//                System.err.println("* getAddeServer: null list of added servers somehow!");
                setBadServer(name, getGroup(true));
                return null;
            }
            for (DatasetDescriptor descriptor : added) {
                updateServerList();
                AddeServer addedServer = descriptor.getServer();
                serverSelector.setSelectedItem(addedServer);
//                System.err.println("* getAddeServer: returning newly added AddeServer=" + addedServer.getName() + " group=" + addedServer.getGroups());
                setLastServer(name, group, addedServer);
                lastServerUser = descriptor.getUser();
                lastServerProj = descriptor.getProj();
                return addedServer;
            }
        } else if (selected == null) {
//            System.err.println("* getAddeServer: returning null due to null object in selector");
        } else {
//            System.err.println("* getAddeServer: returning null due to unknown object type in selector: " + selected.toString());
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
    
    protected void handleConnectionError(Exception e) {
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
    	if (isLocalServer()) {
    		((McIDASV)getIdv()).showAddeManager();
    		return;
    	}
    	getIdv().getPreferenceManager().showTab(Constants.PREF_LIST_ADDE_SERVERS);
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
            if (aes.indexOf("Invalid project number") >= 0) {
                LogUtil.userErrorMessage("Invalid project number");
                return STATUS_NEEDSLOGIN;
            }
            if (aes.indexOf("Invalid user id") >= 0) {
                LogUtil.userErrorMessage("Invalid user ID");
                return STATUS_NEEDSLOGIN;
            }
            if (aes.indexOf("Accounting data") >= 0) {
                return STATUS_NEEDSLOGIN;
            }
            if (aes.indexOf("cannot run server 'txtgserv'") >= 0) {
                return STATUS_OK;
            }
            LogUtil.userErrorMessage("Error connecting to server " + getServer() + ":\n"
                                     + ae.getMessage());
            return STATUS_ERROR;
        } catch (ConnectException exc) {
            setState(STATE_UNCONNECTED);
    		setHaveData(false);
    		resetDescriptorBox();
    		String message = "Error connecting to server " + getServer();
    		if (isLocalServer())
    			message += "\n\nLocal servers can be restarted from the\n'Local ADDE Data Manager' in the 'Tools' menu";
            LogUtil.userErrorMessage(message);
    		return STATUS_ERROR;
        } catch (EOFException exc) {
            setState(STATE_UNCONNECTED);
    		setHaveData(false);
    		resetDescriptorBox();
            LogUtil.userErrorMessage("Server " + getServer() + " is not responding");
    		return STATUS_ERROR;
        } catch (Exception exc) {
            logException("Connecting to server: " + getServer(), exc);
            return STATUS_ERROR;
        }
    }

//    /**
//     * This method checks if the current server is valid. If it is valid
//     * then it checks if there is authentication required
//     *
//     * @return true if the server exists and can be accessed
//     */
//    protected boolean canAccessServer() {
//        //Try reading the public.serv file to see if we need a username/proj
//        JTextField projFld   = null;
//        JTextField userFld   = null;
//        JComponent contents  = null;
//        JLabel     label     = null;
//        boolean    firstTime = true;
//        while (true) {
//            int status = checkIfServerIsOk();
//            if (status == STATUS_OK) {
//                break;
//            }
//            if (status == STATUS_ERROR) {
//                setState(STATE_UNCONNECTED);
//                return false;
//            }
//            
//            AddeServer server = getAddeServer();
//
//            if (projFld == null) {
//                projFld = new JTextField("", 10);
//                userFld = new JTextField("", 10);
//                GuiUtils.tmpInsets = GuiUtils.INSETS_5;
//
//                contents = GuiUtils.doLayout(new Component[] {
//                  GuiUtils.rLabel("User ID:"), userFld, 
//                  GuiUtils.rLabel("Project #:"), projFld, }, 2, GuiUtils.WT_N, 
//                  GuiUtils.WT_N);
//
//                label  = new JLabel(" ");
//                contents = GuiUtils.topCenter(label, contents);
//                contents = GuiUtils.inset(contents, 5);
//            }
//
//            String lbl = (firstTime
//                ? "The server: " + server.getName()
//                    + " requires a user ID & project number for access"
//                    : "Authentication for server: " + server.getName()
//                    + " failed. Please try again");
//            label.setText(lbl);
//
//            if ( !GuiUtils.showOkCancelDialog(null, "ADDE Project/User name",
//                contents, null)) {
//                setState(STATE_UNCONNECTED);
//                System.err.println("canAccessServer: cancel dialog?");
//                return false;
//            }
//            firstTime = false;
//            String userName = userFld.getText().trim();
//            String project = projFld.getText().trim();
//            if ((userName.length() > 0) && (project.length() > 0)) {
//                passwords.put(server.getName(),
//                    new String[] { userName, project });
//            }
////            }
//        }
////        System.err.println("canAccessServer: returning true");
//        return true;
//    }

    public boolean canAccessServer() {
        Set<Types> defaultTypes = EnumSet.of(ServerPropertyDialog.convertDataType(getDataType()));
        while (true) {
            int status = checkIfServerIsOk();
            if (status == STATUS_OK) {
                break;
            }
            if (status == STATUS_ERROR) {
                setState(STATE_UNCONNECTED);
                return false;
            }

            AddeServer server = getAddeServer();
            Map<String, String> accounting = serverManager.getAccounting(server);

            String name = server.getName();
            String group = getGroup();
            String user = accounting.get("user");
            String proj = accounting.get("proj");

            ServerPropertyDialog dialog = new ServerPropertyDialog(null, true, serverManager);
            dialog.setTitle("Edit Server Information");
            dialog.showDialog(name, group, user, proj, defaultTypes);

            if (!dialog.getAddedDatasetDescriptors().isEmpty()) {
                System.err.println("verified info: " + dialog.getAddedDatasetDescriptors());
                break;
            }
        }
        return true;
    }

    public Map<String, String> getAccountingInfo() {
        AddeServer server = getAddeServer();
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (server != null) {
            Map<String, String> accounting = serverManager.getAccounting(server);
            map.putAll(accounting);
            map.put("server", server.getName());
            map.put("group", getGroup());
        } else {
            map.put("user", ServerPreferenceManager.getDefaultUser());
            map.put("proj", ServerPreferenceManager.getDefaultProject());
            map.put("server", "");
            map.put("group", "");
        }
        return map;
    }

    /**
     * Connect to the server.
     */
    protected void connectToServer() {
        setDescriptors(null);
        setDoAbsoluteTimes(false);
        if ( !canAccessServer()) {
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
    
    /**
     *  Generate a list of image descriptors for the descriptor list.
     */
    protected void readDescriptors() {
        try {
            StringBuffer buff   = getGroupUrl(REQ_DATASETINFO, getGroup());
            buff.append("&type=" + getDataType());
            DataSetInfo  dsinfo = new DataSetInfo(buff.toString());
            descriptorTable = dsinfo.getDescriptionTable();
            String[]    names       = new String[descriptorTable.size()];
            Enumeration enumeration = descriptorTable.keys();
            for (int i = 0; enumeration.hasMoreElements(); i++) {
            	Object thisElement = enumeration.nextElement();
            	if (!isLocalServer())
            		names[i] = descriptorTable.get(thisElement).toString() + nameSeparator + thisElement.toString();
            	else
            		names[i] = thisElement.toString();
            }
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
     * @param selection   String name from the widget
     *
     * @return  the descriptor
     */
    protected String getDescriptorFromSelection(String selection) {
        if (descriptorTable == null) {
            return null;
        }
        if (selection == null) {
            return null;
        }

        if (!selection.contains(nameSeparator)) {
            return (String)descriptorTable.get(selection);
        }
        else {
	        String[] toks = selection.split(nameSeparator);
	        String key = toks[1].trim();
	        return (String)descriptorTable.get(key);
        }
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
        appendKeyValue(buff, PROP_DEBUG, DEFAULT_DEBUG);
        appendKeyValue(buff, PROP_VERSION, DEFAULT_VERSION);
        appendKeyValue(buff, PROP_USER, getLastAddedUser());
        appendKeyValue(buff, PROP_PROJ, getLastAddedProj());
    }

    public String getLastAddedUser() {
        if (lastServerUser != null && lastServerUser.length() > 0) {
//            System.err.println("appendMisc: using dialog user=" + lastServerUser);
            return lastServerUser;
        }
        else {
//            System.err.println("appendMisc: using default user=" + DEFAULT_USER);
            return DEFAULT_USER;
        }
    }

    public String getLastAddedProj() {
       if (lastServerProj != null && lastServerProj.length() > 0) {
//            System.err.println("appendMisc: using dialog proj=" + lastServerProj);
            return lastServerProj;
        }
        else {
//            System.err.println("appendMisc: using default proj=" + DEFAULT_PROJ);
            return DEFAULT_PROJ;
        }
    }

    /**
     * Show the groups dialog.  This method is not meant to be called
     * but is public by reason of implementation (or insanity).
     */
    public void showGroups() {
        List groups = readGroups();
        if ((groups == null) || (groups.size() == 0)) {
            LogUtil.userMessage("No public datasets found on " + getServer());
            return;
        }
        final JDialog dialog = GuiUtils.createDialog("Server Groups", true);
        final String[] selected = { null };
        List comps = new ArrayList();
        for (int i = 0; i < groups.size(); i++) {
            final String group = groups.get(i).toString();
            JButton btn = new JButton(group);
            comps.add(btn);
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    selected[0] = group;
                    dialog.dispose();
                }
            });
        }

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                dialog.dispose();
            }
        });

        JComponent buttons = GuiUtils.vbox(comps);
        buttons = new JScrollPane(GuiUtils.vbox(comps));
        int xsize = ((JComponent) comps.get(0)).getPreferredSize().width;
        buttons.setPreferredSize(new Dimension(xsize + 50, 150));
        JComponent top =
            GuiUtils.inset(new JLabel("Available data sets on server: "
                                      + getServer()), 5);
        JComponent bottom = GuiUtils.inset(closeBtn, 5);
        JComponent contents = GuiUtils.topCenterBottom(top, buttons,
                                  GuiUtils.wrap(bottom));
        dialog.setLocation(200, 200);
        dialog.getContentPane().add(contents);
        dialog.pack();
        dialog.setVisible(true);
        if (selected[0] != null) {
            groupSelector.setSelectedItem(selected[0]);
            doConnect();
        }
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
     * Is the group selector editable?  Override if ya want.
     * @return
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
        serverSelector.removeItemListener((ItemListener)ell[0]);
        updateServers();
        updateGroups();
        serverSelector.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            	setState(STATE_UNCONNECTED);
            	resetDescriptorBox();
                updateGroups();
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
                }
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
     * Set the relative and absolute extra components
     */
    protected JPanel makeTimesPanel(JComponent relativeCard, JComponent absoluteCard) {
    	JPanel timesPanel = super.makeTimesPanel(false,true);
    	    	
    	// Make a new timesPanel that has extra components tacked on the bottom, inside the tabs
    	Component[] comps = timesPanel.getComponents();
    	if (comps.length==2 && comps[0] instanceof JTabbedPane && comps[1] instanceof JLabel) {    		
            timesCardPanelExtra = new GuiUtils.CardLayoutPanel();
    		if (relativeCard == null) relativeCard = new JPanel();
    		if (absoluteCard == null) absoluteCard = new JPanel();
    		absoluteCard = GuiUtils.hbox(comps[1], GuiUtils.right(absoluteCard));
    		timesCardPanelExtra.add(relativeCard, "relative");
    		timesCardPanelExtra.add(absoluteCard, "absolute");
            timesPanel = GuiUtils.centerBottom(comps[0], timesCardPanelExtra);
    	}
    	
    	return timesPanel;
    }
    
    /**
     * Make the UI for this selector.
     * 
     * Thank you NetBeans for helping with the layout!
     * 
     * @return The gui
     */ 
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
    
    protected JComponent doMakeContents() {
    	JPanel outerPanel = new JPanel();
    	
        JLabel serverLabel = McVGuiUtils.makeLabelRight("Server:");    	    	

        clearOnChange(serverSelector);
        McVGuiUtils.setComponentWidth(serverSelector, Width.DOUBLE);

        JLabel groupLabel = McVGuiUtils.makeLabelRight("Dataset:");

        groupSelector.setEditable(isGroupEditable());
        clearOnChange(groupSelector);
        McVGuiUtils.setComponentWidth(groupSelector, Width.DOUBLE);
        
        JButton manageButton =
        	McVGuiUtils.makeImageButton("/edu/wisc/ssec/mcidasv/resources/icons/toolbar/preferences-system22.png",
        			this, "doManager", null, "Manage servers");
        
        JButton publicButton =
        	McVGuiUtils.makeImageButton("/edu/wisc/ssec/mcidasv/resources/icons/toolbar/show-layer-controls22.png",
        			this, "showGroups", null, "List public datasets available on the server");
        
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

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(helpButton)
                        .add(GAP_RELATED)
                        .add(refreshButton)
                        .add(GAP_RELATED)
                        .add(cancelButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(loadButton))
                        .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(innerPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(layout.createSequentialGroup()
                                .add(serverLabel)
                                .add(GAP_RELATED)
                                .add(serverSelector)
                                .add(GAP_RELATED)
                                .add(manageButton)
                                .add(GAP_RELATED)
                                .add(groupLabel)
                                .add(GAP_RELATED)
                                .add(groupSelector)
                                .add(GAP_RELATED)
                                .add(publicButton)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(connectButton))
                            .add(layout.createSequentialGroup()
                                .add(statusLabelLabel)
                                .add(GAP_RELATED)
                                .add(statusLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
            	.addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(serverLabel)
                    .add(serverSelector)
                    .add(manageButton)
                    .add(groupLabel)
                    .add(groupSelector)
                    .add(publicButton)
                    .add(connectButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(innerPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(statusLabelLabel)
                    .add(statusLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(loadButton)
                    .add(cancelButton)
                    .add(refreshButton)
                    .add(helpButton))
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

