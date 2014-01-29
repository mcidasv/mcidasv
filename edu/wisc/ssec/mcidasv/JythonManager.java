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
package edu.wisc.ssec.mcidasv;

import static ucar.unidata.util.GuiUtils.makeMenu;
import static ucar.unidata.util.GuiUtils.makeMenuItem;
import static ucar.unidata.util.MenuUtil.MENU_SEPARATOR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.python.util.PythonInterpreter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.util.CollectionHelpers;

import ucar.unidata.data.DataSource;
import ucar.unidata.data.DescriptorDataSource;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.ImageGenerator;
import ucar.unidata.idv.ui.JythonShell;

/**
 * Overrides the IDV's {@link ucar.unidata.idv.JythonManager JythonManager} to 
 * associate a {@link JythonShell} with a given {@code JythonManager}.
 */
public class JythonManager extends ucar.unidata.idv.JythonManager {
    
    /** Trusty logging object. */
    private static final Logger logger = LoggerFactory.getLogger(JythonManager.class);
    
    /** Associated Jython Shell. May be {@code null}. */
    private JythonShell jythonShell;
    
    /**
     * Create the manager and call initPython.
     *
     * @param idv The IDV.
     */
    public JythonManager(IntegratedDataViewer idv) {
        super(idv);
    }
    
    /**
     * Create a Jython shell, if one doesn't already exist. This will also 
     * bring the window {@literal "to the front"} of the rest of the McIDAS-V
     * session.
     * 
     * @return JythonShell object for interactive Jython usage.
     */
    public JythonShell createShell() {
        if (jythonShell == null) {
            jythonShell = new JythonShell(getIdv());
            
        }
        jythonShell.toFront();
        return jythonShell;
    }
    
    /** 
     * Returns the Jython Shell associated with this {@code JythonManager}.
     * 
     * @return Jython Shell being used by this manager. May be {@code null}.
     */
    public JythonShell getShell() {
        return jythonShell;
    }
    
    /**
     * Create and initialize a Jython interpreter.
     * 
     * @return Newly created Jython interpreter.
     */
    @Override public PythonInterpreter createInterpreter() {
        PythonInterpreter interpreter = super.createInterpreter();
        return interpreter;
    }
    
    /**
     * Removes the given interpreter from the list of active interpreters. 
     * 
     * <p>Also attempts to close any Jython Shell associated with the 
     * interpreter.</p>
     * 
     * @param interpreter Interpreter to remove. Should not be {@code null}. 
     */
    @Override public void removeInterpreter(PythonInterpreter interpreter) {
        super.removeInterpreter(interpreter);
        if (jythonShell != null && !jythonShell.isShellResetting() && jythonShell.getInterpreter().equals(interpreter)) {
            jythonShell.close();
            jythonShell = null;
        }
    }
    
    /**
     * Overridden so that McIDAS-V can inject a variable named {@code _idv}
     * into {@code interpreter's} globals.
     * 
     * @param interpreter Jython interpreter being initialized by the IDV. Cannot be {@code null}.
     */
    @Override protected void initBasicInterpreter(PythonInterpreter interpreter) {
        interpreter.set("_idv", getIdv());
        interpreter.set("idv", getIdv());
        super.initBasicInterpreter(interpreter);
    }
    
    /**
     * Overridden so that McIDAS-V can add an {@code islInterpreter} object
     * to the interpreter's locals (before executing the contents of {@code}.
     * 
     * @param code Jython code to evaluate. {@code null} is probably a bad idea.
     * @param properties {@code String->Object} pairs to insert into the 
     * locals. Parameter may be {@code null}.
     */
    @SuppressWarnings("unchecked") // dealing with idv code that predates generics.
    @Override public void evaluateTrusted(String code, Map<String, Object> properties) {
        if (properties == null) {
            properties = CollectionHelpers.newMap();
        }
        if (!properties.containsKey("islInterpreter")) {
            properties.put("islInterpreter", new ImageGenerator(getIdv()));
        }
        if (!properties.containsKey("_idv")) {
            properties.put("_idv", getIdv());
        }
        if (!properties.containsKey("idv")) {
            properties.put("idv", getIdv());
        }
        super.evaluateTrusted(code, properties);
    }
    
    /**
     * Return the list of menu items to use when the user has clicked on a 
     * formula {@link DataSource}.
     * 
     * @param dataSource The data source clicked on.
     * 
     * @return {@link List} of menu items.
     */
    @SuppressWarnings("unchecked") // dealing with idv code that predates generics.
    @Override public List doMakeFormulaDataSourceMenuItems(DataSource dataSource) {
        List menuItems = new ArrayList(100);
        menuItems.add(makeMenuItem("Create Formula", this, "showFormulaDialog"));
        List editItems;
        if (dataSource instanceof DescriptorDataSource) {
            editItems = doMakeEditMenuItems((DescriptorDataSource)dataSource);
        }
        else {
            editItems = doMakeEditMenuItems();
        }
        menuItems.add(makeMenu("Edit Formulas", editItems));
        menuItems.add(MENU_SEPARATOR);
        menuItems.add(makeMenuItem("Jython Library", this, "showJythonEditor"));
        menuItems.add(makeMenuItem("Jython Shell", this, "createShell"));
        menuItems.add(MENU_SEPARATOR);
        menuItems.add(makeMenuItem("Import", this, "importFormulas"));
        menuItems.add(makeMenuItem("Export", this, "exportFormulas"));
        return menuItems;
    }
    
}
