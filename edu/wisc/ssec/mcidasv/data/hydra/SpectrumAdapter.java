package edu.wisc.ssec.mcidasv.data.hydra;

import visad.Data;
import visad.FlatField;
import visad.FunctionType;
import visad.RealType;
import visad.Gridded1DSet;
import visad.QuickSort;
import visad.Unit;
import visad.ScaledUnit;
import visad.CommonUnit;
import visad.DerivedUnit;
import java.util.HashMap;


public class SpectrumAdapter extends MultiDimensionAdapter {

  public static String channels_name = "Channels";
  public static String channelIndex_name = "channelIndex";
  public static String channelUnit = "cm";
  public static String channelType = "wavenumber";
  public static String array_name  = "array_name";
  public static String x_dim_name  = "x_dim"; //- 2 spatial dimensions, x fastest varying
  public static String y_dim_name  = "y_dim"; //-----------------------------------------
  public static String time_dim_name = "time_dim";
  public static String ancillary_file_name = "ancillary_file";


  public static HashMap getEmptyMetadataTable() {
    HashMap<String, String> metadata = new HashMap<String, String>();
    metadata.put(array_name, null);
    metadata.put(channelIndex_name, null);
    metadata.put(ancillary_file_name, null);
    metadata.put(x_dim_name, null);
    metadata.put(y_dim_name, null);
    metadata.put(time_dim_name, null);
    /*
    metadata.put(scale_name, null);
    metadata.put(offset_name, null);
    metadata.put(fill_value_name, null);
    metadata.put(range_name, null);
    metadata.put(range_unit, null);
    metadata.put(valid_range, null);
    */
    return metadata;
   }

   public static HashMap getEmptySubset() {
     HashMap<String, double[]> subset = new HashMap<String, double[]>();
     subset.put(x_dim_name, new double[3]);
     subset.put(y_dim_name, new double[3]);
     subset.put(channelIndex_name, new double[3]);
     return subset;
   }

   int numChannels;
   int channelIndex;
   int[] channel_sort;
   Gridded1DSet domainSet;
   RealType channelRealType;
   RealType spectrumRangeType;
   FunctionType spectrumType;

  public SpectrumAdapter(MultiDimensionReader reader, HashMap metadata) {
    super(reader, metadata);
    this.init();
  }

  private void init() {
    for (int k=0; k<array_rank;k++) {
      if ( ((String)metadata.get(channelIndex_name)).equals(array_dim_names[k]) ) {
        channelIndex = k;
      }
    }
    numChannels = array_dim_lengths[channelIndex];
    try {
      domainSet = getDomainSet();
      makeSpectrumRangeType();
      spectrumType = new FunctionType(channelRealType, spectrumRangeType);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("cannot create spectrum domain");
    }
  }

  public Gridded1DSet getDomainSet() throws Exception {
    RealType domainType = makeSpectrumDomainType();
    float[] channels = getChannels();
    channel_sort = QuickSort.sort(channels);
    Gridded1DSet domainSet = new Gridded1DSet(domainType, new float[][] {channels}, numChannels);
    return domainSet;
  }

  public float[] getChannels() throws Exception {
    float[] channels = reader.getFloatArray((String)metadata.get(channels_name), 
                                               new int[] {0}, new int[] {numChannels}, new int[] {1});
    return channels;
  }

  public RealType makeSpectrumDomainType() throws Exception {
    /**
    if ( ((String)metadata.get(channelType)).equals("wavenumber") ) {
      ScaledUnit centimeter = new ScaledUnit(0.01, CommonUnit.meter, "cm");
      Unit tmp_unit = centimeter.pow(-1);
      ScaledUnit inv_centimeter = new ScaledUnit(1.0, tmp_unit, "cm^-1");
      channelRealType = RealType.getRealType("wavenumber", null);
    }
    **/
    channelRealType = RealType.getRealType("wavenumber", null);
    return channelRealType;
  }

  public RealType makeSpectrumRangeType() throws Exception {
    spectrumRangeType = RealType.getRealType("Radiance");
    return spectrumRangeType;
  }

  public FlatField makeFlatField(Gridded1DSet domainSet, float[][] range) throws Exception {
    FlatField field = new FlatField(spectrumType, domainSet);
    float[] sorted_range = new float[numChannels];
    for (int k=0; k<numChannels; k++) sorted_range[k] = range[0][channel_sort[k]];
    field.setSamples(new float[][] {sorted_range});
    return field;
  }

  public FlatField makeFlatField(Gridded1DSet domainSet, double[][] range) throws Exception {
    FlatField field = new FlatField(spectrumType, domainSet);
    double[] sorted_range = new double[numChannels];
    for (int k=0; k<numChannels; k++) sorted_range[k] = range[0][channel_sort[k]];
    field.setSamples(new double[][] {sorted_range});
    return field;
  }

  public FlatField getData(Object subset) throws Exception {
    Object range = readArray(subset);
    FlatField f_field = null;                                                                                                                  
    if (arrayType == Float.TYPE) {
      float[] new_range = processRange((float[])range);
      f_field = makeFlatField(domainSet, new float[][] {(float[])range});
    }
    else if (arrayType == Double.TYPE) {
      double[] new_range = processRange((double[])range);
      f_field = makeFlatField(domainSet, new double[][] {(double[])range});
    }
    else if (arrayType == Short.TYPE) {
      throw new Exception("short type not currently supported");
      /*
      float[] float_range = processRange((short[])range, subset);
      f_field = makeFlatField(domainSet, new float[][] {float_range});
      */
    }
    return f_field;
  }


  public float[] processRange(float[] range) {
    return range;
  }

  public double[] processRange(double[] range) {
    return range;
  }

  public HashMap getDefaultSubset() {
    HashMap subset = SpectrumAdapter.getEmptySubset();
    
    double[] coords = (double[])subset.get(y_dim_name);
    coords[0] = 1.0;
    coords[1] = 1.0;
    coords[2] = 1.0;
    subset.put(y_dim_name, coords);
                                                                                                                                     
    coords = (double[])subset.get(x_dim_name);
    coords[0] = 1.0;
    coords[1] = 1.0;
    coords[2] = 1.0;
    subset.put(x_dim_name, coords);

    coords = (double[])subset.get(channelIndex_name);
    coords[0] = 0.0;
    coords[1] = (double) (numChannels - 1);
    coords[2] = 1.0;
    subset.put(channelIndex_name, coords);

    return subset;
  }

  public int getChannelIndexFromWavenumber(float wavenumber) throws Exception {
    int idx = (domainSet.valueToIndex(new float[][] {{wavenumber}}))[0];
    return channel_sort[idx];
  }

  public int getNumChannels() {
    return numChannels;
  }
}
