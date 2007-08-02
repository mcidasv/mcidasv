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
public class HDFVHydraDataSource extends HydraDataSource {

    /** Sources file */
    protected String filename;

    /** list of data sets */
    protected List sdsList;

    private HDF hdf;

    /** Identifier for Station location */
    private static final String DATA_DESCRIPTION = "Hydra hdf data";

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    public HDFVHydraDataSource() {}

    /**
     * Construct a new HYDRA hdf data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  fileName  name of the hdf file to read
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public HDFVHydraDataSource(DataSourceDescriptor descriptor,
                                 String fileName, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(fileName), properties);
/*
        System.out.println("HDFVHydraDataSource:");
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
    public HDFVHydraDataSource(DataSourceDescriptor descriptor,
                                 List newSources, Hashtable properties)
            throws VisADException {
        super(descriptor, newSources, DATA_DESCRIPTION, properties);
/*
        System.out.println("HDFVHydraDataSource:");
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
            this.hdf = hf.hdf;
            //System.out.println("number of datasets=" + hf.num_SDdatasets);
            HDFVariable var = null;
            for (int idx=0; idx<hf.num_SDdatasets; idx++) {
                var = hf.select(idx);
                HDFVariableInfo info = var.getinfo();
                descriptors.add(info.var_name);
            }
        } catch (Exception e) {
            System.out.println("makeSdsDescriptors e=" + e);
        }
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
     * Check to see if this <code>HDFVHydraDataSource</code> is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof HDFVHydraDataSource)) {
            return false;
        }
        return (this == (HDFVHydraDataSource) o);
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
        try {
            HDFVFile hdfvFile = new HDFVFile(hdf, filename);
            HDFVData hdfvData = hdfvFile.select(dataChoice.getDescription());
            //float[] fdata = hdfvData.read(int nRecs, int startIdx);
        } catch (Exception e) {
            System.out.println("getDataInner e=" + e);
        }
        return data;
    }
}

