/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2023
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

package edu.wisc.ssec.mcidasv.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SandwichSpeedup {

    private static final Logger logger =
            LoggerFactory.getLogger(SandwichSpeedup.class);

    public static void sandwichSpeedup(float[] scaledFloats,
                                       float[] floatsIR,
                                       float[] rFloats,
                                       float[] gFloats,
                                       float[] bFloats,
                                       float[] rTable,
                                       float[] gTable,
                                       float[] bTable,
                                       float minIR,
                                       float maxIR,
                                       int nCols,
                                       float noIRContribution)
    {
        for (int i = 0; i < scaledFloats.length; i++) {
            // set anything colder than threshold to r,g,b from color table,
            // otherwise just set to 1 (so that result after multiply is just
            // the vis image)
            if (floatsIR[i] < maxIR) {

                // if anything falls below the minIR, set it to the minIR
                // (scaledFloats=0)
                if (floatsIR[i] < minIR) {
                    // java conversion note: "pixel" is just scaledFloats at
                    // current index
                    scaledFloats[i] = 0;
                }

                // need to convert float ranging from 0.0 to 1.0 into integer
                // index ranging from 0 to nCols testing
                int ind = (int)(scaledFloats[i] * nCols);

                rFloats[i] = rTable[ind];
                gFloats[i] = gTable[ind];
                bFloats[i] = bTable[ind];
            } else {
                rFloats[i] = noIRContribution; // previously was set to 1
                gFloats[i] = noIRContribution; // see note for rFloats
                bFloats[i] = noIRContribution; // see note for rFloats
            }
        }
    }
}
