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

package edu.wisc.ssec.adapter;

import java.util.HashMap;

public class AMSA_L1_NCDF_RangeProcessor extends RangeProcessor {

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
    float freq = Float.NaN;

    float wnc = Float.NaN;
    float wnc3 = Float.NaN;
    float alpha = Float.NaN;
    float beta = Float.NaN;

    public AMSA_L1_NCDF_RangeProcessor(MultiDimensionReader reader, HashMap metadata, String channelName) throws Exception {
        super(reader, metadata);

        this.channelName = channelName;
        int idx = 0;

        if (channelName.equals("CH1")) {
            idx = 0;
        } else if (channelName.equals("CH2")) {
            idx = 1;
        } else if (channelName.equals("CH3")) {
            idx = 2;
        } else if (channelName.equals("CH4")) {
            idx = 3;
        } else if (channelName.equals("CH5")) {
            idx = 4;
        } else if (channelName.equals("CH6")) {
            idx = 5;
        } else if (channelName.equals("CH7")) {
            idx = 6;
        } else if (channelName.equals("CH8")) {
            idx = 7;
        } else if (channelName.equals("CH9")) {
            idx = 8;
        } else if (channelName.equals("CH10")) {
            idx = 9;
        } else if (channelName.equals("CH11")) {
            idx = 10;
        } else if (channelName.equals("CH12")) {
            idx = 11;
        } else if (channelName.equals("CH13")) {
            idx = 12;
        } else if (channelName.equals("CH14")) {
            idx = 13;
        } else if (channelName.equals("CH15")) {
            idx = 14;
        }

        int[] intVal =
                reader.getIntArray("wnc", new int[]{idx}, new int[]{1}, new int[]{1});

        wnc = intVal[0] * 1E-6f;
        wnc3 = wnc * wnc * wnc;

        intVal =
                reader.getIntArray("alpha", new int[]{idx}, new int[]{1}, new int[]{1});
        alpha = intVal[0] * 1E-6f;

        intVal =
                reader.getIntArray("beta", new int[]{idx}, new int[]{1}, new int[]{1});
        beta = intVal[0] * 1E-6f;


        //gamma = 1.0f/((3.0E08f/(freq*1E09f))*100.0f);
    }

    public float[] processRange(int[] values, HashMap subset) {
        float[] fltValues = super.processRange(values, subset);

        for (int k = 0; k < values.length; k++) {
            float R = fltValues[k];
            float BT = (float) (C2 * wnc / (java.lang.Math.log(1.0 + ((C1 * wnc3) / R))));

            fltValues[k] = BT;
            //fltValues[k] = (BT - beta)/alpha; // alpha and beta seem to be wrong
        }

        return fltValues;
    }
}
