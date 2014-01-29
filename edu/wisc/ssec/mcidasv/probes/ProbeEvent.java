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

import java.util.EventObject;

import edu.wisc.ssec.mcidasv.util.Contract;

/**
 * This class captures a change to a probe and stores both the previous and
 * current (as of the event's creation) changed values.
 */
@SuppressWarnings("serial")
public class ProbeEvent<T> extends EventObject {

    /**
     * Previous value of the probe.
     */
    private final T oldValue;

    /**
     * Current value of the probe.
     */
    private final T newValue;

    /**
     * Generated when a {@link ReadoutProbe} changes either its position, 
     * color, or visibility. Currently stores either position, color, or 
     * visibility both before and after the change.
     * 
     * @param source Probe that generated this event.
     * @param oldValue Old value of the probe.
     * @param newValue New value of the probe.
     * 
     * @throws NullPointerException if any parameters are {@code null}.
     */
    public ProbeEvent(final ReadoutProbe source, final T oldValue, final T newValue) {
        super(source);

        Contract.notNull(source, "Events cannot originate from a null source object");
        Contract.notNull(oldValue, "Old value cannot be null");
        Contract.notNull(newValue, "New value cannot be null");

        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public ReadoutProbe getProbe() {
        return (ReadoutProbe)getSource();
    }

    /**
     * Returns the value of the probe before this event was generated.
     * 
     * @return Previous probe value.
     */
    public T getOldValue() {
        return oldValue;
    }

    /**
     * Returns the current (as of this event) value of the probe.
     * 
     * @return Current probe value.
     */
    public T getNewValue() {
        return newValue;
    }

    /**
     * Returns a brief summary of this event. Please note that this format is
     * subject to change.
     * 
     * @return String that looks like {@code [ProbeEvent@HASHCODE: source=..., 
     * oldValue=..., newValue=...]}.
     */
    @Override public String toString() {
        return String.format("[ProbeEvent@%x: source=%s, oldValue=%s, newValue=%s]", hashCode(), source, oldValue, newValue);
    }
}
