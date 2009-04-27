/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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

import visad.Set;
import visad.Gridded2DSet;
import visad.Gridded2DDoubleSet;
import visad.Linear2DSet;
import visad.CoordinateSystem;
import visad.GridCoordinateSystem;
import visad.RealTupleType;
import java.util.HashMap;

public class SwathNavigation implements Navigation  {

  public static SwathNavigation createNavigation(SwathAdapter swathAdapter) throws Exception {
    String product_name = null;
    SwathNavigation swathNav = null;
    
    product_name = (String) ((HashMap)swathAdapter.getMetadata()).get(SwathAdapter.product_name);

    if (product_name == null) {
      swathNav = new SwathNavigation(swathAdapter);
    }
    else if (product_name == "IASI_L1C_xxx") {
      swathNav = new IASI_L1C_LonLatNavigation(swathAdapter);
    }
    
    return swathNav;
  }

  int geo_track_idx;
  int geo_xtrack_idx;
  int geoTrackLen;
  int geoXTrackLen;

  SwathAdapter swathAdapter;
  MultiDimensionReader reader;
  String lon_array_name;
  String lat_array_name;
  int[] idx_order = new int[2];
  float ratio = 1;
  float track_ratio = 1;
  float xtrack_ratio = 1;
  double track_offset = 0;
  double xtrack_offset = 0;
  int track_idx;
  int xtrack_idx;
  int[] geo_stride = new int[2];
  int[] geo_count = new int[2];
  int[] geo_start = new int[2];
  int[] geo_end = new int[2];

  Class type;

  public SwathNavigation(SwathAdapter swathAdapter) throws Exception {

    HashMap metadata = (HashMap) swathAdapter.getMetadata();
    reader = swathAdapter.getReader();
    this.swathAdapter = swathAdapter;
    track_idx = swathAdapter.track_idx;
    xtrack_idx = swathAdapter.xtrack_idx;

    lon_array_name = (String)metadata.get(SwathAdapter.lon_array_name);
    lat_array_name = (String)metadata.get(SwathAdapter.lat_array_name);

    String[] lon_dim_names = reader.getDimensionNames(lon_array_name);
    String[] lat_dim_names = reader.getDimensionNames(lat_array_name);
    int[] lon_dim_lengths = reader.getDimensionLengths(lon_array_name);
    int[] lat_dim_lengths = reader.getDimensionLengths(lat_array_name);

    String geo_track_name = (String) metadata.get(SwathAdapter.geo_track_name);
    String geo_xtrack_name = (String) metadata.get(SwathAdapter.geo_xtrack_name);

    for (int k=0; k<2;k++) {
      if ( geo_track_name.equals(lon_dim_names[k]) ) {
         geo_track_idx = k;
      }
      if ( geo_xtrack_name.equals(lon_dim_names[k]) ) {
         geo_xtrack_idx = k;
      }
    }

    if (geo_track_idx < geo_xtrack_idx) {
      idx_order[0] = geo_xtrack_idx;
      idx_order[1] = geo_track_idx;
    }
    else {
      idx_order[0] = geo_track_idx;
      idx_order[1] = geo_xtrack_idx;
    }

    geoTrackLen  = lon_dim_lengths[geo_track_idx];
    geoXTrackLen = lon_dim_lengths[geo_xtrack_idx];

    String str = (String) metadata.get(SwathAdapter.geo_track_skip_name);

    if (str != null) {
      track_ratio = (float) Double.parseDouble(str);
      ratio = track_ratio;
    }
    str = (String) metadata.get(SwathAdapter.geo_xtrack_skip_name);
    if (str != null) {
      xtrack_ratio = (float) Double.parseDouble(str);
    }
    str = (String) metadata.get(SwathAdapter.geo_track_offset_name);
    if (str != null) {
      track_offset = Double.parseDouble(str);
    }
    str = (String) metadata.get(SwathAdapter.geo_xtrack_offset_name);
    if (str != null) {
      xtrack_offset = Double.parseDouble(str);
    }
 
    type = reader.getArrayType(lon_array_name);
  }

  public CoordinateSystem getVisADCoordinateSystem(Linear2DSet domainSet, Object domainSubset) throws Exception
  {
      Subset select = swathAdapter.getIndexes((HashMap)domainSubset);

      double[] track_coords = (double[]) ((HashMap)domainSubset).get(SwathAdapter.track_name);
      double[] xtrack_coords = (double[]) ((HashMap)domainSubset).get(SwathAdapter.xtrack_name);
      
      int[] start = new int[2];
      int[] count = new int[2];
      int[] stride = new int[2];

      stride[geo_track_idx] = (int) track_coords[2];
      stride[geo_xtrack_idx] = (int) xtrack_coords[2]; 


      if (ratio/(float)stride[0] <= 1) {
        geo_stride[geo_track_idx] = Math.round((1f/(track_ratio/((float)stride[0]))));
        geo_stride[geo_xtrack_idx] = Math.round((1f/(xtrack_ratio/((float)stride[1]))));
      }
      else {
        geo_stride[0] = 1;
        geo_stride[1] = 1;
      }

      int geo_track_start  = (int) Math.ceil((track_coords[0] - track_offset)/track_ratio);
      int geo_xtrack_start = (int) Math.ceil((xtrack_coords[0] - xtrack_offset)/xtrack_ratio);

      int geo_track_end  = (int) ((track_coords[1] - track_offset)/((double)track_ratio));
      int geo_xtrack_end = (int) ((xtrack_coords[1] - xtrack_offset)/((double)xtrack_ratio));

      geo_count[geo_track_idx]  = (int) ((geo_track_end - geo_track_start)/geo_stride[geo_track_idx]) + 1;
      geo_count[geo_xtrack_idx] = (int) ((geo_xtrack_end - geo_xtrack_start)/geo_stride[geo_xtrack_idx]) + 1;

      geo_track_end = geo_track_start + (geo_count[geo_track_idx]-1)*geo_stride[geo_track_idx];
      geo_xtrack_end = geo_xtrack_start + (geo_count[geo_xtrack_idx]-1)*geo_stride[geo_xtrack_idx];
     
      geo_start[geo_track_idx]  = geo_track_start;
      geo_start[geo_xtrack_idx] = geo_xtrack_start;

      //-- convert back track/xtrack coords:
      int new_track_start  = (int) (geo_track_start*track_ratio + (float)track_offset);
      int new_xtrack_start = (int) (geo_xtrack_start*xtrack_ratio + (float)xtrack_offset);
      int new_track_end  = (int) (geo_track_end*track_ratio + (float)track_offset);
      int new_xtrack_end = (int) (geo_xtrack_end*xtrack_ratio + (float)xtrack_offset);


      double[] first = new double[2];
      double[] last  = new double[2];
      int[] length   = new int[2];

      first[1-geo_track_idx]  = new_track_start;
      first[1-geo_xtrack_idx] = new_xtrack_start;
      last[1-geo_track_idx]   = new_track_end;
      last[1-geo_xtrack_idx]  = new_xtrack_end;
      length[1-geo_track_idx]  = (int) ((last[1-geo_track_idx] - first[1-geo_track_idx])/stride[geo_track_idx] + 1);
      length[1-geo_xtrack_idx] = (int) ((last[1-geo_xtrack_idx] - first[1-geo_xtrack_idx])/stride[geo_xtrack_idx] + 1);

      domainSet = new Linear2DSet(first[0], last[0], length[0], first[1], last[1], length[1]);
   
      Gridded2DSet gset = null;

      gset = createInterpSet();

      CoordinateSystem cs = new LongitudeLatitudeCoordinateSystem(domainSet, gset);

      return cs;
  }

  Gridded2DSet createInterpSet() throws Exception {
    Gridded2DSet gset = null;
    if (type == Float.TYPE) {
      float[] lonValues = reader.getFloatArray(lon_array_name, geo_start, geo_count, geo_stride);
      float[] latValues = reader.getFloatArray(lat_array_name, geo_start, geo_count, geo_stride);
                                                                                                                                             
      gset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
                     new float[][] {lonValues, latValues},
                         geo_count[idx_order[0]], geo_count[idx_order[1]],
                            null, null, null, false, false);
    }
    else if (type == Double.TYPE) {
      double[] lonValues = reader.getDoubleArray(lon_array_name, geo_start, geo_count, geo_stride);
      double[] latValues = reader.getDoubleArray(lat_array_name, geo_start, geo_count, geo_stride);
                                                                                                                                             
      gset = new Gridded2DDoubleSet(RealTupleType.SpatialEarth2DTuple,
                    new double[][] {lonValues, latValues},
                       geo_count[idx_order[0]], geo_count[idx_order[1]],
                           null, null, null, false);
    }
    return gset;
  }



  public static Linear2DSet getNavigationDomain(double data_x_start, double data_x_count, double data_x_stride,
                                         double data_y_start, double data_y_count, double data_y_stride,
                                         double ratio_x, double ratio_y,
                                         double offset_x, double offset_y,
                                         int[] geo_start, int[] geo_count, int[] geo_stride)
      throws Exception {

      int geo_track_idx = 1;
      int geo_xtrack_idx = 0;
      double track_ratio = ratio_y;
      double xtrack_ratio = ratio_x;
      double track_offset = offset_y;
      double xtrack_offset = offset_x;
 
      double[] track_coords = new double[3];
      double[] xtrack_coords = new double[3];

      double[] stride =  new double[2];
      stride[geo_track_idx] = data_y_stride;
      stride[geo_xtrack_idx] = data_x_stride;

      if (track_ratio/(float)stride[0] <= 1) {
        geo_stride[geo_track_idx] = (int) Math.round((1/(track_ratio/(stride[1]))));
        geo_stride[geo_xtrack_idx] = (int) Math.round((1/(xtrack_ratio/(stride[0]))));
      }
      else {
        geo_stride[0] = 1;
        geo_stride[1] = 1;
      }

      int geo_track_start  = (int) Math.ceil((track_coords[0] - track_offset)/track_ratio);
      int geo_xtrack_start = (int) Math.ceil((xtrack_coords[0] - xtrack_offset)/xtrack_ratio);

      int geo_track_end  = (int) ((track_coords[1] - track_offset)/((double)track_ratio));
      int geo_xtrack_end = (int) ((xtrack_coords[1] - xtrack_offset)/((double)xtrack_ratio));

      geo_count[geo_track_idx]  = (int) ((geo_track_end - geo_track_start)/geo_stride[geo_track_idx]) + 1;
      geo_count[geo_xtrack_idx] = (int) ((geo_xtrack_end - geo_xtrack_start)/geo_stride[geo_xtrack_idx]) + 1;

      geo_track_end = geo_track_start + (geo_count[geo_track_idx]-1)*geo_stride[geo_track_idx];
      geo_xtrack_end = geo_xtrack_start + (geo_count[geo_xtrack_idx]-1)*geo_stride[geo_xtrack_idx];

      geo_start[geo_track_idx]  = geo_track_start;
      geo_start[geo_xtrack_idx] = geo_xtrack_start;

      //-- convert back track/xtrack coords:
      int new_track_start  = (int) (geo_track_start*track_ratio + (float)track_offset);
      int new_xtrack_start = (int) (geo_xtrack_start*xtrack_ratio + (float)xtrack_offset);
      int new_track_end  = (int) (geo_track_end*track_ratio + (float)track_offset);
      int new_xtrack_end = (int) (geo_xtrack_end*xtrack_ratio + (float)xtrack_offset);


      double[] first = new double[2];
      double[] last  = new double[2];
      int[] length   = new int[2];
      first[geo_track_idx]  = new_track_start;
      first[geo_xtrack_idx] = new_xtrack_start;
      last[geo_track_idx]   = new_track_end;
      last[geo_xtrack_idx]  = new_xtrack_end;
      length[geo_track_idx]  = (int) ((last[geo_track_idx] - first[geo_track_idx])/stride[geo_track_idx] + 1);
      length[geo_xtrack_idx] = (int) ((last[geo_xtrack_idx] - first[geo_xtrack_idx])/stride[geo_xtrack_idx] + 1);

      return new Linear2DSet(first[0], last[0], length[0], first[1], last[1], length[1]);

  }


}
