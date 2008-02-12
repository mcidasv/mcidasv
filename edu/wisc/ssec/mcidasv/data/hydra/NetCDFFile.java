/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import ucar.nc2.*;
import ucar.ma2.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;


public class NetCDFFile implements MultiDimensionReader {

   HashMap<String, Variable> varMap = new HashMap<String, Variable>();
   HashMap<String, String[]> varDimNames = new HashMap<String, String[]>();
   HashMap<String, int[]> varDimLengths = new HashMap<String, int[]>();
   HashMap<String, Class> varDataType = new HashMap<String, Class>();


   NetcdfFile ncfile = null;


   public NetCDFFile(String filename) throws Exception {
     ncfile = NetcdfFile.open(filename);
     
     Iterator varIter = ncfile.getVariables().iterator();
     while(varIter.hasNext()) {
       Variable var = (Variable) varIter.next();

       if (var instanceof Structure) {
         analyzeStructure((Structure) var);
         continue;
       }

       int rank = var.getRank();
       String varName = var.getName();
       varMap.put(var.getName(), var);
       Iterator dimIter = var.getDimensions().iterator();
       String[] dimNames = new String[rank];
       int[] dimLengths = new int[rank];
       int cnt = 0;
       while(dimIter.hasNext()) {
         Dimension dim = (Dimension) dimIter.next();
         String dim_name = dim.getName();
         if (dim_name == null) dim_name = "dim"+cnt;
         dimNames[cnt] = dim_name;
         dimLengths[cnt] = dim.getLength();
         cnt++;
       }
       varDimNames.put(varName, dimNames);
       varDimLengths.put(varName, dimLengths);
       varDataType.put(varName, var.getDataType().getPrimitiveClassType());
     }
   }

   void analyzeStructure(Structure var) throws Exception {
     if ((var.getShape()).length == 0) {
       return;
     }
     String varName = var.getName();
     String[] dimNames = new String[2];
     int[] dimLengths = new int[2];
     List vlist = var.getVariables();
     int cnt = 0;
     dimLengths[0] = (var.getShape())[0];
     dimNames[0] = "dim"+cnt;
     
     varDataType.put(varName, ((Variable)vlist.get(0)).getDataType().getClassType());
     
     cnt++;
     StructureData sData = var.readStructure(0);
     List memList = sData.getMembers();
     dimLengths[1] = memList.size();
     dimNames[1] = "dim"+cnt;

     varDimNames.put(varName, dimNames);
     varDimLengths.put(varName, dimLengths);
     varMap.put(var.getName(), var);
   }

   public Class getArrayType(String array_name) {
     Variable var = varMap.get(array_name);
     return varDataType.get(array_name);
   }

   public String[] getDimensionNames(String array_name) {
     return varDimNames.get(array_name);
   }

   public int[] getDimensionLengths(String array_name) {
     return varDimLengths.get(array_name);
   }

   public float[] getFloatArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     return (float[]) readArray(array_name, start, count, stride);
   }

   public int[] getIntArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     return (int[]) readArray(array_name, start, count, stride);
   }

   public double[] getDoubleArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     return (double[]) readArray(array_name, start, count, stride);
   }

   public short[] getShortArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     return (short[]) readArray(array_name, start, count, stride);
   }

   public byte[] getByteArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     return (byte[]) readArray(array_name, start, count, stride);
   }

   private Object readArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     Variable var = varMap.get(array_name);
     if (var instanceof Structure) {
       Array array = Array.factory(getArrayType(array_name), count);
       Index2D idx = new Index2D(count);
       for (int i=0; i<count[0]; i++) {
         StructureData sData = ((Structure)var).readStructure(start[0]+i);
         for (int j=0; j<count[1]; j++) {
           Object obj = sData.getScalarObject(sData.getMember(start[1]+j));
           idx.set(i,j);
           array.setObject(idx, obj);
         }
       }
       return array.copyTo1DJavaArray();
     }
     else {
       ArrayList rangeList = new ArrayList();
       for (int i=0;i<start.length;i++) {
         Range rng = new Range(start[i], start[i]+(count[i]-1)*stride[i], stride[i]);
         rangeList.add(i, rng);
       }
       Array array = var.read(rangeList);
       return array.copyTo1DJavaArray();
     }
   }

   public HDFArray getGlobalAttribute(String attr_name) throws Exception {
     throw new Exception("NetCDFFile.getGlobalAttributes: Unimplemented");
   }

   public HDFArray getArrayAttribute(String array_name, String attr_name) throws Exception {
     Variable var = varMap.get(array_name);
     Attribute attr = var.findAttribute(attr_name);
     Array attrVals = attr.getValues();
     DataType dataType = attr.getDataType();
     Object array = attrVals.copyTo1DJavaArray();
     
     HDFArray harray = null;

     if (dataType.getPrimitiveClassType() == Float.TYPE) {
       harray = HDFArray.make((float[])array);
     }
     else if (dataType.getPrimitiveClassType() == Double.TYPE) {
       harray = HDFArray.make((double[])array);
     }
     else if (dataType == DataType.STRING) {
       harray = HDFArray.make((String[])array);
     }
     else if (dataType.getPrimitiveClassType() == Short.TYPE) {
       harray = HDFArray.make((short[])array);
     }
     else if (dataType.getPrimitiveClassType() == Integer.TYPE) {
       harray = HDFArray.make((int[])array);
     }
     return harray;
   }

   public void close() throws Exception {
     ncfile.close();
   }

   public HashMap getVarMap() {
     return varMap;
   }

   public static void main(String[] args) throws Exception {
     NetCDFFile ncfile = new NetCDFFile(args[0]);
     ncfile.close();
   }
}
