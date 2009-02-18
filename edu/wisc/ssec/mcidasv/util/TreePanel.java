package edu.wisc.ssec.mcidasv.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Hashtable;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

/**
 *  This class shows a tree on the left and a card panel on the right.
 *  
 *  Ripped right out of the IDV, for the time being.
 */
public class TreePanel extends JPanel implements TreeSelectionListener {

    public static final String CATEGORY_DELIMITER = ">";

    /** the tree */
    private JTree tree;

    /** The scroller */
    private JScrollPane treeView;

    /** _more_ */
    private boolean useSplitPane = true;

    /** _more_ */
    private int treeWidth = -1;

    /** The root */
    private DefaultMutableTreeNode root;

    /** the model */
    private DefaultTreeModel treeModel;

    /** The panel */
    private GuiUtils.CardLayoutPanel panel;

    /** _more_ */
    private JPanel emptyPanel;

    /** _more_ */
    private Hashtable catComponents = new Hashtable();

    /** Maps categories to tree node */
    private Hashtable catToNode = new Hashtable();

    /** Maps components to tree node */
    private Hashtable compToNode = new Hashtable();

    /** ok to respond to selection changes */
    private boolean okToUpdateTree = true;

    /**
     * ctor
     */
    public TreePanel() {
        this(true, -1);
    }

    /**
     * _more_
     *
     * @param useSplitPane _more_
     * @param treeWidth _more_
     */
    public TreePanel(boolean useSplitPane, int treeWidth) {
        this.useSplitPane = useSplitPane;
        this.treeWidth    = treeWidth;

        setLayout(new BorderLayout());
        root      = new DefaultMutableTreeNode("");
        treeModel = new DefaultTreeModel(root);
        tree      = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree theTree,
                    Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {

                super.getTreeCellRendererComponent(theTree, value, sel,
                        expanded, leaf, row, hasFocus);
                if ( !(value instanceof MyTreeNode)) {
                    return this;
                }
                MyTreeNode node = (MyTreeNode) value;
                if (node.icon != null) {
                    setIcon(node.icon);
                } else {
                    setIcon(null);
                }
                return this;
            }
        };
        renderer.setIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        tree.setCellRenderer(renderer);

        panel = new GuiUtils.CardLayoutPanel() {
            public void show(Component comp) {
                super.show(comp);
                showPath(panel.getVisibleComponent());
            }
        };

        catToNode = new Hashtable();
        treeView  = new JScrollPane(tree);

        if (treeWidth > 0) {
            treeView.setPreferredSize(new Dimension(treeWidth, 100));
        }

        JComponent center;
        if (useSplitPane) {
            JSplitPane splitPane = ((treeWidth > 0)
                                    ? GuiUtils.hsplit(treeView, panel,
                                        treeWidth)
                                    : GuiUtils.hsplit(treeView, panel, 150));
            center = splitPane;
            splitPane.setOneTouchExpandable(true);
        } else {
            center = GuiUtils.leftCenter(treeView, panel);
        }
        this.add(BorderLayout.CENTER, center);
        tree.addTreeSelectionListener(this);
    }

    public Component getVisibleComponent() {
        return panel.getVisibleComponent();
    }

    /**
     * Handle tree selection changed
     *
     * @param e event
     */
    public void valueChanged(TreeSelectionEvent e) {
        if ( !okToUpdateTree) {
            return;
        }

        DefaultMutableTreeNode node =
            (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            TwoFacedObject tfo = (TwoFacedObject) node.getUserObject();
            panel.show((Component) tfo.getId());
        } else {
            if (emptyPanel == null) {
                if (emptyPanel == null) {
                    panel.addCard(emptyPanel =
                        new JPanel(new BorderLayout()));
                }
            }
            if (node.getUserObject() instanceof TwoFacedObject) {
                TwoFacedObject tfo = (TwoFacedObject) node.getUserObject();
                JComponent interior =
                    (JComponent) catComponents.get(tfo.getId());
                if (interior != null) {
                    if ( !panel.contains(interior)) {
                        panel.addCard(interior);
                    }
                    panel.show(interior);
                    return;
                }
            }
            panel.show(emptyPanel);
        }
    }

    public void setIcon(Component comp,ImageIcon icon) {
        MyTreeNode node =
            (MyTreeNode) compToNode.get(comp);
        if(node!=null) {
            node.icon = icon;
            tree.repaint();
        }
    }

    /**
     * Add the component to the panel
     *
     * @param component component
     * @param category tree category. May be null.
     * @param label Tree node label
     * @param icon Node icon. May be null.
     */
    public void addComponent(JComponent component, String category,
                             String label, ImageIcon icon) {
        TwoFacedObject         tfo = new TwoFacedObject(label, component);
        DefaultMutableTreeNode panelNode = new MyTreeNode(tfo, icon);
        compToNode.put(component, panelNode);
        if (category == null) {
            root.add(panelNode);
        } else {
            List toks = StringUtil.split(category, CATEGORY_DELIMITER, true, true);
            String                 catSoFar = "";
            DefaultMutableTreeNode catNode  = root;
            for (int i = 0; i < toks.size(); i++) {
                String cat = (String) toks.get(i);
                catSoFar = catSoFar + CATEGORY_DELIMITER + cat;
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) catToNode.get(catSoFar);
                if (node == null) {
                    TwoFacedObject catTfo = new TwoFacedObject(cat, catSoFar);
                    node = new DefaultMutableTreeNode(catTfo);
                    catToNode.put(catSoFar, node);
                    catNode.add(node);
                }
                catNode = node;
            }
            catNode.add(panelNode);
        }
        panel.addCard(component);
        treeChanged();
    }

    private void treeChanged() {
        Hashtable stuff = GuiUtils.initializeExpandedPathsBeforeChange(tree,
                              root);
        treeModel.nodeStructureChanged(root);
        GuiUtils.expandPathsAfterChange(tree, stuff, root);
    }

    /**
     * _more_
     *
     * @param cat _more_
     * @param comp _more_
     */
    public void addCategoryComponent(String cat, JComponent comp) {
        catComponents.put(CATEGORY_DELIMITER + cat, comp);
    }

    /**
     * _more_
     *
     * @param component _more_
     */
    public void removeComponent(JComponent component) {
        DefaultMutableTreeNode node =
            (DefaultMutableTreeNode) compToNode.get(component);
        if (node == null) {
            return;
        }
        compToNode.remove(component);
        if (node.getParent() != null) {
            node.removeFromParent();
        }
        panel.remove(component);
        treeChanged();
    }

    public void show(Component component) {
        panel.show(component);
    }

    /**
     * Show the tree node that corresponds to the component
     *
     * @param component comp
     */
    public void showPath(Component component) {
        if (component != null) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) compToNode.get(component);
            if (node != null) {
                TreePath path = new TreePath(treeModel.getPathToRoot(node));
                okToUpdateTree = false;
                tree.setSelectionPath(path);
                tree.expandPath(path);
                okToUpdateTree = true;
            }
        }
    }

    /**
     * Open all paths
     */
    public void openAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandPath(tree.getPathForRow(i));
        }
        showPath(panel.getVisibleComponent());
    }

    /**
     * Close all paths
     */
    public void closeAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.collapsePath(tree.getPathForRow(i));
        }
        showPath(panel.getVisibleComponent());
    }

    /**
     * Class MyTreeNode _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision$
     */
    private static class MyTreeNode extends DefaultMutableTreeNode {

        /** icon */
        public ImageIcon icon;

        /**
         * ctor
         *
         * @param o object
         * @param icon icon
         */
        public MyTreeNode(Object o, ImageIcon icon) {
            super(o);
            this.icon = icon;
        }
    }
}