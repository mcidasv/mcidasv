package edu.wisc.ssec.mcidasv.monitors.memory;

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
    private final List<Monitoring> listeners = new CopyOnWriteArrayList<Monitoring>();

    public MemoryMonitor(final MonitorManager manager, final int threshold, final int cancel) {
        this.manager = manager;
        percentThreshold = threshold;
        percentCancel = cancel;
    }

    public void addMonitor(final Monitoring listener) {
        listeners.add(listener);
    }

    public void removeMonitor(final Monitoring listener) {
        if (!listeners.isEmpty())
            listeners.remove(listener);
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
        if (lastTimeRanGC < 0)
            lastTimeRanGC = now;

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

    public static java.awt.Color doColorThing(final int percent) {
        Float alpha = new Float(percent).floatValue() / 100;
        return new java.awt.Color(1.0f, 0.0f, 0.0f, alpha);
    }
}
