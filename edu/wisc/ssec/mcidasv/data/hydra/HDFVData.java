package edu.wisc.ssec.mcidasv.data.hydra;

public class HDFVData {
   String name;
   int fId; 
   int vId;
   HDF hdf;

   HDFVData(String name, int fId, int vId, HDF hdf) {
      this.name = name;
      this.fId = fId;
      this.vId = vId;
      this.hdf = hdf;
   }
                                                                                                                    
   float[] read(int nRecs, int startIdx) throws Exception {
      return null;
   }
                                                                                                                    
   public String toString() {
      return "HDF Vdata object id"+vId;
   }
                                                                                                                    
   void close() throws Exception {
     hdf.vsDetach(vId);
     hdf.vEnd(fId);
   }
}
