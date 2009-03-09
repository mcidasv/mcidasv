package edu.wisc.ssec.mcidasv.util;

// TODO(jon): think about introducing preconditions and postconditions.
public final class Contract {
    private Contract() { }

    public static <T> T notNull(T object) {
        if (object == null)
            throw new NullPointerException();
        return object;
    }

    public static <T> T notNull(T object, Object message) {
        if (object == null)
            throw new NullPointerException(String.valueOf(message));
        return object;
    }

    public static <T> T notNull(T object, String format, Object... values) {
        if (object == null)
            throw new NullPointerException(String.format(format, values));
        return object;
    }

    public static void checkArg(boolean expression) {
        if (!expression)
            throw new IllegalArgumentException();
    }

    public static void checkArg(boolean expression, Object message) {
        if (!expression)
            throw new IllegalArgumentException(String.valueOf(message));
    }

    public static void checkArg(boolean expression, String format, Object... values) {
        if (!expression)
            throw new IllegalArgumentException(String.format(format, values));
    }
}
