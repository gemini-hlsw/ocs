package edu.gemini.itc.base;

import edu.gemini.spModel.core.Redshift;

/**
 * This class creates a PowerLaw spectrum over the interval defined by the
 * blocking filter.  The code comes from PPuxley's Mathcad Demo
 * This Class implements Visitable sampled specturm to create the sed.
 */

public class PowerLawSpectrum extends DefaultSampledSpectrum {

    public PowerLawSpectrum(final double powerLawIndex, final double start, final double end, final double interval, final Redshift redshift) {

        super(new double[1], 0.0, 1.0);  // create a dummy Default Spectrum.  Will be

        final double _sampling = interval;

        double _start = 0.2 * start;//.8
        double _end = 1.8 * end;//1.2

        //shift start and end depending on redshift
        final double z = redshift.z();
        _start /= (1 + z);
        _end /= (1 + z);

        final int n = (int) ((_end - _start) / _sampling + 1);
        double[] fluxArray = new double[n];
        for (int i = 0; i < n ; i++) {
            double lam = _start + i * _sampling;
            fluxArray[i] = _pLawFlux(lam, powerLawIndex);
        }

        reset(fluxArray, _start, _sampling);

    }


    private double _pLawFlux(double lambda, double powerLawIndex) {
        //this funtion will calculate the eline spectum for a given wavelen
        // and sigme (specified by the user. The flux is just the line flux
        // of the object in question.  The units are returned internal units.
        // That is good so we dont have to do any thing to the result.

        double returnFlux;

        returnFlux = Math.pow(lambda, powerLawIndex);
        return returnFlux;
    }

}
