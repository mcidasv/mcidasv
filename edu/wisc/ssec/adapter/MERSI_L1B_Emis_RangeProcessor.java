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

import java.util.HashMap;
import java.util.ArrayList;
import visad.util.Util;

public class MERSI_L1B_Emis_RangeProcessor extends RangeProcessor {

        double C1 = 1.1910659/100000;
        double C2 = 1.438833;

        double B = 1.0103;
        double A = -1.8521;

        double freq = 875.1379;
        double freq3 = freq*freq*freq;

        public MERSI_L1B_Emis_RangeProcessor() throws Exception {
        }

        public float[] processRange(short[] values, HashMap subset) {
           float[] fltValues = new float[values.length];

           for (int k=0; k<values.length; k++) {
              double rad = values[k]/100.0;

              double BT = C2*freq/Math.log((C1*freq3/rad) + 1);

              BT = BT*B + A;

              fltValues[k] = (float) BT;
           }

           return fltValues;
        }
}
