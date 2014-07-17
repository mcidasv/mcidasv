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
    void probePositionChanged(final ProbeEvent<RealTuple> event);

    /**
     * Invoked when a probe's color has changed.
     * 
     * @param event Describes the probe that changed, its old color, and its
     * new color.
     */
    void probeColorChanged(final ProbeEvent<Color> event);

    /**
     * Invoked when a probe's visibility has changed.
     * 
     * @param event Describes the probe that changed, its old visibility, and
     * the new visibility. The previous and current values will always be the
     * opposites of each other.
     */
    void probeVisibilityChanged(final ProbeEvent<Boolean> event);
}
