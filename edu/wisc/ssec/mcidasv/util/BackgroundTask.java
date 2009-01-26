package edu.wisc.ssec.mcidasv.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Background task class supporting cancellation, completion notification, 
 * and progress notification. Courtesy of <i>Java Concurrency in Practice</i>, 
 * written by Brian Goetz and Tim Peierls.
 */
//TODO: couldn't find a license for this code? is it public domain?
public abstract class BackgroundTask <V> implements Runnable, Future<V> {
    private final FutureTask<V> computation = new Computation();

    private class Computation extends FutureTask<V> {
        public Computation() {
            super(new Callable<V>() {
                public V call() throws Exception {
                    return BackgroundTask.this.compute();
                }
            });
        }

        protected final void done() {
            GuiExecutor.instance().execute(new Runnable() {
                public void run() {
                    V value = null;
                    Throwable thrown = null;
                    boolean cancelled = false;
                    try {
                        value = get();
                    } catch (ExecutionException e) {
                        thrown = e.getCause();
                    } catch (CancellationException e) {
                        cancelled = true;
                    } catch (InterruptedException consumed) {
                    } finally {
                        onCompletion(value, thrown, cancelled);
                    }
                };
            });
        }
    }

    protected void setProgress(final int current, final int max) {
        GuiExecutor.instance().execute(new Runnable() {
            public void run() {
                onProgress(current, max);
            }
        });
    }

    // Called in the background thread
    protected abstract V compute() throws Exception;

    // Called in the event thread
    protected void onCompletion(V result, Throwable exception,
                                boolean cancelled) {
    }

    protected void onProgress(int current, int max) {
    }

    // Other Future methods just forwarded to computation
    public boolean cancel(boolean mayInterruptIfRunning) {
        return computation.cancel(mayInterruptIfRunning);
    }

    public V get() throws InterruptedException, ExecutionException {
        return computation.get();
    }

    public V get(long timeout, TimeUnit unit)
            throws InterruptedException,
            ExecutionException,
            TimeoutException {
        return computation.get(timeout, unit);
    }

    public boolean isCancelled() {
        return computation.isCancelled();
    }

    public boolean isDone() {
        return computation.isDone();
    }

    public void run() {
        computation.run();
    }
}
