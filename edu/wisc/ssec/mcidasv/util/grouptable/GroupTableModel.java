/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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
package edu.wisc.ssec.mcidasv.util.grouptable;

import java.awt.Dimension;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

public class GroupTableModel extends DefaultTableModel {

    protected CellAttribute cellAtt;

    public GroupTableModel() {
        this((Vector<Object>)null, 0);
    }

    public GroupTableModel(int numRows, int numColumns) {
        Vector<Object> names = new Vector<Object>(numColumns);
        names.setSize(numColumns);
        setColumnIdentifiers(names);
        dataVector = new Vector<Object>();
        setNumRows(numRows);
        cellAtt = new DefaultCellAttribute(numRows,numColumns);
    }

    public GroupTableModel(Vector<Object> columnNames, int numRows) {
        setColumnIdentifiers(columnNames);
        dataVector = new Vector<Object>();
        setNumRows(numRows);
        cellAtt = new DefaultCellAttribute(numRows,columnNames.size());
    }

    public GroupTableModel(Object[] columnNames, int numRows) {
        this(convertToVector(columnNames), numRows);
    }

    public GroupTableModel(Vector<Object> data, Vector<Object> columnNames) {
        setDataVector(data, columnNames);
    }

    public GroupTableModel(Object[][] data, Object[] columnNames) {
        setDataVector(data, columnNames);
    }

    public void setDataVector(Vector newData, Vector columnNames) {
        if (newData == null)
            throw new NullPointerException();

        super.setDataVector(newData, columnNames);

        dataVector = newData;

        cellAtt = new DefaultCellAttribute(dataVector.size(), columnIdentifiers.size());

        newRowsAdded(new TableModelEvent(this, 0, getRowCount()-1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
    }

    public void addColumn(Object columnName, Vector columnData) {
        if (columnName == null)
            throw new NullPointerException();

        columnIdentifiers.addElement(columnName);
        int index = 0;
        Enumeration enumeration = dataVector.elements();
        while (enumeration.hasMoreElements()) {
            Object value;
            if ((columnData != null) && (index < columnData.size()))
                value = columnData.elementAt(index);
            else
                value = null;
            ((Vector)enumeration.nextElement()).addElement(value);
            index++;
        }

        cellAtt.addColumn();
        fireTableStructureChanged();
    }

    public void addRow(Vector rowData) {
        Vector<Object> newData = null;
        if (rowData == null) {
            newData = new Vector<Object>(getColumnCount());
        } else {
            rowData.setSize(getColumnCount());
        }

        dataVector.addElement(newData);
        cellAtt.addRow();

        newRowsAdded(new TableModelEvent(this, getRowCount()-1, getRowCount()-1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
    }

    public void insertRow(int row, Vector rowData) {
        if (rowData == null) {
            rowData = new Vector<Object>(getColumnCount());
        } else {
            rowData.setSize(getColumnCount());
        }

        dataVector.insertElementAt(rowData, row);
        cellAtt.insertRow(row);

        newRowsAdded(new TableModelEvent(this, row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
    }

    public CellAttribute getCellAttribute() {
        return cellAtt;
    }

    public void setCellAttribute(CellAttribute newCellAtt) {
        int numColumns = getColumnCount();
        int numRows = getRowCount();
        if ((newCellAtt.getSize().width != numColumns) || (newCellAtt.getSize().height != numRows))
            newCellAtt.setSize(new Dimension(numRows, numColumns));

        cellAtt = newCellAtt;
        fireTableDataChanged();
    }
}

