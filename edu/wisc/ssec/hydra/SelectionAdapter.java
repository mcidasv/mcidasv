/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
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

import javax.swing.*;
import java.util.*;


public abstract class SelectionAdapter implements Selection {

    private static ArrayList<Selection> listOfSelectors = new ArrayList<Selection>();

    protected ArrayList<SelectionListener> selectionListeners = new ArrayList<SelectionListener>();

    private DataSource dataSource;

    public static SelectionEvent lastSelectionEvent = null;

    public SelectionAdapter(DataSource dataSource) {
        this.dataSource = dataSource;
        for (int k = 0; k < listOfSelectors.size(); k++) {
            Selection selection = listOfSelectors.get(k);
            ArrayList<SelectionListener> listeners = selection.getSelectionListeners();
            int numListeners = listeners.size();
            for (int t = 0; t < numListeners; t++) {
                SelectionListener listener = listeners.get(t);
                addSelectionListener(listener);
            }
        }
        listOfSelectors.add(this);
    }

    public SelectionAdapter() {
        this(null);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public String getSelectedName() {
        return null;
    }

    public JComponent getComponent() {
        return null;
    }

    public synchronized void remove() {
        listOfSelectors.remove(this);
    }

    public synchronized static void addSelectionListenerToAll(SelectionListener listener) {
        for (int k = 0; k < listOfSelectors.size(); k++) {
            listOfSelectors.get(k).addSelectionListener(listener);
        }
    }

    public synchronized static void removeSelectionListenerFromAll(SelectionListener listener) {
        for (int k = 0; k < listOfSelectors.size(); k++) {
            listOfSelectors.get(k).removeSelectionListener(listener);
        }
    }

    public synchronized void addSelectionListener(SelectionListener listener) {
        selectionListeners.add(listener);
    }

    public synchronized void removeSelectionListener(SelectionListener listener) {
        selectionListeners.remove(listener);
    }

    public synchronized ArrayList<SelectionListener> getSelectionListeners() {
        return selectionListeners;
    }

    public void fireSelectionEvent() {
        SelectionEvent e = new SelectionEvent(this, getDataSource(), getSelectedDataChoice(), getSelectedName());
        for (int k = 0; k < selectionListeners.size(); k++) {
            selectionListeners.get(k).selectionPerformed(e);
        }
    }
}
