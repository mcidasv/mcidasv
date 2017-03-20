/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2017
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
 * Interface definition for a callback to be invoked when a file under
 * watch is changed.
 */
public interface OnFileChangeListener {

    /**
     * Called when a file is created.
     *
     * @param filePath Path to file that was created.
     */
    default void onFileCreate(String filePath) {}

    /**
     * Called when a file is modified.
     *
     * @param filePath Path to file that was modified.
     */
    default void onFileModify(String filePath) {}

    /**
     * Called when a file is deleted.
     *
     * @param filePath Path to file that was removed.
     */
    default void onFileDelete(String filePath) {}
}
