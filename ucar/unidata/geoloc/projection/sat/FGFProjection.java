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

package ucar.unidata.geoloc.projection.sat;

import ucar.unidata.geoloc.*;

import java.util.List;
import java.util.ArrayList;



public class FGFProjection extends ProjectionImpl {

  FGFNavigation navigation = null;

  public FGFProjection() {
    navigation = new FGFNavigation();
  }

  public FGFProjection(double subLonDegrees, double delLamdaRadians) {
    navigation = new FGFNavigation(subLonDegrees, delLamdaRadians);
  }

  @Override
  public String toString() {
    return "FGFProjection";
  }

  /**
   * copy constructor - avoid clone !!
   */
  @Override
  public ProjectionImpl constructCopy() {
    //return new FGFProjection();
    return this;
  }

  @Override
  public String paramsToString() {
    return "";
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl destPoint) {
    double[] satCoords = navigation.earthToSat(latlon.getLongitude(), latlon.getLatitude());
    destPoint.setLocation(satCoords[0], satCoords[1]);
    return destPoint;
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl destPoint) {
    double[] lonlat = navigation.satToEarth(ppt.getX(), ppt.getY());
    destPoint.setLongitude(lonlat[0]);
    destPoint.setLatitude(lonlat[1]);
    return destPoint;
  }

  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // either point is infinite
    if (ProjectionPointImpl.isInfinite(pt1) || ProjectionPointImpl.isInfinite(pt2))
      return true;

    // opposite signed X values, larger then 100 km
    return (pt1.getX() * pt2.getX() < 0) && (Math.abs(pt1.getX() - pt2.getX()) > 100);
  }

  @Override
  public boolean equals(Object proj) {
    //return proj instanceof FGFProjection;
    return false;
  }

  /**
   * Create a ProjectionRect from the given LatLonRect.
   * Handles lat/lon points that do not intersect the projection panel.
   *
   * @param rect the LatLonRect
   * @return ProjectionRect, or null if no part of the LatLonRect intersects the projection plane
   */
  @Override
  public ProjectionRect latLonToProjBB(LatLonRect rect) {
    return super.latLonToProjBB(rect);
  }

  /**
  public ProjectionRect getDefaultMapArea() {
    return new ProjectionRect(-.16, -.16, .16, .16);
  }
  **/

}
