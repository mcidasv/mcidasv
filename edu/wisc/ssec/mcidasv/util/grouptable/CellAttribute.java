package edu.wisc.ssec.mcidasv.util.grouptable;

import java.awt.Dimension;

public interface CellAttribute {
    public void addColumn();
    public void addRow();
    public void insertRow(int row);
    public Dimension getSize();
    public void setSize(Dimension size);
}
