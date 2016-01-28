package edu.gemini.spModel.target.obsComp;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.SPUtil;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
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

    private static final Index NO_SELECTION = new Index(-1);

    private static boolean isValid(final ISPNode node) {
        return (node instanceof ISPObsComponent) && ((ISPObsComponent) node).getType() == SPComponentType.TELESCOPE_TARGETENV;
    }

    public static int getIndex(final ISPNode node) {
        if (!isValid(node)) return NO_SELECTION.value;
        final Object cd = node.getTransientClientData(KEY);
        return ((cd == null) ? NO_SELECTION : (Index) cd).value;
    }

    public static void setIndex(final ISPNode node, final int index) {
        if (isValid(node)) node.putTransientClientData(KEY, new Index(index));
    }

    private static final class Selection {
        final GuideGroup guideGroup;
        final SPTarget target;

        private Selection(final GuideGroup grp, final SPTarget target) {
            this.guideGroup = grp;
            this.target     = target;
        }
    }

    private static ImList<Selection> toSelections(final TargetEnvironment env) {
        if (env == null) return ImCollections.emptyList();

        final List<Selection> res = new ArrayList<>();
        res.add(new Selection(null, env.getBase()));
        env.getGroups().foreach(g -> {
            res.add(new Selection(g, null));
            g.getTargets().foreach(t -> res.add(new Selection(g, t)));
        });
        env.getUserTargets().foreach(t -> res.add(new Selection(null, t)));
        return DefaultImList.create(res);
    }

    private static final MapOp<Tuple2<Selection, Integer>, Integer> INDEX_OF = Tuple2::_2;

    public static int indexOf(final TargetEnvironment env, final SPTarget target) {
        return toSelections(env).zipWithIndex().
                find(tup -> target.equals(tup._1().target)).
                map(INDEX_OF).getOrElse(NO_SELECTION.value);
    }

    public static int indexOf(final TargetEnvironment env, final GuideGroup grp) {
        return toSelections(env).zipWithIndex().
                find(tup -> grp.equals(tup._1().guideGroup)).
                map(INDEX_OF).getOrElse(NO_SELECTION.value);
    }

    private static Selection selectionAt(final TargetEnvironment env, final int index) {
        if (index < 0) return null;
        final ImList<Selection> lst = toSelections(env);
        return (index < lst.size()) ? lst.get(index) : null;
    }

    public static SPTarget get(final TargetEnvironment env, final int index) {
        final Selection s = selectionAt(env, index);
        return (s == null) ? null : s.target;
    }

    public static SPTarget get(final TargetEnvironment env, final ISPNode node) {
        return get(env, getIndex(node));
    }

    public static void set(final TargetEnvironment env, final ISPNode node, final SPTarget target) {
        setIndex(node, indexOf(env, target));
    }

    public static GuideGroup getGuideGroup(final TargetEnvironment env, final int index) {
        final Selection s = selectionAt(env, index);
        return (s == null) ? null : s.guideGroup;
    }

    public static void setGuideGroup(final TargetEnvironment env, final ISPNode node, final GuideGroup grp) {
        setIndex(node, indexOf(env, grp));
    }

    public static GuideGroup getGuideGroup(final TargetEnvironment env, final ISPNode node) {
        return getGuideGroup(env, getIndex(node));
    }

    public static Option<Tuple2<Integer, GuideGroup>> getIndexedGuideGroup(final TargetEnvironment env, final ISPNode node) {
        final ImList<Tuple2<Selection,Integer>> lst = toSelections(env).zipWithIndex();
        final Integer idx = getIndex(node);
        if ((idx < 0) || (idx >= lst.size())) {
            return ImOption.<Tuple2<Integer, GuideGroup>>empty();
        } else {
            final Tuple2<Selection, Integer> sel = lst.get(idx);
            if (sel._1().guideGroup == null) {
                return ImOption.<Tuple2<Integer, GuideGroup>>empty();
            } else {
                // Figure out the group index by counting groups until sel. Ugh.
                int groupIndex = 0;
                for (int i=0; i<sel._2(); ++i) {
                    final Tuple2<Selection, Integer> cur = lst.get(i);
                    groupIndex += (cur._1().guideGroup == null) ? 0 : 1;
                }
                return new Some<>(new Pair<>(groupIndex, sel._1().guideGroup));
            }
        }
    }

    public static void listenTo(final ISPNode node, final PropertyChangeListener listener) {
        if (isValid(node)) node.addTransientPropertyChangeListener(PROP, listener);
    }

    public static void deafTo(final ISPNode node, final PropertyChangeListener listener) {
        if (isValid(node)) node.removeTransientPropertyChangeListener(PROP, listener);
    }
}
