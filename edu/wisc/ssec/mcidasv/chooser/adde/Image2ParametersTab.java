/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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

package edu.wisc.ssec.mcidasv.chooser.adde;


import edu.wisc.ssec.mcidasv.ResourceManager;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ucar.unidata.ui.XmlTree;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.NamedThing;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import visad.DateTime;


/**
 * Test a new Image selector GUI.
 * Default group selector.
 */
public class Image2ParametersTab extends NamedThing {

    private static final String TAG_FOLDER = "folder";
    private static final String TAG_DEFAULT = "default";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SERVER = "server";
    private static final String ATTR_GROUP = "GROUP";
    private static final String ATTR_DESCRIPTOR = "DESCRIPTOR";
    private static final String ATTR_POS = "POS";
    private static final String ATTR_UNIT = "UNIT";
    private static final String ATTR_BAND = "BAND";
    private static final String ATTR_MAG = "MAG";
    private static final String ATTR_LINELE = "LINELE";
    private static final String ATTR_DAY = "DAY";
    private static final String ATTR_TIME = "TIME";
    private static final String ATTR_USER = "USER";
    private static final String ATTR_PROJ = "PROJ";
    private static final String ATTR_NAV = "NAV";

    private static String newFolder;

    private XmlTree xmlTree;

    /** Command for connecting */

    /** Holds the current save set tree */
    private JPanel treePanel;

    /** The main gui contents */
    private JPanel myContents;

    /** The user imagedefaults xml root */
    private Element imageDefaultsRoot;

    /** The user imagedefaults xml document */
    private static Document imageDefaultsDocument;

    /** Holds the ADDE servers and groups*/
    private XmlResourceCollection imageDefaults;

    private Node lastCat;
    private static Element lastClicked;

    private static Test2AddeImageChooser chooser;
    private static JTabbedPane tabbedPane;

    /**
     * Construct an Adde image selection widget
     *
     * @param imageDefaults The xml resources for the image defaults
     * @param descList Holds the preferences for the image descriptors
     * @param serverList Holds the preferences for the adde servers
     */
    public Image2ParametersTab(Test2AddeImageChooser imageChooser, JTabbedPane tabbedPane) {
        this.chooser = imageChooser;
        this.tabbedPane = tabbedPane;
        this.imageDefaults = getImageDefaultsXRC(chooser);

        if (imageDefaults.hasWritableResource()) {
            imageDefaultsDocument =
                imageDefaults.getWritableDocument("<imagedefaults></imagedefaults>");
            imageDefaultsRoot = imageDefaults.getWritableRoot("<imagedefaults></imagedefaults>");
        }
    }

    /**
     * Get the xml resource collection that defines the image default xml
     *
     * @return Image defaults resources
     */
    protected XmlResourceCollection getImageDefaultsXRC(Test2AddeImageChooser imageChooser) {
        return imageChooser.getIdv().getResourceManager().getXmlResources(
            ResourceManager.RSC_IMAGEDEFAULTS);
    }

    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    protected JPanel doMakeContents() {
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String cmd = event.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_CANCEL)) {
                } else if (cmd.equals(GuiUtils.CMD_UPDATE)) {
                    handleUpdate();
                } else {
                    tabbedPane.setSelectedIndex(chooser.getMainIndex());
                }
            }
        };

        treePanel = new JPanel();
        treePanel.setLayout(new BorderLayout());
        makeXmlTree();
        JPanel bottomPanel = GuiUtils.center(makeUpdateOkCancelButtons(listener));
        myContents = GuiUtils.centerBottom(treePanel, bottomPanel);
        return myContents;
    }

    /**
     * Utility to make update/ok/cancel button panel
     * 
     * @param l The listener to add to the buttons
     * @return Button panel
     */
    public static JPanel makeUpdateOkCancelButtons(ActionListener l) {
        return GuiUtils.makeButtons(l, new String[] { "Update", "OK", "Cancel" },
                           new String[] { GuiUtils.CMD_UPDATE,
                                          GuiUtils.CMD_OK, GuiUtils.CMD_CANCEL });
    }

    /**
     * Handle when the user presses the update button
     *
     * @throws Exception On badness
     */
    public void handleUpdate() {
        makeXmlTree();
        try {
            imageDefaults.writeWritable();
        } catch (Exception e) {
            System.out.println("write error e=" + e);
        }
        imageDefaults.setWritableDocument(imageDefaultsDocument,
            imageDefaultsRoot);
    }


    private void removeNode(Element node) {
        Node parent = node.getParentNode();
        parent.removeChild(node);
        makeXmlTree();
        try {
            imageDefaults.writeWritable();
        } catch (Exception e) {
            System.out.println("write error e=" + e);
        }
        imageDefaults.setWritableDocument(imageDefaultsDocument,
            imageDefaultsRoot);
    }


    /**
     * Just creates an empty XmlTree
     */
    private void makeXmlTree() {
        xmlTree = new XmlTree(imageDefaultsRoot, true, "") {
            public void doClick(XmlTree theTree, XmlTree.XmlTreeNode node,
                                Element element) {
                Element clicked = xmlTree.getSelectedElement();
                String lastTag = clicked.getTagName();
                if (lastTag.equals("folder")) {
                    lastCat = clicked;
                    lastClicked = null;
                } else {
                    lastCat = clicked.getParentNode();
                    lastClicked = clicked;
                    restoreParameterSet(lastClicked);
                    tabbedPane.setSelectedIndex(chooser.getMainIndex());
                }
            }

            public void doRightClick(XmlTree theTree,
                                     XmlTree.XmlTreeNode node,
                                     Element element, MouseEvent event) {
                JPopupMenu popup = new JPopupMenu();
                if (makePopupMenu(theTree, element, popup)) {
                    popup.show((Component) event.getSource(), event.getX(),
                               event.getY());
                }
            }


        };
        List tagList = new ArrayList();
        tagList.add(TAG_FOLDER);
        tagList.add(TAG_DEFAULT);
        xmlTree.addTagsToProcess(tagList);
        xmlTree.defineLabelAttr(TAG_FOLDER, ATTR_NAME);
        addToContents(GuiUtils.inset(GuiUtils.topCenter(new JPanel(),
                xmlTree.getScroller()), 5));
        return;
    }

    private List getFolders() {
        return XmlUtil.findChildren(imageDefaultsRoot, TAG_FOLDER);
    }


    private void doDeleteRequest(Node node) {
        if (node == null) return;
        Element ele = (Element)node;
        String tagName = ele.getTagName();
        if (tagName.equals("folder")) {
            if (!GuiUtils.askYesNo("Verify Delete Folder",
                "Do you want to delete the folder " +
                "\"" + ele.getAttribute("name") + "\"?" +
                "\nNOTE: All parameter sets it contains will be deleted.")) return;
            XmlUtil.removeChildren(ele);
        } else if (tagName.equals("default")) {
            if (!GuiUtils.askYesNo("Verify Delete", "Do you want to delete " +
                "\"" + ele.getAttribute(ATTR_NAME) + "\"?")) return;
        } else { return; }
        removeNode(ele);
    }

    /**
     *  Create and popup a command menu for when the user has clicked on the given xml node.
     *
     *  @param theTree The XmlTree object displaying the current xml document.
     *  @param node The xml node the user clicked on.
     *  @param popup The popup menu to put the menu items in.
     * @return Did we add any items into the menu
     */
    private boolean makePopupMenu(final XmlTree theTree, final Element node,
                                  JPopupMenu popup) {
        theTree.selectElement(node);
        String    tagName = node.getTagName();
        final Element parent = (Element)node.getParentNode();
        boolean   didone  = false;
        JMenuItem mi;

        if (tagName.equals("default")) {
            lastClicked = node;
            JMenu moveMenu = new JMenu("Move to");
            List folders = getFolders();
            for (int i=0; i<folders.size(); i++) {
                final Element newFolder = (Element)folders.get(i);
                if (!newFolder.isSameNode(parent)) {
                    String name = newFolder.getAttribute(ATTR_NAME);
                    mi = new JMenuItem(name);
                    mi.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            moveParameterSet(parent, newFolder);
                        }
                    });
                    moveMenu.add(mi);
                }  
            }
            popup.add(moveMenu);
            popup.addSeparator();
            didone = true;
        }

        mi = new JMenuItem("Rename...");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                doRename(node);
            }
        });
        popup.add(mi); 
        didone = true;

        mi = new JMenuItem("Delete");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                doDeleteRequest(node);
            }
        });
        popup.add(mi);
        didone = true;

        return didone;
    }

    public void moveParameterSet(Element parent, Element newFolder) {
        if (lastClicked == null) return;
        Node copyNode = lastClicked.cloneNode(true);
        newFolder.appendChild(copyNode);
        parent.removeChild(lastClicked);
        lastCat = newFolder;
        makeXmlTree();
        try {
            imageDefaults.writeWritable();
        } catch (Exception e) {
            System.out.println("write error e=" + e);
        }
        imageDefaults.setWritableDocument(imageDefaultsDocument,
            imageDefaultsRoot);
    }

    private void doRename(Element node) {
        if (!node.hasAttribute(ATTR_NAME)) return;
        JLabel label = new JLabel("New name: ");
        JTextField nameFld = new JTextField("", 20);
        JComponent contents = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("New name: "), nameFld, }, 2,
            GuiUtils.WT_N, GuiUtils.WT_N);
        contents = GuiUtils.center(contents);
        contents = GuiUtils.inset(contents, 10);
        if (!GuiUtils.showOkCancelDialog(null, "Rename \"" + 
               node.getAttribute("name") + "\"", contents, null)) return;
        String newName = nameFld.getText().trim();
        String tagName = node.getTagName();
        Element root = imageDefaultsRoot;
        if (tagName.equals("default")) 
            root = (Element)node.getParentNode();
        Element exists = XmlUtil.findElement(root, tagName, ATTR_NAME, newName);
        if (!(exists == null)) {
           if (!GuiUtils.askYesNo("Name Already Exists",
               "Do you want to replace " + node.getAttribute("name") + " with" +
               "\"" + newName + "\"?")) return;
        }
        node.removeAttribute(ATTR_NAME);
        node.setAttribute(ATTR_NAME, newName);
        makeXmlTree();
        try {
            imageDefaults.writeWritable();
        } catch (Exception e) {
            System.out.println("write error e=" + e);
        }
        imageDefaults.setWritableDocument(imageDefaultsDocument,
            imageDefaultsRoot);
    }


    private boolean restoreParameterSet(Element restElement) {
        if (restElement == null) return false;
        String tagName = restElement.getTagName();
        if (tagName.equals("folder")) return false;
            chooser.setRestElement(restElement);
            if (restElement.hasAttribute(ATTR_USER)) {
                String user = restElement.getAttribute(ATTR_USER);
                if (restElement.hasAttribute(ATTR_PROJ)) {
                    String proj = restElement.getAttribute(ATTR_PROJ);
                    chooser.setUserAndProj(user, proj);
                }
            }
            String server = restElement.getAttribute(ATTR_SERVER);
            chooser.setServerOnly(server);
            String group = restElement.getAttribute(ATTR_GROUP);
            chooser.setGroupOnly(group);
            String desc = restElement.getAttribute(ATTR_DESCRIPTOR);
            chooser.setDescriptor(desc);

            List dtList = new ArrayList(); 
            if (restElement.hasAttribute(ATTR_POS)) {
                chooser.resetDoAbsoluteTimes(false);
                Integer pos = new Integer(restElement.getAttribute(ATTR_POS));
                if (!(pos.intValue() < 0))
                    chooser.setTime(pos.intValue());
            } else {
                if ((restElement.hasAttribute(ATTR_DAY)) &&
                    (restElement.hasAttribute(ATTR_TIME))) {
                    chooser.resetDoAbsoluteTimes(true);
                    chooser.clearTimesList();
                    String dateStr = restElement.getAttribute(ATTR_DAY);
                    String timeStr = restElement.getAttribute(ATTR_TIME);
                    List dateS = breakdown(dateStr, ",");
                    List timeS = breakdown(timeStr, ",");
                    int numImages = timeS.size();
                    try {
                        DateTime dt = new DateTime();
                        dt.resetFormat();
                        String dtformat = dt.getFormatPattern();
                        for (int ix=0; ix<numImages; ix++) {
                            DateTime dtImage = dt.createDateTime((String)dateS.get(ix) + " " 
                                                                 + (String)timeS.get(ix));
                            dtList.add(dtImage);
                        }
                    } catch (Exception e) {
                        System.out.println("Exception e=" + e);
                        return false;
                    }
                }
            }

            if (restElement.hasAttribute(ATTR_MAG)) {
                String mag = restElement.getAttribute(ATTR_MAG);
                chooser.restoreMag(mag);
            }

            if (restElement.hasAttribute(ATTR_BAND)) {
                String band = restElement.getAttribute(ATTR_BAND);
                chooser.restoreBand(band);
            }
            if (restElement.hasAttribute(ATTR_UNIT)) {
                String unit = restElement.getAttribute(ATTR_UNIT);
                chooser.restoreUnit(unit);
            }
            int indx = 0;
            if (restElement.hasAttribute(ATTR_LINELE)) {
                indx = 1;
            }
            if (restElement.hasAttribute(ATTR_NAV)) {
                String nav = restElement.getAttribute(ATTR_NAV);
                chooser.restoreNav(nav);
            }
            if (dtList != null) {
                chooser.setDtList(dtList);
            }
            return true;
    }

    /**
     *  Remove the currently display gui and insert the given one.
     *
     *  @param comp The new gui.
     */
    private void addToContents(JComponent comp) {
        treePanel.removeAll();
        comp.setPreferredSize(new Dimension(200, 300));
        treePanel.add(comp, BorderLayout.CENTER);
        if (myContents != null) {
            myContents.invalidate();
            myContents.validate();
            myContents.repaint();
        }
    }


    /**
     * Returns a list of values from a delimited string.
     *
     * @param str  The delimited string.
     * @param delim   The delimiter.
     *
     * @return List of strings.
     */
    private List breakdown(String str, String delim) {
        List retList = new ArrayList();
        StringTokenizer tok = new StringTokenizer(str, delim);
        while (tok.hasMoreTokens()) {
            String next = (String)tok.nextElement();
            retList.add(next);
        }
        return retList;
    }
}
