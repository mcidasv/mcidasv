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

package edu.wisc.ssec.mcidasv.ui;

import java.io.IOException;

import java.util.List;
import java.util.ArrayList;

import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.ui.colortable.ColorTableDefaults;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.NamedObject;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.ResourceManager;

import ucar.unidata.xml.XmlEncoder;

/**
 * A class to manage Hydra color tables
 */
public class McIdasColorTableManager extends ColorTableManager {

    /** The color table category */
    public static final String CATEGORY_HYDRA = "HYDRA";

    /** File filter used for {@literal "HYDRA"} color tables */
    public static final PatternFileFilter FILTER_HYDRA =
        new PatternFileFilter(".+\\.ascii", "3-column RGB color table (*.ascii)", ".ascii");

    /** File filter used for McIDAS-X {@literal "enhancement files"} */
    public static final PatternFileFilter FILTER_ET =
        new PatternFileFilter(".+\\.et", "McIDAS-X color table (*.et)", ".et");

    /**
     * Create me
     *
     */
    public McIdasColorTableManager() {
        super();
    }

    /**
     * Filles the given list with menu items that represent that available 
     * color tables.
     * 
     * Overridden in McIDAS-V to force the presence of the "<local>" tag.
     */
    @Override public void makeColorTableMenu(final ObjectListener listener, List l) {
        makeColorTableMenu(listener, l, true);
    }

    /**
     * Return the file filters used for writing a file on an import
     *
     * @return Read file  filters
     */
    @Override public List getReadFileFilters() {
        ColorTableManager ctm = new ColorTableManager();
        List filters = ctm.getWriteFileFilters();
        filters.add(FILTER_HYDRA);
        filters.add(FILTER_ET);
        return filters;
    }

    /**
     * Import a color table
     *
     * @param makeUnique If true then we change the name of the color table so it is unique
     * @return The imported color table
     */
    @Override public NamedObject doImport(boolean makeUnique) {
        String file = FileManager.getReadFile(getTitle() + " import",
                          getReadFileFilters());
        if (file == null) {
            return null;
        }

        try {
            List cts = processSpecial(file, null, null);
            if (cts != null) {
                return doImport(cts, makeUnique);
            }
        } catch (IOException ioe) {
            LU.printException(log_, "Error reading file: " + file, ioe);
            return null;
        }
        try {
            String xml = IOUtil.readContents(file, ResourceManager.class);
            if (xml == null) {
                return null;
            }
            Object o = (new XmlEncoder()).toObject(xml);
            return doImport(o, makeUnique);
        } catch (Exception exc) {
            LU.printException(log_, "Error reading file: " + file, exc);
        }
        return null;
    }

    /**
     * Try to load in one of the special colortables
     *
     * @param file file
     * @param name _more_
     * @param category category
     *
     * @return the ct
     *
     * @throws IOException _more_
     */
    private List processSpecial(String file, String name, String category)
            throws IOException {

        String cat = category;
        if (name == null) {
            name = IOUtil.stripExtension(IOUtil.getFileTail(file));
        }
        if (cat == null) {
            cat = ColorTable.CATEGORY_BASIC;
        }

        String suffix = file.toLowerCase();
        List<ColorTable> cts = new ArrayList<ColorTable>();
        if (suffix.endsWith(".et")) {
            if (category == null) {
                cat = ColorTable.CATEGORY_SATELLITE;
            }
            cts.add(ColorTableDefaults.createColorTable(name, cat,
                    ColorTableDefaults.makeTableFromET(file, false)));
        } else if (suffix.endsWith(".pa1")) {
            cts.add(ColorTableDefaults.createColorTable(name, cat,
                    ColorTableDefaults.makeTableFromPal1(file)));
        } else if (suffix.endsWith(".pa2")) {
            cts.add(ColorTableDefaults.createColorTable(name, cat,
                    ColorTableDefaults.makeTableFromPal2(file)));
        } else if (suffix.endsWith(".pal")) {
            cts.add(ColorTableDefaults.createColorTable(name, cat,
                    ColorTableDefaults.makeTableFromPal(file)));
        } else if (suffix.endsWith(".act")) {
            cts.add(ColorTableDefaults.createColorTable(name, cat,
                    ColorTableDefaults.makeTableFromAct(file)));
        } else if (suffix.endsWith(".ncmap")) {
            //Treat these like gempak
            cts.addAll(ColorTableDefaults.makeGempakColorTables(name, cat, file));
        } else if (suffix.endsWith(".gp")) {
            cts.addAll(ColorTableDefaults.makeNclRgbColorTables(name, cat, file, null));
        } else if (isGempakFile(file)) {
            cts.addAll(ColorTableDefaults.makeGempakColorTables(name, cat, file));
        } else if (suffix.endsWith(".rgb")) {
            cts.addAll(ColorTableDefaults.makeRgbColorTables(name, cat, file));
        } else if (suffix.endsWith(".ascii")) {
            if (category == null) {
                cat = CATEGORY_HYDRA;
            }
            cts.add(McIdasColorTableDefaults.createColorTable(name, cat,
                    McIdasColorTableDefaults.makeTableFromASCII(file)));
        } else {
            return null;
        }
        if (cts.isEmpty()) {
            return null;
        }
        return cts;
    }

    /**
     * Is the given file a Gempak file
     *          
     * @param file The file name to check
     * @return Is it a Gempak file
     */
    private boolean isGempakFile(String file) {
        return file.toLowerCase().endsWith(".tbl");
    }
}

