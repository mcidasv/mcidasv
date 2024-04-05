/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
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

package edu.wisc.ssec.mcidasv.util.pathwatcher;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.concurrentMap;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.concurrentSet;

import java.io.IOException;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Adapted from https://gist.github.com/hindol-viz/394ebc553673e2cd0699

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
    
    /** Logging object. */
    private static final Logger logger =
        LoggerFactory.getLogger(SimpleDirectoryWatchService.class);
        
    /**
     * {@code WatchService} used to monitor changes in various
     * {@link Path Paths}.
     */
    private final WatchService mWatchService;
    
    /** Whether or not this {@link DirectoryWatchService} is running. */
    private final AtomicBoolean mIsRunning;
    
    /**
     * Mapping of monitoring {@literal "registration"} keys to the
     * {@link Path} that it will be watching.
     */
    private final Map<WatchKey, Path> mWatchKeyToDirPathMap;
    
    /**
     * Mapping of {@link Path Paths} to the {@link Set} of
     * {@link OnFileChangeListener OnFileChangeListeners} listening for
     * changes to the associated {@code Path}.
     */
    private final Map<Path, Set<OnFileChangeListener>> mDirPathToListenersMap;
    
    /**
     * Mapping of {@link OnFileChangeListener OnFileChangeListeners} to the
     * {@link Set} of patterns being used to observe changes in
     * {@link Path Paths} of interest.
     */
    private final Map<OnFileChangeListener, Set<PathMatcher>>
        mListenerToFilePatternsMap;
        
    /**
     * A simple no argument constructor for creating a
     * {@code SimpleDirectoryWatchService}.
     *
     * @throws IOException If an I/O error occurs.
     */
    public SimpleDirectoryWatchService() throws IOException {
        mWatchService = FileSystems.getDefault().newWatchService();
        mIsRunning = new AtomicBoolean(false);
        mWatchKeyToDirPathMap = concurrentMap();
        mDirPathToListenersMap = concurrentMap();
        mListenerToFilePatternsMap = concurrentMap();
    }
    
    /**
     * Utility method used to make {@literal "valid"} casts of the given
     * {@code event} to a specific type of {@link WatchEvent}.
     *
     * @param <T> Type to which {@code event} will be casted.
     * @param event Event to cast.
     *
     * @return {@code event} casted to {@code WatchEvent<T>}.
     */
    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
    
    /**
     * Returns a {@link PathMatcher} that performs {@literal "glob"} matches
     * with the given {@code globPattern} against the {@code String}
     * representation of {@link Path} objects.
     *
     * @param globPattern Pattern to match against. {@code null} or empty
     *                    {@code String} values will be converted to {@code *}.
     *
     * @return Path matching object for the given {@code globPattern}.
     *
     * @throws IOException if there was a problem creating the
     *                     {@code PathMatcher}.
     */
    private static PathMatcher matcherForGlobExpression(String globPattern)
        throws IOException
    {
        if ((globPattern == null) || globPattern.isEmpty()) {
            globPattern = "*";
        }
        
        return FileSystems.getDefault().getPathMatcher("glob:"+globPattern);
    }
    
    /**
     * Check the given {@code input} {@link Path} against the given {@code
     * pattern}.
     *
     * @param input Path to check.
     * @param pattern Pattern to check against. Cannot be {@code null}.
     *
     * @return Whether or not {@code input} matches {@code pattern}.
     */
    public static boolean matches(Path input, PathMatcher pattern) {
        return pattern.matches(input);
    }
    
    /**
     * Check the given {@code input} {@link Path} against <i>all</i> of the
     * specified {@code patterns}.
     *
     * @param input Path to check.
     * @param patterns {@link Set} of patterns to attempt to match
     *                 {@code input} against. Cannot be {@code null}.
     *
     * @return Whether or not {@code input} matches any of the given
     *         {@code patterns}.
     */
    private static boolean matchesAny(Path input, Set<PathMatcher> patterns) {
        for (PathMatcher pattern : patterns) {
            if (matches(input, pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the path associated with the given {@link WatchKey}.
     *
     * @param key {@code WatchKey} whose corresponding {@link Path} is being
     *            requested.
     *
     * @return Either the correspond {@code Path} or {@code null}.
     */
    private Path getDirPath(WatchKey key) {
        return mWatchKeyToDirPathMap.get(key);
    }
    
    /**
     * Get the {@link OnFileChangeListener OnFileChangeListeners} associated
     * with the given {@code path}.
     *
     * @param path Path whose listeners should be returned. Cannot be
     *             {@code null}.
     *
     * @return Either the {@link Set} of listeners associated with {@code path}
     *         or {@code null}.
     */
    private Set<OnFileChangeListener> getListeners(Path path) {
        return mDirPathToListenersMap.get(path);
    }
    
    /**
     * Get the {@link Set} of patterns associated with the given
     * {@link OnFileChangeListener}.
     *
     * @param listener Listener of interest.
     *
     * @return Either the {@code Set} of patterns associated with
     *         {@code listener} or {@code null}.
     */
    private Set<PathMatcher> getPatterns(OnFileChangeListener listener) {
        return mListenerToFilePatternsMap.get(listener);
    }
    
    /**
     * Get the {@link Path} associated with the given
     * {@link OnFileChangeListener}.
     *
     * @param listener Listener whose path is requested.
     *
     * @return Either the {@code Path} associated with {@code listener} or
     *         {@code null}.
     */
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
    
    /**
     * Get the monitoring {@literal "registration"} key associated with the
     * given {@link Path}.
     *
     * @param dir {@code Path} whose {@link WatchKey} is requested.
     *
     * @return Either the {@code WatchKey} corresponding to {@code dir} or
     *         {@code null}.
     */
    private WatchKey getWatchKey(Path dir) {
        Set<Map.Entry<WatchKey, Path>> entries =
            mWatchKeyToDirPathMap.entrySet();
            
        WatchKey key = null;
        for (Map.Entry<WatchKey, Path> entry : entries) {
            if (entry.getValue().equals(dir)) {
                key = entry.getKey();
                break;
            }
        }
        
        return key;
    }
    
    /**
     * Get the {@link Set} of
     * {@link OnFileChangeListener OnFileChangeListeners} that should be
     * notified that {@code file} has changed.
     *
     * @param dir Directory containing {@code file}.
     * @param file File that changed.
     *
     * @return {@code Set} of listeners that should be notified that
     *         {@code file} has changed.
     */
    private Set<OnFileChangeListener> matchedListeners(Path dir, Path file) {
        return getListeners(dir)
                .stream()
                .filter(listener -> matchesAny(file, getPatterns(listener)))
                .collect(Collectors.toSet());
    }
    
    /**
     * Method responsible for notifying listeners when a file matching their
     * relevant pattern has changed.
     *
     * Note: {@literal "change"} means one of:
     * <ul>
     *   <li>file creation</li>
     *   <li>file removal</li>
     *   <li>file contents changing</li>
     * </ul>
     *
     * @param key {@link #mWatchService} {@literal "registration"} key for
     *            one of the {@link Path Paths} being watched. Cannot be
     *            {@code null}.
     *
     * @see #run()
     */
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
     * Method responsible for notifying listeners when the path they are 
     * watching has been deleted (or otherwise {@literal "invalidated"} 
     * somehow).
     * 
     * @param key Key that has become invalid. Cannot be {@code null}.
     */
    private void notifyListenersOfInvalidation(WatchKey key) {
        Path dir = getDirPath(key);
        getListeners(dir).forEach(l -> l.onWatchInvalidation(dir.toString()));
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
            mDirPathToListenersMap.put(dir, concurrentSet());
        }
        
        getListeners(dir).add(listener);
        
        Set<PathMatcher> patterns = concurrentSet();
        
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
    @Override public void unregisterAll() {
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
    @Override public boolean isRunning() {
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
                // order matters here; if you remove the key first, we can't
                // work out who the appropriate listeners are.
                notifyListenersOfInvalidation(key);
                mWatchKeyToDirPathMap.remove(key);
            }
        }
        
        mIsRunning.set(false);
        logger.trace("Stopping file watcher service.");
    }
}
