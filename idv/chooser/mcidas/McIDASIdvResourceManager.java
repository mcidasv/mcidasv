package ucar.unidata.idv;

public class McIDASIdvResourceManager extends IdvResourceManager {

    /** Points to the adde image defaults */
    public static final XmlIdvResource RSC_FRAMEDEFAULTS =
        new XmlIdvResource("application.resource.framedefaults",
                           "McIDAS-X Frame Defaults");

    public McIDASIdvResourceManager(IntegratedDataViewer idv) {
        super(idv);
    }
}
