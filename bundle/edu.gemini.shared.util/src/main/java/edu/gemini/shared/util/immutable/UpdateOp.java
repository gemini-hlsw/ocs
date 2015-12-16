package edu.gemini.shared.util.immutable;

/**
 * A shorthand definition for a map function that takes and returns a value of
 * the same type.
 */
@FunctionalInterface
public interface UpdateOp<T> extends Function1<T, T> {
}
