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

import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.*;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;



import edu.wisc.ssec.mcidasv.util.CollectionHelpers;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.data.imagery.BandInfo;
import ucar.unidata.data.imagery.ImageDataSource;
import ucar.unidata.data.imagery.ImageDataset;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.idv.chooser.adde.*;
import ucar.unidata.idv.chooser.adde.AddeServer.Group;

import ucar.unidata.ui.ChooserList;
import ucar.unidata.ui.DateTimePicker;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlNodeList;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.UtcDate;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/**
 * Widget to select images from a remote ADDE server
 * Displays a list of the descriptors (names) of the image datasets
 * available for a particular ADDE group on the remote server.
 *
 * @author Don Murray
 */
public class Test2AddeImageChooser extends AddeImageChooser implements Constants {


    /**
     * Public keys for server, group, dataset, user, project.
     */
    public final static String SIZE_KEY = "size";
    public final static String BAND_KEY = "band";
    public final static String PLACE_KEY = "place";
    public final static String LATLON_KEY = "latlon";
    public final static String LINELE_KEY = "linele";
    public final static String MAG_KEY = "mag";
    public final static String UNIT_KEY = "unit";
    public final static String PREVIEW_KEY = "preview";

    /** Command for connecting */
    protected static final String CMD_MANAGER = "cmd.manager";

    /** descriptor label */
    protected JLabel descriptorLabel = new JLabel(getDescriptorLabel()+":");

    /** Property for the satband file */
    protected static final String FILE_SATBAND = "SATBAND";

    /** Property for image default value band */
    protected static final String PROP_BAND = "BAND";

    /** Property for image default value id */
    protected static final String PROP_ID = "ID";

    /** Property for image default value key */
    protected static final String PROP_KEY = "key";

    /** Property for image default value line/ele */
    protected static final String PROP_LINEELE = "LINELE";

    /** Property for image default value mag */
    protected static final String PROP_MAG = "MAG";

    /** Property for image default value place */
    protected static final String PROP_PLACE = "PLACE";

    /** Property for image default value spac */
    protected static final String PROP_SPAC = "SPAC";

    /** Property for image default value unit */
    protected static final String PROP_UNIT = "UNIT";

    /** Property for image default value unit */
    protected static final String PROP_NAV = "NAV";

    /** Xml tag name for the defaults */
    protected static final String TAG_DEFAULT = "default";

    /** identifiere for the default value */
    protected static final String VALUE_DEFAULT = "default";

    /** Xml attr name for the defaults */
    protected static final String ATTR_NAME = "name";

    /** Xml attr name for the defaults */
    protected static final String ATTR_PATTERN = "pattern";

    /** Xml element from imagedefaults.xml */
    private static Element restElement;
    private List dtList;
    private List dateTimes;

    /** Xml attr name for the defaults */
    private static final String ATTR_UNIT = "UNIT";
    private static final String ATTR_BAND = "BAND";
    private static final String ATTR_PLACE = "PLACE";
    private static final String ATTR_SIZE = "SIZE";
    private static final String ATTR_MAG = "MAG";
    private static final String ATTR_LATLON = "LATLON";
    private static final String ATTR_LINELE = "LINELE";

    /** Selection label text */
    protected static final String LABEL_SELECT = " -- Select -- ";

    /** flag for setting properties */
    private boolean amSettingProperties = false;

    /** Are we currently reading times */
    protected Object readTimesTask;

    /** archive date */
    //private String archiveDay = null;

    /** List of descriptors */
    //private PreferenceList descList;

    /** Holds the properties */
    private JPanel propPanel;

    /** archive day button */
    private JComponent archiveDayComponent;

    /** archive day button */
    private JLabel archiveDayLabel;

    /** Maps the PROP_ property name to the gui component */
    private Hashtable propToComps = new Hashtable();

    /**
     * This is a list of hashtables, one per imagedefaults resource.
     * The Hashtables map the pattern to the xml node
     */
    private List resourceMaps;

    /** Holds the subsetting defaults */
    private XmlResourceCollection addeServerRSC;
    private XmlResourceCollection imageDefaultsRSC;

    /** archive date formatter */
    private SimpleDateFormat archiveDayFormatter;

    /**
     * List of JComponent-s that depend on a descriptor being selected
     * to be enabled
     */
//    protected ArrayList compsThatNeedDescriptor = new ArrayList();

    /** Flag to keep from infinite looping */
//    private boolean ignoreDescriptorChange = false;

    /** Identifier for the maximum number of bands */
    int MAX_BANDS = 100;

    /** The last AreaDirectory we have seen. */
    AreaDirectory lastAD;

    /** The current AreaDirectory used for properties */
    AreaDirectory propertiesAD;

    /** The previous AreaDirectory used for properties */
    AreaDirectory prevPropertiesAD;

    /** Mapping of area directory to list of BandInfos */
    protected Hashtable bandTable;

    /**
     *  The list of currently loaded AddeImageDescriptor-s
     */
    private Vector imageDescriptors;

    /** the place string */
    private String place;

    /**
     * Mapping of sensor id (String) to hashtable that maps
     * Integer band number to name
     */
    private Hashtable sensorToBandToName;

    /** A flag so we can debug the new way of processing sat band file */
    private boolean useSatBandInfo = true;

    /** Used to parse the sat band file */
    private AddeSatBands satBandInfo;

    /** Widget for selecting the band */
    protected JComboBox bandComboBox;

    /** string for ALL */
    private static final String ALL = "ALL";

    /** object for selecting all bands */
    private static final TwoFacedObject ALLBANDS =
        new TwoFacedObject("All Bands", ALL);

    /** object for selecting all calibrations */
    private static final TwoFacedObject ALLUNITS =
        new TwoFacedObject("All Types", ALL);

    /**
     *  Keep track of which image load we are on.
     */
    private int currentTimestep = 0;

    /**
     *  Keep track of the lines to element ratio
     */
    private double linesToElements = 1.0;

    /**
     * Only changes in ele slider should change ratio
     */
    private boolean recomputeLineEleRatio = true;

    /**
     * limit of slider
     */
    private static final int SLIDER_MAX = 29;

    /**
     * the  list of band infos
     */
    private List<BandInfo> bandInfos;

    private String bandDefault = ALL;
    private String unitDefault = ALL;
    
    /** Default value for the user property */
    protected static String DEFAULT_USER = "";

    /** Default value for the proj property */
    protected static String DEFAULT_PROJ = "";

    /** Accounting information */
    private static String user;
    private static String proj;

    /** UI for selecting a server */
    private JComboBox serverSelector;

    /** My servers */
    private List addeServers;

    private static int mainIndex;

    protected JTabbedPane tabbedPane;

    private int descIndex;

    private boolean allServersFlag;

    private static JToggleButton mineBtn = null;
    private static JCheckBox  previewBox = null;

    /** Separator string */
    private static String separator = "----------------";
    private static String lastUrl = "";

    /**
     * Construct an Adde image selection widget
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public Test2AddeImageChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        serverSelector = getServerSelector();
        loadServerState();
    }


    /**
     * Load any saved server state
     */
    private void loadServerState() {
        if (addeServers == null) {
            return;
        }
        String id = getId();
        String[] serverState =
            (String[]) getIdv().getStore().get(Constants.PREF_SERVERSTATE + "." + id);
        if (serverState == null) {
            return;
        }
        AddeServer server = AddeServer.findServer(addeServers,
                                serverState[0]);
        if (server == null) {
            return;
        }
        serverSelector.setSelectedItem(server);
        setGroups();
        if (serverState[1] != null) {
            AddeServer.Group group =
                (AddeServer.Group) server.findGroup(serverState[1]);
            if (group != null) {
                groupSelector.setSelectedItem(group);
            }
        }
        if (server.getIsLocal()) {
            try {
                handleUpdate();
            } catch (Exception e) {}
        }
    }


    private boolean getAllServersFlag() {
        return getIdv().getStore().get(Constants.PREF_SYSTEMSERVERSIMG, true);
    }


    /**
     * Get the xml resource collection that defines the adde servers xml
     *
     * @return Image defaults resources
     */
    protected XmlResourceCollection getAddeServers() {
        return getIdv().getResourceManager().getXmlResources(
            IdvResourceManager.RSC_ADDESERVER);
    }


    protected int getMainIndex() {
        return mainIndex;
    }


    /**
     * Create the 'Manage...' button.
     *
     * @return The manage button.
     */
    protected JComponent getManageButton() {
        JButton managerBtn = new JButton("Manage...");
        managerBtn.setActionCommand(CMD_MANAGER);
        managerBtn.addActionListener(this);
        return registerStatusComp("manager", managerBtn);
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
        } else {
            super.actionPerformed(ae);
        }
    }


    protected void setUserAndProj(String user, String proj) {
        DEFAULT_USER = user;
        DEFAULT_PROJ = proj;
        this.user = user;
        this.proj = proj;
    }


    protected void setServerOnly(String serverName) {
        serverName = serverName.toLowerCase();
        AddeServer newServer = new AddeServer(serverName);
        if (newServer != null) {
            descIndex = -1;
            serverSelector.setSelectedItem(newServer);
            updateGroups();
        }
    }


    protected void setGroupOnly(String groupName) {
        if (groupSelector != null) {
            groupSelector.setSelectedItem(groupName);
        }
    }


    /**
     * Get the names for the buttons.
     *
     * @return array of button names
     */
    protected String[] getButtonLabels() {
        return new String[] { getLoadCommandName(), GuiUtils.CMD_UPDATE,
                              GuiUtils.CMD_HELP, GuiUtils.CMD_CANCEL};
    }


    /**
     * Do we have times selected. Either we are doing absolute
     * times and there are some selected in the list. Or we
     * are doing relative times and we have done a connect to the
     * server
     *
     * @return Do we have times
     */
    public boolean timesOk() {
        if (getDoAbsoluteTimes() && !haveTimeSelected()) {
            return false;
        }
        return (lastAD != null);
    }


    /**
     * Get the name of the dataset.
     *
     * @return descriptive name of the dataset.
     */
    public String getDatasetName() {
        StringBuffer buf = new StringBuffer();
        buf.append(getSelectedDescriptor());
        if (!bandDefault.equals(ALL)) {
            buf.append(" (Band: ");
            buf.append(new Integer(bandDefault).toString());
            buf.append(")");
        }
        return buf.toString();
    }


    /**
     * Handle when the user presses the update button
     *
     * @throws Exception On badness
     */
    public void handleUpdate() throws Exception {
        super.handleUpdate();
        setRestElement(null);
    }


    public void updateServers() {
        super.updateServers();
        if (serverManager == null)
            serverManager = ((McIDASV)getIdv()).getServerManager();
        String type = getGroupType();
        List<AddeServer> managedServers = serverManager.getIdvStyleEntries();

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
        addeServers = servers;
    }


    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */

    public JComponent doMakeContents() {
        List allComps = new ArrayList();
        JComponent mainComp = makeMainPanel();
        allComps.add(mainComp);
        JPanel imagePanel = GuiUtils.doLayout(allComps, 1, GuiUtils.WT_NY,
                                GuiUtils.WT_N);
        tabbedPane = new JTabbedPane();
        imagePanel.setName("Satellite Imagery");
        tabbedPane.add(imagePanel);
        mainIndex = tabbedPane.getSelectedIndex();
        Image2ParametersTab ip = new Image2ParametersTab(this, tabbedPane);
        JPanel parameterSetsPanel = ip.doMakeContents();
        tabbedPane.addTab("Restore Parameter Set",parameterSetsPanel);
        return GuiUtils.top(tabbedPane);
    }


    public JComponent makeMainPanel() {
        JPanel myPanel = new JPanel();

        JLabel timesLabel = McVGuiUtils.makeLabelRight("Times:");
        addDescComp(timesLabel);

        JPanel timesPanel = makeTimesPanel();
        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addDescComp(timesPanel);

        JLabel navigationLabel = McVGuiUtils.makeLabelRight("Navigation:");
        addDescComp(navigationLabel);

        // Use processPropertyComponents to build combo boxes that we rely on
        processPropertyComponents();
        addDescComp(navComboBox);
        McVGuiUtils.setComponentWidth(navComboBox, McVGuiUtils.Width.DOUBLE);

        previewBox = new JCheckBox("Preview Image", true);
        addDescComp(previewBox);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(descriptorLabel)
                        .add(GAP_RELATED)
                        .add(descriptorComboBox))
                        .add(layout.createSequentialGroup()
                            .add(timesLabel)
                            .add(GAP_RELATED)
                            .add(timesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .add(layout.createSequentialGroup()
                                .add(navigationLabel)
                                .add(GAP_RELATED)
                                .add(navComboBox))
                                .add(layout.createSequentialGroup()
                                    .add(previewBox))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(descriptorLabel)
                    .add(descriptorComboBox))
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(timesLabel)
                        .add(timesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(navigationLabel)
                            .add(navComboBox))
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                               .add(previewBox)))
        );

        setInnerPanel(myPanel);
        return super.doMakeContents(true);
    }


    /**
     * Add the bottom advanced gui panel to the list
     *
     * @param bottomComps  the bottom components
     */
    protected void getBottomComponents(List bottomComps) {
        Insets  dfltGridSpacing = new Insets(4, 0, 4, 0);
        String  dfltLblSpacing  = " ";

        JComponent propComp = null;
        String     prop     = PROP_NAV;
        navComboBox = new JComboBox();
        GuiUtils.setListData(
            navComboBox,
            Misc.newList(
                new TwoFacedObject("Default", "X"),
                new TwoFacedObject("Lat/Lon", "LALO")));
        addPropComp(PROP_NAV, navComboBox);
        propComp = GuiUtils.hbox(
            GuiUtils.inset(
                new JLabel("Navigation Type:"),
                new Insets(0, 0, 0, 5)), navComboBox, 5);
        if (propComp != null) {
            bottomComps.add(GuiUtils.rLabel(" "));
            bottomComps.add(GuiUtils.left(propComp));
        }

        GuiUtils.tmpInsets = new Insets(3, 4, 0, 4);
        propPanel = GuiUtils.doLayout(bottomComps, 2, GuiUtils.WT_N,
                                      GuiUtils.WT_N);
        enableWidgets();
    }


    /**
     * Get the default relative time index
     *
     * @return default index
     */
    protected int getDefaultRelativeTimeIndex() {
        return 0;
    }


    /**
     * Get the selected descriptor.
     *
     * @return  the currently selected descriptor.
     */
/*
    protected String getDescriptor() {
        return getDescriptorFromSelection(getSelectedDescriptor());
    }
*/

    /**
     * Get the descriptor relating to the selection.
     *
     * @param selection   String name from the widget
     *
     * @return  the descriptor
     */
/*
    protected String getDescriptorFromSelection(String selection) {
        if (descriptorTable == null) {
            return null;
        }
        if (selection == null) {
            return null;
        }
        return (String) descriptorTable.get(selection);
    }
*/

    /**
     * Set the list of dates/times based on the image selection
     *
     * @param timestep    the timestep for the image selection
     */
    protected void readTimesInner() {
        String       descriptor  = getDescriptor();
        String       pos = (getDoAbsoluteTimes() || (archiveDay != null))
                           ? "all"
                           : "0";


        StringBuffer addeCmdBuff = getGroupUrl(REQ_IMAGEDIR, getGroup());
        String       id          = getSelectedStation();
        if (id != null) {
            appendKeyValue(addeCmdBuff, PROP_ID, id);
        }
        appendKeyValue(addeCmdBuff, PROP_DESCR, descriptor);
        appendKeyValue(addeCmdBuff, PROP_POS, "" + pos);
        if (archiveDay != null) {
            appendKeyValue(addeCmdBuff, PROP_DAY, archiveDay);
        }
        loadImages(addeCmdBuff.toString());
    }


    /** locking mutex */
    private Object MUTEX = new Object();


    /**
     * Load the images for the given URL and timestep
     *
     * @param url          ADDE URL
     */
    protected void loadImages(String url) {
        readTimesTask = startTask();
        updateStatus();
        Object task = readTimesTask;
        lastUrl = url;
        try {
            AreaDirectoryList adir = new AreaDirectoryList(url);
            //Make sure no other loads are  occurred
            boolean ok = stopTaskAndIsOk(task);
            if(!Misc.equals(readTimesTask, task) || !ok) {
                return;
            }
            readTimesTask = null;

            synchronized (MUTEX) {
                // Array of AreaDirectory-s sorted by time
                AreaDirectory[][] dirs      = adir.getSortedDirs();
                int               numImages = dirs.length;
                imageDescriptors = new Vector();
                bandTable = new Hashtable(numImages);
                lastAD    = null;
                dateTimes = new ArrayList();
                for (int i = 0; i < numImages; i++) {
                    int bandIndex = 0;
                    lastAD = (AreaDirectory) dirs[i][0];
                    int[]    allBands = new int[MAX_BANDS];
                    Vector[] allCals  = new Vector[MAX_BANDS];
                    for (int j = 0; j < dirs[i].length; j++) {
                        int      nbands = dirs[i][j].getNumberOfBands();
                        int[]    abands = dirs[i][j].getBands();
                        Vector[] vb     = dirs[i][j].getCalInfo();
                        for (int k = 0; k < nbands; k++) {
                            allBands[bandIndex]  = abands[k];
                            allCals[bandIndex++] = vb[k];
                        }
                    }
                    int[] bands = new int[bandIndex];
                    System.arraycopy(allBands, 0, bands, 0, bandIndex);
                    Vector[] cals = new Vector[bandIndex];
                    System.arraycopy(allCals, 0, cals, 0, bandIndex);
                    lastAD.setCalInfo(cals);
                    bandTable.put(lastAD, bands);
                    AddeImageDescriptor aid = new AddeImageDescriptor(lastAD,
                                                  null);
                    imageDescriptors.add(aid);
                    dateTimes.add(aid.getImageTime());
                }

                Collections.sort(imageDescriptors);
                if (getDoAbsoluteTimes()) {
                    setAbsoluteTimes(imageDescriptors);
                }
                if (restElement != null) {
                    int timeCount = 0;
                    for (int i=0; i<dtList.size(); i++) {
                        if (dateTimes.contains((DateTime)dtList.get(i))) {
                            ++timeCount;
                        }
                    }
                    DateTime[] dTs = new DateTime[timeCount];
                    int ix = 0;
                    for (int i=0; i<dtList.size(); i++) {
                        DateTime dT = (DateTime)dtList.get(i);
                        if (dateTimes.contains(dT)) {
                            dTs[ix] = dT;
                            ix++;
                        }
                    }
                    if (dTs.length > 0) setSelectedTimes(dTs);
                }
            }
            setState(STATE_CONNECTED);
        } catch (McIDASException e) {
            System.out.println("Exception from loadImages........ e=" + e);
            stopTask(task);
            readTimesTask = null;
            handleConnectionError(e);
        }
    }


    protected void setDtList(List restList) {
        dtList = restList;
    }


    /**
     * Set the selected times in the times list based on the input times
     *
     * @param times  input times
     */
    protected void setSelectedTimes(DateTime[] times) {
        if ((times == null) || (times.length == 0)) {
            return;
        }
        List selectedIndices = new ArrayList();
        DateTime[] imageTimes = new DateTime[times.length];

        if (imageDescriptors != null) {
            imageTimes = new DateTime[imageDescriptors.size()];
            for (int idIdx = 0; idIdx < imageDescriptors.size(); idIdx++) {
                AddeImageDescriptor aid =
                    (AddeImageDescriptor) imageDescriptors.get(idIdx);
                imageTimes[idIdx] = aid.getImageTime();
            }
        } else {
            imageTimes = times;
        }
        if (imageTimes.length > 0) {
            try {
                Gridded1DSet imageSet    = DateTime.makeTimeSet(imageTimes);
                int          numTimes    = times.length;
                double[][]   timesValues = new double[1][numTimes];
                for (int i = 0; i < times.length; i++) {
                    DateTime dt = times[i];
                    timesValues[0][i] =
                        times[i].getValue(imageSet.getSetUnits()[0]);
                }
                setSelectedAbsoluteTimes(imageSet.doubleToIndex(timesValues));
                absoluteTimesSelectionChanged();
            } catch (VisADException ve) {
                logException("Unable to set times from display", ve);
            }
        }
    }


    /**
     * Set the center location portion of the request.  If the input
     * from the widget is null, use the centerpoint from the image descriptor.
     *
     * @param aid   image descriptor for the image
     */
    protected void setCenterLocation(AddeImageDescriptor aid) {
        String latPoint = "";
        String lonPoint = "";
        if (aid != null) {
            AreaDirectory ad = aid.getDirectory();
            latPoint = "" + ad.getCenterLatitude();
            lonPoint = "" + ad.getCenterLongitude();
        }
    }


    /**
     * Does this selector have all of its state set to load in data
     *
     * @return Has the user chosen everything they need to choose to load data
     */
    protected boolean getGoodToGo() {
        if (getDoAbsoluteTimes()) {
            return getHaveAbsoluteTimesSelected();
        } else {
            return canReadTimes() && (lastAD != null);
        }
    }

    /**
     *  Generate a list of image descriptors for the descriptor list.
     */
/*
    protected void readDescriptors() {
        try {
            StringBuffer buff   = getGroupUrl(REQ_DATASETINFO, getGroup());
            DataSetInfo  dsinfo = new DataSetInfo(buff.toString());
            descriptorTable = dsinfo.getDescriptionTable();
            String[]    names       = new String[descriptorTable.size()];
            Enumeration enumeration = descriptorTable.keys();
            for (int i = 0; enumeration.hasMoreElements(); i++) {
                names[i] = enumeration.nextElement().toString();
            }
            Arrays.sort(names);
            setDescriptors(names);
            setState(STATE_CONNECTED);
        } catch (Exception e) {
            handleConnectionError(e);
        }
    }
*/

    /**
     * Returns a list of the images to load or null if none have been
     * selected.
     *
     * @return  list  get the list of image descriptors
     */
    public List getImageList() {
        if ( !timesOk()) {
            return null;
        }
        List images = new ArrayList();
        if (getDoRelativeTimes()) {
            AddeImageDescriptor firstDescriptor =
                (AddeImageDescriptor) imageDescriptors.get(0);
            AreaDirectory ad = firstDescriptor.getDirectory();
            int[] bands    = (int[]) bandTable.get(ad);
            bandInfos = makeBandInfos(ad, bands);
            int[] relativeTimesIndices = getRelativeTimeIndices();
            for (int i = 0; i < relativeTimesIndices.length; i++) {
                AddeImageDescriptor aid =
                    new AddeImageDescriptor(relativeTimesIndices[i],
                                            firstDescriptor);
                AddeImageInfo aii = makeImageInfo(aid.getDirectory(), true,
                                        relativeTimesIndices[i]);
                aii.setBand(ALL);
                aid.setImageInfo(aii);
                aid.setSource(aii.getURLString());
                images.add(aid);
            }
        } else {
            List selectedTimes = getSelectedAbsoluteTimes();
            for (int i = 0; i < selectedTimes.size(); i++) {
                AddeImageDescriptor aid =
                    new AddeImageDescriptor(
                        (AddeImageDescriptor) selectedTimes.get(i));
                AddeImageInfo aii = makeImageInfo(aid.getDirectory(), false,
                                        i);
                aii.setBand(ALL);
                aid.setImageInfo(aii);
                aid.setSource(aii.getURLString());
                images.add(aid);
            }
        }
        return images;
    }


    /*
     * Create the date time string for the given area directory
     *
     *
     * @param ad The areadirectory to make the dttm string for
     * @param cnt Which number in the list of selected times is this
     * @param doTimes Should we do regular time or create a relative time
     * @return  ADDE image select string ("&amp;DAY=day day&amp;TIME=time time")
     */
    protected String makeDateTimeString(AreaDirectory ad, int cnt,
                                        boolean doTimes) {
        if ( !doTimes) {
            return "&POS=" + ((cnt == 0)
                              ? cnt
                              : (-cnt));
        } else {
            return makeDateTimeString(ad);
        }
    }


    /**
     * Create the date time string for the given area directory
     *
     * @param ad   AreaDirectory with time
     * @return  ADDE image select string ("&amp;DAY=day day&amp;TIME=time time")
     */
    protected String makeDateTimeString(AreaDirectory ad) {
        try {
            DateTime dt   = new DateTime(ad.getNominalTime());
            String   jday = UtcDate.getYMD(dt);
            String   time = UtcDate.getHMS(dt);
            return "&DAY=" + jday + "&TIME=" + time + " " + time + " I ";
        } catch (visad.VisADException ve) {
            return "";
        }
    }


    /**
     * Process the image defaults resources
     */
    protected void initializeAddeDefaults() {
        resourceMaps = new ArrayList();
        if (imageDefaultsRSC == null) {
            return;
        }
        for (int resourceIdx = 0; resourceIdx < imageDefaultsRSC.size();
                resourceIdx++) {
            Element root = imageDefaultsRSC.getRoot(resourceIdx);
            if (root == null) {
                continue;
            }
            Hashtable resourceMap = new Hashtable();
            resourceMaps.add(resourceMap);

            XmlNodeList defaultNodes = XmlUtil.getElements(root, TAG_DEFAULT);
            for (int nodeIdx = 0; nodeIdx < defaultNodes.size(); nodeIdx++) {
                Element dfltNode = (Element) defaultNodes.item(nodeIdx);
                String pattern = XmlUtil.getAttribute(dfltNode, ATTR_PATTERN,
                                     (String) null);
                if (pattern == null) {
                    pattern = XmlUtil.getAttribute(dfltNode, ATTR_NAME);
                }
                if (pattern != null) {
                    pattern = pattern.toLowerCase();
                }
                resourceMap.put(pattern, dfltNode);
            }
        }
    }


    protected void setDescriptor(String descriptorName) {
        String newName = null;
        try {
            StringBuffer buff   = getGroupUrl(REQ_DATASETINFO, getGroup());
            DataSetInfo  dsinfo = new DataSetInfo(buff.toString());
            descriptorTable = dsinfo.getDescriptionTable();
            String[]    names       = new String[descriptorTable.size()];
            Enumeration enumeration = descriptorTable.keys();
            for (int i = 0; enumeration.hasMoreElements(); i++) {
                String key = enumeration.nextElement().toString();
                Object val = descriptorTable.get(key);
                names[i] = key;
                if (descriptorName.equals(val)) {
                    newName = key;
                }
            } 
            Arrays.sort(names);
            setDescriptors(names); 
            setState(STATE_CONNECTED);
        } catch (Exception e) {
            handleConnectionError(e);
        }

        if (newName == null) return;
        descriptorComboBox.setSelectedItem(newName);
        String newDescriptor = getDescriptor();
        descriptorChanged();
    }


    protected void setTime(int pos) {
        if (pos < 0) return;
        List timeList = getAbsoluteTimes();
        ChooserList newTimesList = getRelativeTimesList();
        newTimesList.setSelectedIndex(pos);
    }


    /**
     *  Get the default value for a key
     *
     *  @param property      property (key type)
     *  @param dflt        default value
     *  @return value for key or dflt if not found
     */
    protected String getDefault(String property, String dflt) {
        if (resourceMaps == null) {
            initializeAddeDefaults();
        }
        property = property.toLowerCase();

        String   userDefault = null;
        String   server      = getServer();
        String   group       = getGroup();
        String   descriptor  = getDescriptor();
        String[] keys        = {
            userDefault, server + ":" + group + "/" + descriptor,
            group + "/" + descriptor, server + ":" + group + "/*",
            group + "/*", server + ":*/" + descriptor, "*/" + descriptor,
            descriptor, server + ":*/*", server, "*"
        };

        if (server != null) {
            if (property.equals(PROP_USER) || property.equals(PROP_PROJ)) {
                String[] pair = (String[]) passwords.get(server);
                if (pair != null) {
                    if (property.equals(PROP_USER)) {
                        return pair[0];
                    }
                    return pair[1];
                }
            }
        }

        for (int resourceIdx = 0; resourceIdx < resourceMaps.size();
                resourceIdx++) {
            Hashtable resourceMap = (Hashtable) resourceMaps.get(resourceIdx);
            for (int keyIdx = 0; keyIdx < keys.length; keyIdx++) {
                String key = keys[keyIdx];
                if (key == null) {
                    continue;
                }
                key = key.toLowerCase();
                Element defaultNode = (Element) resourceMap.get(key);
                if (defaultNode == null) {
                    continue;
                }
                String value = XmlUtil.getAttribute(defaultNode, property,
                                   (String) null);
                if (value == null) {
                    continue;
                }
                if (value.equals(VALUE_DEFAULT)) {
                    return dflt;
                } else {
                    return value;
                }
            }
        }
        return dflt;
    }


    /**
     * Get any extra key=value pairs that are appended to all requests.
     *
     *
     * @param buff The buffer to append to
     */
    protected void appendMiscKeyValues(StringBuffer buff) {
        appendKeyValue(buff, PROP_COMPRESS, DEFAULT_COMPRESS);
        appendKeyValue(buff, PROP_PORT, DEFAULT_PORT);
        appendKeyValue(buff, PROP_DEBUG, DEFAULT_DEBUG);
        appendKeyValue(buff, PROP_VERSION, DEFAULT_VERSION);
        if (DEFAULT_USER.equals("") || DEFAULT_USER.equals("idv")) DEFAULT_USER = getLastAddedUser();
        appendKeyValue(buff, PROP_USER, DEFAULT_USER);
        if (DEFAULT_PROJ.equals("") || DEFAULT_PROJ.equals("0")) DEFAULT_PROJ = getLastAddedProj();
        appendKeyValue(buff, PROP_PROJ, DEFAULT_PROJ);
    }


    /**
     * Get the value for the given property. This can either be the value
     * supplied by the end user through the advanced GUI or is the default
     *
     * @param prop The property
     * @param ad The AreaDirectory
     *
     * @return The value of the property to use in the request string
     */
    protected String getPropValue(String prop, AreaDirectory ad) {
        if (prop.equals(PROP_NAV)) {
	    return TwoFacedObject.getIdString(navComboBox.getSelectedItem());
        }
        return getDefault(prop, getDefaultPropValue(prop, ad, false));
    }


    /**
     * Get the default property value for the adde request string
     *
     * @param prop The property
     * @param ad The AreaDirectory
     * @param forDisplay Is this to display to the user in the gui
     *
     * @return The default of the property to use in the request string
     */
    protected String getDefaultPropValue(String prop, AreaDirectory ad,
                                         boolean forDisplay) {
        if (prop.equals(PROP_USER)) {
            return DEFAULT_USER;
        }
        if (prop.equals(PROP_PROJ)) {
            return DEFAULT_PROJ;
        }
        if (prop.equals(PROP_DESCR)) {
            return getDescriptor();
        }
        if (prop.equals(PROP_VERSION)) {
            return DEFAULT_VERSION;
        }
        if (prop.equals(PROP_COMPRESS)) {
            return "gzip";
        }
        if (prop.equals(PROP_PORT)) {
            return DEFAULT_PORT;
        }
        if (prop.equals(PROP_DEBUG)) {
            return DEFAULT_DEBUG;
        }
        if (prop.equals(PROP_NAV)) {
            return "X";
        }
        return "";
    }


    /**
     * Set the properties on the AddeImageInfo from the list of properties
     *
     * @param aii  The AddeImageInfo
     * @param props  list of props to set
     * @param ad The AreaDirectory
     */
    protected void setImageInfoProps(AddeImageInfo aii, String[] props,
                                     AreaDirectory ad) {
        for (int i = 0; i < props.length; i++) {
            String prop  = props[i];
            String value = getPropValue(prop, ad);
            if (prop.equals(PROP_USER)) {
                aii.setUser(value);
            } else if (prop.equals(PROP_PROJ)) {
                aii.setProject(Integer.parseInt(value));
            } else if (prop.equals(PROP_DESCR)) {
                aii.setDescriptor(value);
            } else if (prop.equals(PROP_VERSION)) {
                aii.setVersion(value);
            } else if (prop.equals(PROP_COMPRESS)) {
                int compVal = AddeURL.GZIP;
                if (value.equals("none") || value.equals("1")) {
                    compVal = AddeURL.NO_COMPRESS;
                } else if (value.equals("compress") || value.equals("2")
                           || value.equals("true")) {
                    compVal = AddeURL.COMPRESS;
                }
                aii.setCompression(compVal);
            } else if (prop.equals(PROP_PORT)) {
                aii.setPort(Integer.parseInt(value));
            } else if (prop.equals(PROP_DEBUG)) {
                aii.setDebug(Boolean.getBoolean(value));
            } else if (prop.equals(PROP_UNIT)) {
                value = unitDefault;
                if (value.equals(ALLUNITS.getId())) {
                    value = getDefault(PROP_UNIT,
                                       getDefaultPropValue(prop, ad, false));
                }
                aii.setUnit(value);
            } else if (prop.equals(PROP_BAND)) {
                value = bandDefault;
                if (value.equals(ALLBANDS.toString())
                        || value.equals(ALLBANDS.toString())) {
                    value = getDefault(prop,
                                       getDefaultPropValue(prop, ad, false));
                }
                aii.setBand(value);
            } else if (prop.equals(PROP_NAV)) {
                aii.setNavType(value);
            } else if (prop.equals(PROP_ID)) {
                aii.setId(value);
            }
        }
    }


    /**
     * Set the widgets with the state from the given AreaDirectory
     *
     * @param ad   AreaDirectory for the image
     * @param force force an update regardless of the previous invocation
     */
    protected void setPropertiesState(AreaDirectory ad, boolean force) {
        if (amSettingProperties) {
            return;
        }
        prevPropertiesAD = propertiesAD;
        propertiesAD     = ad;
        if ( !force && checkPropertiesEqual(prevPropertiesAD, propertiesAD)) {
            return;
        }

        amSettingProperties = true;

        if (ad == null) {
            amSettingProperties = false;
            return;
        }

        List<BandInfo> bandList = null;
        int[]          bands    = (int[]) bandTable.get(ad);
        if (bands != null) {
            bandList = makeBandInfos(ad, bands);
        }
        bandInfos = bandList;

        if (bandComboBox != null) {
            List comboList = bandList;
            if (bandList.size() > 1) {
                comboList = new ArrayList();
                comboList.addAll(bandList);
                comboList.add(ALLBANDS);
            }
            GuiUtils.setListData(bandComboBox, comboList);
        }

        amSettingProperties = false;
    }


    /**
     * Get the band name for a particular area
     *
     * @param ad AreaDirectory
     * @param band band number
     *
     * @return name of the band
     */

    protected String getBandName(AreaDirectory ad, int band) {

        if (useSatBandInfo) {
            if (satBandInfo == null) {
                return "Band: " + band;
            }
            String[] descrs = satBandInfo.getBandDescr(ad.getSensorID(),
                                  ad.getSourceType());
            if (descrs != null) {
                if ((band >= 0) && (band < descrs.length)) {
                    return descrs[band];
                }
            }
            return "Band: " + band;
        }


        if (sensorToBandToName == null) {
            return "Band: " + band;
        }
        Hashtable bandToName =
            (Hashtable) sensorToBandToName.get(new Integer(ad.getSensorID()));
        String  name        = null;
        Integer bandInteger = new Integer(band);

        if (bandToName != null) {
            name = (String) bandToName.get(bandInteger);
        }
        if (name == null) {
            name = "Band: " + band;
        }
        return name;
    }


    /**
     * Set the available units in the  unit selector
     *
     * @param ad   AreaDirectory for the image
     * @param band band to use for units
     *
     * @return List of available units
     */
    protected List<TwoFacedObject> getAvailableUnits(AreaDirectory ad,
            int band) {
        // get Vector array of Calibration types.   Layout is
        // v[i] = band[i] and for each band, it is a vector of
        // strings of calibration names and descriptions
        // n = name, n+1 = desc.
        // for radar, we only have one band
        if (ad == null) {
            return new ArrayList<TwoFacedObject>();
        }
        bandDefault = ALL;
        unitDefault = ALL;
        if (!bandDefault.equals(ALL)) {
            if (band != new Integer(bandDefault).intValue())
                return new ArrayList<TwoFacedObject>();
        }
        int index = 0;
        try {
            int[] bands = (int[]) bandTable.get(ad);
            index = (bands == null)
                          ? 0
                          : Arrays.binarySearch(bands, band);
        } catch (Exception e) {}
        if (index < 0) {
            index = 0;
        }
        Vector<TwoFacedObject> l = new Vector<TwoFacedObject>();
        Vector                 v                  = ad.getCalInfo()[index];
        TwoFacedObject         tfo                = null;
        int                    preferredUnitIndex = 0;
        String                 preferredUnit = getDefault(PROP_UNIT, "BRIT");
        if ((v != null) && (v.size() / 2 > 0)) {
            for (int i = 0; i < v.size() / 2; i++) {
                String name = (String) v.get(2 * i);
                String desc = (String) v.get(2 * i + 1);
                desc = desc.substring(0, 1).toUpperCase()
                       + desc.substring(1).toLowerCase();
                if (unitDefault.equals(name) || unitDefault.equals(ALL)) {
                    tfo = new TwoFacedObject(desc, name);
                    l.add(tfo);
                }
                if (name.equalsIgnoreCase(preferredUnit)) {
                    preferredUnitIndex = i;
                }
            }
        } else {
            l.add(new TwoFacedObject("Raw Value", "RAW"));
        }
        return l;
    }


    /**
     * Get the band name information from the server
     */
    protected void readSatBands() {
        satBandInfo        = null;
        sensorToBandToName = null;
        List lines = null;
        try {
            StringBuffer buff = getUrl(REQ_TEXT);
            appendKeyValue(buff, PROP_FILE, FILE_SATBAND);
            lines = readTextLines(buff.toString());
            if (lines == null) {
                return;
            }
            if (useSatBandInfo) {
                satBandInfo =
                    new AddeSatBands(StringUtil.listToStringArray(lines));
                return;
            }
        } catch (Exception e) {
            return;
        }

        if (lines == null) {
            return;
        }

        sensorToBandToName = new Hashtable();

        for (int i = 0; i < lines.size(); i++) {
            if ( !lines.get(i).toString().startsWith("Sat")) {
                continue;
            }
            List satIds = StringUtil.split(lines.get(i).toString(), " ",
                                           true, true);
            satIds.remove(0);
            Hashtable bandToName = new Hashtable();
            for (int j = i + 1; j < lines.size(); j++, i++) {
                String line = lines.get(i).toString();
                line = line.trim();
                if (line.startsWith("EndSat")) {
                    break;
                }

                int idx = line.indexOf(" ");
                if (idx < 0) {
                    continue;
                }
                String bandTok = line.substring(0, idx);
                try {
                    bandToName.put(Integer.decode(bandTok.trim()),
                                   line.substring(idx).trim());
                } catch (NumberFormatException nfe) {}
            }
            for (int j = 0; j < satIds.size(); j++) {
                Integer sensorId = new Integer(satIds.get(j).toString());
                sensorToBandToName.put(sensorId, bandToName);
            }
        }
    }


    /**
     * Make an AddeImageInfo from a URL and an AreaDirectory
     *
     * @param dir    AreaDirectory
     * @param isRelative true if is relative
     * @param num    number (for relative images)
     *
     * @return corresponding AddeImageInfo
     */
    protected AddeImageInfo makeImageInfo(AreaDirectory dir,
                                          boolean isRelative, int num) {
        AddeImageInfo info = new AddeImageInfo(getServer(),
                                 AddeImageInfo.REQ_IMAGEDATA, getGroup(),
                                 getDescriptor());
        if (isRelative) {
            info.setDatasetPosition((num == 0)
                                    ? 0
                                    : -num);
        } else {
            info.setStartDate(dir.getNominalTime());
        }
        setImageInfoProps(info, getMiscKeyProps(), dir);
        setImageInfoProps(info, getBaseUrlProps(), dir);

        String locKey   = getDefault(PROP_KEY, PROP_LINEELE);
        String locValue = null;
        info.setLocateKey(locKey);
        info.setLocateValue(locValue);

        String placeKey = getPropValue(PROP_PLACE, dir);
        info.setPlaceValue(placeKey);

        int             lmag   = 1;
        int             emag   = 1;
        info.setLineMag(lmag);
        info.setElementMag(emag);

        int    lines   = dir.getLines();
        int    elems   = dir.getElements();
        info.setLines(lines);
        info.setElements(elems);

        return info;
    }


    /**
     * Get the list of bands for the images
     *
     * @param ad   AreaDirectory
     * @param bands  list of bands
     * @return list of BandInfos for the selected images
     */
    protected List<BandInfo> makeBandInfos(AreaDirectory ad, int[] bands) {
        readSatBands();
        List<BandInfo> l = new ArrayList<BandInfo>();
        if (ad != null) {
            if (bands != null) {
                for (int i = 0; i < bands.length; i++) {
                    int      band = bands[i];
                    if (band > 0) {
                        BandInfo bi   = new BandInfo(ad.getSensorID(), band);
                        bi.setBandDescription(getBandName(ad, band));
                        bi.setCalibrationUnits(getAvailableUnits(ad, band));
                        bi.setPreferredUnit(getDefault(PROP_UNIT, "BRIT"));
                        l.add(bi);
                    }
                }
            }
        }
        return l;
    }


    /**
     * Get the list of BandInfos for the current selected images
     * @return list of BandInfos
     */
    public List<BandInfo> getSelectedBandInfos() {
        return bandInfos;
    }


    /**
     * User said go, we go. Simply get the list of images
     * from the imageChooser and create the ADDE.IMAGE
     * DataSource
     *
     */
    public void doLoadInThread() {
        if ( !getGoodToGo()) {
            updateStatus();
            return;
        }

        List imageList = getImageList();
        if ((imageList == null) || (imageList.size() == 0)) {
            return;
        }

        ImageDataset ids = new ImageDataset(getDatasetName(), imageList);
        // make properties Hashtable to hand the station name
        // to the AddeImageDataSource
        Hashtable ht = new Hashtable();
        getDataSourceProperties(ht);
        if (restElement != null) {
            if (restElement.hasAttribute(ATTR_BAND)) {
                ht.put(BAND_KEY, (Object)(restElement.getAttribute(ATTR_BAND)));
            }
            if (restElement.hasAttribute(ATTR_LATLON)) {
                ht.put(LATLON_KEY, (Object)(restElement.getAttribute(ATTR_LATLON)));
            }
            if (restElement.hasAttribute(ATTR_LINELE)) {
                ht.put(LINELE_KEY, (Object)(restElement.getAttribute(ATTR_LINELE)));
            }
            if (restElement.hasAttribute(ATTR_MAG)) {
                ht.put(MAG_KEY, (Object)(restElement.getAttribute(ATTR_MAG)));
            }
            if (restElement.hasAttribute(ATTR_PLACE)) {
                ht.put(PLACE_KEY, (Object)(restElement.getAttribute(ATTR_PLACE)));
            }
            if (restElement.hasAttribute(ATTR_SIZE)) {
                ht.put(SIZE_KEY, (Object)(restElement.getAttribute(ATTR_SIZE)));
            }
            if (restElement.hasAttribute(ATTR_UNIT)) {
                ht.put(UNIT_KEY, (Object)(restElement.getAttribute(ATTR_UNIT)));
            }
        }
        ht.put(PREVIEW_KEY, (Object)previewBox.isSelected());
        makeDataSource(ids, "ADDE.IMAGE.V", ht);
        saveServerState();
    }


    protected void setRestElement(Element elem) {
        restElement = elem;
    }


    protected void restoreBand(String band) {
        bandDefault = band;
    }


    protected void restoreUnit(String unit) {
        unitDefault = unit;
    }

    protected void restoreNav(String nav) {
        if (nav.equals("LALO")) {
            TwoFacedObject tfo = new TwoFacedObject("Lat/Lon", "LALO");
            navComboBox.setSelectedItem((Object)tfo);
        }
    }


    protected void resetDoAbsoluteTimes(boolean val) {
        setDoAbsoluteTimes(val);
        if (val == true) readTimes();
    }


    protected void restoreMag(String magStr) {
        String[] mags = getPair(magStr);
        Integer lint = new Integer(mags[0]);
        int lin = lint.intValue();
        lint = new Integer(mags[1]);
        int ele = lint.intValue();
    }


    /**
     * return the String id of the chosen server name
     *
     * @return  the server name
     */
    public String getServer() {
        Object selected = serverSelector.getSelectedItem();
        if (selected == null) {
            return null;
        }
        AddeServer server;
        if (selected instanceof AddeServer) {
            server = (AddeServer) selected;
            return server.getName();
        }
        return " ";
    }


    /**
     * Get the image group from the gui.
     *
     * @return The iamge group.
     */
    protected String getGroup() {
        Object selected = groupSelector.getSelectedItem();
        if (selected instanceof AddeServer.Group) {
            AddeServer.Group group = (AddeServer.Group) selected;
            return group.getName();
        }
        return (String)selected;
    }
}
