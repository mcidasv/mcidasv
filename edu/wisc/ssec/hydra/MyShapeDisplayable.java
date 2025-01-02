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

package edu.wisc.ssec.hydra;

import ucar.visad.display.ShapeDisplayable;
import visad.DataRenderer;
import visad.VisADException;
import visad.bom.ScreenLockedRendererJ3D;
import visad.VisADGeometryArray;

import java.rmi.RemoteException;

public class MyShapeDisplayable extends ShapeDisplayable {
    VisADGeometryArray array = null;

    public MyShapeDisplayable(String name, VisADGeometryArray array) throws VisADException, RemoteException {
        super(name, array);
        this.array = array;
    }

    protected DataRenderer getDataRenderer() throws VisADException {
        return new ScreenLockedRendererJ3D();
    }

}
