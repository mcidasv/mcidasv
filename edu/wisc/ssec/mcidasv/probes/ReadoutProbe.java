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
package edu.wisc.ssec.mcidasv.probes;

import static edu.wisc.ssec.mcidasv.util.Contract.*;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.concurrent.CopyOnWriteArrayList;

import ucar.unidata.collab.SharableImpl;
import ucar.unidata.util.LogUtil;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.visad.ShapeUtility;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineProbe;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.TextDisplayable;

import visad.Data;
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

public class ReadoutProbe extends SharableImpl implements PropertyChangeListener {

    public static final String SHARE_PROFILE = "ReadoutProbeDeux.SHARE_PROFILE";

    public static final String SHARE_POSITION = "ReadoutProbeDeux.SHARE_POSITION";

    private static final Color DEFAULT_COLOR = Color.MAGENTA;

    private static final TupleType TUPTYPE = makeTupleType();

    private final CopyOnWriteArrayList<ProbeListener> listeners = 
        new CopyOnWriteArrayList<>();

    /** Displays the value of the data at the current position. */
    private final TextDisplayable valueDisplay = createValueDisplay(DEFAULT_COLOR);

    private final LineProbe probe = new LineProbe(getInitialLinePosition());

    private final DisplayMaster master;

    private Color currentColor = DEFAULT_COLOR;

    private String currentValue = "NaN";

    private double currentLatitude = Double.NaN;
    private double currentLongitude = Double.NaN;

    private float pointSize = 1.0f;

    private FlatField field;

    private static final DecimalFormat numFmt = new DecimalFormat();

    private RealTuple prevPos = null;

    public ReadoutProbe(final DisplayMaster master, final FlatField field, final Color color, final boolean visible) throws VisADException, RemoteException {
        super();
        notNull(master, "DisplayMaster can't be null");
        notNull(field, "Field can't be null");
        notNull(color, "Color can't be null");

        this.master = master;
        this.field = field;

        initSharable();

        probe.setColor(color);
        valueDisplay.setVisible(visible);
        valueDisplay.setColor(color);
        currentColor = color;
        probe.setVisible(visible);
        probe.setPointSize(pointSize);
        probe.setAutoSize(true);
        probe.addPropertyChangeListener(this);
        probe.setPointSize(getDisplayScale());

        numFmt.setMaximumFractionDigits(2);

        master.addDisplayable(valueDisplay);
        master.addDisplayable(probe);
        setField(field);
    }

    /**
     * Called whenever the probe fires off a {@link PropertyChangeEvent}. Only
     * handles position changes right now, all other events are discarded.
     *
     * @param e Object that describes the property change.
     * 
     * @throws NullPointerException if passed a {@code null} 
     * {@code PropertyChangeEvent}.
     */
    public void propertyChange(final PropertyChangeEvent e) {
        notNull(e, "Cannot handle a null property change event");
        if (e.getPropertyName().equals(SelectorDisplayable.PROPERTY_POSITION)) {
            RealTuple prev = getEarthPosition();
            //handleProbeUpdate();
            RealTuple current = getEarthPosition();
            if (prevPos != null) {
              fireProbePositionChanged(prev, current);
              handleProbeUpdate();
            }
            prevPos = current;
            //fireProbePositionChanged(prev, current);
        }
    }

    public void setField(final FlatField field) {
        notNull(field);
        this.field = field;
        handleProbeUpdate();
    }

    /**
     * Adds a {@link ProbeListener} to the listener list so that it can be
     * notified when the probe is changed.
     * 
     * @param listener {@code ProbeListener} to register. {@code null} 
     * listeners are not allowed.
     * 
     * @throws NullPointerException if {@code listener} is null.
     */
    public void addProbeListener(final ProbeListener listener) {
        notNull(listener, "Can't add a null listener");
        listeners.add(listener);
    }

    /**
     * Removes a {@link ProbeListener} from the notification list.
     * 
     * @param listener {@code ProbeListener} to remove. {@code null} values
     * are permitted, but since they are not allowed to be added...
     */
    public void removeProbeListener(final ProbeListener listener) {
        listeners.remove(listener);
    }

    public boolean hasListener(final ProbeListener listener) {
        return listeners.contains(listener);
    }

    /**
     * Notifies the registered {@link ProbeListener}s that this probe's 
     * position has changed.
     * 
     * @param previous Previous position.
     * @param current Current position.
     */
    protected void fireProbePositionChanged(final RealTuple previous, final RealTuple current) {
        notNull(previous);
        notNull(current);

        ProbeEvent<RealTuple> event = new ProbeEvent<>(this, previous, current);
        for (ProbeListener listener : listeners) {
            listener.probePositionChanged(event);
        }
    }

    /**
     * Notifies the registered {@link ProbeListener}s that this probe's color
     * has changed.
     * 
     * @param previous Previous color.
     * @param current Current color.
     */
    protected void fireProbeColorChanged(final Color previous, final Color current) {
        notNull(previous);
        notNull(current);

        ProbeEvent<Color> event = new ProbeEvent<>(this, previous, current);
        for (ProbeListener listener : listeners) {
            listener.probeColorChanged(event);
        }
    }

    /**
     * Notifies registered {@link ProbeListener}s that this probe's visibility
     * has changed. Only takes a {@literal "previous"} value, which is negated
     * to form the {@literal "current"} value.
     * 
     * @param previous Visibility <b>before</b> change.
     */
    protected void fireProbeVisibilityChanged(final boolean previous) {
        ProbeEvent<Boolean> event = new ProbeEvent<>(this, previous, !previous);
        for (ProbeListener listener : listeners) {
            listener.probeVisibilityChanged(event);
        }
    }

    public void setColor(final Color color) {
        notNull(color, "Cannot set a probe to a null color");
        setColor(color, false);
    }

    private void setColor(final Color color, final boolean quietly) {
        assert color != null;

        if (currentColor.equals(color)) {
            return;
        }

        try {
            probe.setColor(color);
            valueDisplay.setColor(color);
            Color prev = currentColor;
            currentColor = color;

            if (!quietly) {
                fireProbeColorChanged(prev, currentColor);
            }
        } catch (Exception e) {
            LogUtil.logException("Couldn't set the color of the probe", e);
        }
    }

    public Color getColor() {
        return currentColor;
    }

    public String getValue() {
        return currentValue;
    }

    public double getLatitude() {
        return currentLatitude;
    }

    public double getLongitude() {
        return currentLongitude;
    }

    public void setLatLon(final Double latitude, final Double longitude) {
        notNull(latitude, "Null latitude values don't make sense!");
        notNull(longitude, "Null longitude values don't make sense!");

        try {
            EarthLocationTuple elt = new EarthLocationTuple(latitude, longitude, 0.0);
            double[] tmp = ((NavigatedDisplay)master).getSpatialCoordinates(elt, null);
            probe.setPosition(tmp[0], tmp[1]);
        } catch (Exception e) {
            LogUtil.logException("Failed to set the probe's position", e);
        }
    }

    public void quietlySetVisible(final boolean visibility) {
        try {
            probe.setVisible(visibility);
            valueDisplay.setVisible(visibility);
        } catch (Exception e) {
            LogUtil.logException("Couldn't set the probe's internal visibility", e);
        }
    }

    public void quietlySetColor(final Color newColor) {
        setColor(newColor, true);
    }

    public void handleProbeUpdate() {
        RealTuple pos = getEarthPosition();
        if (pos == null)
            return;

        Tuple positionValue = valueAtPosition(pos, field);
        if (positionValue == null) {
            return;
        }

        try {
            valueDisplay.setData(positionValue);
        } catch (Exception e) {
            LogUtil.logException("Failed to set readout value", e);
        }
    }

    public void handleProbeRemoval() {
        listeners.clear();
        try {
            master.removeDisplayable(valueDisplay);
            master.removeDisplayable(probe);
        } catch (Exception e) {
            LogUtil.logException("Problem removing visible portions of readout probe", e);
        }
        currentColor = null;
        field = null;
    }

    /**
     * Get the scaling factor for probes and such. The scaling is
     * the parameter that gets passed to TextControl.setSize() and
     * ShapeControl.setScale().
     * 
     * @return ratio of the current matrix scale factor to the
     * saved matrix scale factor.
     */
    public float getDisplayScale() {
        float scale = 1.0f;
        try {
            scale = master.getDisplayScale();
        } catch (Exception e) {
            System.err.println("Error getting display scale: "+e);
        }
        return scale;
    }

    public void setXYPosition(final RealTuple position) {
        if (position == null) {
            throw new NullPointerException("cannot use a null position");
        }

        try {
            probe.setPosition(position);
        } catch (Exception e) {
            LogUtil.logException("Had problems setting probe's xy position", e);
        }
    }

    public RealTuple getXYPosition() {
        RealTuple position = null;
        try {
            position = probe.getPosition();
        } catch (Exception e) {
            LogUtil.logException("Could not determine the probe's xy location", e);
        }
        return position;
    }

    public EarthLocationTuple getEarthPosition() {
        EarthLocationTuple earthTuple = null;
        try {
            double[] values = probe.getPosition().getValues();
            earthTuple = (EarthLocationTuple)((NavigatedDisplay)master).getEarthLocation(values[0], values[1], 1.0, true);
            currentLatitude = earthTuple.getLatitude().getValue();
            currentLongitude = earthTuple.getLongitude().getValue();
        } catch (Exception e) {
            LogUtil.logException("Could not determine the probe's earth location", e);
        }
        return earthTuple;
    }

    private Tuple valueAtPosition(final RealTuple position, final FlatField imageData) {
        assert position != null : "Cannot provide a null position";
        assert imageData != null : "Cannot provide a null image";

        double[] values = position.getValues();
        if (values[1] < -180) {
            values[1] += 360f;
        }

        if (values[0] > 180) {
            values[0] -= 360f;
        }

        Tuple positionTuple = null;
        try {
            // TODO(jon): do the positionFormat stuff in here. maybe this'll 
            // have to be an instance method?
            RealTuple corrected = new RealTuple(RealTupleType.SpatialEarth2DTuple, new double[] { values[1], values[0] });

            Real realVal = (Real)imageData.evaluate(corrected, Data.NEAREST_NEIGHBOR, Data.NO_ERRORS);
            float val = (float)realVal.getValue();
            if (Float.isNaN(val)) {
                currentValue = "NaN";
            } else {
                currentValue = numFmt.format(realVal.getValue());
            }
            positionTuple = new Tuple(TUPTYPE, new Data[] { corrected, new Text(TextType.Generic, currentValue) });
        } catch (Exception e) {
            LogUtil.logException("Encountered trouble when determining value at probe position", e);
        }
        return positionTuple;
    }

    private static RealTuple getInitialLinePosition() {
        RealTuple position = null;
        try {
            double[] center = { 0.0, 0.0 };
            position = new RealTuple(RealTupleType.SpatialCartesian2DTuple, 
                    new double[] { center[0], center[1] });
        } catch (Exception e) {
            LogUtil.logException("Problem with finding an initial probe position", e);
        }
        return position;
    }

    private static TextDisplayable createValueDisplay(final Color color) {
        assert color != null;

        DecimalFormat fmt = new DecimalFormat();
        fmt.setMaximumIntegerDigits(3);
        fmt.setMaximumFractionDigits(1);

        TextDisplayable td = null;
        try {
            td = new TextDisplayable(TextType.Generic);
            td.setLineWidth(2f);
            td.setColor(color);
            td.setNumberFormat(fmt);
        } catch (Exception e) {
            LogUtil.logException("Problem creating readout value container", e);
        }
        return td;
    }

    private static TupleType makeTupleType() {
        TupleType t = null;
        try {
            t = new TupleType(new MathType[] { RealTupleType.SpatialEarth2DTuple, TextType.Generic });
        } catch (Exception e) {
            LogUtil.logException("Problem creating readout tuple type", e);
        }
        return t;
    }

    /**
     * Returns a brief summary of a ReadoutProbe. Please note that this format
     * is subject to change.
     * 
     * @return String that looks like {@code [ReadProbe@HASHCODE: color=..., 
     * latitude=..., longitude=..., value=...]}
     */
    public String toString() {
        return String.format("[ReadoutProbe@%x: color=%s, latitude=%s, longitude=%s, value=%f]", 
            hashCode(), currentColor, currentLatitude, currentLongitude, currentValue);
    }
}
