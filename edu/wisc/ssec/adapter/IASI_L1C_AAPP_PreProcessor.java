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

public class IASI_L1C_AAPP_PreProcessor extends RangeProcessor {
    RangeProcessor rangeProcessor = null;

    public IASI_L1C_AAPP_PreProcessor() throws Exception {
        super();
    }

    public float[] processRange(short[] values, HashMap subset) {
        int multiScaleDimLen = 1;

        if (subset.get(multiScaleDimName) != null) {
            double[] coords = (double[]) subset.get(multiScaleDimName);
            multiScaleDimLen = (int) (coords[1] - coords[0] + 1.0);
        }

        if (multiScaleDimLen == 1) {
            int channelIndex = (int) ((double[]) subset.get(SpectrumAdapter.channelIndex_name))[0];
            return IASI_L1C_Utility.getRadianceImage(values, null, channelIndex);
        } else {
            if (subset.containsKey(SwathAdapter.track_name)) {
                double[] coords = (double[]) subset.get(SwathAdapter.track_name);
                int YLen = (int) (coords[1] - coords[0]) + 1;

                coords = (double[]) subset.get(SwathAdapter.xtrack_name);
                int XLen = (int) (coords[1] - coords[0]) + 1;

                double[] chans = (double[]) subset.get(SpectrumAdapter.channelIndex_name);
                int numChans = ((int) chans[1] - (int) chans[0]) + 1;

                if (XLen * YLen > 1) {
                    int nFOV = 4;
                    short[] oneSpectrum = new short[numChans];
                    float[] oneDecodedSpectrum = new float[numChans];
                    float[] decodedSpectra = new float[YLen * XLen * nFOV * numChans];

                    for (int j = 0; j < YLen; j++) {
                        for (int i = 0; i < XLen; i++) {
                            for (int f = 0; f < nFOV; f++) {
                                int idx = j * XLen * nFOV * numChans + i * nFOV * numChans + f * numChans;

                                System.arraycopy(values, idx, oneSpectrum, 0, numChans);
                                IASI_L1C_Utility.getDecodeIASISpectra(oneSpectrum, oneDecodedSpectrum, (int) chans[0], (int) chans[1]);
                                System.arraycopy(oneDecodedSpectrum, 0, decodedSpectra, idx, numChans);
                            }
                        }
                    }
                    return decodedSpectra;
                } else {
                    // fix this, may not be the full spectrum!
                    return IASI_L1C_Utility.getDecodedIASISpectra(values, null);
                }
            }

            return IASI_L1C_Utility.getDecodedIASISpectra(values, null);
        }
    }

}
