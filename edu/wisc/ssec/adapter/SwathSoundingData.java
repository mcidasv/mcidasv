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

package edu.wisc.ssec.adapter;

import java.rmi.RemoteException;
import java.util.HashMap;

import visad.CoordinateSystem;
import visad.FlatField;
import visad.FunctionType;
import visad.Linear2DSet;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.Set;
import visad.VisADException;

public class SwathSoundingData extends MultiDimensionAdapter {

  SwathAdapter swathAdapter = null;
  AtmSoundingAdapter soundingAdapter = null;
  CoordinateSystem cs = null;

  HashMap soundingSelect = null;
  String sensorName = null;
  String platformName = null;
  String paramName = null;
  String inputParamName = null;
  String name = null;

  public float init_level = 700f;
  public String init_bandName = null;

  float[] dataRange = new float[] {180f, 320f};

  
  public SwathSoundingData(SwathAdapter swathAdapter, AtmSoundingAdapter soundingAdapter,
                           String inputParamName, String paramName, String sensorName, String platformName) {
    this.swathAdapter = swathAdapter;
    this.soundingAdapter = soundingAdapter;
    this.paramName = paramName;
    this.inputParamName = inputParamName;
    this.name = swathAdapter.getArrayName();

    if (soundingAdapter != null) {
      this.soundingSelect = soundingAdapter.getDefaultSubset();
      try {
        setInitialLevel(init_level);
      } 
      catch (Exception e) {
        e.printStackTrace();
        System.out.println("could not initialize initial wavenumber");
      }
    }

    this.sensorName = sensorName;
    this.platformName = platformName;
  }

  public SwathSoundingData(SwathAdapter swathAdapter, AtmSoundingAdapter soundingAdapter,
                           String sensorName, String platformName) {
    this(swathAdapter, soundingAdapter, "Radiance", "BrightnessTemp", sensorName, platformName);
  }

  public SwathSoundingData(SwathAdapter swathAdapter, AtmSoundingAdapter soundingAdapter) {
    this(swathAdapter, soundingAdapter, null, null);
  }

  public SwathSoundingData() {
    this(null, null, null, null);
  }

  public FlatField getSounding(int[] coords) 
      throws Exception, VisADException, RemoteException {
    if (coords == null) return null;
    if (soundingAdapter == null) return null;
    soundingSelect.put(AtmSoundingAdapter.x_dim_name, new double[] {(double)coords[0], (double)coords[0], 1.0});
    soundingSelect.put(AtmSoundingAdapter.y_dim_name, new double[] {(double)coords[1], (double)coords[1], 1.0});

    FlatField sounding = soundingAdapter.getData(soundingSelect);
    return sounding;
  }

  public FlatField getSounding(RealTuple location) 
      throws Exception, VisADException, RemoteException {
    if (soundingAdapter == null) return null;
    int[] coords = getSwathCoordinates(location, cs);
    if (coords == null) return null;
    soundingSelect.put(AtmSoundingAdapter.x_dim_name, new double[] {(double)coords[0], (double)coords[0], 1.0});
    soundingSelect.put(AtmSoundingAdapter.y_dim_name, new double[] {(double)coords[1], (double)coords[1], 1.0});

    FlatField sounding = soundingAdapter.getData(soundingSelect);
    return sounding;
  }

  public FlatField getImage(HashMap subset) 
    throws Exception, VisADException, RemoteException {
    FlatField image = swathAdapter.getData(subset);
    cs = ((RealTupleType) ((FunctionType)image.getType()).getDomain()).getCoordinateSystem();

    int levelIndex = (int) ((double[])subset.get(AtmSoundingAdapter.levelIndex_name))[0];
    float level = soundingAdapter.getLevelFromLevelIndex(levelIndex);

    return image;
    //return convertImage(image, level, paramName);
  }

  public FlatField getImage(float level, HashMap subset) 
      throws Exception, VisADException, RemoteException {
    if (soundingAdapter == null) { 
       return getImage(subset);
    }
    int levelIndex = soundingAdapter.getLevelIndexFromLevel(level);
    subset.put(AtmSoundingAdapter.levelIndex_name, new double[] {(double)levelIndex, (double)levelIndex, 1.0});
    FlatField image = swathAdapter.getData(subset);
    cs = ((RealTupleType) ((FunctionType)image.getType()).getDomain()).getCoordinateSystem();
    return image;
    //return convertImage(image, channel, paramName);
  }

  public FlatField getData(Object subset) throws Exception {
    return getImage((HashMap)subset);
  }

  public Set makeDomain(Object subset) throws Exception {
    throw new Exception("makeDomain unimplented");
  } 


  public void setDataRange(float[] range) {
    dataRange = range;
  }

  public float[] getDataRange() {
    return dataRange;
  }

  public String getParameter() {
    return paramName;
  }

  public String getName() {
    return name;
  }

  public CoordinateSystem getCoordinateSystem() {
    return cs;
  }

  public void setCoordinateSystem(CoordinateSystem cs) {
    this.cs = cs;
  }

  public float[] getSoundingLevels() {
    return soundingAdapter.getLevels();
  }

  public void setInitialLevel(float val) {
    init_level = val;
  }

  public int[] getSwathCoordinates(RealTuple location, CoordinateSystem cs) 
      throws VisADException, RemoteException {
    if (location == null) return null;
    if (cs == null) return null;
    Real[] comps = location.getRealComponents();
    //- trusted: latitude:0, longitude:1
    float lon = (float) comps[1].getValue();
    float lat = (float) comps[0].getValue();
    if (lon < -180) lon += 360f;
    if (lon > 180) lon -= 360f;
    float[][] xy = cs.fromReference(new float[][] {{lon}, {lat}});
    if ((Float.isNaN(xy[0][0])) || Float.isNaN(xy[1][0])) return null;
    Set domain = swathAdapter.getDatasetDomain();
    
    int[] idx = domain.valueToIndex(xy);
    int[] lens = ((Linear2DSet)domain).getLengths();
    int lenX = lens[0];
    int lenY = lens[1];
    int[] coords = new int[2];
    coords[0] = idx[0] % lenX;
    coords[1] = idx[0]/lenX;
    
    if ((coords[0] < 0)||(coords[1] < 0)) return null;
    
    return coords;
  }

  public RealTuple getEarthCoordinates(float[] xy)
      throws VisADException, RemoteException {
    float[][] tup = cs.toReference(new float[][] {{xy[0]}, {xy[1]}});
    return new RealTuple(RealTupleType.SpatialEarth2DTuple, new double[] {(double)tup[0][0], (double)tup[1][0]});
  }

  public int getLevelIndexFromLevel(float level) throws Exception {
    return soundingAdapter.getLevelIndexFromLevel(level);
  }

  public float getLevelFromLevelIndex(int index) throws Exception {
    return soundingAdapter.getLevelFromLevelIndex(index);
  }

  public HashMap getDefaultSubset() {
    HashMap subset = swathAdapter.getDefaultSubset();
    double levIdx=0;

    try {
       levIdx = soundingAdapter.getLevelIndexFromLevel(init_level);
    }
    catch (Exception e) {
      System.out.println("couldn't get levIdx, using zero");
    }
      
    subset.put(AtmSoundingAdapter.levelIndex_name, new double[] {levIdx, levIdx, 1});
    return subset;
  }
 

  public AtmSoundingAdapter getAtmSoundingAdapter() {
    return soundingAdapter;
  }
}
