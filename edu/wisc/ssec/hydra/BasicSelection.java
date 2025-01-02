/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.hydra;

import edu.wisc.ssec.hydra.data.DataSource;
import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataSelection;

import java.util.List;
import java.awt.event.*;
import javax.swing.*;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import visad.FlatField;

public class BasicSelection extends SelectionAdapter {
    private JLabel picture;
    public DefaultMutableTreeNode root;
    private JComponent list;

    private JTree browserTree;

    List dataChoices = null;
    String[] dataChoiceNames = null;
    String[] dataChoiceDescs = null;

    PreviewDisplay previewDisplay;

    JFrame frame = null;

    boolean initDone = false;

    DataSource dataSource = null;

    PreviewSelection[] previewSelects = null;

    int selectedIdx = 0;
    int defaultIdx = 0;

    DataBrowser dataBrowser;
    TreePath firstPath;

    private int dataSourceId;

    private String sourceDescription;

    TreePath lastSelectedLeafPath = null;

    private MouseListener ml = null;


    public BasicSelection(DataSource dataSource, final Hydra hydra, int dataSourceId) {
        super(dataSource);

        this.dataSource = dataSource;
        this.dataSourceId = dataSourceId;

        dataBrowser = hydra.getDataBrowser();
        sourceDescription = hydra.getSourceDescription();

        dataChoices = dataSource.getDataChoices();
        dataChoiceNames = new String[dataChoices.size()];
        dataChoiceDescs = new String[dataChoices.size()];
        previewSelects = new PreviewSelection[dataChoices.size()];
        for (int k = 0; k < dataChoiceNames.length; k++) {
            dataChoiceNames[k] = ((DataChoice) dataChoices.get(k)).getName();
            dataChoiceDescs[k] = dataSource.getDescription((DataChoice) dataChoices.get(k));
        }
        selectedIdx = dataSource.getDefaultChoice();
        defaultIdx = selectedIdx;

        browserTree = dataBrowser.getBrowserTree();
        buildTreeSelectionComponent(hydra.toString());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                makeGeoTimeSelect(getSelectedDataChoice(), selectedIdx);
                previewDisplay = DataBrowser.getPreviewDisplay();
                dataBrowser.addDataSetTree(root, firstPath, hydra, previewSelects[defaultIdx]);
            }
        });


        final JTree tree = browserTree;
        final Object thisObj = this;
        ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                if (selRow > 0) {
                    if (e.getClickCount() == 1) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                        if (node == null) return;
                        setSelected(node);
                    } else if (e.getClickCount() == 2) {
                        //pass
                    }
                }
            }
        };
        tree.addMouseListener(ml);
    }

    public void remove() {
        super.remove();
        browserTree.removeMouseListener(ml);
    }

    public void setSelected(Object obj) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            LeafInfo leaf = (LeafInfo) nodeInfo;
            if (leaf.source == this) {
                selectedIdx = leaf.index;
                lastSelectedLeafPath = new TreePath(node.getPath());
                updateSelectionDisplay(selectedIdx);
            }
        }
    }

    public void buildTreeSelectionComponent(String topName) {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(new NodeInfo(this, topName));
        root = top;

        for (int k = 0; k < dataChoiceNames.length; k++) {
            DefaultMutableTreeNode leafNode =
                    new DefaultMutableTreeNode(new LeafInfo(this, dataChoiceNames[k], dataChoiceDescs[k], k));
            top.add(leafNode);
            if (k == selectedIdx) {
                firstPath = new TreePath(new Object[]{top, leafNode});
            }
        }
    }

    protected void updateSelectionDisplay(int idx) {
        if (previewSelects[idx] == null) {
            dataBrowser.setCursorToWait();
            makeGeoTimeSelect((DataChoice) dataChoices.get(idx), idx);
            dataBrowser.updateSpatialTemporalSelectionComponent(previewSelects[idx]);
            dataBrowser.setCursorToDefault();
            fireSelectionEvent();
        } else {
            try {
                dataBrowser.updateSpatialTemporalSelectionComponent(previewSelects[idx]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            fireSelectionEvent();
        }
    }

    void makeGeoTimeSelect(DataChoice choice, int idx) {
        try {
            FlatField image = PreviewSelection.makePreviewImage(dataSource, choice, sourceDescription);
            PreviewSelection preview = new PreviewSelection(choice, image, null);
            previewSelects[idx] = preview;
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public int applyToDataSelection(DataChoice dataChoice, DataSelection dataSelection) {
        int index = -1;
        for (int k = 0; k < dataChoices.size(); k++) {
            if (((DataChoice) dataChoices.get(k)) == dataChoice) {
                index = k;
            }
        }
        if (index != -1) {
            if (previewSelects[index] == null) { // We haven't created one yet, go ahead and do so now.
                makeGeoTimeSelect(dataChoice, index);
            }
            previewSelects[index].applyToDataSelection(dataSelection);
        }
        return index;
    }

    public void applyToDataSelection(DataSelection dataSelection) {
        previewSelects[selectedIdx].applyToDataSelection(dataSelection);
    }

    public DataChoice getSelectedDataChoice() {
        return (DataChoice) dataChoices.get(selectedIdx);
    }

    public String getSelectedName() {
        String str = dataSourceId + ":" + dataChoiceNames[selectedIdx];
        return str;
    }

    public Object getLastSelectedLeafPath() {
        return lastSelectedLeafPath;
    }

    public Object getLastSelectedComp() {
        return previewSelects[selectedIdx];
    }
}
