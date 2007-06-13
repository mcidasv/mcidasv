package ucar.unidata.idv.chooser.mcidas;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.IdvResourceManager;

import ucar.unidata.xml.XmlResourceCollection;

public class McIDASIdvResourceManager extends IdvResourceManager {

    /** Points to the adde image defaults */
    public static final XmlIdvResource RSC_FRAMEDEFAULTS =
        new XmlIdvResource("application.resource.framedefaults",
                           "McIDAS-X Frame Defaults");

    /** Points to the server definitions */
    public static final XmlIdvResource RSC_SERVERS =
        new XmlIdvResource("idv.resource.servers",
                           "Servers", "servers\\.xml$");

    public McIDASIdvResourceManager(IntegratedDataViewer idv) {
        super(idv);
    }
}
