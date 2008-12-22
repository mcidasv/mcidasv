package edu.wisc.ssec.mcidasv.util.functional;

public interface Function<A, B> {

    /**
     * Transforms an {@code A} into a {@code B}.
     * 
     * @param arg The particular {@code A} to be transformed.
     * 
     * @return Result of the transformation.
     */
    public B apply(A arg);
}
