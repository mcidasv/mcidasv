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

package edu.wisc.ssec.mcidasv.chooser.adde;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.ListSelectionModel;

import org.w3c.dom.Element;

import ucar.unidata.data.AddeUtil;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.TwoFacedObject;
import ucar.visad.UtcDate;
import visad.DateTime;

import edu.wisc.ssec.mcidas.McIDASUtil;

/**
 * Selection widget for ADDE point data
 *
 * @author MetApps Development Team
 * @version $Revision$ $Date$
 */
public class AddeLightningDataChooser extends AddePointDataChooser {


    /**
     * Create a new <code>AddeLightningDataChooser</code> with the preferred
     * list of ADDE servers.
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeLightningDataChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * Get the default station model for this chooser.
     * @return name of default station model
     */
    public String getDefaultStationModel() {
        return "flash";
    }

    /**
     * This allows derived classes to provide their own name for labeling, etc.
     *
     * @return  the dataset name
     */
    public String getDataName() {
        return "Lightning Data";
    }

    /**
     * Get the request string for times particular to this chooser
     *
     * @return request string
     * protected String getTimesRequest() {
     *   StringBuffer buf = getGroupUrl(REQ_POINTDATA, getGroup());
     *   appendKeyValue(buf, PROP_DESCR, getDescriptor());
     *   // this is hokey, but take a smattering of stations.
     *   //appendKeyValue(buf, PROP_SELECT, "'CO US'");
     *   appendKeyValue(buf, PROP_POS, "0");
     *   appendKeyValue(buf, PROP_NUM, "ALL");
     *   appendKeyValue(buf, PROP_PARAM, "DAY TIME");
     *   return buf.toString();
     * }
     */

    /**
     * Get the default datasets for the chooser.  The objects are
     * a descriptive name and the ADDE group/descriptor
     *
     * @return  default datasets.
     */
    protected TwoFacedObject[] getDefaultDatasets() {
        return new TwoFacedObject[] { new TwoFacedObject("NLDN", "LGT/NLDN"),
                                      new TwoFacedObject("USPLN",
                                      "LGT/USPLN") };
    }

    /**
     * Get the increment between times for relative time requests
     *
     * @return time increment (hours)
     */
    public float getRelativeTimeIncrement() {
        return .5f;
    }

    /**
     * Create the date time selection string for the "select" clause
     * of the ADDE URL.
     *
     * @return the select day and time strings
     */
    protected String getDayTimeSelectString() {
        StringBuffer buf = new StringBuffer();
        if (getDoAbsoluteTimes()) {
            buf.append("time ");
            List     times = getSelectedAbsoluteTimes();
            DateTime dt    = (DateTime) times.get(0);
            buf.append(UtcDate.getHMS(dt));
            buf.append(" ");
            dt = (DateTime) times.get(times.size() - 1);
            buf.append(UtcDate.getHMS(dt));
        } else {
            buf.append(getRelativeTimeId());
        }
        return buf.toString();
    }

    /**
     * Get the identifier for relative time.  Subclasses can override.
     * @return the identifier
     */
    protected String getRelativeTimeId() {
        return AddeUtil.RELATIVE_TIME_RANGE;
    }

    /**
     * Get the selection mode for the absolute times panel. Subclasses
     * can override.
     *
     * @return the list selection mode
     */
    protected int getAbsoluteTimeSelectMode() {
        return ListSelectionModel.SINGLE_INTERVAL_SELECTION;
    }


    /**
     * Set the list of available times.
     */
    public void readTimes() {
        clearTimesList();
        ArrayList uniqueTimes = new ArrayList();

        setState(STATE_CONNECTING);
        try {
            float    hours      = getRelativeTimeIncrement();
            int      numTimes   = (int) (24f / hours);
            DateTime currentDay = new DateTime(new Date());
            int day = Integer.parseInt(UtcDate.formatUtcDate(currentDay,
                          "yyyyMMdd"));
            for (int i = 0; i < numTimes; i++) {
                int hour = McIDASUtil.mcDoubleToPackedInteger(i * hours);
                try {
                    DateTime dt =
                        new DateTime(McIDASUtil.mcDayTimeToSecs(day, hour));
                    uniqueTimes.add(dt);
                } catch (Exception e) {}
            }
            setState(STATE_CONNECTED);
            //System.out.println(
            //       "found " + uniqueTimes.size() + " unique times");
        } catch (Exception excp) {
            handleConnectionError(excp);
            return;
        }
        if (getDoAbsoluteTimes()) {
            if ( !uniqueTimes.isEmpty()) {
                setAbsoluteTimes(new ArrayList(uniqueTimes));
            }
            int selectedIndex = getAbsoluteTimes().size() - 1;
            setSelectedAbsoluteTime(selectedIndex);
        }
    }


}

