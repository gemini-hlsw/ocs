//
// $
//

package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.target.SPTarget;

/**
 * An interface identifying an object that contains {@link SPTarget}s and
 * providing a few conveniences for its clients.
 */
public interface TargetContainer {
    /**
     * @return <code>true</code> if the target container contains the given
     * target
     */
    boolean containsTarget(SPTarget target);

    /**
     * @return all the targets contained in this target container
     */
    ImList<SPTarget> getTargets();

    /**
     * Creates a copy of this target container, but with all included targets
     * cloned.  This is sometimes necessary because {@link SPTarget} is a
     * mutable object, even though the target containers themselves are
     * immutable
     *
     * @return copy of this target container, with all included targets cloned
     */
    TargetContainer cloneTargets();

    /**
     * @return copy of this target container that does not include the given
     * target
     */
    TargetContainer removeTarget(SPTarget target);
}
