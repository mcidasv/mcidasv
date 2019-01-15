/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2019
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

package ucar.unidata.idv.ui;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.util.McVTextField;
import org.w3c.dom.Document;

import org.w3c.dom.Element;

import ucar.unidata.idv.*;

import ucar.unidata.ui.DndTree;

import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

/**
 * Class BundleTree Gives a tree gui for editing bundles
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.34 $
 */
public class BundleTree extends DndTree {

    /** action command */
    public static final String CMD_EXPORT_TO_PLUGIN = "Export to Plugin";

    /** The window */
    private JFrame frame;

    /** What is the type of the bundles we are showing */
    private int bundleType;

    /** The root of the tree */
    private DefaultMutableTreeNode treeRoot;

    /** The tree model */
    private DefaultTreeModel treeModel;

    /** A mapping from tree node to, either, category or SavedBundle */
    private Hashtable<DefaultMutableTreeNode, Object> nodeToData;

    /** The ui manager */
    private IdvUIManager uiManager;

    /** Icon to use for categories */
    private ImageIcon categoryIcon;

    /** Icon to use for bundles */
    private ImageIcon bundleIcon;

    /**
     * Create the tree with the given bundle type
     *
     *
     * @param uiManager The UI manager
     * @param bundleType The type of the bundles we are showing
     */
    public BundleTree(final IdvUIManager uiManager, int bundleType) {

        categoryIcon = GuiUtils.getImageIcon("/auxdata/ui/icons/folder.png",
                                             getClass());
        bundleIcon = GuiUtils.getImageIcon("/auxdata/ui/icons/page.png",
                                           getClass());

        this.uiManager = uiManager;

        setToolTipText(
            "<html>Right click to show popup menu.<br>Drag to move bundles or categories</html>");

        this.bundleType = bundleType;
        treeRoot = new DefaultMutableTreeNode(
            getPersistenceManager().getBundleTitle(getBundleType()));

        //        setRootVisible(false);
        setShowsRootHandles(true);
        treeModel = new DefaultTreeModel(treeRoot);
        setModel(treeModel);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree theTree,
                    Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {

                super.getTreeCellRendererComponent(theTree, value, sel,
                        expanded, leaf, row, hasFocus);
                if ((nodeToData == null) || (value == null)) {
                    return this;
                }
                Object data = nodeToData.get(value);
                if (data == null) {
                    setIcon(categoryIcon);
                    return this;
                }
                if (data instanceof SavedBundle) {
                    setToolTipText(
                        "<html>Right click to show bundle menu.<br>Drag to move bundle</html>");
                    setIcon(bundleIcon);
                } else {
                    setToolTipText(
                        "<html>Right click to show category menu.<br>Drag to move bundles or categories</html><");
                    setIcon(categoryIcon);
                }
                return this;
            }
        };
        setCellRenderer(renderer);

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (GuiUtils.isDeleteEvent(e)) {
                    deleteSelected();
                } else if (e.getKeyCode() == e.VK_ENTER) {
                    SavedBundle bundle = findSelectedBundle();
                    if (bundle != null) {
                        doOpen(bundle);
                    }
                }
            }
        });

        getSelectionModel().setSelectionMode(
            TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        //            TreeSelectionModel.SINGLE_TREE_SELECTION);


        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                TreePath path = getPathForLocation(event.getX(),
                                    event.getY());
                Object data = findDataAtPath(path);
                if ( !SwingUtilities.isRightMouseButton(event)) {
                    if (event.getClickCount() > 1) {
                        if (data instanceof SavedBundle) {
                            doOpen((SavedBundle) data);
                        }
                    }
                    return;
                }
                clearSelection();
                addSelectionPath(path);
                final DefaultMutableTreeNode parentNode =
                    (DefaultMutableTreeNode) path.getLastPathComponent();

                JPopupMenu popup = new JPopupMenu();
                if (data == null) {
                    popup.add(GuiUtils.makeMenuItem("Add Sub-Category",
                            BundleTree.this, "addCategory", parentNode));
                } else {
                    if (data instanceof SavedBundle) {
                        SavedBundle bundle = (SavedBundle)data;
                        popup.add(GuiUtils.makeMenuItem("Open",
                                BundleTree.this, "doOpen", bundle));
                        popup.add(GuiUtils.makeMenuItem("Rename",
                                BundleTree.this, "doRename", bundle));
                        popup.add(GuiUtils.makeMenuItem("Export",
                                BundleTree.this, "doExport", bundle));
                        popup.add(GuiUtils.makeMenuItem("Export to Plugin",
                                BundleTree.this, "doExportToPlugin", bundle));
                        popup.add(GuiUtils.makeMenuItem("Delete",
                                BundleTree.this, "deleteBundle", bundle));
                    } else {
                        popup.add(GuiUtils.makeMenuItem("Import Bundle",
                                BundleTree.this, "doImport", parentNode));
                        popup.add(GuiUtils.makeMenuItem("Delete Category",
                                BundleTree.this, "deleteCategory",
                                data.toString()));
                        popup.add(GuiUtils.makeMenuItem("Add Sub-Category",
                                BundleTree.this, "addCategory", parentNode));
                        popup.add(GuiUtils.makeMenuItem("Rename Sub-Category", BundleTree.this, "renameCategory", parentNode));
                    }
                }
                popup.show((Component) event.getSource(), event.getX(),
                           event.getY());
            }
        });
        loadBundles();

        String title =
            "Local "
            + getPersistenceManager().getBundleTitle(getBundleType())
            + " Manager";
        Dimension defaultDimension = new Dimension(300, 400);
        JScrollPane sp = GuiUtils.makeScrollPane(this,
                             (int) defaultDimension.getWidth(),
                             (int) defaultDimension.getHeight());
        sp.setPreferredSize(defaultDimension);


        JMenuBar menuBar  = new JMenuBar();
        JMenu    fileMenu = new JMenu("File");
        JMenu    helpMenu = new JMenu("Help");
        menuBar.add(fileMenu);
        //        menuBar.add(helpMenu);
        fileMenu.add(GuiUtils.makeMenuItem("Export to File", this,
                                           "doExport"));
        fileMenu.add(GuiUtils.makeMenuItem("Export to Plugin", this,
                                           "doExportToPlugin"));
        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Close", this, "doClose"));


        final XmlObjectStore store = uiManager.getIdv().getStore();
        final boolean showSystemBundles =
            store.get(Constants.PREF_SHOW_SYSTEM_BUNDLES, true);
        final JCheckBox showSystemBox =
            new JCheckBox("Show McIDAS-V system bundles", showSystemBundles);
        showSystemBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                boolean value = showSystemBox.isSelected();
                store.put(Constants.PREF_SHOW_SYSTEM_BUNDLES, value);
                uiManager.favoriteBundlesChanged();
            }
        });

        JComponent close = GuiUtils.wrap(GuiUtils.makeButton("Close", this,
                                "doClose"));
        JPanel bottom = GuiUtils.topBottom(showSystemBox, close);

        JPanel contents = GuiUtils.topCenterBottom(menuBar, sp, bottom);
        frame = GuiUtils.createFrame(title);
        frame.getContentPane().add(contents);
        frame.pack();
        frame.setLocation(100, 100);
    }

    /**
     * Open the bundle
     * @param bundle the bundle
     */
    public void doOpen(SavedBundle bundle) {
        getPersistenceManager().open(bundle);
    }

    /**
     * Rename the bundle
     * @param bundle the bundle
     */
    public void doRename(SavedBundle bundle) {
        getPersistenceManager().rename(bundle, getBundleType());
    }

    /**
     * Export the bundle
     * @param bundle the bundle
     */
    public void doExport(SavedBundle bundle) {
        getPersistenceManager().export(bundle, getBundleType());
    }

    /**
     * Export the bundle
     * @param bundle the bundle
     */
    public void doExportToPlugin(SavedBundle bundle) {
        uiManager.getIdv().getPluginManager().addObject(bundle);
    }

    /**
     * close
     */
    public void doClose() {
        frame.dispose();
    }

    /**
     * Get the list of selected bundles
     *
     * @return The selected bundles
     */
    private List<SavedBundle> getSelectedBundles() {
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        if ((paths == null) || (paths.length == 0)) {
            return Collections.emptyList();
        }
        List<SavedBundle> bundles = new ArrayList<>(paths.length);
        for (int i = 0; i < paths.length; i++) {
            Object data = findDataAtPath(paths[i]);
            if (data == null) {
                continue;
            }
            if ( !(data instanceof SavedBundle)) {
                continue;
            }
            bundles.add((SavedBundle)data);
        }
        return bundles;
    }

    /**
     * Export the selected bundles to the plugin creator.
     */
    public void doExportToPlugin() {
        List bundles = getSelectedBundles();
        if (bundles.isEmpty()) {
            LogUtil.userMessage("No bundles are selected");
            return;
        }
        uiManager.getIdv().getPluginManager().addObjects(bundles);
    }

    /**
     * Export the selected bundles.
     */
    public void doExport() {
        try {
            List bundles = getSelectedBundles();
            if (bundles.isEmpty()) {
                LogUtil.userMessage("No bundles are selected!");
                return;
            }

            String filename =
                FileManager.getWriteFile(FileManager.FILTER_XML,
                                         FileManager.SUFFIX_XML);
            if (filename == null) {
                return;
            }

            File    dir      = new File(filename).getParentFile();

            boolean anyExist = false;
            for (int i = 0; i < bundles.size(); i++) {
                SavedBundle bundle = (SavedBundle) bundles.get(i);
                if (new File(
                        IOUtil.joinDir(
                            dir,
                            IOUtil.getFileTail(bundle.getUrl()))).exists()) {
                    anyExist = true;
                    break;
                }
            }
            if (anyExist) {
                if ( !GuiUtils.showOkCancelDialog(null,
                        "Overwrite Bundle Files",
                        new JLabel("<html>One or more bundle files already exist.<br>Do you want to overwrite them?</html>"),
                        null, null)) {
                    return;
                }
            }

            Document doc  = XmlUtil.makeDocument();
            Element  root = doc.createElement(SavedBundle.TAG_BUNDLES);
            for (int i = 0; i < bundles.size(); i++) {
                SavedBundle bundle = (SavedBundle)bundles.get(i);
                bundle.toXml(doc, root);
            }

            String xml = XmlUtil.toString(doc.getDocumentElement());
            IOUtil.writeFile(filename, xml);
            String msg =
                "<html>The selected bundles have been exported. The files are:<br>";

            for (int i = 0; i < bundles.size(); i++) {
                SavedBundle bundle = (SavedBundle) bundles.get(i);
                String      tail   = IOUtil.getFileTail(bundle.getUrl());
                msg += "&nbsp;&nbsp;<b>" + tail + "</b><br>";
                File toFile = new File(IOUtil.joinDir(dir, tail));
                IOUtil.copyFile(new File(bundle.getUrl()), toFile);
            }
            msg += "</html>";
            LogUtil.userMessage(msg);
        } catch (Exception exc) {
            LogUtil.logException("Exporting bundles", exc);
        }
    }

    /**
     * Return the bundle type.
     *
     * @return Bundle type.
     */
    private int getBundleType() {
        return bundleType;
    }

    /**
     * Return the persistence manager
     *
     * @return Persistence manager.
     */
    private IdvPersistenceManager getPersistenceManager() {
        return uiManager.getPersistenceManager();
    }

    /**
     * Show or hide {@link #frame}.
     *
     * @param visible if {@code true}, show {@code frame}. Otherwise hides
     * {@code frame}.
     */
    @Override public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    /**
     * Ok to drag the node.
     *
     * @param sourceNode Node to drag.
     *
     * @return Ok to drag
     */
    protected boolean okToDrag(DefaultMutableTreeNode sourceNode) {
        return sourceNode.getParent() != null;
    }

    /**
     * Ok to drop the node.
     *
     * @param sourceNode Dragged node.
     * @param destNode Where to drop {@code sourceNode}.
     *
     * @return Ok to drop.
     */
    protected boolean okToDrop(DefaultMutableTreeNode sourceNode,
                               DefaultMutableTreeNode destNode) {

        //Don't drop a bundle onto the root. It must be in a catgegory
        if (sourceNode.getUserObject() instanceof SavedBundle) {
            if (destNode.getParent() == null) {
                return false;
            }
        }

        if (destNode.getUserObject() instanceof SavedBundle) {
            return false;
        }
        if (destNode == sourceNode.getParent()) {
            return false;
        }
        while (destNode != null) {
            if (destNode == sourceNode) {
                return false;
            }
            destNode = (DefaultMutableTreeNode) destNode.getParent();
        }
        return true;
    }

    /**
     * Handle the DND drop
     *
     *
     * @param sourceNode The dragged node
     * @param destNode Where to drop
     */
    protected void doDrop(DefaultMutableTreeNode sourceNode,
                          DefaultMutableTreeNode destNode) {
        if (sourceNode.getUserObject() instanceof SavedBundle) {
            uiManager.getPersistenceManager().moveBundle(
                (SavedBundle) sourceNode.getUserObject(),
                getCategoryList(destNode), bundleType);
        } else {
            uiManager.getPersistenceManager().moveCategory(
                getCategoryList(sourceNode), getCategoryList(destNode),
                bundleType);
        }
        loadBundles();
    }

    /**
     * Create the list of categories
     *
     * @param destNode From where
     *
     * @return List of String categories
     */
    private List<String> getCategoryList(DefaultMutableTreeNode destNode) {
        List<String> categories = new ArrayList<>();
        while (destNode.getParent() != null) {
            categories.add(0, destNode.getUserObject().toString());
            destNode = (DefaultMutableTreeNode)destNode.getParent();
        }
        return categories;
    }

    /**
     * Delete the selected item in the tree
     */
    public void deleteSelected() {
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        if ((paths == null) || (paths.length == 0)) {
            return;
        }
        Object data = findDataAtPath(paths[0]);
        if (data == null) {
            return;
        }
        if (data instanceof SavedBundle) {
            deleteBundle((SavedBundle) data);
        } else {
            deleteCategory(data.toString());
        }
    }

    /**
     * Find and return the selected bundle. May return null if none selected
     *
     * @return Selected bundle
     */
    public SavedBundle findSelectedBundle() {
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        if ((paths == null) || (paths.length == 0)) {
            return null;
        }
        Object data = findDataAtPath(paths[0]);
        if (data == null) {
            return null;
        }
        if (data instanceof SavedBundle) {
            return (SavedBundle) data;
        }
        return null;
    }

    /**
     * Load in the bundles into the tree
     */
    protected void loadBundles() {

        Enumeration paths =
            getExpandedDescendants(new TreePath(treeRoot.getPath()));
        Hashtable expandedState =
            GuiUtils.initializeExpandedPathsBeforeChange(this, treeRoot);

        List allCategories =
            uiManager.getPersistenceManager().getAllCategories(bundleType);
        nodeToData = new Hashtable<>();
        treeRoot.removeAllChildren();
        Hashtable catNodes    = new Hashtable();
        Hashtable fakeBundles = new Hashtable();
        List      bundles     = new ArrayList();

        //We use a set of fake bundles to we include all categories into the tree
        for (int i = 0; i < allCategories.size(); i++) {
            List categories =
                uiManager.getPersistenceManager().stringToCategories(
                    (String) allCategories.get(i));
            SavedBundle fakeBundle = new SavedBundle("", "", categories);
            fakeBundles.put(fakeBundle, fakeBundle);
            bundles.add(fakeBundle);
        }
        bundles.addAll(
            uiManager.getPersistenceManager().getWritableBundles(bundleType));
        for (int i = 0; i < bundles.size(); i++) {
            SavedBundle            bundle     = (SavedBundle) bundles.get(i);
            List                   categories = bundle.getCategories();
            DefaultMutableTreeNode catNode    = treeRoot;
            String                 fullCat    = "";
            for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
                String cat = (String) categories.get(catIdx);
                if (fullCat.length() > 0) {
                    fullCat = fullCat
                              + IdvPersistenceManager.CATEGORY_SEPARATOR;
                }
                fullCat = fullCat + cat;
                DefaultMutableTreeNode tmpNode =
                    (DefaultMutableTreeNode) catNodes.get(fullCat);
                if (tmpNode == null) {
                    tmpNode = new DefaultMutableTreeNode(cat);
                    nodeToData.put(tmpNode, fullCat);
                    catNode.add(tmpNode);
                    catNodes.put(fullCat, tmpNode);
                }
                catNode = tmpNode;
            }
            //Skip over the fake ones
            if (fakeBundles.get(bundle) == null) {
                DefaultMutableTreeNode bundleNode =
                    new DefaultMutableTreeNode(bundle);
                nodeToData.put(bundleNode, bundle);
                catNode.add(bundleNode);
            }
        }
        treeModel.nodeStructureChanged(treeRoot);
        GuiUtils.expandPathsAfterChange(this, expandedState, treeRoot);
    }

    /**
     * Delete the given bundle
     *
     * @param bundle The bundle to delete
     */
    public void deleteBundle(SavedBundle bundle) {
        if ( !GuiUtils.askYesNo(
                "Bundle delete confirmation",
                "Are you sure you want to delete the bundle \"" + bundle
                + "\"  ?")) {
            return;
        }
        uiManager.getPersistenceManager().deleteBundle(bundle.getUrl());
        loadBundles();
    }

    /**
     * Create a new category under the given node
     *
     * @param parentNode The parent tree node
     */
    public void addCategory(DefaultMutableTreeNode parentNode) {
        String cat = getInput("Please enter the new sub-category name", "Name: ", "", "", null, "", 20, null);
        if (cat == null) {
            return;
        }
        String parentCat = (String) nodeToData.get(parentNode);
        String fullCat   = (parentCat == null)
                           ? cat
                           : (parentCat
                              + IdvPersistenceManager.CATEGORY_SEPARATOR
                              + cat);
        if ( !uiManager.getPersistenceManager().addBundleCategory(bundleType,
                fullCat)) {
            LogUtil.userMessage(
                "A subcategory with the given name already exists");
            return;
        }
        DefaultMutableTreeNode newCatNode = new DefaultMutableTreeNode(cat);
        nodeToData.put(newCatNode, fullCat);
        parentNode.add(newCatNode);


        Hashtable expandedState =
            GuiUtils.initializeExpandedPathsBeforeChange(this, treeRoot);
        treeModel.nodeStructureChanged(treeRoot);
        GuiUtils.expandPathsAfterChange(this, expandedState, treeRoot);
    }

    /**
     * Rename an existing category.
     *
     * @param renameNode Node representing the category to be renamed.
     * Cannot be {@code null}.
     */
    public void renameCategory(DefaultMutableTreeNode renameNode) {
        String newCategory = getInput("Please enter the new sub-category name", "New Name: ", "", "", null, "", 20, null);
        if (newCategory != null) {
            String originalCategory     = renameNode.toString();
            String parentCategory       = (String) nodeToData.get(renameNode.getParent());
            String originalFullCategory = (parentCategory == null)
                                          ? originalCategory
                                          : (parentCategory
                                             + IdvPersistenceManager.CATEGORY_SEPARATOR
                                             + originalCategory);
            String newFullCategory      = (parentCategory == null)
                                          ? newCategory
                                          : (parentCategory
                                             + IdvPersistenceManager.CATEGORY_SEPARATOR
                                             + newCategory);

            boolean status =
                uiManager.getPersistenceManager().renameBundleCategory(bundleType,
                    originalFullCategory, newFullCategory);

            if (status) {
                nodeToData.put(renameNode, newFullCategory);
                renameNode.setUserObject(newCategory);
                treeModel.nodeChanged(renameNode);
                uiManager.favoriteBundlesChanged();
            } else {
                LogUtil.userMessage("Could not rename '" + renameNode.toString() + "' to '" + newCategory + "'.");
            }
        }
    }

    /**
     * Create a new category under the given node
     *
     * @param parentNode The parent tree node
     */
    public void doImport(DefaultMutableTreeNode parentNode) {
        String filename =
            FileManager
                .getReadFile("Import Bundle",
                             Misc
                             .newList(uiManager.getIdv().getArgsManager()
                                 .getXidvZidvFileFilter()));
        if (filename == null) {
            return;
        }

        String fullCat = (String) nodeToData.get(parentNode);
        uiManager.getPersistenceManager().doImport(bundleType, filename,
                fullCat);
        Hashtable expandedState =
            GuiUtils.initializeExpandedPathsBeforeChange(this, treeRoot);
        treeModel.nodeStructureChanged(treeRoot);
        GuiUtils.expandPathsAfterChange(this, expandedState, treeRoot);
    }

    /**
     * Delete the given bundle category
     *
     * @param category The category to delete
     */
    public void deleteCategory(String category) {
        if ( !GuiUtils.askYesNo(
                "Bundle Category Delete Confirmation",
                "<html>Are you sure you want to delete the category:<p> <center>\""
                + category
                + "\"</center> <br> and all bundles and categories under it?</html>")) {
            return;
        }
        uiManager.getPersistenceManager().deleteBundleCategory(bundleType,
                category);
        loadBundles();
    }

    /**
     * Find the data (either a SavedBundle or a category)
     * associated with the given  tree path
     *
     * @param path The path
     *
     * @return The data
     */
    private Object findDataAtPath(TreePath path) {
        if ((path == null) || (nodeToData == null)) {
            return null;
        }
        DefaultMutableTreeNode last =
            (DefaultMutableTreeNode)path.getLastPathComponent();
        if (last == null) {
            return null;
        }
        return nodeToData.get(last);
    }

    /**
     * Ask the user the question. Return their response or null.
     *
     * @param question The question.
     * @param label Extra label.
     * @param initValue Initial value of answer
     * @param trailingLabel Label after the text field.
     * @param underLabel Label under the text field.
     * @param title for the dialog box.
     * @param fieldWidth Field width
     * @param nearComponent If non-null then show the dialog near this component
     *
     * @return The user's response
     */
    public static String getInput(String question, String label,
                                  String initValue, String trailingLabel,
                                  Object underLabel, String title,
                                  int fieldWidth, JComponent nearComponent)
    {
        final JDialog      dialog = GuiUtils.createDialog(title, true);
        final McVTextField field  = new McVTextField((initValue == null) ? "" : initValue, fieldWidth);
        field.setDeny('\\', '/', ':', '*', '?', '"', '<', '>', '|');
        ObjectListener listener = new ObjectListener(false) {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if ((ae.getSource() == field) || cmd.equals(GuiUtils.CMD_OK)) {
                    theObject = true;
                } else {
                    theObject = false;
                }
                dialog.setVisible(false);
            }
        };
        field.addActionListener(listener);
        List comps = new ArrayList();
        if (question != null) {
            comps.add(LayoutUtil.left(LayoutUtil.inset(new JLabel(question), 4)));
        }
        if (trailingLabel != null) {
            comps.add(LayoutUtil.left(LayoutUtil.centerRight(GuiUtils.label(label, field),
                new JLabel(trailingLabel))));
        } else {
            comps.add(LayoutUtil.left(GuiUtils.label(label, field)));
        }

        if (underLabel != null) {
            if (underLabel instanceof String) {
                comps.add(LayoutUtil.left(new JLabel(underLabel.toString())));
            } else if (underLabel instanceof Component) {
                comps.add(LayoutUtil.left((Component) underLabel));
            }
        }

        JComponent contents = LayoutUtil.inset(LayoutUtil.centerBottom(LayoutUtil.vbox(comps),
            GuiUtils.makeOkCancelButtons(listener)), 4);

        GuiUtils.packDialog(dialog, contents);
        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
        if (nearComponent != null) {
            GuiUtils.showDialogNearSrc(nearComponent, dialog);
        } else {
            Point ctr = new Point(ss.width / 2 - 100, ss.height / 2 - 100);
            dialog.setLocation(ctr);
            dialog.setVisible(true);
        }
        if ( !((Boolean) listener.getObject()).booleanValue()) {
            return null;
        }
        return field.getText();
    }

}
