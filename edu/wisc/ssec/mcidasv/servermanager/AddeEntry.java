package edu.wisc.ssec.mcidasv.servermanager;

public interface AddeEntry {

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

    public void setEntryStatus(final EntryStatus newStatus);
}
