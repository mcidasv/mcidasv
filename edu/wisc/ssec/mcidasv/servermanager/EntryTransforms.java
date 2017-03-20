/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2017
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

import static java.util.Objects.requireNonNull;

import static ucar.unidata.xml.XmlUtil.findChildren;
import static ucar.unidata.xml.XmlUtil.getAttribute;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.map;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Element;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.idv.chooser.adde.AddeServer.Group;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.ResourceManager;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntrySource;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryStatus;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryValidity;
import edu.wisc.ssec.mcidasv.servermanager.LocalAddeEntry.AddeFormat;
import edu.wisc.ssec.mcidasv.servermanager.LocalAddeEntry.ServerName;
import edu.wisc.ssec.mcidasv.util.functional.Function;

/**
 * Useful methods for doing things like converting a {@link AddeServer} to a
 * {@link RemoteAddeEntry}.
 */
public class EntryTransforms {

    /** Logger object. */
    private static final Logger logger = LoggerFactory.getLogger(EntryTransforms.class);

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

    // TODO(jon): plz to be removing these
    private static final String cygwinPrefix = "/cygdrive/";
    private static final int cygwinPrefixLength = cygwinPrefix.length();

    /** This is a utility class. Don't create it! */
    private EntryTransforms() { }

    /**
     * {@link Function} that transforms an {@link AddeServer} into a
     * {@link RemoteAddeEntry}.
     */
    // TODO(jon): shouldn't this use AddeEntry rather than RemoteAddeEntry?
    public static final Function<AddeServer, RemoteAddeEntry> convertIdvServer =
        arg -> {
            String hostname = arg.toString().toLowerCase();
//            for (Group ignored : (List<Group>)arg.getGroups()) {
//
//            }
            return new RemoteAddeEntry.Builder(hostname, "temp").build();
        };

    @SuppressWarnings({"SetReplaceableByEnumSet"})
    public static Set<EntryType> findEntryTypes(
        final Collection<? extends AddeEntry> entries)
    {
        Set<EntryType> types = new HashSet<>(entries.size());
        types.addAll(entries.stream()
                            .map(AddeEntry::getEntryType)
                            .collect(Collectors.toList()));
        return EnumSet.copyOf(types);
    }

    // converts a list of AddeServers to a set of RemoteAddeEntry

    /**
     * Converts given {@code idvServers} to a
     * {@link RemoteAddeEntry RemoteAddeEntries}.
     *
     * @param idvServers {@literal "IDV-style"} ADDE servers to convert.
     *
     * @return {@code Set} of remote ADDE entries that corresponds to the unique
     * objects in {@code idvServers}.
     */
    public static Set<RemoteAddeEntry> convertIdvServers(final List<AddeServer> idvServers) {
        Set<RemoteAddeEntry> addeEntries = newLinkedHashSet(idvServers.size());
        addeEntries.addAll(map(convertIdvServer, idvServers));
        return addeEntries;
    }

    /**
     * Converts given {@link AddeEntry AddeEntries} to
     * {@link AddeServer AddeServers}.
     *
     * @param entries {@literal "McIDAS-V style"} ADDE entries to convert.
     *
     * @return {@code Set} of {@code AddeServer} objects that corresponds to
     * the ones found in {@code entries}.
     */
    public static Set<AddeServer> convertMcvServers(final Collection<AddeEntry> entries) {
        Set<AddeServer> addeServs = newLinkedHashSet(entries.size());
        Set<String> addrs = newLinkedHashSet(entries.size());
        for (AddeEntry e : entries) {
            EntryStatus status = e.getEntryStatus();
            if ((status == EntryStatus.DISABLED) || (status == EntryStatus.INVALID)) {
                continue;
            }
            String addr = e.getAddress();
            if (addrs.contains(addr)) {
                continue;
            }

            String newGroup = e.getGroup();
            String type = entryTypeToStr(e.getEntryType());

            AddeServer addeServ;
            if (e instanceof LocalAddeEntry) {
                addeServ = new AddeServer("localhost:"+EntryStore.getLocalPort(), "<LOCAL-DATA>");
                addeServ.setIsLocal(true);
            } else {
                addeServ = new AddeServer(addr);
            }
            Group addeGroup = new Group(type, newGroup, newGroup);
            addeServ.addGroup(addeGroup);
            addeServs.add(addeServ);
            addrs.add(addr);
        }
        return addeServs;
    }

    /**
     * Converts the XML contents of {@link ResourceManager#RSC_NEW_USERSERVERS}
     * to a {@link Set} of {@link RemoteAddeEntry RemoteAddeEntries}.
     * 
     * @param root {@literal "Root"} of the XML to convert.
     * 
     * @return {@code Set} of remote ADDE entries described by
     * {@code root}.
     */
    protected static Set<RemoteAddeEntry> convertUserXml(final Element root) {
        // <entry name="SERVER/DATASET" user="ASDF" proj="0000" source="user" enabled="true" type="image"/>
        Pattern slashSplit = Pattern.compile("/");
        List<Element> elements = (List<Element>)findChildren(root, "entry");
        Set<RemoteAddeEntry> entries = newLinkedHashSet(elements.size());
        for (Element entryXml : elements) {
            String name = getAttribute(entryXml, "name");
            String user = getAttribute(entryXml, "user");
            String proj = getAttribute(entryXml, "proj");
            String source = getAttribute(entryXml, "source");
            String type = getAttribute(entryXml, "type");

            boolean enabled = Boolean.parseBoolean(getAttribute(entryXml, "enabled"));

            EntryType entryType = strToEntryType(type);
            EntryStatus entryStatus = (enabled) ? EntryStatus.ENABLED : EntryStatus.DISABLED;
            EntrySource entrySource = strToEntrySource(source);

            if (name != null) {
                String[] arr = slashSplit.split(name);
                String description = arr[0];
                if (arr[0].toLowerCase().contains("localhost")) {
                    description = "<LOCAL-DATA>";
                }

                RemoteAddeEntry.Builder incomplete = 
                    new RemoteAddeEntry.Builder(arr[0], arr[1])
                        .type(entryType)
                        .status(entryStatus)
                        .source(entrySource)
                        .validity(EntryValidity.VERIFIED);

                if (((user != null) && (proj != null)) && ((!user.isEmpty()) && (!proj.isEmpty()))) {
                    incomplete = incomplete.account(user, proj);
                }
                entries.add(incomplete.build());
            }
        }
        return entries;
    }

    public static Set<RemoteAddeEntry> createEntriesFrom(final RemoteAddeEntry entry) {
        Set<RemoteAddeEntry> entries = newLinkedHashSet(EntryType.values().length);

        RemoteAddeEntry.Builder incomp =
            new RemoteAddeEntry.Builder(entry.getAddress(), entry.getGroup())
            .account(entry.getAccount().getUsername(), entry.getAccount().getProject())
            .source(entry.getEntrySource()).status(entry.getEntryStatus())
            .validity(entry.getEntryValidity());

        entries.addAll(
            EnumSet.of(
                EntryType.IMAGE, EntryType.GRID, EntryType.POINT,
                EntryType.TEXT, EntryType.RADAR, EntryType.NAV)
            .stream()
            .filter(type -> !(type == entry.getEntryType()))
            .map(type -> incomp.type(type).build())
            .collect(Collectors.toList()));

        logger.trace("built entries={}", entries);
        return entries;
    }

    
    /**
     * Converts the XML contents of {@link IdvResourceManager#RSC_ADDESERVER} 
     * to a {@link Set} of {@link RemoteAddeEntry RemoteAddeEntries}.
     * 
     * @param root XML to convert.
     * @param source Used to {@literal "bulk set"} the origin of whatever
     *               remote ADDE entries get created.
     * 
     * @return {@code Set} of remote ADDE entries contained within {@code root}.
     */
    @SuppressWarnings("unchecked")
    protected static Set<AddeEntry> convertAddeServerXml(Element root, EntrySource source) {
        List<Element> serverNodes = findChildren(root, "server");
        Set<AddeEntry> es = newLinkedHashSet(serverNodes.size() * 5);
        for (int i = 0; i < serverNodes.size(); i++) {
            Element element = serverNodes.get(i);
            String address = getAttribute(element, "name");
            String description = getAttribute(element, "description", "");

            // loop through each "group" entry.
            List<Element> groupNodes = findChildren(element, "group");
            for (int j = 0; j < groupNodes.size(); j++) {
                Element group = groupNodes.get(j);

                // convert whatever came out of the "type" attribute into a 
                // valid EntryType.
                String strType = getAttribute(group, "type");
                EntryType type = strToEntryType(strType);

                // the "names" attribute can contain comma-delimited group
                // names.
                List<String> names = StringUtil.split(getAttribute(group, "names", ""), ",", true, true);
                for (String name : names) {
                    if (name.isEmpty()) {
                        continue;
                    }
                    RemoteAddeEntry e =  new RemoteAddeEntry
                                            .Builder(address, name)
                                            .source(source)
                                            .type(type)
                                            .validity(EntryValidity.VERIFIED)
                                            .status(EntryStatus.ENABLED)
                                            .validity(EntryValidity.VERIFIED)
                                            .status(EntryStatus.ENABLED)
                                            .build();
                    es.add(e);
                }

                // there's also an optional "name" attribute! woo!
                String name = getAttribute(group, "name", (String) null);
                if ((name != null) && !name.isEmpty()) {

                    RemoteAddeEntry e = new RemoteAddeEntry
                                            .Builder(address, name)
                                            .source(source)
                                            .validity(EntryValidity.VERIFIED)
                                            .status(EntryStatus.ENABLED)
                                            .validity(EntryValidity.VERIFIED)
                                            .status(EntryStatus.ENABLED)
                                            .build();
                    es.add(e);
                }
            }
        }
        return es;
    }

    /**
     * Converts a given {@link ServerName} to its {@link String} representation.
     * Note that the resulting {@code String} is lowercase.
     * 
     * @param serverName The server name to convert. Cannot be {@code null}.
     * 
     * @return {@code serverName} converted to a lowercase {@code String}.
     * 
     * @throws NullPointerException if {@code serverName} is {@code null}.
     */
    public static String serverNameToStr(final ServerName serverName) {
        requireNonNull(serverName);
        return serverName.toString().toLowerCase();
    }

    /**
     * Attempts to convert a {@link String} to a {@link ServerName}.
     * 
     * @param s Value whose {@code ServerName} is wanted.
     *          Cannot be {@code null}.
     * 
     * @return One of {@code ServerName}. If there was no {@literal "sensible"}
     * conversion, the method returns {@link ServerName#INVALID}.
     * 
     * @throws NullPointerException if {@code s} is {@code null}.
     */
    public static ServerName strToServerName(final String s) {
        ServerName serverName = ServerName.INVALID;
        requireNonNull(s);
        try {
            serverName = ServerName.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            // TODO: anything to do in this situation?
        }
        return serverName;
    }

    /**
     * Converts a given {@link EntryType} to its {@link String} representation.
     * Note that the resulting {@code String} is lowercase.
     * 
     * @param type The type to convert. Cannot be {@code null}.
     * 
     * @return {@code type} converted to a lowercase {@code String}.
     * 
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    public static String entryTypeToStr(final EntryType type) {
        requireNonNull(type);
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
        requireNonNull(s);
        EntryType type = EntryType.UNKNOWN;
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
        requireNonNull(s);
        EntrySource source = EntrySource.USER;
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
        requireNonNull(s);
        EntryValidity valid = EntryValidity.UNVERIFIED;
        try {
            valid = EntryValidity.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            // TODO: anything to do in this situation?
        }
        return valid;
    }

    /**
     * Attempts to convert a {@link String} into an {@link EntryStatus}.
     * 
     * @param s {@code String} representation of an {@code EntryStatus}. 
     * Cannot be {@code null}.
     * 
     * @return Uses {@link EntryStatus#valueOf(String)} to convert {@code s}
     * into an {@code EntryStatus} and returns. If no conversion was possible, 
     * returns {@link EntryStatus#DISABLED}.
     * 
     * @throws NullPointerException if {@code s} is {@code null}.
     */
    public static EntryStatus strToEntryStatus(final String s) {
        requireNonNull(s);
        EntryStatus status = EntryStatus.DISABLED;
        try {
            status = EntryStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            // TODO: anything to do in this situation?
        }
        return status;
    }

    /**
     * Attempts to convert a {@link String} into a member of {@link AddeFormat}.
     * This method does a little bit of magic with the incoming {@code String}:
     * <ol>
     *   <li>spaces are replaced with underscores</li>
     *   <li>dashes ({@literal "-"}) are removed</li>
     * </ol>
     * This was done because older {@literal "RESOLV.SRV"} files permitted the
     * {@literal "MCV"} key to contain spaces or dashes, and that doesn't play
     * so well with Java's enums.
     * 
     * @param s {@code String} representation of an {@code AddeFormat}. Cannot 
     * be {@code null}.
     * 
     * @return Uses {@link AddeFormat#valueOf(String)} to convert
     * <i>the modified</i> {@code String} into an {@code AddeFormat} and
     * returns. If no conversion was possible, returns
     * {@link AddeFormat#INVALID}.
     * 
     * @throws NullPointerException if {@code s} is {@code null}.
     */
    public static AddeFormat strToAddeFormat(final String s) {
        requireNonNull(s);

        AddeFormat format = AddeFormat.INVALID;
        try {
            format = AddeFormat.valueOf(s.toUpperCase().replace(' ', '_').replace("-", ""));
        } catch (IllegalArgumentException e) {
            // TODO: anything to do in this situation?
        }
        return format;
    }

    public static String addeFormatToStr(final AddeFormat format) {
        requireNonNull(format);

        return format.toString().toLowerCase();
    }

    // TODO(jon): re-add verify flag?
    protected static Set<RemoteAddeEntry> extractMctableEntries(final String path, final String username, final String project) {
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

            boolean validFile = false;
            while ((line = reader.readLine()) != null) {
                routeMatcher.reset(line);
                hostMatcher.reset(line);

                if (routeMatcher.find()) {
                    String dataset = routeMatcher.group(1);
                    String host = routeMatcher.group(2).toLowerCase();
                    datasetToHost.put(dataset, host);
                    validFile = true;
                }
                else if (hostMatcher.find()) {
                    String name = hostMatcher.group(1).toLowerCase();
                    String ip = hostMatcher.group(2);

                    Set<String> nameSet = hosts.get(ip);
                    if (nameSet == null) {
                        nameSet = newLinkedHashSet();
                    }
                    nameSet.add(name);
                    hosts.put(ip, nameSet);
                    hostToIp.put(name, ip);
                    hostToIp.put(ip, ip); // HACK :(
                    validFile = true;
                }
            }

            if (validFile) {
                Map<String, String> datasetsToIp =
                    mapDatasetsToIp(datasetToHost, hostToIp);
                Map<String, String> ipToName = mapIpToName(hosts);
                List<RemoteAddeEntry> l =
                    mapDatasetsToName(datasetsToIp, ipToName, username, project);
                entries.addAll(l);
            } else {
                entries = Collections.emptySet();
            }
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
     * <p>Basically create {@link RemoteAddeEntry RemoteAddeEntries} by using
     * a hostname to determine which dataset belongs to which IP.</p>
     * 
     * @param datasetToHost {@code Map} of ADDE groups to host names.
     * @param hostToIp {@code Map} of host names to IP addresses.
     * @param username ADDE username.
     * @param project ADDE project number (as a {@code String}).
     * 
     * @return {@link List} of {@link RemoteAddeEntry} instances. Each hostname
     * will have a value from {@code datasetToHost} and the accounting information
     * is formed from {@code username} and {@code project}.
     */
    private static List<RemoteAddeEntry> mapDatasetsToName(
        final Map<String, String> datasetToHost,
        final Map<String, String> hostToIp,
        final String username,
        final String project)
    {
        boolean defaultAcct = false;
        AddeAccount defAcct = AddeEntry.DEFAULT_ACCOUNT;
        if (defAcct.getUsername().equalsIgnoreCase(username) && defAcct.getProject().equals(project)) {
            defaultAcct = true;
        }

        List<RemoteAddeEntry> entries = arrList(datasetToHost.size());
        for (Entry<String, String> entry : datasetToHost.entrySet()) {
            String dataset = entry.getKey();
            String ip = entry.getValue();
            String name = ip;

            if (hostToIp.containsKey(ip)) {
                name = hostToIp.get(ip);
            }

            RemoteAddeEntry.Builder builder =
                new RemoteAddeEntry.Builder(name, dataset)
                                   .source(EntrySource.MCTABLE);

            if (!defaultAcct) {
                builder.account(username, project);
            }

            // now go ahead and actually create the new entry
            RemoteAddeEntry remoteEntry = builder.build();
            logger.trace("built entry={}", remoteEntry);
            entries.add(builder.build());
        }
        return entries;
    }

    private static Map<String, String> mapIpToName(
        final Map<String, Set<String>> map) 
    {
        assert map != null;

        Map<String, String> ipToName = newMap(map.size());
        for (Entry<String, Set<String>> entry : map.entrySet()) {
            Set<String> names = entry.getValue();
            String displayName = "";
            for (String name : names) {
                if (name.length() >= displayName.length()) {
                    displayName = name;
                }
            }

            if (displayName.isEmpty()) {
                displayName = entry.getKey();
            }
            ipToName.put(entry.getKey(), displayName);
        }
        return ipToName;
    }

    private static Map<String, String> mapDatasetsToIp(
        final Map<String, String> datasets,
        final Map<String, String> hostMap)
    {
        assert datasets != null;
        assert hostMap != null;

        Map<String, String> datasetToIp = newMap(datasets.size());
        for (Entry<String, String> entry : datasets.entrySet()) {
            String dataset = entry.getKey();
            String alias = entry.getValue();
            if (hostMap.containsKey(alias)) {
                datasetToIp.put(dataset, hostMap.get(alias));
            }
        }
        return datasetToIp;
    }

    /**
     * Reads a {@literal "RESOLV.SRV"} file and converts the contents into a 
     * {@link Set} of {@link LocalAddeEntry LocalAddeEntries}.
     * 
     * @param filename Filename containing desired local ADDE entries.
     * Cannot be {@code null}.
     * 
     * @return {@code Set} of local ADDE entries contained within
     * {@code filename}.
     * 
     * @throws IOException if there was a problem reading from {@code filename}.
     * 
     * @see #readResolvLine(String)
     */
    public static Set<LocalAddeEntry> readResolvFile(final String filename) throws IOException {
        Set<LocalAddeEntry> servers = newLinkedHashSet();
//        BufferedReader br = null;
//        try {
//            br = new BufferedReader(new FileReader(filename));
//            String line;
//            while ((line = br.readLine()) != null) {
//                line = line.trim();
//                if (line.isEmpty()) {
//                    continue;
//                } else if (line.startsWith("SSH_")) {
//                    continue;
//                }
//                servers.add(readResolvLine(line));
//            }
//        } finally {
//            if (br != null) {
//                br.close();
//            }
//        }
        try (Stream<String> stream = Files.lines(Paths.get(filename))) {
//            stream.forEach(line -> {
//                line = line.trim();
//                if (!line.isEmpty() && !line.startsWith("SSH_")) {
//                    servers.add(readResolvLine(line));
//                }
//            })
            servers.addAll(
                stream.map(String::trim)
                      .filter(l -> !l.isEmpty() && !l.startsWith("SSH_"))
                      .map(EntryTransforms::readResolvLine)
                      .collect(Collectors.toSet()));
        }
        return servers;
    }

    /**
     * Converts a {@code String} containing a {@literal "RESOLV.SRV"} entry
     * into a {@link LocalAddeEntry}.
     *
     * @param line Line from {@code RESOLV.SRV}.
     *
     * @return {@code LocalAddeEntry} that represents the given {@code line}
     * from {@code RESOLV.SRV}.
     */
    public static LocalAddeEntry readResolvLine(String line) {
        boolean disabled = line.startsWith("#");
        if (disabled) {
            line = line.substring(1);
        }
        Pattern commaSplit = Pattern.compile(",");
        Pattern equalSplit = Pattern.compile("=");

        String[] pairs = commaSplit.split(line.trim());
        String[] pair;
        Map<String, String> keyVals = new HashMap<>(pairs.length);
        for (String tempPair : pairs) {
            if ((tempPair == null) || tempPair.isEmpty()) {
                continue;
            }

            pair = equalSplit.split(tempPair);
            if ((pair.length != 2) || pair[0].isEmpty() || pair[1].isEmpty()) {
                continue;
            }

            // group
//            if ("N1".equals(pair[0])) {
////                builder.group(pair[1]);
//            }
//            // descriptor/dataset
//            else if ("N2".equals(pair[0])) {
////                builder.descriptor(pair[1]);
//            }
//            // data type (only image supported?)
//            else if ("TYPE".equals(pair[0])) {
////                builder.type(strToEntryType(pair[1]));
//            }
//            // file format
//            else if ("K".equals(pair[0])) {
////                builder.kind(pair[1].toUpperCase());
//            }
//            // comment
//            else if ("C".equals(pair[0])) {
////                builder.name(pair[1]);
//            }
//            // mcv-specific; allows us to infer kind+type?
//            else if ("MCV".equals(pair[0])) {
////                builder.format(strToAddeFormat(pair[1]));
//            }
//            // realtime ("Y"/"N"/"A")
//            else if ("RT".equals(pair[0])) {
////                builder.realtime(pair[1]);
//            }
//            // start of file number range
//            else if ("R1".equals(pair[0])) {
////                builder.start(pair[1]);
//            }
//            // end of file number range
//            else if ("R2".equals(pair[0])) {
////                builder.end(pair[1]);
//            }
//            // filename mask
            if ("MASK".equals(pair[0])) {
                pair[1] = demungeFileMask(pair[1]);
            }
            keyVals.put(pair[0], pair[1]);
        }

        if (keyVals.containsKey("C") && keyVals.containsKey("N1") && keyVals.containsKey("MCV") && keyVals.containsKey("MASK")) {
            LocalAddeEntry entry = new LocalAddeEntry.Builder(keyVals).build();
            EntryStatus status = disabled ? EntryStatus.DISABLED : EntryStatus.ENABLED;
            entry.setEntryStatus(status);
            return entry;
        } else {
            return LocalAddeEntry.INVALID_ENTRY;
        }
    }

    /**
     * Writes a {@link Collection} of {@link LocalAddeEntry LocalAddeEntries}
     * to a {@literal "RESOLV.SRV"} file. <b>This method discards the current
     * contents of {@code filename}!</b>
     * 
     * @param filename Filename that will contain the local ADDE entries
     * within {@code entries}. Cannot be {@code null}.
     * 
     * @param entries {@code Set} of entries to be written to {@code filename}.
     * Cannot be {@code null}.
     * 
     * @throws IOException if there was a problem writing to {@code filename}.
     * 
     * @see #appendResolvFile(String, Collection)
     */
    public static void writeResolvFile(
        final String filename,
        final Collection<LocalAddeEntry> entries) throws IOException
    {
        writeResolvFile(filename, false, entries);
    }

    /**
     * Writes a {@link Collection} of {@link LocalAddeEntry LocalAddeEntries}
     * to a {@literal "RESOLV.SRV"} file. This method will <i>append</i> the
     * contents of {@code entries} to {@code filename}.
     * 
     * @param filename Filename that will contain the local ADDE entries within
     * {@code entries}. Cannot be {@code null}.
     * 
     * @param entries {@code Collection} of entries to be written to {@code filename}.
     * Cannot be {@code null}.
     * 
     * @throws IOException if there was a problem writing to {@code filename}.
     * 
     * @see #writeResolvFile(String, Collection)
     */
    public static void appendResolvFile(
        final String filename,
        final Collection<LocalAddeEntry> entries) throws IOException
    {
        writeResolvFile(filename, true, entries);
    }

    /**
     * Writes a {@link Collection} of {@link LocalAddeEntry LocalAddeEntries}
     * to a {@literal "RESOLV.SRV"} file.
     * 
     * @param filename Filename that will contain the local ADDE entries within
     * {@code entries}. Cannot be {@code null}.
     * 
     * @param append If {@code true}, append {@code entries} to
     * {@code filename}. Otherwise discards contents of {@code filename}.
     * 
     * @param entries {@code Collection} of entries to be written to
     * {@code filename}. Cannot be {@code null}.
     * 
     * @throws IOException if there was a problem writing to {@code filename}.
     * 
     * @see #appendResolvFile(String, Collection)
     * @see #asResolvEntry(LocalAddeEntry)
     */
    private static void writeResolvFile(
        final String filename,
        final boolean append,
        final Collection<LocalAddeEntry> entries) throws IOException
    {
//        BufferedWriter bw = null;
//        try {
//            bw = new BufferedWriter(new FileWriter(filename));
//            for (LocalAddeEntry entry : entries) {
//                bw.write(asResolvEntry(entry)+'\n');
//            }
//        } finally {
//            if (bw != null) {
//                bw.close();
//            }
//        }
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(filename))) {
            String result = entries.stream()
                                   .map(EntryTransforms::asResolvEntry)
                                   .collect(Collectors.joining("\n")) + '\n';
            bw.write(result);
        }
    }

    public static Set<LocalAddeEntry> removeTemporaryEntriesFromResolvFile(
        final String filename,
        final Collection<LocalAddeEntry> entries) throws IOException
    {
        requireNonNull(filename, "Path to resolv file cannot be null");
        requireNonNull(entries, "Local entries cannot be null");

        Set<LocalAddeEntry> removedEntries = newLinkedHashSet(entries.size());
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(filename));
            for (LocalAddeEntry entry : entries) {
                if (!entry.isEntryTemporary()) {
                    bw.write(asResolvEntry(entry)+'\n');
                } else {
                    removedEntries.add(entry);
                }
            }
        } finally {
            if (bw != null) {
                bw.close();
            }
        }
        return removedEntries;
    }

    /**
     * De-munges file mask strings.
     *
     * <p>This process is largely used to generate
     * {@literal "Windows-friendly"} masks.</p>
     *
     * @param path File path to fix.
     *
     * @return {@code path} with Windows fixes applied.
     *
     * @throws NullPointerException if {@code path} is {@code null}. 
     */
    public static String demungeFileMask(final String path) {
        requireNonNull(path, "how dare you! null paths cannot be munged!");
        int index = path.indexOf("/*");
        if (index < 0) {
            return path;
        }
        String tmpFileMask = path.substring(0, index);
        // Look for "cygwinPrefix" at start of string and munge accordingly
        if ((tmpFileMask.length() > (cygwinPrefixLength + 1)) &&
                tmpFileMask.substring(0, cygwinPrefixLength).equals(cygwinPrefix)) {
            String driveLetter = tmpFileMask.substring(cygwinPrefixLength,cygwinPrefixLength+1).toUpperCase();
            return driveLetter + ':' + tmpFileMask.substring(cygwinPrefixLength+1).replace('/', '\\');
        } else {
            return tmpFileMask;
        }
    }

    /**
     * Munges a file mask {@link String} into something {@literal "RESOLV.SRV"}
     * expects.
     * 
     * <p>Munging is only needed for Windows users--the process converts 
     * back slashes into forward slashes and prefixes with {@literal "/cygdrive/"}.
     *
     * @param mask File mask that may need to be fixed before storing in
     *             {@code RESOLV.SRV}.
     *
     * @return Path suitable for storing in {@code RESOLV.SRV}.
     *
     * @throws NullPointerException if {@code mask} is {@code null}.
     */
    public static String mungeFileMask(final String mask) {
        requireNonNull(mask, "Cannot further munge this mask; it was null upon arriving");
        StringBuilder s = new StringBuilder(100);
        if ((mask.length() > 3) && ":".equals(mask.substring(1, 2))) {
            String newFileMask = mask;
            String driveLetter = newFileMask.substring(0, 1).toLowerCase();
            newFileMask = newFileMask.substring(3);
            newFileMask = newFileMask.replace('\\', '/');
            s.append("/cygdrive/").append(driveLetter).append('/').append(newFileMask);
        } else {
            s.append("").append(mask);
        }
        return s.toString();
    }

    /**
     * Converts a {@link Collection} of {@link LocalAddeEntry LocalAddeEntries}
     * into a {@link List} of strings.
     * 
     * @param entries {@code Collection} of entries to convert. Should not be
     * {@code null}.
     * 
     * @return {@code entries} represented as strings.
     * 
     * @see #asResolvEntry(LocalAddeEntry)
     */
    public static List<String> asResolvEntries(final Collection<LocalAddeEntry> entries) {
        List<String> resolvEntries = arrList(entries.size());
        resolvEntries.addAll(entries.stream()
                                    .map(EntryTransforms::asResolvEntry)
                                    .collect(Collectors.toList()));
        return resolvEntries;
    }

    /**
     * Converts a given {@link LocalAddeEntry} into a {@code String} that is 
     * suitable for including in a {@literal "RESOLV.SRV"} file. This method
     * does <b>not</b> append a newline to the end of the {@code String}.
     * 
     * @param entry The {@code LocalAddeEntry} to convert. Should not be
     * {@code null}.
     * 
     * @return {@code entry} as a {@literal "RESOLV.SRV"} entry.
     */
    public static String asResolvEntry(final LocalAddeEntry entry) {
        AddeFormat format = entry.getFormat();
        ServerName servName = format.getServerName();

        StringBuilder s = new StringBuilder(150);
        if (entry.getEntryStatus() != EntryStatus.ENABLED) {
            s.append('#');
        }
        s.append("N1=").append(entry.getGroup().toUpperCase())
            .append(",N2=").append(entry.getDescriptor().toUpperCase())
            .append(",TYPE=").append(format.getType())
            .append(",RT=").append(entry.getRealtimeAsString())
            .append(",K=").append(format.getServerName())
            .append(",R1=").append(entry.getStart())
            .append(",R2=").append(entry.getEnd())
            .append(",MCV=").append(format.name())
            .append(",C=").append(entry.getName())
            .append(",TEMPORARY=").append(entry.isEntryTemporary());

        if (servName == ServerName.LV1B) {
            s.append(",Q=LALO");
        }

        String tmpFileMask = entry.getFileMask();
        if (tmpFileMask.length() > 3 && ":".equals(tmpFileMask.substring(1, 2))) {
            String newFileMask = tmpFileMask;
            String driveLetter = newFileMask.substring(0, 1).toLowerCase();
            newFileMask = newFileMask.substring(3);
            newFileMask = newFileMask.replace('\\', '/');
            s.append(",MASK=/cygdrive/").append(driveLetter).append('/').append(newFileMask);
        } else {
            s.append(",MASK=").append(tmpFileMask);
        }
        // local servers seem to really like trailing commas!
        return s.append('/').append(format.getFileFilter()).append(',').toString(); 
    }
}
