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

import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.hydra.data.DataSource;

import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextField;


import visad.Data;
import visad.FlatField;
import visad.RealType;
import visad.FunctionType;
import visad.georef.MapProjection;


public class HyperToBroadBand extends Compute implements ActionListener {

    JLabel[] colorComponents;
    JLabel[] operandComponents;

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

    MultiSpectralDisplay multiSpectDsp;
    float cntrWavenum;
    float wavenumL = Float.NaN;
    float wavenumR = Float.NaN;
    float deltaWavenum = 200;


    String dateTimeStr = null;

    HydraRGBDisplayable thisImageDsp = null;

    ImageDisplay imageDisplay;

    boolean tophat = true;
    boolean gauss = false;
    String prob;
    String kType;

    String selectorID_left;
    String selectorID_rght;

    JTextField leftWaveNum;
    JTextField rghtWaveNum;

    Kernel kernel = Kernel.TH;

    JFrame parentFrame;

    public HyperToBroadBand() {
    }

    public HyperToBroadBand(String idLeft, String idRght, DataBrowser dataBrowser, DataSource dataSource, MultiSpectralDisplay msd, ImageDisplay imageDisplay) {
        this.dataBrowser = dataBrowser;
        this.multiSpectDsp = msd;
        this.numOperands = 1;

        Operand oper = new Operand();
        oper.dataSource = dataSource;
        oper.dataSourceId = dataSource.getDataSourceId();
        this.operands = new Operand[]{oper};
        this.imageDisplay = imageDisplay;
        selectorID_left = idLeft;
        selectorID_rght = idRght;
    }


    public JComponent buildGUI() {
        JPanel panel = new JPanel(new FlowLayout());

        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("Kernel");

        ButtonGroup bg = new ButtonGroup();

        JMenuItem topHat = new JCheckBoxMenuItem(Kernel.TH.guiName, true);
        topHat.setActionCommand(Kernel.TH.guiName);
        topHat.addActionListener(this);
        menu.add(topHat);

        JMenu gaussM = new JMenu("Gauss");
        JMenuItem prob68 = new JCheckBoxMenuItem(Kernel.G68.guiName, false);
        prob68.setActionCommand(Kernel.G68.guiName);
        prob68.addActionListener(this);
        gaussM.add(prob68);

        JMenuItem fwhm = new JCheckBoxMenuItem(Kernel.FWHM.guiName, false);
        fwhm.setActionCommand(Kernel.FWHM.guiName);
        fwhm.addActionListener(this);
        gaussM.add(fwhm);

        JMenuItem prob95 = new JCheckBoxMenuItem(Kernel.G95.guiName, false);
        prob95.setActionCommand(Kernel.G95.guiName);
        prob95.addActionListener(this);
        gaussM.add(prob95);

        JMenuItem prob99 = new JCheckBoxMenuItem(Kernel.G99.guiName, false);
        prob99.setActionCommand(Kernel.G99.guiName);
        prob99.addActionListener(this);
        gaussM.add(prob99);

        bg.add(topHat);
        bg.add(prob68);
        bg.add(prob95);
        bg.add(prob99);

        menu.add(gaussM);

        menuBar.add(menu);

        JPanel panelB = new JPanel(new FlowLayout());
        leftWaveNum = (JTextField) doMakeHyperSpectralSelectComponent(selectorID_left);
        rghtWaveNum = (JTextField) doMakeHyperSpectralSelectComponent(selectorID_rght);
        panelB.add(leftWaveNum);
        panelB.add(rghtWaveNum);

        panel.add(panelB);
        panel.add(menuBar);

        return panel;
    }

    public JComponent doMakeHyperSpectralSelectComponent(final String id) {
        final JTextField wavenumbox = new JTextField(Float.toString(multiSpectDsp.getWaveNumber()), 8);
        wavenumbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String tmp = wavenumbox.getText().trim();
                try {
                    multiSpectDsp.setSelectorValue(id, Float.valueOf(tmp));
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });

        return wavenumbox;
    }

    public void actionPerformed(ActionEvent evt) {
        String cmd = evt.getActionCommand();
        if (cmd.equals(Kernel.TH.guiName)) {
            kernel = Kernel.TH;
        } else if (cmd.equals(Kernel.G68.guiName)) {
            kernel = Kernel.G68;
        } else if (cmd.equals(Kernel.FWHM.guiName)) {
            kernel = Kernel.FWHM;
        } else if (cmd.equals(Kernel.G95.guiName)) {
            kernel = Kernel.G95;
        } else if (cmd.equals(Kernel.G99.guiName)) {
            kernel = Kernel.G99;
        }
    }

    @Override
    public void updateUI(SelectionEvent e) {
    }

    public void updateOperandComp(int index, Object obj) {
        float fval = ((Float) obj);
        if (index == 0) {
            wavenumL = fval;
            if (leftWaveNum != null) {
                leftWaveNum.setText(Float.toString(fval));
            }
        } else if (index == 1) {
            wavenumR = fval;
            if (rghtWaveNum != null) {
                rghtWaveNum.setText(Float.toString(fval));
            }
        }
        if (!Float.isNaN(wavenumL) && !Float.isNaN(wavenumR)) {
            cntrWavenum = wavenumL + (wavenumR - wavenumL) / 2;
        }
    }

    @Override
    public Data compute() throws Exception {
        int loIdx = multiSpectDsp.getChannelIndex(wavenumL);
        int hiIdx = multiSpectDsp.getChannelIndex(wavenumR);

        int dir = 1;
        int numChans = (hiIdx - loIdx) + 1;
        if (hiIdx < loIdx) {
            numChans = (loIdx - hiIdx) + 1;
            dir = -1;
        }

        // top hat
        float[] wght = new float[numChans];

        if (kernel.equals(Kernel.TH)) {
            for (int k = 0; k < numChans; k++) {
                wght[k] = 1f / numChans;
            }
        } else {
            // gaussian
            double sigma = 1.0;
            // For sigma == 1
            // -1.0 < x < 1.0 ~ 68%
            // -1.1675 < x < 1.1675 ~ 76%
            // -2 < x < 2 ~ 95%
            // -3 < x < 3 ~ 99.7%

            float fac = 1;
            fac = kernel.halfWidth;

            if (sigma != 1) {
                fac = 1;
            }

            // gaussian
            float wghtSum = 0;
            float scale = fac / (wavenumR - cntrWavenum);
            for (int k = 0; k < numChans; k++) {
                int chanIdx = loIdx + k * dir;
                float waveNum = multiSpectDsp.getMultiSpectralData().getWavenumberFromChannelIndex(chanIdx);
                float dist = (waveNum - cntrWavenum) * scale;
                float distsqrd = dist * dist;

                wght[k] = (float) (1.0 / (Math.sqrt(2.0 * Math.PI) * sigma) * Math.exp(-distsqrd / (2 * sigma * sigma)));
                wghtSum += wght[k];
            }
            for (int k = 0; k < numChans; k++) {
                wght[k] /= wghtSum;
            }
        }

        FlatField swath = multiSpectDsp.makeConvolvedRadiances(new int[]{loIdx, loIdx + (numChans - 1)}, wght);

        MultiSpectralData msd = multiSpectDsp.getMultiSpectralData();
        swath = msd.convertImage(swath, cntrWavenum, "BrightnessTemp");

        // have to replace decimal with comma - VisAD doesn't '.' or ' ' in RealType names
        String noDotName = (Float.toString(wavenumL) + "_" + Float.toString(wavenumR) + "_" + kernel.toString()).replace(".", ",");
        swath = Hydra.cloneButRangeType(RealType.getRealType(noDotName), swath, false);

        // No-op except for CrIS
        swath = reproject(swath);

        return swath;
    }

    public FlatField reproject(FlatField swath) throws Exception {
        return swath;
    }

    public HyperToBroadBand clone() {
        HyperToBroadBand clone = new HyperToBroadBand();
        clone.dataBrowser = this.dataBrowser;
        clone.multiSpectDsp = this.multiSpectDsp;
        clone.cntrWavenum = multiSpectDsp.getWaveNumber();
        clone.cntrWavenum = this.cntrWavenum;
        clone.wavenumL = this.wavenumL;
        clone.wavenumR = this.wavenumR;
        clone.imageDisplay = this.imageDisplay;
        clone.operands = new Operand[]{this.operands[0]};
        clone.kernel = this.kernel;
        copy(clone);
        return clone;
    }

    public String getOperationName() {
        String operName = Float.toString(wavenumL) + "_" + Float.toString(wavenumR) + ":" + kernel.toString();
        return operName;
    }

    public void createDisplay(Data data, int mode, int windowNumber) throws Exception {
        FlatField fld = (FlatField) data;
        String name = ((RealType) ((FunctionType) fld.getType()).getRange()).getName();
        name = name.replace(",", ".");
        MapProjection mp = Hydra.getDataProjection(fld);
        DatasetInfo dsInfo = new DatasetInfo(name, new DataSourceInfo(dateTimeStr));

        if (mode == 0 || ImageDisplay.getTarget() == null) {
            HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(fld, null, Hydra.grayTable, name);
            ImageDisplay iDisplay = new ImageDisplay(imageDsp, mp, windowNumber, dsInfo, false);
        } else if (mode == 1) {
            ImageDisplay.getTarget().updateImageData(fld, Hydra.invGrayTable, mp, dsInfo);
        } else if (mode == 2) {
            fld = Hydra.makeFlatFieldWithUniqueRange(fld);
            HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(fld, null, Hydra.invGrayTable, name);
            ImageDisplay.getTarget().addOverlayImage(imageDsp, dsInfo);
        }
    }

    public void localCreateDisplay(Data data) throws Exception {
        FlatField fld = (FlatField) data;
        String name = ((RealType) ((FunctionType) fld.getType()).getRange()).getName();
        name = name.replace(",", ".");
        MapProjection mp = Hydra.getDataProjection(fld);
        DatasetInfo dsInfo = new DatasetInfo(name, new DataSourceInfo(dateTimeStr));

        fld = Hydra.makeFlatFieldWithUniqueRange(fld);
        if (thisImageDsp == null) {
            HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(fld, null, Hydra.invGrayTable, name);
            imageDisplay.addOverlayImage(imageDsp, dsInfo, false, false);
            thisImageDsp = imageDsp;
        } else {
            imageDisplay.updateImageData(thisImageDsp, fld, Hydra.invGrayTable, mp, dsInfo);
        }
    }

    public void destroy() {
        imageDisplay.removeDisplayable(thisImageDsp);
    }

    void setParentFrame(JFrame frame) {
        parentFrame = frame;
    }

    public JFrame getParentFrame() {
        return parentFrame;
    }

    public enum Kernel {
        TH(Float.NaN, "Boxcar"),
        G68(1f, "68%"),
        FWHM(1.1676f, "FWHM"),
        G95(2f, "95%"),
        G99(3f, "99.7%");

        public final float halfWidth;
        public final String guiName;

        Kernel(float halfWidth, String guiName) {
            this.halfWidth = halfWidth;
            this.guiName = guiName;
        }
    }

    // A standalone static for generating convolution weights.
    public static float[] generate_convolution_weights(Kernel kernel, MultiSpectralData msd,
                                                       float wavenumL, float cntrWavenum, float wavenumR)
            throws Exception {

        int loIdx = msd.getChannelIndexFromWavenumber(wavenumL);
        int hiIdx = msd.getChannelIndexFromWavenumber(wavenumR);

        int dir = 1;
        int numChans = (hiIdx - loIdx) + 1;
        if (hiIdx < loIdx) {
            numChans = (loIdx - hiIdx) + 1;
            dir = -1;
        }

        // top hat
        float[] wght = new float[numChans];

        if (kernel.equals(Kernel.TH)) {
            for (int k = 0; k < numChans; k++) {
                wght[k] = 1f / numChans;
            }
        } else {
            // gaussian
            double sigma = 1.0;
            // For sigma == 1
            // -1.0 < x < 1.0 ~ 68%
            // -1.1675 < x < 1.1675 ~ 76%
            // -2 < x < 2 ~ 95%
            // -3 < x < 3 ~ 99.7%

            float fac = 1;
            fac = kernel.halfWidth;

            if (sigma != 1) {
                fac = 1;
            }

            // gaussian
            float wghtSum = 0;
            float scale = fac / (wavenumR - cntrWavenum);
            for (int k = 0; k < numChans; k++) {
                int chanIdx = loIdx + k * dir;
                float waveNum = msd.getWavenumberFromChannelIndex(chanIdx);
                float dist = (waveNum - cntrWavenum) * scale;
                float distsqrd = dist * dist;

                wght[k] = (float) (1.0 / (Math.sqrt(2.0 * Math.PI) * sigma) * Math.exp(-distsqrd / (2 * sigma * sigma)));
                wghtSum += wght[k];
            }
            for (int k = 0; k < numChans; k++) {
                wght[k] /= wghtSum;
            }
        }
        return wght;
    }
}
