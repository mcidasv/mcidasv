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
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class GroupTableCellRenderer extends JLabel implements TableCellRenderer {
    protected static Border noFocusBorder; 

    public GroupTableCellRenderer() {
        noFocusBorder = new EmptyBorder(1, 2, 1, 2);
        setOpaque(true);
        setBorder(noFocusBorder);  
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Color foreground = null;
        Color background = null;
        Font font = null;
        TableModel model = table.getModel();
        if (model instanceof GroupTableModel) {
            CellAttribute cellAtt = ((GroupTableModel)model).getCellAttribute();
            if (cellAtt instanceof ColoredCell) {
                foreground = ((ColoredCell)cellAtt).getForeground(row,column);
                background = ((ColoredCell)cellAtt).getBackground(row,column);
            }
            if (cellAtt instanceof CellFont) {
                font = ((CellFont)cellAtt).getFont(row,column);
            }
        }
        if (isSelected) {
            setForeground((foreground != null) ? foreground
                    : table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
        } else {
            setForeground((foreground != null) ? foreground 
                    : table.getForeground());
            setBackground((background != null) ? background 
                    : table.getBackground());
        }
        setFont((font != null) ? font : table.getFont());

        if (hasFocus) {
            setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
            if (table.isCellEditable(row, column)) {
                setForeground((foreground != null) ? foreground
                        : UIManager.getColor("Table.focusCellForeground") );
                setBackground( UIManager.getColor("Table.focusCellBackground") );
            }
        } else {
            setBorder(noFocusBorder);
        }
        setValue(value);        
        return this;
    }

    protected void setValue(Object value) {
        setText((value == null) ? "" : value.toString());
    }
}


