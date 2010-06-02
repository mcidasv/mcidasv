/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

import java.util.HashMap;
import java.util.Iterator;

public class RangeProcessor {

  static RangeProcessor createRangeProcessor(MultiDimensionReader reader, HashMap metadata) throws Exception {
    if (metadata.get("scale_name") == null) {
      String product_name = (String) metadata.get(SwathAdapter.product_name);
      if (product_name == "IASI_L1C_xxx") {
        return new IASI_RangeProcessor();
      }
      return null;
    }
    else {
      String product_name = (String) metadata.get(ProfileAlongTrack.product_name);
      if (product_name == "2B-GEOPROF") {
        return new CloudSat_2B_GEOPROF_RangeProcessor(reader, metadata);
      }
      else {
        return new RangeProcessor(reader, metadata);
      }
    }
  }

  MultiDimensionReader reader;
  HashMap metadata;

  float[] scale = null;
  float[] offset = null;
  float[] missing = null;
  float[] valid_range = null;
  float valid_low  = -Float.MAX_VALUE;
  float valid_high = Float.MAX_VALUE;
  float[] low = new float[] {-Float.MAX_VALUE};
  float[] high = new float[] {Float.MAX_VALUE};
  boolean unpack = false;

  int scaleOffsetLen = 1;

  String multiScaleDimName = SpectrumAdapter.channelIndex_name;

  public RangeProcessor() {
  }

  public RangeProcessor(float scale, float offset, float valid_low, float valid_high, float missing) {
    this.scale = new float[] {scale};
    this.offset = new float[] {offset};
    this.missing = new float[] {missing};
    this.valid_low = valid_low;
    this.valid_high = valid_high;
  }


  public RangeProcessor(MultiDimensionReader reader, HashMap metadata, String multiScaleDimName) throws Exception {
    this(reader, metadata);
    this.multiScaleDimName = multiScaleDimName;
  }

  public RangeProcessor(MultiDimensionReader reader, HashMap metadata) throws Exception {
    this.reader = reader;
    this.metadata = metadata;

    if (metadata.get("unpack") != null) {
      unpack = true;
    }

    String array_name = (String) metadata.get("array_name");

    scale = getAttributeAsFloatArray(array_name, (String) metadata.get("scale_name"));

    offset = getAttributeAsFloatArray(array_name, (String) metadata.get("offset_name"));

    if (scale.length != offset.length) {
      throw new Exception("RangeProcessor: scale and offset array lengths must be equal");
    }
    scaleOffsetLen = scale.length;


    missing = getAttributeAsFloatArray(array_name, (String) metadata.get("fill_value_name"));

    String metaStr = (String)metadata.get("valid_range");
    if (metaStr != null) {

      valid_range = getAttributeAsFloatArray(array_name, metaStr);
      if (valid_range != null) {

        valid_low = valid_range[0];
        valid_high = valid_range[1];

        if (valid_range[0] > valid_range[1]) {
          valid_low = valid_range[1];
          valid_high = valid_range[0];
        }
      }
    }

  }

  public float[] getAttributeAsFloatArray(String arrayName, String attrName) 
         throws Exception 
  {
    float[] fltArray = null;
    HDFArray arrayAttr = reader.getArrayAttribute(arrayName, attrName);

    if (arrayAttr != null) {

      if (arrayAttr.getType().equals(Float.TYPE)) {
        float[] attr = (float[]) arrayAttr.getArray();
        fltArray = new float[attr.length];
        for (int k=0; k<attr.length; k++) fltArray[k] = attr[k];
      }
      else if (arrayAttr.getType().equals(Short.TYPE)) {
        short[] attr = (short[]) arrayAttr.getArray();
        fltArray = new float[attr.length];
        for (int k=0; k<attr.length; k++) fltArray[k] = (float) attr[k];
      }
      else if (arrayAttr.getType().equals(Integer.TYPE)) {
        int[] attr = (int[]) arrayAttr.getArray();
        fltArray = new float[attr.length];
        for (int k=0; k<attr.length; k++) fltArray[k] = (float) attr[k];
      }
      else if (arrayAttr.getType().equals(Double.TYPE)) {
        double[] attr = (double[]) arrayAttr.getArray();
        fltArray = new float[attr.length];
        for (int k=0; k<attr.length; k++) fltArray[k] = (float) attr[k];
      }

    }

    return fltArray;
  }

  public float[] processRange(short[] values, HashMap subset) {
     int channelIndex = 0;
     if (subset != null) {
       if (subset.get(SpectrumAdapter.channelIndex_name) != null) {
         channelIndex  = (int) ((double[])subset.get(SpectrumAdapter.channelIndex_name))[0];
       }
     }
     float[] new_values = new float[values.length];
     for (int k=0; k<values.length;k++) {
       float val = (float) values[k];
       if ((val == missing[0]) || (val < low[0]) || (val > high[0])) {
         new_values[k] = Float.NaN;
       }
       else {
         if (unpack) {
           new_values[k] = scale[channelIndex]*(val) + offset[channelIndex];
         }
         else {
           new_values[k] = scale[channelIndex]*(val - offset[channelIndex]);
         }
       }
     }
     return new_values;
  }


  public float[] processRange(short[] values) {
     float[] new_values = new float[values.length];
     for (int k=0; k<values.length;k++) {
       float val = (float) values[k];
       if ((val == missing[0]) || (val < low[0]) || (val > high[0])) {
         new_values[k] = Float.NaN;
       }
       else {
         if (unpack) {
           new_values[k] = scale[k]*val + offset[k];
         }
         else {
           new_values[k] = scale[k]*(val - offset[k]);
         }
       }
     }
     return new_values;
  }

  public float[] processRange(byte[] values, HashMap subset) {
     int channelIndex = 0;
     if (subset != null) {
       if (subset.get(SpectrumAdapter.channelIndex_name) != null) {
         channelIndex  = (int) ((double[])subset.get(SpectrumAdapter.channelIndex_name))[0];
       }
     }
     float[] new_values = new float[values.length];
     for (int k=0; k<values.length;k++) {
       float val = (float) values[k];
       if ((val == missing[0]) || (val < low[0]) || (val > high[0])) {
         new_values[k] = Float.NaN;
       }
       else {
         if (unpack) {
           new_values[k] = scale[channelIndex]*(val) + offset[channelIndex];
         }
         else {
           new_values[k] = scale[channelIndex]*(val - offset[channelIndex]);
         }
       }
     }
     return new_values;
  }

  public float[] processRange(byte[] values) {
     float[] new_values = new float[values.length];
     for (int k=0; k<values.length;k++) {
       float val = (float) values[k];
       if ((val == missing[0]) || (val < low[0]) || (val > high[0])) {
         new_values[k] = Float.NaN;
       }
       else {
         if (unpack) {
           new_values[k] = scale[k]*val + offset[k];
         }
         else {
           new_values[k] = scale[k]*(val - offset[k]);
         }
       }
     }
     return new_values;
  }

}

class IASI_RangeProcessor extends RangeProcessor {

  public IASI_RangeProcessor() throws Exception {
    super();
  }

  public float[] processRange(short[] values, HashMap subset) {
    int channelIndex = (int) ((double[]) subset.get(SpectrumAdapter.channelIndex_name))[0];

    float[] new_values = IASI_L1C_Utility.getDecodedIASIImage(values, null, channelIndex);

    double[] track_coords = (double[]) subset.get(SwathAdapter.track_name);
    double[] xtrack_coords = (double[]) subset.get(SwathAdapter.xtrack_name);

    int numElems = ((int)(xtrack_coords[1] - xtrack_coords[0]) + 1);
    int numLines = ((int)(track_coords[1] - track_coords[0]) + 1);

    new_values = IASI_L1C_Utility.psuedoScanReorder2(new_values, 60, numLines*2); 

    //- subset here, if necessary

    return new_values;
  }

}

class CloudSat_2B_GEOPROF_RangeProcessor extends RangeProcessor {

  public CloudSat_2B_GEOPROF_RangeProcessor(MultiDimensionReader reader, HashMap metadata) throws Exception {
    super(reader, metadata);
  }

  public float[] processRange(short[] values, HashMap subset) {
     float[] new_values = new float[values.length];
     for (int k=0; k<values.length;k++) {
       float val = (float) values[k];
       if (val == missing[0]) {
         new_values[k] = Float.NaN;
       }
       else if ((val < valid_low) || (val > valid_high)) {
         new_values[k] = -40f;
       }
       else {
         new_values[k] = val/scale[0] + offset[0];
       }
     }
     return new_values;
  }

}
