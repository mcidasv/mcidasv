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
import java.util.ArrayList;
import java.util.Iterator;

public class DataSourceFactory {
   
   /* DataSources currently created by the application */
   private static final ArrayList<DataSource> dataSourceList = new ArrayList<>();
   
   /* DataSources supported by this Factory */
   private final ArrayList<String> dataSourceClassList;
   
   public DataSourceFactory() {
      dataSourceClassList = new ArrayList();
      
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.ABIDirectory");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.AHIDirectory");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.GEOSDataSource");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.VIIRSDataSource");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.CMIP_MultiBand_DataSource");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.NOAA_SNPP_DataSource");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.NOAA_VIIRS_Products_DataSource");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.AIRSv1_SoundingDataSource");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.AIRSv2_SoundingDataSource");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.IASI_SoundingDataSource");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.CrIS_SoundingDataSource");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.NASA_CrIS_DataSource");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.MultiDimensionDataSource");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.MultiSpectralDataSource");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.SIPS_VIIRS_SVM");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.SIPS_VIIRS_SVI");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.SIPS_VIIRS_DNB");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.SIPS_VIIRS_FSN");
      dataSourceClassList.add("edu.wisc.ssec.hydra.data.CLAVRX_VIIRS_DataSource");
   }
   
   public DataSource createDataSource(File[] files) throws Exception {
        DataSource dataSource = null;
        
        ArrayList<File> fal = new ArrayList();
        for (int k=0; k<files.length; k++) {
          if (files[k] == null) {
             throw new Exception("files can't be null");
          }
          fal.add(files[k]);
        }
        
        Iterator<String> iter = dataSourceClassList.iterator();
        while (iter.hasNext()) {
           Class ds = Class.forName(iter.next());
           try {
              dataSource = (DataSource) ds.getConstructor(new Class[] {ArrayList.class}).newInstance(fal);
              break;
           }
           catch (Exception e) {
           }
        }
        if (dataSource == null) {
           throw new Exception("No suitable DataSource fournd for: "+files[0]);
        }

        synchronized(dataSourceList) {
           dataSourceList.add(dataSource);
        }
        return dataSource;      
   }
   
   public DataSource createDataSource(File dir) throws Exception {
        Iterator<String> iter = dataSourceClassList.iterator();
        DataSource dataSource = null;
        while (iter.hasNext()) {
           Class ds = Class.forName(iter.next());
           try {
              dataSource = (DataSource) ds.getConstructor(new Class[] {File.class}).newInstance(dir);
              break;
           }
           catch (Exception e) {
           }
        }
        if (dataSource == null) {
           throw new Exception("No suitable DataSource fournd for: "+dir);
        }
        synchronized(dataSourceList) {
           dataSourceList.add(dataSource);
        }
        return dataSource;      
   }
   
   public static void removeDataSource(DataSource dataSource) {
      synchronized(dataSourceList) {
         dataSourceList.remove(dataSource);
      }
   }
   
   public static ArrayList<DataSource> getDataSourcesByDescription(String description) {
      ArrayList<DataSource> list = new ArrayList();
      
      synchronized(dataSourceList) {
         Iterator iter = dataSourceList.iterator();
         while (iter.hasNext()) {
            DataSource ds = (DataSource) iter.next();
            if (ds.getDescription().equals(description)) {
               list.add(ds);
            }
         }
      }
      
      return list;
   }
   
   public static ArrayList<DataSource> getDataSources() {
      ArrayList<DataSource> list = new ArrayList();
     
      synchronized(dataSourceList) {
         Iterator iter = dataSourceList.iterator();
         while (iter.hasNext()) {
            list.add((DataSource)iter.next());
         }
      }
      
      return list;
   }
   
}
