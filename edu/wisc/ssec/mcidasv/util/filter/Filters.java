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
