/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
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
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.control;

import java.awt.Color;
import java.rmi.RemoteException;
import java.text.DecimalFormat;

import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;

import ucar.unidata.idv.control.LineProbeControl;
import ucar.unidata.util.LogUtil;
import ucar.visad.display.TextDisplayable;
import visad.CellImpl;
import visad.Data;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.FlatField;
import visad.MathType;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.Text;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.VisADException;
import visad.georef.EarthLocationTuple;

public class HydraImageProbe extends LineProbeControl {

    private static final TupleType TUPTYPE = makeTupleType();

    private DataReference positionRef = null;

    private DataReference spectrumRef = null;

    private Color currentColor = Color.MAGENTA;

    private RealTuple currentPosition = null;

    private Tuple locationValue = null;

    private MultiSpectralDisplay display = null;

    private TextDisplayable valueDisplay = null;

    public HydraImageProbe() throws VisADException, RemoteException {
        super();

        currentPosition = new RealTuple(RealTupleType.Generic2D);

        spectrumRef = new DataReferenceImpl(hashCode() + "_spectrumRef");
        positionRef = new DataReferenceImpl(hashCode() + "_positionRef");

        valueDisplay = createValueDisplayer(currentColor);

        new Updater();
    }

    public void setDisplay(final MultiSpectralDisplay disp) throws VisADException, RemoteException {
        display = disp;
        display.addRef(spectrumRef, currentColor);
    }

    // triggered for both position and color changes.
    protected void probePositionChanged(final RealTuple newPos) {
        if (display == null)
            return;

        if (!currentPosition.equals(newPos)) {
            updatePosition(newPos);
            updateLocationValue();
            updateSpectrum();
            currentPosition = newPos;
        } 

        Color tmp = getColor();
        if (!currentColor.equals(tmp)) {
            updateSpectrumColor(tmp);
            currentColor = tmp;
        }
    }

    public RealTuple getCurrentPosition() {
        return currentPosition;
    }
    
    public Color getCurrentColor() {
        return currentColor;
    }
    
    public TextDisplayable getValueDisplay() {
        return valueDisplay;
    }

    public DataReference getSpectrumRef() {
        return spectrumRef;
    }

    public DataReference getPositionRef() {
        return positionRef;
    }

    public Tuple getLocationValue() {
        return locationValue;
    }

    private void updateLocationValue() {
        Tuple tup = null;

        try {
            RealTuple location = (RealTuple)positionRef.getData();
            if (location == null)
                return;

            FlatField image = (FlatField)display.getImageDisplay().getData();
            if (image == null)
                return;

            double[] vals = location.getValues();
            if (vals[1] < -180)
                vals[1] += 360f;

            if (vals[1] > 180)
                vals[1] -= 360f;

            RealTuple lonLat = new RealTuple(RealTupleType.SpatialEarth2DTuple, new double[] { vals[1], vals[0] });
            Real val = (Real)image.evaluate(lonLat, Data.NEAREST_NEIGHBOR, Data.NO_ERRORS);
            float fval = (float)val.getValue();
            tup = new Tuple(TUPTYPE, new Data[] { lonLat, new Text(TextType.Generic, Float.toString(fval)) });
            valueDisplay.setData(tup);
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.updateLocationValue", e);
        }

        if (tup != null)
            locationValue = tup;
    }

    public void forceUpdateSpectrum() {
        updateLocationValue();
        updateSpectrum();
        updatePosition(currentPosition);
    }

    private void updateSpectrum() {
        try {
            RealTuple tmp = (RealTuple)positionRef.getData();
            FlatField spectrum = display.getMultiSpectralData().getSpectrum(tmp);
            spectrumRef.setData(spectrum);
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.updateSpectrum", e);
        }
    }

    protected void updatePosition(final RealTuple position) {
        double[] vals = position.getValues();
        try {
            EarthLocationTuple elt = (EarthLocationTuple)boxToEarth(
                new double[] { vals[0], vals[1], 1.0 });

            positionRef.setData(elt.getLatLonPoint());
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.updatePosition", e);
        }
    }

    private void updateSpectrumColor(final Color color) {
        try {
            display.updateRef(spectrumRef, color);
            valueDisplay.setColor(color);
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.updateColor", e);
        }
    }

    private static TextDisplayable createValueDisplayer(final Color color) 
        throws VisADException, RemoteException 
    {
        DecimalFormat fmt = new DecimalFormat();
        fmt.setMaximumIntegerDigits(3);
        fmt.setMaximumFractionDigits(1);

        TextDisplayable td = new TextDisplayable(TextType.Generic);
        td.setLineWidth(2f);
        td.setColor(color);
        td.setNumberFormat(fmt);

        return td;
    }

    private static TupleType makeTupleType() {
        TupleType t = null;
        try {
            t = new TupleType(new MathType[] {RealTupleType.SpatialEarth2DTuple, 
                                              TextType.Generic});
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.makeTupleType", e);
        }
        return t;
    }

    private class Updater extends CellImpl {
        public Updater() throws VisADException, RemoteException {
            this.addReference(positionRef);
        }
        
        public void doAction() {

        }
    }
}
