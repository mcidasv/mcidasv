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

import visad.CoordinateSystem;
import visad.VisADException;
import visad.RealTupleType;
import visad.RealType;
import visad.Linear2DSet;
import visad.Gridded2DSet;
import visad.Gridded1DSet;
import visad.Set;

import java.awt.geom.Rectangle2D;

//import visad.data.hdfeos.LambertAzimuthalEqualArea;
import edu.wisc.ssec.adapter.LambertAzimuthalEqualArea;
import visad.Data;


public class LongitudeLatitudeCoordinateSystem extends CoordinateSystem {
//public class LongitudeLatitudeCoordinateSystem extends MapProjection {

    Linear2DSet domainSet;
    Linear2DSet subSet;
    Gridded2DSet gset;
    Gridded2DSet projSet;
    CoordinateSystem projCS;
    float[][] lonlat;

    Gridded1DSet goodLinesSet;

    //- assumes incoming GriddedSet is (longitude,latitude) with range (-180,+180)
    boolean neg180pos180 = true;  //false: longitude range (0,+360)

    boolean needInterp = true;

    public LongitudeLatitudeCoordinateSystem(Linear2DSet domainSet, Gridded2DSet gset) throws VisADException {
        this(domainSet, gset, false);
    }

    public LongitudeLatitudeCoordinateSystem(Linear2DSet domainSet, Gridded2DSet gset, boolean lonFlag) throws VisADException {
        super(RealTupleType.SpatialEarth2DTuple, null);
        this.gset = gset;
        this.domainSet = domainSet;
        this.neg180pos180 = lonFlag;
        int[] lengths = domainSet.getLengths();
        int[] gset_lengths = gset.getLengths();
        subSet = new Linear2DSet(0.0, gset_lengths[0] - 1, lengths[0],
                0.0, gset_lengths[1] - 1, lengths[1]);

        lonlat = gset.getSamples(false);

        if ((lengths[0] == gset_lengths[0]) && (lengths[1] == gset_lengths[1])) {
            needInterp = false;
        }

        // Check for missing (complete) scan lines.
        int[] goodLines = checkForMissingLines(lonlat, gset_lengths[0], gset_lengths[1]);
        int lenY = gset_lengths[1];
        if (goodLines != null) {
            lonlat = removeMissingLines(lonlat, gset_lengths[0], gset_lengths[1], goodLines);
            lenY = goodLines.length;
            float[] flts = new float[lenY];
            for (int k = 0; k < lenY; k++) {
                flts[k] = (float) goodLines[k];
            }
            goodLinesSet = new Gridded1DSet(RealType.Generic, new float[][]{flts}, lenY);
        }

        // Create map projected set of locations to handle problems using Lon/Lat
        // coordinates around the dateline, prime-meridian or poles.
        // Use missing removed lonlat array to get center lon,lat
        int gxc = gset_lengths[0] / 2;
        int gyc = gset_lengths[1] / 2;
        int iic = gyc * gset_lengths[0] + gxc;
        float[][] cntr = new float[][]{{lonlat[0][iic]}, {lonlat[1][iic]}};

        float earthRadius = 6367470;
        float lonCenter = cntr[0][0];
        float latCenter = cntr[1][0];
        projCS = new LambertAzimuthalEqualArea(RealTupleType.SpatialEarth2DTuple, earthRadius,
                lonCenter * Data.DEGREES_TO_RADIANS, latCenter * Data.DEGREES_TO_RADIANS,
                0, 0);

        float[][] grdLocs = projCS.fromReference(lonlat);

        projSet = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                grdLocs, gset_lengths[0], lenY,
                null, null, null, false, false);
    }

    public float[][] toReference(float[][] values) throws VisADException {

        if (!needInterp) {
            return toReferenceNoInterp(values);
        }

        float[][] coords = domainSet.valueToGrid(values);
        coords = subSet.gridToValue(coords);

        // Use nearest neigbhor if grid box stradles dateline because of resulting
        // interpolation problems.

        int[] lens = gset.getLengths();
        int lenX = lens[0];
        float[][] lonlat = gset.getSamples(false);

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        float lon;
        int gx, gy, idx;

        for (int i = 0; i < coords[0].length; i++) {
            gx = (int) coords[0][i];
            gy = (int) coords[1][i];
            idx = gy * lenX + gx;

            min = Float.MAX_VALUE;
            max = -Float.MAX_VALUE;

            // look at grid cell corners
            lon = lonlat[0][idx];
            if (lon < min) min = lon;
            if (lon > max) max = lon;

            if ((gx + 1) < lens[0]) {
                lon = lonlat[0][idx + 1];
                if (lon < min) min = lon;
                if (lon > max) max = lon;
            }

            if ((gy + 1) < lens[1]) {
                lon = lonlat[0][idx + lenX];
                if (lon < min) min = lon;
                if (lon > max) max = lon;
            }

            if (((gx + 1) < lens[0]) && ((gy + 1) < lens[1])) {
                lon = lonlat[0][idx + lenX + 1];
                if (lon < min) min = lon;
                if (lon > max) max = lon;
            }

            if ((max - min) > 300) { // grid cell probably stradles the dateline so force nearest neighbor
                coords[0][i] = (float) Math.floor(coords[0][i] + 0.5);
                coords[1][i] = (float) Math.floor(coords[1][i] + 0.5);
            }
        }

        coords = gset.gridToValue(coords);
        // original set of lon,lat may contain fill values so perform a valid lat range check
        for (int k = 0; k < coords[0].length; k++) {
            if (Math.abs(coords[1][k]) > 90) {
                coords[0][k] = Float.NaN;
                coords[1][k] = Float.NaN;
            }
        }
        return coords;
    }

    private float[][] toReferenceNoInterp(float[][] values) throws VisADException {
        float[][] coords = domainSet.valueToGrid(values);

        int[] lens = gset.getLengths();
        int lenX = lens[0];
        float[][] lonlat = gset.getSamples(false);

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        float lon;
        float lat;
        int gx, gy, idx;

        for (int i = 0; i < coords[0].length; i++) {
            gx = (int) coords[0][i];
            gy = (int) coords[1][i];
            idx = gy * lenX + gx;

            min = Float.MAX_VALUE;
            max = -Float.MAX_VALUE;

            // look at grid cell corners
            lon = lonlat[0][idx];
            lat = lonlat[1][idx];

            coords[0][i] = lon;
            coords[1][i] = lat;
        }

        // original set of lon,lat may contain fill values so perform a valid lat range check
        for (int k = 0; k < coords[0].length; k++) {
            if (Math.abs(coords[1][k]) > 90) {
                coords[0][k] = Float.NaN;
                coords[1][k] = Float.NaN;
            }
        }
        return coords;
    }

    public float[][] fromReference(float[][] values) throws VisADException {
        if (!neg180pos180) { // force to longitude range (0,360)
            for (int t = 0; t < values[0].length; t++) {
                if (values[0][t] > 180f) {
                    values[0][t] -= 360f;
                }
            }
        }

        //float[][] grid_vals = gset.valueToGrid(values);
        // use the projected set
        values = projCS.fromReference(values);
        float[][] grid_vals = projSet.valueToGrid(values);

        // return original domain coordinates if missing geo lines were removed
        if (goodLinesSet != null) {
            float[][] tmp = goodLinesSet.gridToValue(new float[][]{grid_vals[1]});
            grid_vals[1] = tmp[0];
        }

        float[][] coords = subSet.valueToGrid(grid_vals);
        coords = domainSet.gridToValue(coords);
        return coords;
    }

    public double[][] toReference(double[][] values) throws VisADException {
        float[][] coords = domainSet.valueToGrid(Set.doubleToFloat(values));
        if (!needInterp) {
            return Set.floatToDouble(toReferenceNoInterp(coords));
        }

        coords = subSet.gridToValue(coords);

        // Use nearest neigbhor if grid box stradles dateline because of resulting
        // interpolation problems.

        int[] lens = gset.getLengths();
        int lenX = lens[0];

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        float lon;
        int gx, gy, idx;

        for (int i = 0; i < coords[0].length; i++) {
            gx = (int) coords[0][i];
            gy = (int) coords[1][i];
            idx = gy * lenX + gx;

            min = Float.MAX_VALUE;
            max = -Float.MAX_VALUE;

            lon = lonlat[0][idx];
            if (lon < min) min = lon;
            if (lon > max) max = lon;

            if ((gx + 1) < lens[0]) {
                lon = lonlat[0][idx + 1];
                if (lon < min) min = lon;
                if (lon > max) max = lon;
            }

            if ((gy + 1) < lens[1]) {
                lon = lonlat[0][idx + lenX];
                if (lon < min) min = lon;
                if (lon > max) max = lon;
            }

            if (((gx + 1) < lens[0]) && ((gy + 1) < lens[1])) {
                lon = lonlat[0][idx + lenX + 1];
                if (lon < min) min = lon;
                if (lon > max) max = lon;
            }

            if ((max - min) > 300) { // grid cell probably stradles the dateline so force nearest neighbor
                coords[0][i] = (float) Math.floor(coords[0][i] + 0.5);
                coords[1][i] = (float) Math.floor(coords[1][i] + 0.5);
            }
        }

        coords = gset.gridToValue(coords);
        // original set of lon,lat may contain fill values so perform a valid lat range check
        for (int k = 0; k < coords[0].length; k++) {
            if (Math.abs(coords[1][k]) > 90) {
                coords[0][k] = Float.NaN;
                coords[1][k] = Float.NaN;
            }
        }
        return Set.floatToDouble(coords);
    }

    private double[][] toReferenceNoInterp(double[][] values) throws VisADException {
        float[][] coords = domainSet.valueToGrid(Set.doubleToFloat(values));

        int[] lens = gset.getLengths();
        int lenX = lens[0];
        float[][] lonlat = gset.getSamples(false);

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        float lon;
        float lat;
        int gx, gy, idx;

        for (int i = 0; i < coords[0].length; i++) {
            gx = (int) coords[0][i];
            gy = (int) coords[1][i];
            idx = gy * lenX + gx;

            min = Float.MAX_VALUE;
            max = -Float.MAX_VALUE;

            // look at grid cell corners
            lon = lonlat[0][idx];
            lat = lonlat[1][idx];

            coords[0][i] = lon;
            coords[1][i] = lat;
        }

        // original set of lon,lat may contain fill values so perform a valid lat range check
        for (int k = 0; k < coords[0].length; k++) {
            if (Math.abs(coords[1][k]) > 90) {
                coords[0][k] = Float.NaN;
                coords[1][k] = Float.NaN;
            }
        }
        return Set.floatToDouble(coords);
    }

    public double[][] fromReference(double[][] values) throws VisADException {
        if (!neg180pos180) { // force to longitude range (0,360)
            for (int t = 0; t < values[0].length; t++) {
                if (values[0][t] > 180.0) {
                    values[0][t] -= 360.0;
                }
            }
        }

        //float[][] grid_vals = gset.valueToGrid(Set.doubleToFloat(values));
        // use the projected set
        float[][] grid_vals = projSet.valueToGrid(Set.doubleToFloat(values));

        // return original domain coordinates if missing geo lines were removed
        if (goodLinesSet != null) {
            float[][] tmp = goodLinesSet.gridToValue(new float[][]{grid_vals[1]});
            grid_vals[1] = tmp[0];
        }

        float[][] coords = subSet.valueToGrid(grid_vals);
        coords = domainSet.gridToValue(coords);
        return Set.floatToDouble(coords);
    }

    public Rectangle2D getDefaultMapArea() {
        float[] lo = domainSet.getLow();
        float[] hi = domainSet.getHi();
        return new Rectangle2D.Float(lo[0], lo[1], hi[0] - lo[0], hi[1] - lo[1]);
    }

    public boolean equals(Object obj) {
        if (obj instanceof LongitudeLatitudeCoordinateSystem) {
            LongitudeLatitudeCoordinateSystem llcs = (LongitudeLatitudeCoordinateSystem) obj;
            if (domainSet.equals(llcs.domainSet) && (gset.equals(llcs.gset))) {
                return true;
            }
        }
        return false;
    }

    public int[] checkForMissingLines(float[][] lonlat, int lenX, int lenY) {
        int[] good_lines = new int[lenY];
        int cnt = 0;
        for (int j = 0; j < lenY; j++) {
            int idx = j * lenX + lenX / 2; // check scan line center: NaN and valid Lat range check
            if ((!(Float.isNaN(lonlat[0][idx]) || Float.isNaN(lonlat[1][idx]))) && (Math.abs(lonlat[1][idx]) <= 90.0)) {
                good_lines[cnt++] = j;
            }
        }

        if (cnt == lenY) {
            return null;
        } else {
            int[] tmp = new int[cnt];
            System.arraycopy(good_lines, 0, tmp, 0, cnt);
            good_lines = tmp;
            return good_lines;
        }
    }

    public float[][] removeMissingLines(float[][] lonlat, int lenX, int lenY, int[] good_lines) {
        float[][] noMissing = new float[2][lenX * (good_lines.length)];

        for (int k = 0; k < good_lines.length; k++) {

            int idx = good_lines[k] * lenX;

            System.arraycopy(lonlat[0], idx, noMissing[0], k * lenX, lenX);
            System.arraycopy(lonlat[1], idx, noMissing[1], k * lenX, lenX);
        }

        return noMissing;
    }

    public Gridded2DSet getTheGridded2DSet() {
        return gset;
    }
}
