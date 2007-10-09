package edu.wisc.ssec.mcidasv.data.hydra;

import visad.Data;
import visad.FlatField;
import visad.Set;
import visad.CoordinateSystem;
import visad.RealType;
import visad.RealTupleType;
import visad.SetType;
import visad.Linear2DSet;
import visad.Unit;
import visad.FunctionType;
import visad.VisADException;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.HashMap;


public class SwathAdapter extends MultiDimensionAdapter {

      private FunctionType mathtype;

      String nav_type = "Interp";
      boolean lon_lat_trusted = true;

      private int TrackLen;
      private int XTrackLen;

      static String longitude_name = "Longitude";
      static String latitude_name  = "Latitude";
      static String track_name  = "Track";
      static String xtrack_name = "XTrack";
      static String geo_track_name = "geo_Track";
      static String geo_xtrack_name  = "geo_XTrack";
      static String array_name = "array_name";
      static String lon_array_name = "lon_array_name";
      static String lat_array_name = "lat_array_name";
      static String range_name = "range_name";
      static String scale_name = "scale_name";
      static String offset_name = "offset_name";
      static String fill_value_name = "fill_value_name";

      String[] rangeName_s  = null;
      Class[] arrayType_s = null;
      Unit[] rangeUnit_s  = new Unit[] {null};

      RealType track  = RealType.getRealType(track_name);
      RealType xtrack = RealType.getRealType(xtrack_name);
      RealType[] domainRealTypes = new RealType[2];
      RealType rangeType;

      int track_idx      = -1;
      int xtrack_idx     = -1;
      int lon_track_idx  = -1;
      int lon_xtrack_idx = -1;
      int lat_track_idx  = -1;
      int lat_xtrack_idx = -1;
      int range_rank     = -1;

      private RangeProcessor rangeProcessor;
      private SwathNavigation navigation;

      private Linear2DSet swathDomain;

      public static HashMap getEmptySubset() {
        HashMap<String, double[]> subset = new HashMap<String, double[]>();
        subset.put(track_name, new double[3]);
        subset.put(xtrack_name, new double[3]);
        return subset;
      }

      public static HashMap getEmptyMetadataTable() {
        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put(array_name, null);
        metadata.put(track_name, null);
        metadata.put(xtrack_name, null);
        metadata.put(geo_track_name, null);
        metadata.put(geo_xtrack_name, null);
        metadata.put(lon_array_name, null);
        metadata.put(lat_array_name, null);
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

      public SwathAdapter() {

      }

      public SwathAdapter(MultiDimensionReader reader, HashMap metadata) {
        super(reader, metadata);
        this.init();
      }

      private void init() {
        for (int k=0; k<array_rank;k++) {
          if ( ((String)metadata.get(track_name)).equals(array_dim_names[k]) ) {
            track_idx = k;
          }
          if ( ((String)metadata.get(xtrack_name)).equals(array_dim_names[k]) ) {
            xtrack_idx = k;
          }
        }
        int[] lengths = new int[2];
        if (track_idx < xtrack_idx) {
          domainRealTypes[0] = xtrack;
          domainRealTypes[1] = track;
          lengths[0] = array_dim_lengths[xtrack_idx];
          lengths[1] = array_dim_lengths[track_idx];
        }
        else {
          domainRealTypes[0] = track;
          domainRealTypes[1] = xtrack;
          lengths[0] = array_dim_lengths[track_idx];
          lengths[1] = array_dim_lengths[xtrack_idx];
        }

        TrackLen = array_dim_lengths[track_idx];
        XTrackLen = array_dim_lengths[xtrack_idx];

        rangeType = RealType.getRealType((String)metadata.get(array_name), rangeUnit_s[0]);

        try {
          rangeProcessor = RangeProcessor.createRangeProcessor(reader, metadata);
        } 
        catch (Exception e) {
          System.out.println("RangeProcessor failed to create");
          e.printStackTrace();
        }

        try {
          navigation = SwathNavigation.createNavigation(this);
          RealTupleType domainTupType = new RealTupleType(domainRealTypes[0], domainRealTypes[1]);
          swathDomain = new Linear2DSet(domainTupType, 0, lengths[0]-1, lengths[0], 0, lengths[1]-1, lengths[1]);
        }
        catch (Exception e) {
          System.out.println("Navigation failed to create");
          e.printStackTrace();
        }

      }

      public FlatField getData(Object subset) throws Exception {
        Set domainSet = makeDomain(subset);
        return makeFlatField(domainSet, subset);
      }

      public int getTrackLength() {
        return TrackLen;
      }

      public int getXtrackLength() {
        return XTrackLen;
      }

      Set makeDomain(Object subset) throws Exception {
        double[] first = new double[2];
        double[] last = new double[2];
        int[] length = new int[2];

        HashMap<String, double[]> domainSubset = new HashMap<String, double[]>();
        domainSubset.put(track_name, (double[]) ((HashMap)subset).get(track_name));
        domainSubset.put(xtrack_name, (double[]) ((HashMap)subset).get(xtrack_name));

        for (int kk=0; kk<2; kk++) {
          RealType rtype = domainRealTypes[kk];
          double[] coords = (double[]) ((HashMap)subset).get(rtype.getName());
          first[kk] = coords[0];
          last[kk] = coords[1];
          length[kk] = (int) ((last[kk] - first[kk])/coords[2] + 1);
        }

        Linear2DSet domainSet = new Linear2DSet(first[0], last[0], length[0], first[1], last[1], length[1]);
        CoordinateSystem cs = navigation.getVisADCoordinateSystem(domainSet, domainSubset);

        RealTupleType domainTupType = new RealTupleType(domainRealTypes[0], domainRealTypes[1], cs, null);
        domainSet = new Linear2DSet(domainTupType, first[0], last[0], length[0], first[1], last[1], length[1]);

        return domainSet;
      }


      private FlatField makeFlatField(Set domainSet, float[][] range) throws VisADException, RemoteException {
        FlatField f_field = makeFlatField(domainSet);
        f_field.setSamples(range, false);
        return f_field;
      }

      private FlatField makeFlatField(Set domainSet, double[][] range) throws VisADException, RemoteException {
        FlatField f_field = makeFlatField(domainSet);
        f_field.setSamples(range, false);
        return f_field;
      }

      private FlatField makeFlatField(Set domainSet) throws VisADException, RemoteException {
        FlatField f_field = new FlatField(new FunctionType(((SetType)domainSet.getType()).getDomain(), rangeType), domainSet);
        return f_field;
      }

      public FlatField makeFlatField(Set domainSet, Object subset) throws Exception {
        FlatField f_field = null;

        Object range = readArray(subset);

        if (arrayType == Float.TYPE) {
          f_field = makeFlatField(domainSet, new float[][] {(float[])range});
        }
        else if (arrayType == Double.TYPE) {
          f_field = makeFlatField(domainSet, new double[][] {(double[])range});
        }
        else if (arrayType == Short.TYPE) {
          float[] float_range = processRange((short[])range, subset);
          f_field = makeFlatField(domainSet, new float[][] {float_range});
        }

        return f_field;
      }

      public float[] processRange(short[] range, Object subset) {
        return rangeProcessor.processRange(range, (HashMap)subset);
      }

      public FunctionType getMathType() {
        return null;
      }

      public RealType[] getDomainRealTypes() {
        return domainRealTypes;
      }

      public Linear2DSet getSwathDomain() {
        return swathDomain;
      }

      public HashMap getDefaultSubset() {
        HashMap subset = SwathAdapter.getEmptySubset();

        double[] coords = (double[])subset.get("Track");
        coords[0] = 0.0;
        coords[1] = TrackLen - 1;
        coords[2] = 1.0;
        subset.put("Track", coords);

        coords = (double[])subset.get("XTrack");
        coords[0] = 0.0;
        coords[1] = XTrackLen - 1 ;
        coords[2] = 1.0;
        subset.put("XTrack", coords);
        return subset;
      }
        
}
