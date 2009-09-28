package edu.wisc.ssec.mcidasv.monitors.time;

import edu.wisc.ssec.mcidasv.monitors.MonitorEvent;
import edu.wisc.ssec.mcidasv.monitors.MonitorManager.MonitorType;

@SuppressWarnings("serial")
public class TimeMonitorEvent extends MonitorEvent {
    private final String output;
    private String toStr = null;

    public TimeMonitorEvent(final TimeMonitor source, final String output) {
        super(source, MonitorType.TIME);
        this.output = output;
    }

    public String getOutput() {
        return output;
    }

    public String toString() {
        if (toStr == null)
            toStr = String.format("[TimeMonitorEvent%x: source=%s, output=%s]", hashCode(), source, output);
        return toStr;
    }
}
