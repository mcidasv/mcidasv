/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv;

import edu.wisc.ssec.mcidas.adde.AddeServerInfo;

import edu.wisc.ssec.mcidasv.chooser.ServerInfo;
import edu.wisc.ssec.mcidasv.chooser.ServerDescriptor;

import java.awt.*;
import java.awt.event.*;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.idv.chooser.adde.AddeServer.Group;

import ucar.unidata.ui.CheckboxCategoryPanel;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;


public class ServerPreferenceManager extends IdvManager implements ActionListener {


    /** Should we show all of the display control descriptors */
    protected boolean showAllServers = true;

    /** A mapping that holds the servers that should be shown */
    protected Hashtable serversToShow = null;

    /** A mapping that holds the choosers that should be shown */
    protected Hashtable typesToShow = null;

    /** mapping between types and servers */
    private Hashtable cbxToServerMap;

    /** add server dialog */
    private JFrame addWindow;

    /** add accounting dialog */
    private JFrame acctWindow;

    /** Shows the status */
    private JLabel statusLabel;

    /** _more_          */
    private JComponent statusComp;

    private PreferenceManager serversManager = null;
    private JPanel serversPanel = null;

    private final JButton deleteServer = new JButton("Delete");
    private ServerInfo si;

    private static String user;
    private static String proj;

    private Hashtable catMap = new Hashtable();

    private String[] allTypes = {"image", "point", "grid", "text", "nav"};

    private List allServers = new ArrayList();
    private List servImage = new ArrayList();
    private List servPoint = new ArrayList();
    private List servGrid = new ArrayList();
    private List servText = new ArrayList();
    private List servNav = new ArrayList();

    /** Install data type cbxs */
    private JCheckBox imageTypeCbx;
    private JCheckBox pointTypeCbx;
    private JCheckBox gridTypeCbx;
    private JCheckBox textTypeCbx;
    private JCheckBox navTypeCbx;

    private String lastCat;
    private JPanel lastPan;
    private JCheckBox lastBox;

    /** Install server and group name flds */
    private JTextField serverFld;
    private JTextField groupFld;

    /** Install user ID and project number flds */
    private JTextField userFld;
    private JTextField projFld;

    /** tags */
    public static final String TAG_TYPE = "type";
    public static final String TAG_SERVER = "server";
    public static final String TAG_SERVERS = "servers";
    public static final String TAG_USERID = "userID";
    public static final String TAG_GROUP = "group";

    /** attributes */
    public static final String ATTR_ACTIVE = "active";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_NAMES = "names";
    public static final String ATTR_GROUP = "group";
    public static final String ATTR_USER = "user";
    public static final String ATTR_PROJ = "proj";
    public static final String ATTR_TYPE = "type";

    private final XmlResourceCollection serversXRC = getServers();

    /** Action command used for the Cancel button */
    private static String CMD_VERIFY = "Verify";
    private static String CMD_APPLY = "Apply";
    private static String CMD_VERIFYAPPLY = "VerifyApply";

    /** Default value for the user property */
    private static String DEFAULT_USER = "idv";

    /** Default value for the proj property */
    private static String DEFAULT_PROJ = "0";

    private static List typePanels = new ArrayList();

    private static boolean alreadChecked = false;
    private static String lastServerChecked = "";

    /**
     * Create the dialog with the given idv
     *
     * @param idv The IDV
     *
     */
    public ServerPreferenceManager(IntegratedDataViewer idv) {
        super(idv);
    }


    public void addServerPreferences(IdvPreferenceManager ipm) {
        getServerPreferences();
        ipm.add("ADDE Servers",
                 "What servers should be shown in choosers?",
                 serversManager, serversPanel, cbxToServerMap);
    }

    protected JComponent getStatusComponent() {
        if (statusComp == null) {
            JLabel statusLabel = getStatusLabel();
            statusComp = GuiUtils.inset(statusLabel, 2);
            statusComp.setBackground(new Color(255, 255, 204));
        }
        return statusComp;
    }

    /**
     * Create (if needed) and return the JLabel that shows the status messages.
     *
     * @return The status label
     */
    protected JLabel getStatusLabel() {
        if (statusLabel == null) {
            statusLabel = new JLabel();
        }
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(255, 255, 204));
        return statusLabel;
    }

    public void setStatus(String msg) {
        getStatusLabel().setText(msg);
        serversPanel.paintImmediately(0,0,serversPanel.getWidth(),
                                        serversPanel.getHeight());
    }


    /**
     * Add in the user preference tab for the servers to show.
     */
    protected void getServerPreferences() {
        cbxToServerMap = new Hashtable();
        List servList = new ArrayList();
        si = new ServerInfo(getIdv(), serversXRC);
        final List catPanels          = new ArrayList();
        List types = si.getServerTypes();
        typePanels = types;
        String typeString;

        deleteServer.setEnabled(false);
        deleteServer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                deleteServers();
                deleteServer.setEnabled(false);
            }
        });

        for (int i=0; i<types.size(); i++) {
            typeString = (String)types.get(i);
            CheckboxCategoryPanel catPanel =
                (CheckboxCategoryPanel) catMap.get(typeString);
            if (catPanel == null) {
                catPanel = new CheckboxCategoryPanel(typeString, false);
                catPanels.add(catPanel);
                catMap.put(typeString, catPanel);
                servList.add(catPanel.getTopPanel());
                servList.add(catPanel);
            }
            List servers = si.getServers(typeString, true, true);
            if (servers.size() > 0) {
                for (int j=0; j<servers.size(); j++) {
                    final ServerDescriptor sd = (ServerDescriptor)servers.get(j);
                    allServers.add(sd);
                    final JCheckBox cbx = new JCheckBox(sd.toString(), sd.getIsActive());
                    final String str = typeString;
                    final int indx = j;
                    final JPanel pan = GuiUtils.inset(cbx, new Insets(0, 20, 0, 0));
                    final CheckboxCategoryPanel catpan = catPanel;
                    cbx.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            lastBox = cbx;
                            lastCat = str;
                            lastPan = pan;
                            sd.setIsActive(!sd.getIsActive());
                            deleteServer.setEnabled(true);
                        }
                    });
                    cbxToServerMap.put(cbx, sd);
                    catPanel.add(pan);
                }
            }
        }

        for (int i = 0; i < catPanels.size(); i++) {
            ((CheckboxCategoryPanel) catPanels.get(i)).checkVisCbx();
        }

        List comps = new ArrayList();

        final JButton accounting = new JButton("Accounting");
        accounting.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                addAccounting();
            }
        });
        comps.add(accounting);
        comps.add(new JLabel(" "));
        final JButton allOn = new JButton("All on");
        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Hashtable table = cbxToServerMap;
                for (Enumeration keys = table.keys(); keys.hasMoreElements(); ) {
                    JCheckBox cbx = (JCheckBox)keys.nextElement();
                    ServerDescriptor sd = (ServerDescriptor)table.get(cbx);
                    sd.setIsActive(true);
                    cbx.setSelected(true);
                }
                for (int i = 0; i < catPanels.size(); i++) {
                    CheckboxCategoryPanel cPanel = (CheckboxCategoryPanel) catPanels.get(i);
                    cPanel.toggleAll(true);
                }
            }
        });
        comps.add(allOn);
        final JButton allOff = new JButton("All off");
        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Hashtable table = cbxToServerMap;
                for (Enumeration keys = table.keys(); keys.hasMoreElements(); ) {
                    JCheckBox cbx = (JCheckBox)keys.nextElement();
                    ServerDescriptor sd = (ServerDescriptor)table.get(cbx);
                    sd.setIsActive(false);
                    cbx.setSelected(false);
                }
                for (int i = 0; i < catPanels.size(); i++) {
                    CheckboxCategoryPanel cPanel = (CheckboxCategoryPanel) catPanels.get(i);
                    cPanel.toggleAll(false);
                }
            }
        });
        comps.add(allOff);
        comps.add(new JLabel(" "));
        final JButton addServer = new JButton("Add");
        addServer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                addServers();
            }
        });
        comps.add(addServer);
        comps.add(deleteServer);
        for (int i=0; i<10; i++) {
            comps.add(new JLabel(" "));
        }
        comps.add(new JLabel("     -- Import --"));
        final JButton fromMcX = new JButton("from McIDAS-X");
        fromMcX.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showWaitCursor();
                getServersFromMctable();
                showNormalCursor();
            }
        });
        final JButton fromSystem = new JButton("System Servers");
        fromSystem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showWaitCursor();
                getSystemServers();
                showNormalCursor();
            }
        });
        comps.add(fromMcX);
        comps.add(fromSystem);

        final JPanel servPanel = GuiUtils.top(GuiUtils.doLayout(new JPanel(), 
             GuiUtils.getComponentArray(servList), 1, GuiUtils.WT_Y, GuiUtils.WT_N));
        JScrollPane  servScroller = new JScrollPane(servPanel);
        servScroller.getVerticalScrollBar().setUnitIncrement(10);
        servScroller.setPreferredSize(new Dimension(300, 300));
/*
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if (cmd.equals("import")) {
                    showWaitCursor();
                    getServersFromMctable();
                    showNormalCursor();
                } else if (cmd.equals("export")) {
                    exportServersToPlugin();
                }
            }
        };
        fromMcX.addActionListener(listener);
        fromSystem.addActionListener(listener);

        String[] labels = {"Import from McIDAS-X", "Import System Servers"};
        String[] cmds = {"import", "export"};
        JComponent exportImportServers =
            GuiUtils.right(GuiUtils.makeButtons(listener, labels, cmds));
        final JPanel exportImportPanel = (JPanel)exportImportServers;
        Component[] cmps = exportImportPanel.getComponents();
  
        JComponent servComp = GuiUtils.centerBottom(servScroller, exportImportServers);
*/
        JComponent servComp = GuiUtils.centerBottom(servScroller, new JLabel(" "));
        JPanel bottomPanel =
            GuiUtils.leftCenter(
                GuiUtils.inset(
                    GuiUtils.top(GuiUtils.vbox(comps)),
                    4), new Msg.SkipPanel(
                        GuiUtils.hgrid(
                            Misc.newList(servComp, new JLabel(" ")), 0)));
  
        serversPanel =
            GuiUtils.inset(GuiUtils.topCenter( GuiUtils.vbox(new JLabel(" "),
                GuiUtils.hbox(GuiUtils.rLabel("Status: "),getStatusComponent()),
                new JLabel(" "), new JLabel(" ")),
                bottomPanel), 6);
        GuiUtils.enableTree(servPanel, true);

        allOn.setEnabled( true);
        allOff.setEnabled( true);

        serversManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
                updateXml();
            }
        };
    }


    /**
     * Add servers
     */
    private void addServers() {
        if (addWindow == null) {
            showAddDialog();
            return;
        }
        addWindow.setVisible(true);
        GuiUtils.toFront(addWindow);
    }

    /**
     * Add accounting  
     */             
    private void addAccounting() {
        if (acctWindow == null) {
            showAcctDialog();
            return;
        }
        acctWindow.setVisible(true);
        GuiUtils.toFront(acctWindow);
    }

    /**
     * Delete server
     */
    private void deleteServers() {
        if (lastCat != null) {
            CheckboxCategoryPanel catPanel =
                (CheckboxCategoryPanel) catMap.get(lastCat);
            cbxToServerMap.remove(lastBox);
            if (catPanel.getComponentCount() == 1)
                catPanel.setVisible(false);
            catPanel.remove(lastPan);
            catPanel.validate();
            GuiUtils.getWindow(catPanel).pack();
        }
    }


    /**
     * showAddDialog
     */
    private void showAddDialog() {
        if (addWindow == null) {
            List comps = new ArrayList();
            comps.add(imageTypeCbx =
                new JCheckBox("Image", false));
            comps.add(pointTypeCbx =
                new JCheckBox("Point", false));
            comps.add(gridTypeCbx =
                new JCheckBox("Grid", false));
            comps.add(textTypeCbx =
                new JCheckBox("Text", false));
            comps.add(navTypeCbx =
                new JCheckBox("Navigation", false));

            JPanel dataTypes = GuiUtils.inset(GuiUtils.hbox(comps, 5),20);

            addWindow = GuiUtils.createFrame("Add Server");

            serverFld = new JTextField("", 30);
            groupFld = new JTextField("", 30);

            List textComps = new ArrayList();
            textComps.add(new JLabel(" "));
            textComps.add(GuiUtils.hbox(new JLabel("Server: "), serverFld));
            textComps.add(new JLabel(" "));
            textComps.add(GuiUtils.hbox(new JLabel("Group(s): "), groupFld));
            textComps.add(new JLabel(" "));
            JComponent nameComp = GuiUtils.center(GuiUtils.inset(
                                     GuiUtils.vbox(textComps),20));

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    String cmd = event.getActionCommand();
                    if (cmd.equals(GuiUtils.CMD_CANCEL)) {
                        addWindow.setVisible(false);
                        addWindow = null;
                    } else {
                        String newServer = serverFld.getText().trim();
                        String grp = groupFld.getText().trim();
                        StringTokenizer tok = new StringTokenizer(grp, ",");
                        List newGroups = new ArrayList();
                        while (tok.hasMoreTokens()) {
                            newGroups.add(tok.nextToken().trim());
                        }
                        List typeList = new ArrayList();
                        if (imageTypeCbx.isSelected()) typeList.add("image");
                        if (pointTypeCbx.isSelected()) typeList.add("point");
                        if (gridTypeCbx.isSelected()) typeList.add("grid");
                        if (textTypeCbx.isSelected()) typeList.add("text");
                        if (navTypeCbx.isSelected()) typeList.add("nav");
                        boolean apply = false;
                        boolean verify = false;
                        if (cmd.equals(CMD_VERIFY)) {
                            verify = true;
                        }
                        else if (cmd.equals(CMD_APPLY)) {
                            apply = true;
                        }
                        else if (cmd.equals(CMD_VERIFYAPPLY)) {
                            apply = true;
                            verify = true;
                        }
                        if (verify) {
                            showWaitCursor();
                            int hits = 0;
                            for (int j=0; j<newGroups.size(); j++) {
                                setStatus("Verifying image");
                                typeList = new ArrayList();
                                String newGroup = (String)newGroups.get(j);
                                boolean check = false;
                                int intCheck = checkServer(newServer, "image", newGroup);
                                if (intCheck == -2) return;
                                else if (intCheck == 0) check = true;
                                imageTypeCbx.setSelected(check);
                                if (check) {
                                    hits++;
                                    typeList.add("image");
                                }
                                setStatus("Verifying point");
                                intCheck = checkServer(newServer, "point", newGroup);
                                if (intCheck == -2) return;
                                check = false;
                                if (intCheck == 0) check = true;
                                pointTypeCbx.setSelected(check);
                                if (check) {
                                    hits++;
                                    typeList.add("point");
                                }
                                setStatus("Verifying grid");
                                intCheck = checkServer(newServer, "grid", newGroup);
                                if (intCheck == -2) return;
                                check = false;
                                if (intCheck == 0) check = true;
                                gridTypeCbx.setSelected(check);
                                if (check) {
                                    hits++;
                                    typeList.add("grid");
                                }
                                setStatus("Verifying text");
                                intCheck = checkServer(newServer, "text", newGroup);
                                if (intCheck == -2) return;
                                check = false;
                                if (intCheck == 0) check = true;
                                textTypeCbx.setSelected(check);
                                if (check) {
                                    hits++;
                                    typeList.add("text");
                                }
                                setStatus("Verifying nav");
                                intCheck = checkServer(newServer, "nav", newGroup);
                                if (intCheck == -2) return;
                                check = false;
                                if (intCheck == 0) check = true;
                                navTypeCbx.setSelected(check);
                                if (check) {
                                    hits++;
                                    typeList.add("nav");
                                }
                                if (apply) {
                                    if (hits > 0) addNewServer(newServer, newGroup, typeList);
                                }
                                if (hits == 0) {
                                     //user = "";
                                     //proj = "";
                                     sendVerificationFailure(newServer, newGroup);
                                     //user = DEFAULT_USER;
                                     //proj = DEFAULT_PROJ;
                                }
                                //setStatus("Verify done");
                            }
                        }
                        if (apply) {
                            if (!verify) addNewServer(newServer, grp, typeList);
                            closeAddServer();
                            //setStatus("Apply done");
                        }
                        showNormalCursor();
                    }
                    setStatus("Done");
                }
            };


            JPanel bottom =
                //GuiUtils.inset(GuiUtils.makeApplyCancelButtons(listener),5);
                GuiUtils.inset(makeVerifyApplyCancelButtons(listener),5);
            JComponent contents = GuiUtils.topCenterBottom(nameComp, dataTypes, bottom);
            addWindow.getContentPane().add(contents);
            addWindow.pack();
            addWindow.setLocation(200, 200);
        }
        addWindow.setVisible(true);
        GuiUtils.toFront(addWindow);
    }

    /**
     * showAacctDialog
     */
    private void showAcctDialog() {
        if (acctWindow == null) {
            List comps = new ArrayList();

            acctWindow = GuiUtils.createFrame("ADDE Project/User name");

            userFld = new JTextField(user, 10);
            projFld = new JTextField(proj, 10);

            List textComps = new ArrayList();
            textComps.add(new JLabel(" "));
            textComps.add(GuiUtils.hbox(new JLabel("User ID: "), userFld));
            textComps.add(new JLabel(" "));
            textComps.add(GuiUtils.hbox(new JLabel("Project #: "), projFld));
            textComps.add(new JLabel(" "));
            JComponent textComp = GuiUtils.center(GuiUtils.inset(
                                     GuiUtils.vbox(textComps),20));

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    String cmd = event.getActionCommand();
                    if (cmd.equals(GuiUtils.CMD_CANCEL)) {
                        acctWindow.setVisible(false);
                        acctWindow = null;
                    } else {
                        user = userFld.getText().trim();
                        proj = projFld.getText().trim();
                        setUserProj();
                        closeAccounting();
                    }
                }
            };


            JPanel bottom =
                GuiUtils.inset(GuiUtils.makeOkCancelButtons(listener),5);
            JComponent contents = GuiUtils.centerBottom(textComp, bottom);
            acctWindow.getContentPane().add(contents);
            acctWindow.pack();
            acctWindow.setLocation(200, 200);
        }
        acctWindow.setVisible(true);
        GuiUtils.toFront(acctWindow);
    }

    /**
     * Utility to make verify/apply/cancel button panel
     *
     * @param l The listener to add to the buttons
     * @return The button panel
     */
    public static JPanel makeVerifyApplyCancelButtons(ActionListener l) {
        return GuiUtils.makeButtons(l, new String[] { "Verify and Apply", 
                                                      "Verify", "Apply", "Cancel" },
                           new String[] { CMD_VERIFYAPPLY,
                                          CMD_VERIFY,
                                          GuiUtils.CMD_APPLY,
                                          GuiUtils.CMD_CANCEL });
    }

    /**
     * Close the add dialog
     */
    public void closeAddServer() {
        if (addWindow != null) {
            addWindow.setVisible(false);
        }
    }

    /**
     * Close the accounting dialog
     */
    public void closeAccounting() {
        if (acctWindow != null) {
            acctWindow.setVisible(false);
        }
    }

    /**
     * Import the servers and groups from paths other than %USERPATH%
     */
    public void getSystemServers() {
        List addeServers = new ArrayList();
        List myServers = new ArrayList();

        XmlResourceCollection addeServerResources =
            getIdv().getResourceManager().getXmlResources(
                IdvResourceManager.RSC_ADDESERVER);
        try {
            for (int resourceIdx = 0;
                    resourceIdx < addeServerResources.size(); resourceIdx++) {
                //if (addeServerResources.isWritableResource(resourceIdx)) continue;
                Element root = addeServerResources.getRoot(resourceIdx);
                if (root == null) {
                    continue;
                }
                List servers = AddeServer.processXml(root);
                for (int serverIdx = 0; serverIdx < servers.size();
                        serverIdx++) {
                    AddeServer addeServer =
                        (AddeServer) servers.get(serverIdx);
                    String name = addeServer.getName();
                    name = name.toUpperCase();
                    addeServer.setName(name);
                    addeServer.setIsLocal(true);
                    List groups = addeServer.getGroups();
                    for (int groupIdx = 0; groupIdx < groups.size();
                            groupIdx++) {
                        AddeServer.Group group =
                            (AddeServer.Group) groups.get(groupIdx);
                        group.setIsLocal(true);
                    }
                }
                addeServers.addAll(servers);
                if (addeServerResources.isWritableResource(resourceIdx))
                    myServers.addAll(servers);
            }
        } catch (Exception exc) {
            System.out.println("ServerPreferenceManager getSystemServers: exc=" + exc);
        }
        if ((myServers.size() > 0) && (addeServers.size() > 0)) {
            for (int i=0; i<myServers.size(); i++) {
                AddeServer mine = (AddeServer)myServers.get(i);
                for (int j=0; j<addeServers.size(); j++) {
                    Object o = addeServers.get(j);
                    if (mine.equals(o))
                        addeServers.remove(o);
                }
            }
        }
        addeServers = AddeServer.coalesce(addeServers);
        updateTree(addeServers);
        XmlResourceCollection serverCollection =
           getIdv().getResourceManager().getXmlResources(
           IdvResourceManager.RSC_ADDESERVER);
        Element serverRoot = serverCollection.getWritableRoot("<tabs></tabs>");
        Document serverDocument = serverCollection.getWritableDocument("<tabs></tabs>");
        try {
            Element serversEle = AddeServer.toXml(addeServers, false);
            serversEle.setAttribute(ATTR_USER, user);
            serversEle.setAttribute(ATTR_PROJ, proj);
            serverCollection.setWritableDocument(serverDocument, serversEle);
            serverCollection.writeWritable();
        } catch (Exception e) {
            System.out.println("AddeServer.toXml e=" + e);
        }
        si = null;
        setStatus("Done");
        return;
    }
 
    /**
     * Import the servers and groups from MCTABLE
     */
    public void getServersFromMctable() {
        setStatus("Locate MCTABLE.TXT");
        if (si == null) {
            si = new ServerInfo(getIdv(), serversXRC);
        }
        List addeServers = new ArrayList();
        for (int i=0; i<allTypes.length; i++) {
            List siList = si.getAddeServers(allTypes[i], true, false);
            if (!siList.isEmpty()) {
                addeServers.addAll(siList);
            }
        }
        JFileChooser chooser = new JFileChooser();
        PatternFileFilter ff = new PatternFileFilter("MCTABLE.TXT", "McIDAS-X ADDE Routing Table");
        chooser.setFileFilter(ff);
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.CANCEL_OPTION) {
            setStatus("");
            return;
        }
        File file = chooser.getSelectedFile();
        if (file == null) return;
        setStatus("Checking user and project number...");
        if (!setUserProj()) {
            setStatus(" ");
            return;
        }
        StringTokenizer tok;
        String next;
        try {
            setStatus("Reading MCTABLE.TXT...");
            InputStream is = IOUtil.getInputStream(file.toString());
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));
            String lineOut = reader.readLine();
            String lineType;
            String serv;
            StringTokenizer tokTwo;
            AddeServer as = new AddeServer();
            CharSequence dot = (CharSequence)".";
            while (lineOut != null) {
                tok = new StringTokenizer(lineOut, "_");
                lineType = tok.nextToken();
                next = tok.nextToken();
                if (lineType.equals("HOST")) {
                    tokTwo = new StringTokenizer(next, "=");
                    String server = tokTwo.nextToken();
                    if (!server.contains(dot)) {
                        next = tokTwo.nextToken();
                        for (int i=0; i<addeServers.size(); i++) {
                            as = (AddeServer)addeServers.get(i);
                            serv = (String)as.getName();
                            if (serv.equals(server)) {
                                as.setName(next);
                                addeServers.set(i,as);
                            }
                        }
                    }
                } else if (lineType.equals("ADDE")) {
                    if (next.equals("ROUTE")) {
                        next = tok.nextToken();
                        tokTwo = new StringTokenizer(next,"=");
                        next = tokTwo.nextToken();
                        serv = tokTwo.nextToken();
                        if (!serv.equals("LOCAL-DATA")) {
                            for (int typeIdx=0; typeIdx<allTypes.length; typeIdx++) {
                                String typ = allTypes[typeIdx];
                                setStatus(serv + "/" + next + "   Checking for " + typ);
                                if (checkServer(serv, typ, next) != 0) continue;
                                as = new AddeServer(serv);
                                AddeServer.Group g = new AddeServer.Group(typ, next, "");
                                as.addGroup(g);
                                addeServers.add(as);
                                List typeList = new ArrayList();
                                typeList.add(typ);
                                addNewServer(serv, next, typeList);
                            }
                        }
                    }
                }
                lineOut = reader.readLine();
            } 
        } catch (Exception e) {
            System.out.println("getServersFromMctable e=" + e);
            return;
        }

        List serversFinal = AddeServer.coalesce(addeServers);

        XmlResourceCollection serverCollection =
           getIdv().getResourceManager().getXmlResources(
           IdvResourceManager.RSC_ADDESERVER);
        Element serverRoot = serverCollection.getWritableRoot("<tabs></tabs>");
        Document serverDocument = serverCollection.getWritableDocument("<tabs></tabs>");
        try {
            Element serversEle = AddeServer.toXml(serversFinal, false);
            serversEle.setAttribute(ATTR_USER, user);
            serversEle.setAttribute(ATTR_PROJ, proj);
            serverCollection.setWritableDocument(serverDocument, serversEle);
            serverCollection.writeWritable();
        } catch (Exception e) {
            System.out.println("AddeServer.toXml e=" + e);
        }
        si = null;
        setStatus("Done");
        return;
    }

    private boolean setUserProj() {
        if (!(((user.equals(DEFAULT_USER)) || (user.equals(""))) && 
            ((proj.equals(DEFAULT_PROJ)) || (proj.equals(""))))) return true;
        projFld = new JTextField("", 10);
        userFld = new JTextField("", 10);
        JLabel     label     = null;
        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent contents = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("User ID:"),
            userFld, GuiUtils.rLabel("Project #:"), projFld, }, 2,
                GuiUtils.WT_N, GuiUtils.WT_N);
        label    = new JLabel(" ");
        contents = GuiUtils.topCenter(label, contents);
        contents = GuiUtils.inset(contents, 5);
        String lbl = ("Please enter a user ID & project number for access");
        label.setText(lbl);

        if ( !GuiUtils.showOkCancelDialog(null, "ADDE Project/User name",
                contents, null)) {
            return false;
        }
        user = userFld.getText().trim();
        proj  = projFld.getText().trim();

        if (si == null) {
            si = new ServerInfo(getIdv(), serversXRC);
        }
        return true;
    }

    private int checkServer(String server, String type, String group) {
        String[] servers = { server };
        AddeServerInfo asi = new AddeServerInfo(servers);
        asi.setUserIDandProjString("user=" + user + "&proj=" + proj);
        int stat = asi.setSelectedServer(server, type.toUpperCase());
        String[] serverList = asi.getServerList();
        if (!server.equals(lastServerChecked)) {
            lastServerChecked = server;
            while (stat == -1) {
                user = "";
                proj = "";
                if (!setUserProj()) {
                    user = DEFAULT_USER;
                    proj = DEFAULT_PROJ;
                    return -2;
                }
                asi.setUserIDandProjString("user=" + user + "&proj=" + proj);
                stat =asi.setSelectedServer(server , type.toUpperCase());
                if (stat < 0) {
                    sendVerificationFailure(server, group);
                    user = DEFAULT_USER;
                    proj = DEFAULT_PROJ;
                }
            }
        }
        asi.setSelectedGroup(group);
        String[] datasets = asi.getDatasetList();
        int len =0;
        try {
            len = datasets.length;
        } catch (Exception e) {};
        if (len < 1) return -1;
        return 0;
    }

    private void sendVerificationFailure(String server, String group) {
        String titleBar = "Verification Failure";
        Component[] comps = new Component[4];
        comps[0] = GuiUtils.lLabel("  Server: " + server);
        comps[1] = GuiUtils.lLabel("  Group(s): " + group);
        comps[2] = GuiUtils.lLabel("  User ID: " + user);
        comps[3] = GuiUtils.lLabel("  Project Number: " + proj);
        JComponent contents = GuiUtils.doLayout(comps, 1,
            GuiUtils.WT_N, GuiUtils.WT_N);
        contents = GuiUtils.center(contents);
        contents = GuiUtils.inset(contents, 10);
        GuiUtils.showOkCancelDialog(null, titleBar, contents, null);
    }

    private void addNewServer(String newServer, String grp, List type) {
        showWaitCursor();
        StringTokenizer tok = new StringTokenizer(grp, ",");
        List newGroups = new ArrayList();
        while (tok.hasMoreTokens()) {
            newGroups.add(tok.nextToken().trim());
        }
        String typeString = "";
        if (type != null) {
            for (int i=0; i<type.size(); i++) {
                typeString =(String)type.get(i);
                for (int j=0; j<newGroups.size(); j++) {
                    ServerDescriptor sd = new ServerDescriptor(typeString,
                        newServer, (String)newGroups.get(j), "true");
                    final JCheckBox cbx = new JCheckBox(sd.toString(), sd.getIsActive());
                    cbxToServerMap.put(cbx, sd);
                    CheckboxCategoryPanel catPanel =
                        (CheckboxCategoryPanel) catMap.get(typeString);
                    final JPanel pan = GuiUtils.inset(cbx, new Insets(0, 20, 0, 0));
                    catPanel.add(pan);
                    final String str = typeString;
                    cbx.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            lastCat = str;
                            lastBox = cbx;
                            lastPan = pan;
                            deleteServer.setEnabled(true);
                        }
                    });
                    catPanel.validate();
                }
            }
        }
        showNormalCursor();
    }


    /**
     * Export the selected servers to the plugin manager
     */
    public void exportServersToPlugin() {
        JLabel label = new JLabel("Not yet implemented");
        JPanel contents = GuiUtils.top(GuiUtils.inset(label, 5));
        GuiUtils.showOkCancelDialog(null, "Export to Plugin",
            contents, null, null);
/*
        Hashtable    selected           = new Hashtable();
        Hashtable    table              = cbxToCdMap;
        List         controlDescriptors = getIdv().getAllControlDescriptors();
        StringBuffer sb                 =
            new StringBuffer(XmlUtil.XML_HEADER);
        sb.append("<" + ControlDescriptor.TAG_CONTROLS + ">\n");
        for (Enumeration keys = table.keys(); keys.hasMoreElements(); ) {
            JCheckBox cbx = (JCheckBox) keys.nextElement();
            if ( !cbx.isSelected()) {
                continue;
            }
            ControlDescriptor cd = (ControlDescriptor) table.get(cbx);
            cd.getDescriptorXml(sb);
        }

        sb.append("</" + ControlDescriptor.TAG_CONTROLS + ">\n");
        getIdv().getPluginManager().addText(sb.toString(), "controls.xml");
*/
    }

    /**
     * Update servers.xml
     */
    private void updateXml() {
        List servers = new ArrayList();
        Hashtable table = cbxToServerMap;
        allServers.clear();
        for (Enumeration keys = table.keys(); keys.hasMoreElements(); ) {
            JCheckBox cbx = (JCheckBox)keys.nextElement();
            ServerDescriptor sd = (ServerDescriptor)table.get(cbx);
            sd.setIsActive(true);
            if (!cbx.isSelected()) {
                sd.setIsActive(false);
            }
            allServers.add(sd);
            AddeServer as = new AddeServer(sd.getServerName());
            AddeServer.Group g = new AddeServer.Group(sd.getDataType(), 
                                         sd.getGroupName(), "");
            g.setActive(sd.getIsActive());
            as.addGroup(g);
            servers.add(as);
        }
        List serversFinal = AddeServer.coalesce(servers);

        XmlResourceCollection serverCollection =
           getIdv().getResourceManager().getXmlResources(
           IdvResourceManager.RSC_ADDESERVER);
        Element serverRoot = serverCollection.getWritableRoot("<tabs></tabs>");
        Document serverDocument = serverCollection.getWritableDocument("<tabs></tabs>");
        try {
            Element serversEle = AddeServer.toXml(serversFinal, false);
            serversEle.setAttribute(ATTR_USER, user);
            serversEle.setAttribute(ATTR_PROJ, proj);
            serverCollection.setWritableDocument(serverDocument, serversEle);
            serverCollection.writeWritable();
        } catch (Exception e) {
            System.out.println("updateXml AddeServer.toXml e=" + e);
        }
    }
                
    /**
     * Get the xml resource collection that defines the servers xml
     *
     * @return server resources
     */
    protected XmlResourceCollection getServers() {
        XmlResourceCollection serverCollection =
           getIdv().getResourceManager().getXmlResources(
           IdvResourceManager.RSC_ADDESERVER);
        si = new ServerInfo(getIdv(), serverCollection);
        user = si.getUser();
        proj = si.getProj();
        return serverCollection;
    }


    private void updateTree(List addeServers) {
        for (int i=0; i<addeServers.size(); i++) {
            AddeServer newServer = (AddeServer)addeServers.get(i);
            String name = newServer.getName();
            List groups = newServer.getGroups();
            for (int j=0; j<groups.size(); j++) {
                Group grp = (Group)groups.get(j);
                String groupName = grp.getName();
                List type = new ArrayList();
                String grpType = grp.getType();
                if (typePanels.contains(grpType)) {
                    type.add(grp.getType());
                    addNewServer(name, groupName, type);
                }
            }
        }
    }
}
