package edu.wisc.ssec.mcidasv.probes;

import java.awt.Color;
import java.util.EventListener;

import visad.RealTuple;

/**
 * The listener interface for receiving {@link ReadoutProbe} events. Everything
 * is handled in the standard Java idiom: implement required methods, register
 * the implementing class using 
 * {@link ReadoutProbe#addProbeListener(ProbeListener)} and listen away!
 */
public interface ProbeListener extends EventListener {

    /**
     * Invoked when a probe's position is changed.
     * 
     * @param event Describes the probe that moved, its old position, and its
     * new position.
     */
    public void probePositionChanged(final ProbeEvent<RealTuple> event);

    /**
     * Invoked when a probe's color has changed.
     * 
     * @param event Describes the probe that changed, its old color, and its
     * new color.
     */
    public void probeColorChanged(final ProbeEvent<Color> event);

    /**
     * Invoked when a probe's visibility has changed.
     * 
     * @param event Describes the probe that changed, its old visibility, and
     * the new visibility. The previous and current values will always be the
     * opposites of each other.
     */
    public void probeVisibilityChanged(final ProbeEvent<Boolean> event);
}
