package edu.wisc.ssec.mcidasv.jython;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.border.BevelBorder;

import org.python.core.PyObject;

public class DefaultMenuWrangler implements MenuWrangler {

    private final Console console;

    private final CutTextAction cutAction;
    private final CopyTextAction copyAction;
    private final PasteTextAction pasteAction;

    private final ClearBufferAction clearAction;
    private final SelectBufferAction selectAction;

    public DefaultMenuWrangler(final Console console) {
        if (console == null)
            throw new NullPointerException("Cannot provide a null console.");
        this.console = console;

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

    public void stateChanged() {
        System.err.println("noop");
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
        // stuff.
        Map<String, PyObject> locals = console.getLocalNamespace();
        for (Map.Entry<String, PyObject> entry : locals.entrySet()) {

            String key = entry.getKey();
            PyObject value = entry.getValue();

            String itemName = key+": "+value.getClass().getName();

            JMenuItem item = new JMenuItem(itemName);
            item.setActionCommand(key);
            item.addActionListener(menuClickHandler);
            menu.add(item);
        }
        return menu;
    }
    

    private static abstract class MenuAction {
        protected final Console console;
        protected final String label;
        protected final JMenuItem item;
        protected MenuAction(final Console console, final String label) {
            this.console = console;
            this.label = label;
            this.item = buildMenuItem();
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

    private static class CutTextAction extends MenuAction {
        public CutTextAction(final Console console) {
            super(console, "Cut");
        }

        public boolean validConsoleState() {
            if (console == null || console.getTextPane() == null)
                return false;

            String selection = console.getTextPane().getSelectedText();
            if (selection != null && selection.length() > 0)
                return true;

            return false;
        }

        public void doAction() {
            console.getTextPane().cut();
        }
    }
    private static class CopyTextAction extends MenuAction {
        public CopyTextAction(final Console console) {
            super(console, "Copy");
        }

        public boolean validConsoleState() {
            if (console == null || console.getTextPane() == null)
                return false;
            String selection = console.getTextPane().getSelectedText();
            if (selection != null && selection.length() > 0)
                return true;
            return false;
        }

        public void doAction() {
            console.getTextPane().copy();
        }
    }
    private static class PasteTextAction extends MenuAction {
        public PasteTextAction(final Console console) {
            super(console, "Paste");
        }

        public boolean validConsoleState() {
            return false;
        }

        public void doAction() {
            
        }
    }
    private static class ClearBufferAction extends MenuAction {
        public ClearBufferAction(final Console console) {
            super(console, "Clear Buffer");
        }

        public boolean validConsoleState() {
            if (console == null || console.getTextPane() == null)
                return false;
            return true;
        }

        public void doAction() {
            console.getTextPane().selectAll();
            console.getTextPane().replaceSelection("");
            console.prompt();
        }
    }
    private static class SelectBufferAction extends MenuAction {
        public SelectBufferAction(final Console console) {
            super(console, "Select All");
        }

        public boolean validConsoleState() {
            if (console == null || console.getTextPane() == null)
                return false;
            return true;
        }

        public void doAction() {
            console.getTextPane().selectAll();
        }
    }
}
