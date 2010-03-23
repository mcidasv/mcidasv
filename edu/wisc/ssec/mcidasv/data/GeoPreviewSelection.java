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

import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.control.LambertAEA;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.net.URL;
import java.rmi.RemoteException;

import javax.swing.*;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.util.Range;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.MapLines;

import visad.*;
import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.BaseMapAdapter;
import ucar.visad.display.Displayable;
import ucar.visad.display.LineDrawing;
import visad.georef.MapProjection;


public class GeoPreviewSelection extends DataSelectionComponent {


      DataChoice dataChoice;
      FlatField image;
      boolean isLL;
      MapProjection sampleProjection;

      double[] x_coords = new double[2];
      double[] y_coords = new double[2];
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;
      MapViewManager mvm;
      IdvObjectStore store;

      final private GeoSubsetRubberBandBox rbb;
      private int lineMag;
      private int elementMag;

      private GeoLatLonSelection laloSel;

      private LineDrawing box;
                                    
      public GeoPreviewSelection(DataSourceImpl dataSource,
             DataChoice dataChoice, FlatField image,
             GeoLatLonSelection laLoSel,
             MapProjection sample, int lMag, int eMag, boolean showPreview) 
             throws VisADException, RemoteException {
        super("Region");

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
            System.err.println("Can't open map file " + mapSource);
            System.err.println(excp);
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
            System.err.println("Can't open map file " + mapSource);
            System.err.println(excp);
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
            System.err.println("Can't open map file " + mapSource);
            System.err.println(excp);
        }

        if (showPreview) dspMaster.addDisplayable(imageDsp);
        rbb =
            new GeoSubsetRubberBandBox(isLL, image, ((MapProjectionDisplay)mapProjDsp).getDisplayCoordinateSystem(), 1);
        mvm = new MapViewManager(dataSource.getDataContext().getIdv());
        store = dataSource.getDataContext().getIdv().getStore();
        rbb.setColor((Color)store.get(mvm.PREF_FGCOLOR, Color.GREEN));
        rbb.addAction(new CellImpl() {
          public void doAction()
             throws VisADException, RemoteException
           {
              eraseBox();
              forceCoords();
           }
        });
        addRBB();
        makeBox();

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
             System.err.println("GeoPreviewSelection getDataProjection: e="+e);
         }
         return mp;
      }


      protected JComponent doMakeContents() {
        try {
          JPanel panel = new JPanel(new BorderLayout());
          panel.add("Center", dspMaster.getDisplayComponent());
          panel.addComponentListener (new ComponentListener() {
              public void componentHidden(ComponentEvent ce) {
              }
              public void componentShown(ComponentEvent ce) {
                  drawBox();
              }
              public void componentMoved(ComponentEvent ce) {
              }
              public void componentResized(ComponentEvent ce) {
              }
          });
          return panel;
        }
        catch (Exception e) {
          System.err.println("GeoPreviewSelection doMakeContents: e=" + e);
        }
        return null;
      }

      private void forceCoords() {
          float[] extrms = rbb.getRanges();
          x_coords[0] = (double)extrms[0];
          y_coords[0] = (double)extrms[1];
          x_coords[1] = (double)extrms[2];
          y_coords[1] = (double)extrms[3];

          int line = (int)y_coords[1];
          int ele = (int)x_coords[1];
          if ((laloSel != null) && (line > 0) && (ele > 0)) {
              laloSel.setCoordinateType(laloSel.TYPE_LATLON);
              laloSel.setPlace(laloSel.PLACE_CENTER);
              if (laloSel.getIsLineEle()) laloSel.flipLocationPanel(0);
              int lineMid = (int)((y_coords[0] + y_coords[1])/2.0 + 0.5);
              int eleMid = (int)((x_coords[0] + x_coords[1])/2.0 + 0.5);
              laloSel.convertToLatLon(eleMid, lineMid);
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

              AREACoordinateSystem mcs = (AREACoordinateSystem)sampleProjection;
              int[] dirBlk = mcs.getDirBlock();
              int linRes = laloSel.getPreviewLineRes();
              int eleRes = laloSel.getPreviewEleRes();

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
              laloSel.setLineMag(1);
              laloSel.setElementMag(1);
              laloSel.lineMagSlider.setValue(1);
              laloSel.setLRes(-1.0);
              laloSel.lineMagSliderChanged(false);
              laloSel.elementMagSlider.setValue(1);
              laloSel.setERes(-1.0);
              laloSel.elementMagSliderChanged(false);
              laloSel.getGeoLocationInfo();
          }
      }
           
      public void applyToDataSelection(DataSelection dataSelection) {
      }

      protected void drawBox() {
          if (box == null) makeBox();
          removeRBB();
          double[][] latlon = laloSel.getLatLonPoints();
          if (latlon == null) return;
          for (int i=0; i<2; i++) {
              for (int j=0; j<5; j++) {
                  Double val = new Double(latlon[i][j]);
                  if (val.isNaN()) {
                      eraseBox();
                      return;
                  }
              }
          }
          try {
              Gridded2DSet set = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                  new float[][] {
                { (float)latlon[0][1], (float)latlon[0][2], (float)latlon[0][4],
                  (float)latlon[0][3], (float)latlon[0][1] },
                { (float)latlon[1][1], (float)latlon[1][2], (float)latlon[1][4],
                  (float)latlon[1][3], (float)latlon[1][1] }
                }, 5);
              box.setData(set);
          } catch (Exception e) {
              System.err.println("GeoPreviewSelection drawBox: e=" + e);
          }
     }

     private void makeBox() {
         if (box == null) {
             try {
                 box = new LineDrawing("box");
                 box.setColor((Color)store.get(mvm.PREF_FGCOLOR, Color.GREEN));
                 dspMaster.addDisplayable(box);
             } catch (Exception e) {
                 System.err.println("GeoPreviewSelection makeBox: e=" + e);
             }
         }
     }

      private void eraseBox() {
          Gridded2DSet set = null;
          if (box == null) makeBox();
          try {
              set = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                  new float[][] {
                { (float)0.0, (float)0.0 },
                { (float)0.0, (float)0.0 },
                }, 2);
              box.setData(set);
          } catch (Exception e) {
              System.err.println("GeoPreviewSelection eraseBox: e=" + e);
          }
          addRBB();
     }

     private boolean rBBPresent() {
         Displayable[] dsps = dspMaster.getDisplayables();
         if (dsps.length > 0) {
             for (int i = 0; i<dsps.length; i++) {
                 Displayable disp = dsps[i];
                 if (disp == (Displayable)rbb) {
                     return true;
                 }
             }
         }
         return false;
     }

     private void removeRBB() {
         if (rBBPresent()) {
             try {
                 dspMaster.removeDisplayable(rbb);
             } catch (Exception e) {
                 System.err.println("GeoPreviewSelection removeRBB: e=" + e);
             }
         }
         addRBB();
     }

     private void addRBB() {
         if (!rBBPresent()) {
             try {
                 dspMaster.addDisplayable(rbb);
             } catch (Exception e) {
                 System.err.println("GeoPreviewSelection addRBB: e=" + e);
             }
         }
     }
}
