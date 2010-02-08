/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

public class HDFVariable {
      int sds_id;
      HDF hdf;

      public HDFVariable(HDF hdf, int sds_id) throws Exception {
        this.hdf = hdf;
        this.sds_id = sds_id;
      }

      public int findattr(String attr_name) throws Exception {
        return hdf.findattr(sds_id, attr_name);
      }

      public HDFArray readattr(int attr_index) throws Exception {
        return hdf.readattr(sds_id, attr_index);
      }

      public int getdimid(int dim_index) throws Exception {
        return hdf.getdimid(sds_id, dim_index);
      }

      public HDFVariableInfo getinfo() throws Exception {
        return hdf.getinfo(sds_id);
      }

      public HDFArray readdata(int[] start, int[] stride, int[] edges) throws Exception {
        return hdf.readdata(sds_id, start, stride, edges);
      }

}
