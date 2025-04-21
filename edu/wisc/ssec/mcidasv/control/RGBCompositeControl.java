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
package edu.wisc.ssec.mcidasv.control;

import edu.wisc.ssec.mcidasv.data.hydra.ImageRGBDisplayable;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.LogUtil;
import ucar.visad.display.DisplayMaster;

import visad.*;
import visad.georef.MapProjection;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.Container;
import java.awt.FlowLayout;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Iterator;

public class RGBCompositeControl extends DisplayControlImpl {

    public final static String FORMULA_IN_PROGRESS_FLAG = "Formula_Active";
    private static final Logger logger = LoggerFactory.getLogger(RGBCompositeControl.class);
    /** Displayable for the data */
    private ImageRGBDisplayable imageDisplay;

    private DisplayMaster displayMaster;

    private ScalarMap redMap = null;
    private ScalarMap grnMap = null;
    private ScalarMap bluMap = null;

    float[][] redTable = null;
    float[][] grnTable = null;
    float[][] bluTable = null;

    final private double[] redRange = { Double.NaN, Double.NaN };
    final private double[] grnRange = { Double.NaN, Double.NaN };
    final private double[] bluRange = { Double.NaN, Double.NaN };

    final double[] initRedRange = { Double.NaN, Double.NaN };
    final double[] initGrnRange = { Double.NaN, Double.NaN };
    final double[] initBluRange = { Double.NaN, Double.NaN };

    private FieldImpl imageField = null;
    private MapProjection mapProjection = null;

    private static final double DEFAULT_GAMMA = 1.0d;
    private double gamma = DEFAULT_GAMMA;

    private double redGamma = DEFAULT_GAMMA;
    private double grnGamma = DEFAULT_GAMMA;
    private double bluGamma = DEFAULT_GAMMA;

    private JCheckBox matchFieldsCbox = null;

    private final JTextField gammaTxtFld =
        new JTextField(Double.toString(gamma), 4);
    private final JTextField redGammaTxtFld =
        new JTextField(Double.toString(redGamma), 4);
    private final JTextField grnGammaTxtFld =
        new JTextField(Double.toString(grnGamma), 4);
    private final JTextField bluGammaTxtFld =
        new JTextField(Double.toString(bluGamma), 4);

    private final JTextField redLowTxtFld =
        new JTextField(Float.toString(1.0f), 10);
    private final JTextField redHighTxtFld =
        new JTextField(Float.toString(1.0f), 10);
    private final JTextField grnLowTxtFld =
        new JTextField(Float.toString(1.0f), 10);
    private final JTextField grnHighTxtFld =
        new JTextField(Float.toString(1.0f), 10);
    private final JTextField bluLowTxtFld =
        new JTextField(Float.toString(1.0f), 10);
    private final JTextField bluHighTxtFld =
        new JTextField(Float.toString(1.0f), 10);

    public boolean init(DataChoice dataChoice) throws VisADException, RemoteException {

        displayMaster = getViewManager().getMaster();
        DataSelection dataSelection = getDataSelection();

        // TJJ Jul 2014
        // by sharing a property via the active View Manager, we can signal all three
        // preview windows they are part of an in-progress RGB Composite. If so, it
        // appears we need to use a shared HydraContext so our geographic coverage
        // subset applies across channels.

        Hashtable ht = getIdv().getViewManager().getProperties();
        ht.put(FORMULA_IN_PROGRESS_FLAG, true);

        imageField = (FieldImpl) dataChoice.getData(dataSelection);

        imageDisplay = new ImageRGBDisplayable("rgb composite", null, false, imageField);

        ht.put(FORMULA_IN_PROGRESS_FLAG, false);

        Iterator iter = imageDisplay.getScalarMapSet().iterator();
        while (iter.hasNext()) {
            ScalarMap map = (ScalarMap) iter.next();
            if (map.getScalarName().startsWith("redimage")) {
                redMap = map;
            }
            if (map.getScalarName().startsWith("greenimage")) {
                grnMap = map;
            }
            if (map.getScalarName().startsWith("blueimage")) {
                bluMap = map;
            }
        }

        if (checkRange()) { //- from unpersistence if true, initialize gui, ScalarMaps
            double[] redRange = getRedRange();
            double[] grnRange = getGrnRange();
            double[] bluRange = getBluRange();

            initRedRange[0] = redRange[0];
            initRedRange[1] = redRange[1];
            initGrnRange[0] = grnRange[0];
            initGrnRange[1] = grnRange[1];
            initBluRange[0] = bluRange[0];
            initBluRange[1] = bluRange[1];

            redLowTxtFld.setText(Float.toString((float)redRange[0]));
            redHighTxtFld.setText(Float.toString((float)redRange[1]));
            grnLowTxtFld.setText(Float.toString((float)grnRange[0]));
            grnHighTxtFld.setText(Float.toString((float)grnRange[1]));
            bluLowTxtFld.setText(Float.toString((float)bluRange[0]));
            bluHighTxtFld.setText(Float.toString((float)bluRange[1]));

            gammaTxtFld.setText(Float.toString((float)gamma));
            redGammaTxtFld.setText(Float.toString((float)redGamma));
            grnGammaTxtFld.setText(Float.toString((float)grnGamma));
            bluGammaTxtFld.setText(Float.toString((float)bluGamma));

            redMap.setRange(redRange[0], redRange[1]);
            grnMap.setRange(grnRange[0], grnRange[1]);
            bluMap.setRange(bluRange[0], bluRange[1]);
        } else {
            redMap.resetAutoScale();
            grnMap.resetAutoScale();
            bluMap.resetAutoScale();

            redMap.addScalarMapListener(new ColorMapListener(redMap, initRedRange, redRange, redLowTxtFld, redHighTxtFld));
            grnMap.addScalarMapListener(new ColorMapListener(grnMap, initGrnRange, grnRange, grnLowTxtFld, grnHighTxtFld));
            bluMap.addScalarMapListener(new ColorMapListener(bluMap, initBluRange, bluRange, bluLowTxtFld, bluHighTxtFld));
        }

        setShowInDisplayList(true);

        addDisplayable(imageDisplay, FLAG_COLORTABLE | FLAG_ZPOSITION);

        return true;
    }

    public void initDone() {
        while (true) {
            if (null != redMap.getControl()) {
                redTable = ((BaseColorControl) redMap.getControl()).getTable();
                break;
            }
        }
        while (true) {
            if (null != grnMap.getControl()) {
                grnTable = ((BaseColorControl) grnMap.getControl()).getTable();
                break;
            }
        }

        while (true) {
            if (null != bluMap.getControl()) {
                bluTable = ((BaseColorControl) bluMap.getControl()).getTable();
                break;
            }
        }

        float[][] newRedTbl = getZeroOutArray(redTable);
        float[][] newGrnTbl = getZeroOutArray(grnTable);
        float[][] newBluTbl = getZeroOutArray(bluTable);

        for (int k=0; k<redTable[0].length; k++) {
            newRedTbl[0][k] = (float) Math.pow(redTable[0][k], redGamma);
            newGrnTbl[1][k] = (float) Math.pow(grnTable[1][k], grnGamma);
            newBluTbl[2][k] = (float) Math.pow(bluTable[2][k], bluGamma);
        }

        try {
            displayMaster.setDisplayInactive();
            ((BaseColorControl)redMap.getControl()).setTable(newRedTbl);
            ((BaseColorControl)grnMap.getControl()).setTable(newGrnTbl);
            ((BaseColorControl)bluMap.getControl()).setTable(newBluTbl);
            imageDisplay.loadData(imageField);
            displayMaster.setDisplayActive();
        } catch (Exception ex) {
            LogUtil.logException("setDisplayInactive", ex);
        }
    }

    public MapProjection getDataProjection() {
        CoordinateSystem cs = null;
        try {
            if (imageField instanceof FlatField) {
                cs = ((FunctionType)imageField.getType()).getDomain().getCoordinateSystem();
            }
            else if (imageField instanceof FieldImpl) {
                Data dat = imageField.getSample(0, false);
                if (dat instanceof FlatField) {
                    FlatField img = (FlatField) dat;
                    cs = ((FunctionType)img.getType()).getDomain().getCoordinateSystem();
                }
            }
        }
        catch (Exception ex) {
            LogUtil.logException("problem accessing data", ex);
        }

        if (cs instanceof MapProjection) {
            mapProjection = (MapProjection)cs;
        }

        return mapProjection;
    }

    boolean checkRange() {
        return !(Double.isNaN(redRange[0]) || Double.isNaN(grnRange[0]) || Double.isNaN(bluRange[0]));
    }

    private void updateRedRange(double lo, double hi) {
        redRange[0] = lo;
        redRange[1] = hi;
        redHighTxtFld.setText(Float.toString((float)hi));
        redLowTxtFld.setText(Float.toString((float)lo));
        try {
            redMap.setRange(lo, hi);
        } catch (VisADException | RemoteException ex) {
            LogUtil.logException("redMap.setRange", ex);
        }
    }

    public double[] getRedRange() {
        return new double[] {redRange[0], redRange[1]};
    }

    private void updateGrnRange(double lo, double hi) {
        grnRange[0] = lo;
        grnRange[1] = hi;
        grnHighTxtFld.setText(Float.toString((float)hi));
        grnLowTxtFld.setText(Float.toString((float)lo));
        try {
            grnMap.setRange(lo, hi);
        } catch (VisADException | RemoteException ex) {
            LogUtil.logException("grnMap.setRange", ex);
        }
    }

    public double[] getGrnRange() {
        return new double[] {grnRange[0], grnRange[1]};
    }

    private void updateBluRange(double lo, double hi) {
        bluRange[0] = lo;
        bluRange[1] = hi;
        bluHighTxtFld.setText(Float.toString((float)hi));
        bluLowTxtFld.setText(Float.toString((float)lo));
        try {
            bluMap.setRange(lo, hi);
        } catch (VisADException | RemoteException ex) {
            LogUtil.logException("bluMap.setRange", ex);
        }
    }

    public double[] getBluRange() {
        return new double[] {bluRange[0], bluRange[1]};
    }

    public void setRedGamma(double gamma) {
        redGamma = gamma;
    }

    public void setGrnGamma(double gamma) {
        grnGamma = gamma;
    }

    public void setBluGamma(double gamma) {
        bluGamma = gamma;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public double getGamma() {
        return gamma;
    }

    /**
     * TJJ - quick hack, just do something visually jarring to test path
     */

    /**
     * Computes a Rayleigh scattering corrected 2D grid for visible range data.
     *
     * @param visibleDataGrid      2D grid of remote sensing data in the visible range
     * (assuming it represents top-of-atmosphere radiance or reflectance).
     * @param satelliteZenithGrid  2D grid of satellite zenith angles (in degrees).
     * @param solarZenithGrid      2D grid of solar zenith angles (in degrees).
     * @param satelliteAzimuthGrid 2D grid of satellite azimuth angles (in degrees).
     * @param solarAzimuthGrid     2D grid of solar azimuth angles (in degrees).
     * @param wavelengthVisible    Wavelength of the visible band (in micrometers).
     * @param atmosphericPressure  Atmospheric pressure at the surface (in hPa).
     * @return A 2D grid representing the Rayleigh scattering corrected data.
     * @throws IllegalArgumentException if input grid dimensions are inconsistent.
     */

    public static FieldImpl correctRayleighVisible(FieldImpl visibleField,
                                               FieldImpl satelliteZenithField,
                                               FieldImpl solarZenithField,
                                               FieldImpl satelliteAzimuthField,
                                               FieldImpl solarAzimuthField,
                                               double wavelengthVisible,
                                               double atmosphericPressure)
            throws VisADException, RemoteException {

        double[][] visibleDataGrid = visibleField.getValues();
        double[][] satelliteZenithGrid = satelliteZenithField.getValues();
        double[][] solarZenithGrid = solarZenithField.getValues();
        double[][] satelliteAzimuthGrid = satelliteAzimuthField.getValues();
        double[][] solarAzimuthGrid = solarAzimuthField.getValues();

        int rows = visibleDataGrid.length;
        int cols = visibleDataGrid[0].length;

        float[][] correctedDataGrid = new float[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                float thetaV = (float) Math.toRadians(satelliteZenithGrid[i][j]);
                float thetaS = (float) Math.toRadians(solarZenithGrid[i][j]);
                float phiV = (float) Math.toRadians(satelliteAzimuthGrid[i][j]);
                float phiS = (float) Math.toRadians(solarAzimuthGrid[i][j]);

                // Calculate Rayleigh scattering reflectance for the visible band
                double rhoRayleigh = calculateRayleighReflectance(wavelengthVisible, thetaS, thetaV, phiS, phiV, atmosphericPressure);

                // Apply Rayleigh correction (assuming observed data includes Rayleigh scattering)
                // This is a simplified correction; more sophisticated methods exist.
                correctedDataGrid[i][j] = (float) Math.max(0, visibleDataGrid[i][j] - rhoRayleigh);
            }
        }
        FieldImpl outField = visibleField;
        outField.setSamples(correctedDataGrid);

        logger.info("3141 - made it out");
        return outField;
    }

    /**
     * Calculates the Rayleigh scattering reflectance.
     *
     * This is a simplified implementation based on approximations. More complex
     * models might be needed for higher accuracy.
     *
     * @param wavelength        Wavelength of the band (in micrometers).
     * @param solarZenithRad    Solar zenith angle (in radians).
     * @param satelliteZenithRad Satellite zenith angle (in radians).
     * @param solarAzimuthRad   Solar azimuth angle (in radians).
     * @param satelliteAzimuthRad Satellite azimuth angle (in radians).
     * @param pressureHPa       Atmospheric pressure (in hPa).
     * @return Rayleigh scattering reflectance (unitless).
     */

    private static double calculateRayleighReflectance(
            double wavelength, double solarZenithRad, double satelliteZenithRad,
            double solarAzimuthRad, double satelliteAzimuthRad, double pressureHPa) {

        // Standard pressure (1013.25 hPa)
        double standardPressure = 1013.25;
        double pressureRatio = pressureHPa / standardPressure;

        // Air molecule number density at standard temperature and pressure (cm^-3)
        double n0 = 2.545 * Math.pow(10, 19);

        // Refractive index of air (simplified formula)
        double n_minus_1 = 8342.4 * Math.pow(10, -8) * (1 + 0.007525 * Math.pow(1 / wavelength, 2));
        double n = 1 + n_minus_1;

        // Rayleigh scattering cross-section (cm^2)
        double sigmaRayleigh = (24 * Math.PI * Math.PI * Math.PI * Math.pow(n * n - 1, 2)) /
                (Math.pow(wavelength * 1e-4, 4) * n0 * n0 * ((6 - 7 * Math.sin(solarZenithRad) * Math.sin(solarZenithRad)) /
                        (6 + 3 * Math.sin(solarZenithRad) * Math.sin(solarZenithRad))));

        // Optical thickness due to Rayleigh scattering
        double tauRayleigh = sigmaRayleigh * n0 * pressureRatio;

        // Scattering angle cosine
        double cosScatteringAngle = Math.cos(solarZenithRad) * Math.cos(satelliteZenithRad) +
                Math.sin(solarZenithRad) * Math.sin(satelliteZenithRad) * Math.cos(solarAzimuthRad - satelliteAzimuthRad);

        // Phase function for Rayleigh scattering (unpolarized light)
        double phaseFunction = 0.75 * (1 + cosScatteringAngle * cosScatteringAngle);

        // Simplified Rayleigh reflectance (approximation)
        // This is a very basic approximation and doesn't account for multiple scattering
        double rhoRayleigh = (tauRayleigh * phaseFunction * (Math.exp(-tauRayleigh / Math.cos(solarZenithRad)) +
                Math.exp(-tauRayleigh / Math.cos(satelliteZenithRad)))) /
                (4 * Math.cos(solarZenithRad) * Math.cos(satelliteZenithRad));

        return rhoRayleigh;
    }

    private void applyRayleighCorrection() {
        float[][] newRedTbl = getZeroOutArray(redTable);
        float[][] newGrnTbl = getZeroOutArray(grnTable);
        float[][] newBluTbl = getZeroOutArray(bluTable);

        for (int k = 0; k < redTable[0].length; k++) {
            newRedTbl[0][k] = (float) Math.pow(redTable[0][k], 0.2);
            newGrnTbl[1][k] = (float) Math.pow(grnTable[1][k], 0.2);
            newBluTbl[2][k] = (float) Math.pow(bluTable[2][k], 0.2);
        }
        try {
            displayMaster.setDisplayInactive();
            ((BaseColorControl) redMap.getControl()).setTable(newRedTbl);
            ((BaseColorControl) grnMap.getControl()).setTable(newGrnTbl);
            ((BaseColorControl) bluMap.getControl()).setTable(newBluTbl);
            displayMaster.setDisplayActive();
        } catch (Exception e) {
            LogUtil.logException("setDisplayInactive", e);
        }
    }

    private void updateGamma(double gamma) {
        setGamma(gamma);
        setRedGamma(gamma);
        setGrnGamma(gamma);
        setBluGamma(gamma);
        redGammaTxtFld.setText(Float.toString((float)gamma));
        grnGammaTxtFld.setText(Float.toString((float)gamma));
        bluGammaTxtFld.setText(Float.toString((float)gamma));

        float[][] newRedTbl = getZeroOutArray(redTable);
        float[][] newGrnTbl = getZeroOutArray(grnTable);
        float[][] newBluTbl = getZeroOutArray(bluTable);

        for (int k=0; k<redTable[0].length; k++) {
            newRedTbl[0][k] = (float) Math.pow(redTable[0][k], gamma);
            newGrnTbl[1][k] = (float) Math.pow(grnTable[1][k], gamma);
            newBluTbl[2][k] = (float) Math.pow(bluTable[2][k], gamma);
        }
        try {
            displayMaster.setDisplayInactive();
            ((BaseColorControl)redMap.getControl()).setTable(newRedTbl);
            ((BaseColorControl)grnMap.getControl()).setTable(newGrnTbl);
            ((BaseColorControl)bluMap.getControl()).setTable(newBluTbl);
            displayMaster.setDisplayActive();
        } catch (Exception ex) {
            LogUtil.logException("setDisplayInactive", ex);
        }
    }

    private void updateRedGamma(double gamma) {
        setRedGamma(gamma);

        float[][] newRedTbl = getZeroOutArray(redTable);

        for (int k=0; k<redTable[0].length; k++) {
            newRedTbl[0][k] = (float) Math.pow(redTable[0][k], gamma);
        }

        try {
            displayMaster.setDisplayInactive();
            ((BaseColorControl)redMap.getControl()).setTable(newRedTbl);
            displayMaster.setDisplayActive();
        } catch (Exception ex) {
            LogUtil.logException("setDisplayInactive", ex);
        }
    }

    private void updateGrnGamma(double gamma) {
        setGrnGamma(gamma);

        float[][] newGrnTbl = getZeroOutArray(grnTable);
        for (int k=0; k<grnTable[0].length; k++) {
            newGrnTbl[1][k] = (float) Math.pow(grnTable[1][k], gamma);
        }

        try {
            displayMaster.setDisplayInactive();
            ((BaseColorControl)grnMap.getControl()).setTable(newGrnTbl);
            displayMaster.setDisplayActive();
        } catch (Exception ex) {
            LogUtil.logException("setDisplayInactive", ex);
        }
    }

    private void updateBluGamma(double gamma) {
        setBluGamma(gamma);

        float[][] newBluTbl = getZeroOutArray(bluTable);
        for (int k=0; k<bluTable[0].length; k++) {
            newBluTbl[2][k] = (float) Math.pow(bluTable[2][k], gamma);
        }

        try {
            displayMaster.setDisplayInactive();
            ((BaseColorControl)bluMap.getControl()).setTable(newBluTbl);
            displayMaster.setDisplayActive();
        } catch (Exception ex) {
            LogUtil.logException("setDisplayInactive", ex);
        }
    }

    public float[][] getZeroOutArray(float[][] array) {
        float[][] newArray = new float[array.length][array[0].length];
        for (int i=0; i<newArray.length; i++) {
            for (int j=0; j<newArray[0].length; j++) {
                newArray[i][j] = 0.0f;
            }
        }
        return newArray;
    }

    protected ColorTable getInitialColorTable() {
        return getDisplayConventions().getParamColorTable("image");
    }

    void setAllFields(String txtl1, String txtl2) {
        Double l1 = Double.valueOf(txtl1.trim());
        Double l2 = Double.valueOf(txtl2.trim());
        bluRange[0] = l1;
        bluRange[1] = l2;
        redRange[0] = l1;
        redRange[1] = l2;
        grnRange[0] = l1;
        grnRange[1] = l2;
        updateRedRange(redRange[0], redRange[1]);
        updateBluRange(redRange[0], redRange[1]);
        updateGrnRange(redRange[0], redRange[1]);

        redLowTxtFld.setText(txtl1);
        grnLowTxtFld.setText(txtl1);
        bluLowTxtFld.setText(txtl1);

        redHighTxtFld.setText(txtl2);
        bluHighTxtFld.setText(txtl2);
        grnHighTxtFld.setText(txtl2);
    }

    public Container doMakeContents() {

        JButton rayleighButton = new JButton("Apply Rayleigh Correction");
        rayleighButton.addActionListener(e -> {
            applyRayleighCorrection();
        });

        JButton allGammaButton = new JButton("Apply to All Gamma Fields");
        allGammaButton.addActionListener(e -> {
            String tmp = gammaTxtFld.getText().trim();
            updateGamma(Double.valueOf(tmp));
        });

        gammaTxtFld.addActionListener(e -> {
            String tmp = gammaTxtFld.getText().trim();
            updateGamma(Double.valueOf(tmp));
        });

        // McIDAS Inquiry #3193-3141
        redLowTxtFld.addActionListener(e -> {
            if (matchFieldsCbox.isSelected())
                setAllFields(redLowTxtFld.getText(),redHighTxtFld.getText());

            Double l1 = Double.valueOf(redLowTxtFld.getText().trim());
            Double l2 = Double.valueOf(redHighTxtFld.getText().trim());
            redRange[0] = l1;
            redRange[1] = l2;
            updateRedRange(redRange[0], redRange[1]);

        });

        redHighTxtFld.addActionListener(e -> {
            if (matchFieldsCbox.isSelected())
                setAllFields(redLowTxtFld.getText(),redHighTxtFld.getText());

            Double l1 = Double.valueOf(redLowTxtFld.getText().trim());
            Double l2 = Double.valueOf(redHighTxtFld.getText().trim());
            redRange[0] = l1;
            redRange[1] = l2;
            updateRedRange(redRange[0], redRange[1]);
        });

        redGammaTxtFld.addActionListener(e -> {
            String tmp = redGammaTxtFld.getText().trim();
            updateRedGamma(Double.valueOf(tmp));

            if (matchFieldsCbox.isSelected()) {
                grnGammaTxtFld.setText(tmp);
                bluGammaTxtFld.setText(tmp);
                updateBluGamma(Double.valueOf(tmp));
                updateGrnGamma(Double.valueOf(tmp));
            }
        });

        JButton redReset = new JButton("Reset");
        redReset.addActionListener(e -> {
            updateRedRange(initRedRange[0], initRedRange[1]);
            redRange[0] = initRedRange[0];
            redRange[1] = initRedRange[1];
            redLowTxtFld.setText(Float.toString((float)redRange[0]));
            redHighTxtFld.setText(Float.toString((float)redRange[1]));
            updateRedGamma(1.0);
            redGammaTxtFld.setText("1.0");
        });

        grnLowTxtFld.addActionListener(e -> {
            if (matchFieldsCbox.isSelected())
                setAllFields(grnLowTxtFld.getText(),grnHighTxtFld.getText());

            Double l1 = Double.valueOf(grnLowTxtFld.getText().trim());
            Double l2 = Double.valueOf(grnHighTxtFld.getText().trim());
            grnRange[0] = l1;
            grnRange[1] = l2;
            updateGrnRange(grnRange[0], grnRange[1]);
        });

        grnHighTxtFld.addActionListener(e -> {
            if (matchFieldsCbox.isSelected())
                setAllFields(grnLowTxtFld.getText(),grnHighTxtFld.getText());

            Double l1 = Double.valueOf(grnLowTxtFld.getText().trim());
            Double l2 = Double.valueOf(grnHighTxtFld.getText().trim());
            grnRange[0] = l1;
            grnRange[1] = l2;
            updateGrnRange(grnRange[0], grnRange[1]);
        });

        grnGammaTxtFld.addActionListener(e -> {
            String tmp = grnGammaTxtFld.getText().trim();
            updateGrnGamma(Double.valueOf(tmp));

            if (matchFieldsCbox.isSelected()) {
                redGammaTxtFld.setText(tmp);
                bluGammaTxtFld.setText(tmp);
                updateRedGamma(Double.valueOf(tmp));
                updateGrnGamma(Double.valueOf(tmp));
            }
        });

        JButton grnReset = new JButton("Reset");
        grnReset.addActionListener(e -> {
            updateGrnRange(initGrnRange[0], initGrnRange[1]);
            grnRange[0] = initGrnRange[0];
            grnRange[1] = initGrnRange[1];
            grnLowTxtFld.setText(Float.toString((float)grnRange[0]));
            grnHighTxtFld.setText(Float.toString((float)grnRange[1]));
            updateGrnGamma(1.0);
            grnGammaTxtFld.setText("1.0");
        });

        bluLowTxtFld.addActionListener(e -> {
            if (matchFieldsCbox.isSelected())
                setAllFields(bluLowTxtFld.getText(),bluHighTxtFld.getText());

            Double l1 = Double.valueOf(bluLowTxtFld.getText().trim());
            Double l2 = Double.valueOf(bluHighTxtFld.getText().trim());
            bluRange[0] = l1;
            bluRange[1] = l2;
            updateBluRange(bluRange[0], bluRange[1]);
        });

        bluHighTxtFld.addActionListener(e -> {
            if (matchFieldsCbox.isSelected())
                setAllFields(bluLowTxtFld.getText(),bluHighTxtFld.getText());

            Double l1 = Double.valueOf(bluLowTxtFld.getText().trim());
            Double l2 = Double.valueOf(bluHighTxtFld.getText().trim());
            bluRange[0] = l1;
            bluRange[1] = l2;
            updateBluRange(bluRange[0], bluRange[1]);
        });

        bluGammaTxtFld.addActionListener(e -> {
            String tmp = bluGammaTxtFld.getText().trim();
            updateBluGamma(Double.valueOf(tmp));

            if (matchFieldsCbox.isSelected()) {
                grnGammaTxtFld.setText(tmp);
                redGammaTxtFld.setText(tmp);
                updateGrnGamma(Double.valueOf(tmp));
                updateRedGamma(Double.valueOf(tmp));
            }
        });

        JButton bluReset = new JButton("Reset");
        bluReset.addActionListener(e -> {
            updateBluRange(initBluRange[0], initBluRange[1]);
            bluRange[0] = initBluRange[0];
            bluRange[1] = initBluRange[1];
            bluLowTxtFld.setText(Float.toString((float)bluRange[0]));
            bluHighTxtFld.setText(Float.toString((float)bluRange[1]));
            updateBluGamma(1.0);
            bluGammaTxtFld.setText("1.0");
        });

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            String redLow = redLowTxtFld.getText().trim();
            String redHigh = redHighTxtFld.getText().trim();
            updateRedRange(Double.valueOf(redLow), Double.valueOf(redHigh));
            String grnLow = grnLowTxtFld.getText().trim();
            String grnHigh = grnHighTxtFld.getText().trim();
            updateGrnRange(Double.valueOf(grnLow), Double.valueOf(grnHigh));
            String bluLow = bluLowTxtFld.getText().trim();
            String bluHigh = bluHighTxtFld.getText().trim();
            updateBluRange(Double.valueOf(bluLow), Double.valueOf(bluHigh));

            String tmp1 = redGammaTxtFld.getText().trim();
            updateRedGamma(Double.valueOf(tmp1));
            String tmp2 = grnGammaTxtFld.getText().trim();
            updateGrnGamma(Double.valueOf(tmp2));
            String tmp3 = bluGammaTxtFld.getText().trim();
            updateBluGamma(Double.valueOf(tmp3));
        });

        // McIDAS Inquiry #3193-3141
        matchFieldsCbox = new JCheckBox();
        matchFieldsCbox.setToolTipText("When enabled, changing a setting for one color changes the setting for all colors.");
        JPanel topPanel = new JPanel(new MigLayout());
        topPanel.add(new JLabel("Match fields: "));
        topPanel.add(matchFieldsCbox, "wrap");
        topPanel.add(new JLabel("Red Range: "));
        topPanel.add(redLowTxtFld);
        topPanel.add(redHighTxtFld);
        topPanel.add(new JLabel("Red Gamma: "));
        topPanel.add(redGammaTxtFld);
        topPanel.add(redReset, "wrap");

        topPanel.add(new JLabel("Green Range: "));
        topPanel.add(grnLowTxtFld);
        topPanel.add(grnHighTxtFld);
        topPanel.add(new JLabel("Green Gamma: "));
        topPanel.add(grnGammaTxtFld);
        topPanel.add(grnReset, "wrap");

        topPanel.add(new JLabel("Blue Range: "));
        topPanel.add(bluLowTxtFld);
        topPanel.add(bluHighTxtFld);
        topPanel.add(new JLabel("Blue Gamma: "));
        topPanel.add(bluGammaTxtFld);
        topPanel.add(bluReset, "wrap");

        topPanel.add(Box.createHorizontalStrut(2), "span 5");
        topPanel.add(applyButton, "wrap");

        JPanel bottomPanel = new JPanel(new MigLayout());
        bottomPanel.add(new JLabel("Common Gamma: "));
        bottomPanel.add(gammaTxtFld);
        bottomPanel.add(allGammaButton, "wrap");
        bottomPanel.add(rayleighButton, "wrap");
        bottomPanel.add(new JLabel("Vertical Position: "));
        bottomPanel.add(doMakeZPositionSlider());

        JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        mainPanel.add(topPanel);
        mainPanel.add(bottomPanel);

        return mainPanel;
    }

    private class ColorMapListener implements ScalarMapListener {
        ScalarMap clrMap;

        double[] range = null;
        double[] initRange = null;

        JTextField lowTxtFld;
        JTextField highTxtFld;

        ColorMapListener(ScalarMap clrMap, double[] initRange, double[] range, JTextField lowTxtFld, JTextField highTxtFld) {
            this.clrMap = clrMap;
            this.lowTxtFld = lowTxtFld;
            this.highTxtFld = highTxtFld;
            this.range = range;
            this.initRange = initRange;
        }

        public void controlChanged(ScalarMapControlEvent event) throws RemoteException, VisADException {
        }

        @Override
        public void mapChanged(ScalarMapEvent event) throws RemoteException, VisADException {
            if (event.getId() == ScalarMapEvent.AUTO_SCALE) {
                double[] rng = clrMap.getRange();
                boolean shouldRemove = false;
                //Ghansham: decide whether it is first time. The cleaner way
                if (!Double.isNaN(rng[0]) && !Double.isNaN(rng[1]) && Double.isNaN(initRange[0]) && Double.isNaN(initRange[1])) {
                    shouldRemove = true;
                }
                range[0] = rng[0];
                range[1] = rng[1];
                initRange[0] = rng[0];
                initRange[1] = rng[1];
                lowTxtFld.setText(Float.toString((float)rng[0]));
                highTxtFld.setText(Float.toString((float)rng[1]));
                //Ghansham:If its first time remove the scalarmaplistener and setRange manually to disable autscaling of the scalarmap
                if (shouldRemove) {
                    clrMap.removeScalarMapListener(this);
                    //-Lock out auto-scaling
                    clrMap.disableAutoScale();
                }
            } else if (event.getId() == ScalarMapEvent.MANUAL) {
                double[] rng = clrMap.getRange();
                range[0] = rng[0];
                range[1] = rng[1];
            }
        }
    }
}
