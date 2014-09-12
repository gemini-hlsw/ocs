//
// $Id: Sequence.java 8396 2008-01-18 14:25:49Z swalker $
//
package edu.gemini.spModel.obsseq;

import edu.gemini.spModel.config2.Config;

import java.util.*;
import java.io.Serializable;

/**
 * The Sequence class represents both a container of nested child Sequence
 * objects, and a producer of changing configuration information of its own
 * right.  For example, a Sequence object might contain various telescope
 * offset positions as well as child Sequence objects designed to switch
 * among instrument filters.  The Sequence object itself (optionally) contains
 * a {@link ConfigProducer} to handle stepping through its associated
 * configuration changes (e.g., offset position), and implements
 * {@link SequenceContainer} to handle its responsibilites as a parent of
 * nested child Sequence objects.
 */
public final class Sequence implements ConfigProducer, SequenceContainer, Serializable {
    public static final Sequence[] EMPTY_ARRAY = new Sequence[0];

    private class SequenceConfigMerger implements ConfigMerger {
        private ConfigMerger _ourMerger;
        private Iterator _childIterator;
        private ConfigMerger _childMerger;
        private boolean _childReset;

        private SequenceConfigMerger() {
            if (_configProducer != null) {
                _ourMerger = _configProducer.getConfigMerger();
            }
            _resetChildConfig();
        }

        private void _resetChildConfig() {
            _childIterator = _sequences.iterator();
            _childReset = true;
            _advanceToNextChild();
        }

        private void _advanceToNextChild() {
            _childMerger = null;
            while (_childIterator.hasNext()) {
                Sequence seq = (Sequence) _childIterator.next();
                _childMerger = seq.getConfigMerger();
                if (_childMerger.hasNextConfig()) {
                    break;
                } else {
                    _childMerger = null;
                }
            }

        }

        private boolean _hasNextChildConfig() {
            return (_childMerger != null) && _childMerger.hasNextConfig();
        }

        private boolean _hasNextLocalConfig() {
            return (_ourMerger != null) && _ourMerger.hasNextConfig();
        }

        public boolean hasNextConfig() {
            return _hasNextChildConfig() || _hasNextLocalConfig();
        }

        public void mergeNextConfig(Config config) {
            // Merge in our contribution first, assuming this is the first
            // time through or we've just finished going through everything
            // our children have to offer.
            if (_childReset && _hasNextLocalConfig()) {
                _childReset = false;
                _ourMerger.mergeNextConfig(config);
            }

            // Add in the next child contribution.  An invariant of this method
            // is that the _childMerger variable, if set to something, is
            // poised to deliver the next configuration.  So after getting
            // the next configuration (if any), we update _childMerger.
            if (_hasNextChildConfig()) {
                _childMerger.mergeNextConfig(config);

                // see if we need to advance or reset the child iterators
                if (!_childMerger.hasNextConfig()) {
                    _advanceToNextChild();
                    if ((_childMerger == null) && _hasNextLocalConfig()) {
                        // nothing more to add from the children, but we do have
                        // more to contribute from the local iterator, so reset
                        // the children to have them run through their
                        // configurations again
                        _resetChildConfig();
                    }
                }
            } else {
                // There are no children; set the reset flag in order to
                // have _ourMerger make its contribution the next time.
                _childReset = true;
            }
        }
    }

    private ConfigProducer _configProducer;
    private List<Sequence> _sequences = new ArrayList<Sequence>();

    /**
     * Constructs an empty Sequence object without children or
     * {@link ConfigProducer}.
     */
    public Sequence() {
    }

    /**
     * Constructs a Sequence object with a particular configuration producer.
     */
    public Sequence(ConfigProducer configProducer) {
        _configProducer = configProducer;
    }

    /**
     * Gets a reference to the {@link ConfigProducer} in use by this
     * Sequence object.
     */
    public ConfigProducer getConfigProducer() {
        return _configProducer;
    }

    /**
     * Sets the {@link ConfigProducer} to be used by this Sequence object.
     *
     * @param producer new ConfigProducer to use (replacing the existing one
     * if any
     *
     * @return previous ConfigProdcuer in use by this Sequence, if any;
     * <code>null</code> otherwise
     */
    public ConfigProducer setConfigProducer(ConfigProducer producer) {
        ConfigProducer orig = _configProducer;
        _configProducer = producer;
        return orig;
    }

    public Sequence[] getAllSequences() {
        return _sequences.toArray(Sequence.EMPTY_ARRAY);
    }

    public void setAllSequences(Sequence[] children) {
        _sequences.clear();
        //noinspection ManualArrayToCollectionCopy
        for (Sequence aChildren : children) {
            _sequences.add(aChildren);
        }
    }

    public void addSequence(int index, Sequence child) {
        _sequences.add(index, child);
    }

    public void addSequence(Sequence child) {
        _sequences.add(child);
    }

    public void clearSequences() {
        _sequences.clear();
    }

    public Iterator sequenceIterator() {
        return _sequences.iterator();
    }

    public Sequence getSequence(int index) {
        return _sequences.get(index);
    }

    public boolean isEmptySequenceContainer() {
        return _sequences.isEmpty();
    }

    public Sequence removeSequence(int index) {
        return _sequences.remove(index);
    }

    public boolean removeSequence(Sequence child) {
        return _sequences.remove(child);
    }

    public int getSequenceCount() {
        return _sequences.size();
    }

    private int _getOurStepCount() {
        return (_configProducer != null) ? _configProducer.getStepCount() : 0;
    }

    private int _getChildrenStepCount() {
        int childStepCount = 0;
        for (Iterator it=sequenceIterator(); it.hasNext(); ) {
            Sequence seq = (Sequence) it.next();
            childStepCount += seq.getStepCount();
        }
        return childStepCount;
    }

    public int getStepCount() {
        int ourStepCount = _getOurStepCount();
        int childStepCount = _getChildrenStepCount();

        if (ourStepCount == 0) return childStepCount;
        if (childStepCount == 0) return ourStepCount;

        return ourStepCount * childStepCount;
    }

    /*
    Flawed implementation.  The problem is that siblings, taken together,
    can cause an items value to change.  For example the first child might
    set the observe type to "OBJECT", but never actually change it.  The
    next child might then set the observe type to "FLAT" and never change it.
    The algorithm below would miss that.
    public ItemKey[] getIteratedKeys() {
        Set s = new TreeSet();
        if (_configProducer != null) {
            ItemKey[] keys = _configProducer.getIteratedKeys();
            for (int i=0; i<keys.length; ++i) {
                s.add(keys[i]);
            }
        }
        for (Iterator it=sequenceIterator(); it.hasNext(); ) {
            Sequence seq = (Sequence) it.next();
            ItemKey[] keys = seq.getIteratedKeys();
            for (int i=0; i<keys.length; ++i) {
                s.add(keys[i]);
            }
        }

        return (ItemKey[]) s.toArray(ItemKey.EMPTY_ARRAY);
    }
    */

    public ConfigMerger getConfigMerger() {
        return new SequenceConfigMerger();
    }
}
