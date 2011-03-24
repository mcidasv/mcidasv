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

import java.util.Iterator;

public class HDFFile {

      private int sd_id;
      private HDF hdf;
      private int num_SDdatasets;
      private int num_globalAttrs;

      public HDFFile(String filename) throws Exception {
        hdf = new HDF("./plug_hdf");
        sd_id = hdf.start(filename);
        HDFFileInfo hInfo = hdf.fileinfo(sd_id);
        num_SDdatasets = hInfo.num_SDdatasets;
        num_globalAttrs = hInfo.num_globalAttrs;
      }

      public HDFVariable select(int sds_idx) throws Exception {
        return new HDFVariable(hdf, hdf.select(sd_id, sds_idx));
      }
    
      public HDFArray readattr(int attr_index) throws Exception {
        return hdf.readattr(sd_id, attr_index);
      }

      public int endaccess(int sds_id) throws Exception {
        return hdf.endaccess(sds_id);
      }

      public int findattr(String name) throws Exception {
        return hdf.findattr(sd_id, name);
      }

      public int nametoindex(String name) throws Exception {
        return hdf.nametoindex(sd_id, name);
      }

      public HDFDimension diminfo(int dim_id) throws Exception {
        return hdf.diminfo(dim_id);
      }

      public int getNumberSDdatasets() {
        return num_SDdatasets;
      }

      public int getNumberGlobalAttrs() {
        return num_globalAttrs;
      }

      public void close() throws Exception {
        hdf.close();
      }

      public static void main(String[] args) throws Exception {
        System.out.println("Here args[0]=" + args[0]);
        HDFVariable var = null;
        System.out.println("var=" + var);
        HDFFile hf = new HDFFile(args[0]);
        System.out.println("hf=" + hf);
        System.out.println("number of SD datasets: "+hf.num_SDdatasets);
        System.out.println("number of global attributes: "+hf.num_globalAttrs);

        for (int idx=0; idx<hf.num_SDdatasets; idx++) {
          var = hf.select(idx);
          HDFVariableInfo info = var.getinfo();
          System.out.print("name: "+info.var_name);
          System.out.print(",  rank: "+info.rank);
          System.out.print(",  dimension lengths: ");
          int[] lens = info.dim_lengths;
          for (int k=0;k<info.rank;k++) System.out.print(lens[k]+" ");
          System.out.println("");
        }

        /*
        double[] dbl_data = null;
        float[] flt_data = null;
        short[] sht_data = null;
        int[] int_data = null;

        HDFArray a = var.readdata(new int[] {0,0,0}, new int[] {1,1,1}, new int[] {1,1,50});
        System.out.println(a.getType());
        if (a.getType().equals(Float.TYPE)) {
          flt_data = (float[]) a.getArray();
        }
        else if (a.getType().equals(Double.TYPE)) {
          dbl_data = (double[]) a.getArray();
        }
        */

        /*
        int attr_idx = hf.findattr("coremetadata");
        System.out.println("attr_idx: "+attr_idx);
        HDFAttribute attr = hf.readattr(attr_idx);
        Iterator iter = attr.getAttribute();
        while (iter.hasNext()) {
          System.out.println((String)iter.next());
        }
        */

        //- end access, close I/O pipes.
        hf.close();
      }

}
