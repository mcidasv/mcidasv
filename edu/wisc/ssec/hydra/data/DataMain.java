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

package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.SpectrumAdapter;
import edu.wisc.ssec.hydra.HyperToBroadBand;
import visad.FlatField;

import java.io.File;

import edu.wisc.ssec.adapter.MultiSpectralData;
import visad.RealTuple;

import java.util.List;
import java.util.HashMap;

/* This class demonstrates the workflow using HYDRA2's VisAD Data adapter classes from the edu.wisc.ssec.adapter package
   and the support package, edu.wisc.ssec.hydra.data, which maps data (various files from various providers) to source
   specific VisAD adapters. As a Java based system, HYDRA2 relies heavily on the Java-NetCDF package, although custom
   Java I/0 readers for file formats not supported by Java-NetCDF could be supported by implementing the interface:
   edu.wisc.ssec.adapter.MultiDimensionReader.

   Most adapters are SwathAdapters or extensions thereof. A swath is an abstraction for 2D gridded set of observation
   points, e.g. radiance, reflectance, cloud top temperature, etc., covering the Earth's surface at a fixed altitude or
   pressure. The most common use-case for HYDRA2: polar orbiting instruments scanning perpendicular to the orbit track, are
   represented as (Track, XTrack) -> observation(s). Geo-stationary instruments with domain (Line, Element) also fit this
   model, except that time sequences are not supported as HYDRA2 does not have animation displays. Since all of HYDRA2's
   displays are navigated, SwathAdapters must define a transformation from the 2D data coordinate system to 2D Earth
   coordinates (Latitude, Longitude) wherein the latter must form a spatially coherent 2D gridded set. This transformation
   is stored as the CoordinateSystem of the VisAD domain's RealTupleType. Some instruments, for example, IASI and CrIS, the
   data coordinate system are not 2D and/or the mapping to the Earth coordinate system doesn't form a spatially coherent 2D
   grid. For these instruments, HYDRA2 offers a Swath view of the data using custom extensions to the top-level class
   that manage the mapping from data coordinates to Earth coordinates and visa-versa and may offer utility to remap to a
   2D grid on a map-projection.

   Notes:

   (1) HYDRA2 is not a database, you need to know things about the target data and how to localize that data.

   (2) Although the classes in edu.wisc.ssec.hydra.data resemble, and sort of mimic, the IDV's data package, these classes
   can't be used interchangeably. The intent is a lighter-weight, interactive system, focussing on multi/hyper-spectral
   data interrogation and analysis, with some support for higher-level derived products when comparison with radiance and
   brightness temperatures/reflectance is instructive.

 */
public class DataMain {


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java MainSetup <file_path>");
            System.exit(1);
        }

        String filePath = args[0];  // Get path from arguments

        try {
            // An instance of a DataSourceFactory
            DataSourceFactory dataSourceFactory = new DataSourceFactory();

            // Create a DataSource specific to the file(s). For adjoining, time consecutive granules, specify multiple
            // files in the list. Uses filename pattern matching internally to determine the correct DataSource.
            DataSource dataSource = dataSourceFactory.createDataSource(new File[]{new File(filePath)});
            System.out.println(dataSource);

            // The list of DataChoices, or data fields, the DataSource can adapt.
            List dataChoices = dataSource.getDataChoices();

            // Get the first one.
            DataChoice dataChoice = (DataChoice) dataChoices.get(0);

            // Multi/Hyper-Spectral DataSources support two-view, (Swath at a particular wavelength, and a spectrum at a
            // particular FOV in the Swath domain), VisAD Data adapters which implement the top-level class:
            // edu.wisc.ssec.adapter.MultiSpectralData, that manage a SwathAdapter and a SpectrumAdapter.
            if (dataSource.hasMultiSpectralData()) {
                // Get the adapter one of two ways: by DataChoice or name printed out above:
                MultiSpectralData msd = dataSource.getMultiSpectralData(dataChoice);
                //MultiSpectralData msd = dataSource.getMultiSpectralData("radiances");

                // The default subset, really a hyper-plane for the backing multi-dimension array, maps the named
                // dimensions of the adapter to their numerical extents. New HashMaps can be made mapping the key-names
                // to different dimension extent [start, stop, stride] to access different regions at different channels.
                MultiDimensionSubset swath_subset = msd.getDefaultSubsetImage();

                // print to see what's what.
                System.out.println(swath_subset);

                // get the swath image corresponding to the subset
                FlatField swath = msd.getImage(swath_subset.getSubset());

                MultiDimensionSubset spec_subset = msd.getDefaultSubsetSpectrum();
                System.out.println(spec_subset);


                // Get the Earth coordinates from the swath or grid coordinates:
                RealTuple latlon = msd.getEarthCoordinates(new float[]{20, 20});
                FlatField spectrum = msd.getSpectrum(latlon);


                // Let's make a synthetic broad-band channel from a hyper-spectral range of wavenumbers:
                float wavenumL = 830.0f;
                float wavenumR = 890.0f;
                float cntrWavenum = 860.0f;  // The Kernel peak response, usually the center

                FlatField cnvld_swath = msd.makeConvolvedRadiances(swath_subset, HyperToBroadBand.Kernel.TH, wavenumL, cntrWavenum, wavenumR);

                // For CrIS only: feed the swath through the re-projection utility, necessary to make an image
                // that makes sense for humans:
                // FlatField grid = edu.wisc.ssec.adapter.CrIS_SDR_Utility.reprojectCrIS_SDR_swath(swath);
                // FlatField cnvld_grid = edu.wisc.ssec.adapter.CrIS_SDR_Utility.reprojectCrIS_SDR_swath(cnvld_swath);
            }


        } catch (Exception ex) {
            System.err.println("Failed to create the data source.");
            ex.printStackTrace();
        }
    }
}

