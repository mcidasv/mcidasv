package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.map;

import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.AddeAccount;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntrySource;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryStatus;
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

    // converts older-style user xml into a set of adde entries
    protected static Set<RemoteAddeEntry> convertUserXml(final Element root) {
        Set<RemoteAddeEntry> entries = newLinkedHashSet();
        // <entry name="SERVER/DATASET" user="ASDF" proj="0000" source="user" enabled="true" type="image"/>
        List<Element> elements = XmlUtil.findChildren(root, "entry");
        for (Element entryXml : elements) {
            String name = XmlUtil.getAttribute(entryXml, "name");
            String user = XmlUtil.getAttribute(entryXml, "user");
            String proj = XmlUtil.getAttribute(entryXml, "proj");
            String source = XmlUtil.getAttribute(entryXml, "source");
            String type = XmlUtil.getAttribute(entryXml, "type");

            boolean enabled = Boolean.parseBoolean(XmlUtil.getAttribute(entryXml, "enabled"));

            EntryType entryType = strToEntryType(type);
            EntryStatus entryStatus = (enabled == true) ? EntryStatus.ENABLED : EntryStatus.DISABLED; 

            if (source.equals("user") && (name != null)) {
                String[] arr = name.split("/");
                String description = arr[0];
                if (arr[0].toLowerCase().contains("localhost")) {
                    description = "<LOCAL-DATA>";
                }

                RemoteAddeEntry.Builder incomplete = 
                    new RemoteAddeEntry.Builder(arr[0], arr[1])
                        .type(entryType)
                        .status(entryStatus)
                        .source(EntrySource.USER)
                        .description(description);

                if (((user != null) && (proj != null)) && ((user.length() > 0) && (proj.length() > 0)))
                    incomplete = incomplete.account(user, proj);

                entries.add(incomplete.build());
            }
        }

        return entries;
    }

    // convert a AddeServer XML element into a RemoteAddeEntry...
    @SuppressWarnings("unchecked")
    protected static Set<RemoteAddeEntry> convertAddeServerXml(Element root, EntrySource source) {
        Set<RemoteAddeEntry> es = newLinkedHashSet();

        List<Element> serverNodes = XmlUtil.findChildren(root, "server");
        for (int i = 0; i < serverNodes.size(); i++) {
            Element element = (Element)serverNodes.get(i);
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
