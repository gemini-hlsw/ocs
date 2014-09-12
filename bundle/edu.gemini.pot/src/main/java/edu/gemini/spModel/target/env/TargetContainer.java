//
// $
//

package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.Function1;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.PredicateOp;
import edu.gemini.spModel.target.SPTarget;

/**
 * An interface identifying an object that contains {@link SPTarget}s and
 * providing a few conveniences for its clients.
 */
public interface TargetContainer {

    /**
     * A {@link PredicateOp} that can be used with an {@link ImList} of
     * <code>? extends TargetContainer</code> to match on those containing the
     * given {@link SPTarget}. For example, it can be used to find an instance
     * that contains a particular {@link SPTarget}:
     * <pre>
     *     ImList<GuideProbeTargets> lst = ...
     *     Option<GuideProbeTargets> result = lst.find(new TargetMatch(target));
     * </pre>
     */
    final class TargetMatch implements PredicateOp<TargetContainer> {
        private final SPTarget target;
        public TargetMatch(SPTarget target) { this.target = target; }
        @Override public Boolean apply(TargetContainer tc) {
            return tc.containsTarget(target);
        }
    }

    /**
     * A function that can be used to extract the targets contained in a
     * TargetContainer.  For example, to get all the targets in a collection
     * of {@link GuideGroup}:
     * <pre>
     *     ImList<GuideGroup> lst = ...
     *     ImList<SPTarget> alltargets = lst.flatMap(EXTRACT_TARGET);
     * </pre>
     */
    Function1<TargetContainer, ImList<SPTarget>> EXTRACT_TARGET = new Function1<TargetContainer, ImList<SPTarget>>() {
        @Override public ImList<SPTarget> apply(TargetContainer tc) {
            return tc.getTargets();
        }
    };

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
