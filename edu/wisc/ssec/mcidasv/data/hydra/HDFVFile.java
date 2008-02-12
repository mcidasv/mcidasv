/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.data.hydra;

public class HDFVFile {
  int fId;
  HDF hdf;

  HDFVFile(HDF hdf, String filename) throws Exception {
    this.hdf = hdf;
    fId = hdf.hOpen(filename);
    int rc = hdf.vStart(fId);
  }

  public int getId() {
    return fId;
  }

  public int find(String data_name) throws Exception {
    return hdf.vsFind(fId, data_name);
  }

  public HDFVData select(String name) throws Exception {
    int v_ref = find(name);
    return new HDFVData(name, fId, v_ref, hdf);
  }
}
