package edu.wisc.ssec.mcidasv.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.util.Msg;

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

    /**
     * Add in the menu items for the given display menu
     *
     * @param displayMenu The display menu
     */
    protected void initializeDisplayMenu(JMenu displayMenu) {
        JMenuItem mi;
        
        mi = new JMenuItem("Remove All Displays");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                getIdv().removeAllDisplays();
            }
        });
        displayMenu.add(mi);
        displayMenu.addSeparator();                                                                                 
    	
        processBundleMenu(displayMenu,
                          IdvPersistenceManager.BUNDLES_FAVORITES);
        processBundleMenu(displayMenu, IdvPersistenceManager.BUNDLES_DISPLAY);

        processMapMenu(displayMenu, true);
        processStationMenu(displayMenu, true);
        processStandAloneMenu(displayMenu, true);
        
        mi = new JMenuItem("Show Dashboard");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showDashboard();
            }
        });
        displayMenu.addSeparator();
        displayMenu.add(mi);
        
        Msg.translateTree(displayMenu);
    }
    
}