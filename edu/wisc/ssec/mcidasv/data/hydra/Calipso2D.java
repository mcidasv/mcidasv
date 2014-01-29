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

import visad.Data;
import visad.FlatField;
import visad.Set;
import visad.CoordinateSystem;
import visad.RealType;
import visad.RealTupleType;
import visad.SetType;
import visad.Linear2DSet;
import visad.Unit;
import visad.BaseUnit;
import visad.OffsetUnit;
import visad.FunctionType;
import visad.VisADException;
import visad.data.units.Parser;
import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.HashMap;
import java.util.StringTokenizer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class Calipso2D extends ProfileAlongTrack {

      double start_time;

      public Calipso2D() {
      }

      public Calipso2D(MultiDimensionReader reader, HashMap metadata, boolean isVertTypeAlt) {
        super(reader, metadata, isVertTypeAlt);
      }

      public Calipso2D(MultiDimensionReader reader, HashMap metadata) {
        super(reader, metadata);
        HashMap table = ProfileAlongTrack.getEmptyMetadataTable();
        table.put(ProfileAlongTrack.array_name, "Surface_Elevation");
      }

      public float[] getVertBinAltitude() throws Exception {
        String propertyFileName = null;
        float[] altitude = new float[VertLen];
        int line_cnt = 0;
        try {
          propertyFileName = (String) metadata.get(ancillary_file_name);
          InputStream ios = getClass().getResourceAsStream(propertyFileName);
          BufferedReader ancillaryReader = new BufferedReader(new InputStreamReader(ios));

          while (true) {
            String line = ancillaryReader.readLine();
            if (line == null) break;
            if (line.startsWith("!")) continue;
            StringTokenizer strTok = new StringTokenizer(line);
            String[] tokens = new String[strTok.countTokens()];
            int tokCnt = 0;
            while (strTok.hasMoreElements()) {
              tokens[tokCnt++] = strTok.nextToken();
            }
            altitude[line_cnt] = (Float.valueOf(tokens[0]))*1000f;
            line_cnt++;
          }
          ios.close();
          }
          catch (Exception e) {
            System.out.println("fail on ancillary file read: "+propertyFileName);
          }
          return altitude;
      }

      public float[] getTrackTimes() throws Exception {
        int[] start = new int[] {0,0};
        int[] count = new int[] {TrackLen/10, 1};
        int[] stride = new int[] {10,1};
        double[] times = reader.getDoubleArray((String)metadata.get(profileTime_name), start, count, stride);
        start_time = times[0];
        double time_inc = (times[times.length-1] - times[0])/times.length;
        float[] new_times = new float[TrackLen];
        for (int t=0; t<TrackLen;t++) {
          new_times[t] = (float) times[0] + (float)(t*time_inc);
        }
        return new_times;
      }

      public float[] getTrackLongitude() throws Exception {
        int[] start = new int[] {0,0};
        int[] count = new int[] {TrackLen, 1};
        int[] stride = new int[] {1,1};
        float[] vals = reader.getFloatArray((String)metadata.get(longitude_name), start, count, stride);
        return vals;
      }

      public float[] getTrackLatitude() throws Exception {
        int[] start = new int[] {0,0};
        int[] count = new int[] {TrackLen, 1};
        int[] stride = new int[] {1,1};
        float[] vals = reader.getFloatArray((String)metadata.get(latitude_name), start, count, stride);
        return vals;
      }

      public RealType makeVertLocType() throws Exception {
        return RealType.Altitude;
      }

      public RealType makeTrackTimeType() throws Exception {
        OffsetUnit unit = (OffsetUnit) Parser.parse("seconds since 1993-01-01 00:00:00Z");
        OffsetUnit new_unit = new OffsetUnit(start_time, unit);
        RealType timeType = RealType.getRealType("Track_Time", new_unit);
        return timeType;
      }

      public FlatField getData(Object subset) throws Exception {
        FlatField field = super.getData(subset);
        return field;
      }
}
