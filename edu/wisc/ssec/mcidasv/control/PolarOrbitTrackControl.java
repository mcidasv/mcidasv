/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

package edu.wisc.ssec.mcidasv.control;

import edu.wisc.ssec.mcidasv.data.GroundStations;
import edu.wisc.ssec.mcidasv.data.PolarOrbitTrackDataSource;
import edu.wisc.ssec.mcidasv.data.adde.sgp4.AstroConst;
import edu.wisc.ssec.mcidasv.data.hydra.CurveDrawer;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.Math;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.idv.control.DisplayControlImpl;
//import ucar.unidata.idv.control.ZSlider;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.GuiUtils.ColorSwatch;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.visad.display.CompositeDisplayable;
import ucar.visad.display.Displayable;
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
import visad.TextControl.Justification;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.UnionSet;
import visad.VisADException;
import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

/**
 * {@link ucar.unidata.idv.control.PolarOrbitTrackControl} with some McIDAS-V
 * specific extensions. Namely parameter sets and support for inverted 
 * parameter defaults.
 */
public class PolarOrbitTrackControl extends DisplayControlImpl {

    private static final Logger logger = LoggerFactory.getLogger(PolarOrbitTrackControl.class);

    /** The spacing used in the grid layout */
    protected static final int GRID_SPACING = 3;

    /** Used by derived classes when they do a GuiUtils.doLayout */
    protected static final Insets GRID_INSETS = new Insets(GRID_SPACING,
                                                    GRID_SPACING,
                                                    GRID_SPACING,
                                                    GRID_SPACING);
    /**
     * position slider
     */
//    private ZSlider levelSlider = null;
    private double latitude;
    private double longitude;
    private double altitude;
    private JPanel fontSizePanel;
    private JPanel colorPanel;
    private JPanel antColorPanel;
    private JPanel locationPanel;
    private JPanel latLonAltPanel;

    private int locationIndex = -1;
    private List stations;
    private List lats;
    private List lons;
    private List alts;

    /** Property name to get the list or urls */
    public final String PREF_GROUNDSTATIONS = "mcv.groundstations";

    /** Manages the pull down list of urls */
    private static PreferenceList prefList;

    private JComboBox locationComboBox;
    private JTextField locationEditor;

    private String station = "";
    private TextDisplayable groundStationDsp;

    private static final int defaultAntAngle = 5;
    private int angle = defaultAntAngle;

    /** Input for lat/lon center point */
    protected LatLonWidget latLonWidget = new LatLonWidget();

    private JTextField latFld;
    private JTextField lonFld;
    private JTextField altitudeFld = new JTextField(" ", 5);
    private JTextField antennaAngle = new JTextField(" 5", 5);

//    private ChangeListener sizeListener;
    private ActionListener fontSizeChange;
    private FocusListener fontSizeFocusChange;

    /** Font size control */
    private static final int SLIDER_MAX = 10;
    private static final int SLIDER_MIN = 1;
    private static final int SLIDER_WIDTH = 150;
    private static final int SLIDER_HEIGHT = 16;

    private JSlider fontSizeSlider;
    private JTextField fontSizeFld = new JTextField();

    private CompositeDisplayable trackDsp;
    private CompositeDisplayable circleDsp;
    private static final TupleType TUPTYPE = makeTupleType();

    private int fontSize;
    private int defaultSize = 3;
    private ColorSwatch colorSwatch;
    private Color color;
    private Color defaultColor = Color.GREEN;
    private ColorSwatch antColorSwatch;
    private Color antColor;
    private Color defaultAntColor = Color.WHITE;
    private PolarOrbitTrackDataSource dataSource;

    private CurveDrawer coverageCircle;
    private double satelliteAltitude = 0.0;

    private double centerAlt = 0.0;
    private double centerLat = 0.0;
    private double centerLon = 0.0;
    private double satZ = 0.0;
    private NavigatedDisplay navDsp = null;


    public PolarOrbitTrackControl() {
        super();
        logger.trace("created new tlecontrol={}", Integer.toHexString(hashCode()));
    }

    @Override public boolean init(DataChoice dataChoice) 
        throws VisADException, RemoteException 
    {
        try {
            trackDsp = new CompositeDisplayable();
            circleDsp = new CompositeDisplayable();
        } catch (Exception e) {
            System.out.println("problem creating composite displayable e=" + e);
            return false;
        }
        boolean result = super.init((DataChoice)this.getDataChoices().get(0));

        String dispName = getDisplayName();
        setDisplayName(getLongParamName() + " " + dispName);

        Data data = getData(getDataInstance());
        createTrackDisplay(data);
        this.dataSource = getDataSource();
        try {
            navDsp = getNavigatedDisplay();
            EarthLocation earthLoc = navDsp.getCenterPoint();
            LatLonPoint llp = earthLoc.getLatLonPoint();
            centerLat = llp.getLatitude().getValue();
            centerLon = llp.getLongitude().getValue();
            centerAlt = dataSource.getNearestAltToGroundStation(centerLat, centerLon)/1000.0;
            EarthLocationTuple elt = new EarthLocationTuple(centerLat, centerLon, centerAlt);
            double[] xyz = navDsp.getSpatialCoordinates((EarthLocation)elt).getValues();
            satZ = xyz[2]/5.0;
            applyTrackPosition();
        } catch (Exception e) {
            System.out.println("get display center e=" + e);
        }
        return result;
    }

    private void createTrackDisplay(Data data) {
        try {
            this.fontSize = getFontSize();
            this.color = getColor();
            List<String> dts = new ArrayList();
            if (data instanceof Tuple) {
                Data[] dataArr = ((Tuple)data).getComponents();
                int npts = dataArr.length;
                float[][] latlon = new float[2][npts];
                float fSize = this.fontSize/10.f;
                for (int i=0; i<npts; i++) {
                    Tuple t = (Tuple)dataArr[i];
                    Data[] tupleComps = t.getComponents();
                    String str = ((Text)tupleComps[0]).getValue();
                    dts.add(str);
                    int indx = str.indexOf(" ") + 1;
                    String subStr = "- " + str.substring(indx, indx+5);

                    TextDisplayable time = new TextDisplayable(TextType.Generic);
                    time.setJustification(TextControl.Justification.LEFT);
                    time.setVerticalJustification(TextControl.Justification.CENTER);
                    time.setColor(this.color);
                    
                    addDisplayable(time, FLAG_COLORTABLE);
                    LatLonTuple llt = (LatLonTuple)tupleComps[1];
                    double dlat = llt.getLatitude().getValue();
                    double dlon = llt.getLongitude().getValue();
                    RealTuple lonLat =
                        new RealTuple(RealTupleType.SpatialEarth2DTuple,
                            new double[] { dlon, dlat });
                    Tuple tup = new Tuple(TUPTYPE,
                        new Data[] { lonLat, new Text(subStr)});
                    time.setData(tup);
                    this.trackDsp.addDisplayable(time);
                    float lat = (float)dlat;
                    float lon = (float)dlon;
                    //System.out.println("    Time=" + subStr + " Lat=" + lat + " Lon=" + lon);
                    latlon[0][i] = lat;
                    latlon[1][i] = lon;
                }
                setDisplayableTextSize(this.fontSize);
                Gridded2DSet track = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                           latlon, npts);
                SampledSet[] set = new SampledSet[1];
                set[0] = track;
                UnionSet uset = new UnionSet(set);
                CurveDrawer trackLines = new CurveDrawer(uset);
                trackLines.setData(uset);
                this.trackDsp.addDisplayable(trackLines);
                this.trackDsp.setColor(this.color);
                this.trackDsp.setLineWidth(2.0f);
                addDisplayable(this.trackDsp, FLAG_COLORTABLE);
                trackLines.setDrawingEnabled(false);
            }
        } catch (Exception e) {
            System.out.println("getData e=" + e);
        }
        return;
    }

    private static TupleType makeTupleType() {
        TupleType t = null;
        try {
            t = new TupleType(new MathType[] {RealTupleType.SpatialEarth2DTuple,
                                              TextType.Generic});
        } catch (Exception e) {
            System.out.println("\nPolarOrbitTrackControl.makeTupleType e=" + e);
        }
        return t;
    }

    public JComponent makeColorBox(Color swatchColor) {
        GuiUtils.ColorSwatch swatch = new GuiUtils.ColorSwatch(swatchColor,
                                               "Color") {
            public void userSelectedNewColor(Color c) {
                try {
                    getIdv().showWaitCursor();
                    setColor(c);
                    setBackground(c);
                    getIdv().showNormalCursor();
                } catch (Exception e) {
                    System.out.println("\nsetColor e=" + e);
                    setColor(defaultColor);
                }
            }
        };
        return swatch;
    }

    public JComponent makeAntColorBox(Color swatchAntColor) {
        GuiUtils.ColorSwatch swatch = new GuiUtils.ColorSwatch(swatchAntColor,
                                               "Color") {
            public void userSelectedNewColor(Color c) {
                try {
                    getIdv().showWaitCursor();
                    setAntColor(c);
                    setBackground(c);
                    getIdv().showNormalCursor();
                } catch (Exception e) {
                    System.out.println("\nsetAntColor e=" + e);
                    setAntColor(defaultAntColor);
                }
            }
        };
        return swatch;
    }

    /**
     * Called by doMakeWindow in DisplayControlImpl, which then calls its
     * doMakeMainButtonPanel(), which makes more buttons.
     *
     * @return container of contents
     */
    public Container doMakeContents() {
/*
        JPanel topPanel = GuiUtils.leftCenter(
                        new JLabel("Track Z-Position:  "),
                        makePositionSlider());
*/
        this.fontSizeChange =new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String str = fontSizeFld.getText();
                int size = new Integer(str).intValue();
                moveFontSizeSlider(size);
                setDisplayableTextSize(size);
            }
        };
        this.fontSizeFocusChange = new FocusListener() {
            public void focusGained(FocusEvent fe) {
            }
            public void focusLost(FocusEvent fe) {
                String str = fontSizeFld.getText();
                int size = new Integer(str).intValue();
                moveFontSizeSlider(size);
                setDisplayableTextSize(size);
            }
        };

        this.fontSizeSlider = GuiUtils.makeSlider(SLIDER_MIN, SLIDER_MAX, defaultSize,
                                     this, "sliderChanged", true);
        this.fontSizeSlider.setPreferredSize(new Dimension(SLIDER_WIDTH,SLIDER_HEIGHT));
        this.fontSizeSlider.setMajorTickSpacing(1);
        this.fontSizeSlider.setSnapToTicks(true);
        int size = getSizeValue(this.fontSizeSlider);
        setFontSize(size);
        this.fontSizeFld = new JTextField(Integer.toString(size),3);
        this.fontSizeFld.addFocusListener(this.fontSizeFocusChange);
        this.fontSizeFld.addActionListener(this.fontSizeChange);
        this.fontSizePanel = GuiUtils.doLayout( new Component[] {
                 new JLabel("FontSize: "),
                 this.fontSizeFld,
                 new JLabel(" "),
                 this.fontSizeSlider }, 4,
                 GuiUtils.WT_N, GuiUtils.WT_N);

        Color swatchColor = getColor();
        colorSwatch = (GuiUtils.ColorSwatch)makeColorBox(swatchColor);
        colorPanel = GuiUtils.doLayout(new Component[] {
                           new JLabel("Set Color: "),
                           colorSwatch }, 2,
                           GuiUtils.WT_N, GuiUtils.WT_N);
        JPanel groundStationPanel = makeGroundStationPanel();

        Insets  dfltGridSpacing = new Insets(4, 0, 4, 0);
        String  dfltLblSpacing  = " ";
        List allComps = new ArrayList();

//        allComps.add(topPanel);
//        allComps.add(new JLabel(" "));
//        allComps.add(new JLabel(" "));
        allComps.add(fontSizePanel);
        allComps.add(colorPanel);
        allComps.add(new JLabel(" "));
        allComps.add(new JLabel(" "));
        allComps.add(locationPanel);
        allComps.add(groundStationPanel);
        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel dateTimePanel = GuiUtils.doLayout(allComps, 1, GuiUtils.WT_NY,
                               GuiUtils.WT_N);
        return GuiUtils.top(dateTimePanel);
    }

    private JPanel makeGroundStationPanel() {
        locationComboBox = new JComboBox();
        locationComboBox.setEditable(true);
        locationEditor = (JTextField)locationComboBox.getEditor().getEditorComponent();
        locationEditor.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
            public void keyTyped(KeyEvent e) {
                locationIndex = -1;
            }
        });

        GroundStations gs = new GroundStations(null);
        stations = gs.getStations();
        GuiUtils.setListData(locationComboBox, stations);
        lats = gs.getLatitudes();
        lons = gs.getLongitudes();
        alts = gs.getAltitudes();

        locationComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setStation((String)locationComboBox.getSelectedItem());
                locationIndex = locationComboBox.getSelectedIndex();
                //System.out.println("\n locationIndex=" + locationIndex);
                if (locationIndex < 0) {
                    //locationIndex = 0;
                    //locationComboBox.setSelectedIndex(locationIndex);
                    try {
                        Object stat = (Object)getStation();
                        //System.out.println("\n station=" + stat);
                        if (stations.contains(stat)) {
                            locationIndex = stations.indexOf(stat);
                            //System.out.println("\nstat at location: " + locationIndex);
                            locationComboBox.setSelectedIndex(locationIndex);
                        }
/*
                          else {
                            locationIndex = 0;
                            locationComboBox.insertItemAt(stat, locationIndex);
                            locationComboBox.setSelectedIndex(locationIndex);
                            stations.add(locationIndex, stat);
                            locationEditor.setText(getStation());
                            String zero = "";
                            lats.add(locationIndex, zero);
                            lons.add(locationIndex, zero);
                            alts.add(locationIndex, zero);
                            latLonWidget.setLatLon(" ", " ");
                            altitudeFld.setText(zero);
                            setAltitude();
                        }
*/
                    } catch (Exception e) {
                    }
                } else {
                    try {
                        String str = (String)(lats.get(locationIndex));
                        Double d = new Double(str);
                        double dVal = d.doubleValue();
                        latLonWidget.setLat(dVal);
                        setLatitude();
                        str = (String)(lons.get(locationIndex));
                        d = new Double(str);
                        dVal = d.doubleValue();
                        latLonWidget.setLon(dVal);
                        setLongitude();
                        str = (String)(alts.get(locationIndex));
                        altitudeFld.setText(str);
                        setAltitude();
                        int val = getAntennaAngle();
                        setAntennaAngle(val);
                    } catch (Exception e) {
                    }
                }
                setSatelliteAltitude(dataSource.getNearestAltToGroundStation(latitude, longitude)/1000.0);
                redrawCoverageCircle();
            }
        });

        latFld = latLonWidget.getLatField();
        lonFld = latLonWidget.getLonField();
        String str = (String)(alts.get(0));
        altitudeFld = new JTextField(str, 5);
        str = (String)(lats.get(0));
        Double d;
        double dVal;
        if (!str.equals(" ")) {
            d = new Double(str);
            dVal = d.doubleValue();
            latLonWidget.setLat(dVal);
        }
        if (!str.equals(" ")) {
            str = (String)(lons.get(0));
            d = new Double(str);
            dVal = d.doubleValue();
            latLonWidget.setLon(dVal);
        }

        ActionListener latLonListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setLatitude();
                setLongitude();
                setAltitude();
                redrawCoverageCircle();
            }
        };
        FocusListener latLonFocusChange = new FocusListener() {
            public void focusGained(FocusEvent fe) {
                latFld.setCaretPosition(latFld.getText().length());
                lonFld.setCaretPosition(lonFld.getText().length());
            }
            public void focusLost(FocusEvent fe) {
                setLatitude();
                setLongitude();
                setAltitude();
                redrawCoverageCircle();
            }
        };
        latFld.addActionListener(latLonListener);
        lonFld.addActionListener(latLonListener);
        latFld.addFocusListener(latLonFocusChange);
        lonFld.addFocusListener(latLonFocusChange);
        antennaAngle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String str = antennaAngle.getText();
                Integer iVal = new Integer(str.trim());
                int val = iVal.intValue();
                setAntennaAngle(val);
                redrawCoverageCircle();
            }
        });
        antennaAngle.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent fe) {
            }
            public void focusLost(FocusEvent fe) {
                String str = antennaAngle.getText();
                Integer iVal = new Integer(str.trim());
                int val = iVal.intValue();
                setAntennaAngle(val);
                redrawCoverageCircle();
            }
        });
        locationPanel = GuiUtils.doLayout(new Component[] {
                           new JLabel("Ground Station: "),
                           locationComboBox }, 2,
                           GuiUtils.WT_N, GuiUtils.WT_N);
        latLonAltPanel = GuiUtils.doLayout(new Component[] {
                           latLonWidget,
                           new JLabel(" Altitude: "), altitudeFld,
                           new JLabel(" Antenna Angle: "), antennaAngle }, 5,
                           GuiUtils.WT_N, GuiUtils.WT_N);

        Color swatchAntColor = getAntColor();
        antColorSwatch = (GuiUtils.ColorSwatch)makeAntColorBox(swatchAntColor);
        antColorPanel = GuiUtils.doLayout(new Component[] {
                           new JLabel("Set Color: "),
                           antColorSwatch }, 2,
                           GuiUtils.WT_N, GuiUtils.WT_N);

        List allComps = new ArrayList();
        allComps.add(locationPanel);
        allComps.add(new JLabel(" "));
        allComps.add(latLonAltPanel);
        allComps.add(new JLabel(" "));
        allComps.add(antColorPanel);
        JPanel retPanel = GuiUtils.doLayout(allComps, 1, GuiUtils.WT_NY,
                               GuiUtils.WT_N);
        return GuiUtils.top(retPanel);
    }

    private void getGroundStation() {
    }
/*
    private JComponent makePositionSlider() {
        levelSlider = new ZSlider(this.satZ) {
            public void valueHasBeenSet() {
                applyTrackPosition();
            }
        };
        return levelSlider.getContents();
    }
*/
    /**
     * Apply the map (height) position to the displays
     */
    private void applyTrackPosition() {
        try {
            DisplayRealType dispType = navDsp.getDisplayAltitudeType();
            trackDsp.setConstantPosition(this.satZ, dispType);
        } catch (Exception exc) {
            //System.out.println("Setting track z-position exc=" + exc);
        }
    }

    private void redrawCoverageCircle() {
        try {

            int num = circleDsp.displayableCount();
            for (int i=0; i<num; i++) {
                circleDsp.removeDisplayable(0);
            }

            if (drawCoverageCircle(Math.toRadians(this.latitude), Math.toRadians(this.longitude),
                       this.satelliteAltitude, getAntColor()) != null) {
                drawGroundStation();
                circleDsp.setColor(getAntColor());
                circleDsp.setLineWidth(1f);
                circleDsp.addDisplayable(this.coverageCircle);
                circleDsp.addDisplayable(this.groundStationDsp);
                addDisplayable(circleDsp, FLAG_COLORTABLE);
            }
        } catch (Exception e) {
            System.out.println("redrawCoverageCircle e=" + e);
        }
    }

    private CurveDrawer drawCoverageCircle(double lat, double lon, double satAlt, Color color) {
        if (!(latLonWidget.isLatLonDefined())) return null;

        double earthRadius = AstroConst.R_Earth/1000.0;
        satAlt += earthRadius;
        double pi = Math.PI;
        double SAC = pi/2.0 + Math.toRadians(getAntennaAngle());
        double sinASC = earthRadius * Math.sin(SAC) / satAlt;
        double dist = earthRadius * (Math.PI - SAC - Math.asin(sinASC));
        double rat = dist/earthRadius;

        int npts = 360;
        float[][] latlon = new float[2][npts];
        double cosDist = Math.cos(rat);
        double sinDist = Math.sin(rat);
        double sinLat = Math.sin(lat);
        double cosLat = Math.cos(lat);
        double sinLon = -Math.sin(lon);
        double cosLon = Math.cos(lon);
        for (int i=0; i<npts; i++) {
            double azimuth = Math.toRadians((double)i);
            double cosBear = Math.cos(azimuth);
            double sinBear = Math.sin(azimuth);
            double z = cosDist * sinLat +
                       sinDist * cosLat * cosBear;
            double y = cosLat * cosLon * cosDist +
                       sinDist * (sinLon*sinBear - sinLat*cosLon*cosBear);
            double x = cosLat * sinLon * cosDist -
                       sinDist * (cosLon*sinBear + sinLat*sinLon*cosBear);
            double r = Math.sqrt(x*x + y*y);
            double latRad = Math.atan2(z, r);
            double lonRad = 0.0;
            if (r > 0.0) lonRad = -Math.atan2(x, y);
            latlon[0][i] = (float)Math.toDegrees(latRad);
            latlon[1][i] = (float)Math.toDegrees(lonRad);
        }
        try {
            Gridded2DSet circle = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                               latlon, npts);
            SampledSet[] set = new SampledSet[1];
            set[0] = circle;
            UnionSet uset = new UnionSet(set);
            this.coverageCircle = new CurveDrawer(uset);
            this.coverageCircle.setLineStyle(1);
            this.coverageCircle.setColor(getAntColor());
            this.coverageCircle.setData(uset);
            this.coverageCircle.setDrawingEnabled(false);
        } catch (Exception e) {
            System.out.println("drawCoverageCircle e=" + e);
            return null;
        }
        return this.coverageCircle;
    }

    private int getSizeValue(JSlider slider) {
        int value = slider.getValue();
        if (value < SLIDER_MIN) {
            value = SLIDER_MIN;
        } else if (value > SLIDER_MAX) {
            value = SLIDER_MAX;
        }
        return value;
    }

    public int getFontSize() {
        if (this.fontSize < 1) this.fontSize = defaultSize;
        return this.fontSize;
    }

    public void setFontSizeTextField(int size) {
        size = setFontSize(size);
        try {
            if (this.fontSizeFld != null) {
                this.fontSizeFld.setText(new Integer(size).toString());
            }
        } catch (Exception e) {
            System.out.println("Exception in PolarOrbitTrackControl.setFontSizeTextField e=" + e);
        }
    }

    private void moveFontSizeSlider(int size) {
        size = setFontSize(size);
        try {
            if (this.fontSizeSlider != null) {
                this.fontSizeSlider.setValue(size);
            }
        } catch (Exception e) {
            System.out.println("Exception in PolarOrbitTrackControl.moveFontSizeSlider e=" + e);
        }
    }

    private void setDisplayableTextSize(int size) {
        size = setFontSize(size);
        try {
            float fSize = (float)size/10.0f;
            int num = this.trackDsp.displayableCount()-1;
            TextDisplayable textDsp = null;
            for (int i=num; i>-1; i--) {
                Displayable dsp = this.trackDsp.getDisplayable(i);
                if (dsp instanceof TextDisplayable) {
                    textDsp = (TextDisplayable)dsp;
                    break;
                }
            }
            if (textDsp != null) {
                textDsp.setTextSize(fSize);
            }
        } catch (Exception e) {
            System.out.println("Exception in PolarOrbitTrackControl.setDisplayableTextSize e=" + e);
        }
    }

    public int setFontSize(int size) {
        if (size < 1) size = defaultSize;
        this.fontSize = size;
        return this.fontSize;
    }

    public Color getColor() {
        if (this.color == null) this.color = defaultColor;
        return this.color;
    }

    public void setColor(Color color) {
        if (this.color == null) this.color = defaultColor;
        try {
            this.trackDsp.setColor(color);
            this.color = color;
        } catch (Exception e) {
            System.out.println("Exception in PolarOrbitTrackControl.setColor e=" + e);
        }
    }

    public Color getAntColor() {
        if (this.antColor == null) this.antColor = defaultAntColor;
        return this.antColor;
    }

    public void setAntColor(Color color) {
        if (color == null) color = defaultAntColor;
        try {
            this.antColor = color;
            circleDsp.setColor(color);
        } catch (Exception e) {
            System.out.println("Exception in PolarOrbitTrackControl.setAntColor e=" + e);
        }
    }

    public void setLatitude() {
        this.latitude = latLonWidget.getLat();
        //System.out.println("setLatitude: " + this.latitude);
    }

    public double getLatitude() {
        return this.latitude;
    }

    public void setLongitude() {
        this.longitude = latLonWidget.getLon();
        //System.out.println("setLongitude: " + this.longitude);
    }

    public double getLongitude() {
        return this.longitude;
    }

    public void setAltitude() {
        String str = altitudeFld.getText();
        //System.out.println("\nsetAltitude: str=" + str);
        try {
            Double d = new Double(str);
            this.altitude = d.doubleValue();
        } catch (Exception e) {
            this.altitude = 0.0;
        }
    }

    public void sliderChanged(int sliderValue) {
        setFontSizeTextField(sliderValue);
        setDisplayableTextSize(sliderValue);
    }

    public void setStation(String val) {
        this.station = val.trim();
        //System.out.println("setStation: " + this.station);
    }

    public String getStation() {
        return this.station;
    }

    public void setAntennaAngle(int val) {
        //System.out.println("\nsetAntennaAngle: val=" + val);
        String str = " " + val;
        antennaAngle.setText(str);
        this.angle = val;
    }

    public int getAntennaAngle() {
        //System.out.println("\ngetAntennaAngle:");
        String str = antennaAngle.getText();
        this.angle = new Integer(str.trim()).intValue();
        if (this.angle < defaultAntAngle) this.angle = defaultAntAngle;
        return this.angle;
    }

    private void setSatelliteAltitude(double val) {
        //System.out.println("\nsetSatelliteAltitude: val=" + val);
        this.satelliteAltitude = val;
    }

    private void drawGroundStation() {
        try {
            String str = "+" + getStation();
            this.groundStationDsp = new TextDisplayable(TextType.Generic);
            this.groundStationDsp.setJustification(TextControl.Justification.LEFT);
            this.groundStationDsp.setVerticalJustification(TextControl.Justification.CENTER);
            this.groundStationDsp.setTextSize(0.3f);
            this.groundStationDsp.setColor(getAntColor());
                    
            //addDisplayable(this.groundStationDsp, FLAG_COLORTABLE);
            //circleDsp.addDisplayable(this.groundStationDsp);
            double dlat = getLatitude();
            double dlon = getLongitude();
            RealTuple lonLat =
                new RealTuple(RealTupleType.SpatialEarth2DTuple,
                    new double[] { dlon, dlat });
            Tuple tup = new Tuple(TUPTYPE,
                new Data[] { lonLat, new Text(str)});
            this.groundStationDsp.setData(tup);
        } catch (Exception e) {
            System.out.println("drawGroundStation e=" + e);
        }
    }
        
    public PolarOrbitTrackDataSource getDataSource() {
        DataSourceImpl ds = null;
        List dataSources = getDataSources();
        boolean gotit = false;
        if (!dataSources.isEmpty()) {
            int nsrc = dataSources.size();
            for (int i=0; i<nsrc; i++) {
                ds = (DataSourceImpl)dataSources.get(nsrc-i-1);
                if (ds instanceof PolarOrbitTrackDataSource) {
                    gotit = true;
                    break;
                }
            }
        }
        if (!gotit) return null;
        return (PolarOrbitTrackDataSource)ds;
    }
}
