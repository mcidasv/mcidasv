package edu.wisc.ssec.mcidasv.data.hydra;

import java.lang.Class;

public interface MultiDimensionReader {

  public float[] getFloatArray(String name, int[] start, int[] count, int[] stride) throws Exception;
  public double[] getDoubleArray(String name, int[] start, int[] count, int[] stride) throws Exception;
  public short[] getShortArray(String name, int[] start, int[] count, int[] stride) throws Exception;
  public byte[] getByteArray(String name, int[] start, int[] count, int[] stride) throws Exception;

  public Class getArrayType(String name);

  public String[] getDimensionNames(String arrayName);

  public int[] getDimensionLengths(String arrayName);

  public HDFArray getGlobalAttribute(String attrName) throws Exception;

  public HDFArray getArrayAttribute(String arrayName, String attrName) throws Exception;

  public void close() throws Exception;
}
