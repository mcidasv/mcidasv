package edu.wisc.ssec.mcidasv.display.hydra;

import java.awt.Color;
import java.awt.Component;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.wisc.ssec.mcidasv.control.HydraControl;
import edu.wisc.ssec.mcidasv.data.hydra.GrabLineRendererJ3D;
import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionDataSource;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;

import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.idv.ViewContext;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.util.LogUtil;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.XYDisplay;

import visad.CellImpl;
import visad.ConstantMap;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.Display;
import visad.DisplayEvent;
import visad.DisplayListener;
import visad.DisplayRealType;
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
import visad.java3d.DisplayImplJ3D;
import visad.java3d.TwoDDisplayRendererJ3D;
import visad.bom.RubberBandBoxRendererJ3D;

public class MultiSpectralDisplay implements DisplayListener {
    private static final String SELECTOR_ID = "line";
    private static final String DISP_NAME = "Spectrum";
    private static final String VIEW_NAME = "spectrum";
    private static final String VIEWMANAGER_PROPS = "showControlLegend=false;";

    private Color DEFAULT_FOREGROUND = Color.WHITE;
    private Color DEFAULT_BACKGROUND = Color.BLACK;

    private DirectDataChoice dataChoice;
    private ViewContext viewContext;

    private ViewManager viewManager;

    private float[] initialRangeX;
    private float[] initialRangeY = { 180f, 320f };

    private RealType domainType;
    private RealType rangeType;

    private ScalarMap xmap;
    private ScalarMap ymap;
    
    private LocalDisplay display;

    private FlatField image;
    private boolean imageExpired = true;

    private MultiSpectralData data;

    private float waveNumber = MultiSpectralData.init_wavenumber;

    private DataReference displayedChannel;

    private List<DataReference> displayedThings = new ArrayList<DataReference>();
    private HashMap<DataReference, ConstantMap[]> colorMaps = new HashMap<DataReference, ConstantMap[]>();

    private HydraControl displayControl;

    private DisplayableData imageDisplay = null;

    private XYDisplay master;

//    private DataReference channelSelector;
    
    public MultiSpectralDisplay(final HydraControl control) throws VisADException, RemoteException {
        displayControl = control;
        dataChoice = (DirectDataChoice)displayControl.getDataChoice();

        init();
    }

    public MultiSpectralDisplay(DirectDataChoice dataChoice)
             throws VisADException, RemoteException {
      this.dataChoice = dataChoice;

      init();
    }

    public FlatField getImageData() {
        try {
            if ((imageExpired) || (image == null)) {
                imageExpired = false;
                image = data.getImage(waveNumber, (HashMap)((MultiDimensionSubset)dataChoice.getDataSelection()).getSubset());
            }
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralDisplay.getImageData", e);
        }

        return image;
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

    private Gridded1DSet domainSet;

    public Gridded1DSet getDomainSet() {
        return domainSet;
    }

    private void init() throws VisADException, RemoteException {
        MultiDimensionDataSource source = (MultiDimensionDataSource)dataChoice.getDataSource();
        data = source.getMultiSpectralData();

        FlatField spectrum = null;
        try {
            spectrum = data.getSpectrum(new int[] { 1, 1 });
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralDisplay.init", e);
        }

//        Gridded1DSet domain = (Gridded1DSet)spectrum.getDomainSet();
        domainSet = (Gridded1DSet)spectrum.getDomainSet();
//        initialRangeX = getXRange((Gridded1DSet)spectrum.getDomainSet());
//        initialRangeX = getXRange(domain);
        initialRangeX = getXRange(domainSet);

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

        try {
            DragLine line1 = new DragLine(this);
        } catch (Exception e) {
            System.err.println("uh oh");
            e.printStackTrace();
        }
        new RubberBandBox(this, xmap, ymap);

        if (displayControl == null) { //- add in a ref for the default spectrum, ie no DisplayControl
          DataReferenceImpl spectrumRef = new DataReferenceImpl(hashCode() + "_spectrumRef");
          spectrumRef.setData(spectrum);
          addRef(spectrumRef, Color.GREEN);
        }
    }

    public void displayChanged(final DisplayEvent e) throws VisADException, RemoteException {
//        System.err.println("displayChanged: " + e);
        if (e.getId() == DisplayEvent.MOUSE_RELEASED_CENTER) {
            float val = (float)display.getDisplayRenderer().getDirectAxisValue(domainType);
            setWaveNumber(val);
            if (displayControl != null) {
              displayControl.handleChannelChange(val);
            }
        } 
        else if (e.getId() == DisplayEvent.MOUSE_PRESSED_LEFT) {
            if (e.getInputEvent().isShiftDown()) {
                xmap.setRange(initialRangeX[0], initialRangeX[1]);
                ymap.setRange(initialRangeY[0], initialRangeY[1]);
            }
        }
    }

    public DisplayableData getImageDisplay() {
        if (imageDisplay == null) {
            try {
                imageDisplay = new HydraRGBDisplayable("image", rangeType, null, true, displayControl);
            } catch (Exception e) {
                LogUtil.logException("MultiSpectralDisplay.getImageDisplay", e);
            }
        }
        return imageDisplay;
    }

    public float getWaveNumber() {
        return waveNumber;
    }

    public void refreshDisplay() throws VisADException, RemoteException {
        if (display == null)
            return;

        for (DataReference ref : displayedThings) {
            display.removeReference(ref);
            display.addReference(ref, colorMaps.get(ref));
        }
    }

    public void showChannelSelector() {
        if (displayedChannel != null)
            return;

        try {
            displayedChannel = new DataReferenceImpl(SELECTOR_ID);
            addRef(displayedChannel, Color.GREEN);
            moveChannelSelector(waveNumber);

//            channelSelector = new DragLine(this);
            moveChannelSelector2(waveNumber);
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralDisplay.showChannelSelector", e);
        }
    }

    public void hideChannelSelector() {
        if (displayedChannel == null)
            return;

        try {
            removeRef(displayedChannel);
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralDisplay.hideChannelSelector", e);
        }

        displayedChannel = null;
    }

    /**
     * @return Whether or not the channel selector is being displayed.
     */
    public boolean displayingChannel() {
        return (displayedChannel != null);
    }

    public void removeRef(final DataReference thing) throws VisADException, 
        RemoteException 
    {
        if (display == null)
            return;

        displayedThings.remove(thing);
        colorMaps.remove(thing);

        display.removeReference(thing);
    }

    public void addRef(final DataReference thing, final Color color) 
        throws VisADException, RemoteException 
    {
        if (display == null)
            return;

        ConstantMap[] colorMap = makeColorMap(color);

        displayedThings.add(thing);
        colorMaps.put(thing, colorMap);

        display.addReference(thing, colorMap);
    }

    public void updateRef(final DataReference thing, final Color color)
        throws VisADException, RemoteException 
    {
        ConstantMap[] colorMap = makeColorMap(color);
        colorMaps.put(thing, colorMap);
        refreshDisplay();
    }

    public boolean setWaveNumber(final float val) {
        if ((data == null))
            return false;

        if (waveNumber == val)
            return true;

        try {
            FlatField spectrum = null;

            spectrum = data.getSpectrum(new int[] { 1, 1 });

            Gridded1DSet domain = (Gridded1DSet)spectrum.getDomainSet();
            int[] idx = domain.valueToIndex(new float[][] { { val } });
            float[][] tmp = domain.indexToValue(idx);
            float channel = tmp[0][0];

            moveChannelSelector(channel);
            moveChannelSelector2(channel);

            imageExpired = true;
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralDisplay.setDisplayedWaveNum", e);
            return false;
        }

        waveNumber = val;

        return true;
    }

    /**
     * Moves the channel selector to the channel given by <code>val</code>.
     */
    protected void moveChannelSelector(final float val) throws VisADException, RemoteException {
        if (displayedChannel != null)
            displayedChannel.setData(new Gridded2DSet(new RealTupleType(domainType, rangeType), 
                new float[][] { { val, val }, { initialRangeY[0], initialRangeY[1] } }, 2));
    }
    
    protected void moveChannelSelector2(final float val) throws VisADException, RemoteException {
//        if (channelSelector == null)
//            return;
        
//        channelSelector.moveLine(val);
    }

    /**
     * Get the ConstantMap representation of <code>color</code>.
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

            rubberBand = new DataReferenceImpl("rubber band");
            rubberBand.setData(new RealTuple(new RealTupleType(domainType,
                rangeType), new double[] { Double.NaN, Double.NaN }));

            display.addReferences(new RubberBandBoxRendererJ3D(domainType,
                rangeType, 2, 2), new DataReference[] { rubberBand }, null);

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

    private static class DragLine extends CellImpl {
        private static final Color lineColor = Color.GRAY;
        private float[] initialRangeY = { 180f, 320f };

        private ConstantMap[] mappings = new ConstantMap[5];

        private DataReference line;

        private DataReference selector;

        private MultiSpectralDisplay multiSpectralDisplay;

        public DragLine(final MultiSpectralDisplay msd) throws Exception {
            multiSpectralDisplay = msd;
            ConstantMap[] tmp = MultiSpectralDisplay.makeColorMap(Color.GREEN);
            for (int i = 0; i < tmp.length; i++) {
                mappings[i] = tmp[i];
            }
            mappings[4] = new ConstantMap(-0.5, Display.YAxis);

            Gridded1DSet domain = multiSpectralDisplay.getDomainSet();

            float wave = multiSpectralDisplay.getWaveNumber();

            RealType domainType = multiSpectralDisplay.getDomainType();

            selector = new DataReferenceImpl(hashCode() + "_selector");
            selector.setData(new Real(domainType, wave));

            line = new DataReferenceImpl(hashCode() + "_line");

            LocalDisplay display = multiSpectralDisplay.getDisplay();
            display.addReferences(new GrabLineRendererJ3D(domain), new DataReference[] { selector }, new ConstantMap[][] { mappings });
            display.addReference(line, MultiSpectralDisplay.makeColorMap(lineColor));

            moveLine(wave);
            this.addReference(selector);
        }

        public void doAction() throws VisADException, RemoteException {
            LocalDisplay asdf = multiSpectralDisplay.getDisplay();
            RealType domainType = multiSpectralDisplay.getDomainType();

            float val = (float)asdf.getDisplayRenderer().getDirectAxisValue(domainType);
            moveLine(val);
        }

        public void moveLine(final float val) throws VisADException, RemoteException {
//            System.err.println("moveLine: val=" + val);
            RealType domainType = multiSpectralDisplay.getDomainType();
            RealType rangeType = multiSpectralDisplay.getRangeType();
            line.setData(new Gridded2DSet(new RealTupleType(domainType, rangeType), new float[][] { { val, val }, { initialRangeY[0], initialRangeY[1] } }, 2));
        }
    }
}
