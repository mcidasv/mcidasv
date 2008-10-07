package edu.wisc.ssec.mcidasv.jython;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;

/**
 * A {@code Command} is an action that can alter the state of an 
 * {@link Interpreter}.
 */
public abstract class Command {
    /** Console that created this command. */
    protected Console console;

    /**
     * Creates a command.
     * 
     * @param console Console that created this command.
     */
    public Command(final Console console) {
        this.console = console;
    }

    /**
     * Hook to provide various implementations of command execution.
     * 
     * @param interpreter Jython interpreter that will execute the command.
     * 
     * @throws Exception An error was encountered executing the command. Jython
     * will catch three standard Python exceptions: SyntaxError, ValueError, 
     * and OverflowError. Other exceptions are thrown.
     */
    public abstract void execute(final Interpreter interpreter)
        throws Exception;

    protected InputStream getInputStream(final String path) throws Exception {
        File f = new File(path);
        if (f.exists())
            return f.toURL().openStream();

        URL url = getClass().getResource(path);
        if (url != null) 
            return url.openStream();

        return null;
    }
}

/**
 * This class is a type of {@link Command} that represents a line of Jython. 
 * These sorts of commands are only created by user input in a {@link Console}.
 */
class LineCommand extends Command {
    /** The line of jython that needs to be passed to the interpreter */
    private String command;

    /**
     * Creates a command based upon the contents of {@code command}.
     * 
     * @param console Console where the specified text came from.
     * @param command Text that will be passed to an {@link Interpreter} for
     * execution.
     */
    public LineCommand(final Console console, final String command) {
        super(console);
        this.command = command;
    }

    /**
     * Attempts to execute a line of Jython. Displays the appropriate prompt
     * on {@link Command#console}, depending upon whether Jython requires more
     * input.
     * 
     * @param interpreter Interpreter that will execute this command.
     * 
     * @throws Exception See {@link Command#execute(Interpreter)}.
     */
    public void execute(final Interpreter interpreter) throws Exception {
        if (!interpreter.push(command)) {
            interpreter.handleStreams(console, command);
            console.prompt();
        } else {
            console.moreInput();
        }
    }

    @Override public String toString() {
        return "[LineCommand@" + Integer.toHexString(hashCode()) +
            ": command=" + command + "]";
    }
}

/**
 * This class represents a {@link Command} that injects a standard Java 
 * variable into the local namespace of an {@link Interpreter}. This is useful
 * for allowing Jython to manipulate objects created by the IDV or McIDAS-V.
 */
class InjectCommand extends Command {
    /** Name Jython will use to refer to {@link #pyObject}. */
    private String name;

    /** Wrapper around the Java object that is being injected. */
    private PyObject pyObject;

    /**
     * Creates an injection command based upon the specified name and object.
     * 
     * @param console Likely not required in this context!
     * @param name Name Jython will use to refer to {@code pyObject}.
     * @param pyObject Wrapper around the Java object that is being injected.
     */
    public InjectCommand(final Console console, final String name, 
        final PyObject pyObject) 
    {
        super(console);
        this.name = name;
        this.pyObject = pyObject;
    }

    /**
     * Attempts to inject a variable created in Java into the local namespace 
     * of {@code interpreter}.
     * 
     * @param interpreter Interpreter that will execute this command.
     * 
     * @throws Exception if {@link Interpreter#set(String, PyObject)} had 
     * problems.
     */
    public void execute(final Interpreter interpreter) throws Exception {
        interpreter.set(name, pyObject);
    }

    @Override public String toString() {
        return "[InjectCommand@" + Integer.toHexString(hashCode()) + 
            ": name=" + name + ", pyObject=" + pyObject + "]";
    }
}

class EjectCommand extends Command {
    private String name;

    public EjectCommand(final Console console, final String name) {
        super(console);
        this.name = name;
    }

    public void execute(final Interpreter interpreter) throws Exception {
        interpreter.getLocals().__delitem__(name);
    }

    @Override public String toString() {
        return String.format("[EjectCommand@%x: name=%s]", hashCode(), name);
    }
}

// NOTE: this is different than loading a module!
class LoadFileCommand extends Command {
    private String name;
    private String path;

    public LoadFileCommand(final Console console, final String name, 
        final String path) 
    {
        super(console);
        this.name = name;
        this.path = path;
    }

    /**
     * Tries to load the file specified by {@code path} using {@code moduleName}
     * for the {@code __name__} attribute.
     * 
     * <p>If {@code moduleName} is not {@code __main__}, this command is 
     * basically the same thing as doing {@code from moduleName import *}.
     * 
     * <p>If {@code moduleName} <b>is</b> {@code __main__}, then this command
     * will work for {@code if __name__ == '__main__'} and will run main 
     * functions as expected.
     * 
     * @param interpreter Interpreter to use to load the specified file.
     */
    // TODO(jon): document the exceptions
    public void execute(final Interpreter interpreter) throws Exception {
        InputStream stream = getInputStream(path);
        if (stream == null)
            return;

        PyStringMap locals = (PyStringMap)interpreter.getLocals();
        PyObject currentName = locals.__getitem__(new PyString("__name__"));
        locals.__setitem__("__name__", new PyString(name));
        interpreter.execfile(stream);
        locals.__setitem__("__name__", currentName);
        interpreter.handleStreams(console, " ");
        console.prompt();
    }

    @Override public String toString() {
        return "[LoadFileCommand@" + Integer.toHexString(hashCode()) + 
            ": path=" + path + "]";
    }
}