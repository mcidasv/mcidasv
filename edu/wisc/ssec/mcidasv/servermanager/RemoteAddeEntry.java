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

public class RemoteAddeEntry implements AddeEntry {

    public static final RemoteAddeEntry INVALID_ENTRY = new Builder("localhost", "BIGBAD").invalidate().build();

    /** Represents the {@literal "no accounting"} entries. */
    public static final AddeAccount DEFAULT_ACCOUNT = new AddeAccount("idv", "0");

    /** Holds the accounting information for this entry. */
    private final AddeAccount account;

    /** The server {@literal "address"} of this entry. */
    private final String address;

    /** The {@literal "dataset"} of this entry. */
    private final String group;

    /** Err... */
    // TODO(jon): wait, what is this?
    private final String description;

    /** This entry's type. */
    private EntryType entryType;

    /** Whether or not this entry is valid. */
    private EntryValidity entryValidity;

    /** Where this entry came from. */
    private EntrySource entrySource;

    /** Whether or not this entry is in the {@literal "active set"}. */
    private EntryStatus entryStatus;

    /** Allows the user to refer to this entry with an arbitrary name. */
    private String entryAlias = "";

    /** 
     * Used so that the hashCode of this entry is not needlessly 
     * recalculated.
     * 
     * @see #hashCode()
     */
    private volatile int hashCode = 0;

    /**
     * Creates a new ADDE entry using a give {@literal "ADDE entry builder"}.
     * 
     * @param builder Object used to build this entry.
     */
    private RemoteAddeEntry(Builder builder) {
        this.account = builder.account;
        this.address = builder.address;
        this.group = builder.group;
        this.description = builder.description;
        this.entryType = builder.entryType;
        this.entryValidity = builder.entryValidity;
        this.entrySource = builder.entrySource;
        this.entryStatus = builder.entryStatus;
    }

    /**
     * @return {@link #address}
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return {@link #group}
     */
    public String getGroup() {
        return group;
    }

    /**
     * @return {@link #account}
     */
    public AddeAccount getAccount() {
        return account;
    }

    /**
     * @return {@link #entryType}
     */
    public EntryType getEntryType() {
        return entryType;
    }

    /**
     * @return {@link #entryValidity}
     */
    public EntryValidity getEntryValidity() {
        return entryValidity;
    }

    /**
     * @return {@link #entrySource}
     */
    public EntrySource getEntrySource() {
        return entrySource;
    }

    /**
     * @return {@link #entryStatus}
     */
    public EntryStatus getEntryStatus() {
        return entryStatus;
    }

    public void setEntryStatus(EntryStatus newStatus) {
        entryStatus = newStatus;
    }

    public String getEntryAlias() {
        return entryAlias;
    }

    public void setEntryAlias(final String newAlias) {
        if (newAlias == null)
            throw new NullPointerException("Null aliases are not allowable.");
        entryAlias = newAlias;
    }

    /**
     * Handy {@code String} representation of this ADDE entry. Currently looks
     * like {@code ADDRESS/GROUP}, but this is subject to change.
     * 
     * @return Alternate {@code String} representation of this entry.
     */
    public String getEntryText() {
        return address+"/"+group;
    }

    /**
     * Determines whether or not the given object is equivalent to this ADDE 
     * entry.
     * 
     * @param o Object to test against. {@code null} values are okay, but 
     * return {@code false}.
     * 
     * @return {@code true} if the given object is the same as this ADDE 
     * entry, {@code false} otherwise... including when {@code o} is 
     * {@code null}.
     */
    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RemoteAddeEntry)) {
            return false;
        }
        RemoteAddeEntry other = (RemoteAddeEntry) obj;
        if (account == null) {
            if (other.account != null) {
                return false;
            }
        } else if (!account.equals(other.account)) {
            return false;
        }
        if (address == null) {
            if (other.address != null) {
                return false;
            }
        } else if (!address.equals(other.address)) {
            return false;
        }
        if (entryType == null) {
            if (other.entryType != null) {
                return false;
            }
        } else if (!entryType.equals(other.entryType)) {
            return false;
        }
        if (group == null) {
            if (other.group != null) {
                return false;
            }
        } else if (!group.equals(other.group)) {
            return false;
        }
        return true;
    }

    /**
     * Returns a hash code for this ADDE entry. The hash code is computed 
     * using the values of the following fields: 
     * {@link #address}, {@link #group}, {@link #entryType}, {@link #account}.
     * 
     * @return Hash code value for this object.
     */
    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((account == null) ? 0 : account.hashCode());
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + ((entryType == null) ? 0 : entryType.hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        return result;
    }

    public String toString() {
        return String.format("[RemoteAddeEntry@%x: address=%s, group=%s, entryType=%s, account=%s, description=%s]", hashCode(), address, group, entryType, account, description);
    }

    /**
     * Something of a hack... this approach allows us to build a 
     * {@code RemoteAddeEntry} in a <b>readable</b> way, despite there being
     * multiple {@code final} fields. 
     * 
     * <p>The only <i>required</i> parameters are
     * the {@link RemoteAddeEntry#address} and {@link RemoteAddeEntry#group}.
     * 
     * <p>Some examples:<br/>
     * <pre>
     * RemoteAddeEntry e = RemoteAddeEntry.Builder("adde.cool.com", "RTIMAGES").build();
     * e = RemoteAddeEntry.Builder("adde.cool.com", "RTIMAGES").type(EntryType.IMAGE).account("user", "1337").build();
     * e = RemoteAddeEntry.Builder("adde.cool.com", "RTIMAGES").account("user", "1337").type(EntryType.IMAGE).build()
     * e = RemoteAddeEntry.Builder("a.c.com", "RTIMGS").validity(EntryValidity.VERIFIED).build();
     * </pre>
     * 
     */
    public static class Builder {
        private final String address;
        private final String group;

        /** 
         * Optional {@link EntryType} of the entry. Defaults to 
         * {@link EntryType#UNKNOWN}. 
         */
        private EntryType entryType = EntryType.UNKNOWN;

        /** Optional {@link EntryValidity} of the entry. Defaults to 
         * {@link EntryValidity#UNVERIFIED}. 
         */
        private EntryValidity entryValidity = EntryValidity.UNVERIFIED;

        /** 
         * Optional {@link EntrySource} of the entry. Defaults to 
         * {@link EntrySource#SYSTEM}. 
         */
        private EntrySource entrySource = EntrySource.SYSTEM;

        /** 
         * Optional {@link EntryStatus} of the entry. Defaults to 
         * {@link EntryStatus#ENABLED}. 
         */
        private EntryStatus entryStatus = EntryStatus.ENABLED;

        /** 
         * Optional {@link AddeAccount} of the entry. Defaults to 
         * {@link RemoteAddeEntry#DEFAULT_ACCOUNT}. 
         */
        private AddeAccount account = RemoteAddeEntry.DEFAULT_ACCOUNT;

        /** Optional description of the entry. Defaults to {@literal ""}. */
        private String description = "";

        /**
         * Creates a new {@literal "builder"} for an ADDE entry. Note that
         * the two parameters to this constructor are the only <i>required</i>
         * parameters to create an ADDE entry.
         * 
         * @param address Address of the ADDE entry. Cannot be null.
         * @param group Group of the ADDE entry. Cannot be null.
         * 
         * @throws NullPointerException if either {@code address} or 
         * {@code group} is {@code null}.
         */
        public Builder(final String address, final String group) {
            if (address == null)
                throw new NullPointerException("ADDE address cannot be null");
            if (group == null)
                throw new NullPointerException("ADDE group cannot be null");

            this.address = address.toLowerCase();
            this.group = group;
        }

        /** 
         * Optional {@literal "parameter"} for an ADDE entry. Allows you to
         * specify the accounting information. If this method is not called,
         * the resulting ADDE entry will be built with 
         * {@link RemoteAddeEntry#DEFAULT_ACCOUNT}.
         * 
         * @param username Username of the ADDE account. Cannot be 
         * {@code null}.
         * @param project Project number for the ADDE account. Cannot be 
         * {@code null}.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         * 
         * @see AddeAccount#AddeAccount(String, String)
         */
        public Builder account(final String username, final String project) {
            account = new AddeAccount(username, project);
            return this;
        }

        /**
         * Optional {@literal "parameter"} for an ADDE entry. Allows you to
         * set {@link RemoteAddeEntry#description}. If this method is not 
         * called, {@code description} will default to {@literal ""}.
         * 
         * @param description Description field. Cannot be {@code null}.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         * 
         * @throw NullPointerException if {@code description} is {@code null}.
         */
        public Builder description(final String description) {
            if (description == null)
                throw new NullPointerException("Description cannot be null");
            this.description = description;
            return this;
        }

        /**
         * Optional {@literal "parameter"} for an ADDE entry. Allows you to
         * set the {@link RemoteAddeEntry#entryType}. If this method is not 
         * called, {@code entryType} will default to {@link EntryType#UNKNOWN}.
         * 
         * @param entryType ADDE entry {@literal "type"}.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         */
        public Builder type(EntryType entryType) {
            this.entryType = entryType;
            return this;
        }

        /**
         * Optional {@literal "parameter"} for an ADDE entry. Allows you to
         * set the {@link RemoteAddeEntry#entryValidity}. If this method is 
         * not called, {@code entryValidity} will default to 
         * {@link EntryValidity#UNVERIFIED}.
         * 
         * @param entryValidity ADDE entry {@literal "validity"}.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         */
        public Builder validity(EntryValidity entryValidity) {
            this.entryValidity = entryValidity;
            return this;
        }

        /**
         * Optional {@literal "parameter"} for an ADDE entry. Allows you to
         * set the {@link RemoteAddeEntry#entrySource}. If this method is not 
         * called, {@code entrySource} will default to 
         * {@link EntrySource#SYSTEM}.
         * 
         * @param entrySource ADDE entry {@literal "source"}.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         */
        public Builder source(EntrySource entrySource) {
            this.entrySource = entrySource;
            return this;
        }

        /**
         * Optional {@literal "parameter"} for an ADDE entry. Allows you to
         * set the {@link RemoteAddeEntry#entryStatus}. If this method is not 
         * called, {@code entryStatus} will default to 
         * {@link EntryStatus#ENABLED}.
         * 
         * @param entryType ADDE entry {@literal "status"}.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         */
        public Builder status(EntryStatus entryStatus) {
            this.entryStatus = entryStatus;
            return this;
        }

        public Builder invalidate() {
            this.entryType = EntryType.INVALID;
            this.entryValidity = EntryValidity.INVALID;
            this.entrySource = EntrySource.INVALID;
            this.entryStatus = EntryStatus.INVALID;
            return this;
        }

        /** 
         * Creates an entry based upon the values supplied to the other 
         * methods. 
         */
        public RemoteAddeEntry build() {
            return new RemoteAddeEntry(this);
        }
    }
}
