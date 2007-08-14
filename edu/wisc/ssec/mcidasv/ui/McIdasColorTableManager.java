package edu.wisc.ssec.mcidasv.ui;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.ui.colortable.ColorTableCanvas;
import ucar.unidata.ui.colortable.ColorTableDefaults;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.NamedObject;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.ResourceManager;

import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;

/**
 * A class to manage Hydra color tables
 */
public class McIdasColorTableManager extends ColorTableManager {


    /** The singleton */
    private static McIdasColorTableManager manager;

    /** The color table category */
    public static final String CATEGORY_HYDRA = "HYDRA";

    /** File filter used for IDV color tables */
    public static final PatternFileFilter FILTER_HYDRA =
        new PatternFileFilter(".+\\.ascii", "HYDRA color table (*.ascii)", ".ascii");


    /**
     * Create me
     *
     */
    public McIdasColorTableManager() {
        super();
    }

    /**
     * Return the file filters used for writing a file on an import
     *
     * @return Read file  filters
     */
    public List getReadFileFilters() {
        ColorTableManager ctm = new ColorTableManager();
        List filters = ctm.getWriteFileFilters();
        filters.add(FILTER_HYDRA);
        return filters;
    }

    /**
     * Import a color table
     *
     * @param makeUnique If true then we change the name of the color table so it is unique
     * @return The imported color table
     */
    public NamedObject doImport(boolean makeUnique) {
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
        List   cts    = new ArrayList();
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
            cts.addAll(ColorTableDefaults.makeGempakColorTables(name, cat,
                    file));
        } else if (suffix.endsWith(".gp")) {
            cts.addAll(ColorTableDefaults.makeNclRgbColorTables(name, cat,
                    file, null));
        } else if (isGempakFile(file)) {
            cts.addAll(ColorTableDefaults.makeGempakColorTables(name, cat,
                    file));
        } else if (suffix.endsWith(".rgb")) {
            cts.addAll(ColorTableDefaults.makeRgbColorTables(name, cat,
                    file));
         
        } else if (suffix.endsWith(".ascii")) {
            if (category == null) {
                cat = CATEGORY_HYDRA;
            }
            cts.add(McIdasColorTableDefaults.createColorTable(name, cat,
                    McIdasColorTableDefaults.makeTableFromASCII(file)));
        } else {
            return null;
        }
        if (cts.size() == 0) {
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

