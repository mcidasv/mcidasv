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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableCellRenderer;

public class GroupTableUI extends BasicTableUI {

    public void paint(Graphics g, JComponent c) {
        Rectangle oldClipBounds = g.getClipBounds();
        Rectangle clipBounds = new Rectangle(oldClipBounds);
        int tableWidth = table.getColumnModel().getTotalColumnWidth();
        clipBounds.width = Math.min(clipBounds.width, tableWidth);
        g.setClip(clipBounds);

        int firstIndex = table.rowAtPoint(new Point(0, clipBounds.y));
        int  lastIndex = table.getRowCount()-1;

        Rectangle rowRect = new Rectangle(0,0,
                tableWidth, table.getRowHeight() + table.getRowMargin());
        rowRect.y = firstIndex*rowRect.height;

        for (int index = firstIndex; index <= lastIndex; index++) {
            if (rowRect.intersects(clipBounds)) {
//                System.err.println();                  // debug
//                System.err.print("" + index +": ");    // row
                paintRow(g, index);
            }
            rowRect.y += rowRect.height;
        }
        g.setClip(oldClipBounds);
    }

    private void paintRow(Graphics g, int row) {
        Rectangle rect = g.getClipBounds();
        boolean drawn  = false;

        GroupTableModel tableModel = (GroupTableModel)table.getModel();
        CellSpan cellAtt = (CellSpan)tableModel.getCellAttribute();
        int numColumns = table.getColumnCount();

        for (int column = 0; column < numColumns; column++) {
            Rectangle cellRect = table.getCellRect(row,column,true);
            int cellRow,cellColumn;
            if (cellAtt.isVisible(row,column)) {
                cellRow    = row;
                cellColumn = column;
//                System.err.print("   "+column+" ");
            } else {
                cellRow    = row + cellAtt.getSpan(row,column)[CellSpan.ROW];
                cellColumn = column + cellAtt.getSpan(row,column)[CellSpan.COLUMN];
//                System.err.print("  ("+column+")");
            }
            if (cellRect.intersects(rect)) {
                drawn = true;
                paintCell(g, cellRect, cellRow, cellColumn);
            } else {
                if (drawn)
                    break;
            }
        }

    }

    private void paintCell(Graphics g, Rectangle cellRect, int row, int column) {
        int spacingHeight = table.getRowMargin();
        int spacingWidth  = table.getColumnModel().getColumnMargin();

        Color c = g.getColor();
        g.setColor(table.getGridColor());
        g.drawRect(cellRect.x,cellRect.y,cellRect.width-1,cellRect.height-1);
        g.setColor(c);

        cellRect.setBounds(cellRect.x + spacingWidth/2, cellRect.y + spacingHeight/2,
                cellRect.width - spacingWidth, cellRect.height - spacingHeight);

        if (table.isEditing() && table.getEditingRow() == row &&
                table.getEditingColumn() == column) {
            Component component = table.getEditorComponent();
            component.setBounds(cellRect);
            component.validate();
        }
        else {
            TableCellRenderer renderer = table.getCellRenderer(row, column);
            Component component = table.prepareRenderer(renderer, row, column);

            if (component.getParent() == null) {
                rendererPane.add(component);
            }
            rendererPane.paintComponent(g, component, table, cellRect.x, cellRect.y,
                    cellRect.width, cellRect.height, true);
        }
    }
}

