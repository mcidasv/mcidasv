/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.control;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
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

import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.VisADException;
import visad.georef.MapProjection;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.SpectrumAdapter;
import edu.wisc.ssec.mcidasv.data.hydra.SubsetRubberBandBox;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;

public class MultiSpectralControl extends HydraControl {

    private static final String PROBE_ID = "hydra.probe";

    private static final String PARAM = "BrightnessTemp";

    private static final int DEFAULT_FLAGS = 
        FLAG_COLORTABLE | FLAG_SELECTRANGE | FLAG_ZPOSITION;

    private MultiSpectralDisplay display;

    private DisplayMaster displayMaster;

    private final JTextField wavenumbox =  
        new JTextField(Float.toString(MultiSpectralData.init_wavenumber), 12);

    final JTextField minBox = new JTextField(6);
    final JTextField maxBox = new JTextField(6);

    private McIDASVHistogramWrapper histoWrapper;

    private HydraImageProbe probeA;
    private HydraImageProbe probeB;

    private List<List<Double>> probePositions = new ArrayList<List<Double>>();
    private List<Color> probeColors = new ArrayList<Color>();
    
    private int rangeMin;
    private int rangeMax;

    public MultiSpectralControl() {
        super();
    }

    @Override public boolean init(final DataChoice choice)
        throws VisADException, RemoteException 
    {
        List<DataChoice> choices = Collections.singletonList(choice);
        histoWrapper = new McIDASVHistogramWrapper("histo", choices, this);

            Float fieldSelectorChannel =
                (Float)getDataSelection().getProperty(Constants.PROP_CHAN);
            if (fieldSelectorChannel == null)
                fieldSelectorChannel = MultiSpectralData.init_wavenumber;

        display = new MultiSpectralDisplay(this);
        display.setWaveNumber(fieldSelectorChannel);

        displayMaster = getViewManager().getMaster();

        addDisplayable(display.getImageDisplay(), DEFAULT_FLAGS);

        // put the multispectral display into the layer controls
        addViewManager(display.getViewManager());

        // tell the idv what options to give the user
        setAttributeFlags(DEFAULT_FLAGS);

        setProjectionInView(true);

        probeA = createProbe(choice, Color.ORANGE, 0);
        probeB = createProbe(choice, Color.MAGENTA, 1);

        probePositions.clear();
        probeColors.clear();
        return true;
    }

    @Override public void initDone() {
        
        try {
            display.showChannelSelector();
            // TODO: this is ugly.
            Float fieldSelectorChannel = 
                (Float)getDataSelection().getProperty(Constants.PROP_CHAN);
            if (fieldSelectorChannel == null)
                fieldSelectorChannel = MultiSpectralData.init_wavenumber;

            handleChannelChange(fieldSelectorChannel);

            // TODO: this type of thing needs to go. probes should Just Work.
            probeA.forceUpdateSpectrum();
            probeB.forceUpdateSpectrum();

            /** don't add rubberband selector to main display at this time
                SubsetRubberBandBox rbb = 
                   new SubsetRubberBandBox(display.getImageData(), 
                         ((MapProjectionDisplay)displayMaster).getDisplayCoordinateSystem(), 1);
                rbb.setColor(Color.GREEN);
                addDisplayable(rbb);
            */
        } catch (Exception e) {
            logException("MultiSpectralControl.initDone", e);
        }
    }

    // TODO(jon): this is GODAWFUL, use a collection of probes and make the
    // probes more friendly to persisting.
    public List<List<Double>> getProbePositions() {
        List<Double> aList = new ArrayList<Double>(2);
        aList.add(probeA.getCurrentPosition().getValues()[0]);
        aList.add(probeA.getCurrentPosition().getValues()[1]);

        List<Double> bList = new ArrayList<Double>(2);
        bList.add(probeB.getCurrentPosition().getValues()[0]);
        bList.add(probeB.getCurrentPosition().getValues()[1]);

        List<List<Double>> positions = new ArrayList<List<Double>>();
        positions.add(aList);
        positions.add(bList);
        return positions;
    }

    public void setProbePositions(final List<List<Double>> newPositions) {
        probePositions.clear();
        probePositions.addAll(newPositions);
    }

    public List<Color> getProbeColors() {
        List<Color> colorList = new ArrayList<Color>();
        colorList.add(probeA.getCurrentColor());
        colorList.add(probeB.getCurrentColor());
        return colorList;
    }
    
    public void setProbeColors(final List<Color> newColors) {
        probeColors.clear();
        probeColors.addAll(newColors);
    }

    @Override public DataSelection getDataSelection() {
        DataSelection selection = super.getDataSelection();
        if (display != null) {
            selection.putProperty(Constants.PROP_CHAN, display.getWaveNumber());
            try {
                selection.putProperty(SpectrumAdapter.channelIndex_name, display.getChannelIndex());
            } catch (Exception e) {
                LogUtil.logException("MultiSpectralControl.getDataSelection", e);
            }
        }
        return selection;
    }

    @Override public void setDataSelection(final DataSelection newSelection) {
        super.setDataSelection(newSelection);
    }

    @Override public MapProjection getDataProjection() {
        MapProjection mp = null;
        Rectangle2D rect = 
            MultiSpectralData.getLonLatBoundingBox(display.getImageData());

        try {
            mp = new LambertAEA(rect);
        } catch (Exception e) {
            logException("MultiSpectralControl.getDataProjection", e);
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
            pane.add("Settings", 
                     GuiUtils.inset(GuiUtils.top(doMakeWidgetComponent()), 5));
            pane.add("Histogram", GuiUtils.inset(GuiUtils.top(getHistogramTabComponent()), 5));
            GuiUtils.handleHeavyWeightComponentsInTabs(pane);
            return pane;
        } catch (Exception e) {
            logException("MultiSpectralControl.doMakeContents", e);
        }
        return null;
    }

    @Override public void doRemove() throws VisADException, RemoteException {
        // removes the image display
        removeDisplayables();

        // forcibly clear the value displays when the user has elected to kill
        // the display. the displays will persist otherwise.
        displayMaster.removeDisplayable(probeA.getValueDisplay());
        displayMaster.removeDisplayable(probeB.getValueDisplay());
    }

    public HydraImageProbe createProbe(final DataChoice choice, final Color c, int index) {
        HydraImageProbe probe = null;
        try {

            probe = (HydraImageProbe)getIdv().doMakeControl(Misc.newList(choice),
                getIdv().getControlDescriptor(PROBE_ID), (String)null, null,
                false);

            probe.doMakeProbe();
            probe.setDisplay(display);

            if (!probeColors.isEmpty()) {
                Color persistedColor = probeColors.get(index);
                probe.setColor(persistedColor);
            } else {
                probe.setColor(c);
            }

            if (!probePositions.isEmpty()) {
                List<Double> coords = probePositions.get(index);
                double[] arr = { coords.get(0), coords.get(1) };
                RealTuple pos = new RealTuple(RealTupleType.Generic2D, arr);
                probe.setProbePosition(arr[0], arr[1]);
            }

            displayMaster.addDisplayable(probe.getValueDisplay());
        } catch (Exception e) {
            logException("MultiSpectralControl.createProbe", e);
        }
        return probe;
    }

    protected MultiSpectralDisplay getMultiSpectralDisplay() {
        return display;
    }

    public boolean updateImage(final float newChan) {
        if (!display.setWaveNumber(newChan))
            return false;

        DisplayableData imageDisplay = display.getImageDisplay();
        ((HydraRGBDisplayable)imageDisplay).getColorMap().resetAutoScale();
        displayMaster.reScale();

        try {
            imageDisplay.setData(display.getImageData());
            updateHistogramTab();

            // TODO: might want to expose updateLocationValues rather than make
            // unneeded calls to updateSpectrum and updatePosition 
            probeA.forceUpdateSpectrum();
            probeB.forceUpdateSpectrum();
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralControl.updateImage", e);
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
        return GuiUtils.centerBottom(display.getDisplayComponent(), waveNo);
    }

    private JComponent getHistogramTabComponent() {
        updateHistogramTab();
        JComponent histoComp = histoWrapper.doMakeContents();
        JLabel rangeLabel = GuiUtils.rLabel("Range   ");
        JLabel minLabel = GuiUtils.rLabel("Min");
        JLabel maxLabel = GuiUtils.rLabel("   Max");
        List<JComponent> rangeComps = new ArrayList<JComponent>();
        rangeComps.add(rangeLabel);
        rangeComps.add(minLabel);
        rangeComps.add(minBox);
        rangeComps.add(maxLabel);
        rangeComps.add(maxBox);
        minBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                rangeMin = Integer.valueOf(minBox.getText().trim());
                rangeMax = Integer.valueOf(maxBox.getText().trim());
                histoWrapper.modifyRange(rangeMin, rangeMax);
            }
        });
        maxBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                rangeMin = Integer.valueOf(minBox.getText().trim());
                rangeMax = Integer.valueOf(maxBox.getText().trim());
                histoWrapper.modifyRange(rangeMin, rangeMax);
            }
        });
        JPanel rangePanel =
            GuiUtils.center(GuiUtils.doLayout(rangeComps, 5, GuiUtils.WT_N, GuiUtils.WT_N));
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                resetColorTable();
            }
        });

        JPanel resetPanel = 
            GuiUtils.center(GuiUtils.inset(GuiUtils.wrap(resetButton), 4));

        return GuiUtils.topCenterBottom(histoComp, rangePanel, resetPanel);
    }

    private void updateHistogramTab() {
        try {
            histoWrapper.loadData(display.getImageData());
            org.jfree.data.Range range = histoWrapper.getRange();
            rangeMin = (int)range.getLowerBound();
            rangeMax = (int)range.getUpperBound();
            minBox.setText(Integer.toString(rangeMin));
            maxBox.setText(Integer.toString(rangeMax));
        } catch (Exception e) {
            logException("MultiSpectralControl.getHistogramTabComponent", e);
        }
    }

    public void resetColorTable() {
        histoWrapper.doReset();
    }

    protected void contrastStretch(final double low, final double high) {
        try {
            org.jfree.data.Range range = histoWrapper.getRange();
            rangeMin = (int)range.getLowerBound();
            rangeMax = (int)range.getUpperBound();
            minBox.setText(Integer.toString(rangeMin));
            maxBox.setText(Integer.toString(rangeMax));
            setRange(getInitialColorTable().getName(), new Range(low, high));
        } catch (Exception e) {
            logException("MultiSpectralControl.contrastStretch", e);
        }
    }

}
