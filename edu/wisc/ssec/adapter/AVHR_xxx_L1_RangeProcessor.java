/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
 *
 * All Rights Reserved
 *
 * McIDAS-V is built on Unidata's IDV, as well as SSEC's VisAD and HYDRA
 * projects. Parts of McIDAS-V source code are based on IDV, VisAD, and
 * HYDRA source code.
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

package edu.wisc.ssec.adapter;

import java.util.HashMap;
import java.util.ArrayList;
import visad.util.Util;

public class AVHR_xxx_L1_RangeProcessor extends RangeProcessor {

        String channelName;
        float irradiance = Float.NaN;
        boolean reflective = true;
        float scale;

        double C1 = 1.191062E-5;  // (mW/(m2.sr.cm-4))
        double C2 = 1.4387863;  // (K/cm-1)

        float A = Float.NaN;
        float B = Float.NaN;
        float gamma = Float.NaN;
        float gamma3 = Float.NaN;

        public AVHR_xxx_L1_RangeProcessor(MultiDimensionReader reader, String channelName) throws Exception {
           this.channelName = channelName;
           if (channelName.equals("CH1") || channelName.equals("CH2") || channelName.equals("CH3A")) {
              reflective = true;

              int idx = 0;

              if (channelName.equals("CH1")) {
                 idx = 31;
                 scale = 100f;
              }
              else if (channelName.equals("CH2")) {
                 idx = 33;
                 scale = 100f;
              }
              else if (channelName.equals("CH3A")) {
                 idx = 35;
                 scale = 10000f;
              }

              short[] shrtVal =
                 reader.getShortArray("U-MARF/EPS/AVHR_xxx_1B/METADATA/GIADR/GIADR_RADIANCE_AVHRR_L1_ARRAY_000001",
                      new int[] {0, idx}, new int[] {1,1}, new int[] {1,1});

              irradiance = shrtVal[0]/10f;
           }
           else if (channelName.equals("CH3B") || channelName.equals("CH4") || channelName.equals("CH5")) {
              reflective = false;
              int idxGamma = 0;
              int idxA = 0;
              int idxB = 0;

              float gScale = 1;
              float aScale = 1;
              float bScale = 1;

              if (channelName.equals("CH4")) {
                 scale = 100f;
                 idxGamma = 40;
                 idxA = 41;
                 idxB = 42;
                 gScale = 1000;
                 aScale = 100000;
                 bScale = 1000000;
              }
              else if (channelName.equals("CH5")) {
                 scale = 100f;
                 idxGamma = 43;
                 idxA = 44;
                 idxB = 45;
                 gScale = 1000;
                 aScale = 100000;
                 bScale = 1000000;
              }
              else if (channelName.equals("CH3B")) {
                 scale = 10000f;
                 idxGamma = 37;
                 idxA = 38;
                 idxB = 39;
                 gScale = 100f;
                 aScale = 100000f;
                 bScale = 1000000f;
              }

              int[] intVal =
                 reader.getIntArray("U-MARF/EPS/AVHR_xxx_1B/METADATA/GIADR/GIADR_RADIANCE_AVHRR_L1_ARRAY_000001",
                      new int[] {0, idxGamma}, new int[] {1,1}, new int[] {1,1});

              gamma = intVal[0]/gScale;
              gamma3 = gamma*gamma*gamma;

              intVal =
                 reader.getIntArray("U-MARF/EPS/AVHR_xxx_1B/METADATA/GIADR/GIADR_RADIANCE_AVHRR_L1_ARRAY_000001",
                      new int[] {0, idxA}, new int[] {1,1}, new int[] {1,1});

              A = intVal[0]/aScale;

              intVal =
                 reader.getIntArray("U-MARF/EPS/AVHR_xxx_1B/METADATA/GIADR/GIADR_RADIANCE_AVHRR_L1_ARRAY_000001",
                      new int[] {0, idxB}, new int[] {1,1}, new int[] {1,1});

              B = intVal[0]/bScale;

           }
        }

        public float[] processRange(short[] values, HashMap subset) {
           float[] fltValues = new float[values.length];

           if (reflective) {
              for (int k=0; k<values.length; k++) {
                 short scldRad = values[k];
                 fltValues[k] = ((float)java.lang.Math.PI)*(scldRad/scale)/irradiance;
                 if (fltValues[k] < 0f) {
                    fltValues[k] = Float.NaN;
                 }
              }
           }
           else { // Emmissive
              for (int k=0; k<values.length; k++) {
                 float R = values[k]/scale;
                 float BT = (float) (C2*gamma/(java.lang.Math.log(1.0 + ((C1*gamma3)/R))));

                 fltValues[k] = A + B*BT;
              }
           }

           return fltValues;
        }
}
