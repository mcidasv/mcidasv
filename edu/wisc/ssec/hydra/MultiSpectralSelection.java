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

import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataSelection;


import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.SpectrumAdapter;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.hydra.data.DataSource;
import edu.wisc.ssec.hydra.data.MultiSpectralDataSource;


import java.util.List;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import visad.FlatField;

public class MultiSpectralSelection extends SelectionAdapter {

    private JComponent list;
    private JComponent outerPanel;

    List dataChoices = null;
    String[] dataChoiceNames = null;

    JComponent geoTimeSelect = null;

    boolean initDone = false;

    DataSource dataSource = null;

    JComponent[] geoTimeSelectComps = null;

    PreviewSelection[] previewSelects = null;
    PreviewSelection preview = null;

    int selectedIdx = 0;

    float wavenumber = 0;
    int channelIndex = 0;
    String bandName;
    TreePath firstPath;
    DefaultMutableTreeNode root;

    DataBrowser dataBrowser;

    private int dataSourceId;

    TreePath lastSelectedLeafPath = null;

    HashMap[] bandMapArray;
    MultiSpectralData[] msdArray;

    public MultiSpectralSelection(DataSource dataSource, Hydra hydra, int dataSourceId) {
        super(dataSource);

        this.dataSource = dataSource;
        this.dataSourceId = dataSourceId;

        dataChoices = dataSource.getDataChoices();
        dataChoiceNames = new String[dataChoices.size()];
        geoTimeSelectComps = new JComponent[dataChoices.size()];
        previewSelects = new PreviewSelection[dataChoices.size()];
        for (int k = 0; k < dataChoiceNames.length; k++) {
            dataChoiceNames[k] = ((DataChoice) dataChoices.get(k)).getName();
        }

        geoTimeSelect = makeGeoTimeSelect(getSelectedDataChoice(), selectedIdx);
        geoTimeSelectComps[selectedIdx] = geoTimeSelect;

        dataBrowser = hydra.getDataBrowser();
        list = buildTreeSelectionComponent(dataBrowser.getBrowserTree(), hydra.toString());

        dataBrowser.addDataSetTree(root, firstPath, hydra, previewSelects[selectedIdx]);
    }

    public JComponent buildTreeSelectionComponent(final JTree tree, String rootName) {
        int cnt = 0;
        for (int k = 0; k < dataChoices.size(); k++) {
            if (((MultiSpectralDataSource) dataSource).getMultiSpectralData(k) != null) cnt++;
        }
        msdArray = new MultiSpectralData[cnt];
        bandMapArray = new HashMap[msdArray.length];
        for (int k = 0; k < msdArray.length; k++) {
            msdArray[k] = ((MultiSpectralDataSource) dataSource).getMultiSpectralData(k);
            bandMapArray[k] = msdArray[k].getBandNameMap();
        }

        Object[] allBands = null;
        float[] cntrWvln = null;
        if (msdArray.length == 2) { // combine emissive and reflective bands
            Object[] emisBands = msdArray[0].getBandNames().toArray();
            Object[] reflBands = msdArray[1].getBandNames().toArray();
            allBands = new Object[reflBands.length + emisBands.length];
            System.arraycopy(reflBands, 0, allBands, 0, reflBands.length);
            System.arraycopy(emisBands, 0, allBands, reflBands.length, emisBands.length);

            cntrWvln = new float[allBands.length];
            HashMap<String, Float> reflMap = msdArray[1].getBandNameMap();
            for (int k = 0; k < reflBands.length; k++) {
                float val = reflMap.get((String) reflBands[k]).floatValue();
                cntrWvln[k] = val;
            }
            HashMap<String, Float> emisMap = msdArray[0].getBandNameMap();
            for (int k = 0; k < emisBands.length; k++) {
                float val = emisMap.get((String) emisBands[k]).floatValue();
                cntrWvln[reflBands.length + k] = val;
            }
        } else {
            allBands = msdArray[0].getBandNames().toArray();
            cntrWvln = new float[allBands.length];
            HashMap<String, Float> bandMap = msdArray[0].getBandNameMap();
            for (int k = 0; k < allBands.length; k++) {
                float val = bandMap.get((String) allBands[k]).floatValue();
                cntrWvln[k] = val;
            }
        }

        String dfltBandName = msdArray[0].init_bandName;
        bandName = dfltBandName;

        //TODO: combine this with same code in mousePressed
        HashMap<String, Float> bandMap = null;
        try {
            for (int k = 0; k < bandMapArray.length; k++) {
                if (bandMapArray[k].containsKey(bandName)) {
                    bandMap = bandMapArray[k];
                    setWaveNumber(bandMap.get(bandName), bandName);
                    setChannelIndex(msdArray[k].getChannelIndexFromWavenumber(bandMap.get(bandName)));
                    setDataChoice(k);
                }
            }
        } catch (Exception exc) {
            System.out.println(exc);
        }


        root = new DefaultMutableTreeNode(new NodeInfo(this, rootName));

        for (int k = 0; k < allBands.length; k++) {
            String bandName = (String) allBands[k];
            DefaultMutableTreeNode leafNode =
                    new DefaultMutableTreeNode(new LeafInfo(this, bandName, "(" + Float.toString(cntrWvln[k]) + ")", k));
            root.add(leafNode);
            if (bandName.equals(dfltBandName)) {
                firstPath = new TreePath(new Object[]{root, leafNode});
            }
        }

        final Object thisObj = this;
        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1) {
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

        return tree;
    }

    public void setSelected(Object obj) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
        String bandName = null;
        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            LeafInfo leaf = (LeafInfo) nodeInfo;
            if (leaf.source == this) {
                lastSelectedLeafPath = new TreePath(node.getPath());
                bandName = leaf.name;
                dataBrowser.updateSpatialTemporalSelectionComponent(previewSelects[0]);
            }
        } else {
            NodeInfo info = (NodeInfo) node.getUserObject();
            if (info.source == this) {
                dataBrowser.updateSpatialTemporalSelectionComponent(previewSelects[0]);
                previewSelects[0].updateBoxSelector();
            }
        }

        if (bandName == null) return;

        HashMap<String, Float> bandMap = null;
        try {
            for (int k = 0; k < bandMapArray.length; k++) {
                if (bandMapArray[k].containsKey(bandName)) {
                    bandMap = bandMapArray[k];
                    setWaveNumber(bandMap.get(bandName), bandName);
                    setChannelIndex(msdArray[k].getChannelIndexFromWavenumber(bandMap.get(bandName)));
                    setDataChoice(k);
                    fireSelectionEvent();
                }
            }
        } catch (Exception exc) {
            System.out.println(exc);
        }

        previewSelects[0].updateBoxSelector();
    }

    public Object getLastSelectedLeafPath() {
        return lastSelectedLeafPath;
    }

    public Object getLastSelectedComp() {
        return previewSelects[0];
    }

    public JComponent getComponent() {
        return outerPanel;
    }

    JComponent makeGeoTimeSelect(DataChoice choice, int idx) {
        //- create preview image
        FlatField image = null;
        try {
            image = (FlatField) dataSource.getData(choice, null);
        } catch (Exception e) {
            System.out.println(e);
        }

        JComponent geoTimeSelect = null;
        try {
            PreviewSelection preview = new PreviewSelection(choice, image, null);
            previewSelects[idx] = preview;
        } catch (Exception e) {
            System.out.println(e);
        }

        return geoTimeSelect;
    }

    void setChannelIndex(int idx) {
        channelIndex = idx;
    }

    void setWaveNumber(float wavenumber, String bandName) {
        this.wavenumber = wavenumber;
        this.bandName = bandName;
    }

    void setDataChoice(int idx) {
        selectedIdx = idx;
        DataChoice choice = (DataChoice) dataChoices.get(idx);
    }

    public int applyToDataSelection(DataChoice choice, DataSelection dataSelection) {
        previewSelects[0].applyToDataSelection(dataSelection);
        return 0;
    }

    public void applyToDataSelection(DataSelection dataSelection) {
        previewSelects[0].applyToDataSelection(dataSelection);

        HashMap subset = ((MultiDimensionSubset) dataSelection).getSubset();

        double[] coords = new double[]{(double) channelIndex, (double) channelIndex, (double) 1};

        ((MultiDimensionSubset) dataSelection).setCoords(SpectrumAdapter.channelIndex_name, coords);
    }

    public DataChoice getSelectedDataChoice() {
        return (DataChoice) dataChoices.get(selectedIdx);
    }

    public void setDataSourceId(int dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public String getSelectedName() {
        return dataSourceId + ":" + bandName;
    }
}
