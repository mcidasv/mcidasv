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
        notNull(console, "Cannot provide a null Jython console");
        notNull(interpreter, "Cannot provide a null Jython interpreter");
        this.type = notNull(type, "Cannot provide a null output type");
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
        ByteArrayOutputStream stream = streamMap.get(id());
        if (stream == null) {
            return "null";
        } else {
            return stream.toString();
        }
    }
}
