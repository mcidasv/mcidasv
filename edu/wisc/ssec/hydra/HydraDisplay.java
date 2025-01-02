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

package edu.wisc.ssec.hydra;

import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.Dimension;

import visad.MouseBehavior;


public abstract class HydraDisplay implements WindowListener, ComponentListener, MouseListener {

    boolean gotFirstResizeEvent = false;

    // TODO:  need a manager for this someday
    public static Dimension sharedWindowSize = new Dimension(500, 500);

    public void windowClosing(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        if (!gotFirstResizeEvent) gotFirstResizeEvent = true;
    }

    public void componentShown(ComponentEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }


    public static double[] getTranslation(MouseBehavior mouseBehav, double[] matrix) {
        double[] rot_a = new double[3];
        double[] scale_a = new double[3];
        double[] trans_a = new double[3];
        mouseBehav.instance_unmake_matrix(rot_a, scale_a, trans_a, matrix);
        return trans_a;
    }

}
