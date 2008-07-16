package edu.wisc.ssec.mcidasv.jython;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.python.core.PySystemState;

public class Runner extends Thread {

    /** The maximum number of commands that can be queued. */
    // TODO(jon): investigate this!
    private static final int QUEUE_CAPACITY = 10;

    /** Queue of jython commands awaiting execution. */
    private BlockingQueue<Command> queue = 
        new ArrayBlockingQueue<Command>(QUEUE_CAPACITY, true);

    /** The jython interpreter that will actually run the queued commands. */
    private Interpreter interpreter;

    /** Not in use yet. */
    private boolean interrupted = false;

    public Runner() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        PySystemState sys = new PySystemState();

        interpreter = new Interpreter(sys, stdout, stderr);
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
     * Queues up a line of jython for execution.
     * 
     * @param console The console where the command originated.
     * @param line The text of the command.
     */
    public void queueLine(final Console console, final String line) {
        Command command = new LineCommand(console, line);
        try {
            queue.put(command);
        } catch (InterruptedException e) {
            System.err.println("Runner.queueLine: caught interrupted exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
