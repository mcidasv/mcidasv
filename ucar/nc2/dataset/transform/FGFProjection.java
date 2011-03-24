/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.unidata.geoloc.ProjectionImpl;

/**
 * Class Description
 *
 * @author caron
 * @since Jan 9, 2010
 */


public class FGFProjection extends AbstractCoordTransBuilder {

    public String getTransformName() {
      return "FGFProjection";
    }

    public TransformType getTransformType() {
      return TransformType.Projection;
    }

    public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {

      double subLonDegrees = readAttributeDouble( ctv, "subpoint_longitude_degrees", -75.0);
      double delLamdaRadians = readAttributeDouble( ctv, "delta_lamda_radians", 56.0E-06);
      /**
      double lat0 = readAttributeDouble( ctv, "latitude_of_projection_origin", Double.NaN);
      double minor_axis = readAttributeDouble( ctv, "semi_minor_axis", Double.NaN);
      double major_axis = readAttributeDouble( ctv, "semi_major_axis", Double.NaN);
      double height = readAttributeDouble( ctv, "height_from_earth_center", Double.NaN);
      double scale_x = readAttributeDouble( ctv, "scale_x", Double.NaN);
      double scale_y = readAttributeDouble( ctv, "scale_y", Double.NaN);
      **/

      ProjectionImpl proj = new ucar.unidata.geoloc.projection.sat.FGFProjection(subLonDegrees, delLamdaRadians);
      return new ProjectionCT(ctv.getShortName(), "FGF", proj);
    }

}
