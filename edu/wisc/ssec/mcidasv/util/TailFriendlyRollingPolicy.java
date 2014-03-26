package edu.wisc.ssec.mcidasv.util;

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
import ch.qos.logback.core.rolling.helper.RenameUtil;
import ch.qos.logback.core.util.FileUtil;

public class TailFriendlyRollingPolicy<E> extends TimeBasedRollingPolicy<E> {

    private Compressor compressor;
    private RenameUtil renameUtil = new RenameUtil();
    Future<?> future;

    @Override public void rollover() throws RolloverFailure {

        // when rollover is called the elapsed period's file has
        // been already closed. This is a working assumption of this method.

        TimeBasedFileNamingAndTriggeringPolicy timeBasedFileNamingAndTriggeringPolicy = getTimeBasedFileNamingAndTriggeringPolicy();
        String elapsedPeriodsFileName =
            timeBasedFileNamingAndTriggeringPolicy.getElapsedPeriodsFileName();

        String elapsedPeriodStem = FileFilterUtil.afterLastSlash(elapsedPeriodsFileName);

        if (compressionMode == CompressionMode.NONE) {
            if (getParentsRawFileProperty() != null) {
                renameByCopying(getParentsRawFileProperty(), elapsedPeriodsFileName);
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

    Future asyncCompress(String nameOfFile2Compress, String nameOfCompressedFile, String innerEntryName)
        throws RolloverFailure {
        AsynchronousCompressor ac = new AsynchronousCompressor(compressor);
        return ac.compressAsynchronously(nameOfFile2Compress, nameOfCompressedFile, innerEntryName);
    }

    Future renamedRawAndAsyncCompress(String nameOfCompressedFile, String innerEntryName)
        throws RolloverFailure {
        String parentsRawFile = getParentsRawFileProperty();
        String tmpTarget = parentsRawFile + System.nanoTime() + ".tmp";
        renameByCopying(parentsRawFile, tmpTarget);
        return asyncCompress(tmpTarget, nameOfCompressedFile, innerEntryName);
    }

    public void renameByCopying(String src, String target)
        throws RolloverFailure {

        FileUtil fileUtil = new FileUtil(getContext());
        fileUtil.copy(src, target);

        FileOutputStream writer = null;
        try {
            writer = new FileOutputStream(src);
            addInfo("zeroing out " + src);
        } catch (IOException e) {
            addError("Could not reset " + src, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    addError("Could not close " + src, e);
                }
            }
        }

    }

}
