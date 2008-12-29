/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.data;

import edu.wisc.ssec.mcidas.AreaDirectory;

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
import visad.georef.EarthLocationTuple;
import visad.georef.MapProjection;
import visad.data.mcidas.BaseMapAdapter;

import java.io.File;
import java.net.URL;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Insets;
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


public class GeoLatLonSelection extends DataSelectionComponent {

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
      private static final String[] ADVANCED_PROPS = {
          PROP_UNIT, PROP_BAND, PROP_PLACE, PROP_LOC, PROP_SIZE, PROP_MAG,
          PROP_NAV
      };

      /** This is the list of labels used for the advanced gui */
      private static final String[] ADVANCED_LABELS = {
        "Data Type:", "Channel:", "Placement:", "Location:", "   Image Size:",
        "Magnification:", "Navigation Type:"
      };

      /** Input for lat/lon center point */
      protected LatLonWidget latLonWidget;

      /** Widget to hold the number of elements in the advanced */
      JTextField numElementsFld;

      /** Widget to hold  the number of lines   in the advanced */
      JTextField numLinesFld;

      /** Widget for the line  center point in the advanced section */
      JTextField centerLineFld;

      /** Widget for the element  center point in the advanced section */
      JTextField centerElementFld;

      /** Label used for the line center */
      private JLabel centerLineLbl;

      /** Label used for the element center */
      private JLabel centerElementLbl;

      /** Label used for the center latitude */
      private JLabel centerLatLbl;

      /** Label used for the center longitude */
      private JLabel centerLonLbl;

      private JPanel magPanel;

      /** Widget for the line magnfication in the advanced section */
      JSlider lineMagSlider;

      /** Label for the line mag. in the advanced section */
      JLabel lineMagLbl;

      /** Widget for the element magnfication in the advanced section */
      JSlider elementMagSlider;

      /** Label for the element mag. in the advanced section */
      JLabel elementMagLbl;

      /** location panel */
      protected GuiUtils.CardLayoutPanel locationPanel;

      /** flag for setting properties */
      private boolean amSettingProperties = false;

      /** place label */
      private JLabel placeLbl;

      /** the place string */
      private String place = PLACE_CENTER;
      private int numLines;
      private int numEles;
      private double latitude = 999.0;
      private double longitude = 999.0;
      private int line = -1;
      private int element = -1;
      private int lineMag;
      private int elementMag;
      protected boolean isLineEle;

      private Hashtable properties;
      private int uLLine;
      private int uLEle;
      private int centerLine;
      private int centerEle;

      /** Maps the PROP_ property name to the gui component */
      private Hashtable propToComps = new Hashtable();

      private JButton locPosButton;
      /** size label */ JLabel sizeLbl;

      private DataSourceImpl dataSource;
      private AreaDirectory previewDir;

      private static int flipFlag = 0;

      private JPanel latLonPanel;
      private JPanel lineElementPanel;
      protected JButton locTypeButton;

      /**
       * limit of slider
       */
      private static final int SLIDER_MAX = 29;

      /**
       *  Keep track of the lines to element ratio
       */
      private double linesToElements = 1.0;
 
      public GeoLatLonSelection(DataSourceImpl dataSource,
             DataChoice dataChoice, Hashtable initProps, MapProjection sample, AreaDirectory dir) 
              throws VisADException, RemoteException {
          super("Lat/Lon");
/*
          System.out.println("GeoLatLonSelection:");
          System.out.println("    dataSource=" + dataSource);
          System.out.println("    previewSel=" + previewSel);
          System.out.println("    dataChoice=" + dataChoice);
          System.out.println("    initProps=" + initProps);
          System.out.println("    sample=" + sample);
          System.out.println("    dir=" + dir);
*/
          this.properties = initProps;
          this.dataSource = dataSource;
          this.dataChoice = dataChoice;
          this.sampleProjection = sample;
          this.previewDir = dir;

          if (properties.containsKey(PROP_PLACE)) {
              this.place = (String)properties.get(PROP_PLACE);
          }

          if (properties.containsKey(PROP_LATLON)) {
              String str = (String)properties.get(PROP_LATLON);
              String[] strs = StringUtil.split(str, " ", 2);
              this.latitude = new Double(strs[0]).doubleValue();
              this.longitude = new Double(strs[1]).doubleValue();
              this.isLineEle = false;
          } else if (properties.containsKey(PROP_LINEELE)) {
              String str = (String)properties.get(PROP_LINEELE);
              String[] strs = StringUtil.split(str, " ", 3);
              this.line = new Integer(strs[0]).intValue();
              this.element = new Integer(strs[1]).intValue();
              this.isLineEle = true;
          } else  {
              this.isLineEle = false;
              if (previewDir != null) {
                  this.latitude = new Double(previewDir.getCenterLatitude());
                  this.longitude = new Double(previewDir.getCenterLongitude());
              }
          }

          if (properties.containsKey(PROP_SIZE)) {
              String str = (String)properties.get(PROP_SIZE);
              String[] strs = StringUtil.split(str, " ", 2);
              this.numLines = new Integer(strs[0]).intValue();
              this.numEles = new Integer(strs[1]).intValue();
          } else {
              this.numLines = new Integer(1000);
              this.numEles = new Integer(1000);
          }

          if (properties.containsKey(PROP_MAG)) {
              String str = (String)properties.get(PROP_MAG);
              String[] strs = StringUtil.split(str, " ", 2);
              this.lineMag = new Integer(strs[0]).intValue();
              this.elementMag = new Integer(strs[1]).intValue();
          } else {
              this.lineMag = 1;
              this.elementMag = 1;
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
                  changePlace(this.place);
                  addPropComp(PROP_PLACE, placeLbl);

                  latLonWidget     = new LatLonWidget();
                  if (!isLineEle) latLonWidget.setLatLon((Double.toString(this.latitude)),
                                                              (Double.toString(this.longitude)));
                  String lineStr = "";
                  String eleStr = "";
                  if ((this.line != 0) && (this.element != 0)) {
                      lineStr =Integer.toString(this.line);
                      eleStr =Integer.toString(this.element);
                  }
                  centerLineFld    = new JTextField(lineStr, 3);
                  final String lineField = "";
/*
                  centerLineFld.addActionListener(new ActionListener() {
                      public void actionPerformed(ActionEvent ae) {
                          //previewSel.ping(getLine());
                      }
                  });
*/
                  centerElementFld = new JTextField(eleStr, 3);
                  final JButton centerPopupBtn =
                      GuiUtils.getImageButton(
                        "/auxdata/ui/icons/MapIcon16.png", getClass());
                  centerPopupBtn.setToolTipText("Center on current displays");

                  centerPopupBtn.addActionListener(new ActionListener() {
                      public void actionPerformed(ActionEvent ae) {
                          //dataSource.getDataContext().getIdv().getIdvUIManager().
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
                  if (isLineEle) locTypeButton.doClick();
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
                  numLinesFld    = new JTextField(Integer.toString(this.numLines), 4);
                  numElementsFld = new JTextField(Integer.toString(this.numEles), 4);
                  numLinesFld.setToolTipText("Number of lines");
                  numElementsFld.setToolTipText("Number of elements");
                  GuiUtils.tmpInsets = dfltGridSpacing;
                  sizeLbl            = GuiUtils.lLabel("");

                  JPanel sizePanel =
                      GuiUtils.left(GuiUtils.doLayout(new Component[] {
                          numLinesFld,
                          new JLabel(" X "), numElementsFld,
                          sizeLbl }, 4, GuiUtils.WT_N, GuiUtils.WT_N));
                  addPropComp(PROP_SIZE, propComp = sizePanel);
              } else if (prop.equals(PROP_MAG)) {
                boolean oldAmSettingProperties = amSettingProperties;
                amSettingProperties = true;
                ChangeListener lineListener =
                    new javax.swing.event.ChangeListener() {
                    public void stateChanged(ChangeEvent evt) {
                        if (amSettingProperties) {
                            return;
                        }
                        lineMagSliderChanged(true);
                    }
                };
                ChangeListener elementListener = new ChangeListener() {
                    public void stateChanged(
                            javax.swing.event.ChangeEvent evt) {
                        if (amSettingProperties) {
                            return;
                        }
                        elementMagSliderChanged(true);
                    }
                };
                JComponent[] lineMagComps =
                    GuiUtils.makeSliderPopup(-SLIDER_MAX, SLIDER_MAX, 0,
                                             lineListener);
                lineMagSlider = (JSlider) lineMagComps[1];
                lineMagSlider.setMajorTickSpacing(1);
                lineMagSlider.setSnapToTicks(true);
                lineMagSlider.setExtent(1);
                lineMagSlider.setValue(this.lineMag);
                lineMagComps[0].setToolTipText(
                    "Change the line magnification");
                JComponent[] elementMagComps =
                    GuiUtils.makeSliderPopup(-SLIDER_MAX, SLIDER_MAX, 0,
                                             elementListener);
                elementMagSlider = (JSlider) elementMagComps[1];
                elementMagSlider.setExtent(1);
                elementMagSlider.setMajorTickSpacing(1);
                elementMagSlider.setSnapToTicks(true);
                elementMagSlider.setValue(this.elementMag);
                elementMagComps[0].setToolTipText(
                    "Change the element magnification");
                lineMagSlider.setToolTipText(
                    "Slide to set line magnification factor");
                String str = Integer.toString(getLineMag());
                lineMagLbl =
                    GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
                elementMagSlider.setToolTipText(
                    "Slide to set element magnification factor");
                str = Integer.toString(getElementMag());
                elementMagLbl =
                    GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
                amSettingProperties = oldAmSettingProperties;


                GuiUtils.tmpInsets  = dfltGridSpacing;

                magPanel = GuiUtils.doLayout(new Component[] {
                                      lineMagLbl,
                                      GuiUtils.inset(lineMagComps[0],
                                          new Insets(0, 4, 0, 0)),
                                      new JLabel("    X "), elementMagLbl,
                                      GuiUtils.inset(elementMagComps[0],
                                          new Insets(0, 4, 0, 0)), }, 6,
                                              GuiUtils.WT_N, GuiUtils.WT_N);
                addPropComp(PROP_MAG, propComp = magPanel);
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
         if (geoInfo == null) {
             dataSelection = new DataSelection(true);
         }

         double lat = getLat();
         double lon = getLon();
         if (lat > 90.0 && lon> 360.0) {
             convertToLatLon();
             lat = getLat();
             lon = getLon();
         }
         String latString = Double.toString(lat);
         latString = latString.substring(0,7);
         String lonString = Double.toString(lon);
         lonString = lonString.substring(0,8);
         dataSelection.putProperty(PROP_LATLON, (latString + " " + lonString));

         dataSelection.putProperty(PROP_PLACE, getPlace());
         dataSelection.putProperty(PROP_MAG, (getLineMag() + " " + getElementMag()));

         GeoSelection geoSelection = new GeoSelection(geoInfo);
         dataSelection.setGeoSelection(geoSelection);
         dataChoice.setDataSelection(dataSelection);

         int nlins = getNumLines();
         int neles = getNumEles();
         if (nlins > 0 && neles > 0) {
             dataSelection.putProperty(PROP_SIZE, (nlins + " " + neles));
         }

         geoSelection = dataSelection.getGeoSelection(true);
         if (geoSelection == null) return;
         GeoLocationInfo bbox = geoSelection.getBoundingBox();
         LatLonRect llr = bbox.getLatLonRect();
         LatLonPoint lowerRight = llr.getLowerRightPoint();
         LatLonPoint lowerLeft = llr.getLowerLeftPoint();
         LatLonPoint upperRight = llr.getUpperRightPoint();
         LatLonPoint upperLeft = llr.getUpperLeftPoint();
         //System.out.println("Upper left = " + upperLeft);
         //System.out.println("Lower left = " + lowerLeft);
         //System.out.println("Upper right = " + upperRight);
         //System.out.println("Lower right = " + lowerRight);

      }

    private GeoLocationInfo getGeoLocationInfo() {
         GeoLocationInfo gli = null;
         //System.out.println("\n\nGeoLatLonSelection getGeoLocationInfo:"); 
         int nlins = getNumLines();
         int neles = getNumEles();
         //System.out.println("    nlins=" + nlins + " neles=" + neles); 

         if (nlins < 0) return gli;

         String plc = getPlace();
         int lin = getLine();
         int ele = getElement();
         double lat = getLat();
         double lon = getLon();
         //System.out.println("    lin=" + lin + " ele=" + ele); 
         if (lin < 0 && ele < 0) {
             //System.out.println("    lat=" + lat + " lon=" + lon); 
             if (lat <= 90.0 && lon < 999.0) {
                 convertToLinEle();
                 lin = getLine();
                 ele = getElement(); 
                 //System.out.println("    converted: lin=" + lin + " ele=" + ele); 
             } else {
                 return gli;
             }
          } else if (lat > 90.0 && lon > 360.0) {
             convertToLatLon();
             lat = getLat();
             lon = getLon();
             //System.out.println("    converted: lat=" + lat + " lon=" + lon); 
          }
            
         int linMag = getLineMag();
         int eleMag = getElementMag();
         //System.out.println("\n\n"); 
/*
         System.out.println("place=" + plc); 
         System.out.println("lat=" + lat); 
         System.out.println("lon=" + lon); 
         System.out.println("line=" + lin); 
         System.out.println("element=" + ele); 
         System.out.println("numLines=" + nlins); 
         System.out.println("numEles=" + neles); 
         System.out.println("lineMag=" + linMag); 
         System.out.println("elementMag=" + eleMag); 
*/
         if ((lat > 90.0) || (lat < -90.0)) {
             return gli;
         }
         //String latString = Double.toString(lat);
         //latString = latString.substring(0,7);
         //String lonString = Double.toString(lon);
         //lonString = lonString.substring(0,8);
         //dataSelection.putProperty(PROP_LATLON, (latString + " " + lonString));
         //dataSelection.putProperty(PROP_PLACE, getPlace());
         McIDASVAREACoordinateSystem macs = (McIDASVAREACoordinateSystem)sampleProjection;
         int[] dirBlk = macs.getDirBlock();
/*
         for (int i=0; i<dirBlk.length; i++) {
             System.out.println("   " + i + ": " + dirBlk[i]);
         }
*/
         //dataSelection.putProperty(PROP_MAG, (linMag + " " + eleMag));
         double[][] elelin = new double[2][2];
         double[][] latlon = new double[2][2];
         try {
             if (lat < 99.0) {
                 latlon[0][0] = lat;
                 latlon[1][0] = lon;
                 latlon[0][1] = lat;
                 latlon[1][1] = lon;
                 elelin = macs.fromReference(latlon);
/*
                 System.out.println("\nlatlon[0][0]=" + latlon[0][0] + " latlon[1][0]=" + latlon[1][0] +
                                   " latlon[0][1]=" + latlon[0][1] + " latlon[1][1]=" + latlon[1][1]);
                 System.out.println("elelin[0][0]=" + elelin[0][0] + " elelin[1][0]=" + elelin[1][0] +
                                   " elelin[0][1]=" + elelin[0][1] + " elelin[1][1]=" + elelin[1][1]);
*/
             } else {
                 elelin[1][0] = (double)(dirBlk[8] + (dirBlk[5] - lin)/dirBlk[11]);
                 elelin[0][0] = (double)((ele - dirBlk[6])/dirBlk[12]);
             }
             int lineRes = dirBlk[11];
             int eleRes = dirBlk[12];
             int dLine = nlins/(2*lineRes);
             int dEle = neles/(2*eleRes);
             //System.out.println("dLine=" + dLine + " dEle=" + dEle);

             if (plc.equals(PLACE_CENTER)) {
                 //System.out.println("\n    elelin[0][0]=" + elelin[0][0]);
                 elelin[0][0] -= dEle;
                 //System.out.println("    elelin[0][0]=" + elelin[0][0]);
                 if (elelin[0][0] > dirBlk[6]) elelin[0][0] = dirBlk[6];
                 //System.out.println("    elelin[0][0]=" + elelin[0][0]);
                 //System.out.println("\n    elelin[1][0]=" + elelin[1][0]);
                 elelin[1][0] += dLine;
                 //System.out.println("    elelin[1][0]=" + elelin[1][0]);
                 if (elelin[1][0] > dirBlk[5]) elelin[1][0] = dirBlk[5];
                 //System.out.println("    elelin[1][0]=" + elelin[1][0]);
             }
             //System.out.println("\n    elelin[0][1]=" + elelin[0][1]);
             elelin[0][1] = elelin[0][0] + 2*dEle;
             //System.out.println("    elelin[0][1]=" + elelin[0][1] + elelin[1][1]=" + elelin[1][1]);
             elelin[1][1] = elelin[1][0] - 2*dLine;
             //System.out.println("    elelin[1][1]=" + elelin[1][1]);
             latlon = macs.toReference(elelin);
/*
             System.out.println("elelin[0][0]=" + elelin[0][0] + " elelin[1][0]=" + elelin[1][0] +
                               " elelin[0][1]=" + elelin[0][1] + " elelin[1][1]=" + elelin[1][1]);
             System.out.println("latlon[0][0]=" + latlon[0][0] + " latlon[1][0]=" + latlon[1][0] +
                               " latlon[0][1]=" + latlon[0][1] + " latlon[1][1]=" + latlon[1][1]);
*/
         } catch (Exception e) {
             System.out.println("Error converting input lat/lon e=" + e);
         }

         //if (nlins > 0 && neles > 0) {
         //    dataSelection.putProperty(PROP_SIZE, (nlins + " " + neles));
         //}

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
        McIDASVAREACoordinateSystem macs = (McIDASVAREACoordinateSystem)sampleProjection;
        int[] dirBlk = macs.getDirBlock();
        int lineRes = dirBlk[11];
        int eleRes = dirBlk[12];
        int lMag = getLineMag();
        int eMag = getElementMag();
        int dLine = getNumLines()/(2*lineRes);
        int dEle = getNumEles()/(2*eleRes);
        if (lMag > 0) {
            dLine *= lMag;
        } else if (lMag < 0) {
            dLine /= -lMag;
        }
        if (eMag > 0) {
            dEle *= eMag;
        } else if (eMag < 0) {
            dEle /= -eMag;
        }
        if (this.place.equals(PLACE_CENTER)) {
            changePlace(PLACE_ULEFT);
            setLine(getLine()+dLine);
            setElement(getElement()-dEle);
        } else {
            changePlace(PLACE_CENTER);
            setLine(getLine()-dLine);
            setElement(getElement()+dEle);
        }
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
        return this.place;
    }

    public void setPlace(String str) {
        this.place = str;
        changePlace(this.place);
    }

    public int getNumLines() {
        this.numLines = new Integer(numLinesFld.getText().trim()).intValue();
        return this.numLines;
    }

    public void setNumLines(int val) {
        numLinesFld.setText(new Integer(val).toString());
        this.numLines = val;
    }

    public int getNumEles() {
        this.numEles = new Integer(numElementsFld.getText().trim()).intValue();
        return this.numEles;
    }

    public void setNumEles(int val) {
        val = (int)((double)val/4.0 + 0.5)*4;
        numElementsFld.setText(new Integer(val).toString());
        this.numEles = val;
    }

    public int getLine() {
        this.line = new Integer(centerLineFld.getText().trim()).intValue();
        return this.line;
    }

    public void setLine(int val) {
        centerLineFld.setText(new Integer(val).toString());
        this.line = val;
    }

    public int getElement() {
        this.element = new Integer(centerElementFld.getText().trim()).intValue();
        return this.element;
    }

    public void setElement(int val) {
        centerElementFld.setText(new Integer(val).toString());
        this.element = val;
    }

    public int getLineMag() {
        return this.lineMag;
    }

    public void setLineMag(int val) {
        this.lineMag = val;
    }

    public int getElementMag() {
        return this.elementMag;
    }

    public void setElementMag(int val) {
        this.elementMag = val;
    }

    public double getLat() {
        this.latitude = latLonWidget.getLat();
        return this.latitude;
    }

    public void setLat(double val) {
        latLonWidget.setLat(val);
        this.latitude = val;
    }

    public double getLon() {
        this.longitude = latLonWidget.getLon();
        return this.longitude;
    }

    public void setLon(double val) {
        latLonWidget.setLon(val);
        this.longitude = val;
    }

    protected void convertToLatLon() {
        try {
            McIDASVAREACoordinateSystem macs = (McIDASVAREACoordinateSystem)sampleProjection;
            double[][] latlon = new double[2][1];
            double[][] elelin = new double[2][1];
            elelin[0][0] = (double)getElement();
            elelin[1][0] = (double)getLine();
            latlon = macs.toReference(elelin);
            setLat(latlon[0][0]);
            setLon(latlon[1][0]);
        } catch (Exception e) {
            System.out.println("convertToLatLon e=" + e);
        }
    }

    protected void convertToLinEle() {
        try {
            McIDASVAREACoordinateSystem macs = (McIDASVAREACoordinateSystem)sampleProjection;
            double[][] latlon = new double[2][1];
            double[][] elelin = new double[2][1];
            latlon[0][0] = getLat();
            latlon[1][0] = getLon();
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
        } else {
            flipFlag = 0;
            convertToLatLon();
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


    private void elementMagSliderChanged(boolean recomputeLineEleRatio) {

        int value = getElementMagValue();
        setElementMag(value);
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
        elementMagLbl.setText(StringUtil.padLeft("" + value,
                                                 4));
    }

    /**
     * Handle the line mag slider changed event 
     *
     * @param evt  the event
     */
    private void lineMagSliderChanged(boolean autoSetSize) {
        try {
            int value = getLineMagValue();
            setLineMag(value);
            lineMagLbl.setText(StringUtil.padLeft("" + value, 4));
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
        LatLonPoint center = new LatLonPointImpl(getLat(), getLon());
        menuItems.add(makeLocationMenuItem(center, "Center"));
        GeoLocationInfo gli = getGeoLocationInfo();
        LatLonRect llr = gli.getLatLonRect();
        LatLonPoint upperLeft = llr.getUpperLeftPoint();
        menuItems.add(makeLocationMenuItem(upperLeft, "Upper Left"));
        LatLonPoint upperRight = llr.getUpperRightPoint();
        menuItems.add(makeLocationMenuItem(upperRight, "Upper Right"));
        LatLonPoint lowerLeft = llr.getLowerLeftPoint();
        menuItems.add(makeLocationMenuItem(lowerLeft, "Lower Left"));
        LatLonPoint lowerRight = llr.getLowerRightPoint();
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
}
