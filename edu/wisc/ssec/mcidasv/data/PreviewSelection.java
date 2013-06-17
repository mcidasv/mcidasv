/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2013
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JPanel;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.Range;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.MapLines;

import visad.BaseColorControl;
import visad.CellImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded2DSet;
import visad.RealType;
import visad.SampledSet;
import visad.ScalarMap;
import visad.VisADException;
import visad.data.mcidas.BaseMapAdapter;
import visad.georef.MapProjection;

import edu.wisc.ssec.mcidasv.control.LambertAEA;
import edu.wisc.ssec.mcidasv.data.hydra.HydraContext;
import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.SubsetRubberBandBox;

public class PreviewSelection extends DataSelectionComponent {
      DataChoice dataChoice;
      FlatField image;
      boolean isLL;
      MapProjection sampleProjection;

      double[] x_coords = new double[2];
      double[] y_coords = new double[2];
      boolean hasSubset = false;
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;

      DataSourceImpl dataSource;

      DataCategory dataCategory;

      static SampledSet lines_outlsupu = null;
      static SampledSet lines_outlsupw = null;
      static SampledSet lines_outlhpol = null;
                                    

      public PreviewSelection() {
        super("Region");
      }

      public PreviewSelection(final DataChoice dataChoice, FlatField image,
             MapProjection sample) throws VisADException, RemoteException {
        this(dataChoice, image, sample, null, null);
      }

      public PreviewSelection(final DataChoice dataChoice, FlatField image,
             MapProjection sample, Range displayRange, byte[][] colorTable) throws VisADException, RemoteException {
        super("Region");

        this.dataChoice = dataChoice;
        this.dataCategory = (DataCategory) dataChoice.getCategories().get(0);
        this.dataSource = (DataSourceImpl) ((DirectDataChoice)dataChoice).getDataSource();
        this.image = image;
        this.sampleProjection = sample;
        sample = getDataProjection();

        DisplayConventions dspConv = dataSource.getDataContext().getIdv().getDisplayConventions();

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
        HydraRGBDisplayable imageDsp = new HydraRGBDisplayable("image", imageRangeType, null, true, null);
        imageDsp.setData(image);

        dspMaster.addDisplayable(imageDsp);

        MapLines mapLines  = new MapLines("maplines");
        URL      mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPU");
        try {
            if (lines_outlsupu == null) {
              BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
              lines_outlsupu = (SampledSet) mapAdapter.getData();
            }
            mapLines.setMapLines(lines_outlsupu);
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
            if (lines_outlsupw == null) {
              BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
              lines_outlsupw = (SampledSet) mapAdapter.getData();
            }
            mapLines.setMapLines(lines_outlsupw);
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
            if (lines_outlhpol == null) {
              BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
              lines_outlhpol = (SampledSet) mapAdapter.getData();
            }
            mapLines.setMapLines(lines_outlhpol);
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }



        Hashtable table = dataChoice.getProperties();
        Enumeration keys = table.keys();
        while (keys.hasMoreElements()) {
           Object key = keys.nextElement();
           if (key instanceof MultiDimensionSubset) {
             hasSubset = true;
             MultiDimensionSubset select = (MultiDimensionSubset) table.get(key);
             HydraContext hydraContext = HydraContext.getHydraContext(dataSource, dataCategory);
             // HydraContext hydraContext = HydraContext.getHydraContext();
             if (hydraContext.getMultiDimensionSubset() == null) {
                hydraContext.setMultiDimensionSubset(select);
             }
           }
        }

        final SubsetRubberBandBox rbb =
            new SubsetRubberBandBox(isLL, image, ((MapProjectionDisplay)mapProjDsp).getDisplayCoordinateSystem(), 1);
        rbb.setColor(Color.green);
        rbb.addAction(new CellImpl() {
          boolean init = false;
          public void doAction()
             throws VisADException, RemoteException
           {
             if (!init) {
               init = true;
               return;
             }
             Gridded2DSet set = rbb.getBounds();
             float[] low = set.getLow();
             float[] hi = set.getHi();
             x_coords[0] = low[0];
             x_coords[1] = hi[0];

             y_coords[0] = low[1];
             y_coords[1] = hi[1];

             if (hasSubset) {
               HydraContext hydraContext = HydraContext.getHydraContext(dataSource, dataCategory);
               // HydraContext hydraContext = HydraContext.getHydraContext();
               MultiDimensionSubset select = hydraContext.getMultiDimensionSubset();
               HashMap map = select.getSubset();

               double[] coords0 = (double[]) map.get("Track");
               coords0[0] = y_coords[0];
               coords0[1] = y_coords[1];
               coords0[2] = 1;
               double[] coords1 = (double[]) map.get("XTrack");
               coords1[0] = x_coords[0];
               coords1[1] = x_coords[1];
               coords1[2] = 1;
               
               hydraContext.setMultiDimensionSubset(new MultiDimensionSubset(map));
             }
           }
        });
        dspMaster.addDisplayable(rbb);

        ScalarMap colorMap = imageDsp.getColorMap();
        Range[] range = GridUtil.fieldMinMax(this.image);
        Range imageRange = range[0];
        double max;
        double min;
        double dMax = imageRange.getMax();
        double dMin = imageRange.getMin();
        String name = this.dataChoice.getName();

        float[][] clrTbl = BaseColorControl.initTableGreyWedge(new float[4][256], true);

        if (name.endsWith("BRIT")) {
           dMin = imageRange.getMin();
           min = dMax;
           max = dMin;
        } 
        else if (imageRangeType.getName().contains("Reflectance")) {
           min = dMax;
           max = 0.0;
        }
        else if (imageRangeType.getName().equals("BrightnessTemp")) {
           max = dMax*1.06;
           min = dMax * 0.74;
        }
        else {
           Range rng = dspConv.getParamRange(name, null);
           max = dMax;
           min = dMin;
           ColorTable ct = dspConv.getParamColorTable(name);
           clrTbl = ct.getTable();
        }
        colorMap.setRange(min, max);

        /*-  must to draw first so colorMap has a Control */
        dspMaster.draw();

        BaseColorControl clrCntrl = (BaseColorControl) colorMap.getControl();
        clrCntrl.setTable(clrTbl);
      }

       public MapProjection getDataProjection() {
         MapProjection mp = null;

         if (image == null) return mp;

         Rectangle2D rect = MultiSpectralData.getLonLatBoundingBox(image);
         try {
           mp = new LambertAEA(rect);
         } catch (Exception e) {
             System.out.println(" getDataProjection"+e);
         }
         return mp;
      }

      public JComponent doMakeContents() {
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
         if (hasSubset) {
           HydraContext hydraContext = HydraContext.getHydraContext(dataSource, dataCategory);
           // HydraContext hydraContext = HydraContext.getHydraContext();
           Hashtable table = dataChoice.getProperties();
           table.put(MultiDimensionSubset.key, hydraContext.getMultiDimensionSubset());

           table = dataSelection.getProperties();
           table.put(MultiDimensionSubset.key, hydraContext.getMultiDimensionSubset());

           dataChoice.setDataSelection(dataSelection);
         }
      }
  }
