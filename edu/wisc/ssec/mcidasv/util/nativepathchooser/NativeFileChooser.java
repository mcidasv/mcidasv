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

import javafx.stage.FileChooser;

import java.io.File;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class NativeFileChooser {
    private final Supplier<FileChooser> fileChooserFactory;

    /**
     * Constructs a new file chooser that will use the provided factory.
     * 
     * The factory is accessed from the JavaFX event thread, so it should either
     * be immutable or at least its state shouldn't be changed randomly while
     * one of the dialog-showing method calls is in progress.
     * 
     * The factory should create and set up the chooser, for example,
     * by setting extension filters. If there is no need to perform custom
     * initialization of the chooser, FileChooser::new could be passed as
     * a factory.
     * 
     * Alternatively, the method parameter supplied to the showDialog()
     * function can be used to provide custom initialization.
     * 
     * @param fileChooserFactory the function used to construct new choosers
     */
    public NativeFileChooser(Supplier<FileChooser> fileChooserFactory) {
        this.fileChooserFactory = fileChooserFactory;
    }

    /**
     * Shows the FileChooser dialog by calling the provided method.
     * 
     * Waits for one second for the dialog-showing task to start in the JavaFX
     * event thread, then throws an IllegalStateException if it didn't start.
     * 
     * @see #showDialog(java.util.function.Function, long, java.util.concurrent.TimeUnit) 
     * @param <T> the return type of the method, usually File or List&lt;File&gt;
     * @param method a function calling one of the dialog-showing methods
     * @return whatever the method returns
     */
    public <T> T showDialog(Function<FileChooser, T> method) {
        return showDialog(method, 1, TimeUnit.SECONDS);
    }

    /**
     * Shows the FileChooser dialog by calling the provided method. The dialog 
     * is created by the factory supplied to the constructor, then it is shown
     * by calling the provided method on it, then the result is returned.
     * <p>
     * Everything happens in the right threads thanks to
     * {@link SynchronousJFXCaller}. The task performed in the JavaFX thread
     * consists of two steps: construct a chooser using the provided factory
     * and invoke the provided method on it. Any exception thrown during these
     * steps will be rethrown in the calling thread, which shouldn't
     * normally happen unless the factory throws an unchecked exception.
     * </p>
     * <p>
     * If the calling thread is interrupted during either the wait for
     * the task to start or for its result, then null is returned and
     * the Thread interrupted status is set.
     * </p>
     * @param <T> return type (usually File or List&lt;File&gt;)
     * @param method a function that calls the desired FileChooser method
     * @param timeout time to wait for Platform.runLater() to <em>start</em>
     * the dialog-showing task (once started, it is allowed to run as long
     * as needed)
     * @param unit the time unit of the timeout argument
     * @return whatever the method returns
     * @throws IllegalStateException if Platform.runLater() fails to start
     * the dialog-showing task within the given timeout
     */
    public <T> T showDialog(Function<FileChooser, T> method,
            long timeout, TimeUnit unit) {
        Callable<T> task = () -> {
            FileChooser chooser = fileChooserFactory.get();
            return method.apply(chooser);
        };
        SynchronousJFXCaller<T> caller = new SynchronousJFXCaller<>(task);
        try {
            return caller.call(timeout, unit);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception ex) {
            throw new AssertionError("Got unexpected checked exception from"
                    + " SynchronousJFXCaller.call()", ex);
        }
    }

    /**
     * Shows a FileChooser using FileChooser.showOpenDialog().
     * 
     * @see #showDialog(java.util.function.Function, long, java.util.concurrent.TimeUnit) 
     * @return the return value of FileChooser.showOpenDialog()
     */
    public File showOpenDialog() {
        return showDialog(chooser -> chooser.showOpenDialog(null));
    }

    /**
     * Shows a FileChooser using FileChooser.showSaveDialog().
     * 
     * @see #showDialog(java.util.function.Function, long, java.util.concurrent.TimeUnit) 
     * @return the return value of FileChooser.showSaveDialog()
     */
    public File showSaveDialog() {
        return showDialog(chooser -> chooser.showSaveDialog(null));
    }

    /**
     * Shows a FileChooser using FileChooser.showOpenMultipleDialog().
     * 
     * @see #showDialog(java.util.function.Function, long, java.util.concurrent.TimeUnit) 
     * @return the return value of FileChooser.showOpenMultipleDialog()
     */
    public List<File> showOpenMultipleDialog() {
        return showDialog(chooser -> chooser.showOpenMultipleDialog(null));
    }

//    public static void main(String[] args) {
//        javafx.embed.swing.JFXPanel dummy = new javafx.embed.swing.JFXPanel();
//        Platform.setImplicitExit(false);
//        try {
//            SynchronousJFXFileChooser chooser = new SynchronousJFXFileChooser(() -> {
//                FileChooser ch = new FileChooser();
//                ch.setTitle("Open any file you wish");
//                return ch;
//            });
//            File file = chooser.showOpenDialog();
//            System.out.println(file);
//            // this will throw an exception:
//            chooser.showDialog(ch -> ch.showOpenDialog(null), 1, TimeUnit.NANOSECONDS);
//        } finally {
//            Platform.exit();
//        }
//    }
}
