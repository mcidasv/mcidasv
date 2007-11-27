package edu.wisc.ssec.mcidasv.chooser;



import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSource;


import ucar.unidata.idv.*;
import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeChooser;

import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.idv.ui.*;





import ucar.unidata.ui.XmlUi;



import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectArray;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Resource;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlPersistable;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;


import java.io.File;


import java.lang.reflect.Constructor;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;

import javax.swing.event.*;




/**
 * This creates and manages the set of choosers.
 * It makes the chooser GUI from an xml specification
 * e.g.: /ucar/unidata/idv/resources/choosers.xml
 * It uses the {@link ucar.unidata.ui.XmlUi} to process
 * the xml.
 * <p>
 * This class also processes the end-user created choosers.
 * This piece has always been a bit flaky
 *
 * @author IDV development team
 * @version $Revision$Date: 2007/08/23 20:06:42 $
 */

public class McIdasChooserManager extends IdvChooserManager {



    /** All of the adde servers */
    private List addeServers = new ArrayList();

    private static boolean myServers = true;

    
    /**
     *  Create a new IdvChooserManager.
     *
     *  @param idv The singleton IDV
     */
    public McIdasChooserManager(IntegratedDataViewer idv) {
        super(idv);
        addeServers = initializeAddeServers(idv);
    }

    /**
     * Initialize addeServers list
     *
     */
    public List initializeAddeServers(IntegratedDataViewer idv) {
        List servers = initializeAddeServers(idv, true);
        return servers;
    }


    /**
     * Initialize addeServers list
     *
     */
    public List initializeAddeServers(IntegratedDataViewer idv, boolean allServers) {
        addeServers = new ArrayList();

        XmlResourceCollection addeServerResources =
            idv.getResourceManager().getXmlResources(
                IdvResourceManager.RSC_ADDESERVER);
        try {
            for (int resourceIdx = 0;
                    resourceIdx < addeServerResources.size(); resourceIdx++) {
                Element root = addeServerResources.getRoot(resourceIdx);
                if (root == null) {
                    continue;
                }
                List servers = AddeServer.processXml(root);
                for (int serverIdx = 0; serverIdx < servers.size();
                        serverIdx++) {
                    AddeServer addeServer =
                        (AddeServer) servers.get(serverIdx);
                    addeServer.setIsLocal(true);
                    List groups = addeServer.getGroups();
                    for (int groupIdx = 0; groupIdx < groups.size();
                            groupIdx++) {
                        AddeServer.Group group =
                            (AddeServer.Group) groups.get(groupIdx);
                        group.setIsLocal(true);
                    }
                }
                addeServers.addAll(servers);
                if (!allServers) break;
            }
        } catch (Exception exc) {
            LogUtil.logException("Error processing adde server descriptions",
                                 exc);
        }
        addeServers = AddeServer.coalesce(addeServers);

        Object oldServers =
            getIdv().getStore().get(IdvChooser.PREF_ADDESERVERS);
        if ((oldServers != null) && (oldServers instanceof List)) {
            List prefs = (List) oldServers;
            for (int i = 0; i < prefs.size(); i++) {
                String server = (String) prefs.get(i);
                addAddeServer(server);
            }
            getIdv().getStore().remove(IdvChooser.PREF_ADDESERVERS);
            getIdv().getStore().saveIfNeeded();
            writeAddeServers();
        }
        return addeServers;
    }

    /**
     * Get AddeServers to use
     *
     * @param groupType If null return all, else return the servers that have groups of the given type
     *
     * @return List of AddeServers
     */
    public List getAddeServers(String groupType) {
        return getAddeServers(groupType, true);
    }


    /**
     * Get AddeServers to use
     *
     * @param groupType If null return all, else return the servers that have groups of the given type
     * @param onlyActive If true then only fetch the active servers
     *
     * @return List of AddeServers
     */
    public List getAddeServers(String groupType, boolean onlyActive) {
        List servers;
        if (groupType == null) {
            servers = new ArrayList(addeServers);
        } else {
            servers = AddeServer.getServersWithType(groupType, addeServers);
        }
        if ( !onlyActive) {
            return servers;
        }

        List       activeServers = new ArrayList();
        AddeServer addeServer;
        for (int i = 0; i < addeServers.size(); i++) {
            addeServer = (AddeServer) addeServers.get(i);
            if (addeServer.getActive()) {
                activeServers.add(addeServer);
            }
        }
        return activeServers;
    }
}
