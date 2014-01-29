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

import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;


public class AIRS_L1B_Spectrum extends SpectrumAdapter {

  String propertyFileName;
  float[] srf_centroid_freq;
  int[] radiance_quality;

  public AIRS_L1B_Spectrum(MultiDimensionReader reader, HashMap metadata) {
    super(reader, metadata);
  }

  public float[] getChannels() throws Exception {
    srf_centroid_freq = new float[numChannels];
    radiance_quality = new int[numChannels];
    propertyFileName = (String) metadata.get(SpectrumAdapter.ancillary_file_name);
    InputStream ios = getClass().getResourceAsStream(propertyFileName);
    BufferedReader ancillaryReader = new BufferedReader(new InputStreamReader(ios));
  
    int cnt = 0;
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
      srf_centroid_freq[cnt] = Float.valueOf(tokens[1]);
      radiance_quality[cnt] = Integer.valueOf(tokens[12]);
      cnt++;
    }
    ios.close();

    float[] channels = new float[numChannels];
    System.arraycopy(srf_centroid_freq,0,channels,0,numChannels);
    return srf_centroid_freq;
  }

  public float[] processRange(float[] range, Object subset) {
    for (int k=0; k<numChannels; k++) {
      if (radiance_quality[k] != 0) range[k] = Float.NaN;
    }
    float[] new_range = sortRange(range);
    return new_range;
  }

  public double[] processRange(double[] range, Object subset) {
    for (int k=0; k<numChannels; k++) {
      if (radiance_quality[k] != 0) range[k] = Double.NaN;
    }
    double[] new_range = sortRange(range);
    return new_range;
  }


}
