/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.mcidasv.data.adde;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.RandomAccessFile;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaDirectoryList;
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.AreaFileException;
import edu.wisc.ssec.mcidas.adde.AddeImageURL;
import edu.wisc.ssec.mcidas.adde.AddeTextReader;
import edu.wisc.ssec.mcidas.adde.AddeURL;
import edu.wisc.ssec.mcidasv.data.GeoLatLonSelection;
import edu.wisc.ssec.mcidasv.data.GeoPreviewSelection;
import visad.CommonUnit;
import visad.Data;
import visad.DateTime;
import visad.FlatField;
import visad.FunctionType;
import visad.MathType;
import visad.RealType;
import visad.Set;
import visad.VisADException;
import visad.data.DataRange;
import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.AreaAdapter;
import visad.georef.MapProjection;
import visad.meteorology.ImageSequence;
import visad.meteorology.ImageSequenceImpl;
import visad.meteorology.SingleBandedImage;
import ucar.nc2.iosp.mcidas.McIDASAreaProjection;
import ucar.unidata.data.BadDataException;
import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.imagery.AddeImageDataSource;
import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.data.imagery.BandInfo;
import ucar.unidata.data.imagery.ImageDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.ThreeDSize;
import ucar.unidata.util.TwoFacedObject;
import ucar.visad.UtcDate;
import ucar.visad.Util;
import ucar.visad.data.AreaImageFlatField;

/**
 * Abstract DataSource class for images files.
 */

public class AddeImageParameterDataSource extends AddeImageDataSource {

    private static final Logger logger = LoggerFactory.getLogger(AddeImageParameterDataSource.class);

    /**
     * Public keys for server, group, dataset, user, project.
     */
    public final static String SIZE_KEY = "size";
    public final static String PLACE_KEY = "place";
    public final static String LATLON_KEY = "latlon";
    public final static String LINELE_KEY = "linele";
    public final static String MAG_KEY = "mag";
    public final static String BAND_KEY = "band";
    public final static String BANDINFO_KEY = "bandinfo";
    public final static String UNIT_KEY = "unit";
    public final static String PREVIEW_KEY = "preview";
    public final static String SPAC_KEY = "spac";
    public final static String NAV_KEY = "nav";
    public final static String AUX_KEY = "aux";
    public final static String DOC_KEY = "doc";
    public final static String SPACING_BRIT = "1";
    public final static String SPACING_NON_BRIT = "4";

    /** The first projection we find */
//    protected ProjectionImpl sampleProjection;
    public MapProjection sampleMapProjection;

    /* ADDE request string */
    private String source;
    private String baseSource;

    /* properties for this data source */
    private Hashtable sourceProps;
    private Hashtable selectionProps;

    private int lineResolution;
    private int elementResolution;
    private float lRes;
    private float eRes;
    private int lineMag = 1;
    private int elementMag = 1;

    private GeoSelection lastGeoSelection;
    private DataChoice lastChoice = null;
    private Boolean showPreview = Boolean.FALSE;
    private FlatField previewImage = null;
    private MapProjection previewProjection;
    private Hashtable initProps;

    private AreaDirectory previewDir = null;
    private AREAnav previewNav = null;
    private boolean haveDataSelectionComponents = false;

    private GeoPreviewSelection previewSel;
    private GeoLatLonSelection laLoSel;

    private String choiceName;

    private String saveCoordType;
    private String savePlace;
    private double saveLat;
    private double saveLon;
    private int saveNumLine;
    private int saveNumEle;
    private int saveLineMag;
    private int saveEleMag;
    private Boolean saveShowPreview;

    private String displaySource;

    // keep track of extra info for derived fields, since they may be a mix of bands and resolutions
    boolean isDerived = false;
    HashMap<Integer, Double> derivedBandLineRes = new HashMap<Integer, Double>();
    HashMap<Integer, Double> derivedBandElemRes = new HashMap<Integer, Double>();
    HashMap<Integer, Integer> derivedBandMagFactor = new HashMap<Integer, Integer>();

    protected List<DataChoice> stashedChoices = null;
    private List iml = new ArrayList();
    private List saveImageList = new ArrayList();

    private int previewLineRes = 1;
    private int previewEleRes = 1;
    
    // Do binary search on preview bounds for GEO sensors to speed up overall response time
    boolean isGeoSensor = false;

    // Also need to know when dealing with a sensor where sectors shift around spatially
    boolean isABISensor = false;

    /** Whether or not this DataSource was loaded from a bundle. */
    private boolean fromBundle = false;
    
    /** Are any of the data choices based upon remote files? */
    private boolean hasRemoteChoices = false;

    private Map<String, AreaDirectory> requestIdToDirectory = new HashMap<String, AreaDirectory>();
    
    // TJJ Aug 2020
    // Detect domain shift - for ex ABI MESOs can abruptly move
    // If we detect one, load the later, shifted image(s) earth-centered relative to the earlier
    boolean domainShiftDetected = false;
    boolean domainShiftNoticeDerivedShown = false;
    boolean domainShiftNoticeTargetShown = false;

    public AddeImageParameterDataSource() {} 

    /**
     * Creates a {@code AddeImageParameterDataSource} with a single ADDE URL.
     * <b>Note:</b> the URLs should point at {@literal "image"} data.
     * 
     * @param descriptor {@link ucar.unidata.data.DataSourceDescriptor DataSourceDescriptor} for this data source.
     * @param image ADDE URL
     * @param properties The properties for this data source.
     * 
     * @throws VisADException
     */
    public AddeImageParameterDataSource(DataSourceDescriptor descriptor, String image,
                               Hashtable properties)
            throws VisADException {
        super(descriptor, new String[] { image }, properties);
        logger.trace("1: desc={}, image={}, properties={}", new Object[] { descriptor, image, properties });
    }

    /**
     * Create a new AddeImageParameterDataSource with an array of ADDE URL strings.
     * <b>Note:</b> the URLs should point at {@literal "image"} data.
     * 
     * @param descriptor {@link ucar.unidata.data.DataSourceDescriptor DataSourceDescriptor} for this data source.
     * @param images Array of ADDE URLs.
     * @param properties Properties for this data source.
     * 
     * @throws VisADException
     */
    public AddeImageParameterDataSource(DataSourceDescriptor descriptor, String[] images,
                           Hashtable properties) throws VisADException {
        super(descriptor, images, properties);
        logger.trace("2: desc={}, images={}, properties={}", new Object[] { descriptor, images, properties });
    }

    /**
     * Creates a new {@code AddeImageParameterDataSource} with an 
     * {@link java.util.List List} of ADDE URL strings.
     * <b>Note:</b> the URLs should point at {@literal "image"} data.
     * 
     * @param descriptor {@link ucar.unidata.data.DataSourceDescriptor DataSourceDescriptor} for this data source.
     * @param images {@code List} of ADDE URL strings.
     * @param properties Properties for this data source.
     * 
     * @throws VisADException
     */
    public AddeImageParameterDataSource(DataSourceDescriptor descriptor, List images,
                           Hashtable properties) throws VisADException {
        super(descriptor, images, properties);
        logger.trace("3: desc={}, images={}, properties={}", new Object[] { descriptor, images, properties });
    }

    /**
     * Create a new AddeImageParameterDataSource with the given dataset.
     * 
     * @param descriptor {@link ucar.unidata.data.DataSourceDescriptor DataSourceDescriptor} for this data source.
     * @param ids Dataset.
     * @param properties Properties for this data source.
     * 
     * @throws VisADException
     */
    
    public AddeImageParameterDataSource(DataSourceDescriptor descriptor, ImageDataset ids,
                           Hashtable properties) throws VisADException {
        super(descriptor, ids, properties);
        logger.trace("4: desc={}, ids={}, properties={}", new Object[] { descriptor, ids, properties });
        this.sourceProps = properties;
        if (properties.containsKey((Object)PREVIEW_KEY)) {
            this.showPreview = (Boolean)(properties.get((Object)PREVIEW_KEY));
            saveShowPreview = showPreview;
        } else {
            if (saveShowPreview != null) {
                showPreview = saveShowPreview;
            }
        }
        
        List descs = ids.getImageDescriptors();
        AddeImageDescriptor aid = (AddeImageDescriptor)descs.get(0);
        this.source = aid.getSource();
        if (this.source.contains("localhost")) {
            AreaDirectory areaDirectory = aid.getDirectory();
            if (!sourceProps.containsKey((Object)UNIT_KEY)) {
                if (!sourceProps.containsKey((Object)BAND_KEY)) {
                    String calType = areaDirectory.getCalibrationType();
                    if (!calType.equals("RAW")) {
                        sourceProps.put(UNIT_KEY, calType);
                        int[] bandNums = areaDirectory.getBands();
                        String bandString = Integer.valueOf(bandNums[0]).toString();
                        sourceProps.put(BAND_KEY, bandString);
                    }
                }
            }
        }
        setMag();
        getAreaDirectory(properties);
    }

    @Override protected void propertiesChanged() {
        logger.trace("fired");
        super.propertiesChanged();
    }

    @Override protected boolean initDataFromPollingInfo() {
        boolean result = super.initDataFromPollingInfo();
        logger.trace("result={}", result);
        return result;
    }

    @Override protected boolean isPolling() {
        boolean result = super.isPolling();
        logger.trace("isPolling={}", result);
        return result;
    }

    @Override public void setPollingInfo(PollingInfo value) {
        logger.trace("value={}", value);
        super.setPollingInfo(value);
    }

    @Override protected boolean hasPollingInfo() {
        boolean result = super.hasPollingInfo();
        logger.trace("hasPollingInfo={}", result);
        return result;
    }

    @Override public PollingInfo getPollingInfo() {
        PollingInfo result = super.getPollingInfo();
        logger.trace("getPollingInfo={}", result);
        return result;
    }

    @Override public void initAfterUnpersistence() {
        logger.trace("unbundled!");
        super.initAfterUnpersistence();

        if (this.sourceProps.containsKey(PREVIEW_KEY)) {
            this.showPreview = (Boolean)this.sourceProps.get(PREVIEW_KEY);
            if (this.showPreview == null) {
                this.showPreview = Boolean.FALSE;
            }
            this.saveShowPreview = this.showPreview;
        }
        
        this.fromBundle = true;
        List<AddeImageDescriptor> descriptors = (List<AddeImageDescriptor>) getImageList();
        this.source = descriptors.get(0).getSource(); // TODO: why not use the source from
                                                      // each AddeImageDescriptor?
        for (AddeImageDescriptor descriptor : descriptors) {
            if (!isFromFile(descriptor)) {
                this.hasRemoteChoices = true;
                break;
            }
        }
    }

    @Override public boolean canSaveDataToLocalDisk() {
        return true;
    }

    private Hashtable<String, DataSelection> choiceToSel = new Hashtable<String, DataSelection>();

    public DataSelection getSelForChoice(final DataChoice choice) {
        String key = choice.getName();
        return choiceToSel.get(key);
    }
    public boolean hasSelForChoice(final DataChoice choice) {
        String key = choice.getName();
        return choiceToSel.containsKey(key);
    }
    public void putSelForChoice(final DataChoice choice, final DataSelection sel) {
        String key = choice.getName();
        choiceToSel.put(key, sel);
    }

    /**
     * Save files to local disk
     *
     * @param prefix destination dir and file prefix
     * @param loadId For JobManager
     * @param changeLinks Change internal file references
     *
     * @return Files copied
     *
     * @throws Exception On badness
     */
    
    @Override protected List saveDataToLocalDisk(String prefix, Object loadId, boolean changeLinks) throws Exception {
        logger.trace("prefix={} loadId={} changeLinks={}", new Object[] { prefix, loadId, changeLinks });
        final List<JCheckBox> checkboxes = new ArrayList<JCheckBox>();
        List categories = new ArrayList();
        Hashtable catMap = new Hashtable();
        Hashtable currentDataChoices = new Hashtable();

        List displays = getIdv().getDisplayControls();
        for (int i = 0; i < displays.size(); i++) {
            List dataChoices = ((DisplayControl)displays.get(i)).getDataChoices();
            if (dataChoices == null) {
                continue;
            }
            List finalOnes = new ArrayList();
            for (int j = 0; j < dataChoices.size(); j++) {
                ((DataChoice)dataChoices.get(j)).getFinalDataChoices(finalOnes);
            }
            for (int dcIdx = 0; dcIdx < finalOnes.size(); dcIdx++) {
                DataChoice dc = (DataChoice)finalOnes.get(dcIdx);
                if (!(dc instanceof DirectDataChoice)) {
                    continue;
                }
                DirectDataChoice ddc = (DirectDataChoice) dc;
                if (ddc.getDataSource() != this) {
                    continue;
                }
                currentDataChoices.put(ddc.getName(), "");
            }
        }

        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice dataChoice = (DataChoice) dataChoices.get(i);
            if (!(dataChoice instanceof DirectDataChoice)) {
                continue;
            }

            // skip over datachoices that the user has not already loaded.
            // (but fill the "slot" with null (it's a hack to signify that 
            // the "download" loop should skip over the data choice associated 
            // with this slot)
            if (!currentDataChoices.containsKey(dataChoice.getName())) {
                checkboxes.add(null); // 
                continue;
            }

            String label = dataChoice.getDescription();
            if (label.length() > 30) {
                label = label.substring(0, 29) + "...";
            }
            JCheckBox cbx =
                new JCheckBox(label, 
                              currentDataChoices.get(dataChoice.getName())
                              != null);
            ThreeDSize size = (ThreeDSize)dataChoice.getProperty(SIZE_KEY);
            cbx.setToolTipText(dataChoice.getName());
            checkboxes.add(cbx);
            DataCategory dc = dataChoice.getDisplayCategory();
            if (dc == null) {
                dc = DataCategory.createCategory(DataCategory.CATEGORY_IMAGE);
            }
            List comps = (List)catMap.get(dc);
            if (comps == null) {
                comps = new ArrayList();
                catMap.put(dc, comps);
                categories.add(dc);
            }
            comps.add(cbx);
            comps.add(GuiUtils.filler());
            if (size != null) {
                JLabel sizeLabel = GuiUtils.rLabel(size.getSize() + "  ");
                sizeLabel.setToolTipText(size.getLabel());
                comps.add(sizeLabel);
            } else {
                comps.add(new JLabel(""));
            }
        }
        final JCheckBox allCbx = new JCheckBox("Select All");
        allCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (JCheckBox cbx : checkboxes) {
                    if (cbx != null) {
                        cbx.setSelected(allCbx.isSelected());
                    }
                }
            }
        });

        JTabbedPane tab = new JTabbedPane(JTabbedPane.LEFT);

        for (int i = 0; i < categories.size(); i++) {
            List comps = (List) catMap.get(categories.get(i));
            JPanel innerPanel = GuiUtils.doLayout(comps, 3, GuiUtils.WT_NYN, GuiUtils.WT_N);
            JScrollPane sp = new JScrollPane(GuiUtils.top(innerPanel));
            sp.setPreferredSize(new Dimension(500, 400));
            JPanel top = GuiUtils.right(GuiUtils.rLabel("  "));
            JComponent inner = GuiUtils.inset(GuiUtils.topCenter(top, sp), 5);
            tab.addTab(categories.get(i).toString(), inner);
        }

        JComponent contents = tab;
        contents = GuiUtils.topCenter(
            GuiUtils.inset(
                GuiUtils.leftRight(
                    new JLabel("Select the fields to download"),
                    allCbx), 5), contents);
        JLabel label = new JLabel(getNameForDataSource(this, 50, true));
        contents = GuiUtils.topCenter(label, contents);
        contents = GuiUtils.inset(contents, 5);
        if (!GuiUtils.showOkCancelDialog(null, "", contents, null)) {
            return null;
        }

        // iterate through user's selection to build list of things to download
        List<String> realUrls = new ArrayList<String>();
        List<AddeImageDescriptor> descriptorsToSave = new ArrayList<AddeImageDescriptor>();
        List<BandInfo> bandInfos = (List<BandInfo>)getProperty(PROP_BANDINFO, (Object)null);
        List<BandInfo> savedBands = new ArrayList<BandInfo>();
        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice dataChoice = (DataChoice)dataChoices.get(i);
            if (!(dataChoice instanceof DirectDataChoice)) {
                continue;
            }
            JCheckBox cbx = (JCheckBox)checkboxes.get(i);
            if (cbx == null || !cbx.isSelected()) {
                continue;
            }

            if (dataChoice.getDataSelection() == null) {
                dataChoice.setDataSelection(getSelForChoice(dataChoice));
            }
            logger.trace("selected choice={} id={}", dataChoice.getName(), dataChoice.getId());
            List<AddeImageDescriptor> descriptors = getDescriptors(dataChoice, dataChoice.getDataSelection());
            logger.trace("descriptors={}", descriptors);
            
            BandInfo bandInfo;
            Object dataChoiceId = dataChoice.getId();
            if (dataChoiceId instanceof BandInfo) {
                bandInfo = (BandInfo)dataChoiceId;
            } else {
                bandInfo = bandInfos.get(0);
            }
            String preferredUnit = bandInfo.getPreferredUnit();
            List<TwoFacedObject> filteredCalUnits = new ArrayList<TwoFacedObject>();
            for (TwoFacedObject tfo : (List<TwoFacedObject>)bandInfo.getCalibrationUnits()) {
                if (preferredUnit.equals(tfo.getId())) {
                    filteredCalUnits.add(tfo);
                }
            }
            bandInfo.setCalibrationUnits(filteredCalUnits);
            savedBands.add(bandInfo);

            DataSelection selection = dataChoice.getDataSelection();
            if (selection == null) {
                if (getSelForChoice(dataChoice) != null) {
                    selection = getSelForChoice(dataChoice);
                } else {
                    selection = getDataSelection();
                }
            }

            Hashtable selectionProperties = selection.getProperties();
//            Hashtable selectionProperties;
//            if (selection != null) {
//                selectionProperties = selection.getProperties();
//            } else {
//                DataSelection sel = this.getDataSelection();
//                selectionProperties = new Hashtable();
//            }
            logger.trace("bandinfo.getUnit={} selection props={}", bandInfo.getPreferredUnit(), selectionProperties);
            for (AddeImageDescriptor descriptor : descriptors) {
//                AddeImageInfo aii = (AddeImageInfo)descriptor.getImageInfo().clone();
                if (!isFromFile(descriptor)) {
                    String src = descriptor.getSource();
                    logger.trace("src before={}", src);
                    src = replaceKey(src, AddeImageURL.KEY_UNIT, bandInfo.getPreferredUnit());
                    if (selectionProperties.containsKey(AddeImageURL.KEY_PLACE)) {
                        src = replaceKey(src, AddeImageURL.KEY_PLACE, selectionProperties.get(AddeImageURL.KEY_PLACE));
                    }
                    if (selectionProperties.containsKey(AddeImageURL.KEY_LATLON)) {
                        src = replaceKey(src, AddeImageURL.KEY_LINEELE, AddeImageURL.KEY_LATLON, selectionProperties.get(AddeImageURL.KEY_LATLON));
                    }
                    if (selectionProperties.containsKey(AddeImageURL.KEY_LINEELE)) {
                        src = removeKey(src, AddeImageURL.KEY_LATLON);
                        src = replaceKey(src, AddeImageURL.KEY_LINEELE, selectionProperties.get(AddeImageURL.KEY_LINEELE));
                    }
                    if (selectionProperties.containsKey(AddeImageURL.KEY_MAG)) {
                        src = replaceKey(src, AddeImageURL.KEY_MAG, selectionProperties.get(AddeImageURL.KEY_MAG));
                    }
                    if (selectionProperties.containsKey(AddeImageURL.KEY_SIZE)) {
                        src = replaceKey(src, AddeImageURL.KEY_SIZE, selectionProperties.get(AddeImageURL.KEY_SIZE));
                    }
                    logger.trace("src after={}", src);
                    descriptor.setSource(src);
                }
                descriptorsToSave.add(descriptor);
            }
//          descriptorsToSave.addAll(descriptors);
        }
        if (!savedBands.isEmpty()) {
            setProperty(PROP_BANDINFO, savedBands);
        }
        if (descriptorsToSave.isEmpty()) {
            return null;
        }

        // Start the load, showing the dialog
        List<String> suffixes = new ArrayList<String>();
        SimpleDateFormat sdf = new SimpleDateFormat("_" + DATAPATH_DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        for (int i = 0; i < descriptorsToSave.size(); i++) {
            AddeImageDescriptor descriptor = descriptorsToSave.get(i);
            AddeImageInfo aii = descriptor.getImageInfo();
            DateTime dttm = (DateTime)timeMap.get(descriptor.getSource());
            if (dttm != null) {
                suffixes.add(sdf.format(ucar.visad.Util.makeDate(dttm)) + ".area");
            } else if (aii != null) {
                String suffix = "_Band"+aii.getBand()+"_Unit"+aii.getUnit()+"_Pos"+i+".area";
                suffixes.add(suffix);
                logger.trace("test suffix={}", suffix);
            } else {
                suffixes.add(i + ".area");
            }
            realUrls.add(descriptor.getSource());
        }
        logger.trace("urls={}", realUrls);
        logger.trace("prefix={}", prefix);
        logger.trace("suffixes={}", suffixes);
        logger.trace("loadId={}", loadId);
        List newFiles = IOUtil.writeTo(realUrls, prefix, suffixes, loadId);
        logger.trace("files={}", newFiles);
        if (newFiles == null) {
            logger.trace("failed while in writeTo?");
            return null;
        } else {
            logger.trace("finished writeTo!");
        }
        if (changeLinks) {
            imageList = newFiles;
        }

        // write 0 as the first word
        for (int i = 0; i < newFiles.size(); i++) {
            try {
                RandomAccessFile to = new RandomAccessFile((String)newFiles.get(i), "rw");
                to.seek(0);
                to.writeInt(0);
                to.close();
            } catch (Exception e) {
                logger.error("unable to set first word to zero", e);
            }
        }


//        if (geoSubset != null) {
//            geoSubset.clearStride();
//            geoSubset.setBoundingBox(null);
//            if (geoSelectionPanel != null) {
//                geoSelectionPanel.initWith(doMakeGeoSelectionPanel());
//            }
//        }

//        List newFiles = Misc.newList(path);
//        if (changeLinks) {
//            //Get rid of the resolver URL
//            getProperties().remove(PROP_RESOLVERURL);
//            setNewFiles(newFiles);
//        }
//        
        logger.trace("returning={}", newFiles);
        return newFiles;
    }

    @Override protected String getDataPrefix() {
        String tmp = StringUtil.replace(getName(), ' ', "");
        tmp = StringUtil.replace(tmp, '/', "");
        tmp = StringUtil.replace(tmp, "(AllBands)", "");
        tmp = IOUtil.cleanFileName(tmp);
        logger.trace("data prefix={}", tmp);
        return tmp;
    }
    
    /**
     * A utility method that helps us deal with legacy bundles that used to
     * have String file names as the id of a data choice.
     *
     * @param object     May be an AddeImageDescriptor (for new bundles) or a
     *                   String that is converted to an image descriptor.
     * @return The image descriptor.
     */
    @Override public AddeImageDescriptor getDescriptor(Object object) {
//        logger.trace("--------------------");
        if (object == null) {
//            logger.trace("null obj");
            return null;
        }
        if (object instanceof DataChoice) {
            object = ((DataChoice)object).getId();
            logger.trace("datachoice getId={}", object);
        }
        if (object instanceof ImageDataInfo) {
            int index = ((ImageDataInfo) object).getIndex();
            if (index < myDataChoices.size()) {
                DataChoice dc = (DataChoice)myDataChoices.get(index);
                Object tmpObject = dc.getId();
                if (tmpObject instanceof ImageDataInfo) {
//                    logger.trace("returning imagedatainfo");
                    return ((ImageDataInfo)tmpObject).getAid();
                }
            }
//            logger.trace("invalid idx for imagedatainfo? (idx={} vs size={})", index, myDataChoices.size());
            return null;
            //            return ((ImageDataInfo) object).getAid();
        }

        if (object instanceof AddeImageDescriptor) {
//            logger.trace("already addeimagedesc! desc={}", object);
            return (AddeImageDescriptor)object;
        }
        AddeImageDescriptor tmp = new AddeImageDescriptor(object.toString());
//        logger.trace("return descriptor={}", tmp);
//        logger.trace("--------------------");
        return tmp;
    }

    /**
     *  Overwrite base class  method to return the name of this class.
     *
     *  @return The name.
     */
    public String getImageDataSourceName() {
        return "Adde Image Data Source (Parameter)";
    }

    private void setMag() {
        Object magKey = (Object)"mag";
        if (sourceProps.containsKey(magKey)) {
            String magVal = (String)(sourceProps.get(magKey));
            String[] magVals = magVal.split(" ");
            this.lineMag = Integer.parseInt(magVals[0]);
            this.elementMag = Integer.parseInt(magVals[1]);
        }
    }

    private void getAreaDirectory(Hashtable properties) {
        String addeCmdBuff = source;
        if (addeCmdBuff.contains("BAND=")) {
            String bandStr = getKey(addeCmdBuff, "BAND");
            if (bandStr.length() == 0) {
                addeCmdBuff = replaceKey(addeCmdBuff, "BAND", "1");
            }
        }
        if (addeCmdBuff.contains("MAG=")) {
            String[] segs = addeCmdBuff.split("MAG=");
            String seg0 = segs[0];
            String seg1 = segs[1];
            int indx = seg1.indexOf("&");
            seg1 = seg1.substring(indx);
            String magString = lineMag + " " + elementMag;
            addeCmdBuff = seg0 + "MAG=" + magString + seg1;
        }
        addeCmdBuff = addeCmdBuff.replace("imagedata", "imagedir");
        AreaDirectoryList dirList = null;
        try {
            dirList = new AreaDirectoryList(addeCmdBuff);
        } catch (Exception e) {
            try {
                List<BandInfo> bandInfos = (List<BandInfo>)getProperty(PROP_BANDINFO, (Object)null);
                BandInfo bi = bandInfos.get(0);
//                String bandStr = Integer.valueOf(bi.getBandNumber()).toString();
                addeCmdBuff = replaceKey(addeCmdBuff, "BAND", bi.getBandNumber());
                dirList = new AreaDirectoryList(addeCmdBuff);
            } catch (Exception eOpen) {
                setInError(true);
                logger.error("problem opening AREA file", eOpen);
            }
        }

        try {
            List areaDirs = dirList.getDirs();
            AreaDirectory ad = (AreaDirectory)areaDirs.get(0);
            float[] res = getLineEleResolution(ad);
            float resol = res[0];
            if (this.lineMag < 0) {
                resol *= Math.abs(this.lineMag);
            }
//            this.lineResolution = ad.getValue(11);
            this.lineResolution = ad.getValue(AreaFile.AD_LINERES);
            this.lRes = resol;
            resol = res[1];
            if (this.elementMag < 0) {
                resol *= Math.abs(this.elementMag);
            }
//            this.elementResolution = ad.getValue(12);
            this.elementResolution = ad.getValue(AreaFile.AD_ELEMRES);
            this.eRes = resol;
        } catch (Exception e) {
            setInError(true);
            logger.error("getting area directory", e);
        }
        baseSource = addeCmdBuff;
    }

    protected void initDataSelectionComponents(
        List components, final DataChoice dataChoice)
    {
        if (fromBundle && !hasRemoteChoices) {
            components.add(new BundlePreviewSelection("Region (Disabled)"));
            components.add(new BundlePreviewSelection("Advanced (Disabled)"));
            return;
        }

        getIdv().showWaitCursor();

        boolean hasImagePreview = true;
        if (this.showPreview == null) {
            this.showPreview = true;
        }
        boolean basically = false;
        if (this.lastChoice != null) {
            basically = dataChoice.basicallyEquals(this.lastChoice);
        }
        logger.trace("dataChoice={}", dataChoice);
        // check for comps and whether or not dataChoice is hooping right back into line
        if (this.haveDataSelectionComponents && dataChoice.equals(this.lastChoice)) {
            try {
                // did the datachoice ever actually get data?
                if (dataChoice.getDataSelection() == null) {
                    if (!basically) {
                        logger.trace("creating geolatlonselection");
                        this.laLoSel = new GeoLatLonSelection(this,
                                         dataChoice, this.initProps, this.previewProjection,
                                         previewDir, previewNav);

                        this.lineMag = this.laLoSel.getLineMag();
                        this.elementMag = this.laLoSel.getElementMag();

                    /* DAVEP: Force preview on. "No preview" means blank image */
//                    this.previewSel = new GeoPreviewSelection(this, dataChoice, this.previewImage,
//                                     this.laLoSel, this.previewProjection,
//                                     this.lineMag, this.elementMag, this.showPreview);
                        logger.trace("1: creating geopreviewselection: has geoprevsel: {}", this.previewSel!=null);
                        this.previewSel = new GeoPreviewSelection(this, dataChoice, this.previewImage,
                            this.laLoSel, this.previewProjection,
                            this.lineMag, this.elementMag, true);
                    }
//                    this.lineMag = this.laLoSel.getLineMag();
//                    this.elementMag = this.laLoSel.getElementMag();
//
//                    /* DAVEP: Force preview on. "No preview" means blank image */
////                    this.previewSel = new GeoPreviewSelection(this, dataChoice, this.previewImage,
////                                     this.laLoSel, this.previewProjection,
////                                     this.lineMag, this.elementMag, this.showPreview);
//                    logger.trace("1: creating geopreviewselection: has geoprevsel: {}", this.previewSel!=null);
//                    this.previewSel = new GeoPreviewSelection(this, dataChoice, this.previewImage,
//                            this.laLoSel, this.previewProjection,
//                            this.lineMag, this.elementMag, true);
                }
                components.add(this.previewSel);
                components.add(this.laLoSel);
            } catch (Exception e) {
                logger.error("error while repeating addition of selection components", e);
                getIdv().showNormalCursor();
            }
        } else {
            try {
                hasImagePreview = makePreviewImage(dataChoice);
                if (basically) {
                    getSaveComponents();
                }
            } catch (Exception e) {
                logger.error("Preview image Exception: " + e.getMessage());
                JLabel label = new JLabel("Can't make preview image");
                JPanel contents = GuiUtils.top(GuiUtils.inset(label, label.getText().length() + 12));
                GuiUtils.showOkDialog(null, "No Preview Image", contents, null);
                getIdv().showNormalCursor();
                logger.error("problem creating preview image", e);
                return;
            }
            this.lastChoice = dataChoice;
            if (hasImagePreview) {
                try {
                    String magStr = getKey(baseSource, MAG_KEY);
                    String saveMagStr = magStr;
                    String[] vals = StringUtil.split(magStr, " ", 2);
                    Integer iVal = Integer.valueOf(vals[0]);
                    int lMag = iVal.intValue() * -1;
                    if (lMag == -1) {
                        lMag = 1;
                    }
                    iVal = Integer.valueOf(vals[1]);
                    int eMag = iVal.intValue() * -1;
                    if (eMag == -1) {
                        eMag = 1;
                    }
                    magStr = lMag + " " + eMag;
                    replaceKey(MAG_KEY, magStr);
//                    String saveStr = baseSource;
//                    if (!showPreview) {
//                        replaceKey(SIZE_KEY, "2 2");
//                    }
                    AreaAdapter aa = null;
                    AREACoordinateSystem acs = null;
                    try {
                        logger.trace("creating AreaFile from src={}", baseSource);
                        if (showPreview) {
                            aa = new AreaAdapter(baseSource, false);
                            this.previewImage = (FlatField)aa.getImage();
                        } else {
                            this.previewImage = Util.makeField(0, 1, 1, 0, 1, 1, 0, "TEMP");
                        }
                        
                        AreaFile af = new AreaFile(baseSource);
                        previewNav = af.getNavigation();
                        AreaDirectory ad = af.getAreaDirectory();
                        this.lineResolution = ad.getValue(AreaFile.AD_LINERES);
                        this.elementResolution = ad.getValue(AreaFile.AD_ELEMRES);
                        acs = new AREACoordinateSystem(af);
                    } catch (Exception e) {
                        String excp = e.toString();
                        int indx = excp.lastIndexOf(":");
                        String errorText = excp.substring(indx+1);
                        JLabel label = new JLabel(errorText);
                        JPanel contents = GuiUtils.top(GuiUtils.inset(label, label.getText().length() + 12));
                        GuiUtils.showOkDialog(null, "Can't Make Geographical Selection Tabs", contents, null);
                        getIdv().showNormalCursor();
                        logger.error("problem creating preview image", e);
                        return;
                    }
                    this.initProps = new Hashtable();
                    Enumeration propEnum = sourceProps.keys();
                    for (int i = 0; propEnum.hasMoreElements(); i++) {
                        String key = propEnum.nextElement().toString();
                        Object val = sourceProps.get(key);
                        key = key.toUpperCase();
                        if (val instanceof String) {
                            String str = (String)val;
                            val = (Object)(str.toUpperCase());
                        }
                        this.initProps.put(key,val);
                    }
                    replaceKey(MAG_KEY, saveMagStr);
                    magStr = getKey(baseSource, MAG_KEY);
                    vals = StringUtil.split(magStr, " ", 2);
                    iVal = Integer.valueOf(vals[0]);
                    lMag = iVal.intValue();
                    iVal = Integer.valueOf(vals[1]);
                    eMag = iVal.intValue();

                    this.initProps.put("LRES", String.valueOf((this.lRes)));
                    this.initProps.put("ERES", String.valueOf((this.eRes)));
                    this.initProps.put("PLRES", String.valueOf((this.previewLineRes)));
                    this.initProps.put("PERES", String.valueOf((this.previewEleRes)));
                    this.previewProjection = (MapProjection)acs;

                    String coordType = "";
                    double coords[] = { 0.0, 0.0 };
                    
                    logger.trace("basically={} laLoSel==null?={}", basically, (this.laLoSel==null));
                    if (!basically) {
                        if (this.laLoSel != null) {
                            coordType = this.laLoSel.getCoordinateType();
                            if (coordType.equals(this.laLoSel.getLatLonType())) {
                                coords[0] = this.laLoSel.getLatitude();
                                coords[1] = this.laLoSel.getLongitude();
                            } else {
                                coords[0] = (double)this.laLoSel.getLine();
                                coords[1] = (double)this.laLoSel.getElement();
                            }

                            // turns out that laLoSel is reused for datachoices
                            // from the same source. if you don't update laLoSel's
                            // dataChoice, it'll apply whatever data selection
                            // you set up... to the first data choice that you
                            // loaded! (and causing an NPE when attempting to
                            // bundle the dataselection for the newly-selected
                            // datachoice.
                            this.previewSel.setDataChoice(dataChoice);
                            this.laLoSel.setDataChoice(dataChoice);
                            this.laLoSel.setPreviewLineRes(this.previewLineRes);
                            this.laLoSel.setPreviewEleRes(this.previewEleRes);
                            this.laLoSel.update(previewDir, this.previewProjection, previewNav,
                                           coordType, coords);
                            
                        } else {
                            this.laLoSel = new GeoLatLonSelection(this, 
                                          dataChoice, this.initProps, this.previewProjection,
                                          previewDir, previewNav);
                            this.lineMag = this.laLoSel.getLineMag();
                            this.elementMag = this.laLoSel.getElementMag();
                        }
                    } else {
                        if (this.laLoSel != null) {
                            this.previewSel.setDataChoice(dataChoice);
                            this.laLoSel.setDataChoice(dataChoice);
                        }
                    }
                    /* DAVEP: Force preview on. "No preview" means blank image */
//                    this.previewSel = new GeoPreviewSelection(this, dataChoice, this.previewImage, 
//                                     this.laLoSel, this.previewProjection,
//                                     this.lineMag, this.elementMag, this.showPreview);
                    logger.trace("even reaching this point?");
                    this.previewSel = new GeoPreviewSelection(this, dataChoice, this.previewImage, 
                            this.laLoSel, this.previewProjection,
                            this.lineMag, this.elementMag, showPreview);
                    logger.trace("how about this one?");
                } catch (Exception e) {
                    logger.error("problem making selection components", e);
                    getIdv().showNormalCursor();
                }
                this.haveDataSelectionComponents = true;
//                replaceKey(MAG_KEY, (Object)(this.lineMag + " " + this.elementMag));
                replaceKey(MAG_KEY, (this.lineMag + " " + this.elementMag));
                components.add(this.previewSel);
                components.add(this.laLoSel);
            }
        }
        if (this.previewSel != null) {
            this.previewSel.initBox();
        }
        getIdv().showNormalCursor();
    }

    /**
     * A hook to allow this data source to add data selection components
     * to the IDV field selector
     *
     * @param dataChoice the data choice
     *
     * @return list of components
     */
//    @Override public List<DataSelectionComponent> getDataSelectionComponents(DataChoice dataChoice) {
////        List<DataSelectionComponent> dataSelectionComponents = new ArrayList<DataSelectionComponent>();
////        initDataSelectionComponents(dataSelectionComponents, dataChoice);
////        return dataSelectionComponents;
//        return new ArrayList<DataSelectionComponent>();
//    }
    
    private boolean makePreviewImage(DataChoice dataChoice) {
        logger.trace("Starting with dataChoice={}", dataChoice);
        getIdv().showWaitCursor();
        
        // For dealing with derived fields - will need to know which bands are involved
        // They may be a mix of base resolutions
        ArrayList<Integer> derivedBands = new ArrayList<Integer>();
        
        // TJJ Inq #2181 Feb 2020
        // If derived, figure out which bands are involved. Open to better ideas here :-)
        if (dataChoice instanceof DerivedDataChoice) {
            derivedBandLineRes.clear();
            derivedBandElemRes.clear();
            derivedBandMagFactor.clear();
            isDerived = true;
            Hashtable ht = ((DerivedDataChoice) dataChoice).getUserSelectedChoices();
            java.util.Set<String> set = ht.keySet();
            for (String s : set) {
                DataChoice dc = (DataChoice) ht.get(s);
                logger.debug("Data choice String ID " + dc.getStringId());
                logger.debug("Data choice Name " + dc.getName());
                String dcName = dc.getName();
                // Pull out band number, definitely hacky but don't know a better way
                int idxLo = dcName.indexOf("Band") + 4;
                int idxHi = dcName.indexOf('_', idxLo);
                String bandStr = dcName.substring(idxLo, idxHi);
                logger.debug("Data choice Band " + bandStr);
                derivedBands.add(Integer.parseInt(bandStr));
                Hashtable dcProps = dc.getProperties();
            }
        } else {
            isDerived = false;
        }
        
        boolean msgFlag = false;
        showPreview = saveShowPreview;
        List<BandInfo> bandInfos = (List<BandInfo>)getProperty(PROP_BANDINFO, (Object) null);
        BandInfo bi = null;

        String saveBand = getKey(source, BAND_KEY);

        int bandIdx = 0;
        int sensorID = -1;

        logger.trace("band index stuff: saveBand={}, bandIdx={}, source={}", new Object[] { saveBand, bandIdx, source });
        List<TwoFacedObject> calList = null;
        try {
            Object dcObj = dataChoice.getId();
            if (dcObj instanceof BandInfo) {
                bi = (BandInfo) dcObj;
                Integer bandInt = Integer.valueOf(bandInfos.indexOf(dcObj)+1);
                saveBand = bandInt.toString();
            } else {
                msgFlag = true;
                bi = bandInfos.get(bandIdx);
                this.showPreview = false;
                // If derived, use band representing best available res
                if (isDerived) {
                    this.showPreview = true;
                    double bestRes = Double.MAX_VALUE;
                    int tmpIdx = 0;
                    for (BandInfo bandInfo : bandInfos) {
                        AreaDirectory ad = (AreaDirectory) allBandDirs.get(bandInfo.getBandNumber());
                        double lineRes = ad.getCenterLatitudeResolution();
                        double elemRes = ad.getCenterLongitudeResolution();
                        if (derivedBands.contains(bandInfo.getBandNumber())) {
                            int tmpBand = bandInfo.getBandNumber();
                            derivedBandLineRes.put(tmpBand, lineRes);
                            derivedBandElemRes.put(tmpBand, elemRes);
                        }
                        if (lineRes < bestRes && (derivedBands.contains(bandInfo.getBandNumber()))) {
                            bestRes = lineRes;
                            bandIdx = tmpIdx;
                            logger.debug("Derived field, updated bandIdx to: " + bandIdx);
                            bi = bandInfo;
                            saveBand = String.valueOf(bi.getBandNumber());
                            logger.debug("Updated bandInfo: " + bi);
                        }
                        tmpIdx++;
                    }
                    // Now that we've got all the derived bands and resolutions,
                    // set multipliers for line/elem ranges based on mixtures of resolutions
                    boolean allThree = false;
                    boolean twoLower = false;
                    boolean twoHigher = false;
                    boolean allSame = true;
                    Collection<Double> resVals = (Collection<Double>) derivedBandLineRes.values();
                    if (resVals.contains(0.5)) {
                        if (resVals.contains(1.0)) {
                            if (resVals.contains(2.0)) {
                                allThree = true;
                                allSame = false;
                            } else {
                                twoLower = true;
                                allSame = false;
                            }
                        }
                    } else {
                        if (resVals.contains(1.0)) {
                            if (resVals.contains(2.0)) {
                                twoHigher = true;
                                allSame = false;
                            }
                        }
                    }

                    for (Integer bandNum : derivedBandLineRes.keySet()) {

                        Double bandRes = derivedBandLineRes.get(bandNum);
                        if (allSame) {
                            derivedBandMagFactor.put(bandNum, 1);
                        }
                        if (twoLower) {
                            if (bandRes < 1) {
                                derivedBandMagFactor.put(bandNum, 1);
                            } else {
                                derivedBandMagFactor.put(bandNum, 2);
                            }
                        }
                        if (twoHigher) {
                            if (bandRes > 1) {
                                derivedBandMagFactor.put(bandNum, 2);
                            } else {
                                derivedBandMagFactor.put(bandNum, 1);
                            }
                        }
                        if (allThree) {
                            if (bandRes > 1) {
                                derivedBandMagFactor.put(bandNum, 4);
                            } else {
                                if (bandRes < 1) {
                                    derivedBandMagFactor.put(bandNum, 1);
                                } else {
                                    derivedBandMagFactor.put(bandNum, 2);
                                }
                            }
                        }
                    }

                    // Print all mags for verification
                    derivedBandMagFactor.entrySet().forEach(entry -> {
                        logger.debug("Derived Mag Band : " + entry.getKey() + " Mag Value : " + entry.getValue());
                    });

                }
            }
            // pull out the list of cal units, we'll need for type check later...
            calList = bi.getCalibrationUnits();
            
            // TJJ Dec 2017
            // Kinda hacky (but then again so is this entire class) :-/
            // Do our GEO speedup code for known GEO sensor IDs
            sensorID = bi.getSensor();
            if (sensorIsGEO(sensorID)) {
                isGeoSensor = true;
            }
            if (sensorIsABI(sensorID)) {
                isABISensor = true;
            }

            logger.trace("replacing band: new={} from={}", bi.getBandNumber(), source);
            source = replaceKey(source, BAND_KEY, bi.getBandNumber());
            // if we're replacing the band, replace cal type with preferred type for that band
            logger.trace("replacing unit: new={} from={}", bi.getPreferredUnit(), source);
            source = replaceKey(source, UNIT_KEY, bi.getPreferredUnit());
        } catch (Exception excp) {
            handlePreviewImageError(1, excp);
        }
        String name = dataChoice.getName();
        int idx = name.lastIndexOf('_');
        String unit = name.substring(idx + 1);

        // if this is not a valid cal unit (e.g. could be set to a plugin formula name)
        // set it to something valid
        boolean validCal = false;
        for (TwoFacedObject tfo : calList) {
            if (unit.equals((String) tfo.getId())) {
                validCal = true;
                break;
            }
        }
        if (!validCal) {
            unit = bi.getPreferredUnit();
        }

        if (getKey(source, UNIT_KEY).length() == 0) {
            logger.trace("non-empty unit, replacing: new={} from={}", unit, source);
//            source = replaceKey(source, UNIT_KEY, (Object)(unit));
            source = replaceKey(source, UNIT_KEY, unit);
        }

        AddeImageDescriptor aid = null;
        while (aid == null) {
            try {
                logger.trace("creating new AddeImageDescriptor from {}", this.source);
                aid = new AddeImageDescriptor(this.source);
            } catch (Exception excp) {
                msgFlag = true;
                if (bandIdx > (bandInfos.size() - 1)) {
                	getIdv().showNormalCursor();
                    return false;
                }
                
                bi = bandInfos.get(bandIdx);
                logger.trace("replacing band: new={} from={}", bi.getBandNumber(), source);
//                source = replaceKey(source, BAND_KEY, (Object)(bi.getBandNumber()));
                source = replaceKey(source, BAND_KEY, bi.getBandNumber());
                ++bandIdx;
            }
        }
//        previewDir = getPreviewDirectory(aid);
        AddeImageDescriptor previewDescriptor = getPreviewDirectory(aid, dataChoice);
        previewDir = previewDescriptor.getDirectory();

        // On the off chance user is working with ABI MESO or AHI TARGET sectors,
        // *and* they have polling on, we need to turn auto-set projection off
        // since the domain can shift around

        if (isABISensor) {
            String memo = previewDir.getMemoField().toLowerCase();
            boolean isMesoOrTarget = false;
            if ((memo.contains("meso")) || (memo.contains("targ"))) isMesoOrTarget = true;
            logger.info("Preview Directory memo field: " + memo);
            if (isPolling() && isMesoOrTarget) {
                MapViewManager mvm = (MapViewManager) getIdv().getViewManager();
                mvm.setUseProjectionFromData(false);
                boolean offScreen = getIdv().getArgsManager().getIsOffScreen();
                if (! domainShiftNoticeTargetShown) {
                    if (! offScreen) {
                        String msg = "You currently have polling active, and are working with\n" +
                                "a targeted sector (e.g. ABI MESO). Since these domains can\n" +
                                "shift geographically, auto-set projection has been turned off.\n";
                        Object[] params = { msg };
                        JOptionPane.showMessageDialog(null, params, "Notice", JOptionPane.OK_OPTION);
                        domainShiftNoticeTargetShown = true;
                    } else {
                        logger.warn("Note: Polling with shifting sector - auto-set projection turned off");
                    }
                }
            }
        }

        logger.trace("using previewDir={}", previewDir);
//        try {
//            logger.trace("preview areadir: stlines={} stelements={} lines={} elements={}", new Object[] { previewDir.getValue(AreaFile.AD_STLINE), previewDir.getValue(AreaFile.AD_STELEM), previewDir.getLines(), previewDir.getElements() });
//        } catch (Exception e) {
//            logger.error("error logging areadir preview", e);
//        }
        int eMag = 1;
        int lMag = 1;
        int eSize = 1;
        int lSize = 1;
        try {
            int plMag = 1;
            int peMag = 1;
            Object magKey = (Object) "mag";
            if (sourceProps.containsKey(magKey)) {
                String magVal = (String)(sourceProps.get(magKey));
                String[] magVals = magVal.split(" ");
                peMag = Integer.parseInt(magVals[0]);
                plMag = Integer.parseInt(magVals[1]);
            }
            double feSize = (double) previewDir.getElements();
            double flSize = (double) previewDir.getLines();
            double feMag = (double) peMag;
            double flMag = (double) plMag;
            if (feSize > flSize) {
                feMag = feSize / 525.0;
                flMag = feMag * (double) plMag / (double) peMag;
            } else {
                flMag = flSize/500.0;
                feMag = flMag * (double) peMag / (double) plMag;
            }
            eMag = (int) Math.ceil(feMag);
            lMag = (int) Math.ceil(flMag);
        } catch(Exception excp) {
           handlePreviewImageError(3, excp);
        }
        if (eMag < 1) eMag = 1;
        if (lMag < 1) lMag = 1;

        eSize = 525;
        lSize = 500;
        if ((baseSource == null) || msgFlag) {
            logger.trace("replacing\nbaseSource={}\nsource={}", baseSource, source);
            baseSource = source;
        }
        this.previewLineRes = lMag;
        this.previewEleRes = eMag;
        String uLStr = "0 0 F";
        // MJH getting uLStr from previewDir here breaks Himawari-8,
        // and is apparently unnecessary in general, so just use "0 0 F" always.
        // try {
        //     int startLine = previewDir.getValue(AreaFile.AD_STLINE);
        //     int startEle = previewDir.getValue(AreaFile.AD_STELEM);
        //     uLStr = startLine + " " + startEle + " I";
        // } catch (Exception e) {
        // }
//        String src = aid.getSource();
        String src = previewDescriptor.getSource();
        logger.trace("building preview request from src={}", src);
        
        src = removeKey(src, LATLON_KEY);
        src = replaceKey(src, LINELE_KEY, uLStr);
        src = replaceKey(src, PLACE_KEY, "ULEFT");
        src = replaceKey(src, SIZE_KEY,(lSize + " " + eSize));
        src = replaceKey(src, MAG_KEY, (lMag + " " + eMag));
        src = replaceKey(src, BAND_KEY, bi.getBandNumber());
        src = replaceKey(src, UNIT_KEY, unit);
//        if (aid.getIsRelative()) {
//            logger.trace("injecting POS={}", aid.getRelativeIndex());
//            src = replaceKey(src, "POS", (Object)aid.getRelativeIndex());
//        }
//        if (previewDescriptor.getIsRelative()) {
//            logger.trace("inject POS={} into src={}", previewDescriptor.getRelativeIndex(), src);
//            src = replaceKey(src, "POS", (Object)previewDescriptor.getRelativeIndex());
//            src = replaceKey(src, "POS", previewDescriptor.getRelativeIndex());
//        }

        logger.trace("creating AddeImageDescriptor from src={}", src);
        try {
            aid = new AddeImageDescriptor(src);
        } catch (Exception excp) {
            handlePreviewImageError(4, excp);
            src = replaceKey(src, BAND_KEY, saveBand);
            aid = new AddeImageDescriptor(src);
            src = replaceKey(src, BAND_KEY, bi.getBandNumber());
        }
        if (msgFlag && (!"ALL".equals(saveBand))) {
            src = replaceKey(src, BAND_KEY, saveBand);
        }
        logger.trace("overwriting\nbaseSource={}\nsrc={}", baseSource, src);
        baseSource = src;
        getIdv().showNormalCursor();
        return true;
    }

    /**
     * Return true if the Sensor is ABI variant (ABI, AHI, AMI)
     * These sensors have MESO sectors which move around geospatially
     *
     * @param sensorID McIDAS Sensor Source number.
     *  See https://www.ssec.wisc.edu/mcidas/doc/users_guide/2017.2/app_c-1.html
     * @return true if ID matches a defined ABI sensor
     */

    private boolean sensorIsABI(int sensorID) {

        boolean isABI = false;

        // GOES 16 - 19
        if (sensorID == 186) isABI = true;
        if (sensorID == 188) isABI = true;
        if (sensorID == 190) isABI = true;
        if (sensorID == 192) isABI = true;

        // Himawari-8 and Himawari-9
        if (sensorID == 86) isABI = true;
        if (sensorID == 286) isABI = true;
        if (sensorID == 287) isABI = true;
        if (sensorID == 288) isABI = true;
        if (sensorID == 289) isABI = true;

        return isABI;
    }

    /**
     * Return true if the Sensor is Geostationary
     * 
     * @param sensorID McIDAS Sensor Source number.
     *  See https://www.ssec.wisc.edu/mcidas/doc/users_guide/2017.2/app_c-1.html
     * @return true if ID matches a defined GEO sensor
     */

    private boolean sensorIsGEO(int sensorID) {
        boolean isGEO = false;
        
        // early GEO through FY
        if ((sensorID >= 12) && (sensorID <= 40)) isGEO = true; 
        
        // Meteosat
        if ((sensorID >= 51) && (sensorID <= 58)) isGEO = true;
        if (sensorID == 354) isGEO = true;

        // GOES 8 - 12
        if ((sensorID >= 70) && (sensorID <= 79)) isGEO = true;

        // GMS, MTSAT (which is actually pre-8 Himawari), H-8/9
        if ((sensorID >= 82) && (sensorID <= 86)) isGEO = true;
        if ((sensorID >= 286) && (sensorID <= 288)) isGEO = true;

        // Feng Yun
        if ((sensorID >= 95) && (sensorID <= 97)) isGEO = true;
        if ((sensorID >= 275) && (sensorID <= 277)) isGEO = true;

        // GOES 13 - 19
        // NOTE: through all four GOES-R series satellites
        if ((sensorID >= 180) && (sensorID <= 193)) isGEO = true;

        // Kalpana and INSAT
        if ((sensorID >= 230) && (sensorID <= 232)) isGEO = true;

        // COMS
        if (sensorID == 250) isGEO = true;
        
        return isGEO;
    }

    /**
     * Show the given error to the user. 
     *
     * @param excp The exception
     */
    protected void handlePreviewImageError(int flag, Exception excp) {
        getIdv().showNormalCursor();
        LogUtil.userErrorMessage("Error in makePreviewImage  e=" + flag + " " + excp);
    }

    public static String removeKey(String src, String key) {
        String returnString = src;
        key = key.toUpperCase() + '=';
        if (returnString.contains(key)) {
            String[] segs = returnString.split(key);
            String seg0 = segs[0];
            String seg1 = segs[1];
            int indx = seg1.indexOf('&');
            if (indx >= 0) {
                seg1 = seg1.substring(indx + 1);
            }
            returnString = seg0 + seg1;
        }
        return returnString;
    }

    public static String replaceKey(String sourceUrl, String key, Object value) {
        String returnString = sourceUrl;

        // make sure we got valid key/value pair
        if ((key == null) || (value == null)) {
            return returnString;
        }

        key = key.toUpperCase() + '=';
        String strValue = value.toString();
        if (returnString.contains(key)) {
            String[] segs = returnString.split(key);
            String seg0 = segs[0];
            String seg1 = segs[1];
            int indx = seg1.indexOf('&');
            if (indx < 0) {
                seg1 = "";
            } else if (indx > 0) {
                seg1 = seg1.substring(indx);
            }
            returnString = seg0 + key + strValue + seg1;
        } else {
            returnString = returnString + '&' + key + strValue;
        }

        // if key is for cal units, and it was changed to BRIT,
        // must change the spacing key too 
        if ((key.equals(UNIT_KEY + '=')) && ("BRIT".equals(strValue))) {
            returnString = replaceKey(returnString, SPAC_KEY, SPAC_KEY, SPACING_BRIT);
        } else {
            returnString = replaceKey(returnString, SPAC_KEY, SPAC_KEY, SPACING_NON_BRIT); 
        }
        return returnString;
    }

    public static String replaceKey(String src, String oldKey, String newKey, Object value) {
        String returnString = src;
        oldKey = oldKey.toUpperCase() + '=';
        newKey = newKey.toUpperCase() + '=';
        if (returnString.contains(oldKey)) {
            String[] segs = returnString.split(oldKey);
            String seg0 = segs[0];
            String seg1 = segs[1];
            int indx = seg1.indexOf('&');
            if (indx < 0) {
                seg1 = "";
            } else if (indx > 0) {
                seg1 = seg1.substring(indx);
            }
            returnString = seg0 + newKey + value.toString() + seg1;
        }
        else {
            returnString = returnString + '&' + newKey + value.toString();
        }
        return returnString;
    }

    private <T> void replaceKey(String key, T value) {
        baseSource = replaceKey(baseSource, key, value);
    }

    public static String getKey(String src, String key) {
        String returnString = "";
        key = key.toUpperCase() + '=';
        if (src.contains(key)) {
            String[] segs = src.split(key);
            segs = segs[1].split("&");
            returnString = segs[0];
        }
        return returnString;
    }


    /**
     * Create the set of {@link ucar.unidata.data.DataChoice} that represent
     * the data held by this data source.  We create one top-level
     * {@link ucar.unidata.data.CompositeDataChoice} that represents
     * all of the image time steps. We create a set of children
     * {@link ucar.unidata.data.DirectDataChoice}, one for each time step.
     */
    
    public void doMakeDataChoices() {
        super.doMakeDataChoices();
        List<BandInfo> bandInfos = (List<BandInfo>) getProperty(PROP_BANDINFO, (Object) null);
        String name = "";
        if (this.choiceName != null) {
            name = this.choiceName;
        }
        if (name.length() != 0) {
            logger.trace("already have a name={}", name);
            return;
        }
        if (!sourceProps.containsKey(UNIT_KEY)) {
            logger.trace("sourceProps has no unit key={}", sourceProps);
            return;
        }
        BandInfo bi = null;
        if (sourceProps.containsKey(BAND_KEY)) {
            int bandProp = Integer.parseInt((String)(sourceProps.get(BAND_KEY)));
            int bandIndex = BandInfo.findIndexByNumber(bandProp, bandInfos);
            bi = (BandInfo)bandInfos.get(bandIndex);
            if (sourceProps.containsKey(UNIT_KEY)) {
                bi.setPreferredUnit((String)(sourceProps.get(UNIT_KEY)));
            } else {
                bi.setPreferredUnit("");
            }
            name = makeBandParam(bi);
        }
        else if (sourceProps.containsKey(BANDINFO_KEY)) {
            ArrayList al = (ArrayList) sourceProps.get(BANDINFO_KEY);
            bi = (BandInfo) al.get(0);
            name = makeBandParam(bi);
        }
        if (stashedChoices != null) {
            int numChoices = stashedChoices.size();
            for (int i = 0; i < numChoices; i++) {
               DataChoice choice = (DataChoice) stashedChoices.get(i);
               if (name.equals(choice.getName())) {
                   setProperty(PROP_DATACHOICENAME, choice.getName());
               }
            }
        }
    }

    /**
     * Overridden so that McIDAS-V can <i>attempt</i> to return the correct
     * {@code DataSelection} for the current {@code DataChoice}.
     */
    
    @Override public DataSelection getDataSelection() {
        DataSelection tmp;

        if (this.laLoSel != null) {
            logger.trace("* mcv getSelForChoice: choice='{}'", this.laLoSel.getDataChoice());
            tmp = this.getSelForChoice(this.laLoSel.getDataChoice());
        } else {
            logger.trace("* idvland getDataSelection laLoSel=null: {}; choiceToSel=null: {}", (this.laLoSel==null), (this.choiceToSel==null));
            tmp = super.getDataSelection();
        }
//        if (this.laLoSel == null || this.choiceToSel == null || !this.choiceToSel.containsKey(this.laLoSel.getDataChoice())) {
//            logger.trace("* idvland getDataSelection laLoSel=null: {}; choiceToSel=null: {}", (this.laLoSel==null), (this.choiceToSel==null));
//            tmp = super.getDataSelection();
//        } else if (this.laLoSel != null) {
//            logger.trace("* mcv getSelForChoice");
//            tmp = this.getSelForChoice(this.laLoSel.getDataChoice());
//        }
        if (tmp != null) {
            logger.trace("return selection props={} geo={}", tmp.getProperties(), tmp.getGeoSelection());
        } else {
            logger.trace("return selection props=null geo=null choiceToSel={} :(", this.choiceToSel);
        }
        return tmp;
    }

    /**
     * Overridden so that McIDAS-V can associate this data source's current 
     * {@code DataChoice} with the given {@code DataSelection}.
     */
    
    @Override public void setDataSelection(DataSelection s) {

        GeoSelection tmp = null;
        if (s != null) {
            tmp = s.getGeoSelection();
        }
        if (tmp != null && this.laLoSel != null) {
            GeoLocationInfo bbox = tmp.getBoundingBox();
            GeoLocationInfo laloBbox = this.laLoSel.getGeoLocationInfo();
            tmp.setBoundingBox(laloBbox);
            logger.trace("incoming bbox={} laLo bbox={}", bbox, laloBbox);
        }

        super.setDataSelection(s);

        if (this.laLoSel != null) {
            this.putSelForChoice(this.laLoSel.getDataChoice(), s);
        } else {
            logger.trace("laLoSel is null; s={}", s);
        }

    }
    
    /**
     * Insert the new DataChoice into the dataChoice list.
     *
     * @param choice   new choice to add
     */
    
    protected void addDataChoice(DataChoice choice) {
        logger.trace("choice={}", choice);
        super.addDataChoice(choice);
        if (stashedChoices == null) {
            stashedChoices = new ArrayList();
        }
        stashedChoices.add(choice);
    }

    /**
     * Checks to see if a given {@code AddeImageDescriptor} is based upon a 
     * local (or remote) file.
     * 
     * <p>The check is pretty simple: is {@code descriptor.getSource()} a valid
     * path?
     * 
     * @param descriptor {@code AddeImageDescriptor} of questionable origins. Shouldn't be {@code null}.
     * 
     * @return {@code true} if {@code descriptor}'s source is a valid path.
     */
    
    public static boolean isFromFile(final AddeImageDescriptor descriptor) {
        return new File(descriptor.getSource()).exists();
    }

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
        Data img = null;
        iml = new ArrayList();

        if (dataSelection == null) {
            return null;
        }
        setDataSelection(dataSelection);

        // assuming the Double.NaN bbox is easy to replace with
        // an actual bbox, the getGeoSelection(createIfNeeded=true) should
        // probably populate the geoselection with the Double.NaN bbox.
        GeoSelection geoSelection = dataSelection.getGeoSelection(true);
        if (geoSelection == null) {
            return null;
        }

        GeoLocationInfo selectionBox = geoSelection.getBoundingBox();
        if (selectionBox == null) {
            logger.info("MCV3088: incoming dataselection has null bounding box, replacing with default! dataChoice: '{}', dataSelection: {}", dataChoice.getName(), dataSelection);
            selectionBox = new GeoLocationInfo(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
            geoSelection.setBoundingBox(selectionBox);
        }

        boolean validState = geoSelection.getHasValidState();
        if (!validState) {
            return null;
        }

        if (this.lastGeoSelection == null) {
            this.lastGeoSelection = geoSelection;
        }

        this.selectionProps = dataSelection.getProperties();
        Enumeration propEnum = this.selectionProps.keys();
        while (propEnum.hasMoreElements()) {
            String key = propEnum.nextElement().toString();
            if (key.compareToIgnoreCase(LATLON_KEY) == 0) {
                String val = (String)this.selectionProps.get(key);
                if (val.contains("NaN")) {
                    return img;
                }
            }
            if (key.compareToIgnoreCase(LINELE_KEY) == 0) {
                String val = (String)this.selectionProps.get(key);
                if (val.contains("NaN")) {
                    return img;
                }
            }
        }

        if (this.selectionProps.containsKey("MAG")) {
            String str = (String)this.selectionProps.get("MAG");
            String[] strs = StringUtil.split(str, " ", 2);
            this.lineMag = Integer.parseInt(strs[0]);
            this.elementMag = Integer.parseInt(strs[1]);
        }
        this.choiceName = dataChoice.getName();
        if (this.choiceName != null) {
            setProperty(PROP_DATACHOICENAME, this.choiceName);
        }
        try {
            img = super.getDataInner(dataChoice, category, dataSelection, requestProperties);
        } catch (Exception e) {
            String displaySrc = getDisplaySource();
            if (displaySrc != null) {
                AddeImageDescriptor aid = new AddeImageDescriptor(displaySrc);
                dataChoice.setId((Object)aid);
                img = super.getDataInner(dataChoice, category, dataSelection, requestProperties);
            }
        }
        return img;
    }

    /**
     * Check if the DataChoice has a BandInfo for its ID
     *
     * @param dataChoice  choice to check
     *
     * @return true if the choice ID is a BandInfo
     */
    
    private boolean hasBandInfo(DataChoice dataChoice) {
        Object id = dataChoice.getId();
        return id instanceof BandInfo;
    }

    /** _more_ */
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
    
    protected ImageSequence makeImageSequence(DataChoice dataChoice, DataSelection subset)
            throws VisADException, RemoteException {

//        if (dataChoice.getDataSelection() == null) {
//            dataChoice.setDataSelection(subset);
//        }
        Hashtable subsetProperties = subset.getProperties();
        Enumeration propEnum = subsetProperties.keys();
        int numLines = 0;
        int numEles = 0;
        while (propEnum.hasMoreElements()) {
            String key = propEnum.nextElement().toString();
            if (key.compareToIgnoreCase(SIZE_KEY) == 0) {
                String sizeStr = (String)(subsetProperties.get(key));
                String[] vals = StringUtil.split(sizeStr, " ", 2);
                Integer iVal = Integer.valueOf(vals[0]);
                numLines = iVal.intValue();
                iVal = Integer.valueOf(vals[1]);
                numEles = iVal.intValue();
                break;
            }
        }

        if (sampleMapProjection == null) {
            String addeCmdBuff = baseSource;
            AreaFile af = null;
            try {
                af = new AreaFile(addeCmdBuff);
            } catch (Exception eOpen) {
                logger.error("could not open area file: {}", eOpen);
                setInError(true);
                throw new BadDataException("Opening area file: " + eOpen.getMessage(), eOpen);
            }
            try {
//                McIDASAreaProjection map = new McIDASAreaProjection(af);
                AREACoordinateSystem acs = new AREACoordinateSystem(af);
                sampleMapProjection = (MapProjection)acs;
//                sampleProjection = map;
            } catch (Exception e) {
                logger.error("making area projection: {}", e);
                setInError(true);
                throw new BadDataException("Making area projection: " + e.getMessage(), e);
            }
        }
        AREACoordinateSystem macs = (AREACoordinateSystem)sampleMapProjection;
        int[] dirBlk = macs.getDirBlock();
        if (numLines == 0) {
            double elelin[][] = new double[2][2];
            double latlon[][] = new double[2][2];
            GeoSelection gs = subset.getGeoSelection();
            GeoLocationInfo gli = gs.getBoundingBox();
            if ((gli == null) && (lastGeoSelection != null)) {
                subset.setGeoSelection(lastGeoSelection);
                gs = lastGeoSelection;
                gli = gs.getBoundingBox();
            }
            LatLonPoint llp = gli.getUpperLeft();
            latlon[0][0] = llp.getLatitude();
            latlon[1][0] = llp.getLongitude();
            llp = gli.getLowerRight();
            latlon[0][1] = llp.getLatitude();
            latlon[1][1] = llp.getLongitude();
            elelin = macs.fromReference(latlon);
            numLines = (int)(Math.abs(elelin[1][0] - elelin[1][1]))*dirBlk[11];
            numEles = (int)(Math.abs(elelin[0][1] - elelin[0][0]))*dirBlk[12];
        }

        try {
            descriptorsToUse = new ArrayList();
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

            if (descriptorsToUse == null || descriptorsToUse.size() == 0) {
                return null;
            }
            AddeImageInfo biggestPosition = null;
            int           pos             = 0;
            boolean       anyRelative     = false;
            // Find the descriptor with the largest position
            for (Iterator iter = descriptorsToUse.iterator(); iter.hasNext(); ) {
                AddeImageDescriptor aid = (AddeImageDescriptor) iter.next();
                if (aid.getIsRelative()) {
                    anyRelative = true;
                }
                AddeImageInfo       aii = aid.getImageInfo();

                // Are we dealing with area files here?
                if (aii == null) {
                    break;
                }

                // Check if this is absolute time
                if ((aii.getStartDate() != null) || (aii.getEndDate() != null)) {
                    biggestPosition = null;
                    break;
                }
                if ((biggestPosition == null) || (Math.abs(aii.getDatasetPosition()) > pos)) {
                    pos             = Math.abs(aii.getDatasetPosition());
                    biggestPosition = aii;
                }
            }

            if (getCacheDataToDisk() && anyRelative && (biggestPosition != null)) {
                biggestPosition.setRequestType(AddeImageInfo.REQ_IMAGEDIR);
                AreaDirectoryList adl = new AreaDirectoryList(biggestPosition.getURLString());
                biggestPosition.setRequestType(AddeImageInfo.REQ_IMAGEDATA);
                currentDirs = adl.getSortedDirs();
            } else {
                currentDirs = null;
            }

            int cnt = 1;
            DataChoice parent = dataChoice.getParent();
            final List<SingleBandedImage> images = new ArrayList<SingleBandedImage>();
            MathType rangeType = null;
            for (Iterator iter = descriptorsToUse.iterator(); iter.hasNext(); ) {
                final AddeImageDescriptor aid = (AddeImageDescriptor) iter.next();
                if (currentDirs != null) {
                    int idx = Math.abs(aid.getImageInfo().getDatasetPosition());
                    if (idx >= currentDirs.length) {
                        continue;
                    }
                }

                String label = "";
                if (parent != null) {
                    label = label + parent.toString() + ' ';
                } else {
                    DataCategory displayCategory = dataChoice.getDisplayCategory();
                    if (displayCategory != null) {
                        label = label + displayCategory + ' ';
                    }
                }
                label = label + dataChoice.toString();
                final String readLabel = "Time: " + (cnt++) + '/'
                    + descriptorsToUse.size() + ' '
                    + label;

                String src = aid.getSource();
                if (!isFromFile(aid)) {
                    try {
                        src = replaceKey(src, LINELE_KEY, (Object)("1 1"));
                        String sizeString = "10 10";
                        src = replaceKey(src, SIZE_KEY, (Object)(sizeString));
                        String name = dataChoice.getName();
                        int idx = name.lastIndexOf('_');
                        String unit = name.substring(idx+1);
                        if (getKey(src, UNIT_KEY).length() == 0) {
                            src = replaceKey(src, UNIT_KEY, (Object)(unit));
                        }
                        int lSize = numLines;
                        int eSize = numEles;

                        int magFactor = 1;

                        // TJJ Mar 2020 - For derived fields, see if product bands are mixed-resolution bands
                        if (isDerived) {
                            int derivedBandNum = Integer.parseInt(getKey(src, BAND_KEY));
                            magFactor = derivedBandMagFactor.get(derivedBandNum);
                        }

                        sizeString = lSize / magFactor + " " + eSize / magFactor;
                        src = replaceKey(src, SIZE_KEY, (Object) (sizeString));
                        src = replaceKey(src, MAG_KEY, (Object) (this.lineMag + " " + this.elementMag));
                        aid.setSource(src);
                        logger.debug("makeImageSequence src: " + src);
                    } catch (Exception exc) {
                        logger.error("error trying to adjust AddeImageDescriptor: {}", exc);
                        super.makeImageSequence(dataChoice, subset);
                    }
                }

                try {
                    SingleBandedImage image = makeImage(aid, rangeType, true, readLabel, subset);
                    if (image != null) {
                        if(rangeType==null) {
                            rangeType = ((FunctionType) image.getType()).getRange();
                        }
                        synchronized (images) {
                            images.add(image);
                        }
                    }
                } catch (VisADException e) {
                    logger.error("avoiding visad exception: ",e);
                } catch (RemoteException e) {
                    logger.error("avoiding remote exception: ", e);
                }
            }

            TreeMap imageMap = new TreeMap();
            for (SingleBandedImage image : images) {
                imageMap.put(image.getStartTime(), image);
            }
            List<SingleBandedImage> sortedImages = (List<SingleBandedImage>) new ArrayList(imageMap.values());
            if ((sortedImages.size() > 0) && (sortedImages.get(0) instanceof AreaImageFlatField)) {
                DataRange[] sampleRanges = null;
                Set domainSet = null;
                for (SingleBandedImage sbi : sortedImages) {
                    AreaImageFlatField aiff = (AreaImageFlatField) sbi;
                    sampleRanges = aiff.getRanges(true);
                    if (domainSet == null) {
                        domainSet = aiff.getDomainSet();
                    }
                    if ((sampleRanges != null) && (sampleRanges.length > 0)) {
                        for (int rangeIdx = 0; rangeIdx < sampleRanges.length; rangeIdx++) {
                            DataRange r = sampleRanges[rangeIdx];
                            if (Double.isInfinite(r.getMin()) || Double.isInfinite(r.getMax())) {
                                sampleRanges = null;
                                break;
                            }
                        }
                    }
                    if (sampleRanges != null) {
                        break;
                    }
                }

                if (sampleRanges != null) {
                    for (SingleBandedImage sbi : sortedImages) {
                        AreaImageFlatField aiff = (AreaImageFlatField) sbi;
                        aiff.setSampleRanges(sampleRanges);
                        aiff.setDomainIfNeeded(domainSet);
                    }
                }
            }

            SingleBandedImage[] imageArray =
                (SingleBandedImage[]) sortedImages.toArray(
                    new SingleBandedImage[sortedImages.size()]);
            FunctionType imageFunction =
                (FunctionType) imageArray[0].getType();
            FunctionType ftype = new FunctionType(RealType.Time,
                                     imageFunction);
            return new ImageSequenceImpl(ftype, imageArray);
        } catch (Exception exc) {
            throw new ucar.unidata.util.WrapperException(exc);
        }
    }
    
    /**
     * Create the single image defined by the given 
     * {@link ucar.unidata.data.imagery.AddeImageDescriptor AddeImageDescriptor}.
     *
     * @param aid Holds image directory and location of the desired image.
     * @param rangeType {@literal "rangeType"} to use (if non-{@code null}).
     * @param fromSequence _more_
     * @param readLabel 
     * @param subset geographical subsetting info
     *
     * @return The data.
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException VisAD problem
     */
    
    private SingleBandedImage makeImage(AddeImageDescriptor aid,
                                        MathType rangeType,
                                        boolean fromSequence, 
                                        String readLabel, DataSelection subset)
            throws VisADException, RemoteException {
        
        if (aid == null) {
            return null;
        }

        logger.trace("incoming src={} DateTime={} readLabel={}", new Object[] { aid.getSource(), aid.getImageTime(), readLabel });
        String src = aid.getSource();

        Hashtable props = subset.getProperties();
        
//        String areaDirectoryKey = getKey(src, "POS");
        String areaDirectoryKey = null;
        if (aid.getIsRelative()) {
            areaDirectoryKey = getKey(src, "POS");
        } else {
            areaDirectoryKey = aid.getImageTime().toString();
//            areaDirectoryKey = getKey(src, "TIME");
        }

        // it only makes sense to set the following properties for things
        // coming from an ADDE server
        if (!isFromFile(aid)) {
            if (props.containsKey("PLACE")) {
                src = replaceKey(src, "PLACE", props.get("PLACE"));
            }
            if (props.containsKey("LATLON")) { 
                src = replaceKey(src, "LINELE", "LATLON", props.get("LATLON"));
            }
            if (props.containsKey("LINELE")) {
                src = removeKey(src, "LATLON");
                src = replaceKey(src, "LINELE", props.get("LINELE"));
            }
            if (props.containsKey("MAG")) {
                src = replaceKey(src, "MAG", props.get("MAG"));
            }
        }

        aid.setSource(src);

        SingleBandedImage result;
        result = (SingleBandedImage)getCache(src);
        if (result != null) {
            setDisplaySource(src, props);
            return result;
        }

        // For now handle non ADDE URLs here
        try {
            AddeImageInfo aii = aid.getImageInfo();
            AreaDirectory areaDir = null;
            try {
                if (aii != null) {
                    logger.trace("imageinfo={}", aii.toString());
                    if (currentDirs != null) {
                        int pos = Math.abs(aii.getDatasetPosition());
                        int band = 0;
                        String bandString = aii.getBand();
                        if ((bandString != null) && !AddeURL.ALL.equals(bandString)) {
                            band = Integer.parseInt(bandString);
                        }
                        // TODO: even though the band is non-zero we might only 
                        // get back one band
                        band = 0;
                        areaDir = currentDirs[currentDirs.length - pos - 1][band];
                    } else {
                        // If its absolute time then just use the AD from the descriptor
                        if ((aii.getStartDate() != null) || (aii.getEndDate() != null)) {
                            areaDir = aid.getDirectory();
                        } else {
                        }
                    }
                } else {
                    logger.trace("uh oh");
                }
            } catch (Exception exc) {
                LogUtil.printMessage("error looking up area dir");
                logger.error("error looking up area dir", exc);
                return null;
            }

            if (areaDir == null) {
                areaDir = aid.getDirectory();
            }

            if (!getCacheDataToDisk()) {
                areaDir = null;
            }

            if (!fromSequence || (aid.getIsRelative() && (currentDirs == null))) {
                areaDir = null;
            }

            if (areaDir != null) {
                if (isFromFile(aid)) {
                  if (rangeType == null) {
                      result = AreaImageFlatField.createImmediate(aid, readLabel);
                  } else {
                      // Else, pass in the already created range type
                      result  = AreaImageFlatField.create(aid, areaDir, rangeType, readLabel);
                  }
                }

            } else {
                src = aid.getSource();
                try {
                    savePlace = this.laLoSel.getPlace();
                    saveLat = this.laLoSel.getLatitude();
                    saveLon = this.laLoSel.getLongitude();
                    saveNumLine = this.laLoSel.getNumLines();
                    saveNumEle = this.laLoSel.getNumEles();
                    saveLineMag = this.laLoSel.getLineMag();
                    saveEleMag = this.laLoSel.getElementMag();
                } catch (Exception e) {
                    logger.error("error reading from laLoSel", e);
                    this.laLoSel.setPlace(savePlace);
                    this.laLoSel.setLatitude(saveLat);
                    this.laLoSel.setLongitude(saveLon);
                    this.laLoSel.setNumLines(saveNumLine);
                    this.laLoSel.setNumEles(saveNumEle);
                    this.laLoSel.setLineMag(saveLineMag);
                    this.laLoSel.setElementMag(saveEleMag);
                }

                src = replaceKey(src, PLACE_KEY, savePlace);
                src = removeKey(src, LINELE_KEY);
                if (getKey(src, LATLON_KEY).length() != 0) {
                    String latStr = Double.toString(saveLat);
                    if (latStr.length() > 8) {
                        latStr = latStr.substring(0,7);
                    }
                    String lonStr = Double.toString(saveLon);
                    if (lonStr.length() > 9) {
                        lonStr = lonStr.substring(0,8);
                    }
                    src = replaceKey(src, LATLON_KEY, latStr + ' ' + lonStr);
                }
                src = replaceKey(src, SIZE_KEY, saveNumLine + ' ' + saveNumEle);
                src = replaceKey(src, MAG_KEY, saveLineMag + ' ' + saveEleMag);
            }

            AreaAdapter aa = new AreaAdapter(src, false);
            logger.trace("Getting a new aa={} for src=: {}", aa, src);
            areaDir = previewDir;
            result = aa.getImage();

            putCache(src, result);
            aid.setSource(src);
            iml.add(aid);
            setImageList(iml);
            setDisplaySource(src, props);
            return result;

        } catch (java.io.IOException ioe) {
            throw new VisADException("Error creating AreaAdapter", ioe);
        }
    }
    
    /**
     * Make a parameter name for the BandInfo
     *
     * @param bi    the BandInfo in question
     *
     * @return  a name for the parameter
     */
    
    private static String makeBandParam(BandInfo bi) {
        return new StringBuilder()
            .append(bi.getSensor())
            .append("_Band")
            .append(bi.getBandNumber())
            .append('_')
            .append(bi.getPreferredUnit()).toString();
    }
    
    private static String makeBandParam(AddeImageDescriptor descriptor) {
        AreaDirectory areaDir = descriptor.getDirectory();
        if (areaDir == null) {
            throw new NullPointerException("No AREA directory!");
        }
        return new StringBuilder()
            .append(areaDir.getSensorID())
            .append("_Band")
            .append(areaDir.getBands()[0])
            .append('_')
            .append(areaDir.getCalibrationType()).toString();
    }

    /**
     * Get a list of descriptors from the choice and subset
     *
     * @param dataChoice  Data choice
     * @param subset  subsetting info
     *
     * @return  list of descriptors matching the selection
     */
    
    public List getDescriptors(DataChoice dataChoice, DataSelection subset) {
//        logger.trace("choice={} subset props={} geo={}", new Object[] { dataChoice, subset.getProperties(), subset.getGeoSelection() });
        int linRes = this.lineResolution;
        int eleRes = this.elementResolution;
        int newLinRes = linRes;
        int newEleRes = eleRes;
//        List<TwoFacedObject> times = getTimesFromDataSelection(subset, dataChoice);
        List times = getTimesFromDataSelection(subset, dataChoice);
        boolean usingTimeDriver = ((subset != null) && (subset.getTimeDriverTimes() != null));
        if (usingTimeDriver) {
            times = subset.getTimeDriverTimes();
        }

//        if (dataChoice.getDataSelection() == null) {
//            logger.trace("setting datasel!");
//            dataChoice.setDataSelection(subset);
//        }
        if ((times == null) || times.isEmpty()) {
            times = imageTimes;
        }
//        List<AddeImageDescriptor> descriptors = new ArrayList<AddeImageDescriptor>(times.size());
        List descriptors = new ArrayList();

        if (usingTimeDriver) {
            if (imageList.isEmpty()) {
                return imageList;
            }
            AddeImageDescriptor aid = getDescriptor(imageList.get(0));
            if (aid.getImageInfo() != null) {
                try {
                    AddeImageInfo aii =
                        (AddeImageInfo) aid.getImageInfo().clone();
                    // set the start and end dates
                    Collections.sort(times);
                    DateTime start = (DateTime) times.get(0);
                    DateTime end   = (DateTime) times.get(times.size() - 1);
                    // In ADDE, you can't specify something like DAY=2011256 2011257 TIME=23:45:00 01:45:00
                    // and expect that to be 2011256/23:45 to 2011257 01:45.  Time ranges are on a per day
                    // basis.  So, we see if the starting time is a different day than the ending day and if so,
                    // we set the start time to be 00Z on the first day an 23:59Z on the end day.
                    // Even worse is that for archive datasets, you can't span multiple days.  So make separate
                    // requests for each day.
                    String       startDay = UtcDate.getYMD(start);
                    String       endDay   = UtcDate.getYMD(end);
                    List<String> days     = new ArrayList<>(times.size());
                    if (!startDay.equals(endDay)) {
                        days = getUniqueDayStrings(times);
                    } else {
                        days.add(startDay);
                    }
                    Map<DateTime, AreaDirectory> dateDir = new HashMap<>(days.size());
                    List<DateTime> dirTimes = new ArrayList<>(days.size() * 10);
                    for (String day : days) {
                        startDay = day + " 00:00:00";
                        endDay   = day + " 23:59:59";
                        start = DateTime.createDateTime(startDay,
                            DateTime.DEFAULT_TIME_FORMAT);
                        end = UtcDate.createDateTime(endDay,
                            DateTime.DEFAULT_TIME_FORMAT);
                        aii.setStartDate(new Date((long) (start
                            .getValue(CommonUnit.secondsSinceTheEpoch) * 1000)));
                        aii.setEndDate(new Date((long) (end
                            .getValue(CommonUnit.secondsSinceTheEpoch) * 1000)));
                        // make the request for the times (AreaDirectoryList)
                        aii.setRequestType(AddeURL.REQ_IMAGEDIR);
                        AreaDirectoryList ad;
                        try {  // we may be asking for a date that doesn't exist
                            ad = new AreaDirectoryList(aii.getURLString());
                        } catch (AreaFileException afe) {
                            // If there's an error, we just ignore it.  In the
                            // end, the descriptor list will be empty if there is no
                            // data for any of the days.
                            continue;

                            // TODO: This is a hack because different servers return different
                            // messages.  AREA and GINI servers seem to have "no images" in the
                            // exception message when there are no images.
                            //String message = afe.getMessage().toLowerCase();
                            //if (message.indexOf("no images") >= 0 ||
                            //    message.indexOf("error generating list of files") >= 0) {
                            //    continue;
                            //} else {
                            //    throw afe;
                            //}

                        }
                        AreaDirectory[][] dirs = ad.getSortedDirs();
                        for (int d = 0; d < dirs.length; d++) {
                            AreaDirectory dir = dirs[d][0];
                            DateTime dirTime =
                                new DateTime(dir.getNominalTime());
                            dateDir.put(dirTime, dir);
                            dirTimes.add(dirTime);
                        }
                    }
                    List<DateTime> matchedTimes = selectTimesFromList(subset,
                        dirTimes, times);
                    for (DateTime dirTime : matchedTimes) {
                        AreaDirectory dir = dateDir.get(dirTime);
                        // shouldn't happen, but what the hey
                        if (dir == null) {
                            continue;
                        }
                        AddeImageInfo newaii =
                            (AddeImageInfo) aid.getImageInfo().clone();
                        newaii.setRequestType(aii.REQ_IMAGEDATA);
                        newaii.setStartDate(dir.getNominalTime());
                        newaii.setEndDate(dir.getNominalTime());
                        setBandInfo(dataChoice, newaii);
                        AddeImageDescriptor newaid =
                            new AddeImageDescriptor(dir,
                                newaii.getURLString(), newaii);
                        newaid.setIsRelative(false);
                        descriptors.add(newaid);
                    }
                } catch (CloneNotSupportedException cnse) {
                    logger.error("Unable to clone AddeImageInfo (aii)", cnse);
                } catch (VisADException vader) {
                    logger.error("Unable to get date values", vader);
                } catch (AreaFileException afe) {
                    logger.error("Unable to make ADDE request", afe);
                } catch (Exception excp) {
                    logger.error("Exception occurred while handling time driver", excp);
                }
                // we do this so save data local will work.  However, if
                // this then gets set to be the time driver, it would not
                // necessarily be correct
                imageList = descriptors;
                return descriptors;
            } else if (imageList != null) {
                return imageList;
            }
        }

        int choiceBandNum = ((BandInfo) dataChoice.getId()).getBandNumber();
        int choiceSensorId = ((BandInfo) dataChoice.getId()).getSensor();
        String choicePrefUnit = ((BandInfo) dataChoice.getId()).getPreferredUnit();
        for (Iterator iter = times.iterator(); iter.hasNext(); ) {
            Object time  = iter.next();
            AddeImageDescriptor found = null;
            AddeImageDescriptor foundTimeMatch = null;
            if (saveImageList.isEmpty()) {
                saveImageList = getImageList();
            }
            for (Iterator iter2 = saveImageList.iterator(); iter2.hasNext(); ) {
                AddeImageDescriptor aid = getDescriptor(iter2.next());
                if (aid != null) {
                    if (aid.getIsRelative()) {
                        Object id;
                        if (time instanceof TwoFacedObject) {
                            id = ((TwoFacedObject)time).getId();
                        } else {
                            id = time;
                        }
                        if ((id instanceof Integer) && ((Integer)id).intValue() == aid.getRelativeIndex()) {
                            found = aid;
                            break;
                        }
                    } else {
                        int aidBand = aid.getDirectory().getBands()[0];
                        int aidSensorId = aid.getDirectory().getSensorID();
                        String calType = aid.getDirectory().getCalibrationType();
                        if (foundTimeMatch == null && aid.getImageTime().equals(time)) {
                            logger.trace("found time match {}", time);
                            foundTimeMatch = aid;
                        }
                        if (aid.getImageTime().equals(time) && choiceBandNum == aidBand && choiceSensorId == aidSensorId && choicePrefUnit.equals(calType)) {
                            // the problem is here!
                            logger.trace("found aid={} src={}", makeBandParam(aid), aid.getSource());
                            logger.trace("target info: param={}", dataChoice.getName());
                            found = aid;
                            break;
                        }
                    }
                }
            }

            if (found == null && foundTimeMatch != null) {
                logger.trace("good enough!?");
                found = foundTimeMatch;
            }
            
            if (found != null) {
                try {
                    AddeImageDescriptor desc = new AddeImageDescriptor(found);
                    // Sometimes we might have a null imageinfo
                    if (desc.getImageInfo() != null) {
                        AddeImageInfo aii =
                            (AddeImageInfo) desc.getImageInfo().clone();
                        BandInfo bi = (BandInfo) dataChoice.getId();
                        List<BandInfo> bandInfos =
                            (List<BandInfo>) getProperty(PROP_BANDINFO, (Object) null);
                        boolean hasBand = true;
                        // If this data source has been changed after we have create a display 
                        // then the possibility exists that the bandinfo contained by the incoming
                        // data choice might not be valid. If it isn't then default to the first 
                        // one in the list
                        if (bandInfos != null) {
                            hasBand = bandInfos.contains(bi);
                            if(!hasBand) {
                            }
                            if(!hasBand && bandInfos.size() > 0) {
                                bi = bandInfos.get(0);
                            } else {
                                //Not sure what to do here.
                            }
                        }
                        aii.setBand("" + bi.getBandNumber());
                        aii.setPlaceValue("ULEFT");

                        try {
                            AddeImageDescriptor newAid = new AddeImageDescriptor(aii.getURLString());
                            AreaDirectory newAd = newAid.getDirectory();
                            newLinRes = newAd.getValue(AreaFile.AD_LINERES);
                            newEleRes = newAd.getValue(AreaFile.AD_ELEMRES);
                        } catch (Exception e) {
                            logger.error("resetting resolution", e);
                        }

                        double[][] projCoords = new double[2][2];
                        try {
                            AreaDirectory ad = desc.getDirectory();
                            double lin = (double) ad.getValue(AreaFile.AD_STLINE);
                            double ele = (double) ad.getValue(AreaFile.AD_STELEM);
                            aii.setLocateKey("LINELE");
                            aii.setLocateValue((int)lin + " " + (int)ele);
                            projCoords[0][0] = lin;
                            projCoords[1][0] = ele;
                            lin += (double)ad.getValue(AreaFile.AD_NUMLINES);
                            ele += (double)ad.getValue(AreaFile.AD_NUMELEMS);
                            projCoords[0][1] = lin;
                            projCoords[1][1] = ele;
                        } catch (Exception e) {
                            logger.error("problem with adjusting projCoords?", e);
                            return descriptors;
                        }
                        int lins = Math.abs((int)(projCoords[1][1] - projCoords[1][0]));
                        int eles = Math.abs((int)(projCoords[0][1] - projCoords[0][0]));
                        lins = lins*linRes/newLinRes;
                        if (this.lineMag > 0) {
                            lins *= this.lineMag;
                        } else {
                            lins /= -this.lineMag;
                        }

                        eles = eles*eleRes/newEleRes;

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

    private List<AreaDirectory> getPreviewDirectories(final AddeImageDescriptor imageDescriptor) {
        List<AreaDirectory> directories = new ArrayList<AreaDirectory>(imageTimes.size());
        
        return directories;
    }
    
//    private AreaDirectory getPreviewDirectory(AddeImageDescriptor aid) {
//        AreaDirectory directory = aid.getDirectory();
//        AddeImageDescriptor descriptor = null;
//        int times = imageTimes.size();
//        if (times == 1) {
//            logger.trace("only looking for a single time; returning AreaDirectory grabbed from incoming AddeImageDescriptor. directory={}", directory);
//            return directory;
//        }
//        String src = aid.getSource();
//        logger.trace("URL for incoming AddeImageDescriptor: {}", src);
//        src = removeKey(src, LATLON_KEY);
//        src = removeKey(src, LINELE_KEY);
//        src = removeKey(src, PLACE_KEY);
//        src = removeKey(src, SIZE_KEY);
//        src = removeKey(src, UNIT_KEY);
//        src = removeKey(src, MAG_KEY);
//        src = removeKey(src, SPAC_KEY);
//        src = removeKey(src, NAV_KEY);
//        src = removeKey(src, AUX_KEY);
//        src = removeKey(src, DOC_KEY);
//
//        int maxLine = 0;
//        int maxEle = 0;
//        int imageSize = 0;
//        src = src.replace("imagedata", "imagedir");
//        boolean isRelative = aid.getIsRelative();
//        for (int i=0; i<times; i++) {
//            if (isRelative) {
//                src = replaceKey(src, "POS", Integer.valueOf(i).toString());
//            } else {
//                DateTime dt = (DateTime)imageTimes.get(i);
//                String timeStr = dt.timeString();
//                timeStr = timeStr.replace("Z", " ");
//                src = removeKey(src, "POS");
//                src = replaceKey(src, "TIME", timeStr + timeStr + "I");
//            }
//            try {
//                logger.trace("attempting to create AreaDirectoryList using src={}", src);
//                AreaDirectoryList dirList = new AreaDirectoryList(src);
//                List ad = dirList.getDirs();
//                AreaDirectory areaDir = (AreaDirectory)ad.get(0);
//                logger.trace("created AreaDirectory: {}", areaDir);
//                int lines = areaDir.getLines();
//                int eles =  areaDir.getElements();
//                if (imageSize < lines*eles) {
//                    imageSize = lines * eles;
//                    maxLine = lines;
//                    maxEle = eles;
//                    directory = areaDir;
//                    descriptor = new AddeImageDescriptor(src);
//                }
//            } catch (Exception e) {
//                logger.error("problem when dealing with AREA directory", e);
//            }
//        }
//        logger.trace("returning AreaDirectory: {}", directory);
//        logger.trace("could return AddeImageDescriptor:\nisRelative={}\nrelativeIndex={}\ntime={}\ndirectory={}\n", new Object[] { descriptor.getIsRelative(), descriptor.getRelativeIndex(), descriptor.getImageTime(), descriptor.getDirectory()});
//        return directory;
//    }

    private AddeImageDescriptor getPreviewDirectory(AddeImageDescriptor aid, DataChoice dataChoice) {

        // Used to detect domain shifts for sectors like ABI MESO
        float domainCenterLat = -1.0f;
        float domainCenterLon = -1.0f;

        AreaDirectory directory = aid.getDirectory();
        AddeImageDescriptor descriptor = null;
        int times = imageTimes.size();
        if (times == 1) {
            logger.trace("only looking for a single time; returning AreaDirectory grabbed from incoming AddeImageDescriptor. directory={}", directory);
//            return directory;
            return aid;
        }

        String src = aid.getSource();
        logger.trace("URL for incoming AddeImageDescriptor: {}", src);

        src = removeKey(src, LATLON_KEY);
        src = removeKey(src, LINELE_KEY);
        src = removeKey(src, PLACE_KEY);
        src = removeKey(src, SIZE_KEY);
        src = removeKey(src, UNIT_KEY);
        src = removeKey(src, MAG_KEY);
        src = removeKey(src, SPAC_KEY);
        src = removeKey(src, NAV_KEY);
        src = removeKey(src, AUX_KEY);
        src = removeKey(src, DOC_KEY);

        int maxLine = 0;
        int maxEle = 0;
        int imageSize = 0;
        int prevSize = 0;
        src = src.replace("imagedata", "imagedir");
        boolean isRelative = aid.getIsRelative();

        // Note the index if we detect a domain shift, we will cut the loop off there
        int domainShiftIndex = 0;

        List<String> previewUrls = new ArrayList<String>(times);

        if (isRelative) {
        	
        	// TJJ Mar 2015 - (1891, R14)
        	// Iterate through list to determine relative position different
        	// if the objects are VisAD DateTime instead of TwoFacedObject
        	
        	boolean isTwoFaced = false;
        	List<?> tmpList = getAllDateTimes();
        	if ((tmpList != null) && (! tmpList.isEmpty())) {
        		Object firstItem = tmpList.get(0);
        		if (firstItem instanceof TwoFacedObject) isTwoFaced = true;
        	}
        	
        	int maxIndex;
			if (isTwoFaced) {
				maxIndex = Integer.MIN_VALUE;
				for (TwoFacedObject tfo : (List<TwoFacedObject>) getAllDateTimes()) {
					int relativeIndex = ((Integer) tfo.getId()).intValue();
					if (relativeIndex > maxIndex) {
						maxIndex = relativeIndex;
					}
				}
			} else {
				maxIndex = Integer.MIN_VALUE;
				int relativeIndex = 0;
				double maxTime = Double.MIN_VALUE;
				for (DateTime dt : (List<DateTime>) getAllDateTimes()) {
					double d = dt.getValue();
					if (d > maxTime) {
						maxIndex = relativeIndex;
						maxTime = d;
					}
					relativeIndex++;
				}
			}
            
//            TwoFacedObject tfo = (TwoFacedObject)this.getAllDateTimes().get(0);
            // negate maxIndex so we can get things like POS=0 or POS=-4
            maxIndex = 0 - maxIndex;
            logger.trace("using maxIndex={}", maxIndex);
            src = replaceKey(src, "POS", Integer.toString(maxIndex));
            previewUrls.add(src);
            
        } else {
            for (int i = 0; i < times; i++) {
                DateTime dt = (DateTime)imageTimes.get(i);
                String timeStr = dt.timeString();
                timeStr = timeStr.replace("Z", " ");
                src = removeKey(src, "POS");
                src = replaceKey(src, "TIME", timeStr + timeStr + 'I');
                logger.trace("using time value: ", timeStr + timeStr + 'I');
                logger.trace("added absolute time preview url: {}", src);
                previewUrls.add(src);
            }
        }
        
        if (! isRelative && (previewUrls.size() == 1)) {
            logger.trace("only a single previewUrl; returning '{}'", previewUrls.get(0));
            return new AddeImageDescriptor(previewUrls.get(0));
        } else {
            logger.trace("preparing to examine previewUrls={}", previewUrls);
        }
        
        // TJJ Aug 2020
        // Look for GEO-based domain shifts (e.g. MESO sector moves mid-loop)
        // The first image checked will initialize domain center lat.  After
        // that, anything different is a domain shift

        if (isABISensor) {

            int urlIdx = 0;
            for (String urlStr : previewUrls) {

                AddeImageDescriptor tmpAID = new AddeImageDescriptor(urlStr);
                AreaDirectory tmpAD = tmpAID.getDirectory();

                if (domainCenterLat == -1.0f) {
                    domainCenterLat = (float) tmpAD.getCenterLatitude();
                } else {
                    if ((float) tmpAD.getCenterLatitude() != domainCenterLat) {
                        domainCenterLat = (float) tmpAD.getCenterLatitude();
                        logger.info("Domain shift, set LAT to: " + domainCenterLat);
                        domainShiftDetected = true;
                        domainShiftIndex = urlIdx;
                    }
                }
                // The first image checked will initialize domain center lon
                if (domainCenterLon == -1.0f) {
                    domainCenterLon = (float) tmpAD.getCenterLongitude();
                } else {
                    if ((float) tmpAD.getCenterLongitude() != domainCenterLon) {
                        domainCenterLon = (float) tmpAD.getCenterLongitude();
                        logger.info("Domain shift, set LON to: " + domainCenterLon);
                        domainShiftDetected = true;
                        domainShiftIndex = urlIdx;
                    }
                }

                urlIdx++;

            }

        }

        try {
            
            // TJJ Nov 2017
            // As an interim speedup, for Inq #2496, we are going to binary search the list
            // intstead of iterating through every single URL to make large loop times 
            // (e.g. 50 GOES-16 MESO images) more tolerable.
            
            if (isGeoSensor && (previewUrls.size() > 1)) {

                int loIdx = 0;
                int hiIdx = previewUrls.size() - 1;
                boolean directionInc = true;
                boolean sizeMatch = false;
                int iterations = 0;

                while (! sizeMatch) {

                    String previewUrl = null;
                    if (directionInc) {
                        previewUrl = previewUrls.get(loIdx);
                        loIdx++;
                        directionInc = false;
                    } else {
                        previewUrl = previewUrls.get(hiIdx);
                        hiIdx--;
                        directionInc = true;
                    }

                    // Sanity check - if we get to the middle, we just quit and use what we found
                    if ((loIdx > hiIdx) && (! isRelative)) break;

                    logger.trace("attempting to create AreaDirectoryList using previewUrl={}", previewUrl);
                    AreaDirectoryList directoryList = new AreaDirectoryList(previewUrl);
                    logger.trace("created directoryList! size={}\n{}", directoryList.getDirs().size(), directoryList);
                    List<AreaDirectory> areaDirectories = (List<AreaDirectory>) directoryList.getDirs();

                    for (int i = 0; i < areaDirectories.size(); i++) {
                        iterations++;
                        AreaDirectory areaDirectory = areaDirectories.get(i);
                        String pos = Integer.toString(0 - i);
                        int lines = areaDirectory.getLines();
                        int elements = areaDirectory.getElements();
                        int currentDimensions = lines * elements;
                        logger.trace("image pos={} lines={} elements={} lat={} lon={} startTime='{}' nominalTime='{}' currentDimensions={} areaDirectory={}", new Object[] { pos, lines, elements, areaDirectory.getCenterLatitude(), areaDirectory.getCenterLongitude(), areaDirectory.getStartTime(), areaDirectory.getNominalTime(), currentDimensions, areaDirectory });
                        String key = null;
                        if (isRelative) {
                            key = pos;
                        } else {
                            try {
                                DateTime reformatted = new DateTime(areaDirectory.getNominalTime());
                                key = reformatted.toString();
                            } catch (VisADException e) {
                                logger.error("could not reformat time string='"+areaDirectory.getNominalTime().toString()+"'", e);
                                key = areaDirectory.getNominalTime().toString();
                            }
                        }
                        requestIdToDirectory.put(key, areaDirectory);

                        if (imageSize < currentDimensions) {

                            imageSize = currentDimensions;
                            maxLine = lines;
                            maxEle = elements;
                            directory = areaDirectory;
                            // TODO(jon): should be grabbing coord sys from chooser setting (HOW TO DO THAT!?)
                            String latlonString = areaDirectory.getCenterLatitude() + " " + areaDirectory.getCenterLongitude() + " E";
                            String largestPreviewUrl = previewUrl.replace("imagedir", "imagedata");
                            largestPreviewUrl = replaceKey(largestPreviewUrl, "PLACE", "CENTER");
                            largestPreviewUrl = replaceKey(largestPreviewUrl, "LATLON", latlonString);
                            Hashtable dataSourceProperties = this.getProperties();
                            if (dataSourceProperties.containsKey("navigation")) {
                                largestPreviewUrl = replaceKey(largestPreviewUrl, "NAV", dataSourceProperties.get("navigation"));
                            }

                            if (isRelative) {
                                largestPreviewUrl = replaceKey(largestPreviewUrl, "POS", pos);
                            } else {
                                logger.trace("need to set DAY and TIME keywords for absolute times!");
                            }

                            logger.trace("found new max size! old={} new={} url={}", new Object[] { imageSize, currentDimensions, largestPreviewUrl });
                            descriptor = new AddeImageDescriptor(areaDirectory, largestPreviewUrl);

                        }

                        if (prevSize == currentDimensions) {
                            logger.debug("Exiting Preview URL loop after " + iterations + " iterations...");
                            sizeMatch = true;
                        }
                        prevSize = currentDimensions;
                    }
                }
            } else {
                
                // Old way - go through all preview URLs looking for largest geographic coverage

                for (String previewUrl : previewUrls) {

                    logger.trace("attempting to create AreaDirectoryList using previewUrl={}", previewUrl);
                    AreaDirectoryList directoryList = new AreaDirectoryList(previewUrl);
                    logger.trace("created directoryList! size={}\n{}", directoryList.getDirs().size(), directoryList);
                    List<AreaDirectory> areaDirectories = (List<AreaDirectory>) directoryList.getDirs();

                    for (int i = 0; i < areaDirectories.size(); i++) {

                        AreaDirectory areaDirectory = areaDirectories.get(i);
                        String pos = Integer.toString(0 - i);
                        int lines = areaDirectory.getLines();
                        int elements = areaDirectory.getElements();
                        int currentDimensions = lines * elements;
                        logger.trace("image pos={} lines={} elements={} lat={} lon={} startTime='{}' nominalTime='{}' currentDimensions={} areaDirectory={}", new Object[] { pos, lines, elements, areaDirectory.getCenterLatitude(), areaDirectory.getCenterLongitude(), areaDirectory.getStartTime(), areaDirectory.getNominalTime(), currentDimensions, areaDirectory });
                        String key = null;
                        if (isRelative) {
                            key = pos;
                        } else {
                            try {
                                DateTime reformatted = new DateTime(areaDirectory.getNominalTime());
                                key = reformatted.toString();
                            } catch (VisADException e) {
                                logger.error("could not reformat time string='"+areaDirectory.getNominalTime().toString()+"'", e);
                                key = areaDirectory.getNominalTime().toString();
                            }
                        }
                        requestIdToDirectory.put(key, areaDirectory);
                        if (imageSize < currentDimensions) {

                            imageSize = currentDimensions;
                            maxLine = lines;
                            maxEle = elements;
                            directory = areaDirectory;
                            // TODO(jon): should be grabbing coord sys from chooser setting (HOW TO DO THAT!?)
                            String latlonString = areaDirectory.getCenterLatitude() + " " + areaDirectory.getCenterLongitude() + " E";
                            String largestPreviewUrl = previewUrl.replace("imagedir", "imagedata");
                            largestPreviewUrl = replaceKey(largestPreviewUrl, "PLACE", "CENTER");
                            largestPreviewUrl = replaceKey(largestPreviewUrl, "LATLON", latlonString);
                            Hashtable dataSourceProperties = this.getProperties();
                            if (dataSourceProperties.containsKey("navigation")) {
                                largestPreviewUrl = replaceKey(largestPreviewUrl, "NAV", dataSourceProperties.get("navigation"));
                            }

                            if (isRelative) {
                                largestPreviewUrl = replaceKey(largestPreviewUrl, "POS", pos);
                            } else {
                                logger.trace("need to set DAY and TIME keywords for absolute times!");
                            }

                            logger.trace("found new max size! old={} new={} url={}", new Object[] { imageSize, currentDimensions, largestPreviewUrl });
                            descriptor = new AddeImageDescriptor(areaDirectory, largestPreviewUrl);

                        }
                    }
                }
            }
        } catch (AreaFileException areaException) {
            logger.error("problem when dealing with AREA directory", areaException);
        }
//        for (int i = 0; i < times; i++) {
//            if (isRelative) {
//                src = replaceKey(src, "POS", Integer.valueOf(i).toString());
//            } else {
//                DateTime dt = (DateTime)imageTimes.get(i);
//                String timeStr = dt.timeString();
//                timeStr = timeStr.replace("Z", " ");
//                src = removeKey(src, "POS");
//                src = replaceKey(src, "TIME", timeStr + timeStr + "I");
//            }
//            // don't forget to NOT negate POS=0
//            try {
//                logger.trace("attempting to create AreaDirectoryList using src={}", src);
//                AreaDirectoryList dirList = new AreaDirectoryList(src);
//                List ad = dirList.getDirs();
//                AreaDirectory areaDir = (AreaDirectory)ad.get(0);
//                logger.trace("created AreaDirectory: {}", areaDir);
//                int lines = areaDir.getLines();
//                int eles =  areaDir.getElements();
//                logger.trace("image lines={} eles={} for src={}", new Object[] { lines, eles, src });
//                if (imageSize < lines*eles) {
//                    logger.trace("found new max size! old={} new={}", imageSize, lines*eles);
//                    imageSize = lines * eles;
//                    maxLine = lines;
//                    maxEle = eles;
//                    directory = areaDir;
//                    descriptor = new AddeImageDescriptor(src);
//                }
//            } catch (Exception e) {
//                logger.error("problem when dealing with AREA directory", e);
//            }
//        }
//        

        // If domain shift was detected, keep only the pre-shift times for derived data
        logger.info("domainShiftIndex: " + domainShiftIndex);

        logger.trace("returning AreaDirectory: {}", directory);
//        logger.trace("could return AddeImageDescriptor:\nisRelative={}\nrelativeIndex={}\ntime={}\ndirectory={}\n", new Object[] { descriptor.getIsRelative(), descriptor.getRelativeIndex(), descriptor.getImageTime(), descriptor.getDirectory()});
        return descriptor;
    }

    private String getServer(String urlString) {
        int ix = urlString.indexOf("//") + 2;
        String temp = urlString.substring(ix);
        ix = temp.indexOf("/");
        String retStr = temp.substring(0, ix);
        return retStr;
    }

    public void setDisplaySource(String src, Hashtable props) {
         if (!props.isEmpty()) {
             Enumeration propEnum = props.keys();
             for (int i=0; propEnum.hasMoreElements(); i++) {
                 String key = propEnum.nextElement().toString();
                 Object val = props.get(key);
                 if (getKey(src, key).length() != 0) {
                     src = replaceKey(src, key, val);
                 }
             }
         }
         this.displaySource = src;
         String unit = getKey(src, UNIT_KEY);
         if (unit.length() != 0) {
             sourceProps.put(UNIT_KEY.toUpperCase(), unit);
         }
    }

    public String getDisplaySource() {
        return this.displaySource;
    }


    private float[] getLineEleResolution(AreaDirectory ad) {
        logger.trace("ad: {} sensor: {}", ad, ad.getSensorID());
        float[] res = {(float)1.0, (float)1.0};
        int sensor = ad.getSensorID();
        List lines = null;
        try {
            String buff = getUrl();

            lines = readTextLines(buff);
            if (lines == null) {
                return res;
            }

            int gotit = -1;
            String[] cards = StringUtil.listToStringArray(lines);
            logger.trace("cards: {}", cards);

            for (int i = 0; i < cards.length; i++) {
                if ( ! cards[i].startsWith("Sat ")) continue;
                StringTokenizer st = new StringTokenizer(cards[i]," ");
                String temp = st.nextToken();  // throw away the key
                int m = st.countTokens();
                for (int k = 0; k < m; k++) {
                    int ss = Integer.parseInt(st.nextToken().trim());
                    if (ss == sensor) {
                        gotit = i;
                        break;
                    }
                }

                if (gotit != -1) {
                    break;
                }
            }

            if (gotit == -1) {
                return res;
            }

            int gotSrc = -1;
            for (int i = gotit; i < cards.length; i++) {
                if (cards[i].startsWith("EndSat")) {
                    return res;
                }
                if (!cards[i].startsWith("B") ) {
                    continue;
                }
                StringTokenizer tok = new StringTokenizer(cards[i]);
                String str = tok.nextToken();
                str = tok.nextToken();
                Float flt = Float.valueOf(str);
                res[0] = flt.floatValue();
                str = tok.nextToken();
                flt = Float.valueOf(str);
                res[1] = flt.floatValue();
                return res;
            }
        } catch (Exception e) {
            logger.error("problem getting the line+element rez", e);
        }
        return res;
    }

    /**
     * Read the adde text url and return the lines of text.
     * If unsuccessful return null.
     *
     * @param url adde url to a text file
     *
     * @return List of lines or {@code null} if in error.
     */
    
    protected List readTextLines(String url) {
        AddeTextReader reader = new AddeTextReader(url);
        List lines = null;
        if ("OK".equals(reader.getStatus())) {
            lines = reader.getLinesOfText();
        }
        return lines;
    }

    /**
     * Create the first part of the ADDE request URL
     * 
     * @return ADDE URL prefix
     */
    
    protected String getUrl() {
        String str = source;
        str = str.replaceFirst("imagedata", "text");
        int indx = str.indexOf("VERSION");
        str = str.substring(0, indx);
        str = str.concat("file=SATBAND");
        return str;
    }

    public Hashtable getSourceProps() {
        return this.sourceProps;
    }

    public void setSourceProps(Hashtable sourceProps) {
        this.sourceProps = sourceProps;
    }

    public String getChoiceName() {
        return this.choiceName;
    }

    public void setChoiceName(String choiceName) {
        this.choiceName = choiceName;
    }

    public String getSavePlace() {
        return this.savePlace;
    }

    public void setSavePlace(String savePlace) {
        this.savePlace = savePlace;
    }

    public double getSaveLat() {
        return this.saveLat;
    }

    public void setSaveLat(double saveLat) {
        this.saveLat = saveLat;
    }

    public double getSaveLon() {
        return this.saveLon;
    }

    public void setSaveLon(double saveLon) {
        this.saveLon = saveLon;
    }

    public int getSaveNumLine() {
        return this.saveNumLine;
    }

    public void setSaveNumLine(int saveNumLine) {
        this.saveNumLine = saveNumLine;
    }

    public int getSaveNumEle() {
        return this.saveNumEle;
    }

    public void setSaveNumEle(int saveNumEle) {
        this.saveNumEle = saveNumEle;
    }

    public int getSaveLineMag() {
        return this.saveLineMag;
    }

    public void setSaveLineMag(int saveLineMag) {
        this.saveLineMag = saveLineMag;
    }

    public int getSaveEleMag() {
        return this.saveEleMag;
    }

    public void setSaveEleMag(int saveEleMag) {
        this.saveEleMag = saveEleMag;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean getShowPreview() {
        return this.showPreview;
    }

    public void setShowPreview(boolean showPreview) {
        this.showPreview = showPreview;
    }

    public boolean getSaveShowPreview() {
        return this.saveShowPreview;
    }

    public void setSaveShowPreview(boolean saveShowPreview) {
        this.saveShowPreview = saveShowPreview;
    }

    private void getSaveComponents() {
        saveCoordType = this.laLoSel.getCoordinateType();
        savePlace = this.laLoSel.getPlace();
        if (saveCoordType.equals(this.laLoSel.getLatLonType())) {
            saveLat = this.laLoSel.getLatitude();
            saveLon = this.laLoSel.getLongitude();
        }
        saveNumLine = this.laLoSel.getNumLines();
        saveNumEle = this.laLoSel.getNumEles();
        saveLineMag = this.laLoSel.getLineMag();
        saveEleMag = this.laLoSel.getElementMag();
    }

    /**
     * Return the list of times held by the DataSelection member.
     *
     * @return  DataSelection times
     */
    
    public List getDateTimeSelection() {
//        return super.getDateTimeSelection();
        DataSelection s = getDataSelection();
        if (s == null) {
            logger.trace("oh no getDataSelection is null :(");
            return null;
        } else {
            return s.getTimes();
        }
    }

    /**
     * Set the list of selected times for this data source. This is used
     * for XML persistence.
     *
     * @param selectedTimes   List of selected times
     */
    
    public void setDateTimeSelection(List selectedTimes) {
//        //Check to see if we need to convert the absolute times into an index list.
//        if (holdsDateTimes(selectedTimes) && (timesList != null)) {
//            selectedTimes = Misc.getIndexList(selectedTimes,
//                getAllDateTimes());
//        }
//        getDataSelection().setTimes(selectedTimes);
        super.setDateTimeSelection(selectedTimes);
        List selected = getDateTimeSelection();
        logger.trace("incoming: {} result: {}", selectedTimes, selected);
    }

    protected boolean canDoProgressiveResolution() {
        return false;
    }

    public boolean getIsProgressiveResolution() {
        return false;
    }

    public void setIsProgressiveResolution(boolean isPG) {

    }

    public boolean getMatchDisplayRegion() {
        return false;
    }

    public static class BundlePreviewSelection extends DataSelectionComponent {
        final String label;
        public BundlePreviewSelection(final String label) {
            super(label);
            this.label = label;
        }

        @Override protected JComponent doMakeContents() {
            // TODO Auto-generated method stub
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            JLabel label1 = new JLabel("Area coverage has been defined by the data bundle;");
            JLabel label2 = new JLabel("further subsetting is not currently supported.");
            label1.setAlignmentX(Component.CENTER_ALIGNMENT);
            label2.setAlignmentX(Container.CENTER_ALIGNMENT);
            panel.add(label1);
            panel.add(label2);
            return panel;
        }

        @Override public void applyToDataSelection(DataSelection dataSelection) {
        }

        /**
         * Overridden to disable these dummy tabs from showing up in properties
         * dialog.
         */
        @Override public boolean getShowInControlProperties() {
            return false;
        }
    }
}
