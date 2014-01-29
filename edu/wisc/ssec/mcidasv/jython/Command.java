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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.python.core.Py;
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

    /**
     * Creates a {@link InputStream} using {@code path}. It's here entirely for
     * convenience.
     * 
     * @param path Path to the desired file.
     * 
     * @return {code InputStream} for {@code path}.
     * 
     * @throws Exception if there was badness.
     */
    protected InputStream getInputStream(final String path) throws Exception {
        File f = new File(path);
        if (f.exists()) {
            return f.toURI().toURL().openStream();
        }
        URL url = getClass().getResource(path);
        if (url != null) { 
            return url.openStream();
        }
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
        if (!interpreter.push(console, command)) {
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
//class InjectCommand extends Command {
//    /** Name Jython will use to refer to {@link #pyObject}. */
//    private String name;
//
//    /** Wrapper around the Java object that is being injected. */
//    private PyObject pyObject;
//
//    /**
//     * Creates an injection command based upon the specified name and object.
//     * 
//     * @param console Likely not required in this context!
//     * @param name Name Jython will use to refer to {@code pyObject}.
//     * @param pyObject Wrapper around the Java object that is being injected.
//     */
//    public InjectCommand(final Console console, final String name, 
//        final PyObject pyObject) 
//    {
//        super(console);
//        this.name = name;
//        this.pyObject = pyObject;
//    }
//
//    /**
//     * Attempts to inject a variable created in Java into the local namespace 
//     * of {@code interpreter}.
//     * 
//     * @param interpreter Interpreter that will execute this command.
//     * 
//     * @throws Exception if {@link Interpreter#set(String, PyObject)} had 
//     * problems.
//     */
//    public void execute(final Interpreter interpreter) throws Exception {
//        interpreter.set(name, pyObject);
//    }
//
//    @Override public String toString() {
//        return "[InjectCommand@" + Integer.toHexString(hashCode()) + 
//            ": name=" + name + ", pyObject=" + pyObject + "]";
//    }
//}
class InjectCommand extends Command {
    /** Name Jython will use to refer to {@link #object}. */
    private String name;

    /** Wrapper around the Java object that is being injected. */
    private Object object;

    /**
     * Creates an injection command based upon the specified name and object.
     * 
     * @param console Likely not required in this context!
     * @param name Name Jython will use to refer to {@code object}.
     * @param object Wrapper around the Java object that is being injected.
     */
    public InjectCommand(final Console console, final String name, 
        final Object object) 
    {
        super(console);
        this.name = name;
        this.object = object;
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
        interpreter.set(name, object);
    }

    @Override public String toString() {
        return "[InjectCommand@" + Integer.toHexString(hashCode()) + 
            ": name=" + name + ", object=" + object + "]";
    }
}

/**
 * This class represents a {@link Command} that removes an object from the 
 * local namespace of an {@link Interpreter}. These commands can remove any 
 * Jython objects, while {@link InjectCommand} may only inject Java objects.
 */
class EjectCommand extends Command {
    /** Name of the Jython object to remove. */
    private String name;

    /**
     * Creates an ejection command for {@code name}.
     * 
     * @param console Console that requested {@code name}'s removal.
     * @param name Name of the Jython object that needs removin'.
     */
    public EjectCommand(final Console console, final String name) {
        super(console);
        this.name = name;
    }

    /**
     * Attempts to remove whatever Jython knows as {@code name} from the local
     * namespace of {@code interpreter}.
     * 
     * @param interpreter Interpreter whose local namespace is required.
     * 
     * @throws Exception if {@link PyObject#__delitem__(PyObject)} had some
     * second thoughts about ejection.
     */
    public void execute(final Interpreter interpreter) throws Exception {
        interpreter.getLocals().__delitem__(name);
    }

    @Override public String toString() {
        return String.format("[EjectCommand@%x: name=%s]", hashCode(), name);
    }
}

// TODO(jon): when documenting this, make sure to note that the commands appear
// in the console as "normal" user input.
class BatchCommand extends Command {
    private final String bufferSource;
    private final List<String> commandBuffer;

    public BatchCommand(final Console console, final String bufferSource,
        final List<String> buffer) 
    {
        super(console);
        this.bufferSource = bufferSource;
        this.commandBuffer = new ArrayList<String>(buffer);
    }

    public void execute(final Interpreter interpreter) throws Exception {
        PyStringMap locals = (PyStringMap)interpreter.getLocals();
        PyObject currentName = locals.__getitem__(new PyString("__name__"));
        locals.__setitem__("__name__", new PyString("__main__"));

        for (String command : commandBuffer) {
            console.insert(Console.TXT_NORMAL, command);
            if (!interpreter.push(console, command)) {
                interpreter.handleStreams(console, command);
                console.prompt();
            } else {
                console.moreInput();
            }
        }
        locals.__setitem__("__name__", currentName);
        commandBuffer.clear();
    }

    @Override public String toString() {
        return String.format("[BatchCommand@%x: bufferSource=%s, commandBuffer=%s]",
            hashCode(), bufferSource, commandBuffer);
    }
}

class RegisterCallbackCommand extends Command {
    private final ConsoleCallback callback;
    public RegisterCallbackCommand(final Console console, final ConsoleCallback callback) {
        super(console);
        this.callback = callback;
    }

    public void execute(final Interpreter interpreter) throws Exception {
        if (interpreter == null) {
            throw new NullPointerException("Interpreter is null!");
        }
        interpreter.setCallbackHandler(callback);
    }
}

/**
 * This class is a type of {@link Command} that represents a request to use
 * Jython to run a file containing Jython statements. This is conceptually a 
 * bit similar to importing a module, but the loading is done behind the scenes
 * and you may specify whatever namespace you like (be careful!).
 */
class LoadFileCommand extends Command {
    /** Namespace to use when executing {@link #path}. */
    private String name;

    /** Path to the Jython file awaiting execution. */
    private String path;

    /**
     * Creates a command that will attempt to execute a Jython file in the 
     * namespace given by {@code name}.
     * 
     * @param console Originating console.
     * @param name Namespace to use when executing {@code path}.
     * @param path Path to a Jython file.
     */
    public LoadFileCommand(final Console console, final String name, 
        final String path) 
    {
        super(console);
        this.name = name;
        this.path = path;
    }

    /**
     * Tries to load the file specified by {@code path} using {@code moduleName}
     * for the {@code __name__} attribute. Note that this command does not
     * currently display any results in the originating {@link Console}.
     * 
     * <p>If {@code moduleName} is not {@code __main__}, this command is 
     * basically the same thing as doing {@code from moduleName import *}.
     * 
     * <p>If {@code moduleName} <b>is</b> {@code __main__}, then this command
     * will work for {@code if __name__ == '__main__'} and will run main 
     * functions as expected.
     * 
     * @param interpreter Interpreter to use to load the specified file.
     * 
     * @throws Exception if Jython has a problem with running {@code path}.
     */
    public void execute(final Interpreter interpreter) throws Exception {
        InputStream stream = getInputStream(path);
        if (stream == null) {
            return;
        }
        PyStringMap locals = (PyStringMap)interpreter.getLocals();
        PyObject currentName = locals.__getitem__(new PyString("__name__"));
        locals.__setitem__("__name__", new PyString(name));
        interpreter.execfile(stream, path);
        locals.__setitem__("__name__", currentName);

        Py.getSystemState().stdout.invoke("flush");
        Py.getSystemState().stderr.invoke("flush");
//        interpreter.handleStreams(console, " ");
//        console.prompt();
    }

    @Override public String toString() {
        return "[LoadFileCommand@" + Integer.toHexString(hashCode()) + 
            ": path=" + path + "]";
    }
}
