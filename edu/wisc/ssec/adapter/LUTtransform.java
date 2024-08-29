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

import visad.Gridded1DSet;
import visad.Linear1DSet;
import visad.RealType;

/**
 * Convert radiance to/from brightness temperature via provided Lookup Tables
 */
public class LUTtransform {

    public static final int LINEAR = 0;
    public static final int GENERAL = 1;

    int ALGO = LINEAR;

    float[] radianceLUT;
    float[] brightnessTempLUT;

    int numOfLUTvalues;

    Gridded1DSet radLUTSet;
    Gridded1DSet btLUTSet;

    public LUTtransform(float[] radianceLUT, float[] brightnessTempLUT) throws Exception {
        this(radianceLUT, brightnessTempLUT, GENERAL);
    }

    public LUTtransform(float[] radianceLUT, float[] brightnessTempLUT, int algo) throws Exception {
        this.radianceLUT = radianceLUT;
        this.brightnessTempLUT = brightnessTempLUT;
        this.ALGO = algo;

        numOfLUTvalues = radianceLUT.length;
        if (numOfLUTvalues != brightnessTempLUT.length) {
            throw new Exception("radiance and brightnessTemp LUTs must have same length");
        }

        switch (ALGO) {
            case GENERAL:
                radLUTSet = new Gridded1DSet(RealType.Generic, new float[][]{radianceLUT}, numOfLUTvalues);
                btLUTSet = new Gridded1DSet(RealType.Generic, new float[][]{brightnessTempLUT}, numOfLUTvalues);
                break;
            case LINEAR:
                radLUTSet = new Linear1DSet(radianceLUT[0], radianceLUT[numOfLUTvalues - 1], numOfLUTvalues);
                btLUTSet = new Linear1DSet(brightnessTempLUT[0], brightnessTempLUT[numOfLUTvalues - 1], numOfLUTvalues);
                break;
        }
    }

    public float[] radianceToBrightnessTemp(float[] radiances) throws Exception {
        int numObs = radiances.length;
        float[] brightnessTemps = new float[numObs];

        int[] btLUTindexes = radLUTSet.valueToIndex(new float[][]{radiances});

        for (int k = 0; k < numObs; k++) {
            int idx = btLUTindexes[k];
            if (idx >= 0) { // Just in case
                brightnessTemps[k] = brightnessTempLUT[idx];
            } else {
                brightnessTemps[k] = Float.NaN;
            }
        }

        return brightnessTemps;
    }

    public float[] brightnessTempToRadiance(float[] brightnessTemps) throws Exception {
        int numObs = brightnessTemps.length;
        float[] radiances = new float[numObs];

        int[] radLUTindexes = btLUTSet.valueToIndex(new float[][]{brightnessTemps});
        for (int k = 0; k < numObs; k++) {
            int idx = radLUTindexes[k];
            if (idx >= 0) {
                radiances[k] = radianceLUT[idx];
            } else {
                radiances[k] = Float.NaN;
            }
        }

        return radiances;
    }
}
