/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
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

package edu.wisc.ssec.mcidasv.data.hydra;

import edu.wisc.ssec.mcidasv.data.HydraDataSource;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DirectDataChoice;

import ucar.unidata.util.Misc;

import visad.Data;
import visad.VisADException;


/**
 * A data source for HYDRA hdf data
 */
public class HDFHydraDataSource extends HydraDataSource {

    /** Sources file */
    protected String filename;

    /** list of data sets */
    protected List sdsList;

    private static final String DATA_DESCRIPTION = "Hydra hdf data";

    private HDF4File reader;

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    public HDFHydraDataSource() {}

    /**
     * Construct a new HYDRA hdf data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  fileName  name of the hdf file to read
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public HDFHydraDataSource(DataSourceDescriptor descriptor,
                                 String fileName, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(fileName), properties);
/*
        System.out.println("HDFHydraDataSource:");
        System.out.println("   descriptor=" + descriptor); 
        System.out.println("   fileName=" + fileName); 
        System.out.println("   properties=" + properties); 
*/
    }

    /**
     * Construct a new HYDRA hdf data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  sources   List of filenames
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public HDFHydraDataSource(DataSourceDescriptor descriptor,
                                 List newSources, Hashtable properties)
            throws VisADException {
        super(descriptor, newSources, DATA_DESCRIPTION, properties);
/*
        System.out.println("HDFHydraDataSource:");
        System.out.println("   descriptor=" + descriptor); 
        System.out.println("   newSources=" + newSources); 
        System.out.println("   properties=" + properties);
*/
        this.filename = (String)sources.get(0);
        setSDSList(makeSdsDescriptors(filename));
    }


    /**
     * Set the list of SDS descriptors 
     * source.
     *
     * @param l The list of image descriptors.
     */
    private void setSDSList(List l) {
        this.sdsList = l;
    }

    /**
     * Get the list of SDS descriptors 
     *
     * @param l The list of image descriptors.
     */
    public List  getSDSList() {
        return sdsList;
    }

    /**
     * Make a list of sds descriptors
     *
     * @param fileName  Name of hdf file
     *
     * @return List of sds descriptors
     */
    private List makeSdsDescriptors(String fileName) {
        List descriptors = new ArrayList();
        try {
            reader = new HDF4File(fileName);
            HDFFile hf = reader.hdf;
            int numSD = hf.getNumberSDdatasets();
/*
            System.out.println("number of datasets=" + numSD);
            System.out.println("number of globalAttrs=" + hf.num_globalAttrs);
*/
            HDFVariable var = null;
            for (int idx=0; idx<numSD; idx++) {
                var = hf.select(idx);
                HDFVariableInfo info = var.getinfo();
                descriptors.add(info.var_name);
/*
                System.out.print(info.var_name);
                System.out.print("   dimensions=");
                int[] lens = info.dim_lengths;
                for (int k=0;k<info.rank;k++) System.out.print(lens[k]+" ");
                System.out.println("");
*/
            }
/*
            System.out.println(" ");
            HDFAttribute attr = null;
            for (int idx=0; idx<hf.num_globalAttrs; idx++) {
                attr = hf.readattr(idx);
                System.out.println(attr);
                System.out.println("   class=" + attr.type);
            }
*/
        } catch (Exception e) {
            System.out.println("makeSdsDescriptors e=" + e);
        }
//        System.out.println("");
        return descriptors;
    }


    /**
     * Make and insert the <code>DataChoice</code>-s for this
     * <code>DataSource</code>.
     */
    public void doMakeDataChoices() {
        DataChoice choice;
        for (int idx=0; idx<sdsList.size(); idx++) {
            choice = doMakeDataChoice(idx, (String)sdsList.get(idx));
            addDataChoice(choice);
        }
    }

    private DataChoice doMakeDataChoice(int idx, String var) {
        List categories = DataCategory.parseCategories("2D grid;GRID-2D;");
        String name = var;
        DataSelection dataSel = new DataSelection(Misc.newList(new Integer(idx)));
        DirectDataChoice ddc = new DirectDataChoice(this, idx, name, name,
            categories, dataSel);
        return ddc;
    }

    /**
     * Check to see if this <code>HDFHydraDataSource</code> is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof HDFHydraDataSource)) {
            return false;
        }
        return (this == (HDFHydraDataSource) o);
    }



    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {
/*
        System.out.println("McIdasXDataSource getDataInner:");
        System.out.println("   dataChoice=" + dataChoice);
        System.out.println("   category=" + category);
        System.out.println("   dataSelection=" + dataSelection);
        System.out.println("   requestProperties=" + requestProperties);
*/
        HashMap table = SwathAdapter.getEmptyMetadataTable();

        table.put("array_name", "Optical_Depth_Land_And_Ocean");
        table.put("lon_array_name", "Longitude");
        table.put("lat_array_name", "Latitude");
        table.put("XTrack", "Cell_Across_Swath:mod04");
        table.put("Track", "Cell_Along_Swath:mod04");
        table.put("geo_Track", "Cell_Along_Swath:mod04");
        table.put("geo_XTrack", "Cell_Across_Swath:mod04");
        table.put("scale_name", "scale_factor");
        table.put("offset_name", "add_offset");
        table.put("fill_value_name", "_FillValue");
/*
        table.put("array_name", "Cloud_Optical_Thickness");
        table.put("lon_array_name", "Longitude");
        table.put("lat_array_name", "Latitude");
        table.put("XTrack", "Cell_Across_Swath_1km:mod06");
        table.put("Track", "Cell_Along_Swath_1km:mod06");
        table.put("geo_Track", "Cell_Along_Swath_5km:mod06");
        table.put("geo_XTrack", "Cell_Across_Swath_5km:mod06");
        table.put("scale_name", "scale_factor");
        table.put("offset_name", "add_offset");
        table.put("fill_value_name", "_FillValue");
*/

        SwathAdapter swath = new SwathAdapter(reader, table);
        HashMap subset = SwathAdapter.getEmptySubset();

        double[] coords = (double[])subset.get("Track");
        coords[0] = 0.0;
        coords[1] = 202.0;
        //coords[1] = 2029.0;
        coords[2] = 1.0;
        subset.put("Track", coords);

        coords = (double[])subset.get("XTrack");
        coords[0] = 0.0;
        coords[1] = 134.0;
        //coords[1] = 1353.0;
        coords[2] = 1.0;
        subset.put("XTrack", coords);

        Data data = null;
        try {
            data = swath.getData(subset);
           
        } catch (Exception e) {
            System.out.println("getData exception e=" + e);
        }
        return data;
    }
}

