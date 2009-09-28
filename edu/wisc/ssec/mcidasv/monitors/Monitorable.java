package edu.wisc.ssec.mcidasv.monitors;

public interface Monitorable extends Runnable {
    public void addMonitor(final Monitoring listener);
}
