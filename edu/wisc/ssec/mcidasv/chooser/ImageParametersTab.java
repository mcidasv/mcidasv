package edu.wisc.ssec.mcidasv.chooser;


import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidasv.ResourceManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

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
import ucar.unidata.util.NamedThing;
import ucar.unidata.util.PreferenceList;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import visad.DateTime;


/**
 * Test a new Image selector GUI.
 * Default group selector.
 */
public class ImageParametersTab extends NamedThing {

    private static final String TAG_FOLDER = "folder";
    private static final String TAG_DEFAULT = "default";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SERVER = "server";
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

    private Element lastCat;
    private static Element lastClicked;

    private static TestAddeImageChooser chooser;
    private static JTabbedPane tabbedPane;

    private JRadioButton saveBtn;
    private JRadioButton restBtn;

    private JButton newFolderBtn;
    private JButton newSetBtn;

    private String newCompName = "";

    /** Shows the status */
    private JLabel statusLabel;

    /** Status bar component */
    private JComponent statusComp;

    /**
     * Construct an Adde image selection widget
     *
     * @param imageDefaults The xml resources for the image defaults
     * @param descList Holds the preferences for the image descriptors
     * @param serverList Holds the preferences for the adde servers
     */
    public ImageParametersTab(TestAddeImageChooser imageChooser, JTabbedPane tabbedPane) {
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
    protected XmlResourceCollection getImageDefaultsXRC(TestAddeImageChooser imageChooser) {
        return imageChooser.getIdv().getResourceManager().getXmlResources(
            ResourceManager.RSC_IMAGEDEFAULTS);
    }

    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    protected JPanel doMakeContents() {
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
                newName.setText("");
                Element newEle = makeNewFolder();
                makeXmlTree();
                xmlTree.selectElement(newEle);
                lastCat = newEle;
                lastClicked = null;
                newSetBtn.setEnabled(true);
                newFolderBtn.setEnabled(false);
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
                newName.setText("");
                Element newEle = saveParameterSet();
                xmlTree.selectElement(newEle);
                lastClicked = newEle;
                tabbedPane.setSelectedIndex(chooser.getMainIndex());
            }
        });
        newComps.add(newSetBtn);
        newSetBtn.setEnabled(false);
       
        JPanel newPanel = GuiUtils.top(GuiUtils.left(GuiUtils.hbox(newComps)));
        JPanel topPanel = GuiUtils.topCenter(rbPanel, newPanel);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String cmd = event.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_CANCEL)) {
                } else {
                    tabbedPane.setSelectedIndex(chooser.getMainIndex());
                }
            }
        };

        treePanel = new JPanel();
        treePanel.setLayout(new BorderLayout());
        makeXmlTree();

        ItemListener btnListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (saveBtn.isSelected()) {
                    if (lastCat == null) {
                       setStatus("Please select a folder or create a new one");
                       newSetBtn.setEnabled(false);
                       newFolderBtn.setEnabled(true);
                    } else {
                       newSetBtn.setEnabled(true);
                       newFolderBtn.setEnabled(false);
                       setStatus("Please enter a name for the new parameter set");
                    }
                } else if (restBtn.isSelected()) {
                    setStatus("Please select a parameter set");
                    newSetBtn.setEnabled(false);
                    newFolderBtn.setEnabled(false);
                }
            }
        };
        saveBtn.addItemListener(btnListener);
        restBtn.addItemListener(btnListener);

        JPanel bottomPanel = GuiUtils.center(GuiUtils.makeOkCancelButtons(listener));
        myContents = GuiUtils.topCenterBottom(topPanel, treePanel, bottomPanel);
        setStatus("Please select a folder from tree, or create a new folder");
        return myContents;
    }


    private void setStatus(String msg) {
        statusLabel.setText(msg);
        myContents.paintImmediately(0,0,myContents.getWidth(),
                                        myContents.getHeight());
    }

    private void removeLastClicked(Element last) {
        Node parent = last.getParentNode();
        parent.removeChild(last);
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
 
    private Element makeNewFolder() {
        List newChild = new ArrayList();
        Element newEle = imageDefaultsDocument.createElement(TAG_FOLDER);
        lastCat = newEle;
        String[] newAttrs = { ATTR_NAME, newFolder };
        XmlUtil.setAttributes(newEle, newAttrs);
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
                lastClicked = xmlTree.getSelectedElement();
                String lastTag = lastClicked.getTagName();
                if (lastTag.equals("folder")) {
                    lastCat = lastClicked;
                    lastClicked = null;
                    if (saveBtn.isSelected()) {
                       setStatus("Please enter a name for the new parameter set");
                       newSetBtn.setEnabled(true);
                    }
                } else {
                    if (restBtn.isSelected()) {
                        setStatus("Please select a parameter set, or click OK");
                    }
                }
                if (restBtn.isSelected()) {
                    restoreParameterSet(lastClicked);
                    tabbedPane.setSelectedIndex(chooser.getMainIndex());
                }
            }
        };
        List tagList = new ArrayList();
        tagList.add(TAG_FOLDER);
        tagList.add(TAG_DEFAULT);
        xmlTree.addTagsToProcess(tagList);
        xmlTree.defineLabelAttr(TAG_FOLDER, ATTR_NAME);
        KeyListener keyListener = new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_DELETE) {
                    if (lastClicked == null) {
                        if (lastCat != null) {
                            String folderName = lastCat.getTagName();
                            if (GuiUtils.askYesNo("Verify Delete Folder",
                                "Do you want to delete the folder " +
                                "\"" + lastCat.getAttribute("name") + "\"?")) {
                                XmlUtil.removeChildren(lastCat);
                                removeLastClicked(lastCat);
                                lastCat = null;
                            }
                        }
                    } else if (GuiUtils.askYesNo("Verify Delete", "Do you want to delete " +
                        //lastClicked.getTagName() + "\n" + "   " +
                        "\"" + lastClicked.getAttribute(ATTR_NAME) + "\"?")) {
                        removeLastClicked(lastClicked);
                        lastClicked = null;
                    }
                }
            }
        };
        xmlTree.addKeyListener(keyListener);
        addToContents(GuiUtils.inset(GuiUtils.topCenter(new JPanel(),
                xmlTree.getScroller()), 5));
        return;
    }

    private Object VUTEX = new Object();

    private void restoreParameterSet(Element restElement) {
        chooser.setRestElement(restElement);
        String server = restElement.getAttribute(ATTR_SERVER);
        chooser.setServerOnly(server);
        String desc = restElement.getAttribute(ATTR_DESCRIPTOR);
        chooser.setDescriptorOnly(desc);
        if (restElement.hasAttribute(ATTR_POS)) {
            chooser.resetDoAbsoluteTimes(false);
            Integer pos = new Integer(restElement.getAttribute(ATTR_POS));
            if (!(pos.intValue() < 0))
                chooser.setTime(pos.intValue());
        } else {
            synchronized (VUTEX) {
                chooser.resetDoAbsoluteTimes(true);
                while (!chooser.timesOk()) {}
                if ((restElement.hasAttribute(ATTR_DAY)) &&
                    (restElement.hasAttribute(ATTR_TIME))) {
                    String dateStr = restElement.getAttribute(ATTR_DAY);
                    String timeStr = restElement.getAttribute(ATTR_TIME);
                    try {
                        DateTime dt = new DateTime();
                        DateTime[] dtList = { dt };
                        //System.out.println("size of dtList = " + dtList.length);
                        dt.resetFormat();
                        String dtformat = dt.getFormatPattern();
                        //System.out.println("dtformat=" + dtformat);
                        DateTime dtImage = dt.createDateTime(dateStr + " " + timeStr);
                        dtList[0] = dtImage;
                        //System.out.println("dtList=" + dtList);
                        //System.out.println("calling readDescriptors...");
                        //chooser.readDescriptors();
                        //System.out.println("...returned from readDescriptors");
                        //chooser.setAbsoluteTimes(dtList);
                        chooser.setSelectedTimes(dtList);
                        //System.out.println("...returned from setSelectedTimes");
                        //System.out.println("Here");
                    } catch (Exception e) {
                        System.out.println("Exception e=" + e);
                    }
                }
            }
        }
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

    public Element saveParameterSet() {
        Element newChild = imageDefaultsDocument.createElement(TAG_DEFAULT);
        newChild.setAttribute(ATTR_NAME, newCompName);
        List imageList = chooser.getImageList();
        int numImages = imageList.size();
        if (!(imageList == null)) {
            AddeImageDescriptor aid = (AddeImageDescriptor)(imageList.get(0));
            String url = aid.getSource();
            ImageParameters ip = new ImageParameters(url);
            List props = ip.getProperties();
            List vals = ip.getValues();
            String server = ip.getServer();
            newChild.setAttribute(ATTR_SERVER, server);
            int num = props.size();
            if (num > 0) {
                String attr;
                String val;
                for (int i=0; i<num; i++) {
                    attr = (String)(props.get(i));
                    if (attr.equals("POS")) {
                        val = new Integer(numImages - 1).toString();
                    } else {
                        val = (String)(vals.get(i));
                    }
                    newChild.setAttribute(attr, val);
                }
            }
        }
        Element parent = xmlTree.getSelectedElement();
        if (parent == null) parent = lastCat;
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
}
