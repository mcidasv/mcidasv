package edu.wisc.ssec.mcidasv.util.filter;


public class AndFilter<T> extends Filter<T> {
    private Filter<T> left;
    private Filter<T> right;
    public AndFilter(Filter<T> left, Filter<T> right) {
        this.left = left;
        this.right = right;
    }
    public boolean matches(T t) {
        return left.matches(t) && right.matches(t);
    }
}
