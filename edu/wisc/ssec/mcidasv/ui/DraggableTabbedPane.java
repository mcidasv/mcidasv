/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.metal.MetalTabbedPaneUI;

import org.w3c.dom.Element;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.ui.ComponentGroup;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.xml.XmlUtil;
import edu.wisc.ssec.mcidasv.Constants;

/**
 * This is a rather simplistic drag and drop enabled JTabbedPane. It allows
 * users to use drag and drop to move tabs between windows and reorder tabs.
 */
public class DraggableTabbedPane extends JTabbedPane implements 
    DragGestureListener, DragSourceListener, DropTargetListener, MouseListener,
    MouseMotionListener
{
    private static final long serialVersionUID = -5710302260509445686L;

    /** Local shorthand for the actions we're accepting. */
    private static final int VALID_ACTION = DnDConstants.ACTION_COPY_OR_MOVE;

    /** Path to the icon we'll use as an index indicator. */
    private static final String IDX_ICON = 
        "/edu/wisc/ssec/mcidasv/resources/icons/tabmenu/go-down.png";

    /** 
     * Used to signal across all DraggableTabbedPanes that the component 
     * currently being dragged originated in another window. This'll let McV
     * determine if it has to do a quiet ComponentHolder transfer.
     */
    protected static boolean outsideDrag = false;

    /** The actual image that we'll use to display the index indications. */
    private final Image INDICATOR = 
        (new ImageIcon(getClass().getResource(IDX_ICON))).getImage();

    /** The tab index where the drag started. */
    private int sourceIndex = -1;

    /** The tab index that the user is currently over. */
    private int overIndex = -1;

    /** Used for starting the dragging process. */
    private DragSource dragSource;

    /** Used for signaling that we'll accept drops (registers listeners). */
    private DropTarget dropTarget;

    /** The component group holding our components. */
    private McvComponentGroup group;

    /** The IDV window that contains this tabbed pane. */
    private IdvWindow window;

    /** Keep around this reference so that we can access the UI Manager. */
    private IntegratedDataViewer idv;

    /**
     * Mostly just registers that this component should listen for drag and
     * drop operations.
     * 
     * @param win The IDV window containing this tabbed pane.
     * @param idv The main IDV instance.
     * @param group The {@link McvComponentGroup} that holds this component's tabs.
     */
    public DraggableTabbedPane(IdvWindow win, IntegratedDataViewer idv, McvComponentGroup group) {
        dropTarget = new DropTarget(this, this);
        dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(this, VALID_ACTION, this);

        this.group = group;
        this.idv = idv;
        window = win;

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /**
     * Returns the {@link McvComponentGroup} that represents the window 
     * containing this tabbed pane.
     */
    private McvComponentGroup getGroup() {
        return group;
    }

    /**
     * Triggered when the user does a (platform-dependent) drag initiating 
     * gesture. Used to populate the things that the user is attempting to 
     * drag. 
     */
    public void dragGestureRecognized(DragGestureEvent e) {
        sourceIndex = getSelectedIndex();

        // transferable allows us to store the current DraggableTabbedPane and
        // the source index of the drag inside the various drag and drop event
        // listeners.
        Transferable transferable = new TransferableIndex(this, sourceIndex);

        Cursor cursor = DragSource.DefaultMoveDrop;
        if (e.getDragAction() != DnDConstants.ACTION_MOVE)
            cursor = DragSource.DefaultCopyDrop;

        dragSource.startDrag(e, cursor, transferable, this);
    }

    /** 
     * Triggered when the user drags into <tt>dropTarget</tt>.
     */
    public void dragEnter(DropTargetDragEvent e) {
        DataFlavor[] flave = e.getCurrentDataFlavors();
        if ((flave.length == 0) || !(flave[0] instanceof DraggableTabFlavor))
            return;

        //System.out.print("entered window outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);

        // if the DraggableTabbedPane associated with this drag isn't the 
        // "current" DraggableTabbedPane we're dealing with a drag from another
        // window and we need to make this DraggableTabbedPane aware of that.
        if (((DraggableTabFlavor)flave[0]).getDragTab() != this) {
            //System.out.println(" coming from outside!");
            outsideDrag = true;
        } else {
            //System.out.println(" re-entered parent window");
            outsideDrag = false;
        }
    }

    /**
     * Triggered when the user drags out of {@code dropTarget}.
     */
    public void dragExit(DropTargetEvent e) {
        //      System.out.println("drag left a window outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);
        overIndex = -1;

        //outsideDrag = true;
        repaint();
    }

    /**
     * Triggered continually while the user is dragging over 
     * {@code dropTarget}. McIDAS-V uses this to draw the index indicator.
     * 
     * @param e Information about the current state of the drag.
     */
    public void dragOver(DropTargetDragEvent e) {
        //      System.out.println("dragOver outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);
        if ((!outsideDrag) && (sourceIndex == -1))
            return;

        Point dropPoint = e.getLocation();
        overIndex = indexAtLocation(dropPoint.x, dropPoint.y);

        repaint();
    }

    /**
     * Triggered when a drop has happened over {@code dropTarget}.
     * 
     * @param e State that we'll need in order to handle the drop.
     */
    public void drop(DropTargetDropEvent e) {
        // if the dragged ComponentHolder was dragged from another window we
        // must do a behind-the-scenes transfer from its old ComponentGroup to 
        // the end of the new ComponentGroup.
        if (outsideDrag) {
            DataFlavor[] flave = e.getCurrentDataFlavors();
            DraggableTabbedPane other = ((DraggableTabFlavor)flave[0]).getDragTab();

            ComponentHolder target = other.removeDragged();
            sourceIndex = group.quietAddComponent(target);
            outsideDrag = false;
        }

        // check to see if we've actually dropped something McV understands.
        if (sourceIndex >= 0) {
            e.acceptDrop(VALID_ACTION);
            Point dropPoint = e.getLocation();
            int dropIndex = indexAtLocation(dropPoint.x, dropPoint.y);

            // make sure the user chose to drop over a valid area/thing first
            // then do the actual drop.
            if ((dropIndex != -1) && (getComponentAt(dropIndex) != null))
                doDrop(sourceIndex, dropIndex);

            // clean up anything associated with the current drag and drop
            e.getDropTargetContext().dropComplete(true);
            sourceIndex = -1;
            overIndex = -1;

            repaint();
        }
    }

    /**
     * {@literal "Quietly"} removes the dragged component from its group. If the
     * last component in a group has been dragged out of the group, the 
     * associated window will be killed.
     * 
     * @return The removed component.
     */
    private ComponentHolder removeDragged() {
        ComponentHolder removed = group.quietRemoveComponentAt(sourceIndex);

        // no point in keeping an empty window around... but killing the 
        // window here doesn't properly terminate the drag and drop (as this
        // method is typically called from *another* window).
        return removed;
    }

    /**
     * Moves a component to its new index within the component group.
     * 
     * @param srcIdx The old index of the component.
     * @param dstIdx The new index of the component.
     */
    public void doDrop(int srcIdx, int dstIdx) {
        List<ComponentHolder> comps = group.getDisplayComponents();
        ComponentHolder src = comps.get(srcIdx);

        group.removeComponent(src);
        group.addComponent(src, dstIdx);
    }

    /**
     * Overridden so that McIDAS-V can draw an indicator of a dragged tab's 
     * possible 
     */
    @Override public void paint(Graphics g) {
        super.paint(g);

        if (overIndex == -1)
            return;

        Rectangle bounds = getBoundsAt(overIndex);

        if (bounds != null)
            g.drawImage(INDICATOR, bounds.x-7, bounds.y, null);
    }

    /**
     * Overriden so that McIDAS-V can change the window title upon changing
     * tabs.
     */
    @Override public void setSelectedIndex(int index) {
        super.setSelectedIndex(index);

        // there are only ever component holders in the display comps.
        @SuppressWarnings("unchecked")
        List<ComponentHolder> comps = group.getDisplayComponents();

        ComponentHolder h = comps.get(index);
        String newTitle = 
            UIManager.makeTitle(idv.getStateManager().getTitle(), h.getName());
        if (window != null)
            window.setTitle(newTitle);
    }

    /**
     * Used to simply provide a reference to the originating 
     * DraggableTabbedPane while we're dragging and dropping.
     */
    private static class TransferableIndex implements Transferable {
        private DraggableTabbedPane tabbedPane;

        private int index;

        public TransferableIndex(DraggableTabbedPane dt, int i) {
            tabbedPane = dt;
            index = i;
        }

        // whatever is returned here needs to be serializable. so we can't just
        // return the tabbedPane. :(
        public Object getTransferData(DataFlavor flavor) {
            return index;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { new DraggableTabFlavor(tabbedPane) };
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return true;
        }
    }

    /**
     * To be perfectly honest I'm still a bit fuzzy about DataFlavors. As far 
     * as I can tell they're used like so: if a user dragged an image file on
     * to a toolbar, the toolbar might be smart enough to add the image. If the
     * user dragged the same image file into a text document, the text editor
     * might be smart enough to insert the path to the image or something.
     * 
     * I'm thinking that would require two data flavors: some sort of toolbar
     * flavor and then some sort of text flavor?
     */
    private static class DraggableTabFlavor extends DataFlavor {
        private DraggableTabbedPane tabbedPane;

        public DraggableTabFlavor(DraggableTabbedPane dt) {
            super(DraggableTabbedPane.class, "DraggableTabbedPane");
            tabbedPane = dt;
        }

        public DraggableTabbedPane getDragTab() {
            return tabbedPane;
        }
    }

    /**
     * Handle the user dropping a tab outside of a McV window. This will create
     * a new window and add the dragged tab to the ComponentGroup within the
     * newly created window. The new window is the same size as the origin 
     * window, with the top centered over the location where the user released
     * the mouse.
     * 
     * @param dragged The ComponentHolder that's being dragged around.
     * @param drop The x- and y-coordinates where the user dropped the tab.
     */
    private void newWindowDrag(ComponentHolder dragged, Point drop) {
        //      if ((dragged == null) || (window == null))
        if (dragged == null)
            return;

        UIManager ui = (UIManager)idv.getIdvUIManager();

        try {
            Element skinRoot = XmlUtil.getRoot(Constants.BLANK_COMP_GROUP, getClass());

            // create the new window with visibility off, so we can position 
            // the window in a sensible way before the user has to see it.
            IdvWindow w = ui.createNewWindow(null, false, "McIDAS-V", 
                    Constants.BLANK_COMP_GROUP, 
                    skinRoot, false, null);

            // make the new window the same size as the old and center the 
            // *top* of the window over the drop point.
            int height = window.getBounds().height;
            int width = window.getBounds().width;
            int startX = drop.x - (width / 2);

            w.setBounds(new Rectangle(startX, drop.y, width, height));

            // be sure to add the dragged component holder to the new window.
            ComponentGroup newGroup = 
                (ComponentGroup)w.getComponentGroups().get(0);

            newGroup.addComponent(dragged);

            // let there be a window
            w.setVisible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles what happens at the very end of a drag and drop. Since I could
     * not find a better method for it, tabs that are dropped outside of a McV
     * window are handled with this method.
     */
    public void dragDropEnd(DragSourceDropEvent e) {
        if (!e.getDropSuccess() && e.getDropAction() == 0) {
            newWindowDrag(removeDragged(), e.getLocation());
        }

        // this should probably be the last thing to happen in this method.
        // checks to see if we've got a blank window after a drag and drop; 
        // if so, dispose!
        List<ComponentHolder> comps = group.getDisplayComponents();
        if (comps == null || comps.isEmpty()) {
            window.dispose();
        }
    }

    // required methods that we don't need to implement yet.
    public void dragEnter(DragSourceDragEvent e) { }
    public void dragExit(DragSourceEvent e) { }
    public void dragOver(DragSourceDragEvent e) { }
    public void dropActionChanged(DragSourceDragEvent e) { }
    public void dropActionChanged(DropTargetDragEvent e) { }

    public void mouseClicked(final MouseEvent e) { }

    public void mouseExited(final MouseEvent e) { }

    public void mousePressed(final MouseEvent e) { }

    public void mouseEntered(final MouseEvent e) { }

    public void mouseMoved(final MouseEvent e) { }

    public void mouseDragged(final MouseEvent e) { }

    public void mouseReleased(final MouseEvent e) { }

    @Override public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        super.insertTab(title, icon, component, tip, index);
        super.setTabComponentAt(index, new ButtonTabComponent(this, component));
        revalidate();
        repaint();
    }

    private static final Color unselected = new Color(165, 165, 165);
    private static final Color selected = new Color(225, 225, 225);

    private static final String indexColorMetal = "#AAAAAA";
    private static final String indexColorUglyTabs = "#708090";
    private String currentTabColor = indexColorUglyTabs;

    private String getCurrentTabColor() {
        return currentTabColor;
    }

    // ButtonTabComponent was taken from
    // http://java.sun.com/docs/books/tutorial/uiswing/examples/components/TabComponentsDemoProject/src/components/ButtonTabComponent.java
    public static class ButtonTabComponent extends JPanel {
        private final DraggableTabbedPane pane;
        private final Component component;

        public ButtonTabComponent(final DraggableTabbedPane pane, final Component component) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            if (pane == null)
                throw new NullPointerException("pane cannot be null");
            if (component == null)
                throw new NullPointerException("component cannot be null");

            this.pane = pane;
            this.component = component;
            setOpaque(false);

            JLabel label = new JLabel() {
                public String getText() {
                    int idx = pane.indexOfComponent(component);
                    String title = (idx != -1) ? pane.getTitleAt(idx) : "no title";
                    if (idx < 9)
                        return "<html><font color=\""+pane.getCurrentTabColor()+"\">"+(idx+1)+"</font> "+title+"</html>";
                    else if (idx == 9)
                        return "<html><font color=\""+pane.getCurrentTabColor()+"\">0</font> "+title+"</html>";
                    else
                        return title;
                }
            };

            add(label);
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            JButton button = new TabButton();
            add(button);
            setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        }

        private class TabButton extends JButton implements ActionListener {
            public TabButton() {
                int size = 17;
                setPreferredSize(new Dimension(size, size));
                setToolTipText("close this tab");

                // Make the button looks the same for all L&Fs
                setUI(new BasicButtonUI());

                // Make it transparent
                setContentAreaFilled(false);

                // No need to be focusable
                setFocusable(false);
                setBorder(BorderFactory.createEtchedBorder());
                setBorderPainted(false);

                // Making nice rollover effect
                // we use the same listener for all buttons
                addMouseListener(buttonMouseListener);
                setRolloverEnabled(true);

                // Close the proper tab by clicking the button
                addActionListener(this);
            }

            public void actionPerformed(ActionEvent e) {
                int idx = pane.indexOfTabComponent(ButtonTabComponent.this);
                if (idx == -1)
                    return;

                if (pane.getGroup().destroyDisplay(idx)) {
                    pane.removeTabAt(idx);
                    pane.revalidate();
                    pane.repaint();
                }
            }

            //we don't want to update UI for this button
            public void updateUI() {
            }

            //paint the cross
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                //shift the image for pressed buttons
                if (getModel().isPressed()) {
                    g2.translate(1, 1);
                }
                g2.setStroke(new BasicStroke(2));
                g2.setColor(Color.BLACK);
                if (getModel().isRollover()) {
                    g2.setColor(Color.MAGENTA);
                }
                int delta = 6;
                g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
                g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
                g2.dispose();
            }
        }

        private final static MouseListener buttonMouseListener = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                Component component = e.getComponent();
                if (component instanceof AbstractButton) {
                    AbstractButton button = (AbstractButton) component;
                    button.setBorderPainted(true);
                }
            }

            public void mouseExited(MouseEvent e) {
                Component component = e.getComponent();
                if (component instanceof AbstractButton) {
                    AbstractButton button = (AbstractButton) component;
                    button.setBorderPainted(false);
                }
            }
        };
    }
}
