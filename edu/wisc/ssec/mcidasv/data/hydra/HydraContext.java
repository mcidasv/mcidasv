package edu.wisc.ssec.mcidasv.data.hydra;

import ucar.unidata.data.DataSelection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class HydraContext {

  private static HydraContext hydraContext = null;
  private boolean useSubset = false;
  private MultiDimensionSubset subset = null;


  public static HydraContext getHydraContext() {
    if (hydraContext == null) {
      hydraContext =  new HydraContext();
    }
    return hydraContext;
  }
 

  public HydraContext() {

  }


  public void setMultiDimensionSubset(MultiDimensionSubset subset) {
    useSubset = true;
    this.subset = subset;
  }


  public MultiDimensionSubset getMultiDimensionSubset() {
    if (useSubset) {
      useSubset = false;
      MultiDimensionSubset temp = subset;
      subset = null;
      return temp;
    }
    else {
      return null;
    }
  }


}
