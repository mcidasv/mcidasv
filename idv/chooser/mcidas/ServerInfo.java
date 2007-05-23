package ucar.unidata.idv.chooser.mcidas;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.IdvResourceManager;

import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.List;

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
    private List allServers = new ArrayList();
    private List serverStatus = new ArrayList();

    private List serverDescriptors = new ArrayList();

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
        }
        getServersFromXml(serversXRC);
    }


    /**
     * getServerGroupNames
     *  Read and parse servers.xml
     *
     *  fill lists:
     *    typeList - type name Strings
     *    allServers - TwoFacedObjects: Id=type String, Label=server name or IP address
     *    seperate lists for image, point, grid, text and nav types
     *       These are TwoFacedObject lists: Id=server name or IP address, Label=group name
     */
    private void getServersFromXml(XmlResourceCollection servers) {
        for (int resourceIdx = 0; resourceIdx < servers.size();
                resourceIdx++) {
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
                    TwoFacedObject tfo = new TwoFacedObject(typeName, name);
                    TwoFacedObject serv = new TwoFacedObject(name, group);
                    allServers.add(tfo);
                    serverStatus.add(active);
                }
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
     *    return List of TwoFacedObjects: Id=type String, Label=server name String
     */
    public List getServers() {
        if (allServers==null) init();
        return allServers;
    }

    /**
     * getServerStatus
     *    return List of status Strings for allServers
     */
    public List getServerStatus() {
        if (allServers==null) init();
        return serverStatus;
    }

    /**
     * getServers
     *    input: type = data type
     *    return List of ServerDescriptors
     */
    public List getServers(String type) {
        if (serverDescriptors == null) init();
        List servers = new ArrayList();
        if (typeList.contains(type)) {
            for (int i=0; i<serverDescriptors.size(); i++) {
                ServerDescriptor sd = (ServerDescriptor)serverDescriptors.get(i);
                if (sd.isDataType(type)) servers.add(sd);
            }
        }
        return servers;
    }

    /**
     * init
     *    read servers.xml and initialize all Lists
     */
    private boolean init() {
       serversXRC =
           myIdv.getResourceManager().getXmlResources(
           IdvResourceManager.RSC_SERVERS);
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
        List groups = new ArrayList();
        if (typeList.contains(type)) {
            for (int i=0; i<serverDescriptors.size(); i++) {
                ServerDescriptor sd = (ServerDescriptor)serverDescriptors.get(i);
                if (sd.isDataType(type)) groups.add(sd.getGroupName());
            }
        }
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
                for (int i=0; i<num; i+=2) {
                    Element tempRoot = XmlUtil.findElement(serversRoot, TAG_TYPE,
                         ATTR_NAME, type);
                    Element tempElement = serversDocument.createElement(TAG_SERVER);
                    String[] tempString = {ATTR_NAME, "", ATTR_GROUP, "", ATTR_ACTIVE, "true"};
                    ServerDescriptor sd = (ServerDescriptor) serverList.get(i);
                    tempString[1] = sd.getServerName();
                    tempString[3] = sd.getGroupName();
                    XmlUtil.setAttributes(tempElement, tempString);
                    tempRoot.appendChild(tempElement);
                }
            } catch (Exception e) {};
        }
        try {
            serversXRC.writeWritable();
        } catch (Exception e) {
            System.out.println("writeXml e=" + e);
        }
        serversXRC.setWritableDocument(serversDocument, serversRoot);
        init();
    }

    /**
     * writeAllServers to servers.xml
     */
    public void updateServersXml(List serverList) {
    }
}
