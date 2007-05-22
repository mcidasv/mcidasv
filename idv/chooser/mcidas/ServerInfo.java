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

    private List imageServers = new ArrayList();
    private List pointServers = new ArrayList();
    private List gridServers = new ArrayList();
    private List textServers = new ArrayList();
    private List navServers = new ArrayList();

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
        getServerGroupNames(serversXRC);
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
    private void getServerGroupNames(XmlResourceCollection servers) {
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
                    TwoFacedObject tfo = new TwoFacedObject(typeName, name);
                    TwoFacedObject serv = new TwoFacedObject(name, group);
                    allServers.add(tfo);
                    serverStatus.add(active);
                    if (typeName.equals("image"))
                        if (active.equals("true")) imageServers.add(serv);
                    else if (typeName.equals("point"))
                        if (active.equals("true")) pointServers.add(serv);
                    else if (typeName.equals("grid"))
                        if (active.equals("true")) gridServers.add(serv);
                    else if (typeName.equals("text"))
                        if (active.equals("true")) textServers.add(serv);
                    else if (typeName.equals("nav"))
                        if (active.equals("true")) navServers.add(serv);
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
     * getImageServers
     *    return List of TwoFacedObjects: Id=server name String, Label=group name String
     */
    public List getImageServers() {
        if (imageServers==null) init();
        return imageServers;
    }

    /**
     * getPointServers
     *    return List of TwoFacedObjects: Id=server name String, Label=group name String
     */
    public List getPointServers() {
        if (pointServers==null) init();
        return pointServers; 
    }
 
    /**
     * getGridServers
     *    return List of TwoFacedObjects: Id=server name String, Label=group name String
     */
    public List getGridServers() {
        if (gridServers==null) init();
        return gridServers; 
    }
 
    /**
     * getTextServers
     *    return List of TwoFacedObjects: Id=server name String, Label=group name String
     */
    public List getTextServers() {
        if (textServers==null) init();
        return textServers; 
    }

    /**
     * getNavServers
     *    return List of TwoFacedObjects: Id=server name String, Label=group name String
     */
    public List getNavServers() {
        if (navServers==null) init();
        return navServers;
    }

    /**
     * init
     *    read servers.xml and initialize all Lists
     */
    private boolean init() {
       serversXRC =
           myIdv.getResourceManager().getXmlResources(
           IdvResourceManager.RSC_SERVERS);
       getServerGroupNames(serversXRC);
       if (typeList==null) return false;
       return true;
    }

    /**
     * getServers - return all servers of a given data type
     *    return List of server name Strings
     */
    public List getServers(String type) {
        List servers = new ArrayList();
        if (typeList == null) {
            if (!init()) return servers;
        }
        if (typeList.contains(type)) {
            for (int i=0; i<allServers.size(); i++) {
                TwoFacedObject tfo = (TwoFacedObject)allServers.get(i);
                String typeString = (String) tfo.getLabel();
                if (typeString.equals(type)) {
                    String name = (String) tfo.getId();
                    if (!servers.contains(name))
                        servers.add(name);
                }
            }
        }
        return servers;
    }

    /**
     * getGroups - return all groups for a given data type
     *    return List of group name Strings
     */
    public List getGroups(String type) {
        List groups = new ArrayList();
        List servers;
        if (typeList == null) {
            if (!init()) return groups;
        }
        if (type.equals("image")) servers= imageServers;
        else if (type.equals("point")) servers= pointServers;
        else if (type.equals("grid")) servers= gridServers;
        else if (type.equals("text")) servers= textServers;
        else if (type.equals("nav")) servers= navServers;
        else return groups;
        for (int i=0; i<servers.size(); i++) {
            TwoFacedObject tfo = (TwoFacedObject)servers.get(i);
            String groupString = (String) tfo.getId();
            if (!groups.contains(groupString)) {
                groups.add(groupString);
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
                    tempString[1] = (String) serverList.get(i);
                    tempString[3] = (String) serverList.get(i+1);
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
}
