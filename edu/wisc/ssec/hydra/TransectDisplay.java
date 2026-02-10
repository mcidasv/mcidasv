/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.hydra;


import visad.*;

import visad.util.Util;
import visad.util.HersheyFont;

import java.rmi.RemoteException;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Component;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;

import javax.swing.JFrame;

import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

public class TransectDisplay extends HydraDisplay implements ActionListener {

    float[] yRange = new float[2];      // current Y-axis range
    float[] dataYrange = new float[2];  // full data min/max range

    DataReference transectDataRef = null;
    RealType rangeType = null;
    RealType domainType = null;
    TupleType valueType;

    LocalDisplay display = null;

    ScalarMap ymap = null;

    ArrayList<ScalarMap> yAxisMaps = new ArrayList<ScalarMap>();

    DragLine drgline;

    JFrame frame;

    double markerDist = Double.NaN;

    private HashMap<Transect, DataReference> transectToDataRef = new HashMap();
    private HashMap<Transect, NumberFormat> transectToNumFmt = new HashMap();

    private int numTransects = 0;

    public TransectDisplay(Transect transect, Color color, Point loc) throws VisADException, RemoteException {

        transectDataRef = transect.getTransectDataRef();
        FlatField data = (FlatField) transectDataRef.getData();
        FunctionType fncType = (FunctionType) data.getType();

        domainType = Transect.DistAlongTransect;
        rangeType = (RealType) fncType.getRange();

        display = new visad.java3d.DisplayImplJ3D("2D disp", new visad.java3d.TwoDDisplayRendererJ3D());
        display.getDisplayRenderer().getMouseBehavior().getMouseHelper().setFunctionMap(ucar.visad.display.DisplayMaster.defaultMouseFunctions);
        ((DisplayImpl) display).disableAction();
        display.getDisplayRenderer().setBackgroundColor(new Color(0.92f, 0.92f, 0.92f)); // off-white
        display.getDisplayRenderer().setForegroundColor(Color.black);
        ScalarMap xMapA = new ScalarMap(RealType.XAxis, Display.XAxis);
        ScalarMap yMapA = new ScalarMap(RealType.YAxis, Display.YAxis);
        xMapA.setRange(-2.5, 2.5);
        yMapA.setRange(-0.75, 0.75);
        xMapA.setScaleEnable(false);
        yMapA.setScaleEnable(false);
        display.addMap(xMapA);
        display.addMap(yMapA);

        ProjectionControl pCntrl = display.getProjectionControl();
        double[] proj = pCntrl.getMatrix();
        proj[0] = 0.286;
        proj[5] = 0.286;
        proj[10] = 0.286;
        pCntrl.setMatrix(proj);

        ScalarMap xmap = new ScalarMap(domainType, Display.XAxis);

        /** Use addTransect in the initialization? */

        ymap = new ScalarMap(rangeType, Display.YAxis);
        yAxisMaps.add(ymap);

        ScalarMap txtMap = new ScalarMap(TextType.Generic, Display.Text);

        pCntrl = display.getProjectionControl();
        pCntrl.setAspectCartesian(new double[]{2.50, 0.75, 1.0});
        ((DisplayImpl) display).setAlwaysAutoScale(true);
        //display.getGraphicsModeControl().setLineWidth(1.5f);
        //((visad.java3d.GraphicsModeControlJ3D)display.getGraphicsModeControl()).setSceneAntialiasingEnable(true);
        display.addMap(xmap);
        display.addMap(ymap);
        display.addMap(txtMap);
        TextControl txtCntrl = (TextControl) txtMap.getControl();
        txtCntrl.setJustification(TextControl.Justification.CENTER);

        valueType = new TupleType(new MathType[]{TextType.Generic, domainType});

        HersheyFont font = new HersheyFont("timesr");

        AxisScale xAxis = xmap.getAxisScale();
        xAxis.setFont(font);
        xAxis.setColor(Color.black);
        xAxis.setSnapToBox(true);
        xAxis.setLabelSize(Hydra.getFontSize());
        xmap.setScaleEnable(true);
        xAxis.setLabel("Distance along transect (km)");

        AxisScale yAxis = ymap.getAxisScale();
        yAxis.setFont(font);
        yAxis.setColor(Color.black);
        yAxis.setSnapToBox(true);
        yAxis.setLabelSize(Hydra.getFontSize());
        ymap.setScaleEnable(true);

        display.getGraphicsModeControl().setScaleEnable(true);

        ConstantMap lineWidth = new ConstantMap(1.5, Display.LineWidth);
        ConstantMap[] constantMaps = new ConstantMap[]{lineWidth};

        if (color != null) {
            color = getGraphColor(color);
            ConstantMap[] clrs = Util.getColorMaps(color);
            constantMaps = Hydra.makeConstantMapArray(clrs, lineWidth);
            display.addReference(transectDataRef, constantMaps);
        } else {
            display.addReference(transectDataRef);
        }
        ((DisplayImpl) display).enableAction();

        float[][] vals = data.getFloats(false);
        float[] minmax = Hydra.minmax(vals[0]);
        try {
            drgline = new DragLine(display, (Gridded1DSet) data.getDomainSet(), domainType, rangeType, Hydra.makeColorMap(Color.BLACK), minmax, 0f, xmap);
            drgline.addListener(transect);
        } catch (Exception e) {
            e.printStackTrace();
        }

        addValueLabel(transect, color);

        frame = Hydra.createAndShowFrameFromEDT("Transect Display", doMakeComponent(), buildMenuBar(), new Dimension(700, 350), loc);
        frame.toFront();
        frame.addWindowListener(this);
    }

    void addValueLabel(Transect transect, Color color) throws VisADException, RemoteException {
        DataReference dataRef = transect.getTransectDataRef();
        FlatField data = (FlatField) dataRef.getData();

        //- text readout for index selector
        final DataReference txtRef = new DataReferenceImpl("text");
        float[][] vals = data.getFloats(false);
        float[] minmax = Hydra.minmax(vals[0]);

        NumberFormat numFmt = Hydra.getDecimalFormat(minmax[1]);
        transectToDataRef.put(transect, txtRef);
        transectToNumFmt.put(transect, numFmt);

        TupleType tupType = new TupleType(new MathType[]{TextType.Generic, domainType});
        double selectorLoc = drgline.getSelectedValue();
        Real selectorX = new Real(domainType, selectorLoc);
        double rngVal = ((Real) data.evaluate(selectorX)).getValue();

        txtRef.setData(new Tuple(tupType, new Data[]{new Text(TextType.Generic, numFmt.format(rngVal)), selectorX}));

        ConstantMap[] clrs = Util.getColorMaps(color);
        ConstantMap ypos = new ConstantMap(0.9, Display.YAxis);
        ConstantMap[] constantMaps = Hydra.makeConstantMapArray(clrs, ypos);
        ConstantMap offset = new ConstantMap(numTransects * 0.34, Display.XAxisOffset);
        constantMaps = Hydra.makeConstantMapArray(constantMaps, offset);
        display.addReference(txtRef, constantMaps);
    }

    void updateValueLabel(Transect transect) throws VisADException, RemoteException {
        DataReference dataRef = transect.getTransectDataRef();
        FlatField data = (FlatField) dataRef.getData();
        if (!transectToDataRef.containsKey(transect)) {
            return;
        }
        DataReference ref = transectToDataRef.get(transect);
        NumberFormat numFmt = transectToNumFmt.get(transect);

        double selectorLoc = drgline.getSelectedValue();
        Real selectorX = new Real(domainType, selectorLoc);
        double rngVal = ((Real) data.evaluate(selectorX)).getValue();

        ref.setData(new Tuple(valueType, new Data[]{new Text(TextType.Generic, numFmt.format(rngVal)), selectorX}));
    }

    void makeValueText() {
        //??
    }

    public void addTransect(Transect transect, Color color) throws VisADException, RemoteException {
        FlatField dataTransect = (FlatField) transect.getTransectDataRef().getData();
        RealType rangeType = (RealType) ((FunctionType) dataTransect.getType()).getRange();

        addScalarMapForRangeType(rangeType);

        color = getGraphColor(color);
        ConstantMap lineWidth = new ConstantMap(1.5, Display.LineWidth);
        ConstantMap[] constantMaps = Hydra.makeConstantMapArray(Util.getColorMaps(color), lineWidth);

        if (drgline != null) {
            drgline.addListener(transect);
        }

        this.display.addReference(transect.getTransectDataRef(), constantMaps);
        numTransects++;

        addValueLabel(transect, color);
    }

    public void transectRangeChanged(RealType rangeType) throws VisADException, RemoteException {
        addScalarMapForRangeType(rangeType);
    }

    private void addScalarMapForRangeType(RealType rangeType) throws VisADException, RemoteException {
        boolean hasMap = false;
        for (int k = 0; k < yAxisMaps.size(); k++) {
            ScalarMap map = yAxisMaps.get(k);
            if (rangeType.equals(map.getScalar())) {
                hasMap = true;
                break;
            }
        }

        if (!hasMap) {
            ScalarMap map = new ScalarMap(rangeType, Display.YAxis);

            AxisScale yAxis = map.getAxisScale();
            yAxis.setColor(Color.black);
            yAxis.setLabelSize(24);
            yAxis.setSide(AxisScale.SECONDARY);
            if (yAxisMaps.size() < 2) {
                yAxis.setSnapToBox(true);
            }

            this.display.addMap(map);
            yAxisMaps.add(map);
        }
    }

    public void removeTransect(Transect transect) throws VisADException, RemoteException {
        this.display.removeReference(transect.getTransectDataRef());
        this.display.removeReference(transectToDataRef.get(transect));
        transectToDataRef.remove(transect);

        numTransects--;

        //- rescale the display to account for removed data
        ((DisplayImpl) this.display).reDisplayAll();
    }

    private Color getGraphColor(Color color) {
        if (color.equals(Color.green)) {
            color = new Color(34, 190, 24);
        }
        return color;
    }

    public Component doMakeComponent() {
        return display.getComponent();
    }

    public void windowClosing(WindowEvent e) {
        Transect.removeAll();
    }

    public void setMarkerDist(double dist, Gridded1DSet dset) throws VisADException, RemoteException {
        if (markerDist != dist && drgline != null) {
            drgline.updateSelector(dist, dset);
            markerDist = dist;
        }
    }

    public JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        menuBar.add(toolsMenu);

        JMenu captureMenu = new JMenu("Capture");
        captureMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        JMenuItem jpegItem = new JMenuItem("JPEG");
        jpegItem.addActionListener(this);
        jpegItem.setActionCommand("captureToJPEG");
        captureMenu.add(jpegItem);

        toolsMenu.add(captureMenu);

        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.getPopupMenu().setLightWeightPopupEnabled(false);

        JMenuItem axes = new JMenuItem("Axes");
        axes.setActionCommand("Axes");

        try {
            FlatField data = (FlatField) transectDataRef.getData();
            float[][] vals = data.getFloats(false);
            float minVal = Float.MAX_VALUE;
            float maxVal = -Float.MAX_VALUE;
            for (float v : vals[0]) {
                if (!Float.isNaN(v)) {
                    if (v < minVal) minVal = v;
                    if (v > maxVal) maxVal = v;
                }
            }
            dataYrange[0] = minVal;
            dataYrange[1] = maxVal;
            yRange[0] = minVal;
            yRange[1] = maxVal;
        } catch (Exception ex) {
            dataYrange[0] = 0f;
            dataYrange[1] = 1f;
            yRange[0] = 0f;
            yRange[1] = 1f;
        }

        // Y-Axis Range Menu Item
        JMenuItem axesItem = new JMenuItem("Y-Axis Range...");
        axesItem.setActionCommand("Axes");
        axesItem.addActionListener(e -> {
            if (!"Axes".equals(e.getActionCommand())) return;

            JDialog dialog = new JDialog(frame, "Y-Axis Range", true);
            dialog.setLocationRelativeTo(frame);

            JPanel panel = new JPanel(new GridLayout(1, 1));

            // RangeListener to update Y-axis
            class Y implements RangeListener {
                @Override
                public void rangeChanged(float low, float high) {
                    yRange[0] = low;
                    yRange[1] = high;
                    if (ymap != null) {
                        try {
                            ymap.setRange(low, high);
                        } catch (VisADException | RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            // lowhigh is your existing helper for entering min/max
            JPanel yPanel = new lowhigh(new Y(), "Y:", yRange[0], yRange[1], dataYrange[0], dataYrange[1]).panel;

            panel.add(yPanel);
            dialog.setContentPane(panel);
            dialog.pack();
            dialog.setVisible(true);
        });

        settingsMenu.add(axesItem);
        menuBar.add(settingsMenu);

        return menuBar;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("captureToJPEG")) {
            DisplayCapture.capture(frame, display, "jpeg");
        }
    }

}
