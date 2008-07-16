package edu.wisc.ssec.mcidasv.jython;

import java.awt.event.ActionEvent;

import javax.swing.text.TextAction;

public abstract class ConsoleAction extends TextAction {
    protected Console console;

    public ConsoleAction(final Console console, final String name) {
        super(name);
        this.console = console;
    }

    public abstract void actionPerformed(final ActionEvent e);
}

class EnterAction extends ConsoleAction {
    public static final String NAME = "jython.enter";

    public EnterAction(final Console console) {
        super(console, NAME);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleEnter();
    }
}

class DeleteAction extends ConsoleAction {
    public static final String NAME = "jython.delete";

    public DeleteAction(final Console console) {
        super(console, NAME);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleDelete();
    }
}

class HomeAction extends ConsoleAction {
    public static final String NAME = "jython.home";

    public HomeAction(final Console console) {
        super(console, NAME);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleHome();
    }
}

class EndAction extends ConsoleAction {
    public static final String NAME = "jython.end";

    public EndAction(final Console console) {
        super(console, NAME);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleEnd();
    }
}

class UpAction extends ConsoleAction {
    public static final String NAME = "jython.up";

    public UpAction(final Console console) {
        super(console, NAME);
    }

    public void actionPerformed(final ActionEvent e) {
//        System.err.println("actionPerformed: " + NAME);
    }
}

class DownAction extends ConsoleAction {
    public static final String NAME = "jython.down";

    public DownAction(final Console console) {
        super(console, NAME);
    }

    public void actionPerformed(final ActionEvent e) {
//        System.err.println("actionPerformed: " + NAME);
    }
}