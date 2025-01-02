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

package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.hydra.Hydra;

import java.io.File;

import visad.VisADException;
import visad.Data;
import visad.FlatField;

import java.rmi.RemoteException;
import java.util.ArrayList;

import ucar.unidata.util.ColorTable;


public class AHIDirectory extends GEOSDirectory {

    public AHIDirectory(File directory) throws Exception {
        this(directory.listFiles());
    }

    public AHIDirectory(ArrayList<File> files) throws Exception {
        this(files.toArray(new File[]{null}));
    }

    public AHIDirectory(File[] files) throws Exception {
        super(files);

        nadirResolution = new float[]{
                1000f, 1000f, 500f, 1000f,
                2000f, 2000f, 2000f, 2000f,
                2000f, 2000f, 2000f, 2000f,
                2000f, 2000f, 2000f, 2000f
        };

        bandNames = new String[]{
                "C01", "C02", "C03", "C04", "c05",
                "C06", "C07", "C08", "C09", "C10",
                "C11", "C12", "C13", "C14", "C15", "C16"
        };

        centerWavelength = new float[]{
                0.47f, 0.51f, 0.64f, 0.86f, 1.6f, 2.3f, 3.9f, 6.2f, 6.9f, 7.3f,
                8.6f, 9.6f, 10.4f, 11.2f, 12.4f, 13.3f
        };

        category = new DataGroup[]{
                cat1KMrefl, cat1KMrefl, catHKMrefl, cat1KMrefl,
                cat2KMrefl, cat2KMrefl, cat2KMemis, cat2KMemis,
                cat2KMemis, cat2KMemis, cat2KMemis, cat2KMemis,
                cat2KMemis, cat2KMemis, cat2KMemis, cat2KMemis
        };

        sensorName = new String[]{
                "AHI_1KM", "AHI_1KM", "AHI_HKM", "AHI_2KM",
                "AHI_2KM", "AHI_2KM", "AHI_2KM", "AHI_2KM",
                "AHI_2KM", "AHI_2KM", "AHI_2KM", "AHI_2KM",
                "AHI_2KM", "AHI_2KM", "AHI_2KM", "AHI_2KM"
        };

        default_stride = new int[]{
                20, 20, 40, 20,
                10, 10, 10, 10,
                10, 10, 10, 10,
                10, 10, 10, 10
        };

        targetList.add("CMI");

        unpack = true;

        zeroBased = false;

        init(files);
    }

    public boolean canUnderstand(File[] files) {
        for (int k = 0; k < files.length; k++) {
            String name = files[k].getName();
            if (!(name.startsWith("HS_H08") || name.contains("HIMAWARI8-AHI"))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "H08 AHI";
    }

    boolean fileBelongsToThis(String filename) {
        if (filename.contains("AHI-L2-CMIP") && filename.endsWith(".nc")) {
            return true;
        }
        return false;
    }


    @Override
    public ColorTable getDefaultColorTable(DataChoice choice) {
        ColorTable clrTbl = Hydra.grayTable;
        String name = choice.getName();
        if (name.equals("C07") || name.equals("C08") || name.equals("C09") || name.equals("C10") || name.equals("C11") || name.equals("C12") || name.equals("C13") || name.equals("C14") || name.equals("C15") || name.equals("B16")) {
            clrTbl = Hydra.invGrayTable;
        }

        return clrTbl;
    }

    @Override
    Data postProcess(DataChoice choice, Data data) throws VisADException, RemoteException {
        if (choice.getName().equals("brightness_temp")) {
            float[][] rngVals = ((FlatField) data).getFloats(false);
            for (int k = 0; k < rngVals[0].length; k++) {
                float fval = rngVals[0][k];
                if (fval < 150f || fval > 340f) {
                    rngVals[0][k] = Float.NaN;
                }
            }
        }
        return data;
    }

}
