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
 * McIDAS-V is built on Unidata's IDV, as well as SSEC's VisAD and HYDRA
 * projects. Parts of McIDAS-V source code are based on IDV, VisAD, and
 * HYDRA source code.
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
                                                                                                                                                           
import java.util.ArrayList;
import java.util.HashMap;

import visad.Set;

public class CrIS_NASA_Spectrum extends SpectrumAdapter {

  public static int[] ifov_order = new int[] {8,5,2,7,4,1,6,3,0};

  public HashMap new_subset = new HashMap();
  
  public CrIS_NASA_Spectrum(MultiDimensionReader reader, HashMap metadata) {
    super(reader, metadata);
  }
  
  public int computeNumChannels() {	  
     String arrayName = (String) metadata.get("array_name");
     if (arrayName.contains("lw")) {
        numChannels = 713;
     }
     else if (arrayName.contains("mw")) {
        numChannels = 865;
     }
     else if (arrayName.contains("sw")) {
        numChannels = 633;
     }
	  
     return numChannels;
  }

  public float[] getChannels() throws Exception {
    String arrayName = (String) metadata.get(SpectrumAdapter.channels_name);
    ArrayList<NetCDFFile> rdrs = ((GranuleAggregation)reader).getReaders();
    NetCDFFile rdr = rdrs.get(0);
        
    double[] spectrum = rdr.getDoubleArray(arrayName, new int[] {2}, new int[] {numChannels}, new int[] {1});
    float[][] fltvalues = Set.doubleToFloat(new double[][] {spectrum});

    return fltvalues[0];
  }
  
  public float getInitialWavenumber() {
    return CrIS_SDR_Utility.getWavenumberStart(getArrayName());
  }

}
