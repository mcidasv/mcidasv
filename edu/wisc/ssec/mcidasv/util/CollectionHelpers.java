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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.wisc.ssec.mcidasv.util.functional.Function;


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

    /**
     * Determines the {@literal "length"} of a given object. This method 
     * currently understands {@link Collection}s, {@link Map}s, 
     * {@link CharSequence}s, and {@link Array}s.
     * 
     * <p>More coming!
     * 
     * @param o Object whose length we want. Cannot be {@code null}.
     * 
     * @return {@literal "Length"} of {@code o}.
     * 
     * @throws NullPointerException if {@code o} is {@code null}.
     * @throws IllegalArgumentException if the method doesn't know how to test
     * whatever type of object {@code o} might be.
     */
    public static int len(final Object o) {
        if (o == null)
            throw new NullPointerException("Null arguments do not have a length");
        if (o instanceof Collection) {
            return ((Collection)o).size();
        }
        else if (o instanceof Map) {
            return ((Map)o).size();
        }
        else if (o instanceof CharSequence) {
            return ((CharSequence)o).length();
        }
        throw new IllegalArgumentException("Don't know how to find the length of a "+o.getClass().getName());
    }

    /**
     * Searches an object to see if it {@literal "contains"} another object.
     * This method currently knows how to search {@link String}s, 
     * {@link Collection}s, {@link Map}s, and {@link Array}s.
     * 
     * <p>More coming!
     * 
     * @param o Object that'll be searched for {@code item}. Cannot be 
     * {@code null}.
     * @param item Object to search for within {@code o}. {@code null} values
     * are allowed. 
     * 
     * @return {@code true} if {@code o} contains {@code item}, {@code false}
     * otherwise.
     * 
     * @throws NullPointerException if {@code o} is {@code null}.
     */
    // TODO(jon:89): item should probably become an array/collection too...
    public static boolean contains(final Object o, final Object item) {
        if (o == null)
            throw new NullPointerException("Cannot search a null object");
        if (o instanceof Collection) {
            return ((Collection)o).contains(item);
        }
        else if ((o instanceof String) && (item instanceof CharSequence)) {
            return ((String)o).contains((CharSequence)item);
        }
        else if (o instanceof Map) {
            return ((Map)o).containsKey(item);
        }
        else if (o.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(o); i++) {
                Object value = Array.get(o, i);
                if (value.equals(item))
                    return true;
            }
        }
        return false;
    }

    /**
     * Creates an empty {@link HashSet} that uses a little cleverness with 
     * Java's generics. Useful for eliminating redundant type information and
     * declaring fields as {@code final}.
     * 
     * @return A new, empty {@code HashSet}.
     */
    public static <E> Set<E> newHashSet() {
        return new HashSet<E>();
    }

    /**
     * Creates an empty {@link LinkedHashSet} that uses a little cleverness 
     * with Java's generics. Useful for eliminating redundant type 
     * information and declaring fields as {@code final}.
     * 
     * @return A new, empty {@code HashSet}.
     */
    public static <E> Set<E> newLinkedHashSet() {
        return new LinkedHashSet<E>();
    }

    /**
     * Copies a {@link Collection} into a new {@link LinkedHashSet}.
     * 
     * @param original Collection to be copied.
     * 
     * @return A new {@code LinkedHashSet} whose contents are the same as 
     * {@code original}.
     */
    public static <E> Set<E> newLinkedHashSet(Collection<E> original) {
        return new LinkedHashSet<E>(original);
    }

    /**
     * Creates an empty {@link HashSet} that uses a little cleverness with 
     * Java's generics. Useful for eliminating redundant type information and
     * declaring fields as {@code final}.
     * 
     * @return A new, empty {@code HashSet}.
     */
    public static <K,V> Map<K,V> newMap() {
        return new HashMap<K,V>();
    }

    /**
     * Creates an empty {@link ArrayList} that uses a little cleverness with
     * Java's generics. Useful for eliminating redundant type information and
     * declaring fields as {@code final}.
     * 
     * <p>Used like so:
     * <pre>List&lt;String&gt; listy = arrList();</pre>
     * 
     * @return A new, empty {@code ArrayList}.
     */
    public static <E> ArrayList<E> arrList() {
        return new ArrayList<E>();
    }

    /**
     * Creates an empty {@link ArrayList} with a given capacity.
     * 
     * @param capacity The initial size of the returned {@code ArrayList}.
     * 
     * @return A new, empty {@code ArrayList} that has an initial capacity of
     * {@code capacity} elements.
     * 
     * @see ArrayList#ArrayList(int)
     */
    public static <E> ArrayList<E> arrList(final int capacity) {
        return new ArrayList<E>(capacity);
    }

    /**
     * Creates a {@link List} from incoming {@literal "varargs"}. Currently 
     * uses {@link ArrayList} as the {@code List} implementation.
     * 
     * <p>Used like so:
     * <pre>List&lt;String&gt; listy = list("y", "helo", "thar");</pre>
     * 
     * @param elements Items that will make up the elements of the returned
     * {@code List}.
     * 
     * @return A {@code List} whose elements are each of {@code elements}.
     */
    public static <E> List<E> list(E... elements) {
        List<E> newList = arrList(elements.length);
        Collections.addAll(newList, elements);
        return newList;
    }

    /**
     * Takes arrays of {@code keys} and {@code values} and merges them 
     * together to form a {@link HashMap}.
     * 
     * <p>This is intended for use as {@literal "varargs"} supplied to 
     * {@link #arr(Object...)}. Rather than doing something ugly like:
     * <pre>
     * Map&lt;String, String&gt; mappy = new HashMap&lt;String, String&gt;();
     * mappy.put("key0", "val0");
     * mappy.put("key1", "val1");
     * ...
     * mappy.put("keyN", "valN");
     * </pre>
     * 
     * Simply do like so:
     * <pre>
     * mappy = zipMap(
     *     arr("key0", "key1", ..., "keyN"),
     *     arr("val0", "val1", ..., "valN"));
     * </pre>
     * 
     * <p>The latter approach also allows you to make {@code final static} 
     * {@link Map}s much more easily.
     * 
     * @param keys Array whose elements will be the keys in a {@code Map}.
     * @param values Array whose elements will the values in a {@code Map}.
     * 
     * @return A {@code Map} whose entries are of the form 
     * {@code keys[N], values[N]}.
     * 
     * @see #arr(Object...)
     * @see #zipMap(List, List)
     */
    public static <K, V> Map<K, V> zipMap(K[] keys, V[] values) {
        // TODO(jon:#84): what if you have more keys than values?
        // TODO(jon:#86): should this method return a LinkedHashMap rather than a HashMap?
        Map<K, V> zipped = new HashMap<K, V>(keys.length);
        for (int i = 0; i < keys.length; i++)
            zipped.put(keys[i], values[i]);
        return zipped;
    }

    /**
     * A version of {@link #zipMap(Object[], Object[])} that works with
     * 
     * @param keys Items that will be the keys in the resulting {@code Map}.
     * @param values Items that will be the values in the result {@code Map}.
     * 
     * @return A {@code Map} whose entries are of the form 
     * {@code keys[N], values[N]}.
     * 
     * @see #zipMap(Object[], Object[])
     */
    public static <K, V> Map<K, V> zipMap(List<? extends K> keys, List<? extends V> values) {
        // TODO(jon:#84): should this method accept collections rather than a list? what about a set version?
        // TODO(jon:#85): what if you have more keys than values?
        // TODO(jon:#86): should this method return a LinkedHashMap rather than a HashMap?
        Map<K, V> zipped = new HashMap<K, V>(keys.size());
        for (int i = 0; i < keys.size(); i++)
            zipped.put(keys.get(i), values.get(i));
        return zipped;
    }

    /**
     * Applies a given function to each item in a given list.
     * 
     * @param f The {@link Function} to apply.
     * @param as The list whose items are to be fed into {@code f}.
     * 
     * @return New list containing the results of each element of {@code as}
     * being passed through {@code f}.
     */
    public static <A, B> List<B> map(final Function<A, B> f, List<A> as) {
        List<B> bs = arrList();
        for (A a : as)
            bs.add(f.apply(a));
        return bs;
    }

    /**
     * Applies a given function to each item in a given {@link Set}.
     * 
     * @param f The {@link Function} to apply to {@code as}.
     * @param as The {@code Set} whose items are to be fed into {@code f}.
     * 
     * @return New {@code Set} containing the results of passing each element
     * in {@code as} through {@code f}.
     */
    public static <A, B> Set<B> map(final Function<A, B> f, Set<A> as) {
        Set<B> bs = newLinkedHashSet();
        for (A a : as)
            bs.add(f.apply(a));
        return bs;
    }
}
