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

    private List imageServers = new ArrayList();
    private List pointServers = new ArrayList();
    private List gridServers = new ArrayList();
    private List textServers = new ArrayList();
    private List navServers = new ArrayList();

    /**
     * Constructor
     *
     *
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
     * _more_
     *
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
                    String name = XmlUtil.getAttribute(serverElement, ATTR_NAME);
                    String group = XmlUtil.getAttribute(serverElement, ATTR_GROUP);
                    TwoFacedObject tfo = new TwoFacedObject(typeName, name);
                    TwoFacedObject serv = new TwoFacedObject(name, group);
                    allServers.add(tfo);
                    if (typeName.equals("image"))
                        imageServers.add(serv);
                    else if (typeName.equals("point"))
                        pointServers.add(serv);
                    else if (typeName.equals("grid"))
                        gridServers.add(serv);
                    else if (typeName.equals("text"))
                        textServers.add(serv);
                    else if (typeName.equals("nav"))
                        navServers.add(serv);
                }
            }
        }
        return;
    }

    public String getUser() {
        return user;
    }

    public String getProj() {
        return proj;
    }

    public List getServerTypes() {
        return typeList;
    }

    public List getServers() {
        return allServers;
    }

    public List getImageServers() {
        return imageServers;
    }

    public List getPointServers() {
        return pointServers; 
    }
 
    public List getGridServers() {
        return gridServers; 
    }
 
    public List getTextServers() {
        return textServers; 
    }

    public List getNavServers() {
        return navServers;
    }

    public List getServers(String type) {
        List servers = new ArrayList();
        if (typeList == null) {
           serversXRC =
               myIdv.getResourceManager().getXmlResources(
               IdvResourceManager.RSC_SERVERS);
           getServerGroupNames(serversXRC);
           if (typeList == null) return servers;
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

    public void addServers(String type, List serverList) {
        int num = serverList.size();
        if (num > 0) {
            try {
                for (int i=0; i<num; i+=2) {
                    Element tempRoot = XmlUtil.findElement(serversRoot, TAG_TYPE,
                         ATTR_NAME, type);
                    Element tempElement = serversDocument.createElement(TAG_SERVER);
                    String[] tempString = {ATTR_NAME, "", ATTR_GROUP, ""};
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
    } 
}
