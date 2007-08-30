package edu.wisc.ssec.mcidasv.data.hydra;

import edu.wisc.ssec.mcidasv.data.HydraDataSource;

import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import ucar.unidata.data.*;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.ui.colortable.ColorTableManager;

import ucar.visad.quantities.CommonUnits;

import visad.*;


/**
 * A data source for HYDRA hdf data
 */
public class HDFHydraDataSource extends HydraDataSource {

    /** Sources file */
    protected String filename;

    /** list of data sets */
    protected List sdsList;

    /** Identifier for Station location */
    private static final String DATA_DESCRIPTION = "Hydra hdf data";

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
     * Make a list of sds descriptors
     *
     * @param fileName  Name of hdf file
     *
     * @return List of sds descriptors
     */
    private List makeSdsDescriptors(String fileName) {
        List descriptors = new ArrayList();
        try {
            HDFFile hf = new HDFFile(fileName);
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
        List categories = DataCategory.parseCategories("GRID-2D");
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
        Data data = null;

        return data;
    }
}

