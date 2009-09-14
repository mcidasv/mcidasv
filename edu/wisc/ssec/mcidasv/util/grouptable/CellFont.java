/*
 * (swing1.1beta3)
 * 
 */

package edu.wisc.ssec.mcidasv.util.grouptable;

import java.awt.Font;

public interface CellFont {
    public Font getFont(int row, int column);
    public void setFont(Font font, int row, int column);
    public void setFont(Font font, int[] rows, int[] columns);
}
