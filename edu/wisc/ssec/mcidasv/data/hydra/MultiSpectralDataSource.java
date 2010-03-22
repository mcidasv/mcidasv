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

package edu.wisc.ssec.mcidasv.data.hydra;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.data.PreviewSelection;

import edu.wisc.ssec.mcidasv.data.HydraDataSource;
import edu.wisc.ssec.mcidasv.data.ComboDataChoice;
import edu.wisc.ssec.mcidasv.control.LambertAEA;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataDataChoice;
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
import java.awt.FlowLayout;
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
public class MultiSpectralDataSource extends HydraDataSource {

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
    private boolean hasBandNames = false;

    private ComboDataChoice comboChoice;

    private PreviewSelection previewSelection = null;
    private FlatField previewImage = null;

    public static final String paramKey = "paramKey";

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    public MultiSpectralDataSource() {}

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

        try {
          if (name.startsWith("NSS.HRPT.NP") && name.endsWith("obs.hdf")) { // get file union
            reader = NetCDFFile.makeUnion(filename);
          }
          else {
            reader = new NetCDFFile(filename);
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
          //-hasImagePreview = true;
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

         swathAdapter = new SwathAdapter(reader, table);
         HashMap subset = swathAdapter.getDefaultSubset();
         subset.put(SpectrumAdapter.channelIndex_name, new double[] {10,10,1});
         defaultSubset = subset;
         double[] coords = (double[]) defaultSubset.get("Track");
         coords[2] = 10;
         coords = (double[]) defaultSubset.get("XTrack");
         coords[2] = 10;


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
         spectrumAdapter.setRangeProcessor(swathAdapter.getRangeProcessor());

         multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter, "MODIS", "Aqua");
         multiSpectData.init_wavenumber = 11.0f;
         multiSpectData.init_bandName = multiSpectData.getBandNameFromWaveNumber(multiSpectData.init_wavenumber);
         previewImage = multiSpectData.getImage(multiSpectData.init_wavenumber, defaultSubset);
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
         swathAdapter = sadapt0;
         subset = sadapt0.getDefaultSubset();
         subset.put(SpectrumAdapter.channelIndex_name, new double[] {1,1,1});
         defaultSubset = subset;
         coords = (double[]) defaultSubset.get("Track");
         coords[2] = 10;
         coords = (double[]) defaultSubset.get("XTrack");
         coords[2] = 10;


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
         specadap0.setRangeProcessor(sadapt0.getRangeProcessor());
         MultiSpectralData multispec0 = new MultiSpectralData(sadapt0, specadap0, "Reflectance", "Reflectance", "MODIS", "Aqua");

         DataCategory.createCategory("MultiSpectral");
         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
         hasImagePreview = true;
         hasChannelSelect = true;
         hasBandNames = true;

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
         specadap1.setRangeProcessor(sadapt1.getRangeProcessor());
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
         specadap2.setRangeProcessor(sadapt2.getRangeProcessor());
         MultiSpectralData multispec2 = new MultiSpectralData(sadapt2, specadap2, "Reflectance", "Reflectance", "MODIS", "Aqua");

         MultiSpectralAggr aggr = new MultiSpectralAggr(new MultiSpectralData[] {multispec1, multispec2, multispec0});
         aggr.init_wavenumber = 0.650f;
         aggr.init_bandName = aggr.getBandNameFromWaveNumber(aggr.init_wavenumber); 
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
         HashMap subset = swathAdapter.getDefaultSubset();
         subset.put(SpectrumAdapter.channelIndex_name, new double[] {1,1,1});
         defaultSubset = subset;
         double[] coords = (double[]) defaultSubset.get("Track");
         coords[2] = 40;
         coords = (double[]) defaultSubset.get("XTrack");
         coords[2] = 40;

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
         spectrumAdapter.setRangeProcessor(swathAdapter.getRangeProcessor());

         multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter, "Reflectance", "Reflectance", "MODIS", "Aqua");
         multiSpectData.init_wavenumber = 0.650f;
         multiSpectData.init_bandName = multiSpectData.getBandNameFromWaveNumber(multiSpectData.init_wavenumber);
         previewImage = multiSpectData.getImage(multiSpectData.init_wavenumber, defaultSubset);
         multiSpectData_s.add(multiSpectData);

         DataCategory.createCategory("MultiSpectral");
         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
         hasImagePreview = true;
         hasChannelSelect = true;
         hasBandNames = true;
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
         HashMap subset = swathAdapter0.getDefaultSubset();
         subset.put(SpectrumAdapter.channelIndex_name, new double[] {1,1,1});
         defaultSubset = subset;
         double[] coords = (double[]) defaultSubset.get("Track");
         coords[2] = 20;
         coords = (double[]) defaultSubset.get("XTrack");
         coords[2] = 20;

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
         spectrumAdapter0.setRangeProcessor(swathAdapter0.getRangeProcessor());

         MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "Reflectance", "Reflectance", "MODIS", "Aqua");
         multiSpectData0.init_wavenumber = 0.650f;
         multiSpectData0.init_bandName = multiSpectData0.getBandNameFromWaveNumber(multiSpectData0.init_wavenumber);
         previewImage = multiSpectData0.getImage(multiSpectData0.init_wavenumber, defaultSubset);

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
         subset = swathAdapter1.getDefaultSubset();
         subset.put(SpectrumAdapter.channelIndex_name, new double[] {1,1,1});
         defaultSubset = subset;
         coords = (double[]) defaultSubset.get("Track");
         coords[2] = 20;
         coords = (double[]) defaultSubset.get("XTrack");
         coords[2] = 20;

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
         spectrumAdapter1.setRangeProcessor(swathAdapter1.getRangeProcessor());

         MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "Reflectance", "Reflectance", "MODIS", "Aqua");
         multiSpectData1.init_wavenumber = 0.470f;
         multiSpectData1.init_bandName = multiSpectData1.getBandNameFromWaveNumber(multiSpectData1.init_wavenumber);

         MultiSpectralAggr aggr = 
            new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData0, multiSpectData1});
         aggr.init_wavenumber = 0.650f;
         aggr.init_bandName = aggr.getBandNameFromWaveNumber(aggr.init_wavenumber);
         aggr.setDataRange(new float[] {0f, 0.8f});
         multiSpectData_s.add(aggr);
         multiSpectData = aggr;

         DataCategory.createCategory("MultiSpectral");
         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
         hasImagePreview = true;
         hasChannelSelect = true;
         hasBandNames = true;
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

         SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
         HashMap subset = swathAdapter0.getDefaultSubset();
         defaultSubset = subset;
         double[] coords = (double[]) defaultSubset.get("Track");
         coords[2] = 10;
         coords = (double[]) defaultSubset.get("XTrack");
         coords[2] = 10;

         HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
         specTable.put(SpectrumAdapter.array_name, "ch3b_temperature");
         specTable.put(SpectrumAdapter.x_dim_name, "pixels_across_track");
         specTable.put(SpectrumAdapter.y_dim_name, "scan_lines_along_track");
         specTable.put(SpectrumAdapter.channelValues, new float[] {3.740f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch3b"});
         specTable.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter0 = new SpectrumAdapter(reader, specTable);
         spectrumAdapter0.setRangeProcessor(swathAdapter0.getRangeProcessor());

         MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "BrightnessTemp", "BrightnessTemp", null, null);
         multiSpectData0.init_wavenumber = 3.740f;
         multiSpectData0.init_bandName = multiSpectData0.getBandNameFromWaveNumber(multiSpectData0.init_wavenumber);
         previewImage = multiSpectData0.getImage(multiSpectData0.init_wavenumber, defaultSubset);

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


         SwathAdapter swathAdapter1 = new SwathAdapter(reader, table);
         subset = swathAdapter0.getDefaultSubset();
         defaultSubset = subset;
         coords = (double[]) defaultSubset.get("Track");
         coords[2] = 10;
         coords = (double[]) defaultSubset.get("XTrack");
         coords[2] = 10;

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "ch4_temperature");
         table.put(SpectrumAdapter.x_dim_name, "pixels_across_track");
         table.put(SpectrumAdapter.y_dim_name, "scan_lines_along_track");
         table.put(SpectrumAdapter.channelValues, new float[] {10.80f});
         table.put(SpectrumAdapter.bandNames, new String[] {"ch4"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter1 = new SpectrumAdapter(reader, table);
         spectrumAdapter1.setRangeProcessor(swathAdapter1.getRangeProcessor());

         MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "BrightnessTemp", "BrightnessTemp", null, null);
         multiSpectData1.init_wavenumber = 10.80f;
         multiSpectData1.init_bandName = multiSpectData1.getBandNameFromWaveNumber(multiSpectData1.init_wavenumber);

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


         SwathAdapter swathAdapter2 = new SwathAdapter(reader, table);
         subset = swathAdapter2.getDefaultSubset();
         defaultSubset = subset;
         coords = (double[]) defaultSubset.get("Track");
         coords[2] = 10;
         coords = (double[]) defaultSubset.get("XTrack");
         coords[2] = 10;

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "ch5_temperature");
         table.put(SpectrumAdapter.x_dim_name, "pixels_across_track");
         table.put(SpectrumAdapter.y_dim_name, "scan_lines_along_track");
         table.put(SpectrumAdapter.channelValues, new float[] {12.00f});
         table.put(SpectrumAdapter.bandNames, new String[] {"ch5"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter2 = new SpectrumAdapter(reader, table);
         spectrumAdapter2.setRangeProcessor(swathAdapter2.getRangeProcessor());

         MultiSpectralData multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "BrightnessTemp", "BrightnessTemp", null, null);
         multiSpectData2.init_wavenumber = 12.00f;
         multiSpectData2.init_bandName = multiSpectData2.getBandNameFromWaveNumber(multiSpectData2.init_wavenumber);


         MultiSpectralAggr aggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData0, multiSpectData1, multiSpectData2});
         aggr.init_wavenumber = 3.740f;
         aggr.init_bandName = aggr.getBandNameFromWaveNumber(aggr.init_wavenumber);
         aggr.setDataRange(new float[] {180f, 340f});
         multiSpectData = aggr;
         multiSpectData_s.add(aggr);

         //- now do the reflective bands
         swthTable.put("array_name", "ch1_reflectance");
         swthTable.put("range_name", "Reflective_Bands");

         swathAdapter0 = new SwathAdapter(reader, swthTable);
         subset = swathAdapter0.getDefaultSubset();
         defaultSubset = subset;
         coords = (double[]) defaultSubset.get("Track");
         coords[2] = 10;
         coords = (double[]) defaultSubset.get("XTrack");
         coords[2] = 10;

         specTable.put(SpectrumAdapter.array_name, "ch1_reflectance");
         specTable.put(SpectrumAdapter.channelValues, new float[] {0.630f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch1"});
         spectrumAdapter0 = new SpectrumAdapter(reader, specTable);
         spectrumAdapter0.setRangeProcessor(swathAdapter0.getRangeProcessor());

         multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "Reflectance", "Reflectance", null, null);
         multiSpectData0.init_wavenumber = 0.630f;
         multiSpectData0.init_bandName = multiSpectData0.getBandNameFromWaveNumber(multiSpectData0.init_wavenumber);

         swthTable.put("array_name", "ch2_reflectance");
         swthTable.put("range_name", "Reflective_Bands");
         
         swathAdapter1 = new SwathAdapter(reader, swthTable);
         subset = swathAdapter1.getDefaultSubset();
         defaultSubset = subset;
         coords = (double[]) defaultSubset.get("Track");
         coords[2] = 10;
         coords = (double[]) defaultSubset.get("XTrack");
         coords[2] = 10;
         
         specTable.put(SpectrumAdapter.array_name, "ch2_reflectance");
         specTable.put(SpectrumAdapter.channelValues, new float[] {0.862f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch2"});
         spectrumAdapter1 = new SpectrumAdapter(reader, specTable);
         spectrumAdapter1.setRangeProcessor(swathAdapter1.getRangeProcessor());

         multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "Reflectance", "Reflectance", null, null);
         multiSpectData1.init_wavenumber = 0.862f;
         multiSpectData1.init_bandName = multiSpectData1.getBandNameFromWaveNumber(multiSpectData1.init_wavenumber);

         swthTable.put("array_name", "ch3ab_reflectance");
         swthTable.put("range_name", "Reflective_Bands");
         
         swathAdapter2 = new SwathAdapter(reader, swthTable);
         subset = swathAdapter2.getDefaultSubset();
         defaultSubset = subset;
         coords = (double[]) defaultSubset.get("Track");
         coords[2] = 10;
         coords = (double[]) defaultSubset.get("XTrack");
         coords[2] = 10;
         
         specTable.put(SpectrumAdapter.array_name, "ch3ab_reflectance");
         specTable.put(SpectrumAdapter.channelValues, new float[] {1.610f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch3ab"});
         spectrumAdapter2 = new SpectrumAdapter(reader, specTable);
         spectrumAdapter2.setRangeProcessor(swathAdapter2.getRangeProcessor());

         multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "Reflectance", "Reflectance", null, null);
         multiSpectData2.init_wavenumber = 1.610f;
         multiSpectData2.init_bandName = multiSpectData2.getBandNameFromWaveNumber(multiSpectData2.init_wavenumber);

         aggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData0, multiSpectData1, multiSpectData2});
         aggr.init_wavenumber = 0.630f;
         aggr.init_bandName = aggr.getBandNameFromWaveNumber(aggr.init_wavenumber);
         aggr.setDataRange(new float[] {0f, 100f});
         multiSpectData_s.add(aggr);


         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");

         hasImagePreview = true;
         hasChannelSelect = true;
         hasBandNames = true;
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
        combo.setDataSource(this);
        addDataChoice(combo);
        getDataContext().dataSourceChanged(this);
    }

    private DataChoice doMakeDataChoice(int idx, MultiSpectralData adapter) throws Exception {
        String name = adapter.getName();
        DataSelection dataSel = new MultiDimensionSubset(defaultSubset);
        Hashtable subset = new Hashtable();
        subset.put(MultiDimensionSubset.key, dataSel);
        subset.put(MultiSpectralDataSource.paramKey, adapter.getParameter());
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
                  if ((props != null) && (props.get(MultiDimensionSubset.key) != null)) {
                    subset = (HashMap) ((MultiDimensionSubset)props.get(MultiDimensionSubset.key)).getSubset();
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
              data = multiSpectData.getImage(subset);
              data = applyProperties(data, requestProperties, subset);
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
          components.add(new ImageChannelSelection(new PreviewSelection(dataChoice, previewImage, null), new ChannelSelection2(dataChoice)));
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
          components.add(new ChannelSelection2(dataChoice));
        } 
        catch (Exception e) {
          e.printStackTrace();
        }
      }
      }
    }
}


class ChannelSelection2 extends DataSelectionComponent {

  DataChoice dataChoice;
  MultiSpectralDisplay display;

  ChannelSelection2(DataChoice dataChoice) throws Exception {
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
   ChannelSelection2 channelSelection;

   ImageChannelSelection(PreviewSelection previewSelection, ChannelSelection2 channelSelection) {
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
