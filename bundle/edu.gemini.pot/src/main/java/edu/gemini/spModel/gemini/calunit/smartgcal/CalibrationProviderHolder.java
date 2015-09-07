// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id:$
//
package edu.gemini.spModel.gemini.calunit.smartgcal;

/**
 * A "holder" that allows to centrally access a provider for calibrations. Since most classes of the smartgcal
 * module are not known in this module but the *CB classes need access to a provider for the calibrations we
 * use this as a workaround to have a way to centrally access an object that implements the provider interface.
 */
public class CalibrationProviderHolder {

    private static CalibrationProvider provider = EmptyCalibrationProvider.instance;

    public static void setProvider(CalibrationProvider newProvider) {
        provider = newProvider;
    }

    public static CalibrationProvider getProvider() {
        return provider;
    }
}
