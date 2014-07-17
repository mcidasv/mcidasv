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
package edu.wisc.ssec.mcidasv.monitors.time;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import ucar.unidata.util.GuiUtils;

import edu.wisc.ssec.mcidasv.monitors.Monitorable;
import edu.wisc.ssec.mcidasv.monitors.Monitoring;

public class TimeMonitor implements Monitorable {

    private final List<Monitoring> listeners = new CopyOnWriteArrayList<>();
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm:ss z");

    public TimeMonitor() {
        // nothin!
    }

    public void addMonitor(final Monitoring listener) {
        listeners.add(listener);
    }

    public void removeMonitor(final Monitoring listener) {
        if (!listeners.isEmpty()) {
            listeners.remove(listener);
        }
    }

    public boolean hasMonitors() {
        return !listeners.isEmpty();
    }

    public void run() {
        Date date = new Date();
        clockFormat.setTimeZone(GuiUtils.getTimeZone());
        String output = "  "+clockFormat.format(date);
        final TimeMonitorEvent event = new TimeMonitorEvent(this, output);
        for (final Monitoring listener : listeners) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    listener.monitorUpdated(event);
                }
            });
        }
    }
}
