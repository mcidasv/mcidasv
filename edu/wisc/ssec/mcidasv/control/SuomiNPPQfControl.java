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

package edu.wisc.ssec.mcidasv.control;

import java.util.ArrayList;
import java.util.List;

import edu.wisc.ssec.mcidasv.data.QualityFlag;
import edu.wisc.ssec.mcidasv.data.hydra.SuomiNPPDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.idv.control.ImagePlanViewControl;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.control.ReadoutInfo;

import visad.Real;
import visad.georef.EarthLocation;

public class SuomiNPPQfControl extends ImagePlanViewControl {
    
    private static final Logger logger = LoggerFactory.getLogger(SuomiNPPQfControl.class);

    /**
     * Get the {@literal "first"} {@link DataSourceImpl} associated with this
     * control.
     *
     * @return Either the first {@code DataSourceImpl} for this control, or
     * {@code null}.
     */
    public DataSourceImpl getDataSource() {
        DataSourceImpl ds = null;
        List dataSources = getDataSources();
        if (!dataSources.isEmpty()) {
            ds = (DataSourceImpl)dataSources.get(0);
        }
        return ds;
    }

    @Override protected List getCursorReadoutInner(EarthLocation el,
                                                   Real animationValue,
                                                   int animationStep,
                                                   List<ReadoutInfo> samples) throws Exception {
        try {
        
            if (currentSlice == null) {
                return null;
            }
            List result = new ArrayList();
            Real r = GridUtil.sampleToReal(
                         currentSlice, el, animationValue, getSamplingModeValue(NEAREST_NEIGHBOR));
            if (r != null) {
                ReadoutInfo readoutInfo = new ReadoutInfo(this, r, el,
                                              animationValue);
                readoutInfo.setUnit(getDisplayUnit());
                readoutInfo.setRange(getRange());
                samples.add(readoutInfo);
            }
    
            if ((r != null) && !r.isMissing()) {
                
                //logger.trace("cursor value: {}", r.getValue());
                DataChoice dc = getDataChoice();
                // TODO: why do we have to append All_Data anyway?
                String prod = ("All_Data/").concat(dc.toString());
                //logger.trace("prod: {}", prod);
                QualityFlag qf = ((SuomiNPPDataSource) getDataSource()).getQfMap().get(prod);
                //logger.trace("qf: {}", qf.toString());
                Integer intVal = (int) r.getValue();
                //logger.trace("intVal: {}", intVal.toString());
                // getNameForValue wants a STRING representation of an INTEGER
                String str = qf.getNameForValue(intVal.toString());
                //logger.trace("str: {}", str);
                
                result.add("<tr><td>" + getMenuLabel()
                           + ":</td><td  align=\"right\">"
                           + str + "</td></tr>");
    
                return result;
            }
            } catch (Exception exc) {
                // Just catching the exception here so we can send it to logger,
                // otherwise it'll get caught in DisplayControlImpl.getCursorReadout,
                // get lost in LogUtil.consoleMessage (where do those go??...),
                // and "doCursorReadout" will be mysteriously shut off.
                logger.warn("Exception caught: {}", exc.getMessage());
                // re-throw it so DisplayControlImpl can still do its thing.
                throw exc;
            }
        return null;

    }
}
