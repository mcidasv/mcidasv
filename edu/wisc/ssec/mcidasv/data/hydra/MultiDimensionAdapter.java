package edu.wisc.ssec.mcidasv.data.hydra;

import visad.Data;
import visad.VisADException;
import visad.CoordinateSystem;
import visad.RealTupleType;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;


public abstract class MultiDimensionAdapter {

   MultiDimensionReader reader = null;
   HashMap metadata = null;
   String arrayName = null;
   String[] array_dim_names = null;
   int[] array_dim_lengths  = null;
   int array_rank;
   Class arrayType;

   public MultiDimensionAdapter() {

   }

   public MultiDimensionAdapter(MultiDimensionReader reader, HashMap metadata) {
     this.reader = reader;
     this.metadata = metadata;
     this.init();
   }

   public abstract Data getData(Object subset) throws Exception;

   private void init() {
     this.arrayName = (String) metadata.get("array_name");
     array_dim_names = reader.getDimensionNames(arrayName);
     array_dim_lengths = reader.getDimensionLengths(arrayName);
     array_rank = array_dim_lengths.length;
     arrayType = reader.getArrayType(arrayName);
   }

   public Subset getIndexes(HashMap select) {
     Subset subset = new Subset(array_rank);
     int[] start = subset.getStart();
     int[] count = subset.getCount();
     int[] stride = subset.getStride();

     Iterator iter = select.keySet().iterator();
     while (iter.hasNext()) {
       String key = (String) iter.next();
       String name = (String) metadata.get(key);
       for (int kk=0; kk<array_rank; kk++) {
         if (array_dim_names[kk].equals(name)) {
           double[] coords = (double[]) select.get(key);
           start[kk] = (int) coords[0];
           count[kk] = (int) ((coords[1] - coords[0])/coords[2] + 1f);
           stride[kk] = (int) coords[2];
         }
       }
     }
     return subset;
   }

   public Object readArray(Object subset) throws Exception {
     Subset select = getIndexes((HashMap)subset);
     int[] start = select.getStart();
     int[] count = select.getCount();
     int[] stride = select.getStride();

     if (arrayType == Float.TYPE) {
       float[] range = reader.getFloatArray(arrayName, start, count, stride);
       return range;
     }
     else if (arrayType == Double.TYPE) {
       double[] range = reader.getDoubleArray(arrayName, start, count, stride);
       return range;
     }
     else if (arrayType == Short.TYPE) {
       short[] range = reader.getShortArray(arrayName, start, count, stride);
       return range;
     }
     else if (arrayType == Byte.TYPE) {
       byte[] range = reader.getByteArray(arrayName, start, count, stride);
       return range;
     }
     return null;
   }

   public MultiDimensionReader getReader() {
     return reader;
   }

   public Object getMetadata() {
     return metadata;
   }

   public abstract HashMap getDefaultSubset();

   String getArrayName() {
     return arrayName;
   }

}
