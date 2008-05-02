package edu.wisc.ssec.mcidasv.data;

import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.AddeServerInfo;

import edu.wisc.ssec.mcidasv.McIDASV;

import java.awt.*;
import java.awt.event.*;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.*;

import ucar.unidata.data.*;

import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.data.imagery.*;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.McIDASAreaProjection;
import ucar.unidata.geoloc.projection.ProjectionAdapter;

import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.geoloc.ProjectionManager;

import ucar.visad.data.AddeImageFlatField;

import visad.*;
import visad.data.*;
import visad.data.mcidas.*;
import visad.meteorology.*;

/**
 * Abstract DataSource class for images files.
 */
public abstract class Test2ImageDataSource extends ImageDataSource {

    /**
     * Public keys for server, group, dataset, user, project.
     */
    public final static String SIZE_KEY = "size";
    public final static String PLACE_KEY = "place";
    public final static String LATLON_KEY = "latlon";
    public final static String LINELE_KEY = "linele";
    public final static String MAG_KEY = "mag";

    /** The first projection we find */
    //protected McIDASVProjectionImpl sampleProjection;
    protected ProjectionImpl sampleProjection;

    /** My composite */
    private CompositeDataChoice myCompositeDataChoice;

    /** children choices */
    private List myDataChoices = new ArrayList();

    /** sequence manager for displaying data */
    private ImageSequenceManager sequenceManager;

    private McIDASVGeoSelectionPanel mcidasvGeoSelectionPanel;

    /** list of twod categories */
    private List twoDCategories;

    /** list of 2D time series categories */
    private List twoDTimeSeriesCategories;

    /** list of twod categories */
    private List bandCategories;

    /** list of 2D time series categories */
    private List bandTimeSeriesCategories;

    /* ADDE request string */
    private String source;

    /* properties for this data source */
    private Hashtable sourceProps;

    private int lineResolution;
    private int elementResolution;
    private int lineMag = 1;
    private int elementMag = 1;

    private LatLonRect lastLlr;


    public Test2ImageDataSource() {}


    /**
     * Create a new Test2ImageDataSource with a list of (String) images. These
     * can either be AREA files or ADDE URLs.
     *
     * @param descriptor       The descriptor for this data source.
     * @param images           Array of  file anmes or urls.
     * @param properties       The properties for this data source.
     */
    public Test2ImageDataSource(DataSourceDescriptor descriptor, String[] images,
                           Hashtable properties) {
        super(descriptor, images, properties);
    }


    /**
     * Create a new Test2ImageDataSource with a list of (String) images. These
     * can either be AREA files or ADDE URLs.
     *
     * @param descriptor       The descriptor for this data source.
     * @param images           Array of  file anmes or urls.
     * @param properties       The properties for this data source.
     */
    public Test2ImageDataSource(DataSourceDescriptor descriptor, List images,
                           Hashtable properties) {
        super(descriptor, images, properties);
    }




    /**
     * Create a new Test2ImageDataSource with the given {@link ImageDataset}.
     * The dataset may hold eight AREA file filepaths or ADDE URLs.
     *
     * @param descriptor    The descriptor for this data source.
     * @param ids           The dataset.
     * @param properties    The properties for this data source.
     */
    public Test2ImageDataSource(DataSourceDescriptor descriptor, ImageDataset ids,
                           Hashtable properties) {
        super(descriptor, ids, properties);
/*
        System.out.println("");
        System.out.println("Test2ImageDataSource:");
        System.out.println("    descriptor=" + descriptor);
        System.out.println("    ids=" + ids);
        System.out.println("    properties=" + properties);
        System.out.println("");
*/
        this.sourceProps = properties;
        List descs = ids.getImageDescriptors();
        AddeImageDescriptor aid = (AddeImageDescriptor)descs.get(0);
        source = aid.getSource();
        setMag();
        getAreaDirectory(properties);
        JTabbedPane testTab = new JTabbedPane();
    }

    private void setMag() {
        Object magKey = (Object)"mag";
        if (sourceProps.containsKey(magKey)) {
            String magVal = (String)(sourceProps.get(magKey));
            String[] magVals = magVal.split(" ");
            this.lineMag = new Integer(magVals[0]).intValue();
            this.elementMag = new Integer(magVals[1]).intValue();
        }
    }

    private void getAreaDirectory(Hashtable properties) {
        String addeCmdBuff = source;
        if (addeCmdBuff.contains("BAND=")) {
            String[] segs = addeCmdBuff.split("BAND=");
            String seg0 = segs[0];
            String seg1 = segs[1];
            int indx = seg1.indexOf("&");
            if (indx == 0) {
                addeCmdBuff = seg0 + "BAND=1" + seg1;
            }
        }
        //if (sourceProps.containsKey("mag")) {
            if (addeCmdBuff.contains("MAG=")) {
                String[] segs = addeCmdBuff.split("MAG=");
                String seg0 = segs[0];
                String seg1 = segs[1];
                int indx = seg1.indexOf("&");
                seg1 = seg1.substring(indx);
                //String magString = (String)sourceProps.get("mag");
                String magString = lineMag + " " + elementMag;
                addeCmdBuff = seg0 + "MAG=" + magString + seg1;
            }
        //}

        try {
            AreaFile af = new AreaFile(addeCmdBuff);
            AreaDirectory ad = af.getAreaDirectory();
            this.lineResolution = ad.getValue(11);
            this.elementResolution = ad.getValue(12);
            McIDASAreaProjection map = new McIDASAreaProjection(af);
            //System.out.println("map.getDefaultMapArea=" + map.getDefaultMapArea());
            //int lines = ad.getValue(8);
            //int elements = ad.getValue(9);
            //determineDefaultMapArea(map);
            sampleProjection = map;
        } catch (Exception e) {
            System.out.println("getAreaDirectory e=" + e);
        }
    }

    /**
     * Can this datasource do the geoselection subsetting and decimation
     *
     * @return _can do geo subsetting
     */
    public boolean canDoGeoSelection() {
        return true;
    }

    /**
     * Used for the geo subsetting property gui as to whether to
     * show the stride or not
     *
     * @return default is true
     */
    protected boolean canDoGeoSelectionStride() {
        return false;
    }

    /**
     * Used for the geo subsetting property gui as to whether to
     * show the map selection or not
     *
     * @return default is true
     */
    protected boolean canDoGeoSelectionMap() {
        return true;
    }

    /**
     * Return the sample projection
     *
     * @return the sample projection
     */
    protected ProjectionImpl getSampleDataProjection() {
        return sampleProjection;
    }

    /**
     * The user changed the properties. Update me.
     */
    protected void propertiesChanged() {
        PollingInfo pollingInfo = getPollingInfo();
        if (pollingInfo.doILookForNewFiles()) {
            List newSources = pollingInfo.getFiles();
            if (newSources.size() != imageList.size()) {
                initDataFromPollingInfo();
                dataChoices = null;
                notifyDataChange();
            }
        }
        super.propertiesChanged();
    }

    /**
     * A utility method that helps us deal with legacy bundles that used to
     * have String file names as the id of a data choice.
     *
     * @param object     May be an AddeImageDescriptor (for new bundles) or a
     *                   String that is converted to an image descriptor.
     * @return The image descriptor.
     */
    private AddeImageDescriptor getDescriptor(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof DataChoice) {
            object = ((DataChoice) object).getId();
        }
        if (object instanceof ImageDataInfo) {
            int index = ((ImageDataInfo) object).getIndex();
            if (index < myDataChoices.size()) {
                DataChoice dc        = (DataChoice) myDataChoices.get(index);
                Object     tmpObject = dc.getId();
                if (tmpObject instanceof ImageDataInfo) {
                    return ((ImageDataInfo) tmpObject).getAid();
                }
            }
            return null;
            //            return ((ImageDataInfo) object).getAid();
        }

        if (object instanceof AddeImageDescriptor) {
            return (AddeImageDescriptor) object;
        }
        return new AddeImageDescriptor(object.toString());
    }

    /**
     * This is used when we are unbundled and we may have different times than when we were saved.
     * Use the current set of data choices.
     *
     * @param compositeDataChoice The composite
     * @param dataChoices Its choices
     *
     * @return The  current choices
     */
    public List getCompositeDataChoices(
            CompositeDataChoice compositeDataChoice, List dataChoices) {
        //Force  creation of data choices
        getDataChoices();
        return !(hasBandInfo(compositeDataChoice))
               ? myDataChoices
               : dataChoices;
    }

    /**
     * Create the set of {@link ucar.unidata.data.DataChoice} that represent
     * the data held by this data source.  We create one top-level
     * {@link ucar.unidata.data.CompositeDataChoice} that represents
     * all of the image time steps. We create a set of children
     * {@link ucar.unidata.data.DirectDataChoice}, one for each time step.
     */
    public void doMakeDataChoices() {
        String type = (String) getProperty(PROP_IMAGETYPE, TYPE_SATELLITE);
        List<BandInfo> bandInfos =
            (List<BandInfo>) getProperty(PROP_BANDINFO, (Object) null);
        Hashtable props = Misc.newHashtable(DataChoice.PROP_ICON,
                                            (type.equals(TYPE_RADAR)
                                             ? "/auxdata/ui/icons/Radar.gif"
                                             : "/auxdata/ui/icons/Satellite.gif"));

        List categories = (imageList.size() > 1)
                          ? getTwoDTimeSeriesCategories()
                          : getTwoDCategories();

        // This is historical an is not added into the list of choices
        // for selection by the users.
        myCompositeDataChoice = new CompositeDataChoice(this, imageList,
                getName(), getDataName(), categories, props);
        myCompositeDataChoice.setUseDataSourceToFindTimes(true);
        doMakeDataChoices(myCompositeDataChoice);

        if ((bandInfos != null) && !bandInfos.isEmpty()) {
            List biCategories = (imageList.size() > 1)
                                ? getBandTimeSeriesCategories()
                                : getBandCategories();
            if (bandInfos.size() == 1) {
                BandInfo test  = (BandInfo) bandInfos.get(0);
                List     units = test.getCalibrationUnits();
                if ((units == null) || units.isEmpty()) {
                    return;
                }
            }
            for (Iterator<BandInfo> i = bandInfos.iterator(); i.hasNext(); ) {
                BandInfo bi      = i.next();
                String   name    = makeBandParam(bi);
                String   catName = bi.getBandDescription();
                List biSubCategories = Misc.newList(new DataCategory(catName,
                                           true));
                biSubCategories.addAll(biCategories);
                List l = bi.getCalibrationUnits();
                if (l.isEmpty() || (l.size() == 1)) {
                    DataChoice choice = new DirectDataChoice(this, bi, name,
                                            bi.getBandDescription(),
                                            biCategories, props);
                    addDataChoice(choice);
                } else {
                    for (int j = 0; j < l.size(); j++) {
                        Object   o           = l.get(j);
                        BandInfo bi2         = new BandInfo(bi);
                        String   calUnit     = o.toString();
                        String   calibration = TwoFacedObject.getIdString(o);
                        bi2.setPreferredUnit(calibration);
                        name = makeBandParam(bi2);
                        DataChoice subChoice = new DirectDataChoice(this,
                                                   bi2, name, calUnit,
                                                   biSubCategories, props);
                        addDataChoice(subChoice);
                    }
                }
            }
        } else {
            addDataChoice(myCompositeDataChoice);
        }
    }

    /**
     * Make the data choices and add them to the given composite
     *
     * @param composite The parent data choice to add to
     */
    private void doMakeDataChoices(CompositeDataChoice composite) {
        int cnt = 0;
        imageTimes = new ArrayList();
        List timeChoices = new ArrayList();
        myDataChoices = new ArrayList();

        String type = (String) getProperty(PROP_IMAGETYPE, TYPE_SATELLITE);
        Hashtable props = Misc.newHashtable(DataChoice.PROP_ICON,
                                            (type.equals(TYPE_RADAR)
                                             ? "/auxdata/ui/icons/clock.gif"
                                             : "/auxdata/ui/icons/clock.gif"));

        for (Iterator iter = imageList.iterator(); iter.hasNext(); ) {
            Object              object     = iter.next();
            AddeImageDescriptor aid        = getDescriptor(object);
            String              name       = aid.toString();
            DataSelection       timeSelect = null;
            if ( !aid.getIsRelative()) {
                DateTime imageTime = aid.getImageTime();
                if (imageTime != null) {
                    imageTimes.add(imageTime);
                    //timeSelect = new DataSelection (Misc.newList (imageTime));
                    //We will create the  data choice with an index, not with the actual time.
                    timeSelect =
                        new DataSelection(Misc.newList(new Integer(cnt)));
                }
            } else {
                imageTimes.add(getRelativeTimeObject(aid));
            }
            timeSelect = null;
            DataChoice choice = new DirectDataChoice(this,
                                    new ImageDataInfo(cnt, aid),
                                    composite.getName(), name,
                                    getTwoDCategories(), timeSelect, props);
            myDataChoices.add(choice);
            cnt++;
            timeChoices.add(choice);
        }
        //Sort the data choices.
        composite.replaceDataChoices(sortChoices(timeChoices));
    }

    /**
     * Return the list of {@link ucar.unidata.data.DataCategory} used for
     * single time step data.
     * 
     * @return A list of categories.
     */
    public List getTwoDCategories() {
        if (twoDCategories == null) {
            makeCategories();
        }
        return twoDCategories;
    }

    /**
     * Return the list of {@link ucar.unidata.data.DataCategory} used for
     * multiple time step data.
     * 
     * @return A list of categories.
     */

    public List getTwoDTimeSeriesCategories() {
        if (twoDCategories == null) {
            makeCategories();
        }
        return twoDTimeSeriesCategories;
    }


    /**
     * Initialize the {@link ucar.unidata.data.DataCategory} objects that
     * this data source uses. 
     */
    private void makeCategories() {
        twoDTimeSeriesCategories =
            DataCategory.parseCategories("IMAGE2-2D-TIME;", false);
        twoDCategories = DataCategory.parseCategories("IMAGE2-2D;", false);
        bandCategories = DataCategory.parseCategories("IMAGE2-BAND;", false);
        bandTimeSeriesCategories =
            DataCategory.parseCategories("IMAGE2-BAND-TIME;", false);

    }

    /**
     * Return the list of {@link ucar.unidata.data.DataCategory} used for
     * single time step data with band information.
     *
     * @return A list of categories.
     */
    public List getBandCategories() {
        if (bandCategories == null) {
            makeCategories();
        }
        return bandCategories;
    }

    /**
     * Return the list of {@link ucar.unidata.data.DataCategory} used for
     * multiple time step data with band information.
     *
     * @return A list of categories.
     */

    public List getBandTimeSeriesCategories() {
        if (bandTimeSeriesCategories == null) {
            makeCategories();
        }
        return bandTimeSeriesCategories;
    }


    /** _more_ */
    private Range[] sampleRanges = null;

    /**
     * Create the actual data represented by the given
     * {@link ucar.unidata.data.DataChoice}.
     *
     * @param dataChoice        Either the
     *                          {@link ucar.unidata.data.CompositeDataChoice}
     *                          representing all time steps or a
     *                          {@link ucar.unidata.data.DirectDataChoice}
     *                          representing a single time step.
     * @param category          Not really used.
     * @param dataSelection     Defines any time subsets.
     * @param requestProperties extra request properties
     *
     * @return The image or image sequence data.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        sampleRanges = null;

        GeoSelection geoSelection = dataSelection.getGeoSelection(true);
        GeoLocationInfo bbox = geoSelection.getBoundingBox();
        LatLonRect llr;
        if (bbox == null) {
            try {
                if (mcidasvGeoSelectionPanel == null) doMakeGeoSelectionPanel(false);
                llr = mcidasvGeoSelectionPanel.getLatLonRect();
                bbox = new GeoLocationInfo(llr);
                geoSelection.setBoundingBox(bbox);
                dataSelection.setGeoSelection(geoSelection);
            } catch (Exception e) {
                System.out.println("e=" + e);
                return null;
            }
        } else {
            llr = geoSelection.getLatLonRect();
        }
        if (lastLlr == null) lastLlr = llr;
        if (!llr.equals(lastLlr)) {
            Object sizeKey = (Object)"size";
            if (sourceProps.containsKey(sizeKey))
                sourceProps.remove(sizeKey);
        }
        dataSelection.setGeoSelection(geoSelection);
        lineMag = mcidasvGeoSelectionPanel.getLineMagnification();
        elementMag = mcidasvGeoSelectionPanel.getElementMagnification();

        if (dataChoice instanceof CompositeDataChoice) {
            return makeImageSequence(myCompositeDataChoice, dataSelection);
        } else if (hasBandInfo(dataChoice)) {
            return makeImageSequence(dataChoice, dataSelection);
        }
        return (Data) makeImage(dataChoice, dataSelection);
    }

    /**
     * Check if the DataChoice has a BandInfo for it's Id
     *
     * @param dataChoice  choice to check
     *
     * @return true if the choice ID is a BandInfo
     */
    private boolean hasBandInfo(DataChoice dataChoice) {
        return dataChoice.getId() instanceof BandInfo;
    }

    /** _more_ */
    String readLabel;
    AreaDirectory[][] currentDirs;

    /**
     * Create the  image sequence defined by the given dataChoice.
     *
     * @param dataChoice     The choice.
     * @param subset     any time subsets.
     * @return The image sequence.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected ImageSequence makeImageSequence(DataChoice dataChoice,
            DataSelection subset)
            throws VisADException, RemoteException {
        GeoSelection gs = subset.getGeoSelection();

        try {
            List descriptorsToUse = new ArrayList();
            if (hasBandInfo(dataChoice)) {
                descriptorsToUse = getDescriptors(dataChoice, subset);
            } else {
                List choices = (dataChoice instanceof CompositeDataChoice)
                               ? getChoicesFromSubset(
                                   (CompositeDataChoice) dataChoice, subset)
                               : Arrays.asList(new DataChoice[] {
                                   dataChoice });
                for (Iterator iter = choices.iterator(); iter.hasNext(); ) {
                    DataChoice          subChoice = (DataChoice) iter.next();
                    AddeImageDescriptor aid =
                        getDescriptor(subChoice.getId());
                    if (aid == null) {
                        continue;
                    }
                    DateTime dttm = aid.getImageTime();
                    if ((subset != null) && (dttm != null)) {
                        List times = getTimesFromDataSelection(subset,
                                         dataChoice);
                        if ((times != null) && (times.indexOf(dttm) == -1)) {
                            continue;
                        }
                    }
                    descriptorsToUse.add(aid);
                }
            }

            if (descriptorsToUse.size() == 0) {
                return null;
            }
            AddeImageInfo biggestPosition = null;
            int           pos             = 0;
            //Find the descriptor with the largets position
            for (Iterator iter =
                    descriptorsToUse.iterator(); iter.hasNext(); ) {
                AddeImageDescriptor aid = (AddeImageDescriptor) iter.next();
                AddeImageInfo       aii = aid.getImageInfo();

                //Are we dealing with area files here?
                if (aii == null) {
                    break;
                }

                //Check if this is absolute time
                if ((aii.getStartDate() != null)
                        || (aii.getEndDate() != null)) {
                    biggestPosition = null;
                    break;
                }
                if (Math.abs(aii.getDatasetPosition()) > pos) {
                    pos             = Math.abs(aii.getDatasetPosition());
                    biggestPosition = aii;
                }
            }

            if (getCacheDataToDisk() && (biggestPosition != null)) {
                biggestPosition.setRequestType(AddeImageInfo.REQ_IMAGEDIR);
                AreaDirectoryList adl =
                    new AreaDirectoryList(biggestPosition.getURLString());
                    //new AreaDirectoryList(biggestPosition.makeAddeUrl());
                biggestPosition.setRequestType(AddeImageInfo.REQ_IMAGEDATA);
                currentDirs = adl.getSortedDirs();
            } else {
                currentDirs = null;
            }

            if (sequenceManager == null) {
                sequenceManager = new ImageSequenceManager();
            }
            sequenceManager.clearSequence();
            ImageSequence sequence = null;
            int           cnt      = 1;
            DataChoice    parent   = dataChoice.getParent();
            for (Iterator iter =
                    descriptorsToUse.iterator(); iter.hasNext(); ) {
                AddeImageDescriptor aid = (AddeImageDescriptor) iter.next();
                if (currentDirs != null) {
                    int idx =
                        Math.abs(aid.getImageInfo().getDatasetPosition());
                    if (idx >= currentDirs.length) {
                        continue;
                    }
                }

                String label = "";
                if (parent != null) {
                    label = label + parent.toString() + " ";
                } else {
                    DataCategory displayCategory =
                        dataChoice.getDisplayCategory();
                    if (displayCategory != null) {
                        label = label + displayCategory + " ";
                    }
                }
                label = label + dataChoice.toString();
                readLabel = "Time: " + (cnt++) + "/"
                            + descriptorsToUse.size() + "  " + label;

                try {
                    SingleBandedImage image = makeImage(aid, true);
                    if (image != null) {
                        sequence = sequenceManager.addImageToSequence(image);
                    }
                } catch (VisADException ve) {
                    LogUtil.printMessage(ve.toString());
                }
            }
            return sequence;
        } catch (Exception exc) {
            throw new ucar.unidata.util.WrapperException(exc);
        }
    }

    /**
     * Create the single image defined by the given dataChoice.
     *
     * @param aid AddeImageDescriptor
     * @param fromSequence _more_
     *
     * @return The data.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private SingleBandedImage makeImage(AddeImageDescriptor aid,
                                        boolean fromSequence)
            throws VisADException, RemoteException {
        if (aid == null) {
            return null;
        }

        source = aid.getSource();
        Object magKey = (Object)"mag";
        boolean hasMag = sourceProps.containsKey(magKey);
        if (hasMag) {
            String magVal = (String)(sourceProps.get(magKey));
            if (source.contains("MAG=")) {
                String[] segs = source.split("MAG=");
                String seg0 = segs[0];
                String seg1 = segs[1];
                StringTokenizer tok = new StringTokenizer(seg1,"&");
                String mags = tok.nextToken();
                String[] magVals = mags.split(" ");
                this.lineMag = new Integer(magVals[0]).intValue();
                this.elementMag = new Integer(magVals[1]).intValue();
                int indx = seg1.indexOf("&");
                source = seg0 + "MAG=" + magVal + seg1.substring(indx);
                aid.setSource(source);
            }
        }

        SingleBandedImage result = (SingleBandedImage) getCache(source);
        if (result != null) {
            return result;
        }
        //For now handle non adde urls here
        try {
            if ( !source.startsWith("adde:")) {
                AreaAdapter aa = new AreaAdapter(source, false);
                result = aa.getImage();
                putCache(source, result);
                return result;
            }
            AddeImageInfo aii     = aid.getImageInfo();

            AreaDirectory areaDir = null;
            try {
                if (getCacheDataToDisk()) {
                    if (currentDirs != null) {
                        int    pos        =
                            Math.abs(aii.getDatasetPosition());
                        int    band       = 0;
                        String bandString = aii.getBand();
                        if ((bandString != null)
                                && !bandString.equals(aii.ALL)) {
                            band = new Integer(bandString).intValue();
                        }
                        //TODO: even though the band is non-zero we might only get back one band
                        band = 0;
                        areaDir =
                            currentDirs[currentDirs.length - pos - 1][band];
                    } else {
                        //If its absolute time then just use the AD from the descriptor
                        if ((aii.getStartDate() != null)
                                || (aii.getEndDate() != null)) {
                            areaDir = aid.getDirectory();
                        } else {
                        }
                    }
                }
            } catch (Exception exc) {
                LogUtil.printMessage("out looking up area dir");
                exc.printStackTrace();
                return null;
            }
            if ( !fromSequence) {
                areaDir = null;
            }
            if (areaDir != null) {
                //int hash = aii.makeAddeUrl().hashCode();
                int hash = aii.getURLString().hashCode();
                String filename = IOUtil.joinDir(getDataCachePath(),
                                      "image_" + hash + "_" + aii.getBand()
                                      + "_"
                                      + ((areaDir.getStartTime() != null)
                                         ? "" + areaDir.getStartTime()
                                             .getTime()
                                         : "") + ".dat");
                AddeImageFlatField aiff = AddeImageFlatField.create(aid,
                                              areaDir, getCacheDataToDisk(),
                                              filename, getCacheClearDelay());

                aiff.setReadLabel(readLabel);
                result = aiff;
                if (sampleRanges == null) {
                    sampleRanges = aiff.getRanges(true);
                    if ((sampleRanges != null) && (sampleRanges.length > 0)) {
                        for (int rangeIdx = 0; rangeIdx < sampleRanges.length;
                                rangeIdx++) {
                            Range r = sampleRanges[rangeIdx];
                            if (Double.isInfinite(r.getMin())
                                    || Double.isInfinite(r.getMax())) {
                                sampleRanges = null;
                                break;
                            }
                        }
                    }
                } else {
                    aiff.setSampleRanges(sampleRanges);
                }
            } else {
                AreaAdapter aa = new AreaAdapter(aid.getSource(), false);
                result = aa.getImage();
            }
            putCache(source, result);
//            sampleProjection = GridUtil.getNavigation((FieldImpl) result);
            return result;
        } catch (java.io.IOException ioe) {
            throw new VisADException("Creating AreaAdapter - " + ioe);
        }


    }


    /**
     * Make a parmeter name for the BandInfo
     *
     * @param bi    the BandInfo in question
     *
     * @return  a name for the parameter
     */
    private String makeBandParam(BandInfo bi) {
        StringBuffer buf = new StringBuffer();
        buf.append(bi.getSensor());
        buf.append("_Band");
        buf.append(bi.getBandNumber());
        buf.append("_");
        buf.append(bi.getPreferredUnit());
        return buf.toString();
    }

    /**
     * Get the object that we use to display relative time. Relative time is defined
     * using an integer index, 0...n. We don't want to show the actual integer.
     * Rather we want to show "Third most recent", "Fourth most recent", etc.
     *
     * @param aid The image descriptor
     * @return The object that represents the relative time index of the aid
     */
    private Object getRelativeTimeObject(AddeImageDescriptor aid) {
        return new TwoFacedObject(aid.toString(),
                                  new Integer(aid.getRelativeIndex()));
    }

    /**
     * Sort the list of data choices on their time
     *
     * @param choices The data choices
     *
     * @return The data choices sorted
     */
    private List sortChoices(List choices) {
        Object[]   choicesArray = choices.toArray();
        Comparator comp         = new Comparator() {
            public int compare(Object o1, Object o2) {
                AddeImageDescriptor aid1 = getDescriptor(o1);
                AddeImageDescriptor aid2 = getDescriptor(o2);
                if ((aid1 == null) || (aid2 == null)) {
                    return -1;
                }
                if (aid1.getIsRelative()) {
                    if (aid1.getRelativeIndex() < aid2.getRelativeIndex()) {
                        return 0;
                    } else if (aid1.getRelativeIndex()
                               == aid2.getRelativeIndex()) {
                        return 1;
                    }
                    return -1;
                }
                return aid1.getImageTime().compareTo(aid2.getImageTime());
            }
        };
        Arrays.sort(choicesArray, comp);
        return new ArrayList(Arrays.asList(choicesArray));

    }

    /**
     * Get a list of descriptors from the choice and subset
     *
     * @param dataChoice  Data choice
     * @param subset  subsetting info
     *
     * @return  list of descriptors matching the selection
     */
    private List getDescriptors(DataChoice dataChoice, DataSelection subset) {
        List times = getTimesFromDataSelection(subset, dataChoice);
        if ((times == null) || times.isEmpty()) {
            times = imageTimes;
        }
        List descriptors = new ArrayList();
        for (Iterator iter = times.iterator(); iter.hasNext(); ) {
            Object              time  = iter.next();
            AddeImageDescriptor found = null;
            for (Iterator iter2 = imageList.iterator(); iter2.hasNext(); ) {
                AddeImageDescriptor aid = getDescriptor(iter2.next());
                if (aid != null) {
                    if (aid.getIsRelative()) {
                        Object id = (time instanceof TwoFacedObject)
                                    ? ((TwoFacedObject) time).getId()
                                    : time;
                        if ((id instanceof Integer)
                                && ((Integer) id).intValue()
                                   == aid.getRelativeIndex()) {
                            found = aid;
                            break;
                        }

                    } else {
                        if (aid.getImageTime().equals(time)) {
                            found = aid;
                            break;
                        }
                    }

                }
            }
            if (found != null) {
                try {
                    AddeImageDescriptor desc = new AddeImageDescriptor(found);
                    //Sometimes we might have a null imageinfo
                    if(desc.getImageInfo()!=null) {
                        AddeImageInfo aii =
                            (AddeImageInfo) desc.getImageInfo().clone();
                        BandInfo bi = (BandInfo) dataChoice.getId();
                        List<BandInfo> bandInfos =
                            (List<BandInfo>) getProperty(PROP_BANDINFO, (Object) null);
                        boolean hasBand = true;
                        //If this data source has been changed after we have create a display 
                        //then the possibility exists that the bandinfo contained by the incoming
                        //data choice might not be valid. If it isn't then default to the first 
                        //one in the list
                        if(bandInfos!=null) {
                            hasBand = bandInfos.contains(bi);
                            if(!hasBand) {
                            }
                            if(!hasBand && bandInfos.size()>0) {
                                bi = bandInfos.get(0);
                            } else {
                                //Not sure what to do here.
                            }
                        }
                        GeoSelection geoSelection = subset.getGeoSelection(true);
                        GeoLocationInfo geoInfo = geoSelection.getBoundingBox();
                        aii.setBand("" + bi.getBandNumber());
                        aii.setUnit(bi.getPreferredUnit());
                        aii.setPlaceValue("ULEFT");
                        double[][] projCoords = new double[2][2];
                        if (geoInfo != null) {
                            LatLonPoint ulp = geoInfo.getUpperLeft();
                            LatLonPoint lrp = geoInfo.getLowerRight();
                            double lat = ulp.getLatitude();
                            double lon = ulp.getLongitude();
                            double[][] earthCoords = new double[2][2];
                            aii.setLocateKey("LATLON");
                            aii.setLocateValue(lat + " " + lon);
                            earthCoords[0][0] = lat;
                            earthCoords[1][0] = lon;
                            lat = lrp.getLatitude();
                            lon = lrp.getLongitude();
                            earthCoords[0][1] = lat;
                            earthCoords[1][1] = lon;
                            projCoords = sampleProjection.latLonToProj(earthCoords);
/*
                            System.out.println("------------------------------------------");
                            System.out.println("UL: " + earthCoords[0][0] + " " + earthCoords[1][0]);
                            System.out.println("LR: " + earthCoords[0][1] + " " + earthCoords[1][1]);
                            System.out.println("");
                            System.out.println("UL: " + projCoords[0][0] + " " + projCoords[1][0]);
                            System.out.println("LR: " + projCoords[0][1] + " " + projCoords[1][1]);
                            System.out.println("------------------------------------------");
*/
                        } else {
                            try {
                                AreaDirectory ad = desc.getDirectory();
                                double lin = (double)ad.getValue(5);
                                double ele = (double)ad.getValue(6);
                                aii.setLocateKey("LINELE");
                                aii.setLocateValue((int)lin + " " + (int)ele);
                                projCoords[0][0] = lin;
                                projCoords[1][0] = ele;
                                lin += (double)ad.getValue(8);
                                ele += (double)ad.getValue(9);
                                projCoords[0][1] = lin;
                                projCoords[1][1] = ele;
                            } catch (Exception e) {
                                System.out.println("exception e=" + e);
                                return descriptors;
                            }
                        }
                        int lins = Math.abs((int)(projCoords[1][1] - projCoords[1][0]));
                        int eles = Math.abs((int)(projCoords[0][1] - projCoords[0][0]));
                        lins *= this.lineResolution;

                        if (lineMag > 0) {
                            lins *= lineMag;
                        } else {
                            lins /= -lineMag;
                        }

                        eles *= this.elementResolution;

                        if (elementMag > 0) {
                            eles *= elementMag;
                        } else {
                            eles /= -elementMag;
                        }

                        aii.setLines(lins);
                        aii.setElements(eles);
                        desc.setImageInfo(aii);
                        desc.setSource(aii.getURLString());
                    }
                    descriptors.add(desc);
                } catch (CloneNotSupportedException cnse) {}
            }
        }
        return descriptors;
    }

    /**
     * Get the subset of the composite based on the selection
     *
     * @param choice  composite choice
     * @param subset  time selection
     *
     * @return subset list
     */
    private List getChoicesFromSubset(CompositeDataChoice choice,
                                      DataSelection subset) {
        List choices = choice.getDataChoices();
        if (subset == null) {
            return choices;
        }
        List times = subset.getTimes();
        if (times == null) {
            return choices;
        }
        times = TwoFacedObject.getIdList(times);
        List   subChoices = new ArrayList();
        Object firstTime  = times.get(0);
        if (firstTime instanceof Integer) {
            for (Iterator iter = times.iterator(); iter.hasNext(); ) {
                subChoices.add(
                    choices.get(((Integer) iter.next()).intValue()));
            }
        } else {  // TODO: what if they are DateTimes?
            subChoices.addAll(choices);
        }
        return subChoices;
    }

    /**
     * Make the geoselection panel
     *
     * @param forProperties   true if for the properties widget
     * @param geoSelection    geoselection to populate the panel
     *
     * @return the panel
     */

    public McIDASVGeoSelectionPanel doMakeGeoSelectionPanel(boolean forProperties,
            GeoSelection geoSelection) {
        Hashtable properties = this.sourceProps;
        List names = new ArrayList();
        List vals = new ArrayList();
        int propSize = properties.size();
        if (propSize > 0) {
            Enumeration eNum = properties.keys();
            for (int i=0; eNum.hasMoreElements(); i++) {
                names.add(eNum.nextElement().toString());
                vals.add(properties.get(names.get(i)).toString());
            }
        }
        ProjectionImpl sampleProjection = getSampleDataProjection();

        LatLonRect llr = new LatLonRect();
        if (names.size() > 2) llr = haveBoundingBox(names, vals);

        boolean enabled = true;
        forProperties = true;
        if (geoSelection == null) {
            GeoLocationInfo gli = new GeoLocationInfo(llr);
            geoSelection = new GeoSelection(gli);
        } else {
            enabled = false;
        }
        GeoLocationInfo gli = new GeoLocationInfo(llr);
        geoSelection.setBoundingBox(gli);
        McIDASVGeoSelectionPanel msp = new McIDASVGeoSelectionPanel(new GeoSelection(geoSelection),
                                     forProperties, enabled,
                                     canDoGeoSelectionStride(),
                                     canDoGeoSelectionMap(),
                                     (ProjectionImpl)sampleProjection,
                                     getExtraGeoSelectionComponent(),
                                     this.lineMag, this.elementMag);
        this.mcidasvGeoSelectionPanel = msp;
        return msp;
    }


    private LatLonRect haveBoundingBox(List keys, List strs) {
        ProjectionImpl pi = sampleProjection;
        ProjectionRect dma = pi.getDefaultMapArea();
        ProjectionPoint pp = dma.getUpperLeftPoint();
        pp = dma.getLowerRightPoint();
        double[][] dPts = new double[2][3];
        double[][] ePts = new double[2][3];
        double[][] aPts = new double[2][3];
        for (int i=0; i<2; i++) {
            for (int j=0; j<3; j++) {
                dPts[i][j] = (double)0.0;
                ePts[i][j] = (double)0.0;
                aPts[i][j] = (double)0.0;
            }
        }
        double centerY = dma.getCenterY();
        double centerX = dma.getCenterX();

        dPts[0][0] = centerY;
        dPts[1][0] = centerX;
        ePts = pi.projToLatLon(dPts);
        double centerLat = ePts[0][0];
        double centerLon = ePts[1][0];

        LatLonRect boundingBox = new LatLonRect();
        String place = "CENTER";
        double lat = centerLat;
        double lon = centerLon;
        int lin = (int)centerY;
        int ele = (int)centerX;
        int delY = 500;
        int delX = 500;
        for (int i=0; i<keys.size(); i++) {
            String key = (String)keys.get(i);
            if (key.equals("place")) {
                place = (String)strs.get(i);
            } else if (key.equals("latlon")) {
                String latlon = (String)strs.get(i);
                String[] vals = StringUtil.split(latlon, " ", 2);
                Double dVal = new Double(vals[0]);
                lat = dVal.doubleValue();
                dVal = new Double(vals[1]);
                lon = dVal.doubleValue();
            } else if (key.equals("linele")) {
                String linele = (String)strs.get(i);
                String[] vals = StringUtil.split(linele, " ", 2);
                Integer iVal = new Integer(vals[0]);
                lin = iVal.intValue();
                iVal = new Integer(vals[1]);
                ele = iVal.intValue();
            } else if (key.equals("size")) {
                String size = (String)strs.get(i);
                String[] vals = StringUtil.split(size, " ", 2);
                Integer iVal = new Integer(vals[0]);
                delY = iVal.intValue();
                iVal = new Integer(vals[1]);
                delX = iVal.intValue();
            }
        }
        if (sampleProjection != null) {
            if ((lat != 0.0) && (lon != 0.0)) {
                double deltax = delX;
                double deltay = delY;

                ePts[0][0] = (double)  47.9;
                ePts[0][1] = (double) 40.0;
                ePts[0][2] = (double) 33.4;
                ePts[1][0] = (double) -101.6;
                ePts[1][1] = (double) -90.0;
                ePts[1][2] = (double) -82.0;
                dPts = sampleProjection.latLonToProj(ePts);
/*
                System.out.println("------------------------------------------");
                System.out.println("UL: " + ePts[0][0] + " " + ePts[1][0]);
                System.out.println(" C: " + ePts[0][1] + " " + ePts[1][1]);
                System.out.println("LR: " + ePts[0][2] + " " + ePts[1][2]);
                System.out.println("");
                System.out.println("UL: " + dPts[0][0] + " " + dPts[1][0]);
                System.out.println(" C: " + dPts[0][1] + " " + dPts[1][1]);
                System.out.println("LR: " + dPts[0][2] + " " + dPts[1][2]);
                System.out.println("------------------------------------------");
*/
                ePts[0][1] = lat;
                ePts[1][1] = lon;
                dPts = sampleProjection.latLonToProj(ePts);
                if (place.equals("CENTER")) {
                    deltax /= 2.0;
                    deltay /= 2.0;

                    double centery = dPts[0][1];
                    double centerx = dPts[1][1];

                    dPts[1][0] = Math.abs(centerx - deltay);
                    dPts[0][0] = Math.abs(centery - deltax);
                    dPts[1][2] = Math.abs(centerx + deltay);
                    dPts[0][2] = Math.abs(centery + deltax);
                    ePts = sampleProjection.projToLatLon(dPts);
/*
                    dPts[0][0] = centery - deltay;
                    dPts[1][0] = centerx - deltax;
                    dPts[0][2] = centery + deltay;
                    dPts[1][2] = centerx + deltax;
                    ePts = sampleProjection.projToLatLon(dPts);
                    if (ePts[0][0] < ePts[0][2]) {
                        deltay *= -1;
                    }
                    if (ePts[1][0] > ePts[1][2]) {
                        deltax *= -1;
                    }
                    dPts[0][0] = centery - deltay;
                    dPts[1][0] = centerx - deltax;
                    dPts[0][2] = centery + deltay;
                    dPts[1][2] = centerx + deltax;
                    ePts = sampleProjection.projToLatLon(dPts);
*/
/*
                    System.out.println("------------------------------------------");
                    System.out.println("UL: " + ePts[0][0] + " " + ePts[1][0]);
                    System.out.println(" C: " + ePts[0][1] + " " + ePts[1][1]);
                    System.out.println("LR: " + ePts[0][2] + " " + ePts[1][2]);
                    System.out.println("");
                    System.out.println("UL: " + dPts[0][0] + " " + dPts[1][0]);
                    System.out.println(" C: " + dPts[0][1] + " " + dPts[1][1]);
                    System.out.println("LR: " + dPts[0][2] + " " + dPts[1][2]);
                    System.out.println("------------------------------------------");
*/
                    LatLonPoint right = new LatLonPointImpl(ePts[0][0], ePts[1][0]);
                    LatLonPoint left = new LatLonPointImpl(ePts[0][2], ePts[1][2]);
                    //System.out.println("    right=" + right);
                    //System.out.println("    left=" + left);
                    boundingBox = new LatLonRect(right, left);
                    //System.out.println("    boundingBox=" + boundingBox);
                }
            }
        }
        return boundingBox;
    }

    /**
     * Return the DataSelection for this DataSource.  The DataSelection
     * represents the default  criteria used for refining the getData calls.
     * For example, the user can set the date/times to be used for this
     * DataSource. This list of times is held in the DataSelection member.
     *
     * @return  the DataSelection for this DataSource
     */
/*
    public DataSelection getDataSelection() {
        DataSelection theDataSelection = new DataSelection();
        LatLonRect llr = getLatLonRect((McIDASAreaProjection)sampleProjection);
        GeoLocationInfo gli = new GeoLocationInfo(llr);
        GeoSelection gs = new GeoSelection(gli);
        theDataSelection.setGeoSelection(gs);
        return theDataSelection;
    }
*/

    private void determineDefaultMapArea(McIDASAreaProjection mcAProj) {
        //System.out.println("determineDefaultMapArea: mcAProj=" + mcAProj);
        int[] aDir = mcAProj.getDirBlock();
        double x1 = (double)0.0;
        double y1 = (double)0.0;
        double x2 = (double)(aDir[9]);
        double y2 = (double)(aDir[8]);
        //System.out.println("    x1=" + x1 + " y1=" + y1);
        //System.out.println("    x2=" + x2 + " y2=" + y2);
        ProjectionRect pr = new ProjectionRect(x1, y1, x2, y2);
        //System.out.println("---------- pr=" + pr);
        mcAProj.setDefaultMapArea(pr);
    }

    private LatLonRect getLatLonRect(McIDASAreaProjection map) {
        int[] aDir = map.getDirBlock();
        int lines = aDir[8];
        int elements = aDir[9];
        LatLonPoint lr = findFirst(map, lines, elements);
        LatLonPoint ul = findLast(map, lines, elements);
        return new LatLonRect(ul, lr);
    }


    private LatLonPoint findFirst(McIDASAreaProjection map, int lines, int elements) {
        double[][] linele = new double[2][1];
        for (int i=0; i<lines; i++) {
            linele[0][0] = (double)i;
            for (int j=0; j<elements; j++) {
                linele[1][0] = (double)j;
                double[][] result = new double[2][1];
                result = map.projToLatLon(linele, result);
                double lonVal = result[1][0];
                double latVal = result[0][0];
                if ((lonVal >= -180.0) && (lonVal <= 180.0)) {
                    if ((latVal >= -90.0) && (latVal <= 90.0)) {
                        return new LatLonPointImpl((double)j, (double)i);
                    }
                }
            }
        }
        System.out.println("findFirst: Valid lat/lon not found");
        return null;
    }

    private LatLonPoint findLast(McIDASAreaProjection map, int lines, int elements) {
        double[][] linele = new double[2][1];
        for (int i=lines-1; i>=0; i--) {
            linele[0][0] = (double)i;
            for (int j=elements-1; j>=0; j--) {
                linele[1][0] = (double)j;
                double[][] result = new double[2][1];
                result = map.projToLatLon(linele, result);
                double lonVal = result[1][0];
                double latVal = result[0][0];
                if ((lonVal >= -180.0) && (lonVal <= 180.0)) {
                    if ((latVal >= -90.0) && (latVal <= 90.0)) {
                        return new LatLonPointImpl((double)j, (double)i);
                    }
                }
            }
        }
        System.out.println("findLast: Valid lat/lon not found");
        return null;
    }


    private String rebuildSource(String source, Hashtable sourceProps) {
        String newSource = source;
        sourceProps.remove((Object)"bandinfo");
        sourceProps.remove((Object)"name");
        int propSize = sourceProps.size();
        if (propSize > 0) {
            Enumeration eNum = sourceProps.keys();
            for (int i=0; eNum.hasMoreElements(); i++) {
                String name = eNum.nextElement().toString();
                String val = (String)(sourceProps.get(name));
                name = name.toUpperCase();
                StringTokenizer tok = new StringTokenizer(source, "&");
                newSource = tok.nextToken();
                String next;
                while (tok.hasMoreElements()) {
                    next = (String)(tok.nextElement());
                    StringTokenizer tok2 = new StringTokenizer(next, "=");
                    String pName = (String)tok2.nextElement();
                    if (name.equals(pName)) {
                        newSource = newSource + "&" + pName + "=" + val;
                    } else {
                        newSource = newSource + "&" + next;
                    }
                }
            }
        }
        return newSource;
    }
}
