package edu.wisc.ssec.mcidasv.control;

import java.awt.Container;
import java.rmi.RemoteException;

import javax.swing.JTabbedPane;

import ucar.unidata.data.DataChoice;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.Range;
import visad.VisADException;
import visad.georef.MapProjection;

public class LinearCombo extends HydraControl {
    
    private static final String PARAM = "BrightnessTemp";
    
    public LinearCombo() {
        super();
    }
    
    @Override public boolean init(final DataChoice choice) throws VisADException, RemoteException {
        return true;
    }
    
    @Override public void initDone() {
        
    }
    
    @Override public MapProjection getDataProjection() {
        return null;
    }
    
    @Override protected Range getInitialRange() throws VisADException, RemoteException {
        return getDisplayConventions().getParamRange(PARAM, null);
    }
    
    @Override protected ColorTable getInitialColorTable() {
        return getDisplayConventions().getParamColorTable(PARAM);
    }
    
    @Override public Container doMakeContents() {
        JTabbedPane pane = new JTabbedPane();
        return pane;
    }
    
//    @Override public void doRemove() throws VisADException, RemoteException {
//        super.doRemove();
//    }
    
}