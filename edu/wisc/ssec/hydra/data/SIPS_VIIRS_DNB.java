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

package edu.wisc.ssec.hydra.data;

import java.io.File;

import edu.wisc.ssec.adapter.SwathAdapter;

import java.util.ArrayList;


public class SIPS_VIIRS_DNB extends SIPS_VIIRS_DataSource {


    public SIPS_VIIRS_DNB(File directory) throws Exception {
        this(directory.listFiles());
    }

    public SIPS_VIIRS_DNB(ArrayList<File> files) throws Exception {
        this(files.toArray(new File[]{null}));
    }

    public SIPS_VIIRS_DNB(File[] files) throws Exception {
        super(files);
    }

    void init() throws Exception {

        String groupName = "observation_data/";
        String geoGroupName = "geolocation_data/";
        String[] arrayNames = new String[]{"DNB_observations"};
        String[] rangeNames = new String[]{"DNB"};
        datGrp = catDNB;

        int nAdapters = 0;

        for (int k = 0; k < arrayNames.length; k++) {
            String arrayName = groupName + arrayNames[k];
            String rangeName = rangeNames[k];

            SwathAdapter adapter = buildAdapter("number_of_pixels", "number_of_lines", arrayName, rangeName,
                    "number_of_pixels", "number_of_lines", geoGroupName + "longitude", geoGroupName + "latitude", null, null, null, "_FillValue");

            setDataChoice(adapter, nAdapters, rangeName);
            nAdapters++;
        }


        bandNames = new String[]{
                "DNB"
        };

        centerWavelength = new float[]{
                0.70f
        };

    }

    public float getNadirResolution(DataChoice choice) {
        return 770f;
    }

    public boolean canUnderstand(File[] files) {

        String name = files[0].getName();

        if (name.startsWith("VNP02DNB")) {
            return true;
        } else if (name.startsWith("VJ102DNB")) {
            return true;
        } else if (name.startsWith("VJ202DNB")) {
            return true;
        }

        return false;
    }

}
