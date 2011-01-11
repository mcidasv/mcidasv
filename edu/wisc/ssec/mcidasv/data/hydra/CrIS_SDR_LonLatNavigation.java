/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

import visad.Set;
import visad.Gridded2DSet;
import visad.Gridded2DDoubleSet;
import visad.Linear2DSet;
import visad.CoordinateSystem;
import visad.GridCoordinateSystem;
import visad.RealTupleType;
import java.util.HashMap;


public class CrIS_SDR_LonLatNavigation extends SwathNavigation  {


  public CrIS_SDR_LonLatNavigation(SwathAdapter swathAdapter) throws Exception {
    super(swathAdapter);
  }

  Gridded2DSet createInterpSet() throws Exception {

    int[] new_geo_start = new int[3]; 
    int[] new_geo_count = new int[3]; 
    int[] new_geo_stride = new int[3]; 

    new_geo_start[geo_xtrack_idx] = 0;
    new_geo_count[geo_xtrack_idx] = 30;
    new_geo_stride[geo_xtrack_idx] = 1;
    new_geo_start[geo_track_idx] = 0;
    new_geo_count[geo_track_idx] = geo_count[geo_track_idx]/3;
    new_geo_stride[geo_track_idx] = 1;
    new_geo_start[2] = 0;
    new_geo_count[2] = 9;
    new_geo_stride[2] = 1;

    float[] lons = reader.getFloatArray(lon_array_name, new_geo_start, new_geo_count, new_geo_stride);
    float[] lats = reader.getFloatArray(lat_array_name, new_geo_start, new_geo_count, new_geo_stride);

    /** will be different for CrIS
      lons = IASI_L1C_Utility.psuedoScanReorder2(lons, 60, geo_count[0]);
      lats = IASI_L1C_Utility.psuedoScanReorder2(lats, 60, geo_count[0]);
    */

    Gridded2DSet gset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
                   new float[][] {lons, lats},
                        geo_count[idx_order[0]], geo_count[idx_order[1]],
                            null, null, null, false, false);
    return gset;
  }
}
