/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.chooser;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.w3c.dom.Element;

import edu.wisc.ssec.mcidasv.ui.McIDASVXmlUi;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.ui.TreePanel;
import ucar.unidata.ui.XmlUi;
import ucar.unidata.util.LogUtil;
import ucar.unidata.xml.XmlResourceCollection;

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
 * @version $Revision$Date: 2011/03/24 16:06:31 $
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
     * Create the Choosers component from the choosers.xml resources
     *
     * @param inTabs  Do we use the buttontabbedpane or the treepanel
     *
     * @return choosers gui
     */
    @Override
    public JComponent createChoosers(boolean inTabs) {
    	return createChoosers(inTabs, new ArrayList(), null);
    }

    /**
     * Initialize addeServers list
     */
    public List initializeAddeServers(IntegratedDataViewer idv) {
        List servers = initializeAddeServers(idv, true);
        return servers;
    }

    /**
     * Creates a new {@link McIDASVXmlUi} that can create the UI described in
     * {@code root}.
     * 
     * @param root XML description of a GUI component.
     * 
     * @return A new {@code McIDASVXmlUi} to use for creating {@code root}.
     */
    @Override protected XmlUi createXmlUi(final Element root) {
        return new McIDASVXmlUi(getIdv(), root);
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
                if (!allServers)
                   if (!addeServerResources.isWritableResource(resourceIdx)) continue;
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
//                if (!allServers) break;
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
