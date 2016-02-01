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

package edu.wisc.ssec.mcidasv.util.pathwatcher;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.python.bouncycastle.asn1.x500.style.RFC4519Style.l;

import java.io.IOException;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Taken from https://gist.github.com/hindol-viz/394ebc553673e2cd0699

/**
 * A simple class which can monitor files and notify interested parties
 * (i.e. listeners) of file changes.
 *
 * This class is kept lean by only keeping methods that are actually being
 * called.
 */
public class SimpleDirectoryWatchService implements DirectoryWatchService,
        Runnable
{

    private static final Logger logger =
            LoggerFactory.getLogger(SimpleDirectoryWatchService.class);

    private final WatchService mWatchService;
    private final AtomicBoolean mIsRunning;
    private final ConcurrentMap<WatchKey, Path> mWatchKeyToDirPathMap;
    private final ConcurrentMap<Path, Set<OnFileChangeListener>> mDirPathToListenersMap;
    private final ConcurrentMap<OnFileChangeListener, Set<PathMatcher>> mListenerToFilePatternsMap;

    /**
     * A simple no argument constructor for creating a
     * {@code SimpleDirectoryWatchService}.
     *
     * @throws IOException If an I/O error occurs.
     */
    public SimpleDirectoryWatchService() throws IOException {
        mWatchService = FileSystems.getDefault().newWatchService();
        mIsRunning = new AtomicBoolean(false);
        mWatchKeyToDirPathMap = newConcurrentMap();
        mDirPathToListenersMap = newConcurrentMap();
        mListenerToFilePatternsMap = newConcurrentMap();
    }

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    private static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        return new ConcurrentHashMap<>();
    }

    private static <T> Set<T> newConcurrentSet() {
        return Collections.newSetFromMap(newConcurrentMap());
    }

    public static PathMatcher matcherForGlobExpression(String globPattern) {
        if ((globPattern == null) || globPattern.isEmpty()) {
            globPattern = "*";
        }
        return FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
    }

    public static boolean matches(Path input, PathMatcher pattern) {
        return pattern.matches(input);
    }

    public static boolean matchesAny(Path input, Set<PathMatcher> patterns) {
        for (PathMatcher pattern : patterns) {
            if (matches(input, pattern)) {
                return true;
            }
        }

        return false;
    }

    private Path getDirPath(WatchKey key) {
        return mWatchKeyToDirPathMap.get(key);
    }

    private Set<OnFileChangeListener> getListeners(Path dir) {
        return mDirPathToListenersMap.get(dir);
    }

    private Set<PathMatcher> getPatterns(OnFileChangeListener listener) {
        return mListenerToFilePatternsMap.get(listener);
    }

    private Path getDir(OnFileChangeListener listener) {

        Set<Map.Entry<Path, Set<OnFileChangeListener>>> entries =
                mDirPathToListenersMap.entrySet();

        Path result = null;
        for (Map.Entry<Path, Set<OnFileChangeListener>> entry : entries) {
            Set<OnFileChangeListener> listeners = entry.getValue();
            if (listeners.contains(listener)) {
                result = entry.getKey();
                break;
            }
        }

        return result;
    }

    private WatchKey getWatchKey(Path dir) {
        Set<Map.Entry<WatchKey, Path>> entries = mWatchKeyToDirPathMap.entrySet();
        WatchKey key = null;
        for (Map.Entry<WatchKey, Path> entry : entries) {
            if (entry.getValue().equals(dir)) {
                key = entry.getKey();
                break;
            }
        }
        return key;
    }

    private Set<OnFileChangeListener> matchedListeners(Path dir, Path file) {
        return getListeners(dir)
                .stream()
                .filter(listener -> matchesAny(file, getPatterns(listener)))
                .collect(Collectors.toSet());
    }

    private void notifyListeners(WatchKey key) {
        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind eventKind = event.kind();

            // Overflow occurs when the watch event queue is overflown
            // with events.
            if (eventKind.equals(OVERFLOW)) {
                // TODO: Notify all listeners.
                return;
            }

            WatchEvent<Path> pathEvent = cast(event);
            Path file = pathEvent.context();
            String completePath =  getDirPath(key).resolve(file).toString();

            if (eventKind.equals(ENTRY_CREATE)) {
                matchedListeners(getDirPath(key), file)
                        .forEach(l -> l.onFileCreate(completePath));
            } else if (eventKind.equals(ENTRY_MODIFY)) {
                matchedListeners(getDirPath(key), file)
                        .forEach(l -> l.onFileModify(completePath));
            } else if (eventKind.equals(ENTRY_DELETE)) {
                matchedListeners(getDirPath(key), file)
                        .forEach(l -> l.onFileDelete(completePath));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public void register(OnFileChangeListener listener,
                                   String dirPath, String... globPatterns)
            throws IOException
    {
        Path dir = Paths.get(dirPath);

        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException(dirPath + " not a directory.");
        }

        if (!mDirPathToListenersMap.containsKey(dir)) {
            // May throw
            WatchKey key = dir.register(
                    mWatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE
            );

            mWatchKeyToDirPathMap.put(key, dir);
            mDirPathToListenersMap.put(dir, newConcurrentSet());
        }

        getListeners(dir).add(listener);

        Set<PathMatcher> patterns = newConcurrentSet();

        for (String globPattern : globPatterns) {
            patterns.add(matcherForGlobExpression(globPattern));
        }

        if (patterns.isEmpty()) {
            // Match everything if no filter is found
            patterns.add(matcherForGlobExpression("*"));
        }

        mListenerToFilePatternsMap.put(listener, patterns);

        logger.trace("Watching files matching {} under '{}' for changes",
                Arrays.toString(globPatterns), dirPath);
    }

    /**
     * {@inheritDoc}
     */
    public void unregister(OnFileChangeListener listener) {
        Path dir = getDir(listener);

        mDirPathToListenersMap.get(dir).remove(listener);

        // is this step truly needed?
        if (mDirPathToListenersMap.get(dir).isEmpty()) {
            mDirPathToListenersMap.remove(dir);
        }

        mListenerToFilePatternsMap.remove(listener);

        WatchKey key = getWatchKey(dir);
        if (key != null) {
            mWatchKeyToDirPathMap.remove(key);
            key.cancel();
        }
        logger.trace("listener unregistered");
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterAll() {
        // can't simply clear the key->dir map; need to cancel
        mWatchKeyToDirPathMap.keySet().forEach(WatchKey::cancel);

        mWatchKeyToDirPathMap.clear();
        mDirPathToListenersMap.clear();
        mListenerToFilePatternsMap.clear();
    }

    /**
     * Start this {@code SimpleDirectoryWatchService} instance by spawning a
     * new thread.
     *
     * @see #stop()
     */
    @Override public void start() {
        if (mIsRunning.compareAndSet(false, true)) {
            String name = DirectoryWatchService.class.getSimpleName();
            Thread runnerThread = new Thread(this, name);
            runnerThread.start();
        }
    }

    /**
     * Stop this {@code SimpleDirectoryWatchService} thread.
     *
     * <p>The killing happens lazily, giving the running thread an opportunity
     * to finish the work at hand.</p>
     *
     * @see #start()
     */
    @Override public void stop() {
        // Kill thread lazily
        mIsRunning.set(false);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRunning() {
        return mIsRunning.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void run() {
        logger.info("Starting file watcher service.");

        while (mIsRunning.get()) {
            WatchKey key;
            try {
                key = mWatchService.take();
            } catch (InterruptedException e) {
                logger.trace("{} service interrupted.",
                        DirectoryWatchService.class.getSimpleName());
                break;
            }

            if (null == getDirPath(key)) {
                logger.error("Watch key not recognized.");
                continue;
            }

            notifyListeners(key);

            // Reset key to allow further events for this key to be processed.
            boolean valid = key.reset();
            if (!valid) {
                mWatchKeyToDirPathMap.remove(key);
                if (mWatchKeyToDirPathMap.isEmpty()) {
                    break;
                }
            }
        }

        mIsRunning.set(false);
        logger.trace("Stopping file watcher service.");
    }
}
