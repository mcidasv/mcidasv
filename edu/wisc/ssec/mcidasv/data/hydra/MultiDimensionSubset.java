/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import ucar.unidata.data.DataSelection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


public class MultiDimensionSubset extends DataSelection {

  private double[][] coords = null;
  private String[] keys = null;

  public MultiDimensionSubset() {
    super();
  }

  public MultiDimensionSubset(HashMap subset) {
    super();
    coords = new double[subset.size()][];
    keys = new String[subset.size()];
    Iterator iter = subset.keySet().iterator();
    int cnt =0;
    while (iter.hasNext()) {
       String key = (String) iter.next();
       keys[cnt] = key;
       coords[cnt] = (double[]) subset.get(key);
       cnt++;
    }
  }

  public MultiDimensionSubset(double[][] coords, String[] keys) {
    this.coords = coords;
    this.keys = keys;
  }

  public HashMap getSubset() {
    HashMap hmap = new HashMap();
    for (int k=0; k<keys.length; k++) {
      hmap.put(keys[k], coords[k]);
    }
    return hmap;
  }

  public double[][] getCoords() {
    return coords;
  }

  public void setCoords(double[][] coords) {
    this.coords = coords;
  }

  public String[] getKeys() {
    return keys;
  }

  public void setKeys(String[] keys) {
    this.keys = keys;
  }

  public MultiDimensionSubset cloneMe() {
    return new MultiDimensionSubset(coords, keys);
  }

}
