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

package ucar.unidata.ui;

import static ucar.unidata.util.GuiUtils.getImageIcon;
import static ucar.unidata.util.GuiUtils.makeButton;
import static ucar.unidata.util.GuiUtils.makeImageButton;
import static ucar.unidata.util.LogUtil.registerWindow;
import static ucar.unidata.xml.XmlUtil.decodeBase64;
import static ucar.unidata.xml.XmlUtil.encodeBase64;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;


/**
 * This class provides  an abstract interactive shell
 *
 * @author IDV development team
 * @version $Revision$Date: 2012/04/25 17:03:28 $
 */
public class InteractiveShell implements HyperlinkListener {

    private static final Logger logger = LoggerFactory.getLogger(InteractiveShell.class);

    private static Object MUTEX =  new Object();

    /** _more_ */
    protected JFrame frame;

    /** _more_ */
    protected JTextField commandFld;

    /** _more_ */
    protected JTextArea commandArea;

    /** _more_ */
    private JButton flipBtn;

    /** _more_ */
    private GuiUtils.CardLayoutPanel cardLayoutPanel;

    /** _more_ */
    protected JEditorPane editorPane;

    /** _more_ */
    protected StringBuffer sb = new StringBuffer();

    private boolean bufferOutput = false;

    /** _more_ */
    protected final List<String> history = new ArrayList<String>();

    /** _more_ */
    protected int historyIdx = -1;

    /** _more_          */
    private String title;

    /** _more_          */
    protected JComponent contents;

    /**
     * _more_
     *
     * @param title _more_
     */
    public InteractiveShell(String title) {
        this.title = title;
    }

    /**
     * _more_
     */
    protected void makeFrame() {
        frame = new JFrame(title);
        frame.getContentPane().add(contents);
        frame.pack();
        frame.setLocation(100, 100);
        frame.setVisible(true);
        registerWindow(frame);
    }

    public void close() {
        frame.dispose();
        contents = null;
        title = null;
        history.clear();
        sb = null;
        editorPane = null;
        cardLayoutPanel = null;
        flipBtn = null;
        commandArea = null;
        commandFld = null;
        frame = null;
    }

    public void show() {
        frame.setVisible(true);
    }

    protected String getHref(String text, String label) {
        return new StringBuilder("<a href=\"")
            .append(encodeBase64(("text:" + text).getBytes()))
            .append("\">")
            .append(label)
            .append("</a>").toString();
    }

    protected void showWaitCursor() {
        frame.setCursor(GuiUtils.waitCursor);
    }

    protected void showNormalCursor() {
        frame.setCursor(GuiUtils.normalCursor);
    }

    /**
     * _more_
     */
    protected void init() {
        contents = doMakeContents();
        makeFrame();
    }

    /**
     * _more_
     *
     * @param e _more_
     */
    @Override public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }
        String url;
        if (e.getURL() == null) {
            url = e.getDescription();
        } else {
            url = e.getURL().toString();
        }
        url = new String(decodeBase64(url));
        if (url.startsWith("eval:")) {
            Misc.run(this, "eval", url.substring(5));
        } else if(url.startsWith("text:")) {
            setText(url.substring(5));
        }
    }

    public void setText(String text) {
        JTextComponent field = getCommandFld();
        field.setText(text);
        field.requestFocus();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent doMakeContents() {
        
        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.addHyperlinkListener(this);
        // http://www.coderanch.com/t/537810/GUI/java/auto-scroll-bottom-jtextarea
        // scroll to bottom on text updates:   
        DefaultCaret caret = (DefaultCaret) editorPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        JScrollPane scroller = GuiUtils.makeScrollPane(editorPane, 400, 300);
        scroller.setPreferredSize(new Dimension(400, 300));
        commandFld = new JTextField();
        GuiUtils.setFixedWidthFont(commandFld);
        GuiUtils.addKeyBindings(commandFld);
        commandFld.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                handleKeyPress(e, commandFld);
            }
        });
        commandFld.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightMouseClick(commandFld, e);
                }
            }
        });

        commandArea = new JTextArea("", 4, 30);
        GuiUtils.setFixedWidthFont(commandArea);
        GuiUtils.addKeyBindings(commandArea);
        commandArea.setTabSize(4);
        commandArea.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                handleKeyPress(e, commandArea);
            }
        });
        commandArea.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightMouseClick(commandArea, e);
                }
            }
        });
        cardLayoutPanel = new GuiUtils.CardLayoutPanel();
        cardLayoutPanel.addCard(GuiUtils.top(commandFld));
        cardLayoutPanel.addCard(GuiUtils.makeScrollPane(commandArea, 200, 100));
        flipBtn = makeImageButton("/auxdata/ui/icons/DownDown.gif", this, "flipField");
        JButton evalBtn = makeButton("Evaluate:", this, "handleEvaluationAction");
        JComponent bottom = GuiUtils.leftCenterRight(
            GuiUtils.top(evalBtn),
            GuiUtils.inset(cardLayoutPanel, 2),
            GuiUtils.top(flipBtn));

        JComponent contents = GuiUtils.vsplit(scroller, bottom,300);
        contents = GuiUtils.inset(contents, 5);

        JMenuBar menuBar = doMakeMenuBar();
        if (menuBar != null) {
            contents = GuiUtils.topCenter(menuBar, contents);
        }
        return contents;
    }

    /**
     * _more_
     *
     * @param commandFld _more_
     * @param e _more_
     */
    protected void handleRightMouseClick(JTextComponent commandFld, MouseEvent e) {}

    /**
     * _more_
     *
     * @return _more_
     */
    protected JMenuBar doMakeMenuBar() {
        return null;
    }

    /**
     * _more_
     */
    public void toFront() {
        GuiUtils.toFront(frame);
    }

    /**
     * _more_
     */
    public void flipField() {
        cardLayoutPanel.flip();
        if (getCommandFld() instanceof JTextArea) {
            flipBtn.setIcon(getImageIcon("/auxdata/ui/icons/UpUp.gif"));
        } else {
            flipBtn.setIcon(getImageIcon("/auxdata/ui/icons/DownDown.gif"));
        }
        // save the user a redundant mouse click:
        getCommandFld().requestFocusInWindow();
    }

    /**
     * _more_
     *
     * @param t _more_
     */
    public void insertText(String t) {
        GuiUtils.insertText(getCommandFld(), t);
    }

    public void handleEvaluationAction() {
        eval();
    }

    public void handleHistoryPreviousAction() {
        int historySize = history.size();
        if ((historyIdx < 0) || (historyIdx >= historySize)) {
            historyIdx = historySize - 1;
        } else {
            historyIdx--;
            if (historyIdx < 0) {
                historyIdx = 0;
            }
        }
        if ((historyIdx >= 0) && (historyIdx < historySize)) {
            getCommandFld().setText(history.get(historyIdx));
        }
    }

    public void handleHistoryNextAction() {
        int historySize = history.size();
        if ((historyIdx < 0) || (historyIdx >= historySize)) {
            historyIdx = historySize - 1;
        } else {
            historyIdx++;
            if (historyIdx >= historySize) {
                historyIdx = historySize - 1;
            }
        }
        if ((historyIdx >= 0) && (historyIdx < historySize)) {
            getCommandFld().setText(history.get(historyIdx));
        }
    }

    public void handleFlipCommandAreaAction() {
        flipField();
    }

    /**
     * _more_
     *
     * @param e _more_
     * @param cmdFld _more_
     */
    protected void handleKeyPress(KeyEvent e, JTextComponent cmdFld) {
        boolean isArea  = (cmdFld instanceof JTextArea);
        int keyCode = e.getKeyCode();
        boolean isControlDown = e.isControlDown();
        boolean isShiftDown = e.isShiftDown();
        boolean isAltDown = e.isAltDown();
        boolean isMetaDown = e.isMetaDown();
        boolean hasHistory = !history.isEmpty();
//        logger.trace("isArea={} keyCode={} isMetaDown={} isControlDown={} isShiftDown={} isAltDown={}", new Object[] { isArea, keyCode, isMetaDown, isControlDown, isShiftDown, isAltDown });
        if (((!isArea && keyCode == KeyEvent.VK_UP) || ((keyCode == KeyEvent.VK_P) && isControlDown)) && (hasHistory)) {
            handleHistoryPreviousAction();
        } else if (((!isArea && keyCode == KeyEvent.VK_DOWN) || ((keyCode == KeyEvent.VK_N) && isControlDown)) && (hasHistory)) {
            handleHistoryNextAction();
        } else if ((!isArea && keyCode == KeyEvent.VK_ENTER) || ((keyCode == KeyEvent.VK_ENTER && isShiftDown))) {
            handleEvaluationAction();
        } else if (isControlDown && (keyCode == KeyEvent.VK_SLASH)) {
            handleFlipCommandAreaAction();
        }
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    public void insert(String s) {
        JTextComponent commandField = getCommandFld();
        String t = commandField.getText();
        int pos = commandField.getCaretPosition();
        t = t.substring(0, pos) + s + t.substring(pos);
        commandField.setText(t);
        commandField.setCaretPosition(pos + s.length());
    }

    /**
     * _more_
     */
    public void clear() {
        historyIdx = -1;
        history.clear();
        clearOutput();
    }

    public void clearOutput() {
        sb = new StringBuffer();
        editorPane.setText("");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected JTextComponent getCommandFld() {
        if (cardLayoutPanel.getVisibleIndex() == 0) {
            return commandFld;
        }
        return commandArea;
    }

    /**
     * _more_
     */
    public void eval() {
        JTextComponent cmdFld = getCommandFld();
        String cmd = cmdFld.getText();
        if ("!!".equals(cmd.trim())) {
            if (history.isEmpty()) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            cmd = history.get(history.size()-1);
        } else if (cmd.trim().startsWith("!")) {
            if (history.isEmpty()) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            String prefix = cmd.substring(1);
            cmd = null;
            for (int i = history.size() - 1; i >= 0; i--) {
                String tmp = history.get(i);
                if (tmp.startsWith(prefix)) {
                    cmd = tmp;
                    break;
                }
            }
            if (cmd == null) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
        }
        cmdFld.setText("");
        history.add(cmd);
        historyIdx = -1;
        Misc.run(this, "eval", cmd);
    }

    protected void startBufferingOutput() {
        bufferOutput = true;
    }

    protected void endBufferingOutput() {
        bufferOutput = false;
        updateText();
    }

    private void updateText() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                editorPane.setText(sb.toString());
            }
        });
    }

    /**
     * _more_
     *
     * @param m _more_
     */
    public void output(String m) {
        sb.append("<span style=\"font-family: monospace; font-size: 10px;\">").append(m).append("</span>");
        if (!bufferOutput) {
            updateText();
        }
    }

    /**
     * _more_
     *
     * @param code the code that was evaluated
     */
    public void eval(String code) {
        String evalCode = new StringBuilder("eval:").append(code).toString();
        String encoded1 = encodeBase64(evalCode.getBytes());
        String textCode = new StringBuilder("text:").append(code).toString();
        String encoded2 = encodeBase64(textCode.getBytes());
        output(new StringBuilder("<div style=\"margin:0; margin-bottom:1; background-color:#cccccc; \"><table width=\"100%\"><tr><td>")
//            .append("<pre>").append(formatCode(code)).append("</pre>")
            .append(formatCode(code))
            .append("</td><td align=\"right\" valign=\"top\"><a href=\"")
            .append(encoded2)
            .append("\"><img src=\"idvresource:/auxdata/ui/icons/Down16.gif\" border=0></a><a href=\"")
            .append(encoded1)
            .append("\"><img alt=\"Reload\" src=\"idvresource:/auxdata/ui/icons/Refresh16.gif\" border=0></a></td></tr></table></div>").toString());
    }

    /**
     * _more_
     *
     * @param code _more_
     *
     * @return _more_
     */
    protected String formatCode(String code) {
        return code;
    }

    /**
     * 
     * @return a reference to this shell's JSplitPane that holds the text input and text output areas
     */
    private JSplitPane getJSplitPane() {
        // this implementation is highly dependent on the current structure of the
        // InteractiveShell layout...but I don't see a better way to do it.
        JPanel panel1 = (JPanel) frame.getContentPane().getComponent(0);
        // get the JPanel holding the JSplitPane, not the menu bar
        JPanel panel2 = (JPanel) panel1.getComponent(1);
        return (JSplitPane) panel2.getComponent(0);
    }

    /**
     * Get the location of the horizontal JSplitPane divider that separates the text input area
     * from the text output.
     * 
     * @return the location of the horizontal divider bar as provided by the JSplitPane
     */
    protected int getDividerLocation() {
        return this.getJSplitPane().getDividerLocation();
    }

    /**
     * Set the location of the horizontal JSplitPane divider that separates the text input area
     * from the text output.
     * 
     * @param loc the location of the horizontal JSplitPane divider. 
     * (larger number means further from top of window.)
     */
    protected void setDividerLocation(int loc) {
        this.getJSplitPane().setDividerLocation(loc);
    }
}

