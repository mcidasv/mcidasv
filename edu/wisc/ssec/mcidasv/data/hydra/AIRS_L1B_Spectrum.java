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
    InputStream ios = new FileInputStream(propertyFileName);
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

  public float[] processRange(float[] range) {
    for (int k=0; k<numChannels; k++) {
      if (radiance_quality[k] != 0) range[k] = Float.NaN;
    }
    return range;
  }

  public double[] processRange(double[] range) {
    for (int k=0; k<numChannels; k++) {
      if (radiance_quality[k] != 0) range[k] = Double.NaN;
    }
    return range;
  }


}
