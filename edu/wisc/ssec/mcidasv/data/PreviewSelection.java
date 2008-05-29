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
import edu.wisc.ssec.mcidasv.control.LambertAEA;

import java.rmi.RemoteException;

import java.util.ArrayList;
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

import ucar.unidata.util.Misc;

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


public class PreviewSelection extends DataSelectionComponent {
      DataChoice dataChoice;
      FlatField image;

      double[] x_coords = new double[2];
      double[] y_coords = new double[2];
      boolean hasSubset = true;
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;
                                                                                                                                             
      public PreviewSelection(DataChoice dataChoice, FlatField image) throws VisADException, RemoteException {
        super("Region");
        this.dataChoice = dataChoice;
        this.image = image;
        mapProjDsp = new MapProjectionDisplayJ3D(MapProjectionDisplay.MODE_2Din3D);
        mapProjDsp.enableRubberBanding(false);
        dspMaster = mapProjDsp;
        mapProjDsp.setMapProjection(getDataProjection());
        RealType imageRangeType = 
          (((FunctionType)image.getType()).getFlatRange().getRealComponents())[0];
/* ??? need to generalize ??? */
        HydraRGBDisplayable imageDsp = new HydraRGBDisplayable("image", imageRangeType, null, true, null);
        imageDsp.setData(image);

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

        dspMaster.addDisplayable(imageDsp);

        final SubsetRubberBandBox rbb =
            new SubsetRubberBandBox(image, ((MapProjectionDisplay)mapProjDsp).getDisplayCoordinateSystem(), 1);
        rbb.setColor(Color.green);
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
           }
        });
        dspMaster.addDisplayable(rbb);

        dspMaster.draw();
        ScalarMap colorMap = imageDsp.getColorMap();
        colorMap.setRange(225, 320);
        BaseColorControl clrCntrl = (BaseColorControl) colorMap.getControl();
        clrCntrl.setTable(BaseColorControl.initTableGreyWedge(new float[4][256], true));
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
         HashMap map = ((MultiDimensionSubset)dataChoice.getDataSelection()).getSubset();

         double[] coords0 = (double[]) map.get("Track");
               coords0[0] = y_coords[0];
               coords0[1] = y_coords[1];
               coords0[2] = 1;
         double[] coords1 = (double[]) map.get("XTrack");
               coords1[0] = x_coords[0];
               coords1[1] = x_coords[1];
               coords1[2] = 1;
         if (hasSubset) {
           dataChoice.setDataSelection(new MultiDimensionSubset(map));
         }
      }
  }
