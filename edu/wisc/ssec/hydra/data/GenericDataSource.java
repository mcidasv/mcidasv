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
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import visad.VisADException;
import visad.Data;
import java.rmi.RemoteException;
import java.util.Date;


public class GenericDataSource extends DataSource {

   String dateTimeStamp = null;
   
   Date dateTime = null;

   String description = null;

   File[] files = null;

   HashMap<DataChoice, SwathAdapter> dataChoiceToAdapter = new HashMap<DataChoice, SwathAdapter>();

   HashMap<String, DimensionSet>  arrayNameToDims = new HashMap<String, DimensionSet>();

   ArrayList<FieldInfo> swathFields = new ArrayList<FieldInfo>();
   

   public GenericDataSource(File directory) {
     this(directory.listFiles());
   }

   public GenericDataSource(File[] files) {

      this.files = files;
      int numFiles = files.length;
      File file = files[0];
      try { 
        init(file);
      } catch (Exception e) {
        e.printStackTrace();
      }
   }

   void init(File file) throws Exception {
      NetCDFFile reader = new NetCDFFile(file.getAbsolutePath());

      String name = file.getName();
      dateTimeStamp = DataSource.getDateTimeStampFromFilename(name);
      dateTime = DataSource.getDateTimeFromFilename(name);
      description = DataSource.getDescriptionFromFilename(name);

      if (name.startsWith("NPR-MIRS") && name.endsWith(".nc")) {
         DimensionSet dimSet = new DimensionSet("Scanline", "Field_of_view", null, null, "Scanline", "Field_of_view");
         String[] arrayNames = new String[] {"RR", "TPW", "CldTop", "CldBase"};
         String[] rangeNames = new String[] {"RainRate", "TPW", "CloudTopPress", "CloudBasePress"};

         for (int k=0; k<arrayNames.length; k++) {
            SwathInfo sInfo = new SwathInfo();
            sInfo.track = dimSet.track;
            sInfo.xtrack = dimSet.xtrack;
            sInfo.geo_track = dimSet.geo_track;
            sInfo.geo_xtrack = dimSet.geo_xtrack;
            sInfo.lonArrayName = "Longitude";
            sInfo.latArrayName = "Latitude";

            FieldInfo fInfo = new FieldInfo();
            fInfo.dimSet = dimSet;
            fInfo.arrayName = arrayNames[k];
            fInfo.rangeName = rangeNames[k];
            fInfo.scaleName = "scale";
            fInfo.divideByScale = true;

            fInfo.swathInfo = sInfo;
          
            swathFields.add(fInfo);
         }
      }


      for (int k=0; k<swathFields.size(); k++) {
         FieldInfo fInfo = swathFields.get(k);
         SwathAdapter swathAdapter = buildSwathAdapter(reader, fInfo);

         HashMap subset = swathAdapter.getDefaultSubset();
         MultiDimensionSubset dataSel = new MultiDimensionSubset(subset);
         DataChoice dataChoice = new DataChoice(this, fInfo.rangeName, null);
         dataChoice.setDataSelection(dataSel);
         myDataChoices.add(dataChoice);
         dataChoiceToAdapter.put(dataChoice, swathAdapter);
      }
      
   }

   public SwathAdapter buildSwathAdapter(NetCDFFile reader, FieldInfo swathField) {
      HashMap metadata = SwathAdapter.getEmptyMetadataTable();

      SwathInfo swthInfo = swathField.swathInfo;

      metadata.put(SwathAdapter.array_name, swathField.arrayName);
      metadata.put(SwathAdapter.scale_name, swathField.scaleName);
      metadata.put(SwathAdapter.range_name, swathField.rangeName);
      metadata.put(SwathAdapter.xtrack_name, swthInfo.xtrack);
      metadata.put(SwathAdapter.track_name, swthInfo.track);
      metadata.put(SwathAdapter.geo_xtrack_name, swthInfo.geo_xtrack);
      metadata.put(SwathAdapter.geo_track_name, swthInfo.geo_track);
      metadata.put(SwathAdapter.lon_array_name, swthInfo.lonArrayName);
      metadata.put(SwathAdapter.lat_array_name, swthInfo.latArrayName);
      if ((swthInfo.lonDimNames != null && swthInfo.latDimNames != null)) {
         metadata.put(SwathAdapter.lon_array_dimension_names, swthInfo.lonDimNames);
         metadata.put(SwathAdapter.lat_array_dimension_names, swthInfo.latDimNames);
      }

      if (swathField.divideByScale) {
         metadata.put("divideByScale", "divideByScale");
      }

      SwathAdapter swathAdapter = new SwathAdapter(reader, metadata);

      return swathAdapter;
   }

   public SwathAdapter buildSwathAdapter(NetCDFFile reader, String arrayName, String rangeName, String track, String xtrack,
                                         String latArray, String lonArray, String geoTrack, String geoXtrack,
                                         String[] latArrayDims, String[] lonArrayDims, String scaleName, boolean divideByScale) {

      HashMap metadata = SwathAdapter.getEmptyMetadataTable();

      metadata.put(SwathAdapter.array_name, arrayName);
      metadata.put(SwathAdapter.scale_name, scaleName);
      metadata.put(SwathAdapter.range_name, rangeName);
      metadata.put(SwathAdapter.xtrack_name, xtrack);
      metadata.put(SwathAdapter.track_name, track);
      metadata.put(SwathAdapter.geo_xtrack_name, geoXtrack);
      metadata.put(SwathAdapter.geo_track_name, geoTrack);
      metadata.put(SwathAdapter.lon_array_name, lonArray);
      metadata.put(SwathAdapter.lat_array_name, latArray);
      if ((lonArrayDims != null && latArrayDims != null)) {
         metadata.put(SwathAdapter.lon_array_dimension_names, lonArrayDims);
         metadata.put(SwathAdapter.lat_array_dimension_names, latArrayDims);
      }

      if (divideByScale) {
         metadata.put("divideByScale", "divideByScale");
      }

      SwathAdapter swathAdapter = new SwathAdapter(reader, metadata);

      return swathAdapter;
   }

   public SwathAdapter getSwathAdapter(DataChoice dataChoice) {
      return dataChoiceToAdapter.get(dataChoice);
   }

   @Override
   public String getDescription() {
     return description;
   }
   
   @Override
   public String getDateTimeStamp() {
      return dateTimeStamp;
   }
   
   public Date getDateTime() {
     return dateTime;
   }

   @Override
   public boolean getDoFilter(DataChoice choice) {
     return true;
   }
   
   @Override
   public boolean getOverlayAsMask(DataChoice choice) {
     return false;
   }
   
   public Data getData(DataChoice dataChoice, DataSelection dataSelection)
           throws VisADException, RemoteException 
   {
      try {
         SwathAdapter adapter = dataChoiceToAdapter.get(dataChoice);

         MultiDimensionSubset select = (MultiDimensionSubset) dataChoice.getDataSelection();
         HashMap subset = select.getSubset();

         return adapter.getData(subset);
      } catch (Exception e) {
         e.printStackTrace();
      }

      return null;
   }

   @Override
   public float getNadirResolution(DataChoice choice) throws Exception {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public boolean canUnderstand(File[] files) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

}

class DimensionSet {

   String track = null;
   String xtrack = null;
   String vertical = null;   // perpendicular to (track,xtrack), altitude or pressure coords
   String channel = null;    // non spatial, spectral dimension
   String other = null;      // non sptial, index dimension 
   String geo_track = null;
   String geo_xtrack = null;

   public DimensionSet(String track, String xtrack, String vertical, String other,
                       String geo_track, String geo_xtrack) {
      this.track = track;
      this.xtrack = xtrack;
      this.geo_track = geo_track;
      this.geo_xtrack = geo_xtrack;
   }
}

class SwathInfo {

   String track = null;
   String xtrack = null;
   String geo_track = null;
   String geo_xtrack = null;
   String lonArrayName = null;
   String latArrayName = null;
   String geo_scale = null;
   String geo_offset = null;
   String[] lonDimNames = null;
   String[] latDimNames = null;
}

class FieldInfo {

   DimensionSet dimSet = null;

   String arrayName = null;
   String rangeName = null;
   String scaleName = null;
   String offsetName = null;
   String multiScaleDimensionIndex = null;
   boolean divideByScale = false;
 
   SwathInfo swathInfo = null;
}
