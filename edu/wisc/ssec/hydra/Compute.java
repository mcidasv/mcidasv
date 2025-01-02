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

import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.hydra.data.DataSelection;
import edu.wisc.ssec.hydra.data.DataSource;
import edu.wisc.ssec.hydra.data.DataSourceFactory;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;

import visad.Data;


public abstract class Compute implements SelectionListener {

    int activeIndex;
    Operand[] operands;
    int numOperands;
    String[] operators;
    int numOperators;

    String title;
    DataBrowser dataBrowser;

    private static ArrayList<Compute> listOfComputes = new ArrayList();

    public Compute(int numOperands, int numOperators, String title) {
        this.numOperands = numOperands;
        this.numOperators = numOperators;
        this.title = title;

        operands = new Operand[numOperands];
        for (int k = 0; k < numOperands; k++) {
            operands[k] = new Operand();
        }

        if (numOperators > 0) {
            operators = new String[numOperators];
            for (int k = 0; k < numOperators; k++) {
                operators[k] = new String();
            }
        }
    }

    public Compute(int numOperands, String title) {
        this(numOperands, 0, title);
    }

    public Compute() {
    }

    public abstract JComponent buildGUI();

    public abstract Data compute() throws Exception;

    public abstract void createDisplay(Data data, int mode, int windowNumber) throws Exception;

    public abstract String getOperationName();

    public abstract void updateUI(SelectionEvent e);

    public void updateOperandComp(int idx, Object obj) {
    }

    public JComponent makeActionComponent() {
        JButton create = new JButton("Create");
        class MyListener implements ActionListener {
            Compute compute;

            public MyListener(Compute compute) {
                this.compute = compute;
            }

            public void actionPerformed(ActionEvent e) {
                try {
                    Compute clonedCompute = compute.clone();
                    if (Hydra.getReplicateCompute()) {
                        replicateCompute(clonedCompute, dataBrowser);
                    } else {
                        listOfComputes.add(clonedCompute);
                        LeafInfo info = new LeafInfo(clonedCompute, getOperationName(), 0);
                        DefaultMutableTreeNode node = new DefaultMutableTreeNode(info);
                        dataBrowser.addUserTreeNode(node);
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }
        ;
        create.addActionListener(new MyListener(this));

        JPanel panel = new JPanel();
        panel.add(create);
        return panel;
    }

    public void show(int x, int y, String title) {
        JComponent gui = buildGUI();
        gui.add(makeActionComponent());
        SelectionAdapter.addSelectionListenerToAll(this);

        JFrame frame = Hydra.createAndShowFrame(title, gui);
        frame.setLocation(x, y);
        final Compute compute = this;
        frame.addWindowListener(new WindowAdapter() {
                                    public void windowClosing(WindowEvent e) {
                                        SelectionAdapter.removeSelectionListenerFromAll(compute);
                                    }
                                }
        );

    }

    public void show(int x, int y) {
        show(x, y, title);
    }

    public void selectionPerformed(SelectionEvent e) {
        Operand operand = operands[activeIndex];

        if (!e.fromCompute) {
            operand.dataSource = e.getDataSource();
            operand.dataSourceId = operand.dataSource.getDataSourceId();
            operand.selection = e.getSelection();
            operand.dataChoice = e.getSelection().getSelectedDataChoice();

            DataSelection dataSelection = new MultiDimensionSubset();
            operand.selection.applyToDataSelection(dataSelection);
            operand.dataSelection = dataSelection;
            operand.compute = null;
        } else {
            operand = new Operand();
            operand.dataSource = e.compute.operands[0].dataSource;
            operand.dataSourceId = e.compute.operands[0].dataSourceId;
            operand.selection = e.compute.operands[0].selection;
            operand.dataChoice = e.compute.operands[0].dataChoice;
            operand.compute = e.compute;
            operands[activeIndex] = operand;
        }

        operand.name = e.getName();
        operand.isEmpty = false;

        updateUI(e);
    }

    public void setActive(int idx) {
        activeIndex = idx;
    }

    public Compute copy(Compute compute) {
        compute.numOperands = numOperands;
        compute.operands = new Operand[numOperands];
        for (int k = 0; k < numOperands; k++) {
            compute.operands[k] = operands[k].clone();
        }

        compute.numOperators = numOperators;
        compute.operators = new String[numOperators];
        for (int k = 0; k < numOperators; k++) {
            compute.operators[k] = new String(operators[k]);
        }

        return compute;
    }

    public Compute clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Can't clone abstract Compute");
    }

    public Compute cloneForDataSource(DataSource dataSource, Selection selection) throws CloneNotSupportedException {
        Compute aclone = clone();

        for (int k = 0; k < numOperators; k++) {
            aclone.operators[k] = operators[k];
        }

        for (int k = 0; k < numOperands; k++) {
            Operand operand = operands[k];
            if (!operand.isEmpty) {
                aclone.operands[k] = operand.clone();
                if (operand.dataSource.getDescription().equals(dataSource.getDescription())) {
                    aclone.operands[k].dataChoice = dataSource.getDataChoiceByName(operand.dataChoice.getName());
                    aclone.operands[k].dataSource = dataSource;
                    aclone.operands[k].dataSourceId = dataSource.getDataSourceId();
                    aclone.operands[k].name = dataSource.getDataSourceId() + ":" + operand.dataChoice.getName();
                    aclone.operands[k].selection = selection;
                }
            }
        }

        return aclone;
    }

    public static void replicateCompute(DataBrowser dataBrowser, DataSource dataSource, Selection selection) {
        if (listOfComputes.isEmpty()) return;
        Compute compute = null;

        Iterator<Compute> iter = listOfComputes.iterator();
        while (iter.hasNext()) {
            compute = iter.next();
            boolean all = true;
            for (int k = 0; k < compute.numOperands; k++) {
                Operand operand = compute.operands[k];
                if (!operand.isEmpty()) {
                    if (!(operand.dataSource.getDescription().equals(dataSource.getDescription()))) {
                        all = false;
                    }
                }
            }
            if (!all) {
                compute = null;
            }


            if (compute != null) {
                try {
                    compute = compute.cloneForDataSource(dataSource, selection);
                } catch (CloneNotSupportedException exc) {
                    exc.printStackTrace();
                }

                LeafInfo info = new LeafInfo(compute, compute.getOperationName(), 0);
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(info);
                dataBrowser.addUserTreeNode(node);
            }

        }
    }

    public static void replicateCompute(Compute compute, DataBrowser dataBrowser) {

        Iterator<DataSource> iter = DataSourceFactory.getDataSources().iterator();
        ArrayList<DefaultMutableTreeNode> nodes = new ArrayList();

        while (iter.hasNext()) {
            DataSource dataSource = iter.next();

            boolean all = true;
            for (int k = 0; k < compute.numOperands; k++) {
                Operand operand = compute.operands[k];
                if (!operand.isEmpty()) {
                    if (!(operand.dataSource.getDescription().equals(dataSource.getDescription()))) {
                        all = false;
                    }
                }
            }

            if (all) {
                try {
                    compute = compute.cloneForDataSource(dataSource, Hydra.dataSourceIdToSelector.get(dataSource.getDataSourceId()));
                } catch (CloneNotSupportedException exc) {
                    exc.printStackTrace();
                }
                listOfComputes.add(compute);
                LeafInfo info = new LeafInfo(compute, compute.getOperationName(), 0);
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(info);
                nodes.add(node);
            }
        }

        if (!nodes.isEmpty()) {
            dataBrowser.addUserTreeNodes((DefaultMutableTreeNode[]) nodes.toArray(new DefaultMutableTreeNode[1]));
        }
    }

    public static void removeCompute(Compute compute) {
        listOfComputes.remove(compute);
    }

    public static void removeCompute(int dataSourceId) {
        ArrayList<Compute> removeThese = new ArrayList<>();
        Iterator<Compute> iter = listOfComputes.iterator();
        while (iter.hasNext()) {
            Compute compute = iter.next();

            for (int k = 0; k < compute.numOperands; k++) {
                Operand operand = compute.operands[k];
                if (!operand.isEmpty()) {
                    if (operand.dataSource.getDataSourceId() == dataSourceId) {
                        removeThese.add(compute);
                        break;
                    }
                }
            }
        }

        iter = removeThese.iterator();
        while (iter.hasNext()) {
            Compute compute = iter.next();
            listOfComputes.remove(compute);
        }
    }
}
