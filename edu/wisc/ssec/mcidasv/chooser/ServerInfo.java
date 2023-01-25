/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2023
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


import edu.wisc.ssec.mcidasv.*;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.IdvResourceManager;

import ucar.unidata.idv.chooser.adde.AddeServer;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;


/**
 * {@code ServerInfo} objects have methods for accessing the contents of
 * {@code servers.xml}.
 */
public class ServerInfo {

    private IntegratedDataViewer myIdv;

    /** tags */
    public static final String TAG_SERVER = "server";
    public static final String TAG_SERVERS = "servers";
    public static final String TAG_GROUP = "group";

    /** attributes */
    public static final String ATTR_ACTIVE = "active";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_NAMES = "names";
    public static final String ATTR_USER = "user";
    public static final String ATTR_PROJ = "proj";
    public static final String ATTR_TYPE = "type";

    private XmlResourceCollection serversXRC;
    private Document serversDocument;
    private Element serversRoot;

    private static String user = "idv";
    private static String proj = "0";

    private List typeList = new ArrayList();
    private List groups = new ArrayList();

    private List serverDescriptors = new ArrayList();

    private String[] defTypes = {"image", "point", "grid", "text", "nav"};

    /**
     * Creates a new {@code ServerInfo} object.
     *
     * @param idv Reference to the main application object.
     * @param servers ADDE server definitions.
     */
    public ServerInfo(IntegratedDataViewer idv, XmlResourceCollection servers) {
        myIdv = idv;
        serversXRC = servers;

        if (serversXRC.hasWritableResource()) {
            serversDocument = 
                serversXRC.getWritableDocument("<servers></servers>");
            serversRoot =
                serversXRC.getWritableRoot("<servers></servers>");
            String tagName = serversRoot.getTagName();
        }
        getServersFromXml(serversXRC);
    }


    /**
     * Read and parse servers.xml.
     *
     * @param servers Collection of ADDE server definitions.
     */
    private void getServersFromXml(XmlResourceCollection servers) {
        servers.clearCache();
        serverDescriptors.clear();
        if (serversXRC.hasWritableResource()) {
            serversDocument =
                serversXRC.getWritableDocument("<tabs></tabs>");
            serversRoot =
                serversXRC.getWritableRoot("<tabs></tabs>");
            try {
                Element root = serversRoot;
                if (root == null) return;
//                if ((user.equals("")) && (proj.equals(""))) {
                    if (serversRoot.hasAttribute(ATTR_USER))
                        user = serversRoot.getAttribute(ATTR_USER);
                    if (serversRoot.hasAttribute(ATTR_PROJ))
                        proj = serversRoot.getAttribute(ATTR_PROJ);
//                }
                List serverElements = XmlUtil.getElements(root, TAG_SERVER);
                for (int serverIdx = 0; serverIdx < serverElements.size(); serverIdx++) {
                    Element serverElement = (Element) serverElements.get(serverIdx);
                    String nameServer = XmlUtil.getAttribute(serverElement, ATTR_NAME);
                    String activeServer = XmlUtil.getAttribute(serverElement, ATTR_ACTIVE);
                    List groupElements = XmlUtil.getElements(serverElement, TAG_GROUP);
                    for (int groupIdx = 0; groupIdx < groupElements.size(); groupIdx++) {
                        Element groupElement = (Element) groupElements.get(groupIdx);
                        String activeGroup = XmlUtil.getAttribute(groupElement, ATTR_ACTIVE);
                        String nameGroup = XmlUtil.getAttribute(groupElement, ATTR_NAMES);
                        String type = XmlUtil.getAttribute(groupElement, ATTR_TYPE);
                        if (!typeList.contains(type)) {
                            typeList.add(type);
                        }
                        ServerDescriptor sd =
                            new ServerDescriptor(type, nameServer, nameGroup, activeGroup);
                            serverDescriptors.add(sd);
                    }
                }
            } catch (Exception e) {
                System.out.println("getServersFromXml e=" + e);
            }
        }
        return;
    }

    /**
     * Return userId, default="idv".
     *
     * @return User ID. Default value is {@code "idv"}.
     */
    public String getUser() {
        return user;
    }

    /**
     * Return project number, default="0"
     *
     * @return Project number. Default value is {@code "0"}.
     */
    public String getProj() {
        return proj;
    }

    /**
     * Change the user id and project number for this instance.
     *
     * @param user New user ID.
     * @param proj New project number.
     */
    public void setUserProj(String user, String proj) {
        Element serverRoot = serversXRC.getWritableRoot("<tabs></tabs>");
        Document serverDocument = serversXRC.getWritableDocument("<tabs></tabs>");
        try {
            serverRoot.setAttribute(ATTR_USER, user);
            serverRoot.setAttribute(ATTR_PROJ, proj);
            serversXRC.setWritableDocument(serverDocument, serverRoot);
            serversXRC.writeWritable();
        } catch (Exception e) {
            System.out.println("updateXml AddeServer.toXml e=" + e);
        }
    }

    /**
     * Get the different {@literal "types"} supported by the server.
     *
     * @return A list whose possible values may contain {@literal "image",
     * "point", "grid", "text", "nav"}.
     */
    public List getServerTypes() {
        for (int i=0; i<defTypes.length; i++) {
            if (!typeList.contains(defTypes[i])) typeList.add(defTypes[i]);
        }
        return typeList;
    }

    /**
     * Get list of {@link ServerDescriptor ServerDescriptors} that match the
     * given {@code type}.
     *
     * @param type Data type. Should be one of
     *             {@literal "image", "point", "grid", "text", "nav"}.
     * @param all Includes all definitions, even duplicates.
     * @param includeDuplicates Whether or not duplicate definitions are
     *                          included.
     *
     * @return List of {@code ServerDescriptor} objects that match
     *         {@code type}.
     */
    public List getServers(String type, boolean all, boolean includeDuplicates) {
        init();
        List servers = new ArrayList();
        List sds = new ArrayList();
        groups = new ArrayList();
        for (int i=0; i<serverDescriptors.size(); i++) {
            ServerDescriptor sd = (ServerDescriptor)serverDescriptors.get(i);
            if (sd.isDataType(type)) {
                String name = sd.getServerName();
                if (!all) {
                    if (!sd.getIsActive()) continue;
                    if (!includeDuplicates) {
                        if (servers.contains(name)) continue;
                    }
                }
                servers.add(name);
                sds.add(sd);
                groups.add(sd.getGroupName());
            }
        }
        return sds;
    }

    /**
     * Get list of server names that match {@code type}.
     *
     * @param type Data type. Should be one of
     *             {@literal "image", "point", "grid", "text", "nav"}.
     * @param all Includes all definitions, even duplicates.
     * @param includeDuplicates Whether or not duplicate definitions are
     *                          included.
     *
     * @return List of server name strings.
     */
     public List getServerNames(String type, boolean all, boolean includeDuplicates) {
        if (serverDescriptors == null) init();
        List servers = new ArrayList();
        if (typeList.contains(type)) {
            for (int i=0; i<serverDescriptors.size(); i++) {
                ServerDescriptor sd = (ServerDescriptor)serverDescriptors.get(i);
                if (sd.isDataType(type)) {
                    String name = sd.getServerName();
                    if (!all) {
                        if (!sd.getIsActive()) continue;
                        if (!includeDuplicates) {
                            if (servers.contains(name)) continue;
                        }
                    }
                    servers.add(name);
                }
            }
        }
        return servers;
     }

    /**
     * Get list of {@link AddeServer AddeServers} that match {@code type}.
     *
     * @param type Data type. Should be one of
     *             {@literal "image", "point", "grid", "text", "nav"}.
     * @param all Includes all definitions, even duplicates.
     * @param includeDuplicates Whether or not duplicate definitions are
     *                          included.
     *
     * @return List of {@code AddeServer} objects.
     */
     public List getAddeServers(String type, boolean all, boolean includeDuplicates) {
        if (serverDescriptors == null) init();
        List servers = new ArrayList();
        List names = new ArrayList();
        if (typeList.contains(type)) {
            for (int i=0; i<serverDescriptors.size(); i++) {
                ServerDescriptor sd = (ServerDescriptor)serverDescriptors.get(i);
                if (sd.isDataType(type)) {
                    String name = sd.getServerName();
                    if (!all) {
                        if (!sd.getIsActive()) continue;
                    }
                    if (!includeDuplicates) {
                        if (names.contains(name)) continue;
                    }
                    AddeServer as = new AddeServer(sd.getServerName());
                    AddeServer.Group g = new AddeServer.Group(sd.getDataType(), sd.getGroupName(), "");
                    g.setActive(sd.getIsActive());
                    as.addGroup(g);
                    servers.add(as);
                    names.add(as.getName());
                }
            }
        }
        return servers;
     }

    /**
     * Re-reads {@code servers.xml} and re-initialize all lists.
     *
     * @param type Data type. Should be one of
     *             {@literal "image", "point", "grid", "text", "nav"}.
     * @param all Includes all definitions, even duplicates.
     * @param includeDuplicates Whether or not duplicate definitions are
     *                          included.
     *
     * @return List of server name strings that support {@code type} data.
     */
     public List updateAddeServers(String type, boolean all, boolean includeDuplicates) {
         init();
         return getAddeServers(type, all, includeDuplicates);
     }

    /**
     * Read servers.xml and initialize all lists.
     *
     * @return {@code false} if {@link #serverDescriptors} is {@code null},
     * {@code true} otherwise.
     */
    private boolean init() {
       serversXRC =
           myIdv.getResourceManager().getXmlResources(
           IdvResourceManager.RSC_ADDESERVER);
       getServersFromXml(serversXRC);
       if (serverDescriptors == null) return false;
       return true;
    }

    /**
     * Get all group names for a given data {@code type}.
     *
     * @param type Data type. Should be one of
     *             {@literal "image", "point", "grid", "text", "nav"}.
     *
     * @return List of groups that support {@code type} data.
     */
    public List getGroups(String type) {
        init();
        getServers(type, false, true);
        return groups;
    }


    /**
     * Adds {@code serverList} to {@code servers.xml}.
     *
     * @param idv Reference to main application object.
     * @param serverList alternating strings: name1, group1, name2, group2...
     */
    public void addServers(IntegratedDataViewer idv, List serverList) {
        int num = serverList.size();
        List addeServers = new ArrayList();
        if (num > 0) {
            try {
                for (int i=0; i<num; i++) {
                    ServerDescriptor sd = (ServerDescriptor) serverList.get(i);
                    String servName = sd.getServerName();
                    AddeServer as = new AddeServer(sd.getServerName());
                    AddeServer.Group g = new AddeServer.Group(sd.getDataType(), sd.getGroupName(), "");
                    g.setActive(sd.getIsActive());
                    as.addGroup(g);
                    addeServers.add(as);
                }
            } catch (Exception e) {
                System.out.println("addServers e=" + e);
            };
        }
        List serversFinal = AddeServer.coalesce(addeServers);

        XmlResourceCollection serverCollection =
           idv.getResourceManager().getXmlResources(
           IdvResourceManager.RSC_ADDESERVER);
        Element serverRoot = serverCollection.getWritableRoot("<tabs></tabs>");
        Document serverDocument = serverCollection.getWritableDocument("<tabs></tabs>");
        try {
            Element serversEle = AddeServer.toXml(serversFinal, false);
            serverCollection.setWritableDocument(serverDocument, serversEle);
            serverCollection.writeWritable();
        } catch (Exception e) {
            System.out.println("AddeServer.toXml e=" + e);
        }
    }

    /**
     * Clear {@code servers.xml}.
     *
     * @param serversXRC Collection of server definitions.
     */
    public void clear(XmlResourceCollection serversXRC) {
        List typeElements = XmlUtil.getElements(serversRoot, TAG_SERVER);
        for (int typeIdx = 0; typeIdx < typeElements.size(); typeIdx++) {
            Element typeElement = (Element) typeElements.get(typeIdx);
            XmlUtil.removeChildren(typeElement);
        }
        try {
            serversXRC.writeWritable();
        } catch (Exception e) {
        }
        serversXRC.setWritableDocument(serversDocument, serversRoot);
    }
}
