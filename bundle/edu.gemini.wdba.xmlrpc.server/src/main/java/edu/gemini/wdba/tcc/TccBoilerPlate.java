package edu.gemini.wdba.tcc;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: hgillies
 * Date: Mar 9, 2003
 * Time: 12:53:59 PM
 * To change this template use Options | File Templates.
 */
public class TccBoilerPlate {

    private static final Logger LOG = Logger.getLogger(TccBoilerPlate.class.getName());

    ObservationEnvironment _oe;

    public TccBoilerPlate(ObservationEnvironment oe) {
        if (oe == null) throw new NullPointerException("observation environment can not be null");
        _oe = oe;
    }

    ParamSet[] getBoilerPlate() {

        ParamSet p;
        List<ParamSet> _params = new ArrayList<>();

        LOG.info("North is:" + _oe.isNorth());
        LOG.info("South is:" + _oe.isSouth());

        if (_oe.isNorth()) {
            p = new ParamSet("GMOS5", TccNames.INSTRUMENT);
            p.putParameter(TccNames.IAA, "180");
            p.putParameter(TccNames.ISS_PORT, "5");
            p.putParameter(TccNames.CENTRAL_BAFFLE, "closed");
            p.putParameter(TccNames.DEPLOYABLE_BAFFLE, "visible");
            p.putParameter(TccNames.FOCUS_OFFSET, "0.0");
            p.putParameter(TccNames.NAME, "gmos");
            _params.add(p);

            p = new ParamSet("NIRI1", "instrument");
            p.putParameter(TccNames.IAA, "90");
            p.putParameter(TccNames.ISS_PORT, "1");
            p.putParameter(TccNames.CENTRAL_BAFFLE, "closed");
            p.putParameter(TccNames.DEPLOYABLE_BAFFLE, "near IR");
            p.putParameter(TccNames.FOCUS_OFFSET, "0.0");
            p.putParameter(TccNames.NAME, "niri1");
            _params.add(p);

            p = new ParamSet("AcqCam", "instrument");
            p.putParameter(TccNames.IAA, "180");
            p.putParameter(TccNames.ISS_PORT, "AC");
            p.putParameter(TccNames.CENTRAL_BAFFLE, "closed");
            p.putParameter(TccNames.DEPLOYABLE_BAFFLE, "visible");
            p.putParameter(TccNames.FOCUS_OFFSET, "0.0");
            p.putParameter(TccNames.NAME, "AC");
            _params.add(p);

            p = new ParamSet("GMOS OIWFS", TccNames.WAVELENGTH);
            p.putParameter(TccNames.WAVEL, "0.65");
            _params.add(p);

            p = new ParamSet("NIRI OIWFS", TccNames.WAVELENGTH);
            p.putParameter(TccNames.WAVEL, "1.25");
            _params.add(p);

            p = new ParamSet("NIRIF6P", "instrument");
            p.putParameter(TccNames.IAA, "90");
            p.putParameter(TccNames.ISS_PORT, "3");
            p.putParameter(TccNames.CENTRAL_BAFFLE, "closed");
            p.putParameter(TccNames.DEPLOYABLE_BAFFLE, "near IR");
            p.putParameter(TccNames.FOCUS_OFFSET, "0.0");
            p.putParameter(TccNames.NAME, "nirif6p");
            _params.add(p);

            p = new ParamSet("NIRIF14P", "instrument");
            p.putParameter(TccNames.IAA, "90");
            p.putParameter(TccNames.ISS_PORT, "3");
            p.putParameter(TccNames.CENTRAL_BAFFLE, "closed");
            p.putParameter(TccNames.DEPLOYABLE_BAFFLE, "near IR");
            p.putParameter(TccNames.FOCUS_OFFSET, "0.0");
            p.putParameter(TccNames.NAME, "nirif14p");
            _params.add(p);

            p = new ParamSet("NIRIF32P", "instrument");
            p.putParameter(TccNames.IAA, "90");
            p.putParameter(TccNames.ISS_PORT, "3");
            p.putParameter(TccNames.CENTRAL_BAFFLE, "closed");
            p.putParameter(TccNames.DEPLOYABLE_BAFFLE, "near IR");
            p.putParameter(TccNames.FOCUS_OFFSET, "0.0");
            p.putParameter(TccNames.NAME, "nirif32p");
            _params.add(p);
        }

        // South
        if (_oe.isSouth()) {
            /*
            p = new ParamSet("GMOS3", TccNames.INSTRUMENT);
            p.putParameter(TccNames.IAA, "180");
            p.putParameter(TccNames.ISS_PORT, "3");
            p.putParameter(TccNames.CENTRAL_BAFFLE, "closed");
            p.putParameter(TccNames.DEPLOYABLE_BAFFLE, "visible");
            p.putParameter(TccNames.FOCUS_OFFSET, "0.0");
            p.putParameter(TccNames.NAME, "gmos3");
            _params.add(p);

            p = new ParamSet("AcqCam", "instrument");
            p.putParameter(TccNames.IAA, "180");
            p.putParameter(TccNames.ISS_PORT, "AC");
            p.putParameter(TccNames.CENTRAL_BAFFLE, "closed");
            p.putParameter(TccNames.DEPLOYABLE_BAFFLE, "visible");
            p.putParameter(TccNames.FOCUS_OFFSET, "0.0");
            p.putParameter(TccNames.NAME, "AC");
            _params.add(p);

            p = new ParamSet("PHO5", "instrument");
            p.putParameter(TccNames.IAA, "180");
            p.putParameter(TccNames.ISS_PORT, "AC");
            p.putParameter(TccNames.CENTRAL_BAFFLE, "closed");
            p.putParameter(TccNames.DEPLOYABLE_BAFFLE, "visible");
            p.putParameter(TccNames.FOCUS_OFFSET, "0.0");
            p.putParameter(TccNames.NAME, "pho5");
            _params.add(p);
*/
        }

        ParamSet[] _ps = new ParamSet[_params.size()];

        return _params.toArray(_ps);

    }

}
