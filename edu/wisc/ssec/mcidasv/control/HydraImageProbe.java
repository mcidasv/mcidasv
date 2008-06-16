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
            updateLocationValue();
            updateSpectrum();
            updatePosition(newPos);
            currentPosition = newPos;
        } 

        Color tmp = getColor();
        if (!currentColor.equals(tmp)) {
            updateSpectrumColor(tmp);
            currentColor = tmp;
        }
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

            // lat = vals[0]; lon = vals[1]
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

    private void updatePosition(final RealTuple position) {
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
