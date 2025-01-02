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
import visad.*;

import visad.util.Util;
import visad.util.HersheyFont;

import java.rmi.RemoteException;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Component;
import java.awt.Color;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import visad.georef.LatLonTuple;

public class SpectraDisplay extends HydraDisplay {

    DataReference probeLocationRef;
    DataReference reflRef;
    DataReference emisRef;
    DataReference bandNameRef;
    RealType rangeType = null;

    LocalDisplay display = null;

    ScalarMap ymap = null;

    ArrayList<ScalarMap> yAxisMaps = new ArrayList<ScalarMap>();

    JFrame frame;

    static RealType rType = RealType.getRealType("REFL");
    static RealType eType = RealType.getRealType("EMIS");

    ArrayList<MultiSpectralData> reflMSD;
    ArrayList<MultiSpectralData> emisMSD;
    int numBands = 0;
    int numReflBands = 0;
    int numEmisBands = 0;
    float[] reflBandVals = new float[numReflBands];
    float[] reflChannels = new float[numReflBands];
    String[] reflBandNames = new String[numReflBands];

    float[] emisBandVals = new float[numEmisBands];
    float[] emisChannels = new float[numEmisBands];
    String[] emisBandNames = new String[numEmisBands];

    FlatField[] reflSpect;
    FlatField[] emisSpect;

    HydraRGBDisplayable imageDsp;
    MultiSpectralData[] multiSpectData;
    String name;

    CoordinateSystem swathCS;

    HashMap<HydraRGBDisplayable, Spectrum> internalMap = new HashMap();

    public SpectraDisplay(DataReference probeLocationRef, MultiSpectralData[] msds, Point screenLoc, HydraRGBDisplayable imageDsp, String name) throws VisADException, RemoteException {
        this.probeLocationRef = probeLocationRef;
        this.imageDsp = imageDsp;
        this.multiSpectData = msds;
        this.name = name;


        reflRef = new DataReferenceImpl("spectrumRef_" + Hydra.getUniqueID());
        emisRef = new DataReferenceImpl("spectrumRef_" + Hydra.getUniqueID());
        bandNameRef = new DataReferenceImpl("bandName_" + Hydra.getUniqueID());

        try {
            Spectrum spec = new Spectrum(probeLocationRef, reflRef, emisRef, bandNameRef, msds, imageDsp, name);
            internalMap.put(imageDsp, spec);
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        FieldImpl data = (FieldImpl) reflRef.getData();
        FunctionType fncType = (FunctionType) data.getType();

        RealType domainType = fncType.getDomain().getRealComponents()[0];

        display = new visad.java3d.DisplayImplJ3D("2D disp", new visad.java3d.TwoDDisplayRendererJ3D());
        ((DisplayImpl) display).disableAction();
        display.getDisplayRenderer().setBackgroundColor(new Color(0.92f, 0.92f, 0.92f));
        display.getDisplayRenderer().setForegroundColor(Color.black);


        ProjectionControl pCntrl = display.getProjectionControl();
        double[] proj = pCntrl.getMatrix();
        proj[0] = 0.286;
        proj[5] = 0.286;
        proj[10] = 0.286;
        pCntrl.setMatrix(proj);

        ScalarMap xmap = new ScalarMap(domainType, Display.XAxis);


        ScalarMap ymapR = new ScalarMap(rType, Display.YAxis);
        yAxisMaps.add(ymapR);
        ScalarMap ymapE = new ScalarMap(eType, Display.YAxis);
        yAxisMaps.add(ymapE);

        ScalarMap txtMap = new ScalarMap(TextType.Generic, Display.Text);

        pCntrl = display.getProjectionControl();
        pCntrl.setAspectCartesian(new double[]{2.80, 0.75, 1.0});
        ((DisplayImpl) display).setAlwaysAutoScale(true);
        display.addMap(xmap);
        display.addMap(ymapR);
        display.addMap(ymapE);
        display.addMap(txtMap);

        display.getGraphicsModeControl().setScaleEnable(true);

        HersheyFont font = new HersheyFont("rowmant");

        AxisScale xAxis = xmap.getAxisScale();
        xAxis.setFont(font);
        xAxis.setColor(Color.black);
        xAxis.setSnapToBox(true);
        xAxis.setLabelSize(Hydra.getFontSize());
        xmap.setScaleEnable(false);

        AxisScale yAxis = ymapR.getAxisScale();
        yAxis.setFont(font);
        yAxis.setColor(Color.black);
        yAxis.setSnapToBox(true);
        yAxis.setLabelSize(Hydra.getFontSize());
        ymapR.setScaleEnable(true);
        ymapR.setRange(0, 1.2);

        yAxis = ymapE.getAxisScale();
        yAxis.setFont(font);
        yAxis.setColor(Color.black);
        yAxis.setSnapToBox(true);
        yAxis.setLabelSize(Hydra.getFontSize());
        yAxis.setSide(AxisScale.SECONDARY);
        ymapE.setScaleEnable(true);
        ymapE.setRange(160, 340);

        TextControl txtCntrl = (TextControl) txtMap.getControl();
        txtCntrl.setJustification(TextControl.Justification.CENTER);
        txtCntrl.setFont(font);
        txtCntrl.setSize(Hydra.getTextSizeFactor() * 0.94);

        display.getGraphicsModeControl().setPointMode(true);


        ConstantMap lineWidth = new ConstantMap(6.4, Display.PointSize);
        ConstantMap[] constantMaps = new ConstantMap[]{lineWidth};

        Color color = Color.black;
        if (color != null) {
            color = getGraphColor(color);
            ConstantMap[] clrs = Util.getColorMaps(color);
            constantMaps = Hydra.makeConstantMapArray(clrs, lineWidth);
            display.addReference(reflRef, constantMaps);
            clrs = Util.getColorMaps(color);
            constantMaps = Hydra.makeConstantMapArray(clrs, new ConstantMap(6.0, Display.PointSize));
            display.addReference(emisRef, constantMaps);
            clrs = Util.getColorMaps(color);
            constantMaps = Hydra.makeConstantMapArray(clrs, new ConstantMap(-0.92, Display.YAxis));
            display.addReference(bandNameRef, constantMaps);
        } else {
            display.addReference(reflRef);
            display.addReference(emisRef);
        }
        ((DisplayImpl) display).enableAction();


        frame = Hydra.createAndShowFrameFromEDT("Spectral Values", doMakeComponent(), new Dimension(400, 160), screenLoc);
        frame.toFront();
        frame.addWindowListener(this);
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
        internalMap.clear();
    }

    public void update(HydraRGBDisplayable imageDsp) {
        Iterator iter = internalMap.keySet().iterator();
        while (iter.hasNext()) {
            (internalMap.get(iter.next())).setActive(false);
        }
        internalMap.get(imageDsp).update();
    }

    public void update(MultiSpectralData[] msds, HydraRGBDisplayable imageDsp, String name) {
        Iterator iter = internalMap.keySet().iterator();
        while (iter.hasNext()) {
            (internalMap.get(iter.next())).setActive(false);
        }
        if (internalMap.containsKey(imageDsp)) {
            internalMap.get(imageDsp).update();
        } else {
            try {
                Spectrum spec = new Spectrum(probeLocationRef, reflRef, emisRef, bandNameRef, msds, imageDsp, name);
                internalMap.put(imageDsp, spec);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }
}

class Spectrum {
    static RealType rType = RealType.getRealType("REFL");
    static RealType eType = RealType.getRealType("EMIS");

    ArrayList<MultiSpectralData> reflMSD;
    ArrayList<MultiSpectralData> emisMSD;
    int numBands = 0;
    int numReflBands = 0;
    int numEmisBands = 0;
    float[] reflBandVals = new float[numReflBands];
    float[] reflChannels = new float[numReflBands];
    String[] reflBandNames = new String[numReflBands];

    float[] emisBandVals = new float[numEmisBands];
    float[] emisChannels = new float[numEmisBands];
    String[] emisBandNames = new String[numEmisBands];

    FlatField[] reflSpect;
    FlatField[] emisSpect;

    HydraRGBDisplayable imageDsp;
    MultiSpectralData[] multiSpectData;
    String name;
    int msdIdx = 0;

    CoordinateSystem swathCS;

    DataReference probeLocationRef;
    DataReference emisRef;
    DataReference reflRef;
    DataReference bandNameRef;

    boolean active = true;
    CellImpl cell;

    public Spectrum(DataReference probeLocationRef, DataReference reflRef, DataReference emisRefl, DataReference bandNameRef,
                    MultiSpectralData[] msds, HydraRGBDisplayable imageDsp, String name) throws Exception {
        this.multiSpectData = msds;
        this.probeLocationRef = probeLocationRef;
        this.reflRef = reflRef;
        this.emisRef = emisRefl;
        this.bandNameRef = bandNameRef;
        this.name = name.substring(2); // Fixme: need a better way to keep dataSourceID and name separate

        setup(msds, imageDsp);

        cell = new CellImpl() {
            public void doAction() throws VisADException, RemoteException {
                if (active) {
                    updateSpectra();
                }
            }
        };
        cell.doAction();
        cell.addReference(probeLocationRef);
    }

    public void update() {
        active = true;
        try {
            cell.doAction();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    private void setup(MultiSpectralData[] msds, HydraRGBDisplayable imageDsp) throws Exception {
        reflMSD = new ArrayList();
        emisMSD = new ArrayList();
        int num = msds.length;
        for (int k = 0; k < num; k++) {
            MultiSpectralData msd = msds[k];
            if (msds[k].getParameter().equals("Reflectance")) reflMSD.add(msd);
            if (msds[k].getParameter().equals("BrightnessTemp")) emisMSD.add(msd);
            if (msds[k].hasBandName(name)) {
                msdIdx = k;
            }
        }
        reflSpect = new FlatField[reflMSD.size()];
        emisSpect = new FlatField[emisMSD.size()];

        numBands = 0;
        numReflBands = 0;
        numEmisBands = 0;
        for (int k = 0; k < reflSpect.length; k++) {
            numReflBands += reflMSD.get(k).getNumChannels();
        }
        numBands += numReflBands;
        for (int k = 0; k < emisSpect.length; k++) {
            numEmisBands += emisMSD.get(k).getNumChannels();
        }
        numBands += numEmisBands;

        reflBandVals = new float[numReflBands];
        reflChannels = new float[numReflBands];
        reflBandNames = new String[numReflBands];

        emisBandVals = new float[numEmisBands];
        emisChannels = new float[numEmisBands];
        emisBandNames = new String[numEmisBands];

        int pos = 0;
        for (int k = 0; k < reflMSD.size(); k++) {
            float[] vals = reflMSD.get(k).getSpectralDomain().getSamples()[0];
            int len = vals.length;
            System.arraycopy(vals, 0, reflChannels, pos, len);
            for (int i = 0; i < len; i++) {
                String name = reflMSD.get(k).getBandNameFromWaveNumber(vals[i]);
                name = name.startsWith("B") ? name.split("B")[1] : name;
                name = name.startsWith("C") ? name.split("C")[1] : name;
                reflBandNames[pos + i] = name;
            }
            pos += len;
        }
        for (int k = 0; k < numReflBands; k++) {
            String bname = reflBandNames[k];
            switch (bname) {
                case "13L":
                    bname = "13";
                    break;
                case "13H":
                    bname = " ";
                    break;
                case "14L":
                    bname = "14";
                    break;
                case "14H":
                    bname = " ";
                    break;
                default:
                    break;
            }
            reflBandNames[k] = bname;
        }

        pos = 0;
        for (int k = 0; k < emisMSD.size(); k++) {
            float[] vals = emisMSD.get(k).getSpectralDomain().getSamples()[0];
            int len = vals.length;
            System.arraycopy(vals, 0, emisChannels, pos, len);
            for (int i = 0; i < vals.length; i++) {
                String name = emisMSD.get(k).getBandNameFromWaveNumber(vals[i]);
                name = name.startsWith("B") ? name.split("B")[1] : name;
                name = name.startsWith("C") ? name.split("C")[1] : name;
                emisBandNames[pos + i] = name;
            }
            pos += len;
        }
    }

    public Tuple makeProfile(RealTuple tup) throws Exception {
        int[] coords = multiSpectData[msdIdx].getSwathCoordinates(tup, multiSpectData[msdIdx].getCoordinateSystem());
        if (coords == null) {
            return null;
        }
        int[] tCoords = new int[]{coords[0], coords[1]};
        String sensorName = multiSpectData[msdIdx].getSensorName();

        boolean anyMissing = false;

        for (int k = 0; k < reflSpect.length; k++) {
            tCoords[0] = coords[0];
            tCoords[1] = coords[1];
            if (k != msdIdx) {
                tCoords = reflMSD.get(k).transformSwathCoords(coords, sensorName);
            }
            reflSpect[k] = reflMSD.get(k).getSpectrum(tCoords);
        }
        int numRefl = 0;
        for (int k = 0; k < reflSpect.length; k++) {
            if (reflSpect[k] != null) {
                int len = reflSpect[k].getLength();
                float[] vals = reflSpect[k].getFloats()[0];
                System.arraycopy(vals, 0, reflBandVals, numRefl, len);
                numRefl += len;
            } else {
                anyMissing = true;
            }
        }

        for (int k = 0; k < emisSpect.length; k++) {
            tCoords[0] = coords[0];
            tCoords[1] = coords[1];
            if (k != msdIdx) {
                tCoords = emisMSD.get(k).transformSwathCoords(coords, sensorName);
            }
            emisSpect[k] = emisMSD.get(k).getSpectrum(tCoords);
        }
        int numEmis = 0;
        for (int k = 0; k < emisSpect.length; k++) {
            if (emisSpect[k] != null) {
                int len = emisSpect[k].getLength();
                float[] vals = emisSpect[k].getFloats()[0];
                System.arraycopy(vals, 0, emisBandVals, numEmis, len);
                numEmis += len;
            } else {
                anyMissing = true;
            }
        }


        Integer1DSet dset;
        if ((numRefl + numEmis) == 0) {
            dset = new Integer1DSet(1);
        } else {
            dset = new Integer1DSet(numRefl + numEmis);
        }

        FieldImpl reflFld = new FieldImpl(new FunctionType(RealType.Generic, rType), dset);
        if (!anyMissing) {
            for (int k = 0; k < numReflBands; k++) {
                Real rngVal = new Real(rType, reflBandVals[k]);
                reflFld.setSample(k, rngVal);
            }
            for (int k = 0; k < numEmisBands; k++) {
                Real rngVal = new Real(rType, Float.NaN);
                reflFld.setSample(numReflBands + k, rngVal);
            }
        }

        FieldImpl bandNameFld = new FieldImpl(new FunctionType(RealType.Generic, TextType.Generic), dset);
        if (!anyMissing) {
            for (int k = 0; k < numReflBands; k++) {
                String bname = reflBandNames[k];
                Text rngVal = new Text(bname);
                bandNameFld.setSample(k, rngVal);
            }
            for (int k = 0; k < numEmisBands; k++) {
                Text rngVal = new Text(emisBandNames[k]);
                bandNameFld.setSample(numReflBands + k, rngVal);
            }
        }

        FieldImpl emisFld = new FieldImpl(new FunctionType(RealType.Generic, eType), dset);
        if (!anyMissing) {
            for (int k = 0; k < numReflBands; k++) {
                Real rngVal = new Real(eType, Float.NaN);
                emisFld.setSample(k, rngVal);
            }
            for (int k = 0; k < numEmisBands; k++) {
                Real rngVal = new Real(eType, emisBandVals[k]);
                emisFld.setSample(numReflBands + k, rngVal);
            }
        }

        return new Tuple(new Data[]{reflFld, emisFld, bandNameFld});
    }

    private void updateSpectra() throws VisADException, RemoteException {
        LatLonTuple tup = (LatLonTuple) probeLocationRef.getData();
        try {
            Tuple specTup = makeProfile(tup);
            if (specTup == null) {
                return;
            }
            reflRef.setData(specTup.getComponent(0));
            emisRef.setData(specTup.getComponent(1));
            bandNameRef.setData(specTup.getComponent(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}