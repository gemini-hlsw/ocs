package edu.gemini.shared.util.immutable;

/**
 * A generic operation applied to all members of an immutable collection.
 */
@FunctionalInterface
public interface ApplyOp<T> {
    void apply(T t);
}
