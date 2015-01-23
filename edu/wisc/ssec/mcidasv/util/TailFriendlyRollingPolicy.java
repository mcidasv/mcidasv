/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2015
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Date;
import java.util.concurrent.Future;

import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.helper.ArchiveRemover;
import ch.qos.logback.core.rolling.helper.AsynchronousCompressor;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.Compressor;
import ch.qos.logback.core.rolling.helper.FileFilterUtil;
import ch.qos.logback.core.util.FileUtil;

/**
 * This Logback {@literal "rolling policy"} copies the contents of a log file
 * (in this case, mcidasv.log) to the specified destination, and then
 * {@literal "zeroes out"} the original log file. This approach allows McIDAS-V
 * users to run a command like {@literal "tail -f mcidasv.log"} without any
 * issue. Even on Windows.
 */
public class TailFriendlyRollingPolicy<E> extends TimeBasedRollingPolicy<E> {

    private Compressor compressor;

    Future<?> future;

    @Override public void rollover() throws RolloverFailure {

        // when rollover is called the elapsed period's file has
        // been already closed. This is a working assumption of this method.

        TimeBasedFileNamingAndTriggeringPolicy timeBasedFileNamingAndTriggeringPolicy = getTimeBasedFileNamingAndTriggeringPolicy();
        String elapsedPeriodsFileName =
            timeBasedFileNamingAndTriggeringPolicy.getElapsedPeriodsFileName();

        String elapsedPeriodStem = FileFilterUtil.afterLastSlash(elapsedPeriodsFileName);

        if (compressionMode == CompressionMode.NONE) {
            String src = getParentsRawFileProperty();
            if (src != null) {
                if (isFileEmpty(src)) {
                    addInfo("File '"+src+"' exists and is zero-length; avoiding copy");
                } else {
                    renameByCopying(src, elapsedPeriodsFileName);
                }
            } // else { nothing to do if CompressionMode == NONE and parentsRawFileProperty == null }
        } else {
            if (getParentsRawFileProperty() == null) {
                future = asyncCompress(elapsedPeriodsFileName, elapsedPeriodsFileName, elapsedPeriodStem);
            } else {
                future = renamedRawAndAsyncCompress(elapsedPeriodsFileName, elapsedPeriodStem);
            }
        }

        ArchiveRemover archiveRemover = getTimeBasedFileNamingAndTriggeringPolicy().getArchiveRemover();

        if (archiveRemover != null) {
            archiveRemover.clean(new Date(timeBasedFileNamingAndTriggeringPolicy.getCurrentTime()));
        }
    }

    Future<?> asyncCompress(String nameOfFile2Compress,
                            String nameOfCompressedFile, String innerEntryName)
        throws RolloverFailure
    {
        AsynchronousCompressor ac = new AsynchronousCompressor(compressor);
        return ac.compressAsynchronously(nameOfFile2Compress, nameOfCompressedFile, innerEntryName);
    }

    Future<?> renamedRawAndAsyncCompress(String nameOfCompressedFile,
                                         String innerEntryName)
        throws RolloverFailure
    {
        String parentsRawFile = getParentsRawFileProperty();
        String tmpTarget = parentsRawFile + System.nanoTime() + ".tmp";
        renameByCopying(parentsRawFile, tmpTarget);
        return asyncCompress(tmpTarget, nameOfCompressedFile, innerEntryName);
    }

    /**
     * Copies the contents of {@code src} into {@code target}, and then
     * {@literal "zeroes out"} {@code src}.
     *
     * @param src Path to the file to be copied. Cannot be {@code null}.
     * @param target Path to the destination file. Cannot be {@code null}.
     * @throws RolloverFailure
     */
    public void renameByCopying(String src, String target)
        throws RolloverFailure
    {
        FileUtil fileUtil = new FileUtil(getContext());
        fileUtil.copy(src, target);
        try (FileOutputStream writer = new FileOutputStream(src)) {
            addInfo("zeroing out " + src);
        } catch (IOException e) {
            addError("Could not reset " + src, e);
        }
    }

    /**
     * Determine if the file at the given path is zero length.
     *
     * @param filepath Path to the file to be tested. Cannot be {@code null}.
     *
     * @return {@code true} if {@code filepath} exists and is empty.
     */
    private static boolean isFileEmpty(String filepath) {
        File f = new File(filepath);
        return f.exists() && (f.length() == 0L);
    }
}
