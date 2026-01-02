/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
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

package ucar.unidata.idv.ui;


import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.NavigatedViewManager;
import ucar.unidata.idv.control.ReadoutInfo;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.visad.display.Animation;

import visad.Real;
import visad.VisADException;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;


import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseEvent;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.border.BevelBorder;



/**
 *
 * @author IDV development team
 */

public class CursorReadoutWindow {

    /**
     * The navigated view manager
     */
    NavigatedViewManager vm;


    /**
     * The window
     */
    protected JWindow window;

    /**
     * The label
     */
    protected JLabel label;

    /**
     * The window width
     */
    protected int windowWidth = 0;

    /**
     * The last earth location
     */
    private EarthLocation lastEarthLocation;

    /**
     * Ignore missing
     */
    private boolean ignoreMissing = true;


    /**
     * Instantiates a new cursor readout window.
     *
     * @param vm the vm
     */
    public CursorReadoutWindow(NavigatedViewManager vm) {
        this(vm, true);
    }


    /**
     * Instantiates a new cursor readout window.
     *
     * @param vm the vm
     * @param ignoreMissing the ignore missing
     */
    public CursorReadoutWindow(NavigatedViewManager vm,
                               boolean ignoreMissing) {
        this.vm            = vm;
        this.ignoreMissing = ignoreMissing;
        label              = GuiUtils.getFixedWidthLabel("");
    }



    /**
     * Handle mouse released.
     *
     * @param e the e
     */
    public void handleMouseReleased(MouseEvent e) {
        if (window != null) {
            window.dispose();
            window      = null;
            windowWidth = -1;
        }
    }



    /**
     * Sets the window location.
     */
    private void setWindowLocation() {
        JComponent contents   = (JComponent) vm.getContents();
        Rectangle  b          = contents.bounds();
        Point      loc        = contents.getLocationOnScreen();

        // Usable area (excludes taskbar, etc.)
        Rectangle usableBounds = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getMaximumWindowBounds();

        int left = loc.x;
        if (left < usableBounds.x) {
            left = usableBounds.x;
        }

        int right = left + window.getBounds().width;
        if (right > usableBounds.x + usableBounds.width) {
            left = usableBounds.x + usableBounds.width - window.getBounds().width;
        }

        int top = loc.y + b.height;
        int bottom = top + window.getBounds().height;

        if (bottom > usableBounds.y + usableBounds.height) {
            top = usableBounds.y + usableBounds.height - window.getBounds().height - 10;
        }

        window.setLocation(left, top);
    }

    /**
     * Handle mouse pressed or dragged.
     *
     * @param e the e
     * @throws RemoteException the remote exception
     * @throws VisADException the VisAD exception
     */
    public void handleMousePressedOrDragged(MouseEvent e)
            throws RemoteException, VisADException {
        if (window == null) {
            JComponent contents = (JComponent) vm.getContents();
            Window     parent   = GuiUtils.getWindow(contents);
            window = new JWindow(parent);
            window.pack();
            setWindowLocation();
            window.setVisible(true);
        }
        double[] box =
            vm.getNavigatedDisplay().getSpatialCoordinatesFromScreen(
                e.getX(), e.getY());
        lastEarthLocation = vm.getNavigatedDisplay().getEarthLocation(box[0],
                box[1], box[2], true);

        if ((lastEarthLocation.getLongitude().getValue() < -180.0)
                || (lastEarthLocation.getLongitude().getValue() > 180.0)) {
            // re-create the EarthLocationTuple, 
            // ensuring a longitude between -180 and 180
            lastEarthLocation =
                new EarthLocationTuple(lastEarthLocation.getLatitude()
                    .getValue(), Misc
                    .normalizeLongitude(lastEarthLocation.getLongitude()
                        .getValue()), lastEarthLocation.getAltitude()
                            .getValue());
        }

        updateReadout();
    }


    /**
     * Update readout.
     */
    public void updateReadout() {
        if ((lastEarthLocation == null) || (window == null)) {
            return;
        }
        String readout = getReadout(lastEarthLocation, true, false,
                                    new ArrayList<ReadoutInfo>());
        if (readout == null) {
            readout = "";
        }

        final String theReadout = readout;
        GuiUtils.invokeInSwingThread(() -> {
            label.setText(theReadout);
            window.getContentPane().removeAll();
            JComponent wrapper = GuiUtils.inset(label,
                                     new Insets(2, 5, 1, 5));
            wrapper.setBorder(
                BorderFactory.createBevelBorder(BevelBorder.RAISED));
            window.getContentPane().add(wrapper);
            window.pack();
            setWindowLocation();
            window.toFront();
        });
    }




    /**
     * Gets the readout.
     *
     * @param earthLocation the earth location
     * @param showDisplays the show displays
     * @param showAlt the show alt
     * @param samples the samples
     * @return the readout
     */
    public String getReadout(EarthLocation earthLocation,
                             boolean showDisplays, boolean showAlt,
                             List<ReadoutInfo> samples) {
        if (earthLocation == null) {
            return "";
        }
        List         controls  = vm.getControls();
        StringBuffer sb        = new StringBuffer();
        Animation    animation = vm.getAnimation();
        int          step      = animation.getCurrent();
        Real         aniValue  = animation.getAniValue();

        boolean      didone    = false;
    
        if (aniValue == null) {
            // McV Inq 1521: Animation's "aniValue" field is not set by the standard
            // "choose data" -> display data process; either you have to manually 
            // select a time step or sit through your animation loop once.
            // 
            // in fact, it looks as through it would be strictly better to
            // simply always use getCurrentAnimationValue() rather than 
            // getAniValue()...but for now I'll take the less adventurous route.
            aniValue = animation.getCurrentAnimationValue();
        }
        
        try {
            if (showDisplays) {
                for (int i = 0; i < controls.size(); i++) {
                    DisplayControl display = (DisplayControl) controls.get(i);
                    List readout = display.getCursorReadout(earthLocation,
                                       aniValue, step, samples);
                    if ((readout != null) && (readout.size() > 0)) {
                        didone = true;
                        sb.append(StringUtil.join("", readout));
                    }

                }
            }

            if ( !didone) {
                //                window.setVisible(false);
                //                return;
            } else {
                //                window.setVisible(true);
                //                window.toFront();

            }


            String llp = ((earthLocation == null)
                          ? ""
                          : vm.getIdv().getDisplayConventions()
                              .formatEarthLocation(earthLocation, showAlt));
            llp = StringUtil.padRight(llp, 6 * 100, "&nbsp;");

            return "<html>Location: " + llp + (didone
                    ? "<hr>"
                    : "") + "<table cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">"
                          + sb + "</table></html>";
        } catch (Exception exc) {
            vm.logException("Getting cursor readouts", exc);
            return "";
        }
    }



}
