// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: AbstractObsComponentCB.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.config;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.data.config.IConfig;


import java.util.Map;


/**
 * An abstract base class useful for building
 * {@link IConfigBuilder configuration builder}s for observation components.
 * This class handles the required contract of a configuration builder,
 * leaving the subclass to focus on extracting the configuration information
 * from its observation component.  In particular, there are three methods
 * that the subclass must implement:
 *
 * <ul>
 *   <li> <code>{@link #thisReset}</code>
 *   <li> <code>{@link #hasNext}</code>
 *   <li> <code>{@link #thisApplyNext}</code>
 * </ul>
 *
 * Each is detailed below.
 */
public abstract class AbstractObsComponentCB implements IConfigBuilder {

    private ISPObsComponent _obsComp;

    private transient boolean _firstTime;
    private transient Object _dataObject;

    /**
     * Constructs with the observation component whose state will be applied
     * to the configuration passed to the <code>{@link #applyNext}</code>
     * method.
     */
    protected AbstractObsComponentCB(ISPObsComponent obsComp) {
        _obsComp = obsComp;
    }

    public Object clone() {
        AbstractObsComponentCB result;
        try {
            result = (AbstractObsComponentCB) super.clone();
        } catch (CloneNotSupportedException ex) {
            // Won't happen, since Object implements cloneable ...
            throw new InternalError();
        }
        result._firstTime  = false;
        result._dataObject = null;
        return result;
    }

    public void reset(Map options)  {
        _firstTime = true;
        // Go to the data object cache for acces
        _dataObject = _obsComp.getDataObject();
        thisReset(options);
    }

    /**
     * Provides the subclass an opportunity to reset its state, if necessary.
     * A subclass might choose to read the state of its node's data object
     * for example.
     */
    protected abstract void thisReset(Map options) ;

    /**
     * Gets the observation component whose configuration is being iterated.
     */
    protected ISPObsComponent getObsComponent() {
        return _obsComp;
    }

    /**
     * Gets the data object associated with the node.  The data object
     * typically contains the state from which the configuration is applied.
     */
    protected Object getDataObject() {
        return _dataObject;
    }

    public boolean hasNext()  {
        return _firstTime && thisHasConfiguration();
    }

    /**
     * Should return <code>true</code> if there is any configuration
     * information to apply.   Should only return <code>false</code>
     * in the rare instance when there is no state information from which
     * to produce a configuration.  There is no need to worry about
     * whether <code>applyNext</code> has been called previously etc.
     * This class takes care of those details.
     */
    protected abstract boolean thisHasConfiguration() ;

    public void applyNext(IConfig config, IConfig prevFull)  {
        if (!_firstTime) {
            throw new IllegalStateException("Builder not reset.");
        }
        thisApplyNext(config, prevFull);

        _firstTime = false;
        _dataObject = null;
    }

    /**
     * Applies state information (typically from the node's data object) to
     * the given configuration.  In other words, it translates the state into
     * parameters and values for one or more system configurations and applies
     * them.
     */
    protected abstract void thisApplyNext(IConfig config, IConfig prevFull) ;

}

