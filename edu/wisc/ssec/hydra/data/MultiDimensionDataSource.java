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

import edu.wisc.ssec.adapter.ArrayAdapter;
import edu.wisc.ssec.adapter.GOESGridAdapter;
import edu.wisc.ssec.adapter.MultiDimensionAdapter;
import edu.wisc.ssec.adapter.MultiDimensionReader;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.RangeProcessor;
import edu.wisc.ssec.adapter.SpectrumAdapter;
import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.adapter.TrackAdapter;
import edu.wisc.ssec.hydra.GEOSProjection;
import edu.wisc.ssec.hydra.GEOSTransform;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import ucar.unidata.util.ColorTable;

import visad.Data;
import visad.VisADException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import ucar.unidata.util.Range;


/**
 * A data source for Multi Dimension Data
 */

public class MultiDimensionDataSource extends DataSource {

    /**
     * Sources file
     */
    protected ArrayList<File> sources;
    protected String filename;

    protected File file;

    protected MultiDimensionReader reader;

    protected MultiDimensionReader geoReader;

    protected MultiDimensionAdapter[] adapters = null;
    protected HashMap[] defaultSubsets = null;
    private HashMap<String, MultiDimensionAdapter> adapterMap = new HashMap<String, MultiDimensionAdapter>();
    protected DataGroup[] categoriesArray = null;
    protected float[] nadirResolution = null;


    protected SpectrumAdapter spectrumAdapter;

    private HashMap defaultSubset;
    public TrackAdapter track_adapter;

    String dateTimeStamp;
    Date dateTime;
    String description;
    boolean reduceBowTie = true;
    boolean doReproject = true;

    ArrayAdapter l2P_flagsAdapter;

    public MultiDimensionDataSource(ArrayList<File> sources) throws Exception {

        this.sources = sources;
        this.file = sources.get(0);
        this.filename = file.getAbsolutePath();

        setup();
        doMakeDataChoices();
    }

    public void setup() throws Exception {
        String name = file.getName();
        dateTimeStamp = DataSource.getDateTimeStampFromFilename(name);
        dateTime = DataSource.getDateTimeFromFilename(name);
        description = DataSource.getDescriptionFromFilename(name);

        try {
            if (filename.contains("MYD02SSH")) { // get file union
                String other = (String) sources.get(1).getName();
                if (filename.endsWith("nav.hdf")) {
                    String tmp = filename;
                    filename = other;
                    other = tmp;
                }
                reader = NetCDFFile.makeUnion(filename, other);
            } else if (filename.contains("mod14")) { // IMAPP Fire mask
                String other = filename.replaceAll("mod14", "geo");
                reader = new NetCDFFile(filename);
                geoReader = new NetCDFFile(other);
            } else if (name.startsWith("MYD14") || name.startsWith("MOD14")) { // NASA Fire mask
                String prefix = null;
                if (name.startsWith("MYD14")) prefix = "MYD03";
                if (name.startsWith("MOD14")) prefix = "MOD03";
                String[] strs = name.split("\\.");
                String regex = prefix + "\\." + strs[1] + "\\." + strs[2] + "\\." + strs[3] + ".*";
                File dir = new File(file.getParent());
                Pattern pattern = Pattern.compile(regex);
                File[] list = dir.listFiles();
                File geoFile = null;
                for (int k = 0; k < list.length; k++) {
                    Matcher matcher = pattern.matcher(list[k].getName());
                    if (matcher.find()) {
                        geoFile = list[k];
                        break;
                    }
                }
                if (geoFile == null) {
                    throw new Exception("Can't find or open matching geolocation file for: " + filename);
                }
                reader = new NetCDFFile(filename);
                geoReader = new NetCDFFile(geoFile.getPath());
            } else {
                reader = new NetCDFFile(filename);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("cannot create NetCDF reader for file: " + filename);
            if (reader != null) {
                reader.close();
            }
            if (geoReader != null) {
                geoReader.close();
            }
            return;
        }

        adapters = new MultiDimensionAdapter[2];
        defaultSubsets = new HashMap[2];


        if (name.startsWith("MOD04") || name.startsWith("MYD04") ||
                ((name.startsWith("a1") || name.startsWith("t1")) && name.contains("mod04"))) {

            String path = "mod04/Data_Fields/";
            String[] arrayNames;
            String[] rangeNames;

            float res;

            if (!name.contains("_3k")) {
                arrayNames = new String[]{"Optical_Depth_Land_And_Ocean", "Deep_Blue_Aerosol_Optical_Depth_550_Land", "Optical_Depth_Ratio_Small_Ocean_0\\.55micron"};
                rangeNames = new String[]{"OD", "OD_Blue", "OD_small"};
                res = 3060f;
            } else {
                arrayNames = new String[]{"Optical_Depth_Land_And_Ocean", "Optical_Depth_Ratio_Small_Ocean_0\\.55micron"};
                rangeNames = new String[]{"OD", "OD_small"};
                res = 10100f;
            }
            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];
            categoriesArray = new DataGroup[adapters.length];
            nadirResolution = new float[adapters.length];

            for (int k = 0; k < arrayNames.length; k++) {
                HashMap table = SwathAdapter.getEmptyMetadataTable();
                table.put("array_name", path.concat(arrayNames[k]));
                table.put("lon_array_name", "mod04/Geolocation_Fields/Longitude");
                table.put("lat_array_name", "mod04/Geolocation_Fields/Latitude");
                table.put("XTrack", "Cell_Across_Swath");
                table.put("Track", "Cell_Along_Swath");
                table.put("geo_Track", "Cell_Along_Swath");
                table.put("geo_XTrack", "Cell_Across_Swath");
                table.put("scale_name", "scale_factor");
                table.put("offset_name", "add_offset");
                table.put("fill_value_name", "_FillValue");
                table.put("range_name", rangeNames[k]);
                adapters[k] = new SwathAdapter(reader, table);
                categoriesArray[k] = new DataGroup("2D grid");
                defaultSubset = adapters[k].getDefaultSubset();
                defaultSubsets[k] = defaultSubset;
                nadirResolution[k] = res;
            }
        } else if (name.contains("seadas") && (name.startsWith("a1") || name.startsWith("t1"))) {
            String path = "Geophysical_Data/";
            String[] arrayNames;
            String[] rangeNames;

            arrayNames = new String[]{"chlor_a", "sst4", "sst", "par"};
            rangeNames = new String[]{"chlor", "SST_4um", "SST", "photo_rad"};

            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];
            categoriesArray = new DataGroup[adapters.length];
            nadirResolution = new float[adapters.length];

            for (int k = 0; k < arrayNames.length; k++) {
                HashMap table = SwathAdapter.getEmptyMetadataTable();
                table.put("array_name", path.concat(arrayNames[k]));
                table.put("lon_array_name", "Navigation_Data/longitude");
                table.put("lat_array_name", "Navigation_Data/latitude");
                table.put("XTrack", "Pixels_per_Scan_Line");
                table.put("Track", "Number_of_Scan_Lines");
                table.put("geo_Track", "Number_of_Scan_Lines");
                table.put("geo_XTrack", "Number_of_Pixel_Control_Points");
                table.put("scale_name", "slope");
                table.put("offset_name", "intercept");
                table.put("fill_value_name", "bad_value_scaled");
                table.put("range_name", rangeNames[k]);
                adapters[k] = new SwathAdapter(reader, table);
                categoriesArray[k] = new DataGroup("2D grid");
                defaultSubset = adapters[k].getDefaultSubset();
                defaultSubsets[k] = defaultSubset;
                nadirResolution[k] = 1020f;
            }
        } else if (name.contains("SEADAS_npp") || name.contains("SEADAS_modis")) {
            String path = "Geophysical_Data/";
            String[] arrayNames;
            String[] rangeNames;

            if (name.contains("SEADAS_modis")) {
                arrayNames = new String[]{"chlor_a", "sst4", "sst", "par", "pic", "poc", "Kd_490", "nflh", "Rrs_412", "Rrs_443", "Rrs_469", "Rrs_488", "Rrs_531"};
                rangeNames = new String[]{"chlor", "SST_4um", "SST", "photo_rad", "pic", "poc", "Kd_490", "nflh", "Rrs_412", "Rrs_443", "Rrs_469", "Rrs_488", "Rrs_531"};
            } else {
                arrayNames = new String[]{"chlor_a", "sst3", "sst", "par", "pic", "poc", "Kd_490", "nflh", "Rrs_410", "Rrs_443", "Rrs_486", "Rrs_551", "Rrs_671"};
                rangeNames = new String[]{"chlor", "SST_3", "SST", "photo_rad", "pic", "poc", "Kd_490", "nflh", "Rrs_410", "Rrs_443", "Rrs_486", "Rrs_551", "Rrs_671"};
                reduceBowTie = false;
            }


            MultiDimensionAdapter[] tmp_adapters = new MultiDimensionAdapter[arrayNames.length];
            HashMap[] tmp_defaultSubsets = new HashMap[arrayNames.length];
            DataGroup[] tmp_categoriesArray = new DataGroup[arrayNames.length];
            float[] tmp_nadirResolution = new float[arrayNames.length];

            int nok = 0;
            for (int k = 0; k < arrayNames.length; k++) {
                try {
                    HashMap table = SwathAdapter.getEmptyMetadataTable();
                    table.put("array_name", path.concat(arrayNames[k]));
                    table.put("lon_array_name", "Navigation_Data/longitude");
                    table.put("lat_array_name", "Navigation_Data/latitude");
                    table.put("XTrack", "Pixels_per_Scan_Line");
                    table.put("Track", "Number_of_Scan_Lines");
                    table.put("geo_Track", "Number_of_Scan_Lines");
                    table.put("geo_XTrack", "Number_of_Pixel_Control_Points");
                    table.put("scale_name", "slope");
                    table.put("offset_name", "intercept");
                    table.put("fill_value_name", "bad_value_unscaled");
                    table.put("range_check_after_scaling", "range_check_after_scaling");
                    table.put("unpack", "true");
                    table.put("range_name", rangeNames[k]);
                    tmp_adapters[k] = new SwathAdapter(reader, table);
                    tmp_categoriesArray[k] = new DataGroup("2D grid");
                    defaultSubset = tmp_adapters[k].getDefaultSubset();
                    tmp_defaultSubsets[k] = defaultSubset;
                    tmp_nadirResolution[k] = 770f;
                    nok++;
                } catch (Exception exc) {
                    System.out.println("cant make adapter for: " + arrayNames[k]);
                }
            }

            adapters = new MultiDimensionAdapter[nok];
            defaultSubsets = new HashMap[nok];
            categoriesArray = new DataGroup[nok];
            nadirResolution = new float[nok];

            int cnt = 0;
            for (int k = 0; k < arrayNames.length; k++) {
                if (tmp_adapters[k] != null) {
                    adapters[cnt] = tmp_adapters[k];
                    defaultSubsets[cnt] = tmp_defaultSubsets[k];
                    categoriesArray[cnt] = tmp_categoriesArray[k];
                    nadirResolution[cnt] = tmp_nadirResolution[k];
                    cnt++;
                }
            }
        } else if ((name.startsWith("MOD35") || name.startsWith("MYD35")) ||
                ((name.contains("mod35") && (name.startsWith("a1") || name.startsWith("t1"))))) {
            String dataPath = "mod35/Data_Fields/";
            String geoPath = "mod35/Geolocation_Fields/";
            String[] arrayNames = new String[]{"Cloud_Mask"};
            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];
            categoriesArray = new DataGroup[adapters.length];
            nadirResolution = new float[adapters.length];

            int k = 0;
            HashMap table = SwathAdapter.getEmptyMetadataTable();
            table.put("array_name", "mod35/Data_Fields/Cloud_Mask");
            table.put("lon_array_name", "mod35/Geolocation_Fields/Longitude");
            table.put("lat_array_name", "mod35/Geolocation_Fields/Latitude");
            table.put("XTrack", "Cell_Across_Swath_1km");
            table.put("Track", "Cell_Along_Swath_1km");
            table.put("geo_Track", "Cell_Along_Swath_5km");
            table.put("geo_XTrack", "Cell_Across_Swath_5km");
            table.put("byteSegmentIndexName", "Byte_Segment");
            table.put("scale_name", "scale_factor");
            table.put("offset_name", "add_offset");
            table.put("fill_value_name", "_FillValue");
            table.put("range_name", arrayNames[k]);
            table.put("product_name", "ModisCloudMask");

            table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
            table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
            table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
            table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0148148148));

            SwathAdapter swathAdapter = new SwathAdapter(reader, table);
            swathAdapter.setDefaultStride(10);
            defaultSubset = swathAdapter.getDefaultSubset();
            double[] coords = new double[]{0.0, 0.0, 1.0};
            defaultSubset.put("Byte_Segment", coords);

            adapters[k] = swathAdapter;
            defaultSubsets[k] = defaultSubset;
            categoriesArray[k] = new DataGroup("1km swath");
            nadirResolution[k] = 1020f;
        }
        /* Need to keep for backward compatibility with old files
        else if ( name.contains("mod35") && (name.startsWith("a1") || name.startsWith("t1"))) {
            hasImagePreview = true;
            String[] arrayNames = new String[] {"Cloud_Mask"};
            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];
            categoriesArray = new List[adapters.length];

            int k = 0;
            HashMap table = SwathAdapter.getEmptyMetadataTable();
            table.put("array_name", "Cloud_Mask");
            table.put("lon_array_name", "Longitude");
            table.put("lat_array_name", "Latitude");
            table.put("XTrack", "Cell_Across_Swath_1km");
            table.put("Track", "Cell_Along_Swath_1km");
            table.put("geo_Track", "Cell_Along_Swath_5km");
            table.put("geo_XTrack", "Cell_Across_Swath_5km");
            table.put("byteSegmentIndexName", "Byte_Segment");
            table.put("scale_name", "scale_factor");
            table.put("offset_name", "add_offset");
            table.put("fill_value_name", "_FillValue");
            table.put("range_name", arrayNames[k]);
            table.put("product_name", "ModisCloudMask");

            table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
            table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
            table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
            table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0148148148));

            SwathAdapter swathAdapter = new SwathAdapter(reader, table);
            swathAdapter.setDefaultStride(10);
            defaultSubset = swathAdapter.getDefaultSubset();
            double[] coords = new double[] {0.0, 0.0, 1.0};
            defaultSubset.put("Byte_Segment", coords);

            adapters[k] = swathAdapter;
            defaultSubsets[k] = defaultSubset;
            categoriesArray[k] = new DataGroup("1km swath");
        }
        */
        else if (name.contains("mod14") || name.startsWith("MOD14") || name.startsWith("MYD14")) {
            String[] arrayNames = new String[]{"fire_mask"};
            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];
            categoriesArray = new DataGroup[adapters.length];
            nadirResolution = new float[adapters.length];

            int k = 0;
            HashMap table = SwathAdapter.getEmptyMetadataTable();
            table.put("array_name", "fire_mask");
            table.put("lon_array_name", "MODIS_Swath_Type_GEO/Geolocation_Fields/Longitude");
            table.put("lat_array_name", "MODIS_Swath_Type_GEO/Geolocation_Fields/Latitude");
            table.put("XTrack", "pixels_per_scan_line");
            table.put("Track", "number_of_scan_lines");
            table.put("geo_Track", "nscans*10");
            table.put("geo_XTrack", "mframes");
            table.put("range_name", arrayNames[k]);

            SwathAdapter swathAdapter = new SwathAdapter(reader, table, geoReader);
            swathAdapter.setDefaultStride(10);
            defaultSubset = swathAdapter.getDefaultSubset();

            adapters[k] = swathAdapter;
            defaultSubsets[k] = defaultSubset;
            categoriesArray[k] = new DataGroup("1km swath");
            nadirResolution[k] = 1020f;
        } else if (name.contains("mod28") || name.startsWith("MOD28") || name.startsWith("MYD28")) {
            String[] arrayNames = new String[]{"Sea_Surface_Temperature"};
            String[] rangeNames = new String[]{"SST"};
            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];
            categoriesArray = new DataGroup[adapters.length];
            nadirResolution = new float[adapters.length];

            int k = 0;
            HashMap table = SwathAdapter.getEmptyMetadataTable();
            table.put("array_name", "Sea_Surface_Temperature");
            table.put("lon_array_name", "Longitude");
            table.put("lat_array_name", "Latitude");
            table.put("XTrack", "Cell_Across_Swath_1km");
            table.put("Track", "Cell_Along_Swath_1km");
            table.put("geo_Track", "Cell_Along_Swath_5km");
            table.put("geo_XTrack", "Cell_Across_Swath_5km");
            table.put("scale_name", "scale_factor");
            table.put("offset_name", "add_offset");
            table.put("fill_value_name", "_FillValue");
            table.put("geo_fillValue_name", "_FillValue");
            table.put("range_name", arrayNames[k]);

            table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
            table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
            table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
            table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0148148148));

            SwathAdapter swathAdapter = new SwathAdapter(reader, table);
            swathAdapter.setDefaultStride(10);
            defaultSubset = swathAdapter.getDefaultSubset();

            adapters[k] = swathAdapter;
            defaultSubsets[k] = defaultSubset;
            categoriesArray[k] = new DataGroup("1km swath");
            nadirResolution[k] = 1020f;
        } else if (name.startsWith("MOD06") || name.startsWith("MYD06")) {
            String path = "mod06/Data_Fields/";
            String[] arrayNames = new String[]{"Cloud_Optical_Thickness", "Cloud_Effective_Radius", "Cloud_Water_Path"};
            //String[] rangeNames = new String[] {"CldOptThk", "CldEffRad", "CldWaterPath"};
            String[] rangeNames = new String[]{"Cloud_Optical_Thickness", "Cloud_Effective_Radius", "Cloud_Water_Path"};
            String[] arrayNames_5km = new String[]{"Cloud_Top_Pressure", "Cloud_Top_Temperature", "Cloud_Fraction", "Cloud_Phase_Infrared"};
            //String[] rangeNames_5km = new String[] {"CldTopPress", "CldTopTemp", "CldFrac", "CldPhaseEmis"};
            String[] rangeNames_5km = new String[]{"Cloud_Top_Pressure", "Cloud_Top_Temperature", "Cloud_Fraction", "Cloud_Phase_Infrared"};

            adapters = new MultiDimensionAdapter[arrayNames.length + arrayNames_5km.length];
            defaultSubsets = new HashMap[arrayNames.length + arrayNames_5km.length];
            categoriesArray = new DataGroup[adapters.length];
            nadirResolution = new float[adapters.length];


            for (int k = 0; k < arrayNames.length; k++) {
                HashMap table = SwathAdapter.getEmptyMetadataTable();
                table.put("array_name", path.concat(arrayNames[k]));
                table.put("lon_array_name", "mod06/Geolocation_Fields/Longitude");
                table.put("lat_array_name", "mod06/Geolocation_Fields/Latitude");
                table.put("XTrack", "Cell_Across_Swath_1km");
                table.put("Track", "Cell_Along_Swath_1km");
                table.put("geo_Track", "Cell_Along_Swath_5km");
                table.put("geo_XTrack", "Cell_Across_Swath_5km");
                table.put("scale_name", "scale_factor");
                table.put("offset_name", "add_offset");
                table.put("fill_value_name", "_FillValue");
                table.put("range_name", rangeNames[k]);

                table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
                table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
                table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
                table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0148148148));

                SwathAdapter swathAdapter = new SwathAdapter(reader, table);
                swathAdapter.setDefaultStride(10);
                defaultSubset = swathAdapter.getDefaultSubset();
                adapters[k] = swathAdapter;
                defaultSubsets[k] = defaultSubset;
                categoriesArray[k] = new DataGroup("1km swath");
                nadirResolution[k] = 1020f;
            }

            for (int k = 0; k < arrayNames_5km.length; k++) {
                HashMap table = SwathAdapter.getEmptyMetadataTable();
                table.put("array_name", path.concat(arrayNames_5km[k]));
                table.put("lon_array_name", "mod06/Geolocation_Fields/Longitude");
                table.put("lat_array_name", "mod06/Geolocation_Fields/Latitude");
                table.put("XTrack", "Cell_Across_Swath_5km");
                table.put("Track", "Cell_Along_Swath_5km");
                table.put("geo_Track", "Cell_Along_Swath_5km");
                table.put("geo_XTrack", "Cell_Across_Swath_5km");
                table.put("scale_name", "scale_factor");
                table.put("offset_name", "add_offset");
                table.put("fill_value_name", "_FillValue");
                table.put("range_name", rangeNames_5km[k]);

                SwathAdapter swathAdapter = new SwathAdapter(reader, table);
                defaultSubset = swathAdapter.getDefaultSubset();
                adapters[arrayNames.length + k] = swathAdapter;
                defaultSubsets[arrayNames.length + k] = defaultSubset;
                categoriesArray[arrayNames.length + k] = new DataGroup("5km swath");
                nadirResolution[arrayNames.length + k] = 5000f;
            }
        } else if ((name.startsWith("a1") || name.startsWith("t1")) && name.contains("mod06")) {
            String path = "mod06/Data_Fields/";
            String[] arrayNames = new String[]{"Cloud_Optical_Thickness", "Cloud_Effective_Radius", "Cloud_Water_Path"};
            String[] arrayNames_5km = new String[]{"Cloud_Top_Pressure", "Cloud_Top_Temperature", "Cloud_Fraction", "Cloud_Phase_Infrared"};

            adapters = new MultiDimensionAdapter[arrayNames.length + arrayNames_5km.length];
            defaultSubsets = new HashMap[arrayNames.length + arrayNames_5km.length];
            categoriesArray = new DataGroup[adapters.length];
            nadirResolution = new float[adapters.length];


            for (int k = 0; k < arrayNames.length; k++) {
                HashMap table = SwathAdapter.getEmptyMetadataTable();
                table.put("array_name", path.concat(arrayNames[k]));
                table.put("lon_array_name", "mod06/Geolocation_Fields/Longitude");
                table.put("lat_array_name", "mod06/Geolocation_Fields/Latitude");
                table.put("array_dimension_names", new String[]{"Cell_Along_Swath_1km", "Cell_Across_Swath_1km"});
                table.put("lon_array_dimension_names", new String[]{"Cell_Along_Swath_5km", "Cell_Across_Swath_5km"});
                table.put("lat_array_dimension_names", new String[]{"Cell_Along_Swath_5km", "Cell_Across_Swath_5km"});
                table.put("XTrack", "Cell_Across_Swath_1km");
                table.put("Track", "Cell_Along_Swath_1km");
                table.put("geo_Track", "Cell_Along_Swath_5km");
                table.put("geo_XTrack", "Cell_Across_Swath_5km");
                table.put("scale_name", "scale_factor");
                table.put("offset_name", "add_offset");
                table.put("fill_value_name", "_FillValue");
                table.put("range_name", arrayNames[k]);

                table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
                table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
                table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
                table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0148148148));

                SwathAdapter swathAdapter = new SwathAdapter(reader, table);
                swathAdapter.setDefaultStride(10);
                defaultSubset = swathAdapter.getDefaultSubset();
                adapters[k] = swathAdapter;
                defaultSubsets[k] = defaultSubset;
                categoriesArray[k] = new DataGroup("1km swath");
                nadirResolution[k] = 1020f;
            }

            for (int k = 0; k < arrayNames_5km.length; k++) {
                HashMap table = SwathAdapter.getEmptyMetadataTable();
                table.put("array_name", path.concat(arrayNames_5km[k]));
                table.put("lon_array_name", "mod06/Geolocation_Fields/Longitude");
                table.put("lat_array_name", "mod06/Geolocation_Fields/Latitude");
                table.put("array_dimension_names", new String[]{"Cell_Along_Swath_5km", "Cell_Across_Swath_5km"});
                table.put("lon_array_dimension_names", new String[]{"Cell_Along_Swath_5km", "Cell_Across_Swath_5km"});
                table.put("lat_array_dimension_names", new String[]{"Cell_Along_Swath_5km", "Cell_Across_Swath_5km"});
                table.put("XTrack", "Cell_Across_Swath_5km");
                table.put("Track", "Cell_Along_Swath_5km");
                table.put("geo_Track", "Cell_Along_Swath_5km");
                table.put("geo_XTrack", "Cell_Across_Swath_5km");
                table.put("scale_name", "scale_factor");
                table.put("offset_name", "add_offset");
                table.put("fill_value_name", "_FillValue");
                table.put("range_name", arrayNames_5km[k]);

                SwathAdapter swathAdapter = new SwathAdapter(reader, table);
                defaultSubset = swathAdapter.getDefaultSubset();
                adapters[arrayNames.length + k] = swathAdapter;
                defaultSubsets[arrayNames.length + k] = defaultSubset;
                categoriesArray[arrayNames.length + k] = new DataGroup("5km swath");
                nadirResolution[arrayNames.length + k] = 5000f;
            }
        } else if (name.startsWith("MYD02SSH")) {
            String[] arrayNames = null;

            if (name.endsWith("level2.hdf")) {
                arrayNames = new String[]{"cld_press_acha", "cld_temp_acha", "cld_height_acha", "cloud_type",
                        "cloud_albedo_0_65um_nom", "cloud_transmission_0_65um_nom", "cloud_fraction"};
            } else if (name.endsWith("obs.hdf")) {
                arrayNames = new String[]{"refl_0_65um_nom", "refl_0_86um_nom", "refl_3_75um_nom", "refl_1_60um_nom", "refl_1_38um_nom",
                        "temp_3_75um_nom", "temp_11_0um_nom", "temp_12_0um_nom", "temp_6_7um_nom",
                        "temp_8_5um_nom", "temp_13_3um_nom"};
            }

            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];

            categoriesArray = new DataGroup[adapters.length];
            nadirResolution = new float[adapters.length];

            for (int k = 0; k < arrayNames.length; k++) {
                HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
                swthTable.put("array_name", arrayNames[k]);
                swthTable.put("lon_array_name", "pixel_longitude");
                swthTable.put("lat_array_name", "pixel_latitude");
                swthTable.put("XTrack", "pixel_elements_along_scan_direction");
                swthTable.put("Track", "scan_lines_along_track_direction");
                swthTable.put("geo_Track", "scan_lines_along_track_direction");
                swthTable.put("geo_XTrack", "pixel_elements_along_scan_direction");
                swthTable.put("scale_name", "SCALE_FACTOR");
                swthTable.put("offset_name", "ADD_OFFSET");
                swthTable.put("fill_value_name", "_FILLVALUE");
                swthTable.put("geo_scale_name", "SCALE_FACTOR");
                swthTable.put("geo_offset_name", "ADD_OFFSET");
                swthTable.put("geo_fillValue_name", "_FILLVALUE");
                swthTable.put("range_name", arrayNames[k]);
                swthTable.put("unpack", "unpack");

                SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
                HashMap subset = swathAdapter0.getDefaultSubset();
                defaultSubset = subset;
                adapters[k] = swathAdapter0;
                defaultSubsets[k] = defaultSubset;
                categoriesArray[k] = new DataGroup("2D grid");
                nadirResolution[k] = 1020f;
            }
        } else if (name.startsWith("geocatL2_OT")) {
            String[] arrayNames = new String[]{"ot_overshooting_top_grid_magnitude"};
            String[] rangeNames = new String[]{"OT_grid_mag"};
            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];
            categoriesArray = new DataGroup[adapters.length];
            nadirResolution = new float[adapters.length];

            int k = 0;
            HashMap table = SwathAdapter.getEmptyMetadataTable();
            table.put("array_name", arrayNames[0]);
            table.put("lon_array_name", "pixel_longitude");
            table.put("lat_array_name", "pixel_latitude");
            table.put("XTrack", "elements");
            table.put("Track", "lines");
            table.put("fill_value_name", "_FillValue");
            table.put("geo_Track", "lines");
            table.put("geo_XTrack", "elements");
            table.put("geo_scale_name", "scale_factor");
            table.put("geo_offset_name", "add_offset");
            table.put("geo_fillValue_name", "_FillValue");
            table.put("range_name", rangeNames[k]);

            SwathAdapter swathAdapter = new SwathAdapter(reader, table);
            swathAdapter.setDefaultStride(10);
            defaultSubset = swathAdapter.getDefaultSubset();

            adapters[k] = swathAdapter;
            defaultSubsets[k] = defaultSubset;
            categoriesArray[k] = new DataGroup("1km swath");
            nadirResolution[k] = 1020f;
            reduceBowTie = false;
        } else if (name.startsWith("geocatL2") && name.endsWith("ci.hdf")) {
            String[] arrayNames = new String[]{"box_average_11um_ctc", "box_average_11um_ctc_scaled", "conv_init", "cloud_type"};

            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];

            categoriesArray = new DataGroup[adapters.length];
            nadirResolution = new float[adapters.length];

            for (int k = 0; k < arrayNames.length; k++) {
                HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
                swthTable.put("array_name", arrayNames[k]);
                swthTable.put("lon_array_name", "lon");
                swthTable.put("lat_array_name", "lat");
                swthTable.put("XTrack", "Elements");
                swthTable.put("Track", "Lines");
                swthTable.put("geo_Track", "Lines");
                swthTable.put("geo_XTrack", "Elements");
                swthTable.put("range_name", arrayNames[k]);

                SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
                swathAdapter0.setDefaultStride(1);
                HashMap subset = swathAdapter0.getDefaultSubset();
                defaultSubset = subset;
                adapters[k] = swathAdapter0;
                defaultSubsets[k] = defaultSubset;
                categoriesArray[k] = new DataGroup("1km swath");
                nadirResolution[k] = 1020f;
            }
            reduceBowTie = false;
        } else if (name.startsWith("geocatL2") && name.contains("HIMAWARI-8") && name.contains("FLDK")) {
            String[] arrayNames = new String[]{"ACHA_mode_8_cloud_top_height", "ACHA_mode_8_cloud_top_pressure", "ACHA_mode_8_cloud_top_temperature",
                    "eps_cmask_ahi_cloud_mask", "enterprise_cldphase_10_11_13_14_15_cloud_phase", "enterprise_cldphase_10_11_13_14_15_cloud_type"};

            String[] rangeNames = new String[]{"Cld_Top_Hght", "Cld_Top_Pres", "Cld_Top_Temp", "Cloud_Mask", "Cloud_Phase", "Cloud_Type"};


            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];

            nadirResolution = new float[adapters.length];

            double scale_x = 5.588799029559623E-5;
            double offset_x = -0.15371991730803744;
            double scale_y = 5.588799029559623E-5;
            double offset_y = -0.15371991730803744;

            float subLonDegrees = 140.7f;
            String sweepAngleAxis = "GEOS";
            double inverty = 1.0;
            int xDimLen = 5500;
            int yDimLen = 5500;

            GEOSTransform geosTran = new GEOSTransform(subLonDegrees, sweepAngleAxis);

            GEOSProjection mapProj = new GEOSProjection(geosTran, 0.0, 0.0, (double) xDimLen, (double) yDimLen,
                    scale_x, offset_x, inverty * scale_y, inverty * offset_y);

            for (int k = 0; k < arrayNames.length; k++) {
                HashMap metadata = GOESGridAdapter.getEmptyMetadataTable();
                metadata.put(MultiDimensionAdapter.array_name, arrayNames[k]);
                metadata.put(MultiDimensionAdapter.range_name, rangeNames[k]);
                metadata.put(GOESGridAdapter.gridX_name, "elements");
                metadata.put(GOESGridAdapter.gridY_name, "lines");
                metadata.put(MultiDimensionAdapter.scale_name, "scale_factor");
                metadata.put(MultiDimensionAdapter.offset_name, "add_offset");
                metadata.put(MultiDimensionAdapter.fill_value_name, "_FillValue");
                metadata.put("range_check_after_scaling", "false");
                metadata.put("unpack", "true");

                GOESGridAdapter goesAdapter = new GOESGridAdapter(reader, metadata, mapProj, 10);
                HashMap subset = goesAdapter.getDefaultSubset();

                defaultSubset = subset;
                adapters[k] = goesAdapter;
                defaultSubsets[k] = defaultSubset;
                nadirResolution[k] = 2000;
            }
            reduceBowTie = false;
            doReproject = false;
        } else if (name.startsWith("geocatL2") && name.contains("HIMAWARI-8")) {
            String[] arrayNames = new String[]{"ACHA_mode_8_cloud_top_height", "ACHA_mode_8_cloud_top_pressure", "ACHA_mode_8_cloud_top_temperature",
                    "eps_cmask_ahi_cloud_mask", "enterprise_cldphase_10_11_13_14_15_cloud_phase", "enterprise_cldphase_10_11_13_14_15_cloud_type"};

            String[] rangeNames = new String[]{"Cld_Top_Hght", "Cld_Top_Pres", "Cld_Top_Temp", "Cloud_Mask", "Cloud_Phase", "Cloud_Type"};

            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];

            nadirResolution = new float[adapters.length];

            for (int k = 0; k < arrayNames.length; k++) {
                HashMap metadata = SwathAdapter.getEmptyMetadataTable();
                metadata.put(MultiDimensionAdapter.array_name, arrayNames[k]);
                metadata.put(MultiDimensionAdapter.range_name, rangeNames[k]);
                metadata.put(SwathAdapter.xtrack_name, "elements");
                metadata.put(SwathAdapter.track_name, "lines");
                metadata.put(MultiDimensionAdapter.scale_name, "scale_factor");
                metadata.put(MultiDimensionAdapter.offset_name, "add_offset");
                metadata.put(MultiDimensionAdapter.fill_value_name, "_FillValue");
                metadata.put("range_check_after_scaling", "false");
                metadata.put("unpack", "true");
                metadata.put(SwathAdapter.lon_array_name, "pixel_longitude");
                metadata.put(SwathAdapter.lat_array_name, "pixel_latitude");
                metadata.put(SwathAdapter.geo_xtrack_name, "elements");
                metadata.put(SwathAdapter.geo_track_name, "lines");
                metadata.put(SwathAdapter.lon_array_dimension_names, new String[]{"lines", "elements"});
                metadata.put(SwathAdapter.lat_array_dimension_names, new String[]{"lines", "elements"});
                metadata.put(SwathAdapter.geo_scale_name, "scale_factor");
                metadata.put(SwathAdapter.geo_fillValue_name, "_FillValue");

                SwathAdapter adapter = new SwathAdapter(reader, metadata);
                HashMap subset = adapter.getDefaultSubset();

                defaultSubset = subset;
                adapters[k] = adapter;
                defaultSubsets[k] = defaultSubset;
                nadirResolution[k] = 2000;
            }
            reduceBowTie = false;
            doReproject = false;
        } else if (name.startsWith("ACSPO-VIIRS")) {
            String[] arrayNames = new String[]{"sst_regression"};
            String[] rangeNames = new String[]{"SST"};
            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];
            nadirResolution = new float[arrayNames.length];

            for (int k = 0; k < arrayNames.length; k++) {
                HashMap metadata = SwathAdapter.getEmptyMetadataTable();
                metadata.put(MultiDimensionAdapter.array_name, arrayNames[k]);
                metadata.put(MultiDimensionAdapter.range_name, rangeNames[k]);
                metadata.put(SwathAdapter.xtrack_name, "pixels_across_track");
                metadata.put(SwathAdapter.track_name, "scan_lines_along_track");
                metadata.put(SwathAdapter.array_dimension_names, new String[]{"scan_lines_along_track", "pixels_across_track"});
                metadata.put(SwathAdapter.lon_array_name, "longitude");
                metadata.put(SwathAdapter.lat_array_name, "latitude");
                metadata.put(SwathAdapter.geo_xtrack_name, "pixels_across_track");
                metadata.put(SwathAdapter.geo_track_name, "scan_lines_along_track");
                metadata.put(SwathAdapter.lon_array_dimension_names, new String[]{"scan_lines_along_track", "pixels_across_track"});
                metadata.put(SwathAdapter.lat_array_dimension_names, new String[]{"scan_lines_along_track", "pixels_across_track"});

                SwathAdapter adapter = new SwathAdapter(reader, metadata);
                adapter.setDefaultStride(10);
                HashMap subset = adapter.getDefaultSubset();

                defaultSubset = subset;
                adapters[k] = adapter;
                defaultSubsets[k] = defaultSubset;
                nadirResolution[k] = 780;
            }

            HashMap metadata = SwathAdapter.getEmptyMetadataTable();
            metadata.put(MultiDimensionAdapter.array_name, "acspo_mask");
            metadata.put(SwathAdapter.xtrack_name, "pixels_across_track");
            metadata.put(SwathAdapter.track_name, "scan_lines_along_track");
            metadata.put(SwathAdapter.array_dimension_names, new String[]{"scan_lines_along_track", "pixels_across_track"});
            metadata.put(SwathAdapter.lat_array_dimension_names, new String[]{"scan_lines_along_track", "pixels_across_track"});
            l2P_flagsAdapter = new ArrayAdapter(reader, metadata);
        } else if (name.contains("SST") && name.contains("VIIRS_NPP-ACSPO")) {
            String[] arrayNames = new String[]{"sea_surface_temperature"};
            String[] rangeNames = new String[]{"SST"};
            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];
            nadirResolution = new float[arrayNames.length];

            for (int k = 0; k < arrayNames.length; k++) {
                HashMap metadata = SwathAdapter.getEmptyMetadataTable();
                metadata.put(MultiDimensionAdapter.array_name, arrayNames[k]);
                metadata.put(MultiDimensionAdapter.range_name, rangeNames[k]);
                metadata.put(SwathAdapter.xtrack_name, "ni");
                metadata.put(SwathAdapter.track_name, "nj");
                metadata.put(SwathAdapter.array_dimension_names, new String[]{"time", "nj", "ni"});
                metadata.put(MultiDimensionAdapter.scale_name, "scale_factor");
                metadata.put(MultiDimensionAdapter.offset_name, "add_offset");
                metadata.put(MultiDimensionAdapter.fill_value_name, "_FillValue");
                metadata.put("range_check_after_scaling", "false");
                metadata.put("unpack", "true");
                metadata.put(SwathAdapter.lon_array_name, "lon");
                metadata.put(SwathAdapter.lat_array_name, "lat");
                metadata.put(SwathAdapter.geo_xtrack_name, "ni");
                metadata.put(SwathAdapter.geo_track_name, "nj");
                metadata.put(SwathAdapter.lon_array_dimension_names, new String[]{"nj", "ni"});
                metadata.put(SwathAdapter.lat_array_dimension_names, new String[]{"nj", "ni"});

                SwathAdapter adapter = new SwathAdapter(reader, metadata);
                adapter.setDefaultStride(10);
                HashMap subset = adapter.getDefaultSubset();
                double[] coords = new double[]{0.0, 0.0, 1.0};
                subset.put("time", coords);

                defaultSubset = subset;
                adapters[k] = adapter;
                defaultSubsets[k] = defaultSubset;
                nadirResolution[k] = 780;
            }

            HashMap metadata = SwathAdapter.getEmptyMetadataTable();
            metadata.put(MultiDimensionAdapter.array_name, "l2p_flags");
            metadata.put(SwathAdapter.xtrack_name, "ni");
            metadata.put(SwathAdapter.track_name, "nj");
            metadata.put(SwathAdapter.array_dimension_names, new String[]{"time", "nj", "ni"});
            metadata.put(SwathAdapter.lat_array_dimension_names, new String[]{"nj", "ni"});
            l2P_flagsAdapter = new ArrayAdapter(reader, metadata);
        } else if (name.contains("SEVIRI_C")) {
            String[] arrayNames = new String[]{"ch1", "ch2", "ch3", "ch4", "ch5", "ch6", "ch7", "ch8", "ch9", "ch10", "ch11"};


            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];

            nadirResolution = new float[adapters.length];

            for (int k = 0; k < arrayNames.length; k++) {
                HashMap metadata = SwathAdapter.getEmptyMetadataTable();
                metadata.put(MultiDimensionAdapter.array_name, arrayNames[k]);
                metadata.put(MultiDimensionAdapter.range_name, arrayNames[k]);
                metadata.put(SwathAdapter.xtrack_name, "num_cols_vis_ir");
                metadata.put(SwathAdapter.track_name, "num_rows_vis_ir");
                metadata.put(MultiDimensionAdapter.scale_name, "scale_factor");
                metadata.put(MultiDimensionAdapter.offset_name, "add_offset");
                //metadata.put("range_check_after_scaling", "false");
                //metadata.put("unpack", "true");
                metadata.put(SwathAdapter.lon_array_name, "lon");
                metadata.put(SwathAdapter.lat_array_name, "lat");
                metadata.put(SwathAdapter.geo_xtrack_name, "num_cols_vis_ir");
                metadata.put(SwathAdapter.geo_track_name, "num_rows_vis_ir");
                metadata.put(SwathAdapter.array_dimension_names, new String[]{"num_rows_vis_ir", "num_cols_vis_ir"});
                metadata.put(SwathAdapter.lon_array_dimension_names, new String[]{"num_rows_vis_ir", "num_cols_vis_ir"});
                metadata.put(SwathAdapter.lat_array_dimension_names, new String[]{"num_rows_vis_ir", "num_cols_vis_ir"});

                SwathAdapter adapter = new SwathAdapter(reader, metadata);
                HashMap subset = adapter.getDefaultSubset();

                defaultSubset = subset;
                adapters[k] = adapter;
                defaultSubsets[k] = defaultSubset;
                nadirResolution[k] = 3000;
            }
            reduceBowTie = false;
            doReproject = false;
        } else if (name.startsWith("NPR-MIRS-IMG")) {
            String[] arrayNames = new String[]{"TPW", "RR"};
            String[] rangeNames = new String[]{"TotalPrecipWater", "RainRate"};

            adapters = new MultiDimensionAdapter[arrayNames.length];
            defaultSubsets = new HashMap[arrayNames.length];
            nadirResolution = new float[adapters.length];

            for (int k = 0; k < arrayNames.length; k++) {
                HashMap metadata = SwathAdapter.getEmptyMetadataTable();
                metadata.put(MultiDimensionAdapter.array_name, arrayNames[k]);
                metadata.put(MultiDimensionAdapter.range_name, rangeNames[k]);
                metadata.put(SwathAdapter.xtrack_name, "Field_of_view");
                metadata.put(SwathAdapter.track_name, "Scanline");
                metadata.put(MultiDimensionAdapter.scale_name, "scale_factor");
                metadata.put(SwathAdapter.lon_array_name, "Longitude");
                metadata.put(SwathAdapter.lat_array_name, "Latitude");
                metadata.put(SwathAdapter.geo_xtrack_name, "Field_of_view");
                metadata.put(SwathAdapter.geo_track_name, "Scanline");
                metadata.put(SwathAdapter.array_dimension_names, new String[]{"Scanline", "Field_of_view"});
                metadata.put(SwathAdapter.lon_array_dimension_names, new String[]{"Scanline", "Field_of_view"});
                metadata.put(SwathAdapter.lat_array_dimension_names, new String[]{"Scanline", "Field_of_view"});

                SwathAdapter adapter = new SwathAdapter(reader, metadata);
                adapter.setRangeProcessor(new RangeProcessor(reader, metadata, new double[]{-999}));
                adapter.setDefaultStride(1);
                HashMap subset = adapter.getDefaultSubset();

                defaultSubset = subset;
                adapters[k] = adapter;
                defaultSubsets[k] = defaultSubset;
                nadirResolution[k] = 16000;
            }
            reduceBowTie = false;
            doReproject = false;
        } else {
            if (reader != null) {
                reader.close();
            }
            throw new Exception("MultiDimensionDataSource doesn't understand these files");
        }
    }


    /**
     * Make and insert the <code>DataChoice</code>-s for this
     * <code>DataSource</code>.
     */
    public void doMakeDataChoices() {
        DataChoice choice = null;
        if (adapters != null) {
            for (int idx = 0; idx < adapters.length; idx++) {
                try {
                    if (adapters[idx] != null) {
                        String arrayName = adapters[idx].getArrayName();
                        choice = doMakeDataChoice(idx, arrayName);
                        if (choice != null) {
                            adapterMap.put(choice.getName(), adapters[idx]);
                            myDataChoices.add(choice);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("doMakeDataChoice failed");
                }
            }
        }
    }

    private DataChoice doMakeDataChoice(int idx, String var) throws Exception {
        String name = var;
        DataSelection dataSel = (defaultSubsets[idx] == null) ? new MultiDimensionSubset() : new MultiDimensionSubset(defaultSubsets[idx]);

        DataChoice ddc = null;

        if (categoriesArray != null) {
            ddc = new DataChoice(this, name, categoriesArray[idx]);
        } else {
            ddc = new DataChoice(this, name, null);
        }

        ddc.setDataSelection(dataSel);

        return ddc;
    }

    /**
     * Check to see if this <code>HDFHydraDataSource</code> is equal to the object
     * in question.
     *
     * @param o object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if (!(o instanceof MultiDimensionDataSource)) {
            return false;
        }
        return (this == (MultiDimensionDataSource) o);
    }

    public String getDatasetName() {
        return filename;
    }

    public void setDatasetName(String name) {
        filename = name;
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
        String name = choice.getName();

        switch (name) {
            case "Cloud_Mask":
                return false;
            case "Cloud_Phase_Infrared":
            case "Cloud_Phase":
                return false;
            case "fire_mask":
                return false;
            case "Cloud_Type":
                return false;
            default:
                break;
        }

        return true;
    }

    @Override
    public Range getDefaultColorRange(DataChoice choice) {
        String name = choice.getName();

        Range rng;

        if (name.equals("Cloud_Mask")) {
            rng = new Range(0f - 0.5f, 3f + 0.5f);
        } else if (name.equals("Cloud_Phase_Infrared") || name.equals("Cloud_Phase")) {
            rng = new Range(0f - 0.5f, 6f + 0.5f);
        } else if (name.equals("fire_mask")) {
            rng = new Range(0f - 0.5f, 9f + 0.5f);
        } else if (name.equals("Cloud_Type")) {
            rng = new Range(0f - 0.5f, 9 + 0.5f);
        } else if (choice.getName().equals("Cloud_Phase") && file.getName().contains("geocatL2")) {
            rng = new Range(0f - 0.5f, 5f + 0.5f);
        } else {
            rng = super.getDefaultColorRange(choice);
        }

        return rng;
    }

    @Override
    public boolean getOverlayAsMask(DataChoice choice) {
        String name = choice.getName();

        if (name.equals("fire_mask")) {
            return true;
        } else if (name.equals("OT_grid_mag")) {
            return true;
        }
        return false;
    }


    public ColorTable getDefaultColorTable(DataChoice choice) {
        ColorTable clrTbl;

        if (choice.getName().equals("Cloud_Phase") && file.getName().contains("geocatL2")) {

            float[][] palette = new float[][]{{0f, 15f, 44f, 11f, 251f, 243f},
                    {0f, 190f, 248f, 102f, 246f, 42f},
                    {0f, 250f, 43f, 12f, 47f, 250f},
                    {0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f}};

            for (int i = 0; i < palette[0].length; i++) palette[0][i] /= 256;
            for (int i = 0; i < palette[1].length; i++) palette[1][i] /= 256;
            for (int i = 0; i < palette[2].length; i++) palette[2][i] /= 256;

            clrTbl = new ColorTable();
            clrTbl.setTable(palette);
        } else {
            clrTbl = super.getDefaultColorTable(choice);
        }

        return clrTbl;
    }

    public float getNadirResolution(DataChoice choice) throws Exception {
        String name = choice.getName();
        for (int k = 0; k < myDataChoices.size(); k++) {
            if (name.equals(((DataChoice) myDataChoices.get(k)).getName())) {
                return nadirResolution[k];
            }
        }
        throw new Exception("nadirResolution not specified for: " + name);
    }

    public boolean getReduceBowtie(DataChoice choice) {
        return reduceBowTie;
    }

    public boolean getDoReproject(DataChoice choice) {
        return doReproject;
    }

    public Data getData(DataChoice dataChoice, DataSelection dataSelection)
            throws VisADException, RemoteException {

        MultiDimensionAdapter adapter = null;
        adapter = adapterMap.get(dataChoice.getName());


        Data data = null;
        if (adapters == null) {
            return data;
        }

        HashMap subset = null;

        MultiDimensionSubset select = (MultiDimensionSubset) dataChoice.getDataSelection();

        try {

            if (select != null) {
                subset = select.getSubset();
            }

            if (subset != null) {
                data = adapter.getData(subset);
                if (l2P_flagsAdapter != null) {
                    float[][] sst = ((visad.FlatField) data).getFloats(false);
                    short[] flags = (short[]) l2P_flagsAdapter.readArray(subset);
                    short sb = -32768;
                    for (int i = 0; i < flags.length; i++) {
                        if (((flags[i] & sb) == sb)) {
                            sst[0][i] = Float.NaN;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("getData exception e=" + e);
        }

        return data;
    }

    @Override
    public boolean canUnderstand(File[] files) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}