package ucar.unidata.ui.imagery.mcidas;

import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.idv.chooser.IdvChooser;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;

import ucar.unidata.ui.AddeChooser;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.ui.XmlUi;
import ucar.unidata.ui.imagery.ImageSelector;

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

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Test a new Image selector GUI.
 * Default group selector.
 */
public class TestAddeImageChooser extends AddeChooser implements ImageSelector {

    /** default magnification */
    private static final int DEFAULT_MAG = 0;

    /** descriptor label */
    private JComponent descriptorLabel;

    /** Property for the satband file */
    protected static final String FILE_SATBAND = "SATBAND";

    /** Property for image default value band */
    protected static final String PROP_BAND = "BAND";

    /** Property for accessing a file */
    protected static final String PROP_FILE = "file";

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

    /** This is the list of labels used for the advanced gui */
    private static final String[] ADVANCED_LABELS = {
        "Data Type:", "Channel:", "Placement:", "Location:", "Image Size:",
        "Magnification:"
    };

    private List nameList = new ArrayList();
    private int defaultListIndex;

    JButton saveBtn;

    /** Xml tag name for the defaults */
    protected static final String TAG_DEFAULT = "default";

    /** Xml attr name for the defaults */
    protected static final String ATTR_NAME = "name";
    protected static final String ATTR_SERVER = "server";
    protected static final String ATTR_DATASET = "dataset";
    protected static final String ATTR_DATATYPE = "datatype";

    /** Selection label text */
    protected static final String LABEL_SELECT = "Select a Type";

    /** flag for setting properties */
    private boolean amSettingProperties = false;

    /** Are we currently reading times */
    private boolean readingTimes = false;

    /** List of descriptors */
    private PreferenceList descList;

    /** Holds the properties */
    private JPanel propPanel;

    /** Widget for selecting the data group */
    private JComboBox groupSelector;

    /** Maps the PROP_ property name to the gui component */
    private Hashtable propToComps = new Hashtable();

    /** Holds the mapping of id to xml Element */
    private Hashtable idToNodeList;

    /** This is a list of hashtables, one per imagedefaults resource. The Hashtables map the name to the xml node */
    private List resourceMaps;

    /** Holds the subsetting defaults */
    private XmlResourceCollection imageDefaults;

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

    /** Input for lat/lon center point */
    protected LatLonWidget latLonWidget;

    /** Widget for the line magnfication in the advanced section */
    JSlider lineMagSlider;

    /** Label for the line mag. in the advanced section */
    JLabel lineMagLbl;

    /** Widget for the element magnfication in the advanced section */
    JSlider elementMagSlider;

    /** Label for the element mag. in the advanced section */
    JLabel elementMagLbl;

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

    /** Should we use lat/lon or line/element for the center point */
    protected JRadioButton useLatLonBtn;

    /** Should we use lat/lon or line/element for the center point */
    protected JRadioButton useLineElementBtn;

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

    /** maximum size for the widget */
    private static final int MAX_SIZE = 700;

    /** Widget for selecting image units */
    protected JComboBox unitComboBox;

    /** Widget for selecting image place */
    protected JComboBox placeComboBox;

    /** Widget for selecting default parameter set */
    private JComboBox defaultComboBox;

    /** Widget for selecting a name to Save As */
    private JTextField saveFld = null;

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

    /** The user imagedefaults xml root */
    private Element imageDefaultsRoot;

    /** The user imagedefaults xml document */
    private Document imageDefaultsDocument;

    /**
     * Construct an Adde image selection widget
     *
     * @param imageDefaults The xml resources for the image defaults
     * @param descList Holds the preferences for the image descriptors
     * @param serverList Holds the preferences for the adde servers
     */
    public TestAddeImageChooser(IdvChooser idvChooser,
                            XmlResourceCollection imageDefaults,
                            PreferenceList descList,
                            PreferenceList serverList) {
        super(idvChooser, serverList);
        this.descList      = descList;
        groupSelector = descList.createComboBox(GuiUtils.CMD_UPDATE, this);
        this.imageDefaults = imageDefaults;
         if (imageDefaults.hasWritableResource()) {
            imageDefaultsDocument =
                imageDefaults.getWritableDocument("<tabs></tabs>");
            imageDefaultsRoot = imageDefaults.getWritableRoot("<tabs></tabs>");
         }
        setLayout(new BorderLayout(5, 5));
        JComponent contents = doMakeContents();
        this.add(contents, BorderLayout.CENTER);
        this.add(getDefaultButtons(this), BorderLayout.SOUTH);
        updateStatus();
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
        enableWidgets();

        if (getState() != STATE_CONNECTED) {
            setDescriptors(null);
            //      setPropertiesState(null);
            return;
        }

        if ( !haveDescriptorSelected()) {
            if ( !usingStations() || haveStationSelected()) {
                String name = getDataName().toLowerCase();
                if (StringUtil.startsWithVowel(name)) {
                    setStatus("Please select an " + name + " type");
                } else {
                    setStatus("Please select a " + name + " type");
                }
            }
        } else if (readingTimes) {
            setStatus(
                "Reading available times from server (this may take a while)");
        } else if (getDoAbsoluteTimes() && !haveTimeSelected()) {
            setStatus(MSG_TIMES);
        }
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
     * Get the name of the dataset.
     *
     * @return descriptive name of the dataset.
     */
    public String getDatasetName() {
        return getSelectedDescriptor();
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
        initializeImageDefaults();
        setAvailableDefaultSets();
        if (getState() != STATE_CONNECTED) {
            //If not connected then connect.
            handleConnect();
        } else {
            //If we are already connected  then update the rest of the chooser
            descriptorChanged();
        }
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
        descList.saveState(groupSelector);
        ignoreStateChangedEvents = false;
    }


    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    protected JComponent doMakeContents() {
        List allComps = new ArrayList();
        clearOnChange(groupSelector);
        descriptorLabel = addServerComp(GuiUtils.rLabel("   " + getDataName()
                + " Type:"));


        descriptorComboBox = new JComboBox();
        descriptorComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( !ignoreDescriptorChange
                        && (e.getStateChange() == e.SELECTED)) {
                    descriptorChanged();
                    setAvailableDefaultSets();
                }
            }
        });

        //      JComponent extraTop = GuiUtils.hbox(groupSelector,descriptorLabel,
        //                                          descriptorComboBox);


        JComponent extraTop = groupSelector;
        addTopComponents(allComps, LABEL_DATASET, extraTop);
        getComponents(allComps);

        List defaultSetComps = new ArrayList();
        getDefaultSetComponent(defaultSetComps);
        allComps.addAll(defaultSetComps);

        //        JComponent bottomComp = getBottomComponent();
        List bottomComps = new ArrayList();
        getBottomComponents(bottomComps);

        //Empty the list if we are in simple mode
        if ((idvChooser != null) 
                && !idvChooser.getProperty(idvChooser.ATTR_SHOWDETAILS,
                                           true)) {
            bottomComps = new ArrayList();
        }

        for (int i = 0; i < bottomComps.size(); i++) {
            addDescComp((JComponent) bottomComps.get(i));
        }

        if (bottomComps.size() > 2) {
            allComps.add(GuiUtils.rLabel("<html><u>Properties:</u></html>"));
            allComps.add(new JLabel(""));
        }
        List saveAsComps = new ArrayList();
        getSaveAsComponent(saveAsComps);
        allComps.addAll(bottomComps);
        allComps.addAll(saveAsComps);

        if (numLinesFld != null) {
            //            allComps.add(GuiUtils.rLabel("Image Properties:"));
            //            allComps.add(sizeLbl);
        }

        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel imagePanel = GuiUtils.doLayout(allComps, 2, GuiUtils.WT_NN,
                                GuiUtils.WT_N);

        return imagePanel;

    }


    /**
     * Select default parameter set
     *
     */

     private void getDefaultSetComponent(List defaultSetComps) {
         defaultSetComps.add(GuiUtils.rLabel("<html><u>Default Set:</u><html>"));
         defaultComboBox = new JComboBox();
         defaultComboBox.setPreferredSize(new Dimension(200,
               defaultComboBox.getPreferredSize().height));
        defaultComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( e.getStateChange() == e.SELECTED) {
                    defaultListIndex =defaultComboBox.getSelectedIndex();
                    useDefaultSetComponent(defaultListIndex);
                    updateStatus();
                }
            }
        });
         defaultComboBox.disable();
         addDescComp(defaultComboBox);
         defaultSetComps.add(GuiUtils.left(defaultComboBox));
         return;
     }


     private void useDefaultSetComponent(int defaultListIndex){
         String key = getDefault(PROP_KEY, PROP_LINEELE);
         boolean latlonBtnOn = false;
         if (key.equals(PROP_LATLON)) latlonBtnOn=true;
         useLatLonBtn.setSelected(latlonBtnOn);
         useLineElementBtn.setSelected(!latlonBtnOn);
         checkCenterEnabled();
     }

 
    /**
     * Save to xml
     *
     */

     private void getSaveAsComponent(List saveAsComps) {
         JLabel saveLabel = GuiUtils.rLabel("Label:");
         saveAsComps.add(saveLabel);
         saveFld = new JTextField("", 20);
         saveFld.disable();
         saveLabel.disable();
         addDescComp(saveFld);
         addDescComp(saveLabel);
         saveFld.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent ae) {
                 saveBtn.setEnabled(true);
             }
         });
         List saveList = new ArrayList();
         saveList.add(saveFld);
         saveList.add(new JLabel(" "));
         saveBtn = getSaveButton();
         saveList.add(saveBtn);
         JPanel savePanel = GuiUtils.doLayout(saveList, 3, GuiUtils.WT_NNN,
                                              GuiUtils.WT_N);
         saveAsComps.add(GuiUtils.left(savePanel));
         return;
     }

     protected JButton getSaveButton() {
         JButton saveBtn = new JButton("Save");
         saveBtn.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent ae) {
                 String saveName = (saveFld.getText()).trim();
                 saveImageSpecs(saveName);
             }
         });
         saveBtn.setEnabled(false);
         return saveBtn;
     }

     private void saveImageSpecs(String saveName) {

         List parts = deconstructUrl();
         List keywords = getKeywordList(parts);
         List attrs = getKeyValues(saveName, keywords);
         String[] sAttrs = new String[attrs.size()];
         for (int i=0; i<attrs.size(); i++)  sAttrs[i] = (String)attrs.get(i);
         Element element = imageDefaultsDocument.createElement(TAG_DEFAULT);
         XmlUtil.setAttributes(element, sAttrs);
         imageDefaultsRoot.appendChild(element);
         try {
             imageDefaults.writeWritable();
         } catch (Exception e) {
             System.out.println("saveImageSpecs exception=" + e);
             return;
         }
         imageDefaults.setWritableDocument(imageDefaultsDocument, imageDefaultsRoot);
         initializeImageDefaults();
         setAvailableDefaultSets();

         defaultComboBox.setSelectedItem(saveName);
         defaultListIndex =defaultComboBox.getSelectedIndex();
         useDefaultSetComponent(defaultListIndex);
         updateStatus();
         saveFld.setText(" ");
         saveBtn.setEnabled(false);

     }


    /**
     * Returns a list of the images to load or null if none have been
     * selected.
     *
     * @return  list  get the list of image descriptors
     */
    private List deconstructUrl() {
        if ( !timesOk()) {
            return null;
        }
        List parts = new ArrayList();
        if (getDoRelativeTimes()) {
            AddeImageDescriptor firstDescriptor =
                (AddeImageDescriptor) imageDescriptors.get(0);
            //for (int i = 0; i < getNumRelativeTimes(); i++) {
                int i=0;
                AddeImageDescriptor aid = new AddeImageDescriptor(i,
                                              firstDescriptor);
                AddeImageInfo aii = makeImageInfo(aid.getDirectory(), true,
                                        i);
                String url = aii.makeAddeUrl();
                //System.out.println(aii.makeAddeUrl());
                StringTokenizer tok      = new StringTokenizer(url,"&");
                while (tok.hasMoreTokens()) {
                    parts.add(tok.nextElement());
                }
                //System.out.println(" ");
            //}
        } else {
            Object[] selectedTimes = getTimesList().getSelectedValues();
            //for (int i = 0; i < selectedTimes.length; i++) {
                int i=0;
                AddeImageDescriptor aid =
                    new AddeImageDescriptor(
                        (AddeImageDescriptor) selectedTimes[i]);
                AddeImageInfo aii = makeImageInfo(aid.getDirectory(), false,
                                        i);
                String url = aii.makeAddeUrl();
                //System.out.println(aii.makeAddeUrl());
                StringTokenizer tok      = new StringTokenizer(url,"&");
                while (tok.hasMoreTokens()) {
                    parts.add(tok.nextElement());
                }
                //System.out.println(" ");
            //}
        }
        return parts;
    }

    private List getKeywordList(List parts) {
        List keywords = new ArrayList();
        String str = null;
        for (int i=1; i<parts.size(); i++) {
            str = (String)parts.get(i);
            keywords.add(str);
        }
        return keywords;
    }

    private List getKeyValues(String saveName, List keywords) {
        List values = new ArrayList();
        values.add("name");
        values.add(saveName);
        values.add("server");
        values.add(getServer());
        int posGroup = 0;
        String str;
        for (int i=0; i<keywords.size(); i++) {
            str = (String)keywords.get(i);
            StringTokenizer tok      = new StringTokenizer(str,"=");
            str = tok.nextToken().toLowerCase();
            if (str.equals("group")) {
                posGroup = i;
                continue;
            }
        }
        if (posGroup == 0) return null;
        for (int i=posGroup; i<keywords.size(); i++) {
            str = (String)keywords.get(i);
            StringTokenizer tok      = new StringTokenizer(str,"=");
            str = tok.nextToken().toLowerCase();
            if (str.equals("descr")) {
                str = "datatype";
            } else if (str.equals("group")) {
                    str = "dataset";
            } else if ((str.equals("linele")) || (str.equals("latlon"))) {
                values.add("key");
                values.add(str.toUpperCase());
                str = "loc";
            }
            values.add(str);
            values.add(tok.nextToken());
        }
        return values;
    }


    /**
     * Should we show the advanced properties component in a separate panel
     *
     * @return true
     */
    public boolean showAdvancedInTab() {
        return true;
    }


    /**
     * Show the settings in the holder
     *
     * @param holder  the holder
     */
    public void showSettings(JComponent holder) {
        if (holder instanceof JTabbedPane) {
            ((JTabbedPane) holder).setSelectedIndex(0);
        } else {
            CardLayout cardLayout = (CardLayout) holder.getLayout();
            cardLayout.show(holder, "settings");
        }
    }


    /**
     * Show the advanced settings in the holder
     *
     * @param holder  the holder of the settings
     */
    public void showAdvanced(JComponent holder) {
        if (holder instanceof JTabbedPane) {
            ((JTabbedPane) holder).setSelectedIndex(1);
        } else {
            CardLayout cardLayout = (CardLayout) holder.getLayout();
            cardLayout.show(holder, "advanced");
        }
    }

    /**
     * Add the bottom advanced gui panel to the list
     *
     * @param bottomComps  the bottom components
     */
    protected void getBottomComponents(List bottomComps) {

        String[] propArray  = getAdvancedProps();
        String[] labelArray = getAdvancedLabels();
        //        List     bottomComps     = new ArrayList();
        Insets  dfltGridSpacing = new Insets(4, 0, 4, 0);
        String  dfltLblSpacing  = " ";

        boolean haveBand        = Misc.toList(propArray).contains(PROP_BAND);
        for (int propIdx = 0; propIdx < propArray.length; propIdx++) {
            JComponent propComp = null;
            String     prop     = propArray[propIdx];
            if (prop.equals(PROP_UNIT)) {
                unitComboBox = new JComboBox();
                addPropComp(PROP_UNIT, propComp = unitComboBox);
                unitComboBox.setPreferredSize(new Dimension(100,
                        unitComboBox.getPreferredSize().height));
                if (haveBand) {
                    bandComboBox = new JComboBox();
                    bandComboBox.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            setAvailableUnits(propertiesAD,
                                    getSelectedBand());
                        }
                    });
                    addPropComp(PROP_BAND, bandComboBox);
                    propComp = GuiUtils.hbox(propComp,
                                             new JLabel("    Channel: "),
                                             bandComboBox, 5);
                }
            } else if (prop.equals(PROP_BAND)) {
                //Moved to PROP_UNIT
            } else if (prop.equals(PROP_PLACE)) {
                //Moved to PROP_LOC
            } else if (prop.equals(PROP_LOC)) {
                placeComboBox = new JComboBox();
                GuiUtils.setListData(
                    placeComboBox,
                    Misc.newList(
                        new TwoFacedObject("Center:", "CENTER"),
                        new TwoFacedObject("Upper Left:", "ULEFT")));
                addPropComp(PROP_PLACE, placeComboBox);

                latLonWidget      = new LatLonWidget();
                centerLineFld     = new JTextField("", 3);
                centerElementFld  = new JTextField("", 3);
                useLatLonBtn      = new JRadioButton("", true);
                useLineElementBtn = new JRadioButton("", false);
                useLineElementBtn.setBorder(
                    BorderFactory.createEmptyBorder());
                useLatLonBtn.setBorder(BorderFactory.createEmptyBorder());
                GuiUtils.buttonGroup(useLatLonBtn, useLineElementBtn);
                ActionListener centerListener = new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        checkCenterEnabled();
                    }
                };

                useLatLonBtn.addActionListener(centerListener);
                useLineElementBtn.addActionListener(centerListener);

                GuiUtils.tmpInsets = dfltGridSpacing;
                JComponent centerPopup;
                if (idvChooser != null) {
                    final JButton centerPopupBtn =
                        GuiUtils.getImageButton(
                            "/auxdata/ui/icons/Map16.gif", getClass());
                    centerPopupBtn.setToolTipText(
                        "Center on current displays");
                    centerPopupBtn.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            popupCenterMenu(centerPopupBtn);
                        }
                    });
                    centerPopup = GuiUtils.inset(centerPopupBtn,
                            new Insets(0, 0, 0, 4));
                } else {
                    centerPopup = new JPanel();
                }


                GuiUtils.tmpInsets = dfltGridSpacing;
                JPanel centerPanel = GuiUtils.doLayout(new Component[] {
                    useLatLonBtn,
                    centerLatLbl = GuiUtils.rLabel(" Lat:" + dfltLblSpacing),
                    latLonWidget.getLatField(),
                    centerLonLbl = GuiUtils.rLabel(" Lon:" + dfltLblSpacing),
                    latLonWidget.getLonField(), new JLabel(" "), centerPopup,
                    useLineElementBtn,
                    centerLineLbl = GuiUtils.rLabel(" Line:"
                        + dfltLblSpacing),
                    centerLineFld,
                    centerElementLbl = GuiUtils.rLabel(" Element:"
                        + dfltLblSpacing),
                    centerElementFld
                }, 12, GuiUtils.WT_N, GuiUtils.WT_N);

                propComp = GuiUtils.hbox(GuiUtils.wrap(placeComboBox),
                                         centerPanel, 5);
                addPropComp(PROP_LOC, centerPanel);
            } else if (prop.equals(PROP_MAG)) {
                boolean oldAmSettingProperties = amSettingProperties;
                amSettingProperties = true;
                ChangeListener lineListener =
                    new javax.swing.event.ChangeListener() {
                    public void stateChanged(ChangeEvent evt) {
                        lineMagSliderChanged(evt);
                    }
                };
                ChangeListener elementListener = new ChangeListener() {
                    public void stateChanged(
                            javax.swing.event.ChangeEvent evt) {
                        if (amSettingProperties) {
                            return;
                        }
                        int value = getElementMagValue();
                        if (recomputeLineEleRatio
                                && (Math.abs(value) < SLIDER_MAX)) {
                            int lineMag = getLineMagValue();

                            if (lineMag > value) {
                                linesToElements = Math.abs(lineMag
                                        / (double) value);
                            } else {
                                linesToElements = Math.abs((double) value
                                        / lineMag);
                            }
                        }
                        elementMagLbl.setText(StringUtil.padLeft("" + value,
                                4));
                        if (value > 0) {
                            numElementsFld.setText(""
                                    + (int) (baseNumElements * value));
                        } else {
                            numElementsFld.setText(""
                                    + (int) (baseNumElements
                                             / (double) -value));
                        }
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
                JPanel magPanel = GuiUtils.doLayout(new Component[] {
                    GuiUtils.rLabel("Line:" + dfltLblSpacing), lineMagLbl,
                    GuiUtils.inset(lineMagComps[0], new Insets(0, 4, 0, 0)),
                    GuiUtils.rLabel("   Element:" + dfltLblSpacing),
                    elementMagLbl,
                    GuiUtils.inset(elementMagComps[0],
                                   new Insets(0, 4, 0, 0)),
                }, 6, GuiUtils.WT_N, GuiUtils.WT_N);

                addPropComp(PROP_MAG, propComp = magPanel);
            } else if (prop.equals(PROP_SIZE)) {
                numLinesFld        = new JTextField("", 4);
                numElementsFld     = new JTextField("", 4);
                GuiUtils.tmpInsets = dfltGridSpacing;
                sizeLbl            = GuiUtils.lLabel("");

                JPanel sizePanel =
                    GuiUtils.left(GuiUtils.doLayout(new Component[] {
                    GuiUtils.rLabel("Lines:" + dfltLblSpacing), numLinesFld,
                    GuiUtils.rLabel(" Elements:" + dfltLblSpacing),
                    numElementsFld, new JLabel(" "), sizeLbl
                }, 6, GuiUtils.WT_N, GuiUtils.WT_N));

                addPropComp(PROP_SIZE, propComp = sizePanel);
            }

            if (propComp != null) {
                bottomComps.add(GuiUtils.rLabel(labelArray[propIdx]));
                bottomComps.add(GuiUtils.left(propComp));
            }

        }

        enableWidgets();
        /*
        GuiUtils.tmpInsets = new Insets(3, 4, 0, 4);
        propPanel = GuiUtils.doLayout(bottomComps, 2, GuiUtils.WT_N,
                                      GuiUtils.WT_N);
                                      return propPanel;*/
    }


    /**
     * Popup a centering menu
     *
     * @param near component to popup near
     */
    private void popupCenterMenu(JComponent near) {
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    EarthLocation el = (EarthLocation) ae.getSource();
                    latLonWidget.setLatLon(el.getLatitude().getValue(),
                                           el.getLongitude().getValue());
                } catch (Exception exc) {
                    logException("Setting center", exc);
                }
            }
        };
        List menuItems = idvChooser.makeCenterMenus(listener);
        if (menuItems.size() == 0) {
            menuItems.add(new JMenuItem("No map displays"));
        }
        GuiUtils.showPopupMenu(menuItems, near);
    }


    /**
     * Handle the line mag slider changed event
     *
     * @param evt  the event
     */
    private void lineMagSliderChanged(ChangeEvent evt) {
        if (amSettingProperties) {
            return;
        }
        try {
            int value = getLineMagValue();
            lineMagLbl.setText(StringUtil.padLeft("" + value, 4));
            if (value > 0) {
                numLinesFld.setText("" + (int) (baseNumLines * value));
            } else {
                numLinesFld.setText("" + (int) (baseNumLines
                        / (double) -value));
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

            recomputeLineEleRatio = false;
            elementMagSlider.setValue(value);
            recomputeLineEleRatio = true;

        } catch (Exception exc) {
            logException("Setting line magnification", exc);
        }
        //amSettingProperties = false;
    }


    /**
     * Show the advanced properties dialog
     */
    public void showPropPanel() {
        GuiUtils.makeDialog(null, "Properties", propPanel, null,
                            new String[] { "OK" });
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
     * Make the components (label/widget) and return them
     *
     *
     * @param comps The list to add components to
     */
    protected void getComponents(List comps) {
        getTimesList().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if ( !getDoAbsoluteTimes()) {
                    return;
                }
                setPropertiesState(getASelectedTime());
            }
        });
        GuiUtils.tmpInsets = new Insets(0, 4, 0, 4);
        comps.add(descriptorLabel);
        comps.add(GuiUtils.left(descriptorComboBox));
        addTimesComponent(comps);
    }

    /**
     * Add the times component to the list
     *
     * @param comps  List to add to
     */
    protected void addTimesComponent(List comps) {
        comps.add(GuiUtils.top(addServerComp(GuiUtils.rLabel("Times:"))));
        comps.add(GuiUtils.left(addServerComp(makeTimesPanel())));
    }


    /**
     * Get the image group from the gui.
     *
     * @return The iamge group.
     */
    protected String getGroup() {
        return groupSelector.getSelectedItem().toString();
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
            AddeImageDescriptor aid =
                (AddeImageDescriptor) getTimesList().getSelectedValue();
            if (aid != null) {
                return aid.getDirectory();
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
        return 0;
    }

    /**
     * Read the groups from the public.srv file on the server
     *
     * @return List of groups
     */
    protected List readGroups() {
        List groups = new ArrayList();
        try {
            StringBuffer buff = getUrl(REQ_TEXT);
            appendKeyValue(buff, PROP_FILE, FILE_PUBLICSRV);
            List lines = readTextLines(buff.toString());
            if (lines == null) {
                return null;
            }
            Hashtable seen = new Hashtable();
            for (int i = 0; i < lines.size(); i++) {
                List toks = StringUtil.split(lines.get(i).toString(), ",",
                                             true, true);
                if (toks.size() == 0) {
                    continue;
                }
                String tok = (String) toks.get(0);
                int    idx = tok.indexOf("=");
                if (idx < 0) {
                    continue;
                }
                if ( !tok.substring(0, idx).trim().equals("N1")) {
                    continue;
                }
                String group = tok.substring(idx + 1).trim();
                if (seen.get(group) != null) {
                    continue;
                }
                seen.put(group, group);
                groups.add(group);
            }
        } catch (Exception e) {
            return null;
        }
        return groups;
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


        if (timesOk) {
            checkCenterEnabled();
        }
        checkTimesLists();

        getTimesList().setEnabled(getDoAbsoluteTimes() && descriptorState);
        getRelativeTimesChooser().setEnabled( !getDoAbsoluteTimes()
                && descriptorState);
    }


    /**
     * Enable or disable the center lat/lon and  line/element widgets
     */
    private void checkCenterEnabled() {
        if (useLatLonBtn == null) {
            return;
        }

        latLonWidget.getLatField().setEnabled(useLatLonBtn.isSelected());
        latLonWidget.getLonField().setEnabled(useLatLonBtn.isSelected());
        //centerLatLbl.setEnabled(useLatLonBtn.isSelected());
        //centerLonLbl.setEnabled(useLatLonBtn.isSelected());

        centerLineFld.setEnabled( !useLatLonBtn.isSelected());
        centerElementFld.setEnabled( !useLatLonBtn.isSelected());
        //centerLineLbl.setEnabled( !useLatLonBtn.isSelected());
        //centerElementLbl.setEnabled( !useLatLonBtn.isSelected());


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

    protected String getSelectionFromDescriptor(String descriptor) {
        synchronized (WIDGET_MUTEX) {
            setDescriptors(null);
            readDescriptors();
            String selection = null;
            if (descriptorTable == null) {
                return null;
            }
            if (descriptor == null) {
                return null;
            }
            Enumeration eKeys = descriptorTable.keys();
            Enumeration eValues = descriptorTable.elements();
            String val = null;
            while (eValues.hasMoreElements()) {
                selection = (String)eKeys.nextElement();
                val = (String)eValues.nextElement();
                if (descriptor.equals(val)) return selection;
            }
        }
        return null;
    }


    /**
     * Get the selected descriptor.
     *
     * @return the selected descriptor
     */
    protected String getSelectedDescriptor() {
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
     * Get the selected calibration unit.
     *
     * @return  the selected calibration unit
     */
    protected String getSelectedUnit() {
        String selection =
            (String) ((TwoFacedObject) unitComboBox.getSelectedItem())
                .getId();
        return selection;
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
     *  call to {@link #readTimesInner(int)}; in a try/catch block
     */
    protected void readTimes() {
        clearTimesList();
        if ( !canReadTimes()) {
            return;
        }
        Misc.run(new Runnable() {
            public void run() {
                readingTimes = true;
                updateStatus();
                showWaitCursor();
                try {
                    currentTimestep++;
                    readTimesInner(currentTimestep);
                } catch (Exception e) {
                    resetDescriptorBox();
                    handleConnectionError(e);
                }
                readingTimes = false;
                showNormalCursor();
                updateStatus();
            }
        });
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
        String       id          = getSelectedStation();
        if (id != null) {
            appendKeyValue(addeCmdBuff, PROP_ID, id);
        }
        appendKeyValue(addeCmdBuff, PROP_DESCR, descriptor);
        appendKeyValue(addeCmdBuff, PROP_POS, "" + pos);
        loadImages(addeCmdBuff.toString(), timestep);
    }



    /**
     * See if the timestep is valid.
     *
     * @param timestep  timestep index.
     * @return  true if <code>timestep</code> is equal to the currentTimestep
     */
    private boolean isValidTimestep(int timestep) {
        return timestep == currentTimestep;
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
                    //null, makeImageInfo(lastAD));
                    imageDescriptors.add(aid);
                }
                Collections.sort(imageDescriptors);
                if (getDoAbsoluteTimes()) {
                    getTimesList().setListData(imageDescriptors);
                }
                getTimesList().setVisibleRowCount(Math.min(numImages,
                        getTimesListSize()));
                getTimesList().setSelectedIndex(numImages - 1);
                getTimesList().ensureIndexIsVisible(
                    getTimesList().getSelectedIndex());
                revalidate();
            }
            setState(STATE_CONNECTED);
        } catch (McIDASException e) {
            resetDescriptorBox();
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
        List       selectedIndices = new ArrayList();
        DateTime[] imageTimes      = new DateTime[imageDescriptors.size()];

        for (int idIdx = 0; idIdx < imageDescriptors.size(); idIdx++) {
            AddeImageDescriptor aid =
                (AddeImageDescriptor) imageDescriptors.get(idIdx);
            imageTimes[idIdx] = aid.getImageTime();
        }
        if (imageTimes.length > 0) {
            try {
                Gridded1DSet imageSet = DateTime.makeTimeSet(imageTimes);
                System.out.println("num images = " + imageSet.getLength());
                int        numTimes    = times.length;
                double[][] timesValues = new double[1][numTimes];
                for (int i = 0; i < times.length; i++) {
                    timesValues[0][i] =
                        times[i].getValue(imageSet.getSetUnits()[0]);
                }
                int[] indices = imageSet.doubleToIndex(timesValues);
                // doesn't seem to barf on -1 or > than length indices
                getTimesList().setSelectedIndices(indices);
                // scroll to first in list
                getTimesList().ensureIndexIsVisible(
                    getTimesList().getSelectedIndex());
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
        if ( !latPoint.trim().equals("")) {
            latLonWidget.setLat(latPoint);
        }
        if ( !lonPoint.trim().equals("")) {
            latLonWidget.setLon(lonPoint);
        }
    }


    /**
     * Does this selector have all of its state set to load in data
     *
     * @return Has the user chosen everything they need to choose to load data
     */
    protected boolean getGoodToGo() {
        //  if(!super.getGoodToGo()) return false;
        if (getDoAbsoluteTimes()) {
            return getTimesList().haveDataSelected();
        } else {
            return canReadTimes() && (lastAD != null);
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
            int[] relativeTimesIndices = getRelativeTimeIndices();
            for (int i = 0; i < relativeTimesIndices.length; i++) {
                AddeImageDescriptor aid = new AddeImageDescriptor(relativeTimesIndices[i],
                                              firstDescriptor);
                AddeImageInfo aii = makeImageInfo(aid.getDirectory(), true,
                                        relativeTimesIndices[i]);
                aid.setImageInfo(aii);
                aid.setSource(aii.makeAddeUrl());
                //System.out.println(aii.makeAddeUrl());
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
                //System.out.println(aii.makeAddeUrl());
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
                if (resourceIdx == 0) {
                    nameList.add(name);
                }
                if (name != null) {
                    name = name.toLowerCase();
                }
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
     * Trim the image defaults list
     */
    private void modifyNameList() {
        String server = getServer();
        String group = getGroup();
        String descriptor = getDescriptor();
        nameList.clear();
        nameList.add(group + "/" + descriptor);

        XmlNodeList defaultNodes = XmlUtil.getElements(imageDefaultsRoot, TAG_DEFAULT);
        for (int nodeIdx = 0; nodeIdx < defaultNodes.size(); nodeIdx++) {
            Element dfltNode = (Element) defaultNodes.item(nodeIdx);

            String  name     = XmlUtil.getAttribute(dfltNode, ATTR_NAME);
            if (name.equals("*")) continue;
            String dGroup = null;
            String dDescriptor = null;
            String dServer = null;
            try {
                dGroup = XmlUtil.getAttribute(dfltNode, ATTR_DATASET);
            } catch (Exception e) { }
            if ((dGroup == null) && (name != null)) {
                StringTokenizer tok      = new StringTokenizer(name,"/");
                dGroup = tok.nextToken();
                dDescriptor = tok.nextToken();
            }
            try {
                dServer  = XmlUtil.getAttribute(dfltNode, ATTR_SERVER);
            } catch (Exception e) { }
            if (dDescriptor == null) {
                try {
                    dDescriptor = XmlUtil.getAttribute(dfltNode, ATTR_DATATYPE);
                } catch (Exception e) { }
            }
            if (server.equals(dServer)) {
                if (group.equals(dGroup)) {
                    if (descriptor.equals(dDescriptor)) {
                        nameList.add(name);
                    }
                }
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
                String key = null;
                if (defaultListIndex != 0) {
                     key = (String) nameList.get(defaultListIndex);
                } else {
                     key = keys[keyIdx];
                }
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
     *  Get the default value for a key
     *
     *  @param property      property (key type)
     *  @param dflt        default value
     *  @return value for key or dflt if not found
     */
    protected String getDefaultOldWay(String property, String dflt) {
        String userDefault = null;
        //An array of  keys we use to lookup properties
        String   server     = getServer();
        String   group      = getGroup();
        String   descriptor = getDescriptor();
        String[] keys       = {
            userDefault, server + ":" + group + "/" + descriptor,
            group + "/" + descriptor, server + ":" + group + "/*",
            group + "/*", server + ":*/" + descriptor, "*/" + descriptor,
            descriptor, server + ":*/*", server, "*"
        };
        if (idToNodeList == null) {
            initializeImageDefaults();
        }


        for (int keyIdx = 0; keyIdx < keys.length; keyIdx++) {
            String key = keys[keyIdx];
            if (key == null) {
                continue;
            } else {  // when we set the name, we lowercase it so match on that
                key = key.toLowerCase();
            }

            List nodes = (List) idToNodeList.get(key);
            if (nodes != null) {
                for (int elementIdx = 0; elementIdx < nodes.size();
                        elementIdx++) {
                    Element defaultNode = (Element) nodes.get(elementIdx);
                    String value = XmlUtil.getAttribute(defaultNode,
                                       property.toLowerCase(), (String) null);
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
        appendKeyValue(buff, PROP_COMPRESS,
                       getPropValue(PROP_COMPRESS, null));
        appendKeyValue(buff, PROP_PORT, getPropValue(PROP_PORT, null));
        appendKeyValue(buff, PROP_DEBUG, getPropValue(PROP_DEBUG, null));
        appendKeyValue(buff, PROP_VERSION, getPropValue(PROP_VERSION, null));
        appendKeyValue(buff, PROP_USER, getPropValue(PROP_USER, null));
        appendKeyValue(buff, PROP_PROJ, getPropValue(PROP_PROJ, null));
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
        if (usePropFromUser(PROP_LOC)) {
            if ((useLatLonBtn == null) || useLatLonBtn.isSelected()) {
                String msg = latLonWidget.isValidValues();
                if ((msg != null) && (msg.length() > 0)) {
                    LogUtil.userMessage(msg);
                    return false;
                }
            }
        }
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

        if (usePropFromUser(PROP_LOC)) {
            if (useLatLonBtn.isSelected()) {
                appendKeyValue(buf, PROP_LATLON,
                               getUserPropValue(PROP_LATLON, ad));
            } else {
                appendKeyValue(buf, PROP_LINEELE,
                               getUserPropValue(PROP_LINEELE, ad));
            }
        } else {
            appendKeyValue(buf, getDefault(PROP_KEY, PROP_LINEELE),
                           getPropValue(PROP_LOC, ad));
        }


        String[] props = {
            PROP_DESCR, PROP_SIZE, PROP_UNIT, PROP_SPAC, PROP_BAND, PROP_MAG,
            PROP_PLACE
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
        return new String[] { PROP_DESCR, PROP_UNIT, PROP_SPAC, PROP_BAND };
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
        if (usePropFromUser(prop)) {
            return getUserPropValue(prop, ad);
        }
        return getDefault(prop, getDefaultPropValue(prop, ad, false));
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
    protected String getUserPropValue(String prop, AreaDirectory ad) {
        if (prop.equals(PROP_LATLON) && (latLonWidget != null)) {
            return latLonWidget.getLat() + " " + latLonWidget.getLon();
        }
        if (prop.equals(PROP_PLACE) && (placeComboBox != null)) {
            return TwoFacedObject.getIdString(
                placeComboBox.getSelectedItem());
        }

        if (prop.equals(PROP_LINEELE) && (centerLineFld != null)) {
            return centerLineFld.getText().trim() + " "
                   + centerElementFld.getText().trim();
        }

        if (prop.equals(PROP_SIZE) && (numLinesFld != null)) {
            return numLinesFld.getText().trim() + " "
                   + numElementsFld.getText().trim();
        }
        if (prop.equals(PROP_MAG) && (lineMagSlider != null)) {
            return getLineMagValue() + " " + getElementMagValue();
        }
        if (prop.equals(PROP_BAND) && (bandComboBox != null)) {
            Object selected = bandComboBox.getSelectedItem();
            if (selected != null) {
                return ((TwoFacedObject) selected).getId().toString();
            }
        }
        if (prop.equals(PROP_UNIT)) {
            return getSelectedUnit();
        }
        return null;
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
        if (prop.equals(PROP_PLACE)) {
            return "CENTER";
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
        if (prop.equals(PROP_SIZE)) {
            if (ad != null) {
                return ad.getLines() + " " + ad.getElements();
            }
            return MAX_SIZE + " " + MAX_SIZE;

        }
        if (prop.equals(PROP_MAG)) {
            return "1 1";
        }
        if (prop.equals(PROP_LOC) || prop.equals(PROP_LINEELE)) {
            if (ad == null) {
                return "0 0";
            }
            return ad.getLines() / 2 + " " + ad.getElements() / 2;
        }
        if (prop.equals(PROP_LATLON)) {
            if (ad == null) {
                return "0 0";
            }
            return ad.getCenterLatitude() + " " + ad.getCenterLongitude();
        }
        if (prop.equals(PROP_BAND)) {
            if (forDisplay) {
                return getBandName(ad, ((int[]) bandTable.get(ad))[0]);
            }
            return "" + ((int[]) bandTable.get(ad))[0];
        }
        if (prop.equals(PROP_SPAC)) {
            return getSelectedUnit().equalsIgnoreCase("BRIT")
                   ? "1"
                   : "4";
        }
        if (prop.equals(PROP_UNIT)) {
            return getSelectedUnit();
            //            return "";
        }
        return "";
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
     * Clear the properties widgets
     */
    private void clearPropertiesWidgets() {
        if (latLonWidget != null) {
            latLonWidget.getLatField().setText("");
            latLonWidget.getLonField().setText("");
        }
        if (centerLineFld != null) {
            centerLineFld.setText("");
            centerElementFld.setText("");
        }
        if (numLinesFld != null) {
            if (sizeLbl != null) {
                sizeLbl.setText("");
            }
            numLinesFld.setText("");
            numElementsFld.setText("");
        }
        if (unitComboBox != null) {
            GuiUtils.setListData(unitComboBox, new Vector());
        }
        if (bandComboBox != null) {
            GuiUtils.setListData(bandComboBox, new Vector());
        }

        setMagSliders(DEFAULT_MAG, DEFAULT_MAG);

        if (placeComboBox != null) {
            placeComboBox.setSelectedIndex(0);
        }
        baseNumLines    = 0.0;
        baseNumElements = 0.0;
    }


    /**
     * Set the widgets with the state from the given AreaDirectory
     *
     * @param ad   AreaDirectory for the image
     */
    private void setPropertiesState(AreaDirectory ad) {

        if (amSettingProperties) {
            return;
        }
        propertiesAD     = ad;

        amSettingProperties = true;

        if (ad == null) {
            clearPropertiesWidgets();
            amSettingProperties = false;
            return;
        }


        String[] propArray  = getAdvancedProps();
        String[] labelArray = getAdvancedLabels();


        if (numLinesFld != null) {
            baseNumLines    = ad.getLines();
            baseNumElements = ad.getElements();



            //use defaults here
            String sizeDefault = getDefault(PROP_SIZE, (String) null);
            List   toks        = ((sizeDefault != null)
                                  ? StringUtil.split(sizeDefault, " ", true,
                                      true)
                                  : null);
            if ((toks == null) || (toks.size() == 0)) {
                numLinesFld.setText("" + ad.getLines());
                numElementsFld.setText("" + ad.getElements());
            } else {
                numLinesFld.setText("" + toks.get(0));
                numElementsFld.setText("" + ((toks.size() > 1)
                                             ? toks.get(1)
                                             : toks.get(0)));
            }


            if (sizeLbl != null) {
                String defaultSize = getDefault(PROP_SIZE,
                                         getDefaultPropValue(PROP_SIZE, ad,
                                             true));

                String label = "  Raw size: " + ad.getLines() + " X "
                               + ad.getElements();
                sizeLbl.setText(label);
            }
        }
        if (latLonWidget != null) {
            latLonWidget.getLatField().setText("" + ad.getCenterLatitude());
            latLonWidget.getLonField().setText("" + ad.getCenterLongitude());
        }
        if (centerLineFld != null) {
            centerLineFld.setText("" + ad.getLines() / 2);
            centerElementFld.setText("" + ad.getElements() / 2);
        }


        Vector bandList = new Vector();
        int[]  bands    = (int[]) bandTable.get(ad);
        if (bands != null) {
            for (int i = 0; i < bands.length; i++) {
                String name = getBandName(ad, bands[i]);
                bandList.add(new TwoFacedObject(name,
                        (Object) new Integer(bands[i])));
            }
        }


        if (bandComboBox != null) {

            GuiUtils.setListData(bandComboBox, bandList);
        }

        setAvailableUnits(ad, getSelectedBand());

        for (int propIdx = 0; propIdx < propArray.length; propIdx++) {
            String prop = propArray[propIdx];
            String value = getDefault(prop,
                                      getDefaultPropValue(prop, ad, false));
            if (value == null) {
                value = "";
            }
            value = value.trim();
            if (prop.equals(PROP_LOC)) {
                //String key = getDefault(PROP_KEY, PROP_LINEELE);
                String key = getDefault(PROP_KEY, PROP_LATLON);
                useLineElementBtn.setSelected(key.equals(PROP_LINEELE));
                if (useLineElementBtn.isSelected()) {
                    value = getDefault(PROP_LOC,
                                       getDefaultPropValue(PROP_LINEELE, ad,
                                           false));
                } else {
                    value = getDefault(PROP_LOC,
                                       getDefaultPropValue(PROP_LATLON, ad,
                                           false));
                }
                String[] pair = getPair(value);
                if (pair != null) {
                    if (useLineElementBtn.isSelected()) {
                        centerLineFld.setText(pair[0]);
                        centerElementFld.setText(pair[1]);
                    } else {
                        latLonWidget.setLat(pair[0]);
                        latLonWidget.setLon(pair[1]);

                    }
                }
            } else if (prop.equals(PROP_BAND)) {
                Integer bandNum = null;
                try {
                    bandNum = Integer.decode(value);
                } catch (NumberFormatException nfe) {}
                if (bandNum != null) {

                    TwoFacedObject tfo = TwoFacedObject.findId(bandNum,
                                             bandList);
                    if (tfo != null) {
                        bandComboBox.setSelectedItem(tfo);
                    }
                }
            } else if (prop.equals(PROP_PLACE)) {
                placeComboBox.setSelectedItem(
                    new TwoFacedObject(translatePlace(value), value));
            } else if (prop.equals(PROP_MAG)) {
                String[] pair = getPair(value);
                if (pair != null) {
                    setMagSliders(new Integer(pair[0]).intValue(),
                                  new Integer(pair[1]).intValue());
                } else {
                    setMagSliders(DEFAULT_MAG, DEFAULT_MAG);
                }
            }
        }
        amSettingProperties = false;

    }


    /**
     * Set the mag slider values
     *
     * @param lineValue    the line value
     * @param elementValue the element value
     */
    private void setMagSliders(int lineValue, int elementValue) {
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
     * Get the selected band from the advanced chooser
     *
     * @return selected band number
     */
    private int getSelectedBand() {
        TwoFacedObject tfo = (bandComboBox == null)
                             ? (TwoFacedObject) null
                             : (TwoFacedObject) bandComboBox
                                 .getSelectedItem();
        if (tfo == null) {
            return 0;
        }
        return ((Integer) tfo.getId()).intValue();
    }

    /**
     * Translate a place name into a human readable form
     *
     * @param place raw name
     *
     * @return human readable name
     */
    private String translatePlace(String place) {
        place = place.toUpperCase();
        if (place.equals("ULEFT")) {
            return "Upper left";
        }
        if (place.equals("LLEFT")) {
            return "Lower left";
        }
        if (place.equals("URIGHT")) {
            return "Upper right";
        }
        if (place.equals("LRIGHT")) {
            return "Lower right";
        }
        if (place.equals("CENTER")) {
            return "Center";
        }
        return place;
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
     * Set the available parameter default sets
     */
    private void setAvailableDefaultSets() {
        modifyNameList();
        GuiUtils.setListData(defaultComboBox, nameList);
    }


    /**
     * Set the available units in the  unit selector
     *
     * @param ad   AreaDirectory for the image
     * @param band band to use for units
     */
    private void setAvailableUnits(AreaDirectory ad, int band) {
        // get Vector array of Calibration types.   Layout is
        // v[i] = band[i] and for each band, it is a vector of
        // strings of calibration names and descriptions
        // n = name, n+1 = desc.
        // for radar, we only have one band
        if (ad == null) {
            return;
        }
        int[] bands = (int[]) bandTable.get(ad);
        int   index = (bands == null)
                      ? 0
                      : Arrays.binarySearch(bands, band);
        if (index < 0) {
            index = 0;
        }
        Vector         l                  = new Vector();
        Vector         v                  = ad.getCalInfo()[index];
        TwoFacedObject tfo                = null;
        int            preferredUnitIndex = 0;
        String         preferredUnit      = getDefault(PROP_UNIT, "BRIT");
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
        GuiUtils.setListData(unitComboBox, l);
        unitComboBox.setSelectedIndex(preferredUnitIndex);
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
        try {
            if ( !checkForValidValues()) {
                return;
            }
            if ( !getGoodToGo()) {
                updateStatus();
                return;
            }
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
        if (usePropFromUser(PROP_LOC)) {
            if (useLatLonBtn.isSelected()) {
                locKey   = PROP_LATLON;
                locValue = getUserPropValue(PROP_LATLON, dir);
            } else {
                locKey   = PROP_LINEELE;
                locValue = getUserPropValue(PROP_LINEELE, dir);
            }
        } else {
            locValue = getPropValue(PROP_LOC, dir);
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

}

