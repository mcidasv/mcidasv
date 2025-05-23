/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import visad.RealType;

public class CloudSat2D extends ProfileAlongTrack {

	  private static final Logger logger = LoggerFactory.getLogger(CloudSat2D.class);
	  
      public CloudSat2D() {
      }

      public CloudSat2D(MultiDimensionReader reader, Map<String, Object> metadata) {
        super(reader, metadata);
      }

      public float[] getVertBinAltitude() throws Exception {
        String propertyFileName = null;
        float[] altitude = new float[VertLen];
        try {
        propertyFileName = (String) metadata.get(ancillary_file_name);
        InputStream ios = getClass().getResourceAsStream(propertyFileName);
        BufferedReader ancillaryReader = new BufferedReader(new InputStreamReader(ios));

        int line_cnt = 0;
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
          altitude[line_cnt] = (Float.valueOf(tokens[0])) * 1000f;
          line_cnt++;
        }
        ios.close();
        }
        catch (Exception e) {
          logger.error("fail on ancillary file read: " + propertyFileName);
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
        int[] start = new int[] {0};
        int[] count = new int[] {TrackLen};
        int[] stride = new int[] {1};
        float[] vals = reader.getFloatArray((String)metadata.get(longitude_name), start, count, stride);
        return vals;
      }

      public float[] getTrackLatitude() throws Exception {
        int[] start = new int[] {0};
        int[] count = new int[] {TrackLen};
        int[] stride = new int[] {1};
        float[] vals = reader.getFloatArray((String)metadata.get(latitude_name), start, count, stride);
        return vals;
      }

      public Map<String, double[]> getDefaultSubset() {
        Map<String, double[]> subset = ProfileAlongTrack.getEmptySubset();

        double[] coords = subset.get("TrackDim");
        coords[0] = 1000.0;
        coords[1] = (TrackLen - 1000.0) - 1;
        coords[2] = TrackSelection.DEFAULT_TRACK_STRIDE;
        subset.put("TrackDim", coords);

        coords = subset.get("VertDim");
        coords[0] = 10.0;
        coords[1] = (VertLen) - 1;
        coords[2] = TrackSelection.DEFAULT_VERTICAL_STRIDE;
        subset.put("VertDim", coords);
        return subset;
      }

}
