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

public class Subset {

    private int[] start = null;
    private int[] count = null;
    private int[] stride = null;
    private int rank;

    public Subset(int rank) {
        this.rank = rank;
        start = new int[rank];
        count = new int[rank];
        stride = new int[rank];

        for (int t = 0; t < rank; t++) {
            start[t] = -1;
            count[t] = -1;
            stride[t] = -1;
        }
    }

    public int[] getStart() {
        return start;
    }

    public int[] getCount() {
        return count;
    }

    public int[] getStride() {
        return stride;
    }

    public int getRank() {
        return rank;
    }

}
