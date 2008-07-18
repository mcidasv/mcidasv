package edu.wisc.ssec.mcidasv.control;

import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.util.Range;

public abstract class HydraControl extends DisplayControlImpl {

//    public abstract boolean init(DataChoice dataChoice) throws VisADException, RemoteException;
//    
//    public abstract void initDone();
//    
//    public abstract MapProjection getDataProjection();

    public void updateRange(Range range) {
        ctw.setRange(range);
        srw.setRange(range);
    }

    public void handleChannelChange(final float newChan) {
        return;
    }
}
