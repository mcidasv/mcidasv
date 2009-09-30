package edu.wisc.ssec.mcidasv.monitors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import edu.wisc.ssec.mcidasv.monitors.memory.MemoryMonitor;
import edu.wisc.ssec.mcidasv.monitors.time.TimeMonitor;

import ucar.unidata.util.CacheManager;

public class MonitorManager {

    public enum MonitorType { MEMORY, TIME };

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    private final Map<MonitorType, Monitorable> monitors = new ConcurrentHashMap<MonitorType, Monitorable>();

    public MonitorManager() {
        monitors.put(MonitorType.MEMORY, new MemoryMonitor(this, 75, 95));
        monitors.put(MonitorType.TIME, new TimeMonitor());
        scheduler.scheduleWithFixedDelay(monitors.get(MonitorType.MEMORY), 10, 2, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(monitors.get(MonitorType.TIME), 10, 2, TimeUnit.SECONDS);
    }

    public void addListener(final MonitorType type, final Monitoring listener) {
        monitors.get(type).addMonitor(listener);
    }

    public void removeListener(final MonitorType type, final Monitoring listener) {
        monitors.get(type).removeMonitor(listener);
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
