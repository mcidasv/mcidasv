package edu.wisc.ssec.mcidasv.servermanager;

import edu.wisc.ssec.mcidasv.util.Contract;

/**
 * persistedservers.xml:
 * <entry deleted="false" description="weather3.admin.niu.edu" enabled="false" name="weather3.admin.niu.edu/RTWXTEXT" proj="" source="default" type="text" user=""/>
 * 
 * addeservers.xml:
 * <server active="true" description="test.jon.com" name="test.jon.com">
 *   <group active="true" description="TEST" names="TEST" type="any"/>
 *   <group active="true" description="WOO" names="WOO" type="oogabooga"/>
 * </server>
 * 
 * the prefs format will look very similar to persistedservers.xml...
 */
public class RemoteAddeEntry {

    public static final AddeAccount DEFAULT_ACCOUNT = new AddeAccount("idv", "0");

    public enum EntryType { IMAGE, POINT, GRID, TEXT, NAV, RADAR, UNKNOWN }

    public enum EntryValidity { VERIFIED, UNVERIFIED, DELETED };

    public enum EntrySource { SYSTEM, MCTABLE, USER };

    public enum EntryStatus { ENABLED, DISABLED };

    private final AddeAccount account;

    private final String address;

    private final String group;

    private final String description;

    private EntryType entryType;

    private EntryValidity entryValidity;

    private EntrySource entrySource;

    private EntryStatus entryStatus;

    private volatile int hashCode = 0;

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

    public String getAddress() {
        return address;
    }

    public String getGroup() {
        return group;
    }

    public AddeAccount getAccount() {
        return account;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public EntryValidity getEntryValidity() {
        return entryValidity;
    }

    public EntrySource getEntrySource() {
        return entrySource;
    }

    public EntryStatus getEntryStatus() {
        return entryStatus;
    }

    public void setEntryStatus(EntryStatus newStatus) {
        entryStatus = newStatus;
    }

    public String getEntryText() {
        return address+"/"+group;
    }

    public boolean equals(Object o) {
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

    public static class AddeAccount {
        private final String username;
        private final String project;

        public AddeAccount(final String user, final String proj) {
            username = user;
            project = proj;
        }

        public String getUsername() {
            return username;
        }

        public String getProject() {
            return project;
        }

        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof AddeAccount))
                return false;
            
            AddeAccount a = (AddeAccount)o;
            return a.getUsername().equals(username) && a.getProject().equals(project);
        }

        public int hashCode() {
            int result = 17;
            result = 31 * result + username.hashCode();
            result = 31 * result + project.hashCode();
            return result;
        }

        public String toString() {
            return String.format("[AddeAccount@%x: username=%s, project=%s]", hashCode(), username, project);
        }
    }

    public static class Builder {
        private final String address;
        private final String group;

        private EntryType entryType = EntryType.UNKNOWN;
        private EntryValidity entryValidity = EntryValidity.UNVERIFIED;
        private EntrySource entrySource = EntrySource.SYSTEM;
        private EntryStatus entryStatus = EntryStatus.ENABLED;
        private AddeAccount account = RemoteAddeEntry.DEFAULT_ACCOUNT;

        private String description = "";

        public Builder(String address, String group) {
            this.address = address;
            this.group = group;
        }

        public Builder account(String username, String project) {
            account = new AddeAccount(username, project);
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder type(EntryType entryType) {
            this.entryType = entryType;
            return this;
        }

        public Builder validity(EntryValidity entryValidity) {
            this.entryValidity = entryValidity;
            return this;
        }

        public Builder source(EntrySource entrySource) {
            this.entrySource = entrySource;
            return this;
        }

        public Builder status(EntryStatus entryStatus) {
            this.entryStatus = entryStatus;
            return this;
        }

        public RemoteAddeEntry build() {
            return new RemoteAddeEntry(this);
        }
    }
}
