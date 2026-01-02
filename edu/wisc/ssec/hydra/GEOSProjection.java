/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
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

import visad.georef.MapProjection;
import visad.RealTupleType;

import java.awt.geom.Rectangle2D;

import visad.VisADException;


public class GEOSProjection extends MapProjection {

    GEOSTransform geosTran;
    Rectangle2D rect;
    double scaleX;
    double offsetX;
    double scaleY;
    double offsetY;

    public GEOSProjection(GEOSTransform geosTran, double startX, double startY, double lenX, double lenY,
                          double scaleX, double offsetX, double scaleY, double offsetY) throws VisADException {
        super(RealTupleType.SpatialEarth2DTuple, null, false, true);

        this.geosTran = geosTran;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;

        rect = new Rectangle2D.Double(startX, startY, lenX, lenY);
    }

    public GEOSProjection(GEOSProjection geosProj, double startX, double startY, double lenX, double lenY) throws VisADException {
        super(RealTupleType.SpatialEarth2DTuple, null, false, true);

        this.geosTran = geosProj.geosTran;
        this.scaleX = geosProj.scaleX;
        this.scaleY = geosProj.scaleY;
        this.offsetX = geosProj.offsetX;
        this.offsetY = geosProj.offsetY;

        rect = new Rectangle2D.Double(startX, startY, lenX, lenY);
    }

    public Rectangle2D getDefaultMapArea() {
        return rect;
    }

    public float[][] toReference(float[][] values) throws VisADException {
        int num = values[0].length;
        float[][] lonlat = new float[2][num];
        for (int k = 0; k < num; k++) {
            double xrad = values[0][k] * scaleX + offsetX;
            double yrad = values[1][k] * scaleY + offsetY;
            double[] dvals = geosTran.satToEarth(xrad, yrad);
            lonlat[0][k] = (float) dvals[0];
            lonlat[1][k] = (float) dvals[1];
        }
        return lonlat;
    }

    public float[][] fromReference(float[][] values) throws VisADException {
        int num = values[0].length;
        float[][] grid = new float[2][num];
        for (int k = 0; k < num; k++) {
            float lon = values[0][k];
            float lat = values[1][k];
            double[] dvals = geosTran.earthToSat((double) lon, (double) lat);
            grid[0][k] = (float) ((dvals[0] - offsetX) / scaleX);
            grid[1][k] = (float) ((dvals[1] - offsetY) / scaleY);
        }
        return grid;
    }

    public double[][] toReference(double[][] values) throws VisADException {
        int num = values[0].length;
        double[][] lonlat = new double[2][num];
        for (int k = 0; k < num; k++) {
            double xrad = values[0][k] * scaleX + offsetX;
            double yrad = values[1][k] * scaleY + offsetY;
            double[] dvals = geosTran.satToEarth(xrad, yrad);
            lonlat[0][k] = dvals[0];
            lonlat[1][k] = dvals[1];
        }
        return lonlat;
    }

    public double[][] fromReference(double[][] values) throws VisADException {
        int num = values[0].length;
        double[][] grid = new double[2][num];
        for (int k = 0; k < num; k++) {
            double lon = values[0][k];
            double lat = values[1][k];
            double[] dvals = geosTran.earthToSat(lon, lat);
            grid[0][k] = (dvals[0] - offsetX) / scaleX;
            grid[1][k] = (dvals[1] - offsetY) / scaleY;
        }
        return grid;
    }

    public double getScaleX() {
        return scaleX;
    }

    public double getScaleY() {
        return scaleY;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getSatelliteNadirLongitude() {
        return geosTran.sub_lon_degrees;
    }

    public boolean equals(Object obj) {
        if (obj instanceof GEOSProjection) {
            GEOSProjection that = (GEOSProjection) obj;
            if ((this.geosTran.equals(that.geosTran)) && this.getDefaultMapArea().equals(that.getDefaultMapArea())) {
                return true;
            }
        }
        return false;
    }
}
