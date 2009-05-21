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
