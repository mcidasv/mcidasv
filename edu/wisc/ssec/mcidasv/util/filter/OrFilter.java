package edu.wisc.ssec.mcidasv.util.filter;


public class OrFilter<T> extends Filter<T> {
    private Filter<T> left;
    private Filter<T> right;
    public OrFilter(Filter<T> left, Filter<T> right) {
        this.left = left;
        this.right = right;
    }
    public boolean matches(T t) {
        return left.matches(t) || right.matches(t);
    }
}
