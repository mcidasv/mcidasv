/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Range;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/**
 * This file should be renamed - think about that Tommy.
 * This file needs to implement the same signatures NetCDFFile does,
 * but for aggregations of consecutive granules.
 * 
 * @author tommyj
 *
 */

public class GranuleAggregation implements MultiDimensionReader {
 
	private static final Logger logger = LoggerFactory.getLogger(GranuleAggregation.class);
	
	// this structure holds the NcML readers that get passed in 
   ArrayList<NetcdfFile> nclist = new ArrayList<NetcdfFile>();

   // this holds the MultiDimensionReaders, here NetCDFFile
   ArrayList<NetCDFFile> ncdfal = null;
   
   // need an ArrayList for each variable hashmap structure
   ArrayList<HashMap<String, Variable>> varMapList = new ArrayList<HashMap<String, Variable>>();
   ArrayList<HashMap<String, String[]>> varDimNamesList = new ArrayList<HashMap<String, String[]>>();
   ArrayList<HashMap<String, int[]>> varDimLengthsList = new ArrayList<HashMap<String, int[]>>();
   ArrayList<HashMap<String, Class>> varDataTypeList = new ArrayList<HashMap<String, Class>>();
   ArrayList<HashMap<String, Integer>> varGranuleRatiosList = new ArrayList<HashMap<String, Integer>>();

   // variable can have bulk array processor set by the application
   HashMap<String, RangeProcessor> varToRangeProcessor = new HashMap<String, RangeProcessor>();
   
   private int granuleCount = -1;
   private int granuleLength = -1;
   private String inTrackDimensionName = null;
   private String crossTrackDimensionName = null;

   public GranuleAggregation(ArrayList<NetCDFFile> ncdfal, int granuleLength, String inTrackDimensionName, String crossTrackDimensionName) throws Exception {
	   if (ncdfal == null) throw new Exception("No data: empty NPP aggregation object");
	   logger.trace("granule length: " + granuleLength + " inTrack: " + inTrackDimensionName);
	   this.granuleLength = granuleLength;
	   this.inTrackDimensionName = inTrackDimensionName;
	   this.crossTrackDimensionName = crossTrackDimensionName;
       this.ncdfal = ncdfal;
	   init(ncdfal);
   }
  
   public Class getArrayType(String array_name) {
     return varDataTypeList.get(0).get(array_name);
   }

   public String[] getDimensionNames(String array_name) {
	   logger.trace("GranuleAggregation.getDimensionNames, requested: " + array_name);
     return varDimNamesList.get(0).get(array_name);
   }

   public int[] getDimensionLengths(String array_name) {
	   logger.trace("GranuleAggregation.getDimensionLengths, requested: " + array_name);
	   int[] lengths = varDimLengthsList.get(0).get(array_name);
	   for (int i = 0; i < lengths.length; i++) {
		   logger.trace("Length: " + lengths[i]);
	   }
     return varDimLengthsList.get(0).get(array_name);
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

   public Object getArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     return readArray(array_name, start, count, stride);
   }

   public HDFArray getGlobalAttribute(String attr_name) throws Exception {
     throw new Exception("GranuleAggregation.getGlobalAttributes: Unimplemented");
   }

   public HDFArray getArrayAttribute(String array_name, String attr_name) throws Exception {
	   Variable var = varMapList.get(0).get(array_name);
	   if (var == null) return null;
	   
	   Attribute attr = var.findAttribute(attr_name);
	   if (attr == null) return null;
	   
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
	   // close each NetCDF file
	   for (NetcdfFile n : nclist) {
		   n.close();
	   }
   }

   private void init(ArrayList<NetCDFFile> ncdfal) throws Exception {
	   
	   logger.debug("init in...");
	   // make a NetCDFFile object from the NcML for each granule
	   for (NetCDFFile n : ncdfal) {
		   logger.debug("loading another NetCDF file from NcML...");
		   NetcdfFile ncfile = n.getNetCDFFile();
		   nclist.add(ncfile);
	   }
	   
	   granuleCount = nclist.size();
	   logger.debug("Granule count: " + granuleCount);
	   
	   for (int ncIdx = 0; ncIdx < nclist.size(); ncIdx++) {
		   
		   NetcdfFile ncfile = nclist.get(ncIdx);
		   
		   HashMap<String, Variable> varMap = new HashMap<String, Variable>();
		   HashMap<String, String[]> varDimNames = new HashMap<String, String[]>();
		   HashMap<String, int[]> varDimLengths = new HashMap<String, int[]>();
		   HashMap<String, Class> varDataType = new HashMap<String, Class>();
		   HashMap<String, Integer> varGranuleRatios = new HashMap<String, Integer>();
		   
		   Iterator varIter = ncfile.getVariables().iterator();
		   int varInTrackIndex = -1;
		   while (varIter.hasNext()) {
			   Variable var = (Variable) varIter.next();
			   
			   logger.debug("Working on variable: " + var.getName());
			   
			   if (var instanceof Structure) {
				   analyzeStructure((Structure) var, varMap, varDimNames, varDimLengths, varDataType);
				   continue;
			   }

			   int rank = var.getRank();
			   
			   // bypass any less-than-2D variables for now...
			   if (rank < 2) {
				   logger.debug("Skipping 1D variable: " + var.getName());
				   continue;
			   }
			   
			   String varName = var.getName();
			   varMap.put(var.getName(), var);
			   Iterator dimIter = var.getDimensions().iterator();
			   String[] dimNames = new String[rank];
			   int[] dimLengths = new int[rank];
			   int cnt = 0;
			   boolean notDisplayable = false;
			   while (dimIter.hasNext()) {
				   Dimension dim = (Dimension) dimIter.next();
				   String s = dim.getName();
				   logger.trace("DIMENSION name: " + s);
				   if ((s != null) && (!s.isEmpty())) {
					   if ((! s.equals(inTrackDimensionName)) && 
							   ((! s.startsWith("Band")) && (cnt == 0)) &&
							   (! var.getName().endsWith("Latitude")) &&
							   (! var.getName().endsWith("Longitude")) &&
							   (! s.equals(crossTrackDimensionName))) {
						   notDisplayable = true;
						   break;
					   }
				   }
				   String dimName = dim.getName();
				   logger.trace("GranuleAggregation init, variable: " + varName + ", dimension name: " + dimName + ", length: " + dim.getLength());
				   if (dimName == null) dimName = "dim" + cnt;
				   dimNames[cnt] = dimName;
				   dimLengths[cnt] = dim.getLength();
				   cnt++;
			   }
			   
			   // skip to next variable if it's not displayable data
			   if (notDisplayable) continue;
			   
			   varInTrackIndex = getInTrackIndex(var);
			   logger.debug("Found in-track index of: " + varInTrackIndex);
			   
			   if (varInTrackIndex < 0) {
				   logger.debug("Skipping variable with unknown dimension: " + var.getName());
				   continue;
			   }
			   
			   // store the ratio of this variable's in-track length to the granule length
			   varGranuleRatios.put(varName, new Integer(granuleLength / dimLengths[varInTrackIndex]));
			   
			   dimLengths[varInTrackIndex] = dimLengths[varInTrackIndex] * granuleCount;
			   
			   varDimNames.put(varName, dimNames);
			   varDimLengths.put(varName, dimLengths);
			   varDataType.put(varName, var.getDataType().getPrimitiveClassType());
		   }
		   
		   // add the new hashmaps to our enclosing lists
		   varMapList.add(varMap);
		   varDimNamesList.add(varDimNames);
		   varDimLengthsList.add(varDimLengths);
		   varDataTypeList.add(varDataType);
		   varGranuleRatiosList.add(varGranuleRatios);
	   }
   }
   
   /**
    * Based on the names of the variable dimensions, determine the in-track index
    * @param dimNames names of dimensions - should match static strings in relevant classes
    * @return correct index (0 or greater), or -1 if error
    */
   
   private int getInTrackIndex(Variable v) {
	   
	   int index = -1;
	   boolean is2D = false;
	   boolean is3D = false;
	   
	   String inTrackName = null;
	    
	   // typical sanity check
	   if (v == null) return index;
	   logger.debug("getInTrackIndex called for variable: " + v.getName());
	   
	   // lat/lon vars have different dimension names
	   if ((v.getName().endsWith("Latitude")) || (v.getName().endsWith("Longitude"))) {
		   if (v.getName().startsWith("All_Data")) {
			   inTrackName = inTrackDimensionName;
		   } else {
			   inTrackName = "2*nscans";
		   }
	   } else {
		   inTrackName = inTrackDimensionName;
	   }
	   // pull out the dimensions
	   List<Dimension> dList = v.getDimensions();
	   
	   // right now, we only handle 2D and 3D variables.
	   // TJJ XXX it does get trickier, and we will have to expand this
	   // to deal with for example CrIS data...
	   int numDimensions = dList.size();
	   logger.debug("Number of dimensions: " + numDimensions);
	   if ((numDimensions == 2) || (numDimensions == 3)) {
		   if (numDimensions == 2) is2D = true;
		   if (numDimensions == 3) is3D = true;
	   } else {
		   return index;
	   }
	   
	   // if the data is 2D, we use the SwathAdapter class,
	   // if 3D, we use the SpectrumAdapter class
	   for (int i = 0; i < numDimensions; i++) {
		   if (is2D) {
			   // XXX TJJ - if empty name, in-track index is 0
			   if (dList.get(i).getName().isEmpty()) {
				   logger.debug("WARNING: Empty dimension name!, assuming in-track dim is 0");
				   return 0;
			   }
			   logger.debug("Comparing: " + dList.get(i).getName() + " with: " + inTrackName);
			   if (dList.get(i).getName().equals(inTrackName)) {
				   index = i;
				   break;
			   }
		   }
		   if (is3D) {
			   logger.debug("Comparing: " + dList.get(i).getName() + " with: " + inTrackName);
			   if (dList.get(i).getName().equals(inTrackName)) {
				   index = i;
				   break;
			   }
		   }
	   }
	   
	   // hopefully we found the right one
	   return index;
   }

   void analyzeStructure(Structure var, HashMap<String, Variable> varMap, 
		   HashMap<String, String[]> varDimNames,
		   HashMap<String, int[]> varDimLengths,
		   HashMap<String, Class> varDataType) throws Exception {
	   if ((var.getShape()).length == 0) {
		   return;
	   }
	   String varName = var.getName();
	   String[] dimNames = new String[2];
	   int[] dimLengths = new int[2];
	   int cnt = 0;
	   dimLengths[0] = (var.getShape())[0];
	   dimNames[0] = "dim"+cnt;

	   cnt++;
	   StructureData sData = var.readStructure(0);
	   List memList = sData.getMembers();
	   dimLengths[1] = memList.size();
	   dimNames[1] = "dim"+cnt;

	   varDimNames.put(varName, dimNames);
	   varDimLengths.put(varName, dimLengths);
	   varMap.put(var.getName(), var);

	   StructureMembers sMembers = sData.getStructureMembers();
	   Object obj = sData.getScalarObject(sMembers.getMember(0));
	   varDataType.put(varName, obj.getClass());
   }
   
   private synchronized Object readArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
	   
	   // how many dimensions are we dealing with
	   int dimensionCount = start.length;
	   
	   // pull out a representative variable so we can determine which index is in-track
	   Variable vTmp = varMapList.get(0).get(array_name);
	   int vInTrackIndex = getInTrackIndex(vTmp);
	   
	   int vGranuleRatio = varGranuleRatiosList.get(0).get(array_name);
	   int vGranuleLength = granuleLength / vGranuleRatio;
	   
	   logger.trace("READING: " + array_name + ", INTRACKINDEX: " + vInTrackIndex +
			   ", RATIO: " + vGranuleRatio);
	   
	   // which granules will we be dealing with?
	   int loGranuleId = start[vInTrackIndex] / vGranuleLength;
	   int hiGranuleId = (start[vInTrackIndex] + ((count[vInTrackIndex] - 1) * stride[vInTrackIndex])) / vGranuleLength;
	   
	   // next, we break out the offsets, counts, and strides for each granule
	   int granuleSpan = hiGranuleId - loGranuleId + 1;
	   logger.trace("readArray req, loGran: " + loGranuleId + ", hiGran: " + 
			   hiGranuleId + ", granule span: " + granuleSpan + ", dimCount: " + dimensionCount);
	   for (int i = 0; i < dimensionCount; i++) {
		   logger.trace("start[" + i + "]: " + start[i]);
		   logger.trace("count[" + i + "]: " + count[i]);
		   logger.trace("stride[" + i + "]: " + stride[i]);
	   }
	   int [][] startSet = new int [granuleSpan][dimensionCount];
	   int [][] countSet = new int [granuleSpan][dimensionCount];
	   int [][] strideSet = new int [granuleSpan][dimensionCount];
	   int countSubtotal = 0;
	   
	   // this part is a little tricky - set the values for each granule we need to access for this read
	   int granuleNumber = loGranuleId;
	   for (int i = 0; i < granuleSpan; i++) {
		   granuleNumber++;
		   for (int j = 0; j < dimensionCount; j++) {
			   // for all indeces other than the in-track index, the numbers match what was passed in
			   if (j != vInTrackIndex) {
				   startSet[i][j] = start[j];
				   countSet[i][j] = count[j] * stride[j];
				   strideSet[i][j] = stride[j];  
			   } else {
				   // for the in-track index, it's not so easy...
				   // for first granule, start is what's passed in
				   if (i == 0) {
					   startSet[i][j] = start[j] % vGranuleLength;
				   } else {
					   startSet[i][j] = ((vGranuleLength * granuleNumber) - start[j]) % stride[j];
				   }
				   // counts may be different for start, end, and middle granules
				   if (i == 0) {
					   // is this the first and only granule?
					   if (granuleSpan == 1) {
						   countSet[i][j] = count[j] * stride[j];
					   // or is this the first of multiple granules...
					   } else {
						   if (((vGranuleLength * granuleNumber) - start[j]) < (count[j] * stride[j])) {	
							   countSet[i][j] = ((vGranuleLength * granuleNumber) - start[j]);
						   } else {
							   countSet[i][j] = count[j] * stride[j];
						   }
						   countSubtotal += countSet[i][j];
					   }
				   } else {
					   // middle granules
					   if (i < (granuleSpan - 1)) {
						   countSet[i][j] = vGranuleLength;
						   countSubtotal += countSet[i][j];
					   } else {
						   // the end granule
						   countSet[i][j] = (count[j] * stride[j]) - countSubtotal;
					   }
				   }
				   // luckily, stride never changes
				   strideSet[i][j] = stride[j];
			   }
		   }
	   }
	   
	   int totalLength = 0;
	   int rangeListCount = 0;
	   ArrayList<Array> arrayList = new ArrayList<Array>();
	   for (int granuleIdx = 0; granuleIdx < granuleCount; granuleIdx++) {
		   if ((granuleIdx >= loGranuleId) && (granuleIdx <= hiGranuleId)) {
			   Variable var = varMapList.get(loGranuleId + (granuleIdx-loGranuleId)).get(array_name);

			   if (var instanceof Structure) {
				   // what to do here?
			   } else {
				   ArrayList<Range> rangeList = new ArrayList<Range>();
				   for (int dimensionIdx = 0; dimensionIdx < dimensionCount; dimensionIdx++) {
					   logger.trace("Creating new Range: " + startSet[rangeListCount][dimensionIdx] +
							   ", " + (startSet[rangeListCount][dimensionIdx] + countSet[rangeListCount][dimensionIdx] - 1) + ", " + strideSet[rangeListCount][dimensionIdx]);
					   Range range = new Range(
							   startSet[rangeListCount][dimensionIdx], 
							   startSet[rangeListCount][dimensionIdx] + countSet[rangeListCount][dimensionIdx] - 1,
							   strideSet[rangeListCount][dimensionIdx]
					   );
					   rangeList.add(dimensionIdx, range);
				   }
				   rangeListCount++;
				   logger.trace("reading range list from variable: " + var.getName());
				   List<Dimension> dl = var.getDimensions();
				   for (Dimension d : dl) {
					   logger.trace("Dimension name: " + d.getName() + ", length: " + d.getLength());
				   }
				   Array subarray = var.read(rangeList);
				   //dataType = subarray.getElementType();
				   totalLength += subarray.getSize();
				   arrayList.add(subarray);
			   }
			   // put in an empty ArrayList placeholder to retain a slot for each granule
		   } else {
			   Array emptyArray = null;
			   arrayList.add(emptyArray);
		   }
	   }
	   
	   // last, concatenate the individual NetCDF arrays pulled out 

	   Class outType;
	   Class arrayType = getArrayType(array_name);
	   RangeProcessor rngProcessor = varToRangeProcessor.get(array_name);
	   if (rngProcessor == null) {
		   outType = getArrayType(array_name);
	   }
	   else {
		   outType = java.lang.Float.TYPE;
	   }
	   Object o = java.lang.reflect.Array.newInstance(outType, totalLength);
           
	   int destPos = 0;
           int granIdx = 0;

    	   for (Array a : arrayList) {
    		   if (a != null) {
    			   Object primArray = a.copyTo1DJavaArray();
    			   primArray = processArray(array_name, arrayType, granIdx, primArray, rngProcessor);
    			   System.arraycopy(primArray, 0, o, destPos, (int) a.getSize());
    			   destPos += a.getSize();
    		   }
    		   granIdx++;
    	   }


	   return o;
   }
   
   public HashMap getVarMap() {
     return varMapList.get(0);
   }

   public ArrayList<NetCDFFile> getReaders() {
     return this.ncdfal;
   }

   /* pass individual granule pieces just read from dataset through the RangeProcessor */
   private Object processArray(String array_name, Class arrayType, int granIdx, Object values, RangeProcessor rngProcessor) {
     if (rngProcessor == null) {
       return values;
     }
     else {
        ((AggregationRangeProcessor)rngProcessor).setIndex(granIdx);

        Object outArray = null;
        if (arrayType == Short.TYPE) {
           outArray = rngProcessor.processRange((short[])values, null);
        } else if (arrayType == Byte.TYPE) {
           outArray = rngProcessor.processRange((byte[])values, null);
        } else if (arrayType == Float.TYPE) {
           outArray = rngProcessor.processRange((float[])values, null);
        } else if (arrayType == Double.TYPE) {
           outArray = rngProcessor.processRange((double[])values, null);
        }
        return outArray;
     }
   }

   /* Application can supply a RangeProcessor for an variable 'arrayName' */
   public void addRangeProcessor(String arrayName, RangeProcessor rangeProcessor) {
     varToRangeProcessor.put(arrayName, rangeProcessor);
   }
}
