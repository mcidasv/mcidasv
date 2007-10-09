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
       int rank = var.getRank();
       String varName = var.getName();
       varMap.put(var.getName(), var);
       Iterator dimIter = var.getDimensions().iterator();
       String[] dimNames = new String[rank];
       int[] dimLengths = new int[rank];
       int cnt = 0;
       while(dimIter.hasNext()) {
         Dimension dim = (Dimension) dimIter.next();
         dimNames[cnt] = dim.getName();
         dimLengths[cnt] = dim.getLength();
         cnt++;
       }
       varDimNames.put(varName, dimNames);
       varDimLengths.put(varName, dimLengths);
       varDataType.put(varName, var.getDataType().getPrimitiveClassType());
     }
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
     return (float[]) readArray(array_name, start, count, stride);
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
     ArrayList rangeList = new ArrayList();
     for (int i=0;i<start.length;i++) {
       Range rng = new Range(start[i], start[i]+(count[i]-1)*stride[i], stride[i]);
       rangeList.add(i, rng);
     }
     Array array = var.read(rangeList);
     return array.copyTo1DJavaArray();
   }

   public HDFArray getGlobalAttribute(String attr_name) throws Exception {
     throw new Exception("NetCDFFile.getGlobalAttributes: Unimplemented");
   }

   public HDFArray getArrayAttribute(String array_name, String attr_name) throws Exception {
     throw new Exception("NetCDFFile.getArrayAttribute: Unimplemented");
   }

   public void close() throws Exception {
     ncfile.close();
   }

   public static void main(String[] args) throws Exception {
     NetCDFFile ncfile = new NetCDFFile(args[0]);
     ncfile.close();
   }
}
