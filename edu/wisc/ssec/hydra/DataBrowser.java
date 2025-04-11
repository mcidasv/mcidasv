/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
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

import static edu.wisc.ssec.mcidasv.McIDASV.getStaticMcv;

import edu.wisc.ssec.adapter.HydraContext;
import edu.wisc.ssec.hydra.data.DataSource;
import edu.wisc.ssec.mcidasv.chooser.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileSystemView;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeCellRenderer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.MouseEvent;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Frame;

import java.io.File;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class DataBrowser extends HydraDisplay implements ActionListener, TreeSelectionListener, TreeExpansionListener {

    private static final Logger logger = LoggerFactory.getLogger(DataBrowser.class);
    private static final String HYDRA_LAST_PATH_ID = "mcidasv.hydra.lastpath";

    // TJJ - keep around as original version McV drop-in was based on, but do not display any more
    public static String version = "5.0.2";

    private static DataBrowser instance = null;

    JFileChooser fc;

    JFrame frame = null;

    JComponent guiPanel;

    JSplitPane splitPane;

    JComboBox windowSelect;

    JComboBox actionType;

    JList dataSourceList;

    JMenuBar menuBar;

    JCheckBoxMenuItem regionMatch = null;

    JCheckBoxMenuItem parallelCompute = null;

    JCheckBoxMenuItem replicateCombinations = null;

    Point frameLoc = null;

    DefaultMutableTreeNode root;
    DefaultTreeModel rootModel;
    JTree rootTree;

    JTree datasetTree;
    DefaultMutableTreeNode datasetsNode;
    DefaultTreeModel treeModel;

    DefaultMutableTreeNode userNode;

    JComponent geoTimeSelect;

    HashMap<DefaultMutableTreeNode, Hydra> datasetToHydra = new HashMap<>();

    HashMap<DefaultMutableTreeNode, TreePath> datasetToDefaultPath = new HashMap<>();

    HashMap<TreePath, TreePath> datasetToLastPath = new HashMap<>();

    HashMap<Integer, TreePath> datasetIDtoPath = new HashMap<>();

    HashMap<Integer, Hydra> datasetIDtoHydra = new HashMap<>();

    HashMap<Integer, DefaultMutableTreeNode> datasetIDtoDatasetNode = new HashMap<>();

    HashMap<Integer, ArrayList<DefaultMutableTreeNode>> datasetIDtoUserNode = new HashMap<>();

    HashMap<DefaultMutableTreeNode, PreviewSelection> datasetToDefaultComp = new HashMap<>();

    HashMap<DefaultMutableTreeNode, DefaultMutableTreeNode> datasetToDefaultNode = new HashMap<>();

    Hydra hydra = null;

    DefaultMutableTreeNode selectedLeafNode = null;

    DefaultMutableTreeNode selectedNode = null;

    TreePath currentDatasetPath = null;

    TreePath lastDatasetPath = null;

    FormulaSelection formulaSelection = null;

    int cnt = 0;
    boolean first = true;

    private static PreviewDisplay previewDisplay = null;

    private boolean doUpdateSpatialTemporalComp = true;
    private JMenuItem probeSettingsItem;

    public DataBrowser() {

        instance = this;
        //Create a file chooser
        fc = new JFileChooser(getDataPath(System.getProperty("user.home")));
        fc.setMultiSelectionEnabled(true);
        fc.setAcceptAllFileFilterUsed(false);

        guiPanel = buildGUI();

        buildMenuBar();

        formulaSelection = new FormulaSelection();

        String title = "HYDRA Control Window";
        frame = Hydra.createAndShowFrame(title, guiPanel, menuBar, new Dimension(568, 360));
        frame.addWindowListener(this);
        frameLoc = frame.getLocation();
    }

    public JComponent buildGUI() {

        userNode = new DefaultMutableTreeNode("Combinations");

        datasetsNode = new DefaultMutableTreeNode("Datasets");
        DefaultTreeCellRenderer render = new DefaultTreeCellRenderer();
        render.setLeafIcon(null);
        render.setClosedIcon(null);
        render.setOpenIcon(null);

        root = new DefaultMutableTreeNode("root");
        root.add(datasetsNode);
        root.add(userNode);
        rootModel = new DefaultTreeModel(root);

        rootTree = new JTree(rootModel);
        rootTree.setExpandsSelectedPaths(false);
        rootTree.setShowsRootHandles(true);
        rootTree.setRootVisible(false);
        render = new DefaultTreeCellRenderer();
        render.setLeafIcon(null);
        render.setClosedIcon(null);
        render.setOpenIcon(null);
        rootTree.setCellRenderer(render);
        rootTree.addTreeSelectionListener(this);
        rootTree.addTreeExpansionListener(this);
        rootTree.addMouseListener(this);


        JScrollPane treeScrollPane = new JScrollPane(rootTree);

        geoTimeSelect = new JPanel();
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, geoTimeSelect);
        splitPane.setDividerLocation(184);

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BorderLayout());

        outerPanel.add(splitPane, BorderLayout.CENTER);
        JComponent actionComp = makeActionComponent();
        outerPanel.add(actionComp, BorderLayout.SOUTH);


        return outerPanel;
    }

    public JTree getBrowserTree() {
        return this.rootTree;
    }

    public static PreviewDisplay getPreviewDisplay() {
        if (previewDisplay == null) {
            try {
                previewDisplay = new PreviewDisplay();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return previewDisplay;
    }

    public void addDataSetTree(final DefaultMutableTreeNode node, TreePath firstPath, Hydra hydra, final PreviewSelection comp) {
        datasetToHydra.put(node, hydra);
        Object[] nodes = firstPath.getPath();
        TreePath selectedPath = new TreePath(root);
        selectedPath = selectedPath.pathByAddingChild(datasetsNode);
        for (int k = 0; k < nodes.length; k++) {
            selectedPath = selectedPath.pathByAddingChild(nodes[k]);
        }

        final TreePath finalPath = selectedPath;
        datasetToDefaultPath.put(node, selectedPath);
        datasetToDefaultComp.put(node, comp);

        TreePath datasetPath = new TreePath(root);
        datasetPath = datasetPath.pathByAddingChild(datasetsNode);
        datasetPath = datasetPath.pathByAddingChild(node);
        datasetIDtoPath.put(hydra.getDataSource().getDataSourceId(), datasetPath);
        datasetIDtoHydra.put(hydra.getDataSource().getDataSourceId(), hydra);
        datasetIDtoDatasetNode.put(hydra.getDataSource().getDataSourceId(), node);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                rootModel.insertNodeInto(node, datasetsNode, 0);
                rootTree.setSelectionPath(finalPath);
                rootTree.scrollPathToVisible(finalPath);
                updateSpatialTemporalSelectionComponent(comp);
            }
        });
    }

    public void addUserTreeNode(final DefaultMutableTreeNode node) {

        Compute compute = (Compute) ((LeafInfo) node.getUserObject()).source;
        int numOperands = compute.numOperands;
        for (int k = 0; k < numOperands; k++) {
            Operand operand = compute.operands[k];
            if (!operand.isEmpty()) {
                int dataSourceId = operand.dataSourceId;
                if (datasetIDtoUserNode.get(dataSourceId) == null) {
                    ArrayList<DefaultMutableTreeNode> alist = new ArrayList<>();
                    alist.add(node);
                    datasetIDtoUserNode.put(dataSourceId, alist);
                } else {
                    ArrayList<DefaultMutableTreeNode> alist = datasetIDtoUserNode.get(dataSourceId);
                    if (!alist.contains(node)) {
                        alist.add(node);
                    }
                }
            }
        }

        TreePath selectedPath = new TreePath(root);
        selectedPath = selectedPath.pathByAddingChild(userNode);
        selectedPath = selectedPath.pathByAddingChild(node);

        final TreePath finalPath = selectedPath;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                rootModel.insertNodeInto(node, userNode, 0);
                rootTree.setSelectionPath(finalPath);
                rootTree.scrollPathToVisible(finalPath);
            }
        });
    }

    public void addUserTreeNodes(final DefaultMutableTreeNode[] nodes) {

        for (int n = 0; n < nodes.length; n++) {
            Compute compute = (Compute) ((LeafInfo) nodes[n].getUserObject()).source;
            int numOperands = compute.numOperands;
            for (int k = 0; k < numOperands; k++) {
                Operand operand = compute.operands[k];
                if (!operand.isEmpty()) {
                    int dataSourceId = operand.dataSourceId;
                    if (datasetIDtoUserNode.get(dataSourceId) == null) {
                        ArrayList<DefaultMutableTreeNode> alist = new ArrayList<>();
                        alist.add(nodes[n]);
                        datasetIDtoUserNode.put(dataSourceId, alist);
                    } else {
                        ArrayList<DefaultMutableTreeNode> alist = datasetIDtoUserNode.get(dataSourceId);
                        alist.add(nodes[n]);
                    }
                }
            }
        }

        TreePath selectedPath = new TreePath(root);
        selectedPath = selectedPath.pathByAddingChild(userNode);
        selectedPath = selectedPath.pathByAddingChild(nodes[0]);

        final TreePath finalPath = selectedPath;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                rootModel.insertNodeInto(nodes[0], userNode, 0);
                rootTree.setSelectionPath(finalPath);
                rootTree.scrollPathToVisible(finalPath);
                doUpdateSpatialTemporalComp = false;
                for (int i = 1; i < nodes.length; i++) {
                    rootModel.insertNodeInto(nodes[i], userNode, 0);
                }
                doUpdateSpatialTemporalComp = true;
            }
        });
    }

    public void updateSpatialTemporalSelectionComponent(PreviewSelection comp) {
        if (first) {
            try {
                previewDisplay.updateFrom(comp);
                splitPane.setRightComponent(previewDisplay.doMakeContents());
                previewDisplay.draw();
                first = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (comp != null) {
                comp.updateBoxSelector();
            }
            try {
                previewDisplay.updateFrom(comp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void buildMenuBar() {
        menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        menuBar.add(fileMenu);

        JMenuItem openFile = new JMenuItem("File(s)");
        openFile.addActionListener(this);
        openFile.setActionCommand("OpenFile");

        JMenu dirMenu = new JMenu("Directory");
        dirMenu.getPopupMenu().setLightWeightPopupEnabled(false);

        JMenuItem openDir = new JMenuItem("VIIRS");
        openDir.addActionListener(this);
        openDir.setActionCommand("OpenDirV");

        JMenuItem openDirA = new JMenuItem("AHI");
        openDirA.addActionListener(this);
        openDirA.setActionCommand("OpenDirA");

        JMenuItem openDirABI = new JMenuItem("ABI");
        openDirABI.addActionListener(this);
        openDirABI.setActionCommand("OpenDirABI");

        JMenuItem openRemote = new JMenuItem("Remote");
        openRemote.addActionListener(this);
        openRemote.setActionCommand("OpenRemote");

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(this);
        exitItem.setActionCommand("Exit");

        fileMenu.add(openFile);
        fileMenu.add(dirMenu);
        dirMenu.add(openDir);
        dirMenu.add(openDirA);
        dirMenu.add(openDirABI);
        // not yet, fileMenu.add(openRemote);
        fileMenu.add(exitItem);


        JMenu editMenu = new JMenu("Edit");
        editMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        menuBar.add(editMenu);

        JMenuItem remove = new JMenuItem("Remove Dataset");
        remove.addActionListener(this);
        remove.setActionCommand("RemoveDataset");
        editMenu.add(remove);

        remove = new JMenuItem("Remove Combination");
        remove.addActionListener(this);
        remove.setActionCommand("RemoveFormula");
        editMenu.add(remove);


        JMenuItem rename = new JMenuItem("Rename Combination");
        rename.addActionListener(this);
        rename.setActionCommand("RenameFormula");
        editMenu.add(rename);


        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        JMenuItem rgb = new JMenuItem("RGB Composite");
        rgb.addActionListener(this);
        rgb.setActionCommand("doRGB");
        toolsMenu.add(rgb);
        JMenuItem fourChannelCombine = new JMenuItem("Band Math");
        fourChannelCombine.addActionListener(this);
        fourChannelCombine.setActionCommand("doFourChannelCombine");
        toolsMenu.add(fourChannelCombine);

        menuBar.add(toolsMenu);

        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        regionMatch = new JCheckBoxMenuItem("Region Matching", true);
        regionMatch.addActionListener(this);
        regionMatch.setActionCommand("regionMatch");
        settingsMenu.add(regionMatch);

        JMenu reprojectMode = new JMenu("Reproject Mode");
        JRadioButtonMenuItem mode0 = new JRadioButtonMenuItem("Nearest", true);
        JRadioButtonMenuItem mode2 = new JRadioButtonMenuItem("Bilinear", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(mode0);
        bg.add(mode2);
        mode0.addActionListener(this);
        mode0.setActionCommand("nearest");
        mode2.addActionListener(this);
        mode2.setActionCommand("bilinear");
        reprojectMode.add(mode0);
        reprojectMode.add(mode2);

        settingsMenu.add(reprojectMode);

        parallelCompute = new JCheckBoxMenuItem("Parallel Compute", Hydra.getDoParallel());
        parallelCompute.addActionListener(this);
        parallelCompute.setActionCommand("doParallel");
        settingsMenu.add(parallelCompute);

        replicateCombinations = new JCheckBoxMenuItem("Replicate Combinations", Hydra.getReplicateCompute());
        replicateCombinations.addActionListener(this);
        replicateCombinations.setActionCommand("doReplicateCombinations");
        replicateCombinations.setToolTipText("automatically generates combinations for same instrument and platform");
        settingsMenu.add(replicateCombinations);

        probeSettingsItem = new JMenuItem("Text");
        probeSettingsItem.addActionListener(this);
        probeSettingsItem.setActionCommand("textSettings");
        settingsMenu.add(probeSettingsItem);

        menuBar.add(settingsMenu);
    }

    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) ((JTree) rootTree).getLastSelectedPathComponent();

        if (node == null) {
            hydra = null;
            return;
        }

        if (node.isLeaf()) {
            selectedLeafNode = node;
            selectedNode = (DefaultMutableTreeNode) node.getParent();
            hydra = datasetToHydra.get(node.getParent());

            TreePath tpath;
            if (!(node.getParent() == userNode)) {
                tpath = new TreePath(((DefaultMutableTreeNode) node.getParent()).getPath());
                currentDatasetPath = tpath;
                datasetToLastPath.put(currentDatasetPath, new TreePath(((DefaultMutableTreeNode) node).getPath()));
                lastDatasetPath = currentDatasetPath;
            } else { // when selected a combinations leafnode, highlight the dataset currently shown in PreviewDisplay
                LeafInfo info = (LeafInfo) node.getUserObject();
                Compute compute = (Compute) info.source;
                Operand operand = compute.operands[0];
                if (operand.dataSource != null) {
                    int id = operand.dataSourceId;
                    if (!currentDatasetPath.equals(datasetIDtoPath.get(id))) {
                        currentDatasetPath = datasetIDtoPath.get(id);
                        if (doUpdateSpatialTemporalComp) {
                            updateSpatialTemporalSelectionComponent(datasetToDefaultComp.get(datasetIDtoDatasetNode.get(id)));
                        }
                    }
                }
                tpath = currentDatasetPath;
            }

            rootTree.removeTreeSelectionListener(this);
            rootTree.addSelectionPath(tpath);
            rootTree.addTreeSelectionListener(this);

            updateDisplayAction();
        } else {
            currentDatasetPath = new TreePath(((DefaultMutableTreeNode) node).getPath());
            selectedNode = node;
            if (selectedNode.equals(datasetsNode)) {
                return;
            }

            TreePath dfltPath = datasetToDefaultPath.get(node);
            TreePath path = datasetToLastPath.get(currentDatasetPath);
            if (path == null) path = dfltPath;

            rootTree.removeTreeSelectionListener(this);
            rootTree.addSelectionPath(path);
            rootTree.addTreeSelectionListener(this);

            selectedLeafNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            hydra = datasetToHydra.get(selectedLeafNode.getParent());

            if (lastDatasetPath != null && lastDatasetPath.equals(currentDatasetPath)) {
                return;
            }
            lastDatasetPath = currentDatasetPath;

            // This must be done manually here.
            hydra.getSelection().setSelected(selectedLeafNode);

            updateDisplayAction();
        }
    }

    private void updateDisplayAction() {
        // TODO: this block of code modifies the display action gui for the multiChannelView
        // Need to think about a generalized approach.  Note: hydra will be null for combinations
        // hence the check.
        if (hydra != null) {
            if (hydra.multiDisplay) {
                windowSelect.setSelectedIndex(0);
                windowSelect.setEnabled(false);
                actionType.setEnabled(false);
            } else {
                windowSelect.setEnabled(true);
                if (windowSelect.getSelectedIndex() > 0) {
                    actionType.setEnabled(true);
                }
            }
        } else {
            if (windowSelect.getSelectedIndex() == 0) {
                if (windowSelect.getItemCount() == 1) {
                    actionType.setEnabled(false);
                } else {
                    windowSelect.setEnabled(true);
                }
            } else {
                windowSelect.setEnabled(true);
                actionType.setEnabled(true);
            }
        }
    }

    public void treeCollapsed(TreeExpansionEvent e) {
    }

    public void treeExpanded(TreeExpansionEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        int selRow = rootTree.getRowForLocation(e.getX(), e.getY());
        if (selRow != -1) {
            if (e.getClickCount() == 1) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) rootTree.getLastSelectedPathComponent();

                if (node == null) return;

                // only for Formulas, return otherwise
                if (node.getParent() != userNode) return;

                Object nodeInfo = node.getUserObject();
                if (node.isLeaf()) {
                    LeafInfo leaf = (LeafInfo) nodeInfo;
                    formulaSelection.fireSelectionEvent((Compute) leaf.source, leaf.name);
                } else {
                    NodeInfo info = (NodeInfo) nodeInfo;
                }
            } else if (e.getClickCount() == 2) {
                //pass
            }
        }
    }

    /**
     * Change the path that the file chooser is presenting to the user.
     *
     * <p>This value will be written to the user's preferences so that the user
     * can pick up where they left off after restarting McIDAS-V.</p>
     *
     * @param newPath Path to set. Should not be {@code null}.
     */
    public void setDataPath(String newPath) {
        getStaticMcv().getStateManager().writePreference(HYDRA_LAST_PATH_ID, newPath);
    }

    /**
     * Get the path the {@link JFileChooser} should be using.
     *
     * <p>If the path in the user's preferences is {@code null}
     * (or does not exist), {@code defaultValue} will be returned.</p>
     *
     * <p>If there is a nonexistent path in the preferences file,
     * {@link FileChooser#findValidParent(String)} will be used.</p>
     *
     * @param defaultValue Default path to use if there is a {@literal "bad"}
     *                     path in the user's preferences.
     *                     Cannot be {@code null}.
     * @return Path to use for the chooser.
     * @throws NullPointerException if {@code defaultValue} is {@code null}.
     */
    public String getDataPath(final String defaultValue) {
        Objects.requireNonNull(defaultValue,
                "Default value may not be null");
        String tempPath =
                (String) getStaticMcv().getPreference(HYDRA_LAST_PATH_ID);
        try {
            if ((tempPath == null)) {
                tempPath = defaultValue;
            } else if (!Files.exists(Paths.get(tempPath))) {
                tempPath = FileChooser.findValidParent(tempPath);
            }
        } catch (Exception e) {
            logger.warn("Could not find valid parent directory for '" + tempPath + "', using '" + defaultValue + '\'');
            tempPath = defaultValue;
        }
        return tempPath;
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (e.getActionCommand().equals("OpenFile")) {
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = fc.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File[] files = fc.getSelectedFiles();
                filesSelected(files);
                setDataPath(files[0].getPath());
            }
        } else if (cmd.startsWith("OpenDir")) {
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                directorySelected(file);
                setDataPath(file.getPath());
            }
        } else if (cmd.equals("RemoveDataset")) {
            if (selectedNode == datasetsNode) {
                return;
            } else if (selectedNode == null) {
                return;
            }

            DefaultMutableTreeNode datasetNode = null;
            if (lastDatasetPath != null) {
                datasetNode = (DefaultMutableTreeNode) lastDatasetPath.getPath()[2];
            }
            if (datasetNode == null) {
                return;
            }

            DataSource dataSource = datasetToHydra.get(datasetNode).getDataSource();
            Hydra.removeDataSource(dataSource);
            HydraContext.removeContext(dataSource);
            ((BasicSelection) datasetToHydra.get(datasetNode).getSelection()).remove();
            datasetToHydra.remove(datasetNode);
            datasetIDtoHydra.remove(dataSource.getDataSourceId());
            Hydra.dataSourceMap.remove(dataSource.getDataSourceId());
            Hydra.dataSourceIdToSelector.remove(dataSource.getDataSourceId());
            datasetToDefaultPath.remove(datasetNode);
            datasetToDefaultComp.remove(datasetNode);
            datasetToDefaultNode.remove(datasetNode);
            datasetIDtoDatasetNode.remove(dataSource.getDataSourceId());
            datasetIDtoPath.remove(dataSource.getDataSourceId());
            datasetToLastPath.remove(lastDatasetPath);
            rootModel.removeNodeFromParent(datasetNode);

            removeFormulas(dataSource.getDataSourceId());

            int cnt = rootModel.getChildCount(datasetsNode);
            if (cnt >= 1) { // At least one DataSource left
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) datasetsNode.getChildAt(0);
                NodeInfo nInfo = (NodeInfo) node.getUserObject();
                TreePath path = (TreePath) ((SelectionAdapter) nInfo.source).getLastSelectedLeafPath();
                if (path == null) {
                    path = datasetToDefaultPath.get(node);
                }
                rootTree.setSelectionPath(path);
                rootTree.scrollPathToVisible(path);
                PreviewSelection comp = (PreviewSelection) ((SelectionAdapter) nInfo.source).getLastSelectedComp();
                updateSpatialTemporalSelectionComponent(comp);
            } else { // All Datasources have been removed.
                hydra = null;
                selectedNode = null;
                selectedLeafNode = null;
                currentDatasetPath = null;
                lastDatasetPath = null;
                HydraContext.setLastManual(null);

                updateSpatialTemporalSelectionComponent(null);
            }
        } else if (cmd.equals("RemoveFormula")) {
            if (selectedLeafNode == userNode) {
                return;
            }
            removeFormula(selectedLeafNode);

        } else if (cmd.equals("RenameFormula")) {
            if (selectedLeafNode == userNode) {
                return;
            }

            Compute cmp = ((Compute) ((LeafInfo) selectedLeafNode.getUserObject()).source);

            JDialog dialog = new JDialog((Frame) null, "Rename Combination", true);
            dialog.setLayout(new FlowLayout());

            JTextField textField = new JTextField(20);
            JButton button = new JButton("OK");
            final String[] result = new String[1];

            button.addActionListener(exp -> {
                result[0] = textField.getText();
                dialog.dispose();
            });

            dialog.add(textField);
            dialog.add(button);
            dialog.setSize(300, 100);
            dialog.setLocationRelativeTo(null);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);

            if (!result[0].isBlank()) {
                LeafInfo info = new LeafInfo(cmp, result[0], 0);
                selectedLeafNode.setUserObject(info);
            }

        } else if (cmd.equals("OpenRemote")) {
        } else if (cmd.equals("Exit")) {
            closeFrame();
        } else if (cmd.equals("doRGB")) {
            (new RGBComposite(this)).show(frameLoc.x + 600, frameLoc.y);
        } else if (cmd.equals("doFourChannelCombine")) {
            (new FourChannelCombine(this)).show(frameLoc.x + 600, frameLoc.y);
        } else if (cmd.equals("regionMatch")) {
            if (regionMatch.getState()) {
                Hydra.setRegionMatching(true);
            } else {
                Hydra.setRegionMatching(false);
            }
        } else if (cmd.equals("nearest")) {
            Hydra.setReprojectMode(0);
        } else if (cmd.equals("bilinear")) {
            Hydra.setReprojectMode(2);
        } else if (cmd.equals("doParallel")) {
            if (parallelCompute.getState()) {
                Hydra.setDoParallel(true);
            } else {
                Hydra.setDoParallel(false);
            }
        } else if (cmd.equals("doReplicateCombinations")) {
            if (replicateCombinations.getState()) {
                Hydra.setReplicateCompute(true);
            } else {
                Hydra.setReplicateCompute(false);
            }
        } else if (cmd.equals("textSettings")) {
            new TextSettings(frame);
        }
    }

    private void removeFormula(DefaultMutableTreeNode leafNode) {
        Iterator iter = datasetIDtoUserNode.keySet().iterator();
        Object key = null;
        while (iter.hasNext()) {
            Object obj = iter.next();
            Object val = datasetIDtoUserNode.get(obj);
            if (val.equals(leafNode)) {
                key = obj;
                break;
            }
        }
        if (key != null) {
            datasetIDtoDatasetNode.remove(key);
        }

        rootTree.removeTreeSelectionListener(this);
        if (leafNode.getParent() != null) {
            rootModel.removeNodeFromParent(leafNode);
        }
        Compute.removeCompute((Compute) ((LeafInfo) leafNode.getUserObject()).source);
        cnt = rootModel.getChildCount(userNode);
        if (cnt > 0) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) userNode.getChildAt(0);
            this.selectedLeafNode = node;
            TreePath tpath = new TreePath(node.getPath());
            rootTree.setSelectionPath(tpath);
        }
        rootTree.addSelectionPath(currentDatasetPath);
        rootTree.addTreeSelectionListener(this);
    }

    private void removeFormulas(int dataSourceId) {
        ArrayList<DefaultMutableTreeNode> alist = datasetIDtoUserNode.get(dataSourceId);
        if (alist != null) {
            Iterator<DefaultMutableTreeNode> iter = alist.iterator();
            while (iter.hasNext()) {
                DefaultMutableTreeNode userNode = iter.next();
                removeFormula(userNode);
            }
            alist.clear();
        }
    }

    public void filesSelected(File[] files) {
        setCursorToWait();
        Hydra hydra = new Hydra(this);

        class Task extends SwingWorker<String, Object> {
            Hydra hydra;
            File[] files;

            public Task(File[] files, Hydra hydra) {
                this.files = files;
                this.hydra = hydra;
            }

            public String doInBackground() {
                this.hydra.dataSourceSelected(files);
                return "done";
            }

            protected void done() {
                setCursorToDefault();
            }
        }

        (new Task(files, hydra)).execute();
    }

    public void directorySelected(File dir) {
        setCursorToWait();
        Hydra hydra = new Hydra(this);

        class Task extends SwingWorker<String, Object> {
            File dir;
            Hydra hydra;

            public Task(File dir, Hydra hydra) {
                this.dir = dir;
                this.hydra = hydra;
            }

            public String doInBackground() {
                this.hydra.dataSourceSelected(dir);
                return "done";
            }

            protected void done() {
                setCursorToDefault();
            }
        }

        (new Task(dir, hydra)).execute();
    }

    public void windowClosing(WindowEvent e) {
        closeFrame();
    }

    public Point getLocation() {
        return frame.getLocation();
    }

    public static DataBrowser getInstance() {
        return instance;
    }

    public void windowRemoved(int windowNumber) {
        WindowItem item = null;
        int cnt = windowSelect.getItemCount() - 1;
        for (int k = 0; k < cnt; k++) {
            item = (WindowItem) windowSelect.getItemAt(k + 1);
            if (item.windowNumber == windowNumber) {
                windowSelect.removeItem(item);
                break;
            }
        }
        // reset to position 1 so that New is not the final selection if windows remain
        if (windowSelect.getSelectedIndex() == 0 && windowSelect.getItemCount() > 1) {
            windowSelect.setSelectedIndex(1);
        }
    }

    public JComponent makeActionComponent() {

        JPanel actionPanel = new JPanel(new FlowLayout());

        actionType = new JComboBox(new String[]{"Replace", "Overlay"});
        actionType.setEnabled(false);

        windowSelect = new JComboBox(new String[]{"New"});
        windowSelect.setSelectedIndex(0);
        windowSelect.setEnabled(false);
        windowSelect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (windowSelect.getSelectedItem().equals("New")) {
                    actionType.setSelectedIndex(0);
                    actionType.setEnabled(false);
                } else {
                    ArrayList imgDspList = ImageDisplay.getImageDisplayList();
                    for (int k = 0; k < imgDspList.size(); k++) {
                        ImageDisplay imgDsp = (ImageDisplay) imgDspList.get(k);
                        imgDsp.setIsTarget(false);
                    }
                    int targetIndex = windowSelect.getSelectedIndex();
                    WindowItem item = (WindowItem) windowSelect.getSelectedItem();
                    for (int k = 0; k < imgDspList.size(); k++) {
                        ImageDisplay imgDsp = (ImageDisplay) imgDspList.get(k);
                        if (item.windowNumber == imgDsp.getWindowNumber()) {
                            imgDsp.setIsTarget(true);
                            imgDsp.toFront();
                            if (imgDsp.onlyOverlayNoReplace) {
                                actionType.setSelectedIndex(1);
                                actionType.setEnabled(false);
                            } else {
                                actionType.setEnabled(true);
                            }
                            break;
                        }
                    }
                }
            }
        });

        JButton displayButton = new JButton("Display");
        displayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setCursorToWait();

                class Task extends SwingWorker<String, Object> {
                    int mode = 0;
                    int windowNumber = 1;
                    boolean imageCreated = false;

                    public String doInBackground() {
                        if (windowSelect.getSelectedItem().equals("New")) {
                            mode = 0;
                            windowNumber = 1;
                            int cnt = windowSelect.getItemCount() - 1;
                            if (cnt == 0) {
                                windowNumber = 1;
                            }
                            for (int k = 0; k < cnt; k++) {
                                WindowItem item = (WindowItem) windowSelect.getItemAt(k + 1);
                                if ((item.windowNumber > 1) && (k == 0)) {
                                    windowNumber = 1;
                                    break;
                                }
                                WindowItem itemNext = (WindowItem) windowSelect.getItemAt(k + 2);
                                windowNumber = item.windowNumber + 1;
                                if (itemNext != null) {
                                    if ((itemNext.windowNumber - item.windowNumber) > 1) {
                                        break;
                                    }
                                }
                            }
                        } else {
                            String action = (String) actionType.getSelectedItem();
                            if (action.equals("Replace")) {
                                mode = 1;
                            } else if (action.equals("Overlay")) {
                                mode = 2;
                            }

                            if (hydra != null && hydra.multiDisplay) {
                                int num = windowSelect.getItemCount();
                                WindowItem item = (WindowItem) windowSelect.getItemAt(num - 1);
                                windowNumber = item.windowNumber + 1;
                            }
                        }

                        // check if a selected leaf node references an instance of Compute
                        if (selectedLeafNode != null) {
                            LeafInfo leafInfo = (LeafInfo) selectedLeafNode.getUserObject();
                            Object source = leafInfo.source;
                            if (source instanceof Compute) {
                                try {
                                    Compute compute = (Compute) source;
                                    compute.createDisplay(compute.compute(), mode, windowNumber);
                                    imageCreated = true;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                imageCreated = hydra.createImageDisplay(mode, windowNumber);
                            }
                        }

                        return "done";
                    }

                    protected void done() {
                        setCursorToDefault();

                        if (!imageCreated) {
                            return;
                        }

                        if ((mode == 0) || (hydra != null && hydra.multiDisplay)) {
                            if (hydra != null && !hydra.multiDisplay) {
                                if (!windowSelect.isEnabled()) {
                                    windowSelect.setEnabled(true);
                                    actionType.setEnabled(true);
                                }
                            } else if (hydra == null) {
                                windowSelect.setEnabled(true);
                                actionType.setEnabled(true);
                            }
                            WindowItem item = new WindowItem();
                            item.windowNumber = windowNumber;
                            windowSelect.insertItemAt(item, windowNumber);
                            windowSelect.setSelectedItem(item);
                        }
                    }
                }

                (new Task()).execute();
            }

        });

        actionPanel.add(displayButton);
        actionPanel.add(windowSelect);
        actionPanel.add(actionType);

        return actionPanel;
    }

    public void setCursorToWait() {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void setCursorToDefault() {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void closeFrame() {
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(false);
            frame.dispose();
        });
    }

    public static void main(String[] args) throws Exception {

        FileSystemView fsv = FileSystemView.getFileSystemView();
        File homeDir = fsv.getHomeDirectory();

        // try {
        //    PrintStream prntStrm = new PrintStream(new FileOutputStream(new File(homeDir, "hydraout.txt"), false));
        //    System.setOut(prntStrm);
        //    System.setErr(prntStrm);
        // }
        // catch (Exception e) { // Just in case we can't open the log file.
        //    e.printStackTrace();
        //    System.out.println("Could not open hydraout.txt in: "+homeDir);
        // }

        previewDisplay = getPreviewDisplay();
        DataBrowser dataBrowser = new DataBrowser();

        dataBrowser.setCursorToWait();
        Hydra.initializeMapBoundaries();
        Hydra.initializeColorTables();
        previewDisplay.init();
        dataBrowser.setCursorToDefault();
        System.out.println("Found " + Runtime.getRuntime().availableProcessors() + " cpu processing cores");
    }

    class WindowItem {
        int windowNumber;

        public String toString() {
            return "Window #" + windowNumber;
        }
    }

}

class TextSettings {
    public TextSettings(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Text Settings");
        dialog.setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridLayout(2, 1));
        final JTextField txtFld = new JTextField(5);
        final JTextField fontFld = new JTextField(5);
        txtFld.setText(Float.toString(Hydra.getTextSizeFactor()));
        fontFld.setText(Integer.toString(Hydra.getFontSize()));

        JPanel panelA = new JPanel(new FlowLayout());
        panelA.add(new Label("Text Scale Factor"));
        panelA.add(txtFld);

        JPanel panelB = new JPanel(new FlowLayout());
        panelB.add(new Label("Font Size"));
        panelB.add(fontFld);

        panel.add(panelA);
        panel.add(panelB);

        dialog.setContentPane(panel);
        dialog.validate();
        dialog.setVisible(true);
        dialog.setSize(dialog.getPreferredSize());

        txtFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                float fac = Float.valueOf(txtFld.getText());
                Hydra.setTextSizeFactor(fac);
            }
        });

        fontFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int size = Integer.valueOf(fontFld.getText());
                Hydra.setFontSize(size);
            }
        });
    }

}
