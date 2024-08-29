/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
 *
 * All Rights Reserved
 *
 * McIDAS-V is built on Unidata's IDV, as well as SSEC's VisAD and HYDRA
 * projects. Parts of McIDAS-V source code are based on IDV, VisAD, and
 * HYDRA source code.
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

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.XYDisplay;
import ucar.visad.display.RGBDisplayable;

import visad.georef.MapProjection;

import edu.wisc.ssec.adapter.ReprojectSwath;

import visad.*;

import java.rmi.RemoteException;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.Color;

import javax.swing.JFrame;


public class Scatter {

    public static ImageDisplay imageX = null;
    public static ImageDisplay imageY = null;
    public static FlatField X_field = null;
    public static FlatField Y_field = null;
    public static Linear2DSet domSetX = null;
    public static Linear2DSet domSetY = null;
    public static MyScatterDisplay display = null;
    public static FlatField lastImage = null;
    public static MapProjection lastProj = null;
    public static boolean isXgeos = false;
    public static boolean isYgeos = false;
    public static boolean anyGEOS = false;
    public static boolean bothGEOS = false;

    public Scatter() {
    }

    public static void makeScatterDisplay(ImageDisplay image) {
        if (imageX == null && imageY == null) {
            imageX = image;
        } else if (imageY == null) {
            imageY = image;
            try {
                X_field = (FlatField) imageX.getImageDisplayable().getData();
                Y_field = (FlatField) imageY.getImageDisplayable().getData();

                CoordinateSystem csX = X_field.getDomainCoordinateSystem();
                isXgeos = csX instanceof GEOSProjection;

                CoordinateSystem csY = Y_field.getDomainCoordinateSystem();
                isYgeos = csY instanceof GEOSProjection;

                anyGEOS = isXgeos | isYgeos;
                bothGEOS = isXgeos & isYgeos;

                domSetX = (Linear2DSet) X_field.getDomainSet();
                domSetY = (Linear2DSet) Y_field.getDomainSet();

                // resample if sets not equal
                if (!(domSetX.equals(domSetY))) {
                    int mode = Hydra.getReprojectMode();

                    int visadMode = Data.NEAREST_NEIGHBOR;
                    if (mode == 0) {
                        visadMode = Data.NEAREST_NEIGHBOR;
                    } else if (mode == 2) {
                        visadMode = Data.WEIGHTED_AVERAGE;
                    }

                    FlatField swathImageY = Hydra.displayableToImage.get((HydraRGBDisplayable) imageY.getImageDisplayable());

                    if (swathImageY == null) { // may not have original swath, eg. came from a computation
                        Y_field = resample(X_field, Y_field);
                    } else { // New logic, first get original swath data
                        lastImage = Y_field;
                        lastProj = imageY.getMapProjection();

                        if (bothGEOS) {
                            FlatField radDomX = GOESgridUtil.makeGEOSRadiansDomainField(X_field, (GEOSProjection) csX);
                            FlatField radDomY = GOESgridUtil.makeGEOSRadiansDomainField(Y_field, (GEOSProjection) csY);
                            FlatField resamp = GOESgridUtil.goesResample(radDomY, (Linear2DSet) radDomX.getDomainSet(), visadMode);
                            Y_field = new FlatField((FunctionType) Y_field.getType(), X_field.getDomainSet());
                            Y_field.setSamples(resamp.getFloats(false), false);
                        } else {
                            Linear2DSet grd = domSetX;
                            MapProjection mp = imageX.getMapProjection();
                            Y_field = ReprojectSwath.swathToGrid(grd, swathImageY, mode);
                        }

                        imageY.getImageDisplayable().setData(Y_field);
                        imageY.getReadoutProbe().updateData(Y_field);
                        imageY.setMapProjection(imageX.getMapProjection());
                    }
                }

                display = new MyScatterDisplay(X_field, Y_field, imageX, imageY);
                //TODO: use if don't have/want interactive point/image selection
                //ScatterDisplay display = new ScatterDisplay(X_field, imageX.name, Y_field, imageY.name);
            } catch (VisADException e) {
                System.out.println(e);
            } catch (RemoteException e) {
                System.out.println(e);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            imageX = image;
            imageY = null;
        }
    }

    public static void clear() {
        try {
            if (lastImage != null) {
                imageY.getImageDisplayable().setData(lastImage);
                imageY.setMapProjection(lastProj);
                imageY.getReadoutProbe().updateData(lastImage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        imageX = null;
        imageY = null;
        X_field = null;
        Y_field = null;
        domSetX = null;
        domSetY = null;
        display = null;
        lastImage = null;
        lastProj = null;
    }

    public static FlatField resample(FlatField X_field, FlatField Y_field) throws VisADException, RemoteException {

        RealTupleType X_domainRef = null;
        RealTupleType Y_domainRef = null;
        float[][] coords = null;
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

        Gridded2DSet domSetY = (Gridded2DSet) Y_field.getDomainSet();

        if (X_domainRef != null && Y_domainRef != null) {
            Xsamples = X_cs.toReference(Xsamples);
            Xsamples = Y_cs.fromReference(Xsamples);
        } else if (X_domainRef == null && Y_domainRef != null) {
            Xsamples = Y_cs.fromReference(Xsamples);
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
        } else if (X_domainRef == null && Y_domainRef == null) {
            Gridded2DSet domSet = (Gridded2DSet) Y_field.getDomainSet();
        }

        int length = Xsamples[0].length;
        float[][] new_values = new float[1][length];

        // Force weighted average for now.

        if (false) { // nearest neighbor
            int[] indexes = domSetY.valueToIndex(Xsamples);
            for (int k = 0; k < indexes.length; k++) {
                new_values[0][k] = Float.NaN;
                if (indexes[k] >= 0) {
                    new_values[0][k] = Yvalues[0][indexes[k]];
                }
            }
        } else { // weighted average
            int[][] indices = new int[length][];
            float[][] coefs = new float[length][];
            domSetY.valueToInterp(Xsamples, indices, coefs);
            for (int i = 0; i < length; i++) {
                float v = Float.NaN;
                int len = indices[i] == null ? 0 : indices[i].length;
                if (len > 0) {
                    v = Yvalues[0][indices[i][0]] * coefs[i][0];
                    for (int k = 1; k < len; k++) {
                        v += Yvalues[0][indices[i][k]] * coefs[i][k];
                    }
                    new_values[0][i] = v;
                } else { // values outside grid
                    new_values[0][i] = Float.NaN;
                }
            }
        }

        FunctionType ftype =
                new FunctionType(((FunctionType) X_field.getType()).getDomain(),
                        ((FunctionType) Y_field.getType()).getRange());
        Y_field = new FlatField(ftype, X_field.getDomainSet());
        Y_field.setSamples(new_values);

        return Y_field;
    }
}

class ScatterDisplay extends HydraDisplay {
    Color[] selectorColors = new Color[]{Color.magenta, Color.green, Color.blue};
    int n_selectors = selectorColors.length;
    float[][] maskColorPalette = new float[][]{{0.8f, 0f, 0f}, {0f, 0.8f, 0f}, {0.8f, 0f, 0.8f}};
    float[][] markColorPalette = new float[][]{{1f, 0.8f, 0f, 0f}, {1f, 0f, 0.8f, 0f}, {1f, 0.8f, 0f, 0.8f}};

    DisplayMaster master = null;

    public ScatterDisplay(FlatField X_field, String X_name, FlatField Y_field, String Y_name) throws VisADException, RemoteException {

        ScatterDisplayable scatterDsp = new ScatterDisplayable("scatter",
                RealType.getRealType("mask"), markColorPalette, false);
        float[] valsX = X_field.getFloats(false)[0];
        float[] valsY = Y_field.getFloats(false)[0];

        // NaN test
        for (int k = 0; k < valsX.length; k++) {
            if (Float.isNaN(valsX[k]) || Float.isNaN(valsY[k])) {
                valsX[k] = Float.NaN;
                valsY[k] = Float.NaN;
            }
        }

        Integer1DSet set = new Integer1DSet(valsX.length);
        FlatField scatter = new FlatField(
                new FunctionType(RealType.Generic,
                        new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), set);
        float[] mask = new float[valsX.length];
        for (int k = 0; k < mask.length; k++) mask[k] = 0;
        float[][] scatterFieldRange = new float[][]{valsX, valsY, mask};
        scatter.setSamples(scatterFieldRange);
        scatterDsp.setPointSize(2f);
        scatterDsp.setRangeForColor(0, n_selectors);

        float[] xRange = Hydra.minmax(valsX);
        float[] yRange = Hydra.minmax(valsY);

        scatterDsp.setData(scatter);

        /**
         scatterMarkDsp = new ScatterDisplayable("scatter",
         RealType.getRealType("mask"), markColorPalette, false);
         set = new Integer1DSet(2);
         scatter = new FlatField(
         new FunctionType(RealType.Generic,
         new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), set);
         scatterMarkDsp.setData(scatter);
         scatterMarkDsp.setPointSize(2f);
         scatterMarkDsp.setRangeForColor(0,n_selectors);
         */

        master = new XYDisplay("Scatter", RealType.XAxis, RealType.YAxis);
        master.setMouseFunctions(Hydra.getMouseFunctionMap());
        master.draw();
        ((XYDisplay) master).showAxisScales(true);
        AxisScale scaleX = ((XYDisplay) master).getXAxisScale();
        scaleX.setTitle(X_name);
        AxisScale scaleY = ((XYDisplay) master).getYAxisScale();
        scaleY.setTitle(Y_name);

        ((XYDisplay) master).setXRange((double) xRange[0], (double) xRange[1]);
        ((XYDisplay) master).setYRange((double) yRange[0], (double) yRange[1]);
        master.addDisplayable(scatterDsp);
        //master.addDisplayable(scatterMarkDsp);

        JFrame frame = Hydra.createAndShowFrame("Scatter", doMakeComponent(), new Dimension(280, 280));
        frame.addWindowListener(this);

    }

    public Component doMakeComponent() {
        return master.getDisplayComponent();
    }

}

class ScatterDisplayable extends RGBDisplayable {
    ScatterDisplayable(String name, RealType rgbRealType, float[][] colorPalette, boolean alphaflag)
            throws VisADException, RemoteException {
        super(name, rgbRealType, colorPalette, alphaflag);
    }
}
