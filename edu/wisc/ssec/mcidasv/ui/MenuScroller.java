/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Arrays;
import java.util.EventListener;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;


/**
 * A class that provides scrolling capabilities to a long menu dropdown or
 * popup menu. A number of items can optionally be frozen at the top of the menu.
 * <p>
 * <b>Implementation note:</b>  The default scrolling interval is 150 milliseconds.
 * <p>
 * @author Darryl, https://tips4java.wordpress.com/2009/02/01/menu-scroller/
 * @since 4593
 *
 * MenuScroller.java    1.5.0 04/02/12
 * License: use / modify without restrictions (see https://tips4java.wordpress.com/about/)
 * Heavily modified for JOSM needs =&gt; drop unused features and replace static scrollcount approach by dynamic behaviour
 */
public class MenuScroller {

    private JComponent parent;
    private JPopupMenu menu;
    private Component[] menuItems;
    private MenuScrollItem upItem;
    private MenuScrollItem downItem;
    private final MenuScrollListener menuListener = new MenuScrollListener();
    private final MouseWheelListener mouseWheelListener = new MouseScrollListener();
    private int interval;
    private int topFixedCount;
    private int firstIndex = 0;

    private static final int ARROW_ICON_HEIGHT = 10;

    /**
     * Computes the maximum dimension for a component to fit in screen
     * displaying {@code component}.
     *
     * @param component The component to get current screen info from.
     * Must not be {@code null}
     *
     * @return Maximum dimension for a component to fit in current screen.
     *
     * @throws NullPointerException if {@code component} is {@code null}.
     */
    public static Dimension getMaxDimensionOnScreen(JComponent parent, JComponent component) {
        Objects.requireNonNull(component, "component");
        // Compute max dimension of current screen
        Dimension result = new Dimension();
        GraphicsConfiguration gc = component.getGraphicsConfiguration();
        if ((gc == null) && (parent != null)) {
            gc = parent.getGraphicsConfiguration();
        }
        if (gc != null) {
            // Max displayable dimension (max screen dimension - insets)
            Rectangle bounds = gc.getBounds();
            Insets insets = component.getToolkit().getScreenInsets(gc);
            result.width  = bounds.width  - insets.left - insets.right;
            result.height = bounds.height - insets.top - insets.bottom;
        }
        return result;
    }



    private int computeScrollCount(int startIndex) {
        int result = 15;
        if (menu != null) {
            // Compute max height of current screen
//            Component parent = IdvWindow.getActiveWindow().getFrame();
            int maxHeight = getMaxDimensionOnScreen(parent, menu).height - parent.getInsets().top;

            // Remove top fixed part height
            if (topFixedCount > 0) {
                for (int i = 0; i < topFixedCount; i++) {
                    maxHeight -= menuItems[i].getPreferredSize().height;
                }
                maxHeight -= new JSeparator().getPreferredSize().height;
            }

            // Remove height of our two arrow items + insets
            maxHeight -= menu.getInsets().top;
            maxHeight -= upItem.getPreferredSize().height;
            maxHeight -= downItem.getPreferredSize().height;
            maxHeight -= menu.getInsets().bottom;

            // Compute scroll count
            result = 0;
            int height = 0;
            for (int i = startIndex; (i < menuItems.length) && (height <= maxHeight); i++, result++) {
                height += menuItems[i].getPreferredSize().height;
            }

            if (height > maxHeight) {
                // Remove extra item from count
                result--;
            } else {
                // Increase scroll count to take into account upper items that will be displayed
                // after firstIndex is updated
                for (int i = startIndex-1; (i >= 0) && (height <= maxHeight); i--, result++) {
                    height += menuItems[i].getPreferredSize().height;
                }
                if (height > maxHeight) {
                    result--;
                }
            }
        }
        return result;
    }

    /**
     * Registers a menu to be scrolled with the default scrolling interval.
     *
     * @param menu Menu to
     * @return the MenuScroller
     */
    public static MenuScroller setScrollerFor(JMenu menu) {
        return new MenuScroller(menu);
    }

    /**
     * Registers a popup menu to be scrolled with the default scrolling interval.
     *
     * @param menu the popup menu
     * @return the MenuScroller
     */
    public static MenuScroller setScrollerFor(JPopupMenu menu) {
        return new MenuScroller(menu);
    }

    /**
     * Registers a menu to be scrolled, with the specified scrolling interval.
     *
     * @param menu the menu
     * @param interval the scroll interval, in milliseconds
     * @return the MenuScroller
     * @throws IllegalArgumentException if scrollCount or interval is 0 or negative
     */
    public static MenuScroller setScrollerFor(JMenu menu, int interval) {
        return new MenuScroller(menu, interval);
    }

    /**
     * Registers a popup menu to be scrolled, with the specified scrolling interval.
     *
     * @param menu the popup menu
     * @param interval the scroll interval, in milliseconds
     * @return the MenuScroller
     * @throws IllegalArgumentException if scrollCount or interval is 0 or negative
     */
    public static MenuScroller setScrollerFor(JPopupMenu menu, int interval) {
        return new MenuScroller(menu, interval);
    }

    /**
     * Registers a menu to be scrolled, with the specified scrolling interval,
     * and the specified numbers of items fixed at the top of the menu.
     *
     * @param menu the menu
     * @param interval the scroll interval, in milliseconds
     * @param topFixedCount the number of items to fix at the top.  May be 0.
     * @throws IllegalArgumentException if scrollCount or interval is 0 or
     * negative or if topFixedCount is negative
     * @return the MenuScroller
     */
    public static MenuScroller setScrollerFor(JMenu menu, int interval, int topFixedCount) {
        return new MenuScroller(menu, interval, topFixedCount);
    }

    /**
     * Registers a popup menu to be scrolled, with the specified scrolling interval,
     * and the specified numbers of items fixed at the top of the popup menu.
     *
     * @param menu the popup menu
     * @param interval the scroll interval, in milliseconds
     * @param topFixedCount the number of items to fix at the top. May be 0
     * @throws IllegalArgumentException if scrollCount or interval is 0 or
     * negative or if topFixedCount is negative
     * @return the MenuScroller
     */
    public static MenuScroller setScrollerFor(JPopupMenu menu, int interval, int topFixedCount) {
        return new MenuScroller(menu, interval, topFixedCount);
    }

    /**
     * Constructs a {@code MenuScroller} that scrolls a menu with the
     * default scrolling interval.
     *
     * @param menu the menu
     * @throws IllegalArgumentException if scrollCount is 0 or negative
     */
    public MenuScroller(JMenu menu) {
        this(menu, 150);
    }

    public MenuScroller(JComponent parentComp, JMenu menu) {
        this(menu, 150);
        parent = parentComp;
    }

    /**
     * Constructs a {@code MenuScroller} that scrolls a popup menu with the
     * default scrolling interval.
     *
     * @param menu the popup menu
     * @throws IllegalArgumentException if scrollCount is 0 or negative
     */
    public MenuScroller(JPopupMenu menu) {
        this(menu, 150);
    }

    /**
     * Constructs a {@code MenuScroller} that scrolls a menu with the
     * specified scrolling interval.
     *
     * @param menu the menu
     * @param interval the scroll interval, in milliseconds
     * @throws IllegalArgumentException if scrollCount or interval is 0 or negative
     */
    public MenuScroller(JMenu menu, int interval) {
        this(menu, interval, 0);
    }

    /**
     * Constructs a {@code MenuScroller} that scrolls a popup menu with the
     * specified scrolling interval.
     *
     * @param menu the popup menu
     * @param interval the scroll interval, in milliseconds
     * @throws IllegalArgumentException if scrollCount or interval is 0 or negative
     */
    public MenuScroller(JPopupMenu menu, int interval) {
        this(menu, interval, 0);
    }

    public MenuScroller(JComponent parentComp, JMenu menu, int interval) {
        this(menu, interval, 0);
        parent = parentComp;
    }

    /**
     * Constructs a {@code MenuScroller} that scrolls a menu with the
     * specified scrolling interval, and the specified numbers of items fixed at
     * the top of the menu.
     *
     * @param menu the menu
     * @param interval the scroll interval, in milliseconds
     * @param topFixedCount the number of items to fix at the top.  May be 0
     * @throws IllegalArgumentException if scrollCount or interval is 0 or
     * negative or if topFixedCount is negative
     */
    public MenuScroller(JMenu menu, int interval, int topFixedCount) {
        this(menu.getPopupMenu(), interval, topFixedCount);
    }

    public MenuScroller(JComponent parentComp, JMenu menu, int interval, int topFixedCount) {
        this(menu.getPopupMenu(), interval, topFixedCount);
        parent = parentComp;
    }

    /**
     * Constructs a {@code MenuScroller} that scrolls a popup menu with the
     * specified scrolling interval, and the specified numbers of items fixed at
     * the top of the popup menu.
     *
     * @param menu the popup menu
     * @param interval the scroll interval, in milliseconds
     * @param topFixedCount the number of items to fix at the top.  May be 0
     * @throws IllegalArgumentException if scrollCount or interval is 0 or
     * negative or if topFixedCount is negative
     */
    public MenuScroller(JPopupMenu menu, int interval, int topFixedCount) {
        if (interval <= 0) {
            throw new IllegalArgumentException("interval must be greater than 0");
        }
        if (topFixedCount < 0) {
            throw new IllegalArgumentException("topFixedCount cannot be negative");
        }

        upItem = new MenuScrollItem(MenuIcon.UP, -1);
        downItem = new MenuScrollItem(MenuIcon.DOWN, +1);
        setInterval(interval);
        setTopFixedCount(topFixedCount);

        this.menu = menu;
        menu.addPopupMenuListener(menuListener);
        menu.addMouseWheelListener(mouseWheelListener);
    }

    /**
     * Returns the scroll interval in milliseconds
     *
     * @return the scroll interval in milliseconds
     */
    public int getInterval() {
        return interval;
    }

    /**
     * Sets the scroll interval in milliseconds
     *
     * @param interval the scroll interval in milliseconds
     * @throws IllegalArgumentException if interval is 0 or negative
     */
    public void setInterval(int interval) {
        if (interval <= 0) {
            throw new IllegalArgumentException("interval must be greater than 0");
        }
        upItem.setInterval(interval);
        downItem.setInterval(interval);
        this.interval = interval;
    }

    /**
     * Returns the number of items fixed at the top of the menu or popup menu.
     *
     * @return the number of items
     */
    public int getTopFixedCount() {
        return topFixedCount;
    }

    /**
     * Sets the number of items to fix at the top of the menu or popup menu.
     *
     * @param topFixedCount the number of items
     */
    public void setTopFixedCount(int topFixedCount) {
        if (firstIndex <= topFixedCount) {
            firstIndex = topFixedCount;
        } else {
            firstIndex += (topFixedCount - this.topFixedCount);
        }
        this.topFixedCount = topFixedCount;
    }

    /**
     * Removes this MenuScroller from the associated menu and restores the
     * default behavior of the menu.
     */
    public void dispose() {
        if (menu != null) {
            menu.removePopupMenuListener(menuListener);
            menu.removeMouseWheelListener(mouseWheelListener);
            menu.setPreferredSize(null);
            menu = null;
        }
    }

    public void resetMenu() {
        menuItems = menu.getComponents();
        refreshMenu();
    }

    public void setParent(JComponent parent) {
        this.parent = parent;
    }

    private void refreshMenu() {
        if ((menuItems != null) && (menuItems.length > 0)) {

            int allItemsHeight = Arrays.stream(menuItems).mapToInt(item -> item.getPreferredSize().height).sum();
            int allowedHeight = getMaxDimensionOnScreen(parent, menu).height - parent.getInsets().top;
            boolean mustScroll = allItemsHeight > allowedHeight;

            if (mustScroll) {
                firstIndex = Math.min(menuItems.length-1, Math.max(topFixedCount, firstIndex));
                int scrollCount = computeScrollCount(firstIndex);
                firstIndex = Math.min(menuItems.length - scrollCount, firstIndex);

                upItem.setEnabled(firstIndex > topFixedCount);
                downItem.setEnabled((firstIndex + scrollCount) < menuItems.length);

                menu.removeAll();
                for (int i = 0; i < topFixedCount; i++) {
                    menu.add(menuItems[i]);
                }
                if (topFixedCount > 0) {
                    menu.addSeparator();
                }

                menu.add(upItem);
                for (int i = firstIndex; i < (scrollCount + firstIndex); i++) {
                    menu.add(menuItems[i]);
                }
                menu.add(downItem);

                int preferredWidth = 0;
                for (Component item : menuItems) {
                    preferredWidth = Math.max(preferredWidth, item.getPreferredSize().width);
                }
                menu.setPreferredSize(new Dimension(preferredWidth, menu.getPreferredSize().height));

            } else if (!Arrays.equals(menu.getComponents(), menuItems)) {
                // Scroll is not needed but menu is not up to date
                menu.removeAll();
                for (Component item : menuItems) {
                    menu.add(item);
                }
            }

            menu.revalidate();
            menu.repaint();
        }
    }

    private class MenuScrollListener implements PopupMenuListener {

        @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            setMenuItems();
        }

        @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            // this does the menu.removeAll() that makes it possible to reuse the scroller buttons.
            restoreMenuItems();
            //setMenuItems();
        }


        @Override public void popupMenuCanceled(PopupMenuEvent e) {
            //restoreMenuItems();
            setMenuItems();
        }

        private void setMenuItems() {
            menuItems = menu.getComponents();
            refreshMenu();
        }

        private void restoreMenuItems() {
            menu.removeAll();
            for (Component component : menuItems) {
                menu.add(component);
            }
        }
    }

    private class MenuScrollTimer extends Timer {
        public MenuScrollTimer(final int increment, int interval) {
            super(interval, e -> {
                firstIndex += increment;
                refreshMenu();
            });
        }
    }

    private class MenuScrollItem extends JMenuItem
        implements ChangeListener {

        private MenuScrollTimer timer;

        public MenuScrollItem(MenuIcon icon, int increment) {
            setIcon(icon);
            setDisabledIcon(icon);
            timer = new MenuScrollTimer(increment, interval);
            addChangeListener(this);
        }

        public void setInterval(int interval) {
            timer.setDelay(interval);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (isArmed() && !timer.isRunning()) {
                timer.start();
            }
            if (!isArmed() && timer.isRunning()) {
                timer.stop();
            }
        }
    }

    private static enum MenuIcon implements Icon {

        UP(9, 1, 9),
        DOWN(1, 9, 1);
        static final int[] XPOINTS = {1, 5, 9};
        final int[] yPoints;

        MenuIcon(int... yPoints) {
            this.yPoints = yPoints;
        }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Dimension size = c.getSize();
            Graphics g2 = g.create((size.width / 2) - 5, (size.height / 2) - 5, 10, 10);
            g2.setColor(Color.GRAY);
            g2.drawPolygon(XPOINTS, yPoints, 3);
            if (c.isEnabled()) {
                g2.setColor(Color.BLACK);
                g2.fillPolygon(XPOINTS, yPoints, 3);
            }
            g2.dispose();
        }

        @Override public int getIconWidth() {
            return 0;
        }

        @Override public int getIconHeight() {
            return ARROW_ICON_HEIGHT;
        }
    }

    private class MouseScrollListener implements MouseWheelListener {
        @Override public void mouseWheelMoved(MouseWheelEvent mwe) {
            firstIndex += mwe.getWheelRotation();
            refreshMenu();
            mwe.consume();
        }
    }
}
