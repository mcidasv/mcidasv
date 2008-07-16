package edu.wisc.ssec.mcidasv.jython;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Properties;
import java.util.Random;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.TextAction;

import org.python.util.InteractiveConsole;


public class Console implements Runnable {

    /** Offset array used when actual offsets cannot be determined. */
    private static final int[] BAD_OFFSETS = { -1, -1 };

    /** Color of the jython text as it is being entered. */
    private static final Color TXT_NORMAL = Color.BLACK;

    /** Color of text coming from &quot;stdout&quot;. */
    private static final Color TXT_GOOD = Color.BLUE;

    /** Not used just yet... */
    private static final Color TXT_WARN = Color.ORANGE;

    /** Color of text coming from &quot;stderr&quot;. */
    private static final Color TXT_ERROR = Color.RED;

    /** Normal jython prompt. */
    private static final String PS1 = ">>> ";

    /** Prompt that indicates more input is needed. */
    private static final String PS2 = "... ";

    /** Not used yet. */
    private static final String BANNER = InteractiveConsole.getDefaultBanner();

    /** All text will appear in this font. */
    private static final Font FONT = new Font("Monospaced", Font.PLAIN, 14);

//  private static enum Actions { ENTER, DELETE, HOME, UP, DOWN };

    private Runner jythonRunner;

    private JTextPane textPane;

    private Document document;

    private Position initialLocation;

    private JPanel panel;

    public Console() {
        jythonRunner = new Runner();
        jythonRunner.start();
        panel = new JPanel(new BorderLayout());
        textPane = new JTextPane();
        document = textPane.getDocument();
        panel.add(BorderLayout.CENTER, new JScrollPane(textPane));
        try {
            showBanner();
            initialLocation = 
                document.createPosition(document.getLength() - 1);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        // TODO(jon): less stupid!
        Keymap keyMap = JTextComponent.addKeymap("jython", textPane.getKeymap());
        
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        ConsoleAction action = new EnterAction(this);
        keyMap.addActionForKeyStroke(stroke, action);
        
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
        action = new DeleteAction(this);
        keyMap.addActionForKeyStroke(stroke, action);
        
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0);
        action = new HomeAction(this);
        keyMap.addActionForKeyStroke(stroke, action);
        
//        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
//        action = new UpAction(this);
//        keyMap.addActionForKeyStroke(stroke, action);
        
//        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
//        action = new DownAction(this);
//        keyMap.addActionForKeyStroke(stroke, action);
        
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_END, 0);
        action = new EndAction(this);
        keyMap.addActionForKeyStroke(stroke, action);
        
        textPane.setKeymap(keyMap);
        textPane.setFont(FONT);
    }

    public JPanel getPanel() {
        return panel;
    }

    /**
     * Displays non-error output.
     * 
     * @param text The message to display.
     */
    public void result(final String text) {
        insert(TXT_GOOD, "\n" + text);
    }

    /**
     * Displays an error.
     * 
     * @param text The error message.
     */
    public void error(final String text) {
        insert(TXT_ERROR, "\n" + text);
    }

    /**
     * Shows the normal Jython prompt.
     */
    public void prompt() {
        insert(TXT_NORMAL, "\n" + PS1);
    }

    /**
     * Shows the prompt that indicates more input is needed.
     */
    public void moreInput() {
        insert(TXT_NORMAL, "\n" + PS2);
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
     * Does the actual work of displaying color-coded messages in 
     * {@link #textPane}.
     * 
     * @param color The color of the message.
     * @param text The actual message.
     */
    private void insert(final Color color, final String text) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        style.addAttribute(StyleConstants.Foreground, color);
        try {
            document.insertString(document.getLength(), text, style);
            textPane.setCaretPosition(document.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return Number of lines in the document.
     */
    public int getLineCount() {
        return document.getRootElements()[0].getElementCount();
    }

    
    public int getLineOffsetStart(final int lineNumber) {
        return document.getRootElements()[0].getElement(lineNumber).getStartOffset();
    }
    
    public int getLineOffsetEnd(final int lineNumber) {
        return document.getRootElements()[0].getElement(lineNumber).getEndOffset();
    }

    public int[] getLineOffsets(final int lineNumber) {
        if (lineNumber >= getLineCount())
            return BAD_OFFSETS;

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
     * Returns the line of jython that occupies a specified line number. 
     * This is different than {@link #getLineText(int)} in that both 
     * {@link #PS1} and {@link #PS2} are removed from the returned line.
     * 
     * @param lineNumber Line number whose text is to be returned.
     * 
     * @return Either the line of jython or null if there was an error.
     */
    public String getLineJython(final int lineNumber) {
        String text = getLineText(lineNumber);
        if (text == null)
            return null;

        int start = -1;
        if (text.startsWith(PS1)) {
            start = PS1.length();
        } else if (text.startsWith(PS2)) {
            start = PS2.length();
        } else {
            return null;
        }

        return text.substring(start, text.length() - 1);
    }

    // TODO: basically makes it so that when a user hits "home" the caret is
    // moved to just after PS1 or PS2, rather than the beginning of the line.
    public void handleHome() {
        String line = getLineText(getCaretLine());
        int[] offsets = getLineOffsets(getCaretLine());

        int linePosition = 0;
        if (line.startsWith(PS1))
            linePosition = PS1.length();
        else if (line.startsWith(PS2))
            linePosition = PS2.length();

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

    // TODO(jon): what about selected regions?
    // TODO(jon): what about multi lines?
    public void handleDelete() {
        if (!onLastLine()) {
            System.err.println("handleDelete: not on last line");
            return;
        }

        int position = textPane.getCaretPosition();
        String line = getLineText(getCaretLine());
        if (line == null) {
            System.err.println("handleDelete: weirdness!!");
            return;
        }

        try {
            document.remove(position - 1, 1);
        } catch (BadLocationException e) {
            e.printStackTrace();
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
        if (line == null)
            return;

        if (onLastLine())
            jythonRunner.queueLine(this, line);
        else
            insert(TXT_NORMAL, line);
    }

    public void run() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(getPanel());
        frame.getContentPane().setPreferredSize(new Dimension(600, 200));
        frame.pack();
        frame.setVisible(true);
    }

    // TODO(jon): is this going to work on windows?
    private static final String PYTHON_HOME = "~/.mcidasv/jython";

    public static void main(String[] args) {
        Properties systemProperties = System.getProperties();
        Properties jythonProperties = new Properties();
        jythonProperties.setProperty("python.home", PYTHON_HOME);
        Interpreter.initialize(systemProperties, jythonProperties, new String[]{""});
        EventQueue.invokeLater(new Console());
        EventQueue.invokeLater(new Console());
    }

    // TODO(jon): implement this to get rid of all that BS in the Console constructor
//    public static class KeyBinding {
//        public KeyBinding(int event, int modifier, Actions type) {
//            
//        }
//    }
}
