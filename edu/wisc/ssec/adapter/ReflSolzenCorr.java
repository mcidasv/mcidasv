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

import visad.FlatField;
import java.util.HashMap;

/**
 *
 * 
 */
public class ReflSolzenCorr extends RangeProcessor {
   
   ArrayAdapter solzenAdapter;
   
   
   public ReflSolzenCorr(MultiDimensionReader reader, HashMap metadata, ArrayAdapter solzenAdapter) throws Exception {
      super(reader, metadata);
      this.solzenAdapter = solzenAdapter;
   }
   
   public ReflSolzenCorr(MultiDimensionReader reader, HashMap metadata) throws Exception {
      super(reader, metadata);
   }
   
   /**
    * super returns uncorrected reflectance.
    * 
    */
   public float[] processRange(short[] values, HashMap subset) {
      float[] refls = super.processRange(values, subset);
      float[] solzen = null;
      
      try {
         solzen = ((FlatField)solzenAdapter.getData(subset)).getFloats()[0];
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      for (int k=0; k<refls.length; k++) {
         float refl = refls[k];
         float solz = solzen[k];
         if (solz < 89.0f) {
            refls[k] = refl/((float)Math.cos((Math.PI/180.0)*solz));
         }
      }
      
      return refls;
   }
}
