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

package edu.wisc.ssec.mcidasv.control;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import visad.Data;
import visad.DateTime;
import visad.FieldImpl;
import visad.FlatField;
import visad.VisADException;
import visad.meteorology.ImageSequenceImpl;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.BandInfo;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.control.ColorTableWidget;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.XmlTree;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Range;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import edu.wisc.ssec.mcidasv.PersistenceManager;
import edu.wisc.ssec.mcidasv.chooser.ImageParameters;
import edu.wisc.ssec.mcidasv.data.ComboDataChoice;
import edu.wisc.ssec.mcidasv.data.adde.AddeImageParameterDataSource;

/**
 * {@link ucar.unidata.idv.control.ImagePlanViewControl} with some McIDAS-V
 * specific extensions. Namely parameter sets and support for inverted 
 * parameter defaults.
 */
public class ImagePlanViewControl extends ucar.unidata.idv.control.ImagePlanViewControl {

    private static final Logger logger = LoggerFactory.getLogger(ImagePlanViewControl.class);

    private static final String TAG_FOLDER = "folder";
    private static final String TAG_DEFAULT = "default";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SERVER = "server";
    private static final String ATTR_POS = "POS";
    private static final String ATTR_DAY = "DAY";
    private static final String ATTR_TIME = "TIME";
    private static final String ATTR_UNIT = "UNIT";

    /** Command for connecting */
    protected static final String CMD_NEWFOLDER = "cmd.newfolder";
    protected static final String CMD_NEWPARASET = "cmd.newparaset";

    /** save parameter set */
    private JFrame saveWindow;

    private static String newFolder;

    private XmlTree xmlTree;

    /** Install new folder fld */
    private JTextField folderFld;

    /** Holds the current save set tree */
    private JPanel treePanel;

    /** The user imagedefaults xml root */
    private static Element imageDefaultsRoot;

    /** The user imagedefaults xml document */
    private static Document imageDefaultsDocument;

    /** Holds the ADDE servers and groups*/
    private static XmlResourceCollection imageDefaults;

    private Node lastCat;

    private static Element lastClicked;

    private JButton newFolderBtn;

    private JButton newSetBtn;

    private String newCompName = "";

    /** Shows the status */
    private JLabel statusLabel;

    /** Status bar component */
    private JComponent statusComp;

    private JPanel contents;

    private DataSourceImpl dataSource;

    private FlatField image;

    private McIDASVHistogramWrapper histoWrapper;

    public ImagePlanViewControl() {
        super();
        logger.trace("created new imageplanviewcontrol={}", Integer.toHexString(hashCode()));
        this.imageDefaults = getImageDefaults();
    }

    @Override public boolean init(DataChoice dataChoice) 
        throws VisADException, RemoteException 
    {
//        this.dataChoice = (DataChoice)this.getDataChoices().get(0);
        boolean result = super.init((DataChoice)this.getDataChoices().get(0));
        return result;
    }

    /**
     * Get the xml resource collection that defines the image default xml
     *
     * @return Image defaults resources
     */
    protected XmlResourceCollection getImageDefaults() {
        XmlResourceCollection ret = null;
        try {
            ControlContext controlContext = getControlContext();
            if (controlContext != null) {
                IdvResourceManager irm = controlContext.getResourceManager();
                ret=irm.getXmlResources( IdvResourceManager.RSC_IMAGEDEFAULTS);
                if (ret.hasWritableResource()) {
                    imageDefaultsDocument =
                        ret.getWritableDocument("<imagedefaults></imagedefaults>");
                    imageDefaultsRoot =
                        ret.getWritableRoot("<imagedefaults></imagedefaults>");
                }
            }
        } catch (Exception e) {
            logger.error("problem trying to set up xml document", e);
        }
        return ret;
    }


    /**
     * Called by doMakeWindow in DisplayControlImpl, which then calls its
     * doMakeMainButtonPanel(), which makes more buttons.
     *
     * @return container of contents
     */
    public Container doMakeContents() {
        try {
            JTabbedPane tab = new MyTabbedPane();
            tab.add("Settings",
                GuiUtils.inset(GuiUtils.top(doMakeWidgetComponent()), 5));
            
            // MH: just add a dummy component to this tab for now..
            //            don't init histogram until the tab is clicked.
            tab.add("Histogram", new JLabel("Histogram not yet initialized"));

            return tab;
        } catch (Exception exc) {
            logException("doMakeContents", exc);
        }
        return null;
    }
    
    /**
     * Take out the histogram-related stuff that was in doMakeContents and put it
     * in a standalone method, so we can wait and call it only after the
     * histogram is actually initialized.
     */
    private void setInitialHistogramRange() {
        try {
            Range range = getRange();
            double lo = range.getMin();
            double hi = range.getMax();
            histoWrapper.setHigh(hi);
            histoWrapper.setLow(lo);
        } catch (Exception exc) {
            logException("setInitialHistogramRange", exc);
        }
    }

    protected JComponent getHistogramTabComponent() {
        List choices = new ArrayList();
        if (datachoice == null) {
            datachoice = getDataChoice();
        }
        choices.add(datachoice);
        histoWrapper = new McIDASVHistogramWrapper("histo", choices, (DisplayControlImpl)this);
        dataSource = getDataSource();

        if (dataSource == null) {
            try {
                image = (FlatField)((ComboDataChoice)datachoice).getData();
                histoWrapper.loadData(image);
            } catch (Exception e) {
                
            }
        } else {
            Hashtable props = dataSource.getProperties();
            try {
                DataSelection testSelection = datachoice.getDataSelection();
                DataSelection realSelection = getDataSelection();
                if (testSelection == null) {
                    datachoice.setDataSelection(realSelection);
                }
                ImageSequenceImpl seq = null;
                if (dataSelection == null)
                    dataSelection = dataSource.getDataSelection();
                if (dataSelection == null) {
                    image = (FlatField)dataSource.getData(datachoice, null, props);
                    if (image == null) {
                        image = (FlatField)datachoice.getData(null);
                    }
                } else {
                    Data data = dataSource.getData(datachoice, null, dataSelection, props);
                    if (data instanceof ImageSequenceImpl) {
                        seq = (ImageSequenceImpl) data;
                    } else if (data instanceof FlatField) {
                        image = (FlatField) data;
                    } else if (data instanceof FieldImpl) {
                        image = (FlatField) ((FieldImpl)data).getSample(0, false);
                    }
                    else {
                        throw new Exception("Histogram must be made from a FlatField");
                    }
                }
                if (seq != null) {
                    if (seq.getImageCount() > 0) 
                        image = (FlatField)seq.getImage(0);
                }
                histoWrapper.loadData(image);
            } catch (Exception e) {
                logger.error("attempting to set up histogram", e);
            }
        }

        JComponent histoComp = histoWrapper.doMakeContents();
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                resetColorTable();
            }
        });
        JPanel resetPanel =
            GuiUtils.center(GuiUtils.inset(GuiUtils.wrap(resetButton), 4));
        return GuiUtils.centerBottom(histoComp, resetPanel);
    }

    protected void contrastStretch(double low, double high) {
        ColorTable ct = getColorTable();
        if (ct != null) {
            Range range = new Range(low, high);
            try {
                setRange(ct.getName(), range);
            } catch (Exception e) {
                logger.error("problem stretching contrast", e);
            }
        }
    }

    @Override public boolean setData(DataChoice dataChoice) throws VisADException, RemoteException {
        boolean result = super.setData(dataChoice);
        logger.trace("result: {}, dataChoice: {}", result, dataChoice);
        return result;
    }

    @Override public void setRange(final Range newRange) throws RemoteException, VisADException {
        logger.trace("newRange: {} [avoiding NPE!]", newRange);
        super.setRange(newRange);
    }
        
    public void resetColorTable() {
        try {
            revertToDefaultColorTable();
            revertToDefaultRange();
            histoWrapper.resetPlot();
        } catch (Exception e) {
            logger.error("problem resetting color table", e);
        }
    }

    protected void getSaveMenuItems(List items, boolean forMenuBar) {
        super.getSaveMenuItems(items, forMenuBar);

        // DAVEP: Remove the parameter set save options for now...
//        items.add(GuiUtils.makeMenuItem("Save Image Parameter Set (TEST)", this,
//        "popupPersistImageParameters"));
//
//        items.add(GuiUtils.makeMenuItem("Save Image Parameter Set", this,
//        "popupSaveImageParameters"));
        
        items.add(GuiUtils.makeMenuItem("Save As Local Data Source", this,
        "saveDataToLocalDisk"));
    }

    public void popupPersistImageParameters() {
        PersistenceManager pm = (PersistenceManager)getIdv().getPersistenceManager();
        pm.saveParameterSet("addeimagery", makeParameterValues());
    }

    private Hashtable makeParameterValues() {
        Hashtable parameterValues = new Hashtable();
        //    	Document doc = XmlUtil.makeDocument();
        //    	Element newChild = doc.createElement(TAG_DEFAULT);

        if (datachoice == null) {
            datachoice = getDataChoice();
        }
        dataSource = getDataSource();
        if (!(dataSource.getClass().isInstance(new AddeImageParameterDataSource()))) {
            logger.trace("dataSource not a AddeImageParameterDataSource; it is: {}", dataSource.getClass().toString());
            return parameterValues;
        }
        AddeImageParameterDataSource testDataSource = (AddeImageParameterDataSource)dataSource;
        List imageList = testDataSource.getDescriptors(datachoice, this.dataSelection);
        int numImages = imageList.size();
        List dateTimes = new ArrayList();
        DateTime thisDT = null;
        if (!(imageList == null)) {
            AddeImageDescriptor aid = null;
            for (int imageNo=0; imageNo<numImages; imageNo++) {
                aid = (AddeImageDescriptor)(imageList.get(imageNo));
                thisDT = aid.getImageTime();
                if (!(dateTimes.contains(thisDT))) {
                    if (thisDT != null) {
                        dateTimes.add(thisDT);
                    }
                }
            }

            // Set the date and time for later reference
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

            // Set the unit for later reference
            String unitS = "";
            if (!(datachoice.getId() instanceof BandInfo)) {
                logger.trace("dataChoice ID not a BandInfo; it is: {}", datachoice.getId().getClass().toString());
                return parameterValues;
            }
            BandInfo bi = (BandInfo)datachoice.getId();
            unitS = bi.getPreferredUnit();

            if (aid != null) {
                String displayUrl = testDataSource.getDisplaySource();
                ImageParameters ip = new ImageParameters(displayUrl);    			
                List props = ip.getProperties();
                List vals = ip.getValues();
                String server = ip.getServer();
                parameterValues.put(ATTR_SERVER, server);
                //    			newChild.setAttribute(ATTR_SERVER, server);
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
                        } else if (attr.equals(ATTR_UNIT)) {
                            val = unitS;
                        } else {
                            val = (String)(vals.get(i));
                        }
                        parameterValues.put(attr, val);
                    }
                }
            }
        }
        return parameterValues;
    }

    public void saveDataToLocalDisk() {
        getDataSource().saveDataToLocalDisk();
    }

    public void popupSaveImageParameters() {
        if (saveWindow == null) {
            showSaveDialog();
            return;
        }
        saveWindow.setVisible(true);
        GuiUtils.toFront(saveWindow);
    }

    private void showSaveDialog() {
        if (saveWindow == null) {
            saveWindow = GuiUtils.createFrame("Save Image Parameter Set");
        }
        if (statusComp == null) {
            statusLabel = new JLabel();
            statusComp = GuiUtils.inset(statusLabel, 2);
            statusComp.setBackground(new Color(255, 255, 204));
            statusLabel.setOpaque(true); 
            statusLabel.setBackground(new Color(255, 255, 204));
        }
        JPanel statusPanel = GuiUtils.inset(GuiUtils.top( GuiUtils.vbox(new JLabel(" "),
            GuiUtils.hbox(GuiUtils.rLabel("Status: "), statusComp),
            new JLabel(" "))), 6);
        JPanel sPanel = GuiUtils.topCenter(statusPanel, GuiUtils.filler());

        List newComps = new ArrayList();
        final JTextField newName = new JTextField(20);
        newName.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setStatus("Click New Folder or New ParameterSet button");
                newCompName = newName.getText().trim();
            }
        });
        newComps.add(newName);
        newComps.add(GuiUtils.filler());
        newFolderBtn = new JButton("New Folder");
        newFolderBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                newFolder = newName.getText().trim();
                if (newFolder.length() == 0) {
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
                if (newCompName.length() == 0) {
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
        JPanel topPanel = GuiUtils.topCenter(sPanel, newPanel);

        treePanel = new JPanel();
        treePanel.setLayout(new BorderLayout());
        makeXmlTree();
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String cmd = event.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_CANCEL)) {
                    if (lastClicked != null) {
                        removeNode(lastClicked);
                        lastClicked = null;
                    }
                    saveWindow.setVisible(false);
                    saveWindow = null;
                } else {
                    saveWindow.setVisible(false);
                    saveWindow = null;
                }
            }
        };
        JPanel bottom =
            GuiUtils.inset(GuiUtils.makeApplyCancelButtons(listener), 5);
        contents = 
            GuiUtils.topCenterBottom(topPanel, treePanel, bottom);

        saveWindow.getContentPane().add(contents);
        saveWindow.pack();
        saveWindow.setLocation(200, 200);

        saveWindow.setVisible(true);
        GuiUtils.toFront(saveWindow);
        setStatus("Please select a folder from tree, or create a new folder");
    }

    private void newComponentError(String comp) {
        JLabel label = new JLabel("Please enter " + comp +" name");
        JPanel contents = GuiUtils.top(GuiUtils.inset(label, 24));
        GuiUtils.showOkCancelDialog(null, "Make Component Error", contents, null);
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
        contents.paintImmediately(0,0,contents.getWidth(),
            contents.getHeight());
    }

    private void removeNode(Element node) {
        if (imageDefaults == null) {
            imageDefaults = getImageDefaults();
        }
        Node parent = node.getParentNode();
        parent.removeChild(node);
        makeXmlTree();
        try {
            imageDefaults.writeWritable();
        } catch (Exception e) {
            logger.error("write error!", e);
        }
        imageDefaults.setWritableDocument(imageDefaultsDocument,
            imageDefaultsRoot);
    }

    private Node makeNewFolder() {
        if (imageDefaults == null) {
            imageDefaults = getImageDefaults();
        }
        if (newFolder.length() == 0) {
            return null;
        }
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
            logger.error("write error!", e);
        }
        imageDefaults.setWritableDocument(imageDefaultsDocument,
            imageDefaultsRoot);
        return newEle;
    }

    /**
     * Just creates an empty XmlTree
     */
    private void makeXmlTree() {
        if (imageDefaults == null) {
            imageDefaults = getImageDefaults();
        }
        xmlTree = new XmlTree(imageDefaultsRoot, true, "") {
            public void doClick(XmlTree theTree, XmlTree.XmlTreeNode node,
                Element element) {
                Element clicked = xmlTree.getSelectedElement();
                String lastTag = clicked.getTagName();
                if ("folder".equals(lastTag)) {
                    lastCat = clicked;
                    lastClicked = null;
                    setStatus("Please enter a name for the new parameter set");
                    newSetBtn.setEnabled(true);
                } else {
                    lastCat = clicked.getParentNode();
                    lastClicked = clicked;
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
        if (node == null) {
            return;
        }
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
        JPopupMenu popup) 
    {
        theTree.selectElement(node);
        String tagName = node.getTagName();
        final Element parent = (Element)node.getParentNode();
        boolean didone  = false;
        JMenuItem mi;

        if (tagName.equals("default")) {
            lastClicked = node;
            JMenu moveMenu = new JMenu("Move to");
            List folders = getFolders();
            for (int i = 0; i < folders.size(); i++) {
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
        if (imageDefaults == null) {
            imageDefaults = getImageDefaults();
        }
        if (lastClicked == null) {
            return;
        }
        Node copyNode = lastClicked.cloneNode(true);
        newFolder.appendChild(copyNode);
        parent.removeChild(lastClicked);
        lastCat = newFolder;
        makeXmlTree();
        try {
            imageDefaults.writeWritable();
        } catch (Exception e) {
            logger.error("write error!", e);
        }
        imageDefaults.setWritableDocument(imageDefaultsDocument, imageDefaultsRoot);
    }

    private void doRename(Element node) {
        if (imageDefaults == null) {
            imageDefaults = getImageDefaults();
        }
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
        if (tagName.equals("default")) {
            root = (Element)node.getParentNode();
        }
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
            logger.error("write error!", e);
        }
        imageDefaults.setWritableDocument(imageDefaultsDocument,
            imageDefaultsRoot);
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
        if (contents != null) {
            contents.invalidate();
            contents.validate();
            contents.repaint();
        }
    }

    public DataSourceImpl getDataSource() {
        DataSourceImpl ds = null;
        List dataSources = getDataSources();
        if (!dataSources.isEmpty()) {
            ds = (DataSourceImpl)dataSources.get(0);
        }
        return ds;
    }

    public Element saveParameterSet() {
        if (imageDefaults == null) {
            imageDefaults = getImageDefaults();
        }
        if (newCompName.length() == 0) {
            newComponentError("parameter set");
            return null;
        }
        Element newChild = imageDefaultsDocument.createElement(TAG_DEFAULT);
        newChild.setAttribute(ATTR_NAME, newCompName);

        if (datachoice == null) {
            datachoice = getDataChoice();
        }
        dataSource = getDataSource();
        if (!(dataSource.getClass().isInstance(new AddeImageParameterDataSource()))) {
            return newChild;
        }
        AddeImageParameterDataSource testDataSource = (AddeImageParameterDataSource)dataSource;
        List imageList = testDataSource.getDescriptors(datachoice, this.dataSelection);
        int numImages = imageList.size();
        List dateTimes = new ArrayList();
        DateTime thisDT = null;
        if (!(imageList == null)) {
            AddeImageDescriptor aid = null;
            for (int imageNo = 0; imageNo < numImages; imageNo++) {
                aid = (AddeImageDescriptor)(imageList.get(imageNo));
                thisDT = aid.getImageTime();
                if (!(dateTimes.contains(thisDT))) {
                    if (thisDT != null) {
                        dateTimes.add(thisDT);
                    }
                }
            }
            String dateS = "";
            String timeS = "";
            if (!(dateTimes.isEmpty())) {
                thisDT = (DateTime)dateTimes.get(0);
                dateS = thisDT.dateString();
                timeS = thisDT.timeString();
                if (dateTimes.size() > 1) {
                    for (int img = 1; img < dateTimes.size(); img++) {
                        thisDT = (DateTime)dateTimes.get(img);
                        String str = ',' + thisDT.dateString();
                        String newString = new String(dateS + str);
                        dateS = newString;
                        str = ',' + thisDT.timeString();
                        newString = new String(timeS + str);
                        timeS = newString;
                    }
                }
            }
            if (aid != null) {
                String displayUrl = testDataSource.getDisplaySource();
                ImageParameters ip = new ImageParameters(displayUrl);
                List props = ip.getProperties();
                List vals = ip.getValues();
                String server = ip.getServer();
                newChild.setAttribute(ATTR_SERVER, server);
                int num = props.size();
                if (num > 0) {
                    String attr = "";
                    String val = "";
                    for (int i = 0; i < num; i++) {
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
        if (parent == null) {
            parent = (Element)lastCat;
        }
        if (parent != null) {
            Element exists = XmlUtil.findElement(parent, "default", ATTR_NAME, newCompName);
            if (!(exists == null)) {
                JLabel label = new JLabel("Replace \"" + newCompName + "\"?");
                JPanel contents = GuiUtils.top(GuiUtils.inset(label, newCompName.length()+12));
                if (!GuiUtils.showOkCancelDialog(null, "Parameter Set Exists", contents, null)) {
                    return newChild;
                }
                parent.removeChild(exists);
            }
            parent.appendChild(newChild);
            makeXmlTree();
        }
        try {
            imageDefaults.writeWritable();
        } catch (Exception e) {
            logger.error("write error!", e);
        }
        imageDefaults.setWritableDocument(imageDefaultsDocument, imageDefaultsRoot);
        return newChild;
    }

    /**
     * Holds a JFreeChart histogram of image values.
     */
    private class MyTabbedPane extends JTabbedPane implements ChangeListener {
        /** Have we been painted */
        boolean painted = false;

        boolean popupFlag = false;
        
        boolean haveDoneHistogramInit = false;

        /**
         * Creates a new {@code MyTabbedPane} that gets immediately registered
         * as a {@link javax.swing.event.ChangeListener} for its own events.
         */
        public MyTabbedPane() {
            addChangeListener(this);
        }
        /**
         *
         * Only make the histogram once the user clicks the Histogram tab
         * for the first time.
         *
         * @param e The event
         */
        public void stateChanged(ChangeEvent e) {
            // MH: don't make the histogram until user clicks the tab.
            if (getTitleAt(getSelectedIndex()).equals("Histogram")  
                    && !haveDoneHistogramInit) {
                getIdv().showWaitCursor();
                this.setComponentAt(getSelectedIndex(), 
                        GuiUtils.inset(getHistogramTabComponent(),5));
                setInitialHistogramRange();
                getIdv().clearWaitCursor();
                haveDoneHistogramInit = true;
            }
        }

        /**
         * MH: Not really doing anything useful...but will leave it here for now...
         */
        private void setPopupFlag(boolean flag) {
            this.popupFlag = flag;
        }

        /**
         * MH: Not really doing anything useful...but will leave it here for now...
         *
         * @param g graphics
         */
        public void paint(java.awt.Graphics g) {
            if (!painted) {
                painted = true;
            }
            super.paint(g);
        }
    }
}
