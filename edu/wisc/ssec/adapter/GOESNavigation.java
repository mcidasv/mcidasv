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

package edu.wisc.ssec.adapter;

import edu.wisc.ssec.hydra.GEOSTransform;
import visad.CoordinateSystem;
import visad.Linear2DSet;
import visad.georef.MapProjection;

/**
 * @author rink
 */
public class GOESNavigation implements Navigation {

    GEOSTransform geosTrans;
    double scale_x;
    double offset_x;
    double scale_y;
    double offset_y;

    MapProjection mapProj;


    public GOESNavigation(GEOSTransform geosTrans, double scale_x, double offset_x, double scale_y, double offset_y) {
        this.geosTrans = geosTrans;
        this.scale_x = scale_x;
        this.offset_x = offset_x;
        this.scale_y = scale_y;
        this.offset_y = offset_y;
    }

    public GOESNavigation(MapProjection mapProj) {
        this.mapProj = mapProj;
    }

    @Override
    public CoordinateSystem getVisADCoordinateSystem(Object subset) throws Exception {
        return mapProj;
    }

    @Override
    public double[] getEarthLocOfDataCoord(int[] coord) throws Exception {
        double[][] lonlat = mapProj.toReference(new double[][]{{coord[0]}, {coord[1]}});

        return new double[]{lonlat[0][0], lonlat[1][0]};
    }

}
