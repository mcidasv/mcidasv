package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newMap;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import edu.wisc.ssec.mcidas.adde.AddeServerInfo;
import edu.wisc.ssec.mcidas.adde.AddeTextReader;
import edu.wisc.ssec.mcidasv.ServerPreferenceManager.AddeStatus;
import edu.wisc.ssec.mcidasv.addemanager.AddeEntry;


public class RemoteAddeVerification {

    /** Possible entry verification states. */
//    private enum AddeStatus { BAD_SERVER, BAD_ACCOUNTING, NO_METADATA, OK, BAD_GROUP };

    /** Number of threads in the thread pool. */
    private static final int POOL = 5;

    /** 
     * {@link String#format(String, Object...)}-friendly string for building a
     * request to read a server's PUBLIC.SRV.
     */
    private static final String publicSrvFormat = "adde://%s/text?compress=gzip&port=112&debug=false&version=1&user=%s&proj=%s&file=PUBLIC.SRV";

    /**
     * Attempts to verify whether or not the information in a given 
     * {@link RemoteAddeEntry} represents a valid remote ADDE server. If not,
     * the method tries to determine which parts of the entry are invalid.
     * 
     * @param entry The {@code RemoteAddeEntry} to check. Cannot be 
     * {@code null}.
     * 
     * @return The {@link AddeStatus} that represents the verification status
     * of {@code entry}.
     * 
     * @throws NullPointerException if {@code entry} is {@code null}.
     * 
     * @see AddeStatus
     */
    public static AddeStatus checkEntry(final RemoteAddeEntry entry) {
        if (entry == null)
            throw new NullPointerException("Cannot check a null entry");

        if (!checkHost(entry))
            return AddeStatus.BAD_SERVER;

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
        int status = serverInfo.setSelectedServer(server, type);
        if (status == -2)
            return AddeStatus.NO_METADATA;
        if (status == -1)
            return AddeStatus.BAD_ACCOUNTING;

        serverInfo.setSelectedGroup(entry.getGroup());
        String[] datasets = serverInfo.getDatasetList();
        if (datasets != null && datasets.length > 0)
            return AddeStatus.OK;
        else
            return AddeStatus.BAD_GROUP;
    }

    /**
     * Tries to connect to a given {@link RemoteAddeEntry} and read the list
     * of ADDE {@literal "groups"} available to the public.
     * 
     * @param entry The {@code RemoteAddeEntry} to query. Cannot be {@code null}.
     * 
     * @return The {@link Set} of public groups on {@code entry}.
     * 
     * @throws NullPointerException if {@code entry} is {@code null}.
     * @throws IllegalArgumentException if the server address is an empty 
     * {@link String}.
     */
    public static Set<String> readPublicGroups(final RemoteAddeEntry entry) {
        if (entry == null)
            throw new NullPointerException("entry cannot be null");
        if (entry.getAddress() == null)
            throw new NullPointerException();
        if (entry.getAddress().length() == 0)
            throw new IllegalArgumentException();

        String user = entry.getAccount().getUsername();
        if (user == null || user.length() == 0)
            user = RemoteAddeEntry.DEFAULT_ACCOUNT.getUsername();

        String proj = entry.getAccount().getProject();
        if (proj == null || proj.length() == 0)
            proj = RemoteAddeEntry.DEFAULT_ACCOUNT.getProject();

        String url = String.format(publicSrvFormat, entry.getAddress(), user, proj);

        Set<String> groups = newLinkedHashSet();

        AddeTextReader reader = new AddeTextReader(url);
        if (reader.getStatus().equals("OK"))
            for (String line : (List<String>)reader.getLinesOfText())
                groups.add(new AddeEntry(line).getGroup());

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
        if (entry == null)
            throw new NullPointerException("descriptor cannot be null");
        String host = entry.getAddress();
        Socket socket = null;
        boolean connected = false;
        try { 
            socket = new Socket(host, 112);
            connected = true;
        } catch (UnknownHostException e) {
            connected = false;
        } catch (IOException e) {
            connected = false;
        }
        try {
            socket.close();
        } catch (Exception e) {}
        return connected;
    }

    /**
     * Attempt to verify a {@link Set} of {@link RemoteAddeEntry}s. Useful for
     * checking a {@literal "MCTABLE.TXT"} after importing.
     * 
     * @param entries {@code Set} of remote ADDE entries to validate. Cannot 
     * be {@code null}.
     * 
     * @return {@code Set} of {@code RemoteAddeEntry}s that McIDAS-V was able
     * to connect to. 
     * 
     * @throws NullPointerException if {@code entries} is {@code null}.
     * 
     * @see #checkHost(RemoteAddeEntry)
     */
    public Set<RemoteAddeEntry> checkHosts(final Set<RemoteAddeEntry> entries) {
        if (entries == null)
            throw new NullPointerException("descriptors cannot be null");

        Set<RemoteAddeEntry> goodEntries = newLinkedHashSet();
        Set<String> checkedHosts = newLinkedHashSet();
        Map<String, Boolean> hostStatus = newMap();
        for (RemoteAddeEntry entry : entries) {
            String host = entry.getAddress();
            if (hostStatus.get(host) == Boolean.FALSE) {
                continue;
            } else if (hostStatus.get(host) == Boolean.TRUE) {
                goodEntries.add(entry);
            } else {
                checkedHosts.add(host);
                boolean connected = checkHost(entry);

                if (connected) {
                    goodEntries.add(entry);
                    hostStatus.put(host, Boolean.TRUE);
                } else {
                    hostStatus.put(host, Boolean.FALSE);
                }
            }
        }
        return goodEntries;
    }

    /**
     * Associates a {@link RemoteAddeEntry} with one of the states from 
     * {@link AddeStatus}.
     */
    private static class StatusWrapper {
        /** */
        private final RemoteAddeEntry entry;

        /** Current {@literal "status"} of {@link entry}. */
        private AddeStatus status;

        /**
         * Builds an entry/status pairing.
         * 
         * @param entry The {@code RemoteAddeEntry} to wrap up.
         * 
         * @throws NullPointerException if {@code entry} is {@code null}.
         */
        public StatusWrapper(final RemoteAddeEntry entry) {
            if (entry == null)
                throw new NullPointerException("cannot create a entry/status pair with a null descriptor");
            this.entry = entry;
        }

        /**
         * Set the {@literal "status"} of this {@link entry} to a given 
         * {@link AddeStatus}.
         * 
         * @param status New status of {@code entry}.
         */
        public void setStatus(AddeStatus status) {
            this.status = status;
        }

        /**
         * Returns the current {@literal "status"} of {@link entry}.
         * 
         * @return One of {@link AddeStatus}.
         */
        public AddeStatus getStatus() {
            return status;
        }

        /**
         * Returns the {@link RemoteAddeEntry} stored in this wrapper.
         * 
         * @return {@link entry}
         */
        public RemoteAddeEntry getEntry() {
            return entry;
        }
    }

    private class VerifyEntryTask implements Callable<StatusWrapper> {
        private final StatusWrapper entryStatus;
        public VerifyEntryTask(final StatusWrapper descStatus) {
            if (descStatus == null)
                throw new NullPointerException("cannot verify or set status of a null descriptor/status pair");
            this.entryStatus = descStatus;
        }

        public StatusWrapper call() throws Exception {
            entryStatus.setStatus(checkEntry(entryStatus.getEntry()));
            return entryStatus;
        }
    }
}
