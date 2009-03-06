package edu.wisc.ssec.mcidasv.control;

import java.awt.Component;
import java.awt.Container;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;

import ucar.unidata.util.GuiUtils;
import visad.VisADException;

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
