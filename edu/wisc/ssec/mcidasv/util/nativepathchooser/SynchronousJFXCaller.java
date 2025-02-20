/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.mcidasv.util.nativepathchooser;

import javax.swing.SwingUtilities;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// taken from http://stackoverflow.com/a/30004156

/**
 * A utility class to execute a Callable synchronously
 * on the JavaFX event thread.
 * 
 * @param <T> Return type of the {@link Callable}.
 */
public class SynchronousJFXCaller<T> {
    private final Callable<T> callable;

    /**
     * Constructs a new caller that will execute the provided callable.
     * 
     * The callable is accessed from the JavaFX event thread, so it should either
     * be immutable or at least its state shouldn't be changed randomly while
     * the call() method is in progress.
     * 
     * @param callable Action to execute on the JFX event thread.
     */
    public SynchronousJFXCaller(Callable<T> callable) {
        this.callable = callable;
    }

    /**
     * Executes the Callable.
     * <p>
     * A specialized task is run using Platform.runLater(). The calling thread
     * then waits first for the task to start, then for it to return a result.
     * Any exception thrown by the Callable will be rethrown in the calling
     * thread.
     * </p>
     * @param startTimeout time to wait for Platform.runLater() to <em>start</em>
     * the dialog-showing task
     * @param startTimeoutUnit the time unit of the startTimeout argument
     * @return whatever the Callable returns
     * @throws IllegalStateException if Platform.runLater() fails to start
     * the task within the given timeout
     * @throws InterruptedException if the calling (this) thread is interrupted
     * while waiting for the task to start or to get its result (note that the
     * task will still run anyway and its result will be ignored)
     */
    public T call(long startTimeout, TimeUnit startTimeoutUnit)
            throws Exception {
        final CountDownLatch taskStarted = new CountDownLatch(1);
        // Can't use volatile boolean here because only finals can be accessed
        // from closures like the lambda expression below.
        final AtomicBoolean taskCancelled = new AtomicBoolean(false);
        // disabling the modality emulation (for now?)
        // a trick to emulate modality:
        // final JDialog modalBlocker = new JDialog();
        // modalBlocker.setModal(true);
        // modalBlocker.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        // modalBlocker.setUndecorated(true);
        // modalBlocker.setOpacity(0.0f);
        // modalBlocker.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // final CountDownLatch modalityLatch = new CountDownLatch(1);
        final FutureTask<T> task = new FutureTask<T>(() -> {
            synchronized (taskStarted) {
                if (taskCancelled.get()) {
                    return null;
                } else {
                    taskStarted.countDown();
                }
            }
            try {
                return callable.call();
            } finally {
                // Wait until the Swing thread is blocked in setVisible():
                // modalityLatch.await();
                // and unblock it:
                // SwingUtilities.invokeLater(() ->
                //         modalBlocker.setVisible(false));
            }
        });
        Platform.runLater(task);
        if (!taskStarted.await(startTimeout, startTimeoutUnit)) {
            synchronized (taskStarted) {
                // the last chance, it could have been started just now
                if (!taskStarted.await(0, TimeUnit.MILLISECONDS)) {
                    // Can't use task.cancel() here because it would
                    // interrupt the JavaFX thread, which we don't own.
                    taskCancelled.set(true);
                    throw new IllegalStateException("JavaFX was shut down"
                            + " or is unresponsive");
                }
            }
        }
        // a trick to notify the task AFTER we have been blocked
        // in setVisible()
        SwingUtilities.invokeLater(() -> {
            // notify that we are ready to get the result:
//            modalityLatch.countDown();
        });
//        modalBlocker.setVisible(true); // blocks
//        modalBlocker.dispose(); // release resources
        try {
            return task.get();
        } catch (ExecutionException ex) {
            Throwable ec = ex.getCause();
            if (ec instanceof Exception) {
                throw (Exception) ec;
            } else if (ec instanceof Error) {
                throw (Error) ec;
            } else {
                throw new AssertionError("Unexpected exception type", ec);
            }
        }
    }

}
