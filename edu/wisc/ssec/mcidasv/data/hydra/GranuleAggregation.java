/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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
import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.data.QualityFlag;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.Range;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/**
 * Provides a view and operations on a set of contiguous data granules as if they 
 * were a single granule.
 * 
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
   ArrayList<HashMap<String, Class>> varDataTypeList = new ArrayList<HashMap<String, Class>>();

   // map of granule index and granule in-track length for each variable
   HashMap<String, HashMap<Integer, Integer>> varGranInTrackLengths = new HashMap<String, HashMap<Integer, Integer>>();
   HashMap<String, int[]> varAggrDimLengths = new HashMap<String, int[]>();
   
   // this object is used to handle granules like VIIRS Imagery EDRs, where scan
   // gaps of varying sizes and locations in the granule must be removed.  If 
   // present, an initial read with these "cut" ranges will be done before subsetting
   HashMap<Integer, ArrayList<Range>> granCutRanges = new HashMap<Integer, ArrayList<Range>>();
   HashMap<Integer, Integer> granCutScans = new HashMap<Integer, Integer>();
   
   // except quality flags - only need one hashmap per aggregation
   // it maps the broken out variable name back to the original packed variable name
   HashMap<String, QualityFlag> qfMap = null;

   // variable can have bulk array processor set by the application
   HashMap<String, RangeProcessor> varToRangeProcessor = new HashMap<String, RangeProcessor>();
   
   private int granuleCount = -1;
   private String inTrackDimensionName = null;
   private String inTrackGeoDimensionName = null;
   private String crossTrackDimensionName = null;
   private LinkedHashSet<String> products;
   private String origName = null;
   private boolean isEDR = false;

   public GranuleAggregation(ArrayList<NetCDFFile> ncdfal, LinkedHashSet<String> products, 
		   String inTrackDimensionName, String inTrackGeoDimensionName, 
		   String crossTrackDimensionName, boolean isEDR) throws Exception {
	   if (ncdfal == null) throw new Exception("No data: empty Suomi NPP aggregation object");
	   this.inTrackDimensionName = inTrackDimensionName;
	   this.crossTrackDimensionName = crossTrackDimensionName;
	   this.inTrackGeoDimensionName = inTrackGeoDimensionName;
       this.ncdfal = ncdfal;
       this.products = products;
       this.isEDR = isEDR;
	   init(ncdfal);
   }
   
   public GranuleAggregation(ArrayList<NetCDFFile> ncdfal, LinkedHashSet<String> products, 
		   String inTrackDimensionName, String inTrackGeoDimensionName, 
		   String crossTrackDimensionName) throws Exception {
	   this(ncdfal, products, inTrackDimensionName, inTrackGeoDimensionName, crossTrackDimensionName, false);
   }
   
   public GranuleAggregation(ArrayList<NetCDFFile> ncdfal, LinkedHashSet<String> products, 
		   String inTrackDimensionName, String crossTrackDimensionName) throws Exception {
       this(ncdfal, products, inTrackDimensionName, inTrackDimensionName, crossTrackDimensionName, false);
   }
   
   public GranuleAggregation(ArrayList<NetCDFFile> ncdfal, LinkedHashSet<String> products, 
		   String inTrackDimensionName, String crossTrackDimensionName, boolean isEDR) throws Exception {
        this(ncdfal, products, inTrackDimensionName, inTrackDimensionName, crossTrackDimensionName, isEDR);
   }

   public Class getArrayType(String array_name) {
	   array_name = mapNameIfQualityFlag(array_name);
	   return varDataTypeList.get(0).get(array_name);
   }

   public String[] getDimensionNames(String array_name) {
	   array_name = mapNameIfQualityFlag(array_name);
	   return varDimNamesList.get(0).get(array_name);
   }

   public int[] getDimensionLengths(String array_name) {
	   array_name = mapNameIfQualityFlag(array_name);
	   logger.debug("For var " + array_name + ", sending back dim len: " + varAggrDimLengths.get(array_name));
	   return varAggrDimLengths.get(array_name);
   }

   private String mapNameIfQualityFlag(String array_name) {
	   // only applies if name is from a packed quality flag
	   // we pull data from the "mapped" variable name, a packed byte
	   if (qfMap != null) {
		   logger.debug("mapNameIfQualityFlag, checking key: " + array_name);
		   if (qfMap.containsKey(array_name)) {
			   origName = array_name;
			   QualityFlag qf = qfMap.get(array_name);
			   String mappedName = qf.getPackedName();
			   logger.debug("Mapped to: " + mappedName);
			   return mappedName;
		   }
	   }
	   return array_name;
   }

   /**
    * @return the isEDR
    */
   public boolean isEDR() {
	   return isEDR;
   }

   /**
    * @param isEDR the isEDR to set
    */
   public void setEDR(boolean isEDR) {
	   this.isEDR = isEDR;
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

       // All files do NOT have the same structure, so need to look at each ncfile
	   // For ex, some MODIS granules have slightly different in-track and along-track 
	   // lengths
	   
	   NetcdfFile ncfile = null;
	   for (int ncIdx = 0; ncIdx < nclist.size(); ncIdx++) {
		   
		   // good place to initialize the cut Range ArrayList for each granule
		   Integer granuleIndex = new Integer(ncIdx);
		   ArrayList<Range> al = new ArrayList<Range>();
		   granCutRanges.put(granuleIndex, al);
		   int cutScanCount = 0;
		   
		   ncfile = nclist.get(ncIdx); 
		   
		   Iterator<Variable> varIter = ncfile.getVariables().iterator();
		   while (varIter.hasNext()) {
			   Variable var = varIter.next();
			   logger.debug("Variable " + var.getShortName() + ", Rank: " + var.getRank());
			   varAggrDimLengths.put(var.getFullName(), new int[var.getRank()]);
			   varGranInTrackLengths.put(var.getFullName(), new HashMap<Integer, Integer>()); 
			   
			   // Here, let's try to check the data for EDR fill lines
			   // and if found, try to handle it by simply adjusting the dimensions
			   // for this granule.  Sound like a plan?  We'll see...
			   
			   if (isEDR) {
				   
				   logger.debug("IS an EDR, need to look for fill scans...");
				   // look through lat grid, look for missing scans
				   String varName = var.getShortName();
				   if ((varName.endsWith("Latitude")) || (varName.endsWith("Latitude_TC"))){
					   // iterate through the scan lines, looking for fill lines
					   // NOTE: we only need to check the first column! so set
					   // up an appropriate Range to cut the read down significantly
					   int[] shape = var.getShape();
					   ArrayList<Range> alr = new ArrayList<Range>();
					   alr.add(new Range(0, shape[0] - 1, 1));
					   alr.add(new Range(0, 1, 1));
					   Array a = var.read(alr);
					   logger.debug("Lat shape: " + shape[0] + " by " + shape[1]);
					   int scanLength = shape[1];
					   Index index = a.getIndex();
					   float fVal = 0.0f;

					   int rangeOffset = 1;
					   int rangeCount = 0;
					   boolean prvScanWasCut = false;
					   boolean needClosingRange = false;
					   boolean hadCutRanges = false;
					   boolean someMissing = false;

					   for (int i = 0; i < shape[0]; i++) {

						   someMissing = false;
						   fVal = a.getFloat(index.set(i, 0));
						   if (fVal < -90.0f) {
							   someMissing = true;
						   }

						   if (someMissing) {
							   hadCutRanges = true;
							   cutScanCount++;
							   logger.debug("Found a cut scan " + (i + 1)
									   + ", last val: " + fVal);
							   if ((prvScanWasCut) || (i == 0)) {
								   if (i == 0) {
									   rangeOffset = 1;
								   } else {
									   rangeOffset = i + 2;
								   }
							   } else {
								   try {
									   // We are using 2D ranges
									   logger.debug("Adding Range: " + rangeOffset
											   + ", " + i + ", 1");
									   al.add(new Range(rangeOffset, i, 1));
									   logger.debug("Adding Range: " + 1 + ", "
											   + (scanLength - 1) + ", 1");
									   al.add(new Range(0, scanLength - 1, 1));
								   } catch (Exception e) {
									   e.printStackTrace();
								   }
								   rangeCount = 0;
								   rangeOffset = i + 1;
							   }
							   prvScanWasCut = true;
						   } else {
							   prvScanWasCut = false;
							   rangeCount += scanLength;
						   }

						   // check to see if closing Range needed, good data at end
						   if ((! prvScanWasCut) && (i == (scanLength - 1))) {
							   needClosingRange = true;
						   }
					   }

					   if (needClosingRange) {
						   // We are using 2D ranges
						   al.add(new Range(rangeOffset, rangeOffset + shape[0]
								   - 1, 1));
						   al.add(new Range(0, scanLength - 1, 1));
						   logger.debug("Adding closing cut Range, offs: "
								   + rangeOffset + ", len: " + rangeCount);
					   }

					   // if only one contiguous range, process as a normal clean granule
					   if (! hadCutRanges) {
						   al.clear();
					   }

					   granCutScans.put(granuleIndex, new Integer(cutScanCount));
					   logger.debug("Total scans cut this granule: "
							   + cutScanCount);

				   }
			   } else {
				   granCutScans.put(granuleIndex, new Integer(0));
				   logger.debug("is NOT an EDR, no need to check for fill scans...");
			   }
		   }
	   }
	   
	   for (int ncIdx = 0; ncIdx < nclist.size(); ncIdx++) {
		   
		   ncfile = nclist.get(ncIdx);
		   
		   HashMap<String, Variable> varMap = new HashMap<String, Variable>();
		   HashMap<String, String[]> varDimNames = new HashMap<String, String[]>();
		   HashMap<String, Class> varDataType = new HashMap<String, Class>();
		   
		   Iterator<Variable> varIter = ncfile.getVariables().iterator();
		   int varInTrackIndex = -1;
		   while (varIter.hasNext()) {
			   Variable var = (Variable) varIter.next();
			   
			   logger.debug("Working on variable: " + var.getFullName());
			   
			   boolean foundProduct = false;
			   for (String s : products) {
				   if (s.contains(var.getFullName())) {
					   logger.debug("Valid product: " + var.getFullName());
					   foundProduct = true;
					   break;
				   }
			   }
			   
			   if (! foundProduct) {
				   logger.debug("Skipping variable: " + var.getFullName());
				   continue;
			   }
			   
			   if (var instanceof Structure) {
				   	// simply skip these, applicable only to IASI far as I know
				   	continue;
			   }

			   int rank = var.getRank();
			   
			   // bypass any less-than-2D variables for now...
			   if (rank < 2) {
				   logger.debug("Skipping 1D variable: " + var.getFullName());
				   continue;
			   }
			   
			   String varName = var.getFullName();
			   varMap.put(varName, var);
			   Iterator<Dimension> dimIter = var.getDimensions().iterator();
			   String[] dimNames = new String[rank];
			   int[] dimLengths = new int[rank];
			   int cnt = 0;
			   boolean notDisplayable = false;
			   varInTrackIndex = getInTrackIndex(var);

			   while (dimIter.hasNext()) {
				   Dimension dim = dimIter.next();
				   String s = dim.getShortName();
				   if ((s != null) && (!s.isEmpty())) {
					   if ((! s.equals(inTrackDimensionName)) && 
							   ((! s.startsWith("Band")) && (cnt == 0)) &&
							   (! varName.endsWith("Latitude")) &&
							   (! varName.endsWith("Latitude_TC")) &&
							   (! varName.endsWith("Longitude")) &&
							   (! varName.endsWith("Longitude_TC")) &&
							   (! s.equals(crossTrackDimensionName))) {
						   notDisplayable = true;
						   break;
					   }
				   }
				   String dimName = dim.getShortName();
				   logger.debug("GranuleAggregation init, variable: " + varName + ", dimension name: " + dimName + ", length: " + dim.getLength());
				   if (dimName == null)  dimName = "dim" + cnt;
				   if (dimName.isEmpty()) {
					   dimName = "dim" + cnt;
				   }
				   dimNames[cnt] = dimName;
				   dimLengths[cnt] = dim.getLength();
				   cnt++;
			   }
			   
			   // skip to next variable if it's not displayable data
			   if (notDisplayable) continue;
			   
			   // adjust in-track dimension if needed (scans were cut)
			   int cutScans = granCutScans.get(ncIdx);
			   dimLengths[varInTrackIndex] = dimLengths[varInTrackIndex] - cutScans;
			   
			   // XXX TJJ - can below block go away?  Think so...
			   int[] aggrDimLengths = varAggrDimLengths.get(varName);
			   for (int i = 0; i < rank; i++) {
				   if (i == varInTrackIndex) {
					   aggrDimLengths[i] += dimLengths[i];
				   } else {
					   aggrDimLengths[i] = dimLengths[i];
				   }
			   }
			   
			   varDimNames.put(varName, dimNames);
			   varDataType.put(varName, var.getDataType().getPrimitiveClassType());

			   if (varInTrackIndex < 0) {
				   logger.debug("Skipping variable with unknown dimension: " + var.getFullName());
				   continue;
			   }

			   HashMap<Integer, Integer> granIdxToInTrackLen = varGranInTrackLengths.get(varName);
			   granIdxToInTrackLen.put(ncIdx, new Integer(dimLengths[varInTrackIndex]));
			   
			   dimLengths[varInTrackIndex] = dimLengths[varInTrackIndex] * granuleCount;
			   varDataType.put(varName, var.getDataType().getPrimitiveClassType());
		   }
		   
		   // add the new hashmaps to our enclosing lists
		   varMapList.add(varMap);
		   varDimNamesList.add(varDimNames);
		   varDataTypeList.add(varDataType);
		   
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
	   logger.debug("getInTrackIndex called for variable: " + v.getShortName());
	   
	   // lat/lon vars have different dimension names
	   if ((v.getFullName().endsWith("Latitude")) || 
			   (v.getFullName().endsWith("Latitude_TC")) ||
			   (v.getFullName().endsWith("Longitude")) ||
			   (v.getFullName().endsWith("LongitudeTC"))) {
		   if (v.getFullName().startsWith("All_Data")) {
			   inTrackName = inTrackDimensionName;
		   } else {
			   inTrackName = inTrackGeoDimensionName;
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
	   
	   // the only 4D data right now is CrIS, return 0
	   if (numDimensions == 4) return 0;
	   
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
			   if ((dList.get(i).getShortName() == null) || (dList.get(i).getShortName().isEmpty())) {
				   logger.warn("Empty dimension name!, assuming in-track dim is 0");
				   return 0;
			   }
			   if (dList.get(i).getShortName().equals(inTrackName)) {
				   index = i;
				   break;
			   }
		   }
		   if (is3D) {
			   // XXX TJJ - if empty name, in-track index is 0
			   if ((dList.get(i).getShortName() == null) || (dList.get(i).getShortName().isEmpty())) {
				   logger.warn("Empty dimension name!, assuming in-track dim is 0");
				   return 0;
			   }
			   if (dList.get(i).getShortName().equals(inTrackName)) {
				   index = i;
				   break;
			   }
		   }
	   }
	   
	   // hopefully we found the right one
	   return index;
   }
   
   private synchronized Object readArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
	   
	   array_name = mapNameIfQualityFlag(array_name);
	   // how many dimensions are we dealing with
	   int dimensionCount = start.length;
	   
	   // pull out a representative variable so we can determine which index is in-track
	   Variable vTmp = varMapList.get(0).get(array_name);
	   int vInTrackIndex = getInTrackIndex(vTmp);
	   
	   int loGranuleId = 0;
	   int hiGranuleId = 0;

	   HashMap<Integer, Integer> granIdxToInTrackLen = varGranInTrackLengths.get(array_name);
	   int numGrans = granIdxToInTrackLen.size();

	   int[] vGranuleLengths = new int[numGrans];
	   for (int k = 0; k < numGrans; k++) {
		   vGranuleLengths[k] = granIdxToInTrackLen.get(k);
		   logger.debug("readArray, gran len: " + vGranuleLengths[k] + ", scans cut: " + granCutScans.get(k));
	   }

	   int strt = start[vInTrackIndex];
	   int stp = strt + (count[vInTrackIndex] - 1) * stride[vInTrackIndex];
	   int cnt = 0;
	   for (int k = 0; k < numGrans; k++) {
		   int granLen = granIdxToInTrackLen.get(k);
		   cnt += granLen;
		   if (strt < cnt) {
			   loGranuleId = k;
			   break;
		   }
	   }

	   cnt = 0;
	   for (int k = 0; k < numGrans; k++) {
		   int granLen = granIdxToInTrackLen.get(k);
		   cnt += granLen;
		   if (stp < cnt) {
			   hiGranuleId = k;
			   break;
		   }
	   }
	   logger.debug("loGranuleId: " + loGranuleId);
	   logger.debug("hiGranuleId: " + hiGranuleId);
	   

	   // next, we break out the offsets, counts, and strides for each granule
	   int granuleSpan = hiGranuleId - loGranuleId + 1;
	   
	   logger.debug("readArray req, loGran: " + loGranuleId + ", hiGran: " + 
			   hiGranuleId + ", granule span: " + granuleSpan + ", dimCount: " + dimensionCount);
	   
	   for (int i = 0; i < dimensionCount; i++) {
		   logger.debug("start[" + i + "]: " + start[i]);
		   logger.debug("count[" + i + "]: " + count[i]);
		   logger.debug("stride[" + i + "]: " + stride[i]);
	   }

	   int [][] startSet = new int [granuleSpan][dimensionCount];
	   int [][] countSet = new int [granuleSpan][dimensionCount];
	   int [][] strideSet = new int [granuleSpan][dimensionCount];
	   int countSubtotal = 0;

       int inTrackTotal = 0;
       for (int i = 0; i < loGranuleId; i++) {
           inTrackTotal += vGranuleLengths[i];
       }
	   
	   // this part is a little tricky - set the values for each granule we need to access for this read
	   for (int i = 0; i < granuleSpan; i++) {
           inTrackTotal += vGranuleLengths[loGranuleId+i];
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
					   startSet[i][j] = start[j] - (inTrackTotal - vGranuleLengths[loGranuleId]);
				   } else {
					   startSet[i][j] = (inTrackTotal - start[j]) % stride[j];
					   // TJJ Sep 2013, zero-base starts that offset into subsequent granules
					   if (startSet[i][j] > 0) {
						   startSet[i][j]--;
					   }
				   }
				   // counts may be different for start, end, and middle granules
				   if (i == 0) {
					   // is this the first and only granule?
					   if (granuleSpan == 1) {
						   countSet[i][j] = count[j] * stride[j];
					   // or is this the first of multiple granules...
					   } else {
						   if ((inTrackTotal - start[j]) < (count[j] * stride[j])) {	
                                                           countSet[i][j] = inTrackTotal - start[j];
						   } else {
							   countSet[i][j] = count[j] * stride[j];
						   }
						   countSubtotal += countSet[i][j];
					   }
				   } else {
					   // middle granules
					   if (i < (granuleSpan - 1)) {
						   countSet[i][j] = vGranuleLengths[loGranuleId+i];
						   countSubtotal += countSet[i][j];
					   } else {
						   // the end granule
						   countSet[i][j] = (count[j] * stride[j]) - countSubtotal;
						   // XXX TJJ - limiting count to valid numbers here, why??
						   // need to revisit, see why this condition manifests
						   if (countSet[i][j] > (vGranuleLengths[loGranuleId+i] - startSet[i][j])) 
							   countSet[i][j] = vGranuleLengths[loGranuleId+i] - startSet[i][j];
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
					   logger.debug("Creating new Range: " + startSet[rangeListCount][dimensionIdx] +
							   ", " + (startSet[rangeListCount][dimensionIdx] + countSet[rangeListCount][dimensionIdx] - 1) + ", " + strideSet[rangeListCount][dimensionIdx]);
					   Range range = new Range(
							   startSet[rangeListCount][dimensionIdx], 
							   startSet[rangeListCount][dimensionIdx] + countSet[rangeListCount][dimensionIdx] - 1,
							   strideSet[rangeListCount][dimensionIdx]
					   );
					   rangeList.add(dimensionIdx, range);
				   }
				   rangeListCount++;
				   
				   // If there were chunks of fill data to remove...
				   ArrayList<Range> al = granCutRanges.get(new Integer(granuleIdx));
				   if (! al.isEmpty()) {
					   ArrayList<Variable> varChunks = new ArrayList<Variable>();
					   for (int rangeCount = 0; rangeCount < al.size(); rangeCount+=2) {
						   ArrayList<Range> rl = new ArrayList<Range>();
						   rl.add(al.get(rangeCount));
						   rl.add(al.get(rangeCount + 1));
						   varChunks.add(var.section(rl));
					   }

					   int [] newShape = var.getShape();
					   int cutScans = granCutScans.get(granuleIdx);
					   newShape[0] = newShape[0] - cutScans;
					   logger.debug("New Shape: " + newShape[0] + ", " + newShape[1]);
					   Array single = Array.factory(var.getDataType(), newShape);

					   // now read variable chunk data into single contiguous array
					   int idx = 0;
					   for (Variable v : varChunks) {
						   Array data = v.read();
						   int [] tmpShape = v.getShape();
						   for (int tIdx = 0; tIdx < tmpShape.length; tIdx++) {
							   logger.debug("Shape[" + tIdx + "]: " + tmpShape[tIdx]);
						   }
						   IndexIterator ii = data.getIndexIterator();
						   while (ii.hasNext()) {
							   single.setFloat(idx, ii.getFloatNext());
							   idx++;
						   }
					   }

					   // finally, apply subset ranges
					   Array subarray = single.section(rangeList);
					   totalLength += subarray.getSize();
					   arrayList.add(subarray);
					   logger.debug("Size of final data array: " + subarray.getSize());

				   } else {
					   Array subarray = var.read(rangeList);
					   totalLength += subarray.getSize();
					   arrayList.add(subarray);
				   }
				   
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
			   primArray = processArray(array_name, arrayType, granIdx, primArray, rngProcessor, start, count);
			   System.arraycopy(primArray, 0, o, destPos, (int) a.getSize());
			   destPos += a.getSize();
		   }
		   granIdx++;
	   }
       
	   return o;
   }
   
   /**
    * @param qfMap the qfMap to set
    */
   public void setQfMap(HashMap<String, QualityFlag> qfMap) {
	   this.qfMap = qfMap;
   }

   public HashMap getVarMap() {
	   return varMapList.get(0);
   }

   public ArrayList<NetCDFFile> getReaders() {
     return this.ncdfal;
   }

   /* pass individual granule pieces just read from dataset through the RangeProcessor */
   private Object processArray(String array_name, Class arrayType, int granIdx, Object values, RangeProcessor rngProcessor, int[] start, int[] count) {
	   
	   if (rngProcessor == null) {
		   return values;
	   }
     else {
        ((AggregationRangeProcessor)rngProcessor).setWhichRangeProcessor(granIdx);

        boolean processAlongMultiScaleDim = false;

        if (rngProcessor.hasMultiDimensionScale()) { // this data variable has an array > 1 of scale/offsets.  For example, one for each band.
           rngProcessor.setMultiScaleIndex(start[rngProcessor.getMultiScaleDimensionIndex()]);
           if (count[rngProcessor.getMultiScaleDimensionIndex()] > 1) {  // if the multiScaleDim is > 1, use processAlongMultiScaleDim below
              processAlongMultiScaleDim = true;
           }
        }

        Object outArray = null;

        if (processAlongMultiScaleDim) {

           if (arrayType == Short.TYPE) {
              outArray = rngProcessor.processAlongMultiScaleDim((short[])values);
           } else if (arrayType == Byte.TYPE) {
              outArray = rngProcessor.processAlongMultiScaleDim((byte[])values);
           } else if (arrayType == Float.TYPE) {
              outArray = values;
           } else if (arrayType == Double.TYPE) {
              outArray = values;
           }

        }
        else {

           if (arrayType == Short.TYPE) {
              outArray = rngProcessor.processRange((short[]) values, null);
           } else if (arrayType == Byte.TYPE) {
        	   // if variable is a bit-field quality flag, apply mask
        	   if (qfMap.containsKey(origName)) {
        		   QualityFlag qf = qfMap.get(origName);
        		   outArray = rngProcessor.processRangeQualityFlag((byte[]) values, null, qf);
        	   } else {
        		   outArray = rngProcessor.processRange((byte[]) values, null);
        	   }
           } else if (arrayType == Float.TYPE) {
              outArray = rngProcessor.processRange((float[]) values, null);
           } else if (arrayType == Double.TYPE) {
              outArray = rngProcessor.processRange((double[]) values, null);
           }

        }
        
        return outArray;
     }
   }

   /* Application can supply a RangeProcessor for a variable 'arrayName' */
   public void addRangeProcessor(String arrayName, RangeProcessor rangeProcessor) {
	   varToRangeProcessor.put(arrayName, rangeProcessor);
   }
   
}
