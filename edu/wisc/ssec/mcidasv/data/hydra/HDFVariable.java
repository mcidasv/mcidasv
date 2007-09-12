package edu.wisc.ssec.mcidasv.data.hydra;

public class HDFVariable {
      int sds_id;
      HDF hdf;

      public HDFVariable(HDF hdf, int sds_id) throws Exception {
        this.hdf = hdf;
        this.sds_id = sds_id;
      }

      public int findattr(String attr_name) throws Exception {
        return hdf.findattr(sds_id, attr_name);
      }

      public HDFArray readattr(int attr_index) throws Exception {
        return hdf.readattr(sds_id, attr_index);
      }

      public int getdimid(int dim_index) throws Exception {
        return hdf.getdimid(sds_id, dim_index);
      }

      public HDFVariableInfo getinfo() throws Exception {
        return hdf.getinfo(sds_id);
      }

      public HDFArray readdata(int[] start, int[] stride, int[] edges) throws Exception {
        return hdf.readdata(sds_id, start, stride, edges);
      }

}
