package edu.wisc.ssec.mcidasv.ui;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.IdvUIManager;


/**
 * Derive our own ui manager to do some  example specific things.
 */

public class UIManager extends IdvUIManager {

    /** The tag in the xml ui for creating the special example chooser */
    public static final String TAG_EXAMPLECHOOSER = "examplechooser";


    /**
     * The ctor. Just pass along the reference to the idv.
     *
     * @param idv The idv
     */
    public UIManager(IntegratedDataViewer idv) {
        super(idv);
    }

}







