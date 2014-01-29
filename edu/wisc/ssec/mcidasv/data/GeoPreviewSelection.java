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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GeoPreviewSelection extends DataSelectionComponent {

      private static final Logger logger = LoggerFactory.getLogger(GeoPreviewSelection.class);
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

        if (lMag == 0) lMag = 1;
        if (eMag == 0) eMag = 1;
        this.lineMag = lMag;
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
        RealType imageRangeType = (((FunctionType)image.getType()).getFlatRange().getRealComponents())[0];
        HydraRGBDisplayable imageDsp = new HydraRGBDisplayable("image", imageRangeType, null, true, null);

        if (showPreview) imageDsp.setData(image);

        MapLines mapLines  = new MapLines("maplines");
        URL mapSource = mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPU");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            logger.error("can't open map file="+mapSource, excp);
        }

        mapLines  = new MapLines("maplines");
        mapSource = mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPW");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            logger.error("can't open map file="+mapSource, excp);
        }

        mapLines  = new MapLines("maplines");
        mapSource = mapProjDsp.getClass().getResource("/auxdata/maps/OUTLHPOL");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            logger.error("can't open map file="+mapSource, excp);
        }

        if (showPreview) {
            dspMaster.addDisplayable(imageDsp);
        }
        rbb = new GeoSubsetRubberBandBox(isLL, image, ((MapProjectionDisplay)mapProjDsp).getDisplayCoordinateSystem(), 1);
        mvm = new MapViewManager(dataSource.getDataContext().getIdv());
        store = dataSource.getDataContext().getIdv().getStore();
        rbb.setColor((Color)store.get(mvm.PREF_FGCOLOR, Color.GREEN));
        rbb.addAction(new CellImpl() {
          public void doAction() throws VisADException, RemoteException {
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
             logger.error("error while attempting to create new LambertAEA", e);
         }
         return mp;
      }

      public void initBox() {
    	  this.drawBox();
      }

      protected JComponent doMakeContents() {
        try {
          JPanel panel = new JPanel(new BorderLayout());
          panel.add("Center", dspMaster.getDisplayComponent());
          panel.addComponentListener (new ComponentListener() {
              public void componentHidden(ComponentEvent ce) {
                  dspMaster.getDisplayComponent().setVisible(false);
              }
              public void componentShown(ComponentEvent ce) {
                  dspMaster.getDisplayComponent().setVisible(true);
                  drawBox();
                  rbb.resetExtremes();
              }
              public void componentMoved(ComponentEvent ce) {
              }
              public void componentResized(ComponentEvent ce) {
              }
          });
          return panel;
        }
        catch (Exception e) {
            logger.error("error building preview panel", e);
        }
        return null;
      }

      public void setDataChoice(DataChoice choice) {
          logger.trace("oldChoice={} newChoice={}", this.dataChoice, choice);
          this.dataChoice = choice;
      }
      public DataChoice getDataChoice() {
          return this.dataChoice;
      }
      
      private void forceCoords() {
          float[] extrms = rbb.getRanges();
          x_coords[0] = (double)extrms[0];
          y_coords[0] = (double)extrms[1];
          x_coords[1] = (double)extrms[2];
          y_coords[1] = (double)extrms[3];

          int height = (int)(y_coords[1] - y_coords[0]);
          int width = (int)(x_coords[1] - x_coords[0]);
          if ((height < 1) || (width < 1)) return;

          if (laloSel != null) {
              int lineMid = (int)((y_coords[0] + y_coords[1])/2.0 + 0.5);
              int eleMid = (int)((x_coords[0] + x_coords[1])/2.0 + 0.5);
              double uLLine = y_coords[1];
              double uLEle = x_coords[0];
              if (height < 0) {
                  height *= -1;
                  uLLine = y_coords[0];
              }
              if (width < 0) {
                  width *= -1;
                  uLEle = x_coords[1];
              }

              int line = lineMid;
              int ele = eleMid;
              if (laloSel.getPlace().equals(laloSel.PLACE_ULEFT)) {
                  line = (int)Math.floor(uLLine + 0.5);
                  ele = (int)Math.floor(uLEle + 0.5);
              }

              int linRes = laloSel.getPreviewLineRes();
              int eleRes = laloSel.getPreviewEleRes();

              height *= linRes;
              width *= eleRes;
              laloSel.setBaseNumLines(height);
              laloSel.setBaseNumElements(width);

              this.lineMag = laloSel.getLineMag();
              this.elementMag = laloSel.getElementMag();
              if (lineMag > 0) {
                  height *= lineMag;
              } else if (lineMag < 0) {
                  height /= -lineMag;
              }
              if (elementMag > 0) {
                  width *= elementMag;
              } else if (elementMag < 0) {
                  width /= -elementMag;
              }

              Rectangle2D mapArea = sampleProjection.getDefaultMapArea();
              double previewXDim = mapArea.getWidth();
              double previewYDim = mapArea.getHeight();
              double dLin = (double)line;
              double dEle = (double)ele;
              if ((line < 0) || (dLin > previewYDim) ||
                  (ele < 0) || (dEle > previewXDim)) {
                  line = -1;
                  ele = -1;
              }

//              boolean lock = laloSel.getLockOn();
//              laloSel.setLockOn(true);
//              int lineMag = 1;
//              int eleMag = 1;
              laloSel.setNumLines(height);
              laloSel.setNumEles(width);
//              laloSel.setBaseNumLines(height);
//              laloSel.setBaseNumElements(width);
//              laloSel.setLineMag(lineMag);
//              laloSel.setElementMag(eleMag);
//              laloSel.lineMagSlider.setValue(lineMag);
//              laloSel.setLRes(-1.0);
//              laloSel.elementMagSlider.setValue(eleMag);
//              laloSel.setERes(-1.0);
//              laloSel.amUpdating = true;
//              laloSel.lineMagSliderChanged(false);
//              laloSel.elementMagSliderChanged(false);
//              laloSel.amUpdating = false;
//              laloSel.setLockOn(lock);

              laloSel.getGeoLocationInfo(line, ele);
              String type = laloSel.getCoordinateType();
              int pos = 0;
              if (laloSel.getPlace().equals(laloSel.PLACE_ULEFT)) pos = 1;
              if (type.equals(laloSel.TYPE_LATLON)) {
                  double[][] pts = laloSel.getLatLonPoints();
                  laloSel.setLatitude(pts[0][pos]);
                  laloSel.setLongitude(pts[1][pos]);
                  laloSel.convertToLineEle();
              } else {
                  double[][] pts = laloSel.getImagePoints();
                  if (type.equals(laloSel.TYPE_AREA))
                      pts = laloSel.getAreaPoints();
                  laloSel.setElement((int)Math.floor(pts[0][pos] + 0.5));
                  laloSel.setLine((int)Math.floor(pts[1][pos] + 0.5));
                  laloSel.setLineElement();
                  laloSel.convertToLatLon();
              }
          }
      }
           
      @Override public void applyToDataSelection(DataSelection dataSelection) {
      }

      @Override public boolean getShowInControlProperties() {
          return false;
      }

      public void drawBox() {
          if (box == null) makeBox();
          removeRBB();

          double[][] elelin = laloSel.getDisplayELPoints();
          if (elelin == null) return;

          for (int i=0; i<2; i++) {
              for (int j=0; j<5; j++) {
                  Double val = new Double(elelin[i][j]);
                  if (val.isNaN()) {
                      eraseBox();
                      return;
                  }
              }
          }

          float[][] floatVals = new float[][] {
                { (float)elelin[0][0], (float)elelin[0][1], (float)elelin[0][2],
                  (float)elelin[0][3], (float)elelin[0][4] },
                { (float)elelin[1][0], (float)elelin[1][1], (float)elelin[1][2],
                  (float)elelin[1][3], (float)elelin[1][4] }};

          float[][] dispVals = new float[][] {
                { floatVals[0][1], floatVals[0][2], floatVals[0][4],
                  floatVals[0][3], floatVals[0][1] },
                { floatVals[1][1], floatVals[1][2], floatVals[1][4],
                  floatVals[1][3], floatVals[1][1] }
                };

          try {
              float[][] refVals = rbb.getDisplayCoordSystem().toReference(dispVals);
              Gridded2DSet set = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                refVals, 5);
              box.setData(set);
          } catch (Exception e) {
              logger.error("error drawing box", e);
          }
     }

     private void makeBox() {
         if (box == null) {
             try {
                 box = new LineDrawing("box");
                 box.setColor((Color)store.get(mvm.PREF_FGCOLOR, Color.GREEN));
                 dspMaster.addDisplayable(box);
             } catch (Exception e) {
                 logger.error("error making box", e);
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
              logger.error("error erasing box", e);
          }
          addRBB();
     }

     private boolean rBBPresent() {
         Displayable[] dsps = dspMaster.getDisplayables();
         if (dsps.length > 0) {
             for (int i = 0; i < dsps.length; i++) {
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
                 logger.error("error removing rubberband box", e);
             }
         }
         addRBB();
     }

     private void addRBB() {
         if (!rBBPresent()) {
             try {
                 dspMaster.addDisplayable(rbb);
             } catch (Exception e) {
                 logger.error("error adding rubberband box", e);
             }
         }
     }
}
