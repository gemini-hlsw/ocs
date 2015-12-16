package edu.gemini.shared.util.immutable;

/**
 * A function wrapper object for a function that accepts a single argument of
 * type T and returns a result of type R.
 */
@FunctionalInterface
public interface Function1<T, R> {
    R apply(T t);
}
