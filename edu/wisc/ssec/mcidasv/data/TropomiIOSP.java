/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
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
import java.util.List;

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

public class TropomiIOSP extends AbstractIOServiceProvider {

    private static final String LAT = "latitude";

    private static final String LON = "longitude";

    private static final Logger logger = LoggerFactory.getLogger(TropomiIOSP.class);
    
    private static final String BASE_GROUP = "PRODUCT";

    // Dimensions of a product we can work with, init this early
    private static int[] dimLen = null;
    
    private NetcdfFile hdfFile;

    @Override public boolean isValidFile(RandomAccessFile raf)
        throws IOException
    {
        // this isn't exactly a suitable test - just proof-of-concept for now
        logger.trace("TropOMI IOSP isValidFile()...");
        String location = raf.getLocation();
        return location.contains("S5P_") && location.endsWith(".nc");
    }

    @Override public void open(RandomAccessFile raf, NetcdfFile ncfile,
                     CancelTask cancelTask) throws IOException
    {
        logger.trace("TropOMI IOSP open()...");
        try {
            
            hdfFile = NetcdfFile.open(
               raf.getLocation(), "ucar.nc2.iosp.hdf5.H5iosp", -1, (CancelTask) null, (Object) null
            );
            
            createGroupFromHdf(ncfile, BASE_GROUP);
            ncfile.finish();
        } catch (ClassNotFoundException e) {
            logger.error("error loading HDF5 IOSP", e);
        } catch (IllegalAccessException e) {
            logger.error("java reflection error", e);
        } catch (InstantiationException e) {
            logger.error("error instantiating", e);
        }
    }

    private void createGroupFromHdf(NetcdfFile ncOut, String groupName)
        throws IOException
    {
        Group productGroup = new Group(ncOut, null, groupName);
        if (dimLen == null) dimLen = getDimensionLengths(hdfFile, groupName);
        for (int i = 0; i < dimLen.length; i++) {
            logger.trace("group Dim: " + dimLen[i]);
        }
        
        ncOut.addGroup(null, productGroup);
        productGroup.addDimension(new Dimension("line", dimLen[1]));
        productGroup.addDimension(new Dimension("ele", dimLen[2]));

        Group hdfGroup = hdfFile.findGroup(groupName);
        List<Variable> varList = hdfGroup.getVariables();
        for (Variable v : varList) {
            addVar(ncOut, productGroup, v);
        }
       
    }

    @Override public Array readData(Variable variable, Section section)
        throws IOException, InvalidRangeException
    {
        String variableName = variable.getShortName();
        
        String groupName = "PRODUCT";
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

    private static boolean validProduct(Variable variable) {
        int[] varShape = variable.getShape();
        if (varShape.length != dimLen.length) return false;
        // Same dimensions, make sure each individual dimension matches
        for (int i = 0; i < varShape.length; i++) {
            if (varShape[i] != dimLen[i]) return false;
        }
        return true;
    }

    private static int[] getDimensionLengths(NetcdfFile hdf, String groupName) throws IOException {
        Group group = hdf.findGroup(groupName);
        Variable geoVar = group.findVariable("latitude");
        
        return new int[] {
            geoVar.getDimension(0).getLength(),
            geoVar.getDimension(1).getLength(),
            geoVar.getDimension(2).getLength()
        };
    }

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
            logger.trace("adding vname: " + vIn.getFullName());
            
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
