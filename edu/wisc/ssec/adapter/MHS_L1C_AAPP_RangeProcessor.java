/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
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
import java.util.ArrayList;

import visad.util.Util;

public class MHS_L1C_AAPP_RangeProcessor extends RangeProcessor {

    String channelName;
    float irradiance = Float.NaN;
    boolean reflective = true;
    float scale;

    double C1 = 1.191044E-5;  // (mW/(m2.sr.cm-4))
    double C2 = 1.4387869;  // (K/cm-1)

    float alpha = 1f;
    float beta = 0f;
    float wnc = Float.NaN;
    float wnc3 = Float.NaN;

    float[] offsets;
    float[] slopes;

    public MHS_L1C_AAPP_RangeProcessor(MultiDimensionReader reader, HashMap metadata) throws Exception {
        super(reader, metadata);

        offsets = (float[]) reader.getGlobalAttribute("AMSUB_temprad_offset").getArray();
        slopes = (float[]) reader.getGlobalAttribute("AMSUB_temprad_slope").getArray();
    }

    public float[] processRange(int[] values, HashMap subset) {
        //float[] fltValues = super.processRange(values, subset);

        float[] fltValues = new float[values.length];
        double[] coords = (double[]) subset.get(SpectrumAdapter.channelIndex_name);
        boolean multiChan = (coords[1] - coords[0] + 1.0) > 1;
        int channelIndex = (int) coords[0];

        float beta = offsets[channelIndex];
        float alpha = slopes[channelIndex];

        for (int k = 0; k < values.length; k++) {

            if (multiChan) {
                beta = offsets[k];
                alpha = slopes[k];
            }

            if (values[k] >= 0) {
                fltValues[k] = (values[k] * 0.01f - beta) / alpha;
            } else {
                fltValues[k] = Float.NaN;
            }
        }

        return fltValues;
    }
}
