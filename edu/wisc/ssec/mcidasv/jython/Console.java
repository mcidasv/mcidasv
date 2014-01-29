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

package edu.wisc.ssec.mcidasv.jython;

import static edu.wisc.ssec.mcidasv.util.Contract.notNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.python.core.PyJavaType;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyObjectDerived;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyTuple;
import org.python.util.InteractiveConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.util.StringUtil;

// TODO(jon): Console should become an interface. there is no reason people 
//            should have to deal with the UI stuff when they only want to use
//            an interpreter.
public class Console implements Runnable, KeyListener {

    public enum HistoryType { INPUT, SYSTEM };

    /** Color of the Jython text as it is being entered. */
    protected static final Color TXT_NORMAL = Color.BLACK;

    /** Color of text coming from {@literal "stdout"}. */
    protected static final Color TXT_GOOD = Color.BLUE;

    /** Not used just yet... */
    protected static final Color TXT_WARN = Color.ORANGE;

    /** Color of text coming from {@literal "stderr"}. */
    protected static final Color TXT_ERROR = Color.RED;

    /** {@link Logger} object for Jython consoles. */
    private static final Logger logger = LoggerFactory.getLogger(Console.class);

    /** Offset array used when actual offsets cannot be determined. */
    private static final int[] BAD_OFFSETS = { -1, -1 };

    /** Normal jython prompt. */
    private static final String PS1 = ">>> ";

    /** Prompt that indicates more input is needed. */
    private static final String PS2 = "... ";

    /** Actual {@link String} of whitespace to insert for blocks and whatnot. */
    private static final String WHITESPACE = "    ";

    /** Not used yet. */
    private static final String BANNER = InteractiveConsole.getDefaultBanner();

    /** All text will appear in this font. */
    private static final Font FONT = new Font("Monospaced", Font.PLAIN, 14);

    /** Jython statements entered by the user. */
    // TODO(jon): consider implementing a limit to the number of lines stored?
    private final List<String> jythonHistory;

    /** Thread that handles Jython command execution. */
    private Runner jythonRunner;

    /** A hook that allows external classes to respond to events. */
    private ConsoleCallback callback;

    /** Where the user interacts with the Jython interpreter. */
    private JTextPane textPane;

    /** {@link #textPane}'s internal representation. */
    private Document document;

    /** Panel that holds {@link #textPane}. */
    private JPanel panel;

    /** Title of the console window. */
    private String windowTitle = "Super Happy Jython Fun Console "+hashCode();

    private MenuWrangler menuWrangler;

    /**
     * Build a console with no initial commands.
     */
    public Console() {
        this(Collections.<String>emptyList());
    }

    /**
     * Builds a console and executes a list of Jython statements. It's been
     * useful for dirty tricks needed during setup.
     * 
     * @param initialCommands Jython statements to execute.
     */
    public Console(final List<String> initialCommands) {
        notNull(initialCommands, "List of initial commands cannot be null");
        jythonHistory = new ArrayList<String>();
        jythonRunner = new Runner(this, initialCommands);
        jythonRunner.start();
        // err, shouldn't the gui stuff be done explicitly in the EDT!?
        menuWrangler = new DefaultMenuWrangler(this);
        panel = new JPanel(new BorderLayout());
        textPane = new JTextPane() {
            @Override public void paste() {
                
                super.paste();
            }
        };
        document = textPane.getDocument();
        panel.add(BorderLayout.CENTER, new JScrollPane(textPane));
        setCallbackHandler(new DummyCallbackHandler());
        try {
            showBanner(); 
            document.createPosition(document.getLength() - 1);
        } catch (BadLocationException e) {
            logger.error("had difficulties setting up the console msg", e);
        }

        new EndAction(this, Actions.END);
        new EnterAction(this, Actions.ENTER);
        new DeleteAction(this, Actions.DELETE);
        new HomeAction(this, Actions.HOME);
        new TabAction(this, Actions.TAB);
        new PasteAction(this, Actions.PASTE);
//        new UpAction(this, Actions.UP);
//        new DownAction(this, Actions.DOWN);

        JTextComponent.addKeymap("jython", textPane.getKeymap());

        textPane.setFont(FONT);
        textPane.addKeyListener(this);
        textPane.addMouseListener(new PopupListener());
    }

    /**
     * Returns the panel containing the various UI components.
     */
    public JPanel getPanel() {
        return panel;
    }

    /**
     * Returns the {@link JTextPane} used by the console.
     */
    protected JTextPane getTextPane() {
        return textPane;
    }

    /**
     * Inserts the specified object into Jython's local namespace using the
     * specified name.
     * 
     * <p><b>Example:</b><br/> 
     * {@code console.injectObject("test", new PyJavaInstance("a test"))}<br/>
     * Allows the interpreter to refer to the {@link String} {@code "a test"}
     * as {@code test}.
     * 
     * @param name Object name as it will appear within the interpreter.
     * @param object Object to place in the interpreter's local namespace.
     */
//    public void injectObject(final String name, final PyObject pyObject) {
//        jythonRunner.queueObject(name, pyObject);
//    }
    public void injectObject(final String name, final Object object) {
        jythonRunner.queueObject(name, object);
    }

    public void ejectObjectByName(final String name) {
        jythonRunner.queueRemoval(name);
    }

    // TODO(jon): may not need this one.
    public void ejectObject(final PyObject pyObject) {
        Map<String, PyObject> locals = getLocalNamespace();
        for (Map.Entry<String, PyObject> entry : locals.entrySet()) {
            if (pyObject == entry.getValue()) {
                jythonRunner.queueRemoval(entry.getKey());
            }
        }
    }

    /**
     * Runs the file specified by {@code path} in the {@link Interpreter}.
     * 
     * @param name {@code __name__} attribute to use for loading {@code path}.
     * @param path The path to the Jython file.
     */
    public void runFile(final String name, final String path) {
        jythonRunner.queueFile(name, path);
    }

    /**
     * Displays non-error output.
     * 
     * @param text The message to display.
     */
    public void result(final String text) {
        insert(TXT_GOOD, '\n'+text);
    }

    /**
     * Displays an error.
     * 
     * @param text The error message.
     */
    public void error(final String text) {
        if (getLineText(getLineCount()-1).trim().length() > 0) {
            endln(TXT_ERROR);
        }
        insert(TXT_ERROR, '\n'+text);
    }

    /**
     * Shows the normal Jython prompt.
     */
    public void prompt() {
        if (getLineText(getLineCount()-1).trim().length() > 0) {
            endln(TXT_NORMAL);
        }
        insert(TXT_NORMAL, PS1);
    }

    /**
     * Displays non-error output that was not the result of an 
     * {@literal "associated"} {@link Command}.
     * 
     * @param text The text to display.
     * @see #generatedError(String)
     */
    public void generatedOutput(final String text) {
        if (getPromptLength(getLineText(getLineCount()-1)) > 0) {
            endln(TXT_GOOD);
        }
        insert(TXT_GOOD, text);
    }

    /**
     * Displays error output. Differs from {@link #error(String)} in that this
     * is intended for output not {@literal "associated"} with a {@link Command}.
     * 
     * <p>Example: say you fire off a background thread. If it generates an
     * error somehow, this is the method you want.
     * 
     * @param text The error message.
     */
    public void generatedError(final String text) {
        if (getPromptLength(getLineText(getLineCount()-1)) > 0) {
            insert(TXT_ERROR, '\n'+text);
        } else {
            insert(TXT_ERROR, text);
        }
    }

    /**
     * Shows the prompt that indicates more input is needed.
     */
    public void moreInput() {
        insert(TXT_NORMAL, '\n'+PS2);
    }

    public void moreInput(final int blockLevel) {
        
    }

    /**
     * Will eventually display an initial greeting to the user.
     * 
     * @throws BadLocationException Upon attempting to clear out an invalid 
     * portion of the document.
     */
    private void showBanner() throws BadLocationException {
        document.remove(0, document.getLength());
        prompt();
        textPane.requestFocus();
    }

    /** 
     * Inserts a newline character at the end of the input.
     * 
     * @param color Perhaps this should go!?
     */
    protected void endln(final Color color) {
        insert(color, "\n");
    }

    /**
     * Does the actual work of displaying color-coded messages in 
     * {@link #textPane}.
     * 
     * @param color The color of the message.
     * @param text The actual message.
     */
    protected void insert(final Color color, final String text) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        style.addAttribute(StyleConstants.Foreground, color);
        try {
            document.insertString(document.getLength(), text, style);
            textPane.setCaretPosition(document.getLength());
        } catch (BadLocationException e) {
            logger.error("bad location", e);
        }
    }

    protected void insertAtCaret(final Color color, final String text) {
        assert color != null : color;
        assert text != null : text;

        int position = textPane.getCaretPosition();
        if (!canInsertAt(position)) {
            return;
        }

        SimpleAttributeSet style = new SimpleAttributeSet();
        style.addAttribute(StyleConstants.Foreground, color);

        try {
            document.insertString(position, text, style);
        } catch (BadLocationException e) {
            logger.trace("position={}", position);
            logger.error("couldn't insert text", e);
        }
    }

    /**
     * Determines whether or not {@code position} is an acceptable place to
     * insert text. Currently the criteria for {@literal "acceptable"} means
     * that {@code position} is located within the last (or active) line, and
     * not within either {@link #PS1} or {@link #PS2}.
     * 
     * @param position Position to test. Values less than zero are not allowed.
     * 
     * @return Whether or not text can be inserted at {@code position}.
     */
    private boolean canInsertAt(final int position) {
        assert position >= 0;

        if (!onLastLine()) {
            return false;
        }

        int lineNumber = getCaretLine();
        String currentLine = getLineText(lineNumber);
        int[] offsets = getLineOffsets(lineNumber);
        logger.debug("position={} offsets[0]={} promptLen={}", new Object[] { position, offsets[0], getPromptLength(currentLine)});
        return ((position - offsets[0]) >= getPromptLength(currentLine));
    }

    /**
     * @return Number of lines in the document.
     */
    public int getLineCount() {
        return document.getRootElements()[0].getElementCount();
    }

    // TODO(jon): Rethink some of these methods names, especially getLineOffsets and getOffsetLine!!

    public int getLineOffsetStart(final int lineNumber) {
        return document.getRootElements()[0].getElement(lineNumber).getStartOffset();
    }

    public int getLineOffsetEnd(final int lineNumber) {
        return document.getRootElements()[0].getElement(lineNumber).getEndOffset();
    }

    public int[] getLineOffsets(final int lineNumber) {
        if (lineNumber >= getLineCount()) {
            return BAD_OFFSETS;
        }
        // TODO(jon): possible inline these calls?
        int start = getLineOffsetStart(lineNumber);
        int end = getLineOffsetEnd(lineNumber);
        return new int[] { start, end };
    }

    /**
     * Returns the line number that contains the specified offset.
     * 
     * @param offset Offset whose line number you want.
     * 
     * @return Line number.
     */
    public int getOffsetLine(final int offset) {
        return document.getRootElements()[0].getElementIndex(offset);
    }

    /**
     * Returns the offsets of the beginning and end of the last line.
     */
    private int[] locateLastLine() {
        return getLineOffsets(getLineCount() - 1);
    }

    /**
     * Determines whether or not the caret is on the last line.
     */
    private boolean onLastLine() {
        int[] offsets = locateLastLine();
        int position = textPane.getCaretPosition();
        return (position >= offsets[0] && position <= offsets[1]);
    }

    /**
     * @return The line number of the caret's offset within the text.
     */
    public int getCaretLine() {
        return getOffsetLine(textPane.getCaretPosition());
    }

    /**
     * Returns the line of text that occupies the specified line number.
     * 
     * @param lineNumber Line number whose text is to be returned.
     * 
     * @return Either the line of text or null if there was an error.
     */
    public String getLineText(final int lineNumber) {
        int start = getLineOffsetStart(lineNumber);
        int stop = getLineOffsetEnd(lineNumber);
        String line = null;
        try {
            line = document.getText(start, stop - start);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return line;
    }

    /**
     * Returns the line of Jython that occupies a specified line number. 
     * This is different than {@link #getLineText(int)} in that both 
     * {@link #PS1} and {@link #PS2} are removed from the returned line.
     * 
     * @param lineNumber Line number whose text is to be returned.
     * 
     * @return Either the line of Jython or null if there was an error.
     */
    public String getLineJython(final int lineNumber) {
        String text = getLineText(lineNumber);
        if (text == null) {
            return null;
        }
        int start = getPromptLength(text);
        return text.substring(start, text.length() - 1);
    }

    /**
     * Returns the length of {@link #PS1} or {@link #PS2} depending on the 
     * contents of the specified line.
     * 
     * @param line The line in question. Cannot be {@code null}.
     * 
     * @return Either the prompt length or zero if there was none.
     * 
     * @throws NullPointerException if {@code line} is {@code null}.
     */
    public static int getPromptLength(final String line) {
        notNull(line, "Null lines do not have prompt lengths");
        if (line.startsWith(PS1)) {
            return PS1.length();
        } else if (line.startsWith(PS2)) {
            return PS2.length();
        } else {
            return 0;
        }
    }

    /**
     * Returns the {@literal "block depth"} of a given line of Jython.
     * 
     * <p>Examples:<pre>
     * "print 'x'"         -> 0
     * "    print 'x'"     -> 1
     * "            die()" -> 3
     * </pre>
     * 
     * @param line Line to test. Can't be {@code null}.
     * @param whitespace The indent {@link String} used with {@code line}. Can't be {@code null}.
     * 
     * @return Either the block depth ({@code >= 0}) or {@code -1} if there was an error.
     */
    // TODO(jon): maybe need to explicitly use getLineJython?
    public static int getBlockDepth(final String line, final String whitespace) {
        int indent = whitespace.length();
        int blockDepth = 0;
        int tmpIndex = 0;
        while ((tmpIndex+indent) < line.length()) {
            int stop = tmpIndex + indent;
            if (line.substring(tmpIndex, stop).trim().length() != 0) {
                break;
            }
            tmpIndex += indent;
            blockDepth++;
        }
        return blockDepth;
    }

    /**
     * Registers a new callback handler with the console. Note that to maximize
     * utility, this method also registers the same handler with 
     * {@link #jythonRunner}.
     * 
     * @param newHandler The new callback handler.
     * 
     * @throws NullPointerException if the new handler is null.
     */
    public void setCallbackHandler(final ConsoleCallback newHandler) {
        notNull(newHandler, "Callback handler cannot be null");
        jythonRunner.setCallbackHandler(newHandler);
    }

    public Set<String> getJythonReferencesTo(final Object obj) {
        notNull(obj, "Cannot find references to a null object");
        Set<String> refs = new TreeSet<String>();
        // TODO(jon): possibly inline getJavaInstances()?
        for (Map.Entry<String, Object> entry : getJavaInstances().entrySet()) {
            if (obj == entry.getValue()) {
                refs.add(entry.getKey());
            }
        }
        return refs;
    }

    /**
     * Returns a subset of Jython's local namespace containing only variables
     * that are {@literal "pure"} Java objects.
     * 
     * @return Jython variable names mapped to their Java instantiation.
     */
    public Map<String, Object> getJavaInstances() {
        Map<String, Object> javaMap = new HashMap<String, Object>();
        Map<String, PyObject> locals = getLocalNamespace();
        for (Map.Entry<String, PyObject> entry : locals.entrySet()) {
            PyObject val = entry.getValue();
            if (val instanceof PyObjectDerived) {
                PyObjectDerived derived = (PyObjectDerived)val;
                if (derived.getType() instanceof PyJavaType) {
                    javaMap.put(entry.getKey(), val.__tojava__(Object.class));
                }
            }
        }
        return javaMap;
    }

    /**
     * Retrieves the specified Jython variable from the interpreters local 
     * namespace.
     * 
     * @param var Variable name to retrieve.
     * @return Either the variable or null. Note that null will also be 
     * returned if {@link Runner#copyLocals()} returned null.
     */
    public PyObject getJythonObject(final String var) {
        PyStringMap locals = jythonRunner.copyLocals();
        if (locals == null) {
            return null;
        }
        return locals.__finditem__(var);
    }

    /**
     * Returns a copy of Jython's local namespace.
     * 
     * @return Jython variable names mapped to {@link PyObject}s.
     */
    public Map<String, PyObject> getLocalNamespace() {
        Map<String, PyObject> localsMap = new HashMap<String, PyObject>();
        PyStringMap jythonLocals = jythonRunner.copyLocals();
        if (jythonLocals != null) {
            PyList items = jythonLocals.items();
            for (int i = 0; i < items.__len__(); i++) {
                PyTuple tuple = (PyTuple)items.__finditem__(i);
                String key = ((PyString)tuple.__finditem__(0)).toString();
                PyObject val = tuple.__finditem__(1);
                localsMap.put(key, val);
            }
        }
        return localsMap;
    }

    public void handlePaste() {
        logger.trace("not terribly sure...");
        getTextPane().paste();
        logger.trace("after forcing paste!");
    }

    /**
     * Handles the user hitting the {@code Home} key. If the caret is on a line
     * that begins with either {@link #PS1} or {@link #PS2}, the caret will be
     * moved to just after the prompt. This is done mostly to emulate CPython's
     * IDLE.
     */
    public void handleHome() {
        int caretPosition = getCaretLine();
        int[] offsets = getLineOffsets(caretPosition);
        int linePosition = getPromptLength(getLineText(caretPosition));
        textPane.setCaretPosition(offsets[0] + linePosition);
    }

    /**
     * Moves the caret to the end of the line it is currently on, rather than
     * the end of the document.
     */
    public void handleEnd() {
        int[] offsets = getLineOffsets(getCaretLine());
        textPane.setCaretPosition(offsets[1] - 1);
    }

    public void handleUp() {
        logger.trace("handleUp");
    }

    public void handleDown() {
        logger.trace("handleDown");
    }

    /**
     * Inserts the contents of {@link #WHITESPACE} wherever the cursor is 
     * located.
     */
    // TODO(jon): completion!
    public void handleTab() {
        logger.trace("handling tab!");
        insertAtCaret(TXT_NORMAL, WHITESPACE);
    }

    // TODO(jon): what about selected regions?
    // TODO(jon): what about multi lines?
    public void handleDelete() {
        if (!onLastLine()) {
            return;
        }

        String line = getLineText(getCaretLine());
        if (line == null) {
            return;
        }

        int position = textPane.getCaretPosition();
        int start = getPromptLength(line);

        // don't let the user delete parts of PS1 or PS2
        int lineStart = getLineOffsetStart(getCaretLine());
        if (((position-1)-lineStart) < start) {
            return;
        }

        try {
            document.remove(position - 1, 1);
        } catch (BadLocationException e) {
            logger.error("failed to backspace at position={}", (position-1));
        }
    }

    /**
     * Handles the user pressing enter by basically grabbing the line of jython
     * under the caret. If the caret is on the last line, the line is queued
     * for execution. Otherwise the line is reinserted at the end of the 
     * document--this lets the user preview a previous command before they 
     * rerun it.
     */
    // TODO(jon): if you hit enter at the start of a block, maybe it should
    // replicate the enter block at the end of the document?
    public void handleEnter() {
        String line = getLineJython(getCaretLine());
        if (line == null) {
            line = "";
        }

        if (onLastLine()) {
            queueLine(line);
        } else {
            insert(TXT_NORMAL, line);
        }
    }

    /**
     * Returns the Jython statements as entered by the user, ordered from first
     * to last.
     * 
     * @return User's history.
     */
    public List<String> getHistory() {
        return new ArrayList<String>(jythonHistory);
    }

    /**
     * Sends a line of Jython to the interpreter via {@link #jythonRunner} and
     * saves it to the history.
     * 
     * @param line Jython to queue for execution.
     */
    public void queueLine(final String line) {
        jythonRunner.queueLine(line);
        jythonHistory.add(line);
    }

    /**
     * Sends a batch of Jython commands to the interpreter. <i>This is 
     * different than simply calling {@link #queueLine(String)} for each 
     * command;</i> the interpreter will attempt to execute each batched 
     * command before returning {@literal "control"} to the console.
     * 
     * <p>This method is mostly useful for restoring Console sessions. Each
     * command in {@code commands} will appear in the console as though the
     * user typed it. The batch of commands will also be saved to the history.
     * 
     * @param name Identifier for the batch. Doesn't need to be unique, merely
     * non-null.
     * @param commands The commands to execute.
     */
    public void queueBatch(final String name, final List<String> commands) {
//        jythonRunner.queueBatch(this, name, commands);
        jythonRunner.queueBatch(name, commands);
        jythonHistory.addAll(commands);
    }

    public void addPretendHistory(final String line) {
        jythonHistory.add(line);
    }

    /**
     * Puts together the GUI once EventQueue has processed all other pending 
     * events.
     */
    public void run() {
        JFrame frame = new JFrame(windowTitle);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(getPanel());
        frame.getContentPane().setPreferredSize(new Dimension(600, 200));
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Noop.
     */
    public void keyPressed(final KeyEvent e) { }

    /**
     * Noop.
     */
    public void keyReleased(final KeyEvent e) { }

    // this is weird: hasAction is always false
    // seems to work so long as the ConsoleActions fire first...
    // might want to look at default actions again
    public void keyTyped(final KeyEvent e) {
        logger.trace("hasAction={} key={}", hasAction(textPane, e), e.getKeyChar());
        int caretPosition = textPane.getCaretPosition();
        if (!hasAction(textPane, e) && !canInsertAt(caretPosition)) {
            logger.trace("hasAction={} lastLine={}", hasAction(textPane, e), onLastLine());
            e.consume();
        }
    }

    private static boolean hasAction(final JTextPane jtp, final KeyEvent e) {
        assert jtp != null;
        assert e != null;
        KeyStroke stroke = 
            KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers());
        return (jtp.getKeymap().getAction(stroke) != null);
    }

    /**
     * Maps a {@literal "jython action"} to a keystroke.
     */
    public enum Actions {
        TAB("jython.tab", KeyEvent.VK_TAB, 0),
        DELETE("jython.delete", KeyEvent.VK_BACK_SPACE, 0),
        END("jython.end", KeyEvent.VK_END, 0),
        ENTER("jython.enter", KeyEvent.VK_ENTER, 0),
        HOME("jython.home", KeyEvent.VK_HOME, 0),
        PASTE("jython.paste", KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
        //        PASTE("jython.paste", KeyEvent.VK_V, KeyEvent.CTRL_MASK);
//        UP("jython.up", KeyEvent.VK_UP, 0),
//        DOWN("jython.down", KeyEvent.VK_DOWN, 0);
        ;

        private final String id;
        private final int keyCode;
        private final int modifier;

        Actions(final String id, final int keyCode, final int modifier) {
            this.id = id;
            this.keyCode = keyCode;
            this.modifier = modifier;
        }

        public String getId() {
            return id;
        }

        public KeyStroke getKeyStroke() {
            return KeyStroke.getKeyStroke(keyCode, modifier);
        }
    }

    public static class HistoryEntry {
        private HistoryType type;
        private String entry;

        public HistoryEntry() {}

        public HistoryEntry(final HistoryType type, final String entry) {
            this.type = notNull(type, "type cannot be null");
            this.entry = notNull(entry, "entry cannot be null");
        }

        public void setEntry(final String entry) {
            this.entry = notNull(entry, "entry cannot be null");
        }

        public void setType(final HistoryType type) {
            this.type = notNull(type, "type cannot be null");
        }

        public String getEntry() {
            return entry;
        }

        public HistoryType getType() {
            return type;
        }

        @Override public String toString() {
            return String.format("[HistoryEntry@%x: type=%s, entry=\"%s\"]", 
                hashCode(), type, entry);
        }
    }

    private class PopupListener extends MouseAdapter {
        public void mouseClicked(final MouseEvent e) {
            checkPopup(e);
        }

        public void mousePressed(final MouseEvent e) {
            checkPopup(e);
        }

        public void mouseReleased(final MouseEvent e) {
            checkPopup(e);
        }

        private void checkPopup(final MouseEvent e) {
            if (!e.isPopupTrigger()) {
                return;
            }
            JPopupMenu popup = menuWrangler.buildMenu();
            popup.show(textPane, e.getX(), e.getY());
        }
    }

    public static String getUserPath(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("-userpath".equals(args[i]) && (i+1) < args.length) {
                return args[i+1];
            }
        }
        return System.getProperty("user.home");
    }

    public static void main(String[] args) {
        String os = System.getProperty("os.name");
        String sep = "/";
        if (os.startsWith("Windows")) {
            sep = "\\";
        }
        String pythonHome = getUserPath(args);

        Properties systemProperties = System.getProperties();
        Properties jythonProperties = new Properties();
        jythonProperties.setProperty("python.home", pythonHome+sep+"jython");
        Interpreter.initialize(systemProperties, jythonProperties, new String[]{""});
        EventQueue.invokeLater(new Console());
        EventQueue.invokeLater(new Console());
    }
}
