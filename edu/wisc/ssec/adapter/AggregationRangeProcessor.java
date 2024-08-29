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

public class AggregationRangeProcessor extends RangeProcessor {

    ArrayList<RangeProcessor> rangeProcessors = new ArrayList<RangeProcessor>();

    int rngIdx = 0;

    /**
     * Creates a new AggregationRangeProcessor instance from supplied RangeProcessors. One for each granule.
     *
     * @param rngProcessors Supplied RangeProcessors for each individual granule.
     * @throws Exception If there are granules with multiDimensionScale and others without it, in the granule set.
     */
    public AggregationRangeProcessor(RangeProcessor[] rngProcessors) throws Exception {
        int num = 0;
        for (int k = 0; k < rngProcessors.length; k++) {
            if (rngProcessors[k].hasMultiDimensionScale()) {
                num++;
            }
            rangeProcessors.add(rngProcessors[k]);
        }
        if (num > 0 && num != rngProcessors.length) {
            throw new Exception("AggregationRangeProcessor: all or none can define a multiDimensionScale");
        } else if (num == rngProcessors.length) {
            setHasMultiDimensionScale(true);
        }

    }

    /**
     * Creates a new AggregationRangeProcessor instance, using the provided GranuleAggregation instance,
     * metadata. Generates a RangeProcessor from metadata for each reader in the GranuleAggregation.
     *
     * @param aggrReader A GranuleAggregation object, to extract readers.
     * @param metadata   A metadata HashMap object used by the RangeProcessor.
     * @throws Exception If there are granules with multiDimensionScale and others without it, in the granule set.
     */
    public AggregationRangeProcessor(GranuleAggregation aggrReader, HashMap metadata) throws Exception {
        super();

        ArrayList readers = aggrReader.getReaders();

        int num = 0;

        for (int rdrIdx = 0; rdrIdx < readers.size(); rdrIdx++) {
            RangeProcessor rngProcessor =
                    RangeProcessor.createRangeProcessor((MultiDimensionReader) readers.get(rdrIdx), metadata);

            if (rngProcessor.hasMultiDimensionScale()) {
                num++;
            }

            rangeProcessors.add(rngProcessor);
        }

        if (num > 0 && num != readers.size()) {
            throw new Exception("AggregationRangeProcessor: all or none can define a multiDimensionScale");
        } else if (num == readers.size()) {
            setHasMultiDimensionScale(true);
        }

    }

    /**
     * Creates a new AggregationRangeProcessor instance, using the provided GranuleAggregation instance,
     * and supplied RangeProcessor.
     *
     * @param aggrReader   A GranuleAggregation object, to extract readers.
     * @param rngProcessor A supplied RangeProcessor.
     * @throws Exception If there are granules with multiDimensionScale and others without it, in the granule set.
     */
    public AggregationRangeProcessor(GranuleAggregation aggrReader, RangeProcessor rngProcessor) throws Exception {
        super();

        ArrayList readers = aggrReader.getReaders();

        int num = 0;

        for (int rdrIdx = 0; rdrIdx < readers.size(); rdrIdx++) {

            if (rngProcessor.hasMultiDimensionScale()) {
                num++;
            }

            rangeProcessors.add(rngProcessor);
        }

        if (num > 0 && num != readers.size()) {
            throw new Exception("AggregationRangeProcessor: all or none can define a multiDimensionScale");
        } else if (num == readers.size()) {
            setHasMultiDimensionScale(true);
        }

    }

    public synchronized void setWhichRangeProcessor(int index) {
        rngIdx = index;
    }

    public synchronized void setMultiScaleIndex(int idx) {
        rangeProcessors.get(rngIdx).setMultiScaleIndex(idx);
    }


    public synchronized float[] processRange(byte[] values, HashMap subset) {
        return rangeProcessors.get(rngIdx).processRange(values, subset);
    }

    public synchronized float[] processRange(short[] values, HashMap subset) {
        return rangeProcessors.get(rngIdx).processRange(values, subset);
    }

    public synchronized float[] processRange(int[] values, HashMap subset) {
        return rangeProcessors.get(rngIdx).processRange(values, subset);
    }

    public synchronized float[] processRange(float[] values, HashMap subset) {
        return rangeProcessors.get(rngIdx).processRange(values, subset);
    }

    public synchronized double[] processRange(double[] values, HashMap subset) {
        return rangeProcessors.get(rngIdx).processRange(values, subset);
    }
}