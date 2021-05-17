/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2021
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
package edu.wisc.ssec.mcidasv.data.hydra;

import java.rmi.RemoteException;
import java.util.Arrays;

import visad.CoordinateSystem;
import visad.FlatField;
import visad.FunctionType;
import visad.Linear2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.SetType;
import visad.VisADException;
import visad.util.ThreadManager;

public class ReprojectSwath {
  private static int count = 0;

  Linear2DSet grid;
  Linear2DSet swathDomain;
  FunctionType ftype;
  float[][] swathRange;
  CoordinateSystem swathCoordSys;
  CoordinateSystem gridCoordSys;

  float[][] allSwathGridCoords;
  int[] allSwathGridIndexs;
  float[][] swathGridCoord;
  int[] swathIndexAtGrid;

  int trackLen;
  int xtrackLen;

  int gridXLen;
  int gridYLen;
  int gridLen;

  float[][] gridRange;
  int rngTupDim;
  FlatField grdFF;

  int[][][] quads;
  int mode;
  
  public static final int NEAREST = 1;
  public static final int BILINEAR_VISAD = 0;
  
  int numProc = Runtime.getRuntime().availableProcessors();
  private static boolean doParallel = false;

  private static ReprojectSwath lastReproject = null;

  public static void setDoParallel(boolean enable) {
     doParallel = enable;
  }
  
  public static FlatField swathToGrid(Linear2DSet grid, FlatField[] swaths, int mode) throws Exception {
     return swathToGrid(grid, swaths, mode, true);
  }
  
  public static FlatField swathToGrid(Linear2DSet grid, FlatField[] swaths, int mode, boolean filter) throws Exception {
    FunctionType ftype = (FunctionType) swaths[0].getType();
    visad.Set domSet = swaths[0].getDomainSet();

    FlatField swath = new FlatField(new FunctionType(ftype.getDomain(),
        new RealTupleType(new RealType[] 
           {RealType.getRealType("redimage_"+count), RealType.getRealType("greenimage_"+count), RealType.getRealType("blueimage_"+count)})), domSet);

    swath.setSamples(new float[][]
        {swaths[0].getFloats(false)[0], swaths[1].getFloats(false)[0], swaths[2].getFloats(false)[0]});

    count++;

    return swathToGrid(grid, swath, mode);
  }
  
  public static FlatField swathToGrid(Linear2DSet grid, FlatField swath, int mode) throws Exception {
      return swathToGrid(grid, swath, mode, true);
  }
  
  public static FlatField swathToGrid(Linear2DSet grid, FlatField swath, int mode, boolean filter) throws Exception {
    ReprojectSwath obj = null;
    FlatField ff = null;

    if (lastReproject != null) {
       if (grid.equals(lastReproject.grid) && (swath.getDomainSet()).equals(lastReproject.swathDomain)) {
         obj = lastReproject;
         ff = obj.reproject(swath, mode, filter);
       }
       else {
         obj = new ReprojectSwath(grid, swath);
         ff = obj.reproject(mode, filter);
       }
    }
    else {
      obj = new ReprojectSwath(grid, swath);
      ff = obj.reproject(mode, filter);
    }
    lastReproject = obj;

    return ff;
  }
  
  public ReprojectSwath() {
  }
  
  public ReprojectSwath(Linear2DSet grid, FlatField swath) throws Exception {
      
    init(grid, swath);
    
    if (trackLen < 200 || doParallel == false) {
        numProc = 1;
    }

    projectSwathToGrid();
  }
  
  private void init(Linear2DSet grid, FlatField swath) throws VisADException {
     this.grid = grid;
     gridLen = grid.getLength();
     int[] lens = grid.getLengths();
     gridXLen = lens[0];
     gridYLen = lens[1];
     gridCoordSys = grid.getCoordinateSystem();
     
     swathDomain = (Linear2DSet) swath.getDomainSet();
     lens = swathDomain.getLengths();
     trackLen = lens[1];
     xtrackLen = lens[0];
     int swathLen = trackLen*xtrackLen;
     swathCoordSys = swathDomain.getCoordinateSystem();
     swathRange = swath.getFloats(false);
     ftype = (FunctionType) swath.getType();
    
     allSwathGridCoords = new float[2][swathLen];
     allSwathGridIndexs = new int[swathLen];
     swathGridCoord = new float[2][gridLen];
     swathIndexAtGrid = new int[gridLen];
    
     quads = new int[gridYLen][gridXLen][4];
   }

  /*
  public FlatField reproject(Linear2DSet grid, FlatField swath, int mode, boolean filter) throws Exception {
    init();
    
    if (trackLen < 200 || doParallel == false) {
        numProc = 1;
    }
  
    return reproject(mode, filter);
  }
  */
    
  public FlatField reproject(int mode, boolean filter) throws Exception {
    this.mode = mode;
    
    initGrid();

    getBoundingQuadAtGridPts();

    interpolateToGrid();
    
    if (filter) {
       grdFF.setSamples(filter(), false);
    }
    else {
       grdFF.setSamples(gridRange, false);
    }
    
    return grdFF;
  }

  private FlatField reproject(FlatField swath, int mode, boolean filter) throws Exception {
     this.mode = mode;
     swathRange = swath.getFloats(false);
     ftype = (FunctionType) swath.getType();
     
     initGrid();
     
     getBoundingQuadAtGridPts();
     
     interpolateToGrid();
     
     if (filter) {
       grdFF.setSamples(filter(), false);
     }
     else {
       grdFF.setSamples(gridRange, false);
     }
     
     return grdFF;
  }
  
   private void getBoundingQuadAtGridPts() throws VisADException, RemoteException {
    int ystart = 3;
    int ystop = gridYLen-4;
    int subLen = ((ystop - ystart)+1)/numProc;
    int rem = ((ystop - ystart)+1) % numProc;
    
    
    ThreadManager threadManager = new ThreadManager("getBoundingQuadAtGridPts");
    for (int i=0; i<numProc; i++) {
        final int start = i*subLen + ystart;
        final int stop = (i != numProc-1 ) ? (start + subLen - 1): (start + subLen + rem - 1);
          threadManager.addRunnable(new ThreadManager.MyRunnable() {
                  public void run()  throws Exception {
                     getBoundingQuadAtGridPts(start, stop);
                  }
              });
    }
    
    if (numProc == 1 || !doParallel) {
       threadManager.runSequentially();
    }
    else {
       threadManager.runAllParallel();
    }
  }

  // start to stop inclusive
  private void getBoundingQuadAtGridPts(int grdYstart, int grdYstop) {

    for (int j=grdYstart; j<=grdYstop; j++) {
       for (int i=3; i<gridXLen-3; i++) {
          int grdIdx = i + j*gridXLen;

          int ll = findSwathGridLoc(grdIdx, swathGridCoord, gridYLen, gridXLen, "LL");
          quads[j][i][0] = ll;

          int lr = findSwathGridLoc(grdIdx, swathGridCoord, gridYLen, gridXLen, "LR");
          quads[j][i][1] = lr;

          int ul = findSwathGridLoc(grdIdx, swathGridCoord, gridYLen, gridXLen, "UL");
          quads[j][i][2] = ul;

          int ur = findSwathGridLoc(grdIdx, swathGridCoord, gridYLen, gridXLen, "UR");
          quads[j][i][3] = ur;
       }
    }
  }
  
  public void interpolateToGrid() throws VisADException, RemoteException {
    int ystart = 3;
    int ystop = gridYLen-4;
    int subLen = ((ystop - ystart)+1)/numProc;
    int rem = ((ystop - ystart)+1) % numProc;
    
    ThreadManager threadManager = new ThreadManager("interpolateToGrid");
    for (int i=0; i<numProc; i++) {
        final int start = i*subLen + ystart;
        final int stop = (i != numProc-1 ) ? (start + subLen - 1): (start + subLen + rem - 1);
          threadManager.addRunnable(new ThreadManager.MyRunnable() {
                  public void run()  throws Exception {
                     interpolateToGrid(start, stop);
                  }
              });
    }
    
    if (numProc == 1 || !doParallel) {
       threadManager.runSequentially();
    }
    else {
       threadManager.runAllParallel();
    }
  }

  // start to stop inclusive
  public void interpolateToGrid(int grdYstart, int grdYstop) throws VisADException, RemoteException {

    float[][] corners = new float[2][4];
    float[][] rngVals = new float[rngTupDim][4];
    float[] values = new float[4];
    float gx;
    float gy;

    for (int j=grdYstart; j<=grdYstop; j++) {
       for (int i=3; i<gridXLen-3; i++) {
          int grdIdx = i + j*gridXLen;
          gx = (float) (grdIdx % gridXLen);
          gy = (float) (grdIdx / gridXLen);

          java.util.Arrays.fill(corners[0], Float.NaN);
          java.util.Arrays.fill(corners[1], Float.NaN);
        
          int ll = quads[j][i][0];
          int lr = quads[j][i][1];
          int ul = quads[j][i][2];
          int ur = quads[j][i][3];

          if (ll >= 0) {
             corners[0][0] = swathGridCoord[0][ll] - gx;
             corners[1][0] = swathGridCoord[1][ll] - gy;
          }
          if (lr >= 0) {
             corners[0][1] = swathGridCoord[0][lr] - gx;
             corners[1][1] = swathGridCoord[1][lr] - gy;
          }
          if (ul >= 0) {
             corners[0][2] = swathGridCoord[0][ul] - gx;
             corners[1][2] = swathGridCoord[1][ul] - gy;
          }
          if (ur >= 0) {
             corners[0][3] = swathGridCoord[0][ur] - gx;
             corners[1][3] = swathGridCoord[1][ur] - gy;
          }

          if (mode == NEAREST) { // Nearest neighbor
             for (int t=0; t<rngTupDim; t++) {
                java.util.Arrays.fill(values, Float.NaN);
                if (ll >=0) {
                   values[0] = swathRange[t][swathIndexAtGrid[ll]];
                }
                if (lr >= 0) {
                   values[1] = swathRange[t][swathIndexAtGrid[lr]];
                }
                if (ul >= 0) {
                   values[2] = swathRange[t][swathIndexAtGrid[ul]];
                }
                if (ur >= 0) {
                   values[3] = swathRange[t][swathIndexAtGrid[ur]];
                }
                float val = nearest(0f, 0f, corners, values);
                gridRange[t][grdIdx] = val;
             }
          }
          else if (mode == BILINEAR_VISAD) {  //from VisAD
             if (!(ll >= 0 && lr >= 0 && ul >= 0 && ur >= 0)) {
                continue;
             }
             corners[0][0] = swathGridCoord[0][ll];
             corners[1][0] = swathGridCoord[1][ll];
             corners[0][1] = swathGridCoord[0][lr];
             corners[1][1] = swathGridCoord[1][lr];
             corners[0][2] = swathGridCoord[0][ul];
             corners[1][2] = swathGridCoord[1][ul];
             corners[0][3] = swathGridCoord[0][ur];
             corners[1][3] = swathGridCoord[1][ur];
             for (int t=0; t<rngTupDim; t++) {
                java.util.Arrays.fill(values, Float.NaN);
                values[0] = swathRange[t][swathIndexAtGrid[ll]];
                values[1] = swathRange[t][swathIndexAtGrid[lr]];
                values[2] = swathRange[t][swathIndexAtGrid[ul]];
                values[3] = swathRange[t][swathIndexAtGrid[ur]];
                float val = visad2D(gy, gx, corners, values);
                gridRange[t][grdIdx] = val;
             }
          }
          else if (mode == 1) { //TODO: not working yet
             if (!(ll >= 0 && lr >= 0 && ul >= 0 && ur >= 0)) {
                continue;
             }
             gx -= swathGridCoord[0][ll];
             gy -= swathGridCoord[1][ll];
             corners[0][0] = 0f;
             corners[1][0] = 0f;
             corners[0][1] = swathGridCoord[0][ul] - swathGridCoord[0][ll];
             corners[1][1] = swathGridCoord[1][ul] - swathGridCoord[1][ll];
             corners[0][2] = swathGridCoord[0][ur] - swathGridCoord[0][ll];
             corners[1][2] = swathGridCoord[1][ur] - swathGridCoord[1][ll];
             corners[0][3] = swathGridCoord[0][lr] - swathGridCoord[0][ll];
             corners[1][3] = swathGridCoord[1][lr] - swathGridCoord[1][ll];
             for (int t=0; t<rngTupDim; t++) {
                java.util.Arrays.fill(values, Float.NaN);
                values[0] = swathRange[t][swathIndexAtGrid[ll]];
                values[1] = swathRange[t][swathIndexAtGrid[ul]];
                values[2] = swathRange[t][swathIndexAtGrid[ur]];
                values[3] = swathRange[t][swathIndexAtGrid[lr]];
                float val = biLinearIntrp(gy, gx, corners, values);
                gridRange[t][grdIdx] = val;
             }

          }
       }
    }

  }

 public void projectSwathToGrid() throws VisADException, RemoteException {
    int subLen = trackLen/numProc;
    int rem = trackLen % numProc;
    
    ThreadManager threadManager = new ThreadManager("projectSwathToGrid");
    for (int i=0; i<numProc; i++) {
        final int start = i*subLen;
        final int stop = (i != numProc-1 ) ? (start + subLen - 1): (start + subLen + rem - 1);
          threadManager.addRunnable(new ThreadManager.MyRunnable() {
                  public void run()  throws Exception {
                     projectSwathToGrid(start, stop);
                  }
              });
    }
    
    if (numProc == 1 || !doParallel) {
       threadManager.runSequentially();
    }
    else {
       threadManager.runAllParallel();
    }
 }
 
 public void projectSwathToGrid(int trackStart, int trackStop) throws VisADException, RemoteException {

    for (int j=trackStart; j <= trackStop; j++) {
       for (int i=0; i < xtrackLen; i++) {
         int swathIdx = j*xtrackLen + i;

         float[][] swathCoord = swathDomain.indexToValue(new int[] {swathIdx});
         float[][] swathEarthCoord = swathCoordSys.toReference(swathCoord);

         float[][] gridValue = gridCoordSys.fromReference(swathEarthCoord);
         float[][] gridCoord = grid.valueToGrid(gridValue);
         float g0 = gridCoord[0][0];
         float g1 = gridCoord[1][0];
         int grdIdx = (g0 != g0 || g1 != g1) ? -1 : ((int) g0) + gridXLen * ((int) g1);
         int m=0;
         int n=0;
         int k = grdIdx + (m + n*gridXLen);
         
         allSwathGridCoords[0][swathIdx] = g0;
         allSwathGridCoords[1][swathIdx] = g1;
         allSwathGridIndexs[swathIdx] = k;
             
       }
    }
 }
 
 public void initGrid() throws VisADException {
    Arrays.fill(swathGridCoord[0], -999.9f);
    Arrays.fill(swathGridCoord[1], -999.9f);
    Arrays.fill(swathIndexAtGrid, -1);

    for (int j=0; j < trackLen; j++) {
       for (int i=0; i < xtrackLen; i++) {
         int swathIdx = j*xtrackLen + i;
         float val = swathRange[0][swathIdx];
         int k = allSwathGridIndexs[swathIdx];
         
         if ( !(Float.isNaN(val)) && ((k >=0) && (k < gridLen)) ) { // val or val[rngTupDim] ?
            if (swathIndexAtGrid[k] == -1) {
               swathGridCoord[0][k] = allSwathGridCoords[0][swathIdx];
               swathGridCoord[1][k] = allSwathGridCoords[1][swathIdx];
               swathIndexAtGrid[k] = swathIdx;
            }
         }
       }
    }
    
    for (int j=0; j<gridYLen; j++) {
       for (int i=0; i<gridXLen; i++) {
          java.util.Arrays.fill(quads[j][i], -1);
       }
    }
    
    RealTupleType rtt = ((SetType)grid.getType()).getDomain();
    grdFF = new FlatField(new FunctionType(rtt, ftype.getRange()), grid);
    gridRange = grdFF.getFloats(false);
    rngTupDim = gridRange.length;
    for (int t=0; t<rngTupDim; t++) {
       java.util.Arrays.fill(gridRange[t], Float.NaN);
    }
 }

 private float[][] filter() throws VisADException, RemoteException {

    double mag = 3.0;
    double sigma = 0.4;

    float[][] weights = new float[3][3];

    float sumWeights = 0f;
    for (int n=-1; n<=1; n++) {
       for (int m=-1; m<=1; m++) {
          double del_0 = m;
          double del_1 = n;
          double dst = Math.sqrt(del_0*del_0 + del_1*del_1);

          weights[n+1][m+1] = (float) (mag/Math.exp(dst/sigma));

          sumWeights += weights[n+1][m+1];
       }
    }

    for (int n=-1; n<=1; n++) {
       for (int m=-1; m<=1; m++) {
          weights[n+1][m+1] /= sumWeights;
        }
    }

    float[][] newRange = new float[rngTupDim][gridLen];
    for (int t=0; t<rngTupDim; t++) {
       java.util.Arrays.fill(newRange[t], Float.NaN);
    }
    float[] sum = new float[rngTupDim];

    for (int j=2; j<gridYLen-2; j++) {
       for (int i=2; i<gridXLen-2; i++) {
         int grdIdx = i + j*gridXLen;

         java.util.Arrays.fill(sum, 0f);
         for (int n=-1; n<=1; n++) {
            for (int m=-1; m<=1; m++) {
               int k = grdIdx + (m + n*gridXLen);

               for (int t=0; t<rngTupDim; t++) {
                  sum[t] += weights[n+1][m+1]*gridRange[t][k];
               }
            }
         }

         for (int t=0; t<rngTupDim; t++) {
            newRange[t][grdIdx] = sum[t];
         }
       }
    }

    return newRange;
 }
 private static int findSwathGridLoc(int grdIdx, float[][] swathGridCoord, int gridYLen, int gridXLen, String which) {
  
    int idx = -1;

    int gy = grdIdx/gridXLen;
    int gx = grdIdx % gridXLen;
    
    int yu1 = (gy+1)*gridXLen;
    int yu2 = (gy+2)*gridXLen;
    int yu3 = (gy+3)*gridXLen;    
    
    int yd1 = (gy-1)*gridXLen;
    int yd2 = (gy-2)*gridXLen;
    int yd3 = (gy-3)*gridXLen;

    switch (which) {
       case "LL":

          idx = yd1 + (gx-1);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd2 + (gx-1);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd1 + (gx-2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd2 + (gx-2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }


          idx = yd2 + (gx-3);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd3 + (gx-2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd3 + (gx-1);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd1 + (gx-3);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd3 + (gx-3);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }

          idx = -1;
          break;
       case "UL":
          idx = (gy)*gridXLen + (gx-1);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = (gy)*gridXLen + (gx-2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yu1 + (gx-1);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yu1 + (gx-2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }

          idx = yu1 + (gx-3);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yu2 + (gx-3);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yu2 + (gx-2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = (gy)*gridXLen + (gx-3);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd1 + (gx-3);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }

          idx = -1;
          break;
       case "UR":
          idx = (gy)*gridXLen + (gx);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yu1 + (gx);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = (gy)*gridXLen + (gx+1);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yu1 + (gx+1);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }

          idx = yu2 + (gx+1);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yu1 + (gx+2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yu2 + (gx+2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = (gy)*gridXLen + (gx+2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd1 + (gx+2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }

          idx = -1;
          break;
       case "LR":
          idx = yd1 + (gx);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd1 + (gx+1);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd2 + (gx);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd2 + (gx+1);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }

          idx = yd1 + (gx+2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd2 + (gx+2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd3 + (gx);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd3 + (gx+1);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
          idx = yd3 + (gx+2);
          if (swathGridCoord[0][idx] != -999.9f) {
             break;
          }
 
          idx = -1;
          break;
    }

    return idx;
 }

 /* Reference: David W. Zingg, University of Toronto, Downsview, Ontario, Canada
               Maurice Yarrow, Sterling Software, Arnes Research Center, Moffett Field, California. 
               NASA Technical Memorandum 102213

     y -> q, x -> p; first point (x=0, y=0) and clockwise around
  */
 public static float biLinearIntrp(float gy, float gx, float[][] corners, float[] values) {
    // transform physical space (gy, gx) to unit square (q, p)
    // bilinear mapping coefficients
/*
    float a = corners[0][3];
    float b = corners[0][1];
    float c = (corners[0][2] - corners[0][3] - corners[0][1]);

    float d = corners[1][3];
    float e = corners[1][1];
    float f = (corners[1][2] - corners[1][3] - corners[1][1]);
*/

    float a = corners[0][1];
    float b = corners[0][3];
    float c = (corners[0][2] - corners[0][1] - corners[0][3]);

    float d = corners[1][1];
    float e = corners[1][3];
    float f = (corners[1][2] - corners[1][1] - corners[1][3]);


    // quadratic terms to determine p
    // p = (-coef1 +/- sqrt(coef1^2 - 4*coef2*coef0))/2*coef2

    float coef2 = (c*d - a*f);  // A 
    float coef1 = (-c*gy + b*d + gx*f - a*e);  // B
    float coef0 = (-gy*b + gx*e);  // C

    // two p vals from quadratic:
    float p0 =  (-coef1 + ((float)Math.sqrt(coef1*coef1 - 4*coef2*coef0)) )/2f*coef2;
    float p1 =  (-coef1 - ((float)Math.sqrt(coef1*coef1 - 4*coef2*coef0)) )/2f*coef2;

    // the corresponding q values for both p solutions:
    float q0 = (gx - a*p0)/(b + c*p0);
    float q1 = (gx - a*p1)/(b + c*p1);

    // test  which point to use. One will be outside unit square:
    float p = Float.NaN;
    float q = Float.NaN;
    if ((p0 >= 0f && p0 <= 1f) && (q0 >= 0f && q0 <= 1f)) {
       p = p0;
       q = q0;
    }
    else if ((p1 >= 0f && p1 <= 1f) && (q1 >= 0f && q1 <= 1f)) {
       p = p1;
       q = q1;
    }

    // bilinear interpolation within the unit square:

    float intrp = values[0]*(1f-p)*(1f-q) + values[1]*(1f-p)*q + values[2]*p*q + values[3]*p*(1f-q);

    return intrp;
 }

 private static float nearest(float gy, float gx, float[][] corners, float[] values) {
   float minDist = Float.MAX_VALUE;

   float delx;
   float dely;
   int closest = 0;
   for (int k=0; k<4; k++) {
      delx = corners[0][k] - gx;
      dely = corners[1][k] - gy;
      float dst_sqrd = delx*delx + dely*dely;

      if (dst_sqrd < minDist) {
         minDist = dst_sqrd;
         closest = k;
      }
   }

   return values[closest];
 }
 public static float visad2D(float gy, float gx, float[][] corners, float[] values) {
    boolean Pos = true;

    // A:0, B:1, C:2, D:3
    float v0x = corners[0][0];
    float v0y = corners[1][0];
    float v1x = corners[0][1];
    float v1y = corners[1][1];
    float v2x = corners[0][2];
    float v2y = corners[1][2];
    float v3x = corners[0][3];
    float v3y = corners[1][3];

    float bdx = v2x-v1x;
    float bdy = v2y-v1y;

    float bpx = gx-v1x;
    float bpy = gy-v1y;

    float dpx = gx-v2x;
    float dpy = gy-v2y;

    // lower triangle first

    boolean insideTri = true;

    float abx = v1x-v0x;
    float aby = v1y-v0y;
    float dax = v0x-v2x;
    float day = v0y-v2y;
    float apx = gx-v0x;
    float apy = gy-v0y;
    
    float tval1 = abx*apy-aby*apx;
    float tval2 = bdx*bpy-bdy*bpx;
    float tval3 = dax*dpy-day*dpx;
    boolean test1 = (tval1 == 0) || ((tval1 > 0) == Pos);
    boolean test2 = (tval2 == 0) || ((tval2 > 0) == Pos);
    boolean test3 = (tval3 == 0) || ((tval3 > 0) == Pos);

    if (!test1 && !test2) {      // Go UP & RIGHT
       insideTri = false;
    }
    else if (!test2 && !test3) { // Go DOWN & LEFT
       insideTri = false;
    }
    else if (!test1 && !test3) { // Go UP & LEFT
       insideTri = false;
    }
    else if (!test1) {           // Go UP
       insideTri = false;
    }
    else if (!test3) {           // Go LEFT
       insideTri = false;
    }

    insideTri = (insideTri && test2);
 
    float gxx = Float.NaN;
    float gyy = Float.NaN;

    if (insideTri) {
       gxx = ((gx-v0x)*(v2y-v0y)
                        + (v0y-gy)*(v2x-v0x))
                       / ((v1x-v0x)*(v2y-v0y)
                        + (v0y-v1y)*(v2x-v0x));

       gyy =  ((gx-v0x)*(v1y-v0y)
                        + (v0y-gy)*(v1x-v0x))
                       / ((v2x-v0x)*(v1y-v0y)
                        + (v0y-v2y)*(v1x-v0x)); 
    }
    else {
       insideTri = true;

       float bcx = v3x-v1x;
       float bcy = v3y-v1y;
       float cdx = v2x-v3x;
       float cdy = v2y-v3y;
       float cpx = gx-v3x;
       float cpy = gy-v3y;

       tval1 = bcx*bpy-bcy*bpx;
       tval2 = cdx*cpy-cdy*cpx;
       tval3 = bdx*dpy-bdy*dpx;
       test1 = (tval1 == 0) || ((tval1 > 0) == Pos);
       test2 = (tval2 == 0) || ((tval2 > 0) == Pos);
       test3 = (tval3 == 0) || ((tval3 < 0) == Pos);
       if (!test1 && !test3) {      // Go UP & RIGHT
          insideTri = false;
       }
       else if (!test2 && !test3) { // Go DOWN & LEFT
          insideTri = false;
       }
       else if (!test1 && !test2) { // Go DOWN & RIGHT
          insideTri = false;
       }
       else if (!test1) {           // Go RIGHT
          insideTri = false;
       }
       else if (!test2) {           // Go DOWN
          insideTri = false;
       }
       insideTri = (insideTri && test3);

       if (insideTri) {
            // Found correct grid triangle
            // Solve the point with the reverse interpolation
            gxx = ((v3x-gx)*(v1y-v3y)
                        + (gy-v3y)*(v1x-v3x))
                       / ((v2x-v3x)*(v1y-v3y)
                        - (v2y-v3y)*(v1x-v3x)) + 1;
            gyy = ((v2y-v3y)*(v3x-gx)
                        + (v2x-v3x)*(gy-v3y))
                       / ((v1x-v3x)*(v2y-v3y)
                        - (v2x-v3x)*(v1y-v3y)) + 1;
       }

    }

    // bilinear interpolation within the unit square:

    float intrp = values[0]*(1f-gxx)*(1f-gyy) + values[2]*(1f-gxx)*gyy + values[3]*gxx*gyy + values[1]*gxx*(1f-gyy);


    return intrp;
 }

}
