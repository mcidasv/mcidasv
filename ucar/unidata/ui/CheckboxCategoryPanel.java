/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.ui;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.bushe.swing.event.EventBus;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Resource;

/**
 * Class CheckboxCategoryPanel  holds the checkboxes under a category
 * 
 * @author IDV Development Team
 * @version $Revision$
 */
public class CheckboxCategoryPanel extends JPanel implements ChangeListener {

    /** Toggle icon used to show open categories and legend details */
    public static ImageIcon categoryOpenIcon = new ImageIcon(Resource.getImage("/auxdata/ui/icons/CategoryOpen.gif"));

    /** Toggle icon used to show closed categories and legend details */
    public static ImageIcon categoryClosedIcon = new ImageIcon(Resource.getImage("/auxdata/ui/icons/CategoryClosed.gif"));

    /** Are we currently in checkVisCbx */
    private boolean checking = false;

    /** Visibility of the panel. */
    private boolean panelOpen;

    /** Panel's name. */
    private String catName;

    /** The list of checkboxes */
    private List<JCheckBox> items = new ArrayList<JCheckBox>();

    /** The visibility checkbox */
    private JCheckBox visCbx;

    /** The toggle button */
    private JButton toggleBtn;

    /** font */
    private Font normalFont;

    /** font used when we have at least one child box on but not all of them on */
    private Font specialFont;

    /**
     * Create me
     *
     * @param catName The name of the category
     * @param visible Is it initially visible
     */
    public CheckboxCategoryPanel(final String catName, final boolean visible) {
        this.catName = catName;
        setLayout(new GridLayout(0, 1, 0, 0));
        setVisible(visible);
        panelOpen = visible;
        final CheckboxCategoryPanel theCatPanel = this;
        toggleBtn = GuiUtils.getImageButton(categoryClosedIcon);
        toggleBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (theCatPanel.isVisible()) {
                    theCatPanel.setVisible(false);
                    toggleBtn.setIcon(categoryClosedIcon);
                    panelOpen = false;
                } else {
                    theCatPanel.setVisible(true);
                    toggleBtn.setIcon(categoryOpenIcon);
                    panelOpen = true;
                }
                EventBus.publish("CheckboxCategoryPanel.PanelToggled", theCatPanel);
            }
        });

        visCbx = new JCheckBox(catName);
        visCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                toggleAll(visCbx.isSelected());
            }
        });
        normalFont  = visCbx.getFont();
        specialFont = normalFont.deriveFont(Font.ITALIC | Font.BOLD);
    }

    /**
     * Name of the current category.
     */
    public String getCategoryName() {
        return catName;
    }

    /**
     * Is the category panel currently visible?
     */
    public boolean isOpen() {
        return panelOpen;
    }

    /**
     * Add the given item into the list of children
     *
     * @param box The item. Cannot be {@code null}.
     */
    public void addItem(final JCheckBox box) {
        items.add(box);
        box.addChangeListener(this);
        checkVisCbx();
    }

    /**
     * Returns list of {@link javax.swing.JCheckBox JCheckBoxes} that belong to
     * the current category.
     * 
     * @return List containing {@link #items}.
     */
    public List<JCheckBox> getItems() {
        return new ArrayList<JCheckBox>(items);
    }

    /**
     * Replaces existing {@link javax.swing.JCheckBox JCheckBoxes} with the 
     * elements of {@code newItems}.
     * 
     * @param newItems New items for this category. Cannot be {@code null}.
     */
    public void setItems(final Collection<JCheckBox> newItems) {
        items.clear();
        items.addAll(newItems);
        checkVisCbx();
    }

    /**
     * handle change event
     *
     * @param e event
     */
    public void stateChanged(final ChangeEvent e) {
        checkVisCbx();
    }

    /**
     * Create and return the top panel. That is, the one that holds
     * the toggle button, vis checkbox and the label.
     *
     * @return The top panel
     */
    public JPanel getTopPanel() {
        return GuiUtils.hbox(Misc.newList(toggleBtn, visCbx));
    }

    /**
     * Turn on/off all of the checkboxes held under this category
     *
     * @param toWhat What do we turn the checkboxes to
     */
    public void toggleAll(final boolean toWhat) {
        visCbx.setSelected(toWhat);
        for (JCheckBox item : items) {
            item.setSelected(toWhat);
        }
        checkVisCbx();
    }

    /**
     * Turn on the vis checkbox if all sub elements are on
     */
    public void checkVisCbx() {
        if (checking) {
            return;
        }
        checking = true;
        boolean anyOn = false;
        boolean allOn = true;

        for (JCheckBox cbx : items) {
            if (cbx.isSelected()) {
                anyOn = true;
            } else {
                allOn = false;
            }
        }

        visCbx.setSelected(anyOn);
        if (anyOn) {
            if (allOn) {
                visCbx.setFont(normalFont);
            } else {
                visCbx.setFont(specialFont);
            }
        } else {
            visCbx.setFont(normalFont);
        }
        checking = false;
    }
}

