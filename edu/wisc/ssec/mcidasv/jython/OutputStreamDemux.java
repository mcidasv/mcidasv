package edu.wisc.ssec.mcidasv.jython;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OutputStreamDemux extends ByteArrayOutputStream {

    protected enum OutputType { NORMAL, ERROR };

    private OutputType type;

    private final Map<String, ByteArrayOutputStream> streamMap = new ConcurrentHashMap<String, ByteArrayOutputStream>();
    private final Map<String, Interpreter> interpreterMap = new ConcurrentHashMap<String, Interpreter>();
    private final Map<String, Console> consoleMap = new ConcurrentHashMap<String, Console>();

    private static String id() {
        return Thread.currentThread().getName();
    }

    public synchronized void addStream(final Console console, final Interpreter interpreter, final OutputType type) {
        if (console == null)
            throw new NullPointerException("Cannot provide a null Jython console");
        if (interpreter == null)
            throw new NullPointerException("Cannot provide a null Jython interpreter");
        if (type == null)
            throw new NullPointerException("Cannot provide a null output type");

        this.type = type;
        String threadId = id();
        streamMap.put(threadId, new ByteArrayOutputStream());
        interpreterMap.put(threadId, interpreter);
        consoleMap.put(threadId, console);
    }

    @Override public void close() throws IOException {
        streamMap.get(id()).close();
    }

    @Override public synchronized void flush() throws IOException {
        streamMap.get(id()).flush();
        Console console = consoleMap.get(id());
        Interpreter interpreter = interpreterMap.get(id());
        interpreter.handleStreams(console, null);
    }

    @Override public void write(byte[] b) throws IOException {
        streamMap.get(id()).write(b);
    }

    @Override public void write(byte[] b, int off, int len) {
        streamMap.get(id()).write(b, off, len);
    }

    @Override public void write(int b) {
        streamMap.get(id()).write(b);
    }
    
    @Override public void reset() {
        streamMap.get(id()).reset();
    }
    
    @Override public int size() {
        return streamMap.get(id()).size();
    }
    
    @Override public byte[] toByteArray() {
        return streamMap.get(id()).toByteArray();
    }
    
    @Deprecated @Override public String toString(int hibyte) {
        return streamMap.get(id()).toString();
    }
    
    @Override public String toString(String charsetName) throws UnsupportedEncodingException {
        return streamMap.get(id()).toString(charsetName);
    }
    
    @Override public void writeTo(OutputStream out) throws IOException {
        streamMap.get(id()).writeTo(out);
    }
    
    @Override public String toString() {
        return streamMap.get(id()).toString();
    }
}
