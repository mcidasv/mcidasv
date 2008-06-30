/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import edu.wisc.ssec.mcidasv.data.PreviewSelection;

import edu.wisc.ssec.mcidasv.data.HydraDataSource;
import edu.wisc.ssec.mcidasv.data.hydra.ProfileAlongTrack;
import edu.wisc.ssec.mcidasv.data.hydra.ProfileAlongTrack3D;
import edu.wisc.ssec.mcidasv.data.hydra.Calipso2D;
import edu.wisc.ssec.mcidasv.control.LambertAEA;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.GeoSelectionPanel;

import ucar.unidata.util.Misc;
import ucar.unidata.idv.ViewContext;

import visad.Data;
import visad.FlatField;
import visad.GriddedSet;
import visad.Gridded2DSet;
import visad.SampledSet;
import visad.VisADException;
import visad.georef.MapProjection;
import visad.data.mcidas.BaseMapAdapter;

import java.io.File;
import java.net.URL;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.geom.Rectangle2D;

import visad.*;
import visad.bom.RubberBandBoxRendererJ3D;
import visad.java3d.DisplayImplJ3D;
import visad.java3d.TwoDDisplayRendererJ3D;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.control.DisplayControlBase;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;
import ucar.visad.display.XYDisplay;
import ucar.visad.display.MapLines;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.RubberBandBox;

import ucar.visad.ProjectionCoordinateSystem;
import ucar.unidata.geoloc.projection.LatLonProjection;

import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;



/**
 * A data source for Multi Dimension Data 
 */
public class MultiDimensionDataSource extends HydraDataSource {

    /** Sources file */
    protected String filename;

    protected MultiDimensionReader reader;

    protected MultiDimensionAdapter[] adapters = null;

    protected SpectrumAdapter spectrumAdapter;


    private static final String DATA_DESCRIPTION = "Multi Dimension Data";


    private HashMap defaultSubset;
    private SwathAdapter swathAdapter;
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
        /*
        try {
          if ( filename.endsWith(".hdf") ) {
            reader = new HDF4File(filename);
          }
        }
        catch (Exception e) {
          System.out.println("cannot create HDF4 reader for file:"+filename+" e= "+e);
          throw new VisADException();
        }
        */

        try {
            reader = new NetCDFFile(filename);
        }
        catch (Exception e) {
          e.printStackTrace();
          System.out.println("cannot create NetCDF reader for file: "+filename);
        }
                                                                                                                                                     
        adapters = new MultiDimensionAdapter[2];
        Hashtable<String, String[]> properties = new Hashtable<String, String[]>(); 

        String name = (new File(filename)).getName();
        if ( name.startsWith("MOD04") || name.startsWith("MYD04")) {
          HashMap table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "mod04/Data Fields/Optical_Depth_Land_And_Ocean");
          table.put("lon_array_name", "mod04/Geolocation Fields/Longitude");
          table.put("lat_array_name", "mod04/Geolocation Fields/Latitude");
          //-table.put("XTrack", "Cell_Across_Swath:mod04");
          //-table.put("Track", "Cell_Along_Swath:mod04");
          //-table.put("geo_Track", "Cell_Along_Swath:mod04");
          //-table.put("geo_XTrack", "Cell_Across_Swath:mod04");
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
        }
        else if ( name.startsWith("MOD06") || name.startsWith("MYD06") ) {
          HashMap table = SwathAdapter.getEmptyMetadataTable();
          //-table.put("array_name", "Cloud_Optical_Thickness");
          //-table.put("lon_array_name", "Longitude");
          //-table.put("lat_array_name", "Latitude");
          table.put("array_name", "mod06/Data Fields/Cloud_Optical_Thickness");
          table.put("lon_array_name", "mod06/Geolocation Fields/Longitude");
          table.put("lat_array_name", "mod06/Geolocation Fields/Latitude");
          //-table.put("XTrack", "Cell_Across_Swath_1km:mod06");
          //-table.put("Track", "Cell_Along_Swath_1km:mod06");
          //-table.put("geo_Track", "Cell_Along_Swath_5km:mod06");
          //-table.put("geo_XTrack", "Cell_Across_Swath_5km:mod06");
          table.put("XTrack", "Cell_Across_Swath_1km");
          table.put("Track", "Cell_Along_Swath_1km");
          table.put("geo_Track", "Cell_Along_Swath_5km");
          table.put("geo_XTrack", "Cell_Across_Swath_5km");
          table.put("scale_name", "scale_factor");
          table.put("offset_name", "add_offset");
          table.put("fill_value_name", "_FillValue");
          table.put("range_name", "Cloud_Optical_Thickness");

          table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
          table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
          table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
          table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0148148148));

          adapters[0] = new SwathAdapter(reader, table);
          categories = DataCategory.parseCategories("2D grid;GRID-2D;");
          defaultSubset = adapters[0].getDefaultSubset();
          double[] coords = (double[]) defaultSubset.get("Track");
          coords[2] = 1;
          coords = (double[]) defaultSubset.get("XTrack");
          coords[2] = 1;
        }
        else if ( name.startsWith("AIRS")) {
          HashMap table = SpectrumAdapter.getEmptyMetadataTable();
          /*
          table.put(SpectrumAdapter.array_name, "radiances");
          table.put(SpectrumAdapter.channelIndex_name, "Channel:L1B_AIRS_Science");
          table.put(SpectrumAdapter.ancillary_file_name, "/edu/wisc/ssec/mcidasv/data/hydra/resources/airs/L2.chan_prop.2003.11.19.v6.6.9.anc");
          table.put(SpectrumAdapter.x_dim_name, "GeoXTrack:L1B_AIRS_Science");
          table.put(SpectrumAdapter.y_dim_name, "GeoTrack:L1B_AIRS_Science");
          */
          table.put(SpectrumAdapter.array_name, "L1B_AIRS_Science/Data Fields/radiances");
          table.put(SpectrumAdapter.range_name, "radiances");
          table.put(SpectrumAdapter.channelIndex_name, "Channel");
          table.put(SpectrumAdapter.ancillary_file_name, "/edu/wisc/ssec/mcidasv/data/hydra/resources/airs/L2.chan_prop.2003.11.19.v6.6.9.anc");
          table.put(SpectrumAdapter.x_dim_name, "GeoXTrack");
          table.put(SpectrumAdapter.y_dim_name, "GeoTrack");
          adapters[1] = new AIRS_L1B_Spectrum(reader, table);
                                                                                                                                                     
          table = SwathAdapter.getEmptyMetadataTable();
          /*
          table.put("array_name", "radiances");
          table.put("lon_array_name", "Longitude");
          table.put("lat_array_name", "Latitude");
          table.put("XTrack", "GeoXTrack:L1B_AIRS_Science");
          table.put("Track", "GeoTrack:L1B_AIRS_Science");
          table.put("geo_Track", "GeoTrack:L1B_AIRS_Science");
          table.put("geo_XTrack", "GeoXTrack:L1B_AIRS_Science");
          table.put(SpectrumAdapter.channelIndex_name, "Channel:L1B_AIRS_Science"); //- think about this?
          */
          table.put("array_name", "L1B_AIRS_Science/Data Fields/radiances");
          table.put(SwathAdapter.range_name, "radiances");
          table.put("lon_array_name", "L1B_AIRS_Science/Geolocation Fields/Longitude");
          table.put("lat_array_name", "L1B_AIRS_Science/Geolocation Fields/Latitude");
          table.put("XTrack", "GeoXTrack");
          table.put("Track", "GeoTrack");
          table.put("geo_Track", "GeoTrack");
          table.put("geo_XTrack", "GeoXTrack");
          table.put(SpectrumAdapter.channelIndex_name, "Channel"); //- think about this?

          swathAdapter = new SwathAdapter(reader, table);
          HashMap subset = swathAdapter.getDefaultSubset();
          subset.put(SpectrumAdapter.channelIndex_name, new double[] {793,793,1});
          defaultSubset = subset;
          multiSpectData = new MultiSpectralData(swathAdapter, (SpectrumAdapter)adapters[1]);
          adapters[0] = swathAdapter;
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;");
          //-hasImagePreview = true;
          multiSpectData.init_wavenumber = 919.5f; 
       }
       else if ( name.startsWith("IASI_xxx_1C") && name.endsWith("h5")) {
          HashMap table = SpectrumAdapter.getEmptyMetadataTable();
          table.put(SpectrumAdapter.array_name, "U-MARF/EPS/IASI_xxx_1C/DATA/SPECT_DATA");
          table.put(SpectrumAdapter.channelIndex_name, "dim2");
          table.put(SpectrumAdapter.x_dim_name, "dim1");
          table.put(SpectrumAdapter.y_dim_name, "dim0");
          adapters[1] = new IASI_L1C_Spectrum(reader, table);
                                                                                                                                             
          table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "U-MARF/EPS/IASI_xxx_1C/DATA/SPECT_DATA");
          table.put("lon_array_name", "U-MARF/EPS/IASI_xxx_1C/DATA/SPECT_LON_ARRAY");
          table.put("lat_array_name", "U-MARF/EPS/IASI_xxx_1C/DATA/SPECT_LAT_ARRAY");
          table.put("XTrack", "dim1");
          table.put("Track", "dim0");
          table.put("geo_XTrack", "dim1");
          table.put("geo_Track", "dim0");
          table.put("product_name", "IASI_L1C_xxx");
          table.put(SpectrumAdapter.channelIndex_name, "dim2");
          swathAdapter = new IASI_L1C_SwathAdapter(reader, table);
          HashMap subset = swathAdapter.getDefaultSubset();
          subset.put(SpectrumAdapter.channelIndex_name, new double[] {793,793,1});
          defaultSubset = subset;
          multiSpectData = new MultiSpectralData(swathAdapter, (SpectrumAdapter)adapters[1]);
          adapters[0] = swathAdapter;
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;");
          multiSpectData.init_wavenumber = 919.5f; 
       }
       else if ( name.startsWith("IASI")) {
          HashMap table = SpectrumAdapter.getEmptyMetadataTable();
          table.put(SpectrumAdapter.array_name, "observations");
          table.put(SpectrumAdapter.channelIndex_name, "obsChannelIndex");
          table.put(SpectrumAdapter.x_dim_name, "obsElement");
          table.put(SpectrumAdapter.y_dim_name, "obsLine");
          table.put(SpectrumAdapter.channels_name, "observationChannels");
          adapters[1] = new SpectrumAdapter(reader, table);

          table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "observations");
          table.put("lon_array_name", "obsLongitude");
          table.put("lat_array_name", "obsLatitude");
          table.put("XTrack", "obsElement");
          table.put("Track", "obsLine");
          table.put("geo_XTrack", "obsElement");
          table.put("geo_Track", "obsLine");
          table.put(SpectrumAdapter.channelIndex_name, "obsChannelIndex"); //- think about this?
          swathAdapter = new SwathAdapter(reader, table);
          HashMap subset = swathAdapter.getDefaultSubset();
          subset.put(SpectrumAdapter.channelIndex_name, new double[] {793,793,1});
          defaultSubset = subset;
          multiSpectData = new MultiSpectralData(swathAdapter, (SpectrumAdapter)adapters[1]);
          adapters[0] = swathAdapter;
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;");
          multiSpectData.init_wavenumber = 919.5f; 
       }
       else if (name.startsWith("CAL_LID_L1")) {
         HashMap table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Total_Attenuated_Backscatter_532");
         table.put(ProfileAlongTrack.ancillary_file_name, "/edu/wisc/ssec/mcidasv/data/hydra/resources/calipso/altitude");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         table.put(ProfileAlongTrack.profileTime_name, "Profile_Time");
         table.put(ProfileAlongTrack.longitude_name, "Longitude");
         table.put(ProfileAlongTrack.latitude_name, "Latitude");
         ProfileAlongTrack adapter = new Calipso2D(reader, table);
         ProfileAlongTrack3D adapter3D = new ProfileAlongTrack3D(adapter);
         HashMap subset = adapter.getDefaultSubset();
         adapters[0] = adapter3D;
         defaultSubset = subset;
         DataCategory.createCategory("ProfileAlongTrack");
         categories = DataCategory.parseCategories("ProfileAlongTrack;ProfileAlongTrack;");

         ArrayAdapter[] adapter_s = new ArrayAdapter[3];
         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Latitude");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");

         adapter_s[0] = new ArrayAdapter(reader, table);
         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Surface_Elevation");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");

         adapter_s[1] = new ArrayAdapter(reader, table);
         table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Longitude");
         table.put(ProfileAlongTrack.trackDim_name, "dim0");
         table.put(ProfileAlongTrack.vertDim_name, "dim1");
         adapter_s[2] = new ArrayAdapter(reader, table);

         track_adapter = new TrackAdapter(adapter_s[2], adapter_s[0], adapter_s[1]);
         properties.put("medianFilter", new String[] {Double.toString(7), Double.toString(15)});
         properties.put("setBelowSfcMissing", new String[] {"true"});
         hasTrackPreview = true;
       }
       else if (name.startsWith("MOD02") || name.startsWith("MYD02") || name.startsWith("a1")) {
         HashMap table = SwathAdapter.getEmptyMetadataTable();
         //- Java-Netcdf
         table.put("array_name", "MODIS_SWATH_Type_L1B/Data Fields/EV_1KM_Emissive");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Latitude");
         table.put("XTrack", "Max_EV_frames");
         table.put("Track", "10*nscans");
         table.put("geo_Track", "2*nscans");
         table.put("geo_XTrack", "1KM_geo_dim");
         table.put("scale_name", "radiance_scales");
         table.put("offset_name", "radiance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "EV_1KM_Emissive");
         table.put(SpectrumAdapter.channelIndex_name, "Band_1KM_Emissive");

         //- HDF4
         /*
         table.put("array_name", "EV_1KM_Emissive");
         table.put("lon_array_name", "Longitude");
         table.put("lat_array_name", "Latitude");
         table.put("XTrack", "Max_EV_frames:MODIS_SWATH_Type_L1B");
         table.put("Track", "10*nscans:MODIS_SWATH_Type_L1B");
         table.put("geo_Track", "2*nscans:MODIS_SWATH_Type_L1B");
         table.put("geo_XTrack", "1KM_geo_dim:MODIS_SWATH_Type_L1B");
         table.put("scale_name", "radiance_scales");
         table.put("offset_name", "radiance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "EV_1KM_Emissive");
         table.put("range_name", "EV_250_Aggr1km_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_1KM_Emissive:MODIS_SWATH_Type_L1B");
         */
         //-------------

         /*
         table.put("array_name", "EV_1KM_Emissive");
         table.put("lon_array_name", "Longitude");
         table.put("lat_array_name", "Latitude");
         table.put("XTrack", "fakeDim26");
         table.put("Track", "fakeDim25");
         table.put("geo_Track", "fakeDim32");
         table.put("geo_XTrack", "fakeDim33");
         table.put("scale_name", "radiance_scales");
         table.put("offset_name", "radiance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "EV_1KM_Emissive");
         table.put(SpectrumAdapter.channelIndex_name, "fakeDim24");
         */

         /*
         table.put("array_name", "EV_250_Aggr500_RefSB");
         table.put("lon_array_name", "Longitude");
         table.put("lat_array_name", "Latitude");
         table.put("XTrack", "2*Max_EV_frames:MODIS_SWATH_Type_L1B");
         table.put("Track", "20*nscans:MODIS_SWATH_Type_L1B");
         table.put("geo_Track", "10*nscans:MODIS_SWATH_Type_L1B");
         table.put("geo_XTrack", "Max_EV_frames:MODIS_SWATH_Type_L1B");
         table.put("scale_name", "radiance_scales");
         table.put("offset_name", "radiance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "EV_250_Aggr500_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M:MODIS_SWATH_Type_L1B");
         */

         table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0));

         swathAdapter = new SwathAdapter(reader, table);
         HashMap subset = swathAdapter.getDefaultSubset();
         subset.put(SpectrumAdapter.channelIndex_name, new double[] {10,10,1});
         defaultSubset = subset;
         double[] coords = (double[]) defaultSubset.get("Track");
         coords[2] = 10;
         coords = (double[]) defaultSubset.get("XTrack");
         coords[2] = 10;

         adapters[0] = swathAdapter;
         hasImagePreview = true;

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data Fields/EV_1KM_Emissive");
         table.put(SpectrumAdapter.channelIndex_name, "Band_1KM_Emissive");
         table.put(SpectrumAdapter.x_dim_name, "Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "10*nscans");
         //-table.put(SpectrumAdapter.array_name, "EV_1KM_Emissive");
         //-table.put(SpectrumAdapter.channelIndex_name, "Band_1KM_Emissive:MODIS_SWATH_Type_L1B");
         //-table.put(SpectrumAdapter.x_dim_name, "Max_EV_frames:MODIS_SWATH_Type_L1B");
         //-table.put(SpectrumAdapter.y_dim_name, "10*nscans:MODIS_SWATH_Type_L1B");
         table.put(SpectrumAdapter.channelValues, new float[] {3.799f,3.992f,3.968f,4.070f,4.476f,4.549f,6.784f,7.345f,8.503f,9.700f,11.000f,12.005f,13.351f,13.717f,13.908f,14.205f});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, table);
         spectrumAdapter.setRangeProcessor(swathAdapter.getRangeProcessor());
         adapters[1] = spectrumAdapter;

         multiSpectData = new MultiSpectralData(swathAdapter, (SpectrumAdapter)adapters[1], "MODIS", "Aqua");
         multiSpectData.init_wavenumber = 11.0f;
         DataCategory.createCategory("MultiSpectral");
         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;");
       }
       else {
          HashMap table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "MODIS_SWATH_Type_L1B/Data Fields/EV_1KM_Emissive");
          table.put("lon_array_name", "pixel_longitude");
          table.put("lat_array_name", "pixel_latitude");
          table.put("XTrack", "elements");
          table.put("Track", "lines");
          table.put("geo_Track", "lines");
          table.put("geo_XTrack", "elements");
          table.put("scale_name", "scale_factor");
          table.put("offset_name", "add_offset");
          table.put("fill_value_name", "_FillValue");
          adapters[0] = new SwathAdapter(reader, table);
          categories = DataCategory.parseCategories("2D grid;GRID-2D;");
          defaultSubset = adapters[0].getDefaultSubset();
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
        //for (int idx=0; idx<adapters.length; idx++) {
        if (adapters != null) {
        for (int idx=0; idx<1; idx++) {
            try {
              choice = doMakeDataChoice(idx, adapters[idx].getArrayName());
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
        DataSelection dataSel = new MultiDimensionSubset(defaultSubset);
        DirectDataChoice ddc = new DirectDataChoice(this, idx, name, name,
            categories, dataSel);

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


    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {
        GeoLocationInfo ginfo = null;
        GeoSelection geoSelection = null;
        
        if ((dataSelection != null) && (dataSelection.getGeoSelection() != null)){
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

        MultiDimensionAdapter adapter = null;

        if (category == null) {
          adapter = adapters[0];
        }
        else {
          adapter = adapters[1];
        }
        adapter = adapters[0];

        try {
            HashMap subset = null;
            if (ginfo != null) {
              subset = adapter.getSubsetFromLonLatRect(ginfo.getMinLat(), ginfo.getMaxLat(),
                                        ginfo.getMinLon(), ginfo.getMaxLon());
            }
            else {
              MultiDimensionSubset select = (MultiDimensionSubset) dataChoice.getDataSelection();
              subset = select.getSubset();
            }

            if (subset != null) {
              data = adapter.getData(subset);
              data = applyProperties(data, requestProperties, subset);
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
        String[] items = (String[]) requestProperties.get("setBelowSfcMissing");
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

      try {
        components.add(new ChannelSelection(dataChoice));
      } catch (Exception e) {
        System.out.println(e); 
      }

      if (hasImagePreview) {
        try {
          FlatField image = multiSpectData.getImage(multiSpectData.init_wavenumber, defaultSubset);
          components.add(new PreviewSelection(dataChoice, image, null));
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

class ChannelSelection extends DataSelectionComponent {

   DataChoice dataChoice;
   MultiSpectralDisplay display;

   ChannelSelection(DataChoice dataChoice) throws Exception {
     super("Channels");
     this.dataChoice = dataChoice;
     display = new MultiSpectralDisplay((DirectDataChoice)dataChoice);
     display.showChannelSelector();
   }

  protected JComponent doMakeContents() {
    try {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add("Center", display.getDisplayComponent());
      return panel;
    }
    catch (Exception e) {
      System.out.println(e);
    }
    return null;
  }
                                                                                                                                                   
                                                                                                                                                   
  public void applyToDataSelection(DataSelection dataSelection) {
      /*
         if (hasSubset) {
           dataSelection.setGeoSelection(
              new GeoSelection(
                new GeoLocationInfo(y_coords[1], x_coords[0], y_coords[0], x_coords[1])));
         }
      */
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
          return panel;
        }
        catch (Exception e) {
          System.out.println(e);
        }
        return null;
      }
                                                                                                                                                     
      public void applyToDataSelection(DataSelection dataSelection) {
         if (hasSubset) {
           dataSelection.setGeoSelection(
              new GeoSelection(
                new GeoLocationInfo(y_coords[1], x_coords[0], y_coords[0], x_coords[1])));
         }
      }
}
