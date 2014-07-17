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
package edu.wisc.ssec.mcidasv.monitors.memory;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import ucar.unidata.util.Msg;

import edu.wisc.ssec.mcidasv.monitors.MonitorManager;
import edu.wisc.ssec.mcidasv.monitors.Monitorable;
import edu.wisc.ssec.mcidasv.monitors.Monitoring;

public class MemoryMonitor implements Monitorable {

    private final DecimalFormat fmt = new DecimalFormat("#0");

    private final MonitorManager manager;

    private final int percentThreshold;

    private final int percentCancel;

    private int timesAboveThreshold;

    private long lastTimeRanGC = -1;

    private final List<Monitoring> listeners = new CopyOnWriteArrayList<>();

    public MemoryMonitor(final MonitorManager manager, final int threshold, final int cancel) {
        this.manager = manager;
        percentThreshold = threshold;
        percentCancel = cancel;
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
        double totalMem = Runtime.getRuntime().maxMemory();
        double highMem = Runtime.getRuntime().totalMemory();
        double freeMem = Runtime.getRuntime().freeMemory();
        double usedMem = (highMem - freeMem);

        totalMem = totalMem / 1048576;
        usedMem = usedMem / 1048576;
        highMem = highMem / 1048576;

        int percent = (int)(100.0f * (usedMem / totalMem));
        int stretchedPercent = 0;

        long now = System.currentTimeMillis();
        if (lastTimeRanGC < 0) {
            lastTimeRanGC = now;
        }

        if (percent > percentThreshold) {
            timesAboveThreshold++;
            if (timesAboveThreshold > 5) {
                if (now - lastTimeRanGC > 5000) {
                    manager.scheduleClearCache();
                    lastTimeRanGC = now;
                }
            }
            stretchedPercent = Math.round((percent - percentThreshold) * (100.0f / (100.0f - percentThreshold)));
        } else {
            timesAboveThreshold = 0;
            lastTimeRanGC = now;
        }

        String output = " " + Msg.msg("Memory:") + " " +
                        fmt.format(usedMem) + "/" +
                        fmt.format(highMem) + "/" +
                        fmt.format(totalMem) + " " + Msg.msg("MB");
        final MemoryMonitorEvent event = new MemoryMonitorEvent(this, doColorThing(stretchedPercent), output);
        for (final Monitoring listener : listeners) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    listener.monitorUpdated(event);
                }
            });
        }
    }

    public static Color doColorThing(final int percent) {
        Float alpha = new Float(percent).floatValue() / 100;
        return new Color(1.0f, 0.0f, 0.0f, alpha);
    }
}
