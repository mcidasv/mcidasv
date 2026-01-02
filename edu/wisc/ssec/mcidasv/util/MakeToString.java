/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
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

import java.util.Arrays;
import java.util.Objects;

/**
 * Utility class for {@code toString()} methods.
 * 
 * <p>Largely taken from Guava's {@code toStringHelper()}, with some 
 * formatting differences as well as the hash code for the given instance.</p>
 */
public class MakeToString {
    
    private final ValueHolder head = new ValueHolder();
    private final String className;
    private final int instanceHashCode;
    
    private boolean omitNullValues = false;
    private ValueHolder tail = head;
    
    private MakeToString(Object instance) {
        Objects.requireNonNull(instance, "Cannot generate toString for a null object");
        className = instance.getClass().getSimpleName();
        instanceHashCode = instance.hashCode();
    }
    
    private MakeToString(String clazzName) {
        className = clazzName;
        instanceHashCode = -1;
    }
    
    public static MakeToString fromInstance(Object instance) {
        return new MakeToString(instance);
    }
    
    public static MakeToString fromClass(Class<?> clazz) {
        return new MakeToString(clazz.getSimpleName());
    }
    
    public MakeToString omitNullValues() {
        omitNullValues = true;
        return this;
    }
    
    public MakeToString addQuoted(String name, Object value) {
        return addHolder(name, '"' + String.valueOf(value) + '"');
    }
    
    public MakeToString add(String name, Object value) {
        return addHolder(name, String.valueOf(value));
    }
    
    public MakeToString add(String name, long value) {
        return addHolder(name, String.valueOf(value));
    }
    
    public MakeToString add(String name, boolean value) {
        return addHolder(name, String.valueOf(value));
    }
    
    public MakeToString add(String name, int value) {
        return addHolder(name, String.valueOf(value));
    }
    
    public MakeToString add(String name, char value) {
        return addHolder(name, String.valueOf(value));
    }
    
    public MakeToString add(String name, float value) {
        return addHolder(name, String.valueOf(value));
    }
    
    public MakeToString add(String name, double value) {
        return addHolder(name, String.valueOf(value));
    }
    
    public MakeToString add(String name, double... arr) {
        StringBuilder buf = new StringBuilder(512);
        buf.append("[ ");
        for (int i = 0; i < arr.length; i++) {
            buf.append(arr[i]);
            if ((i+1) < arr.length) {
                buf.append(',');
            }
        }
        buf.append(" ]");
        return addHolder(name, buf.toString());
    }
    
    private ValueHolder addHolder() {
        ValueHolder valueHolder = new ValueHolder();
        tail = tail.next = valueHolder;
        return valueHolder;
    }
    
    private MakeToString addHolder(String name, Object value) {
        ValueHolder valueHolder = addHolder();
        valueHolder.name = Objects.requireNonNull(name);
        valueHolder.value = value;
        return this;
    }
    
    /**
     * After calling this method, you can keep adding more properties to 
     * later call toString() again and get a more complete representation of 
     * the same object; but properties cannot be removed, so this only allows 
     * limited reuse of the helper instance. The helper allows duplication of 
     * properties (multiple name/value pairs with the same name can be added).
     */
    @Override public String toString() {
        boolean omitNullValuesSnapshot = omitNullValues;
        String hex = Integer.toHexString(instanceHashCode);
        StringBuilder builder = 
            new StringBuilder((className.length() + hex.length()) * 10);
        builder.append('[').append(className);
        if (instanceHashCode != -1) {
            builder.append('@').append(hex);
        }
        builder.append(": ");
        String nextSeparator = "";
        for (ValueHolder valueHolder = head.next;
             valueHolder != null;
             valueHolder = valueHolder.next) 
        {
            Object value = valueHolder.value;
            if (!omitNullValuesSnapshot || (value != null)) {
                builder.append(nextSeparator);
                nextSeparator = ", ";
    
                if (valueHolder.name != null) {
                    builder.append(valueHolder.name).append('=');
                }
                if ((value != null) && value.getClass().isArray()) {
                    Object[] objectArray = { value };
                    String arrayString = Arrays.deepToString(objectArray);
                    builder.append(arrayString, 1, arrayString.length() - 1);
                } else {
                    builder.append(value);
                }
            }
        }
        return builder.append(']').toString();
    }
    
    private static final class ValueHolder {
        String name;
        Object value;
        ValueHolder next;
    }
}
