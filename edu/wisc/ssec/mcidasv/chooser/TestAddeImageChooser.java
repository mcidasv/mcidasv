/*
 * $Id$
 * 
 * Copyright 2007-2008 Space Science and Engineering Center (SSEC) University
 * of Wisconsin - Madison, 1225 W. Dayton Street, Madison, WI 53706, USA
 * 
 * http://www.ssec.wisc.edu/mcidas
 * 
 * This file is part of McIDAS-V.
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser Public License along with
 * this program. If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.chooser;

import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.data.imagery.BandInfo;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.ui.ChooserList;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlResourceCollection;

import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.adde.AddeSatBands;
import edu.wisc.ssec.mcidas.adde.AddeURL;
import edu.wisc.ssec.mcidas.adde.DataSetInfo;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.ServerPreferenceManager;
import edu.wisc.ssec.mcidasv.chooser.adde.AddeImageChooser;

/**
 * Widget to select images from a remote ADDE server Displays a list of the
 * descriptors (names) of the image datasets available for a particular ADDE
 * group on the remote server.
 * 
 * @author Don Murray
 */
public class TestAddeImageChooser extends AddeImageChooser implements
    ucar.unidata.ui.imagery.ImageSelector {

    /**
     * 
     */
    private static final long serialVersionUID = -4229247286953750952L;

    /** Command for connecting */
    protected static final String CMD_MANAGER = "cmd.manager";

    private static final int SIZE_THRESHOLD = 30;

    /** monospaced font */
    private final Font monoFont = null;

    /** default magnification */
    private static final int DEFAULT_MAG = 0;

    /** flag for center */
    private static final String PLACE_CENTER = "CENTER";

    /** flag for upper left */
    private static final String PLACE_ULEFT = "ULEFT";

    /** flag for lower left */
    private static final String PLACE_LLEFT = "LLEFT";

    /** flag for upper right */
    private static final String PLACE_URIGHT = "URIGHT";

    /** flag for lower right */
    private static final String PLACE_LRIGHT = "LRIGHT";

    /** Property for the descriptor table */
    public static final String DESCRIPTOR_TABLE = "DESCRIPTOR_TABLE";

    /** Property for the satband file */
    protected static final String FILE_SATBAND = "SATBAND";

    /** Property for image default value band */
    protected static final String PROP_BAND = "BAND";

    /** Property for image default value id */
    protected static final String PROP_ID = "ID";

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

    /** Property for num */
    protected static final String PROP_NUM = "NUM";

    /** Property for image default value place */
    protected static final String PROP_PLACE = "PLACE";

    /** Property for image default value size */
    protected static final String PROP_SIZE = "SIZE";

    /** Property for image default value spac */
    protected static final String PROP_SPAC = "SPAC";

    /** Property for image default value unit */
    protected static final String PROP_UNIT = "UNIT";

    /** Property for image default value unit */
    protected static final String PROP_NAV = "NAV";

    /** This is the list of properties that are used in the advanced gui */
    private static final String[] ADVANCED_PROPS =
        { PROP_UNIT, PROP_BAND, PROP_PLACE, PROP_LOC, PROP_SIZE, PROP_MAG,
         PROP_NAV };

    /** This is the list of labels used for the advanced gui */
    private static final String[] ADVANCED_LABELS =
        { "Data Type:", "Channel:", "Placement:", "Location:", "Image Size:",
         "Magnification:", "Navigation Type:" };

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

    /** Xml attr name for the defaults */
    private static final String ATTR_UNIT = "UNIT";

    private static final String ATTR_BAND = "BAND";

    private static final String ATTR_PLACE = "PLACE";

    private static final String ATTR_LOC = "LOC";

    private static final String ATTR_SIZE = "SIZE";

    private static final String ATTR_MAG = "MAG";

    private static final String ATTR_NAV = "NAV";

    private static final String ATTR_DESCRIPTOR = "DESCRIPTOR";

    private static final String ATTR_GROUP = "GROUP";

    private static final String ATTR_SERVER = "SERVER";

    private static final String ATTR_LATLON = "LATLON";

    private static final String ATTR_LINELE = "LINELE";

    private static final String ATTR_POS = "POS";

    private static final String ATTR_DAY = "DAY";

    private static final String ATTR_TIME = "TIME";

    private static final String ATTR_USER = "USER";

    private static final String ATTR_PROJ = "PROJ";

    private static final String ATTR_KEY = "key";

    /** Attribute names for imagedefaults.xml */
    private static final String[] ATTRS =
        { ATTR_KEY, ATTR_UNIT, ATTR_BAND, ATTR_PLACE, ATTR_LOC, ATTR_SIZE,
         ATTR_MAG, ATTR_NAV, ATTR_DESCRIPTOR, ATTR_GROUP, ATTR_SERVER,
         ATTR_LATLON, ATTR_LINELE, ATTR_POS, ATTR_DAY, ATTR_TIME, ATTR_USER,
         ATTR_PROJ };

    /** Selection label text */
    protected static final String LABEL_SELECT = " -- Select -- ";

    /** flag for setting properties */
    private boolean amSettingProperties = false;

    /** Are we currently reading times */
    private Object readTimesTask;

    /** archive date */
    private String archiveDay = null;

    /** List of descriptors */
    private PreferenceList descList;

    /** Holds the properties */
    private JPanel propPanel;

    /** archive day button */
    private JComponent archiveDayComponent;

    /** archive day button */
    private JLabel archiveDayLabel;

    /** Maps the PROP_ property name to the gui component */
    private final Hashtable propToComps = new Hashtable();

    /**
     * This is a list of hashtables, one per imagedefaults resource. The
     * Hashtables map the pattern to the xml node
     */
    private List resourceMaps;

//    /** Holds the subsetting defaults */
//    private XmlResourceCollection addeServerRSC;
//
    private final XmlResourceCollection imageDefaultsRSC;

    /** the center load point */
    private String centerPoint;

    /** archive date formatter */
    private SimpleDateFormat archiveDayFormatter;

    /**
     * List of JComponent-s that depend on a descriptor being selected to be
     * enabled
     */
    protected ArrayList compsThatNeedDescriptor = new ArrayList();

    /** Flag to keep from infinite looping */
    private boolean ignoreDescriptorChange = false;

    /** The descriptor names */
    protected String[] descriptorNames;

    /** Input for lat/lon center point */
    protected LatLonWidget latLonWidget;

    /** base number of lines */
    private double baseNumLines = 0.0;

    /** size label */
    JLabel sizeLbl;

    /** base number of lines */
    private double baseNumElements = 0.0;

    /** Widget to hold the number of elements in the advanced */
    JTextField numElementsFld;

    /** Widget to hold the number of lines in the advanced */
    JTextField numLinesFld;

    /** Widget for the line center point in the advanced section */
    protected JTextField centerLineFld;

    /** Widget for the element center point in the advanced section */
    protected JTextField centerElementFld;

    protected JToggleButton lockBtn;

    /** Label used for the line center */
    private JLabel centerLineLbl;

    /** Label used for the element center */
    private JLabel centerElementLbl;

    /** Label used for the center latitude */
    private JLabel centerLatLbl;

    /** Label used for the center longitude */
    private JLabel centerLonLbl;

    /** Identifier for the maximum number of bands */
    int MAX_BANDS = 100;

    /** The last AreaDirectory we have seen. */
    AreaDirectory lastAD;

    /** The current AreaDirectory used for properties */
    AreaDirectory propertiesAD;

    /** The previous AreaDirectory used for properties */
    AreaDirectory prevPropertiesAD;

    /** Descriptor/name hashtable */
    protected Hashtable descriptorTable;

    /** Mapping of area directory to list of BandInfos */
    protected Hashtable bandTable;

    /**
     * The list of currently loaded AddeImageDescriptor-s
     */
    private Vector imageDescriptors;

    /** maximum size for the widget */
    private static final int MAX_SIZE = 700;

    /** Widget for selecting image units */
    protected JComboBox unitComboBox;

    /** place label */
    private JLabel placeLbl;

    /** the place string */
    private String place;

    /** location panel */
    private GuiUtils.CardLayoutPanel locationPanel;

    /** Widget for selecting image nav type */
    protected JComboBox navComboBox;

    /**
     * Mapping of sensor id (String) to hashtable that maps Integer band number
     * to name
     */
    private Hashtable sensorToBandToName;

    /** A flag so we can debug the new way of processing sat band file */
    private final boolean useSatBandInfo = true;

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
     * Keep track of which image load we are on.
     */
    private final int currentTimestep = 0;

    /**
     * Keep track of the lines to element ratio
     */
    private double linesToElements = 1.0;

    /**
     * Only changes in ele slider should change ratio
     */
    private final boolean recomputeLineEleRatio = true;

    /**
     * limit of slider
     */
    private static final int SLIDER_MAX = 29;

    /**
     * the list of band infos
     */
    private List<BandInfo> bandInfos;

    /** Some more useful server methods */
    private ServerInfo serverInfo = null;

    /** Default value for the user property */
    protected static String DEFAULT_USER = "idv";

    /** Default value for the proj property */
    protected static String DEFAULT_PROJ = "0";

    /** Accounting information */
    private static String user;

    private static String proj;

    /** My servers */
    private List addeServers;

    private static int mainIndex;

    protected JTabbedPane tabbedPane;

    private int descIndex;

    /**
     * Construct an Adde image selection widget
     * 
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public TestAddeImageChooser(final IdvChooserManager mgr, final Element root) {
        super(mgr, root);
        imageDefaultsRSC = getImageDefaults();
//        addeServerRSC = getAddeServers();
        Element imageDefaultsRoot;
        Document imageDefaultsDoc;
        if (imageDefaultsRSC.hasWritableResource()) {
            imageDefaultsDoc = imageDefaultsRSC.getWritableDocument("<imagedefaults></imagedefaults>");
            imageDefaultsRoot = imageDefaultsRSC.getWritableRoot("<imagedefaults></imagedefaults>");
        }
        try {
            imageDefaultsRSC.writeWritable();
        } catch (Exception e) {
        }
//        serverInfo = getServerInfo();
//        user = serverInfo.getUser();
//        proj = serverInfo.getProj();

        DEFAULT_USER = ServerPreferenceManager.getDefaultUser();
        DEFAULT_PROJ = ServerPreferenceManager.getDefaultProject();

        allServersFlag = getAllServersFlag();

        serverManager = ((McIDASV)getIdv()).getServerManager();
        serverManager.addManagedChooser(this);

        updateServers();
        loadServerState();
    }

    private boolean getAllServersFlag() {
        return getIdv().getStore().get(Constants.PREF_SYSTEMSERVERSIMG, true);
    }

    protected int getMainIndex() {
        return mainIndex;
    }


    protected void setServerOnly(final String serverName) {
        AddeServer newServer = AddeServer.findServer(addeServers, serverName);
        if (newServer != null) {
            descIndex = -1;
            getServerSelector().setSelectedItem(newServer);
            updateGroups();
        }
    }

    protected void setGroupOnly(final String groupName) {
        if (groupSelector != null) {
            groupSelector.setSelectedItem(groupName);
        }
    }

    /**
     * get the adde server grup type to use
     * 
     * @return group type
     */
    @Override protected String getGroupType() {
        return AddeServer.TYPE_IMAGE;
    }

    /**
     * Make the UI for this selector.
     * 
     * @return The gui
     */
    protected JComponent doMakeContents() {
        List allComps = processServerComponents();
        getComponents(allComps);
        allComps.addAll(processPropertyComponents());
        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel imagePanel =
            GuiUtils.doLayout(allComps, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
        tabbedPane = new JTabbedPane();
        JPanel mainPanel =
            GuiUtils.centerBottom(imagePanel, getDefaultButtons(this));
        mainPanel.setName("Satellite Imagery");
        tabbedPane.add(mainPanel);
        mainIndex = tabbedPane.getSelectedIndex();
        ImageParametersTab ip = new ImageParametersTab(this, tabbedPane);
        JPanel parameterSetsPanel = ip.doMakeContents();
        tabbedPane.addTab("Parameter Sets", parameterSetsPanel);
        return tabbedPane;
    }

    protected void setDescriptorOnly(final String descriptorName) {
        String newName = null;
        try {
            StringBuffer buff = getGroupUrl(REQ_DATASETINFO, getGroup());
            DataSetInfo dsinfo = new DataSetInfo(buff.toString());
            descriptorTable = dsinfo.getDescriptionTable();
            String[] names = new String[descriptorTable.size()];
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
            // System.out.println("from setDescriptor...");
            handleConnectionError(e);
        }

        if (newName == null)
            return;
        descriptorComboBox.setSelectedItem(newName);
        String newDescriptor = getDescriptor();
        // descriptorChanged();
    }

    protected void setTime(final int pos) {
        if (pos < 0)
            return;
        List timeList = getAbsoluteTimes();
        ChooserList newTimesList = getRelativeTimesList();
        newTimesList.setSelectedIndex(pos);
    }

    private String getDefaultFromSaveset(final String property,
        final String dflt) {
        if (restElement == null)
            return dflt;
        if (property.equals(PROP_KEY) || property.equals(PROP_LOC)) {
            String str = dflt;
            if (restElement.hasAttribute(ATTR_LATLON))
                str = ATTR_LATLON;
            else
                str = ATTR_LINELE;
            if (property.equals(PROP_KEY))
                return str;
            return restElement.getAttribute(str);
        }
        for (int i = 0; i < ATTRS.length; i++) {
            if (property.equals(ATTRS[i])) {
                if (!restElement.hasAttribute(ATTRS[i])) {
                    return dflt;
                }
                return restElement.getAttribute(ATTRS[i]);
            }
        }
        return dflt;
    }

    /**
     * Get the id for the default display type
     * 
     * @return the display id
     */
    protected String getDefaultDisplayType() {
        return "imagedisplay";
    }

    protected void setRestElement(final Element elem) {
        restElement = elem;
    }

    protected void setLocationPanel(final int indx) {
        locationPanel.show(indx);
    }

    protected void resetDoAbsoluteTimes(final boolean val) {
        setDoAbsoluteTimes(val);
    }
}
