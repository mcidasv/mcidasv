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

/**
 *
 */
public class RadianceToBTbyLUT extends RangeProcessor {

    LUTtransform lutCal;


    public RadianceToBTbyLUT(NetCDFFile reader, HashMap metadata, String radLUTname, String btLUTname) throws Exception {
        super(reader, metadata);

        int numLUTvals = (reader.getDimensionLengths(radLUTname))[0];
        float[] radLUT = reader.getFloatArray(radLUTname, new int[]{0}, new int[]{numLUTvals}, new int[]{1});
        float[] btLUT = reader.getFloatArray(btLUTname, new int[]{0}, new int[]{numLUTvals}, new int[]{1});

        lutCal = new LUTtransform(radLUT, btLUT);
    }

    public RadianceToBTbyLUT(NetCDFFile reader, HashMap metadata) throws Exception {
        super(reader, metadata);
    }

    /**
     * calls super to unscale radiances then converts to BT
     */
    public float[] processRange(short[] values, HashMap subset) {
        float[] radiances = super.processRange(values, subset);

        float[] brightnessTemps = null;

        try {
            brightnessTemps = lutCal.radianceToBrightnessTemp(radiances);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return brightnessTemps;
    }
}