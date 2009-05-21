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
package edu.wisc.ssec.mcidasv;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JSeparator;

import ucar.unidata.data.DataSource;
import ucar.unidata.data.DescriptorDataSource;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.GuiUtils;

public class JythonManager extends ucar.unidata.idv.JythonManager {

    /**
     * Create the manager and call initPython.
     *
     * @param idv The IDV
     */
    public JythonManager(IntegratedDataViewer idv) {
        super(idv);
    }

    /**
     *  Return the list of menu items to use when the user has clicked on a formula DataSource.
     *
     * @param dataSource The data source clicked on
     * @return List of menu items
     */
    public List doMakeFormulaDataSourceMenuItems(DataSource dataSource) {
        List menuItems = new ArrayList();
        menuItems.add(GuiUtils.makeMenuItem("Create Formula", this,
                                            "showFormulaDialog"));
        if (dataSource instanceof DescriptorDataSource) {
            menuItems.add(
                GuiUtils.makeMenu(
                    "Edit Formulas",
                    doMakeEditMenuItems((DescriptorDataSource) dataSource)));
        }
        else {
            menuItems.add(
                    GuiUtils.makeMenu(
                        "Edit Formulas",
                        doMakeEditMenuItems()));
        }
        menuItems.add(GuiUtils.MENU_SEPARATOR);
        menuItems.add(GuiUtils.makeMenuItem("Jython Library", this,
                                            "showJythonEditor"));
        menuItems.add(GuiUtils.makeMenuItem("Jython Shell", this,
        									"createShell"));
        menuItems.add(GuiUtils.MENU_SEPARATOR);
        menuItems.add(GuiUtils.makeMenuItem("Import", this,
                                            "importFormulas"));
        menuItems.add(GuiUtils.makeMenuItem("Export", this,
                                            "exportFormulas"));
        return menuItems;
    }

}
