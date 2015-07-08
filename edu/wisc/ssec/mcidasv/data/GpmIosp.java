/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2015
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

    private static final Logger logger = LoggerFactory.getLogger(GpmIosp.class);

    private NetcdfFile hdfFile;

    private static int[] getLineElementCounts(Group group) throws IOException {
        Variable lat = group.findVariable("Latitude");
        return new int[] { lat.getDimension(0).getLength(), lat.getDimension(1).getLength() };
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

    public void open(RandomAccessFile raf, NetcdfFile ncfile,
                     CancelTask cancelTask) throws IOException
    {
        try {
            hdfFile = NetcdfFile.open(raf.getLocation(), "ucar.nc2.iosp.hdf5.H5iosp", -1, (CancelTask)null, (Object)null);

            Group hdfGroup1 = hdfFile.findGroup("S1");
            int[] counts = getLineElementCounts(hdfGroup1);
            Group s1 = new Group(ncfile, null, "S1");
            Dimension s1Lines = new Dimension("line", counts[0]);
            Dimension s1Elements = new Dimension("ele", counts[1]);
            s1.addDimension(s1Lines);
            s1.addDimension(s1Elements);

            Group hdfGroup2 = hdfFile.findGroup("S2");
            counts = getLineElementCounts(hdfGroup2);
            Group s2 = new Group(ncfile, null, "S2");
            Dimension s2Lines = new Dimension("line", counts[0]);
            Dimension s2Elements = new Dimension("ele", counts[1]);
            s2.addDimension(s2Lines);
            s2.addDimension(s2Elements);

            Variable s1Lat = new Variable(ncfile, s1, null, "latitude", DataType.FLOAT, "line ele");
            s1Lat.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
            Variable s1Lon = new Variable(ncfile, s1, null, "longitude", DataType.FLOAT, "line ele");
            s1Lon.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

            Variable s2Lat = new Variable(ncfile, s2, null, "latitude", DataType.FLOAT, "line ele");
            s2Lat.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
            Variable s2Lon = new Variable(ncfile, s2, null, "longitude", DataType.FLOAT, "line ele");
            s2Lon.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

            Attribute attr = new Attribute("coordinates", "latitude longitude");

            Variable s1Var0 = new Variable(ncfile, s1, null, "Tc_0", DataType.FLOAT, "line ele");
            s1Var0.addAttribute(attr);
            Variable s1Var1 = new Variable(ncfile, s1, null, "Tc_1", DataType.FLOAT, "line ele");
            s1Var1.addAttribute(attr);
            Variable s1Var2 = new Variable(ncfile, s1, null, "Tc_2", DataType.FLOAT, "line ele");
            s1Var2.addAttribute(attr);
            Variable s1Var3 = new Variable(ncfile, s1, null, "Tc_3", DataType.FLOAT, "line ele");
            s1Var3.addAttribute(attr);
            Variable s1Var4 = new Variable(ncfile, s1, null, "Tc_4", DataType.FLOAT, "line ele");
            s1Var4.addAttribute(attr);
            Variable s1Var5 = new Variable(ncfile, s1, null, "Tc_5", DataType.FLOAT, "line ele");
            s1Var5.addAttribute(attr);
            Variable s1Var6 = new Variable(ncfile, s1, null, "Tc_6", DataType.FLOAT, "line ele");
            s1Var6.addAttribute(attr);
            Variable s1Var7 = new Variable(ncfile, s1, null, "Tc_7", DataType.FLOAT, "line ele");
            s1Var7.addAttribute(attr);
            Variable s1Var8 = new Variable(ncfile, s1, null, "Tc_8", DataType.FLOAT, "line ele");
            s1Var8.addAttribute(attr);

            Variable s2Var0 = new Variable(ncfile, s2, null, "Tc_0", DataType.FLOAT, "line ele");
            s2Var0.addAttribute(attr);
            Variable s2Var1 = new Variable(ncfile, s2, null, "Tc_1", DataType.FLOAT, "line ele");
            s2Var1.addAttribute(attr);
            Variable s2Var2 = new Variable(ncfile, s2, null, "Tc_2", DataType.FLOAT, "line ele");
            s2Var2.addAttribute(attr);
            Variable s2Var3 = new Variable(ncfile, s2, null, "Tc_3", DataType.FLOAT, "line ele");
            s2Var3.addAttribute(attr);
            Variable s2Var4 = new Variable(ncfile, s2, null, "Tc_4", DataType.FLOAT, "line ele");
            s2Var4.addAttribute(attr);
            Variable s2Var5 = new Variable(ncfile, s2, null, "Tc_5", DataType.FLOAT, "line ele");
            s2Var5.addAttribute(attr);
            Variable s2Var6 = new Variable(ncfile, s2, null, "Tc_6", DataType.FLOAT, "line ele");
            s2Var6.addAttribute(attr);
            Variable s2Var7 = new Variable(ncfile, s2, null, "Tc_7", DataType.FLOAT, "line ele");
            s2Var7.addAttribute(attr);
            Variable s2Var8 = new Variable(ncfile, s2, null, "Tc_8", DataType.FLOAT, "line ele");
            s2Var8.addAttribute(attr);

            ncfile.addGroup(null, s1);
            ncfile.addGroup(null, s2);

            s1.addVariable(s1Lat);
            s1.addVariable(s1Lon);
            s1.addVariable(s1Var0);
            s1.addVariable(s1Var1);
            s1.addVariable(s1Var2);
            s1.addVariable(s1Var3);
            s1.addVariable(s1Var4);
            s1.addVariable(s1Var5);
            s1.addVariable(s1Var6);
            s1.addVariable(s1Var7);
            s1.addVariable(s1Var8);

            s2.addVariable(s2Lat);
            s2.addVariable(s2Lon);
            s2.addVariable(s2Var0);
            s2.addVariable(s2Var1);
            s2.addVariable(s2Var2);
            s2.addVariable(s2Var3);
            s2.addVariable(s2Var4);
            s2.addVariable(s2Var5);
            s2.addVariable(s2Var6);
            s2.addVariable(s2Var7);
            s2.addVariable(s2Var8);

            ncfile.finish();
        } catch (ClassNotFoundException e) {
            logger.error("error loading HDF5 IOSP", e);
        } catch (IllegalAccessException e) {
            logger.error("java reflection error", e);
        } catch (InstantiationException e) {
            logger.error("error instantiating", e);
        }
    }

    private static int variableToChannel(String variableName) {
        int result = -1;
        if (variableName.startsWith("Tc_")) {
            String temp = variableName.substring(3);
            result = Integer.valueOf(temp);
        }
        return result;
    }

    @Override public Array readData(Variable variable, Section section)
        throws IOException, InvalidRangeException
    {
        logger.trace("variable='{}' group='{}' section='{}'", variable.getShortName(), variable.getParentGroup().getShortName(), section);
//        String groupName = variable.getGroup().getShortName();
        String variableName = variable.getShortName();

        Group hdfGroup = hdfFile.findGroup(variable.getParentGroup().getFullName());
        if (variableName.equals("latitude")) {

            // case matters
            Variable hdfVariable = hdfFile.findVariable(hdfGroup, "Latitude");
            return hdfVariable.read();
        }
        if (variableName.equals("longitude")) {
            // case matters
            Variable hdfVariable = hdfFile.findVariable(hdfGroup, "Longitude");
            return hdfVariable.read();
        }
        if (variableName.startsWith("Tc_")) {
            int channel = variableToChannel(variableName);
            logger.trace("trying to read '{}' channel={}", variableName, channel);
            Variable hdfVariable = hdfFile.findVariable(hdfGroup, "Tc");
            int[] counts = getLineElementCounts(hdfGroup);
            return readChannel(hdfVariable, counts[0], counts[1], channel);
        }

        logger.trace("return null :(");
        return null;
    }

    private Array readChannel(Variable v, int lines, int elements, int channel)
        throws IOException, InvalidRangeException
    {
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
