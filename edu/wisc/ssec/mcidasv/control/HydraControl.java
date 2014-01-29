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

import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.SelectRangeWidget;
import ucar.visad.display.ColorScale;
import ucar.unidata.util.Range;
import ucar.unidata.util.LogUtil;

public abstract class HydraControl extends DisplayControlImpl {

//    public abstract boolean init(DataChoice dataChoice) throws VisADException, RemoteException;
//    
//    public abstract void initDone();
//    
//    public abstract MapProjection getDataProjection();

    public void updateRange(Range range) {
        if (ctw != null)
          ctw.setRange(range);

        try {
            SelectRangeWidget srw = getSelectRangeWidget(range);
            if (srw != null) {
                srw.setRange(range);
            }
        } catch (Exception e) {
            LogUtil.logException("Error updating select range widget", e);
        }

        if (colorScales != null) {
            ColorScale scale = (ColorScale) colorScales.get(0);
            try {
                scale.setRangeForColor(range.getMin(), range.getMax());
            }
            catch (Exception exc) {
                LogUtil.logException("Error updating display ColorScale range", exc);
            }
        }
    }

    public void handleChannelChange(final float newChan) {
        return;
    }

    protected abstract MultiSpectralDisplay getMultiSpectralDisplay();
}
