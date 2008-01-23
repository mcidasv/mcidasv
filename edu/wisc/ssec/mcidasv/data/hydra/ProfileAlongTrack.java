package edu.wisc.ssec.mcidasv.data.hydra;

import visad.Data;
import visad.FlatField;
import visad.Set;
import visad.Gridded1DSet;
import visad.CoordinateSystem;
import visad.RealType;
import visad.RealTupleType;
import visad.SetType;
import visad.Linear1DSet;
import visad.Linear2DSet;
import visad.Unit;
import visad.FunctionType;
import visad.VisADException;
import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.HashMap;
import java.util.StringTokenizer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public abstract class ProfileAlongTrack extends MultiDimensionAdapter {

      private FunctionType mathtype;

      int TrackLen;
      int VertLen;

      private float[] vertLocs = null;
      private float[] trackTimes = null;

      public static String longitude_name = "Longitude";
      public static String latitude_name  = "Latitude";
      public static String trackDim_name  = "TrackDim";
      public static String vertDim_name  = "VertDim";
      public static String array_name = "array_name";
      public static String profileTime_name = "ProfileTime";
      public static String profileTime_unit = "ProfileTime_unit";
      public static String altitude_unit = "altitude_unit";
      public static String sfcElev_name = "SurfaceElev";
      public static String range_name = "range_name";
      public static String scale_name = "scale_name";
      public static String offset_name = "offset_name";
      public static String fill_value_name = "fill_value_name";
      public static String ancillary_file_name = "ancillary_file";
      
      String[] rangeName_s  = null;
      Class[] arrayType_s = null;
      Unit[] rangeUnit_s  = new Unit[] {null};

      RealType track  = RealType.getRealType(trackDim_name);
      RealType vert = RealType.getRealType(vertDim_name);
      RealType[] domainRealTypes = new RealType[2];

      RealType vertLocType;
      RealType trackTimeType;

      int track_idx      = -1;
      int vert_idx       = -1;
      int range_rank     = -1;

      int track_tup_idx;
      int vert_tup_idx;

      //-private RangeProcessor rangeProcessor;

      private Linear2DSet swathDomain;

      CoordinateSystem cs = null;

      public static HashMap getEmptySubset() {
        HashMap<String, double[]> subset = new HashMap<String, double[]>();
        subset.put(trackDim_name, new double[3]);
        subset.put(vertDim_name, new double[3]);
        return subset;
      }

      public static HashMap getEmptyMetadataTable() {
        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put(array_name, null);
        metadata.put(trackDim_name, null);
        metadata.put(vertDim_name, null);
        metadata.put(longitude_name, null);
        metadata.put(latitude_name, null);
        metadata.put(profileTime_name, null);
        metadata.put(profileTime_unit, null);
        metadata.put(altitude_unit, null);
        metadata.put(sfcElev_name, null);
        metadata.put(scale_name, null);
        metadata.put(offset_name, null);
        metadata.put(fill_value_name, null);
        /*
        metadata.put(range_name, null);
        metadata.put(range_unit, null);
        metadata.put(valid_range, null);
        */
        return metadata;
      }

      public ProfileAlongTrack() {

      }

      public ProfileAlongTrack(MultiDimensionReader reader, HashMap metadata) {
        super(reader, metadata);
        this.init();
      }


      private void init() {
        for (int k=0; k<array_rank;k++) {
          if ( ((String)metadata.get(trackDim_name)).equals(array_dim_names[k]) ) {
            track_idx = k;
          }
          if ( ((String)metadata.get(vertDim_name)).equals(array_dim_names[k]) ) {
            vert_idx = k;
          }
        }

        int[] lengths = new int[2];

        if (track_idx < vert_idx) {
          domainRealTypes[0] = vert;
          domainRealTypes[1] = track;
          track_tup_idx = 1;
          vert_tup_idx = 0;
          lengths[0] = array_dim_lengths[vert_idx];
          lengths[1] = array_dim_lengths[track_idx];
        }
        else {
          domainRealTypes[0] = track;
          domainRealTypes[1] = vert;
          track_tup_idx = 0;
          vert_tup_idx = 1;
          lengths[0] = array_dim_lengths[track_idx];
          lengths[1] = array_dim_lengths[vert_idx];
        }

        TrackLen = array_dim_lengths[track_idx];
        VertLen = array_dim_lengths[vert_idx];

        rangeType = RealType.getRealType((String)metadata.get(array_name), rangeUnit_s[0]);

        try {
          rangeProcessor = RangeProcessor.createRangeProcessor(reader, metadata);
        } 
        catch (Exception e) {
          System.out.println("RangeProcessor failed to create");
          e.printStackTrace();
        }

        try {
          RealTupleType domainTupType = new RealTupleType(domainRealTypes[0], domainRealTypes[1]);
          swathDomain = new Linear2DSet(domainTupType, 0, lengths[0]-1, lengths[0], 0, lengths[1]-1, lengths[1]);
        }
        catch (Exception e) {
          System.out.println("Navigation failed to create");
          e.printStackTrace();
        }

        try {
          vertLocs = getVertBinAltitude();
          trackTimes = getTrackTimes();
          vertLocType = makeVertLocType();
          trackTimeType = makeTrackTimeType();
        } 
        catch (Exception e) {
          System.out.println(e);
        }

      }

      public int getTrackLength() {
        return TrackLen;
      }

      public int getVertLength() {
        return VertLen;
      }

      public int getVertIdx() {
        return vert_idx;
      }

      public int getTrackIdx() {
        return track_idx;
      }
                                                                                                                                                     
      public Set makeDomain(Object subset) throws Exception {
        double[] first = new double[2];
        double[] last = new double[2];
        int[] length = new int[2];

        HashMap<String, double[]> domainSubset = new HashMap<String, double[]>();
        domainSubset.put(trackDim_name, (double[]) ((HashMap)subset).get(trackDim_name));
        domainSubset.put(vertDim_name, (double[]) ((HashMap)subset).get(vertDim_name));

        for (int kk=0; kk<2; kk++) {
          RealType rtype = domainRealTypes[kk];
          double[] coords = (double[]) ((HashMap)subset).get(rtype.getName());
          first[kk] = coords[0];
          last[kk] = coords[1];
          length[kk] = (int) ((last[kk] - first[kk])/coords[2] + 1);
          last[kk] = first[kk]+coords[2]*(length[kk]-1);
        }
        Linear2DSet domainSet = new Linear2DSet(first[0], last[0], length[0], first[1], last[1], length[1]);
        final Linear1DSet[] lin1DSet_s = new Linear1DSet[] {domainSet.getLinear1DComponent(0),
                                           domainSet.getLinear1DComponent(1)};

        float[] new_altitudes = new float[length[vert_tup_idx]];
        float[] new_times = new float[length[track_tup_idx]];

        int track_start = (int) first[track_tup_idx];
        int vert_start = (int) first[vert_tup_idx];
        int vert_skip = (int) ((double[]) ((HashMap)subset).get(vertDim_name))[2];
        int track_skip = (int) ((double[]) ((HashMap)subset).get(trackDim_name))[2];
        for (int k=0; k<new_altitudes.length; k++) {
          new_altitudes[k] = vertLocs[vert_start+(k*vert_skip)];
        }

        for (int k=0; k<new_times.length; k++) {
          new_times[k] = trackTimes[track_start+(k*track_skip)];
        }

        final Gridded1DSet alt_set = new Gridded1DSet(vertLocType, new float[][] {new_altitudes}, new_altitudes.length);
        final Gridded1DSet time_set = new Gridded1DSet(trackTimeType, new float[][] {new_times}, new_times.length);
        final float vert_offset = (float) first[vert_tup_idx];
        final float track_offset = (float) first[track_tup_idx];

        RealTupleType reference = new RealTupleType(vertLocType, trackTimeType);
        
        CoordinateSystem cs = null;

        try {
        cs = new CoordinateSystem(reference, null) {
          public float[][] toReference(float[][] vals) throws VisADException {
            int[] indexes = lin1DSet_s[0].valueToIndex(new float[][] {vals[0]});
            for (int k=0; k<vals[0].length;k++) {
              //-indexes[k] = (int) (vals[vert_tup_idx][k] - vert_offset); ?
            }
            float[][] alts = alt_set.indexToValue(indexes);

            indexes = lin1DSet_s[1].valueToIndex(new float[][] {vals[1]});
            for (int k=0; k<vals[0].length;k++) {
              //-indexes[k] = (int) (vals[track_tup_idx][k] - track_offset); ?
            }
            float[][] times = time_set.indexToValue(indexes);

            return new float[][] {alts[0], times[0]};
          }
          public float[][] fromReference(float[][] vals) throws VisADException {
            int[] indexes = alt_set.valueToIndex(new float[][] {vals[vert_tup_idx]});
            float[][] vert_coords = lin1DSet_s[vert_tup_idx].indexToValue(indexes);
            indexes = time_set.valueToIndex(new float[][] {vals[track_tup_idx]});
            float[][] track_coords = lin1DSet_s[track_tup_idx].indexToValue(indexes);
            return new float[][] {vert_coords[0], track_coords[0]};
          }
          public double[][] toReference(double[][] vals) throws VisADException {
            return Set.floatToDouble(toReference(Set.doubleToFloat(vals)));
          }
          public double[][] fromReference(double[][] vals) throws VisADException {
            return Set.floatToDouble(fromReference(Set.doubleToFloat(vals)));
          }
          public boolean equals(Object obj) {
            return true;
          }
        };
        }
        catch (Exception e) {
        }

        RealTupleType domainTupType = new RealTupleType(domainRealTypes[0], domainRealTypes[1], cs, null);
        domainSet = new Linear2DSet(domainTupType, first[0], last[0], length[0], first[1], last[1], length[1]);

        return domainSet;
      }

      public FunctionType getMathType() {
        return null;
      }

      public RealType[] getDomainRealTypes() {
        return domainRealTypes;
      }

      public HashMap getDefaultSubset() {
        HashMap subset = ProfileAlongTrack.getEmptySubset();

        double[] coords = (double[])subset.get("TrackDim");
        coords[0] = 0.0;
        coords[1] = TrackLen - 1;
        coords[2] = 100.0;
        subset.put("TrackDim", coords);

        coords = (double[])subset.get("VertDim");
        coords[0] = 0.0;
        coords[1] = VertLen - 1;
        coords[2] = 5.0;
        subset.put("VertDim", coords);
        return subset;
      }

      public abstract float[] getVertBinAltitude() throws Exception;
      public abstract float[] getTrackTimes() throws Exception;
      public abstract RealType makeVertLocType() throws Exception;
      public abstract RealType makeTrackTimeType() throws Exception;
}
