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

import visad.Data;
import visad.FlatField;
import visad.GriddedSet;
import visad.Gridded2DSet;
import visad.SampledSet;
import visad.VisADException;
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

      double[] x_coords = new double[2];
      double[] y_coords = new double[2];
      boolean hasSubset = true;
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;

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
          "Data Type:", "Channel:", "Placement:", "Location:", "Image Size:",
          "Magnification:", "Navigation Type:"
      };

      /** Input for lat/lon center point */
      protected LatLonWidget latLonWidget;

      /** Widget to hold the number of elements in the advanced */
      JTextField numElementsFld;

      /** Widget to hold  the number of lines   in the advanced */
      JTextField numLinesFld;

      /** Widget for the line  center point in the advanced section */
      protected JTextField centerLineFld;

      /** Widget for the element  center point in the advanced section */
      protected JTextField centerElementFld;

      /** Label used for the line center */
      private JLabel centerLineLbl;

      /** Label used for the element center */
      private JLabel centerElementLbl;

      /** Label used for the center latitude */
      private JLabel centerLatLbl;

      /** Label used for the center longitude */
      private JLabel centerLonLbl;

      /** location panel */
      private GuiUtils.CardLayoutPanel locationPanel;

      /** flag for setting properties */
      private boolean amSettingProperties = false;

      /** place label */
      private JLabel placeLbl;

      /** the place string */
      private String place;

      /** Maps the PROP_ property name to the gui component */
      private Hashtable propToComps = new Hashtable();


      /** size label */ JLabel sizeLbl;

      private DataSourceImpl dataSource;
 
      public GeoLatLonSelection(DataSourceImpl dataSource, DataChoice dataChoice) 
              throws VisADException, RemoteException {
          super("Lat/Lon");
          this.dataSource = dataSource;

          this.dataChoice = dataChoice;
      }

      protected JComponent doMakeContents() {
/* ---------from AddeImageChooser------------------------------------------- */
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
                  changePlace(PLACE_CENTER);
                  addPropComp(PROP_PLACE, placeLbl);

                  latLonWidget     = new LatLonWidget();
                  centerLineFld    = new JTextField("", 3);
                  centerElementFld = new JTextField("", 3);
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
                  final JPanel latLonPanel = GuiUtils.hbox(new Component[] {
                      centerLatLbl = GuiUtils.rLabel(" Lat:" + dfltLblSpacing),
                      latLonWidget.getLatField(),
                      centerLonLbl = GuiUtils.rLabel(" Lon:" + dfltLblSpacing),
                      latLonWidget.getLonField(), new JLabel(" "), centerPopup
                  });

                  final JPanel lineElementPanel =
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

                  JButton locPosButton = GuiUtils.makeImageButton(
                                             "/auxdata/ui/icons/Refresh16.gif",
                                             this, "cyclePlace", null, true);

                  locPosButton.setToolTipText("Change place type");
  
                  JButton locTypeButton =
                      GuiUtils.makeImageButton(
                          "/auxdata/ui/icons/Refresh16.gif", locationPanel,
                          "flip", null, true);
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
                  numLinesFld    = new JTextField("", 4);
                  numElementsFld = new JTextField("", 4);
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
/* ---------------------------------------------------- */
      }
                                                                                                                                             
      public void applyToDataSelection(DataSelection dataSelection) {
/*
         if (dataSelection == null) {
             dataSelection = new DataSelection(true);
         }
         double[][] linele = new double[2][2];
         linele[1][1] = y_coords[0];
         linele[0][0] = x_coords[0];
         linele[1][0] = y_coords[1];
         linele[0][1] = x_coords[1];
       
         try {
             double[][] latlon = ((McIDASVAREACoordinateSystem)sampleProjection).toReference(linele);
             GeoLocationInfo geoInfo = new GeoLocationInfo(latlon[0][0], latlon[1][0],
                                                           latlon[0][1], latlon[1][1]);
             GeoSelection geoSelection = new GeoSelection(geoInfo);
             dataSelection.setGeoSelection(geoSelection);
             dataChoice.setDataSelection(dataSelection);
         } catch (Exception e) {
             System.out.println("Exception e=" + e);
         } 
*/
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
        if (place.equals(PLACE_CENTER)) {
            changePlace(PLACE_ULEFT);
        } else {
            changePlace(PLACE_CENTER);
        }
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
}
