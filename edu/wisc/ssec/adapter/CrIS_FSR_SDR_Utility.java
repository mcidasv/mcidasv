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

import edu.wisc.ssec.hydra.data.MultiSpectralDataSource;
import visad.*;

public class CrIS_FSR_SDR_Utility {

    public static int[][] ifov_order = new int[][]{new int[]{2, 0}, new int[]{2, 1}, new int[]{2, 2},
            new int[]{1, 0}, new int[]{1, 1}, new int[]{1, 2},
            new int[]{0, 0}, new int[]{0, 1}, new int[]{0, 2}};

    public static int LW_CHANNELS = 713;
    public static int MW_CHANNELS = 865;
    public static int SW_CHANNELS = 633;

    public static float LW_INIT_SR = 650.0f;
    public static float MW_INIT_SR = 1210.0f;
    public static float SW_INIT_SR = 2155.0f;

    public static float LW_SR_INCR = 0.625f;
    public static float MW_SR_INCR = 0.625f;
    public static float SW_SR_INCR = 0.625f;

    public static float getWavenumberStart(String name) {
        if (name.endsWith("LW")) {
            return LW_INIT_SR;
        } else if (name.endsWith("MW")) {
            return MW_INIT_SR;
        } else if (name.endsWith("SW")) {
            return SW_INIT_SR;
        } else {
            return Float.NaN;
        }
    }

    public static float getWavenumberIncrement(String name) {
        if (name.endsWith("LW")) {
            return LW_SR_INCR;
        } else if (name.endsWith("MW")) {
            return MW_SR_INCR;
        } else if (name.endsWith("SW")) {
            return SW_SR_INCR;
        } else {
            return Float.NaN;
        }
    }

    public static int getNumChannels(String name) {
        if (name.endsWith("LW")) {
            return LW_CHANNELS;
        } else if (name.endsWith("MW")) {
            return MW_CHANNELS;
        } else if (name.endsWith("SW")) {
            return SW_CHANNELS;
        } else {
            return -1;
        }
    }

    public static float[] psuedoScanReorder(float[] values, int numElems, int numLines) {
        float[] new_values = new float[values.length];
        for (int j = 0; j < numLines / 3; j++) { //- loop over EFOVs or FORs
            for (int i = 0; i < numElems / 3; i++) {
                int i2 = i * 3;
                int j2 = j * 3;
                for (int jj = 0; jj < 3; jj++) {  //- loop over IFOVs
                    for (int ii = 0; ii < 3; ii++) {
                        int k = jj * 3 + ii;
                        int idx_ma = j * (numElems / 3 * 9) + i * 9 + k;
                        int idx_a = (j2 + ifov_order[k][0]) * numElems + i2 + ifov_order[k][1];  // idx_a: aligned
                        new_values[idx_a] = values[idx_ma];
                    }
                }
            }
        }
        return new_values;
    }

    public static FlatField makeViewableCrISpreview(FlatField image) throws Exception {

        FunctionType ftype = (FunctionType) image.getType();
        RealType rngType = (RealType) ftype.getRange();
        RealTupleType domain = ftype.getDomain();
        RealType[] rcomps = domain.getRealComponents();
        LongitudeLatitudeCoordinateSystem cs = (LongitudeLatitudeCoordinateSystem) domain.getCoordinateSystem();
        Gridded2DSet gset = cs.getTheGridded2DSet();
        Linear2DSet domSet = (Linear2DSet) image.getDomainSet();

        float[][] lonlat = gset.getSamples(false);
        float[][] rngVals = image.getFloats(false);

        int[] lens = domSet.getLengths();
        int lenX = lens[0];
        int lenY = lens[1];

        int subLenX = lenX / 3;
        int subLenY = lenY / 3;

        float[][] subLonLat = new float[2][subLenX * subLenY];
        float[][] subRange = new float[1][subLenX * subLenY];

        int cnt = 0;
        for (int j = 0; j < subLenY; j++) {
            for (int i = 0; i < subLenX; i++) {
                int idx = 1 + (j * 3) * subLenX * 3 + i * 3;
                subLonLat[0][cnt] = lonlat[0][idx];
                subLonLat[1][cnt] = lonlat[1][idx];
                subRange[0][cnt] = rngVals[0][idx];
                cnt++;
            }
        }

        float lastX = 1 + (subLenX - 1) * 3;
        float lastY = 1 + (subLenY - 1) * 3;

        Linear2DSet subDomSet = new Linear2DSet(1, lastX - 1, subLenX, 1, lastY - 1, subLenY);

        gset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
                subLonLat, subLenX, subLenY,
                null, null, null, false, false);

        cs = new LongitudeLatitudeCoordinateSystem(subDomSet, gset);


        domain = new RealTupleType(rcomps[0], rcomps[1], cs, null);
        ftype = new FunctionType(domain, rngType);

        image = new FlatField(ftype, subDomSet);
        image.setSamples(subRange);

        return image;
    }

    public static FlatField reprojectCrIS_SDR_swath(FlatField swath) throws Exception {

        float[][] corners = MultiSpectralData.getLonLatBoundingCorners(swath.getDomainSet());
        visad.georef.MapProjection mp = MultiSpectralDataSource.getSwathProjection(swath, corners);
        float res = 14400;
        Linear2DSet grid = MultiSpectralDataSource.makeGrid(mp, corners, res);
        Linear1DSet set0 = grid.getLinear1DComponent(0);
        Linear1DSet set1 = grid.getLinear1DComponent(1);

        // expand the grid a little bit
        double first0 = set0.getFirst() - 7 * res;
        double first1 = set1.getFirst() - 7 * res;
        double last0 = set0.getLast() + 9 * res;
        double last1 = set1.getLast() + 9 * res;
        int len0 = set0.getLength() + 16;
        int len1 = set1.getLength() + 16;

        grid = new Linear2DSet(grid.getType(), first0, last0, len0, first1, last1, len1);

        return ReprojectSwath.swathToGrid(grid, swath, 0);
    }
}
