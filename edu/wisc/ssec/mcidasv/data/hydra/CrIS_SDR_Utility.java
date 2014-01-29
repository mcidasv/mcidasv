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

public class CrIS_SDR_Utility {

   public static int[][] ifov_order = new int[][] {new int[] {2,2}, new int[] {1,2}, new int[] {0,2}, 
                                                   new int[] {2,1}, new int[] {1,1}, new int[] {0,1},
                                                   new int[] {2,0}, new int[] {1,0}, new int[] {0,0}};

   public static int LW_CHANNELS = 717;
   public static int MW_CHANNELS = 437;
   public static int SW_CHANNELS = 163;
   
   public static float LW_INIT_SR = 649.75f;
   public static float MW_INIT_SR = 1210.0f;
   public static float SW_INIT_SR = 2155.0f;
   
   public static float LW_SR_INCR = 0.625f;
   public static float MW_SR_INCR = 1.25f;
   public static float SW_SR_INCR = 2.5f;

   public static float getWavenumberStart(String name) {
     if (name.endsWith("LW")) {
       return LW_INIT_SR;
     }
     else if (name.endsWith("MW")) {
       return MW_INIT_SR;
     }
     else if (name.endsWith("SW")) {
       return SW_INIT_SR;
     }
     else {
       return Float.NaN;
     }
   }

   public static float getWavenumberIncrement(String name) {
     if (name.endsWith("LW")) {
       return LW_SR_INCR;
     }
     else if (name.endsWith("MW")) {
       return MW_SR_INCR;
     }
     else if (name.endsWith("SW")) {
       return SW_SR_INCR;
     }
     else {
       return Float.NaN;
     }
   }

   public static int getNumChannels(String name) {
     if (name.endsWith("LW")) {
       return LW_CHANNELS;
     }
     else if (name.endsWith("MW")) {
       return MW_CHANNELS;
     }
     else if (name.endsWith("SW")) {
       return SW_CHANNELS;
     }
     else {
       return -1;
     }
   }

   public static float[] psuedoScanReorder(float[] values, int numElems, int numLines) {
     float[] new_values = new float[values.length];
      for (int j=0; j<numLines/3; j++) { //- loop over EFOVs or FORs
        for (int i=0; i<numElems/3; i++) {
          int i2 = i*3;
          int j2 = j*3;
          for (int jj=0; jj<3; jj++) {  //- loop over IFOVs
            for (int ii=0; ii<3; ii++) {
              int k = jj*3 + ii;
              int idx_ma = j*(numElems/3*9) + i*9 + k;
              int idx_a = (j2+ifov_order[k][0])*numElems + i2+ifov_order[k][1];  // idx_a: aligned
              new_values[idx_a] = values[idx_ma];
            }
          }
        }
      }
      return new_values;
   }
}
