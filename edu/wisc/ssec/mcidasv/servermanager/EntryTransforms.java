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

import static ucar.unidata.xml.XmlUtil.findChildren;
import static ucar.unidata.xml.XmlUtil.getAttribute;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.map;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.idv.chooser.adde.AddeServer.Group;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.StringUtil;

import edu.wisc.ssec.mcidasv.ResourceManager;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntrySource;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryStatus;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryValidity;
import edu.wisc.ssec.mcidasv.util.Contract;
import edu.wisc.ssec.mcidasv.util.functional.Function;

// useful methods for doing things like converting a "AddeServer" to a "RemoteAddeEntry"
// and so on.
public class EntryTransforms {

    /** Matches dataset routing information in a MCTABLE file. */
    private static final Pattern routePattern = 
        Pattern.compile("^ADDE_ROUTE_(.*)=(.*)$");

    /** Matches {@literal "host"} declarations in a MCTABLE file. */
    private static final Pattern hostPattern = 
        Pattern.compile("^HOST_(.*)=(.*)$");

    /** No sense in rebuilding things that don't need to be rebuilt. */
    private static final Matcher routeMatcher = routePattern.matcher("");

    /** No sense in rebuilding things that don't need to be rebuilt. */
    private static final Matcher hostMatcher = hostPattern.matcher("");

    private EntryTransforms() { }

    public static final Function<AddeServer, RemoteAddeEntry> convertIdvServer = new Function<AddeServer, RemoteAddeEntry>() {
        public RemoteAddeEntry apply(final AddeServer arg) {
            String hostname = arg.toString().toLowerCase();
            for (AddeServer.Group group : (List<AddeServer.Group>)arg.getGroups()) {
                
            }
            return new RemoteAddeEntry.Builder(hostname, "temp").build();
        }
    };

    // converts a list of AddeServers to a set of RemoteAddeEntry
    public static Set<RemoteAddeEntry> convertIdvServers(final List<AddeServer> idvServers) {
        Set<RemoteAddeEntry> addeEntries = newLinkedHashSet();
        addeEntries.addAll(map(convertIdvServer, idvServers));
        return addeEntries;
    }

    public static List<AddeServer> convertMcvServers(final Collection<RemoteAddeEntry> entries) {
        Set<AddeServer> addeServs = newLinkedHashSet();
        Set<String> addrs = newLinkedHashSet();
        for (RemoteAddeEntry e : entries) {
            String addr = e.getAddress();
            if (addrs.contains(addr))
                continue;

            String newGroup = e.getGroup();
            String type = entryTypeToStr(e.getEntryType());
            AddeServer addeServ = new AddeServer(addr);
            Group addeGroup = new Group(type, newGroup, newGroup);
            addeServ.addGroup(addeGroup);
            addeServs.add(addeServ);
            addrs.add(addr);
        }
        return arrList(addeServs);
    }

    /**
     * Converts the XML contents of {@link ResourceManager#RSC_NEW_USERSERVERS}
     * to a {@link Set} of {@link RemoteAddeEntry}s.
     * 
     * @param root {@literal "Root"} of the XML to convert.
     * 
     * @return {@code Set} of {@code RemoteAddeEntry}s described by 
     * {@code root}.
     */
    protected static Set<RemoteAddeEntry> convertUserXml(final Element root) {
        Set<RemoteAddeEntry> entries = newLinkedHashSet();
        // <entry name="SERVER/DATASET" user="ASDF" proj="0000" source="user" enabled="true" type="image"/>
        List<Element> elements = findChildren(root, "entry");
        for (Element entryXml : elements) {
            String name = getAttribute(entryXml, "name");
            String user = getAttribute(entryXml, "user");
            String proj = getAttribute(entryXml, "proj");
            String source = getAttribute(entryXml, "source");
            String type = getAttribute(entryXml, "type");

            boolean enabled = Boolean.parseBoolean(getAttribute(entryXml, "enabled"));

            EntryType entryType = strToEntryType(type);
            EntryStatus entryStatus = (enabled == true) ? EntryStatus.ENABLED : EntryStatus.DISABLED; 
            EntrySource entrySource = strToEntrySource(source);

            if (name != null) {
                String[] arr = name.split("/");
                String description = arr[0];
                if (arr[0].toLowerCase().contains("localhost")) {
                    description = "<LOCAL-DATA>";
                }

                RemoteAddeEntry.Builder incomplete = 
                    new RemoteAddeEntry.Builder(arr[0], arr[1])
                        .type(entryType)
                        .status(entryStatus)
                        .source(entrySource)
                        .validity(EntryValidity.VERIFIED)
                        .description(description);
//                System.err.println("AWWWOOOOOGAAA: remove that .validity(EntryValidity.VERIFIED) junk!");
                if (((user != null) && (proj != null)) && ((user.length() > 0) && (proj.length() > 0)))
                    incomplete = incomplete.account(user, proj);

                entries.add(incomplete.build());
            }
        }

        return entries;
    }

    /**
     * Converts the XML contents of {@link IdvResourceManager#RSC_ADDESERVER} 
     * to a {@link Set} of {@link RemoteAddeEntry}s.
     * 
     * @param root XML to convert.
     * @param source Used to {@literal "bulk set"} the origin of whatever
     * {@code RemoteAddeEntry}s get created.
     * 
     * @return {@code Set} of {@code RemoteAddeEntry}s contained within 
     * {@code root}.
     */
    @SuppressWarnings("unchecked")
    protected static Set<RemoteAddeEntry> convertAddeServerXml(Element root, EntrySource source) {
        Set<RemoteAddeEntry> es = newLinkedHashSet();

        List<Element> serverNodes = findChildren(root, "server");
        for (int i = 0; i < serverNodes.size(); i++) {
            Element element = (Element)serverNodes.get(i);
            String address = getAttribute(element, "name");
            String description = getAttribute(element, "description", "");

            // loop through each "group" entry.
            List<Element> groupNodes = findChildren(element, "group");
            for (int j = 0; j < groupNodes.size(); j++) {
                Element group = (Element)groupNodes.get(j);

                // convert whatever came out of the "type" attribute into a 
                // valid EntryType.
                String strType = getAttribute(group, "type");
                EntryType type = strToEntryType(strType);

                // the "names" attribute can contain comma-delimited group
                // names.
                List<String> names = StringUtil.split(getAttribute(group, "names", ""), ",", true, true);
                for (String name : names) {
                    if (name.length() == 0)
                        continue;

                    RemoteAddeEntry e =  new RemoteAddeEntry
                                            .Builder(address, name)
                                            .source(source)
                                            .type(type)
                                            .description(description)
                                            .build();
                    es.add(e);
                }

                // there's also an optional "name" attribute! woo!
                String name = getAttribute(group, "name", (String) null);
                if ((name != null) && (name.length() > 0)) {

                    RemoteAddeEntry e = new RemoteAddeEntry
                                            .Builder(address, name)
                                            .source(source)
                                            .description(description)
                                            .build();
                    es.add(e);
                }

                // anything else?
            }
        }
        return es;
    }

    public static String entryTypeToStr(final EntryType type) {
        Contract.notNull(type);
        return type.toString().toLowerCase();
    }

    /**
     * Attempts to convert a {@link String} to a {@link EntryType}.
     * 
     * @param s Value whose {@code EntryType} is wanted. Cannot be {@code null}.
     * 
     * @return One of {@code EntryType}. If there was no {@literal "sensible"}
     * conversion, the method returns {@link EntryType#UNKNOWN}.
     * 
     * @throws NullPointerException if {@code s} is {@code null}.
     */
    public static EntryType strToEntryType(final String s) {
        EntryType type = EntryType.UNKNOWN;
        Contract.notNull(s);
        try {
            type = EntryType.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            // TODO: anything to do in this situation?
        }
        return type;
    }

    /**
     * Attempts to convert a {@link String} to an {@link EntrySource}.
     * 
     * @param s {@code String} representation of an {@code EntrySource}. 
     * Cannot be {@code null}.
     * 
     * @return Uses {@link EntrySource#valueOf(String)} to convert {@code s}
     * to an {@code EntrySource} and returns. If no conversion was possible, 
     * returns {@link EntrySource#USER}.
     * 
     * @throws NullPointerException if {@code s} is {@code null}.
     */
    public static EntrySource strToEntrySource(final String s) {
        EntrySource source = EntrySource.USER;
        Contract.notNull(s);
        try {
            source = EntrySource.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            // TODO: anything to do in this situation?
        }
        return source;
    }

    /**
     * Attempts to convert a {@link String} to an {@link EntryValidity}.
     * 
     * @param s {@code String} representation of an {@code EntryValidity}. 
     * Cannot be {@code null}.
     * 
     * @return Uses {@link EntryValidity#valueOf(String)} to convert 
     * {@code s} to an {@code EntryValidity} and returns. If no conversion 
     * was possible, returns {@link EntryValidity#UNVERIFIED}.
     * 
     * @throws NullPointerException if {@code s} is {@code null}.
     */
    public static EntryValidity strToEntryValidity(final String s) {
        EntryValidity valid = EntryValidity.UNVERIFIED;
        Contract.notNull(s);
        try {
            valid = EntryValidity.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            // TODO: anything to do in this situation?
        }
        return valid;
    }

    /**
     * Attempts to convert a {@link String} to an {@link EntryStatus}.
     * 
     * @param s {@code String} representation of an {@code EntryStatus}. 
     * Cannot be {@code null}.
     * 
     * @return Uses {@link EntryStatus#valueOf(String)} to convert {@code s}
     * to an {@code EntryStatus} and returns. If no conversion was possible, 
     * returns {@link EntryStatus#DISABLED}.
     * 
     * @throws NullPointerException if {@code s} is {@code null}.
     */
    public static EntryStatus strToEntryStatus(final String s) {
        EntryStatus status = EntryStatus.DISABLED;
        Contract.notNull(s);
        try {
            status = EntryStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            // TODO: anything to do in this situation?
        }
        return status;
    }

    // TODO(jon): re-add verify flag?
    protected static Set<RemoteAddeEntry> extractMctableEntries(final String path) {
        Set<RemoteAddeEntry> entries = newLinkedHashSet();

        try {
            InputStream is = IOUtil.getInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;

            Map<String, Set<String>> hosts = newMap();
            Map<String, String> hostToIp = newMap();
            Map<String, String> datasetToHost = newMap();

            // special case for an local ADDE entries.
            Set<String> blah = newLinkedHashSet();
            blah.add("LOCAL-DATA");
            hosts.put("LOCAL-DATA", blah);
            hostToIp.put("LOCAL-DATA", "LOCAL-DATA");

            while ((line = reader.readLine()) != null) {
                routeMatcher.reset(line);
                hostMatcher.reset(line);

                if (routeMatcher.find()) {
                    String dataset = routeMatcher.group(1);
                    String host = routeMatcher.group(2).toLowerCase();
                    datasetToHost.put(dataset, host);
                }
                else if (hostMatcher.find()) {
                    String name = hostMatcher.group(1).toLowerCase();
                    String ip = hostMatcher.group(2);

                    Set<String> nameSet = hosts.get(ip);
                    if (nameSet == null)
                        nameSet = newLinkedHashSet();

                    nameSet.add(name);
                    hosts.put(ip, nameSet);

                    hostToIp.put(name, ip);
                    hostToIp.put(ip, ip); // HACK :(
                }
            }

            Map<String, String> datasetsToIp = mapDatasetsToIp(datasetToHost, hostToIp);
            Map<String, String> ipToName = mapIpToName(hosts);
            List<RemoteAddeEntry> l = mapDatasetsToName(datasetsToIp, ipToName);
            entries.addAll(l);
            is.close();
        } catch (IOException e) {
            LogUtil.logException("Reading file: "+path, e);
        }

        return entries;
    }

    /**
     * This method is slightly confusing, sorry! Think of it kind of like a
     * {@literal "SQL JOIN"}... 
     * 
     * <p>Basically create {@link RemoteAddeEntry}s by using a hostname to
     * determine which dataset belongs to which IP.
     * 
     * @param datasetToHost {@code Map} of ADDE groups to host names.
     * @param hostToIp {@code Map} of host names to IP addresses.
     * 
     * @return
     */
    private static List<RemoteAddeEntry> mapDatasetsToName(
        final Map<String, String> datasetToHost, final Map<String, String> hostToIp) 
    {
        List<RemoteAddeEntry> entries = arrList();
        for(Entry<String, String> entry : datasetToHost.entrySet()) {
            String dataset = entry.getKey();
            String ip = entry.getValue();
            String name = ip;
            if (hostToIp.containsKey(ip))
                name = hostToIp.get(ip);

            RemoteAddeEntry e = new RemoteAddeEntry.Builder(name, dataset)
                                    .source(EntrySource.MCTABLE).build();
            entries.add(e);
        }
        return entries;
    }

    private static Map<String, String> mapIpToName(
        final Map<String, Set<String>> map) 
    {
        assert map != null;

        Map<String, String> ipToName = newMap();
        for (Entry<String, Set<String>> entry : map.entrySet()) {
            Set<String> names = entry.getValue();
            String displayName = "";
            for (String name : names)
                if (name.length() >= displayName.length())
                    displayName = name;

            if (displayName.equals(""))
                displayName = entry.getKey();

            ipToName.put(entry.getKey(), displayName);
        }
        return ipToName;
    }

    private static Map<String, String> mapDatasetsToIp(final Map<String, String> datasets, final Map<String, String> hostMap) {
        assert datasets != null;
        assert hostMap != null;

        Map<String, String> datasetToIp = newMap();
        for (Entry<String, String> entry : datasets.entrySet()) {
            String dataset = entry.getKey();
            String alias = entry.getValue();
            if (hostMap.containsKey(alias))
                datasetToIp.put(dataset, hostMap.get(alias));
        }
        return datasetToIp;
    }
}
