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
package ucar.unidata.view.geoloc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.util.Parameter;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.EarthEllipsoid;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.ArrayList;

public class EditableLatLon extends ProjectionImpl {

    private static final Logger logger = LoggerFactory.getLogger(EditableLatLon.class);

    private double latitude0;

    private double longitude0;

    private double latitude1;

    private double longitude1;

    private Earth earth;

    private double centerLon;

    /**
     * Label getter
     * @return
     */
    public String getProjectionTypeLabel() {
        return "Editable Lat/Lon";
    }

    /**
     * Resize ProjectionRect based on the params
     */
    private void resize() {
        // McIDAS Inquiry #934-3141: Bug1 from Request 5
        this.defaultMapArea = new ProjectionRect(longitude0, latitude0, longitude1, latitude1);
    }

    /**
     * Getter for latitude0
     * @return latitude0
     */
    public double getLatitude0() {
        return this.latitude0;
    }

    /**
     * Getter for latitude1
     * @return
     */
    public double getLatitude1() {
        return this.latitude1;
    }

    /**
     * Getter for longitude0
     * @return
     */
    public double getLongitude0() {
        return this.longitude0;
    }

    /**
     * Getter for longitude1
     * @return
     */
    public double getLongitude1() {
        return this.longitude1;
    }

    /**
     * Setter for latitude0
     * @param latitude0
     */
    public void setLatitude0(double latitude0) {
        this.latitude0 = latitude0;
        resize();
    }

    /**
     * Setter for latitude1
     * @param latitude1
     */
    public void setLatitude1(double latitude1) {
        this.latitude1 = latitude1;
        resize();
    }

    /**
     * Setter for longitude0
     * @param longitude0
     */
    public void setLongitude0(double longitude0) {
        this.longitude0 = longitude0;
        resize();
    }

    /**
     * Setter for longitude1
     * @param longitude1
     */
    public void setLongitude1(double longitude1) {
        this.longitude1 = longitude1;
        resize();
    }

    /**
     * toString for params
     * @return params as a string
     */
    public String paramsToString() {
        return "LatLon{latitude0=" + this.latitude0 + ", longitude0=" + this.longitude0 + ", latitude1=" + this.latitude1 + ", longitude1=" + this.longitude1+"}";
    }

    /**
     * toString override
     * @return params as a string
     */
    public String toString() {
        return paramsToString();
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @return
     */
    public ProjectionImpl constructCopy() {
        EditableLatLon result = new EditableLatLon(this.getName(), this.getDefaultMapArea());
        result.setDefaultMapArea(this.defaultMapArea);
        result.setName(this.name);
        result.earth = this.earth;

        return result;
    }

    /**
     * Constructor
     */
    public EditableLatLon() {
        super("Editable LatLon", true);
        this.earth = EarthEllipsoid.DEFAULT;
        this.defaultMapArea = new ProjectionRect(-90, -45, 90, 45);
        this.addParameter("latitude0", latitude0);
        this.addParameter("longitude0", longitude0);
        this.addParameter("latitude1", latitude1);
        this.addParameter("longitude1", longitude1);
        // new ProjectionRect(-90, -45, 90, 45)
    }

    /**
     * Constructor
     * @param earth
     */
    public EditableLatLon(Earth earth) {
        super("Editable LatLon", true);
        this.earth = earth;
        this.defaultMapArea = new ProjectionRect(-90, -45, 90, 45);
        this.addParameter("latitude0", latitude0);
        this.addParameter("longitude1", longitude0);
        this.addParameter("latitude1", latitude1);
        this.addParameter("longitude1", longitude1);

    }

    /**
     * Constructor
     * @param name
     */
    public EditableLatLon(String name) {
        super(name, true);
        this.earth = EarthEllipsoid.DEFAULT;
        this.defaultMapArea = new ProjectionRect(-90, -45, 90, 45);
        this.addParameter("latitude0", latitude0);
        this.addParameter("longitude1", longitude0);
        this.addParameter("latitude1", latitude1);
        this.addParameter("longitude1", longitude1);

    }

    /**
     * Constructor
     * @param name
     * @param defaultMapArea
     */
    public EditableLatLon(String name, ProjectionRect defaultMapArea) {
        super(name, true);
        this.earth = EarthEllipsoid.DEFAULT;
        this.defaultMapArea = defaultMapArea;
        this.addParameter("latitude0", latitude0);
        this.addParameter("longitude1", longitude0);
        this.addParameter("latitude1", latitude1);
        this.addParameter("longitude1", longitude1);

    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param o
     * @return
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            EditableLatLon that = (EditableLatLon)o;
            if (this.defaultMapArea == null != (that.defaultMapArea == null)) {
                return false;
            } else if (this.defaultMapArea != null && !that.defaultMapArea.equals(this.defaultMapArea)) {
                return false;
            } else {
                return Double.compare(that.centerLon, this.centerLon) == 0;
            }
        } else {
            return false;
        }
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @return
     */
    public int hashCode() {
        long temp = Double.doubleToLongBits(this.centerLon);
        int result = (int)(temp ^ temp >>> 32);
        if (this.defaultMapArea != null) {
            result = 31 * result + this.defaultMapArea.hashCode();
        }

        return result;
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param latlon
     * @param result
     * @return
     */
    public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl result) {
        result.setLocation(LatLonPoints.lonNormal(latlon.getLongitude(), this.centerLon), latlon.getLatitude());
        return result;
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param world
     * @param result
     * @return
     */
    public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
        result.setLongitude(world.getX());
        result.setLatitude(world.getY());
        return result;
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param from
     * @param to
     * @return
     */
    public float[][] projToLatLon(float[][] from, float[][] to) {
        float[] fromX = from[0];
        float[] fromY = from[1];
        to[0] = fromY;
        to[1] = fromX;
        return to;
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param from
     * @param to
     * @param latIndex
     * @param lonIndex
     * @return
     */
    public float[][] latLonToProj(float[][] from, float[][] to, int latIndex, int lonIndex) {
        int cnt = from[0].length;
        float[] toX = to[0];
        float[] toY = to[1];
        float[] fromLat = from[latIndex];
        float[] fromLon = from[lonIndex];

        for(int i = 0; i < cnt; ++i) {
            float lat = fromLat[i];
            float lon = (float)(this.centerLon + Math.IEEEremainder((double)fromLon[i] - this.centerLon, 360.0));
            toX[i] = lon;
            toY[i] = lat;
        }

        return to;
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param from
     * @param to
     * @return
     */
    public double[][] projToLatLon(double[][] from, double[][] to) {
        double[] fromX = from[0];
        double[] fromY = from[1];
        to[0] = fromY;
        to[1] = fromX;
        return to;
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param from
     * @param to
     * @param latIndex
     * @param lonIndex
     * @return
     */
    public double[][] latLonToProj(double[][] from, double[][] to, int latIndex, int lonIndex) {
        int cnt = from[0].length;
        double[] toX = to[0];
        double[] toY = to[1];
        double[] fromLat = from[latIndex];
        double[] fromLon = from[lonIndex];

        for(int i = 0; i < cnt; ++i) {
            double lat = fromLat[i];
            double lon = this.centerLon + Math.IEEEremainder(fromLon[i] - this.centerLon, 360.0);
            toX[i] = lon;
            toY[i] = lat;
        }

        return to;
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param centerLon
     * @return
     */
    public double setCenterLon(double centerLon) {
        this.centerLon = LatLonPoints.lonNormal(centerLon);
        return this.centerLon;
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @return
     */
    public double getCenterLon() {
        return this.centerLon;
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param pt1
     * @param pt2
     * @return
     */
    public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
        return Math.abs(pt1.getX() - pt2.getX()) > 270.0;
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param bb
     */
    public void setDefaultMapArea(ProjectionRect bb) {
        super.setDefaultMapArea(bb);
        this.centerLon = bb.getCenterX();
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param latlonR
     * @return
     */
    public ProjectionRect[] latLonToProjRect(LatLonRect latlonR) {
        double latitude0 = latlonR.getLowerLeftPoint().getLatitude();
        double height = Math.abs(latlonR.getUpperRightPoint().getLatitude() - latitude0);
        double width = latlonR.getWidth();
        double longitude0 = LatLonPoints.lonNormal(latlonR.getLowerLeftPoint().getLongitude(), this.centerLon);
        double longitude1 = LatLonPoints.lonNormal(latlonR.getUpperRightPoint().getLongitude(), this.centerLon);
        ProjectionRect[] rects = new ProjectionRect[]{new ProjectionRect(), new ProjectionRect()};
        if (longitude0 < longitude1) {
            rects[0].setRect(longitude0, latitude0, width, height);
            rects[1] = null;
        } else {
            double y = this.centerLon + 180.0 - longitude0;
            rects[0].setRect(longitude0, latitude0, y, height);
            rects[1].setRect(longitude1 - width + y, latitude0, width - y, height);
        }

        return rects;
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param world
     * @return
     */
    public LatLonRect projToLatLonBB(ProjectionRect world) {
        double startLat = world.getMinY();
        double startLon = world.getMinX();
        double deltaLat = world.getHeight();
        double deltaLon = world.getWidth();
        LatLonPoint llpt = LatLonPoint.create(startLat, startLon);
        return new LatLonRect(llpt, deltaLat, deltaLon);
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @param latitude0
     * @param longitude0
     * @param latitude1
     * @param longitude1
     * @return
     */
    public ProjectionRect[] latLonToProjRect(double latitude0, double longitude0, double latitude1, double longitude1) {
        double height = Math.abs(latitude1 - latitude0);
        latitude0 = Math.min(latitude1, latitude0);
        double width = longitude1 - longitude0;
        if (width < 1.0E-8) {
            width = 360.0;
        }

        longitude0 = LatLonPoints.lonNormal(longitude0, this.centerLon);
        longitude1 = LatLonPoints.lonNormal(longitude1, this.centerLon);
        ProjectionRect[] rects = new ProjectionRect[]{new ProjectionRect(), new ProjectionRect()};
        if (width >= 360.0) {
            rects[0].setRect(this.centerLon - 180.0, latitude0, 360.0, height);
            rects[1] = null;
        } else if (longitude0 < longitude1) {
            rects[0].setRect(longitude0, latitude0, width, height);
            rects[1] = null;
        } else {
            double y = this.centerLon + 180.0 - longitude0;
            rects[0].setRect(longitude0, latitude0, y, height);
            rects[1].setRect(longitude1 - width + y, latitude0, width - y, height);
        }

        return rects;
    }

}
