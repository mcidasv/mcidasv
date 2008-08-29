package edu.wisc.ssec.mcidasv.util.filter;

import java.util.LinkedHashSet;
import java.util.Set;

public class Test {

    public static <T> Set<T> filter(Filter<T> filter, Set<T> ts) {
        Set<T> filtered = new LinkedHashSet<T>(ts.size());
        for (T t : ts)
            if (filter.matches(t))
                filtered.add(t);
        return filtered;
    }
    public static void main(String[] args) {
        Set<Integer> blah = new LinkedHashSet<Integer>();
        blah.add(1);
        blah.add(2);
        blah.add(3);
        blah.add(4);
        blah.add(5);
        blah.add(6);
        blah.add(7);
        blah.add(8);
        blah.add(9);
        blah.add(10);
        
        Filter<Integer> largeNum = new LargeFilter(5);
        Filter<Integer> even = new EvenFilter();
        Filter<Integer> notTen = new NotTenFilter();
        
/*        Filter<Integer> f = largeNum.and(even).and(notTen);*/
        Filter<Integer> f = largeNum.or(notTen.and(even));
        System.err.println("Before: " + blah);
        System.err.println("Filter: " + filter(f, blah));
        final Filter<Integer> f1 = new Filter<Integer>() {
            public boolean matches(Integer num) {
                return num != 1;
            }
        };
        System.err.println(filter(f1, blah));
    }
    
    public static class LargeFilter extends Filter<Integer> {
        private Integer threshold;
        public LargeFilter(Integer threshold) {
            this.threshold = threshold;
        }
        public boolean matches(Integer num) {
            return num >= threshold;
        }
    }
    
    public static class EvenFilter extends Filter<Integer> {
        public boolean matches(Integer num) {
            return (num % 2) == 0;
        }
    }
    
    public static class NotTenFilter extends Filter<Integer> {
        public boolean matches(Integer num) {
            return num != 10;
        }
    }
}
