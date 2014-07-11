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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private static final Logger logger = LoggerFactory.getLogger(PreviewSelection.class);
	
      DataChoice dataChoice;
      FlatField image;
      boolean isLL;
      MapProjection sampleProjection;

      double[] x_coords = new double[2];
      double[] y_coords = new double[2];
      boolean hasSubset = false;
      boolean selectionOutOfBounds = false;
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;

      DataSourceImpl dataSource;

      DataCategory dataCategory;
      private SubsetRubberBandBox rbb = null;

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

        rbb = new SubsetRubberBandBox(isLL, image, ((MapProjectionDisplay)mapProjDsp).getDisplayCoordinateSystem(), 1);
        rbb.setColor(Color.green);
        rbb.addAction(new CellImpl() {
          boolean init = false;
          
          public void doAction()
             throws VisADException, RemoteException
           {
        	  logger.debug("doAction in...");
        	  
             if (!init) {
               init = true;
               return;
             }
             Gridded2DSet set = rbb.getBounds();

             float[] low = set.getLow();
             float[] hi = set.getHi();

             // TJJ Apr 2014 
             // The fact that we can even get here with invalid bounding boxes  
             // (edges == Infinity) is another problem that should be investigated.
             // For now we should at least let the user know they selected off 
             // the valid data bounds
             
             if ((low[0] == Float.NEGATIVE_INFINITY) || (low[0] == Float.POSITIVE_INFINITY)) 
            	 selectionOutOfBounds = true;
             if ((hi[0] == Float.NEGATIVE_INFINITY) || (hi[0] == Float.POSITIVE_INFINITY)) 
            	 selectionOutOfBounds = true;
             if ((low[1] == Float.NEGATIVE_INFINITY) || (low[1] == Float.POSITIVE_INFINITY)) 
            	 selectionOutOfBounds = true;
             if ((hi[1] == Float.NEGATIVE_INFINITY) || (hi[1] == Float.POSITIVE_INFINITY)) 
            	 selectionOutOfBounds = true;
             
	       	  if (selectionOutOfBounds) {
	            	JOptionPane.showMessageDialog(null, 
	          			"Data selection is not valid, please select within preview bounds", 
	          			"Data Selection Error", JOptionPane.ERROR_MESSAGE);
	            	selectionOutOfBounds = false;
	          	return;
	      	  }
             
             // TJJ Mar 2014
             // The checks below are because the subset rubber-band box selector is
             // able to select regions outside the data bounds, which causes 
             // errors.  The simplest solution was to check the selection bounds
             // and constrain them if they go outside data bounds.
             
             x_coords[0] = low[0];
             if (x_coords[0] < 0)  {
            	 logger.debug("Constraining X lo bound: " + low[0] + " to: " + 0);
            	 x_coords[0] = 0;
             }
             x_coords[1] = hi[0];
             int lineMax = rbb.getLineMax();
             if (x_coords[1] > lineMax) {
            	 logger.debug("Constraining X hi bound: " + hi[0] + " to: " + lineMax);
            	 x_coords[1] = lineMax;
             }

             y_coords[0] = low[1];
             if (y_coords[0] < 0) {
            	 logger.debug("Constraining Y lo bound: " + low[1] + " to: " + 0);
            	 y_coords[0] = 0;
             }
             y_coords[1] = hi[1];
             int elemMax = rbb.getElemMax();
             if (y_coords[1] > elemMax) {
            	 logger.debug("Constraining Y hi bound: " + hi[1] + " to: " + elemMax);
            	 y_coords[1] = elemMax;
             }

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
    	   JPanel panel = new JPanel(new BorderLayout());
    	   panel.add(BorderLayout.CENTER, dspMaster.getDisplayComponent());
    	   return panel;
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

      /**
       * Enable or disable region subsetting
       * 
       * @param b true or false 
       */
      
      public void enableSubsetting(boolean b) {
    	  try {
    		  rbb.setVisible(b);
    	  } catch (RemoteException | VisADException e) {
    		  e.printStackTrace();
    	  }
      }
	
  }
