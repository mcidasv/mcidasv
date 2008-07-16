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
    private boolean moreInput = false;

    /**
     * Creates a Jython interpreter based upon the specified system state and
     * whose output streams are mapped to the specified byte streams.
     * 
     * <p>Additionally, the &quot;__main__&quot; module is imported by default 
     * so that the locals namespace makes sense.
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

        setOut(stdout);
        setErr(stderr);
        
        PyModule mod = imp.addModule("__main__");
        PyStringMap locals = ((PyStringMap)mod.__dict__).copy();
        setLocals(locals);
    }

    /**
     * Here's the magic! Basically just accumulates a buffer that gets passed
     * off to jython-land until it can run.
     * 
     * @param line A Jython command.
     * @return False if Jython did something. True if more input is needed.
     */
    public boolean push(final String line) {
        if (buffer.length() > 0)
            buffer.append("\n");

        buffer.append(line);
        moreInput = runsource(buffer.toString(), CONSOLE_FILENAME);
        if (!moreInput)
            resetbuffer();

        return moreInput;
    }

    /**
     * @return Whether or not Jython needs more input to run something.
     */
    public boolean needMoreInput() {
        return moreInput;
    }

    /**
     * Sends the contents of {@link #stdout} and {@link #stderr} on their 
     * merry way.
     * 
     * @param console Console where the command originated.
     * @param command The command that was executed.
     */
    public void handleStreams(final Console console, final String command) {
        sendOut(console, command);
        sendErr(console, command);
    }

    // TODO(jon): generalize sendOut/sendErr

    /**
     * Sends any data in {@link #stdout} to the specified console.
     * 
     * @param console Console where <code>command</code> originated.
     * @param command The command that was executed.
     */
    private void sendOut(final Console console, final String command) {
        if (stdout.size() > 1) {
            String text = stdout.toString();
            String str = text.substring(0, text.length() - ((command.length() == 0) ? 0 : 1));
            console.result(str);
        }
        stdout.reset();
    }

    /**
     * Sends any data in {@link #stderr} to the specified console.
     * 
     * @param console Console where <code>command</code> originated.
     * @param command The command that was executed.
     */
    private void sendErr(final Console console, final String command) {
        if (stderr.size() > 1) {
            String text = stderr.toString();
            String str = text.substring(0, text.length() - ((command.length() == 0) ? 0 : 1));
            console.error(str);
        }
        stderr.reset();
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
