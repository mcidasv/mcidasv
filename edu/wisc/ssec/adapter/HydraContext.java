/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
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
import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataGroup;
import edu.wisc.ssec.hydra.data.DataSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class HydraContext {

    private static HashMap<DataSource, HydraContext> dataSourceToContextMap = new HashMap<DataSource, HydraContext>();
    private static HashMap<DataChoice, HydraContext> dataChoiceToContextMap = new HashMap<DataChoice, HydraContext>();
    private static HashMap<DataGroup, HydraContext> dataCategoryToContextMap = new HashMap<DataGroup, HydraContext>();

    private static HashMap<DataSource, HashMap<DataGroup, HydraContext>> contextMap = new HashMap<DataSource, HashMap<DataGroup, HydraContext>>();

    private MultiDimensionSubset subset = null;
    private Object selectBox = null;

    DataSource dataSource = null;

    private static HydraContext lastManual = null;

    public static HydraContext getHydraContext(DataSource source, DataGroup dataCategory) {
        if (dataCategory == null) {
            return getHydraContext(source);
        }
        if (contextMap.containsKey(source)) {
            if ((contextMap.get(source)).containsKey(dataCategory)) {
                return contextMap.get(source).get(dataCategory);
            } else {
                HashMap catMap = contextMap.get(source);
                HydraContext hydraContext = new HydraContext();
                hydraContext.dataSource = source;
                catMap.put(dataCategory, hydraContext);
                return hydraContext;
            }
        } else {
            HydraContext hydraContext = new HydraContext();
            hydraContext.dataSource = source;
            HashMap catMap = new HashMap();
            catMap.put(dataCategory, hydraContext);
            contextMap.put(source, catMap);
            return hydraContext;
        }
    }

    public static HydraContext getHydraContext(DataSource source) {
        if (dataSourceToContextMap.isEmpty()) {
            HydraContext hydraContext = new HydraContext();
            hydraContext.dataSource = source;
            dataSourceToContextMap.put(source, hydraContext);
            return hydraContext;
        }

        if (dataSourceToContextMap.containsKey(source)) {
            return dataSourceToContextMap.get(source);
        } else {
            HydraContext hydraContext = new HydraContext();
            hydraContext.dataSource = source;
            dataSourceToContextMap.put(source, hydraContext);
            return hydraContext;
        }
    }

    public static void removeContext(DataSource source) {
        contextMap.remove(source);
        dataSourceToContextMap.remove(source);
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

    public DataSource getDataSource() {
        return dataSource;
    }

    public static HydraContext getLastManual() {
        return lastManual;
    }

    public static void setLastManual(HydraContext context) {
        lastManual = context;
    }

    public synchronized MultiDimensionSubset getMultiDimensionSubset() {
        return subset;
    }


}
