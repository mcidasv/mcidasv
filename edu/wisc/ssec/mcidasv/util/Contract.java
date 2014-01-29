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

import java.util.List;

/**
 * This is a {@literal "convenience"} class--use these methods to reduce 
 * boilerplate parameter verification. For example:
 * <pre>
 * if (str == null) {
 *     throw new NullPointerException("null is bad");
 * }</pre>
 * can be replaced with
 * <pre>
 * notNull(str, "null is bad");</pre>
 * 
 * Remember that these methods are used to signal an error in the <b>calling</b>
 * method!
 */
public final class Contract {
    private Contract() { }

    /**
     * Ensures that a parameter passed to the calling method is not 
     * {@code null}.
     * 
     * @param object Object to test.
     * 
     * @return {@code object}, if it is not {@code null}.
     * 
     * @throws NullPointerException if {@code object} is {@code null}.
     */
    public static <T> T notNull(T object) {
        if (object == null) {
            throw new NullPointerException();
        }
        return object;
    }

    /**
     * Ensures that a parameter passed to the calling method is not 
     * {@code null}.
     * 
     * @param object Object to test.
     * @param message Exception message to use if {@code object} is 
     * {@code null}.
     * 
     * @return {@code object}, if it is not {@code null}.
     * 
     * @throws NullPointerException if {@code object} is {@code null}.
     */
    public static <T> T notNull(T object, Object message) {
        if (object == null) {
            throw new NullPointerException(String.valueOf(message));
        }
        return object;
    }

    /**
     * Ensures that a parameter passed to the calling method is not 
     * {@code null}.
     * 
     * @param object Object to test.
     * @param format Template used to create an exception if {@code object} is 
     * {@code null}. Uses {@link String#format(String, Object...)}.
     * @param values Values to use within {@code format}.
     * 
     * @return {@code object}, if it is not {@code null}.
     * 
     * @throws NullPointerException if {@code object} is {@code null}.
     */
    public static <T> T notNull(T object, String format, Object... values) {
        if (object == null) {
            throw new NullPointerException(String.format(format, values));
        }
        return object;
    }

    public static <T> List<T> noNulls(String message, T... objects) {
        for (T object : objects) {
            if (object == null) {
                throw new NullPointerException(message);
            }
        }
        return CollectionHelpers.list(objects);
    }

    /**
     * Ensures the {@literal "truth"} of an expression involving parameters 
     * passed to the calling method.
     * 
     * @param expression A boolean expression to test.
     * 
     * @throws IllegalArgumentException if {@code expression} is {@code false}.
     */
    public static void checkArg(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Ensures the {@literal "truth"} of an expression involving parameters 
     * passed to the calling method.
     * 
     * @param expression A boolean expression to test.
     * @param message Exception message to use if {@code expression} is 
     * {@code false}.
     * 
     * @throws IllegalArgumentException if {@code expression} is {@code false}.
     */
    public static void checkArg(boolean expression, Object message) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(message));
        }
    }

    /**
     * Ensures the {@literal "truth"} of an expression involving parameters 
     * passed to the calling method.
     * 
     * @param expression A boolean expression to test.
     * @param format Template used to create an exception if {@code expression} is 
     * {@code false}. Uses {@link String#format(String, Object...)}.
     * @param values Values to use within {@code format}.
     * 
     * @throws IllegalArgumentException if {@code expression} is {@code false}.
     */
    public static void checkArg(boolean expression, String format, 
        Object... values) 
    {
        if (!expression) {
            throw new IllegalArgumentException(String.format(format, values));
        }
    }

    public static void instanceOf(Object object, Class<?> clazz) {
        if (!clazz.isInstance(object)) {
            throw new IllegalArgumentException();
        }
    }

    public static void instanceOf(Object message, Object object, Class<?> clazz) {
        if (!clazz.isInstance(object)) {
            throw new IllegalArgumentException(String.valueOf(message));
        }
    }

    public static boolean isInstanceOf(Object object, Class<?> clazz) {
        notNull(object);
        notNull(clazz);
        return clazz.isInstance(object);
    }
}
