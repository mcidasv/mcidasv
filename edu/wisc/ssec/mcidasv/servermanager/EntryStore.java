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

import static edu.wisc.ssec.mcidasv.util.Contract.notNull;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;

import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.xml.XmlResourceCollection;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.ResourceManager;
import edu.wisc.ssec.mcidasv.servermanager.AddeAccount;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntrySource;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryStatus;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryValidity;
import edu.wisc.ssec.mcidasv.servermanager.AddeThread.McservEvent;
import edu.wisc.ssec.mcidasv.util.trie.CharSequenceKeyAnalyzer;
import edu.wisc.ssec.mcidasv.util.trie.PatriciaTrie;

public class EntryStore {

    private static final String PROP_DEBUG_LOCALROOT = "debug.localadde.rootdir";

    public enum Event { REPLACEMENT, REMOVAL, ADDITION, UPDATE, FAILURE, STARTED, UNKNOWN };

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

    /** Path to the user's {@literal ".mcidasv"} directory. */
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

    public EntryStore(final IdvObjectStore store, final IdvResourceManager rscManager) {
        notNull(store);
        notNull(rscManager);

        this.idvStore = store;
        this.trie = new PatriciaTrie<String, AddeEntry>(new CharSequenceKeyAnalyzer());
        this.ADDE_DIRECTORY = getAddeRootDirectory();
        this.ADDE_BIN = ADDE_DIRECTORY + File.separator + "bin";
        this.ADDE_DATA = ADDE_DIRECTORY + File.separator + "data";
        this.localPort = Constants.LOCAL_ADDE_PORT;
        this.restartingMcserv = false;
        this.lastAdded = arrList();
        AnnotationProcessor.process(this);

        USER_DIRECTORY = store.getUserDirectory().toString();
        MCTRACE = "0";
        if (McIDASV.isWindows()) {
            ADDE_MCSERVL = ADDE_BIN + "\\mcservl.exe";
            ADDE_RESOLV = USER_DIRECTORY + "\\RESOLV.SRV";
        } else {
            ADDE_MCSERVL = ADDE_BIN + "/mcservl";
            ADDE_RESOLV = USER_DIRECTORY + "/RESOLV.SRV";
        }

        try {
            Set<LocalAddeEntry> locals = EntryTransforms.readResolvFile(ADDE_RESOLV);
            putEntries(trie, locals);
        } catch (IOException e) {
            logger.error("EntryStore: RESOLV.SRV missing; expected=\""+ADDE_RESOLV+"\"");
        }

        XmlResourceCollection userResource = rscManager.getXmlResources(ResourceManager.RSC_NEW_USERSERVERS);
        XmlResourceCollection sysResource = rscManager.getXmlResources(IdvResourceManager.RSC_ADDESERVER);
        putEntries(trie, extractFromPreferences(store));
        putEntries(trie, extractUserEntries(userResource));
        putEntries(trie, extractResourceEntries(EntrySource.SYSTEM, sysResource));
    }

    private static void putEntries(final PatriciaTrie<String, AddeEntry> trie, final Collection<? extends AddeEntry> newEntries) {
        notNull(trie);
        notNull(newEntries);
        for (AddeEntry e : newEntries) {
            trie.put(e.asStringId(), e);
        }
    }

    protected IdvObjectStore getIdvStore() {
        return idvStore;
    }

    protected String[] getWindowsAddeEnv() {
        String driveLetter = McIDASV.getJavaDriveLetter();
        return new String[] {
            "PATH=" + ADDE_BIN,
            "MCPATH=" + USER_DIRECTORY+':'+ADDE_DATA,
            "MCNOPREPEND=1",
            "MCTRACE=" + MCTRACE,
            "MCJAVAPATH=" + System.getProperty("java.home"),
            "MCBUFRJARPATH=" + ADDE_BIN,
            "SYSTEMDRIVE=" + driveLetter,
            "SYSTEMROOT=" + driveLetter + "\\Windows",
            "HOMEDRIVE=" + driveLetter,
            "HOMEPATH=\\Windows"
        };
    }

    protected String[] getUnixAddeEnv() {
        return new String[] {
            "PATH=" + ADDE_BIN,
            "MCPATH=" + USER_DIRECTORY+':'+ADDE_DATA,
            "LD_LIBRARY_PATH=" + ADDE_BIN,
            "DYLD_LIBRARY_PATH=" + ADDE_BIN,
            "MCNOPREPEND=1",
            "MCTRACE=" + MCTRACE,
            "MCJAVAPATH=" + System.getProperty("java.home"),
            "MCBUFRJARPATH=" + ADDE_BIN
        };
    }

    protected String[] getAddeCommands() {
        return new String[] { ADDE_MCSERVL, "-p", localPort, "-v" };
    }

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

    private Set<AddeEntry> extractFromPreferences(final IdvObjectStore store) {
        assert store != null;
        Set<AddeEntry> entries = newLinkedHashSet();

        // this is valid--the only thing ever written to 
        // PREF_REMOTE_ADDE_ENTRIES is an ArrayList of RemoteAddeEntry objects.
        @SuppressWarnings("unchecked")
        List<AddeEntry> asList = 
            (List<AddeEntry>)store.get(PREF_ADDE_ENTRIES);
        if (asList != null) {
            for (AddeEntry entry : asList) {
                if (entry instanceof RemoteAddeEntry) {
                    entries.add(entry);
                }
            }
        }

        return entries;
    }

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

    public List<AddeEntry> getLastAddedByType(final EntryType type) {
        notNull(type);
        List<AddeEntry> entries = arrList();
        for (AddeEntry entry : lastAdded) {
            if (entry.getEntryType() == type) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public List<AddeEntry> getLastAddedByTypes(final EnumSet<EntryType> types) {
        notNull(types);
        List<AddeEntry> entries = arrList();
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
        Set<AddeEntry> verified = newLinkedHashSet();
        for (AddeEntry entry : trie.values()) {
            if (entry.getEntryType() != type)
                continue;

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
        Map<EntryType, Set<AddeEntry>> entryMap = new LinkedHashMap<EntryType, Set<AddeEntry>>();
        for (EntryType type : EntryType.values()) {
            entryMap.put(type, new LinkedHashSet<AddeEntry>());
        }

        for (AddeEntry entry : trie.values()) {
            Set<AddeEntry> entrySet = entryMap.get(entry.getEntryType());
            entrySet.add(entry);
        }
        return entryMap;
    }

    /**
     * Returns the {@link Set} of {@link AddeEntry#group}s that match
     * the given {@code address} and {@code type}.
     * 
     * @param address
     * @param type
     * 
     * @return Either a set containing the desired groups, or an empty set if
     * there were no matches.
     */
    public Set<String> getGroupsFor(final String address, EntryType type) {
        notNull(address);
        notNull(type);
        Set<String> groups = newLinkedHashSet();
        for (AddeEntry entry : trie.getPrefixedBy(address+'!').values()) {
            if (entry.getAddress().equals(address) && entry.getEntryType() == type) {
                groups.add(entry.getGroup());
            }
        }
        return groups;
    }

    protected List<AddeEntry> searchWithPrefix(final String prefix) {
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
        Set<String> addresses = newLinkedHashSet();
        for (AddeEntry entry : trie.values()) {
            addresses.add(entry.getAddress());
        }
        return addresses;
    }

    protected Set<String> getRemoteEntryTexts() {
        Set<String> strs = newLinkedHashSet();
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
        Set<String> groups = newLinkedHashSet();
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
        Set<EntryType> types = newLinkedHashSet();
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
        List<AddeServer.Group> groups = idvServer.getGroups();
        if (groups != null && !groups.isEmpty()) {
            EntryType type =EntryTransforms.strToEntryType(typeAsStr);
            return getAccountingFor(address, groups.get(0).getName(), type);
        } else {
            return RemoteAddeEntry.DEFAULT_ACCOUNT;
        }
    }

    /**
     * Returns the complete {@link Set} of {@link AddeEntry}s.
     */
    protected Set<AddeEntry> getEntrySet() {
        return newLinkedHashSet(trie.values());
    }

    /**
     * Returns the complete {@link Set} of {@link RemoteAddeEntry}s.
     * 
     * @return The {@code RemoteAddeEntry}s stored within {@link #entries}.
     */
    protected Set<RemoteAddeEntry> getRemoteEntries() {
        Set<RemoteAddeEntry> remotes = newLinkedHashSet();
        for (AddeEntry e : trie.values()) {
            if (e instanceof RemoteAddeEntry)
                remotes.add((RemoteAddeEntry)e);
        }
        return remotes;
    }

    /**
     * Returns the complete {@link Set} of {@link LocalAddeEntry}s.
     * 
     * @return The {@code LocalAddeEntry}s stored within {@link #entries}.
     */
    protected Set<LocalAddeEntry> getLocalEntries() {
        Set<LocalAddeEntry> locals = newLinkedHashSet();
        for (AddeEntry e : trie.getPrefixedBy("localhost").values()) {
            if (e instanceof LocalAddeEntry)
                locals.add((LocalAddeEntry)e);
        }
        return locals;
    }

    protected boolean removeEntries(
        final Collection<? extends AddeEntry> removedEntries) 
    {
        notNull(removedEntries);

        boolean val = true;
        for (AddeEntry entry : removedEntries) {
            if (val) {
                val = (trie.remove(entry.asStringId()) != null);
            }
        }
        Event evt = (val) ? Event.REMOVAL : Event.FAILURE; 
        saveEntries();
        EventBus.publish(evt);
        return val;
    }

    protected boolean removeEntry(final AddeEntry entry) {
        notNull(entry);
        boolean val = (trie.remove(entry.asStringId()) != null);
        Event evt = (val) ? Event.REMOVAL : Event.FAILURE;
        saveEntries();
        EventBus.publish(evt);
        return val;
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
        Set<AddeServer.Group> idvGroups = newLinkedHashSet();
        for (LocalAddeEntry entry : getLocalEntries()) {
            if (entry.getEntryStatus() == EntryStatus.ENABLED && entry.getEntryValidity() == EntryValidity.VERIFIED) {
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
        Set<AddeServer.Group> idvGroups = newLinkedHashSet();
        String typeStr = type.name();
        for (AddeEntry matched : trie.getPrefixedBy(server).values()) {
            if (matched == RemoteAddeEntry.INVALID_ENTRY) {
                continue;
            }

            if (matched.getEntryStatus() == EntryStatus.ENABLED && matched.getEntryValidity() == EntryValidity.VERIFIED && matched.getEntryType() == type) {
                String group = matched.getGroup();
                idvGroups.add(new AddeServer.Group(typeStr, group, group));
            }
        }
        return idvGroups;
    }

    public List<AddeServer> getIdvStyleEntries() {
        return arrList(EntryTransforms.convertMcvServers(getEntrySet()));
    }

    public Set<AddeServer> getIdvStyleEntries(final EntryType type) {
        return EntryTransforms.convertMcvServers(getVerifiedEntries(type));
    }

    public Set<AddeServer> getIdvStyleEntries(final String typeAsStr) {
        return getIdvStyleEntries(EntryTransforms.strToEntryType(typeAsStr));
    }

    /**
     * Process all of the {@literal "IDV-style"} XML resources.
     * 
     * @param source
     * @param xmlResources
     * 
     * @return
     */
    private Set<AddeEntry> extractResourceEntries(EntrySource source, final XmlResourceCollection xmlResources) {
        Set<AddeEntry> entries = newLinkedHashSet();

        for (int i = 0; i < xmlResources.size(); i++) {
            Element root = xmlResources.getRoot(i);
            if (root == null) {
                continue;
            }

            Set<AddeEntry> woot = EntryTransforms.convertAddeServerXml(root, source);
            entries.addAll(woot);
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
        Set<AddeEntry> entries = newLinkedHashSet();
        for (int i = 0; i < xmlResources.size(); i++) {
            Element root = xmlResources.getRoot(i);
            if (root == null) {
                continue;
            }

            entries.addAll(EntryTransforms.convertUserXml(root));
            //            for (RemoteAddeEntry e : entries) {
            //                System.err.println(e);
            //            }
        }

        return entries;
    }

    // allow for the user to specify a "debug.localadde.rootdir" System property;
    public static String getAddeRootDirectory() {
        if (System.getProperties().containsKey(PROP_DEBUG_LOCALROOT)) {
            return System.getProperty(PROP_DEBUG_LOCALROOT);
        }
        return System.getProperty("user.dir") + File.separatorChar + "adde";
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
     * Ask for the port we are listening on
     */
    public static String getLocalPort() {
        return localPort;
    }

    /**
     * Get the next port by incrementing current port.
     */
    protected static String nextLocalPort() {
        return Integer.toString(Integer.parseInt(localPort) + 1);
    }

    /**
     * start addeMcservl if it exists
     */
    public void startLocalServer(final boolean restarting) {
        if ((new File(ADDE_MCSERVL)).exists()) {
            // Create and start the thread if there isn't already one running
            if (!checkLocalServer()) {
                thread = new AddeThread(this);
                thread.start();
                EventBus.publish(McservEvent.STARTED);
            }
        }
    }

    /**
     * stop the thread if it is running
     */
    public void stopLocalServer(final boolean restarting) {
        if (checkLocalServer()) {
            //TODO: stopProcess (actually Process.destroy()) hangs on Macs...
            //      doesn't seem to kill the children properly
            if (!McIDASV.isMac()) {
                thread.stopProcess();
            }

            thread.interrupt();
            thread = null;
            if (!restarting) {
                EventBus.publish(McservEvent.STOPPED);
            }
        }
    }

    /**
     * restart the thread
     */
    synchronized public void restartLocalServer() {
        restartingMcserv = true;
        if (checkLocalServer()) {
            stopLocalServer(restartingMcserv);
        }
        startLocalServer(restartingMcserv);
        restartingMcserv = false;
    }

    synchronized public boolean getRestarting() {
        return restartingMcserv;
    }

    /**
     * check to see if the thread is running
     */
    public boolean checkLocalServer() {
        if (thread != null && thread.isAlive()) {
            return true;
        } else {
            return false;
        }
    }
}
