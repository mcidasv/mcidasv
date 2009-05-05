package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.map;

import java.util.List;
import java.util.Set;

import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.util.functional.Function;
import ucar.unidata.idv.chooser.adde.AddeServer;

// useful methods for doing things like converting a "AddeServer" to a "RemoteAddeEntry"
// and so on.
public class EntryTransforms {
    private EntryTransforms() { }

    public static final Function<AddeServer, RemoteAddeEntry> convertIdvServer = new Function<AddeServer, RemoteAddeEntry>() {
        public RemoteAddeEntry apply(final AddeServer arg) {
            String hostname = arg.toString().toLowerCase();
            for (AddeServer.Group group : (List<AddeServer.Group>)arg.getGroups()) {
                System.err.println("apply: hostname="+hostname+" group="+group.getName()+" type="+group.getType());
            }

            return new RemoteAddeEntry.Builder(hostname, "temp").build();
        }
    };

    public static Set<RemoteAddeEntry> convertIdvServers(final List<AddeServer> idvServers) {
        Set<RemoteAddeEntry> addeEntries = newLinkedHashSet();
        addeEntries.addAll(map(convertIdvServer, idvServers));
        return addeEntries;
    }
}
