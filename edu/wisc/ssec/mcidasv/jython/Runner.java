/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.jython;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;

import edu.wisc.ssec.mcidasv.jython.OutputStreamDemux.OutputType;

/**
 * This class represents a specialized {@link Thread} that creates and executes 
 * {@link Command}s. A {@link BlockingQueue} is used to maintain thread safety
 * and to cause a {@code Runner} to wait when the queue is at capacity or has
 * no {@code Command}s to execute.
 */
public class Runner extends Thread {

    /** The maximum number of {@link Command}s that can be queued. */
    private static final int QUEUE_CAPACITY = 10;

    private static final OutputStreamDemux STD_OUT = new OutputStreamDemux();
    private static final OutputStreamDemux STD_ERR = new OutputStreamDemux();
    
    /** Queue of {@link Command}s awaiting execution. */
    private BlockingQueue<Command> queue = 
        new ArrayBlockingQueue<Command>(QUEUE_CAPACITY, true);

    private final Console console;
    
    /** The Jython interpreter that will actually run the queued commands. */
    private Interpreter interpreter;

    /** Not in use yet. */
    private boolean interrupted = false;

    public Runner(final Console console) {
        this(console, Collections.<String>emptyList());
    }

    public Runner(final Console console, final List<String> commands) {
        this.console = console;
        for (String command : commands)
            queueLine(command);
    }

    /**
     * Registers a new callback handler. Currently this only forwards the new
     * handler to {@link Interpreter#setCallbackHandler(ConsoleCallback)}.
     * 
     * @param newCallback The callback handler to register.
     */
    protected void setCallbackHandler(final ConsoleCallback newCallback) {
        interpreter.setCallbackHandler(newCallback);
    }

    /**
     * Fetches, copies, and returns the {@link #interpreter}'s local namespace.
     * 
     * @return Copy of the interpreter's local namespace.
     */
    protected PyStringMap copyLocals() {
        return ((PyStringMap)interpreter.getLocals()).copy();
    }

    /**
     * Takes commands out of the queue and executes them. We get a lot of 
     * mileage out of BlockingQueue; it's thread-safe and will block if the 
     * queue is at capacity or empty.
     * 
     * <p>Please note that this method <b>needs</b> to be the first method that
     * gets called after creating a {@code Runner}.
     */
    public void run() {
        PySystemState sys = new PySystemState();

        // has to be in this order, for now :(
        interpreter = new Interpreter(sys, STD_OUT, STD_ERR);
        STD_OUT.addStream(console, interpreter, OutputType.NORMAL);
        STD_ERR.addStream(console, interpreter, OutputType.ERROR);
        while (true) {
            try {
                // woohoo for BlockingQueue!!
                Command command = queue.take();
                command.execute(interpreter);
            } catch (Exception e) {
                System.err.println("Runner.run: badness: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Queues up a series of Jython statements. Currently each command is 
     * treated as though the current user just entered it; the command appears
     * in the input along with whatever output the command generates.
     * 
     * @param console Console where the command originated.
     * @param source Batched command source. Anything but null is acceptable.
     * @param batch The actual commands to execute.
     */
    public void queueBatch(final String source,
        final List<String> batch) 
    {
        queueCommand(new BatchCommand(console, source, batch));
    }

    /**
     * Queues up a line of Jython for execution.
     * 
     * @param console Console where the command originated.
     * @param line Text of the command.
     */
    public void queueLine(final String line) {
        queueCommand(new LineCommand(console, line));
    }

    /**
     * Queues the addition of an object to {@code interpreter}'s local 
     * namespace.
     * 
     * @param console Likely not needed!
     * @param name Object name as it will appear to {@code interpreter}.
     * @param pyObject Object to put in {@code interpreter}'s local namespace.
     */
    public void queueObject(final String name,
        final PyObject pyObject) 
    {
        queueCommand(new InjectCommand(console, name, pyObject));
    }

    /**
     * Queues the removal of an object from {@code interpreter}'s local 
     * namespace. 
     * 
     * @param console Console Of Origin!
     * @param name Name of the object to be removed, <i>as it appears to
     * Jython</i>.
     * 
     * @see #queueObject(Console, String, PyObject)
     */
    public void queueRemoval(final String name) {
        queueCommand(new EjectCommand(console, name));
    }

    /**
     * Queues up a Jython file to be run by {@code interpreter}.
     * 
     * @param console Likely not needed!
     * @param name {@code __name__} attribute to use for loading {@code path}.
     * @param path The path to the Jython file.
     */
    public void queueFile(final String name,
        final String path) 
    {
        queueCommand(new LoadFileCommand(console, name, path));
    }

    /**
     * Queues up a command for execution.
     * 
     * @param command Command to place in the execution queue.
     */
    private void queueCommand(final Command command) {
        assert command != null : command;
        try {
            queue.put(command);
        } catch (InterruptedException e) {
            System.err.println("Runner.queueCommand: " + e.getMessage());
            System.err.println("Runner.queueCommand: command: " + command);
        }
    }

    @Override public String toString() {
        return "[Runner@" + Integer.toHexString(hashCode()) + 
            ": interpreter=" + interpreter + ", interrupted=" + interrupted +
            ", QUEUE_CAPACITY=" + QUEUE_CAPACITY + ", queue=" + queue + "]"; 
    }
}
