package edu.wisc.ssec.mcidasv.data;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

public class GpmIosp extends AbstractIOServiceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GpmIosp.class);

    private NetcdfFile hdfFile;

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

    }

    @Override public Array readData(Variable variable, Section section)
        throws IOException, InvalidRangeException
    {
        logger.trace("variable='{}' group='{}' section='{}'", variable.getShortName(), variable.getParentGroup().getShortName(), section);
        return null;
    }

    @Override public String getFileTypeId() {
        return null;
    }

    @Override public String getFileTypeDescription() {
        return null;
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
