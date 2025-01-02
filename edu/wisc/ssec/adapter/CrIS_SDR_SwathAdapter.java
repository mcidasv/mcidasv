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

import visad.FlatField;
import visad.Set;

public class CrIS_SDR_SwathAdapter extends SwathAdapter {
    int apod_offset = 2;


    public CrIS_SDR_SwathAdapter(MultiDimensionReader reader, HashMap metadata, MultiDimensionReader geoReader) {
        super(reader, metadata, geoReader);

        try {
            RangeProcessor crisRngProcessor = new CrIS_RangeProcessor();
            setRangeProcessor(crisRngProcessor);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void setLengths() { // define the swath dimensions
        // define the 2D swath dimensions transforming from native (scan, EFOV, 9 IFOVs) -> (scan*3, EFOV*3)
        int len = getTrackLength();
        setTrackLength(len *= 3);
        len = getXTrackLength();
        setXTrackLength(len *= 3);
    }

    public FlatField getData(Object subset) throws Exception {

        HashMap new_subset = (HashMap) ((HashMap) subset).clone();
        new_subset.putAll((HashMap) subset);

        // reform subset to integral numbers of EFOV (FORs)
        // you may not get exactly what you ask for in this case.
        // Keep the spatial coordinates in the 2D coords, but carry along the IFOV indexes.

        double[] coords = (double[]) new_subset.get(SwathAdapter.track_name);
        double[] new_coords = new double[]{3.0 * Math.floor((coords[0]) / 3), 3.0 * Math.floor((coords[1] + 1) / 3) - 1, 1.0};
        new_subset.put(SwathAdapter.track_name, new_coords);

        coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
        new_coords = new double[]{3.0 * Math.floor((coords[0]) / 3), 3.0 * Math.floor((coords[1] + 1) / 3) - 1, 1.0};
        new_subset.put(SwathAdapter.xtrack_name, new_coords);

        new_coords = new double[]{0.0, (9.0 - 1.0), 1.0};
        new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

        Set domainSet = makeDomain(new_subset);


        // transfrom the coordinates from the 2D form back to the (scan, EFOV, 9 IFOVs) form
        // which is the native storage order.  Needed for makeFlatField which accesses readArray directly.

        new_subset = (HashMap) ((HashMap) subset).clone();
        new_subset.putAll((HashMap) subset);

        coords = (double[]) new_subset.get(SwathAdapter.track_name);
        new_coords = new double[]{Math.floor(coords[0] / 3), Math.floor((coords[1] + 1) / 3) - 1, 1.0};
        new_subset.put(SwathAdapter.track_name, new_coords);

        coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
        new_coords = new double[]{Math.floor(coords[0] / 3), Math.floor((coords[1] + 1) / 3) - 1, 1.0};
        new_subset.put(SwathAdapter.xtrack_name, new_coords);

        new_coords = new double[]{0.0, (9.0 - 1.0), 1.0};
        new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

        FlatField swath = makeFlatField(domainSet, new_subset);

        return swath;
    }

    public FlatField makeConvolvedRadiances(HashMap subset, float[] wghts) throws Exception {
        HashMap new_subset = (HashMap) ((HashMap) subset).clone();
        new_subset.putAll((HashMap) subset);

        // reform subset to integral numbers of EFOV (FORs)
        // you may not get exactly what you ask for in this case.
        // Keep the spatial coordinates in the 2D coords, but carry along the IFOV indexes.

        double[] coords = (double[]) new_subset.get(SwathAdapter.track_name);
        double[] new_coords = new double[]{3.0 * Math.floor((coords[0]) / 3), 3.0 * Math.floor((coords[1] + 1) / 3) - 1, 1.0};
        new_subset.put(SwathAdapter.track_name, new_coords);

        coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
        new_coords = new double[]{3.0 * Math.floor((coords[0]) / 3), 3.0 * Math.floor((coords[1] + 1) / 3) - 1, 1.0};
        new_subset.put(SwathAdapter.xtrack_name, new_coords);

        new_coords = new double[]{0.0, (9.0 - 1.0), 1.0};
        new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

        Set domainSet = makeDomain(new_subset);


        // transfrom the coordinates from the 2D form back to the (scan, EFOV, 9 IFOVs) form
        // which is the native storage order.  Needed for makeFlatField which accesses readArray directly.

        new_subset = (HashMap) ((HashMap) subset).clone();
        new_subset.putAll((HashMap) subset);

        coords = (double[]) new_subset.get(SwathAdapter.track_name);
        new_coords = new double[]{Math.floor(coords[0] / 3), Math.floor((coords[1] + 1) / 3) - 1, 1.0};
        new_subset.put(SwathAdapter.track_name, new_coords);
        int YLen = (int) (new_coords[1] - new_coords[0]) + 1;

        coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
        new_coords = new double[]{Math.floor(coords[0] / 3), Math.floor((coords[1] + 1) / 3) - 1, 1.0};
        new_subset.put(SwathAdapter.xtrack_name, new_coords);
        int XLen = (int) (new_coords[1] - new_coords[0]) + 1;

        new_coords = new double[]{0.0, (9.0 - 1.0), 1.0};
        new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

        double[] chans = (double[]) new_subset.get(SpectrumAdapter.channelIndex_name);
        int numChans = ((int) chans[1] - (int) chans[0]) + 1;
        chans[0] += apod_offset;
        chans[1] += apod_offset;

        float[] svalues = (float[]) readArray(new_subset);

        int nFOV = 9;
        float[] spectra = new float[numChans];
        float[] convldRads = new float[YLen * XLen * nFOV];

        for (int j = 0; j < YLen; j++) {
            for (int i = 0; i < XLen; i++) {
                for (int f = 0; f < nFOV; f++) {

                    int idx = j * XLen * nFOV * numChans + i * nFOV * numChans + f * numChans;

                    System.arraycopy(svalues, idx, spectra, 0, numChans);
                    /* convolve here, using incoming weights */
                    float val = 0;
                    for (int k = 0; k < numChans; k++) {
                        val += wghts[k] * spectra[k];
                    }
                    convldRads[j * XLen * nFOV + i * nFOV + f] = val;
                }
            }
        }

        convldRads = CrIS_SDR_Utility.psuedoScanReorder(convldRads, XLen * 3, YLen * 3);

        FlatField swath = makeFlatField(domainSet, new float[][]{convldRads});

        return swath;

    }
}
