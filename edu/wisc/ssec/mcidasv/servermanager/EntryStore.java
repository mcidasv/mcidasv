package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IdvResourceManager.IdvResource;
import ucar.unidata.xml.XmlResourceCollection;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.ResourceManager;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.AddeAccount;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntrySource;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryStatus;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryValidity;
import edu.wisc.ssec.mcidasv.util.Contract;

public class EntryStore {

    /** The set of ADDE servers known to McIDAS-V. */
    private final Set<RemoteAddeEntry> entries = newLinkedHashSet();

    /** Object that's running the show */
    private final McIDASV mcv;

    public EntryStore(final McIDASV mcv) {
        Contract.notNull(mcv);
        this.mcv = mcv;

        entries.addAll(extractUserEntries(ResourceManager.RSC_NEW_USERSERVERS));
        entries.addAll(extractResourceEntries(EntrySource.SYSTEM, IdvResourceManager.RSC_ADDESERVER));
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

    /**
     * Process all of the {@literal "idv-style"} XML resources.
     * 
     * @param source
     * @param resource
     * 
     * @return
     */
    private Set<RemoteAddeEntry> extractResourceEntries(EntrySource source, final IdvResource resource) {
        Set<RemoteAddeEntry> entries = newLinkedHashSet();

        XmlResourceCollection xmlResource = mcv.getResourceManager().getXmlResources(resource);

        for (int i = 0; i < xmlResource.size(); i++) {
            Element root = xmlResource.getRoot(i);
            if (root == null)
                continue;

            Set<RemoteAddeEntry> woot = EntryTransforms.convertAddeServerXml(root, source);
            entries.addAll(woot);
        }

        return entries;
    }

    /**
     * Process all of the {@literal "user"} XML resources.
     * 
     * @param resource Resource collection. Cannot be {@code null}.
     * 
     * @return {@link Set} of {@link RemoteAddeEntry}s contained within 
     * {@code resource}.
     */
    private Set<RemoteAddeEntry> extractUserEntries(final IdvResource resource) {
        Set<RemoteAddeEntry> entries = newLinkedHashSet();

        XmlResourceCollection xmlResource = mcv.getResourceManager().getXmlResources(resource);
        for (int i = 0; i < xmlResource.size(); i++) {
            Element root = xmlResource.getRoot(i);
            if (root == null)
                continue;

            entries.addAll(EntryTransforms.convertUserXml(root));
            for (RemoteAddeEntry e : entries) {
                System.err.println(e);
            }
        }

        return entries;
    }
}
