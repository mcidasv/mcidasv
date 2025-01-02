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

import edu.wisc.ssec.hydra.data.DataSelection;

import java.util.HashMap;
import java.util.Iterator;


public class MultiDimensionSubset extends DataSelection {

    public static final MultiDimensionSubset key = new MultiDimensionSubset();

    private final HashMap<String, double[]> coordsMap = new HashMap<>();

    public MultiDimensionSubset() {
        super();
    }

    public MultiDimensionSubset(HashMap<String, double[]> subset) {
        super();

        Iterator<String> iter = subset.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            double[] coords = (double[]) subset.get(key);
            coordsMap.put(key, coords);
        }
    }


    public HashMap getSubset() {
        HashMap hmap = new HashMap();
        Iterator iter = coordsMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            double[] coords = coordsMap.get(key);
            double[] new_coords = new double[coords.length];
            System.arraycopy(coords, 0, new_coords, 0, new_coords.length);
            hmap.put(key, new_coords);
        }
        return hmap;
    }


    public double[] getCoords(String key) {
        double[] dblA = new double[3];
        double[] tmp = coordsMap.get(key);
        if (tmp == null) {
            return null;
        }
        System.arraycopy(tmp, 0, dblA, 0, dblA.length);
        return dblA;
    }


    public void setCoords(String key, double[] rpl) {
        coordsMap.put(key, rpl);
    }

    public void setCoords(HashMap subset) {
        Iterator iter = subset.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            double[] coords = (double[]) subset.get(key);
            double[] new_coords = new double[coords.length];
            System.arraycopy(coords, 0, new_coords, 0, new_coords.length);
            coordsMap.put(key, new_coords);
        }
    }

    public boolean isEmtpy() {
        return coordsMap.isEmpty();
    }

    public MultiDimensionSubset clone() {
        MultiDimensionSubset subset = new MultiDimensionSubset(getSubset());
        return subset;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        Iterator<String> iter = coordsMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            double[] coords = coordsMap.get(key);
            sb.append(new String(key + ": " + coords[0] + ", " + coords[1] + ", " + coords[2] + "\n"));
        }
        return sb.toString();
    }
}
