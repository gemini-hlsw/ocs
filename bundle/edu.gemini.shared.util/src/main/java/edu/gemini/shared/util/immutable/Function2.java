package edu.gemini.shared.util.immutable;

/**
 * A function wrapper object for a function that accepts two arguments of
 * type T1 and T2 and returns a result of type R.
 */
@FunctionalInterface
public interface Function2<T1, T2, R> {
    R apply(T1 t1, T2 t2);
}
