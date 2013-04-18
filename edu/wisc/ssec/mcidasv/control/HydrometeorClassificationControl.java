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
            
            // TODO: find full list of numerical codes and the category they represent
            String str;
            if (r.getValue()  == 40) {
                str = "DRY SNOW!!!";
            } else {
                str = "NOT DRY SNOW :(";
            }

            result.add("<tr><td>AHHHHHH" + getMenuLabel()
                       + ":</td><td  align=\"right\">"
                       + str + ((currentLevel != null)
                    ? ("@" + currentLevel)
                    : "") + "</td></tr>");
        }
        return result;

    }
}
