package edu.gemini.spModel.target.obsComp;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.SPUtil;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.IndexedGuideGroup;
import edu.gemini.spModel.target.env.TargetEnvironment;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages target and guide group selection transient client data for clients.
 */
public final class TargetSelection {
    private TargetSelection() {}

    public static final Logger LOG = Logger.getLogger(TargetSelection.class.getName());

    public static final String KEY  = TargetSelection.class.getName();
    public static final String PROP = SPUtil.getTransientClientDataPropertyName(KEY);

    // An int wrapper which doesn't define equals/hashCode so that each instance
    // is distinct. This is done so that storing a new SelectionIndex with the
    // same int value will still cause the MemAbstractBase PropertyChangeSupport
    // to trigger a property change event.
    private static final class Index {
        final int value;
        Index(int value) { this.value = value; }
    }

    // Used internally as a placeholder for None.
    private static final Index NO_SELECTION = new Index(-1);

    private static boolean isValid(final ISPNode node) {
        return (node instanceof ISPObsComponent) &&
                ((ISPObsComponent) node).getType() == SPComponentType.TELESCOPE_TARGETENV;
    }

    public static Option<Integer> getIndex(final ISPNode node) {
        if (!isValid(node)) return None.instance();
        return ImOption.apply((Index) node.getTransientClientData(KEY))
                .map(i -> i.value).filter(i -> i != NO_SELECTION.value);
    }

    private static void setIndex(final ISPNode node, final int index) {
        setIndex(node, index == -1 ? None.instance() : new Some<>(index));
    }

    public static void setIndex(final ISPNode node, final Option<Integer> index) {
        if (isValid(node)) node.putTransientClientData(KEY, index.map(Index::new).getOrElse(NO_SELECTION));
    }

    /**
     * Marks a selection object.
     */
    private static abstract class Selection {
        private final int index;

        protected Selection(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public Option<SPTarget> getTarget() {
            return None.instance();
        }

        public Option<GuideGroup> getGuideGroup() {
            return None.instance();
        }

        /**
         * Create a list of Selection from the target environment, consisting of:
         * 1. The base;
         * 2. A list of GuideGroups and their targets; and
         * 3. A list of the user targets.
         */
        static ImList<Selection> toSelections(final TargetEnvironment env) {
            if (env == null) return ImCollections.emptyList();

            int idx = 0;
            final List<Selection> res = new ArrayList<>();

            // TODO:ASTERISM: How do we handle multiples? For now just select the a random target.
            res.add(new NormalTargetSelection(idx++, env.getArbitraryTargetFromAsterism()));
            for (final GuideGroup g : env.getGroups()) {
                res.add(new GuideGroupSelection(idx++, g));
                for (final SPTarget t : g.getTargets()) {
                    res.add(new GuideStarSelection(idx++, g, t));
                }
            }
            for (final SPTarget t : env.getUserTargets()) {
                res.add(new NormalTargetSelection(idx++, t));
            }
            return DefaultImList.create(res);
        }
    }

    private static final class GuideGroupSelection extends Selection {
        final GuideGroup guideGroup;

        GuideGroupSelection(int index, final GuideGroup guideGroup) {
            super(index);
            this.guideGroup = guideGroup;
        }

        @Override public Option<GuideGroup> getGuideGroup() {
            return new Some<>(guideGroup);
        }

        /**
         * Create a list of GuideGroupSelection from the target environment.
         * This is the same as if the guide groups were filtered out of the Selection.toSelection method,
         * but avoids creating the unnecessary objects.
         */
        static ImList<GuideGroupSelection> toGuideGroupSelections(final TargetEnvironment env) {
            if (env == null) return ImCollections.emptyList();

            // Start at 1 to omit base positon.
            int idx = 1;
            final List<GuideGroupSelection> res = new ArrayList<>();
            for (final GuideGroup g : env.getGroups()) {
                res.add(new GuideGroupSelection(idx++, g));

                // Skip idx over the targets.
                idx += g.getTargets().size();
            }
            return DefaultImList.create(res);
        }
    }

    private static final class GuideStarSelection extends Selection {
        final GuideGroup guideGroup;
        final SPTarget target;

        GuideStarSelection(int index, final GuideGroup guideGroup, final SPTarget target) {
            super(index);
            this.guideGroup = guideGroup;
            this.target     = target;
        }

        @Override public Option<GuideGroup> getGuideGroup() {
            return new Some<>(guideGroup);
        }

        @Override public Option<SPTarget> getTarget() {
            return new Some<>(target);
        }
    }

    private static final class NormalTargetSelection extends Selection {
        final SPTarget target;

        NormalTargetSelection(int index, final SPTarget target) {
            super(index);
            this.target = target;
        }

        @Override public Option<SPTarget> getTarget() {
            return new Some<>(target);
        }
    }

    public static Option<Integer> indexOfTarget(final TargetEnvironment env, final SPTarget target) {
        return Selection.toSelections(env).find(sel -> sel.getTarget().exists(target::equals)).map(sel -> sel.index);
    }

    /**
     * Given an index of a guide group in the list of all guide groups, find the corresponding node index.
     */
    public static Option<Integer> indexOfGuideGroupByIndex(final TargetEnvironment env,
                                                           final int guideGroupIndex) {
        final ImList<GuideGroupSelection> selections = GuideGroupSelection.toGuideGroupSelections(env);
        return selections.getOption(guideGroupIndex).map(GuideGroupSelection::getIndex);
    }

    private static Option<Selection> selectionAtIndex(final TargetEnvironment env, final int index) {
        return Selection.toSelections(env).getOption(index);
    }

    public static Option<SPTarget> getTargetAtIndex(final TargetEnvironment env, final int index) {
        return selectionAtIndex(env, index).flatMap(Selection::getTarget);
    }

    public static Option<SPTarget> getTargetForNode(final TargetEnvironment env, final ISPNode node) {
        return getIndex(node).flatMap(i -> getTargetAtIndex(env, i));
    }

    public static void setTargetForNode(final TargetEnvironment env, final ISPNode node, final SPTarget target) {
        indexOfTarget(env, target).foreach(i -> setIndex(node, i));
    }

    /**
     * For a given node in the list, find its guide group if any, and the index of said guide group amongst all guide
     * groups.
     */
    public static Option<IndexedGuideGroup> getIndexedGuideGroupForNode(final TargetEnvironment env,
                                                                        final ISPNode node) {
        // The list of all nodes in the tree, and the index of the currently selected node.
        final ImList<GuideGroupSelection> selections = GuideGroupSelection.toGuideGroupSelections(env);
        final int idx = getIndex(node).getOrElse(NO_SELECTION.value);

        // Look for the entry numbered idx in the selections, and if it exists, return it as an IndexedGuideGroup
        // with its index amongst all guide groups.
        return selections.zipWithIndex().find(tup -> tup._1().getIndex() == idx)
                .map(tup -> new IndexedGuideGroup(tup._2(), tup._1().guideGroup));
    }

    /**
     * For a given guide group, find the corresponding node in the list if it exists, and set it as the index.
     */
    public static void setGuideGroupByIndex(final TargetEnvironment env, final ISPNode node, final int index) {
        setIndex(node, indexOfGuideGroupByIndex(env, index));
    }

    public static void listenTo(final ISPNode node, final PropertyChangeListener listener) {
        if (isValid(node)) node.addTransientPropertyChangeListener(PROP, listener);
    }

    public static void deafTo(final ISPNode node, final PropertyChangeListener listener) {
        if (isValid(node)) node.removeTransientPropertyChangeListener(PROP, listener);
    }
}
