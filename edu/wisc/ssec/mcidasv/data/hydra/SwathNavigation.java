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
    return new SwathNavigation(swathAdapter);
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
  float ratio;
  Class type;

  public SwathNavigation(SwathAdapter swathAdapter) throws Exception {

    HashMap metadata = (HashMap) swathAdapter.getMetadata();
    reader = swathAdapter.getReader();
    this.swathAdapter = swathAdapter;

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

    geoTrackLen = lon_dim_lengths[geo_track_idx];
    geoXTrackLen = lon_dim_lengths[geo_xtrack_idx];

    ratio = swathAdapter.getTrackLength()/geoTrackLen;
    type = reader.getArrayType(lon_array_name);
  }

  public CoordinateSystem getVisADCoordinateSystem(Linear2DSet domainSet, Object domainSubset) throws Exception
  {
      Subset select = swathAdapter.getIndexes((HashMap)domainSubset);
      int[] start = select.getStart();
      int[] count = select.getCount();
      int[] stride = select.getStride();

      int[] geo_stride = new int[2];
      int[] geo_count = new int[2];

      if (ratio/(float)stride[0] <= 1) {
        geo_stride[0] = (int) (1.0/(ratio/((float)stride[0])));
        geo_stride[1] = (int) (1.0/(ratio/((float)stride[1]))); 
        geo_count[0] = (int) count[0];
        geo_count[1] = (int) count[1];
      }
      else {
        geo_stride[0] = 1;
        geo_stride[1] = 1;
        geo_count[0] = (int) ((count[0])/(ratio/((float)stride[0])));
        geo_count[1] = (int) ((count[1])/(ratio/((float)stride[1])));
      }

      int[] geo_start = new int[] {(int) (((float)start[0])/ratio), (int) (((float)start[1])/ratio)};

      Gridded2DSet gset = null;
      if (type == Float.TYPE) {
        float[] lonValues = reader.getFloatArray(lon_array_name, geo_start, geo_count, geo_stride);
        float[] latValues = reader.getFloatArray(lat_array_name, geo_start, geo_count, geo_stride);

        gset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {lonValues, latValues}, geo_count[idx_order[0]], geo_count[idx_order[1]], null, null, null, false, false);
      }
      else if (type == Double.TYPE) {
        double[] lonValues = reader.getDoubleArray(lon_array_name, geo_start, geo_count, geo_stride);
        double[] latValues = reader.getDoubleArray(lat_array_name, geo_start, geo_count, geo_stride);
                                                                                                                                     
        gset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, Set.doubleToFloat(new double[][] {lonValues, latValues}), geo_count[idx_order[0]], geo_count[idx_order[1]], null, null, null, false, false);
      }

      CoordinateSystem cs = new LongitudeLatitudeCoordinateSystem(domainSet, gset);

      return cs;
  }

}
