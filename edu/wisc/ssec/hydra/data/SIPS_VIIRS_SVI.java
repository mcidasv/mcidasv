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

import edu.wisc.ssec.adapter.MultiSpectralData;

import java.io.File;

import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.hydra.Hydra;

import java.util.ArrayList;

import ucar.unidata.util.ColorTable;


public class SIPS_VIIRS_SVI extends SIPS_VIIRS_DataSource {


    public SIPS_VIIRS_SVI(File directory) throws Exception {
        this(directory.listFiles());
    }

    public SIPS_VIIRS_SVI(ArrayList<File> files) throws Exception {
        this(files.toArray(new File[]{null}));
    }

    public SIPS_VIIRS_SVI(File[] files) throws Exception {
        super(files);
    }

    void init() throws Exception {

        String groupName = "observation_data/";
        String geoGroupName = "geolocation_data/";
        String[] emisBandNames = new String[]{"I04", "I05"};
        String[] reflBandNames = new String[]{"I01", "I02", "I03"};
        String sensorName = "VIIRS-I";
        datGrp = catI;

        int nAdapters = 0;

        for (int k = 0; k < reflBandNames.length; k++) {
            String arrayName = groupName + reflBandNames[k];
            if (!reader.hasArray(arrayName)) {
                continue;
            }
            String rangeName = reflBandNames[k];
            String prodName = "SV" + rangeName;

            SwathAdapter adapter = buildReflAdapter("number_of_pixels", "number_of_lines", arrayName, rangeName,
                    "number_of_pixels", "number_of_lines", geoGroupName + "longitude", geoGroupName + "latitude", null, null, null, "_FillValue");

            String paramName = "Reflectance";
            MultiSpectralData multiSpectData = NOAA_VIIRS_DataSource.makeMultiSpectralData(adapter, reader, arrayName, "number_of_pixels", "number_of_lines", prodName, sensorName, paramName);
            Irefl.add(multiSpectData);

            setDataChoice(adapter, nAdapters, rangeName);
            nAdapters++;
        }

        for (int k = 0; k < emisBandNames.length; k++) {
            String arrayName = groupName + emisBandNames[k];
            String rangeName = emisBandNames[k];
            String prodName = "SV" + rangeName;
            String btLUTname = groupName + emisBandNames[k] + "_brightness_temperature_lut";

            SwathAdapter adapter = buildEmisAdapter("number_of_pixels", "number_of_lines", arrayName, rangeName,
                    "number_of_pixels", "number_of_lines", geoGroupName + "longitude", geoGroupName + "latitude",
                    null, null, null, "_FillValue", btLUTname);

            String paramName = "BrightnessTemp";
            MultiSpectralData multiSpectData = NOAA_VIIRS_DataSource.makeMultiSpectralData(adapter, reader, arrayName, "number_of_pixels", "number_of_lines", prodName, sensorName, paramName);
            Iemis.add(multiSpectData);

            setDataChoice(adapter, nAdapters, rangeName);
            nAdapters++;
        }

        bandNames = new String[]{
                "I01", "I02", "I03", "I04", "I05"
        };

        centerWavelength = new float[]{
                0.640f, 0.856f, 1.610f, 3.740f, 11.450f
        };
    }

    public ColorTable getDefaultColorTable(DataChoice choice) {
        ColorTable clrTbl = Hydra.grayTable;
        String name = choice.getName();
        if (name.equals("I04") || name.equals("I05")) {
            clrTbl = Hydra.invGrayTable;
        }
        return clrTbl;
    }

    public float getNadirResolution(DataChoice choice) {
        return 380f;
    }

    public boolean canUnderstand(File[] files) {

        String name = files[0].getName();

        if (name.startsWith("VNP02IMG")) {
            return true;
        } else if (name.startsWith("VJ102IMG")) {
            return true;
        } else if (name.startsWith("VJ202IMG")) {
            return true;
        }

        return false;
    }

}
