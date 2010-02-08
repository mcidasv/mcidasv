/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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
package edu.wisc.ssec.mcidasv.servermanager;

import java.util.EventObject;

import edu.wisc.ssec.mcidasv.util.Contract;

public class McservEvent extends EventObject {

    public enum McservStatus { STARTED, STOPPED, DIED, RESTARTED, NOT_STARTED, NO_STATUS };

    public enum EventLevel { ERROR, NORMAL, DEBUG };

    private final McservStatus status;

    private final EventLevel level;

    private final String msg;

    public McservEvent(final EntryStore source, final McservStatus status, final EventLevel level, final String msg) {
        super(source);

        Contract.notNull(source, "McservEvents cannot originate from a null source object");
        Contract.notNull(status, "McservEvents must have a McservStatus");
        Contract.notNull(level, "McservEvents must have an EventLevel");
        Contract.notNull(msg, "McservEvents must have a message");

        this.status = status;
        this.level = level;
        this.msg = msg;
    }

    public McservStatus getStatus() {
        return status;
    }

    public EventLevel getLevel() {
        return level;
    }

    public String getMsg() {
        return msg;
    }

    @Override public String toString() {
        return String.format("[McservEvent@%x: status=%s, level=%s, msg=%s]", hashCode(), status, level, msg);
    }
}
