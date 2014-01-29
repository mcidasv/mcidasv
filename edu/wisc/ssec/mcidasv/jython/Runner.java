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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.jython.OutputStreamDemux.OutputType;

/**
 * This class represents a specialized {@link Thread} that creates and executes 
 * {@link Command}s. A {@link BlockingQueue} is used to maintain thread safety
 * and to cause a {@code Runner} to wait when the queue is at capacity or has
 * no {@code Command}s to execute.
 */
public class Runner extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    /** The maximum number of {@link Command}s that can be queued. */
    private static final int QUEUE_CAPACITY = 10;

    /** 
     * Acts like a global output stream that redirects data to whichever 
     * {@link Console} matches the current thread name.
     */
    private final OutputStreamDemux STD_OUT;

    /** 
     * Acts like a global error stream that redirects data to whichever 
     * {@link Console} matches the current thread name.
     */
    private final OutputStreamDemux STD_ERR;

    /** Queue of {@link Command}s awaiting execution. */
    private final BlockingQueue<Command> queue;

    /** */
    private final Console console;

    /** */
    private final PySystemState systemState;

    /** The Jython interpreter that will actually run the queued commands. */
    private final Interpreter interpreter;

    /** Not in use yet. */
    private boolean interrupted = false;

    /**
     * 
     * 
     * @param console
     */
    public Runner(final Console console) {
        this(console, Collections.<String>emptyList());
    }

    /**
     * 
     * 
     * @param console
     * @param commands
     */
    public Runner(final Console console, final List<String> commands) {
        notNull(console, commands);
        this.console = console;
        this.STD_ERR = new OutputStreamDemux();
        this.STD_OUT = new OutputStreamDemux();
        this.queue = new ArrayBlockingQueue<Command>(QUEUE_CAPACITY, true);
        this.systemState = new PySystemState();
        this.interpreter = new Interpreter(systemState, STD_OUT, STD_ERR);
        for (String command : commands) {
            queueLine(command);
        }
    }

    /**
     * Registers a new callback handler. Currently this only forwards the new
     * handler to {@link Interpreter#setCallbackHandler(ConsoleCallback)}.
     * 
     * @param newCallback The callback handler to register.
     */
    protected void setCallbackHandler(final ConsoleCallback newCallback) {
        queueCommand(new RegisterCallbackCommand(console, newCallback));
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
        synchronized (this) {
            STD_OUT.addStream(console, interpreter, OutputType.NORMAL);
            STD_ERR.addStream(console, interpreter, OutputType.ERROR);
        }
        while (true) {
            try {
                // woohoo for BlockingQueue!!
                Command command = queue.take();
                command.execute(interpreter);
            } catch (Exception e) {
                logger.error("failed to execute", e);
            }
        }
    }

    /**
     * Queues up a series of Jython statements. Currently each command is 
     * treated as though the current user just entered it; the command appears
     * in the input along with whatever output the command generates.
     * 
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
     * @param line Text of the command.
     */
    public void queueLine(final String line) {
        queueCommand(new LineCommand(console, line));
    }

    /**
     * Queues the addition of an object to {@code interpreter}'s local 
     * namespace.
     *
     * @param name Object name as it will appear to {@code interpreter}.
     * @param object Object to put in {@code interpreter}'s local namespace.
     */
    public void queueObject(final String name, final Object object) {
        queueCommand(new InjectCommand(console, name, object));
    }

    /**
     * Queues the removal of an object from {@code interpreter}'s local 
     * namespace. 
     * 
     * @param name Name of the object to be removed, <i>as it appears to
     * Jython</i>.
     * 
     * @see Runner#queueObject(String, Object)
     */
    public void queueRemoval(final String name) {
        queueCommand(new EjectCommand(console, name));
    }

    /**
     * Queues up a Jython file to be run by {@code interpreter}.
     *
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
            logger.warn("msg='{}' command='{}'", e.getMessage(), command);
        }
    }

    @Override public String toString() {
        return "[Runner@" + Integer.toHexString(hashCode()) + 
            ": interpreter=" + interpreter + ", interrupted=" + interrupted +
            ", QUEUE_CAPACITY=" + QUEUE_CAPACITY + ", queue=" + queue + "]"; 
    }
}
