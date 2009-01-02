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

package edu.wisc.ssec.mcidasv.util.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public abstract class Filters {
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
    // TODO(jon): implement map
}
