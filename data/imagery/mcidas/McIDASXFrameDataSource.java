package ucar.unidata.data.imagery;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import ucar.unidata.data.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.LogUtil;

import visad.Data;
import visad.DataReference;
import visad.VisADException;
import visad.data.mcidas.AreaAdapter;

import visad.meteorology.SingleBandedImage;


/**
 * A data source for McIDAS-X frames. This is a thin wrapper (derived class) around the McIDASDataSource
 * which does all of the work.
 */

public class McIDASXFrameDataSource extends McIDASDataSource {


    /**
     *  The parameterless ctor unpersisting.
     */
    public McIDASXFrameDataSource() {}

    /**
     *  Create a new McIDASXFrameDataSource with continual refresh
     *  of the currently displayed frame.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param frame      current.
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */
    public McIDASXFrameDataSource(DataSourceDescriptor descriptor, String frame, Hashtable properties)
            throws VisADException {
        super(descriptor, frame, properties);
    }

    /**
     *  Create a new McIDASXFrameDataSource with an array (String) of frames.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param frames     Array of frames.
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */
/*

    public McIDASXFrameDataSource(DataSourceDescriptor descriptor, String[] frames, Hashtable properties)
            throws VisADException {
        super(descriptor, frames, properties);
    }
*/


    /**
     *  Create a new McIDASXFrameDataSource with the given dataset.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  ids       The dataset.
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */
/*
    public McIDASXFrameDataSource(DataSourceDescriptor descriptor, McIDASDataset ids, Hashtable properties)
            throws VisADException {
        super(descriptor, ids, properties);
    }
*/
}
