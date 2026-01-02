/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.adapter.MultiSpectralAggr;
import edu.wisc.ssec.adapter.MultiSpectralData;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import visad.VisADException;
import visad.Data;

import java.rmi.RemoteException;
import java.util.Date;

import visad.CoordinateSystem;
import visad.FlatField;
import visad.FunctionType;
import visad.Linear2DSet;
import visad.RealTupleType;


public class GEOSDirectory extends DataSource {

    public float[] nadirResolution;

    public String[] bandNames;

    public float[] centerWavelength;

    public String[] sensorName;

    ArrayList<String> targetList = new ArrayList();

    HashMap<String, ArrayList> bandIDtoFileList = new HashMap();

    HashMap<String, GEOSDataSource> bandIDtoDataSource = new HashMap();

    HashMap<String, String> bandIDtoBandName = new HashMap();
    HashMap<String, String> bandNameToBandID = new HashMap();

    String dateTimeStamp = null;

    Date dateTime = null;

    DataGroup catHKMrefl = new DataGroup("HKMrefl");
    DataGroup cat1KMrefl = new DataGroup("1KMrefl");
    DataGroup cat2KMrefl = new DataGroup("2KMrefl");
    DataGroup cat2KMemis = new DataGroup("2KMemis");

    DataGroup[] category;

    public int[] default_stride;

    boolean unpack = false;

    boolean zeroBased = true;

    ArrayList<MultiSpectralData> reflHKM = new ArrayList<>();
    ArrayList<MultiSpectralData> refl1KM = new ArrayList<>();
    ArrayList<MultiSpectralData> refl2KM = new ArrayList<>();
    ArrayList<MultiSpectralData> emis2KM = new ArrayList<>();

    MultiSpectralData reflMSDhkm;
    MultiSpectralData reflMSD1km;
    MultiSpectralData reflMSD2km;
    MultiSpectralData emisMSD2km;

    public GEOSDirectory(File directory) throws Exception {
        this(directory.listFiles());
    }

    public GEOSDirectory(File[] files) throws Exception {
        if (!canUnderstand(files)) {
            throw new Exception("GEOSDirectory doesn't understand");
        }
    }

    void init(File[] files) {
        int numFiles = files.length;
        int numBands = bandNames.length;
        boolean[] used = new boolean[numFiles];
        for (int k = 0; k < numBands; k++) {
            ArrayList<File> fileList = new ArrayList<File>();
            bandIDtoFileList.put(bandNames[k], fileList);
            bandIDtoBandName.put(bandNames[k], bandNames[k]);
            bandNameToBandID.put(bandNames[k], bandNames[k]);

            for (int t = 0; t < numFiles; t++) {
                if (!used[t]) {
                    File file = files[t];
                    String name = file.getName();
                    if (fileBelongsToThis(name) && name.contains(bandNames[k])) {
                        fileList.add(file);
                        used[t] = true;
                    }
                }
            }
        }

        int num = 0;
        for (int k = 0; k < numBands; k++) {
            ArrayList<File> fileList = bandIDtoFileList.get(bandNames[k]);
            if (!fileList.isEmpty()) {

                if (dateTime == null) {
                    try {
                        String filename = (String) fileList.get(0).getName();
                        dateTimeStamp = DataSource.getDateTimeStampFromFilename(filename);
                        dateTime = DataSource.getDateTimeFromFilename(filename);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                GEOSDataSource datasource = null;
                try {
                    datasource = new GEOSDataSource(fileList.get(0), default_stride[k], unpack, bandNames[k], centerWavelength[k], sensorName[k], category[k], zeroBased);
                    bandIDtoDataSource.put(bandNames[k], datasource);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                List dataChoices = datasource.getDataChoices();

                DataChoice targetDataChoice = null;

                for (int t = 0; t < dataChoices.size(); t++) {
                    DataChoice choice = (DataChoice) dataChoices.get(t);
                    String name = choice.getName();
                    if (targetList.contains(name)) {
                        targetDataChoice = choice;
                    }
                }

                doMakeDataChoice(bandNames[k], k, num, targetDataChoice, category[k]);

                MultiSpectralData msd = datasource.getMultiSpectralData(targetDataChoice);

                DataGroup cat = category[k];


                if (cat.equals(catHKMrefl)) {
                    reflHKM.add(msd);
                } else if (cat.equals(cat1KMrefl)) {
                    refl1KM.add(msd);
                } else if (cat.equals(cat2KMrefl)) {
                    refl2KM.add(msd);
                } else if (cat.equals(cat2KMemis)) {
                    emis2KM.add(msd);
                }

                num++;
            }
        }

        try {
            if (reflHKM.size() > 0) {
                reflMSDhkm = new MultiSpectralAggr((MultiSpectralData[]) reflHKM.toArray(new MultiSpectralData[1]));
            }
            if (refl1KM.size() > 0) {
                reflMSD1km = new MultiSpectralAggr((MultiSpectralData[]) refl1KM.toArray(new MultiSpectralData[1]));
            }
            if (refl2KM.size() > 0) {
                reflMSD2km = new MultiSpectralAggr((MultiSpectralData[]) refl2KM.toArray(new MultiSpectralData[1]));
            }
            if (emis2KM.size() > 0) {
                emisMSD2km = new MultiSpectralAggr((MultiSpectralData[]) emis2KM.toArray(new MultiSpectralData[1]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void doMakeDataChoice(String name, int bandIdx, int idx, DataChoice targetDataChoice, DataGroup category) {
        DataChoice dataChoice = new DataChoice(this, name, category);
        dataChoice.setDataSelection(targetDataChoice.getDataSelection());
        myDataChoices.add(dataChoice);
    }

    @Override
    public float getNadirResolution(DataChoice choice) throws Exception {
        String name = choice.getName();
        float res = 0;

        for (int k = 0; k < bandNames.length; k++) {
            if (name.equals(bandNames[k])) {
                res = nadirResolution[k];
                break;
            }
        }

        if (res == 0) {
            throw new Exception("Item not found so can't get resolution");
        } else {
            return res;
        }
    }

    @Override
    public String getDescription(DataChoice choice) {
        String name = choice.getName();

        float cntrWvln = 0;
        for (int k = 0; k < bandNames.length; k++) {
            if (name.equals(bandNames[k])) {
                cntrWvln = centerWavelength[k];
                break;
            }
        }

        if (cntrWvln == 0) {
            return null;
        } else {
            return "(" + cntrWvln + ")";
        }

    }

    @Override
    public String getDescription() {
        return "H08 AHI";
    }

    public Date getDateTime() {
        return dateTime;
    }

    @Override
    public boolean getDoFilter(DataChoice choice) {
        return true;
    }

    @Override
    public boolean getDoReproject(DataChoice choice) {
        return false;
    }

    public boolean getOverlayAsMask(DataChoice choice) {
        return false;
    }

    public Data getData(DataChoice dataChoice, DataSelection dataSelection)
            throws VisADException, RemoteException {
        String name = dataChoice.getName();
        name = bandNameToBandID.get(name);
        GEOSDataSource datasource = bandIDtoDataSource.get(name);
        List dataChoices = datasource.getDataChoices();

        DataChoice targetDataChoice = null;

        for (int t = 0; t < dataChoices.size(); t++) {
            DataChoice choice = (DataChoice) dataChoices.get(t);
            name = choice.getName();
            if (targetList.contains(name)) {
                targetDataChoice = choice;
            }
        }

        targetDataChoice.setDataSelection(dataChoice.getDataSelection());
        Data data = datasource.getData(targetDataChoice);
        CoordinateSystem cs = ((RealTupleType) ((FunctionType) data.getType()).getDomain()).getCoordinateSystem();
        DataGroup datGrp = dataChoice.getGroup();

        if (datGrp.equals(catHKMrefl) && reflMSDhkm != null) {
            reflMSDhkm.setCoordinateSystem(cs);
            reflMSDhkm.setSwathDomainSet((Linear2DSet) ((FlatField) data).getDomainSet());
        } else if (datGrp.equals(cat1KMrefl) && reflMSD1km != null) {
            reflMSD1km.setCoordinateSystem(cs);
            reflMSD1km.setSwathDomainSet((Linear2DSet) ((FlatField) data).getDomainSet());
        } else if (datGrp.equals(cat2KMrefl) && reflMSD2km != null) {
            reflMSD2km.setCoordinateSystem(cs);
            reflMSD2km.setSwathDomainSet((Linear2DSet) ((FlatField) data).getDomainSet());
        } else if (datGrp.equals(cat2KMemis) && emisMSD2km != null) {
            emisMSD2km.setCoordinateSystem(cs);
            emisMSD2km.setSwathDomainSet((Linear2DSet) ((FlatField) data).getDomainSet());
        }

        data = postProcess(targetDataChoice, data);
        return data;
    }

    boolean fileBelongsToThis(String filename) {
        return true;
    }

    Data postProcess(DataChoice choice, Data data) throws VisADException, RemoteException {
        return data;
    }

    public MultiSpectralData[] getMultiSpectralData() {
        ArrayList<MultiSpectralData> list = new ArrayList();

        if (reflMSDhkm != null) {
            list.add(reflMSDhkm);
        }
        if (reflMSD1km != null) {
            list.add(reflMSD1km);
        }
        if (reflMSD2km != null) {
            list.add(reflMSD2km);
        }
        if (emisMSD2km != null) {
            list.add(emisMSD2km);
        }

        return list.toArray(new MultiSpectralData[1]);
    }

    @Override
    public boolean canUnderstand(File[] files) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
