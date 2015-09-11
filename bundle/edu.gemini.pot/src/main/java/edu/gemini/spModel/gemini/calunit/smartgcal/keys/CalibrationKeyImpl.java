// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id:$
//
package edu.gemini.spModel.gemini.calunit.smartgcal.keys;

import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKey;

/**
 * Implementation for keys that are used to do the lookup of calibrations in the calibration maps.
 * The main part is an instrument configuration that represents a set of instrument settings which are
 * relevant to know the corresponding calibration unit settings.
 * <p/>
 * In addition to the instrument configuration for some instruments additional parameters like the central
 * wavelength are also important for the lookup but are not part of the actual instrument configuration
 * representation since for different instrument configurations they may map to different ranges.
 * The translation of these additional (range) values in combination with the instrument configuration
 * is done by the corresponding calibration map
 * (e.g. @see edu.gemini.spModel.gemini.calunit.smartgcal.maps.CentralWavelengthMap).
 */
public class CalibrationKeyImpl implements CalibrationKey {

    private final ConfigurationKey config;

    public CalibrationKeyImpl(ConfigurationKey config) {
        this.config = config;
    }

    @Override
    public ConfigurationKey getConfig() {
        return config;
    }


    public static class WithWavelength extends CalibrationKeyImpl implements CalibrationKey.WithWavelength {

        private final Double wavelength;

        public WithWavelength(ConfigurationKey config, Double wavelength) {
            super(config);
            this.wavelength = wavelength;
        }

        @Override
        public Double getWavelength() {
            return wavelength;
        }

    }

}
