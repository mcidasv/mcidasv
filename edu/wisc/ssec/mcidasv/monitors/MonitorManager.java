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
package edu.wisc.ssec.mcidasv.monitors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import edu.wisc.ssec.mcidasv.monitors.memory.MemoryMonitor;
import edu.wisc.ssec.mcidasv.monitors.time.TimeMonitor;

import ucar.unidata.util.CacheManager;

public class MonitorManager {

    public enum MonitorType { MEMORY, TIME };

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    private final Map<MonitorType, Monitorable> monitors = new ConcurrentHashMap<>();

    private final Map<Monitorable, ScheduledFuture<?>> woot = new ConcurrentHashMap<>();

    public MonitorManager() {
        monitors.put(MonitorType.MEMORY, new MemoryMonitor(this, 75, 95));
        monitors.put(MonitorType.TIME, new TimeMonitor());
    }

    public void addListener(final MonitorType type, final Monitoring listener) {
        Monitorable m = monitors.get(type);
        if (!m.hasMonitors()) {
            woot.put(m, scheduler.scheduleWithFixedDelay(m, 0, 2, TimeUnit.SECONDS));
        }
        m.addMonitor(listener);
    }

    public void removeListener(final MonitorType type, final Monitoring listener) {
        Monitorable m = monitors.get(type);
        m.removeMonitor(listener);
        if (!m.hasMonitors()) {
            ScheduledFuture<?> handle = woot.remove(m);
            if (handle != null) {
                handle.cancel(false);
            }
        }
    }

    public void scheduleClearCache() {
        Runnable r = new Runnable() {
            public void run() {
                CacheManager.clearCache();
            }
        };
        scheduler.schedule(r, 1, TimeUnit.SECONDS);
    }
}
