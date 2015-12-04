package edu.wisc.ssec.mcidasv.data.hydra;

import visad.Gridded1DSet;
import visad.Linear1DSet;
import visad.RealType;

/**
 * Convert radiance to/from brightness temperature via provided Lookup Tables
 * 
 */
public class LUTtransform {
   
   public static final int LINEAR = 0;
   public static final int GENERAL = 1;
   
   int ALGO = LINEAR;
   
   float[] radianceLUT;
   float[] brightnessTempLUT;
   
   int numOfLUTvalues;
   
   Gridded1DSet radLUTSet;
   Gridded1DSet btLUTSet;
   
   public LUTtransform(float[] radianceLUT, float[] brightnessTempLUT) throws Exception {
      this(radianceLUT, brightnessTempLUT, GENERAL);
   }
   
   public LUTtransform(float[] radianceLUT, float[] brightnessTempLUT, int algo) throws Exception {
      this.radianceLUT = radianceLUT;
      this.brightnessTempLUT = brightnessTempLUT;
      this.ALGO = algo;
      
      numOfLUTvalues = radianceLUT.length;
      if (numOfLUTvalues != brightnessTempLUT.length) {
         throw new Exception("radiance and brightnessTemp LUTs must have same length");
      }
      
      switch (ALGO) {
         case GENERAL:
            radLUTSet = new Gridded1DSet(RealType.Generic, new float[][] {radianceLUT}, numOfLUTvalues);           
            btLUTSet = new Gridded1DSet(RealType.Generic, new float[][] {brightnessTempLUT}, numOfLUTvalues);           
            break;
         case LINEAR:
            radLUTSet = new Linear1DSet(radianceLUT[0], radianceLUT[numOfLUTvalues-1], numOfLUTvalues);            
            btLUTSet = new Linear1DSet(brightnessTempLUT[0], brightnessTempLUT[numOfLUTvalues-1], numOfLUTvalues);            
            break;
      }
   }
   
   public float[] radianceToBrightnessTemp(float[] radiances) throws Exception {
      int numObs = radiances.length;
      float[] brightnessTemps = new float[numObs];
      
      int[] btLUTindexes = radLUTSet.valueToIndex(new float[][] {radiances});
      
      for (int k=0; k<numObs; k++) {
         int idx = btLUTindexes[k];
         if (idx >= 0) { // Just in case
            brightnessTemps[k] = brightnessTempLUT[idx];
         }
         else {
            brightnessTemps[k] = Float.NaN;
         }
      }
      
      return brightnessTemps;
   }
   
   public float[] brightnessTempToRadiance(float[] brightnessTemps) throws Exception {
      int numObs = brightnessTemps.length;
      float[] radiances = new float[numObs];
      
      int[] radLUTindexes = btLUTSet.valueToIndex(new float[][] {brightnessTemps});
      for (int k=0; k<numObs; k++) {
         int idx = radLUTindexes[k];
         if (idx >= 0) {
            radiances[k] = radianceLUT[idx];
         }
         else {
            radiances[k] = Float.NaN;
         }
      }
      
      return radiances;
   }
}
