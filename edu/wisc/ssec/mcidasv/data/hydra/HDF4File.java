/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2011
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

   public int[] getIntArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     HDFVariable var = varMap.get(array_name);
     HDFArray array = var.readdata(start,stride,count);
     return (int[]) array.getArray();
   }

   public byte[] getByteArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     HDFVariable var = varMap.get(array_name);
     HDFArray array = (HDFArray) var.readdata(start, stride, count);
     return (byte[]) array.getArray();
   }

   public Object getArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     throw new Exception("getArray not implemented");
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
