/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
 *
 * All Rights Reserved
 *
 * McIDAS-V is built on Unidata's IDV, as well as SSEC's VisAD and HYDRA
 * projects. Parts of McIDAS-V source code are based on IDV, VisAD, and
 * HYDRA source code.
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.hydra;

import edu.wisc.ssec.adapter.SubsetRubberBandBox;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.HydraContext;

import java.rmi.RemoteException;

import java.util.HashMap;

import edu.wisc.ssec.hydra.data.DataSource;
import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataGroup;
import edu.wisc.ssec.hydra.data.DataSelection;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.util.Range;
import ucar.unidata.util.ColorTable;

import visad.Data;
import visad.FlatField;
import visad.Gridded2DSet;
import visad.VisADException;
import visad.georef.MapProjection;

import visad.*;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineDrawing;


public class PreviewSelection {

      private PreviewDisplay previewDisplay = null;

      DataChoice dataChoice;
      FlatField image;
      boolean isLL;
      MapProjection sampleProjection;

      double[] x_coords = new double[2];
      double[] y_coords = new double[2];
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;

      DataSource dataSource;

      DataGroup dataCategory;

      LineDrawing boxDsp = null;

      LineDrawing outlineDsp = null;

      CoordinateSystem displayCS = null;

      MultiDimensionSubset select = null;

      private HydraContext hydraContext = null;

      static boolean regionMatch = true;

      HydraRGBDisplayable imageDsp;
     
      SubsetRubberBandBox rbb;

      Range imageRange;

      RealType imageRangeType;

      float[][] clrTbl;

      Gridded2DSet boxOutline;


      public PreviewSelection() {
      }

      public PreviewSelection(final DataChoice dataChoice, FlatField image,
             MapProjection sample) throws VisADException, RemoteException {
        this(dataChoice, image, sample, null, null);
      }

      public PreviewSelection(final DataChoice dataChoice, FlatField image,
             MapProjection sample, Range displayRange, byte[][] colorTable) throws VisADException, RemoteException {

        this.dataChoice = dataChoice;
        this.dataCategory = dataChoice.getGroup();
        this.dataSource = (DataSource) ((DataChoice)dataChoice).getDataSource();
        this.image = image;

        Range[] range = GridUtil.fieldMinMax(this.image);
        imageRange = range[0];

        this.sampleProjection = sample;
        sample = getDataProjection(image);
        if (this.sampleProjection == null) {
            this.sampleProjection = sample;
        }
        isLL = sampleProjection.isLatLonOrder();

        hydraContext = HydraContext.getHydraContext(dataSource, dataCategory);
        if (HydraContext.getLastManual() == null) {
           HydraContext.setLastManual(hydraContext);
        }

        imageRangeType = 
          (((FunctionType)image.getType()).getFlatRange().getRealComponents())[0];
      
        String name = this.dataChoice.getName();
        clrTbl = getImageColorTableAndRange(imageRangeType, name, imageRange);


        double[] trkCoords = new double[3];
        double[] xtrkCoords = new double[3];
        getSubsetCoords(dataChoice, trkCoords, xtrkCoords);
        setupFromCoords(trkCoords, xtrkCoords);

        previewDisplay = DataBrowser.getPreviewDisplay();

        updateBoxSelector();
        this.image = fillMissingLines(this.image); 
      }

      public float[][] getImageColorTableAndRange(RealType imageRangeType, String name, Range imageRange) {
         double max;
         double min;
         double dMax = imageRange.getMax();
         double dMin = imageRange.getMin();

         float[][] clrTbl = BaseColorControl.initTableGreyWedge(new float[4][256], true);
         
         String rngName = imageRangeType.getName();

         if (rngName.contains("Reflectance") || rngName.contains("albedo")) {
            clrTbl = BaseColorControl.initTableGreyWedge(new float[4][256]);
            clrTbl = setGamma(clrTbl, 0.70);
            min = dMin;
            max = dMax;
         }
         else if (name.equals("DNB")) {
            clrTbl = BaseColorControl.initTableGreyWedge(new float[4][256]);
            // these apply for incoming log10(dnb_radiance) values --------
            if (dMax > -4.0) {
               min = dMin;
               max = dMax;
            }
            else if (dMin < -10) {
               min = -11;
               max = -7;
            }
            else {
               min = -9;
               max = -7;
            }
           //----------------------------
         }
         else {
            min = dMin;
            max = dMax;
            clrTbl = dataSource.getDefaultColorTable(dataChoice).getColorTable();
            Range rng = dataSource.getDefaultColorRange(dataChoice);
            if (rng != null) {
               min = rng.getMin();
               max = rng.getMax();
            }
            if (clrTbl.length == 3) clrTbl = ColorTable.addAlpha(clrTbl);
         }

         imageRange.setMin(min);
         imageRange.setMax(max);
         return clrTbl;
      }

      public void getSubsetCoords(DataChoice choice, double[] trkCoords, double[] xtrkCoords) {
         // get subset info from incoming DataChoice
         select = (MultiDimensionSubset) choice.getDataSelection();
         if (select != null) {
            HashMap map = select.getSubset();

            double[] coords = (double[]) map.get("Track");
            if (coords == null) {
               coords = (double[]) map.get("GridY");
            }

            trkCoords[0] = coords[0];
            trkCoords[1] = coords[1];
            trkCoords[2] = coords[2];

            coords = (double[]) map.get("XTrack");
            if (coords == null) {
               coords = (double[]) map.get("GridX");
            }

            xtrkCoords[0] = coords[0];
            xtrkCoords[1] = coords[1];
            xtrkCoords[2] = coords[2];
         }
      }

      public void setupFromCoords(double[] trkCoords, double[] xtrkCoords) throws VisADException, RemoteException {
             double xtrkStart = xtrkCoords[0];
             double  trkStart =  trkCoords[0];
             double xtrkStop  = xtrkCoords[1];
             double  trkStop  =  trkCoords[1];
             double xtrkSkip  = xtrkCoords[2];
             double  trkSkip  =  trkCoords[2];

             boxOutline = makeBoxOutline(xtrkStart, xtrkStop, xtrkSkip, trkStart, trkStop, trkSkip);

             //- preset to a sub-region at full-res, show the box
             if ((xtrkStop - xtrkStart) > 640) {
                xtrkStart = Math.floor(xtrkCoords[1]/3);
                trkStart = Math.floor(trkCoords[1]/3);
                xtrkStop = xtrkStart + 640;
                trkStop = trkStart + 640;
             }
             if (trkStop >= trkCoords[1]) trkStop = trkCoords[1] - 2; // in case we go too far
             // A terrible hack for GranuleAggregation problem
             trkStart += 1;

             Gridded2DSet selectBox = makeBoxOutline(xtrkStart, xtrkStop, xtrkCoords[2], trkStart, trkStop, trkCoords[2]);

             // initialize select box for this context
             if (hydraContext.getSelectBox() == null) {
                hydraContext.setSelectBox(new visad.Tuple(new Data[] {selectBox, selectBox}));
             }

             // initialize subset region for this context
             if (hydraContext.getMultiDimensionSubset() == null) {
                MultiDimensionSubset newSubset = select.clone();
                HashMap map = newSubset.getSubset();

                double[] coords0 = (double[]) map.get("Track");
                if (coords0 == null) {
                  coords0 = (double[]) map.get("GridY");
                }
                coords0[0] = trkStart;
                coords0[1] = trkStop;
                coords0[2] = 1;

                double[] coords1 = (double[]) map.get("XTrack");
                if (coords1 == null) {
                  coords1 = (double[]) map.get("GridX");
                }
                coords1[0] = xtrkStart;
                coords1[1] = xtrkStop;
                coords1[2] = 1;

                hydraContext.setMultiDimensionSubset(new MultiDimensionSubset(map));
             }
      }

       public MapProjection getDataProjection(FlatField image) {
         MapProjection mp = null;
         if (image == null) return mp;

         //- get MapProjection from incoming image.  If none, use default method
         FunctionType fnc_type = (FunctionType) image.getType();
         RealTupleType rtt = fnc_type.getDomain();
         CoordinateSystem cs = rtt.getCoordinateSystem();

         if (cs instanceof visad.CachingCoordinateSystem) {
           cs = ((visad.CachingCoordinateSystem)cs).getCachedCoordinateSystem();
         }

         if (cs instanceof MapProjection) {
           return (MapProjection) cs;
         }


         try {
           float[][] corners = MultiSpectralData.getLonLatBoundingCorners(image.getDomainSet());
           mp = new LambertAEA(corners);
         } catch (Exception e) {
             System.out.println(" getDataProjection"+e);
         }
         return mp;
      }

      public void applyToDataSelection(DataSelection dataSelection) {
         updateHydraContext();
         
         MultiDimensionSubset select = hydraContext.getMultiDimensionSubset();
         
         ((MultiDimensionSubset)dataSelection).setCoords(((MultiDimensionSubset)dataChoice.getDataSelection()).getSubset());
         //if (((MultiDimensionSubset)dataSelection).isEmtpy()) {
         if (false) { // Don't replace incoming dataSelection with contents hydraContex, just the spatial info.
            ((MultiDimensionSubset)dataSelection).setCoords(select.getSubset());
         }
         else {
            String key = "XTrack";
            double[] coords = select.getCoords(key);
            if (coords != null) {
               ((MultiDimensionSubset)dataSelection).setCoords(key, coords);
            }
            else {
               key = "GridX";
               coords = select.getCoords(key);
               if (coords != null) {
                  ((MultiDimensionSubset)dataSelection).setCoords(key, coords);              
               }
            }

            key = "Track";
            coords = select.getCoords(key);
            if (coords != null) {
               ((MultiDimensionSubset)dataSelection).setCoords(key, coords);
            }
            else {
               key = "GridY";
               coords = select.getCoords(key);
               if (coords != null) {
                  ((MultiDimensionSubset)dataSelection).setCoords(key, coords);              
               }            
            }
         }

         dataChoice.setDataSelection(dataSelection);
      }
 
      public HydraContext getHydraContext() {
         return hydraContext;
      }
      
      private void updateHydraContext() {
           try {
              HydraContext lastContext = HydraContext.getLastManual();
              if (lastContext != hydraContext) {
                 if (Hydra.getRegionMatching() || 
                      (lastContext.getDataSource() == hydraContext.getDataSource())) {
                    syncRegionFrom(lastContext);
                 }
              }
           }
           catch (VisADException e) {
             System.out.println(e);
           }
           catch (RemoteException e) {
             System.out.println(e);
           }         
      }

      public void updateBoxSelector() {
        try {
           HydraContext lastContext = HydraContext.getLastManual();
           if (hydraContext == lastContext) {
              previewDisplay.setBox((Gridded2DSet) ((visad.Tuple)hydraContext.getSelectBox()).getComponent(0));               
           }
           else if (!Hydra.getRegionMatching()) {
              if (lastContext.getDataSource() == hydraContext.getDataSource()) {
                 previewDisplay.setBox(syncRegionFrom(lastContext));                    
              }
              else {
                 previewDisplay.setBox((Gridded2DSet) ((visad.Tuple)hydraContext.getSelectBox()).getComponent(0));
              }
           }
           else {
              previewDisplay.setBox(syncRegionFrom(lastContext));                 
           }
        }
        catch (VisADException e) {
          System.out.println(e);
        }
        catch (RemoteException e) {
          System.out.println(e);
        }
      }

      public Gridded2DSet syncRegionFrom(HydraContext lastContext) throws VisADException, RemoteException {
         Gridded2DSet set = (Gridded2DSet) ((visad.Tuple)lastContext.getSelectBox()).getComponent(1);
         float[][] lonlat = set.getSamples();
         RealTupleType domain = ((FunctionType)image.getType()).getDomain();
         CoordinateSystem cs = domain.getCoordinateSystem();
         float[][] points = cs.fromReference(lonlat);
         int numPts = points[0].length;
         boolean overlap = true;
         int cnt = 0;
         for (int k=0; k<points[0].length; k++) {
            if (Float.isNaN(points[0][k]) || Float.isNaN(points[1][k])) cnt++;
         }
         if (cnt/numPts > 0.8) overlap = false;

         Gridded2DSet box;
         if (overlap) {
            float[] xlohi = Hydra.minmax(points[0]);
            float[] ylohi = Hydra.minmax(points[1]);
            double xskip = (xlohi[1] - xlohi[0])/60.0;
            double yskip = (ylohi[1] - ylohi[0])/60.0;

            box = makeBoxOutline(xlohi[0], xlohi[1], xskip, ylohi[0], ylohi[1], yskip);

            MultiDimensionSubset newSubset = select.clone();
            HashMap map = newSubset.getSubset();

            // bounds check just in case
            Gridded2DSet domSet = (Gridded2DSet) image.getDomainSet();
            float[] hi = domSet.getHi();
            if (xlohi[0] < 0) xlohi[0] = 0;
            if (ylohi[0] < 0) ylohi[0] = 0;
            if (xlohi[1] > hi[0]) xlohi[1] = hi[0];
            if (ylohi[1] > hi[1]) ylohi[1] = hi[1];

            double[] coords0 = (double[]) map.get("Track");
            if (coords0 == null) {
              coords0 = (double[]) map.get("GridY");
            }

            coords0[0] = ylohi[0];
            coords0[1] = ylohi[1];
            coords0[2] = 1;

            double[] coords1 = (double[]) map.get("XTrack");
            if (coords1 == null) {
              coords1 = (double[]) map.get("GridX");
            }

            coords1[0] = xlohi[0];
            coords1[1] = xlohi[1];
            coords1[2] = 1;

            hydraContext.setMultiDimensionSubset(new MultiDimensionSubset(map));
            hydraContext.setSelectBox(new visad.Tuple(new Data[] {box, box}));            
         }
         else {
            // set empty, but not null -> Exception
            box = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                  new float[][] {{0},{0}}, 1);
         }
         return box;
      }

      Gridded2DSet makeBoxOutline(double xtrkStart, double xtrkStop, double xtrkSkip,
                                  double trkStart, double trkStop, double trkSkip) throws VisADException, RemoteException {
         //- make the data region outline box
         int numXtrk = (int) ((xtrkStop - xtrkStart)/xtrkSkip);
         int numtrk = (int) ((trkStop - trkStart)/trkSkip);
         float[][] points = new float[2][(2*numXtrk+2*numtrk)-8];

         int cnt = 0;
         for (int t=1; t<numXtrk-1; t++) {
            points[0][cnt] = (float) (xtrkStart + xtrkSkip*t);
            points[1][cnt] = (float) (trkStart + trkSkip);
            cnt++;
         }
         for (int t=1; t<numtrk-1; t++) {
            points[1][cnt] = (float) (trkStart + trkSkip*t);
            points[0][cnt] = (float) (xtrkStop - xtrkSkip);
            cnt++;
         }
         for (int t=1; t<numXtrk-1; t++) {
            points[0][cnt] = (float) (xtrkStop - xtrkSkip*t);
            points[1][cnt] = (float) (trkStop - trkSkip);
            cnt++;
         }
         for (int t=1; t<numtrk-1; t++) {
            points[1][cnt] = (float) (trkStop - trkSkip*t);
            points[0][cnt] = (float) (xtrkStart + xtrkSkip);
            cnt++;
         }

         RealTupleType domain = ((FunctionType)image.getType()).getDomain();
         CoordinateSystem cs = domain.getCoordinateSystem();
         float[][] new_points = cs.toReference(points);
         Gridded2DSet llset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new_points, points[0].length);

         /** will need catesian coords for off earth geo scenes
         double[][] points3D = new double[][] {points[1], points[0], new double[points[0].length]};
         RealTupleType dspXYtype = displayCS.getReference();
         points3D = displayCS.toReference(points3D);
         float[][] flts = Set.doubleToFloat(points3D);
         Gridded2DSet gset = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][] {flts[0], flts[1]}, flts[0].length);
         return gset;
         */

         return llset;
      }

   private float[][] setGamma(float[][] clrTable, double gamma) {
     float[][] newClrTbl = getZeroOutArray(clrTable);

     for (int k=0; k<clrTable[0].length; k++) {
       newClrTbl[0][k] = (float) Math.pow(clrTable[0][k], gamma);
       newClrTbl[1][k] = (float) Math.pow(clrTable[1][k], gamma);
       newClrTbl[2][k] = (float) Math.pow(clrTable[2][k], gamma);
     }

     return newClrTbl;
   }

   /* only for cases where a single line is missing with non-missing lines obove/below */
   public FlatField fillMissingLines(FlatField ff) throws VisADException, RemoteException {
      Gridded2DSet dset = (Gridded2DSet) ff.getDomainSet();
      float[][] rngVals = ff.getFloats(true);
      FlatField newFF = new FlatField((FunctionType)ff.getType(), dset);
      int[] lens = dset.getLengths();
      for (int j=1; j<lens[1]-1; j++) {
         for (int i=0; i<lens[0]; i++) {
            int idx = j*lens[0] + i;
            if (Float.isNaN(rngVals[0][idx])) {
               rngVals[0][idx] = (rngVals[0][idx+lens[0]] + rngVals[0][idx-lens[0]])/2;
            }
         }
      }
      newFF.setSamples(rngVals, false);
      return newFF;
   }

   public float[][] getZeroOutArray(float[][] array) {
     float[][] newArray = new float[array.length][array[0].length];
     for (int i=0; i<newArray.length; i++) {
       for (int j=0; j<newArray[0].length; j++) {
         newArray[i][j] = 0f;
       }
     }
     if (newArray.length == 4) { // test if table has alpha component
        for (int j=0; j<newArray[0].length; j++) {
            newArray[3][j] = 1.0f; // initialize to opaque
        }
     }
     return newArray;
   }

   public static FlatField makePreviewImage(DataSource dataSource, DataChoice choice, String sourceDescription) throws Exception {

      FlatField image = (FlatField) dataSource.getData(choice, null);
      if (sourceDescription != null) {
         if (sourceDescription.contains("CrIS")) {
            image = edu.wisc.ssec.adapter.CrIS_SDR_Utility.makeViewableCrISpreview(image);
         }
      }

      return image;
   } 

  }
