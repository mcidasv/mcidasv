/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package ucar.unidata.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.ui.ColorSwatchComponent;
import ucar.unidata.gis.maps.MapData;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlObjectStore;

/**
 * Panel to hold map gui items for one map
 */

public class MapPanel extends JPanel {

    /** This holds the data that describes the map */
    private MapData mapData;

    /** The visibility cbx */
    private JCheckBox shownCbx;

    /** The line width box */
    private JComboBox widthBox;

    /** The line style box */
    private JComboBox styleBox;

    /** Shows the color */
    private ColorSwatchComponent colorButton;

    /** The fast rendering  cbx */
    private JCheckBox fastRenderingCbx;

    /** Do we ignore the setVisibility */
    private boolean ignoreAction = false;

    /** Are we updating the UI */
    private boolean updatingUI = false;

    private XmlObjectStore store;

    /** Limit on map label string length */
    private static final int MAP_LABEL_MAX_LENGTH = 35;

    /**
     * Create the MapPanel with the given MapData
     *
     * @param data The MapData we represent
     *
     */
    public MapPanel(XmlObjectStore store, MapData data) {
        this.store = store;
        mapData = data;
        init();
    }

    /**
     * Set the visibility checkbox to the given value.
     * Ignore any actions fired from that
     *
     * @param visiblity The checkbox value
     */
    public void setVisibility(boolean visiblity) {
        ignoreAction = true;
        shownCbx.setSelected(visiblity);
        ignoreAction = false;
    }

    /**
     *  Create the GUI
     */
    private void init() {
        setLayout(new GridLayout(1, 2));
        setAlignmentY(Component.CENTER_ALIGNMENT);
        String name = mapData.getDescription();
        String longName = name;
        boolean truncatedName = false;
        
        // TJJ Apr 2014
        if (name.length() > MAP_LABEL_MAX_LENGTH) {
        	// Pad with ellipses if we exceeded max length
            name = name.substring(0, (MAP_LABEL_MAX_LENGTH - 3));
            name = StringUtil.padRight(name, MAP_LABEL_MAX_LENGTH, ".");
            truncatedName = true;
        }
        
        shownCbx = new JCheckBox(name, mapData.getVisible());

        Font lblFont = shownCbx.getFont();
        Font monoFont = new Font("Monospaced", lblFont.getStyle(),
                                 lblFont.getSize());
        shownCbx.setFont(monoFont);

        // Tooltip for most rows
        shownCbx.setToolTipText("Toggle visibility");
        
        // For truncated rows, tooltip is full description of map name
        if (truncatedName) {
        	shownCbx.setToolTipText(longName);
        }

        shownCbx.setHorizontalAlignment(JCheckBox.LEFT);
        add(shownCbx);
        shownCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (updatingUI) {
                    return;
                }
                if ( !ignoreAction) {
                    mapData.setVisible(shownCbx.isSelected());
                }
            }
        });

        JPanel p = new JPanel();
        p.setAlignmentX(Component.RIGHT_ALIGNMENT);
        p.setAlignmentY(Component.TOP_ALIGNMENT);
        widthBox = new JComboBox(new String[] { "1.0", "1.5", "2.0", "2.5",
                "3.0" });
        widthBox.setToolTipText("Set the line width");
        //        widthBox.setMaximumSize(new Dimension(30, 16));
        widthBox.setPreferredSize(new Dimension(60, 16));
        widthBox.setEditable(true);
        widthBox.setSelectedItem(String.valueOf(mapData.getLineWidth()));
        widthBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mapData.setLineWidth(
                    Float.parseFloat((String) widthBox.getSelectedItem()));
            }
        });
        p.add(GuiUtils.wrap(widthBox));
        styleBox = new JComboBox(new String[] { "_____", "_ _ _", ".....",
                "_._._" });
        styleBox.setMaximumSize(new Dimension(30, 16));
        styleBox.setToolTipText("Set the line style");
        styleBox.setSelectedIndex(mapData.getLineStyle());
        Font f = Font.decode("monospaced-BOLD");
        if (f != null) {
            styleBox.setFont(f);
        }
        styleBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mapData.setLineStyle(styleBox.getSelectedIndex());
            }
        });
        p.add(styleBox);
        colorButton = new ColorSwatchComponent(store, mapData.getColor(),
                "Set Map Line Color");
        colorButton.setToolTipText("Set the line color");
        colorButton.setPreferredSize(Constants.DEFAULT_COLOR_PICKER_SIZE);
        colorButton.addPropertyChangeListener("background",
                new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                Color c = colorButton.getBackground();
                if (c != null) {
                    mapData.setColor(c);
                }
            }
        });

        fastRenderingCbx = new JCheckBox("Fast rendering", mapData.getFastRendering());
        fastRenderingCbx.setToolTipText("Toggle fast rendering");
        fastRenderingCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                mapData.setFastRendering(fastRenderingCbx.isSelected());
            }
        });


        p.add(fastRenderingCbx);


        add(p);
    }







    /**
     * Apply the mapData state to the GUI widgets
     */
    public void updateUI() {
        if (updatingUI || (colorButton == null)) {
            return;
        }
        updatingUI = true;
        colorButton.setBackground(mapData.getColor());
        fastRenderingCbx.setSelected(mapData.getFastRendering());
        shownCbx.setSelected(mapData.getVisible());
        styleBox.setSelectedIndex(mapData.getLineStyle());
        widthBox.setSelectedItem(String.valueOf(mapData.getLineWidth()));
        updatingUI = false;
    }


    /**
     * Get the GUI components
     *
     * @return a list of the components
     */
    public java.util.List getGuiComponents() {
        JPanel colorPanel = new JPanel(new FlowLayout());
        colorPanel.add(colorButton);
        java.util.List comps = new ArrayList();
        comps.add(shownCbx);
        comps.add(widthBox);
        comps.add(styleBox);
        comps.add(colorPanel);
        comps.add(fastRenderingCbx);
        return comps;
    }


    /**
     * Get the MapData we represent
     *
     * @return The map data
     */
    public MapData getMapData() {
        return mapData;
    }
}
