/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
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

import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryValidity;

// Note: this probably has some utility... don't delete quite yet!
public class RemoteAddeUtilities {

    // make an entry without accounting
    protected static RemoteAddeEntry _me(String h, String g, EntryType t, EntryValidity v) {
        return new RemoteAddeEntry.Builder(h, g).type(t).validity(v).build();
    }

    // make an entry with accounting
    protected static RemoteAddeEntry _meacc(String h, String g, EntryType t, String u, String p, EntryValidity v) {
        return new RemoteAddeEntry.Builder(h, g).type(t).account(u, p).validity(v).build();
    }
}
