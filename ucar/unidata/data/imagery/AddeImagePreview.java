/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2023
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

package ucar.unidata.data.imagery;


import edu.wisc.ssec.mcidas.AreaFileException;
import edu.wisc.ssec.mcidas.adde.AddeImageURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.geoloc.ProjectionImpl;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.MapProjectionProjection;

import visad.VisADException;

import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.AreaAdapter;

import java.awt.Point;
import java.awt.image.*;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Created with IntelliJ IDEA.
 * User: yuanho
 * Date: 5/29/13
 * Time: 9:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddeImagePreview {

    /** _more_ */
    AddeImageDescriptor aid;

    /** _more_ */
    private String BandNames[];

    /** _more_ */
    private AreaAdapter adapter;


    /** _more_ */
    private int subSampledPixels;

    /** _more_ */
    private int subSampledScans;

    /** _more_ */
    private BufferedImage preview_image;

    /** _more_ */
    private ProjectionImpl proj;

    /**
     * Construct a AddeImagePreview
     *
     * @param adapter _more_
     * @param aid _more_
     *
     * @throws IOException _more_
     */
    public AddeImagePreview(AreaAdapter adapter, AddeImageDescriptor aid)
            throws IOException {

        this.adapter = adapter;
        this.aid     = aid;

        try {
            init();
        } catch (Exception e) {}
    }

    private static final Logger logger = LoggerFactory.getLogger(AddeImagePreview.class);

    /**
     * _more_
     *
     * @param image_data _more_
     */
    private void createBufferedImage(float image_data[][]) {
        WritableRaster raster    = null;
        int            num_bands = 0;
        if (null != preview_image) {
            preview_image.getSampleModel().getNumBands();
        }
        if ((null == preview_image) || (image_data.length != num_bands)) {
            if (image_data.length == 1) {
                preview_image = new BufferedImage(subSampledPixels,
                        subSampledScans, BufferedImage.TYPE_BYTE_GRAY);
            } else {
                preview_image = new BufferedImage(subSampledPixels,
                        subSampledScans, BufferedImage.TYPE_3BYTE_BGR);
            }
            DataBufferFloat dbuf = new DataBufferFloat(image_data,
                                       subSampledPixels * subSampledScans
                                       * image_data.length);
            SampleModel sampleModel = new BandedSampleModel(0,
                                          subSampledPixels, subSampledScans,
                                          image_data.length);
            raster = Raster.createWritableRaster(sampleModel, dbuf,
                    new Point(0, 0));
            preview_image.setData(raster);
        } else if (1 == num_bands) {
            preview_image.getRaster().setDataElements(0, 0,
                    preview_image.getWidth(), preview_image.getHeight(),
                    image_data[0]);
        } else if (3 == num_bands) {
            preview_image.getRaster().setDataElements(0, 0,
                    preview_image.getWidth(), preview_image.getHeight(),
                    image_data);
        }
    }

    /**
     * _more_
     *
     * @throws AreaFileException _more_
     * @throws IOException _more_
     * @throws VisADException _more_
     */
    private void init()
            throws IOException, VisADException, AreaFileException {

        visad.meteorology.SingleBandedImageImpl ff =
            (visad.meteorology.SingleBandedImageImpl) adapter.getImage();
        AREACoordinateSystem acs = null;
        acs       = new AREACoordinateSystem(adapter.getAreaFile());
        this.proj = new MapProjectionProjection(acs);

        int[] ldir = adapter.getAreaDirectory().getDirectoryBlock();

        subSampledPixels = ldir[9];
        subSampledScans  = ldir[8];

        ucar.unidata.util.Range r = GridUtil.fieldMinMax(ff)[0];
        float[][] image_data = ff.unpackFloats();
        float[][] remapped = remap(image_data, (float)r.getMin(), (float)r.getMax(), 0.0f, 255.0f);
//        createBufferedImage(image_data);
        createBufferedImage(remapped);
    }

    private static final float[][] EMPTY = new float[0][0];

    private static float[][] remap(float[][] imageData, float oldMin, float oldMax, float newMin, float newMax) {
        if (oldMin == oldMax) {
            logger.warn("zero input range: old");
            return EMPTY;
        }

        if (newMin == newMax) {
            logger.warn("zero input range: new");
            return EMPTY;
        }

        boolean reversedInput = false;
        float oMin = Math.min(oldMin, oldMax);
        float oMax = Math.max(oldMin, oldMax);
        if (oMin != oldMin) {
            reversedInput = true;
        }

        boolean reversedOutput = false;
        float nMin = Math.min(newMin, newMax);
        float nMax = Math.max(newMin, newMax);
        if (nMin != newMin) {
            reversedOutput = true;
        }

        float[][] newData = new float[1][imageData[0].length];
        for (int i = 0; i < imageData[0].length; i++) {
//            float portion = (imageData[0][i] - oMin) * (nMax - nMin) / (oMax - oMin);
            float portion;
            if (reversedInput) {
                portion = (oMax - imageData[0][i]) * (nMax - nMin) / (oMax - oMin);
            } else {
                portion = (imageData[0][i] - oMin) * (nMax - nMin) / (oMax - oMin);
            }

//            float result = portion + nMin;
            float result;
            if (reversedOutput) {
                result = nMax - portion;
            } else {
                result = portion + nMin;
            }
            newData[0][i] = result;
        }
        return newData;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public ProjectionImpl getSampledProjection() {
        return proj;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public BufferedImage getPreviewImage() {
        return preview_image;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getActualScans() {
        return subSampledScans;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getActualPixels() {
        return subSampledPixels;
    }

}
