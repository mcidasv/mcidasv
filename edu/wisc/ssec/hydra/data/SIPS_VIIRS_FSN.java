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
import edu.wisc.ssec.hydra.Hydra;

import java.util.ArrayList;

import ucar.unidata.util.ColorTable;


public class SIPS_VIIRS_FSN extends SIPS_VIIRS_DataSource {


    public SIPS_VIIRS_FSN(File directory) throws Exception {
        this(directory.listFiles());
    }

    public SIPS_VIIRS_FSN(ArrayList<File> files) throws Exception {
        this(files.toArray(new File[]{null}));
    }

    public SIPS_VIIRS_FSN(File[] files) throws Exception {
        super(files);
    }

    void init() throws Exception {

        String groupName = "observation_data/";
        String geoGroupName = "geolocation_data/";
        String[] emisBandNames = new String[]{"M12", "M13", "M14", "M15", "M16", "Fusion15", "Fusion16",
                "MODIS23", "MODIS24", "MODIS25", "MODIS27", "MODIS28", "MODIS29", "MODIS30", "MODIS31", "MODIS32", "MODIS33", "MODIS34", "MODIS35", "MODIS36"};
        String[] reflBandNames = new String[]{"M01", "M02", "M03", "M04", "M05", "M06", "M07", "M08", "M09", "M10", "M11"};

        int nAdapters = 0;
        datGrp = catM;

        for (int k = 0; k < reflBandNames.length; k++) {
            String arrayName = groupName + reflBandNames[k];
            if (!reader.hasArray(arrayName)) {
                continue;
            }
            String rangeName = reflBandNames[k];

            SwathAdapter adapter = buildReflAdapter("number_of_pixels", "number_of_lines", arrayName, rangeName,
                    "number_of_pixels", "number_of_lines", geoGroupName + "longitude", geoGroupName + "latitude", null, null, null, "_FillValue");

            setDataChoice(adapter, nAdapters, rangeName);
            nAdapters++;
        }

        for (int k = 0; k < emisBandNames.length; k++) {
            String arrayName = groupName + emisBandNames[k];
            String rangeName = emisBandNames[k];
            String radLUTname = groupName + emisBandNames[k] + "_radiance_lut";
            String btLUTname = groupName + emisBandNames[k] + "_brightness_temperature_lut";

            SwathAdapter adapter = buildEmisAdapter("number_of_pixels", "number_of_lines", arrayName, rangeName,
                    "number_of_pixels", "number_of_lines", geoGroupName + "longitude", geoGroupName + "latitude",
                    null, null, null, "_FillValue", btLUTname);

            setDataChoice(adapter, nAdapters, rangeName);
            nAdapters++;
        }

        bandNames = new String[]{
                "M01", "M02", "M03", "M04", "M05",
                "M06", "M07", "M08", "M09", "M10",
                "M11", "M12", "M13", "M14", "M15",
                "M16", "Fusion15", "Fusion16",
                "MODIS23", "MODIS24", "MODIS25", "MODIS27", "MODIS28", "MODIS29", "MODIS30", "MODIS31", "MODIS32", "MODIS33", "MODIS34", "MODIS35", "MODIS36"
        };

        centerWavelength = new float[]{
                0.412f, 0.445f, 0.488f, 0.555f, 0.672f,
                0.746f, 0.865f, 1.240f, 1.378f, 1.61f, 2.250f, 3.700f, 4.050f, 8.550f, 10.763f, 12.013f, 10.763f, 12.013f,
                0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f
        };

    }

    public ColorTable getDefaultColorTable(DataChoice choice) {
        ColorTable clrTbl = Hydra.grayTable;
        String name = choice.getName();
        if (name.equals("M12") || name.equals("M13") || name.equals("M14") || name.equals("M15") || name.equals("M16") || name.equals("Fusion15") || name.equals("Fusion16") ||
                name.equals("MODIS23") || name.equals("MODIS24") || name.equals("MODIS25") || name.equals("MODIS27") || name.equals("MODIS28") || name.equals("MODIS29") || name.equals("MODIS30") || name.equals("MODIS31") || name.equals("MODIS32") || name.equals("MODIS33") || name.equals("MODIS34") || name.equals("MODIS35") || name.equals("MODIS36")) {
            clrTbl = Hydra.invGrayTable;
        }
        return clrTbl;
    }

    public float getNadirResolution(DataChoice choice) {
        return 770f;
    }

    @Override
    public boolean canUnderstand(File[] files) {
        String name = files[0].getName();

        if (name.startsWith("VNP02FSN")) {
            return true;
        } else if (name.startsWith("VJ102FSN")) {
            return true;
        } else if (name.startsWith("VJ202FSN")) {
            return true;
        }

        return false;
    }

}
