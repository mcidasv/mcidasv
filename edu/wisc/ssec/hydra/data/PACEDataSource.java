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

package edu.wisc.ssec.hydra.data;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;

import edu.wisc.ssec.adapter.PACE_Spectrum;
import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.adapter.SpectrumAdapter;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.GranuleAggregation;
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.RangeProcessor;
import edu.wisc.ssec.adapter.AggregationRangeProcessor;
import edu.wisc.ssec.adapter.MultiDimensionAdapter;
import edu.wisc.ssec.adapter.MultiSpectralAggr;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.hydra.Hydra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Date;

import ucar.unidata.util.ColorTable;
import visad.Data;
import visad.VisADException;


public class PACEDataSource extends DataSource {

    private static final Logger logger = LoggerFactory.getLogger(PACEDataSource.class);

    String dateTimeStamp = null;

    Date dateTime;

    String description = null;

    float nadirResolution;

    File[] files = null;

    String[] bandNames = null;
    float[] centerWavelength = null;

    private ArrayList<MultiSpectralData> msdPace = new ArrayList<>();
    private ArrayList<MultiSpectralData> multiSpectralData = new ArrayList<>();
    private HashMap<String, MultiSpectralData> msdMap = new HashMap<>();

    public PACEDataSource(File directory) throws Exception {
        this(directory.listFiles());
    }

    public PACEDataSource(ArrayList<File> files) throws Exception {
        this(files.toArray(new File[]{null}));
    }

    public PACEDataSource(File[] files) throws Exception {

        logger.info("TJJ PACEDataSource constructor in...");

        if (!canUnderstand(files)) {
            throw new Exception("PACEDataSource doesn't understand input files");
        }

        ArrayList<File> dataList = new <File>ArrayList();
        for (int i = 0; i < files.length; i++) {
            String fname = files[i].getName();
            if (fname.startsWith("PACE_OCI") && fname.contains("L1B") && fname.endsWith(".nc")) {
                dataList.add(files[i]);
            }
        }

        logger.info("TJJ num good files: " + files.length);

        ArrayList<String> sortedList = DataSource.getTimeSortedFilenameList(dataList);
        for (int i = 0; i < sortedList.size(); i++) {
            files[i] = new File(sortedList.get(i));
        }

        File file = files[0];

        logger.info("TJJ time stuff...");

        dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName());
        description = DataSource.getDescriptionFromFilename(file.getName());
        dateTime = DataSource.getDateTimeFromFilename(file.getName());

        try {
            init(files);
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
        }

        logger.info("TJJ PACEDataSource constructor out...");
    }

    public boolean canUnderstand(File[] files) {
        if (files[0].getName().startsWith("PACE_OCI")
                && files[0].getName().contains("L1B")
                && files[0].getName().endsWith(".nc")) {
            return true;
        }
        return false;
    }

    void init(File[] files) throws Exception {

        logger.info("TJJ PACEDataSource init() in...");

        ArrayList<NetCDFFile> ncdfal = new ArrayList<>();
        for (int k = 0; k < files.length; k++) {
            ncdfal.add(new NetCDFFile(files[k].getAbsolutePath()));
        }
        GranuleAggregation aggReader = new GranuleAggregation(ncdfal, "number_of_scans");

        MultiSpectralData msd = null;
        String[] productPaths;
        String path = "observation_data/";
        productPaths = new String[] { path + "rhot_blue", path + "rhot_red", path + "rhot_SWIR" };
        msd = buildPACE(aggReader, productPaths);
        nadirResolution = 14000;

        multiSpectralData.add(msd);

        DataChoice choice = setDataChoice(msd, 0, "rhot_blue");

        msdMap.put(choice.getName(), msd);
        logger.info("TJJ PACEDataSource init() out...");
    }

    MultiSpectralData buildPACE(GranuleAggregation reader, String[] productPaths) throws Exception {

        logger.info("TJJ buildPACE() in, LEN PRODUCT PATHS: " + productPaths.length);

        boolean unsigned = false;
        boolean unpack = false;
        boolean range_check_after_scaling = false;

        MultiSpectralData aggrMSDs;
        String[] channelIndex_names = {"blue_bands", "red_bands", "SWIR_bands"};
        String[] channelNames = {"sensor_band_parameters/blue_wavelength", "sensor_band_parameters/red_wavelength", "sensor_band_parameters/SWIR_wavelength"};

        // for (int bandRangeIndex = 0; bandRangeIndex < productPaths.length; bandRangeIndex++) {
        for (int bandRangeIndex = 0; bandRangeIndex < 1; bandRangeIndex++) {

            String productPath = productPaths[bandRangeIndex];

            HashMap metadata = fillSwathMetadataTable(
                    "number_of_scans",
                    "ccd_pixels",
                    channelIndex_names[bandRangeIndex],
                    null,
                    productPath,
                    "reflectance",
                    "number_of_scans",
                    "ccd_pixels",
                    "geolocation_data/longitude",
                    "geolocation_data/latitude",
                    null,
                    null,
                    null,
                    unsigned, unpack, range_check_after_scaling, "PACE"
            );

            HashMap<String, Object> spectTable = SpectrumAdapter.getEmptyMetadataTable();
            spectTable.put("array_name", productPath);
            spectTable.put("product_name", "PACE");
            spectTable.put(SpectrumAdapter.channelIndex_name, channelIndex_names[bandRangeIndex]);
            spectTable.put(SpectrumAdapter.channelType, "channel_number");
            spectTable.put(SpectrumAdapter.channels_name, channelNames[bandRangeIndex]);
            spectTable.put(SpectrumAdapter.x_dim_name, "number_of_scans");
            spectTable.put(SpectrumAdapter.y_dim_name, "ccd_pixels");
            spectTable.put(SpectrumAdapter.FOVindex_name, null);

            float scale = 1.0f;
            float offset = 0.0f;

            float[] range = getValidRange();
            double[] missing = getMissing();
            RangeProcessor rngProcessor = new RangeProcessor(reader, metadata, scale, offset, range[0], range[1], missing);

            AggregationRangeProcessor aggrRangeProcessor = new AggregationRangeProcessor(reader, rngProcessor);
            reader.addPreProcessor(productPath, aggrRangeProcessor);

            logger.info("TJJ band range chunk #" + (bandRangeIndex + 1) + ", create SwathAdapter");
            SwathAdapter adapter = new SwathAdapter(reader, metadata);
            logger.info("TJJ band range chunk #" + (bandRangeIndex + 1) + ", create PACE_Spectrum");
            SpectrumAdapter psa = new PACE_Spectrum(reader, spectTable);
            logger.info("TJJ band range chunk #" + (bandRangeIndex + 1) + ", create MultiSpectralData");
            MultiSpectralData msd = new MultiSpectralData(adapter, psa);
            // TJJ temporary hardcode from ncdump, but still not affecting display range
            msd.setDataRange(new float[] { 0.0f, 1.3f } );
            logger.info("TJJ Valid Range: " + msd.getDataRange().toString());
            msdPace.add(msd);

        }

        aggrMSDs = new MultiSpectralAggr(msdPace.toArray(new MultiSpectralData[msdPace.size()]), "radiances");
        // Start of blue range
        aggrMSDs.setInitialWavenumber(305.0f);

        logger.info("TJJ buildPACE() out...");
        return aggrMSDs;
    }

    public Date getDateTime() {
        return dateTime;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean getDoFilter(DataChoice choice) {
        return true;
    }

    @Override
    public boolean getOverlayAsMask(DataChoice choice) {
        return false;
    }

    public boolean hasMultiSpectralData() {
        return true;
    }

    @Override
    public boolean isSounder() {
        return true;
    }

    public HashMap fillSwathMetadataTable(String xtrack, String track, String channel, String fovIndex,
                                          String array, String range, String geoXtrack, String geoTrack,
                                          String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                          boolean unsigned, boolean unpack, boolean range_check_after_scaling, String product_name) {

        logger.info("TJJ fillSwathMetadataTable() in...");
        HashMap metadata = SwathAdapter.getEmptyMetadataTable();

        metadata.put(SwathAdapter.xtrack_name, xtrack);
        metadata.put(SwathAdapter.track_name, track);
        metadata.put(SpectrumAdapter.channelIndex_name, channel);
        if (fovIndex != null) {
            metadata.put(SpectrumAdapter.FOVindex_name, fovIndex);
        }
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
        if (product_name != null) {
            metadata.put("product_name", product_name);
        }

        logger.info("TJJ fillSwathMetadataTable() out...");
        return metadata;
    }

    @Override
    public MultiSpectralData getMultiSpectralData(DataChoice choice) {
        return msdMap.get(choice.getName());
    }

    public MultiSpectralData getMultiSpectralData(String name) {
        return msdMap.get(name);
    }

    DataChoice setDataChoice(MultiSpectralData adapter, int idx, String name) {
        HashMap subset = adapter.getDefaultSubset();
        DataSelection dataSel = new MultiDimensionSubset(subset);
        DataChoice dataChoice = new DataChoice(this, name, null);
        dataChoice.setDataSelection(dataSel);
        myDataChoices.add(dataChoice);
        return dataChoice;
    }

    @Override
    public Data getData(DataChoice dataChoice, DataSelection dataSelection)
            throws VisADException, RemoteException {
        try {
            MultiDimensionAdapter adapter = getMultiSpectralData(dataChoice);

            MultiDimensionSubset select = (MultiDimensionSubset) dataChoice.getDataSelection();
            HashMap subset = select.getSubset();

            Data data = adapter.getData(subset);
            return data;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public static double[] getMissing() {
        double[] missing = new double[] {
                -32767.f,
        };
        return missing;
    }

    public static float[] getValidRange() {
        float[] validRange = new float[2];
        validRange[0] = 0.0f;
        validRange[1] = 1.3f;
        return validRange;
    }

    public ColorTable getDefaultColorTable(DataChoice choice) {
        return Hydra.invGrayTable;
    }

    @Override
    public float getNadirResolution(DataChoice choice) throws Exception {
        return nadirResolution;
    }

    public String toString() {

        StringBuilder result = new StringBuilder();
        result.append("PACEDataSource toString(): \n");
        result.append("{\n");
        for (
                String key : msdMap.keySet()) {
            MultiSpectralData value = msdMap.get(key);
            result.append("\t").append("Adapter name: " + key).append("-> ").append(value.toString()).append(",\n");

        }
        result.append("}");
        return result.toString();

    }
}
