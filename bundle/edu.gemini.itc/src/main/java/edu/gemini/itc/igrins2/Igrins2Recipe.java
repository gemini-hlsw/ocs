package edu.gemini.itc.igrins2;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import scala.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


// This class performs the calculations for IGRINS2
public final class Igrins2Recipe {
    private static final Logger Log = Logger.getLogger(Igrins2Recipe.class.getName());
    private final ItcParameters p;
    private final Igrins2[] _mainInstrument;
    private final Igrins2Parameters igrins2Parameters;
    private final ObservingConditions _obsConditionParameters;
    private final ObservationDetails _obsDetailParameters;
    private final SourceDefinition _sdParameters;
    private final TelescopeDetails _telescope;


    // Construct an Igrins2Recipe
    public Igrins2Recipe(final ItcParameters p, final Igrins2Parameters instr)
    {
        this.p                  = p;
        _mainInstrument         = createIgrins2(instr, p.observation());
        igrins2Parameters       = instr;
        _sdParameters           = p.source();
        _obsDetailParameters    = p.observation();
        _obsConditionParameters = p.conditions();
        _telescope              = p.telescope();
        validateInputParameters();
    }

    private Igrins2[] createIgrins2(final Igrins2Parameters parameters, final ObservationDetails observationDetails) {
        return new Igrins2[] {new Igrins2(parameters, observationDetails, Igrins2Arm.H),
                              new Igrins2(parameters, observationDetails, Igrins2Arm.K)};
    }

    private void validateInputParameters() {
        if (_obsDetailParameters.exposureTime() > 600) {
            throw new IllegalArgumentException("The maximum exposure time is 600s.");
        }
        Validation.validate(_mainInstrument[0], _obsDetailParameters, _sdParameters);  // other general validations
    }


    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult[] result, final boolean headless) {
        final List<List<SpcChartData>> groups = new ArrayList<>();
        for (SpectroscopyResult r : result){
            final List<SpcChartData> dataSets = new ArrayList<SpcChartData>();
            String b = ((Igrins2) r.instrument()).getWaveBand();
            dataSets.add(Recipe$.MODULE$.createSignalChart(r,
                    "Signal and Sqrt(Background) in 1 pixel: " + b,0));
            dataSets.add(Recipe$.MODULE$.createS2NChart(r,
                    "Intermediate Single-Exposure S/N and Final S/N with Sky Sub: " + b,0));
            groups.add(dataSets);
        }
        return Recipe$.MODULE$.serviceGroupedResult(result, groups, headless);
    }


    public SpectroscopyResult[] calculateSpectroscopy() {
        int length = _mainInstrument.length;
        final SpectroscopyResult[] results = new SpectroscopyResult[length];
        for (int i = 0; i < length; i++) {
            results[i] = calculateSpectroscopy(_mainInstrument[i]);
        }
        return results;
    }


    private SpectroscopyResult calculateSpectroscopy(Igrins2 instrument) {

        Log.fine("Calculating from " + instrument.getWavelengthStart() + " to " + instrument.getWavelengthEnd() + " nm");

        // Module 1b
        // Define the source energy (as function of wavelength).
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifted SED

        Log.fine("Calculating image quality...");
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        final Option<AOSystem> altair;
        if (igrins2Parameters.altair().isDefined()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(),
                    IQcalc.getImageQuality(), _obsConditionParameters.ccExtinction(), igrins2Parameters.altair().get(), 0.0);
            altair = Option.apply(ao);
        } else {
            altair = Option.empty();
        }

        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope, altair);

        // Module 1a
        // The purpose of this section is to calculate the fraction of the source flux contained within an aperture
        // adopted to derive the signal-to-noise ratio. There are several cases depending on the source morphology.
        // Define the source morphology
        // inputs: source morphology specification

        final double im_qual = altair.isDefined() ? altair.get().getAOCorrectedFWHM() : IQcalc.getImageQuality();

        // We are bypassing morphology modules 3a-5a. i.e. the output morphology is same as the input morphology.

        final Slit slit = Slit$.MODULE$.apply(_sdParameters, _obsDetailParameters, instrument, instrument.getSlitWidth(), IQcalc.getImageQuality());
        final SlitThroughput throughput = new SlitThroughput(_sdParameters, slit, im_qual);

        Log.fine("Creating specS2N...");
        final SpecS2NSlitVisitor specS2N = new SpecS2NSlitVisitor(
                slit,
                instrument.disperser.get(),
                throughput,
                instrument.getSpectralPixelWidth(),
                instrument.getWavelengthStart(),
                instrument.getWavelengthEnd(),
                im_qual,
                instrument.getReadNoise(),
                instrument.getDarkCurrent(),
                _obsDetailParameters);

        Log.fine("Setting source spectrum...");
        specS2N.setSourceSpectrum(calcSource.sed);
        specS2N.setBackgroundSpectrum(calcSource.sky);
        calcSource.sed.accept(specS2N);

        final SpecS2N[] specS2Narr = new SpecS2N[]{specS2N};
        return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, throughput.throughput(), altair, Option.empty());
    }

}
