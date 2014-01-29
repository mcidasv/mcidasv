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

package edu.wisc.ssec.mcidasv.data.hydra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import visad.CellImpl;
import visad.Data;
import visad.FlatField;
import visad.Gridded2DSet;
import visad.GriddedSet;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.UnionSet;
import visad.VisADException;
import visad.data.mcidas.BaseMapAdapter;
import visad.georef.MapProjection;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Misc;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;
import ucar.visad.ProjectionCoordinateSystem;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.MapLines;
import ucar.visad.display.RubberBandBox;

import edu.wisc.ssec.mcidasv.data.HydraDataSource;
import edu.wisc.ssec.mcidasv.data.PreviewSelection;

/**
 * A data source for Multi Dimension Data 
 */

public class MultiDimensionDataSource extends HydraDataSource {

    /** Sources file */
    protected String filename;

    protected MultiDimensionReader reader;

    protected MultiDimensionAdapter[] adapters = null;
    protected HashMap[] defaultSubsets = null;
    private HashMap<String, MultiDimensionAdapter> adapterMap = new HashMap<String, MultiDimensionAdapter>();
    protected Hashtable[] propsArray = null;
    protected List[] categoriesArray = null;


    protected SpectrumAdapter spectrumAdapter;

    private static final String DATA_DESCRIPTION = "Multi Dimension Data";

    private HashMap defaultSubset;
    public TrackAdapter track_adapter;
    private MultiSpectralData multiSpectData;

    private List categories;
    private boolean hasImagePreview = false;
    private boolean hasTrackPreview = false;

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    public MultiDimensionDataSource() {}

    /**
     * Construct a new HYDRA hdf data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
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
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  sources   List of filenames
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public MultiDimensionDataSource(DataSourceDescriptor descriptor,
                                 List newSources, Hashtable properties)
            throws VisADException {
        super(descriptor, newSources, DATA_DESCRIPTION, properties);

        this.filename = (String)sources.get(0);

        try {
          setup();
        }
        catch (Exception e) {
          e.printStackTrace();
          throw new VisADException();
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
        }
        catch (Exception e) {
          e.printStackTrace();
          System.out.println("cannot create NetCDF reader for file: "+filename);
        }

        adapters = new MultiDimensionAdapter[2];
        defaultSubsets = new HashMap[2]; 
        Hashtable<String, String[]> properties = new Hashtable<String, String[]>(); 
        
        String name = (new File(filename)).getName();

        if ( name.startsWith("MOD04") || name.startsWith("MYD04")) {
          HashMap table = SwathAdapter.getEmptyMetadataTable();
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
        }
        else if ( name.startsWith("MOD06") || name.startsWith("MYD06")) {
          hasImagePreview = true;
          String path = "mod06/Data_Fields/";
          String[] arrayNames = new String[] {"Cloud_Optical_Thickness", "Cloud_Effective_Radius", "Cloud_Water_Path"};
          String[] arrayNames_5km = new String[] {"Cloud_Top_Pressure", "Cloud_Top_Temperature", "Cloud_Fraction"};
  
          adapters = new MultiDimensionAdapter[arrayNames.length+arrayNames_5km.length];
          defaultSubsets = new HashMap[arrayNames.length+arrayNames_5km.length];
          categoriesArray = new List[adapters.length];

          
          for (int k=0; k<arrayNames.length; k++) {
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
            HashMap table = SwathAdapter.getEmptyMetadataTable();
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
            HashMap table = SwathAdapter.getEmptyMetadataTable();
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
       else if (name.startsWith("CAL_LID_L1")) {
         String[] arrayNames = null;
         adapters = new MultiDimensionAdapter[4];
         defaultSubsets = new HashMap[4];
         propsArray = new Hashtable[4]; 
         
         
         HashMap table = ProfileAlongTrack.getEmptyMetadataTable();
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
         HashMap subset = adapter.getDefaultSubset();
         adapters[0] = adapter3D;
         defaultSubset = subset;
         defaultSubsets[0] = defaultSubset;
         DataCategory.createCategory("ProfileAlongTrack");
         categories = DataCategory.parseCategories("ProfileAlongTrack;ProfileAlongTrack;");

         properties.put("medianFilter", new String[] {Double.toString(8), Double.toString(16)});
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
         adapters = new MultiDimensionAdapter[4];
         defaultSubsets = new HashMap[4];
         propsArray = new Hashtable[4];

         ArrayAdapter[] adapter_s = new ArrayAdapter[3];

         HashMap table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Longitude");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         adapter_s[0] = new ArrayAdapter(reader, table);

         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Latitude");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         adapter_s[1] = new ArrayAdapter(reader, table);

         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "DEM_Surface_Elevation");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         adapter_s[2] = new ArrayAdapter(reader, table);

         TrackDomain track_domain = new TrackDomain(adapter_s[0], adapter_s[1], adapter_s[2]);
         track_adapter = new TrackAdapter(track_domain, adapter_s[2]);
         adapters[1] = track_adapter;
         defaultSubsets[1] = track_adapter.getDefaultSubset();

         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Layer_Top_Altitude");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         ArrayAdapter layer_top_altitude = new ArrayAdapter(reader, table);
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
         adapters = new MultiDimensionAdapter[2];
         defaultSubsets = new HashMap[2];
         propsArray = new Hashtable[2];

         HashMap table = ProfileAlongTrack.getEmptyMetadataTable();
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
         HashMap subset = adapter.getDefaultSubset();
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

         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "2B-GEOPROF/Geolocation_Fields/Longitude");
         table.put(ProfileAlongTrack.range_name, "Longitude");
         table.put(ProfileAlongTrack.trackDim_name, "nray");
         adapter_s[2] = new ArrayAdapter(reader, table);

         TrackDomain track_domain = new TrackDomain(adapter_s[2], adapter_s[0], adapter_s[1]);
         track_adapter = new TrackAdapter(track_domain, adapter_s[1]);

         /*
         adapters[2] = new TrackAdapter(new TrackDomain(adapter_s[2], adapter_s[0], adapter_s[1]), adapter_s[1]);
         ((TrackAdapter)adapters[2]).setName("Track3D");
         defaultSubsets[2] = adapters[2].getDefaultSubset();
         */

         adapters[1] = new TrackAdapter(new TrackDomain(adapter_s[2], adapter_s[0]), adapter_s[1]);
         ((TrackAdapter)adapters[1]).setName("Track2D");
         defaultSubsets[1] = adapters[1].getDefaultSubset();


         properties.put("medianFilter", new String[] {Double.toString(6), Double.toString(14)});
         properties.put("setBelowSfcMissing", new String[] {"true"});
         hasTrackPreview = true;
       }
       else if ( name.startsWith("MHSx_xxx_1B") && name.endsWith("h5")) {
          HashMap table = SwathAdapter.getEmptyMetadataTable();
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
          HashMap subset = swathAdapter.getDefaultSubset();
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
           HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
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
           HashMap subset = swathAdapter0.getDefaultSubset();
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
           HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
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
           HashMap subset = swathAdapter0.getDefaultSubset();
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
           HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
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
           HashMap subset = swathAdapter0.getDefaultSubset();
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
           HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
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
           HashMap subset = swathAdapter0.getDefaultSubset();
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
      } 
      catch (Exception e) {
      }
    }

    /**
     * Make and insert the <code>DataChoice</code>-s for this
     * <code>DataSource</code>.
     */
    public void doMakeDataChoices() {
        DataChoice choice = null;
        if (adapters != null) {
          for (int idx=0; idx<adapters.length; idx++) {
             try {
               String arrayName = (adapters[idx] == null) ? "_     " : adapters[idx].getArrayName();
               choice = doMakeDataChoice(idx, arrayName);
               adapterMap.put(choice.getName(), adapters[idx]);
             } 
             catch (Exception e) {
               e.printStackTrace();
               System.out.println("doMakeDataChoice failed");
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
     * Check to see if this <code>HDFHydraDataSource</code> is equal to the object
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

    public HashMap getSubsetFromLonLatRect(MultiDimensionSubset select, GeoSelection geoSelection) {
      GeoLocationInfo ginfo = geoSelection.getBoundingBox();
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
          geoSelection = (dataSelection.getGeoSelection().getBoundingBox() != null) ? dataSelection.getGeoSelection() :
                                    dataChoice.getDataSelection().getGeoSelection();
        }

        if (geoSelection != null) {
          ginfo = geoSelection.getBoundingBox();
        }

        Data data = null;
        if (adapters == null) {
          return data;
        }

        HashMap subset = null;
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
              subset = adapter.getSubsetFromLonLatRect(ginfo.getMinLat(), ginfo.getMaxLat(),
                                                       ginfo.getMinLon(), ginfo.getMaxLon(),
                                                       geoSelection.getXStride(),
                                                       geoSelection.getYStride(),
                                                       geoSelection.getZStride());
              if (subset == null && select != null) {
                subset = select.getSubset();
              }
            }
            else {
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
            e.printStackTrace();
            System.out.println("getData exception e=" + e);
        }

        return data;
    }

    protected Data applyProperties(Data data, Hashtable requestProperties, HashMap subset) 
          throws VisADException, RemoteException {
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
          float val = sfcElev[j]*1000f; // convert to meters
          for (int i=0; i<lens[vrtIdx]; i++) {
            if (vrtIdx < trkIdx) k = i + j*lens[0];
            if (trkIdx < vrtIdx) k = j + i*lens[0];
            if (samples[2][k] <= val || samples[2][k] < 0.0) {
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
          //components.add(new edu.wisc.ssec.mcidasv.data.PreviewSelectionNew(dataChoice, image));
        } catch (Exception e) {
          System.out.println("Can't make PreviewSelection: "+e);
          e.printStackTrace();
        }
      }
      if (hasTrackPreview) {
        try {
          FlatField track = track_adapter.getData(track_adapter.getDefaultSubset());
          components.add(new TrackSelection(dataChoice, track));
        } catch (Exception e) {
          System.out.println("Can't make PreviewSelection: "+e);
          e.printStackTrace();
        }
      }
    }
}

class TrackSelection extends DataSelectionComponent {
      DataChoice dataChoice;
      FlatField track;

      double[] x_coords = new double[2];
      double[] y_coords = new double[2];
      boolean hasSubset = true;
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;

      int trackStride;
      int verticalStride;

      JTextField trkStr;
      JTextField vrtStr;


   TrackSelection(DataChoice dataChoice, FlatField track) throws VisADException, RemoteException {
        super("track");
        this.dataChoice = dataChoice;
        this.track = track;
        mapProjDsp = new MapProjectionDisplayJ3D(MapProjectionDisplay.MODE_2Din3D);
        mapProjDsp.enableRubberBanding(false);
        dspMaster = mapProjDsp;
        mapProjDsp.setMapProjection(getDataProjection());
        LineDrawing trackDsp = new LineDrawing("track");
        trackDsp.setLineWidth(2f);
        trackDsp.setData(track);
        mapProjDsp.addDisplayable(trackDsp);


        MapLines mapLines  = new MapLines("maplines");
        URL      mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPU");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }
                                                                                                                                                     
        mapLines  = new MapLines("maplines");
        mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPW");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }
                                                                                                                                                     
        mapLines  = new MapLines("maplines");
        mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLHPOL");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }

        final LineDrawing selectBox = new LineDrawing("select");
        selectBox.setColor(Color.green);

        final RubberBandBox rbb =
            new RubberBandBox(RealType.Longitude, RealType.Latitude, 1);
        rbb.setColor(Color.green);
        rbb.addAction(new CellImpl() {
          public void doAction()
             throws VisADException, RemoteException
           {
              Gridded2DSet set = rbb.getBounds();
              float[] low = set.getLow();
              float[] hi = set.getHi();
              x_coords[0] = low[0];
              x_coords[1] = hi[0];
              y_coords[0] = low[1];
              y_coords[1] = hi[1];
              
              SampledSet[] sets = new SampledSet[4];
              sets[0] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {{low[0], hi[0]}, {low[1], low[1]}}, 2);
              sets[1] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {{hi[0], hi[0]}, {low[1], hi[1]}}, 2);
              sets[2] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {{hi[0], low[0]}, {hi[1], hi[1]}}, 2);
              sets[3] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {{low[0], low[0]}, {hi[1], low[1]}}, 2);
              UnionSet uset = new UnionSet(sets);
              selectBox.setData(uset);
           }
        });
        dspMaster.addDisplayable(rbb);
        dspMaster.addDisplayable(selectBox);

        dspMaster.draw();
   }

       public MapProjection getDataProjection() {
         MapProjection mp = null;
         try {
           mp = new ProjectionCoordinateSystem(new LatLonProjection());
         } catch (Exception e) {
             System.out.println(" getDataProjection"+e);
         }
         return mp;
       }

      protected JComponent doMakeContents() {
        try {
          JPanel panel = new JPanel(new BorderLayout());
          panel.add("Center", dspMaster.getDisplayComponent());

          JPanel stridePanel = new JPanel(new FlowLayout());
          trkStr = new JTextField(Integer.toString(5), 3);
          vrtStr = new JTextField(Integer.toString(2), 3);
          trkStr.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ae) {
                setTrackStride(Integer.valueOf(trkStr.getText().trim()));
              }
          });
          vrtStr.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ae) {
                setVerticalStride(Integer.valueOf(vrtStr.getText().trim()));
              }
          });

          stridePanel.add(new JLabel("track stride: "));
          stridePanel.add(trkStr);
          stridePanel.add(new JLabel("vertical stride: "));
          stridePanel.add(vrtStr);
          panel.add("South", stridePanel);

          return panel;
        }
        catch (Exception e) {
          System.out.println(e);
        }
        return null;
      }
                                                                                                                                                     
      public void setTrackStride(int stride) {
        trackStride = stride;
      }

      public void setVerticalStride(int stride) {
        verticalStride = stride;
      }

      public void setTrackStride() {
        trackStride = Integer.valueOf(trkStr.getText().trim());
      }

      public void setVerticalStride() {
        verticalStride = Integer.valueOf(vrtStr.getText().trim());
      }

      public void applyToDataSelection(DataSelection dataSelection) {
         setTrackStride();
         setVerticalStride();
         if (hasSubset) {
           GeoSelection geoSelect = new GeoSelection(
                new GeoLocationInfo(y_coords[1], x_coords[0], y_coords[0], x_coords[1]));
           geoSelect.setXStride(trackStride);
           geoSelect.setYStride(verticalStride);
           dataSelection.setGeoSelection(geoSelect);
         }
      }
}
