/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2021
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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * 
 */
public class RadianceToBTbyLUT extends RangeProcessor {
   
   private static final Logger logger =
       LoggerFactory.getLogger(RadianceToBTbyLUT.class);
   
   LUTtransform lutCal;
   
   
   public RadianceToBTbyLUT(NetCDFFile reader, Map<String, Object> metadata, String radLUTname, String btLUTname) throws Exception {
      super(reader, metadata);
      
      int numLUTvals = (reader.getDimensionLengths(radLUTname))[0];
      float[] radLUT = reader.getFloatArray(radLUTname, new int[] {0}, new int[] {numLUTvals}, new int[] {1});
      float[] btLUT = reader.getFloatArray(btLUTname, new int[] {0}, new int[] {numLUTvals}, new int[] {1});
      
      lutCal = new LUTtransform(radLUT, btLUT);
   }
   
   public RadianceToBTbyLUT(NetCDFFile reader, Map<String, Object> metadata) throws Exception {
      super(reader, metadata);
   }
   
   /**
    * calls super to unscale radiances then converts to BT
    * 
    */
   public float[] processRange(short[] values, Map<String, double[]> subset) {
      float[] radiances = super.processRange(values, subset);
      
      float[] brightnessTemps = null;
      
      try {
         brightnessTemps = lutCal.radianceToBrightnessTemp(radiances);
      } catch (Exception e) {
         logger.error("Problem processing range", e);
      }
      
      return brightnessTemps;
   }
}
