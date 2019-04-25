/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2019
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

import static java.util.Objects.requireNonNull;
import static visad.RealTupleType.SpatialCartesian2DTuple;
import static visad.RealTupleType.SpatialEarth2DTuple;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ucar.unidata.collab.SharableImpl;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.util.LogUtil;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.PointProbe;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.SelectorPoint;
import ucar.visad.display.TextDisplayable;

import visad.ActionImpl;
import visad.ConstantMap;
import visad.Data;
import visad.Display;
import visad.DisplayEvent;
import visad.DisplayListener;
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
import visad.VisADGeometryArray;
import visad.georef.EarthLocationTuple;
import visad.georef.MapProjection;

import edu.wisc.ssec.mcidasv.control.LambertAEA;
import edu.wisc.ssec.mcidasv.util.MakeToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code ReadoutProbe} is a probe that combines a {@literal "pickable"} probe
 * widget with an adjacent text {@literal "readout"} of the data value at the 
 * probe's current location.
 * 
 * <p>Primarily used with 
 * {@link edu.wisc.ssec.mcidasv.control.MultiSpectralControl}.</p>
 */
public class ReadoutProbe 
    extends SharableImpl 
    implements PropertyChangeListener, DisplayListener 
{

    public static final String SHARE_PROFILE =
        "ReadoutProbeDeux.SHARE_PROFILE";

    public static final String SHARE_POSITION =
        "ReadoutProbeDeux.SHARE_POSITION";

    private static final Color DEFAULT_COLOR = Color.MAGENTA;

    private static final TupleType TUPTYPE = makeTupleType();
    
    private static final Logger logger =
        LoggerFactory.getLogger(ReadoutProbe.class);

    private final List<ProbeListener> listeners = new CopyOnWriteArrayList<>();

    /** Displays the value of the data at the current position. */
    private final TextDisplayable valueDisplay =
        createValueDisplay(DEFAULT_COLOR);

    private final PointSelector pointSelector =
        new PointSelector(getInitialProbePosition());

    private final DisplayMaster master;

    private Color currentColor;

    private String currentValue = "NaN";

    private double currentLatitude = Double.NaN;
    private double currentLongitude = Double.NaN;

    private float pointSize = 1.0f;

    private FlatField field;

    private static final DecimalFormat numFmt = new DecimalFormat();
    
    /** Used to keep track of the last zoom {@literal "level"}. */
    private float lastScale = Float.MIN_VALUE;

    /**
     * Create a {@literal "HYDRA"} probe that allows for displaying things 
     * like value at current position, current color, and location.
     *
     * <p>Note: <b>none</b> of the parameters permit {@code null} values.</p>
     *
     * @param control {@literal "Layer"} that will be probed.
     * @param flatField Data to probe.
     * @param color {@code Color} of the probe.
     * @param pattern Format string to use with probe's location values.
     * @param visible Whether or not the probe is visible.
     *
     * @throws NullPointerException if any of the given parameters are 
     *                              {@code null}.
     * @throws VisADException if VisAD had problems.
     * @throws RemoteException if VisAD had problems.
     */
    public ReadoutProbe(final DisplayControlImpl control,
                        final FlatField flatField,
                        final Color color,
                        final String pattern,
                        final boolean visible)
        throws VisADException, RemoteException
    {
        requireNonNull(control, "DisplayControlImpl can't be null");
        requireNonNull(flatField, "Field can't be null");
        requireNonNull(color, "Color can't be null");
        requireNonNull(pattern, "Pattern can't be null");
        
        master = control.getNavigatedDisplay();
        field = flatField;
        
        initSharable();
        
        pointSelector.setColor(color);
        valueDisplay.setVisible(visible);
        valueDisplay.setColor(color);
        currentColor = color;
        pointSelector.setVisible(visible);
        pointSelector.setPointSize(pointSize);
        pointSelector.setAutoSize(true);
        pointSelector.setPointSize(getDisplayScale());
        pointSelector.setZ(control.getZPosition());
        
        numFmt.applyPattern(pattern);
        
        master.addDisplayable(valueDisplay);
        master.addDisplayable(pointSelector);
        setField(flatField);
        
        // done mostly to avoid using "this" while we're still within the 
        // constructor
        addListeners();
    }
    
    /**
     * Add this probe instance to the relevant listeners.
     */
    private void addListeners() {
        pointSelector.addPropertyChangeListener(this);
        master.getDisplay().addDisplayListener(this);
    }

    /**
     * Called whenever the probe fires off a {@link PropertyChangeEvent}. 
     * 
     * <p>Only handles position changes right now, all other events are 
     * discarded.</p>
     *
     * @param e Object that describes the property change.
     * 
     * @throws NullPointerException if passed a {@code null} 
     * {@code PropertyChangeEvent}.
     */
    @Override public void propertyChange(final PropertyChangeEvent e) {
        requireNonNull(e, "Cannot handle a null property change event");
        if (e.getPropertyName().equals(SelectorDisplayable.PROPERTY_POSITION)) {
            RealTuple prev = getEarthPosition();
            RealTuple current = getEarthPosition();
            fireProbePositionChanged(prev, current);
            handleProbeUpdate();
        }
    }
    
    /**
     * Called for events happening in the {@link visad.DisplayImpl} 
     * associated with {@link DisplayMaster}. 
     * 
     * <p>The only event that is actually handled is 
     * {@link DisplayEvent#FRAME_DONE}, which allows us to snap the text 
     * value displayable to the actual {@literal "pickable"} probe.</p>
     * 
     * @param e Event to handle.
     */
    @Override public void displayChanged(DisplayEvent e) {
        // "snap" the text to the probe when zooming. the test for display
        // scale values is to ensure we don't attempt to update if the zoom
        // level didn't change.
        if (e.getId() == DisplayEvent.FRAME_DONE) {
            float currentScale = getDisplayScale();
            if (Float.compare(lastScale, currentScale) != 0) {
                handleProbeUpdate();
                lastScale = currentScale;
            }
        }
    }

    /**
     * Sets the {@link FlatField} associated with this probe to the given
     * {@code field}.
     *
     * @param flatField New {@code FlatField} for this probe.
     *
     * @throws NullPointerException if passed a {@code null} {@code field}.
     */
    public void setField(final FlatField flatField) {
        requireNonNull(flatField);
        this.field = flatField;
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
        requireNonNull(listener, "Can't add a null listener");
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

    /**
     * Determine whether or not a given {@link ProbeListener} is listening to
     * the current probe.
     *
     * @param listener {@code ProbeListener} to check. {@code null} values are
     * permitted.
     *
     * @return {@code true} if {@code listener} has been added to the list of
     * {@code ProbeListener} objects, {@code false} otherwise.
     */
    public boolean hasListener(final ProbeListener listener) {
        return listeners.contains(listener);
    }

    /**
     * Notifies the registered {@link ProbeListener ProbeListeners} that this
     * probe's position has changed.
     * 
     * @param previous Previous position. Cannot be {@code null}.
     * @param current Current position. Cannot be {@code null}.
     */
    protected void fireProbePositionChanged(final RealTuple previous,
                                            final RealTuple current)
    {
        requireNonNull(previous);
        requireNonNull(current);

        ProbeEvent<RealTuple> event =
            new ProbeEvent<>(this, previous, current);
        for (ProbeListener listener : listeners) {
            listener.probePositionChanged(event);
        }
    }

    /**
     * Notifies the registered {@link ProbeListener ProbeListeners} that this
     * probe's color has changed.
     * 
     * @param previous Previous color. Cannot be {@code null}.
     * @param current Current color. Cannot be {@code null}.
     */
    protected void fireProbeColorChanged(final Color previous,
                                         final Color current)
    {
        requireNonNull(previous);
        requireNonNull(current);

        ProbeEvent<Color> event =
            new ProbeEvent<>(this, previous, current);
        for (ProbeListener listener : listeners) {
            listener.probeColorChanged(event);
        }
    }

    /**
     * Notifies registered {@link ProbeListener ProbeListeners} that this
     * probe's visibility has changed. Only takes a {@literal "previous"}
     * value, which is negated to form the {@literal "current"} value.
     * 
     * @param previous Visibility <b>before</b> change.
     */
    protected void fireProbeVisibilityChanged(final boolean previous) {
        ProbeEvent<Boolean> event =
            new ProbeEvent<>(this, previous, !previous);
        for (ProbeListener listener : listeners) {
            listener.probeVisibilityChanged(event);
        }
    }

    /**
     * Notifies the registered {@link ProbeListener ProbeListeners} that this
     * probe's location format pattern has changed.
     *
     * @param previous Previous location format pattern.
     * @param current Current location format pattern.
     */
     protected void fireProbeFormatPatternChanged(final String previous,
                                                  final String current)
     {
         ProbeEvent<String> event =
             new ProbeEvent<>(this, previous, current);
         for (ProbeListener listener : listeners) {
             listener.probeFormatPatternChanged(event);
         }
     }

    /**
     * Change the color of this {@code ReadoutProbe} instance.
     *
     * @param color New color. Cannot be {@code null}.
     */
    public void setColor(final Color color) {
        requireNonNull(color, "Cannot set a probe to a null color");
        setColor(color, false);
    }

    public PointSelector getPointSelector() {
        return pointSelector;
    }
    
    public TextDisplayable getValueDisplay() {
        return valueDisplay;
    }
    
    /**
     * Change the color of this {@code ReadoutProbe} instance and control 
     * whether or not listeners should be notified.
     *
     * <p>Note that if {@code color} is the same as {@code currentColor},
     * nothing will happen (the method exits early).</p>
     *
     * @param color New color for this probe. Cannot be {@code null}.
     * @param quietly Whether or not to notify the list of
     * {@link ProbeListener ProbeListeners} of a color change.
     */
    private void setColor(final Color color, final boolean quietly) {
        assert color != null;

        if (currentColor.equals(color)) {
            return;
        }

        try {
            pointSelector.setColor(color);
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
    
    /**
     * Get the current color of this {@code ReadoutProbe} instance.
     * 
     * @return {@code Color} of this {@code ReadoutProbe}.
     */
    public Color getColor() {
        return currentColor;
    }
    
    /**
     * Get the current {@literal "readout value"} of this 
     * {@code ReadoutProbe} instance. 
     * 
     * @return The value of the data at the probe's current location.
     */
    public String getValue() {
        return currentValue;
    }
    
    /**
     * Get the current latitude of this {@code ReadoutProbe} instance.
     * 
     * @return Current latitude of the probe.
     */
    public double getLatitude() {
        return currentLatitude;
    }
    
    /**
     * Get the current longitude of this {@code ReadoutProbe} instance.
     *
     * @return Current longitude of the probe.
     */
    public double getLongitude() {
        return currentLongitude;
    }

    public void setLatLon(final double latitude, final double longitude) {
        try {
            EarthLocationTuple elt =
                new EarthLocationTuple(latitude, longitude, 0.0);
            double[] tmp =
                ((NavigatedDisplay)master).getSpatialCoordinates(elt, null);
            pointSelector.setPosition(tmp[0], tmp[1]);
        } catch (Exception e) {
            LogUtil.logException("Failed to set the pointSelector's position", e);
        }
    }

    public void quietlySetVisible(final boolean visibility) {
        try {
            pointSelector.setVisible(visibility);
            valueDisplay.setVisible(visibility);
        } catch (Exception e) {
            LogUtil.logException("Couldn't set the probe's internal visibility", e);
        }
    }

    public void quietlySetColor(final Color newColor) {
        setColor(newColor, true);
    }

    /**
     * Update the location format pattern for the current probe.
     *
     * @param pattern New location format pattern. Cannot be {@code null}.
     */
    public void setFormatPattern(final String pattern) {
        setFormatPattern(pattern, false);
    }

    /**
     * Update the location format pattern for the current probe, but
     * <b>do not</b> fire off any events.
     *
     * @param pattern New location format pattern. Cannot be {@code null}.
     */
    public void quietlySetFormatPattern(final String pattern) {
        setFormatPattern(pattern, true);
    }

    /**
     * Update the location format pattern for the current probe and optionally
     * fire off an update event.
     *
     * @param pattern New location format pattern. Cannot be {@code null}.
     * @param quietly Whether or not to fire a format pattern change update.
     */
    private void setFormatPattern(final String pattern,
                                  final boolean quietly)
    {
        String previous = numFmt.toPattern();
        numFmt.applyPattern(pattern);
        if (!quietly) {
            fireProbeFormatPatternChanged(previous, pattern);
        }
    }

    /**
     * Returns the number format string current being used.
     *
     * @return Location format pattern string.
     */
    public String getFormatPattern() {
        return numFmt.toPattern();
    }
    
    public void handleProbeUpdate() {
        RealTuple pos = getEarthPosition();
        if (pos == null) {
            return;
        }

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
    
    /**
     * Called when this probe has been removed.
     */
    public void handleProbeRemoval() {
        listeners.clear();
        try {
            master.getDisplay().removeDisplayListener(this);
            master.removeDisplayable(valueDisplay);
            master.removeDisplayable(pointSelector);
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
    public final float getDisplayScale() {
        float scale = 1.0f;
        try {
            scale = master.getDisplayScale();
        } catch (Exception e) {
            LogUtil.logException("Error getting display scale.", e);
        }
        return scale;
    }

    public void setXYPosition(final RealTuple position) {
        if (position == null) {
            throw new NullPointerException("cannot use a null position");
        }

        try {
            pointSelector.setPosition(position);
        } catch (Exception e) {
            LogUtil.logException("Had problems setting probe's xy position", e);
        }
    }

    public RealTuple getXYPosition() {
        RealTuple position = null;
        try {
            position = pointSelector.getPosition();
        } catch (Exception e) {
            LogUtil.logException("Could not determine the probe's xy location", e);
        }
        return position;
    }
    
    /**
     * Get the current {@literal "earth location"} of the probe.
     * 
     * <p>Note: this method will attempt to change the {@link #currentLatitude} 
     * and {@link #currentLongitude} fields.</p>
     * 
     * @return Location of {@link #pointSelector}, or {@code null} if the 
     *         location could not be determined.
     */
    public EarthLocationTuple getEarthPosition() {
        EarthLocationTuple earthTuple = null;
        try {
            double[] values = pointSelector.getPosition().getValues();
            earthTuple = (EarthLocationTuple)((NavigatedDisplay)master).getEarthLocation(values[0], values[1], 1.0, true);
            currentLatitude = earthTuple.getLatitude().getValue();
            currentLongitude = earthTuple.getLongitude().getValue();
        } catch (Exception e) {
            LogUtil.logException("Could not determine the probe's earth location", e);
        }
        return earthTuple;
    }
    
    /**
     * Respond to the projection having been changed.
     * 
     * @param newProjection New projection. Can be {@code null}.
     */
    public void projectionChanged(MapProjection newProjection) {
        setLatLon(currentLatitude, currentLongitude);
        handleProbeUpdate();
    }
    
    private Tuple valueAtPosition(final RealTuple position,
                                  final FlatField imageData)
    {
        assert position != null : "Cannot provide a null position";
        assert imageData != null : "Cannot provide a null image";
        double[] values = position.getValues();
        
        // offset slightly so that the value readout isn't directly on top of
        // the actual pointSelector
        double offset = 0.5 * getDisplayScale();
        
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
            
            // "corrected" is where the text should be positioned
            RealTuple corrected = makeEarth2dTuple(values[0] + offset,
                                                   values[1] + offset);
            
            // "probeLoc" is where pointSelector is positioned
            RealTuple probeLoc = makeEarth2dTuple(values[0], values[1]);
            
            Real realVal = (Real)imageData.evaluate(probeLoc, Data.NEAREST_NEIGHBOR, Data.NO_ERRORS);
            float val = (float)realVal.getValue();
            if (Float.isNaN(val)) {
                currentValue = "NaN";
            } else {
                currentValue = numFmt.format(realVal.getValue());
            }
            positionTuple = new Tuple(TUPTYPE, new Data[] { corrected, new Text(TextType.Generic, currentValue) });
        } catch (Exception e) {
            LogUtil.logException("Encountered trouble when determining value at pointSelector position", e);
        }
        return positionTuple;
    }
    
    /**
     * Returns a {@link RealTupleType#SpatialEarth2DTuple SpatialEarth2DTuple}
     * for the given latitude and longitude.
     * 
     * <p>Be aware that for whatever reason VisAD wants the longitude first,
     * then the latitude.</p>
     * 
     * @param lat Latitude of the position.
     * @param lon Longitude of the position.
     * 
     * @return {@code SpatialEarth2DTuple} containing {@code lat} and 
     *         {@code lon}.
     *
     * @throws VisADException Problem creating VisAD object.
     * @throws RemoteException Java RMI error.
     */
    private static RealTuple makeEarth2dTuple(double lat, double lon)
        throws VisADException, RemoteException
    {
        return new RealTuple(SpatialEarth2DTuple, new double[] { lon, lat });
    }
    
    private static RealTuple getInitialProbePosition() {
        RealTuple position = null;
        try {
            position = new RealTuple(SpatialCartesian2DTuple,
                                     new double[] { 0.0, 0.0 });
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
            t = new TupleType(new MathType[] { SpatialEarth2DTuple, TextType.Generic });
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
        return MakeToString.fromInstance(this)
                           .add("color", currentColor)
                           .add("latitude", currentLatitude)
                           .add("longitude", currentLongitude)
                           .add("value", currentValue).toString();
    }
    
    /**
     * This class is a reimplementation of {@link PointProbe} that whose 
     * mouse movement is limited to the x- and y- axes.
     * 
     * <p>To change the position of the instance along the z-axis, try something
     * like the following:
     * {@code new PointSelector().setZ(zPosition)}.
     * </p>
     */
    public static class PointSelector extends SelectorDisplayable {
        
        /** pointSelector */
        private SelectorPoint point;
        
        /** flag for whether we're in the process of setting the position */
        private volatile boolean settingPosition = false;
        
        /**
         * Construct a point pointSelector.
         *
         * @throws VisADException Problem creating VisAD object.
         * @throws RemoteException Java RMI error.
         */
        public PointSelector() throws VisADException, RemoteException {
            this(0, 0);
        }
        
        /**
         * Construct a point pointSelector at the location specified.
         *
         * @param x X position.
         * @param y Y position.
         *
         * @throws VisADException Problem creating VisAD object.
         * @throws RemoteException Java RMI error.
         */
        public PointSelector(double x, double y) 
            throws VisADException, RemoteException 
        {
            this(new RealTuple(SpatialCartesian2DTuple,
                               new double[] { x, y }));
        }
    
        /**
         * Construct a pointSelector at the position specified.
         *
         * @param position Position of the pointSelector.
         *
         * @throws VisADException Problem creating VisAD object.
         * @throws RemoteException Java RMI error.
         */
        public PointSelector(RealTuple position)
            throws VisADException, RemoteException 
        {
            point = new SelectorPoint("Probe point", position);
            
            addDisplayable(point);
            setPosition(position);
            point.addAction(new ActionImpl("point listener") {
                @Override public void doAction() {
                    if (settingPosition) {
                        return;
                    }
                    notifyListenersOfMove();
                }
            });
        }
        
        /**
         * Get the selector point
         *
         * @return the selector point
         */
        public SelectorPoint getSelectorPoint() {
            return point;
        }
    
        /**
         * Set if any of the axis movements are fixed
         *
         * @param x x fixed
         * @param y y fixed
         * @param z z fixed
         */
        public void setFixed(boolean x, boolean y, boolean z) {
            point.setFixed(x, y, z);
        }
    
        public void setZ(double newz) {
            try {
                point.addConstantMap(new ConstantMap(newz, Display.ZAxis));
            } catch (VisADException | RemoteException e) {
                logger.error("problem setting z", e);
            }
        }
        
        /**
         * Get the point scale
         *
         * @return the point scale
         */
        public float getPointScale() {
            if (point != null) {
                return point.getScale();
            }
            return 1.0f;
        }
        
        /**
         * Set the type of marker used for the pointSelector.
         *
         * @param marker  marker as a VisADGeometryArray
         *
         * @throws RemoteException Java RMI error
         * @throws VisADException Problem creating VisAD object.
         */
        public void setMarker(VisADGeometryArray marker)
            throws VisADException, RemoteException
        {
            point.setMarker(marker);
        }
        
        /**
         * Set the type of marker used for the pointSelector.
         *
         * @param marker {@link ucar.visad.ShapeUtility ShapeUtility} marker.
         *
         * @throws VisADException Problem creating VisAD object.
         * @throws RemoteException Java RMI error.
         */
        public void setMarker(String marker)
            throws VisADException, RemoteException
        {
            point.setMarker(marker);
        }
        
        /**
         * Set whether the marker should automatically resize as the
         * display is zoomed.
         *
         * @param yesorno  true to automatically resize the marker.
         *
         * @throws VisADException Problem creating VisAD object.
         * @throws RemoteException Java RMI error.
         */
        public void setAutoSize(boolean yesorno)
            throws VisADException, RemoteException
        {
            point.setAutoSize(yesorno);
        }
        
        /**
         * Get the position of the pointSelector.
         * 
         * @return Current position.
         */
        public RealTuple getPosition() {
            return point.getPoint();
        }
        
        /**
         * Set the pointSelector's x/y position
         *
         * @param x X position.
         * @param y X position.
         *
         * @throws VisADException Problem creating VisAD object.
         * @throws RemoteException Java RMI error.
         */
        public void setPosition(double x, double y)
            throws VisADException, RemoteException 
        {
            setPosition(
                new RealTuple(SpatialCartesian2DTuple,
                              new double[] { x, y }));
        }
        
        /**
         * Set the pointSelector's position.
         *
         * @param position Position of the pointSelector
         *
         * @throws VisADException Problem creating VisAD object.
         * @throws RemoteException Java RMI error.
         */
        public void setPosition(RealTuple position)
            throws VisADException, RemoteException 
        {
            settingPosition = true;
            try {
                point.setPoint(position);
            } finally {
                settingPosition = false;
            }
        }
    }
}
