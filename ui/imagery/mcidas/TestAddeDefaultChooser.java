package ucar.unidata.ui.imagery.mcidas;


import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.*;

import org.w3c.dom.Element;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;

import ucar.unidata.ui.AddeChooser;
import ucar.unidata.ui.imagery.ImageSelector;

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

    /** This is the list of properties that are used in the advanced gui */
    private static final String[] ADVANCED_PROPS = {
        PROP_UNIT, PROP_BAND, PROP_PLACE, PROP_LOC, PROP_SIZE, PROP_MAG
    };


    /** Xml tag name for the defaults */
    protected static final String TAG_DEFAULT = "default";

    /** Xml attr names for the defaults */
    protected static final String ATTR_NAME = "name";
    protected static final String ATTR_SERVER = "server";
    protected static final String ATTR_DATASET = "dataset";
    protected static final String ATTR_DATATYPE = "datatype";
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

    private List servers = new ArrayList();
    private List datasets = new ArrayList();
    private List datatypes = new ArrayList();
    private List positions = new ArrayList();
    private List units = new ArrayList();
    private List bandNumbers = new ArrayList();
    private List places = new ArrayList();
    private List locKeys = new ArrayList();
    private List sizes = new ArrayList();
    private List locs = new ArrayList();
    private List mags = new ArrayList();

    private int defaultIndex = 0;

    /**
     * Construct an Adde image selection widget
     *
     * @param imageDefaults The xml resources for the image defaults
     * @param descList Holds the preferences for the image descriptors
     * @param serverList Holds the preferences for the adde servers
     */
    public TestAddeDefaultChooser(XmlResourceCollection imageDefaults,
                            PreferenceList descList,
                            PreferenceList serverList) {
        super(serverList);
        this.imageDefaults = imageDefaults;
        setLayout(new BorderLayout(5, 5));

        JComponent contents = doMakeContents();

        this.add(contents, BorderLayout.CENTER);
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

    /**
     * Handle when the user presses the update button
     *
     * @throws Exception On badness
     */
    public void handleUpdate() throws Exception {
        descriptorChanged();
    }


    /**
     * Overwrite base class method to clear out the lastAD member here.
     */
    protected void clearTimesList() {
        lastAD = null;
        super.clearTimesList();
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

        JPanel defButtons = getDefaultButtons();
        JPanel boxWrapper = GuiUtils.doLayout(new Component[]{ defsComp }, 1,
                                              GuiUtils.WT_Y, GuiUtils.WT_N);
        boxWrapper.setPreferredSize(new Dimension(200, 40));
        GuiUtils.setHFill();

        JPanel comp = GuiUtils.vbox(boxWrapper, defButtons);
        setHaveData(true);

        if (defsComp != null) {
            return GuiUtils.vbox(GuiUtils.left(defsComp), comp);
        }
        return comp;
    }


    private JComboBox getDefaultsComponent() {
        defaultsCbx = new JComboBox();
        List defaultNames = new ArrayList();

        if (imageDefaults == null) {
            return null;
        }
        for (int resourceIdx = 0; resourceIdx < imageDefaults.size();
                resourceIdx++) {
            Element root = imageDefaults.getRoot(resourceIdx);
            if (root == null) {
                continue;
            }

            XmlNodeList defaultNodes = XmlUtil.getElements(root, TAG_DEFAULT);
            for (int nodeIdx = 1; nodeIdx < defaultNodes.size(); nodeIdx++) {
                Element dfltNode = (Element) defaultNodes.item(nodeIdx);
                String  name     = XmlUtil.getAttribute(dfltNode, ATTR_NAME);
                if (name != null) {
                    defaultNames.add(name);
                }
                String  server     = XmlUtil.getAttribute(dfltNode, ATTR_SERVER);
                if (server != null) {
                    server = server.toLowerCase();
                    servers.add(server);
                }
                String  dataset     = XmlUtil.getAttribute(dfltNode, ATTR_DATASET);
                if (dataset != null) {
                    datasets.add(dataset);
                }
                String  datatype     = XmlUtil.getAttribute(dfltNode, ATTR_DATATYPE);
                if (datatype != null) {
                    datatypes.add(datatype);
                }
                String  pos     = XmlUtil.getAttribute(dfltNode, ATTR_POS);
                if (pos != null) {
                    pos = pos.toLowerCase();
                    positions.add(pos);
                    setDoAbsoluteTimes(false);
                } else {
                    setDoAbsoluteTimes(true);
                }
                String  unit     = XmlUtil.getAttribute(dfltNode, ATTR_UNIT);
                if (unit != null) {
                    units.add(unit);
                }
                String  band     = XmlUtil.getAttribute(dfltNode, ATTR_BAND);
                if (band != null) {
                    bandNumbers.add(band);
                }
                String  key     = XmlUtil.getAttribute(dfltNode, ATTR_KEY);
                if (key != null) {
                    locKeys.add(key);
                }
                String  place     = XmlUtil.getAttribute(dfltNode, ATTR_PLACE);
                if (place != null) {
                    places.add(place);
                }
                String size = XmlUtil.getAttribute(dfltNode, ATTR_SIZE);
                if (size != null) {
                       sizes.add(size);
                }
                String loc = XmlUtil.getAttribute(dfltNode, ATTR_LOC);
                if (loc != null) {
                       locs.add(loc);
                }
                String mag = XmlUtil.getAttribute(dfltNode, ATTR_MAG);
                if (mag != null) {
                       mags.add(mag);
                }
            }
        }
        GuiUtils.setListData(defaultsCbx, defaultNames);
        return defaultsCbx;
    }


    /**
     * Get the image group from the xml file.
     *
     * @return The image group.
     */
    protected String getGroup() {
        return datasets.get(defaultIndex).toString();
    }

    /**
     * Get the selected descriptor.
     *
     * @return  the currently selected descriptor.
     */
    protected String getDescriptor() {
        return datatypes.get(defaultIndex).toString();
    }

    /**
     * Get the selected position.
     *
     * @return  the currently selected position.
     */
    private int getPosition() {
        String posS = positions.get(defaultIndex).toString();
        Integer posInt = new Integer(posS);
        return posInt.intValue();
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
        String       pos         = (getDoAbsoluteTimes()
                                    ? "all"
                                    : "0");


        StringBuffer addeCmdBuff = getGroupUrl(REQ_IMAGEDIR, getGroup());
        appendKeyValue(addeCmdBuff, PROP_DESCR, descriptor);
        appendKeyValue(addeCmdBuff, PROP_POS, "" + pos);
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
            if (timestep != currentTimestep) {
                return;
            }
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
                    getTimesList().setListData(imageDescriptors);
                }
                getTimesList().setVisibleRowCount(Math.min(numImages,
                        getTimesListSize()));
                getTimesList().ensureIndexIsVisible(
                    getTimesList().getSelectedIndex());
                revalidate();
            }
            setState(STATE_CONNECTED);
        } catch (McIDASException e) {
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
        if (getDoRelativeTimes()) {
            AddeImageDescriptor firstDescriptor =
                (AddeImageDescriptor) imageDescriptors.get(0);
            for (int i = 0; i < getNumRelativeTimes(); i++) {
                AddeImageDescriptor aid = new AddeImageDescriptor(i,
                                              firstDescriptor);
                AddeImageInfo aii = makeImageInfo(aid.getDirectory(), true,
                                        i);
                aid.setImageInfo(aii);
                aid.setSource(aii.makeAddeUrl());
                images.add(aid);
            }
        } else {
            Object[] selectedTimes = getTimesList().getSelectedValues();
            for (int i = 0; i < selectedTimes.length; i++) {
                AddeImageDescriptor aid =
                    new AddeImageDescriptor(
                        (AddeImageDescriptor) selectedTimes[i]);
                AddeImageInfo aii = makeImageInfo(aid.getDirectory(), false,
                                        i);
                aid.setImageInfo(aii);
                aid.setSource(aii.makeAddeUrl());
                images.add(aid);
            }
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
            for (int nodeIdx = 0; nodeIdx < defaultNodes.size(); nodeIdx++) {
                Element dfltNode = (Element) defaultNodes.item(nodeIdx);
                String  name     = XmlUtil.getAttribute(dfltNode, ATTR_NAME);
                resourceMap.put(name, dfltNode);
                List nodes = (List) idToNodeList.get(name);
                if (nodes == null) {
                    idToNodeList.put(name, nodes = new ArrayList());
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
        doConnect();
        try {
            firePropertyChange(NEW_SELECTION, null, getImageList());
        } catch (Exception exc) {
            logException("doLoad", exc);
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
