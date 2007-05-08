package ucar.unidata.idv.chooser.mcidas;


import org.w3c.dom.Element;

import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.ui.CheckboxCategoryPanel;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.Misc;

import ucar.unidata.util.Msg;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

//JDK1.4
import java.beans.*;
import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

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
        JComponent exportServers =
            GuiUtils.right(GuiUtils.makeButton("Export to Plugin", this,
                "exportServersToPlugin"));

        JComponent servComp = GuiUtils.centerBottom(servScroller, exportServers);

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
    public void exportServersToPlugin() {
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

