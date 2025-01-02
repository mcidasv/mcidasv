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


public class IASI_L1C_NCDF_Spectrum extends SpectrumAdapter {

    public static int[][] ifov_order2 = new int[][]{new int[]{1, 1}, new int[]{0, -1}, new int[]{0, 0}, new int[]{-1, 0}};

    public HashMap new_subset = new HashMap();

    private float[] scaleFactors = null;

    public IASI_L1C_NCDF_Spectrum(MultiDimensionReader reader, HashMap metadata) {
        super(reader, metadata);
        try {
            scaleFactors = reader.getFloatArray("scale_factor", new int[]{0}, new int[]{numChannels}, new int[]{1});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public float[] getChannels() throws Exception {
        float[] chans = super.getChannels();
        int cnt = 0;
        for (int k = 0; k < chans.length; k++) {
            chans[k] /= 100f; // m-1 to cm-1
            if (chans[k] > 0f) cnt++;
        }
        // remove zeros at the end, reset numChannels
        float[] new_chans = new float[cnt];
        System.arraycopy(chans, 0, new_chans, 0, cnt);
        numChannels = cnt;
        return new_chans;
    }

    public FlatField getData(Object subset) throws Exception {
        new_subset.putAll((HashMap) subset);

        double[] xx = (double[]) ((HashMap) subset).get(SpectrumAdapter.x_dim_name);
        double[] yy = (double[]) ((HashMap) subset).get(SpectrumAdapter.y_dim_name);
        double[] new_xx = new double[3];
        double[] new_yy = new double[3];

        int i = (int) xx[0] / 2;
        int j = (int) yy[0] / 2;

        int ii = ((int) xx[0]) - i * 2;
        int jj = ((int) yy[0]) - j * 2;

        int k = jj * 2 + ii;
        int idx = j * 120 + i * 4 + (jj + ifov_order2[k][0]) * 2 + (ii + ifov_order2[k][1]);

        double y = (double) ((int) (idx / 120));
        double x = idx - (int) y * 120;

        new_yy[0] = y;
        new_yy[1] = y;
        new_yy[2] = 1;

        new_xx[0] = x;
        new_xx[1] = x;
        new_xx[2] = 1;

        new_subset.put(SpectrumAdapter.x_dim_name, new_xx);
        new_subset.put(SpectrumAdapter.y_dim_name, new_yy);

        return super.getData(new_subset);
    }

    public float[] processRange(short[] range, Object subset) {
        float[] new_range = new float[numChannels];
        for (int k = 0; k < numChannels; k++) {
            new_range[k] = (100000f * range[k]) / (scaleFactors[k]);
        }
        return new_range;
    }

}
