/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.mcidasv.chooser;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.chooser.adde.AddeChooser;
import edu.wisc.ssec.mcidasv.util.McVTextField;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import thredds.catalog.XMLEntityResolver;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.radar.TDSRadarDatasetCollection;
import ucar.nc2.units.DateUnit;
import ucar.unidata.data.radar.RadarQuery;
import ucar.unidata.geoloc.StationImpl;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.TimesChooser;
import ucar.unidata.metdata.NamedStation;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.util.DateSelection;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.DatedThing;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.Product;
import ucar.unidata.util.TwoFacedObject;

import visad.CommonUnit;
import visad.DateTime;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.TextColor;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

/**
 * This class is responsible for the {@literal "Remote Level II Radar"} 
 * chooser in McIDAS-V.
 */
public class TDSRadarChooser extends TimesChooser implements Constants {
    
    /** Loggging object. */
    private static final Logger logger = 
        LoggerFactory.getLogger(TDSRadarChooser.class);
        
    /** The collection */
    private TDSRadarDatasetCollection collection;
    
    /** The currently selected station */
    private NamedStation selectedStation;

    /** The currently selected level3 product */
    private String selectedProduct;

    /** Those urls we connect to */
    //"http://motherlode.ucar.edu:8080/thredds/radarServer/catalog.xml";
    private String serverUrl;

    ///** Each dataset collection URL */
    //"http://motherlode.ucar.edu:8080/thredds/radarServer/level2/idd/dataset.xml";
    //private String collectionUrl;

    /** Component to hold collections */
    private JComboBox collectionSelector;

    /** Component to hold product list */
    private JComboBox productComboBox;
    
    /** Level 3 panel that can be hidden */
    private JPanel productPanel;
    
    /** components that need a server for activation */
    private List compsThatNeedServer = new ArrayList();

    /** components that need a server for activation */
    private List level3CompsThatNeedServer = new ArrayList();

    /** persistent holder for catalog URLS */
    private PreferenceList urlListHandler;

    /** catalog URL holder */
    private JComboBox urlBox;

    /** ok flag */
    private boolean okToDoUrlListEvents = true;

    /** dataset list */
    private List datasetList;

    /** Command for connecting */
    protected static final String CMD_CONNECT = "cmd.connect";
    
    /** _more_          */
    private boolean isLevel3;
    
    /** _more_          */
    public static final String[] level3_ExName = { "NVW", "DPA" };
    
    /** Number of relative time steps to load */
    private int relativeTimes = 5;
    
    /**
     * Create the RadarChooser.
     *
     * @param mgr {@code IdvChooserManager}
     * @param root XML root that defines this chooser.
     *
     */
    public TDSRadarChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        
        loadButton = McVGuiUtils.makeImageTextButton(ICON_ACCEPT_SMALL, getLoadCommandName());
        loadButton.setActionCommand(getLoadCommandName());
        loadButton.addActionListener(this);
        
        cancelButton = McVGuiUtils.makeImageButton(ICON_CANCEL, "Cancel");
        cancelButton.setActionCommand(GuiUtils.CMD_CANCEL);
        cancelButton.addActionListener(this);
        cancelButton.setEnabled(false);
        
    }



    /**
     * Handle the update event. Just pass it through to the imageChooser
     */
    public void doUpdate() {
        if ((serverUrl == null) || (datasetList == null)
                || (datasetList.size() == 0) || (selectedProduct == null)) {
            if (urlBox != null) {
                setServer((String) urlBox.getSelectedItem());
            }
            return;
        }
        Misc.run(this, "stationOrProductChanged");
    }



    /**
     * Update the status of the gui
     */
    protected void updateStatus() {
        super.updateStatus();
        if (serverUrl == null) {
            setHaveData(false);
            setStatus("Please connect to the server");
        }
        else if (selectedStation == null) {
            setHaveData(false);
            setStatus("Please select a station", "stations");
        }
        else if (isLevel3 && (selectedProduct == null)) {
            setHaveData(false);
            setStatus("Please select a level 3 product", "products");
        }
        else {
            boolean haveTimesSelected;
            if (getDoAbsoluteTimes()) {
                haveTimesSelected = getSelectedAbsoluteTimes().size() > 0;
            } else {
                haveTimesSelected = true;
            }
            setHaveData(haveTimesSelected);
            if (haveTimesSelected) {
                setStatus("Press \"" + CMD_LOAD + "\" to load the selected radar data", "buttons");
            } else {
                setStatus("Please select times", "timepanel");
            }
        }
        GuiUtils.enableTree(loadButton, getHaveData());
        if (drivercbx != null) {
            drivercbx.setEnabled(anyTimeDrivers() && getHaveData());
        }
    }



    /**
     * Handle when there are newly selected stations
     *
     * @param stations list of newly selected stations
     */
    protected void newSelectedStations(List stations) {
        super.newSelectedStations(stations);
        if ((stations == null) || (stations.size() == 0)) {
            selectedStation = null;
        } else {
            NamedStation newStation = (NamedStation) stations.get(0);
            if (Misc.equals(newStation, selectedStation)) {
                return;
            }
            selectedStation = newStation;
        }
        Misc.run(TDSRadarChooser.this, "stationOrProductChanged");
    }


    /** A widget for the list of dataset descriptors */


    /** Flag to keep from infinite looping */
    private boolean ignoreProductChange = false;

    /** Selection label text */
    protected static final String LABEL_SELECT = " -- Select -- ";

    /**
     * _more_
     */
    protected void productChanged() {
        stationOrProductChanged();
        // updateStatus();
    }

    /**
     * Reset the descriptor stuff
     */
    private void resetProductBox() {
        ignoreProductChange = true;
        productComboBox.setSelectedItem(LABEL_SELECT);
        ignoreProductChange = false;
    }

    /**
     * Should we update on first display
     *
     * @return true
     */
    protected boolean shouldDoUpdateOnFirstDisplay() {
        return false;
    }

    /**
     * Set the server
     *
     * @param s the server URL
     */
    private void setServer(String s) {
        datasetList = new ArrayList();
        serverUrl = s;
        try {
            datasetList = getRadarCollections(serverUrl);
            GuiUtils.setListData(collectionSelector, datasetList);
            for (int i = 0; i < datasetList.size(); i++) {
                TwoFacedObject obj = (TwoFacedObject) datasetList.get(i);
                String tmpStr = (String) (obj.getLabel());
                if ((tmpStr).contains("NEXRAD Level II Radar from IDD")) {
                    collectionSelector.setSelectedIndex(i);
                }
            }
        } catch (Exception e) {
            GuiUtils.setListData(collectionSelector, new ArrayList());
        }
    }

    /**
     * Set the active collection
     *
     * @param s collection URL
     */
    private void setCollection(String s) {
        isLevel3 = false;
        GuiUtils.enableComponents(level3CompsThatNeedServer, false);
        productPanel.setVisible(false);
        GuiUtils.enableComponents(compsThatNeedServer, true);
        if (drivercbx != null) {
            drivercbx.setEnabled(anyTimeDrivers() && getHaveData());
        }
        setAbsoluteTimes(new ArrayList());
        selectedProduct = null;
        selectedStation = null;
        Misc.run(this, "initializeCollection", s);
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    private void setLevel3Collection(String s) {
        isLevel3 = true;
        GuiUtils.enableComponents(level3CompsThatNeedServer, true);
        productPanel.setVisible(true);
        GuiUtils.enableComponents(compsThatNeedServer, true);
        if (drivercbx != null) {
            drivercbx.setEnabled(anyTimeDrivers() && getHaveData());
        }
        setAbsoluteTimes(new ArrayList());
        selectedProduct = null;
        selectedStation = null;
        Misc.run(this, "initializeLevel3Collection", s);
    }

    /**
     * Add a component that needs to have a valid server
     *
     * @param comp  the component
     *
     * @return  the component
     */
    protected JComponent addServerComp(JComponent comp) {
        compsThatNeedServer.add(comp);
        return comp;
    }

    /**
     * Add a component that needs to have a valid server
     *
     * @param comp  the component
     *
     * @return  the component
     */
    protected JComponent addLevel3ServerComp(JComponent comp) {
        level3CompsThatNeedServer.add(comp);
        return comp;
    }

    /**
     * Get  the radar collections for  the given server URL
     *
     * @param radarServerURL  server URL
     *
     * @return  a map of the collection names to URL
     */
    private List getRadarCollections(String radarServerURL) {
        SAXBuilder        builder;
        Document          doc  = null;
        XMLEntityResolver jaxp = new XMLEntityResolver(true);
        builder = jaxp.getSAXBuilder();
        List<TwoFacedObject> collections = new ArrayList<>();

        try {
            doc = builder.build(radarServerURL);
        } catch (JDOMException e) {
            if (canShowErrorWindows()) {
                userMessage("Invalid catalog");
            }
            logger.warn("Invalid catalog: "+radarServerURL, e);
        } catch (IOException e) {
            if (canShowErrorWindows()) {
                userMessage("Unable to open catalog");
            }
            logger.warn("Unable to open catalog: "+radarServerURL, e);
        }

        org.jdom2.Element rootElem    = doc.getRootElement();
        org.jdom2.Element serviceElem = readElements(rootElem, "service");
        String           uriBase     = serviceElem.getAttributeValue("base");
        org.jdom2.Element dsElem      = readElements(rootElem, "dataset");
        String           naming      = "catalogRef";
        Namespace        nss         = rootElem.getNamespace("xlink");
        List             children    = dsElem.getChildren();
        for (int j = 0; j < children.size(); j++) {
            org.jdom2.Element child     = (org.jdom2.Element) children.get(j);
            String           childName = child.getName();
            if (childName.equals(naming)) {
                //String id   = child.getAttributeValue("ID");
                String desc    = child.getAttributeValue("title", nss);
                String urlpath = child.getAttributeValue("href", nss);
                String[] c = radarServerURL.split(uriBase);  //.replaceFirst("catalog.xml", "");
                String         ul     = c[0] + uriBase + urlpath;
                TwoFacedObject twoObj = new TwoFacedObject(desc, ul);
                collections.add(twoObj);
                //collections.put(desc, ul);
            }

        }

        return collections;
    }

    /**
     * Read the elements
     *
     * @param elem  element
     * @param eleName element name
     *
     * @return an element
     */
    public org.jdom2.Element readElements(org.jdom2.Element elem,
                                         String eleName) {
        List children = elem.getChildren();
        for (int j = 0; j < children.size(); j++) {
            org.jdom2.Element child     = (org.jdom2.Element) children.get(j);
            String           childName = child.getName();
            if (childName.equals(eleName)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Make the collection.  If there is an error, pop up a user message.
     *
     * @param url   URL for the collection
     */
    public synchronized void initializeCollection(String url) {

        List<NamedStationImpl> stations = new ArrayList<NamedStationImpl>();
        try {
            StringBuffer errlog = new StringBuffer();
            try {
                logger.debug("Initializing collection from URL: " + url);
                collection = TDSRadarDatasetCollection.factory("test", url, errlog);
            } catch (Exception exc) {
                if (canShowErrorWindows()) {
                    userMessage("Invalid catalog");
                }
                logger.warn("Invalid catalog: "+url, exc);
                return;
            }
            logger.debug("Iterating over collection...");
            List tdsStations = collection.getRadarStations();
            for (int i = 0; i < tdsStations.size(); i++) {
                StationImpl stn = (StationImpl) tdsStations.get(i);
                // thredds.catalog.query.Location loc = stn.getLocation();
                //TODO: need better station  need to switch lat lon
                NamedStationImpl station =
                    new NamedStationImpl(stn.getName(), stn.getName(),
                                         stn.getLatitude(),
                                         stn.getLongitude(),
                                         stn.getAltitude(), CommonUnit.meter);
                stations.add(station);

            }

            getStationMap().setStations(stations);
        } catch (Exception exc) {
            if (canShowErrorWindows()) {
                userMessage("Unable to load stations");
            }
            logger.warn("Unable to load stations from URL: "+url, exc);
            return;
        }

        SwingUtilities.invokeLater(() -> urlListHandler.saveState(urlBox));
    }

    /**
     * Utility method to check to see if McIDAS-V is ready to show error dialogs.
     *
     * @return {@code true} if McIDAS-V has finished starting and not in background mode.
     * {@code false} otherwise.
     */
    private static boolean canShowErrorWindows() {
        // TODO(jon): maybe this should be integrated into the userMessage method instead?
        McIDASV mcv = McIDASV.getStaticMcv();
        boolean result = false;
        if (mcv != null) {
            result = mcv.getHaveInitialized() && mcv.okToShowWindows();
        }
        return result;
    }

    /**
     * _more_
     *
     * @param url _more_
     */
    public void initializeLevel3Collection(String url) {

        List<NamedStationImpl> stations = new ArrayList<NamedStationImpl>();
        List<Product>          products;
        List<String>           exProducts = new ArrayList<String>();

        for(String ename: level3_ExName){
            exProducts.add(ename);
        }

        try {
            StringBuffer errlog = new StringBuffer();
            try {
                collection = TDSRadarDatasetCollection.factory("test", url,
                        errlog);
            } catch (Exception exc) {
                if (canShowErrorWindows()) {
                    userMessage("Invalid catalog");
                }
                logger.warn("Invalid catalog: "+url, exc);
                return;
            }
            products = collection.getRadarProducts();
            List tdsStations = collection.getRadarStations();
            for (int i = 0; i < tdsStations.size(); i++) {
                StationImpl stn = (StationImpl) tdsStations.get(i);
                // thredds.catalog.query.Location loc = stn.getLocation();
                //TODO: need better station  need to switch lat lon
                NamedStationImpl station =
                    new NamedStationImpl(stn.getName(), stn.getName(),
                                         stn.getLatitude(),
                                         stn.getLongitude(),
                                         stn.getAltitude(), CommonUnit.meter);
                stations.add(station);

            }
            List<TwoFacedObject> productNames = new ArrayList();
            for (Product product : products) {
               // if ( !product.getID().contains("DPA")
                 //       && !product.getID().contains("NVW")) {
                if ( !exProducts.contains(product.getID())) {
                    String lable = product.getName() + " (" + product.getID()
                                   + ")";
                    TwoFacedObject twoObj = new TwoFacedObject(lable,
                                                product.getID());
                    productNames.add(twoObj);
                }
            }
            GuiUtils.setListData(productComboBox, productNames);

            // GuiUtils.setListData(dataTypeComboBox, dataTypes);
            getStationMap().setStations(stations);
        } catch (Exception exc) {
            if (canShowErrorWindows()) {
                userMessage("Unable to load stations");
            }
            logger.warn("Unable to load stations from URL: "+url, exc);
            return;
        }
        urlListHandler.saveState(urlBox);
    }


    /**
     * Handle when the user has selected a new station
     */
    public void stationOrProductChanged() {
        Vector times = new Vector();
        setHaveData(false);
        if ((!isLevel3 && selectedStation != null) ||
                (isLevel3 && selectedStation != null && selectedProduct != null)) {
            List timeSpan = collection.getRadarTimeSpan();
            Date fromDate =  DateUnit.getStandardOrISO((String) timeSpan.get(0));
            //Date toDate = DateUnit.getStandardOrISO((String) timeSpan.get(1));
            Date toDate = new Date(System.currentTimeMillis()
                                   + DateUtil.daysToMillis(1));
            //Go back 10 years (or so)
            //Date fromDate = new Date(System.currentTimeMillis()
            //                         - DateUtil.daysToMillis(365 * 10));
            try {
                showWaitCursor();
                setAbsoluteTimes(new ArrayList());
                setStatus("Reading times for station: " + selectedStation,
                          "");
                //                LogUtil.message("Reading times for station: "
                //                                + selectedStation);
                String pid = null;
                if(isLevel3)
                    pid = TwoFacedObject.getIdString(
                                 productComboBox.getSelectedItem());
                List allTimes =
                    collection.getRadarStationTimes(selectedStation.getID(),
                        pid, fromDate, toDate);

             //   if(allTimes.size() == 0) {
             //       toDate = new Date(System.currentTimeMillis()
             //                + DateUtil.daysToMillis(1));
             //       allTimes =
             //       collection.getRadarStationTimes(selectedStation.getID(),
             //           pid, fromDate, toDate);
             //   }

                for (int timeIdx = 0; timeIdx < allTimes.size(); timeIdx++) {
                    Object timeObj = allTimes.get(timeIdx);
                    Date   date;
                    if (timeObj instanceof Date) {
                        date = (Date) timeObj;
                    } else {
                        date = DateUnit.getStandardOrISO(timeObj.toString());
                    }
                    times.add(new DateTime(date));
                }
                //                LogUtil.message("");
                showNormalCursor();
            } catch (Exception exc) {
                if (canShowErrorWindows()) {
                    userMessage("Error reading times for station: "
                            + selectedStation);
                }
                //logException("Getting times for station: " + selectedStation,
                //             exc);
                setStatus("Select a different collection", "collections");
                showNormalCursor();
                return;
            }
        }
        setAbsoluteTimes(times);
        updateStatus();
    }





    /**
     * Load the data
     */
    public void doLoadInThread() {
        // to the CDMRadarDataSource
        Hashtable ht = new Hashtable();
        if (selectedStation != null) {
            ht.put(ucar.unidata.data.radar.RadarDataSource.STATION_LOCATION,
                   selectedStation.getNamedLocation());
            ht.put(DataSelection.PROP_CHOOSERTIMEMATCHING, getDoTimeDrivers());
        } else {
            LogUtil.userMessage("No Station selected");
        }

        if (isLevel3 && (selectedProduct == null)) {

            LogUtil.userMessage("No Product selected");
        }

        try {
            DateSelection dateSelection = new DateSelection();
            String collectionUrl = TwoFacedObject.getIdString(
                                       collectionSelector.getSelectedItem());
            String     pid = null;
            RadarQuery radarQuery;
            if (isLevel3) {
                pid = TwoFacedObject.getIdString(
                    productComboBox.getSelectedItem());
                radarQuery = new RadarQuery(collectionUrl,
                                            selectedStation.getID(), pid,
                                            dateSelection);
            } else {
                radarQuery = new RadarQuery(collectionUrl,
                                            selectedStation.getID(),
                                            dateSelection);
            }

            List urls = new ArrayList();

            if (getDoAbsoluteTimes()) {
                List times    = new ArrayList();
                List selected = makeDatedObjects(getSelectedAbsoluteTimes());
                for (int i = 0; i < selected.size(); i++) {
                    DatedThing datedThing = (DatedThing) selected.get(i);
                    Date       date       = datedThing.getDate();
                    times.add(date);
                    URI uri = null;
                    try {
                        uri = collection.getRadarDatasetURI(
                            selectedStation.getID(), pid, date);
                    } catch (Exception excp) {
                        LogUtil.userMessage("incorrect times selected");
                        return;
                    }
                    urls.add(uri.toString());
                }
                if (urls.size() == 0) {
                    LogUtil.userMessage("No times selected");
                    return;
                }
                dateSelection.setTimes(times);
            } else {
//                int count = getRelativeTimesList().getSelectedIndex() + 1;
                if (relativeTimes == 0) {
                    LogUtil.userMessage("No relative times selected");
                    return;
                }
                Date toDate = new Date(System.currentTimeMillis()
                                       + DateUtil.daysToMillis(365 * 100));
                //Go back 10 years (or so)
                Date fromDate = new Date(System.currentTimeMillis()
                                         - DateUtil.daysToMillis(365 * 10));

                dateSelection.setStartFixedTime(fromDate);
                dateSelection.setEndFixedTime(toDate);
                dateSelection.setCount(relativeTimes);
            }
            makeDataSource(radarQuery, "FILE.RADAR", ht);
        } catch (Exception exc) {
            logException("Loading radar data", exc);
        }
        // uncheck the check box every time click the add source button
        drivercbx.setSelected(false);
        enableTimeWidgets();
        setDoTimeDrivers(false);
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
     * Add the interval selector to the component.
     * @return superclass component with extra stuff
     */
    protected JPanel makeTimesPanel() {
        JComponent extra = getExtraTimeComponent();
        GuiUtils.enableTree(extra, false);
        JPanel timesPanel = makeTimesPanel(extra, null);
        return timesPanel;
    }
    
    /**
     * Set the relative and absolute extra components
     */
    protected JPanel makeTimesPanel(JComponent relativeCard, JComponent absoluteCard) {
        JPanel timesPanel = super.makeTimesPanel(false, true, getIdv().getUseTimeDriver());

        // Make a new timesPanel that has extra components tacked on the bottom, inside the tabs
        Component[] comps = timesPanel.getComponents();

        if ((comps.length == 2) && (comps[0] instanceof JTabbedPane) && (comps[1] instanceof JLabel)) {
            timesCardPanelExtra = new GuiUtils.CardLayoutPanel();
            if (relativeCard == null) {
                relativeCard = new JPanel();
            }
            if (absoluteCard == null) {
                absoluteCard = new JPanel();
            }
            absoluteCard = GuiUtils.hbox(comps[1], GuiUtils.right(absoluteCard));
            timesCardPanelExtra.add(relativeCard, "relative");
            timesCardPanelExtra.add(absoluteCard, "absolute");
            timesPanel = GuiUtils.centerBottom(comps[0], timesCardPanelExtra);
        }
        return timesPanel;
    }
    
    /**
     * Get the time popup widget
     *
     * @return  a widget for selecting the day
     */
    protected JComponent getExtraTimeComponent() {
        JPanel filler = new JPanel();
        McVGuiUtils.setComponentHeight(filler, new JComboBox());
        return filler;
    }
 
    /**
     * Make the contents
     *
     * @return  the contents
     */
    protected JPanel doMakeInnerPanel() {
        JPanel myPanel = new JPanel();

        JLabel collectionLabel = McVGuiUtils.makeLabelRight("Collection:");

        collectionSelector = new JComboBox();
        collectionSelector.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                newSelectedStations(new ArrayList());
                if (collectionSelector.getSelectedItem() == null) {
                    return;
                }
                String collectionUrl =
                    TwoFacedObject.getIdString(
                        collectionSelector.getSelectedItem());

                if (collectionUrl.contains("level3")) {
                    setLevel3Collection(collectionUrl);
                } else {
                    setCollection(collectionUrl);
                }
            }

        });
        addServerComp(collectionLabel);
        addServerComp(collectionSelector);
                
        productComboBox = new JComboBox();
        productComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (productComboBox.getSelectedItem() == null) {
                    return;
                }
                selectedProduct =
                    productComboBox.getSelectedItem().toString();
                resetProductBox();
                productChanged();
            }

        });
        addLevel3ServerComp(productComboBox);
                
        productPanel = McVGuiUtils.makeLabeledComponent("Product:", productComboBox);
        
        JLabel stationLabel = McVGuiUtils.makeLabelRight("Station:");
        addServerComp(stationLabel);

        JComponent stationPanel = getStationMap();
        registerStatusComp("stations", stationPanel);
        addServerComp(stationPanel);
        
        JLabel timesLabel = McVGuiUtils.makeLabelRight("Times:");
        addServerComp(timesLabel);
        
        JPanel timesPanel = makeTimesPanel();
        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addServerComp(timesPanel);

        GuiUtils.enableComponents(compsThatNeedServer, false);
        GuiUtils.enableComponents(level3CompsThatNeedServer, false);
        productPanel.setVisible(false);

        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(collectionLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(collectionSelector, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(productPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(stationLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(stationPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(timesLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(collectionLabel)
                    .addComponent(collectionSelector)
                    .addComponent(productPanel))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(stationLabel)
                    .addComponent(stationPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(timesLabel)
                    .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(RELATED))
        );
        
        return myPanel;
    }

    private JPanel innerPanel = doMakeInnerPanel();

    private JLabel statusLabel = new JLabel("Status");

    @Override public void setStatus(String statusString, String foo) {
        if (statusString == null) {
            statusString = "";
        }
        statusLabel.setText(statusString);
    }
    
    protected void setInnerPanel(JPanel newInnerPanel) {
        innerPanel = newInnerPanel;
    }
    
    /**
     * Create the widget responsible for handling relative time selection.
     *
     * @return GUI widget.
     */
    @Override public JComponent getRelativeTimesChooser() {
        McVTextField relativeTimesField =
            McVGuiUtils.makeTextFieldAllow(String.valueOf(relativeTimes),
                                           4,
                                           false,
                                           '0', '1', '2', '3', '4', '5', '6',
                                           '7','8','9');
                                           
        // need to keep *both* the ActionListener and DocumentListener around.
        // removing the ActionListener results in strange behavior when you've
        // accidentally cleared out the text field.
        relativeTimesField.setAllow(Pattern.compile("^[1-9][0-9]*$"), true);
//        relativeTimesField.setDeny(Pattern.compile("^0$"), true);
        relativeTimesField.setColumns(4);
        relativeTimesField.addActionListener(e -> {
            String text = ((JTextField)e.getSource()).getText();
            validateRelativeTimeInput(text);
        });
        relativeTimesField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {
                handleRelativeTimeChange(e);
            }
            
            @Override public void removeUpdate(DocumentEvent e) {
                handleRelativeTimeChange(e);
            }
            
            @Override public void changedUpdate(DocumentEvent e) {
                handleRelativeTimeChange(e);
            }
        });
        relativeTimesField.setToolTipText(AddeChooser.RELATIVE_TIMES_TOOLTIP);
        
        JPanel panel =
            GuiUtils.topLeft(GuiUtils.label(AddeChooser.RELATIVE_TIMES_LABEL,
                relativeTimesField));
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(150, 100));
        return scrollPane;
    }
    
    /**
     * Validate the contents of the relative times text field.
     *
     * <p>This method overwrites {@link #relativeTimes} if {@code text} is an 
     * integer greater than zero.</p>
     *
     * @param text Contents of the text field.
     */
    private void validateRelativeTimeInput(String text) {
        try {
            int value = Integer.valueOf(text);
            if (value > 0) {
                relativeTimes = value;
                setHaveData(true);
                updateStatus();
            }
        } catch (NumberFormatException e) {
            setHaveData(false);
            setStatus("Please provide an integer value greater than zero.");
        }
    }
    
    /**
     * Handle {@link DocumentListener} events for the {@link JTextField}
     * created by {@link #getRelativeTimesChooser()}.
     *
     * @param event Event to handle. Cannot be {@code null}.
     */
    private void handleRelativeTimeChange(DocumentEvent event) {
        int len = event.getDocument().getLength();
        try {
            String text = event.getDocument().getText(0, len);
            validateRelativeTimeInput(text);
        } catch (BadLocationException ex) {
            logger.warn("Could not get contents of text field!", ex);
        }
    }
    
    /**
     * Get the relative time indices
     *
     * @return an array of indices
     */
    @Override public int[] getRelativeTimeIndices() {
        int[] indices = new int[relativeTimes];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        return indices;
    }
    
    /**
     * Make the UI for this selector.
     *
     * Thank you NetBeans for helping with the layout!
     *
     * @return The GUI.
     */
    public JComponent doMakeContents() {
        JPanel outerPanel = new JPanel();
        
        JLabel serverLabel = McVGuiUtils.makeLabelRight("Catalog:");                
        
        // Get the list of catalogs but remove the old catalog.xml entry
        urlListHandler = getPreferenceList(PREF_TDSRADARSERVER);

        ActionListener catListListener = ae -> {
            if (okToDoUrlListEvents) {
                setServer((String) urlBox.getSelectedItem());
            }
        };

        urlBox = urlListHandler.createComboBox(GuiUtils.CMD_UPDATE, catListListener, true);
        
        // TJJ Nov 2019
        // Since URLs are stored in user prefs, and THREDDS wants HTTPS now for radar catalog,
        // need to convert on-the-fly where they occur
        // See: https://mcidas.ssec.wisc.edu/inquiry-v/?inquiry=2857

        String selectedURL = (String) urlBox.getSelectedItem();
        // At first init, URL may be null. If so, set to default
        if (selectedURL != null) {
            selectedURL = selectedURL.replace("http:", "https:");
            urlBox.setSelectedItem(selectedURL);
        } else {
            urlBox.setSelectedItem(getProperty(PREF_TDSRADARSERVER, null));
        }

        McVGuiUtils.setComponentWidth(urlBox, Width.DOUBLEDOUBLE);
        
        // productComboBox gets created a little too tall--set to same height as urlBox
        if (productComboBox != null) {
            McVGuiUtils.setComponentHeight(productComboBox, urlBox);
        }
        JButton connectButton = McVGuiUtils.makeImageTextButton(ICON_CONNECT_SMALL, "Connect");
        McVGuiUtils.setComponentWidth(connectButton, Width.DOUBLE);
        connectButton.setActionCommand(GuiUtils.CMD_UPDATE);
        connectButton.addActionListener(this);
        
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
                                .addComponent(urlBox, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(GAP_UNRELATED)
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
                    .addComponent(urlBox)
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
}
