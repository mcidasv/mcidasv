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

package edu.wisc.ssec.mcidasv.data.hydra;
import java.util.HashMap;
import visad.Set;
import visad.RealTupleType;
import visad.RealType;
import visad.Gridded3DSet;


public class ProfileAlongTrack3D extends MultiDimensionAdapter {

  public ProfileAlongTrack adapter2D;
  MultiDimensionReader reader;
  float[] vertLocs;
  RealTupleType domain3D;


  public ProfileAlongTrack3D(ProfileAlongTrack adapter2D) {
    super(adapter2D.getReader(), (HashMap) adapter2D.getMetadata());
    this.adapter2D = adapter2D;
    this.reader = adapter2D.getReader();
    try {
      init();
    } catch (Exception e) {
      System.out.println("init failed");
    }
  }

  void init() throws Exception {
    vertLocs = adapter2D.getVertBinAltitude();
    domain3D = RealTupleType.SpatialEarth3DTuple;
    rangeType = adapter2D.getRangeType();
  }


  public Set makeDomain(Object subset) throws Exception {
    double[] vert_coords = (double[]) ((HashMap)subset).get(ProfileAlongTrack.vertDim_name);
    double[] track_coords = (double[])  ((HashMap)subset).get(ProfileAlongTrack.trackDim_name);

    float[] lonValues = null;
    float[] latValues = null;

    int[] start = new int[2];
    int[] count = new int[2];
    int[] stride = new int[2];

    int vert_idx = adapter2D.getVertIdx();
    int track_idx = adapter2D.getTrackIdx();

    start[vert_idx] = (int) 0;
    count[vert_idx] = (int) 1;
    stride[vert_idx] = (int) vert_coords[2];

    start[track_idx] = (int) track_coords[0];
    count[track_idx] = (int) ((track_coords[1] - track_coords[0])/track_coords[2] + 1f);
    stride[track_idx] = (int) track_coords[2];

    int vert_len = (int) ((vert_coords[1] - vert_coords[0])/vert_coords[2] + 1f);
    int track_len = count[track_idx];
    
    if (reader.getArrayType((String) ((HashMap)getMetadata()).get(ProfileAlongTrack.longitude_name)) == Float.TYPE ) {
      lonValues = reader.getFloatArray(ProfileAlongTrack.longitude_name, start, count, stride);
      latValues = reader.getFloatArray(ProfileAlongTrack.latitude_name, start, count, stride);
    }

    float[] altitudes = new float[vert_len];
    for (int k=0; k<vert_len;k++) {
      altitudes[k] = vertLocs[(int)vert_coords[0] + k*((int)vert_coords[2])];
    }

    float[][] alt_lon_lat = new float[3][vert_len*track_len];
    oneD_threeDfill(lonValues, latValues, track_len, altitudes, vert_len, alt_lon_lat);

    return new Gridded3DSet(domain3D, alt_lon_lat, vert_len, track_len);
  }
  

  public HashMap getDefaultSubset() {
    return adapter2D.getDefaultSubset();
  }

  public HashMap getSubsetFromLonLatRect(HashMap subset, double minLat, double maxLat, double minLon, double maxLon) {
    return adapter2D.getSubsetFromLonLatRect(subset, minLat, maxLat, minLon, maxLon);
  }

  public HashMap getSubsetFromLonLatRect(double minLat, double maxLat, double minLon, double maxLon) {
    return adapter2D.getSubsetFromLonLatRect(minLat, maxLat, minLon, maxLon);
  }

  public HashMap getSubsetFromLonLatRect(double minLat, double maxLat, double minLon, double maxLon, int xStride, int yStride, int zStride) {
    return adapter2D.getSubsetFromLonLatRect(minLat, maxLat, minLon, maxLon, xStride, yStride, zStride);
  }

  public static void oneD_threeDfill(float[] b, float[] c, int leny, float[] a, int lenx, float[][] abc) {
    int cnt = 0;
    for (int i=0; i<leny; i++) {
      for (int j=0; j<lenx; j++) {
        abc[0][cnt] = b[i];
        abc[1][cnt] = c[i];
        abc[2][cnt] = a[j];
        cnt++;
       }
     }
   }

}
