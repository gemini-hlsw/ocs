package edu.gemini.spModel.target.offset;

import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeMap;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A data object that describes a list of telescope offset positions.
 * The offset position list data is mapped into a set of attributes,
 * one for each Offset and one for the ordered list of tags of
 * the positions.
 */
public final class OffsetPosList<P extends OffsetPosBase> implements Cloneable, Serializable, Iterable<P> {
    private static final Logger LOG = Logger.getLogger(OffsetPosList.class.getName());

    // for serialization
    private static final long serialVersionUID = 2L;

    // Flag indicating add at end or unknown index
    public static final int UNKNOWN_INDEX = -1;

    /** The name of the attribute that holds the target tags. */
    public static final String ADVANCED_GUIDING_PROP = "advancedGuiding";

    // Position List content/ordering watchers
    private transient List<OffsetPosListWatcher<P>> _watchers;

    /** The implementation of the list. */
    private List<P> _posList = new ArrayList<>();

    private final OffsetPosBase.Factory<P> _factory;

    // Guiders that need to override the default guide setting at one or more
    // offset positions.  Usually this will be an empty set so we'll maintain
    // a reference to the Collections.emptySet() instead of making a new
    // modifiable empty set for each pos list.  When a new guider is added,
    // we'll create a new unmodifiable Set to hold it and any other guiders
    // found in advancedGuiding.
    private Set<GuideProbe> advancedGuiding = Collections.emptySet();

    /**
     * Create an empty offset list.
     */
    public OffsetPosList(OffsetPosBase.Factory<P> f) {
        _factory = f;
    }

    /**
     * Override clone to make sure the position list is correctly
     * initialized.
     */
    public Object clone() {
        final OffsetPosList<P> ol;
        try {
            //noinspection unchecked
            ol = (OffsetPosList<P>) super.clone();
            ol._watchers    = null;
            ol._posList     = new ArrayList<>();
        } catch (CloneNotSupportedException ex) {
            // Should not happen
            throw new UnsupportedOperationException();
        }

        for (int i = 0; i < size(); i++) {
            P p = getPositionAt(i);
            //noinspection unchecked
            P np = (P) p.clone();
            ol.addPosition(np);
        }

        return ol;
    }

    /**
     * Return an iterator for the position list
     */
    public synchronized Iterator<P> iterator() {
        return _posList.iterator();
    }

    /**
     * Get the number of positions that are in the list.
     */
    public int size() {
        return _posList.size();
    }

    /**
     * Return an array containing all the positions.
     */
    public synchronized List<P> getAllPositions() {
        if (_posList == null) return Collections.emptyList();
        return new ArrayList<>(_posList);
    }

    /**
     * Is the position list empty?
     */
    public boolean isEmpty() {
        return _posList.isEmpty();
    }

    /**
     * Return true if the position list contains the given position.
     */
    public boolean contains(P pos) {
        return _posList.contains(pos);
    }

    /**
     * Does the named position exist?
     *
     * @see OffsetPosList
     */
    public synchronized boolean exists(String tag) {
        return (getPositionIndex(tag) != UNKNOWN_INDEX);
    }

    /**
     * Retrieve the position with the given tag.
     */
    public synchronized P getPosition(String tag) {
        int i = getPositionIndex(tag);
        if (i == UNKNOWN_INDEX) return null;

        return getPositionAt(i);
    }

    /**
     * Retrieve the position at the given index.
     */
    public synchronized P getPositionAt(int index) {
        if (index < 0 || index >= _posList.size()) return null;
        return _posList.get(index);
    }

    /**
     * Get the index of the given position, returning ITEM_NOT_FOUND if the
     * position isn't in the list.
     */
    public synchronized int getPositionIndex(P tp) {
        return getPositionIndex(tp.getTag());
    }

    /**
     * Get the index of the given position, returning ITEM_NOT_FOUND if the
     * position isn't in the list.
     */
    public synchronized int getPositionIndex(String tag) {
        int res = UNKNOWN_INDEX;
        int sz = size();
        for (int i = 0; i < sz; ++i) {
            P tp = getPositionAt(i);
            if (tp.getTag().equals(tag)) {
                res = i;
                break;
            }
        }
        return res;
    }


    //
    // Get a unique tag for the offset position.
    //
    private synchronized String _getUniqueTag() {
        int i;
        int size = size();
        for (i = 0; i < size; ++i) {
            if (!exists(OffsetPosBase.OFFSET_TAG + i)) {
                break;
            }
        }

        return OffsetPosBase.OFFSET_TAG + i;
    }

    /**
     * Create an offset position at the end of the list and add it in.
     * A unique tag will be generated and assigned to the created position.
     * Returns the new OffsetPosBase object.
     */
    public P addPosition() {
        return addPosition(UNKNOWN_INDEX, 0.0, 0.0);
    }


    /**
     * Create an offset position at the end of the list and add it in.
     * A unique tag will be generated and assigned to the created position.
     * Returns the new OffsetPosBase object.
     */
    public P addPosition(double xAxis, double yAxis) {
        return addPosition(UNKNOWN_INDEX, xAxis, yAxis);
    }

    /**
     * Create an offset position at the given index and add it to the list.
     * A unique tag will be generated and assigned to the created position.
     * Returns the new OffsetPosBase object.
     */
    public P addPosition(int index) {
        P op;
        synchronized (this) {
            String tag = _getUniqueTag();

            // Create the position
            op = _factory.create(tag);

            if ((index == UNKNOWN_INDEX) || (index >= size())) {
                addPosition(op);
            } else {
                addPosition(index, op);
            }
        }
        return op;
    }

    /**
     * Create a new position in the list with the given tag and
     * set its positional coordinates to xAxis and yAxis assumed to
     * be in degrees.
     */
    public P addPosition(int index, double xOff, double yOff) {
        P op;
        synchronized (this) {
            String tag = _getUniqueTag();

            // Create the position
            op = _factory.create(tag, xOff, yOff);

            if ((index == UNKNOWN_INDEX) || (index >= size())) {
                addPosition(op);
            } else {
                addPosition(index, op);
            }
        }
        return op;
    }

    private void addPosition(P op) {
        syncAdvancedGuiding(op);
        if (op == null) return;
        _posList.add(op);
        _notifyOfAdd(op);
    }

    private void addPosition(int index, P  op) {
        syncAdvancedGuiding(op);
        if (op == null) return;
        _posList.add(index, op);
        _notifyOfAdd(op);
    }

    private void syncAdvancedGuiding(P op) {
        // Remove any extra advanced guiding probes that don't apply to this
        // list.
        Set<GuideProbe> rmProbes = op.getGuideProbes();
        rmProbes.removeAll(advancedGuiding);
        rmProbes.forEach(op::removeLink);

        // Add any that are missing.
        for (GuideProbe gp : advancedGuiding) {
            if (op.getLink(gp) == null) {
                GuideOption opt = gp.getGuideOptions().fromDefaultGuideOption(op.getDefaultGuideOption());
                op.setLink(gp, opt);
            }
        }
    }
    private void syncAdvancedGuiding() {
        for (P op : this) syncAdvancedGuiding(op);
    }

    public Set<GuideProbe> getAdvancedGuiding() {
        return advancedGuiding;  // this must be unmodifiable!
    }

    private void updateAdvancedGuiding(Set<GuideProbe> newValue) {
        Set<GuideProbe> oldValue = advancedGuiding;
        advancedGuiding = newValue;
        syncAdvancedGuiding();
        _notifyOfPropertyChange(ADVANCED_GUIDING_PROP, oldValue, newValue);
    }

    public void addAdvancedGuiding(GuideProbe gp) {
        if (advancedGuiding.contains(gp)) return;
        final Set<GuideProbe> a = new TreeSet<>(GuideProbe.KeyComparator.instance);
        a.addAll(advancedGuiding);
        a.add(gp);
        updateAdvancedGuiding(Collections.unmodifiableSet(a));
    }

    public void removeAdvancedGuiding(GuideProbe gp) {
        if (!advancedGuiding.contains(gp)) return;

        final Set<GuideProbe> newValue;
        if (advancedGuiding.size() == 1) {
            newValue = Collections.emptySet();
        } else {
            Set<GuideProbe> a = new TreeSet<>(GuideProbe.KeyComparator.instance);
            a.addAll(advancedGuiding);
            a.remove(gp);
            newValue = Collections.unmodifiableSet(a);
        }
        updateAdvancedGuiding(newValue);
    }

    private static boolean sameGuiders(Collection<GuideProbe> g1, Collection<GuideProbe> g2) {
        return (g1.size() == g2.size()) && g1.containsAll(g2) && g2.containsAll(g1);
    }

    public void setAdvancedGuiding(Collection<GuideProbe> guiders) {
        // no change, skip the property change etc.
        if (sameGuiders(advancedGuiding, guiders)) return;

        final Set<GuideProbe> newValue;
        if (guiders.isEmpty()) {
            newValue = Collections.emptySet();
        } else {
            Set<GuideProbe> a = new TreeSet<>(GuideProbe.KeyComparator.instance);
            a.addAll(guiders);
            newValue = Collections.unmodifiableSet(a);
        }
        updateAdvancedGuiding(newValue);
    }

    /**
     * Remove all positions.
     */
    public void removeAllPositions() {
        synchronized (this) { _posList.clear(); }
        _notifyOfReset();
    }

    /**
     * Remove a given position by its tag.
     */
    public P removePosition(String tag) {
        final P p;
        synchronized (this) {
            p = getPosition(tag);
            if (p != null) _posList.remove(p);
        }
        if (p != null) _notifyOfRemove(p);
        return p;
    }

    /**
     * Remove a position with the same tag as the given position from the list.
     */
    public void removePosition(P op) {
        final boolean notify;
        synchronized (this) {
            notify = _posList.remove(op);
        }
        if (notify) _notifyOfRemove(op);
    }

    // Decrement the position's location in the list.  This is the private
    // method that does the work.  A public interface is provided that
    // notifies observers.
    private synchronized int _decrementPosition(P op) {
        // See where the offset position currently is
        String tag = op.getTag();

        int i = getPositionIndex(tag);
        if (i == UNKNOWN_INDEX) return UNKNOWN_INDEX;

        // Can't decrement past the beginning of the list
        if (i == 0) return i;

        // Don't want the watchers removed.
        _posList.remove(op);
        _posList.add(i - 1, op);

        return i - 1;
    }

    /**
     * Decrement the position's location in the list.  The new position is
     * returned, or UNKNOWN_INDEX if the position isn't in the list.
     * @return new index or 0 if item is already at the beginning of the list.
     */
    public int decrementPosition(P op) {
        int i = _decrementPosition(op);
        if (i != UNKNOWN_INDEX) _notifyOfReset();
        return i;
    }

    // Increment the position's location in the list.  This is the private
    // method that does the work.  A public interface is provided that
    // notifies observers.
    private synchronized int _incrementPosition(P op) {
        // See where the offset position currently is
        String tag = op.getTag();

        int i = getPositionIndex(tag);
        if (i == UNKNOWN_INDEX) return UNKNOWN_INDEX;

        // Can't increment past the end of the list
        if (i + 1 == size()) return i;


        // Don't want the watchers removed.
        _posList.remove(op);
        _posList.add(i + 1, op);

        return i + 1;
    }

    /**
     * Increment the position's location in the list.  The new position is
     * returned, or UNKNOWN_INDEX if the position isn't in the list.
     */
    public int incrementPosition(P op) {
        int i = _incrementPosition(op);
        if (i != UNKNOWN_INDEX) _notifyOfReset();
        return i;
    }

    // Move the position to the back of the list.  This is the private
    // method that does the work.  A public interface is provided that
    // notifies observers.
    private synchronized int _positionToBack(P op) {
        // See where the offset position currently is
        String tag = op.getTag();

        int i = getPositionIndex(tag);
        if (i == UNKNOWN_INDEX) return UNKNOWN_INDEX;

        // Already at the back of the list
        if (i + 1 == size()) return i;

        // Don't want the watchers removed.
        _posList.remove(op);
        _posList.add(op);

        return (size() - 1);
    }

    /**
     * Increment the position's location in the list.  The new position is
     * returned, or UNKNOWN_INDEX if the position isn't in the list.
     */
    public int positionToBack(P op) {
        int i = _positionToBack(op);
        if (i != UNKNOWN_INDEX) _notifyOfReset();
        return i;
    }

    // Move the position to the front of the list.  This is the private
    // method that does the work.  A public interface is provided that
    // notifies observers.
    private synchronized int _positionToFront(P op) {
        // See where the offset position currently is
        String tag = op.getTag();

        int i = getPositionIndex(tag);
        if (i == UNKNOWN_INDEX) return UNKNOWN_INDEX;

        // Can't decrement past the beginning of the list
        if (i == 0) return 0;

        // Don't want the watchers removed.
        _posList.remove(op);
        _posList.add(0, op);

        return 0;
    }

    /**
     * Move the position to the initial place in the list.
     */
    public int positionToFront(P op) {
        int i = _positionToFront(op);
        if (i != UNKNOWN_INDEX) _notifyOfReset();
        return i;
    }

    /** Return a paramset describing this object */
    public ParamSet getParamSet(PioFactory factory, String name) {
        ParamSet paramSet = factory.createParamSet(name);

        if (advancedGuiding.size() > 0) {
            List<String> keys = new ArrayList<>(advancedGuiding.size());
            for (GuideProbe gp : advancedGuiding) keys.add(gp.getKey());
            Pio.addListParam(factory, paramSet, ADVANCED_GUIDING_PROP, keys);
        }

        int size = size();
        if (size == 0) return paramSet;

        for (int i = 0; i < size; i++) {
            P op = getPositionAt(i);
            ParamSet ps = op.getParamSet(factory, op.getTag());
            ps.setSequence(i);
            paramSet.addParamSet(ps);
        }

        return paramSet;
    }

     // Initialize this object from the given paramset
    public void setParamSet(ParamSet paramSet) {
        if (paramSet == null) return;

        boolean migrate = false;
        final List<P> positions = new ArrayList<>(paramSet.getParamSetCount());
        for (ParamSet ps : paramSet.getParamSets()) {
            final P op = _factory.create(ps.getName());
            op.setParamSet(ps);
            positions.add(op);

            if (!migrate) {
                final Object def = Pio.getValue(ps, OffsetPosBase.DEFAULT_GUIDE_OPTION_PARAM);
                migrate = (def == null);
            }
        }

        final Set<GuideProbe> advanced = new TreeSet<>(GuideProbe.KeyComparator.instance);
        if (migrate) {
            OffsetPosMigration.apply(positions);
            if (positions.size() > 0) {
                advanced.addAll(positions.get(0).getLinks().keySet());
            }
        } else {
            // Figure out what to use for advanced guiding, if anything.
            List<String> keys = Pio.getValues(paramSet, ADVANCED_GUIDING_PROP);
            if (keys != null && keys.size() > 0) {
                for (String key : keys) {
                    GuideProbe gp = GuideProbeMap.instance.get(key);
                    if (gp == null) {
                        LOG.log(Level.WARNING, "Skipping unrecognized guide probe key: " + key);
                        continue;
                    }
                    advanced.add(gp);
                }
            }
        }

        synchronized (this) {
            if (advanced.size() == 0) {
                advancedGuiding = Collections.emptySet();
            } else {
                advancedGuiding = Collections.unmodifiableSet(advanced);
            }

            _posList.clear();
            for (P pos : positions) _posList.add(pos);
        }
        _notifyOfReset();
    }


    public synchronized String toString() {
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < size(); ++i) {
            out.append(getPositionAt(i));
            out.append("\n");
        }
        return out.toString();
    }

    /**
     * Add a pos list observer.
     */
    public synchronized void addWatcher(OffsetPosListWatcher<P> w) {
        if (_watchers == null) {
            _watchers = new ArrayList<>();
        } else if (_watchers.contains(w)) {
            return;
        }
        _watchers.add(w);
    }

    /**
     * Remove a pos list observer.
     */
    public synchronized void deleteWatcher(OffsetPosListWatcher<P> w) {
        if (_watchers == null) return;
        _watchers.remove(w);
    }

    /**
     * Copy the watchers list.
     */
    protected final synchronized List<OffsetPosListWatcher<P>> _getWatchers() {
        if (_watchers == null) return null;
        return new ArrayList<>(_watchers);
    }

    /**
     * Notify of a reset.
     */
    protected void _notifyOfReset() {
        List<OffsetPosListWatcher<P>> v = _getWatchers();
        if (v == null) return;

        int n = v.size();
        for (int i = 0; i < n; i++) {
            OffsetPosListWatcher<P> w = v.get(i);
            w.posListReset(this);
        }
    }

    /**
     * Notify that a position has been added.
     */
    protected void _notifyOfAdd(P tp) {
        _notifyOfAdd(Collections.singletonList(tp));
    }

    protected void _notifyOfAdd(List<P> tp) {
        List<OffsetPosListWatcher<P>> v = _getWatchers();
        if (v == null) return;

        int n = v.size();
        for (int i = 0; i < n; i++) {
            OffsetPosListWatcher<P> w = v.get(i);
            w.posListAddedPosition(this, new ArrayList<>(tp));
        }
    }

    /**
     * Notify that a position has been removed.
     */
    protected void _notifyOfRemove(P tp) {
        _notifyOfRemove(Collections.singletonList(tp));
    }
    protected void _notifyOfRemove(List<P> tp) {
        List<OffsetPosListWatcher<P>> v = _getWatchers();
        if (v == null) return;

        int n = v.size();
        for (int i = 0; i < n; i++) {
            OffsetPosListWatcher<P> w = v.get(i);
            w.posListRemovedPosition(this, new ArrayList<>(tp));
        }
    }

    protected void _notifyOfPropertyChange(String propertyName, Object oldValue, Object newValue) {
        List<OffsetPosListWatcher<P>> v = _getWatchers();
        if (v == null) return;

        for (OffsetPosListWatcher<P> w : v) {
            w.posListPropertyUpdated(this, propertyName, oldValue, newValue);
        }
    }
}
