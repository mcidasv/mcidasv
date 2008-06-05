package edu.wisc.ssec.mcidasv.control;

import java.rmi.RemoteException;

import ucar.unidata.data.DataChoice;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.util.Range;
import visad.VisADException;
import visad.georef.MapProjection;


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
    
}
