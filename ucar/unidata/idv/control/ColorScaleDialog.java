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

package ucar.unidata.idv.control;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.ui.ColorSwatchComponent;

import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.ui.FontSelector;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.visad.display.ColorScaleInfo;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A JFrame widget to get color scale info from the user.
 *
 * The code to handle button events and actions is
 * in the event Listeners appearing in the constructor.
 *
 * @author Unidata Development Team
 */
public class ColorScaleDialog implements ActionListener {

    /** Was the setting successful */
    private boolean ok;

    /** the UI contents */
    private JPanel contents;

    /** The dialog we show in */
    private JDialog dialog;

    /** Holds the state */
    private ColorScaleInfo myInfo;

    /** combobox for the placement */
    private JComboBox<String> placementBox;

    /** combobox for the label color */
    private ColorSwatchComponent colorSwatch;

    /** checkbox for visibility */
    private JCheckBox visibilityCbx;

    /** checkbox for unit display */
    private JCheckBox unitCbx;

    /** checkbox for label visibility */
    private JCheckBox labelVisibilityCbx;

    /** checkbox for including transparency */
    private JCheckBox alphaCbx;

    /** The display */
    private DisplayControlImpl displayControl;

    /** The Font selector */
    private FontSelector fontSelector = null;

    /** list of orientations */
    private final static String[] positions = new String[] {
        ColorScaleInfo.TOP,
        ColorScaleInfo.BOTTOM,
        ColorScaleInfo.LEFT,
        ColorScaleInfo.RIGHT
    };

    /**
     * Construct the widget.
     * with interval, min, max entry boxes
     * and ok and cancel buttons.
     *
     * @param displayControl The display
     * @param title  title for frame
     * @param info   the color scale info
     * @param showDialog  true to show the dialog
     */
    public ColorScaleDialog(DisplayControlImpl displayControl, String title,
                            ColorScaleInfo info, boolean showDialog) {
        ok                  = false;
        this.displayControl = displayControl;


        myInfo              = new ColorScaleInfo(info);

        if (showDialog) {
            dialog = GuiUtils.createDialog(((displayControl != null)
                                            ? displayControl.getWindow()
                                            : null), title, true);
        }

        doMakeContents(showDialog);
        String place = myInfo.getPlacement();
        // account for old bundles
        if (place != null) {
            placementBox.setSelectedItem(place);
        }

        visibilityCbx.setSelected(myInfo.getIsVisible());
        unitCbx.setSelected(myInfo.isUnitVisible());
        labelVisibilityCbx.setSelected(myInfo.getLabelVisible());
        alphaCbx.setSelected(myInfo.getUseAlpha());

        if (showDialog) {
            dialog.setVisible(true);
        }


    }

    /**
     * Get the main contents of the dialog
     *
     * @return  the contents
     */
    public JComponent getContents() {
        return contents;
    }

    /**
     * Dispose of the dialog
     */
    protected void dispose() {
        displayControl = null;
        if (dialog != null) {
            dialog.dispose();
            dialog = null;
        }
    }

    /**
     * Apply the state to the display
     *
     * @return Was this successful
     */
    protected boolean doApply() {

        String place = (String) placementBox.getSelectedItem();
        if (place != null) {
            myInfo.setPlacement(place);
        }
        myInfo.setLabelColor(colorSwatch.getSwatchColor());
        myInfo.setIsVisible(visibilityCbx.isSelected());
        myInfo.setUnitVisible(unitCbx.isSelected());
        myInfo.setLabelVisible(labelVisibilityCbx.isSelected());
        myInfo.setUseAlpha(alphaCbx.isSelected());
        myInfo.setLabelFont(fontSelector.getFont());
        try {
            if (displayControl != null) {
                if (displayControl.getColorUnit() != null) {
                    // mjh inq 1925: use colorUnit instead of displayUnit
                    // if available; this fixes unit label when using the
                    // "color by another value" displays.
                    myInfo.setUnit(displayControl.getColorUnit());
                } else {
                    myInfo.setUnit(displayControl.getDisplayUnit());
                }
                displayControl.setColorScaleInfo(
                    new ColorScaleInfo(getInfo()));
            }
            return true;
        } catch (Exception exc) {
            LogUtil.logException("Setting color scale info", exc);
            return false;
        }
    }

    /**
     * Initialize the contents
     *
     * @param showDialog  true to show the dialog
     */

    private void doMakeContents(boolean showDialog) {
        placementBox = new JComboBox<String>(positions);
        IdvObjectStore ios = null;
        if (displayControl != null) {
            ios = displayControl.getIdv().getStore();
        }
        colorSwatch  = new ColorSwatchComponent(ios, myInfo.getLabelColor(),
                "Color Scale Label Color");
        colorSwatch.setPreferredSize(Constants.DEFAULT_COLOR_PICKER_SIZE);
        final JComponent colorComp = colorSwatch.getSetPanel();
        visibilityCbx = new JCheckBox("", myInfo.getIsVisible());
        unitCbx       = new JCheckBox("Show Unit", myInfo.isUnitVisible());
        alphaCbx      = new JCheckBox("", myInfo.getUseAlpha());
        fontSelector  = new FontSelector(FontSelector.COMBOBOX_UI, false, false);
        fontSelector.setFont(myInfo.getLabelFont());

        labelVisibilityCbx = new JCheckBox("Visible",
                                           myInfo.getLabelVisible());
        labelVisibilityCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean showLabel = ((JCheckBox) e.getSource()).isSelected();
                GuiUtils.enableTree(fontSelector.getComponent(), showLabel);
                GuiUtils.enableTree(colorComp, showLabel);
            }
        });
        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);

        JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fontPanel.add(new JLabel("Font: "));
        fontPanel.add(fontSelector.getComponent());

        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        colorPanel.add(new JLabel("Color: "));
        colorPanel.add(colorSwatch);

        contents = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Visible: "), visibilityCbx,
            GuiUtils.rLabel("Position: "),
            GuiUtils.leftRight(placementBox, GuiUtils.filler()),
            GuiUtils.rLabel("Labels: "),
            GuiUtils.leftRight(labelVisibilityCbx, GuiUtils.filler()),
            GuiUtils.filler(),
            GuiUtils.leftRight(unitCbx,GuiUtils.filler()),
            GuiUtils.filler(),
            GuiUtils.left(fontPanel),
            GuiUtils.filler(),
            GuiUtils.left(colorPanel),
            GuiUtils.filler(),
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
        contents = GuiUtils.leftRight(contents, GuiUtils.filler());
        if (showDialog) {
            JPanel buttons;
            if (displayControl != null) {
                buttons = GuiUtils.makeApplyOkCancelButtons(this);
            } else {
                buttons = GuiUtils.makeOkCancelButtons(this);
            }
            dialog.getContentPane().add(GuiUtils.centerBottom(contents,
                    buttons));
            GuiUtils.packInCenter(dialog);
        }
    }

    /**
     * Show the dialog box and wait for results and deal with them
     * (ok or cancel).
     * @param evt ActionEvent
     */
    public void actionPerformed(ActionEvent evt) {
        String cmd = evt.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_APPLY)) {
            if ( !doApply()) {
                return;
            }
        }
        if (cmd.equals(GuiUtils.CMD_CANCEL)) {
            ok = false;
        } else if (cmd.equals(GuiUtils.CMD_OK)) {
            ok = true;
        }

        if (cmd.equals(GuiUtils.CMD_CANCEL) || cmd.equals(GuiUtils.CMD_OK)) {
            dialog.setVisible(false);
        }
    }


    /**
     * Get the info
     *
     * @return The info
     */
    public ColorScaleInfo getInfo() {
        return myInfo;
    }


    /**
     * Was ok pressed
     *
     * @return was ok pressed
     */
    public boolean getOk() {
        return ok;
    }


}
