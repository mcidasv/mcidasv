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
import java.util.List;
import java.util.ArrayList;

import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.adapter.AtmSoundingAdapter;
import edu.wisc.ssec.adapter.SwathSoundingData;
import edu.wisc.ssec.adapter.MultiDimensionReader;
import edu.wisc.ssec.adapter.RangeProcessor;
import visad.VisADException;
import visad.Data;

import java.rmi.RemoteException;
import java.util.Date;


public class AtmSoundingDataSource extends DataSource {

    String dateTimeStamp = null;

    Date dateTime = null;

    String description = null;

    File[] files = null;

    SwathSoundingData data = null;

    ArrayList<SwathSoundingData> mySoundingDatas = new ArrayList<SwathSoundingData>();


    public AtmSoundingDataSource(File directory) throws Exception {
        this(directory.listFiles());
    }

    public AtmSoundingDataSource(File[] files) throws Exception {

        if (!canUnderstand(files)) {
            throw new Exception("AtmSoundingDataSource doesnt understand this: ");
        }

        this.files = files;
        int numFiles = files.length;
        File file = files[0];

        dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName());
        description = DataSource.getDescriptionFromFilename(file.getName());
        dateTime = DataSource.getDateTimeFromFilename(file.getName());

        try {
            init(files);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void init(File[] files) throws Exception {
    }

    public SwathSoundingData buildAdapter(MultiDimensionReader reader, String xtrack, String track, String levelIndex, String levelsName, float[] levelValues,
                                          String array, String range, String geoXtrack, String geoTrack,
                                          String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                          String fillValueName) {

        HashMap metadata = AtmSoundingAdapter.getEmptyMetadataTable();
        metadata.put(AtmSoundingAdapter.levels_name, levelsName);
        metadata.put(AtmSoundingAdapter.array_name, array);
        metadata.put(AtmSoundingAdapter.range_name, range);
        metadata.put(AtmSoundingAdapter.levelIndex_name, levelIndex);
        metadata.put(AtmSoundingAdapter.x_dim_name, xtrack);
        metadata.put(AtmSoundingAdapter.y_dim_name, track);
        metadata.put(AtmSoundingAdapter.levelValues, levelValues);
        if (arrayDims != null) {
            metadata.put(AtmSoundingAdapter.array_dimension_names, arrayDims);
        }
        if (fillValueName != null) {
            metadata.put(AtmSoundingAdapter.fill_value_name, fillValueName);
        }

        AtmSoundingAdapter soundingAdapter = new AtmSoundingAdapter(reader, metadata);

        try {
            soundingAdapter.setRangeProcessor(RangeProcessor.createRangeProcessor(reader, metadata));
        } catch (Exception e) {
            e.printStackTrace();
        }

        metadata = SwathAdapter.getEmptyMetadataTable();

        metadata.put(SwathAdapter.xtrack_name, xtrack);
        metadata.put(SwathAdapter.track_name, track);
        metadata.put(SwathAdapter.geo_xtrack_name, geoXtrack);
        metadata.put(SwathAdapter.geo_track_name, geoTrack);
        metadata.put(SwathAdapter.array_name, array);
        metadata.put(SwathAdapter.range_name, range);
        metadata.put(SwathAdapter.lon_array_name, lonArray);
        metadata.put(SwathAdapter.lat_array_name, latArray);
        metadata.put(SwathAdapter.lon_array_dimension_names, lonArrayDims);
        metadata.put(SwathAdapter.lat_array_dimension_names, latArrayDims);
        metadata.put(AtmSoundingAdapter.levelIndex_name, levelIndex);
        if (arrayDims != null) {
            metadata.put(SwathAdapter.array_dimension_names, arrayDims);
        }
        if (fillValueName != null) {
            metadata.put(SwathAdapter.fill_value_name, fillValueName);
        }

        SwathAdapter swathAdapter = new SwathAdapter(reader, metadata);

        SwathSoundingData data = new SwathSoundingData(swathAdapter, soundingAdapter);

        return data;
    }

    public SwathSoundingData getSwathSoundingData(DataChoice dataChoice) {
        for (int k = 0; k < myDataChoices.size(); k++) {
            if (myDataChoices.get(k).equals(dataChoice)) {
                return mySoundingDatas.get(k);
            }
        }
        return null;
    }

    public String getDescription() {
        return description;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public boolean getDoFilter(DataChoice choice) {
        return true;
    }

    public boolean getOverlayAsMask(DataChoice choice) {
        return false;
    }

    public boolean isAtmRetrieval() {
        return true;
    }


    public Data getData(DataChoice dataChoice, DataSelection dataSelection)
            throws VisADException, RemoteException {
        try {
            data = getSwathSoundingData(dataChoice);
            return data.getImage(data.getDefaultSubset());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static AtmSoundingDataSource makeDataSource() {
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
