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
                                                                                                                                                           
import java.util.HashMap;

import visad.FlatField;

public class CrIS_SDR_Spectrum extends SpectrumAdapter {

  public static int[] ifov_order = new int[] {8,5,2,7,4,1,6,3,0};

  public HashMap new_subset = new HashMap();

  public CrIS_SDR_Spectrum(MultiDimensionReader reader, HashMap metadata) {
    super(reader, metadata);
  }

  public int computeNumChannels() {
    int numChannels = 717;
    return numChannels;
  }

  public float[] getChannels() throws Exception {
    float[] spectrum = new float[numChannels];
    for (int k=0; k<numChannels; k++) {
       spectrum[k] = 649.75f + k*(0.625f);
    }
    return spectrum;
  }

   
  public FlatField getData(Object subset) throws Exception {
     new_subset.putAll((HashMap) subset);

     double[] xx = (double[]) ((HashMap)subset).get(SpectrumAdapter.x_dim_name);
     double[] yy = (double[]) ((HashMap)subset).get(SpectrumAdapter.y_dim_name);
     double[] new_xx = new double[3];
     double[] new_yy = new double[3];
     double[] new_kk = new double[3];

     int i = (int) xx[0]/3;
     int j = (int) yy[0]/3;

     int ii = ((int)xx[0]) - i*3;
     int jj = ((int)yy[0]) - j*3;

     int k = jj*3 + ii;

     new_yy[0] = j;
     new_yy[1] = j;
     new_yy[2] = 1;

     new_xx[0] = i;
     new_xx[1] = i;
     new_xx[2] = 1;

     new_kk[0] = ifov_order[k]; 
     new_kk[1] = ifov_order[k];
     new_kk[2] = 1;

     new_subset.put(SpectrumAdapter.x_dim_name, new_xx);
     new_subset.put(SpectrumAdapter.y_dim_name, new_yy);
     new_subset.put(SpectrumAdapter.FOVindex_name, new_kk);

     return super.getData(new_subset);
   }

}
