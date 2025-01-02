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

import edu.wisc.ssec.hydra.GEOSProjection;

import java.util.HashMap;

import visad.Linear2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.Set;
import visad.georef.MapProjection;

public class GOESGridAdapter extends GeoSfcAdapter {

    public static String gridX_name = "GridX";
    public static String gridY_name = "GridY";

    RealType gridx = RealType.getRealType(gridX_name);
    RealType gridy = RealType.getRealType(gridY_name);
    RealType[] domainRealTypes = new RealType[2];

    int GridXLen;
    int GridYLen;

    int gridx_idx;
    int gridy_idx;
    int gridx_tup_idx;
    int gridy_tup_idx;

    MapProjection mapProj;
    Linear2DSet datasetDomain;

    double default_stride = 10;

    Navigation navigation;

    private boolean zeroBased;

    public static HashMap getEmptySubset() {
        HashMap<String, double[]> subset = new HashMap<String, double[]>();
        subset.put(gridY_name, new double[3]);
        subset.put(gridX_name, new double[3]);
        return subset;
    }

    public static HashMap<String, Object> getEmptyMetadataTable() {
        HashMap<String, Object> metadata = new HashMap<String, Object>();
        metadata.put(array_name, null);
        metadata.put(gridX_name, null);
        metadata.put(gridY_name, null);
        metadata.put(scale_name, null);
        metadata.put(offset_name, null);
        metadata.put(fill_value_name, null);
        metadata.put(range_name, null);
        return metadata;
    }

    public GOESGridAdapter(MultiDimensionReader reader, HashMap metadata, MapProjection mapProj, double default_stride) {
        this(reader, metadata, mapProj, default_stride, true);
    }

    public GOESGridAdapter(MultiDimensionReader reader, HashMap metadata, MapProjection mapProj, double default_stride, boolean zeroBased) {
        super(reader, metadata);

        this.mapProj = mapProj;
        this.default_stride = default_stride;
        this.navigation = new GOESNavigation(mapProj);
        this.zeroBased = zeroBased;

        gridx_idx = getIndexOfDimensionName((String) metadata.get(gridX_name));
        GridXLen = getDimensionLengthFromIndex(gridx_idx);

        gridy_idx = getIndexOfDimensionName((String) metadata.get(gridY_name));
        GridYLen = getDimensionLengthFromIndex(gridy_idx);

        int[] lengths = new int[2];

        if (gridy_idx < gridx_idx) {
            domainRealTypes[0] = gridx;
            domainRealTypes[1] = gridy;
            lengths[0] = GridXLen;
            lengths[1] = GridYLen;
            gridy_tup_idx = 1;
            gridx_tup_idx = 0;
        } else {
            domainRealTypes[0] = gridy;
            domainRealTypes[1] = gridx;
            lengths[0] = GridYLen;
            lengths[1] = GridXLen;
            gridy_tup_idx = 0;
            gridx_tup_idx = 1;
        }

        lengths[gridy_tup_idx] = GridYLen;
        lengths[gridx_tup_idx] = GridXLen;

        try {
            RealTupleType domainTupType = new RealTupleType(domainRealTypes[0], domainRealTypes[1]);
            if (zeroBased) {
                datasetDomain = new Linear2DSet(domainTupType, 0, lengths[0] - 1, lengths[0], 0, lengths[1] - 1, lengths[1]);
            } else {
                datasetDomain = new Linear2DSet(domainTupType, 1, lengths[0], lengths[0], 1, lengths[1], lengths[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            setRangeProcessor(new RangeProcessor(getReader(), metadata));
        } catch (Exception e) {
            System.out.println("RangeProcessor failed to create.");
        }

    }

    public String getArrayName() {
        return rangeName;
    }

    public Set makeDomain(Object subset) throws Exception {
        double[] first = new double[2];
        double[] last = new double[2];
        int[] length = new int[2];

        // compute coordinates for the Linear2D domainSet
        for (int kk = 0; kk < 2; kk++) {
            RealType rtype = domainRealTypes[kk];
            String name = rtype.getName();
            double[] coords = (double[]) ((HashMap) subset).get(name);
            // replace with integral swath coordinates
            coords[0] = Math.ceil(coords[0]);
            coords[1] = Math.floor(coords[1]);
            first[kk] = coords[0];
            last[kk] = coords[1];
            length[kk] = (int) ((last[kk] - first[kk]) / coords[2] + 1);
            last[kk] = first[kk] + (length[kk] - 1) * coords[2];
        }

        if (zeroBased) {
            mapProj = new GEOSProjection((GEOSProjection) mapProj, first[0], first[1], last[0] - first[0], last[1] - first[1]);
        } else {
            mapProj = new GEOSProjection((GEOSProjection) mapProj, first[0] + 1, first[1] + 1, last[0] - first[0], last[1] - first[1]);
        }

        RealTupleType domainTupType = new RealTupleType(domainRealTypes[0], domainRealTypes[1], mapProj, null);

        Linear2DSet domainSet;
        if (zeroBased) {
            domainSet = new Linear2DSet(domainTupType, first[0], last[0], length[0], first[1], last[1], length[1]);
        } else {
            domainSet = new Linear2DSet(domainTupType, first[0] + 1, last[0] + 1, length[0], first[1] + 1, last[1] + 1, length[1]);
        }

        return domainSet;
    }

    public HashMap getDefaultSubset() {
        HashMap subset = GOESGridAdapter.getEmptySubset();

        double[] coords = (double[]) subset.get(gridY_name);
        if (zeroBased) {
            coords[0] = 0;
            coords[1] = GridYLen - 1;
        } else {
            coords[0] = 1.0;
            coords[1] = GridYLen;
        }
        coords[2] = default_stride;
        subset.put(gridY_name, coords);

        coords = (double[]) subset.get(gridX_name);
        if (zeroBased) {
            coords[0] = 0f;
            coords[1] = GridXLen - 1;
        } else {
            coords[0] = 1.0;
            coords[1] = GridXLen;
        }
        coords[2] = default_stride;
        subset.put(gridX_name, coords);

        return subset;
    }

    public void setDomainSet(Linear2DSet dset) {
        // No-op
    }

    public Set getDatasetDomain() {
        return datasetDomain;
    }

    public Navigation getNavigation() {
        return navigation;
    }
}
