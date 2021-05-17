/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2021
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.accessibility.AccessibleContext;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.colorchooser.AbstractColorChooserPanel;

import org.jdesktop.beans.AbstractBean;

/**
 * This has been essentially ripped out of the (wonderful) GNU Classpath
 * project. Initial implementation of persistable recent colors courtesy of
 *
 * http://stackoverflow.com/a/11080701
 *
 * (though I had to hack things up a bit)
 */
public class PersistableSwatchChooserPanel extends AbstractColorChooserPanel implements PropertyChangeListener {

//    private static final Logger logger = LoggerFactory.getLogger(PersistableSwatchChooserPanel.class);

    /** The main panel that holds the set of choosable colors. */
    MainSwatchPanel mainPalette;

    /** A panel that holds the recent colors. */
    RecentSwatchPanel recentPalette;

    /** The mouse handlers for the panels. */
    MouseListener mouseHandler;

    /** Main Palette {@code KeyListener}. */
    KeyListener mainSwatchKeyListener;

    /** Recent palette {@code KeyListener}. */
    KeyListener recentSwatchKeyListener;

    ColorTracker tracker;

    /**
     * This the base class for all swatch panels. Swatch panels are panels that
     * hold a set of blocks where colors are displayed.
     */
    abstract static class SwatchPanel extends JPanel {

        /** Width of each block. */
        protected int cellWidth = 10;

        /** Height of each block. */
        protected int cellHeight = 10;

        /** Gap between blocks. */
        protected int gap = 1;

        /** Number of rows in the swatch panel. */
        protected int numRows;

        /** Number of columns in the swatch panel. */
        protected int numCols;

        /** Row of the selected color's cell. */
        protected int selRow;

        /** Column of the selected color's cell. */
        protected int selCol;

        /**
         * Creates a new SwatchPanel object.
         */
        SwatchPanel() {
            selRow = 0;
            selCol = 0;
            setBackground(Color.WHITE);
            setFocusable(true);

            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override public void focusLost(FocusEvent e) {
                    repaint();
                }
            });

            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    int code = e.getKeyCode();
                    switch (code) {
                        case KeyEvent.VK_UP:
                            if (selRow > 0) {
                                selRow--;
                                repaint();
                            }
                            break;
                        case KeyEvent.VK_DOWN:
                            if (selRow < numRows - 1) {
                                selRow++;
                                repaint();
                            }
                            break;
                        case KeyEvent.VK_LEFT:
                            if (selCol > 0 && SwatchPanel.this.getComponentOrientation().isLeftToRight()) {
                                selCol--;
                                repaint();
                            } else if (selCol < numCols -1 && !SwatchPanel.this.getComponentOrientation().isLeftToRight()) {
                                selCol++;
                                repaint();
                            }
                            break;
                        case KeyEvent.VK_RIGHT:
                            if (selCol < numCols - 1
                                && SwatchPanel.this.getComponentOrientation().isLeftToRight()) {
                                selCol++;
                                repaint();
                            } else if (selCol > 0 && !SwatchPanel.this.getComponentOrientation().isLeftToRight()) {
                                selCol--;
                                repaint();
                            }
                            break;
                        case KeyEvent.VK_HOME:
                            selCol = 0;
                            selRow = 0;
                            repaint();
                            break;
                        case KeyEvent.VK_END:
                            selCol = numCols - 1;
                            selRow = numRows - 1;
                            repaint();
                            break;
                    }
                }
            });
        }

        /**
         * This method returns the preferred size of the swatch panel based on the
         * number of rows and columns and the size of each cell.
         *
         * @return Preferred size of the swatch panel.
         */
        @Override public Dimension getPreferredSize() {
            int height = (numRows * cellHeight) + ((numRows - 1) * gap);
            int width = (numCols * cellWidth) + ((numCols - 1) * gap);
            Insets insets = getInsets();

            return new Dimension(width + insets.left + insets.right,
                height + insets.top + insets.bottom);
        }

        /**
         * Return the {@literal "selected"} color.
         *
         * @return The color at {@code selRow} and {@code selCol}.
         */
        public Color getSelectedColor() {
            return getColorForCell(selRow, selCol);
        }

        /**
         * This method returns the color for the given position.
         *
         * @param x X coordinate of the position.
         * @param y Y coordinate of the position.
         *
         * @return The color at the given position.
         */
        public abstract Color getColorForPosition(int x, int y);

        /**
         * Return the color at a given cell.
         *
         * @param row Cell row.
         * @param column Cell column.
         *
         * @return Color of the cell at {@code row} and {@code column}.
         */
        public abstract Color getColorForCell(int row, int column);

        /**
         * This method initializes the colors for the swatch panel.
         */
        protected abstract void initializeColors();

        /**
         * Set the {@literal "selected"} cell using screen location.
         *
         * @param x X coordinate of the position.
         * @param y Y coordinate of the position.
         */
        protected abstract void setSelectedCellFromPosition(int x, int y);
    }

    /**
     * This is the main swatch panel. This panel sits in the middle and allows a
     * set of colors to be picked which will move to the recent swatch panel.
     */
    static class MainSwatchPanel extends SwatchPanel {
        /** The color describing (204, 255, 255) */
        public static final Color C204255255 = new Color(204, 204, 255);

        /** The color describing (255, 204, 204) */
        public static final Color C255204204 = new Color(255, 204, 204);

        /** The color describing (204, 255, 204) */
        public static final Color C204255204 = new Color(204, 255, 204);

        /** The color describing (204, 204, 204) */
        public static final Color C204204204 = new Color(204, 204, 204);

        /** The color (153, 153, 255). */
        public static final Color C153153255 = new Color(153, 153, 255);

        /** The color (51, 51, 255). */
        public static final Color C051051255 = new Color(51, 51, 255);

        /** The color (153, 0, 153). */
        public static final Color C153000153 = new Color(153, 0, 153);

        /** The color (0, 51, 51). */
        public static final Color C000051051 = new Color(0, 51, 51);

        /** The color (51, 0, 51). */
        public static final Color C051000051 = new Color(51, 0, 51);

        /** The color (51, 51, 0). */
        public static final Color C051051000 = new Color(51, 51, 0);

        /** The color (102, 102, 0). */
        public static final Color C102102000 = new Color(102, 102, 0);

        /** The color (153, 255, 153). */
        public static final Color C153255153 = new Color(153, 255, 153);

        /** The color (102, 255, 102). */
        public static final Color C102255102 = new Color(102, 255, 102);

        /** The color (0, 102, 102). */
        public static final Color C000102102 = new Color(0, 102, 102);

        /** The color (102, 0, 102). */
        public static final Color C102000102 = new Color(102, 0, 102);

        /** The color (0, 153, 153). */
        public static final Color C000153153 = new Color(0, 153, 153);

        /** The color (153, 153, 0). */
        public static final Color C153153000 = new Color(153, 153, 0);

        /** The color (204, 204, 0). */
        public static final Color C204204000 = new Color(204, 204, 0);

        /** The color (204, 0, 204). */
        public static final Color C204000204 = new Color(204, 0, 204);

        /** The color (0, 204, 204). */
        public static final Color C000204204 = new Color(0, 204, 204);

        /** The color (51, 255, 51). */
        public static final Color C051255051 = new Color(51, 255, 51);

        /** The color (255, 51, 51). */
        public static final Color C255051051 = new Color(255, 51, 51);

        /** The color (255, 102, 102). */
        public static final Color C255102102 = new Color(255, 102, 102);

        /** The color (102, 102, 255). */
        public static final Color C102102255 = new Color(102, 102, 255);

        /** The color (255, 153, 153). */
        public static final Color C255153153 = new Color(255, 153, 153);
        static Color[] colors =
            {
                // Row 1
                Color.WHITE, new Color(204, 255, 255), C204255255, C204255255, C204255255,
                C204255255, C204255255, C204255255, C204255255,
                C204255255, C204255255, new Color(255, 204, 255),
                C255204204, C255204204, C255204204, C255204204,
                C255204204, C255204204, C255204204, C255204204,
                C255204204, new Color(255, 255, 204), C204255204,
                C204255204, C204255204, C204255204, C204255204,
                C204255204, C204255204, C204255204, C204255204,

                // Row 2
                C204204204, new Color(153, 255, 255), new Color(153, 204, 255), C153153255,
                C153153255, C153153255, C153153255, C153153255,
                C153153255, C153153255, new Color(204, 153, 255),
                new Color(255, 153, 255),
                new Color(255, 153, 204), C255153153, C255153153,
                C255153153, C255153153, C255153153, C255153153,
                C255153153, new Color(255, 204, 153),
                new Color(255, 255, 153),
                new Color(204, 255, 153), C153255153, C153255153,
                C153255153, C153255153, C153255153, C153255153,
                C153255153, new Color(153, 255, 204),

                // Row 3
                C204204204, new Color(102, 255, 255), new Color(102, 204, 255),
                new Color(102, 153, 255), C102102255, C102102255,
                C102102255, C102102255, C102102255,
                new Color(153, 102, 255),
                new Color(204, 102, 255),
                new Color(255, 102, 255),
                new Color(255, 102, 204),
                new Color(255, 102, 153), C255102102, C255102102,
                C255102102, C255102102, C255102102,
                new Color(255, 153, 102),
                new Color(255, 204, 102),
                new Color(255, 255, 102),
                new Color(204, 255, 102),
                new Color(153, 255, 102), C102255102, C102255102,
                C102255102, C102255102, C102255102,
                new Color(102, 255, 153),
                new Color(102, 255, 204),

                // Row 4
                new Color(153, 153, 153), new Color(51, 255, 255), new Color(51, 204, 255),
                new Color(51, 153, 255), new Color(51, 102, 255),
                C051051255, C051051255, C051051255,
                new Color(102, 51, 255), new Color(153, 51, 255),
                new Color(204, 51, 255), new Color(255, 51, 255),
                new Color(255, 51, 204), new Color(255, 51, 153),
                new Color(255, 51, 102), C255051051, C255051051,
                C255051051, new Color(255, 102, 51),
                new Color(255, 153, 51), new Color(255, 204, 51),
                new Color(255, 255, 51), new Color(204, 255, 51),
                new Color(153, 255, 51), new Color(102, 255, 51),
                C051255051, C051255051, C051255051,
                new Color(51, 255, 102), new Color(51, 255, 153),
                new Color(51, 255, 204),

                // Row 5
                new Color(153, 153, 153), new Color(0, 255, 255), new Color(0, 204, 255),
                new Color(0, 153, 255), new Color(0, 102, 255),
                new Color(0, 51, 255), new Color(0, 0, 255),
                new Color(51, 0, 255), new Color(102, 0, 255),
                new Color(153, 0, 255), new Color(204, 0, 255),
                new Color(255, 0, 255), new Color(255, 0, 204),
                new Color(255, 0, 153), new Color(255, 0, 102),
                new Color(255, 0, 51), new Color(255, 0, 0),
                new Color(255, 51, 0), new Color(255, 102, 0),
                new Color(255, 153, 0), new Color(255, 204, 0),
                new Color(255, 255, 0), new Color(204, 255, 0),
                new Color(153, 255, 0), new Color(102, 255, 0),
                new Color(51, 255, 0), new Color(0, 255, 0),
                new Color(0, 255, 51), new Color(0, 255, 102),
                new Color(0, 255, 153), new Color(0, 255, 204),

                // Row 6
                new Color(102, 102, 102), C000204204, C000204204, new Color(0, 153, 204),
                new Color(0, 102, 204), new Color(0, 51, 204),
                new Color(0, 0, 204), new Color(51, 0, 204),
                new Color(102, 0, 204), new Color(153, 0, 204),
                C204000204, C204000204, C204000204,
                new Color(204, 0, 153), new Color(204, 0, 102),
                new Color(204, 0, 51), new Color(204, 0, 0),
                new Color(204, 51, 0), new Color(204, 102, 0),
                new Color(204, 153, 0), C204204000, C204204000,
                C204204000, new Color(153, 204, 0),
                new Color(102, 204, 0), new Color(51, 204, 0),
                new Color(0, 204, 0), new Color(0, 204, 51),
                new Color(0, 204, 102), new Color(0, 204, 153),
                new Color(0, 204, 204),

                // Row 7
                new Color(102, 102, 102), C000153153, C000153153, C000153153,
                new Color(0, 102, 153), new Color(0, 51, 153),
                new Color(0, 0, 153), new Color(51, 0, 153),
                new Color(102, 0, 153), C153000153, C153000153,
                C153000153, C153000153, C153000153,
                new Color(153, 0, 102), new Color(153, 0, 51),
                new Color(153, 0, 0), new Color(153, 51, 0),
                new Color(153, 102, 0), C153153000, C153153000,
                C153153000, C153153000, C153153000,
                new Color(102, 153, 0), new Color(51, 153, 0),
                new Color(0, 153, 0), new Color(0, 153, 51),
                new Color(0, 153, 102), C000153153, C000153153,

                // Row 8
                new Color(51, 51, 51), C000102102, C000102102, C000102102, C000102102,
                new Color(0, 51, 102), new Color(0, 0, 102),
                new Color(51, 0, 102), C102000102, C102000102,
                C102000102, C102000102, C102000102, C102000102,
                C102000102, new Color(102, 0, 51),
                new Color(102, 0, 0), new Color(102, 51, 0),
                C102102000, C102102000, C102102000, C102102000,
                C102102000, C102102000, C102102000,
                new Color(51, 102, 0), new Color(0, 102, 0),
                new Color(0, 102, 51), C000102102, C000102102,
                C000102102,

                // Row 9.
                Color.BLACK, C000051051, C000051051, C000051051, C000051051, C000051051,
                new Color(0, 0, 51), C051000051, C051000051,
                C051000051, C051000051, C051000051, C051000051,
                C051000051, C051000051, C051000051,
                new Color(51, 0, 0), C051051000, C051051000,
                C051051000, C051051000, C051051000, C051051000,
                C051051000, C051051000, new Color(0, 51, 0),
                C000051051, C000051051, C000051051, C000051051,
                new Color(51, 51, 51)
            };

        /**
         * Creates a new MainSwatchPanel object.
         */
        MainSwatchPanel() {
            numCols = 31;
            numRows = 9;
            initializeColors();
            // incredibly, this setToolTipText call is how you register to
            // listen for events
            setToolTipText("");
            revalidate();
        }

        /**
         * This method returns the color for the given position.
         *
         * @param x X location for the position.
         * @param y Y location for the position.
         *
         * @return {@link Color} for the given position.
         */
        @Override public Color getColorForPosition(int x, int y) {
            if (((x % (cellWidth + gap)) > cellWidth) || ((y % (cellHeight + gap)) > cellHeight)) {
                // position is located in gap.
                return null;
            }

            int row = y / (cellHeight + gap);
            int col = x / (cellWidth + gap);
            return colors[row * numCols + col];
        }

        @Override protected void setSelectedCellFromPosition(int x, int y) {
            if (((x % (cellWidth + gap)) > cellWidth) || ((y % (cellHeight + gap)) > cellHeight)) {
                // position is located in gap.
                return;
            }
            selRow = y / (cellHeight + gap);
            selCol = x / (cellWidth + gap);
        }

        @Override public Color getColorForCell(int row, int column) {
            return colors[row * numCols + column];
        }

        /**
         * This method initializes the colors for the main swatch panel.
         */
        @Override protected void initializeColors() {
            // Unnecessary
        }

        /**
         * This method paints the main graphics panel with the given Graphics
         * object.
         *
         * @param graphics The Graphics object to paint with.
         */
        @Override public void paint(Graphics graphics) {
            int index = 0;
            Insets insets = getInsets();
            int currX = insets.left;
            int currY = insets.top;
            Color saved = graphics.getColor();

            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    Color current = colors[index++];
                    graphics.setColor(current);
                    graphics.fill3DRect(currX, currY, cellWidth, cellHeight, true);
                    if ((selRow == i) && (selCol == j) && this.isFocusOwner()) {
                        Color cursorColor = new Color(current.getRed() < 125 ? 255 : 0,
                            current.getGreen() < 125 ? 255 : 0,
                            current.getBlue() < 125 ? 255 : 0);

                        graphics.setColor(cursorColor);
                        graphics.drawLine(currX, currY, currX + cellWidth - 1, currY);
                        graphics.drawLine(currX, currY, currX, currY + cellHeight - 1);
                        graphics.drawLine(currX + cellWidth - 1, currY, currX + cellWidth- 1, currY + cellHeight - 1);
                        graphics.drawLine(currX, currY + cellHeight - 1, currX + cellWidth - 1, currY + cellHeight - 1);
                        graphics.drawLine(currX, currY, currX + cellWidth - 1, currY + cellHeight - 1);
                        graphics.drawLine(currX, currY + cellHeight - 1, currX + cellWidth - 1, currY);
                    }
                    currX += gap + cellWidth;
                }
                currX = insets.left;
                currY += gap + cellHeight;
            }
            graphics.setColor(saved);
        }

        /**
         * This method returns the tooltip text for the given MouseEvent.
         *
         * @param e The MouseEvent to find tooltip text for.
         *
         * @return The tooltip text.
         */
        @Override public String getToolTipText(MouseEvent e) {
            Color c = getColorForPosition(e.getX(), e.getY());
            String tip = null;
            if (c != null) {
                tip = c.getRed() + ", " + c.getGreen() + ", " + c.getBlue();
            }
            return tip;
        }
    }

    /**
     * This class is the recent swatch panel. It holds recently selected colors.
     */
    static class RecentSwatchPanel extends SwatchPanel {

        /** The array for storing recently stored colors. */
        Color[] colors;

        /** The index of the array that is the start. */
        int start = 0;

        /**
         * Creates a new RecentSwatchPanel object.
         */
        RecentSwatchPanel() {
            numCols = 5;
            numRows = 7;
            initializeColors();
            // incredibly, this setToolTipText call is how you register to
            // listen for events
            setToolTipText("");
            revalidate();
        }

        /**
         * This method returns the color for the given position.
         *
         * @param x The x coordinate of the position.
         * @param y The y coordinate of the position.
         *
         * @return The color for the given position.
         */
        @Override public Color getColorForPosition(int x, int y) {
            if (((x % (cellWidth + gap)) > cellWidth) || ((y % (cellHeight + gap)) > cellHeight)) {
                // position is located in gap.
                return null;
            }

            int row = y / (cellHeight + gap);
            int col = x / (cellWidth + gap);

            return colors[getIndexForCell(row, col)];
        }

        @Override protected void setSelectedCellFromPosition(int x, int y) {
            if (((x % (cellWidth + gap)) > cellWidth) || ((y % (cellHeight + gap)) > cellHeight)) {
                // position is located in gap.
                return;
            }
            selRow = y / (cellHeight + gap);
            selCol = x / (cellWidth + gap);
        }

        /**
         * This method initializes the colors for the recent swatch panel.
         */
        @Override protected void initializeColors() {
            final Color defaultColor =
                UIManager.getColor("ColorChooser.swatchesDefaultRecentColor", getLocale());
            colors = new Color[numRows * numCols];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = defaultColor;
            }
        }

        /**
         * This method returns the array index for the given row and column.
         *
         * @param row The row.
         * @param col The column.
         *
         * @return The array index for the given row and column.
         */
        private int getIndexForCell(int row, int col) {
            return ((row * numCols) + col + start) % (numRows * numCols);
        }

        public Color getColorForCell(int row, int column) {
            return colors[getIndexForCell(row, column)];
        }

        /**
         * This method adds the given color to the beginning of the swatch panel.
         * Package-private to avoid an accessor method.
         *
         * @param c The color to add.
         */
        void addColorToQueue(Color c) {
            if (--start == -1) {
                start = numRows * numCols - 1;
            }
            colors[start] = c;
        }

        void addColorsToQueue(List<Color> colorsToAdd) {
            if ((colorsToAdd != null) && !colorsToAdd.isEmpty()) {
                for (int i = colorsToAdd.size() - 1; i >= 0; i--) {
                    addColorToQueue(colorsToAdd.get(i));
                }
            }
        }

        /**
         * This method paints the panel with the given Graphics object.
         *
         * @param g The Graphics object to paint with.
         */
        @Override public void paint(Graphics g) {
            Color saved = g.getColor();
            Insets insets = getInsets();
            int currX = insets.left;
            int currY = insets.top;

            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    Color current = colors[getIndexForCell(i, j)];
                    g.setColor(current);
                    g.fill3DRect(currX, currY, cellWidth, cellHeight, true);
                    if ((selRow == i) && (selCol == j) && this.isFocusOwner()) {
                        Color cursorColor = new Color(current.getRed() < 125 ? 255 : 0,
                            current.getGreen() < 125 ? 255 : 0,
                            current.getBlue() < 125 ? 255 : 0);
                        g.setColor(cursorColor);
                        g.drawLine(currX, currY, currX + cellWidth - 1, currY);
                        g.drawLine(currX, currY, currX, currY + cellHeight - 1);
                        g.drawLine(currX + cellWidth - 1, currY, currX + cellWidth- 1, currY + cellHeight - 1);
                        g.drawLine(currX, currY + cellHeight - 1, currX + cellWidth - 1, currY + cellHeight - 1);
                        g.drawLine(currX, currY, currX + cellWidth - 1, currY + cellHeight - 1);
                        g.drawLine(currX, currY + cellHeight - 1, currX + cellWidth - 1, currY);
                    }
                    currX += cellWidth + gap;
                }
                currX = insets.left;
                currY += cellWidth + gap;
            }
        }

        /**
         * This method returns the tooltip text for the given MouseEvent.
         *
         * @param e The MouseEvent.
         *
         * @return The tooltip text.
         */
        @Override public String getToolTipText(MouseEvent e) {
            Color c = getColorForPosition(e.getX(), e.getY());
//            logger.trace("x={} y={} c={}", e.getX(), e.getY(), c);
            String tip = null;
            if (c != null) {
                tip = c.getRed() + ", " + c.getGreen() + ", " + c.getBlue();
            }
            return tip;
        }
    }

    /**
     * This class handles mouse events for the two swatch panels.
     */
    class MouseHandler extends MouseAdapter {
        /**
         * This method is called whenever the mouse is pressed.
         *
         * @param e The MouseEvent.
         */
        @Override public void mousePressed(MouseEvent e) {
            if (isEnabled()) {
                SwatchPanel panel = (SwatchPanel)e.getSource();
                Color c = panel.getColorForPosition(e.getX(), e.getY());
                panel.setSelectedCellFromPosition(e.getX(), e.getY());
                // yes, the "!=" is intentional.
                if (panel != recentPalette) {
                    recentPalette.addColorToQueue(c);
                    if (tracker != null) {
                        tracker.addColor(c);
                    }
                }
                PersistableSwatchChooserPanel.this.getColorSelectionModel().setSelectedColor(c);
                PersistableSwatchChooserPanel.this.repaint();
                panel.requestFocusInWindow();
            }
        }
    }

    /**
     * This class handles the user {@literal "selecting"} a recently used
     * color using the space key.
     */
    private class RecentSwatchKeyListener extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            if (KeyEvent.VK_SPACE == e.getKeyCode()) {
                Color color = recentPalette.getSelectedColor();
                PersistableSwatchChooserPanel.this.getColorSelectionModel().setSelectedColor(color);
                PersistableSwatchChooserPanel.this.repaint();
            }
        }
    }

    /**
     * This class handles the user {@literal "selecting"} a color using the
     * space key.
     */
    private class MainSwatchKeyListener extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            if (KeyEvent.VK_SPACE == e.getKeyCode()) {
                Color color = mainPalette.getSelectedColor();
                recentPalette.addColorToQueue(color);
                if (tracker != null) {
                    tracker.addColor(color);
                }
                PersistableSwatchChooserPanel.this.getColorSelectionModel().setSelectedColor(color);
                PersistableSwatchChooserPanel.this.repaint();
            }
        }
    }

    /**
     * This is the layout manager for the main panel.
     */
    static class MainPanelLayout implements LayoutManager {
        /**
         * This method is called when a new component is added to the container.
         *
         * @param name The name of the component.
         * @param comp The added component.
         */
        @Override public void addLayoutComponent(String name, Component comp) {
            // Nothing to do here.
        }

        /**
         * This method is called to set the size and position of the child
         * components for the given container.
         *
         * @param parent The container to lay out.
         */
        @Override public void layoutContainer(Container parent) {
            Component[] comps = parent.getComponents();
            Insets insets = parent.getInsets();
            Dimension[] pref = new Dimension[comps.length];

            int xpos = 0;
            int ypos = 0;
            int maxHeight = 0;
            int totalWidth = 0;

            for (int i = 0; i < comps.length; i++) {
                pref[i] = comps[i].getPreferredSize();
                if (pref[i] == null) {
                    return;
                }
                maxHeight = Math.max(maxHeight, pref[i].height);
                totalWidth += pref[i].width;
            }

            ypos = (parent.getSize().height - maxHeight) / 2 + insets.top;
            xpos = insets.left + (parent.getSize().width - totalWidth) / 2;

            for (int i = 0; i < comps.length; i++) {
                if (pref[i] == null) {
                    continue;
                }
                comps[i].setBounds(xpos, ypos, pref[i].width, pref[i].height);
                xpos += pref[i].width;
            }
        }

        /**
         * This method is called when a component is removed from the container.
         *
         * @param comp The component that was removed.
         */
        @Override public void removeLayoutComponent(Component comp) {
            // Nothing to do here.
        }

        /**
         * This methods calculates the minimum layout size for the container.
         *
         * @param parent The container.
         *
         * @return The minimum layout size.
         */
        @Override public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        /**
         * This method returns the preferred layout size for the given container.
         *
         * @param parent The container.
         *
         * @return The preferred layout size.
         */
        @Override public Dimension preferredLayoutSize(Container parent) {
            int xmax = 0;
            int ymax = 0;

            Component[] comps = parent.getComponents();

            for (Component comp : comps) {
                Dimension pref = comp.getPreferredSize();
                if (pref == null) {
                    continue;
                }
                xmax += pref.width;
                ymax = Math.max(ymax, pref.height);
            }

            Insets insets = parent.getInsets();

            return new Dimension(insets.left + insets.right + xmax,
                insets.top + insets.bottom + ymax);
        }
    }

    /**
     * This is the layout manager for the recent swatch panel.
     */
    static class RecentPanelLayout implements LayoutManager {
        /**
         * This method is called when a component is added to the container.
         *
         * @param name The name of the component.
         * @param comp The added component.
         */
        @Override public void addLayoutComponent(String name, Component comp) {
            // Nothing needs to be done.
        }

        /**
         * This method sets the size and position of the child components of the
         * given container.
         *
         * @param parent The container to lay out.
         */
        @Override public void layoutContainer(Container parent) {
            Component[] comps = parent.getComponents();
            Dimension parentSize = parent.getSize();
            Insets insets = parent.getInsets();
            int currY = insets.top;

            for (Component comp : comps) {
                Dimension pref = comp.getPreferredSize();
                if (pref == null) {
                    continue;
                }
                comp.setBounds(insets.left, currY, pref.width, pref.height);
                currY += pref.height;
            }
        }

        /**
         * This method calculates the minimum layout size for the given container.
         *
         * @param parent The container.
         *
         * @return The minimum layout size.
         */
        @Override public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        /**
         * This method calculates the preferred layout size for the given
         * container.
         *
         * @param parent The container.
         *
         * @return The preferred layout size.
         */
        @Override public Dimension preferredLayoutSize(Container parent) {
            int width = 0;
            int height = 0;
            Insets insets = parent.getInsets();
            Component[] comps = parent.getComponents();
            for (Component comp : comps) {
                Dimension pref = comp.getPreferredSize();
                if (pref != null) {
                    width = Math.max(width, pref.width);
                    height += pref.height;
                }
            }

            return new Dimension(width + insets.left + insets.right,
                height + insets.top + insets.bottom);
        }

        /**
         * This method is called whenever a component is removed from the
         * container.
         *
         * @param comp The removed component.
         */
        @Override public void removeLayoutComponent(Component comp) {
            // Nothing needs to be done.
        }
    }

    /**
     * Creates a new DefaultSwatchChooserPanel object.
     */
    PersistableSwatchChooserPanel() {
        super();
    }

    /**
     * This method updates the chooser panel with the new value from the
     * JColorChooser.
     */
    @Override public void updateChooser() {
        // Nothing to do here yet.
    }

    /**
     * This method builds the chooser panel.
     */
    @Override protected void buildChooser() {
        // The structure of the swatch panel is:
        // One large panel (minus the insets).
        // Inside that panel, there are two panels, one holds the palette.
        // The other holds the label and the recent colors palette.
        // The two palettes are two custom swatch panels.
        setLayout(new MainPanelLayout());

        JPanel mainPaletteHolder = new JPanel();
        JPanel recentPaletteHolder = new JPanel();

        mainPalette = new MainSwatchPanel();
        mainPalette.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, getDisplayName());
        mainPalette.setInheritsPopupMenu(true);

        String recentLabel = UIManager.getString("ColorChooser.swatchesRecentText", getLocale());
        recentPalette = new RecentSwatchPanel();
        recentPalette.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, recentLabel);
        recentPalette.setInheritsPopupMenu(true);

        JLabel label = new JLabel(recentLabel);
        label.setLabelFor(recentPalette);

        mouseHandler = new MouseHandler();
        mainPalette.addMouseListener(mouseHandler);
        recentPalette.addMouseListener(mouseHandler);

        mainSwatchKeyListener = new MainSwatchKeyListener();
        mainPalette.addKeyListener(mainSwatchKeyListener);

        recentSwatchKeyListener = new RecentSwatchKeyListener();
        recentPalette.addKeyListener(recentSwatchKeyListener);

        mainPaletteHolder.setLayout(new BorderLayout());
        mainPaletteHolder.add(mainPalette, BorderLayout.CENTER);
        mainPaletteHolder.setInheritsPopupMenu(true);

        recentPaletteHolder.setLayout(new RecentPanelLayout());
        recentPaletteHolder.add(label);
        recentPaletteHolder.add(recentPalette);
        recentPaletteHolder.setInheritsPopupMenu(true);

        JPanel main = new JPanel();
        main.add(mainPaletteHolder);
        main.add(recentPaletteHolder);

        this.add(main);
    }

    /**
     * This method removes the chooser panel from the JColorChooser.
     *
     * @param chooser The JColorChooser this panel is being removed from.
     */
    @Override public void uninstallChooserPanel(JColorChooser chooser) {
        mainPalette.removeMouseListener(mouseHandler);
        mainPalette.removeKeyListener(mainSwatchKeyListener);

        recentPalette.removeMouseListener(mouseHandler);
        recentPalette.removeKeyListener(recentSwatchKeyListener);

        recentPalette = null;
        mainPalette = null;
        mouseHandler = null;
        mainSwatchKeyListener = null;
        recentSwatchKeyListener = null;
        removeAll();
        super.uninstallChooserPanel(chooser);
    }

    /**
     * This method returns the JTabbedPane displayed name.
     *
     * @return The name displayed in the JTabbedPane.
     */
    @Override public String getDisplayName() {
        return "Swatches";
    }

    /**
     * This method returns the small display icon.
     *
     * @return The small display icon.
     */
    @Override public Icon getSmallDisplayIcon() {
        return null;
    }

    /**
     * This method returns the large display icon.
     *
     * @return The large display icon.
     */
    @Override public Icon getLargeDisplayIcon() {
        return null;
    }

    /**
     * This method paints the chooser panel with the given Graphics object.
     *
     * @param g The Graphics object to paint with.
     */
    @Override public void paint(Graphics g) {
        super.paint(g);
    }

    /**
     * This method returns the tooltip text for the given MouseEvent.
     *
     * @param e The MouseEvent.
     *
     * @return The tooltip text.
     */
    @Override public String getToolTipText(MouseEvent e) {
        return null;
    }

    /**
     * Set the color tracking object.
     *
     * @param tracker
     */
    public void setColorTracker(ColorTracker tracker) {
        this.tracker = tracker;
        if (tracker != null) {
            tracker.addPropertyChangeListener("colors", this);
        }
        updateRecentSwatchPanel();
    }

    @Override public void propertyChange(PropertyChangeEvent evt) {
//        logger.trace("old='{}' new='{}'", evt.getOldValue(), evt.getNewValue());
//        updateRecentSwatchPanel();
    }

    /**
     * A method updating the recent colors in the swatchPanel
     * This is called whenever necessary, specifically after building the panel,
     * on changes of the tracker, from the mouseListener
     */
    protected void updateRecentSwatchPanel() {
        if (recentPalette != null) {
            recentPalette.addColorsToQueue(tracker != null ? tracker.getColors() : null);
        }
    }

    /**
     * This class is used to save and restore the recent color choices..
     */
    public static class ColorTracker extends AbstractBean implements ActionListener {

        /** The list of recent {@link Color Colors}. */
        private List<Color> colors = new ArrayList<>();

        /**
         * Add a {@link Color} to the list of recent color choices. This method
         * will fire off a {@literal "colors"} property change.
         *
         * @param color {@code Color} to be added.
         */
        public void addColor(Color color) {
            List<Color> old = getColors();
            colors.add(0, color);
            firePropertyChange("colors", old, getColors());
        }

        /**
         * Set the list of recent color choices. This method is what should be
         * called when {@literal "restoring"} the recent colors panel.
         *
         * <p>This method will fire off a {@literal "colors"} property change.
         * </p>
         *
         * @param colors {@code List} of recent color choices. {@code null}
         * is allowed, but will result in {@link #colors} being empty.
         */
        public void setColors(List<Color> colors) {
            List<Color> old = getColors();
            this.colors = new ArrayList<>(colors);
            firePropertyChange("colors", old, getColors());
        }

        /**
         * Get the recent color choices.
         *
         * @return {@link ArrayList} containing the recently picked colors.
         * May be empty.
         */
        public List<Color> getColors() {
            return new ArrayList<>(colors);
        }

        /**
         * Returns the user's last {@link Color} selection.
         *
         * @return Either the last {@code Color} that was selected, or
         * {@code null} if no colors have been selected.
         */
        public Color getMostRecentColor() {
            Color c = null;
            if (!colors.isEmpty()) {
                c = colors.get(0);
            }
            return c;
        }

        /**
         * This method currently does nothing.
         *
         * @param e Ignored.
         */
        @Override public void actionPerformed(ActionEvent e) {
            // noop
        }
    }
}
