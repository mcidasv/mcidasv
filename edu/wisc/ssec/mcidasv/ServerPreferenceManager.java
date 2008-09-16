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

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.list;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newHashSet;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newMap;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.set;
import static edu.wisc.ssec.mcidasv.util.filter.Filters.any;
import static edu.wisc.ssec.mcidasv.util.filter.Filters.filter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.IdvResourceManager.IdvResource;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.idv.chooser.adde.AddeServer.Group;
import ucar.unidata.ui.CheckboxCategoryPanel;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.Resource;
import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import edu.wisc.ssec.mcidas.adde.AddeServerInfo;
import edu.wisc.ssec.mcidas.adde.AddeTextReader;
import edu.wisc.ssec.mcidasv.addemanager.AddeEntry;
import edu.wisc.ssec.mcidasv.addemanager.AddeManager;
import edu.wisc.ssec.mcidasv.chooser.ServerInfo;
import edu.wisc.ssec.mcidasv.chooser.TestAddeImageChooser;
import edu.wisc.ssec.mcidasv.chooser.adde.AddeChooser;
import edu.wisc.ssec.mcidasv.util.filter.Filter;

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
//    private JFrame addWindow;

    /** add accounting dialog */
    private JFrame acctWindow;

    /** Shows the status */
    private JLabel statusLabel;

    /** _more_          */
    private JComponent statusComp;

    private PreferenceManager serversManager = null;
    private JPanel serversPanel = null;

//    private final JButton deleteServer = new JButton("Delete");
    private ServerInfo si;

    private static String user;
    private static String proj;

    private Hashtable catMap = new Hashtable();

    private String[] allTypes = {"image", "point", "grid", "text", "nav"};
    private static final Set<String> VALID_TYPES = set("image", "point", "grid", "text", "nav", "radar", "any");

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

//    private final XmlResourceCollection serversXRC = getServers();

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

    protected static final String PREF_LIST_SITE_SERV = 
        "mcv.servers.listsite";
    protected static final String PREF_LIST_DEFAULT_SERV = 
        "mcv.servers.listdefault";
    protected static final String PREF_LIST_MCTABLE_SERV = 
        "mcv.servers.listmcx";
    protected static final String PREF_LIST_USER_SERV = 
        "mcv.servers.listuser";

    protected static final String PREF_DEFAULT_USER = "mcv.servers.defaultuser";
    protected static final String PREF_DEFAULT_PROJ = "mcv.servers.defaultproj";

    private static final Pattern routePattern = 
        Pattern.compile("^ADDE_ROUTE_(.*)=(.*)$");

    private static final Matcher routeMatcher = 
        routePattern.matcher("");

    private static final Pattern hostPattern = 
        Pattern.compile("^HOST_(.*)=(.*)$");

    private static final Matcher hostMatcher = 
        hostPattern.matcher("");

    private boolean findNewMctable = false;

    private List<JPanel> servList = arrList();
    private Set<DatasetDescriptor> currentDescriptors = newLinkedHashSet();
    private Map<String, Category> panelMap = newMap();
    private Set<AddeChooser> managedChoosers = newLinkedHashSet();
    private Set<DatasetDescriptor> addedBatch = newLinkedHashSet();

    private Map<String, Set<DatasetDescriptor>> sourceToData = 
        unpersistServers();

    public enum AddeStatus { BAD_SERVER, BAD_ACCOUNTING, NO_METADATA, OK, BAD_GROUP };

    /** Number of threads in the thread pool. */
    private static final int POOL = 5;

    /** Thread pool. */
    private static final ExecutorService exec = Executors.newFixedThreadPool(POOL);

    /** 
     * {@link String#format(String, Object...)}-friendly string for building a
     * request to read a server's PUBLIC.SRV.
     */
    private static final String publicSrvFormat = "adde://%s/text?compress=gzip&port=112&debug=false&version=1&user=%s&proj=%s&file=PUBLIC.SRV";

    /**
     * Create the dialog with the given idv
     * 
     * @param idv The IDV
     */
    public ServerPreferenceManager(IntegratedDataViewer idv) {
        super(idv);
        user = DEFAULT_USER;
        proj = DEFAULT_PROJ;

        getServerPreferences();
    }

    public static String getDefaultUser() {
        return DEFAULT_USER;
    }

    public static String getDefaultProject() {
        return DEFAULT_PROJ;
    }

    public Map<String, String> getAccounting(final AddeServer server) {
//        System.err.println("getAccounting: looking for " + server);
        Map<String, String> info = newMap();
        info.put("user", DEFAULT_USER);
        info.put("proj", DEFAULT_PROJ);
        for (DatasetDescriptor descriptor : currentDescriptors) {
            AddeServer addeServer = descriptor.getServer();
            if (!server.equals(addeServer))
                continue;

            String user = descriptor.getUser();
            if (user != null || user.length() > 0)
                info.put("user", user);

            String proj = descriptor.getProj();
            if (proj != null || proj.length() > 0)
                info.put("proj", proj);
            break;
        }
//        System.err.println("getAccounting: found " + info);
        return info;
    }
    
    @Override protected JComponent doMakeContents() {
        serversPanel = buildServerPanel(createPanelThings());
        ((McIdasPreferenceManager)getIdv().getPreferenceManager()).replaceServerPrefPanel(serversPanel);
        return serversPanel;
    }

    public void addServerPreferences(IdvPreferenceManager ipm) {
        getServerPreferences();
        ipm.add("ADDE Servers",
                 "What servers should be shown in choosers?",
                 serversManager, serversPanel, cbxToServerMap);
    }

    public void addManagedChooser(AddeChooser chooser) {
        if (chooser == null)
            throw new NullPointerException();
        
        managedChoosers.add(chooser);
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

    protected void categoryChanged(final Category category) {
        serversPanel = buildServerPanel(createPanelThings());
        ((McIdasPreferenceManager)getIdv().getPreferenceManager()).replaceServerPrefPanel(serversPanel);
    }

    private Map<String, Category> buildCategories(
        final Set<DatasetDescriptor> descriptors) 
    {
        assert descriptors != null;

        Map<String, Category> panelMap = newMap();
        for (String typeName : VALID_TYPES) {
            Filter<DatasetDescriptor> typeFilter = 
                new GroupTypeFilter(typeName);

            Filter<DatasetDescriptor> invisFilter = 
                new InvisibleFilter(getStore());

            Filter<DatasetDescriptor> f = typeFilter.and(invisFilter);

            Set<DatasetDescriptor> filtered = filter(f, descriptors);
            if (filtered.isEmpty())
                continue;

            Category catPanel = new Category(getIdv(), this, typeName, filtered);
            panelMap.put(typeName, catPanel);
//            if (typeName.equals("any")) {
//                System.err.println("filtered:");
//                printDDSet(filtered);
//                System.err.println("from panel:");
//                printDDSet(panelMap.get(typeName).getAllDescriptors());
//                System.err.println();
//            }
        }
        return panelMap;
    }

    private JButton createDeleteButton() {
        final JButton deleteServer = new JButton("Delete");
        deleteServer.setEnabled(false);
        deleteServer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                deleteServers();
                deleteServer.setEnabled(false);
            }
        });
        return deleteServer;
    }

    private JButton createAccountingButton() {
        final JButton accounting = new JButton("Accounting");
        accounting.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                addAccounting();
            }
        });
        return accounting;
    }

    private JButton createEnableAllButton() {
        final JButton allOn = new JButton("All on");
        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                // once you get the category checkboxes working you can merely 
                // iterate over the categories.
                for (DatasetDescriptor dd : getAllServers()) {
                    dd.setEnabled(true);
                }
              serversPanel = buildServerPanel(createPanelThings());
              ((McIdasPreferenceManager)getIdv().getPreferenceManager()).replaceServerPrefPanel(serversPanel);
            }
        });
        allOn.setEnabled(true);
        return allOn;
    }

    private JButton createDisableAllButton() {
        final JButton allOff = new JButton("All off");
        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (DatasetDescriptor dd : getAllServers()) {
                    dd.setEnabled(false);
                }
              serversPanel = buildServerPanel(createPanelThings());
              ((McIdasPreferenceManager)getIdv().getPreferenceManager()).replaceServerPrefPanel(serversPanel);
            }
        });
        allOff.setEnabled(true);
        return allOff;
    }

    private JButton createAddServerButton() {
        final JButton addServer = new JButton("Add");
        addServer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showAddDialog(null, null, false, false);
//                addWindow.setVisible(true);
//                GuiUtils.toFront(addWindow);
//                serversPanel = buildServerPanel(createPanelThings());
//                ((McIdasPreferenceManager)getIdv().getPreferenceManager()).replaceServerPrefPanel(serversPanel);
            }
        });
        return addServer;
    }

    private JButton createImportMctableButton() {
        final JButton fromMcX = new JButton("From McIDAS-X");
        fromMcX.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showWaitCursor();
                findNewMctable = true;
                serversPanel = buildServerPanel(createPanelThings());
                ((McIdasPreferenceManager)getIdv().getPreferenceManager()).replaceServerPrefPanel(serversPanel);
                showNormalCursor();
            }
        });
        return fromMcX;
    }

    private JCheckBox createFilterMctableBox() {
        final JCheckBox includeMctableServers = createInclusionBox(PREF_LIST_MCTABLE_SERV, "Include McIDAS-X Servers", false);
        includeMctableServers.addActionListener(new ActionListener() { 
            public void actionPerformed(final ActionEvent e) {
                showWaitCursor();
                getStore().put(PREF_LIST_MCTABLE_SERV, includeMctableServers.isSelected());
                serversPanel = buildServerPanel(createPanelThings());
                ((McIdasPreferenceManager)getIdv().getPreferenceManager()).replaceServerPrefPanel(serversPanel);
                showNormalCursor();
            }
        });
        return includeMctableServers;
    }

    private JCheckBox createFilterDefaultBox() {
        final JCheckBox includeDefaultServers = createInclusionBox(PREF_LIST_DEFAULT_SERV, "Include Default Servers", true);
        includeDefaultServers.addActionListener(new ActionListener() { 
            public void actionPerformed(final ActionEvent e) {
                showWaitCursor();
                getStore().put(PREF_LIST_DEFAULT_SERV, includeDefaultServers.isSelected());
                serversPanel = buildServerPanel(createPanelThings());
                ((McIdasPreferenceManager)getIdv().getPreferenceManager()).replaceServerPrefPanel(serversPanel);
                showNormalCursor();
            }
        });
        return includeDefaultServers;
    }

    private JCheckBox createFilterSiteBox() {
        final JCheckBox includeSiteServers = createInclusionBox(PREF_LIST_SITE_SERV, "Include SSEC Servers", false);
        includeSiteServers.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                showWaitCursor();
                getStore().put(PREF_LIST_SITE_SERV, includeSiteServers.isSelected());
                serversPanel = buildServerPanel(createPanelThings());
                ((McIdasPreferenceManager)getIdv().getPreferenceManager()).replaceServerPrefPanel(serversPanel);
                showNormalCursor();
            }
        });
        return includeSiteServers;
    }
    
    private JCheckBox createFilterUserBox() {
        final JCheckBox includeFilterServers = createInclusionBox(PREF_LIST_USER_SERV, "Include Your Servers", false);
        includeFilterServers.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                showWaitCursor();
                getStore().put(PREF_LIST_USER_SERV, includeFilterServers.isSelected());
                serversPanel = buildServerPanel(createPanelThings());
                ((McIdasPreferenceManager)getIdv().getPreferenceManager()).replaceServerPrefPanel(serversPanel);
                showNormalCursor();
            }
        });
        return includeFilterServers;
    }

    private List<JComponent> createPanelThings() {
        List<JComponent> comps = arrList();
        comps.add(createAccountingButton());
        comps.add(new JLabel(" "));
        comps.add(createEnableAllButton());
        comps.add(createDisableAllButton());
        comps.add(new JLabel(" "));
        comps.add(createAddServerButton());
        comps.add(createDeleteButton());
        comps.add(new JLabel(" "));
        comps.add(createImportMctableButton());
        for (int i = 0; i < 9; i++)
            comps.add(new JLabel(" "));
        comps.add(new JLabel("     -- Filter Server List --"));
        comps.add(createFilterMctableBox());
        comps.add(createFilterDefaultBox());
        comps.add(createFilterSiteBox());
        comps.add(createFilterUserBox());
        return comps;
    }

    /**
     * Add in the user preference tab for the servers to show.
     */
    protected void getServerPreferences() {
        serversPanel = buildServerPanel(createPanelThings());
        serversManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore store, Object data) {
                persistServers(getAllServers());
            }
        };
    }

    public void updateManagedChoosers() {
        for (AddeChooser chooser : managedChoosers) {
//            chooser.updateServers();
//            chooser.updateGroups();
            chooser.updateServerList();
        }
    }

    private JPanel buildServerPanel(final List<JComponent> comps) {
//        System.err.println("build server panel");
        currentDescriptors = getServerSet();
        panelMap = buildCategories(currentDescriptors);
        servList = extractPanels(panelMap);

        final JPanel servPanel = GuiUtils.top(GuiUtils.doLayout(new JPanel(), 
             GuiUtils.getComponentArray(servList), 1, GuiUtils.WT_Y, GuiUtils.WT_N));
        GuiUtils.enableTree(servPanel, true);
        JScrollPane servScroller = new JScrollPane(servPanel);
        servScroller.getVerticalScrollBar().setUnitIncrement(10);
        servScroller.setPreferredSize(new Dimension(300, 300));

        JComponent servComp = GuiUtils.centerBottom(servScroller, new JLabel(" "));
        JPanel bottomPanel =
            GuiUtils.leftCenter(
                GuiUtils.inset(
                    GuiUtils.top(GuiUtils.vbox(comps)),
                    4), new Msg.SkipPanel(
                        GuiUtils.hgrid(
                            Misc.newList(servComp, new JLabel(" ")), 0)));

        JPanel serverPanel =
            GuiUtils.inset(GuiUtils.topCenter( GuiUtils.vbox(new JLabel(" "),
                GuiUtils.hbox(GuiUtils.rLabel("Status: "),getStatusComponent()),
                new JLabel(" "), new JLabel(" ")),
                bottomPanel), 6);
        return serverPanel;
    }

    private List<JPanel> extractPanels(final Map<String, Category> map) {
        assert map != null;
        List<JPanel> servList = arrList();
        for (String typeName : VALID_TYPES) {
            if (!map.containsKey(typeName)) {
//                System.err.println("missing key=" + typeName);
                continue;
            }
            servList.addAll(map.get(typeName).buildEntry());
        }
        return servList;
    }

    private JCheckBox createInclusionBox(final String id, final String title, 
        final boolean defaultValue) 
    {
        assert id != null;
        assert title != null;

        JCheckBox box = new JCheckBox(title);
        box.setSelected(getStore().get(id, defaultValue));
        return box;
    }

    public Set<DatasetDescriptor> getPreferredServers() {
        Filter enabled = new EnabledDatasetFilter();
        Filter invis = new InvisibleFilter(getStore());
        Filter f = enabled.and(invis);
        return filter(f, getAllServers());
    }

    public List<AddeServer> getAddeServers() {
        Set<DatasetDescriptor> datasets = getPreferredServers();
        List<AddeServer> servers = arrList();
        for (DatasetDescriptor descriptor : datasets) {
            servers.add(descriptor.getServer());
        }
        return AddeServer.coalesce(servers);
    }

    public Set<DatasetDescriptor> getAllServers() {
        return currentDescriptors;
    }

    public List<Group> getGroups(final AddeServer server, final String type) {
        if (server == null)
            throw new NullPointerException();
        if (type == null)
            throw new NullPointerException();

        Filter<DatasetDescriptor> enabled = new EnabledDatasetFilter();
        Filter<DatasetDescriptor> servers = new AddeServerFilter(server);
        Filter<DatasetDescriptor> types = new GroupTypeFilter(type);
        Filter<DatasetDescriptor> groupFilter = servers.and(enabled).and(types);
//        Filter groupFilter = servers;

        Set<DatasetDescriptor> validDescriptors = filter(groupFilter, getAllServers());
        List<Group> groups = new ArrayList<Group>();
        for (DatasetDescriptor descriptor : validDescriptors) {
//            System.err.println("adding group=" + descriptor.getGroup().getName());
            groups.add(descriptor.getGroup());
        }
        return groups;
    }

//  <entry name="SERVER/DATASET" user="ASDF" proj="0000" source="user" enabled="true" type="image"/>
    public void persistServers(final Set<DatasetDescriptor> servers) {
        XmlResourceCollection userServers = 
            getResourceManager().getXmlResources(
                ResourceManager.RSC_NEW_USERSERVERS);

        Document doc = userServers.getWritableDocument("<servers></servers>");
        Element root = userServers.getWritableRoot("<servers></servers>");

        XmlUtil.removeChildren(root);

        for (DatasetDescriptor server : servers) {
            Element xml = doc.createElement("entry");
            xml.setAttribute("name", server.toPrefString());
            xml.setAttribute("description", server.getServerDescription());
            xml.setAttribute("user", server.getUser());
            xml.setAttribute("proj", server.getProj());
            xml.setAttribute("source", server.getSource());
            xml.setAttribute("enabled", Boolean.toString(server.getEnabled()));
            xml.setAttribute("type", server.getType());
            root.appendChild(xml);
        }

        try {
            userServers.writeWritable();
            userServers.clearCache();
        } catch (Exception e) {
            LogUtil.logException("ServerPreferenceManager.persistServers", e);
        }
    }

    private Map<String, Set<DatasetDescriptor>> unpersistServers() {
        Map<String, Set<DatasetDescriptor>> map = newMap();

        XmlResourceCollection userServers = 
            getResourceManager().getXmlResources(
                ResourceManager.RSC_NEW_USERSERVERS);

        Document doc = userServers.getWritableDocument("<servers></servers>");
        Element root = userServers.getWritableRoot("<servers></servers>");

        List<Element> entries = XmlUtil.findChildren(root, "entry");
        for (Element entryXml : entries) {
            String name = XmlUtil.getAttribute(entryXml, "name");
            String desc = "";
            if (XmlUtil.hasAttribute(entryXml, "description"))
                desc = XmlUtil.getAttribute(entryXml, "description");
            String user = XmlUtil.getAttribute(entryXml, "user");
            String proj = XmlUtil.getAttribute(entryXml, "proj");
            String source = XmlUtil.getAttribute(entryXml, "source");
            String type = XmlUtil.getAttribute(entryXml, "type");
            boolean enabled = Boolean.parseBoolean(XmlUtil.getAttribute(entryXml, "enabled"));
            boolean localData = false;
            if (name != null) {
                String[] arr = name.split("/");
                if (arr[0].toLowerCase().contains("localhost")) {
                    localData = true;
                    desc = "<LOCAL-DATA>";
                }

                if (desc.length() == 0)
                    desc = arr[0].toLowerCase();

                AddeServer server = new AddeServer(arr[0], desc);
                Group group = new Group(type, arr[1], arr[1]);
                server.addGroup(group);
                server.setIsLocal(localData);
                group.setIsLocal(localData);
                DatasetDescriptor dd = new DatasetDescriptor(server, group, source, user, proj);
                dd.setEnabled(enabled);

                Set<DatasetDescriptor> descSet = map.get(source);
                if (descSet == null)
                    descSet = newLinkedHashSet();

                descSet.add(dd);
                map.put(source, descSet);
            }
        }
        return map;
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

    public void serverEditor(String server, String group, boolean enableImage, boolean doWait) {
        JCheckBox isImage = new JCheckBox("Image", enableImage);
        JCheckBox isPoint = new JCheckBox("Point", false);
        JCheckBox isGrid = new JCheckBox("Grid", false);
    }
    
    /**
     * showAddDialog
     */
    public void showAddDialog(String server, String group, boolean enableImage, boolean doWait) {
        List comps = new ArrayList();
        comps.add(imageTypeCbx = new JCheckBox("Image", enableImage));
        comps.add(pointTypeCbx = new JCheckBox("Point", false));
        comps.add(gridTypeCbx = new JCheckBox("Grid", false));
        comps.add(textTypeCbx = new JCheckBox("Text", false));
        comps.add(navTypeCbx = new JCheckBox("Navigation", false));

        JPanel dataTypes = GuiUtils.inset(GuiUtils.hbox(comps, 5), 20);

        final JFrame addWindow = GuiUtils.createFrame("Add Server");

        if (server == null)
            server = "";
        if (group == null)
            group = "";
        serverFld = new JTextField(server, 30);
        groupFld = new JTextField(group, 30);

        List textComps = new ArrayList();
        textComps.add(new JLabel(" "));
        textComps.add(GuiUtils.hbox(new JLabel("Server: "), serverFld));
        textComps.add(new JLabel(" "));
        textComps.add(GuiUtils.hbox(new JLabel("Group(s): "), groupFld));
        textComps.add(new JLabel(" "));
        JComponent nameComp =
            GuiUtils.center(GuiUtils.inset(GuiUtils.vbox(textComps), 20));

        ActionListener listener = new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                String cmd = event.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_CANCEL)) {
//                    addWindow.setVisible(false);
//                    addWindow = null;
                    addWindow.dispose();
                } else {
                    String newServer = serverFld.getText().trim();
                    String grp = groupFld.getText().trim();
                    StringTokenizer tok = new StringTokenizer(grp, ",");
                    List newGroups = new ArrayList();
                    while (tok.hasMoreTokens()) {
                        newGroups.add(tok.nextToken().trim());
                    }
//                    List typeList = new ArrayList();
                    Set<String> types = newLinkedHashSet();
                    if (imageTypeCbx.isSelected())
                        types.add("image");
                    if (pointTypeCbx.isSelected())
                        types.add("point");
                    if (gridTypeCbx.isSelected())
                        types.add("grid");
                    if (textTypeCbx.isSelected())
                        types.add("text");
                    if (navTypeCbx.isSelected())
                        types.add("nav");
                    boolean apply = false;
                    boolean verify = false;
                    if (cmd.equals(CMD_VERIFY)) {
                        verify = true;
                    } else if (cmd.equals(CMD_APPLY)) {
                        apply = true;
                    } else if (cmd.equals(CMD_VERIFYAPPLY)) {
                        apply = true;
                        verify = true;
                    }
                    if (verify) {
                        showWaitCursor();
                        int hits = 0;
                        for (int j = 0; j < newGroups.size(); j++) {
                            setStatus("Verifying image");
                            types = newLinkedHashSet();
                            String newGroup = (String)newGroups.get(j);
                            boolean check = false;
                            int intCheck =
                                checkServer(newServer, "image", newGroup, user, proj);
                            if (intCheck == -2)
                                return;
                            else if (intCheck == 0)
                                check = true;
                            imageTypeCbx.setSelected(check);
                            if (check) {
                                hits++;
                                types.add("image");
                            }
                            setStatus("Verifying point");
                            intCheck =
                                checkServer(newServer, "point", newGroup, user, proj);
                            if (intCheck == -2)
                                return;
                            check = false;
                            if (intCheck == 0)
                                check = true;
                            pointTypeCbx.setSelected(check);
                            if (check) {
                                hits++;
                                types.add("point");
                            }
                            setStatus("Verifying grid");
                            intCheck =
                                checkServer(newServer, "grid", newGroup, user, proj);
                            if (intCheck == -2)
                                return;
                            check = false;
                            if (intCheck == 0)
                                check = true;
                            gridTypeCbx.setSelected(check);
                            if (check) {
                                hits++;
                                types.add("grid");
                            }
                            setStatus("Verifying text");
                            intCheck =
                                checkServer(newServer, "text", newGroup, user, proj);
                            if (intCheck == -2)
                                return;
                            check = false;
                            if (intCheck == 0)
                                check = true;
                            textTypeCbx.setSelected(check);
                            if (check) {
                                hits++;
                                types.add("text");
                            }
                            setStatus("Verifying nav");
                            intCheck = checkServer(newServer, "nav", newGroup, user, proj);
                            if (intCheck == -2)
                                return;
                            check = false;
                            if (intCheck == 0)
                                check = true;
                            navTypeCbx.setSelected(check);
                            if (check) {
                                hits++;
                                types.add("nav");
                            }
                            if (apply) {
                                if (hits > 0)
                                    addedBatch = addNewServer(newServer, newGroup, types, user, proj);
                            }
                            if (hits == 0) {
                                sendVerificationFailure(newServer, newGroup);
                            }
                            // setStatus("Verify done");
                        }
                    }
                    if (apply) {
                        if (!verify) {
                            if (types.isEmpty())
                                types.add("any");
                            addedBatch = addNewServer(newServer, grp, types, user, proj);
                        }
                        addWindow.dispose();
                        // setStatus("Apply done");
                    }
                    showNormalCursor();
                }
                setStatus("Done");
            }
        };

        JPanel bottom = 
            GuiUtils.inset(makeVerifyApplyCancelButtons(listener), 5);
        JComponent contents =
            GuiUtils.topCenterBottom(nameComp, dataTypes, bottom);
        addWindow.getContentPane().add(contents);
        addWindow.pack();
        addWindow.setLocation(200, 200);
        addWindow.setVisible(true);
    }

    public Set<DatasetDescriptor> getRecentlyAdded() {
        return addedBatch;
    }
    
    public Set<DatasetDescriptor> addNewServer(final String server, final String group, final Set<String> types, final String user, final String proj) {
        assert server != null;
        assert group != null;
        assert types != null;
        assert user != null;
        assert proj != null;

        showWaitCursor();
        StringTokenizer tok = new StringTokenizer(group, ",");
        List<String> newGroups = arrList();
        while (tok.hasMoreTokens())
            newGroups.add(tok.nextToken().trim());

        Set<DatasetDescriptor> added = newLinkedHashSet();
        for (String type : types) {
            Set<DatasetDescriptor> descriptors = newLinkedHashSet();
            for (String newGroup : newGroups) {
                AddeServer addeServ = new AddeServer(server);
                Group addeGroup = new Group(type, newGroup, newGroup);
                addeServ.addGroup(addeGroup);
                DatasetDescriptor dd = new DatasetDescriptor(addeServ, addeGroup, "user", user, proj);
                dd.setEnabled(true);
                descriptors.add(dd);

                if (currentDescriptors.contains(dd)) {
                    List<DatasetDescriptor> tmpList = 
                        new ArrayList<DatasetDescriptor>(currentDescriptors);
                    int index = tmpList.indexOf(dd);
                    if (index >= 0)
                        tmpList.set(index, dd);
                    else
                        tmpList.add(dd);

                    currentDescriptors = 
                        new LinkedHashSet<DatasetDescriptor>(tmpList);
                    System.err.println("altered " + dd);
                } else {
                    if (!currentDescriptors.add(dd)) {
                        System.err.println("error adding " + dd);
                    } else {
//                        System.err.println("added " + dd);
//                        printDDSet(currentDescriptors);
                        added.add(dd);
                    }
                }
            }
        }

        persistServers(currentDescriptors);
        sourceToData = unpersistServers();
        serversPanel = buildServerPanel(createPanelThings());
        ((McIdasPreferenceManager)getIdv().getPreferenceManager()).replaceServerPrefPanel(serversPanel);
        showNormalCursor();
        return added;
    }

    public Set<DatasetDescriptor> addNewDescriptors(final Set<DatasetDescriptor> descriptors) {
        if (descriptors == null)
            throw new NullPointerException("Cannot add a null descriptor");
        
        Set<DatasetDescriptor> added = newLinkedHashSet();
        for (DatasetDescriptor descriptor : descriptors) {
            if (!currentDescriptors.contains(descriptor)) {
                currentDescriptors.add(descriptor);
                added.add(descriptor);
            }
        }
        persistServers(currentDescriptors);
        sourceToData = unpersistServers();
        serversPanel = buildServerPanel(createPanelThings());
        ((McIdasPreferenceManager)getIdv().getPreferenceManager()).replaceServerPrefPanel(serversPanel);
        return added;
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
                        getStore().put(PREF_DEFAULT_USER, user);
                        getStore().put(PREF_DEFAULT_PROJ, proj);
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
     * Close the accounting dialog
     */
    public void closeAccounting() {
        if (acctWindow != null) {
            acctWindow.setVisible(false);
        }
    }

    private Set<DatasetDescriptor> getServers(final String source, 
        final IdvResource resources) 
    {
        assert source != null;
        assert resources != null;

        XmlResourceCollection xmlResources = getResourceManager().getXmlResources(resources);
        List<AddeServer> addeServers = arrList();
        for (int i = 0; i < xmlResources.size(); i++) {
            Element root = xmlResources.getRoot(i);
            if (root == null)
                continue;

            List<AddeServer> servers = AddeServer.processXml(root);
            for (AddeServer server : servers) {
                String servStr = server.toString().toLowerCase();
                server.setIsLocal(servStr.contains("localhost"));
                List<AddeServer.Group> groups = server.getGroups();
                for (AddeServer.Group group : groups)
                    group.setIsLocal(server.getIsLocal());
            }
            addeServers.addAll(servers);
        }
        return serversToDescriptors(source, AddeServer.coalesce(addeServers));
    }

    private static Set<DatasetDescriptor> printDDSet(Set<DatasetDescriptor> ugh) {
        for (DatasetDescriptor s : ugh)
            if (s.getServerName().contains("JON"))
                System.err.println(s);
        return ugh;
    }

    private Set<DatasetDescriptor> getDefaultServers() {
        Set<DatasetDescriptor> tmp = getServers("default", IdvResourceManager.RSC_ADDESERVER);
//      System.err.println("getDefServ: " + tmp.size() + ": " + tmp);
        return tmp;
    }

    private Set<DatasetDescriptor> getSiteServers() {
        Set<DatasetDescriptor> tmp = getServers("site", ResourceManager.RSC_SITESERVERS);
//      System.err.println("getSiteServ: " + tmp.size() + ": " + tmp);
        return tmp;
    }

    private Set<DatasetDescriptor> getUserServers() {
        Set<DatasetDescriptor> tmp = extractUserServers(ResourceManager.RSC_NEW_USERSERVERS);
//      System.err.println("getUserServ: " + tmp.size() + ": " + tmp);
        return tmp;
    }

    private Set<DatasetDescriptor> getOldStyleUserServers() {
        Set<DatasetDescriptor> tmp = getServers("user", ResourceManager.RSC_OLD_USERSERVERS);
//        System.err.println("getOldStyle");
//        printDDSet(tmp);
        return tmp;
    }

    private Set<DatasetDescriptor> getLocalServers() {
        Set<DatasetDescriptor> tmp = newLinkedHashSet();
        AddeManager addeManager = ((McIDASV)getIdv()).getAddeManager();
        List<AddeEntry> entries = addeManager.getAddeEntries();
        String port = addeManager.getLocalPort();
        for (AddeEntry entry : entries) {
            String name = entry.getGroup();
            String type = entry.getType().toLowerCase();
            String desc = entry.getDescription();
            Group group = new Group(type, name, desc);
            AddeServer server = new AddeServer("localhost:"+port, "<LOCAL-DATA>");
            server.setIsLocal(true);
            group.setIsLocal(true);
            server.addGroup(group);
            DatasetDescriptor dd = new DatasetDescriptor(server, group, "user", "", "");
            tmp.add(dd);
        }
        return tmp;
    }

    private Set<DatasetDescriptor> getMctableServers() {
        Set<DatasetDescriptor> tmp = newLinkedHashSet();
        if (findNewMctable) {
            String path = FileManager.getReadFile();
            if (path != null)
                tmp.addAll(extractMctableServers(path));
            findNewMctable = false;
        }
        return tmp;
    }

    private Set<DatasetDescriptor> extractUserServers(final IdvResource resources) {
        assert resources != null;

        Set<DatasetDescriptor> servers = newLinkedHashSet();
        XmlResourceCollection xmlResources = getResourceManager().getXmlResources(resources);
        for (int i = 0; i < xmlResources.size(); i++) {
            Element root = xmlResources.getRoot(i);
            if (root == null)
                continue;

//            <entry name="SERVER/DATASET" user="ASDF" proj="0000" source="user" enabled="true" type="image"/>
            List<Element> entries = XmlUtil.findChildren(root, "entry");
            for (Element entryXml : entries) {
                String name = XmlUtil.getAttribute(entryXml, "name");
                String user = XmlUtil.getAttribute(entryXml, "user");
                String proj = XmlUtil.getAttribute(entryXml, "proj");
                String source = XmlUtil.getAttribute(entryXml, "source");
                String type = XmlUtil.getAttribute(entryXml, "type");
                boolean enabled = Boolean.parseBoolean(XmlUtil.getAttribute(entryXml, "enabled"));
                boolean localData = false;
                if (source.equals("user") && (name != null)) {
                    String[] arr = name.split("/");
                    String desc = arr[0];
                    if (arr[0].toLowerCase().contains("localhost")) {
                        desc = "<LOCAL-DATA>";
                        localData = true;
                    }
                    AddeServer server = new AddeServer(arr[0], desc);
                    
                    if (user == null)
                        user = "";
                    if (proj == null)
                        proj = "";
                    if (type == null)
                        type = "any";

                    Group group = new Group(type, arr[1], arr[1]);
                    server.addGroup(group);

                    server.setIsLocal(localData);
                    group.setIsLocal(localData);

                    DatasetDescriptor dd = new DatasetDescriptor(server, group, "user", user, proj);
                    dd.setEnabled(enabled);
                    servers.add(dd);
                }
            }
        }
//        System.err.println("from xml");
//        printDDSet(servers);
        return servers;
    }

    private Set<DatasetDescriptor> extractMctableServers(final String path) {
        List<AddeServer> mctableServers = arrList();

        try {
            InputStream is = IOUtil.getInputStream(path);
            BufferedReader reader = 
                new BufferedReader(new InputStreamReader(is));
            String line;

            // maps an IP to a set of aliases
            Map<String, Set<String>> hosts = newMap();
            Map<String, String> hostToIp = newMap();
            Map<String, String> datasetToHost = newMap();

            Set<String> blah = newHashSet();
            blah.add("LOCAL-DATA");
            hosts.put("LOCAL-DATA", blah);
            hostToIp.put("LOCAL-DATA", "LOCAL-DATA");

            while ((line = reader.readLine()) != null) {
                routeMatcher.reset(line);
                hostMatcher.reset(line);
                if (routeMatcher.find()) {
                    String dataset = routeMatcher.group(1);
                    String host = routeMatcher.group(2);
                    datasetToHost.put(dataset, host);
                } else if (hostMatcher.find()) {
                    String name = hostMatcher.group(1);
                    String ip = hostMatcher.group(2);

                    Set<String> nameSet = hosts.get(ip);
                    if (nameSet == null)
                        nameSet = newHashSet();

                    nameSet.add(name);
                    hosts.put(ip, nameSet);

                    hostToIp.put(name, ip);
                    hostToIp.put(ip, ip);
                }
            }

            // rework these
            Map<String, String> datasetsToIp = 
                mapDatasetsToIp(datasetToHost, hostToIp);

            Map<String, String> ipToName = 
                mapIpToName(hosts);

            mctableServers.addAll(
                AddeServer.coalesce(
                    mapDatasetsToName(datasetsToIp, ipToName)));

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
        return serversToDescriptors("mctable", mctableServers);
    }

    private List<AddeServer> mapDatasetsToName(final Map<String, String> datasets, final Map<String, String> names) {
        List<AddeServer> servers = arrList();
        for(Entry<String, String> entry : datasets.entrySet()) {
            String dataset = entry.getKey();
            String ip = entry.getValue();
            String name = ip;
            if (names.containsKey(ip))
                name = names.get(ip);

            AddeServer server = new AddeServer(name);
            server.addGroup(new Group("any", dataset, dataset));
            servers.add(server);
        }
        return servers;
    }

    private Map<String, String> mapIpToName(final Map<String, Set<String>> map) {
        assert map != null;

        Map<String, String> ipToName = newMap();
        for (Entry<String, Set<String>> entry : map.entrySet()) {
            Set<String> names = entry.getValue();
            String displayName = "";
            for (String name : names)
                if (name.length() >= displayName.length())
                    displayName = name;

            if (displayName.equals(""))
                displayName = entry.getKey();

            ipToName.put(entry.getKey(), displayName);
        }
        return ipToName;
    }

    private Map<String, String> mapDatasetsToIp(final Map<String, String> datasets, 
        final Map<String, String> hostMap) 
    {
        assert datasets != null;
        assert hostMap != null;

        Map<String, String> datasetToIp = newMap();
        for (Entry<String, String> entry : datasets.entrySet()) {
            String dataset = entry.getKey();
            String alias = entry.getValue();
            if (hostMap.containsKey(alias))
                datasetToIp.put(dataset, hostMap.get(alias));
        }
        return datasetToIp;
    }

    private Set<DatasetDescriptor> getServerSet() {
        // holy crap does this need some work
        Set<DatasetDescriptor> servers = newLinkedHashSet();

        Set<DatasetDescriptor> defaultServers = getDefaultServers();
        Set<DatasetDescriptor> siteServers = getSiteServers();
        Set<DatasetDescriptor> mctableServers = getMctableServers();
        Set<DatasetDescriptor> userServers = getUserServers();
        userServers.addAll(getOldStyleUserServers());
        userServers.addAll(getLocalServers());

        if (sourceToData.containsKey("user")) {
            servers.addAll(applyPersisted(sourceToData.get("user"), userServers));
        } else {
            servers.addAll(userServers);
        }

        if (sourceToData.containsKey("default")) {
            servers.addAll(applyPersisted(sourceToData.get("default"), defaultServers));
        } else {
            servers.addAll(defaultServers);
        }

        if (sourceToData.containsKey("site")) {
            servers.addAll(applyPersisted(sourceToData.get("site"), siteServers));
        } else {
            servers.addAll(siteServers);
        }

        if (sourceToData.containsKey("mctable")) {
            servers.addAll(applyPersisted(sourceToData.get("mctable"), mctableServers));
        } else {
            servers.addAll(mctableServers);
        }

        return servers;
    }

    private Set<DatasetDescriptor> serversToDescriptors(final String source,
        final List<AddeServer> addeServers)
    {
        assert addeServers != null;
        assert source != null;
        Set<DatasetDescriptor> datasets = newLinkedHashSet();
        for (AddeServer addeServer : addeServers)
            for (Group group : (List<Group>)addeServer.getGroups())
                datasets.add(new DatasetDescriptor(addeServer, group, source, "", ""));
        return datasets;
    }

    private Set<DatasetDescriptor> applyPersisted(final Set<DatasetDescriptor> persisted, final Set<DatasetDescriptor> raw) {
        assert persisted != null;
        assert raw != null;

        Set<DatasetDescriptor> servers = newLinkedHashSet();
        servers.addAll(persisted);
        servers.addAll(raw);

        return servers;
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

//        if (si == null) {
//            si = new ServerInfo(getIdv(), serversXRC);
//        }
        return true;
    }

    // -2 = user stopped entering accounting info?
    // -1 = no datasets returned or getDatasetList failed.
    // 0 = it worked?
    // this needs to be reworked.
    protected int checkServer(String server, String type, String group, String user, String proj) {
        String[] servers = { server };
        AddeServerInfo asi = new AddeServerInfo(servers);
        asi.setUserIDandProjString("user=" + user + "&proj=" + proj);
        int stat = asi.setSelectedServer(server, type.toUpperCase());

        if (!server.equals(lastServerChecked)) {
            lastServerChecked = server;
            while (stat == -1) {
                user = "";
                proj = "";
                // user cancelled out of setting up accounting info
                if (!setUserProj()) {
                    user = DEFAULT_USER;
                    proj = DEFAULT_PROJ;
                    return -2;
                }
                asi.setUserIDandProjString("user=" + user + "&proj=" + proj);
                stat = asi.setSelectedServer(server , type.toUpperCase());
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

    /**
     * Verifies that a given {@link DatasetDescriptor} correctly describes a
     * dataset. Verified datasets specify a {@literal "live"} ADDE server, 
     * correct username and project number, and a valid group on the server.
     * 
     * @param descriptor Dataset that needs verification.
     * 
     * @return One of {@link AddeStatus}.
     * 
     * @throws NullPointerException if {@code descriptor} is null.
     * 
     * @see DatasetDescriptor
     * @see AddeStatus
     * @see AddeServerInfo
     */
    public static AddeStatus checkDescriptor(final DatasetDescriptor descriptor) {
        if (descriptor == null)
            throw new NullPointerException("Dataset Descriptor cannot be null");

        String server = descriptor.getServerName();
        String type = descriptor.getType().toUpperCase();
        String username = descriptor.getUser();
        String project = descriptor.getProj();
        String[] servers = { server };
        AddeServerInfo serverInfo = new AddeServerInfo(servers);
        
        // I just want to go on the record here: 
        // AddeServerInfo#setUserIDAndProjString(String) was not a good API 
        // decision.
        serverInfo.setUserIDandProjString("user="+username+"&proj="+project);
        int status = serverInfo.setSelectedServer(server, type);
        if (status == -2)
            return AddeStatus.NO_METADATA;
        if (status == -1)
            return AddeStatus.BAD_ACCOUNTING;

        serverInfo.setSelectedGroup(descriptor.getGroup().getName());
        String[] datasets = serverInfo.getDatasetList();
        if (datasets != null && datasets.length > 0)
            return AddeStatus.OK;
        else
            return AddeStatus.BAD_GROUP;
    }

    public static Set<String> readPublicGroups(final DatasetDescriptor descriptor) {
        if (descriptor == null)
            throw new NullPointerException("descriptor cannot be null");
        if (descriptor.getServerName() == null)
            throw new NullPointerException();
        if (descriptor.getServerName().length() == 0)
            throw new IllegalArgumentException();

        String user = descriptor.getUser();
        if (user == null || user.length() == 0)
            user = DEFAULT_USER;

        String proj = descriptor.getProj();
        if (proj == null || proj.length() == 0)
            proj = DEFAULT_PROJ;

        String url = String.format(publicSrvFormat, descriptor.getServerName(), user, proj);

        Set<String> groups = newLinkedHashSet();

        AddeTextReader reader = new AddeTextReader(url);
        if (reader.getStatus().equals("OK"))
            for (String line : (List<String>)reader.getLinesOfText())
                groups.add(new AddeEntry(line).getGroup());

        return groups;
    }

    public Set<DatasetDescriptor> checkGroups(final Set<DatasetDescriptor> descriptors) {
        if (descriptors == null)
            throw new NullPointerException("descriptors cannot be null");

        CompletionService<DescriptorStatus> ecs = new ExecutorCompletionService<DescriptorStatus>(exec);
        for (DatasetDescriptor descriptor : descriptors) {
            DescriptorStatus pairing = new DescriptorStatus(descriptor);
            ecs.submit(new VerifyDescriptorTask(pairing));
        }

        Set<DatasetDescriptor> validGroups = newLinkedHashSet();

        try {
            for (int i = 0; i < descriptors.size(); i++) {
                DescriptorStatus pairing = ecs.take().get();
                if (pairing.getStatus() == AddeStatus.OK)
                    validGroups.add(pairing.getDescriptor());
                else
                    System.err.println(pairing.getDescriptor()+" is "+pairing.getStatus());
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted while checking descriptors: " + e);
        } catch (ExecutionException e) {
            System.err.println("Problem executing: " + e);
        } finally {
            exec.shutdown();
        }
        return validGroups;
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

    public Set<DatasetDescriptor> checkHosts(final Set<DatasetDescriptor> descriptors) {
        if (descriptors == null)
            throw new NullPointerException("descriptors cannot be null");

        Set<DatasetDescriptor> goodDescriptors = newLinkedHashSet();
        Set<String> checkedHosts = newLinkedHashSet();
        Map<String, Boolean> hostStatus = newMap();
        for (DatasetDescriptor descriptor : descriptors) {
            String host = descriptor.getServerName();
            if (hostStatus.get(host) == Boolean.FALSE) {
                continue;
            } else if (hostStatus.get(host) == Boolean.TRUE) {
                goodDescriptors.add(descriptor);
            } else {
                Socket socket = null;
                boolean connected = false;
                checkedHosts.add(host);
                try {
                    socket = new Socket(host, 112);
                    connected = true;
                } catch (UnknownHostException e) {
                    connected = false;
                } catch (IOException e) {
                    connected = false;
                }

                try {
                    socket.close();
                } catch (Exception e) { }

                if (connected) {
                    goodDescriptors.add(descriptor);
                    hostStatus.put(host, Boolean.TRUE);
                } else {
                    hostStatus.put(host, Boolean.FALSE);
                }
            }
        }
        return goodDescriptors;
    }
    
    // TODO(jon): improve this API
    public static class DatasetDescriptor {
        private final Set<String> aliases = newLinkedHashSet();
        private final AddeServer server;
        private final Group group;
        private boolean enabled = true;
        private static final Insets INSET = new Insets(0, 20, 0, 0);
        private final String source;
        private final String user;
        private final String proj;
        private Category category;
        
        public DatasetDescriptor(final AddeServer server, final Group group, 
            final String source, final String user, final String proj) 
        {
            if (server == null)
                throw new NullPointerException("");
            if (group == null)
                throw new NullPointerException("");
            if (source == null)
                throw new NullPointerException("");
            if (user == null)
                throw new NullPointerException("");
            if (proj == null)
                throw new NullPointerException("");
            this.server = server;
            this.group = group;
            this.source = source;
            this.user = user;
            this.proj = proj;
        }

        public Set<String> getAliases() {
            return aliases;
        }

        public boolean addAlias(final String alias) {
            if (alias == null)
                throw new NullPointerException();
            return aliases.add(alias);
        }

        public boolean addAliases(final Collection<String> moreAliases) {
            if (moreAliases == null)
                throw new NullPointerException();
            return aliases.addAll(moreAliases);
        }

//        public String getServerName() {
//            return server.getName();
//        }

//        public String getType() {
//            return group.getType();
//        }

        public String getServerName() {
            return server.getName().toLowerCase();
//            return server.getName();
        }

        public String getServerDescription() {
            return server.getDescription().toLowerCase();
        }

        public String getType() {
            return group.getType().toLowerCase();
//            return group.getType();
        }

        public String getName() {
            return group.getName();
        }

        public String getUser() {
            return user;
        }

        public String getProj() {
            return proj;
        }

        public AddeServer getServer() {
            return server;
        }

        public Group getGroup() {
            return group;
        }

        public boolean getEnabled() {
            return enabled;
        }

        public String getSource() {
            return source;
        }

        public void setEnabled(final boolean newValue) {
            enabled = newValue;
        }

        public void setCategory(final Category cat) {
            if (cat == null)
                throw new NullPointerException("");
            category = cat;
        }

        public String toPrefString() {
            return server.getName() + "/" + group.getName();
        }

        @Override public String toString() {
            return String.format(
                "[DatasetDescriptor@%s: server=%s, description=%s, group=%s, type=%s, enabled=%s, source=%s, user=%s, proj=%s]", 
                Integer.toHexString(hashCode()), server.getName(), 
                server.getDescription(), group.getName(), group.getType(), 
                enabled, source, user, proj);
        }

        @Override public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof DatasetDescriptor))
                return false;
            DatasetDescriptor other = (DatasetDescriptor)o;

            boolean group = other.getName().equals(getName());
            boolean server = other.getServerName().equals(getServerName());
            boolean desc = other.getServerDescription().equals(getServerDescription());
            boolean type = other.getType().equals(getType());

            boolean ret = group && server && desc && type;
            return ret;
        }

        @Override public int hashCode() {
            int result = 31337;
            result += 31 * result + getServerName().hashCode();
            result += 31 * result + getServerDescription().hashCode();
//            result += 31 * result + server.getName().hashCode();
            result += 31 * result + group.getName().hashCode();
            result += 31 * result + getType().hashCode();
            return result;
        }

        public JPanel gooify() {
            final DatasetDescriptor descriptor = this;
            final JCheckBox checkbox = new JCheckBox("", getEnabled());
            checkbox.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    setEnabled(checkbox.isSelected());

                    if (category != null)
                        category.updateCheckbox();
                }
            });

            final JLabel label = new JLabel(toPrefString());
            label.addMouseListener(new MouseAdapter() {
                public void mouseClicked(final MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        System.err.println("edit: " + descriptor);
                    }
                }
            });

            return GuiUtils.inset(GuiUtils.hbox(Misc.newList(checkbox, label)), INSET);
        }
    }

    private static class AddeServerFilter extends Filter<DatasetDescriptor> {
        private final AddeServer server;
        public AddeServerFilter(final AddeServer server) {
            if (server == null)
                throw new NullPointerException();
            this.server = server;
        }
        public boolean matches(final DatasetDescriptor descriptor) {
//            System.err.print("comparing " + descriptor.getServer().getName() + " against " + server.getName());
            boolean b = descriptor.getServer().getName().equals(server.getName());
//            return descriptor.getServer().equals(server);
//            System.err.println(" val=" + b);
            return b;
        }
    }

    private static class GroupTypeFilter extends Filter<DatasetDescriptor> {
        private final String typeName;
        public GroupTypeFilter(final String typeName) {
            if (typeName == null)
                throw new NullPointerException();
            this.typeName = typeName;
        }
        public boolean matches(final DatasetDescriptor descriptor) {
            boolean b = descriptor.getType().toLowerCase().equals(typeName);
//            System.err.println("group type filter " + b + "=" + descriptor);
            return b;
        }
    }

    private static class EnabledDatasetFilter extends Filter<DatasetDescriptor> {
        public boolean matches(final DatasetDescriptor descriptor) {
            return descriptor.getEnabled();
        }
    }

    private static class DisabledDatasetFilter extends Filter<DatasetDescriptor> {
        public boolean matches(final DatasetDescriptor descriptor) {
            return !descriptor.getEnabled();
        }
    }

    private static class InvisibleFilter extends Filter<DatasetDescriptor> {
        private IdvObjectStore store;
        public InvisibleFilter(final IdvObjectStore store) {
            this.store = store;
        }
        public boolean matches(final DatasetDescriptor descriptor) {
            String source = descriptor.getSource();
            String prop;
            // this is awful :(
            if (source.equals("site"))
                prop = PREF_LIST_SITE_SERV;
            else if (source.equals("default"))
                prop = PREF_LIST_DEFAULT_SERV;
            else if (source.equals("mctable"))
                prop = PREF_LIST_MCTABLE_SERV;
            else if (source.equals("user"))
                prop = PREF_LIST_USER_SERV;
            else {
                System.err.println(source + " is unknown!!");
                System.err.println(descriptor);
                return false;
            }

            boolean b = store.get(prop, false);
//            if (source.equals("user")) {
//                System.err.println("returning " + b + " for " + descriptor);
//            }
            return b;
        }
    }

    public static class Category {
        private static ImageIcon expandedIcon;
        private static ImageIcon closedIcon;

        private static final EnabledDatasetFilter enabledFilter = 
            new EnabledDatasetFilter();
        private static final DisabledDatasetFilter disabledFilter = 
            new DisabledDatasetFilter();
        
        private JCheckBox checkbox;

        static {
            expandedIcon = new ImageIcon(Resource.getImage("/auxdata/ui/icons/CategoryOpen.gif"));
            closedIcon = new ImageIcon(Resource.getImage("/auxdata/ui/icons/CategoryClosed.gif"));
        }
        private final IntegratedDataViewer idv;
        private final Set<DatasetDescriptor> items;
        private boolean expanded = false;
        private final String categoryName;
        private final String expandedProp;
        private ServerPreferenceManager manager;

        public Category(final IntegratedDataViewer idv, 
            final ServerPreferenceManager manager, final String categoryName) 
        {
            this(idv, manager, categoryName, Collections.EMPTY_SET);
        }

        public Category(final IntegratedDataViewer idv, 
            final ServerPreferenceManager manager, final String categoryName, 
            final Set<DatasetDescriptor> items) 
        {
            if (idv == null)
                throw new NullPointerException();
            if (manager == null)
                throw new NullPointerException();
            if (categoryName == null)
                throw new NullPointerException();
            if (items == null)
                throw new NullPointerException();

            this.idv = idv;
            this.manager = manager;
            this.categoryName = categoryName;
            this.items = newLinkedHashSet(items);
            this.expandedProp = "mcv.servers.category." + categoryName;

            expanded = idv.getObjectStore().get(expandedProp, false);

            for (DatasetDescriptor descriptor : items)
                descriptor.setCategory(this);
        }

        public boolean addDescriptors(final Set<DatasetDescriptor> newItems) {
            if (newItems == null)
                throw new NullPointerException();

            for (DatasetDescriptor descriptor : newItems)
                descriptor.setCategory(this);

            return items.addAll(newItems);
        }

        public Set<DatasetDescriptor> getAllDescriptors() {
            return newLinkedHashSet(items);
        }

        public Set<DatasetDescriptor> getEnabledDescriptors() {
            Set<DatasetDescriptor> enabled = filter(enabledFilter, items);
            return enabled;
        }

        public Set<DatasetDescriptor> getDisabledDescriptors() {
            Set<DatasetDescriptor> disabled = filter(disabledFilter, items);
            return disabled;
        }

        public boolean replaceDescriptors(final Set<DatasetDescriptor> newItems) {
            if (newItems == null)
                throw new NullPointerException();
            items.clear();
            return items.addAll(newItems);
        }

        public void updateCheckbox() {
            if (checkbox == null)
                return;

            boolean val = any(enabledFilter, getAllDescriptors());
            checkbox.setSelected(val);
            manager.updateManagedChoosers();
        }
        
        public void setCategorySelected(final boolean selected) {
            for (DatasetDescriptor descriptor : getAllDescriptors())
                descriptor.setEnabled(selected);
            
            manager.categoryChanged(this);
        }

        public List<JPanel> buildEntry() {
            Set<DatasetDescriptor> descriptors = getAllDescriptors();
            ImageIcon icon = (expanded) ? expandedIcon : closedIcon;

            final JPanel itemPanel = new JPanel(new GridLayout(0, 1, 0, 0));

            for (DatasetDescriptor descriptor : descriptors)
                itemPanel.add(descriptor.gooify());

            final JToggleButton expando = new JToggleButton(icon, expanded);
            expando.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    expanded = expando.isSelected();
                    ImageIcon icon = (expanded) ? expandedIcon : closedIcon;
                    expando.setIcon(icon);
                    itemPanel.setVisible(expanded);
                    idv.getObjectStore().put(expandedProp, expanded);
                }
            });

            boolean checked = any(enabledFilter, descriptors);
            checkbox = new JCheckBox(categoryName, checked);
            checkbox.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    setCategorySelected(checkbox.isSelected());
                }
            });
            itemPanel.setVisible(expanded);
            return list(GuiUtils.hbox(Misc.newList(expando, checkbox)), itemPanel);
        }
    }

    public static class ServerPropertyDialog extends JDialog implements ActionListener {
        private JCheckBox typeImage = new JCheckBox("Image", false);
        private JCheckBox typePoint = new JCheckBox("Point", false);
        private JCheckBox typeGrid = new JCheckBox("Grid", false);
        private JCheckBox typeText = new JCheckBox("Text", false);
        private JCheckBox typeNav = new JCheckBox("Navigation", false);
        private JCheckBox typeRadar = new JCheckBox("Radar", false);
        
        private JCheckBox enableAccounting = new JCheckBox("Need accounting information", false);

        private JLabel labelServer = new JLabel("Server:");
        private JLabel labelGroup = new JLabel("Group(s):");
        private JLabel labelUser = new JLabel("User ID:");
        private JLabel labelProj = new JLabel("Project #");

        private JTextField textServer = new JTextField("", 30);
        private JTextField textGroup = new JTextField("", 30);
        private JTextField textUser = new JTextField("", 30);
        private JTextField textProj = new JTextField("", 30);

        private JPanel buttonRow = makeButtonRow(this);

        private List<JComponent> checkboxes = arrList();
        private List<JComponent> textfields = arrList();

        private JFrame frame;

        private String serverName = "super test";

        private boolean hitApply = false;

        private ServerPreferenceManager serverManager;

        public enum Types { IMAGE, POINT, GRID, TEXT, NAVIGATION, RADAR };

        private Set<DatasetDescriptor> addedDescriptors = newLinkedHashSet();

        public ServerPropertyDialog(final JFrame frame, final boolean modal, final ServerPreferenceManager serverManager) {
            super(frame, modal);
            this.frame = frame;
            this.serverManager = serverManager;
            
            enableAccounting.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    textUser.setEnabled(enableAccounting.isSelected());
                    textProj.setEnabled(enableAccounting.isSelected());
                }
            });
        }

        // note that both server and group are allowed to be null
        public void showDialog(final String server, final String group, final Set<Types> defaultTypes) {

            // be safe and clear out the added descriptors
            addedDescriptors.clear();

            if (server != null)
                textServer.setText(server);
            if (group != null)
                textGroup.setText(group);

            textUser.setEnabled(enableAccounting.isSelected());
            textProj.setEnabled(enableAccounting.isSelected());

            if (defaultTypes.contains(Types.IMAGE))
                typeImage.setSelected(true);
            if (defaultTypes.contains(Types.POINT))
                typePoint.setSelected(true);
            if (defaultTypes.contains(Types.GRID))
                typeGrid.setSelected(true);
            if (defaultTypes.contains(Types.TEXT))
                typeText.setSelected(true);
            if (defaultTypes.contains(Types.NAVIGATION))
                typeNav.setSelected(true);
            if (defaultTypes.contains(Types.RADAR))
                typeRadar.setSelected(true);

            textfields.add(new JLabel(" "));
            textfields.add(GuiUtils.hbox(labelServer, textServer));
            textfields.add(new JLabel(" "));
            textfields.add(GuiUtils.hbox(labelGroup, textGroup));
            textfields.add(new JLabel(" "));
            textfields.add(enableAccounting);
            textfields.add(GuiUtils.hbox(labelUser, textUser));
            textfields.add(new JLabel(" "));
            textfields.add(GuiUtils.hbox(labelProj, textProj));
            textfields.add(new JLabel(" "));

            checkboxes.add(typeImage);
            checkboxes.add(typePoint);
            checkboxes.add(typeGrid);
            checkboxes.add(typeText);
            checkboxes.add(typeNav);
            checkboxes.add(typeRadar);

            JComponent fields = GuiUtils.center(GuiUtils.inset(GuiUtils.vbox(textfields), 20));
            JComponent types = GuiUtils.inset(GuiUtils.hbox(checkboxes, 5), 20);

            JPanel bottom = GuiUtils.inset(buttonRow, 5);
            JComponent contents = GuiUtils.topCenterBottom(fields, types, bottom);

            setContentPane(contents);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            if (frame != null)
                setLocationRelativeTo(frame);
            pack();
            setVisible(true);
        }

        public void actionPerformed(final ActionEvent e) {
//          System.err.println("action performed: " + e);

          String command = e.getActionCommand();
          if (command.equals(CMD_VERIFY)) {
//              System.err.println("SPM.actionPerformed: before verify " + Thread.currentThread().getName());
              verifyInput();
//              System.err.println("SPM.actionPerformed: after verify " + Thread.currentThread().getName());
          } else if (command.equals(CMD_VERIFYAPPLY)) {
//              System.err.println("SPM.actionPerformed: before applyverify " + Thread.currentThread().getName());
              verifyInput();
              addServer();
//              System.err.println("SPM.actionPerformed: after applyverify " + Thread.currentThread().getName());
          } else if (command.equals(GuiUtils.CMD_APPLY)) {
//              System.err.println("SPM.actionPerformed: before apply " + Thread.currentThread().getName());
              addServer();
//              System.err.println("SPM.actionPerformed: after apply " + Thread.currentThread().getName());
          } else if (command.equals(GuiUtils.CMD_CANCEL)) {
//              System.err.println("SPM.actionPerformed: before cancel " + Thread.currentThread().getName());
              cancel();
//              System.err.println("SPM.actionPerformed: after cancel " + Thread.currentThread().getName());
          } else {
//              System.err.println("ServerPropertiesDialog.actionPerformed(): unknown action");
              hitApply = false;
          }
      }

        public String getServerName() {
            return serverName;
        }

        public Set<DatasetDescriptor> getAddedDatasetDescriptors() {
            return addedDescriptors;
        }
        
        public boolean hitApply(final boolean resetValue) {
            boolean val = hitApply;
            if (resetValue)
                hitApply = false;
            return val;
        }

        public AddeServer getAddedServer() {

            List<AddeServer> servers = arrList();
            for (DatasetDescriptor dd : addedDescriptors) {
                AddeServer server = dd.getServer();
                servers.add(server);
            }

            List<AddeServer> merged = AddeServer.coalesce(servers);

            if (merged.isEmpty()) {
//                System.err.println("getAddedServer: no added servers?");
                return null;
            }
//            for (AddeServer s : merged) {
//                System.err.println("getAddedServer: " + s.getName() + " " + s.getGroups());
//            }
            return merged.get(0);
        }
        
        public void clearAddedDescriptors() {
            addedDescriptors.clear();
        }

        public String getUser() {
            if (enableAccounting.isSelected())
                return textUser.getText().trim();
            return DEFAULT_USER;
        }
        
        public String getProj() {
            if (enableAccounting.isSelected())
                return textProj.getText().trim();
            return DEFAULT_PROJ;
        }
        
        private Set<DatasetDescriptor> pollWidgets(final boolean ignoreCheckBoxes) {
            String newServer = textServer.getText().trim();
            String grp = textGroup.getText().trim();
            String username = DEFAULT_USER;
            String project = DEFAULT_PROJ;
            if (enableAccounting.isSelected()) {
                username = textUser.getText().trim();
                project= textProj.getText().trim();
            }

            Set<String> enabledTypes = newLinkedHashSet();
            if (!ignoreCheckBoxes) {
                if (typeImage.isSelected())
                    enabledTypes.add("image");
                if (typePoint.isSelected())
                    enabledTypes.add("point");
                if (typeGrid.isSelected())
                    enabledTypes.add("grid");
                if (typeText.isSelected())
                    enabledTypes.add("text");
                if (typeNav.isSelected())
                    enabledTypes.add("nav");
                if (typeRadar.isSelected())
                    enabledTypes.add("radar");
            } else {
                enabledTypes.addAll(set("image", "point", "grid", "text", "nav", "radar"));
            }

            StringTokenizer tok = new StringTokenizer(grp, ",");
            Set<String> newGroups = newLinkedHashSet();
            while (tok.hasMoreTokens())
                newGroups.add(tok.nextToken().trim());

            Set<DatasetDescriptor> descriptors = newLinkedHashSet();
            for (String newGroup : newGroups) {
                for (String type : enabledTypes) {
                    AddeServer addeServ = new AddeServer(newServer);
                    Group addeGroup = new Group(type, newGroup, newGroup);
                    addeServ.addGroup(addeGroup);
                    DatasetDescriptor dd = new DatasetDescriptor(addeServ, addeGroup, "user", username, project);
                    descriptors.add(dd);
                }
            }
            return descriptors;
        }

        // TODO(jon): add a more generic UI polling method for both addServer and verifyInput
        private void addServer() {
//            System.err.println("addServer: clicked");

            Set<DatasetDescriptor> descriptors = pollWidgets(false);
            Set<DatasetDescriptor> added = serverManager.addNewDescriptors(descriptors);
//            if (added.isEmpty()) {
//                System.err.println("addServer: Nothing added??");
//            }
            addedDescriptors.addAll(added);
            hitApply = true;
            dispose();
        }

        private void verifyInput() {
//            System.err.println("verifyInput: entered");
            Set<DatasetDescriptor> descriptors = pollWidgets(true);

            // TODO(jon): merge this up into the previous loop?
            Set<String> validTypes = newLinkedHashSet();
            for (DatasetDescriptor descriptor : descriptors) {
                String type = descriptor.getType();
                if (validTypes.contains(type))
                    continue;

                AddeStatus status = serverManager.checkDescriptor(descriptor);
                if (status == AddeStatus.BAD_SERVER) {
//                    System.err.println("since server is bad, quitting");
                    return;
                } else if (status == AddeStatus.OK) {
                    validTypes.add(type);
                }
            }

            typeImage.setSelected(validTypes.contains("image"));
            typePoint.setSelected(validTypes.contains("point"));
            typeGrid.setSelected(validTypes.contains("grid"));
            typeText.setSelected(validTypes.contains("text"));
            typeNav.setSelected(validTypes.contains("nav"));
            typeRadar.setSelected(validTypes.contains("radar"));
//            System.err.println("verifyInput: leaving!");
        }

        private void cancel() {
//            System.err.println("cancel: clicked, disposing");
            hitApply = false;
            dispose();
        }

        /**
         * Utility to make verify/apply/cancel button panel
         *
         * @param l The listener to add to the buttons
         * @return The button panel
         */
        private static JPanel makeButtonRow(final ActionListener listener) {
            assert listener != null;
            return GuiUtils.makeButtons(listener, 
                new String[] { "Verify and Apply", "Verify", "Apply", "Cancel" },
                new String[] { CMD_VERIFYAPPLY, CMD_VERIFY, GuiUtils.CMD_APPLY,
                               GuiUtils.CMD_CANCEL });
        }
    }

    
    private static class DescriptorStatus {
        private AddeStatus status;
        private final DatasetDescriptor descriptor;

        public DescriptorStatus(final DatasetDescriptor descriptor) {
            if (descriptor == null)
                throw new NullPointerException("cannot create a descriptor/status pair with a null descriptor");
            this.descriptor = descriptor;
        }

        public void setStatus(AddeStatus status) {
            this.status = status;
        }

        public AddeStatus getStatus() {
            return status;
        }

        public DatasetDescriptor getDescriptor() {
            return descriptor;
        }
    }

    private static class VerifyDescriptorTask implements Callable<DescriptorStatus> {
        private final DescriptorStatus descStatus;
        public VerifyDescriptorTask(final DescriptorStatus descStatus) {
            if (descStatus == null)
                throw new NullPointerException("cannot verify or set status of a null descriptor/status pair");
            this.descStatus = descStatus;
        }

        public DescriptorStatus call() throws Exception {
            descStatus.setStatus(checkDescriptor(descStatus.getDescriptor()));
            return descStatus;
        }
    }
    
}
