package edu.wisc.ssec.mcidasv.chooser;

import org.w3c.dom.Element;

import ucar.unidata.idv.*;
import ucar.unidata.idv.chooser.IdvChooser;

public abstract class McIdasChooser extends IdvChooser { 

    /** Used by derived classes to save the list of image descriptors */
    public static final String PREF_FRAMEDESCLIST =
        "application.framedescriptors";

    public McIdasChooser(IntegratedDataViewer idv, Element chooserNode) {
        super(idv, chooserNode);
    }
}









