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

package edu.wisc.ssec.adapter;

import java.util.HashMap;

import visad.FlatField;
import visad.Set;

public class IASI_L1C_AAPP_SwathAdapter extends SwathAdapter {

   public IASI_L1C_AAPP_SwathAdapter(MultiDimensionReader reader, HashMap metadata) {
     super(reader, metadata);

     RangeProcessor rangeProcessor = null;
     try {
         rangeProcessor = new IASI_L1C_AAPP_RangeProcessor(null);

     } catch (Exception e) {
        e.printStackTrace();
     }
     setRangeProcessor(rangeProcessor);
   }

   protected void setLengths() { // define the swath dimensions
     // define the 2D swath dimensions transforming from native (scan, EFOV, 9 IFOVs) -> (scan*2, EFOV*2) 
     int len = getTrackLength();
     setTrackLength(len *= 2);
     len = getXTrackLength();
     setXTrackLength(len *= 2);
   }

   public FlatField getData(Object subset) throws Exception {

     HashMap new_subset = (HashMap) ((HashMap)subset).clone();
     new_subset.putAll((HashMap)subset);

     // reform subset to integral numbers of EFOV (FORs)
     // you may not get exactly what you ask for in this case.
     // Keep the spatial coordinates in the 2D coords, but carry along the IFOV indexes.

     double[] coords = (double[]) new_subset.get(SwathAdapter.track_name);
     double[] new_coords = new double[] {2.0*Math.floor((coords[0])/2), 2.0*Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.track_name, new_coords);

     coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
     new_coords = new double[] {2.0*Math.floor((coords[0])/2), 2.0*Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_coords);

     new_coords = new double[] {0.0, (4.0 - 1.0), 1.0};
     new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

     Set domainSet = makeDomain(new_subset);


     // transfrom the coordinates from the 2D form back to the (scan, EFOV, 4 IFOVs) form
     // which is the native storage order.  Needed for makeFlatField which accesses readArray directly.

     new_subset = (HashMap) ((HashMap)subset).clone();
     new_subset.putAll((HashMap)subset);

     coords = (double[]) new_subset.get(SwathAdapter.track_name);
     new_coords = new double[] {Math.floor(coords[0]/2), Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.track_name, new_coords);

     coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
     new_coords = new double[] {Math.floor(coords[0]/2), Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_coords);

     new_coords = new double[] {0.0, (4.0 - 1.0), 1.0};
     new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

     FlatField swath = makeFlatField(domainSet, new_subset);

     return swath;
   }
 
   public FlatField makeConvolvedRadiances(HashMap subset, float[] wghts) throws Exception {

     HashMap new_subset = (HashMap) ((HashMap)subset).clone();
     new_subset.putAll((HashMap)subset);

     // reform subset to integral numbers of EFOV (FORs)
     // you may not get exactly what you ask for in this case.
     // Keep the spatial coordinates in the 2D coords, but carry along the IFOV indexes.

     double[] coords = (double[]) new_subset.get(SwathAdapter.track_name);
     double[] new_coords = new double[] {2.0*Math.floor((coords[0])/2), 2.0*Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.track_name, new_coords);

     coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
     new_coords = new double[] {2.0*Math.floor((coords[0])/2), 2.0*Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_coords);

     new_coords = new double[] {0.0, (4.0 - 1.0), 1.0};
     new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

     Set domainSet = makeDomain(new_subset);


     // transfrom the coordinates from the 2D form back to the (scan, EFOV, 4 IFOVs) form
     // which is the native storage order.  Needed for makeFlatField which accesses readArray directly.

     new_subset = (HashMap) ((HashMap)subset).clone();
     new_subset.putAll((HashMap)subset);

     coords = (double[]) new_subset.get(SwathAdapter.track_name);
     new_coords = new double[] {Math.floor(coords[0]/2), Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.track_name, new_coords);
     int YLen = (int) (new_coords[1] - new_coords[0]) + 1;

     coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
     new_coords = new double[] {Math.floor(coords[0]/2), Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_coords);
     int XLen = (int) (new_coords[1] - new_coords[0]) + 1;

     new_coords = new double[] {0.0, (4.0 - 1.0), 1.0};
     new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

     int nFOV = 4;
     double[] chans = (double[]) new_subset.get(SpectrumAdapter.channelIndex_name);
     int c_idx_start = (int) chans[0];
     int numChans = ((int)chans[1] - (int)chans[0]) + 1;
     float[][] radiances = new float[numChans][YLen*XLen*nFOV];
     for (int c_idx = 0; c_idx < numChans; c_idx++) {

         new_subset.put(SpectrumAdapter.channelIndex_name,
                 new double[] {(double)c_idx_start + c_idx, (double)c_idx_start + c_idx, 1.0});

         float[] f_array = (float[]) readArray(new_subset);
         radiances[c_idx] = f_array;
     }

     float[] convldRads = new float[YLen*XLen*nFOV];
     for (int j = 0; j < YLen; j++) {
        for (int i = 0; i < XLen; i++) {
           for (int f=0; f<nFOV; f++) {

              int idx = j*XLen*nFOV+i*nFOV+f;

              float val = 0;
              for (int k=0; k<numChans; k++) {
                 val += wghts[k]*radiances[k][idx];
              }
              convldRads[idx] = val;
           }                          
        }       
     }

     convldRads = IASI_L1C_Utility.psuedoScanReorder2(convldRads, XLen*2, YLen*2);

     FlatField swath = makeFlatField(domainSet, new float[][] {convldRads});

     return swath;
        
   }
   
}
