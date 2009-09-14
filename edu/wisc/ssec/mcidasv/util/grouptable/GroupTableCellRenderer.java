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


