/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
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

package edu.wisc.ssec.mcidasv.startupmanager.options;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import edu.wisc.ssec.mcidasv.ArgumentManager;
import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.startupmanager.StartupManager.TreeCellRenderer;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.OptionPlatform;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Type;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Visibility;
import edu.wisc.ssec.mcidasv.util.MakeToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a startup option that should be selected from the contents of a
 * given directory. The visual representation of this class is a tree.
 */
public final class DirectoryOption extends AbstractOption {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryOption.class);

    /** Regular expression pattern for ensuring that no quote marks are present in {@link #value}. */
    private static final Pattern CLEAN_VALUE_REGEX = Pattern.compile("\"");

    /** Selected tree node. Value may be {@code null}. */
    private DefaultMutableTreeNode selected = null;

    /** Current option value. Empty {@code String} signifies no selection. */
    private String value = "";

    /** Default value of this option. */
    private final String defaultValue;

    public DirectoryOption(final String id, final String label, final String defaultValue, final OptionPlatform optionPlatform, final Visibility optionVisibility) {
        super(id, label, Type.DIRTREE, optionPlatform, optionVisibility);
        this.defaultValue = defaultValue;
        setValue(defaultValue);
    }

    private void exploreDirectory(final String directory, final DefaultMutableTreeNode parent) {
        assert directory != null : "Cannot traverse a null directory";
        System.err.println("scanning bundle directory: '"+directory+'\'');
        File dir = new File(directory);
        assert dir.exists() : "Cannot traverse a directory that does not exist";

        File[] files = dir.listFiles();
        assert files != null;
        for (File f : files) {
            DefaultMutableTreeNode current = new DefaultMutableTreeNode(f);
            if (f.isDirectory()) {
                System.err.println(f+": directory!");
                parent.add(current);
                exploreDirectory(f.getPath(), current);
            } else if (ArgumentManager.isBundle(f.getPath())) {
                System.err.println(f+": bundle!");
                parent.add(current);
                if (f.getPath().equals(getUnquotedValue())) {
                    selected = current;
                }
            } else {
                System.err.println(f+": neither! :(");
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

    private void useSelectedTreeValue(final JTree tree) {
        assert tree != null : "cannot use a null JTree";
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();

        if (node != null) {
            File f = (File) node.getUserObject();
            if (f.isDirectory()) {
                setValue(defaultValue);
            } else {
                setValue(node.toString());
            }

            TreePath nodePath = new TreePath(node.getPath());
            tree.setSelectionPath(nodePath);
            tree.scrollPathToVisible(nodePath);
        }
    }

    @Override public JPanel getComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        
        final String path = StartupManager.getInstance().getPlatform().getUserBundles();
        final DefaultMutableTreeNode root = getRootNode(path);
        if (root == null) {
            return panel;
        }
        
        final JTree tree = new JTree(root);
        tree.addTreeSelectionListener(e -> useSelectedTreeValue(tree));
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        JScrollPane scroller = new JScrollPane(tree);
        
        ToolTipManager.sharedInstance().registerComponent(tree);
        tree.setCellRenderer(new TreeCellRenderer());
        scroller.setPreferredSize(new Dimension(140, 130));
        tree.expandRow(0);
        
        if (selected != null) {
            TreePath nodePath = new TreePath(selected.getPath());
            tree.setSelectionPath(nodePath);
            tree.scrollPathToVisible(nodePath);
        }
        
        final JCheckBox enabled = new JCheckBox("Specify default bundle:", true);
        enabled.addActionListener(e -> {
            tree.setEnabled(enabled.isSelected());
            if (tree.isEnabled()) {
                useSelectedTreeValue(tree);
            } else {
                setValue(defaultValue);
            }
        });

        // this listener is what creates (and destroys) the bundle tree.
        // ancestorAdded is triggered when "tree" becomes visible, and
        // ancestorRemoved is triggered when "tree" is no longer visible.
        tree.addAncestorListener(new AncestorListener() {
            @Override public void ancestorAdded(AncestorEvent event) {
                System.err.println("tree visible! calling exploreDirectory: path='"+path+"' root='"+root+'\'');
                exploreDirectory(path, root);
                System.err.println("exploreDirectory finished");
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.reload();
                tree.revalidate();
            }

            @Override public void ancestorRemoved(AncestorEvent event) {
                System.err.println("tree hidden!");
                root.removeAllChildren();
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.reload();
            }

            @Override public void ancestorMoved(AncestorEvent event) { }
        });

        panel.add(enabled, BorderLayout.PAGE_START);
        panel.add(scroller, BorderLayout.PAGE_END);
        return panel;
    }

    @Override public String getValue() {
        return '"' + value + '"';
    }

    public String getUnquotedValue() {
        return value;
    }

    @Override public void setValue(final String newValue) {
        value = CLEAN_VALUE_REGEX.matcher(newValue).replaceAll("");
    }

    public String toString() {
        return MakeToString.fromInstance(this)
                           .add("optionId", getOptionId())
                           .add("value", getValue()).toString();
    }
}
