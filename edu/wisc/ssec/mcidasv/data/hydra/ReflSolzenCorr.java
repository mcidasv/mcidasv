package edu.wisc.ssec.mcidasv.data.hydra;

import visad.FlatField;
import java.util.HashMap;

/**
 *
 * 
 */
public class ReflSolzenCorr extends RangeProcessor {
   
   ArrayAdapter solzenAdapter;
   
   
   public ReflSolzenCorr(NetCDFFile reader, HashMap metadata, ArrayAdapter solzenAdapter) throws Exception {
      super(reader, metadata);
      this.solzenAdapter = solzenAdapter;
   }
   
   public ReflSolzenCorr(NetCDFFile reader, HashMap metadata) throws Exception {
      super(reader, metadata);
   }
   
   /**
    * super returns uncorrected reflectance.
    * 
    */
   public float[] processRange(short[] values, HashMap subset) {
      float[] refls = super.processRange(values, subset);
      float[] solzen = null;
      
      try {
         solzen = ((FlatField)solzenAdapter.getData(subset)).getFloats()[0];
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      for (int k=0; k<refls.length; k++) {
         float refl = refls[k];
         float solz = solzen[k];
         if (solz < 88f) {
            refls[k] = refl/((float)Math.cos((Math.PI/180.0)*solz));
         }
      }
      
      return refls;
   }
}
