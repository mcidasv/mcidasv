package edu.wisc.ssec.mcidasv.chooser;


import edu.wisc.ssec.mcidasv.*;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.IdvResourceManager;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;


/**
 * Class ServerInfo Holds has methods for accessing
 * the contents of servers.xml
 */
public class ServerInfo {

    private IntegratedDataViewer myIdv;

    /** Default value for the user property */
    protected static String DEFAULT_USER = "idv";

    /** Default value for the proj property */
    protected static String DEFAULT_PROJ = "0";

    /** tags */
    public static final String TAG_TYPE = "type";
    public static final String TAG_SERVER = "server";
    public static final String TAG_USERID = "userID";

    /** attributes */
    public static final String ATTR_ACTIVE = "active";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_GROUP = "group";
    public static final String ATTR_USER = "user";
    public static final String ATTR_PROJ = "proj";

    private XmlResourceCollection serversXRC;
    private Document serversDocument;
    private Element serversRoot;

    private String user;
    private String proj;

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
                serversXRC.getWritableDocument("<tabs></tabs>");
            serversRoot =
                serversXRC.getWritableRoot("<tabs></tabs>");
            String tagName = serversRoot.getTagName();
            if (!tagName.equals("servers")) {
                serversRoot = serversDocument.createElement("servers");
                Element tempElement = serversDocument.createElement("userID");
                String[] tempString = {"proj", "0000", "user", "MCV"};
                XmlUtil.setAttributes(tempElement, tempString);
                serversRoot.appendChild(tempElement);
                String[] attrString = {"name", ""};
                for (int i=0; i<defTypes.length; i++) {
                    tempElement = serversDocument.createElement("type");
                    attrString[1] = defTypes[i];
                    XmlUtil.setAttributes(tempElement, attrString);
                    serversRoot.appendChild(tempElement);
                }
            }
        }
        getServersFromXml(serversXRC);
    }


    /**
     * getServerGroupNames
     *  Read and parse servers.xml
     *
     *  fill lists:
     *    typeList - type name Strings
     */
    private void getServersFromXml(XmlResourceCollection servers) {
        for (int resourceIdx = 0; resourceIdx < servers.size();
                resourceIdx++) {
            try {
                Element root = servers.getRoot(resourceIdx);
                if (root == null) {
                    continue;
                }
                if ((user == null) || (proj == null)) {
                    Element accountElement = XmlUtil.getElement(root, TAG_USERID);
                    user = XmlUtil.getAttribute(accountElement, ATTR_USER);
                    if (user == null) user = DEFAULT_USER;
                    proj = XmlUtil.getAttribute(accountElement, ATTR_PROJ);
                    if (proj == null) proj = DEFAULT_PROJ;
                }
                List typeElements = XmlUtil.getElements(root, TAG_TYPE);
                for (int typeIdx = 0; typeIdx < typeElements.size(); typeIdx++) {
                    Element typeElement = (Element) typeElements.get(typeIdx);
                    String typeName = XmlUtil.getAttribute(typeElement, ATTR_NAME);
                    if (!typeList.contains(typeName)) {
                        typeList.add(typeName);
                        List serverElements = XmlUtil.getElements(typeElement, TAG_SERVER);
                        for (int serverIdx = 0; serverIdx < serverElements.size(); serverIdx++) {
                            Element serverElement = (Element) serverElements.get(serverIdx);
                            String active = XmlUtil.getAttribute(serverElement, ATTR_ACTIVE);
                            String name = XmlUtil.getAttribute(serverElement, ATTR_NAME);
                            String group = XmlUtil.getAttribute(serverElement, ATTR_GROUP);
                            ServerDescriptor sd =
                                new ServerDescriptor(typeName, name, group, active);
                            serverDescriptors.add(sd);
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        return;
    }

    /**
     * getUser
     *   return userId, default="idv"
     */
    public String getUser() {
        if (user==null) user = DEFAULT_USER;
        return user;
    }

    /**
     * getProj
     *   return project number, default="0"
     */
    public String getProj() {
        if (proj == null) proj = DEFAULT_PROJ;
        return proj;
    }

    /**
     * getServerTypes
     *    return List of type Strings
     */
    public List getServerTypes() {
        if (typeList==null) init();
        return typeList;
    }

    /**
     * getServers
     *    input: type = data type
     *    return List of ServerDescriptors
     */
    public List getServers(String type, boolean all, boolean includeDuplicates) {
        if (serverDescriptors == null) init();
        List servers = new ArrayList();
        List sds = new ArrayList();
        groups = new ArrayList();
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
                    sds.add(sd);
                    groups.add(sd.getGroupName());
                }
            }
        }
        return sds;
    }

    /**
     * init
     *    read servers.xml and initialize all Lists
     */
    private boolean init() {
       serversXRC =
           myIdv.getResourceManager().getXmlResources(
           McIDASV.RSC_SERVERS);
       getServersFromXml(serversXRC);
       if (serverDescriptors == null) return false;
       return true;
    }

    /**
     * getGroups - return all groups for a given data type
     *    return List of group name Strings
     */
    public List getGroups(String type) {
        if (serverDescriptors == null) init();
        getServers(type, false, true);
        return groups;
    }

    /**
     * addServers to servers.xml
     *   input: type = data type
     *          serverList = alternating strings: name1, group1, name2, group2, etc.
     */
    public void addServers(String type, List serverList) {
        int num = serverList.size();
        if (num > 0) {
            try {
                for (int i=0; i<num; i++) {
                    Element tempRoot = XmlUtil.findElement(serversRoot, TAG_TYPE,
                         ATTR_NAME, type);
                    Element tempElement = serversDocument.createElement(TAG_SERVER);
                    String[] tempString = {ATTR_NAME, "", ATTR_GROUP, "", ATTR_ACTIVE, ""};
                    ServerDescriptor sd = (ServerDescriptor) serverList.get(i);
                    tempString[1] = sd.getServerName();
                    tempString[3] = sd.getGroupName();
                    if (sd.getIsActive()) {
                        tempString[5] = "true";
                    } else {
                        tempString[5] = "false";
                    }
                    XmlUtil.setAttributes(tempElement, tempString);
                    tempRoot.appendChild(tempElement);
                }
            } catch (Exception e) {
                System.out.println("addServers e=" + e);
            };
        }

        try {
            serversXRC.writeWritable();
        } catch (Exception e) {
            System.out.println("writeXml e=" + e);
        }
        serversXRC.setWritableDocument(serversDocument, serversRoot);

    }

    /**
     * Clear servers.xml
     */
    public void clear() {
        List typeElements = XmlUtil.getElements(serversRoot, TAG_TYPE);
        for (int typeIdx = 0; typeIdx < typeElements.size(); typeIdx++) {
            Element typeElement = (Element) typeElements.get(typeIdx);
            XmlUtil.removeChildren(typeElement);
        }
        try {
            serversXRC.writeWritable();
        } catch (Exception e) {
            System.out.println("writeXml e=" + e);
        }
        serversXRC.setWritableDocument(serversDocument, serversRoot);
    }
}
