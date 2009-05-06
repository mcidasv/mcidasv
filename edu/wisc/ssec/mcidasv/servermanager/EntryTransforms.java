package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.map;

import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntrySource;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.util.Contract;
import edu.wisc.ssec.mcidasv.util.functional.Function;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

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

    // converts a list of AddeServers to a set of RemoteAddeEntry
    public static Set<RemoteAddeEntry> convertIdvServers(final List<AddeServer> idvServers) {
        Set<RemoteAddeEntry> addeEntries = newLinkedHashSet();
        addeEntries.addAll(map(convertIdvServer, idvServers));
        return addeEntries;
    }

    // convert a AddeServer XML element into a RemoteAddeEntry...
    @SuppressWarnings("unchecked")
    protected static Set<RemoteAddeEntry> convertAddeServerXml(Element root, EntrySource source) {
        Set<RemoteAddeEntry> es = newLinkedHashSet();

        List<Element> serverNodes = XmlUtil.findChildren(root, "server");
        for (int i = 0; i < serverNodes.size(); i++) {
            Element element = (Element)serverNodes.get(i);
            System.err.println("processing xml: "+XmlUtil.toString(element));
            String address = XmlUtil.getAttribute(element, "name");
            String description = XmlUtil.getAttribute(element, "description", "");

            // loop through each "group" entry.
            List<Element> groupNodes = XmlUtil.findChildren(element, "group");
            for (int j = 0; j < groupNodes.size(); j++) {
                Element group = (Element)groupNodes.get(j);

                // convert whatever came out of the "type" attribute into a 
                // valid EntryType.
                String strType = XmlUtil.getAttribute(group, "type");
                EntryType type = strToEntryType(strType);

                // the "names" attribute can contain comma-delimited group
                // names.
                List<String> names = StringUtil.split(XmlUtil.getAttribute(group, "names", ""), ",", true, true);
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
                String name = XmlUtil.getAttribute(group, "name", (String) null);
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

    private static EntryType strToEntryType(final String s) {
        EntryType type = EntryType.UNKNOWN;
        Contract.notNull(s);
        try {
            type = EntryType.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) { }
        return type;
    }
}
