/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
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

import edu.wisc.ssec.adapter.HDFArray;
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

    // make this a class variable, since getValidRange() is a public method
    private static float[] overallMinMax = new float[2];

    private ArrayList<MultiSpectralData> msdPace = new ArrayList<>();
    private ArrayList<MultiSpectralData> multiSpectralData = new ArrayList<>();
    private HashMap<String, MultiSpectralData> msdMap = new HashMap<>();

    public PACEDataSource(File directory) throws Exception {
        this(directory.listFiles());
    }

    public PACEDataSource(ArrayList<File> files) throws Exception {
        this(files.toArray(new File[]{null}));
    }

    private int getPaceVersion(File file) {
        String name = file.getName();
        if (!name.startsWith("PACE_OCI")) return -1;
        if (name.endsWith("V3.nc")) return 3;
        if (name.endsWith("V2.nc")) return 2;
        return -1;
    }

    public PACEDataSource(File[] files) throws Exception {
		
        logger.info("PACEDataSource constructor in...");

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

        logger.info("num good files: " + files.length);

        ArrayList<String> sortedList = DataSource.getTimeSortedFilenameList(dataList);
        for (int i = 0; i < sortedList.size(); i++) {
            files[i] = new File(sortedList.get(i));
        }

        File file = files[0];

        dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName());
        description = DataSource.getDescriptionFromFilename(file.getName());
        dateTime = DataSource.getDateTimeFromFilename(file.getName());

        try {
            init(files);
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
        }

        logger.info("PACEDataSource constructor out...");
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
        this.files = files;

        logger.trace("PACEDataSource init() in...");

        int version = getPaceVersion(files[0]);
        String scanDim = (version == 3 ? "scans" : "number_of_scans");

        NetCDFFile metaDataHandle = null;

        ArrayList<NetCDFFile> ncdfal = new ArrayList<>();
        for (File f : files) {
            ncdfal.add(new NetCDFFile(f.getAbsolutePath()));
            if (metaDataHandle == null) {
                metaDataHandle = new NetCDFFile(f.getAbsolutePath());
            }
        }

        GranuleAggregation aggReader = new GranuleAggregation(ncdfal, scanDim);

        String spectraPath = "sensor_band_parameters/";
        String[] spectraPaths = { spectraPath + "blue_wavelength", spectraPath + "red_wavelength", spectraPath + "SWIR_wavelength" };

        String dataPath = "observation_data/";
        String[] productPaths = { dataPath + "rhot_blue", dataPath + "rhot_red", dataPath + "rhot_SWIR" };

        String[] channelIndexNames = { "blue_bands", "red_bands", "SWIR_bands" };
        String[] channelNames = {
            "sensor_band_parameters/blue_wavelength",
            "sensor_band_parameters/red_wavelength",
            "sensor_band_parameters/SWIR_wavelength"
        };

        MultiSpectralData msd = buildPACE(aggReader, productPaths, spectraPaths, channelIndexNames, channelNames, metaDataHandle);
        multiSpectralData.add(msd);
        DataChoice choice = setDataChoice(msd, 0, "Reflectance");
        msdMap.put(choice.getName(), msd);

        logger.trace("PACEDataSource init() out...");
    }

    MultiSpectralData buildPACE(
            GranuleAggregation reader,
            String[] productPaths,
            String[] spectraPaths,
            String[] channelIndexNames,
            String[] channelNames,
            NetCDFFile metaDataHandle
            ) throws Exception {

        int version = getPaceVersion(files[0]);
        boolean isV3 = (version == 3);
        String scanDim = isV3 ? "scans" : "number_of_scans";
        // The name of this dimension CHANGES FOR SWIR BANDS!, so set this within the product loop
        String pixDim = null;

        boolean unsigned = false;
        boolean unpack = false;
        boolean range_check_after_scaling = false;
        float firstWavenum = 0.0f;

        // in case some products have different valid max/min values, track overall range
        float overallValidMin = 0.0f;
        float overallValidMax = 0.0f;

        for (int k = 0; k < productPaths.length; k++) {
            String productPath = productPaths[k];
            logger.info("buildPACE() loop for product: " + productPath);
            if (productPath.contains("SWIR")) {
                pixDim = isV3 ? "pixels" : "SWIR_pixels";
            } else {
                pixDim = isV3 ? "pixels" : "ccd_pixels";
            }

            HashMap<String, Object> metadata = fillSwathMetadataTable(
                    scanDim,
                    pixDim,
                    channelIndexNames[k],
                    null,
                    productPath,
                    "reflectance",
                    scanDim,
                    pixDim,
                    "geolocation_data/longitude",
                    "geolocation_data/latitude",
                    null,
                    null,
                    null,
                    unsigned,
                    unpack,
                    range_check_after_scaling,
                    "PACE"
            );

            HashMap<String, Object> spectTable = SpectrumAdapter.getEmptyMetadataTable();
            spectTable.put("array_name", productPath);
            spectTable.put("product_name", "PACE");
            spectTable.put(SpectrumAdapter.channelIndex_name, channelIndexNames[k]);
            //spectTable.put(SpectrumAdapter.channelType, "channel_number");
            spectTable.put(SpectrumAdapter.channelType, "Wavelength");
            spectTable.put(SpectrumAdapter.channels_name, channelNames[k]);
            spectTable.put(SpectrumAdapter.x_dim_name, scanDim);
            spectTable.put(SpectrumAdapter.y_dim_name, pixDim);
            spectTable.put(SpectrumAdapter.FOVindex_name, null);

            // Load metadata for the band groups and wavelengths of all bands
            int dimLen = metaDataHandle.getDimensionLength(channelIndexNames[k]);
            logger.debug("dimension length for: " + channelIndexNames[k] + " is: " + dimLen);
            int[] startArr = new int[1];
            startArr[0] = 0;
            int[] countArr = new int[1];
            countArr[0] = dimLen;
            int[] strideArr = new int[1];
            strideArr[0] = 1;
            float[] spectraArr = metaDataHandle.getFloatArray(spectraPaths[k], startArr, countArr, strideArr);
            logger.info("THESE BETTER MATCH, dimLen: " + dimLen + ", and spectraArr.length: " + spectraArr.length);
            spectTable.put("num_channels", spectraArr.length);
            logger.info("First wavenumber in this spectra group: " + spectraArr[0]);
            spectTable.put("first_wavenumber", spectraArr[0]);
            if (firstWavenum == 0.0f) {
                firstWavenum = spectraArr[0];
            }
            spectTable.put("wavenumber_array", spectraArr);

            // pull out valid min & max from observation data
            HDFArray validMinArr = metaDataHandle.getArrayAttribute(productPaths[k], "valid_min");
            float validMin = ((float[]) validMinArr.getArray())[0];
            HDFArray validMaxArr = metaDataHandle.getArrayAttribute(productPaths[k], "valid_max");
            float validMax = ((float[]) validMaxArr.getArray())[0];

            logger.debug("valid min: " + validMin + ", valid max: " + validMax);

            // I don't know for certain each band group has the same min and max, so track overall min max
            // Use that in the aggregated reader
            if (validMin < overallValidMin) overallValidMin = validMin;
            if (validMax > overallValidMax) overallValidMax = validMax;

            float scale = 1.0f;
            float offset = 0.0f;

            float[] range = new float[2];
            range[0] = validMin;
            range[1] = validMax;
            double[] missing = getMissing();

            RangeProcessor rngProcessor = new RangeProcessor(reader, metadata, scale, offset, range[0], range[1], missing);
            AggregationRangeProcessor aggrRangeProcessor = new AggregationRangeProcessor(reader, rngProcessor);
            reader.addPreProcessor(productPath, aggrRangeProcessor);

            SwathAdapter adapter = new SwathAdapter(reader, metadata);

            SpectrumAdapter psa = new PACE_Spectrum(reader, spectTable);

            MultiSpectralData msd = new MultiSpectralData(adapter, psa, "Reflectance", "Reflectance", null, null);

            overallMinMax[0] = overallValidMin;
            overallMinMax[1] = overallValidMax;
            msd.setDataRange(overallMinMax);
            msdPace.add(msd);

        }

        MultiSpectralAggr aggrMSDs = new MultiSpectralAggr(msdPace.toArray(new MultiSpectralData[msdPace.size()]), "radiances");
        aggrMSDs.setInitialWavenumber(firstWavenum);
        aggrMSDs.setDataRange(overallMinMax);

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

    // Valid range has been pulled out of file metadata, it varies V2 to V3
    // So just return the static float array we filled during build/init
    public static float[] getValidRange() {
        return overallMinMax;
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
        for (String key : msdMap.keySet()) {
            MultiSpectralData value = msdMap.get(key);
            result.append("\t").append("Adapter name: " + key).append("-> ").append(value.toString()).append(",\n");
        }
        result.append("}");
        return result.toString();

    }
}
