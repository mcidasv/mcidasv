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

import visad.Data;
import visad.FlatField;
import visad.VisADException;
import visad.CoordinateSystem;
import visad.RealType;
import visad.Real;
import visad.MathType;
import visad.IntegerNDSet;
import visad.GriddedSet;
import visad.LinearNDSet;
import visad.LinearSet;
import visad.Linear1DSet;
import visad.Linear2DSet;
import visad.Linear3DSet;
import visad.RealTupleType;
import visad.SetType;
import visad.FunctionType;
import visad.Set;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;


public class ArrayAdapter extends MultiDimensionAdapter {

   RealTupleType domainType;
   FunctionType ftype;
   GriddedSet domain;
   RealType[] realTypes;

   public ArrayAdapter() {
   }

   public ArrayAdapter(MultiDimensionReader reader, HashMap metadata) {
     super(reader, metadata);
     init();
   }

   private void init() {
     try {
     realTypes = new RealType[array_rank];
     int[] lengths = new int[array_rank];
     for (int i=0; i<array_rank; i++) {
       realTypes[i] = RealType.getRealType(array_dim_names[i]);
       lengths[i] = array_dim_lengths[i];
     }

     domainType = new RealTupleType(realTypes);

     String rangeName = null;
     if (metadata.get("range_name") != null) {
        rangeName = (String)metadata.get("range_name");
     } 
     else {
        rangeName = (String)metadata.get("array_name");
     }
     rangeType = RealType.getRealType(rangeName);
     ftype = new FunctionType(domainType, rangeType);
     domain = IntegerNDSet.create(domainType, lengths);
     }
     catch (Exception e) {
       e.printStackTrace();
     }
   }

   public GriddedSet getDomain() {
     return domain;
   }

   public FunctionType getMathType() {
     return ftype;
   }

   public GriddedSet makeDomain(Object subset) throws Exception {
     if (subset == null) {
        subset = getDefaultSubset();
     }

     double[] first = new double[array_rank];
     double[] last = new double[array_rank];
     int[] length = new int[array_rank];

     for (int kk=0; kk<array_rank; kk++) {
       RealType rtype = realTypes[kk];
       double[] coords = (double[]) ((HashMap)subset).get(dimNameMap.get(rtype.getName()));
       if (array_dim_lengths[kk] == 1) {
         coords[0] = 0;
         coords[1] = 0;
         coords[2] = 1;
       }
       first[kk] = coords[0];
       last[kk] = coords[1];
       length[kk] = (int) ((last[kk] - first[kk])/coords[2] + 1);
     }

     LinearSet lin_set = LinearNDSet.create(domainType, first, last, length);
     GriddedSet new_domain = null;

     if (array_rank == 1) {
          new_domain = (Linear1DSet) lin_set;
     } else if (array_rank == 2) {
          new_domain = (Linear2DSet) lin_set;
     } else if (array_rank == 3) {
          new_domain = (Linear3DSet) lin_set;
     } else {
          new_domain = (LinearNDSet) lin_set;
     } 

     return new_domain;
   }

   public HashMap getDefaultSubset() {
     HashMap map = getEmptySubset();
     for (int i=0; i<array_rank; i++) {
       double[] coords = (double[]) map.get(dimNameMap.get(array_dim_names[i]));
       coords[0] = 0;
       coords[1] = array_dim_lengths[i] - 1;
       coords[2] = 1;
     }
     return map;
   }

   public HashMap getEmptySubset() {
     HashMap<String, double[]> subset = new HashMap<String, double[]>();
     for (int i=0; i<array_rank; i++) {
       subset.put(dimNameMap.get(array_dim_names[i]), new double[3]);
     }
     return subset;
   }

}
