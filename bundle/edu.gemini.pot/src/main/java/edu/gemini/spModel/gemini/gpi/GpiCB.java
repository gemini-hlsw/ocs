package edu.gemini.spModel.gemini.gpi;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.core.Magnitude;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.util.SPTreeUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The configuration builder for the Gpi data object.
 */
public class GpiCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public GpiCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    @Override
    public Object clone() {
        GpiCB result = (GpiCB) super.clone();
        result._sysConfig = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        Gpi dataObj = (Gpi) getDataObject();
        if (dataObj == null)
            throw new IllegalArgumentException("The data objectfor Gpi can not be null");
        _sysConfig = dataObj.getSysConfig();
    }

    @Override
    protected boolean thisHasConfiguration() {
        if (_sysConfig == null)
            return false;
        return (_sysConfig.getParameterCount() > 0);
    }

    @Override
    protected void thisApplyNext(final IConfig config, final IConfig prevFull) {
        final String systemName = _sysConfig.getSystemName();
        final Collection<IParameter> sysConfig = _sysConfig.getParameters();

        for (final IParameter param : sysConfig) {
            config.putParameter(systemName,
                    DefaultParameter.getInstance(param.getName(), param.getValue()));
        }
        config.putParameter(systemName,
                StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP,
                        Gpi.INSTRUMENT_NAME_PROP));

        // Set the magnitude properties.
        final ISPObsComponent targetcomp = SPTreeUtil.findTargetEnvNode(getObsComponent().getContextObservation());
        if (targetcomp != null) {
            final TargetObsComp toc = (TargetObsComp) targetcomp.getDataObject();
            if (toc != null) {
                // The Asterism here should be Single(base), so we only need to concern ourselves with the head target.
                toc.getAsterism().allSpTargetsJava().headOption().foreach(base -> {
                    final ImList<Magnitude> magnitudes = base.getMagnitudesJava();
                    MAG_PROPERTIES.forEach((band, propName) ->
                        magnitudes.find(m -> m.band().equals(band)).
                                foreach(mag -> config.putParameter(systemName, DefaultParameter.getInstance(propName, mag.value()))));
                });
            }
        }
    }

    // The magnitude bands of interest and their property names in the configuration.
    private static final Map<MagnitudeBand, String> MAG_PROPERTIES = new HashMap<>();
    static {
        MAG_PROPERTIES.put(MagnitudeBand.H$.MODULE$, Gpi.MAG_H_PROP);
        MAG_PROPERTIES.put(MagnitudeBand.I$.MODULE$, Gpi.MAG_I_PROP);
    }
}
