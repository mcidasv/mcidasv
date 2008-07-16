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
    private static final long serialVersionUID = 6866731355386212866L;
    public static final String NAME = "jython.enter";

    public EnterAction(final Console console) {
        super(console, NAME);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleEnter();
    }
}

class DeleteAction extends ConsoleAction {
    private static final long serialVersionUID = 4084595316508126660L;
    public static final String NAME = "jython.delete";

    public DeleteAction(final Console console) {
        super(console, NAME);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleDelete();
    }
}

class HomeAction extends ConsoleAction {
    private static final long serialVersionUID = 8416339385843554054L;
    public static final String NAME = "jython.home";

    public HomeAction(final Console console) {
        super(console, NAME);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleHome();
    }
}

class EndAction extends ConsoleAction {
    private static final long serialVersionUID = 5990936637916440399L;
    public static final String NAME = "jython.end";

    public EndAction(final Console console) {
        super(console, NAME);
    }

    public void actionPerformed(final ActionEvent e) {
        console.handleEnd();
    }
}

class UpAction extends ConsoleAction {
    private static final long serialVersionUID = 6710943250074726107L;
    public static final String NAME = "jython.up";

    public UpAction(final Console console) {
        super(console, NAME);
    }

    public void actionPerformed(final ActionEvent e) {
//        System.err.println("actionPerformed: " + NAME);
    }
}

class DownAction extends ConsoleAction {
    private static final long serialVersionUID = 5700659549452276829L;
    public static final String NAME = "jython.down";

    public DownAction(final Console console) {
        super(console, NAME);
    }

    public void actionPerformed(final ActionEvent e) {
//        System.err.println("actionPerformed: " + NAME);
    }
}