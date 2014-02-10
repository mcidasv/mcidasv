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

import ucar.unidata.util.LogUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;

import ucar.unidata.data.*;


import visad.*;

import visad.georef.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


/**
 * A data choice that simply holds a reference to a visad.Data object
 *
 * @author IDV development team
 * @version $Revision$
 */
public class ComboDataChoice extends DataChoice {
    private static final List<DataCategory> CATEGORIES = 
        DataCategory.parseCategories("MultiSpectral;IMAGE;");

    /** The data */
    private Data data;


    /**
     *  The bean constructor. We need this for xml decoding.
     */
    public ComboDataChoice() {}


    /**
     * Create a new DataChoice, using the state of the given DataChoice to
     * initialize the new object.
     *
     * @param other      The other data choice.
     */
    public ComboDataChoice(ComboDataChoice other) {
        super(other);
        this.data = other.data;
    }

    /**
     * Create a new DataChoice with a random identifier.
     *
     * @param name Short name of this choice.
     * @param categories List of {@link DataCategory DataCategories}.
     * @param props Properties for this data choice. {@code null} is allowed.
     */
    public ComboDataChoice(String name, List categories, Hashtable props) {
        super(Math.random(), name, name, categories, props);
    }

    public ComboDataChoice(final String id, final String name, final Hashtable props, 
        final Data data) 
    {
        super(id, name, name, CATEGORIES, props);
        this.data = data;
    }

    /**
     * Clone me.
     *
     * @return my clone
     */
    public DataChoice cloneMe() {
        return new ComboDataChoice(this);
    }

    public void setData(Data data) {
      this.data = data;
    }

    /**
     * Return the {@link visad.Data} object that this DataChoice represents.
     *
     * @param category          The {@link DataCategory} used to subset this
     *                          call (usually not used but  placed in here
     *                          just in case it is needed.)
     * @param dataSelection     Allows one to subset the data request (e.g.,
     *                          asking for a smaller set of times, etc.)
     * @param requestProperties Extra selection properties
     *
     * @return The data.
     *
     * @throws DataCancelException   if the request to get data is canceled
     * @throws RemoteException       problem accessing remote data
     * @throws VisADException        problem creating the Data object
     */
    protected Data getData(DataCategory category,
                           DataSelection dataSelection,
                           Hashtable requestProperties)
            throws VisADException, RemoteException, DataCancelException {
        return data;
    }

    public Data getData() {
      return data;
    }

    /**
     * add listener. This is a noop
     *
     * @param listener listener
     */
    public void addDataChangeListener(DataChangeListener listener) {}


    /**
     * Remove the {@link DataChangeListener}.
     *
     * @param listener The {@link DataChangeListener} to remove.
     */
    public void removeDataChangeListener(DataChangeListener listener) {}



}

