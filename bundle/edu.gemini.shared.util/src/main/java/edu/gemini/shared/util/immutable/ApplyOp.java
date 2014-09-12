//
// $
//

package edu.gemini.shared.util.immutable;

/**
 * A generic operation applied to all members of an immutable collection.
 */
public interface ApplyOp<T> {
    void apply(T t);
}
