package edu.wisc.ssec.mcidasv.control;

import java.rmi.RemoteException;

import ucar.unidata.data.DataChoice;

import visad.VisADException;

/**
 * Rather trivial extension to the IDV's {@link ucar.unidata.idv.control.ProfilerTimeHeightControl}.
 * All this class does is {@literal "observe"} changes to its {@code isLatestOnLeft}
 * field. These get persisted between sessions.
 */
public class ProfilerTimeHeightControl 
    extends ucar.unidata.idv.control.ProfilerTimeHeightControl 
{
    /** Pref ID! */
    public static final String PREF_WIND_PROFILER_LATEST_LEFT = "mcidasv.control.latestleft";

    /**
     *  Default Constructor; does nothing. See init() for creation actions.
     */
    public ProfilerTimeHeightControl() {}

    /**
     * Construct the {@link DisplayMaster}, {@link Displayable}, frame, and 
     * controls. Overridden in McIDAS-V so that we can force the value of 
     * {@code isLatestOnLeft} to its previous value (defaults to {@code false}).
     *
     * @param dataChoice {@link DataChoice} to use.
     * 
     * @return boolean {@code true} if {@code dataChoice} is ok.
     *
     * @throws RemoteException Java RMI error
     * @throws VisADException VisAD Error
     */
    @Override public boolean init(DataChoice dataChoice) 
        throws VisADException, RemoteException 
    {
        isLatestOnLeft = getIdv().getObjectStore().get(PREF_WIND_PROFILER_LATEST_LEFT, false);
        return super.init(dataChoice);
    }

    /**
     * Set whether latest data is displayed on the left or right
     * side of the plot. Used by both {@literal "property"} and 
     * {@literal "XML"} persistence.
     * 
     * @param yesorno {@code true} if latest data should appear on the left.
     */
    @Override public void setLatestOnLeft(final boolean yesorno) {
        isLatestOnLeft = yesorno;
        getIdv().getObjectStore().put(PREF_WIND_PROFILER_LATEST_LEFT, yesorno);
    }

    /**
     * Set the XAxis values. Overriden in McIDAS-V so that changes to the 
     * {@code isLatestOnLeft} field are captured.
     *
     * @throws VisADException Couldn't set the values
     */
    @Override protected void setXAxisValues() throws VisADException {
        setLatestOnLeft(isLatestOnLeft);
        super.setXAxisValues();
    }
}
