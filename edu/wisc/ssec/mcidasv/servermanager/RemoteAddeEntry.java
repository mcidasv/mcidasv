package edu.wisc.ssec.mcidasv.servermanager;

public class RemoteAddeEntry {

    /** Represents the {@literal "no accounting"} entries. */
    public static final AddeAccount DEFAULT_ACCOUNT = new AddeAccount("idv", "0");

    /** Type of chooser this should appear under. */
    public enum EntryType { IMAGE, POINT, GRID, TEXT, NAV, RADAR, UNKNOWN }

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
        DELETED 
    };

    /** Where did this entry come from? */
    public enum EntrySource { 
        /** Entry originated from McIDAS-V. */
        SYSTEM, 

        /** Entry was imported from a MCTABLE file. */
        MCTABLE, 

        /** Entry was added by the user.*/
        USER 
    };

    /** 
     * Has the user elected to disable this entry from appearing in its 
     * relevant chooser? 
     */
    public enum EntryStatus { ENABLED, DISABLED };

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
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o == this)
            return true;
        if (!(o instanceof RemoteAddeEntry))
            return false;

        RemoteAddeEntry e = (RemoteAddeEntry)o;

        return e.getAddress().equals(address) &&
            e.getGroup().equals(group) &&
            e.getEntryType() == entryType &&
            e.getAccount().equals(account);
    }

    /**
     * Returns a hash code for this ADDE entry. The hash code is computed 
     * using the values of the following fields: 
     * {@link #address}, {@link #group}, {@link #entryType}, {@link #account}.
     * 
     * @return Hash code value for this object.
     */
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = 17;
            result = 31 * result + address.hashCode();
            result = 31 * result + group.hashCode();
            result = 31 * result + entryType.hashCode();
            result = 31 * result + account.hashCode();
            hashCode = result;
        }
        return result;
    }

    public String toString() {
        return String.format("[RemoteAddeEntry@%x: address=%s, group=%s, entryType=%s, account=%s, description=%s]", hashCode(), address, group, entryType, account, description);
    }

    /**
     * Simplistic representation of ADDE accounting information. This is an
     * immutable class.
     */
    public static class AddeAccount {
        /** Username to hand off to the server. */
        private final String username;

        /** Project number (currently not limited to a numeric value). */
        private final String project;

        /** 
         * Builds a new ADDE account object.
         * 
         * @param user Username to store. Cannot be {@code null}.
         * @param proj Project number to store. Cannot be {@code null}.
         * 
         * @throws NullPointerException if {@code user} or {@code proj} is
         * {@code null}.
         */
        public AddeAccount(final String user, final String proj) {
            if (user == null)
                throw new NullPointerException();
            if (proj == null)
                throw new NullPointerException();

            username = user;
            project = proj;
        }

        /**
         * Get the username associated with this account.
         * 
         * @return {@link #username}
         */
        public String getUsername() {
            return username;
        }

        /**
         * Get the project number associated with this account.
         * 
         * @return {@link #project}
         */
        public String getProject() {
            return project;
        }

        /**
         * Determines whether or not a given object is equivalent to this ADDE
         * account. Currently the username and project number <b>are</b> case
         * sensitive, though this is likely to change.
         * 
         * @param o Object to test against.
         * 
         * @return Whether or not {@code o} is equivalent to this ADDE account.
         * 
         * @see {@link String#equals(Object)}.
         */
        @Override public boolean equals(Object o) {
            if (o == null)
                return false;
            if (o == this)
                return true;
            if (!(o instanceof AddeAccount))
                return false;

            AddeAccount a = (AddeAccount)o;
            return a.getUsername().equals(username) && a.getProject().equals(project);
        }

        /**
         * Computes the hashcode of this ADDE account using the hashcodes of 
         * {@link #username} and {@link #project}.
         * 
         * @return A hash code value for this object.
         * 
         * @see {@link String#hashCode()}.
         */
        @Override public int hashCode() {
            int result = 17;
            result = 31 * result + username.hashCode();
            result = 31 * result + project.hashCode();
            return result;
        }

        /**
         * Returns a string representation of this account. The formatting of
         * this string is subject to change, but currently looks like:<br/>
         * <pre>[AddeAccount@HASHCODE: username=..., project=...]</pre>
         * 
         * @return {@link String} representation of this ADDE account.
         */
        public String toString() {
            return String.format("[AddeAccount@%x: username=%s, project=%s]", hashCode(), username, project);
        }
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

        /** 
         * Creates an entry based upon the values supplied to the other 
         * methods. 
         */
        public RemoteAddeEntry build() {
            return new RemoteAddeEntry(this);
        }
    }
}
