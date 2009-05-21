/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
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

// TODO(jon): think about introducing preconditions and postconditions.
public final class Contract {
    private Contract() { }

    public static <T> T notNull(T object) {
        if (object == null)
            throw new NullPointerException();
        return object;
    }

    public static <T> T notNull(T object, Object message) {
        if (object == null)
            throw new NullPointerException(String.valueOf(message));
        return object;
    }

    public static <T> T notNull(T object, String format, Object... values) {
        if (object == null)
            throw new NullPointerException(String.format(format, values));
        return object;
    }

    public static void checkArg(boolean expression) {
        if (!expression)
            throw new IllegalArgumentException();
    }

    public static void checkArg(boolean expression, Object message) {
        if (!expression)
            throw new IllegalArgumentException(String.valueOf(message));
    }

    public static void checkArg(boolean expression, String format, Object... values) {
        if (!expression)
            throw new IllegalArgumentException(String.format(format, values));
    }
}
