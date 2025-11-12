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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PACE_Spectrum extends SpectrumAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PACE_Spectrum.class);

    public HashMap new_subset = new HashMap();

    private float initialSpectralResolution = 0f;
    private float spectralIncrement = 0f;

    public PACE_Spectrum(MultiDimensionReader reader, HashMap metadata) {
        super(reader, metadata);
    }

    public int computeNumChannels() {

        String arrayName = (String) metadata.get("array_name");

        // TJJ this part very hacky for the moment, hardcoding some stuff
        logger.info("PACE array name: " + arrayName);
        int numChannels = 0;
        if (arrayName.contains("blue")) {
            numChannels = 119;
            initialSpectralResolution = 305.0f;
        }
        if (arrayName.contains("red")) {
            numChannels = 163;
            initialSpectralResolution = 595.0f;
        }
        if (arrayName.contains("SWIR")) {
            numChannels = 9;
            initialSpectralResolution = 900.0f;
        }

        // FIXME TJJ - this is not correct!
        // some bands have specific center wavelengths and varying bandwidths, not a constant increment
        // this is just to get things working

        spectralIncrement = 2.5f;
        // initialSpectralResolution = CrIS_FSR_SDR_Utility.getWavenumberStart(arrayName);
        // spectralIncrement = CrIS_FSR_SDR_Utility.getWavenumberIncrement(arrayName);

        return numChannels;
    }

    // XXX TJJ - THIS WILL NEED TO BE FIXED!
    public float[] getChannels() throws Exception {
        float[] spectrum = new float[numChannels];
        for (int k = 0; k < numChannels; k++) {
            spectrum[k] = initialSpectralResolution + k * (spectralIncrement);
        }
        return spectrum;
    }

    public float getInitialWavenumber() {
        return 305.0f;
        // return CrIS_FSR_SDR_Utility.getWavenumberStart(getArrayName());
    }

}
