/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
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

import java.util.Comparator;

/**
 * @author tommyj
 * Sorts NOAA format VIIRS variable names. For example, ensures:
 * 
 * All_Data/VIIRS-M2-SDR_All/Radiance
 * 
 * appears before:
 * 
 * All_Data/VIIRS-M16-SDR_All/Radiance
 * 
 * Using natural ordering, it will not, since the band numbers are
 * not zero-padded.
 *
 */

public class VIIRSSort implements Comparator<String> {

    @Override
    public int compare(String v1, String v2) {
        int result = 0;
        String band1 = null;
        String band2 = null;
        int index1 = -1;
        int index2 = -1;
        
        // Do the regular natural ordering check first
        if ((v1 != null) && (v2 != null)) {
            
            result = v1.compareTo(v2);
            
            // Assume caller is testing on NOAA-format VIIRS data,
            // but do some basic checks just in case.  If true,
            // apply a further filter based on Band/Product token

            if ((v1.contains("VIIRS")) && (v2.contains("VIIRS"))) {
                
                // pull band out of 1st variable name
                index1 = v1.indexOf('-');
                index2 = v1.indexOf('-', index1 + 1);

                // band or product is there if both indices are non-negative
                if ((index1 >= 0) && (index2 >= 0)) {
                    band1 = v1.substring(index1 + 1, index2);
                }

                // pull band out of 2nd variable name
                index1 = v2.indexOf('-');
                index2 = v2.indexOf('-', index1 + 1);

                // band or product is there if both indices are non-negative
                if ((index1 >= 0) && (index2 >= 0)) {
                    band2 = v2.substring(index1 + 1, index2);
                }

                // if band variables are still null, we can try one more thing -
                // an underscore instead of dash for delimiter in variable name
                if ((band1 == null) && (band2 == null)) {

                    // pull band out of 1st variable name
                    index1 = v1.indexOf('_');
                    index2 = v1.indexOf('_', index1 + 1);

                    // band or product is there if both indices are non-negative
                    if ((index1 >= 0) && (index2 >= 0)) {
                        band1 = v1.substring(index1 + 1, index2);
                    }

                    // pull band out of 2nd variable name
                    index1 = v2.indexOf('_');
                    index2 = v2.indexOf('_', index1 + 1);

                    // band or product is there if both indices are non-negative
                    if ((index1 >= 0) && (index2 >= 0)) {
                        band2 = v2.substring(index1 + 1, index2);
                    }

                }
                
                if ((band1 != null) && (band2 != null)) {
                    // zero pad if needed
                    if (band1.length() == 2) {
                        band1 = String.join("0", band1.substring(0, 1), band1.substring(1));
                    }
                    if (band2.length() == 2) {
                        band2 = String.join("0", band2.substring(0, 1), band2.substring(1));
                    }
                    result = band1.compareTo(band2);
                }
            }
        }
        
        return result;
    }

}
