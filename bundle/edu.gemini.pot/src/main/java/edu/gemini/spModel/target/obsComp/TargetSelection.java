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
    private static final class Selection {
        final int index;
        final Option<GuideGroup> guideGroup;
        final Option<SPTarget> target;

        private Selection(final int index, final Option<GuideGroup> grp, final Option<SPTarget> target) {
            this.index      = index;
            this.guideGroup = grp;
            this.target     = target;
        }

        boolean isGuideGroup() {
            return guideGroup.isDefined() && target.isEmpty();
        }

        boolean isGuideProbeTarget() {
            return guideGroup.isDefined() && target.isDefined();
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

            res.add(new Selection(idx++, None.instance(), new Some<>(env.getBase())));
            for (final GuideGroup g : env.getGroups()) {
                final Option<GuideGroup> gOpt = new Some<>(g);
                res.add(new Selection(idx++, gOpt, None.instance()));
                for (final Option<SPTarget> tOpt : g.getTargets().map(Some::new)) {
                    res.add(new Selection(idx++, gOpt, tOpt));
                }
            }
            for (final Option<SPTarget> tOpt : env.getUserTargets().map(Some::new)) {
                res.add(new Selection(idx++, None.instance(), tOpt));
            }
            return DefaultImList.create(res);
        }
    }

    public static Option<Integer> indexOfTarget(final TargetEnvironment env, final SPTarget target) {
        return Selection.toSelections(env).find(sel -> sel.target.exists(target::equals)).map(sel -> sel.index);
    }

    public static Option<Integer> indexOfIndexedGuideGroup(final TargetEnvironment env,
                                                           final IndexedGuideGroup igg) {
        // Extract the selections (and their indices) corresponding to guide groups.
        final ImList<Selection> filtered = Selection.toSelections(env).filter(Selection::isGuideGroup);

        // Now make sure the guide group at position gpIdx is gp, and if so, return its index in the selections.
        final int gpIdx     = igg.index();
        final GuideGroup gp = igg.group();
        if (gpIdx >= 0 && gpIdx < filtered.size()) {
            final Selection sel = filtered.get(gpIdx);
            return sel.guideGroup.filter(gp::equals).map(ign -> sel.index);
        } else {
            return None.instance();
        }
    }

    private static Option<Selection> selectionAtIndex(final TargetEnvironment env, final int index) {
        if (index < 0) return None.instance();
        final ImList<Selection> lst = Selection.toSelections(env);
        return ImOption.apply(index < lst.size() ? lst.get(index) : null);
    }

    public static Option<SPTarget> getTargetAtIndex(final TargetEnvironment env, final int index) {
        return selectionAtIndex(env, index).flatMap(s -> s.target);
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
        final ImList<Selection> lst = Selection.toSelections(env);
        final int idx = getIndex(node).getOrElse(NO_SELECTION.value);

        // If node is not in the valid range, no guide group.
        // Similarly, if the current node has no guide group set, then no guide group.
        if ((idx < 0) || (idx >= lst.size()))
            return None.instance();
        final Selection sel = lst.get(idx);
        if (sel.guideGroup.isEmpty())
            return None.instance();

        // Filter out the guide groups in positions 0...idx inclusive to get number of guide groups.
        final int gpIdx = lst.zipWithIndex().filter(tup -> tup._1().isGuideGroup() && tup._2() <= idx).size() - 1;
        return sel.guideGroup.map(g -> IndexedGuideGroup.apply(gpIdx,g));
    }

    /**
     * For a given guide group, find the corresponding node in the list if it exists, and set it as the index.
     */
    public static void setIndexedGuideGroup(final TargetEnvironment env, final ISPNode node,
                                            final IndexedGuideGroup igg) {
        setIndex(node, indexOfIndexedGuideGroup(env, igg));
    }

    public static void listenTo(final ISPNode node, final PropertyChangeListener listener) {
        if (isValid(node)) node.addTransientPropertyChangeListener(PROP, listener);
    }

    public static void deafTo(final ISPNode node, final PropertyChangeListener listener) {
        if (isValid(node)) node.removeTransientPropertyChangeListener(PROP, listener);
    }
}
