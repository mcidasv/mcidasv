package edu.wisc.ssec.mcidasv.probes;

import java.awt.Color;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.concurrent.CopyOnWriteArrayList;

import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;

import ucar.unidata.idv.control.LineProbeControl;
import ucar.unidata.util.LogUtil;
import ucar.visad.display.TextDisplayable;

import visad.Data;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.FlatField;
import visad.MathType;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.Text;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.VisADException;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonTuple;

public class ReadoutProbe extends LineProbeControl {

    // why create it more than once?
    protected static final TupleType TUPTYPE = createTupleType();

    /** Default probe color. */
    private static final Color DEFAULT_COLOR = Color.MAGENTA;

    /**
     * {@link ProbeListener}s that are interested in knowing when this probe
     * changes.
     */
    private final CopyOnWriteArrayList<ProbeListener> listeners = 
        new CopyOnWriteArrayList<ProbeListener>();

    /** Displays the value of the data at the current position. */
    private final TextDisplayable valueDisplay = createValueDisplay(DEFAULT_COLOR);

    /** Holds the current location of the probe. */
    protected final DataReference positionRef;

    /** The active color of the probe. Defaults to {@link #DEFAULT_COLOR}. */
    protected Color currentColor = DEFAULT_COLOR;

    /**
     * {@code true} if a {@literal "non-NaN"} value has been displayed at some 
     * point. 
     */
    private boolean dataLoaded = false;

    // TODO(jon): this is gonna have to change if we want to use these probes
    // for ANY imagery. Maybe I can get away with just using the FlatField?
    protected MultiSpectralDisplay display;

    public ReadoutProbe() throws VisADException, RemoteException {
        super();

        positionRef = new DataReferenceImpl(hashCode() + "_positionRef");
        positionRef.setData(new LatLonTuple());
    }

    @Override public void setDisplayVisibility(final boolean visibility) {
        boolean current = getDisplayVisibility(); // must be first, duh.
        super.setDisplayVisibility(visibility);

        if (current != visibility) {
            try {
                valueDisplay.setVisible(visibility);
            } catch (Exception e) { 
                LogUtil.logException("ReadoutProbe.setDisplayVisibility", e);
            }
            fireProbeVisibilityChanged(current);
        }
    }

    @Override protected void probePositionChanged(final RealTuple position) {
        if (display == null)
            return;

        try {
            RealTuple currentLocation = positionToLonLat(positionRef);
            updateLocation(position);
            RealTuple updatedLocation = positionToLonLat(positionRef);
            if (!currentLocation.equals(updatedLocation)) {
                updateReadoutValue();
                fireProbePositionChanged(currentLocation, (RealTuple)positionRef.getData());
            }

            Color tmp = getColor();
            if (!currentColor.equals(tmp)) {
                valueDisplay.setColor(tmp);
                fireProbeColorChanged(currentColor, tmp);
                currentColor = tmp;
            }
        } catch (Exception e) {
            // TODO(jon): figure out a way to restore to a better state.
            LogUtil.logException("ReadoutProbe.probePositionChanged", e);
        }
    }

    public void setDisplay(final MultiSpectralDisplay disp) throws VisADException, RemoteException {
        if (disp == null)
            throw new NullPointerException("Display cannot be null");
        display = disp;
    }

    /**
     * Returns a reference to the actual object handling the readout values. 
     * {@code valueDisplay} also contains the current location and the actual 
     * (non-text) data value.
     * 
     * @return Magical readout object.
     * 
     * @see #TUPTYPE
     */
    public TextDisplayable getValueDisplay() {
        return valueDisplay;
    }

    /**
     * Updates and displays the value at the probe's current location.
     */
    public void updateReadoutValue() {
        try {
            RealTuple lonLat = positionToLonLat(positionRef);
            if (lonLat == null)
                return;

            float value = valueAtLonLat(lonLat, display);
            // this test is here so that we don't display NaN values before the
            // image data has been loaded--if we display NaN values right away
            // the probe's TextDisplayable will be *under* the imagery
            if (!dataLoaded && Float.isNaN(value))
                return; 

            // however, *never* displaying NaN means that drags to areas 
            // without imagery don't get displayed! dataLoaded allows us to 
            // display any NaN values *after* the data is loaded up.
            dataLoaded = true;

            Tuple state = new Tuple(TUPTYPE, new Data[] { lonLat, new Text(TextType.Generic, Float.toString(value)), new Real(value) });
            valueDisplay.setData(state);
        } catch (Exception e) {
            LogUtil.logException("ReadoutProbe.updateLocationValue", e);
        }
    }

    /**
     * Updates {@link #positionRef} so that it reflects the current position 
     * of the probe.
     * 
     * @param location New x and y coordinates of the probe.
     */
    protected void updateLocation(final RealTuple location) {
        double[] vals = location.getValues();
        try {
            EarthLocationTuple elt = (EarthLocationTuple)boxToEarth(
                new double[] { vals[0], vals[1], 1.0 });
            positionRef.setData(elt.getLatLonPoint());
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.updatePosition", e);
        }
    }

    /**
     * Returns an array of the current latitude and longitude of the probe.
     * 
     * @return Either an array consisting of the latitude/longitude pair, or
     * two {@link Double#NaN}s if VisAD had problems.
     */
    public double[] getLatLonVals() {
        double[] latLon = new double[] { Double.NaN, Double.NaN };
        try {
            Tuple tup = (Tuple)valueDisplay.getData();
            double[] location = ((RealTuple)tup.getComponent(0)).getValues();
            latLon = new double[] { location[1], location[0] };
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.getLatLonVals", e);
        }
        return latLon;
    }

    /**
     * Returns an array of the {@literal "box"} coordinates of {@code lat} and
     * {@code lon}.
     * 
     * @param lat Latitude of the desired position.
     * @param lon Longitude of the desired position.
     * 
     * @return Either an array consisting of the x and y coordinates of 
     * {@code lat} and {@code lon}, or two {@link Double#NaN}s if VisAD had 
     * problems.
     */
    public double[] getBoxVals(double lat, double lon) {
        double[] xy = new double[] { Double.NaN, Double.NaN };
        try {
            EarthLocationTuple elt = new EarthLocationTuple(lat, lon, 0.0);
            double[] tmp = earthToBox(elt);
            xy[0] = tmp[0];
            xy[1] = tmp[1];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return xy;
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
        if (listener == null)
            throw new NullPointerException("Cannot add a null listener");
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
     * Notifies the registered {@link ProbeListener}s that this probe's 
     * position has changed.
     * 
     * @param previous Previous position.
     * @param current Current position.
     */
    protected void fireProbePositionChanged(final RealTuple previous, final RealTuple current) {
        if (previous == null)
            throw new NullPointerException();
        if (current == null)
            throw new NullPointerException();

        ProbeEvent<RealTuple> event = new ProbeEvent<RealTuple>(this, previous, current);
        for (ProbeListener listener : listeners)
            listener.probePositionChanged(event);
    }

    /**
     * Notifies the registered {@link ProbeListener}s that this probe's color
     * has changed.
     * 
     * @param previous Previous color.
     * @param current Current color.
     */
    protected void fireProbeColorChanged(final Color previous, final Color current) {
        if (previous == null)
            throw new NullPointerException();
        if (current == null)
            throw new NullPointerException();

        ProbeEvent<Color> event = new ProbeEvent<Color>(this, previous, current);
        for (ProbeListener listener : listeners)
            listener.probeColorChanged(event);
    }

    /**
     * Notifies registered {@link ProbeListener}s that this probe's visibility
     * has changed. Only takes a {@literal "previous"} value, which is negated
     * to form the {@literal "current"} value.
     * 
     * @param previous Visibility <b>before</b> change.
     */
    protected void fireProbeVisibilityChanged(final boolean previous) {
        ProbeEvent<Boolean> event = new ProbeEvent<Boolean>(this, previous, !previous);
        for (ProbeListener listener : listeners)
            listener.probeVisibilityChanged(event);
    }

    /**
     * Determines the {@literal "value"} displayed at a given location.
     * 
     * @param lonLat Location of the desired value.
     * @param display 
     * 
     * @return If all went well, the value of {@code display}'s image at 
     * {@code lonLat}. Otherwise, {@link Float#NaN}.
     * 
     * @throws VisADException if VisAD had problems.
     * @throws RemoteException if VisAD had problems.
     */
    protected static float valueAtLonLat(final RealTuple lonLat, final MultiSpectralDisplay display) throws VisADException, RemoteException {
        if (lonLat == null)
            return Float.NaN;

        FlatField image = (FlatField)display.getImageDisplay().getData();
        if (image == null)
            return Float.NaN;

        Real realVal = (Real)image.evaluate(lonLat, Data.NEAREST_NEIGHBOR, Data.NO_ERRORS);
        return (float)realVal.getValue();
    }

    /**
     * Builds and returns a {@link RealTupleType#SpatialEarth2DTuple} based off
     * of the data stored in {@code position}.
     * 
     * @param position {@code DataReference} that holds a location. Typically
     * {@link #positionRef}.
     * 
     * @return Either a tuple containing the longitude/latitude pair, or 
     * {@code null} if {@code position} had no data.
     * 
     * @throws VisADException if VisAD had problems.
     * @throws RemoteException if VisAD had problems.
     */
    protected static RealTuple positionToLonLat(final DataReference position) throws VisADException, RemoteException {
        RealTuple location = (RealTuple)position.getData();
        if (location == null)
            return null; // better value needed!

        double[] values = location.getValues();
        if (values[1] < -180)
            values[1] += 360f;

        if (values[0] > 180)
            values[0] -= 360f;

        return new RealTuple(RealTupleType.SpatialEarth2DTuple, new double[] { values[1], values[0] });
    }

    /**
     * Creates a displayable text object that can be used for the probe 
     * readout.
     * 
     * @param color Color of the displayed text. {@code null} values are not
     * permitted.
     * 
     * @return If there were no problems, a text object suitable for displaying
     * readout values. If there were problems, {@code null} is returned.
     * 
     * @throws VisADException if VisAD had trouble.
     * @throws RemoteException if VisAD had trouble.
     * @throws NullPointerException if {@code color} is null.
     */
    protected static TextDisplayable createValueDisplay(final Color color) throws VisADException, RemoteException {
        if (color == null)
            throw new NullPointerException("Cannot create a value display with a null color");

        DecimalFormat fmt = new DecimalFormat();
        fmt.setMaximumIntegerDigits(3);
        fmt.setMaximumFractionDigits(1);

        TextDisplayable td = new TextDisplayable(TextType.Generic);
        td.setLineWidth(2f);
        td.setColor(color);
        td.setNumberFormat(fmt);
        return td;
    }

    /**
     * Creates a tuple that allows for 
     * {@link RealTupleType#SpatialEarth2DTuple}, {@link TextType#Generic} and
     * {@link RealType#Generic} values. These values are used to convey a 
     * probe's location, text readout, and the actual data value.
     * 
     * @return A tuple that allows a {@literal "lonlat"} pair, a generic
     * text value, and the actual value at a given point.
     */
    protected static TupleType createTupleType() {
        TupleType t = null;
        try {
            t = new TupleType(new MathType[] { RealTupleType.SpatialEarth2DTuple, TextType.Generic, RealType.Generic });
        } catch (Exception e) {
            LogUtil.logException("ReadoutProbe.createTupleType", e);
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
    @Override public String toString() {
        double lat = Float.NaN;
        double lon = Float.NaN;
        double val = Float.NaN;

        try {
            Tuple tup = (Tuple)valueDisplay.getData();
            double[] location = ((RealTuple)tup.getComponent(0)).getValues();
            lat = location[1];
            lon = location[0];
            val = ((Real)tup.getComponent(2)).getValue();
        } catch (Exception e) {
            // Ignoring exceptions is bad, but here they are contextually 
            // irrelevant.
        }
        return String.format("[ReadoutProbe@%x: color=%s, latitude=%f, longitude=%f, value=%f]", hashCode(), currentColor, lat, lon, val);
    }
}
