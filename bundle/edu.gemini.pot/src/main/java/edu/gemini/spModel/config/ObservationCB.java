// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ObservationCB.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.config;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProviderHolder;
import edu.gemini.spModel.seqcomp.SeqRepeatCbOptions;

import java.io.IOException;
import java.io.ObjectInputStream;

import java.util.*;


/**
 * The {@link IConfigBuilder configuration builder} for an observation node.
 * When an observation is executed, this configuration builder is called
 * upon to step through the available configurations.  It makes use of the
 * configuration builders associated with the contained observation and
 * sequence components.
 *
 * <p> In the first step, two things occur
 * <ol>
 *   <li>the contained observation component's builders (if any) are used
 *       to set the static configuration
 *   <li>the contained sequence component's builder (if any) is used to
 *       set the first step of the sequence.
 * </ol>
 *
 * <p>Subsequent steps call upon the sequence configuration builder
 * successively until all the iteration steps have been completed.
 * <p>This configuration builder has been specialized for Gemini.
 * It manipulates the configurations at each step to make sure that
 * the configurations match the "time line" and that there is always
 * a clear value for ncoadds and exposureTime.
 */
public class ObservationCB implements IConfigBuilder {

    private ISPObservation _obs;

    private transient ArrayList<IConfigBuilder> _obsCompBuilders;
    // The current top level seq builder
    private transient IConfigBuilder _seqBuilder;
    // The current index of the seq builder
    private transient int _seqBuilderIndex;
    private transient boolean _firstTime = true;
    private transient boolean _isReset;
    private transient Map _options;

    /**
     * Constructs with the observation that will be sequenced.
     */
    public ObservationCB(ISPObservation obs) {
        _obs = obs;
    }

    // return the observation node for this observation
    protected ISPObservation _getObsNode() {
        return _obs;
    }

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public Object clone() {
        ObservationCB result;
        try {
            result = (ObservationCB) super.clone();
        } catch (CloneNotSupportedException ex) {
            // Won't happen, since Object implements cloneable ...
            throw new InternalError();
        }

        result._obsCompBuilders = null;
        result._seqBuilder      = null;
        result._seqBuilderIndex = 0;
        result._firstTime       = true;
        result._isReset         = false;
        result._options         = null;

        return result;
    }

    // Private method to reset the obs components
    private void _resetObsComponents()  {
        // First handle the observation components.  For each observation
        // component with a configuration builder user object, reset the
        // builder and place it in a list (_obsCompBuilders).

        List<ISPObsComponent> obsCompList = _obs.getObsComponents();
        if (obsCompList != null) {
            _obsCompBuilders = new ArrayList<IConfigBuilder>(obsCompList.size());

            for (ISPObsComponent obsComp : obsCompList) {
                IConfigBuilder cb;
                cb = (IConfigBuilder) obsComp.getClientData(IConfigBuilder.USER_OBJ_KEY);
                if (cb != null) {
                    cb.reset(_options);
                    _obsCompBuilders.add(cb);
                }
            }

            _obsCompBuilders.trimToSize();
        }
    }

    // private method to fetch a top level sequence builder via
    // an index
    private IConfigBuilder _getTopLevelBuilder(int index)  {
        // Next handle the sequence component (if any).  Gemini uses a
        // "dummy" node as the root of the sequence
        // If this doesn't yet exist, return false
        final ISPSeqComponent top = _obs.getSeqComponent();
        if (top == null) return null;

        // Get all the seq builders that are children of top.  If there
        // are none, return false
        final List<ISPSeqComponent> seqCompList = top.getSeqComponents();
        if (seqCompList == null) return null;

        // If the requested builder is out of range, return false
        final int size = seqCompList.size();
        if (index >= size) return null;

        final ISPSeqComponent seqComp = seqCompList.get(index);

        // Now try to get a builder
        final IConfigBuilder cb = (IConfigBuilder) seqComp.getClientData(IConfigBuilder.USER_OBJ_KEY);

        // Ignore nodes like SeqDataProc that don't have a config builder
        return (cb == null) ? _getTopLevelBuilder(++_seqBuilderIndex) : cb;
    }

    // Internal private routine that does the meat of the reset
    private void _doReset(Map options)  {
        // First handle the observation components.  For each observation
        // component with a configuration builder user object, reset the
        // builder and place it in a list (_obsCompBuilders).
        _resetObsComponents();

        if (_seqBuilder != null) {
            _seqBuilder.reset(options);
        }

        _firstTime = true;
        _isReset = true;
        _options = options;
    }

    /**
     * The public reset method of ObservationCB sets a reset
     * to all the obs components that contain a configuration builder
     * and then recursively to all sequence components that have
     * configuration builders.
     */
    public void reset(Map options)  {
        _seqBuilderIndex = 0;
        _seqBuilder = _getTopLevelBuilder(_seqBuilderIndex);
        _doReset(options);
    }

    // Private method to test for seqComp hasNext and shuffles the
    // correct _seqComp as needed
    private boolean _seqHasNext()  {
        // If the ISPSeqComponent's builder has a next configuration, then the
        // observation has a next sequence too.
        if (_seqBuilder == null)
            return false;

        // Does the current seq builder have next
        if (_seqBuilder.hasNext())
            return true;

        // The current seq builder has no more are there more seqbuilders?
        _seqBuilder = _getTopLevelBuilder(_seqBuilderIndex + 1);
        if (_seqBuilder == null)
            return false;
        _seqBuilderIndex++;

        // Now reset the new seq builder run
        _doReset(_options);

        // Now that it has been moved, try again
        return _seqHasNext();
    }

    public boolean hasNext()  {
        // Make sure reset has been called.
        if ((_isFirstTime()) && !_isReset) {
            throw new IllegalStateException("Builder not reset.");
        }

        // If the ISPSeqComponent's builder has a next configuration, then the
        // observation has a next sequence too.
        if (_seqHasNext())
            return true;

        // In case there is no sequence at all, check whether there is a
        // base (static) configuration for this observation yet to be applied.

        if ((!_isFirstTime()) || (_obsCompBuilders == null)) {
            return false;
        }

        // If we are first time and we have obs comp builders check
        // to see if any have next

        for (IConfigBuilder cb : _obsCompBuilders) {
            if (cb.hasNext()) {
                // There is at least one remaining observation component to apply.
                return true;
            }
        }

        return false;
    }

    // Private method to apply the obs components
    // This assumes that for each top level
    private void _applyObsComponents(IConfig config, IConfig prevFull)  {
        // Make sure reset has been called.
        if (!_isReset) {
            throw new IllegalStateException("Builder not reset.");
        }
        _isReset = false;

        // Go through each observation component builder and apply it.
        // This sets the static configuration.
        if (_isFirstTime() && (_obsCompBuilders != null)) {
            for (IConfigBuilder cb : _obsCompBuilders) {
                if (cb.hasNext()) cb.applyNext(config, prevFull);
            }
        }
    }

    public void applyNext(IConfig config, IConfig prevFull)  {
        Set<String> obsCompNames = null;

        // This is done for the first applyNext
        if (_isFirstTime()) {
            _applyObsComponents(config, prevFull);
            // Save the system names so they can be moved up at the end
            obsCompNames = config.getSystemNames();
            _firstTime = false;
        }

        // Now apply the next step in the sequence.
        if (_seqHasNext()) {
            _seqBuilder.applyNext(config, prevFull);
        }

        // If obsCompNames is != null remove the names and insert them
        // at the beginning
        if (obsCompNames != null) {
            for (Object obsCompName : obsCompNames) {
                String sysName = (String) obsCompName;
                ISysConfig sysConfig = config.getSysConfig(sysName);
                if (sysConfig != null) {
                    // Now remove it
                    config.removeSysConfig(sysName);
                    // Now add it back at the beginning
                    config.putSysConfig(sysConfig);
                }
            }
        }
    }

    /**
     * Allows a subclass of ObservationCB to check to see if we are taking the
     * first step.
     */
    protected boolean _isFirstTime() {
        return _firstTime;
    }

    /**
     * Provides a specialized serialization read method to make sure that
     * transient variables are initialized approriately.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        _firstTime = true;
        _isReset = false;
    }

    public static Map getDefaultSequenceOptions(Map options) {
        // add a calibration provider that will be used for all calculations in the sequence; the same provider
        // has to be used throughout the calculations to make sure async updates have no effect on the sequence
        // (Note that updates will create a new provider so the values for a given provider will never change)
        if (options == null) {
            options = new HashMap();
        }
        if (SeqRepeatCbOptions.getCalibrationProvider(options) == null) {
            SeqRepeatCbOptions.setCalibrationProvider(options, CalibrationProviderHolder.getProvider());
        }
        return options;
    }

}
