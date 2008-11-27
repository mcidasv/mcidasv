package edu.wisc.ssec.mcidasv.startupmanager.options;

import java.awt.Dimension;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import edu.wisc.ssec.mcidasv.ArgumentManager;
import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.startupmanager.StartupManager.TreeCellRenderer;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.OptionPlatform;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Type;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Visibility;

public class DirectoryOption extends AbstractOption {
    private DefaultMutableTreeNode selected = null;
    private String value = "";
    public DirectoryOption(final String id, final String label, final String defaultValue, final OptionPlatform optionPlatform, final Visibility optionVisibility) {
        super(id, label, Type.DIRTREE, optionPlatform, optionVisibility);
        setValue(defaultValue);
    }

    private void exploreDirectory(final String directory, final DefaultMutableTreeNode parent) {
        assert directory != null : "Cannot traverse a null directory";

        File dir = new File(directory);
        assert dir.exists() : "Cannot traverse a directory that does not exist";

        for (File f : dir.listFiles()) {
            DefaultMutableTreeNode current = new DefaultMutableTreeNode(f);
            if (f.isDirectory()) {
                parent.add(current);
                exploreDirectory(f.getPath(), current);
            } else if (ArgumentManager.isBundle(f.getPath())){
                parent.add(current);
                if (f.getPath().equals(getValue()))
                    selected = current;
            }
        }
    }

    private DefaultMutableTreeNode getRootNode(final String path) {
        File bundleDir = new File(path);
        if (bundleDir.isDirectory()) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(bundleDir);
            return root;
        }
        return null;
    }

    public JComponent getComponent() {
        String path = StartupManager.INSTANCE.getPlatform().getUserBundles();
        DefaultMutableTreeNode root = getRootNode(path);
        if (root == null)
            throw new AssertionError("Directory missing; can't traverse "+path);
        final JTree tree = new JTree(root);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(final TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
                setValue(node.toString());
            }
        });
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        JScrollPane scroller = new JScrollPane(tree);
        exploreDirectory(path, root);


        ToolTipManager.sharedInstance().registerComponent(tree);
        tree.setCellRenderer(new TreeCellRenderer());
        scroller.setPreferredSize(new Dimension(140,130));
        tree.expandRow(0);

        if (selected != null) {
            TreePath nodePath = new TreePath(selected.getPath());
            tree.setSelectionPath(nodePath);
            tree.scrollPathToVisible(nodePath);
        }
        return scroller;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String newValue) {
        value = newValue;
    }

    public String toString() {
        return String.format("[DirectoryOption@%x: optionId=%s, value=%s]", hashCode(), getOptionId(), getValue());
    }
}
