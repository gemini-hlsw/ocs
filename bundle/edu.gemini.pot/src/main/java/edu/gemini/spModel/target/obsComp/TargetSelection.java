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

    private static boolean isValid(ISPNode node) {
        return (node instanceof ISPObsComponent) && ((ISPObsComponent) node).getType() == SPComponentType.TELESCOPE_TARGETENV;
    }

    public static int getIndex(ISPNode node) {
        if (!isValid(node)) return NO_SELECTION.value;
        final Object cd = node.getTransientClientData(KEY);
        return ((cd == null) ? NO_SELECTION : (Index) cd).value;
    }

    public static void setIndex(ISPNode node, int index) {
        if (isValid(node)) node.putTransientClientData(KEY, new Index(index));
    }

    private static final class Selection {
        final GuideGroup guideGroup;
        final SPTarget target;

        private Selection(GuideGroup grp, SPTarget target) {
            this.guideGroup = grp;
            this.target     = target;
        }
    }

    private static ImList<Selection> toSelections(TargetEnvironment env) {
        if (env == null) return ImCollections.emptyList();

        final List<Selection> res = new ArrayList<>();
        res.add(new Selection(null, env.getBase()));
        for (GuideGroup g : env.getGroups()) {
            res.add(new Selection(g, null));
            for (SPTarget t : g.getTargets()) res.add(new Selection(g, t));
        }
        for (SPTarget t : env.getUserTargets()) res.add(new Selection(null, t));
        return DefaultImList.create(res);
    }

    private static final MapOp<Tuple2<Selection, Integer>, Integer> INDEX_OF = new MapOp<Tuple2<Selection, Integer>, Integer>() {
        @Override public Integer apply(Tuple2<Selection, Integer> tup) { return tup._2(); }
    };

    public static int indexOf(TargetEnvironment env, final SPTarget target) {
        return toSelections(env).zipWithIndex().find(new PredicateOp<Tuple2<Selection, Integer>>() {
            @Override public Boolean apply(Tuple2<Selection, Integer> tup) {
                return tup._1().target == target;
            }
        }).map(INDEX_OF).getOrElse(NO_SELECTION.value);
    }

    public static int indexOf(TargetEnvironment env, final GuideGroup grp) {
        return toSelections(env).zipWithIndex().find(new PredicateOp<Tuple2<Selection, Integer>>() {
            @Override public Boolean apply(Tuple2<Selection, Integer> tup) {
                return tup._1().guideGroup == grp;
            }
        }).map(INDEX_OF).getOrElse(NO_SELECTION.value);
    }

    private static Selection selectionAt(TargetEnvironment env, int index) {
        if (index < 0) return null;
        final ImList<Selection> lst = toSelections(env);
        return (index < lst.size()) ? lst.get(index) : null;
    }

    public static SPTarget get(TargetEnvironment env, int index) {
        final Selection s = selectionAt(env, index);
        return (s == null) ? null : s.target;
    }

    public static SPTarget get(TargetEnvironment env, ISPNode node) {
        return get(env, getIndex(node));
    }

    public static void set(TargetEnvironment env, ISPNode node, SPTarget target) {
        setIndex(node, indexOf(env, target));
    }

    public static GuideGroup getGuideGroup(TargetEnvironment env, int index) {
        final Selection s = selectionAt(env, index);
        return (s == null) ? null : s.guideGroup;
    }

    public static void setGuideGroup(TargetEnvironment env, ISPNode node, GuideGroup grp) {
        setIndex(node, indexOf(env, grp));
    }

    public static GuideGroup getGuideGroup(TargetEnvironment env, ISPNode node) {
        return getGuideGroup(env, getIndex(node));
    }

    public static void listenTo(ISPNode node, PropertyChangeListener listener) {
        if (isValid(node)) node.addTransientPropertyChangeListener(PROP, listener);
    }

    public static void deafTo(ISPNode node, PropertyChangeListener listener) {
        if (isValid(node)) node.removeTransientPropertyChangeListener(PROP, listener);
    }
}
