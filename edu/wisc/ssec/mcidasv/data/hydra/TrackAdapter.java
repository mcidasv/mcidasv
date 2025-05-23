/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
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

package edu.wisc.ssec.mcidasv.data.hydra;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import visad.FlatField;
import visad.FunctionType;
import visad.RealTupleType;
import visad.Set;
import visad.SetType;
import visad.VisADException;

public class TrackAdapter extends MultiDimensionAdapter {
   
   private static final Logger logger =
       LoggerFactory.getLogger(TrackAdapter.class);
   
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

   public Set makeDomain(Map<String, double[]> subset) throws Exception {
     throw new Exception("Unimplemented");
   } 

   public FlatField getData(Map<String, double[]> subset) throws VisADException, RemoteException {

     try {
         float[] rngValues = null;
         Set set = trackDomain.makeDomain(subset);

         domainType = ((SetType)set.getType()).getDomain();

         rngValues = (rngAdapter.getData(subset).getFloats())[0];
         FlatField field = new FlatField(new FunctionType(domainType, rngAdapter.getMathType().getRange()), set);
         field.setSamples(new float[][] {rngValues}, false);

         return field;
     } catch (Exception e) {
       logger.error("Problem getting data", e);
       return null;
     }
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

   public Map<String, double[]> getDefaultSubset() {
     Map<String, double[]> subset = rngAdapter.getDefaultSubset();
     if (subset.containsKey("VertDim")) {
       double[] coords = subset.get("VertDim");
       if (coords != null) {
         coords[0] = listIndex;
         coords[1] = listIndex;
         coords[2] = 1;
       }
     }
     return subset;
   }

   public Map<String, double[]> getSubsetFromLonLatRect(double minLat, double maxLat,
                                          double minLon, double maxLon) {
      return trackDomain.getSubsetFromLonLatRect(getDefaultSubset(), minLat, maxLat, minLon, maxLon);
   }

   public Map<String, double[]> getSubsetFromLonLatRect(double minLat, double maxLat,
                                          double minLon, double maxLon,
                                          int xStride, int yStride, int zStride) {
      return trackDomain.getSubsetFromLonLatRect(getDefaultSubset(), minLat, maxLat, minLon, maxLon,
                                                 xStride, yStride, zStride);
   }

}
