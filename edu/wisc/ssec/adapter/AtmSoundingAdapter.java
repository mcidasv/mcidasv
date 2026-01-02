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

package edu.wisc.ssec.adapter;

import java.rmi.RemoteException;
import java.util.HashMap;

import visad.FlatField;

import visad.FunctionType;
import visad.Gridded1DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.SingletonSet;
import visad.VisADException;

public class AtmSoundingAdapter extends MultiDimensionAdapter {

    public static String levels_name = "Levels";
    public static String levelIndex_name = "levelIndex";
    public static String FOVindex_name = "FOVindex";
    public static String levelUnit = "hPa";
    public static String channelType = "wavenumber";
    public static String array_name = "array_name";
    public static String array_dimension_names = "array_dimension_names";
    public static String range_name = "range_name";
    public static String x_dim_name = "x_dim"; //- 2 spatial dimensions, x fastest varying
    public static String y_dim_name = "y_dim"; //-----------------------------------------
    public static String time_dim_name = "time_dim";
    public static String ancillary_file_name = "ancillary_file";
    public static String levelValues = "levelValues";


    public static HashMap getEmptyMetadataTable() {
        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put(array_name, null);
        metadata.put(range_name, null);
        metadata.put(levelIndex_name, null);
        metadata.put(ancillary_file_name, null);
        metadata.put(x_dim_name, null);
        metadata.put(y_dim_name, null);
        metadata.put(time_dim_name, null);
        metadata.put(levelUnit, null);
        metadata.put(levelValues, null);

        return metadata;
    }

    public static HashMap<String, double[]> getEmptySubset() {
        HashMap<String, double[]> subset = new HashMap<String, double[]>();
        subset.put(x_dim_name, new double[3]);
        subset.put(y_dim_name, new double[3]);
        subset.put(levelIndex_name, new double[3]);
        return subset;
    }

    int numLevels;
    float[] levels;
    int levelIndex = -1;
    SampledSet domainSet;
    RealType levelRealType;
    FunctionType soundingType;

    public AtmSoundingAdapter(MultiDimensionReader reader, HashMap metadata) {
        super(reader, metadata);
        this.init();
    }

    private void init() {
        for (int k = 0; k < array_rank; k++) {
            String name = (String) metadata.get(levelIndex_name);
            if (name != null) {
                if (name.equals(array_dim_names[k])) {
                    levelIndex = k;
                }
            }
        }

        //numLevels = computeNumLevels();

        try {
            levels = makeLevels();
            numLevels = computeNumLevels();
            domainSet = makeDomainSet();
            rangeType = makeSoundingRangeType();
            soundingType = new FunctionType(levelRealType, rangeType);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("cannot create sounding domain");
        }

    }

    public int computeNumLevels() {
        if (levelIndex == -1) {
            //return 1;
            return levels.length;
        } else {
            return array_dim_lengths[levelIndex];
        }
    }

    public Set makeDomain(Object subset) throws Exception {
        return domainSet;
    }

    public SampledSet getDomainSet() throws Exception {
        return domainSet;
    }

    public FlatField makeFlatField(Set domainSet, float[][] range) throws VisADException, RemoteException {
        if (domainSet.getLength() != range[0].length) {
            float fval = range[0][0];
            range[0] = new float[domainSet.getLength()];
            java.util.Arrays.fill(range[0], fval);
        }
        FlatField f_field = makeFlatField(domainSet);
        f_field.setSamples(range, false);
        return f_field;
    }

    private SampledSet makeDomainSet() throws Exception {
        RealType domainType = makeSoundingDomainType();
        levels = makeLevels();
        if (numLevels == 1) {
            domainSet = new SingletonSet(new RealTupleType(domainType), new double[]{(double) levels[0]}, null, null, null);
        } else {
            domainSet = new Gridded1DSet(domainType, new float[][]{levels}, numLevels);
        }
        return domainSet;
    }

    public float[] makeLevels() throws Exception {
        float[] levels = null;
        if (metadata.get(levelValues) == null) {
            levels = reader.getFloatArray((String) metadata.get(levels_name),
                    new int[]{0}, new int[]{numLevels}, new int[]{1});
        } else {
            levels = (float[]) metadata.get(levelValues);
        }

        return levels;
    }

    public RealType makeSoundingDomainType() throws Exception {
        levelRealType = RealType.getRealType((String) metadata.get(levels_name), null);
        return levelRealType;
    }

    public RealType makeSoundingRangeType() throws Exception {
        RealType soundingRangeType = RealType.getRealType((String) metadata.get(array_name), null);
        return soundingRangeType;
    }

    public HashMap getDefaultSubset() {
        HashMap<String, double[]> subset = AtmSoundingAdapter.getEmptySubset();

        double[] coords = (double[]) subset.get(y_dim_name);
        coords[0] = 1.0;
        coords[1] = 1.0;
        coords[2] = 1.0;
        subset.put(y_dim_name, coords);

        coords = (double[]) subset.get(x_dim_name);
        coords[0] = 1.0;
        coords[1] = 1.0;
        coords[2] = 1.0;
        subset.put(x_dim_name, coords);

        coords = (double[]) subset.get(levelIndex_name);
        coords[0] = 0.0;
        coords[1] = (double) (numLevels - 1);
        coords[2] = 1.0;
        subset.put(levelIndex_name, coords);

        return subset;
    }

    public int getLevelIndexFromLevel(float level) throws Exception {
        int idx = (domainSet.valueToIndex(new float[][]{{level}}))[0];
        return idx;
    }

    public float getLevelFromLevelIndex(int index) throws Exception {
        return (domainSet.indexToValue(new int[]{index}))[0][0];
    }

    public int getNumLevels() {
        return numLevels;
    }

    public float[] getLevels() {
        return levels;
    }

    public static void main(String[] args) throws Exception {

        HashMap metadata = AtmSoundingAdapter.getEmptyMetadataTable();
        metadata.put(AtmSoundingAdapter.levels_name, "vertPressLevel");
        metadata.put(AtmSoundingAdapter.array_name, "TAirStd");
        metadata.put(AtmSoundingAdapter.range_name, "Temperature");
        metadata.put(AtmSoundingAdapter.levelIndex_name, "fakeDim10");
        metadata.put(AtmSoundingAdapter.x_dim_name, "fakeDim9");
        metadata.put(AtmSoundingAdapter.y_dim_name, "fakeDim8");

        float[] vals = new float[101];
        for (int k = 0; k < vals.length; k++) {
            vals[k] = 10f + k * 10f;
        }
        metadata.put(levelValues, vals);

        NetCDFFile reader = new NetCDFFile("/Users/rink/data/AIRS/AIRS.2005.08.28.103.atm_prof_rtv_npc030.hdf");
        AtmSoundingAdapter soundingAdapter = new AtmSoundingAdapter(reader, metadata);

        HashMap subset = soundingAdapter.getDefaultSubset();
        visad.FlatField sounding = soundingAdapter.getData(subset);

        metadata = SwathAdapter.getEmptyMetadataTable();

        metadata.put(SwathAdapter.xtrack_name, "fakeDim9");
        metadata.put(SwathAdapter.track_name, "fakeDim8");
        metadata.put(SwathAdapter.geo_xtrack_name, "fakeDim3");
        metadata.put(SwathAdapter.geo_track_name, "fakeDim2");
        metadata.put(SwathAdapter.array_name, "TAirStd");
        metadata.put(SwathAdapter.range_name, "Temperature");
        metadata.put(SwathAdapter.lon_array_name, "Longitude");
        metadata.put(SwathAdapter.lat_array_name, "Latitude");
        metadata.put(SwathAdapter.lon_array_dimension_names, new String[]{"fakeDim2", "fakeDim3"});
        metadata.put(SwathAdapter.lat_array_dimension_names, new String[]{"fakeDim0", "fakeDim1"});
        metadata.put(AtmSoundingAdapter.levelIndex_name, "fakeDim10");

        SwathAdapter swathAdapter = new SwathAdapter(reader, metadata);

        SwathSoundingData adapter = new SwathSoundingData(swathAdapter, soundingAdapter);
        subset = adapter.getDefaultSubset();
        visad.FlatField ff = adapter.getData(subset);

    }
}
