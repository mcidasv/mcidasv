package ucar.unidata.idv.chooser.mcidas;

import edu.wisc.ssec.mcidas.adde.AddeServerInfo;

import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.ui.CheckboxCategoryPanel;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;

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


public class McVServerPreferenceManager extends IdvManager implements ActionListener {


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


    /** Shows the status */
    private JLabel statusLabel;

    /** _more_          */
    private JComponent statusComp;

    private PreferenceManager serversManager = null;
    private JPanel serversPanel = null;

    private ServerInfo si;

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

    /** Install server and group name flds */
    private JTextField serverFld;
    private JTextField groupFld;

    private final XmlResourceCollection serversXRC = getServers();

    /**
     * Create the dialog with the given idv
     *
     * @param idv The IDV
     *
     */
    public McVServerPreferenceManager(IntegratedDataViewer idv) {
        super(idv);
    }


    public void addServerPreferences(IdvPreferenceManager ipm) {
        getServerPreferences();
        ipm.add("Available Servers",
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
        statusLabel.paintImmediately(0,0,400,30);
    }


    /**
     * Add in the user preference tab for the servers to show.
     */
    protected void getServerPreferences() {
        cbxToServerMap = new Hashtable();
        List servList = new ArrayList();
        si = new ServerInfo(getIdv(), serversXRC);
        final List catPanels          = new ArrayList();
        Hashtable  catMap             = new Hashtable();
        List types = si.getServerTypes();
        String typeString;
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
            List servers = si.getServers(typeString);
            if (servers.size() > 0) {
                for (int j=0; j<servers.size(); j++) {
                    ServerDescriptor sd = (ServerDescriptor)servers.get(j);
                    JCheckBox cbx = new JCheckBox(sd.toString(), sd.getIsActive());
                    cbxToServerMap.put(cbx, sd);
                    catPanel.addItem(cbx);
                    catPanel.add(GuiUtils.inset(cbx, new Insets(0, 20, 0, 0)));
                }
            }
        }

        for (int i = 0; i < catPanels.size(); i++) {
            ((CheckboxCategoryPanel) catPanels.get(i)).checkVisCbx();
        }

        List comps = new ArrayList();
        final JButton allOn = new JButton("All on");
        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (int i = 0; i < catPanels.size(); i++) {
                    ((CheckboxCategoryPanel) catPanels.get(i)).toggleAll(
                        true);
                }
            }
        });
        comps.add(allOn);
        final JButton allOff = new JButton("All off");
        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (int i = 0; i < catPanels.size(); i++) {
                    ((CheckboxCategoryPanel) catPanels.get(i)).toggleAll(
                        false);
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

        Boolean serversAll =
            (Boolean) getIdv().getPreference(PROP_SERVERS_ALL,
                                             Boolean.TRUE);
        final JRadioButton useAllBtn = new JRadioButton("Use all servers",
                                           serversAll.booleanValue());
        final JRadioButton useTheseBtn =
            new JRadioButton("Use selected servers:",
                             !serversAll.booleanValue());
        GuiUtils.buttonGroup(useAllBtn, useTheseBtn);

        final JPanel servPanel = GuiUtils.doLayout(new JPanel(), 
             GuiUtils.getComponentArray(servList), 1, GuiUtils.WT_Y, GuiUtils.WT_Y);
        JScrollPane  servScroller = new JScrollPane(servPanel);
        servScroller.getVerticalScrollBar().setUnitIncrement(10);
        servScroller.setPreferredSize(new Dimension(300, 300));

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if (cmd.equals("import")) {
                    showWaitCursor();
                    List tfos = getServersFromMctable();
                    showNormalCursor();
                } else if (cmd.equals("export")) {
                    exportServersToPlugin();
                }
            }
        };
        String[] labels = {"Import from MCTABLE", "Export to Plugin"};
        String[] cmds = {"import", "export"};
        JComponent exportImportServers =
            GuiUtils.right(GuiUtils.makeButtons(listener, labels, cmds));

        JComponent servComp = GuiUtils.centerBottom(servScroller, exportImportServers);

        JPanel bottomPanel =
            GuiUtils.leftCenter(
                GuiUtils.inset(
                    GuiUtils.top(GuiUtils.vbox(comps)),
                    4), new Msg.SkipPanel(
                        GuiUtils.hgrid(
                            Misc.newList(servComp, GuiUtils.filler()), 0)));

        serversPanel =
            GuiUtils.inset(GuiUtils.topCenterBottom(GuiUtils.vbox(new JLabel(" "),
                GuiUtils.hbox(GuiUtils.rLabel("Status: "),getStatusComponent()), new JLabel(" ")),
                GuiUtils.hbox(useAllBtn, useTheseBtn),
                bottomPanel), 6);
        GuiUtils.enableTree(servPanel, !useAllBtn.isSelected());
        useAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(servPanel, !useAllBtn.isSelected());
                allOn.setEnabled( !useAllBtn.isSelected());
                allOff.setEnabled( !useAllBtn.isSelected());
            }
        });
        useTheseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(servPanel, !useAllBtn.isSelected());
                allOn.setEnabled( !useAllBtn.isSelected());
                allOff.setEnabled( !useAllBtn.isSelected());
            }
        });

        GuiUtils.enableTree(servPanel, !useAllBtn.isSelected());
        allOn.setEnabled( !useAllBtn.isSelected());
        allOff.setEnabled( !useAllBtn.isSelected());

        serversManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
		System.out.println("McVServerPreferenceManager applyPreference:");
                serversToShow = new Hashtable();
                Hashtable table         = (Hashtable) data;
                for (Enumeration keys =
                        table.keys(); keys.hasMoreElements(); ) {
                    JCheckBox         cbx = (JCheckBox) keys.nextElement();
                    System.out.println("   " + cbx.getLabel());
                    TwoFacedObject tfo = 
                        (TwoFacedObject) table.get(cbx);
                    System.out.println("      " + tfo.getId() + " " + tfo.getLabel());
                    serversToShow.put(tfo.getId(),
                            new Boolean(cbx.isSelected()));
                }
                showAllServers = useAllBtn.isSelected();
                theStore.put(PROP_SERVERS,
                             serversToShow);
                theStore.put(PROP_SERVERS_ALL,
                             new Boolean(showAllServers));
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
                    if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_APPLY)) {
                        String newServer = serverFld.getText().trim();
                        String newGroup = groupFld.getText().trim();
                        List typeList = new ArrayList();
                        if (imageTypeCbx.isSelected()) typeList.add("image");
                        if (pointTypeCbx.isSelected()) typeList.add("point");
                        if (gridTypeCbx.isSelected()) typeList.add("grid");
                        if (textTypeCbx.isSelected()) typeList.add("text");
                        if (navTypeCbx.isSelected()) typeList.add("nav");
                        addNewServer(newServer, newGroup, typeList);
                        closeAddServer();
                    }
                    if (cmd.equals(GuiUtils.CMD_CANCEL)) {
                        addWindow.setVisible(false);
                        addWindow = null;
                    }
                }
            };


            JPanel bottom =
                GuiUtils.inset(GuiUtils.makeApplyCancelButtons(listener),5);
//                GuiUtils.inset(GuiUtils.wrap(GuiUtils.makeButton("Close",
//                    this, "closeAddServer")),2);
            JComponent contents = GuiUtils.topCenterBottom(nameComp, dataTypes, bottom);
            addWindow.getContentPane().add(contents);
            addWindow.pack();
            addWindow.setLocation(200, 200);
        }
        addWindow.setVisible(true);
        GuiUtils.toFront(addWindow);
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
     * Export the selected servers to the plugin manager
     */
    public List getServersFromMctable() {
        setStatus("Locate MCTABLE.TXT");
        List serversGroups = new ArrayList();
        JFileChooser chooser = new JFileChooser();
        chooser.showOpenDialog(null);
        File file = chooser.getSelectedFile();
        if (file == null) {
            return serversGroups;
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
            TwoFacedObject tfo = new TwoFacedObject();
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
                        for (int i=0; i<serversGroups.size(); i++) {
                            tfo = (TwoFacedObject)serversGroups.get(i);
                            serv = (String)tfo.getId();
                            if (serv.equals(server)) {
                                tfo.setId(next);
                                serversGroups.set(i,tfo);
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
                            tfo = new TwoFacedObject((Object)next, (Object)serv);
                            serversGroups.add(tfo);
                        }
                    }
                }
                lineOut = reader.readLine();
            } 
        } catch (Exception e) {
            System.out.println("getServersFromMctable e=" + e);
            return serversGroups;
        }

        setStatus("Please wait...");
        String serv;
        String grp;
        int num = serversGroups.size();
        if (num < 1) return serversGroups;

        String[] servers = new String[num];
        String[] groups = new String[num];
        for (int i=0; i<num; i++) {
            TwoFacedObject tfo = (TwoFacedObject)serversGroups.get(i);
            serv = (String)tfo.getId();
            grp = (String)tfo.getLabel();
            servers[i] = serv;
            groups[i] = grp;
        }

        int stat = 0;
        AddeServerInfo asi = new AddeServerInfo(servers);
        if (si == null)
            si = new ServerInfo(getIdv(), serversXRC);
        asi.setUserIDandProjString("user=" + si.getUser() + "&proj=" + si.getProj());
        for (int i=0; i<num; i++) {

            setStatus(servers[i] + "   Checking for image data...");
            stat = asi.setSelectedServer(servers[i],"IMAGE");
            if (stat == 0) {
                asi.setSelectedGroup(groups[i]);
                String[] datasets = asi.getDatasetList();
                try {
                    int jnum = datasets.length;
                    if (jnum > 0) {
                       servImage.add(servers[i]);
                       servImage.add(groups[i]);
                    }
                } catch (Exception e) {
                }
            }

            setStatus(servers[i] + "   Checking for point data...");
            stat = asi.setSelectedServer(servers[i],"POINT");
            if (stat == 0) {
                asi.setSelectedGroup(groups[i]);
                String[] datasets = asi.getDatasetList();
                try {
                    int jnum = datasets.length;
                    if (jnum > 0) {
                       servPoint.add(servers[i]);
                       servPoint.add(groups[i]);
                    }
                } catch (Exception e) {
                }
            }

            setStatus(servers[i] + "   Checking for grid data...");
            stat = asi.setSelectedServer(servers[i],"GRID");
            if (stat == 0) {
                asi.setSelectedGroup(groups[i]);
                String[] datasets = asi.getDatasetList();
                try {
                    int jnum = datasets.length;
                    if (jnum > 0) {
                       servGrid.add(servers[i]);
                       servGrid.add(groups[i]);
                    }
                } catch (Exception e) {
                }
            }

            setStatus(servers[i] + "   Checking for text data...");
            stat = asi.setSelectedServer(servers[i],"TEXT");
            if (stat == 0) {
                asi.setSelectedGroup(groups[i]);
                String[] datasets = asi.getDatasetList();
                try {
                    int jnum = datasets.length;
                    if (jnum > 0) {
                       servText.add(servers[i]);
                       servText.add(groups[i]);
                    }
                } catch (Exception e) {
                }
            }

            setStatus(servers[i] + "   Checking for navigation data...");
            stat = asi.setSelectedServer(servers[i],"NAV");
            if (stat == 0) {
                asi.setSelectedGroup(groups[i]);
                String[] datasets = asi.getDatasetList();
                try {
                    int jnum = datasets.length;
                    if (jnum > 0) {
                       servNav.add(servers[i]);
                       servNav.add(groups[i]);
                    }
                } catch (Exception e) {
                }
            }
        }
        writeXml();
        setStatus("Done");
        return serversGroups;
    }


    private void addNewServer(String newServer, String grp, List type) {
        showWaitCursor();
        StringTokenizer tok = new StringTokenizer(grp, ",");
        List newGroups = new ArrayList();
        while (tok.hasMoreTokens()) {
            newGroups.add(tok.nextToken().trim());
        }
        if (si == null)
            si = new ServerInfo(getIdv(), serversXRC);
        if (type != null) {
            for (int i=0; i<type.size(); i++) {
                String typeString =(String)type.get(i);
                List outList = new ArrayList();
                for (int j=0; j<newGroups.size(); j++) {
                    outList.add(newServer);
                    outList.add((String)newGroups.get(j));
                }
                si.addServers(typeString, outList);
            }
        }
        showNormalCursor();
    }


    private void writeXml() {
        if (si == null)
            si = new ServerInfo(getIdv(), serversXRC);
        si.addServers("image", servImage);
        si.addServers("point", servPoint);
        si.addServers("grid", servGrid);
        si.addServers("text", servText);
        si.addServers("nav", servNav);
    }


    /**
     * Export the selected servers to the plugin manager
     */
    public void exportServersToPlugin() {
        System.out.println("Exporting");
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
     * Get the xml resource collection that defines the servers xml
     *
     * @return server resources
     */
    protected XmlResourceCollection getServers() {
        XmlResourceCollection serverCollection =
           getIdv().getResourceManager().getXmlResources(
            IdvResourceManager.RSC_SERVERS);
        return serverCollection;
    }
}
