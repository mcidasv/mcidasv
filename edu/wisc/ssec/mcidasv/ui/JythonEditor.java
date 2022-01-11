/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2022
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

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * A bare-bones text editor that can do relatively robust syntax highlighting
 * of Jython code.
 *
 * <p>This class relies <i>very heavily</i> upon the wonderful
 * <a href="https://github.com/bobbylight/RSyntaxTextArea">RSyntaxTextArea</a>
 * project.</p>
 */
public class JythonEditor implements UndoableEditListener {

    /** Text area that contains the syntax-highlighted text. */
    private final McvJythonTextArea textArea;

    /** Scroll pane for {@link #textArea}. */
    private final RTextScrollPane scrollPane;

    /** Undo manager. */
    private final UndoManager undo;

    /**
     * Creates a new JythonEditor.
     */
    public JythonEditor() {
        textArea = new McvJythonTextArea(20, 60);

        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);

        textArea.setCodeFoldingEnabled(true);
        textArea.setWhitespaceVisible(true);

        // hrm?
        textArea.setBracketMatchingEnabled(true);
        textArea.setPaintMatchedBracketPair(true);

        textArea.setAnimateBracketMatching(true);
        textArea.setPaintTabLines(true);
        textArea.setTabsEmulated(true);
        textArea.setTabSize(4);

        scrollPane = new RTextScrollPane(textArea);

        undo = new UndoManager();

        addUndoableEditListener(this);
    }

    /**
     * Returns the text area responsible for syntax highlighting.
     *
     * @return Reference to {@link #textArea}.
     */
    public JTextComponent getTextComponent() {
        return textArea;
    }

    /**
     * Returns the {@code JScrollPane} that contains {@link #textArea}.
     *
     * @return {@code JScrollPane} with the text area. Suitable for adding to
     * a {@code JPanel}.
     */
    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    /**
     * Sets the text of this document to the given {@code String}.
     *
     * @param text New text to use in {@link #textArea}.
     */
    public void setText(String text) {
        textArea.setText(text);
    }

    /**
     * Returns a string containing the text of {@link #textArea}.
     *
     * @return The current contents of the text area.
     */
    public String getText() {
        return textArea.getText();
    }

    /**
     * Controls whether or not changes can be made to the contents of
     * {@link #textArea}.
     *
     * @param enabled {@code true} if the editor should be enabled,
     *                {@code false} otherwise.
     */
    public void setEnabled(boolean enabled) {
        textArea.setEnabled(enabled);
        scrollPane.getGutter().setEnabled(enabled);
        scrollPane.getGutter().setBackground(textArea.getBackground());
    }

    /**
     * Returns the component (aka {@literal "the gutter"} that contains
     * optional information like line numbers.
     *
     * @return {@code JPanel} that contains the line numbers.
     */
    public JPanel getLineNumberComponent() {
        return scrollPane.getGutter();
    }

    /**
     * Copies selected text into system clipboard.
     */
    public void copy() {
        textArea.copy();
    }

    /**
     * Whether or not changes can be made to {@link #textArea}.
     *
     * @return {@code true} if changes are allowed, {@code false} otherwise.
     */
    public boolean isEnabled() {
        return textArea.isEnabled();
    }

    /**
     * Insert the given text at the caret.
     *
     * @param textToInsert Text to insert.
     */
    public void insertText(String textToInsert) {
        int pos = textArea.getCaretPosition();
        String t = textArea.getText();
        t = t.substring(0, pos) + textToInsert + t.substring(pos);
        textArea.setText(t);
        textArea.setCaretPosition(pos + textToInsert.length());
    }

    /**
     * Handles undoable edits
     *
     * @param e Event that represents the undoable edit.
     */
    @Override public void undoableEditHappened(UndoableEditEvent e) {
        if (e.getEdit().isSignificant()) {
            undo.addEdit(e.getEdit());
        }
    }

    /**
     * Adds the given undoable edit listener to {@link #textArea}.
     *
     * @param l Listener to add.
     */
    public void addUndoableEditListener(UndoableEditListener l) {
        textArea.getDocument().addUndoableEditListener(l);
    }

    /**
     * Remove the given undoable edit listener from {@link #textArea}.
     *
     * @param l Listener to remove.
     */
    public void removeUndoableEditListener(UndoableEditListener l) {
        textArea.getDocument().removeUndoableEditListener(l);
    }

    /**
     * Returns the default {@link JPopupMenu} created by
     * {@link RSyntaxTextArea#createPopupMenu()}.
     *
     * @return Popup menu.
     */
    public JPopupMenu createPopupMenu() {
        return textArea.makePopupMenu();
    }

    public static class McvJythonTextArea extends RSyntaxTextArea {

        McvJythonTextArea(int rows, int columns) {
            super(rows, columns);
        }

        @Override protected JPopupMenu createPopupMenu() {
            // this is needed so that the popup is disabled by default, which
            // allows JythonManager's addMouseListener stuff to work a bit
            // better.
            return null;
        }

        public JPopupMenu makePopupMenu() {
            // this method is mostly for getting around the fact that
            // createPopupMenu is protected
            return super.createPopupMenu();
        }
    }
}
