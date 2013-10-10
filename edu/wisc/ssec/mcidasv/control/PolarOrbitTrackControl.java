/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2013
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

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.Math;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.idv.control.DisplayControlImpl;
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
 * {@link ucar.unidata.idv.control.DisplayControlImpl} with some McIDAS-V
 * specific extensions. Namely parameter sets and support for inverted 
 * parameter defaults.
 */

public class PolarOrbitTrackControl extends DisplayControlImpl implements ActionListener, ItemListener {

    private static final Logger logger = LoggerFactory.getLogger(PolarOrbitTrackControl.class);

    private JLabel satelliteName = new JLabel("");
    private static final JLabel kmLabel = new JLabel("km");
    private JTextField swathWidthFld = null;
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

    /** Property name to get the list or urls */
    public final String PREF_GROUNDSTATIONS = "mcv.groundstations";

    private JComboBox locationComboBox;
    private JTextField locationEditor;
    private JCheckBox jcb;

    private String station = "";
    private TextDisplayable groundStationDsp;

    private static final int DEFAULT_ANTENNA_ANGLE = 5;
    private static final int MAX_ANTENNA_ANGLE = 90;
    private int angle = DEFAULT_ANTENNA_ANGLE;

    private DataChoice dataChoice;

    private JLabel latLabel;
    private JLabel lonLabel;
    private JLabel altLabel;
    private JTextField antennaAngle = new JTextField("" + DEFAULT_ANTENNA_ANGLE, DEFAULT_ANTENNA_ANGLE);

    private ActionListener fontSizeChange;

    /** Font size control */
    private static final int SLIDER_MAX = 10;
    private static final int SLIDER_MIN = 1;
    
    /** Ground station line width control */
    private static final int GS_SLIDER_MAX = 4;
    private static final int GS_SLIDER_MIN = 1;

    private JSlider fontSizeSlider;
    private JSlider gsSizeSlider;
    private JSpinner js = null;
    private JTextField fontSizeFld = new JTextField();

    private CompositeDisplayable trackDsp;
    private CompositeDisplayable timeLabelDsp;
    private CompositeDisplayable swathDsp;
    private CompositeDisplayable circleDsp;
    private TupleType tupleType;
    
    // time label variables
    private static final int DEFAULT_LABEL_INTERVAL = 5;
    private int labelInterval = DEFAULT_LABEL_INTERVAL;

    private int fontSize;
    private int defaultSize = 1;
    private ColorSwatch colorSwatch;
    private Color color;
    private Color defaultColor = Color.GREEN;
    private ColorSwatch antColorSwatch;
    private Color antColor;
    private Color defaultAntColor = Color.MAGENTA;
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

	private static final String SWATH_MODS = "SwathMods";
	private static final String SAVE_CMD = "Save";
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
            logger.error("problem reading swathwidths.xml e=" + e);
        }
    }

    @Override public boolean init(DataChoice dataChoice) 
        throws VisADException, RemoteException 
    {
        // instantiate labels
    	latLabel = new JLabel();
    	lonLabel = new JLabel();
    	altLabel = new JLabel();
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
                    width = dWidth.doubleValue();
                    break;
                }
            }
        }
        try {
            trackDsp = new CompositeDisplayable();
            timeLabelDsp = new CompositeDisplayable();
            swathDsp = new CompositeDisplayable();
            circleDsp = new CompositeDisplayable();
        } catch (Exception e) {
            logger.error("problem creating composite displayable e=" + e);
            return false;
        }
        boolean result = super.init((DataChoice)this.getDataChoices().get(0));

        tupleType = makeTupleType();

        Data data = getData(getDataInstance());
        createTrackDisplay(data, true);
        dataSource = getDataSource();
        try {
            navDsp = getNavigatedDisplay();
            EarthLocation earthLoc = navDsp.getCenterPoint();
            LatLonPoint llp = earthLoc.getLatLonPoint();
            centerLat = llp.getLatitude().getValue();
            centerLon = llp.getLongitude().getValue();
            centerAlt = dataSource.getNearestAltToGroundStation(centerLat, centerLon) / 1000.0;
            EarthLocationTuple elt = new EarthLocationTuple(centerLat, centerLon, centerAlt);
            double[] xyz = navDsp.getSpatialCoordinates((EarthLocation) elt).getValues();
            satZ = xyz[2] / 5.0;
            applyTrackPosition();
        } catch (Exception e) {
            logger.error("get display center e=" + e);
        }

        return result;
    }

    private void createTrackDisplay(Data data, boolean doTrack) {
        try {
            fontSize = getFontSize();
            color = getColor();
            List<String> dts = new ArrayList<String>();
            if (data instanceof Tuple) {
                Data[] dataArr = ((Tuple) data).getComponents();

                int npts = dataArr.length;
                float[][] latlon = new float[2][npts];

                for (int i = 0; i < npts; i++) {
                    Tuple t = (Tuple) dataArr[i];
                    Data[] tupleComps = t.getComponents();

                    LatLonTuple llt = (LatLonTuple)tupleComps[1];
                    double dlat = llt.getLatitude().getValue();
                    double dlon = llt.getLongitude().getValue();

                    if (doTrack) {
                        if ((i % labelInterval) == 0) {
                            String str = ((Text)tupleComps[0]).getValue();
                            logger.info("Adding time for str: " + str);
                            dts.add(str);
                            int indx = str.indexOf(" ") + 1;
                            String subStr = "- " + str.substring(indx, indx+5);
                            TextDisplayable time = new TextDisplayable(getTextType());
                            time.setJustification(TextControl.Justification.LEFT);
                            time.setVerticalJustification(TextControl.Justification.CENTER);
                            time.setColor(color);
                            
                            RealTuple lonLat =
                                new RealTuple(RealTupleType.SpatialEarth2DTuple,
                                    new double[] { dlon, dlat });
                            Tuple tup = new Tuple(getTupleType(),
                                new Data[] { lonLat, new Text(getTextType(), subStr)});
                            time.setData(tup);
                            timeLabelDsp.addDisplayable(time);
                            logger.info("Mod check, i: " + i);
                        }
                    }
                    latlon[0][i] = (float) dlat;
                    latlon[1][i] = (float) dlon;
                }

                if (doTrack) {
                    setDisplayableTextSize(fontSize);
                    Gridded2DSet track = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                               latlon, npts);
                    SampledSet[] set = new SampledSet[1];
                    set[0] = track;
                    UnionSet uset = new UnionSet(set);
                    CurveDrawer trackLines = new CurveDrawer(uset);
                    trackLines.setData(uset);
                    trackDsp.addDisplayable(trackLines);
                    trackLines.setDrawingEnabled(false);

                    trackDsp.setColor(color);
                    trackDsp.setLineWidth(2.0f);

                    addDisplayable(trackDsp, FLAG_COLORTABLE);
                    addDisplayable(timeLabelDsp, FLAG_COLORTABLE);
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

                swathDsp.setColor(color);
                swathDsp.setLineWidth(1.0f);
                addDisplayable(swathDsp, FLAG_COLORTABLE);
            }
        } catch (Exception e) {
            logger.error("getData e=" + e);
        }
        return;
    }

    private float[][][] getSwath(float[][] track) {
        double earthRadius = AstroConst.R_Earth_mean / 1000.0;
        int npt = track[0].length-1;
        float[][][] ret = new float[2][2][npt-1];
        try {
            int indx = 0;
            for (int i = 1; i < npt; i++) {
                double latA = Math.toRadians(track[0][i-1]);
                double lonA = Math.toRadians(track[1][i-1]);

                double latB = Math.toRadians(track[0][i+1]);
                double lonB = Math.toRadians(track[1][i+1]);

                double diffLon = lonB - lonA;
                double bX = Math.cos(latB) * Math.cos(diffLon);
                double bY = Math.cos(latB) * Math.sin(diffLon);
                double xFac = Math.cos(latA) + bX;
                double latC = Math.atan2(Math.sin(latA) + Math.sin(latB), Math.sqrt(xFac * xFac + bY * bY));
                double lonC = lonA + Math.atan2(bY, xFac);

                double bearing = Math.atan2(Math.sin(diffLon) * Math.cos(latB),
                                 Math.cos(latA) * Math.sin(latB) - Math.sin(latA) * Math.cos(latB) * Math.cos(diffLon))
                                 + Math.PI / 2.0;
                double dist = width / 2.0;
                dist /= earthRadius;
                double lat = Math.asin(Math.sin(latC) * Math.cos(dist) +
                                       Math.cos(latC) * Math.sin(dist) * Math.cos(bearing));
                double lon = lonC + Math.atan2(Math.sin(bearing) * Math.sin(dist) * Math.cos(latC),
                                               Math.cos(dist) - Math.sin(latC) * Math.sin(lat));
                float latD = (float)Math.toDegrees(lat);
                float lonD = (float)Math.toDegrees(lon);

                bearing += Math.PI;
                lat = Math.asin(Math.sin(latC) * Math.cos(dist) +
                                       Math.cos(latC) * Math.sin(dist) * Math.cos(bearing));
                lon = lonC + Math.atan2(Math.sin(bearing) * Math.sin(dist) * Math.cos(latC),
                                               Math.cos(dist) - Math.sin(latC) * Math.sin(lat));
                float latE = (float) Math.toDegrees(lat);
                float lonE = (float) Math.toDegrees(lon);

                ret[0][0][indx] = latD;
                ret[0][1][indx] = lonD;

                ret[1][0][indx] = latE;
                ret[1][1][indx] = lonE;
                ++indx;
            }
        } catch (Exception e) {
            logger.error("e=" + e);
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
            logger.error("\nPolarOrbitTrackControl.makeTupleType e=" + e);
            logger.error("    textType=" + getTextType());
        }
        return t;
    }

    public JComponent makeColorBox(Color swatchColor) {
        GuiUtils.ColorSwatch swatch = new GuiUtils.ColorSwatch(swatchColor, "Color") {
            public void userSelectedNewColor(Color c) {
                try {
                    getIdv().showWaitCursor();
                    setColor(c);
                    setBackground(c);
                    getIdv().showNormalCursor();
                } catch (Exception e) {
                    logger.error("\nsetColor e=" + e);
                    setColor(defaultColor);
                }
            }
        };
        return swatch;
    }

    public JComponent makeAntColorBox(Color swatchAntColor) {
        GuiUtils.ColorSwatch swatch = new GuiUtils.ColorSwatch(swatchAntColor, "Color") {
            public void userSelectedNewColor(Color c) {
                try {
                    getIdv().showWaitCursor();
                    setAntColor(c);
                    setBackground(c);
                    getIdv().showNormalCursor();
                } catch (Exception e) {
                    logger.error("\nsetAntColor e=" + e);
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
        fontSizeChange =new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
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
        int size = fontSizeSlider.getValue();
        setFontSize(size);
        fontSizeFld = new JTextField(Integer.toString(size), 3);
        fontSizeFld.addActionListener(fontSizeChange);
        
        // init the ground station line width control slider
        gsSizeSlider = new JSlider();
        gsSizeSlider = GuiUtils.makeSlider(GS_SLIDER_MIN, GS_SLIDER_MAX, defaultSize,
                this, "gsSliderChanged", true);
        gsSizeSlider.setMajorTickSpacing(1);
        gsSizeSlider.setSnapToTicks(true);
        gsSizeSlider.setPaintTicks(true);
        gsSizeSlider.setPaintLabels(true);
        gsSizeSlider.setValue(GS_SLIDER_MIN);
        
        fontSizePanel = new JPanel();
        fontSizePanel.setLayout(new BoxLayout(fontSizePanel, BoxLayout.Y_AXIS));
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jcb = new JCheckBox("Labels On/Off");
        jcb.setSelected(true);
        jcb.addItemListener(this);
        topPanel.add(jcb);
        
        // same row, add label interval spinner
        Integer defaultInterval = new Integer(5); 
        Integer minInterval = new Integer(1);
        Integer maxInterval = new Integer(120); 
        Integer intervalStep = new Integer(1); 
        SpinnerNumberModel snm = 
        		new SpinnerNumberModel(defaultInterval, minInterval, maxInterval, intervalStep);
        JLabel intervalLabel = new JLabel("Label Interval:");
        JLabel intervalUnits = new JLabel("minutes");
        js = new JSpinner(snm);
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(intervalLabel);
        topPanel.add(js);
        topPanel.add(intervalUnits);
        
        fontSizePanel.add(topPanel);
        JPanel botPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botPanel.add(new JLabel("Font Size: "));
        botPanel.add(fontSizeFld);
        botPanel.add(fontSizeSlider);
        fontSizePanel.add(botPanel);

        Color swatchColor = getColor();
        colorSwatch = (GuiUtils.ColorSwatch)makeColorBox(swatchColor);
        
        colorPanel = new JPanel();
        colorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        colorPanel.add(new JLabel("Swath Color: "));
        colorPanel.add(colorSwatch);
        
        JPanel groundStationPanel = makeGroundStationPanel();

        swathWidthPanel = makeSwathWidthPanel();
        
        JPanel outerPanel = new JPanel(new MigLayout());
        // outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
        
        JPanel mainPanel = new JPanel(new MigLayout());
        // mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder(" Swath Width Controls "));
        mainPanel.add(swathWidthPanel, "wrap");
        mainPanel.add(fontSizePanel, "wrap");
        mainPanel.add(colorPanel, "wrap");
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton applySwathMods = new JButton("Apply");
        applySwathMods.setActionCommand(SWATH_MODS);
        applySwathMods.addActionListener(this);
        bottomPanel.add(applySwathMods);
        mainPanel.add(bottomPanel);
        
        outerPanel.add(mainPanel, "wrap");
        outerPanel.add(groundStationPanel, "wrap");
        
        return outerPanel;
    }

    private JPanel makeGroundStationPanel() {
    	
    	JPanel jp = new JPanel(new MigLayout());
        // jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
        jp.setBorder(BorderFactory.createTitledBorder(" Ground Station Controls "));

        locationComboBox = new JComboBox();
        locationComboBox.setEditable(true);
        locationEditor = (JTextField) locationComboBox.getEditor().getEditorComponent();
        locationEditor.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
			@Override
			public void keyTyped(KeyEvent arg0) {
				// TODO Auto-generated method stub
			}
        });

        // Ground Stations are now a natural-order map (alphabetical)
        GroundStations gs = new GroundStations(null);
        stationMap = gs.getGroundStations();
        TreeSet<String> keySet = new TreeSet<String>(stationMap.keySet());

        GuiUtils.setListData(locationComboBox, keySet.toArray());

        locationComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setStation((String) locationComboBox.getSelectedItem());
                try {
                	EarthLocationTuple elt = stationMap.get(station);
                    latLabel.setText(elt.getLatitude().toString());
                    lonLabel.setText(elt.getLongitude().toString());
                	altLabel.setText(elt.getAltitude().toString());
                	setLatitude();
                	setLongitude();
                    int val = getAntennaAngle();
                    setAntennaAngle(val);
                } catch (Exception e) {
                }
                setSatelliteAltitude(dataSource.getNearestAltToGroundStation(latitude, longitude) / 1000.0);
                redrawCoverageCircle();
            }
        });

        // initialize with first Earth Location in our map
        EarthLocationTuple elt = stationMap.get(locationComboBox.getSelectedItem());

        latLabel.setText(elt.getLatitude().toString());
        lonLabel.setText(elt.getLongitude().toString());
        altLabel.setText(elt.getAltitude().toString());

        setLatitude();
        setLongitude();

        antennaAngle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
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
        
        latLonAltPanel.add(new JLabel("Latitude: "));
        latLonAltPanel.add(latLabel);
        latLonAltPanel.add(Box.createHorizontalStrut(5));
        
        latLonAltPanel.add(new JLabel("Longitude: "));
        latLonAltPanel.add(lonLabel);
        latLonAltPanel.add(Box.createHorizontalStrut(5));
        
        latLonAltPanel.add(new JLabel("Altitude: "));
        latLonAltPanel.add(altLabel);
        latLonAltPanel.add(Box.createHorizontalStrut(5));
        latLonAltPanel.add(new JLabel("Antenna Angle: "));
        latLonAltPanel.add(antennaAngle);

        Color swatchAntColor = getAntColor();
        antColorSwatch = (GuiUtils.ColorSwatch)makeAntColorBox(swatchAntColor);
        
        antColorPanel = new JPanel();
        antColorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        antColorPanel.add(new JLabel("Ground Station Color: "));
        antColorPanel.add(antColorSwatch);
        
        antColorPanel.add(Box.createHorizontalStrut(5));
        antColorPanel.add(new JLabel("Ground Station Line Width: "));
        antColorPanel.add(gsSizeSlider);
        
        jp.add(locationPanel, "wrap");
        jp.add(latLonAltPanel, "wrap");
        jp.add(antColorPanel, "wrap");
        
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton applyGroundStationMods = new JButton("Apply");
        bottomRow.add(applyGroundStationMods);
        jp.add(bottomRow);
        
        return jp;
    }

    private JPanel makeSwathWidthPanel() {
        if (dataChoice != null)
            satelliteName = new JLabel(dataChoice.getName());
        Integer isw = new Integer((int) width);
        swathWidthFld = new JTextField(isw.toString(), 5);
        swathWidthFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                changeSwathWidth();
            }
        });

        saveBtn = new JButton("Save");
        saveBtn.setActionCommand(SAVE_CMD);
        saveBtn.addActionListener(this);

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

    	String s = swathWidthFld.getText().trim();
    	int val = -1;
    	try {
    		val = Integer.parseInt(s);
    	} catch (NumberFormatException nfe) {
    		// throw up a dialog to tell user the problem
    		JOptionPane.showMessageDialog(latLonAltPanel, 
    				"Invalid swath width: must be an integer value in km");
    		return;
    	}

    	int earthDiam = (int) ((AstroConst.R_Earth_major * 2) / 1000);
    	if ((val < 1) || (val > earthDiam)) {
    		// throw up a dialog to tell user the problem
    		JOptionPane.showMessageDialog(latLonAltPanel, 
    				"Swath width valid range is " + 1 + 
    				" to " + earthDiam + " (Earth diameter) km");
    		return;
    	} else {
    		setSwathWidth(val);
    		try {
    			removeDisplayable(swathDsp);
    			Data data = getData(getDataInstance());
    			swathDsp = new CompositeDisplayable();
    			createTrackDisplay(data, false);
    		} catch (Exception e) {
    			logger.error("\nproblem redrawing swaths e=" + e);
    		}
    	}
    }

    private void setSwathWidth(double val) {
        width = val;
    }

    /**
     * Apply the map (height) position to the displays
     */
    
    private void applyTrackPosition() {
        try {
            DisplayRealType dispType = navDsp.getDisplayAltitudeType();
            trackDsp.setConstantPosition(satZ, dispType);
            timeLabelDsp.setConstantPosition(satZ, dispType);
        } catch (Exception exc) {
            logger.error("Setting track z-position exc=" + exc);
        }
    }

    private void redrawCoverageCircle() {
        try {

            int num = circleDsp.displayableCount();
            for (int i = 0; i < num; i++) {
                circleDsp.removeDisplayable(0);
            }

            if (drawCoverageCircle(Math.toRadians(latitude), Math.toRadians(longitude),
                       satelliteAltitude, getAntColor()) != null) {
                drawGroundStation();
                circleDsp.setColor(getAntColor());
                circleDsp.addDisplayable(coverageCircle);
                circleDsp.addDisplayable(groundStationDsp);
                addDisplayable(circleDsp, FLAG_COLORTABLE);
            }
        } catch (Exception e) {
            logger.error("redrawCoverageCircle e=" + e);
        }
    }

    private CurveDrawer drawCoverageCircle(double lat, double lon, double satAlt, Color color) {

        /* mean Earth radius in km */
        double earthRadius = AstroConst.R_Earth_mean / 1000.0;
        satAlt += earthRadius;
        double SAC = Math.PI / 2.0 + Math.toRadians(getAntennaAngle());
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
        for (int i = 0; i < npts; i++) {
            double azimuth = Math.toRadians((double)i);
            double cosBear = Math.cos(azimuth);
            double sinBear = Math.sin(azimuth);
            double z = cosDist * sinLat +
                       sinDist * cosLat * cosBear;
            double y = cosLat * cosLon * cosDist +
                       sinDist * (sinLon * sinBear - sinLat * cosLon * cosBear);
            double x = cosLat * sinLon * cosDist -
                       sinDist * (cosLon * sinBear + sinLat * sinLon * cosBear);
            double r = Math.sqrt(x*x + y*y);
            double latRad = Math.atan2(z, r);
            double lonRad = 0.0;
            if (r > 0.0) lonRad = -Math.atan2(x, y);
            latlon[0][i] = (float) Math.toDegrees(latRad);
            latlon[1][i] = (float) Math.toDegrees(lonRad);
        }
        try {
            Gridded2DSet circle = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                               latlon, npts);
            SampledSet[] set = new SampledSet[1];
            set[0] = circle;
            UnionSet uset = new UnionSet(set);
            coverageCircle = new CurveDrawer(uset);
            coverageCircle.setLineStyle(1);
            coverageCircle.setColor(getAntColor());
            coverageCircle.setData(uset);
            coverageCircle.setDrawingEnabled(false);
        } catch (Exception e) {
            logger.error("drawCoverageCircle e=" + e);
            return null;
        }
        return coverageCircle;
    }

    public int getFontSize() {
        if (fontSize < 1) fontSize = defaultSize;
        return fontSize;
    }

    public void setFontSizeTextField(int size) {
        size = setFontSize(size);
        try {
            if (fontSizeFld != null) {
                fontSizeFld.setText(new Integer(size).toString());
            }
        } catch (Exception e) {
            logger.error("Exception in PolarOrbitTrackControl.setFontSizeTextField e=" + e);
        }
    }

    private void moveFontSizeSlider(int size) {
        size = setFontSize(size);
        try {
            if (fontSizeSlider != null) {
                fontSizeSlider.setValue(size);
            }
        } catch (Exception e) {
            logger.error("Exception in PolarOrbitTrackControl.moveFontSizeSlider e=" + e);
        }
    }

    private void setDisplayableTextSize(int size) {
        size = setFontSize(size);
        try {
            // float fSize = (float) size / 10.0f;
        	float fSize = (float) size;
            logger.info("fSize: " + fSize);
//            // XXX TJJ need to swap for time label displayable contents..
//            int num = trackDsp.displayableCount() - 1;
//            TextDisplayable textDsp = null;
//            for (int i = num; i > -1; i--) {
//                Displayable dsp = trackDsp.getDisplayable(i);
//                if (dsp instanceof TextDisplayable) {
//                    textDsp = (TextDisplayable)dsp;
//                    break;
//                }
//            }
//            if (textDsp != null) {
//                textDsp.setTextSize(fSize);
//            }
            int num = timeLabelDsp.displayableCount();
            for (int i = 0; i < num; i++) {
            	Displayable dsp = timeLabelDsp.getDisplayable(i);
            	if (dsp instanceof TextDisplayable) {
            		if (i == 0) {
            			logger.info("ORIG TXT SZ: " + ((TextDisplayable) dsp).getTextSize());
            		}
            		((TextDisplayable) dsp).setTextSize(fSize);
            	}
            }
        } catch (Exception e) {
            logger.error("Exception in PolarOrbitTrackControl.setDisplayableTextSize e=" + e);
        }
    }

    public int setFontSize(int size) {
        if (size < 1) size = defaultSize;
        fontSize = size;
        return fontSize;
    }

    public Color getColor() {
        if (color == null) color = defaultColor;
        return color;
    }

    public void setColor(Color c) {
        if (c == null) c = defaultColor;
        try {
            trackDsp.setColor(c);
            timeLabelDsp.setColor(c);
            swathDsp.setColor(c);
            color = c;
        } catch (Exception e) {
            logger.error("Exception in PolarOrbitTrackControl.setColor e=" + e);
        }
    }

    public Color getAntColor() {
        if (antColor == null) antColor = defaultAntColor;
        return antColor;
    }

    public void setAntColor(Color c) {
        if (c == null) c = defaultAntColor;
        try {
            antColor = c;
            circleDsp.setColor(c);
        } catch (Exception e) {
            logger.error("Exception in PolarOrbitTrackControl.setAntColor e=" + e);
        }
    }

    public void setLatitude() {
    	latitude = Double.parseDouble(latLabel.getText());
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLongitude() {
        longitude = Double.parseDouble(lonLabel.getText());
    }

    public double getLongitude() {
        return longitude;
    }

    public void sliderChanged(int sliderValue) {
        setFontSizeTextField(sliderValue);
        setDisplayableTextSize(sliderValue);
    }
    
    public void gsSliderChanged(int sliderValue) {
        // ground station outline width
    	try {
			circleDsp.setLineWidth(sliderValue);
		// doubtful these could ever happen, but required	
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (VisADException e) {
			e.printStackTrace();
		}
    }

    public void setStation(String val) {
        station = val.trim();
    }

    public String getStation() {
        return station;
    }

    public void setAntennaAngle(int val) {
    	if ((val < DEFAULT_ANTENNA_ANGLE) || (val > MAX_ANTENNA_ANGLE)) {
        	// throw up a dialog to tell user the problem
        	JOptionPane.showMessageDialog(latLonAltPanel, 
        			"Antenna angle valid range is " + DEFAULT_ANTENNA_ANGLE + 
        			" to " + MAX_ANTENNA_ANGLE + " degrees");
    	} else {
    		String str = " " + val;
    		antennaAngle.setText(str);
    		angle = val;
    	}
    }

    public int getAntennaAngle() {
        String str = antennaAngle.getText();
        angle = new Integer(str.trim()).intValue();
        if (angle < DEFAULT_ANTENNA_ANGLE) angle = DEFAULT_ANTENNA_ANGLE;
        return angle;
    }

    private void setSatelliteAltitude(double val) {
        satelliteAltitude = val;
    }

    private void drawGroundStation() {
        try {
            String str = "+" + getStation();
            groundStationDsp = new TextDisplayable(getTextType());
            groundStationDsp.setJustification(TextControl.Justification.LEFT);
            groundStationDsp.setVerticalJustification(TextControl.Justification.CENTER);
            float tSize = (float) getFontSize() / 10.0f;
            groundStationDsp.setTextSize(tSize);
            groundStationDsp.setColor(getAntColor());
                    
            double dlat = getLatitude();
            double dlon = getLongitude();
            RealTuple lonLat =
                new RealTuple(RealTupleType.SpatialEarth2DTuple,
                    new double[] { dlon, dlat });
            Tuple tup = new Tuple(getTupleType(),
                new Data[] { lonLat, new Text(getTextType(), str)});
            groundStationDsp.setData(tup);
        } catch (Exception e) {
            logger.error("drawGroundStation e=" + e);
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
        return (PolarOrbitTrackDataSource) ds;
    }

    private TextType getTextType() {
        if (textType == null) {
            String dispName = getDisplayName();
            setDisplayName(getLongParamName() + " " + dispName);
            try {
                String longName = getLongParamName().replaceAll(" ", "");
                textType = new TextType(longName);
            } catch (Exception e) {
                textType = TextType.Generic;
            }
        }
        return textType;
    }

    private TupleType getTupleType() {
        return tupleType;
    }
    
    /**
     * Deal with action events
     *
     * @param  ae the ActionEvent fired when the user applys changes
     */

    public void actionPerformed(ActionEvent ae) {
    	
    	// swath-related changes
    	if (SWATH_MODS.equals(ae.getActionCommand())) {
    		logger.info("Apply Swath Mods...");
    		
    		// see if label interval has changed
    		SpinnerNumberModel snm = (SpinnerNumberModel) (js.getModel());
    		int tmpLabelInterval = ((Integer) snm.getValue()).intValue();
    		if (tmpLabelInterval != labelInterval) {
    			logger.info("Label interval change from: " + labelInterval +
    					" to: " + tmpLabelInterval);
    			labelInterval = tmpLabelInterval;
    			try {
    				// remove the current set of labels
    				int numLabels = timeLabelDsp.displayableCount();
    				for (int i = 0; i < numLabels; i++) {
    					timeLabelDsp.removeDisplayable(0);
    				}
    				// get the currently loaded data
    				Data data = getData(getDataInstance());
					if (data instanceof Tuple) {
		                Data[] dataArr = ((Tuple) data).getComponents();

		                int npts = dataArr.length;

		                for (int i = 0; i < npts; i++) {
		                    Tuple t = (Tuple) dataArr[i];
		                    Data[] tupleComps = t.getComponents();

		                    LatLonTuple llt = (LatLonTuple)tupleComps[1];
		                    double dlat = llt.getLatitude().getValue();
		                    double dlon = llt.getLongitude().getValue();

	                        if ((i % labelInterval) == 0) {
	                            String str = ((Text)tupleComps[0]).getValue();
	                            logger.info("Adding time for str: " + str);
	                            int indx = str.indexOf(" ") + 1;
	                            String subStr = "- " + str.substring(indx, indx+5);
	                            TextDisplayable time = new TextDisplayable(getTextType());
	                            time.setJustification(TextControl.Justification.LEFT);
	                            time.setVerticalJustification(TextControl.Justification.CENTER);
	                            time.setColor(color);
	                            
	                            RealTuple lonLat =
	                                new RealTuple(RealTupleType.SpatialEarth2DTuple,
	                                    new double[] { dlon, dlat });
	                            Tuple tup = new Tuple(getTupleType(),
	                                new Data[] { lonLat, new Text(getTextType(), subStr)});
	                            time.setData(tup);
	                            timeLabelDsp.addDisplayable(time);
	                        }
		                }
		            }
				} catch (RemoteException re) {
					re.printStackTrace();
				} catch (VisADException vade) {
					vade.printStackTrace();
				}
    		}
    		
    		updateDisplayList();
    		return;
    	}
    	
    	// Save button - not yet implemented
    	if (SAVE_CMD.equals(ae.getActionCommand())) {
    		// throw up a dialog to tell user
    		JOptionPane.showMessageDialog(latLonAltPanel, 
    				"Save option is not yet implemented.");
    		return;
    	}

    }
    
    public void itemStateChanged(ItemEvent e) {

    	// only one checkbox, either checked or not!
        try {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				logger.info("Labels OFF...");
					timeLabelDsp.setVisible(false);
					updateDisplayList();
			} else {
				logger.info("Labels ON...");
					timeLabelDsp.setVisible(true);
					updateDisplayList();
			}
		} catch (RemoteException re) {
			re.printStackTrace();
		} catch (VisADException vade) {
			vade.printStackTrace();
		}

    }
}
