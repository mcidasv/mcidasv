package edu.wisc.ssec.mcidasv.chooser;

import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.*;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.data.imagery.BandInfo;
import ucar.unidata.data.imagery.ImageDataSource;
import ucar.unidata.data.imagery.ImageDataset;

import ucar.unidata.idv.ui.IdvUIManager;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.idv.chooser.adde.*;

import ucar.unidata.ui.ChooserList;
import ucar.unidata.ui.ChooserPanel;
import ucar.unidata.ui.DateTimePicker;
import ucar.unidata.ui.LatLonWidget;

import ucar.unidata.util.DateSelection;
import ucar.unidata.util.DatedObject;
import ucar.unidata.util.DatedThing;
import ucar.unidata.util.Format;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlNodeList;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.UtcDate;


import visad.*;

import visad.georef.EarthLocation;


import java.awt.*;
import java.awt.event.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
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
public class Test2AddeImageChooser extends AddeChooser implements ucar.unidata.ui
    .imagery.ImageSelector {


    /**
     * Public keys for server, group, dataset, user, project.
     */
    public final static String SIZE_KEY = "size";
    public final static String PLACE_KEY = "place";
    public final static String LATLON_KEY = "latlon";
    public final static String LINELE_KEY = "linele";
    public final static String MAG_KEY = "mag";

    /** Command for connecting */
    protected static final String CMD_MANAGER = "cmd.manager";

    private static final int SIZE_THRESHOLD = 30;

    /** monospaced font */
    private Font monoFont = null;

    /** default magnification */
    private static final int DEFAULT_MAG = 0;

    /** descriptor label */
    private JComponent descriptorLabel;

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

    /** Property for image default value mag */
    protected static final String PROP_MAG = "MAG";

    /** Property for num */
    protected static final String PROP_NUM = "NUM";

    /** Property for image default value place */
    protected static final String PROP_PLACE = "PLACE";

    /** Property for image default value spac */
    protected static final String PROP_SPAC = "SPAC";

    /** Property for image default value unit */
    protected static final String PROP_UNIT = "UNIT";

    /** Property for image default value unit */
    protected static final String PROP_NAV = "NAV";

    /** This is the list of properties that are used in the advanced gui */
    private static final String[] ADVANCED_PROPS = {
        PROP_MAG, PROP_NAV
    };

    /** This is the list of labels used for the advanced gui */
    private static final String[] ADVANCED_LABELS = {
        "Magnification:", "Navigation Type:"
    };


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
    private static final String[] ATTRS = {
        ATTR_KEY,
        ATTR_UNIT, ATTR_BAND, ATTR_PLACE, ATTR_LOC, ATTR_SIZE, ATTR_MAG,
        ATTR_NAV, ATTR_DESCRIPTOR, ATTR_GROUP, ATTR_SERVER, ATTR_LATLON,
        ATTR_LINELE, ATTR_POS, ATTR_DAY, ATTR_TIME, ATTR_USER, ATTR_PROJ
    };

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
    private Hashtable propToComps = new Hashtable();


    /**
     * This is a list of hashtables, one per imagedefaults resource.
     * The Hashtables map the pattern to the xml node
     */
    private List resourceMaps;


    /** Holds the subsetting defaults */
    private XmlResourceCollection addeServerRSC;
    private XmlResourceCollection imageDefaultsRSC;


    /** the center load point */
    private String centerPoint;


    /** archive date formatter */
    private SimpleDateFormat archiveDayFormatter;


    /**
     * List of JComponent-s that depend on a descriptor being selected
     * to be enabled
     */
    protected ArrayList compsThatNeedDescriptor = new ArrayList();

    /** A widget for the list of dataset descriptors */
    protected JComboBox descriptorComboBox;

    /** Flag to keep from infinite looping */
    private boolean ignoreDescriptorChange = false;

    /** The descriptor names */
    protected String[] descriptorNames;

    /** Widget for the line magnfication in the advanced section */
    JSlider lineMagSlider;

    /** Label for the line mag. in the advanced section */
    JLabel lineMagLbl;

    /** Widget for the element magnfication in the advanced section */
    JSlider elementMagSlider;

    /** Label for the element mag. in the advanced section */
    JLabel elementMagLbl;

    /** Label for the properties */
    JLabel propertiesLabel;

    /** base number of lines */
    private double baseNumLines = 0.0;


    /** size label */
    JLabel sizeLbl;

    /** base number of lines */
    private double baseNumElements = 0.0;

    /** Widget to hold the number of elements in the advanced */
    JTextField numElementsFld;

    /** Widget to hold  the number of lines   in the advanced */
    JTextField numLinesFld;

    /** Widget for the line  center point in the advanced section */
    protected JTextField centerLineFld;

    /** Widget for the element  center point in the advanced section */
    protected JTextField centerElementFld;

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
     *  The list of currently loaded AddeImageDescriptor-s
     */
    private Vector imageDescriptors;

    /** maximum size for the widget */
    private static final int MAX_SIZE = 700;

    /** Widget for selecting image units */
    protected JComboBox unitComboBox;

    /** the place string */
    private String place;

    /** Widget for selecting image nav type */
    protected JComboBox navComboBox;

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

    private String bandDefault = "ALL";
    private String unitDefault = "ALL";
    
    /** Some more useful server methods */
    private ServerInfo serverInfo = null;

    /** Default value for the user property */
    protected static String DEFAULT_USER = "idv";

    /** Default value for the proj property */
    protected static String DEFAULT_PROJ = "0";

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

    private boolean allServersFlag = false;

    private static JButton mineBtn = null;

    /**
     * Construct an Adde image selection widget
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public Test2AddeImageChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        this.imageDefaultsRSC = getImageDefaults();
        this.addeServerRSC = getAddeServers();
        Element imageDefaultsRoot;
        Document imageDefaultsDoc;
        if (imageDefaultsRSC.hasWritableResource()) {
            imageDefaultsDoc =
                imageDefaultsRSC.getWritableDocument("<imagedefaults></imagedefaults>");
            imageDefaultsRoot =
                imageDefaultsRSC.getWritableRoot("<imagedefaults></imagedefaults>");
        }
        try {
            imageDefaultsRSC.writeWritable();
        } catch (Exception e) {
        }
        serverInfo = getServerInfo();
        this.user = serverInfo.getUser();
        this.proj = serverInfo.getProj();
        serverSelector = getServerSelector();
        DEFAULT_USER = this.user;
        DEFAULT_PROJ = this.proj;
    }


    /**
     * Get the xml resource collection that defines the image default xml
     *
     * @return Image defaults resources
     */
    protected XmlResourceCollection getImageDefaults() {
        return getIdv().getResourceManager().getXmlResources(
            IdvResourceManager.RSC_IMAGEDEFAULTS);
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

    private ServerInfo getServerInfo() {
        if (addeServerRSC == null) addeServerRSC = getAddeServers();
        return new ServerInfo(getIdv(), addeServerRSC);
    }


    protected int getMainIndex() {
        return mainIndex;
    }

    /**
     * Get the server selector
     * @return The server selector
     */
    public JComboBox getServerSelector() {
        serverSelector = super.getServerSelector();
        ItemListener[] ell = serverSelector.getItemListeners();
        serverSelector.removeItemListener((ItemListener)ell[0]);
        updateServers();
        updateGroups();
        serverSelector.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateGroups();
            }
        });
        return serverSelector;
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

    /**
     * Go directly to the Server Manager
     */
    protected final void doManager() {
        getIdv().getPreferenceManager().showTab("ADDE Servers");
    }

    /**
     * Reload the list of servers if they have changed
     */
    public void updateServers() {
        serverInfo = getServerInfo();
        this.user = serverInfo.getUser();
        this.proj = serverInfo.getProj();
        DEFAULT_USER = this.user;
        DEFAULT_PROJ = this.proj;
        McIDASV idv = (McIDASV)getIdv();
        McIdasChooserManager mcm = idv.getMcIdasChooserManager();
        String type = getGroupType();
        List myServers = AddeServer.getServersWithType(type,
                             mcm.initializeAddeServers(idv, false));
        int mine = myServers.size();
        List servers = AddeServer.getServersWithType(type,
                           mcm.initializeAddeServers(idv, true));
        if (!allServersFlag) servers = myServers;
        this.addeServers = servers;
        if (allServersFlag && (mine > 0)) {
            servers = insertSeparator(servers, mine);
        }
        GuiUtils.setListData(serverSelector, servers);
        if (addeServers.size() > 0) {
            serverSelector.setSelectedIndex(0);
            updateGroups();
        }
    }

    private List insertSeparator(List servers, int after) {
        List newServerList = servers;
        String str = "---------------";
        AddeServer blank = new AddeServer(str);
        newServerList.add(after, blank);
        return newServerList;
    }

    /**
     * This method checks if the current server is valid. If it is valid
     * then it checks if there is authentication required
     *
     * @return true if the server exists and can be accessed
     */
    protected boolean canAccessServer() {
        //Try reading the public.serv file to see if we need a username/proj
        JTextField projFld   = null;
        JTextField userFld   = null;
        JComponent contents  = null;
        JLabel     label     = null;
        boolean    firstTime = true;
        while (true) {
            int status = checkIfServerIsOk();
            if (status == STATUS_OK) {
                break;
            }
            if (status == STATUS_ERROR) {
                setState(STATE_UNCONNECTED);
                return false;
            }
            if (projFld == null) {
                projFld            = new JTextField("", 10);
                userFld            = new JTextField("", 10);
                GuiUtils.tmpInsets = GuiUtils.INSETS_5;
                contents = GuiUtils.doLayout(new Component[] {
                    GuiUtils.rLabel("User ID:"),
                    userFld, GuiUtils.rLabel("Project #:"), projFld, }, 2,
                        GuiUtils.WT_N, GuiUtils.WT_N);
                label    = new JLabel(" ");
                contents = GuiUtils.topCenter(label, contents);
                contents = GuiUtils.inset(contents, 5);
            }
            String lbl = (firstTime
                          ? "The server: " + getServer()
                            + " requires a user ID & project number for access"
                          : "Authentication for server: " + getServer()
                            + " failed. Please try again");
            label.setText(lbl);

            if ( !GuiUtils.showOkCancelDialog(null, "ADDE Project/User name",
                    contents, null)) {
                setState(STATE_UNCONNECTED);
                return false;
            }
            firstTime = false;
            String userName = userFld.getText().trim();
            String project  = projFld.getText().trim();
            if ((userName.length() > 0) && (project.length() > 0)) {
                passwords.put(getServer(),
                              new String[] { userName, project });
                DEFAULT_USER = userName;
                DEFAULT_PROJ = project;
                this.user = userName;
                this.proj = project;
                serverInfo.setUserProj(userName, project);
            }
        }
        return true;
    }

    protected void setServer(String serverName) {
        AddeServer newServer = AddeServer.findServer(addeServers, serverName);
        if (newServer != null) {
            descIndex = -1;
            serverSelector.setSelectedItem(newServer);
            updateGroups();
            try {
                handleConnect();
            } catch (Exception e) {
            }
        }
    }

    protected void setServerOnly(String serverName) {
        AddeServer newServer = AddeServer.findServer(addeServers, serverName);
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
     * get the adde server grup type to use
     *
     * @return group type
     */
    protected String getGroupType() {
        return AddeServer.TYPE_IMAGE;
    }

    /**
     * Set the group list
     */
    protected void updateGroups() {
        if (groupSelector != null) {
            try {
                if (serverSelector.getItemCount() < 1) {
                    groupSelector.removeAllItems();
                } else {
                    AddeServer selectedServer = (AddeServer)serverSelector.getSelectedItem();
                    if (selectedServer != null) {
                        List groups   = selectedServer.getGroupsWithType(getGroupType(), true);
                        if (groups != null) {
                            GuiUtils.setListData(groupSelector, groups);
                            if (groups.size() > 0) groupSelector.setSelectedIndex(0);
                        }
                    }
                }
            } catch (Exception e) {
            }
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
     * Update labels, enable widgets, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        if (getDoAbsoluteTimes()) {
            setPropertiesState(getASelectedTime());
        } else {
            setPropertiesState(lastAD);
        }

        if (getState() != STATE_CONNECTED) {
            setDescriptors(null);
            return;
        }

        if ( !haveDescriptorSelected()) {
            if ( !usingStations() || haveStationSelected()) {
                String name = getDescriptorLabel().toLowerCase();
                if (StringUtil.startsWithVowel(name)) {
                    setStatus("Please select an " + name, "imagetype");
                } else {
                    setStatus("Please select a " + name, "imagetype");
                }
            }
        } else if (readTimesTask!=null) {
            if(taskOk(readTimesTask)) {
                setStatus("Reading available times from server");
            } 
        } else if (getDoAbsoluteTimes() && !haveTimeSelected()) {
            setStatus(MSG_TIMES);
        }
        enableWidgets();
    }

    /**
     * Do we have times selected. Either we are doing absolute
     * times and there are some selected in the list. Or we
     * are doing relative times and we have done a connect to the
     * server
     *
     * @return Do we have times
     */
    protected boolean timesOk() {
        if (getDoAbsoluteTimes() && !haveTimeSelected()) {
            return false;
        }
        return (lastAD != null);
    }

    /**
     * Get the list of advanced property names
     *
     * @return array of advanced property names
     */
    protected String[] getAdvancedProps() {
        return ADVANCED_PROPS;
    }

    /**
     * Get the list of advanced property labels
     *
     * @return list of advanced property labels
     */
    protected String[] getAdvancedLabels() {
        return ADVANCED_LABELS;
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
     * This allows derived classes to provide their own name for labeling, etc.
     *
     * @return  the dataset name
     */
    public String getDataName() {
        return "Image Data";
    }

    /**
     * Get the descriptor widget label
     *
     * @return  label for the descriptor  widget
     */
    public String getDescriptorLabel() {
        return "Image Type";
    }

    /**
     * Get the name of the dataset.
     *
     * @return descriptive name of the dataset.
     */
    public String getDatasetName() {
        StringBuffer buf = new StringBuffer();
        buf.append(getSelectedDescriptor());
        if (bandComboBox != null) {
            if (bandComboBox.getItemCount() > 1) {
                buf.append(" (");
                buf.append(bandComboBox.getSelectedItem());
                buf.append(")");
            }
        }
        return buf.toString();
    }

    /**
     * Check if we are ready to read times
     *
     * @return  true if times can be read
     */
    protected boolean canReadTimes() {
        return haveDescriptorSelected();
    }


    /**
     * Respond to a change in the descriptor list.
     */
    protected void descriptorChanged() {
        readTimes();
        updateStatus();
    }


    /**
     * Check if a descriptor (image type) has been chosen
     *
     * @return  true if an image type has been chosen
     */
    protected boolean haveDescriptorSelected() {
        if ( !GuiUtils.anySelected(descriptorComboBox)) {
            return false;
        }
        return (getDescriptor() != null);
    }



    /**
     * Handle when the user presses the connect button
     *
     * @throws Exception On badness
     */
    public void handleConnect() throws Exception {
        setState(STATE_CONNECTING);
        connectToServer();
        updateStatus();
    }


    /**
     * Handle when the user presses the update button
     *
     * @throws Exception On badness
     */
    public void handleUpdate() throws Exception {
        serverInfo = null;
        updateServers();
    }


    /**
     * Overwrite base class method to clear out the lastAD member here.
     */
    protected void clearTimesList() {
        lastAD = null;
        super.clearTimesList();
    }

    public void showServers() {
        allServersFlag = !allServersFlag;
        updateServers();
        updateGroups();
    }


    /**
     * Show the groups dialog.  This method is not meant to be called
     * but is public by reason of implementation (or insanity).
     */
    public void showGroups() {
        List groups = readGroups();
        if ((groups == null) || (groups.size() == 0)) {
            LogUtil.userMessage("No public datasets found on " + getServer());
            return;
        }
        final JDialog  dialog   = GuiUtils.createDialog("Server Groups",
                                      true);
        final String[] selected = { null };
        List           comps    = new ArrayList();
        for (int i = 0; i < groups.size(); i++) {
            final String group = groups.get(i).toString();
            JButton      btn   = new JButton(group);
            comps.add(btn);
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    selected[0] = group;
                    dialog.dispose();
                }
            });
        }

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                dialog.dispose();
            }
        });

        JComponent buttons = GuiUtils.vbox(comps);
        buttons = new JScrollPane(GuiUtils.vbox(comps));
        int xsize = ((JComponent) comps.get(0)).getPreferredSize().width;
        buttons.setPreferredSize(new Dimension(xsize + 50, 150));
        JComponent top =
            GuiUtils.inset(new JLabel("Available data sets on server: "
                                      + getServer()), 5);
        JComponent bottom = GuiUtils.inset(closeBtn, 5);
        JComponent contents = GuiUtils.topCenterBottom(top, buttons,
                                  GuiUtils.wrap(bottom));
        dialog.setLocation(200, 200);
        dialog.getContentPane().add(contents);
        dialog.pack();
        dialog.setVisible(true);
        if (selected[0] != null) {
            groupSelector.setSelectedItem(selected[0]);
            doConnect();
        }
    }


    /**
     * Show the groupds dialog.  This method is not meant to be called
     * but is public by reason of implementation (or insanity).
     */
    public void getArchiveDay() {
        final JDialog dialog = GuiUtils.createDialog("Set Archive Day", true);
        final DateTimePicker dtp = new DateTimePicker((Date) null, false);
        if (archiveDay != null) {
            if (archiveDayFormatter == null) {
                archiveDayFormatter =
                    new SimpleDateFormat(UtcDate.YMD_FORMAT);
            }
            Date d = null;
            try {
                d = archiveDayFormatter.parse(archiveDay);
                dtp.setDate(d);
            } catch (Exception e) {
                logException("parsing archive day " + archiveDay, e);
            }
        }

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_REMOVE)) {
                    archiveDay = null;
                    archiveDayLabel.setText("");
                    setDoAbsoluteTimes(true);
                    descriptorChanged();
                }

                if (cmd.equals(GuiUtils.CMD_OK)) {
                    try {
                        DateTime dt = new DateTime(dtp.getDate());
                        archiveDay = UtcDate.getYMD(dt);
                        archiveDayLabel.setText(archiveDay);
                    } catch (Exception e) {}
                    setDoAbsoluteTimes(true);
                    descriptorChanged();
                }
                
                dialog.dispose();
            }
        };

        JPanel buttons = GuiUtils.makeButtons(listener,
                             new String[] { GuiUtils.CMD_OK,
                                            GuiUtils.CMD_REMOVE,
                                            GuiUtils.CMD_CANCEL });

        JComponent contents =
            GuiUtils.topCenterBottom(
                GuiUtils.inset(
                    GuiUtils.lLabel("Please select a day for this dataset:"),
                    10), GuiUtils.inset(dtp, 10), buttons);
        Point p = new Point(200, 200);
        if (archiveDayComponent != null) {
            try {
                p = archiveDayComponent.getLocationOnScreen();
            } catch (IllegalComponentStateException ice) {}
        }
        dialog.setLocation(p);
        dialog.getContentPane().add(contents);
        dialog.pack();
        dialog.setVisible(true);
    }


    /**
     * Connect to the server.
     */
    protected void connectToServer() {
        setDescriptors(null);
        archiveDay = null;
        if (archiveDayLabel != null) {
            archiveDayLabel.setText("");
        }
        // set to relative times
        setDoAbsoluteTimes(false);
        if ( !canAccessServer()) {
            return;
        }
        readSatBands();
        readDescriptors();
        readTimes();
        //Save the server/group state
        saveServerState();
        ignoreStateChangedEvents = true;
        if (descList != null) {
            descList.saveState(groupSelector);
        }
        ignoreStateChangedEvents = false;
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
        JPanel imagePanel = GuiUtils.doLayout(allComps, 2, GuiUtils.WT_NY,
                                GuiUtils.WT_N);
        tabbedPane = new JTabbedPane();
        JComponent buttons = getDefaultButtons(this);
        buttons = turnOffAutoCreate(buttons);
        JPanel mainPanel = GuiUtils.centerBottom(imagePanel, buttons);
        mainPanel.setName("Test Images");
        tabbedPane.add(mainPanel);
        mainIndex = tabbedPane.getSelectedIndex();
        Image2ParametersTab ip = new Image2ParametersTab(this, tabbedPane);
        JPanel parameterSetsPanel = ip.doMakeContents();
        tabbedPane.addTab("Restore Parameter Set",parameterSetsPanel);
        return tabbedPane;
    }

    protected List processServerComponents() {
        if (groupSelector != null) {
            clearOnChange(groupSelector);
        }
        descriptorLabel = addServerComp(GuiUtils.rLabel(getDescriptorLabel()
                + ":"));
        descriptorComboBox = new JComboBox();

        descriptorComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( !ignoreDescriptorChange
                        && (e.getStateChange() == e.SELECTED)) {
                    descriptorChanged();
                }
            }
        });

        JButton showBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/About16.gif", this,
                                     "showGroups");
        showBtn.setToolTipText(
            "List the public datasets available on the server");
        JComponent extraTop = GuiUtils.hbox(groupSelector, showBtn);
        List comps = new ArrayList();
        addTopComponents(comps, LABEL_DATASET, extraTop);
        return comps;
    }

    /**
     * Add to the given comps list all the status line and server
     * components.
     *
     * @param comps List of comps to add to
     * @param extra The components after the server box if non-null.
     */
    protected void addTopComponents(List comps, Component extra) {
        comps.add(GuiUtils.rLabel(""));
        comps.add(getStatusComponent());
        comps.add(GuiUtils.rLabel(LABEL_SERVER));
        if (extra == null) {
            extra = GuiUtils.filler();
        }
        GuiUtils.tmpInsets = GRID_INSETS;
        mineBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Import16.gif", this,
                                     "showServers");
        mineBtn.setToolTipText(
            "Toggle system servers on/off after mine");
        JComponent mine = GuiUtils.hbox(mineBtn, serverSelector);
        JPanel right = GuiUtils.doLayout(new Component[] { mine,
                extra, getConnectButton(), getManageButton() },4, GuiUtils.WT_YN,
                                             GuiUtils.WT_N);
        comps.add(GuiUtils.left(right));
    }

    protected List processPropertyComponents() {
        List bottomComps = new ArrayList();
        // need to call this to create the propPanel
        getBottomComponents(bottomComps);

        for (int i = 0; i < bottomComps.size(); i++) {
            addDescComp((JComponent) bottomComps.get(i));
        }
        return bottomComps;
    }

    /**
     * Add the bottom advanced gui panel to the list
     *
     * @param bottomComps  the bottom components
     */
    protected void getBottomComponents(List bottomComps) {
        String[] propArray  = getAdvancedProps();
        String[] labelArray = getAdvancedLabels();
        Insets  dfltGridSpacing = new Insets(4, 0, 4, 0);
        String  dfltLblSpacing  = " ";

        boolean haveNav         = Misc.toList(propArray).contains(PROP_NAV);
        for (int propIdx = 0; propIdx < propArray.length; propIdx++) {
            JComponent propComp = null;
            String     prop     = propArray[propIdx];
            if (prop.equals(PROP_MAG)) {
                boolean oldAmSettingProperties = amSettingProperties;
                amSettingProperties = true;
                ChangeListener lineListener =
                    new javax.swing.event.ChangeListener() {
                    public void stateChanged(ChangeEvent evt) {
                        if (amSettingProperties) {
                            return;
                        }
                        //lineMagSliderChanged(!lockBtn.isSelected());
                        lineMagSliderChanged(true);
                    }
                };
                ChangeListener elementListener = new ChangeListener() {
                    public void stateChanged(
                            javax.swing.event.ChangeEvent evt) {
                        if (amSettingProperties) {
                            return;
                        }
                        elementMagSliderChanged(true);
                    }
                };
                JComponent[] lineMagComps =
                    GuiUtils.makeSliderPopup(-SLIDER_MAX, SLIDER_MAX, 0,
                                             lineListener);
                lineMagSlider = (JSlider) lineMagComps[1];
                lineMagSlider.setMajorTickSpacing(1);
                lineMagSlider.setSnapToTicks(true);
                lineMagSlider.setExtent(1);
                lineMagComps[0].setToolTipText(
                    "Change the line magnification");
                JComponent[] elementMagComps =
                    GuiUtils.makeSliderPopup(-SLIDER_MAX, SLIDER_MAX, 0,
                                             elementListener);
                elementMagSlider = (JSlider) elementMagComps[1];
                elementMagSlider.setExtent(1);
                elementMagSlider.setMajorTickSpacing(1);
                elementMagSlider.setSnapToTicks(true);
                elementMagComps[0].setToolTipText(
                    "Change the element magnification");
                lineMagSlider.setToolTipText(
                    "Slide to set line magnification factor");
                lineMagLbl =
                    GuiUtils.getFixedWidthLabel(StringUtil.padLeft("1", 4));
                elementMagSlider.setToolTipText(
                    "Slide to set element magnification factor");
                elementMagLbl =
                    GuiUtils.getFixedWidthLabel(StringUtil.padLeft("1", 4));
                amSettingProperties = oldAmSettingProperties;


                GuiUtils.tmpInsets  = dfltGridSpacing;
                /*
                JPanel magPanel = GuiUtils.doLayout(new Component[] {
                    GuiUtils.rLabel("Line:" + dfltLblSpacing), lineMagLbl,
                    GuiUtils.inset(lineMagComps[0], new Insets(0, 4, 0, 0)),
                    GuiUtils.rLabel("   Element:" + dfltLblSpacing),
                    elementMagLbl,
                    GuiUtils.inset(elementMagComps[0],
                                   new Insets(0, 4, 0, 0)),
                                   }, 6, GuiUtils.WT_N, GuiUtils.WT_N);*/

                JPanel magPanel = GuiUtils.doLayout(new Component[] {
                                      lineMagLbl,
                                      GuiUtils.inset(lineMagComps[0],
                                          new Insets(0, 4, 0, 0)),
                                      new JLabel("    X "), elementMagLbl,
                                      GuiUtils.inset(elementMagComps[0],
                                          new Insets(0, 4, 0, 0)), }, 6,
                                              GuiUtils.WT_N, GuiUtils.WT_N);
                addPropComp(PROP_MAG, propComp = magPanel);
                if (haveNav) {
                    navComboBox = new JComboBox();
                    GuiUtils.setListData(
                        navComboBox,
                        Misc.newList(
                            new TwoFacedObject("Default", "X"),
                            new TwoFacedObject("Lat/Lon", "LALO")));
                    addPropComp(PROP_NAV, navComboBox);
                    boolean showNav = false;
                    showNav = getProperty("includeNavComp", false);
                    if (showNav) {
                        propComp = GuiUtils.hbox(
                            propComp,
                            GuiUtils.inset(
                                new JLabel("Navigation Type:"),
                                new Insets(0, 10, 0, 5)), navComboBox, 5);
                    }
                }
            }
            if (propComp != null) {
                bottomComps.add(GuiUtils.rLabel(labelArray[propIdx]));
                bottomComps.add(GuiUtils.left(propComp));
            }
        }

        GuiUtils.tmpInsets = new Insets(3, 4, 0, 4);
        propPanel = GuiUtils.doLayout(bottomComps, 2, GuiUtils.WT_N,
                                      GuiUtils.WT_N);
        enableWidgets();
    }

    /**
     * Associates the goven JComponent with the PROP_ property
     * identified  by the given propId
     *
     * @param propId The property
     * @param comp The gui component that allows the user to set the property
     *
     * @return Just returns the given comp
     */
    protected JComponent addPropComp(String propId, JComponent comp) {
        Object oldComp = propToComps.get(propId);
        if (oldComp != null) {
            throw new IllegalStateException(
                "Already have a component defined:" + propId);
        }
        propToComps.put(propId, comp);
        return comp;
    }

    private void elementMagSliderChanged(boolean recomputeLineEleRatio) {
        int value = getElementMagValue();
        if ((Math.abs(value) < SLIDER_MAX)) {
            int lineMag = getLineMagValue();
            if (lineMag > value) {
                linesToElements = Math.abs(lineMag
                                           / (double) value);
            } else {
                linesToElements = Math.abs((double) value
                                           / lineMag);
            }
        }
        //System.out.println(" changelistener: linesToElements = " + linesToElements);
        elementMagLbl.setText(StringUtil.padLeft("" + value,
                                                 4));
/*
        if(!lockBtn.isSelected()) {
            if (value > 0) {
                numElementsFld.setText(""
                                       + (int) (baseNumElements * value));
            } else {
                numElementsFld.setText(""
                                       + (int) (baseNumElements
                                                / (double) -value));
            }
        }
*/
    }

    /**
     * Get the value of the line magnification slider.
     *
     * @return The magnification value for the line
     */
    private int getLineMagValue() {
        return getMagValue(lineMagSlider);

    }



    /**
     * Get the value of the element magnification slider.
     *
     * @return The magnification value for the element
     */
    private int getElementMagValue() {
        return getMagValue(elementMagSlider);
    }

    /**
     * Get the value of the given  magnification slider.
     *
     * @param slider The slider to get the value from
     * @return The magnification value
     */
    private int getMagValue(JSlider slider) {
        //Value is [-SLIDER_MAX,SLIDER_MAX]. We change 0 and -1 to 1
        int value = slider.getValue();
        if (value >= 0) {
            return value + 1;
        }
        return value - 1;
    }

    /**
     * Handle the line mag slider changed event 
     *
     * @param evt  the event
     */
    private void lineMagSliderChanged(boolean autoSetSize) {
        try {
            int value = getLineMagValue();
            lineMagLbl.setText(StringUtil.padLeft("" + value, 4));
            if(autoSetSize) {
                if (value > 0) {
                    numLinesFld.setText("" + (int) (baseNumLines * value));
                } else {
                    numLinesFld.setText("" + (int) (baseNumLines
                                                    / (double) -value));
                }
            }

            if (value == 1) {                     // special case
                if (linesToElements < 1.0) {
                    value = (int) (-value / linesToElements);
                } else {
                    value = (int) (value * linesToElements);
                }

            } else if (value > 1) {
                value = (int) (value * linesToElements);

            } else {
                value = (int) (value / linesToElements);
            }

            value                 = (value > 0)
                                    ? value - 1
                                    : value + 1;  // since slider is one off
            amSettingProperties = true;
            elementMagSlider.setValue(value);
            amSettingProperties = false;
            elementMagSliderChanged(false);
        } catch (Exception exc) {
            logException("Setting line magnification", exc);
        }
        //amSettingProperties = false;
    }

    /**
     * Handle the absolute time selection changing
     */
    protected void absoluteTimesSelectionChanged() {
        if ( !getDoAbsoluteTimes()) {
            return;
        }
        setPropertiesState(getASelectedTime());
    }


    /**
     * Make the components (label/widget) and return them
     *
     *
     * @param comps The list to add components to
     */
    protected void getComponents(List comps) {
        comps.add(descriptorLabel);
        comps.add(GuiUtils.left(registerStatusComp("imagetype",
                descriptorComboBox)));
        addTimesComponent(comps);
    }

    /**
     * Add the times component to the list
     *
     * @param comps  List to add to
     */
    protected void addTimesComponent(List comps) {
        comps.add(GuiUtils.top(addServerComp(GuiUtils.rLabel("Times:"))));
        comps.add(addServerComp(makeTimesPanel(true)));

    }



    /**
     * Get the time popup widget
     *
     * @return  a widget for selecing the day
     */
    protected JComponent getExtraTimeComponent() {
        JButton archiveDayBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Archive.gif", this,
                                     "getArchiveDay");
        archiveDayBtn.setToolTipText("Select a day for archive datasets");
        archiveDayLabel     = new JLabel("");
        archiveDayComponent = GuiUtils.hbox(archiveDayBtn, archiveDayLabel);
        return GuiUtils.top(archiveDayComponent);
    }


    /**
     * Should we use the user supplied property
     *
     * @param propId The property
     *
     * @return Should use the value from the advanced widget
     */
    protected boolean usePropFromUser(String propId) {
        if (propToComps.get(propId) == null) {
            return false;
        }
        return true;
    }


    /**
     * A utility to add a component to the list of components that
     * need the descriptor
     *
     * @param comp The component
     * @return The component
     */
    protected JComponent addDescComp(JComponent comp) {
        compsThatNeedDescriptor.add(comp);
        return comp;
    }


    /**
     * Get one of the selected times.
     *
     * @return One of the selected times.
     */
    protected AreaDirectory getASelectedTime() {
        if (haveTimeSelected()) {
            List selected = getSelectedAbsoluteTimes();
            if (selected.size() > 0) {
                AddeImageDescriptor aid =
                    (AddeImageDescriptor) selected.get(0);
                if (aid != null) {
                    return aid.getDirectory();
                }
            }
        }
        return null;
    }

    /**
     * Get the default relative time index
     *
     * @return default index
     */
    protected int getDefaultRelativeTimeIndex() {
        return 4;
    }


    /**
     * Enable or disable the GUI widgets based on what has been
     * selected.
     */
    protected void enableWidgets() {
        boolean descriptorState = ((getState() == STATE_CONNECTED)
                                   && canReadTimes());

        for (int i = 0; i < compsThatNeedDescriptor.size(); i++) {
            JComponent comp = (JComponent) compsThatNeedDescriptor.get(i);
            GuiUtils.enableTree(comp, descriptorState);
        }

        boolean timesOk = timesOk();
        if (propPanel != null) {
            GuiUtils.enableTree(propPanel, timesOk);
        }

        String[] propArray  = getAdvancedProps();
        String[] labelArray = getAdvancedLabels();

        checkTimesLists();

        enableAbsoluteTimesList(getDoAbsoluteTimes() && descriptorState);

        getRelativeTimesChooser().setEnabled( !getDoAbsoluteTimes()
                && descriptorState);
        if (archiveDayComponent != null) {
            GuiUtils.enableTree(archiveDayComponent, getDoAbsoluteTimes());
        }
        revalidate();
    }



    /**
     * Check if we are using the lat/lon widget
     *
     * @return true if we are using the lat/lon widget
     */
    private boolean useLatLon() {
        return false;
    }


    /**
     * Get the selected descriptor.
     *
     * @return  the currently selected descriptor.
     */
    protected String getDescriptor() {
        return getDescriptorFromSelection(getSelectedDescriptor());
    }

    /**
     * Get the descriptor relating to the selection.
     *
     * @param selection   String name from the widget
     *
     * @return  the descriptor
     */
    protected String getDescriptorFromSelection(String selection) {
        if (descriptorTable == null) {
            return null;
        }
        if (selection == null) {
            return null;
        }
        return (String) descriptorTable.get(selection);
    }

    /**
     * Get the selected descriptor.
     *
     * @return the selected descriptor
     */
    public String getSelectedDescriptor() {
        String selection = (String) descriptorComboBox.getSelectedItem();
        if (selection == null) {
            return null;
        }
        if (selection.equals(LABEL_SELECT)) {
            return null;
        }
        return selection;
    }


    /**
     * Get the data type for this chooser
     *
     * @return the data type
     */
    public String getDataType() {
        return "IMAGE";
    }


    /**
     * Get a description of the currently selected dataset
     *
     * @return  a description of the currently selected dataset
     * @deprecated  use #getDatasetName()
     */
    public String getDatasetDescription() {
        return getDatasetName();
    }

    /**
     *  Read the set of image times available for the current server/group/type
     *  This method is a wrapper, setting the wait cursor and wrapping the
     *  call to {@link #readTimesInner()}; in a try/catch block
     */
    protected void readTimes() {
        clearTimesList();
        if ( !canReadTimes()) {
            return;
        }
        Misc.run(new Runnable() {
            public void run() {
                updateStatus();
                showWaitCursor();
                try {
                    readTimesInner();
                } catch (Exception e) {
                    handleConnectionError(e);
                }
                showNormalCursor();
                updateStatus();
            }
        });
    }


    public void doCancel() {
        readTimesTask = null;
        super.doCancel();
    }

    public void doReset() {
        serverInfo = null;
        restElement = null;
        bandDefault = "ALL";
        unitDefault = "ALL";
        resetDoAbsoluteTimes(false);
        getRelativeTimesList().setSelectedIndex(getDefaultRelativeTimeIndex());
        clearTimesList();
        updateServers();
    }


    /**
     * Set the list of dates/times based on the image selection
     *
     * @param timestep    the timestep for the image selection
     */
    private void readTimesInner() {
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

    private Object NUTEX = new Object();


    /**
     * Load the images for the given URL and timestep
     *
     * @param url          ADDE URL
     * @param timestep     valid timestep
     */
    protected void loadImages(String url) {
        //System.out.println("url=" + url);
        readTimesTask = startTask();
        updateStatus();
        Object task = readTimesTask;
        try {
            AreaDirectoryList adir = null;
            synchronized (NUTEX) {
                adir = new AreaDirectoryList(url);
                //Make sure no other loads are  occurred
                boolean ok = stopTaskAndIsOk(task);
                if(!Misc.equals(readTimesTask, task) || !ok) {
                    return;
                }
                readTimesTask = null;
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
                    setAbsoluteTimes(imageDescriptors);
                }
            }
            setState(STATE_CONNECTED);
        } catch (McIDASException e) {
            System.out.println("Excepiton from loadImages........ e=" + e);
            stopTask(task);
            readTimesTask = null;
            handleConnectionError(e);
        }
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
        DateTime[] imageTimes = new DateTime[imageDescriptors.size()];
        for (int idIdx = 0; idIdx < imageDescriptors.size(); idIdx++) {
            AddeImageDescriptor aid =
                (AddeImageDescriptor) imageDescriptors.get(idIdx);
            imageTimes[idIdx] = aid.getImageTime();
        }
        if (imageTimes.length > 0) {
            try {
                Gridded1DSet imageSet    = DateTime.makeTimeSet(imageTimes);
                int          numTimes    = times.length;
                double[][]   timesValues = new double[1][numTimes];
                for (int i = 0; i < times.length; i++) {
                    timesValues[0][i] =
                        times[i].getValue(imageSet.getSetUnits()[0]);
                }
                setSelectedAbsoluteTimes(imageSet.doubleToIndex(timesValues));
            } catch (VisADException ve) {
                logException("Unable to set times from display", ve);
            }
        }
    }



    /**
     * Reset the descriptor stuff
     */
    private void resetDescriptorBox() {
        ignoreDescriptorChange = true;
        descriptorComboBox.setSelectedItem(LABEL_SELECT);
        ignoreDescriptorChange = false;
    }


    /**
     * Set the center location portion of the request.  If the input
     * from the widget is null, use the centerpoint from the image descriptor.
     *
     * @param aid   image descriptor for the image
     */
    private void setCenterLocation(AddeImageDescriptor aid) {
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


    /**
     * Initialize the descriptor list from a list of names
     *
     * @param names  list of names
     */
    protected void setDescriptors(String[] names) {
        synchronized (WIDGET_MUTEX) {
            ignoreDescriptorChange = true;
            descriptorComboBox.removeAllItems();
            descriptorNames = names;
            if ((names == null) || (names.length == 0)) {
                return;
            }
            descriptorComboBox.addItem(LABEL_SELECT);
            for (int j = 0; j < names.length; j++) {
                descriptorComboBox.addItem(names[j]);
            }
            ignoreDescriptorChange = false;
        }
    }


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
                aii.setBand(bandDefault);
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
                aii.setBand(bandDefault);
                aid.setImageInfo(aii);
                aid.setSource(aii.getURLString());
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
    private void initializeAddeDefaults() {
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

    protected void setDescriptorOnly(String descriptorName) {
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
        appendKeyValue(buff, PROP_USER, DEFAULT_USER);
        appendKeyValue(buff, PROP_PROJ, DEFAULT_PROJ);
    }

    /**
     * Get the image size string from the directory and defaults
     *
     * @param ad    image directory
     * @return   request size
     */
    protected String getSize(AreaDirectory ad) {
        String retString = MAX_SIZE + " " + MAX_SIZE;
        if (ad != null) {
            int x = ad.getElements();
            int y = ad.getLines();
            if ((x < MAX_SIZE) && (y < MAX_SIZE)) {
                retString = x + " " + y;
            } else if ((x >= MAX_SIZE) && (y >= MAX_SIZE)) {
                retString = MAX_SIZE + " " + MAX_SIZE;
            } else if ((x >= MAX_SIZE) && (y < MAX_SIZE)) {
                retString = MAX_SIZE + " " + y;
            } else if ((x < MAX_SIZE) && (y >= MAX_SIZE)) {
                retString = x + " " + MAX_SIZE;
            }
        }
        return retString;
    }

    /**
     * Check for valid lat/lon values
     *
     * @return  true if values are valid
     */
    protected boolean checkForValidValues() {
        return true;
    }


    /**
     * Given the <code>AreaDirectory</code>, create the appropriate
     * request string for the image.
     *
     * @param ad  <code>AreaDirectory</code> for the image in question.
     * @return  the ADDE request URL
     */
    protected String makeRequestString(AreaDirectory ad) {
        return makeRequestString(ad, true, 0);

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
    protected String makeRequestString(AreaDirectory ad, boolean doTimes,
                                       int cnt) {
        StringBuffer buf = getGroupUrl(REQ_IMAGEDATA, getGroup());
        buf.append(makeDateTimeString(ad, cnt, doTimes));

        String[] props = {
            PROP_DESCR, PROP_UNIT, PROP_SPAC, PROP_BAND, PROP_MAG,
            PROP_PLACE, PROP_NAV
        };
        buf.append(makeProps(props, ad));
        return buf.toString();
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
        return new String[] { PROP_DESCR, PROP_UNIT, PROP_SPAC, PROP_BAND,
                              PROP_NAV };
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
     * Get the value for the given property. This can either be the value
     * supplied by the end user through the advanced GUI or is the default
     *
     * @param prop The property
     * @param ad The AreaDirectory
     *
     * @return The value of the property to use in the request string
     */
    protected String getPropValue(String prop, AreaDirectory ad) {
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
        if (prop.equals(PROP_MAG)) {
            return "1 1";
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
                //value = ALLBANDS.toString();
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
        for (int i = 0; i < props.length; i++) {
            String prop  = props[i];
            String value = getPropValue(prop, ad);
            if (prop.equals(PROP_USER)) {
                value = this.user;
                aii.setUser(value);
            } else if (prop.equals(PROP_PROJ)) {
                value = this.proj;
                aii.setProject(Integer.parseInt(value));
            }
        }
    }


    /**
     * Set the widgets with the state from the given AreaDirectory
     *
     * @param ad   AreaDirectory for the image
     */
    protected void setPropertiesState(AreaDirectory ad) {
        setPropertiesState(ad, false);
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


        String[] propArray  = getAdvancedProps();
        String[] labelArray = getAdvancedLabels();


        if (numLinesFld != null) {
            baseNumLines    = ad.getLines();
            baseNumElements = ad.getElements();



            //use defaults here
            String sizeDefault = null;
            List   toks        = ((sizeDefault != null)
                                  ? StringUtil.split(sizeDefault, " ", true,
                                      true)
                                  : null);
            if ((toks == null) || (toks.size() == 0)) {
                numLinesFld.setText("" + (int) baseNumLines);
                numElementsFld.setText("" + (int) baseNumElements);
            } else {
                String lines = "" + toks.get(0);
                if (lines.equalsIgnoreCase(ALL)) {
                    lines = "" + (int) baseNumLines;
                }
                numLinesFld.setText(lines);
                String elems = (toks.size() > 1)
                               ? "" + toks.get(1)
                               : "" + (int) baseNumElements;
                if (elems.equalsIgnoreCase(ALL)) {
                    elems = "" + baseNumElements;
                }
                numElementsFld.setText(elems);
            }


            if (sizeLbl != null) {
                String label = "  Raw size: " + ad.getLines() + " X "
                               + ad.getElements();
                sizeLbl.setText(label);
            }
        }
        if (centerLineFld != null) {
            centerLineFld.setText("" + ad.getLines() / 2);
            centerElementFld.setText("" + ad.getElements() / 2);
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
     * Get a pair of properties
     *
     * @param v   a space separated string
     *
     * @return an array of the two strings
     */
    private String[] getPair(String v) {
        if (v == null) {
            return null;
        }
        v = v.trim();
        List toks = StringUtil.split(v, " ", true, true);
        if ((toks == null) || (toks.size() == 0)) {
            return null;
        }
        String tok1 = toks.get(0).toString();
        return new String[] { tok1, ((toks.size() > 1)
                                     ? toks.get(1).toString()
                                     : tok1) };

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
        return name;
    }


    /**
     * Set the available units in the  unit selector
     *
     * @param ad   AreaDirectory for the image
     * @param band band to use for units
     */
    protected void setAvailableUnits(AreaDirectory ad, int band) {
        List l = getAvailableUnits(ad, band);
        l.add(ALLUNITS);
        GuiUtils.setListData(unitComboBox, l);
        TwoFacedObject tfo = null;
    }

    /**
     * Set the available units in the  unit selector
     *
     * @param ad   AreaDirectory for the image
     * @param band band to use for units
     *
     * @return List of available units
     */
    private List<TwoFacedObject> getAvailableUnits(AreaDirectory ad,
            int band) {
        // get Vector array of Calibration types.   Layout is
        // v[i] = band[i] and for each band, it is a vector of
        // strings of calibration names and descriptions
        // n = name, n+1 = desc.
        // for radar, we only have one band
        if (ad == null) {
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
                tfo = new TwoFacedObject(desc, name);
                l.add(tfo);
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

        String          magKey = getPropValue(PROP_MAG, dir);
        int             lmag   = 1;
        int             emag   = 1;
        StringTokenizer tok    = new StringTokenizer(magKey);
        lmag = (int) Misc.parseNumber((String) tok.nextElement());
        if (tok.hasMoreTokens()) {
            emag = (int) Misc.parseNumber((String) tok.nextElement());
        } else {
            emag = lmag;
        }
        info.setLineMag(lmag);
        info.setElementMag(emag);

        int    lines   = dir.getLines();
        int    elems   = dir.getElements();
        String size = "all";
        if ( !size.equalsIgnoreCase("all")) {
            lines = (int) Misc.parseNumber(size);
            if (tok.hasMoreTokens()) {
                elems = (int) Misc.parseNumber((String) tok.nextElement());
            } else {
                elems = lines;
            }
        }
        info.setLines(lines);
        info.setElements(elems);

        return info;
    }

    /**
     * Check to see if the two Area directories are equal
     *
     * @param ad1  first AD (may be null)
     * @param ad2  second AD (may be null)
     *
     * @return true if they are equal
     */
    private boolean checkPropertiesEqual(AreaDirectory ad1,
                                         AreaDirectory ad2) {
        if (ad1 == null) {
            return false;
        }
        if (ad2 == null) {
            return false;
        }
        return Misc.equals(ad1, ad2)
               || ((ad1.getLines() == ad2.getLines())
                   && (ad1.getElements() == ad2.getElements())
                   && Arrays.equals(ad1.getBands(), ad2.getBands()));
    }


    /**
     * Show the given error to the user. If it was an Adde exception
     * that was a bad server error then print out a nice message.
     *
     * @param excp The exception
     */
    protected void handleConnectionError(Exception excp) {
        String message = excp.getMessage().toLowerCase();
        if ((excp instanceof AreaFileException)
                && (message.indexOf("must be used with archived datasets")
                    >= 0)) {
            getArchiveDay();
        } else {
            resetDescriptorBox();
            super.handleConnectionError(excp);
        }
    }

    /**
     * Get the descriptor table for this chooser
     *
     * @return a Hashtable of descriptors and names
     */
    public Hashtable getDescriptorTable() {
        return descriptorTable;
    }


    /**
     * Get the list of bands for the images
     *
     * @param ad   AreaDirectory
     * @param bands  list of bands
     * @return list of BandInfos for the selected images
     */
    private List<BandInfo> makeBandInfos(AreaDirectory ad, int[] bands) {
        readSatBands();
        List<BandInfo> l = new ArrayList<BandInfo>();
        if (ad != null) {
            if (bands != null) {
                for (int i = 0; i < bands.length; i++) {
                    int      band = bands[i];
                    BandInfo bi   = new BandInfo(ad.getSensorID(), band);
                    bi.setBandDescription(getBandName(ad, band));
                    bi.setCalibrationUnits(getAvailableUnits(ad, band));
                    bi.setPreferredUnit(getDefault(PROP_UNIT, "BRIT"));
                    l.add(bi);
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
        // update the BandInfo list based on what has been chosen
        List selectedBandInfos = new ArrayList<BandInfo>();
        if (bandDefault.equals("ALL"))
            selectedBandInfos = bandInfos;
        else {
            Integer i = new Integer(bandDefault);
            int indx = BandInfo.findIndexByNumber(i.intValue(), bandInfos);
            selectedBandInfos.add(bandInfos.get(indx));
        }
        return selectedBandInfos;
    }


    /**
     * Get the id for the default display type
     *
     * @return the display id
     */
    protected String getDefaultDisplayType() {
        return "imagedisplay";
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
        if(imageList==null || imageList.size()==0) return;
        ImageDataset ids = new ImageDataset(getDatasetName(), imageList);
        // make properties Hashtable to hand the station name
        // to the AddeImageDataSource
        Hashtable ht = new Hashtable();
        getDataSourceProperties(ht);
        if (restElement != null) {
            if (restElement.hasAttribute(ATTR_PLACE))
                ht.put(PLACE_KEY, (Object)(restElement.getAttribute(ATTR_PLACE)));
            if (restElement.hasAttribute(ATTR_LATLON))
                ht.put(LATLON_KEY, (Object)(restElement.getAttribute(ATTR_LATLON)));
            if (restElement.hasAttribute(ATTR_LINELE))
                ht.put(LINELE_KEY, (Object)(restElement.getAttribute(ATTR_LINELE)));
            if (restElement.hasAttribute(ATTR_SIZE))
                ht.put(SIZE_KEY, (Object)(restElement.getAttribute(ATTR_SIZE)));
            if (restElement.hasAttribute(ATTR_MAG))
                ht.put(MAG_KEY, (Object)(restElement.getAttribute(ATTR_MAG)));
        } else {
            String magVal = getLineMagValue() + " " + getElementMagValue();
                ht.put(MAG_KEY, (Object)magVal);
        }

        makeDataSource(ids, "ADDE.IMAGE2", ht);
        saveServerState();
    }

    /**
     * Get the DataSource properties
     *
     * @param ht  Hashtable of properties
     */
    protected void getDataSourceProperties(Hashtable ht) {
        super.getDataSourceProperties(ht);
        ht.put(DATASET_NAME_KEY, getDatasetName());
        ht.put(ImageDataSource.PROP_BANDINFO, getSelectedBandInfos());
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

    protected void resetDoAbsoluteTimes(boolean val) {
        setDoAbsoluteTimes(val);
    }

    protected void restoreMag(String magStr) {
        //System.out.println("restoreMag: magStr=" + magStr);
        String[] mags = getPair(magStr);
        Integer lint = new Integer(mags[0]);
        int lin = lint.intValue();
        lint = new Integer(mags[1]);
        int ele = lint.intValue();
        //System.out.println("restoreMag");
        setMagSliders(lin, ele);
        //setMagSliders(new Integer(mags[0]).intValue(),
        //              new Integer(mags[1]).intValue());
    }

    /**
     * Set the mag slider values
     *
     * @param lineValue    the line value
     * @param elementValue the element value
     */
    private void setMagSliders(int lineValue, int elementValue) {
        //System.out.println("setMagSliders: line=" + lineValue + " ele=" + elementValue);
        if (lineMagSlider != null) {
            if (lineValue > 0) {
                lineValue--;
            } else if (lineValue < 0) {
                lineValue++;
            }
            if (elementValue > 0) {
                elementValue--;
            } else if (elementValue < 0) {
                elementValue++;
            }


            lineMagSlider.setValue(lineValue);
            elementMagSlider.setValue(elementValue);
            lineMagLbl.setText(StringUtil.padLeft("" + getLineMagValue(), 4));
            elementMagLbl.setText(StringUtil.padLeft(""
                    + getElementMagValue(), 4));
            linesToElements = Math.abs(lineValue / (double) elementValue);
            if (Double.isNaN(linesToElements)) {
                linesToElements = 1.0;
            }
        }
    }

    private JComponent turnOffAutoCreate(JComponent buttons) {
         Component[] comps = buttons.getComponents();
         JButton resButton = GuiUtils.makeButton("Reset", this, "doReset");
         JCheckBox box = new JCheckBox();
         if (comps.length > 0) {
             for (int i=0; i<comps.length; i++) {
                 Component comp = comps[i];
                 if (comp.getClass().isInstance(box)) {
                     ((JCheckBox)comp).setSelected(false);
                     buttons.add(resButton, i);
                     return buttons;
                 }
             }
         }
         return buttons;
    }
}

