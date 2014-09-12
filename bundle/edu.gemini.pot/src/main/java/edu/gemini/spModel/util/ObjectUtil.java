package edu.gemini.spModel.util;

public final class ObjectUtil {
    /**
     * Returns true if the two object are reference equality equals or
     * semantic equality equals and handles the case where either or both is
     * <code>null</code>.
     */
    public static boolean equals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

}
