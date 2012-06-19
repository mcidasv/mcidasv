/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2012
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

import edu.wisc.ssec.mcidasv.McIdasPreferenceManager;

import edu.wisc.ssec.mcidasv.data.GroundStations;
import edu.wisc.ssec.mcidasv.data.PolarOrbitTrackDataSource;
import edu.wisc.ssec.mcidasv.data.adde.sgp4.AstroConst;
import edu.wisc.ssec.mcidasv.data.hydra.CurveDrawer;
import edu.wisc.ssec.mcidasv.util.XmlUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
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
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.GuiUtils.ColorSwatch;
import ucar.unidata.util.IOUtil;
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
    private JLabel satelliteName = new JLabel("           ");
    private static final JLabel kmLabel = new JLabel(" km");
    private JTextField swathWidthFld = new JTextField(" ", 5);
    private JPanel swathWidthPanel;
    private JButton saveBtn;
    
    // Ground Station hashmap
    private HashMap<String, EarthLocationTuple> stationMap = null;

    private double latitude;
    private double longitude;
    private JPanel fontSizePanel;
    private JPanel colorPanel;
    private JPanel antColorPanel;
    private JPanel locationPanel;
    private JPanel latLonAltPanel;

    private int locationIndex = -1;

    /** Property name to get the list or urls */
    public final String PREF_GROUNDSTATIONS = "mcv.groundstations";

    private JComboBox locationComboBox;
    private JTextField locationEditor;

    private String station = "";
    private TextDisplayable groundStationDsp;

    private static final int defaultAntAngle = 5;
    private int angle = defaultAntAngle;

    private DataChoice dataChoice;

    /** Input for lat/lon center point */
    protected LatLonWidget latLonWidget = new LatLonWidget();

    private JTextField latFld;
    private JTextField lonFld;
    private JTextField altitudeFld = new JTextField(" ", 5);
    private JTextField antennaAngle = new JTextField(" 5", 5);

    private ActionListener fontSizeChange;
    private FocusListener fontSizeFocusChange;

    /** Font size control */
    private static final int SLIDER_MAX = 10;
    private static final int SLIDER_MIN = 1;

    private JSlider fontSizeSlider;
    private JTextField fontSizeFld = new JTextField();

    private CompositeDisplayable trackDsp;
    private CompositeDisplayable swathDsp;
    private CompositeDisplayable circleDsp;
    private TupleType tupleType;

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
    private TextType textType = null;
    private double width = 0.0;

    /** Path to the McV swathwidths.xml */
    private static final String SWATH_WIDTHS = "/edu/wisc/ssec/mcidasv/resources/swathwidths.xml";
    private static final String TAG_SATELLITE = "satellite";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_WIDTH = "width";
    private Element root = null;

    public PolarOrbitTrackControl() {
        super();
        logger.trace("created new tlecontrol={}", Integer.toHexString(hashCode()));
        setAttributeFlags(FLAG_COLORTABLE);
        try {
            final String xml =
                IOUtil.readContents(SWATH_WIDTHS, McIdasPreferenceManager.class);
            root = XmlUtil.getRoot(xml);
        } catch (Exception e) {
            System.out.println("problem reading swathwidths.xml e=" + e);
        }
    }

    @Override public boolean init(DataChoice dataChoice) 
        throws VisADException, RemoteException 
    {
        this.dataChoice = dataChoice;
        String choiceName = dataChoice.getName();
        NodeList nodeList = root.getElementsByTagName(TAG_SATELLITE);
        int num = nodeList.getLength();
        if (num > 0) {
            for (int i=0; i<num; i++) {
                Element n =(Element)(nodeList.item(i));
                String satName = n.getAttribute(ATTR_NAME);
                if (satName.equals(choiceName)) {
                    String strWidth = n.getAttribute(ATTR_WIDTH);
                    if (strWidth.isEmpty()) strWidth = "0";
                    Double dWidth = new Double(strWidth);
                    this.width = dWidth.doubleValue();
                    break;
                }
            }
        }
        try {
            trackDsp = new CompositeDisplayable();
            swathDsp = new CompositeDisplayable();
            circleDsp = new CompositeDisplayable();
        } catch (Exception e) {
            System.out.println("problem creating composite displayable e=" + e);
            return false;
        }
        boolean result = super.init((DataChoice)this.getDataChoices().get(0));

        this.tupleType = makeTupleType();

        Data data = getData(getDataInstance());
        createTrackDisplay(data, true);
        this.dataSource = getDataSource();
        try {
            navDsp = getNavigatedDisplay();
            EarthLocation earthLoc = navDsp.getCenterPoint();
            LatLonPoint llp = earthLoc.getLatLonPoint();
            centerLat = llp.getLatitude().getValue();
            centerLon = llp.getLongitude().getValue();
            centerAlt = dataSource.getNearestAltToGroundStation(centerLat, centerLon) / 1000.0;
            EarthLocationTuple elt = new EarthLocationTuple(centerLat, centerLon, centerAlt);
            double[] xyz = navDsp.getSpatialCoordinates((EarthLocation)elt).getValues();
            satZ = xyz[2] / 5.0;
            applyTrackPosition();
        } catch (Exception e) {
            System.out.println("get display center e=" + e);
        }

        return result;
    }

    private void createTrackDisplay(Data data, boolean doTrack) {
        try {
            this.fontSize = getFontSize();
            this.color = getColor();
            List<String> dts = new ArrayList();
            if (data instanceof Tuple) {
                Data[] dataArr = ((Tuple)data).getComponents();

                int npts = dataArr.length;
                float[][] latlon = new float[2][npts];

                for (int i=0; i<npts; i++) {
                    Tuple t = (Tuple)dataArr[i];
                    Data[] tupleComps = t.getComponents();

                    LatLonTuple llt = (LatLonTuple)tupleComps[1];
                    double dlat = llt.getLatitude().getValue();
                    double dlon = llt.getLongitude().getValue();

                    if (doTrack) {
                        String str = ((Text)tupleComps[0]).getValue();
                        dts.add(str);
                        int indx = str.indexOf(" ") + 1;
                        String subStr = "- " + str.substring(indx, indx+5);
                        TextDisplayable time = new TextDisplayable(getTextType());
                        time.setJustification(TextControl.Justification.LEFT);
                        time.setVerticalJustification(TextControl.Justification.CENTER);
                        time.setColor(this.color);
 
                        RealTuple lonLat =
                            new RealTuple(RealTupleType.SpatialEarth2DTuple,
                                new double[] { dlon, dlat });
                        if ((i % 5) == 0) {
                            Tuple tup = new Tuple(getTupleType(),
                                new Data[] { lonLat, new Text(getTextType(), subStr)});
                            time.setData(tup);
                            trackDsp.addDisplayable(time);
                        }
                    }
                    float lat = (float)dlat;
                    float lon = (float)dlon;
                    latlon[0][i] = lat;
                    latlon[1][i] = lon;
                }

                if (doTrack) {
                    setDisplayableTextSize(this.fontSize);
                    Gridded2DSet track = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                               latlon, npts);
                    SampledSet[] set = new SampledSet[1];
                    set[0] = track;
                    UnionSet uset = new UnionSet(set);
                    CurveDrawer trackLines = new CurveDrawer(uset);
                    trackLines.setData(uset);
                    trackDsp.addDisplayable(trackLines);
                    trackLines.setDrawingEnabled(false);

                    trackDsp.setColor(this.color);
                    trackDsp.setLineWidth(2.0f);

                    addDisplayable(trackDsp, FLAG_COLORTABLE);
                }

                float[][][] crv = getSwath(latlon);
                int npt = crv[0][0].length;
                float[][] leftC = new float[2][npt];
                float[][] rightC = new float[2][npt];
                for (int i=0; i<npt; i++) {
                    leftC[0][i] = crv[0][0][i];
                    leftC[1][i] = crv[0][1][i];
                    rightC[0][i] = crv[1][0][i];
                    rightC[1][i] = crv[1][1][i];
                }
                Gridded2DSet left = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                           leftC, npt);
                SampledSet[] lSet = new SampledSet[1];
                lSet[0] = left;
                UnionSet lUSet = new UnionSet(lSet);
                CurveDrawer leftLines = new CurveDrawer(lUSet);
                leftLines.setLineStyle(1);
                leftLines.setData(lUSet);
                swathDsp.addDisplayable(leftLines);
                leftLines.setDrawingEnabled(false);

                Gridded2DSet right = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                           rightC, npt);
                SampledSet[] rSet = new SampledSet[1];
                rSet[0] = right;
                UnionSet rUSet = new UnionSet(rSet);
                CurveDrawer rightLines = new CurveDrawer(rUSet);
                rightLines.setLineStyle(1);
                rightLines.setData(rUSet);
                swathDsp.addDisplayable(rightLines);
                rightLines.setDrawingEnabled(false);

                swathDsp.setColor(this.color);
                swathDsp.setLineWidth(1.0f);
                addDisplayable(swathDsp, FLAG_COLORTABLE);
            }
        } catch (Exception e) {
            System.out.println("getData e=" + e);
        }
        return;
    }

    private float[][][] getSwath(float[][] track) {
        double earthRadius = AstroConst.R_Earth_mean / 1000.0;
        int npt = track[0].length-1;
        float[][][] ret = new float[2][2][npt-1];
        try {
            int indx = 0;
            for (int i=1; i<npt; i++) {
                double latA = Math.toRadians(track[0][i-1]);
                double lonA = Math.toRadians(track[1][i-1]);

                double latB = Math.toRadians(track[0][i+1]);
                double lonB = Math.toRadians(track[1][i+1]);

                double diffLon = lonB - lonA;
                double bX = Math.cos(latB) * Math.cos(diffLon);
                double bY = Math.cos(latB) * Math.sin(diffLon);
                double xFac = Math.cos(latA)+bX;
                double latC = Math.atan2(Math.sin(latA)+Math.sin(latB), Math.sqrt(xFac*xFac + bY*bY));
                double lonC = lonA + Math.atan2(bY, xFac);

                double bearing = Math.atan2(Math.sin(diffLon)*Math.cos(latB),
                                 Math.cos(latA)*Math.sin(latB)-Math.sin(latA)*Math.cos(latB)*Math.cos(diffLon))
                                 + Math.PI/2.0;
                double dist = this.width/2.0;
                dist /= earthRadius;
                double lat = Math.asin(Math.sin(latC)*Math.cos(dist) +
                                       Math.cos(latC)*Math.sin(dist)*Math.cos(bearing));
                double lon = lonC + Math.atan2(Math.sin(bearing)*Math.sin(dist)*Math.cos(latC),
                                               Math.cos(dist)-Math.sin(latC)*Math.sin(lat));
                float latD = (float)Math.toDegrees(lat);
                float lonD = (float)Math.toDegrees(lon);

                bearing += Math.PI;
                lat = Math.asin(Math.sin(latC)*Math.cos(dist) +
                                       Math.cos(latC)*Math.sin(dist)*Math.cos(bearing));
                lon = lonC + Math.atan2(Math.sin(bearing)*Math.sin(dist)*Math.cos(latC),
                                               Math.cos(dist)-Math.sin(latC)*Math.sin(lat));
                float latE = (float)Math.toDegrees(lat);
                float lonE = (float)Math.toDegrees(lon);

                ret[0][0][indx] = latD;
                ret[0][1][indx] = lonD;

                ret[1][0][indx] = latE;
                ret[1][1][indx] = lonE;
                ++indx;
            }
        } catch (Exception e) {
            System.out.println("e=" + e);
            return null;
        }
        return ret;
    }

    private TupleType makeTupleType() {
        TupleType t = null;
        try {
            t = new TupleType(new MathType[] {RealTupleType.SpatialEarth2DTuple,
                                              getTextType()});
        } catch (Exception e) {
            System.out.println("\nPolarOrbitTrackControl.makeTupleType e=" + e);
            System.out.println("    textType=" + getTextType());
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

        fontSizeSlider = GuiUtils.makeSlider(SLIDER_MIN, SLIDER_MAX, defaultSize,
                                     this, "sliderChanged", true);
        fontSizeSlider.setMajorTickSpacing(1);
        fontSizeSlider.setSnapToTicks(true);
        fontSizeSlider.setPaintTicks(true);
        fontSizeSlider.setPaintLabels(true);
        int size = getSizeValue(fontSizeSlider);
        setFontSize(size);
        this.fontSizeFld = new JTextField(Integer.toString(size), 3);
        this.fontSizeFld.addFocusListener(this.fontSizeFocusChange);
        this.fontSizeFld.addActionListener(this.fontSizeChange);
        
        fontSizePanel = new JPanel();
        fontSizePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        fontSizePanel.add(new JLabel("Font Size: "));
        fontSizePanel.add(fontSizeFld);
        fontSizePanel.add(fontSizeSlider);

        Color swatchColor = getColor();
        colorSwatch = (GuiUtils.ColorSwatch)makeColorBox(swatchColor);
        
        colorPanel = new JPanel();
        colorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        colorPanel.add(new JLabel("Set Color: "));
        colorPanel.add(colorSwatch);
        
        JPanel groundStationPanel = makeGroundStationPanel();

        swathWidthPanel = makeSwathWidthPanel();
        
        JPanel outerPanel = new JPanel(new BorderLayout());
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(swathWidthPanel);
        mainPanel.add(fontSizePanel);
        mainPanel.add(colorPanel);
        mainPanel.add(locationPanel);
        mainPanel.add(groundStationPanel);
        mainPanel.add(latLonAltPanel);
        mainPanel.add(antColorPanel);
        
        outerPanel.add(mainPanel, BorderLayout.NORTH);
        
        return outerPanel;
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

        // Ground Stations are now a natural-order map (alphabetical)
        GroundStations gs = new GroundStations(null);
        // stations = gs.getStations();
        stationMap = gs.getGroundStations();
        TreeSet<String> keySet = new TreeSet<String>(stationMap.keySet());

        GuiUtils.setListData(locationComboBox, keySet.toArray());

        locationComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setStation((String) locationComboBox.getSelectedItem());
                locationIndex = locationComboBox.getSelectedIndex();
                if (locationIndex < 0) {
                	// TJJ will clean up further next rev, make sure never happens
                } else {
                    try {
                    	EarthLocationTuple elt = stationMap.get(station);
                    	latLonWidget.setLat(elt.getLatitude().getValue());
                    	latLonWidget.setLon(elt.getLongitude().getValue());
                    	altitudeFld.setText(elt.getAltitude().toString());
                    	setLatitude();
                    	setLongitude();
                    	
                        int val = getAntennaAngle();
                        setAntennaAngle(val);
                    } catch (Exception e) {
                    }
                }
                setSatelliteAltitude(dataSource.getNearestAltToGroundStation(latitude, longitude) / 1000.0);
                redrawCoverageCircle();
            }
        });

        // initialize with first Earth Location in our map
        EarthLocationTuple elt = stationMap.get(locationComboBox.getSelectedItem());
        latFld = latLonWidget.getLatField();
        lonFld = latLonWidget.getLonField();

        altitudeFld = new JTextField(elt.getAltitude().toString(), 5);

        latLonWidget.setLat(elt.getLatitude().getValue());
        setLatitude();

        latLonWidget.setLon(elt.getLongitude().getValue());
        setLongitude();

        ActionListener latLonListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setLatitude();
                setLongitude();
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
        
        locationPanel = new JPanel();
        locationPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        locationPanel.add(new JLabel("Ground Station:"));
        locationPanel.add(locationComboBox);
        
        latLonAltPanel = new JPanel();
        latLonAltPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        latLonAltPanel.add(latLonWidget);
        latLonAltPanel.add(new JLabel("Altitude: "));
        latLonAltPanel.add(altitudeFld);
        latLonAltPanel.add(new JLabel("Antenna Angle: "));
        latLonAltPanel.add(antennaAngle);

        Color swatchAntColor = getAntColor();
        antColorSwatch = (GuiUtils.ColorSwatch)makeAntColorBox(swatchAntColor);
        
        antColorPanel = new JPanel();
        antColorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        antColorPanel.add(new JLabel("Set Color: "));
        antColorPanel.add(antColorSwatch);
        
        return latLonAltPanel;
    }

    private JPanel makeSwathWidthPanel() {
        if (this.dataChoice != null)
            satelliteName = new JLabel(this.dataChoice.getName());
        Double dWidth = new Double(this.width);
        swathWidthFld = new JTextField(dWidth.toString(), 6);
        swathWidthFld.setHorizontalAlignment(JTextField.CENTER);
        swathWidthFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                changeSwathWidth();
            }
        });
        swathWidthFld.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent fe) {
            }
            public void focusLost(FocusEvent fe) {
                changeSwathWidth();
            }
        });

        saveBtn = new JButton("Save");

        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jp.add(new JLabel("Satellite: "));
        jp.add(satelliteName);
        jp.add(Box.createHorizontalStrut(5));
        jp.add(new JLabel("Swath Width: "));
        jp.add(swathWidthFld);
        jp.add(kmLabel);
        jp.add(Box.createHorizontalStrut(5));
        jp.add(saveBtn);
        
        return jp;
    }

    private void changeSwathWidth() {
        String str = swathWidthFld.getText();
        Double dVal = new Double(str.trim());
        double val = dVal.doubleValue();
        setSwathWidth(val);
        try {
            removeDisplayable(swathDsp);
            Data data = getData(getDataInstance());
            swathDsp = new CompositeDisplayable();
            createTrackDisplay(data, false);
        } catch (Exception e) {
            System.out.println("\nproblem redrawing swaths e=" + e);
        }
    }

    private void setSwathWidth(double val) {
        this.width = val;
    }

    /**
     * Apply the map (height) position to the displays
     */
    
    private void applyTrackPosition() {
        try {
            DisplayRealType dispType = navDsp.getDisplayAltitudeType();
            trackDsp.setConstantPosition(this.satZ, dispType);
        } catch (Exception exc) {
            System.out.println("Setting track z-position exc=" + exc);
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
        /* mean earthRadius in km */
        double earthRadius = AstroConst.R_Earth_mean / 1000.0;
        satAlt += earthRadius;
        double pi = Math.PI;
        double SAC = pi / 2.0 + Math.toRadians(getAntennaAngle());
        double sinASC = earthRadius * Math.sin(SAC) / satAlt;
        double dist = earthRadius * (Math.PI - SAC - Math.asin(sinASC));
        double rat = dist / earthRadius;

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
            if (fontSizeFld != null) {
                fontSizeFld.setText(new Integer(size).toString());
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
            float fSize = (float) size / 10.0f;
            int num = trackDsp.displayableCount() - 1;
            TextDisplayable textDsp = null;
            for (int i=num; i > -1; i--) {
                Displayable dsp = trackDsp.getDisplayable(i);
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
            trackDsp.setColor(color);
            this.swathDsp.setColor(color);
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
        latitude = latLonWidget.getLat();
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLongitude() {
        longitude = latLonWidget.getLon();
    }

    public double getLongitude() {
        return longitude;
    }

    public void sliderChanged(int sliderValue) {
        setFontSizeTextField(sliderValue);
        setDisplayableTextSize(sliderValue);
    }

    public void setStation(String val) {
        station = val.trim();
    }

    public String getStation() {
        return station;
    }

    public void setAntennaAngle(int val) {
        String str = " " + val;
        antennaAngle.setText(str);
        this.angle = val;
    }

    public int getAntennaAngle() {
        String str = antennaAngle.getText();
        this.angle = new Integer(str.trim()).intValue();
        if (this.angle < defaultAntAngle) this.angle = defaultAntAngle;
        return this.angle;
    }

    private void setSatelliteAltitude(double val) {
        this.satelliteAltitude = val;
    }

    private void drawGroundStation() {
        try {
            String str = "+" + getStation();
            this.groundStationDsp = new TextDisplayable(getTextType());
            this.groundStationDsp.setJustification(TextControl.Justification.LEFT);
            this.groundStationDsp.setVerticalJustification(TextControl.Justification.CENTER);
            float tSize = (float) getFontSize() / 10.0f;
            this.groundStationDsp.setTextSize(tSize);
            this.groundStationDsp.setColor(getAntColor());
                    
            double dlat = getLatitude();
            double dlon = getLongitude();
            RealTuple lonLat =
                new RealTuple(RealTupleType.SpatialEarth2DTuple,
                    new double[] { dlon, dlat });
            Tuple tup = new Tuple(getTupleType(),
                new Data[] { lonLat, new Text(getTextType(), str)});
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
            for (int i = 0; i < nsrc; i++) {
                ds = (DataSourceImpl) dataSources.get(nsrc-i-1);
                if (ds instanceof PolarOrbitTrackDataSource) {
                    gotit = true;
                    break;
                }
            }
        }
        if (!gotit) return null;
        return (PolarOrbitTrackDataSource)ds;
    }

    private TextType getTextType() {
        if (this.textType == null) {
            String dispName = getDisplayName();
            setDisplayName(getLongParamName() + " " + dispName);
            try {
                String longName = getLongParamName().replaceAll(" ", "");
                this.textType = new TextType(longName);
            } catch (Exception e) {
                this.textType = TextType.Generic;
            }
        }
        return this.textType;
    }

    private TupleType getTupleType() {
        return this.tupleType;
    }
}
