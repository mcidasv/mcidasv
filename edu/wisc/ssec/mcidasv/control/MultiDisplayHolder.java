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
package edu.wisc.ssec.mcidasv.control;

import java.awt.Component;
import java.awt.Container;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;

import visad.VisADException;

import ucar.unidata.util.GuiUtils;

public class MultiDisplayHolder extends ucar.unidata.idv.control.multi.MultiDisplayHolder {

    /**
     * Make the menu bar
     *
     * @return The menu bar
     */
    @Override protected JMenuBar doMakeMenuBar() {
        List<JMenu> menus = doMakeMenuBarMenus(new ArrayList<JMenu>());
        JMenuBar menuBar = new JMenuBar();
        for (int i = 0; i < menus.size(); i++) {
            menuBar.add((JMenu) menus.get(i));
        }
        return menuBar;
    }

    /**
     * Make the UI contents for this control.
     *
     * @return  UI container
     *
     * @throws RemoteException Java RMI error
     * @throws VisADException VisAD Error
     */
    @Override protected Container doMakeContents()
            throws VisADException, RemoteException {
        Container container = super.doMakeContents();
        Component first = container.getComponent(0);
        if ((container.getComponentCount() == 2) && (first instanceof JToolBar)) {
            return GuiUtils.center(container.getComponent(1));
        }
        return container;
    }
}
