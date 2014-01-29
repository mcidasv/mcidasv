/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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
package edu.wisc.ssec.mcidasv.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.border.LineBorder;

/**
 * This is a better version of LineBorder which allows you to show line only at one side or several
 * sides and supports rounded corner.
 */
public class PartialLineBorder extends LineBorder {

    final static int NORTH = 1;
    final static int SOUTH = 2;
    final static int EAST = 4;
    final static int WEST = 8;
    final static int HORIZONTAL = NORTH | SOUTH;
    final static int VERTICAL = EAST | WEST;
    final static int ALL = VERTICAL | HORIZONTAL;
    
    private int _sides = ALL;
    private int _roundedCornerSize = 5;

    public PartialLineBorder(Color color) {
        super(color);
    }

    public PartialLineBorder(Color color, int thickness) {
        super(color, thickness);
    }

    public PartialLineBorder(Color color, int thickness, boolean roundedCorners) {
        super(color, thickness, roundedCorners);
    }

    public PartialLineBorder(Color color, int thickness, boolean roundedCorners, int roundedCornerSize) {
        super(color, thickness, roundedCorners);
        _roundedCornerSize = roundedCornerSize;
    }

    public PartialLineBorder(Color color, int thickness, int side) {
        super(color, thickness);
        _sides = side;
    }

    public int getSides() {
        return _sides;
    }

    public void setSides(int sides) {
        _sides = sides;
    }

    public int getRoundedCornerSize() {
        return _roundedCornerSize;
    }

    public void setRoundedCornerSize(int roundedCornerSize) {
        _roundedCornerSize = roundedCornerSize;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();
        int i;

        g.setColor(lineColor);
        for (i = 0; i < thickness; i++) {
            if (_sides == ALL) {
                if (!roundedCorners)
                    g.drawRect(x + i, y + i, width - i - i - 1, height - i - i - 1);
                else {
                    Object o = setupShapeAntialiasing(g);
                    g.drawRoundRect(x + i, y + i, width - i - i - 1, height - i - i - 1, _roundedCornerSize, _roundedCornerSize);
                    restoreShapeAntialiasing(g, o);
                }
            }

            else {
                if ((_sides & NORTH) != 0) {
                    g.drawLine(x, y + i, x + width - 1, y + i);
                }
                if ((_sides & SOUTH) != 0) {
                    g.drawLine(x, y + height - i - 1, x + width - 1, y + height - i - 1);
                }
                if ((_sides & WEST) != 0) {
                    g.drawLine(x + i, y, x + i, y + height - 1);
                }
                if ((_sides & EAST) != 0) {
                    g.drawLine(x + width - i - 1, y, x + width - i - 1, y + height - 1);
                }
            }

        }
        g.setColor(oldColor);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        Insets borderInsets = super.getBorderInsets(c);
        if ((_sides & NORTH) == 0) {
            borderInsets.top = 0;
        }
        if ((_sides & SOUTH) == 0) {
            borderInsets.bottom = 0;
        }
        if ((_sides & WEST) == 0) {
            borderInsets.left = 0;
        }
        if ((_sides & EAST) == 0) {
            borderInsets.right = 0;
        }
        return borderInsets;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        Insets borderInsets = super.getBorderInsets(c, insets);
        if ((_sides & NORTH) == 0) {
            borderInsets.top = 0;
        }
        if ((_sides & SOUTH) == 0) {
            borderInsets.bottom = 0;
        }
        if ((_sides & WEST) == 0) {
            borderInsets.left = 0;
        }
        if ((_sides & EAST) == 0) {
            borderInsets.right = 0;
        }
        return borderInsets;
    }

    /**
     * Setups the graphics to draw shape using anti-alias.
     *
     * @param g
     * @return the old hints. You will need this value as the third parameter in {@link
     *         #restoreShapeAntialiasing(java.awt.Graphics,Object)}.
     */
    private static Object setupShapeAntialiasing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Object oldHints = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return oldHints;
    }

    /**
     * Restores the old setting for shape anti-alias.
     *
     * @param g
     * @param oldHints the value returned from {@link #setupShapeAntialiasing(java.awt.Graphics)}.
     */
    private static void restoreShapeAntialiasing(Graphics g, Object oldHints) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHints);
    }
}
