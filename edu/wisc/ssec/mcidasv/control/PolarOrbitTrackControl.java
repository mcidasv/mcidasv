/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
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

package edu.wisc.ssec.mcidasv.control;

import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static ucar.unidata.util.LayoutUtil.centerBottom;
import static ucar.unidata.util.LayoutUtil.inset;
import static visad.RealTupleType.LatitudeLongitudeTuple;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIdasPreferenceManager;
import edu.wisc.ssec.mcidasv.data.GroundStation;
import edu.wisc.ssec.mcidasv.data.GroundStations;
import edu.wisc.ssec.mcidasv.data.PolarOrbitTrackDataSource;
import edu.wisc.ssec.mcidasv.data.TimeRangeSelection;
import edu.wisc.ssec.mcidasv.data.hydra.CurveDrawer;
import edu.wisc.ssec.mcidasv.ui.ColorSwatchComponent;
import edu.wisc.ssec.mcidasv.util.XmlUtil;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import name.gano.astro.AstroConst;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.control.DisplayControlBase;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.FontSelector;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Msg;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.visad.UtcDate;
import ucar.visad.Util;
import ucar.visad.display.CompositeDisplayable;
import ucar.visad.display.TextDisplayable;
import visad.Data;
import visad.DisplayRealType;
import visad.Gridded2DSet;
import visad.MathType;
import visad.RealTuple;
import visad.RealTupleType;
import visad.SampledSet;
import visad.Text;
import visad.TextControl;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.UnionSet;
import visad.VisADException;
import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonTuple;

/**
 * {@link DisplayControlImpl} with some McIDAS-V specific extensions.
 *
 * <p>Namely parameter sets and support for inverted parameter defaults.</p>
 */

public class PolarOrbitTrackControl extends DisplayControlImpl {
    
    private static final Logger logger =
        LoggerFactory.getLogger(PolarOrbitTrackControl.class);
    
    private static final String ERR_DIALOG_TITLE = "Time Range Selection Error";

    private static final Pattern REGEX = Pattern.compile(" ");
    
    private JLabel satelliteName = new JLabel("");
    private static final JLabel kmLabel = new JLabel("km");
    private JTextField swathWidthFld = null;
    private JPanel swathWidthPanel;
    
    private double latitude;
    private double longitude;
    private JPanel fontSizePanel;
    private JPanel colorPanel;
    private JPanel antColorPanel;
    private JPanel locationPanel;
    private JPanel latLonAltPanel;
    
    /** Property name to get the list or URLs */
    public final String PREF_GROUNDSTATIONS = "mcv.groundstations";
    
    private JComboBox<GroundStation> locationComboBox;
    private JComboBox<GroundStation> jcbStationsPlotted;
    
    private final List<GroundStation> stations = new ArrayList<>();
    
    private JComboBox<String> jcbTrackLineStyle =
        new JComboBox<>(Constants.lineStyles);
    
    private JComboBox<String> jcbEdgeLineStyle =
        new JComboBox<>(Constants.lineStyles);
    
    private JComboBox<String> jcbStationLineStyle =
        new JComboBox<>(Constants.lineStyles);
    
    private JCheckBox jcbLabels;
    private JCheckBox jcbSwathEdges;
    private boolean swathEdgesOn = false;
    private String [] lineWidths = {"1", "2", "3", "4"};
    
    private JComboBox<String> jcbStationLineWidth =
        new JComboBox<>(lineWidths);
    
    private JComboBox<String> jcbSwathCenterLineWidth =
        new JComboBox<>(lineWidths);
    
    // names to distinguish checkbox event sources
    private static final String CHECKBOX_LABELS = "CHECKBOX_LABELS";
    private static final String CHECKBOX_SWATH_EDGES = "CHECKBOX_SWATH_EDGES";
    
    private String station = "";
    
    private static final int SWATH_WIDTH_MIN = 0;
    // swath width not applicable, e.g. GEO sensor
    private static final String SWATH_NA = "N/A";
    // TJJ Feb 2014 - need to determine max of any sensor. VIIRS is over 3000 km
    private static final int SWATH_WIDTH_MAX = 4000;
    
    private static final int MAX_ANTENNA_ANGLE = 90;
    private int curAngle = GroundStation.DEFAULT_ANTENNA_ANGLE;
    private int curElevation = 0;
    private static final double LABEL_DISTANCE_THRESHOLD = 2.5d;
    
    // Valid range for custom ground station elevation
    private static final int MIN_ELEVATION = -500;
    private static final int MAX_ELEVATION = 8850;

    private DataChoice dataChoice;
    
    private JLabel latLabel;
    private JLabel lonLabel;
    private JLabel altLabel;
    private JTextField antennaAngle = new JTextField("" +
        GroundStation.DEFAULT_ANTENNA_ANGLE, GroundStation.DEFAULT_ANTENNA_ANGLE);
    
    // custom ground station UI components
    JTextField customLat = null;
    JTextField customLon = null;
    JTextField customLab = null;
    JTextField customAlt = null;
    
    /** the font selectors, Orbit Track (ot) and Ground Station (gs) */
    private FontSelector otFontSelector;
    private Font otCurFont = null;
    private int otCurFontSize = -1;
    private FontSelector gsFontSelector;
    
    // line width combo boxes, Station: Ground Station, SC: Swath Center, SE: Swath Edge
    private JComboBox<String> jcbSwathEdgeLineWidth =
        new JComboBox<>(lineWidths);
    
    private JSpinner js = null;
    
    private CompositeDisplayable trackDsp;
    private CompositeDisplayable timeLabelDsp;
    private CompositeDisplayable swathEdgeDsp;
    
    // time label variables
    private static final int DEFAULT_LABEL_INTERVAL = 5;
    private int curLabelInterval = DEFAULT_LABEL_INTERVAL;
    private int prvLabelInterval = DEFAULT_LABEL_INTERVAL;
    
    private ColorSwatchComponent colorSwatch;
    
    private static final Color DEFAULT_COLOR = Color.GREEN;
    private Color curSwathColor = null;
    private Color prvSwathColor = DEFAULT_COLOR;
    
    private ColorSwatchComponent antColorSwatch;
    private Color antColor;
    private Color defaultAntColor = Color.MAGENTA;
    private PolarOrbitTrackDataSource dataSource;
    
    private double trackZ = 0.0d;
    private double gsZ = 0.0d;
    private NavigatedDisplay navDsp = null;
    private TextType otTextType = null;
    private static long ttCounter = 0;
    private double curWidth = 0.0d;
    private double prvWidth = 0.0d;
    
    private int prvTrackLineStyle = 0;
    private int prvEdgeLineStyle = 1;
    private int curTrackLineStyle = 0;
    private int curEdgeLineStyle = 1;
    private static final float FONT_SCALE_FACTOR = 12.0f;
    
    // line width for drawing track center and swath edges
    private int prvSwathCenterWidth = 2;
    private int curSwathCenterWidth = 2;
    private int prvSwathEdgeWidth = 1;
    private int curSwathEdgeWidth = 1;
    
    /** Path to the McV swathwidths.xml */
    private static final String SWATH_WIDTHS = "/edu/wisc/ssec/mcidasv/resources/swathwidths.xml";
    private static final String TAG_SATELLITE = "satellite";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_WIDTH = "width";
    
    private static final String SWATH_MODS = "OrbitTrack";
    private static final String STATION_MODS = "GroundStation";
    private static final String STATION_ADD = "AddStation";
    private static final String STATION_REM = "RemStation";
    private static final String CUSTOM_ADD = "AddCustom";
    private static final String ACTIVE_STATION = "ActiveStation";
    
    // Constants for the various UI tool-tips
    private static final String TOOLTIP_ADD_CUSTOM =
       "Station will be plotted with Color, Font, and Line Width/Style options currently selected";
    private static final String TOOLTIP_ADD_SELECTED =
       "Station will be plotted with Color, Font, and Line Width/Style options currently selected";
    private static final String TOOLTIP_ANTENNA_ANGLE =
       "Antenna elevation angle, valid range 5 to 90 degrees";
    private static final String TOOLTIP_CUSTOM_ALT =
       "Antenna altitude valid range: " + MIN_ELEVATION + " m to: " + MAX_ELEVATION + " m";
    private static final String TOOLTIP_CUSTOM_LABEL =
       "Choose a label, e.g. \"Mesa, AZ\"";
    private static final String TOOLTIP_CUSTOM_LAT =
       "Latitude of your custom groundstation";
    private static final String TOOLTIP_CUSTOM_LON =
       "Longitude of your custom groundstation";
    private static final String TOOLTIP_SWATH_WIDTH =
       "Valid range: > " + SWATH_WIDTH_MIN + " km to: " + SWATH_WIDTH_MAX + " km";
    private static final String TOOLTIP_LABEL_INTERVAL =
       "Interval in minutes between orbit track time labels";

    private final Map<GroundStation, TextDisplayable> stationToText =
        new HashMap<>();
    
    private final Map<GroundStation, CurveDrawer> stationToCurve =
        new HashMap<>();
    
    // Used in showProperties override to help decide if we need to redraw
    Hashtable<String, Object> oldProps = null;
    
    private Element root = null;
    
    private boolean showingLabels;
    
    // initial scale for labeling 
    float scale = 1.0f;
    
    public PolarOrbitTrackControl() {
        logger.trace("created new PolarOrbitTrackControl...");
        setHelpUrl("idv.control.orbittrackcontrol");
        try {
            final String xml =
                IOUtil.readContents(SWATH_WIDTHS, McIdasPreferenceManager.class);
            root = XmlUtil.getRoot(xml);
            if (curSwathColor == null) curSwathColor = DEFAULT_COLOR;
        } catch (Exception e) {
            logger.error("problem reading swathwidths.xml", e);
        }
    }

    /**
     * Get the DisplayListTemplate property. This method is a fair bit different from its parent,
     * in order to allow overrides to utilize the default display list template method.
     * TJJ Jun 2023 - See https://mcidas.ssec.wisc.edu/inquiry-v/?inquiry=2772
     *
     * @return The DisplayListTemplate
     */
    public String getDisplayListTemplate() {
        if (displayListTemplate == null) {
            String pref = PREF_DISPLAYLIST_TEMPLATE + '.' + displayId;
            boolean haveData = (getShortParamName() != null);
            pref = pref + (haveData ? ".data" : ".nodata");
            displayListTemplate = getStore().get(pref, getDefaultDisplayListTemplate());
        }
        return displayListTemplate;
    }

    /**
     * Override because the base class template results in a very long layer label.
     * TJJ Jun 2023 - See https://mcidas.ssec.wisc.edu/inquiry-v/?inquiry=2772
     *
     * @return The DefaultDisplayListTemplate
     */
    @Override protected String getDefaultDisplayListTemplate() {
        return (getShortParamName() != null)  // haveData
                ? MACRO_DISPLAYNAME + " - " + MACRO_TIMESTAMP : MACRO_DISPLAYNAME;
    }

    /**
     * Deal with action events
     *
     * @param  ae the ActionEvent fired when the user applies changes
     */
    
    public void actionPerformed(ActionEvent ae) {
        
        // user trying to add a custom ground station
        if (CUSTOM_ADD.equals(ae.getActionCommand())) {

            logger.debug("Custom Ground Station...");
            String labStr = customLab.getText();
            if ((labStr == null) || (labStr.isEmpty())) {
                JOptionPane.showMessageDialog(null,
                    "Please provide a label for the custom ground station.");
                return;
            }
            float fLat;
            float fLon;
            try {
                fLat = Float.parseFloat(customLat.getText());
                fLon = Float.parseFloat(customLon.getText());
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(null,
                    "Latitude and Longitude must be floating point numbers, please correct.");
                return;
            }
            if ((fLat < -90) || (fLat > 90)) {
                JOptionPane.showMessageDialog(null,
                    "Latitude is out of valid range: " + fLat);
                return;
            }
            if ((fLon < -180) || (fLon > 180)) {
                JOptionPane.showMessageDialog(null,
                    "Longitude is out of valid range: " + fLon);
                return;
            }

            // Validate the elevation
            String s = customAlt.getText();
            try {
                int newElevation = Integer.parseInt(s);
                if (newElevation != curElevation) {
                    // Always need to do range check too
                    if ((newElevation < MIN_ELEVATION) ||
                        (newElevation > MAX_ELEVATION)) {
                        throw new NumberFormatException();
                    }
                    curElevation = newElevation;
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(null,
                    TOOLTIP_CUSTOM_ALT);
                return;
            }

            // Validate the antenna angle
            s = antennaAngle.getText();
            try {
                int newAngle = Integer.parseInt(s);
                if (newAngle != curAngle) {
                    // Always need to do range check too
                    if ((newAngle < GroundStation.DEFAULT_ANTENNA_ANGLE) ||
                        (newAngle > MAX_ANTENNA_ANGLE)) {
                        throw new NumberFormatException();
                    }
                    curAngle = newAngle;
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(null,
                    "Invalid antenna angle: " + s);
                return;
            }

            // last check, is this label already used?
            int numPlotted = jcbStationsPlotted.getItemCount();
            for (int i = 0; i < numPlotted; i++) {
                GroundStation gs = jcbStationsPlotted.getItemAt(i);
                if ((gs.getName() != null) && gs.getName().equals(station)) {
                    JOptionPane.showMessageDialog(null,
                        "A station with this label has already been plotted: " + station);
                    return;
                }
            }
            
            // if we made it this far, fields are valid, we can create a custom ground station
            // create new earth location, add it to stations plotted, set index, 

            // update scale in case user changed zoom level
            scale = getViewManager().getMaster().getDisplayScale();
            
            // make an Earth location
            EarthLocationTuple elt = null;
            try {
                elt = new EarthLocationTuple(fLat, fLon, curElevation);
            } catch (VisADException | RemoteException e) {
                logger.error("Problem creating EarthLocationTuple", e);
            }
            
            double satelliteAltitude = dataSource.getNearestAltToGroundStation(latitude, longitude) / 1000.0;
            GroundStation gs = new GroundStation(labStr, elt, curAngle, satelliteAltitude);
            addGroundStation(gs, true);
            jcbStationsPlotted.addItem(gs);
            jcbStationsPlotted.setSelectedItem(gs);
            updateDisplayList();
            return;
        }
        
        // user trying to add a new ground station to those plotted on display
        if (STATION_ADD.equals(ae.getActionCommand())) {
            logger.debug("Add Station...");
            GroundStation addedStation =
                (GroundStation) locationComboBox.getSelectedItem();
            
            boolean alreadyPlotted = false;
            int numPlotted = jcbStationsPlotted.getItemCount();
            for (int i = 0; i < numPlotted; i++) {
                GroundStation gs = jcbStationsPlotted.getItemAt(i);
                if (Objects.equals(gs.getName(), addedStation.getName())) {
                    alreadyPlotted = true;
                    break;
                }
            }
            if (alreadyPlotted) {
                JOptionPane.showMessageDialog(null,
                    "Station already plotted on display: " + addedStation);
                return;
            } else {
                // Validate the antenna angle - only piece that can have errors for "stock" stations
                String s = antennaAngle.getText();
                try {
                    int newAngle = Integer.parseInt(s);
                    if (newAngle != curAngle) {
                        // Always need to do range check too
                        if ((newAngle < GroundStation.DEFAULT_ANTENNA_ANGLE) ||
                            (newAngle > MAX_ANTENNA_ANGLE)) {
                            throw new NumberFormatException();
                        }
                        curAngle = newAngle;
                        addedStation.setAntennaAngle(curAngle);
                    }
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(null,
                        "Invalid antenna angle: " + s);
                    return;
                }
                addGroundStation(addedStation, false);
                jcbStationsPlotted.addItem(addedStation);
                jcbStationsPlotted.setSelectedItem(addedStation);
                
            }
            updateDisplayList();
            return;
        }
        
        // Active station changed (selection from plotted stations)
        if (ACTIVE_STATION.equals(ae.getActionCommand())) {
            logger.debug("Active Station changed...");
            GroundStation gs = (GroundStation) jcbStationsPlotted.getSelectedItem();
            if (gs == null) {
                JOptionPane.showMessageDialog(null,
                    "No Active Stations");
            } else {
                // Update UI with settings for this station
                updateGroundStationWidgets(gs);
            }
            return;
        }
        
        // user removing a ground station from the display
        if (STATION_REM.equals(ae.getActionCommand())) {
            logger.debug("Rem Station...");
            GroundStation gs = (GroundStation) jcbStationsPlotted.getSelectedItem();
            if (gs == null) {
                JOptionPane.showMessageDialog(null,
                    "Nothing to remove");
            } else {
                try {
                    removeDisplayable(stationToCurve.get(gs));
                    removeDisplayable(stationToText.get(gs));
                } catch (RemoteException | VisADException e) {
                    logger.error("Problem removing displayables", e);
                }
                
                jcbStationsPlotted.removeItem(gs);
                // Did we remove the last active station?
                if (jcbStationsPlotted.getItemCount() == 0) {
                    latLabel.setText(" - ");
                    lonLabel.setText(" - ");
                    altLabel.setText(" - ");
                }
                
            }
            updateDisplayList();
            return;
        }
        
        // swath-related changes
        if (SWATH_MODS.equals(ae.getActionCommand())) {
            logger.debug("Apply Swath Mods...");
            
            boolean fontChanged = false;
            boolean swathChanged = false;
            scale = getViewManager().getMaster().getDisplayScale();
            
            curSwathCenterWidth = jcbSwathCenterLineWidth.getSelectedIndex() + 1;
            if (curSwathCenterWidth != prvSwathCenterWidth) {
                prvSwathCenterWidth = curSwathCenterWidth;
                swathChanged = true;
            }
            
            curSwathEdgeWidth = jcbSwathEdgeLineWidth.getSelectedIndex() + 1;
            if (curSwathEdgeWidth != prvSwathEdgeWidth) {
                prvSwathEdgeWidth = curSwathEdgeWidth;
                swathChanged = true;
            }
            
            curTrackLineStyle = jcbTrackLineStyle.getSelectedIndex();
            if (curTrackLineStyle != prvTrackLineStyle) {
                prvTrackLineStyle = curTrackLineStyle;
                swathChanged = true;
            }
            
            curEdgeLineStyle = jcbEdgeLineStyle.getSelectedIndex();
            if (curEdgeLineStyle != prvEdgeLineStyle) {
                prvEdgeLineStyle = curEdgeLineStyle;
                swathChanged = true;
            }
            
            curSwathColor = colorSwatch.getColor();
            if (! curSwathColor.equals(prvSwathColor)) {
                prvSwathColor = curSwathColor;
                swathChanged = true;
            }
            
            float newSwathWidth = validateSwathWidthField();
            if (newSwathWidth > 0) {
                curWidth = newSwathWidth;
                if (Double.compare(curWidth, prvWidth) != 0) {
                    prvWidth = curWidth;
                    swathChanged = true;
                }
            } else {
                // Don't apply anything if there are "errors on the form"
                if (newSwathWidth == -1) return;
            }
            
            // update font attributes if necessary
            Font f = otFontSelector.getFont();
            if (! f.equals(otCurFont)) {
                otCurFont = f;
                fontChanged = true;
            }
            if (f.getSize() != otCurFontSize) {
                otCurFontSize = f.getSize();
                fontChanged = true;
            }
            
            // see if label interval has changed
            SpinnerNumberModel snm = (SpinnerNumberModel) (js.getModel());
            prvLabelInterval = ((Integer) snm.getValue()).intValue();
            if ((prvLabelInterval != curLabelInterval) || fontChanged) {
                curLabelInterval = prvLabelInterval;
                swathChanged = true;
            }
            
            // check swath width field, update if necessary
            if (swathChanged || fontChanged) redrawAll();
            updateDisplayList();
            return;
        }
        
        // Ground station mods
        if (STATION_MODS.equals(ae.getActionCommand())) {
            
            GroundStation gs = (GroundStation) jcbStationsPlotted.getSelectedItem();
            if (gs == null) {
                // No stations plotted, nothing to do
                JOptionPane.showMessageDialog(null,
                    "No stations plotted, nothing to apply.");
                return;
            }
            
            logger.debug("Apply Station mods for: {}, cur font name: {}, cur font size: {}, cur color: {}",
                gs.getName(),
                gs.getFont().getFontName(),
                gs.getFont().getSize(),
                gs.getColor());
            
            // flag indicates user changed some parameter
            boolean somethingChanged = false;
            
            // Check each parameter of the ground station selected with UI settings
            
            // Color
            if (stationToText.get(gs).getColor() != antColorSwatch.getColor()) {
                logger.debug("GroundStation color change...");
                try {
                    updateStationColor(gs, antColorSwatch.getColor());
                } catch (RemoteException | VisADException e) {
                    logger.error("Problem changing ground station color", e);
                }
                somethingChanged = true;
            }
            
            // Font
            if (stationToText.get(gs).getFont() != gsFontSelector.getFont()) {
                logger.debug("GroundStation font change...");
                try {
                    updateStationFont(gs, gsFontSelector.getFont());
                } catch (RemoteException | VisADException e) {
                    logger.error("Problem changing ground station font", e);
                }
                somethingChanged = true;
            }
            
            // Antenna angle
            // If this changes, need to create a new CurveDrawer object
            
            String s = antennaAngle.getText();
            curAngle = gs.getAntennaAngle();
            try {
                int newAngle = Integer.parseInt(s);
                if (newAngle != curAngle) {
                    // TJJ Jun 2015 range check
                    if ((newAngle < GroundStation.DEFAULT_ANTENNA_ANGLE) ||
                        (newAngle > MAX_ANTENNA_ANGLE)) {
                        throw new NumberFormatException();
                    } else {
                        logger.debug("GroundStation antenna angle change...");
                        try {
                            removeDisplayable(stationToCurve.get(gs));
                            removeDisplayable(stationToText.get(gs));
                        } catch (RemoteException | VisADException e) {
                            logger.error("Problem removing displayable", e);
                        }
                        gs.setAntennaAngle(newAngle);
                        gs.setColor(antColorSwatch.getColor());
                        CurveDrawer cdNew = makeCoverageCircle(gs);
                        addDisplayable(cdNew);
                        addDisplayable(stationToText.get(gs));
                        stationToCurve.put(gs, cdNew);
                        curAngle = newAngle;
                        somethingChanged = true;
                    }
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(latLonAltPanel,
                    "Antenna angle valid range is " + GroundStation.DEFAULT_ANTENNA_ANGLE +
                        " to " + MAX_ANTENNA_ANGLE + " degrees");
                return;
            }
            
            // Line style and width
            
            CurveDrawer cd = stationToCurve.get(gs);
            int cdWidth = (int) cd.getLineWidth();
            int curStyle = cd.getLineStyle();
            
            if (cdWidth != (jcbStationLineWidth.getSelectedIndex() + 1)) {
                try {
                    logger.debug("GroundStation line width change...");
                    gs.setColor(antColorSwatch.getColor());
                    replaceCurve(gs);
                } catch (RemoteException | VisADException e) {
                    logger.error("Problem changing ground station line width", e);
                }
                somethingChanged = true;
            }
            
            if (curStyle != (jcbStationLineStyle.getSelectedIndex())) {
                try {
                    logger.debug("GroundStation line style change...");
                    gs.setColor(antColorSwatch.getColor());
                    replaceCurve(gs);
                } catch (RemoteException | VisADException e) {
                    logger.error("Problem changing ground station line style", e);
                }
                somethingChanged = true;
            }
            
            if (somethingChanged) {
                updateDisplayList();
            }
            return;
        }
    }
    
    /**
     * Apply the map (height) position to the displays
     */
    
    private void applyDisplayableLevels() {
        try {
            DisplayRealType dispType = navDsp.getDisplayAltitudeType();
            trackDsp.setConstantPosition(trackZ, dispType);
            timeLabelDsp.setConstantPosition(trackZ, dispType);
            swathEdgeDsp.setConstantPosition(trackZ, dispType);
            applyProperties();
        } catch (Exception e) {
            logger.error("Problem applying displayable levels", e);
        }
    }
    
    // No explicit dimension changes, but for times we need to redraw
    // everything due to combinations of zooming and visibility toggling
    private void redrawAll() {
        logger.debug("redrawAll() in...");
        try {
            removeDisplayable(swathEdgeDsp);
            removeDisplayable(trackDsp);
            removeDisplayable(timeLabelDsp);
            swathEdgeDsp = null;
            trackDsp = null;
            timeLabelDsp = null;
            Data data = getData(getDataInstance());
            swathEdgeDsp = new CompositeDisplayable();
            trackDsp = new CompositeDisplayable();
            timeLabelDsp = new CompositeDisplayable();
            // turn visibility off for those elements which have checkboxes for this
            if (! jcbSwathEdges.isSelected()) swathEdgeDsp.setVisible(false);
            if (! jcbLabels.isSelected()) timeLabelDsp.setVisible(false);
            createTrackDisplay(data, true);
            applyDisplayableLevels();
        } catch (Exception e) {
            logger.error("Problem redrawing", e);
        }
    }
    
    private void createTrackDisplay(Data data, boolean doTrack) {
        
        logger.debug("createTrackDisplay() in...");
        // Always check for View scale change (user zoomed in or out)
        scale = getViewManager().getMaster().getDisplayScale();
        try {
            List<String> dts = new ArrayList<>();
            if (data instanceof Tuple) {
                Data[] dataArr = ((Tuple) data).getComponents();
                
                int npts = dataArr.length;
                float[][] latlon = new float[2][npts];
                double distance = 0.0d;
                LatLonTuple prvPoint = null;
                
                for (int i = 0; i < npts; i++) {
                    Tuple t = (Tuple) dataArr[i];
                    Data[] tupleComps = t.getComponents();
                    
                    LatLonTuple llt = (LatLonTuple) tupleComps[1];
                    double dlat = llt.getLatitude().getValue();
                    double dlon = llt.getLongitude().getValue();
                    
                    if (doTrack) {
                        if ((i % curLabelInterval) == 0) {
                            
                            if (prvPoint != null) {
                                distance = Util.distance(prvPoint, llt);
                                if (distance < LABEL_DISTANCE_THRESHOLD) {
                                    latlon[0][i] = (float) dlat;
                                    latlon[1][i] = (float) dlon;
                                    continue;
                                }
                            }
                            
                            String str = ((Text) tupleComps[0]).getValue();
                            dts.add(str);
                            int indx = str.indexOf(' ') + 1;
                            String subStr = "- " + str.substring(indx, indx + 5);
                            TextDisplayable time = new TextDisplayable(SWATH_MODS, otTextType);
                            time.setJustification(TextControl.Justification.LEFT);
                            time.setVerticalJustification(TextControl.Justification.CENTER);
                            time.setColor(curSwathColor);
                            time.setTextSize((float) scale * otFontSelector.getFontSize() / FONT_SCALE_FACTOR);
                            time.setFont(otFontSelector.getFont());
                            time.setSphere(inGlobeDisplay());
                            time.setUseFastRendering(false);
                            
                            RealTuple lonLat =
                                new RealTuple(RealTupleType.SpatialEarth2DTuple,
                                    new double[] { dlon, dlat });
                            Tuple tup = new Tuple(makeTupleType(otTextType),
                                new Data[] { lonLat, new Text(otTextType, subStr)});
                            time.setData(tup);
                            if (jcbLabels.isSelected()) timeLabelDsp.addDisplayable(time);
                            
                            prvPoint = llt;
                        }
                    }
                    latlon[0][i] = (float) dlat;
                    latlon[1][i] = (float) dlon;
                }
                
                if (doTrack) {
                    drawSwathLine(latlon,
                                  npts,
                                  jcbTrackLineStyle.getSelectedIndex(),
                                  trackDsp,
                                  curSwathColor,
                                  curSwathCenterWidth);
                    
                    addDisplayable(trackDsp);
                    addDisplayable(timeLabelDsp);
                }
                
                // We initialize swath edge objects whenever possible, we just show or
                // hide them based on checkbox state
                if (curWidth > 0) {
                    float[][][] crv = getSwath(latlon);
                    int npt = crv[0][0].length;
                    float[][] leftC = new float[2][npt];
                    float[][] rightC = new float[2][npt];
                    for (int i = 0; i < npt; i++) {
                        leftC[0][i] = crv[0][0][i];
                        leftC[1][i] = crv[0][1][i];
                        rightC[0][i] = crv[1][0][i];
                        rightC[1][i] = crv[1][1][i];
                    }
                    
                    drawSwathLine(leftC,
                                  npt,
                                  jcbEdgeLineStyle.getSelectedIndex(),
                                  swathEdgeDsp,
                                  curSwathColor,
                                  curSwathEdgeWidth);
                    
                    drawSwathLine(rightC,
                                  npt,
                                  jcbEdgeLineStyle.getSelectedIndex(),
                                  swathEdgeDsp,
                                  curSwathColor,
                                  curSwathEdgeWidth);
                    
                    if (! jcbSwathEdges.isSelected()) {
                        swathEdgeDsp.setVisible(false);
                    }
                    addDisplayable(swathEdgeDsp);
                }
            }
        } catch (Exception e) {
            logger.error("Problem creating track display", e);
        }
    }
    
    private static void drawSwathLine(float[][] points,
                                      int pointCount,
                                      int lineStyle,
                                      CompositeDisplayable displayable,
                                      Color color,
                                      int width)
        throws VisADException, RemoteException
    {
        Gridded2DSet g2dset = new Gridded2DSet(LatitudeLongitudeTuple,
                                               points,
                                               pointCount);
        SampledSet[] sampledSets = new SampledSet[] { g2dset };
        UnionSet unionSet = new UnionSet(sampledSets);
        CurveDrawer lines = new CurveDrawer(unionSet);
        lines.setLineStyle(lineStyle);
        lines.setData(unionSet);
        lines.setDrawingEnabled(false);
        displayable.addDisplayable(lines);
        displayable.setColor(color);
        displayable.setLineWidth(width);
    }
    
    /* (non-Javadoc)
     * @see ucar.unidata.idv.control.DisplayControlImpl#displayableToFront()
     */
    @Override
    public void displayableToFront() {
        redrawAll();
    }
    
    /**
     * Called by doMakeWindow in DisplayControlImpl, which then calls its
     * doMakeMainButtonPanel(), which makes more buttons.
     *
     * @return container of contents
     */
    
    public Container doMakeContents() {
        
        fontSizePanel = new JPanel();
        fontSizePanel.setLayout(new BoxLayout(fontSizePanel, BoxLayout.Y_AXIS));
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        labelPanel.add(jcbLabels);
        
        // same row, add label interval spinner
        Integer defaultInterval = 5;
        Integer minInterval = 1;
        Integer maxInterval = 120;
        Integer intervalStep = 1;
        SpinnerNumberModel snm =
            new SpinnerNumberModel(defaultInterval, minInterval, maxInterval, intervalStep);
        JLabel intervalLabel = new JLabel("Label Interval:");
        JLabel intervalUnits = new JLabel("minutes");
        js = new JSpinner(snm);
        js.setToolTipText(TOOLTIP_LABEL_INTERVAL);
        labelPanel.add(Box.createHorizontalStrut(5));
        labelPanel.add(intervalLabel);
        labelPanel.add(js);
        labelPanel.add(intervalUnits);
        
        // line style for drawing swath track and width edges
        jcbTrackLineStyle.addActionListener(this);
        // will init to default of solid
        jcbTrackLineStyle.setSelectedIndex(curTrackLineStyle);
        
        // Swath center line width
        jcbSwathCenterLineWidth.addActionListener(this);
        jcbSwathCenterLineWidth.setSelectedIndex(curSwathCenterWidth - 1);
        
        // Swath edge line width
        jcbSwathEdgeLineWidth.addActionListener(this);
        jcbSwathEdgeLineWidth.setSelectedIndex(curSwathEdgeWidth - 1);
        
        jcbEdgeLineStyle.addActionListener(this);
        // will init to default of dashed
        jcbEdgeLineStyle.setSelectedIndex(curEdgeLineStyle);
        
        fontSizePanel.add(labelPanel);
        JPanel botPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        botPanel.add(new JLabel("Font: "));
        botPanel.add(otFontSelector.getComponent());
        fontSizePanel.add(botPanel);
        
        colorSwatch = new ColorSwatchComponent(getStore(), curSwathColor, "Color");
        colorSwatch.setPreferredSize(Constants.DEFAULT_COLOR_PICKER_SIZE);
        
        colorPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        colorPanel.add(new JLabel("Color: "));
        colorPanel.add(colorSwatch);
        
        colorPanel.add(Box.createHorizontalStrut(5));
        colorPanel.add(new JLabel("Track Width: "));
        colorPanel.add(jcbSwathCenterLineWidth);
        
        colorPanel.add(Box.createHorizontalStrut(4));
        colorPanel.add(new JLabel("Track Style: "));
        colorPanel.add(jcbTrackLineStyle);
        
        colorPanel.add(Box.createHorizontalStrut(5));
        colorPanel.add(new JLabel("Edge Width: "));
        colorPanel.add(jcbSwathEdgeLineWidth);
        
        colorPanel.add(Box.createHorizontalStrut(4));
        colorPanel.add(new JLabel("Edge Style: "));
        colorPanel.add(jcbEdgeLineStyle);
        
        JPanel groundStationPanel = makeGroundStationPanel();
        
        swathWidthPanel = makeSwathWidthPanel();
        
        JPanel outerPanel = new JPanel(new MigLayout());
        
        JPanel mainPanel = new JPanel(new MigLayout());
        mainPanel.setBorder(BorderFactory.createTitledBorder(" Swath Controls "));
        mainPanel.add(swathWidthPanel, "wrap");
        mainPanel.add(fontSizePanel, "wrap");
        mainPanel.add(colorPanel, "wrap");
        mainPanel.add(makeBottomRow(SWATH_MODS));
        outerPanel.add(mainPanel, "wrap");
        outerPanel.add(groundStationPanel, "wrap");
        return outerPanel;
    }
    
    private CurveDrawer makeCoverageCircle(GroundStation gs) {
        double lat = Math.toRadians(gs.getElt().getLatitude().getValue());
        double lon = Math.toRadians(gs.getElt().getLongitude().getValue());
        double satelliteAltitude = gs.getAltitude();
        double elevation = gs.getElt().getAltitude().getValue();
        
        /* mean Earth radius in km */
        double earthRadius = AstroConst.R_Earth_mean / 1000.0;
        // total radius to satellite
        satelliteAltitude += earthRadius;
        double SAC = (Math.PI / 2.0) + Math.toRadians(gs.getAntennaAngle());
        // now accounts for station elevation - don't forget to convert from meters to km
        double sinASC = (earthRadius * sin(SAC)) / (satelliteAltitude - (elevation / 1000.0d));
        double dist = earthRadius * (Math.PI - SAC - asin(sinASC));
        double rat = dist / earthRadius;
        
        // 360 degrees +1 points so we connect final segment, last point to first
        int npts = 361;
        float[][] latlon = new float[2][npts];
        double cosDist = cos(rat);
        double sinDist = sin(rat);
        double sinLat = sin(lat);
        double cosLat = cos(lat);
        double sinLon = -sin(lon);
        double cosLon = cos(lon);
        for (int i = 0; i < npts; i++) {
            double azimuth = Math.toRadians((double) i);
            double cosBear = cos(azimuth);
            double sinBear = sin(azimuth);
            
            double z = (cosDist * sinLat) + (sinDist * cosLat * cosBear);
            
            double y = (cosLat * cosLon * cosDist) +
                (sinDist * ((sinLon * sinBear) - (sinLat * cosLon * cosBear)));
            
            double x = (cosLat * sinLon * cosDist) -
                (sinDist * ((cosLon * sinBear) + (sinLat * sinLon * cosBear)));
            
            double r = sqrt((x * x) + (y * y));
            
            double latRad = atan2(z, r);
            double lonRad = 0.0;
            if (r > 0.0) lonRad = -atan2(x, y);
            latlon[0][i] = (float) Math.toDegrees(latRad);
            latlon[1][i] = (float) Math.toDegrees(lonRad);
        }
        
        CurveDrawer coverageCircle = null;
        try {
            Gridded2DSet circle = new Gridded2DSet(LatitudeLongitudeTuple,
                latlon, npts);
            SampledSet[] set = new SampledSet[1];
            set[0] = circle;
            UnionSet uset = new UnionSet(set);
            coverageCircle = new CurveDrawer(uset);
            coverageCircle.setLineWidth(gs.getLineWidth());
            coverageCircle.setLineStyle(gs.getLineStyle());
            coverageCircle.setColor(gs.getColor());
            coverageCircle.setData(uset);
            coverageCircle.setDrawingEnabled(false);
            if (! inGlobeDisplay()) {
                coverageCircle.setConstantPosition(gsZ, navDsp.getDisplayAltitudeType());
            }
        } catch (Exception e) {
            logger.error("Problem creating coverage circle", e);
        }
        stationToCurve.put(gs, coverageCircle);
        return coverageCircle;
    }
    
    public Color getAntColor() {
        if (antColor == null) antColor = defaultAntColor;
        return antColor;
    }
    
    /**
     * @return the curEdgeLineStyle
     */
    public int getCurEdgeLineStyle() {
        return curEdgeLineStyle;
    }
    
    /**
     * @param curEdgeLineStyle the curEdgeLineStyle to set
     */
    public void setCurEdgeLineStyle(int curEdgeLineStyle) {
        this.curEdgeLineStyle = curEdgeLineStyle;
    }
    
    /**
     * @return the curLabelInterval
     */
    public int getCurLabelInterval() {
        return curLabelInterval;
    }
    
    /**
     * @param curLabelInterval the curLabelInterval to set
     */
    public void setCurLabelInterval(int curLabelInterval) {
        this.curLabelInterval = curLabelInterval;
    }
    
    /**
     * @return the curSwathCenterWidth
     */
    public int getCurSwathCenterWidth() {
        return curSwathCenterWidth;
    }
    
    /**
     * @param curSwathCenterWidth the curSwathCenterWidth to set
     */
    public void setCurSwathCenterWidth(int curSwathCenterWidth) {
        this.curSwathCenterWidth = curSwathCenterWidth;
    }
    
    /**
     * @return the curSwathEdgeWidth
     */
    public int getCurSwathEdgeWidth() {
        return curSwathEdgeWidth;
    }
    
    /**
     * @param curSwathEdgeWidth the curSwathEdgeWidth to set
     */
    public void setCurSwathEdgeWidth(int curSwathEdgeWidth) {
        this.curSwathEdgeWidth = curSwathEdgeWidth;
    }
    
    /**
     * @return the curSwathColor
     */
    public Color getCurSwathColor() {
        return curSwathColor;
    }
    
    /**
     * @param curSwathColor the curSwathColor to set
     */
    public void setCurSwathColor(Color curSwathColor) {
        this.curSwathColor = curSwathColor;
    }
    
    /**
     * @return the curTrackLineStyle
     */
    public int getCurTrackLineStyle() {
        return curTrackLineStyle;
    }
    
    /**
     * @param curTrackLineStyle the curTrackLineStyle to set
     */
    public void setCurTrackLineStyle(int curTrackLineStyle) {
        this.curTrackLineStyle = curTrackLineStyle;
    }
    
    /**
     * @return the curWidth
     */
    public double getCurWidth() {
        return curWidth;
    }
    
    /**
     * @param curWidth the curWidth to set
     */
    public void setCurWidth(double curWidth) {
        this.curWidth = curWidth;
    }
    
    public PolarOrbitTrackDataSource getDataSource() {
        DataSourceImpl ds = null;
        List dataSources = getDataSources();
        boolean gotit = false;
        if (! dataSources.isEmpty()) {
            int nsrc = dataSources.size();
            for (int i = 0; i < nsrc; i++) {
                ds = (DataSourceImpl) dataSources.get(nsrc - i - 1);
                if (ds instanceof PolarOrbitTrackDataSource) {
                    gotit = true;
                    break;
                }
            }
        }
        if (! gotit) return null;
        return (PolarOrbitTrackDataSource) ds;
    }
    
    /* (non-Javadoc)
     * @see ucar.unidata.idv.control.DisplayControlImpl#getDisplayListData()
     */
    @Override
    protected Data getDisplayListData() {
        
        // get time range that was specified in the Field Selector
        String startTime = (String) getDataInstance().getDataSelection().getProperties().get(TimeRangeSelection.PROP_BEGTIME);
        String endTime = (String) getDataInstance().getDataSelection().getProperties().get(TimeRangeSelection.PROP_ENDTIME);
        
        // get the template used for the Display Properties Layer Label
        String labelTemplate = getDisplayListTemplate();
        
        // see if time macro is enabled
        boolean hasTimeMacro = UtcDate.containsTimeMacro(labelTemplate);
        
        // fetch the label superclass would normally generate
        Data data = super.getDisplayListData();

        // if so, modify label with time range for this selection
        if (hasTimeMacro) {
            try {
                TextType tt = TextType.getTextType(DISPLAY_LIST_NAME);
                data  = new Text(tt, data.toString() + startTime + " - " + endTime);
            } catch (VisADException vade) {
                logger.error("Problem creating text", vade);
            }
        }
        
        // return either original or modified data object
        return data;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    /**
     * @return the otCurFont
     */
    public Font getOtCurFont() {
        return otCurFont;
    }
    
    /**
     * @param otCurFont the otCurFont to set
     */
    public void setOtCurFont(Font otCurFont) {
        this.otCurFont = otCurFont;
    }
    
    public String getStation() {
        return station;
    }
    
    /**
     * @return the swathEdgesOn
     */
    public boolean isSwathEdgesOn() {
        return swathEdgesOn;
    }
    
    /**
     * @param swathEdgesOn the swathEdgesOn to set
     */
    public void setSwathEdgesOn(boolean swathEdgesOn) {
        this.swathEdgesOn = swathEdgesOn;
    }
    
    private float[][][] getSwath(float[][] track) {
        double earthRadius = AstroConst.R_Earth_mean / 1000.0;
        int npt = track[0].length - 1;
        float[][][] ret = new float[2][2][npt - 1];
        try {
            int indx = 0;
            for (int i = 1; i < npt; i++) {
                double latA = Math.toRadians(track[0][i - 1]);
                double lonA = Math.toRadians(track[1][i - 1]);
                
                double latB = Math.toRadians(track[0][i]);
                double lonB = Math.toRadians(track[1][i]);
                
                double diffLon = lonB - lonA;
                double bX = cos(latB) * cos(diffLon);
                double bY = cos(latB) * sin(diffLon);
                double xFac = cos(latA) + bX;
                double latC = atan2(sin(latA) + sin(latB), sqrt(xFac * xFac + bY * bY));
                double lonC = lonA + atan2(bY, xFac);
                
                double bearing = atan2(sin(diffLon) * cos(latB),
                    cos(latA) * sin(latB) - sin(latA) * cos(latB) * cos(diffLon))
                    + (Math.PI / 2.0);
                double dist = curWidth / 2.0;
                dist /= earthRadius;
                double lat = asin((sin(latC) * cos(dist)) +
                    (cos(latC) * sin(dist) * cos(bearing)));
                double lon = lonC + atan2(sin(bearing) * sin(dist) * cos(latC),
                    cos(dist) - (sin(latC) * sin(lat)));
                float latD = (float) Math.toDegrees(lat);
                float lonD = (float) Math.toDegrees(lon);
                
                bearing += Math.PI;
                lat = asin((sin(latC) * cos(dist)) +
                    (cos(latC) * sin(dist) * cos(bearing)));
                lon = lonC + atan2(sin(bearing) * sin(dist) * cos(latC),
                    cos(dist) - (sin(latC) * sin(lat)));
                float latE = (float) Math.toDegrees(lat);
                float lonE = (float) Math.toDegrees(lon);
                
                ret[0][0][indx] = latD;
                ret[0][1][indx] = lonD;
                
                ret[1][0][indx] = latE;
                ret[1][1][indx] = lonE;
                ++indx;
            }
        } catch (Exception e) {
            logger.error("Problem getting swath", e);
            return null;
        }
        return ret;
    }
    
    /**
     * Overridden by McIDAS-V so that we can force the {@code display name} to
     * {@literal "Satellite Orbit Track"} when loading from a bundle.
     *
     * <p>This is done because {@link #init(DataChoice)} will call 
     * {@link #setDisplayName(String)} essentially like this: 
     * {@code setDisplayName(getLongParamName() + " " + getDisplayName()}. 
     * This results in the display name for a bundled orbit track control
     * being something like 
     * {@literal "SUOMI NPP SUOMI NPP Satellite Orbit Track"}.</p>
     *
     * @param vc Context in which this control exists.
     * @param properties Properties that may hold things.
     * @param preSelectedDataChoices Set of preselected data choices.
     */

    @Override public void initAfterUnPersistence(ControlContext vc,
                                                 Hashtable properties,
                                                 List preSelectedDataChoices)
    {
        setDisplayName("Satellite Orbit Track");
        super.initAfterUnPersistence(vc, properties, preSelectedDataChoices);
        jcbTrackLineStyle.setSelectedIndex(curTrackLineStyle);
        jcbEdgeLineStyle.setSelectedIndex(curEdgeLineStyle);
        jcbSwathCenterLineWidth.setSelectedIndex(curSwathCenterWidth - 1);
        jcbSwathEdgeLineWidth.setSelectedIndex(curSwathEdgeWidth - 1);
        
        // no idea if these invokeLater calls should be grouped into a single
        // call or not :(
        
        SwingUtilities.invokeLater(() -> {
            DefaultComboBoxModel<GroundStation> cbm =
                new DefaultComboBoxModel<>();
            
            for (GroundStation s : stations) {
                logger.trace("adding ground station {}", s);
                cbm.addElement(s);
                CurveDrawer cd = makeCoverageCircle(s);
                TextDisplayable td = labelGroundStation(s);
                stationToCurve.put(s, cd);
                stationToText.put(s, td);
                addDisplayable(cd);
                addDisplayable(td);
                // We don't know how many to expect, so update "current station" labels
                // with last one processed when restoring bundles.
                latLabel.setText(s.getElt().getLatitude().toString());
                lonLabel.setText(s.getElt().getLongitude().toString());
                altLabel.setText(s.getElt().getAltitude().toString());
            }
            jcbStationsPlotted.setModel(cbm);
        });
        
        SwingUtilities.invokeLater(() -> {
            js.getModel().setValue(curLabelInterval);
            jcbLabels.getModel().setSelected(showingLabels);
        });
    }

    @Override public boolean init(DataChoice dataChoice)
        throws VisADException, RemoteException
    {
        logger.debug("init() in...");
        
        PolarOrbitTrackDataSource potdc = getDataSource();
        
        // Show tool tips immediately
        ToolTipManager.sharedInstance().setInitialDelay(0);

        // if we're not coming back from a bundle, then we need to handle
        // otCurFont being null (was previously done in constructor)
        if (otCurFont == null) {
            otCurFont = FontSelector.DEFAULT_FONT;
        }
        otCurFontSize = otCurFont.getSize();
        
        if (potdc.getTrs() != null) {
            // validate time range before going ahead with control initialization
            if (! potdc.getTrs().begTimeOk()) {
                JOptionPane.showMessageDialog(null,
                    "Invalid start time, must follow format HH:MM:SS",
                    ERR_DIALOG_TITLE, JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            if (! potdc.getTrs().endTimeOk()) {
                JOptionPane.showMessageDialog(null,
                    "Invalid end time, must follow format HH:MM:SS",
                    ERR_DIALOG_TITLE, JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            if (! potdc.getTrs().timeRangeOk()) {
                JOptionPane.showMessageDialog(null,
                    "Invalid time range selection, please correct",
                    ERR_DIALOG_TITLE, JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            // allow at most two full days of orbit tracks - more than this will
            // at best clutter the display and at worst grind McV indefinitely
            long timeDiff = potdc.getTrs().getTimeRangeInSeconds();
            if (timeDiff >= (60 * 60 * 24 * 2)) {
                JOptionPane.showMessageDialog(null,
                    "Time range greater than two full days is not allowed, please correct",
                    ERR_DIALOG_TITLE, JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        
        // instantiate components we need to exist at initialization
        latLabel = new JLabel();
        lonLabel = new JLabel();
        altLabel = new JLabel();
        
        // create time label checkbox toggle, start out enabled
        jcbLabels = new JCheckBox("Labels On/Off");
        jcbLabels.setSelected(true);
        jcbLabels.setName(CHECKBOX_LABELS);
        jcbLabels.addItemListener(this);
        
        // create swath edges toggle, start out disabled
        jcbSwathEdges = new JCheckBox("Swath Edges On/Off");
        jcbSwathEdges.setSelected(swathEdgesOn);
        jcbSwathEdges.setName(CHECKBOX_SWATH_EDGES);
        jcbSwathEdges.addItemListener(this);
        
        // initialize the various swath and groundstation params
        jcbSwathCenterLineWidth.setSelectedIndex(curSwathCenterWidth - 1);
        jcbSwathEdgeLineWidth.setSelectedIndex(curSwathEdgeWidth - 1);
        jcbEdgeLineStyle.setSelectedIndex(curEdgeLineStyle);
        jcbTrackLineStyle.setSelectedIndex(curTrackLineStyle);
        
        otFontSelector = new FontSelector(FontSelector.COMBOBOX_UI, false, false);
        otFontSelector.setFont(otCurFont);
        gsFontSelector = new FontSelector(FontSelector.COMBOBOX_UI, false, false);
        gsFontSelector.setFont(FontSelector.DEFAULT_FONT);
        
        // Bump default font size down just a bit...
        gsFontSelector.setFontSize(9);
        otCurFont = otFontSelector.getFont();
        otCurFontSize = otCurFont.getSize();
        
        this.dataChoice = dataChoice;
        String choiceName = dataChoice.getName();
        NodeList nodeList = root.getElementsByTagName(TAG_SATELLITE);
        int num = nodeList.getLength();
        if (num > 0) {
            for (int i = 0; i < num; i++) {
                Element n = (Element) (nodeList.item(i));
                String satName = n.getAttribute(ATTR_NAME);
                if (satName.equals(choiceName)) {
                    String strWidth = n.getAttribute(ATTR_WIDTH);
                    if (strWidth.isEmpty()) strWidth = "0";
                    Double dWidth = new Double(strWidth);
                    curWidth = dWidth.doubleValue();
                    break;
                }
            }
        }
        try {
            trackDsp = new CompositeDisplayable();
            timeLabelDsp = new CompositeDisplayable();
            swathEdgeDsp = new CompositeDisplayable();
        } catch (Exception e) {
            logger.error("Problem creating composite displayable", e);
            return false;
        }
        boolean result = super.init((DataChoice) this.getDataChoices().get(0));
        
        String dispName = getDisplayName();
        setDisplayName(getLongParamName() + ' ' + dispName);
        logger.debug("Setting display name: {}", getDisplayName());
        try {
            String longName = 
                REGEX.matcher(getLongParamName()).replaceAll("");
            otTextType = new TextType(SWATH_MODS + longName);
        } catch (Exception e) {
            logger.trace("Problem creating texttype", e);
            otTextType = TextType.Generic;
        }
        
        Data data = getData(getDataInstance());
        createTrackDisplay(data, true);
        dataSource = getDataSource();
        try {
            navDsp = getNavigatedDisplay();
            float defaultZ = getMapViewManager().getDefaultMapPosition();
            // we're just nudging a bit so tracks (and their labels) get drawn below
            // ground stations (and their labels), which get drawn over default map level
            // user can change this in map controls if they prefer maps on top
            gsZ = defaultZ + 0.02f;
            trackZ = defaultZ + 0.01f;
            // range on "map level" stuff is -1 to 1, stay within these limits
            if (trackZ > 1.0f) trackZ = 0.9f;
            if (gsZ > 1.0f) gsZ = 1.0f;
            if (! inGlobeDisplay()) {
                applyDisplayableLevels();
            }
        } catch (Exception e) {
            logger.error("Problem getting display center", e);
        }
        
        // set the default legend label template
        setLegendLabelTemplate(DisplayControlBase.MACRO_DISPLAYNAME);
        
        return result;
    }
    
    @Override
    public void itemStateChanged(ItemEvent ie) {
        
        // now we have multiple checkboxes, so first see which one applies
        String source = ((JCheckBox) ie.getSource()).getName();
        try {
            if (source.equals(CHECKBOX_LABELS)) {
                if (ie.getStateChange() == ItemEvent.DESELECTED) {
                    timeLabelDsp.setVisible(false);
                } else {
                    if (timeLabelDsp.displayableCount() > 0) {
                        // TJJ Apr 2019 - see if scale changed, if so need to redraw
                        float currentScale = getViewManager().getMaster().getDisplayScale();
                        if (Float.compare(currentScale, scale) != 0) {
                            scale = currentScale;
                            redrawAll();
                        } else {
                            timeLabelDsp.setVisible(true);
                        }
                    } else {
                        redrawAll();
                    }
                }
            }
            if (source.equals(CHECKBOX_SWATH_EDGES)) {
                // There must first be a valid swath width before we can draw edges
                // Test the current value in the text input and update if appropriate
                if (ie.getStateChange() == ItemEvent.DESELECTED) {
                    swathEdgeDsp.setVisible(false);
                    swathEdgesOn = false;
                } else {
                    swathEdgesOn = true;
                    float newSwathWidth = validateSwathWidthField();
                    if (newSwathWidth > 0) {
                        curWidth = newSwathWidth;
                        if (Double.compare(curWidth, prvWidth) != 0) {
                            prvWidth = curWidth;
                            redrawAll();
                        } else {
                            if (swathEdgeDsp.displayableCount() > 0) {
                                swathEdgeDsp.setVisible(true);
                            } else {
                                redrawAll();
                            }
                        }
                    }
                }
            }
        } catch (VisADException | RemoteException e) {
            logger.error("Problem handing state change", e);
        }
        
    }
    
    private TextDisplayable labelGroundStation(GroundStation station) {
        TextDisplayable groundStationDsp = null;
        try {
            String str = "+ " + station;
            logger.debug("Drawing station: {}", str);
            TextType tt = new TextType(STATION_MODS + ttCounter);
            groundStationDsp =
                new TextDisplayable(STATION_MODS + jcbStationsPlotted.getItemCount(), tt);
            ttCounter++;
            groundStationDsp.setJustification(TextControl.Justification.LEFT);
            groundStationDsp.setVerticalJustification(TextControl.Justification.CENTER);
            groundStationDsp.setColor(station.getColor());
            groundStationDsp.setFont(station.getFont());
            groundStationDsp.setTextSize((float) scale * station.getFont().getSize() / FONT_SCALE_FACTOR);
            groundStationDsp.setSphere(inGlobeDisplay());
            DisplayRealType dispType = navDsp.getDisplayAltitudeType();
            groundStationDsp.setConstantPosition(gsZ, dispType);
            
            double dlat = station.getElt().getValues()[0];
            double dlon = station.getElt().getValues()[1];
            RealTuple lonLat =
                new RealTuple(RealTupleType.SpatialEarth2DTuple,
                    new double[] { dlon, dlat });
            Tuple tup = new Tuple(makeTupleType(tt),
                new Data[] { lonLat, new Text(tt, str)});
            groundStationDsp.setData(tup);
            stationToText.put(station, groundStationDsp);
        } catch (Exception e) {
            logger.error("Problem drawing station", e);
        }
        return groundStationDsp;
    }
    
    private JPanel makeGroundStationPanel() {
        JPanel jp = new JPanel(new MigLayout());
        jp.setBorder(BorderFactory.createTitledBorder(" Ground Station Controls "));
        
        jcbStationLineStyle = new JComboBox<>(Constants.lineStyles);
        jcbStationLineStyle.addActionListener(this);
        jcbStationLineStyle.setSelectedIndex(1);
        
        locationComboBox = new JComboBox<>();
        jcbStationsPlotted = new JComboBox<>();
        jcbStationsPlotted.addItemListener(event -> {
            logger.debug("Active Station changed...");
            GroundStation gs = (GroundStation) jcbStationsPlotted.getSelectedItem();
            if (gs != null) {
                // Update UI with settings for this station
                updateGroundStationWidgets(gs);
            }
        });
        
        // Ground Stations are now a natural-order map (alphabetical)
        GroundStations gs = new GroundStations(null);
        GuiUtils.setListData(locationComboBox, gs.getGroundStations());
        
        // initialize reasonable output for no stations plotted yet
        if (locationComboBox.getItemCount() > 0) {
            latLabel.setText(" - ");
            lonLabel.setText(" - ");
            altLabel.setText(" - ");
        }
        
        locationPanel = new JPanel();
        locationPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        locationPanel.add(new JLabel("Ground Stations Available:"));
        locationPanel.add(locationComboBox);
        JButton addButton = new JButton("Add Selected");
        addButton.setToolTipText(TOOLTIP_ADD_SELECTED);
        addButton.setActionCommand(STATION_ADD);
        addButton.addActionListener(this);
        locationPanel.add(addButton);
        
        JPanel customPanel = new JPanel();
        customPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        customPanel.add(new JLabel("Custom Ground Station:   Label:"));
        customLab = new JTextField(6);
        customLab.setToolTipText(TOOLTIP_CUSTOM_LABEL);
        customPanel.add(customLab);
        customPanel.add(new JLabel("Lat:"));
        customLat = new JTextField(6);
        customLat.setToolTipText(TOOLTIP_CUSTOM_LAT);
        customPanel.add(customLat);
        customPanel.add(new JLabel("Lon:"));
        customLon = new JTextField(6);
        customLon.setToolTipText(TOOLTIP_CUSTOM_LON);
        customPanel.add(customLon);
        customPanel.add(new JLabel("Alt:"));
        customAlt = new JTextField(6);
        customAlt.setToolTipText(TOOLTIP_CUSTOM_ALT);
        customPanel.add(customAlt);
        customPanel.add(new JLabel("m"));
        JButton customButton = new JButton("Add Custom");
        customButton.setToolTipText(TOOLTIP_ADD_CUSTOM);
        customButton.setActionCommand(CUSTOM_ADD);
        customButton.addActionListener(this);
        customPanel.add(customButton);
        
        JPanel plottedStationsPanel = new JPanel();
        plottedStationsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        plottedStationsPanel.add(new JLabel("Ground Stations Plotted:"));
        plottedStationsPanel.add(jcbStationsPlotted);
        JButton remButton = new JButton("Remove Selected");
        remButton.setActionCommand(STATION_REM);
        remButton.addActionListener(this);
        plottedStationsPanel.add(remButton);
        
        latLonAltPanel = new JPanel();
        latLonAltPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        
        latLonAltPanel.add(new JLabel("Current Station,  Latitude: "));
        latLonAltPanel.add(latLabel);
        latLonAltPanel.add(Box.createHorizontalStrut(5));
        
        latLonAltPanel.add(new JLabel("Longitude: "));
        latLonAltPanel.add(lonLabel);
        latLonAltPanel.add(Box.createHorizontalStrut(5));
        
        latLonAltPanel.add(new JLabel("Altitude: "));
        latLonAltPanel.add(altLabel);
        
        JPanel gsFontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gsFontPanel.add(new JLabel("Font: "));
        gsFontPanel.add(gsFontSelector.getComponent());
        
        Color swatchAntColor = getAntColor();
        antColorSwatch = new ColorSwatchComponent(getStore(), swatchAntColor, "Color");
        antColorSwatch.setPreferredSize(Constants.DEFAULT_COLOR_PICKER_SIZE);
        
        antColorPanel = new JPanel();
        antColorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        antColorPanel.add(new JLabel("Color: "));
        antColorPanel.add(antColorSwatch);
        
        antColorPanel.add(Box.createHorizontalStrut(5));
        antColorPanel.add(new JLabel("Line Width: "));
        antColorPanel.add(jcbStationLineWidth);
        
        antColorPanel.add(Box.createHorizontalStrut(5));
        antColorPanel.add(new JLabel("Line Style: "));
        antColorPanel.add(jcbStationLineStyle);
        
        antColorPanel.add(Box.createHorizontalStrut(5));
        antColorPanel.add(new JLabel("Antenna Angle: "));
        antennaAngle.setToolTipText(TOOLTIP_ANTENNA_ANGLE);
        antColorPanel.add(antennaAngle);
        
        jp.add(locationPanel, "wrap");
        jp.add(customPanel, "wrap");
        jp.add(plottedStationsPanel, "wrap");
        jp.add(latLonAltPanel, "wrap");
        jp.add(Box.createVerticalStrut(5), "wrap");
        jp.add(gsFontPanel, "wrap");
        jp.add(antColorPanel, "wrap");
        jp.add(makeBottomRow(STATION_MODS));
        return jp;
    }
    
    /**
     * Create the {@literal "Apply"} button used by both sections of the 
     * control's GUI.
     * 
     * @param command {@literal "Command"} used in 
     *                {@link #actionPerformed(ActionEvent)}.
     * 
     * @return {@code JPanel} containing our {@literal "Apply"} button, 
     *         suitable for adding to the end of the control's bordered panels.
     */

    private JPanel makeBottomRow(String command) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton applyButton = new JButton("Apply");
        applyButton.setActionCommand(command);
        applyButton.addActionListener(this);
        row.add(applyButton);
        return row;
    }
    
    private JPanel makeSwathWidthPanel() {
        if (dataChoice != null)
            satelliteName = new JLabel(dataChoice.getName());
        swathWidthFld = new JTextField("" + curWidth, 5);
        if (curWidth == 0) swathWidthFld.setText(SWATH_NA);
        
        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // first on this panel, check box to turn on/off swath line edges
        jp.add(jcbSwathEdges);
        
        jp.add(Box.createHorizontalStrut(5));
        jp.add(new JLabel("Satellite: "));
        jp.add(satelliteName);
        jp.add(Box.createHorizontalStrut(5));
        jp.add(new JLabel("Swath Width: "));
        swathWidthFld.setToolTipText(TOOLTIP_SWATH_WIDTH);
        jp.add(swathWidthFld);
        jp.add(kmLabel);
        jp.add(Box.createHorizontalStrut(5));
        
        return jp;
    }
    
    private TupleType makeTupleType(TextType tt) {
        TupleType t = null;
        try {
            t = new TupleType(new MathType[] {RealTupleType.SpatialEarth2DTuple, tt});
        } catch (Exception e) {
            logger.error("Problem creating TupleType", e);
        }
        return t;
    }
    
    private void addGroundStation(GroundStation gs, boolean isCustom) {
        
        logger.debug("addGroundStation() in, name: {}", gs.getName());
        
        try {
            
            EarthLocationTuple elt = gs.getElt();
            latLabel.setText(elt.getLatitude().toString());
            lonLabel.setText(elt.getLongitude().toString());
            altLabel.setText(elt.getAltitude().toString());
            
            // quick and easy way to limit sig digits to something not too crazy
            if (altLabel.getText().length() > 10) altLabel.setText(altLabel.getText().substring(0, 9));
            latitude = Double.parseDouble(latLabel.getText());
            longitude = Double.parseDouble(lonLabel.getText());

            // For non-custom, "stock" groundstations, compute altitude
            // For custom, the user will have specified it
            if (! isCustom) {
                double altitude = dataSource.getNearestAltToGroundStation(latitude, longitude) / 1000.0;
                gs.setAltitude(altitude);
            }
            gs.setColor(antColorSwatch.getColor());
            gs.setLineWidth(jcbStationLineWidth.getSelectedIndex() + 1);
            gs.setLineStyle(jcbStationLineStyle.getSelectedIndex());
            gs.setFont(gsFontSelector.getFont());
            CurveDrawer cd = makeCoverageCircle(gs);
            
            if (cd != null) {
                logger.debug("Adding ground station, station name: {}", gs.getName());
                TextDisplayable label = labelGroundStation(gs);
                cd.setConstantPosition(gsZ, navDsp.getDisplayAltitudeType());
                addDisplayable(cd);
                stationToCurve.put(gs, cd);
                stationToText.put(gs, label);
            } else {
                logger.error("could not draw curve!!");
            }
            
            TextDisplayable td = stationToText.get(gs);
            td.setConstantPosition(gsZ, navDsp.getDisplayAltitudeType());
            addDisplayable(td);
        } catch (Exception e) {
            logger.error("Problem adding ground station", e);
        }
    }
    
    private void updateStationColor(GroundStation gs, Color newColor)
        throws VisADException, RemoteException
    {
        gs.setColor(newColor);
        stationToCurve.get(gs).setColor(newColor);
        stationToText.get(gs).setColor(newColor);
    }
    
    private void updateStationFont(GroundStation gs, Font newFont)
        throws VisADException, RemoteException
    {
        gs.setFont(newFont);
        TextDisplayable label = stationToText.get(gs);
        label.setFont(newFont);
        scale = getViewManager().getMaster().getDisplayScale();
        label.setTextSize((float) scale * newFont.getSize() / FONT_SCALE_FACTOR);
    }
    
    private void replaceCurve(GroundStation gs)
        throws VisADException, RemoteException
    {
        gs.setLineWidth(jcbStationLineWidth.getSelectedIndex() + 1);
        gs.setLineStyle(jcbStationLineStyle.getSelectedIndex());
        CurveDrawer cdOld = stationToCurve.get(gs);
        CurveDrawer cdNew = makeCoverageCircle(gs);
        TextDisplayable label = stationToText.get(gs);
        cdNew.setLineWidth(gs.getLineWidth());
        removeDisplayable(cdOld);
        removeDisplayable(label);
        addDisplayable(cdNew);
        addDisplayable(label);
        stationToCurve.put(gs, cdNew);
    }
    
    private void updateGroundStationWidgets(GroundStation gs) {
        gsFontSelector.setFont(gs.getFont());
        antColorSwatch.setBackground(gs.getColor());
        jcbStationLineStyle.setSelectedIndex(gs.getLineStyle());
        jcbStationLineWidth.setSelectedIndex(gs.getLineWidth() - 1);
        antennaAngle.setText(String.valueOf(gs.getAntennaAngle()));
        curAngle = gs.getAntennaAngle();
        curElevation = (int) gs.getAltitude();
        
        EarthLocation elt = gs.getElt();
        latLabel.setText(String.valueOf(elt.getLatitude().getValue()));
        lonLabel.setText(String.valueOf(elt.getLongitude().getValue()));
        altLabel.setText(String.valueOf(elt.getAltitude().getValue()));
    }
    
    public void setStations(List<GroundStation> newStations) {
        stations.clear();
        stations.addAll(newStations);
    }
    
    public List<GroundStation> getStations() {
        // this clear() call is important!
        // if not done, saving more than one bundle will result in the 
        // "stations" list containing the contents of "jcbStationsPlotted"
        // for *every call to getStations()!*
        stations.clear();
        for (int i = 0; i < jcbStationsPlotted.getItemCount(); i++) {
            stations.add(jcbStationsPlotted.getItemAt(i));
        }
        return stations;
    }
    
    public void setShowingLabels(boolean newValue) {
        showingLabels = newValue;
    }
    
    public boolean getShowingLabels() {
        return jcbLabels.isSelected();
    }
    
    /* (non-Javadoc)
     * @see ucar.unidata.idv.control.DisplayControlImpl#projectionChanged()
     */
    @Override
    public void projectionChanged() {
        super.projectionChanged();
        applyDisplayableLevels();
    }
    
    public void setAntColor(Color c) {
        if (c == null) c = defaultAntColor;
        try {
            antColor = c;
        } catch (Exception e) {
            logger.error("Exception in PolarOrbitTrackControl.setAntColor", e);
        }
    }
    
    public void setStation(String val) {
        station = val.trim();
    }
    
    private float validateSwathWidthField() {
        String s = swathWidthFld.getText().trim();
        float val = -1.0f;
        try {
            val = Float.parseFloat(s);
        } catch (NumberFormatException nfe) {
            // TJJ Jun 2015 - if GEO sensor, N/A means return invalid, but no warning msg needed
            if ((s != null) && (s.equals(SWATH_NA))) {
                return -2;
            }
            // throw up a dialog to tell user the problem
            JOptionPane.showMessageDialog(latLonAltPanel,
                "Invalid swath width: must be a decimal value in km");
            return -1;
        }
        
        // Need <= on low end because value must be positive
        if ((val <= SWATH_WIDTH_MIN) || (val > SWATH_WIDTH_MAX)) {
            // throw up a dialog to tell user the problem
            JOptionPane.showMessageDialog(latLonAltPanel,
                "Swath width valid range is > " + SWATH_WIDTH_MIN +
                    " to " + SWATH_WIDTH_MAX + " km");
            return -1;
        }
        return val;
    }
    
    /* (non-Javadoc)
     * @see ucar.unidata.idv.control.DisplayControlImpl#showProperties()
     * We need this because the TimeSelection widget in preview window
     * and properties window are two different objects we are trying to
     * keep in sync, and only redraw the display when necessary.
     */
    
    @Override public void showProperties() {
        oldProps = new Hashtable(getDataInstance().getDataSelection().getProperties());
        
        JTabbedPane jtp = new JTabbedPane();
        addPropertiesComponents(jtp);
        final JDialog propertiesDialog = GuiUtils.createDialog("Properties -- " + getTitle(), true);
        ActionListener listener = event -> {
            String cmd = event.getActionCommand();
            if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_APPLY)) {
                if (! applyProperties()) {
                    return;
                }
                PolarOrbitTrackDataSource ds = getDataSource();
                ds.setSelectionProps(getDataSelection().getProperties());
                if (! oldProps.equals(getDataSelection().getProperties())) {
                    redrawAll();
                    oldProps = getDataSelection().getProperties();
                }
            }
            if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_CANCEL)) {
                propertiesDialog.dispose();
            }
        };
        Window f = GuiUtils.getWindow(getContents());
        JComponent buttons = GuiUtils.makeApplyOkCancelButtons(listener);
        JComponent propContents = inset(centerBottom(jtp, buttons), 5);
        Msg.translateTree(jtp, true);
        propertiesDialog.getContentPane().add(propContents);
        propertiesDialog.pack();
        if (f != null) {
            GuiUtils.showDialogNearSrc(f, propertiesDialog);
        } else {
            propertiesDialog.setVisible(true);
        }
    }
}
