/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2012
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
import static ucar.unidata.util.LogUtil.userMessage;
import static ucar.unidata.util.StringUtil.join;
import static ucar.unidata.util.StringUtil.replace;
import static ucar.unidata.util.StringUtil.split;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
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

/**
 * This class provides  an interactive shell for running JYthon
 *
 * @author IDV development team
 * @version $Revision$Date: 2012/05/23 21:08:03 $
 */
public class JythonShell extends InteractiveShell {

    /** property that holds the history */
    public static final String PROP_JYTHON_SHELL_HISTORY =
        "prop.jython.shell.history";

    protected static final Logger jythonLogger = LoggerFactory.getLogger("jython");

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
    
    /** history file where all Jython commands get recorded */
    private BufferedWriter historyFile;

    /**
     * ctor
     *
     * @param theIdv idv
     */
    public JythonShell(IntegratedDataViewer theIdv) {
        super("Jython Shell");
        this.idv = theIdv;
        List<String> oldHistory = (List<String>)idv.getStore().get(PROP_JYTHON_SHELL_HISTORY);
        if (oldHistory != null) {
            history.clear();
            history.addAll(oldHistory);
        }
        
        String historyFilename = ((McIDASV)theIdv).getUserFile("jython_history");
        try {
        	// open in append mode
        	this.historyFile = new BufferedWriter(new FileWriter(historyFilename, true));
        } catch (IOException e) {
            logException("An error occurred trying to open jython_history file", e);
        }
        
        createInterpreter();
        //Create the gui
        init();
    }
    
    /**
     * The Jython shell window has been closed, so close historyFile
     */
    @Override public void close() {
    	try {
    		this.historyFile.close();
    	} catch (IOException exc) {
    		logException("An error occurred trying to close jython_history file", exc);
    	}
    	super.close();
    }

    /**
     *  print the history
     */
    public void listHistory() {
        for (int i = 0; i < history.size(); i++) {
            super.eval(history.get(i));
        }
    }

    /**
     * write the hostory
     */
    public void saveHistory() {
        IdvObjectStore store = idv.getStore();
        store.put(PROP_JYTHON_SHELL_HISTORY, history);
        store.save();
    }

    /**
     * This gets called by the base class to make the frame.
     * If you don't want this to popup then make this method a noop
     * You can access the GUI contents with the member contents
     *
     */
    @Override protected void makeFrame() {
        super.makeFrame();
        //When the window closes remove the interpreter
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (interp != null) {
                    idv.getJythonManager().removeInterpreter(interp);
                }
            }
        });
    }

    /**
     * Get the interp
     *
     * @return interp
     */
    public PythonInterpreter getInterpreter() {
        if (interp == null) {
            createInterpreter();
        }
        return interp;
    }


    /**
     * popup menu
     *
     * @param cmdFld field
     */
    public void showProcedurePopup(JTextComponent cmdFld) {
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
                String block = history.get(i);
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
            popup.show(cmdFld, 0, (int) cmdFld.getBounds().getHeight());
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
        if ((e.getKeyCode() == e.VK_M) && e.isControlDown()) {
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
        s.append("#From shell\n").append(s).append("\n\n");
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
            logException("An error occurred creating the interpeter", exc);
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
                print(output);
                output(output.replace("<", "&lt;")
                             .replace(">", "&gt;")
                             .replace("\n", "<br>")
                             .replace(" ", "&nbsp;")
                             .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"));
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
        try {
            super.clear();
            createInterpreter();
        } catch (Exception exc) {
            logException("An error occurred clearing the Jython shell", exc);
        }
    }

    /**
     * Make menu bar
     *
     * @return menu bar
     */
    @Override protected JMenuBar doMakeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        List<JMenuItem> items = new ArrayList<JMenuItem>();
        items.add(makeMenuItem("Export Commands", this, "exportHistory"));
        items.add(makeMenuItem("Save History", this, "saveHistory"));
        items.add(makeMenuItem("List History", this, "listHistory"));
        items.add(makeMenuItem("List Variables", this, "listVars"));
        menuBar.add(makeMenu("File", items));

        items.clear();
        items.add(makeMenuItem("Clear All", this, "clear"));
        items.add(makeMenuItem("Clear Output", this, "clearOutput"));
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
        List<JMenuItem> displayMenuItems = new ArrayList<JMenuItem>();
        List<ControlDescriptor> cds = idv.getControlDescriptors();
        Map<String, JMenu> catMenus = new HashMap<String, JMenu>();
        for (ControlDescriptor cd : cds) {
            JMenu catMenu = (JMenu)catMenus.get(cd.getDisplayCategory());
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
        return items;
    }

    /**
     * handle event
     *
     * @param commandFld field
     * @param e event
     */
    @Override protected void handleRightMouseClick(JTextComponent commandFld, MouseEvent e) {
        showProcedurePopup(commandFld);
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
            .replace("\n", "<br>");
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
            super.eval(jython);
            StringBuilder sb = new StringBuilder();
            for (String line : split(jython, "\n", false, false)) {
                if (line.trim().startsWith("?")) {
                    while (!line.startsWith("?")) {
                        sb.append(line.substring(0, 1));
                        line = line.substring(1);
                    }
                    line = "print " + line.trim().substring(1);
                }
                sb.append(line).append('\n');
            }

            String code = sb.toString().trim();
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
                List<DataOperand> unboundOperands = new ArrayList<DataOperand>();
                for (DataOperand operand : operands) {
                    PyObject obj = interp.get(operand.getParamName());
                    if (obj == null) {
                        unboundOperands.add(operand);
                    }
                }

                if (!unboundOperands.isEmpty()) {
                    List result = idv.selectDataChoices(unboundOperands);
                    if (result == null) {
                        return;
                    }
                    for (int i = 0; i < result.size(); i++) {
                        DataOperand operand = operands.get(i);
                        Data data = (Data)((DataChoice)result.get(i)).getData(null);
                        interp.set(operand.getParamName(), data);
                    }
                }
            }
            PythonInterpreter interp = getInterpreter();
            startBufferingOutput();
            interp.exec(sb.toString());
            endBufferingOutput();
            
            // Only write to history file if Jython is "valid" (didn't cause an exception).
            // Not sure if this is the desired behavior or not...
            // (do before interp.exec if you want to write to history no matter what.)
            historyFile.write(sb.toString());
        	historyFile.flush();
            jythonLogger.info(sb.toString());
        	
        } catch (PyException pse) {
            endBufferingOutput();
            output("<font color=\"red\">Error: " + pse.toString() + "</font><br>");
        } catch (IOException exc) {
        	logException("An error occurred trying to write to jython_history file", exc);
        } catch (Exception exc) {
            endBufferingOutput();
            output("<font color=\"red\">Error: " + exc + "</font><br>");
        }
    }

    /**
     * print type
     *
     * @param d data
     */
    public void printType(Data d) {
        try {
            startBufferingOutput();
            MathType t = d.getType();
            visad.jmet.DumpType.dumpMathType(t, outputStream);
            output("<hr>DataType analysis...");
            visad.jmet.DumpType.dumpDataType(d, outputStream);
        } catch (Exception exc) {
            logException("An error occurred printing types", exc);
        }
        endBufferingOutput();
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
}
