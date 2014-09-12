//
// $
//

package edu.gemini.shared.util.immutable;

/**
 * An operation used to obtain a value of type U given a value of type T.
 * Can be applied to an {@link ImList} to convert a list of T to a list of U.
 *
 * <p>This is just a renaming of Function1 and may be removed.
 */
public interface MapOp<T, U> extends Function1<T, U> {
//    U apply(T t);
}
