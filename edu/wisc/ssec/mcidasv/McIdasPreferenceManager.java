package edu.wisc.ssec.mcidasv;


import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IntegratedDataViewer;

/**
 * This class is responsible for the preference dialog and
 * managing general preference state.
 * A  set of {@link ucar.unidata.xml.PreferenceManager}-s are added
 * into the dialog. This class then constructs a tabbed pane
 * window, one pane for each PreferenceManager.
 * On  the user's Ok or Apply the dialog will
 * have each PreferenceManager apply its preferences.
 *
 * @author IDV development team
 */
public class McIdasPreferenceManager extends IdvPreferenceManager {


    public McIdasPreferenceManager(IntegratedDataViewer idv) {
        super(idv);
    }


    /**
     * Init the preference gui
     */
    protected void initPreferences() {
        super.initPreferences();
        ServerPreferenceManager mspm = new ServerPreferenceManager(getIdv());
        mspm.addServerPreferences(this);
    }

}

