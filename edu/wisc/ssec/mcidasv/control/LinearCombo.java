package edu.wisc.ssec.mcidasv.control;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;
import edu.wisc.ssec.mcidasv.jython.Console;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Range;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import visad.VisADException;
import visad.georef.MapProjection;

public class LinearCombo extends HydraControl {
    
    private static final String PARAM = "BrightnessTemp";
    
    private static final int DEFAULT_FLAGS =
        FLAG_COLORTABLE | FLAG_SELECTRANGE | FLAG_ZPOSITION;
    
    private Console console;
    
    private MultiSpectralDisplay display;
    
    private DisplayMaster displayMaster;
    
    public LinearCombo() {
        super();
    }

    @Override public boolean init(final DataChoice choice) throws VisADException, RemoteException {
        Float fieldSelectorChannel = (Float)getDataSelection().getProperty(Constants.PROP_CHAN);
        if (fieldSelectorChannel == null)
            fieldSelectorChannel = MultiSpectralData.init_wavenumber;
        
        console = new Console();
        
        display = new MultiSpectralDisplay((DirectDataChoice)choice);
        display.setWaveNumber(fieldSelectorChannel);
        
        addDisplayable(display.getImageDisplay(), DEFAULT_FLAGS);
        
        addViewManager(display.getViewManager());
        
        setAttributeFlags(DEFAULT_FLAGS);
        
        return true;
    }

    @Override public void initDone() {
    }

    @Override public MapProjection getDataProjection() {
        MapProjection mp = null;
        Rectangle2D rect = MultiSpectralData.getLonLatBoundingBox(display.getImageData());
        try {
            mp = new LambertAEA(rect);
        } catch (Exception e) {
            logException("LinearCombo.getDataProjection", e);
        }
        return mp;
    }

    @Override protected Range getInitialRange() throws VisADException, RemoteException {
        return getDisplayConventions().getParamRange(PARAM, null);
    }

    @Override protected ColorTable getInitialColorTable() {
        return getDisplayConventions().getParamColorTable(PARAM);
    }

    @Override public Container doMakeContents() {
        JTabbedPane pane = new JTabbedPane();
        pane.add("Display", GuiUtils.inset(getDisplayTab(), 5));
        return pane;
    }

    private JComponent getDisplayTab() {
        Console console = new Console();
        JPanel consolePanel = console.getPanel();
        consolePanel.setPreferredSize(new Dimension(500, 250));
        
        List<Component> compList = new ArrayList<Component>();
//        compList.add(display.getDisplayComponent());
        JPanel tmp = GuiUtils.centerBottom(display.getDisplayComponent(), consolePanel);
        return tmp;
    }
    
    @Override public void doRemove() throws VisADException, RemoteException {
//        super.doRemove();
        removeDisplayables();
    }
    
}