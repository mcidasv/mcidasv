/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2022
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

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.IOException;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows for easy searching of files matching {@literal "glob"} patterns
 * (e.g. {@code *.py}) in a given directories and its subdirectories.
 *
 * <p>Note: the {@code findFiles(...)} methods will block until the search
 * finishes! If this is a concern, for the time being, please consider using
 * {@link #findFiles(String, String, int)} with a reasonable depth value.</p>
 */
public class FileFinder {

    // TODO(jon): make this async somehow

    // adapted from
    // https://docs.oracle.com/javase/tutorial/essential/io/find.html

    /** Logging object. */
    private static final Logger logger =
            LoggerFactory.getLogger(FileFinder.class);

    /**
     * Internal class used by the {@code findFiles(...)} methods to actually
     * {@literal "walk"} the directory tree.
     */
    private static class Finder extends SimpleFileVisitor<Path> {

        /** Pattern matcher. */
        private final PathMatcher matcher;

        /** {@code String} representations of matching {@link Path Paths}. */
        private final List<String> matches;

        /**
         * Creates a new file searcher.
         *
         * <p>Please see {@link FileSystem#getPathMatcher(String)} for more
         * details concerning patterns.</p>
         *
         * @param pattern Pattern to match against.
         */
        Finder(String pattern) {
            matches = new ArrayList<>();
            matcher =
                FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        }

        /**
         * Compare the given file or directory against the glob pattern.
         *
         * <p>If {@code file} matches, it is added to {@link #matches}.</p>
         *
         * @param file File (or directory) to compare against the glob pattern.
         *
         * @see #results()
         */
        void find(Path file) {
            Path name = file.getFileName();
            if ((name != null) && matcher.matches(name)) {
                matches.add(file.toString());
            }
        }

        /**
         * Prints the total number of matches to standard out.
         */
        void done() {
            // TODO(jon): not the most useful method...
            System.out.println("Matched: " + matches.size());
        }

        /**
         * Returns the matching paths as strings.
         *
         * @return {@code List} of the matching paths as {@code String} values.
         */
        List<String> results() {
            List<String> results = Collections.emptyList();
            if (!matches.isEmpty()) {
                results = new ArrayList<>(matches);
            }
            return results;
        }

        /**
         * Invokes pattern matching method on the given file.
         *
         * @param file File in question.
         * @param attrs Attributes of {@code dir}. Not currently used.
         *
         * @return Always returns {@link FileVisitResult#CONTINUE} (for now).
         */
        @Override public FileVisitResult visitFile(Path file,
                                                   BasicFileAttributes attrs)
        {
            find(file);
            return CONTINUE;
        }

        /**
         * Invokes the pattern matching method on the given directory.
         *
         * @param dir Directory in question.
         * @param attrs Attributes of {@code dir}. Not currently used.
         *
         * @return Always returns {@link FileVisitResult#CONTINUE} (for now).
         */
        @Override public FileVisitResult preVisitDirectory(
            Path dir,
            BasicFileAttributes attrs)
        {
            find(dir);
            return CONTINUE;
        }

        /**
         * Handle file {@literal "visitation"} errors.
         *
         * @param file File that could not be {@literal "visited"}.
         * @param exc Exception associated with {@literal "visit"} to
         *            {@code file}.
         *
         * @return Always returns {@link FileVisitResult#CONTINUE} (for now).
         *
         */
        @Override public FileVisitResult visitFileFailed(Path file,
                                                         IOException exc)
        {
            logger.warn("file='"+file+"'", exc);
            return CONTINUE;
        }
    }

    /**
     * Find files matching the specified {@literal "glob"} pattern in the given
     * directory (and all of its subdirectories).
     *
     * <p>Note: {@literal "glob"} patterns are simple DOS/UNIX style. Think
     * {@literal "*.py"}.</p>
     *
     * @param path Directory to search.
     * @param globPattern Pattern to match against.
     *
     * @return {@code List} of {@code String} versions of matching paths. The
     * list will be empty ({@link Collections#emptyList()} if there were no
     * matches.
     */
    public static List<String> findFiles(String path, String globPattern) {
        return findFiles(path, globPattern, Integer.MAX_VALUE);
    }

    /**
     * Find files matching the specified {@literal "glob"} pattern in the given
     * directory (and not exceeding the given {@literal "depth"}).
     *
     * <p>Note: {@literal "glob"} patterns are simple DOS/UNIX style. Think
     * {@literal "*.py"}.</p>
     *
     * @param path Directory to search.
     * @param globPattern Pattern to match against.
     * @param depth Maximum number of directory levels to visit.
     *
     * @return {@code List} of {@code String} versions of matching paths. The
     * list will be empty ({@link Collections#emptyList()} if there were no
     * matches.
     */
    public static List<String> findFiles(String path,
                                         String globPattern,
                                         int depth)
    {
        Finder f = new Finder(globPattern);
        List<String> results = Collections.emptyList();
        try {
            Files.walkFileTree(Paths.get(path),
                               EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                               depth,
                               f);
            results = f.results();
        } catch (IOException e) {
            logger.error("Could not search '"+path+"'", e);
        }
        return results;
    }
}
