/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.chooser;


import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidasv.ResourceManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.data.imagery.AddeImageDescriptor;

import ucar.unidata.ui.XmlTree;
import ucar.unidata.ui.imagery.ImageSelector;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.NamedThing;
import ucar.unidata.util.PreferenceList;

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
    private static final String ATTR_URL = "url";
    private static final String ATTR_POS = "POS";
    private static final String ATTR_UNIT = "UNIT";
    private static final String ATTR_BAND = "BAND";
    private static final String ATTR_PLACE = "PLACE";
    private static final String ATTR_LOC = "LOC";
    private static final String ATTR_SIZE = "SIZE";
    private static final String ATTR_MAG = "MAG";
    private static final String ATTR_NAV = "NAV";
    private static final String ATTR_LATLON = "LATLON";
    private static final String ATTR_LINELE = "LINELE";
    private static final String ATTR_DAY = "DAY";
    private static final String ATTR_TIME = "TIME";
    private static final String ATTR_PATTERN = "pattern";
//    private static final String ATTR_LOCK = "LOCK";

    /** Property for image default value unit */
    protected static final String PROP_UNIT = "UNIT";

    /** Property for image default value band */
    protected static final String PROP_BAND = "BAND";

    /** Property for image default value place */
    protected static final String PROP_PLACE = "PLACE";

    /** Property for image default value loc */
    protected static final String PROP_LOC = "LOC";

    /** Property for image default value size */
    protected static final String PROP_SIZE = "SIZE";

    /** Property for image default value mag */
    protected static final String PROP_MAG = "MAG";

    /** Property for image default value nav */
    protected static final String PROP_NAV = "NAV";

    /** This is the list of properties that are used in the advanced gui */
    private static final String[] ADVANCED_PROPS = {
        PROP_UNIT, PROP_BAND, PROP_PLACE, PROP_LOC, PROP_SIZE, PROP_MAG,
        PROP_NAV
    };

    private static String newFolder;

    private XmlTree xmlTree;

    /** Command for connecting */
    protected static final String CMD_NEWFOLDER = "cmd.newfolder";
    protected static final String CMD_NEWPARASET = "cmd.newparaset";

    /** add new folder dialog */
//    private JFrame newFolderWindow;

    /** Install new folder fld */
    private JTextField folderFld;

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
/*
    private JRadioButton saveBtn;
    private JRadioButton restBtn;

    private JButton newFolderBtn;
    private JButton newSetBtn;

    private String newCompName = "";

    *** Shows the status **
    private JLabel statusLabel;

    *** Status bar component **
    private JComponent statusComp;
*/
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
/*
        //GuiUtils.setHFill();
        if (statusComp == null) {
            statusLabel = new JLabel();
            statusComp = GuiUtils.inset(statusLabel, 2);
            statusComp.setBackground(new Color(255, 255, 204));
            statusLabel.setOpaque(true);
            statusLabel.setBackground(new Color(255, 255, 204));
        }
        JPanel statusPanel = GuiUtils.inset(GuiUtils.top( GuiUtils.vbox(new JLabel(" "),
                GuiUtils.hbox(GuiUtils.rLabel("Status: "),statusComp),
                new JLabel(" "))), 6);
        saveBtn = new JRadioButton("Save current parameters", true);
        restBtn = new JRadioButton("Restore", false);
        GuiUtils.buttonGroup(saveBtn, restBtn);
        JPanel rbPanel = GuiUtils.topCenterBottom(statusPanel,
               GuiUtils.center(GuiUtils.hbox(saveBtn, restBtn)),
               GuiUtils.filler());

        List newComps = new ArrayList(); 
        final JTextField newName = new JTextField(20);
        newName.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setStatus("Click New Folder or New ParameterSet button");
                newCompName = newName.getText().trim();
                //newName.setText("");
            }
        });
        newComps.add(newName);
        newComps.add(GuiUtils.filler());
        newFolderBtn = new JButton("New Folder");
        newFolderBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                newFolder = newName.getText().trim();
                if (newFolder.equals("")) {
                    newComponentError("folder");
                    return;
                }
                Element exists = XmlUtil.findElement(imageDefaultsRoot, "folder", ATTR_NAME, newFolder);
                if (!(exists == null)) {
                    if (!GuiUtils.askYesNo("Verify Replace Folder",
                        "Do you want to replace the folder " +
                        "\"" + newFolder + "\"?" +
                        "\nNOTE: All parameter sets it contains will be deleted.")) return;
                    imageDefaultsRoot.removeChild(exists);
                }
                newName.setText("");
                Node newEle = makeNewFolder();
                makeXmlTree();
                xmlTree.selectElement((Element)newEle);
                lastCat = newEle;
                lastClicked = null;
                newSetBtn.setEnabled(true);
                //newFolderBtn.setEnabled(false);
                setStatus("Please enter a name for the new parameter set");
            }
        });
        newComps.add(newFolderBtn);
        newComps.add(GuiUtils.filler());
        newName.setEnabled(true);
        newFolderBtn.setEnabled(true);
        newSetBtn = new JButton("New Parameter Set");
        newSetBtn.setActionCommand(CMD_NEWPARASET);
        newSetBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                newCompName = newName.getText().trim();
                if (newCompName.equals("")) {
                    newComponentError("parameter set");
                    return;
                }
                newName.setText("");
                Element newEle = saveParameterSet();
                if (newEle == null) return;
                xmlTree.selectElement(newEle);
                lastClicked = newEle;
            }
        });
        newComps.add(newSetBtn);
        newSetBtn.setEnabled(false);

        JPanel newPanel = GuiUtils.top(GuiUtils.left(GuiUtils.hbox(newComps)));
        JPanel topPanel = GuiUtils.topCenter(rbPanel, newPanel);
*/
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
/*
        ItemListener btnListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (saveBtn.isSelected()) {
                       setStatus("Please select a folder or create a new one");
                       newFolderBtn.setEnabled(true);
                       newSetBtn.setEnabled(true);
                } else if (restBtn.isSelected()) {
                    setStatus("Please select a parameter set");
                    newSetBtn.setEnabled(false);
                    newFolderBtn.setEnabled(false);
                    restoreParameterSet(lastClicked);
                }
            }
        };
        saveBtn.addItemListener(btnListener);
        restBtn.addItemListener(btnListener);
*/
        JPanel bottomPanel = GuiUtils.center(makeUpdateOkCancelButtons(listener));
        myContents = GuiUtils.centerBottom(treePanel, bottomPanel);
//        myContents = GuiUtils.topCenterBottom(topPanel, treePanel, bottomPanel);
//        setStatus("Please select a folder from tree, or create a new folder");
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


/*
    private void newComponentError(String comp) {
        JLabel label = new JLabel("Please enter " + comp +" name");
        JPanel contents = GuiUtils.top(GuiUtils.inset(label, 24));
        GuiUtils.showOkCancelDialog(null, "Make Component Error", contents, null);
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
        myContents.paintImmediately(0,0,myContents.getWidth(),
                                        myContents.getHeight());
    }
*/
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
     * Handle the event
     * 
     * @param ae The event
     */
/*
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(CMD_NEWFOLDER)) {
            doNewFolder();
        } else {
            this.chooser.actionPerformed(ae);
        }
    }
*/

    /**
     * Go directly to the Server Manager
     */
/*
    protected final void doNewFolder() {
        if (newFolderWindow == null) {
            showNewFolderDialog();
            return;
        }
        newFolderWindow.setVisible(true);
        GuiUtils.toFront(newFolderWindow);
    }
*/

    /**
     * showAacctDialog
     */
/*
    private void showNewFolderDialog() {
        if (newFolderWindow == null) {
            List comps = new ArrayList();

            newFolderWindow = GuiUtils.createFrame("Create New Save Set Folder");
            folderFld = new JTextField("", 20);

            List textComps = new ArrayList();
            textComps.add(new JLabel(" "));
            textComps.add(GuiUtils.hbox(new JLabel("Folder Name: "), folderFld));
            JComponent textComp = GuiUtils.center(GuiUtils.inset(
                                     GuiUtils.vbox(textComps),20));

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    String cmd = event.getActionCommand();
                    if (cmd.equals(GuiUtils.CMD_CANCEL)) {
                        newFolderWindow.setVisible(false);
                        newFolderWindow = null;
                    } else {
                        newFolder = folderFld.getText().trim();
                        makeNewFolder();
                        makeXmlTree();
                        closeNewFolder();
                    }
                }
            };

            JPanel bottom =
                GuiUtils.inset(GuiUtils.makeOkCancelButtons(listener),5);
            JComponent contents = GuiUtils.centerBottom(textComp, bottom);
            newFolderWindow.getContentPane().add(contents);
            newFolderWindow.pack();
            newFolderWindow.setLocation(200, 200);

        }
        newFolderWindow.setVisible(true);
        GuiUtils.toFront(newFolderWindow);
    }
*/
 
    private Node makeNewFolder() {
        if (newFolder.equals("")) return null;
        List newChild = new ArrayList();
        Node newEle = imageDefaultsDocument.createElement(TAG_FOLDER);
        lastCat = newEle;
        String[] newAttrs = { ATTR_NAME, newFolder };
        XmlUtil.setAttributes((Element)newEle, newAttrs);
        newChild.add(newEle);
        XmlUtil.addChildren(imageDefaultsRoot, newChild);
        try {
            imageDefaults.writeWritable();
        } catch (Exception e) {
            System.out.println("write error e=" + e);
        }
        imageDefaults.setWritableDocument(imageDefaultsDocument,
            imageDefaultsRoot);
        return newEle;
    }

    /**
     * Close the new folder dialog
     */
/*
    public void closeNewFolder() {
        if (newFolderWindow != null) {
            newFolderWindow.setVisible(false);
        }
    }
*/

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
/*
                    if (saveBtn.isSelected()) {
                       setStatus("Please enter a name for the new parameter set");
                       newSetBtn.setEnabled(true);
                    }
*/
                } else {
                    lastCat = clicked.getParentNode();
                    lastClicked = clicked;
                    restoreParameterSet(lastClicked);
                    tabbedPane.setSelectedIndex(chooser.getMainIndex());
/*
                    if (restBtn.isSelected()) {
                        setStatus("Please wait...");
                    }
*/
                }
/*
                if (restBtn.isSelected()) {
                    if (restoreParameterSet(lastClicked))
                        tabbedPane.setSelectedIndex(chooser.getMainIndex());
                }
*/
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
/*
        KeyListener keyListener = new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_DELETE) {
                    Node node = lastClicked;
                    if (node == null) {
                        if (lastCat != null) {
                            node = lastCat;
                            lastCat = null;
                        }
                    }
                    if (node != null) {
                        doDeleteRequest(node);
                        lastClicked = null;
                    }
                }
            }
        };
        xmlTree.addKeyListener(keyListener);
*/
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
                    chooser.readTimes();
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
/*
    public Element saveParameterSet() {
        if (newCompName.equals("")) {
            newComponentError("parameter set");
            return null;
        }
        Element newChild = imageDefaultsDocument.createElement(TAG_DEFAULT);
        newChild.setAttribute(ATTR_NAME, newCompName);
        List imageList = chooser.getImageList();
        if (imageList == null) {
            JLabel label = new JLabel("No image parameters have been entered");
            JPanel contents = GuiUtils.top(GuiUtils.inset(label, 24));
            GuiUtils.showOkCancelDialog(null, "Make Component Error", contents, null);
            return null;
        }
        boolean locked = chooser.lockBtn.isSelected();
        int numImages = imageList.size();
        List dateTimes = new ArrayList();
        DateTime thisDT = null;
        if (!(imageList == null)) {
            AddeImageDescriptor aid = null;
            for (int imageNo=0; imageNo<numImages; imageNo++) {
                aid = (AddeImageDescriptor)(imageList.get(imageNo));
                thisDT = aid.getImageTime();
                if (!(dateTimes.contains(thisDT))) {
                    if (thisDT != null) dateTimes.add(thisDT);
                }
            }
            String dateS = "";
            String timeS = ""; 
            if (!(dateTimes.isEmpty())) {
                thisDT = (DateTime)dateTimes.get(0);
                dateS = thisDT.dateString();
                timeS = thisDT.timeString();
                if (dateTimes.size() > 1) {
                    for (int img=1; img<dateTimes.size(); img++) {
                        thisDT = (DateTime)dateTimes.get(img);
                        String str = "," + thisDT.dateString();
                        String newString = new String(dateS + str);
                        dateS = newString;
                        str = "," + thisDT.timeString();
                        newString = new String(timeS + str);
                        timeS = newString;
                    }
                 }
             }
             if (aid != null) {
                String url = aid.getSource();
                ImageParameters ip = new ImageParameters(url);
                List props = ip.getProperties();
                List vals = ip.getValues();
                String server = ip.getServer();
                newChild.setAttribute(ATTR_SERVER, server);
                if (locked) {
                    newChild.setAttribute(ATTR_LOCK, "true");
                } else {
                    newChild.setAttribute(ATTR_LOCK, "false");
                }
                int num = props.size();
                if (num > 0) {
                    String attr = "";
                    String val = "";
                    for (int i=0; i<num; i++) {
                        attr = (String)(props.get(i));
                        if (attr.equals(ATTR_POS)) {
                            val = new Integer(numImages - 1).toString();
                        } else if (attr.equals(ATTR_DAY)) {
                            val = dateS;
                        } else if (attr.equals(ATTR_TIME)) {
                            val = timeS;
                        } else {
                            val = (String)(vals.get(i));
                        }
                        newChild.setAttribute(attr, val);
                    }
                }
            }
        }
        Element parent = xmlTree.getSelectedElement();
        if (parent == null) parent = (Element)lastCat;
        if (parent != null) {
            Element exists = XmlUtil.findElement(parent, "default", ATTR_NAME, newCompName);
            if (!(exists == null)) {
                JLabel label = new JLabel("Replace \"" + newCompName + "\"?");
                JPanel contents = GuiUtils.top(GuiUtils.inset(label, newCompName.length()+12));
                if (!GuiUtils.showOkCancelDialog(null, "Parameter Set Exists", contents, null))
                    return newChild;
                parent.removeChild(exists);
            }
            parent.appendChild(newChild);
            makeXmlTree();
        }
        try {
            imageDefaults.writeWritable();
        } catch (Exception e) {
            System.out.println("write error e=" + e);
        }
        imageDefaults.setWritableDocument(imageDefaultsDocument,
            imageDefaultsRoot);
        return newChild;
    }
*/

    /**
     * Returns a list of the images to load or null if none have been
     * selected.
     *
     * @return  list  get the list of image descriptors
     */

    public List getImageList() {
        List images = new ArrayList();
        return images;
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
