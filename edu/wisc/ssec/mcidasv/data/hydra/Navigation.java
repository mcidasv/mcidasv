package edu.wisc.ssec.mcidasv.data.hydra;

import visad.CoordinateSystem;
import visad.Linear2DSet;

public interface Navigation {

  public CoordinateSystem getVisADCoordinateSystem(Linear2DSet domain, Object subset) throws Exception;

}


