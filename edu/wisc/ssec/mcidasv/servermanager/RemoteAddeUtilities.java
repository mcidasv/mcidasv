package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.xml.XmlUtil;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.util.Contract;

public class RemoteAddeUtilities {

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
    
}
