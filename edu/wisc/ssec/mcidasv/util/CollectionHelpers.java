/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


public class CollectionHelpers {
    private CollectionHelpers() {}

    public static <T> T[] arr(T... ts) { 
        return ts;
    }

    public static <E> Set<E> set(E... elements) {
        Set<E> newSet = new LinkedHashSet<E>(elements.length);
        for (E t : elements)
            newSet.add(t);
        return newSet;
    }

    public static <E> Set<E> newHashSet() {
        return new HashSet<E>();
    }

    public static <E> Set<E> newLinkedHashSet() {
        return new LinkedHashSet<E>();
    }

    public static <E> Set<E> newLinkedHashSet(Collection<E> original) {
        return new LinkedHashSet<E>(original);
    }

    public static <K,V> Map<K,V> newMap() {
        return new HashMap<K,V>();
    }

    public static <E> ArrayList<E> arrList() {
        return new ArrayList<E>();
    }

    public static <E> ArrayList<E> arrList(final int capacity) {
        return new ArrayList<E>(capacity);
    }

    public static <E> List<E> list(E... elements) {
        List<E> newList = arrList(elements.length);
        Collections.addAll(newList, elements);
        return newList;
    }

    public static <K, V> Map<K, V> zipMap(K[] keys, V[] values) {
        Map<K, V> zipped = new HashMap<K, V>(keys.length);
        for (int i = 0; i < keys.length; i++)
            zipped.put(keys[i], values[i]);
        return zipped;
    }

    public static <K, V> Map<K, V> zipMap(List<? extends K> keys, List<? extends V> values) {
        Map<K, V> zipped = new HashMap<K, V>(keys.size());
        for (int i = 0; i < keys.size(); i++)
            zipped.put(keys.get(i), values.get(i));
        return zipped;
    }
}
