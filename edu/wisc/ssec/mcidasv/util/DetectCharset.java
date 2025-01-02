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

package edu.wisc.ssec.mcidasv.util;

import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Based on the juniversalchardet example code.
 *
 * This code is primarily used by the {@literal "editFile"} function in 
 * {@code interactive.py}.
 */
public final class DetectCharset {

    private static final Logger logger = 
        LoggerFactory.getLogger(DetectCharset.class);

    private DetectCharset() { }

    public static String detect(String file) throws IOException {
        try (InputStream fis = new FileInputStream(file)) {
            UniversalDetector detector = new UniversalDetector(null);
            int nread;
            byte[] buf = new byte[4096];
            while (((nread = fis.read(buf)) > 0) && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }

            detector.dataEnd();

            String encoding = detector.getDetectedCharset();
            if (encoding != null) {
                logger.trace("detected encoding '{}'", encoding);
            } else {
                logger.trace("no encoding detected!");
            }

            detector.reset();
            return encoding;
        }
    }
}
