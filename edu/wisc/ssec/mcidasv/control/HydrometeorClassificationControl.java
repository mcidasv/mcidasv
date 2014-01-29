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

import ucar.unidata.idv.control.ImagePlanViewControl;

import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.control.ReadoutInfo;
import visad.Real;
import visad.georef.EarthLocation;

public class HydrometeorClassificationControl extends ImagePlanViewControl {

    @Override protected List getCursorReadoutInner(EarthLocation el,
                                                   Real animationValue,
                                                   int animationStep,
                                                   List<ReadoutInfo> samples)
              throws Exception {

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
            
            // list of codes found here:
            // (51.2.2) http://www.roc.noaa.gov/wsr88d/PublicDocs/ICDs/2620003R.pdf
            String str;
            switch ((int) r.getValue()) {
                case 0:  str = "SNR&lt;Threshold";  // black
                         break;
                case 10:  str = "Biological";  // medium gray
                         break;
                case 20:  str = "AP/Ground Clutter";  // dark gray
                         break;
                case 30:  str = "Ice Crystals";  // light pink
                         break;
                case 40:  str = "Dry Snow";  // light blue
                         break;
                case 50:  str = "Wet Snow";  // medium blue
                         break;
                case 60:  str = "Light-Moderate Rain";  // light green
                         break;
                case 70:  str = "Heavy Rain";  // medium green
                         break;
                case 80:  str = "Big Drops Rain";  // dark yellow
                         break;
                case 90:  str = "Graupel";  // medium pink
                         break;
                case 100: str = "Hail, Possibly With Rain";  // red
                         break;
                // classification algorithm reports "unknown type" here.
                // How to distinguish this from "McV doesnt understand the code"?
                case 140: str = "Unknown Type";  // purple
                         break;
                case 150: str = "RF";  // dark purple
                         break;
                default: str = "code undefined";
                         break;
            }

            result.add("<tr><td>" + getMenuLabel()
                       + ":</td><td  align=\"right\">"
                       + str + "</td></tr>");
        }
        return result;

    }
}
