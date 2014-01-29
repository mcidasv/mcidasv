/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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
  
  private float initialSpectralResolution = 0f;
  private float spectralIncrement = 0f;

  public CrIS_SDR_Spectrum(MultiDimensionReader reader, HashMap metadata) {
    super(reader, metadata);
  }

  /**
   * A valid CrIS product/variable name will always end with two capital
   * letters for spectral range, LW: longwave, MW: mediumwave, SW: shortwave
   * @param productName
   * @return
   */
  
  public int computeNumChannels() {
	  
     String arrayName = (String) metadata.get("array_name");
     int numChannels = CrIS_SDR_Utility.getNumChannels(arrayName);
     initialSpectralResolution = CrIS_SDR_Utility.getWavenumberStart(arrayName);
     spectralIncrement = CrIS_SDR_Utility.getWavenumberIncrement(arrayName);
	  
     return numChannels;
  }

  public float[] getChannels() throws Exception {
    float[] spectrum = new float[numChannels];
    for (int k=0; k < numChannels; k++) {
       spectrum[k] = initialSpectralResolution + k * (spectralIncrement);
    }
    return spectrum;
  }
  
  public float getInitialWavenumber() {
    return CrIS_SDR_Utility.getWavenumberStart(getArrayName());
  }

}
