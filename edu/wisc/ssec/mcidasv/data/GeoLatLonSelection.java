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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.accessibility.*;
import javax.swing.*;
import javax.swing.event.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(GeoLatLonSelection.class);
    
      private GeoLocationInfo geoLocInfo;

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
        "Magnification:", "", "", "Approx. Area: "
      };

      private static final String[] readoutLabels = {
          "Center     ",
          "Upper Left ",
          "Upper Right",
          "Lower Left ",
          "Lower Right"
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

      JTextField lineMagFld = new JTextField();
      JTextField eleMagFld = new JTextField();

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

       JLabel rawSizeLbl = new JLabel();

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

//      static double dNaN = Double.NaN;

/** the place string */
      private String defaultType = TYPE_LATLON;
      private String place;
      private String defaultPlace = PLACE_CENTER;
      private int defaultNumLines = 1000;
      private int defaultNumEles = 1000;
      private int numLines = defaultNumLines;
      private int numEles = defaultNumEles;
      private double latitude;
      private double defaultLat = Double.NaN;
      private double longitude;
      private double defaultLon = Double.NaN;
      private  boolean resetLatLon = true;
      private int imageLine;
      private int areaLine;
      private int defaultLine = -1;
      private int imageElement;
      private int areaElement;
      private int defaultElement = -1;
      private int lineMag;
      private double dLineMag;
      private int defaultLineMag;
      private int elementMag;
      private double dElementMag;
      private int defaultElementMag;
      private boolean isLineEle = false;
      private double lRes;
      protected double baseLRes = 0.0;
      private double eRes;
      protected double baseERes = 0.0;

      private Hashtable properties;
      private int uLLine;
      private int uLEle;
      private int centerLine;
      private int centerEle;
      protected boolean amUpdating = false;


      /** Maps the PROP_ property name to the gui component */
      private Hashtable propToComps = new Hashtable();

      /** size label */ JLabel sizeLbl;

      /** base number of lines */
      private double baseNumLines;

      /** base number of elements */
      private double baseNumElements;

      private DataSourceImpl dataSource;
      private static DataSourceImpl lastDataSource;
      private AreaDirectory previewDir;
      private AREAnav previewNav;
      private AREAnav areaNav;

      private List latLonLbls = new ArrayList();
      private List linEleImageLbls = new ArrayList();
      private List linEleAreaLbls = new ArrayList();
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
      private int maxLines = 0;
      private int maxEles = 0;

      private double bLRes = 0.0;
      private double bERes = 0.0;

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

      public GeoLatLonSelection(DataSourceImpl dataSource,
             DataChoice dataChoice, Hashtable initProps, MapProjection sample,
             AreaDirectory dir, AREAnav nav) 
              throws VisADException, RemoteException {
          super("Advanced");

          if (dataSource != lastDataSource) {
              this.resetLatLon = true;
          }
          lastDataSource = dataSource;

          this.properties = initProps;
          this.dataSource = dataSource;
          this.dataChoice = dataChoice;
          this.sampleProjection = sample;
          this.previewDir = dir;

          setBaseNumLines(dir.getLines());
          setBaseNumElements(dir.getElements());
          this.previewNav = nav;
          previewDirBlk = this.previewDir.getDirectoryBlock();
          int areaLinRes = previewDirBlk[11];
          int areaEleRes = previewDirBlk[12];
          this.areaNav = this.previewNav;
          this.areaNav.setRes(areaLinRes, areaEleRes);
          this.areaNav.setImageStart(previewDirBlk[5], previewDirBlk[6]);

          int numberOfLines;
          int numberOfElements;
          if (properties.containsKey(PROP_SIZE)) {
              String str = (String)properties.get(PROP_SIZE);
              String[] strs = StringUtil.split(str, " ", 2);
              numberOfLines = Integer.parseInt(strs[0]);
              numberOfElements = Integer.parseInt(strs[1]);
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
                  logger.error("no directory", e);
                  return;
              }
          }
          setNumLines(numberOfLines);
          setNumEles(numberOfElements);
          if (properties.containsKey(PROP_MAG)) {
              String str = (String)properties.get(PROP_MAG);
              String[] strs = StringUtil.split(str, " ", 2);
              this.defaultLineMag = Integer.parseInt(strs[0]);
              this.defaultElementMag = Integer.parseInt(strs[1]);
              this.dLineMag = (double)this.defaultLineMag;
              this.dElementMag = (double)this.defaultElementMag;
          } else {
              this.dLineMag = -(double)this.previewDir.getLines()/(double)numberOfLines;
              this.dElementMag = -(double)this.previewDir.getElements()/(double)numberOfElements;
              this.defaultLineMag = (int)(Math.floor(dLineMag));
              this.defaultElementMag = (int)(Math.floor(dElementMag));
          }
          setLineMag(this.defaultLineMag);
          setElementMag(this.defaultElementMag);

          try {
              if (properties.containsKey(PROP_LRES)) {
                  this.bLRes = Double.parseDouble((String)properties.get(PROP_LRES));
                  //if (dir.getValue(11) == 1) this.bLRes = this.previewDir.getCenterLatitudeResolution();
                  this.baseLRes = this.bLRes * (double)(dir.getValue(11));
                  setLRes(this.baseLRes * Math.abs(this.defaultLineMag));
              }
              if (properties.containsKey(PROP_ERES)) {
                  this.bERes = Double.parseDouble((String)properties.get(PROP_ERES));
                  //if (dir.getValue(12) == 1) this.bERes = this.previewDir.getCenterLongitudeResolution();
                  this.baseERes = this.bERes * (double)(dir.getValue(12));
                  setERes(this.baseERes * Math.abs(this.defaultElementMag));
              }
          } catch (Exception e) {
              logger.error("unable to get resolution", e);
             
          }
          setBLRes(this.bLRes);
          setBERes(this.bERes);
          if (this.baseLRes == 0.0)
              this.baseLRes = this.previewDir.getCenterLatitudeResolution();
          if (this.baseERes == 0.0)
              this.baseERes = this.previewDir.getCenterLongitudeResolution();

          this.place = getPlace();
          if (properties.containsKey(PROP_PLACE)) {
              setPlace((String)properties.get(PROP_PLACE));
          }

          if (properties.containsKey(PROP_PLRES)) {
              this.previewLineRes = Integer.parseInt((String)properties.get(PROP_PLRES));
          }
          if (properties.containsKey(PROP_PERES)) {
              this.previewEleRes = Integer.parseInt((String)properties.get(PROP_PERES));
          }

          if (this.resetLatLon) {
              if (this.previewDir != null) {
                  setLatitude(this.previewDir.getCenterLatitude());
                  setLongitude(this.previewDir.getCenterLongitude());
              }
          } else {
              setLatitude(this.latitude);
              setLongitude(this.longitude);
          }
          convertToLineEle();

          if (properties.containsKey(PROP_LATLON)) {
              String str = (String)properties.get(PROP_LATLON);
              String[] strs = StringUtil.split(str, " ", 2);
              setLatitude(Double.parseDouble(strs[0]));
              setLongitude(Double.parseDouble(strs[1]));
              convertToLineEle();
              this.isLineEle = false;
          } else if (properties.containsKey(PROP_LINEELE)) {
              String str = (String)properties.get(PROP_LINEELE);
              String[] strs = StringUtil.split(str, " ", 3);
              setLine(Integer.parseInt(strs[0]));
              setElement(Integer.parseInt(strs[1]));
              convertToLatLon();
              this.isLineEle = true;
          }

          if (this.defaultLineMag > 1) {
	      numberOfLines = numberOfLines * this.defaultLineMag;
              setNumLines(numberOfLines);
              setLRes(lRes/this.defaultLineMag);
              this.defaultLineMag = 1;
              setLineMag(this.defaultLineMag);
          }
          if (this.defaultElementMag > 1) {
              numberOfElements = numberOfElements * this.defaultElementMag;
              setNumEles(numberOfElements);
              setERes(lRes/this.defaultElementMag);
              this.defaultElementMag = 1;
              setElementMag(this.defaultElementMag);
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
                      public void actionPerformed(ActionEvent ae) {
                          String selected = getPlace();
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
                              convertToLineEle();
                              getGeoLocationInfo();
                          } else {
                              setLineElement();
                              convertToLatLon();
                              getGeoLocationInfo();
                          }
                      }
                  };

                  FocusListener linEleFocusChange = new FocusListener() {
                      public void focusGained(FocusEvent fe) {
                      }
                      public void focusLost(FocusEvent fe) {
                          setLineElement();
                          convertToLatLon();
                          getGeoLocationInfo();
                      }
                  };

                  if (latLonWidget == null)
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
                          convertToLineEle();
                          getGeoLocationInfo();
                      }
                  };

                  if (!this.isLineEle) {
                      latLonWidget.setLatLon(this.latitude, this.longitude);
                  }
                  String lineStr = "";
                  String eleStr = "";
                  setLine(this.imageLine);
                  setElement(this.imageElement);
                  if ((this.imageLine >= 0) && (this.imageElement >= 0)) {
                      lineStr =Integer.toString(this.imageLine);
                      eleStr =Integer.toString(this.imageElement);
                  }
                  centerLineFld    = new JTextField(lineStr, 3);
                  centerLineFld.addActionListener(latLonChange);
                  centerLineFld.addFocusListener(linEleFocusChange);
                  final String lineField = "";
                  centerElementFld = new JTextField(eleStr, 3);
                  centerElementFld.addActionListener(latLonChange);
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
                          int lines = getNumLines() * Math.abs(getLineMag());
                          if (lines > maxLines) lines = maxLines;
                          setBaseNumLines(lines);
                          int eles = getNumEles() * Math.abs(getElementMag());
                          if (eles > maxEles) eles = maxEles;
                          setBaseNumElements(eles);
                          getGeoLocationInfo();
                      }
                  };
                  FocusListener sizeFocusChange = new FocusListener() {
                      public void focusGained(FocusEvent fe) {
                      }
                      public void focusLost(FocusEvent fe) {
                          int lines = getNumLines() * Math.abs(getLineMag());
                          if (lines > maxLines) lines = maxLines;
                          setBaseNumLines(lines);
                          int eles = getNumEles() * Math.abs(getElementMag());
                          if (eles > maxEles) eles = maxEles;
                          setBaseNumElements(eles);
                          getGeoLocationInfo();
                      }
                  };
                  
                  this.maxLines = this.previewDir.getLines();
                  this.maxEles = this.previewDir.getElements();
                  
                  int lmag = getLineMag();
                  int emag = getElementMag();
                  if (lmag < 0) this.numLines = this.maxLines / Math.abs(lmag);
                  if (emag < 0) this.numEles = this.maxEles / Math.abs(emag);

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
                  fullResBtn.setToolTipText("Set fields to retrieve full image");

                  lockBtn =
                          GuiUtils.getToggleImageButton(IdvUIManager.ICON_UNLOCK,
                                          IdvUIManager.ICON_LOCK, 0, 0, true);
                  lockBtn.setContentAreaFilled(false);
                  lockBtn.setSelected(true);
                  lockBtn.setToolTipText(
                                  "Unlock to automatically change size when changing magnification");

                  rawSizeLbl = new JLabel(" Raw size: " + this.maxLines + " X " + 
                                                          this.maxEles);
                  JPanel sizePanel =
                      GuiUtils.left(GuiUtils.doLayout(new Component[] {
                          numLinesFld,
                          new JLabel(" X "), numElementsFld, sizeLbl, new JLabel(" "),
                          fullResBtn, new JLabel("  "), lockBtn,
                          rawSizeLbl }, 9, GuiUtils.WT_N, GuiUtils.WT_N));
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
                          int val = getMagValue(lineMagSlider);
                          setLineMag(val);
                          amUpdating = true;
                          lineMagSliderChanged(!lockBtn.isSelected());
                          amUpdating = false;
                          getGeoLocationInfo();
                      }
                  };
                  ActionListener lineMagChange =new ActionListener() {
                      public void actionPerformed(ActionEvent ae) {
                          if (amSettingProperties) {
                              return;
                          }
                          setLineMag();
                          changeLineMagSlider(!lockBtn.isSelected());
                          getGeoLocationInfo();
                      }
                  };
                  FocusListener lineMagFocusChange = new FocusListener() {
                      public void focusGained(FocusEvent fe) {
                      }
                      public void focusLost(FocusEvent fe) {
                          if (amSettingProperties) {
                              return;
                          }
                          setLineMag();
                          changeLineMagSlider(!lockBtn.isSelected());
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
                  int mag = getLineMag();
                  setLineMagSlider(mag);
                  lineMagComps[0].setToolTipText(
                      "Change the line magnification");
                  lineMagSlider.setToolTipText(
                      "Slide to set line magnification factor");
                  String str = "Line Mag=";
                  lineMagFld = new JTextField(Integer.toString(mag),3);
                  lineMagFld.addFocusListener(lineMagFocusChange);
                  lineMagFld.addActionListener(lineMagChange);
                  lineMagLbl =
                      GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
                  str = truncateNumericString(Double.toString(this.baseLRes*Math.abs(getLineMag())), 1);
                  str = " Res=" + str + kmLbl;
                  lineResLbl =
                      GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
                  amSettingProperties = oldAmSettingProperties;

                  GuiUtils.tmpInsets  = dfltGridSpacing;
                  lMagPanel = GuiUtils.doLayout(new Component[] {
                                        lineMagLbl, lineMagFld,
                                        GuiUtils.inset(lineMagComps[1],
                                            new Insets(0, 4, 0, 0)), lineResLbl, }, 5,
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
                          int val = getMagValue(elementMagSlider);
                          setElementMag(val);
                          amUpdating = true;
                          elementMagSliderChanged(true);
                          amUpdating = false;
                          getGeoLocationInfo();
                      }
                  };
                  ActionListener eleMagChange =new ActionListener() {
                      public void actionPerformed(ActionEvent ae) {
                          if (amSettingProperties) {
                              return;
                          }
                          setElementMag();
                          changeEleMagSlider(!lockBtn.isSelected());
                          getGeoLocationInfo();
                      }
                  };
                  FocusListener eleMagFocusChange = new FocusListener() {
                      public void focusGained(FocusEvent fe) {
                      }
                      public void focusLost(FocusEvent fe) {
                          if (amSettingProperties) {
                              return;
                          }
                          setElementMag();
                          changeEleMagSlider(!lockBtn.isSelected());
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
                  int mag = getElementMag();
                  setElementMagSlider(mag);
                  elementMagComps[0].setToolTipText(
                      "Change the element magnification");
                  elementMagSlider.setToolTipText(
                      "Slide to set element magnification factor");
                  eleMagFld = new JTextField(Integer.toString(mag),3);
                  eleMagFld.addFocusListener(eleMagFocusChange);
                  eleMagFld.addActionListener(eleMagChange);
                  String str = "Ele  Mag=";
                  elementMagLbl =
                      GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
                  str = truncateNumericString(Double.toString(this.baseERes*Math.abs(getElementMag())), 1);
                  str = " Res=" + str + kmLbl;
                  elementResLbl =
                      GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
                  amSettingProperties = oldAmSettingProperties;

                  GuiUtils.tmpInsets  = dfltGridSpacing;
                  eMagPanel = GuiUtils.doLayout(new Component[] {
                                        elementMagLbl, eleMagFld,
                                        GuiUtils.inset(elementMagComps[1],
                                            new Insets(0, 4, 0, 0)), elementResLbl, }, 5,
                                                GuiUtils.WT_N, GuiUtils.WT_N);
                  propComp = GuiUtils.hbox(new Component[] { new JLabel(" "), eMagPanel }, 2);
                  addPropComp(PROP_EMAG, propComp = eMagPanel);
              } else if (prop.equals(PROP_READOUT)) {
                  allComps.add(new JLabel(" "));
                  allComps.add(new JLabel(" "));
                  for (int i=0; i<5; i++) {
                      latLonLbls.add(GuiUtils.getFixedWidthLabel(" "));
                      linEleImageLbls.add(GuiUtils.getFixedWidthLabel(" "));
                      linEleAreaLbls.add(GuiUtils.getFixedWidthLabel(" "));
                  }
                  latLonReadoutPanel = GuiUtils.left(GuiUtils.doLayout(
                      latLonLbls, 1, GuiUtils.WT_N, GuiUtils.WT_Y));
                  lineElementImageReadoutPanel = GuiUtils.left(GuiUtils.doLayout(
                      linEleImageLbls, 1, GuiUtils.WT_N, GuiUtils.WT_Y));
                  lineElementAreaReadoutPanel = GuiUtils.left(GuiUtils.doLayout(
                      linEleAreaLbls, 1, GuiUtils.WT_N, GuiUtils.WT_Y));

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
          int numCols = 7;
          for (int i=0; i<5; i++) {
              String str = readoutLabels[i] +
                           " Lat: " + formatDoubleCoord(numCols, latLon[0][i]) +
                           " Lon: " + formatDoubleCoord(numCols, latLon[1][i]);
              ((JLabel)latLonLbls.get(i)).setText(str);

              String lineStr = formatIntegerCoord(numCols, imageEL[1][i]);
              String eleStr = formatIntegerCoord(numCols, imageEL[0][i]);
              str = readoutLabels[i] + " Line: " + lineStr +
                                       " Element: " + eleStr;
              ((JLabel)linEleImageLbls.get(i)).setText(str);

              lineStr = formatIntegerCoord(numCols, areaEL[1][i]);
              eleStr = formatIntegerCoord(numCols, areaEL[0][i]);
              str = readoutLabels[i] + " Line: " + lineStr +
                                       " Element: " + eleStr;
              ((JLabel)linEleAreaLbls.get(i)).setText(str);
          }
      }

      private String formatIntegerCoord(int cols, double val) {
          String outStr = Misc.MISSING;
          Double dbl = new Double(val);
          if (!dbl.isNaN()) {
              int ival = (int)Math.floor(val + 0.5);
              outStr = Integer.toString(ival);
          }
          int len = outStr.length() + 1;
          while (len < cols) {
              outStr = new String(" ").concat(outStr);
              len++;
          }
          return outStr;
      }

      private String formatDoubleCoord(int cols, double val) {
          String outStr = Misc.MISSING;
          Double dbl = new Double(val);
          if (!dbl.isNaN()) {
              outStr = Double.toString(val);
              if (outStr.length() > cols)
                  outStr = outStr.substring(0, cols);
          }
          int len = outStr.length();
          while (len < cols) {
              outStr = new String(" ").concat(outStr);
              len++;
          }
          return outStr;
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
              int ele = this.imageElement;
              int lin = this.imageLine;
              if (type.equals(TYPE_AREA)) {
                  ele = this.areaElement;
                  lin = this.areaLine;
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
          setPlace(PLACE_CENTER);
          setLatitude(this.previewDir.getCenterLatitude());
          setLongitude(this.previewDir.getCenterLongitude());
          convertToLinEle();
          setNumLines(this.maxLines);
          setNumEles(this.maxEles);
          setBaseNumLines(this.maxLines);
          setBaseNumElements(this.maxEles);
          setLineMag(1);
          setElementMag(1);
          setLineMagSlider(1);
          setLRes(this.baseLRes);
          setElementMagSlider(1);
          setERes(this.baseERes);
          amUpdating = true;
          lineMagSliderChanged(false);
          elementMagSliderChanged(false);
          amUpdating = false;
          getGeoLocationInfo();
      }

      @Override public void applyToDataSelection(DataSelection dataSelection) {
          logger.trace("dataSelection={}", dataSelection);

         if (dataSelection == null) {
             dataSelection = new DataSelection(true);
         }

         if (geoLocInfo == null) {
             getGeoLocationInfo();
         }
         
         GeoLocationInfo geoInfo = geoLocInfo;
         if (geoInfo == null) {
             logger.trace("err, wtf?");
             dataSelection = null;
             return;
         }

         String coordType = getCoordinateType();

         if (!isLineEle) {
             logger.trace("dealing with LALO coords for datasel={}", dataSelection);
             double lat = getLatitude();
             double lon = getLongitude();
             if (lat > 90.0 && lon> 360.0) {
                 convertToLatLon();
                 lat = getLatitude();
                 lon = getLongitude();
             }
//             if ((lat == dNaN) || (lon == dNaN)) {
//                 return;
//             }
             if ((Double.isNaN(lat) || (Double.isNaN(lon)))) {
                 return;
             }

             String latString = Double.toString(lat);
             if (latString.length() > 8) {
                 latString = latString.substring(0,7);
             }
             String lonString = Double.toString(lon);
             if (lonString.length() > 9) {
                 lonString = lonString.substring(0,8);
             }
             dataSelection.putProperty(PROP_LATLON, (latString + " " + lonString));
         } else {
             logger.trace("dealing with line-ele coords for datasel={}", dataSelection);
             int lin = getLine();
             int ele = getElement();
             if ((Double.isNaN(lin)) || (Double.isNaN(ele))) {
                 return;
             }
//             if ((lin == dNaN) || (ele == dNaN)) return;

             String typeStr = " I";
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
         logger.trace("dataChoice={} dataSelection={}", dataChoice, dataSelection);
         dataChoice.setDataSelection(dataSelection);
         
         this.dataSource.setDataSelection(dataSelection);
    }

      @Override public boolean getShowInControlProperties() {
          return false;
      }

    public GeoLocationInfo getGeoLocationInfo() {
        geoLocInfo = null;
        double[][] el = convertToDisplayCoords();
        int ele = (int)Math.floor(el[0][0] + 0.5);
        if (ele < 0) ele = 0;
        int lin = (int)Math.floor(el[1][0] + 0.5);
        if (lin < 0) lin = 0;
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
                return geoLocInfo;
            }
        }
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                latLon[i][j] = Double.NaN;
                imageEL[i][j] = Double.NaN;
                areaEL[i][j] = Double.NaN;
                displayEL[i][j] = Double.NaN;
            }
        }
        updateReadout();
        setLine(-1);
        setElement(-1);
        return null;
    }

    private GeoLocationInfo makeGeoLocationInfo(int lin, int ele, int nlins, int neles,
                            int linMag, int eleMag) {
         geoLocInfo = null;

         String plc = getPlace();
         String type = getCoordinateType();

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
         for (int i=0; i<5; i++) {
             if (areaEL[0][i] < 0.0) areaEL[0][i] = 0.0;
             if (areaEL[0][i] > this.maxEles) areaEL[0][i] = (double)this.maxEles;
             if (areaEL[1][i] < 0.0) areaEL[1][i] = 0.0;
             if (areaEL[1][i] > this.maxLines) areaEL[1][i] = (double)this.maxLines;
         }

         try {
             latLon = macs.toReference(displayEL);
         } catch (Exception e) {
             logger.error("converting input latitude/longitude", e);
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

         imageEL = this.previewNav.areaCoordToImageCoord(areaEL);
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
        int dLine = getNumLines()/2 * Math.abs(getLineMag());
        int dEle = getNumEles()/2 * Math.abs(getElementMag());
        if (this.place.equals(PLACE_CENTER)) {
            int newVal = this.areaLine + dLine;
            if (newVal > this.maxLines/2) newVal = this.maxLines/2;
            this.areaLine = newVal;
            newVal = this.areaElement + dEle;
            if (newVal > this.maxEles/2) newVal = this.maxEles/2;
            this.areaElement = newVal;
        } else {
            int newVal = this.areaLine - dLine;
            if (newVal < 0) newVal = 0;
            this.areaLine = newVal;
            newVal = this.areaElement - dEle;
            if (newVal < 0) newVal = 0;
            this.areaElement = newVal;
        }
        double[][] el = new double[2][1];
        el[0][0] = this.areaElement;
        el[1][0] = this.areaLine;
        double[][] vals = this.areaNav.areaCoordToImageCoord(el);
        this.imageElement = (int)Math.floor(vals[0][0] + 0.5);
        this.imageLine = (int)Math.floor(vals[1][0] + 0.5);

        if (type.equals(TYPE_AREA)) {
            setLine(this.areaLine);
            setElement(this.areaElement);
        } else if (type.equals(TYPE_IMAGE)) {
            setLine(this.imageLine);
            setElement(this.imageElement);
        }
        double[][] ll = this.areaNav.toLatLon(el);
        setLatitude(ll[0][0]);
        setLongitude(ll[1][0]);
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
     * @param thisPlace raw name
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
        numLinesFld.setText(Integer.toString(val));
    }

    private void setNumberOfElements(int val) {
        numElementsFld.setText(Integer.toString(val));
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
            val = Integer.parseInt(numLinesFld.getText().trim());
        } catch (Exception e) {
            logger.error("problem within getNumLines", e);
        }
        setNumLines(val);
        return this.numLines;
    }

    public void setNumLines(int val) {
        this.numLines = val;
        if (val >= 0) setNumberOfLines(val);
    }

    public int getNumEles() {
        int val = -1;
        try {
            val = Integer.parseInt(numElementsFld.getText().trim());
        } catch (Exception e) {
            logger.error("problem within getNumEles", e);
        }
        setNumEles(val);
        return this.numEles;
    }

    public void setNumEles(int val) {
        val = (int)((double)val/4.0 + 0.5)*4;
        this.numEles = val;
        if (val >= 0) setNumberOfElements(val);
    }

    public int getLine() {
        int val = -1;
        try {
            if (!(centerLineFld.getText().equals(Misc.MISSING)))
                val = Integer.parseInt(centerLineFld.getText().trim());
        } catch (Exception e) {
        }
        return val;
    }

    protected void setLineElement() {
        double[][] el = getLineElement();
        String type = getCoordinateType();
        if (type.equals(TYPE_IMAGE)) {
            this.imageElement = (int)Math.floor(el[0][0] + 0.5);
            this.imageLine = (int)Math.floor(el[1][0] + 0.5);
            double[][] vals = this.areaNav.imageCoordToAreaCoord(el);
            this.areaElement = (int)Math.floor(vals[0][0] + 0.5);
            this.areaLine = (int)Math.floor(vals[1][0] + 0.5);
        } else {
            this.areaElement = (int)Math.floor(el[0][0] + 0.5);
            this.areaLine = (int)Math.floor(el[1][0] + 0.5);
            double[][] vals = this.areaNav.areaCoordToImageCoord(el);
            this.imageElement = (int)Math.floor(vals[0][0] + 0.5);
            this.imageLine = (int)Math.floor(vals[1][0] + 0.5);
        }
    }

    public void setLine(int val) {
        if (val < 0)
            centerLineFld.setText(Misc.MISSING);
        else
            centerLineFld.setText(Integer.toString(val));
    }

    public int getElement() {
        int val =-1;
        try {
            val = Integer.parseInt(centerElementFld.getText().trim());
        } catch (Exception e) {
        }
        return val;
    }

    private double[][] getLineElement() {
        double[][] el = new double[2][1];
        el[0][0] = (double)getElement();
        el[1][0] = (double)getLine();
        return el;
    }

    public void setElement(int val) {
        if (val < 0)
            centerElementFld.setText(Misc.MISSING);
        else
            centerElementFld.setText(Integer.toString(val));
    }

    public int getLineMag() {
        return this.lineMag;
    }

    private void setElementMag() {
        int val = 1;
        try {
            val = Integer.parseInt(eleMagFld.getText().trim());
        } catch (Exception e) {
            logger.error("problem setting element mag", e);
            return;
        }
        setElementMag(val);
    }

    public void setLineMag(int val) {
        if (val > SLIDER_MAX) val = SLIDER_MAX;
        if (val < SLIDER_MIN-1) val = SLIDER_MIN-1;
        if (val == -1) val = 1;
        this.lineMag = val;
        setDLineMag((double)val);
    }

    private void setLineMagSlider(int val) {
        if (val == 1) val = -1;
        if (val > SLIDER_MAX) val = -1;
        if (val < SLIDER_MIN) val = SLIDER_MIN-1;
        lineMagSlider.setValue(val + 1);
    }

    public int getElementMag() {
        return this.elementMag;
    }

    private void setLineMag() {
        int val = 1;
        try {
            val = Integer.parseInt(lineMagFld.getText().trim());
        } catch (Exception e) {
            logger.error("problem setting line mag", e);
        }
        setLineMag(val);
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

    private void setElementMagSlider(int val) {
        if (val == 1) val = -1;
        if (val > SLIDER_MAX) val = -1;
        if (val < SLIDER_MIN) val = SLIDER_MIN-1;
        elementMagSlider.setValue(val + 1);
    }

    public double getDElementMag() {
        return this.dElementMag;
    }

    public void setElementMag(int val) {
        if (val > SLIDER_MAX) val = SLIDER_MAX;
        if (val < SLIDER_MIN-1) val = SLIDER_MIN-1;
        if (val == -1) val = 1;
        this.elementMag = val;
        setDElementMag((double)val);
    }

    public double getLatitude() {
        double val = latLonWidget.getLat();
//        Double dbl = new Double(val);
        if (Double.isNaN(val)) val = defaultLat;
        if (val < -90.0 || val > 90.0) val = defaultLat;
        setLatitude(val);
        return this.latitude;
    }

    private void setLatitude() {
        this.latitude = latLonWidget.getLat();
    }

    public void setLatitude(double val) {
        if (val < -90.0 || val > 90.0)
            val = defaultLat;
        latLonWidget.setLat(val);
        this.latitude = val;
        this.resetLatLon = false;
    }

    private void setLongitude() {
        this.longitude = latLonWidget.getLon();
    }

    public double getLongitude() {
        double val = latLonWidget.getLon();
//        Double dbl = new Double(val);
        if (Double.isNaN(val)) val = defaultLon;
        if (val < -180.0 || val > 180.0) val = defaultLon;
        setLongitude(val);
        return this.longitude;
    }

    public void setLongitude(double val) {
        if (val < -180.0 || val > 180.0)
            val = defaultLon;
        latLonWidget.setLon(val);
        this.longitude = val;
        this.resetLatLon = false;
    }

    protected void convertToLineEle() {
        double[][] ll = new double[2][1];
        ll[0][0] = getLatitude();
        ll[1][0] = getLongitude();
        double[][] el = this.areaNav.toLinEle(ll);
        this.areaElement = (int)Math.floor(el[0][0] + 0.5);
        this.areaLine = (int)Math.floor(el[1][0] + 0.5);
        el = this.areaNav.areaCoordToImageCoord(el);
        this.imageElement = (int)Math.floor(el[0][0] + 0.5);
        this.imageLine = (int)Math.floor(el[1][0] + 0.5);
    }

    protected void convertToLatLon() {
        double[][] el = getLineElement();
        double[][] ll = new double[2][1];
        String coordType = getCoordinateType();
        if (coordType.equals(TYPE_IMAGE))
            el = this.previewNav.imageCoordToAreaCoord(el);

        try {
            AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
            ll = macs.toReference(el);
            setLatitude(ll[0][0]);
            setLongitude(ll[1][0]);
            getGeoLocationInfo();
        } catch (Exception e) {
            logger.error("problem converting to latitude/longitude", e);
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
            areaLE = this.previewNav.toLinEle(ll);
            imageLE = this.previewNav.areaCoordToImageCoord(areaLE);
            setCenterCoords((int)imageLE[0][0], (int)imageLE[1][0]);
            getGeoLocationInfo();
        } catch (Exception e) {
            logger.error("problem converting to latitude/longitude", e);
        }
    }

    protected double[][] convertToDisplayCoords() {
        double[][] el = getLineElement();
        try {
            double[][] ll = new double[2][1];
            AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
            String type = getCoordinateType();
            if (type.equals(TYPE_LATLON)) {
                ll[0][0] = getLatitude();
                ll[1][0] = getLongitude();
                el = macs.fromReference(ll);
            } else {
                int[] dirB = macs.getDirBlock();
                int previewLineMag = dirB[11];
                int previewEleMag = dirB[12];
                int dirLMag = this.previewDir.getValue(11);
                int dirEMag = this.previewDir.getValue(12);
                if (type.equals(TYPE_IMAGE))
                    el = this.areaNav.imageCoordToAreaCoord(el);
                Rectangle2D mapArea = macs.getDefaultMapArea();
                int previewXDim = new Long(new Double(mapArea.getMaxX() - mapArea.getMinX()).longValue()).intValue();
                int previewYDim = new Long(new Double(mapArea.getMaxY() - mapArea.getMinY()).longValue()).intValue();
                el[0][0] = el[0][0] * dirEMag / previewEleMag;
                el[1][0] = previewYDim - 1 - el[1][0] * dirLMag / previewLineMag;;
            }
        } catch (Exception e) {
            logger.error("problem converting to display coordinates", e);
        }
        return el;
    }

    private double[][] displayCoordToAreaCoord(double[][] disp) {
        double[][] area = new double[2][disp[0].length];
        try {
            if (sampleProjection != null) {
                AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
                double[][] ll = macs.toReference(disp);
                double[][] el = this.areaNav.toLinEle(ll);
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
            logger.error("problem converting display coordinates to area coordinates", e);
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
            logger.error("problem converting area coordinates to display coordinates", e);
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
            logger.error("problem converting to lines/elements", e);
        }
    }

    public String getCoordinateType() {
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

    public boolean getLockOn() {
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
        }
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
        elementMagLbl.setText("Ele  Mag=");
        eleMagFld.setText(new Integer(value).toString());
        String str = " Res=" +
            truncateNumericString(Double.toString(this.baseERes*Math.abs(value)), 1);
        elementResLbl.setText(StringUtil.padLeft(str, 4) + kmLbl);

        if (!lockBtn.isSelected()) {
            if (value > 0) {
                setNumberOfElements((int)(this.baseNumElements * value));
            } else {
                setNumberOfElements((int)(this.baseNumElements
                                                / (double) -value));
            }
        }
    }

    private void changeLineMagSlider(boolean autoSetSize) {
        int value = getLineMag();
        setLineMagSlider(value);
    }

    private void changeEleMagSlider(boolean autoSetSize) {
        int value = getElementMag();
        setElementMagSlider(value);
    }

    /**
     * Handle the line mag slider changed event.
     *
     * @param autoSetSize Whether or not the size is automatically set.
     */
    protected void lineMagSliderChanged(boolean autoSetSize) {
        try {
            int value = getLineMag();
            if (!amUpdating) {
                value = getLineMagValue();
                setLineMag(value);
            }
            lineMagLbl.setText("Line Mag=");
            lineMagFld.setText(new Integer(value).toString());
            String str = " Res=" +
                truncateNumericString(Double.toString(this.baseLRes*Math.abs(value)), 1);
            lineResLbl.setText(StringUtil.padLeft(str, 4) + kmLbl);

            if (autoSetSize) {
                if (value > 0) {
                    setNumberOfLines((int)(this.baseNumLines * value));
                } else {
                    setNumberOfLines((int)(this.baseNumLines
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

            amSettingProperties = true;
            setElementMag(value);
            setElementMagSlider(value);
            amSettingProperties = false;
            elementMagSliderChanged(false);
        } catch (Exception exc) {
            logger.error("could not set line magnifiction", exc);
        }
    }

    /**
     * Get the value of the line magnification slider.
     *
     * @return The magnification value for the line
     */
    protected int getLineMagValue() {
        int val = getMagValue(lineMagSlider);
        return val;
    }

    /**
     * Get the value of the element magnification slider.
     *
     * @return The magnification value for the element
     */
    protected int getElementMagValue() {
        int val = getMagValue(elementMagSlider) - 1;
        setElementMag(val);
        return val;
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
        if (value == 0) {
            value = SLIDER_MAX;
            return value;
        } else if (value < SLIDER_MIN) {
            value = SLIDER_MIN;
        }
        return value - 1;
    }

    /**
     * Create a menu item based upon a given location.
     *
     * @param llp Location to use in the resulting menu item. Cannot be
     * {@code null}.
     * @param name Name of {@code location}. Cannot be {@code null}.
     *
     * @return Menu item based on {@code llp} and {@code name}.
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
            logger.error("could not create location menu item", e);
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
        if (val < 1) val = this.baseLRes;
        this.lRes = val;
    }

    public void setBLRes(double val) {
        this.bLRes = val;
    }

    public void setBERes(double val) {
        this.bERes = val;
    }

    public double getBLRes() {
        return this.bLRes;
    }

    public double getBERes() {
        return this.bERes;
    }

    public double getERes() {
        return this.eRes;
    }

    public void setERes(double val) {
        if (val < 1) val = this.baseERes;
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
        if (indx >= str.length()) indx = str.length();
        return str.substring(0,indx);
    }
    
    public String getLatLonType() {
    	return TYPE_LATLON;
    }

    protected double[][] getLatLonPoints() {
        return latLon;
    }

    protected double[][] getImagePoints() {
        return imageEL;
    }

    protected double[][] getAreaPoints() {
        return areaEL;
    }

    protected double[][] getDisplayELPoints() {
        return displayEL;
    }

    protected double getBaseLRes() {
        return this.baseLRes;
    }

    protected double getBaseERes() {
        return this.baseERes;
    }

    protected void setBaseNumLines(int val) {
        this.baseNumLines = (double)val;
    }

    public void setDataChoice(DataChoice choice) {
        logger.trace("oldChoice={} newChoice={}", this.dataChoice, choice);
        this.dataChoice = choice;
    }

    public DataChoice getDataChoice() {
        return this.dataChoice;
    }
    
    protected void setBaseNumElements(int val) {
        this.baseNumElements = (double)val;
    }

    public void update(AreaDirectory dir, MapProjection sample, AREAnav nav, 
                          String coordType, double[] coords) {
        boolean saveLock = getLockOn();
        setLockOn(true);
        this.maxLines = dir.getLines();
        this.maxEles = dir.getElements();
        sampleProjection = sample;

        double baseLResOld = getBaseLRes();
        double baseEResOld = getBaseERes();
        double lDMagOld = getDLineMag();
        double eDMagOld = getDElementMag();
        int lMagOld = getLineMag();
        int eMagOld = getElementMag();
        int lSizeOld = getNumLines();
        int eSizeOld = getNumEles();
        
        double baseLResNew = getBLRes();
        double baseEResNew = getBERes();
        try {
            baseLResNew *= (double)(dir.getValue(11));
            baseEResNew *= (double)(dir.getValue(12));
        } catch (Exception e) {
        }

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
        if (lSizeNew > this.maxLines) lSizeNew = this.maxLines;
        int eSizeNew = (int)Math.floor(((double)eSizeOld * eResOld / eResNew) + 0.5);
        if (eSizeNew > this.maxEles) eSizeNew = this.maxEles;        
        setNumLines(lSizeNew);
        setNumEles(eSizeNew);
        
        this.baseLRes = baseLResNew;
        this.baseERes = baseEResNew;

        amUpdating = true;
        amSettingProperties = true;
        int newVal = 0;
        try {
            this.defaultLineMag = lMagNew;
            setLRes(lResNew);
            newVal = lMagNew;
            if (newVal > -2)  newVal = 1;
            setLineMag(newVal);
            changeLineMagSlider(!lockBtn.isSelected());
        } catch (Exception e) {
            logger.error("error adjusting line mag slider", e);
        }

        try {
            this.defaultElementMag = eMagNew;
            setERes(eResNew);
            newVal = eMagNew;
            if (newVal > -2) newVal = 1;
        } catch (Exception e) {
            logger.error("error adjusting element mag slider", e);
        }
        amUpdating = false;
        amSettingProperties = false;

        int ele = 0;
        AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
        Rectangle2D mapArea = macs.getDefaultMapArea();
        double previewYDim = mapArea.getHeight();
        int line = (int)Math.floor(previewYDim);
        try {
            int lat = (int)Math.floor(getLatitude() + 0.5);
            if ((lat <= 90.0) && (lat >= -90.0)) {
                double[][] ll = new double[2][1];
                ll[0][0] = lat;
                ll[1][0] = getLongitude();
                double[][] el = sample.fromReference(ll);
                ele = (int)Math.floor(el[0][0] + 0.5);
                line = (int)Math.floor(el[1][0] + 0.5);
            }
            this.areaNav = nav;
            int areaLinRes = dir.getValue(11);
            int areaEleRes = dir.getValue(12);
            int startLine = dir.getValue(5);
            int startEle = dir.getValue(6);
            this.previewDir = dir;
            this.areaNav = this.previewNav;
            this.areaNav.setRes(areaLinRes, areaEleRes);
            this.areaNav.setImageStart(startLine, startEle);
            
            setCoordinateType(coordType);
            if (coordType.equals(TYPE_LATLON)) {
                setLatitude(coords[0]);
                setLongitude(coords[1]);
                convertToLineEle();
            } else if (coordType.equals(TYPE_AREA)) {
                double dCoord = coords[0] * baseLResOld/baseLResNew;
                setLine((int)Math.floor(dCoord+0.5));
                dCoord = coords[1] * baseEResOld/baseEResNew;
                setElement((int)Math.floor(dCoord+0.5));
                setLineElement();
                convertToLatLon();
            }

        } catch (Exception e) {
            logger.error("error updating", e);
        }

        try {
            rawSizeLbl.setText(" Raw size: " + this.maxLines + " X " + this.maxEles);
        } catch (Exception e) {
            logger.error("could not update raw size", e);
        }

        amUpdating = true;
        lineMagSliderChanged(false);
        setElementMag(newVal);
        elementMagSliderChanged(false);
        amUpdating = false;
        changeEleMagSlider(!lockBtn.isSelected());
        setLockOn(saveLock);
        getGeoLocationInfo(line, ele);
    }
}
