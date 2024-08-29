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

public class AMSA_xxx_L1_RangeProcessor extends RangeProcessor {

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

    public AMSA_xxx_L1_RangeProcessor(MultiDimensionReader reader, String channelName) throws Exception {
        this.channelName = channelName;
        int idxGamma = 0;
        int idxA = 0;
        int idxB = 0;

        float gScale = 1;
        float aScale = 1;
        float bScale = 1;

        if (channelName.equals("CH1")) {
            scale = 1E07f;
            idxGamma = 0;
            idxA = 0;
            idxB = 0;
            gScale = 1E06f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 23.8f;
        } else if (channelName.equals("CH2")) {
            scale = 1E07f;
            idxGamma = 1;
            idxA = 1;
            idxB = 1;
            gScale = 1E06f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 31.4f;
        } else if (channelName.equals("CH3")) {
            scale = 1E07f;
            idxGamma = 2;
            idxA = 2;
            idxB = 2;
            gScale = 1E06f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 50.3f;
        } else if (channelName.equals("CH4")) {
            scale = 1E07f;
            idxGamma = 3;
            idxA = 3;
            idxB = 3;
            gScale = 1E06f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 52.8f;
        } else if (channelName.equals("CH5")) {
            scale = 1E07f;
            idxGamma = 4;
            idxA = 4;
            idxB = 4;
            gScale = 1E06f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 53.596f;
        } else if (channelName.equals("CH6")) {
            scale = 1E07f;
            idxGamma = 5;
            idxA = 5;
            idxB = 5;
            gScale = 1E06f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 54.4f;
        } else if (channelName.equals("CH7")) {
            scale = 1E07f;
            idxGamma = 6;
            idxA = 6;
            idxB = 6;
            gScale = 1E06f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 54.9f;
        } else if (channelName.equals("CH8")) {
            scale = 1E07f;
            idxGamma = 7;
            idxA = 7;
            idxB = 7;
            gScale = 1E06f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 55.5f;
        } else if (channelName.equals("CH9")) {
            scale = 1E07f;
            idxGamma = 8;
            idxA = 8;
            idxB = 8;
            gScale = 1E06f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 57.290f;
        } else if (channelName.equals("CH10")) {
            scale = 1E07f;
            idxGamma = 9;
            idxA = 9;
            idxB = 9;
            gScale = 1E06f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 57.290f;
        } else if (channelName.equals("CH11")) {
            scale = 1E07f;
            idxGamma = 10;
            idxA = 10;
            idxB = 10;
            gScale = 1E06f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 59.290f;
        } else if (channelName.equals("CH12")) {
            scale = 1E07f;
            idxGamma = 11;
            idxA = 11;
            idxB = 11;
            gScale = 1E06f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 59.290f;
        } else if (channelName.equals("CH13")) {
            scale = 1E07f;
            idxGamma = 12;
            idxA = 12;
            idxB = 12;
            gScale = 1E05f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 59.290f;
        } else if (channelName.equals("CH14")) {
            scale = 1E07f;
            idxGamma = 13;
            idxA = 13;
            idxB = 13;
            gScale = 1E05f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 59.290f;
        } else if (channelName.equals("CH15")) {
            scale = 1E07f;
            idxGamma = 14;
            idxA = 14;
            idxB = 14;
            gScale = 1E05f;
            aScale = 1E06f;
            bScale = 1E06f;
            freq = 89.0f;
        }

        /**
         int[] intVal =
         reader.getIntArray("U-MARF/EPS/HIRS_xxx_1B/METADATA/GIADR/GIADR_TEMP_HIRS_L1_ARRAY_000001",
         new int[] {0, 0}, new int[] {1,1}, new int[] {1,1});

         gamma = intVal[idxGamma]/gScale;
         gamma3 = gamma*gamma*gamma;

         intVal =
         reader.getIntArray("U-MARF/EPS/HIRS_xxx_1B/METADATA/GIADR/GIADR_TEMP_HIRS_L1_ARRAY_000001",
         new int[] {0, 1}, new int[] {1,1}, new int[] {1,1});

         A = intVal[idxA]/aScale;

         intVal =
         reader.getIntArray("U-MARF/EPS/HIRS_xxx_1B/METADATA/GIADR/GIADR_TEMP_HIRS_L1_ARRAY_000001",
         new int[] {0, 2}, new int[] {1,1}, new int[] {1,1});

         B = intVal[idxB]/bScale;
         */
        gamma = 1.0f / ((3.0E08f / (freq * 1E09f)) * 100.0f);
        gamma3 = gamma * gamma * gamma;

    }

    public float[] processRange(int[] values, HashMap subset) {
        float[] fltValues = new float[values.length];

        for (int k = 0; k < values.length; k++) {
            float R = values[k] / scale;
            float BT = (float) (C2 * gamma / (java.lang.Math.log(1.0 + ((C1 * gamma3) / R))));

            //fltValues[k] = A + B*BT;
            fltValues[k] = BT;
        }

        return fltValues;
    }
}
