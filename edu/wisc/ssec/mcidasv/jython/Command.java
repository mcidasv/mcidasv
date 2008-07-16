package edu.wisc.ssec.mcidasv.jython;

public abstract class Command {
    protected Console console;

    public Command(final Console console) {
        this.console = console;
    }

    public abstract void execute(final Interpreter interpreter) throws Exception;
}

class LineCommand extends Command {
    private String command;

    public LineCommand(final Console console, final String command) {
        super(console);
        this.command = command;
    }

    public void execute(final Interpreter interpreter) throws Exception {
        if (!interpreter.push(command)) {
            interpreter.handleStreams(console, command);
            console.prompt();
        } else {
            console.moreInput();
        }
    }

    public String toString() {
        return command;
    }
}