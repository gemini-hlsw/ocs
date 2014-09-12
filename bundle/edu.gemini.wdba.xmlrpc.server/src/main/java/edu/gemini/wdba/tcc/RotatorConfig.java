package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;

/**
 *
 */
public class RotatorConfig extends ParamSet {

    private ObservationEnvironment _oe;
    private String _name;

    public RotatorConfig(ObservationEnvironment oe) {
        super("");
        if (oe == null) throw new NullPointerException("Config requires a non-null observation environment");
        _oe = oe;
        ITccInstrumentSupport is = _oe.getInstrumentSupport();
        _name = is.getPositionAngle();
    }

    /**
     * The name to be used in the "value" of the field rotator param
     *
     * @return String that is name
     */
    String getConfigName() {
        return _name;
    }

    /**
     * build will use the <code>(@link ObservationEnvironment}</code> to construct
     * an XML document.
     *
     * @return true if build was successful
     */
    public boolean build() {
        // First check to see if we have Altair and if the cass rotator is fixed
        boolean fixed = false;
        // The name of an optional instrument fixed rotator config
        String fixedName = null;

        if (_oe.isAltair()) {
            InstAltair altair = _oe.getAltairConfig();
            if (altair != null) {
                AltairParams.CassRotator cr = altair.getCassRotator();
                fixed = (cr == AltairParams.CassRotator.FIXED);
            }
        } else {
            //@TODO This special hack is for NICI - SCT-231.  I hope to make a more general solution to this when I have time
            fixedName = _oe.getInstrumentSupport().getFixedRotatorConfigName();
            fixed = fixedName != null;
        }

        // Note that I'm completing this even though at the upper level I'm going to check to see if
        // it's fixed or not.  In the fixed case, I'm just referencing a value in the TCC config files
        if (fixed) {
            if (_oe.isAltair()) {

                _name = TccNames.ALTAIR_FIXED;

                // TODO: cleanup this mess
                // Check TccFieldConfig -- the ParamSet we fill out here
                // is ultimately ignored!  When using Altair with cass rotator
                // fixed, the WDBA just adds a param:
                //         <param name="rotator" value="AltairFixed"/>

                addAttribute(NAME, _name);
                addAttribute(TYPE, TccNames.ROTATOR);

                putParameter(TccNames.IPA, "-90");
                putParameter(TccNames.COSYS, TccNames.AZEL);
            } else {
                // It's some other instrument -- like NICI - fixed but not NICI
                _name = fixedName;

                addAttribute(NAME, _name);
                addAttribute(TYPE, TccNames.ROTATOR);

                putParameter(TccNames.IPA,  _oe.getInstrumentSupport().getPositionAngle());

                // Changed from "AZEL" to "FIXED" at Gely's request:
                //
                // ... I will need a very small modification of the ODB/WDBA.
                // Look like that Chris mayer used  a different term for the
                // new CRCS Fixed, and when we load the program for the OT it
                // will not send the correct CRCS fixed!.
                putParameter(TccNames.COSYS, TccNames.FIXED);
            }
        } else {
            addAttribute(NAME, _name);
            addAttribute(TYPE, TccNames.POSANGLE);

            putParameter(TccNames.IPA, _name);
            putParameter(TccNames.COSYS, TccNames.FK5J2000);
        }

        return true;
    }
}
