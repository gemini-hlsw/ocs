package edu.gemini.spModel.gemini.gnirs;

import edu.gemini.pot.sp.ISPSeqComponent;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;

import static edu.gemini.spModel.obscomp.InstConstants.INSTRUMENT_NAME_PROP;
import static edu.gemini.spModel.obscomp.InstConstants.OBSERVING_WAVELENGTH_PROP;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME;
import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import static edu.gemini.spModel.gemini.gnirs.GNIRSConstants.ACQUISITION_MIRROR_PROP;
import static edu.gemini.spModel.gemini.gnirs.GNIRSConstants.CENTRAL_WAVELENGTH_PROP;
import static edu.gemini.spModel.gemini.gnirs.GNIRSConstants.FILTER_PROP;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.AcquisitionMirror;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.Filter;
import edu.gemini.spModel.util.SPTreeUtil;

import java.util.Map;

/**
 * A configuration builder for the GNIRS iterator.
 */
public final class SeqConfigGNIRSCB extends HelperSeqCompCB {

    /**
     * Constructor for creating this seq comp CB.
     */
    public SeqConfigGNIRSCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    private boolean overrideAcqObsWavelength = false;

    @Override
    protected void thisReset(Map<String, Object> options) {

        // Pre 2017A programs are treated differently when it comes to
        // calculating the observing wavelength.  The `overrideAcqObsWavelength`
        // flag kept in the static component tells us which way to go so we
        // record its value upon reset.
        overrideAcqObsWavelength =
            ImOption.apply(getSeqComponent())
                    .flatMap(c -> ImOption.apply(c.getContextObservation()))
                    .flatMap(o -> ImOption.apply(SPTreeUtil.findInstrument(o)))
                    .flatMap(i -> ImOption.apply(i.getDataObject()))
                    .filter(d -> d instanceof InstGNIRS)
                    .map(d -> ((InstGNIRS) d).isOverrideAcqObsWavelength())
                    .getOrElse(true);

        super.thisReset(options);
    }

    private static Option<Object> getInstrumentParameterValue(String n, Option<IConfig> oc) {
        return oc.flatMap(c -> ImOption.apply(c.getSysConfig(INSTRUMENT_CONFIG_NAME)))
                 .flatMap(s -> ImOption.apply(s.getParameterValue(n)));
    }

    private static Option<Object> getInstrumentParameterValue(String n, Option<IConfig> oc, Option<IConfig> op) {
        return getInstrumentParameterValue(n, oc)
                 .orElse(() -> getInstrumentParameterValue(n, op));
    }

    private static Option<Object> getInstrumentParameterValue(String n, IConfig c, IConfig p) {
        return getInstrumentParameterValue(n, ImOption.apply(c), ImOption.apply(p));
    }

    private static AcquisitionMirror getAcquisitionMirror(IConfig c, IConfig p) {
        return getInstrumentParameterValue(ACQUISITION_MIRROR_PROP, c, p)
                 .filter(o -> o instanceof AcquisitionMirror)
                 .map(o -> (AcquisitionMirror) o)
                 .getOrElse(AcquisitionMirror.OUT);
    }

    private static Option<Filter> getFilter(IConfig c, IConfig p) {
        return getInstrumentParameterValue(FILTER_PROP, c, p)
                 .filter(o -> o instanceof Filter)
                 .map(o -> (Filter) o);
    }

    private static Option<String> getObservingWavelength(IConfig c, IConfig p) {
        return getInstrumentParameterValue(OBSERVING_WAVELENGTH_PROP, c, p)
                 .filter(o -> o instanceof String)
                 .map(o -> (String) o);
    }

    private static Option<String> getCentralWavelength(IConfig c, IConfig p) {
        return getInstrumentParameterValue(CENTRAL_WAVELENGTH_PROP, c, p)
                 .filter(o -> o instanceof String)
                 .map(o -> (String) o);
    }

    // Override the observing wavelength for acquisition steps.  It must
    // match the filter wavelength in this case (unless this is an old
    // executed pre-2017A observation in which case we must continue to
    // use the old method for calculating the observing wavelength).

    private void addObservingWavelength(IConfig c, IConfig p) {

        // Get the filter wavelength if there is a filter and if this is an
        // acquisition.
        final Option<String> fLambda;
        if (overrideAcqObsWavelength && getAcquisitionMirror(c, p) == AcquisitionMirror.IN) {
            fLambda = getFilter(c, p)
                        .flatMap(f -> ImOption.apply(f.wavelength()))
                        .map(d -> String.format("%.2f", d));
        } else {
            fLambda = ImOption.<String>empty();
        }

        // The observing wavelength is the filter wavelength if defined at this
        // point, or else the central wavelength.
        final Option<String> obsLambda = fLambda.orElse(() -> getCentralWavelength(c, p));

        // If the observing wavelength value is changing in this step, add it to
        // the current configuration.
        if (!obsLambda.equals(getObservingWavelength(c, p))) {
            obsLambda.foreach(l ->
                ImOption.apply(c).foreach(c0 ->
                    c0.putParameter(
                        INSTRUMENT_CONFIG_NAME,
                        StringParameter.getInstance(OBSERVING_WAVELENGTH_PROP, l)
                    )
                )
            );
        }
    }

    /**
     * This thisApplyNext overrides the HelperSeqCompCB
     * so that the integration time, exposure time and ncoadds can
     * be inserting in the observe system.
     */
    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        super.thisApplyNext(config, prevFull);

        // Insert the instrument name
        config.putParameter(
            INSTRUMENT_CONFIG_NAME,
            StringParameter.getInstance(INSTRUMENT_NAME_PROP, GNIRSConstants.INSTRUMENT_NAME_PROP)
        );

        addObservingWavelength(config, prevFull);
    }
}
