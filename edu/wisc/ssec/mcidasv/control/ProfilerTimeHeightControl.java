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
     * Construct the {@link ucar.visad.display.DisplayMaster DisplayMaster},
     * {@link ucar.visad.display.Displayable Displayable}, frame, and
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
