package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.HashMap;

public class HDF4File implements MultiDimensionReader {

   HashMap<String, HDFVariable> varMap = new HashMap<String, HDFVariable>();
   HashMap<String, String[]> varDimNames = new HashMap<String, String[]>();
   HashMap<String, int[]> varDimLengths = new HashMap<String, int[]>();
   HashMap<String, Class> varDataType = new HashMap<String, Class>();

   HDFFile hdf = null;

   public HDF4File(String filename) throws Exception {
     hdf = new HDFFile(filename);
     
     for (int kk=0; kk<hdf.getNumberSDdatasets(); kk++) {
       HDFVariable var = hdf.select(kk);
       HDFVariableInfo info = var.getinfo();
       varMap.put(info.var_name, var);
       varDataType.put(info.var_name, getArrayClass(info.data_type));
 
       int[] dim_lengths = info.dim_lengths;
       int[] new_dim_lengths = new int[info.rank];
       String[] dim_names = new String[info.rank];
       for (int tt=0; tt<info.rank; tt++) {
         int dim_id = var.getdimid(tt);
         HDFDimension hdim = hdf.diminfo(dim_id);
         dim_names[tt] = hdim.name;
         new_dim_lengths[tt] = dim_lengths[tt];
       }
       varDimNames.put(info.var_name, dim_names);
       varDimLengths.put(info.var_name, dim_lengths);
     }
   }

   private Class getArrayClass(int hdf_data_type_id) {
     if (hdf_data_type_id == 3 || hdf_data_type_id == 4) {
       return Character.TYPE;
     } else if (hdf_data_type_id == 20 || hdf_data_type_id == 21) {
       return Byte.TYPE;
     } else if ( hdf_data_type_id == 22 || hdf_data_type_id == 23) {
       return Short.TYPE;
     } else if ( hdf_data_type_id == 5 ) {
       return Float.TYPE;
     } else if ( hdf_data_type_id == 25 || hdf_data_type_id == 24) {
       return Integer.TYPE;
     } else if ( hdf_data_type_id == 6 ) {
       return Double.TYPE;
     }
     return null;
   }

   public Class getArrayType(String array_name) {
     return varDataType.get(array_name);
   }

   public String[] getDimensionNames(String array_name) {
     return varDimNames.get(array_name);
   }

   public int[] getDimensionLengths(String array_name) {
     return varDimLengths.get(array_name);
   }

   public float[] getFloatArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     HDFVariable var = varMap.get(array_name);
     HDFArray array = var.readdata(start, stride, count);
     return (float[]) array.getArray();
   }

   public double[] getDoubleArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     HDFVariable var = varMap.get(array_name);
     HDFArray array = var.readdata(start, stride, count);
     return (double[]) array.getArray();
   }

   public short[] getShortArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     HDFVariable var = varMap.get(array_name);
     HDFArray array = var.readdata(start,stride,count);
     return (short[]) array.getArray();
   }

   public byte[] getByteArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     HDFVariable var = varMap.get(array_name);
     HDFArray array = (HDFArray) var.readdata(start, stride, count);
     return (byte[]) array.getArray();
   }

   public HDFArray getGlobalAttribute(String attr_name) throws Exception {
     int attr_idx = hdf.findattr(attr_name);
     return hdf.readattr(attr_idx);
   }

   public HDFArray getArrayAttribute(String array_name, String attr_name) throws Exception {
     HDFVariable var = varMap.get(array_name);
     int attr_idx = var.findattr(attr_name);
     return var.readattr(attr_idx);
   }

   public void close() throws Exception {
     hdf.close();
   }

   public static void main(String[] args) throws Exception {
     HDF4File hfile = new HDF4File(args[0]);
     hfile.close();
   }
}
