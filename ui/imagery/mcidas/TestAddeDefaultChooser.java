package ucar.unidata.ui.imagery.mcidas;


import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import ucar.unidata.idv.chooser.IdvChooser;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;

import ucar.unidata.ui.AddeChooser;
import ucar.unidata.ui.imagery.ImageSelector;
import ucar.unidata.ui.XmlUi;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlNodeList;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.UtcDate;

import visad.DateTime;

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.*;


/**
 * Test a new Image selector GUI.
 * Default group selector.
 */
public class TestAddeDefaultChooser extends AddeChooser implements ImageSelector {

    /** Property for the satband file */
    protected static final String FILE_SATBAND = "SATBAND";

    /** Property for image compress */
    protected static final String PROP_COMPRESS = "compress";
                                             
    /** Property for image default value descriptor */
    protected static final String PROP_DEBUG = "debug";

    /** Property for the port */
    protected static final String PROP_PORT = "port";

    /** Property for image default value version */
    protected static final String PROP_VERSION = "version";

    /** Property for image default value band */
    protected static final String PROP_BAND = "BAND";

    /** Property for accessing a file */
    protected static final String PROP_FILE = "file";

    /** Property for image default value key */
    protected static final String PROP_KEY = "key";

    /** Property for image default value lat/lon */
    protected static final String PROP_LATLON = "LATLON";

    /** Property for image default value line/ele */
    protected static final String PROP_LINEELE = "LINELE";

    /** Property for image default value loc */
    protected static final String PROP_LOC = "LOC";

    /** Property for image default value mag */
    protected static final String PROP_MAG = "MAG";

    /** Property for image default value place */
    protected static final String PROP_PLACE = "PLACE";

    /** Property for image default value size */
    protected static final String PROP_SIZE = "SIZE";

    /** Property for image default value spac */
    protected static final String PROP_SPAC = "SPAC";

    /** Property for image default value unit */
    protected static final String PROP_UNIT = "UNIT";

    /** Property for image default value day */
    protected static final String PROP_DAY = "DAY";

    /** Property for image default value time */
    protected static final String PROP_TIME = "TIME";

    /** This is the list of properties that are used in the advanced gui */
    private static final String[] ADVANCED_PROPS = {
        PROP_UNIT, PROP_BAND, PROP_PLACE, PROP_LOC, PROP_SIZE, PROP_MAG
    };
/*
    private static final String[] PANEL_NAMES = {
        "GOES-EAST", "GOES-WEST"
    };

    private static final String[] GROUP_NAMES = {
        "GINIEAST", "GINIWEST"
    };
*/

    /** Xml tag name for the defaults */
    protected static final String TAG_DEFAULT = "default";

    /** Default value for the compress property */
    protected static final String DEFAULT_COMPRESS = "true";

    /** Default value for the port property */
    protected static final String DEFAULT_PORT = "112";

    /** Default value for the debug property */
    protected static final String DEFAULT_DEBUG = "false";

    /** Default value for the version property */
    protected static final String DEFAULT_VERSION = "1";

    /** Default value for the user property */
    protected static String DEFAULT_USER = "idv";

    /** Default value for the proj property */
    protected static String DEFAULT_PROJ = "0";

    /** Xml attr names for the defaults */
    protected static final String ATTR_LABEL = "label";
    protected static final String ATTR_USER = "user";
    protected static final String ATTR_PROJ = "proj";
    protected static final String ATTR_DATASETS = "datasets";
    protected static final String ATTR_SERVER = "server";
    protected static final String ATTR_DATASET = "dataset";
    protected static final String ATTR_DATATYPE = "datatype";
    protected static final String ATTR_DAY = "day";
    protected static final String ATTR_TIME = "time";
    protected static final String ATTR_POS = "pos";
    protected static final String ATTR_UNIT = "unit";
    protected static final String ATTR_BAND = "band";
    protected static final String ATTR_KEY = "key";
    protected static final String ATTR_PLACE = "place";
    protected static final String ATTR_SIZE = "size";
    protected static final String ATTR_LOC = "loc";
    protected static final String ATTR_MAG = "mag";


    /** List of descriptors */
    private PreferenceList descList;

    /** Holds the mapping of id to xml Element */
    private Hashtable idToNodeList;

    /** This is a list of hashtables, one per imagedefaults resource. The Hashtables map the name to the xml node */
    private List resourceMaps;

    /** Holds the subsetting defaults */
    private XmlResourceCollection imageDefaults;

    /** A widget for the list of dataset descriptors */
    protected JComboBox defaultsCbx;

    /** Identifier for the maximum number of bands */
    int MAX_BANDS = 100;

    /** The last AreaDirectory we have seen. */
    AreaDirectory lastAD;

    /** The current AreaDirectory used for properties */
    AreaDirectory propertiesAD;

    /** Descriptor/name hashtable */
    protected Hashtable descriptorTable;

    /** Mapping of area directory to list of BandInfos */
    protected Hashtable bandTable;

    /**
     *  The list of currently loaded AddeImageDescriptor-s
     */
    private Vector imageDescriptors;

    /**
     * Mapping of sensor id (String) to hashtable that maps
     * Integer band number to name
     */
    private Hashtable sensorToBandToName;

    /** A flag so we can debug the new way of processing sat band file */
    private boolean useSatBandInfo = true;

    /** Used to parse the sat band file */
    private AddeSatBands satBandInfo;

    /**
     *  Keep track of which image load we are on.
     */
    private int currentTimestep = 0;

    private List defaultLabels = new ArrayList();
    private List servers = new ArrayList();
    private List datasets = new ArrayList();
    private List datatypes = new ArrayList();
    private List users = new ArrayList();
    private List projs = new ArrayList();
    private List days = new ArrayList();
    private List times = new ArrayList();
    private List positions = new ArrayList();
    private List units = new ArrayList();
    private List bandNumbers = new ArrayList();
    private List places = new ArrayList();
    private List locKeys = new ArrayList();
    private List sizes = new ArrayList();
    private List locs = new ArrayList();
    private List mags = new ArrayList();

    private List groupNames = new ArrayList();

    private int defaultIndex = 0;
    private static Boolean absoluteTimesFlag = new Boolean(false);

    private boolean satSelected = true;

    /** The user imagedefaults xml root */
    private Element imageDefaultsRoot;

    /** The user imagedefaults xml document */
    private Document imageDefaultsDocument;

    private List timesList = new ArrayList();

    /**
     * Construct an Adde image selection widget
     *
     * @param imageDefaults The xml resources for the image defaults
     * @param descList Holds the preferences for the image descriptors
     * @param serverList Holds the preferences for the adde servers
     */
    public TestAddeDefaultChooser(IdvChooser idvChooser,
                            XmlResourceCollection imageDefaults,
                            PreferenceList descList,
                            PreferenceList serverList,
                            String title) {
        super(idvChooser, serverList);

/*
        if (idvChooser != null) {
            simpleMode = !idvChooser.getProperty(IdvChooser.ATTR_SHOWDETAILS,
                    true);
        }
*/

        this.imageDefaults = imageDefaults;

        if (imageDefaults.hasWritableResource()) {
            imageDefaultsDocument =
                imageDefaults.getWritableDocument("<tabs></tabs>");
            imageDefaultsRoot = imageDefaults.getWritableRoot("<tabs></tabs>");
        }
/*
        String grp = new String(" ");
        for (int i = 0; i < PANEL_NAMES.length; i++) {
            if (PANEL_NAMES[i].equals(title)) {
                grp = GROUP_NAMES[i];
                break;
            }
        }
*/

        JComponent contents = this.getContents();
        contents.setName(title);
        getGroupNames(); 
        initializeDefaultLabels();
        setAvailableDefaultSets();
    }

    /**
     * Override base class method so we don't show the Help button.
     *
     * @return The array of button names.
     */
    protected String[] getButtonLabels() {
        return new String[]{ getLoadCommandName(), "Delete", GuiUtils.CMD_UPDATE,
                             GuiUtils.CMD_CANCEL };
    }


    /**
     * Get the name of the dataset.
     *
     * @return descriptive name of the dataset.
     */
    public String getDatasetName() {
        return getDescriptor();
    }

    /**
     * Get the name of the selected band
     * 
     * @return the name of the band
     */
    public String getSelectedBandName() {
        return getBandName(propertiesAD, getSelectedBand());
    }

    /**
     * Convenience method for lazy people who don't want to call
     * {@link ucar.unidata.util.LogUtil#logException(String, Throwable)}.
     *
     * @param msg    log message
     * @param exc    Exception to log
     */
    public void logException(String msg, Exception exc) {
        LogUtil.logException(msg, exc);
    }

    /**
     * Respond to a change in the descriptor list.
     */
    protected void descriptorChanged() {
        readTimes();
        updateStatus();
    }

    protected void updateStatus() {
        setHaveData(true);
    }

    /**
     * Handle when the user presses the update button
     *
     * @throws Exception On badness
     */
    public void handleUpdate() throws Exception {

        //JComponent contents = this.getContents();
        initializeDefaultLabels();
        if (setAvailableDefaultSets() > 0) {
            descriptorChanged();
        }
    }


    /**
     * Overwrite base class method to clear out the lastAD member here.
     */
    protected void clearTimesList() {
        lastAD = null;
        if (timesList.size() > 0) timesList.clear();
    }

    /**
     * Connect to the server.
     */
    protected void connectToServer() {
        setDescriptors(null);
        // set to relative times
        setDoAbsoluteTimes(false);
        readSatBands();
        readDescriptors();
        readTimes();
        //Save the server/group state
        saveServerState();
        ignoreStateChangedEvents = true;
        ignoreStateChangedEvents = false;
    }


    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    protected JComponent doMakeContents() {
        JComponent defsComp = getDefaultsComponent();
        defsComp = GuiUtils.inset(GuiUtils.label("Default Sets: ", defsComp),
                                4);

        JComponent defButtons = getDefaultButtons(this);
        
        JPanel boxWrapper = GuiUtils.doLayout(new Component[]{ defsComp }, 1,
                                              GuiUtils.WT_Y, GuiUtils.WT_N);
        //boxWrapper.setPreferredSize(new Dimension(200, 40));
        //GuiUtils.setHFill();

        JPanel comp = GuiUtils.vbox(boxWrapper, defButtons);

        if (defsComp != null) {
            return GuiUtils.vbox(GuiUtils.left(defsComp), comp);
        }
        return comp;
    }

    /**
     * Receive the update,cancel, load commands and call:
     * doUpdate, doCancel or doLoad.
     *
     * @param ae    ActionEvent to process
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals("Delete")) {
            doDelete();
        } else if (cmd.equals(GuiUtils.CMD_CANCEL)) {
            doCancel();
        } else if (cmd.equals(getLoadCommandName())) {
            doLoad();
        } else if (cmd.equals(GuiUtils.CMD_UPDATE)) {
            doUpdate();
        }

    }

    private void doDelete() {
        defaultIndex = defaultsCbx.getSelectedIndex();
        if (defaultIndex < 0) return;
        boolean deleteYN = GuiUtils.showYesNoDialog( null,
                            "Do you want to delete "
                            + getDefaultLabel(), "Delete Default Set");
        if (!deleteYN) return;
        removeDefaultSet();
    }

    private void removeDefaultSet() {
        String label = getDefaultLabel();
        deleteFromImagedefaults(label);
    }

    private void deleteFromImagedefaults(String label) {
        Element root = imageDefaultsRoot;
        if (root == null) {
            System.out.println("   Error reading imageDefaultsRoot");
            return;
        }

        XmlNodeList defaultNodes = XmlUtil.getElements(root, TAG_DEFAULT);
        for (int nodeIdx = 1; nodeIdx < defaultNodes.size(); nodeIdx++) {
            Element dfltNode = (Element) defaultNodes.item(nodeIdx);
            String nodeLabel = null;
            String nodeGroup = null;
            try {
                nodeLabel     = XmlUtil.getAttribute(dfltNode, ATTR_LABEL);
                nodeGroup     = XmlUtil.getAttribute(dfltNode, ATTR_DATASET);
            } catch (Exception e) {
                continue;
            }
            if (label.equals(nodeLabel)) {
                if (getGroup().equals(nodeGroup)) {
                    try {
                        root.removeChild(dfltNode);
                    } catch (Exception e) {
                    }
                    try {
                        imageDefaults.writeWritable();
                    } catch (Exception exc) {
                    }
                    imageDefaults.setWritableDocument(
                         imageDefaultsDocument, imageDefaultsRoot);
                    //JComponent contents = this.getContents();
                    initializeDefaultLabels();
                    int setCount = setAvailableDefaultSets();
                    defaultIndex = 0;
                    if (setCount > 0) {
                        defaultsCbx.setSelectedIndex(defaultIndex);
                    } else {
                        defaultsCbx.setSelectedIndex(-1);
                    }
                    break;
                }
            }
        }
    }

    private JComboBox getDefaultsComponent() {
        defaultsCbx = new JComboBox();
        return defaultsCbx;
    }

    private void getGroupNames() {
        JComponent contents = this.getContents();
        String title = contents.getName();
        if (imageDefaultsRoot == null) return;

        Element grpNode  = XmlUtil.findChild(imageDefaultsRoot, title);
        if (grpNode != null) {
            try {
                String names = XmlUtil.getAttribute(grpNode, ATTR_DATASETS);
                groupNames.clear();
                StringTokenizer tok = new StringTokenizer(names," ");
                while (tok.hasMoreElements()) {
                    groupNames.add(tok.nextToken());
                }
            } catch (Exception e) {
                System.out.println("e=" + e);
            }
        }
    }

    private void initializeDefaultLabels() {

        defaultLabels.clear();
        Element root = imageDefaultsRoot;
        int listIndex = 0;
        if (root != null) {

            //int listIndex = 0;
            XmlNodeList defaultNodes = XmlUtil.getElements(root, TAG_DEFAULT);
            for (int nodeIdx = 0; nodeIdx < defaultNodes.size(); nodeIdx++) {
                Element dfltNode = (Element) defaultNodes.item(nodeIdx);
                String dataset = null;
                String datatype = null;
                String server = null;
                String label = null;
                try {
                    label     = XmlUtil.getAttribute(dfltNode, ATTR_LABEL);
                } catch (Exception e) {
                    continue;
                }
                try {
                    dataset     = XmlUtil.getAttribute(dfltNode, ATTR_DATASET);
                } catch (Exception e) { 
                    StringTokenizer tok      = new StringTokenizer(label,"/");
                    try {
                        dataset = tok.nextToken();
                        datatype = tok.nextToken();
                    } catch (Exception ex) { 
                        continue;
                    };
                }
                for (int nameIndex=0; nameIndex<groupNames.size(); nameIndex++) {
                    if (dataset.equals(groupNames.get(nameIndex))) {
                //if (dataset.equals(grp)) {
                        datasets.add(listIndex, dataset);
                        defaultLabels.add(listIndex, label);
                        try {
                            server     = XmlUtil.getAttribute(dfltNode, ATTR_SERVER);
                        } catch (Exception e) { 
                            continue;
                        }
                        server = server.toLowerCase();
                        servers.add(listIndex, server);
                        if (datatype == null) {
                            try {
                                datatype     = XmlUtil.getAttribute(dfltNode, ATTR_DATATYPE);
                            } catch (Exception e) {
                                continue;
                            };
                        }
                        datatypes.add(listIndex, datatype);
    
                        String pos = null;
                        String day = null;
                        String time = null;
                        try {
                            pos     = XmlUtil.getAttribute(dfltNode, ATTR_POS);
                        } catch (Exception e) { };
                        if (pos == null) {
                            try {
                                day = XmlUtil.getAttribute(dfltNode, ATTR_DAY);
                            } catch (Exception e) {
                                continue;
                            };
                            try {
                                time = XmlUtil.getAttribute(dfltNode, ATTR_TIME);
                            } catch (Exception e) {
                                continue;
                            };
                        }
                        String user = null;
                        String proj = null;
                        try {
                            user = XmlUtil.getAttribute(dfltNode, ATTR_USER);
                            proj = XmlUtil.getAttribute(dfltNode, ATTR_PROJ);
                        } catch (Exception e) {
                            user = DEFAULT_USER;
                            proj = DEFAULT_PROJ;
                        }
                        user.toUpperCase();
                        users.add(listIndex, user);
                        projs.add(listIndex, proj);

                        days.add(listIndex, day);
                        times.add(listIndex, time);
                        positions.add(listIndex, pos);

                        String unit = null;
                        try {
                            unit     = XmlUtil.getAttribute(dfltNode, ATTR_UNIT);
                        } catch (Exception e) { };
                        units.add(listIndex, unit);
                        String band = null;
                        try {
                            band     = XmlUtil.getAttribute(dfltNode, ATTR_BAND);
                        } catch (Exception e) { };
                        bandNumbers.add(listIndex, band);
                        String key = null;
                        try {
                            key     = XmlUtil.getAttribute(dfltNode, ATTR_KEY);
                        } catch (Exception e) { };
                        locKeys.add(listIndex, key);
                        String place = null;
                        try {
                            place     = XmlUtil.getAttribute(dfltNode, ATTR_PLACE);
                        } catch (Exception e) { };
                        places.add(listIndex, place);
                        String size = null;
                        try {
                            size = XmlUtil.getAttribute(dfltNode, ATTR_SIZE);
                        } catch (Exception e) { };
                        sizes.add(listIndex, size);
                        String loc = null;
                        try {
                            loc = XmlUtil.getAttribute(dfltNode, ATTR_LOC);
                        } catch (Exception e) { };
                        locs.add(listIndex, loc);
                        String mag = null;
                        try {
                            mag = XmlUtil.getAttribute(dfltNode, ATTR_MAG);
                        } catch (Exception e) { };
                        mags.add(listIndex, mag);
    
                        listIndex++;
                    }
                }
            }
        }
    }

    /**
     * Set the available parameter default sets
     */
    private int setAvailableDefaultSets() {
        int labelCount = defaultLabels.size();
        if (labelCount > 0) {
            if (defaultsCbx != null) {
                GuiUtils.setListData(defaultsCbx, defaultLabels);
                return labelCount;
            }
        }
        return 0;
    }

    /**
     * Get the image group from the xml file.
     *
     * @return The image group.
     */
    protected String getGroup() {
        String dataset = null;
        if (defaultIndex >= 0) {
            dataset = datasets.get(defaultIndex).toString();
        }
        return dataset;
    }

    public String getServer() {
        String server = null;
        if (defaultIndex >= 0) {
            server = servers.get(defaultIndex).toString();
        }
        return server;
    }

    /**
     * Get the selected name.
     *
     * @return  the currently selected descriptor.
     */
    protected String getDefaultLabel() {
        return defaultLabels.get(defaultIndex).toString();
    }

    /**
     * Get the selected descriptor.
     *
     * @return  the currently selected descriptor.
     */
    protected String getDescriptor() {
        String datatype = null;
        if (defaultIndex >= 0) {
            datatype = datatypes.get(defaultIndex).toString();
        }
        return datatype;
    }

    /**
     * Get the selected user.
     *
     * @return  the currently selected user.
     */
    protected String getUser() {
        String user = null;
        if (defaultIndex >= 0) {
            user = users.get(defaultIndex).toString();
        }
        return user;
    }

    /**
     * Get the selected proj.
     *
     * @return  the currently selected proj.
     */
    protected String getProj() {
        String proj = null;
        if (defaultIndex >= 0) {
            proj = projs.get(defaultIndex).toString();
        }
        return proj;
    }

    /**
     * Get the selected day.
     *
     * @return  the currently selected day.
     */
    private String getDay() {
        String day = null;
        if (defaultIndex < days.size()) day=(String)(days.get(defaultIndex));
        return day;
    }

    /**
     * Get the selected time.
     *
     * @return  the currently selected time.
     */
    private String getTime() { 
        String time = null;
        if (defaultIndex < times.size()) time=(String)(times.get(defaultIndex));
        return time;
    }


    /**
     * Get the selected position.
     *
     * @return  the currently selected position.
     */
    private String getPosition() {
        String posS = null;
        setDoAbsoluteTimes(true);
        if (positions.get(defaultIndex) != null) {
           setDoAbsoluteTimes(false);
           posS = positions.get(defaultIndex).toString();
        }
        return posS;
    }

    /**
     * Get the selected calibration unit.
     *
     * @return  the selected calibration unit
     */
    protected String getSelectedUnit() {
        return units.get(defaultIndex).toString();
    }

    /**
     * Get the selected band from the advanced chooser
     *
     * @return selected band number
     */
    private int getSelectedBand() {
        String band = bandNumbers.get(defaultIndex).toString();
        Integer bandInt = new Integer(band);
        return bandInt.intValue();
    }

    /**
     * Get the selected location key.
     *
     * @return  the selected location key
     */
    protected String getSelectedKey() {
        return locKeys.get(defaultIndex).toString();
    }

    /**
     * Get the selected location place.
     *
     * @return  the selected location place
     */
    protected String getSelectedPlace() {
        return places.get(defaultIndex).toString();
    }

    /**
     * Get the value of the size.
     *
     * @return The size value
     */
    private String getSelectedSizes() {
        return sizes.get(defaultIndex).toString();
    }

    /**
     * Get the value of the location.
     *
     * @return The location value
     */
    private String getSelectedLocs() {
        return locs.get(defaultIndex).toString();
    }

    /**
     * Get the value of the magnification.
     *
     * @return The magnification value
     */
    private String getSelectedMags() {
        return mags.get(defaultIndex).toString();
    }


    /**
     *  Read the set of image times available for the current server/group/type
     *  This method is a wrapper, setting the wait cursor and wrapping the
     *  call to {@link #readTimesInner(int)}; in a try/catch block
     */
    protected void readTimes() {
        clearTimesList();
        updateStatus();
        showWaitCursor();
        try {
            currentTimestep++;
            readTimesInner(currentTimestep);
        } catch (Exception e) {
            System.out.println("    readTimesInner e=" + e);
            handleConnectionError(e);
        }
        showNormalCursor();
        updateStatus();
    }


    /**
     * Set the list of dates/times based on the image selection
     *
     * @param timestep    the timestep for the image selection
     */
    protected void readTimesInner(int timestep) {
        String       descriptor  = getDescriptor();
        StringBuffer addeCmdBuff = getGroupUrl(REQ_IMAGEDIR, getGroup());
        appendKeyValue(addeCmdBuff, PROP_DESCR, descriptor);
        String day = getDay();
        String time = getTime();
        String pos = getPosition();
        if (pos == null)  {
            setAbsoluteTimesFlag();
            appendKeyValue(addeCmdBuff, PROP_DAY, "" + day);
            appendKeyValue(addeCmdBuff, PROP_TIME, "" + time);
        } else {
            appendKeyValue(addeCmdBuff, PROP_POS, "" + pos);
        }
        loadImages(addeCmdBuff.toString(), timestep);
    }


/** locking mutex */
    private Object MUTEX = new Object();



    /**
     * Load the images for the given URL and timestep
     *
     * @param url          ADDE URL
     * @param timestep     valid timestep
     */
    protected void loadImages(String url, int timestep) {
        try {
            AreaDirectoryList adir = new AreaDirectoryList(url);
            //Make sure no other loads are  occurred
/*
            if (timestep != currentTimestep) {
                return;
            }
*/
            synchronized (MUTEX) {
                // Array of AreaDirectory-s sorted by time
                AreaDirectory[][] dirs      = adir.getSortedDirs();
                int               numImages = dirs.length;
                imageDescriptors = new Vector();
                //TODO:  Add a setBands method to AreaDirectory to replace
                // bandTable
                bandTable = new Hashtable(numImages);
                lastAD    = null;
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
                }
                Collections.sort(imageDescriptors);
                if (getDoAbsoluteTimes()) {
                    timesList.addAll(imageDescriptors);
                }
/*
                getTimesList().setVisibleRowCount(Math.min(numImages,
                        getTimesListSize()));
                getTimesList().ensureIndexIsVisible(
                    getTimesList().getSelectedIndex());
                revalidate();
*/
            }
            setState(STATE_CONNECTED);
        } catch (McIDASException e) {
            System.out.println("    loadImages e=" + e);
            handleConnectionError(e);
        }
    }


    /**
     *  Generate a list of image descriptors for the descriptor list.
     */
    private void readDescriptors() {
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
            System.out.println("    readDescriptors e=" + e);
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
            if ((names == null) || (names.length == 0)) {
                return;
            }
        }
    }


    /**
     * Returns a list of the images to load or null if none have been
     * selected.
     *
     * @return  list  get the list of image descriptors
     */
    public List getImageList() {
        List images = new ArrayList();
        readTimesInner(1);
        if (getPosition() != null  ) {
            AddeImageDescriptor firstDescriptor =
                (AddeImageDescriptor) imageDescriptors.get(0);
            int[] relativeTimesIndices = getRelativeTimeIndices();
            for (int i = 0; i < relativeTimesIndices.length; i++) {
                AddeImageDescriptor aid = new AddeImageDescriptor(relativeTimesIndices[i],
                                              firstDescriptor);
                AddeImageInfo aii = makeImageInfo(aid.getDirectory(), true,
                                        relativeTimesIndices[i]);
                aid.setImageInfo(aii);
                aid.setSource(aii.makeAddeUrl());
                images.add(aid);
            }
        } else {
/*
            Object[] selectedTimes = timesList.get(0);
            System.out.println("    selectedTimes.length=" + selectedTimes.length);
            for (int i = 0; i < selectedTimes.length; i++) {
*/
                AddeImageDescriptor aid =
                    new AddeImageDescriptor(
                        (AddeImageDescriptor) timesList.get(0));
                AddeImageInfo aii = makeImageInfo(aid.getDirectory(), false,
                                        0);
                aid.setImageInfo(aii);
                aid.setSource(aii.makeAddeUrl());
                images.add(aid);
//            }
        }
        return images;
    }

    /**
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
    private void initializeImageDefaults() {
        idToNodeList = new Hashtable();
        resourceMaps = new ArrayList();
        if (imageDefaults == null) {
            return;
        }
        for (int resourceIdx = 0; resourceIdx < imageDefaults.size();
                resourceIdx++) {
            Element root = imageDefaults.getRoot(resourceIdx);
            if (root == null) {
                continue;
            }
            Hashtable resourceMap = new Hashtable();
            resourceMaps.add(resourceMap);

            XmlNodeList defaultNodes = XmlUtil.getElements(root, TAG_DEFAULT);
            String label = null;
            for (int nodeIdx = 0; nodeIdx < defaultNodes.size(); nodeIdx++) {
                Element dfltNode = (Element) defaultNodes.item(nodeIdx);
                try {
                    label     = XmlUtil.getAttribute(dfltNode, ATTR_LABEL);
                } catch (Exception e) {
                    continue;
                }
                resourceMap.put(label, dfltNode);
                List nodes = (List) idToNodeList.get(label);
                if (nodes == null) {
                    idToNodeList.put(label, nodes = new ArrayList());
                }
                nodes.add(dfltNode);
            }
        }
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
            initializeImageDefaults();
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
                if (value.equals(TAG_DEFAULT)) {
                    return dflt;
                } else {
                    return value;
                }
            }
        }
        return dflt;
    }



    /**
     * Create the appropriate request string for the image.
     *
     * @param ad  <code>AreaDirectory</code> for the image in question.
     * @param doTimes  true if this is for absolute times, false for relative
     * @param cnt  image count (position in dataset)
     *
     * @return  the ADDE request URL
     */
    protected String getBaseUrl(AreaDirectory ad, boolean doTimes, int cnt) {
        StringBuffer buf = getGroupUrl(REQ_IMAGEDATA, getGroup());
        buf.append(makeDateTimeString(ad, cnt, doTimes));
        buf.append(makeProps(getBaseUrlProps(), ad));
        return buf.toString();
    }

    /**
     * Get the list of properties for the base URL
     * @return list of properties
     */
    protected String[] getBaseUrlProps() {
        return new String[] { PROP_DESCR, PROP_SIZE, PROP_UNIT, PROP_SPAC,
                              PROP_BAND, PROP_MAG, PROP_PLACE};
    }

    /**
     * A utility that creates the url argument  line for the given set of properties.
     *
     * @param props The PROP_ properties to make the request string for
     * @param ad The area directory.
     *
     * @return The adde request string
     */
    protected String makeProps(String[] props, AreaDirectory ad) {
        StringBuffer buf = new StringBuffer();
        for (int propIdx = 0; propIdx < props.length; propIdx++) {
            appendKeyValue(buf, props[propIdx],
                           getPropValue(props[propIdx], ad));
        }
        return buf.toString();
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
        appendKeyValue(buff, PROP_USER, getUser());
        appendKeyValue(buff, PROP_PROJ, getProj());
    }

    /**
     * Get the user supplied property value for the adde request string
     *
     * @param prop The property
     * @param ad The AreaDirectory
     *
     * @return The value, supplied by the user, of the property to use
     *         in the request string
     */
    protected String getPropValue(String prop, AreaDirectory ad) {
        if (prop.equals(PROP_DESCR)) {
            return getDescriptor();
        }
        if (prop.equals(PROP_LATLON) || prop.equals(PROP_LINEELE)) {
            return getSelectedLocs();
        }
        if (prop.equals(PROP_PLACE)) {
            return getSelectedPlace();
        }
        if (prop.equals(PROP_SIZE)) {
            return getSelectedSizes();
        }
        if (prop.equals(PROP_MAG)) {
            return getSelectedMags();
        }
        if (prop.equals(PROP_BAND)) {
            return bandNumbers.get(defaultIndex).toString();
        }
        if (prop.equals(PROP_UNIT)) {
            return getSelectedUnit();
        }
        return null;
    }


    /**
     * Get the band name for a particular area
     *
     * @param ad AreaDirectory
     * @param band band number
     *
     * @return name of the band
     */
    private String getBandName(AreaDirectory ad, int band) {

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
        /*
        else {
            name = band + " - " + name.trim();
        }
        */
        return name;
    }


    /**
     * Read the adde text url and return the lines of text.
     * If unsuccessful return null.
     *
     * @param url adde url to a text file
     *
     * @return List of lines or null if in error
     */
    private List readTextLines(String url) {
        AddeTextReader reader = new AddeTextReader(url);
        if ( !reader.getStatus().equals("OK")) {
            return null;
        }
        return reader.getLinesOfText();
    }


    /**
     * Get the band name information from the server
     */
    private void readSatBands() {
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
     * Method to do the work of loading the data
     */
    public void doLoad() {
        defaultIndex = defaultsCbx.getSelectedIndex();
        if (defaultIndex < 0) return;

        try {
            handleUpdate();
        } catch (Exception exc) {
            logException("handleUpdate", exc);
        }

        try {
            firePropertyChange(NEW_SELECTION, null, getImageList());
        } catch (Exception exc) {
            logException("doLoad", exc);
        }
    }

    private void setAbsoluteTimesFlag() {
        String str = getPosition();
        absoluteTimesFlag = new Boolean(true);
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
        String baseUrl  = getBaseUrl(dir, !isRelative, num);
        String locKey   = getDefault(PROP_KEY, PROP_LINEELE);
        String locValue = null;
        String keyString = getSelectedKey();
        if (keyString.equals((Object)PROP_LATLON)) {
            locKey = PROP_LATLON;
            locValue = getPropValue(PROP_LATLON, dir);
        } else {
            locKey = PROP_LINEELE;
            locValue = getPropValue(PROP_LINEELE, dir);
        }
        String          placeKey = getPropValue(PROP_PLACE, dir);
        String          magKey   = getPropValue(PROP_MAG, dir);
        int             lmag     = 1;
        int             emag     = 1;
        StringTokenizer tok      = new StringTokenizer(magKey);
        lmag = (int) Misc.parseValue((String) tok.nextElement());
        if (tok.hasMoreTokens()) {
            emag = (int) Misc.parseValue((String) tok.nextElement());
        } else {
            emag = lmag;
        }
        int    lines   = dir.getLines();
        int    elems   = dir.getElements();
        String sizeKey = getPropValue(PROP_SIZE, dir);
        tok = new StringTokenizer(sizeKey);
        String size = (String) tok.nextElement();
        if ( !size.equalsIgnoreCase("all")) {
            lines = (int) Misc.parseValue(size);
            if (tok.hasMoreTokens()) {
                elems = (int) Misc.parseValue((String) tok.nextElement());
            } else {
                elems = lines;
            }
        }
        return new AddeImageInfo(baseUrl, locKey, locValue, placeKey, lines,
                                 elems, lmag, emag);
    }
}
