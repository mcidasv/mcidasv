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

import visad.FlatField;
import visad.Set;

import java.util.HashMap;


public class IASI_L1C_SwathAdapter extends SwathAdapter {

    public IASI_L1C_SwathAdapter(MultiDimensionReader reader, HashMap metadata) {
        super(reader, metadata);
    }

    protected void setLengths() {
        int len = getTrackLength();
        setTrackLength(len *= 2);
        len = getXTrackLength();
        setXTrackLength(len /= 2);
    }

    public FlatField getData(Object subset) throws Exception {

        HashMap new_subset = (HashMap) ((HashMap) subset).clone();
        new_subset.putAll((HashMap) subset);

        // reform subset to integral numbers of EFOV (FORs)
        // you may not get exactly what you ask for in this case.

        double[] coords = (double[]) new_subset.get(SwathAdapter.track_name);
        double[] new_trk_coords = new double[]{2.0 * Math.floor((coords[0]) / 2), 2.0 * Math.floor((coords[1] + 1) / 2) - 1, 1.0};
        new_subset.put(SwathAdapter.track_name, new_trk_coords);

        coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
        double[] new_xtrk_coords = new double[]{2.0 * Math.floor((coords[0]) / 2), 2.0 * Math.floor((coords[1] + 1) / 2) - 1, 1.0};
        new_subset.put(SwathAdapter.xtrack_name, new_xtrk_coords);

        Set domainSet = makeDomain(new_subset);

        // transform the integral swath EFOV coordinates to dataset storage indexes.

        new_subset = (HashMap) ((HashMap) subset).clone();
        new_subset.putAll((HashMap) subset);

        new_trk_coords = new double[]{new_trk_coords[0] / 2, ((new_trk_coords[1] + 1) / 2) - 1, 1.0};
        new_subset.put(SwathAdapter.track_name, new_trk_coords);

        new_xtrk_coords = new double[]{2.0 * new_xtrk_coords[0], (2.0 * (new_xtrk_coords[1] + 1)) - 1, 1.0};
        new_subset.put(SwathAdapter.xtrack_name, new_xtrk_coords);

        return makeFlatField(domainSet, new_subset);
    }
}
