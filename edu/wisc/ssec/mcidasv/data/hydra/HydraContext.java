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


  public synchronized void setMultiDimensionSubset(MultiDimensionSubset subset) {
    this.subset = subset;
  }


  public synchronized MultiDimensionSubset getMultiDimensionSubset() {
    return subset;
  }


}
