package edu.wisc.ssec.mcidasv.control;

import java.awt.Color;
import java.rmi.RemoteException;

import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;

import ucar.unidata.idv.control.LineProbeControl;
import ucar.unidata.util.LogUtil;
import ucar.visad.display.DisplayableData;
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

public class HydraImageProbe2 extends LineProbeControl {

    private DataReference positionRef = null;
    
    private DataReference spectrumRef = null;
    
    private Color currentColor = Color.MAGENTA;
    
    private RealTuple currentPosition = null;
    
//    private DisplayableData imageDisplay = null;
    
    private Tuple locationValue = null;
    
    private MultiSpectralDisplay display = null;
    
//    private boolean isValidState = false;
    
    public HydraImageProbe2() throws VisADException, RemoteException {
        super();
        currentPosition = new RealTuple(RealTupleType.Generic2D);
        
        spectrumRef = new DataReferenceImpl(hashCode() + "_spectrumRef");
        positionRef = new DataReferenceImpl(hashCode() + "_positionRef");
        
        new Ugh();
    }
    
    // TODO: not using this one
    public HydraImageProbe2(final MultiSpectralDisplay disp, final Color color) throws VisADException, RemoteException {
        super();

        display = disp;
        
        spectrumRef = new DataReferenceImpl(hashCode() + "_spectrumRef");
        positionRef = new DataReferenceImpl(hashCode() + "_positionRef");
        
        currentPosition = new RealTuple(RealTupleType.Generic2D);
        
        currentColor = color;
        setColor(currentColor);
        
        display.addRef(spectrumRef, currentColor);
        
        new Ugh();
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
            updateLocationValue(newPos);
            updateSpectrum(newPos);
            updatePosition(newPos);
            currentPosition = newPos;
        } 

        Color tmp = getColor();
        if (!currentColor.equals(tmp)) {
            updateSpectrumColor(tmp);
            currentColor = tmp;
        }
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
    
    private void updateLocationValue(final RealTuple position) {
        Tuple tup = null;
        try {
            FlatField image = (FlatField)display.getImageDisplay().getData();
            if (image == null)
                return;

            double[] vals = position.getValues();
            double lon = vals[1];
            double lat = vals[0];

            if (lon < -180)
                lon += 360f;
            if (lon > 180)
                lon -= 360f;

            RealTuple lonLat = new RealTuple(RealTupleType.SpatialEarth2DTuple, new double[] { lon, lat });
            Real val = (Real)image.evaluate(lonLat, Data.NEAREST_NEIGHBOR, Data.NO_ERRORS);
            float fval = (float)val.getValue();
            tup = new Tuple(new TupleType(new MathType[] {RealTupleType.SpatialEarth2DTuple, TextType.Generic}),
                new Data[] { lonLat, new Text(TextType.Generic, Float.toString(fval)) });
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.updateLocationValue", e);
        }

        if (tup != null)
            locationValue = tup;
    }
    
    
    public void forceUpdateSpectrum() {
        updatePosition(currentPosition);
        updateSpectrum(currentPosition);
        
    }
    
    // TODO: position isn't actually used!
    private void updateSpectrum(final RealTuple position) {
        try {
            // TODO: better name!
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
            EarthLocationTuple elt = (EarthLocationTuple)boxToEarth(new double[] { vals[0], vals[1], 1.0 });
            positionRef.setData(elt.getLatLonPoint());
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.updatePosition", e);
        }
    }
    
    private void updateSpectrumColor(final Color color) {
        try {
            display.updateRef(spectrumRef, color);
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.updateColor", e);
        }
    }

    private class Ugh extends CellImpl {
        private boolean init = false;
        public Ugh() throws VisADException, RemoteException {
            this.addReference(positionRef);
        }
        
        public void doAction() {

        }
    }
    
}
