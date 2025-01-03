/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
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
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.Contract.checkArg;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.wisc.ssec.mcidasv.util.MakeToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidas.adde.AddeServerInfo;
import edu.wisc.ssec.mcidas.adde.AddeTextReader;
import edu.wisc.ssec.mcidas.adde.AddeURLException;
import edu.wisc.ssec.mcidas.adde.DataSetInfo;

import edu.wisc.ssec.mcidasv.servermanager.RemoteEntryEditor.AddeStatus;

public class RemoteAddeEntry implements AddeEntry {

    /** Typical logger object. */
    private static final Logger logger =
        LoggerFactory.getLogger(RemoteAddeEntry.class);

    /** Represents an invalid remote ADDE entry. */
    public static final RemoteAddeEntry INVALID_ENTRY = 
        new Builder("localhost", "BIGBAD").invalidate().build();

    /** Represents a collection of invalid remote ADDE entries. */
    public static final List<RemoteAddeEntry> INVALID_ENTRIES = 
        Collections.singletonList(INVALID_ENTRY);

    /** Default port for remote ADDE servers. */
    public static final int ADDE_PORT = 112;

    /** 
     * {@link String#format(String, Object...)}-friendly string for building a
     * request to read a server's {@literal "PUBLIC.SRV"}.
     */
    private static final String publicSrvFormat = "adde://%s/text?compress=gzip&port=112&debug=%s&version=1&user=%s&proj=%s&file=PUBLIC.SRV";

    /** Holds the accounting information for this entry. */
    private final AddeAccount account;

    /** The server {@literal "address"} of this entry. */
    private final String address;

    /** The {@literal "dataset"} of this entry. */
    private final String group;

    /** Whether or not this entry will persist between McIDAS-V sessions. */
    private final boolean isTemporary;

    /** This entry's type. */
    private EntryType entryType;

    /** Whether or not this entry is valid. */
    private EntryValidity entryValidity;

    /** Where this entry came from. */
    private EntrySource entrySource;

    /** Whether or not this entry is in the {@literal "active set"}. */
    private EntryStatus entryStatus;

    /** Allows the user to refer to this entry with an arbitrary name. */
    private String entryAlias;

    private String asStringId;

    /** 
     * Used so that the hashCode of this entry is not needlessly 
     * recalculated.
     * 
     * @see #hashCode()
     */
    private volatile int hashCode = 0;

    /**
     * Creates a new ADDE entry using a give {@literal "ADDE entry builder"}.
     * 
     * @param builder Object used to build this entry.
     */
    private RemoteAddeEntry(Builder builder) {
        this.account = builder.account;
        this.address = builder.address;
        this.group = builder.group;
        this.entryType = builder.entryType;
        this.entryValidity = builder.entryValidity;
        this.entrySource = builder.entrySource;
        this.entryStatus = builder.entryStatus;
        this.isTemporary = builder.temporary;
        this.entryAlias = builder.alias;
    }

    /**
     * @return {@link #address}
     */
    @Override public String getAddress() {
        return address;
    }

    /**
     * @return {@link #group}
     */
    @Override public String getGroup() {
        return group;
    }

    @Override public String getName() {
        return "$";
    }

    /**
     * @return {@link #account}
     */
    @Override public AddeAccount getAccount() {
        return account;
    }

    /**
     * @return {@link #entryType}
     */
    @Override public EntryType getEntryType() {
        return entryType;
    }

    /**
     * @return {@link #entryValidity}
     */
    @Override public EntryValidity getEntryValidity() {
        return entryValidity;
    }

    public void setEntryValidity(final EntryValidity entryValidity) {
        this.entryValidity = entryValidity;
    }

    /**
     * @return {@link #entrySource}
     */
    @Override public EntrySource getEntrySource() {
        return entrySource;
    }

    /**
     * @return {@link #entryStatus}
     */
    @Override public EntryStatus getEntryStatus() {
        return entryStatus;
    }

    @Override public void setEntryStatus(EntryStatus newStatus) {
        entryStatus = newStatus;
    }

    @Override public String getEntryAlias() {
        return entryAlias;
    }

    @Override public void setEntryAlias(final String newAlias) {
        if (newAlias == null) {
            throw new NullPointerException("Null aliases are not allowable.");
        }
        entryAlias = newAlias;
    }

    @Override public boolean isEntryTemporary() {
        return isTemporary;
    }

    /**
     * Handy {@code String} representation of this ADDE entry. Currently looks
     * like {@code ADDRESS/GROUP}, but this is subject to change.
     * 
     * @return Alternate {@code String} representation of this entry.
     */
    @Override public String getEntryText() {
        return address+'/'+group;
    }

    /**
     * Determines whether or not the given object is equivalent to this ADDE 
     * entry.
     * 
     * @param obj Object to test against. {@code null} values are okay, but 
     * return {@code false}.
     * 
     * @return {@code true} if the given object is the same as this ADDE 
     * entry, {@code false} otherwise... including when {@code o} is 
     * {@code null}.
     */
    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RemoteAddeEntry)) {
            return false;
        }
        RemoteAddeEntry other = (RemoteAddeEntry) obj;
        if (account == null) {
            if (other.account != null) {
                return false;
            }
        } else if (!account.equals(other.account)) {
            return false;
        }
        if (address == null) {
            if (other.address != null) {
                return false;
            }
        } else if (!address.equals(other.address)) {
            return false;
        }
        if (entryType == null) {
            if (other.entryType != null) {
                return false;
            }
        } else if (!entryType.equals(other.entryType)) {
            return false;
        }
        if (group == null) {
            if (other.group != null) {
                return false;
            }
        } else if (!group.equals(other.group)) {
            return false;
        }
        if (entryAlias == null) {
            if (other.entryAlias != null) {
                return false;
            }
        } else if (!entryAlias.equals(other.entryAlias)) {
            return false;
        }
        if (isTemporary != other.isTemporary) {
            return false;
        }
        return true;
    }

    /**
     * Returns a hash code for this ADDE entry. The hash code is computed 
     * using the values of the following fields: 
     * {@link #address}, {@link #group}, {@link #entryType}, {@link #account}.
     * 
     * @return Hash code value for this object.
     */
    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((account == null) ? 0 : account.hashCode());
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + ((entryType == null) ? 0 : entryType.hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((entryAlias == null) ? 0 : entryAlias.hashCode());
        result = prime * result + (isTemporary ? 1231 : 1237);
        return result;
    }

    @Override public String asStringId() {
        if (asStringId == null) {
            asStringId = address+'!'+group+'!'+entryType.name();
        }
        return asStringId;
    }

    public String toString() {
        return MakeToString.fromInstance(this)
                           .add("address", address)
                           .add("group", group)
                           .add("entryType", entryType)
                           .add("entryValidity", entryValidity)
                           .add("account", account)
                           .add("entryStatus", entryStatus.name())
                           .add("entrySource", entrySource)
                           .add("isTemporary", isTemporary)
                           .add("entryAlias", entryAlias).toString();
    }

    /**
     * Something of a hack... this approach allows us to build a 
     * {@code RemoteAddeEntry} in a <b>readable</b> way, despite there being
     * multiple {@code final} fields. 
     * 
     * <p>The only <i>required</i> parameters are
     * the {@link RemoteAddeEntry#address} and {@link RemoteAddeEntry#group}.</p>
     * 
     * <p>Some examples:</p>
     *
     * <pre>
     * RemoteAddeEntry e = RemoteAddeEntry.Builder("adde.cool.com", "RTIMAGES").build();
     * e = RemoteAddeEntry.Builder("adde.cool.com", "RTIMAGES").type(EntryType.IMAGE).account("user", "1337").build();
     * e = RemoteAddeEntry.Builder("adde.cool.com", "RTIMAGES").account("user", "1337").type(EntryType.IMAGE).build()
     * e = RemoteAddeEntry.Builder("a.c.com", "RTIMGS").validity(EntryValidity.VERIFIED).build();
     * </pre>
     */
    public static class Builder {

        /** Hostname or IP of the resulting entry. */
        private final String address;

        /** ADDE group to use for the resulting entry. */
        private final String group;

        /** 
         * Optional {@link EntryType} of the entry. Defaults to 
         * {@link EntryType#UNKNOWN}. 
         */
        private EntryType entryType = EntryType.UNKNOWN;

        /** Optional {@link EntryValidity} of the entry. Defaults to 
         * {@link EntryValidity#UNVERIFIED}. 
         */
        private EntryValidity entryValidity = EntryValidity.UNVERIFIED;

        /** 
         * Optional {@link EntrySource} of the entry. Defaults to 
         * {@link EntrySource#SYSTEM}. 
         */
        private EntrySource entrySource = EntrySource.SYSTEM;

        /** 
         * Optional {@link EntryStatus} of the entry. Defaults to 
         * {@link EntryStatus#ENABLED}. 
         */
        private EntryStatus entryStatus = EntryStatus.ENABLED;

        /** 
         * Optional {@link AddeAccount} of the entry. Defaults to 
         * {@link RemoteAddeEntry#DEFAULT_ACCOUNT}. 
         */
        private AddeAccount account = RemoteAddeEntry.DEFAULT_ACCOUNT;

        /** Optional description of the entry. Defaults to {@literal ""}. */
        private String description = "";

        /**
         * Optional flag for whether or not the entry is temporary.
         * Defaults to {@code false}.
         */
        private boolean temporary = false;

        /** Optional alias for the entry. Default to {@literal ""}. */
        private String alias = "";

        /**
         * Creates a new {@literal "builder"} for an ADDE entry. Note that
         * the two parameters to this constructor are the only <i>required</i>
         * parameters to create an ADDE entry.
         * 
         * @param address Address of the ADDE entry. Cannot be null.
         * @param group Group of the ADDE entry. Cannot be null.
         * 
         * @throws NullPointerException if either {@code address} or 
         * {@code group} is {@code null}.
         */
        public Builder(final String address, final String group) {
            if (address == null) {
                throw new NullPointerException("ADDE address cannot be null");
            }
            if (group == null) {
                throw new NullPointerException("ADDE group cannot be null");
            }

            this.address = address.toLowerCase();
            this.group = group;
        }

        /** 
         * Optional {@literal "parameter"} for an ADDE entry. Allows you to
         * specify the accounting information. If this method is not called,
         * the resulting ADDE entry will be built with 
         * {@link RemoteAddeEntry#DEFAULT_ACCOUNT}.
         * 
         * @param username Username of the ADDE account. Cannot be 
         * {@code null}.
         * @param project Project number for the ADDE account. Cannot be 
         * {@code null}.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         * 
         * @see AddeAccount#AddeAccount(String, String)
         */
        public Builder account(final String username, final String project) {
            account = new AddeAccount(username, project);
            return this;
        }

        /**
         * Optional {@literal "parameter"} for an ADDE entry. Allows you to
         * set the {@link RemoteAddeEntry#entryType}. If this method is not 
         * called, {@code entryType} will default to {@link EntryType#UNKNOWN}.
         * 
         * @param entryType ADDE entry {@literal "type"}.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         */
        public Builder type(EntryType entryType) {
            this.entryType = entryType;
            return this;
        }

        /**
         * Optional {@literal "parameter"} for an ADDE entry. Allows you to
         * set the {@link RemoteAddeEntry#entryValidity}. If this method is 
         * not called, {@code entryValidity} will default to 
         * {@link EntryValidity#UNVERIFIED}.
         * 
         * @param entryValidity ADDE entry {@literal "validity"}.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         */
        public Builder validity(EntryValidity entryValidity) {
            this.entryValidity = entryValidity;
            return this;
        }

        /**
         * Optional {@literal "parameter"} for an ADDE entry. Allows you to
         * set the {@link RemoteAddeEntry#entrySource}. If this method is not 
         * called, {@code entrySource} will default to 
         * {@link EntrySource#SYSTEM}.
         * 
         * @param entrySource ADDE entry {@literal "source"}.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         */
        public Builder source(EntrySource entrySource) {
            this.entrySource = entrySource;
            return this;
        }

        /**
         * Optional {@literal "parameter"} for an ADDE entry. Allows you to
         * set the {@link RemoteAddeEntry#entryStatus}. If this method is not 
         * called, {@code entryStatus} will default to 
         * {@link EntryStatus#ENABLED}.
         * 
         * @param entryStatus ADDE entry {@literal "status"}.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         */
        public Builder status(EntryStatus entryStatus) {
            this.entryStatus = entryStatus;
            return this;
        }

        /**
         * Convenient way to generate a new, invalid entry.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         */
        public Builder invalidate() {
            this.entryType = EntryType.INVALID;
            this.entryValidity = EntryValidity.INVALID;
            this.entrySource = EntrySource.INVALID;
            this.entryStatus = EntryStatus.INVALID;
            return this;
        }

        /**
         * Optionally control whether or not the resulting entry is
         * {@literal "temporary"}.
         * 
         * @param temporary Whether or not the entry is temporary.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         */
        public Builder temporary(boolean temporary) {
            this.temporary = temporary;
            return this;
        }

        /**
         * Optionally sets the {@literal "alias"} that can be used to refer to
         * the resulting entry.
         * 
         * @param alias Alias for the resulting entry.
         * 
         * @return Current {@literal "builder"} for an ADDE entry.
         */
        public Builder alias(final String alias) {
            this.alias = alias;
            return this;
        }

        /** 
         * Creates an entry based upon the values supplied to the other 
         * methods. 
         * 
         * @return A newly created {@code RemoteAddeEntry}.
         */
        public RemoteAddeEntry build() {
            return new RemoteAddeEntry(this);
        }
    }

    /**
     * Tries to connect to a given {@code RemoteAddeEntry} and read the list
     * of ADDE {@literal "groups"} available to the public.
     * 
     * @param entry The {@code RemoteAddeEntry} to query. Cannot be {@code null}.
     * 
     * @return {@link Set} of public groups on {@code entry}.
     * 
     * @throws NullPointerException if {@code entry} is {@code null}.
     * @throws IllegalArgumentException if the server address is an empty 
     * {@link String}.
     */
    public static Set<String> readPublicGroups(final RemoteAddeEntry entry) {
        requireNonNull(entry, "entry cannot be null");
        requireNonNull(entry.getAddress());
        checkArg(!entry.getAddress().isEmpty());

        String user = entry.getAccount().getUsername();
        if ((user == null) || user.isEmpty()) {
            user = RemoteAddeEntry.DEFAULT_ACCOUNT.getUsername();
        }

        String proj = entry.getAccount().getProject();
        if ((proj == null) || proj.isEmpty()) {
            proj = RemoteAddeEntry.DEFAULT_ACCOUNT.getProject();
        }

        boolean debugUrl = EntryStore.isAddeDebugEnabled(false);
        String url = String.format(publicSrvFormat, entry.getAddress(), debugUrl, user, proj);

        Set<String> groups = newLinkedHashSet();

        AddeTextReader reader = new AddeTextReader(url);
        if ("OK".equals(reader.getStatus())) {
            for (String line : (List<String>)reader.getLinesOfText()) {
                String[] pairs = line.trim().split(",");
                for (String pair : pairs) {
                    if ((pair == null) || pair.isEmpty() || !pair.startsWith("N1")) {
                        continue;
                    }
                    String[] keyval = pair.split("=");
                    if ((keyval.length != 2) || keyval[0].isEmpty() || keyval[1].isEmpty() || !keyval[0].equals("N1")) {
                        continue;
                    }
                    groups.add(keyval[1]);
                }
            }
        }
        return groups;
    }

    /**
     * Determines whether or not the server specified in {@code entry} is
     * listening on port 112.
     * 
     * @param entry Descriptor containing the server to check.
     * 
     * @return {@code true} if a connection was opened, {@code false} otherwise.
     * 
     * @throws NullPointerException if {@code entry} is null.
     */
    public static boolean checkHost(final RemoteAddeEntry entry) {
        requireNonNull(entry, "entry cannot be null");
        String host = entry.getAddress();
        boolean connected;
        if (host.startsWith("localhost:")) {
            connected = true;
        } else {
            try (Socket socket = new Socket()){
                socket.connect(new InetSocketAddress(host, ADDE_PORT), 1000);
                connected = true;
                socket.close();
            } catch (UnknownHostException e) {
                logger.debug("can't resolve IP for '{}'", host);
                connected = false;
            } catch (IOException e) {
                logger.debug("IO problem while connecting to '{}': {}", entry.getAddress(), e.getMessage());
                connected = false;
            }
        }
        logger.trace("host={} type={} result={}", entry.getAddress(), entry.getEntryType(), connected);
        return connected;
    }

    /**
     * Attempts to verify whether or not the information in a given
     * RemoteAddeEntry represents a valid remote ADDE server. If not, the
     * method tries to determine which parts of the entry are invalid.
     * 
     * <p>Note that this method uses {@code checkHost(RemoteAddeEntry)} to 
     * verify that the server is listening. To forego the check, simply call
     * {@code checkEntry(false, entry)}.
     * 
     * @param entry {@code RemoteAddeEntry} to check. Cannot be 
     * {@code null}.
     * 
     * @return The {@link AddeStatus} that represents the verification status
     * of {@code entry}.
     * 
     * @see #checkHost(RemoteAddeEntry)
     * @see #checkEntry(boolean, RemoteAddeEntry)
     */
    public static AddeStatus checkEntry(final RemoteAddeEntry entry) {
        return checkEntry(true, entry);
    }

    /**
     * Attempts to verify whether or not the information in a given 
     * RemoteAddeEntry represents a valid remote ADDE server. If not, the
     * method tries to determine which parts of the entry are invalid.
     * 
     * @param checkHost {@code true} tries to connect to the remote ADDE server
     * before doing anything else.
     * @param entry {@code RemoteAddeEntry} to check. Cannot be 
     * {@code null}.
     * 
     * @return The {@link AddeStatus} that represents the verification status
     * of {@code entry}.
     * 
     * @throws NullPointerException if {@code entry} is {@code null}.
     * 
     * @see AddeStatus
     */
    public static AddeStatus checkEntry(final boolean checkHost, final RemoteAddeEntry entry) {
        requireNonNull(entry, "Cannot check a null entry");

        if (checkHost && !checkHost(entry)) {
            return AddeStatus.BAD_SERVER;
        }

        String server = entry.getAddress();
        String type = entry.getEntryType().toString();
        String username = entry.getAccount().getUsername();
        String project = entry.getAccount().getProject();
        String[] servers = { server };
        AddeServerInfo serverInfo = new AddeServerInfo(servers);

        // I just want to go on the record here: 
        // AddeServerInfo#setUserIDAndProjString(String) was not a good API 
        // decision.
        serverInfo.setUserIDandProjString("user="+username+"&proj="+project);
        // TJJ Sep 2024
        // We now set the group as well, because to verify accounting we
        // switched from 1) looking for PUBLIC.SRV to 2) a DSINFO call
        // ...because we found cases where PUBLIC.SRV does not exist
        serverInfo.setSelectedGroup(entry.getGroup());
        int status = serverInfo.setSelectedServer(server, type);
        if (status == -2) {
            return AddeStatus.NO_METADATA;
        }
        if (status == -1) {
            return AddeStatus.BAD_ACCOUNTING;
        }

        serverInfo.setSelectedGroup(entry.getGroup());
        String[] datasets = serverInfo.getDatasetList();
        if ((datasets != null) && (datasets.length > 0)) {
            // TJJ 7 Nov 2013, not my proudest moment. See Inq #905
            // if type is NEXR, this is a Radar server, not Image
            String ff = serverInfo.getFileFormat();
            if ("NEXR".equals(ff)) {
                entry.entryType = AddeEntry.EntryType.RADAR;
            }
            return AddeStatus.OK;
        }
        // TJJ - see Inq 1975, needed to add this hack because it seems
        // imagery always technically validates as radar.
        else if (!"RADAR".equals(type)) {
            // try dsinfo
            String addeUrl = "adde://"+server+"/datasetinfo?group="+entry.getGroup()+"&type="+type+"&user="+username+"&proj="+project+"&compress=gzip&port=112&debug=true&version=1";
            logger.trace("dsinfo url: '{}'", addeUrl);
            try {
                DataSetInfo dsinfo = new DataSetInfo(addeUrl);
                Map<?, ?> descriptionTable = dsinfo.getDescriptionTable();
                if ((descriptionTable != null) && !descriptionTable.isEmpty()) {
                    return AddeStatus.OK;
                }
            } catch (AddeURLException e) {
                logger.trace("dsinfo failed for url: '{}'", addeUrl);
            }
            return AddeStatus.BAD_GROUP;
        }
        // at this point can only be a bad group
        else {
            return AddeStatus.BAD_GROUP;
        }
    }

    /**
     * Determine the types of ADDE data within the given {@code group} on
     * {@code host}. This method uses the {@literal "default"} ADDE user name
     * and project number.
     *
     * <p>Note: <b>parameters cannot be {@code null}.</b></p>
     *
     * @param host Host to check.
     * @param group ADDE group.
     *
     * @return {@link EnumMap} that maps ADDE data type to whether or not it
     * is available for the given {@code host} and {@code group}.
     */
    public static Map<EntryType, AddeStatus> checkEntryTypes(final String host, final String group) {
        return checkEntryTypes(host, group, AddeEntry.DEFAULT_ACCOUNT.getUsername(), AddeEntry.DEFAULT_ACCOUNT.getProject());
    }

    /**
     * Determine the types of ADDE data within the given {@code group} on
     * {@code host}.
     *
     * <p>Note: <b>parameters cannot be {@code null}.</b></p>
     *
     * @param host Host to check.
     * @param group ADDE group.
     * @param user ADDE user name.
     * @param proj ADDE project number.
     *
     * @return {@link EnumMap} that maps ADDE data type to whether or not it
     * is available for the given set of parameters.
     *
     * @see #checkEntry(boolean, RemoteAddeEntry)
     */
    public static Map<EntryType, AddeStatus> checkEntryTypes(final String host, final String group, final String user, final String proj) {
        // current type count is six. doubling it to be safe.
        Map<EntryType, AddeStatus> valid = new EnumMap<>(EntryType.class);
        RemoteAddeEntry entry = new Builder(host, group).account(user, proj).build();
        for (RemoteAddeEntry tmp : EntryTransforms.createEntriesFrom(entry)) {
            valid.put(tmp.entryType, checkEntry(true, tmp));
        }
        return valid;
    }

    /**
     * Attempts to determine the {@literal "public"} ADDE groups available on
     * the given {@code host}.
     *
     * <p>Note: this method uses the {@literal "default"} ADDE user name and
     * project number.</p>
     *
     *
     * @param host Host from which public groups are to be read. Cannot be {@code null}.
     *
     * @return {@link Set} of the public groups on {@code host}. The
     * {@code Set} will be empty if there are no groups.
     */
    public static Set<String> readPublicGroups(final String host) {
        return readGroups(host, AddeEntry.DEFAULT_ACCOUNT.getUsername(), AddeEntry.DEFAULT_ACCOUNT.getProject());
    }

    /**
     * Attempts to determine which (if any) ADDE groups are available on the
     * given {@code host}.
     *
     * <p>Note: <b>parameters cannot be {@code null}.</b></p>
     *
     * @param host Host from which public groups are to be read.
     * @param user ADDE user name.
     * @param proj ADDE project number.
     *
     * @return {@link Set} of the groups on {@code host}. The {@code Set} will
     * be empty if there are no groups.
     */
    public static Set<String> readGroups(final String host, final String user, final String proj) {
        RemoteAddeEntry entry = new Builder(host, "").account(user, proj).build();
        return readPublicGroups(entry);
    }
}
