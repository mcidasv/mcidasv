package edu.wisc.ssec.mcidasv.control;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.SubsetRubberBandBox;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.TextDisplayable;
import visad.DisplayEvent;
import visad.FlatField;
import visad.RealType;
import visad.TextType;
import visad.VisADException;
import visad.georef.MapProjection;


public class LinearCombo extends HydraControl {

    private static final String PROBE_ID = "hydra.probe2";

    private static final String PARAM = "BrightnessTemp";

    private static final int DEFAULT_FLAGS = 
        FLAG_COLORTABLE | FLAG_SELECTRANGE | FLAG_ZPOSITION;

    private MultiSpectralDisplay display;

    private DisplayMaster displayMaster;

    private final JTextField wavenumbox =  
        new JTextField(Float.toString(MultiSpectralData.init_wavenumber), 12);

    private HydraImageProbe2 probeA;
    private HydraImageProbe2 probeB;
    
    public LinearCombo() {
        super();
    }

    @Override public boolean init(final DataChoice choice)
        throws VisADException, RemoteException 
    {
        display = new MultiSpectralDisplay(this);

        displayMaster = getViewManager().getMaster();

        addDisplayable(display.getImageDisplay(), DEFAULT_FLAGS);

        // put the multispectral display into the layer controls
        addViewManager(display.getViewManager());

        // tell the idv what options to give the user
        setAttributeFlags(DEFAULT_FLAGS);

        probeA = createProbe(choice, Color.ORANGE);
        probeB = createProbe(choice, Color.MAGENTA);
        return true;
    }
    
    @Override public void initDone() {
        try {
            display.showChannelSelector();
            SubsetRubberBandBox rbb = new SubsetRubberBandBox(display.getImageData(), ((MapProjectionDisplay)displayMaster).getDisplayCoordinateSystem(), 1);
            updateImage(MultiSpectralData.init_wavenumber);
            
            // TODO: this type of thing needs to go. probes should Just Work.
            probeA.forceUpdateSpectrum();
            probeB.forceUpdateSpectrum();
        } catch (Exception e) {
            logException("LinearCombo.initDone", e);
        }
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

    @Override protected Range getInitialRange() throws VisADException,
        RemoteException
    {
        return getDisplayConventions().getParamRange(PARAM, null);
    }

    @Override protected ColorTable getInitialColorTable() {
        return getDisplayConventions().getParamColorTable(PARAM);
    }

    @Override public Container doMakeContents() {
        try {
            JTabbedPane pane = new JTabbedPane();
            pane.add("Display", GuiUtils.inset(getDisplayTab(), 5));
            pane.add("Settings", GuiUtils.inset(GuiUtils.top(doMakeWidgetComponent()), 5));
            GuiUtils.handleHeavyWeightComponentsInTabs(pane);
            return pane;
        } catch (Exception e) {
            logException("LinearCombo.doMakeContents", e);
        }
        return null;
    }

    public HydraImageProbe2 createProbe(final DataChoice choice, final Color c) {
        HydraImageProbe2 probe = null;
        try {

            probe = (HydraImageProbe2)getIdv().doMakeControl(Misc.newList(choice),
                getIdv().getControlDescriptor(PROBE_ID), (String)null, null, 
                false);

            probe.doMakeProbe();
            probe.setDisplay(display);
            probe.setColor(c);
        } catch (Exception e) {
            logException("LinearCombo.createProbe", e);
        }
        return probe;
    }
    
    public boolean updateImage(final float newChan) {
        if (!display.setWaveNumber(newChan))
            return false;

        DisplayableData imageDisplay = display.getImageDisplay();
        ((HydraRGBDisplayable)imageDisplay).getColorMap().resetAutoScale();
        displayMaster.reScale();

        try {
            imageDisplay.setData(display.getImageData());
        } catch (Exception e) {
            LogUtil.logException("LinearCombo.updateImage", e);
            return false;
        }
        
        return true;
    }
    
    // be sure to update the displayed image even if a channel change 
    // originates from the msd itself.
    @Override public void handleChannelChange(final float newChan) {
        if (updateImage(newChan))
            wavenumbox.setText(Float.toString(newChan));
    }

    private static TextDisplayable createValueDisplayer(final Color color) throws VisADException, RemoteException {
        DecimalFormat fmt = new DecimalFormat();
        fmt.setMaximumIntegerDigits(3);
        fmt.setMaximumFractionDigits(1);

        TextDisplayable td = new TextDisplayable(TextType.Generic);
        td.setLineWidth(2f);
        td.setColor(color);
        td.setNumberFormat(fmt);

        return td;
    }
    
//    private float getCurrentWaveNum() {
//        if (display == null) {
//            return 0f;
//        } else {
//            return display.getWaveNumber();
//        }
//    }
    
    private JComponent getDisplayTab() {

        final JLabel nameLabel = GuiUtils.rLabel("Wavenumber: ");

        wavenumbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String tmp = wavenumbox.getText().trim();
                updateImage(Float.valueOf(tmp));
            }
        });

        List<JComponent> compList = new ArrayList<JComponent>();
        compList.add(nameLabel);
        compList.add(wavenumbox);

        JPanel waveNo = GuiUtils.center(GuiUtils.doLayout(compList, 2, GuiUtils.WT_N, GuiUtils.WT_N));
        return GuiUtils.centerBottom(display.getViewManager().getContents(), waveNo);
    }
}
