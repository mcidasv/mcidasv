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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.wisc.ssec.mcidasv.util.functional.Function;

/**
 * A <i>collection</i> (ugh) of static methods that make working with Java's 
 * default collections a bit easier, or at least allows you to elide some of
 * the redundancy of idiomatic Java.
 * 
 * <p>Make use of {@literal "static imports"} to omit even more needless code.
 */
public final class CollectionHelpers {

    /** Never! */
    private CollectionHelpers() {}

    /**
     * {@literal "Converts"} the incoming {@literal "varargs"} into an array.
     * 
     * <p>Useful for doing things like:
     * {@code String[] strs = arr("hello", "how", "are", "you?");}
     * 
     * @param ts Items that will make up the elements of the returned array.
     * Cannot be {@code null}, and (for now) the items should be of the 
     * <i>same</i> type.
     * 
     * @return An array populated with each item from {@code ts}.
     */
    public static <T> T[] arr(T... ts) { 
        return ts;
    }

    /**
     * Creates a {@link List} from incoming {@literal "varargs"}. Currently 
     * uses {@link ArrayList} as the {@code List} implementation.
     * 
     * <p>Used like so:
     * {@code List<String> listy = list("y", "helo", "thar");}
     * 
     * @param elements Items that will make up the elements of the returned
     * {@code List}.
     * 
     * @return A {@code List} whose elements are each item within {@code elements}.
     */
    public static <E> List<E> list(E... elements) {
        List<E> newList = arrList(elements.length);
        Collections.addAll(newList, elements);
        return newList;
    }

    /**
     * Creates a {@link Set} from incoming {@literal "varargs"}. Currently uses
     * {@link LinkedHashSet} as the {@code Set} implementation (to preserve 
     * ordering).
     * 
     * <p>Used like so:
     * <pre>for (String s :{@code set("beep", "boop", "blorp")}) { ... }</pre>
     * 
     * @param elements Items that will appear within the returned {@code Set}.
     * Cannot be {@code null}, and (for now) the items should be of the 
     * <i>same</i> type.
     * 
     * @return A {@code Set} containing the items in {@code elements}. Remember
     * that {@code Set}s only contain <i>unique</i> elements!
     */
    public static <E> Set<E> set(E... elements) {
        Set<E> newSet = new LinkedHashSet<E>(elements.length);
        for (E t : elements)
            newSet.add(t);
        return newSet;
    }

    /**
     * Creates a new {@code cl} instance (limited to things implementing 
     * {@link Collection}) populated with the {@literal "varargs"}. Useful if
     * you truly despise {@link #arr(Object...)}, {@link #list(Object...)}, 
     * or {@link #set(Object...)}.
     * 
     * <p>Example: {@code Collection<Integer> ints = collect(PriorityBlockingQueue.class, 1, 2, 3);}
     * 
     * @param cl A (non-abstract!) class that implements {@code Collection}. Cannot be {@code null}.
     * @param elements Objects that will be added to the collection.
     * 
     * @return An instance of {@code cl} containing the given objects.
     * 
     * @throws RuntimeException if {@link Constructor#newInstance(Object)} had problems.
     * 
     * @see #arr(Object...)
     * @see #list(Object...)
     * @see #set(Object...)
     */
    @SuppressWarnings("unchecked") // the only things being added to the collection are objects of type "E"
    public static <E> Collection<E> collect(Class<? extends Collection> cl, E... elements) {
        try {
            Collection<E> c = cl.getConstructor().newInstance();
            Collections.addAll(c, elements);
            return c;
        } catch (Exception e) {
            throw new RuntimeException("Problem creating a new "+cl, e);
        }
    }

    /**
     * Determines the {@literal "length"} of a given object. This method 
     * currently understands:<ul>
     * <li>{@link Collection}</li>
     * <li>{@link Map}</li> 
     * <li>{@link CharSequence}</li>
     * <li>{@link Array}</li>
     * <li>{@link Iterable}</li>
     * <li>{@link Iterator}</li>
     * </ul>
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

        if (o instanceof Collection<?>) {
            return ((Collection<?>)o).size();
        }
        else if (o instanceof Map<?, ?>) {
            return ((Map<?, ?>)o).size();
        }
        else if (o instanceof CharSequence) {
            return ((CharSequence)o).length();
        } 
        else if (o instanceof Iterator<?>) {
            int count = 0;
            Iterator<?> it = (Iterator<?>)o;
            while (it.hasNext()) {
                it.next();
                count++;
            }
            return count;
        }
        else if (o instanceof Iterable<?>) {
            return len(((Iterable<?>)o).iterator());
        }

        throw new IllegalArgumentException("Don't know how to find the length of a "+o.getClass().getName());
    }

    /**
     * Searches an object to see if it {@literal "contains"} another object.
     * This method currently knows how to search:<ul>
     * <li>{@link Collection}</li>
     * <li>{@link Map}</li> 
     * <li>{@link CharSequence}</li>
     * <li>{@link Array}</li>
     * <li>{@link Iterable}</li>
     * <li>{@link Iterator}</li>
     * </ul>
     * 
     * <p>More coming!
     * 
     * @param collection Object that'll be searched for {@code item}. Cannot be 
     * {@code null}.
     * @param item Object to search for within {@code o}. {@code null} values
     * are allowed. 
     * 
     * @return {@code true} if {@code o} contains {@code item}, {@code false}
     * otherwise.
     * 
     * @throws NullPointerException if {@code o} is {@code null}.
     * @throws IllegalArgumentException if the method doesn't know how to 
     * search whatever type of object {@code o} might be.
     */
    // TODO(jon:89): item should probably become an array/collection too...
    public static boolean contains(final Object collection, final Object item) {
        if (collection == null)
            throw new NullPointerException("Cannot search a null object");

        if (collection instanceof Collection<?>) {
            return ((Collection<?>)collection).contains(item);
        }
        else if ((collection instanceof String) && (item instanceof CharSequence)) {
            return ((String)collection).contains((CharSequence)item);
        }
        else if (collection instanceof Map<?, ?>) {
            return ((Map<?, ?>)collection).containsKey(item);
        }
        else if (collection instanceof Iterator<?>) {
            Iterator<?> it = (Iterator<?>)collection;
            if (item == null) {
                while (it.hasNext())
                    if (it.next() == null)
                        return true;
            } else {
                while (it.hasNext())
                    if (item.equals(it.next()))
                        return true;
            }
            return false;
        }
        else if (collection instanceof Iterable<?>) {
            return contains(((Iterable<?>)collection).iterator(), item);
        }
        else if (collection.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(collection); i++) {
                Object value = Array.get(collection, i);
                if (value.equals(item))
                    return true;
            }
        }
        throw new IllegalArgumentException("Don't know how to search a "+collection.getClass().getName());
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
     * @return A new, empty {@code HashMap}.
     */
    public static <K, V> Map<K, V> newMap() {
        return new HashMap<K, V>();
    }

    /**
     * Abuses Java's sad {@literal "type"} implementation to create a new
     * {@link ConcurrentHashMap}.
     * 
     * @return Shiny and new {@code ConcurrentHashMap}
     */
    public static <K, V> Map<K, V> concurrentMap() {
        return new ConcurrentHashMap<K,V>();
    }

    /**
     * Creates an empty {@link ArrayList} that uses a little cleverness with
     * Java's generics. Useful for eliminating redundant type information and
     * declaring fields as {@code final}.
     * 
     * <p>Used like so:
     * {@code List<String> listy = arrList();}
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
     * Copies an existing {@link Collection} into a new {@link ArrayList}.
     * 
     * @param c {@code Collection} whose elements are to be placed into the 
     * returned {@code ArrayList}.
     * 
     * @return An {@code ArrayList} containing the elements of {@code c}.
     * 
     * @see ArrayList#ArrayList(Collection)
     */
    public static <E> ArrayList<E> arrList(final Collection<E> c) {
        return new ArrayList<E>(c);
    }

    /**
     * Copies an existing {@link Collection} into a new (non-abstract!) 
     * {@code Collection} class.
     * 
     * @param cl Non-abstract {@code Collection} class.
     * @param old An existing {@code Collection}.
     * 
     * @return A new instance of {@code cl} that contains all of the elements
     * from {@code old}.
     * 
     * @throws RuntimeException if there was trouble creating a new instance 
     * of {@code cl}.
     * 
     * @see #collect(Class, Object...)
     */
    // not sure about the utility of this one...
    @SuppressWarnings("unchecked") // again, only adding items of type "E"
    public static <E> Collection<E> recollect(Class<? extends Collection> cl, Collection<E> old) {
        try {
            Collection<E> c = cl.getConstructor().newInstance();
            c.addAll(old);
            return c;
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    /**
     * Takes arrays of {@code keys} and {@code values} and merges them 
     * together to form a {@link Map}. The returned {@code Map} is a 
     * {@link LinkedHashMap} and is truncated in length to the length of the 
     * shorter parameter.
     * 
     * <p>This is intended for use as {@literal "varargs"} supplied to 
     * {@link #arr(Object...)}. Rather than doing something ugly like:
     * <pre>
     * Map&lt;String, String&gt; mappy = new LinkedHashMap&lt;String, String&gt;();
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
     * <p>The latter approach also allows you to make {@code static final} 
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
        Map<K, V> zipped = new LinkedHashMap<K, V>(keys.length);
        for (int i = 0; (i < keys.length && i < values.length); i++)
            zipped.put(keys[i], values[i]);
        return zipped;
    }

    /**
     * A version of {@link #zipMap(Object[], Object[])} that works with 
     * {@link Collection}s.
     * 
     * @param keys Items that will be the keys in the resulting {@code Map}.
     * @param values Items that will be the values in the result {@code Map}.
     * 
     * @return A {@code Map} whose entries are of the form 
     * {@code keys[N], values[N]}.
     * 
     * @see #zipMap(Object[], Object[])
     */
    public static <K, V> Map<K, V> zipMap(Collection<? extends K> keys, Collection<? extends V> values) {
        Map<K, V> zipped = new LinkedHashMap<K, V>(keys.size());
        Iterator<? extends K> keyIterator = keys.iterator();
        Iterator<? extends V> valueIterator = values.iterator();
        while (keyIterator.hasNext() && valueIterator.hasNext())
            zipped.put(keyIterator.next(), valueIterator.next());
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

    // simple tests go here. though we really must implement some form of automated testing.
    // public static void main(String[] args) {
    // }
}
