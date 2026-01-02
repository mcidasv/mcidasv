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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.JButton;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.JLabel;


import visad.AxisScale;
import visad.CellImpl;
import visad.CoordinateSystem;
import visad.Data;
import visad.DelaunayCustom;
import visad.Display;
import visad.DisplayEvent;
import visad.DisplayListener;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded2DSet;
import visad.Gridded3DSet;
import visad.Integer1DSet;
import visad.Linear2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.ScalarMap;
import visad.ConstantMap;
import visad.Set;
import visad.SetType;
import visad.UnionSet;
import visad.VisADException;
import visad.java3d.DisplayImplJ3D;
import visad.java3d.GraphicsModeControlJ3D;
import visad.java3d.DisplayRendererJ3D;
import visad.util.HersheyFont;

import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataSelection;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.RGBDisplayable;
import ucar.visad.display.RubberBandBox;
import ucar.visad.display.XYDisplay;

import edu.wisc.ssec.adapter.CurveDrawer;
import edu.wisc.ssec.adapter.HistogramField;
import edu.wisc.ssec.adapter.SubsetRubberBandBox;

import java.text.DecimalFormat;
import javax.swing.JRadioButtonMenuItem;

public class MyScatterDisplay extends HydraDisplay {

    private FlatField X_field;
    private FlatField Y_field;
    private double total_area;
    private DisplayMaster scatterMaster = null;
    private DisplayRendererJ3D dspRenderer = null;

    private DisplayMaster dspMasterX;
    private DisplayMaster dspMasterY;

    private HistogramField histoField;

    private FlatField mask_field;
    private float[][] mask_range;
    private float[][] scatterFieldRange;
    private Data X_data;
    private Data Y_data;
    private String X_name;
    private String Y_name;

    private boolean cancel = false;

    private ScatterDisplayable scatterDsp;
    private ScatterDisplayable scatterMarkDsp;

    private BoxCurveSwitch boxCurveSwitch;

    public DataChoice dataChoiceX = null;

    public DataChoice dataChoiceY = null;

    public DataSelection dataSelectionX = null;

    public DataSelection dataSelectionY = null;

    JComponent ctwCompX;

    JComponent ctwCompY;

    int n_selectors = 3;

    List<ScatterBoxSelector> scatterBoxSelectors = new ArrayList<ScatterBoxSelector>();

    List<ScatterCurveSelector> scatterCurveSelectors = new ArrayList<ScatterCurveSelector>();

    List<ImageBoxSelector> imageXBoxSelectors = new ArrayList<ImageBoxSelector>();

    List<ImageBoxSelector> imageYBoxSelectors = new ArrayList<ImageBoxSelector>();

    List<ImageCurveSelector> imageXCurveSelectors = new ArrayList<ImageCurveSelector>();

    List<ImageCurveSelector> imageYCurveSelectors = new ArrayList<ImageCurveSelector>();

    JToggleButton[] selectorToggleButtons = new JToggleButton[n_selectors];
    CompoundBorder[] toggleOnBorders = new CompoundBorder[n_selectors];
    CompoundBorder[] toggleOffBorders = new CompoundBorder[n_selectors];
    Color[] selectorColors = new Color[]{Color.magenta, Color.green, Color.blue};
    float[][] maskColorPalette = new float[][]{{0.8f, 0f, 0f}, {0f, 0.8f, 0f}, {0.8f, 0f, 0.8f}, {0.97f, 0.97f, 0.98f}};
    float[][] markColorPaletteB = new float[][]{{0f, 0.8f, 0f, 0f}, {0f, 0f, 0.8f, 0f}, {0f, 0.8f, 0f, 0.8f}};
    float[][] markColorPaletteW = new float[][]{{1f, 0.8f, 0f, 0f}, {1f, 0f, 0.8f, 0f}, {1f, 0.8f, 0f, 0.8f}};

    JButton computeStatsButton;
    StatsTable statsTable;

    boolean selectByCurve = false;

    int currentSelectIndex = 0;

    JComponent guiPanel;

    HydraRGBDisplayable maskDspX;
    HydraRGBDisplayable maskDspY;
    ImageDisplay imgDisplayX;
    ImageDisplay imgDisplayY;
    Depiction maskDepictX;
    Depiction maskDepictY;

    DisplayMaster scatterDspMaster;
    HydraRGBDisplayable scatterDensityDsp;

    JFrame frame = null;

    JMenuBar menuBar = null;

    float[] dataXrange;
    float[] dataYrange;

    float[] xRange = new float[2];
    float[] yRange = new float[2];

    public MyScatterDisplay(FlatField X_field, FlatField Y_field, ImageDisplay imgDisplayX, ImageDisplay imgDisplayY) {
        this.X_field = X_field;
        this.Y_field = Y_field;
        this.dspMasterX = imgDisplayX.getDisplayMaster();
        this.dspMasterY = imgDisplayY.getDisplayMaster();
        this.imgDisplayX = imgDisplayX;
        this.imgDisplayY = imgDisplayY;

        X_name = imgDisplayX.getName();
        Y_name = imgDisplayY.getName();

        try {
            scatterDspMaster = makeScatterDisplay();
            doMakeComponent();
            menuBar = buildMenuBar();
            frame = Hydra.createAndShowFrame("scatter", guiPanel, menuBar, new Dimension(440, 440));
            frame.addWindowListener(this);
            init();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                dspMasterX.setDisplayActive();
                dspMasterY.setDisplayActive();
            } catch (VisADException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    public boolean init() throws VisADException, RemoteException {

        setup();

        mask_field = new FlatField(
                new FunctionType(((FunctionType) X_field.getType()).getDomain(), RealType.Generic),
                X_field.getDomainSet());

        int len = X_field.getDomainSet().getLength();
        int[] lens = ((Gridded2DSet) X_field.getDomainSet()).getLengths();
        mask_range = new float[1][len];
        for (int t = 0; t < len; t++) {
            mask_range[0][t] = Float.NaN;
        }
        mask_range[0][0] = 0; //- field should not be all missing
        mask_field.setSamples(mask_range, false);

        int binSize = ((lens[0] * lens[1] / (256 * 256)) * 4) / 10;
        if (binSize < 2) binSize = 2;
        histoField = new HistogramField(X_field, Y_field, mask_field, 256, binSize);

        FlatField scatterDensity = histoField.getScatterDensityField();
        RealType rtype = ((FunctionType) scatterDensity.getType()).getFlatRange().getRealComponents()[0];
        scatterDensityDsp = new HydraRGBDisplayable("density", rtype, null, true);
        scatterDensityDsp.setData(scatterDensity);
        scatterDensityDsp.setVisible(false);
        scatterMaster.addDisplayable(scatterDensityDsp);

        if (statsTable != null) statsTable.setNames(X_name, Y_name);

        maskDspX = new HydraRGBDisplayable("mask", RealType.Generic, null, maskColorPalette, true, null);
        maskDspX.addConstantMap(new ConstantMap(1.0, Display.RenderOrderPriority));
        maskDspX.setData(mask_field);
        maskDspX.setRangeForColor(0, n_selectors - 1);
        maskDepictX = new Depiction(dspMasterX, maskDspX, "mask", false, true);
        maskDepictX.setPopupName(X_name + " vs " + Y_name);
        imgDisplayX.addOverlayImage(maskDspX, maskDepictX, true);

        maskDspY = new HydraRGBDisplayable("mask", RealType.Generic, null, maskColorPalette, true, null);
        maskDspY.addConstantMap(new ConstantMap(1.0, Display.RenderOrderPriority));
        maskDspY.setData(mask_field);
        maskDspY.setRangeForColor(0, n_selectors - 1);
        maskDepictY = new Depiction(dspMasterY, maskDspY, "mask", false, true);
        maskDepictY.setPopupName(X_name + " vs " + Y_name);
        imgDisplayY.addOverlayImage(maskDspY, maskDepictY, true);

        initDone();
        return true;
    }

    public void setup() throws VisADException, RemoteException {
        if (!(X_field.getDomainSet().equals(Y_field.getDomainSet()))) {
            Y_field = resample(X_field, Y_field);
        }

        statsTable = new StatsTable(false);
    }

    public void windowClosing(WindowEvent evt) {
        try {
            if (imgDisplayX.getDisplayMaster() != null) {
                if (!imgDisplayX.getDisplayMaster().getDestroyed()) {
                    maskDepictX.remove();
                    for (int k = 0; k < n_selectors; k++) {
                        imageXBoxSelectors.get(k).remove();
                        imageXCurveSelectors.get(k).remove();
                    }
                }
            }
            if (imgDisplayY.getDisplayMaster() != null) {
                if (!imgDisplayY.getDisplayMaster().getDestroyed()) {
                    maskDepictY.remove();
                    for (int k = 0; k < n_selectors; k++) {
                        imageYBoxSelectors.get(k).remove();
                        imageYCurveSelectors.get(k).remove();
                    }
                }
            }
            for (int k = 0; k < n_selectors; k++) {
                scatterCurveSelectors.get(k).remove();
            }

          /* May Causes a non-fatal Java3D Exception
          scatterDspMaster.destroy();
          */
            guiPanel.removeAll();
            dspRenderer.destroy();
            scatterDspMaster = null;
            Scatter.clear();
            frame.removeWindowListener(this);
            frame.dispose();
            frame = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void initDone() throws VisADException, RemoteException {
        for (int k = 0; k < n_selectors; k++) {
            scatterBoxSelectors.add(new ScatterBoxSelector(scatterMaster, selectorColors[k], (float) k));
            scatterCurveSelectors.add(new ScatterCurveSelector(scatterMaster, selectorColors[k], (float) k));
        }

        dspMasterX.setDisplayInactive();
        dspMasterY.setDisplayInactive();

        for (int k = 0; k < n_selectors; k++) {
            SubsetRubberBandBox X_subsetBox =
                    new SubsetRubberBandBox(X_field, ((MapProjectionDisplayJ3D) dspMasterX).getDisplayCoordinateSystem(), 0, false);
            X_subsetBox.setColor(selectorColors[k]);

            ImageBoxSelector markX = new ImageBoxSelector(X_subsetBox, X_field.getDomainSet(), dspMasterX, selectorColors[k], (float) k + 1, statsTable);

            SubsetRubberBandBox Y_subsetBox =
                    new SubsetRubberBandBox(Y_field, ((MapProjectionDisplayJ3D) dspMasterY).getDisplayCoordinateSystem(), 0, false);
            Y_subsetBox.setColor(selectorColors[k]);
            ImageBoxSelector markY = new ImageBoxSelector(Y_subsetBox, Y_field.getDomainSet(), dspMasterY, selectorColors[k], (float) k + 1, statsTable);

            markX.setOther(markY);
            markY.setOther(markX);
            imageXBoxSelectors.add(markX);
            imageYBoxSelectors.add(markY);
        }

        for (int k = 0; k < n_selectors; k++) {
            CurveDrawer curveDraw = new CurveDrawer(RealType.Longitude, RealType.Latitude, 1);
            curveDraw.setColor(selectorColors[k]);
            curveDraw.setLineWidth(2);
            ImageCurveSelector curveX = new ImageCurveSelector(curveDraw, X_field, dspMasterX, selectorColors[k], (float) k + 1, statsTable);
            curveX.setActive(false);
            curveDraw.addAction(curveX);
            curveX.setVisible(false);
            dspMasterX.addDisplayable(curveDraw);

            curveDraw = new CurveDrawer(RealType.Longitude, RealType.Latitude, 1);
            curveDraw.setColor(selectorColors[k]);
            curveDraw.setLineWidth(2);
            ImageCurveSelector curveY = new ImageCurveSelector(curveDraw, Y_field, dspMasterY, selectorColors[k], (float) k + 1, statsTable);
            curveY.setActive(false);
            curveDraw.addAction(curveY);
            curveY.setVisible(false);
            dspMasterY.addDisplayable(curveDraw);

            curveX.setOther(curveY);
            curveY.setOther(curveX);
            imageXCurveSelectors.add(curveX);
            imageYCurveSelectors.add(curveY);
        }

        dspMasterX.setDisplayActive();
        dspMasterY.setDisplayActive();

        for (int k = 0; k < n_selectors; k++) {
            JToggleButton jtog = selectorToggleButtons[k];

            jtog.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int idx = Integer.valueOf(e.getActionCommand());
                    try {
                        for (int i = 0; i < n_selectors; i++) {
                            ScatterBoxSelector boxSel = (ScatterBoxSelector) scatterBoxSelectors.get(i);
                            ScatterCurveSelector curveSel = (ScatterCurveSelector) scatterCurveSelectors.get(i);
                            ImageBoxSelector imageXbox = (ImageBoxSelector) imageXBoxSelectors.get(i);
                            ImageBoxSelector imageYbox = (ImageBoxSelector) imageYBoxSelectors.get(i);
                            ImageCurveSelector imageXcurve = (ImageCurveSelector) imageXCurveSelectors.get(i);
                            ImageCurveSelector imageYcurve = (ImageCurveSelector) imageYCurveSelectors.get(i);

                            if (i == idx) {
                                currentSelectIndex = idx;
                                selectorToggleButtons[i].setBorder(toggleOnBorders[i]);
                                if (!selectorToggleButtons[i].isSelected()) {

                                    if (statsTable != null) statsTable.resetValues(i);

                                    boxSel.reset();
                                    boxSel.setActive(false);
                                    boxSel.setVisible(false);
                                    curveSel.reset();
                                    curveSel.setActive(false);
                                    curveSel.setVisible(false);

                                    imageXbox.reset();
                                    imageXbox.setActive(false);
                                    imageXbox.setVisible(false);
                                    imageYbox.reset();
                                    imageYbox.setActive(false);
                                    imageYbox.setVisible(false);

                                    imageXcurve.reset();
                                    imageXcurve.setActive(false);
                                    imageXcurve.setVisible(false);
                                    imageYcurve.reset();
                                    imageYcurve.setActive(false);
                                    imageYcurve.setVisible(false);
                                    selectorToggleButtons[i].setSelected(true);
                                }
                                boxSel.setActive(!getSelectByCurve());
                                boxSel.setVisible(!getSelectByCurve());
                                curveSel.setActive(getSelectByCurve());
                                curveSel.setVisible(getSelectByCurve());

                                imageXbox.setActive(!getSelectByCurve());
                                imageXbox.setVisible(!getSelectByCurve());
                                imageYbox.setActive(!getSelectByCurve());
                                imageYbox.setVisible(!getSelectByCurve());

                                imageXcurve.setActive(getSelectByCurve());
                                imageXcurve.setVisible(getSelectByCurve());
                                imageYcurve.setActive(getSelectByCurve());
                                imageYcurve.setVisible(getSelectByCurve());
                            } else {
                                selectorToggleButtons[i].setBorder(toggleOffBorders[i]);
                                selectorToggleButtons[i].setSelected(false);
                                boxSel.setActive(false);
                                boxSel.setVisible(false);
                                curveSel.setActive(false);
                                curveSel.setVisible(false);
                                imageXbox.setActive(false);
                                imageXbox.setVisible(false);
                                imageYbox.setActive(false);
                                imageYbox.setVisible(false);
                                imageXcurve.setActive(false);
                                imageXcurve.setVisible(false);
                                imageYcurve.setActive(false);
                                imageYcurve.setVisible(false);
                            }
                        }
                    } catch (Exception exc) {
                        System.out.println(exc);
                    }
                }
            });

            ScatterBoxSelector boxSel = (ScatterBoxSelector) scatterBoxSelectors.get(k);
            ScatterCurveSelector curveSel = (ScatterCurveSelector) scatterCurveSelectors.get(k);
            ImageBoxSelector imageXbox = (ImageBoxSelector) imageXBoxSelectors.get(k);
            ImageBoxSelector imageYbox = (ImageBoxSelector) imageYBoxSelectors.get(k);
            ImageCurveSelector imageXcurve = (ImageCurveSelector) imageXCurveSelectors.get(k);
            ImageCurveSelector imageYcurve = (ImageCurveSelector) imageYCurveSelectors.get(k);

            if (k == 0) {
                jtog.setSelected(true);
                boxSel.setActive(!getSelectByCurve());
                boxSel.setVisible(!getSelectByCurve());
                curveSel.setActive(getSelectByCurve());
                curveSel.setVisible(getSelectByCurve());
                imageXbox.setActive(!getSelectByCurve());
                imageXbox.setVisible(!getSelectByCurve());
                imageYbox.setActive(!getSelectByCurve());
                imageYbox.setVisible(!getSelectByCurve());

                imageXcurve.setActive(getSelectByCurve());
                imageXcurve.setVisible(getSelectByCurve());
                imageYcurve.setActive(getSelectByCurve());
                imageYcurve.setVisible(getSelectByCurve());
            } else {
                boxSel.setActive(false);
                boxSel.setVisible(false);
                curveSel.setActive(false);
                curveSel.setVisible(false);
                imageXbox.setActive(false);
                imageXbox.setVisible(false);
                imageYbox.setActive(false);
                imageYbox.setVisible(false);
                imageXcurve.setActive(false);
                imageXcurve.setVisible(false);
                imageYcurve.setActive(false);
                imageYcurve.setVisible(false);
            }
        }
    }

    public DisplayMaster makeScatterDisplay() throws VisADException, RemoteException {

        scatterDsp = new ScatterDisplayable("scatter",
                RealType.getRealType("mask"), markColorPaletteW, false);
        float[] valsX = X_field.getFloats(false)[0];
        float[] valsY = Y_field.getFloats(false)[0];
        Integer1DSet set = new Integer1DSet(valsX.length);
        FlatField scatter = new FlatField(
                new FunctionType(RealType.Generic,
                        new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), set);
        float[] mask = new float[valsX.length];
        for (int k = 0; k < mask.length; k++) mask[k] = 0;
        scatterFieldRange = new float[][]{valsX, valsY, mask};
        scatter.setSamples(scatterFieldRange);
        scatterDsp.setPointSize(2f);
        scatterDsp.setRangeForColor(0, n_selectors);

        dataXrange = minmax(valsX);
        dataYrange = minmax(valsY);
        xRange[0] = dataXrange[0];
        xRange[1] = dataXrange[1];
        yRange[0] = dataYrange[0];
        yRange[1] = dataYrange[1];

        scatterDsp.setData(scatter);

        scatterMarkDsp = new ScatterDisplayable("scatter",
                RealType.getRealType("mask"), markColorPaletteW, false);
        set = new Integer1DSet(2);
        scatter = new FlatField(
                new FunctionType(RealType.Generic,
                        new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), set);
        scatterMarkDsp.setData(scatter);
        scatterMarkDsp.setPointSize(2f);
        scatterMarkDsp.setRangeForColor(0, n_selectors);

        DisplayMaster master = new XYDisplay("Scatter", RealType.XAxis, RealType.YAxis);
        master.setBackground(Color.black);
        master.setForeground(Color.white);
        double[] proj = master.getProjectionMatrix();
        proj[0] = 0.56;
        proj[5] = 0.56;
        proj[10] = 0.56;
        master.setProjectionMatrix(proj);

        DisplayImplJ3D display = (DisplayImplJ3D) master.getDisplay();
        dspRenderer = (DisplayRendererJ3D) display.getDisplayRenderer();
        GraphicsModeControlJ3D mode = (GraphicsModeControlJ3D) display.getGraphicsModeControl();
        mode.setDepthBufferEnable(false, false);
        master.draw();
        scatterMaster = master;
        ((XYDisplay) master).showAxisScales(true);

        HersheyFont font = new HersheyFont("timesrb");

        AxisScale scaleX = ((XYDisplay) master).getXAxisScale();
        scaleX.setOppSideTitle2D(true);
        scaleX.setTitle(X_name);
        scaleX.setSnapToBox(true);
        scaleX.setFont(font);
        //scaleX.setLabelSize(2*scaleX.getLabelSize());
        scaleX.setLabelSize(Hydra.getFontSize());

        AxisScale scaleY = ((XYDisplay) master).getYAxisScale();
        scaleY.setOppSideTitle2D(true);
        scaleY.setTitle(Y_name);
        scaleY.setSnapToBox(true);
        scaleY.setFont(font);
        //scaleY.setLabelSize(2*scaleY.getLabelSize());
        scaleY.setLabelSize(Hydra.getFontSize());

        ((XYDisplay) master).setXRange((double) xRange[0], (double) xRange[1]);
        ((XYDisplay) master).setYRange((double) yRange[0], (double) yRange[1]);
        master.addDisplayable(scatterDsp);
        master.addDisplayable(scatterMarkDsp);

        return master;
    }


    public JComponent doMakeComponent() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scatterMaster.getComponent(), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        JRadioButton boxSelect = new JRadioButton("Box");
        boxSelect.setSelected(true);
        JRadioButton curveSelect = new JRadioButton("Curve");
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(boxSelect);
        buttonGroup.add(curveSelect);
        buttonPanel.add(boxSelect);
        buttonPanel.add(curveSelect);

        boxCurveSwitch = new BoxCurveSwitch();
        boxSelect.addActionListener(boxCurveSwitch);
        curveSelect.addActionListener(boxCurveSwitch);

        JPanel toggleButtonPanel = new JPanel(new FlowLayout());
        for (int k = 0; k < n_selectors; k++) {
            JToggleButton jtog =
                    new JToggleButton(
                            new ImageIcon(getClass().getResource("/resources/icons/subset12.jpg")));

            toggleOnBorders[k] = new CompoundBorder(new LineBorder(selectorColors[k], 2), new LineBorder(selectorColors[k], 4));
            toggleOffBorders[k] = new CompoundBorder(new LineBorder(selectorColors[k], 2), new EmptyBorder(4, 4, 4, 4));

            if (k == currentSelectIndex) { //initialize
                jtog.setBorder(toggleOnBorders[k]);
            } else {
                jtog.setBorder(toggleOffBorders[k]);
            }
            jtog.setActionCommand(String.valueOf(k));
            jtog.setToolTipText("click to select, click again to clear");
            toggleButtonPanel.add(jtog);
            selectorToggleButtons[k] = jtog;
        }

        buttonPanel.add(toggleButtonPanel);

        final JButton displayTypeButton = new JButton("points ");
        displayTypeButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                try {
                    if (displayTypeButton.getText().equals("points ")) {
                        scatterDsp.setVisible(false);
                        scatterMarkDsp.setVisible(false);
                        scatterDensityDsp.setVisible(true);
                        displayTypeButton.setText("density");
                    } else {
                        scatterDensityDsp.setVisible(false);
                        scatterDsp.setVisible(true);
                        scatterMarkDsp.setVisible(true);
                        displayTypeButton.setText("points ");
                    }
                } catch (VisADException exc) {
                    exc.printStackTrace();
                } catch (RemoteException exc) {
                    exc.printStackTrace();
                }
            }
        });
        buttonPanel.add(displayTypeButton);

        JButton computeStatsButton = new JButton("stats");
        computeStatsButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {

                if (statsTable == null) {
                    statsTable = new StatsTable();
                }

                statsTable.setIsShowing();
                statsTable.setFields(X_field, Y_field, 0);
            }
        });
        buttonPanel.add(computeStatsButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        guiPanel = panel;
        return guiPanel;
    }

    public JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        menuBar.add(toolsMenu);

        JMenu captureMenu = new JMenu("Capture");
        captureMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        JMenuItem jpegItem = new JMenuItem("JPEG");
        jpegItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cmd = e.getActionCommand();
                if (cmd.equals("captureToJPEG")) {
                    DisplayCapture.capture(frame, scatterDspMaster.getDisplay(), "jpeg");
                }
            }
        });
        jpegItem.setActionCommand("captureToJPEG");
        captureMenu.add(jpegItem);

        toolsMenu.add(captureMenu);

        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.getPopupMenu().setLightWeightPopupEnabled(false);

        JMenu backGroundClr = new JMenu("Background Color");
        JRadioButtonMenuItem white = new JRadioButtonMenuItem("white", false);
        JRadioButtonMenuItem black = new JRadioButtonMenuItem("black", true);
        ButtonGroup bg = new ButtonGroup();
        bg.add(black);
        bg.add(white);

        white.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scatterDspMaster.setBackground(Color.white);
                scatterDspMaster.setForeground(Color.black);
                try {
                    scatterDsp.setColorPalette(markColorPaletteB);
                    scatterMarkDsp.setColorPalette(markColorPaletteB);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }

            }
        });
        white.setActionCommand("white");

        black.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scatterDspMaster.setBackground(Color.black);
                scatterDspMaster.setForeground(Color.white);
                try {
                    scatterDsp.setColorPalette(markColorPaletteW);
                    scatterMarkDsp.setColorPalette(markColorPaletteW);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
        black.setActionCommand("black");

        backGroundClr.add(black);
        backGroundClr.add(white);
        settingsMenu.add(backGroundClr);

        JMenuItem axes = new JMenuItem("Axes");
        axes.setActionCommand("Axes");
        axes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("Axes")) {
                    JDialog dialog = new JDialog(frame, "Axes");
                    dialog.setLocationRelativeTo(frame);

                    JPanel panel = new JPanel(new GridLayout(2, 1));

                    class X implements RangeListener {
                        XYDisplay display;

                        X(XYDisplay display) {
                            this.display = display;
                        }

                        public void rangeChanged(float low, float high) {
                            xRange[0] = low;
                            xRange[1] = high;
                            display.setXRange(low, high);
                        }
                    }
                    JPanel xpanel = (new lowhigh(new X((XYDisplay) scatterDspMaster), "X:", xRange[0], xRange[1], dataXrange[0], dataXrange[1])).panel;

                    class Y implements RangeListener {
                        XYDisplay display;

                        Y(XYDisplay display) {
                            this.display = display;
                        }

                        public void rangeChanged(float low, float high) {
                            yRange[0] = low;
                            yRange[1] = high;
                            display.setYRange(low, high);
                        }
                    }
                    JPanel ypanel = (new lowhigh(new Y((XYDisplay) scatterDspMaster), "Y:", yRange[0], yRange[1], dataYrange[0], dataYrange[1])).panel;

                    panel.add(xpanel);
                    panel.add(ypanel);
                    dialog.setContentPane(panel);
                    dialog.validate();
                    dialog.setVisible(true);
                    dialog.setSize(dialog.getPreferredSize());

                }
            }
        });


        settingsMenu.add(axes);


        menuBar.add(settingsMenu);

        return menuBar;
    }

    public boolean getSelectByCurve() {
        return selectByCurve;
    }

    private FlatField resample(FlatField X_field, FlatField Y_field) throws VisADException, RemoteException {

        RealTupleType X_domainRef = null;
        RealTupleType Y_domainRef = null;
        float[][] coords = null;
        int[] indexes = null;
        float[][] Yvalues = Y_field.getFloats(false);
        float[][] Xsamples = ((SampledSet) X_field.getDomainSet()).getSamples(false);

        CoordinateSystem X_cs = X_field.getDomainCoordinateSystem();
        if (X_cs == null) {
            RealTupleType X_domain = ((FunctionType) X_field.getType()).getDomain();
        } else {
            X_domainRef = X_cs.getReference();
        }

        CoordinateSystem Y_cs = Y_field.getDomainCoordinateSystem();
        if (Y_cs == null) {
            RealTupleType Y_domain = ((FunctionType) Y_field.getType()).getDomain();
        } else {
            Y_domainRef = Y_cs.getReference();
        }

        if (X_domainRef != null && Y_domainRef != null) {
            Xsamples = X_cs.toReference(Xsamples);
            coords = Y_cs.fromReference(Xsamples);
            indexes = ((SampledSet) Y_field.getDomainSet()).valueToIndex(coords);
        } else if (X_domainRef == null && Y_domainRef != null) {
            Xsamples = Y_cs.fromReference(Xsamples);
            indexes = ((SampledSet) Y_field.getDomainSet()).valueToIndex(Xsamples);
        } else if (X_domainRef != null && Y_domainRef == null) {
            Xsamples = X_cs.toReference(Xsamples);
            Gridded2DSet domSet = (Gridded2DSet) Y_field.getDomainSet();

            // TODO this is a hack for the longitude range problem
            float[] hi = domSet.getHi();
            if (hi[0] <= 180f) {
                for (int t = 0; t < Xsamples[0].length; t++) {
                    if (Xsamples[0][t] > 180f) Xsamples[0][t] -= 360;
                }
            }

            indexes = ((SampledSet) Y_field.getDomainSet()).valueToIndex(Xsamples);
        } else if (X_domainRef == null && Y_domainRef == null) {
            Gridded2DSet domSet = (Gridded2DSet) Y_field.getDomainSet();
            indexes = domSet.valueToIndex(Xsamples);
        }

        float[][] new_values = new float[1][indexes.length];
        for (int k = 0; k < indexes.length; k++) {
            new_values[0][k] = Float.NaN;
            if (indexes[k] >= 0) {
                new_values[0][k] = Yvalues[0][indexes[k]];
            }
        }

        FunctionType ftype = new FunctionType(((FunctionType) X_field.getType()).getDomain(),
                ((FunctionType) Y_field.getType()).getRange());
        Y_field = new FlatField(ftype, X_field.getDomainSet());
        Y_field.setSamples(new_values);

        return Y_field;
    }


    private class ScatterDisplayable extends RGBDisplayable {
        ScatterDisplayable(String name, RealType rgbRealType, float[][] colorPalette, boolean alphaflag)
                throws VisADException, RemoteException {
            super(name, rgbRealType, colorPalette, alphaflag);
        }
    }


    private class ImageCurveSelector extends CellImpl implements DisplayListener {
        boolean init = false;
        CurveDrawer curveDraw;
        DisplayMaster dspMaster;
        Gridded2DSet domainSet;
        CoordinateSystem cs;
        int domainLen_0;
        int domainLen_1;
        ImageCurveSelector other;
        UnionSet last_uSet = null;
        boolean imageLatLon = false;
        boolean active = true;
        float maskVal;
        LineDrawing lastCurve;
        StatsTable myTable = null;
        int myTableIndex = 0;

        ImageCurveSelector(CurveDrawer curveDraw, FlatField image, DisplayMaster master, Color color, float maskVal, StatsTable mst)
                throws VisADException, RemoteException {
            this.curveDraw = curveDraw;
            this.maskVal = maskVal;
            this.myTable = mst;
            myTableIndex = 0;
            if (color == Color.magenta) myTableIndex = 1;
            if (color == Color.green) myTableIndex = 2;
            if (color == Color.blue) myTableIndex = 3;
            dspMaster = master;
            dspMaster.addDisplayListener(this);
            domainSet = (Gridded2DSet) image.getDomainSet();
            int[] lens = domainSet.getLengths();
            domainLen_0 = lens[0];
            domainLen_1 = lens[1];
            cs = ((FunctionType) image.getType()).getDomain().getCoordinateSystem();
            RealTupleType reference = null;
            if (cs != null) {
                reference = cs.getReference();
            } else {
                reference = ((SetType) domainSet.getType()).getDomain();
            }
            RealType[] rtypes = reference.getRealComponents();
            if (rtypes[0].equals(RealType.Latitude)) imageLatLon = true;
            lastCurve = new LineDrawing("lastCurve");
            lastCurve.setColor(color);
            lastCurve.setLineWidth(2);
            master.addDisplayable(lastCurve);
        }

        @Override
        public void displayChanged(DisplayEvent de)
                throws VisADException, RemoteException {
            if ((de.getId() == DisplayEvent.MOUSE_RELEASED) && (active)) {
                UnionSet uSet = curveDraw.getCurves();
                if (uSet == last_uSet) return;
                SampledSet[] sets = uSet.getSets();
                int s_idx = sets.length - 1;
                float[][] crv;

                if (cs != null) {
                    crv = sets[s_idx].getSamples();
                    if (imageLatLon) {
                        float[] tmp = crv[0];
                        crv[0] = crv[1];
                        crv[1] = tmp;
                    }
                    crv = cs.fromReference(crv);
                    crv = domainSet.valueToGrid(crv);
                } else {
                    crv = sets[s_idx].getSamples();
                    crv = domainSet.valueToGrid(crv);
                }

                float[][] onImage = new float[2][crv[0].length];
                int cnt = 0;
                for (int i = 0; i < crv[0].length; i++) {
                    if (((crv[0][i] >= 0) && (crv[0][i] <= domainLen_0)) &&
                            ((crv[1][i] >= 0) && (crv[1][i] <= domainLen_1))) {
                        onImage[0][cnt] = crv[0][i];
                        onImage[1][cnt] = crv[1][i];
                        cnt++;
                    }
                }
                uSet = new UnionSet(new SampledSet[]{sets[s_idx]});
                last_uSet = uSet;
                lastCurve.setData(last_uSet);
                curveDraw.setCurves(uSet);
                other.updateCurve(sets[s_idx]);

                if (cnt == 0) {
                    return;
                }

                float[][] tmp = new float[2][cnt];
                System.arraycopy(onImage[0], 0, tmp[0], 0, cnt);
                System.arraycopy(onImage[1], 0, tmp[1], 0, cnt);
                onImage = tmp;

                float[] minmaxvals = minmax(onImage[0]);
                int low_0 = Math.round(minmaxvals[0]);
                int hi_0 = Math.round(minmaxvals[1]);
                minmaxvals = minmax(onImage[1]);
                int low_1 = Math.round(minmaxvals[0]);
                int hi_1 = Math.round(minmaxvals[1]);

                int len_0 = (hi_0 - low_0) + 1;
                int len_1 = (hi_1 - low_1) + 1;
                int len = len_0 * len_1;

                tmp = new float[3][len];
                int[] tmpsel = new int[len];

                int num_inside = 0;
                for (int j = 0; j < len_1; j++) {
                    for (int i = 0; i < len_0; i++) {
                        int idx = (j + low_1) * domainLen_0 + (i + low_0);
                        float x = (float) (i + low_0);
                        float y = (float) (j + low_1);
                        if (DelaunayCustom.inside(crv, x, y)) {
                            tmp[0][num_inside] = scatterFieldRange[0][idx];
                            tmp[1][num_inside] = scatterFieldRange[1][idx];
                            tmp[2][num_inside] = maskVal;
                            tmpsel[num_inside] = idx;
                            num_inside++;
                        }
                    }
                }
                len = num_inside;
                float[][] markScatter = new float[3][len];
                System.arraycopy(tmp[0], 0, markScatter[0], 0, len);
                System.arraycopy(tmp[1], 0, markScatter[1], 0, len);
                System.arraycopy(tmp[2], 0, markScatter[2], 0, len);


                int last_len = 0;
                float[][] lastMark = ((FlatField) scatterMarkDsp.getData()).getFloats(false);
                tmp = new float[3][lastMark[0].length];
                for (int k = 0; k < lastMark[0].length; k++) {
                    if (lastMark[2][k] != maskVal) {
                        tmp[0][last_len] = lastMark[0][k];
                        tmp[1][last_len] = lastMark[1][k];
                        tmp[2][last_len] = lastMark[2][k];
                        last_len++;
                    }
                }

                float[][] newMarkScatter = new float[3][len + last_len];
                System.arraycopy(tmp[0], 0, newMarkScatter[0], 0, last_len);
                System.arraycopy(tmp[1], 0, newMarkScatter[1], 0, last_len);
                System.arraycopy(tmp[2], 0, newMarkScatter[2], 0, last_len);
                System.arraycopy(markScatter[0], 0, newMarkScatter[0], last_len, len);
                System.arraycopy(markScatter[1], 0, newMarkScatter[1], last_len, len);
                System.arraycopy(markScatter[2], 0, newMarkScatter[2], last_len, len);

                Integer1DSet dset = new Integer1DSet(len + last_len);
                FlatField scatterFieldMark = new FlatField(
                        new FunctionType(RealType.Generic,
                                new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), dset);

                scatterFieldMark.setSamples(newMarkScatter, false);
                scatterMarkDsp.setData(scatterFieldMark);

                if (myTable != null) {
                    int[] selected = new int[len];
                    System.arraycopy(tmpsel, 0, selected, 0, len);
                    total_area = 0f;
                    myTable.setPoints(markScatter, len, myTableIndex, total_area);
                }

            }
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void reset() throws VisADException, RemoteException {
            float[][] lastMark = ((FlatField) scatterMarkDsp.getData()).getFloats(false);
            float[][] tmp = new float[3][lastMark[0].length];
            int cnt = 0;
            for (int k = 0; k < lastMark[0].length; k++) {
                if (lastMark[2][k] != maskVal) {
                    tmp[0][cnt] = lastMark[0][k];
                    tmp[1][cnt] = lastMark[1][k];
                    tmp[2][cnt] = lastMark[2][k];
                    cnt++;
                }
            }

            RealTupleType type = ((SetType) curveDraw.getCurves().getType()).getDomain();
            curveDraw.setCurves(new UnionSet(new Gridded2DSet[]{
                    new Gridded2DSet(type, new float[][]{
                            {0.0f}, {0.0f}}, 1)}));

            lastCurve.setData(new UnionSet(new Gridded2DSet[]{
                    new Gridded2DSet(type, new float[][]{
                            {0.0f}, {0.0f}}, 1)}));

            FlatField scatterFieldMark = null;
            if (cnt == 0) {
                Integer1DSet dset = new Integer1DSet(2);
                scatterFieldMark = new FlatField(
                        new FunctionType(RealType.Generic,
                                new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), dset);
                float[][] markScatter = new float[3][2];
                for (int k = 0; k < 2; k++) {
                    markScatter[0][k] = scatterFieldRange[0][k];
                    markScatter[1][k] = scatterFieldRange[1][k];
                    markScatter[2][k] = 0;
                }
                scatterFieldMark.setSamples(markScatter, false);
            } else {
                Integer1DSet dset = new Integer1DSet(cnt);
                scatterFieldMark = new FlatField(
                        new FunctionType(RealType.Generic,
                                new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), dset);
                float[][] markScatter = new float[3][cnt];
                for (int k = 0; k < cnt; k++) {
                    markScatter[0][k] = tmp[0][k];
                    markScatter[1][k] = tmp[1][k];
                    markScatter[2][k] = tmp[2][k];
                }
                scatterFieldMark.setSamples(markScatter, false);
            }

            scatterMarkDsp.setData(scatterFieldMark);
        }

        public void updateCurve(SampledSet set) throws VisADException, RemoteException {
            last_uSet = new UnionSet(new SampledSet[]{set});
            curveDraw.setCurves(last_uSet);
            lastCurve.setData(last_uSet);
        }

        public void setOther(ImageCurveSelector other) {
            this.other = other;
        }

        @Override
        public void doAction()
                throws VisADException, RemoteException {
            if (!init) {
                init = true;
                return;
            }
        }

        public void setVisible(boolean visible) throws VisADException, RemoteException {
            curveDraw.setVisible(visible);
        }

        public void remove() throws VisADException, RemoteException {
            dspMaster.removeDisplayable(curveDraw);
            dspMaster.removeDisplayable(lastCurve);
            dspMaster.getDisplay().removeDisplayListener(this);
        }

    }

    private class ImageBoxSelector extends CellImpl {
        boolean init = false;
        boolean active = true;
        SubsetRubberBandBox subsetBox;
        DisplayMaster master;
        Set imageDomain;
        int domainLen_0;
        LineDrawing lastBox;
        ImageBoxSelector other;
        float maskVal;
        boolean earthCoordDomain = false;
        StatsTable myTable = null;
        int myTableIndex = 0;

        ImageBoxSelector(SubsetRubberBandBox subsetBox, Set imageDomain, DisplayMaster master, Color color, float maskVal, StatsTable mst)
                throws VisADException, RemoteException {
            super();
            this.myTable = mst;
            myTableIndex = 0;
            if (color == Color.magenta) myTableIndex = 1;
            if (color == Color.green) myTableIndex = 2;
            if (color == Color.blue) myTableIndex = 3;
            this.subsetBox = subsetBox;
            this.imageDomain = imageDomain;
            this.master = master;
            int[] lens = ((Gridded2DSet) imageDomain).getLengths();
            this.maskVal = maskVal;
            domainLen_0 = lens[0];
            lastBox = new LineDrawing("last_box");
            lastBox.setColor(color);
            master.addDisplayable(lastBox);
            subsetBox.addAction(this);
            master.addDisplayable(subsetBox);
            RealTupleType rtt = ((SetType) imageDomain.getType()).getDomain();
            if (rtt.equals(RealTupleType.SpatialEarth2DTuple) ||
                    rtt.equals(RealTupleType.LatitudeLongitudeTuple)) {
                earthCoordDomain = true;
            }
        }

        @Override
        public void doAction()
                throws VisADException, RemoteException {
            if (!init) {
                init = true;
                return;
            }

            if (!active) {
                return;
            }

            Gridded2DSet set = subsetBox.getBounds();
            float[][] corners = set.getSamples(false);
            if (corners == null) return;
            float[][] coords = corners;

            if ((imageDomain instanceof Linear2DSet) || !earthCoordDomain) {
                coords = ((Gridded2DSet) imageDomain).valueToGrid(corners);
            }

            float[] coords_0 = coords[0];
            float[] coords_1 = coords[1];

            int low_0 = Math.round(Math.min(coords_0[0], coords_0[1]));
            int low_1 = Math.round(Math.min(coords_1[0], coords_1[1]));
            int hi_0 = Math.round(Math.max(coords_0[0], coords_0[1]));
            int hi_1 = Math.round(Math.max(coords_1[0], coords_1[1]));

            int len_0 = (hi_0 - low_0) + 1;
            int len_1 = (hi_1 - low_1) + 1;
            int len = len_0 * len_1;

            float[][] markScatter = new float[3][len];
            int[] selected = new int[len];

            for (int j = 0; j < len_1; j++) {
                for (int i = 0; i < len_0; i++) {
                    int idx = (j + low_1) * domainLen_0 + (i + low_0);
                    int k = j * len_0 + i;
                    markScatter[0][k] = scatterFieldRange[0][idx];
                    markScatter[1][k] = scatterFieldRange[1][idx];
                    markScatter[2][k] = maskVal;
                    selected[k] = idx;
                }
            }

            int last_len = 0;
            float[][] lastMark = ((FlatField) scatterMarkDsp.getData()).getFloats(false);
            float[][] tmp = new float[3][lastMark[0].length];
            for (int k = 0; k < lastMark[0].length; k++) {
                if (lastMark[2][k] != maskVal) {
                    tmp[0][last_len] = lastMark[0][k];
                    tmp[1][last_len] = lastMark[1][k];
                    tmp[2][last_len] = lastMark[2][k];
                    last_len++;
                }
            }

            float[][] newMarkScatter = new float[3][len + last_len];
            System.arraycopy(tmp[0], 0, newMarkScatter[0], 0, last_len);
            System.arraycopy(tmp[1], 0, newMarkScatter[1], 0, last_len);
            System.arraycopy(tmp[2], 0, newMarkScatter[2], 0, last_len);
            System.arraycopy(markScatter[0], 0, newMarkScatter[0], last_len, len);
            System.arraycopy(markScatter[1], 0, newMarkScatter[1], last_len, len);
            System.arraycopy(markScatter[2], 0, newMarkScatter[2], last_len, len);

            Integer1DSet dset = new Integer1DSet(len + last_len);
            FlatField scatterFieldMark = new FlatField(
                    new FunctionType(RealType.Generic,
                            new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), dset);

            scatterFieldMark.setSamples(newMarkScatter, false);
            scatterMarkDsp.setData(scatterFieldMark);

            if (myTable != null) {
                total_area = 0f;
                myTable.setPoints(markScatter, len, myTableIndex, total_area);
            }

            updateBox();
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void setVisible(boolean visible) throws VisADException, RemoteException {
            subsetBox.setVisible(visible);
            if (visible) {
                lastBox.setVisible(visible);
            }
        }

        public void reset() throws VisADException, RemoteException {
            Gridded2DSet set2D =
                    new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                            new float[][]{{0}, {0}}, 1);
            lastBox.setVisible(false);
            lastBox.setData(set2D);

            float[][] lastMark = ((FlatField) scatterMarkDsp.getData()).getFloats(false);
            float[][] tmp = new float[3][lastMark[0].length];
            int cnt = 0;
            for (int k = 0; k < lastMark[0].length; k++) {
                if (lastMark[2][k] != maskVal) {
                    tmp[0][cnt] = lastMark[0][k];
                    tmp[1][cnt] = lastMark[1][k];
                    tmp[2][cnt] = lastMark[2][k];
                    cnt++;
                }
            }

            FlatField scatterFieldMark;
            if (cnt == 2) {
                Integer1DSet dset = new Integer1DSet(2);
                scatterFieldMark = new FlatField(
                        new FunctionType(RealType.Generic,
                                new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), dset);
                float[][] markScatter = new float[3][2];
                for (int k = 0; k < 2; k++) {
                    markScatter[0][k] = scatterFieldRange[0][k];
                    markScatter[1][k] = scatterFieldRange[1][k];
                    markScatter[2][k] = 0;
                }
                scatterFieldMark.setSamples(markScatter, false);
            } else {
                Integer1DSet dset = new Integer1DSet(cnt);
                scatterFieldMark = new FlatField(
                        new FunctionType(RealType.Generic,
                                new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), dset);
                float[][] markScatter = new float[3][cnt];
                for (int k = 0; k < cnt; k++) {
                    markScatter[0][k] = tmp[0][k];
                    markScatter[1][k] = tmp[1][k];
                    markScatter[2][k] = tmp[2][k];
                }
                scatterFieldMark.setSamples(markScatter, false);
            }

            scatterMarkDsp.setData(scatterFieldMark);
        }

        public void setOther(ImageBoxSelector other) {
            this.other = other;
        }

        public void updateBox() throws VisADException, RemoteException {
            Gridded3DSet set3D = subsetBox.getLastBox();
            float[][] samples = set3D.getSamples(false);
            Gridded2DSet set2D =
                    new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                            new float[][]{samples[0], samples[1]}, samples[0].length);
            lastBox.setData(set2D);
            other.updateBox(set2D);
        }

        public void updateBox(Gridded2DSet set2D) throws VisADException, RemoteException {
            lastBox.setData(set2D);
        }

        public void remove() throws VisADException, RemoteException {
            master.removeDisplayable(subsetBox);
            master.removeDisplayable(lastBox);
        }

    }

    private class ScatterBoxSelector extends CellImpl {
        boolean init = false;
        double[] x_coords = new double[2];
        double[] y_coords = new double[2];
        RubberBandBox rbb;
        LineDrawing selectBox;
        boolean active = true;
        float maskVal = 0;

        ScatterBoxSelector(DisplayMaster master, Color color, float maskVal) throws VisADException, RemoteException {
            selectBox = new LineDrawing("select");
            selectBox.setColor(color);
            selectBox.setLineWidth(2);

            rbb = new RubberBandBox(RealType.XAxis, RealType.YAxis, 0);
            rbb.setColor(color);
            rbb.addAction(this);

            master.addDisplayable(rbb);
            master.addDisplayable(selectBox);
            this.maskVal = maskVal;
        }


        @Override
        public void doAction() throws VisADException, RemoteException {
            if (!init) {
                init = true;
                return;
            }

            if (!active) {
                return;
            }

            Gridded2DSet set = (Gridded2DSet) rbb.getData();
            float[] low = set.getLow();
            float[] hi = set.getHi();
            x_coords[0] = low[0];
            x_coords[1] = hi[0];
            y_coords[0] = low[1];
            y_coords[1] = hi[1];

            SampledSet[] sets = new SampledSet[4];
            sets[0] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][]{{low[0], hi[0]}, {low[1], low[1]}}, 2);
            sets[1] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][]{{hi[0], hi[0]}, {low[1], hi[1]}}, 2);
            sets[2] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][]{{hi[0], low[0]}, {hi[1], hi[1]}}, 2);
            sets[3] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][]{{low[0], low[0]}, {hi[1], low[1]}}, 2);
            UnionSet uset = new UnionSet(sets);
            selectBox.setData(uset);

            try {
                histoField.markMaskFieldByRange(x_coords, y_coords, maskVal);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setVisible(boolean visible) throws VisADException, RemoteException {
            rbb.setVisible(visible);
            if (visible) {
                selectBox.setVisible(visible);
            }
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void reset() throws Exception {
            if (!active) return;
            selectBox.setVisible(false);
            selectBox.setData(new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][]{{0f, 0f}, {0f, 0f}}, 2));
            histoField.resetMaskField(maskVal);
        }
    }

    private class ScatterCurveSelector extends CellImpl implements DisplayListener {
        CurveDrawer curveDraw;
        boolean init = false;
        UnionSet last_uSet = null;
        boolean active = true;
        float maskVal = 0;
        LineDrawing selectCurve;
        DisplayMaster master;

        ScatterCurveSelector(DisplayMaster master, Color color, float maskVal) throws VisADException, RemoteException {
            curveDraw = new CurveDrawer(RealType.XAxis, RealType.YAxis, 1);
            curveDraw.setColor(color);
            curveDraw.setLineWidth(2);
            curveDraw.setData(new UnionSet(new Gridded2DSet[]{
                    new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][]{
                            {0f}, {0f}
                    }, 1)}));

            selectCurve = new LineDrawing("select");
            selectCurve.setColor(color);
            selectCurve.setLineWidth(2);
            this.master = master;
            master.addDisplayable(curveDraw);
            master.addDisplayable(selectCurve);
            this.maskVal = maskVal;

            curveDraw.addAction(this);
            master.addDisplayListener(this);
        }

        @Override
        public void displayChanged(DisplayEvent de)
                throws VisADException, RemoteException {
            if ((de.getId() == DisplayEvent.MOUSE_RELEASED) && (active)) {
                UnionSet uSet = curveDraw.getCurves();
                if (uSet == last_uSet) return;
                SampledSet[] sets = uSet.getSets();
                int s_idx = sets.length - 1;
                float[][] crv;

                crv = sets[s_idx].getSamples();
                last_uSet = new UnionSet(new SampledSet[]{sets[s_idx]});
                curveDraw.setCurves(last_uSet);
                selectCurve.setData(last_uSet);

                try {
                    histoField.clearMaskField(maskVal);
                    histoField.markMaskFieldByCurve(crv, maskVal);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void doAction() throws VisADException, RemoteException {
            if (!init) {
                init = true;
                return;
            }
        }

        public void setVisible(boolean visible) throws VisADException, RemoteException {
            curveDraw.setVisible(visible);
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void reset() throws Exception {
            if (!active) return;
            curveDraw.setData(new UnionSet(new Gridded2DSet[]{
                    new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][]{
                            {0f}, {0f}
                    }, 1)}));
            selectCurve.setData(new UnionSet(new Gridded2DSet[]{
                    new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][]{
                            {0f}, {0f}
                    }, 1)}));
            histoField.resetMaskField(maskVal);
        }

        public void remove() {
            master.getDisplay().removeDisplayListener(this);
        }
    }

    private class BoxCurveSwitch implements ActionListener {

        public BoxCurveSwitch() {
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            String cmd = ae.getActionCommand();
            try {
                update(cmd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void update(String cmd) throws Exception {
            if (cmd.equals("Box")) {
                selectByCurve = false;
                for (int k = 0; k < 3; k++) {
                    ImageCurveSelector imageXcrv = (ImageCurveSelector) imageXCurveSelectors.get(k);
                    ImageCurveSelector imageYcrv = (ImageCurveSelector) imageYCurveSelectors.get(k);
                    ScatterCurveSelector scatCrv = (ScatterCurveSelector) scatterCurveSelectors.get(k);

                    scatCrv.setActive(true);
                    scatCrv.reset();
                    scatCrv.setActive(false);
                    scatCrv.setVisible(false);

                    imageXcrv.setActive(true);
                    imageXcrv.reset();
                    imageXcrv.setActive(false);
                    imageXcrv.setVisible(false);

                    imageYcrv.setActive(true);
                    imageYcrv.reset();
                    imageYcrv.setActive(false);
                    imageYcrv.setVisible(false);

                    if (k == currentSelectIndex) {
                        scatterBoxSelectors.get(k).setActive(true);
                        scatterBoxSelectors.get(k).setVisible(true);
                        imageXBoxSelectors.get(k).setActive(true);
                        imageXBoxSelectors.get(k).setVisible(true);
                        imageYBoxSelectors.get(k).setActive(true);
                        imageYBoxSelectors.get(k).setVisible(true);
                    }
                }
            } else if (cmd.equals("Curve")) {
                selectByCurve = true;
                for (int k = 0; k < 3; k++) {
                    ImageBoxSelector imageXbox = (ImageBoxSelector) imageXBoxSelectors.get(k);
                    ImageBoxSelector imageYbox = (ImageBoxSelector) imageYBoxSelectors.get(k);
                    ScatterBoxSelector scatBox = (ScatterBoxSelector) scatterBoxSelectors.get(k);

                    scatBox.setActive(true);
                    scatBox.reset();
                    scatBox.setActive(false);
                    scatBox.setVisible(false);

                    imageXbox.setActive(true);
                    imageXbox.reset();
                    imageXbox.setActive(false);
                    imageXbox.setVisible(false);

                    imageYbox.setActive(true);
                    imageYbox.reset();
                    imageYbox.setActive(false);
                    imageYbox.setVisible(false);

                    if (k == currentSelectIndex) {
                        scatterCurveSelectors.get(k).setActive(true);
                        scatterCurveSelectors.get(k).setVisible(true);
                        imageXCurveSelectors.get(k).setActive(true);
                        imageXCurveSelectors.get(k).setVisible(true);
                        imageYCurveSelectors.get(k).setActive(true);
                        imageYCurveSelectors.get(k).setVisible(true);
                    }
                }
            }
        }
    }

    public static float[] minmax(float[] values) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int k = 0; k < values.length; k++) {
            float val = values[k];
            if ((val == val) && (val < Float.POSITIVE_INFINITY) && (val > Float.NEGATIVE_INFINITY)) {
                if (val < min) min = val;
                if (val > max) max = val;
            }
        }
        return new float[]{min, max};
    }
}

class lowhigh {
    JTextField lowTxtFld = new JTextField(6);
    JTextField highTxtFld = new JTextField(6);

    JPanel panel = new JPanel(new FlowLayout());

    float low;
    float high;
    ScalarMap smap;
    XYDisplay display;
    int which;
    String label;
    float[] initRange = new float[]{Float.NaN, Float.NaN};

    RangeListener rangeListener;

    public lowhigh(RangeListener rngListener, String label, float low, float high, float initLow, float initHigh) {
        this.rangeListener = rngListener;
        this.label = label;
        this.low = low;
        this.high = high;
        this.which = which;
        this.initRange[0] = initLow;
        this.initRange[1] = initHigh;

        DecimalFormat numFmt = Hydra.getDecimalFormat(low);
        lowTxtFld.setText(numFmt.format(low));
        highTxtFld.setText(numFmt.format(high));

        lowTxtFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String lowTxt = lowTxtFld.getText().trim();
                    String hiTxt = highTxtFld.getText().trim();
                    updateRange(Float.valueOf(lowTxt), Float.valueOf(hiTxt));
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });

        highTxtFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String lowTxt = lowTxtFld.getText().trim();
                    String hiTxt = highTxtFld.getText().trim();
                    updateRange(Float.valueOf(lowTxt), Float.valueOf(hiTxt));
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });

        JButton button = new JButton("reset");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateRange(initRange[0], initRange[1]);
                DecimalFormat numFmt = Hydra.getDecimalFormat(initRange[0]);
                lowTxtFld.setText(numFmt.format(initRange[0]));
                highTxtFld.setText(numFmt.format(initRange[1]));
            }
        });

        panel.add(new JLabel(label));
        panel.add(lowTxtFld);
        panel.add(highTxtFld);
        panel.add(button);
    }

    public void updateRange(float low, float high) {
        this.low = low;
        this.high = high;

        rangeListener.rangeChanged(low, high);
    }
}

interface RangeListener {
    public void rangeChanged(float low, float high);
}
