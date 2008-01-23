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
