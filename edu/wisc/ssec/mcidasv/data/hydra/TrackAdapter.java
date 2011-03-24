/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2011
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
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
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import java.rmi.RemoteException;
import java.util.HashMap;

import visad.FlatField;
import visad.FunctionType;
import visad.Gridded3DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.VisADException;

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
     if (rangeAdapter != null) {
       domainType = RealTupleType.SpatialEarth3DTuple;
     }
     else {
       domainType = RealTupleType.SpatialEarth3DTuple;
     }
   }

   public FlatField getData(Object subset) throws VisADException, RemoteException {
     
     float[] lonValues = null;
     float[] latValues = null;
     float[] rngValues = null;

     try {
       lonValues = (lonAdapter.getData(subset).getFloats())[0];
       latValues = (latAdapter.getData(subset).getFloats())[0];
       if (rngAdapter != null) {
         rngValues = (rngAdapter.getData(subset).getFloats())[0];
       }
     } 
     catch (Exception e) {
       e.printStackTrace();
       return null;
     }

     FlatField field = null;
     if (rngAdapter != null) {
       for (int k=0; k< rngValues.length; k++) {
         rngValues[k] *= 1000.0;
       }
       Gridded3DSet set = new Gridded3DSet(domainType, new float[][] {lonValues, latValues, rngValues}, lonValues.length);
       field = new FlatField(new FunctionType(domainType, rngAdapter.getMathType().getRange()), set);
       field.setSamples(new float[][] {rngValues}, false);
     }
     else {
       rngValues = new float[lonValues.length];
       for (int k=0; k< rngValues.length; k++) rngValues[k] = 0f;
       Gridded3DSet set = new Gridded3DSet(domainType, new float[][] {lonValues, latValues, rngValues}, lonValues.length);
       field = new FlatField(new FunctionType(domainType, RealType.Generic), set);
       field.setSamples(new float[][] {rngValues}, false);
     }
     return field;
   }

   public HashMap getDefaultSubset() {
     return lonAdapter.getDefaultSubset();
   }
}
