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

import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.ReprojectSwath;
import visad.CoordinateSystem;

import visad.Data;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.RealType;
import visad.Linear2DSet;
import visad.Set;
import visad.georef.MapProjection;

public class FourChannelCombine extends Compute {

    JLabel[] colorComponents;
    JComponent[] operandComponents;

    LineBorder[] borders;
    LineBorder[] borders3;

    JComponent operandA;
    JComponent operandB;
    JComboBox comboAB;
    JComponent operandC;
    JComponent operandD;
    JComboBox comboCD;
    JComboBox comboLR;
    String operationAB = "-";
    String operationCD = " ";
    String operationLR = " ";
    boolean[] operandEnabled;

    JTextField multplrA;
    JTextField multplrB;
    JTextField multplrC;
    JTextField multplrD;

    FlatField swathImage;
    FlatField result;

    boolean needResample = false;
    boolean needReproject = true;

    String dateTimeStr = null;
    String sourceDesc = null;

    public FourChannelCombine() {
    }

    public FourChannelCombine(DataBrowser dataBrowser) {
        super(4, 3, "Band Math");
        this.dataBrowser = dataBrowser;
        operators[0] = operationAB;
        operators[1] = operationCD;
        operators[2] = operationLR;
    }

    public JComponent buildGUI() {
        JTextArea textPanel = new JTextArea(" Select items in main window to update target (bold box) operand.\n" + " Target operand advances automatically, but can be manually selected.");
        textPanel.setEditable(false);
        textPanel.setCursor(null);
        textPanel.setOpaque(false);
        textPanel.setFocusable(false);
        JPanel outerPanel = new JPanel(new GridLayout(4, 1));

        LineBorder blackBorder = new LineBorder(Color.black);
        LineBorder blackBorder3 = new LineBorder(Color.black, 3);

        JPanel panel = new JPanel(new FlowLayout());
        textPanel.setBackground(panel.getBackground());
        colorComponents = new JLabel[numOperands];
        borders = new LineBorder[numOperands];
        borders3 = new LineBorder[numOperands];

        operandEnabled = new boolean[numOperands];
        final String[] compNames = new String[numOperands];

        for (int k = 0; k < colorComponents.length; k++) {
            JLabel label = new JLabel();
            borders[k] = blackBorder;
            borders3[k] = blackBorder3;
            compNames[k] = "           ";
            operandEnabled[k] = true;
            label.setText(compNames[k]);
            label.setBorder(borders[k]);
            colorComponents[k] = label;

            label.addMouseListener(new java.awt.event.MouseAdapter() {
                                       public void mouseClicked(java.awt.event.MouseEvent e) {
                                           for (int k = 0; k < colorComponents.length; k++) {
                                               if (e.getComponent() == colorComponents[k]) {
                                                   setActive(k);
                                               }
                                           }
                                       }
                                   }
            );
        }
        colorComponents[activeIndex].setBorder(borders3[activeIndex]);

        operandEnabled[2] = false;
        operandEnabled[3] = false;

        String[] operations = new String[]{"-", "+", "/", "*", " "};
        operandA = colorComponents[0];
        operandB = colorComponents[1];
        comboAB = new JComboBox(operations);
        comboAB.setSelectedIndex(0);
        comboAB.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                operationAB = (String) comboAB.getSelectedItem();
                operators[0] = operationAB;
            }
        });

        operandC = colorComponents[2];
        operandD = colorComponents[3];
        operations = new String[]{"+", "-", "*", "/", " "};
        comboCD = new JComboBox(operations);
        comboCD.setSelectedIndex(4);
        comboCD.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                operationCD = (String) comboCD.getSelectedItem();
                operators[1] = operationCD;
                if (operationCD == " ") {
                    disableOperand(3);
                } else {
                    enableOperand(3);
                }
            }
        });

        operations = new String[]{"/", "+", "-", "*", " "};
        comboLR = new JComboBox(operations);
        comboLR.setSelectedIndex(4);
        comboLR.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                operationLR = (String) comboLR.getSelectedItem();
                operators[2] = operationLR;
                if (operationLR == " ") {
                    disableOperand(2);
                    disableOperand(3);

                    comboCD.setSelectedItem(" ");
                } else {
                    enableOperand(2);
                }
            }
        });

        //Left
        panel.add(new JLabel("("));
        panel.add(new JLabel("a*"));
        panel.add(operandA);
        panel.add(comboAB);
        panel.add(new JLabel("b*"));
        panel.add(operandB);
        panel.add(new JLabel(")"));

        panel.add(comboLR);

        //Right
        panel.add(new JLabel("("));
        panel.add(new JLabel("c*"));
        panel.add(operandC);
        panel.add(comboCD);
        panel.add(new JLabel("d*"));
        panel.add(operandD);
        panel.add(new JLabel(")"));

        JPanel panel3 = new JPanel(new FlowLayout());
        JLabel lblA = new JLabel("a=");
        JLabel lblB = new JLabel("b=");
        JLabel lblC = new JLabel("c=");
        JLabel lblD = new JLabel("d=");
        multplrA = new JTextField("1", 2);
        multplrB = new JTextField("1", 2);
        multplrC = new JTextField("1", 2);
        multplrD = new JTextField("1", 2);
        panel3.add(lblA);
        panel3.add(multplrA);
        panel3.add(lblB);
        panel3.add(multplrB);
        panel3.add(lblC);
        panel3.add(multplrC);
        panel3.add(lblD);
        panel3.add(multplrD);

        outerPanel.add(textPanel);
        outerPanel.add(panel);
        outerPanel.add(panel3);

        return outerPanel;
    }

    public void disableOperand(int idx) {
        colorComponents[idx].setText("           ");
        operands[idx].setEmpty();
        operandEnabled[idx] = false;
    }

    public void enableOperand(int idx) {
        operandEnabled[idx] = true;
        setActive(idx);
    }

    public void setActive(int idx) {
        if (!operandEnabled[idx]) return;

        super.setActive(idx);

        for (int k = 0; k < colorComponents.length; k++) {
            if (k == activeIndex) {
                colorComponents[k].setBorder(borders3[k]);
            } else {
                colorComponents[k].setBorder(borders[k]);
            }
        }
    }

    public void updateUI(SelectionEvent e) {
        String name = e.getName();
        colorComponents[activeIndex].setText(name);
        int next = activeIndex;
        while (true) {
            next = ((next + 1) % numOperands);
            if (operandEnabled[next]) {
                setActive(next);
                break;
            }
        }
    }

    public Data compute() throws Exception {
        Operand operandA = operands[0];
        Operand operandB = operands[1];
        Operand operandC = operands[2];
        Operand operandD = operands[3];

        operationAB = (String) operators[0];
        operationCD = (String) operators[1];
        operationLR = (String) operators[2];

        FlatField fldA = null;
        FlatField fldB = null;
        FlatField fldC = null;
        FlatField fldD = null;

        String nameA = null;
        String nameB = null;
        String nameC = null;
        String nameD = null;

        float fltA = 1f;
        float fltB = 1f;
        float fltC = 1f;
        float fltD = 1f;

        try {
            fltA = Float.valueOf(multplrA.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            fltB = Float.valueOf(multplrB.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            fltC = Float.valueOf(multplrC.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            fltD = Float.valueOf(multplrD.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }

        needResample = false;
        Linear2DSet commonGrid = null; // Grid to which operand data is resampled (if necessary)
        float nadirResolution;

        fldA = (FlatField) operandA.getData();
        if (fltA != 1f) {
            fldA = multiply(fltA, fldA);
        }
        Linear2DSet setA = (Linear2DSet) fldA.getDomainSet();
        nameA = operandA.getName();
        nadirResolution = operandA.dataSource.getNadirResolution(operandA.dataChoice);
        needReproject = operandA.dataSource.getDoReproject(operandA.dataChoice);
        dateTimeStr = operandA.dataSource.getDateTimeStamp();
        sourceDesc = operandA.dataSource.getDescription();

        CoordinateSystem coordSysA = fldA.getDomainCoordinateSystem();
        CoordinateSystem coordSysB = null;
        CoordinateSystem coordSysC = null;
        CoordinateSystem coordSysD = null;
        boolean allGEOS = coordSysA instanceof GEOSProjection;

        boolean needReproA;
        boolean needReproB;
        boolean needReproC;
        boolean needReproD;

        boolean needResmplA;
        boolean needResmplB;
        boolean needResmplC;
        boolean needResmplD;

        if (!operandB.isEmpty()) {
            fldB = (FlatField) operandB.getData();
            if (fltB != 1f) {
                fldB = multiply(fltB, fldB);
            }
            Linear2DSet setB = (Linear2DSet) fldB.getDomainSet();
            nameB = operandB.getName();
            needResample = !((operandA.dataSource == operandB.dataSource) && (setA.equals(setB)));
            float res = operandB.dataSource.getNadirResolution(operandB.dataChoice);
            needReproB = operandB.dataSource.getDoReproject(operandB.dataChoice);
            if (res > nadirResolution) nadirResolution = res;
            coordSysB = fldB.getDomainCoordinateSystem();
            allGEOS = allGEOS && (coordSysB instanceof GEOSProjection);
        }
        if (!operandC.isEmpty()) {
            fldC = (FlatField) operandC.getData();
            if (fltC != 1f) {
                fldC = multiply(fltC, fldC);
            }
            Linear2DSet setC = (Linear2DSet) fldC.getDomainSet();
            nameC = operandC.getName();
            needResample = !((operandA.dataSource == operandC.dataSource) && (setA.equals(setC)));
            float res = operandC.dataSource.getNadirResolution(operandC.dataChoice);
            needReproC = operandC.dataSource.getDoReproject(operandC.dataChoice);
            if (res > nadirResolution) nadirResolution = res;
            coordSysC = fldC.getDomainCoordinateSystem();
            allGEOS = allGEOS && (coordSysC instanceof GEOSProjection);
        }
        if (!operandD.isEmpty()) {
            fldD = (FlatField) operandD.getData();
            if (fltD != 1f) {
                fldD = multiply(fltD, fldD);
            }
            Linear2DSet setD = (Linear2DSet) fldD.getDomainSet();
            nameD = operandD.getName();
            needResample = !((operandA.dataSource == operandD.dataSource) && (setA.equals(setD)));
            float res = operandD.dataSource.getNadirResolution(operandD.dataChoice);
            needReproD = operandD.dataSource.getDoReproject(operandD.dataChoice);
            if (res > nadirResolution) nadirResolution = res;
            coordSysD = fldD.getDomainCoordinateSystem();
            allGEOS = allGEOS && (coordSysD instanceof GEOSProjection);
        }

        String operName;

        FunctionType ftypeA = (FunctionType) fldA.getType();
        Set dSetA = fldA.getDomainSet();

        int mode = Hydra.getReprojectMode();

        int visadMode = Data.NEAREST_NEIGHBOR;
        if (mode == 0) {
            visadMode = Data.NEAREST_NEIGHBOR;
        } else if (mode == 2) {
            visadMode = Data.WEIGHTED_AVERAGE;
        }

        if (needResample && allGEOS) {
            fldA = GOESgridUtil.makeGEOSRadiansDomainField(fldA, (GEOSProjection) coordSysA);

            if (fldB != null) {
                fldB = GOESgridUtil.makeGEOSRadiansDomainField(fldB, (GEOSProjection) coordSysB);
                if (!fldB.getDomainSet().equals(fldA.getDomainSet())) {
                    fldB = GOESgridUtil.goesResample(fldB, (Linear2DSet) fldA.getDomainSet(), visadMode);
                }
            }
            if (fldC != null) {
                fldC = GOESgridUtil.makeGEOSRadiansDomainField(fldC, (GEOSProjection) coordSysC);
                if (!fldC.getDomainSet().equals(fldA.getDomainSet())) {
                    fldC = GOESgridUtil.goesResample(fldC, (Linear2DSet) fldA.getDomainSet(), visadMode);
                }
            }
            if (fldD != null) {
                fldD = GOESgridUtil.makeGEOSRadiansDomainField(fldD, (GEOSProjection) coordSysD);
                if (!fldD.getDomainSet().equals(fldA.getDomainSet())) {
                    fldD = GOESgridUtil.goesResample(fldD, (Linear2DSet) fldA.getDomainSet(), visadMode);
                }
            }
        } else if (needResample) {
            float[][] corners = MultiSpectralData.getLonLatBoundingCorners(fldA.getDomainSet());
            MapProjection mp = Hydra.getSwathProjection(corners);
            commonGrid = Hydra.makeGrid(mp, corners, nadirResolution);

            if (operandA.dataSource.getReduceBowtie(operandA.dataChoice)) {
                String sensorName = operandA.dataSource.getSensorName(operandA.dataChoice);
                Hydra.reduceSwathBowtie(fldA, sensorName);
            }
            fldA = ReprojectSwath.swathToGrid(commonGrid, fldA, mode);

            if (fldB != null) {
                if (operandB.dataSource.getReduceBowtie(operandB.dataChoice)) {
                    String sensorName = operandB.dataSource.getSensorName(operandB.dataChoice);
                    Hydra.reduceSwathBowtie(fldB, sensorName);
                }
                fldB = ReprojectSwath.swathToGrid(commonGrid, fldB, mode);
            }
            if (fldC != null) {
                if (operandC.dataSource.getReduceBowtie(operandC.dataChoice)) {
                    String sensorName = operandC.dataSource.getSensorName(operandC.dataChoice);
                    Hydra.reduceSwathBowtie(fldC, sensorName);
                }
                fldC = ReprojectSwath.swathToGrid(commonGrid, fldC, mode);
            }
            if (fldD != null) {
                if (operandD.dataSource.getReduceBowtie(operandD.dataChoice)) {
                    String sensorName = operandD.dataSource.getSensorName(operandD.dataChoice);
                    Hydra.reduceSwathBowtie(fldD, sensorName);
                }
                fldD = ReprojectSwath.swathToGrid(commonGrid, fldD, mode);
            }
        }


        FieldImpl fldAB = null;
        if (null != operationAB) switch (operationAB) {
            case "-":
                fldAB = (FieldImpl) fldA.subtract(fldB, visadMode, Data.NO_ERRORS);
                break;
            case "+":
                fldAB = (FieldImpl) fldA.add(fldB, visadMode, Data.NO_ERRORS);
                break;
            case "/":
                fldAB = (FieldImpl) fldA.divide(fldB, visadMode, Data.NO_ERRORS);
                fldAB = Hydra.infiniteToNaN(fldAB);
                break;
            case "*":
                fldAB = (FieldImpl) fldA.multiply(fldB, visadMode, Data.NO_ERRORS);
                break;
            case " ":
                fldAB = fldA;
                break;
            default:
                break;
        }

        FieldImpl fldCD = null;
        if (!operandD.isEmpty) {
            if (null != operationCD) switch (operationCD) {
                case "-":
                    fldCD = (FieldImpl) fldC.subtract(fldD, visadMode, Data.NO_ERRORS);
                    break;
                case "+":
                    fldCD = (FieldImpl) fldC.add(fldD, visadMode, Data.NO_ERRORS);
                    break;
                case "*":
                    fldCD = (FieldImpl) fldC.multiply(fldD, visadMode, Data.NO_ERRORS);
                    break;
                case "/":
                    fldCD = (FieldImpl) fldC.divide(fldD, visadMode, Data.NO_ERRORS);
                    fldCD = Hydra.infiniteToNaN(fldCD);
                    break;
                default:
                    break;
            }
        } else if (!operandC.isEmpty) {
            fldCD = fldC;
        }

        FlatField fld = (FlatField) fldAB;

        if (fldAB != null && fldCD != null) {
            if (null != operationLR) switch (operationLR) {
                case "-":
                    fld = (FlatField) fldAB.subtract(fldCD, visadMode, Data.NO_ERRORS);
                    break;
                case "+":
                    fld = (FlatField) fldAB.add(fldCD, visadMode, Data.NO_ERRORS);
                    break;
                case "*":
                    fld = (FlatField) fldAB.multiply(fldCD, visadMode, Data.NO_ERRORS);
                    break;
                case "/":
                    fld = (FlatField) fldAB.divide(fldCD, visadMode, Data.NO_ERRORS);
                    fld = (FlatField) Hydra.infiniteToNaN(fld);
                    break;
                default:
                    break;
            }
        }

        if (allGEOS && needResample) {
            FlatField tmp = new FlatField(ftypeA, dSetA);
            tmp.setSamples(fld.getFloats(false), false);
            fld = tmp;
        }

        operName = getOperationName();

        fld = Hydra.cloneButRangeType(RealType.getRealType(operName), fld, false);

        if (!needResample) { // if already resampled, don't resample again
            swathImage = null; // not swath domain
        } else {
            swathImage = fld;
        }
        result = fld;

        return fld;
    }

    public String getOperationName() { // what to call this?
        Operand operandA = operands[0];
        Operand operandB = operands[1];
        Operand operandC = operands[2];
        Operand operandD = operands[3];

        String operName = null;
        String nameAB = null;
        String nameCD = null;

        String nameA = null;
        if (!operandA.isEmpty()) {
            nameA = operandA.getName();
            nameA = nameA.trim();
            String txt = multplrA.getText().trim();
            float flt = Float.valueOf(txt);
            if (flt != 1f) {
                nameA = txt + "*" + nameA;
            }
            operName = nameA;
        }

        String nameB = null;
        if (!operandB.isEmpty()) {
            nameB = operandB.getName();
            nameB = nameB.trim();
            String txt = multplrB.getText().trim();
            float flt = Float.valueOf(txt);
            if (flt != 1f) {
                nameB = txt + "*" + nameB;
            }
            nameAB = nameA + operationAB + nameB;
            operName = nameAB;
        }

        String nameC = null;
        if (!operandC.isEmpty()) {
            nameC = operandC.getName();
            nameC = nameC.trim();
            String txt = multplrC.getText().trim();
            float flt = Float.valueOf(txt);
            if (flt != 1f) {
                nameC = txt + "*" + nameC;
            }
            operName = "[" + operName + "]" + operationLR + nameC;

            if (!operandD.isEmpty()) {
                String nameD = operandD.getName();
                nameD = nameD.trim();
                txt = multplrD.getText().trim();
                flt = Float.valueOf(txt);
                if (flt != 1f) {
                    nameD = txt + "*" + nameD;
                }
                nameCD = nameC + operationCD + nameD;
                operName = "[" + nameAB + "]" + operationLR + "[" + nameCD + "]";
            }
        }

        operName = operName.trim();
        operName = operName.replace('.', ',');

        return operName;
    }

    public void createDisplay(Data data, int mode, int windowNumber) throws Exception {
        FlatField fld = (FlatField) data;

        MapProjection mp;
        if (!needResample && needReproject) {
            Operand operandA = operands[0];
            float nadirResolution = operandA.dataSource.getNadirResolution(operandA.dataChoice);
            float[][] corners = MultiSpectralData.getLonLatBoundingCorners(fld.getDomainSet());
            mp = Hydra.getSwathProjection(corners);
            Linear2DSet grd = Hydra.makeGrid(mp, corners, nadirResolution);
            if (operandA.dataSource.getReduceBowtie(operandA.dataChoice)) {
                String sensorName = operandA.dataSource.getSensorName(operandA.dataChoice);
                Hydra.reduceSwathBowtie(fld, sensorName);
            }
            fld = ReprojectSwath.swathToGrid(grd, fld, Hydra.getReprojectMode());
        } else {
            mp = Hydra.getDataProjection(fld);
        }

        String name = ((RealType) ((FunctionType) fld.getType()).getRange()).getName();

        DatasetInfo dsInfo = new DatasetInfo(name, new DataSourceInfo(sourceDesc, dateTimeStr));

        if (mode == 0 || ImageDisplay.getTarget() == null) {
            HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(fld, null, Hydra.grayTable, name);
            if (swathImage != null) {
                Hydra.displayableToImage.put(imageDsp, swathImage);
            }
            ImageDisplay iDisplay = new ImageDisplay(imageDsp, mp, windowNumber, dsInfo, false);
        } else if (mode == 1) {
            ImageDisplay.getTarget().updateImageData(fld, Hydra.grayTable, mp, dsInfo);
        } else if (mode == 2) {
            fld = Hydra.makeFlatFieldWithUniqueRange(fld);
            HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(fld, null, Hydra.grayTable, name);
            if (swathImage != null) {
                Hydra.displayableToImage.put(imageDsp, swathImage);
            }
            ImageDisplay.getTarget().addOverlayImage(imageDsp, dsInfo);
        }
    }

    public FourChannelCombine clone() {
        FourChannelCombine clone = new FourChannelCombine();
        clone.dataBrowser = this.dataBrowser;
        copy(clone);
        clone.operationAB = this.operationAB;
        clone.operationCD = this.operationCD;
        clone.operationLR = this.operationLR;
        clone.multplrA = new JTextField(multplrA.getText());
        clone.multplrB = new JTextField(multplrB.getText());
        clone.multplrC = new JTextField(multplrC.getText());
        clone.multplrD = new JTextField(multplrD.getText());
        return clone;
    }

    public void selectionPerformed(SelectionEvent e) {
        super.selectionPerformed(e);
    }

    public static FlatField multiply(float fval, FlatField fltFld) throws Exception {
        FlatField newFF = new FlatField((FunctionType) fltFld.getType(), fltFld.getDomainSet());
        float[][] values = fltFld.getFloats();
        for (int t = 0; t < values.length; t++) {
            for (int i = 0; i < values[t].length; i++) {
                values[t][i] *= fval;
            }
        }
        newFF.setSamples(values, false);
        return newFF;
    }
}
