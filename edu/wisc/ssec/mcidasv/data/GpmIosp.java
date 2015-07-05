package edu.wisc.ssec.mcidasv.data;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import ucar.unidata.io.RandomAccessFile;

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



//            Variable group1Latitude = hdfGroup1.findVariable("Latitude");
//            Variable group1Longitude = hdfGroup1.findVariable("Longitude");



//            Variable group2Latitude = hdfGroup2.findVariable("Latitude");
//            Variable group2Longitude = hdfGroup2.findVariable("Longitude");

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

            Attribute s1Var0Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s1Var1Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s1Var2Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s1Var3Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s1Var4Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s1Var5Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s1Var6Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s1Var7Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s1Var8Attr = new Attribute("coordinates", "latitude longitude");

            Attribute s2Var0Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s2Var1Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s2Var2Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s2Var3Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s2Var4Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s2Var5Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s2Var6Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s2Var7Attr = new Attribute("coordinates", "latitude longitude");
            Attribute s2Var8Attr = new Attribute("coordinates", "latitude longitude");

            Variable s1Var0 = new Variable(ncfile, s1, null, "Tc_0", DataType.FLOAT, "line ele");
            s1Var0.addAttribute(s1Var0Attr);
            Variable s1Var1 = new Variable(ncfile, s1, null, "Tc_1", DataType.FLOAT, "line ele");
            s1Var1.addAttribute(s1Var1Attr);
            Variable s1Var2 = new Variable(ncfile, s1, null, "Tc_2", DataType.FLOAT, "line ele");
            s1Var2.addAttribute(s1Var2Attr);
            Variable s1Var3 = new Variable(ncfile, s1, null, "Tc_3", DataType.FLOAT, "line ele");
            s1Var3.addAttribute(s1Var3Attr);
            Variable s1Var4 = new Variable(ncfile, s1, null, "Tc_4", DataType.FLOAT, "line ele");
            s1Var4.addAttribute(s1Var4Attr);
            Variable s1Var5 = new Variable(ncfile, s1, null, "Tc_5", DataType.FLOAT, "line ele");
            s1Var5.addAttribute(s1Var5Attr);
            Variable s1Var6 = new Variable(ncfile, s1, null, "Tc_6", DataType.FLOAT, "line ele");
            s1Var6.addAttribute(s1Var6Attr);
            Variable s1Var7 = new Variable(ncfile, s1, null, "Tc_7", DataType.FLOAT, "line ele");
            s1Var7.addAttribute(s1Var7Attr);
            Variable s1Var8 = new Variable(ncfile, s1, null, "Tc_8", DataType.FLOAT, "line ele");
            s1Var8.addAttribute(s1Var8Attr);

            Variable s2Var0 = new Variable(ncfile, s2, null, "Tc_0", DataType.FLOAT, "line ele");
            s2Var0.addAttribute(s2Var0Attr);
            Variable s2Var1 = new Variable(ncfile, s2, null, "Tc_1", DataType.FLOAT, "line ele");
            s2Var1.addAttribute(s2Var1Attr);
            Variable s2Var2 = new Variable(ncfile, s2, null, "Tc_2", DataType.FLOAT, "line ele");
            s2Var2.addAttribute(s2Var2Attr);
            Variable s2Var3 = new Variable(ncfile, s2, null, "Tc_3", DataType.FLOAT, "line ele");
            s2Var3.addAttribute(s2Var3Attr);
            Variable s2Var4 = new Variable(ncfile, s2, null, "Tc_4", DataType.FLOAT, "line ele");
            s2Var4.addAttribute(s2Var4Attr);
            Variable s2Var5 = new Variable(ncfile, s2, null, "Tc_5", DataType.FLOAT, "line ele");
            s2Var5.addAttribute(s2Var5Attr);
            Variable s2Var6 = new Variable(ncfile, s2, null, "Tc_6", DataType.FLOAT, "line ele");
            s2Var6.addAttribute(s2Var6Attr);
            Variable s2Var7 = new Variable(ncfile, s2, null, "Tc_7", DataType.FLOAT, "line ele");
            s2Var7.addAttribute(s2Var7Attr);
            Variable s2Var8 = new Variable(ncfile, s2, null, "Tc_8", DataType.FLOAT, "line ele");
            s2Var8.addAttribute(s2Var8Attr);

//        List<Variable> s1Vars = new ArrayList<>(9);
//        List<Variable> s2Vars = new ArrayList<>(9);
//            rootGroup.addGroup(s1);
//            rootGroup.addGroup(s2);
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
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    @Override public Array readData(Variable variable, Section section)
        throws IOException, InvalidRangeException
    {
        logger.trace("variable='{}' group='{}' section='{}'", variable.getShortName(), variable.getParentGroup().getShortName(), section);
        return null;
    }

    @Override public String getFileTypeId() {
        return "GPM-1C-R-CS";
    }

    @Override public String getFileTypeDescription() {
        return "No Idea!";
    }

    @Override public void close() throws IOException {
        logger.trace("getting called");
    }

    public static void main(String args[]) throws IOException, IllegalAccessException, InstantiationException {
        NetcdfFile.registerIOProvider(GpmIosp.class);
        NetcdfFile ncfile = NetcdfFile.open("/Users/jbeavers/Downloads/mike-gpm-script/1C-R-CS-95W50N74W39N.GPM.GMI.XCAL2014-N.20150109-S122429-E122829.004914.V03C.HDF");
        System.out.println("ncfile = \n" + ncfile);
    }
}
