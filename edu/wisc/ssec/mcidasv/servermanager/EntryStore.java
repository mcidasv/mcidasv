/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */
package edu.wisc.ssec.mcidasv.servermanager;

import static java.util.Objects.requireNonNull;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashMap;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;

import java.io.File;
import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.trie.PatriciaTrie;
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

/**
 * McIDAS-V ADDE server manager. This class is essentially the
 * {@literal "gatekeeper"} for anything having to do with the application's
 * collection of ADDE servers. This class is also responsible for controlling
 * the thread used to manage the external mcservl binary.
 *
 * @see AddeThread
 */
public class EntryStore {

    /** 
     * Property that allows users to supply arbitrary paths to McIDAS-X 
     * binaries used by mcservl.
     * 
     * @see #getAddeRootDirectory()
     */
    private static final String PROP_DEBUG_LOCALROOT =
        "debug.localadde.rootdir";

    /**
     * Property that allows users to control debug output from ADDE requests.
     * 
     * @see #isAddeDebugEnabled(boolean)
     * @see #setAddeDebugEnabled(boolean)
     */
    private static final String PROP_DEBUG_ADDEURL = "debug.adde.reqs";

    /**
     * {@literal "Userpath"} not writable error message.
     * This is the one that is shown in the GUI via
     * {@link LogUtil#userErrorMessage(String)}.
     */
    private static final String ERROR_LOGUTIL_USERPATH =
        "Local servers cannot write to userpath:\n%s";

    /**
     * {@literal "Userpath"} not writable error message.
     * This one is used by the logging system.
     */
    private static final String ERROR_USERPATH =
        "Local servers cannot write to userpath ('{}')";

    /**
     * SLF4J-style formatting string for use when {@code RESOLV.SRV} can not
     * be found. .
     */
    private static final String WARN_NO_RESOLVSRV =
        "EntryStore: RESOLV.SRV missing; expected='{}'";

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
    private static final Logger logger =
        LoggerFactory.getLogger(EntryStore.class);

    /** Preference key for ADDE entries. */
    private static final String PREF_ADDE_ENTRIES = "mcv.servers.entries";

    /** The ADDE servers known to McIDAS-V. */
    private final PatriciaTrie<AddeEntry> trie;

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

    /** Which port is this particular manager operating on */
    private static String localPort;

    /** Thread that monitors the mcservl process. */
    private static AddeThread thread;

    /** Last {@link AddeEntry AddeEntries} added to the manager. */
    private final List<AddeEntry> lastAdded;

    /** McIDAS-V preferences store. */
    private final IdvObjectStore idvStore;

    /**
     * Constructs a server manager.
     * 
     * @param store McIDAS-V's preferences store. Cannot be {@code null}.
     * @param rscManager McIDAS-V's resource manager. Cannot be {@code null}.
     *
     * @throws NullPointerException if either of {@code store} or
     * {@code rscManager} is {@code null}.
     */
    public EntryStore(final IdvObjectStore store,
                      final IdvResourceManager rscManager)
    {
        requireNonNull(store);
        requireNonNull(rscManager);

        this.idvStore = store;
        this.trie = new PatriciaTrie<>();
        this.ADDE_DIRECTORY = getAddeRootDirectory();
        this.ADDE_BIN = ADDE_DIRECTORY + File.separator + "bin";
        this.ADDE_DATA = ADDE_DIRECTORY + File.separator + "data";
        EntryStore.localPort = Constants.LOCAL_ADDE_PORT;
        this.lastAdded = arrList();
        AnnotationProcessor.process(this);

        McIDASV mcv = McIDASV.getStaticMcv();
        USER_DIRECTORY = mcv.getUserDirectory();
        ADDE_RESOLV = mcv.getUserFile("RESOLV.SRV");

        if (McIDASV.isWindows()) {
            ADDE_MCSERVL = ADDE_BIN + "\\mcservl.exe";
        } else {
            ADDE_MCSERVL = ADDE_BIN + "/mcservl";
        }

        try {
            Set<LocalAddeEntry> locals =
                EntryTransforms.readResolvFile(ADDE_RESOLV);
            putEntries(trie, locals);
        } catch (IOException e) {
            logger.warn(WARN_NO_RESOLVSRV, ADDE_RESOLV);
        }

        XmlResourceCollection userResource =
            rscManager.getXmlResources(ResourceManager.RSC_NEW_USERSERVERS);
        XmlResourceCollection sysResource =
            rscManager.getXmlResources(IdvResourceManager.RSC_ADDESERVER);

        Set<AddeEntry> systemEntries =
            extractResourceEntries(EntrySource.SYSTEM, sysResource);

        Set<AddeEntry> prefEntries = extractPreferencesEntries(store);
        prefEntries = removeDeletedSystemEntries(prefEntries, systemEntries);

        Set<AddeEntry> userEntries = extractUserEntries(userResource);
        userEntries = removeDeletedSystemEntries(userEntries, systemEntries);

        putEntries(trie, systemEntries);
        putEntries(trie, userEntries);
        putEntries(trie, prefEntries);

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
    private static Set<AddeEntry> removeDeletedSystemEntries(
        final Collection<? extends AddeEntry> entries,
        final Collection<? extends AddeEntry> systemEntries)
    {
        Set<AddeEntry> pruned = newLinkedHashSet(entries.size());
        for (AddeEntry entry : entries) {
            if (entry.getEntrySource() != EntrySource.SYSTEM) {
                pruned.add(entry);
            } else if (systemEntries.contains(entry)) {
                pruned.add(entry);
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
    private static void putEntries(
        final PatriciaTrie<AddeEntry> trie,
        final Collection<? extends AddeEntry> newEntries)
    {
        requireNonNull(trie);
        requireNonNull(newEntries);

        for (AddeEntry e : newEntries) {
            trie.put(e.asStringId(), e);
        }
    }

    /**
     * Returns the {@link IdvObjectStore} used to save user preferences.
     *
     * @return {@code IdvObjectStore} used by the rest of McIDAS-V.
     */
    public IdvObjectStore getIdvStore() {
        return idvStore;
    }

    /**
     * Returns environment variables that allow mcservl to run on Windows.
     *
     * @return {@code String} array containing mcservl's environment variables.
     */
    protected String[] getWindowsAddeEnv() {
        // Drive letters should come from environment
        // Java drive is not necessarily system drive
        return new String[] {
            "PATH=" + ADDE_BIN,
            "MCPATH=" + USER_DIRECTORY+':'+ADDE_DATA,
            "MCUSERDIR=" + USER_DIRECTORY,
            "MCNOPREPEND=1",
            "MCTRACE=" + (Boolean.parseBoolean(System.getProperty("debug.adde.reqs", "false")) ? "1" : "0"),
            "MCTRACK=NO",
            "MCJAVAPATH=" + System.getProperty("java.home"),
            "MCBUFRJARPATH=" + ADDE_BIN,
            "SYSTEMDRIVE=" + System.getenv("SystemDrive"),
            "SYSTEMROOT=" + System.getenv("SystemRoot"),
            "HOMEDRIVE=" + System.getenv("HOMEDRIVE"),
            "HOMEPATH=\\Windows"
        };
    }

    /**
     * Returns environment variables that allow mcservl to run on
     * {@literal "unix-like"} systems.
     *
     * @return {@code String} array containing mcservl's environment variables.
     */
    protected String[] getUnixAddeEnv() {
        return new String[] {
            "PATH=" + ADDE_BIN,
            "HOME=" + System.getenv("HOME"),
            "USER=" + System.getenv("USER"),
            "MCPATH=" + USER_DIRECTORY+':'+ADDE_DATA,
            "MCUSERDIR=" + USER_DIRECTORY,
            "LD_LIBRARY_PATH=" + ADDE_BIN,
            "DYLD_LIBRARY_PATH=" + ADDE_BIN,
            "MCNOPREPEND=1",
            "MCTRACE=" + (Boolean.parseBoolean(System.getProperty("debug.adde.reqs", "false")) ? "1" : "0"),
            "MCTRACK=NO",
            "MCJAVAPATH=" + System.getProperty("java.home"),
            "MCBUFRJARPATH=" + ADDE_BIN
        };
    }

    /**
     * Returns command line used to launch mcservl.
     *
     * @return {@code String} array that represents an invocation of mcservl.
     */
    protected String[] getAddeCommands() {
        String mcvPID = Integer.toString(PosixModule.getpid());
        if (McIDASV.isWindows() || (mcvPID == null) || "0".equals(mcvPID)) {
            return new String[] { ADDE_MCSERVL, "-v", "-p", localPort };
        } else {
            return new String[] {
                ADDE_MCSERVL, "-v", "-p", localPort, "-i", mcvPID
            };
        }
    }

    /**
     * Determine the validity of a given {@link AddeEntry}.
     * 
     * @param entry Entry to check. Cannot be {@code null}.
     * 
     * @return {@code true} if {@code entry} is invalid or {@code false}
     * otherwise.
     *
     * @throws NullPointerException if {@code entry} is {@code null}.
     * @throws AssertionError if {@code entry} is somehow neither a
     * {@code RemoteAddeEntry} or {@code LocalAddeEntry}.
     * 
     * @see LocalAddeEntry#INVALID_ENTRY
     * @see RemoteAddeEntry#INVALID_ENTRY
     */
    public static boolean isInvalidEntry(final AddeEntry entry) {
        requireNonNull(entry);

        boolean retVal = true;
        if (entry instanceof RemoteAddeEntry) {
            retVal = RemoteAddeEntry.INVALID_ENTRY.equals(entry);
        } else if (entry instanceof LocalAddeEntry) {
            retVal = LocalAddeEntry.INVALID_ENTRY.equals(entry);
        } else {
            String clsName = entry.getClass().getName();
            throw new AssertionError("Unknown AddeEntry type: "+clsName);
        }
        return retVal;
    }

    /**
     * Returns the {@link AddeEntry AddeEntries} stored in the user's
     * preferences.
     * 
     * @param store Object store that represents the user's preferences.
     * Cannot be {@code null}.
     * 
     * @return Either the {@code AddeEntrys} stored in the prefs or an empty
     * {@link Set}.
     */
    private Set<AddeEntry> extractPreferencesEntries(
        final IdvObjectStore store)
    {
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
            entries.addAll(
                asList.stream()
                      .filter(entry -> entry instanceof RemoteAddeEntry)
                      .collect(Collectors.toList()));
        }
        return entries;
    }

    /**
     * Responds to server manager events being passed with the event bus. 
     * 
     * @param evt Event to which this method is responding. Cannot be
     * {@code null}.
     *
     * @throws NullPointerException if {@code evt} is {@code null}.
     */
    @EventSubscriber(eventClass=Event.class)
    public void onEvent(Event evt) {
        requireNonNull(evt);

        saveEntries();
    }

    /**
     * Saves the current set of ADDE servers to the user's preferences and
     * {@link #ADDE_RESOLV}.
     */
    public void saveEntries() {
        idvStore.put(PREF_ADDE_ENTRIES, arrList(trie.values()));
        idvStore.saveIfNeeded();
        try {
            EntryTransforms.writeResolvFile(ADDE_RESOLV, getLocalEntries());
        } catch (IOException e) {
            logger.error(WARN_NO_RESOLVSRV, ADDE_RESOLV);
        }
    }

    /**
     * Saves the list of ADDE entries to both the user's preferences and
     * {@link #ADDE_RESOLV}.
     */
    public void saveForShutdown() {
        idvStore.put(PREF_ADDE_ENTRIES, arrList(getPersistedEntrySet()));
        idvStore.saveIfNeeded();
        try {
            EntryTransforms.writeResolvFile(ADDE_RESOLV,
                getPersistedLocalEntries());
        } catch (IOException e) {
            logger.error(WARN_NO_RESOLVSRV, ADDE_RESOLV);
        }
    }

    /**
     * Searches the newest entries for the entries of the given
     * {@link EntryType}.
     * 
     * @param type Look for entries matching this {@code EntryType}.
     * Cannot be {@code null}.
     * 
     * @return Either a {@link List} of entries or an empty {@code List}.
     *
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    public List<AddeEntry> getLastAddedByType(final EntryType type) {
        requireNonNull(type);

        List<AddeEntry> entries = arrList(lastAdded.size());
        entries.addAll(lastAdded.stream()
                                .filter(entry -> entry.getEntryType() == type)
                                .collect(Collectors.toList()));
        return entries;
    }

    /**
     * Returns the {@link AddeEntry AddeEntries} that were added last, filtered
     * by the given {@link EntryType EntryTypes}.
     *
     * @param types Filter the last added entries by these entry type.
     * Cannot be {@code null}.
     *
     * @return {@link List} of the last added entries, filtered by
     * {@code types}.
     *
     * @throws NullPointerException if {@code types} is {@code null}.
     */
    public List<AddeEntry> getLastAddedByTypes(final EnumSet<EntryType> types) {
        requireNonNull(types);

        List<AddeEntry> entries = arrList(lastAdded.size());
        entries.addAll(
            lastAdded.stream()
                .filter(entry -> types.contains(entry.getEntryType()))
                .collect(Collectors.toList()));
        return entries;
    }

    /**
     * Returns the {@link AddeEntry AddeEntries} that were added last. Note
     * that this value is <b>not</b> preserved between sessions.
     *
     * @return {@link List} of the last ADDE entries that were added. May be
     * empty.
     */
    public List<AddeEntry> getLastAdded() {
        return arrList(lastAdded);
    }

    /**
     * Returns the {@link Set} of {@link AddeEntry AddeEntries} that are known
     * to work (for a given {@link EntryType} of entries).
     * 
     * @param type The {@code EntryType} you are interested in. Cannot be
     * {@code null}.
     * 
     * @return A {@code Set} of matching remote ADDE entries. If there were no
     * matches, an empty {@code Set} is returned.
     *
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    public Set<AddeEntry> getVerifiedEntries(final EntryType type) {
        requireNonNull(type);

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

    /**
     * Returns the available {@link AddeEntry AddeEntries}, grouped by
     * {@link EntryType}.
     *
     * @return {@link Map} of {@code EntryType} to a {@link Set} containing all
     * of the entries that match that {@code EntryType}.
     */
    public Map<EntryType, Set<AddeEntry>> getVerifiedEntriesByTypes() {
        Map<EntryType, Set<AddeEntry>> entryMap =
            newLinkedHashMap(EntryType.values().length);

        int size = trie.size();

        for (EntryType type : EntryType.values()) {
            entryMap.put(type, new LinkedHashSet<>(size));
        }

        for (AddeEntry entry : trie.values()) {
            Set<AddeEntry> entrySet = entryMap.get(entry.getEntryType());
            entrySet.add(entry);
        }
        return entryMap;
    }

    /**
     * Returns the {@link Set} of {@link AddeEntry#getGroup() groups} that
     * match the given {@code address} and {@code type}.
     * 
     * @param address ADDE server address whose groups are needed.
     * Cannot be {@code null}.
     * @param type Only include groups that match {@link EntryType}.
     * Cannot be {@code null}.
     * 
     * @return Either a set containing the desired groups, or an empty set if
     * there were no matches.
     *
     * @throws NullPointerException if either {@code address} or {@code type}
     * is {@code null}.
     */
    public Set<String> getGroupsFor(final String address, EntryType type) {
        requireNonNull(address);
        requireNonNull(type);

        Set<String> groups = newLinkedHashSet(trie.size());
        groups.addAll(
            trie.prefixMap(address + '!').values().stream()
                .filter(e -> e.getAddress().equals(address) && (e.getEntryType() == type))
                .map(AddeEntry::getGroup)
                .collect(Collectors.toList()));
        return groups;
    }

    /**
     * Search the server manager for entries that match {@code prefix}.
     * 
     * @param prefix {@code String} to match. Cannot be {@code null}.
     * 
     * @return {@link List} containing matching entries. If there were no 
     * matches the {@code List} will be empty.
     *
     * @throws NullPointerException if {@code prefix} is {@code null}.
     *
     * @see AddeEntry#asStringId()
     */
    public List<AddeEntry> searchWithPrefix(final String prefix) {
        requireNonNull(prefix);
        return arrList(trie.prefixMap(prefix).values());
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
        addresses.addAll(trie.values().stream()
                             .map(AddeEntry::getAddress)
                             .collect(Collectors.toList()));
        return addresses;
    }

    /**
     * Returns a {@link Set} containing {@code ADDRESS/GROUPNAME}
     * {@link String Strings} for each {@link RemoteAddeEntry}.
     * 
     * @return The {@literal "entry text"} representations of each 
     * {@code RemoteAddeEntry}.
     * 
     * @see RemoteAddeEntry#getEntryText()
     */
    public Set<String> getRemoteEntryTexts() {
        Set<String> strs = newLinkedHashSet(trie.size());
        strs.addAll(trie.values().stream()
                        .filter(entry -> entry instanceof RemoteAddeEntry)
                        .map(AddeEntry::getEntryText)
                        .collect(Collectors.toList()));
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
        requireNonNull(address);

        Set<String> groups = newLinkedHashSet(trie.size());
        groups.addAll(trie.prefixMap(address + '!').values().stream()
                          .map(AddeEntry::getGroup)
                          .collect(Collectors.toList()));
        return groups;
    }

    /**
     * Returns the {@link Set} of {@link EntryType EntryTypes} for a given
     * {@code group} on a given {@code address}.
     * 
     * @param address Address of a server.
     * @param group Group whose {@literal "types"} you want.
     * 
     * @return Either of all the types for a given {@code address} and 
     * {@code group} or an empty {@code Set} if there were no matches.
     */
    public Set<EntryType> getTypes(final String address, final String group) {
        Set<EntryType> types = newLinkedHashSet(trie.size());
        types.addAll(
            trie.prefixMap(address + '!' + group + '!').values().stream()
                .map(AddeEntry::getEntryType)
                .collect(Collectors.toList()));
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
    public AddeAccount getAccountingFor(final String address,
                                        final String group,
                                        EntryType type)
    {
        Collection<AddeEntry> entries =
            trie.prefixMap(address+'!'+group+'!'+type.name()).values();
        for (AddeEntry entry : entries) {
            if (!isInvalidEntry(entry)) {
                return entry.getAccount();
            }
        }
        return AddeEntry.DEFAULT_ACCOUNT;
    }

    /**
     * Returns the accounting for the given {@code idvServer} and
     * {@code typeAsStr}.
     *
     * @param idvServer Server to search for.
     * @param typeAsStr One of {@literal "IMAGE"}, {@literal "POINT"},
     * {@literal "GRID"}, {@literal "TEXT"}, {@literal "NAV"},
     * {@literal "RADAR"}, {@literal "UNKNOWN"}, or {@literal "INVALID"}.
     *
     * @return {@code AddeAccount} associated with {@code idvServer} and
     * {@code typeAsStr}.
     */
    public AddeAccount getAccountingFor(final AddeServer idvServer,
                                        String typeAsStr)
    {
        String address = idvServer.getName();
        List<AddeServer.Group> groups =
            (List<AddeServer.Group>)idvServer.getGroups();
        if ((groups != null) && !groups.isEmpty()) {
            EntryType type = EntryTransforms.strToEntryType(typeAsStr);
            return getAccountingFor(address, groups.get(0).getName(), type);
        } else {
            return RemoteAddeEntry.DEFAULT_ACCOUNT;
        }
    }

    /**
     * Returns the complete {@link Set} of {@link AddeEntry AddeEntries}.
     *
     * @return All of the managed ADDE entries.
     */
    public Set<AddeEntry> getEntrySet() {
        return newLinkedHashSet(trie.values());
    }

    /**
     * Returns all non-temporary {@link AddeEntry AddeEntries}.
     *
     * @return {@link Set} of ADDE entries that stick around between McIDAS-V
     * sessions.
     */
    public Set<AddeEntry> getPersistedEntrySet() {
        Set<AddeEntry> entries = newLinkedHashSet(trie.size());
        entries.addAll(trie.values().stream()
                           .filter(entry -> !entry.isEntryTemporary())
                           .collect(Collectors.toList()));
        return entries;
    }

    /**
     * Returns the complete {@link Set} of
     * {@link RemoteAddeEntry RemoteAddeEntries}.
     * 
     * @return {@code Set} of remote ADDE entries stored within the available
     * entries.
     */
    public Set<RemoteAddeEntry> getRemoteEntries() {
        Set<RemoteAddeEntry> remotes = newLinkedHashSet(trie.size());
        remotes.addAll(trie.values().stream()
                           .filter(e -> e instanceof RemoteAddeEntry)
                           .map(e -> (RemoteAddeEntry) e)
                           .collect(Collectors.toList()));
        return remotes;
    }

    /**
     * Returns the complete {@link Set} of
     * {@link LocalAddeEntry LocalAddeEntries}.
     * 
     * @return {@code Set} of local ADDE entries  stored within the available
     * entries.
     */
    public Set<LocalAddeEntry> getLocalEntries() {
        Set<LocalAddeEntry> locals = newLinkedHashSet(trie.size());
        locals.addAll(trie.prefixMap("localhost").values().stream()
                          .filter(e -> e instanceof LocalAddeEntry)
                          .map(e -> (LocalAddeEntry) e)
                          .collect(Collectors.toList()));
        return locals;
    }

    /**
     * Returns the {@link Set} of {@link LocalAddeEntry LocalAddeEntries} that
     * will be saved between McIDAS-V sessions.
     * 
     * <p>Note: all this does is check {@link LocalAddeEntry#isEntryTemporary()}.
     * 
     * @return Local ADDE entries that will be saved for the next session.
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

    /**
     * Returns any {@link LocalAddeEntry LocalAddeEntries} that will be removed
     * at the end of the current McIDAS-V session.
     *
     * @return {@code Set} of all the temporary local ADDE entries.
     */
    public Set<LocalAddeEntry> getTemporaryLocalEntries() {
        return this.filterLocalEntriesByTemporaryStatus(true);
    }

    /**
     * Filters the local entries by whether or not they are set as
     * {@literal "temporary"}.
     *
     * @param getTemporaryEntries {@code true} returns temporary local
     * entries; {@code false} returns local entries that are permanent.
     *
     * @return {@link Set} of filtered local ADDE entries.
     */
    private Set<LocalAddeEntry> filterLocalEntriesByTemporaryStatus(
        final boolean getTemporaryEntries)
    {
        Set<LocalAddeEntry> locals = newLinkedHashSet(trie.size());
        trie.prefixMap("localhost").values().stream()
            .filter(e -> e instanceof LocalAddeEntry)
            .forEach(e -> {
                LocalAddeEntry local = (LocalAddeEntry)e;
                if (local.isEntryTemporary() == getTemporaryEntries) {
                    locals.add(local);
                }
            });
        return locals;
    }

    /**
     * Removes the given {@link AddeEntry AddeEntries}.
     *
     * @param removedEntries {@code AddeEntry} objects to remove.
     * Cannot be {@code null}.
     *
     * @return Whether or not {@code removeEntries} were removed.
     *
     * @throws NullPointerException if {@code removedEntries} is {@code null}.
     */
    public boolean removeEntries(
        final Collection<? extends AddeEntry> removedEntries)
    {
        requireNonNull(removedEntries);

        boolean val = true;
        boolean tmp = true;
        for (AddeEntry e : removedEntries) {
            if (e.getEntrySource() != EntrySource.SYSTEM) {
                tmp = trie.remove(e.asStringId()) != null;
                logger.trace("attempted bulk remove={} status={}", e, tmp);
                if (!tmp) {
                    val = tmp;
                }
            }
        }
        Event evt = val ? Event.REMOVAL : Event.FAILURE;
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
     *
     * @throws NullPointerException if {@code entry} is {@code null}.
     */
    public boolean removeEntry(final AddeEntry entry) {
        requireNonNull(entry);

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
        requireNonNull(entry, "Cannot add a null entry.");

        trie.put(entry.asStringId(), entry);
        saveEntries();
        lastAdded.clear();
        lastAdded.add(entry);
        EventBus.publish(Event.ADDITION);
    }

    /**
     * Adds a {@link Set} of {@link AddeEntry AddeEntries} to {@link #trie}.
     * 
     * @param newEntries New entries to add to the server manager. Cannot be
     * {@code null}.
     * 
     * @throws NullPointerException if {@code newEntries} is {@code null}.
     */
    public void addEntries(final Collection<? extends AddeEntry> newEntries) {
        requireNonNull(newEntries, "Cannot add a null Collection.");

        for (AddeEntry newEntry : newEntries) {
            trie.put(newEntry.asStringId(), newEntry);
        }
        saveEntries();
        lastAdded.clear();
        lastAdded.addAll(newEntries);
        EventBus.publish(Event.ADDITION);
    }

    /**
     * Replaces the {@link AddeEntry AddeEntries} within {@code trie} with the
     * contents of {@code newEntries}.
     * 
     * @param oldEntries Entries to be replaced. Cannot be {@code null}.
     * @param newEntries Entries to use as replacements. Cannot be 
     * {@code null}.
     * 
     * @throws NullPointerException if either of {@code oldEntries} or 
     * {@code newEntries} is {@code null}.
     */
    public void replaceEntries(
        final Collection<? extends AddeEntry> oldEntries,
        final Collection<? extends AddeEntry> newEntries)
    {
        requireNonNull(oldEntries, "Cannot replace a null Collection.");
        requireNonNull(newEntries, "Cannot add a null Collection.");

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

    /**
     * Returns all enabled, valid {@link LocalAddeEntry LocalAddeEntries} as a
     * collection of {@literal "IDV style"} {@link AddeServer.Group}
     * objects.
     *
     * @return {@link Set} of {@code AddeServer.Group} objects that corresponds
     * with the enabled, valid local ADDE entries.
     */
    // if true, filters out disabled local groups; if false, returns all
    // local groups
    public Set<AddeServer.Group> getIdvStyleLocalGroups() {
        Set<LocalAddeEntry> localEntries = getLocalEntries();
        Set<AddeServer.Group> idvGroups = newLinkedHashSet(localEntries.size());
        for (LocalAddeEntry e : localEntries) {
            boolean enabled = e.getEntryStatus() == EntryStatus.ENABLED;
            boolean verified = e.getEntryValidity() == EntryValidity.VERIFIED;
            if (enabled && verified) {
                String group = e.getGroup();
                AddeServer.Group idvGroup =
                    new AddeServer.Group("IMAGE", group, group);
                idvGroups.add(idvGroup);
            }
        }
        return idvGroups;
    }

    /**
     * Returns the entries matching the given {@code server} and
     * {@code typeAsStr} parameters as a collection of
     * {@link ucar.unidata.idv.chooser.adde.AddeServer.Group AddeServer.Group}
     * objects.
     *
     * @param server Remote ADDE server. Should not be {@code null}.
     * @param typeAsStr Entry type. One of {@literal "IMAGE"},
     * {@literal "POINT"}, {@literal "GRID"}, {@literal "TEXT"},
     * {@literal "NAV"}, {@literal "RADAR"}, {@literal "UNKNOWN"}, or
     * {@literal "INVALID"}. Should not be {@code null}.
     *
     * @return {@link Set} of {@code AddeServer.Group} objects that corresponds
     * to the entries associated with {@code server} and {@code typeAsStr}.
     */
    public Set<AddeServer.Group> getIdvStyleRemoteGroups(
        final String server,
        final String typeAsStr)
    {
        return getIdvStyleRemoteGroups(server,
            EntryTransforms.strToEntryType(typeAsStr));
    }

    /**
     * Returns the entries matching the given {@code server} and
     * {@code type} parameters as a collection of
     * {@link AddeServer.Group}
     * objects.
     *
     * @param server Remote ADDE server. Should not be {@code null}.
     * @param type Entry type. Should not be {@code null}.
     *
     * @return {@link Set} of {@code AddeServer.Group} objects that corresponds
     * to the entries associated with {@code server} and {@code type}.
     */
    public Set<AddeServer.Group> getIdvStyleRemoteGroups(final String server,
                                                         final EntryType type)
    {
        Set<AddeServer.Group> idvGroups = newLinkedHashSet(trie.size());
        String typeStr = type.name();
        for (AddeEntry e : trie.prefixMap(server).values()) {
            if (e == RemoteAddeEntry.INVALID_ENTRY) {
                continue;
            }

            boolean enabled = e.getEntryStatus() == EntryStatus.ENABLED;
            boolean verified = e.getEntryValidity() == EntryValidity.VERIFIED;
            boolean typeMatched = e.getEntryType() == type;
            if (enabled && verified && typeMatched) {
                String group = e.getGroup();
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
     * @param type Only add entries with this type to the returned list.
     * Cannot be {@code null}.
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
    private Set<AddeEntry> extractResourceEntries(
        EntrySource source,
        final XmlResourceCollection xmlResources)
    {
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
     * @return {@link Set} of {@link RemoteAddeEntry RemoteAddeEntries}
     * contained within {@code resource}.
     */
    private Set<AddeEntry> extractUserEntries(
        final XmlResourceCollection xmlResources)
    {
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
     * @return {@code String} containing the path to the McIDAS-X root
     * directory.
     * 
     * @see #PROP_DEBUG_LOCALROOT
     */
    public static String getAddeRootDirectory() {
        if (System.getProperties().containsKey(PROP_DEBUG_LOCALROOT)) {
            return System.getProperty(PROP_DEBUG_LOCALROOT);
        }
        String userDir = System.getProperty("user.dir");
        Path p;
        if (userDir.endsWith("lib") || userDir.endsWith("lib/")) {
            p = Paths.get(userDir, "..", "adde");
        } else {
            p = Paths.get(userDir, "adde");
        }
        return p.normalize().toString();
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
     * @param defValue Value to return if {@code debug.adde.reqs} has
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
    public static boolean isAddeDebugEnabled(final boolean defValue) {
        String systemProperty =
            System.getProperty(PROP_DEBUG_ADDEURL, Boolean.toString(defValue));
        return Boolean.parseBoolean(systemProperty);
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
        String systemProperty =
            System.setProperty(PROP_DEBUG_ADDEURL, Boolean.toString(value));
        return Boolean.parseBoolean(systemProperty);
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
                    String logUtil =
                        String.format(ERROR_LOGUTIL_USERPATH, USER_DIRECTORY);
                    LogUtil.userErrorMessage(logUtil);
                    logger.info(ERROR_USERPATH, USER_DIRECTORY);
                    return;
                }
                thread = new AddeThread(this);
                thread.start();
                EventBus.publish(McservEvent.STARTED);
                boolean status = checkLocalServer();
                logger.debug("started mcservl? checkLocalServer={}", status);
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
            boolean status = checkLocalServer();
            logger.debug("stopped mcservl? checkLocalServer={}", status);
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
        String[] cmds = { ADDE_MCSERVL, "-t" };
        String[] env = McIDASV.isWindows()
                       ? getWindowsAddeEnv()
                       : getUnixAddeEnv();

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
