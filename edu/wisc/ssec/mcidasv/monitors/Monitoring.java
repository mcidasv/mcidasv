package edu.wisc.ssec.mcidasv.monitors;

import java.util.EventListener;

public interface Monitoring extends EventListener {
    public void monitorUpdated(final MonitorEvent event);
}
