package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newMap;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.w3c.dom.Element;

import ucar.unidata.idv.IdvResourceManager.IdvResource;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;
import edu.wisc.ssec.mcidas.adde.AddeServerInfo;
import edu.wisc.ssec.mcidas.adde.AddeTextReader;
import edu.wisc.ssec.mcidasv.addemanager.AddeEntry;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntrySource;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.util.Contract;

public class RemoteAddeUtilities {

    private enum AddeStatus { BAD_SERVER, BAD_ACCOUNTING, NO_METADATA, OK, BAD_GROUP };

    /** Number of threads in the thread pool. */
    private static final int POOL = 5;

    /** 
     * {@link String#format(String, Object...)}-friendly string for building a
     * request to read a server's PUBLIC.SRV.
     */
    private static final String publicSrvFormat = "adde://%s/text?compress=gzip&port=112&debug=false&version=1&user=%s&proj=%s&file=PUBLIC.SRV";

    /*
     * addeservers.xml:
     * <server active="true" description="test.jon.com" name="test.jon.com">
     *   <group active="true" description="TEST" names="TEST" type="any"/>
     *   <group active="true" description="WOO" names="WOO" type="oogabooga"/>
     * </server>
     */
//        private Set<DatasetDescriptor> serversToDescriptors(final String source,
//                final List<AddeServer> addeServers)
//            {
//                assert addeServers != null;
//                assert source != null;
//                Set<DatasetDescriptor> datasets = newLinkedHashSet();
//                for (AddeServer addeServer : addeServers)
//                    for (Group group : (List<Group>)addeServer.getGroups())
//                        datasets.add(new DatasetDescriptor(addeServer, group, source, "", "", false));
//                return datasets;
//            }

    private static Set<RemoteAddeEntry> readAddeServersXml(final String path) {
        Set<RemoteAddeEntry> addeEntries = newLinkedHashSet();

        Element root = grabRoot(path);
        if (root == null)
            return Collections.emptySet();

        List<AddeServer> serversFromIdv = 
            AddeServer.coalesce(AddeServer.processXml(root));

        
        return addeEntries;
    }

    private static Set<RemoteAddeEntry> readPersistedServersXml(final String path) {
        return null;
    }

    // entryList refers to "unverified/verified/deleted"
    private static Set<RemoteAddeEntry> readPreferencesXml(final String path, final String entryList) {
        return null;
    }

    private static EntryType strToEntryType(final String s) {
        EntryType type = EntryType.UNKNOWN;
        Contract.notNull(s);
        try {
            type = EntryType.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) { }
        return type;
    }

    private static Element grabRoot(final String path) {
        Element root = null;
        try {
            FileInputStream stream = new FileInputStream(path);
            root = XmlUtil.getRoot(stream);
        } catch (Exception e) {
            
        }
        return root;
    }

    public static AddeStatus checkEntry(final RemoteAddeEntry entry) {
        if (entry == null)
            throw new NullPointerException("Dataset Descriptor cannot be null");

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
     * Determines whether or not the server specified in {@code descriptor} is
     * listening on port 112.
     * 
     * @param descriptor Descriptor containing the server to check.
     * 
     * @return {@code true} if a connection was opened, {@code false} otherwise.
     * 
     * @throws NullPointerException if {@code descriptor} is null.
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

    public Set<RemoteAddeEntry> checkHosts(final Set<RemoteAddeEntry> entries) {
        if (entries == null)
            throw new NullPointerException("descriptors cannot be null");

        Set<RemoteAddeEntry> goodDescriptors = newLinkedHashSet();
        Set<String> checkedHosts = newLinkedHashSet();
        Map<String, Boolean> hostStatus = newMap();
        for (RemoteAddeEntry entry : entries) {
            String host = entry.getAddress();
            if (hostStatus.get(host) == Boolean.FALSE) {
                continue;
            } else if (hostStatus.get(host) == Boolean.TRUE) {
                goodDescriptors.add(entry);
            } else {
                checkedHosts.add(host);
                boolean connected = checkHost(entry);

                if (connected) {
                    goodDescriptors.add(entry);
                    hostStatus.put(host, Boolean.TRUE);
                } else {
                    hostStatus.put(host, Boolean.FALSE);
                }
            }
        }
        return goodDescriptors;
    }

    private static class StatusWrapper {
        private AddeStatus status;
        private final RemoteAddeEntry entry;

        public StatusWrapper(final RemoteAddeEntry entry) {
            if (entry == null)
                throw new NullPointerException("cannot create a entry/status pair with a null descriptor");
            this.entry = entry;
        }

        public void setStatus(AddeStatus status) {
            this.status = status;
        }

        public AddeStatus getStatus() {
            return status;
        }

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
