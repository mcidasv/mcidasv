package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.AddeAccount;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryStatus;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryValidity;
import edu.wisc.ssec.mcidasv.util.Contract;

public class EntryStore {

    // TODO(jon): xml resource
    public static final String ADDESERVERS = "/Users/jbeavers/.mcidasv/addeservers.xml";

    // TODO(jon): xml resource
    public static final String PERSISTEDSERVERS = "/Users/jbeavers/.mcidasv/persistedservers.xml";

    // TODO(jon): xml resource
    public static final String PREFERENCES = "/Users/jbeavers/.mcidasv/main.xml";

    /** The set of ADDE servers known to McIDAS-V. */
    private final Set<RemoteAddeEntry> entries = newLinkedHashSet();

    private final McIDASV mcv;

    public EntryStore(final McIDASV mcv) {
        Contract.notNull(mcv);
        this.mcv = mcv;

        entries.addAll(getStupidEntries());
    }

    /**
     * Returns the {@link Set} of {@link RemoteAddeEntry}s that are known to work (for
     * a given {@link EntryType} of entries).
     * 
     * @param type The {@code EntryType} you are interested in.
     * 
     * @return A {@code Set} of matching {@code RemoteAddeEntry}s. If there 
     * were no matches, an empty {@code Set} is returned.
     */
    public Set<RemoteAddeEntry> getVerifiedEntries(EntryType type) {
        Set<RemoteAddeEntry> verified = newLinkedHashSet();
        for (RemoteAddeEntry entry : entries) {
            if (entry.getEntryValidity() == EntryValidity.VERIFIED && entry.getEntryType() == type)
                verified.add(entry);
        }
        return verified;
    }

    // TODO(jon): better name
    public Map<EntryType, Set<RemoteAddeEntry>> getVerifiedEntriesByTypes() {
        Map<EntryType, Set<RemoteAddeEntry>> entryMap = new LinkedHashMap<EntryType, Set<RemoteAddeEntry>>();
        for (EntryType type : EntryType.values()) {
            entryMap.put(type, new LinkedHashSet<RemoteAddeEntry>());
        }

        for (RemoteAddeEntry entry : entries) {
            if (entry.getEntryValidity() == EntryValidity.VERIFIED) {
                Set<RemoteAddeEntry> entrySet = entryMap.get(entry.getEntryType());
                entrySet.add(entry);
            }
        }
        return entryMap;
    }

    /** 
     * Returns the {@link Set} of {@link RemoteAddeEntry#group}s that match
     * the given {@code address} and {@code type}.
     * 
     * @param address
     * @param type
     * 
     * @return Either a set containing the desired groups, or an empty set if
     * there were no matches.
     */
    public Set<String> getGroupsFor(final String address, EntryType type) {
        Set<String> groups = newLinkedHashSet();
        for (RemoteAddeEntry entry : entries) {
            if (entry.getAddress().equals(address) && entry.getEntryType() == type)
                groups.add(entry.getGroup());
        }
        return groups;
    }

    /**
     * Searches the set of servers in an attempt to locate the accounting 
     * information for the matching server. <b>Note</b> that because the data
     * structure is a {@link Set}, there <i>cannot</i> be duplicate entries,
     * so there is no need to worry about our criteria finding multiple 
     * matches.
     * 
     * <p>Also note that none of the given parameters accept {@code null} 
     * values.
     * 
     * @param address Address of the server.
     * @param group Dataset.
     * @param type Group type.
     * 
     * @return Either the {@link AddeAccount} for the given criteria, or 
     * {@link RemoteAddeEntry#DEFAULT_ACCOUNT} if there was no match.
     * 
     * @see RemoteAddeEntry#equals(Object)
     */
    public AddeAccount getAccountingFor(final String address, final String group, EntryType type) {
        for (RemoteAddeEntry e : entries) {
            if (e.getAddress().equals(address) && e.getGroup().equals(group) && e.getEntryType() == type)
                return e.getAccount();
        }
        return RemoteAddeEntry.DEFAULT_ACCOUNT;
    }

    /**
     * Returns the complete {@link Set} of {@link RemoteAddeEntry}s.
     * 
     * @return {@link #entries}.
     */
    protected Set<RemoteAddeEntry> getEntrySet() {
        return entries;
    }

    // used for apply/ok?
    public void replaceEntries(final EntryStatus typeToReplace, final Set<RemoteAddeEntry> newEntries) {

    }

    // build an entry without accounting
    private static RemoteAddeEntry _me(String h, String g, EntryType t, EntryValidity v) {
        return new RemoteAddeEntry.Builder(h, g).type(t).validity(v).build();
    }

    // build an entry with accounting
    private static RemoteAddeEntry _meacc(String h, String g, EntryType t, String u, String p, EntryValidity v) {
        return new RemoteAddeEntry.Builder(h, g).type(t).account(u, p).validity(v).build();
    }

    /**
     * @return A set of dummy servers.
     */
    private static Set<RemoteAddeEntry> getStupidEntries() {
        Set<RemoteAddeEntry> entries = newLinkedHashSet();
        entries.add(_me("adde.unverified.com", "ONE", EntryType.IMAGE, EntryValidity.UNVERIFIED));
        entries.add(_me("adde.unverified.com", "TWO", EntryType.POINT, EntryValidity.UNVERIFIED));
        entries.add(_me("adde.unverified.com", "THREE", EntryType.GRID, EntryValidity.UNVERIFIED));
        entries.add(_me("adde.unverified.com", "FOUR", EntryType.TEXT, EntryValidity.UNVERIFIED));
        entries.add(_me("adde.unverified.com", "FIVE", EntryType.NAV, EntryValidity.UNVERIFIED));
        entries.add(_me("adde.unverified.com", "SIX", EntryType.RADAR, EntryValidity.UNVERIFIED));
        entries.add(_me("adde.unverified.com", "SEVEN", EntryType.UNKNOWN, EntryValidity.UNVERIFIED));
        entries.add(_meacc("adde.unverified.com", "EIGHT", EntryType.IMAGE, "jon", "31337", EntryValidity.UNVERIFIED));
        entries.add(_meacc("adde.unverified.com", "NINE", EntryType.IMAGE, "jon", "31337", EntryValidity.UNVERIFIED));
        entries.add(_me("adde.ReallyVerified.com", "ONE", EntryType.IMAGE, EntryValidity.VERIFIED));
        entries.add(_me("adde.ReallyVerified.com", "TWO", EntryType.POINT, EntryValidity.VERIFIED));
        entries.add(_me("adde.ReallyVerified.com", "THREE", EntryType.GRID, EntryValidity.VERIFIED));
        entries.add(_me("adde.ReallyVerified.com", "FOUR", EntryType.TEXT, EntryValidity.VERIFIED));
        entries.add(_me("adde.ReallyVerified.com", "FIVE", EntryType.NAV, EntryValidity.VERIFIED));
        entries.add(_me("adde.ReallyVerified.com", "SIX", EntryType.RADAR, EntryValidity.VERIFIED));
        entries.add(_me("adde.ReallyVerified.com", "SEVEN", EntryType.UNKNOWN, EntryValidity.VERIFIED));
        entries.add(_meacc("adde.ReallyVerified.com", "EIGHT", EntryType.IMAGE, "woot", "10", EntryValidity.VERIFIED));
        entries.add(_meacc("adde.ReallyVerified.com", "NINE", EntryType.IMAGE, "woot", "10", EntryValidity.VERIFIED));
        entries.add(_me("adde.DELETED.net", "ONE", EntryType.IMAGE, EntryValidity.UNVERIFIED));
        entries.add(_me("adde.DELETED.net", "TWO", EntryType.POINT, EntryValidity.UNVERIFIED));
        entries.add(_me("adde.DELETED.net", "THREE", EntryType.GRID, EntryValidity.UNVERIFIED));
        entries.add(_me("adde.DELETED.net", "FOUR", EntryType.TEXT, EntryValidity.UNVERIFIED));
        entries.add(_me("adde.DELETED.net", "FIVE", EntryType.NAV, EntryValidity.UNVERIFIED));
        entries.add(_me("adde.DELETED.net", "SIX", EntryType.RADAR, EntryValidity.UNVERIFIED));
        entries.add(_me("adde.DELETED.net", "SEVEN", EntryType.UNKNOWN, EntryValidity.UNVERIFIED));
        entries.add(_meacc("adde.DELETED.net", "EIGHT", EntryType.IMAGE, "nil", "2000", EntryValidity.UNVERIFIED));
        entries.add(_meacc("adde.DELETED.net", "NINE", EntryType.IMAGE, "bog", "1000", EntryValidity.UNVERIFIED));
        entries.add(_meacc("adde.unverified.com", "NINE", EntryType.IMAGE, "jon", "31337", EntryValidity.UNVERIFIED));
        return entries;
    }
}
