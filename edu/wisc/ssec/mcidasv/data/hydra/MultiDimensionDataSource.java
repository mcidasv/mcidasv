package edu.wisc.ssec.mcidasv.data.hydra;

import edu.wisc.ssec.mcidasv.data.HydraDataSource;
import edu.wisc.ssec.mcidasv.data.hydra.ProfileAlongTrack;
import edu.wisc.ssec.mcidasv.data.hydra.ProfileAlongTrack3D;
import edu.wisc.ssec.mcidasv.data.hydra.Calipso2D;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DirectDataChoice;

import ucar.unidata.util.Misc;

import visad.Data;
import visad.VisADException;

import java.io.File;


/**
 * A data source for Multi Dimension Data 
 */
public class MultiDimensionDataSource extends HydraDataSource {

    /** Sources file */
    protected String filename;

    protected MultiDimensionReader reader;

    protected MultiDimensionAdapter[] adapters;

    protected SpectrumAdapter spectrumAdapter;


    private static final String DATA_DESCRIPTION = "Multi Dimension Data";


    public HashMap defaultSubset;
    public SwathAdapter swathAdapter;

    private MultiSpectralData multiSpectData;

    private List categories;


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

        setup();
    }

    public void setup() {
        try {
          if ( filename.endsWith(".hdf") ) {
            //reader = new HDF4File(filename);
            reader = new NetCDFFile(filename);
          }
        }
        catch (Exception e) {
          e.printStackTrace();
          System.out.println("cannot create HDF4 reader on file:"+filename);
        }
                                                                                                                                                     
        try {
          if ( filename.endsWith(".nc") ) {
            reader = new NetCDFFile(filename);
          }
        }
        catch (Exception e) {
          e.printStackTrace();
          System.out.println("cannot create NetCDF reader on file: "+filename);
        }
                                                                                                                                                     
        adapters = new MultiDimensionAdapter[2];
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
          adapters[0] = new SwathAdapter(reader, table);
          categories = DataCategory.parseCategories("2D grid;GRID-2D;");
          defaultSubset = adapters[0].getDefaultSubset();
        }
        else if ( name.startsWith("AIRS")) {
          HashMap table = SpectrumAdapter.getEmptyMetadataTable();
          table.put(SpectrumAdapter.array_name, "radiances");
          table.put(SpectrumAdapter.channelIndex_name, "Channel:L1B_AIRS_Science");
          table.put(SpectrumAdapter.ancillary_file_name, "/home/rink/devel/Hydra/ancillary/airs/L2.chan_prop.2003.11.19.v6.6.9.anc");
          table.put(SpectrumAdapter.x_dim_name, "GeoXTrack:L1B_AIRS_Science");
          table.put(SpectrumAdapter.y_dim_name, "GeoTrack:L1B_AIRS_Science");
          adapters[1] = new AIRS_L1B_Spectrum(reader, table);
                                                                                                                                                     
          table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "radiances");
          table.put("lon_array_name", "Longitude");
          table.put("lat_array_name", "Latitude");
          table.put("XTrack", "GeoXTrack:L1B_AIRS_Science");
          table.put("Track", "GeoTrack:L1B_AIRS_Science");
          table.put("geo_Track", "GeoTrack:L1B_AIRS_Science");
          table.put("geo_XTrack", "GeoXTrack:L1B_AIRS_Science");
          table.put(SpectrumAdapter.channelIndex_name, "Channel:L1B_AIRS_Science"); //- think about this?
          swathAdapter = new SwathAdapter(reader, table);
          HashMap subset = swathAdapter.getDefaultSubset();
          subset.put(SpectrumAdapter.channelIndex_name, new double[] {793,793,1});
          defaultSubset = subset;
          multiSpectData = new MultiSpectralData(swathAdapter, (SpectrumAdapter)adapters[1]);
          adapters[0] = swathAdapter;
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;");
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
          table.put("geo_Track", "obsElement");
          table.put("geo_XTrack", "obsLine");
          table.put(SpectrumAdapter.channelIndex_name, "obsChannelIndex"); //- think about this?
          swathAdapter = new SwathAdapter(reader, table);
          HashMap subset = swathAdapter.getDefaultSubset();
          subset.put(SpectrumAdapter.channelIndex_name, new double[] {793,793,1});
          defaultSubset = subset;
          multiSpectData = new MultiSpectralData(swathAdapter, (SpectrumAdapter)adapters[1]);
          adapters[0] = swathAdapter;
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;");
       }
       else if (name.startsWith("CAL_LID_L1")) {
         HashMap table = ProfileAlongTrack.getEmptyMetadataTable();
         table.put(ProfileAlongTrack.array_name, "Total_Attenuated_Backscatter_532");
         table.put(ProfileAlongTrack.ancillary_file_name, System.getProperty("user.dir")+"/ancillary/lidar/altitude");
         //-table.put(ProfileAlongTrack.trackDim_name, "fakeDim38");
         //-table.put(ProfileAlongTrack.vertDim_name, "fakeDim39");
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
       }
       else {
          HashMap table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "brightness_temperature");
          table.put("lon_array_name", "longitude");
          table.put("lat_array_name", "latitude");
          table.put("XTrack", "field");
          table.put("Track", "scan");
          table.put("geo_Track", "scan");
          table.put("geo_XTrack", "field");
          adapters[0] = new SwathAdapter(reader, table);
          categories = DataCategory.parseCategories("2D grid;GRID-2D;");
          defaultSubset = adapters[0].getDefaultSubset();
       }
    }

    public void initAfterUnpersistence() {
      setup();
    }

    /**
     * Make and insert the <code>DataChoice</code>-s for this
     * <code>DataSource</code>.
     */
    public void doMakeDataChoices() {
        DataChoice choice = null;
        //for (int idx=0; idx<adapters.length; idx++) {
        for (int idx=0; idx<1; idx++) {
            try {
              choice = doMakeDataChoice(idx, adapters[idx].getArrayName());
            } catch (Exception e) 
            {
              e.printStackTrace();
              System.out.println("doMakeDataChoice failed");
            }
            addDataChoice(choice);
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

    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {
        /*
        System.out.println("MultiDimensionDataSource getDataInner:");
        System.out.println("   dataChoice=" + dataChoice);
        System.out.println("   category=" + category);
        System.out.println("   dataSelection=" + dataSelection);
        System.out.println("   requestProperties=" + requestProperties);
        */

        Data data = null;
        try {
            MultiDimensionSubset select = (MultiDimensionSubset) dataChoice.getDataSelection();
            data = adapters[0].getData(select.getSubset());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("getData exception e=" + e);
        }
        return data;
    }
}

