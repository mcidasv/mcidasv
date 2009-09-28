package edu.wisc.ssec.mcidasv.monitors.memory;

import java.awt.Color;

import edu.wisc.ssec.mcidasv.monitors.MonitorEvent;
import edu.wisc.ssec.mcidasv.monitors.MonitorManager.MonitorType;

@SuppressWarnings("serial")
public class MemoryMonitorEvent extends MonitorEvent {

    private final Color color;
    private final String readout;
    private String toStr = null;
    
    public MemoryMonitorEvent(final MemoryMonitor source, final Color color, final String readout) {
        super(source, MonitorType.MEMORY);
        this.color = color;
        this.readout = readout;
    }

    public Color getColor() {
        return color;
    }

    public String getReadout() {
        return readout;
    }

    @Override public String toString() {
        if (toStr == null)
            toStr = String.format("[MemoryMonitorEvent@%x: source=%s, color=%s, readout=%s]", hashCode(), source, color, readout);
        return toStr;
    }
}
