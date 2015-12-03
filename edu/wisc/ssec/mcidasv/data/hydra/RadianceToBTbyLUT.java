package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.HashMap;

/**
 *
 * 
 */
public class RadianceToBTbyLUT extends RangeProcessor {
   
   LUTtransform lutCal;
   
   
   public RadianceToBTbyLUT(NetCDFFile reader, HashMap metadata, String radLUTname, String btLUTname) throws Exception {
      super(reader, metadata);
      
      int numLUTvals = (reader.getDimensionLengths(radLUTname))[0];
      float[] radLUT = reader.getFloatArray(radLUTname, new int[] {0}, new int[] {numLUTvals}, new int[] {1});
      float[] btLUT = reader.getFloatArray(btLUTname, new int[] {0}, new int[] {numLUTvals}, new int[] {1});
      
      lutCal = new LUTtransform(radLUT, btLUT);
   }
   
   public RadianceToBTbyLUT(NetCDFFile reader, HashMap metadata) throws Exception {
      super(reader, metadata);
   }
   
   /**
    * calls super to unscale radiances then converts to BT
    * 
    */
   public float[] processRange(short[] values, HashMap subset) {
      float[] radiances = super.processRange(values, subset);
      
      float[] brightnessTemps = null;
      
      try {
         brightnessTemps = lutCal.radianceToBrightnessTemp(radiances);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      
      return brightnessTemps;
   }
}
