/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2012
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

/**
 * DataSource for Grid files.
 *
 * @author Metapps development team
 * @version $Revision$ $Date$
 */

package ucar.unidata.data.grid;


import ucar.unidata.data.*;

import ucar.unidata.util.Misc;


import ucar.unidata.xml.XmlEncoder;

import visad.VisADException;

import java.rmi.RemoteException;



import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;


/**
 *  An abstract  class that provides a list of 2d and 3d DataCategory objects
 *  for   grid data sources.
 */
public abstract class GridDataSource extends FilesDataSource {

    /** North attribute */
    public static final String ATTR_NORTH = "north";

    /** south attribute */
    public static final String ATTR_SOUTH = "south";

    /** east attribute */
    public static final String ATTR_EAST = "east";

    /** west attribute */
    public static final String ATTR_WEST = "west";

    /** x attribute */
    public static final String ATTR_X = "x";

    /** y attribute */
    public static final String ATTR_Y = "y";

    /** z attribute */
    public static final String ATTR_Z = "z";


    /** List of 2D categories for grids */
    private List twoDCategories;

    /** List of 3D categories for grids */
    private List threeDCategories;

    /** List of 2D categories for time series of grids */
    private List twoDTimeSeriesCategories;

    /** List of 2D ensemble categories for time series of grids */
    private List twoDEnsTimeSeriesCategories;

    /** List of 3D categories for time series of grids */
    private List threeDTimeSeriesCategories;

    /** List of 3D ensemble categories for time series of grids */
    private List threeDEnsTimeSeriesCategories;

    /** List of ens categories for grids */
    private DataCategory ensDCategory;

    /** grid ensemble members */
    public static final String PROP_ENSEMBLEMEMBERS = "prop.gridmembers";

    /**
     * Default constructor; initializes data categories
     */
    public GridDataSource() {
        initCategories();
    }



    /**
     * Create a GridDataSource from the descriptor
     *
     * @param descriptor  the descriptor
     */
    public GridDataSource(DataSourceDescriptor descriptor) {
        super(descriptor);
        initCategories();
    }

    /**
     * Create a GridDataSource from the specification given.
     *
     * @param descriptor       data source descriptor
     * @param source of file   filename or URL
     * @param name             name of this data source
     * @param properties       extra initialization properties
     */
    public GridDataSource(DataSourceDescriptor descriptor, String source,
                          String name, Hashtable properties) {
        super(descriptor, Misc.newList(source), source, name, properties);
        initCategories();
    }


    /**
     * Create a GridDataSource from the specification given.
     *
     * @param descriptor       data source descriptor
     * @param sources          List of files or URLS
     * @param name             name of this data source
     * @param properties       extra initialization properties
     */
    public GridDataSource(DataSourceDescriptor descriptor, List sources,
                          String name, Hashtable properties) {
        super(descriptor, sources, name, properties);
        initCategories();
    }



    /**
     * Initialize the data categories
     */
    public void initCategories() {
        if (twoDTimeSeriesCategories == null) {
            twoDTimeSeriesCategories =
                DataCategory.parseCategories("2D grid;GRID-2D-TIME;");
            twoDEnsTimeSeriesCategories = DataCategory.parseCategories(
                "2D grid;GRID-2D-TIME;ENSEMBLE;");
            twoDCategories = DataCategory.parseCategories("2D grid;GRID-2D;");
            threeDTimeSeriesCategories =
                DataCategory.parseCategories("3D grid;GRID-3D-TIME;");
            threeDEnsTimeSeriesCategories = DataCategory.parseCategories(
                "3D grid;GRID-3D-TIME;ENSEMBLE;");
            threeDCategories =
                DataCategory.parseCategories("3D grid;GRID-3D;");
            ensDCategory = DataCategory.parseCategory("ENSEMBLE", true);
        }
    }

    /**
     * Get the ensemble data categories
     * @return   list of categories
     */
    public DataCategory getEnsDCategory() {
        return ensDCategory;
    }


    /**
     * Get the 2D data categories
     * @return   list of categories
     */
    public List getTwoDCategories() {
        return twoDCategories;
    }

    /**
     * Get the 3D data categories
     * @return   list of categories
     */
    public List getThreeDCategories() {
        return threeDCategories;
    }


    /**
     * Get the list of 2D time series categories
     * @return   list of categories
     */
    public List getTwoDTimeSeriesCategories() {
        return twoDTimeSeriesCategories;
    }

    /**
     * Get the list of 2D time series ensemble categories
     * @return   list of categories
     */
    public List getTwoDEnsTimeSeriesCategories() {
        return twoDEnsTimeSeriesCategories;
    }

    /**
     * Get the list of 3D time series categories
     * @return   list of categories
     */
    public List getThreeDTimeSeriesCategories() {
        return threeDTimeSeriesCategories;
    }

    /**
     * Get the list of 3D time series ensemble categories
     * @return   list of categories
     */
    public List getThreeDEnsTimeSeriesCategories() {
        return threeDEnsTimeSeriesCategories;
    }

    /**
     *  Set the ensemble selection
     *
     *  @param ensMembers  the ensemble memeber selection for this datasource
     */
    public void setEnsembleSelection(List<Integer> ensMembers) {
        if (ensMembers != null) {
            getProperties().put(PROP_ENSEMBLEMEMBERS, ensMembers);
        }
    }

    /**
     *  Get the ensemble selection
     *
     * @return the ensemble selection for this datasource or null
     */
    public List<Integer> getEnsembleSelection() {
        return (List<Integer>) getProperties().get(PROP_ENSEMBLEMEMBERS);
    }

}
