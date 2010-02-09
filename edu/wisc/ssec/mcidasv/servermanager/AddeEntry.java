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

public interface AddeEntry {

    /** Represents the {@literal "no accounting"} entries. */
    public static final AddeAccount DEFAULT_ACCOUNT = new AddeAccount("idv", "0");

    /** Type of chooser this should appear under. */
    public enum EntryType { IMAGE, POINT, GRID, TEXT, NAV, RADAR, UNKNOWN, INVALID }

    /** Sort of a {@literal "misc"} status field... */
    public enum EntryValidity { 
        /** Entry has been verified by connecting to the server. */
        VERIFIED, 

        /** Unknown whether or not this entry actually works. */
        UNVERIFIED, 

        /** 
         * User has elected to remove this entry. This is an unfortunate 
         * {@literal "special case"}, as we can't simply remove these entries
         * from a list! Say the user import entries from a remote MCTABLE file
         * and later deleted some of the imported entries. Fine, good! But 
         * what should happen if the user hears that new servers have been
         * added to that same MCTABLE file? The entries that the user has 
         * deleted <i>locally</i> should not reappear, right? 
         */
        DELETED,

        INVALID
    };

    /** Where did this entry come from? */
    public enum EntrySource { 
        /** Entry originated from McIDAS-V. */
        SYSTEM, 

        /** Entry was imported from a MCTABLE file. */
        MCTABLE, 

        /** Entry was added by the user.*/
        USER,

        /**
         * Represents an {@literal "invalid"} {@code EntrySource}. Useful for
         * {@link #INVALID_ENTRY}.
         */
        INVALID
    };

    /** 
     * Has the user elected to disable this entry from appearing in its 
     * relevant chooser? 
     */
    public enum EntryStatus { ENABLED, DISABLED, INVALID };

    public String getAddress();
    public String getGroup();
    public AddeAccount getAccount();
    public EntryType getEntryType();
    public EntryValidity getEntryValidity();
    public EntrySource getEntrySource();
    public EntryStatus getEntryStatus();
    public String getEntryText();
    public String getEntryAlias(); // TODO(jon): integrate with parameter sets one fine day?

    public void setEntryStatus(final EntryStatus newStatus);
    public void setEntryAlias(final String newAlias);
}
