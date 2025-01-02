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

import visad.*;

import java.lang.Math;
//import org.apache.commons.math3.util.Math;

/**
 * LambertAzimuthalEqualArea is the VisAD class for coordinate
 * systems for ( X_map, Y_map ).<P>
 */

public class LambertAzimuthalEqualArea extends CoordinateSystem {

    double R;
    double lon_center;
    double lat_center;
    double false_easting;
    double false_northing;
    double sin_lat_o;
    double cos_lat_o;
    Unit[] reference_units;

    private static Unit[] coordinate_system_units =
            {null, null};

    private static Unit[] default_reference_units =
            {CommonUnit.radian, CommonUnit.radian};

    public LambertAzimuthalEqualArea(RealTupleType reference,
                                     double lon_center,
                                     double lat_center)
            throws VisADException {
        this(reference, 6367470, lon_center, lat_center, 0, 0);
    }

    public LambertAzimuthalEqualArea(RealTupleType reference,
                                     double R,
                                     double lon_center,
                                     double lat_center,
                                     double false_easting,
                                     double false_northing
    )
            throws VisADException {

        super(reference, coordinate_system_units);

        reference_units =
                reference.getDefaultUnits();

        if (reference_units != null) {
            if (!Unit.canConvertArray(default_reference_units, reference_units)) {
                throw new VisADException("not compatible with reference units");
            }
        } else {
            reference_units = default_reference_units;
        }

        this.R = R;
        this.lon_center = lon_center;
        this.lat_center = lat_center;
        this.false_easting = false_easting;
        this.false_northing = false_northing;
        this.sin_lat_o = Math.sin(lat_center);
        this.cos_lat_o = Math.cos(lat_center);
    }

    public float[][] toReference(float[][] tuples) throws VisADException {
        double Rh;
        double x;
        double y;
        double z;               // Great circle dist from proj center to given point
        double sin_z;           // Sine of z
        double cos_z;           // Cosine of z
        double temp;            // Re-used temporary variable
        double lon;
        double lat;
        double[] dum_1 = new double[1];
        double[] dum_2 = new double[1];
        double[] dum = new double[1];

        int n_tuples = tuples[0].length;
        int tuple_dim = tuples.length;

        if (tuple_dim != 2) {
            throw new VisADException("LambertAzimuthalEqualArea: tuple dim != 2");
        }

        float t_tuples[][] = new float[2][n_tuples];

        for (int ii = 0; ii < n_tuples; ii++) {

            x = tuples[0][ii] - false_easting;
            y = tuples[1][ii] - false_northing;
            Rh = Math.sqrt(x * x + y * y);
            temp = Rh / (2.0 * R);

            z = 2.0 * GctpFunction.asinz(temp);
            dum[0] = z;
            GctpFunction.sincos(dum, dum_1, dum_2);
            sin_z = dum_1[0];
            cos_z = dum_2[0];
            lon = lon_center;
            if (Math.abs(Rh) > GctpFunction.EPSLN) {
                lat = GctpFunction.asinz(sin_lat_o * cos_z + cos_lat_o * sin_z * y / Rh);
                temp = Math.abs(lat_center) - GctpFunction.HALF_PI;
                if (Math.abs(temp) > GctpFunction.EPSLN) {
                    temp = cos_z - sin_lat_o * Math.sin(lat);
                    if (temp != 0.0) {
                        lon = GctpFunction.adjust_lon(lon_center + Math.atan2(x * sin_z * cos_lat_o, temp * Rh));
                    }
                } else if (lat_center < 0.0) {
                    lon = GctpFunction.adjust_lon(lon_center - Math.atan2(-x, y));
                } else {
                    lon = GctpFunction.adjust_lon(lon_center + Math.atan2(x, -y));
                }
            } else {
                lat = lat_center;
            }

            t_tuples[0][ii] = (float) lon;
            t_tuples[1][ii] = (float) lat;
        }
        if (reference_units[0].equals(CommonUnit.degree)) {
            for (int i = 0; i < n_tuples; i++) {
                t_tuples[0][i] *= Data.RADIANS_TO_DEGREES;
                t_tuples[1][i] *= Data.RADIANS_TO_DEGREES;
            }
        }
        return t_tuples;
    }

    public double[][] toReference(double[][] tuples) throws VisADException {

        double Rh;
        double x;
        double y;
        double z;               // Great circle dist from proj center to given point
        double sin_z;           // Sine of z
        double cos_z;           // Cosine of z
        double temp;            // Re-used temporary variable
        double lon;
        double lat;
        double[] dum_1 = new double[1];
        double[] dum_2 = new double[1];
        double[] dum = new double[1];

        int n_tuples = tuples[0].length;
        int tuple_dim = tuples.length;

        if (tuple_dim != 2) {
            throw new VisADException("LambertAzimuthalEqualArea: tuple dim != 2");
        }

        double t_tuples[][] = new double[2][n_tuples];

        for (int ii = 0; ii < n_tuples; ii++) {

            x = tuples[0][ii] - false_easting;
            y = tuples[1][ii] - false_northing;
            Rh = Math.sqrt(x * x + y * y);
            temp = Rh / (2.0 * R);
            z = 2.0 * GctpFunction.asinz(temp);
            dum[0] = z;
            GctpFunction.sincos(dum, dum_1, dum_2);
            sin_z = dum_1[0];
            cos_z = dum_2[0];
            lon = lon_center;
            if (Math.abs(Rh) > GctpFunction.EPSLN) {
                lat = GctpFunction.asinz(sin_lat_o * cos_z + cos_lat_o * sin_z * y / Rh);
                temp = Math.abs(lat_center) - GctpFunction.HALF_PI;
                if (Math.abs(temp) > GctpFunction.EPSLN) {
                    temp = cos_z - sin_lat_o * Math.sin(lat);
                    if (temp != 0.0) {
                        lon = GctpFunction.adjust_lon(lon_center + Math.atan2(x * sin_z * cos_lat_o, temp * Rh));
                    }
                } else if (lat_center < 0.0) {
                    lon = GctpFunction.adjust_lon(lon_center - Math.atan2(-x, y));
                } else {
                    lon = GctpFunction.adjust_lon(lon_center + Math.atan2(x, -y));
                }
            } else {
                lat = lat_center;
            }

            t_tuples[0][ii] = lon;
            t_tuples[1][ii] = lat;
        }
        return
                Unit.convertTuple(t_tuples, default_reference_units, reference_units);
    }

    public float[][] fromReference(float[][] tuples) throws VisADException {
        int n_tuples = tuples[0].length;
        int tuple_dim = tuples.length;
        double ksp;
        double g;

        if (tuple_dim != 2) {
            throw new VisADException("LambertAzimuthalEqualArea: tuple dim != 2");
        }

        float[][] t_tuples = new float[2][n_tuples];
        double[] delta_lon = new double[1];
        double[] lat = new double[1];
        double[] sin_lat = new double[1];
        double[] cos_lat = new double[1];
        double[] sin_delta_lon = new double[1];
        double[] cos_delta_lon = new double[1];

        for (int ii = 0; ii < n_tuples; ii++) {
            delta_lon[0] = tuples[0][ii] * Data.DEGREES_TO_RADIANS - lon_center;
            lat[0] = tuples[1][ii] * Data.DEGREES_TO_RADIANS;


            GctpFunction.adjust_lon(delta_lon);
            GctpFunction.sincos(lat, sin_lat, cos_lat);
            GctpFunction.sincos(delta_lon, sin_delta_lon, cos_delta_lon);

            g = sin_lat_o * sin_lat[0] + cos_lat_o * cos_lat[0] * cos_delta_lon[0];
            if (g == -1) { // Point project to a circle of radius = 2*R
                t_tuples[0][ii] = Float.NaN;
                t_tuples[1][ii] = Float.NaN;
            }

            ksp = R * Math.sqrt(2.0 / (1.0 + g));

            t_tuples[0][ii] = (float) (ksp * cos_lat[0] * sin_delta_lon[0] + false_easting);
            t_tuples[1][ii] = (float) (ksp * (cos_lat_o * sin_lat[0] -
                    sin_lat_o * cos_lat[0] * cos_delta_lon[0]) +
                    false_northing);
        }

        return t_tuples;
    }

    public double[][] fromReference(double[][] tuples) throws VisADException {
        int n_tuples = tuples[0].length;
        int tuple_dim = tuples.length;
        double ksp;
        double g;

        if (tuple_dim != 2) {
            throw new VisADException("LambertAzimuthalEqualArea: tuple dim != 2");
        }

        double[][] t_tuples = new double[2][n_tuples];
        double[] delta_lon = new double[1];
        double[] lat = new double[1];
        double[] sin_lat = new double[1];
        double[] cos_lat = new double[1];
        double[] sin_delta_lon = new double[1];
        double[] cos_delta_lon = new double[1];

        for (int ii = 0; ii < n_tuples; ii++) {
            delta_lon[0] = tuples[0][ii] * Data.DEGREES_TO_RADIANS - lon_center;
            lat[0] = tuples[1][ii] * Data.DEGREES_TO_RADIANS;


            GctpFunction.adjust_lon(delta_lon);
            GctpFunction.sincos(lat, sin_lat, cos_lat);
            GctpFunction.sincos(delta_lon, sin_delta_lon, cos_delta_lon);

            g = sin_lat_o * sin_lat[0] + cos_lat_o * cos_lat[0] * cos_delta_lon[0];
            if (g == -1) { // Point project to a circle of radius = 2*R
                t_tuples[0][ii] = Float.NaN;
                t_tuples[1][ii] = Float.NaN;
            }

            ksp = R * Math.sqrt(2.0 / (1.0 + g));

            t_tuples[0][ii] = (float) (ksp * cos_lat[0] * sin_delta_lon[0] + false_easting);
            t_tuples[1][ii] = (float) (ksp * (cos_lat_o * sin_lat[0] -
                    sin_lat_o * cos_lat[0] * cos_delta_lon[0]) +
                    false_northing);
        }

        return t_tuples;
    }

    public boolean equals(Object cs) {
        if (cs instanceof LambertAzimuthalEqualArea) {
            LambertAzimuthalEqualArea that = (LambertAzimuthalEqualArea) cs;
            if ((this.R == that.R) && (this.lon_center == that.lon_center) && (this.lat_center == that.lat_center) &&
                    (this.false_easting == that.false_easting) && (this.false_northing == that.false_northing)) {
                return true;
            }
        }
        return false;
    }

}
