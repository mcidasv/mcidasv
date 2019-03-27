/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2019
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
import java.awt.event.ItemListener;
import java.lang.Math;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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

import name.gano.astro.AstroConst;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSourceImpl;
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
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonTuple;
import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;

/**
 * {@link ucar.unidata.idv.control.DisplayControlImpl} with some McIDAS-V
 * specific extensions. Namely parameter sets and support for inverted 
 * parameter defaults.
 */

public class PolarOrbitTrackControl extends DisplayControlImpl {

    private static final Logger logger = LoggerFactory.getLogger(PolarOrbitTrackControl.class);

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

    private JComboBox<String> jcbTrackLineStyle = new JComboBox<String>(Constants.lineStyles);
    private JComboBox<String> jcbEdgeLineStyle = new JComboBox<String>(Constants.lineStyles);
    private JComboBox<String> jcbStationLineStyle = new JComboBox<String>(Constants.lineStyles);
    private JCheckBox jcbLabels;
    private JCheckBox jcbSwathEdges;
    
    // names to distinguish checkbox event sources
    private static final String CHECKBOX_LABELS = "CHECKBOX_LABELS";
    private static final String CHECKBOX_SWATH_EDGES = "CHECKBOX_SWATH_EDGES";

    private String station = "";

    private static final int SWATH_WIDTH_MIN = 1;
    // swath width not applicable, e.g. GEO sensor
    private static final String SWATH_NA = "N/A";
    // TJJ Feb 2014 - need to determine max of any sensor. VIIRS is over 3000 km
    private static final int SWATH_WIDTH_MAX = 4000;

    private static final int MAX_ANTENNA_ANGLE = 90;
    private int curAngle = GroundStation.DEFAULT_ANTENNA_ANGLE;
    private static final double LABEL_DISTANCE_THRESHOLD = 2.5d;

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

    /** the font selectors, Orbit Track (ot) and Ground Station (gs) */
    private FontSelector otFontSelector;
    private Font otCurFont = FontSelector.DEFAULT_FONT;
    private FontSelector gsFontSelector;
    
    // line width combo boxes, Station: Ground Station, SC: Swath Center, SE: Swath Edge
    private JComboBox<String> jcbStationLineWidth;
    private JComboBox<String> jcbSCLineWidth;
    private JComboBox<String> jcbSELineWidth;
    private JSpinner js = null;

    private CompositeDisplayable trackDsp;
    private CompositeDisplayable timeLabelDsp;
    private CompositeDisplayable swathEdgeDsp;
    
    // time label variables
    private static final int DEFAULT_LABEL_INTERVAL = 5;
    private int labelInterval = DEFAULT_LABEL_INTERVAL;

    private ColorSwatchComponent colorSwatch;

    private static final Color DEFAULT_COLOR = Color.GREEN;
    private Color curSwathColor = DEFAULT_COLOR;
    private Color prvSwathColor = null;
    
    private ColorSwatchComponent antColorSwatch;
    private Color antColor;
    private Color defaultAntColor = Color.MAGENTA;
    private PolarOrbitTrackDataSource dataSource;

    private double satelliteAltitude = 0.0;

    private double trackZ = 0.0d;
    private double gsZ = 0.0d;
    private NavigatedDisplay navDsp = null;
    private TextType otTextType = null;
    private long ttCounter = 0;
    private double curWidth = 0.0d;
    private double prvWidth = 0.0d;

    private int prvTrackLineStyle = -1;
    private int prvEdgeLineStyle = -1;
    private int curTrackLineStyle = -1;
    private int curEdgeLineStyle = -1;
    private static final float FONT_SCALE_FACTOR = 12.0f;
    
    // line width for drawing track center and swath edges
    private float prvSwathCenterWidth = 2.0f;
    private float curSwathCenterWidth = 2.0f;
    private float prvSwathEdgeWidth = 1.0f;
    private float curSwathEdgeWidth = 1.0f;

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

	// Used in showProperties override to help decide if we need to redraw
	Hashtable<String, Object> oldProps = null;

    private Element root = null;
    
    // initial scale for labeling 
    float scale = 1.0f;

    // we've been initialized (we force TrivialMapProjection at init)
    private boolean initialized = false;

    public PolarOrbitTrackControl() {
        super();
        logger.trace("created new PolarOrbitTrackControl...");
        setAttributeFlags(FLAG_COLORTABLE);
        try {
            final String xml =
                IOUtil.readContents(SWATH_WIDTHS, McIdasPreferenceManager.class);
            root = XmlUtil.getRoot(xml);
            scale = getViewManager().getMaster().getDisplayScale();
        } catch (Exception e) {
            logger.error("problem reading swathwidths.xml");
            e.printStackTrace();
        }
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
    		// last check, is this label already used?
    		int numPlotted = jcbStationsPlotted.getItemCount();
    		for (int i = 0; i < numPlotted; i++) {
    			GroundStation gs = (GroundStation) jcbStationsPlotted.getItemAt(i);
    			if ((gs.getName() != null) && gs.getName().equals(station)) {
                	JOptionPane.showMessageDialog(null, 
                		"A station with this label has already been plotted: " + station);
                    return;
    			}
    		}
    		
    		// if we made it this far, fields are valid, we can create a custom ground station
    		// create new earth location, add it to stations plotted, set index, 

			// make an Earth location
			double dAlt = dataSource.getNearestAltToGroundStation(latitude, longitude) / 1000.0;
			EarthLocationTuple elt = null;
			try {
				elt = new EarthLocationTuple(fLat, fLon, dAlt);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (VisADException e) {
				e.printStackTrace();
			}

			GroundStation gs = new GroundStation(labStr, elt, gsFontSelector.getFont());
			try {
                gs.getTd().setColor(antColorSwatch.getColor());
                gs.getCd().setColor(antColorSwatch.getColor());
            } catch (RemoteException | VisADException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
			
			gs.setAntennaAngle(curAngle);
			jcbStationsPlotted.addItem(gs);
			jcbStationsPlotted.setSelectedItem(gs);
			addGroundStation(gs);
			updateDisplayList();
    		return;
    	}
    	
    	// user trying to add a new ground station to those plotted on display
    	if (STATION_ADD.equals(ae.getActionCommand())) {
    		logger.debug("Add Station...");
    		GroundStation station = (GroundStation) locationComboBox.getSelectedItem();
    		boolean alreadyPlotted = false;
    		int numPlotted = jcbStationsPlotted.getItemCount();
    		for (int i = 0; i < numPlotted; i++) {
    			GroundStation gs = (GroundStation) jcbStationsPlotted.getItemAt(i);
    			if ((gs.getName() != null) && gs.getName().equals(station.getName())) {
    				alreadyPlotted = true;
    				break;
    			}
    		}
    		if (alreadyPlotted) {
            	JOptionPane.showMessageDialog(null, 
    			"Station already plotted on display: " + station);
            	return;
    		} else {
                addGroundStation(station);
    			jcbStationsPlotted.addItem(station);
    			jcbStationsPlotted.setSelectedItem(station);

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
                gsFontSelector.setFont(gs.getTd().getFont());
                antColorSwatch.setBackground(gs.getCd().getColor());
                jcbStationLineStyle.setSelectedIndex(gs.getCd().getLineStyle());
                jcbStationLineWidth.setSelectedIndex((int) (gs.getCd().getLineWidth() - 1)); 
                antennaAngle.setText("" + gs.getAntennaAngle());
                curAngle = gs.getAntennaAngle();
                latLabel.setText("" + gs.getElt().getLatitude().getValue());
                lonLabel.setText("" + gs.getElt().getLongitude().getValue());
                altLabel.setText("" + gs.getElt().getAltitude().getValue());
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
                    removeDisplayable(gs.getCd());
                    removeDisplayable(gs.getTd());
                } catch (RemoteException | VisADException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
    		
    		boolean fontChanged = true;
    		boolean swathChanged = false;
    		scale = getViewManager().getMaster().getDisplayScale();
    		curSwathCenterWidth = jcbSCLineWidth.getSelectedIndex() + 1;
    		curSwathEdgeWidth = jcbSELineWidth.getSelectedIndex() + 1;
    		if (curSwathCenterWidth != prvSwathCenterWidth) swathChanged = true;
    		if (curSwathEdgeWidth != prvSwathEdgeWidth) swathChanged = true;
    		
    		curTrackLineStyle = jcbTrackLineStyle.getSelectedIndex();
    		if (curTrackLineStyle != prvTrackLineStyle) swathChanged = true; 
    		curEdgeLineStyle = jcbEdgeLineStyle.getSelectedIndex();
    		if (curEdgeLineStyle != prvEdgeLineStyle) swathChanged = true;
    		
    		curSwathColor = colorSwatch.getColor();
    		if (! curSwathColor.equals(prvSwathColor)) swathChanged = true;
    		
    		int newSwathWidth = validateSwathWidthField();
    		if (newSwathWidth > 0) {
    			curWidth = newSwathWidth;
    			if (curWidth != prvWidth) swathChanged = true;
    		}
    		
    		// update font attributes if necessary
    		Font f = otFontSelector.getFont();
    		if (! f.equals(otCurFont)) {
    			otCurFont = f;
    			fontChanged = true;
    		}
    		
    		// see if label interval has changed
    		SpinnerNumberModel snm = (SpinnerNumberModel) (js.getModel());
    		int tmpLabelInterval = ((Integer) snm.getValue()).intValue();
    		if ((tmpLabelInterval != labelInterval) || fontChanged) {
    			logger.debug("Label interval change from: " + labelInterval +
    					" to: " + tmpLabelInterval);
    			labelInterval = tmpLabelInterval;
    			try {
    			    
    			    if (jcbLabels.isSelected()) {
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
    		                double distance = 0.0d;
    		                LatLonTuple prvPoint = null;
    
    		                for (int i = 0; i < npts; i++) {
    		                    Tuple t = (Tuple) dataArr[i];
    		                    Data[] tupleComps = t.getComponents();
    
    		                    LatLonTuple llt = (LatLonTuple) tupleComps[1];
    		                    double dlat = llt.getLatitude().getValue();
    		                    double dlon = llt.getLongitude().getValue();
    
    	                        if ((i % labelInterval) == 0) {
    	                        	
    			                    if (prvPoint != null) {
    			                    	distance = Util.distance(prvPoint, llt);
    			                    	if (distance < LABEL_DISTANCE_THRESHOLD) {
    			                    		continue;
    			                    	}
    			                    }
    			                    
    	                            String str = ((Text) tupleComps[0]).getValue();
    	                            int indx = str.indexOf(" ") + 1;
    	                            String subStr = "- " + str.substring(indx, indx+5);
    	                            TextDisplayable time = new TextDisplayable(SWATH_MODS, otTextType);
    	                            time.setJustification(TextControl.Justification.LEFT);
    	                            time.setVerticalJustification(TextControl.Justification.CENTER);
    	                            time.setColor(curSwathColor);
    	                            time.setFont(otFontSelector.getFont());
    	                            time.setTextSize((float) scale * otFontSelector.getFontSize() / FONT_SCALE_FACTOR);
    	                            time.setSphere(inGlobeDisplay());
    	                            
    	                            RealTuple lonLat =
    	                                new RealTuple(RealTupleType.SpatialEarth2DTuple,
    	                                    new double[] { dlon, dlat });
    	                            Tuple tup = new Tuple(makeTupleType(otTextType),
    	                                new Data[] { lonLat, new Text(otTextType, subStr)});
    	                            time.setData(tup);
    	                            timeLabelDsp.addDisplayable(time);
    	                            
    		                        prvPoint = llt;
    	                        }
    
    		                }
    		            }
    			    }
					
		    		// check swath width field, update if necessary
		    		if (swathChanged) changeSwathWidth();
		    		
				} catch (RemoteException re) {
					re.printStackTrace();
				} catch (VisADException vade) {
					vade.printStackTrace();
				}
    		}
            
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
            
            logger.debug("Apply Station mods for: " + gs.getName() +
                    ", cur font: " + gs.getTd().getFont().getFontName() +
                    ", cur color: " + gs.getTd().getColor());
            
    		// flag indicates user changed some parameter
    		boolean somethingChanged = false;
    		
    		// Check each parameter of the ground station selected with UI settings
    		
    		// Color
    		if (gs.getTd().getColor() != antColorSwatch.getColor()) {
    		    logger.debug("GroundStation color change...");
    		    try {
                    gs.getCd().setColor(antColorSwatch.getColor());
                    gs.getTd().setColor(antColorSwatch.getColor());
                } catch (RemoteException | VisADException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
    		    somethingChanged = true;
    		}
    		
    		// Font
    		if (gs.getTd().getFont() != gsFontSelector.getFont()) {
    		    logger.debug("GroundStation font change...");
    		    try {
                    gs.getTd().setFont(gsFontSelector.getFont());
                } catch (RemoteException | VisADException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
                            removeDisplayable(gs.getCd());
                        } catch (RemoteException | VisADException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
	            		gs.setAntennaAngle(newAngle);
	            		EarthLocationTuple elt = gs.getElt();
	            		double altitude = dataSource.getNearestAltToGroundStation(latitude, longitude) / 1000.0;
	                    CurveDrawer cdNew = makeCoverageCircle(
	                            Math.toRadians(elt.getLatitude().getValue()), 
	                            Math.toRadians(elt.getLongitude().getValue()),
	                            altitude, newAngle, antColorSwatch.getColor());
	                    addDisplayable(cdNew);
	                    gs.setCd(cdNew);
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
            
            CurveDrawer cd = gs.getCd();
            int curWidth = (int) cd.getLineWidth();
            int curStyle = cd.getLineStyle();
            
            if (curWidth != (jcbStationLineWidth.getSelectedIndex() + 1)) {
                try {
                    logger.debug("GroundStation line width change...");
                    double altitude = dataSource.getNearestAltToGroundStation(latitude, longitude) / 1000.0;
                    CurveDrawer cdNew = makeCoverageCircle(
                            Math.toRadians(gs.getElt().getLatitude().getValue()), 
                            Math.toRadians(gs.getElt().getLongitude().getValue()),
                            altitude, gs.getAntennaAngle(), antColorSwatch.getColor());
                    cdNew.setLineWidth(jcbStationLineWidth.getSelectedIndex() + 1);
                    removeDisplayable(gs.getCd());
                    addDisplayable(cdNew);
                    gs.setCd(cdNew);
                } catch (RemoteException | VisADException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                somethingChanged = true;
            }
            
            if (curStyle != (jcbStationLineStyle.getSelectedIndex())) {
                try {
                    logger.debug("GroundStation line style change...");
                    double altitude = dataSource.getNearestAltToGroundStation(latitude, longitude) / 1000.0;
                    CurveDrawer cdNew = makeCoverageCircle(
                            Math.toRadians(gs.getElt().getLatitude().getValue()), 
                            Math.toRadians(gs.getElt().getLongitude().getValue()),
                            altitude, gs.getAntennaAngle(), antColorSwatch.getColor());
                    cdNew.setLineStyle(jcbStationLineStyle.getSelectedIndex());
                    removeDisplayable(gs.getCd());
                    addDisplayable(cdNew);
                    gs.setCd(cdNew);
                } catch (RemoteException | VisADException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    private void changeSwathWidth() {

    	logger.debug("changeSwathWidth() in...");
    	if ((curWidth != prvWidth) || 
    			(curTrackLineStyle != prvTrackLineStyle) ||
    			(curEdgeLineStyle != prvEdgeLineStyle) ||
    			(curSwathCenterWidth != prvSwathCenterWidth) ||
    			(curSwathEdgeWidth != prvSwathEdgeWidth) ||
    			(curSwathColor != prvSwathColor)) {
    		prvWidth = curWidth;
    		prvSwathCenterWidth = curSwathCenterWidth;
    		prvSwathEdgeWidth = curSwathEdgeWidth;
    		prvSwathColor = curSwathColor;
    		prvTrackLineStyle = curTrackLineStyle;
    		prvEdgeLineStyle = curEdgeLineStyle;
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
    			createTrackDisplay(data, true);
    			applyDisplayableLevels();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
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
            createTrackDisplay(data, true);
            applyDisplayableLevels();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void createTrackDisplay(Data data, boolean doTrack) {
        
    	logger.debug("createTrackDisplay() in...");
    	// Always check for View scale change (user zoomed in or out)
    	scale = getViewManager().getMaster().getDisplayScale();
        try {
            List<String> dts = new ArrayList<String>();
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
                        if ((i % labelInterval) == 0) {
		                    
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
                            int indx = str.indexOf(" ") + 1;
                            String subStr = "- " + str.substring(indx, indx + 5);
                            TextDisplayable time = new TextDisplayable(SWATH_MODS, otTextType);
                            time.setJustification(TextControl.Justification.LEFT);
                            time.setVerticalJustification(TextControl.Justification.CENTER);
                            time.setColor(curSwathColor);
                    		time.setTextSize((float) scale * otFontSelector.getFontSize() / FONT_SCALE_FACTOR);
                    		time.setFont(otFontSelector.getFont());
                    		time.setSphere(inGlobeDisplay());
                            
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
                    Gridded2DSet track = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                               latlon, npts);
                    SampledSet[] set = new SampledSet[1];
                    set[0] = track;
                    UnionSet uset = new UnionSet(set);
                    CurveDrawer trackLines = new CurveDrawer(uset);
                    trackLines.setData(uset);
                    trackLines.setDrawingEnabled(false);
                    trackLines.setLineStyle(jcbTrackLineStyle.getSelectedIndex());
                    trackDsp.addDisplayable(trackLines);
                    trackDsp.setColor(curSwathColor);
                    trackDsp.setLineWidth(jcbSCLineWidth.getSelectedIndex() + 1);

                    addDisplayable(trackDsp, FLAG_COLORTABLE);
                    addDisplayable(timeLabelDsp, FLAG_COLORTABLE);
                }

                if (jcbSwathEdges.isSelected()) {
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
                    Gridded2DSet left = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                               leftC, npt);
                    SampledSet[] lSet = new SampledSet[1];
                    lSet[0] = left;
                    UnionSet lUSet = new UnionSet(lSet);
                    CurveDrawer leftLines = new CurveDrawer(lUSet);
                    leftLines.setLineStyle(jcbEdgeLineStyle.getSelectedIndex());
                    leftLines.setData(lUSet);
                    swathEdgeDsp.addDisplayable(leftLines);
                    leftLines.setDrawingEnabled(false);
    
                    Gridded2DSet right = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                               rightC, npt);
                    SampledSet[] rSet = new SampledSet[1];
                    rSet[0] = right;
                    UnionSet rUSet = new UnionSet(rSet);
                    CurveDrawer rightLines = new CurveDrawer(rUSet);
                    rightLines.setLineStyle(jcbEdgeLineStyle.getSelectedIndex());
                    rightLines.setData(rUSet);
                    swathEdgeDsp.addDisplayable(rightLines);
                    rightLines.setDrawingEnabled(false);
                    
                    swathEdgeDsp.setColor(curSwathColor);
                    swathEdgeDsp.setLineWidth(curSwathEdgeWidth);
                    addDisplayable(swathEdgeDsp, FLAG_COLORTABLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /* (non-Javadoc)
     * @see ucar.unidata.idv.control.DisplayControlImpl#displayableToFront()
     */
    @Override
    public void displayableToFront() {
        redrawAll();
        super.displayableToFront();
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
        Integer defaultInterval = new Integer(5); 
        Integer minInterval = new Integer(1);
        Integer maxInterval = new Integer(120); 
        Integer intervalStep = new Integer(1); 
        SpinnerNumberModel snm = 
        		new SpinnerNumberModel(defaultInterval, minInterval, maxInterval, intervalStep);
        JLabel intervalLabel = new JLabel("Label Interval:");
        JLabel intervalUnits = new JLabel("minutes");
        js = new JSpinner(snm);
        labelPanel.add(Box.createHorizontalStrut(5));
        labelPanel.add(intervalLabel);
        labelPanel.add(js);
        labelPanel.add(intervalUnits);
        
        // line style for drawing swath track and width edges
        jcbTrackLineStyle.addActionListener(this);
        // init to solid
        jcbTrackLineStyle.setSelectedIndex(0);
        curTrackLineStyle = jcbTrackLineStyle.getSelectedIndex();

        jcbEdgeLineStyle.addActionListener(this);
        // init to dashed
        jcbEdgeLineStyle.setSelectedIndex(1);
        curEdgeLineStyle = jcbEdgeLineStyle.getSelectedIndex();
        
        fontSizePanel.add(labelPanel);
        JPanel botPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        botPanel.add(new JLabel("Font: "));
        botPanel.add(otFontSelector.getComponent());
        fontSizePanel.add(botPanel);

        colorSwatch = new ColorSwatchComponent(getStore(), curSwathColor, "Color");
        colorSwatch.setPreferredSize(new Dimension(30, 24));
        
        colorPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        colorPanel.add(new JLabel("Color: "));
        colorPanel.add(colorSwatch);
        
        colorPanel.add(Box.createHorizontalStrut(5));
        colorPanel.add(new JLabel("Track Width: "));
        colorPanel.add(jcbSCLineWidth);
        
        colorPanel.add(Box.createHorizontalStrut(4));
        colorPanel.add(new JLabel("Track Style: "));
        colorPanel.add(jcbTrackLineStyle);
        
        colorPanel.add(Box.createHorizontalStrut(5));
        colorPanel.add(new JLabel("Edge Width: "));
        colorPanel.add(jcbSELineWidth);
        
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

    private CurveDrawer makeCoverageCircle(double lat, double lon, double satAlt, int angle, Color color) {

        /* mean Earth radius in km */
        double earthRadius = AstroConst.R_Earth_mean / 1000.0;
        satAlt += earthRadius;
        double SAC = Math.PI / 2.0 + Math.toRadians(angle);
        double sinASC = earthRadius * Math.sin(SAC) / satAlt;
        double dist = earthRadius * (Math.PI - SAC - Math.asin(sinASC));
        double rat = dist / earthRadius;

        // 360 degrees +1 points so we connect final segment, last point to first
        int npts = 361;
        float[][] latlon = new float[2][npts];
        double cosDist = Math.cos(rat);
        double sinDist = Math.sin(rat);
        double sinLat = Math.sin(lat);
        double cosLat = Math.cos(lat);
        double sinLon = -Math.sin(lon);
        double cosLon = Math.cos(lon);
        for (int i = 0; i < npts; i++) {
            double azimuth = Math.toRadians((double) i);
            double cosBear = Math.cos(azimuth);
            double sinBear = Math.sin(azimuth);
            double z = cosDist * sinLat +
                       sinDist * cosLat * cosBear;
            double y = cosLat * cosLon * cosDist +
                       sinDist * (sinLon * sinBear - sinLat * cosLon * cosBear);
            double x = cosLat * sinLon * cosDist -
                       sinDist * (cosLon * sinBear + sinLat * sinLon * cosBear);
            double r = Math.sqrt(x * x + y * y);
            double latRad = Math.atan2(z, r);
            double lonRad = 0.0;
            if (r > 0.0) lonRad = -Math.atan2(x, y);
            latlon[0][i] = (float) Math.toDegrees(latRad);
            latlon[1][i] = (float) Math.toDegrees(lonRad);
        }
        CurveDrawer coverageCircle = null;
        try {
            Gridded2DSet circle = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                               latlon, npts);
            SampledSet[] set = new SampledSet[1];
            set[0] = circle;
            UnionSet uset = new UnionSet(set);
            coverageCircle = new CurveDrawer(uset);
            coverageCircle.setLineWidth(jcbStationLineWidth.getSelectedIndex() + 1);
            coverageCircle.setLineStyle(jcbStationLineStyle.getSelectedIndex());
            coverageCircle.setColor(color);
            coverageCircle.setData(uset);
            coverageCircle.setDrawingEnabled(false);
            if (! inGlobeDisplay()) 
            	coverageCircle.setConstantPosition(gsZ, navDsp.getDisplayAltitudeType());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return coverageCircle;
    }

    public Color getAntColor() {
        if (antColor == null) antColor = defaultAntColor;
        return antColor;
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
				vade.printStackTrace();
			}
		}
		
		// return either original or modified data object
		return data;
	}

	public double getLatitude() {
        return latitude;
    }

    /* (non-Javadoc)
     * @see ucar.unidata.idv.control.DisplayControlImpl#getLegendLabelTemplate()
     */
    @Override
    public String getLegendLabelTemplate() {
        return DisplayControlImpl.MACRO_DISPLAYNAME;
    }

    public double getLongitude() {
        return longitude;
    }

    /**
     * Start out with Trivial Map Projection because it doesn't have
     * the low-level VisAD issue labeling across dateline.  But after
     * init, the user can reproject to whatever works for them
     */

    @Override public MapProjection getDataProjection() {
        
        MapProjection mp = null;
        if (initialized) {
            mp = super.getDataProjection();
            return mp;
        } else {
            try {
                mp = new TrivialMapProjection();
            } catch (Exception e) {
                logger.error("Error setting default projection", e);
            }
        }

        return mp;
    }
    
    public String getStation() {
        return station;
    }

    private float[][][] getSwath(float[][] track) {
        double earthRadius = AstroConst.R_Earth_mean / 1000.0;
        int npt = track[0].length-1;
        float[][][] ret = new float[2][2][npt - 1];
        try {
            int indx = 0;
            for (int i = 1; i < npt; i++) {
                double latA = Math.toRadians(track[0][i - 1]);
                double lonA = Math.toRadians(track[1][i - 1]);

                double latB = Math.toRadians(track[0][i + 1]);
                double lonB = Math.toRadians(track[1][i + 1]);

                double diffLon = lonB - lonA;
                double bX = Math.cos(latB) * Math.cos(diffLon);
                double bY = Math.cos(latB) * Math.sin(diffLon);
                double xFac = Math.cos(latA) + bX;
                double latC = Math.atan2(Math.sin(latA) + Math.sin(latB), Math.sqrt(xFac * xFac + bY * bY));
                double lonC = lonA + Math.atan2(bY, xFac);

                double bearing = Math.atan2(Math.sin(diffLon) * Math.cos(latB),
                                 Math.cos(latA) * Math.sin(latB) - Math.sin(latA) * Math.cos(latB) * Math.cos(diffLon))
                                 + Math.PI / 2.0;
                double dist = curWidth / 2.0;
                dist /= earthRadius;
                double lat = Math.asin(Math.sin(latC) * Math.cos(dist) +
                                       Math.cos(latC) * Math.sin(dist) * Math.cos(bearing));
                double lon = lonC + Math.atan2(Math.sin(bearing) * Math.sin(dist) * Math.cos(latC),
                                               Math.cos(dist) - Math.sin(latC) * Math.sin(lat));
                float latD = (float) Math.toDegrees(lat);
                float lonD = (float) Math.toDegrees(lon);

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
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    @Override public boolean init(DataChoice dataChoice) 
        throws VisADException, RemoteException 
    {
        logger.debug("init() in...");
        
        PolarOrbitTrackDataSource potdc = getDataSource();

        // validate time range before going ahead with control initialization
        if (! potdc.getTrs().begTimeOk()) {
        	JOptionPane.showMessageDialog(null, 
        			"Invalid start time, must follow format HH:MM:SS", 
        			"Time Range Selection Error", JOptionPane.ERROR_MESSAGE);
        	return false;
        }
        
        if (! potdc.getTrs().endTimeOk()) {
        	JOptionPane.showMessageDialog(null, 
        			"Invalid end time, must follow format HH:MM:SS", 
        			"Time Range Selection Error", JOptionPane.ERROR_MESSAGE);
        	return false;
        }
        
        if (! potdc.getTrs().timeRangeOk()) {
        	JOptionPane.showMessageDialog(null, 
        			"Invalid time range selection, please correct", 
        			"Time Range Selection Error", JOptionPane.ERROR_MESSAGE);
        	return false;
        }
        
        // allow at most two full days of orbit tracks - more than this will
        // at best clutter the display and at worst grind McV indefinitely
        long timeDiff = potdc.getTrs().getTimeRangeInSeconds();
        if (timeDiff >= (60 * 60 * 24 * 2)) {
        	JOptionPane.showMessageDialog(null, 
        			"Time range greater than two full days is not allowed, please correct", 
        			"Time Range Selection Error", JOptionPane.ERROR_MESSAGE);
        	return false;        	
        }
        
    	// instantiate components we need to exist at initialization
    	latLabel = new JLabel();
    	lonLabel = new JLabel();
    	altLabel = new JLabel();
        String [] lineWidths = {"1", "2", "3", "4"};
        jcbStationLineWidth = new JComboBox<String>(lineWidths);
        jcbSCLineWidth = new JComboBox<String>(lineWidths);
        
        // create Label checkbox toggle
        jcbLabels = new JCheckBox("Labels On/Off");
        jcbLabels.setSelected(true);
        jcbLabels.setName(CHECKBOX_LABELS);
        jcbLabels.addItemListener(this);
        
        // initialize swath center (track line) to width 2
        jcbSCLineWidth.setSelectedIndex(1);
        jcbEdgeLineStyle.setSelectedIndex(1);
        jcbSELineWidth = new JComboBox<String>(lineWidths);
        otFontSelector = new FontSelector(FontSelector.COMBOBOX_UI, false, false);
        otFontSelector.setFont(FontSelector.DEFAULT_FONT);
        gsFontSelector = new FontSelector(FontSelector.COMBOBOX_UI, false, false);
        gsFontSelector.setFont(FontSelector.DEFAULT_FONT);
        
        // Bump default font size down just a bit...
        otFontSelector.setFontSize(9);
        gsFontSelector.setFontSize(9);
        otCurFont = otFontSelector.getFont();
        
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
            logger.error("problem creating composite displayable");
            e.printStackTrace();
            return false;
        }
        boolean result = super.init((DataChoice) this.getDataChoices().get(0));

        String dispName = getDisplayName();
        setDisplayName(getLongParamName() + " " + dispName);
        logger.debug("Setting display name: " + getDisplayName());
        try {
            String longName = getLongParamName().replaceAll(" ", "");
            otTextType = new TextType(SWATH_MODS + longName);
        } catch (Exception e) {
        	e.printStackTrace();
            otTextType = TextType.Generic;
        }

        Data data = getData(getDataInstance());
        createTrackDisplay(data, true);
        dataSource = getDataSource();
        try {
            navDsp = getNavigatedDisplay();
            float defaultZ = getMapViewManager().getDefaultMapPosition();
            // we're just nudging a bit so tracks (and their labels) get drawn over
            // ground stations (and their labels), which get drawn over default map level
            // user can change this in map controls if they prefer maps on top
            gsZ = defaultZ + 0.01f;
            trackZ = defaultZ + 0.02f;
            // range on "map level" stuff is -1 to 1, stay within these limits
            if (trackZ > 1.0f) trackZ = 1.0f;
            if (gsZ > 1.0f) gsZ = 1.0f;
            if (! inGlobeDisplay()) {
            	applyDisplayableLevels();
            }
        } catch (Exception e) {
            logger.error("get display center e=" + e);
        }

        initialized = true;

        return result;
    }

    public void itemStateChanged(ItemEvent ie) {

    	// now we got multiple checkboxes, so first see which one applies
    	String source = ((JCheckBox) ie.getSource()).getName();
    	try {
    		if (source.equals(CHECKBOX_LABELS)) {
    			if (ie.getStateChange() == ItemEvent.DESELECTED) {
    				timeLabelDsp.setVisible(false);
    			} else {
    				timeLabelDsp.setVisible(true);
    			}
                redrawAll();
    		}
    		if (source.equals(CHECKBOX_SWATH_EDGES)) {
    			if (ie.getStateChange() == ItemEvent.DESELECTED) {
    				swathEdgeDsp.setVisible(false);
    			} else {
    				swathEdgeDsp.setVisible(true);
    			}
				updateDisplayList();
    		}
    	} catch (RemoteException re) {
    		re.printStackTrace();
    	} catch (VisADException vade) {
    		vade.printStackTrace();
    	}

    }

    private void labelGroundStation(GroundStation station) {
        
        // Font f = station.getTd().getFont();
        logger.debug("labelGroundStation() in..." + station.getName());
        scale = getViewManager().getMaster().getDisplayScale();
    	try {
    		String str = "+ " + station;
    		logger.debug("Drawing station: " + str);

    		TextType tt = new TextType(STATION_MODS + ttCounter);
    		TextDisplayable groundStationDsp = 
    		   new TextDisplayable(STATION_MODS + jcbStationsPlotted.getItemCount(), tt);
    		ttCounter++;
    		groundStationDsp.setJustification(TextControl.Justification.LEFT);
    		groundStationDsp.setVerticalJustification(TextControl.Justification.CENTER);
    		groundStationDsp.setColor(antColorSwatch.getColor());
    		groundStationDsp.setFont(gsFontSelector.getFont());
    		groundStationDsp.setTextSize((float) scale * gsFontSelector.getFont().getSize() / FONT_SCALE_FACTOR);
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
    		station.setTd(groundStationDsp);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    private JPanel makeGroundStationPanel() {
    	
    	JPanel jp = new JPanel(new MigLayout());
        jp.setBorder(BorderFactory.createTitledBorder(" Ground Station Controls "));

        jcbStationLineStyle = new JComboBox<String>(Constants.lineStyles);
        jcbStationLineStyle.addActionListener(this);
        jcbStationLineStyle.setSelectedIndex(1);
        
        locationComboBox = new JComboBox<GroundStation>();
        jcbStationsPlotted = new JComboBox<GroundStation>();
        jcbStationsPlotted.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                logger.debug("Active Station changed...");
                GroundStation gs = (GroundStation) jcbStationsPlotted.getSelectedItem();
                if (gs == null) {
                    JOptionPane.showMessageDialog(null, 
                    "No Active Stations");
                } else {
                    // Update UI with settings for this station
                    gsFontSelector.setFont(gs.getTd().getFont());
                    antColorSwatch.setBackground(gs.getCd().getColor());
                    jcbStationLineStyle.setSelectedIndex(gs.getCd().getLineStyle());
                    jcbStationLineWidth.setSelectedIndex((int) (gs.getCd().getLineWidth() - 1)); 
                    antennaAngle.setText("" + gs.getAntennaAngle());
                    curAngle = gs.getAntennaAngle();
                    latLabel.setText("" + gs.getElt().getLatitude().getValue());
                    lonLabel.setText("" + gs.getElt().getLongitude().getValue());
                    altLabel.setText("" + gs.getElt().getAltitude().getValue());
                }
                return;
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
        addButton.setActionCommand(STATION_ADD);
        addButton.addActionListener(this);
        locationPanel.add(addButton);
        
        JPanel customPanel = new JPanel();
        customPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        customPanel.add(new JLabel("Custom Ground Station:    Label: "));
        customLab = new JTextField(6);
        customPanel.add(customLab);
        customPanel.add(new JLabel("Latitude: "));
        customLat = new JTextField(6);
        customPanel.add(customLat);
        customPanel.add(new JLabel("Longitude: "));
        customLon = new JTextField(6);
        customPanel.add(customLon);
        JButton customButton = new JButton("Add Custom");
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
        antColorSwatch.setPreferredSize(new Dimension(30, 24));
        
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
        antColorPanel.add(antennaAngle);
        
        jp.add(locationPanel, "wrap");
        jp.add(customPanel, "wrap");
        jp.add(plottedStationsPanel, "wrap");
        jp.add(latLonAltPanel, "wrap");
        jp.add(Box.createVerticalStrut(5), "wrap");
        jp.add(gsFontPanel, "wrap");
        jp.add(antColorPanel, "wrap");
        
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton applyGroundStationMods = new JButton("Apply");
        applyGroundStationMods.setActionCommand(STATION_MODS);
        applyGroundStationMods.addActionListener(this);
        bottomRow.add(applyGroundStationMods);
        jp.add(bottomRow);
        
        return jp;
    }

    private JPanel makeSwathWidthPanel() {
        if (dataChoice != null)
            satelliteName = new JLabel(dataChoice.getName());
        Integer isw = new Integer((int) curWidth);
        swathWidthFld = new JTextField(isw.toString(), 5);
        if (curWidth == 0) swathWidthFld.setText(SWATH_NA);

        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // first on this panel, check box to turn on/off swath line edges
        jcbSwathEdges = new JCheckBox("Swath Edges On/Off");
        jcbSwathEdges.setSelected(true);
        jcbSwathEdges.setName(CHECKBOX_SWATH_EDGES);
        jcbSwathEdges.addItemListener(this);
        jp.add(jcbSwathEdges);
        
        jp.add(Box.createHorizontalStrut(5));
        jp.add(new JLabel("Satellite: "));
        jp.add(satelliteName);
        jp.add(Box.createHorizontalStrut(5));
        jp.add(new JLabel("Swath Width: "));
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
            e.printStackTrace();
        }
        return t;
    }

    private void addGroundStation(GroundStation gs) {
        
        logger.debug("addGroundStation() in, name: " + gs.getName());
        
        try {

            EarthLocationTuple elt = gs.getElt();
            latLabel.setText(elt.getLatitude().toString());
            lonLabel.setText(elt.getLongitude().toString());
            altLabel.setText(elt.getAltitude().toString());
            
            // quick and easy way to limit sig digits to something not too crazy
            if (altLabel.getText().length() > 10) altLabel.setText(altLabel.getText().substring(0, 9));
            latitude = Double.parseDouble(latLabel.getText());
            longitude = Double.parseDouble(lonLabel.getText());
            setSatelliteAltitude(dataSource.getNearestAltToGroundStation(latitude, longitude) / 1000.0);
            
            CurveDrawer cd = makeCoverageCircle(Math.toRadians(latitude), Math.toRadians(longitude),
                    satelliteAltitude, curAngle, antColorSwatch.getColor());

            if (cd != null) {
            	logger.debug("Adding ground station, station name: " + gs.getName());
                labelGroundStation(gs);
                cd.setLineWidth(jcbStationLineWidth.getSelectedIndex() + 1);
                gs.setCd(cd);

            }

            cd.setConstantPosition(gsZ, navDsp.getDisplayAltitudeType());
    		addDisplayable(cd, FLAG_COLORTABLE);
            TextDisplayable td = gs.getTd();
            td.setConstantPosition(gsZ, navDsp.getDisplayAltitudeType());
            addDisplayable(td);

        } catch (Exception e) {
            e.printStackTrace();
        }
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
            logger.error("Exception in PolarOrbitTrackControl.setAntColor e=" + e);
        }
    }
        
    private void setSatelliteAltitude(double val) {
        satelliteAltitude = val;
    }
    
    public void setStation(String val) {
        station = val.trim();
    }

    private int validateSwathWidthField() {
    	String s = swathWidthFld.getText().trim();
    	int val = -1;
    	try {
    		val = Integer.parseInt(s);
    	} catch (NumberFormatException nfe) {
    		// TJJ Jun 2015 - if GEO sensor, N/A means return invalid, but no warning msg needed
    		if ((s != null) && (s.equals(SWATH_NA))) {
    			return -1;
    		}
    		// throw up a dialog to tell user the problem
    		JOptionPane.showMessageDialog(latLonAltPanel, 
    				"Invalid swath width: must be an integer value in km");
    		return -1;
    	}

    	if ((val < SWATH_WIDTH_MIN) || (val > SWATH_WIDTH_MAX)) {
    		// throw up a dialog to tell user the problem
    		JOptionPane.showMessageDialog(latLonAltPanel, 
    				"Swath width valid range is " + SWATH_WIDTH_MIN + 
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
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
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
            }
        };
        Window f = GuiUtils.getWindow(getContents());
        JComponent buttons = GuiUtils.makeApplyOkCancelButtons(listener);
        JComponent propContents = GuiUtils.inset(GuiUtils.centerBottom(jtp, buttons), 5);
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
