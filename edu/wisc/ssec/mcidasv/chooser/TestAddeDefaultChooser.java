package edu.wisc.ssec.mcidasv.chooser;


//import edu.wisc.ssec.mcidas.AreaDirectory;

import edu.wisc.ssec.mcidasv.ResourceManager;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import java.util.ArrayList;
//import java.util.Hashtable;
import java.util.List;
//import java.util.Vector;

import javax.swing.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeChooser;

import ucar.unidata.ui.XmlTree;
import ucar.unidata.ui.imagery.ImageSelector;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;


/**
 * Test a new Image selector GUI.
 * Default group selector.
 */
public class TestAddeDefaultChooser extends AddeChooser implements ImageSelector {

    private static final String TAG_FOLDER = "folder";
    private static final String TAG_SAVESET = "saveset";
    private static final String ATTR_NAME = "name";

    private static String newFolder;

    private XmlTree xmlTree;
//    private String lastCat;

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
    private Document imageParametersDocument;

    /** Holds the ADDE servers and groups*/
    private XmlResourceCollection imageParameters;


    /**
     * Construct an Adde image selection widget
     *
     * @param imageParameters The xml resources for the image defaults
     * @param descList Holds the preferences for the image descriptors
     * @param serverList Holds the preferences for the adde servers
     */
    public TestAddeDefaultChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);

        this.imageParameters = getImageParameters();

        if (imageParameters.hasWritableResource()) {
            imageParametersDocument =
                imageParameters.getWritableDocument("<tabs></tabs>");
            imageParametersRoot = imageParameters.getWritableRoot("<tabs></tabs>");
        }
    }

    /**
     * Get the xml resource collection that defines the image default xml
     *
     * @return Image defaults resources
     */
    protected XmlResourceCollection getImageParameters() {
        return getIdv().getResourceManager().getXmlResources(
            ResourceManager.RSC_IMAGEPARAMETERS);
    }

    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    protected JComponent doMakeContents() {
        //GuiUtils.setHFill();
        final JRadioButton saveBtn = new JRadioButton("Save current parameters", false);
        final JRadioButton restBtn = new JRadioButton("Restore", true);
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
            }
        });
        final JButton newFolderBtn = new JButton("New Folder...");
        newFolderBtn.setActionCommand(CMD_NEWFOLDER);
        newFolderBtn.addActionListener(this);
        saveComps.add(GuiUtils.filler());
        saveComps.add(newFolderBtn);
        JPanel savePanel = GuiUtils.center(GuiUtils.doLayout(saveComps,4,GuiUtils.WT_N, GuiUtils.WT_N));
        JPanel topPanel = GuiUtils.centerBottom(rbPanel, savePanel);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String cmd = event.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_CANCEL)) {
                } else {
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

    /**
     * Handle the event
     * 
     * @param ae The event
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(CMD_NEWFOLDER)) {
            doNewFolder();
        } else {
            super.actionPerformed(ae);
        }
    }

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
        String[] newAttrs = { ATTR_NAME, newFolder };
        XmlUtil.setAttributes(newEle, newAttrs);
        newChild.add(newEle);
        XmlUtil.addChildren(imageParametersRoot, newChild);
        try {
            imageParameters.writeWritable();
        } catch (Exception e) {
            System.out.println("write error e=" + e);
        }
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
                Element tempEle = xmlTree.getSelectedElement();
                //lastCat = tempEle.getAttribute(ATTR_NAME);
            }
        };
        List tagList = new ArrayList();
        tagList.add(TAG_FOLDER);
        tagList.add(TAG_SAVESET);
        xmlTree.addTagsToProcess(tagList);
        xmlTree.defineLabelAttr(TAG_FOLDER, ATTR_NAME);
        addToContents(GuiUtils.inset(GuiUtils.topCenter(new JPanel(),
                xmlTree.getScroller()), 5));
        return;
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
        Element lastCat = xmlTree.getSelectedElement();
        Element newChild = imageParametersDocument.createElement(TAG_SAVESET);
        newChild.setAttribute(ATTR_NAME, newSet);
        lastCat.appendChild(newChild);
        makeXmlTree();
        return;
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
