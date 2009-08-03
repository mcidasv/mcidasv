/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
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

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IdvResourceManager.IdvResource;
import ucar.unidata.idv.chooser.adde.AddeServer;
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

    private static final String PREF_REMOTE_ADDE_ENTRIES = "mcv.servers.entries";

    /** The ADDE servers known to McIDAS-V. */
    private final InternalStore entries = new InternalStore();

    /** Object that's running the show */
    private final McIDASV mcv;

    public EntryStore(final McIDASV mcv) {
        Contract.notNull(mcv);
        this.mcv = mcv;

        entries.putEntries(extractFromPreferences());
        entries.putEntries(extractUserEntries(ResourceManager.RSC_NEW_USERSERVERS));
        entries.putEntries(extractResourceEntries(EntrySource.SYSTEM, IdvResourceManager.RSC_ADDESERVER));

        dumbTest();
    }

    private void dumbTest() {
        for (String addr : entries.getAddresses()) {
            boolean addrSeen = false;
            for (String group : entries.getGroups(addr)) {
                if (!addrSeen) {
                    System.err.println(addr+"\t\t"+group+"\t\t"+entries.getTypes(addr, group));
                    addrSeen = true;
                } else {
                    System.err.println("\t\t\t"+group+"\t\t"+entries.getTypes(addr, group));
                }
            }
            System.err.println("------------------------------------");
        }
        entries.dumb();
    }
    

    /**
     * 
     * 
     * @return
     */
    private Set<RemoteAddeEntry> extractFromPreferences() {
        Set<RemoteAddeEntry> entries = newLinkedHashSet();

        // this is valid--the only thing ever written to 
        // PREF_REMOTE_ADDE_ENTRIES is an ArrayList of RemoteAddeEntry objects.
        @SuppressWarnings("unchecked")
        List<RemoteAddeEntry> asList = 
            (List<RemoteAddeEntry>)mcv.getStore().get(PREF_REMOTE_ADDE_ENTRIES);
        if (asList != null)
            entries.addAll(asList);

        return entries;
    }

    /**
     * Saves the current set of remote ADDE servers to the user's preferences.
     */
    public void saveEntries() {
        mcv.getStore().put(PREF_REMOTE_ADDE_ENTRIES, entries.asList());
        mcv.getStore().saveIfNeeded();
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
        for (RemoteAddeEntry entry : entries.asSet()) {
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
//            System.err.println("storing type="+type);
        }

        for (RemoteAddeEntry entry : entries.asSet()) {
            Set<RemoteAddeEntry> entrySet = entryMap.get(entry.getEntryType());
            entrySet.add(entry);
//            System.err.println("  boo: "+entry);
//            if (entry.getEntryValidity() == EntryValidity.VERIFIED) {
//                Set<RemoteAddeEntry> entrySet = entryMap.get(entry.getEntryType());
//                entrySet.add(entry);
//            }
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
        for (RemoteAddeEntry entry : entries.asSet()) {
            if (entry.getAddress().equals(address) && entry.getEntryType() == type)
                groups.add(entry.getGroup());
        }
        return groups;
    }

    /**
     * Returns the {@link Set} of {@link RemoteAddeEntry} addresses stored
     * in this {@code EntryStore}.
     * 
     * @return {@code Set} containing all of the stored addresses. If no 
     * addresses are stored, an empty {@code Set} is returned.
     */
    public Set<String> getAddresses() {
        return entries.getAddresses();
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
        return entries.getGroups(address);
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
        return entries.getTypes(address, group);
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
        for (RemoteAddeEntry e : entries.asSet()) {
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
        return entries.asSet();
    }

    protected void removeEntry(final RemoteAddeEntry entry) {
        if (entry == null)
            throw new NullPointerException("");
        entries.remove(entry);
    }

    /**
     * Adds a {@link Set} of {@link RemoteAddeEntry}s to {@link #entries}.
     * 
     * @param newEntries New entries to add to the server manager. Cannot be
     * {@code null}.
     * 
     * @throws NullPointerException if {@code newEntries} is {@code null}.
     */
    public void addEntries(final Set<RemoteAddeEntry> oldEntries, final Set<RemoteAddeEntry> newEntries) {
        if (oldEntries == null)
            throw new NullPointerException("Cannot replace a null set");
        if (newEntries == null)
            throw new NullPointerException("Cannot add a null set");

        entries.removeEntries(oldEntries);
        entries.putEntries(newEntries);
    }

    // used for apply/ok?
    public void replaceEntries(final EntryStatus typeToReplace, final Set<RemoteAddeEntry> newEntries) {
    }

    /**
     * Process all of the {@literal "IDV-style"} XML resources.
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
//            for (RemoteAddeEntry e : entries) {
//                System.err.println(e);
//            }
        }

        return entries;
    }

    private static class InternalStore {
        // "server1": { "group1":[img, text], "group2":[point]}
        // this thing is really brittle and annoying!
        private final Map<String, Map<String, Set<EntryType>>> entryMap = new HashMap<String, Map<String, Set<EntryType>>>();

        private final Set<RemoteAddeEntry> entrySet = newLinkedHashSet();

        private final Addresses addrs = new Addresses();
        
        protected InternalStore() {}

        protected void dumb() {
//            addrs.getGroupsFor("adde.ucar.edu").removeGroup("CIMSS");
//            addrs.getGroupsFor("satepsanone.nesdis.noaa.gov").getTypesFor("PUB").removeType(EntryType.POINT);
//            addrs.removeAddress("stratus.al.noaa.gov");
//            for (String addr : addrs.getAddresses()) {
//                System.err.println(addr);
//                Groups groups = addrs.getGroupsFor(addr);
//                for (String group : groups.getGroups()) {
//                    Types types = groups.getTypesFor(group);
//                    System.err.print("    "+group+": ");
//                    for (EntryType type : types.getTypes()) {
//                        System.err.print(type+" ");
//                    }
//                    System.err.println();
//                }
//            }
//            System.err.println("*******************");
//            
        }

        protected void remove(final RemoteAddeEntry e) {
            
        }

        protected void removeEntries(final Set<RemoteAddeEntry> es) {
            
        }
        
//        protected RemoteAddeEntry remove(final RemoteAddeEntry e) {
//            if (!entrySet.contains(e))
//                return null; // UHM WTF ARE YOU DOING WITH THIS
//            
//            String addr = e.getAddress();
//            String group = e.getGroup();
//            EntryType type = e.getEntryType();
//            
//            if (entryMap.containsKey(addr)) {
//                Map<String, Set<EntryType>> groupMap = entryMap.get(addr);
//                if (groupMap.containsKey(group)) {
//                    groupMap.get(group).remove(type);
//                    groupMap.
//                }
//                
//            }
//        }
//
//        protected void removeEntries(final Set<RemoteAddeEntry> es) {
//            for (RemoteAddeEntry e : es) {
//                entrySet.remove(e);
//                remove(e);
//            }
//        }

        protected void putEntries(final Set<RemoteAddeEntry> es) {
            for (RemoteAddeEntry e : es) 
                putEntry(e);
        }

        protected void putEntry(final RemoteAddeEntry e) {
            entrySet.add(e);
            addrs.addAddress(e);

            String addr = e.getAddress();
            String group = e.getGroup();
            EntryType type = e.getEntryType();

            if (!entryMap.containsKey(addr))
                entryMap.put(addr, new HashMap<String, Set<EntryType>>());

            Map<String, Set<EntryType>> groupMap = entryMap.get(addr);
            if (!groupMap.containsKey(group))
                groupMap.put(group, new LinkedHashSet<EntryType>());

            Set<EntryType> types = groupMap.get(group);
            types.add(type);
        }

        protected Set<String> getAddresses() {
            return new LinkedHashSet<String>(entryMap.keySet());
        }

        protected Set<String> getGroups(final String address) {
            Set<String> groups = newLinkedHashSet();
            if (entryMap.containsKey(address))
                groups.addAll(entryMap.get(address).keySet());
            return groups;
        }

        protected Set<EntryType> getTypes(final String address, final String group) {
            Set<EntryType> types = newLinkedHashSet();
            if (entryMap.containsKey(address)) {
                Map<String, Set<EntryType>> groupMap = entryMap.get(address);
                if (groupMap.containsKey(group)) {
                    types.addAll(groupMap.get(group));
                }
            }
            return types;
        }

        protected List<RemoteAddeEntry> asList() {
            return new ArrayList<RemoteAddeEntry>(entrySet);
        }

        protected Set<RemoteAddeEntry> asSet() {
            return new LinkedHashSet<RemoteAddeEntry>(entrySet);
        }

        private static class Addresses {
            private final Map<String, Groups> addressesToGroups = new HashMap<String, Groups>();
            
            protected void addAddress(final RemoteAddeEntry e) {
                String addr = e.getAddress();
                if (!addressesToGroups.containsKey(addr)) {
                    addressesToGroups.put(addr, new Groups());
                }

                Groups groups = addressesToGroups.get(addr);
                groups.addGroup(e);
            }

            protected Groups getGroupsFor(final RemoteAddeEntry e) {
                return getGroupsFor(e.getAddress());
            }

            protected Groups getGroupsFor(final String addr) {
                if (!addressesToGroups.containsKey(addr))
                    return new Groups();
                return addressesToGroups.get(addr);
            }

            protected Set<String> getAddresses() {
                return addressesToGroups.keySet();
            }
            
            protected void removeAddress(final String addr) {
                addressesToGroups.remove(addr);
            }

            public String toString() {
                return String.format("[Addresses@%x: addressesToGroups=%s]", hashCode(), addressesToGroups);
            }
        }

        private static class Groups {
            private final Map<String, Types> groupsToTypes = new HashMap<String, Types>();

            protected void addGroup(final RemoteAddeEntry e) {
                String group = e.getGroup();
                if (!groupsToTypes.containsKey(group)) {
                    groupsToTypes.put(group, new Types());
                }

                Types types = groupsToTypes.get(group);
                types.addType(e);
            }

            protected Types getTypesFor(final RemoteAddeEntry e) {
                return getTypesFor(e.getGroup());
            }

            protected Types getTypesFor(final String group) {
                if (!groupsToTypes.containsKey(group))
                    return new Types();
                return groupsToTypes.get(group);
            }

            protected Set<String> getGroups() {
                return groupsToTypes.keySet();
            }

            protected void removeGroup(final String group) {
                groupsToTypes.remove(group);
            }
            
            public String toString() {
                return String.format("[Groups@%x: groupsToTypes=%s]", hashCode(), groupsToTypes);
            }
        }

        private static class Types {
            private final Map<EntryType, RemoteAddeEntry> typesToEntries = new HashMap<EntryType, RemoteAddeEntry>();

            protected void addType(final RemoteAddeEntry e) {
                EntryType type = e.getEntryType();
                if (!typesToEntries.containsKey(type)) {
                    typesToEntries.put(type, e);
                }
            }

            protected Set<EntryType> getTypes() {
                return typesToEntries.keySet();
            }

            protected RemoteAddeEntry getEntryFor(final RemoteAddeEntry e) {
                return getEntryFor(e.getEntryType());
            }

            protected RemoteAddeEntry getEntryFor(final EntryType t) {
                if (!typesToEntries.containsKey(t))
                    return null; // ERR!
                return typesToEntries.get(t);
            }

            protected void removeType(final EntryType t) {
                typesToEntries.remove(t);
            }

            public String toString() {
                return String.format("[Types@%x: typesToEntries=%s]", hashCode(), typesToEntries);
            }
        }
    }
}
