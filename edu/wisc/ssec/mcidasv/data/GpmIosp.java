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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.mcidasv.data;

import java.io.IOException;

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

public class GpmIosp extends AbstractIOServiceProvider {

    private static final String LAT = "Latitude";

    private static final String LON = "Longitude";

    private static final String TC_PREFIX = "Tc_";

    private static final Logger logger = LoggerFactory.getLogger(GpmIosp.class);

    private NetcdfFile hdfFile;

    private static int[] getDimensionLengths(NetcdfFile hdf, String groupName) throws IOException {
        Group group = hdf.findGroup(groupName);
        Variable tc = group.findVariableLocal("Tc");
        
        return new int[] {
            tc.getDimension(0).getLength(),
            tc.getDimension(1).getLength(),
            tc.getDimension(2).getLength()
        };
    }

    private static void addVar(NetcdfFile nc, Group g, int channel) {
        String varName = TC_PREFIX + channel;
        Variable v = new Variable(nc, g, null, varName, DataType.FLOAT, "line ele");
        v.addAttribute(new Attribute("coordinates", "latitude longitude"));
        g.addVariable(v);
    }

    private static int variableToChannel(String variableName) {
        int result = -1;
        if (variableName.startsWith("Tc_")) {
            String temp = variableName.substring(3);
            result = Integer.valueOf(temp);
        }
        return result;
    }

    private static void addLatitude(NetcdfFile nc, Group g) {
        Variable lat = new Variable(nc, g, null, LAT, DataType.FLOAT, "line ele");
        lat.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
        g.addVariable(lat);
    }

    private static void addLongitude(NetcdfFile nc, Group g) {
        Variable lon = new Variable(nc, g, null, LON, DataType.FLOAT, "line ele");
        lon.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
        g.addVariable(lon);
    }


    @Override public boolean isValidFile(RandomAccessFile raf)
        throws IOException
    {
        // this isn't exactly a suitable test--but maybe it'll work for now?
        // the better check is to probably try opening the location as a netcdf
        // file and looking for S1/S2 groups and possibly S1/Tc S2/Tc vars?
        String location = raf.getLocation();
        return location.contains("1C-R-CS") && location.endsWith(".HDF");
    }

    @Override public void open(RandomAccessFile raf, NetcdfFile ncfile,
                     CancelTask cancelTask) throws IOException
    {
        try {
            hdfFile = NetcdfFile.open(raf.getLocation(), "ucar.nc2.iosp.hdf5.H5iosp", -1, (CancelTask)null, (Object)null);
            createGroupFromHdf(ncfile, "S1");
            createGroupFromHdf(ncfile, "S2");
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
        Group s1 = new Group(ncOut, null, groupName);
        int[] dimLen = getDimensionLengths(hdfFile, groupName);
        ncOut.addGroup(null, s1);
        s1.addDimension(new Dimension("line", dimLen[0]));
        s1.addDimension(new Dimension("ele", dimLen[1]));
        addLatitude(ncOut, s1);
        addLongitude(ncOut, s1);
        for (int i = 0; i < dimLen[2]; i++) {
            addVar(ncOut, s1, i);
        }
    }

    @Override public Array readData(Variable variable, Section section)
        throws IOException, InvalidRangeException
    {
        String variableName = variable.getShortName();
        String groupName = variable.getParentGroup().getFullName();
        Group hdfGroup = hdfFile.findGroup(groupName);
        Array result;

        if (variableName.equals(LAT) || variableName.equals(LON)) {
            Variable hdfVariable = hdfFile.findVariable(hdfGroup, variableName);
            result = hdfVariable.read();
        } else if (variableName.startsWith("Tc_")) {
            int channel = variableToChannel(variableName);
            int[] counts = getDimensionLengths(hdfFile, groupName);
            Variable hdfVariable = hdfFile.findVariable(hdfGroup, "Tc");
            result = readChannel(hdfVariable, counts[0], counts[1], channel);
        } else {
            result = null;
        }
        return result;
    }

    private Array readChannel(Variable v, int lines, int elements, int channel)
        throws IOException, InvalidRangeException
    {
        // "S1/Tc" and "S2/Tc" (aka "v") is laid out like "line, ele, chan"
        // this origin/size business is the notation required for per-channel
        // access
        // see "Reading data from a Variable" from
        // http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/tutorial/NetcdfFile.html
        int[] origin = { 0, 0, channel };
        int[] size = { lines, elements, 1};
        return v.read(origin, size).reduce();
    }

    @Override public String getFileTypeId() {
        return "GPM-1C-R-CS";
    }

    @Override public String getFileTypeDescription() {
        return "No Idea!";
    }

    @Override public void close() throws IOException {
        logger.trace("getting called");
        hdfFile.close();
    }

    public static void main(String args[]) throws IOException, IllegalAccessException, InstantiationException {
        NetcdfFile.registerIOProvider(GpmIosp.class);
        NetcdfFile ncfile = NetcdfFile.open("/Users/jbeavers/Downloads/mike-gpm-script/1C-R-CS-95W50N74W39N.GPM.GMI.XCAL2014-N.20150109-S122429-E122829.004914.V03C.HDF");
        System.out.println("ncfile = \n" + ncfile);
    }
}
