/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2019
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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;

/**
 * @author tommyj
 *
 */

public class TropomiIOSP extends AbstractIOServiceProvider {

    private static final String LAT = "latitude";

    private static final String LON = "longitude";

    private static final Logger logger = LoggerFactory.getLogger(TropomiIOSP.class);
    
    private static final String BASE_GROUP = "PRODUCT";

    private static final String TROPOMI_FIELD_SEPARATOR = "_";

    // This regular expression matches TROPOMI L2 products
    private static final String TROPOMI_L2_REGEX =
            // Mission Name (ex: S5P)
            "\\w\\w\\w" + TROPOMI_FIELD_SEPARATOR +
            // Type of data: Real-Time, Offline, or Reprocessed
            "(NRTI|OFFL|RPRO)" + TROPOMI_FIELD_SEPARATOR +
            // Product Identifier
            "(L2_|L1B)" + TROPOMI_FIELD_SEPARATOR +
            // Product (can be up to six characters, separator-padded if less, e.g. CH4___)
            "\\w\\w\\w\\w\\w\\w" + TROPOMI_FIELD_SEPARATOR +
            // Start Date and Time (ex: YYYYmmddTHHMMSS)
            "20[0-3]\\d[0-1]\\d[0-3]\\dT[0-2]\\d[0-5]\\d[0-6]\\d" + TROPOMI_FIELD_SEPARATOR +
            // End Date and Time (ex: YYYYmmddTHHMMSS)
            "20[0-3]\\d[0-1]\\d[0-3]\\dT[0-2]\\d[0-5]\\d[0-6]\\d" + TROPOMI_FIELD_SEPARATOR +
            // Orbit Number
            "\\d\\d\\d\\d\\d" + TROPOMI_FIELD_SEPARATOR +
            // Collection Number
            "\\d\\d" + TROPOMI_FIELD_SEPARATOR +
            // Processor Version Number : MMmmpp (Major - Minor - Patch)
            "\\d\\d\\d\\d\\d\\d" + TROPOMI_FIELD_SEPARATOR +
            // Creation Date and Time (ex: YYYYmmddTHHMMSS)
            "20[0-3]\\d[0-1]\\d[0-3]\\dT[0-2]\\d[0-5]\\d[0-6]\\d" +
            // NetCDF suffix
            ".nc";

    /** Compiled representation of {@link #TROPOMI_L2_REGEX}. */
    public static final Pattern TROPOMI_MATCHER =
        Pattern.compile(TROPOMI_L2_REGEX);
    
    private static HashMap<String, String> groupMap = new HashMap<String, String>();

    // Dimensions of a product we can work with, init this early
    private static int[] dimLen = null;
    
    private NetcdfFile hdfFile;
    private static String filename;

    @Override public boolean isValidFile(RandomAccessFile raf)
        throws IOException
    {
        // Uses the regex defined near top
        logger.trace("TropOMI IOSP isValidFile()...");
        filename = Paths.get(raf.getLocation()).getFileName().toString();
        return TROPOMI_MATCHER.matcher(filename).matches();
    }

    @Override public void open(RandomAccessFile raf, NetcdfFile ncfile,
                     CancelTask cancelTask) throws IOException
    {
        logger.trace("TropOMI IOSP open()...");

        // TJJ - kick out anything not supported (most) L2 right now
        if (filename.contains("_L1B_") || filename.contains("_L2__NP")) {
            return;
        }

        try {
            
            hdfFile = NetcdfFile.open(
               raf.getLocation(), "ucar.nc2.iosp.hdf5.H5iosp", -1, (CancelTask) null, (Object) null
            );
            
            // Get the dimension lengths for product data if we haven't yet
            dimLen = getDataShape(hdfFile);
            // Just logging the dimensions here for debugging purposes
            for (int i = 0; i < dimLen.length; i++) {
                logger.trace("Product dimension[" + i + "]: " + dimLen[i]);
            }

            // Populate map pairing group name to any products we deem suitable for visualization
            Map<String, List<Variable>> newGroups = getDataVars(hdfFile, dimLen);

            ncfile.addDimension(null, new Dimension("line", dimLen[1]));
            ncfile.addDimension(null, new Dimension("ele", dimLen[2]));
            populateDataTree(ncfile, newGroups);

            ncfile.finish();
        } catch (ClassNotFoundException e) {
            logger.error("error loading HDF5 IOSP", e);
        } catch (IllegalAccessException e) {
            logger.error("java reflection error", e);
        } catch (InstantiationException e) {
            logger.error("error instantiating", e);
        }
    }

    /*
     * Loop over all data looking for the products we can display
     */

    private static Map<String, List<Variable>> getDataVars(NetcdfFile hdf, int[] dataShape) {
        List<Variable> variables = hdf.getVariables();
        Map<String, List<Variable>> groupsToDataVars = new HashMap<>(variables.size());
        for (Variable v : variables) {
            if (Arrays.equals(dataShape, v.getShape())) {
                String groupName = v.getGroup().getFullNameEscaped();
                if (! groupsToDataVars.containsKey(groupName)) {
                    groupsToDataVars.put(groupName, new ArrayList<Variable>(variables.size()));
                }
                groupsToDataVars.get(groupName).add(v);
            }
        }
        return groupsToDataVars;
    }

    /*
     * Create the group structure and data products for our McV output
     */

    private static void populateDataTree(NetcdfFile ncOut, Map<String, List<Variable>> groupsToVars)
    {
        for (Map.Entry<String, List<Variable>> e : groupsToVars.entrySet()) {
            Group g = new Group(ncOut, null, e.getKey());

            logger.trace("Adding Group: " + g.getFullName());
            // Newly created groups will have path separators converted to underscores
            // We'll need to map back to the original group name for file access
            groupMap.put(g.getFullName(), e.getKey());

            ncOut.addGroup(null, g);

            for (Variable v : e.getValue()) {
                logger.trace("Adding Variable: " + v.getFullNameEscaped());
                addVar(ncOut, g, v);
            }

        }
    }

    /**
     * Fulfill data requests
     * @return Array - an array with the requested data subset
     */

    @Override public Array readData(Variable variable, Section section)
        throws IOException, InvalidRangeException
    {
        String variableName = variable.getShortName();
        
        String groupName = groupMap.get(variable.getGroup().getFullName());
        logger.trace("looking for Group: " + groupName);
        Group hdfGroup = hdfFile.findGroup(groupName);
        Array result;

        logger.trace("TropOMI IOSP readData(), var name: " + variableName);
        Variable hdfVariable = hdfGroup.findVariable(variableName);
        
        logger.trace("found var: " + hdfVariable.getFullName() + 
                " in group: " + hdfVariable.getGroup().getFullName());
        // Need to knock off 1st dimension for Lat and Lon too...
        int[] origin = { 0, 0, 0 };
        int[] size = { 1, dimLen[1], dimLen[2] };
        logger.trace("reading size: 1, " + dimLen[1] + ", " + dimLen[2]);
        result = hdfVariable.read(origin, size).reduce();
        return result;
    }

    /*
     * Test whether file in question is a valid product for this IOSP
     * This method MUST BE LIGHTNING FAST, since it's part of the system
     * process of attempting to infer the proper handler when the user
     * is not certain what the best way to handle the data might be.
     */

    private static boolean validProduct(Variable variable) {
        int[] varShape = variable.getShape();
        if (varShape.length != dimLen.length) return false;
        // Same dimensions, make sure each individual dimension matches
        for (int i = 0; i < varShape.length; i++) {
            if (varShape[i] != dimLen[i]) return false;
        }
        return true;
    }

    /*
     * Get the shape of valid data products. We consider anything that matches
     * the geolocation bounds to be valid.
     */

    private static int[] getDataShape(NetcdfFile hdf) {
        Group productGroup = hdf.findGroup(BASE_GROUP);
        // Shape of valid data products will match that of geolocation, so base on LAT or LON
        Variable geoVar = productGroup.findVariable(LAT);
        return new int[] {
            geoVar.getDimension(0).getLength(),
            geoVar.getDimension(1).getLength(),
            geoVar.getDimension(2).getLength()
        };
    }

    /*
     * Add a variable to the set of available products.
     */

    private static void addVar(NetcdfFile nc, Group g, Variable vIn) {

        logger.trace("Evaluating: " + vIn.getFullName());
        if (validProduct(vIn)) {
            Variable v = new Variable(nc, g, null, vIn.getShortName(), DataType.FLOAT, "line ele");
            if (vIn.getShortName().equals(LAT)) {
                v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
                v.addAttribute(new Attribute("coordinates", "latitude longitude"));
                logger.trace("including: " + vIn.getFullName());
            } else if (vIn.getShortName().equals(LON)) {
                v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
                v.addAttribute(new Attribute("coordinates", "latitude longitude"));
                logger.trace("including: " + vIn.getFullName());
            } else {
                v.addAttribute(new Attribute("coordinates", "latitude longitude"));
                logger.trace("including: " + vIn.getFullName());
            }
            List<Attribute> attList = vIn.getAttributes();
            for (Attribute a : attList) {
                v.addAttribute(a);
            }
            logger.trace("adding vname: " + vIn.getFullName() + " to group: " + g.getFullName());
            
            g.addVariable(v);
        }
    }
    
    @Override public String getFileTypeId() {
        return "TropOMI";
    }

    @Override public String getFileTypeDescription() {
        return "TROPOspheric Monitoring Instrument";
    }

    @Override public void close() throws IOException {
        hdfFile.close();
    }

    public static void main(String args[]) throws IOException, IllegalAccessException, InstantiationException {
        NetcdfFile.registerIOProvider(TropomiIOSP.class);
    }
    
}
