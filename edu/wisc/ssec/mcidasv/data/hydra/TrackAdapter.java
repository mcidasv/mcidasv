/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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
import visad.SetType;
import visad.VisADException;
import visad.Set;

public class TrackAdapter extends MultiDimensionAdapter {
   RealTupleType domainType;
   ArrayAdapter rngAdapter;
   TrackDomain trackDomain;

   int listIndex = 0;

   String adapterName = null;

   public TrackAdapter() {
   }

   public TrackAdapter(TrackDomain trackDomain, ArrayAdapter rangeAdapter) throws VisADException {
     this.trackDomain = trackDomain;
     this.rngAdapter = rangeAdapter;
   }

   public Set makeDomain(Object subset) throws Exception {
     throw new Exception("Unimplemented");
   } 

   public FlatField getData(Object subset) throws VisADException, RemoteException {
     
     float[] rngValues = null;

     Set set = trackDomain.makeDomain(subset);

     domainType = ((SetType)set.getType()).getDomain();

     try {
       rngValues = (rngAdapter.getData(subset).getFloats())[0];
     }
     catch (Exception e) {
       e.printStackTrace();
       return null;
     }

     FlatField field = new FlatField(new FunctionType(domainType, rngAdapter.getMathType().getRange()), set);
     field.setSamples(new float[][] {rngValues}, false);

     return field;
   }

   public void setName(String name) {
     adapterName = name;
   }

   public String getArrayName() {
     if (adapterName != null) {
       return adapterName;
     }
     else {
       return rngAdapter.getArrayName();
     }
   }

   void setListIndex(int idx) {
     listIndex = idx;
   }

   public HashMap getDefaultSubset() {
     HashMap subset = rngAdapter.getDefaultSubset();
     if (subset.containsKey("VertDim")) {
       double[] coords = (double[]) ((HashMap)subset).get("VertDim");
       if (coords != null) {
         coords[0] = listIndex;
         coords[1] = listIndex;
         coords[2] = 1;
       }
     }
     return subset;
   }

   public HashMap getSubsetFromLonLatRect(double minLat, double maxLat,
                                          double minLon, double maxLon) {
      return trackDomain.getSubsetFromLonLatRect(getDefaultSubset(), minLat, maxLat, minLon, maxLon);
   }

   public HashMap getSubsetFromLonLatRect(double minLat, double maxLat,
                                          double minLon, double maxLon,
                                          int xStride, int yStride, int zStride) {
      return trackDomain.getSubsetFromLonLatRect(getDefaultSubset(), minLat, maxLat, minLon, maxLon,
                                                 xStride, yStride, zStride);
   }

}
