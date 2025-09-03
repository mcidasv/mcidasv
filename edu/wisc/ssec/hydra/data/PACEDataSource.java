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

package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.adapter.MultiDimensionReader;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.SwathSoundingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


public class PACEDataSource extends AtmSoundingDataSource {
    private static final Logger logger = LoggerFactory.getLogger(PACEDataSource.class);

    public PACEDataSource(File directory) throws Exception {
        this(directory.listFiles());
    }

    public PACEDataSource(ArrayList<File> files) throws Exception {
        this(files.toArray(new File[]{null}));
    }

    public PACEDataSource(File[] files) throws Exception {
        super(files);
    }

    public SwathSoundingData buildAdapter(MultiDimensionReader reader, String xtrack, String track, String levelIndex, String levelsName, float[] levelValues,
                                          String array, String range, String geoXtrack, String geoTrack,
                                          String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                          String fillValueName) {
        // see AtmSoundingDataSource#buildAdapter for the different sorts of things we'll
        // need to provide
        return super.buildAdapter(reader, xtrack, track, levelIndex, levelsName, levelValues, array, range, geoXtrack, geoTrack, lonArray, latArray, arrayDims, lonArrayDims, latArrayDims, fillValueName);
    }

    void init(File[] files) throws Exception {
        NetCDFFile reader = new NetCDFFile(files[0].getAbsolutePath());

        double[] dvals = {0.0050, 0.0161, 0.0384, 0.0769, 0.1370, 0.2244, 0.3454, 0.5064, 0.7140, 0.9753, 1.2972, 1.6872, 2.1526, 2.7009, 3.3398, 4.0770, 4.9204, 5.8776, 6.9567, 8.1655, 9.5119, 11.0038, 12.6492, 14.4559, 16.4318, 18.5847, 20.9224, 23.4526, 26.1829, 29.1210, 32.2744, 35.6505, 39.2566, 43.1001, 47.1882, 51.5278, 56.1260, 60.9895, 66.1253, 71.5398, 77.2396, 83.2310, 89.5204, 96.1138, 103.0172, 110.2366, 117.7775, 125.6456, 133.8462, 142.3848, 151.2664, 160.4959, 170.0784, 180.0183, 190.3203, 200.9887, 212.0277, 223.4415, 235.2338, 247.4085, 259.9691, 272.9191, 286.2617, 300.0000, 314.1369, 328.6753, 343.6176, 358.9665, 374.7241, 390.8926, 407.4738, 424.4698, 441.8819, 459.7118, 477.9607, 496.6298, 515.7200, 535.2322, 555.1669, 575.5248, 596.3062, 617.5112, 639.1398, 661.1920, 683.6673, 706.5654, 729.8857, 753.6275, 777.7897, 802.3714, 827.3713, 852.7880, 878.6201, 904.8659, 931.5236, 958.5911, 986.0666, 1013.9476, 1042.2319, 1070.9170, 1100.0000};

        float[] vals = new float[101];
        for (int k = 0; k < vals.length; k++) {
            vals[k] = (float) dvals[k];
        }

        SwathSoundingData dataBlue = buildAdapter(reader, "number_of_scans", "ccd_pixels", "blue_bands", "blue_wavelength", vals, "TAirStd", "Temperature", "longitude", "latitude",
                "longitude", "latitude", null, new String[]{"fakeDim2", "fakeDim3"}, new String[]{"fakeDim0", "fakeDim1"}, null);

        HashMap subset = dataBlue.getDefaultSubset();
        DataSelection dataSel = new MultiDimensionSubset(subset);
        DataChoice dataChoice = new DataChoice(this, "Temp", null);
        dataChoice.setDataSelection(dataSel);
        myDataChoices.add(dataChoice);
        mySoundingDatas.add(dataBlue);

        SwathSoundingData dataRed = buildAdapter(reader, "number_of_scans", "ccd_pixels", "red_bands", "red_wavelength", vals, "H2OMMRStd", "WV", "longitude", "latitude",
                "longitude", "latitude", null, new String[]{"fakeDim2", "fakeDim3"}, new String[]{"fakeDim0", "fakeDim1"}, null);
        dataRed.setDataRange(new float[]{0, 20});
        subset = dataRed.getDefaultSubset();
        dataSel = new MultiDimensionSubset(subset);
        dataChoice = new DataChoice(this, "WV", null);
        dataChoice.setDataSelection(dataSel);
        myDataChoices.add(dataChoice);
        mySoundingDatas.add(dataRed);

        SwathSoundingData dataSwir = buildAdapter(reader, "number_of_scans", "SWIR_pixels", "SWIR_bands", "SWIR_wavelength", vals, "O3VMRStd", "O3", "longitude", "latitude",
                "longitude", "latitude", null, new String[]{"fakeDim2", "fakeDim3"}, new String[]{"fakeDim0", "fakeDim1"}, null);
        dataSwir.setDataRange(new float[]{0, 20});
        subset = dataSwir.getDefaultSubset();
        dataSel = new MultiDimensionSubset(subset);
        dataChoice = new DataChoice(this, "O3", null);
        dataChoice.setDataSelection(dataSel);
        myDataChoices.add(dataChoice);
        mySoundingDatas.add(dataSwir);
    }

    public boolean canUnderstand(File[] files) {
        return files[0].getName().startsWith("PACE")
               && files[0].getName().contains("L1B")
               && files[0].getName().endsWith(".nc");
    }
}
