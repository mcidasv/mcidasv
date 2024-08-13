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

import edu.wisc.ssec.adapter.GOESGridAdapter;

import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.MultiDimensionAdapter;
import edu.wisc.ssec.adapter.MultiDimensionReader;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.MultiSpectralAggr;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.SpectrumAdapter;
import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.hydra.Hydra;

import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.rmi.RemoteException;
import java.util.Date;

import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.unidata.util.ColorTable;
import visad.CoordinateSystem;

import visad.Data;
import visad.FlatField;
import visad.FunctionType;
import visad.Linear2DSet;
import visad.RealTupleType;
import visad.VisADException;

public class CMIP_MultiBand_DataSource extends DataSource {

  NetCDFFile reader;
  ArrayList<Variable> projVarList = new ArrayList<>();
  ArrayList<Variable> varsWithProj = new ArrayList<>();
  HashMap<String, Variable> projXCoordVars = new HashMap<>();
  HashMap<String, Variable> projYCoordVars = new HashMap<>();
  HashMap<String, Variable> timeCoordVars = new HashMap<>();

  private ArrayList<GOESGridAdapter> adapters = new ArrayList<>();
  
  private MultiSpectralData msd;
  
  HashMap<DataChoice, MultiSpectralData> msdMap = new HashMap();
  
  String bandName;
  float centerWavelength;
  
  String sensorName = "ABI_2KM";
  
  DataGroup cat;
  
  double default_stride = 10;
  
  boolean unpack = true;
  
  String dateTimeStamp;
  
  Date dateTime;
  
  boolean zeroBased = true;
  
  String[] bandNames =  new String[] {
           "C01", "C02", "C03", "C04", "C05",
           "C06", "C07", "C08", "C09", "C10",
           "C11", "C12", "C13", "C14", "C15", "C16"
  };    
      
  float[] centerWavelengths = new float[] {
           0.47f, 0.64f, 0.86f, 1.37f, 1.61f, 2.25f, 3.9f, 6.19f, 6.95f, 7.34f,
           8.5f, 9.61f, 10.35f, 11.2f, 12.3f, 13.3f
  };
  
  DataGroup cat2KMrefl = new DataGroup("2KMrefl");
  DataGroup cat2KMemis = new DataGroup("2KMemis");
  
  DataGroup[] category = new DataGroup[] {
         cat2KMrefl, cat2KMrefl, cat2KMrefl, cat2KMrefl,
         cat2KMrefl, cat2KMrefl, cat2KMemis, cat2KMemis,
         cat2KMemis, cat2KMemis, cat2KMemis, cat2KMemis,
         cat2KMemis, cat2KMemis, cat2KMemis, cat2KMemis
  };
  
  ArrayList<MultiSpectralData> refl2KM = new ArrayList<>();
  ArrayList<MultiSpectralData> emis2KM = new ArrayList<>();
   
  MultiSpectralData reflMSD2km;
  MultiSpectralData emisMSD2km;
  
  public CMIP_MultiBand_DataSource(ArrayList<File> files) throws Exception {
      this(files.toArray((new File[] {null}))[0]);
  }
  
  public CMIP_MultiBand_DataSource(File file)throws Exception {
     this(file, 10);
  }
  
  public CMIP_MultiBand_DataSource(File file, double default_stride) throws Exception{
     this(file, default_stride, true, true);
  }
  
  /**
   * 
   * @param file
   * @param default_stride
   * @param unpack
   */
  public CMIP_MultiBand_DataSource(File file, double default_stride, boolean unpack, boolean zeroBased) throws Exception {
     
    if (!canUnderstand(new File[] {file})) {
       throw new Exception("CMIP_MultiBand_DataSource doesn't understand this: ");
    }
     
    this.default_stride = default_stride;
    this.dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName());
    this.dateTime = DataSource.getDateTimeFromFilename(file.getName());
    this.unpack = unpack;
    this.zeroBased = zeroBased;
    
    try {
      init(file.getPath());
    } 
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void init(String filename) throws Exception {
     reader = new NetCDFFile(filename);

     HashMap varMap = reader.getVarMap();
     Iterator<Variable> iter = varMap.values().iterator();
     while (iter.hasNext()) {
        Variable var = iter.next();
        String varName = var.getShortName();
        int[] varDims = reader.getDimensionLengths(varName);
        int rank = varDims.length;

        Attribute attr = var.findAttribute("grid_mapping_name");
        if (attr != null) {
           projVarList.add(var);
        }
        else if (var.findAttribute("grid_mapping") != null) {
           varsWithProj.add(var);
        }
        else {
           attr = var.findAttribute("standard_name");
           if (attr != null) {
              String stndName = attr.getStringValue();
              if (stndName.equals("projection_x_coordinate")) {
                 projXCoordVars.put(varName, var);
              }
              else if (stndName.equals("projection_y_coordinate")) {
                 projYCoordVars.put(varName, var);
              }
              else if (stndName.equals("time")) {
                 timeCoordVars.put(varName, var);
              }
           }
           else {
              varsWithProj.add(var);
           }
        }

        if (rank == 1) {
           attr = var.findAttribute("units");
           String[] dimNames = reader.getDimensionNames(varName);
           if (attr != null) {
              String str = attr.getStringValue();
              visad.Unit unit = null;
              try {
                 unit = visad.data.units.Parser.parse(str);
              }
              catch (Exception e) {
              }
              if (unit != null && unit.isConvertible(visad.SI.second)) {
                 if (varName.equals(dimNames[0])) {
                    timeCoordVars.put(varName, var);
                 }
              }
           }
        }
     }

     ArrayList<DataChoice> tmpList = new ArrayList();
     ArrayList<GOESGridAdapter> tmpAdapterList = new ArrayList<>();

     iter = varsWithProj.iterator();
     while (iter.hasNext()) {
        Variable var = iter.next();
        String varName = var.getShortName();
        if (varName.contains("longitude") || varName.contains("latitude")) { // don't want to display these
           continue;
        }
        
        String[] dimNames = reader.getDimensionNames(varName);
        
        Variable varX = null;
        Variable varY = null;
        Variable varT = null;
        
        for (int k=0; k<dimNames.length; k++) {
           Iterator itr = projXCoordVars.keySet().iterator();
           while (itr.hasNext()) {
              Object key = itr.next();
              Variable vr = projXCoordVars.get(key);
              String name = vr.getShortName();
              String[] vrDimsName = reader.getDimensionNames(name);
              if (vrDimsName != null && vrDimsName.length > 0) {
                 String coordDimName = vrDimsName[0];
                 if (dimNames[k].equals(coordDimName)) {
                    varX = vr;
                    break;
                 }
              }
           }
           
           itr = projYCoordVars.keySet().iterator();
           while (itr.hasNext()) {
              Object key = itr.next();
              Variable vr = projYCoordVars.get(key);
              String name = vr.getShortName();
              String[] vrDimsName = reader.getDimensionNames(name);
              if (vrDimsName != null && vrDimsName.length > 0) {
                 String coordDimName = vrDimsName[0];
                 if (dimNames[k].equals(coordDimName)) {
                    varY = vr;
                    break;
                 }
              }   
           }
           
           itr = timeCoordVars.keySet().iterator();
           while (itr.hasNext()) {
              Object key = itr.next();
              Variable vr = timeCoordVars.get(key);
              String name = vr.getShortName();
              String[] vrDimsName = reader.getDimensionNames(name);
              if (vrDimsName != null && vrDimsName.length > 0) {
                 String coordDimName = vrDimsName[0];
                 if (dimNames[k].equals(coordDimName)) {
                    varT = vr;
                    break;
                 }
              }                 
           }          
        }

        Variable projVar = projVarList.get(0); //TODO: may be more than one 
        
        if (varX != null && varY != null) {
           GEOSInfo geosInfo = new GEOSInfo(reader, var, projVar, varT, varX, varY);
           String name = var.getShortName();
           if (name.startsWith("CMI_")) {
              name = name.substring(4, 7);
              bandName = name;
              int bIdx = getBandIdx(bandName);
              centerWavelength = centerWavelengths[bIdx];
              cat = category[bIdx];
           }
           else {
              continue;
           }

            HashMap metadata = GOESGridAdapter.getEmptyMetadataTable();
            metadata.put(MultiDimensionAdapter.array_name, geosInfo.getName());
            metadata.put(GOESGridAdapter.gridX_name, geosInfo.getXDimName());
            metadata.put(GOESGridAdapter.gridY_name, geosInfo.getYDimName());
            metadata.put(MultiDimensionAdapter.fill_value_name, "_FillValue");
            if (unpack) {
               metadata.put("unpack", "true");
            }

            GOESGridAdapter goesAdapter = new GOESGridAdapter(reader, metadata, geosInfo.getMapProjection(), default_stride, zeroBased);
            HashMap subset = goesAdapter.getDefaultSubset();
            if (geosInfo.getTDimName() != null) {
               subset.put(geosInfo.getTDimName(), new double[] {0.0, 0.0, 1.0});
            }
            DataSelection dataSel = new MultiDimensionSubset(subset);
            DataChoice dataChoice = new DataChoice(this, name, null);
            dataChoice.setDataSelection(dataSel);
            tmpList.add(dataChoice);
            tmpAdapterList.add(goesAdapter);
            
            msd = makeMultiSpectralData(goesAdapter, reader, geosInfo.getName(), geosInfo.getXDimName(), geosInfo.getYDimName(), bandName, centerWavelength, sensorName, cat);
            msdMap.put(dataChoice, msd);
            
            if (cat.equals(cat2KMrefl)) {
               refl2KM.add(msd);
            }
            else if (cat.equals(cat2KMemis)) {
               emis2KM.add(msd);
            }
        }
     }
     
     reflMSD2km = new MultiSpectralAggr((MultiSpectralData[]) refl2KM.toArray(new MultiSpectralData[1]));
     emisMSD2km = new MultiSpectralAggr((MultiSpectralData[]) emis2KM.toArray(new MultiSpectralData[1]));
     
     for (int k=0; k<bandNames.length; k++) {
        String bandName = bandNames[k];
        for (int j=0; j<tmpList.size(); j++) {
           DataChoice choice = tmpList.get(j);
           String str = choice.getName();
           if (bandName.equals(str)) {
              addDataChoice(choice);
              adapters.add(tmpAdapterList.get(j));
              break;
           }
        }
     }

  }
  
  MultiSpectralData makeMultiSpectralData(GOESGridAdapter gridAdapter, MultiDimensionReader reader, String array, String gridX, String gridY, String band, float cntrWvlen, String sensorName, DataGroup cat) {
      HashMap table = SpectrumAdapter.getEmptyMetadataTable();
      table.put(SpectrumAdapter.array_name, array);
      table.put(SpectrumAdapter.x_dim_name, gridX);
      table.put(SpectrumAdapter.y_dim_name, gridY);
      table.put(SpectrumAdapter.channelValues, new float[] {cntrWvlen});
      table.put(SpectrumAdapter.bandNames, new String[] {band});
      table.put(SpectrumAdapter.channelType, "wavelength");
      table.put(SwathAdapter.array_dimension_names, new String[] {gridY, gridX});
      SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, table);
      
      String paramName = null;
      
      if (cat == null) {
         paramName = null;
      }
      else if (cat.getName().contains("emis")) {
         paramName = "BrightnessTemp";
      }
      else if (cat.getName().contains("refl")) {
         paramName = "Reflectance";
      }

      MultiSpectralData multiSpectData = new MultiSpectralData(gridAdapter, spectrumAdapter, paramName, paramName, sensorName, null);
      return multiSpectData;
   }
  
   public MultiSpectralData[] getMultiSpectralData() {
      ArrayList<MultiSpectralData> list = new ArrayList();
       
      if (reflMSD2km != null) {
          list.add(reflMSD2km);
      }
      if (emisMSD2km != null) {
         list.add(emisMSD2km);
      }
       
      return list.toArray(new MultiSpectralData[1]);
  }    
    
  @Override
  public Date getDateTime() {
     return dateTime;
  }
  
  @Override
  public String getDescription() {
     return "GOES-16 ABI";
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
  public float getNadirResolution(DataChoice choice) {
     return 2000f;
  }
  
  public int getBandIdx(String name) {
      int idx = -1;
      for (int k=0; k<bandNames.length; k++) {
         if (name.equals(bandNames[k])) {
            idx = k;
            break;
         }
      }
      return idx;
  }
  
  public String getDescription(DataChoice choice) {
      String name = choice.getName();
      
      float cntrWvln = 0;
      for (int k=0; k<bandNames.length; k++) {
         if (name.equals(bandNames[k])) {
            cntrWvln = centerWavelengths[k];
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
  
  public ColorTable getDefaultColorTable(DataChoice choice) {
      ColorTable clrTbl = Hydra.grayTable;
      String name = choice.getName();
      if ( name.equals("C07") || name.equals("C08") || name.equals("C09") || name.equals("C10") || name.equals("C11") || name.equals("C12") || name.equals("C13") || name.equals("C14") || name.equals("C15") || name.equals("C16") ) {
        clrTbl = Hydra.invGrayTable;
      }
      
      return clrTbl;
  }
  
  public boolean getDoReproject(DataChoice choice) {
     return false;
  }
  
  public void addDataChoice(DataChoice dataChoice) {
     myDataChoices.add(dataChoice); 
  }
  
  public MultiSpectralData getMultiSpectralData(DataChoice choice) {
     return msdMap.get(choice);
  }
  

  public Data getData(DataChoice dataChoice, DataSelection dataSelection)
      throws VisADException, RemoteException
  {
      try {
         ArrayList dataChoices = (ArrayList) getDataChoices();
         int idx = dataChoices.indexOf(dataChoice);
         GOESGridAdapter adapter = adapters.get(idx);

         MultiDimensionSubset select = (MultiDimensionSubset)dataChoice.getDataSelection();
         HashMap subset = select.getSubset();
         
         Data data = adapter.getData(subset);
         CoordinateSystem cs = ((RealTupleType) ((FunctionType)data.getType()).getDomain()).getCoordinateSystem();
       
         reflMSD2km.setCoordinateSystem(cs);
         reflMSD2km.setSwathDomainSet((Linear2DSet)((FlatField)data).getDomainSet());
         emisMSD2km.setCoordinateSystem(cs);
         emisMSD2km.setSwathDomainSet((Linear2DSet)((FlatField)data).getDomainSet());         

         return data;
      } catch (Exception e) {
         e.printStackTrace();
      }
      return null;
  }

   @Override
   public boolean canUnderstand(File[] files) {
    String name = files[0].getName();
    if (name.contains("ABI-L2-MCMIP")) {
       return true;
    }
    return false;
   }

}