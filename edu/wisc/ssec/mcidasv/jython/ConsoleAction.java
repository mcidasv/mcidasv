package edu.wisc.ssec.mcidasv.jython;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JTextPane;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;

import edu.wisc.ssec.mcidasv.jython.Console.Actions;

public abstract class ConsoleAction extends TextAction {
    protected Console console;

    protected ConsoleAction(final Console console, final Actions type) {
        super(type.getId());
        this.console = console;

        JTextPane textPane = console.getTextPane();
        Keymap keyMap = textPane.getKeymap();
        keyMap.addActionForKeyStroke(type.getKeyStroke(), this);
    }

    public abstract void actionPerformed(final ActionEvent e);
}

class EnterAction extends ConsoleAction {
    private static final long serialVersionUID = 6866731355386212866L;

    public EnterAction(final Console console, final Actions type) {
        super(console, type);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleEnter();
    }
}

class DeleteAction extends ConsoleAction {
    private static final long serialVersionUID = 4084595316508126660L;

    public DeleteAction(final Console console, final Actions type) {
        super(console, type);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleDelete();
    }
}

class HomeAction extends ConsoleAction {
    private static final long serialVersionUID = 8416339385843554054L;

    public HomeAction(final Console console, final Actions type) {
        super(console, type);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleHome();
    }
}

class EndAction extends ConsoleAction {
    private static final long serialVersionUID = 5990936637916440399L;

    public EndAction(final Console console, final Actions type) {
        super(console, type);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleEnd();
    }
}

class UpAction extends ConsoleAction {
    private static final long serialVersionUID = 6710943250074726107L;
    private Action defaultAction;
    
    public UpAction(final Console console, final Actions type) {
        super(console, type);
        defaultAction = console.getTextPane().getActionMap().get(DefaultEditorKit.upAction);
    }

    public void actionPerformed(final ActionEvent e) {
        defaultAction.actionPerformed(e);
    }
}

class DownAction extends ConsoleAction {
    private static final long serialVersionUID = 5700659549452276829L;
    private Action defaultAction;

    public DownAction(final Console console, final Actions type) {
        super(console, type);
        defaultAction = console.getTextPane().getActionMap().get(DefaultEditorKit.downAction);
    }

    public void actionPerformed(final ActionEvent e) {
        defaultAction.actionPerformed(e);
    }
}