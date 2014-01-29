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
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.ui.DndTree;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import edu.wisc.ssec.mcidasv.ParameterSet;
import edu.wisc.ssec.mcidasv.PersistenceManager;


/**
 * Class ParameterTree Gives a tree gui for editing parameter sets
 *
 *
 * @author IDV Development Team
 * @version $Revision$
 */
public class ParameterTree extends DndTree {

    /** The window */
    private JFrame frame;

    /** What is the type of the parameter set we are showing */
    private String parameterType;

    /** The root of the tree */
    private DefaultMutableTreeNode treeRoot;

    /** The tree model */
    private DefaultTreeModel treeModel;

    /** A mapping from tree node to, either, category or ParameterSet */
    private Hashtable nodeToData;

    /** The ui manager */
    private UIManager uiManager;
    
    /** The persistence manager */
    private PersistenceManager persistenceManager;

    /** Icon to use for categories */
    private ImageIcon categoryIcon;

    /** Icon to use for parameters */
    private ImageIcon parameterIcon;




    /**
     * Create the tree with the given parameter set type
     *
     *
     * @param uiManager The UI manager
     * @param parameterType The type of the parameter set we are showing
     */
    public ParameterTree(UIManager uiManager, String parameterType) {

        categoryIcon = GuiUtils.getImageIcon("/auxdata/ui/icons/folder.png",
                                             getClass());
        parameterIcon = GuiUtils.getImageIcon("/auxdata/ui/icons/page.png",
                                           getClass());

        this.uiManager = uiManager;
        this.persistenceManager = (PersistenceManager)(uiManager.getPersistenceManager());

        setToolTipText(
            "<html>Right click to show popup menu.<br>Drag to move parameter sets or categories</html>");

        this.parameterType = parameterType;
        treeRoot = new DefaultMutableTreeNode("Parameter Sets");

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
                if (data instanceof ParameterSet) {
                    setToolTipText(
                        "<html>Right click to show parameter set menu.<br>Drag to move parameter set</html>");
                    setIcon(parameterIcon);
                } else {
                    setToolTipText(
                        "<html>Right click to show category menu.<br>Drag to move parameter sets or categories</html><");
                    setIcon(categoryIcon);
                }
                return this;
            }
        };
        setCellRenderer(renderer);




        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == e.VK_DELETE) {
                    deleteSelected();
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
//                    if (event.getClickCount() > 1) {
//                        if ((data != null) && (data instanceof SavedBundle)) {
//                            doOpen((SavedBundle) data);
//                        }
//                    }
                    return;
                }
                clearSelection();
                addSelectionPath(path);
                final DefaultMutableTreeNode parentNode =
                    (DefaultMutableTreeNode) path.getLastPathComponent();

                JPopupMenu popup = new JPopupMenu();
                if (data == null) {
                    popup.add(GuiUtils.makeMenuItem("Add Subcategory",
                            ParameterTree.this, "addCategory", parentNode));
                } else {
                    if (data instanceof ParameterSet) {
                    	ParameterSet set = (ParameterSet) data;
                        popup.add(GuiUtils.makeMenuItem("Rename",
                        		ParameterTree.this, "doRename", set));
                        popup.add(GuiUtils.makeMenuItem("Delete",
                        		ParameterTree.this, "deleteParameterSet", set));
                    } else {
                        popup.add(GuiUtils.makeMenuItem("Delete Category",
                        		ParameterTree.this, "deleteCategory",
                                data.toString()));
                        popup.add(GuiUtils.makeMenuItem("Add Subcategory",
                        		ParameterTree.this, "addCategory", parentNode));
                    }
                }
                popup.show((Component) event.getSource(), event.getX(),
                           event.getY());
            }
        });
        loadParameterSets();

        String title = "Parameter Set Manager";

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
//        fileMenu.add(GuiUtils.makeMenuItem("Export to File", this,
//                                           "doExport"));
//        fileMenu.add(GuiUtils.makeMenuItem("Export to Plugin", this,
//                                           "doExportToPlugin"));
//        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Close", this, "doClose"));




        JComponent bottom = GuiUtils.wrap(GuiUtils.makeButton("Close", this,
                                "doClose"));

        JPanel contents = GuiUtils.topCenterBottom(menuBar, sp, bottom);
        frame = GuiUtils.createFrame(title);
        frame.getContentPane().add(contents);
        frame.pack();
        frame.setLocation(100, 100);
    }

    /**
     * close
     */
    public void doClose() {
        frame.dispose();
    }


    /**
     * Get the list of selected parameter sets
     *
     * @return The selected parameter sets
     */
    private List getSelectedParameterSets() {
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        if ((paths == null) || (paths.length == 0)) {
            return new ArrayList();
        }
        List sets = new ArrayList();
        for (int i = 0; i < paths.length; i++) {
            Object data = findDataAtPath(paths[i]);
            if (data == null) {
                continue;
            }
            if ( !(data instanceof ParameterSet)) {
                continue;
            }
            sets.add(data);
        }
        return sets;
    }

    /**
     * Show the window
     */
    public void setVisible(boolean visibility) {
        frame.setVisible(visibility);
    }


    /**
     * Ok to drag the node
     *
     * @param sourceNode The node to drag
     *
     * @return Ok to drag
     */
    protected boolean okToDrag(DefaultMutableTreeNode sourceNode) {
        return sourceNode.getParent() != null;
    }


    /**
     * Ok to drop the node
     *
     *
     * @param sourceNode The dragged node
     * @param destNode Where to drop
     *
     * @return Ok to drop
     */
    protected boolean okToDrop(DefaultMutableTreeNode sourceNode,
                               DefaultMutableTreeNode destNode) {


        //Don't drop a parameter set onto the root. It must be in a category
        if (sourceNode.getUserObject() instanceof ParameterSet) {
            if (destNode.getParent() == null) {
                return false;
            }
        }

        if (destNode.getUserObject() instanceof ParameterSet) {
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
        if (sourceNode.getUserObject() instanceof ParameterSet) {
            persistenceManager.moveParameterSet(
            	parameterType,
                (ParameterSet) sourceNode.getUserObject(),
                getCategoryList(destNode));
        } else {
        	persistenceManager.moveParameterSetCategory(
        		parameterType,
                getCategoryList(sourceNode),
                getCategoryList(destNode));
        }

        loadParameterSets();
    }




    /**
     * Create the list of categories
     *
     * @param destNode From where
     *
     * @return List of String categories
     */
    private List getCategoryList(DefaultMutableTreeNode destNode) {
        List categories = new ArrayList();
        while (destNode.getParent() != null) {
            categories.add(0, destNode.getUserObject().toString());
            destNode = (DefaultMutableTreeNode) destNode.getParent();
        }
        return categories;
    }

    /**
     * Load the parameter sets into the tree
     */
    protected void loadParameterSets() {

        Enumeration paths =
            getExpandedDescendants(new TreePath(treeRoot.getPath()));
        Hashtable expandedState =
            GuiUtils.initializeExpandedPathsBeforeChange(this, treeRoot);

        List allCategories = persistenceManager.getAllParameterSetCategories(parameterType);
        nodeToData = new Hashtable();
        treeRoot.removeAllChildren();
        Hashtable catNodes    = new Hashtable();
        Hashtable fakeSets    = new Hashtable();
        List      sets        = new ArrayList();

        //We use a set of fake parameter sets to we include all categories into the tree
        for (int i = 0; i < allCategories.size(); i++) {
            List categories = persistenceManager.stringToCategories((String) allCategories.get(i));
            ParameterSet fakeSet = new ParameterSet("", categories, parameterType);
            fakeSets.put(fakeSet, fakeSet);
            sets.add(fakeSet);
        }
        sets.addAll(persistenceManager.getAllParameterSets(parameterType));
        for (int i = 0; i < sets.size(); i++) {
            ParameterSet           set     = (ParameterSet) sets.get(i);
            List                   categories = set.getCategories();
            DefaultMutableTreeNode catNode    = treeRoot;
            String                 fullCat    = "";
            for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
                String cat = (String) categories.get(catIdx);
                if (fullCat.length() > 0) {
                    fullCat = fullCat + IdvPersistenceManager.CATEGORY_SEPARATOR;
                }
                fullCat = fullCat + cat;
                DefaultMutableTreeNode tmpNode =  (DefaultMutableTreeNode) catNodes.get(fullCat);
                if (tmpNode == null) {
                    tmpNode = new DefaultMutableTreeNode(cat);
                    nodeToData.put(tmpNode, fullCat);
                    catNode.add(tmpNode);
                    catNodes.put(fullCat, tmpNode);
                }
                catNode = tmpNode;
            }
            //Skip over the fake ones
            if (fakeSets.get(set) == null) {
                DefaultMutableTreeNode setNode = new DefaultMutableTreeNode(set);
                nodeToData.put(setNode, set);
                catNode.add(setNode);
            }
        }
        treeModel.nodeStructureChanged(treeRoot);
        GuiUtils.expandPathsAfterChange(this, expandedState, treeRoot);
    }


    /**
     * Find the data (either a ParameterSet or a category)
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
            (DefaultMutableTreeNode) path.getLastPathComponent();
        if (last == null) {
            return null;
        }
        return nodeToData.get(last);
    }

    /**
     * Delete the selected item in the tree
     */
    public void deleteSelected() {
    	System.out.println("deleteSelected");
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        if ((paths == null) || (paths.length == 0)) {
            return;
        }
        Object data = findDataAtPath(paths[0]);
        if (data == null) {
            return;
        }
        if (data instanceof ParameterSet) {
            deleteParameterSet((ParameterSet) data);
        } else {
            deleteCategory(data.toString());
        }
    }


    /**
     * Delete the given parameter set
     *
     * @param parameterSet The parameter set to delete
     */
    public void deleteParameterSet(ParameterSet parameterSet) {
        if ( !GuiUtils.askYesNo(
                "Parameter set delete confirmation",
                "Are you sure you want to delete the parameter set \"" + parameterSet
                + "\"  ?")) {
            return;
        }
        System.out.println("deleteParameterSet");
        persistenceManager.deleteParameterSet(parameterType, parameterSet);
        loadParameterSets();
    }


    /**
     * Delete the given parameter set category
     *
     * @param category The category to delete
     */
    public void deleteCategory(String category) {
        if ( !GuiUtils.askYesNo(
                "Parameter Set Category Delete Confirmation",
                "<html>Are you sure you want to delete the category:<p> <center>\""
                + category
                + "\"</center> <br> and all parameter sets and categories under it?</html>")) {
            return;
        }
        System.out.println("deleteCategory");
        persistenceManager.deleteParameterSetCategory(parameterType, category);
        loadParameterSets();
    }

    /**
     * Create a new category under the given node
     *
     * @param parentNode The parent tree node
     */
    public void addCategory(DefaultMutableTreeNode parentNode) {
        String cat =
            GuiUtils.getInput("Please enter the new category name",
                              "Name: ", "");
        if (cat == null) {
            return;
        }
        String parentCat = (String) nodeToData.get(parentNode);
        String fullCat   = ((parentCat == null)
                            ? cat
                            : (parentCat
                               + IdvPersistenceManager.CATEGORY_SEPARATOR
                               + cat));
        System.out.println("addCategory");
        if ( !persistenceManager.addParameterSetCategory(parameterType, fullCat)) {
            LogUtil.userMessage(
                "A category with the given name already exists");
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
     * Rename the parameter set
     * @param parameterSet the parameter set
     */
    public void doRename(ParameterSet parameterSet) {
    	System.out.println("doRename");
        persistenceManager.renameParameterSet(parameterType, parameterSet);
    }

}

