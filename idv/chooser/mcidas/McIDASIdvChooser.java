package ucar.unidata.idv.chooser.mcidas;

import ucar.unidata.idv.*;
import ucar.unidata.idv.chooser.IdvChooser;
import org.w3c.dom.Element;

public abstract class McIDASIdvChooser extends IdvChooser { 

    /** Used by derived classes to save the list of image descriptors */
    public static final String PREF_FRAMEDESCLIST =
        "application.framedescriptors";

    public McIDASIdvChooser(IntegratedDataViewer idv, Element chooserNode) {
        super(idv, chooserNode);
    }

}









