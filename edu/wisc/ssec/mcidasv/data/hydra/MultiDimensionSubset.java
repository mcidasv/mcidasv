package edu.wisc.ssec.mcidasv.data.hydra;

import ucar.unidata.data.DataSelection;
import java.util.HashMap;


public class MultiDimensionSubset extends DataSelection {

  HashMap subset;

  public MultiDimensionSubset(HashMap subset) {
    super();
    this.subset = subset;
  }

  HashMap getSubset() {
    return subset;
  }

  public MultiDimensionSubset cloneMe() {
     return new MultiDimensionSubset(this.subset);
  }

}
