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

// Taken from https://gist.github.com/hindol-viz/394ebc553673e2cd0699

/**
 * Interface definition for services.
 */
public interface Service {

    /**
     * Starts the service. This method blocks until the service has completely
     * started.
     */
    void start() throws Exception;

    /**
     * Stops the service. This method blocks until the service has completely
     * shut down.
     */
    void stop();

    /**
     * Checks to see if the service is still running.
     *
     * @return {@code true} if the service is running, {@code false} otherwise.
     */
    boolean isRunning();
}
