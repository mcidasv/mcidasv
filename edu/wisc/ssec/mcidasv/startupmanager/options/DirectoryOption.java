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

package edu.wisc.ssec.mcidasv.startupmanager.options;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
    
    private final String defaultValue;
    
    public DirectoryOption(final String id, final String label, final String defaultValue, final OptionPlatform optionPlatform, final Visibility optionVisibility) {
        super(id, label, Type.DIRTREE, optionPlatform, optionVisibility);
        this.defaultValue = defaultValue;
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
                if (f.getPath().equals(getUnquotedValue()))
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
    
    private void useSelectedTreeValue(final JTree tree) {
        assert tree != null : "cannot use a null JTree";
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
        
        File f = (File)node.getUserObject();
        if (f.isDirectory()) {
            setValue(defaultValue);
        } else {
            setValue(node.toString());
        }
        
        TreePath nodePath = new TreePath(node.getPath());
        tree.setSelectionPath(nodePath);
        tree.scrollPathToVisible(nodePath);
    }
    
    public JPanel getComponent() {
        
        JPanel panel = new JPanel(new BorderLayout());
        
        String path = StartupManager.getInstance().getPlatform().getUserBundles();
        DefaultMutableTreeNode root = getRootNode(path);
        if (root == null) {
            return panel;
        }
        
        final JTree tree = new JTree(root);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(final TreeSelectionEvent e) {
                useSelectedTreeValue(tree);
            }
        });
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        JScrollPane scroller = new JScrollPane(tree);
        exploreDirectory(path, root);
        
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
        enabled.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                tree.setEnabled(enabled.isSelected());
                if (!tree.isEnabled()) {
                    setValue(defaultValue);
                } else {
                    useSelectedTreeValue(tree);
                }
            }
        });
        
        panel.add(enabled, BorderLayout.PAGE_START);
        panel.add(scroller, BorderLayout.PAGE_END);
        return panel;
    }
    
    public String getValue() {
        return "\"" + value + "\"";
    }
    
    public String getUnquotedValue() {
        return value;
    }
    
    public void setValue(final String newValue) {
        value = newValue.replaceAll("\"", "");
    }
    
    public String toString() {
        return String.format("[DirectoryOption@%x: optionId=%s, value=%s]", hashCode(), getOptionId(), getValue());
    }
}
