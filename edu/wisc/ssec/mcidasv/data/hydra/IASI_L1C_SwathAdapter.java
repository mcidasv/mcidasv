/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2019
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
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
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.HashMap;
import java.util.Map;

import visad.FlatField;
import visad.Set;

public class IASI_L1C_SwathAdapter extends SwathAdapter {

   public IASI_L1C_SwathAdapter() {
   }

   public IASI_L1C_SwathAdapter(MultiDimensionReader reader, Map<String, Object> metadata) {
     super(reader, metadata);
   }

   protected void setLengths() {
     int len = getTrackLength();
     setTrackLength(len *= 2);
     len = getXTrackLength();
     setXTrackLength( len /= 2);
   }

   public FlatField getData(Map<String, double[]> subset) throws Exception {
     Set domainSet = makeDomain(subset);

     Map<String, double[]> new_subset = new HashMap<>(subset);

     double[] coords = new_subset.get(SwathAdapter.track_name);
     double[] new_coords = new double[] {0.0, coords[1]/2, 1.0};

     new_subset.put(SwathAdapter.track_name, new_coords);
     new_coords = new double[] {0.0, 119.0, 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_coords);

     return makeFlatField(domainSet, new_subset);
   }
}
