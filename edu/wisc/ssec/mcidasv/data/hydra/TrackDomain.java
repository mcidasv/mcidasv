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

package edu.wisc.ssec.mcidasv.data.hydra;

import java.rmi.RemoteException;
import java.util.HashMap;

import visad.FlatField;
import visad.FunctionType;
import visad.Gridded3DSet;
import visad.Gridded2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.VisADException;
import visad.Set;


public class TrackDomain extends MultiDimensionAdapter {
   RealTupleType domainType;
   ArrayAdapter lonAdapter;
   ArrayAdapter latAdapter;
   ArrayAdapter altAdapter;

   float[] trackLongitude;
   float[] trackLatitude;
   float[] trackAltitude;

   int TrackLen;

   public TrackDomain() {
   }

   public TrackDomain(ArrayAdapter lonAdapter, ArrayAdapter latAdapter) throws Exception {
     this(lonAdapter, latAdapter, null);
   }

   public TrackDomain(ArrayAdapter lonAdapter, ArrayAdapter latAdapter, ArrayAdapter altAdapter) throws Exception {
     this.lonAdapter = lonAdapter;
     this.latAdapter = latAdapter;
     this.altAdapter = altAdapter;

     if (altAdapter != null) {
       domainType = RealTupleType.SpatialEarth3DTuple;
       trackAltitude = (altAdapter.getData(altAdapter.getDefaultSubset()).getFloats(false))[0];
     }
     else {
       domainType = RealTupleType.SpatialEarth2DTuple;
     }

     trackLongitude = (lonAdapter.getData(lonAdapter.getDefaultSubset()).getFloats(false))[0];
     trackLatitude = (latAdapter.getData(latAdapter.getDefaultSubset()).getFloats(false))[0];
     TrackLen = trackLongitude.length;
   }

   public Set makeDomain(Object subset) throws VisADException, RemoteException {
     
     float[] lonValues = null;
     float[] latValues = null;
     float[] altValues = null;

     double[] coords = (double[]) ((HashMap)subset).get("TrackDim");
     HashMap newSubset = this.getDefaultSubset();
     double[] newCoords = (double[])newSubset.get("TrackDim");
     System.arraycopy(coords, 0, newCoords, 0, coords.length);
     subset = newSubset;

     try {
       lonValues = (lonAdapter.getData(subset).getFloats())[0];
       latValues = (latAdapter.getData(subset).getFloats())[0];

       if (altAdapter != null) {
         altValues = (altAdapter.getData(subset).getFloats())[0];
       }
     }
     catch (Exception e) {
       e.printStackTrace();
       return null;
     }

     Set set = null;

     if (altAdapter != null) {
       for (int k=0; k< altValues.length; k++) {
         altValues[k] *= 1000.0;
       }
       set = new Gridded3DSet(domainType, new float[][] {lonValues, latValues, altValues}, lonValues.length);
     }
     else {
       set = new Gridded2DSet(domainType, new float[][] {lonValues, latValues}, lonValues.length);
     }
     return set;
   }

   public float[] getTrackLongitude() {
     return trackLongitude;
   }

   public float[] getTrackLatitude() {
     return trackLatitude;
   }

   public float[] getTrackAlitude() {
     return trackAltitude;
   }

   public int[] getTrackRangeInsideLonLatRect(double minLat, double maxLat, double minLon, double maxLon) {
        int nn = 100;
        int skip = TrackLen/nn;
        double lon;
        double lat;

        int idx = 0;
        while (idx < TrackLen) {
          lon = (double)trackLongitude[idx];
          lat = (double)trackLatitude[idx];
          if (((lon > minLon) && (lon < maxLon)) && ((lat > minLat)&&(lat < maxLat))) break;
          idx += skip;
        }
        if (idx > TrackLen-1) idx = TrackLen-1;
        if (idx == TrackLen-1) return new int[] {-1,-1};

        int low_idx = idx;
        while (low_idx > 0) {
          lon = (double)trackLongitude[low_idx];
          lat = (double)trackLatitude[low_idx];
          if (((lon > minLon) && (lon < maxLon)) && ((lat > minLat)&&(lat < maxLat))) {
            low_idx -= 1;
            continue;
          }
          else {
            break;
          }
        }

        int hi_idx = idx;
        while (hi_idx < TrackLen-1) {
          lon = (double)trackLongitude[hi_idx];
          lat = (double)trackLatitude[hi_idx];
          if (((lon > minLon) && (lon < maxLon)) && ((lat > minLat)&&(lat < maxLat))) {
            hi_idx += 1;
            continue;
          }
          else {
            break;
          }
        }
        return new int[] {low_idx, hi_idx};
   }

   public HashMap getSubsetFromLonLatRect(HashMap subset, double minLat, double maxLat, double minLon, double maxLon) {
      double[] coords = (double[])subset.get("TrackDim");
      int[] idxs = getTrackRangeInsideLonLatRect(minLat, maxLat, minLon, maxLon);
      coords[0] = (double) idxs[0];
      coords[1] = (double) idxs[1];
      if ((coords[0] == -1) || (coords[1] == -1)) return null;
      return subset;
   }

   public HashMap getSubsetFromLonLatRect(HashMap subset, double minLat, double maxLat, double minLon, double maxLon,
                                          int xStride, int yStride, int zStride) {
      double[] coords = (double[])subset.get("TrackDim");
      int[] idxs = getTrackRangeInsideLonLatRect(minLat, maxLat, minLon, maxLon);
      coords[0] = (double) idxs[0];
      coords[1] = (double) idxs[1];
      if ((coords[0] == -1) || (coords[1] == -1)) return null;

      if (xStride > 0) {
         coords[2] = xStride;
      }

      return subset;
   }

   public HashMap getDefaultSubset() {
     return lonAdapter.getDefaultSubset();
   }
}
