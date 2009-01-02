/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import visad.Data;
import visad.FlatField;
import visad.VisADException;
import visad.CoordinateSystem;
import visad.RealType;
import visad.RealTupleType;
import visad.SetType;
import visad.Gridded1DSet;
import visad.Gridded1DDoubleSet;
import visad.MathType;
import visad.Gridded3DSet;
import visad.Gridded2DSet;
import visad.FunctionType;
import visad.Set;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;


public class TrackAdapter {
   RealTupleType domainType;
   ArrayAdapter lonAdapter;
   ArrayAdapter latAdapter;
   ArrayAdapter rngAdapter;

   public TrackAdapter() {
   }

   public TrackAdapter(ArrayAdapter lonAdapter, ArrayAdapter latAdapter, ArrayAdapter rangeAdapter) throws VisADException {
     this.lonAdapter = lonAdapter;
     this.latAdapter = latAdapter;
     this.rngAdapter = rangeAdapter;
     //domainType = RealTupleType.SpatialEarth2DTuple;
     domainType = RealTupleType.SpatialEarth3DTuple;
   }

   public FlatField getData(Object subset) throws VisADException, RemoteException {
     
     float[] lonValues = null;
     float[] latValues = null;
     float[] rngValues = null;

     try {
       lonValues = (lonAdapter.getData(subset).getFloats())[0];
       latValues = (latAdapter.getData(subset).getFloats())[0];
       rngValues = (rngAdapter.getData(subset).getFloats())[0];
     } 
     catch (Exception e) {
       e.printStackTrace();
       System.out.println(e);
       return null;
     }

     for (int k=0; k< rngValues.length; k++) rngValues[k] *= 1000.0;
     Gridded3DSet set = new Gridded3DSet(domainType, new float[][] {lonValues, latValues, rngValues}, lonValues.length);

     FlatField field = new FlatField(new FunctionType(domainType, rngAdapter.getMathType().getRange()), set);
     //FlatField field = new FlatField(new FunctionType(domainType, RealType.Altitude), set);
     field.setSamples(new float[][] {rngValues});
     return field;
   }

   public HashMap getDefaultSubset() {
     return lonAdapter.getDefaultSubset();
   }
}
