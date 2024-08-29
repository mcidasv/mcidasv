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

import java.util.ArrayList;
import java.util.HashMap;

import ucar.ma2.Range;

public class Granule {


    public int trackLen = 0;

    ArrayList<Granule> granules = null;


    public Granule(int trackLen) {
        this.trackLen = trackLen;
    }

    public Granule() {
        granules = new ArrayList<Granule>();
    }

    public void add(Granule granule) {
        trackLen += granule.trackLen;
        granules.add(granule);
    }


    public ArrayList<Segment> getGranulesSpanned(int start, int count, int stride) {

        ArrayList<Segment> segments = new ArrayList<Segment>();

        Segment segment = new Segment();

        // get start granule and initialize first segment;
        int granIdx = 0;

        segment.start = start;
        segment.granIdx = granIdx;
        int totLen = granules.get(granIdx).trackLen;
        int diff = start - (totLen - 1);
        while (diff > 0) {
            granIdx += 1;
            segment.granIdx = granIdx;
            segment.start = diff - 1; // back to zero-based after the diff
            totLen += granules.get(granIdx).trackLen;
            diff = start - (totLen - 1);
        }


        int segStart = 0;
        int segCnt = 0;

        for (int k = 0; k < count; k++) {
            int pos = start + k * stride;

            if ((pos - (totLen - 1)) <= 0) { // make sure matching zero-based index
                segCnt++;
                segment.count = segCnt;
            } else {
                segments.add(segment); // add middle segments


                granIdx += 1;
                segStart = (pos - 1) - (totLen - 1); //make sure zero-based index

                segment = new Segment();
                segment.granIdx = granIdx;
                segment.start = segStart;
                segCnt = 1;
                segment.count = segCnt;
                totLen += granules.get(granIdx).trackLen;
            }
        }

        // add last, or first if only one.
        segments.add(segment);

        return segments;
    }


    public ArrayList<SegmentRange> getGranulesRanges(int start, int count, int stride) throws Exception {
        ArrayList<SegmentRange> ranges = new ArrayList<SegmentRange>();

        ArrayList<Segment> segments = getGranulesSpanned(start, count, stride);

        for (int k = 0; k < segments.size(); k++) {
            Segment segment = segments.get(k);
            int first = segment.start;
            int last = (segment.count - 1) * stride + first;

            Range rng = new Range(first, last, stride);

            SegmentRange segRng = new SegmentRange();
            segRng.granIdx = segment.granIdx;
            segRng.range = rng;
            ranges.add(segRng);
        }

        return ranges;
    }

}

class Segment {
    int granIdx;
    int start;
    int count;
    int stride;
}
