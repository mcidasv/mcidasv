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

import edu.wisc.ssec.adapter.MultiSpectralAggr;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.hydra.Hydra;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import visad.VisADException;
import visad.Data;
import visad.FlatField;
import java.rmi.RemoteException;
import java.util.Date;
import ucar.unidata.util.ColorTable;
import visad.CoordinateSystem;
import visad.FunctionType;
import visad.Linear2DSet;
import visad.RealTupleType;


public class VIIRSDataSource extends DataSource {


   public static String[] bands =  new String[] {
     "SVI01", "SVI02", "SVI03", "SVI04", "SVI05", 
     "SVM01", "SVM02", "SVM03", "SVM04", "SVM05",
     "SVM06", "SVM07", "SVM08", "SVM09", "SVM10",
     "SVM11", "SVM12", "SVM13", "SVM14", "SVM15", 
     "SVM16", "SVDNB"
   };

   public static float[] nadirResolution = new float[] {
     380f, 380f, 380f, 380f, 380f,
     770f, 770f, 770f, 770f, 770f,
     770f, 770f, 770f, 770f, 770f,
     770f, 770f, 770f, 770f, 770f,
     770f, 770f
   };

   public static String[] bandNames =  new String[] {
     "I1", "I2", "I3", "I4", "I5",
     "M1", "M2", "M3", "M4", "M5",
     "M6", "M7", "M8", "M9", "M10",
     "M11", "M12", "M13", "M14", "M15",
     "M16", "DNB"
   };

   public static float[] centerWavelength = new float[] {
     0.640f, 0.856f, 1.610f, 3.740f, 11.450f, 0.412f, 0.445f, 0.488f, 0.555f, 0.672f,
     0.746f, 0.865f, 1.240f, 1.378f, 1.61f, 2.250f, 3.700f, 4.050f, 8.550f, 10.763f, 12.013f, 0.70f 
   };
   
   HashMap<String, ArrayList> bandIDtoFileList = new HashMap<>();

   HashMap<String, NOAA_VIIRS_DataSource> bandIDtoDataSource = new HashMap<>();
   
   HashMap<String, String> bandIDtoBandName = new HashMap<>();
   HashMap<String, String> bandNameToBandID = new HashMap<>();

   DataGroup catI = new DataGroup("I-Band");
   DataGroup catM = new DataGroup("M-Band");
   DataGroup catDNB = new DataGroup("DNB-Band");

   String dateTimeStamp = null;
   Date dateTime = null;
   
   ArrayList<MultiSpectralData> Iemis = new ArrayList<>();
   ArrayList<MultiSpectralData> Irefl = new ArrayList<>();
   ArrayList<MultiSpectralData> Memis = new ArrayList<>();
   ArrayList<MultiSpectralData> Mrefl = new ArrayList<>();
   
   MultiSpectralData IemisMSD;
   MultiSpectralData MemisMSD;
   MultiSpectralData IreflMSD;
   MultiSpectralData MreflMSD;
   
   HashMap<DataChoice, MultiSpectralData> msdMap = new HashMap();
   MultiSpectralData multiSpectData;
   
   public VIIRSDataSource(File directory) throws Exception {
     this(directory.listFiles());
   }
   
   public VIIRSDataSource(ArrayList<File> files) throws Exception {
      this(files.toArray(new File[] {null}));
   }

   public VIIRSDataSource(File[] files) throws Exception {
      
      ArrayList<File> noneHidden = new ArrayList();
      for (int k=0; k<files.length; k++) {
         File file = files[k];
         if (!file.isHidden()) {
            noneHidden.add(file);
         }        
      }
      
      files = noneHidden.toArray(new File[] {null});
      
      if (!canUnderstand(files)) {
         throw new Exception("not compatible with VIIRSDataSource");
      }

      int numFiles = files.length;
      int numBands = bands.length;

      boolean[] used = new boolean[numFiles];

      for (int k=0; k<numBands; k++) {
         ArrayList<File> fileList = new ArrayList<File>();
         bandIDtoFileList.put(bands[k], fileList);
         bandIDtoBandName.put(bands[k], bandNames[k]);
         bandNameToBandID.put(bandNames[k], bands[k]);

         for (int t=0; t<numFiles; t++) {
            if (!used[t]) {
              File file = files[t];
              String name = file.getName();
              String filename = file.getAbsolutePath();
              if (name.startsWith(bands[k]) && name.endsWith(".h5")) {
                fileList.add(file);
                used[t] = true;
              }
            }
         }
      }

      // create DataSource for each band
      int num = 0;
      for (int k=0; k<numBands; k++) {
        ArrayList<File> fileList = bandIDtoFileList.get(bands[k]);
        if (!fileList.isEmpty()) {

          if (dateTime == null) {
            dateTimeStamp = DataSource.getDateTimeStampFromFilename((String)fileList.get(0).getName());
            dateTime = DataSource.getDateTimeFromFilename((String)fileList.get(0).getName());
          }

          NOAA_VIIRS_DataSource datasource = null;
          try {
            ArrayList<String> sortedList = DataSource.getTimeSortedFilenameList(fileList);
            File[] tmpFiles = new File[sortedList.size()];
            for (int i=0; i<tmpFiles.length; i++) {
               tmpFiles[i] = new File(sortedList.get(i));
            }
            datasource = new NOAA_VIIRS_DataSource(tmpFiles);
            bandIDtoDataSource.put(bands[k], datasource);
          }
          catch (Exception e) {
             e.printStackTrace();
          }
          
          DataGroup cat = (k <= 4) ? catI : catM;
          if (k == 21) cat = catDNB;          

          
          List dataChoices = datasource.getDataChoices();

          DataChoice targetDataChoice = null;

          for (int t=0; t< dataChoices.size(); t++) {
            DataChoice choice = (DataChoice) dataChoices.get(t);
            String name = choice.getName(); 
            if (name.contains("Reflectance") || name.contains("BrightnessTemperature")) {
              targetDataChoice = choice;
            }
            if (bands[k].equals("SVDNB") && name.contains("Radiance")) {
              targetDataChoice = choice;
            }
            if (name.contains("Reflectance")) {
               if (cat == (catI)) {
                  Irefl.add(datasource.getMultiSpectralData(choice));
               }
               else if (cat == (catM)) {
                  Mrefl.add(datasource.getMultiSpectralData(choice));     
               }
            }
            else if (name.contains("BrightnessTemperature")) {
               if (cat == (catI)) {
                  Iemis.add(datasource.getMultiSpectralData(choice));
               }
               else if (cat == (catM)) {
                  Memis.add(datasource.getMultiSpectralData(choice));
               }
            }
          }


          doMakeDataChoice(bandNames[k], k, num, targetDataChoice, cat);

          num++;
        }
      }
      
      try {
         if (Irefl.size() > 0) {
            IreflMSD = new MultiSpectralAggr((MultiSpectralData[]) Irefl.toArray(new MultiSpectralData[1]));
         }
         if (Iemis.size() > 0) {
            IemisMSD = new MultiSpectralAggr((MultiSpectralData[]) Iemis.toArray(new MultiSpectralData[1]));
         }
         if (Mrefl.size() > 0) {
            MreflMSD = new MultiSpectralAggr((MultiSpectralData[]) Mrefl.toArray(new MultiSpectralData[1]));
         }
         if (Memis.size() > 0) {
            MemisMSD = new MultiSpectralAggr((MultiSpectralData[]) Memis.toArray(new MultiSpectralData[1]));  
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }

   }

   public void doMakeDataChoice(String name, int bandIdx, int idx, DataChoice targetDataChoice, DataGroup category) {
     DataChoice dataChoice = new DataChoice(this, name, category);
     DataSelection dataSel = targetDataChoice.getDataSelection();
     dataChoice.setDataSelection(dataSel);
     myDataChoices.add(dataChoice);
   }
   
   public float getNadirResolution(DataChoice choice) throws Exception {
      String name = choice.getName();
      float res = 0;

      for (int k=0; k<bands.length; k++) {
        if (name.equals(bandNames[k])) {
          res = nadirResolution[k];
          break;
        }
      }

      if (res == 0) {
        throw new Exception("Item not found so can't get resolution");
      }
      else {
        return res;
      }
   }

   public String getDescription(DataChoice choice) {
      String name = choice.getName();
      
      float cntrWvln = 0;
      for (int k=0; k<bands.length; k++) {
         if (name.equals(bandNames[k])) {
            cntrWvln = centerWavelength[k];
            break;
         }
      }

      if (cntrWvln == 0) {
        return null;
      }
      else {
        return "("+cntrWvln+")";
      }

   }

   @Override
   public String getDescription() {
     return "VIIRS";
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
   
   @Override
   public int getDefaultChoice() {
      int idx = 0;
      Object[] choices = myDataChoices.toArray();
      for (int k=0; k < choices.length; k++) {
         if (((DataChoice)choices[k]).getName().contains("M15")) {
            idx = k;
            break;
         }
      }
      return idx;
   }
   
   public boolean canUnderstand(File[] files) {
      /* data and geolocation files are listed in pairs*/
      String fname = files[0].getName();           
      if (!(fname.startsWith("SV") || fname.startsWith("GM") ||
            fname.startsWith("SV") || fname.startsWith("GI") ||
            fname.startsWith("SV") || fname.startsWith("GD"))) {
        return false;
      }
      return true;
   }
   
   public ColorTable getDefaultColorTable(DataChoice choice) {
      ColorTable clrTbl = Hydra.grayTable;
      String name = choice.getName();     
      if ( name.equals("I4") || name.equals("I04") || name.equals("M12") || name.equals("M13") || name.equals("M14") || name.equals("M15") || name.equals("I5") || name.equals("I05") || name.equals("M16") ) {
        clrTbl = Hydra.invGrayTable;
      }
      return clrTbl;
   }

   public Data getData(DataChoice dataChoice, DataSelection dataSelection)
            throws VisADException, RemoteException
    {
       String name = dataChoice.getName();
       DataGroup datGrp = dataChoice.getGroup();
       name = bandNameToBandID.get(name);
       NOAA_VIIRS_DataSource datasource = bandIDtoDataSource.get(name);
       List dataChoices = datasource.getDataChoices();

       DataChoice targetDataChoice = null;
 
       for (int t=0; t< dataChoices.size(); t++) {
          DataChoice choice = (DataChoice) dataChoices.get(t);
          name = choice.getName();
          if (name.contains("Reflectance") || name.contains("BrightnessTemperature")) {
             targetDataChoice = choice;
          }
          if (dataChoice.getName().equals("DNB") && name.contains("Radiance")) {
             targetDataChoice = choice;
          } 
       }
      
       targetDataChoice.setDataSelection(dataChoice.getDataSelection());
       Data data = datasource.getData(targetDataChoice);
       
       CoordinateSystem cs = ((RealTupleType) ((FunctionType)data.getType()).getDomain()).getCoordinateSystem();
       
       if (datGrp.equals(catM)) {
          if (MreflMSD != null) {
             MreflMSD.setCoordinateSystem(cs);
             MreflMSD.setSwathDomainSet((Linear2DSet)((FlatField)data).getDomainSet());
          }
          if (MemisMSD != null) {
             MemisMSD.setCoordinateSystem(cs);
             MemisMSD.setSwathDomainSet((Linear2DSet)((FlatField)data).getDomainSet());
          }
       }
       else if (datGrp.equals(catI)) {
          if (IreflMSD != null) {
             IreflMSD.setCoordinateSystem(cs);
             IreflMSD.setSwathDomainSet((Linear2DSet)((FlatField)data).getDomainSet());
          }
          if (IemisMSD != null) {
             IemisMSD.setCoordinateSystem(cs);
             IemisMSD.setSwathDomainSet((Linear2DSet)((FlatField)data).getDomainSet());
          }
       }
       
       //-  post process for DNB: replace radiance with log10(radiance)
       if (dataChoice.getName().equals("DNB")) {
          float[][] rngVals = ((FlatField)data).getFloats(false);
          for (int k=0; k<rngVals[0].length; k++) {
             float fval = rngVals[0][k];
             if (fval <= 0f) {
                rngVals[0][k] = Float.NaN;
             }
             else {
                rngVals[0][k] = (float) Math.log10((double)fval);
             }
          }
       }
       return data;
    }
   
    public MultiSpectralData[] getMultiSpectralData() {
       ArrayList<MultiSpectralData> list = new ArrayList();
       
       if (IreflMSD != null) {
          list.add(IreflMSD);
       }
       if (MreflMSD != null) {
          list.add(MreflMSD);
       }
       if (IemisMSD != null) {
          list.add(IemisMSD);
       }
       if (MemisMSD != null) {
          list.add(MemisMSD);
       }
       
       return list.toArray(new MultiSpectralData[1]);
    }

}
