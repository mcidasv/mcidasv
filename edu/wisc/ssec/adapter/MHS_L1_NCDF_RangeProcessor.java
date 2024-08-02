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

public class MHS_L1_NCDF_RangeProcessor extends RangeProcessor {

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
        
        float wnc = Float.NaN;
        float wnc3 = Float.NaN;
        float alpha = 1f;
        float beta = 0f;

        public MHS_L1_NCDF_RangeProcessor(MultiDimensionReader reader, HashMap metadata, String channelName) throws Exception {
           super(reader, metadata);
           this.channelName = channelName;
              
              String wncName = null;
              String alphaName = null;
              String betaName = null;

              if (channelName.equals("CH1")) {
                  wncName = "channel_1_wnc";
                  alphaName = "channel_1_alpha";
                  betaName = "channel_1_beta";
              }
              else if (channelName.equals("CH2")) {
                  wncName = "channel_2_wnc";
                  alphaName = "channel_2_alpha";
                  betaName = "channel_2_beta";                  
              }
              else if (channelName.equals("CH3")) {
                  wncName = "channel_3_wnc";
                  alphaName = "channel_3_alpha";
                  betaName = "channel_3_beta";                  
              }
              else if (channelName.equals("CH4")) {
                  wncName = "channel_4_wnc";
                  alphaName = "channel_4_alpha";
                  betaName = "channel_4_beta";                  
              }
              else if (channelName.equals("CH5")) {
                  wncName = "channel_5_wnc";
                  alphaName = "channel_5_alpha";
                  betaName = "channel_5_beta";                  
              }
              
              wnc = ((float[]) reader.getGlobalAttribute(wncName).getArray())[0];
              wnc3 = wnc*wnc*wnc;
              
              alpha = ((float[]) reader.getGlobalAttribute(alphaName).getArray())[0];
              beta = ((float[]) reader.getGlobalAttribute(betaName).getArray())[0];  
        }

        public float[] processRange(int[] values, HashMap subset) {
           float[] fltValues = super.processRange(values, subset);

            for (int k=0; k<values.length; k++) {
               float R = fltValues[k];
               float BT = (float) (C2*wnc/(java.lang.Math.log(1.0 + ((C1*wnc3)/R))));

               fltValues[k] = (BT - beta)/alpha;
           }

           return fltValues;
        }
}
