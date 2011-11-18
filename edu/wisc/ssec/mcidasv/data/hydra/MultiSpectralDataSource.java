/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2011
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
import java.awt.FlowLayout;
import java.awt.geom.Rectangle2D;

import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import visad.VisADException;
import visad.FunctionType;
import visad.RealType;
import visad.RealTupleType;
import visad.Linear2DSet;
import visad.CoordinateSystem;
import visad.CommonUnit;
import visad.SetType;
import visad.georef.MapProjection;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.control.LambertAEA;
import edu.wisc.ssec.mcidasv.data.ComboDataChoice;
import edu.wisc.ssec.mcidasv.data.HydraDataSource;
import edu.wisc.ssec.mcidasv.data.PreviewSelection;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;

/**
 * A data source for Multi Dimension Data 
 */

public class MultiSpectralDataSource extends HydraDataSource {

	private static final Logger logger = LoggerFactory.getLogger(MultiSpectralDataSource.class);
	
	/** Sources file */
    protected String filename;

    protected MultiDimensionReader reader;

    protected MultiDimensionAdapter[] adapters = null;

    private static final String DATA_DESCRIPTION = "Multi Dimension Data";


    private HashMap defaultSubset;
    private SwathAdapter swathAdapter;
    private SpectrumAdapter spectrumAdapter;
    private MultiSpectralData multiSpectData;

    private ArrayList<MultiSpectralData> multiSpectData_s = new ArrayList<MultiSpectralData>();
    private HashMap<String, MultiSpectralData> adapterMap = new HashMap<String, MultiSpectralData>();

    private List categories;
    private boolean hasImagePreview = false;
    private boolean hasChannelSelect = false;

    private boolean doAggregation = false;

    private ComboDataChoice comboChoice;

    private PreviewSelection previewSelection = null;
    private FlatField previewImage = null;

    public static final String paramKey = "paramKey";

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    public MultiSpectralDataSource() {}

    public MultiSpectralDataSource(String fileName) throws VisADException {
      this(null, Misc.newList(fileName), null);
    }

    /**
     * Construct a new HYDRA hdf data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  fileName  name of the hdf file to read
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public MultiSpectralDataSource(DataSourceDescriptor descriptor,
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
    public MultiSpectralDataSource(DataSourceDescriptor descriptor,
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
        String name = (new File(filename)).getName();
    	// aggregations will use sets of NetCDFFile readers
    	ArrayList<NetCDFFile> ncdfal = new ArrayList<NetCDFFile>();

        try {
          if (name.startsWith("NSS.HRPT.NP") && name.endsWith("obs.hdf")) { // get file union
            String other = new String(filename);
            other = other.replace("obs", "nav");
            reader = NetCDFFile.makeUnion(filename, other);
          }
          else {
        	  if (sources.size() > 1) {
        		  for (int i = 0; i < sources.size(); i++) {
        			  String s = (String) sources.get(i);
        			  ncdfal.add(new NetCDFFile(s));
        		  }
        		  doAggregation = true;
        	  } else {
        		  reader = new NetCDFFile(filename);
        	  }
          }
        }
        catch (Exception e) {
          e.printStackTrace();
          System.out.println("cannot create NetCDF reader for file: "+filename);
        }
                                                                                                                                                     
        Hashtable<String, String[]> properties = new Hashtable<String, String[]>(); 


        multiSpectData_s.clear();

        if ( name.startsWith("AIRS")) {
          HashMap table = SpectrumAdapter.getEmptyMetadataTable();
          table.put(SpectrumAdapter.array_name, "L1B_AIRS_Science/Data Fields/radiances");
          table.put(SpectrumAdapter.range_name, "radiances");
          table.put(SpectrumAdapter.channelIndex_name, "Channel");
          table.put(SpectrumAdapter.ancillary_file_name, "/edu/wisc/ssec/mcidasv/data/hydra/resources/airs/L2.chan_prop.2003.11.19.v6.6.9.anc");
          table.put(SpectrumAdapter.x_dim_name, "GeoXTrack");
          table.put(SpectrumAdapter.y_dim_name, "GeoTrack");
          spectrumAdapter = new AIRS_L1B_Spectrum(reader, table);
                                                                                                                                                     
          table = SwathAdapter.getEmptyMetadataTable();
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
          multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter);
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
          hasChannelSelect = true;
          multiSpectData.init_wavenumber = 919.5f; 
          multiSpectData_s.add(multiSpectData);
       }
       else if ( name.startsWith("IASI_xxx_1C") && name.endsWith("h5")) {
          HashMap table = SpectrumAdapter.getEmptyMetadataTable();
          table.put(SpectrumAdapter.array_name, "U-MARF/EPS/IASI_xxx_1C/DATA/SPECT_DATA");
          table.put(SpectrumAdapter.channelIndex_name, "dim2");
          table.put(SpectrumAdapter.x_dim_name, "dim1");
          table.put(SpectrumAdapter.y_dim_name, "dim0");
          spectrumAdapter = new IASI_L1C_Spectrum(reader, table);
                                                                                                                                             
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
          multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter);
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;");
          multiSpectData.init_wavenumber = 919.5f; 
          hasChannelSelect = true;
          multiSpectData_s.add(multiSpectData);
       }
       else if ( name.startsWith("IASI")) {
          HashMap table = SpectrumAdapter.getEmptyMetadataTable();
          table.put(SpectrumAdapter.array_name, "observations");
          table.put(SpectrumAdapter.channelIndex_name, "obsChannelIndex");
          table.put(SpectrumAdapter.x_dim_name, "obsElement");
          table.put(SpectrumAdapter.y_dim_name, "obsLine");
          table.put(SpectrumAdapter.channels_name, "observationChannels");
          spectrumAdapter = new SpectrumAdapter(reader, table);

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
          multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter);
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;");
          multiSpectData.init_wavenumber = 919.5f; 
          multiSpectData_s.add(multiSpectData);
          hasChannelSelect = true;
       }
       else if (name.startsWith("MOD021KM") || name.startsWith("MYD021KM") || 
               (name.startsWith("a1") && (name.indexOf("1000m") > 0)) || 
               (name.startsWith("t1") && (name.indexOf("1000m") > 0)) ) {
         HashMap table = SwathAdapter.getEmptyMetadataTable();
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
         table.put("range_name", "Emissive_Bands");
         table.put(SpectrumAdapter.channelIndex_name, "Band_1KM_Emissive");
         table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0));
         
         // initialize the aggregation reader object
         logger.debug("Trying to create MODIS 1K GranuleAggregation reader...");
         if (doAggregation) {
        	 try {
        		 reader = new GranuleAggregation(ncdfal, 2030, "10*nscans", "Max_EV_frames");
        	 } catch (Exception e) {
        		 throw new VisADException("Unable to initialize aggregation reader");
        	 }
         }

         swathAdapter = new SwathAdapter(reader, table);
         swathAdapter.setDefaultStride(10);
         logger.debug("Trying to create MODIS 1K SwathAdapter..."); 

         HashMap subset = swathAdapter.getDefaultSubset();

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data Fields/EV_1KM_Emissive");
         table.put(SpectrumAdapter.channelIndex_name, "Band_1KM_Emissive");
         table.put(SpectrumAdapter.x_dim_name, "Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "10*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
           {3.799f,3.992f,3.968f,4.070f,4.476f,4.549f,6.784f,7.345f,8.503f,
            9.700f,11.000f,12.005f,13.351f,13.717f,13.908f,14.205f});
         table.put(SpectrumAdapter.bandNames, new String[] 
           {"20","21","22","23","24","25","27","28","29",
            "30","31","32","33","34","35","36"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, table);

         multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter, "MODIS", "Aqua");
         multiSpectData.setInitialWavenumber(11.0f);
         defaultSubset = multiSpectData.getDefaultSubset();

         previewImage = multiSpectData.getImage(defaultSubset);
         multiSpectData_s.add(multiSpectData);

         //--- aggregate reflective bands
         table = SwathAdapter.getEmptyMetadataTable();

         table.put("array_name", "MODIS_SWATH_Type_L1B/Data Fields/EV_1KM_RefSB");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Latitude");
         table.put("XTrack", "Max_EV_frames");
         table.put("Track", "10*nscans");
         table.put("geo_Track", "2*nscans");
         table.put("geo_XTrack", "1KM_geo_dim");
         table.put("scale_name", "reflectance_scales");
         table.put("offset_name", "reflectance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "EV_1KM_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_1KM_RefSB");

         table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0));

         SwathAdapter sadapt0 = new SwathAdapter(reader, table);
         sadapt0.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data Fields/EV_1KM_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_1KM_RefSB");
         table.put(SpectrumAdapter.x_dim_name, "Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "10*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
            {.412f,.450f,.487f,.531f,.551f,.666f,.668f,.677f,.679f,.748f,
             .869f,.905f,.936f,.940f,1.375f});
         table.put(SpectrumAdapter.bandNames, new String[]
            {"8","9","10","11","12","13lo","13hi","14lo","14hi","15",
             "16","17","18","19","26"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter specadap0 = new SpectrumAdapter(reader, table);
         MultiSpectralData multispec0 = new MultiSpectralData(sadapt0, specadap0, "Reflectance", "Reflectance", "MODIS", "Aqua");

         DataCategory.createCategory("MultiSpectral");
         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
         hasImagePreview = true;
         hasChannelSelect = true;

         table = SwathAdapter.getEmptyMetadataTable();

         table.put("array_name", "MODIS_SWATH_Type_L1B/Data Fields/EV_250_Aggr1km_RefSB");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Latitude");
         table.put("XTrack", "Max_EV_frames");
         table.put("Track", "10*nscans");
         table.put("geo_Track", "2*nscans");
         table.put("geo_XTrack", "1KM_geo_dim");
         table.put("scale_name", "reflectance_scales");
         table.put("offset_name", "reflectance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Reflective_Bands");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M");

         table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0));

         SwathAdapter sadapt1 = new SwathAdapter(reader, table);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data Fields/EV_250_Aggr1km_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M");
         table.put(SpectrumAdapter.x_dim_name, "Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "10*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
            {.650f,.855f});
         table.put(SpectrumAdapter.bandNames, new String[]
            {"1","2"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter specadap1 = new SpectrumAdapter(reader, table);
         MultiSpectralData multispec1 = new MultiSpectralData(sadapt1, specadap1, "Reflectance", "Reflectance", "MODIS", "Aqua");

         table = SwathAdapter.getEmptyMetadataTable();

         table.put("array_name", "MODIS_SWATH_Type_L1B/Data Fields/EV_500_Aggr1km_RefSB");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Latitude");
         table.put("XTrack", "Max_EV_frames");
         table.put("Track", "10*nscans");
         table.put("geo_Track", "2*nscans");
         table.put("geo_XTrack", "1KM_geo_dim");
         table.put("scale_name", "reflectance_scales");
         table.put("offset_name", "reflectance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "EV_500_Aggr1km_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_500M");
         table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0));

         SwathAdapter sadapt2 = new SwathAdapter(reader, table);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data Fields/EV_500_Aggr1km_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_500M");
         table.put(SpectrumAdapter.x_dim_name, "Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "10*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
            {.470f,.555f,1.240f,1.638f,2.130f});
         table.put(SpectrumAdapter.bandNames, new String[]
            {"3","4","5","6","7"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter specadap2 = new SpectrumAdapter(reader, table);
         MultiSpectralData multispec2 = new MultiSpectralData(sadapt2, specadap2, "Reflectance", "Reflectance", "MODIS", "Aqua");

         MultiSpectralAggr aggr = new MultiSpectralAggr(new MultiSpectralData[] {multispec1, multispec2, multispec0});
         aggr.setInitialWavenumber(0.650f);
         aggr.setDataRange(new float[] {0f, 0.8f});
         multiSpectData_s.add(aggr);
       }
       else if (name.startsWith("MOD02QKM") || name.startsWith("MYD02QKM") ||
               (name.startsWith("a1") && (name.indexOf("250m") > 0)) ||
               (name.startsWith("t1") && (name.indexOf("250m") > 0)) ) {
         HashMap table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "MODIS_SWATH_Type_L1B/Data Fields/EV_250_RefSB");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Latitude");
         table.put("XTrack", "4*Max_EV_frames");
         table.put("Track", "40*nscans");
         table.put("geo_Track", "10*nscans");
         table.put("geo_XTrack", "Max_EV_frames");
         table.put("scale_name", "reflectance_scales");
         table.put("offset_name", "reflectance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Reflective_Bands");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M");
         table.put(SwathAdapter.geo_track_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(4.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(4.0));

         swathAdapter = new SwathAdapter(reader, table);
         swathAdapter.setDefaultStride(40);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data Fields/EV_250_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M");
         table.put(SpectrumAdapter.x_dim_name, "4*Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "40*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
            {.650f,.855f});
         table.put(SpectrumAdapter.bandNames, new String[]
            {"1","2"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, table);

         multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter, "Reflectance", "Reflectance", "MODIS", "Aqua");
         multiSpectData.setInitialWavenumber(0.650f);
         multiSpectData.setDataRange(new float[] {0f, 0.8f});
         defaultSubset = multiSpectData.getDefaultSubset();
         previewImage = multiSpectData.getImage(defaultSubset);
         multiSpectData_s.add(multiSpectData);

         DataCategory.createCategory("MultiSpectral");
         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
         hasImagePreview = true;
         hasChannelSelect = true;

         multiSpectData_s.add(null);
       }
       else if (name.startsWith("MOD02HKM") || name.startsWith("MYD02HKM") ||
               (name.startsWith("a1") && (name.indexOf("500m") > 0)) ||
               (name.startsWith("t1") && (name.indexOf("500m") > 0)) ) {
         HashMap table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "MODIS_SWATH_Type_L1B/Data Fields/EV_250_Aggr500_RefSB");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Latitude");
         table.put("XTrack", "2*Max_EV_frames");
         table.put("Track", "20*nscans");
         table.put("geo_Track", "10*nscans");
         table.put("geo_XTrack", "Max_EV_frames");
         table.put("scale_name", "reflectance_scales");
         table.put("offset_name", "reflectance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Reflective_Bands");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M");
         table.put(SwathAdapter.geo_track_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(2.0));

         SwathAdapter swathAdapter0 = new SwathAdapter(reader, table);
         swathAdapter0.setDefaultStride(20);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data Fields/EV_250_Aggr500_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M");
         table.put(SpectrumAdapter.x_dim_name, "2*Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "20*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
            {.650f,.855f});
         table.put(SpectrumAdapter.bandNames, new String[]
            {"1","2"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter0 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "Reflectance", "Reflectance", "MODIS", "Aqua");

         table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "MODIS_SWATH_Type_L1B/Data Fields/EV_500_RefSB");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation Fields/Latitude");
         table.put("XTrack", "2*Max_EV_frames");
         table.put("Track", "20*nscans");
         table.put("geo_Track", "10*nscans");
         table.put("geo_XTrack", "Max_EV_frames");
         table.put("scale_name", "reflectance_scales");
         table.put("offset_name", "reflectance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Reflective_Bands");
         table.put(SpectrumAdapter.channelIndex_name, "Band_500M");
         table.put(SwathAdapter.geo_track_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(2.0));

         SwathAdapter swathAdapter1 = new SwathAdapter(reader, table);
         swathAdapter1.setDefaultStride(20);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data Fields/EV_500_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_500M");
         table.put(SpectrumAdapter.x_dim_name, "2*Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "20*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
            {.470f,.555f,1.240f,1.638f,2.130f});
         table.put(SpectrumAdapter.bandNames, new String[]
            {"3","4","5","6","7"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter1 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "Reflectance", "Reflectance", "MODIS", "Aqua");

         MultiSpectralAggr aggr = 
            new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData0, multiSpectData1});
         aggr.setInitialWavenumber(0.650f);
         aggr.setDataRange(new float[] {0f, 0.8f});
         multiSpectData_s.add(aggr);
         multiSpectData = aggr;
         defaultSubset = aggr.getDefaultSubset();
         previewImage = aggr.getImage(defaultSubset);

         DataCategory.createCategory("MultiSpectral");
         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
         hasImagePreview = true;
         hasChannelSelect = true;

         multiSpectData_s.add(null);
       }
       else if (name.startsWith("NSS")) {
         HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
         swthTable.put("array_name", "ch3b_temperature");
         swthTable.put("lon_array_name", "pixel_longitude");
         swthTable.put("lat_array_name", "pixel_latitude");
         swthTable.put("XTrack", "pixels_across_track");
         swthTable.put("Track", "scan_lines_along_track");
         swthTable.put("geo_Track", "scan_lines_along_track");
         swthTable.put("geo_XTrack", "pixels_across_track");
         swthTable.put("scale_name", "SCALE_FACTOR");
         swthTable.put("offset_name", "ADD_OFFSET");
         swthTable.put("fill_value_name", "_FILLVALUE");
         swthTable.put("range_name", "Emmissive_Bands");
         swthTable.put("unpack", "unpack");
         swthTable.put("geo_scale_name", "SCALE_FACTOR");
         swthTable.put("geo_offset_name", "ADD_OFFSET");
         swthTable.put("geo_fillValue_name", "_FILLVALUE");


         SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
         swathAdapter0.setDefaultStride(10);
         HashMap subset = swathAdapter0.getDefaultSubset();
         defaultSubset = subset;

         HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
         specTable.put(SpectrumAdapter.array_name, "ch3b_temperature");
         specTable.put(SpectrumAdapter.x_dim_name, "pixels_across_track");
         specTable.put(SpectrumAdapter.y_dim_name, "scan_lines_along_track");
         specTable.put(SpectrumAdapter.channelValues, new float[] {3.740f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch3b"});
         specTable.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter0 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "BrightnessTemp", "BrightnessTemp", null, null);

         HashMap table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "ch4_temperature");
         table.put("lon_array_name", "pixel_longitude");
         table.put("lat_array_name", "pixel_latitude");
         table.put("XTrack", "pixels_across_track");
         table.put("Track", "scan_lines_along_track");
         table.put("geo_Track", "scan_lines_along_track");
         table.put("geo_XTrack", "pixels_across_track");
         table.put("scale_name", "SCALE_FACTOR");
         table.put("offset_name", "ADD_OFFSET");
         table.put("fill_value_name", "_FILLVALUE");
         table.put("range_name", "Emmissive_Bands");
         table.put("unpack", "unpack");
         swthTable.put("geo_scale_name", "SCALE_FACTOR");
         swthTable.put("geo_offset_name", "ADD_OFFSET");
         swthTable.put("geo_fillValue_name", "_FILLVALUE");


         SwathAdapter swathAdapter1 = new SwathAdapter(reader, table);
         swathAdapter1.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "ch4_temperature");
         table.put(SpectrumAdapter.x_dim_name, "pixels_across_track");
         table.put(SpectrumAdapter.y_dim_name, "scan_lines_along_track");
         table.put(SpectrumAdapter.channelValues, new float[] {10.80f});
         table.put(SpectrumAdapter.bandNames, new String[] {"ch4"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter1 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "BrightnessTemp", "BrightnessTemp", null, null);

         table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "ch5_temperature");
         table.put("lon_array_name", "pixel_longitude");
         table.put("lat_array_name", "pixel_latitude");
         table.put("XTrack", "pixels_across_track");
         table.put("Track", "scan_lines_along_track");
         table.put("geo_Track", "scan_lines_along_track");
         table.put("geo_XTrack", "pixels_across_track");
         table.put("scale_name", "SCALE_FACTOR");
         table.put("offset_name", "ADD_OFFSET");
         table.put("fill_value_name", "_FILLVALUE");
         table.put("range_name", "Emmissive_Bands");
         table.put("unpack", "unpack");
         swthTable.put("geo_scale_name", "SCALE_FACTOR");
         swthTable.put("geo_offset_name", "ADD_OFFSET");
         swthTable.put("geo_fillValue_name", "_FILLVALUE");


         SwathAdapter swathAdapter2 = new SwathAdapter(reader, table);
         swathAdapter2.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "ch5_temperature");
         table.put(SpectrumAdapter.x_dim_name, "pixels_across_track");
         table.put(SpectrumAdapter.y_dim_name, "scan_lines_along_track");
         table.put(SpectrumAdapter.channelValues, new float[] {12.00f});
         table.put(SpectrumAdapter.bandNames, new String[] {"ch5"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter2 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "BrightnessTemp", "BrightnessTemp", null, null);


         MultiSpectralAggr aggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData0, multiSpectData1, multiSpectData2});
         aggr.setInitialWavenumber(3.740f);
         aggr.setDataRange(new float[] {180f, 340f});
         multiSpectData = aggr;
         multiSpectData_s.add(aggr);
         defaultSubset = aggr.getDefaultSubset();
         previewImage = aggr.getImage(defaultSubset);

         //- now do the reflective bands
         swthTable.put("array_name", "ch1_reflectance");
         swthTable.put("range_name", "Reflective_Bands");

         swathAdapter0 = new SwathAdapter(reader, swthTable);
         swathAdapter0.setDefaultStride(10);

         specTable.put(SpectrumAdapter.array_name, "ch1_reflectance");
         specTable.put(SpectrumAdapter.channelValues, new float[] {0.630f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch1"});
         spectrumAdapter0 = new SpectrumAdapter(reader, specTable);

         multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "Reflectance", "Reflectance", null, null);

         swthTable.put("array_name", "ch2_reflectance");
         swthTable.put("range_name", "Reflective_Bands");
         
         swathAdapter1 = new SwathAdapter(reader, swthTable);
         swathAdapter1.setDefaultStride(10);
         
         specTable.put(SpectrumAdapter.array_name, "ch2_reflectance");
         specTable.put(SpectrumAdapter.channelValues, new float[] {0.862f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch2"});
         spectrumAdapter1 = new SpectrumAdapter(reader, specTable);

         multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "Reflectance", "Reflectance", null, null);

         swthTable.put("array_name", "ch3ab_reflectance");
         swthTable.put("range_name", "Reflective_Bands");
         
         swathAdapter2 = new SwathAdapter(reader, swthTable);
         swathAdapter2.setDefaultStride(10);
         subset = swathAdapter2.getDefaultSubset();
         defaultSubset = subset;
         
         specTable.put(SpectrumAdapter.array_name, "ch3ab_reflectance");
         specTable.put(SpectrumAdapter.channelValues, new float[] {1.610f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch3ab"});
         spectrumAdapter2 = new SpectrumAdapter(reader, specTable);

         multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "Reflectance", "Reflectance", null, null);

         aggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData0, multiSpectData1, multiSpectData2});
         aggr.setInitialWavenumber(0.630f);
         aggr.setDataRange(new float[] {0f, 100f});
         multiSpectData_s.add(aggr);

         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");

         hasImagePreview = true;
         hasChannelSelect = true;
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
          swathAdapter = new SwathAdapter(reader, table);
          categories = DataCategory.parseCategories("2D grid;GRID-2D;");
          defaultSubset = swathAdapter.getDefaultSubset();
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
        try {
          for (int k=0; k<multiSpectData_s.size(); k++) {
            MultiSpectralData adapter = multiSpectData_s.get(k);
            DataChoice choice = doMakeDataChoice(k, adapter);
            adapterMap.put(choice.getName(), adapter);
            addDataChoice(choice);
          }
        }
        catch(Exception e) {
          e.printStackTrace();
        }
    }

    public void addChoice(String name, Data data) {
        ComboDataChoice combo = new ComboDataChoice(name + hashCode(), name, new Hashtable(), data);
        addDataChoice(combo);
        getDataContext().dataSourceChanged(this);
    }

    private DataChoice doMakeDataChoice(int idx, MultiSpectralData adapter) throws Exception {
        String name = "_    ";
        DataSelection dataSel = new MultiDimensionSubset();
        if (adapter != null) {
          name = adapter.getName();
          dataSel = new MultiDimensionSubset(defaultSubset);
        }

        Hashtable subset = new Hashtable();
        subset.put(MultiDimensionSubset.key, dataSel);
        if (adapter != null) {
          subset.put(MultiSpectralDataSource.paramKey, adapter.getParameter());
        }

        DirectDataChoice ddc = new DirectDataChoice(this, new Integer(idx), name, name, categories, subset);
        ddc.setProperties(subset);
        return ddc;
    }

    /**
     * Check to see if this <code>HDFHydraDataSource</code> is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof MultiSpectralDataSource)) {
            return false;
        }
        return (this == (MultiSpectralDataSource) o);
    }

    public MultiSpectralData getMultiSpectralData() {
      return multiSpectData;
    }

    public MultiSpectralData getMultiSpectralData(DataChoice choice) {
      return adapterMap.get(choice.getName());
    }

    public MultiSpectralData getMultiSpectralData(String name) {
      return adapterMap.get(name);
    }

    public MultiSpectralData getMultiSpectralData(int idx) {
      return multiSpectData_s.get(idx);
    }

    public String getDatasetName() {
      return filename;
    }

    public void setDatasetName(String name) {
      filename = name;
    }

    public ComboDataChoice getComboDataChoice() {
      return comboChoice;
    }

    /**
     * Called by the IDV's persistence manager in an effort to collect all of
     * the files that should be included in a zipped bundle.
     * 
     * @return Singleton list containing the file that this data source came from.
     */
    @Override public List getDataPaths() {
        return Collections.singletonList(filename);
    }

  /**
    public HashMap getSubsetFromLonLatRect(MultiDimensionSubset select, GeoSelection geoSelection) {
      GeoLocationInfo ginfo = geoSelection.getBoundingBox();
      return adapters[0].getSubsetFromLonLatRect(select.getSubset(), ginfo.getMinLat(), ginfo.getMaxLat(),
                                        ginfo.getMinLon(), ginfo.getMaxLon());
    }
   */

    public synchronized Data getData(String name, HashMap subset) throws VisADException, RemoteException {
      MultiSpectralData msd =  getMultiSpectralData(name);
      Data data = null;
      try {
        data = msd.getImage(subset);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return data;
    }


    public synchronized Data getData(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {
       return this.getDataInner(dataChoice, category, dataSelection, requestProperties);

    }

    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {

        //- this hack keeps the HydraImageProbe from doing a getData()
        //- TODO: need to use categories?
        if (requestProperties != null) {
          if ((requestProperties.toString()).contains("ReadoutProbe")) {
            return null;
          }
        }

        GeoLocationInfo ginfo = null;
        GeoSelection geoSelection = null;
        
        if ((dataSelection != null) && (dataSelection.getGeoSelection() != null)) {
          if (dataSelection.getGeoSelection().getBoundingBox() != null) {
            geoSelection = dataSelection.getGeoSelection();
          }
          else if (dataChoice.getDataSelection() != null) {
            geoSelection = dataChoice.getDataSelection().getGeoSelection();
          }
        }

        if (geoSelection != null) {
          ginfo = geoSelection.getBoundingBox();
        }

        Data data = null;

        try {
            HashMap subset = null;
            if (ginfo != null) {
              subset = swathAdapter.getSubsetFromLonLatRect(ginfo.getMinLat(), ginfo.getMaxLat(),
                                        ginfo.getMinLon(), ginfo.getMaxLon());
            }
            else {
              MultiDimensionSubset select = null;
              Hashtable table = dataChoice.getProperties();
              Enumeration keys = table.keys();
              while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                if (key instanceof MultiDimensionSubset) {
                  select = (MultiDimensionSubset) table.get(key);
                }
              }  
              if (select != null) {
                subset = select.getSubset();
              }

              if (dataSelection != null) {
                  Hashtable props = dataSelection.getProperties();
                  if (props != null) {
                    if (props.containsKey(MultiDimensionSubset.key)) {
                      subset = (HashMap)((MultiDimensionSubset)props.get(MultiDimensionSubset.key)).getSubset();
                    }
                    else {
                      subset = defaultSubset;
                    }
                    if (props.containsKey(SpectrumAdapter.channelIndex_name)) {
                      int idx = ((Integer) props.get(SpectrumAdapter.channelIndex_name)).intValue();
                      double[] coords = (double[]) subset.get(SpectrumAdapter.channelIndex_name);
                      if (coords == null) {
                        coords = new double[] {(double)idx, (double)idx, (double)1};
                        subset.put(SpectrumAdapter.channelIndex_name, coords);
                      }
                      else {
                        coords[0] = (double)idx;
                        coords[1] = (double)idx;
                        coords[2] = (double)1;
                      }
                   }
                 }
               }
            }

            if (subset != null) {
              MultiSpectralData multiSpectData = getMultiSpectralData(dataChoice);
              if (multiSpectData != null) {
                data = multiSpectData.getImage(subset);
                data = applyProperties(data, requestProperties, subset);
              }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("getData exception e=" + e);
        }
        return data;
    }

    public MapProjection getDataProjection(HashMap subset) {
      MapProjection mp = null;
      try {
        Rectangle2D rect =  multiSpectData.getLonLatBoundingBox(subset);
        mp = new LambertAEA(rect);
      }
      catch (Exception e) {
        logException("MultiSpectralDataSource.getDataProjection", e);
      }
      return mp;
    }

    protected Data applyProperties(Data data, Hashtable requestProperties, HashMap subset) 
          throws VisADException, RemoteException {
      Data new_data = data;

      if (requestProperties == null) {
        new_data = data;
        return new_data;
      }
      return new_data;
    }

    protected void initDataSelectionComponents(
         List<DataSelectionComponent> components,
             final DataChoice dataChoice) {

      if (System.getProperty("os.name").equals("Mac OS X") && hasImagePreview && hasChannelSelect) {
        try {
          components.add(new ImageChannelSelection(new PreviewSelection(dataChoice, previewImage, null), new ChannelSelection(dataChoice)));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      else {
        if (hasImagePreview) {
          try {
            previewSelection = new PreviewSelection(dataChoice, previewImage, null);
            components.add(previewSelection);
          } catch (Exception e) {
            System.out.println("Can't make PreviewSelection: "+e);
            e.printStackTrace();
          }
        }
        if (hasChannelSelect) {
          try {
            components.add(new ChannelSelection(dataChoice));
          } 
          catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }



  public static MapProjection getDataProjection(FlatField fltField) throws Exception {
    Rectangle2D rect = MultiSpectralData.getLonLatBoundingBox(fltField);
    MapProjection mp = new LambertAEA(rect, false);
    return mp;
  }

  public static Linear2DSet makeGrid(MapProjection mp, double res) throws Exception {
    Rectangle2D rect = mp.getDefaultMapArea();

    int xLen = (int) (rect.getWidth()/res);
    int yLen = (int) (rect.getHeight()/res);

    RealType xmap = RealType.getRealType("xmap", CommonUnit.meter);
    RealType ymap = RealType.getRealType("ymap", CommonUnit.meter);

    RealTupleType rtt = new visad.RealTupleType(xmap, ymap, mp, null);

    Linear2DSet grid = new Linear2DSet(rtt, rect.getX(), (xLen-1)*res, xLen,
		                            rect.getY(), (yLen-1)*res, yLen);
    return grid;
  }

  public static FlatField swathToGrid(Linear2DSet grid, FlatField swath) throws Exception {
    return swathToGrid(grid, swath, 0.0);
  }

  public static FlatField swathToGrid(Linear2DSet grid, FlatField swath, double mode) throws Exception {
    FunctionType ftype = (FunctionType) swath.getType();
    Linear2DSet swathDomain = (Linear2DSet) swath.getDomainSet();
    int[] lens = swathDomain.getLengths();
    float[][] swathRange = swath.getFloats(false);
    int trackLen = lens[1];
    int xtrackLen = lens[0];
    int gridLen = grid.getLength();
    lens = grid.getLengths();
    int gridXLen = lens[0];
    int gridYLen = lens[1];

    CoordinateSystem swathCoordSys = swathDomain.getCoordinateSystem();
    CoordinateSystem gridCoordSys = grid.getCoordinateSystem();

    RealTupleType rtt = ((SetType)grid.getType()).getDomain();
    FlatField grdFF = new FlatField(new FunctionType(rtt, ftype.getRange()), grid);
    float[][] gridRange = grdFF.getFloats(false);
    int rngTupDim = gridRange.length;

    float[][] swathGridCoord = new float[2][gridLen];
    java.util.Arrays.fill(swathGridCoord[0], Float.NaN);

    int[] swathIndexAtGrid = null;
    if (true) {
      swathIndexAtGrid = new int[gridLen];
    }

    for (int j=0; j < trackLen; j++) {
       for (int i=0; i < xtrackLen; i++) {
         int swathIdx = j*xtrackLen + i;
	 float val = swathRange[0][swathIdx];

	 float[][] swathCoord = swathDomain.indexToValue(new int[] {swathIdx});
	 float[][] swathEarthCoord = swathCoordSys.toReference(swathCoord);

	 float[][] gridValue = gridCoordSys.fromReference(swathEarthCoord);
	 int grdIdx = (grid.valueToIndex(gridValue))[0];
         float[][] gridCoord = grid.valueToGrid(gridValue);

               int m=0;
               int n=0;
               int k = grdIdx + (m + n*gridXLen);

               if ( !(Float.isNaN(val)) && ((k >=0) && (k < gridXLen*gridYLen)) ) {
                 float grdVal = gridRange[0][k];

                 if (Float.isNaN(grdVal)) {
                   for (int t=0; t<rngTupDim; t++) {
                     gridRange[t][k] = val;
                   }
                   swathGridCoord[0][k] = gridCoord[0][0];
                   swathGridCoord[1][k] = gridCoord[1][0];
                   swathIndexAtGrid[k] = swathIdx;
                 }
                 else {
                   /**
                   // compare current to last distance
                   float[][] gridLoc = grid.indexToValue(new int[] {k});
                   gridLoc = grid.valueToGrid(gridLoc);
                   
                   float del_0 = swathGridCoord[0][k] - gridLoc[0][0];
                   float del_1 = swathGridCoord[1][k] - gridLoc[1][0];
                   float last_dst_sqrd = del_0*del_0 + del_1*del_1;

                   del_0 = gridCoord[0][0] - gridLoc[0][0];
                   del_1 = gridCoord[1][0] - gridLoc[1][0];
                   float dst_sqrd = del_0*del_0 + del_1*del_1;

                   if (dst_sqrd < last_dst_sqrd) {
                     for (int t=0; t<rngTupDim; t++) {
                       gridRange[t][k] = val;
                     }
                     swathGridCoord[0][k] = gridCoord[0][0];
                     swathGridCoord[1][k] = gridCoord[1][0];
                     swathIndexAtGrid[k] = swathIdx;
                   }
                   **/
                 }

               }
       }
    }


    // 2nd pass weighted average
    float[][] gCoord = new float[2][1];
    if (mode > 0.0) {
    float weight = 1f;
    for (int j=2; j<gridYLen-2; j++) {
       for (int i=2; i<gridXLen-2; i++) {
         int grdIdx = i + j*gridXLen;
     
         // dont to weighted average if a nearest neigbhor existed for the grid point
         if (!Float.isNaN(gridRange[0][grdIdx])) {
           continue;
         }

         gCoord[0][0] = swathGridCoord[0][grdIdx];
         gCoord[1][0] = swathGridCoord[1][grdIdx];
         float del_0 = gCoord[0][0] - (float) i;
         float del_1 = gCoord[1][0] - (float) j;
         float dst_sqrd = del_0*del_0 + del_1*del_1;
         
         int num = 0;
         float sumWeights = 0f;
         float sumValue = 0f;
         for (int n = -1; n < 2; n++) {
            for (int m = -1; m < 2; m++) {
               int k = grdIdx + (m + n*gridXLen);

               if ( !Float.isNaN(swathGridCoord[0][k]) ) {

                  gCoord[0][0] = swathGridCoord[0][k];
                  gCoord[1][0] = swathGridCoord[1][k];
                  del_0 = gCoord[0][0] - (float) i;
                  del_1 = gCoord[1][0] - (float) j;
                  dst_sqrd = del_0*del_0 + del_1*del_1;
                  weight = (float) (1.0/Math.exp((double)(dst_sqrd)*2.75f));

                  sumValue += swathRange[0][swathIndexAtGrid[k]]*weight;
                  sumWeights += weight;
                  num++;
               }
            }
          }

          sumValue /= sumWeights;
          gridRange[0][grdIdx] = sumValue;
       }
    }
    }

   grdFF.setSamples(gridRange);

   return grdFF;
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
      if (display.getBandSelectComboBox() != null) {
        JPanel bandPanel = new JPanel(new FlowLayout());
        bandPanel.add(new JLabel("Band: "));
        bandPanel.add(display.getBandSelectComboBox());
        panel.add("South", bandPanel);
      }
      return panel;
    }
    catch (Exception e) {
      System.out.println(e);
    }
    return null;
  }

  public void applyToDataSelection(DataSelection dataSelection) {
      try {
        dataSelection.putProperty(Constants.PROP_CHAN, display.getWaveNumber());
        dataSelection.putProperty(SpectrumAdapter.channelIndex_name, display.getChannelIndex());
      } catch (Exception e) {
        e.printStackTrace();
      }
  }
}

class ImageChannelSelection extends DataSelectionComponent {
   PreviewSelection previewSelection;
   ChannelSelection channelSelection;

   ImageChannelSelection(PreviewSelection previewSelection, ChannelSelection channelSelection) {
     super("MultiSpectral");
     this.previewSelection = previewSelection;
     this.channelSelection = channelSelection;
   }

   protected JComponent doMakeContents() {
      JSplitPane splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
      splitpane.add(previewSelection.doMakeContents());
      splitpane.add(channelSelection.doMakeContents());
      splitpane.setContinuousLayout(true);
      splitpane.setOneTouchExpandable(true);
      splitpane.setResizeWeight(1);
      splitpane.setDividerSize(12);
      return splitpane;
   }

   public void applyToDataSelection(DataSelection dataSelection) {
     previewSelection.applyToDataSelection(dataSelection);
     channelSelection.applyToDataSelection(dataSelection);
   }


}
