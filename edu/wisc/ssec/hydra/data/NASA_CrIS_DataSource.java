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
 * McIDAS-V is built on Unidata's IDV, as well as SSEC's VisAD and HYDRA
 * projects. Parts of McIDAS-V source code are based on IDV, VisAD, and
 * HYDRA source code.
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
import edu.wisc.ssec.adapter.SpectrumAdapter;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.GranuleAggregation;
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.RangeProcessor;
import edu.wisc.ssec.adapter.AggregationRangeProcessor;
import edu.wisc.ssec.adapter.CrIS_NASA_Spectrum;
import edu.wisc.ssec.adapter.CrIS_SDR_MultiSpectralData;
import edu.wisc.ssec.adapter.CrIS_SDR_SwathAdapter;
import edu.wisc.ssec.adapter.MultiDimensionAdapter;
import edu.wisc.ssec.adapter.MultiSpectralAggr;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.hydra.Hydra;
import java.rmi.RemoteException;
import java.util.Date;
import ucar.unidata.util.ColorTable;
import visad.Data;
import visad.VisADException;


public class NASA_CrIS_DataSource extends DataSource {

   String dateTimeStamp = null;
   
   Date dateTime;

   String description = null;
   
   float nadirResolution;

   File[] files = null;

   String[] bandNames = null;
   float[] centerWavelength = null;
   
   private ArrayList<MultiSpectralData> msd_CrIS = new ArrayList<>();
   private ArrayList<MultiSpectralData> multiSpectralData = new ArrayList<>();
   private HashMap<String, MultiSpectralData> msdMap = new HashMap<>();
   

   public NASA_CrIS_DataSource(File directory) throws Exception {
     this(directory.listFiles());
   }
   
   public NASA_CrIS_DataSource(ArrayList<File> files) throws Exception {
      this(files.toArray(new File[] {null}));
   }

   public NASA_CrIS_DataSource(File[] files) throws Exception {
      if (!canUnderstand(files)) {
         throw new Exception("NOAA_SNPP_DataSource doesn't understand input files");
      }

      ArrayList<File> dataList = new <File>ArrayList();
      for (int i=0; i<files.length; i++) {
         String fname = files[i].getName();
         if (fname.startsWith("SNDR.SNPP.CRIS") ||
             fname.contains("SNDR.J1.CRIS") ||
             fname.contains("SNDR.J2.CRIS")) {
            dataList.add(files[i]);
         }
      }
           
      ArrayList<String> sortedList = DataSource.getTimeSortedFilenameList(dataList);
      for (int i=0; i<sortedList.size(); i++) {
         files[i] = new File(sortedList.get(i));
      }
     
      File file = files[0];
      String name = file.getName();
           
      dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName());
      description = DataSource.getDescriptionFromFilename(file.getName());
      dateTime = DataSource.getDateTimeFromFilename(file.getName());
      
      try {
         init(files, null);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   public boolean canUnderstand(File[] files) {
      if ( files[0].getName().startsWith("SNDR.SNPP.CRIS") ||
           files[0].getName().startsWith("SNDR.J1.CRIS") ||
           files[0].getName().startsWith("SNDR.J2.CRIS")) {
         return true;
      }
      return false;
   }
   
   void init(File[] files, File[] geoFiles) throws Exception {
      int numGrans = files.length;
      
      ArrayList<NetCDFFile> ncdfal = new ArrayList<>();
      for (int k=0; k<files.length; k++) {
         ncdfal.add(new NetCDFFile(files[k].getAbsolutePath()));
      }
      GranuleAggregation aggReader = new GranuleAggregation(ncdfal, "atrack");
            
      String name = files[0].getName();
      String[] strs = name.split("_");
      String prodStr = strs[0];
      
      MultiSpectralData msd = null;
      String[] productPaths;
      String geoDatasetPath = "./";

      if (prodStr.contains("SNDR.SNPP.CRIS") ||
          prodStr.contains("SNDR.J1.CRIS") ||
          prodStr.contains("SNDR.J2.CRIS")) {
         prodStr = "CrIS_SNDR";
         String path = getProductName(prodStr);
         productPaths = new String[] {"rad_lw", "rad_mw", "rad_sw"};
         msd = buildCrIS(aggReader, aggReader, productPaths, geoDatasetPath);
         nadirResolution = 14000;
      }
      else {
         throw new Exception("unknown product: "+prodStr);
      }
      
      multiSpectralData.add(msd);      
      
      DataChoice choice = setDataChoice(msd, 0, "BrightnessTemperature");
      
      msdMap.put(choice.getName(), msd);
   }
   
   RangeProcessor buildRangeProcessor(GranuleAggregation aggReader, ArrayList<NetCDFFile> ncdfal, String prodStr, HashMap metadata) throws Exception {
      int numGrans = ncdfal.size();
      RangeProcessor[] rngProcessors = new RangeProcessor[numGrans];
      float scale = 1f;
      float offset = 0f;
      for (int k=0; k<numGrans; k++) {
         NetCDFFile ncFile = ncdfal.get(k);
         String scaleFactorName = getScaleFactorName(prodStr);
         if (scaleFactorName != null) {
            int[] dimLens = ncFile.getDimensionLengths(scaleFactorName);
            float[] fltArray = ncFile.getFloatArray(scaleFactorName, new int[] {0}, new int[] {dimLens[0]}, new int[] {1});
            scale = fltArray[0];
            offset = fltArray[1];
         }
         float[] range = getValidRange(prodStr);
         double[] missing = getMissing(prodStr);
         RangeProcessor rngProcessor = new RangeProcessor(ncFile, metadata, scale, offset, range[0], range[1], missing);
         rngProcessors[k] = rngProcessor;
      }
      
      AggregationRangeProcessor aggRngProcessor = new AggregationRangeProcessor(rngProcessors);
      aggReader.addPreProcessor((String)metadata.get(SwathAdapter.array_name), aggRngProcessor);
      return aggRngProcessor;
   }
      
   MultiSpectralData buildCrIS(GranuleAggregation reader, GranuleAggregation geoReader, String[] productPaths, String geoDatasetPath) throws Exception {
      boolean unsigned = false;
      boolean unpack = false;
      boolean range_check_after_scaling = false;
      
      MultiSpectralData aggrMSDs;
      String[] channelIndex_names = {"wnum_lw", "wnum_mw", "wnum_sw"};
      String[] channelNames = {"wnum_lw", "wnum_mw", "wnum_sw"};
      
      for (int k=0; k<productPaths.length; k++) {
         String productPath = productPaths[k];
      
         HashMap metadata = fillSwathMetadataTable(
                 "xtrack",
                 "atrack",
                 channelIndex_names[k],
                 "fov",
                 productPath,
                 "radiances",
                 "xtrack",
                 "atrack",
                 "lon",
                 "lat",
                 null,
                 null,
                 null,
                 unsigned, unpack, range_check_after_scaling, "CrIS_SNDR"
         );

         HashMap<String, Object> spectTable = SpectrumAdapter.getEmptyMetadataTable();
         spectTable.put("array_name", productPaths[k]);
         spectTable.put("product_name", "CrIS_NASA");
         spectTable.put(SpectrumAdapter.channelIndex_name, channelIndex_names[k]);
         spectTable.put(SpectrumAdapter.channelType, "channel_number");
         spectTable.put(SpectrumAdapter.channels_name, channelNames[k]);
         spectTable.put(SpectrumAdapter.x_dim_name, "xtrack");
         spectTable.put(SpectrumAdapter.y_dim_name, "atrack");  
         spectTable.put(SpectrumAdapter.FOVindex_name, "fov");

         float scale = 1f;
         float offset = 0f;
         
         float[] range = getValidRange("SCRIS");
         double[] missing = getMissing("SCRIS");
         RangeProcessor rngProcessor = new RangeProcessor(reader, metadata, scale, offset, range[0], range[1], missing);
         AggregationRangeProcessor aggrRangeProcessor = new AggregationRangeProcessor(reader, rngProcessor);
         reader.addPreProcessor(productPath, aggrRangeProcessor);

         CrIS_SDR_SwathAdapter adapter = new CrIS_SDR_SwathAdapter(reader, metadata, geoReader);
         CrIS_NASA_Spectrum csa = new CrIS_NASA_Spectrum(reader, spectTable);
         MultiSpectralData msd = new CrIS_SDR_MultiSpectralData(adapter, csa);
         msd_CrIS.add(msd);
      }
      
      aggrMSDs = new MultiSpectralAggr(msd_CrIS.toArray(new MultiSpectralData[msd_CrIS.size()]), "radiances");
      aggrMSDs.setInitialWavenumber(902.25f);
      
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
   
   public boolean isMultiSpectral() {
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
                                throws VisADException, RemoteException 
    {
      try {
         MultiDimensionAdapter adapter = getMultiSpectralData(dataChoice);
         
         
         MultiDimensionSubset select = (MultiDimensionSubset) dataChoice.getDataSelection();
         HashMap subset = select.getSubset();
         
         Data data = adapter.getData(subset);
         return data;
      } catch (Exception e) {
         e.printStackTrace();
      }
      return null;
    }
    
    public static String getProductName(String prodStr) {
       String name = null;
       switch (prodStr) {
          case "SATMS":
             name = "All_Data/ATMS-SDR_All/BrightnessTemperature";
             break;
          case "SCRIS":
             name = "All_Data/CrIS-SDR_All/";
             break;
       }
       return name;
    }    
    
    public static String getScaleFactorName(String prodStr) {
       String name = null;
       switch (prodStr) {
          case "SATMS":
             name = "All_Data/ATMS-SDR_All/BrightnessTemperatureFactors";
             break;
       }
       return name;
    }
    
    public static double[] getMissing(String prodStr) throws Exception {
       float[] missing;
       switch (prodStr) {
          case "SCRIS":
             missing = getMissingCrIS();
             break;
          default:
             throw new Exception("Unrecognized product string");
       }
       double[] dblArray = new double[missing.length];
       for (int i=0; i<dblArray.length; i++) dblArray[i] = missing[i];
       return dblArray;
    }
        
    public static float[] getMissingCrIS() {
       float[] missing = new float[] {
          -999.9f,
          -999.8f,
          -999.5f,
          -999.3f
       };
       return missing;       
    }
    
    public static float[] getValidRange(String prodStr) {
       float[] validRange = new float[] {-Float.MAX_VALUE, Float.MAX_VALUE};
       switch(prodStr) {
          case "SATMS":
             validRange[0] = 0f;
             validRange[1] = 330f;
             break;
       }
       return validRange;
    }
    
    public ColorTable getDefaultColorTable(DataChoice choice) {
       return Hydra.invGrayTable;
    }   

   @Override
   public float getNadirResolution(DataChoice choice) throws Exception {
      return nadirResolution;
   }

   public boolean hasMultiSpectralData() {
      return true;
   }
}
