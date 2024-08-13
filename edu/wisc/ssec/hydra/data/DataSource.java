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

import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.hydra.Hydra;
import ucar.unidata.util.Range;
import java.io.File;
import java.rmi.RemoteException;
import ucar.unidata.util.ColorTable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.sql.Timestamp;
import java.util.List;

import visad.*;

public abstract class DataSource {
   
   ArrayList<DataChoice> myDataChoices = new ArrayList<>();
   
   private int dataSourceId = 0;
   
   public DataSource() {
   }
   
   public int getDataSourceId() {
      return dataSourceId;
   }
   
   public void setDataSourceId(int uniqueId) {
      if (dataSourceId == 0 && uniqueId != 0) { // can't be changed once set
         dataSourceId = uniqueId;
      }
   }
   
   /**
    * Allows spatial Gaussian filtering during re-projecting
    * @param choice
    * @return 
    */
   public abstract boolean getDoFilter(DataChoice choice);
   
   /**
    * Ensures image will be rendered on top of other displayed image layers
    * 
    * @param choice
    * @return
    */
   public abstract boolean getOverlayAsMask(DataChoice choice);

   /**
    * Nadir resolution for this product
    * @param choice
    * @return
    * @throws Exception 
    */
   public abstract float getNadirResolution(DataChoice choice) throws Exception;
   
   /**
    * Provide a string representation of a nominal time for this DataSource
    * @return 
    */
   public String getDateTimeStamp() {
      return makeDateTimeStamp(getDateTime());
   }

   /**
    * Provide a Date representation of a nominal time for this DataSource
    * @return 
    */   
   public abstract Date getDateTime();
  
   /**
    * Provide a short description for this.
    * @return 
    */
   public abstract String getDescription();
   
   /**
    * Examine file(s) (name convention or possibly internals) to determine if this DataSource can import
    * @param files
    * @return 
    */
   public abstract boolean canUnderstand(File[] files);
  
   
   public String getDescription(DataChoice choice) {
      return null;
   }
   
   /**
    * Re-project these products when displaying.
    * @param choice
    * @return 
    */
   public boolean getDoReproject(DataChoice choice) {
     return true;
   }
  
   /**
    * Custom algo for a specific sensor
    * @param choice
    * @return 
    */
   public boolean getReduceBowtie(DataChoice choice) {
      return false;
   }

   /**
    * Optional name specification for sensor, eg. I-band, DNB, MODIS HKM, etc
    * @param choice
    * @return 
    */
   public String getSensorName(DataChoice choice) {
     return null;
   }
   
   /**
    * Force range for color mapping for this choice. Null means use full value range
    * @param choice
    * @return 
    */
   public Range getDefaultColorRange(DataChoice choice) {
      return null;
   }
 
   public static String getDescriptionFromFilename(String filename) {
     String desc = null;
     
     if (filename.contains("SEADAS") || filename.contains("seadas")) {
       desc = "SEADAS";
     }
     else if (filename.startsWith("MOD") || filename.startsWith("t1.")) {
       desc = "MODIS T";
     }
     else if (filename.startsWith("MYD") || filename.startsWith("a1.")) {
       desc = "MODIS A";
     }
     else if (filename.startsWith("SV") && (filename.contains("_npp_") || filename.contains("_j01_") || filename.contains("_j02_"))) {
       desc = "VIIRS";
     }
     else if (filename.startsWith("viirs_l1b")) {
       desc = "VIIRS";
     }
     else if (filename.startsWith("VNP") || filename.startsWith("VJ1") || filename.startsWith("VJ2")) {
       desc = "VIIRS";
       if (filename.contains("02FSN")) {
          desc = "VIIRS+MODIS";
       }
     }
     else if (filename.startsWith("SCRIS_npp") || filename.startsWith("GCRSO-SCRIS_npp") || filename.startsWith("GCRSO-SCRIF_npp")) {
       desc = "CrIS SDR";
     }
     else if (filename.startsWith("SCRIS_j01") || filename.startsWith("GCRSO-SCRIS_j01") || filename.startsWith("GCRSO-SCRIF_j01")) {
       desc = "CrIS SDR";
     }
     else if (filename.startsWith("SCRIS_j02") || filename.startsWith("GCRSO-SCRIS_j02") || filename.startsWith("GCRSO-SCRIF_j02")) {
       desc = "CrIS SDR";
     }     
     else if (filename.startsWith("SNDR.SNPP.CRIS") || filename.startsWith("SNDR.J1.CRIS") || filename.startsWith("SNDR.J2.CRIS")) {
       desc = "CrIS_SNDR";
     }
     else if (filename.startsWith("SATMS_npp") || filename.startsWith("GATMO-SATMS_npp")) {
       desc = "ATMS SDR";
     }
     else if (filename.contains("L1B.AIRS_Rad")) {
       desc = "AIRS L1B";
     }
     else if (filename.startsWith("IASI_xxx_1C")) {
       desc = "IASI L1C";
     }
     else if (filename.contains("IASI_C") && filename.endsWith(".nc")) {
       desc = "IASI L1C";
     }
     else if (filename.startsWith("iasil1c") && filename.endsWith(".h5")) {
       desc = "IASI L1C";
     } 
     else if (filename.contains("AVHR_C") && filename.endsWith(".nc")) {
       desc = "AVHRR L1B";
     }
     else if (filename.startsWith("HIRS_xxx_1B")) {
       desc = "HIRS L1B";
     }
     else if (filename.contains("HIRS_C") && filename.endsWith(".nc")) {
       desc = "HIRS L1B";
     }
     else if (filename.startsWith("hirsl1c") && filename.endsWith(".h5")) {
       desc = "HIRS L1C";
     }     
     else if (filename.startsWith("MHSx_xxx_1B")) {
       desc = "MHS L1B";
     }
     else if (filename.contains("MHS_C") && filename.endsWith(".nc")) {
       desc = "MHS L1B";
     }
     else if (filename.startsWith("AMSA_xxx_1B")) {
       desc = "AMSU-A L1B";
     }
     else if (filename.contains("AMSUA_C") && filename.endsWith(".nc")) {
       desc = "AMSU-A L1B";
     }
     else if (filename.startsWith("amsual1c") && filename.endsWith(".h5")) {
       desc = "AMSU-A L1C";
     }  
     else if (filename.startsWith("mhsl1c") && filename.endsWith(".h5")) {
       desc = "MHS L1C";
     }     
     else if (filename.startsWith("AVHR_xxx_1B")) {
       desc = "AVHRR L1B";
     }
     else if (filename.contains("MERSI")) {
       desc = "MERSI";
     }
     else if (filename.startsWith("NPR-MIRS")) {
       desc = "MIRS";
     }
     else if (filename.startsWith("geocatL2_OT")) {
       desc = "OT";
     }
     else if (filename.startsWith("geocatL1.HIMAWARI-8") || filename.startsWith("geocatL2.HIMAWARI-8")) {
       desc = "AHI";
     }
     else if (filename.contains("SST") && filename.contains("VIIRS_NPP-ACSPO")) {
       desc = "SST ACSPO";
     }
     else if (filename.startsWith("ACSPO-VIIRS")) {
        desc = "SST ACSPO";
     }
     else if (filename.contains("_ABI-L2-")) {
        desc = "ABI L2";
     }
     
     return desc;
   }

   public static String getDateTimeStampFromFilename(String filename) {
      long millis = getSecondsSinceEpoch(filename);
      return makeDateTimeStamp(millis);
   }
   
   public static Date getDateTimeFromFilename(String filename) {   
     Date datetime = null;
     
     try {
        if (filename.contains("_npp_d")) {
           int idx = filename.indexOf("_npp_d");
           idx += 6;
           String str = filename.substring(idx, idx+16);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'_t'HHmmss");
           datetime = sdf.parse(str);
        }
        else if (filename.contains("_j01_d")) {
           int idx = filename.indexOf("_j01_d");
           idx += 6;
           String str = filename.substring(idx, idx+16);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'_t'HHmmss");
           datetime = sdf.parse(str);
        }
        else if (filename.contains("_j02_d")) {
           int idx = filename.indexOf("_j02_d");
           idx += 6;
           String str = filename.substring(idx, idx+16);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'_t'HHmmss");
           datetime = sdf.parse(str);
        }        
        else if (filename.startsWith("MOD14") || filename.startsWith("MYD14")) {
           String yyyyddd = filename.substring(7,14);
           String hhmm = filename.substring(15,19);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDDHHmm");
           String yyyyDDDHHmm = yyyyddd.concat(hhmm);
           datetime = sdf.parse(yyyyDDDHHmm);
        }
        else if (filename.startsWith("MOD") || filename.startsWith("MYD")) {
           String yyyyddd = filename.substring(10,17);
           String hhmm = filename.substring(18,22);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDDHHmm");
           String yyyyDDDHHmm = yyyyddd.concat(hhmm);
           datetime = sdf.parse(yyyyDDDHHmm);
        }
        else if (filename.startsWith("a1") || filename.startsWith("t1")) {
           SimpleDateFormat sdf = new SimpleDateFormat("yyDDDHHmm");
           String yyddd = filename.substring(3,8);
           String hhmm = filename.substring(9,13);
           String yyyyDDDHHmm = yyddd.concat(hhmm);
           datetime = sdf.parse(yyyyDDDHHmm);
        }
        else if (filename.startsWith("HIRS_xxx_1B") || filename.startsWith("MHSx_xxx_1B") ||
                 filename.startsWith("AVHR_xxx_1B") || filename.startsWith("IASI_xxx_1C") || 
                 filename.startsWith("AMSA_xxx_1B")) {
           String yyyymmdd = filename.substring(16,24);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

           String hhmm = filename.substring(24,28);
           String yyyyMMddHHmm = yyyymmdd.concat(hhmm);
           datetime = sdf.parse(yyyyMMddHHmm);
        }
        else if (filename.startsWith("FY3C_MERSI")) {
           String yyyymmdd = filename.substring(19, 27);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
           String hhmm = filename.substring(28,32);
           datetime = sdf.parse(yyyymmdd.concat(hhmm));
        }
        else if (filename.contains("MERSI")) {
           String yyyyddd = filename.substring(2,9);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDDHHmm");
           String HHmm = filename.substring(9,13);
           datetime = sdf.parse(yyyyddd.concat(HHmm));
        }
        else if (filename.startsWith("NPR-MIRS")) {
           int idx = filename.lastIndexOf("NPP_s");
           idx += 5;
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
           String yyyyMMddHHmm = filename.substring(idx, idx+12);
           datetime = sdf.parse(yyyyMMddHHmm);
        }
        else if (filename.contains("IASI_C") && filename.endsWith(".nc")) {
            int idx = filename.lastIndexOf("IASI_C_EUMP");
            idx += 12;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 8;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.startsWith("iasil1c") && filename.endsWith(".h5")) {
            int idx = 12;
            String yyyyMMdd = filename.substring(idx, idx+8);
            idx += 8;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 1;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }        
        else if (filename.contains("AVHR_C") && filename.endsWith(".nc")) {
            int idx = filename.lastIndexOf("AVHR_C_EUMP");
            idx += 12;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 8;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.contains("HIRS_C") && filename.endsWith(".nc")) {
            int idx = filename.lastIndexOf("HIRS_C_EUMP");
            idx += 12;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 8;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.startsWith("hirsl1c") && filename.endsWith(".h5")) {
            int idx = 12;
            String yyyyMMdd = filename.substring(idx, idx+8);
            idx += 8;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 1;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }      
        else if (filename.startsWith("amsual1c") && filename.endsWith(".h5")) {
            int idx = 13;
            String yyyyMMdd = filename.substring(idx, idx+8);
            idx += 8;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 1;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.startsWith("mhsl1c") && filename.endsWith(".h5")) {
            int idx = 11;
            String yyyyMMdd = filename.substring(idx, idx+8);
            idx += 8;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 1;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.contains("AMSUA_C") && filename.endsWith(".nc")) {
            int idx = filename.lastIndexOf("AMSUA_C_EUMP");
            idx += 13;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 8;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.contains("MHS_C") && filename.endsWith(".nc")) {
            int idx = filename.lastIndexOf("MHS_C_EUMP");
            idx += 11;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 8;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.contains("atm_prof_rtv") && filename.endsWith(".h5")) {
            int idx = 6;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 10;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.startsWith("VNP") || filename.startsWith("VJ1") || filename.startsWith("VJ2")) {
            int idx = 10;
            String str = filename.substring(idx,idx+12);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDD'.'HHmm");
            datetime = sdf.parse(str);           
        }
        else if (filename.startsWith("geocatL2_OT.Aqua.")) {
            int idx = 17;
            String str = filename.substring(idx,idx+12);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDD'.'HHmm");
            datetime = sdf.parse(str);
        }
        else if (filename.startsWith("geocatL2_OT.Terra.")) {
            int idx = 18;
            String str = filename.substring(idx,idx+12);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDD'.'HHmm");
            datetime = sdf.parse(str);           
        }
        else if (filename.startsWith("geocatL1.HIMAWARI-8") || filename.startsWith("geocatL2.HIMAWARI-8")) {
            int idx = 20;
            String str = filename.substring(idx, idx+12);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDD'.'HHmm");
            datetime = sdf.parse(str);                       
        }
        else if (filename.startsWith("SEADAS_modis")) {
            int idx = 14;
            String str = filename.substring(idx, idx+14);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'_t'HHmm");
            datetime = sdf.parse(str);
        }
        else if (filename.contains("SST") && filename.contains("VIIRS_NPP-ACSPO")) {
           String str = filename.substring(0, 12);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
           datetime = sdf.parse(str);
        }
        else if (filename.startsWith("ACSPO-VIIRS")) {
           int idx = filename.lastIndexOf("_s");
           idx += 2;
           String str = filename.substring(idx, idx+12);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
           datetime = sdf.parse(str);
        }
        else if (filename.startsWith("HS_H08") && filename.endsWith(".nc")) {
            int idx = 7;
            String yyyyMMdd = filename.substring(idx, idx+=8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 1;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));          
        }
        else if (filename.contains("_ABI-L2-")) {
           int idx = filename.lastIndexOf("G16_s");
           if (idx < 0) {
              idx = filename.lastIndexOf("G17_s");
           }
           idx += 5;
           String yyyyDDDHHmm = filename.substring(idx, idx+11);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDDHHmm");
           datetime = sdf.parse(yyyyDDDHHmm);
        }
        else if (filename.contains("AHI-L2-CMIP")) {
           int idx = filename.lastIndexOf("H8_s");
           if (idx < 0) {
              idx = filename.lastIndexOf("H9_s");
           }
           idx += 4;
           String yyyyDDDHHmm = filename.substring(idx, idx+11);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDDHHmm");
           datetime = sdf.parse(yyyyDDDHHmm);
        }
        else if (filename.startsWith("SNDR.J1") || filename.startsWith("SNDR.J2")) {
           int idx = 13;
           String yyyyMMddHHmm = filename.substring(idx, idx+13);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmm");
           datetime = sdf.parse(yyyyMMddHHmm);
        }
        else if (filename.startsWith("SNDR.SNPP")) {
           int idx = 15;
           String yyyyMMddHHmm = filename.substring(idx, idx+13);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmm");
           datetime = sdf.parse(yyyyMMddHHmm);
        }
     }
     catch (Exception e) {
        return null;
     }
     
     return datetime;
  }
   
  public static long getSecondsSinceEpoch(String filename) {
     Date datetime = getDateTimeFromFilename(filename);
     long time = datetime.getTime();
     return time;
  }
  
  public static String makeDateTimeStamp(Date date) {
     return makeDateTimeStamp(date.getTime());
  }
  
  public static String makeDateTimeStamp(long time) {
     String timeStr = (new Timestamp(time)).toString();
     timeStr = timeStr.substring(0,16);
     return timeStr;
  }
  
  public static ArrayList<File> getTimeSortedFileList(List<File> fileList) throws Exception {
     return getTimeSortedFileList((File[]) fileList.toArray(new File[0]));
  }
    
  public static ArrayList<File> getTimeSortedFileList(File[] fileList) throws Exception {
     int numFiles = fileList.length;
     double[] times = new double[numFiles];
     for (int k=0; k<numFiles; k++) {
        File file = fileList[k];
        String name = file.getName();
        times[k] = (double) DataSource.getSecondsSinceEpoch(name);
     }

     int[] indexes = QuickSort.sort(times);

     ArrayList<File> sortedList = new ArrayList<File>();
     for (int k=0; k<numFiles; k++) {
        sortedList.add(fileList[indexes[k]]);
     } 
      
     return sortedList;
  }
  
  public static ArrayList<String> getTimeSortedFilenameList(List<File> fileList) throws Exception {
     return getTimeSortedFilenameList((File[]) fileList.toArray(new File[0]));
  }
  
  public static ArrayList<String> getTimeSortedFilenameList(File[] fileList) throws Exception {

      int numFiles = fileList.length;
      String[] filenames = new String[numFiles];
      double[] times = new double[numFiles];
      for (int k=0; k<numFiles; k++) {
         File file = fileList[k];
         String name = file.getName();
         times[k] = (double) DataSource.getSecondsSinceEpoch(name);
         filenames[k] = file.getAbsolutePath();
      }

      int[] indexes = QuickSort.sort(times);

      ArrayList<String> sortedList = new ArrayList<String>();
      for (int k=0; k<numFiles; k++) {
         sortedList.add(filenames[indexes[k]]);
      }

      return sortedList;
  }
  
  public DataChoice getDataChoiceByName(String name) {
     for (int i=0; i<myDataChoices.size(); i++) {
        DataChoice choice = myDataChoices.get(i);
        if (choice.getName().equals(name)) {
           return choice;
        }
     }
     return null;
  }
  
  public int getDefaultChoice() {
     return 0;
  }
  
  public List getDataChoices() {
     return myDataChoices;
  }
    
  /**
   * Place to keep common color tables. Can be over-ridden more specific control.
   * @param choice
   * @return 
   */
  public ColorTable getDefaultColorTable(DataChoice choice) {
     ColorTable clrTbl = Hydra.grayTable;
     String name = choice.getName();
     
     if (name.contains("Emissive")) {
        clrTbl = Hydra.invGrayTable;
     }
     else if (name.contains("Cloud_Mask")) {
        float[][] palette = new float[][] {{0.9f,0.9f,0.0f,0.0f},
                                           {0.9f,0.0f,0.9f,0.9f},
                                           {0.9f,0.0f,0.9f,0.0f},
                                           {0.97f,0.97f,0.98f,0.98f}};
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     else if (name.contains("Binary_Cld_Msk")) {
        float[][] palette = new float[][] {{0.9f,0.0f},
                                           {0.9f,0.9f},
                                           {0.9f,0.0f},
                                           {0.97f,0.98f}};
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     else if (name.contains("Cloud_Phase_Infrared") || name.contains("Cloud_Phase")) {
        float[][] palette = new float[][] {{0.0f,0.0f,1.0f,0.8f,0.0f,0.0f,0.0f},
                                           {0.0f,0.0f,0.5f,0.8f,0.8f,0.8f,0.8f},
                                           {0.0f,0.8f,0.5f,0.0f,0.0f,0.0f,0.0f},
                                           {0.00f,0.97f,0.98f,0.98f,0.98f,0.98f,0.98f}};
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     else if (name.contains("fire_mask_abi")) {
        float[][] palette = new float[][] {{0.0f,1.0f,1.0f,1.0f},
                                           {0.0f,1.0f,0.58f,0.0f},
                                           {0.0f,0.0f,0.0f,0.0f},
                                           {0.0f,0.98f,0.98f,0.98f}};
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     else if (name.contains("fire_mask")) {
        float[][] palette = new float[][] {{0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,1.0f,1.0f,1.0f},
                                           {0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,1.0f,0.58f,0.0f},
                                           {0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f},
                                           {0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.98f,0.98f,0.98f}};
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     else if (name.contains("Cloud_Type")) {
        float[][] palette = new float[][] {{0f, 10f, 33f,   41f, 10f,  250f, 246f, 252f, 143f, 244f},
                                           {0f, 35f, 189f, 249f, 102f, 247f, 13f,  136f, 148f, 40f},
                                           {0f, 241f, 249f, 46f, 13f,   54f,  27f, 37f, 144f, 250f},
                       {0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f}};
        
        for (int i=0; i<palette[0].length; i++) palette[0][i] /= 256;
        for (int i=0; i<palette[1].length; i++) palette[1][i] /= 256;
        for (int i=0; i<palette[2].length; i++) palette[2][i] /= 256;
        
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     else if (name.contains("Cloud_Top_Temperature") || name.contains("Cld_Top_Temp")) {
        clrTbl = Hydra.rainbow;
     }
     else if (name.contains("Cloud_Top_Pressure") || name.contains("Cld_Top_Pres")) {
        clrTbl = Hydra.rainbow;
     }
     else if (name.contains("Cld_Top_Hght")) {
        clrTbl = Hydra.rainbow;
     }
     else if (name.contains("Sea_Surface_Temperature") || name.contains("SST")) {
        clrTbl = Hydra.rainbow;
     }
     else if (name.contains("radiances")) {
        clrTbl = Hydra.invGrayTable;
     }
     else if (name.contains("RainRate")) {
        clrTbl = Hydra.rainbow;
     }
     else if (name.contains("TPW")) {
        clrTbl = Hydra.rainbow;
     }
     else if (name.contains("brightness_temp")) {
        clrTbl = Hydra.invGrayTable;
     }
     else if (name.contains("albedo")) {
        clrTbl = Hydra.grayTable;
     }
     
     return clrTbl;
  }
  
  public boolean isAtmRetrieval() {
     return false;
  }
  
  public boolean isSounder() {
     return false;
  }
  
  public boolean isImager() {
     return true;
  }

  public boolean hasMultiSpectralData() { return false; }
  
  public abstract Data getData(DataChoice dataChoice, DataSelection dataSelection) throws VisADException, RemoteException;
  
  public Data getData(DataChoice dataChoice) throws VisADException, RemoteException {
     return getData(dataChoice, null);
  }
  
  /** These are not required, but may be supported */
  public MultiSpectralData getMultiSpectralData(DataChoice dataChoice) {
     return null;
  }

  public MultiSpectralData getMultiSpectralData(String name) {return null;}

  public MultiSpectralData[] getMultiSpectralData() {
     return null;
  }
  
  /* ???
  public Data getData(DataView view, DataChoice dataChoice, DataSelection dataSelection) {
     
  }
  */
}
