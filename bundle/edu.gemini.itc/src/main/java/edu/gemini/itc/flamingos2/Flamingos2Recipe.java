package edu.gemini.itc.flamingos2;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.parameters.*;
import edu.gemini.itc.shared.*;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.util.Calendar;

/**
 * This class performs the calculations for Flamingos 2 used for imaging.
 */
public final class Flamingos2Recipe extends RecipeBase {

	private Flamingos2Parameters _flamingos2Parameters;
	private String _header = new StringBuffer("# Flamingos-2 ITC: "
			+ Calendar.getInstance().getTime() + "\n").toString();
	private ObservingConditionParameters _obsConditionParameters;
	private ObservationDetailsParameters _obsDetailParameters;
	private PlottingDetailsParameters _plotParameters;
	// Parameters from the web page.
	private SourceDefinitionParameters _sdParameters;

	private TeleParameters _teleParameters;

	/**
	 * Constructs an Flamingos2 object by parsing servlet request.
	 * 
	 * @param r
	 *            Servlet request containing form data from ITC web page.
	 * @param out
	 *            Results will be written to this PrintWriter.
	 * @throws Exception
	 *             on failure to parse parameters.
	 */
	public Flamingos2Recipe(HttpServletRequest r, PrintWriter out)
			throws Exception {
		super(out);

		// Read parameters from the four main sections of the web page.
		_sdParameters = new SourceDefinitionParameters(r);
		_obsDetailParameters = new ObservationDetailsParameters(r);
		_obsConditionParameters = new ObservingConditionParameters(r);
		_flamingos2Parameters = new Flamingos2Parameters(r);
		_teleParameters = new TeleParameters(r);
		_plotParameters = new PlottingDetailsParameters(r);
	}

	/**
	 * Constructs an Flamingos 2 object by parsing a Multi part servlet request.
	 * 
	 * @param r
	 *            Servlet request containing form data from ITC web page.
	 * @param out
	 *            Results will be written to this PrintWriter.
	 * @throws Exception
	 *             on failure to parse parameters.
	 */
	public Flamingos2Recipe(ITCMultiPartParser r, PrintWriter out)
			throws Exception {
		super(out);

		// Read parameters from the four main sections of the web page.
		_sdParameters = new SourceDefinitionParameters(r);
		_obsDetailParameters = new ObservationDetailsParameters(r);
		_obsConditionParameters = new ObservingConditionParameters(r);
		_flamingos2Parameters = new Flamingos2Parameters(r);
		_teleParameters = new TeleParameters(r);
		_plotParameters = new PlottingDetailsParameters(r);
	}

	/**
	 * Constructs an Flamingos 2 object given the parameters. Useful for
	 * testing.
	 */
	public Flamingos2Recipe(SourceDefinitionParameters sdParameters,
			ObservationDetailsParameters obsDetailParameters,
			ObservingConditionParameters obsConditionParameters,
			Flamingos2Parameters flamingos2Parameters,
			TeleParameters teleParameters,
			PlottingDetailsParameters plotParameters,
			PrintWriter out) {
		super(out);
		_sdParameters = sdParameters;
		_obsDetailParameters = obsDetailParameters;
		_obsConditionParameters = obsConditionParameters;
		_flamingos2Parameters = flamingos2Parameters;
		_teleParameters = teleParameters;
		_plotParameters = plotParameters;
	}

	/**
	 * Check input parameters for consistency
	 * 
	 * @throws Exception
	 */
	public void checkInputParameters() throws Exception {
		if (_obsDetailParameters.getCalculationMode().equals(
				ObservationDetailsParameters.SPECTROSCOPY)) {
			if (_flamingos2Parameters.getGrism().equalsIgnoreCase("none")) {
				throw new Exception(
						"In spectroscopy mode, a grism must be selected");
			}
			if (_flamingos2Parameters.getFPMask().equalsIgnoreCase("none")) {
				throw new Exception(
						"In spectroscopy mode, a FP must must be selected");
			}
		}
	}

	/**
	 * Performes recipe calculation and writes results to a cached PrintWriter
	 * or to System.out.
	 * 
	 * @throws Exception
	 *             A recipe calculation can fail in many ways, missing data
	 *             files, incorrectly-formatted data files, ...
	 */
	@Override
	public void writeOutput() throws Exception {
		// This object is used to format numerical strings.
		FormatStringWriter device = new FormatStringWriter();
		device.setPrecision(2); // Two decimal places
		device.clear();
		_println("");
		// For debugging, to be removed later
		// _print("<pre>" + _sdParameters.toString() + "</pre>");
		// _print("<pre>" + _flamingos2Parameters.toString() + "</pre>");
		// _print("<pre>" + _obsDetailParameters.toString() + "</pre>");
		// _print("<pre>" + _obsConditionParameters.toString() + "</pre>");

		// Module 1b
		// Define the source energy (as function of wavelength).
		//

		checkInputParameters();

		Flamingos2 instrument = new Flamingos2(_flamingos2Parameters);

		// Get Source spectrum from factory
		VisitableSampledSpectrum halo;
		VisitableSampledSpectrum sed = SEDFactory.getSED(_sdParameters,
				instrument);

		// Apply redshift if needed
		SampledSpectrumVisitor redshift = new RedshiftVisitor(
				_sdParameters.getRedshift());
		sed.accept(redshift);

		// Must check to see if the redshift has moved the spectrum beyond
		// useful range. The shifted spectrum must completely overlap
		// both the normalization waveband and the observation waveband
		// (filter region).

		String band = _sdParameters.getNormBand();
		double start = WavebandDefinition.getStart(band);
		double end = WavebandDefinition.getEnd(band);
		// System.out.println("WStart:" + start + "SStart:" +sed.getStart());
		// System.out.println("WEnd:" + end + "SEnd:" +sed.getEnd());
		// System.out.println("OStart:" + instrument.getObservingStart() +
		// "OEnd:" +instrument.getObservingEnd());

		// any sed except BBODY and ELINE have normailization regions
		if (!(_sdParameters.getSpectrumResource().equals(
				SourceDefinitionParameters.ELINE) || _sdParameters
				.getSpectrumResource().equals(SourceDefinitionParameters.BBODY))) {
			if (sed.getStart() > start || sed.getEnd() < end) {
				throw new Exception(
						"Shifted spectrum lies outside of specified normalisation waveband.");
			}
		}

		if (sed.getStart() > instrument.getObservingStart()
				|| sed.getEnd() < instrument.getObservingEnd()) {
			_println(" Sed start " + sed.getStart()
					+ " > than instrument start "
					+ instrument.getObservingStart());
			_println(" Sed end " + sed.getEnd() + " < than instrument end "
					+ instrument.getObservingEnd());

			throw new Exception(
					"Shifted spectrum lies outside of observed wavelengths");
		}

		// Module 2
		// Convert input into standard internally-used units.
		//
		// inputs: instrument,redshifted SED, waveband, normalization flux,
		// units
		// calculates: normalized SED, resampled SED, SED adjusted for aperture
		// output: SED in common internal units
		SampledSpectrumVisitor norm = new NormalizeVisitor(
				_sdParameters.getNormBand(),
				_sdParameters.getSourceNormalization(),
				_sdParameters.getUnits());
		if (!_sdParameters.getSpectrumResource().equals(
				SourceDefinitionParameters.ELINE)) {
			sed.accept(norm);
		}

		// Resample the spectra for efficiency
		SampledSpectrumVisitor resample = new ResampleVisitor(
				instrument.getObservingStart(), instrument.getObservingEnd(),
				instrument.getSampling());
		// sed.accept(resample);

		// Create and apply Telescope aperture visitor
		SampledSpectrumVisitor tel = new TelescopeApertureVisitor();
		sed.accept(tel);

		// SED is now in units of photons/s/nm

		// Module 3b
		// The atmosphere and telescope modify the spectrum and
		// produce a background spectrum.
		//
		// inputs: SED, AIRMASS, sky emmision file, mirror configuration,
		// output: SED and sky background as they arrive at instruments

		SampledSpectrumVisitor atmos = new AtmosphereVisitor(
				_obsConditionParameters.getAirmass());
		// sed.accept(atmos);

		SampledSpectrumVisitor clouds = CloudTransmissionVisitor.create(
				_obsConditionParameters.getSkyTransparencyCloud());
		sed.accept(clouds);

		SampledSpectrumVisitor water = WaterTransmissionVisitor.create(
				_obsConditionParameters.getSkyTransparencyWater(),
				_obsConditionParameters.getAirmass(), "nearIR_trans_",
				ITCConstants.MAUNA_KEA, ITCConstants.NEAR_IR);
		sed.accept(water);

		// Background spectrum is introduced here.
		VisitableSampledSpectrum sky = SEDFactory.getSED(
				ITCConstants.SKY_BACKGROUND_LIB + "/"
						+ ITCConstants.NEAR_IR_SKY_BACKGROUND_FILENAME_BASE
						+ "_"
						+ _obsConditionParameters.getSkyTransparencyWaterCategory() // REL-557
						+ "_" + _obsConditionParameters.getAirmassCategory()
						+ ITCConstants.DATA_SUFFIX, instrument.getSampling());

		// Create and Add Background for the tele

		SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(
				_teleParameters.getMirrorCoating(),
				_teleParameters.getInstrumentPort(), ITCConstants.CERRO_PACHON,
				ITCConstants.NEAR_IR);
		sky.accept(tb);

		// Apply telescope transmission
		SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(
				_teleParameters.getMirrorCoating(),
				_teleParameters.getInstrumentPort());

		sed.accept(t);
		sky.accept(t);

		sky.accept(tel);

		// Add instrument background to sky background for a total background.
		// At this point "sky" is not the right name

		instrument.addBackground(sky);

		// Module 4 AO module not implemented
		// The AO module affects source and background SEDs.

		// Module 5b
		// The instrument with its detectors modifies the source and
		// background spectra.
		// input: instrument, source and background SED
		// output: total flux of source and background.

		instrument.convolveComponents(sed);
		instrument.convolveComponents(sky);

		// ITCPlot plot2 = new ITCPlot(sky.getDataSource());
		// plot2.addDataSource(sed.getDataSource());
		// plot2.disp();

		double sed_integral = sed.getIntegral();
		double sky_integral = sky.getIntegral();
		double halo_integral = 0;

		// For debugging, print the spectrum integrals.
		// _println("SED integral: "+sed_integral+"\tSKY integral: "+sky_integral);

		// End of the Spectral energy distribution portion of the ITC.

		// Start of morphology section of ITC

		// Module 1a
		// The purpose of this section is to calculate the fraction of the
		// source flux which is contained within an aperture which we adopt
		// to derive the signal to noise ratio. There are several cases
		// depending on the source morphology.
		// Define the source morphology
		//
		// inputs: source morphology specification

		String ap_type = _obsDetailParameters.getApertureType();
		double pixel_size = instrument.getPixelSize();
		double ap_diam = 0;
		double source_fraction = 0;
		double halo_source_fraction = 0;
		double pix_per_sq_arcsec = 0;
		double peak_pixel_count = 0;

		// Calculate image quality
		double im_qual = 0.;
		double uncorrected_im_qual = 0.;

		ImageQualityCalculatable IQcalc =
				ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _teleParameters, instrument);
		IQcalc.calculate();

		im_qual = IQcalc.getImageQuality();

		// Calculate Source fraction
		SourceFractionCalculatable SFcalc =
				SourceFractionCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);

		SFcalc.setImageQuality(im_qual);
		SFcalc.calculate();
		_print(SFcalc.getTextResult(device));
		_println(IQcalc.getTextResult(device));

		// Calculate the Peak Pixel Flux
		PeakPixelFluxCalc ppfc;

		if (_sdParameters.getSourceGeometry().equals(
				SourceDefinitionParameters.POINT_SOURCE)
				|| _sdParameters.getExtendedSourceType().equals(
						SourceDefinitionParameters.GAUSSIAN)) {

			ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
					_obsDetailParameters.getExposureTime(), sed_integral,
					sky_integral, instrument.getDarkCurrent());

			peak_pixel_count = ppfc.getFluxInPeakPixel();

		} else if (_sdParameters.getExtendedSourceType().equals(
				SourceDefinitionParameters.UNIFORM)) {
			double usbApArea = 0;
			ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
					_obsDetailParameters.getExposureTime(), sed_integral,
					sky_integral, instrument.getDarkCurrent());
			peak_pixel_count = ppfc.getFluxInPeakPixelUSB(
					SFcalc.getSourceFraction(), SFcalc.getNPix());
		} else {
			throw new Exception(
					"Peak Pixel Flux could not be calculated for type"
							+ _sdParameters.getSourceGeometry());
		}

		// In this version we are bypassing morphology modules 3a-5a.
		// i.e. the output morphology is same as the input morphology.
		// Might implement these modules at a later time.

		int number_exposures = _obsDetailParameters.getNumExposures();
		double frac_with_source = _obsDetailParameters.getSourceFraction();

		// report error if this does not come out to be an integer
		checkSourceFraction(number_exposures, frac_with_source);

		double exposure_time = _obsDetailParameters.getExposureTime();
		double dark_current = instrument.getDarkCurrent();
		double read_noise = instrument.getReadNoise();

		if (_obsDetailParameters.getCalculationMode().equals(
				ObservationDetailsParameters.SPECTROSCOPY)) {

			String sigSpec, backSpec, singleS2N, finalS2N;
			//SpecS2NVisitor specS2N;
			SpecS2NLargeSlitVisitor specS2N;
			SlitThroughput st;
			SlitThroughput st_halo;
			ITCChart chart = new ITCChart();

			if (ap_type.equals(ObservationDetailsParameters.USER_APER)) {
				st = new SlitThroughput(im_qual,
						_obsDetailParameters.getApertureDiameter(), pixel_size,
						_flamingos2Parameters.getSlitSize()*pixel_size);

				st_halo = new SlitThroughput(uncorrected_im_qual,
						_obsDetailParameters.getApertureDiameter(), pixel_size,
						_flamingos2Parameters.getSlitSize()*pixel_size);

				_println("software aperture extent along slit = "
						+ device.toString(_obsDetailParameters
								.getApertureDiameter()) + " arcsec");
			} else {
				st = new SlitThroughput(im_qual, pixel_size,
						_flamingos2Parameters.getSlitSize()*pixel_size);

				st_halo = new SlitThroughput(uncorrected_im_qual, pixel_size,
						_flamingos2Parameters.getSlitSize()*pixel_size);
				
				if (_sdParameters.getSourceGeometry().equals(
						SourceDefinitionParameters.EXTENDED_SOURCE)) {
					if (_sdParameters.getExtendedSourceType().equals(
							SourceDefinitionParameters.UNIFORM)) {
						_println("software aperture extent along slit = "
								+ device.toString(1 / _flamingos2Parameters
										.getSlitSize()*pixel_size) + " arcsec");
					}
				} else {
					_println("software aperture extent along slit = "
							+ device.toString(1.4 * im_qual) + " arcsec");
				}
			}

			if (_sdParameters.getSourceGeometry().equals(
					SourceDefinitionParameters.POINT_SOURCE)
					|| _sdParameters.getExtendedSourceType().equals(
							SourceDefinitionParameters.GAUSSIAN)) {
				_println("fraction of source flux in aperture = "
						+ device.toString(st.getSlitThroughput()));
			}

			_println("derived image size(FWHM) for a point source = "
					+ device.toString(im_qual) + " arcsec");

			_println("");
			_println("Requested total integration time = "
					+ device.toString(exposure_time * number_exposures)
					+ " secs, of which "
					+ device.toString(exposure_time * number_exposures
							* frac_with_source) + " secs is on source.");

			_print("<HR align=left SIZE=3>");
			// System.out.println(" im_qual: " + im_qual + " " + pixel_size);
			ap_diam = st.getSpatialPix();
			double spec_source_frac = st.getSlitThroughput();

			if (_plotParameters.getPlotLimits().equals(
					PlottingDetailsParameters.USER_LIMITS)) {
				chart.setDomainMinMax(_plotParameters.getPlotWaveL(),
						_plotParameters.getPlotWaveU());
			} else {
				chart.autoscale();
			}

			if (_sdParameters.getSourceGeometry().equals(
					SourceDefinitionParameters.EXTENDED_SOURCE)) {
				if (_sdParameters.getExtendedSourceType().equals(
						SourceDefinitionParameters.UNIFORM)) {
					// im_qual=10000;

					if (ap_type.equals(ObservationDetailsParameters.USER_APER)) {
						spec_source_frac = _flamingos2Parameters.getSlitSize()*pixel_size
								* ap_diam * pixel_size; // ap_diam = Spec_NPix
					} else if (ap_type
							.equals(ObservationDetailsParameters.AUTO_APER)) {
						ap_diam = new Double(
								1 / (_flamingos2Parameters.getSlitSize() * pixel_size) + 0.5)
								.intValue();
						spec_source_frac = 1;
					}
				}
			}

			final double gratDispersion_nmppix = instrument.getSpectralPixelWidth();
			final double gratDispersion_nm = 0.5 / pixel_size * gratDispersion_nmppix;

            specS2N = new SpecS2NLargeSlitVisitor(_flamingos2Parameters.getSlitSize()*pixel_size,
					pixel_size, instrument.getSpectralPixelWidth(),
					instrument.getObservingStart(),
					instrument.getObservingEnd(),
					gratDispersion_nm,
					gratDispersion_nmppix,
                    instrument.getGrismResolution(), spec_source_frac, im_qual,
					ap_diam, number_exposures, frac_with_source, exposure_time,
					dark_current, read_noise,
                    _obsDetailParameters.getSkyApertureDiameter(), 1);

            specS2N.setDetectorTransmission(instrument.getDetectorTransmision());
            specS2N.setSourceSpectrum(sed);
			specS2N.setBackgroundSpectrum(sky);
			specS2N.setSpecHaloSourceFraction(0.0);

			sed.accept(specS2N);
			_println("<p style=\"page-break-inside: never\">");
            chart.addArray(specS2N.getSignalSpectrum().getData(), "Signal ");
			chart.addArray(specS2N.getBackgroundSpectrum().getData(),
					"SQRT(Background)  ");

			chart.addTitle("Signal and SQRT(Background) in software aperture of "
					+ ap_diam + " pixels");
			chart.addxAxisLabel("Wavelength (nm)");
			chart.addyAxisLabel("e- per exposure per spectral pixel");

			_println(chart.getBufferedImage(), "SigAndBack");
			_println("");

			sigSpec = _printSpecTag("ASCII signal spectrum");
			backSpec = _printSpecTag("ASCII background spectrum");

			chart.flush();

			chart.addArray(specS2N.getExpS2NSpectrum().getData(),
					"Single Exp S/N");
			chart.addArray(specS2N.getFinalS2NSpectrum().getData(),
					"Final S/N  ");

			chart.addTitle("Intermediate Single Exp and Final S/N");
			chart.addxAxisLabel("Wavelength (nm)");
			chart.addyAxisLabel("Signal / Noise per spectral pixel");

			_println(chart.getBufferedImage(), "Sig2N");
			_println("");

			singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
			finalS2N = _printSpecTag("Final S/N ASCII data");
			chart.flush();

			_println(specS2N.getSignalSpectrum(), _header.toString(), sigSpec);
			_println(specS2N.getBackgroundSpectrum(), _header.toString(),
					backSpec);
			_println(specS2N.getExpS2NSpectrum(), _header.toString(), singleS2N);
			_println(specS2N.getFinalS2NSpectrum(), _header.toString(),
					finalS2N);

		} else {
			// Observing mode: Imaging

			// Calculate the Signal to Noise

			ImagingS2NCalculatable IS2Ncalc =
					ImagingS2NCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);
			IS2Ncalc.setSedIntegral(sed_integral);

			IS2Ncalc.setSkyIntegral(sky_integral);
			IS2Ncalc.setSkyAperture(_obsDetailParameters
					.getSkyApertureDiameter());
			IS2Ncalc.setSourceFraction(SFcalc.getSourceFraction());
			IS2Ncalc.setNpix(SFcalc.getNPix());
			IS2Ncalc.setDarkCurrent(instrument.getDarkCurrent());
			IS2Ncalc.calculate();
			_println(IS2Ncalc.getTextResult(device));
			// _println(IS2Ncalc.getBackgroundLimitResult());
			device.setPrecision(0); // NO decimal places
			device.clear();

			_println("");
			_println("The peak pixel signal + background is "
					+ device.toString(peak_pixel_count)
					+ ". This is "
					+ device.toString(peak_pixel_count
							/ instrument.getWellDepth() * 100)
					+ "% of the full well depth of "
					+ device.toString(instrument.getWellDepth()) + ".");

			if (peak_pixel_count > (.8 * instrument.getWellDepth()))
				_println("Warning: peak pixel exceeds 80% of the well depth and may be saturated");

			_println("");
			device.setPrecision(2); // TWO decimal places
			device.clear();
		}

		/*
		 * Here end
		 */

		// /////////////////////////////////////////////
		// ////////Print Config////////////////////////

		_print("<HR align=left SIZE=3>");
		_println("<b>Input Parameters:</b>");
		_println("Instrument: " + instrument.getName() + "\n");
		_println(_sdParameters.printParameterSummary());
		_println(instrument.toString());

		_println(_teleParameters.printParameterSummary());
		_println(_obsConditionParameters.printParameterSummary());
		_println(_obsDetailParameters.printParameterSummary());

		if (_obsDetailParameters.getCalculationMode().equals(
				ObservationDetailsParameters.SPECTROSCOPY)) {
			_println(_plotParameters.printParameterSummary());
		}
	}
}
