/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2019
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

package ucar.unidata.idv;

import static edu.wisc.ssec.mcidasv.McIDASV.getStaticMcv;
import static ucar.unidata.util.GuiUtils.MENU_SEPARATOR;
import static ucar.unidata.util.GuiUtils.getInput;
import static ucar.unidata.util.GuiUtils.inset;
import static ucar.unidata.util.GuiUtils.makeButton;
import static ucar.unidata.util.GuiUtils.makeMenu;
import static ucar.unidata.util.GuiUtils.makeMenuItem;
import static ucar.unidata.util.GuiUtils.makeDynamicMenu;
import static ucar.unidata.util.GuiUtils.makePopupMenu;
import static ucar.unidata.util.GuiUtils.topCenterBottom;
import static ucar.unidata.util.GuiUtils.wrap;
import static ucar.unidata.util.IOUtil.cleanFileName;
import static ucar.unidata.util.IOUtil.getFileTail;
import static ucar.unidata.util.IOUtil.getInputStream;
import static ucar.unidata.util.IOUtil.joinDir;
import static ucar.unidata.util.IOUtil.makeDir;
import static ucar.unidata.util.IOUtil.moveFile;
import static ucar.unidata.util.IOUtil.readContents;
import static ucar.unidata.util.IOUtil.writeFile;
import static ucar.unidata.util.LogUtil.userErrorMessage;
import static ucar.unidata.util.LogUtil.userMessage;
import static ucar.unidata.util.StringUtil.listToStringArray;
import static ucar.unidata.util.StringUtil.removeWhitespace;
import static ucar.unidata.util.StringUtil.replace;
import static ucar.unidata.util.StringUtil.split;
import static ucar.unidata.util.StringUtil.stringMatch;
import static ucar.unidata.xml.XmlUtil.getRoot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.JTextComponent;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.ui.JythonEditor;
import edu.wisc.ssec.mcidasv.util.FileFinder;
import edu.wisc.ssec.mcidasv.util.LoudPyStringMap;
import edu.wisc.ssec.mcidasv.util.pathwatcher.OnFileChangeListener;

import org.python.core.CompileMode;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PySyntaxError;
import org.python.core.PySystemState;
import org.python.core.PyTableCode;
import org.python.util.PythonInterpreter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;

import visad.VisADException;

import ucar.unidata.data.DataCancelException;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.DerivedDataDescriptor;
import ucar.unidata.data.DescriptorDataSource;
import ucar.unidata.idv.ui.FormulaDialog;
import ucar.unidata.idv.ui.JythonShell;
import ucar.unidata.ui.Help;
import ucar.unidata.ui.TextSearcher;
import ucar.unidata.ui.TreePanel;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;

import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

/** 
 * Manages Jython-related functionality:
 * <ul>
 *   <li>Jython interpreters used in the application</li>
 *   <li>Jython libraries</li>
 *   <li>
 *     End user formulas. These are defined using 
 *     {@link ucar.unidata.data.DerivedDataDescriptor DerivedDataDescriptor} 
 *     and are held in 
 *     {@link ucar.unidata.data.DescriptorDataSource DescriptorDataSource}
 *   </li>
 * </ul>
 * 
 * @author IDV development team
 */
public class JythonManager extends IdvManager implements ActionListener,
    OnFileChangeListener
{
    
    /** Trusty logging object. */
    private static final Logger logger =
        LoggerFactory.getLogger(JythonManager.class);

    /** Jython script that correctly initializes Jython environment. */
    public static final String CONSOLE_INIT =
        "/edu/wisc/ssec/mcidasv/resources/python/console_init.py";

    /** The path to the editor executable */
    public static final String PROP_JYTHON_EDITOR = "idv.jython.editor";
    
    /** color to use for diabled editors */
    private static final Color COLOR_DISABLED = new Color(210, 210, 210);
    
    /** Logging object used for Jython-specific logging output. */
    protected static final Logger jythonLogger =
        LoggerFactory.getLogger("jython");
    
    /** Used to provide initial capacities for data structures. */
    private static final int ESTIMATED_INTERPRETERS = 42;
    
    /** output stream for interp */
    private OutputStream outputStream;
    
    /** any errors */
    private boolean inError = false;
    
    /** the jtree on the left that lists the different python files */
    private TreePanel treePanel;
    
    /** One text component per tab */
    private List<LibHolder> libHolders = new ArrayList<>();
    
    /** tmp lib */
    private LibHolder tmpHolder;
    
    /**
     * This holds all of the  end-user formulas, the ones
     * from the system and the ones from the users local space.
     */
    private DescriptorDataSource descriptorDataSource;
    
    /** Formula descriptors */
    List<DerivedDataDescriptor> descriptors;
    
    /**
     * This is the interpreter used for processing the
     * UI commands. e.g., the ones in defaultmenu.xml
     */
    private PythonInterpreter uiInterpreter = null;
    
    /** the text searching widget */
    private TextSearcher textSearcher;
    
    /** Used to evaluate derived data choices */
    private PythonInterpreter derivedDataInterpreter;
    
    /** The jython editor */
    private LibHolder mainHolder;
    
    /**
     * List of all active interpreters. We keep these around so when
     * the user changes the jython library we can reevaluate the code
     * in each interpreter.
     */
    private List<PythonInterpreter> interpreters =
        new ArrayList<>(ESTIMATED_INTERPRETERS);
    
    /** The edit menu item */
    private JMenuItem editFileMenuItem;
    
    /** local python dir */
    private String pythonDir;
    
    /** Keep track of the nanoseconds spent initializing interpreters. */
    private final Map<String, Long> interpreterTimes =
        new ConcurrentHashMap<>(ESTIMATED_INTERPRETERS);
    
    /**
     * Create the manager and call initPython.
     *
     * @param idv The IDV
     */
    public JythonManager(IntegratedDataViewer idv) {
        super(idv);
        String userDirectory = getStore().getUserDirectory().toString();
        pythonDir = joinDir(userDirectory, "python");
        if (!new File(pythonDir).exists()) {
            try {
                makeDir(pythonDir);
                //Move the old default.py to the subdir
                File oldFile = new File(joinDir(userDirectory, "default.py"));
                File newFile = new File(joinDir(pythonDir, "default.py"));
                if (oldFile.exists()) {
                    moveFile(oldFile, new File(pythonDir));
                } else {
                    writeFile(newFile.toString(), "");
                }
            } catch (Exception exc) {
                logException("Moving jython lib", exc);
            }
        }
        initPython();
    }
    
    /**
     * Initialize the Python package in a thread.
     * We define the python cache to be in the users
     * .unidata/idv directory. Python puts the results of its
     * parsing the jar files there.
     */
    private void initPython() {
        if (getArgsManager().isScriptingMode()) {
            initPythonInner();
        } else {
            Misc.run(this::initPythonInner);
        }
    }

    /**
     * Initialize the python interpreter. This gets called from initPython inside of a thread.
     */
    private void initPythonInner() {
        String cacheDir = getStore().getJythonCacheDir();
        ResourceCollection rc = getResourceManager().getResources(IdvResourceManager.RSC_JYTHONTOCOPY);
        rc.clearCache();
        try {
            for (int i = 0; i < rc.size(); i++) {
                String path = rc.get(i).toString();
                String name = IOUtil.getFileTail(path);
                File localFile = new File(joinDir(cacheDir, name));
                //System.err.println ("Jython lib:" + localFile);
                //Do we copy all of the time? Perhaps we need to
                //check if we are running a new version?
                //if(localFile.exists()) continue;
                String contents = rc.read(i);
                if (contents == null) {
                    continue;
                }
                //              System.err.println ("Writing:" + contents);
                writeFile(localFile.getPath(), contents);
            }
        } catch (Exception exc) {
            logException("Writing jython lib", exc);
        }
        
        Properties pythonProps = new Properties();
        if (cacheDir != null) {
            pythonProps.put("python.home", cacheDir);
        }

        doMakeContents();

        if (!getArgsManager().isScriptingMode()) {
            makeFormulasFromLib();
            try {
                McIDASV mcv = getStaticMcv();
                if (mcv != null) {
                    mcv.watchDirectory(pythonDir, "*.py", this);
                } else {
                    logger.warn("getStaticMcv() returned null! Could not watch directory '"+pythonDir+'\'');
                }
            } catch (IOException e) {
                logger.error("Could not watch directory '"+pythonDir+ '\'', e);
            }
        }
    }

    public boolean isFileInJythonLibrary(String filePath) {
        return libHolders.stream()
                         .anyMatch(holder -> 
                                   Objects.equals(holder.filePath, filePath));
    }
    
    /**
     * Respond to files being created in the user's Jython directory.
     *
     * @param filePath The file path.
     */
    public void onFileCreate(String filePath) {
        if (!isFileInJythonLibrary(filePath)) {
            logger.trace("filePath='{}'", filePath);
            createNewLibrary(filePath);
        }
    }

    /**
     * Respond to files being modified in the user's Jython directory.
     *
     * <p>Note: this is currently a no-op; unsure how to handle needing to
     * reload files.</p>
     *
     * @param filePath The file path.
     */
    public void onFileModify(String filePath) {
        if (isFileInJythonLibrary(filePath)) {
            logger.trace("filePath='{}'", filePath);
            updateLibrary(filePath);
        }
    }

    /**
     * Respond to files being deleted in the user's Jython directory.
     *
     * <p>Note: this is currently a no-op.</p>
     *
     * @param filePath The file path.
     */
    public void onFileDelete(String filePath) {
        logger.trace("filePath='{}'", filePath);
        if (isFileInJythonLibrary(filePath)) {
            quietRemoveLibrary(filePath);
        }
    }

    // TODO(jon): dox!
    private static boolean addToSysPath(PySystemState sys, final String path) {
        PyString pyStrPath = Py.newString(path);
        boolean result = sys.path.contains(pyStrPath);
        if (!result) {
            sys.path.append(pyStrPath);
        }
        return result;
    }
    
    /**
     * make formulas from the methods in the lib
     */
    private void makeFormulasFromLib() {
        List<PyFunction> procedures = findJythonMethods(true);
        for (int i = 0; i < procedures.size(); i++) {
            PyFunction func = procedures.get(i);
            PyObject docString = func.__doc__;
            if (docString == Py.None) {
                continue;
            }
            
            List<String> lines = split(docString.toString().trim(), "\n", true, true);
            String formulaId = null;
            String desc = null;
            String group = null;
            Map<String, String> attrProps = null;
            for (String line : lines) {
                if (line.startsWith("@formulaid")) {
                    formulaId = line.substring("@formulaid".length()).trim();
                } else if (line.startsWith("@description")) {
                    desc = line.substring("@description".length()).trim();
                } else if (line.startsWith("@group")) {
                    group = line.substring("@group".length()).trim();
                } else if (line.startsWith("@param")) {
                    line = line.substring("@param".length()).trim();
                    String[] toks = split(line, " ", 2);
                    if (attrProps == null) {
                        attrProps = new HashMap<>();
                    }
                    attrProps.put(toks[0], '[' + toks[1] + ']');
                }
            }
            if ((formulaId != null) || (desc != null)) {
                if (formulaId == null) {
                    formulaId = func.__name__;
                }
                List<DataCategory> categories = new ArrayList<>();
                if (group != null) {
                    categories.add(DataCategory.parseCategory(group, true));
                }
                DerivedDataDescriptor ddd =
                    new DerivedDataDescriptor(getIdv(), formulaId,
                        ((desc != null)
                         ? desc
                         : func.__name__), makeCallString(func, attrProps),
                                           categories);
                descriptorDataSource.addDescriptor(ddd);
            }
        }
    }
    
    /**
     * Create, if needed, and show the jython editor.
     */
    public void showJythonEditor() {
        super.show();
        LibHolder visible = findVisibleComponent();
        if (visible != null) {
            logger.trace("showing '{}'", visible.filePath);
            visible.getTextComponent().requestFocusInWindow();
        } else {
            logger.trace("no visible component!! :(");
        }
    }

    /**
     * Find the visible library
     *
     * @return lib being shown
     */
    private LibHolder findVisibleComponent() {
        try {
            for (LibHolder holder : libHolders) {
                if (holder.outerContents.isShowing()) {
                    holder.outerContents.getLocationOnScreen();
                    return holder;
                }
            }
        } catch (Exception exc) {
            logger.error("problem with finding visible component: {}", exc);
        }
        return null;
    }
    
    /**
     * Export selcted text of current tab to plugin
     */
    public void exportSelectedToPlugin() {
        LibHolder holder = findVisibleComponent();
        String text = "";
        holder.copy();
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText =
            (contents != null)
            && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                text = (String)contents.getTransferData(DataFlavor.stringFlavor);
                if (holder.getText().indexOf(text) < 0) {
                    text = null;
                }
            } catch (Exception ex) {
                LogUtil.logException("Accessing clipboard", ex);
                return;
            }
        }
        if ((text == null) || (text.trim().isEmpty())) {
            userMessage("No text selected");
            return;
        }
        getIdv().getPluginManager().addText(text, "jython.py");
    }
    
    /**
     * Export  to plugin
     */
    public void exportToPlugin() {
        LibHolder holder = findVisibleComponent();
        getIdv().getPluginManager().addText(holder.getText(), "jython.py");
    }
    
    /**
     * Create the jython editor. We create a tree panel that holds
     * each valid jython library defined in the
     * IDV's resource manager.
     *
     * @return The gui contents
     */
    @Override protected JComponent doMakeContents() {
        if (contents != null) {
            return contents;
        }
        
        try {
            ResourceCollection resources = getResourceManager().getResources(
                                               IdvResourceManager.RSC_JYTHON);
            if (resources == null) {
                LogUtil.userMessage("No Jython resources defined");
                return null;
            }
            treePanel  = new TreePanel();
            libHolders = new ArrayList<>(resources.size() * 10);
            int systemCnt = 1;
            Map<String, String> seen = new HashMap<>();
            for (int i = 0; i < resources.size(); i++) {
                String showInEditor = resources.getProperty("showineditor", i);
                if ((showInEditor != null) && "false".equals(showInEditor)) {
                    //                    System.err.println ("skipping:" + resources.get(i));
                    continue;
                }
                String path = resources.get(i).toString();
                List files;
                File file = new File(path);
                if (file.exists() && file.isDirectory()) {
//                    File[] libFiles = file.listFiles((java.io.FileFilter)new PatternFileFilter(".*\\.py$"));
//                    files.addAll(toList(libFiles));
//                    files = toList(libFiles);
                    files = FileFinder.findFiles(file.getPath(), "*.py");
                } else {
                    files = new ArrayList();
                    files.add(path);
                }
                for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
                    path = files.get(fileIdx).toString();
                    file = new File(path);
                    if (file.getName().startsWith(".") || file.isDirectory()) {
                        continue;
                    }
                    String canonicalPath = replace(path, "\\", "/");
                    if (seen.get(canonicalPath) != null) {
                        continue;
                    }
                    seen.put(canonicalPath, canonicalPath);
                    String label = resources.getLabel(i);
                    if (label == null) {
                        label = getFileTail(PluginManager.decode(path));
                    }
                    String text = readContents(path, (String)null);
                    if ((text != null) && Misc.isHtml(text)) {
                        continue;
                    }
                    LibHolder libHolder;
                    boolean editable = resources.isWritableResource(i) || (file.exists() && file.canWrite());
                    if (!editable && (new File(path).isDirectory() || (text == null))) {
                        continue;
                    }

                    libHolder = makeLibHolder(editable, label, path, text);
                    String category = resources.getProperty("category", i);
                    String treeCategory = null;
                    if (libHolder.isEditable()) {
                        treeCategory = "Local Jython";
                    } else if (category != null) {
                        treeCategory = category;
                    }
                    treePanel.addComponent(libHolder.outerContents, treeCategory, label, null);
                }
            }
            String tmpPath = getIdv().getStore().getTmpFile("tmp.py");
            tmpHolder = makeLibHolder(false, "Temporary Jython", tmpPath, "");
            treePanel.addComponent(tmpHolder.outerContents, null, "Temporary", null);
            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = makeDynamicMenu("File", this, "makeFileMenu");
            JMenu helpMenu = new JMenu("Help");
            menuBar.add(fileMenu);
            menuBar.add(helpMenu);
            helpMenu.add(makeMenuItem("Show Jython Help", this, "showHelp"));
            textSearcher = new TextSearcher() {
                @Override public TextSearcher.TextWrapper getTextWrapper() {
                    return findVisibleComponent();
                }
            };
            contents = topCenterBottom(menuBar, treePanel, textSearcher);
            setMenuBar(menuBar);
            return contents;
        } catch (Throwable exc) {
            logException("Creating jython editor", exc);
            return null;
        }
    }

    /**
     * Save on exit if anything is changed
     *
     * @return continue with exit
     */
    public boolean saveOnExit() {
        if (getArgsManager().isScriptingMode()) {
            return true;
        }
        List<LibHolder> toSave = new ArrayList<>(libHolders.size());
        for (int i = libHolders.size() - 1; i >= 0; i--) {
            LibHolder holder = libHolders.get(i);
            if ((holder.saveBtn == null) || !holder.isEditable()) {
                continue;
            }
            if (holder.saveBtn.isEnabled()) {
                toSave.add(holder);
            }
        }
        if (!toSave.isEmpty()) {
            int response =
                GuiUtils.showYesNoCancelDialog(null,
                    "Do you want to save the modified Jython before exiting?",
                    "Save Jython");
            if (response == 2) {
                return false;
            }
            if (response == 0) {
                for (int i = toSave.size() - 1; i >= 0; i--) {
                    if (!writeJythonLib(toSave.get(i))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Make lib for editing
     * 
     * @param editable is this editor editable
     * @param label label
     * @param path file
     * @param text text
     * 
     * @return LibHolder
     * 
     * @throws VisADException On badness
     */
    private LibHolder makeLibHolder(boolean editable, String label, String path, String text)
            throws VisADException
    {
        return makeLibHolder(editable, label, path, text, true);
    }
    
    private LibHolder makeLibHolder(boolean editable, 
                                    String label, 
                                    String path, 
                                    String text, 
                                    boolean addToLibrary)
        throws VisADException
    {
        final LibHolder[] holderArray  = { null };
        JythonEditor jythonEditor = new JythonEditor() {
            @Override public void undoableEditHappened(UndoableEditEvent e) {
                if ((holderArray[0] != null)
                        && (holderArray[0].saveBtn != null)) {
                    holderArray[0].saveBtn.setEnabled(true);
                    holderArray[0].functions = null;
                }
                super.undoableEditHappened(e);
            }
        };
        jythonEditor.getTextComponent().addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (McIDASV.isMac() && e.isMetaDown() && e.getKeyCode() == KeyEvent.VK_F) {
                    textSearcher.getFindFld().requestFocusInWindow();
                } else if (!McIDASV.isMac() && GuiUtils.isControlKey(e, KeyEvent.VK_F)) {
                    textSearcher.getFindFld().requestFocusInWindow();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    JTextField field = textSearcher.getFindFld();
                    boolean highlights = textSearcher.getTextWrapper().hasHighlights();
                    if (!field.getText().isEmpty() && highlights) {
                        textSearcher.getTextWrapper().removeHighlights();
                        field.setText("");
                    }
                }
            }
        });
        jythonEditor.getTextComponent().addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                holderArray[0].setSearchIndex(new Point(e.getX(), e.getY()));
                if (!SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                JTextComponent comp = jythonEditor.getTextComponent();
                int point = comp.viewToModel(new Point(e.getX(), e.getY()));
                String text = comp.getText();
                String token = "";
                int idx = point;
                List items = new ArrayList();
                JMenuItem helpMenuItem = null;
                if ((idx < 0) || (idx >= text.length())) {
                    JPopupMenu popup = jythonEditor.createPopupMenu();
                    popup.show(jythonEditor.getTextComponent(), e.getX(), e.getY());
                    return;
                }
                while (idx >= 0) {
                    char c = text.charAt(idx);
                    idx--;
                    if (!Character.isJavaIdentifierPart(c)) {
                        break;
                    }
                    token = c + token;
                }
                int len = text.length();
                idx = point + 1;
                while (idx < len) {
                    char c = text.charAt(idx);
                    idx++;
                    if (!Character.isJavaIdentifierPart(c)) {
                        break;
                    }
                    token = token + c;
                }
                if (token.length() == 0) {
                    token = comp.getSelectedText();
                }
                if ((token != null) && (token.trim().length() > 0)) {
                    token = token.trim();
                    List funcs = findJythonMethods(true, Misc.newList(holderArray[0]));
                    for (int i = 0; i < funcs.size(); i++) {
                        PyFunction func = (PyFunction) funcs.get(i);
                        if (func.__name__.equals(token)) {
                            items.add(makeMenuItem("Make formula for " + token, JythonManager.this, "makeFormula", func));
                            String helpLink = "idv.tools.jythonlib." + func.__name__;
                            if (Help.getDefaultHelp().isValidID(helpLink)) {
                                helpMenuItem = makeMenuItem("Help", JythonManager.this, "showHelp", helpLink);
                            }
                            break;
                        }
                    }
                }
                if (holderArray[0].isEditable()) {
                    items.add(makeMenu("Insert Procedure Call", makeProcedureMenu(jythonEditor, "insertText", null)));
                    items.add(makeMenu("Insert Idv Action", getIdv().getIdvUIManager().makeActionMenu(jythonEditor, "insertText", true)));
                }
                if (helpMenuItem != null) {
                    items.add(MENU_SEPARATOR);
                    items.add(helpMenuItem);
                }
                JPopupMenu popup = makePopupMenu(items);
                if (popup == null) {
                    popup = jythonEditor.createPopupMenu();
                }
                popup.show(jythonEditor.getTextComponent(), e.getX(), e.getY());
            }
        });
        
        jythonEditor.getScrollPane().setPreferredSize(new Dimension(500, 400));
        JComponent wrapper = GuiUtils.center(jythonEditor.getScrollPane());
        LibHolder libHolder = new LibHolder(editable, this, label, jythonEditor, path, wrapper);
        
        holderArray[0] = libHolder;
        
        if (addToLibrary) {
            if (mainHolder == null) {
                mainHolder = libHolder;
            }
            libHolders.add(libHolder);
        }
        //        tabContents.add(BorderLayout.SOUTH, GuiUtils.wrap(libHolder.saveBtn));
        if (text == null) {
            text = "";
        }

        jythonEditor.setText(text);

        if (libHolder.saveBtn != null) {
            libHolder.saveBtn.setEnabled(false);
        }
        return libHolder;
    }
    
    /**
     * Make a formula
     *
     * @param func function
     */
    public void makeFormula(PyFunction func) {
        String name = func.__name__;
        DerivedDataDescriptor ddd  = null;
        for (DerivedDataDescriptor tmp : descriptors) {
            if (tmp.getId().equals(name)) {
                ddd = tmp;
                break;
            }
        }
        boolean isNew = true;
        if (ddd != null) {
            isNew = false;
            if (!GuiUtils.askOkCancel(
                    "Formula Exists",
                    "<html>A formula with the name: " + name
                    + " already exists.</br>Do you want to edit it?</html>")) {
                return;
            }
        }
        if (ddd == null) {
            ddd = new DerivedDataDescriptor(getIdv(), name, "",
                                            makeCallString(func, null),
                                            new ArrayList());
        }
        showFormulaDialog(ddd, isNew);
    }
    
    /**
     * make new library
     */
    public void makeNewLibrary() {
        String name = "";
        try {
            while (true) {
                name = getInput("Enter name for library", "Name: ", name);
                if (name == null) {
                    return;
                }
                String fullName = cleanFileName(name);
                if (!fullName.endsWith(".py")) {
                    fullName = fullName + ".py";
                }
                fullName = joinDir(pythonDir, fullName);
                if (new File(fullName).exists()) {
                    LogUtil.userErrorMessage(
                        "A jython library with the given name already exists");
                    continue;
                }
                writeFile(fullName, "");
                LibHolder libHolder = makeLibHolder(true, name, fullName, "");
                treePanel.addComponent(libHolder.outerContents, "Local Jython", name, null);
                break;
            }
        } catch (Exception exc) {
            logException("An error occurred creating the jython library",
                         exc);
        }
    }
    
    /**
     * Determine which {@link LibHolder} is visible to the user, if any.
     * 
     * @return Either the visible {@code LibHolder} or {@code null}.
     */
    private LibHolder getVisibleLibrary() {
        LibHolder visible = null;
        if (treePanel != null) {
            // TODO(jon): need to verify the reliability of this method
            Component vizComp = treePanel.getVisibleComponent();
            for (LibHolder holder : getLibHolders()) {
                if (Objects.equals(vizComp, holder.outerContents)) {
                    visible = holder;
                    break;
                }
            }
        }
        return visible;
    }
    
    /**
     * Update the {@literal "Jython Library"} GUI.
     * 
     * <p>Note: when {@code modifiedLibrary} is the library that is currently 
     * visible to the user (say the user decided to save the file), we appear
     * to be deadlocking Windows somehow. Bottom line (for now): 
     * {@code modifiedLibrary} should <b>not</b> be the currently visible 
     * library.</p>
     * 
     * @param modifiedLibrary Jython Library that was modified. 
     *                        Cannot be {@code null}.
     */
    private void updateLibraryGui(LibHolder modifiedLibrary) {
        try {
            Path libraryPath = Paths.get(modifiedLibrary.filePath);
            String contents = readContents(libraryPath.toFile());
            modifiedLibrary.setText(contents);
            if (modifiedLibrary.saveBtn != null) {
                modifiedLibrary.saveBtn.setEnabled(false);
            }
            // TODO(jon): might be as simple as removing this
            // repaint call...need to get Windows VM working!
            modifiedLibrary.wrapper.repaint();
        } catch (IOException e) {
            logException("Could not read " + modifiedLibrary.filePath, e);
        }
    }
    
    /**
     * Handle updating the given (pre-existing) Jython Library file.
     * 
     * <b>{@literal "Updating"} means having the Jython Shell essentially 
     * do a Python-style {@code reload()}, and then ensuring that the Jython 
     * Library is also up-to-date.</b>
     * 
     * @param path Path to the modified Jython library file. 
     *             Cannot be {@code null}.
     * 
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    public void updateLibrary(String path) {
        Objects.requireNonNull(path);
        LibHolder viz = getVisibleLibrary();
        for (LibHolder holder : getLibHolders()) {
            if (Objects.equals(holder.filePath, path)) {
                logger.trace("trying to update {}", holder.filePath);
                if ((viz == null) || !Objects.equals(viz, holder)) {
                    SwingUtilities.invokeLater(() -> updateLibraryGui(holder));
                }
                evaluateLibJython(false, holder);
            }
        }
    }
     
    public void createNewLibrary(String path) {
        Path p = Paths.get(path);
        String name = p.getFileName().toString();
        try {
            String contents = readContents(p.toFile());
            LibHolder holder = makeLibHolder(true, name, path, contents);
            treePanel.addComponent(holder.outerContents, "Local Jython", name, null);
            evaluateLibJython(false, holder);
        } catch (Exception e) {
            logException("An error occurred creating the jython library", e);
        }
    }

    /**
     * the libs
     *
     * @return the libs
     */
    public List<LibHolder> getLibHolders() {
        return libHolders;
    }
    
    /**
     * remove lib
     *
     * @param holder lib
     */
    public void removeLibrary(LibHolder holder) {
        if (!GuiUtils.askYesNo(
                "Remove Jython Library",
                "Are you sure you want to remove the library: "
                + getFileTail(holder.filePath))) {
            return;
        }
        try {
            libHolders.remove(holder);
            treePanel.removeComponent(holder.outerContents);
            new File(holder.filePath).delete();
        } catch (Exception exc) {
            logException("An error occurred removing jython library", exc);
        }
    }
    
    public void quietRemoveLibrary(String filePath) {
        LibHolder match = null;
        for (LibHolder holder : getLibHolders()) {
            if (Objects.equals(holder.filePath, filePath)) {
//                libHolders.remove(holder);
//                treePanel.removeComponent(holder.outerContents);
                match = holder;
            }
        }
        if (match != null) {
            libHolders.remove(match);
            treePanel.removeComponent(match.outerContents);
            treePanel.repaint();
        }
    }
    
    /**
     * make men
     *
     * @param fileMenu menu
     */
    public void makeFileMenu(JMenu fileMenu) {
        LibHolder holder = findVisibleComponent();
        fileMenu.add(makeMenuItem("New Jython Library...", this, "makeNewLibrary"));
        if ((holder != null) && holder.isEditable() && (holder.editProcess == null)) {
            fileMenu.add(makeMenuItem("Remove  Library", this, "removeLibrary", holder));
            fileMenu.add(makeMenuItem("Save", this, "writeJythonLib", holder));
            if (getStateManager().getPreferenceOrProperty(PROP_JYTHON_EDITOR, "").trim().length() > 0) {
                fileMenu.add(editFileMenuItem =
                    makeMenuItem("Edit in External Editor", this, "editInExternalEditor"));
            }
        }
        fileMenu.addSeparator();
        fileMenu.add(iconMenuItem("Export to Plugin", "exportToPlugin", "/auxdata/ui/icons/plugin_add.png"));
        fileMenu.add(iconMenuItem("Export Selected to Plugin", "exportSelectedToPlugin", "/auxdata/ui/icons/plugin_add.png"));
        fileMenu.addSeparator();
        fileMenu.add(makeMenuItem("Close", this, "close"));
    }
    
    /**
     * Allow external code to call {@link #applicationClosing()}.
     */
    public void handleApplicationExit() {
        applicationClosing();
    }

    /**
     * Gets called when the IDV is quitting. Kills the editor process if
     * there is one.
     */
    protected void applicationClosing() {
        if (getArgsManager().isScriptingMode()) {
            return;
        }
        for (int i = libHolders.size() - 1; i >= 0; i--) {
            LibHolder holder = libHolders.get(i);
            if (holder.editProcess != null) {
                try {
                    holder.editProcess.destroy();
                } catch (Exception exc) {}
            }
        }
    }
    
    /**
     * Edit the jython in the external editor
     */
    public void editInExternalEditor() {
        Misc.run(() -> this.editInExternalEditorInner(findVisibleComponent()));
    }
    
    /**
     * Edit the jython in the external editor
     *
     * @param holder lib
     */
    public void editInExternalEditorInner(final LibHolder holder) {
        try {
            if ((holder == null) || !holder.isEditable() || (holder.editProcess != null)) {
                return;
            }
            if (!writeJythonLib(holder)) {
                return;
            }
            holder.wrapper.removeAll();
            holder.wrapper.repaint();
            editFileMenuItem.setEnabled(false);
            holder.pythonEditor.setEnabled(false);
            
            String filename = holder.filePath;
            File file = new File(filename);
            long fileTime = file.lastModified();
            String command = getStateManager().getPreferenceOrProperty(PROP_JYTHON_EDITOR, "").trim();
            if (command.length() == 0) {
                return;
            }
            
            List<String> toks = split(command, " ", true, true);
            if (command.indexOf("%filename%") < 0) {
                toks.add("%filename%");
            }
            for (int i = 0; i < toks.size(); i++) {
                String tok = toks.get(i);
                toks.set(i, StringUtil.replace(tok, "%filename%", filename));
            }
            //            System.err.println("toks:" + toks);
            try {
                holder.editProcess =
                    Runtime.getRuntime().exec(listToStringArray(toks));
            } catch (Exception exc) {
                holder.editProcess = null;
                logException("An error occurred editing jython library", exc);
            }
            if (holder.editProcess != null) {
                Misc.run(() -> {
                    try {
                        holder.editProcess.waitFor();
                    } catch (Exception exc) {}
                    holder.editProcess = null;
                });
            }
            
            //This seems to hang?
            while (holder.editProcess != null) {
                Misc.sleep(1000);
                if (file.lastModified() != fileTime) {
                    fileTime = file.lastModified();
                    try {
                        String text = readContents(file);
                        holder.setText(text, false);
                        evaluateLibJython(false, holder);
                    } catch (Exception exc) {
                        logException("An error occurred editing jython library", exc);
                    }
                }
            }
            holder.pythonEditor.setEnabled(true);
            holder.wrapper.add(BorderLayout.CENTER, holder.pythonEditor.getScrollPane());
            holder.wrapper.repaint();
            editFileMenuItem.setEnabled(true);
        } catch (Exception exc) {
            logException("An error occurred editing jython library", exc);
        }
    }
    
    /**
     * Get the window titlexxx
     *
     * @return window title
     */
    @Override public String getWindowTitle() {
        return "Jython libraries";
    }
    
    /**
     * SHow help
     */
    public void showHelp() {
        showHelp("idv.tools.jython");
    }
    
    /**
     * show the help
     *
     * @param help the help id
     */
    public void showHelp(String help) {
        getIdvUIManager().showHelp(help);
    }
    
    private static final AtomicInteger createInterpCount = new AtomicInteger(0);
    
    /**
     * Factory method to create and interpreter. This
     * also adds the interpreter into the list of interpreters.
     * 
     * @return The new interpreter
     */
    public PythonInterpreter createInterpreter() {
        PythonInterpreter interp = new PythonInterpreter();
//        PythonInterpreter interp = new PythonInterpreter(new LoudPyStringMap("createInterpreter " + createInterpCount.addAndGet(1)));
//        logger.debug("000 created new PythonInterpreter(new LoudPyStringMap())");
        addInterpreter(interp);
        // only needed for background output?
        if (getArgsManager().getIsOffScreen()) {
            outputStream = new OutputStream() {
                @Override public void write(byte[] b, int off, int len) {
                    print(new String(b, off, len));
                }

                private void print(final String output) {
                    jythonLogger.info("{}", output);
                }

                @Override public void write(int b) { }
            };
            // NOTE: because this only ever happens in background mode, we
            // don't have to worry about calling resetOutputStreams(...);
            // once our interpreter closes, we quit the session
            interp.setOut(outputStream);
            interp.setErr(outputStream);
            
        }
        return interp;
    }
    
    /**
     * Reset Jython's STDERR/STDOUT streams.
     */
    private void resetOutputStreams() {
        // The reason this method exists is because of Jython's overuse of
        // static fields and the IDV's eagerness to create new Jython
        // interpreters.
        //
        // Jython's STDERR/STDOUT is static, so if you change the
        // streams for one Jython interpreter, you've changed the streams for
        // *all* of the existing interpreters!
        if (!interpreters.isEmpty()) {
            PythonInterpreter i = interpreters.get(0);
            PySystemState sysState = i.getSystemState();
            
            PyObject defaultErr = sysState.__stderr__;
            PyObject defaultOut = sysState.__stdout__;
            
            if (!defaultErr.equals(sysState.stderr)) {
                i.setErr(defaultErr);
            }
            if (!defaultOut.equals(sysState.stdout)) {
                i.setOut(defaultOut);
            }
        }
    }
    
    /**
     * Create a Jython shell
     *
     * @return shell
     */
    public JythonShell createShell() {
        return new JythonShell(getIdv());
    }
    
    /**
     * Add the interpreter into the list of interpreters. Also
     * calls {@link #initInterpreter(PythonInterpreter)}
     *
     * @param interp The interpter to add and intialize
     */
    private void addInterpreter(PythonInterpreter interp) {
        interpreters.add(interp);
        initInterpreter(interp);
    }
    
    /**
     * Remove the interpreter from the list of interpreters.
     *
     * @param interp The interpreter to remove
     */
    public void removeInterpreter(PythonInterpreter interp) {
        interpreters.remove(interp);
        resetOutputStreams();
    }
    
    /**
     * Have the given interpreter evaluate the
     * contents of each valid  Jython library defined in the given resources
     *
     * @param interp The interpreter to use
     * @param resources The set of jython library resources
     */
    private void applyJythonResources(PythonInterpreter interp, ResourceCollection resources) {
        if ((resources == null) || (interp == null)) {
            return;
        }
        for (int i = resources.size() - 1; i >= 0; i--) {
            String resourceName = resources.get(i).toString();
            InputStream is = null;
            try {
                is = getInputStream(resourceName, getClass());
            } catch (Exception exc) {}
            if (is == null) {
                continue;
            }
            try {
                interp.execfile(is, resourceName);
            } catch (PySyntaxError pse) {
                //Check for  html coming back instead of jython
                if (resources.isHttp(i)) {
                    String result = resources.read(i, false);
                    if ((result == null) || Misc.isHtml(result)) {
                        continue;
                    }
                }
                userErrorMessage("Syntax error in the Jython library:" + resourceName + '\n' + pse);
                inError = true;
            } catch (Exception exc) {
                logException("An error occurred reading Jython library: "+ resourceName, exc);
                inError = true;
            }
        }
    }
    
    /**
     * Any errors
     *
     * @return in error
     */
    public boolean getInError() {
        return inError;
    }

    /**
     * Initializes a given {@link PythonInterpreter} so that it can either be
     * used to {@link PythonInterpreter#exec exec} a Jython Library module or
     * set up an interactive Jython Shell.
     *
     * @param interpreter Interpreter to initialize. Cannot be {@code null}.
     * @param isInteractive Whether or not {@code interpreter} will be used with
     * a {@literal "Jython Shell"}.
     */
    protected void initJythonEnvironment(PythonInterpreter interpreter,
                                         boolean isInteractive)
    {
        long start = System.nanoTime();
        // TODO(jon): has to be a better approach
        PySystemState pyState = interpreter.getSystemState();
        pyState.setdefaultencoding("utf-8");

        // this line *must* be the first thing set, otherwise console_init.py
        // will assume it is being run in "interactive" mode.
        interpreter.set("_isInteractive", isInteractive);

        try (InputStream consoleInitializer =
                 IOUtil.getInputStream(CONSOLE_INIT, JythonManager.class))
        {
            interpreter.execfile(consoleInitializer, CONSOLE_INIT);
            interpreter.set("idv", getIdv());
            interpreter.set("_idv", getIdv());
            interpreter.set("interpreter", interpreter);
            interpreter.set("datamanager", getDataManager());
            interpreter.set("installmanager", getInstallManager());
            PyObject locals = interpreter.getLocals();
            if (locals instanceof LoudPyStringMap) {
                ((LoudPyStringMap)locals).bake();
            }
        } catch (IOException e) {
            logException("Failed to initialize Jython's sys.path.", e);
        } finally {
            // trying to get an *idea* of which parts of mcv are slow
            // JMH is the correct approach
            long stop = System.nanoTime();
            String id = Integer.toHexString(interpreter.hashCode());
            long duration = stop - start;
            if (interpreterTimes.containsKey(id)) {
                interpreterTimes.put(id, interpreterTimes.get(id) + duration);
            } else {
                interpreterTimes.put(id, duration);
            }
            logger.trace("jython interpreter '{}' initialization took {} ms",
                         id,
                         String.format("%.2f", duration / 1.0e6));
        }
    }
    
    /**
     * Return the total amount of time spent in 
     * {@link #initJythonEnvironment(PythonInterpreter, boolean)}.
     * 
     * <p><b>The elapsed time is merely a quick estimate.</b> The only way to
     * obtain accurate timing information with the JVM is using
     * <a href="https://openjdk.java.net/projects/code-tools/jmh/">JMH</a>.</p>
     * 
     * @return Nanoseconds spent initializing 
     *         {@link PythonInterpreter Jython Interpreters}.
     */
    public long getElapsedInterpreterInit() {
        return interpreterTimes.values().stream().mapToLong(l -> l).sum();
    }
    
    /**
     *  Initialize the given interpreter. Add in variables for "idv" and "datamanager"
     *  If initVisadLibs is true then load in the visad libs, etc.
     *  We have that check here so some interpreters that don't need the visad libs
     *  (e.g., the main ui interpreter for the idv)  don't have to suffer the
     *  overhead of loading those pkgs.
     *
     * @param interpreter The interpreter to initialize
     */
    private void initInterpreter(PythonInterpreter interpreter) {
        initJythonEnvironment(interpreter, true);
        if (DerivedDataDescriptor.classes != null) {
            for (int i = 0; i < DerivedDataDescriptor.classes.size(); i++) {
                String c = (String)DerivedDataDescriptor.classes.get(i);
                //                seenPaths.put (c, c);
                int i1 = c.lastIndexOf('.');
                String pkg = c.substring(0, i1);
                String className = c.substring(i1 + 1);
                interpreter.exec(new StringBuilder("sys.add_package('").append(pkg).append("')").toString());
                interpreter.exec(new StringBuilder("from ").append(pkg).append(" import ").append(className).toString());
            }
        }
        //        applyJythonResources(
        //            interpreter,
        //            getResourceManager().getResources(IdvResourceManager.RSC_JYTHON));
        for (int i = libHolders.size() - 1; i >= 0; i--) {
            LibHolder holder = libHolders.get(i);
            //            if(!holder.isEditable()) continue;
            String jython = holder.getText();
            interpreter.exec(jython);
        }
    }
    
    /**
     * Get the end user edited text from the jython editor.
     *
     * @return The end user jython code
     */
    public String getUsersJythonText() {
        getContents();
        return mainHolder.getText();
    }
    
    /**
     * Append the given jython to the temp jython
     *
     * @param jython The jython from the bundle
     */
    public void appendTmpJython(String jython) {
        String oldJython = tmpHolder.getText();
        if (oldJython.indexOf(jython) < 0) {
            tmpHolder.setText(
                new StringBuilder(oldJython)
                    .append("\n\n## Imported jython from bundle\n")
                    .append(jython).toString());
        }
        evaluateLibJython(false, tmpHolder);
    }
    
    /**
     * Append the given jython to that is from a bundle to the users jython
     *
     * @param jython The jython from the bundle
     */
    public void appendJythonFromBundle(String jython) {
        String oldJython = getUsersJythonText();
        //Don't add it if we have it.
        if (oldJython.indexOf(jython) >= 0) {
            return;
        }
        mainHolder.setText(
            new StringBuilder(oldJython)
            .append("\n\n## Imported jython from bundle\n")
            .append(jython).toString());
        writeJythonLib(mainHolder);
    }
    
    /**
     * APpend jython to main editable lib
     *
     * @param jython jython
     */
    public void appendJython(String jython) {
        mainHolder.setText(
            new StringBuilder(getUsersJythonText())
                .append("\n\n\n")
                .append(jython).toString());
        show();
        GuiUtils.showComponentInTabs(mainHolder.outerContents);
    }
    
    /**
     *  Have all of the interpreters evaluate the libraries
     *
     * @param forWriting Is this evaluation intended to be for when
     * we write the users jython
     * @param holderToWrite lib
     *
     * @return Was this successful
     */
    private boolean evaluateLibJython(boolean forWriting, LibHolder holderToWrite) {
        boolean ok   = false;
        String what = "";
        try {
            if (interpreters.isEmpty()) {
                getDerivedDataInterpreter();
            }
            List holders = Misc.newList(holderToWrite);
            for (int i = 0; i < holders.size(); i++) {
                LibHolder holder = (LibHolder)holders.get(i);
                if (!holder.isEditable()) {
                    continue;
                }
                String jython = holder.getText();
                for (PythonInterpreter interpreter : interpreters) {
                    interpreter.exec(jython);
                }
            }
            ok = true;
        } catch (PySyntaxError pse) {
            try {
                if (forWriting) {
                    if (GuiUtils.showYesNoDialog(null,
                            "There was an error in the Jython library:"
                            + pse, "Jython error", "Save anyways",
                                   "Cancel")) {
                        return true;
                    }
                } else {
                    if (pse.toString().contains("expecting DEDENT")) {
                        logException("McIDAS-V could not automatically add \""+ holderToWrite.filePath+"\" to your Jython Library due to a syntax error.\n\nA somewhat common problem is text editors using both tab and space characters as indentation.\n\n\nOriginal Exception:\n", pse);
                    } else {
                        logException("There was an error in the Jython library:" + pse, pse);
                    }
                    
                }
            } catch (Throwable exc) {
                logException("Writing Jython library " + exc.getClass().getName(), exc);
            }
        } catch (Throwable exc) {
            logException("Writing Jython library " + exc.getClass().getName(), exc);
        }
        return ok;
    }
    
    /**
     * Save the end user jython code from the jython editor into
     * the user's .unidata/idv area.
     * 
     * @param holder lib
     * @return success
     */
    public boolean writeJythonLib(LibHolder holder) {
        if (evaluateLibJython(true, holder)) {
            try {
                writeFile(holder.filePath, holder.getText());
                if (holder.saveBtn != null) {
                    holder.saveBtn.setEnabled(false);
                }
                return true;
            } catch (Throwable exc) {
                logException("Writing jython library " + exc.getClass().getName(), exc);
            }
        }
        return false;
    }
    
    /**
     * Make sure the given jython code matches the pattern (after removing whitespace):
     * idv.procedure_name ('arg1', arg2, ..., argn)
     * where if an arg is not in single quotes it cannot contain
     * a procedure call.
     * <p>
     * We have this here so (hopefully) a user won't inadvertently execute
     * rogue jython code  on their machine.
     * 
     * @param jython The code
     * @return Does the code  just call into idv or datamanager methods.
     */
    protected static boolean checkUntrustedJython(String jython) {
        jython = removeWhitespace(jython);
        String argPattern  = "([^()',]+|'[^']*')";
        String argsPattern = "((" + argPattern + ",)*" + argPattern + "?)";
        String pattern = "^((idv|datamanager).[^\\s(]+\\(" + argsPattern + "\\);?)+$";
        if (!stringMatch(jython, pattern)) {
            pattern = "^(idv.get[a-zA-Z]+\\(\\).[^\\s(]+\\(" + argsPattern + "\\);?)+$";
            return stringMatch(jython, pattern);
        }
        return true;
    }
    
    /**
     * Evaluate the given jython code. This code is untrusted  and has to be
     * of the form (idv|datamanager).some_method (param1, param2, ..., paramN);
     * 
     * @param jythonCode The code to execute
     */
    public void evaluateUntrusted(String jythonCode) {
        evaluateUntrusted(jythonCode, null);
    }
    
    /**
     * Evaluate the given jython code. This code is untrusted  and has to be
     * of the form (idv|datamanager).some_method (param1, param2, ..., paramN);
     * 
     * @param jythonCode The code to execute
     * @param properties If non-null then populate the interpreter with the name/value pairs
     */
    public void evaluateUntrusted(String jythonCode, Map<String, Object> properties) {
        if (!checkUntrustedJython(jythonCode)) {
            userMessage("Malformed Jython code:\n" + jythonCode);
            return;
        }
        evaluateTrusted(jythonCode, properties);
    }
    
    /**
     * Interpret the given jython code. This code is trusted, i.e.,
     * it is not checked to make sure it is only calling idv or datamanager
     * methods.
     * 
     * @param code The code toe evaluate
     */
    public void evaluateTrusted(String code) {
        evaluateTrusted(code, null);
    }
    
    /**
     * Interpret the given jython code. This code is trusted, i.e.,
     * it is not checked to make sure it is only calling idv or datamanager
     * methods.
     *
     * @param code The code toe evaluate
     * @param properties If non-null then populate the interpreter with the name/value pairs
     */
    public void evaluateTrusted(String code, Map<String, Object> properties) {
        PythonInterpreter interp = getUiInterpreter();
        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                interp.set(entry.getKey(), entry.getValue());
            }
        }
        interp.exec(code);
    }

    /**
     * Largely the same as {@link #evaluateTrusted(String, Map)}.
     *
     * <p>The key difference is that prior to running {@code action}, this
     * method will check the namespace of its Jython interpreter for the
     * {@literal "idv"} object. If not found, {@literal "idv"} is added to the
     * namespace.</p>
     *
     * <p>The idea is to try to mitigate some issues we've seen
     * (McV Inquiry 2109).</p>
     *
     * @param code Action code to evaluate.
     * @param properties Any properties
     */
    public void evaluateAction(String code, Map<String, Object> properties) {
        PythonInterpreter interp = getUiInterpreter();
        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                interp.set(entry.getKey(), entry.getValue());
            }
        }
        if (code.startsWith("idv.")) {
            PyObject locals = interp.getLocals();
            PyStringMap map = (PyStringMap)locals;
            if (!map.has_key("idv")) {
                logger.warn("interpreter does not have 'idv'! adding it...");
                PyObject mcvRef = getMcvReference(this);
                if (mcvRef != null) {
                    interp.set("idv", mcvRef);
                }
            } else {
                boolean result = getStaticMcv() == map.__getitem__("idv").__tojava__(IntegratedDataViewer.class);
                logger.trace("interpreter has idv; is value same as getStaticMcv? result={}", result);
                if (!result) {
                    logger.trace("idv was not the expected value! replacing with output of getStaticMcv()!");
                    PyObject mcvRef = getMcvReference(this);
                    if (mcvRef != null) {
                        interp.set("idv", mcvRef);
                    }
                }
            }
        }
        interp.exec(code);
    }

    /**
     * Attempt to find a valid reference to the object used as a representation
     * of the application session.
     *
     * @param jyManager Jython manager for the session. Cannot be {@code null}.
     *
     * @return The result of {@link McIDASV#getStaticMcv()},
     * {@link IntegratedDataViewer#getIdv()}, or {@code null} if the previous
     * two methods failed.
     */
    private static PyObject getMcvReference(JythonManager jyManager) {
        PyObject result = null;
        McIDASV mcv = getStaticMcv();
        if (mcv != null) {
            result = Py.java2py(mcv);
        } else {
            logger.warn("getStaticMcv() returned null! Trying getIdv()...");
            IntegratedDataViewer idv = jyManager.getIdv();
            if (idv != null) {
                result = Py.java2py(idv);
            } else {
                logger.warn("getIdv() failed as well! Nothing left...");
            }
        }
        return result;
    }

    /**
     * Create (if needed) and initialize a Jython interpreter. The initialization is to map
     * the variable "idv" to this instance of the idv and map "datamanager" to the DataManager
     * 
     * @return The interpreter to be used for theUI
     */
    public PythonInterpreter getUiInterpreter() {
        if (uiInterpreter == null) {
//            uiInterpreter = new PythonInterpreter(new LoudPyStringMap("getUiInterpreter"));
//            logger.debug("111 created new PythonInterpreter(new LoudPyStringMap())");
            uiInterpreter = new PythonInterpreter();
            addInterpreter(uiInterpreter);
            //            initBasicInterpreter(uiInterpreter);
        }
        return uiInterpreter;
    }
    
    /**
     * Update derived needs when the DataGroups change
     */
    public void dataGroupsChanged() {
        for (int dddIdx = 0; dddIdx < descriptors.size(); dddIdx++) {
            descriptors.get(dddIdx).updateDataGroups();
        }
    }
    
    /**
     * Initialize the {@link ucar.unidata.data.DerivedDataDescriptor}s
     * that are defined in the RSC_DERIVED  resource collection
     * from the given resource manager.
     *
     * @param newIrm The resource manager to get the derived resources from
     */
    protected void initUserFormulas(IdvResourceManager newIrm) {
        if (descriptorDataSource != null) {
            return;
        }
        try {
            descriptors = DerivedDataDescriptor.init(getIdv(),
                    newIrm.getXmlResources(newIrm.RSC_DERIVED));
            if (descriptors.isEmpty()) {
                return;
            }
            descriptorDataSource = new DescriptorDataSource("Formulas", "Formulas");
            for (int dddIdx = 0; dddIdx < descriptors.size(); dddIdx++) {
                // add all to the global list
                descriptorDataSource.addDescriptor(descriptors.get(dddIdx));
            }
        } catch (Throwable exc) {
            logException("Initializing user formulas", exc);
        }
    }
    
    /**
     * Popup dialog to select formulas
     *
     * @return List of selected formulas
     */
    protected List selectFormulas() {
        Vector<TwoFacedObject> formulas = new Vector<>(descriptors.size());
        for (DerivedDataDescriptor ddd : descriptors) {
            DataCategory cat = ddd.getDisplayCategory();
            String label = "";
            if (cat != null) {
                label = cat.toString(">") + ">";
            }
            label += ddd.getDescription();
            formulas.add(new TwoFacedObject(label, ddd));
        }
        JList<TwoFacedObject> formulaList = new JList<>(formulas);
        JScrollPane scroller = GuiUtils.makeScrollPane(formulaList, 200, 300);
        JPanel contents =
            GuiUtils.topCenter(
                inset(
                    GuiUtils.cLabel(
                        "Please select the formulas you would like to export"), 4), scroller);
        if (!GuiUtils.showOkCancelDialog(null, "Export Formulas", GuiUtils.inset(contents, 5), null)) {
            return null;
        }
        List<TwoFacedObject> items = formulaList.getSelectedValuesList();
        if (items.isEmpty()) {
            return null;
        }
        List<Object> selected = new ArrayList<>(items.size());
        for (TwoFacedObject tfo : items) {
            selected.add(tfo.getId());
        }
        return selected;
    }
    
    /**
     * Export selected formulas to plugin
     */
    public void exportFormulasToPlugin() {
        List<?> selected = selectFormulas();
        if ((selected == null) || selected.isEmpty()) {
            return;
        }
        getIdv().getPluginManager().addObject(selected);
    }
    
    /**
     * Export user formulas
     */
    public void exportFormulas() {
        List<?> selected = selectFormulas();
        if ((selected == null) || selected.isEmpty()) {
            return;
        }
        String xml = DerivedDataDescriptor.toXml(selected);
        String filename = FileManager.getWriteFile(FILTER_XML, SUFFIX_XML);
        if (filename == null) {
            return;
        }
        try {
            writeFile(filename, xml);
        } catch (Exception exc) {
            logException("Writing file: " + filename, exc);
        }
    }
    
    /**
     * Import user formulas
     */
    public void importFormulas() {
        String filename = FileManager.getReadFile(FILTER_XML);
        if (filename == null) {
            return;
        }
        try {
            Element root = getRoot(filename, getClass());
            // Descriptor list from file
            List fileDescriptors = DerivedDataDescriptor.readDescriptors(getIdv(), root, true);
            // List of current local descriptors
            List localDescriptors = getLocalDescriptors();
            for (int i = 0; i < fileDescriptors.size(); i++) {
                DerivedDataDescriptor ddd = (DerivedDataDescriptor) fileDescriptors.get(i);
                if (! localDescriptors.contains(ddd)) {
                    addFormula(ddd);
                }
            }
            writeUserFormulas();
            getIdvUIManager().dataSourceChanged(descriptorDataSource);
        } catch (Exception exc) {
            logException("Importing  formulas", exc);
        }
    }
    
    /**
     * Save the user created  formulas.
     */
    protected void writeUserFormulas() {
        try {
            String filename =
                getResourceManager().getXmlResources(
                    IdvResourceManager.RSC_DERIVED).getWritable();
            String xml = DerivedDataDescriptor.toXml(getLocalDescriptors());
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(xml.getBytes());
            fos.close();
        } catch (Throwable exc) {
            logException("Writing user formulas", exc);
        }
    }
    
    /**
     * Create the list of menu items for editing a data choice
     * that represents an end user formula.
     * 
     * @param dataChoice The end user formula data choice
     * @param items List of menu items to add to
     */
    public void doMakeDataChoiceMenuItems(DataChoice dataChoice, List<JMenuItem> items) {
        final DerivedDataDescriptor ddd = ((DerivedDataChoice)dataChoice).getDataDescriptor();
        final IntegratedDataViewer idv = getIdv();
        JMenuItem mi;
        items.add(mi = new JMenuItem("Edit Formula"));
        mi.addActionListener(new ObjectListener(dataChoice) {
            @Override public void actionPerformed(ActionEvent ev) {
                DerivedDataDescriptor tmp = ddd;
                if (!ddd.getIsLocalUsers()) {
                    tmp = new DerivedDataDescriptor(ddd);
                }
                showFormulaDialog(tmp);
            }
        });
        items.add(mi = new JMenuItem("Copy Formula"));
        mi.addActionListener(new ObjectListener(dataChoice) {
            @Override public void actionPerformed(ActionEvent ev) {
                showFormulaDialog(new DerivedDataDescriptor(ddd));
            }
        });
        if (ddd.getIsLocalUsers()) {
            items.add(mi = new JMenuItem("Remove Formula"));
            mi.addActionListener(new ObjectListener(dataChoice) {
                @Override public void actionPerformed(ActionEvent ev) {
                    removeFormula((DerivedDataChoice)theObject);
                }
            });
        }
        mi = new JMenuItem("Evaluate Formula");
        mi.addActionListener(new ObjectListener(dataChoice) {
            @Override public void actionPerformed(ActionEvent ev) {
                Misc.run(new Runnable() {
                    @Override public void run() {
                        evaluateDataChoice((DataChoice)theObject);
                    }
                });
            }
        });
        items.add(mi);
        mi = new JMenuItem("Evaluate and Save");
        mi.addActionListener(new ObjectListener(dataChoice) {
            @Override public void actionPerformed(ActionEvent ev) {
                Misc.run(new Runnable() {
                    @Override public void run() {
                        idv.evaluateAndSave((DataChoice)theObject);
                    }
                });
            }
        });
        items.add(mi);
        items.add(makeMenuItem("Export to Plugin", idv.getPluginManager(), "addObject", ddd));
        //items.add(GuiUtils.MENU_SEPARATOR);
    }
    
    /**
     * Delete the data choice if it is a user formula
     * 
     * @param dataChoice The data choice to delete
     */
    public void deleteKeyPressed(DataChoice dataChoice) {
        if (!(dataChoice instanceof DerivedDataChoice)) {
            return;
        }
        DerivedDataDescriptor ddd =
            ((DerivedDataChoice)dataChoice).getDataDescriptor();
            
        if (ddd.getIsLocalUsers()) {
            removeFormula((DerivedDataChoice)dataChoice);
        }
    }
    
    /**
     * This simply clones the given data choice and calls getData
     * on it. We have this here so the user can explicitly, through the
     * GUI, evaluate a formula data choice. This way they
     * don't have to  create a display to simply evaluate a formula.
     * 
     * @param dataChoice The data chocie to evaluate
     */
    public void evaluateDataChoice(DataChoice dataChoice) {
        DataChoice clonedDataChoice = dataChoice.createClone();
        showWaitCursor();
        try {
            clonedDataChoice.getData(new DataSelection());
        } catch (DataCancelException exc) {}
        catch (Exception exc) {
            logException("Evaluating data choice", exc);
        }
        showNormalCursor();
    }
    
    /**
     * Remove a formula from the IDV.  You can only remove end user
     * formulas that are in the editable list.
     * 
     * @param dataChoice  formula data choice
     */
    public void removeFormula(DerivedDataChoice dataChoice) {
        // we can only remove end user formulas
        removeFormula(dataChoice.getDataDescriptor());
    }
    
    /**
     * Remove formula
     *
     * @param ddd ddd
     */
    public void removeFormula(DerivedDataDescriptor ddd) {
        if (ddd.getIsLocalUsers()) {
            descriptors.remove(ddd);
            descriptorDataSource.removeDescriptor(ddd);
            writeUserFormulas();
            getIdvUIManager().dataSourceChanged(descriptorDataSource);
        } else {
            userMessage("Can't remove a system formula");
        }
    }
    
    /**
     * Called when a formula data choice has changed (i.e.,
     * added, removed or edited.
     *
     * @param ddd descriptor for the formula.
     */
    public void descriptorChanged(DerivedDataDescriptor ddd) {
        ddd.setIsLocalUsers(true);
        if (!descriptors.contains(ddd)) {
            descriptors.add(ddd);
            descriptorDataSource.addDescriptor(ddd);
        }
        writeUserFormulas();
        getIdvUIManager().dataSourceChanged(descriptorDataSource);
    }
    
    /**
     * Add a formula to the IDV.
     *
     * @param ddd  formula descriptor
     */
    public void addFormula(DerivedDataDescriptor ddd) {
        descriptorChanged(ddd);
    }
    
    /**
     * Return the list of menu items to use when the user has clicked on a formula DataSource.
     *
     * @param dataSource The data source clicked on
     * @return List of menu items
     */
    public List<JMenuItem> doMakeFormulaDataSourceMenuItems(DataSource dataSource) {
        List<JMenuItem> menuItems = new ArrayList<>();
        menuItems.add(iconMenuItem("Create Formula", "showFormulaDialog", "/auxdata/ui/icons/formula_add.png"));
        menuItems.add(iconMenuItem("Edit Jython Library", "showJythonEditor", "/auxdata/ui/icons/EditJython16.gif"));
        menuItems.add(iconMenuItem("Import Formulas", "importFormulas", "/auxdata/ui/icons/formula_import.png"));
        menuItems.add(iconMenuItem("Export Formulas", "exportFormulas", "/auxdata/ui/icons/formula_export.png"));
        menuItems.add(iconMenuItem("Export Formulas to Plugin", "exportFormulasToPlugin", "/auxdata/ui/icons/plugin_add.png"));
        if (dataSource instanceof DescriptorDataSource) {
            menuItems.add(makeMenu("Edit Formulas", doMakeEditMenuItems((DescriptorDataSource)dataSource)));
        }
        return menuItems;
    }
    
    private JMenuItem iconMenuItem(String label, String action, String iconPath) {
        JMenuItem mi = makeMenuItem(label, this, action);
        if (GuiUtils.getIconsInMenus()) {
            mi.setIcon(GuiUtils.getScaledImageIcon(iconPath, JythonManager.class, true));
        }
        return mi;
    }
    
    /**
     * make the edit menu items for the formula data source
     *
     * @return List of menu items
     */
    public List<JMenuItem> doMakeEditMenuItems() {
        return doMakeEditMenuItems(descriptorDataSource);
    }
    
    /**
     * make the edit menu items for the given formula data source
     *
     * @param dds The formula data source
     * @return List of menu items
     */
    public List<JMenuItem> doMakeEditMenuItems(DescriptorDataSource dds) {
        List<JMenuItem> editMenuItems = new ArrayList<>();
        List<DerivedDataDescriptor> descriptors = (List<DerivedDataDescriptor>)dds.getDescriptors();
        Map<String, JMenuItem> catMenus = new HashMap<>();
        List<JMenuItem> topItems = new ArrayList<>();
        JMenu derivedMenu = null;
        for (DerivedDataDescriptor ddd : descriptors) {
            DataCategory dc = ddd.getDisplayCategory();
            JMenu catMenu = null;
            String catSoFar = "";
            JMenuItem mi = makeMenuItem(
                               GuiUtils.getLocalName(
                                   ddd.getDescription(),
                                   ddd.getIsLocalUsers()), this,
                                       "showFormulaDialog", ddd);
            if (dc == null) {
                if (ddd.getIsDefault() && !ddd.getIsEndUser()) {
                    if (derivedMenu == null) {
                        derivedMenu = new JMenu("Derived Quantities");
                    }
                    derivedMenu.add(mi);
                } else {
                    topItems.add(mi);
                }
                continue;
            }
            while (dc != null) {
                String name = dc.getName();
                catSoFar = catSoFar + '-' + name;
                JMenu tmpMenu = (JMenu)catMenus.get(catSoFar);
                if (tmpMenu == null) {
                    tmpMenu = new JMenu(name);
                    catMenus.put(catSoFar, tmpMenu);
                    if (catMenu == null) {
                        editMenuItems.add(tmpMenu);
                    } else {
                        catMenu.add(tmpMenu);
                    }
                }
                catMenu = tmpMenu;
                dc = dc.getChild();
            }
            if (catMenu == null) {
                editMenuItems.add(mi);
            } else {
                catMenu.add(mi);
            }
        }
        if (derivedMenu != null) {
            editMenuItems.add(derivedMenu);
        }
        for (JMenuItem topItem : topItems) {
            editMenuItems.add(topItem);
        }
        return editMenuItems;
    }
    
    /**
     * Show the formula dialog with no initial state.
     * We do this to create a new formula.
     */
    public void showFormulaDialog() {
        showFormulaDialog(null);
    }
    
    /**
     * get formula descriptors
     *
     * @return descriptors
     */
    public List<DerivedDataDescriptor> getDescriptors() {
        return descriptors;
    }
    
    /**
     * Get all end user formulas
     *
     * @return end user formulas
     */
    public List<DerivedDataDescriptor> getEndUserDescriptors() {
        List<DerivedDataDescriptor> formulas = new ArrayList<>(descriptors.size());
        for (DerivedDataDescriptor ddd : descriptors) {
            if (ddd.getIsEndUser()) {
                formulas.add(ddd);
            }
        }
        return formulas;
    }
    
    /**
     * Get all local descriptors
     *
     * @return local descriptors
     */
    public List<DerivedDataDescriptor> getLocalDescriptors() {
        List<DerivedDataDescriptor> formulas = new ArrayList<>(descriptors.size());
        for (DerivedDataDescriptor ddd : descriptors) {
            if (ddd.getIsLocalUsers()) {
                formulas.add(ddd);
            }
        }
        return formulas;
    }
    
    /**
     * Get all end user formulas
     * 
     * @return end user formulas
     */
    public List<DerivedDataDescriptor> getDefaultDescriptors() {
        List<DerivedDataDescriptor> formulas = new ArrayList<>(descriptors.size());
        for (DerivedDataDescriptor ddd : descriptors) {
            if (ddd.getIsDefault()) {
                formulas.add(ddd);
            }
        }
        return formulas;
    }
    
    /**
     * Show formula dialog with the given initial DDD.
     * 
     * @param descriptor The descriptor for the formula.
     */
    public void showFormulaDialog(DerivedDataDescriptor descriptor) {
        showFormulaDialog(descriptor, descriptor == null);
    }
    
    /**
     * show the formula dialog
     * 
     * @param descriptor the formula
     * @param isNew is this a new one or are we just changing it
     */
    public void showFormulaDialog(DerivedDataDescriptor descriptor, boolean isNew) {
        List<String> categories = new ArrayList<>();
        DescriptorDataSource dds = getDescriptorDataSource();
        if (dds != null) {
            List descriptors = dds.getDescriptors();
            for (int i = 0; i < descriptors.size(); i++) {
                DerivedDataDescriptor ddd = (DerivedDataDescriptor)descriptors.get(i);
                if (!ddd.getIsEndUser()) {
                    continue;
                }
                DataCategory cat = ddd.getDisplayCategory();
                if (cat != null) {
                    String catStr = cat.toString();
                    if (!categories.contains(catStr)) {
                        categories.add(catStr);
                    }
                }
            }
        }
        new FormulaDialog(getIdv(), descriptor, null, categories, isNew);
    }
    
    /**
     * Get the descriptor data source
     *
     * @return  The descriptor data source
     */
    public DescriptorDataSource getDescriptorDataSource() {
        return descriptorDataSource;
    }
    
    /** Used to synchronize when creating the derivedData interpreter */
    private static final Object MUTEX = new Object();
    
    /**
     *  We keep track of past methods that have been used so we don't have
     *  to tell the interpreter to import more than once (though perhaps
     *  the interp tracks this itself?)
     */
    private static Map<String, String> seenMethods = new HashMap<>();
    
    /**
     * We keep track of past package paths that have been used so we don't have
     * to tell the interpreter to import more than once (though perhaps
     * the interp tracks this itself?)
     */
    private static Map<String, String> seenPaths = new HashMap<>();
    
    /**
     * Create a (singleton) jython interpreter and initialize it with the set
     * of classes defined in the xml
     * 
     * @return The singleton Jython interpreter for derived data execution
     */
    public PythonInterpreter getDerivedDataInterpreter() {
        return getDerivedDataInterpreter(null);
    }
    
    /**
     * Create a (singleton) jython interpreter and initialize it with the set
     * of classes defined in the xml and (if needed) with the
     * class path represented by the methodName argument (if methodName
     * is of the form: some.package.path.SomeClass.someMethod).
     * 
     * @param methodName Used to initialize the interpreter (if non -null)
     * @return The singleton Jython interpreter for derived data execution
     */
    public PythonInterpreter getDerivedDataInterpreter(String methodName) {
        synchronized (MUTEX) {
            if (derivedDataInterpreter == null) {
                logger.debug("333 created new PythonInterpreter(new LoudPyStringMap())");
                derivedDataInterpreter = createInterpreter();
                
            }
            if ((methodName != null) && (seenMethods.get(methodName) == null)) {
                seenMethods.put(methodName, methodName);
                int i1 = methodName.lastIndexOf('.');
                int i2 = methodName.indexOf('.');
                if ((i1 >= 0) && (i2 >= 0)) {
                    String fullPath = methodName.substring(0, i1);
                    if ((i1 != i2) && (seenPaths.get(fullPath) == null)) {
                        i1 = fullPath.lastIndexOf('.');
                        String pkg = fullPath.substring(0, i1);
                        String className = fullPath.substring(i1 + 1);
                        derivedDataInterpreter.exec(new StringBuilder("sys.add_package('").append(pkg).append("')").toString());
                        derivedDataInterpreter.exec(new StringBuilder("from ").append(pkg).append(" import ").append(className).toString());
                    }
                }
            }
        }
        return derivedDataInterpreter;
    }
    
    /**
     * Make menu
     * 
     * @param object object to call
     * @param method method to call
     * @param prefix prefic
     * @return menus
     */
    public List<JMenuItem> makeProcedureMenu(final Object object, final String method, final String prefix) {
        List<LibHolder> holders = getLibHolders();
        List<JMenuItem> menuItems = new ArrayList<>(holders.size());
        for (final LibHolder libHolder : holders) {
            final JMenu menu = new JMenu(libHolder.getName());
            menu.addMenuListener(new MenuListener() {
                @Override public void menuSelected(MenuEvent e) {
                    menu.removeAll();
                    int cnt = 0;
                    for (Object[] pair : libHolder.getFunctions()) {
                        PyFunction func = (PyFunction) pair[1];
                        String s = makeCallString(func, null);
                        if ((prefix != null) && !s.startsWith(prefix)) {
                            continue;
                        }
                        JMenuItem menuItem = makeMenuItem(s, object, method, s);
                        PyObject docString = func.__doc__;
                        if (docString != Py.None) {
                            menuItem.setToolTipText("<html><pre>" + docString.toString().trim() + "</pre></html>");
                        }
                        menu.add(menuItem);
                        cnt++;
                    }
                    if (cnt == 0) {
                        menu.add(new JMenuItem("No procedures"));
                    }
                }
                
                @Override public void menuCanceled(MenuEvent e) {}
                @Override public void menuDeselected(MenuEvent e) {}
            });
            menuItems.add(menu);
        }
        return menuItems;
    }
    
    /**
     * utility
     * 
     * @param func func
     * @param props props
     * 
     * @return call string
     */
    private String makeCallString(PyFunction func, Map<String, String> props) {
        StringBuilder sb = new StringBuilder(func.__name__).append('(');
        PyTableCode tc = (PyTableCode)func.__code__;
        for (int argIdx = 0; argIdx < tc.co_argcount; argIdx++) {
            if (argIdx > 0) {
                sb.append(", ");
            }
            String param = tc.co_varnames[argIdx];
            String attrs = ((props != null)
                            ? props.get(param)
                            : "");
            if (attrs == null) {
                attrs = "";
            }
            sb.append(param).append(attrs);
        }
        return sb.append(')').toString();
    }
    
    /**
     * find methods
     * 
     * @param justList If true just the functions
     * 
     * @return A list of Object arrays. First element in array is the name of the lib. Second is the list of PyFunction-s
     */
    public List findJythonMethods(boolean justList) {
        return findJythonMethods(justList, getLibHolders());
    }
    
    /**
     * Find methods.
     * 
     * @param justList If true just the function
     * @param holders libs
     * 
     * @return A list of Object arrays. First element in array is the name of the lib. 
     * Second is the list of PyFunction-s
     */
    public List findJythonMethods(boolean justList, Collection<LibHolder> holders) {
        List result = new ArrayList();
        if (holders == null) {
            return result;
        }
        for (LibHolder libHolder : holders) {
            List<PyFunction> subItems = new ArrayList<>();
            for (Object[] pair : libHolder.getFunctions()) {
                PyFunction func = (PyFunction)pair[1];
                subItems.add(func);
            }
            if (justList) {
                result.addAll(subItems);
            } else {
                result.add(new Object[] { libHolder.getName(), subItems });
            }
        }
        return result;
    }

    /**
     * LibHolder holds all things for a single Jython library.
     */
    public static class LibHolder extends TextSearcher.TextWrapper {
        
        /** the jython manager */
        JythonManager jythonManager;
        
        /** am I editable */
        private boolean editable;
        
        /** functions in lib */
        List<Object[]> functions;
        
        /** label */
        String label;
        
        /** Widget that contains text of {@link #filePath}. */
        JythonEditor pythonEditor;
        
        /** file */
        String filePath;
        
        /** widget */
        JComponent wrapper;
        
        /** widget */
        JComponent outerContents;
        
        /** widget */
        JButton saveBtn;
        
        /** for external editing */
        Process editProcess;
        
        /**
         * Creates a {@code LibHolder} for the given file from the user's
         * Jython Library.
         * 
         * @param editable Whether or not this library accepts changes.
         * @param jythonManager Application's {@code JythonManager}.
         * @param label Label
         * @param editor Editor
         * @param filePath Path to library file.
         * @param wrapper Wrapper
         */
        public LibHolder(boolean editable, JythonManager jythonManager,
            String label, JythonEditor editor, String filePath,
            JComponent wrapper) 
        {
            this.pythonEditor = editor;
            this.editable = editable;
            this.jythonManager = jythonManager;
            this.label = label;
            this.filePath = filePath;
            this.wrapper = wrapper;
            JTextComponent textComponent = pythonEditor.getTextComponent();
            setTextComponent(textComponent);
            JComponent bottom = null;
            if (editable) {
                saveBtn = makeButton("Save", jythonManager, "writeJythonLib", this);
                bottom = saveBtn;
            }
            this.outerContents = topCenterBottom(inset(new JLabel(label), 5), wrapper, wrap(saveBtn));
            if (!editable) {
                textComponent.setEditable(false);
                textComponent.setBackground(COLOR_DISABLED);
                pythonEditor.getLineNumberComponent().setBackground(COLOR_DISABLED);
            }
        }
        
        /**
         * name
         * 
         * @return name
         */
        public String getName() {
            return label;
        }
        
        private static final AtomicInteger getFuncCount = new AtomicInteger(0);
        
        /**
         * Parse the functions if needed
         * 
         * @return functions
         */
        public List<Object[]> getFunctions() {
            if (functions == null) {

//                LoudPyStringMap stringMap =
//                    new LoudPyStringMap("getFunctions " + getFuncCount.addAndGet(1));
//                PythonInterpreter interpreter = new PythonInterpreter(stringMap);
//                logger.debug("222 created new PythonInterpreter(new LoudPyStringMap())");
                PythonInterpreter interpreter = new PythonInterpreter();
                jythonManager.initJythonEnvironment(interpreter, false);
                // bake() is here because i'm specifically curious why the
                // init stuff done in console_init.py seems to fix the problem.
                // putting bake() after the exec(getText()) call would mean that
                // the formula code would also be considered the same as
                // console_init.py.
                //stringMap.bake();
                
                try {
                    interpreter.exec(getText());
                } catch (Exception exc) {
                    logger.error("jython could not exec contents of '"+filePath+'"', exc);
                    return Collections.emptyList();
                }
                PyStringMap locals = (PyStringMap)interpreter.getLocals();
                functions = new ArrayList<>(locals.__len__());
                for (Object name : locals.keys()) {
                    String strName = name.toString();
                    // skip "private" stuff
                    if (!strName.startsWith("_")) {
                        PyObject value = locals.get(new PyString(strName));
                        // the x.isCallable() thing will allow *any* callable code;
                        // later on things barf due to PyReflectedFunction rather
                        // than PyFunction
                        //                    if (value.isCallable()) {
                        if (value instanceof PyFunction) {
                            functions.add(new Object[]{ name, value });
                        }
                    }
                }
                functions = Misc.sortTuples(functions, true);
            }
            return functions;
        }
        
        /**
         * Is this lib editable
         * 
         * @return is editable
         */
        public boolean isEditable() {
            return editable;
        }
        
        /**
         * Get text
         * 
         * @return text
         */
        public String getText() {
            return pythonEditor.getText();
        }
        
        /**
         * Set text
         * 
         * @param text text
         */
        public void setText(String text) {
            setText(text, true);
        }
        
        /**
         * Set text
         * 
         * @param text text
         * @param andEnable enable
         */
        public void setText(String text, boolean andEnable) {
            pythonEditor.setText(text);
            if (andEnable && (saveBtn != null)) {
                saveBtn.setEnabled(true);
                functions = null;
            }
        }
        
        /**
         * Copies the selected text into the system clipboard.
         */
        public void copy() {
            pythonEditor.copy();
        }
    }
}
