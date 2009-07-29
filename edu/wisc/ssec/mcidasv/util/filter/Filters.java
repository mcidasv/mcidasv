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

package edu.wisc.ssec.mcidasv.util.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public abstract class Filters {
    private enum State { FINE, FINISHED };

    public static <T> Set<T> filter(Filter<T> filter, Set<T> ts) {
        Set<T> filtered = new LinkedHashSet<T>(ts.size());
        for (T t : ts)
            if (filter.matches(t))
                filtered.add(t);
        return filtered;
    }

    public static <T> List<T> filter(Filter<T> filter, List<T> ts) {
        List<T> filtered = new ArrayList<T>(ts.size());
        for (T t : ts)
            if (filter.matches(t))
                filtered.add(t);
        return filtered;
    }

    public static <T> boolean any(Filter<T> filter, Collection<T> ts) {
        for (T t : ts)
            if (filter.matches(t))
                return true;
        return false;
    }

    public static <T> boolean all(Filter<T> filter, Collection<T> ts) {
        for (T t : ts)
            if (!filter.matches(t))
                return false;
        return true;
    }

    public static <T> Iterator<T> filter(final Filter<? super T> filter, final Iterator<T> ts) {
        // thanks for the inspiration here, Google Collections!
        return new Iterator<T>() {
            private State state = State.FINE;
            private T next;

            private void lookAhead() {
                while (ts.hasNext()) {
                    T t = ts.next();
                    if (filter.matches(t))
                        next = t;
                }
                state = State.FINISHED;
            }

            public boolean hasNext() {
                if (state != State.FINISHED)
                    lookAhead();
                return (state == State.FINE);
            }

            public T next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return next;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @SuppressWarnings("unchecked") // can cast to <T> because non-Ts are removed
    public static <T> Iterator<T> filter(final Class<T> objType, final Iterator<?> ts) {
        return (Iterator<T>)filter(instanceFilter(objType), ts);
    }

    // now you can do something like:
    // List<DataSource> dataSources = filter(instanceFilter(src.getClass()), sources);
    public static Filter<Object> instanceFilter(Class<?> clazz) {
        return new InstanceOfFilter(clazz);
    }

    public static class InstanceOfFilter extends Filter<Object> {
        private final Class<?> clazz;
        public InstanceOfFilter(final Class<?> clazz) {
            this.clazz = clazz;
        }
        public boolean matches(final Object o) {
            return clazz.isInstance(o);
        }
    }
}
