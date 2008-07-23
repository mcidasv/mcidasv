package edu.wisc.ssec.mcidasv.jython;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.python.core.PyObject;
import org.python.core.PySystemState;

/**
 * This class represents a specialized {@link Thread} that creates and executes 
 * {@link Command}s. A {@link BlockingQueue} is used to maintain thread safety
 * and to cause a {@code Runner} to wait when the queue is at capacity or has
 * no {@code Command}s to execute.
 */
public class Runner extends Thread {

    /** The maximum number of {@link Command}s that can be queued. */
    // TODO(jon): investigate this!
    private static final int QUEUE_CAPACITY = 10;

    /** Queue of {@link Command}s awaiting execution. */
    private BlockingQueue<Command> queue = 
        new ArrayBlockingQueue<Command>(QUEUE_CAPACITY, true);

    /** The Jython interpreter that will actually run the queued commands. */
    private Interpreter interpreter;

    /** Not in use yet. */
    private boolean interrupted = false;

    public Runner() {
        this(Collections.<String>emptyList());
    }

    public Runner(final List<String> commands) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        PySystemState sys = new PySystemState();

        interpreter = new Interpreter(sys, stdout, stderr);
        for (String command : commands)
            interpreter.runsource(command);
    }

    /**
     * Takes commands out of the queue and executes them. We get a lot of 
     * mileage out of BlockingQueue; it's thread-safe and will block if the 
     * queue is at capacity or empty.
     */
    public void run() {
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
     * Queues up a line of Jython for execution.
     * 
     * @param console Console where the command originated.
     * @param line Text of the command.
     */
    public void queueLine(final Console console, final String line) {
        queueCommand(new LineCommand(console, line));
    }

    /**
     * Queues the addition of an object to {@code interpreter}'s local namespace.
     * 
     * @param console Likely not needed!
     * @param name Object name as it will appear to {@code interpreter}.
     * @param pyObject Object to place in {@code interpreter}'s local namespace.
     */
    public void queueObject(final Console console, final String name, 
        final PyObject pyObject) 
    {
        queueCommand(new InjectCommand(console, name, pyObject));
    }

    /**
     * Queues up a Jython file to be run by {@code interpreter}.
     * 
     * @param console Likely not needed!
     * @param name {@code __name__} attribute to use for loading {@code path}.
     * @param path The path to the Jython file.
     */
    public void queueFile(final Console console, final String name, 
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
