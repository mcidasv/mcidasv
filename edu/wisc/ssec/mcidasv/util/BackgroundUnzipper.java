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
package edu.wisc.ssec.mcidasv.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.SwingWorker;
import javax.swing.Timer;

public class BackgroundUnzipper extends SwingWorker<Long, Long>{

    private final String zipFile;
    private CountingInputStream countingStream;
    private ZipInputStream zipStream;

    private long totalSize = 1;
    
    private String currentEntry;

    private final ActionListener taskPerformer = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            getPercentage();
        }
    };

    private final Timer taskTimer = new Timer(250, taskPerformer);
    
    public BackgroundUnzipper(final String zipFile) {
        this.zipFile = zipFile;
    }

    public Long getCurrentBytes() {
        return countingStream.getTotalBytesRead();
    }

    public String getCurrentEntry() {
        return currentEntry;
    }
    
    public long getPercentage() {
        double current = new Double(countingStream.getTotalBytesRead()).doubleValue();
        double total = new Double(totalSize).doubleValue();
        long val = Math.round((current / total) * 100);
        setProgress(new Long(val).intValue());
        return val;
    }
    
    protected Long doInBackground() throws Exception {
        
        countingStream = new CountingInputStream(getInputStream(zipFile));
        zipStream = new ZipInputStream(countingStream);
        totalSize = new File(zipFile).length();
        taskTimer.start();
        ZipEntry entry = null;
        while (!isCancelled() && ((entry = zipStream.getNextEntry()) != null)) {
            publish(countingStream.getTotalBytesRead());
            System.err.println("entry="+entry.getName());
            currentEntry = entry.getName();
            zipStream.closeEntry();
        }
        zipStream.close();
        countingStream.close();
        taskTimer.stop();
        return countingStream.getTotalBytesRead();
    }

    protected void process(List<Long> durr) {
        System.err.println("read "+countingStream.getTotalBytesRead()+" bytes so far...");
    }

    private InputStream getInputStream(final String path) {
        File f = new File(path.trim());
        if (!f.exists()) {
           return null;
        }

        try {
            URL url = f.toURI().toURL();
            URLConnection connection = url.openConnection();
            return new BufferedInputStream(connection.getInputStream());
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class CountingInputStream extends FilterInputStream {

        private long totalBytes = 0;

        protected CountingInputStream(final InputStream in) {
            super(in);
        }

        public long getTotalBytesRead() {
            return totalBytes;
        }

        @Override public int read() throws IOException {
            int byteValue = super.read();
            if (byteValue != -1) totalBytes++;
            return byteValue;
        }

        @Override public int read(byte[] b) throws IOException {
            int bytesRead = super.read(b);
            if (bytesRead != -1)
                totalBytes += bytesRead;
            return bytesRead;
        }

        @Override public int read(byte[] b, int off, int len) throws IOException {
            int bytesRead = super.read(b,off,len);
            if (bytesRead != -1)
                totalBytes += bytesRead;
            return bytesRead;
        }
    }

}
