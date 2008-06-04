/*
 * $Id$
 * 
 * Copyright 2007-2008 Space Science and Engineering Center (SSEC) University
 * of Wisconsin - Madison, 1225 W. Dayton Street, Madison, WI 53706, USA
 * 
 * http://www.ssec.wisc.edu/mcidas
 * 
 * This file is part of McIDAS-V.
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser Public License along with
 * this program. If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.control;

import java.awt.Color;
import java.rmi.RemoteException;

import ucar.unidata.idv.control.LineProbeControl;
import visad.ConstantMap;
import visad.DataReference;
import visad.Display;
import visad.RealTuple;
import visad.VisADException;
import visad.georef.EarthLocationTuple;

public class HydraImageProbe extends LineProbeControl {

    private DataReference posRef = null;

    private DataReference specRef = null;

    private Color oldColor = null;

    private RealTuple oldPos = null;

    private MultiSpectralControl control = null;

    public HydraImageProbe() {
        super();
    }

    // this is triggered for both position AND color changes
    @Override protected void probePositionChanged(final RealTuple position) {
        reposition(position);
        setSpectrumLineColor(getColor());
    }

    public void loadProfile(final RealTuple position) throws VisADException,
        RemoteException {
        System.out.println("HydraImageProbe.loadProfile");
    }

    public void reposition(final RealTuple pos) {
        if ((posRef == null) || (oldPos != null && oldPos.equals(pos)))
            return;

        double[] vals = pos.getValues();

        try {
            EarthLocationTuple elt =
                (EarthLocationTuple)boxToEarth(new double[] { vals[0],
                                                              vals[1], 1.0 });
            if (posRef != null)
                posRef.setData(elt.getLatLonPoint());

            oldPos = pos;
        } catch (Exception e) {
            logException("HydraImageProbe.reposition", e);
        }
    }

    // TODO: better name?
    public void setSpectrumLineColor(final Color color) {
        if ((specRef == null) || (oldColor != null && oldColor.equals(color)))
            return;

        try {
            oldColor = color;

            if (control != null && specRef != null)
                control.updateDisplay();

        } catch (Exception e) {
            logException("HydraImageProbe.setSpectrumLineColor", e);
        }
    }

    public ConstantMap[] getColorMap() {
        ConstantMap[] map = null;
        try {
            map = makeColorMap(getColor());
        } catch (Exception e) {
            logException("HydraImageProbe.getColorMap", e);
        }
        return map;
    }

    public static ConstantMap[] makeColorMap(final Color c)
        throws VisADException, RemoteException {
        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float b = c.getBlue() / 255f;
        float a = c.getAlpha() / 255f;
        return new ConstantMap[] { new ConstantMap(r, Display.Red),
                                   new ConstantMap(g, Display.Green),
                                   new ConstantMap(b, Display.Blue),
                                   new ConstantMap(a, Display.Alpha) };
    }

    public void setSpectrumRef(final DataReference spectrumRef) {
        specRef = spectrumRef;
    }

    public void setPositionRef(final DataReference positionRef) {
        posRef = positionRef;
    }

    public void setControl(final MultiSpectralControl control) {
        this.control = control;
    }
}
