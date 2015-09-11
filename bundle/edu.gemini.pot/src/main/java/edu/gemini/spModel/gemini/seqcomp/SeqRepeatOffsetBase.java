package edu.gemini.spModel.gemini.seqcomp;

import edu.gemini.pot.sp.ISPCloneable;
import edu.gemini.pot.sp.ISPSeqObject;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.IOffsetPosListProvider;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetPosListChangePropagator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Iterator;


/**
 * An iterator for telescope offset positions.  It maintains a position
 * list that details the sequence of offset positions and implements the
 * elements() method to Enumerate them.
 *
 * @see edu.gemini.spModel.target.offset.OffsetPosList
 */
public abstract class SeqRepeatOffsetBase<P extends OffsetPosBase> extends AbstractDataObject implements IOffsetPosListProvider<P>, ISPSeqObject, ISPCloneable, ISPDataObject, Serializable {

    public static final String OFFSET_POS_LIST_PROP = "PosList";

    // Key used for XML datasets
    private static final String _OFFSETS = "offsets";


    // The position list uses the OffsetPosList to
    // construct and maintain a list of offset positions.
    private OffsetPosList<P> _posList;

    private final class PceNotifier implements OffsetPosListChangePropagator.Notifier {
        public void apply() {
            firePropertyChange(OFFSET_POS_LIST_PROP, null, _posList);
        }
    }

    /**
     * Default constructor.
     */
    protected SeqRepeatOffsetBase(SPComponentType type, OffsetPosBase.Factory<P> factory) {
        super(type);
        _posList = new OffsetPosList<P>(factory);
        _posList.addWatcher(new OffsetPosListChangePropagator<P>(new PceNotifier()));
    }


    /**
     * Return the number of iterations performed by this object.
     */
    public int getStepCount() {
        return size();
    }


    /**
     * Return true if the offset position list provider feature is enabled.
     */
    public boolean isOffsetPosListProviderEnabled() {
        return true;
    }

    /**
     * Get the position list data structure for the offset positions in
     * the item's list.
     */
    public OffsetPosList<P> getPosList() {
        return _posList;
    }

    /**
     * Returns the number of offsets in the sequence.
     */
    public int size() {
        return _posList.size();
    }

    /**
     * Enumerate the steps of the offset iterator.
     */
    public Iterator elements() {
        return _posList.iterator();
    }


    /**
     * Return a parameter set describing the current state of this object.
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        if (_posList.size() != 0) {
            ParamSet p = _posList.getParamSet(factory, _OFFSETS);
            paramSet.addParamSet(p);
        }
        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        ParamSet p = paramSet.getParamSet(_OFFSETS);
        if (p != null) {
            getPosList().setParamSet(p);
        }
    }

    /**
     * Override clone to make sure the offset position list is correctly
     * initialized.
     */
    public Object clone() {
        //noinspection unchecked
        SeqRepeatOffsetBase<P> sro = (SeqRepeatOffsetBase<P>) super.clone();

        //noinspection unchecked
        sro._posList = (OffsetPosList<P>)sro._posList.clone();
        sro._posList.addWatcher(new OffsetPosListChangePropagator<P>(sro.new PceNotifier(), sro._posList));

        return sro;
    }

    // Setup change propagation when de-serializing.
    private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
        is.defaultReadObject();
        _posList.addWatcher(new OffsetPosListChangePropagator<P>(new PceNotifier(), _posList));
    }
}
