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


public class CloudSat2D extends ProfileAlongTrack {

      public CloudSat2D() {
      }

      public CloudSat2D(MultiDimensionReader reader, HashMap metadata) {
        super(reader, metadata);
      }

      public float[] getVertBinAltitude() throws Exception {
        String propertyFileName = null;
        float[] altitude = new float[VertLen];
        try {
        propertyFileName = (String) metadata.get(ancillary_file_name);
        InputStream ios = new FileInputStream(propertyFileName);
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
          altitude[line_cnt] = Float.valueOf(tokens[0]);
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
        return null;
      }
      public RealType makeVertLocType() throws Exception {
        return null;
      }
      public RealType makeTrackTimeType() throws Exception {
        return null;
      }

}
