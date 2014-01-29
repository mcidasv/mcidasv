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
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataCategory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class HydraContext {

  private static HashMap<DataSource, HydraContext> dataSourceToContextMap = new HashMap<DataSource, HydraContext>(); 
  private static HashMap<DataChoice, HydraContext> dataChoiceToContextMap = new HashMap<DataChoice, HydraContext>(); 
  private static HashMap<DataCategory, HydraContext> dataCategoryToContextMap = new HashMap<DataCategory, HydraContext>(); 

  private static HashMap<DataSource, HashMap<DataCategory, HydraContext>> contextMap = new HashMap<DataSource, HashMap<DataCategory, HydraContext>>();

  private static HydraContext hydraContext = null;
  private boolean useSubset = false;
  private MultiDimensionSubset subset = null;
  private Object selectBox = null;

  public static HydraContext getHydraContext(DataSource source, DataCategory dataCategory) {
    if (dataCategory == null) {
      return getHydraContext(source);
    }
    if (contextMap.containsKey(source)) {
      if ((contextMap.get(source)).containsKey(dataCategory)) {
        return contextMap.get(source).get(dataCategory);
      }
      else {
        HashMap catMap = contextMap.get(source);
        HydraContext hydraContext = new HydraContext();
        catMap.put(dataCategory, hydraContext);
        return hydraContext;
      }
    }
    else {
      HydraContext hydraContext = new HydraContext();
      HashMap catMap = new HashMap();
      catMap.put(dataCategory, hydraContext);
      contextMap.put(source, catMap);
      return hydraContext;
    }
  }

  public static HydraContext getHydraContext(DataSource source) {
    if (dataSourceToContextMap.isEmpty()) {
      HydraContext hydraContext = new HydraContext();
      dataSourceToContextMap.put(source, hydraContext);
      return hydraContext;
    }

    if (dataSourceToContextMap.containsKey(source)) {
      return dataSourceToContextMap.get(source);
    }
    else {
      HydraContext hydraContext = new HydraContext();
      dataSourceToContextMap.put(source, hydraContext);
      return hydraContext;
    }
  }

  public static HydraContext getHydraContext(DataChoice choice) {
    if (dataChoiceToContextMap.isEmpty()) {
      HydraContext hydraContext = new HydraContext();
      dataChoiceToContextMap.put(choice, hydraContext);
      return hydraContext;
    }

    if (dataChoiceToContextMap.containsKey(choice)) {
      return dataChoiceToContextMap.get(choice);
    }
    else {
      HydraContext hydraContext = new HydraContext();
      dataChoiceToContextMap.put(choice, hydraContext);
      return hydraContext;
    }
  }

  public static HydraContext getHydraContext(DataCategory choice) {
    if (dataCategoryToContextMap.isEmpty()) {
      HydraContext hydraContext = new HydraContext();
      dataCategoryToContextMap.put(choice, hydraContext);
      return hydraContext;
    }

    if (dataCategoryToContextMap.containsKey(choice)) {
      return dataCategoryToContextMap.get(choice);
    }
    else {
      HydraContext hydraContext = new HydraContext();
      dataCategoryToContextMap.put(choice, hydraContext);
      return hydraContext;
    }
  }


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

  public void setSelectBox(Object box) {
    selectBox = box;
  }

  public Object getSelectBox() {
    return selectBox;
  }

  public synchronized MultiDimensionSubset getMultiDimensionSubset() {
    return subset;
  }


}
