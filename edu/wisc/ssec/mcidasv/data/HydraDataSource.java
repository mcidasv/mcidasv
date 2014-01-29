/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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

package edu.wisc.ssec.mcidasv.data;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.util.WrapperException;
import visad.Data;
import visad.VisADException;

public class HydraDataSource extends DataSourceImpl  {

    /** List of sources files */
    protected List sources = new ArrayList();

    public static String request;

    /** List of sources files */
    protected List adapters;

    /** for unpersistence */
    protected String oldSourceFromBundles;

    /**
     * Default constructor
     */
    public HydraDataSource() {}

    /**
     * Create a HydraDataSource
     *
     * @param descriptor The datasource descriptor
     * @param newSources List of files or urls
     * @param description The long name
     * @param properties properties
     *
     * @throws VisADException  couldn't create the data
     */
    public HydraDataSource(DataSourceDescriptor descriptor, List newSources,
                           String description, Hashtable properties) 
            throws VisADException {

        super(descriptor, "Hydra", "Hydra", properties);
/*
        System.out.println("HydraDataSource:");
        System.out.println("    descriptor=" + descriptor);
        System.out.println("    sources=" + newSources);
        System.out.println("    description=" + description);
        System.out.println("    properties=" + properties);
*/
        if (newSources != null)
            sources.addAll(newSources);
    }

    /**
     * Can this data source save its dat to local disk
     *
     * @return can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        return !isFileBased() && (getProperty(PROP_SERVICE_HTTP) != null);
    }

    /**
     * Are we getting data from a file or from server
     * 
     * @return is the data from files
     */
    protected boolean isFileBased() {
        if (sources.isEmpty())
            return false;

        return (new File(sources.get(0).toString())).exists();
    }

    /**
     * This is called when the CacheManager detects the need ot clear memory.
     * It is intended to be overwritten by derived classes that are holding cached
     * data that is not in the normal putCache facilities provided by this class
     * since that data is actually managed by the CacheManager
     */
    public void clearCachedData() {
        super.clearCachedData();
    }

    /**
     * Create, if needed, and return the list of adapters.
     * Will return null if there are no valid adapters.
     *
     * @return List of adapters or null
     */
    protected List getAdapters() {
        if ((adapters == null) || (adapters.size() == 0)) {
            try {
                makeAdapters(sources);
            } catch (Exception exc) {
                setInError(true);
                throw new WrapperException(exc);
            }
        }
        if (adapters.size() == 0) {
            adapters = null;
        }
        return adapters;
    }

    /**
     * Make the adapters for the given list of files
     *
     * @param files Data files
     *
     * @throws Exception When bad things happen
     */
    private void makeAdapters(List files) throws Exception {
        adapters = new ArrayList();
    }

    /**
     * Create the list of times associated with this DataSource.
     * @return list of times.
     */
    protected List doMakeDateTimes() {
        List times = new ArrayList();
        return times;
    }

/*
    public boolean canDoGeoSelection() {
       return true;
    }
*/

    /**
     * Get the data for the given DataChoice and selection criteria.
     * @param dataChoice         DataChoice for selection
     * @param category           DataCategory for the DataChoice (not used)
     * @param subset             subsetting criteria
     * @param requestProperties  extra request properties
     * @return  the Data object for the request
     *
     * @throws RemoteException couldn't create a remote data object
     * @throws VisADException  couldn't create the data
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection subset,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        return null;
    }
}
