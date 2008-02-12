/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import visad.FlatField;
import visad.SampledSet;
import visad.RealTuple;
import visad.RealType;
import visad.RealTupleType;
import visad.VisADException;
import visad.CoordinateSystem;
import visad.FunctionType;
import visad.Real;
import visad.Set;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.awt.geom.Rectangle2D;


public class MultiSpectralData {

  SwathAdapter swathAdapter;
  SpectrumAdapter spectrumAdapter;
  CoordinateSystem cs = null;

  HashMap spectrumSelect;
  HashMap swathSelect;

  public static float init_wavenumber = 919.50f;
  

  public MultiSpectralData(SwathAdapter swathAdapter, SpectrumAdapter spectrumAdapter) {
    this.swathAdapter = swathAdapter;
    this.spectrumAdapter = spectrumAdapter;
    this.spectrumSelect = spectrumAdapter.getDefaultSubset();
    this.swathSelect = swathAdapter.getDefaultSubset();
  }

  public FlatField getSpectrum(int[] coords) 
      throws Exception, VisADException, RemoteException {
    if (coords == null) return null;
    spectrumSelect.put(SpectrumAdapter.x_dim_name, new double[] {(double)coords[0], (double)coords[0], 1.0});
    spectrumSelect.put(SpectrumAdapter.y_dim_name, new double[] {(double)coords[1], (double)coords[1], 1.0});
                                                                                                                                             
    FlatField spectrum = spectrumAdapter.getData(spectrumSelect);
                                                                                                                                             
    //-- convert to BrightnessTemp
    FunctionType f_type = (FunctionType) spectrum.getType();
    FunctionType new_type = new FunctionType(f_type.getDomain(), RealType.getRealType("BrightnessTemp"));
                                                                                                                                             
    float[][] channels = ((SampledSet)spectrum.getDomainSet()).getSamples(false);
    float[][] values = spectrum.getFloats(true);
    float[] bt_values = radianceToBrightnessTempSpectrum(values[0], channels[0]);
    FlatField new_spectrum = new FlatField(new_type, spectrum.getDomainSet());
    new_spectrum.setSamples(new float[][] {bt_values}, true);
                                                                                                                                             
    return new_spectrum;
  }

  public FlatField getSpectrum(RealTuple location) 
      throws Exception, VisADException, RemoteException {
    int[] coords = getSwathCoordinates(location, cs);
    if (coords == null) return null;
    spectrumSelect.put(SpectrumAdapter.x_dim_name, new double[] {(double)coords[0], (double)coords[0], 1.0});
    spectrumSelect.put(SpectrumAdapter.y_dim_name, new double[] {(double)coords[1], (double)coords[1], 1.0});

    FlatField spectrum = spectrumAdapter.getData(spectrumSelect);

    //-- convert to BrightnessTemp
    FunctionType f_type = (FunctionType) spectrum.getType();
    FunctionType new_type = new FunctionType(f_type.getDomain(), RealType.getRealType("BrightnessTemp"));

    float[][] channels = ((SampledSet)spectrum.getDomainSet()).getSamples(false);
    float[][] values = spectrum.getFloats(true);
    float[] bt_values = radianceToBrightnessTempSpectrum(values[0], channels[0]);
    FlatField new_spectrum = new FlatField(new_type, spectrum.getDomainSet());
    new_spectrum.setSamples(new float[][] {bt_values}, true);
    
    return new_spectrum;
  }

  public FlatField getImage(float channel, HashMap subset) 
      throws Exception, VisADException, RemoteException {
    int channelIndex = spectrumAdapter.getChannelIndexFromWavenumber(channel);
    subset.put(SpectrumAdapter.channelIndex_name, new double[] {(double)channelIndex, (double)channelIndex, 1.0});
    FlatField image = swathAdapter.getData(subset);
    cs = ((RealTupleType) ((FunctionType)image.getType()).getDomain()).getCoordinateSystem();

    //-- convert to BrightnessTemp
    FunctionType f_type = (FunctionType)image.getType();
    FunctionType new_type = new FunctionType(f_type.getDomain(), RealType.getRealType("BrightnessTemp"));
    FlatField new_image = new FlatField(new_type, image.getDomainSet());
    float[][] values = image.getFloats(true);
    float[] bt_values = radianceToBrightnessTemp(values[0], channel);
    new_image.setSamples(new float[][] {bt_values}, true);

    return new_image;
  }

  public int[] getSwathCoordinates(RealTuple location, CoordinateSystem cs) 
      throws VisADException, RemoteException {
    if (location == null) return null;
    Real[] comps = location.getRealComponents();
    if (cs == null) return null;
    float[][] xy = cs.fromReference(new float[][] {{(float)comps[1].getValue()}, {(float)comps[0].getValue()}});
    if ((Float.isNaN(xy[0][0])) || Float.isNaN(xy[1][0])) return null;
    Set domain = swathAdapter.getSwathDomain();
    int[] idx = domain.valueToIndex(xy);
    xy = domain.indexToValue(idx);
    int[] coords = new int[2];
    coords[0] = (int) xy[0][0];
    coords[1] = (int) xy[1][0];
    if ((coords[0] < 0)||(coords[1] < 0)) return null;
    return coords;
  }

  public RealTuple getEarthCoordinates(float[] xy)
      throws VisADException, RemoteException {
    float[][] tup = cs.toReference(new float[][] {{xy[0]}, {xy[1]}});
    return new RealTuple(RealTupleType.SpatialEarth2DTuple, new double[] {(double)tup[0][0], (double)tup[1][0]});
  }


  public Rectangle2D getLonLatBoundingBox(CoordinateSystem cs) {
    return null;
  }

  public static Rectangle2D getLonLatBoundingBox(FlatField field) {
    //GriddedSet domainSet = (GriddedSet) field.getDomainSet();
    return null;
  }

  public static float[] radianceToBrightnessTemp(float[] values, float channelValue) {
    float c1=1.191066E-5f;           //- mW/m2/ster/cm^-4
    float c2=1.438833f;              //- K*cm
    float nu = channelValue;         //- nu: wavenumber
    float B, K, BT;

    int n_values = values.length;
    float[] new_values = new float[n_values];
    for (int i=0; i<n_values;i++) {
      B = values[i];
      K = (c1*nu*nu*nu)/B;
      if (K == 0.0) {
        BT = B;
      } 
      else {
        BT = c2*nu/((float) (Math.log((double)((c1*nu*nu*nu)/B)+1.0f)) );
      }
      new_values[i] = BT;
    }
    return new_values;
  }

  public static float[] radianceToBrightnessTempSpectrum(float[] values, float[] channelValues) {
    //- Converts radiances [mW/ster/m2/cm^-1] to BT [K]
    //-  Input: nu  array of wavenmbers [cm^-1]
    //-          B   radiances [mW/ster/m2/cm^-1]
    //-  Output: bt brightness temperature in [K]
    //-   Paolo Antonelli
    //-   Wed Feb 25 16:43:05 CST 1998

    float c1=1.191066E-5f;           //- mW/m2/ster/cm^-4
    float c2=1.438833f;              //- K*cm

    float nu;                        //- wavenumber
    float B, BT;

    int n_values = values.length;
    float[] new_values = new float[n_values];
    for (int i=0; i<n_values; i++) {
      nu = channelValues[i];
      B = values[i];
      BT = c2*nu/((float) (Math.log(((c1*nu*nu*nu)/B)+1.0f)) );
      new_values[i] = BT;
    }
    return new_values;
  }

}
