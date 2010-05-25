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

package edu.wisc.ssec.mcidasv.data;

import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AREAnav;

import edu.wisc.ssec.mcidasv.Constants;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.accessibility.*;
import javax.swing.*;
import javax.swing.event.*;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import visad.VisADException;
import visad.data.mcidas.AREACoordinateSystem;
import visad.georef.*;


public class GeoLatLonSelection extends DataSelectionComponent implements Constants {

      private static GeoLocationInfo geoLocInfo;

      /** The spacing used in the grid layout */
      protected static final int GRID_SPACING = 3;

      /** Used by derived classes when they do a GuiUtils.doLayout */
      protected static final Insets GRID_INSETS = new Insets(GRID_SPACING,
                                                      GRID_SPACING,
                                                      GRID_SPACING,
                                                      GRID_SPACING);

      DataChoice dataChoice;
      MapProjection sampleProjection;

      /** earth coordinates */
      protected static final String TYPE_LATLON = "Latitude/Longitude";

      /** image */
      protected static final String TYPE_IMAGE = "Image Coordinates";

      /** area */
      protected static final String TYPE_AREA = "Area Coordinates";

      /** flag for center */
      protected static final String PLACE_CENTER = "CENTER";

      /** flag for upper left */
      protected static final String PLACE_ULEFT = "ULEFT";

      /** Property for image default value lat/lon */
      protected static final String PROP_LATLON = "LATLON";

      /** Property for image default value line/ele */
      protected static final String PROP_LINEELE = "LINELE";

      /** Property for image default value loc */
      protected static final String PROP_LOC = "LOC";

      /** Property for image default value mag */
      protected static final String PROP_MAG = "MAG";
      protected static final String PROP_LMAG = "LMAG";
      protected static final String PROP_EMAG = "EMAG";

      /** Property for image default value place */
      protected static final String PROP_PLACE = "PLACE";

      /** Property for image default value size */
      protected static final String PROP_SIZE = "SIZE";

      /** Property for image default value unit */
      protected static final String PROP_TYPE = "TYPE";

      /** Property for line resolution */
      protected static final String PROP_LRES = "LRES";
      protected static final String PROP_PLRES = "PLRES";

      /** Property for element resolution */
      protected static final String PROP_ERES = "ERES";
      protected static final String PROP_PERES = "PERES";

      protected static final String PROP_READOUT = "READOUT";

      /** This is the list of properties that are used in the advanced gui */
      private static final String[] ADVANCED_PROPS = {
          PROP_TYPE, PROP_PLACE, PROP_LOC, PROP_SIZE,  PROP_MAG,
          PROP_LMAG, PROP_EMAG, PROP_READOUT
      };

      /** This is the list of labels used for the advanced gui */
      private static final String[] ADVANCED_LABELS = {
        "Coordinate Type:", "Placement:", "Location:", "   Image Size:",
        "Magnification:", "", "", "Selected Area: "
      };

      private String kmLbl = " km";

      /** Input for lat/lon center point */
      protected LatLonWidget latLonWidget = new LatLonWidget();

      /** Widget to hold the number of elements in the advanced */
      JTextField numElementsFld = new JTextField();

      /** Widget to hold  the number of lines   in the advanced */
      JTextField numLinesFld = new JTextField();

      /** Widget for the line  center point in the advanced section */
      JTextField centerLineFld = new JTextField();

      /** Widget for the element  center point in the advanced section */
      JTextField centerElementFld = new JTextField();

      /** Label used for the line center */
      private JLabel centerLineLbl = new JLabel();

      /** Label used for the element center */
      private JLabel centerElementLbl = new JLabel();

      /** Label used for the center latitude */
      private JLabel centerLatLbl = new JLabel();

      /** Label used for the center longitude */
      private JLabel centerLonLbl = new JLabel();

      /** _more_ */
      private JToggleButton lockBtn;
      private JButton fullResBtn;

      private JPanel lMagPanel;
      private JPanel eMagPanel;

      /** Widget for the line magnfication in the advanced section */
      protected JSlider lineMagSlider;

      /** Label for the line mag. in the advanced section */
      JLabel lineMagLbl = new JLabel();
      JLabel lineResLbl = new JLabel();

      /** Widget for the element magnfication in the advanced section */
      protected JSlider elementMagSlider;

      /** Label for the element mag. in the advanced section */
      JLabel elementMagLbl = new JLabel();
      JLabel elementResLbl = new JLabel();

      /** location panel */
      protected GuiUtils.CardLayoutPanel locationPanel;

      /** flag for setting properties */
      private boolean amSettingProperties = false;

      JComboBox coordinateTypeComboBox;
      JComboBox locationComboBox;

      String[] coordinateTypes = { TYPE_LATLON, TYPE_IMAGE, TYPE_AREA };
      String[] locations = {"Center", "Upper Left"};

/** the place string */
      private static String defaultType = TYPE_LATLON;
      private static String place;
      private String defaultPlace = PLACE_CENTER;
      private static int defaultNumLines = 1000;
      private static int defaultNumEles = 1000;
      private int numLines = defaultNumLines;
      private int numEles = defaultNumEles;
      private static double latitude;
      private double defaultLat = 999.0;
      private static double longitude;
      private double defaultLon = 999.0;
      private static boolean resetLatLon = true;
      private static int line;
      private int defaultLine = -1;
      private static int element;
      private int defaultElement = -1;
      private static int lineMag;
      private double dLineMag;
      private int defaultLineMag;
      private static int elementMag;
      private double dElementMag;
      private int defaultElementMag;
      private static boolean isLineEle = false;
      private static double lRes;
      protected static double baseLRes;
      private static double eRes;
      protected static double baseERes;

      private Hashtable properties;
      private int uLLine;
      private int uLEle;
      private int centerLine;
      private int centerEle;
      private boolean amUpdating = false;


      /** Maps the PROP_ property name to the gui component */
      private Hashtable propToComps = new Hashtable();

      /** size label */ JLabel sizeLbl;

      /** base number of lines */
      private double baseNumLines = 0.0;

      /** base number of elements */
      private double baseNumElements = 0.0;

      private DataSourceImpl dataSource;
      private static DataSourceImpl lastDataSource;
      private AreaDirectory previewDir;
      private static AREAnav previewNav;
      private AREAnav areaNav;

      private JPanel latLonPanel;
      private JPanel lineElementPanel;

      /**
       * limit of slider
       */
      private static final int SLIDER_MAX = 1;
      private static final int SLIDER_MIN = -29;
      private static final int SLIDER_WIDTH = 150;
      private static final int SLIDER_HEIGHT = 16;

      /**
       *  Keep track of the lines to element ratio
       */
      private double linesToElements = 1.0;
 
      double[][] imageEL = new double[2][5];
      double[][] areaEL = new double[2][5];
      double[][] displayEL = new double[2][5];
      double[][] latLon = new double[2][5];

      private int[] previewDirBlk;

      private int previewLineRes = 1;
      private int previewEleRes = 1;

      private List readoutLLWidget = new ArrayList();
      private List readoutLatFld = new ArrayList();
      private List readoutLonFld = new ArrayList();
      private JPanel latLonReadoutPanel;

      private List readoutImageLinFld = new ArrayList();
      private List readoutImageEleFld = new ArrayList();
      private JPanel lineElementImageReadoutPanel;

      private List readoutAreaLinFld = new ArrayList();
      private List readoutAreaEleFld = new ArrayList();
      private JPanel lineElementAreaReadoutPanel;

      private GuiUtils.CardLayoutPanel readoutPanel;

      private int eleMax;
      private int lineMax;

      public GeoLatLonSelection(DataSourceImpl dataSource,
             DataChoice dataChoice, Hashtable initProps, MapProjection sample,
             AreaDirectory dir, AREAnav nav) 
              throws VisADException, RemoteException {
          super("Lat/Lon");

          if (dataSource != lastDataSource) this.resetLatLon = true;
          lastDataSource = dataSource;

          this.properties = initProps;
          this.dataSource = dataSource;
          this.dataChoice = dataChoice;
          this.sampleProjection = sample;
          this.baseNumLines = dir.getLines();
          this.baseNumElements = dir.getElements();
          this.previewDir = dir;
          this.previewNav = nav;
          previewDirBlk = this.previewDir.getDirectoryBlock();
          int areaLinRes = previewDirBlk[11];
          int areaEleRes = previewDirBlk[12];
          areaNav = previewNav;
          areaNav.setRes(areaLinRes, areaEleRes);

          int numberOfLines;
          int numberOfElements;
          this.baseNumLines = previewDir.getLines();
          this.baseNumElements = previewDir.getElements();
          if (properties.containsKey(PROP_SIZE)) {
              String str = (String)properties.get(PROP_SIZE);
              String[] strs = StringUtil.split(str, " ", 2);
              numberOfLines = new Integer(strs[0]).intValue();
              numberOfElements = new Integer(strs[1]).intValue();
          } else {
              try {
                  numberOfLines = this.previewDir.getLines();
                  numberOfElements = this.previewDir.getElements();
                  if (numberOfLines < defaultNumLines)
                      defaultNumLines = numberOfLines;
                  if (numberOfElements < defaultNumEles)
                      defaultNumEles = numberOfElements;
                  numberOfLines = defaultNumLines;
                  numberOfElements = defaultNumEles;
              } catch (Exception e) {
                  System.out.println("GeoLatLonSelection: no directory e=" + e);
                  return;
              }
          }
          setNumLines(numberOfLines);
          setNumEles(numberOfElements);
          if (properties.containsKey(PROP_MAG)) {
              String str = (String)properties.get(PROP_MAG);
              String[] strs = StringUtil.split(str, " ", 2);
              defaultLineMag = new Integer(strs[0]).intValue();
              defaultElementMag = new Integer(strs[1]).intValue();
              this.dLineMag = (double)defaultLineMag;
              this.dElementMag = (double)defaultElementMag;
          } else {
              this.dLineMag = -(double)this.previewDir.getLines()/(double)numberOfLines;
              this.dElementMag = -(double)this.previewDir.getElements()/(double)numberOfElements;
              defaultLineMag = (int)(dLineMag - 0.5);
              defaultElementMag = (int)(dElementMag - 0.5);
          }
          setLineMag(defaultLineMag);
          setElementMag(defaultElementMag);
          setDLineMag(this.dLineMag);
          setDElementMag(this.dElementMag);

          try {
              if (properties.containsKey(PROP_LRES)) {
                  double bRes = new Double((String)properties.get(PROP_LRES)).doubleValue();
                  baseLRes = bRes * this.previewDir.getCenterLatitudeResolution();
                  setLRes(baseLRes * Math.abs(defaultLineMag));
              }
              if (properties.containsKey(PROP_ERES)) {
                  double bRes = new Double((String)properties.get(PROP_ERES)).doubleValue();
                  baseERes = bRes * this.previewDir.getCenterLongitudeResolution();
                  setERes(baseERes * Math.abs(defaultElementMag));
              }
          } catch (Exception e) {
              System.out.println("GeoLatLonSelection unable to get resolution: e=" + e);
              return;
          }

          this.place = getPlace();
          if (properties.containsKey(PROP_PLACE)) {
              setPlace((String)properties.get(PROP_PLACE));
          }

          if (properties.containsKey(PROP_PLRES)) {
              this.previewLineRes = new Integer((String)properties.get(PROP_PLRES)).intValue();
          }
          if (properties.containsKey(PROP_PERES)) {
              this.previewEleRes = new Integer((String)properties.get(PROP_PERES)).intValue();
          }

          if (this.resetLatLon) {
              if (previewDir != null) {
                  setLatitude(new Double(previewDir.getCenterLatitude()));
                  setLongitude(new Double(previewDir.getCenterLongitude()));
              }
          } else {
              setLatitude(this.latitude);
              setLongitude(this.longitude);
          }

          if (properties.containsKey(PROP_LATLON)) {
              String str = (String)properties.get(PROP_LATLON);
              String[] strs = StringUtil.split(str, " ", 2);
              setLatitude(new Double(strs[0]).doubleValue());
              setLongitude(new Double(strs[1]).doubleValue());
              this.isLineEle = false;
          } else if (properties.containsKey(PROP_LINEELE)) {
              String str = (String)properties.get(PROP_LINEELE);
              String[] strs = StringUtil.split(str, " ", 3);
              setLine(new Integer(strs[0]).intValue());
              setElement(new Integer(strs[1]).intValue());
              this.isLineEle = true;
          }

          if (defaultLineMag > 1) {
	      numberOfLines = numberOfLines * defaultLineMag;
              setNumLines(numberOfLines);
              setLRes(lRes/defaultLineMag);
              defaultLineMag = 1;
              setDLineMag(1.0);
              setLineMag(defaultLineMag);
          }
          if (defaultElementMag > 1) {
              numberOfElements = numberOfElements * defaultElementMag;
              setNumEles(numberOfElements);
              setERes(lRes/defaultElementMag);
              defaultElementMag = 1;
              setDElementMag(1.0);
              setElementMag(defaultElementMag);
          }
      }

      protected JComponent doMakeContents() {
          String[] propArray  = getAdvancedProps();
          String[] labelArray = getAdvancedLabels();
          Insets  dfltGridSpacing = new Insets(4, 0, 4, 0);
          String  dfltLblSpacing  = " ";
          List allComps = new ArrayList();

          for (int propIdx = 0; propIdx < propArray.length; propIdx++) {
              JComponent propComp = null;
              String     prop     = propArray[propIdx];
              if (prop.equals(PROP_TYPE)) {
                  allComps.add(new JLabel(" "));
                  allComps.add(new JLabel(" "));
                  coordinateTypeComboBox = new JComboBox(coordinateTypes);
                  coordinateTypeComboBox.addActionListener(new ActionListener() {
                      public void actionPerformed(ActionEvent ae) {
                          int selectedIndex = coordinateTypeComboBox.getSelectedIndex();
                          flipLocationPanel(selectedIndex);
                          flipReadoutPanel(selectedIndex);
                      }
                  });
                  propComp = (JComponent)coordinateTypeComboBox;
              }
              else if (prop.equals(PROP_LOC)) {
                  locationComboBox = new JComboBox(locations);
                  setPlace(this.place);
                  locationComboBox.addActionListener(new ActionListener() {
                      public void actionPerformed(ActionEvent e) {
                          String selected =
                                 translatePlace((String)locationComboBox.getSelectedItem());
                          setPlace(selected);
                          cyclePlace();
                      }
                  });
                  propComp = (JComponent)locationComboBox;
                  addPropComp(PROP_LOC, propComp);

                  ActionListener latLonChange =new ActionListener() {
                      public void actionPerformed(ActionEvent ae) {
                          String type = getCoordinateType();
                          if (type.equals(TYPE_LATLON)) {
                              setLatitude();
                              setLongitude();
                              getGeoLocationInfo();
                          } else {
                              setLine();
                              setElement();
                              getGeoLocationInfo();
                          }
                      }
                  };

                  FocusListener linEleFocusChange = new FocusListener() {
                      public void focusGained(FocusEvent fe) {
                      }
                      public void focusLost(FocusEvent fe) {
                          setLine();
                          setElement();
                          getGeoLocationInfo();
                      }
                  };

                  latLonWidget     = new LatLonWidget(latLonChange);

                  FocusListener latLonFocusChange = new FocusListener() {
                      public void focusGained(FocusEvent fe) {
                          JTextField latFld = latLonWidget.getLatField();
                          latFld.setCaretPosition(latFld.getText().length());
                          JTextField lonFld = latLonWidget.getLonField();
                          lonFld.setCaretPosition(lonFld.getText().length());
                      }
                      public void focusLost(FocusEvent fe) {
                          setLatitude();
                          setLongitude();
                          getGeoLocationInfo();
                      }
                  };

                  if (!this.isLineEle) {
                      latLonWidget.setLatLon((Double.toString(this.latitude)),
                                             (Double.toString(this.longitude)));
                  }
                  String lineStr = "";
                  String eleStr = "";
                  setLine(this.line);
                  setElement(this.element);
                  if ((this.line >= 0) && (this.element >= 0)) {
                      lineStr =Integer.toString(this.line);
                      eleStr =Integer.toString(this.element);
                  }
                  centerLineFld    = new JTextField(lineStr, 3);
                  centerLineFld.addFocusListener(linEleFocusChange);
                  final String lineField = "";
                  centerElementFld = new JTextField(eleStr, 3);
                  centerElementFld.addFocusListener(linEleFocusChange);
                  final JButton centerPopupBtn =
                      GuiUtils.getImageButton(
                        "/auxdata/ui/icons/MapIcon16.png", getClass());
                  centerPopupBtn.setToolTipText("Center on current displays");

                  centerPopupBtn.addActionListener(new ActionListener() {
                      public void actionPerformed(ActionEvent ae) {
                              dataSource.getDataContext().getIdv().getIdvUIManager().popupCenterMenu(
                                  centerPopupBtn, latLonWidget);
                      }
                  });

                  JComponent centerPopup = GuiUtils.inset(centerPopupBtn,
                                             new Insets(0, 0, 0, 4));


                  GuiUtils.tmpInsets = dfltGridSpacing;
                  JTextField latFld = latLonWidget.getLatField();
                  JTextField lonFld = latLonWidget.getLonField();
                  latFld.addFocusListener(latLonFocusChange);
                  lonFld.addFocusListener(latLonFocusChange);
                  latLonPanel = GuiUtils.hbox(new Component[] {
                      centerLatLbl = GuiUtils.rLabel(" Lat:" + dfltLblSpacing),
                      latFld,
                      centerLonLbl = GuiUtils.rLabel(" Lon:" + dfltLblSpacing),
                      lonFld,
                      new JLabel(" "), centerPopup
                  });

                  lineElementPanel =
                      GuiUtils.hbox(new Component[] {
                          centerLineLbl =
                              GuiUtils.rLabel(" Line:" + dfltLblSpacing),
                          centerLineFld,
                          centerElementLbl = GuiUtils.rLabel(" Element:"
                              + dfltLblSpacing),
                          centerElementFld });

                  locationPanel = new GuiUtils.CardLayoutPanel();
                  locationPanel.addCard(latLonPanel);
                  locationPanel.addCard(lineElementPanel);

                  if (propComp != null) {
                      allComps.add(GuiUtils.rLabel(labelArray[propIdx]));
                      allComps.add(GuiUtils.left(propComp));
                  }
                  propComp = GuiUtils.hbox(new Component[] { locationPanel }, 1);
                  if (propComp != null) {
                      allComps.add(GuiUtils.rLabel("  "));
                      allComps.add(GuiUtils.left(propComp));
                  }
                  propComp = null;
              } else if (prop.equals(PROP_SIZE)) {
                  ActionListener sizeChange =new ActionListener() {
                      public void actionPerformed(ActionEvent ae) {
                          getGeoLocationInfo();
                      }
                  };
                  FocusListener sizeFocusChange = new FocusListener() {
                      public void focusGained(FocusEvent fe) {
                      }
                      public void focusLost(FocusEvent fe) {
                          getGeoLocationInfo();
                      }
                  };
                  setNumLines(this.numLines);
                  numLinesFld    = new JTextField(Integer.toString(this.numLines), 4);
                  numLinesFld.addActionListener(sizeChange);
                  numLinesFld.addFocusListener(sizeFocusChange);
                  setNumEles(this.numEles);
                  numElementsFld = new JTextField(Integer.toString(this.numEles), 4);
                  numElementsFld.addActionListener(sizeChange);
                  numElementsFld.addFocusListener(sizeFocusChange);
                  numLinesFld.setToolTipText("Number of lines");
                  numElementsFld.setToolTipText("Number of elements");
                  GuiUtils.tmpInsets = dfltGridSpacing;
                  sizeLbl            = GuiUtils.lLabel("");


                  fullResBtn = GuiUtils.makeImageButton(
                      "/auxdata/ui/icons/arrow_out.png", this,
                      "setToFullResolution");
                  fullResBtn.setContentAreaFilled(false);
                  fullResBtn.setToolTipText("Set to full resolution");

                  lockBtn =
                          GuiUtils.getToggleImageButton(IdvUIManager.ICON_UNLOCK,
                                          IdvUIManager.ICON_LOCK, 0, 0, true);
                  lockBtn.setContentAreaFilled(false);
                  lockBtn.setSelected(true);
                  lockBtn.setToolTipText(
                                  "Unlock to automatically change size when changing magnification");

                  lineMax = previewDirBlk[8] * previewDirBlk[11];
                  eleMax = previewDirBlk[9] * previewDirBlk[12];
                  JLabel rawSizeLbl = new JLabel(" Raw size: " + lineMax + " X " + eleMax);
                  JPanel sizePanel =
                      GuiUtils.left(GuiUtils.doLayout(new Component[] {
                          numLinesFld,
                          new JLabel(" X "), numElementsFld, sizeLbl, fullResBtn, lockBtn,
                          rawSizeLbl }, 7, GuiUtils.WT_N, GuiUtils.WT_N));
                  addPropComp(PROP_SIZE, propComp = sizePanel);
              } else if (prop.equals(PROP_MAG)) {
                  propComp = GuiUtils.hbox(new Component[] { new JLabel("") }, 1);
                  addPropComp(PROP_MAG, propComp);
              } else if (prop.equals(PROP_LMAG)) {
                  boolean oldAmSettingProperties = amSettingProperties;
                  amSettingProperties = true;
                  ChangeListener lineListener =
                      new javax.swing.event.ChangeListener() {
                      public void stateChanged(ChangeEvent evt) {
                          if (amSettingProperties) {
                              return;
                          }
                          lineMagSliderChanged(!lockBtn.isSelected());
                          getGeoLocationInfo();
                      }
                  };
                  JComponent[] lineMagComps =
                      GuiUtils.makeSliderPopup(SLIDER_MIN, SLIDER_MAX, 0,
                                               lineListener);
                  lineMagSlider = (JSlider) lineMagComps[1];
                  lineMagSlider.setPreferredSize(new Dimension(SLIDER_WIDTH,SLIDER_HEIGHT));
                  lineMagSlider.setMajorTickSpacing(1);
                  lineMagSlider.setSnapToTicks(true);
                  lineMagSlider.setExtent(1);
                  setLineMag(this.lineMag);
                  lineMagSlider.setValue(this.lineMag);
                  lineMagComps[0].setToolTipText(
                      "Change the line magnification");
                  lineMagSlider.setToolTipText(
                      "Slide to set line magnification factor");
                  String str = "Mag=" + Integer.toString(getLineMag());
                  lineMagLbl =
                      GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
                  str = truncateNumericString(Double.toString(baseLRes*Math.abs(getLineMag())), 1);
                  str = " Res=" + str + kmLbl;
                  lineResLbl =
                      GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
                  amSettingProperties = oldAmSettingProperties;

                  GuiUtils.tmpInsets  = dfltGridSpacing;
                  lMagPanel = GuiUtils.doLayout(new Component[] {
                                        lineMagLbl,
                                        GuiUtils.inset(lineMagComps[1],
                                            new Insets(0, 4, 0, 0)), lineResLbl, }, 4,
                                                GuiUtils.WT_N, GuiUtils.WT_N);
                  propComp = GuiUtils.hbox(new Component[] { new JLabel(" "), lMagPanel }, 2);
                  addPropComp(PROP_LMAG, propComp = lMagPanel);
              } else if (prop.equals(PROP_EMAG)) {
                  boolean oldAmSettingProperties = amSettingProperties;
                  amSettingProperties = true;
                  ChangeListener elementListener = new ChangeListener() {
                      public void stateChanged(
                              javax.swing.event.ChangeEvent evt) {
                          if (amSettingProperties) {
                              return;
                          }
                          elementMagSliderChanged(true);
                          getGeoLocationInfo();
                      }
                  };
                  JComponent[] elementMagComps =
                      GuiUtils.makeSliderPopup(SLIDER_MIN, SLIDER_MAX, 0,
                                               elementListener);
                  elementMagSlider = (JSlider) elementMagComps[1];
                  elementMagSlider.setPreferredSize(new Dimension(SLIDER_WIDTH,SLIDER_HEIGHT));
                  elementMagSlider.setExtent(1);
                  elementMagSlider.setMajorTickSpacing(1);
                  elementMagSlider.setSnapToTicks(true);
                  setElementMag(this.elementMag);
                  elementMagSlider.setValue(this.elementMag);
                  elementMagComps[0].setToolTipText(
                      "Change the element magnification");
                  elementMagSlider.setToolTipText(
                      "Slide to set element magnification factor");
                  String str = "Mag=" + Integer.toString(getElementMag());
                  elementMagLbl =
                      GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
                  str = truncateNumericString(Double.toString(baseERes*Math.abs(getElementMag())), 1);
                  str = " Res=" + str + kmLbl;
                  elementResLbl =
                      GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
                  amSettingProperties = oldAmSettingProperties;

                  GuiUtils.tmpInsets  = dfltGridSpacing;
                  eMagPanel = GuiUtils.doLayout(new Component[] {
                                        elementMagLbl,
                                        GuiUtils.inset(elementMagComps[1],
                                            new Insets(0, 4, 0, 0)), elementResLbl, }, 4,
                                                GuiUtils.WT_N, GuiUtils.WT_N);
                  propComp = GuiUtils.hbox(new Component[] { new JLabel(" "), eMagPanel }, 2);
                  addPropComp(PROP_EMAG, propComp = eMagPanel);
              } else if (prop.equals(PROP_READOUT)) {
                  allComps.add(new JLabel(" "));
                  allComps.add(new JLabel(" "));

                  for (int i=0; i<5; i++) {
                      LatLonWidget llw = new LatLonWidget();
                      readoutLLWidget.add(llw);
                      JTextField lat = (JTextField)llw.getLatField();
                      JTextField lon = (JTextField)llw.getLonField();
                      lat.setEditable(false);
                      lon.setEditable(false);
                      readoutLatFld.add(lat);
                      readoutLonFld.add(lon);
                  }

                  List latLbl = new ArrayList();
                  List lonLbl = new ArrayList();
                  for (int i=0; i<5; i++)
                      lonLbl.add(GuiUtils.getFixedWidthLabel(" Lon: "));
                  latLbl.add(GuiUtils.getFixedWidthLabel("Center      Lat: "));
                  latLbl.add(GuiUtils.getFixedWidthLabel("Upper Left  Lat: "));
                  latLbl.add(GuiUtils.getFixedWidthLabel("Upper Right Lat: "));
                  latLbl.add(GuiUtils.getFixedWidthLabel("Lower Left  Lat: "));
                  latLbl.add(GuiUtils.getFixedWidthLabel("Lower Right Lat: "));

                  JPanel[] panels = new JPanel[5];
                  for (int i=0; i<5; i++) {
                      Component[] comps = new Component[] { 
                      (Component)latLbl.get(i),
                      (Component)readoutLatFld.get(i), 
                      (Component)lonLbl.get(i),
                      (Component)readoutLonFld.get(i)};
                      panels[i] = GuiUtils.left(GuiUtils.doLayout(comps,
                      4, GuiUtils.WT_N, GuiUtils.WT_N));
                  }
                  latLonReadoutPanel = GuiUtils.left(GuiUtils.doLayout(
                      panels, 1, GuiUtils.WT_N, GuiUtils.WT_Y));

                  for (int i=0; i<5; i++) {
                      JTextField imageLin = new JTextField("", 4);
                      JTextField imageEle = new JTextField("", 4);
                      imageLin.setEditable(false);
                      imageEle.setEditable(false);
                      readoutImageLinFld.add(imageLin);
                      readoutImageEleFld.add(imageEle);

                      JTextField areaLin = new JTextField("", 4);
                      JTextField areaEle = new JTextField("", 4);
                      areaLin.setEditable(false);
                      areaEle.setEditable(false);
                      readoutAreaLinFld.add(areaLin);
                      readoutAreaEleFld.add(areaEle);
                  }

                  List linLbl = new ArrayList();
                  List eleLbl = new ArrayList();
                  for (int i=0; i<5; i++)
                      eleLbl.add(GuiUtils.getFixedWidthLabel(" Element: "));
                  linLbl.add(GuiUtils.getFixedWidthLabel("Center      Line: "));
                  linLbl.add(GuiUtils.getFixedWidthLabel("Upper Left  Line: "));
                  linLbl.add(GuiUtils.getFixedWidthLabel("Upper Right Line: "));
                  linLbl.add(GuiUtils.getFixedWidthLabel("Lower Left  Line: "));
                  linLbl.add(GuiUtils.getFixedWidthLabel("Lower Right Line: "));
                  panels = new JPanel[5];
                  for (int i=0; i<5; i++) {
                      Component[] comps = new Component[] { 
                      (Component)linLbl.get(i),
                      (Component)readoutImageLinFld.get(i), 
                      (Component)eleLbl.get(i),
                      (Component)readoutImageEleFld.get(i)};
                      panels[i] = GuiUtils.left(GuiUtils.doLayout(comps,
                      4, GuiUtils.WT_N, GuiUtils.WT_N));
                  }
                  lineElementImageReadoutPanel = GuiUtils.left(GuiUtils.doLayout(
                      panels, 1, GuiUtils.WT_N, GuiUtils.WT_Y));

                  linLbl = new ArrayList();
                  eleLbl = new ArrayList();
                  for (int i=0; i<5; i++)
                      eleLbl.add(GuiUtils.getFixedWidthLabel(" Element: "));
                  linLbl.add(GuiUtils.getFixedWidthLabel("Center      Line: "));
                  linLbl.add(GuiUtils.getFixedWidthLabel("Upper Left  Line: "));
                  linLbl.add(GuiUtils.getFixedWidthLabel("Upper Right Line: "));
                  linLbl.add(GuiUtils.getFixedWidthLabel("Lower Left  Line: "));
                  linLbl.add(GuiUtils.getFixedWidthLabel("Lower Right Line: "));
                  panels = new JPanel[5];
                  for (int i=0; i<5; i++) {
                      Component[] comps = new Component[] {
                      (Component)linLbl.get(i),
                      (Component)readoutAreaLinFld.get(i),
                      (Component)eleLbl.get(i),
                      (Component)readoutAreaEleFld.get(i)};
                      panels[i] = GuiUtils.left(GuiUtils.doLayout(comps,
                      4, GuiUtils.WT_N, GuiUtils.WT_N));
                  }
                  lineElementAreaReadoutPanel = GuiUtils.left(GuiUtils.doLayout(
                      panels, 1, GuiUtils.WT_N, GuiUtils.WT_Y));

                  readoutPanel = new GuiUtils.CardLayoutPanel();
                  readoutPanel.addCard(latLonReadoutPanel);
                  readoutPanel.addCard(lineElementImageReadoutPanel);
                  readoutPanel.addCard(lineElementAreaReadoutPanel);

                  propComp = GuiUtils.hbox(new Component[] { readoutPanel }, 1);
                  addPropComp(PROP_READOUT, propComp);
                  if (propComp != null) {
                      allComps.add(GuiUtils.rLabel(labelArray[propIdx]));
                      allComps.add(GuiUtils.left(propComp));
                  }
                  propComp = null;
              }
              if (propComp != null) {
                  allComps.add(GuiUtils.rLabel(labelArray[propIdx]));
                  allComps.add(GuiUtils.left(propComp));
              }
          }
          GuiUtils.tmpInsets = GRID_INSETS;
          JPanel imagePanel = GuiUtils.doLayout(allComps, 2, GuiUtils.WT_NY,
                                  GuiUtils.WT_N);
          getGeoLocationInfo();
          return GuiUtils.top(imagePanel);
      }

      private void updateReadout() {
          for (int i=0; i<5; i++) {
              LatLonWidget llw = (LatLonWidget)readoutLLWidget.get(i);
              llw.setLatLon(latLon[0][i], latLon[1][i]);
          }
          String plc = getPlace();
          int pos = 0;
          if (plc.equals(PLACE_ULEFT)) pos = 1;
          setLatitude(latLon[0][pos]);
          setLongitude(latLon[1][pos]);
          for (int i=0; i<5; i++) {
              JTextField linField = (JTextField)readoutImageLinFld.get(i);
              Double dbl = new Double(imageEL[1][i]);
              if (!dbl.isNaN()) 
                  linField.setText(trimCoord(imageEL[1][i]));
              else
                  linField.setText(Misc.MISSING);
              JTextField eleField = (JTextField)readoutImageEleFld.get(i);
              dbl = new Double(imageEL[0][i]);
              if (!dbl.isNaN()) 
                  eleField.setText(trimCoord(imageEL[0][i]));
              else
                  eleField.setText(Misc.MISSING);
          }
          for (int i=0; i<5; i++) {
              JTextField linField = (JTextField)readoutAreaLinFld.get(i);
              Double dbl = new Double(areaEL[1][i]);
              if (!dbl.isNaN()) 
                  linField.setText(trimCoord(areaEL[1][i]));
              else
                  linField.setText(Misc.MISSING);
              JTextField eleField = (JTextField)readoutAreaEleFld.get(i);
              dbl = new Double(areaEL[0][i]);
              if (!dbl.isNaN()) 
                  eleField.setText(trimCoord(areaEL[0][i]));
              else
                  eleField.setText(Misc.MISSING);
          }
          String type = getCoordinateType();
          if (type.equals(TYPE_IMAGE)) {
              setElement((int)(imageEL[0][pos] + 0.5));
              setLine((int)(imageEL[1][pos] + 0.5));
          } else if (type.equals(TYPE_AREA)) {
              setElement((int)(areaEL[0][pos] + 0.5));
              setLine((int)(areaEL[1][pos] + 0.5));
          }
      }

      private String trimCoord(double val) {
          int ival = (int)Math.floor(val + 0.5);
          return Integer.toString(ival);
      }

      /**
       * Change coordinate type panel
       */
      protected void flipLocationPanel(int locPanel) {
          int nowPlaying = locationPanel.getVisibleIndex();
          if (locPanel > 0) {
              if (nowPlaying == 0) {
                  locationPanel.flip();
              }
              setIsLineEle(true);
              String type = getCoordinateType();
              String loc = getPlace();
              int indx = 0;
              if (loc.equals(PLACE_ULEFT)) indx = 1;
              int ele = 0;
              int lin = 0;
              if (type.equals(TYPE_IMAGE)) {
                  ele = (int)Math.floor(imageEL[0][indx] + 0.5);
                  lin = (int)Math.floor(imageEL[1][indx] + 0.5);
              } else if (type.equals(TYPE_AREA)) {
                  ele = (int)Math.floor(areaEL[0][indx] + 0.5);
                  lin = (int)Math.floor(areaEL[1][indx] + 0.5);
              }
              setElement(ele);
              setLine(lin);
          } else {
              if (nowPlaying > 0) locationPanel.flip();
              setIsLineEle(false);
          }
      }

      /**
       * Change readout type panel
       */
      protected void flipReadoutPanel(int roPanel) {
          readoutPanel.show(roPanel);
      }

      /**
       * Set to full resolution
       */
      public void setToFullResolution() {
          amSettingProperties = true;
          setIsLineEle(true);
          coordinateTypeComboBox.setSelectedItem(TYPE_IMAGE);
          setPlace(PLACE_CENTER);
          setLatitude(new Double(previewDir.getCenterLatitude()));
          setLongitude(new Double(previewDir.getCenterLongitude()));
          convertToLinEle();

          setNumLines(previewDirBlk[8]);
          setNumEles(previewDirBlk[9]);
          setLineMag(1);
          setDLineMag(1.0);
          setElementMag(1);
          setDElementMag(1.0);
          lineMagSlider.setValue(1);
          setLRes(-1.0);
          lineMagSliderChanged(false);
          elementMagSlider.setValue(1);
          setERes(-1.0);
          elementMagSliderChanged(false);
          getGeoLocationInfo();
          amSettingProperties = false;
      }

      public void applyToDataSelection(DataSelection dataSelection) {

         if (dataSelection == null) {
             dataSelection = new DataSelection(true);
         }

         if (geoLocInfo == null) getGeoLocationInfo();
         GeoLocationInfo geoInfo = geoLocInfo;
         if (geoInfo == null) {
             dataSelection = null;
             return;
         }

         String coordType = getCoordinateType();

         if (!isLineEle) {
             double lat = getLatitude();
             double lon = getLongitude();
             if (lat > 90.0 && lon> 360.0) {
                 convertToLatLon();
                 lat = getLatitude();
                 lon = getLongitude();
             }
             String latString = Double.toString(lat);
             if (latString.length()>8)
                 latString = latString.substring(0,7);
             String lonString = Double.toString(lon);
             if (lonString.length()>9)
                 lonString = lonString.substring(0,8);
             dataSelection.putProperty(PROP_LATLON, (latString + " " + lonString));
         } else {
             String typeStr = " I";
             int lin = getLine();
             int ele = getElement();
             if (coordType.equals(TYPE_AREA)) typeStr = " F";
             String linString = Integer.toString(lin);
             String eleString = Integer.toString(ele);
             dataSelection.putProperty(PROP_LINEELE, (linString + " " + eleString + typeStr));
         }

         dataSelection.putProperty(PROP_PLACE, getPlace());
         dataSelection.putProperty(PROP_MAG, (getLineMag() + " " + getElementMag()));

         GeoSelection geoSelection = new GeoSelection(geoInfo);
         dataSelection.setGeoSelection(geoSelection);

         int nlins = getNumLines();
         int neles = getNumEles();
         if (nlins > 0 && neles > 0) {
             dataSelection.putProperty(PROP_SIZE, (nlins + " " + neles));
         }
         dataChoice.setDataSelection(dataSelection);
         this.dataSource.setDataSelection(dataSelection);
    }

    public GeoLocationInfo getGeoLocationInfo() {
        geoLocInfo = null;
        double[][] el = convertToDisplayCoords();
        int ele = (int)Math.floor(el[0][0] + 0.5);
        if (ele < 1) ele = 1;
        int lin = (int)Math.floor(el[1][0] + 0.5);
        if (lin < 1) lin = 1;
        geoLocInfo = getGeoLocationInfo(lin, ele);
        return geoLocInfo;
    }

    protected GeoLocationInfo getGeoLocationInfo(int lin, int ele) {
        int nLin = getNumLines();
        if (nLin > 0) {
            int nEle = getNumEles();
            if (nEle > 0) {
                int lMag = getLineMag();
                if (lMag > 1) return geoLocInfo;
                int eMag = getElementMag();
                if (eMag > 1) return geoLocInfo;
                geoLocInfo = makeGeoLocationInfo(lin, ele, nLin, nEle,
                             lMag, eMag);
            }
        }
        return geoLocInfo;
    }

    private GeoLocationInfo makeGeoLocationInfo(int lin, int ele, int nlins, int neles,
                            int linMag, int eleMag) {

         geoLocInfo = null;

         String plc = getPlace();

         AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
         Rectangle2D mapArea = macs.getDefaultMapArea();
         double previewXDim = mapArea.getWidth();
         double previewYDim = mapArea.getHeight();

         double dLine = (double)nlins/(2.0*this.previewLineRes)*Math.abs(linMag);
         double dEle = (double)neles/(2.0*this.previewEleRes)*Math.abs(eleMag);

         if (plc.equals(PLACE_CENTER)) {
             displayEL[0][0] = ele;
             displayEL[1][0] = lin;
             displayEL[0][1] = ele - dEle;
             if (displayEL[0][1] < 0) displayEL[0][1] = 0.0;
             displayEL[1][1] = lin + dLine;
             if (displayEL[1][1] > previewYDim) displayEL[1][1] = previewYDim;
         } else if (plc.equals(PLACE_ULEFT)) {
             displayEL[0][0] = ele + dEle;
             if (displayEL[0][0] > previewXDim) displayEL[0][0] = previewXDim;
             displayEL[1][0] = lin - dLine;
             if (displayEL[1][0] < 0) displayEL[1][0] = 0.0;
             displayEL[0][1] = ele;
             displayEL[1][1] = lin;
         }
         int cEle = (int)Math.ceil(displayEL[0][0]);
         int cLin = (int)Math.ceil(displayEL[1][0]);
         displayEL[0][2] = cEle + dEle;
         if (displayEL[0][2] > previewXDim) displayEL[0][2] = previewXDim;
         displayEL[1][2] = cLin + dLine;
         if (displayEL[1][2] > previewYDim) displayEL[1][2] = previewYDim;
         displayEL[0][3] = cEle - dEle;
         if (displayEL[0][3] < 0) displayEL[0][3] = 0.0;
         displayEL[1][3] = cLin - dLine;
         if (displayEL[1][3] < 0) displayEL[1][3] = 0.0;
         displayEL[0][4] = cEle + dEle;
         if (displayEL[0][4] > previewXDim) displayEL[0][4] = previewXDim;
         displayEL[1][4] = cLin - dLine;
         if (displayEL[1][4] < 0) displayEL[1][4] = 0.0;
/*
         System.out.println("\nDisplay:");
         System.out.println("    0: " + displayEL[1][0] + " " + displayEL[0][0]);
         System.out.println("    1: " + displayEL[1][1] + " " + displayEL[0][1]);
         System.out.println("    2: " + displayEL[1][2] + " " + displayEL[0][2]);
         System.out.println("    3: " + displayEL[1][3] + " " + displayEL[0][3]);
         System.out.println("    4: " + displayEL[1][4] + " " + displayEL[0][4]);
*/
         areaEL = displayCoordToAreaCoord(displayEL);
/*
         System.out.println("\nArea:");
         System.out.println("    0: " + areaEL[1][0] + " " + areaEL[0][0]);
         System.out.println("    1: " + areaEL[1][1] + " " + areaEL[0][1]);
         System.out.println("    2: " + areaEL[1][2] + " " + areaEL[0][2]);
         System.out.println("    3: " + areaEL[1][3] + " " + areaEL[0][3]);
         System.out.println("    4: " + areaEL[1][4] + " " + areaEL[0][4]);
*/
         try {
             latLon = macs.toReference(displayEL);
         } catch (Exception e) {
             System.out.println("Error converting input lat/lon e=" + e);
         }
/*
         System.out.println("\nLat/Lon:");
         System.out.println("    0: " + latLon[0][0] + " " + latLon[1][0]);
         System.out.println("    1: " + latLon[0][1] + " " + latLon[1][1]);
         System.out.println("    2: " + latLon[0][2] + " " + latLon[1][2]);
         System.out.println("    3: " + latLon[0][3] + " " + latLon[1][3]);
         System.out.println("    4: " + latLon[0][4] + " " + latLon[1][4]);
*/
         double maxLat = latLon[0][1];
         if (latLon[0][2] > maxLat) maxLat = latLon[0][2];
         double minLat = latLon[0][3];
         if (latLon[0][4] < minLat) minLat = latLon[0][4];
         double maxLon = latLon[1][4];
         if (latLon[1][2] > maxLon) maxLon = latLon[1][2];
         double minLon = latLon[1][1];
         if (latLon[1][3] < minLon) minLon = latLon[1][3];

         imageEL = previewNav.areaCoordToImageCoord(areaEL);
/*
         System.out.println("\nImage:");
         System.out.println("    0: " + imageEL[1][0] + " " + imageEL[0][0]);
         System.out.println("    1: " + imageEL[1][1] + " " + imageEL[0][1]);
         System.out.println("    2: " + imageEL[1][2] + " " + imageEL[0][2]);
         System.out.println("    3: " + imageEL[1][3] + " " + imageEL[0][3]);
         System.out.println("    4: " + imageEL[1][4] + " " + imageEL[0][4]);
*/

         updateReadout();

         geoLocInfo = new GeoLocationInfo(maxLat, minLon, minLat, maxLon);

         return geoLocInfo;
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
     * Cycle the place
     */
    public void cyclePlace() {
        String type = getCoordinateType();
        double finagle = 0.5;
        if (this.place.equals(PLACE_CENTER)) {
            if (type.equals(TYPE_IMAGE)) {
                setLine((int)(imageEL[1][0] + finagle));
                setElement((int)(imageEL[0][0] + finagle));
            } else if (type.equals(TYPE_AREA)) {
                setLine((int)(areaEL[1][0] + finagle));
                setElement((int)(areaEL[0][0] + finagle));
            }
            setLatitude(latLon[0][0]);
            setLongitude(latLon[1][0]);
        } else {
            if (type.equals(TYPE_IMAGE)) {
                setLine((int)(imageEL[1][1] + finagle));
                setElement((int)(imageEL[0][1] + finagle));
            } else if (type.equals(TYPE_AREA)) {
                setLine((int)(areaEL[1][1] + finagle));
                setElement((int)(areaEL[0][1] + finagle));
            }
            setLatitude(latLon[0][1]);
            setLongitude(latLon[1][1]);
        }
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
     * Translate a place name into a human readable form
     *
     * @param place raw name
     *
     * @return human readable name
     */
    protected String translatePlace(String thisPlace) {
        if (thisPlace.equals("Upper Left")) {
            return PLACE_ULEFT;
        }
        if (thisPlace.equals("Center")) {
            return PLACE_CENTER;
        }
        return thisPlace;
    }

    private void setNumberOfLines(int val) {
        numLinesFld.setText(new Integer(val).toString());
    }

    private void setNumberOfElements(int val) {
        numElementsFld.setText(new Integer(val).toString());
    }

    public String getPlace() {
        try {
            this.place = translatePlace((String)locationComboBox.getSelectedItem());
        } catch (Exception e) {
            this.place = defaultPlace;
        }
        return this.place;
    }

    public void setPlace(String str) {
        if (str.equals("")) str = defaultPlace;
        this.place = str;
        if (str.equals(PLACE_CENTER))
            locationComboBox.setSelectedItem("Center");
        else
            locationComboBox.setSelectedItem("Upper Left");
    }

    public int getNumLines() {
        int val = -1;
        try {
            val = new Integer(numLinesFld.getText().trim()).intValue();
        } catch (Exception e) {
            System.out.println("=====> exception in getNumLines: e=" + e);
            if (val < 1) val = defaultNumLines;
        }
        setNumLines(val);
        return this.numLines;
    }

    public void setNumLines(int val) {
        this.numLines = val;
        if (val > 0) setNumberOfLines(val);
    }

    public int getNumEles() {
        int val = -1;
        try {
            val = new Integer(numElementsFld.getText().trim()).intValue();
        } catch (Exception e) {
            System.out.println("=====> exception in getNumEles: e=" + e);
            if (val < 1) val = defaultNumEles;
        }
        setNumEles(val);
        return this.numEles;
    }

    public void setNumEles(int val) {
        val = (int)((double)val/4.0 + 0.5)*4;
        this.numEles = val;
        if (val > 0) setNumberOfElements(val);
    }

    public int getLine() {
        int val = -1;
        try {
            val = new Integer(centerLineFld.getText().trim()).intValue();
        } catch (Exception e) {
        }
        if (val < 0) val = defaultLine;
        setLine(val);
        return this.line;
    }

    private void setLine() {
        this.line = getLine();
    }

    public void setLine(int val) {
        if (val < 0) val = defaultLine;
        centerLineFld.setText(new Integer(val).toString());
        this.line = val;
    }

    private void setElement() {
        this.element = getElement();
    }

    public int getElement() {
        int val =-1;
        try {
            val = new Integer(centerElementFld.getText().trim()).intValue();
        } catch (Exception e) {
        }
        if (val < 0) val = defaultElement;
        setElement(val);
        return this.element;
    }

    public void setElement(int val) {
        if (val < 0) val = defaultElement;
        centerElementFld.setText(new Integer(val).toString());
        this.element = val;
    }

    public int getLineMag() {
        return this.lineMag;
    }

    public void setLineMag(int val) {
        if (val > 1) val = defaultLineMag;
        if (val == -1) val = 1;
        this.lineMag = val;
    }

    public int getElementMag() {
        return this.elementMag;
    }

    public void setDLineMag(double val) {
        this.dLineMag = val;
    }

    public double getDLineMag() {
        return this.dLineMag;
    }

    public void setDElementMag(double val) {
        this.dElementMag = val;
    }

    public double getDElementMag() {
        return this.dElementMag;
    }

    public void setElementMag(int val) {
        if (val > 1) val = defaultElementMag;
        if (val == -1) val = 1;
        this.elementMag = val;
    }

    public double getLatitude() {
        double val = latLonWidget.getLat();
        Double dbl = new Double(val);
        if (dbl.isNaN()) val = defaultLat;
        if (val < -90.0 || val > 90.0) val = defaultLat;
        setLatitude(val);
        return this.latitude;
    }

    private void setLatitude() {
        this.latitude = latLonWidget.getLat();
    }

    public void setLatitude(double val) {
        if (val < -90.0 || val > 90.0) {
            val = defaultLat;
            return;
        }
        latLonWidget.setLat(val);
        this.latitude = val;
        this.resetLatLon = false;
    }

    private void setLongitude() {
        this.longitude = latLonWidget.getLon();
    }

    public double getLongitude() {
        double val = latLonWidget.getLon();
        Double dbl = new Double(val);
        if (dbl.isNaN()) val = defaultLon;
        if (val < -180.0 || val > 180.0) val = defaultLon;
        setLongitude(val);
        return this.longitude;
    }

    public void setLongitude(double val) {
        if (val < -180.0 || val > 180.0) {
            val = defaultLon;
            return;
        }
        latLonWidget.setLon(val);
        this.longitude = val;
        this.resetLatLon = false;
    }

    protected void convertToLatLon() {
        double[][] el = new double[2][1];
        el[0][0] = getElement();
        el[1][0] = getLine();
        double[][] ll = new double[2][1];
        String coordType = getCoordinateType();
        if (coordType.equals(TYPE_IMAGE))
            el = previewNav.imageCoordToAreaCoord(el);

        try {
            AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
            ll = macs.toReference(el);
            setLatitude(ll[0][0]);
            setLongitude(ll[1][0]);
            getGeoLocationInfo();
        } catch (Exception e) {
            System.out.println("convertToLatLon e=" + e);
        }
    }

    protected void convertToLatLon(int ele, int lin) {
        try {
            double[][] el = new double[2][1];
            double[][] ll = new double[2][1];
            AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
            el[0][0] = (double)ele;
            el[1][0] = (double)lin;
            ll = macs.toReference(el);
            setLatitude(ll[0][0]);
            setLongitude(ll[1][0]);
            double[][] imageLE = new double[2][1];
            double[][] areaLE = new double[2][1];
            areaLE = previewNav.toLinEle(ll);
            imageLE = previewNav.areaCoordToImageCoord(areaLE);
            setCenterCoords((int)imageLE[0][0], (int)imageLE[1][0]);
            getGeoLocationInfo();
        } catch (Exception e) {
            System.out.println("convertToLatLon e=" + e);
        }
    }

    protected double[][] convertToDisplayCoords() {
        double[][] el = new double[2][1];
        try {
            double[][] ll = new double[2][1];
            AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
            String type = getCoordinateType();
            int pos = 0;
            if (getPlace().equals(PLACE_ULEFT)) pos = 1;
            if (type.equals(TYPE_LATLON)) {
                ll[0][pos] = getLatitude();
                ll[1][pos] = getLongitude();
                el = macs.fromReference(ll);
            } else {
                el[0][pos] = (double)getElement();
                el[1][pos] = (double)getLine();
                int[] dirB = macs.getDirBlock();
                int previewLineMag = dirB[11];
                int previewEleMag = dirB[12];
                if (type.equals(TYPE_IMAGE))
                    el = areaNav.imageCoordToAreaCoord(el);
                Rectangle2D mapArea = macs.getDefaultMapArea();
                int previewXDim = new Long(new Double(mapArea.getMaxX() - mapArea.getMinX()).longValue()).intValue();
                int previewYDim = new Long(new Double(mapArea.getMaxY() - mapArea.getMinY()).longValue()).intValue();
                el[0][pos] = el[0][pos] / previewEleMag;
                el[1][pos] = previewYDim - 1 - el[1][pos] / previewLineMag;
            }
        } catch (Exception e) {
            System.out.println("convertToDisplayCoords e=" + e);
        }
        return el;
    }

    private double[][] displayCoordToAreaCoord(double[][] disp) {
        double[][] area = new double[2][disp[0].length];
        try {
            if (sampleProjection != null) {
                AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
                double[][] ll = macs.toReference(disp);
                double[][] el = areaNav.toLinEle(ll);
                int midEle = (int)Math.floor(el[0][0] + 0.5);
                int midLin = (int)Math.floor(el[1][0] + 0.5);

                int width = (int)Math.floor(Math.abs(disp[0][2] - disp[0][1])
                            * getPreviewEleRes() + 0.5);

                int height = (int)Math.floor(Math.abs(disp[1][3] - disp[1][1]) 
                            * getPreviewLineRes() + 0.5);
                int deltaEle = width/2;
                int deltaLin = height/2;

                area[0][0] = midEle;
                area[1][0] = midLin;
                area[0][1] = midEle - deltaEle;
                area[1][1] = midLin - deltaLin;
                area[0][2] = midEle + deltaEle;
                area[1][2] = midLin - deltaLin;
                area[0][3] = midEle - deltaEle;
                area[1][3] = midLin + deltaLin;
                area[0][4] = midEle + deltaEle;
                area[1][4] = midLin + deltaLin;

            }
        } catch (Exception e) {
            System.out.println("displayCoordToAreaCoord e=" + e);
        }
        return area;
    }

    private double[][] areaCoordToDisplayCoord(double[][] area) {
        double[][] disp = new double[2][area[0].length];
        try {
            if (sampleProjection != null) {
                AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
                int[] dirB = macs.getDirBlock();
                int previewLineMag = dirB[11];
                int previewEleMag = dirB[12];
                Rectangle2D mapArea = macs.getDefaultMapArea();
                int previewXDim = new Long(new Double(mapArea.getMaxX() - mapArea.getMinX()).longValue()).intValue();
                int previewYDim = new Long(new Double(mapArea.getMaxY() - mapArea.getMinY()).longValue()).intValue();
                for (int i=0; i<area[0].length; i++) {
                    disp[0][i] = area[0][i] / previewEleMag;
                    disp[1][i] = previewYDim - 1 - area[1][i] / previewLineMag;
                }
            }
        } catch (Exception e) {
            System.out.println("displayCoordToAreaCoord e=" + e);
        }
        return disp;
    }

    protected void convertToLinEle() {
        try {
            double[][] el = new double[2][1];
            double[][] ll = new double[2][1];
            AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
            ll[0][0] = getLatitude();
            ll[1][0] = getLongitude();
            String coordType = getCoordinateType();
            el = this.previewNav.toLinEle(ll);
            if (coordType.equals(TYPE_IMAGE))
                el = this.previewNav.areaCoordToImageCoord(el);
            setLine((int)el[1][0]);
            setElement((int)el[0][0]);
            getGeoLocationInfo();
        } catch (Exception e) {
            System.out.println("convertToLinEle e=" + e);
        }
    }

    protected String getCoordinateType() {
        String ret = defaultType;
        try {
            ret = (String)coordinateTypeComboBox.getSelectedItem();
        } catch (Exception e) {
        }
        return ret;
    }

    protected void setCoordinateType(String type) {
        if (!type.equals(TYPE_IMAGE)) {
            if (!type.equals(TYPE_AREA)) {
                type = TYPE_LATLON;
            }
        }
        coordinateTypeComboBox.setSelectedItem(type);
    }

    protected void setLockOn(boolean val) {
        lockBtn.setSelected(val);
    }

    protected boolean getLockOn() {
        return lockBtn.isSelected();
    }
        
    protected void setULCoords(double x, double y) {
        uLLine = (int)y;
        uLEle = (int)x;
    }
 
    protected void setCenterCoords(int x, int y) {
        centerLine = y;
        setLine(y);
        centerEle = x;
        setElement(x);
    }

    protected void elementMagSliderChanged(boolean recomputeLineEleRatio) {
        int value = getElementMag();
        if (!amUpdating) {
          value = getElementMagValue();
          setElementMag(value);
          setDElementMag((double)value);
        }
        double eVal = this.eRes;
        if (value < 0) eVal *= Math.abs(value);
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
        elementMagLbl.setText(StringUtil.padLeft("Mag=" + value, 4));
        String str = " Res=" +
            truncateNumericString(Double.toString(baseERes*Math.abs(value)), 1);
        elementResLbl.setText(StringUtil.padLeft(str, 4) + kmLbl);

        if (!lockBtn.isSelected()) {
            if (value > 0) {
                numElementsFld.setText(""
                                       + (int) (this.baseNumElements * value));
            } else {
                numElementsFld.setText(""
                                       + (int) (this.baseNumElements
                                                / (double) -value));
            }
        }
    }

    /**
     * Handle the line mag slider changed event 
     *
     * @param evt  the event
     */
    protected void lineMagSliderChanged(boolean autoSetSize) {
        try {
            int value = getLineMag();
            if (!amUpdating) {
                value = getLineMagValue();
                setLineMag(value);
                setDLineMag((double)value);
            }
            double lVal = this.lRes;
            if (value < 0) lVal *= Math.abs(value);
            lineMagLbl.setText(StringUtil.padLeft("Mag=" + value, 4));
            String str = " Res=" +
                truncateNumericString(Double.toString(baseLRes*Math.abs(value)), 1);
            lineResLbl.setText(StringUtil.padLeft(str, 4) + kmLbl);

            if (autoSetSize) {
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
            System.out.println("Setting line magnification" + exc);
        }
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
     * _more_
     *
     * @param el _more_
     * @param name _more_
     * @param listener _more_
     *
     * @return _more_
     */

    private JMenuItem makeLocationMenuItem(final LatLonPoint llp,
                                           final String name) {
        JMenuItem mi = null;
        try {
            double alt = 0.0;
            EarthLocationTuple elt = 
                new EarthLocationTuple(llp.getLatitude(), llp.getLongitude(), alt);
            mi =
            new JMenuItem(
                StringUtil.padRight(name + ": ", 15, " ")
                + dataSource.getDataContext().getIdv().getDisplayConventions()
                .formatLatLonPoint(elt.getLatLonPoint()));
            GuiUtils.setFixedWidthFont(mi);
        } catch (Exception e) {
            System.out.println("makeLocationMenuItem e=" + e);
        }
        return mi;
    }


    public boolean getIsLineEle() {
        return this.isLineEle;
    }

    public void setIsLineEle(boolean val) {
        this.isLineEle = val;
    }


    public double getLRes() {
        return this.lRes;
    }

    public void setLRes(double val) {
        if (val < 1) val = baseLRes;
        this.lRes = val;
    }

    public double getERes() {
        return this.eRes;
    }

    public void setERes(double val) {
        if (val < 1) val = baseERes;
        this.eRes = val;
    }

    public int getPreviewLineRes() {
        return this.previewLineRes;
    }

    public void setPreviewLineRes(int val) {
        this.previewLineRes = val;
    }

    public int getPreviewEleRes() {
        return this.previewEleRes;
    }

    public void setPreviewEleRes(int val) {
        this.previewEleRes = val;
    }

    private String truncateNumericString(String str, int numDec) {
        int indx = str.indexOf(".") + numDec + 1;
        return str.substring(0,indx);
    }

    protected double[][] getLatLonPoints() {
        return latLon;
    }

    protected double[][] getEleLinPoints() {
        return imageEL;
    }

    protected double[][] getDisplayELPoints() {
        return displayEL;
    }

    protected double getBaseLRes() {
        return baseLRes;
    }

    protected double getBaseERes() {
        return baseERes;
    }

    protected void update(AreaDirectory dir, MapProjection sample, AREAnav nav, 
                          String coordType, double[] coords) {

        sampleProjection = sample;

        double baseLResOld = getBaseLRes();
        double baseEResOld = getBaseERes();
        double lDMagOld = getDLineMag();
        double eDMagOld = getDElementMag();
        int lMagOld = getLineMag();
        int eMagOld = getElementMag();
        int lSizeOld = getNumLines();
        int eSizeOld = getNumEles();

        setCoordinateType(coordType);
        if (coordType.equals(TYPE_LATLON)) {
            setLatitude(coords[0]);
            setLongitude(coords[1]);
        } else {
            setLine((int)coords[0]);
            setElement((int)coords[1]);
        }

        double baseLResNew = dir.getCenterLatitudeResolution();
        double baseEResNew = dir.getCenterLongitudeResolution();

        double lDMagNew = lDMagOld * baseLResOld / baseLResNew;
        int lMagNew = (int)Math.ceil(lDMagNew - 0.5);
        if (lMagNew > -2) lMagNew = 1;
        double eDMagNew = eDMagOld * baseEResOld / baseEResNew;
        int eMagNew = (int)Math.ceil(eDMagNew - 0.5);
        if (eMagNew > -2) eMagNew = 1;

        double lResOld = Math.abs(lMagOld) * baseLResOld;
        double eResOld = Math.abs(eMagOld) * baseEResOld;
        double lResNew = Math.abs(lMagNew) * baseLResNew;
        double eResNew = Math.abs(eMagNew) * baseEResNew;

        int lSizeNew = (int)Math.floor(((double)lSizeOld * lResOld / lResNew) + 0.5);
        int maxLines = dir.getLines();
        if (lSizeNew > maxLines) lSizeNew = maxLines;
        int eSizeNew = (int)Math.floor(((double)eSizeOld * eResOld / eResNew) + 0.5);
        int maxEles = dir.getElements();
        if (eSizeNew > maxEles) eSizeNew = maxEles;
        setNumLines(lSizeNew);
        setNumEles(eSizeNew);

        baseLRes = baseLResNew;
        baseERes = baseEResNew;
        amUpdating = true;
        int newVal = 0;
        try {
            defaultLineMag = lMagNew;
            setLRes(lResNew);
            setDLineMag(lDMagNew);
            setLineMag(lMagNew);
            newVal = lMagNew+1;
            if (newVal > -2)  newVal = 1;
            lineMagSlider.setValue(newVal);
            lineMagSliderChanged(false);
        } catch (Exception e) {
            System.out.println("lineMagSlider: e=" + e);
        }

        try {
            defaultElementMag = eMagNew;
            setERes(eResNew);
            setDElementMag(eDMagNew);
            setElementMag(eMagNew);
            newVal = eMagNew+1;
            if (newVal > -1) newVal = 1;
            elementMagSlider.setValue(newVal);
            elementMagSliderChanged(false);
        } catch (Exception e) {
            System.out.println("elementMagSlider: e=" + e);
        }

        amUpdating = false;

        int ele = 0;
        int line = 0;
        try {
            double[][] ll = new double[2][1];
            ll[0][0] = getLatitude();
            ll[1][0] = getLongitude();
            double[][] el = sample.fromReference(ll);
            ele = (int)Math.floor(el[0][0] + 0.5);
            line = (int)Math.floor(el[1][0] + 0.5);

            areaNav = nav;
            int areaLinRes = dir.getValue(11);
            int areaEleRes = dir.getValue(12);
            areaNav = previewNav;
            areaNav.setRes(areaLinRes, areaEleRes);
        } catch (Exception e) {
            System.out.println("e=" + e);
        }
        getGeoLocationInfo(line, ele);
    }
}
