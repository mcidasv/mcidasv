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
 * McIDAS-V is built on Unidata's IDV, as well as SSEC's VisAD and HYDRA
 * projects. Parts of McIDAS-V source code are based on IDV, VisAD, and
 * HYDRA source code.
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

import java.awt.geom.Rectangle2D;
import visad.CoordinateSystem;
import visad.RealTupleType;
import visad.Unit;
import visad.VisADException;
import visad.georef.MapProjection;

public class InsatL1BCoordinateSystem1 extends MapProjection {

    private final int scans;
    private final int pixels;
    private final int image_scans;
    private final int image_pixels;
    private final int latIndex = getLatitudeIndex();
    private final int lonIndex = getLongitudeIndex();
    private float latlon[][];
    private double min_lat;
    private double min_lon;
    private double max_lat;
    private double max_lon;
//    private float grid[];
    private static Unit coordinate_system_units[] = {
        null, null
    };
    //more than one if datadisk height is latlon_disk_height
    private int height_scale_factor;
    //more than one if datadisk width is latlon_disk_width
    private int width_scale_factor;
    int total_entries;

    public void destroy() {
        for (int i = 0; i < latlon.length; i++) {
            latlon[i] = null;
        }
        latlon = (float[][]) null;
//        grid = null;
    }

    public float[][] getLatLon() {
        return latlon;
    }

    public int getElements() {
        return pixels;
    }

    public int getLines() {
        return scans;
    }

    public InsatL1BCoordinateSystem1(float latlon[][], int latlon_disk_height, int latlon_disk_width, int height_scale_factor, int width_scale_factor) throws VisADException {
        super(RealTupleType.LatitudeLongitudeTuple, coordinate_system_units);

        this.scans = latlon_disk_height;
        this.pixels = latlon_disk_width;
        this.height_scale_factor = height_scale_factor;
        this.width_scale_factor = width_scale_factor;
        image_scans = scans * height_scale_factor;
        image_pixels = pixels * width_scale_factor;

        total_entries = scans * pixels;
//        grid = new float[2];

        this.latlon = latlon;
        min_lat = 100.0f;//initially setting it very high
        max_lat = -100.0f;//initially setting it very low
        min_lon = 200.0f;//initially setting it very high
        max_lon = -200.0f;//initially setting it very low
        int indx = 0;
        for (indx = 0; indx < latlon[0].length; indx++) {
            if (latlon[0][indx] < 90.0) {
                min_lat = Math.min(min_lat, latlon[0][indx]);
                max_lat = Math.max(max_lat, latlon[0][indx]);
            } else {
                latlon[0][indx] = Float.NaN;
            }
            if (latlon[1][indx] < 180.0) {
//                System.err.println(latlon[1][indx]);
                min_lon = Math.min(min_lon, latlon[1][indx]);
                max_lon = Math.max(max_lon, latlon[1][indx]);
            } else {
                latlon[1][indx] = Float.NaN;
            }
        }

        System.err.println("Scans:" + scans + " Pixels");

        System.err.println(min_lon + "  " + max_lon);
        System.err.println(min_lat + "  " + max_lat);
    }

    public CoordinateSystem getCoordinateSystem() {
        return this;
    }

    public Rectangle2D getDefaultMapArea() {
        return new java.awt.geom.Rectangle2D.Float(0, 0, image_pixels, image_scans);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof InsatL1BCoordinateSystem1)) {
            return false;
        }
        InsatL1BCoordinateSystem1 that = (InsatL1BCoordinateSystem1) obj;
        boolean return_value = (that.scans == this.scans && that.pixels == this.pixels
                && that.image_scans == this.image_scans && that.image_pixels == this.image_pixels
                && java.util.Arrays.equals(this.latlon[0], that.latlon[0]) && java.util.Arrays.equals(this.latlon[1], that.latlon[1])
                && that.min_lat == this.min_lat && that.min_lon == this.min_lon
                && that.max_lat == this.max_lat && that.max_lon == this.max_lon
                && that.latIndex == this.latIndex && that.lonIndex == this.lonIndex);
        return return_value;
    }

    //Dont allocate extra memory for return values
    public double[][] toReference(double tuples[][]) {
        double lat = Double.NaN;
        double lon = Double.NaN;
        //System.err.println("double toReference");
        for (int i = 0; i < tuples[0].length; i++) {
            tuples[1][i] = (tuples[1][i] < 0) ? 0 : ((tuples[1][i] >= image_scans) ? image_scans - 1 : tuples[1][i]);
            tuples[0][i] = (tuples[0][i] < 0) ? 0 : ((tuples[0][i] >= image_pixels) ? image_pixels - 1 : tuples[0][i]);
            int ndx = (int) ((image_scans - tuples[1][i]) / height_scale_factor - 1) * pixels + (int) tuples[0][i] / width_scale_factor;
            lat = latlon[latIndex][ndx];
            lon = latlon[lonIndex][ndx];
            tuples[latIndex][i] = lat;
            tuples[lonIndex][i] = lon;
            if (tuples[lonIndex][i] < 0) {
                tuples[lonIndex][i] += 360.0;
            }
        }
        return tuples;
    }
    //Dont allocate extra memory for return values

    public double[][] fromReference(double tuples[][]) {
        int last_grid_x_index = -1;
        int last_grid_y_index = -1;
        //System.err.println("In double fromReference");
        for (int k = 0; k < tuples[0].length; k++) {
            if (!Double.isNaN(tuples[lonIndex][k]) && tuples[lonIndex][k] > 180.0) {
                tuples[1][k] -= 180.0;
            }
            if (Double.isNaN(tuples[latIndex][k]) || Double.isNaN(tuples[lonIndex][k])) {
                last_grid_x_index = last_grid_y_index = -1;
            } else if (tuples[latIndex][k] >= min_lat && tuples[latIndex][k] <= max_lat && tuples[lonIndex][k] >= min_lon && tuples[lonIndex][k] <= max_lon) {
                float grid[] = valueToGrid((float) tuples[latIndex][k], (float) tuples[lonIndex][k], last_grid_x_index, last_grid_y_index);
                if (Float.isNaN(grid[0]) || Float.isNaN(grid[1])) {
                    tuples[0][k] = tuples[1][k] = Double.NaN;
                    last_grid_x_index = last_grid_y_index = -1;
                } else {
                    tuples[0][k] = grid[0] * width_scale_factor;
                    tuples[1][k] = (scans - grid[1]) * height_scale_factor - 1;
                    //tuples[1][k] = grid[1] * height_scale_factor;
                    last_grid_x_index = (int) grid[0];
                    last_grid_y_index = (int) grid[1];
                    if (last_grid_x_index > pixels - 2) { //Ghansham:Here I check if the last grid x index should not be greater than second last column  (borrowed from valueToGrid when lowerTri is false)
                        last_grid_x_index = pixels - 2;
                    }
                    if (last_grid_y_index > scans - 2) { //Ghansham:Here I check if the last grid y index should not be greater than second last row  (borrowed from valueToGrid when lowerTri is false)
                        last_grid_y_index = scans - 2;
                    }
                }
                grid = null;
            } else {
                tuples[0][k] = tuples[1][k] = Double.NaN;
                last_grid_x_index = last_grid_y_index = -1;
            }
        }
        //long t2 = System.currentTimeMillis();
        //System.err.println("Time taken:" + (t2-t1));
        return tuples;
    }

    //Dont allocate extra memory for return values
    public float[][] toReference(float tuples[][]) {
        //System.err.println("In float toReference coordSys:" + tuples[0].length);
        for (int i = 0; i < tuples[0].length; i++) {
            tuples[1][i] = (tuples[1][i] < 0) ? 0 : ((tuples[1][i] >= image_scans) ? image_scans - 1 : tuples[1][i]);
            tuples[0][i] = (tuples[0][i] < 0) ? 0 : ((tuples[0][i] >= image_pixels) ? image_pixels - 1 : tuples[0][i]);
            //int ndx = (int) ((image_scans - tuples[1][i]) / height_scale_factor - 1) * pixels + (int) tuples[0][i] / width_scale_factor;
            int ndx = (int) ((tuples[1][i]) / height_scale_factor) * pixels + (int) tuples[0][i] / width_scale_factor;
            float lat = latlon[latIndex][ndx];
            float lon = latlon[lonIndex][ndx];
            if ((lat <= 90f) && (lat >= -90)) {
            tuples[latIndex][i] = lat;
            tuples[lonIndex][i] = lon;
            }
            else {
            tuples[latIndex][i] = Float.NaN;
            tuples[lonIndex][i] = Float.NaN;
            }
            if (tuples[lonIndex][i] < 0f) {
                tuples[lonIndex][i] += 360.0f;
            }
        }
        return tuples;
    }

    //Dont allocate extra memory for return values
    public float[][] fromReference(float tuples[][]) {
        System.err.println("Tuple Length:" + tuples[0].length);
        long t1 = System.currentTimeMillis();

        int last_grid_x_index = -1;
        int last_grid_y_index = -1;
        for (int k = 0; k < tuples[0].length; k++) {

            if (!Float.isNaN(tuples[lonIndex][k]) && tuples[lonIndex][k] > 180.0f) {
                tuples[1][k] -= 360.0f;
            }

            if (Float.isNaN(tuples[0][k]) || Float.isNaN(tuples[1][k])) {
                last_grid_x_index = last_grid_y_index = -1;
            } else if (tuples[latIndex][k] >= min_lat && tuples[latIndex][k] <= max_lat && tuples[lonIndex][k] >= min_lon && tuples[lonIndex][k] <= max_lon) {
                float grid[] = valueToGrid(tuples[latIndex][k], tuples[lonIndex][k], last_grid_x_index, last_grid_y_index);
                if (Float.isNaN(grid[0]) || Float.isNaN(grid[1])) {
                    tuples[0][k] = tuples[1][k] = Float.NaN;
                    last_grid_x_index = last_grid_y_index = -1;
                } else {
                    tuples[0][k] = grid[0] * width_scale_factor;
                    tuples[1][k] = (scans - grid[1]) * height_scale_factor - 1;
                    //tuples[1][k] = grid[1] * height_scale_factor;
                    last_grid_x_index = (int) grid[0];
                    last_grid_y_index = (int) grid[1];
                    if (last_grid_x_index > pixels - 2) { //Ghansham:Here I check if the last grid x index should not be greater than second last column  (borrowed from valueToGrid when lowerTri is false)
                        last_grid_x_index = pixels - 2;
                    }
                    if (last_grid_y_index > scans - 2) { //Ghansham:Here I check if the last grid y index should not be greater than second last row (borrowed from valueToGrid when lowerTri is false)
                        last_grid_y_index = scans - 2;
                    }
                }
                grid = null;
            } else {
                tuples[0][k] = tuples[1][k] = Float.NaN;
                last_grid_x_index = last_grid_y_index = -1;
            }

        }

        long t2 = System.currentTimeMillis();
        System.err.println("Time taken:" + (t2 - t1));
        return tuples;
    }

    //This method is borrowed from Gridded2DSet. Thanks to the fast way to go from (lat, lon)->(scan, pix)
    private float[] valueToGrid(float lat, float lon, int last_grid_x_index, int last_grid_y_index) {
        boolean debug = false;
        float grid[] = new float[2];
        int gx = 0;
        int gy = 0;

        int num_iterations = 2 * (pixels + scans);
        boolean useLastGuess = false; //Ghansham:local variable.. not shared among function calls
        if ((last_grid_x_index == -1) && (last_grid_y_index == -1)) { //Ghansham:Set last guess to false if it starts from center of the grid
            gx = (pixels - 1) / 2;
            gy = (scans - 1) / 2;
        } else { 			//Ghansham:Set it to true if we are starting from last grid indices
            gx = last_grid_x_index;
            gy = last_grid_y_index;
	    useLastGuess = true;
        }

        grid[0] = Float.NaN;
        grid[1] = Float.NaN;
        boolean lowertri = true;
        boolean Pos = true;

        for (int itnum = 0; itnum < num_iterations; itnum++) {
            float v0[] = {latlon[0][gy * pixels + gx],
                latlon[1][gy * pixels + gx]};
            float v1[] = {latlon[0][gy * pixels + gx + 1],
                latlon[1][gy * pixels + gx + 1]};
            float v2[] = {latlon[0][(gy + 1) * pixels + gx],
                latlon[1][(gy + 1) * pixels + gx]};
            float v3[] = {latlon[0][(gy + 1) * pixels + gx + 1],
                latlon[1][(gy + 1) * pixels + gx + 1]};




            if (v0[0] != v0[0] || v0[1] != v0[1] || v1[0] != v1[0] || v1[1] != v1[1]
                    || v2[0] != v2[0] || v2[1] != v2[1] || v3[0] != v3[0] || v3[1] != v3[1]) { //Ghansham: Inserted extra to break out as soon as we find any of corners is NaN. We can replace this call with Float.isNaN. This also reduces computation time a lot.
                    break;
            }


            float[] bd = {v2[0] - v1[0], v2[1] - v1[1]};
            float[] bp = {lat - v1[0], lon - v1[1]};
            float[] dp = {lat - v2[0], lon - v2[1]};
            // check the LOWER triangle of the grid box
            if (lowertri) {
                float[] ab = {v1[0] - v0[0], v1[1] - v0[1]};
                float[] da = {v0[0] - v2[0], v0[1] - v2[1]};
                float[] ap = {lat - v0[0], lon - v0[1]};
                float tval1 = ab[0] * ap[1] - ab[1] * ap[0];
                float tval2 = bd[0] * bp[1] - bd[1] * bp[0];
                float tval3 = da[0] * dp[1] - da[1] * dp[0];
                boolean test1 = (tval1 == 0) || ((tval1 > 0) == Pos);
                boolean test2 = (tval2 == 0) || ((tval2 > 0) == Pos);
                boolean test3 = (tval3 == 0) || ((tval3 > 0) == Pos);
                int ogx = gx;
                int ogy = gy;
                if (!test1 && !test2) {      // Go UP & RIGHT
                    gx++;
                    gy--;
                } else if (!test2 && !test3) { // Go DOWN & LEFT
                    gx--;
                    gy++;
                } else if (!test1 && !test3) { // Go UP & LEFT
                    gx--;
                    gy--;
                } else if (!test1) {           // Go UP
                    gy--;
                } else if (!test3) {           // Go LEFT
                    gx--;
                }
                // Snap guesses back into the grid
                if (gx < 0) {
                    gx = 0;
                }
                if (gx > pixels - 2) {
                    gx = pixels - 2;
                }
                if (gy < 0) {
                    gy = 0;
                }
                if (gy > scans - 2) {
                    gy = scans - 2;
                }
                if ((gx == ogx) && (gy == ogy) && (test2)) {
                    // Found correct grid triangle
                    // Solve the point with the reverse interpolation
                    grid[0] = ((lat - v0[0]) * (v2[1] - v0[1])
                            + (v0[1] - lon) * (v2[0] - v0[0]))
                            / ((v1[0] - v0[0]) * (v2[1] - v0[1])
                            + (v0[1] - v1[1]) * (v2[0] - v0[0])) + gx;
                    grid[1] = ((lat - v0[0]) * (v1[1] - v0[1])
                            + (v0[1] - lon) * (v1[0] - v0[0]))
                            / ((v2[0] - v0[0]) * (v1[1] - v0[1])
                            + (v0[1] - v2[1]) * (v1[0] - v0[0])) + gy;
                    break;
                } else {
                    lowertri = false;
                }
            } // check the UPPER triangle of the grid box
            else {
                float[] bc = {v3[0] - v1[0], v3[1] - v1[1]};
                float[] cd = {v2[0] - v3[0], v2[1] - v3[1]};
                float[] cp = {lat - v3[0], lon - v3[1]};
                float tval1 = bc[0] * bp[1] - bc[1] * bp[0];
                float tval2 = cd[0] * cp[1] - cd[1] * cp[0];
                float tval3 = bd[0] * dp[1] - bd[1] * dp[0];
                boolean test1 = (tval1 == 0) || ((tval1 > 0) == Pos);
                boolean test2 = (tval2 == 0) || ((tval2 > 0) == Pos);
                boolean test3 = (tval3 == 0) || ((tval3 < 0) == Pos);
                int ogx = gx;
                int ogy = gy;
                if (!test1 && !test3) {      // Go UP & RIGHT
                    gx++;
                    gy--;
                } else if (!test2 && !test3) { // Go DOWN & LEFT
                    gx--;
                    gy++;
                } else if (!test1 && !test2) { // Go DOWN & RIGHT
                    gx++;
                    gy++;
                } else if (!test1) {           // Go RIGHT
                    gx++;
                } else if (!test2) {           // Go DOWN
                    gy++;
                }
                // Snap guesses back into the grid
                if (gx < 0) {
                    gx = 0;
                }
                if (gx > pixels - 2) {
                    gx = pixels - 2;
                }
                if (gy < 0) {
                    gy = 0;
                }
                if (gy > scans - 2) {
                    gy = scans - 2;
                }
                if ((gx == ogx) && (gy == ogy) && (test3)) {
                    // Found correct grid triangle
                    // Solve the point with the reverse interpolation
                    grid[0] = ((v3[0] - lat) * (v1[1] - v3[1])
                            + (lon - v3[1]) * (v1[0] - v3[0]))
                            / ((v2[0] - v3[0]) * (v1[1] - v3[1])
                            - (v2[1] - v3[1]) * (v1[0] - v3[0])) + gx + 1;
                    grid[1] = ((v2[1] - v3[1]) * (v3[0] - lat)
                            + (v2[0] - v3[0]) * (lon - v3[1]))
                            / ((v1[0] - v3[0]) * (v2[1] - v3[1])
                            - (v2[0] - v3[0]) * (v1[1] - v3[1])) + gy + 1;
                    break;
                } else {
                    lowertri = true;
                }
            }

        }


        if (debug ) {
        	System.out.println(" " + grid[0] + "  " + grid[1]);
        }

	if ((grid[0] >= pixels - 0.5) || (grid[1] >= scans - 0.5)
                || (grid[0] <= -0.5) || (grid[1] <= -0.5)) {
            grid[0] = grid[1] = Float.NaN;
        }

        if (useLastGuess) { //Ghansham: Here is the real trick. If we have searched using lastGuess. 
            if (Float.isNaN(grid[0]) || Float.isNaN(grid[1])) { //But we have not been able to find a valid index. Give it another try make last guess false.
                return valueToGrid(lat, lon, -1, -1);		//Call recursively passing last grid indices as -1, -1 which make the last guess false.
            }
        }
        return grid;
    }

    public double getMinLatitude() {
        return min_lat;
    }

    public double getMaxLatitude() {
        return max_lat;
    }

    public double getMinLongitude() {
        return min_lon;
    }

    public double getMaxLongitude() {
        return max_lon;
    }
}
