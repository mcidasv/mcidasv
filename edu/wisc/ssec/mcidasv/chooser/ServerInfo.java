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
 * Class ServerInfo Holds has methods for accessing
 * the contents of servers.xml
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
     * Constructor
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
     * getServersFromXml
     *  Read and parse servers.xml
     *
     *  fill lists:
     *    typeList - type name Strings
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
     * getUser
     *   return userId, default="idv"
     */
    public String getUser() {
        return user;
    }

    /**
     * getProj
     *   return project number, default="0"
     */
    public String getProj() {
        return proj;
    }

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
     * getServerTypes
     *    return List of type Strings
     */
    public List getServerTypes() {
        for (int i=0; i<defTypes.length; i++) {
            if (!typeList.contains(defTypes[i])) typeList.add(defTypes[i]);
        }
        return typeList;
    }

    /**
     * getServers
     *    input: type = data type
     *           all = boolean flag
     *           includeDuplicates = boolean flag
     *    return List of ServerDescriptors
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
     * getServerNames
     *    input: type = data type
     *           all = boolean flag
     *           includeDuplicates = boolean flag
     *    return List of server name strings
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
     * getAddeServers
     *    input: type = data type
     *           all = boolean flag
     *           includeDuplicates = boolean flag
     *    return List of server name strings
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
     * updateAddeServers(String type, boolean all, boolean includeDuplicates) {
     *    input: type = data type
     *           all = boolean flag
     *           includeDuplicates = boolean flag
     *    return List of server name strings
     */
     public List updateAddeServers(String type, boolean all, boolean includeDuplicates) {
         init();
         return getAddeServers(type, all, includeDuplicates);
     }

    /**
     * init
     *    read servers.xml and initialize all Lists
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
     * getGroups - return all groups for a given data type
     *    return List of group name Strings
     */
    public List getGroups(String type) {
        init();
        getServers(type, false, true);
        return groups;
    }


    /**
     * addServers to servers.xml
     *   input: type = data type
     *          serverList = alternating strings: name1, group1, name2, group2, etc.
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
     * Clear servers.xml
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
