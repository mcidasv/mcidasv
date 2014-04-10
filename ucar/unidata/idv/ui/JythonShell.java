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

package ucar.unidata.idv.ui;

import static ucar.unidata.util.GuiUtils.makeCheckboxMenuItem;
import static ucar.unidata.util.GuiUtils.makeMenu;
import static ucar.unidata.util.GuiUtils.makeMenuItem;
import static ucar.unidata.util.LogUtil.logException;
import static ucar.unidata.util.LogUtil.registerWindow;
import static ucar.unidata.util.LogUtil.userMessage;
import static ucar.unidata.util.StringUtil.join;
import static ucar.unidata.util.StringUtil.replace;
import static ucar.unidata.util.StringUtil.split;

import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;


import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyJavaPackage;
import org.python.core.PyObject;
import org.python.core.PyReflectedFunction;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import visad.Data;
import visad.MathType;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataOperand;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.JythonManager;
import ucar.unidata.ui.InteractiveShell;
import ucar.unidata.util.GuiUtils;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.util.Contract;

/**
 * This class provides an interactive shell for running Jython.
 *
 * @author IDV development team
 * @version $Revision$Date: 2012/10/03 20:10:23 $
 */
public class JythonShell extends InteractiveShell {
    
    /** Internal logging object. */
    private static final Logger logger = 
        LoggerFactory.getLogger(JythonShell.class);
    
    /** Property that holds jython shell window location and size. */
    public static final String PROP_JYTHON_WINDOW = 
        "prop.jython.shell.windowrect";
    
    /** Property that hold location of divider bar between text input/output areas. */
    public static final String PROP_JYTHON_DIVIDER = 
        "prop.jython.shell.divider";
    
    /** Property that holds the history */
    public static final String PROP_JYTHON_SHELL_HISTORY =
        "prop.jython.shell.history";
    
    /** Property that holds the maximum length of the Jython Shell history. */
    public static final String PROP_JYTHON_SHELL_MAX_HISTORY_LENGTH =
        "prop.jython.shell.maxhistorylength";
    
    /** Property that determines whether or not the user is warned before resetting the Jython Shell. */
    public static final String PROP_JYTHON_SHELL_DISABLE_RESET_WARNING = 
        "prop.jython.shell.disableresetwarning";
    
    /** Max number of commands saved in history. */
    public static final int DEFAULT_MAX_HISTORY_LENGTH = 100;
    
    /** Jython shell window title. */
    public static final String WINDOW_TITLE = "Jython Shell";
    
    /** Logging object used for Jython-specific logging output. */
    protected static final Logger jythonLogger = 
        LoggerFactory.getLogger("jython");
    
    /** {@code true} while a shell reset is taking place, {@code false} otherwise. */
    private boolean shellResetting = false;
    
    /** idv */
    private IntegratedDataViewer idv;
    
    /** interp */
    private PythonInterpreter interp;
    
    /** output stream for interp */
    private OutputStream outputStream;
    
    /** _more_          */
    private boolean autoSelect = false;
    
    /** _more_          */
    ImageGenerator islInterpreter;

    /**
     * ctor
     *
     * @param theIdv idv
     */
    public JythonShell(IntegratedDataViewer theIdv) {
        super(WINDOW_TITLE);
        this.idv = theIdv;

        List<ShellHistoryEntry> oldHistory = convertStringHistory(idv.getStore());
        if (oldHistory != null) {
            history.clear();
            history.addAll(oldHistory);
        }
        
        createInterpreter();
        //Create the gui
        init();
    }
    
    /**
     * Run when the user has closed the Jython shell window.
     */
    @Override public void close() {
        saveWindowBounds(idv.getStore(), frame.getBounds());
        saveDividerLocation(idv.getStore(), getDividerLocation());
        super.close();
    }
    
    /**
     * Print the Jython shell history.
     */
    public void listHistory() {
        for (int i = 0; i < history.size(); i++) {
            super.eval(history.get(i).getEntryText());
        }
    }
    
    /**
     * Write Jython shell history.
     */
    public void saveHistory() {
        // trim the history array to desired length
        while (history.size() > loadMaxHistoryLength(idv.getStore(), DEFAULT_MAX_HISTORY_LENGTH)) {
            // remove the "oldest" command
            history.remove(0);
        }
        
        IdvObjectStore store = idv.getStore();
        store.put(PROP_JYTHON_SHELL_HISTORY, history);
        store.save(); // TODO: do we REALLY want to do this every command?
    }
    
    @Override public void flipField() {
        super.flipField();
        getCommandFld().requestFocusInWindow();
    }
    
    @Override public void toFront() {
        getCommandFld().requestFocusInWindow();
        GuiUtils.toFront(frame);
    }
    
    /**
     * This gets called by the base class to make the frame.
     * If you don't want this to popup then make this method a noop
     * You can access the GUI contents with the member contents
     *
     */
    @Override protected void makeFrame() {
        frame = new JFrame(WINDOW_TITLE);
        frame.getContentPane().add(contents);
        frame.pack();
        frame.setBounds(loadWindowBounds(idv.getStore(), new Rectangle(100, 100, 600, 500)));
        frame.setVisible(true);
        registerWindow(frame);
        setDividerLocation(loadDividerLocation(idv.getStore(), getDividerLocation()));
        
        frame.addWindowFocusListener(new WindowFocusListener() {
            @Override public void windowGainedFocus(WindowEvent event) {
                getCommandFld().requestFocusInWindow();
            }
            @Override public void windowLostFocus(WindowEvent event) {
            }
        });
        
        // when the window closes remove the interpreter
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (interp != null) {
                    idv.getJythonManager().removeInterpreter(interp);
                }
//                saveWindowBounds(idv.getStore(), frame.getBounds());
            }
        });
    }
    
    /**
     * Get the interpreter. Note: if {@link #interp} is {@code null}, this 
     * method will call {@link #createInterpreter()} to create a new 
     * {@link PythonInterpreter}. 
     *
     * @return Active {@code PythonInterpreter}.
     */
    public PythonInterpreter getInterpreter() {
        if (interp == null) {
            createInterpreter();
        }
        return interp;
    }
    
    /**
     * Popup menu just outside of the command field widget(s).
     *
     * @param cmdFld field
     */
    public void showProcedurePopup(JTextComponent cmdFld) {
        showProcedurePopup(cmdFld, 0, (int) cmdFld.getBounds().getHeight());
    }

    /**
     * Popup command field menu wherever the user clicked.
     *
     * @param cmdFld Command field widget.
     * @param xPos X coordinate for the popup menu.
     * @param yPos Y coordinate for the popup menu.
     */
    public void showProcedurePopup(JTextComponent cmdFld, int xPos, int yPos) {
        String t = cmdFld.getText();
        /*
          int pos = cmdFld.getCaretPosition();
          t = t.substring(0, pos);
          String tmp = "";
          for(int i=t.length()-1;i>=0;i--) {
          char c = t.charAt(i);
          if(!Character.isJavaIdentifierPart(c)) break;
          tmp = c+tmp;
          }
          t=tmp;
          if(t.length()==0) {
          t = null;
          }
          //            System.err.println(t);
          */
        t = null;
        
        List<JMenuItem> items = new ArrayList<JMenuItem>();
        if (history.isEmpty()) {
            List<JMenuItem> historyItems = new ArrayList<JMenuItem>();
            for (int i = history.size() - 1; i >= 0; i--) {
                String block = history.get(i).getEntryText();
                historyItems.add(
                    makeMenuItem(
                        block, this, "eval",
                        block));
            }
            items.add(makeMenu("History", historyItems));
        }
        
        items.add(makeMenu("Insert Procedure Call", idv.getJythonManager().makeProcedureMenu(this, "insertText", t)));
        
        JMenu dataMenu = makeMenu("Insert Data Source Type", getDataMenuItems());
        GuiUtils.limitMenuSize(dataMenu, "Data Source Types", 10);
        items.add(dataMenu);
        items.add(makeMenu("Insert Display Type", getDisplayMenuItems()));
        items.add(makeMenu("Insert Idv Action", idv.getIdvUIManager().makeActionMenu(this, "insertText", true)));
        
        JPopupMenu popup = GuiUtils.makePopupMenu(items);
        if (popup != null) {
            popup.show(cmdFld, xPos, yPos);
        }
    }
    
    /**
     * List the variables in the interpreter
     */
    public void listVars() {
        PyStringMap locals = (PyStringMap)getInterpreter().getLocals();
        StringBuilder sb = new StringBuilder("Variables:<br>");
        for (Object key : locals.keys()) {
            String name = key.toString();
            PyObject obj = locals.get(new PyString(name));
            if ((obj instanceof PyFunction)
                    || (obj instanceof PyReflectedFunction)
                //                    || (obj instanceof PyJavaClass)
                    || (obj instanceof PyJavaPackage)
                    || (obj instanceof PySystemState)
                    || (obj instanceof PyJavaPackage)
                    || name.startsWith("__") || "JyVars".equals(name)) {
                continue;
            }
            sb.append("&nbsp;&nbsp;&nbsp;").append(name).append("<br>");
        }
        output(sb.toString());
    }
    
    /**
     * Add the idv action
     *
     * @param action action
     */
    public void insertAction(String action) {
        insertText(new StringBuilder("idv.handleAction('action:").append(action).append("')").toString());
    }
    
    /**
     * handle event
     *
     * @param e event
     * @param cmdFld field
     */
    @Override protected void handleKeyPress(KeyEvent e, JTextComponent cmdFld) {
        super.handleKeyPress(e, cmdFld);
        if ((e.getKeyCode() == KeyEvent.VK_M) && e.isControlDown()) {
            showProcedurePopup(cmdFld);
        }
    }
    
    /**
     * show help
     */
    public void showHelp() {
        idv.getIdvUIManager().showHelp("idv.tools.jythonshell");
    }
    
    /**
     * Take all of the commands and write them to the library
     */
    public void exportHistory() {
        if (history.isEmpty()) {
            userMessage("There are no commands to export");
            return;
        }
        String procedureName =
            GuiUtils.getInput("Enter optional procedure name",
                              "Procedure name: ", "",
                              " (Leave blank for no procedure)");
        if (procedureName == null) {
            return;
        }
        StringBuilder s;
        if (procedureName.trim().length() == 0) {
            s = new StringBuilder(join("\n", history));
        } else {
            s = new StringBuilder("def ").append(procedureName).append("():\n    ").append(join("\n    ", history));
        }
        s.append("#From shell\n").append("\n\n");
        idv.getJythonManager().appendJython(s.toString());
    }
    
    /**
     * create interp
     */
    private void createInterpreter() {
        JythonManager jythonManager = idv.getJythonManager();
        if (interp != null) {
            jythonManager.removeInterpreter(interp);
        }
        try {
            interp = jythonManager.createInterpreter();
        } catch (Exception exc) {
            logException("An error occurred creating the interpreter", exc);
            return;
        }
        if (islInterpreter == null) {
            islInterpreter = new ImageGenerator(idv);
        }
        
        interp.set("islInterpreter", islInterpreter);
        interp.set("shell", this);
        outputStream = new OutputStream() {
            @Override public void write(byte[] b, int off, int len) {
                String output = new String(b, off, len);
                if (len < 8192) {
                    // only print "short" output. This combats problem of the Jython
                    // Shell effectively locking up when user prints a large data object.
                    // I use 8192 as a threshold because this is apparently the max
                    // that the PythonInterpreter will send to it's OutputStream at a 
                    // single time...not sure how to choose this threshold more intelligently.
                    print(output);
                    output(output.replace("<", "&lt;")
                                 .replace(">", "&gt;")
                                 .replace("\n", "<br/>")
                                 .replace(" ", "&nbsp;")
                                 .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"));
                    // end buffering (important if we're getting here after hitting big output chunks).
                    endBufferingOutput();
                } else {
                    // we've hit a big chunk of output; start buffering!
                    startBufferingOutput();
                    output(output.substring(0, 50) +
                            "<font color=\"red\">" + ".....output truncated" + "</font><br/>");
                }
            }
            private void print(final String output) {
                jythonLogger.info("{}", output);
            }
            @Override public void write(int b) {}
        };
        interp.setOut(outputStream);
        interp.setErr(outputStream);
    }
    
    /**
     * Clear everything, gui and make new interp
     */
    @Override public void clear() {
        IdvObjectStore store = idv.getStore();
        boolean disableWarning = store.get(PROP_JYTHON_SHELL_DISABLE_RESET_WARNING, false);
        boolean resetConfirmed = false;
        int result = -1;
        
        if (!disableWarning) {
            String[] options = { "Reset Jython Shell", "Cancel" };
            JCheckBox box = new JCheckBox("Turn off this warning?", false);
            JComponent comp = GuiUtils.vbox(new JLabel("<html>Variables created within the Jython Shell <b>cannot</b> be recovered after a reset. Would you like to proceed?</html>"), GuiUtils.inset(box, new Insets(4, 15, 0, 10)));
            result = JOptionPane.showOptionDialog(frame, comp, "Confirm Jython Shell Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, (Icon)null, options, null);
            store.put(PROP_JYTHON_SHELL_DISABLE_RESET_WARNING, box.isSelected());
            resetConfirmed = (result == 0);
        } else {
            resetConfirmed = true;
        }
        
        if (resetConfirmed) {
            shellResetting = true;
            try {
                // regrettably the following code does NOT clean up the heap :(
                //Object o = getInterpreter().getLocals();
                //if ((o != null) && (o instanceof PyStringMap)) {
                //    PyStringMap map = (PyStringMap)o;
                //    map.clear();
                //}
                
                // Yes, I know calling System.gc() is bad form. 
                // My justifications: 
                // * McIDAS-V users frequently work with large datasets. 
                //   When they need to reclaim heap space they *truly* 
                //   do need it.
                // * Jython Shell resets are only triggered by the user 
                //   explicitly requesting a reset (via GUI or script).
                // * Resets are rare, and McIDAS-V uses the CMS GC algorithm in
                //   an attempt to minimize any performance hits.
                System.gc();
                super.clear();
                createInterpreter();
            } catch (Exception exc) {
                logException("An error occurred clearing the Jython shell", exc);
            }
            shellResetting = false;
        }
    }
    
    /**
     * Check for whether or not this Jython Shell is in the midst of a reset.
     * 
     * @return {@code true} if the shell is resetting, {@code false} otherwise.
     */
    public boolean isShellResetting() {
        return shellResetting;
    }
    
    /**
     * Make menu bar
     *
     * @return menu bar
     */
    @Override protected JMenuBar doMakeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        List<JMenuItem> items = new ArrayList<JMenuItem>();
        items.add(makeMenuItem("Save Commands to Jython Library", this, "exportHistory"));
        // not needed if we're "auto-saving" every command:
        //items.add(makeMenuItem("Save Commands to History", this, "saveHistory"));
        items.add(makeMenuItem("List Saved History", this, "listHistory"));
        items.add(makeMenuItem("List Current Variables", this, "listVars"));
        menuBar.add(makeMenu("File", items));
        
        items.clear();
        items.add(makeMenuItem("Reset Jython Shell", this, "clear"));
        items.add(makeMenuItem("Clear Output Buffer", this, "clearOutput"));
        items.add(makeCheckboxMenuItem("Auto-select Operands", this, "autoSelect", null));
        //        items.add(GuiUtils.makeMenu("Insert Display Type", getDisplayMenuItems()));
        menuBar.add(makeMenu("Edit", items));
        
        items.clear();
        items.add(makeMenuItem("Help", this, "showHelp"));
        menuBar.add(makeMenu("Help", items));
        return menuBar;
    }
    
    /**
     * get menu items
     *
     * @return items
     */
    protected List<JMenuItem> getDisplayMenuItems() {
        List<ControlDescriptor> cds = idv.getControlDescriptors();
        List<JMenuItem> displayMenuItems = new ArrayList<JMenuItem>(cds.size());
        Map<String, JMenu> catMenus = new HashMap<String, JMenu>(cds.size());
        for (ControlDescriptor cd : cds) {
            JMenu catMenu = catMenus.get(cd.getDisplayCategory());
            if (catMenu == null) {
                String category = cd.getDisplayCategory();
                catMenu = new JMenu(category);
                catMenus.put(category, catMenu);
                displayMenuItems.add(catMenu);
            }
            catMenu.add(makeMenuItem(cd.getDescription(), this,
                    "insert", '\'' + cd.getControlId() + '\''));
        }
        return displayMenuItems;
    }
    
    /**
     * _more_
     *
     * @return _more_
     */
    protected List<JMenuItem> getDataMenuItems() {
        List<JMenuItem> items = new ArrayList<JMenuItem>();
        for (DataSourceDescriptor descriptor : idv.getDataManager().getDescriptors()) {
            List<String> ids = split(descriptor.getId(), ",", true, true);
            String label = descriptor.getLabel();
            if ((label == null) || (label.trim().length() == 0)) {
                label = ids.get(0);
            }
            items.add(makeMenuItem(label, this, "insert", '\'' + ids.get(0) + '\''));
        }
        logger.trace("items size={}", items.size());
        return items;
    }
    
    /**
     * handle event
     *
     * @param commandFld field
     * @param e event
     */
    @Override protected void handleRightMouseClick(JTextComponent commandFld, MouseEvent e) {
        showProcedurePopup(commandFld, e.getX(), e.getY());
    }
    
    /**
     * Format code to output
     *
     * @param code code
     *
     * @return formatted code
     */
    @Override protected String formatCode(String code) {
        return replace(code.trim(), "\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
            .replace(" ", "&nbsp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br/>");
    }

    protected String formatJavaException(Exception exception) {
        String out = formatCode(exception.toString());
        return out;
    }

    protected String formatJythonException(PyException exception) {
        Throwable cause = exception.getCause();
        String out;
        if (cause != null) {
            out = exception.getCause().toString();
            if (out.startsWith("java.util.concurrent.ExecutionException: ")) {
                out = out.substring(41);
            }
        } else {
            out = exception.toString();
        }
        out = formatCode(out);
        return out;
    }

    /**
     * eval
     *
     * @param jython jython
     */
    @Override public void eval(String jython) {
        try {
            if (jython.trim().length() == 0) {
                return;
            }
            StringBuilder sb = new StringBuilder(jython.length() * 2);
//            sb.append("try:\n");
            for (String line : split(jython, "\n", false, false)) {
                if (line.trim().startsWith("?")) {
                    while (!line.startsWith("?")) {
//                        sb.append("    ");
                        sb.append(line.substring(0, 1));
                        line = line.substring(1);
                    }
                    line = "print " + line.trim().substring(1);
                }
//                sb.append("    ");
                sb.append(line).append('\n');
            }

//            sb.append("except ExecutionException, _xyz:\n    print 'auto-catch:', type(_xyz), _xyz\n");
            String code = sb.toString().trim();
            super.eval(code);
            
            if (autoSelect && !code.startsWith("import") && !code.startsWith("from")) {
                int idx;
                //Strip out any leading assignment
                while (true) {
                    idx = code.indexOf('=');
                    if (idx < 0) {
                        break;
                    }
                    code = code.substring(idx + 1);
                }
                
                List<DataOperand> operands = DerivedDataChoice.parseOperands(code);
                List<DataOperand> unboundOperands = new ArrayList<DataOperand>(operands.size());
                for (DataOperand operand : operands) {
                    PyObject obj = interp.get(operand.getParamName());
                    if (obj == null) {
                        unboundOperands.add(operand);
                    }
                }

                if (!unboundOperands.isEmpty()) {
                    List<DataChoice> result = (List<DataChoice>)idv.selectDataChoices(unboundOperands);
                    if (result == null) {
                        return;
                    }
                    for (int i = 0; i < result.size(); i++) {
                        DataOperand operand = operands.get(i);
                        Data data = result.get(i).getData(null);
                        interp.set(operand.getParamName(), data);
                    }
                }
            }
            PythonInterpreter interp = getInterpreter();
            // MJH March2013: no longer doing a startBufferingOutput here
            interp.exec(sb.toString());

            // write off history to "store" so user doesn't have to save explicitly.
            saveHistory();
        } catch (PyException pse) {
            output("<font color=\"red\">" + formatJythonException(pse) + "</font><br/>");
        } catch (Exception exc) {
            output("<font color=\"red\">" + formatJavaException(exc) + "</font><br/>");
        }
    }
    
    /**
     * print type
     *
     * @param d data
     */
    public void printType(Data d) {
        try {
            MathType t = d.getType();
            visad.jmet.DumpType.dumpMathType(t, outputStream);
            output("<hr>DataType analysis...");
            visad.jmet.DumpType.dumpDataType(d, outputStream);
        } catch (Exception exc) {
            logException("An error occurred printing types", exc);
        }
    }
    
    /**
     * Set the AutoSelect property.
     *
     * @param value The new value for AutoSelect
     */
    public void setAutoSelect(boolean value) {
        autoSelect = value;
    }
    
    /**
     * Get the AutoSelect property.
     *
     * @return The AutoSelect
     */
    public boolean getAutoSelect() {
        return autoSelect;
    }
    
    /**
     * Loads the dimensions of the Jython Shell window.
     * 
     * @param store The {@link IdvObjectStore} that contains persisted session values. Cannot be {@code null}.
     * @param defaultBounds Window bounds to use if {@code PROP_JYTHON_WINDOW_BOUNDS} does not have an associated value. Cannot be {@code null}.
     * 
     * @return Either the value associated with {@code PROP_JYTHON_WINDOW_BOUNDS} or {@code defaultBounds}.
     */
    public static Rectangle loadWindowBounds(final IdvObjectStore store, final Rectangle defaultBounds) {
        Rectangle windowBounds = (Rectangle)store.get(PROP_JYTHON_WINDOW);
        if (windowBounds == null) {
            store.put(PROP_JYTHON_WINDOW, defaultBounds);
            windowBounds = defaultBounds;
        }
        return windowBounds;
    }
    
    /**
     * Saves the dimensions of the Jython Shell window.
     * 
     * @param store The {@link IdvObjectStore} that contains persisted session values. Cannot be {@code null}.
     * @param windowBounds Window bounds to associate with {@code PROP_JYTHON_WINDOW_BOUNDS}. Cannot be {@code null}.
     */
    public static void saveWindowBounds(final IdvObjectStore store, final Rectangle windowBounds) {
        store.put(PROP_JYTHON_WINDOW, windowBounds);
    }

    /**
     * Loads the position of the bar dividing the input and output regions.
     * 
     * @param store The {@link IdvObjectStore} that contains persisted session values. Cannot be {@code null}.
     * @param defaultDividerLocation location to use if {@code PROP_JYTHON_DIVIDER} does not have an associated value. Cannot be {@code null}.
     * 
     * @return Either the value associated with {@code PROP_JYTHON_DIVIDER} or {@code defaultDividerLocation}.
     */
    public static int loadDividerLocation(final IdvObjectStore store, final int defaultDividerLocation) {
        Integer dividerLocation = (Integer)store.get(PROP_JYTHON_DIVIDER);
        if (dividerLocation == null) {
            store.put(PROP_JYTHON_DIVIDER, defaultDividerLocation);
            dividerLocation = defaultDividerLocation;
        }
        return dividerLocation;
    }

    /**
     * Saves the position of the bar dividing the input and output regions.
     * 
     * @param store The {@link IdvObjectStore} that contains persisted session values. Cannot be {@code null}.
     * @param dividerLocation distance from top of window of the horizontal divider bar separating text input/output.
     */
    public static void saveDividerLocation(final IdvObjectStore store, final int dividerLocation) {
        store.put(PROP_JYTHON_DIVIDER, dividerLocation);
    }
    
    /**
     * Loads the maximum number of items stored in the Jython Shell history.
     * 
     * @param store The {@link IdvObjectStore} that contains persisted session values. Cannot be {@code null}.
     * @param defaultLength history length to use if {@code PROP_JYTHON_SHELL_MAX_HISTORY_LENGTH} does not have an associated value. Cannot be {@code null}.
     * 
     * @return Either the value associated with {@code PROP_JYTHON_SHELL_MAX_HISTORY_LENGTH} or {@code defaultLength}.
     */
    public static int loadMaxHistoryLength(final IdvObjectStore store, final int defaultLength) {
        Integer historyLength = (Integer) store.get(PROP_JYTHON_SHELL_MAX_HISTORY_LENGTH);
        if (historyLength == null) {
            store.put(PROP_JYTHON_SHELL_MAX_HISTORY_LENGTH, defaultLength);
            historyLength = defaultLength;
        }
        return historyLength;
    }
    
    /**
     * Saves the maximum number of items stored in the Jython Shell history.
     * 
     * @param store The {@link IdvObjectStore} that contains persisted session values. Cannot be {@code null}.
     * @param historyLength history length to associate with {@code PROP_JYTHON_SHELL_MAX_HISTORY_LENGTH}. Cannot be {@code null}.
     */
    public static void saveMaxHistoryLength(final IdvObjectStore store, final int historyLength) {
        store.put(PROP_JYTHON_SHELL_MAX_HISTORY_LENGTH,  historyLength);
    }

    /**
     * Converts {@link String}-based shell history entries into
     * {@link ShellHistoryEntry ShellHistoryEntries}.
     *
     * @param store {@link IdvObjectStore} containing the shell history.
     * Cannot be {@code null}.
     *
     * @return Shell history as an {@link ArrayList} of
     * {@code ShellHistoryEntry} entries.
     */
    public static List<ShellHistoryEntry> convertStringHistory(IdvObjectStore store) {
        store = Contract.notNull(store, "Cannot use a null store");
        List<?> oldEntries = (List<?>)store.get(PROP_JYTHON_SHELL_HISTORY);
        if (oldEntries == null) {
            oldEntries = Collections.emptyList();
            logger.trace("no history to convert; using empty list");
        }
        List<ShellHistoryEntry> entries = new ArrayList<ShellHistoryEntry>(oldEntries.size());
        for (Object e : oldEntries) {
            ShellHistoryEntry entry;
            if (e instanceof String) {
                entry = new ShellHistoryEntry((String)e);
                logger.trace("convert string entry: {}", entry);
            } else if (e instanceof ShellHistoryEntry) {
                entry = (ShellHistoryEntry)e;
                logger.trace("no conversion needed: {}", entry);
            } else {
                entry = new ShellHistoryEntry(e.toString());
                logger.trace("unknown type: {}, entry: {}", e.getClass().toString(), entry);
            }
            entries.add(entry);
        }
        return entries;
    }
}
