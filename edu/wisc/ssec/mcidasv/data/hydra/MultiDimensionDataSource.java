/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2017
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
package edu.wisc.ssec.mcidasv.data.hydra;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.util.Misc;

import visad.Data;
import visad.FlatField;
import visad.GriddedSet;
import visad.VisADException;

import edu.wisc.ssec.mcidasv.data.HydraDataSource;
import edu.wisc.ssec.mcidasv.data.PreviewSelection;

/**
 * A data source for Multi Dimension Data 
 */

public class MultiDimensionDataSource extends HydraDataSource {

    private static final Logger logger = LoggerFactory.getLogger(MultiDimensionDataSource.class);

    /** Sources file */
    protected String filename;

    protected MultiDimensionReader reader;

    protected MultiDimensionAdapter[] adapters = null;
    protected Map[] defaultSubsets = null;
    private Map<String, MultiDimensionAdapter> adapterMap = new HashMap<>();
    protected Hashtable[] propsArray = null;
    protected List[] categoriesArray = null;

    protected SpectrumAdapter spectrumAdapter;

    private static final String DATA_DESCRIPTION = "Multi Dimension Data";

    private Map<String, double[]> defaultSubset;
    public TrackAdapter track_adapter;
    private MultiSpectralData multiSpectData;

    private List categories;
    private boolean hasImagePreview = false;
    private boolean hasTrackPreview = false;
    
    private TrackSelection trackSelection = null;

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    public MultiDimensionDataSource() {}

    /**
     * Construct a new HYDRA hdf data source.
     * @param  descriptor  descriptor for this {@code DataSource}
     * @param  fileName  name of the hdf file to read
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public MultiDimensionDataSource(DataSourceDescriptor descriptor,
                                 String fileName, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(fileName), properties);
    }

    /**
     * Construct a new HYDRA hdf data source.
     * @param  descriptor  descriptor for this {@code DataSource}
     * @param  newSources  List of filenames
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public MultiDimensionDataSource(DataSourceDescriptor descriptor,
                                 List newSources, Hashtable properties)
            throws VisADException {
        super(descriptor, newSources, DATA_DESCRIPTION, properties);

        this.filename = (String) sources.get(0);

        try {
          setup();
        } catch (Exception e) {
          throw new VisADException("could not set up MultiDimensionDataSource", e);
        }
    }

    public void setup() throws Exception {

        try {
          if (filename.contains("MYD02SSH")) { // get file union
            String other = (String) sources.get(1);
            if (filename.endsWith("nav.hdf")) {
              String tmp = filename;
              filename = other;
              other = tmp;
            }
            reader = NetCDFFile.makeUnion(filename, other);
          }
          else {
            reader = new NetCDFFile(filename);
          }
        } catch (Exception e) {
          logger.error("Cannot create NetCDF reader for file: " + filename, e);
        }

        adapters = new MultiDimensionAdapter[2];
        defaultSubsets = new HashMap[2]; 
        Hashtable<String, String[]> properties = new Hashtable<>();
        
        String name = (new File(filename)).getName();

        if (name.startsWith("MOD04") || name.startsWith("MYD04")) {
          Map<String, Object> table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "mod04/Data_Fields/Optical_Depth_Land_And_Ocean");
          table.put("lon_array_name", "mod04/Geolocation_Fields/Longitude");
          table.put("lat_array_name", "mod04/Geolocation_Fields/Latitude");
          table.put("XTrack", "Cell_Across_Swath");
          table.put("Track", "Cell_Along_Swath");
          table.put("geo_Track", "Cell_Along_Swath");
          table.put("geo_XTrack", "Cell_Across_Swath");
          table.put("scale_name", "scale_factor");
          table.put("offset_name", "add_offset");
          table.put("fill_value_name", "_FillValue");
          table.put("range_name", "Optical_Depth_Land_And_Ocean");
          adapters[0] = new SwathAdapter(reader, table);
          categories = DataCategory.parseCategories("2D grid;GRID-2D;");
          defaultSubset = adapters[0].getDefaultSubset();
          defaultSubsets[0] = defaultSubset;
          hasImagePreview = true;
        }
        else if (name.startsWith("MOD06") || name.startsWith("MYD06")) {
          hasImagePreview = true;
          String path = "mod06/Data_Fields/";
          String[] arrayNames = new String[] {"Cloud_Optical_Thickness", "Cloud_Effective_Radius", "Cloud_Water_Path"};
          String[] arrayNames_5km = new String[] {"Cloud_Top_Pressure", "Cloud_Top_Temperature", "Cloud_Fraction"};
  
          adapters = new MultiDimensionAdapter[arrayNames.length+arrayNames_5km.length];
          defaultSubsets = new HashMap[arrayNames.length+arrayNames_5km.length];
          categoriesArray = new List[adapters.length];

          
          for (int k=0; k<arrayNames.length; k++) {
            Map<String, Object> table = SwathAdapter.getEmptyMetadataTable();
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
            categoriesArray[k] = DataCategory.parseCategories("1km swath;GRID-2D;");
          }

          for (int k=0; k<arrayNames_5km.length; k++) {
            Map<String, Object> table = SwathAdapter.getEmptyMetadataTable();
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
            table.put("range_name", arrayNames_5km[k]);

            SwathAdapter swathAdapter = new SwathAdapter(reader, table);
            defaultSubset = swathAdapter.getDefaultSubset();
            adapters[arrayNames.length+k] = swathAdapter;
            defaultSubsets[arrayNames.length+k] = defaultSubset;
            categoriesArray[arrayNames.length+k] = DataCategory.parseCategories("5km swath;GRID-2D;");
          }
       }
       else if (name.startsWith("a1") && name.contains("mod06")) {
          hasImagePreview = true;
          String[] arrayNames = new String[] {"Cloud_Optical_Thickness", "Cloud_Effective_Radius", "Cloud_Water_Path"};
          String[] arrayNames_5km = new String[] {"Cloud_Top_Pressure", "Cloud_Top_Temperature", "Cloud_Fraction"};

          adapters = new MultiDimensionAdapter[arrayNames.length+arrayNames_5km.length];
          defaultSubsets = new HashMap[arrayNames.length+arrayNames_5km.length];
          categoriesArray = new List[adapters.length];


          for (int k=0; k<arrayNames.length; k++) {
            Map<String, Object> table = SwathAdapter.getEmptyMetadataTable();
            table.put("array_name", arrayNames[k]);
            table.put("lon_array_name", "Longitude");
            table.put("lat_array_name", "Latitude");
            table.put("array_dimension_names", new String[] {"Cell_Along_Swath_1km", "Cell_Across_Swath_1km"});
            table.put("lon_array_dimension_names", new String[] {"Cell_Along_Swath_1km", "Cell_Across_Swath_1km"});
            table.put("lat_array_dimension_names", new String[] {"Cell_Along_Swath_1km", "Cell_Across_Swath_1km"});
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
            categoriesArray[k] = DataCategory.parseCategories("1km swath;GRID-2D;");
          }

          for (int k=0; k<arrayNames_5km.length; k++) {
            Map<String, Object> table = SwathAdapter.getEmptyMetadataTable();
            table.put("array_name", arrayNames_5km[k]);
            table.put("lon_array_name", "Longitude");
            table.put("lat_array_name", "Latitude");
            table.put("array_dimension_names", new String[] {"Cell_Along_Swath_5km", "Cell_Across_Swath_5km"});
            table.put("lon_array_dimension_names", new String[] {"Cell_Along_Swath_5km", "Cell_Across_Swath_5km"});
            table.put("lat_array_dimension_names", new String[] {"Cell_Along_Swath_5km", "Cell_Across_Swath_5km"});
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
            adapters[arrayNames.length+k] = swathAdapter;
            defaultSubsets[arrayNames.length+k] = defaultSubset;
            categoriesArray[arrayNames.length+k] = DataCategory.parseCategories("5km swath;GRID-2D;");
          }
       }
       else if (name.contains("HSRL2_B200") && name.endsWith(".h5")) {
         Map<String, Object> table;
         adapters = new MultiDimensionAdapter[5];
         defaultSubsets = new HashMap[5];
         propsArray = new Hashtable[5];
         
         String dataPath = "DataProducts/";
         String[] arrayNames = new String[] {"532_total_attn_bsc", "1064_total_attn_bsc", "355_total_attn_bsc"};
         String[] rangeNames = new String[] {"Total_Attenuated_Backscatter_532", "Total_Attenuated_Backscatter_1064", "Total_Attenuated_Backscatter_355"};
         
         String[] arrayNameAOT = new String[] {"532_AOT_hi_col", "355_AOT_hi_col"};
         String[] rangeNamesAOT = new String[] {};
         

         for (int k=0; k<arrayNames.length; k++) {
            table = ProfileAlongTrack.getEmptyMetadataTable();
            table.put(ProfileAlongTrack.array_name, dataPath+arrayNames[k]);
            table.put(ProfileAlongTrack.range_name, rangeNames[k]);
            table.put(ProfileAlongTrack.trackDim_name, "dim0");
            table.put(ProfileAlongTrack.vertDim_name, "dim1");
            table.put(ProfileAlongTrack.profileTime_name, "ApplanixIMU/gps_time");
            table.put(ProfileAlongTrack.longitude_name, "ApplanixIMU/gps_lon");
            table.put(ProfileAlongTrack.latitude_name, "ApplanixIMU/gps_lat");
            table.put("array_dimension_names", new String[] {"dim0", "dim1"});
            ProfileAlongTrack adapter = new HSRL2D(reader, table);
            ProfileAlongTrack3D adapter3D = new ProfileAlongTrack3D(adapter);
            Map<String, double[]> subset = adapter.getDefaultSubset();
            adapters[k] = adapter3D;
            defaultSubset = subset;
            defaultSubsets[k] = defaultSubset;

            properties.put("medianFilter", new String[] {Double.toString(12), Double.toString(24)});
            properties.put("setBelowSfcMissing", new String[] {"true"});
            propsArray[k] = properties;
         }
         
         DataCategory.createCategory("ProfileAlongTrack");
         categories = DataCategory.parseCategories("ProfileAlongTrack;ProfileAlongTrack;");

         hasTrackPreview = true;

         ArrayAdapter[] adapter_s = new ArrayAdapter[3];
         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "ApplanixIMU/gps_lat");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         table.put("array_dimension_names", new String[] {"dim0", "dim1"});
         adapter_s[0] = new ArrayAdapter(reader, table);

         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "UserInput/DEM_altitude");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         table.put("array_dimension_names", new String[] {"dim0", "dim1"});
         adapter_s[1] = new ArrayAdapter(reader, table);
         /*
         adapter_s[1].setRangeProcessor(new RangeProcessor() { // Eventually handle unit conversions better.
              public float[] processRange(float[] fvals, Map<String, double[]> subset) {
                 for (int i=0; i<fvals.length; i++) {
                    fvals[i] *= 1000; //km -> m
                 }
                 return fvals;
              }
         });
         */

         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "ApplanixIMU/gps_lon");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         table.put("array_dimension_names", new String[] {"dim0", "dim1"});
         adapter_s[2] = new ArrayAdapter(reader, table);

         TrackDomain track_domain = new TrackDomain(adapter_s[2], adapter_s[0], adapter_s[1]);
         track_adapter = new TrackAdapter(track_domain, adapter_s[1]);
         
         TrackAdapter trkAdapter = new TrackAdapter(new TrackDomain(adapter_s[2], adapter_s[0], adapter_s[1]), adapter_s[1]);
         trkAdapter.setName("Track3D");
         
         trkAdapter = new TrackAdapter(new TrackDomain(adapter_s[2], adapter_s[0]), adapter_s[1]);
         trkAdapter.setName("Track2D");
       }
       else if (name.startsWith("CAL_LID_L1")) {

    	   // Make sure the variables we need are present. If not, this is not a valid
    	   // L1 CALIPSO file McV can work with.
    	   
    	   if (! ((hasVariable("Latitude")) &&
    			  (hasVariable("Longitude")) &&
    			  (hasVariable("Surface_Elevation")) &&
    			  (hasVariable("Tropopause_Height")) &&
    			  (hasVariable("Total_Attenuated_Backscatter_532"))) 
    		  ) {
    		   // Pop up a dialog letting user know we can't work wit this data
	    		String msg = "McIDAS-V is unable to read this Level 1 CALIPSO file.\n" +
	    				"If you believe this is a valid file which should be supported,\n" +
	    				"please contact the MUG or post a message on the MUG Forum.";
	    		Object[] params = { msg };
	    		JOptionPane.showMessageDialog(null, params, "Data Validity Test Failure", JOptionPane.OK_OPTION);
	    		throw new Exception("Unable to load CALIPSO data");
    	   }
    	   
         adapters = new MultiDimensionAdapter[4];
         defaultSubsets = new HashMap[4];
         propsArray = new Hashtable[4]; 
         
         Map<String, Object> table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Total_Attenuated_Backscatter_532");
         table.put(ProfileAlongTrack.ancillary_file_name, "/edu/wisc/ssec/mcidasv/data/hydra/resources/calipso/altitude");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         table.put(ProfileAlongTrack.profileTime_name, "Profile_Time");
         table.put(ProfileAlongTrack.longitude_name, "Longitude");
         table.put(ProfileAlongTrack.latitude_name, "Latitude");
         table.put("array_dimension_names", new String[] {"dim0", "dim1"}); 
         ProfileAlongTrack adapter = new Calipso2D(reader, table);
         ProfileAlongTrack3D adapter3D = new ProfileAlongTrack3D(adapter);
         Map<String, double[]> subset = adapter.getDefaultSubset();
         adapters[0] = adapter3D;
         defaultSubset = subset;
         defaultSubsets[0] = defaultSubset;
         DataCategory.createCategory("ProfileAlongTrack");
         categories = DataCategory.parseCategories("ProfileAlongTrack;ProfileAlongTrack;");

         properties.put("medianFilter", new String[] {Double.toString(12), Double.toString(32)});
         properties.put("setBelowSfcMissing", new String[] {"true"});
         propsArray[0] = properties;

         ArrayAdapter[] adapter_s = new ArrayAdapter[3];
         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Latitude");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         table.put("array_dimension_names", new String[] {"dim0", "dim1"}); 
         adapter_s[0] = new ArrayAdapter(reader, table);

         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Surface_Elevation");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         table.put("array_dimension_names", new String[] {"dim0", "dim1"}); 
         adapter_s[1] = new ArrayAdapter(reader, table);
         adapter_s[1].setRangeProcessor(new RangeProcessor() { // Eventually handle unit conversions better.
              public float[] processRange(float[] fvals, Map<String, double[]> subset) {
                 for (int i=0; i<fvals.length; i++) {
                    fvals[i] *= 1000; //km -> m 
                 }
                 return fvals;
              }
         });

         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Longitude");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         table.put("array_dimension_names", new String[] {"dim0", "dim1"}); 
         adapter_s[2] = new ArrayAdapter(reader, table);

         TrackDomain track_domain = new TrackDomain(adapter_s[2], adapter_s[0], adapter_s[1]);
         track_adapter = new TrackAdapter(track_domain, adapter_s[1]);

         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Tropopause_Height");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         table.put("array_dimension_names", new String[] {"dim0", "dim1"}); 
         ArrayAdapter trop_height = new ArrayAdapter(reader, table);
         track_domain = new TrackDomain(adapter_s[2], adapter_s[0], trop_height);
         adapters[1] = new TrackAdapter(track_domain, trop_height);
         defaultSubsets[1] = adapters[1].getDefaultSubset();

         adapters[2] = new TrackAdapter(new TrackDomain(adapter_s[2], adapter_s[0], adapter_s[1]), adapter_s[1]);
         ((TrackAdapter)adapters[2]).setName("Track3D");
         defaultSubsets[2] = adapters[2].getDefaultSubset();

         adapters[3] = new TrackAdapter(new TrackDomain(adapter_s[2], adapter_s[0]), adapter_s[1]);
         ((TrackAdapter)adapters[3]).setName("Track2D");
         defaultSubsets[3] = adapters[3].getDefaultSubset();
         

         hasTrackPreview = true;
       }
       else if (name.startsWith("CAL_LID_L2")) {
    	   
    	   // Make sure the variables we need are present. If not, this is not a valid
    	   // L2 CALIPSO file McV can work with.
    	   
    	   if (! ((hasVariable("Latitude")) &&
    			  (hasVariable("Longitude")) &&
    			  (hasVariable("DEM_Surface_Elevation")) &&
    			  (hasVariable("Layer_Top_Altitude"))) 
    		  ) {
    		   // Pop up a dialog letting user know we can't work wit this data
	    		String msg = "McIDAS-V is unable to read this Level 2 CALIPSO file.\n" +
	    				"If you believe this is a valid file which should be supported,\n" +
	    				"please contact the MUG or post a message on the MUG Forum.";
	    		Object[] params = { msg };
	    		JOptionPane.showMessageDialog(null, params, "Data Validity Test Failure", JOptionPane.OK_OPTION);
	    		throw new Exception("Unable to load CALIPSO data");
    	   }
    	   
         adapters = new MultiDimensionAdapter[4];
         defaultSubsets = new HashMap[4];
         propsArray = new Hashtable[4];

         ArrayAdapter[] adapter_s = new ArrayAdapter[3];

         adapter_s[0] = createTrackVertArrayAdapter("Longitude");
         adapter_s[1] = createTrackVertArrayAdapter("Latitude");
         adapter_s[2] = createTrackVertArrayAdapter("DEM_Surface_Elevation");

         TrackDomain track_domain = new TrackDomain(adapter_s[0], adapter_s[1], adapter_s[2]);
         track_adapter = new TrackAdapter(track_domain, adapter_s[2]);
         adapters[1] = track_adapter;
         defaultSubsets[1] = track_adapter.getDefaultSubset();

         ArrayAdapter layer_top_altitude = createTrackVertArrayAdapter("Layer_Top_Altitude");

         RangeProcessor rngProcessor =
             new RangeProcessor(1.0f, 0.0f, -Float.MAX_VALUE, Float.MAX_VALUE, -9999.0f);
         layer_top_altitude.setRangeProcessor(rngProcessor);

         track_domain = new TrackDomain(adapter_s[0], adapter_s[1], layer_top_altitude);
         adapters[0] = new TrackAdapter(track_domain, layer_top_altitude);
         defaultSubsets[0] = adapters[0].getDefaultSubset();

         /** another layer, how to show all?
         adapters[2] = new TrackAdapter(track_domain, layer_top_altitude);
         ((TrackAdapter)adapters[2]).setListIndex(1);
         defaultSubsets[2] = adapters[2].getDefaultSubset();
         */

         adapters[2] = new TrackAdapter(new TrackDomain(adapter_s[0], adapter_s[1]), adapter_s[2]);
         ((TrackAdapter)adapters[2]).setName("Track2D");
         defaultSubsets[2] = adapters[2].getDefaultSubset();

         adapters[3] = new TrackAdapter(new TrackDomain(adapter_s[0], adapter_s[1], adapter_s[2]), adapter_s[2]);
         ((TrackAdapter)adapters[3]).setName("Track3D");
         defaultSubsets[3] = adapters[3].getDefaultSubset();

         DataCategory.createCategory("ProfileAlongTrack");
         categories = DataCategory.parseCategories("ProfileAlongTrack;ProfileAlongTrack;");

         hasTrackPreview = true;
       }
       else if (name.indexOf("2B-GEOPROF") > 0) {
         adapters = new MultiDimensionAdapter[4];
         defaultSubsets = new HashMap[4];
         propsArray = new Hashtable[4];

         Map<String, Object> table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "2B-GEOPROF/Data_Fields/Radar_Reflectivity");
         table.put(ProfileAlongTrack.range_name, "2B-GEOPROF_RadarReflectivity");
         table.put(ProfileAlongTrack.scale_name, "factor");
         table.put(ProfileAlongTrack.offset_name, "offset");
         table.put(ProfileAlongTrack.fill_value_name, "_FillValue");
         table.put(ProfileAlongTrack.valid_range, "valid_range");
         table.put(ProfileAlongTrack.ancillary_file_name, "/edu/wisc/ssec/mcidasv/data/hydra/resources/cloudsat/altitude");
         table.put(ProfileAlongTrack.trackDim_name, "nray");
         table.put(ProfileAlongTrack.vertDim_name, "nbin");
         table.put(ProfileAlongTrack.profileTime_name, "2B-GEOPROF/Geolocation_Fields/Profile_Time");
         table.put(ProfileAlongTrack.longitude_name, "2B-GEOPROF/Geolocation_Fields/Longitude");
         table.put(ProfileAlongTrack.latitude_name, "2B-GEOPROF/Geolocation_Fields/Latitude");
         table.put(ProfileAlongTrack.product_name, "2B-GEOPROF");
         ProfileAlongTrack adapter = new CloudSat2D(reader, table);
         ProfileAlongTrack3D adapter3D = new ProfileAlongTrack3D(adapter);
         Map<String, double[]> subset = adapter.getDefaultSubset();
         adapters[0] = adapter3D;
         defaultSubset = subset;
         defaultSubsets[0] = defaultSubset;
         DataCategory.createCategory("ProfileAlongTrack");
         categories = DataCategory.parseCategories("ProfileAlongTrack;ProfileAlongTrack;");

         ArrayAdapter[] adapter_s = new ArrayAdapter[3];
         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "2B-GEOPROF/Geolocation_Fields/Latitude");
         table.put(ProfileAlongTrack.range_name, "Latitude");
         table.put(ProfileAlongTrack.trackDim_name, "nray");
         adapter_s[0] = new ArrayAdapter(reader, table);

         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "2B-GEOPROF/Geolocation_Fields/DEM_elevation");
         table.put(ProfileAlongTrack.range_name, "DEM_elevation");
         table.put(ProfileAlongTrack.trackDim_name, "nray");
         adapter_s[1] = new ArrayAdapter(reader, table);
         adapter_s[1].setRangeProcessor(new RangeProcessor() { // need this because we don't want -9999 mapped to NaN
              public float[] processRange(short[] svals, Map<String, double[]> subset) {
                  float[] fvals = new float[svals.length];
                  for (int i=0; i<svals.length; i++) {
                     short sval = svals[i];
                     if (sval == -9999) {
                        fvals[i] = 0f;
                     }
                     else {
                       fvals[i] = sval;
                     }
                  }
                  return fvals;
               }
         });

         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "2B-GEOPROF/Geolocation_Fields/Longitude");
         table.put(ProfileAlongTrack.range_name, "Longitude");
         table.put(ProfileAlongTrack.trackDim_name, "nray");
         adapter_s[2] = new ArrayAdapter(reader, table);

         TrackDomain track_domain = new TrackDomain(adapter_s[2], adapter_s[0], adapter_s[1]);
         track_adapter = new TrackAdapter(track_domain, adapter_s[1]);

         adapters[2] = new TrackAdapter(new TrackDomain(adapter_s[2], adapter_s[0], adapter_s[1]), adapter_s[1]);
         ((TrackAdapter)adapters[2]).setName("Track3D");
         defaultSubsets[2] = adapters[2].getDefaultSubset();

         adapters[1] = new TrackAdapter(new TrackDomain(adapter_s[2], adapter_s[0]), adapter_s[1]);
         ((TrackAdapter)adapters[1]).setName("Track2D");
         defaultSubsets[1] = adapters[1].getDefaultSubset();


         properties.put("medianFilter", new String[] {Double.toString(6), Double.toString(14)});
         properties.put("setBelowSfcMissing", new String[] {"true"});
         propsArray[0] = properties;
         hasTrackPreview = true;
       }
       else if ( name.startsWith("MHSx_xxx_1B") && name.endsWith("h5")) {
          Map<String, Object> table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "U-MARF/EPS/MHSx_xxx_1B/DATA/Channel1");
          table.put("lon_array_name", "U-MARF/EPS/IASI_xxx_1C/DATA/IMAGE_LON_ARRAY");
          table.put("lat_array_name", "U-MARF/EPS/IASI_xxx_1C/DATA/IMAGE_LAT_ARRAY");
          table.put("XTrack", "dim1");
          table.put("Track", "dim0");
          table.put("geo_XTrack", "dim1");
          table.put("geo_Track", "dim0");
          table.put("product_name", "MHSx_xxx_1B");
          SwathAdapter swathAdapter = new SwathAdapter(reader, table);
          adapters[0] = swathAdapter;
          Map<String, double[]> subset = swathAdapter.getDefaultSubset();
          defaultSubset = subset;
          defaultSubsets[0] = defaultSubset;
          categories = DataCategory.parseCategories("2D grid;GRID-2D;");
       }
       else if ( name.startsWith("MYD02SSH") ) {
         String[] arrayNames = null;

         if (name.endsWith("level2.hdf")) {
           arrayNames = new String[] {"cld_press_acha", "cld_temp_acha", "cld_height_acha", "cloud_type",
                                             "cloud_albedo_0_65um_nom", "cloud_transmission_0_65um_nom", "cloud_fraction"};
         }
         else if (name.endsWith("obs.hdf")) {
           arrayNames = new String[] {"refl_0_65um_nom", "refl_0_86um_nom", "refl_3_75um_nom", "refl_1_60um_nom", "refl_1_38um_nom",
                                      "temp_3_75um_nom", "temp_11_0um_nom", "temp_12_0um_nom", "temp_6_7um_nom",
                                      "temp_8_5um_nom", "temp_13_3um_nom"};
         }
  
         adapters = new MultiDimensionAdapter[arrayNames.length];
         defaultSubsets = new HashMap[arrayNames.length];
         propsArray = new Hashtable[arrayNames.length]; 

         for (int k=0; k<arrayNames.length; k++) {
           Map<String, Object> swthTable = SwathAdapter.getEmptyMetadataTable();
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
           Map<String, double[]> subset = swathAdapter0.getDefaultSubset();
           defaultSubset = subset;
           adapters[k] = swathAdapter0;
           defaultSubsets[k] = defaultSubset;
         }
         categories = DataCategory.parseCategories("2D grid;GRID-2D;");
         hasImagePreview = true;
       }
       else if (name.contains("AWG_OZONE") ) {
         String[] arrayNames = new String[] {"ColumnOzone"};

         adapters = new MultiDimensionAdapter[arrayNames.length];
         defaultSubsets = new HashMap[arrayNames.length];
         propsArray = new Hashtable[arrayNames.length];
         
         for (int k=0; k<arrayNames.length; k++) {
           Map<String, Object> swthTable = SwathAdapter.getEmptyMetadataTable();
           swthTable.put("array_name", arrayNames[k]);
           swthTable.put("lon_array_name", "Longitude");
           swthTable.put("lat_array_name", "Latitude");
           swthTable.put("XTrack", "Columns");
           swthTable.put("Track", "Rows");
           swthTable.put("fill_value_name", "_FillValue");
           swthTable.put("geo_Track", "Rows");
           swthTable.put("geo_XTrack", "Columns");
           swthTable.put("geo_fillValue_name", "_FillValue");
           swthTable.put("range_name", arrayNames[k]);

           SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
           swathAdapter0.setDefaultStride(5);
           Map<String, double[]> subset = swathAdapter0.getDefaultSubset();
           defaultSubset = subset;
           adapters[k] = swathAdapter0;
           defaultSubsets[k] = defaultSubset;
         }

         categories = DataCategory.parseCategories("2D grid;GRID-2D;");
         hasImagePreview = true;
       }
       else if (name.contains("AWG_CLOUD_MASK") ) {
         String[] arrayNames = new String[] {"CloudMask"};

         adapters = new MultiDimensionAdapter[arrayNames.length];
         defaultSubsets = new HashMap[arrayNames.length];
         propsArray = new Hashtable[arrayNames.length];

         for (int k=0; k<arrayNames.length; k++) {
           Map<String, Object> swthTable = SwathAdapter.getEmptyMetadataTable();
           swthTable.put("array_name", arrayNames[k]);
           swthTable.put("lon_array_name", "Longitude");
           swthTable.put("lat_array_name", "Latitude");
           swthTable.put("XTrack", "Columns");
           swthTable.put("Track", "Rows");
           swthTable.put("fill_value_name", "_FillValue");
           swthTable.put("geo_Track", "Rows");
           swthTable.put("geo_XTrack", "Columns");
           swthTable.put("geo_fillValue_name", "_FillValue");
           swthTable.put("range_name", arrayNames[k]);

           SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
           swathAdapter0.setDefaultStride(5);
           Map<String, double[]> subset = swathAdapter0.getDefaultSubset();
           defaultSubset = subset;
           adapters[k] = swathAdapter0;
           defaultSubsets[k] = defaultSubset;
         }

         categories = DataCategory.parseCategories("2D grid;GRID-2D;");
         hasImagePreview = true;
       }
       else if (name.contains("AWG_CLOUD_HEIGHT")) {
         String[] arrayNames = new String[] {"CldTopTemp", "CldTopPres", "CldTopHght"};

         adapters = new MultiDimensionAdapter[arrayNames.length];
         defaultSubsets = new HashMap[arrayNames.length];
         propsArray = new Hashtable[arrayNames.length];

         for (int k=0; k<arrayNames.length; k++) {
           Map<String, Object> swthTable = SwathAdapter.getEmptyMetadataTable();
           swthTable.put("array_name", arrayNames[k]);
           swthTable.put("lon_array_name", "Longitude");
           swthTable.put("lat_array_name", "Latitude");
           swthTable.put("XTrack", "Columns");
           swthTable.put("Track", "Rows");
           swthTable.put("scale_name", "scale_factor");
           swthTable.put("offset_name", "add_offset");
           swthTable.put("fill_value_name", "_FillValue");
           swthTable.put("geo_Track", "Rows");
           swthTable.put("geo_XTrack", "Columns");
           swthTable.put("geo_scale_name", "scale_factor");
           swthTable.put("geo_offset_name", "add_offset");
           swthTable.put("geo_fillValue_name", "_FillValue");
           swthTable.put("range_name", arrayNames[k]);
           swthTable.put("unpack", "unpack");

           SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
           swathAdapter0.setDefaultStride(5);
           Map<String, double[]> subset = swathAdapter0.getDefaultSubset();
           defaultSubset = subset;
           adapters[k] = swathAdapter0;
           defaultSubsets[k] = defaultSubset;
         }
         categories = DataCategory.parseCategories("2D grid;GRID-2D;");
         hasImagePreview = true;
       }
       else if (name.startsWith("geocatL2") && name.endsWith("ci.hdf")) {
         String[] arrayNames = new String[] {"box_average_11um_ctc", "box_average_11um_ctc_scaled", "conv_init", "cloud_type"};

         adapters = new MultiDimensionAdapter[arrayNames.length];
         defaultSubsets = new HashMap[arrayNames.length];
         propsArray = new Hashtable[arrayNames.length];

         for (int k=0; k<arrayNames.length; k++) {
           Map<String, Object> swthTable = SwathAdapter.getEmptyMetadataTable();
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
           Map<String, double[]> subset = swathAdapter0.getDefaultSubset();
           defaultSubset = subset;
           adapters[k] = swathAdapter0;
           defaultSubsets[k] = defaultSubset;
         }
         categories = DataCategory.parseCategories("2D grid;GRID-2D;");
         hasImagePreview = true;
       } 
       else {
         String[] arrayNames = new String[] {"baseline_cmask_seviri_cloud_mask", "baseline_ctype_seviri_cloud_type",
                                             "baseline_ctype_seviri_cloud_phase", "baseline_cld_hght_seviri_cloud_top_pressure",
                                             "baseline_cld_hght_seviri_cloud_top_height"};

         adapters = new MultiDimensionAdapter[arrayNames.length];
         defaultSubsets = new HashMap[arrayNames.length];
         propsArray = new Hashtable[arrayNames.length]; 

         for (int k=0; k<arrayNames.length; k++) {
           Map<String, Object> swthTable = SwathAdapter.getEmptyMetadataTable();
           swthTable.put("array_name", arrayNames[k]);
           swthTable.put("lon_array_name", "pixel_longitude");
           swthTable.put("lat_array_name", "pixel_latitude");
           swthTable.put("XTrack", "elements");
           swthTable.put("Track", "lines");
           swthTable.put("scale_name", "scale_factor");
           swthTable.put("offset_name", "add_offset");
           swthTable.put("fill_value_name", "_FillValue");
           swthTable.put("geo_Track", "lines");
           swthTable.put("geo_XTrack", "elements");
           swthTable.put("geo_scale_name", "scale_factor");
           swthTable.put("geo_offset_name", "add_offset");
           swthTable.put("geo_fillValue_name", "_FillValue");
           swthTable.put("range_name", arrayNames[k]);
           swthTable.put("unpack", "unpack");

           SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
           swathAdapter0.setDefaultStride(2);
           Map<String, double[]> subset = swathAdapter0.getDefaultSubset();
           defaultSubset = subset;
           adapters[k] = swathAdapter0;
           defaultSubsets[k] = defaultSubset;
         }
         categories = DataCategory.parseCategories("2D grid;GRID-2D;");
         hasImagePreview = true;
       }

       setProperties(properties);
    }

    public void initAfterUnpersistence() {
      try {
        setup();
      } catch (Exception e) {
        logger.error("could not set up after unpersisting", e);
      }
    }

    /**
     * Make and insert the {@link DataChoice DataChoices} for this {@code DataSource}.
     */
    public void doMakeDataChoices() {
        DataChoice choice = null;
        if (adapters != null) {
          for (int idx=0; idx<adapters.length; idx++) {
             try {
               String arrayName = (adapters[idx] == null) ? "_     " : adapters[idx].getArrayName();
               choice = doMakeDataChoice(idx, arrayName);
               adapterMap.put(choice.getName(), adapters[idx]);
             } catch (Exception e) {
               logger.error("error making data choices", e);
             }

             if (choice != null) {
               addDataChoice(choice);
             }
          }
        }
    }

    private DataChoice doMakeDataChoice(int idx, String var) throws Exception {
        String name = var;
        DataSelection dataSel = (defaultSubsets[idx] == null) ? new MultiDimensionSubset() : new MultiDimensionSubset(defaultSubsets[idx]);
        Hashtable props = new Hashtable();
        props.put(new MultiDimensionSubset(), dataSel);

        if (propsArray != null) {
          if (propsArray[idx] != null) {
            propsArray[idx].put(new MultiDimensionSubset(), dataSel);
            props = propsArray[idx];
          }
        }
        DirectDataChoice ddc = null;

        if (categories != null) {
           ddc = new DirectDataChoice(this, idx, name, name, categories, props);
        }
        else {
           ddc = new DirectDataChoice(this, idx, name, name, categoriesArray[idx], props);
        }

        return ddc;
    }

    /**
     * Check to see if this {@code HDFHydraDataSource} is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof MultiDimensionDataSource)) {
            return false;
        }
        return (this == (MultiDimensionDataSource) o);
    }

    public MultiSpectralData getMultiSpectralData() {
      return multiSpectData;
    }

    public String getDatasetName() {
      return filename;
    }

    public void setDatasetName(String name) {
      filename = name;
    }

    public Map<String, double[]> getSubsetFromLonLatRect(MultiDimensionSubset select, GeoSelection geoSelection) {
      GeoLocationInfo ginfo = geoSelection.getBoundingBox();
      logger.debug("ginfo0: " + ginfo);
      return adapters[0].getSubsetFromLonLatRect(select.getSubset(), ginfo.getMinLat(), ginfo.getMaxLat(),
                                        ginfo.getMinLon(), ginfo.getMaxLon());
    }

    public synchronized Data getData(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {
       return this.getDataInner(dataChoice, category, dataSelection, requestProperties);
    }


    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {

        MultiDimensionAdapter adapter = null;
        adapter = adapterMap.get(dataChoice.getName());

        Hashtable dataChoiceProps = dataChoice.getProperties();

        //- this hack keeps the HydraImageProbe from doing a getData()
        //- TODO: need to use categories?
        if (requestProperties != null) {
          if ((requestProperties.toString()).equals("{prop.requester=MultiSpectral}")) {
            return null;
          }
        }

        GeoLocationInfo ginfo = null;
        GeoSelection geoSelection = null;
        
        if ((dataSelection != null) && (dataSelection.getGeoSelection() != null)) {

          if (dataSelection.getGeoSelection().getBoundingBox() != null) {
            geoSelection = dataSelection.getGeoSelection();
          }
          else { // no bounding box in the incoming DataSelection. Check the dataChoice.
            DataSelection datSelFromChoice = dataChoice.getDataSelection();
            if (datSelFromChoice != null) {
              geoSelection = datSelFromChoice.getGeoSelection();
            }
          }
        }

        if (geoSelection != null) {
          ginfo = geoSelection.getBoundingBox();
        }

        // Still no geo info so check for the lon/lat b.b. in this datasource (last set by DataSelectionComponent)
        if (ginfo == null) {
           DataSelection localDataSelection = getDataSelection();
           if (localDataSelection != null) {
              geoSelection = localDataSelection.getGeoSelection();
              if (geoSelection != null) {
                 ginfo = geoSelection.getBoundingBox();
              }
           }
        }

        Data data = null;
        if (adapters == null) {
          return data;
        }

        Map<String, double[]> subset = null;
        MultiDimensionSubset select = null;

        Hashtable table = dataChoice.getProperties();
        Enumeration keys = table.keys();
        while (keys.hasMoreElements()) {
           Object key = keys.nextElement();
           if (key instanceof MultiDimensionSubset) {
              select = (MultiDimensionSubset) table.get(key);
           }
        }

        try {
            subset = null;
            if (ginfo != null) {
            	if (trackSelection != null) {
            		boolean trackStrideOk = trackSelection.setTrackStride();
            		boolean verticalStrideOk = trackSelection.setVerticalStride();
            		
            		if (trackStrideOk && verticalStrideOk) {
            		subset = adapter.getSubsetFromLonLatRect(ginfo.getMinLat(), ginfo.getMaxLat(),
            				ginfo.getMinLon(), ginfo.getMaxLon(),
            				trackSelection.trackStride,
            				trackSelection.verticalStride,
            				geoSelection.getZStride());
            		} else {
            			// one of the strides is not an integer, let user know
            		    String msg = "Either the Track or Vertical Stride is invalid.\n" +
            		                 "Stride values must be positive integers.\n";
        	    		Object[] params = { msg };
        	    		JOptionPane.showMessageDialog(null, params, "Invalid Stride", JOptionPane.OK_OPTION);
        	    		return null;
            		}
            	} else {
            		subset = adapter.getSubsetFromLonLatRect(ginfo.getMinLat(), ginfo.getMaxLat(),
            				ginfo.getMinLon(), ginfo.getMaxLon(),
            				geoSelection.getXStride(),
            				geoSelection.getYStride(),
            				geoSelection.getZStride());
            	}
              if (subset == null && select != null) {
                subset = select.getSubset();
              }
            }
            else { // no IDV incoming spatial selection info, so check for HYDRA specific via Properties
              if (select != null) {
                subset = select.getSubset();
              }
              
              if (dataSelection != null) {
                Hashtable props = dataSelection.getProperties();
              }
            }
            
            if (subset != null) {
              data = adapter.getData(subset);
              data = applyProperties(data, dataChoiceProps, subset);
            }
        } catch (Exception e) {
            logger.error("getData exception", e);
        }

        return data;
    }

    protected Data applyProperties(Data data, Hashtable requestProperties, Map<String, double[]> subset)
          throws VisADException, RemoteException, Exception {
      Data new_data = data;

      if (requestProperties == null) {
        new_data = data;
        return new_data;
      }

      if (requestProperties.containsKey("medianFilter")) {
        String[] items = (String[]) requestProperties.get("medianFilter");
        double window_lenx = Double.parseDouble(items[0]);
        double window_leny = Double.parseDouble(items[1]);
        GriddedSet domainSet = (GriddedSet) ((FlatField)data).getDomainSet();
        int[] lens = domainSet.getLengths();
        float[] range_values = (((FlatField)data).getFloats())[0];
        range_values =
           ProfileAlongTrack.medianFilter(range_values, lens[0], lens[1],
                               (int)window_lenx, (int)window_leny);
        ((FlatField)new_data).setSamples(new float[][] {range_values});
      }
      if (requestProperties.containsKey("setBelowSfcMissing")) {
        FlatField track = (FlatField) track_adapter.getData(subset);
        float[] sfcElev = (track.getFloats())[0];
        FlatField field = (FlatField) new_data;
        GriddedSet gset = (GriddedSet) field.getDomainSet();
        float[][] samples = gset.getSamples(false);
        int[] lens = gset.getLengths();
        float[] range_values = (field.getFloats())[0];

        int trkIdx = ((ProfileAlongTrack3D)adapters[0]).adapter2D.getTrackTupIdx();
        int vrtIdx = ((ProfileAlongTrack3D)adapters[0]).adapter2D.getVertTupIdx();

        int k = 0;
        for (int j=0; j<lens[trkIdx]; j++) {
          float val = sfcElev[j];
          for (int i=0; i<lens[vrtIdx]; i++) {
            if (vrtIdx < trkIdx) k = i + j*lens[0];
            if (trkIdx < vrtIdx) k = j + i*lens[0];
            if (samples[2][k] <= val) {
              range_values[k] = Float.NaN;
            }
          }
        }
        field.setSamples(new float[][] {range_values});
      }
      return new_data;
    }

    protected void initDataSelectionComponents(
         List<DataSelectionComponent> components,
             final DataChoice dataChoice) {

      if (hasImagePreview) {
        try {
          FlatField image = (FlatField) getDataInner(dataChoice, null, null, null);
          components.add(new PreviewSelection(dataChoice, image, null));
        } catch (Exception e) {
          logger.error("cannot make preview selection", e);
        }
      }
      if (hasTrackPreview) {
        try {
          FlatField track = track_adapter.getData(track_adapter.getDefaultSubset());     
          Map defaultSubset = (adapterMap.get(dataChoice.getName())).getDefaultSubset();
          trackSelection = new TrackSelection(dataChoice, track, defaultSubset);
          components.add(trackSelection);
        } catch (Exception e) {
          logger.error("cannot make preview selection", e);
        }
      }
    }

    private String getTrackDimensionName(String variableName) {
        return getVariableDimensionName(variableName, 0);
    }

    private String getVerticalDimensionName(String variableName) {
        return getVariableDimensionName(variableName, 1);
    }

    private String getVariableDimensionName(String variableName, int dimension) {
        NetcdfFile ncfile = ((NetCDFFile)reader).getNetCDFFile();
        Variable v = ncfile.findVariable(variableName);
        String name = null;
        if (v != null) {
            name = v.getDimension(dimension).getFullName();
        }
        return name;
    }

    private boolean hasVariable(String variableName) {
        NetcdfFile ncfile = ((NetCDFFile) reader).getNetCDFFile();
        return ncfile.findVariable(variableName) != null;
    }

    private ArrayAdapter createTrackVertArrayAdapter(String variableName) {
        Map<String, Object> table = SwathAdapter.getEmptyMetadataTable();

        String trackDimName = getTrackDimensionName(variableName);
        String vertDimName = getVerticalDimensionName(variableName);

        table.put(ProfileAlongTrack.array_name, variableName);
        table.put(ProfileAlongTrack.trackDim_name, trackDimName);
        table.put(ProfileAlongTrack.vertDim_name, vertDimName);
        table.put("array_dimension_names", new String[] { trackDimName, vertDimName });

        return new ArrayAdapter(reader, table);
    }
}
