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

package edu.wisc.ssec.hydra.data;

import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;

import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.GranuleAggregation;
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.RangeProcessor;
import edu.wisc.ssec.adapter.AggregationRangeProcessor;
import edu.wisc.ssec.adapter.MultiSpectralData;
import visad.VisADException;
import visad.Data;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class NOAA_VIIRS_Products_DataSource extends DataSource {

    String dateTimeStamp = null;

    Date dateTime = null;

    String description = null;

    File[] files = null;

    ArrayList<SwathAdapter> swathAdapters = new ArrayList<SwathAdapter>();

    String[] bandNames = null;
    float[] centerWavelength = null;

    MultiSpectralData multiSpectData;

    HashMap<DataChoice, MultiSpectralData> msdMap = new HashMap<>();


    public NOAA_VIIRS_Products_DataSource(File directory) throws Exception {
        this(directory.listFiles());
    }

    public NOAA_VIIRS_Products_DataSource(ArrayList<File> files) throws Exception {
        this(files.toArray(new File[]{null}));
    }

    public NOAA_VIIRS_Products_DataSource(File[] files) throws Exception {

        if (!canUnderstand(files)) {
            throw new Exception("not compatible with VIIRSDataSource");
        }

        ArrayList<File> geoFileList = new ArrayList<File>();

        File file = files[0];
        String name = file.getName();
        File geoFile;

        File[] geoFiles = new File[files.length];

        String pltfrm = null;
        if (name.contains("_npp_")) {
            pltfrm = "npp";
        } else if (name.contains("_j01_")) {
            pltfrm = "j01";
        } else if (name.contains("_j02_")) {
            pltfrm = "j02";
        } else {
            throw new Exception("unknown VIIRS platform. Should be npp, j01, j02");
        }

        String prefix = null;
        if (name.startsWith("AFMOD")) {
            prefix = "GMTCO_" + pltfrm;
        } else if (name.startsWith("AFIMG")) {
            prefix = "GITCO_" + pltfrm;
        } else {
            throw new Exception("unknown VIIRS filename prefix: " + name);
        }

        File dir = new File(file.getParent());
        File[] list = dir.listFiles();
        for (int k = 0; k < list.length; k++) {
            if (list[k].getName().startsWith(prefix)) {
                geoFileList.add(list[k]);
            }
        }

        for (int i = 0; i < files.length; i++) {
            name = files[i].getName();
            String[] strs = name.split("_");
            String regex = prefix + "_" + strs[2] + "_" + strs[3] + "_" + strs[4] + "_" + strs[5] + ".*";
            Pattern pattern = Pattern.compile(regex);
            for (int k = 0; k < geoFileList.size(); k++) {
                geoFile = geoFileList.get(k);
                Matcher matcher = pattern.matcher(geoFile.getName());
                if (matcher.find()) {
                    geoFiles[i] = geoFile;
                    break;
                }
            }
        }

        dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName());
        dateTime = DataSource.getDateTimeFromFilename(file.getName());
        description = DataSource.getDescriptionFromFilename(file.getName());

        try {
            init(files, geoFiles);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void init(File[] files, File[] geoFiles) throws Exception {
        ArrayList<NetCDFFile> ncdfal = new ArrayList<NetCDFFile>();
        ArrayList<NetCDFFile> ncdfalGeo = new ArrayList<NetCDFFile>();

        int numGrans = files.length;

        for (int k = 0; k < files.length; k++) {
            ncdfal.add(new NetCDFFile(files[k].getAbsolutePath()));
            ncdfalGeo.add(new NetCDFFile(geoFiles[k].getAbsolutePath()));
        }

        GranuleAggregation aggReader = new GranuleAggregation(ncdfal, "Track");
        GranuleAggregation aggGeoReader = new GranuleAggregation(ncdfalGeo, "Track");

        String name = files[0].getName();
        String[] strs = name.split("_");
        String prodStr = strs[0];
        String sensorName = null;

        String geoDatasetPath = null;
        geoDatasetPath = getGeoDataSetPath(prodStr);
        if (geoDatasetPath == null) {
            throw new Exception("Can't determine geo dataset path for: " + prodStr);
        }

        sensorName = getSensorName(prodStr);
        if (sensorName == null) {
            throw new Exception("Can't determine sensor name for: " + prodStr);
        }


        RangeProcessor[] rngProcessors = new RangeProcessor[numGrans];

        boolean unsigned = true;
        boolean unpack = true;
        boolean range_check_after_scaling = true;
        if (prodStr.equals("SVDNB") || prodStr.equals("SVM13")) {
            unsigned = false;
            unpack = false;
            range_check_after_scaling = false;
        }

        HashMap metadata = fillMetadataTable(
                "XTrack",
                "Track",
                getProductName(prodStr),
                prodStr,
                "XTrack",
                "Track",
                geoDatasetPath + "Longitude",
                geoDatasetPath + "Latitude",
                new String[]{"Track", "XTrack"},
                new String[]{"Track", "XTrack"},
                new String[]{"Track", "XTrack"},
                unsigned, unpack, range_check_after_scaling
        );

        float scale = 1f;
        float offset = 0f;
        for (int k = 0; k < numGrans; k++) {
            NetCDFFile ncFile = ncdfal.get(k);
            String scaleFactorName = getScaleFactorName(prodStr);
            if (scaleFactorName != null) {
                int[] dimLens = ncFile.getDimensionLengths(scaleFactorName);
                float[] fltArray = ncFile.getFloatArray(scaleFactorName, new int[]{0}, new int[]{dimLens[0]}, new int[]{1});
                scale = fltArray[0];
                offset = fltArray[1];
            }
            float[] range = getValidRange(prodStr);
            double[] missing = getMissing(prodStr);
            RangeProcessor rngProcessor = new RangeProcessor(ncFile, metadata, scale, offset, range[0], range[1], missing);
            rngProcessors[k] = rngProcessor;
        }

        AggregationRangeProcessor aggRngProcessor = new AggregationRangeProcessor(rngProcessors);
        aggReader.addPreProcessor((String) metadata.get(SwathAdapter.array_name), aggRngProcessor);
        SwathAdapter adapter = new SwathAdapter(aggReader, metadata, aggGeoReader);

        String array = getProductName(prodStr);

        setDataChoice(adapter, 0, getProductName(prodStr));
    }

    public HashMap fillMetadataTable(String xtrack, String track,
                                     String array, String range, String geoXtrack, String geoTrack,
                                     String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                     boolean unsigned, boolean unpack, boolean range_check_after_scaling) {

        HashMap metadata = SwathAdapter.getEmptyMetadataTable();

        metadata.put(SwathAdapter.xtrack_name, xtrack);
        metadata.put(SwathAdapter.track_name, track);
        metadata.put(SwathAdapter.geo_xtrack_name, geoXtrack);
        metadata.put(SwathAdapter.geo_track_name, geoTrack);
        metadata.put(SwathAdapter.array_name, array);
        metadata.put(SwathAdapter.range_name, range);
        metadata.put(SwathAdapter.lon_array_name, lonArray);
        metadata.put(SwathAdapter.lat_array_name, latArray);
        if (unsigned) {
            metadata.put("unsigned", "true");
        }
        if (unpack) {
            metadata.put("unpack", "true");
        }
        if (range_check_after_scaling) {
            metadata.put("range_check_after_scaling", "true");
        }
        if (lonArrayDims != null) {
            metadata.put(SwathAdapter.lon_array_dimension_names, lonArrayDims);
        }
        if (latArrayDims != null) {
            metadata.put(SwathAdapter.lat_array_dimension_names, latArrayDims);
        }
        if (arrayDims != null) {
            metadata.put(SwathAdapter.array_dimension_names, arrayDims);
        }

        return metadata;
    }

    public SwathAdapter getSwathAdapter(DataChoice dataChoice) {
        for (int k = 0; k < myDataChoices.size(); k++) {
            if (myDataChoices.get(k).equals(dataChoice)) {
                return swathAdapters.get(k);
            }
        }
        return null;
    }

    public MultiSpectralData getMultiSpectralData(DataChoice choice) {
        return msdMap.get(choice);
    }


    void setDataChoice(SwathAdapter adapter, int idx, String name) {
        HashMap subset = adapter.getDefaultSubset();
        DataSelection dataSel = new MultiDimensionSubset(subset);
        DataChoice dataChoice = new DataChoice(this, name, null);
        dataChoice.setDataSelection(dataSel);
        myDataChoices.add(dataChoice);
        swathAdapters.add(adapter);
        msdMap.put(dataChoice, multiSpectData);
    }


    public Data getData(DataChoice dataChoice, DataSelection dataSelection)
            throws VisADException, RemoteException {
        try {
            SwathAdapter adapter = getSwathAdapter(dataChoice);

            MultiDimensionSubset select = null;
            select = (MultiDimensionSubset) dataChoice.getDataSelection();

            HashMap subset = select.getSubset();

            Data data = adapter.getData(subset);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public boolean getDoReproject(DataChoice choice) {
        return true;
    }

    @Override
    public boolean getDoFilter(DataChoice choice) {
        return false;
    }

    @Override
    public boolean getOverlayAsMask(DataChoice choice) {
        return true;
    }

    public static String getGeoDataSetPath(String prefix) {
        String path = null;

        Pattern pattern = Pattern.compile("^AFIMG");
        Matcher m = pattern.matcher(prefix);
        if (m.find()) {
            path = "All_Data/VIIRS-IMG-GEO-TC_All/";
            return path;
        }

        pattern = Pattern.compile("^AFMOD");
        m = pattern.matcher(prefix);
        if (m.find()) {
            path = "All_Data/VIIRS-MOD-GEO-TC_All/";
            return path;
        }

        return path;
    }

    public static String getSensorName(String prefix) {
        String name = null;
        Pattern pattern = Pattern.compile("^AFMOD");
        Matcher m = pattern.matcher(prefix);
        if (m.find()) {
            name = "VIIRS-M";
            return name;
        }

        pattern = Pattern.compile("^AFIMG");
        m = pattern.matcher(prefix);
        if (m.find()) {
            name = "VIIRS-I";
            return name;
        }

        return name;
    }

    public static String getProductName(String prodStr) {
        String name = null;
        switch (prodStr) {
            case "AFIMG":
                name = "fire_mask";
                break;
            case "AFMOD":
                name = "fire_mask";
        }
        return name;
    }

    public static String getScaleFactorName(String prodStr) {
        String name = null;
        switch (prodStr) {

        }
        return name;
    }

    public static double[] getMissing(String prodStr) {
        float[] missing = null;
        switch (prodStr) {
            case "AFIMG":
                missing = new float[]{Float.NaN};
                break;
            case "AFMOD":
                missing = new float[]{Float.NaN};
                break;
        }
        double[] dblArray = new double[missing.length];
        for (int i = 0; i < dblArray.length; i++) dblArray[i] = missing[i];
        return dblArray;
    }

    public static float[] getValidRange(String prodStr) {
        float[] validRange = new float[]{-Float.MAX_VALUE, Float.MAX_VALUE};
        switch (prodStr) {
        }
        return validRange;
    }

    @Override
    public float getNadirResolution(DataChoice choice) throws Exception {
        String name = choice.getName();
        switch (name) {
            case "AFIMG":
                return 380f;
            case "AFMOD":
                return 770f;
        }
        return 770f;
    }

    @Override
    public boolean canUnderstand(File[] files) {
        String fname = files[0].getName();
        if (fname.startsWith("AFIMG")) {
            return true;
        } else if (fname.startsWith("AFMOD")) {
            return true;
        }
        return false;
    }
}
