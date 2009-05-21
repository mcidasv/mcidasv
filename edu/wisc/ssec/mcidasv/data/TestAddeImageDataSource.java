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

package edu.wisc.ssec.mcidasv.data;


import ucar.unidata.data.*;
import ucar.unidata.data.imagery.*;
import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Misc;

import visad.Data;
import visad.DataReference;
import visad.VisADException;

import visad.data.mcidas.AreaAdapter;

import visad.meteorology.SingleBandedImage;



import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 * A data source for ADDE images. This is a thin wrapper (derived class) around the ImageDataSource
 * which does all of the work.
 *
 * @author Don Murray
 * @version $Revision$ $Date$
 */

public class TestAddeImageDataSource extends TestImageDataSource {


    /**
     *  The parameterless ctor unpersisting.
     */
    public TestAddeImageDataSource() {}

    /**
     *  Create a new TestAddeImageDataSource with an a single image ADDE url.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  image ADDE Url
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */
    public TestAddeImageDataSource(DataSourceDescriptor descriptor, String image,
                               Hashtable properties)
            throws VisADException {
        super(descriptor, new String[] { image }, properties);
    }

    /**
     *  Create a new TestAddeImageDataSource with an array (String) image ADDE urls.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  images Array of  ADDE urls.
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */

    public TestAddeImageDataSource(DataSourceDescriptor descriptor,
                               String[] images, Hashtable properties)
            throws VisADException {
        super(descriptor, images, properties);
    }

    /**
     *  Create a new TestAddeImageDataSource with an array (String) image ADDE urls.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  images Array of  ADDE urls.
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */

    public TestAddeImageDataSource(DataSourceDescriptor descriptor, List images,
                               Hashtable properties)
            throws VisADException {
        super(descriptor, images, properties);
    }


    /**
     *  Create a new TestAddeImageDataSource with the given dataset.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  ids The dataset.
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */
    public TestAddeImageDataSource(DataSourceDescriptor descriptor,
                               ImageDataset ids, Hashtable properties)
            throws VisADException {
        super(descriptor, ids, properties);
    }

    /**
     *  Overwrite base class  method to return the name of this class.
     *
     *  @return The name.
     */
    public String getImageDataSourceName() {
        return "Adde Image Data Source";
    }

    /**
     * Get the name for this data.  Override base class for more info.
     *
     * @return  name for the main data object
     */
    public String getDataName() {
        String dataName =
            (String) getProperty(ucar.unidata.idv.chooser.adde.AddeChooser.DATA_NAME_KEY,
                                 (String) null);
        if (dataName == null) {
            dataName = (String) getProperty(
                ucar.unidata.idv.chooser.adde.AddeChooser.PROP_DATANAME, (String) null);
        }

        if ((dataName == null) || dataName.trim().equals("")) {
            dataName = super.getDataName();
        }
        return dataName;

    }

}

