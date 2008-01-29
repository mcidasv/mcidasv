package edu.wisc.ssec.mcidasv.data.hydra;

import visad.Data;
import visad.FlatField;
import visad.VisADException;
import visad.CoordinateSystem;
import visad.RealType;
import visad.RealTupleType;
import visad.SetType;
import visad.FunctionType;
import visad.Set;
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

   RealType rangeType;

   RangeProcessor rangeProcessor;

   public MultiDimensionAdapter() {
   }

   public MultiDimensionAdapter(MultiDimensionReader reader, HashMap metadata) {
     this.reader = reader;
     this.metadata = metadata;
     this.init();
   }

   //public abstract Data getData(Object subset) throws Exception;

   public abstract HashMap getDefaultSubset();

   public abstract Set makeDomain(Object subset) throws Exception;

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

   public FlatField getData(Object subset) throws Exception {
     Set domainSet = makeDomain(subset);
     return makeFlatField(domainSet, subset);
   }

   private FlatField makeFlatField(Set domainSet, float[][] range) throws VisADException, RemoteException {
     FlatField f_field = makeFlatField(domainSet);
     f_field.setSamples(range, false);
     return f_field;
   }

   private FlatField makeFlatField(Set domainSet, double[][] range) throws VisADException, RemoteException {
     FlatField f_field = makeFlatField(domainSet);
     f_field.setSamples(range, false);
     return f_field;
   }

   private FlatField makeFlatField(Set domainSet) throws VisADException, RemoteException {
     FlatField f_field = new FlatField(new FunctionType(((SetType)domainSet.getType()).getDomain(), rangeType), domainSet);
     return f_field;
   }

   public FlatField makeFlatField(Set domainSet, Object subset) throws Exception {
     FlatField f_field = null;

     Object range = readArray(subset);

     if (arrayType == Float.TYPE) {
       f_field = makeFlatField(domainSet, new float[][] {(float[])range});
     }
     else if (arrayType == Double.TYPE) {
       f_field = makeFlatField(domainSet, new double[][] {(double[])range});
     }
     else if (arrayType == Short.TYPE) {
       float[] float_range = processRange((short[])range, subset);
       f_field = makeFlatField(domainSet, new float[][] {float_range});
     }
                                                                                                                                                     
     return f_field;
   }
                                                                                                                                                     
   public float[] processRange(short[] range, Object subset) {
     return rangeProcessor.processRange(range, (HashMap)subset);
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

   String getArrayName() {
     return arrayName;
   }

   public RealType getRangeType() {
     return rangeType;
   }
   

}
