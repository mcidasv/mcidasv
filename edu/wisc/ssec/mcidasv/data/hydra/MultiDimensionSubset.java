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

import ucar.unidata.data.DataSelection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;


public class MultiDimensionSubset extends DataSelection {

  public static final MultiDimensionSubset key = new MultiDimensionSubset();

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
    super();
    /**
    int num = keys.length;
    this.keys = new String[num];
    this.coords = new double[num][];
    for (int i=0; i<num; i++) {
      this.keys[i] = keys[i];
      this.coords[i] = new double[coords[i].length];
      for (int j=0; j<coords[i].length; j++) {
        this.coords[i][j] = coords[i][j];
      }
    }
    **/
    this.coords = coords;
    this.keys = keys;
  }

  public HashMap getSubset() {
    HashMap hmap = new HashMap();
    for (int k=0; k<keys.length; k++) {
      double[] new_coords = new double[coords[k].length];
      System.arraycopy(coords[k],0,new_coords,0,new_coords.length);
      hmap.put(keys[k], new_coords);
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

  public MultiDimensionSubset clone() {
    MultiDimensionSubset subset = new MultiDimensionSubset(coords, keys);
    Hashtable props = new Hashtable();
    props.put(MultiDimensionSubset.key, subset);
    subset.setProperties(props);
    return subset;
  }

  public String toString() {
	  StringBuffer sb = new StringBuffer();
	  if (keys != null) {
		  for (int i = 0; i < keys.length; i++) {
			  sb.append(new String(keys[i] + ": " + coords[i][0] + ", " + coords[i][1] + ", " + coords[i][2] + "\n"));
		  }
	  }
	  return sb.toString();
  }

  /***
  public boolean equals(Object obj) {
    if (!(obj instanceof MultiDimensionSubset)) return false;
    if ((keys == null) || (coords == null)) return false;

    String[] keys_in = ((MultiDimensionSubset)obj).getKeys();
    if ((keys_in == null) || (keys_in.length != keys.length)) return false;

    for (int k=0; k<keys.length; k++) {
      if (keys_in[k] != keys[k]) return false;
    } 

    double[][] coords_in = (double[][]) ((MultiDimensionSubset)obj).getCoords();
    if ((coords_in == null) || (coords.length != coords_in.length)) return false;
    for (int k=0; k<coords.length; k++) {
      for (int t=0; t<coords[k].length; t++) {
        if (coords[k][t] != coords_in[k][t]) return false;
      }
    }
    return true;
  }
  ***/
}
