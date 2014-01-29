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

package edu.wisc.ssec.mcidasv.control;


/**
 * Holds a set of definitions concerning McIDAS data.
 */
public interface McIdasComponents {

    /** Image frame component */
    public static final String IMAGE = "McIdasComponents.image";

    /** Graphics frame component */
    public static final String GRAPHICS = "McIdasComponents.graphics";

    /** Color Table frame component */
    public static final String COLORTABLE = "McIdasComponents.colortable";
    
    /** Annotation frame component */
    public static final String ANNOTATION = "McIdasComponents.annotation";
    
    /** Annotation frame component */
    public static final String FAKEDATETIME = "McIdasComponents.fakedatetime";
    
    /** Array of dirty frame info */
    public static final String DIRTYINFO = "McIdasComponents.dirtyinfo";
    
    /** Image dirty component */
    public static final String DIRTYIMAGE = "McIdasComponents.dirtyimage";

    /** Graphics dirty component */
    public static final String DIRTYGRAPHICS = "McIdasComponents.dirtygraphics";

    /** Color Table dirty component */
    public static final String DIRTYCOLORTABLE = "McIdasComponents.dirtycolortable";

}
