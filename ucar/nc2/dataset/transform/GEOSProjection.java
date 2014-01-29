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

package ucar.nc2.dataset.transform;

import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.unidata.geoloc.ProjectionImpl;

public class GEOSProjection extends AbstractCoordTransBuilder {

    public String getTransformName() {
      return "GEOSProjection";
    }

    public TransformType getTransformType() {
      return TransformType.Projection;
    }

    public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {

      double subLonDegrees = readAttributeDouble( ctv, "longitude_of_projection_origin", -75.0);
      Attribute attr = ctv.findAttribute("sweep_angle_axis");
      String sweepAngleAxis = "x";
      if (attr != null) {
         sweepAngleAxis = attr.getStringValue();
      }
      /**
      double minor_axis = readAttributeDouble( ctv, "semi_minor_axis", Double.NaN);
      double major_axis = readAttributeDouble( ctv, "semi_major_axis", Double.NaN);
      double height = readAttributeDouble( ctv, "height_from_earth_center", Double.NaN);
      **/

      ProjectionImpl proj = new ucar.unidata.geoloc.projection.sat.GEOSProjection(subLonDegrees, sweepAngleAxis);
      return new ProjectionCT(ctv.getShortName(), "GEOS", proj);
    }

}
