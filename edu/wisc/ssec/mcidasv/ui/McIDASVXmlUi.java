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

package edu.wisc.ssec.mcidasv.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.IdvXmlUi;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.ui.HtmlComponent;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;

import edu.wisc.ssec.mcidasv.util.TreePanel;

/**
 * <p>
 * McIDAS-V mostly extends this class to preempt the IDV. McIDAS-V needs to
 * control some HTML processing, ensure that {@link McvComponentGroup}s
 * and {@link McvComponentHolder}s are created, and handle some special
 * problems that occur when attempting to load bundles that do not contain
 * component groups.
 * </p>
 */
@SuppressWarnings("unchecked") 
public class McIDASVXmlUi extends IdvXmlUi {

    /**
     *  Maps an ID to an {@link Element}.
     */
//    private Hashtable<String, Element> idToElement;
    private Map<String, Element> idToElement;

    /** Avoids unneeded getIdv() calls. */
    private IntegratedDataViewer idv;

    /**
     * Keep around a reference to the window we were built for, useful for
     * associated component groups with the appropriate window.
     */
    private IdvWindow window;

    public McIDASVXmlUi(IntegratedDataViewer idv, Element root) {
        super(idv, root);
        if (idToElement == null) {
            idToElement = new HashMap<String, Element>();
        }
    }

    public McIDASVXmlUi(IdvWindow window, List viewManagers,
        IntegratedDataViewer idv, Element root) 
    {
        super(window, viewManagers, idv, root);
        this.idv = idv;
        this.window = window;
        if (idToElement == null) {
            idToElement = new HashMap<String, Element>();
        }
    }

    /**
     * Convert the &amp;gt; and &amp;lt; entities to &gt; and &lt;.
     * 
     * @param text The text you'd like to convert.
     * 
     * @return The converted text!
     */
    private static String decodeHtml(String text) {
        return text.replace("&gt", ">").replace("&lt;", ">");
    }

    /**
     * Add the component
     * 
     * @param id id
     * @param component component
     */
    @Override public void addComponent(String id, Element component) {
        // this needs to be here because even if you create idToElement in the
        // constructor, this method will get called from 
        // ucar.unidata.xml.XmlUi#initialize(Element) before control has 
        // returned to the McIDASVXmlUi constructor!
        if (idToElement == null) {
            idToElement = new HashMap<String, Element>();
        }
        super.addComponent(id, component);
        idToElement.put(id, component);
    }

    /**
     * <p>
     * Overridden so that any attempts to generate
     * {@link IdvComponentGroup}s or {@link IdvComponentHolder}s will return 
     * the respective McIDAS-V equivalents.
     * </p>
     * 
     * <p>
     * It makes things like the draggable tabs possible.
     * </p>
     * 
     * @param node The XML representation of the desired component group.
     * 
     * @return An honest-to-goodness McIDASVComponentGroup based upon the
     *         contents of <code>node</code>.
     * 
     * @see ucar.unidata.idv.ui.IdvXmlUi#makeComponentGroup(Element)
     */
    @Override protected IdvComponentGroup makeComponentGroup(Element node) {
        McvComponentGroup group = new McvComponentGroup(idv, "", window);
        group.initWith(node);

        NodeList elements = XmlUtil.getElements(node);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element)elements.item(i);

            String tag = child.getTagName();

            if (tag.equals(IdvUIManager.COMP_MAPVIEW)
                || tag.equals(IdvUIManager.COMP_VIEW)) 
            {
                ViewManager viewManager = getViewManager(child);
                group.addComponent(new McvComponentHolder(idv, viewManager));
            } 
            else if (tag.equals(IdvUIManager.COMP_COMPONENT_CHOOSERS)) {
                IdvComponentHolder comp =
                    new McvComponentHolder(idv, "choosers");
                comp.setType(IdvComponentHolder.TYPE_CHOOSERS);
                comp.setName(XmlUtil.getAttribute(child, "name", "Choosers"));
                group.addComponent(comp);
            } 
            else if (tag.equals(IdvUIManager.COMP_COMPONENT_SKIN)) {
                IdvComponentHolder comp = new McvComponentHolder(idv, 
                    XmlUtil.getAttribute(child, "url"));

                comp.setType(IdvComponentHolder.TYPE_SKIN);
                comp.setName(XmlUtil.getAttribute(child, "name", "UI"));
                group.addComponent(comp);
            } 
            else if (tag.equals(IdvUIManager.COMP_COMPONENT_HTML)) {
                String text = XmlUtil.getChildText(child);
                text = new String(XmlUtil.decodeBase64(text.trim()));
                ComponentHolder comp = new HtmlComponent("Html Text", text);
                comp.setShowHeader(false);
                comp.setName(XmlUtil.getAttribute(child, "name", "HTML"));
                group.addComponent(comp);
            } 
            else if (tag.equals(IdvUIManager.COMP_DATASELECTOR)) {
                group.addComponent(new McvComponentHolder(idv,
                    idv.getIdvUIManager().createDataSelector(false, false)));
            } 
            else if (tag.equals(IdvUIManager.COMP_COMPONENT_GROUP)) {
                group.addComponent(makeComponentGroup(child));
            } 
            else {
                System.err.println("Unknown component element:"
                                   + XmlUtil.toString(child));
            }
        }
        return group;
    }

    /**
     * <p>
     * McIDAS-V overrides this so that it can seize control of some HTML
     * processing in addition to attempting to associate newly-created
     * {@link ucar.unidata.idv.ViewManager}s with ViewManagers found in a
     * bundle.
     * </p>
     * 
     * <p>
     * The latter is done so that McIDAS-V can load bundles that do not use
     * component groups. A &quot;dynamic skin&quot; is built with ViewManagers
     * for each ViewManager in the bundle. The &quot;viewid&quot; attribute of
     * the dynamic skin ViewManager is the name of the
     * {@link ucar.unidata.idv.ViewDescriptor} from the bundled ViewManager.
     * <tt>createViewManager()</tt> is used to actually associate the new
     * ViewManager with its bundled ViewManager.
     * </p>
     * 
     * @param node The XML describing the component to be created.
     * @param id <tt>node</tt>'s ID.
     * 
     * @return The {@link java.awt.Component} described by <tt>node</tt>.
     * 
     * @see ucar.unidata.idv.ui.IdvXmlUi#createComponent(Element, String)
     * @see edu.wisc.ssec.mcidasv.ui.McIDASVXmlUi#createViewManager(Element)
     */
    @Override public Component createComponent(Element node, String id) {
        Component comp = null;
        String tagName = node.getTagName();
        if (tagName.equals(TAG_HTML)) {
            String text = getAttr(node, ATTR_TEXT, NULLSTRING);
            text = decodeHtml(text);
            if (text == null) {
                String url = getAttr(node, ATTR_URL, NULLSTRING);
                if (url != null) {
                    text = IOUtil.readContents(url, (String)null);
                }
                if (text == null) {
                    text = XmlUtil.getChildText(node);
                }
            }
            HyperlinkListener linkListener = new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() != EventType.ACTIVATED)
                        return;

                    String url;
                    if (e.getURL() == null) {
                        url = e.getDescription();
                    } else {
                        url = e.getURL().toString();
                    }
                    actionPerformed(new ActionEvent(this, 0, url));
                }
            };
            Component[] comps =
                GuiUtils.getHtmlComponent(text, linkListener, getAttr(node,
                    ATTR_WIDTH, 200), getAttr(node, ATTR_HEIGHT, 200));
            comp = comps[1];
        } else if (tagName.equals(UIManager.COMP_MAPVIEW)
                   || tagName.equals(UIManager.COMP_VIEW)) {

            // if we're creating a VM for a dynamic skin that was created for
            // a bundle, createViewManager() will return the bundled VM.
            ViewManager vm = createViewManager(node);
            if (vm != null) {
                comp = vm.getContents();
            } else {
                comp = super.createComponent(node, id);
            }
        } else if (tagName.equals(TAG_TREEPANEL)) {
            comp = createTreePanel(node, id);
        } else {
            comp = super.createComponent(node, id);
        }

        return comp;
    }

    /**
     * <p>
     * Attempts to build a {@link ucar.unidata.idv.ViewManager} based upon
     * <tt>node</tt>. If the XML has a &quot;viewid&quot; attribute, the
     * value will be used to search for a ViewManager that has been cached by
     * the McIDAS-V {@link UIManager}. If the UIManager has a matching
     * ViewManager, we'll use the cached ViewManager to initialize a
     * &quot;blank&quot; ViewManager. The cached ViewManager is then removed
     * from the cache and deleted. This method will return <code>null</code> if
     * no cached ViewManager was found.
     * </p>
     * 
     * <p>
     * The ViewManager &quot;cache&quot; will only contain bundled ViewManagers
     * that were not held in a component holder. This means that any 
     * ViewManager returned was created for a dynamic skin, but initialized 
     * with the contents of the corresponding bundled ViewManager.
     * </p>
     * 
     * @param node The XML description of the ViewManager that needs building.
     * 
     * @return Null if there was no cached ViewManager, otherwise a ViewManager
     *         that has been initialized with a bundled ViewManager.
     */
    private ViewManager createViewManager(final Element node) {
        final String viewId = getAttr(node, "viewid", NULLSTRING);
        ViewManager vm = null;
        if (viewId != null) {
            ViewManager old = UIManager.savedViewManagers.remove(viewId);
            if (old != null) {
                vm = getViewManager(node);
                vm.initWith(old);
                old.destroy();
            }
        }
        return vm;
    }

    private TreePanel createTreePanel(final Element node, final String id) {

        TreePanel treePanel = 
            new TreePanel(getAttr(node, ATTR_USESPLITPANE, false), 
                getAttr(node, ATTR_TREEWIDTH, -1));

        List<Element> kids = XmlUtil.getListOfElements(node);

        for (Element kid : kids) {
            Component comp = xmlToUi(kid);
            if (comp == null) {
                continue;
            }

            String label = getAttr(kid, ATTR_TITLE, "");

            ImageIcon icon = getAttr(kid, ATTR_ICON, (ImageIcon)null);
            String cat = getAttr(kid, ATTR_CATEGORY, (String)null);
            if (XmlUtil.getAttribute(kid, ATTR_CATEGORYCOMPONENT, false)) {
                treePanel.addCategoryComponent(cat, (JComponent)comp);
            } else {
                treePanel.addComponent((JComponent)comp, cat, label, icon);
            }
        }
        treePanel.closeAll();
        treePanel.showPersistedSelection();
        return treePanel;
    }

    /**
     * The xml nodes can contain an idref field. If so this returns the
     * node that that id defines
     * 
     * @param node node
     * 
     * @return The node or the referenced node
     */
    private Element getReffedNode(Element node) {
        String idRef = getAttr(node, ATTR_IDREF, NULLSTRING);
        if (idRef == null) {
            return node;
        }
        
        Element reffedNode = (Element)idToElement.get(idRef);
        if (reffedNode == null) {
            throw new IllegalStateException("Could not find idref=" + idRef);
        }

        // TODO(unidata): Make a new copy of the node 
        // reffedNode = reffedNode.copy ();
        NamedNodeMap map = node.getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            Node n = map.item(i);
            if (!n.getNodeName().equals(ATTR_IDREF)) {
                reffedNode.setAttribute(n.getNodeName(), n.getNodeValue());
            }
        }
        return reffedNode;
    }
}
