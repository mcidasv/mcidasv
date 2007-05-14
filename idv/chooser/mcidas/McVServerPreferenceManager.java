package ucar.unidata.idv.chooser.mcidas;

import edu.wisc.ssec.mcidas.adde.AddeServerInfo;

import org.w3c.dom.Element;

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
import ucar.unidata.xml.XmlUtil;

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

    private PreferenceManager serversManager = null;
    private JPanel serversPanel = null;

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


    /**
     * Add in the user preference tab for the choosers to show.
     */
    protected void getServerPreferences() {

        cbxToServerMap = new Hashtable();
        List       servList           = new ArrayList();
        final XmlResourceCollection serversXRC = getServers();
        List       allServers = getAllServerNames(serversXRC);
        final List catPanels          = new ArrayList();
        Hashtable  catMap             = new Hashtable();
        for (int i = 0; i < allServers.size(); i++) {
            TwoFacedObject tfo = (TwoFacedObject) allServers.get(i);
            String typeString = (String) tfo.getLabel();
            String nameString = (String) tfo.getId();
            CheckboxCategoryPanel catPanel =
                (CheckboxCategoryPanel) catMap.get(typeString);
            if (catPanel == null) {
                catPanel = new CheckboxCategoryPanel(typeString, false);
                catPanels.add(catPanel);
                catMap.put(typeString, catPanel);
                servList.add(catPanel.getTopPanel());
                servList.add(catPanel);
            }

            JCheckBox cbx = new JCheckBox(nameString, true);
                                          //shouldShowControl(typeString, true));
            cbxToServerMap.put(cbx, tfo);
            catPanel.addItem(cbx);
            catPanel.add(GuiUtils.inset(cbx, new Insets(0, 20, 0, 0)));
            //            servList.add(cb);
        }

        for (int i = 0; i < catPanels.size(); i++) {
            ((CheckboxCategoryPanel) catPanels.get(i)).checkVisCbx();
        }

        final JButton allOn = new JButton("All on");
        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (int i = 0; i < catPanels.size(); i++) {
                    ((CheckboxCategoryPanel) catPanels.get(i)).toggleAll(
                        true);
                }
            }
        });
        final JButton allOff = new JButton("All off");
        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (int i = 0; i < catPanels.size(); i++) {
                    ((CheckboxCategoryPanel) catPanels.get(i)).toggleAll(
                        false);
                }
            }
        });

        Boolean serversAll =
            (Boolean) getIdv().getPreference(PROP_SERVERS_ALL,
                                             Boolean.TRUE);
        final JRadioButton useAllBtn = new JRadioButton("Use all servers",
                                           serversAll.booleanValue());
        final JRadioButton useTheseBtn =
            new JRadioButton("Use selected servers:",
                             !serversAll.booleanValue());
        GuiUtils.buttonGroup(useAllBtn, useTheseBtn);

        //final JPanel servPanel    = GuiUtils.vbox(servList);
        final JPanel servPanel = GuiUtils.doLayout(new JPanel(), 
             GuiUtils.getComponentArray(servList), 1, GuiUtils.WT_Y, GuiUtils.WT_Y);
        JScrollPane  servScroller = new JScrollPane(servPanel);
        servScroller.getVerticalScrollBar().setUnitIncrement(10);
        servScroller.setPreferredSize(new Dimension(300, 300));

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if (cmd.equals("import")) {
                    getServersFromMctable();
                } else if (cmd.equals("export")) {
                    exportServersToPlugin();
                }
            }
        };
        String[] labels = {"Import from MCTABLE", "Export to Plugin"};
        String[] cmds = {"import", "export"};
        //String[] cmds = {"getServersFromMctable", "exportServersToPlugin"};
        JComponent exportImportServers =
            GuiUtils.right(GuiUtils.makeButtons(listener, labels, cmds));

        JComponent servComp = GuiUtils.centerBottom(servScroller, exportImportServers);

        JPanel bottomPanel =
            GuiUtils.leftCenter(
                GuiUtils.inset(
                    GuiUtils.top(GuiUtils.vbox(allOn, allOff)),
                    4), new Msg.SkipPanel(
                        GuiUtils.hgrid(
                            Misc.newList(servComp, GuiUtils.filler()), 0)));

        serversPanel =
            GuiUtils.inset(GuiUtils.topCenter(GuiUtils.hbox(useAllBtn,
                useTheseBtn), bottomPanel), 6);
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
                serversToShow = new Hashtable();
                Hashtable table         = (Hashtable) data;
                List allServers = getAllServerNames(serversXRC);
                for (Enumeration keys =
                        table.keys(); keys.hasMoreElements(); ) {
                    JCheckBox         cbx = (JCheckBox) keys.nextElement();
                    TwoFacedObject tfo = 
                        (TwoFacedObject) table.get(cbx);
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
     * Export the selected servers to the plugin manager
     */
    public void getServersFromMctable() {
        JFileChooser chooser = new JFileChooser();
        chooser.showOpenDialog(null);
        File file = chooser.getSelectedFile();
        if (file == null) {
            return;
        }
        //List groups = new ArrayList();
        //List servers = new ArrayList();
        List serversGroups = new ArrayList();
        StringTokenizer tok;
        String next;
        try {
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
                        //servers.add(server);
                    }
                } else if (lineType.equals("ADDE")) {
                    if (next.equals("ROUTE")) {
                        next = tok.nextToken();
                        tokTwo = new StringTokenizer(next,"=");
                        next = tokTwo.nextToken();
                        serv = tokTwo.nextToken();
                        if (!serv.equals("LOCAL-DATA")) {
                            //groups.add(next);
                            tfo = new TwoFacedObject((Object)next, (Object)serv);
                            serversGroups.add(tfo);
                        }
                    }
                }
                lineOut = reader.readLine();
            } 
        } catch (Exception e) {
            System.out.println("getServersFromMctable e=" + e);
            return;
        }

        String serv;
        String grp;
        System.out.println(" ");
        System.out.println("number of tfos = " + serversGroups.size());
        for (int i=0; i<serversGroups.size(); i++) {
            TwoFacedObject tfo = (TwoFacedObject)serversGroups.get(i);
            serv = (String)tfo.getId();
            grp = (String)tfo.getLabel();
            System.out.println("   " + i + ": server=" + serv + " group=" + grp);
        }

/*
        System.out.println(" ");
        System.out.println("stat=" + stat);
        System.out.println("groups from AddeServerInfo getGroupList:");
        String[] groupList = asi.getGroupList();
        for (int i=0; i<groupList.length; i++) {
            System.out.println(groupList[i]);
        }
*/
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


    /**
     * _more_
     *
     */
    private List getAllServerNames(XmlResourceCollection servers) {

        String TAG_TYPE = "type";
        String TAG_SERVER = "server";
        String ATTR_NAME = "name";

        List typesList = new ArrayList();
        List serverList = new ArrayList();
        for (int resourceIdx = 0; resourceIdx < servers.size();
                resourceIdx++) {
            Element root = servers.getRoot(resourceIdx);
            if (root == null) {
                continue;
            }
            List typeElements = XmlUtil.getElements(root, TAG_TYPE);
            for (int typeIdx = 0; typeIdx < typeElements.size(); typeIdx++) {
                Element typeElement = (Element) typeElements.get(typeIdx);
                String typeName = XmlUtil.getAttribute(typeElement, ATTR_NAME);
                List serverElements = XmlUtil.getElements(typeElement, TAG_SERVER);
                for (int serverIdx = 0; serverIdx < serverElements.size(); serverIdx++) {
                    Element serverElement = (Element) serverElements.get(serverIdx);
                    String name = XmlUtil.getAttribute(serverElement, ATTR_NAME);
                    TwoFacedObject tfo = new TwoFacedObject(typeName, name);
                    serverList.add(tfo);
                }
            }
        }
        return serverList;
    }
}

