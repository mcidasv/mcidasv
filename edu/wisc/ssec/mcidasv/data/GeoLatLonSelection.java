/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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

import edu.wisc.ssec.mcidas.adde.AddeTextReader;

import edu.wisc.ssec.mcidasv.Constants;

import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.SubsetRubberBandBox;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.data.hydra.HydraContext;
import edu.wisc.ssec.mcidasv.control.LambertAEA;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.GeoSelectionPanel;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.geoloc.*;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.Msg;
import ucar.unidata.util.Range;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import visad.Data;
import visad.FlatField;
import visad.GriddedSet;
import visad.Gridded2DSet;
import visad.SampledSet;
import visad.VisADException;
import visad.data.mcidas.AREACoordinateSystem;
import visad.georef.EarthLocationTuple;
import visad.georef.MapProjection;
import visad.data.mcidas.BaseMapAdapter;

import java.io.File;
import java.net.URL;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.FocusListener;
import java.awt.Insets;
//import java.awt.event.KeyAdapter;
//import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;

import visad.*;
import visad.bom.RubberBandBoxRendererJ3D;
import visad.java3d.DisplayImplJ3D;
import visad.java3d.TwoDDisplayRendererJ3D;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.control.DisplayControlBase;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;
import ucar.visad.display.XYDisplay;
import ucar.visad.display.MapLines;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.RubberBandBox;

import ucar.visad.ProjectionCoordinateSystem;
import ucar.unidata.geoloc.projection.LatLonProjection;


public class GeoLatLonSelection extends DataSelectionComponent implements Constants {

      /** The spacing used in the grid layout */
      protected static final int GRID_SPACING = 3;

      /** Used by derived classes when they do a GuiUtils.doLayout */
      protected static final Insets GRID_INSETS = new Insets(GRID_SPACING,
                                                      GRID_SPACING,
                                                      GRID_SPACING,
                                                      GRID_SPACING);

      DataChoice dataChoice;
      FlatField image;
      boolean isLL;
      MapProjection sampleProjection;

      boolean hasSubset = true;
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;

      /** ADDE request string for text */
      protected static final String REQ_TEXT = "text";

      /** flag for center */
      protected static final String PLACE_CENTER = "CENTER";

      /** flag for upper left */
      protected static final String PLACE_ULEFT = "ULEFT";

      /** flag for lower left */
      private static final String PLACE_LLEFT = "LLEFT";

      /** flag for upper right */
      private static final String PLACE_URIGHT = "URIGHT";

      /** flag for lower right */
      private static final String PLACE_LRIGHT = "LRIGHT";

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

      /** Property for image default value loc */
      protected static final String PROP_LOC = "LOC";

      /** Property for image default value mag */
      protected static final String PROP_MAG = "MAG";
      protected static final String PROP_LMAG = "LMAG";
      protected static final String PROP_EMAG = "EMAG";

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

      /** Property for line resolution */
      protected static final String PROP_LRES = "LRES";
      protected static final String PROP_PLRES = "PLRES";

      /** Property for element resolution */
      protected static final String PROP_ERES = "ERES";
      protected static final String PROP_PERES = "PERES";

      /** This is the list of properties that are used in the advanced gui */
      private static final String[] ADVANCED_PROPS = {
          PROP_UNIT, PROP_BAND, PROP_PLACE, PROP_LOC, PROP_SIZE, PROP_MAG,
          PROP_LMAG, PROP_EMAG
      };

      /** This is the list of labels used for the advanced gui */
      private static final String[] ADVANCED_LABELS = {
        "Data Type:", "Channel:", "Placement:", "Location:", "   Image Size:",
        "Magnification:", "   Line:", "   Element:"
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

      /** place label */
      private JLabel placeLbl = new JLabel();
/** the place string */
      private static String place;
      private String defaultPlace = PLACE_CENTER;
      private static int numLines = 0;
      private int defaultNumLines = 1000;
      private static int numEles = 0;
      private int defaultNumEles = 1000;
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
      private int defaultLineMag;
      private static int elementMag;
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

      /** Maps the PROP_ property name to the gui component */
      private Hashtable propToComps = new Hashtable();

      private JButton locPosButton;
      /** size label */ JLabel sizeLbl;

      /** base number of lines */
      private double baseNumLines = 0.0;

      /** base number of elements */
      private double baseNumElements = 0.0;

      private DataSourceImpl dataSource;
      private AreaDirectory previewDir;

      private static int flipFlag = 0;

      private JPanel latLonPanel;
      private JPanel lineElementPanel;
      protected JButton locTypeButton;

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
 
      double[][] elelin = new double[2][4];
      double[][] latlon = new double[2][4];

      private int[] previewDirBlk;

      private int previewLineRes;
      private int previewEleRes;

      public GeoLatLonSelection(DataSourceImpl dataSource,
             DataChoice dataChoice, Hashtable initProps, MapProjection sample, AreaDirectory dir) 
              throws VisADException, RemoteException {
          super("Lat/Lon");
/*
          System.out.println("GeoLatLonSelection:");
          System.out.println("    dataSource=" + dataSource);
          System.out.println("    dataChoice=" + dataChoice);
          System.out.println("    initProps=" + initProps);
          System.out.println("    sample=" + sample);
*/
          this.properties = initProps;
          this.dataSource = dataSource;
          this.dataChoice = dataChoice;
          this.sampleProjection = sample;
          this.baseNumLines = dir.getLines();
          this.baseNumElements = dir.getElements();
          this.previewDir = dir;
          previewDirBlk = this.previewDir.getDirectoryBlock();
/*
          System.out.println("centerLatitudeResolution=" + dir.getCenterLatitudeResolution());
          System.out.println("centerLongitudeResolution=" + dir.getCenterLongitudeResolution());

          System.out.println("\n");
          for (int i=0; i<20; i++)
              System.out.println(i + ": " + previewDirBlk[i]);
          System.out.println("\n");
*/
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
                  //System.out.println(numberOfLines + " x " + numberOfElements);
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
          setNumLines(new Integer(numberOfLines));
          setNumEles(new Integer(numberOfElements));

          if (properties.containsKey(PROP_MAG)) {
              String str = (String)properties.get(PROP_MAG);
              String[] strs = StringUtil.split(str, " ", 2);
              defaultLineMag = new Integer(strs[0]).intValue();
              defaultElementMag = new Integer(strs[1]).intValue();
          } else {
              defaultLineMag = -(int)((double)this.previewDir.getLines()/(double)numberOfLines + 0.5);
              defaultElementMag = -(int)((double)this.previewDir.getElements()/(double)numberOfElements + 0.5);
          }
          setLineMag(defaultLineMag);
          setElementMag(defaultElementMag);

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
          //System.out.println("baseLRes=" + baseLRes + " baseERes=" + baseERes);

          this.place = getPlace();
          if (properties.containsKey(PROP_PLACE)) {
              setPlace((String)properties.get(PROP_PLACE));
          }

          if (resetLatLon) {
              if (previewDir != null) {
                  setLatitude(new Double(previewDir.getCenterLatitude()));
                  setLongitude(new Double(previewDir.getCenterLongitude()));
              }
          } else {
              setLatitude(this.latitude);
              setLongitude(this.longitude);
          }
          convertToLinEle();

          if (properties.containsKey(PROP_LATLON)) {
              String str = (String)properties.get(PROP_LATLON);
              String[] strs = StringUtil.split(str, " ", 2);
              setLatitude(new Double(strs[0]).doubleValue());
              setLongitude(new Double(strs[1]).doubleValue());
              convertToLinEle();
              this.isLineEle = false;
          } else if (properties.containsKey(PROP_LINEELE)) {
              String str = (String)properties.get(PROP_LINEELE);
              String[] strs = StringUtil.split(str, " ", 3);
              setLine(new Integer(strs[0]).intValue());
              setElement(new Integer(strs[1]).intValue());
              convertToLatLon();
              this.isLineEle = true;
          }

          if (defaultLineMag > 1) {
	      numberOfLines = numberOfLines * defaultLineMag;
              setNumLines(new Integer(numberOfLines));
              setLRes(lRes/defaultLineMag);
              defaultLineMag = 1;
              setLineMag(defaultLineMag);
          }
          if (defaultElementMag > 1) {
              numberOfElements = numberOfElements * defaultElementMag;
              setNumEles(new Integer(numberOfElements));
              setERes(lRes/defaultElementMag);
              defaultElementMag = 1;
              setElementMag(defaultElementMag);
          }

          this.previewLineRes = 1;
          this.previewEleRes = 1;
          if (properties.containsKey(PROP_PLRES)) {
              this.previewLineRes = new Integer((String)properties.get(PROP_PLRES)).intValue();
          }
          if (properties.containsKey(PROP_PERES)) {
              this.previewEleRes = new Integer((String)properties.get(PROP_PERES)).intValue();
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
              if (prop.equals(PROP_LOC)) {
                  placeLbl = GuiUtils.getFixedWidthLabel("");
                  setPlace(this.place);
                  addPropComp(PROP_PLACE, placeLbl);
                  ActionListener latlonChange =new ActionListener() {
                      public void actionPerformed(ActionEvent ae) {
                          convertToLinEle();
                      }
                  };
                  ActionListener lineleChange =new ActionListener() {
                      public void actionPerformed(ActionEvent ae) {
                          convertToLatLon();
                      }
                  };
                  latLonWidget     = new LatLonWidget(latlonChange);
                  if (!this.isLineEle) {
                      latLonWidget.setLatLon((Double.toString(this.latitude)),
                                             (Double.toString(this.longitude)));
                  }
                  String lineStr = "";
                  String eleStr = "";
                  setLine(this.line);
                  setElement(this.element);
                  if ((this.line > 0) && (this.element > 0)) {
                      lineStr =Integer.toString(this.line);
                      eleStr =Integer.toString(this.element);
                  }
                  centerLineFld    = new JTextField(lineStr, 3);
                  centerLineFld.addActionListener(lineleChange);
                  final String lineField = "";
                  centerElementFld = new JTextField(eleStr, 3);
                  centerElementFld.addActionListener(lineleChange);
                  final JButton centerPopupBtn =
                      GuiUtils.getImageButton(
                        "/auxdata/ui/icons/MapIcon16.png", getClass());
                  centerPopupBtn.setToolTipText("Center on current displays");

                  centerPopupBtn.addActionListener(new ActionListener() {
                      public void actionPerformed(ActionEvent ae) {
                          popupCenterMenu( centerPopupBtn, latLonWidget);
                      }
                  });

                  JComponent centerPopup = GuiUtils.inset(centerPopupBtn,
                                             new Insets(0, 0, 0, 4));


                  GuiUtils.tmpInsets = dfltGridSpacing;

                  latLonPanel = GuiUtils.hbox(new Component[] {
                      centerLatLbl = GuiUtils.rLabel(" Lat:" + dfltLblSpacing),
                      latLonWidget.getLatField(),
                      centerLonLbl = GuiUtils.rLabel(" Lon:" + dfltLblSpacing),
                      latLonWidget.getLonField(), new JLabel(" "), centerPopup
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

                  locPosButton = GuiUtils.makeImageButton(
                                             "/auxdata/ui/icons/Refresh16.gif",
                                             this, "cyclePlace", null, true);
                  changePlace(getPlace());

                  locPosButton.setToolTipText("Change place type");
  
                  locTypeButton =
                      GuiUtils.makeImageButton(
                          "/auxdata/ui/icons/Refresh16.gif", locationPanel,
                          "flip", null, true);
                  locTypeButton.addActionListener(new ActionListener() {
                      public void actionPerformed(ActionEvent ae) {
                          convert();
                      }
                  });
                  if (this.isLineEle) locTypeButton.doClick();
                  locTypeButton.setToolTipText(
                      "Toggle between Latitude/Longitude and Line/Element");
  
                  propComp = GuiUtils.hbox(new Component[] { locPosButton,
                          placeLbl}, 2);
                  addPropComp(PROP_LOC, propComp);
                  if (propComp != null) {
                      allComps.add(GuiUtils.rLabel(labelArray[propIdx]));
                      allComps.add(GuiUtils.left(propComp));
                  }
                  propComp = GuiUtils.hbox(new Component[] { locTypeButton, 
                          locationPanel }, 2);
                  if (propComp != null) {
                      allComps.add(GuiUtils.rLabel("  "));
                      allComps.add(GuiUtils.left(propComp));
                  }
                  propComp = null;
              } else if (prop.equals(PROP_SIZE)) {
                  setNumLines(this.numLines);
                  numLinesFld    = new JTextField(Integer.toString(this.numLines), 4);
                  setNumEles(this.numEles);
                  numElementsFld = new JTextField(Integer.toString(this.numEles), 4);
                  numLinesFld.setToolTipText("Number of lines");
                  numElementsFld.setToolTipText("Number of elements");
                  GuiUtils.tmpInsets = dfltGridSpacing;
                  sizeLbl            = GuiUtils.lLabel("");

                  lockBtn =
                          GuiUtils.getToggleImageButton(IdvUIManager.ICON_UNLOCK,
                                          IdvUIManager.ICON_LOCK, 0, 0, true);
                  lockBtn.setContentAreaFilled(false);
                  lockBtn.setSelected(true);
                  lockBtn.setToolTipText(
                                  "Unlock to automatically change size when changing magnification");

                  JLabel rawSizeLbl = new JLabel(" Raw size: " + previewDirBlk[8]
                                                 + " X " + previewDirBlk[9]);
                  JPanel sizePanel =
                      GuiUtils.left(GuiUtils.doLayout(new Component[] {
                          numLinesFld,
                          new JLabel(" X "), numElementsFld, sizeLbl, lockBtn,
                          rawSizeLbl }, 6, GuiUtils.WT_N, GuiUtils.WT_N));
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
              }
              if (propComp != null) {
                  allComps.add(GuiUtils.rLabel(labelArray[propIdx]));
                  allComps.add(GuiUtils.left(propComp));
              }
          }
          GuiUtils.tmpInsets = GRID_INSETS;
          JPanel imagePanel = GuiUtils.doLayout(allComps, 2, GuiUtils.WT_NY,
                                  GuiUtils.WT_N);
        return GuiUtils.top(imagePanel);
      }
                                                                                                                                             
      public void applyToDataSelection(DataSelection dataSelection) {
         if (dataSelection == null) {
             dataSelection = new DataSelection(true);
         }
         GeoLocationInfo geoInfo = getGeoLocationInfo();
         if (geoInfo == null) {
             dataSelection = null;
             return;
         }

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
      }


    private GeoLocationInfo getGeoLocationInfo() {
         GeoLocationInfo gli = null;
         int nlins = getNumLines();
         int neles = getNumEles();

         if (nlins < 0) return gli;

         String plc = getPlace();
         int lin = -1;
         int ele = -1;
         try {
            lin = getLine();
            ele = getElement();
         } catch (Exception e) {
         }
         double lat = 9999.0;
         double lon = 9999.0;
         try {
             lat = getLatitude();
             lon = getLongitude();
         } catch (Exception e) {
         }
         if (lin < 0 && ele < 0) {
             if (lat <= 90.0 && lon < 999.0) {
                 convertToLinEle();
                 lin = getLine();
                 ele = getElement(); 
             } else {
                 return gli;
             }
          } else if (lat > 90.0 && lon > 360.0) {
             convertToLatLon();
             lat = getLatitude();
             lon = getLongitude();
          }

         int linMag = getLineMag();
         int eleMag = getElementMag();
         if ((lat > 90.0) || (lat < -90.0)) {
             return gli;
         }

         AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;

         double dLine = (double)nlins/(2.0*this.previewLineRes);
         double dEle = (double)neles/(2.0*this.previewEleRes);

         if (plc.equals(PLACE_CENTER)) {
             elelin[0][0] = getElement();
             elelin[1][0] = getLine();
             elelin[0][0] -= dEle;
             if (elelin[0][0] > this.previewDirBlk[6])
                 elelin[0][0] = this.previewDirBlk[6];
             elelin[1][0] += dLine;
             if (elelin[1][0] > this.previewDirBlk[5])
                 elelin[1][0] = this.previewDirBlk[5];
         }
         elelin[0][1] = elelin[0][0] + 2.0*dEle;
         elelin[1][1] = elelin[1][0] - 2.0*dLine;
         elelin[0][2] = elelin[0][1];
         elelin[1][2] = elelin[1][0];
         elelin[0][3] = elelin[0][0];
         elelin[1][3] = elelin[1][1];
        
         try {
             latlon = macs.toReference(elelin);
         } catch (Exception e) {
             System.out.println("Error converting input lat/lon e=" + e);
         }

         gli = new GeoLocationInfo(latlon[0][0], latlon[1][0], latlon[0][1], latlon[1][1]);
         return gli;
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
        //System.out.println("cyclePlace:");
        getGeoLocationInfo();
        if (this.place.equals(PLACE_CENTER)) {
            changePlace(PLACE_ULEFT);
            setLine((int)(elelin[1][0] + 0.5));
            setElement((int)(elelin[0][0] + 0.5));
        } else {
            changePlace(PLACE_CENTER);
            setLine((int)((elelin[1][0] + elelin[1][3])/2.0 + 0.5));
            setElement((int)((elelin[0][0] + elelin[0][2])/2.0 + 0.5));
        }
/*
        int[] dirBlk = previewDirBlk;
        for (int i=0; i<20; i++) 
            System.out.println(i + ": " + dirBlk[i]);
        int lineRes = dirBlk[11];
        int eleRes = dirBlk[12];
        System.out.println("lineRes=" + lineRes + " eleRes=" + eleRes);
        int lMag = getLineMag();
        int eMag = getElementMag();
        System.out.println("lMag=" + lMag + " eMag=" + eMag);
        System.out.println("numLines=" + getNumLines() + " numEles=" + getNumEles());
        int dLine = (int)((double)getNumLines()/(2.0*(double)lineRes) - 0.5);
        int dEle = (int)((double)getNumEles()/(2.0*(double)eleRes) - 0.5);
        System.out.println("dLine=" + dLine + " dEle=" + dEle);
        if (lMag > 0) {
            dLine *= lMag;
        } else if (lMag < 0) {
            dLine = (int)((double)dLine/(double)-lMag - 0.5);
        }
        System.out.println("dLine=" + dLine);
        if (eMag > 0) {
            dEle *= eMag;
        } else if (eMag < 0) {
            dEle = (int)((double)dEle/(double)-eMag - 0.5);
        }
        System.out.println("dEle=" + dEle);
        System.out.println("dLine=" + dLine + " dEle=" + dEle);
        if (this.place.equals(PLACE_CENTER)) {
            System.out.println("Here 1");
            changePlace(PLACE_ULEFT);
            System.out.println("    getLine=" + getLine() + " getElement=" + getElement());
            System.out.println("    getLatitude=" + getLatitude() + " getLongitude=" + getLongitude());
            setLine(getLine()+dLine);
            setElement(getElement()-dEle);
        } else {
            System.out.println("Here 2");
            changePlace(PLACE_CENTER);
            System.out.println("    getLine=" + getLine() + " getElement=" + getElement());
            setLine(getLine()-dLine);
            setElement(getElement()+dEle);
        }
*/
        convertToLatLon();
    }


    /**
     * Change the place
     *
     * @param newPlace new place
     */
    public void changePlace(String newPlace) {
        this.place = newPlace;
        String s = translatePlace(place) + "=";
        placeLbl.setText(StringUtil.padRight(s, 12));
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
        thisPlace = thisPlace.toUpperCase();
        if (thisPlace.equals(PLACE_ULEFT)) {
            return "Upper left";
        }
        if (thisPlace.equals(PLACE_LLEFT)) {
            return "Lower left";
        }
        if (thisPlace.equals(PLACE_URIGHT)) {
            return "Upper right";
        }
        if (thisPlace.equals(PLACE_LRIGHT)) {
            return "Lower right";
        }
        if (thisPlace.equals(PLACE_CENTER)) {
            return "Center";
        }
        return thisPlace;
    }

    public String getPlace() {
        if (this.place == null)
            this.place = defaultPlace;
        return this.place;
    }

    public void setPlace(String str) {
        if (str.equals("")) str = defaultPlace;
        this.place = str;
        changePlace(this.place);
    }

    public int getNumLines() {
        try {
            this.numLines = new Integer(numLinesFld.getText().trim()).intValue();
        } catch (Exception e) {
            if (this.numLines < 1) this.numLines = defaultNumLines;
        }
        return this.numLines;
    }

    public void setNumLines(int val) {
        if (val < 1) val = defaultNumLines;
        numLinesFld.setText(new Integer(val).toString());
        this.numLines = val;
    }

    public int getNumEles() {
        try {
            this.numEles = new Integer(numElementsFld.getText().trim()).intValue();
        } catch (Exception e) {
            if (this.numEles < 1) this.numEles = defaultNumEles;
        }
        return this.numEles;
    }

    public void setNumEles(int val) {
        if (val < 1) val = defaultNumEles;
        val = (int)((double)val/4.0 + 0.5)*4;
        numElementsFld.setText(new Integer(val).toString());
        this.numEles = val;
    }

    public int getLine() {
        int val = -1;
        try {
            val = new Integer(centerLineFld.getText().trim()).intValue();
        } catch (Exception e) {
        }
        if (val < 1) val = defaultLine;
        this.line = val;
        return this.line;
    }

    public void setLine(int val) {
        if (val < 0) val = defaultLine;
        centerLineFld.setText(new Integer(val).toString());
        this.line = val;
    }

    public int getElement() {
        int val =-1;
        try {
            val = new Integer(centerElementFld.getText().trim()).intValue();
        } catch (Exception e) {
        }
        if (val < 1) val = defaultElement;
        this.element = val;
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
        this.lineMag = val;
    }

    public int getElementMag() {
        return this.elementMag;
    }

    public void setElementMag(int val) {
        if (val > 1) val = defaultElementMag;
        this.elementMag = val;
    }

    public double getLatitude() {
        double val = latLonWidget.getLat();
        Double dbl = new Double(val);
        if (dbl.isNaN()) val = defaultLat;
        if (val < -90.0 || val > 90.0) val = defaultLat;
        this.latitude = val;
        return this.latitude;
    }

    public void setLatitude(double val) {
        if (val < -90.0 || val > 90.0) val = defaultLat;
        latLonWidget.setLat(val);
        this.latitude = val;
        this.resetLatLon = false;
    }

    public double getLongitude() {
        double val = latLonWidget.getLon();
        Double dbl = new Double(val);
        if (dbl.isNaN()) val = defaultLon;
        if (val < -180.0 || val > 180.0) val = defaultLon;
        this.longitude = val;
        return this.longitude;
    }

    public void setLongitude(double val) {
        if (val < -180.0 || val > 180.0) val = defaultLon;
        latLonWidget.setLon(val);
        this.longitude = val;
        this.resetLatLon = false;
    }

    protected void convertToLatLon() {
        try {
            AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
            elelin[0][0] = (double)getElement();
            elelin[1][0] = (double)getLine();
            latlon = macs.toReference(elelin);
            setLatitude(latlon[0][0]);
            setLongitude(latlon[1][0]);
        } catch (Exception e) {
            System.out.println("convertToLatLon e=" + e);
        }
    }

    protected void convertToLinEle() {
        try {
            AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
            latlon[0][0] = getLatitude();
            latlon[1][0] = getLongitude();
            elelin = macs.fromReference(latlon);
            setLine((int)elelin[1][0]);
            setElement((int)elelin[0][0]);
        } catch (Exception e) {
            System.out.println("convertToLinEle e=" + e);
        }
    }

    private void convert() {
        if (flipFlag == 0) {
            flipFlag = 1;
            convertToLinEle();
            this.isLineEle = true;
        } else {
            flipFlag = 0;
            convertToLatLon();
            this.isLineEle = false;
        }
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
        int value = getElementMagValue();
        setElementMag(value);
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
            int value = getLineMagValue();
            setLineMag(value);
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
            int lMag = getLineMagValue();
            int eMag = getElementMagValue();
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
     * Popup a centering menu
     *
     * @param near component to popup near
     * @param latLonWidget _more_
     */

    public void popupCenterMenu(JComponent near,
                                final LatLonWidget latLonWidget) {
        List menuItems = new ArrayList();
        List menus = new ArrayList();
        if (!getPlace().equals(PLACE_CENTER)) cyclePlace();
        LatLonPoint center = new LatLonPointImpl(getLatitude(), getLongitude());
        menuItems.add(makeLocationMenuItem(center, "Center"));
        GeoLocationInfo gli = getGeoLocationInfo();
        LatLonPoint upperLeft = new LatLonPointImpl(latlon[0][0], latlon[1][0]);
        menuItems.add(makeLocationMenuItem(upperLeft, "Upper Left"));
        LatLonPoint upperRight =new LatLonPointImpl(latlon[0][2], latlon[1][2]); 
        menuItems.add(makeLocationMenuItem(upperRight, "Upper Right"));
        LatLonPoint lowerLeft =new LatLonPointImpl(latlon[0][3], latlon[1][3]); 
        menuItems.add(makeLocationMenuItem(lowerLeft, "Lower Left"));
        LatLonPoint lowerRight =new LatLonPointImpl(latlon[0][1], latlon[1][1]); 
        menuItems.add(makeLocationMenuItem(lowerRight, "Lower Right"));
        menus.add(GuiUtils.makeMenu("Corner Points", menuItems));
        GuiUtils.showPopupMenu(menuItems, near);
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

    public int getPreviewEleRes() {
        return this.previewEleRes;
    }

    private String truncateNumericString(String str, int numDec) {
        int indx = str.indexOf(".") + numDec + 1;
        return str.substring(0,indx);
    }
}
