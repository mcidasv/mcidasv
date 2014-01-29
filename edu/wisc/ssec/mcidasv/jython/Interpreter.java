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

import java.io.ByteArrayOutputStream;

import org.python.core.PyModule;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.core.imp;
import org.python.util.InteractiveInterpreter;

public class Interpreter extends InteractiveInterpreter {
    /** Dummy filename for the interactive interpreter. */
    private static final String CONSOLE_FILENAME = "<console>";

    /** Stream used for error output. */
    private ByteArrayOutputStream stderr;

    /** Stream used for normal output. */
    private ByteArrayOutputStream stdout;

    /** Whether or not jython needs more input to run something. */
    private boolean moreInput;

    /** A hook that allows external classes to respond to events. */
    private ConsoleCallback callback;

    /** Whether or not Jython is working on something */
    private boolean thinking;

    /**
     * Creates a Jython interpreter based upon the specified system state and
     * whose output streams are mapped to the specified byte streams.
     * 
     * <p>Additionally, the {@literal "__main__"} module is imported by 
     * default so that the locals namespace makes sense.
     * 
     * @param state The system state you want to use with the interpreter.
     * @param stdout The stream Jython will use for standard output.
     * @param stderr The stream Jython will use for error output.
     */
    public Interpreter(final PySystemState state, 
        final ByteArrayOutputStream stdout, 
        final ByteArrayOutputStream stderr) 
    {
        super(null, state);
        this.stdout = stdout;
        this.stderr = stderr;
        this.callback = new DummyCallbackHandler();
        this.moreInput = false;
        this.thinking = false;

        setOut(stdout);
        setErr(stderr);

        PyModule mod = imp.addModule("__main__");
        PyStringMap locals = ((PyStringMap)mod.__dict__).copy();
        setLocals(locals);
    }

    /**
     * Registers a new callback handler with the interpreter. This mechanism
     * allows external code to easily react to events taking place in the
     * interpreter.
     * 
     * @param newCallback The new callback handler.
     */
    protected void setCallbackHandler(final ConsoleCallback newCallback) {
        callback = newCallback;
    }

    /**
     * Here's the magic! Basically just accumulates a buffer that gets passed
     * off to jython-land until it can run.
     * 
     * @param line A Jython command.
     * @return False if Jython did something. True if more input is needed.
     */
    public boolean push(Console console, final String line) {
        if (buffer.length() > 0) {
            buffer.append('\n');
        }

        thinking = true;
        buffer.append(line);
        moreInput = runsource(buffer.toString(), CONSOLE_FILENAME);
        if (!moreInput) {
            String bufferCopy = new String(buffer);
            resetbuffer();
            callback.ranBlock(bufferCopy);
        }

        thinking = false;
        return moreInput;
    }

    /**
     * Determines whether or not Jython is busy.
     * 
     * @return {@code true} if busy, {@code false} otherwise.
     */
    public boolean isBusy() {
        return thinking;
    }

    /**
     * 
     * 
     * @return Whether or not Jython needs more input to run something.
     */
    public boolean needMoreInput() {
        return moreInput;
    }

    /**
     * Sends the contents of {@link #stdout} and {@link #stderr} on their 
     * merry way. Both streams are emptied as a result.
     * 
     * @param console Console where the command originated.
     * @param command The command that was executed. Null values are permitted,
     * as they signify that no command was entered for any generated output.
     */
    public void handleStreams(final Console console, final String command) {
        String output = clearStream(command, stdout);
        if (output.length() != 0) {
            if (command != null) {
                console.result(output);
            } else {
                console.generatedOutput(output);
            }
        }

        String error = clearStream(command, stderr);
        if (error.length() != 0) {
            if (command != null) {
                console.error(error);
            } else {
                console.generatedError(error);
            }
        }
    }

    /**
     * Removes and returns all existing text from {@code stream}.
     * 
     * @param command Command that was executed. Null values are permitted and
     * imply that no command is {@literal "associated"} with text in 
     * {@code stream}.
     * @param stream Stream to be cleared out.
     * 
     * @return The contents of {@code stream} before it was reset.
     * @see #handleStreams(Console, String)
     */
    private static String clearStream(final String command, final ByteArrayOutputStream stream) {
        String output = "";
        if (command == null) {
            output = stream.toString();
        } else if (stream.size() > 1) {
            String text = stream.toString();
            int end = text.length() - ((command.length() == 0) ? 0 : 1);
            output = text.substring(0, end);
        }
        stream.reset();
        return output;
    }

    /**
     * Sends error information to the specified console.
     * 
     * @param console The console that caused the exception.
     * @param e The exception!
     */
    public void handleException(final Console console, final Throwable e) {
        handleStreams(console, " ");
        console.error(e.toString());
        console.prompt();
    }
}
