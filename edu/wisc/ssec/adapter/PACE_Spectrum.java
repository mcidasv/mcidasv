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
    private float[] wavenumArray = null;

    public PACE_Spectrum(MultiDimensionReader reader, HashMap metadata) {
        super(reader, metadata);
    }

    public int computeNumChannels() {
        logger.info("computeNumChannels() in...");
        int numChannels = (int) metadata.get("num_channels");
        initialSpectralResolution = (float) metadata.get("first_wavenumber");
        logger.info("num channels: " + numChannels + ", first band: " + initialSpectralResolution);
        wavenumArray = (float[]) metadata.get("wavenumber_array");
        return numChannels;
    }

    // For PACE, the wavenumbers are not always a fixed increment apart, so we have been passed
    // the entire array via the metadata hashmap in the constructor.  Just return that array.
    public float[] getChannels() throws Exception {
        logger.info("initialSpectralResolution: " + initialSpectralResolution);
        logger.info("wavenumArray[0]: " + wavenumArray[0]);
        return wavenumArray;
    }

    public float getInitialWavenumber() {
        logger.info("getInitialWavenumber() in, val: " + initialSpectralResolution);
        return initialSpectralResolution;
    }

}
