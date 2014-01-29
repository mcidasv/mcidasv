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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.border.BevelBorder;

import org.python.core.PyObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO(jon): this will need to be reconsidered, but it's fine for the current
// console.
public class DefaultMenuWrangler implements MenuWrangler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMenuWrangler.class);

    /** The {@link Console} whose menus we're {@literal "wrangling"}. */
    private final Console console;

    /** Handles {@literal "cut"} requests that originate from the menu. */
    private final CutTextAction cutAction;

    /** Handles {@literal "copy"} requests that originate from the menu. */
    private final CopyTextAction copyAction;

    /** Handles {@literal "paste"} requests that originate from the menu. */
    private final PasteTextAction pasteAction;

    /** Allows the user to clear out the buffer via the menu. */
    private final ClearBufferAction clearAction;

    /** Allows the user to select the buffer's contents. */
    private final SelectBufferAction selectAction;

    public DefaultMenuWrangler(final Console console) {
        this.console = notNull(console, "Cannot provide a null console");

        cutAction = new CutTextAction(console);
        copyAction = new CopyTextAction(console);
        pasteAction = new PasteTextAction(console);

        clearAction = new ClearBufferAction(console);
        selectAction = new SelectBufferAction(console);
    }

    public JPopupMenu buildMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(makeLocalsMenu());
        menu.addSeparator();
        menu.add(cutAction.getMenuItem());
        menu.add(copyAction.getMenuItem());
        menu.add(pasteAction.getMenuItem());
        menu.addSeparator();
        menu.add(clearAction.getMenuItem());
        menu.add(selectAction.getMenuItem());
        menu.setBorder(new BevelBorder(BevelBorder.RAISED));
        return menu;
    }

    /**
     * Don't need to handle this just yet.
     */
    public void stateChanged() {
        logger.trace("noop!");
    }

    /**
     * Returns the contents of Jython's local namespace as a {@link JMenu} that
     * allows for (limited) introspection.
     * 
     * @return {@code JMenu} containing the local namespace.
     */
    private JMenu makeLocalsMenu() {
        JMenu menu = new JMenu("Local Namespace");

        ActionListener menuClickHandler = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                String varName = e.getActionCommand();
                // TODO(jon): IDLE doesn't appear to allow inserts on anything
                // except for the last line. is this what we want?
                console.insertAtCaret(Console.TXT_NORMAL, varName);
            }
        };

        // TODO(jon): it would be really cool to allow customizable submenu
        // stuff. [ working on it! ]
        Map<String, PyObject> locals = console.getLocalNamespace();
        for (Map.Entry<String, PyObject> entry : locals.entrySet()) {

            String key = entry.getKey();
            PyObject value = entry.getValue();

            Class<?> c = value.getClass();
//            if (value instanceof PyJavaInstance)
//                c = value.__tojava__(Object.class).getClass();

            String itemName = key + ": " + c.getSimpleName();

            JMenuItem item = new JMenuItem(itemName);
            item.setActionCommand(key);
            item.addActionListener(menuClickHandler);
            menu.add(item);
        }
        return menu;
    }

    /**
     * Generalized representation of a {@literal "context popup menu"}. Handles
     * the more trivial things that the menu items need to handle.
     */
    private static abstract class MenuAction {

        protected final Console console;

        protected final String label;

        protected final JMenuItem item;

        protected MenuAction(final Console console, final String label) {
            this.console = console;
            this.label = label;
            item = buildMenuItem();
        }

        public ActionListener getActionListener() {
            return new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    doAction();
                }
            };
        }

        public JMenuItem getMenuItem() {
            item.setEnabled(validConsoleState());
            return item;
        }

        public JMenuItem buildMenuItem() {
            JMenuItem item = new JMenuItem(label);
            item.setEnabled(validConsoleState());
            item.addActionListener(getActionListener());
            return item;
        }

        abstract public boolean validConsoleState();

        abstract public void doAction();
    }

    /**
     * Allows the user to trigger a {@literal "cut"} operation. There must be 
     * some text that is currently selected in order for this to be enabled.
     * 
     * @see javax.swing.text.JTextComponent#cut()
     */
    private static class CutTextAction extends MenuAction {

        public CutTextAction(final Console console) {
            super(console, "Cut");
        }

        @Override public boolean validConsoleState() {
            if (console == null || console.getTextPane() == null) {
                return false;
            }

            String selection = console.getTextPane().getSelectedText();
            if (selection != null && selection.length() > 0) {
                return true;
            }
            return false;
        }

        @Override public void doAction() {
            console.getTextPane().cut();
        }
    }

    /**
     * Basic {@literal "copy"} operation. Requires that there is some selected
     * text in the console's {@code JTextPane}.
     * 
     * @see javax.swing.text.JTextComponent#copy()
     */
    private static class CopyTextAction extends MenuAction {

        public CopyTextAction(final Console console) {
            super(console, "Copy");
        }

        @Override public boolean validConsoleState() {
            if (console == null || console.getTextPane() == null) {
                return false;
            }

            String selection = console.getTextPane().getSelectedText();
            if (selection != null && selection.length() > 0) {
                return true;
            }

            return false;
        }

        @Override public void doAction() {
            console.getTextPane().copy();
        }
    }

    /**
     * Allows the user to (attempt) to paste the contents of the <i>system</i>
     * clipboard. Clipboard must contain some kind of {@literal "text"} for
     * this to work.
     * 
     * @see javax.swing.text.JTextComponent#paste()
     */
    private static class PasteTextAction extends MenuAction {

        public PasteTextAction(final Console console) {
            super(console, "Paste");
        }

        @Override public boolean validConsoleState() {
            if (console == null || console.getTextPane() == null) {
                return false;
            }

            Clipboard clippy =
                Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clippy.getContents(null);
            if (contents != null) {
                if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    return true;
                }
            }
            return false;
        }

        @Override public void doAction() {
            console.getTextPane().paste();
        }
    }

    /**
     * Clears out the console's {@code JTextPane}, though a fresh jython 
     * prompt is shown afterwards.
     */
    private static class ClearBufferAction extends MenuAction {

        public ClearBufferAction(final Console console) {
            super(console, "Clear Buffer");
        }

        @Override public boolean validConsoleState() {
            if (console == null || console.getTextPane() == null) {
                return false;
            }
            return true;
        }

        @Override public void doAction() {
            console.getTextPane().selectAll();
            console.getTextPane().replaceSelection("");
            console.prompt();
        }
    }

    /**
     * Selects everything contained in the console's {@code JTextPane}.
     * 
     * @see javax.swing.text.JTextComponent#selectAll()
     */
    private static class SelectBufferAction extends MenuAction {

        public SelectBufferAction(final Console console) {
            super(console, "Select All");
        }

        @Override public boolean validConsoleState() {
            if (console == null || console.getTextPane() == null) {
                return false;
            }
            return true;
        }

        @Override public void doAction() {
            console.getTextPane().selectAll();
        }
    }
}
