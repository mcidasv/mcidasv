/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;

import java.awt.Component;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Element;

import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaDirectoryList;
import edu.wisc.ssec.mcidas.McIDASException;
import edu.wisc.ssec.mcidas.adde.AddeURLException;
import edu.wisc.ssec.mcidas.adde.AddeSatBands;
import edu.wisc.ssec.mcidas.adde.AddeURL;

import visad.DateTime;
import visad.Gridded1DSet;
import visad.VisADException;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.data.imagery.BandInfo;
import ucar.unidata.data.imagery.ImageDataSource;
import ucar.unidata.data.imagery.ImageDataset;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.Format;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlNodeList;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;
import ucar.visad.UtcDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.servermanager.EntryStore;
import edu.wisc.ssec.mcidasv.ui.JCalendarPicker;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

/**
 * Widget to select images from a remote ADDE server Displays a list of the
 * descriptors (names) of the image datasets available for a particular ADDE
 * group on the remote server.
 * 
 * @author Don Murray
 */
public class AddeImageChooser extends AddeChooser implements
        ucar.unidata.ui.imagery.ImageSelector {

    private static final Logger logger = LoggerFactory.getLogger(AddeImageChooser.class);

    // TODO: get rid of this button right?
    public static JToggleButton mineBtn = GuiUtils
            .getToggleImageButton(
                    "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/internet-web-browser16.png",
                    "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/system-software-update16.png",
                    0, 0, true);

    /** _more_ */
    private static final int SIZE_THRESHOLD = 50;

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
    protected static final String PROP_LINELE = "LINELE";

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
    private static final String[] ADVANCED_PROPS = { PROP_UNIT, PROP_BAND,
            PROP_PLACE, PROP_LOC, PROP_SIZE, PROP_MAG, PROP_NAV };

    /** This is the list of labels used for the advanced gui */
    private static final String[] ADVANCED_LABELS = { "Data Type:", "Channel:",
            "Placement:", "Location:", "Image Size:", "Magnification:",
            "Navigation Type:" };

    /** Xml tag name for the defaults */
    protected static final String TAG_DEFAULT = "default";

    /** identifiere for the default value */
    protected static final String VALUE_DEFAULT = "default";

    /** Xml attr name for the defaults */
    protected static final String ATTR_NAME = "name";

    /** Xml attr name for the defaults */
    protected static final String ATTR_PATTERN = "pattern";

    /** flag for setting properties */
    private boolean amSettingProperties = false;

    /** Are we currently reading times */
    private Object readTimesTask;

    /** archive date */
    protected String archiveDay = null;

    /** Holds the properties */
    private JPanel propPanel;

    /** archive day button and label */
    protected JLabel archiveDayLabel;
    protected JButton archiveDayBtn;

    /** Maps the PROP_ property name to the gui component */
    private Hashtable propToComps = new Hashtable();

    /**
     * This is a list of hashtables, one per imagedefaults resource. The
     * Hashtables map the pattern to the xml node
     */
    private List resourceMaps;

    /** Holds the subsetting defaults */
    private XmlResourceCollection addeDefaults;

    /** archive date formatter */
    private SimpleDateFormat archiveDayFormatter;

    /** Input for lat/lon center point */
    protected LatLonWidget latLonWidget;

    /** Widget for the line magnfication in the advanced section */
    protected JSlider lineMagSlider;

    /** Label for the line mag. in the advanced section */
    protected JLabel lineMagLbl;

    /** Widget for the element magnfication in the advanced section */
    protected JSlider elementMagSlider;

    /** Label for the element mag. in the advanced section */
    protected JLabel elementMagLbl;

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

    /** _more_ */
    private JToggleButton lockBtn;

    /** full resolution button */
    private JButton fullResBtn;

    /** Identifier for the maximum number of bands */
    int MAX_BANDS = 100;

    /** The last AreaDirectory we have seen. */
    private AreaDirectory lastAD;

    /** The current AreaDirectory used for properties */
    private AreaDirectory propertiesAD;

    /** The previous AreaDirectory used for properties */
    private AreaDirectory prevPropertiesAD;

    /** Mapping of area directory to list of BandInfos */
    protected Hashtable bandTable;

    /**
     * The list of currently loaded AddeImageDescriptor-s
     */
    protected Vector imageDescriptors;

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
    private boolean useSatBandInfo = true;

    /** Used to parse the sat band file */
    private AddeSatBands satBandInfo;

    /** Widget for selecting the band */
    protected JComboBox bandComboBox;

    /** string for ALL */
    protected static final String ALL = "ALL";

    /** object for selecting all bands */
    protected static final TwoFacedObject ALLBANDS = new TwoFacedObject(
            "All Bands", ALL);

    /** object for selecting all calibrations */
    protected static final TwoFacedObject ALLUNITS = new TwoFacedObject(
            "All Types", ALL);

    /**
     * Keep track of the lines to element ratio
     */
    private double linesToElements = 1.0;

    /**
     * limit of slider
     */
    private static final int SLIDER_MAX = 29;

    /**
     * the list of band infos
     */
    private List<BandInfo> bandInfos;

    /**
     * Construct an Adde image selection widget
     * 
     * 
     * @param mgr
     *            The chooser manager
     * @param root
     *            The chooser.xml node
     */
    public AddeImageChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);

        addDescComp(loadButton);

        archiveDayBtn = GuiUtils.makeImageButton(
                "/auxdata/ui/icons/calendar_edit.png", this, "getArchiveDay", null,
                true);
        archiveDayBtn.setToolTipText("Select a day for archive datasets");
        archiveDayLabel = new JLabel("Select day:");

        this.addeDefaults = getImageDefaults();
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
     * Update labels, enable widgets, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        if (getDoAbsoluteTimes()) {
            setPropertiesState(getASelectedTime());
        } else {
            setPropertiesState(lastAD);
        }

        if (readTimesTask != null) {
            if (taskOk(readTimesTask)) {
                setStatus("Reading available times from server");
            }
        } else if (getDoAbsoluteTimes() && !haveTimeSelected()) {
            setStatus(MSG_TIMES);
        }
        enableWidgets();
    }

    /**
     * Do we have times selected. Either we are doing absolute times and there
     * are some selected in the list. Or we are doing relative times and we have
     * done a connect to the server
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
     * @param msg
     *            log message
     * @param exc
     *            Exception to log
     */
    public void logException(String msg, Exception exc) {
        LogUtil.logException(msg, exc);
    }

    /**
     * This allows derived classes to provide their own name for labeling, etc.
     * 
     * @return the dataset name
     */
    public String getDataName() {
        return "Image Data";
    }

    /**
     * Get the descriptor widget label
     * 
     * @return label for the descriptor widget
     */
    public String getDescriptorLabel() {
        return "Image Type";
    }
        
    /**
     * Respond to a change in the descriptor list.
     */
    protected void checkSetNav() {
        String descriptor = getDescriptor();
        if (descriptor!=null) {
            String[] suffixes = { "AMSU", "HIRS", "HRPT", "GAC", "LAC" };
            for (int i=0; i<suffixes.length; i++) {
                Pattern p = Pattern.compile("N\\d\\d" + suffixes[i]);
                Matcher m = p.matcher(descriptor);              
                if (m.find()) {
                    navComboBox.setSelectedIndex(1);
                    break;
                }
            }
        }
    }

    /**
     * Get the name of the dataset.
     * 
     * @return descriptive name of the dataset.
     */
    public String getDatasetName() {
        StringBuffer buf = new StringBuffer();
        buf.append(getSelectedDescriptor());
        if (bandComboBox != null && bandComboBox.getItemCount() > 1) {
            buf.append(" (");
            buf.append(bandComboBox.getSelectedItem());
            buf.append(")");
        }
        return buf.toString();
    }
    
    /**
     * Check if we are ready to read times
     * 
     * @return true if times can be read
     */
    protected boolean canReadTimes() {
        return haveDescriptorSelected();
    }

    /**
     * Handle when the user presses the update button
     * 
     * @throws Exception
     *             On badness
     */
    public void handleUpdate() throws Exception {
        if (getState() != STATE_CONNECTED) {
            // If not connected then update the server list
            updateServerList();
        } else {
            // If we are already connected then update the rest of the chooser
            descriptorChanged();
        }
        updateStatus();
    }

    /**
     * Do server connection stuff... override this with type-specific methods
     */
    protected void readFromServer() {
        archiveDay = null;
        if (archiveDayLabel != null) {
            archiveDayLabel.setText("Select day:");
        }
        readSatBands();
        super.readFromServer();
    }

    /**
     * Overwrite base class method to clear out the lastAD member here.
     */
    protected void clearTimesList() {
        lastAD = null;
        super.clearTimesList();
    }

    /**
     * Show the archive dialog. This method is not meant to be called but is
     * public by reason of implementation (or insanity).
     */
    public void getArchiveDay() {
        final JDialog dialog = GuiUtils.createDialog("Set Archive Day", true);
        String timeZone = (String)getIdv().getPreference(PREF_TIMEZONE, DEFAULT_TIMEZONE);
        final JCalendarPicker picker = new JCalendarPicker(null, timeZone, false);
        if (archiveDay != null) {
            if (archiveDayFormatter == null) {
                archiveDayFormatter = new SimpleDateFormat(UtcDate.YMD_FORMAT);
            }
            Date d = null;
            try {
                d = archiveDayFormatter.parse(archiveDay);
                picker.setDate(d);
            } catch (Exception e) {
                logException("parsing archive day " + archiveDay, e);
            }
        }

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_REMOVE)) {
                    archiveDay = null;
                    archiveDayLabel.setText("Select day:");
                    setDoAbsoluteTimes(true);
                    descriptorChanged();
                } else if (cmd.equals(GuiUtils.CMD_OK)) {
                    try {
                        DateTime dt = new DateTime(picker.getDate());
                        archiveDay = UtcDate.getYMD(dt);
                        // archiveDayLabel.setText(UtcDate.formatUtcDate(dt,
                        // "MMM dd, yyyy"));
                        archiveDayLabel.setText(archiveDay);
                    } catch (Exception e) {
                    }
                    // System.out.println("archiveDay = " + archiveDay);
                    setDoAbsoluteTimes(true);
                    descriptorChanged();
                }
                dialog.dispose();
            }
        };

        JPanel buttons = GuiUtils.makeButtons(listener, new String[] {
                GuiUtils.CMD_OK, GuiUtils.CMD_REMOVE, GuiUtils.CMD_CANCEL });

        JComponent contents = GuiUtils.topCenterBottom(GuiUtils.inset(GuiUtils
                .lLabel("Please select a day for this dataset:"), 10), GuiUtils
                .inset(picker, 10), buttons);
        Point p = new Point(200, 200);
        if (archiveDayBtn != null) {
            try {
                p = archiveDayBtn.getLocationOnScreen();
            } catch (IllegalComponentStateException ice) {
            }
        }
        dialog.setLocation(p);
        dialog.getContentPane().add(contents);
        dialog.pack();
        dialog.setVisible(true);
    }

    /**
     * Add the bottom advanced gui panel to the list
     * 
     * @param bottomComps
     *            the bottom components
     */
    protected void getBottomComponents(List bottomComps) {

        String[] propArray = getAdvancedProps();
        String[] labelArray = getAdvancedLabels();
        // List bottomComps = new ArrayList();
        Insets dfltGridSpacing = new Insets(4, 0, 4, 0);
        String dfltLblSpacing = " ";

        boolean haveBand = Misc.toList(propArray).contains(PROP_BAND);
        boolean haveNav = Misc.toList(propArray).contains(PROP_NAV);
        for (int propIdx = 0; propIdx < propArray.length; propIdx++) {
            JComponent propComp = null;
            String prop = propArray[propIdx];
            if (prop.equals(PROP_UNIT)) {
                unitComboBox = new JComboBox();
                addPropComp(PROP_UNIT, propComp = unitComboBox);
                GuiUtils.setPreferredWidth(unitComboBox, 100);
                if (haveBand) {
                    bandComboBox = new JComboBox();
                    bandComboBox.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            setAvailableUnits(propertiesAD, getSelectedBand());
                        }
                    });
                    addPropComp(PROP_BAND, bandComboBox);

                    propComp = GuiUtils.hbox(propComp, GuiUtils.inset(
                            new JLabel("Channel:"), new Insets(0, 10, 0, 5)),
                            bandComboBox, 5);
                }
            } else if (prop.equals(PROP_BAND)) {
                // Moved to PROP_UNIT
            } else if (prop.equals(PROP_PLACE)) {
                // Moved to PROP_LOC
            } else if (prop.equals(PROP_LOC)) {
                placeLbl = GuiUtils.getFixedWidthLabel("");
                changePlace(PLACE_CENTER);
                addPropComp(PROP_PLACE, placeLbl);

                latLonWidget = new LatLonWidget();
                centerLineFld = new JTextField("", 3);
                centerElementFld = new JTextField("", 3);

                fullResBtn = GuiUtils.makeImageButton(
                        "/auxdata/ui/icons/arrow_out.png", this,
                        "setToFullResolution");
                fullResBtn.setContentAreaFilled(false);
                fullResBtn.setToolTipText("Set to full resolution");

                // lockBtn =
                // GuiUtils.getToggleImageButton(IdvUIManager.ICON_UNLOCK,
                // IdvUIManager.ICON_LOCK, 0, 0, true);
                // lockBtn.setContentAreaFilled(false);
                // lockBtn.setSelected(true);
                // lockBtn.setToolTipText(
                // "Unlock to automatically change size when changing magnification");

                final JButton centerPopupBtn = GuiUtils.getImageButton(
                        "/auxdata/ui/icons/MapIcon16.png", getClass());
                centerPopupBtn.setToolTipText("Center on current displays");
                centerPopupBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        getIdv().getIdvUIManager().popupCenterMenu(
                                centerPopupBtn, latLonWidget);
                    }
                });
                JComponent centerPopup = GuiUtils.inset(centerPopupBtn,
                        new Insets(0, 0, 0, 4));

                GuiUtils.tmpInsets = dfltGridSpacing;
                final JPanel latLonPanel = GuiUtils.hbox(new Component[] {
                        GuiUtils.rLabel(" Lat:" + dfltLblSpacing),
                        latLonWidget.getLatField(),
                        GuiUtils.rLabel(" Lon:" + dfltLblSpacing),
                        latLonWidget.getLonField(), new JLabel(" "),
                        centerPopup });

                final JPanel lineElementPanel = GuiUtils.hbox(new Component[] {
                        GuiUtils.rLabel(" Line:" + dfltLblSpacing),
                        centerLineFld,
                        GuiUtils.rLabel(" Element:" + dfltLblSpacing),
                        centerElementFld });

                locationPanel = new GuiUtils.CardLayoutPanel();
                locationPanel.addCard(latLonPanel);
                locationPanel.addCard(lineElementPanel);

                JButton locPosButton = GuiUtils.makeImageButton(
                        "/auxdata/ui/icons/Refresh16.gif", this, "cyclePlace",
                        null, true);

                locPosButton.setToolTipText("Change place type");

                JButton locTypeButton = GuiUtils.makeImageButton(
                        "/auxdata/ui/icons/Refresh16.gif", locationPanel,
                        "flip", null, true);
                locTypeButton
                        .setToolTipText("Toggle between Latitude/Longitude and Line/Element");

                propComp = GuiUtils.hbox(new Component[] { locPosButton,
                        placeLbl, locTypeButton, locationPanel }, 5);
                addPropComp(PROP_LOC, propComp);
            } else if (prop.equals(PROP_MAG)) {
                boolean oldAmSettingProperties = amSettingProperties;
                amSettingProperties = true;
                ChangeListener lineListener = new javax.swing.event.ChangeListener() {
                    public void stateChanged(ChangeEvent evt) {
                        if (amSettingProperties) {
                            return;
                        }
                        lineMagSliderChanged(!getLockButton().isSelected());
                    }
                };
                ChangeListener elementListener = new ChangeListener() {
                    public void stateChanged(javax.swing.event.ChangeEvent evt) {
                        if (amSettingProperties) {
                            return;
                        }
                        elementMagSliderChanged(true);
                    }
                };
                JComponent[] lineMagComps = GuiUtils.makeSliderPopup(
                        -SLIDER_MAX, SLIDER_MAX, 0, lineListener);
                lineMagSlider = (JSlider) lineMagComps[1];
                lineMagSlider.setMajorTickSpacing(1);
                lineMagSlider.setSnapToTicks(true);
                lineMagSlider.setExtent(1);
                lineMagComps[0].setToolTipText("Change the line magnification");
                JComponent[] elementMagComps = GuiUtils.makeSliderPopup(
                        -SLIDER_MAX, SLIDER_MAX, 0, elementListener);
                elementMagSlider = (JSlider) elementMagComps[1];
                elementMagSlider.setExtent(1);
                elementMagSlider.setMajorTickSpacing(1);
                elementMagSlider.setSnapToTicks(true);
                elementMagComps[0]
                        .setToolTipText("Change the element magnification");
                lineMagSlider
                        .setToolTipText("Slide to set line magnification factor");
                lineMagLbl = GuiUtils.getFixedWidthLabel(StringUtil.padLeft(
                        "1", 4));
                elementMagSlider
                        .setToolTipText("Slide to set element magnification factor");
                elementMagLbl = GuiUtils.getFixedWidthLabel(StringUtil.padLeft(
                        "1", 4));
                amSettingProperties = oldAmSettingProperties;

                GuiUtils.tmpInsets = dfltGridSpacing;
                /*
                 * JPanel magPanel = GuiUtils.doLayout(new Component[] {
                 * GuiUtils.rLabel("Line:" + dfltLblSpacing), lineMagLbl,
                 * GuiUtils.inset(lineMagComps[0], new Insets(0, 4, 0, 0)),
                 * GuiUtils.rLabel("   Element:" + dfltLblSpacing),
                 * elementMagLbl, GuiUtils.inset(elementMagComps[0], new
                 * Insets(0, 4, 0, 0)), }, 6, GuiUtils.WT_N, GuiUtils.WT_N);
                 */
                /*
                 * JPanel magPanel = GuiUtils.doLayout(new Component[] {
                 * lineMagLbl, GuiUtils.inset(lineMagComps[0], new Insets(0, 4,
                 * 0, 0)), new JLabel("    X "), elementMagLbl,
                 * GuiUtils.inset(elementMagComps[0], new Insets(0, 4, 0, 0)),
                 * }, 6, GuiUtils.WT_N, GuiUtils.WT_N);
                 */

                JPanel magPanel = GuiUtils.doLayout(
                        new Component[] {
                                lineMagLbl,
                                GuiUtils.inset(lineMagComps[0], new Insets(0,
                                        4, 0, 0)),
                                new JLabel("    X"),
                                elementMagLbl,
                                GuiUtils.inset(elementMagComps[0], new Insets(
                                        0, 4, 0, 0)),
                                GuiUtils.inset(getLockButton(), new Insets(0,
                                        10, 0, 0)) }, 7, GuiUtils.WT_N,
                        GuiUtils.WT_N);

                addPropComp(PROP_MAG, propComp = magPanel);
                if (haveNav) {
                    navComboBox = new JComboBox();
                    GuiUtils.setListData(navComboBox, Misc.newList(
                            new TwoFacedObject("Default", "X"),
                            new TwoFacedObject("Lat/Lon", "LALO")));
                    addPropComp(PROP_NAV, navComboBox);
                    boolean showNav = false;
                    showNav = getProperty("includeNavComp", false);
                    if (showNav) {
                        propComp = GuiUtils.hbox(propComp, GuiUtils.inset(
                                new JLabel("Navigation Type:"), new Insets(0,
                                        10, 0, 5)), navComboBox, 5);
                    }
                }
            } else if (prop.equals(PROP_SIZE)) {
                numLinesFld = new JTextField("", 4);
                numElementsFld = new JTextField("", 4);
                numLinesFld.setToolTipText("Number of lines");
                numElementsFld.setToolTipText("Number of elements");
                GuiUtils.tmpInsets = dfltGridSpacing;
                sizeLbl = GuiUtils.lLabel("");

                /*
                 * JPanel sizePanel = GuiUtils.left(GuiUtils.doLayout(new
                 * Component[] { GuiUtils.rLabel("Lines:" + dfltLblSpacing),
                 * numLinesFld, GuiUtils.rLabel(" Elements:" + dfltLblSpacing),
                 * numElementsFld, new JLabel(" "), sizeLbl }, 6, GuiUtils.WT_N,
                 * GuiUtils.WT_N));
                 */

                JPanel sizePanel = GuiUtils.left(GuiUtils.doLayout(
                        new Component[] { numLinesFld, new JLabel(" X "),
                                numElementsFld, fullResBtn, /* new JLabel(" "), */
                                sizeLbl }, 5, GuiUtils.WT_N, GuiUtils.WT_N));
                addPropComp(PROP_SIZE, propComp = sizePanel);
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
     * Get the "lock" button
     * 
     * @return the lock button
     */
    private JToggleButton getLockButton() {
        if (lockBtn == null) {
            lockBtn = GuiUtils.getToggleImageButton(
                    "/auxdata/ui/icons/Linked.gif",
                    "/auxdata/ui/icons/Unlinked.gif", 0, 0, true);
            lockBtn.setContentAreaFilled(false);
            lockBtn.setSelected(true);
            lockBtn
                    .setToolTipText("Unlock to automatically change size when changing magnification");
        }
        return lockBtn;

    }

    /**
     * Set to full resolution
     */
    public void setToFullResolution() {

        if (propertiesAD == null) {
            return;
        }
        amSettingProperties = true;
        numLinesFld.setText("" + propertiesAD.getLines());
        numElementsFld.setText("" + propertiesAD.getElements());
        changePlace(PLACE_CENTER);
        if (useLatLon()) {
            locationPanel.flip();
        }
        centerLineFld.setText("" + (propertiesAD.getLines() / 2));
        centerElementFld.setText("" + (propertiesAD.getElements() / 2));

        setMagSliders(1, 1);
        amSettingProperties = false;

    }

    /**
     * Cycle the place
     */
    public void cyclePlace() {
        if (place.equals(PLACE_CENTER)) {
            changePlace(PLACE_ULEFT);
        } else {
            changePlace(PLACE_CENTER);
        }
    }

    /**
     * Change the place
     * 
     * @param newPlace
     *            new place
     */
    public void changePlace(String newPlace) {
        this.place = newPlace;
        String s = translatePlace(place) + "=";
        placeLbl.setText(StringUtil.padRight(s, 12));
    }

    /**
     * _more_
     * 
     * @param recomputeLineEleRatio
     *            _more_
     */
    protected void elementMagSliderChanged(boolean recomputeLineEleRatio) {
        int value = getElementMagValue();
        if ((Math.abs(value) < SLIDER_MAX)) {
            int lineMag = getLineMagValue();
            if (lineMag > value) {
                linesToElements = Math.abs(lineMag / (double) value);
            } else {
                linesToElements = Math.abs((double) value / lineMag);
            }
        }
        // System.out.println(" changelistener: linesToElements = " +
        // linesToElements);
        elementMagLbl.setText(StringUtil.padLeft("" + value, 4));
        if (!getLockButton().isSelected()) {
            if (value > 0) {
                numElementsFld.setText("" + (int) (baseNumElements * value));
            } else {
                numElementsFld.setText(""
                        + (int) (baseNumElements / (double) -value));
            }
        }
    }

    /**
     * Handle the line mag slider changed event
     * 
     * 
     * @param autoSetSize
     *            _more_
     */
    protected void lineMagSliderChanged(boolean autoSetSize) {
        try {
            int value = getLineMagValue();
            lineMagLbl.setText(StringUtil.padLeft("" + value, 4));
            if (autoSetSize) {
                if (value > 0) {
                    numLinesFld.setText("" + (int) (baseNumLines * value));
                } else {
                    numLinesFld.setText(""
                            + (int) (baseNumLines / (double) -value));
                }
            }

            if (value == 1) { // special case
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

            value = (value > 0) ? value - 1 : value + 1; // since slider is one
                                                            // off
            amSettingProperties = true;
            elementMagSlider.setValue(value);
            amSettingProperties = false;
            elementMagSliderChanged(false);
        } catch (Exception exc) {
            logException("Setting line magnification", exc);
        }
        // amSettingProperties = false;
    }

    /**
     * Get the value of the line magnification slider.
     * 
     * @return The magnification value for the line
     */
    protected int getLineMagValue() {
        return getMagValue(lineMagSlider);
    }

    /**
     * Get the value of the element magnification slider.
     * 
     * @return The magnification value for the element
     */
    protected int getElementMagValue() {
        return getMagValue(elementMagSlider);
    }

    /**
     * Handle the absolute time selection changing
     */
    protected void absoluteTimesSelectionChanged() {
        if (!getDoAbsoluteTimes()) {
            return;
        }
        if (getIdv().getProperty("idv.chooser.addeimage.updateontimechange",
                true)) {
            setPropertiesState(getASelectedTime(), false);
        }
    }

    /**
     * Set the relative and absolute extra components
     */
    protected JPanel makeTimesPanel() {
        JComponent extra = getExtraTimeComponent();
        JPanel timesPanel = super.makeTimesPanel(null, extra);
        return timesPanel;
    }

    /**
     * Get the time popup widget
     * 
     * @return a widget for selecing the day
     */
    protected JComponent getExtraTimeComponent() {
        return McVGuiUtils.makeLabeledComponent(archiveDayLabel, archiveDayBtn);
    }

    /**
     * Associates the given JComponent with the PROP_ property identified by the
     * given propId
     * 
     * @param propId
     *            The property
     * @param comp
     *            The gui component that allows the user to set the property
     * 
     * @return Just returns the given comp
     */
    protected JComponent addPropComp(String propId, JComponent comp) {
        Object oldComp = propToComps.get(propId);
        if (oldComp != null) {
            throw new IllegalStateException("Already have a component defined:"
                    + propId);
        }
        propToComps.put(propId, comp);
        return comp;
    }

    /**
     * Should we use the user supplied property
     * 
     * @param propId
     *            The property
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
     * Get one of the selected times.
     * 
     * @return One of the selected times.
     */
    protected AreaDirectory getASelectedTime() {
        if (haveTimeSelected()) {
            List selected = getSelectedAbsoluteTimes();
            if (selected.size() > 0) {
                AddeImageDescriptor aid = (AddeImageDescriptor) selected.get(0);
                if (aid != null) {
                    return aid.getDirectory();
                }
            }
        }
        return null;
    }

    /**
     * Enable or disable the GUI widgets based on what has been selected.
     */
    protected void enableWidgets() {
        boolean descriptorState = ((getState() == STATE_CONNECTED) && canReadTimes());

        for (int i = 0; i < compsThatNeedDescriptor.size(); i++) {
            JComponent comp = (JComponent) compsThatNeedDescriptor.get(i);
            GuiUtils.enableTree(comp, descriptorState);
        }

        boolean timesOk = timesOk();
        if (propPanel != null) {
            GuiUtils.enableTree(propPanel, timesOk);
        }

        // Require times to be selected
        GuiUtils.enableTree(loadButton, descriptorState && timesOk);

        if (timesOk) {
            checkCenterEnabled();
        }
        checkTimesLists();

        // TODO: This is temporary... absolute times on Windows makes the local
        // servers choke
        // Update: this works now, but leave it here as a reminder
        // boolean localWindowsServer = isLocalServer() &&
        // System.getProperty("os.name").startsWith("Windows");
        // setDoAbsoluteTimes(getDoAbsoluteTimes() && !localWindowsServer);

        enableAbsoluteTimesList(getDoAbsoluteTimes() && descriptorState);

        getRelativeTimesChooser().setEnabled(
                !getDoAbsoluteTimes() && descriptorState);

        revalidate();
    }

    /**
     * Check if we are using the lat/lon widget
     * 
     * @return true if we are using the lat/lon widget
     */
    protected boolean useLatLon() {
        return locationPanel.getVisibleIndex() == 0;
    }

    /**
     * Enable or disable the center lat/lon and line/element widgets
     */
    protected void checkCenterEnabled() {
        // NOT NOW
        if (true) {
            return;
        }

        boolean usingLatLon = useLatLon();

        latLonWidget.getLatField().setEnabled(usingLatLon);
        latLonWidget.getLonField().setEnabled(usingLatLon);
        // centerLatLbl.setEnabled(usingLatLon);
        // centerLonLbl.setEnabled(usingLatLon);

        centerLineFld.setEnabled(!usingLatLon);
        centerElementFld.setEnabled(!usingLatLon);
        // centerLineLbl.setEnabled( !usingLatLon);
        // centerElementLbl.setEnabled( !usingLatLon);

    }

    /**
     * Get the selected calibration unit.
     * 
     * @return the selected calibration unit
     */
    protected String getSelectedUnit() {
        if (unitComboBox==null || unitComboBox.getSelectedItem()==null) return "";
        String selection = (String) ((TwoFacedObject) unitComboBox.getSelectedItem()).getId();
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
     * @return a description of the currently selected dataset
     * @deprecated use #getDatasetName()
     */
    public String getDatasetDescription() {
        return getDatasetName();
    }

    /**
     * Read the set of image times available for the current server/group/type
     * This method is a wrapper, setting the wait cursor and wrapping the call
     * to {@link #readTimesInner()}; in a try/catch block
     */
    public void readTimes() {
        clearTimesList();
        if (!canReadTimes()) {
            return;
        }
        Misc.run(new Runnable() {
            public void run() {
                updateStatus();
                showWaitCursor();
                try {
                    readTimesInner();
                    checkSetNav();
                } catch (Exception e) {
                    handleConnectionError(e);
                }
                showNormalCursor();
                updateStatus();
            }
        });
    }

    /**
     * _more_
     */
    public void doCancel() {
        readTimesTask = null;
        setState(STATE_UNCONNECTED);
        super.doCancel();
    }

    /** locking mutex */
    private Object MUTEX = new Object();

    /**
     * Set the list of dates/times based on the image selection
     * 
     */
    protected void readTimesInner() {
        String descriptor = getDescriptor();
        String pos = (getDoAbsoluteTimes() || (archiveDay != null)) ? "all" : "0";

        StringBuffer addeCmdBuff = getGroupUrl(REQ_IMAGEDIR, getGroup());
        String id = getSelectedStation();
        if (id != null) {
            appendKeyValue(addeCmdBuff, PROP_ID, id);
        }
        appendKeyValue(addeCmdBuff, PROP_DESCR, descriptor);
        appendKeyValue(addeCmdBuff, PROP_POS, "" + pos);
        if (archiveDay != null) {
            appendKeyValue(addeCmdBuff, PROP_DAY, archiveDay);
        }
        String url = addeCmdBuff.toString();
        readTimesTask = startTask();
        updateStatus();
        Object task = readTimesTask;
        try {
            AreaDirectoryList adir = new AreaDirectoryList(url);
            // Make sure no other loads are occurred
            boolean ok = stopTaskAndIsOk(task);
            if (!Misc.equals(readTimesTask, task) || !ok) {
                return;
            }
            readTimesTask = null;

            synchronized (MUTEX) {
                // Array of AreaDirectory-s sorted by time
                AreaDirectory[][] dirs = adir.getSortedDirs();
                int numImages = dirs.length;
                imageDescriptors = new Vector();
                // TODO: Add a setBands method to AreaDirectory to replace bandtable
                bandTable = new Hashtable(numImages);
                lastAD = null;
                for (int i = 0; i < numImages; i++) {
                    int bandIndex = 0;
                    lastAD = (AreaDirectory) dirs[i][0];
                    int[] allBands = new int[MAX_BANDS];
                    Vector[] allCals = new Vector[MAX_BANDS];
                    for (int j = 0; j < dirs[i].length; j++) {
                        int nbands = dirs[i][j].getNumberOfBands();
                        int[] abands = dirs[i][j].getBands();
                        Vector[] vb = dirs[i][j].getCalInfo();
                        for (int k = 0; k < nbands; k++) {
                            allBands[bandIndex] = abands[k];
                            allCals[bandIndex++] = vb[k];
                        }
                    }
                    int[] bands = new int[bandIndex];
                    System.arraycopy(allBands, 0, bands, 0, bandIndex);
                    Vector[] cals = new Vector[bandIndex];
                    System.arraycopy(allCals, 0, cals, 0, bandIndex);
                    lastAD.setCalInfo(cals);
                    bandTable.put(lastAD, bands);
                    AddeImageDescriptor aid = new AddeImageDescriptor(lastAD, null);
                    imageDescriptors.add(aid);
                }

                Collections.sort(imageDescriptors);
                if (getDoAbsoluteTimes()) {
                    setAbsoluteTimes(imageDescriptors);
                }
                // revalidate();
            }
            setState(STATE_CONNECTED);
        } catch (McIDASException e) {
            logger.warn("Exception from loadImages", e);
            stopTask(task);
            readTimesTask = null;
            handleConnectionError(e);
        }
    }

    /**
     * Set the selected times in the times list based on the input times
     * 
     * @param times
     *            input times
     */
    protected void setSelectedTimes(DateTime[] times) {
        if ((times == null) || (times.length == 0)) {
            return;
        }
        DateTime[] imageTimes = new DateTime[times.length];

        if (imageDescriptors != null) {
            imageTimes = new DateTime[imageDescriptors.size()];
            for (int idIdx = 0; idIdx < imageDescriptors.size(); idIdx++) {
                AddeImageDescriptor aid = (AddeImageDescriptor) imageDescriptors.get(idIdx);
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
                    timesValues[0][i] = times[i].getValue(imageSet.getSetUnits()[0]);
                }
                setSelectedAbsoluteTimes(imageSet.doubleToIndex(timesValues));
                absoluteTimesSelectionChanged();
            } catch (VisADException ve) {
                logException("Unable to set times from display", ve);
            }
        }
    }

    /**
     * Set the center location portion of the request. If the input from the
     * widget is null, use the centerpoint from the image descriptor.
     * 
     * @param aid
     *            image descriptor for the image
     */
    protected void setCenterLocation(AddeImageDescriptor aid) {
        String latPoint = "";
        String lonPoint = "";
        if (aid != null) {
            AreaDirectory ad = aid.getDirectory();
            latPoint = "" + ad.getCenterLatitude();
            lonPoint = "" + ad.getCenterLongitude();
        }
        if (!latPoint.trim().equals("")) {
            latLonWidget.setLat(latPoint);
        }
        if (!lonPoint.trim().equals("")) {
            latLonWidget.setLon(lonPoint);
        }
    }

    /**
     * get the adde server grup type to use
     * 
     * @return group type
     */
    @Override
    protected String getGroupType() {
        return AddeServer.TYPE_IMAGE;
    }

    /**
     * Does this selector have all of its state set to load in data
     * 
     * @return Has the user chosen everything they need to choose to load data
     */
    protected boolean getGoodToGo() {
        // if(!super.getGoodToGo()) return false;
        if (getDoAbsoluteTimes()) {
            return getHaveAbsoluteTimesSelected();
        } else {
            return canReadTimes() && (lastAD != null);
        }
    }

    /**
     * Returns a list of the images to load or null if none have been selected.
     * 
     * @return list get the list of image descriptors
     */
    public List getImageList() {
        if (!timesOk()) {
            return null;
        }
        List images = new ArrayList();
        String defaultBand = getDefault(PROP_BAND, ALL);
        try {
            if (getDoRelativeTimes()) {
                AddeImageDescriptor firstDescriptor = (AddeImageDescriptor) imageDescriptors.get(0);
                AreaDirectory ad = firstDescriptor.getDirectory();
                int[] bands    = (int[]) bandTable.get(ad);
                bandInfos = makeBandInfos(ad, bands);
                int[] relativeTimesIndices = getRelativeTimeIndices();
                for (int i = 0; i < relativeTimesIndices.length; i++) {
                    AddeImageDescriptor aid = new AddeImageDescriptor(relativeTimesIndices[i], firstDescriptor);
                    AddeImageInfo aii = makeImageInfo(aid.getDirectory(), true, relativeTimesIndices[i]);
                    aii.setDebug(EntryStore.isAddeDebugEnabled(false));
                    if (aii.getBand() == null) aii.setBand(defaultBand);
                    aid.setImageInfo(aii);
                    aid.setSource(aii.getURLString());
                    images.add(aid);
                }
            } else {
                List selectedTimes = getSelectedAbsoluteTimes();
                for (int i = 0; i < selectedTimes.size(); i++) {
                    AddeImageDescriptor aid = new AddeImageDescriptor((AddeImageDescriptor) selectedTimes.get(i));
                    AddeImageInfo aii = makeImageInfo(aid.getDirectory(), false, i);
                    aii.setDebug(EntryStore.isAddeDebugEnabled(false));
                    if (aii.getBand() == null) aii.setBand(defaultBand);
                    aid.setImageInfo(aii);
                    aid.setSource(aii.getURLString());
                    images.add(aid);
                }
            }
        } catch (Exception exc) {
            logException("Error occured", exc);
            return null;
        }
        return images;
    }

    /**
     * Process the image defaults resources
     */
    protected void initializeAddeDefaults() {
        resourceMaps = new ArrayList();
        if (addeDefaults == null) {
            return;
        }
        for (int resourceIdx = 0; resourceIdx < addeDefaults.size(); resourceIdx++) {
            Element root = addeDefaults.getRoot(resourceIdx);
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

    /**
     * Get the default value for a key
     * 
     * @param property
     *            property (key type)
     * @param dflt
     *            default value
     * @return value for key or dflt if not found
     */
    protected String getDefault(String property, String dflt) {
        if (resourceMaps == null) {
            initializeAddeDefaults();
        }
        property = property.toLowerCase();

        String userDefault = null;
        String server = getServer();
        String group = getGroup();
        String descriptor = getDescriptor();
        String[] keys = { userDefault, server + ":" + group + "/" + descriptor,
                group + "/" + descriptor, server + ":" + group + "/*",
                group + "/*", server + ":*/" + descriptor, "*/" + descriptor,
                descriptor, server + ":*/*", server, "*" };

        if (server != null) {
            if (PROP_USER.equals(property))
                return getLastAddedUser();
            if (PROP_PROJ.equals(property))
                return getLastAddedProj();
        }

        for (int resourceIdx = 0; resourceIdx < resourceMaps.size(); resourceIdx++) {
            Hashtable resourceMap = (Hashtable) resourceMaps.get(resourceIdx);
            for (int keyIdx = 0; keyIdx < keys.length; keyIdx++) {
                String key = keys[keyIdx];
                if (key == null) {
                    continue;
                }
                key = key.toLowerCase();
                Element defaultNode = (Element)resourceMap.get(key);
                if (defaultNode == null) {
                    continue;
                }
                String value = XmlUtil.getAttribute(defaultNode, property, (String)null);
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
     * Get the image size string from the directory and defaults
     * 
     * @param ad
     *            image directory
     * @return request size
     */
    protected String getSizeString(AreaDirectory ad) {
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
     * @return true if values are valid
     */
    protected boolean checkForValidValues() {
        if (usePropFromUser(PROP_LOC)) {
            if (useLatLon()) {
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
     * Get the list of properties for the base URL
     * 
     * @return list of properties
     */
    protected String[] getBaseUrlProps() {
        return new String[] { PROP_DESCR, PROP_UNIT, PROP_SPAC, PROP_BAND,
                PROP_NAV, PROP_USER, PROP_PROJ, };
    }

    /**
     * A utility that creates the url argument line for the given set of
     * properties.
     * 
     * @param props
     *            The PROP_ properties to make the request string for
     * @param ad
     *            The area directory.
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
     * @param prop
     *            The property
     * @param ad
     *            The AreaDirectory
     * 
     * @return The value of the property to use in the request string
     */
    protected String getPropValue(String prop, AreaDirectory ad) {
        if (usePropFromUser(prop)) {
            return getUserPropValue(prop, ad);
        }

        // Handle size specially because we really want to get the minimum of
        // the default and the ad size
        if (PROP_SIZE.equals(prop)) {
            int[] size = getSize(ad);
            return size[0] + " " + size[1];
        }

        return getDefault(prop, getDefaultPropValue(prop, ad, false));
    }

    /**
     * Get the user supplied property value for the adde request string
     * 
     * @param prop
     *            The property
     * @param ad
     *            The AreaDirectory
     * 
     * @return The value, supplied by the user, of the property to use in the
     *         request string
     */
    protected String getUserPropValue(String prop, AreaDirectory ad) {
        if (PROP_LATLON.equals(prop) && (latLonWidget != null)) {
            // apparently the ADDE server can't handle long numbers
            return Format.dfrac(latLonWidget.getLat(), 5) + " "
                    + Format.dfrac(latLonWidget.getLon(), 5);
        }
        if (PROP_PLACE.equals(prop) && (placeLbl != null)) {
            return place;
        }

        if (PROP_LINELE.equals(prop) && (centerLineFld != null)) {
            return centerLineFld.getText().trim() + " "
                    + centerElementFld.getText().trim();
        }

        if (PROP_SIZE.equals(prop) && (numLinesFld != null)) {
            return numLinesFld.getText().trim() + " "
                    + numElementsFld.getText().trim();
        }
        if (PROP_MAG.equals(prop) && (lineMagSlider != null)) {
            return getLineMagValue() + " " + getElementMagValue();
        }
        if (PROP_BAND.equals(prop) && (bandComboBox != null)) {

            Object selected = bandComboBox.getSelectedItem();
            if (selected != null) {
                if (selected.equals(ALLBANDS)) {
                    return ALLBANDS.toString();
                } else {
                    return "" + ((BandInfo) selected).getBandNumber();
                }
            }
        }
        if (PROP_UNIT.equals(prop)) {
            return getSelectedUnit();
        }
        if (PROP_NAV.equals(prop)) {
            return TwoFacedObject.getIdString(navComboBox.getSelectedItem());
        }

        if (PROP_USER.equals(prop))
            return getLastAddedUser();
        if (PROP_PROJ.equals(prop))
            return getLastAddedProj();

        return null;
    }

    /**
     * Get the default property value for the adde request string
     * 
     * @param prop
     *            The property
     * @param ad
     *            The AreaDirectory
     * @param forDisplay
     *            Is this to display to the user in the gui
     * 
     * @return The default of the property to use in the request string
     */
    protected String getDefaultPropValue(String prop, AreaDirectory ad,
            boolean forDisplay) {
        if (PROP_USER.equals(prop)) {
            return DEFAULT_USER;
        }
        if (PROP_USER.equals(prop)) {
            return PLACE_CENTER;
        }
        if (PROP_PROJ.equals(prop)) {
            return DEFAULT_PROJ;
        }
        if (PROP_DESCR.equals(prop)) {
            return getDescriptor();
        }
        if (PROP_VERSION.equals(prop)) {
            return DEFAULT_VERSION;
        }
        if (PROP_COMPRESS.equals(prop)) {
            return "gzip";
        }
        if (PROP_PORT.equals(prop)) {
            return DEFAULT_PORT;
        }
        if (PROP_DEBUG.equals(prop)) {
//          return DEFAULT_DEBUG;
            Boolean.toString(EntryStore.isAddeDebugEnabled(false));
        }
        if (PROP_SIZE.equals(prop)) {
            if (ad != null) {
                return ad.getLines() + " " + ad.getElements();
            }
            return MAX_SIZE + " " + MAX_SIZE;
        }
        if (PROP_MAG.equals(prop)) {
            return "1 1";
        }
        // if (prop.equals(PROP_LOC) || prop.equals(PROP_LINELE)) {
        if (PROP_MAG.equals(prop)) {
            if (ad == null) {
                return "0 0";
            }
            return ad.getLines() / 2 + " " + ad.getElements() / 2;
        }
        // if (prop.equals(PROP_LATLON)) {
        if (PROP_LOC.equals(prop) || PROP_LATLON.equals(prop)) {
            if (ad == null) {
                return "0 0";
            }
            return ad.getCenterLatitude() + " " + ad.getCenterLongitude();
        }
        if (PROP_BAND.equals(prop)) {
            if (bandTable==null) return "";
            if (forDisplay) {
                return getBandName(ad, ((int[]) bandTable.get(ad))[0]);
            }
            return "" + ((int[]) bandTable.get(ad))[0];
        }
        if (PROP_SPAC.equals(prop)) {
            return getSelectedUnit().equalsIgnoreCase("BRIT") ? "1" : "4";
        }
        if (PROP_UNIT.equals(prop)) {
            return "X";
        }
        if (PROP_NAV.equals(prop)) {
            return "X";
        }
        return "";
    }

    /**
     * Set the properties on the AddeImageInfo from the list of properties
     * 
     * @param aii
     *            The AddeImageInfo
     * @param props
     *            list of props to set
     * @param ad
     *            The AreaDirectory
     */
    protected void setImageInfoProps(AddeImageInfo aii, String[] props, AreaDirectory ad) {
        for (int i = 0; i < props.length; i++) {
            String prop = props[i];
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
                } else if (value.equals("compress") || value.equals("2") || value.equals("true")) {
                    compVal = AddeURL.COMPRESS;
                }
                aii.setCompression(compVal);
            } else if (prop.equals(PROP_PORT)) {
                aii.setPort(Integer.parseInt(value));
            } else if (prop.equals(PROP_DEBUG)) {
//              aii.setDebug(Boolean.getBoolean(value));
                aii.setDebug(EntryStore.isAddeDebugEnabled(false));
            } else if (prop.equals(PROP_SPAC)) {
                aii.setSpacing(Integer.parseInt(value));
            } else if (prop.equals(PROP_UNIT)) {
                if (value.equals(ALLUNITS.getId())) {
                    value = getDefault(prop, getDefaultPropValue(prop, ad, false));
                }
                aii.setUnit(value);
            } else if (prop.equals(PROP_BAND)) {
                if (value.equals(ALLBANDS.toString())) {
                    value = getDefault(prop, getDefaultPropValue(prop, ad, false));
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
    protected void clearPropertiesWidgets() {
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

        if (placeLbl != null) {
            changePlace(PLACE_CENTER);
        }

        if (navComboBox != null) {
            checkSetNav();
//          navComboBox.setSelectedIndex(0);
        }
        baseNumLines = 0.0;
        baseNumElements = 0.0;
    }

    /**
     * Set the widgets with the state from the given AreaDirectory
     * 
     * @param ad
     *            AreaDirectory for the image
     */
    protected void setPropertiesState(AreaDirectory ad) {
        setPropertiesState(ad, false);
    }

    /**
     * _more_
     * 
     * @param ad
     *            _more_
     * 
     * @return _more_
     */
    protected int[] getSize(AreaDirectory ad) {
        baseNumLines = ad.getLines();
        baseNumElements = ad.getElements();

        String sizeDefault = getDefault(PROP_SIZE, (String) null);
        List toks = ((sizeDefault != null) ? StringUtil.split(sizeDefault, " ",
                true, true) : null);
        if ((toks == null) || (toks.size() == 0)) {
            return new int[] { (int) baseNumLines, (int) baseNumElements };
        } else {
            String lines = "" + toks.get(0);
            if (lines.equalsIgnoreCase(ALL)) {
                lines = "" + (int) baseNumLines;
            }
            int numLines = new Integer(lines.trim()).intValue();

            String elems = (toks.size() > 1) ? "" + toks.get(1) : ""
                    + (int) baseNumElements;
            if (elems.equalsIgnoreCase(ALL)) {
                elems = "" + baseNumElements;
            }
            int numElements = new Integer(elems.trim()).intValue();
            return new int[] { (int) Math.min(numLines, baseNumLines),
                    (int) Math.min(numElements, baseNumElements) };
        }

    }

    /**
     * Set the widgets with the state from the given AreaDirectory
     * 
     * @param ad
     *            AreaDirectory for the image
     * @param force
     *            force an update regardless of the previous invocation
     */
    protected void setPropertiesState(AreaDirectory ad, boolean force) {
        if (amSettingProperties)
            return;

        prevPropertiesAD = propertiesAD;
        propertiesAD = ad;
        if (!force && checkPropertiesEqual(prevPropertiesAD, propertiesAD))
            return;

        amSettingProperties = true;

        if (ad == null) {
            clearPropertiesWidgets();
            amSettingProperties = false;
            return;
        }

        String[] propArray = getAdvancedProps();

        if (numLinesFld != null) {
            int[] size = getSize(ad);
            numLinesFld.setText("" + size[0]);
            numElementsFld.setText("" + size[1]);
            if (sizeLbl != null) {
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

        List<BandInfo> bandList = null;
        int[] bands = (int[]) bandTable.get(ad);
        if (bands != null)
            bandList = makeBandInfos(ad, bands);
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

        setAvailableUnits(ad, getSelectedBand());

        for (int propIdx = 0; propIdx < propArray.length; propIdx++) {
            String prop = propArray[propIdx];
            String value = getDefault(prop, getDefaultPropValue(prop, ad, false));
            if (value == null)
                value = "";

            value = value.trim();
            if (prop.equals(PROP_LOC)) {
                String key = getDefault(PROP_KEY, PROP_LATLON);

                boolean usingLineElement = key.equals(PROP_LINELE);
                if (usingLineElement) {
                    locationPanel.show(1);
                } else {
                    locationPanel.show(0);
                }
                if (usingLineElement) {
                    value = getDefault(PROP_LOC, getDefaultPropValue(
                            PROP_LINELE, ad, false));
                } else {
                    value = getDefault(PROP_LOC, getDefaultPropValue(
                            PROP_LATLON, ad, false));
                }
                String[] pair = getPair(value);
                if (pair != null) {
                    if (usingLineElement) {
                        centerLineFld.setText(pair[0]);
                        centerElementFld.setText(pair[1]);
                    } else {
                        latLonWidget.setLat(pair[0]);
                        latLonWidget.setLon(pair[1]);
                    }
                }
            } else if (prop.equals(PROP_BAND)) {
                if (value.equalsIgnoreCase((String) ALLBANDS.getId())) {
                    bandComboBox.setSelectedItem(ALLBANDS);
                } else {
                    int bandNum = 0;
                    try {
                        bandNum = Integer.parseInt(value);
                    } catch (NumberFormatException nfe) {
                    }
                    int index = BandInfo.findIndexByNumber(bandNum, bandList);
                    if (index != -1) {
                        bandComboBox.setSelectedIndex(index);
                    }
                }
            } else if (prop.equals(PROP_PLACE)) {
                changePlace(value);
            } else if (prop.equals(PROP_MAG)) {
                String[] pair = getPair(value);
                if (pair != null) {
                    setMagSliders(new Integer(pair[0]).intValue(), new Integer(
                            pair[1]).intValue());
                } else {
                    setMagSliders(DEFAULT_MAG, DEFAULT_MAG);
                }
            } else if (prop.equals(PROP_NAV)) {
                if (navComboBox != null) {
                    navComboBox.setSelectedIndex((value
                            .equalsIgnoreCase("LALO") ? 1 : 0));
                }
                checkSetNav();
            }
        }
        amSettingProperties = false;

    }

    /**
     * Set the mag slider values
     * 
     * @param lineValue
     *            the line value
     * @param elementValue
     *            the element value
     */
    protected void setMagSliders(int lineValue, int elementValue) {
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
            elementMagLbl.setText(StringUtil.padLeft("" + getElementMagValue(),
                    4));
            linesToElements = Math.abs(lineValue / (double) elementValue);
            if (Double.isNaN(linesToElements)) {
                linesToElements = 1.0;
            }
        }
    }

    /**
     * Get the value of the given magnification slider.
     * 
     * @param slider
     *            The slider to get the value from
     * @return The magnification value
     */
    protected int getMagValue(JSlider slider) {
        // Value is [-SLIDER_MAX,SLIDER_MAX]. We change 0 and -1 to 1
        int value = slider.getValue();
        if (value >= 0) {
            return value + 1;
        }
        return value - 1;
    }

    /**
     * Get a pair of properties
     * 
     * @param v
     *            a space separated string
     * 
     * @return an array of the two strings
     */
    protected String[] getPair(String v) {
        if (v == null) {
            return null;
        }
        v = v.trim();
        List toks = StringUtil.split(v, " ", true, true);
        if ((toks == null) || (toks.size() == 0)) {
            return null;
        }
        String tok1 = toks.get(0).toString();
        return new String[] { tok1,
                ((toks.size() > 1) ? toks.get(1).toString() : tok1) };

    }

    /**
     * Get the selected band from the advanced chooser
     * 
     * @return selected band number
     */
    protected int getSelectedBand() {

        Object bi = (bandComboBox == null) ? null : bandComboBox
                .getSelectedItem();
        if ((bi == null) || bi.equals(ALLBANDS)) {
            return 0;
        }
        return ((BandInfo) bi).getBandNumber();
    }

    /**
     * Translate a place name into a human readable form
     * 
     * @param place
     *            raw name
     * 
     * @return human readable name
     */
    protected String translatePlace(String place) {
        place = place.toUpperCase();
        if (place.equals(PLACE_ULEFT)) {
            return "Upper left";
        }
        if (place.equals(PLACE_LLEFT)) {
            return "Lower left";
        }
        if (place.equals(PLACE_URIGHT)) {
            return "Upper right";
        }
        if (place.equals(PLACE_LRIGHT)) {
            return "Lower right";
        }
        if (place.equals(PLACE_CENTER)) {
            return "Center";
        }
        return place;
    }

    /**
     * Get the band name for a particular area
     * 
     * @param ad
     *            AreaDirectory
     * @param band
     *            band number
     * 
     * @return name of the band
     */
    protected String getBandName(AreaDirectory ad, int band) {
        // if (band== 0) return ALLBANDS.toString();

        if (useSatBandInfo) {
            if (satBandInfo == null) {
                return "Band: " + band;
            }
            String[] descrs = satBandInfo.getBandDescr(ad.getSensorID(), ad
                    .getSourceType());
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
        Hashtable bandToName = (Hashtable) sensorToBandToName.get(new Integer(
                ad.getSensorID()));
        String name = null;
        Integer bandInteger = new Integer(band);

        if (bandToName != null) {
            name = (String) bandToName.get(bandInteger);
        }
        if (name == null) {
            name = "Band: " + band;
        }
        /*
         * else { name = band + " - " + name.trim(); }
         */
        return name;
    }

    /**
     * Set the available units in the unit selector
     * 
     * @param ad
     *            AreaDirectory for the image
     * @param band
     *            band to use for units
     */
    protected void setAvailableUnits(AreaDirectory ad, int band) {
        List l = getAvailableUnits(ad, band);
        l.add(ALLUNITS);
        GuiUtils.setListData(unitComboBox, l);
        TwoFacedObject tfo = null;
        if ((bandComboBox != null) && (getSelectedBand() == 0)) {
            tfo = ALLUNITS;
        } else {
            String preferredUnit = getDefault(PROP_UNIT, "BRIT");
            tfo = TwoFacedObject.findId(preferredUnit, l);
        }
        if (tfo != null) {
            unitComboBox.setSelectedItem(tfo);
        }
    }

    /**
     * Set the available units in the unit selector
     * 
     * @param ad
     *            AreaDirectory for the image
     * @param band
     *            band to use for units
     * 
     * @return List of available units
     */
    protected List<TwoFacedObject> getAvailableUnits(AreaDirectory ad, int band) {
        // get Vector array of Calibration types. Layout is
        // v[i] = band[i] and for each band, it is a vector of
        // strings of calibration names and descriptions
        // n = name, n+1 = desc.
        // for radar, we only have one band
        if (ad == null) {
            return new ArrayList<TwoFacedObject>();
        }
        int[] bands = (int[]) bandTable.get(ad);
        int index = (bands == null) ? 0 : Arrays.binarySearch(bands, band);
        if (index < 0) index = 0;
        Vector<TwoFacedObject> l = new Vector<TwoFacedObject>();
        Vector v = ad.getCalInfo()[index];
        TwoFacedObject tfo = null;
        int preferredUnitIndex = 0;
        String preferredUnit = getDefault(PROP_UNIT, "BRIT");
        if ((v != null) && (v.size() / 2 > 0)) {
            for (int i = 0; i < v.size() / 2; i++) {
                String name = (String) v.get(2 * i);
                String desc = (String) v.get(2 * i + 1);
                desc = StringUtil.camelCase(desc);
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
    protected void readSatBands() {
        satBandInfo = null;
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
                satBandInfo = new AddeSatBands(StringUtil.listToStringArray(lines));
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
            if (!lines.get(i).toString().startsWith("Sat")) {
                continue;
            }
            List satIds = StringUtil.split(lines.get(i).toString(), " ", true,
                    true);
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
                    bandToName.put(Integer.decode(bandTok.trim()), line
                            .substring(idx).trim());
                } catch (NumberFormatException nfe) {
                }
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
     * @param dir
     *            AreaDirectory
     * @param isRelative
     *            true if is relative
     * @param num
     *            number (for relative images)
     * 
     * @return corresponding AddeImageInfo
     */
    protected AddeImageInfo makeImageInfo(AreaDirectory dir,
            boolean isRelative, int num) {
        AddeImageInfo info = new AddeImageInfo(getAddeServer().getName(),
                AddeImageInfo.REQ_IMAGEDATA, getGroup(), getDescriptor());
        if (isRelative) {
            info.setDatasetPosition((num == 0) ? 0 : -num);
        } else {
            info.setStartDate(dir.getNominalTime());
        }
        setImageInfoProps(info, getMiscKeyProps(), dir);
        setImageInfoProps(info, getBaseUrlProps(), dir);

        String locKey = getDefault(PROP_KEY, PROP_LINELE);
        String locValue = null;
        if (usePropFromUser(PROP_LOC)) {
            if (useLatLon()) {
                locKey = PROP_LATLON;
                locValue = getUserPropValue(PROP_LATLON, dir);
            } else {
                locKey = PROP_LINELE;
                locValue = getUserPropValue(PROP_LINELE, dir);
            }
        } else {
            locValue = getPropValue(PROP_LOC, dir);
        }
        info.setLocateKey(locKey);
        info.setLocateValue(locValue);

        String placeKey = getPropValue(PROP_PLACE, dir);
        info.setPlaceValue(placeKey);

        String magKey = getPropValue(PROP_MAG, dir);
        int lmag = 1;
        int emag = 1;
        StringTokenizer tok = new StringTokenizer(magKey);
        lmag = (int) Misc.parseNumber((String) tok.nextElement());
        if (tok.hasMoreTokens()) {
            emag = (int) Misc.parseNumber((String) tok.nextElement());
        } else {
            emag = lmag;
        }
        info.setLineMag(lmag);
        info.setElementMag(emag);

        int lines = dir.getLines();
        int elems = dir.getElements();
        String sizeKey = getPropValue(PROP_SIZE, dir);
        tok = new StringTokenizer(sizeKey);
        String size = (String) tok.nextElement();
        if (!size.equalsIgnoreCase("all")) {
            lines = (int) Misc.parseNumber(size);
            if (tok.hasMoreTokens()) {
                elems = (int) Misc.parseNumber((String) tok.nextElement());
            } else {
                elems = lines;
            }
        }
        info.setLines(lines);
        info.setElements(elems);
        /*
         * System.out.println("url = " + info.getURLString().toLowerCase() +
         * "\n");
         */
        return info;
    }

    /**
     * Check to see if the two Area directories are equal
     * 
     * @param ad1
     *            first AD (may be null)
     * @param ad2
     *            second AD (may be null)
     * 
     * @return true if they are equal
     */
    protected boolean checkPropertiesEqual(AreaDirectory ad1, AreaDirectory ad2) {
        if (ad1 == null) {
            return false;
        }
        if (ad2 == null) {
            return false;
        }
        return Misc.equals(ad1, ad2)
                || ((ad1.getLines() == ad2.getLines())
                        && (ad1.getElements() == ad2.getElements()) && Arrays
                        .equals(ad1.getBands(), ad2.getBands()));
    }

    /**
     * Get a description of the properties
     * 
     * @return a description
     */
    protected String getPropertiesDescription() {
        StringBuffer buf = new StringBuffer();
        String[] propArray = getAdvancedProps();
        List list = Misc.toList(propArray);
        if (list.contains(PROP_BAND)) {
            buf.append(getSelectedBandName());
            buf.append(", ");
        }
        if (list.contains(PROP_SIZE)) {
            buf.append("Size: ");
            String sizeKey = getUserPropValue(PROP_SIZE, propertiesAD);
            StringTokenizer tok = new StringTokenizer(sizeKey);
            if (tok.hasMoreTokens()) {
                String size = ((String) tok.nextElement()).trim();
                buf.append(size);
                buf.append("x");
                if (!size.equalsIgnoreCase("all")) {
                    if (tok.hasMoreTokens()) {
                        buf.append(((String) tok.nextElement()).trim());
                    } else {
                        buf.append(size);
                    }
                }
            }
        }
        return buf.toString();
    }

    /**
     * Show the given error to the user. If it was an ADDE exception that was
     * a bad server error then print out a nice message.
     * 
     * @param e Exception to be handled.
     */
    protected void handleConnectionError(Exception e) {
        if (e != null && e.getMessage() != null) {
            Throwable cause = e.getCause();
            String msg = cause.getMessage().toLowerCase();
            if ((cause instanceof AddeURLException) && msg.contains("must be used with archived datasets")) {
                getArchiveDay();
                return;
            }
        }
        super.handleConnectionError(e);
    }

    /**
     * Get the list of bands for the images
     * 
     * @param ad
     *            AreaDirectory
     * @param bands
     *            list of bands
     * @return list of BandInfos for the selected images
     */
    protected List<BandInfo> makeBandInfos(AreaDirectory ad, int[] bands) {
        // readSatBands();
        List<BandInfo> l = new ArrayList<BandInfo>();
        if (ad != null) {
            if (bands != null) {
                for (int i = 0; i < bands.length; i++) {
                    int band = bands[i];
                    if (band > 0) {
                        BandInfo bi = new BandInfo(ad.getSensorID(), band);
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
     * Get the pregenerated bandInfos
     */
    protected List<BandInfo> getBandInfos() {
        return bandInfos;
    }
    
    /**
     * Get the list of BandInfos for the current selected images
     * 
     * @return list of BandInfos
     */
    public List<BandInfo> getSelectedBandInfos() {
        // update the BandInfo list based on what has been chosen
        List selectedBandInfos = new ArrayList<BandInfo>();
        List selectedUnits = null;
        if (unitComboBox != null) {
            TwoFacedObject tfo = (TwoFacedObject) unitComboBox.getSelectedItem();
            if (!(tfo.equals(ALLUNITS))) { // specific unit requested
                selectedUnits = new ArrayList<TwoFacedObject>();
                selectedUnits.add(tfo);
            }
        }
        if (getSelectedBand() == 0) { // All bands selected
            if (selectedUnits != null) {
                for (Iterator iter = bandInfos.iterator(); iter.hasNext();) {
                    BandInfo newBI = new BandInfo((BandInfo) iter.next());
                    newBI.setCalibrationUnits(selectedUnits);
                    newBI.setPreferredUnit((String) ((TwoFacedObject) selectedUnits.get(0)).getId());
                    selectedBandInfos.add(newBI);
                }
            } else { // else All Bands, AllUnits
                selectedBandInfos = bandInfos;
            }
        } else { // not All selected;
            int index = BandInfo.findIndexByNumber(getSelectedBand(), bandInfos);
            BandInfo selectedBandInfo = null;
            if (index != -1) {
                selectedBandInfo = bandInfos.get(index);
            }
            if (selectedBandInfo != null) {
                if (selectedUnits != null) {
                    BandInfo newBI = new BandInfo(selectedBandInfo);
                    newBI.setCalibrationUnits(selectedUnits);
                    newBI.setPreferredUnit((String) ((TwoFacedObject) selectedUnits.get(0)).getId());
                    selectedBandInfos.add(newBI);
                } else {
                    selectedBandInfos.add(selectedBandInfo);
                }
            }
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
     * User said go, we go. Simply get the list of images from the imageChooser
     * and create the ADDE.IMAGE DataSource
     * 
     */
    public void doLoadInThread() {
        if (!checkForValidValues()) {
            return;
        }
        if (!getGoodToGo()) {
            updateStatus();
            return;
        }

        List imageList = getImageList();
        if ((imageList == null) || (imageList.size() == 0)) {
            return;
        }

        // Check for size threshold
        final int[] dim = { 0, 0 };
        AddeImageDescriptor aid = (AddeImageDescriptor) imageList.get(0);
        dim[0] = aid.getImageInfo().getElements();
        dim[1] = aid.getImageInfo().getLines();
        // System.err.println("dim:" + dim[0] + " x " + dim[1] + " # images:"
        // + imageList.size());
        int numPixels = dim[0] * dim[1] * imageList.size();
        double megs = (4 * numPixels) / (double) 1000000;
        
        //DAVEP: take this out--it should be warning in the data source, not the chooser
        boolean doSizeCheck = false;
        if (megs > SIZE_THRESHOLD && doSizeCheck) {
            final JCheckBox maintainSize = new JCheckBox(
                    "Maintain spatial extent", false);
            final JLabel sizeLbl = new JLabel(StringUtil.padRight("  "
                    + ((double) ((int) megs * 100)) / 100.0 + " MB", 14));
            GuiUtils.setFixedWidthFont(sizeLbl);
            final List[] listHolder = { imageList };
            final JSlider slider = new JSlider(2, (int) megs, (int) megs);
            slider.setMajorTickSpacing((int) (megs - 2) / 10);
            slider.setMinorTickSpacing((int) (megs - 2) / 10);
            // slider.setPaintTicks(true);
            slider.setSnapToTicks(true);
            final long timeNow = System.currentTimeMillis();
            ChangeListener sizeListener = new javax.swing.event.ChangeListener() {
                public void stateChanged(ChangeEvent evt) {
                    // A hack so we don't respond to the first event that we get
                    // from the slider when
                    // the dialog is first shown
                    if (System.currentTimeMillis() - timeNow < 500)
                        return;
                    JSlider slider = (JSlider) evt.getSource();
                    int pixelsPerImage = 1000000 * slider.getValue()
                            / listHolder[0].size() / 4;
                    double aspect = dim[1] / (double) dim[0];
                    int nx = (int) Math.sqrt(pixelsPerImage / aspect);
                    int ny = (int) (aspect * nx);
                    if (maintainSize.isSelected()) {
                        // doesn't work
                        lineMagSlider.setValue(getLineMagValue() - 1);
                        lineMagSliderChanged(true);
                    } else {
                        numElementsFld.setText("" + nx);
                        numLinesFld.setText("" + ny);
                    }
                    listHolder[0] = getImageList();
                    AddeImageDescriptor aid = (AddeImageDescriptor) listHolder[0]
                            .get(0);
                    dim[0] = aid.getImageInfo().getElements();
                    dim[1] = aid.getImageInfo().getLines();
                    int numPixels = dim[0] * dim[1] * listHolder[0].size();
                    double nmegs = (4 * numPixels) / (double) 1000000;
                    sizeLbl.setText(StringUtil.padRight("  "
                            + ((double) ((int) nmegs * 100)) / 100.0 + " MB",
                            14));
                }
            };
            slider.addChangeListener(sizeListener);

            JComponent msgContents = GuiUtils
                    .vbox(
                            new JLabel(
                                    "<html>You are about to load "
                                            + megs
                                            + " MB of imagery.<br>Are you sure you want to do this?<p><hr><p></html>"),
                            GuiUtils.inset(GuiUtils.leftCenterRight(new JLabel(
                                    "Change Size: "),
                                    GuiUtils.inset(slider, 5), sizeLbl), 5));

            if (!GuiUtils.askOkCancel("Image Size", msgContents)) {
                return;
            }
            imageList = listHolder[0];
        }

        ImageDataset ids = new ImageDataset(getDatasetName(), imageList);
        // make properties Hashtable to hand the station name
        // to the AddeImageDataSource
        Hashtable ht = new Hashtable();
        getDataSourceProperties(ht);
        Object bandName = getSelectedBandName();
        if (bandName != null && !(bandName.equals(ALLBANDS.toString()))) {
            ht.put(DATA_NAME_KEY, bandName);
        }
        
        makeDataSource(ids, getDataSourceId(), ht);
        saveServerState();
    }
    
    /**
     * Return the data source ID.  Used by extending classes.
     */
    protected String getDataSourceId() {
        return "ADDE.IMAGE";
    }

    /**
     * Get the DataSource properties
     * 
     * @param ht
     *            Hashtable of properties
     */
    protected void getDataSourceProperties(Hashtable ht) {
        super.getDataSourceProperties(ht);
        ht.put(DATASET_NAME_KEY, getDatasetName());
        ht.put(ImageDataSource.PROP_BANDINFO, getSelectedBandInfos());
    }

    /**
     * _more_
     * 
     * @return _more_
     */
    protected List processPropertyComponents() {
        List bottomComps = new ArrayList();
        getBottomComponents(bottomComps);
        for (int i = 0; i < bottomComps.size(); i++) {
            addDescComp((JComponent) bottomComps.get(i));
        }
        return bottomComps;
    }

    /**
     * Make the UI for this selector.
     * 
     * @return The gui
     */
    public JComponent doMakeContents() {
        JPanel myPanel = new JPanel();

        JLabel timesLabel = McVGuiUtils.makeLabelRight("Times:");
        addDescComp(timesLabel);

        JPanel timesPanel = makeTimesPanel();
        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addDescComp(timesPanel);

        JLabel imageLabel = McVGuiUtils.makeLabelRight("Other:");
        addDescComp(imageLabel);

        List comps = new ArrayList();
        comps.addAll(processPropertyComponents());
        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel imagePanel = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
        imagePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(descriptorLabel)
                    .addGap(GAP_RELATED)
                    .addComponent(descriptorComboBox))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(timesLabel)
                    .addGap(GAP_RELATED)
                    .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(imageLabel)
                    .addGap(GAP_RELATED)
                    .addComponent(imagePanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)))));

        layout.setVerticalGroup(layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(BASELINE)
                .addComponent(descriptorLabel)
                .addComponent(descriptorComboBox))
            .addPreferredGap(RELATED)
            .addGroup(layout.createParallelGroup(LEADING)
                .addComponent(timesLabel)
                .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(imageLabel)
                    .addComponent(imagePanel))));

        setInnerPanel(myPanel);
        return super.doMakeContents();
    }

    public JComponent doMakeContents(boolean doesOverride) {
        if (doesOverride)
            return super.doMakeContents();
        else
            return doMakeContents();
    }

}
