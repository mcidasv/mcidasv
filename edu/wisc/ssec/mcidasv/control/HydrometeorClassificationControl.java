package edu.wisc.ssec.mcidasv.control;

import java.util.ArrayList;
import java.util.List;

import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.control.ReadoutInfo;
import visad.Real;
import visad.georef.EarthLocation;

public class HydrometeorClassificationControl extends edu.wisc.ssec.mcidasv.control.ImagePlanViewControl {

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
