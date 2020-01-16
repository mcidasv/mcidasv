/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2020
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
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.colorchooser.AbstractColorChooserPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.MenuUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlObjectStore;

import edu.wisc.ssec.mcidasv.Constants;

/**
 * This is largely the same as {@link GuiUtils.ColorSwatch}, but it remembers
 * the user's recently selected colors.
 */
public class ColorSwatchComponent extends JPanel implements PropertyChangeListener {

    /** Logging object. */
    private static final Logger logger =
        LoggerFactory.getLogger(ColorSwatchComponent.class);

    /** Flag for alpha. */
    boolean doAlpha = true;
    
    /** Color of the swatch. */
    Color color;

    /** {@literal "Clear"} button. */
    JButton clearBtn;

    /** {@literal "Set"} button. */
    JButton setBtn;

    /** Label */
    String label;

    /** Application object store. */
    private XmlObjectStore store;

    /**
     * Create a new ColorSwatch for the specified color
     *
     * @param store Application object store. Cannot be {@code null}.
     * @param c Color
     * @param dialogLabel Dialog title.
     */
    public ColorSwatchComponent(XmlObjectStore store, Color c, String dialogLabel) {
        this(store, c, dialogLabel, true);
    }

    /**
     * Create a new color swatch
     *
     * @param store Application object store. Cannot be {@code null}.
     * @param c Color
     * @param dialogLabel Dialog title.
     * @param alphaOk Whether or not to use alpha.
     */
    public ColorSwatchComponent(XmlObjectStore store, Color c, String dialogLabel, boolean alphaOk) {
        this.doAlpha = alphaOk;
        this.color   = c;
        this.label   = dialogLabel;
        this.store   = store;
        setMinimumSize(new Dimension(40, 10));
        setPreferredSize(new Dimension(40, 10));
        setToolTipText("Click to change color");
        setBackground(color);
        setBorder(BorderFactory.createLoweredBevelBorder());

        clearBtn = new JButton("Clear");
        clearBtn.addActionListener(ae -> setBackground(null));

        setBtn = new JButton("Set");
        setBtn.addActionListener(ae -> setColorFromChooser());

        this.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                showColorChooser();
            }
        });
    }

    /**
     * Prompt the user to select a {@link Color} using a dialog box.
     *
     * @param store Application object store. Cannot be {@code null}.
     * @param c Parent component. {@code null} is allowed.
     * @param label Title of the dialog box.
     * @param color Initially selected color. {@code null} will result in
     * either the most recently used color, or {@code Color.WHITE} if there
     * are no persisted colors.
     *
     * @return Either the user's selected {@code Color}, or {@code null} if the
     * user closed the dialog or hit cancel.
     */
    public static Color colorChooserDialog(XmlObjectStore store, Component c, String label, Color color) {
        List<Color> savedColors =
            (List<Color>)store.get(Constants.PROP_RECENT_COLORS);
        if (color == null) {
            if ((savedColors != null) && !savedColors.isEmpty()) {
                color = savedColors.get(0);
            } else {
                color = Color.WHITE;
            }
        }
        ColorSwatchComponent comp = new ColorSwatchComponent(store, color, label);
        JColorChooser chooser = new JColorChooser(comp.getBackground());
        List<AbstractColorChooserPanel> choosers =
            new ArrayList<>(Arrays.asList(chooser.getChooserPanels()));
        choosers.remove(0);
        PersistableSwatchChooserPanel swatch = new PersistableSwatchChooserPanel();
        PersistableSwatchChooserPanel.ColorTracker tracker = new PersistableSwatchChooserPanel.ColorTracker();
        tracker.addPropertyChangeListener("colors", comp);

        if (savedColors != null) {
            tracker.setColors(savedColors);
        }
        swatch.setColorTracker(tracker);
        choosers.add(0, swatch);
        chooser.setChooserPanels(choosers.toArray(new AbstractColorChooserPanel[0]));
        swatch.updateRecentSwatchPanel();
        if (GuiUtils.showOkCancelDialog(null, label, chooser, null)) {
            comp.userSelectedNewColor(chooser.getColor());
        }
        return comp.getBackground();
    }

    /**
     * Show the color chooser
     */
    private void showColorChooser() {
        PersistableSwatchChooserPanel.ColorTracker tracker = new PersistableSwatchChooserPanel.ColorTracker();
        tracker.addPropertyChangeListener("colors", this);
        Color         oldColor    = this.getBackground();
        int           alpha       = oldColor.getAlpha();
        JColorChooser chooser = createChooser(tracker);
        JSlider alphaSlider = new JSlider(0, 255, alpha);

        JComponent contents;
        if (doAlpha) {
            contents =
                LayoutUtil.centerBottom(chooser,
                    LayoutUtil.inset(LayoutUtil.hbox(new JLabel("Transparency:"),
                        alphaSlider), new Insets(5, 5, 5,
                        5)));
        } else {
            contents = chooser;
        }
        if (!GuiUtils.showOkCancelDialog(null, label, contents, null)) {
            return;
        }
        alpha = alphaSlider.getValue();
        Color newColor = chooser.getColor();
        if (newColor != null) {
            newColor = new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), alpha);
            this.userSelectedNewColor(newColor);
        }
    }

    private JColorChooser createChooser(PersistableSwatchChooserPanel.ColorTracker tracker) {
        JColorChooser chooser = new JColorChooser(this.getBackground());
        List<AbstractColorChooserPanel> choosers =
            new ArrayList<>(Arrays.asList(chooser.getChooserPanels()));
        choosers.remove(0);
        PersistableSwatchChooserPanel swatch = new PersistableSwatchChooserPanel();
        List<Color> savedColors;
        if (store != null) {
            savedColors = (List<Color>)store.get(Constants.PROP_RECENT_COLORS);
        } else {
            // don't want to use Collections.emptyList, as the user may still
            // attempt to add colors...they just won't be saved in this case. :(
            savedColors = new ArrayList<>(10);
            logger.warn("'store' field is null! colors cannot be saved between sessions.");
        }
        if (savedColors != null) {
            tracker.setColors(savedColors);
        }
        swatch.setColorTracker(tracker);
        choosers.add(0, swatch);
        chooser.setChooserPanels(choosers.toArray(new AbstractColorChooserPanel[0]));
        swatch.updateRecentSwatchPanel();
        return chooser;
    }

    /**
     * Called from {@link PersistableSwatchChooserPanel} when the user has
     * clicked on a color. This is used to store the list of recent color
     * selections.
     *
     * @param evt Event containing both the old list of colors and the new.
     */
    @Override public void propertyChange(PropertyChangeEvent evt) {
        store.put(Constants.PROP_RECENT_COLORS, evt.getNewValue());
    }

    /**
     * Set color from chooser.
     */
    private void setColorFromChooser() {
        Color newColor = JColorChooser.showDialog(null, label,
            this.getBackground());
        if (newColor != null) {
            this.userSelectedNewColor(newColor);
        }
    }

    /**
     * Get the set button.
     *
     * @return the set button
     */
    public JButton getSetButton() {
        return setBtn;
    }

    /**
     * Get the clear button.
     *
     * @return the clear button
     */
    public JButton getClearButton() {
        return clearBtn;
    }

    /**
     * Get the Color of the swatch.
     *
     * @return the swatch color
     */
    public Color getSwatchColor() {
        return color;
    }

    /**
     * User chose a new color. Set the background. This can be overwritted
     * by client code to act on the color change.
     *
     * @param c color
     */
    public void userSelectedNewColor(Color c) {
        setBackground(c);
    }

    /**
     * Set the background to the color.
     *
     * @param c  Color for background
     */
    @Override public void setBackground(Color c) {
        color = c;
        super.setBackground(c);
    }

    /**
     * Paint this swatch.
     *
     * @param g Graphics
     */
    @Override public void paint(Graphics g) {
        Rectangle b = getBounds();
        if (color != null) {
            g.setColor(Color.black);
            for (int x = 0; x < b.width; x += 4) {
                g.fillRect(x, 0, 2, b.height);
            }
        }

        super.paint(g);
        if (color == null) {
            g.setColor(Color.black);
            g.drawLine(0, 0, b.width, b.height);
            g.drawLine(b.width, 0, 0, b.height);
        }
    }

    /**
     * Get the panel
     *
     * @return the panel
     */
    public JComponent getPanel() {
        return LayoutUtil.hbox(this, clearBtn, 4);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Color getColor() {
        return color;
    }


    /**
     * Get the panel that shows the swatch and the Set button.
     *
     * @return the panel
     */
    public JComponent getSetPanel() {
        List comps    = Misc.newList(this);
        JButton popupBtn = new JButton("Change");
        popupBtn.addActionListener(GuiUtils.makeActionListener(this,
            "popupNameMenu", popupBtn));
        comps.add(popupBtn);
        return LayoutUtil.hbox(comps, 4);
    }


    /**
     * Popup the named list menu
     *
     * @param popupBtn Popup near this button
     */
    public void popupNameMenu(JButton popupBtn) {
        List items = new ArrayList();
        for (int i = 0; i < GuiUtils.COLORNAMES.length; i++) {
            items.add(MenuUtil.makeMenuItem(GuiUtils.COLORNAMES[i], this, "setColorName",
                GuiUtils.COLORNAMES[i]));
        }
        items.add(MenuUtil.MENU_SEPARATOR);
        items.add(MenuUtil.makeMenuItem("Custom", this, "setColorName", "custom"));
        MenuUtil.showPopupMenu(items, popupBtn);
    }

    /**
     * Set the color based on name
     *
     * @param name color name
     */
    public void setColorName(String name) {
        if ("custom".equals(name)) {
            setColorFromChooser();
        } else {
            Color newColor = GuiUtils.decodeColor(name, getBackground());
            if (newColor != null) {
                this.setBackground(newColor);
            }
        }
    }
}
