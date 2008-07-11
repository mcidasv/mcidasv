package edu.wisc.ssec.mcidasv.control;

import java.util.List;
import java.awt.Container;
import java.rmi.RemoteException;

import javax.swing.JTabbedPane;

import ucar.unidata.idv.control.DisplayControlImpl;

import ucar.unidata.data.DataChoice;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.Range;
import visad.VisADException;
import visad.FlatField;
import visad.RealType;
import visad.Integer1DSet;
import visad.RealTupleType;
import visad.FunctionType;
import visad.georef.MapProjection;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
                                                                                                                                          
import ucar.unidata.data.DataChoice;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.XYDisplay;
import ucar.visad.display.RGBDisplayable;


public class ScatterDisplay extends DisplayControlImpl {
    
    Container container;
    FlatField X_field;
    FlatField Y_field;
    ViewManager scatterView = null;
    
    public ScatterDisplay() {
        super();
    }
    
    @Override public boolean init(List choices) throws VisADException, RemoteException {
        X_field = (FlatField) getDataChoice().getData(getDataSelection());
        popupDataDialog("select Y Axis field", container, false, null);

        try {
        java.lang.Thread.sleep(2000);
        } catch (Exception e) {
        }

        Y_field = (FlatField) getDataChoice().getData(getDataSelection());

        return true;
    }

    public void initDone() {

       try {
       ScatterDisplayable scatterDsp = new ScatterDisplayable("scatter",
                   RealType.getRealType("mask"), new float[][] {{1},{1},{0}}, false);
       float[] valsX = X_field.getFloats(false)[0];
       float[] valsY = Y_field.getFloats(false)[0];
       Integer1DSet set = new Integer1DSet(valsX.length);
       FlatField scatter = new FlatField(
           new FunctionType(RealType.Generic,
               new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), set);
       float[] mask = new float[valsX.length];
       scatter.setSamples(new float[][] {valsX, valsY, mask});
       scatterDsp.setData(scatter);
                                                                                                                                                           
       DisplayMaster master = scatterView.getMaster();
       master.addDisplayable(scatterDsp);
       master.draw();
       } catch (Exception e) {
         e.printStackTrace();
       }
    } 
    
    @Override public Container doMakeContents() {
        JTabbedPane pane = new JTabbedPane();
        pane.add("Scatter", GuiUtils.inset(getScatterTabComponent(),5));
        GuiUtils.handleHeavyWeightComponentsInTabs(pane);

        container = pane;
        return pane;
    }
    
//    @Override public void doRemove() throws VisADException, RemoteException {
//        super.doRemove();
//    }


    protected JComponent getScatterTabComponent() {
       try {
       scatterView = new ViewManager(getViewContext(),
                             new XYDisplay("Scatter", RealType.XAxis, RealType.YAxis),
                             new ViewDescriptor("scatter"), "showControlLegend=false;");
       } catch (Exception e) {
         e.printStackTrace();
       }
                                                                                                                                          
       return GuiUtils.centerBottom(scatterView.getContents(), null);
    }

    private class ScatterDisplayable extends RGBDisplayable {
                                                                                                                                          
       ScatterDisplayable(String name, RealType rgbRealType, float[][] colorPalette, boolean alphaflag) throws VisADException, RemoteException {
         super(name, rgbRealType, colorPalette, alphaflag);
       }
                                                                                                                                          
    }


    
}
