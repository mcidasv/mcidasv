package edu.wisc.ssec.mcidasv.data.hydra;

import visad.*;
                                                                                                                                          
                                                                                                                                          
public class HistogramField
{
  Linear2DSet histSet;
  Linear1DSet    set0;
  Linear1DSet    set1;
  int            len0;
  int            len1;
  int[] count;
  int[][] indexes;
  FlatField field_0;
  FlatField field_1;
  FlatField mask_field;
  float[][] maskRange;

  Class rangeType;
                                                                                                                                          
                                                                                                                                          
  public HistogramField(FlatField field_0, FlatField field_1,
                        FlatField mask_field,
                        int n_bins, int bin_size)
         throws Exception
  {
    this.field_0 = field_0;
    this.field_1 = field_1;
    this.mask_field = mask_field;
    maskRange = mask_field.getFloats(false);

    Set[] rangeSets = field_0.getRangeSets();
    Set rngSet = rangeSets[0];

    if (rngSet instanceof FloatSet) {
      rangeType = Float.TYPE; 
    } 
    else if (rngSet instanceof DoubleSet) {
      rangeType = Double.TYPE;
    }
                                                                                                                                          
    double[] minmax_0 = {Double.MAX_VALUE, -Double.MAX_VALUE};
    double[] minmax_1 = {Double.MAX_VALUE, -Double.MAX_VALUE};

    indexes   = new int[n_bins*n_bins][bin_size];
    count     = new int[n_bins*n_bins];
    double[][] val  = new double[2][1];
    int[] histIdx  = null;

    if (rangeType == Double.TYPE) {
      double[][] vals_0  = field_0.getValues(false);
      double[][] vals_1  = field_1.getValues(false);
                                                                                                                                          
      int n_samples = vals_0[0].length;
                                                                                                                                          
      for (int k = 0; k < n_samples; k++) {
        double v0 = vals_0[0][k];
        if (v0 < minmax_0[0]) minmax_0[0] = v0;
        if (v0 > minmax_0[1]) minmax_0[1] = v0;
                                                                                                                                          
        double v1 = vals_1[0][k];
        if (v1 < minmax_1[0]) minmax_1[0] = v1;
        if (v1 > minmax_1[1]) minmax_1[1] = v1;
      }

      histSet = new Linear2DSet(minmax_0[0], minmax_0[1], n_bins,
                                minmax_1[0], minmax_1[1], n_bins);

      for (int k = 0; k < n_samples; k++) {
        val[0][0] = vals_0[0][k];
        val[1][0] = vals_1[0][k];
        histIdx = histSet.doubleToIndex(val);
        if (histIdx[0] >= 0) {
          int len = indexes[histIdx[0]].length;
          if (count[histIdx[0]] > len-1) { //-grow array
            int[] tmp = new int[len+bin_size];
            System.arraycopy(indexes[histIdx[0]], 0, tmp, 0, len);
            indexes[histIdx[0]] = tmp;
          }
          indexes[histIdx[0]][count[histIdx[0]]++] = k;
        }
      }
    }
    else if (rangeType == Float.TYPE) {
      float[][] vals_0 = field_0.getFloats(false);
      float[][] vals_1 = field_1.getFloats(false);

      int n_samples = vals_0[0].length;
      for (int k = 0; k < n_samples; k++) {
        double v0 = vals_0[0][k];
        if (v0 < minmax_0[0]) minmax_0[0] = v0;
        if (v0 > minmax_0[1]) minmax_0[1] = v0;
                                                                                                                                                           
        double v1 = vals_1[0][k];
        if (v1 < minmax_1[0]) minmax_1[0] = v1;
        if (v1 > minmax_1[1]) minmax_1[1] = v1;
      }

      histSet = new Linear2DSet(minmax_0[0], minmax_0[1], n_bins,
                                minmax_1[0], minmax_1[1], n_bins);

      for (int k = 0; k < n_samples; k++) {
        val[0][0] = vals_0[0][k];
        val[1][0] = vals_1[0][k];
        histIdx = histSet.doubleToIndex(val);
        if (histIdx[0] >= 0) {
          int len = indexes[histIdx[0]].length;
          if (count[histIdx[0]] > len-1) { //-grow array
            int[] tmp = new int[len+bin_size];
            System.arraycopy(indexes[histIdx[0]], 0, tmp, 0, len);
            indexes[histIdx[0]] = tmp;
          }
          indexes[histIdx[0]][count[histIdx[0]]++] = k;
        }
      }

    }
                                                                                                                                          
    set0 = histSet.getLinear1DComponent(0);
    set1 = histSet.getLinear1DComponent(1);
    len0 = set0.getLength();
    len1 = set1.getLength();
  }

  public FlatField markMaskFieldByRange(double[] lowhi_0, double[] lowhi_1, float maskVal)
       throws Exception {

    int[] hist0 = set0.doubleToIndex(new double[][] {{lowhi_0[0], lowhi_0[1]}});
    int[] hist1 = set1.doubleToIndex(new double[][] {{lowhi_1[0], lowhi_1[1]}});
                                                                                                                                          
    if (hist0[0] < 0) {
      if (lowhi_0[0] < lowhi_0[1]) {
        hist0[0] = 0;
      }
      else {
        hist0[0] = len0 - 1;
      }
    }
    if (hist0[1] < 0) {
      if (lowhi_0[0] < lowhi_0[1]) {
        hist0[1] = len0 - 1;
      }
      else {
        hist0[1] = 0;
      }
    }
                                                                                                                                          
    if (hist1[0] < 0) {
      if (lowhi_1[0] < lowhi_1[1]) {
        hist1[0] = 0;
      }
      else {
        hist1[0] = len1 - 1;
      }
    }
    if (hist1[1] < 0) {
      if (lowhi_1[0] < lowhi_1[1]) {
        hist1[1] = len1 - 1;
      }
      else {
        hist1[1] = 0;
      }
    }
                                                                                                                                          
    int h00, h01, h10, h11;

    h10 = hist1[1];
    h11 = hist1[0];
    if (hist1[0] < hist1[1]) {
      h10 = hist1[0];
      h11 = hist1[1];
    }

    h00 = hist0[1];
    h01 = hist0[0];
    if (hist0[0] < hist0[1]) {
      h00 = hist0[0];
      h01 = hist0[1];
    }

    for (int k=0; k<maskRange[0].length; k++) {
       if (maskRange[0][k] == maskVal) {
         maskRange[0][k] = Float.NaN;
       }
    }

    for (int j = h10; j <= h11; j++) {
      for (int i = h00; i <= h01; i++) {
        int idx = j*set0.getLengthX() + i;
        for (int k = 0; k < count[idx]; k++) {
          maskRange[0][indexes[idx][k]] = maskVal;
        }
      }
    }

    mask_field.setSamples(maskRange, false);
    return mask_field;
  }

  public void clearMaskField(float maskVal) {
    for (int k=0; k<maskRange[0].length; k++) {
      if (maskRange[0][k] == maskVal) {
         maskRange[0][k] = Float.NaN;
      }
    }
  }
}
