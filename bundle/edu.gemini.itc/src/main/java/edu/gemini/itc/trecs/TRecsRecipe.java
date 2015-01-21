// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
//
package edu.gemini.itc.trecs;

import java.io.PrintWriter;

import java.util.Enumeration;

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import edu.gemini.itc.shared.ArraySpectrum;
import edu.gemini.itc.shared.BlackBodySpectrum;
import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.EmissionLineSpectrum;
import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.Gaussian;
import edu.gemini.itc.shared.Recipe;
import edu.gemini.itc.shared.RecipeBase;
import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SEDFactory;
import edu.gemini.itc.shared.VisitableSampledSpectrum;
import edu.gemini.itc.shared.WavebandDefinition;
import edu.gemini.itc.shared.ITCImageFileIO;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ServerInfo;
import edu.gemini.itc.shared.ITCChart;

import edu.gemini.itc.parameters.ObservingConditionParameters;
import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.parameters.PlottingDetailsParameters;

import edu.gemini.itc.operation.ResampleVisitor;
import edu.gemini.itc.operation.ResampleWithPaddingVisitor;
import edu.gemini.itc.operation.RedshiftVisitor;
import edu.gemini.itc.operation.AtmosphereVisitor;
import edu.gemini.itc.operation.TelescopeApertureVisitor;
import edu.gemini.itc.operation.TelescopeTransmissionVisitor;
import edu.gemini.itc.operation.TelescopeBackgroundVisitor;
import edu.gemini.itc.operation.NormalizeVisitor;
import edu.gemini.itc.operation.CloudTransmissionVisitor;
import edu.gemini.itc.operation.WaterTransmissionVisitor;
import edu.gemini.itc.operation.PeakPixelFluxCalc;
import edu.gemini.itc.operation.SpecS2NLargeSlitVisitor;
import edu.gemini.itc.operation.SlitThroughput;
import edu.gemini.itc.operation.ImageQualityCalculatable;
import edu.gemini.itc.operation.ImageQualityCalculation;
import edu.gemini.itc.operation.ImageQualityCalculationFactory;
import edu.gemini.itc.operation.SourceFractionCalculationFactory;
import edu.gemini.itc.operation.SourceFractionCalculatable;
import edu.gemini.itc.operation.ImagingS2NCalculationFactory;
import edu.gemini.itc.operation.ImagingS2NCalculatable;
import edu.gemini.itc.operation.Calculatable;

import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class performs the calculations for T-Recs used for imaging.
 */
public final class TRecsRecipe extends RecipeBase {
	// Parameters from the web page.
	private SourceDefinitionParameters _sdParameters;
	private ObservationDetailsParameters _obsDetailParameters;
	private ObservingConditionParameters _obsConditionParameters;
	private TRecsParameters _trecsParameters;
	private TeleParameters _teleParameters;
	private PlottingDetailsParameters _plotParameters;

	private String sigSpec, backSpec, singleS2N, finalS2N;
	private SpecS2NLargeSlitVisitor specS2N;

	private Calendar now = Calendar.getInstance();
	private String _header = new StringBuffer("# T-ReCS ITC: " + now.getTime()
			+ "\n").toString();

	/**
	 * Constructs a TRecsRecipe by parsing servlet request.
	 * 
	 * @param r
	 *            Servlet request containing form data from ITC web page.
	 * @param out
	 *            Results will be written to this PrintWriter.
	 * @throws Exception
	 *             on failure to parse parameters.
	 */
	public TRecsRecipe(HttpServletRequest r, PrintWriter out) throws Exception {
		_out = out;

		// Read parameters from the four main sections of the web page.
		_sdParameters = new SourceDefinitionParameters(r);
		_obsDetailParameters = new ObservationDetailsParameters(r);
		_obsConditionParameters = new ObservingConditionParameters(r);
		_trecsParameters = new TRecsParameters(r);
		_teleParameters = new TeleParameters(r);
		_plotParameters = new PlottingDetailsParameters(r);
	}

	/**
	 * Constructs a TRecsRecipe by parsing a Multipart servlet request.
	 * 
	 * @param r
	 *            Servlet request containing form data from ITC web page.
	 * @param out
	 *            Results will be written to this PrintWriter.
	 * @throws Exception
	 *             on failure to parse parameters.
	 */
	public TRecsRecipe(ITCMultiPartParser r, PrintWriter out) throws Exception {
		_out = out;

		// Read parameters from the four main sections of the web page.
		_sdParameters = new SourceDefinitionParameters(r);
		_obsDetailParameters = new ObservationDetailsParameters(r);
		_obsConditionParameters = new ObservingConditionParameters(r);
		_trecsParameters = new TRecsParameters(r);
		_teleParameters = new TeleParameters(r);
		_plotParameters = new PlottingDetailsParameters(r);
	}

	/**
	 * Constructs a TRecsRecipe given the parameters. Useful for testing.
	 */
	public TRecsRecipe(SourceDefinitionParameters sdParameters,
			ObservationDetailsParameters obsDetailParameters,
			ObservingConditionParameters obsConditionParameters,
			TRecsParameters trecsParameters, TeleParameters teleParameters,
			PlottingDetailsParameters plotParameters,
			PrintWriter out)

	{
		super(out);
		_sdParameters = sdParameters;
		_obsDetailParameters = obsDetailParameters;
		_obsConditionParameters = obsConditionParameters;
		_trecsParameters = trecsParameters;
		_teleParameters = teleParameters;
		_plotParameters = plotParameters;
	}

	/**
	 * Performes recipe calculation and writes results to a cached PrintWriter
	 * or to System.out.
	 * 
	 * @throws Exception
	 *             A recipe calculation can fail in many ways, missing data
	 *             files, incorrectly-formatted data files, ...
	 */
	public void writeOutput() throws Exception {
		_println("");

		// This object is used to format numerical strings.
		FormatStringWriter device = new FormatStringWriter();
		device.setPrecision(2); // Two decimal places
		device.clear();

		// Module 1b
		// Define the source energy (as function of wavelength).
		//
		// inputs: instrument, SED
		// calculates: redshifted SED
		// output: redshifteed SED

		TRecs instrument = new TRecs(_trecsParameters, _obsDetailParameters);

		if (_sdParameters.getSourceSpec().equals(_sdParameters.ELINE))
			if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters
					.getELineWavelength() * 1000 / 4))) { // /4 b/c of increased
															// resolution of
															// transmission
															// files
				throw new Exception(
						"Please use a model line width > 4 nm (or "
								+ (3E5 / (_sdParameters.getELineWavelength() * 1000 / 4))
								+ " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
			}

		VisitableSampledSpectrum sed;

		sed = SEDFactory.getSED(_sdParameters, instrument);
		// sed.applyWavelengthCorrection();

		// ITCChart Chart2 = new ITCChart();

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

		// any sed except BBODY and ELINE have normailization regions
		if (!(_sdParameters.getSpectrumResource().equals(_sdParameters.ELINE) || _sdParameters
				.getSpectrumResource().equals(_sdParameters.BBODY))) {
			if (sed.getStart() > start || sed.getEnd() < end) {
				throw new Exception(
						"Shifted spectrum lies outside of specified normalisation waveband.");
			}
		}

		// if (sed.getStart() > instrument.getObservingStart() ||
		// sed.getEnd() < instrument.getObservingEnd()) {
		// _println(" Sed start" + sed.getStart() + "> than instrument start"+
		// instrument.getObservingStart());
		// _println(" Sed END" + sed.getEnd() + "< than instrument end"+
		// instrument.getObservingEnd());

		// throw new
		// Exception("Shifted spectrum lies outside of observed wavelengths");
		// }

		if (_plotParameters.getPlotLimits().equals(_plotParameters.USER_LIMITS)) {
			if (_plotParameters.getPlotWaveL() > instrument.getObservingEnd()
					|| _plotParameters.getPlotWaveU() < instrument
							.getObservingStart()) {
				_println(" The user limits defined for plotting do not overlap with the Spectrum.");

				throw new Exception(
						"User limits for plotting do not overlap with filter.");
			}
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
		if (!_sdParameters.getSpectrumResource().equals(_sdParameters.ELINE)) {
			sed.accept(norm);
		}

		// Resample the spectra for efficiency
		SampledSpectrumVisitor resample = new ResampleWithPaddingVisitor(
				instrument.getObservingStart(), instrument.getObservingEnd(),
				instrument.getSampling(), 0);
		// sed.accept(resample);

		// _println("Sed Start: " + sed.getStart());
		// _println("Sed End:   " + sed.getEnd());
		// _println("Sampling:  " + sed.getSampling());
		// _println("Length:    " + sed.getLength());
		// _println("");

		SampledSpectrumVisitor tel = new TelescopeApertureVisitor();
		sed.accept(tel);

		// SED is now in units of photons/s/nm

		// Module 3b
		// The atmosphere and telescope modify the spectrum and
		// produce a background spectrum.
		//
		// inputs: SED, AIRMASS, sky emmision file, mirror configuration,
		// output: SED and sky background as they arrive at instruments

		SampledSpectrumVisitor clouds = CloudTransmissionVisitor.create(
				_obsConditionParameters.getSkyTransparencyCloud());
		sed.accept(clouds);

		// For mid-IR observation the watervapor percentile and sky background
		// percentile must be the same
		if (_obsConditionParameters.getSkyTransparencyWaterCategory() != _obsConditionParameters
				.getSkyBackgroundCategory()) {
			_println("");
			_println("Sky background percentile must be equal to sky transparency(water vapor): \n "
					+ "    Please modify the Observing condition constraints section of the HTML form \n"
					+ "    and recalculate.");

			throw new Exception("");
		}

		SampledSpectrumVisitor water = WaterTransmissionVisitor.create(
				_obsConditionParameters.getSkyTransparencyWater(),
				_obsConditionParameters.getAirmass(), "midIR_trans_",
				ITCConstants.CERRO_PACHON, ITCConstants.MID_IR);
		sed.accept(water);

		// Background spectrum is introduced here.
		VisitableSampledSpectrum sky = SEDFactory.getSED("/"
				+ ITCConstants.HI_RES + "/" + ITCConstants.CERRO_PACHON
				+ ITCConstants.MID_IR + ITCConstants.SKY_BACKGROUND_LIB + "/"
				+ ITCConstants.MID_IR_SKY_BACKGROUND_FILENAME_BASE + "_"
				+ _obsConditionParameters.getSkyBackgroundCategory() + "_"
				+ _obsConditionParameters.getAirmassCategory()
				+ ITCConstants.DATA_SUFFIX, instrument.getSampling());

		// Chart2.addArray(sky.getData(),"Sky");
		// Chart2.addTitle("Original Sky Spectrum");
		// _println(Chart2.getBufferedImage(), "OrigSky");
		// _println("");
		// Chart2.flush();

		// resample sky_background to instrument parameters
		// sky.accept(resample);

		// Apply telescope transmission to both sed and sky
		SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(
				_teleParameters.getMirrorCoating(),
				_teleParameters.getInstrumentPort());
		sed.accept(t);
		sky.accept(t);

		// _println("Telescope Back ave: " + sky.getAverage());
		// Create and Add background for the telescope.
		SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(
				_teleParameters.getMirrorCoating(),
				_teleParameters.getInstrumentPort(),
				ITCConstants.MID_IR_TELESCOPE_BACKGROUND_FILENAME_BASE,
				ITCConstants.CERRO_PACHON, ITCConstants.MID_IR);
		sky.accept(tb);
		// _println("Telescope Back ave: " + sky.getAverage());

		sky.accept(tel);

		// Add instrument background to sky background for a total background.
		// At this point "sky" is not the right name.
		instrument.addBackground(sky);

		// _println("Telescope Back ave: " + sky.getAverage());

		// Module 4 AO module not implemented
		// The AO module affects source and background SEDs.

		// Module 5b
		// The instrument with its detectors modifies the source and
		// background spectra.
		// input: instrument, source and background SED
		// output: total flux of source and background.
		double before = sky.getAverage();
		instrument.convolveComponents(sed);
		instrument.convolveComponents(sky);

		// _println("Telescope Back ave chage: " + sky.getAverage()/before);

		// Get the summed source and sky
		double sed_integral = sed.getIntegral();
		double sky_integral = sky.getIntegral();

		// For debugging, print the spectrum integrals.
		// _println("SED integral: "+sed_integral+"\tSKY integral: "+sky_integral);
		// _println(sky.printSpecAsString());

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
		double ap_pix = 0;
		double sw_ap = 0;
		double Npix = 0;
		double source_fraction = 0;
		double pix_per_sq_arcsec = 0;
		double peak_pixel_count = 0;

		// Calculate image quality
		double im_qual = 0.;
		ImageQualityCalculatable IQcalc =
				ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _teleParameters, instrument);
		IQcalc.calculate();

		im_qual = IQcalc.getImageQuality();
		double exp_time;
		if (_obsDetailParameters.getTotalObservationTime() == 0)
			exp_time = _obsDetailParameters.getExposureTime();
		else {
			exp_time = instrument.getFrameTime();
			_obsDetailParameters.setExposureTime(exp_time);
		}

		// Calculate the Fraction of source in the aperture
		SourceFractionCalculatable SFcalc =
				SourceFractionCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);
		SFcalc.setImageQuality(im_qual);
		SFcalc.calculate();
		if (_obsDetailParameters.getCalculationMode().equals(
				ObservationDetailsParameters.IMAGING)) {
			_print(SFcalc.getTextResult(device));
			_println(IQcalc.getTextResult(device));
			_println("Sky subtraction aperture = "
					+ _obsDetailParameters.getSkyApertureDiameter()
					+ " times the software aperture.\n");
		}

		// Calculate the Peak Pixel Flux
		PeakPixelFluxCalc ppfc;

		if (_sdParameters.getSourceGeometry().equals(
				SourceDefinitionParameters.POINT_SOURCE)
				|| _sdParameters.getExtendedSourceType().equals(
						SourceDefinitionParameters.GAUSSIAN)) {

			ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
			// _obsDetailParameters.getExposureTime(), // OLD TRECS EXPOSURE
			// TIME;
			// instrument.getFrameTime(),
					exp_time, sed_integral, sky_integral,
					instrument.getDarkCurrent());

			peak_pixel_count = ppfc.getFluxInPeakPixel();
		} else if (_sdParameters.getExtendedSourceType().equals(
				SourceDefinitionParameters.UNIFORM)) {
			double usbApArea = 0;
			ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
			// _obsDetailParameters.getExposureTime(), // OLD TRECS EXPOSURE
			// TIME;
			// instrument.getFrameTime(),
					exp_time, sed_integral, sky_integral,
					instrument.getDarkCurrent());

			peak_pixel_count = ppfc.getFluxInPeakPixelUSB(
					SFcalc.getSourceFraction(), SFcalc.getNPix());
		} else {
			throw new Exception("Peak Pixel could not be calculated ");
		}

		// In this version we are bypassing morphology modules 3a-5a.
		// i.e. the output morphology is same as the input morphology.
		// Might implement these modules at a later time.
		int binFactor;
		int number_exposures;
		double spec_source_frac = 0;
		if (_obsDetailParameters.getTotalObservationTime() == 0) {
			number_exposures = _obsDetailParameters.getNumExposures(); // OLD
																		// TRECS
																		// NUM
																		// EXPOSURE
		} else {
			number_exposures = new Double(
					_obsDetailParameters.getTotalObservationTime()
							/ instrument.getFrameTime() + 0.5).intValue();
			_obsDetailParameters.setNumExposures(number_exposures); // sets
																	// number
																	// exposures
																	// for
																	// classes
																	// that need
																	// it.
		}
		double frac_with_source = _obsDetailParameters.getSourceFraction();
		double dark_current = instrument.getDarkCurrent();
		double exposure_time;
		if (_obsDetailParameters.getTotalObservationTime() == 0) {
			exposure_time = _obsDetailParameters.getExposureTime(); // OLD TRECS
																	// EXPOSURE
																	// TIME
		} else {
			exposure_time = instrument.getFrameTime();
			_obsDetailParameters.setExposureTime(exposure_time); // sets
																	// exposure
																	// time for
																	// classes
																	// that need
																	// it.
		}
		double read_noise = instrument.getReadNoise();
		// report error if this does not come out to be an integer
	    checkSourceFraction(number_exposures, frac_with_source);

		// ObservationMode Imaging or spectroscopy

		if (_obsDetailParameters.getCalculationMode().equals(
				ObservationDetailsParameters.SPECTROSCOPY)) {

			SlitThroughput st;

			// DetectorsTransmissionVisitor dtv =
			// new
			// DetectorsTransmissionVisitor(instrument.getSpectralBinning());

			// sed.accept(dtv);
			// sky.accept(dtv);

			// ChartVisitor TRecsChart = new ChartVisitor();
			ITCChart TRecsChart = new ITCChart();
			if (ap_type.equals(ObservationDetailsParameters.USER_APER)) {
				st = new SlitThroughput(im_qual,
						_obsDetailParameters.getApertureDiameter(), pixel_size,
						_trecsParameters.getFPMask());
				_println("software aperture extent along slit = "
						+ device.toString(_obsDetailParameters
								.getApertureDiameter()) + " arcsec");
			} else {
				st = new SlitThroughput(im_qual, pixel_size,
						_trecsParameters.getFPMask());
				if (_sdParameters.getSourceGeometry().equals(
						SourceDefinitionParameters.EXTENDED_SOURCE)) {
					if (_sdParameters.getExtendedSourceType().equals(
							SourceDefinitionParameters.UNIFORM)) {
						_println("software aperture extent along slit = "
								+ device.toString(1 / _trecsParameters
										.getFPMask()) + " arcsec");
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
					+ device.toString(im_qual) + "arcsec\n");

			_println("Sky subtraction aperture = "
					+ _obsDetailParameters.getSkyApertureDiameter()
					+ " times the software aperture.");

			_println("");
			_println("Requested total integration time = "
					+ device.toString(exposure_time * number_exposures)
					+ " secs, of which "
					+ device.toString(exposure_time * number_exposures
							* frac_with_source) + " secs is on source.");

			_print("<HR align=left SIZE=3>");

			ap_diam = st.getSpatialPix(); // ap_diam really Spec_Npix on Phil's
											// Mathcad change later
			spec_source_frac = st.getSlitThroughput();

			// _println("Spec_source_frac: " + st.getSlitThroughput()+
			// "  Spec_npix: "+ ap_diam);

			if (_plotParameters.getPlotLimits().equals(
					_plotParameters.USER_LIMITS)) {
				TRecsChart.setDomainMinMax(_plotParameters.getPlotWaveL(),
						_plotParameters.getPlotWaveU());
			} else {
				TRecsChart.autoscale();
			}

			// For the usb case we want the resolution to be determined by the
			// slit width and not the image quality for a point source.
			if (_sdParameters.getSourceGeometry().equals(
					SourceDefinitionParameters.EXTENDED_SOURCE)) {
				if (_sdParameters.getExtendedSourceType().equals(
						SourceDefinitionParameters.UNIFORM)) {
					im_qual = 10000;

					if (ap_type.equals(ObservationDetailsParameters.USER_APER)) {
						spec_source_frac = _trecsParameters.getFPMask()
								* ap_diam * pixel_size; // ap_diam = Spec_NPix
					} else if (ap_type
							.equals(ObservationDetailsParameters.AUTO_APER)) {
						ap_diam = new Double(
								1 / (_trecsParameters.getFPMask() * pixel_size) + 0.5)
								.intValue();
						spec_source_frac = 1;
					}
				}
			}
			// _println("Spec_source_frac: " + spec_source_frac+
			// "  Spec_npix: "+ ap_diam);
			// sed.trim(instrument.g);
			// sky.trim();
			specS2N = new SpecS2NLargeSlitVisitor(_trecsParameters.getFPMask(),
					pixel_size, instrument.getSpectralPixelWidth(),
					instrument.getObservingStart(),
					instrument.getObservingEnd(),
					instrument.getGratingDispersion_nm(),
					instrument.getGratingDispersion_nmppix(),
					instrument.getGratingResolution(), spec_source_frac,
					im_qual, ap_diam, number_exposures, frac_with_source,
					exposure_time, dark_current
							* instrument.getSpatialBinning()
							* instrument.getSpectralBinning(), read_noise,
					_obsDetailParameters.getSkyApertureDiameter(),
					instrument.getSpectralBinning());
			specS2N.setDetectorTransmission(instrument.getDetectorTransmision());
			specS2N.setSourceSpectrum(sed);
			specS2N.setBackgroundSpectrum(sky);
			sed.accept(specS2N);
			_println("<p style=\"page-break-inside: never\">");

			TRecsChart.addArray(specS2N.getSignalSpectrum().getData(),
					"Signal ");
			TRecsChart.addArray(specS2N.getBackgroundSpectrum().getData(),
					"SQRT(Background)  ");

			TRecsChart.addTitle("Signal and Background ");
			TRecsChart.addxAxisLabel("Wavelength (nm)");
			TRecsChart.addyAxisLabel("e- per exposure per spectral pixel");

			_println(TRecsChart.getBufferedImage(), "SigAndBack");
			_println("");

			sigSpec = _printSpecTag("ASCII signal spectrum");
			backSpec = _printSpecTag("ASCII background spectrum");

			TRecsChart.flush();

			TRecsChart.addArray(specS2N.getExpS2NSpectrum().getData(),
					"Single Exp S/N");
			TRecsChart.addArray(specS2N.getFinalS2NSpectrum().getData(),
					"Final S/N  ");

			TRecsChart.addTitle("Intermediate Single Exp and Final S/N");
			TRecsChart.addxAxisLabel("Wavelength (nm)");
			TRecsChart.addyAxisLabel("Signal / Noise per spectral pixel");

			_println(TRecsChart.getBufferedImage(), "Sig2N");
			_println("");

			singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
			finalS2N = _printSpecTag("Final S/N ASCII data");
			TRecsChart.flush();

			binFactor = instrument.getSpatialBinning()
					* instrument.getSpectralBinning();

			// THis was used for TED to output the data might be useful later.
			/**
			 * double [][] temp = specS2N.getSignalSpectrum().getData(); for
			 * (int i=0; i< specS2N.getSignalSpectrum().getLength()-2; i++) {
			 * System.out.print(" " +temp[0][i]+ "  ");
			 * System.out.println(temp[1][i]); } System.out.println("END");
			 * double [][] temp2 = specS2N.getFinalS2NSpectrum().getData(); for
			 * (int i=0; i< specS2N.getFinalS2NSpectrum().getLength()-2; i++) {
			 * System.out.print(" " +temp2[0][i]+ "  ");
			 * System.out.println(temp2[1][i]); } System.out.println("END");
			 * 
			 **/

		} else {

			ImagingS2NCalculatable IS2Ncalc =
					ImagingS2NCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);
			IS2Ncalc.setSedIntegral(sed_integral);
			IS2Ncalc.setSkyIntegral(sky_integral);
			IS2Ncalc.setSkyAperture(_obsDetailParameters
					.getSkyApertureDiameter());
			IS2Ncalc.setSourceFraction(SFcalc.getSourceFraction());
			IS2Ncalc.setNpix(SFcalc.getNPix());
			IS2Ncalc.setDarkCurrent(instrument.getDarkCurrent()
					* instrument.getSpatialBinning()
					* instrument.getSpatialBinning());

			IS2Ncalc.setExtraLowFreqNoise(instrument.getExtraLowFreqNoise());
			IS2Ncalc.calculate();
			_println(IS2Ncalc.getTextResult(device));
			// _println(IS2Ncalc.getBackgroundLimitResult());
			device.setPrecision(0); // NO decimal places
			device.clear();
			binFactor = instrument.getSpatialBinning()
					* instrument.getSpatialBinning();

			_println("");
			_println("The peak pixel signal + background is "
					+ device.toString(peak_pixel_count) + ". ");// This is " +
			// device.toString(peak_pixel_count/instrument.getWellDepth()*100) +
			// "% of the full well depth of "+device.toString(instrument.getWellDepth())+".");

			// if (peak_pixel_count > (.95*instrument.getWellDepth()*binFactor))
			// _println("Warning: peak pixel may be saturating the (binned) CCD full well of "+
			// .95*instrument.getWellDepth()*binFactor);

			if (peak_pixel_count > (instrument.getWellDepth()))
				_println("Warning: peak pixel may be saturating the imaging deep well setting of "
						+ instrument.getWellDepth());

		}

		_println("");
		device.setPrecision(2); // TWO decimal places
		device.clear();

		// _println("");
		_print("<HR align=left SIZE=3>");
		// _println("");

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

		if (_obsDetailParameters.getCalculationMode().equals(
				ObservationDetailsParameters.SPECTROSCOPY)) {
			_println(specS2N.getSignalSpectrum(), _header, sigSpec);
			_println(specS2N.getBackgroundSpectrum(), _header, backSpec);
			_println(specS2N.getExpS2NSpectrum(), _header, singleS2N);
			_println(specS2N.getFinalS2NSpectrum(), _header, finalS2N);
		}
	}
}