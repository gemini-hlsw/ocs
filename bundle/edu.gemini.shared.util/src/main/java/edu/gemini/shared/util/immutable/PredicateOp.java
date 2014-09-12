//
// $
//

package edu.gemini.shared.util.immutable;

/**
 * An operation that returns <code>true</code> or <code>false</code> depending
 * upon its argument.  The argument is said to "satisfy" the predicate if it
 * returns <code>true</code>.
 *
 * <p>PredicateOp simply pins the return type of a {@link Function1} to Boolean.
 */
public interface PredicateOp<T> extends Function1<T, Boolean> {
}
