package edu.wisc.ssec.mcidasv.util.filter;


public abstract class Filter<T> {
    public Filter<T> and(Filter<T> other) {
        return new AndFilter<T>(this, other);
    }

    public Filter<T> or(Filter<T> other) {
        return new OrFilter<T>(this, other);
    }

    public abstract boolean matches(T t);
}
