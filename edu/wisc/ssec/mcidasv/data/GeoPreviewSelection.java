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
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.GeoSelectionPanel;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.geoloc.*;
import ucar.unidata.util.Range;
import ucar.unidata.util.Misc;

import visad.Data;
import visad.FlatField;
import visad.GriddedSet;
import visad.Gridded2DSet;
import visad.SampledSet;
import visad.Unit;
import visad.VisADException;
import visad.georef.MapProjection;
import visad.data.mcidas.BaseMapAdapter;

import java.io.File;
import java.net.URL;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.geom.Rectangle2D;

import visad.*;
import visad.bom.RubberBandBoxRendererJ3D;
import visad.java3d.DisplayImplJ3D;
import visad.java3d.TwoDDisplayRendererJ3D;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.control.DisplayControlBase;
//import ucar.unidata.util.GuiUtils;
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


public class GeoPreviewSelection extends DataSelectionComponent {

      /** Property for image default value place */
      protected static final String PROP_PLACE = "PLACE";

      /** Property for image default value line/ele */
      protected static final String PROP_LATLON = "LATLON";

      /** Property for image default value size */
      protected static final String PROP_SIZE = "SIZE";

      /** flag for upper left */
      private static final String PLACE_ULEFT = "ULEFT";

      /** Property for image default value mag */
      protected static final String PROP_MAG = "MAG";

      DataChoice dataChoice;
      FlatField image;
      boolean isLL;
      MapProjection sampleProjection;

      double[] x_coords = new double[2];
      double[] y_coords = new double[2];
      boolean hasSubset = true;
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;

      final private SubsetRubberBandBox rbb;
      private SubsetRubberBandBox box;
      private int lineMag;
      private int elementMag;

      private GeoLatLonSelection laloSel;
                                    
      public GeoPreviewSelection(DataChoice dataChoice, FlatField image,
             GeoLatLonSelection laLoSel,
             MapProjection sample, int lMag, int eMag, boolean showPreview) 
             throws VisADException, RemoteException {
        super("Region");
/*
        System.out.println("GeoPreviewSelection:");
        System.out.println("    dataChoice=" + dataChoice);
        System.out.println("    sample=" + sample);
        System.out.println("    lMag=" + lMag + " eMag=" + eMag);
*/
        this.dataChoice = dataChoice;
        this.image = image;
        this.laloSel = laLoSel;
        this.sampleProjection = sample;
        if (lMag > 0)
            this.lineMag = lMag;
        if (eMag > 0)
            this.elementMag = eMag;
        sample = getDataProjection();

        if (this.sampleProjection == null) {
            this.sampleProjection = sample;
        }

         isLL = sampleProjection.isLatLonOrder();

        mapProjDsp = new MapProjectionDisplayJ3D(MapProjectionDisplay.MODE_2Din3D);
        mapProjDsp.enableRubberBanding(false);
        dspMaster = mapProjDsp;
        mapProjDsp.setMapProjection(sampleProjection);
        RealType imageRangeType = 
          (((FunctionType)image.getType()).getFlatRange().getRealComponents())[0];
        HydraRGBDisplayable imageDsp = 
          new HydraRGBDisplayable("image", imageRangeType, null, true, null);

        if (showPreview) imageDsp.setData(image);

        MapLines mapLines  = new MapLines("maplines");
        URL      mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPU");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }

        mapLines  = new MapLines("maplines");
        mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPW");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }

        mapLines  = new MapLines("maplines");
        mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLHPOL");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }

        if (showPreview) dspMaster.addDisplayable(imageDsp);

        rbb =
            new SubsetRubberBandBox(isLL, image, ((MapProjectionDisplay)mapProjDsp).getDisplayCoordinateSystem(), 1);
        SubsetRubberBandBox box = rbb;
        rbb.setColor(Color.white);
        rbb.addAction(new CellImpl() {
          public void doAction()
             throws VisADException, RemoteException
           {
             Gridded2DSet set = rbb.getBounds();
              float[] low = set.getLow();
              float[] hi = set.getHi();
              x_coords[0] = low[0];
              x_coords[1] = hi[0];
              y_coords[0] = low[1];
              y_coords[1] = hi[1];

              int line = (int)y_coords[0];
              int ele = (int)x_coords[0];
              if ((laloSel != null) && (line > 0) && (ele > 0)) {
                  laloSel.setPlace(laloSel.PLACE_CENTER);
                  if (!laloSel.isLineEle) laloSel.locationPanel.flip();
                  int lineMid = (int)((y_coords[0] + y_coords[1])/2.0 + 0.5);
                  int eleMid = (int)((x_coords[0] + x_coords[1])/2.0 + 0.5);
                  laloSel.setCenterCoords(eleMid, lineMid);
                  laloSel.convertToLatLon();
                  double uLLine = y_coords[1];
                  double uLEle = x_coords[0];
                  int height = (int)(y_coords[1] - y_coords[0]);
                  if (height < 0) {
                      height *= -1;
                      uLLine = y_coords[0];
                  }
                  int width = (int)(x_coords[1] - x_coords[0]);
                  if (width < 0) {
                      width *= -1;
                      uLEle = x_coords[1];
                  }
                  laloSel.setULCoords(uLEle, uLLine);

                  McIDASVAREACoordinateSystem mcs = (McIDASVAREACoordinateSystem)sampleProjection; 
                  int[] dirBlk = mcs.getDirBlock();
                  int linRes = dirBlk[11];
                  int eleRes = dirBlk[12];

                  height *= linRes;
                  width *= eleRes;
                  if (lineMag > 0) {
                      height /= lineMag;
                  } else if (lineMag < 0) {
                      height *= -lineMag;
                  }
                  if (elementMag > 0) {
                      width /= elementMag;
                  } else if (elementMag < 0) {
                      width *= -elementMag;
                  }

                  laloSel.setNumLines(height);
                  laloSel.setNumEles(width);
              }
           }
        });
        dspMaster.addDisplayable(rbb);

        dspMaster.draw();
        ScalarMap colorMap = imageDsp.getColorMap();
        if (showPreview) {
            Range[] range = GridUtil.fieldMinMax(this.image);
            Range imageRange = range[0];
            int max;
            int min;
            double dMax = imageRange.getMax();
            double dMin = imageRange.getMin();
            String name = this.dataChoice.getName();
            DataSelection ds = this.dataChoice.getDataSelection();
            if (ds != null) {
                GeoSelection gs = ds.getGeoSelection();
             }
            if (name.endsWith("TEMP")) {
               min = (int)(dMax);
               max = (int)(dMin);
            } else { 
               max = (int)(dMin);
               min = (int)(dMax); 
            }
            colorMap.setRange(min, max);
            BaseColorControl clrCntrl = (BaseColorControl) colorMap.getControl();
            clrCntrl.setTable(BaseColorControl.initTableGreyWedge(new float[4][256], true));
        }
      }

       public MapProjection getDataProjection() {
         MapProjection mp = null;
         Rectangle2D rect = MultiSpectralData.getLonLatBoundingBox(image);
         try {
           mp = new LambertAEA(rect);
         } catch (Exception e) {
             System.out.println(" getDataProjection"+e);
         }
         return mp;
      }


      protected JComponent doMakeContents() {
        try {
          JPanel panel = new JPanel(new BorderLayout());
          panel.add("Center", dspMaster.getDisplayComponent());
          return panel;
        }
        catch (Exception e) {
          System.out.println(e);
        }
        return null;
      }
                                                                                                                                             
      public void applyToDataSelection(DataSelection dataSelection) {
         //System.out.println("\nfrom GeoPreviewSelection:");
         if (dataSelection == null) {
             dataSelection = new DataSelection(true);
         }
         double[][] linele = new double[2][2];
         linele[1][1] = y_coords[0];
         linele[0][0] = x_coords[0];
         linele[1][0] = y_coords[1];
         linele[0][1] = x_coords[1];
      
         McIDASVAREACoordinateSystem mcs = (McIDASVAREACoordinateSystem)sampleProjection; 
         try {
             double[][] latlon = mcs.toReference(linele);
/*
             System.out.println("linele[0][0]=" + linele[0][0] + " linele[1][0]=" + linele[1][0] +
                               " linele[0][1]=" + linele[0][1] + " linele[1][1]=" + linele[1][1]);
             System.out.println("latlon[0][0]=" + latlon[0][0] + " latlon[1][0]=" + latlon[1][0] +
                               " latlon[0][1]=" + latlon[0][1] + " latlon[1][1]=" + latlon[1][1]);
*/
             GeoLocationInfo geoInfo = new GeoLocationInfo(latlon[0][0], latlon[1][0],
                                                           latlon[0][1], latlon[1][1]);
             GeoSelection geoSelection = new GeoSelection(geoInfo);
             dataSelection.setGeoSelection(geoSelection);
             int lMag = this.lineMag;
             int eMag = this.elementMag;
             lMag = this.laloSel.getLineMagValue();
             eMag = this.laloSel.getElementMagValue(); 
             dataSelection.putProperty(PROP_MAG, (lMag  + " " + eMag));
             dataChoice.setDataSelection(dataSelection);
         } catch (Exception e) {
             System.out.println("Exception e=" + e);
         }
/*
         GeoSelection geoSelection = dataSelection.getGeoSelection(true);
         if (geoSelection == null) return;
         GeoLocationInfo bbox = geoSelection.getBoundingBox();
         LatLonPoint llp = bbox.getUpperLeft();
         System.out.println("llp=" + llp + "\n");
*/
      }

      public SubsetRubberBandBox getRBB() {
          return rbb;
      }

      public void setRBB(SubsetRubberBandBox rubberBBox) {
          box = rubberBBox;
      }
}
