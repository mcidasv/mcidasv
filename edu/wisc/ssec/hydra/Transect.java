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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.CompositeDisplayable;
import ucar.visad.display.ProfileLine;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.Displayable;
import ucar.unidata.collab.Sharable;
import ucar.unidata.collab.SharableImpl;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;
import visad.georef.MapProjection;
import visad.CommonUnit;
import visad.ScaledUnit;

import java.rmi.RemoteException;

import java.awt.Color;

import java.util.ArrayList;

import ucar.visad.ShapeUtility;
import ucar.visad.display.SelectorPoint;


public class Transect extends SharableImpl implements PropertyChangeListener {

    public static Object shareGroup = new String("Transect");
    ImageDisplay imgDisplay;
    DisplayMaster dspMaster;
    MyCrossSectionSelector css;
    ProfileLine line;
    SelectorPoint distMarker;

    HydraRGBDisplayable dataDisplayable = null; //FIXME: generalize to Displayable

    // The transect (line pts) data ref
    DataReference transectDataRef = new DataReferenceImpl("transect");

    // The field (image) being resampled (interrogated)
    DataReference targetDataRef = new DataReferenceImpl("target");

    Color color = null;

    int clrIdx = 0;

    // Projection changed for matching transects between different image display windows.
    boolean projForced = false;

    // a single display to overlay data transect graphs
    public static TransectDisplay transectDisplay = null;

    public static Color[] colors = new Color[]{Color.magenta, Color.green, Color.red, Color.blue};

    public static boolean[] colorUsed = new boolean[]{false, false, false, false};

    public static int numTransects = 0;

    public static MapProjection mapProjection = null;

    public static RealType DistAlongTransect = RealType.getRealType("Distance_Along_Transect_km", new ScaledUnit(1000.0, CommonUnit.meter, "kilometer"));

    public static ArrayList<Transect> transects = new ArrayList<Transect>();

    double frac = 0.1;


    public Transect(ImageDisplay imgDisplay, Displayable dataDisplayable) throws VisADException, RemoteException {
        super(shareGroup, true);
        this.imgDisplay = imgDisplay;
        this.dspMaster = imgDisplay.getDisplayMaster();

        if (mapProjection == null) {
            mapProjection = imgDisplay.getMapProjection();
            imgDisplay.setMapProjection();
        } else if (!mapProjection.equals(imgDisplay.getMapProjection())) {
            imgDisplay.setMapProjection(mapProjection, false);
            projForced = true;
        }

        this.dataDisplayable = (HydraRGBDisplayable) dataDisplayable;
        this.targetDataRef.setData(dataDisplayable.getData());

        color = findColor();

        if (numTransects == 0) {
            double[] trans = ImageDisplay.getTranslation(dspMaster);
            css = new MyCrossSectionSelector(color, ImageDisplay.baseScale / dspMaster.getScale(), trans, 200);
        } else {
            css = new MyCrossSectionSelector(color, 200);
        }
        css.setAutoSize(true);
        css.setInterpolateLinePoints(true);

        if (transectDisplay != null && numTransects > 0) { // initialize to transect(s) already drawn
            Transect transect = transects.get(0);
            css.setStartPoint(transect.getCrossSectionSelector().getStartPoint());
            css.setEndPoint(transect.getCrossSectionSelector().getEndPoint());
            frac = transect.frac;
        }

        line = (ProfileLine) ((CompositeDisplayable) css).getDisplayable(3); // idx=3 the line points
        dspMaster.addDisplayable(css);
        css.addPropertyChangeListener(this);


        VisADGeometryArray marker;
        marker = ShapeUtility.makeShape(ShapeUtility.TRIANGLE);
        marker = ShapeUtility.setSize(marker, .03f);

        distMarker = new SelectorPoint("distmarker", marker, getMarkerLocation(frac, css.getStartPoint(), css.getEndPoint()));
        distMarker.setManipulable(false);
        distMarker.setPointSize(css.getPointSize());
        distMarker.setAutoSize(true);
        distMarker.setColor(Color.RED);
        dspMaster.addDisplayable(distMarker);

        // manually autosize if needed
        double baseSize = css.getStartSelectorPoint().getScale();
        css.getStartSelectorPoint().setScale((float) ((ImageDisplay.baseScale / dspMaster.getScale()) * baseSize));
        css.getMiddleSelectorPoint().setScale((float) ((ImageDisplay.baseScale / dspMaster.getScale()) * baseSize));
        css.getEndSelectorPoint().setScale((float) ((ImageDisplay.baseScale / dspMaster.getScale()) * baseSize));
        distMarker.setScale((float) ((ImageDisplay.baseScale / dspMaster.getScale()) * baseSize));
        //-------------------

        FlatField dataTransect = updateGraph((Gridded2DSet) line.getData());
        transectDataRef.setData(makeTransectData(dataTransect));

        if (transectDisplay == null) {
            transectDisplay = new TransectDisplay(this, color, imgDisplay.getLocationOnScreen());
        } else {
            transectDisplay.addTransect(this, color);
        }

        transects.add(this);
        numTransects++;

        initSharable();
    }


    /**
     * Handle property change
     *
     * @param evt The event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(
                SelectorDisplayable.PROPERTY_POSITION)) {
            crossSectionChanged();
        }
        if (evt.getPropertyName().equals(
                "distmarker")) {
            try {
                frac = ((Float) evt.getNewValue()).floatValue();
                distMarker.setPoint(getMarkerLocation(frac, css.getStartPoint(), css.getEndPoint()));
                transectDisplay.updateValueLabel(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void crossSectionChanged() {
        try {
            Gridded2DSet set = (Gridded2DSet) line.getData();
            FlatField transectData = updateGraph(set);
            transectDataRef.setData(makeTransectData(transectData));
            if (distMarker != null) {
                distMarker.setPoint(getMarkerLocation(frac, css.getStartPoint(), css.getEndPoint()));
                transectDisplay.setMarkerDist(getMarkerDist(frac), (Gridded1DSet) ((FlatField) transectDataRef.getData()).getDomainSet());
                transectDisplay.updateValueLabel(this);
            }
            doShare("theLine", new Object[]{css.getStartPoint(), css.getEndPoint()});
        } catch (VisADException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public double getMarkerDist(double frac) throws VisADException, RemoteException {
        GriddedSet dset = (GriddedSet) ((FlatField) transectDataRef.getData()).getDomainSet();
        double low = dset.getLow()[0];
        double hi = dset.getHi()[0];
        return frac * hi;
    }

    public RealTuple getMarkerLocation(double frac, RealTuple startPt, RealTuple endPt) throws VisADException, RemoteException {
        double[] start = startPt.getValues();
        double[] end = endPt.getValues();

        double delx = end[0] - start[0];
        double dely = end[1] - start[1];

        RealTuple pt = new RealTuple(RealTupleType.SpatialCartesian2DTuple, new double[]{start[0] + delx * frac, start[1] + dely * frac});
        return pt;
    }

    public void remove() {
        try {
            dspMaster.removeDisplayable(css);
            if (distMarker != null) {
                dspMaster.removeDisplayable(distMarker);
            }
            transectDisplay = null;
            mapProjection = null;
            colorUsed[clrIdx] = false;
            imgDisplay.transect = null;
            if (projForced) {
                imgDisplay.resetMapProjection();
            }
            imgDisplay = null;
            dspMaster = null;
            dataDisplayable = null;
            targetDataRef = null;
            removeSharable();
        } catch (VisADException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void removeGraph() {
        try {
            if (transectDisplay != null) {
                transectDisplay.removeTransect(this);
                transects.remove(this);
                removeSharable();
                colorUsed[clrIdx] = false;
                numTransects -= 1;
                if (numTransects == 0) { //- if none left, allow mapProjection to be reset
                    mapProjection = null;
                }
            }
        } catch (VisADException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void removeAll() {
        for (int k = 0; k < transects.size(); k++) {
            transects.get(k).remove();
        }
        transects.clear();
        numTransects = 0;
        transectDisplay = null;
    }

    public void updateData(FlatField image) {
        try {
            this.targetDataRef.setData(image);
            FlatField dataTransect = updateGraph((Gridded2DSet) line.getData());
            FlatField transect = makeTransectData(dataTransect);
            RealType rngType = (RealType) ((FunctionType) transect.getType()).getRange();
            this.transectDisplay.transectRangeChanged(rngType);
            transectDataRef.setData(transect);
            this.transectDisplay.updateValueLabel(this);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public FlatField updateGraph(Gridded2DSet set) throws VisADException, RemoteException {
        FlatField dataTransect = null;
        if (dspMaster instanceof NavigatedDisplay) {
            int npts = set.getLength();
            float[][] xyz = set.getSamples();
            float[][] lonlat = new float[2][npts];
            for (int k = 0; k < npts; k++) {
                EarthLocation el = ((NavigatedDisplay) dspMaster).getEarthLocation(xyz[0][k], xyz[1][k], 0, true);
                LatLonPoint llp = el.getLatLonPoint();
                lonlat[0][k] = (float) llp.getLongitude().getValue();
                lonlat[1][k] = (float) llp.getLatitude().getValue();
            }
            Gridded2DSet llset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, lonlat, npts);
            dataTransect = (FlatField) ((FlatField) targetDataRef.getData()).resample(llset);
        }
        return dataTransect;
    }

    public FlatField makeTransectData(FlatField data) throws VisADException, RemoteException {
        if (data == null) { // TODO: infrequntly can come in as null at beginning of dragging
            return null;
        }
        FunctionType fncType = (FunctionType) data.getType();

        RealType domainType = Transect.DistAlongTransect;
        RealType rangeType = (RealType) fncType.getRange();

        if (rangeType.getName().contains("Reflectance")) {
            rangeType = Hydra.reflectance;
        } else if (rangeType.getName().contains("BrightnessTemp")) {
            rangeType = Hydra.brightnessTemp;
        }


        final Set domainSet = data.getDomainSet();
        int len = domainSet.getLength();
        float[][] lonlat = domainSet.indexToValue(new int[]{0, len - 1});
        float dist = secantDistance(lonlat);
        Linear1DSet newDomain = new Linear1DSet(domainType, 0, dist, len);
        FlatField newFF = new FlatField(new FunctionType(domainType, rangeType), newDomain);
        newFF.setSamples(data.getFloats());

        return newFF;
    }

    public static float secantDistance(float[][] lonlat) {

        float dist = Float.NaN;
        try {
            CoordinateSystem cs = visad.Display.DisplaySphericalCoordSys;
            float[][] xyz = cs.toReference(new float[][]{{lonlat[1][0], lonlat[1][1]}, {lonlat[0][0], lonlat[0][1]}, {6280f, 6280f}});
            double dx = xyz[0][0] - xyz[0][1];
            double dy = xyz[1][0] - xyz[1][1];
            double dz = xyz[2][0] - xyz[2][1];
            dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dist;
    }

    public DataReference getTransectDataRef() {
        return transectDataRef;
    }

    public void receiveShareData(Sharable from, Object id, Object[] data) {
        RealTuple startPt = (RealTuple) data[0];
        RealTuple endPt = (RealTuple) data[1];
        try {
            css.setStartPoint(startPt);
            css.setEndPoint(endPt);
        } catch (VisADException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public MyCrossSectionSelector getCrossSectionSelector() {
        return css;
    }

    public Color findColor() {
        Color color = colors[0];
        for (int ci = 0; ci < colors.length; ci++) {
            if (!colorUsed[ci]) {
                color = colors[ci];
                clrIdx = ci;
                colorUsed[ci] = true;
                break;
            }
        }
        return color;
    }

    public Color getColor() {
        return color;
    }
}
