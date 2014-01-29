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

package edu.wisc.ssec.mcidasv;

import static ucar.unidata.xml.XmlUtil.getAttribute;
import static ucar.unidata.xml.XmlUtil.getChildText;
import static ucar.unidata.xml.XmlUtil.getElements;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.StateManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.ResourceCollection.Resource;
import ucar.unidata.util.StringUtil;

/**
 * McIDAS-V's resource manager. The chief differences from Unidata's
 * {@link IdvResourceManager} are supporting {@literal "default"} McIDAS-V
 * bundles, and some initial attempts at safer resource handling.
 */
public class ResourceManager extends IdvResourceManager {

    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);
    
    /** Points to the adde image defaults */
    public static final XmlIdvResource RSC_PARAMETERSETS =
        new XmlIdvResource("idv.resource.parametersets",
                           "Chooser Parameter Sets", "parametersets\\.xml$");

    public static final IdvResource RSC_SITESERVERS =
        new XmlIdvResource("mcv.resource.siteservers", 
            "Site-specific Servers", "siteservers\\.xml$");

    public static final IdvResource RSC_NEW_USERSERVERS =
        new XmlIdvResource("mcv.resource.newuserservers", 
            "New style user servers", "persistedservers\\.xml$");

    public static final IdvResource RSC_OLD_USERSERVERS =
        new XmlIdvResource("mcv.resource.olduserservers", 
            "Old style user servers", "addeservers\\.xml$");

    public ResourceManager(IntegratedDataViewer idv) {
        super(idv);
        checkMoveOutdatedDefaultBundle();
    }

    /**
     * Overridden so that McIDAS-V can attempt to verify {@literal "critical"}
     * resources without causing crashes. 
     * 
     * <p>Currently doesn't do a whole lot.
     * 
     * @see #verifyResources()
     */
    @Override protected void init(List rbiFiles) {
        super.init(rbiFiles);
//        verifyResources();
    }

    /**
     * Loops through all of the {@link ResourceCollection}s that the IDV knows
     * about. 
     * 
     * <p>I realize that this could balloon into a really tedious thing...
     * there could potentially be verification steps for each type of resource
     * collection! the better approach is probably to identify a few key collections
     * (like the (default?) maps).
     */
    protected void verifyResources() {
        List<IdvResource> resources = new ArrayList<IdvResource>(getResources());
        for (IdvResource resource : resources) {
            ResourceCollection rc = getResources(resource);
            logger.trace("Resource ID='{}'", resource);
            for (int i = 0; i < rc.size(); i++) {
                String path = (String)rc.get(i);
                logger.trace("  path='{}' pathexists={}", path, isPathValid(path));
            }
        }
    }

    /**
     * Pretty much relies upon {@link IOUtil#getInputStream(String, Class)}
     * to determine if {@code path} exists.
     * 
     * @param path Path to an arbitrary file. It can be a remote URL, normal
     * file on disk, or a file included in a JAR. Just so long as it's not 
     * {@code null}!
     * 
     * @return {@code true} <i>iff</i> there were no problems. {@code false}
     * otherwise.
     */
    private boolean isPathValid(final String path) {
        InputStream s = null;
        boolean isValid = false;
        try {
            s = IOUtil.getInputStream(path, getClass());
            isValid = (s != null);
        } catch (IOException e) {
            isValid = false;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    logger.trace("could not close InputStream associated with "+path, e);
                }
            }
        }
        return isValid;
    }

    /**
     * Adds support for McIDAS-V macros. Specifically:
     * <ul>
     *   <li>{@link Constants#MACRO_VERSION}</li>
     * </ul>
     * 
     * @param path Path that contains a macro to be translated.
     * 
     * @return Resource with our macros applied.
     * 
     * @see IdvResourceManager#getResourcePath(String)
     */
    @Override public String getResourcePath(String path) {
        String retPath = path;
        if (path.contains(Constants.MACRO_VERSION)) {
            retPath = StringUtil.replace(
                path, 
                Constants.MACRO_VERSION, 
                ((edu.wisc.ssec.mcidasv.StateManager)getStateManager()).getMcIdasVersion());
        } else {
            retPath = super.getResourcePath(path);
        }
        return retPath;
    }

    /**
     * Look for existing "default.mcv" and "default.xidv" bundles in root userpath
     * If they exist, move them to the "bundles" directory, preferring "default.mcv"
     */
    private void checkMoveOutdatedDefaultBundle() {
        String userDirectory = getIdv().getObjectStore().getUserDirectory().toString();

        File defaultDir;
        File defaultNew;
        File defaultIdv;
        File defaultMcv;

        String os = System.getProperty("os.name");
        if (os == null) {
            throw new RuntimeException();
        }
        
        if (os.startsWith("Windows")) {
            defaultDir = new File(userDirectory + "\\bundles\\General");
            defaultNew = new File(defaultDir.toString() + "\\Default.mcv");
            defaultIdv = new File(userDirectory + "\\default.xidv");
            defaultMcv = new File(userDirectory + "\\default.mcv");
        } else {
            defaultDir = new File(userDirectory + "/bundles/General");
            defaultNew = new File(defaultDir.toString() + "/Default.mcv");
            defaultIdv = new File(userDirectory + "/default.xidv");
            defaultMcv = new File(userDirectory + "/default.mcv");
        }

        // If no Alpha default bundles exist, bail quickly
        if (!defaultIdv.exists() && !defaultMcv.exists()) {
            return;
        }

        // If the destination directory does not exist, create it.
        if (!defaultDir.exists()) {
            if (!defaultDir.mkdirs()) {
                logger.warn("Cannot create directory '{}' for default bundle", defaultDir);
                return;
            }
        }

        // If the destination already exists, print lame error message and bail.
        // This whole check should only happen with Alphas so no biggie right?
        if (defaultNew.exists()) {
            logger.warn("Cannot copy current default bundle: '{}' already exists.", defaultNew);
            return;
        }

        // If only default.xidv exists, try to rename it.
        // If both exist, delete the default.xidv file.  It was being ignored anyway.
        if (defaultIdv.exists()) {
            if (defaultMcv.exists()) {
                defaultIdv.delete();
            } else {
                if (!defaultIdv.renameTo(defaultNew)) {
                    logger.warn("Cannot copy current default bundle: error renaming '{}'", defaultIdv);
                }
            }
        }

        // If only default.mcv exists, try to rename it.
        if (defaultMcv.exists()) {
            if (!defaultMcv.renameTo(defaultNew)) {
                logger.warn("Cannot copy current default bundle: error renaming '{}'", defaultMcv);
            }
        }
    }

    /**
     * Checks an individual map resource (typically from {@code RSC_MAPS}) to
     * verify that all of the specified maps exist?
     * 
     * <p>Currently a no-op. The intention is to return a {@code List} so that the
     * set of missing resources can eventually be sent off in a support 
     * request...
     * 
     * <p>We could also decide to allow the user to search the list of plugins
     * or ignore any missing resources (simply remove the bad stuff from the list of available xml).
     * 
     * @param path Path to a map resource. URLs are allowed, but {@code null} is not.
     * 
     * @return List of map paths that could not be read. If there were no 
     * errors the list is merely empty.
     * 
     * @see IdvResourceManager#RSC_MAPS
     */
    private List<String> getInvalidMapsInResource(final String path) {
        List<String> invalidMaps = new ArrayList<String>();
        return invalidMaps;
    }

    /**
     * Returns either a {@literal "normal"} {@link ResourceCollection} or a
     * {@link ucar.unidata.xml.XmlResourceCollection XmlResourceCollection},
     * based upon {@code rsrc}.
     *
     * @param rsrc XML representation of a resource collection. Should not be 
     * {@code null}.
     * @param name The {@literal "name"} to associate with the returned 
     * {@code ResourceCollection}. Should not be {@code null}.
     */
    private ResourceCollection getCollection(final Element rsrc, final String name) {
        ResourceCollection rc = getResources(name);
        if (rc != null) {
            return rc;
        }
        
        if (getAttribute(rsrc, ATTR_RESOURCETYPE, "text").equals("text")) {
            return createResourceCollection(name);
        } else {
            return createXmlResourceCollection(name);
        }
    }

    /**
     * {@literal "Resource"} elements within a RBI file are allowed to have an
     * arbitrary number of {@literal "property"} child elements (or none at 
     * all). The property elements must have {@literal "name"} and 
     * {@literal "value"} attributes.
     * 
     * <p>This method iterates through any property elements and creates a {@link Map}
     * of {@code name:value} pairs.
     * 
     * @param resourceNode The {@literal "resource"} element to examine. Should
     * not be {@code null}. Resources without {@code property}s are permitted.
     * 
     * @return Either a {@code Map} of {@code name:value} pairs or an empty 
     * {@code Map}. 
     */
    private Map<String, String> getNodeProperties(final Element resourceNode) {
        NodeList propertyList = getElements(resourceNode, TAG_PROPERTY);
        Map<String, String> nodeProperties = new LinkedHashMap<String, String>(propertyList.getLength());
        for (int propIdx = 0; propIdx < propertyList.getLength(); propIdx++) {
            Element propNode = (Element)propertyList.item(propIdx);
            String propName = getAttribute(propNode, ATTR_NAME);
            String propValue = getAttribute(propNode, ATTR_VALUE, (String)null);
            if (propValue == null) {
                propValue = getChildText(propNode);
            }
            nodeProperties.put(propName, propValue);
        }
        nodeProperties.putAll(getNodeAttributes(resourceNode));
        return nodeProperties;
    }

    /**
     * Builds an {@code attribute:value} {@link Map} based upon the contents of
     * {@code resourceNode}.
     * 
     * <p><b>Be aware</b> that {@literal "location"} and {@literal "id"} attributes
     * are ignored, as the IDV apparently considers them to be special.
     * 
     * @param resourceNode The XML element to examine. Should not be 
     * {@code null}.
     * 
     * @return Either a {@code Map} of {@code attribute:value} pairs or an 
     * empty {@code Map}.
     */
    private Map<String, String> getNodeAttributes(final Element resourceNode) {
        Map<String, String> nodeProperties = Collections.emptyMap();
        NamedNodeMap nnm = resourceNode.getAttributes();
        if (nnm != null) {
            nodeProperties = new LinkedHashMap<String, String>(nnm.getLength());
            for (int attrIdx = 0; attrIdx < nnm.getLength(); attrIdx++) {
                Attr attr = (Attr)nnm.item(attrIdx);
                String name = attr.getNodeName();
                if (!name.equals(ATTR_LOCATION) && !name.equals(ATTR_ID)) {
                    nodeProperties.put(name, attr.getNodeValue());
                }
            }
        }
        return nodeProperties;
    }

    /**
     * Expands {@code origPath} (if needed) and builds a {@link List} of paths.
     * Paths beginning with {@literal "index:"} or {@literal "http:"} may be in
     * need of expansion.
     * 
     * <p>{@literal "Index"} files contain a list of paths. These paths should
     * be used instead of {@code origPath}.
     * 
     * <p>Files that reside on a webserver (these begin with {@literal "http:"})
     * may be inaccessible for a variety of reasons. McIDAS-V allows a RBI file
     * to specify a {@literal "property"} named {@literal "default"} whose {@literal "value"}
     * is a path to use as a backup. For example:<br/>
     * <pre>
     * &lt;resources name="idv.resource.pluginindex"&gt;
     *   &lt;resource label="Plugin Index" location="http://www.ssec.wisc.edu/mcidas/software/v/resources/plugins/plugins.xml"&gt;
     *     &lt;property name="default" value="%APPPATH%/plugins.xml"/&gt;
     *   &lt;/resource&gt;
     * &lt;/resources&gt;
     * </pre>
     * The {@code origPath} parameter will be the value of the {@literal "location"}
     * attribute. If {@code origPath} is inaccessible, then the path given by
     * the {@literal "default"} property will be used.
     * 
     * @param origPath Typically the value of the {@literal "location"} 
     * attribute associated with a given resource. Cannot be {@code null}.
     * @param props Contains the property {@code name:value} pairs associated with
     * the resource whose path is being examined. Cannot be {@code null}.
     * 
     * @return {@code List} of paths associated with a given resource.
     * 
     * @see #isPathValid(String)
     */
    private List<String> getPaths(final String origPath, 
        final Map<String, String> props) 
    {
        List<String> paths = new ArrayList<String>();
        if (origPath.startsWith("index:")) {
            String path = origPath.substring(6);
            String index = IOUtil.readContents(path, (String)null);
            if (index != null) {
                List<String> lines = StringUtil.split(index, "\n", true, true);
                for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                    String line = lines.get(lineIdx);
                    if (line.startsWith("#")) {
                        continue;
                    }
                    paths.add(getResourcePath(line));
                }
            }
        } else if (origPath.startsWith("http:")) {
            String tmpPath = origPath;
            if (!isPathValid(tmpPath) && props.containsKey("default")) {
                tmpPath = getResourcePath(props.get("default"));
            }
            paths.add(tmpPath);
        } else {
            paths.add(origPath);
        }
        return paths;
    }

    /**
     * Utility method that calls {@link StateManager#fixIds(String)}.
     */
    private static String fixId(final Element resource) {
        return StateManager.fixIds(getAttribute(resource, ATTR_NAME));
    }

    /**
     * Processes the top-level {@literal "root"} of a RBI XML file. Overridden
     * in McIDAS-V so that remote resources can have a backup location.
     * 
     * @param root The {@literal "root"} element. Should not be {@code null}.
     * @param observeLoadMore Whether or not processing should continue if a 
     * {@literal "loadmore"} tag is encountered.
     * 
     * @see #getPaths(String, Map)
     */
    @Override protected void processRbi(final Element root, 
        final boolean observeLoadMore) 
    {
        NodeList children = getElements(root, TAG_RESOURCES);

        for (int i = 0; i < children.getLength(); i++) {
            Element rsrc = (Element)children.item(i);

            ResourceCollection rc = getCollection(rsrc, fixId(rsrc));
            if (getAttribute(rsrc, ATTR_REMOVEPREVIOUS, false)) {
                rc.removeAll();
            }

            if (observeLoadMore && !rc.getCanLoadMore()) {
                continue;
            }

            boolean loadMore = getAttribute(rsrc, ATTR_LOADMORE, true);
            if (!loadMore) {
                rc.setCanLoadMore(false);
            }

            List<Resource> locationList = new ArrayList<Resource>();
            NodeList resources = getElements(rsrc, TAG_RESOURCE);
            for (int idx = 0; idx < resources.getLength(); idx++) {
                Element node = (Element)resources.item(idx);
                String path = getResourcePath(getAttribute(node, ATTR_LOCATION));
                if ((path == null) || (path.isEmpty())) {
                    continue;
                }

                String label = getAttribute(node, ATTR_LABEL, (String)null);
                String id = getAttribute(node, ATTR_ID, (String)null);

                Map<String, String> nodeProperties = getNodeProperties(node);

                for (String p : getPaths(path, nodeProperties)) {
                    if (id != null) {
                        rc.setIdForPath(id, p);
                    }
                    locationList.add(new Resource(p, label, new Hashtable<String, String>(nodeProperties)));
                }
            }
            rc.addResources(locationList);
        }

    }
}
