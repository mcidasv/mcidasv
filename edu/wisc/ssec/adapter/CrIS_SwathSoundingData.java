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

import java.rmi.RemoteException;

import visad.FlatField;
import visad.Gridded2DSet;
import visad.RealTuple;
import visad.RealTupleType;
import visad.VisADException;

public class CrIS_SwathSoundingData extends SwathSoundingData {

    SwathNavigation swathNav = null;
    SwathAdapter swathAdapter = null;
    private float[][] lonlat = null;

    public CrIS_SwathSoundingData(SwathAdapter swathAdapter, AtmSoundingAdapter soundingAdapter) {
        super(swathAdapter, soundingAdapter, null, null);
        this.swathAdapter = swathAdapter;
        try {
            swathNav = swathAdapter.getNavigation();
            //LongitudeLatitudeCoordinateSystem cs = (LongitudeLatitudeCoordinateSystem) swathNav.getVisADCoordinateSystem(null, swathAdapter.getDefaultSubset());
            LongitudeLatitudeCoordinateSystem cs = (LongitudeLatitudeCoordinateSystem) swathNav.getVisADCoordinateSystem(swathAdapter.getDefaultSubset());
            Gridded2DSet gset = cs.getTheGridded2DSet();
            float[][] tmp = gset.getSamples(false);
            float[] lons = new float[tmp[0].length];
            float[] lats = new float[tmp[0].length];
            System.arraycopy(tmp[0], 0, lons, 0, lons.length);
            System.arraycopy(tmp[1], 0, lats, 0, lats.length);
            lonlat = new float[][]{lons, lats};
        } catch (Exception e) {
        }
    }

    public FlatField getSounding(int[] coords)
            throws Exception, VisADException, RemoteException {
        if (coords == null) return null;

        int ii = 0;
        int jj = 0;
        int kk = 0;

        double[] scan = new double[]{jj, jj, 1.0};
        double[] step = new double[]{ii, ii, 1.0};
        double[] fov = new double[]{kk, kk, 1.0};

        soundingSelect.put(AtmSoundingAdapter.x_dim_name, step);
        soundingSelect.put(AtmSoundingAdapter.y_dim_name, scan);
        soundingSelect.put(AtmSoundingAdapter.FOVindex_name, fov);

        FlatField sounding = soundingAdapter.getData(soundingSelect);
        float[][] vals = sounding.getFloats(false);
        for (int k = 0; k < vals[0].length; k++) {
            if (vals[0][k] == -9999.0) vals[0][k] = Float.NaN;
        }
        return sounding;
    }

    public FlatField getSounding(RealTuple location)
            throws Exception, VisADException, RemoteException {

        double[] tmp = location.getValues();
        float[][] loc = new float[][]{{(float) tmp[1]}, {(float) tmp[0]}};
        if (loc[0][0] > 180) loc[0][0] -= 360;

        int kk = -1;
        int ii = 0;
        int jj = 0;

        float[][] lonlatFOV = new float[2][9];

        int trkLen = swathAdapter.getTrackLength();
        trkLen /= 3;
        scanloop:
        for (jj = 0; jj < trkLen; jj++) {
            for (ii = 0; ii < 30; ii++) {
                int start = jj * 270 + ii * 3;
                for (int n = 0; n < 3; n++) {
                    for (int m = 0; m < 3; m++) {
                        int idx = n * 3 + m;
                        int k = start + n * 90 + m;
                        lonlatFOV[0][idx] = lonlat[0][k];
                        lonlatFOV[1][idx] = lonlat[1][k];
                    }
                }
                Gridded2DSet gsetFOV = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, lonlatFOV, 3, 3);
                int[] idx = gsetFOV.valueToIndex(loc);
                kk = idx[0];
                if (kk >= 0) {
                    break scanloop;
                }
            }
        }

        if (kk < 0) { // incoming (lon,lat) not inside any 3x3 box
            return null;
        } else {
            int n = kk / 3;
            int m = kk % 3;
            int i = ii * 3 + m;
            int j = jj * 3 + n;
            double[] scan = new double[]{j, j, 1.0};
            double[] step = new double[]{i, i, 1.0};

            soundingSelect.put(AtmSoundingAdapter.x_dim_name, step);
            soundingSelect.put(AtmSoundingAdapter.y_dim_name, scan);

            FlatField sounding = soundingAdapter.getData(soundingSelect);
            float[][] vals = sounding.getFloats(false);
            for (int k = 0; k < vals[0].length; k++) {
                if (vals[0][k] == -9999.0) vals[0][k] = Float.NaN;
            }
            return sounding;
        }
    }
}
