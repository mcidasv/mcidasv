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
package edu.wisc.ssec.mcidasv.data;


import edu.wisc.ssec.mcidasv.data.hydra.NetCDFFile;
import visad.Data;
import visad.Unit;
import visad.Gridded3DSet;
import visad.Gridded1DDoubleSet;
import visad.Gridded1DSet;
import visad.Set;
import visad.UnionSet;
import visad.RealTupleType;
import visad.RealType;
import visad.FlatField;
import visad.FieldImpl;
import visad.Integer1DSet;
import visad.FunctionType;
import visad.CoordinateSystem;
import visad.VisADException;
import visad.DateTime;
import visad.data.units.Parser;
import visad.data.units.ParseException;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.List;

import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;

import ucar.unidata.data.DataUtil;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataDataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataSourceDescriptor;



public class NearCastTrajDataSource extends DataSourceImpl {

    private static final String DATA_DESCRIPTION = "NearCastTrajectory";

    public static String parcelDimName = "parcel";
    public static String timeDimName = "times";
    public static String lonName = "lon";
    public static String latName = "lat";

    NetCDFFile ncFile = null;
    String pressName = "pres";
    String timeName = "times";


    int[] start = new int[2];
    int[] count = new int[2];
    int[] stride = new int[2];


    int parcelDimIdx = 0;
    int timeDimIdx = 1;

    String[] paramList = null;
    
    String fileName = null;

    CoordinateSystem presToHeightCS = null;

    Unit timeUnit;
    Set timeSet;

    int numTimes;
    int numParcels;

    Range lonRange = new Range();
    Range latRange = new Range();
    Range paramRange = new Range();

    public NearCastTrajDataSource() {
    }

    public NearCastTrajDataSource(String filename) throws VisADException {
      this(null, Misc.newList(filename), null);
    }

    public NearCastTrajDataSource(DataSourceDescriptor descriptor,
                                 String fileName, Hashtable properties) throws VisADException { 
        this(descriptor, Misc.newList(fileName), properties);
    }

    public NearCastTrajDataSource(DataSourceDescriptor descriptor,
                                 List newSources, Hashtable properties) throws VisADException {

       super(descriptor, DATA_DESCRIPTION, DATA_DESCRIPTION, properties);

       presToHeightCS = DataUtil.getPressureToHeightCS(DataUtil.STD_ATMOSPHERE);

       fileName = (String) newSources.get(0);

       try {
         ncFile = new NetCDFFile(fileName);
       }
       catch (Exception e) {
         e.printStackTrace();
       }
         
       paramList = new String[] {"temp", "q", "the", "tp", "MS", "MQ", "MTe", "TP"};


       String unitStr = ncFile.getArrayUnitString(timeName);
       try {
         timeUnit = Parser.parse(unitStr);
       } 
       catch (ParseException e) {
         System.out.println(e);
       }

       numTimes = ncFile.getDimensionLength(timeDimName);
       numParcels = ncFile.getDimensionLength(parcelDimName);

       try {
         Class type = ncFile.getArrayType(timeName);
         if (type == Double.TYPE) {
            double[] timeValues = ncFile.getDoubleArray(timeName, new int[] {0}, new int[] {numTimes}, new int[] {1});
            timeSet = new Gridded1DDoubleSet(
                       RealType.Time, new double[][] {timeValues}, numTimes, null, new Unit[] {timeUnit}, null);
         }
         else if (type == Float.TYPE) {
            float[] timeValues = ncFile.getFloatArray(timeName, new int[] {0}, new int[] {numTimes}, new int[] {1});
            timeSet = new Gridded1DSet(RealType.Time, new float[][] {timeValues}, numTimes, null, new Unit[] {timeUnit}, null);

         }
       } 
       catch (Exception e) {
         e.printStackTrace();
       }
    }


    public FlatField[] createVisADData(String paramName) {
      return null;
    }

    public FlatField singleTraj(String paramName, int parcelIndex, int timeStart, int timeCount, int timeStride) throws Exception {

      start[parcelDimIdx] = parcelIndex;
      start[timeDimIdx] = timeStart;

      count[parcelDimIdx] = 1;
      count[timeDimIdx] = timeCount;

      stride[parcelDimIdx] = 1;
      stride[timeDimIdx] = 1;

      float[] lons = ncFile.getFloatArray(lonName, start, count, stride);
      float[] lats = ncFile.getFloatArray(latName, start, count, stride);

      float[] minmax = minmax(lons, (float)lonRange.getMin(), (float)lonRange.getMax());
      lonRange.setMin(minmax[0]);
      lonRange.setMax(minmax[1]);
      minmax = minmax(lats, (float)latRange.getMin(), (float)latRange.getMax());
      latRange.setMin(minmax[0]);
      latRange.setMax(minmax[1]);

      float[] pres = ncFile.getFloatArray(pressName, start, count, stride);
      float[] param = ncFile.getFloatArray(paramName, start, count, stride);
      minmax = minmax(param, (float)paramRange.getMin(), (float)paramRange.getMax());
      paramRange.setMin(minmax[0]);
      paramRange.setMax(minmax[1]);

      float[] alt = (presToHeightCS.toReference(new float[][] {pres}))[0];
 
      float[][] trajCoords = new float[][] {lons, lats, alt};

      Gridded3DSet domain = new Gridded3DSet(RealTupleType.SpatialEarth3DTuple, trajCoords, trajCoords[0].length);

      FunctionType fncType = new FunctionType(RealTupleType.SpatialEarth3DTuple, RealType.getRealType(paramName));
      FlatField traj = new FlatField(fncType, domain);
      traj.setSamples(new float[][] {param}, false);
 
      return traj;
    }

    /**
     * Make and insert the <code>DataChoice</code>-s for this
     * <code>DataSource</code>.
     */
    public void doMakeDataChoices() {
        try {
          for (int k=0; k<paramList.length; k++) {
            DataChoice choice = doMakeDataChoice(k);
            if (choice != null) {
	      addDataChoice(choice);
	    }
          }
        }
        catch(Exception e) {
          e.printStackTrace();
        }
    }

    private DataChoice doMakeDataChoice(int idx) throws Exception {
        String name = paramList[idx];
	DirectDataChoice ddc = null;
	if (ncFile.hasArray(name)) {
           ddc = new DirectDataChoice(this, new Integer(idx), name, name, null, new Hashtable());
	}
        return ddc;
    }

    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        String paramName = dataChoice.getName();

        FieldImpl trajField = new FieldImpl(
                      new FunctionType(RealType.Generic, new FunctionType(RealTupleType.SpatialEarth3DTuple, RealType.getRealType(paramName))), new Integer1DSet(numParcels));

        FieldImpl trajTimeField = new FieldImpl(new FunctionType(RealType.Time, trajField.getType()), timeSet); 

        lonRange.setMin(Float.MAX_VALUE);
        lonRange.setMax(-Float.MAX_VALUE);
        latRange.setMin(Float.MAX_VALUE);
        latRange.setMax(-Float.MAX_VALUE);
        paramRange.setMin(Float.MAX_VALUE);
        paramRange.setMax(-Float.MAX_VALUE);

        try {
          for (int t=0; t<numTimes; t++) {
             trajField = new FieldImpl(
                      new FunctionType(RealType.Generic, new FunctionType(RealTupleType.SpatialEarth3DTuple, RealType.getRealType(paramName))), new Integer1DSet(numParcels));
            for (int k=0; k<numParcels/4; k++) {
               FlatField fld = singleTraj(paramName, k*4, 0, t+1, 1);
               trajField.setSample(k, fld);
            }
            trajTimeField.setSample(t, trajField);
          }
          return trajTimeField;
        }
        catch (Exception e) {
          e.printStackTrace();
          return null;
        }
    }

    public static float[] minmax(float[] values, float min, float max) {
      for (int k = 0; k < values.length; k++) {
        float val = values[k];
        if ((val == val) && (val < Float.POSITIVE_INFINITY) && (val > Float.NEGATIVE_INFINITY)) {
          if (val < min) min = val;
          if (val > max) max = val;
        }
      }
      return new float[] {min, max};
    }

   public Range getLonRange() {
     return lonRange;
   }

   public Range getLatRange() {
     return latRange;
   }

   public Range getParamRange() {
     return paramRange;
   }

}
