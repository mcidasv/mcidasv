package edu.wisc.ssec.mcidasv.chooser;


import edu.wisc.ssec.mcidasv.ResourceManager;

import java.awt.BorderLayout;
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


/**
 * Test a new Image selector GUI.
 * Default group selector.
 */
public class ImageParametersTab extends NamedThing {

    private static final String TAG_FOLDER = "folder";
    private static final String TAG_SAVESET = "set";
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

    /** Property for image default value unit */
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

    /** add new folder dialog */
    private JFrame newFolderWindow;

    /** Install new folder fld */
    private JTextField folderFld;

    /** Holds the current save set tree */
    private JPanel treePanel;

    /** The main gui contents */
    private JPanel myContents;

    /** The user imagedefaults xml root */
    private Element imageParametersRoot;

    /** The user imagedefaults xml document */
    private static Document imageParametersDocument;

    /** Holds the ADDE servers and groups*/
    private XmlResourceCollection imageParameters;

    private Element lastCat;
    private static Element lastClicked;

    private static TestAddeImageChooser chooser;
    private static JTabbedPane tabbedPane;

    private JRadioButton saveBtn;
    private JRadioButton restBtn;

    /**
     * Construct an Adde image selection widget
     *
     * @param imageParameters The xml resources for the image defaults
     * @param descList Holds the preferences for the image descriptors
     * @param serverList Holds the preferences for the adde servers
     */
    public ImageParametersTab(TestAddeImageChooser imageChooser, JTabbedPane tabbedPane) {
        this.chooser = imageChooser;
        this.tabbedPane = tabbedPane;
        this.imageParameters = getImageParametersXRC(chooser);

        if (imageParameters.hasWritableResource()) {
            imageParametersDocument =
                imageParameters.getWritableDocument("<imageparameters></imageparameters>");
            imageParametersRoot = imageParameters.getWritableRoot("<imageparameters></imageparameters>");
        }
    }

    /**
     * Get the xml resource collection that defines the image default xml
     *
     * @return Image defaults resources
     */
    protected XmlResourceCollection getImageParametersXRC(TestAddeImageChooser imageChooser) {
        return imageChooser.getIdv().getResourceManager().getXmlResources(
            ResourceManager.RSC_IMAGEPARAMETERS);
    }

    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    protected JPanel doMakeContents() {
        //GuiUtils.setHFill();
        saveBtn = new JRadioButton("Save current parameters", true);
        restBtn = new JRadioButton("Restore", false);
        GuiUtils.buttonGroup(saveBtn, restBtn);
        JPanel rbPanel = GuiUtils.top(GuiUtils.center(GuiUtils.hbox(saveBtn, restBtn)));
        List allComps = new ArrayList();
        allComps.add(rbPanel);
        List saveComps = new ArrayList();
        final JLabel nameLabel = GuiUtils.rLabel("Save As: ");
        saveComps.add(nameLabel);
        final JTextField nameBox = new JTextField(20);
        saveComps.add(nameBox);
        nameBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String newSet = nameBox.getText().trim();
                saveParameterSet(newSet);
                nameBox.setText("");
            }
        });
        final JButton newFolderBtn = new JButton("New Folder...");
        newFolderBtn.setActionCommand(CMD_NEWFOLDER);
        newFolderBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                doNewFolder();
            }
        });
        saveComps.add(GuiUtils.filler());
        saveComps.add(newFolderBtn);
        JPanel savePanel = GuiUtils.center(GuiUtils.doLayout(saveComps,4,GuiUtils.WT_N, GuiUtils.WT_N));
        JPanel topPanel = GuiUtils.centerBottom(rbPanel, savePanel);

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

        if (!saveBtn.isSelected()) {
            nameLabel.setEnabled(false);
            nameBox.setEnabled(false);
            newFolderBtn.setEnabled(false);
        }
        ItemListener btnListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                nameLabel.setEnabled(saveBtn.isSelected());
                nameBox.setEnabled(saveBtn.isSelected());
                newFolderBtn.setEnabled(saveBtn.isSelected());
            }
        };
        saveBtn.addItemListener(btnListener);
        restBtn.addItemListener(btnListener);

        JPanel bottomPanel = GuiUtils.center(GuiUtils.makeOkCancelButtons(listener));
        myContents = GuiUtils.topCenterBottom(topPanel, treePanel, bottomPanel);
        return myContents;
    }

    private void removeLastClicked() {
        Node parent = lastClicked.getParentNode();
        parent.removeChild(lastClicked);
        makeXmlTree();
        try {
            imageParameters.writeWritable();
        } catch (Exception e) {
            System.out.println("write error e=" + e);
        }
        imageParameters.setWritableDocument(imageParametersDocument,
            imageParametersRoot);
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
    protected final void doNewFolder() {
        if (newFolderWindow == null) {
            showNewFolderDialog();
            return;
        }
        newFolderWindow.setVisible(true);
        GuiUtils.toFront(newFolderWindow);
    }

    /**
     * showAacctDialog
     */
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

 
    private void makeNewFolder() {
        List newChild = new ArrayList();
        Element newEle = imageParametersDocument.createElement(TAG_FOLDER);
        lastCat = newEle;
        String[] newAttrs = { ATTR_NAME, newFolder };
        XmlUtil.setAttributes(newEle, newAttrs);
        newChild.add(newEle);
        XmlUtil.addChildren(imageParametersRoot, newChild);
        try {
            imageParameters.writeWritable();
        } catch (Exception e) {
            System.out.println("write error e=" + e);
        }
        imageParameters.setWritableDocument(imageParametersDocument,
            imageParametersRoot);
    }

    /**
     * Close the new folder dialog
     */
    public void closeNewFolder() {
        if (newFolderWindow != null) {
            newFolderWindow.setVisible(false);
        }
    }

    /**
     * Just creates an empty XmlTree
     */
    private void makeXmlTree() {
        xmlTree = new XmlTree(imageParametersRoot, true, "") {
            public void doClick(XmlTree theTree, XmlTree.XmlTreeNode node,
                                Element element) {
                lastClicked = xmlTree.getSelectedElement();
                if (restBtn.isSelected()) {
                    restoreParameterSet(lastClicked);
                }
            }
        };
        List tagList = new ArrayList();
        tagList.add(TAG_FOLDER);
        tagList.add(TAG_SAVESET);
        xmlTree.addTagsToProcess(tagList);
        xmlTree.defineLabelAttr(TAG_FOLDER, ATTR_NAME);
        KeyListener keyListener = new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_DELETE) {
                    if (GuiUtils.askYesNo("Verify Delete", "Do you want to delete " +
                        lastClicked.getTagName() + "\n" + "   " +
                        lastClicked.getAttribute(ATTR_NAME))) {
                        removeLastClicked();
                    }
                }
            }
        };
        xmlTree.addKeyListener(keyListener);
        addToContents(GuiUtils.inset(GuiUtils.topCenter(new JPanel(),
                xmlTree.getScroller()), 5));
        return;
    }

    private void restoreParameterSet(Element restElement) {
        String server = restElement.getAttribute(ATTR_SERVER);
        chooser.setServer(server);
        String desc = restElement.getAttribute(ATTR_DESCRIPTOR);
        chooser.setDescriptor(desc);
        Integer pos = new Integer(restElement.getAttribute(ATTR_POS));
        if (!(pos.intValue() < 0))
            chooser.setTime(pos.intValue());
        chooser.changePlace(restElement.getAttribute(ATTR_PLACE));
        String str = "";
        if (restElement.hasAttribute(ATTR_LATLON)) {
            str = restElement.getAttribute(ATTR_LATLON);
        } else if (restElement.hasAttribute(ATTR_LINELE)) {
            str = restElement.getAttribute(ATTR_LINELE);
        }
        StringTokenizer tok = new StringTokenizer(str," ");
        String line = tok.nextToken();
        String ele = tok.nextToken();
        chooser.setLineElement(line, ele);
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

    public void saveParameterSet(String newSet) {
        List imageList = chooser.getImageList();
        AddeImageDescriptor aid = (AddeImageDescriptor)(imageList.get(0));
        String url = aid.getSource();
        //System.out.println("\n" + url);
        ImageParameters ip = new ImageParameters(url);
        List props = ip.getProperties();
        List vals = ip.getValues();
        String server = ip.getServer();
        int num = props.size();
        Element newChild = imageParametersDocument.createElement(TAG_SAVESET);
        newChild.setAttribute(ATTR_NAME, newSet);
        newChild.setAttribute(ATTR_SERVER, server);
        if (num > 0) {
            String attr;
            String val;
            for (int i=0; i<num; i++) {
                attr = (String)(props.get(i));
                val = (String)(vals.get(i));
                newChild.setAttribute(attr, val);
            }
        }
        Element parent = xmlTree.getSelectedElement();
        if (parent == null) parent = lastCat;
        if (parent != null)
            parent.appendChild(newChild);
        makeXmlTree();
        try {
            imageParameters.writeWritable();
        } catch (Exception e) {
            System.out.println("write error e=" + e);
        }
        imageParameters.setWritableDocument(imageParametersDocument,
            imageParametersRoot);
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
