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
