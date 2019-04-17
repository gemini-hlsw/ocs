package edu.gemini.spModel.config;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.data.config.IConfig;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * An abstract base class useful for building
 * {@link IConfigBuilder configuration builder}s for sequence components.
 * This class handles the required contract of a configuration builder,
 * including applying the configuration on the sequence component's children
 * if any.  The subclass may focus only on extracting the configuration
 * information from its sequence component.  In particular, there are
 * three methods that the subclass must implement:
 *
 * <ul>
 *   <li> <code>{@link #thisReset}</code>
 *   <li> <code>{@link #thisHasNext}</code>
 *   <li> <code>{@link #thisApplyNext}</code>
 * </ul>
 *
 * Each is detailed below.
 */
public abstract class AbstractSeqComponentCB implements IConfigBuilder {

    private ISPSeqComponent _seqComp;

    private transient SPNodeKey _nodeKey;
    private transient Object _dataObject;

    private transient boolean _firstTime = true;
    private transient boolean _isReset;
    // This is a flag indicating that this builder should be applied
    private transient boolean _applyThisIterator;

    // A List of this builder's children builders
    private transient List<IConfigBuilder> _childBuilders;
    // An index indicating which of this builder's children is in use
    private transient int _nextChildBuilderIndex;
    private transient IConfigBuilder _curChildBuilder;

    private transient Map<String, Object> _options;


    /**
     * Constructs with the sequence component whose state will be applied
     * to the configuration passed to the <code>{@link #applyNext}</code>
     * method.
     */
    protected AbstractSeqComponentCB(ISPSeqComponent seqComp) {
        _seqComp = seqComp;
    }

    public Object clone() {
        AbstractSeqComponentCB result;
        try {
            result = (AbstractSeqComponentCB) super.clone();
        } catch (CloneNotSupportedException ex) {
            // Won't happen, since Object implements cloneable ...
            throw new InternalError();
        }
        result._dataObject = null;
        result._firstTime  = true;
        result._isReset    = false;
        result._applyThisIterator = false;
        result._childBuilders     = null;
        result._nextChildBuilderIndex = 0;
        result._curChildBuilder   = null;
        result._options = null;

        return result;
    }

    /**
     * Gets the sequence component whose configurations are being
     * iterated.
     */
    protected ISPSeqComponent getSeqComponent() {
        return _seqComp;
    }

    /**
     * Gets the data object associated with the node.  The data object
     * typically contains the state from which the configuration is applied.
     */
    protected Object getDataObject() {
        return _dataObject;
    }

    /**
     * Reset recursively calls reset on all its children and then
     * itself.  Therefore, seq components are reset from the bottom up.
     */
    public void reset(Map<String, Object> options)  {
        _options = options;
        _nodeKey    = _seqComp.getNodeKey();
        _dataObject = _seqComp.getDataObject();

        ArrayList<IConfigBuilder> childBuilders;

        // Create the list of child sequence component parameter builders.
        List<ISPSeqComponent> childList = _seqComp.getSeqComponents();
        if (childList == null || (childList.size() == 0)) {
            childBuilders = new ArrayList<>(0);
        } else {
            childBuilders = new ArrayList<>(childList.size());

            for (ISPSeqComponent seqComp: childList) {
                IConfigBuilder cb = (IConfigBuilder) seqComp.getClientData(IConfigBuilder.USER_OBJ_KEY);
                if (cb != null) {
                    childBuilders.add(cb);
                }
            }

            childBuilders.trimToSize();
        }
        _childBuilders = childBuilders;

        // This is recursive!
        _firstChildBuilder();
        thisReset(_options);
        _firstTime = true;
        _isReset = true;
        _applyThisIterator = thisHasNext();
    }

    /**
     * Provides the subclass an opportunity to reset its state, most likely
     * based upon the current state of the data object.
     */
    protected abstract void thisReset(Map<String, Object> options) ;

    /**
     * Starts over at the first child configuration builder.
     */
    private void _firstChildBuilder()  {
        _nextChildBuilderIndex = 0;
        _nextChildBuilder();
    }

    /**
     * Moves to the next valid configuration builder that has configurations to
     * apply.
     */
    private void _nextChildBuilder()  {
        int sz = _childBuilders.size();

        // If already at or past the last child, then there are no "next" child
        // builders.
        // This works strangely for a builder with no children.  Since
        // It's builder list is empty, it's size (sz) is 0, therefore this
        // check is true
        if (_nextChildBuilderIndex >= sz) {
            _nextChildBuilderIndex = sz;
            _curChildBuilder = null;
            return;
        }

        // Move to the next child builder. Note that this is post increment
        // so the first time index is 0

        int index = _nextChildBuilderIndex++;
        _curChildBuilder = _childBuilders.get(index);
        // This recursive call descends down the tree of CBs
        _curChildBuilder.reset(_options);

        // If it has a next configuration, then we're done.  Otherwise skip it
        // and move to the next child builder.

        if (!_curChildBuilder.hasNext()) {
            // Recursive call to find something with a configuration
            _nextChildBuilder();
        }
    }


    public boolean hasNext()  {
        // If the applyNext() method has never been called, then the iterator
        // only has more values if the root (this component) has values.
        // _firstTime starts out false.  When reset is completed, _firstTime
        // is true.  When applyNext is called, _firstTime is set back to false.
        // So this is saying, if _firstTime is true, we've just called
        // reset (and no applyNext()) so for their to be a next, thisHasNext
        // must return true.

        if (_firstTime) {
            return thisHasNext();
        }

        // Now we know that applyNext has been called before.  So there are
        // additional values if any child builder has values, or, failing that,
        // this component has more values.  First check child builders.

        if (_curChildBuilder != null) {
            if (_curChildBuilder.hasNext()) {
                return true;
            }

            _nextChildBuilder();
            if (_curChildBuilder != null) {
                return true;
            }
        }

        // Now check this iterator, going back to the first child builder and
        // starting over.

        boolean hasNext = thisHasNext();

        if (hasNext) {
            _firstChildBuilder();
            _applyThisIterator = true;
        }

        return hasNext;
    }

    /**
     * Should return <code>true</code> if the associated sequence component
     * has configuration information yet to be applied.  This class takes
     * care of handling any child sequence components.  The subclass need
     * only examine the current sequence component and report on its
     * ability to continue the iteration.
     */
    protected abstract boolean thisHasNext() ;

    public void applyNext(IConfig config, IConfig prevFull)  {
        if (_firstTime && !_isReset) {
            throw new IllegalStateException("Builder not reset.");
        }
        _firstTime = false;
        _isReset = false;

        // Every sequence config builder will add its node key to the list
        // of node keys in the meta data.  This can be used to match steps to
        // the sequence nodes that had a role in producing them.
        MetaDataConfig.extract(config).addNodeKey(_nodeKey);

        if (_applyThisIterator) {
            thisApplyNext(config, prevFull);
            _applyThisIterator = false;
        }

        // In the case where we are at a leaf, _curChildBuilder will be null
        // see the beginning of _nextchildbulider
        if (_curChildBuilder != null) {
            _curChildBuilder.applyNext(config, prevFull);
        }
    }

    /**
     * Applies state information (typically from the node's data object) to
     * the given configuration.  In other words, it translates the state into
     * parameters and values for one or more system configurations and applies
     * them.  The subclass should not be concerned with any nested
     * sequence components since applying their configurations is handled by
     * this class.
     */
    protected abstract void thisApplyNext(IConfig config, IConfig prevFull) ;

    /**
     * Obtains the associated instrument parameter value in the given config,
     * if it exists.
     */
    public static Option<Object> lookupInstrumentParameterValue(String n, Option<IConfig> oc) {
        return oc.flatMap(c -> ImOption.apply(c.getSysConfig(INSTRUMENT_CONFIG_NAME)))
                 .flatMap(s -> ImOption.apply(s.getParameterValue(n)));
    }

    /**
     * Obtains the associated instrument parameter value in `oc` but falls back
     * on `op` if not found. This is meant to be used inside of apply next
     * implementations where there is a current configuration with the modified
     * values for the current step and a previous full configuration for past
     * steps.
     *
     * @param n parameter name
     * @param oc optional current config
     * @param op optional previous full config
     */
    public static Option<Object> lookupInstrumentParameterValue(String n, Option<IConfig> oc, Option<IConfig> op) {
        return lookupInstrumentParameterValue(n, oc)
                 .orElse(() -> lookupInstrumentParameterValue(n, op));
    }

    /**
     * Obtains the associated instrument parameter value in `c` but falls back
     * on `p` if not found. This is meant to be used inside of apply next
     * implementations where there is a current configuration with the modified
     * values for the current step and a previous full configuration for past
     * steps.
     *
     * @param n parameter name
     * @param c current config, possibly `null`
     * @param p previous full config, possibly `null`
     */
    public static Option<Object> lookupInstrumentParameterValue(String n, IConfig c, IConfig p) {
        return lookupInstrumentParameterValue(n, ImOption.apply(c), ImOption.apply(p));
    }

    /**
     * Obtains the associated instrument parameter value in `c` but falls back
     * on `p` if not found, casting to the expected type.  This is meant to be
     * used inside of apply next implementations where there is a current
     * configuration with the modified values for the current step and a
     * previous full configuration for past steps
     *
     * @param n parameter name
     * @param c current config, possibly `null`
     * @param p previous full config, possibly `null`
     */
    public static <T> Option<T> extractInstrumentParameterValue(String n, IConfig c, IConfig p) {
        return lookupInstrumentParameterValue(n, c, p).map(o -> (T) o);
    }

}

