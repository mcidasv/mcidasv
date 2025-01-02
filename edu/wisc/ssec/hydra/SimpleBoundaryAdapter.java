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

import java.io.*;
import java.util.ArrayList;
import java.net.URL;

import visad.UnionSet;
import visad.Gridded2DSet;
import visad.RealTupleType;
import visad.VisADException;

import java.rmi.RemoteException;

public class SimpleBoundaryAdapter {

    float latMax = 44f;
    float latMin = 22f;
    ;
    float lonWest = 110f;
    float lonEast = 140f;
    boolean pole = false;


    protected int numPtsPolygon;
    protected URL url;

    protected ArrayList<float[][]> polygons = new ArrayList<float[][]>();

    protected ArrayList<float[][]> segments = new ArrayList<float[][]>();


    public SimpleBoundaryAdapter(URL url) throws Exception {
        this.url = url;

        init();
        openSource();
        readFromFile();
        cleanup();
    }

    private void readFromFile() throws Exception {
        try {
            while (true) {
                readHeader();
                if (!skip()) {
                    readPolygonPoints();
                }
            }
        } catch (EOFException e) {
        }
    }

    protected void init() {
    }

    protected void openSource() throws IOException {
    }

    protected void cleanup() throws IOException {
    }

    public synchronized UnionSet getData() throws VisADException, RemoteException {
        for (int k = 0; k < polygons.size(); k++) {
            float[][] pts = polygons.get(k);
            extractSegments(pts);
        }
        int numSegments = segments.size();
        if (numSegments == 0) {
            return null;
        }
        Gridded2DSet[] gsets = new Gridded2DSet[numSegments];
        for (int k = 0; k < numSegments; k++) {
            float[][] pts = segments.get(k);
            int len = pts[0].length;
            Gridded2DSet gset = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple, pts, len);
            gsets[k] = gset;
        }
        UnionSet uset = new UnionSet(gsets);
        return uset;
    }


    protected void readHeader() throws EOFException, IOException {
    }

    protected void readPolygonPoints() throws EOFException, IOException {
    }

    private void extractSegments(float[][] latlon) {
        ArrayList<int[]> endpoints = new ArrayList<int[]>();
        int start = 0;
        int stop = 0;

        int numPtsPolygon = latlon[0].length;
        boolean allInside = true;
        boolean lastIn = false;
        for (int t = 0; t < numPtsPolygon; t++) {
            if (inside(latlon[0][t], latlon[1][t])) {
                if (lastIn) {
                    stop = t;
                } else {
                    start = t;
                    stop = t;
                }
                lastIn = true;
            } else {
                if (lastIn) {
                    // add start/stop pair
                    endpoints.add(new int[]{start, stop});
                }
                lastIn = false;
                allInside = false;
            }
        }
        if (lastIn && !allInside) { //pick up a segment that ends inside
            endpoints.add(new int[]{start, stop});
        }
        if (allInside) { // all pts inside, so no in/out switching
            endpoints.add(new int[]{start, stop});
        }

        if (endpoints.size() > 0) { // iterate over the segment start/stop pairs
            for (int k = 0; k < endpoints.size(); k++) {
                int[] idxs = endpoints.get(k);
                start = idxs[0];
                stop = idxs[1];
                int len = stop - start + 1;
                float[][] segment = new float[2][len];
                System.arraycopy(latlon[0], start, segment[0], 0, len);
                System.arraycopy(latlon[1], start, segment[1], 0, len);
                segments.add(segment);
            }
        }
    }

    public synchronized void setRegion(float latMin, float latMax, float lonWest, float lonEast) {
        segments = new ArrayList<float[][]>();

        this.latMax = latMax;
        this.latMin = latMin;
        this.lonWest = lonWest;
        this.lonEast = lonEast;
        this.pole = false;
    }

    public synchronized void setRegion(float latMin, float latMax) { // has, or getting close, to geographic pole
        segments = new ArrayList<float[][]>();

        this.latMin = latMin;
        this.latMax = latMax;
        this.pole = true;
    }

    private boolean inside(float lat, float lon) {
        if (!(lat <= latMax && lat >= latMin)) {
            return false;
        } else if (pole) {
            return true;
        }

        if (lonWest > 0 && lonEast > 0) {
            if (lon > lonWest && lon < lonEast) return true;
            return false;
        } else if (lonWest < 0 && lonEast < 0) {
            if (lon > lonWest && lon < lonEast) return true;
            return false;
        } else if (lonWest > 0 && lonEast < 0) { // crosses Dateline
            if ((lon >= lonWest && lon <= 180f) || (lon >= -180f && lon <= lonEast)) return true;
            return false;
        } else if (lonWest < 0 && lonEast > 0) { // crosses Grenwich
            if (lon >= lonWest && lon <= lonEast) return true;
            return false;
        } else {
            return false;
        }
    }

    protected boolean skip() throws EOFException, IOException {
        return false;
    }

}
