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
