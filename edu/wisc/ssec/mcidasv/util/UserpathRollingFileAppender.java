package edu.wisc.ssec.mcidasv.util;

import ch.qos.logback.core.rolling.RollingFileAppender;

import java.io.File;

/**
 * Logback {@literal "file appender"} that uses some knowledge of McIDAS-V to
 * infer a default path to {@code mcidasv.log} if the user has run McIDAS-V
 * without setting the {@code mcv.logpath} property.
 *
 * <p>If {@code mcv.logpath} was not set, the default log path will be
 * {@code MCVUSERPATH/mcidasv.log}.</p>
 */
public class UserpathRollingFileAppender<E> extends RollingFileAppender<E> {
    @Override public void setFile(String file) {
        if ("mcv.logpath_IS_UNDEFINED".equals(file)) {
            String logPath = System.getProperty("mcv.userpath") + File.separatorChar + "mcidasv.log";
            addInfo("using default logpath="+logPath);
            super.setFile(logPath);
        } else {
            addInfo("using specified logpath="+file);
            super.setFile(file);
        }
    }
}
