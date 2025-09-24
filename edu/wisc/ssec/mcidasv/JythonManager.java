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
package edu.wisc.ssec.mcidasv;

import static edu.wisc.ssec.mcidasv.McIdasPreferenceManager.PROP_HIQ_FONT_RENDERING;
import static ucar.unidata.util.GuiUtils.makeMenu;
import static ucar.unidata.util.MenuUtil.MENU_SEPARATOR;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.Comparator;

import edu.wisc.ssec.mcidasv.startupmanager.options.BooleanOption;
import org.python.util.PythonInterpreter;

import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster;
import edu.wisc.ssec.mcidasv.util.CollectionHelpers;

import ucar.unidata.data.DataSource;
import ucar.unidata.data.DescriptorDataSource;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.ImageGenerator;
import ucar.unidata.idv.ui.JythonShell;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.Misc;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Overrides the IDV's {@link ucar.unidata.idv.JythonManager JythonManager} to 
 * associate a {@link JythonShell} with a given {@code JythonManager}.
 */
public class JythonManager extends ucar.unidata.idv.JythonManager {
    
//    /** Trusty logging object. */
//    private static final Logger logger = LoggerFactory.getLogger(JythonManager.class);
    
    /** Associated Jython Shell. May be {@code null}. */
    private JythonShell jythonShell;

    private static final Logger logger =
            Logger.getLogger(JythonManager.class.getName());
    
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
        if ((jythonShell != null) && !jythonShell.isShellResetting() && jythonShell.getInterpreter().equals(interpreter)) {
            jythonShell.close();
            jythonShell = null;
        }
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
        properties.putIfAbsent("islInterpreter", new ImageGenerator(getIdv()));
        properties.putIfAbsent("_idv", getIdv());
        properties.putIfAbsent("idv", getIdv());
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
        JMenuItem menuItem;

        menuItem = new JMenuItem("Create Formula");
        menuItem.setToolTipText("Open Formula Editor window");
        menuItem.addActionListener(e -> showFormulaDialog());
        menuItems.add(menuItem);

        List editItems;
        if (dataSource instanceof DescriptorDataSource) {
            editItems = doMakeEditMenuItems((DescriptorDataSource)dataSource);
        } else {
            editItems = doMakeEditMenuItems();
        }
        // Remove any accidental top-level "Edit Formulas" header
        editItems.removeIf(item -> (item instanceof String) && ((String)item).equalsIgnoreCase("Edit Formulas"));

        // Sort within each group
        List<Object> sortedGroupedItems = new ArrayList<>();
        Object currentGroupHeader = null;
        List<JMenuItem> currentGroupItems = new ArrayList<>();

        for (Object item : editItems) {
            if (item instanceof String) {
                if (!currentGroupItems.isEmpty()) {
                    currentGroupItems.sort(Comparator.comparing(JMenuItem::getText, String.CASE_INSENSITIVE_ORDER));
                    if (currentGroupHeader != null) sortedGroupedItems.add(currentGroupHeader);
                    sortedGroupedItems.addAll(currentGroupItems);
                    currentGroupItems.clear();
                }
                currentGroupHeader = item;
            } else if (item instanceof JMenuItem) {
                currentGroupItems.add((JMenuItem)item);
            } else {
                if (!currentGroupItems.isEmpty()) {
                    currentGroupItems.sort(Comparator.comparing(JMenuItem::getText, String.CASE_INSENSITIVE_ORDER));
                    if (currentGroupHeader != null) sortedGroupedItems.add(currentGroupHeader);
                    sortedGroupedItems.addAll(currentGroupItems);
                    currentGroupItems.clear();
                }
                currentGroupHeader = null;
                sortedGroupedItems.add(item);
            }
        }

        if (!currentGroupItems.isEmpty()) {
            currentGroupItems.sort(Comparator.comparing(JMenuItem::getText, String.CASE_INSENSITIVE_ORDER));
            if (currentGroupHeader != null) sortedGroupedItems.add(currentGroupHeader);
            sortedGroupedItems.addAll(currentGroupItems);
        }

        sortMenuItems(sortedGroupedItems);
        menuItems.add(makeMenu("Edit Formulas", sortedGroupedItems));

        menuItems.add(MENU_SEPARATOR);

        menuItem = new JMenuItem("Jython Library");
        menuItem.setToolTipText("Open Jython Library window");
        menuItem.addActionListener(e -> showJythonEditor());
        menuItems.add(menuItem);

        menuItem = new JMenuItem("Jython Shell");
        menuItem.setToolTipText("Open Jython Shell window");
        menuItem.addActionListener(e -> createShell());
        menuItems.add(menuItem);

        // McIDAS Inquiry #2701-3141
        menuItem = new JMenuItem("Load Jython Script...");
        menuItem.setToolTipText("Select a Jython script to run");
        menuItem.addActionListener(e -> {
            createShell();
            jythonShell.loadScript();
        });
        menuItems.add(menuItem);

        menuItem = new JMenuItem("Run Jython Script...");
        menuItem.setToolTipText("Select a Jython script to run");
        menuItem.addActionListener(e -> {
            createShell();
            jythonShell.loadAndRunScript();
        });
        menuItems.add(menuItem);

        menuItems.add(MENU_SEPARATOR);

        menuItem = new JMenuItem("Import");
        menuItem.setToolTipText("Import formulas");
        menuItem.addActionListener(e -> importFormulas());
        menuItems.add(menuItem);

        menuItem = new JMenuItem("Export");
        menuItem.setToolTipText("Export Formulas");
        menuItem.addActionListener(e -> exportFormulas());
        menuItems.add(menuItem);

        return menuItems;
    }

    // Recursively sort nested JMenu children
    private void sortMenuItems(List<?> items) {
        items.sort((o1, o2) -> {
            if (o1 instanceof JMenuItem && o2 instanceof JMenuItem) {
                return ((JMenuItem) o1).getText().compareToIgnoreCase(((JMenuItem) o2).getText());
            }
            return 0;
        });

        for (Object item : items) {
            if (item instanceof JMenu) {
                JMenu submenu = (JMenu) item;
                List<JMenuItem> subItems = new ArrayList<>();
                for (int i = 0; i < submenu.getItemCount(); i++) {
                    JMenuItem child = submenu.getItem(i);
                    if (child != null) subItems.add(child);
                }
                sortMenuItems(subItems);
                submenu.removeAll();
                for (JMenuItem sortedChild : subItems) {
                    submenu.add(sortedChild);
                }
            }
        }
    }
    
    /**
     * Determine if the user should be warned about a potential bug that we've been unable to resolve.
     *
     * <p>The conditions for the bug to appear are:
     * <ul>
     *     <li>In background mode (i.e. running a script).</li>
     *     <li>Geometry by reference is <b>disabled</b>.</li>
     *     <li>New font rendering is <b>enabled</b>.</li>
     * </ul>
     *
     * @return {@code true} if the user's configuration has made it possible for bug to manifest.
     */
    public static boolean shouldWarnImageCapturing() {
        boolean backgroundMode = McIDASV.getStaticMcv().getArgsManager().isScriptingMode();
        boolean shouldWarn = false;
        OptionMaster optMaster = OptionMaster.getInstance();
        BooleanOption useGeometryByRef = optMaster.getBooleanOption("USE_GEOBYREF");
        if (useGeometryByRef != null) {
            shouldWarn = backgroundMode
                    && !Boolean.parseBoolean(System.getProperty("visad.java3d.geometryByRef"))
                    && Boolean.parseBoolean(System.getProperty(PROP_HIQ_FONT_RENDERING, "false"));
        }
//        System.out.println("background mode: "+backgroundMode);
//        System.out.println("geo by ref: "+Boolean.parseBoolean(System.getProperty("visad.java3d.geometryByRef")));
//        System.out.println("new fonts: "+Boolean.parseBoolean(System.getProperty(PROP_HIQ_FONT_RENDERING, "false")));
//        System.out.println("shouldWarn: "+shouldWarn);
//        System.out.println("props:\n"+System.getProperties());
//        return shouldWarn;
        return false;
    }

}
