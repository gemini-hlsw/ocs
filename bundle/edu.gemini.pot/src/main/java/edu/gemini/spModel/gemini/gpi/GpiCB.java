package edu.gemini.spModel.gemini.gpi;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.shared.util.immutable.Function1;
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
import java.util.Map;

/**
 * The configuration builder for the Gpi data object.
 */
public class GpiCB extends AbstractObsComponentCB {

    private transient ISysConfig _sysConfig;

    public GpiCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

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

    protected boolean thisHasConfiguration() {
        if (_sysConfig == null)
            return false;
        return (_sysConfig.getParameterCount() > 0);
    }

    protected void thisApplyNext(final IConfig config, IConfig prevFull) {
        final String systemName = _sysConfig.getSystemName();
        Collection<IParameter> sysConfig = _sysConfig.getParameters();

        final class MagnitudeFilter implements Function1<Magnitude, Boolean> {
            private final MagnitudeBand band;

            private MagnitudeFilter(MagnitudeBand band) {
                this.band = band;
            }

            @Override
            public Boolean apply(Magnitude magnitude) {
                return magnitude.band().equals(band);
            }
        }

        final class MagnitudeSetter implements ApplyOp<Magnitude> {
            private final String magProp;

            private MagnitudeSetter(String magHProp) {
                this.magProp = magHProp;
            }

            @Override
            public void apply(Magnitude magnitude) {
                config.putParameter(systemName,
                            DefaultParameter.getInstance(magProp,
                                magnitude.value()));
            }
        }
        for (IParameter param : sysConfig) {
            config.putParameter(systemName,
                    DefaultParameter.getInstance(param.getName(), param.getValue()));
        }
        config.putParameter(systemName,
                            StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP,
                                    Gpi.INSTRUMENT_NAME_PROP));
    }

}
