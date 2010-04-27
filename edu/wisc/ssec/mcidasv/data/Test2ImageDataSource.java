/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

package edu.wisc.ssec.mcidasv.data;

import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.AddeTextReader;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.*;

import ucar.nc2.iosp.mcidas.McIDASAreaProjection;
import ucar.unidata.data.*;
import ucar.unidata.data.imagery.*;
import ucar.unidata.geoloc.*;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.visad.data.AreaImageFlatField;

import visad.*;
import visad.data.DataRange;
import visad.data.mcidas.*;
import visad.georef.MapProjection;
import visad.meteorology.*;
import visad.util.ThreadManager;

/**
 * Abstract DataSource class for images files.
 */
public class Test2ImageDataSource extends AddeImageDataSource {

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
    public final static String SPACING_BRIT = "1";
    public final static String SPACING_NON_BRIT = "4";

    /** The first projection we find */
    protected ProjectionImpl sampleProjection;
    public MapProjection sampleMapProjection;

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
    private static DataChoice lastChoice;
    private Boolean showPreview = new Boolean(false);
    private FlatField previewImage;
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
    private int saveLine;
    private int saveElement;
    private int saveNumLine;
    private int saveNumEle;
    private int saveLineMag;
    private int saveEleMag;
    private boolean saveLockOn;
    private Boolean saveShowPreview;

    private String displaySource;

    protected List<DataChoice> stashedChoices = null;
    private List iml = new ArrayList();
    private List saveImageList = new ArrayList();

    private int previewLineRes = 1;
    private int previewEleRes = 1;

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
                           Hashtable properties) throws VisADException {
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
                           Hashtable properties) throws VisADException {
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
                           Hashtable properties) throws VisADException {
        super(descriptor, ids, properties);
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
        AddeImageInfo aii     = aid.getImageInfo();
        this.source = aid.getSource();
        if (this.source.contains("localhost")) {
            AreaDirectory areaDirectory = aid.getDirectory();
            if (!sourceProps.containsKey((Object)UNIT_KEY)) {
                if (!sourceProps.containsKey((Object)BAND_KEY)) {
                    String calType = areaDirectory.getCalibrationType();
                    if (!calType.equals("RAW")) {
                        sourceProps.put(UNIT_KEY, calType);
                        int[] bandNums = areaDirectory.getBands();
                        String bandString = new Integer(bandNums[0]).toString();
                        sourceProps.put(BAND_KEY, bandString);
                    }
                }
            }
        }
        setMag();
        getAreaDirectory(properties);
    }

    public boolean canSaveDataToLocalDisk(){
        return true;
    }

    /**
     * _more_
     */
    public void reloadData() {
        super.reloadData();
        if (choiceName != null) {
            setProperty(PROP_DATACHOICENAME, choiceName);
            applyProperties();
        }
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
            String bandStr = getKey(addeCmdBuff, "BAND");
            if (bandStr.equals("")) {
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
                List<BandInfo> bandInfos =
                    (List<BandInfo>) getProperty(PROP_BANDINFO, (Object) null);
                BandInfo bi = bandInfos.get(0);
                String bandStr = new Integer(bi.getBandNumber()).toString();
                addeCmdBuff = replaceKey(addeCmdBuff, "BAND", bandStr);
                dirList = new AreaDirectoryList(addeCmdBuff);
            } catch (Exception eOpen) {
                setInError(true);
                System.out.println("====> Opening area file: " + eOpen.getMessage());
                //throw new BadDataException("Opening area file: " + eOpen.getMessage());
            }
        }

        try {
            List areaDirs = dirList.getDirs();
            AreaDirectory ad = (AreaDirectory)areaDirs.get(0);
            float[] res = getLineEleResolution(ad);
            float resol = res[0];
            if (this.lineMag < 0)
                resol *= Math.abs(this.lineMag);
            this.lRes = resol;
            resol = res[1];
            if (this.elementMag < 0)
                resol *= Math.abs(this.elementMag);
            this.eRes = resol;
            this.lineResolution = ad.getValue(11);
            this.elementResolution = ad.getValue(12);
        } catch (Exception e) {
            setInError(true);
            System.out.println("====> Getting area directory: " + e.getMessage());
            //throw new BadDataException("Getting area directory: " + e.getMessage());
        }
        baseSource = addeCmdBuff;
    }

    protected void initDataSelectionComponents(
                   List<DataSelectionComponent> components, final DataChoice dataChoice) {

        getDataContext().getIdv().showWaitCursor();

        boolean hasImagePreview = true;
        if (this.showPreview == null) this.showPreview = true;
        boolean basically = false;
        if (lastChoice != null)
            basically = dataChoice.basicallyEquals(lastChoice);
        if (this.haveDataSelectionComponents && dataChoice.equals(lastChoice)) {
            try {
                if (dataChoice.getDataSelection() == null) {
                    if (!basically) {
                        laLoSel = new GeoLatLonSelection(this, 
                                         dataChoice, this.initProps, this.previewProjection,
                                         previewDir, previewNav);
                    }
                    this.lineMag = laLoSel.getLineMag();
                    this.elementMag = laLoSel.getElementMag();
                    previewSel = new GeoPreviewSelection(this, dataChoice, this.previewImage, 
                                     this.laLoSel, this.previewProjection,
                                     this.lineMag, this.elementMag, this.showPreview);
                }
                components.add(previewSel);
                components.add(laLoSel);
            } catch (Exception e) {
                System.out.println("error while repeating addition of selection components \n	e= "+e);
                getDataContext().getIdv().showNormalCursor();
            }
        } else {
            try {
                hasImagePreview = makePreviewImage(dataChoice);
                if (basically)
                    getSaveComponents();
            } catch (Exception e) {
                JLabel label = new JLabel("Can't make preview image");
                JPanel contents = GuiUtils.top(GuiUtils.inset(label, label.getText().length() + 12));
                GuiUtils.showOkDialog(null, "No Preview Image", contents, null);
                getDataContext().getIdv().showNormalCursor();
                System.out.println("makePreviewImage e=" + e);
                return;
            }
            lastChoice = dataChoice;
            if (hasImagePreview) {
                try {
                    String magStr = getKey(baseSource, MAG_KEY);
                    String saveMagStr = magStr;
                    String[] vals = StringUtil.split(magStr, " ", 2);
                    Integer iVal = new Integer(vals[0]);
                    int lMag = iVal.intValue() * -1;
                    if (lMag == -1) lMag = 1;
                    iVal = new Integer(vals[1]);
                    int eMag = iVal.intValue() * -1;
                    if (eMag == -1) eMag = 1;
                    magStr = lMag + " " + eMag;
                    replaceKey(MAG_KEY, magStr);
                    AreaAdapter aa = new AreaAdapter(baseSource, false);
                    this.previewImage = (FlatField)aa.getImage();
                    AreaFile af = new AreaFile(baseSource);
                    previewNav = af.getNavigation();
                    AreaDirectory ad = af.getAreaDirectory();
                    this.lineResolution = ad.getValue(11);
                    this.elementResolution = ad.getValue(12);
                    McIDASAreaProjection map = new McIDASAreaProjection(af);
                    AREACoordinateSystem acs = new AREACoordinateSystem(af);
                    this.initProps = new Hashtable();
                    Enumeration propEnum = sourceProps.keys();
                    for (int i=0; propEnum.hasMoreElements(); i++) {
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
                    iVal = new Integer(vals[0]);
                    lMag = iVal.intValue();
                    iVal = new Integer(vals[1]);
                     eMag = iVal.intValue();
                    this.initProps.put("LRES", String.valueOf((this.lRes)));
                    this.initProps.put("ERES", String.valueOf((this.eRes)));
                    this.initProps.put("PLRES", String.valueOf((this.previewLineRes)));
                    this.initProps.put("PERES", String.valueOf((this.previewEleRes)));
                    this.previewProjection = (MapProjection)acs;

                    String coordType = "";
                    double coords[] = { 0.0, 0.0 };
                    double baseLResOld = 0.0;
                    double baseEResOld = 0.0;
                    int lMagOld = 0;
                    int eMagOld = 0;
                    int lSizeOld = 0;
                    int eSizeOld = 0;
                    if (!basically) {
                        if (laLoSel != null) {
                            coordType = laLoSel.getCoordinateType();
                            if (coordType.equals(laLoSel.TYPE_LATLON)) {
                                coords[0] = laLoSel.getLatitude();
                                coords[1] = laLoSel.getLongitude();
                            } else {
                                coords[0] = (double)laLoSel.getLine();
                                coords[1] = (double)laLoSel.getElement();
                            }
                            baseLResOld = laLoSel.getBaseLRes();
                            baseEResOld = laLoSel.getBaseERes();
                            lMagOld = laLoSel.getLineMag();
                            eMagOld = laLoSel.getElementMag();
                            lSizeOld = laLoSel.getNumLines();
                            eSizeOld = laLoSel.getNumEles();

                            laLoSel.setPreviewLineRes(this.previewLineRes);
                            laLoSel.setPreviewEleRes(this.previewEleRes);
                            laLoSel.update(previewDir, this.previewProjection, previewNav,
                                           coordType, coords);
                        } else {
                            laLoSel = new GeoLatLonSelection(this, 
                                          dataChoice, this.initProps, this.previewProjection,
                                          previewDir, previewNav);
                            this.lineMag = laLoSel.getLineMag();
                            this.elementMag = laLoSel.getElementMag();
                        }
                    }
                    previewSel = new GeoPreviewSelection(this, dataChoice, this.previewImage, 
                                     this.laLoSel, this.previewProjection,
                                     this.lineMag, this.elementMag, this.showPreview);
                } catch (Exception e) {
                    System.err.println("Can't make selection components e="+e);
                    System.err.println("\n" + baseSource);
                    getDataContext().getIdv().showNormalCursor();
                }
                this.haveDataSelectionComponents = true;
                replaceKey(MAG_KEY, (Object)(this.lineMag + " " + this.elementMag));
                components.add(previewSel);
                components.add(laLoSel);
            }
        }
        if (previewSel != null) previewSel.drawBox();
        getDataContext().getIdv().showNormalCursor();
    }

    private boolean makePreviewImage(DataChoice dataChoice) {
        boolean msgFlag = false;
        showPreview = saveShowPreview;
        List<BandInfo> bandInfos =
            (List<BandInfo>) getProperty(PROP_BANDINFO, (Object) null);
        BandInfo bi = null;
        String saveBand = getKey(source, BAND_KEY);
        int bandIdx = 0;
        List<TwoFacedObject> calList = null;
        try {
            Object dcObj = dataChoice.getId();
            if (dcObj instanceof BandInfo) {
                bi = (BandInfo) dcObj;
                Integer bandInt = new Integer(bandInfos.indexOf(dcObj)+1);
                saveBand = bandInt.toString();
            } else {
                msgFlag = true;
                bi = bandInfos.get(bandIdx);
                this.showPreview = false;
            }
            // pull out the list of cal units, we'll need for type check later...
            calList = bi.getCalibrationUnits();
            source = replaceKey(source, BAND_KEY, (Object) (bi.getBandNumber()));
            // if we're replacing the band, replace cal type with preferred  
            // type for that band
            source = replaceKey(source, UNIT_KEY, (Object) bi.getPreferredUnit());
        } catch (Exception excp) {
            handlePreviewImageError(1, excp);
        }
        String name = dataChoice.getName();
        int idx = name.lastIndexOf("_");
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
        if (! validCal) {
        	unit = bi.getPreferredUnit();
        }
        
        if (getKey(source, UNIT_KEY).equals(""))
            source = replaceKey(source, UNIT_KEY, (Object)(unit));

        AddeImageDescriptor aid = null;
        while (aid == null) {
            try {
                aid = new AddeImageDescriptor(this.source);
            } catch (Exception excp) {
                msgFlag = true;
                if (bandIdx > bandInfos.size()) return false;
                bi = bandInfos.get(bandIdx);
                source = replaceKey(source, BAND_KEY, (Object)(bi.getBandNumber()));
                ++bandIdx;
            }
        }
        previewDir = getPreviewDirectory(aid); 
        int eMag = 1;
        int lMag = 1;
        int eSize = 1;
        int lSize = 1;
        try {
            int plMag = 1;
            int peMag = 1;
            Object magKey = (Object)"mag";
            if (sourceProps.containsKey(magKey)) {
                String magVal = (String)(sourceProps.get(magKey));
                String[] magVals = magVal.split(" ");
                peMag = new Integer(magVals[0]).intValue();
                plMag = new Integer(magVals[1]).intValue();
            }
            double feSize = (double)previewDir.getElements();
            double flSize = (double)previewDir.getLines();
            double feMag = (double)peMag;
            double flMag = (double)plMag;
            if (feSize > flSize) {
                feMag = feSize/525.0;
                flMag = feMag * (double)plMag/(double)peMag;
            } else {
                flMag = flSize/500.0;
                feMag = flMag * (double)peMag/(double)plMag;
            }
            eMag = (int)Math.ceil(feMag);
            lMag = (int)Math.ceil(flMag);
        } catch(Exception excp) {
           handlePreviewImageError(3, excp);
        }
        if (eMag < 1) eMag = 1;
        if (lMag < 1) lMag = 1;

        eSize = 525;
        lSize = 500;
        if ((baseSource == null) || msgFlag) {
            baseSource = source;
        }
        this.previewLineRes = lMag;
        this.previewEleRes = eMag;
        String src = aid.getSource();
        src = replaceKey(src, LINELE_KEY, (Object)("1 1"));
        src = replaceKey(src, PLACE_KEY, (Object)("ULEFT"));
        src = replaceKey(src, SIZE_KEY, (Object)(lSize + " " + eSize));
        src = replaceKey(src, MAG_KEY, (Object)(lMag + " " + eMag));
        src = replaceKey(src, BAND_KEY, (Object)(bi.getBandNumber()));
        src = replaceKey(src, UNIT_KEY, (Object)(unit));

        try {
            aid = new AddeImageDescriptor(src);
        } catch (Exception excp) {
            handlePreviewImageError(4, excp);
            src = replaceKey(src, BAND_KEY, (Object)saveBand);
            aid = new AddeImageDescriptor(src);
            src = replaceKey(src, BAND_KEY, (Object)(bi.getBandNumber()));
        }
        if (msgFlag && (!saveBand.equals("ALL"))) src = replaceKey(src, BAND_KEY, (Object)saveBand);
        baseSource = src;
        return true;
    }

    /**
     * Show the given error to the user. 
     *
     * @param excp The exception
     */
    protected void handlePreviewImageError(int flag, Exception excp) {
        String message = excp.getMessage();
        LogUtil.userErrorMessage("Error in makePreviewImage  e=" + flag + " "
                                     + excp);
    }

    private String removeKey(String src, String key) {
        String returnString = src;
        String keyStr = key + "=";
        if (returnString.contains(keyStr)) {
            String[] segs = returnString.split(keyStr);
            String seg0 = segs[0];
            String seg1 = segs[1];
            int indx = seg1.indexOf("&");
            if (indx >= 0) {
                seg1 = seg1.substring(indx+1);
            }
            returnString = seg0 + seg1;
        }
        return returnString;
    }

    private String replaceKey(String src, String key, Object val) {
        String returnString = src;
        // make sure we got valid key/val pair
        if ((key == null) || (val == null)) {
        	return returnString;
        }
        key = key.toUpperCase() + "=";
        if (returnString.contains(key)) {
            String[] segs = returnString.split(key);
            String seg0 = segs[0];
            String seg1 = segs[1];
            int indx = seg1.indexOf("&");
            if (indx < 0) {
                seg1 = "";
            } else if (indx > 0) {
                seg1 = seg1.substring(indx);
            }
            returnString = seg0 + key + val + seg1;
        }
        else {
            returnString = returnString + "&" + key + val;
        } 
        // if key is for cal units, and it was changed to BRIT,
        // must change the spacing key too 
        if ((key.equals(UNIT_KEY + "=")) && (val.equals("BRIT"))) {
        	returnString = replaceKey(returnString, SPAC_KEY, SPAC_KEY, SPACING_BRIT);
        } else {
        	returnString = replaceKey(returnString, SPAC_KEY, SPAC_KEY, SPACING_NON_BRIT); 
        }
        return returnString;
    }

    private String replaceKey(String src, String oldKey, String newKey, Object val) {
        String returnString = src;
        oldKey = oldKey.toUpperCase() + "=";
        newKey = newKey.toUpperCase() + "=";
        if (returnString.contains(oldKey)) {
            String[] segs = returnString.split(oldKey);
            String seg0 = segs[0];
            String seg1 = segs[1];
            int indx = seg1.indexOf("&");
            if (indx < 0) {
                seg1 = "";
            } else if (indx > 0) {
                seg1 = seg1.substring(indx);
            }
            returnString = seg0 + newKey + val + seg1;
        }
        else {
            returnString = returnString + "&" + newKey + val;
        }
        return returnString;
    }

    private void replaceKey(String key, Object val) {
        baseSource = replaceKey(baseSource, key, val);
    }

    private String getKey(String src, String key) {
        String returnString = "";
        key = key.toUpperCase() + "=";
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
        List<BandInfo> bandInfos =
            (List<BandInfo>) getProperty(PROP_BANDINFO, (Object) null);
        String name = "";
        if (this.choiceName != null) name = this.choiceName;
        if (!name.equals("")) return;
        if (!sourceProps.containsKey(UNIT_KEY)) return;
        BandInfo bi = null;
        if (sourceProps.containsKey(BAND_KEY)) {
            int bandProp = new Integer((String)(sourceProps.get(BAND_KEY))).intValue();
            int bandIndex = BandInfo.findIndexByNumber(bandProp, bandInfos);
            bi = (BandInfo)bandInfos.get(bandIndex);
            if (sourceProps.containsKey(UNIT_KEY)) {
                bi.setPreferredUnit((String)(sourceProps.get(UNIT_KEY)));
            } else  {
                bi.setPreferredUnit("");
            }
            name = makeBandParam(bi);
        }
        else if (sourceProps.containsKey(BANDINFO_KEY)) {
            ArrayList al = (ArrayList)sourceProps.get(BANDINFO_KEY);
            bi = (BandInfo)al.get(0);
            name = makeBandParam(bi);
        }
        if (stashedChoices != null) {
            int numChoices = stashedChoices.size();
            for (int i=0; i<numChoices; i++) {
               DataChoice choice = (DataChoice)stashedChoices.get(i);
               if (name.equals(choice.getName())) {
                   setProperty(PROP_DATACHOICENAME, choice.getName());
               }
            }
        }
    }

    /**
     * Insert the new DataChoice into the dataChoice list.
     *
     * @param choice   new choice to add
     */
    protected void addDataChoice(DataChoice choice) {
        super.addDataChoice(choice);
        if (stashedChoices == null) {
            stashedChoices = new ArrayList();
        }
        stashedChoices.add(choice);
    }


    /**
     * Initialize the {@link ucar.unidata.data.DataCategory} objects that
     * this data source uses. 
     */
    private void makeCategories() {
        twoDTimeSeriesCategories =
            DataCategory.parseCategories("IMAGE-2D-TIME;", false);
        twoDCategories = DataCategory.parseCategories("IMAGE-2D;", false);
        bandCategories = DataCategory.parseCategories("IMAGE-BAND;", false);
        bandTimeSeriesCategories =
            DataCategory.parseCategories("IMAGE-BAND-TIME;", false);

    }


    /** _more_ */
    private DataRange[] sampleRanges = null;

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
/*
        System.out.println("\ngetDataInner:");
        System.out.println("    dataChoice=" + dataChoice);
        System.out.println("    category=" + category);
        System.out.println("    dataSelection=" + dataSelection + "\n");
        Enumeration propEnum = requestProperties.keys();
        for (int i=0; propEnum.hasMoreElements(); i++) {
            String key = propEnum.nextElement().toString();
            System.out.println("    key=" + key + " val=" + requestProperties.get(key));
        }
        System.out.println("\n");
*/
        sampleRanges = null;

        iml = new ArrayList();

        if (dataSelection == null) return null;
        setDataSelection(dataSelection);
        GeoSelection geoSelection = dataSelection.getGeoSelection(true);
        if (geoSelection == null) return null;
        if (this.lastGeoSelection == null) this.lastGeoSelection = geoSelection;
        this.selectionProps = dataSelection.getProperties();
        if (this.selectionProps.containsKey("MAG")) {
            String str = (String)this.selectionProps.get("MAG");
            String[] strs = StringUtil.split(str, " ", 2);
            this.lineMag = new Integer(strs[0]).intValue();
            this.elementMag = new Integer(strs[1]).intValue();
        }
        this.choiceName = dataChoice.getName();
        Data img = null;
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
     * Check if the DataChoice has a BandInfo for it's Id
     *
     * @param dataChoice  choice to check
     *
     * @return true if the choice ID is a BandInfo
     */
    private boolean hasBandInfo(DataChoice dataChoice) {
        Object id = dataChoice.getId();
        return dataChoice.getId() instanceof BandInfo;
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
    protected ImageSequence makeImageSequence(DataChoice dataChoice,
            DataSelection subset)
            throws VisADException, RemoteException {
/*
        System.out.println("Test2ImageDataSource makeImageSequence:");
        System.out.println("    dataChoice=" + dataChoice);
        System.out.println("    subset=" + subset);
*/
        Hashtable subsetProperties = subset.getProperties();
        Enumeration propEnum = subsetProperties.keys();
        int numLines = 0;
        int numEles = 0;
        for (int i=0; propEnum.hasMoreElements(); i++) {
            String key = propEnum.nextElement().toString();
            if (key.compareToIgnoreCase(SIZE_KEY) == 0) {
                String sizeStr = (String)(subsetProperties.get(key));
                String[] vals = StringUtil.split(sizeStr, " ", 2);
                Integer iVal = new Integer(vals[0]);
                numLines = iVal.intValue();
                iVal = new Integer(vals[1]);
                numEles = iVal.intValue();
                break;
            }
        }
        int saveLines = numLines;
        int saveEles = numEles;
        if (sampleMapProjection == null) {
            String addeCmdBuff = baseSource;
            AreaFile af = null;
            try {
                af = new AreaFile(addeCmdBuff);
            } catch (Exception eOpen) {
                setInError(true);
                throw new BadDataException("Opening area file: " + eOpen.getMessage());
            }
            try {
                McIDASAreaProjection map = new McIDASAreaProjection(af);
                AREACoordinateSystem acs = new AREACoordinateSystem(af);
                sampleMapProjection = (MapProjection)acs;
                sampleProjection = map;
            } catch (Exception e) {
                setInError(true);
                throw new BadDataException("Making area projection: " + e.getMessage());
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
            int line = (int)(elelin[1][0]+0.5)*dirBlk[11];
            int ele = (int)(elelin[0][0]+0.5)*dirBlk[12];
            numLines = (int)(Math.abs(elelin[1][0] - elelin[1][1]))*dirBlk[11];
            numEles = (int)(Math.abs(elelin[0][1] - elelin[0][0]))*dirBlk[12];
        }

        String ulString = dirBlk[5] + " " + dirBlk[6] + " I";
        Hashtable props = subset.getProperties();
        if (props.containsKey("LINELE")) {
            ulString = (String)props.get("LINELE");
        }

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
            boolean       anyRelative     = false;
            //Find the descriptor with the largets position
            String biggestSource = null;
            for (Iterator iter =
                    descriptorsToUse.iterator(); iter.hasNext(); ) {
                AddeImageDescriptor aid = (AddeImageDescriptor) iter.next();
                if (aid.getIsRelative()) {
                    anyRelative = true;
                }
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
                if ((biggestPosition == null)
                    || (Math.abs(aii.getDatasetPosition()) > pos)) {
                    pos             = Math.abs(aii.getDatasetPosition());
                    biggestPosition = aii;
                    biggestSource   = aid.getSource();
                }
            }

            if (getCacheDataToDisk() && anyRelative
                && (biggestPosition != null)) {
                biggestPosition.setRequestType(AddeImageInfo.REQ_IMAGEDIR);
                AreaDirectoryList adl =
                    new AreaDirectoryList(biggestPosition.getURLString());
                biggestPosition.setRequestType(AddeImageInfo.REQ_IMAGEDATA);
                currentDirs = adl.getSortedDirs();
            } else {
                currentDirs = null;
            }

            ThreadManager threadManager =
                new ThreadManager("image data reading");
            final ImageSequenceManager sequenceManager =
                new ImageSequenceManager();
            int           cnt      = 1;
            DataChoice    parent   = dataChoice.getParent();
            final List<SingleBandedImage> images =
                new ArrayList<SingleBandedImage>();
            MathType rangeType = null;
            for (Iterator iter =
                    descriptorsToUse.iterator(); iter.hasNext(); ) {
                final AddeImageDescriptor aid =
                    (AddeImageDescriptor) iter.next();
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
                final String readLabel = "Time: " + (cnt++) + "/"
                    + descriptorsToUse.size() + "  "
                    + label;

                String src = "";
                try {
                    src = aid.getSource();
                    src = replaceKey(src, LINELE_KEY, (Object)("1 1"));
                    String sizeString = "10 10";
                    src = replaceKey(src, SIZE_KEY, (Object)(sizeString));
                    String name = dataChoice.getName();
                    int idx = name.lastIndexOf("_");
                    String unit = name.substring(idx+1);
                    if (getKey(src, UNIT_KEY).equals(""))
                        src = replaceKey(src, UNIT_KEY, (Object)(unit));

                    AreaFile af = new AreaFile(src);
                    AreaDirectory ad = af.getAreaDirectory();
                    int lMag = this.lineMag;
                    int eMag = this.elementMag;
                    int lSize = numLines;
                    int eSize = numEles;
                    sizeString = lSize + " " + eSize;
                    src = replaceKey(src, SIZE_KEY, (Object)(sizeString));
                    src = replaceKey(src, MAG_KEY, (Object)(this.lineMag + " " + this.elementMag));
                    aid.setSource(src);
                } catch (Exception exc) {
                    ImageSequence is = super.makeImageSequence(dataChoice, subset);
                }

                SingleBandedImage image = makeImage(aid, rangeType, true,
                                                    readLabel, subset);
                if (image != null) {
                    if(rangeType==null) {
                        rangeType = ((FunctionType) image.getType()).getRange();
                    }
                    synchronized (images) {
                        images.add(image);
                    }
                }
            }

            TreeMap imageMap = new TreeMap();
            for (SingleBandedImage image : images) {
                imageMap.put(image.getStartTime(), image);
            }
            List<SingleBandedImage> sortedImages =
                (List<SingleBandedImage>) new ArrayList(imageMap.values());
            if ((sortedImages.size() > 0)
                    && (sortedImages.get(0) instanceof AreaImageFlatField)) {
                DataRange[] sampleRanges = null;
                Set domainSet = null;
                for (SingleBandedImage sbi : sortedImages) {
                    AreaImageFlatField aiff = (AreaImageFlatField) sbi;
                    sampleRanges = aiff.getRanges(true);
                    if(domainSet == null)
                        domainSet = aiff.getDomainSet();
                    if ((sampleRanges != null) && (sampleRanges.length > 0)) {
                        for (int rangeIdx = 0; rangeIdx < sampleRanges.length;
                                rangeIdx++) {
                            DataRange r = sampleRanges[rangeIdx];
                            if (Double.isInfinite(r.getMin())
                                || Double.isInfinite(r.getMax())) {
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
     * Create the single image defined by the given dataChoice.
     *
     * @param aid AddeImageDescriptor
     * @param fromSequence _more_
     * @param subset geographical subsetting info
     *
     * @return The data.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private SingleBandedImage makeImage(AddeImageDescriptor aid,
                                        MathType rangeType,
                                        boolean fromSequence, 
                                        String readLabel, DataSelection subset)
            throws VisADException, RemoteException {
/*
        System.out.println("makeImage:");
        System.out.println("    aid=" + aid);
        System.out.println("    rangeType=" + rangeType);
        System.out.println("    fromSequence=" + fromSequence);
        System.out.println("    readLabel=" + readLabel);
*/
        if (aid == null) {
            return null;
        }
        String src = aid.getSource();

        Hashtable props = subset.getProperties();
        if (props.containsKey("PLACE")) 
            src = replaceKey(src, "PLACE", props.get("PLACE"));
        if (props.containsKey("LATLON")) {
            src = replaceKey(src, "LINELE", "LATLON", props.get("LATLON"));
        }
        if (props.containsKey("LINELE")) {
            src = removeKey(src, "LATLON");
            src = replaceKey(src, "LINELE", props.get("LINELE"));
        }
        if (props.containsKey("MAG"))
            src = replaceKey(src, "MAG", props.get("MAG"));
        aid.setSource(src);

        SingleBandedImage result = (SingleBandedImage) getCache(src);
        if (result != null) {
            setDisplaySource(src, props);
            return result;
        }

        //For now handle non adde urls here
        try {
            AddeImageInfo aii     = aid.getImageInfo();
            AreaDirectory areaDir = null;
            try {
                if (aii != null) {
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
                LogUtil.printMessage("error looking up area dir");
                exc.printStackTrace();
                return null;
            }

            if (areaDir == null) {
                areaDir = aid.getDirectory();
            }

            if ( !getCacheDataToDisk()) {
                areaDir = null;
            }

            if ( !fromSequence
                    || (aid.getIsRelative() && (currentDirs == null))) {
                areaDir = null;
            }
            //areaDir = null; /* ?????????????????? */

            if (areaDir != null) {
                int hash = ((aii != null)
                            ? aii.makeAddeUrl().hashCode()
                            : areaDir.hashCode());
                if(rangeType==null) {
                    result =    AreaImageFlatField.createImmediate(aid, readLabel);
                } else {
                    //Else, pass in the already created range type
                    result  = AreaImageFlatField.create(aid,
                                                        areaDir, rangeType, readLabel);
                }
            } else {
                src = aid.getSource();
                try {
                    savePlace = laLoSel.getPlace();
                    saveLat = laLoSel.getLatitude();
                    saveLon = laLoSel.getLongitude();
                    saveNumLine = laLoSel.getNumLines();
                    saveNumEle = laLoSel.getNumEles();
                    saveLineMag = laLoSel.getLineMag();
                    saveEleMag = laLoSel.getElementMag();
                } catch (Exception e) {
                    savePlace = getSavePlace();
                    laLoSel.setPlace(savePlace);
                    saveLat = getSaveLat();
                    laLoSel.setLatitude(saveLat);
                    saveLon = getSaveLon();
                    laLoSel.setLongitude(saveLon);
                    saveNumLine = getSaveNumLine();
                    laLoSel.setNumLines(saveNumLine);
                    saveNumEle = getSaveNumEle();
                    laLoSel.setNumEles(saveNumEle);
                    saveLineMag = getSaveLineMag();
                    laLoSel.setLineMag(saveLineMag);
                    saveEleMag = getSaveEleMag();
                    laLoSel.setElementMag(saveEleMag);
                }

                src = replaceKey(src, PLACE_KEY, savePlace);
                src = removeKey(src, LINELE_KEY);
                if (!getKey(src, LATLON_KEY).equals("")) {
                    String latStr = Double.toString(saveLat);
                    if (latStr.length()>8)
                        latStr = latStr.substring(0,7);
                    String lonStr = Double.toString(saveLon);
                    if (lonStr.length()>9)
                        lonStr = lonStr.substring(0,8);
                    src = replaceKey(src, LATLON_KEY, latStr + " " + lonStr);
                }
                src = replaceKey(src, SIZE_KEY, saveNumLine + " " + saveNumEle);
                src = replaceKey(src, MAG_KEY, saveLineMag + " " + saveEleMag);
            }

            //System.out.println("\n" + src + "\n");
            AreaAdapter aa = new AreaAdapter(src, false);
            areaDir = previewDir;
            result = aa.getImage();
            putCache(src, result);
            aid.setSource(src);
            iml.add(aid);
            setImageList(iml);
            setDisplaySource(src, props);
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
    public List getDescriptors(DataChoice dataChoice, DataSelection subset) {
        int linRes = this.lineResolution;
        int eleRes = this.elementResolution;
        int newLinRes = linRes;
        int newEleRes = eleRes;
        List times = getTimesFromDataSelection(subset, dataChoice);
        if ((times == null) || times.isEmpty()) {
            times = imageTimes;
        }
        List descriptors = new ArrayList();
        for (Iterator iter = times.iterator(); iter.hasNext(); ) {
            Object              time  = iter.next();
            AddeImageDescriptor found = null;
            if (saveImageList.isEmpty()) saveImageList = getImageList();
            for (Iterator iter2 = saveImageList.iterator(); iter2.hasNext(); ) {
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
                        aii.setBand("" + bi.getBandNumber());
                        aii.setPlaceValue("ULEFT");

                        try {
                            AddeImageDescriptor newAid = new AddeImageDescriptor(aii.getURLString());
                            AreaDirectory newAd = newAid.getDirectory();
                            newLinRes = newAd.getValue(11);
                            newEleRes = newAd.getValue(12);
                        } catch (Exception e) {
                            //System.out.println("can't reset resolution.  e=" + e);
                        }

                        double[][] projCoords = new double[2][2];
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

    private AreaDirectory getPreviewDirectory(AddeImageDescriptor aid) {
        AreaDirectory directory = aid.getDirectory();
        int times = imageTimes.size();
        if (times == 1) return directory;
        String src = aid.getSource();

	src = removeKey(src, "LINELE");
	src = removeKey(src, "PLACE");
        src = removeKey(src, "SIZE");
        src = removeKey(src, "UNIT");
        src = removeKey(src, "MAG");
        src = removeKey(src, "SPAC");
        src = removeKey(src, "NAV");
        src = removeKey(src, "AUX");
        src = removeKey(src, "DOC");

        int maxLine = 0;
        int maxEle = 0;
        int imageSize = 0;
        src = src.replace("imagedata", "imagedir");
        boolean isRelative = aid.getIsRelative();
        for (int i=0; i<times; i++) {
            if (isRelative) {
                src = replaceKey(src, "POS", new Integer(i).toString());
            } else {
                DateTime dt = (DateTime)imageTimes.get(i);
                String timeStr = dt.timeString();
                timeStr = timeStr.replace("Z", " ");
                src = removeKey(src, "POS");
                src = replaceKey(src, "TIME", timeStr + timeStr + "I");
            }
            try {
                AreaDirectoryList dirList = new AreaDirectoryList(src);
                List ad = dirList.getDirs();
                AreaDirectory areaDir = (AreaDirectory)ad.get(0);
                int lines = areaDir.getLines();
                int eles =  areaDir.getElements();
                if (imageSize < lines*eles) {
                    imageSize = lines * eles;
                    maxLine = lines;
                    maxEle = eles;
                    directory = areaDir;
                }
            } catch (Exception e) {
                System.out.println("e=" + e);
            }
        }

        return directory;
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
                 if (!getKey(src, key).equals("")) {
                     src = replaceKey(src, key, val);
                 }
             }
         }
         this.displaySource = src;
         String unit = getKey(src, UNIT_KEY);
         if (!unit.equals("")) sourceProps.put(UNIT_KEY.toUpperCase(), unit);
    }

    public String getDisplaySource() {
        return this.displaySource;
    }


    private float[] getLineEleResolution(AreaDirectory ad) {
        float[] res = new float[2];
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

            for (int i=0; i<cards.length; i++) {
                if ( ! cards[i].startsWith("Sat ")) continue;
                StringTokenizer st = new StringTokenizer(cards[i]," ");
                String temp = st.nextToken();  // throw away the key
                int m = st.countTokens();
                for (int k=0; k<m; k++) {
                    int ss = Integer.parseInt(st.nextToken().trim());
                    if (ss == sensor) {
                        gotit = i;
                        break;
                    }
                }

                if (gotit != -1) break;
            }

            if (gotit == -1) return res;

            int gotSrc = -1;
            for (int i=gotit; i<cards.length; i++) {
                if (cards[i].startsWith("EndSat")) return res;
                if (!cards[i].startsWith("B") ) continue;
                StringTokenizer tok = new StringTokenizer(cards[i]);
                String str = tok.nextToken();
                str = tok.nextToken();
                Float flt = new Float(str);
                res[0] = flt.floatValue();
                str = tok.nextToken();
                flt = new Float(str);
                res[1] = flt.floatValue();
                return res;
            }
        } catch (Exception e) {
            System.out.println("getLineEleResolution: e=" + e);
        }
        return res;
    }


    /**
     * Read the adde text url and return the lines of text.
     * If unsuccessful return null.
     *
     * @param url adde url to a text file
     *
     * @return List of lines or null if in error
     */
    protected List readTextLines(String url) {
        AddeTextReader reader = new AddeTextReader(url);
        if ( !reader.getStatus().equals("OK")) {
            return null;
        }
        return reader.getLinesOfText();
    }


    /**
     * Create the first part of the ADDE request URL
     *
     * @param requestType     type of request
     * @return  ADDE URL prefix
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

    public MapProjection getSampleMapProjection() {
        return this.sampleMapProjection;
    }

    public void setSampleMapProjection(MapProjection sampleMapProjection) {
        this.sampleMapProjection = sampleMapProjection;
    }

    public String getChoiceName() {
        return this.choiceName;
    }

    public void setChoiceName(String choiceName) {
        this.choiceName = choiceName;
    }

    public MapProjection getPreviewProjection() {
        return this.previewProjection;
    }

    public void setPreviewProjection(MapProjection previewProjection) {
        this.previewProjection = previewProjection;
    }

    public AreaDirectory getPreviewDir() {
        return this.previewDir;
    }

    public void setPreviewDir(AreaDirectory previewDir) {
        this.previewDir = previewDir;
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
        saveCoordType = laLoSel.getCoordinateType();
        savePlace = laLoSel.getPlace();
        if (saveCoordType.equals(laLoSel.TYPE_LATLON)) {
            saveLat = laLoSel.getLatitude();
            saveLon = laLoSel.getLongitude();
        } else {
            saveLine = laLoSel.getLine();
            saveElement = laLoSel.getElement();
        }
        saveLockOn = laLoSel.getLockOn();
        saveNumLine = laLoSel.getNumLines();
        saveNumEle = laLoSel.getNumEles();
        saveLineMag = laLoSel.getLineMag();
        saveEleMag = laLoSel.getElementMag();
    }
}
