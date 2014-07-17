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

import static edu.wisc.ssec.mcidasv.util.Contract.notNull;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashMap;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;

import org.python.modules.posix.PosixModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;

import ucar.unidata.util.LogUtil;
import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.xml.XmlResourceCollection;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.ResourceManager;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntrySource;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryStatus;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryValidity;
import edu.wisc.ssec.mcidasv.servermanager.AddeThread.McservEvent;
import edu.wisc.ssec.mcidasv.util.trie.CharSequenceKeyAnalyzer;
import edu.wisc.ssec.mcidasv.util.trie.PatriciaTrie;

public class EntryStore {

    /** 
     * Property that allows users to supply arbitrary paths to McIDAS-X 
     * binaries used by mcservl.
     * 
     * @see #getAddeRootDirectory()
     */
    private static final String PROP_DEBUG_LOCALROOT = "debug.localadde.rootdir";

    /**
     * Property that allows users to control debug output from ADDE requests.
     * 
     * @see #isAddeDebugEnabled(boolean)
     * @see #setAddeDebugEnabled(boolean)
     */
    private static final String PROP_DEBUG_ADDEURL = "debug.adde.reqs";

    /** Enumeration of the various server manager events. */
    public enum Event { 
        /** Entries were replaced. */
        REPLACEMENT, 
        /** Entries were removed. */
        REMOVAL, 
        /** Entries were added. */
        ADDITION, 
        /** Entries were updated.*/
        UPDATE, 
        /** Something failed! */
        FAILURE, 
        /** Local servers started. */
        STARTED, 
        /** Catch-all? */
        UNKNOWN 
    }

    /** Logging object. */
    private static final Logger logger = LoggerFactory.getLogger(EntryStore.class);

    private static final String PREF_ADDE_ENTRIES = "mcv.servers.entries";

    /** The ADDE servers known to McIDAS-V. */
    private final PatriciaTrie<String, AddeEntry> trie;

    /** {@literal "Root"} local server directory. */
    private final String ADDE_DIRECTORY;

    /** Path to local server binaries. */
    private final String ADDE_BIN;

    /** Path to local server data. */
    private final String ADDE_DATA;

    /** Path to mcservl. */
    private final String ADDE_MCSERVL;

    /** Path to the user's {@literal "userpath"} directory. */
    private final String USER_DIRECTORY;

    /** Path to the user's {@literal "RESOLV.SRV"}. */
    private final String ADDE_RESOLV;

    /** */
    private final String MCTRACE;

    /** Which port is this particular manager operating on */
    private static String localPort;

    /** Thread that monitors the mcservl process. */
    private static AddeThread thread;

    /** The last {@link AddeEntry}s added to the manager. */
    private final List<AddeEntry> lastAdded;

    private final IdvObjectStore idvStore;

    private boolean restartingMcserv;

    /**
     * Constructs a server manager.
     * 
     * @param store 
     * @param rscManager 
     */
    public EntryStore(final IdvObjectStore store, final IdvResourceManager rscManager) {
        notNull(store);
        notNull(rscManager);

        this.idvStore = store;
        this.trie = new PatriciaTrie<>(new CharSequenceKeyAnalyzer());
        this.ADDE_DIRECTORY = getAddeRootDirectory();
        this.ADDE_BIN = ADDE_DIRECTORY + File.separator + "bin";
        this.ADDE_DATA = ADDE_DIRECTORY + File.separator + "data";
        this.localPort = Constants.LOCAL_ADDE_PORT;
        this.restartingMcserv = false;
        this.lastAdded = arrList();
        AnnotationProcessor.process(this);

        McIDASV mcv = McIDASV.getStaticMcv();
        USER_DIRECTORY = mcv.getUserDirectory();
        ADDE_RESOLV = mcv.getUserFile("RESOLV.SRV");
        MCTRACE = "0";

        if (McIDASV.isWindows()) {
            ADDE_MCSERVL = ADDE_BIN + "\\mcservl.exe";
        } else {
            ADDE_MCSERVL = ADDE_BIN + "/mcservl";
        }

        try {
            Set<LocalAddeEntry> locals = EntryTransforms.readResolvFile(ADDE_RESOLV);
            putEntries(trie, locals);
        } catch (IOException e) {
            logger.warn("EntryStore: RESOLV.SRV missing; expected=\"" + ADDE_RESOLV + '"');
        }

        XmlResourceCollection userResource = rscManager.getXmlResources(ResourceManager.RSC_NEW_USERSERVERS);
        XmlResourceCollection sysResource = rscManager.getXmlResources(IdvResourceManager.RSC_ADDESERVER);

        Set<AddeEntry> systemEntries = extractResourceEntries(EntrySource.SYSTEM, sysResource);

        Set<AddeEntry> prefEntries = extractPreferencesEntries(store);
        prefEntries = removeDeletedSystemEntries(prefEntries, systemEntries);

        Set<AddeEntry> userEntries = extractUserEntries(userResource);
        userEntries = removeDeletedSystemEntries(userEntries, systemEntries);

        putEntries(trie, prefEntries);
        putEntries(trie, userEntries);
        putEntries(trie, systemEntries);
        saveEntries();
    }

    /**
     * Searches {@code entries} for {@link AddeEntry} objects with two characteristics:
     * <ul>
     * <li>the object source is {@link EntrySource#SYSTEM}</li>
     * <li>the object is <b>not</b> in {@code systemEntries}</li>
     * </ul>
     * 
     * <p>The intent behind this method is to safely remove {@literal "system"}
     * entries that have been stored to a user's preferences. {@code entries}
     * can be generated from anywhere you like, but {@code systemEntries} should
     * almost always be created from {@literal "addeservers.xml"}.
     * 
     * @param entries Cannot be {@code null}.
     * @param systemEntries Cannot be {@code null}.
     * 
     * @return {@code Set} of entries that are not system resources that have
     * been removed, or an empty {@code Set}.
     */
    private static Set<AddeEntry> removeDeletedSystemEntries(final Collection<? extends AddeEntry> entries, final Collection<? extends AddeEntry> systemEntries) {
        Set<AddeEntry> pruned = newLinkedHashSet(entries.size());
        for (AddeEntry entry : entries) {
            if (entry.getEntrySource() != EntrySource.SYSTEM) {
                pruned.add(entry);
            } else if (systemEntries.contains(entry)) {
                pruned.add(entry);
            } else {
                continue;
            }
        }
        return pruned;
    }

    /**
     * Adds {@link AddeEntry} objects to a given {@link PatriciaTrie}.
     * 
     * @param trie Cannot be {@code null}.
     * @param newEntries Cannot be {@code null}.
     */
    private static void putEntries(final PatriciaTrie<String, AddeEntry> trie, final Collection<? extends AddeEntry> newEntries) {
        notNull(trie);
        notNull(newEntries);
        for (AddeEntry e : newEntries) {
            trie.put(e.asStringId(), e);
        }
    }

    public IdvObjectStore getIdvStore() {
        return idvStore;
    }

    protected String[] getWindowsAddeEnv() {
        // Drive letters should come from environment
        // Java drive is not necessarily system drive
        return new String[] {
            "PATH=" + ADDE_BIN,
            "MCPATH=" + USER_DIRECTORY+':'+ADDE_DATA,
            "MCUSERDIR=" + USER_DIRECTORY,
            "MCNOPREPEND=1",
            "MCTRACE=" + MCTRACE,
            "MCTRACK=NO",
            "MCJAVAPATH=" + System.getProperty("java.home"),
            "MCBUFRJARPATH=" + ADDE_BIN,
            "SYSTEMDRIVE=" + System.getenv("SystemDrive"),
            "SYSTEMROOT=" + System.getenv("SystemRoot"),
            "HOMEDRIVE=" + System.getenv("HOMEDRIVE"),
            "HOMEPATH=\\Windows"
        };
    }

    protected String[] getUnixAddeEnv() {
        return new String[] {
            "PATH=" + ADDE_BIN,
            "MCPATH=" + USER_DIRECTORY+':'+ADDE_DATA,
            "MCUSERDIR=" + USER_DIRECTORY,
            "LD_LIBRARY_PATH=" + ADDE_BIN,
            "DYLD_LIBRARY_PATH=" + ADDE_BIN,
            "MCNOPREPEND=1",
            "MCTRACE=" + MCTRACE,
            "MCTRACK=NO",
            "MCJAVAPATH=" + System.getProperty("java.home"),
            "MCBUFRJARPATH=" + ADDE_BIN
        };
    }

    protected String[] getAddeCommands() {
        String mcvPID = Integer.toString(PosixModule.getpid());
        if (McIDASV.isWindows() || (mcvPID == null) || "0".equals(mcvPID)) {
            return new String[] { ADDE_MCSERVL, "-v", "-p", localPort };
        } else {
            return new String[] { ADDE_MCSERVL, "-v", "-p", localPort, "-i", mcvPID };
        }
    }

    /**
     * Determine the validity of a given {@link edu.wisc.ssec.mcidasv.servermanager.AddeEntry AddeEntry}.
     * 
     * @param entry Entry to check. Cannot be {@code null}.
     * 
     * @return {@code true} if {@code entry} is invalid or {@code false} otherwise.
     * 
     * @throws AssertionError if {@code entry} is somehow neither a {@code RemoteAddeEntry} or {@code LocalAddeEntry}.
     * 
     * @see edu.wisc.ssec.mcidasv.servermanager.LocalAddeEntry#INVALID_ENTRY
     * @see edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry#INVALID_ENTRY
     */
    public static boolean isInvalidEntry(final AddeEntry entry) {
        notNull(entry);
        boolean retVal = true;
        if (entry instanceof RemoteAddeEntry) {
            retVal = RemoteAddeEntry.INVALID_ENTRY.equals(entry);
        } else if (entry instanceof LocalAddeEntry) {
            retVal = LocalAddeEntry.INVALID_ENTRY.equals(entry);
        } else {
            throw new AssertionError("Unknown AddeEntry type: "+entry.getClass().getName());
        }
        return retVal;
    }

    /**
     * Returns the {@link edu.wisc.ssec.mcidasv.servermanager.AddeEntry AddeEntrys} stored 
     * in the user's preferences.
     * 
     * @param store Object store that represents the user's preferences. Cannot be {@code null}.
     * 
     * @return Either the {@code AddeEntrys} stored in the prefs or an empty {@link java.util.Set Set}.
     */
    private Set<AddeEntry> extractPreferencesEntries(final IdvObjectStore store) {
        assert store != null;

        // this is valid--the only thing ever written to 
        // PREF_REMOTE_ADDE_ENTRIES is an ArrayList of RemoteAddeEntry objects.
        @SuppressWarnings("unchecked")
        List<AddeEntry> asList = 
            (List<AddeEntry>)store.get(PREF_ADDE_ENTRIES);
        Set<AddeEntry> entries;
        if (asList == null) {
            entries = Collections.emptySet();
        } else {
            entries = newLinkedHashSet(asList.size());
            for (AddeEntry entry : asList) {
                if (entry instanceof RemoteAddeEntry) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    /**
     * Responds to server manager events being passed with the event bus. 
     * 
     * @param evt Event to which this method is responding.
     */
    @EventSubscriber(eventClass=Event.class)
    public void onEvent(Event evt) {
        notNull(evt);
        saveEntries();
    }

    /**
     * Saves the current set of ADDE servers to the user's preferences.
     */
    public void saveEntries() {
        idvStore.put(PREF_ADDE_ENTRIES, arrList(trie.values()));
        idvStore.saveIfNeeded();
        try {
            EntryTransforms.writeResolvFile(ADDE_RESOLV, getLocalEntries());
        } catch (IOException e) {
            logger.error("EntryStore: RESOLV.SRV missing; expected=\""+ADDE_RESOLV+"\"");
        }
    }

    public void saveForShutdown() {
        idvStore.put(PREF_ADDE_ENTRIES, arrList(getPersistedEntrySet()));
        idvStore.saveIfNeeded();
        try {
            EntryTransforms.writeResolvFile(ADDE_RESOLV, getPersistedLocalEntries());
        } catch (IOException e) {
            logger.error("EntryStore: RESOLV.SRV missing; expected=\""+ADDE_RESOLV+"\"");
        }
    }

    /**
     * Searches the newest entries for the entries of the given {@link edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType EntryType}.
     * 
     * @param type Look for entries matching this {@code EntryType}. Cannot be {@code null}.
     * 
     * @return Either a {@link java.util.List List} of entries or an empty {@code List}.
     *
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    public List<AddeEntry> getLastAddedByType(final EntryType type) {
        notNull(type);
        List<AddeEntry> entries = arrList(lastAdded.size());
        for (AddeEntry entry : lastAdded) {
            if (entry.getEntryType() == type) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public List<AddeEntry> getLastAddedByTypes(final EnumSet<EntryType> types) {
        notNull(types);
        List<AddeEntry> entries = arrList(lastAdded.size());
        for (AddeEntry entry : lastAdded) {
            if (types.contains(entry.getEntryType())) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public List<AddeEntry> getLastAdded() {
        return arrList(lastAdded);
    }

    /**
     * Returns the {@link Set} of {@link AddeEntry}s that are known to work (for
     * a given {@link EntryType} of entries).
     * 
     * @param type The {@code EntryType} you are interested in.
     * 
     * @return A {@code Set} of matching {@code RemoteAddeEntry}s. If there 
     * were no matches, an empty {@code Set} is returned.
     */
    public Set<AddeEntry> getVerifiedEntries(final EntryType type) {
        notNull(type);
        Set<AddeEntry> verified = newLinkedHashSet(trie.size());
        for (AddeEntry entry : trie.values()) {
            if (entry.getEntryType() != type) {
                continue;
            }

            if (entry instanceof LocalAddeEntry) {
                verified.add(entry);
            } else if (entry.getEntryValidity() == EntryValidity.VERIFIED) {
                verified.add(entry);
            }
        }
        return verified;
    }

    // TODO(jon): better name
    public Map<EntryType, Set<AddeEntry>> getVerifiedEntriesByTypes() {
        Map<EntryType, Set<AddeEntry>> entryMap =
                newLinkedHashMap(EntryType.values().length);
        int size = trie.size();
        for (EntryType type : EntryType.values()) {
            entryMap.put(type, new LinkedHashSet<AddeEntry>(size));
        }

        for (AddeEntry entry : trie.values()) {
            Set<AddeEntry> entrySet = entryMap.get(entry.getEntryType());
            entrySet.add(entry);
        }
        return entryMap;
    }

    /**
     * Returns the {@link Set} of {@link AddeEntry#getGroup()}s
     * that match the given {@code address} and {@code type}.
     * 
     * @param address ADDE server address whose groups are needed.
     * Cannot be {@code null}.
     * @param type Only include groups that match {@link EntryType}.
     * Cannot be {@code null}.
     * 
     * @return Either a set containing the desired groups, or an empty set if
     * there were no matches.
     */
    public Set<String> getGroupsFor(final String address, EntryType type) {
        notNull(address);
        notNull(type);
        Set<String> groups = newLinkedHashSet(trie.size());
        for (AddeEntry entry : trie.getPrefixedBy(address+'!').values()) {
            if (entry.getAddress().equals(address) && entry.getEntryType() == type) {
                groups.add(entry.getGroup());
            }
        }
        return groups;
    }

    /**
     * Search the server manager for entries that match {@code prefix}.
     * 
     * @param prefix {@code String} to match.
     * 
     * @return {@link List} containing matching entries. If there were no 
     * matches the {@code List} will be empty.
     * 
     * @see AddeEntry#asStringId()
     */
    public List<AddeEntry> searchWithPrefix(final String prefix) {
        notNull(prefix);
        return arrList(trie.getPrefixedBy(prefix).values());
    }

    /**
     * Returns the {@link Set} of {@link AddeEntry} addresses stored
     * in this {@code EntryStore}.
     * 
     * @return {@code Set} containing all of the stored addresses. If no 
     * addresses are stored, an empty {@code Set} is returned.
     */
    public Set<String> getAddresses() {
        Set<String> addresses = newLinkedHashSet(trie.size());
        for (AddeEntry entry : trie.values()) {
            addresses.add(entry.getAddress());
        }
        return addresses;
    }

    /**
     * Returns a {@link Set} containing <b>ADDRESS/GROUPNAME</b> {@code String}s
     * for each {@link RemoteAddeEntry}.
     * 
     * @return The {@literal "entry text"} representations of each 
     * {@code RemoteAddeEntry}.
     * 
     * @see RemoteAddeEntry#getEntryText()
     */
    public Set<String> getRemoteEntryTexts() {
        Set<String> strs = newLinkedHashSet(trie.size());
        for (AddeEntry entry : trie.values()) {
            if (entry instanceof RemoteAddeEntry) {
                strs.add(entry.getEntryText());
            }
        }
        return strs;
    }

    /**
     * Returns the {@link Set} of {@literal "groups"} associated with the 
     * given {@code address}.
     * 
     * @param address Address of a server.
     * 
     * @return Either all of the {@literal "groups"} on {@code address} or an
     * empty {@code Set}.
     */
    public Set<String> getGroups(final String address) {
        notNull(address);
        Set<String> groups = newLinkedHashSet(trie.size());
        for (AddeEntry entry : trie.getPrefixedBy(address+'!').values()) {
            groups.add(entry.getGroup());
        }
        return groups;
    }

    /**
     * Returns the {@link Set} of {@link EntryType}s for a given {@code group}
     * on a given {@code address}.
     * 
     * @param address Address of a server.
     * @param group Group whose {@literal "types"} you want.
     * 
     * @return Either of all the types for a given {@code address} and 
     * {@code group} or an empty {@code Set} if there were no matches.
     */
    public Set<EntryType> getTypes(final String address, final String group) {
        Set<EntryType> types = newLinkedHashSet(trie.size());
        for (AddeEntry entry : trie.getPrefixedBy(address+'!'+group+'!').values()) {
            types.add(entry.getEntryType());
        }
        return types;
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
     * {@link AddeEntry#DEFAULT_ACCOUNT} if there was no match.
     * 
     * @see RemoteAddeEntry#equals(Object)
     */
    public AddeAccount getAccountingFor(final String address, final String group, EntryType type) {
        Collection<AddeEntry> entries = trie.getPrefixedBy(address+'!'+group+'!'+type.name()).values();
        for (AddeEntry entry : entries) {
            if (!isInvalidEntry(entry)) {
                return entry.getAccount();
            }
        }
        return AddeEntry.DEFAULT_ACCOUNT;
    }

    public AddeAccount getAccountingFor(final AddeServer idvServer, String typeAsStr) {
        String address = idvServer.getName();
        List<AddeServer.Group> groups = (List<AddeServer.Group>)idvServer.getGroups();
        if (groups != null && !groups.isEmpty()) {
            EntryType type = EntryTransforms.strToEntryType(typeAsStr);
            return getAccountingFor(address, groups.get(0).getName(), type);
        } else {
            return RemoteAddeEntry.DEFAULT_ACCOUNT;
        }
    }

    /**
     * Returns the complete {@link Set} of {@link AddeEntry}s.
     */
    public Set<AddeEntry> getEntrySet() {
        return newLinkedHashSet(trie.values());
    }

    public Set<AddeEntry> getPersistedEntrySet() {
        Set<AddeEntry> entries = newLinkedHashSet(trie.size());
        for (AddeEntry entry : trie.values()) {
            if (!entry.isEntryTemporary()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /**
     * Returns the complete {@link Set} of {@link RemoteAddeEntry}s.
     * 
     * @return The {@code RemoteAddeEntry}s stored within the available entries.
     */
    public Set<RemoteAddeEntry> getRemoteEntries() {
        Set<RemoteAddeEntry> remotes = newLinkedHashSet(trie.size());
        for (AddeEntry e : trie.values()) {
            if (e instanceof RemoteAddeEntry) {
                remotes.add((RemoteAddeEntry)e);
            }
        }
        return remotes;
    }

    /**
     * Returns the complete {@link Set} of {@link LocalAddeEntry}s.
     * 
     * @return The {@code LocalAddeEntry}s stored within the available entries.
     */
    public Set<LocalAddeEntry> getLocalEntries() {
        Set<LocalAddeEntry> locals = newLinkedHashSet(trie.size());
        for (AddeEntry e : trie.getPrefixedBy("localhost").values()) {
            if (e instanceof LocalAddeEntry) {
                locals.add((LocalAddeEntry)e);
            }
        }
        return locals;
    }

    /**
     * Returns the {@link Set} of {@link LocalAddeEntry LocalAddeEntries} that will
     * be saved between McIDAS-V sessions. 
     * 
     * <p>Note: all this does is check {@link LocalAddeEntry#isTemporary} field. 
     * 
     * @return {@code LocalAddeEntry}s that will be saved for the next session.
     */
    public Set<LocalAddeEntry> getPersistedLocalEntries() {
//        Set<LocalAddeEntry> locals = newLinkedHashSet(trie.size());
//        for (AddeEntry e : trie.getPrefixedBy("localhost").values()) {
//            if (e instanceof LocalAddeEntry) {
//                LocalAddeEntry local = (LocalAddeEntry)e;
//                if (!local.isEntryTemporary()) {
//                    locals.add(local);
//                }
//            }
//        }
//        return locals;
        return this.filterLocalEntriesByTemporaryStatus(false);
    }

    public Set<LocalAddeEntry> getTemporaryLocalEntries() {
        return this.filterLocalEntriesByTemporaryStatus(true);
    }
    
    private Set<LocalAddeEntry> filterLocalEntriesByTemporaryStatus(final boolean getTemporaryEntries) {
        Set<LocalAddeEntry> locals = newLinkedHashSet(trie.size());
        for (AddeEntry e : trie.getPrefixedBy("localhost").values()) {
            if (e instanceof LocalAddeEntry) {
                LocalAddeEntry local = (LocalAddeEntry)e;
                if (local.isEntryTemporary() == getTemporaryEntries) {
                    locals.add(local);
                }
            }
        }
        return locals;
    }
    
    public boolean removeEntries(
        final Collection<? extends AddeEntry> removedEntries) 
    {
        notNull(removedEntries);

        boolean val = true;
        boolean tmpVal = true;
        for (AddeEntry entry : removedEntries) {
            if (entry.getEntrySource() != EntrySource.SYSTEM) {
                tmpVal = trie.remove(entry.asStringId()) != null;
                logger.trace("attempted bulk remove={} status={}", entry, tmpVal);
                if (!tmpVal) {
                    val = tmpVal;
                }
            }
        }
        Event evt = (val) ? Event.REMOVAL : Event.FAILURE; 
        saveEntries();
        EventBus.publish(evt);
        return val;
    }

    /**
     * Removes a single {@link AddeEntry} from the set of available entries.
     * 
     * @param entry Entry to remove. Cannot be {@code null}.
     * 
     * @return {@code true} if something was removed, {@code false} otherwise.
     */
    public boolean removeEntry(final AddeEntry entry) {
        notNull(entry);
        boolean val = trie.remove(entry.asStringId()) != null;
        logger.trace("attempted remove={} status={}", entry, val);
        Event evt = val ? Event.REMOVAL : Event.FAILURE;
        saveEntries();
        EventBus.publish(evt);
        return val;
    }

    /**
     * Adds a single {@link AddeEntry} to {@link #trie}.
     * 
     * @param entry Entry to add. Cannot be {@code null}.
     * 
     * @throws NullPointerException if {@code entry} is {@code null}.
     */
    public void addEntry(final AddeEntry entry) {
        notNull(entry, "Cannot add a null entry");
        trie.put(entry.asStringId(), entry);
        saveEntries();
        lastAdded.clear();
        lastAdded.add(entry);
        EventBus.publish(Event.ADDITION);
    }

    /**
     * Adds a {@link Set} of {@link AddeEntry}s to {@link #trie}.
     * 
     * @param newEntries New entries to add to the server manager. Cannot be
     * {@code null}.
     * 
     * @throws NullPointerException if {@code newEntries} is {@code null}.
     */
    public void addEntries(final Collection<? extends AddeEntry> newEntries) {
        notNull(newEntries, "Cannot add a null set");
        for (AddeEntry newEntry : newEntries) {
            trie.put(newEntry.asStringId(), newEntry);
        }
        saveEntries();
        lastAdded.clear();
        lastAdded.addAll(newEntries);
        EventBus.publish(Event.ADDITION);
    }

    /**
     * Replaces the {@link AddeEntry}s within {@code trie} with the contents
     * of {@code newEntries}.
     * 
     * @param oldEntries Entries to be replaced. Cannot be {@code null}.
     * @param newEntries Entries to use as replacements. Cannot be 
     * {@code null}.
     * 
     * @throws NullPointerException if either of {@code oldEntries} or 
     * {@code newEntries} is {@code null}.
     */
    public void replaceEntries(final Collection<? extends AddeEntry> oldEntries, final Collection<? extends AddeEntry> newEntries) {
        notNull(oldEntries, "Cannot replace a null set");
        notNull(newEntries, "Cannot add a null set");

        for (AddeEntry oldEntry : oldEntries) {
            trie.remove(oldEntry.asStringId());
        }
        for (AddeEntry newEntry : newEntries) {
            trie.put(newEntry.asStringId(), newEntry);
        }
        lastAdded.clear();
        lastAdded.addAll(newEntries); // should probably be more thorough
        saveEntries();
        EventBus.publish(Event.REPLACEMENT);
    }

    // if true, filters out disabled local groups; if false, returns all local groups
    public Set<AddeServer.Group> getIdvStyleLocalGroups() {
        Set<LocalAddeEntry> localEntries = getLocalEntries();
        Set<AddeServer.Group> idvGroups = newLinkedHashSet(localEntries.size());
        for (LocalAddeEntry entry : localEntries) {
            if ((entry.getEntryStatus() == EntryStatus.ENABLED) && (entry.getEntryValidity() == EntryValidity.VERIFIED)) {
                String group = entry.getGroup();
                AddeServer.Group idvGroup = new AddeServer.Group("IMAGE", group, group);
                idvGroups.add(idvGroup);
            }
        }
        return idvGroups;
    }

    public Set<AddeServer.Group> getIdvStyleRemoteGroups(final String server, final String typeAsStr) {
        return getIdvStyleRemoteGroups(server, EntryTransforms.strToEntryType(typeAsStr));
    }

    public Set<AddeServer.Group> getIdvStyleRemoteGroups(final String server, final EntryType type) {
        Set<AddeServer.Group> idvGroups = newLinkedHashSet(trie.size());
        String typeStr = type.name();
        for (AddeEntry matched : trie.getPrefixedBy(server).values()) {
            if (matched == RemoteAddeEntry.INVALID_ENTRY) {
                continue;
            }

            if ((matched.getEntryStatus() == EntryStatus.ENABLED) && (matched.getEntryValidity() == EntryValidity.VERIFIED) && (matched.getEntryType() == type)) {
                String group = matched.getGroup();
                idvGroups.add(new AddeServer.Group(typeStr, group, group));
            }
        }
        return idvGroups;
    }

    /**
     * Returns a list of all available ADDE datasets, converted to IDV 
     * {@link AddeServer} objects.
     * 
     * @return List of {@code AddeServer} objects for each ADDE entry.
     */
    public List<AddeServer> getIdvStyleEntries() {
        return arrList(EntryTransforms.convertMcvServers(getEntrySet()));
    }

    /**
     * Returns a list that consists of the available ADDE datasets for a given 
     * {@link EntryType}, converted to IDV {@link AddeServer} objects.
     * 
     * @param type Only add entries with this type to the returned list. Cannot be {@code null}. 
     * 
     * @return {@code AddeServer} objects for each ADDE entry of the given type.
     */
    public Set<AddeServer> getIdvStyleEntries(final EntryType type) {
        return EntryTransforms.convertMcvServers(getVerifiedEntries(type));
    }

    /**
     * Returns a list that consists of the available ADDE datasets for a given 
     * {@link EntryType}, converted to IDV {@link AddeServer} objects.
     * 
     * @param typeAsStr Only add entries with this type to the returned list. 
     * Cannot be {@code null} and must be a value that works with 
     * {@link EntryTransforms#strToEntryType(String)}. 
     * 
     * @return {@code AddeServer} objects for each ADDE entry of the given type.
     * 
     * @see EntryTransforms#strToEntryType(String)
     */
    public Set<AddeServer> getIdvStyleEntries(final String typeAsStr) {
        return getIdvStyleEntries(EntryTransforms.strToEntryType(typeAsStr));
    }

    /**
     * Process all of the {@literal "IDV-style"} XML resources for a given
     * {@literal "source"}.
     * 
     * @param source Origin of the XML resources.
     * @param xmlResources Actual XML resources.
     * 
     * @return {@link Set} of the {@link AddeEntry AddeEntrys} extracted from
     * {@code xmlResources}.
     */
    private Set<AddeEntry> extractResourceEntries(EntrySource source, final XmlResourceCollection xmlResources) {
        Set<AddeEntry> entries = newLinkedHashSet(xmlResources.size());
        for (int i = 0; i < xmlResources.size(); i++) {
            Element root = xmlResources.getRoot(i);
            if (root == null) {
                continue;
            }
            entries.addAll(EntryTransforms.convertAddeServerXml(root, source));
        }
        return entries;
    }

    /**
     * Process all of the {@literal "user"} XML resources.
     * 
     * @param xmlResources Resource collection. Cannot be {@code null}.
     * 
     * @return {@link Set} of {@link RemoteAddeEntry}s contained within 
     * {@code resource}.
     */
    private Set<AddeEntry> extractUserEntries(final XmlResourceCollection xmlResources) {
        int rcSize = xmlResources.size();
        Set<AddeEntry> entries = newLinkedHashSet(rcSize);
        for (int i = 0; i < rcSize; i++) {
            Element root = xmlResources.getRoot(i);
            if (root == null) {
                continue;
            }
            entries.addAll(EntryTransforms.convertUserXml(root));
        }
        return entries;
    }

    /**
     * Returns the path to where the root directory of the user's McIDAS-X 
     * binaries <b>should</b> be. <b>The path may be invalid.</b>
     * 
     * <p>The default path is determined like so:
     * <pre>
     * System.getProperty("user.dir") + File.separatorChar + "adde"
     * </pre>
     * 
     * <p>Users can provide an arbitrary path at runtime by setting the 
     * {@code debug.localadde.rootdir} system property.
     * 
     * @return {@code String} containing the path to the McIDAS-X root directory. 
     * 
     * @see #PROP_DEBUG_LOCALROOT
     */
    public static String getAddeRootDirectory() {
        if (System.getProperties().containsKey(PROP_DEBUG_LOCALROOT)) {
            return System.getProperty(PROP_DEBUG_LOCALROOT);
        }
        return System.getProperty("user.dir") + File.separatorChar + "adde";
    }

    /**
     * Checks the value of the {@code debug.adde.reqs} system property to
     * determine whether or not the user has requested ADDE URL debugging 
     * output. Output is sent to {@link System#out}.
     * 
     * <p>Please keep in mind that the {@code debug.adde.reqs} can not 
     * force debugging for <i>all</i> ADDE requests. To do so will require
     * updates to the VisAD ADDE library.
     * 
     * @param defaultValue Value to return if {@code debug.adde.reqs} has
     * not been set.
     * 
     * @return If it exists, the value of {@code debug.adde.reqs}. 
     * Otherwise {@code debug.adde.reqs}.
     * 
     * @see edu.wisc.ssec.mcidas.adde.AddeURL
     * @see #PROP_DEBUG_ADDEURL
     */
    // TODO(jon): this sort of thing should *really* be happening within the 
    // ADDE library.
    public static boolean isAddeDebugEnabled(final boolean defaultValue) {
        return Boolean.parseBoolean(System.getProperty(PROP_DEBUG_ADDEURL, Boolean.toString(defaultValue)));
    }

    /**
     * Sets the value of the {@code debug.adde.reqs} system property so
     * that debugging output can be controlled without restarting McIDAS-V.
     * 
     * <p>Please keep in mind that the {@code debug.adde.reqs} can not 
     * force debugging for <i>all</i> ADDE requests. To do so will require
     * updates to the VisAD ADDE library.
     * 
     * @param value New value of {@code debug.adde.reqs}.
     * 
     * @return Previous value of {@code debug.adde.reqs}.
     * 
     * @see edu.wisc.ssec.mcidas.adde.AddeURL
     * @see #PROP_DEBUG_ADDEURL
     */
    public static boolean setAddeDebugEnabled(final boolean value) {
        return Boolean.parseBoolean(System.setProperty(PROP_DEBUG_ADDEURL, Boolean.toString(value)));
    }

    /**
     * Change the port we are listening on.
     * 
     * @param port New port number.
     */
    public static void setLocalPort(final String port) {
        localPort = port;
    }

    /**
     * Ask for the port we are listening on.
     * 
     * @return String representation of the listening port.
     */
    public static String getLocalPort() {
        return localPort;
    }

    /**
     * Get the next port by incrementing current port.
     * 
     * @return The next port that will be tried.
     */
    protected static String nextLocalPort() {
        return Integer.toString(Integer.parseInt(localPort) + 1);
    }

    /**
     * Starts the local server thread (if it isn't already running).
     */
    public void startLocalServer() {
        if (new File(ADDE_MCSERVL).exists()) {
            // Create and start the thread if there isn't already one running
            if (!checkLocalServer()) {
                if (!testLocalServer()) {
                    LogUtil.userErrorMessage("Local servers cannot write to userpath:\n"+USER_DIRECTORY);
                    logger.info("Local servers cannot write to userpath");
                    return;
                }
                thread = new AddeThread(this);
                thread.start();
                EventBus.publish(McservEvent.STARTED);
                logger.debug("started mcservl? checkLocalServer={}", checkLocalServer());
            } else {
                logger.debug("mcservl is already running");
            }
        } else {
            logger.debug("invalid path='{}'", ADDE_MCSERVL);
        }
    }

    /**
     * Stops the local server thread if it is running.
     */
    public void stopLocalServer() {
        if (checkLocalServer()) {
            //TODO: stopProcess (actually Process.destroy()) hangs on Macs...
            //      doesn't seem to kill the children properly
            if (!McIDASV.isMac()) {
                thread.stopProcess();
            }

            thread.interrupt();
            thread = null;
            EventBus.publish(McservEvent.STOPPED);
            logger.debug("stopped mcservl? checkLocalServer={}", checkLocalServer());
        } else {
            logger.debug("mcservl is not running.");
        }
    }
    
    /**
     * Test to see if the thread can access userpath
     * 
     * @return {@code true} if the local server can access userpath,
     * {@code false} otherwise.
     */
    public boolean testLocalServer() {
        StringBuilder err = new StringBuilder();
        String[] cmds = { ADDE_MCSERVL, "-t" };
        String[] env = McIDASV.isWindows() ? getWindowsAddeEnv() : getUnixAddeEnv();

        try {
            Process proc = Runtime.getRuntime().exec(cmds, env);
            int result = proc.waitFor();
            if (result != 0) {
                return false;
            }
        } catch (Exception e) {
                return false;
        }
        return true;
    }

    /**
     * Check to see if the thread is running.
     * 
     * @return {@code true} if the local server thread is running;
     * {@code false} otherwise.
     */
    public boolean checkLocalServer() {
        return (thread != null) && thread.isAlive();
    }
}
