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

package edu.wisc.ssec.mcidasv.display.hydra;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import visad.CellImpl;
import visad.ConstantMap;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.Display;
import visad.DisplayEvent;
import visad.DisplayListener;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded1DSet;
import visad.Gridded2DSet;
import visad.LocalDisplay;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.VisADException;
import visad.bom.RubberBandBoxRendererJ3D;

import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.util.LogUtil;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.XYDisplay;

import edu.wisc.ssec.mcidasv.control.HydraCombo;
import edu.wisc.ssec.mcidasv.control.HydraControl;
import edu.wisc.ssec.mcidasv.control.LinearCombo;
import edu.wisc.ssec.mcidasv.control.MultiSpectralControl;
import edu.wisc.ssec.mcidasv.data.HydraDataSource;
import edu.wisc.ssec.mcidasv.data.hydra.GrabLineRendererJ3D;
import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralDataSource;
import edu.wisc.ssec.mcidasv.data.hydra.SuomiNPPDataSource;

public class MultiSpectralDisplay implements DisplayListener {

    private static final Logger logger = LoggerFactory.getLogger(MultiSpectralDisplay.class);
    
    private static final String DISP_NAME = "Spectrum";
    private static int cnt = 1;

    private DirectDataChoice dataChoice;

    private ViewManager viewManager;

    private float[] initialRangeX;
    private float[] initialRangeY = { 180f, 320f };

    private RealType domainType;
    private RealType rangeType;
    private RealType uniqueRangeType;

    private ScalarMap xmap;
    private ScalarMap ymap;

    private LocalDisplay display;

    private FlatField image;

    private FlatField spectrum = null;

    private boolean imageExpired = true;

    private MultiSpectralData data;

    private float waveNumber;

    private List<DataReference> displayedThings = new ArrayList<DataReference>();
    private HashMap<String, DataReference> idToRef = new HashMap<String, DataReference>();
    private HashMap<DataReference, ConstantMap[]> colorMaps = 
        new HashMap<DataReference, ConstantMap[]>();

    private HydraControl displayControl;

    private DisplayableData imageDisplay = null;

    private XYDisplay master;

    private Gridded1DSet domainSet;

    private JComboBox bandSelectComboBox = null;

    public MultiSpectralDisplay(final HydraControl control) 
        throws VisADException, RemoteException 
    {
        displayControl = control;
        dataChoice = (DirectDataChoice)displayControl.getDataChoice();

        init();
    }

    public MultiSpectralDisplay(final DirectDataChoice dataChoice) 
        throws VisADException, RemoteException 
    {
        this.dataChoice = dataChoice;
        init();
    }

    // TODO: generalize this so that you can grab the image data for any
    // channel
    public FlatField getImageData() {
        try {
            if ((imageExpired) || (image == null)) {
                imageExpired = false;

              MultiDimensionSubset select = null;
              Hashtable table = dataChoice.getProperties();
              Enumeration keys = table.keys();
              while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                if (key instanceof MultiDimensionSubset) {
                  select = (MultiDimensionSubset) table.get(key);
                }
              }
              HashMap subset = select.getSubset();
              image = data.getImage(waveNumber, subset);
              image = changeRangeType(image, uniqueRangeType);
            }
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralDisplay.getImageData", e);
        }

        return image;
    }

    public FlatField getImageDataFrom(final float channel) {
        FlatField imageData = null;
        try {
            MultiDimensionSubset select = null;
            Hashtable table = dataChoice.getProperties();
            Enumeration keys = table.keys();
            while (keys.hasMoreElements()) {
              Object key = keys.nextElement();
              if (key instanceof MultiDimensionSubset) {
                select = (MultiDimensionSubset) table.get(key);
              }
            }
            HashMap subset = select.getSubset();
            imageData = data.getImage(channel, subset);
            uniqueRangeType = RealType.getRealType(rangeType.getName()+"_"+cnt++);
            imageData = changeRangeType(imageData, uniqueRangeType);
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralDisplay.getImageDataFrom", e);
        }
        return imageData;
    }

    private FlatField changeRangeType(FlatField image, RealType newRangeType) throws VisADException, RemoteException {
      FunctionType ftype = (FunctionType)image.getType();
      FlatField new_image = new FlatField(
         new FunctionType(ftype.getDomain(), newRangeType), image.getDomainSet());
      new_image.setSamples(image.getFloats(false), false);
      return new_image;
    }
   

    public LocalDisplay getDisplay() {
        return display;
    }

    public Component getDisplayComponent() {
      return master.getDisplayComponent();
    }

    public RealType getDomainType() {
        return domainType;
    }

    public RealType getRangeType() {
        return rangeType;
    }

    public ViewManager getViewManager() {
        return viewManager;
    }

    public MultiSpectralData getMultiSpectralData() {
        return data;
    }

    public Gridded1DSet getDomainSet() {
        return domainSet;
    }

    private void init() throws VisADException, RemoteException {
    	
        HydraDataSource source = 
              (HydraDataSource) dataChoice.getDataSource();

        // TODO revisit this, may want to move method up to base class HydraDataSource
        if (source instanceof SuomiNPPDataSource) {
        	data = ((SuomiNPPDataSource) source).getMultiSpectralData(dataChoice);
        }
        
        if (source instanceof MultiSpectralDataSource) {
        	data = ((MultiSpectralDataSource) source).getMultiSpectralData(dataChoice);
        }

        waveNumber = data.init_wavenumber;

        try {
            spectrum = data.getSpectrum(new int[] { 1, 1 });
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralDisplay.init", e);
        }

        domainSet = (Gridded1DSet)spectrum.getDomainSet();
        initialRangeX = getXRange(domainSet);
        initialRangeY = data.getDataRange();

        domainType = getDomainType(spectrum);
        rangeType = getRangeType(spectrum);

        master = new XYDisplay(DISP_NAME, domainType, rangeType);

        setDisplayMasterAttributes(master);

        // set up the x- and y-axis
        xmap = new ScalarMap(domainType, Display.XAxis);
        ymap = new ScalarMap(rangeType, Display.YAxis);

        xmap.setRange(initialRangeX[0], initialRangeX[1]);
        ymap.setRange(initialRangeY[0], initialRangeY[1]);

        display = master.getDisplay();
        display.addMap(xmap);
        display.addMap(ymap);
        display.addDisplayListener(this);

        new RubberBandBox(this, xmap, ymap);

        if (displayControl == null) { //- add in a ref for the default spectrum, ie no DisplayControl
            DataReferenceImpl spectrumRef = new DataReferenceImpl(hashCode() + "_spectrumRef");
            spectrumRef.setData(spectrum);
            addRef(spectrumRef, Color.WHITE);
        }

        if (data.hasBandNames()) {
            bandSelectComboBox = new JComboBox(data.getBandNames().toArray());
            bandSelectComboBox.setSelectedItem(data.init_bandName);
            bandSelectComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String bandName = (String)bandSelectComboBox.getSelectedItem();
                    if (bandName == null)
                        return;

                    HashMap<String, Float> bandMap = data.getBandNameMap();
                    if (bandMap == null)
                        return;

                    if (!bandMap.containsKey(bandName))
                        return;

                    setWaveNumber(bandMap.get(bandName));
                }
            });
        }
    }

    public JComboBox getBandSelectComboBox() {
      return bandSelectComboBox;
    }

    // TODO: HACK!!
    public void setDisplayControl(final HydraControl control) {
        displayControl = control;
    }

    public void displayChanged(final DisplayEvent e) throws VisADException, RemoteException {
        // TODO: write a method like isChannelUpdate(EVENT_ID)? or maybe just 
        // deal with a super long if-statement and put an "OR MOUSE_RELEASED" 
        // up here?
        if (e.getId() == DisplayEvent.MOUSE_RELEASED_CENTER) {
            float val = (float)display.getDisplayRenderer().getDirectAxisValue(domainType);
            setWaveNumber(val);
            if (displayControl != null)
                displayControl.handleChannelChange(val);
        }
        else if (e.getId() == DisplayEvent.MOUSE_PRESSED_LEFT) {
            if (e.getInputEvent().isControlDown()) {
                xmap.setRange(initialRangeX[0], initialRangeX[1]);
                ymap.setRange(initialRangeY[0], initialRangeY[1]);
            }
        }
        else if (e.getId() == DisplayEvent.MOUSE_RELEASED) {
            float val = getSelectorValue(channelSelector);
            if (val != waveNumber) {
                // TODO: setWaveNumber needs to be rethought, as it calls
                // setSelectorValue which is redundant in the cases of dragging
                // or clicking
                setWaveNumber(val);
                if (displayControl != null)
                    displayControl.handleChannelChange(val);
            }
        }
    }

    public DisplayableData getImageDisplay() {
        if (imageDisplay == null) {
            try {
                uniqueRangeType = RealType.getRealType(rangeType.getName()+"_"+cnt++);
                imageDisplay = new HydraRGBDisplayable("image", uniqueRangeType, null, true, displayControl);
            } catch (Exception e) {
                LogUtil.logException("MultiSpectralDisplay.getImageDisplay", e);
            }
        }
        return imageDisplay;
    }

    public float getWaveNumber() {
        return waveNumber;
    }

    public int getChannelIndex() throws Exception {
      return data.getChannelIndexFromWavenumber(waveNumber);
    }

    public void refreshDisplay() throws VisADException, RemoteException {
        if (display == null)
            return;

        synchronized (displayedThings) {
            for (DataReference ref : displayedThings) {
                display.removeReference(ref);
                display.addReference(ref, colorMaps.get(ref));
            }
        }
    }

    public boolean hasNullData() {
        try {
            synchronized (displayedThings) {
                for (DataReference ref : displayedThings) {
                    if (ref.getData() == null)
                        return true;
                }
            }
        } catch (Exception e) { }
        return false;
    }

    /** ID of the selector that controls the displayed channel. */
    private final String channelSelector = hashCode() + "_chanSelect";

    /** The map of selector IDs to selectors. */
    private final Map<String, DragLine> selectors = 
        new HashMap<String, DragLine>();

    public void showChannelSelector() {
        try {
            createSelector(channelSelector, Color.GREEN);
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralDisplay.showChannelSelector", e);
        }
    }

    public void hideChannelSelector() {
        try {
            DragLine selector = removeSelector(channelSelector);
            selector = null;
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralDisplay.hideChannelSelector", e);
        }
    }

    public DragLine createSelector(final String id, final Color color) throws Exception {
        if (id == null)
            throw new NullPointerException("selector id cannot be null");
        if (color == null)
            throw new NullPointerException("selector color cannot be null");
        return createSelector(id, makeColorMap(color));
    }

    public DragLine createSelector(final String id, final ConstantMap[] color) throws Exception {
        if (id == null)
            throw new NullPointerException("selector id cannot be null");
        if (color == null)
            throw new NullPointerException("selector color cannot be null");

        if (selectors.containsKey(id))
            return selectors.get(id);

        DragLine selector = new DragLine(this, id, color, initialRangeY);
        selector.setHydraControl(displayControl);
        selector.setSelectedValue(waveNumber);
        selectors.put(id, selector);
        return selector;
    }

    public DragLine getSelector(final String id) {
        return selectors.get(id);
    }

    public float getSelectorValue(final String id) {
        DragLine selector = selectors.get(id);
        if (selector == null)
            return Float.NaN;
        return selector.getSelectedValue();
    }

    public void setSelectorValue(final String id, final float value) 
        throws VisADException, RemoteException 
    {
        DragLine selector = selectors.get(id);
        if (selector != null)
            selector.setSelectedValue(value);
    }

    // BAD BAD BAD BAD
    public void updateControlSelector(final String id, final float value) {
        if (displayControl == null)
            return;
        if (displayControl instanceof LinearCombo) {
            ((LinearCombo)displayControl).updateSelector(id, value);
        } else if (displayControl instanceof HydraCombo) {
            ((HydraCombo)displayControl).updateComboPanel(id, value);
        }
    }

    public DragLine removeSelector(final String id) {
        DragLine selector = selectors.remove(id);
        if (selector == null)
            return null;
        selector.annihilate();
        return selector;
    }

    public List<DragLine> getSelectors() {
        return new ArrayList<DragLine>(selectors.values());
    }

    /**
     * @return Whether or not the channel selector is being displayed.
     */
    public boolean displayingChannel() {
        return (getSelector(channelSelector) != null);
    }

    public void removeRef(final DataReference thing) throws VisADException, 
        RemoteException 
    {
        if (display == null)
            return;

        synchronized (displayedThings) {
            displayedThings.remove(thing);
            colorMaps.remove(thing);
            idToRef.remove(thing.getName());
            display.removeReference(thing);
        }
    }

    public void addRef(final DataReference thing, final Color color) 
        throws VisADException, RemoteException 
    {
        if (display == null)
            return;

        synchronized (displayedThings) {
            ConstantMap[] colorMap = makeColorMap(color);

            displayedThings.add(thing);
            idToRef.put(thing.getName(), thing);
            ConstantMap[] constMaps;
            if (data.hasBandNames()) {
                constMaps = new ConstantMap[colorMap.length+2];
                System.arraycopy(colorMap, 0, constMaps, 0, colorMap.length);
                constMaps[colorMap.length] = new ConstantMap(1f, Display.PointMode);
                constMaps[colorMap.length+1] = new ConstantMap(5f, Display.PointSize);
            } else {
                constMaps = colorMap;
            }
            colorMaps.put(thing, constMaps);

            display.addReference(thing, constMaps);
        }
    }

    public void updateRef(final DataReference thing, final Color color)
        throws VisADException, RemoteException 
    {
        ConstantMap[] colorMap = makeColorMap(color);
        ConstantMap[] constMaps;
        if (data.hasBandNames()) {
            constMaps = new ConstantMap[colorMap.length+2];
            System.arraycopy(colorMap, 0, constMaps, 0, colorMap.length);
            constMaps[colorMap.length] = new ConstantMap(1f, Display.PointMode);
            constMaps[colorMap.length+1] = new ConstantMap(5f, Display.PointSize);
        } else {
            constMaps = colorMap;
        }
        colorMaps.put(thing, constMaps);
        idToRef.put(thing.getName(), thing);
        refreshDisplay();
    }

    public void reorderDataRefsById(final List<String> dataRefIds) {
        if (dataRefIds == null)
            throw new NullPointerException("");

        synchronized (displayedThings) {
            try {
                displayedThings.clear();
                for (String refId : dataRefIds) {
                    DataReference ref = idToRef.get(refId);
                    ConstantMap[] color = colorMaps.get(ref);
                    display.removeReference(ref);
                    display.addReference(ref, color);
                }
            } catch (Exception e) { }
        }
    }

    // TODO: needs work
    public boolean setWaveNumber(final float val) {
        if (data == null)
            return false;

        if (waveNumber == val)
            return true;

        try {
            if (spectrum == null) { 
              spectrum = data.getSpectrum(new int[] { 1, 1 });
            }

            Gridded1DSet domain = (Gridded1DSet)spectrum.getDomainSet();
            int[] idx = domain.valueToIndex(new float[][] { { val } });
            float[][] tmp = domain.indexToValue(idx);
            float channel = tmp[0][0];

            setSelectorValue(channelSelector, channel);

            imageExpired = true;
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralDisplay.setDisplayedWaveNum", e);
            return false;
        }

        waveNumber = val;

        if (data.hasBandNames()) {
            String name = data.getBandNameFromWaveNumber(waveNumber);
            bandSelectComboBox.setSelectedItem(name);
        }

        return true;
    }

    /**
     * @return The ConstantMap representation of <code>color</code>.
     */
    public static ConstantMap[] makeColorMap(final Color color)
        throws VisADException, RemoteException 
    {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;
        return new ConstantMap[] { new ConstantMap(r, Display.Red),
                                   new ConstantMap(g, Display.Green),
                                   new ConstantMap(b, Display.Blue),
                                   new ConstantMap(a, Display.Alpha) };
    }

    /**
     * Provides <code>master</code> some sensible default attributes.
     */
    private static void setDisplayMasterAttributes(final XYDisplay master) 
        throws VisADException, RemoteException 
    {
        master.showAxisScales(true);
        master.setAspect(2.5, 0.75);

        double[] proj = master.getProjectionMatrix();
        proj[0] = 0.35;
        proj[5] = 0.35;
        proj[10] = 0.35;

        master.setProjectionMatrix(proj);
    }

    /**
     * @return The minimum and maximum values found on the x-axis.
     */
    private static float[] getXRange(final Gridded1DSet domain) {
        return new float[] { domain.getLow()[0], domain.getHi()[0] };
    }

    public static RealType getRangeType(final FlatField spectrum) {
        return (((FunctionType)spectrum.getType()).getFlatRange().getRealComponents())[0];
    }

    private static RealType getDomainType(final FlatField spectrum) {
        return (((FunctionType)spectrum.getType()).getDomain().getRealComponents())[0];
    }

    private static class RubberBandBox extends CellImpl {

        private static final String RBB = "_rubberband";
        
        private DataReference rubberBand;

        private boolean init = false;

        private ScalarMap xmap;

        private ScalarMap ymap;

        public RubberBandBox(final MultiSpectralDisplay msd,
            final ScalarMap x, final ScalarMap y) throws VisADException,
            RemoteException 
        {
            RealType domainType = msd.getDomainType();
            RealType rangeType = msd.getRangeType();

            LocalDisplay display = msd.getDisplay();

            rubberBand = new DataReferenceImpl(hashCode() + RBB);
            rubberBand.setData(new RealTuple(new RealTupleType(domainType,
                rangeType), new double[] { Double.NaN, Double.NaN }));

            display.addReferences(new RubberBandBoxRendererJ3D(domainType,
                rangeType, 1, 1), new DataReference[] { rubberBand }, null);

            xmap = x;
            ymap = y;

            this.addReference(rubberBand);
        }

        public void doAction() throws VisADException, RemoteException {
            if (!init) {
                init = true;
                return;
            }

            Gridded2DSet set = (Gridded2DSet)rubberBand.getData();

            float[] low = set.getLow();
            float[] high = set.getHi();

            xmap.setRange(low[0], high[0]);
            ymap.setRange(low[1], high[1]);
        }
    }

    public static class DragLine extends CellImpl {
        private final String selectorId = hashCode() + "_selector";
        private final String lineId = hashCode() + "_line";
        private final String controlId;

        private ConstantMap[] mappings = new ConstantMap[5];

        private DataReference line;

        private DataReference selector;

        private MultiSpectralDisplay multiSpectralDisplay;
        
        private HydraControl hydraControl;

        private RealType domainType;
        private RealType rangeType;

        private RealTupleType tupleType;

        private LocalDisplay display;

        private float[] YRANGE;

        private float lastSelectedValue;

        public DragLine(final MultiSpectralDisplay msd, final String controlId, final Color color) throws Exception {
            this(msd, controlId, makeColorMap(color));
        }

        public DragLine(final MultiSpectralDisplay msd, final String controlId, final Color color, float[] YRANGE) throws Exception {
            this(msd, controlId, makeColorMap(color), YRANGE);
        }

        public DragLine(final MultiSpectralDisplay msd, final String controlId,
            final ConstantMap[] color) throws Exception
        {
            this(msd, controlId, color, new float[] {180f, 320f});
        }

        public DragLine(final MultiSpectralDisplay msd, final String controlId, 
            final ConstantMap[] color, float[] YRANGE) throws Exception 
        {
            if (msd == null)
                throw new NullPointerException("must provide a non-null MultiSpectralDisplay");
            if (controlId == null)
                throw new NullPointerException("must provide a non-null control ID");
            if (color == null)
                throw new NullPointerException("must provide a non-null color");

            this.controlId = controlId;
            this.multiSpectralDisplay = msd;
            this.YRANGE = YRANGE;
            lastSelectedValue = multiSpectralDisplay.getWaveNumber();

            for (int i = 0; i < color.length; i++) {
                mappings[i] = (ConstantMap)color[i].clone();
            }
            mappings[4] = new ConstantMap(-0.5, Display.YAxis);

            Gridded1DSet domain = multiSpectralDisplay.getDomainSet();

            domainType = multiSpectralDisplay.getDomainType();
            rangeType = multiSpectralDisplay.getRangeType();
            tupleType = new RealTupleType(domainType, rangeType);

            selector = new DataReferenceImpl(selectorId);
            line = new DataReferenceImpl(lineId);

            display = multiSpectralDisplay.getDisplay();

            display.addReferences(new GrabLineRendererJ3D(domain), new DataReference[] { selector }, new ConstantMap[][] { mappings });
            display.addReference(line, cloneMappedColor(color));

            addReference(selector);
        }

        private static ConstantMap[] cloneMappedColor(final ConstantMap[] color) throws Exception {
            assert color != null && color.length >= 3 : color;
            return new ConstantMap[] { 
                (ConstantMap)color[0].clone(),
                (ConstantMap)color[1].clone(),
                (ConstantMap)color[2].clone(),
            };
        }

        public void annihilate() {
            try {
                display.removeReference(selector);
                display.removeReference(line);
            } catch (Exception e) {
                LogUtil.logException("DragLine.annihilate", e);
            }
        }

        public String getControlId() {
            return controlId;
        }

        /**
         * Handles drag and drop updates.
         */
        public void doAction() throws VisADException, RemoteException {
            setSelectedValue(getSelectedValue());
        }

        public float getSelectedValue() {
            float val = (float)display.getDisplayRenderer().getDirectAxisValue(domainType);
            if (Float.isNaN(val))
                val = lastSelectedValue;
            return val;
        }

        public void setSelectedValue(final float val) throws VisADException,
            RemoteException 
        {
            // don't do work for stupid values
            if ((Float.isNaN(val)) 
                || (selector.getThing() != null && val == lastSelectedValue))
                return;

            line.setData(new Gridded2DSet(tupleType,
                new float[][] { { val, val }, { YRANGE[0], YRANGE[1] } }, 2));

            selector.setData(new Real(domainType, val));
            lastSelectedValue = val;
            
            if (hydraControl instanceof MultiSpectralControl) {
            	((MultiSpectralControl) hydraControl).setWavelengthLabel
            	(
            			MultiSpectralControl.WAVENUMLABEL + val
            	);
            }
            multiSpectralDisplay.updateControlSelector(controlId, val);
        }

		/**
		 * Set the display control so we can call back and update
		 * wavelength readout in real time.
		 * 
		 * @param hydraControl the display control to set
		 */
        
		public void setHydraControl(HydraControl hydraControl) {
			this.hydraControl = hydraControl;
		}
    }
}
