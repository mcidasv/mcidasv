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

    private final List<Monitoring> listeners = new CopyOnWriteArrayList<Monitoring>();
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm:ss z");

    public TimeMonitor() {
        // nothin!
    }

    public void addMonitor(final Monitoring listener) {
        listeners.add(listener);
    }

    public void removeMonitor(final Monitoring listener) {
        if (!listeners.isEmpty())
            listeners.remove(listener);
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
