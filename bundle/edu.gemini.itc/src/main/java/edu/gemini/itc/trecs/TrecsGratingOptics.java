package edu.gemini.itc.trecs;

import edu.gemini.itc.shared.GratingOptics;
import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.DatFile;
import edu.gemini.itc.shared.TransmissionElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class TrecsGratingOptics extends GratingOptics {

    public TrecsGratingOptics(final String directory,
                              final String gratingName,
                              final double centralWavelength,
                              final int detectorPixels,
                              final int spectralBinning) throws Exception {

        super(directory, gratingName, "gratings", centralWavelength, detectorPixels, spectralBinning);
    }

    @Override protected int getGratingNumber() {
        int grating_num = 0;

        if (_gratingName.equals(TRecsParameters.LORES10_G5401)) {
            grating_num = TRecsParameters.LORES10;
        } else if (_gratingName.equals(TRecsParameters.LORES20_G5402)) {
            grating_num = TRecsParameters.LORES20;
        } else if (_gratingName.equals(TRecsParameters.HIRES10_G5403)) {
            grating_num = TRecsParameters.HIRES10;
        }
        return grating_num;
    }

    @Override  public double getStart() {
        return _centralWavelength - (_dispersionArray.get(getGratingNumber()) * _detectorPixels / 2) * _spectralBinning;
    }

    @Override  public double getEnd() {
        return _centralWavelength + (_dispersionArray.get(getGratingNumber()) * _detectorPixels / 2) * _spectralBinning;
    }

}
