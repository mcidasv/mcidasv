package edu.wisc.ssec.mcidasv.monitors;

import java.util.EventObject;

import edu.wisc.ssec.mcidasv.monitors.MonitorManager.MonitorType;

public abstract class MonitorEvent extends EventObject {
    protected final MonitorType type;
    public MonitorEvent(final Object source, final MonitorType type) {
        super(source);
        this.type = type;
    }

    public MonitorType getType() {
        return type;
    }
}
