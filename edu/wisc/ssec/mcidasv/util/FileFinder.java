/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2016
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Allows for easy searching of files matching {@literal "glob"} patterns
 * (e.g. {@code *.py}) in a given directories and its subdirectories.
 *
 * <p>Note: this class is not thread safe!</p>
 */
public class FileFinder {

    // TODO(jon): make thread safe?

    // TODO(jon): along the lines of thread safety...allow for cancelling?

    // adapted from https://docs.oracle.com/javase/tutorial/essential/io/find.html

    private static final Logger logger =
            LoggerFactory.getLogger(FileFinder.class);

    public static class Finder
            extends SimpleFileVisitor<Path> {

        private final PathMatcher matcher;
        private final List<String> matches;

        Finder(String pattern) {
            matches = new ArrayList<>();
            matcher =
                FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        }

        // Compares the glob pattern against
        // the file or directory name.

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
            if (name != null && matcher.matches(name)) {
                matches.add(file.toString());
            }
        }

        // Prints the total number of
        // matches to standard out.
        void done() {
            System.out.println("Matched: " + matches.size());
        }

        List<String> results() {
            return matches;
        }


        /**
         * Invokes pattern matching method on the given file.
         *
         * @param file File in question.
         * @param attrs Attributes of {@code file}.
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
         * @param attrs Attributes of {@code dir}.
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

        @Override public FileVisitResult visitFileFailed(Path file,
                                                         IOException exc)
        {
            logger.warn("file='"+file+"'", exc);
            return CONTINUE;
        }
    }

    public static List<String> findFiles(String path, String globPattern) {
        Path p = Paths.get(path);
        Finder f = new Finder(globPattern);
        List<String> results = Collections.emptyList();
        try {
            Files.walkFileTree(p, f);
            results = f.results();
        } catch (IOException e) {
            logger.error("Could not search '"+path+"'", e);
        }
        return results;
    }
}
