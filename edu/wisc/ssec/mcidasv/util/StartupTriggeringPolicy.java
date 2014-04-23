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

import ch.qos.logback.core.joran.spi.NoAutoStart;
import ch.qos.logback.core.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.FileFilterUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * This is a Logback {@literal "triggering policy"} that forces a log
 * {@literal "roll"} upon starting McIDAS-V.
 *
 * <p>All credit for this belongs to
 * <a href="http://stackoverflow.com/a/12408445">this StackOverflow post</a>.</p>
 *
 */
@NoAutoStart
public class StartupTriggeringPolicy<E>
        extends DefaultTimeBasedFileNamingAndTriggeringPolicy<E> {

    private void renameOldLogDirectory() {
        String userpath = System.getProperty("mcv.userpath");
        if (userpath != null) {
            String oldLogPath = userpath + File.separatorChar + "logs";
            String newLogPath = userpath + File.separatorChar + "archived_logs";
            File oldDirectory = new File(oldLogPath);
            File newDirectory = new File(newLogPath);

            // T F = rename
            // F F = attempt to create
            // T T = remove old dir
            // F T = noop
            if (oldDirectory.exists() && !newDirectory.exists()) {
                oldDirectory.renameTo(newDirectory);
            } else if (!oldDirectory.exists() && !newDirectory.exists()) {
                if (!newDirectory.mkdir()) {
                    addWarn("Could not create '"+newLogPath+'\'');
                } else {
                    addInfo("Created '"+newLogPath+'\'');
                }
            } else if (oldDirectory.exists() && newDirectory.exists()) {
                addWarn("Both log directories exist; moving files from '" + oldLogPath + "' and attempting to delete");
                removeOldLogDirectory(oldDirectory, newDirectory);
            } else if (!oldDirectory.exists() && newDirectory.exists()) {
                addInfo('\''+oldLogPath+"' does not exist; no cleanup is required");
            } else {
                addWarn("Unknown state! oldDirectory.exists()='"+oldDirectory.exists()+"' newDirectory.exists()='"+newDirectory.exists()+'\'');
            }
        }
    }

    private void removeOldLogDirectory(File oldDirectory, File newDirectory) {
        File[] files = oldDirectory.listFiles();
        new Thread(asyncClearFiles(oldDirectory, newDirectory, files)).start();
    }

    private Runnable asyncClearFiles(final File oldDirectory, final File newDirectory, final File[] files) {
        return new Runnable() {
            public void run() {
                boolean success = true;
                for (File f : files) {
                    File newPath = new File(newDirectory, f.getName());
                    if (f.renameTo(newPath)) {
                        addInfo("Moved '"+f.getAbsolutePath()+"' to '"+newPath.getAbsolutePath()+'\'');
                    } else {
                        success = false;
                        addWarn("Could not move '"+f.getAbsolutePath()+"' to '"+newPath.getAbsolutePath()+'\'');
                    }
                }
                if (success) {
                    if (oldDirectory.delete()) {
                        addInfo("Removed '"+oldDirectory.getAbsolutePath()+'\'');
                    } else {
                        addWarn("Could not remove '"+oldDirectory.getAbsolutePath()+'\'');
                    }
                }
            }
        };
    }

    private void cleanupArchivedLogs(int keepFiles) {
        String userpath = System.getProperty("mcv.userpath");
        if (userpath != null) {
            File logDirectory = new File(userpath + File.separatorChar + "archived_logs");
            File[] files = logDirectory.listFiles();
            if (files.length > keepFiles) {
                new Thread(asyncCleanFiles(keepFiles, files)).start();
            }
        }
    }

    private Runnable asyncCleanFiles(final int keep, final File[] files) {
        return new Runnable() {
            public void run() {
                Arrays.sort(files, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
                    }
                });
                for (int i = keep-1; i < files.length; i++) {
                    addInfo("removing '"+files[i]+'\'');
                    files[i].delete();
                }
            }
        };
    }

    @Override public void start() {
        renameOldLogDirectory();
        super.start();
        nextCheck = 0L;
        isTriggeringEvent(null, null);
        try {
            tbrp.rollover();
            int maxHistory = tbrp.getMaxHistory();
            if (maxHistory > 0) {
                addInfo("keep "+maxHistory+" most recent archived logs");
                cleanupArchivedLogs(maxHistory);
            } else {
                addInfo("maxHistory not set; not cleaning archiving logs");
            }
        } catch (RolloverFailure e) {
            addError("could not perform rollover of log file", e);
        }
    }
}
