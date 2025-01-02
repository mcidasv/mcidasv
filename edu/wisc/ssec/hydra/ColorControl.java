/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
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

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.ArrayList;
import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JSplitPane;

import visad.BaseColorControl;
import visad.FlatField;
import visad.ScalarMap;
import visad.ScalarMapControlEvent;
import visad.ScalarMapEvent;
import visad.ScalarMapListener;
import visad.VisADException;
import visad.CellImpl;


import ucar.unidata.util.ColorTable;
import ucar.visad.display.DisplayMaster;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JFrame;


public class ColorControl implements DepictionControl, PropertyChangeListener {

    /**
     * Displayable for the data
     */
    private HydraRGBDisplayable imageDisplay;

    private DisplayMaster displayMaster;

    float[][] clrTable = null;

    final private double[] clrRange = new double[]{Double.NaN, Double.NaN};

    final double[] initClrRange = new double[]{Double.NaN, Double.NaN};

    private double gamma = 1.0;

    private boolean hasRange = false;

    private final JTextField gammaTxtFld = new JTextField(java.lang.Float.toString(1.0f), 3);
    private final JTextField lowTxtFld = new JTextField(6);
    private final JTextField highTxtFld = new JTextField(6);
    private JComboBox clrScaleSelect = null;

    private ScalarMap colorMap = null;

    private ColorMapListener clrMapListnr = null;

    private String clrTblName = null;

    private Histogram histo = null;

    JSplitPane guiComponent = null;

    /* The Depiction to which this Control belongs */
    Depiction depiction = null;

    private ArrayList<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();

    private JFrame frame = null;

    public ColorControl(HydraRGBDisplayable imageDisplay) throws VisADException, RemoteException {
        super();
        this.imageDisplay = imageDisplay;
        this.clrTblName = imageDisplay.getColorPaletteName();
        //setColorMap();
        lowTxtFld.setHorizontalAlignment(JTextField.LEADING);
        highTxtFld.setHorizontalAlignment(JTextField.LEADING);
        gammaTxtFld.setHorizontalAlignment(JTextField.LEADING);

        colorMap = imageDisplay.getColorMap();

        double[] rng = colorMap.getRange();
        initClrRange[0] = rng[0];
        initClrRange[1] = rng[1];

        clrTable = imageDisplay.getColorPalette();

        if (!Double.isNaN(initClrRange[0])) { //comes with an initial range, so AUTO_SCALE is locked out
            DecimalFormat numFmt = Hydra.getDecimalFormat(rng[0]);
            lowTxtFld.setText(numFmt.format(rng[0]));
            highTxtFld.setText(numFmt.format(rng[1]));
            clrRange[0] = Double.valueOf(lowTxtFld.getText().trim());
            clrRange[1] = Double.valueOf(highTxtFld.getText().trim());
        }


        clrMapListnr = new ColorMapListener(this, colorMap, initClrRange, clrRange, lowTxtFld, highTxtFld);
        colorMap.addScalarMapListener(clrMapListnr);

        if (!Double.isNaN(initClrRange[0])) {
            histo = new Histogram((FlatField) imageDisplay.getData(), new float[]{(float) initClrRange[0], (float) initClrRange[1]});
        } else {
            histo = new Histogram((FlatField) imageDisplay.getData());
        }

        histo.setListener(this);

        imageDisplay.addAction(new DataChangeListener());
    }

    public void reset() {
        //TODO: generalize/fix this - where 'remove' is called shouldn't matter?
        //colorMap.removeScalarMapListener(clrMapListnr);
        //setColorMap();
        colorMap = imageDisplay.getColorMap();

        double[] rng = colorMap.getRange();
        initClrRange[0] = rng[0];
        initClrRange[1] = rng[1];

        clrMapListnr = new ColorMapListener(this, colorMap, initClrRange, clrRange, lowTxtFld, highTxtFld);
        colorMap.addScalarMapListener(clrMapListnr);
    }

    private void setColorMap() {
        colorMap = imageDisplay.getColorMap();

        double[] rng = colorMap.getRange();
        initClrRange[0] = rng[0];
        initClrRange[1] = rng[1];

        clrMapListnr = new ColorMapListener(this, colorMap, initClrRange, clrRange, lowTxtFld, highTxtFld);
        colorMap.addScalarMapListener(clrMapListnr);
    }

    boolean checkRange() {
        if (Double.isNaN(clrRange[0])) {
            return false;
        } else {
            return true;
        }
    }

    private void updateClrRange(double lo, double hi) {
        DecimalFormat numFmt = Hydra.getDecimalFormat(clrRange[0]);
        double cr_lo = Double.valueOf(numFmt.format(clrRange[0]).trim());
        double cr_hi = Double.valueOf(numFmt.format(clrRange[1]).trim());
        if ((cr_lo == lo) && (cr_hi == hi)) {
            return;
        }
        clrRange[0] = lo;
        clrRange[1] = hi;
        try {
            colorMap.setRange(lo, hi);
            if (histo != null) {
                histo.updateColorBarForRange(lo, hi);
            }
        } catch (VisADException ex) {
            ex.printStackTrace();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    public void setColorTableNoChange(ColorTable clrTbl) {
        ActionListener[] listeners = clrScaleSelect.getActionListeners();

        for (int k = 0; k < listeners.length; k++) {
            clrScaleSelect.removeActionListener(listeners[k]);
        }

        clrScaleSelect.setSelectedItem(clrTbl.getName());

        for (int k = 0; k < listeners.length; k++) {
            clrScaleSelect.addActionListener(listeners[k]);
        }

        setColorTable(clrTbl.getName(), false);
    }

    public void setColorTable(ColorTable clrTbl) {
        clrScaleSelect.setSelectedItem(clrTbl.getName());
    }

    private void setColorTable(String name) {
        setColorTable(name, true);
    }

    private void setColorTable(String name, boolean change) {
        float[][] table = null;

        if (name.equals("Rainbow")) {
            table = ColorTable.addAlpha(Hydra.rainbow);
        } else if (name.equals("InvRainbow")) {
            table = ColorTable.addAlpha(Hydra.invRainbow);
        } else if (name.equals("gray")) {
            table = ColorTable.addAlpha(Hydra.grayTable);
        } else if (name.equals("invGray")) {
            table = ColorTable.addAlpha(Hydra.invGrayTable);
        } else if (name.equals("Heat")) {
            table = ColorTable.addAlpha(Hydra.heat);
        }

        if (table == null) {
            return;
        }

        clrTable = table;

        gamma = 1.0;
        gammaTxtFld.setText(Float.toString(1.0f));

        try {
            if (histo != null) {
                histo.updateColorBar(table);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (change) {
            applyColorTable(table);
        }

        firePropertyChange(new PropertyChangeEvent(this, "ColorScale", null, table));
    }

    public float[][] getColorTable() {
        return ((BaseColorControl) colorMap.getControl()).getTable();
    }

    private void applyColorTable(float[][] table) {
        int tblClrDim = table.length;

        try {
            BaseColorControl clrCntrl = (BaseColorControl) colorMap.getControl();
            int clrDim = clrCntrl.getNumberOfComponents();

            if (clrDim == tblClrDim) {
                clrCntrl.setTable(table);
            } else if (clrDim == 3) {
                if (tblClrDim == 4) {
                    table = new float[][]{table[0], table[1], table[2]};
                }
            } else { // trusted, must be 4
                if (tblClrDim == 3) { // need to expand alpha. Set to opaque: alpha = 1
                    int numClrs = clrCntrl.getNumberOfColors();
                    float[] alpha = new float[numClrs];
                    java.util.Arrays.fill(alpha, 1f);
                    table = new float[][]{table[0], table[1], table[2], alpha};
                }
            }
            clrCntrl.setTable(table);
        } catch (VisADException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setClrRange(double[] range) {
        double lo = range[0];
        double hi = range[1];

        clrRange[0] = lo;
        clrRange[1] = hi;
        try {
            colorMap.setRange(lo, hi);
        } catch (VisADException ex) {
            ex.printStackTrace();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }

        DecimalFormat numFmt = Hydra.getDecimalFormat(range[0]);
        lowTxtFld.setText(numFmt.format(range[0]));
        highTxtFld.setText(numFmt.format(range[1]));
    }

    private void firePropertyChange(PropertyChangeEvent evt) {
        if (listeners.isEmpty()) {
            return;
        }

        Iterator<PropertyChangeListener> iter = listeners.iterator();
        while (iter.hasNext()) {
            iter.next().propertyChange(evt);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        double[] rng = (double[]) evt.getNewValue();
        setClrRange(rng);
    }

    public double[] getClrRange() {
        return new double[]{clrRange[0], clrRange[1]};
    }

    public double[][] getDataRange() {
        return new double[][]{{initClrRange[0], initClrRange[1]}};
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public double getGamma() {
        return gamma;
    }

    private void updateGamma(double gamma) {
        setGamma(gamma);

        float[][] newClrTbl = applyGammaToTable(clrTable, gamma);

        try {
            if (histo != null) {
                histo.updateColorBar(newClrTbl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        applyColorTable(newClrTbl);
        firePropertyChange(new PropertyChangeEvent(this, "ColorScale", null, newClrTbl));
    }

    public float[][] applyGammaToTable(float[][] clrTable, double gamma) {
        float[][] newClrTbl = getZeroOutArray(clrTable);

        for (int k = 0; k < clrTable[0].length; k++) {
            newClrTbl[0][k] = (float) Math.pow(clrTable[0][k], gamma);
            newClrTbl[1][k] = (float) Math.pow(clrTable[1][k], gamma);
            newClrTbl[2][k] = (float) Math.pow(clrTable[2][k], gamma);
        }

        return newClrTbl;
    }

    public float[][] getZeroOutArray(float[][] array) {
        float[][] newArray = new float[array.length][array[0].length];
        for (int i = 0; i < newArray.length; i++) {
            for (int j = 0; j < newArray[0].length; j++) {
                newArray[i][j] = 0f;
            }
        }
        if (newArray.length == 4) { // test if table has alpha component
            for (int j = 0; j < newArray[0].length; j++) {
                newArray[3][j] = 1.0f; // initialize to opaque
            }
        }
        return newArray;
    }

    public Container doMakeContents() {

        JSplitPane bigPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        guiComponent = bigPanel;

        JPanel subPanel = new JPanel(new FlowLayout());

        JPanel redPanel = new JPanel(new FlowLayout());
        redPanel.add(new JLabel("range:"));

        lowTxtFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String lowTxt = lowTxtFld.getText().trim();
                String hiTxt = highTxtFld.getText().trim();
                updateClrRange(Double.valueOf(lowTxt), Double.valueOf(hiTxt));
            }
        });
        lowTxtFld.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
            }

            public void focusLost(FocusEvent e) {
                String lowTxt = lowTxtFld.getText().trim();
                String hiTxt = highTxtFld.getText().trim();
                updateClrRange(Double.valueOf(lowTxt), Double.valueOf(hiTxt));
            }

        });
        redPanel.add(lowTxtFld);
        highTxtFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String hiTxt = highTxtFld.getText().trim();
                String lowTxt = lowTxtFld.getText().trim();
                updateClrRange(Double.valueOf(lowTxt), Double.valueOf(hiTxt));
            }
        });
        highTxtFld.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
            }

            public void focusLost(FocusEvent e) {
                String lowTxt = lowTxtFld.getText().trim();
                String hiTxt = highTxtFld.getText().trim();
                updateClrRange(Double.valueOf(lowTxt), Double.valueOf(hiTxt));
            }

        });
        redPanel.add(highTxtFld);

        gammaTxtFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String tmp = gammaTxtFld.getText().trim();
                updateGamma(Double.valueOf(tmp));
            }
        });
        redPanel.add(new JLabel("Gamma:"));
        redPanel.add(gammaTxtFld);

        JButton button = new JButton("reset");
        redPanel.add(button);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateClrRange(initClrRange[0], initClrRange[1]);
                clrRange[0] = initClrRange[0];
                clrRange[1] = initClrRange[1];
                DecimalFormat numFmt = Hydra.getDecimalFormat(clrRange[0]);
                lowTxtFld.setText(numFmt.format(clrRange[0]));
                highTxtFld.setText(numFmt.format(clrRange[1]));
            }
        });

        JButton saveB = new JButton("Save");
        redPanel.add(saveB);
        saveB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (frame != null) {
                    DisplayCapture.capture(frame, histo.getDisplay(), "jpeg");
                }
            }
        });

        subPanel.add(redPanel);

        String[] clrScales = new String[]{"invGray", "gray", "Rainbow", "InvRainbow", "Heat"};
        clrScaleSelect = new JComboBox(clrScales);
        clrScaleSelect.setSelectedItem(clrTblName);
        clrScaleSelect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String clrTblName = (String) clrScaleSelect.getSelectedItem();
                setColorTable(clrTblName);
            }
        });


        subPanel.add(clrScaleSelect);

        subPanel.setMinimumSize(subPanel.getPreferredSize());
        bigPanel.setTopComponent(subPanel);

        try {
            bigPanel.setBottomComponent(histo.makeDisplay(clrTable));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bigPanel;
    }

    public void setDepiction(Depiction depiction) {
        if (this.depiction == null) { // Only set once
            this.depiction = depiction;
        }
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    public Depiction getDepiction() {
        return depiction;
    }

    public void destroy() {
        guiComponent.removeAll();
        if (histo != null) {
            histo.destroy();
        }
        imageDisplay = null;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.add(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    private class DataChangeListener extends CellImpl {
        boolean init = false;
        FlatField last = null;

        public void doAction() throws VisADException, RemoteException {
            if (init) {
                if (histo != null) {
                    FlatField fltFld = (FlatField) imageDisplay.getData();
                    // This is a workaround to problem with setMapProjection generating suplurfuous
                    // data change events: which should only occur with setData or setSamples on
                    // a DataReference.  TODO: investigate this.
                    if (last != fltFld) {
                        histo.updateDisplay(fltFld);
                    }
                    last = fltFld;
                }
            } else {
                init = true;
            }
        }
    }

    private class ColorMapListener implements ScalarMapListener {
        ScalarMap clrMap;

        double[] range = null;
        double[] initRange = null;

        JTextField lowTxtFld;
        JTextField highTxtFld;

        ColorControl clrCntrl; // HYDRA class

        ColorMapListener(ColorControl clrCntrl, ScalarMap clrMap, double[] initRange, double[] range, JTextField lowTxtFld, JTextField highTxtFld) {
            this.clrMap = clrMap;
            this.lowTxtFld = lowTxtFld;
            this.highTxtFld = highTxtFld;
            this.range = range;
            this.initRange = initRange;
            this.clrCntrl = clrCntrl;
        }

        public void controlChanged(ScalarMapControlEvent event) throws RemoteException, VisADException {
        }

        public void mapChanged(ScalarMapEvent event) throws RemoteException, VisADException {
            if (event.getId() == event.AUTO_SCALE) {
                double[] rng = clrMap.getRange();
                range[0] = rng[0];
                range[1] = rng[1];
                initRange[0] = rng[0];
                initRange[1] = rng[1];

                DecimalFormat numFmt = Hydra.getDecimalFormat(rng[0]);
                lowTxtFld.setText(numFmt.format(rng[0]));
                highTxtFld.setText(numFmt.format(rng[1]));

                clrCntrl.firePropertyChange(new PropertyChangeEvent(this.clrCntrl, "range", null, new double[]{rng[0], rng[1]}));
            
            /* Turn-off auto scaling after we get the first range compute.
               ResetAutoScale can still be used later. 
               This eliminates unnecessary calls to computeRange especially noted multiple data linked to display.
            */
                clrMap.disableAutoScale();
            } else if (event.getId() == event.MANUAL) {
                double[] rng = clrMap.getRange();
                range[0] = rng[0];
                range[1] = rng[1];

                clrCntrl.firePropertyChange(new PropertyChangeEvent(this.clrCntrl, "range", null, new double[]{rng[0], rng[1]}));
            }
        }
    }

}
