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

package edu.wisc.ssec.mcidasv.servermanager;


/**
 * Represents a source of ADDE data. An ADDE entry may describe a dataset on
 * remote servers or the user's own machine.
 */
public interface AddeEntry {

    /** Represents the possible actions that an ADDE editor can perform. */
//    @PersistableEnum
    enum EditorAction {
        /** Created a new entry; hasn't been verified. */
        ADDED,

        /** Canceled out of creating or editing an entry. */
        CANCELLED,

        /** Verified the contents of the editor GUI. */
        VERIFIED,

        /** Created a new, verified entry. */
        ADDED_VERIFIED,

        /** Updated an existing entry without verifying changes. */
        EDITED,

        /** Updated an entry and verified the changes. */
        EDITED_VERIFIED,

        /** Editor GUI performed some {@literal "invalid"} action. */
        INVALID;
    };

    /** Type of chooser this should appear under. */
    enum EntryType {
        /** {@link edu.wisc.ssec.mcidasv.chooser.adde.AddeImageChooser} */
        IMAGE,

        /** {@link edu.wisc.ssec.mcidasv.chooser.adde.AddePointDataChooser} */
        POINT,

        /** */
        GRID,

        /** {@link edu.wisc.ssec.mcidasv.chooser.adde.AddeFrontChooser} */
        TEXT,

        /** */
        NAV,

        /** {@link edu.wisc.ssec.mcidasv.chooser.adde.AddeRadarChooser} */
        RADAR,

        /** */
        UNKNOWN,

        /** */
        INVALID;

        /**
         * Attempts to convert a given {@code String} value into a valid 
         * {@code EntryType}.
         * 
         * @param str {@code String} to convert. Should not be {@code null}.
         * 
         * @return {@code EntryType} of the value specified in {@code str}.
         */
        public static EntryType fromStr(final String str) {
            return EntryType.valueOf(str);
        }

        /**
         * Attempts to convert a given {@code EntryType} into its 
         * {@code String} representation.
         * 
         * @param type {@code EntryType} constant; should not be {@code null}.
         * 
         * @return {@code String} representation of {@code type}.
         */
        public static String toStr(final EntryType type) {
            return type.name();
        }
    };
    
    /** Sort of a {@literal "misc"} status field... */
    enum EntryValidity {
        /** Entry has been verified by connecting to the server. */
        VERIFIED,

        /** Unknown whether or not this entry actually works. */
        UNVERIFIED,

        /** Entry is being checked for validity. */
        VALIDATING,

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

        /** Entry is invalid in some way. */
        INVALID;

        public static EntryValidity fromStr(final String str) {
            return EntryValidity.valueOf(str);
        }
        public static String toStr(final EntryValidity validity) {
            return validity.name();
        }
    };

    /** Where did this entry come from? */
    enum EntrySource {
        /** Entry originated from McIDAS-V. */
        SYSTEM, 

        /** Entry was imported from a MCTABLE file. */
        MCTABLE, 

        /** Entry was added by the user.*/
        USER,

        /**
         * Represents an {@literal "invalid"} {@code EntrySource}. Useful for
         * invalid entry objects ({@link RemoteAddeEntry#INVALID_ENTRY} and 
         * {@link LocalAddeEntry#INVALID_ENTRY}).
         */
        INVALID;
        
        public static EntrySource fromStr(final String str) {
            return EntrySource.valueOf(str);
        }
        public static String toStr(final EntrySource source) {
            return source.name();
        }
    };

    /** 
     * Has the user elected to disable this entry from appearing in its 
     * relevant chooser? 
     */
    enum EntryStatus {
        /** Entry is valid and toggled on. */
        ENABLED,

        /** Entry is valid and toggled off. */
        DISABLED,

        /** Something is wrong with this entry. */
        INVALID;

        public static EntryStatus fromStr(final String str) {
            return EntryStatus.valueOf(str);
        }

        public static String toStr(final EntryType type) {
            return type.name();
        }
    };

    /** Represents the {@literal "no accounting"} entries. */
    AddeAccount DEFAULT_ACCOUNT = new AddeAccount("idv", "0");

    /**
     * Address of the server associated with the current entry. 
     * {@link LocalAddeEntry}s will return {@code localhost}.
     */
    String getAddress();

    /**
     * Dataset/group located on the server.
     */
    String getGroup();

    // TODO(jon): what part of a resolv.srv does this represent?
    /**
     * Name associated with this entry. 
     * 
     * @return Name associated with this entry.
     */
    String getName();

    /**
     * Accounting information associated with the current entry. If the server
     * does not require accounting information, this method returns 
     * {@link #DEFAULT_ACCOUNT}.
     * 
     * @return ADDE account object.
     */
    AddeAccount getAccount();

    /**
     * Type of chooser this entry should appear under.
     * 
     * @return The {@literal "type"} of data associated with this entry.
     */
    EntryType getEntryType();

    /**
     * Does this entry represent a {@literal "valid"} ADDE server.
     * 
     * @return Whether or not this entry has been validated.
     */
    EntryValidity getEntryValidity();

    /**
     * Source that specified this entry. For example; allows you to 
     * distinguish {@literal "system"} entries (which cannot be removed, only 
     * disabled) from entries created by the user (full control).
     */
    EntrySource getEntrySource();

    /**
     * GUI status of the entry. Differs from {@link EntryValidity} in that 
     * {@code EntryStatus} controls this entry showing up in a chooser and has
     * nothing to do with whether or not the entry is a valid ADDE server.
     */
    EntryStatus getEntryStatus();

    /**
     * Handy {@code String} representation of this ADDE entry. Currently looks
     * like {@code ADDRESS/GROUP}, but this is subject to change.
     */
    String getEntryText();

    // TODO(jon): should this be removed? this makes the entries mutable!
    void setEntryStatus(final EntryStatus newStatus);

    // TODO(jon): integrate with parameter sets one fine day?
    String getEntryAlias();

    // TODO(jon): should this be removed? this makes the entries mutable!
    void setEntryAlias(final String newAlias);

    /** */
    boolean isEntryTemporary();

    /**
     * Currently used as a identifier for convenient storage by the server 
     * manager.
     */
    String asStringId();

    /**
     * String representation of this entry.
     */
    String toString();
}
