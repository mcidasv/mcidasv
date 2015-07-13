/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2015
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

import visad.Data;
import visad.FlatField;
import visad.Set;
import visad.CoordinateSystem;
import visad.RealType;
import visad.RealTupleType;
import visad.SetType;
import visad.Linear2DSet;
import visad.Unit;
import visad.FunctionType;
import visad.VisADException;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class GPM2D extends ProfileAlongTrack {

      public GPM2D() {
      }

      public GPM2D(MultiDimensionReader reader, HashMap metadata) {
        super(reader, metadata);
        System.err.println("constructor...");
      }

	public float[] getVertBinAltitude() throws Exception {
    	  System.err.println("getVertBinAltitude in, VertLen: " + VertLen);
        float[] altitude = new float[VertLen];
        float tmpVal = 0.0f;
        int j = 0;
        for (int i = VertLen - 1; i >= 0 ; i--) {
        	tmpVal = ((float) i * 1000) / 8.0f;
        	System.err.println("Alt idx " + i + ": " + tmpVal);
        	altitude[j] = tmpVal;
        	j++;
        }
        return altitude;
      }

      public float[] getTrackTimes() throws Exception {
        return null;
      }

      public RealType makeVertLocType() throws Exception {
        return RealType.Altitude;
      }

      public RealType makeTrackTimeType() throws Exception {
        return null;
      }

      public float[] getTrackLongitude() throws Exception {
    	  System.err.println("getTrackLongitude, TrackLen: " + TrackLen);
        int[] start = new int[] {0, 25};
        int[] count = new int[] {TrackLen, 1};
        int[] stride = new int[] {1, 1};
        System.err.println("get float Lons...");
        float[] vals = reader.getFloatArray((String)metadata.get(longitude_name), start, count, stride);
        System.err.println("ret float Lons...");
        return vals;
      }

      public float[] getTrackLatitude() throws Exception {
        int[] start = new int[] {0, 25};
        int[] count = new int[] {TrackLen, 1};
        int[] stride = new int[] {1, 1};
        float[] vals = reader.getFloatArray((String)metadata.get(latitude_name), start, count, stride);
        System.err.println("ret float Lats...");
        return vals;
      }

      public HashMap getDefaultSubset() {
        HashMap subset = ProfileAlongTrack.getEmptySubset();

        double[] coords = (double[])subset.get("TrackDim");
        coords[0] = 0.0;
        coords[1] = TrackLen - 1;
        coords[2] = 1.0;
        subset.put("TrackDim", coords);

        coords = (double[])subset.get("VertDim");
        coords[0] = 0.0;
        coords[1] = (VertLen) - 1;
        coords[2] = 1.0;
        subset.put("VertDim", coords);
        return subset;
      }

}
