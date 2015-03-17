package edu.gemini.itc.michelle;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.service.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.HtmlPrinter;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.spModel.core.Site;

import java.io.PrintWriter;
import java.util.Calendar;

/**
 * This class performs the calculations for Michelle
 * used for imaging.
 */
public final class MichelleRecipe extends RecipeBase {

    // Parameters from the web page.
    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final MichelleParameters _michelleParameters;
    private final TelescopeDetails _telescope;
    private final PlottingDetails _plotParameters;

    private SpecS2NLargeSlitVisitor specS2N;
    private String sigSpec, backSpec, singleS2N, finalS2N;

    private Calendar now = Calendar.getInstance();
    private String _header = new StringBuffer("# Michelle ITC: " + now.getTime() + "\n").toString();

    /**
     * Constructs a MichelleRecipe by parsing  a Multipart servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public MichelleRecipe(ITCMultiPartParser r, PrintWriter out) {
        super(out);
        // Set the Http Session object
        //_sessionObject = r.getSession(true);

        //System.out.println(" Session is over after" +_sessionObject.getCreationTime());


        // Read parameters from the four main sections of the web page.
        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _michelleParameters = new MichelleParameters(r);
        _obsDetailParameters = correctedObsDetails(_michelleParameters, ITCRequest.observationParameters(r));
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _telescope = ITCRequest.teleParameters(r);
        _plotParameters = ITCRequest.plotParamters(r);
    }

    /**
     * Constructs a MichelleRecipe given the parameters.
     * Useful for testing.
     */
    public MichelleRecipe(SourceDefinition sdParameters,
                          ObservationDetails obsDetailParameters,
                          ObservingConditions obsConditionParameters,
                          MichelleParameters michelleParameters,
                          TelescopeDetails telescope,
                          PlottingDetails plotParameters,
                          PrintWriter out) {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = correctedObsDetails(michelleParameters, obsDetailParameters);
        _obsConditionParameters = obsConditionParameters;
        _michelleParameters = michelleParameters;
        _telescope = telescope;
        _plotParameters = plotParameters;
    }

    private ObservationDetails correctedObsDetails(MichelleParameters mp, ObservationDetails odp) {
        // TODO : These corrections were previously done in random places throughout the recipe. I moved them here
        // TODO : so the ObservationDetailsParameters object can become immutable. Basically this calculates
        // TODO : some missing parameters and/or turns the total exposure time into a single exposure time.
        // TODO : This is a temporary hack. There needs to be a better solution for this.
        // NOTE : odp.getExposureTime() carries the TOTAL exposure time (as opposed to exp time for a single frame)
        final Michelle instrument = new Michelle(mp, odp); // TODO: Avoid creating an instrument instance twice.
        final double correctedTotalObservationTime;
        if (mp.polarimetryIsUsed()) {
            //If polarimetry is used divide exposure time by 4 because of the 4 waveplate positions
            correctedTotalObservationTime = odp.getExposureTime() / 4;
        } else {
            correctedTotalObservationTime = odp.getExposureTime();
        }
        final double correctedExposureTime = instrument.getFrameTime();
        final int correctedNumExposures = new Double(correctedTotalObservationTime / instrument.getFrameTime() + 0.5).intValue();
        if (odp.getMethod() instanceof ImagingInt) {
            return new ObservationDetails(
                    new ImagingInt(odp.getSNRatio(), correctedExposureTime, odp.getSourceFraction()),
                    odp.getAnalysis()
            );
        } else if (odp.getMethod() instanceof ImagingSN) {
            return new ObservationDetails(
                    new ImagingSN(correctedNumExposures, correctedExposureTime, odp.getSourceFraction()),
                    odp.getAnalysis()
            );
        } else if (odp.getMethod() instanceof SpectroscopySN) {
            return new ObservationDetails(
                    new SpectroscopySN(correctedNumExposures, correctedExposureTime, odp.getSourceFraction()),
                    odp.getAnalysis()
            );
        } else {
            throw new IllegalArgumentException();
        }

    }

    /**
     * Performes recipe calculation and writes results to a cached PrintWriter
     * or to System.out.
     *
     * @throws Exception A recipe calculation can fail in many ways,
     *                   missing data files, incorrectly-formatted data files, ...
     */
    public void writeOutput() {
        _println("");

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();

        Michelle instrument = new Michelle(_michelleParameters, _obsDetailParameters);


        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE))
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength() * 1000 * 5))) {  //*5 b/c of increased resolution of transmission files
                throw new RuntimeException("Please use a model line width > 0.2 nm (or " + (3E5 / (_sdParameters.getELineWavelength() * 1000 * 5)) + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GN, ITCConstants.MID_IR, _sdParameters, _obsConditionParameters, _telescope, _plotParameters);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;
        double sed_integral = sed.getIntegral();
        double sky_integral = sky.getIntegral();


        // For debugging, print the spectrum integrals.
        //_println("SED integral: "+sed_integral+"\tSKY integral: "+sky_integral);
        //_println(sky.printSpecAsString());

        // End of the Spectral energy distribution portion of the ITC.

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio.  There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        double pixel_size = instrument.getPixelSize();
        double ap_diam = 0;
        double peak_pixel_count = 0;

        // Calculate image quality
        double im_qual = 0.;
        ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        im_qual = IQcalc.getImageQuality();
        double exp_time = _obsDetailParameters.getExposureTime();


        // Calculate the Fraction of source in the aperture
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);
        if (_obsDetailParameters.getMethod().isImaging()) {
            _print(SFcalc.getTextResult(device));
            _println(IQcalc.getTextResult(device));
            _println("Sky subtraction aperture = " +
                    _obsDetailParameters.getSkyApertureDiameter()
                    + " times the software aperture.\n");
        }

        if (_michelleParameters.polarimetryIsUsed()) {
            _println("Polarimetry mode enabled.\n");
        }

        // Calculate the Peak Pixel Flux
        PeakPixelFluxCalc ppfc;

        if (!_sdParameters.isUniform()) {

            ppfc = new
                    PeakPixelFluxCalc(im_qual, pixel_size,
                    exp_time,
                    sed_integral, sky_integral,
                    instrument.getDarkCurrent());

            peak_pixel_count = ppfc.getFluxInPeakPixel();

        } else {

            ppfc = new
                    PeakPixelFluxCalc(im_qual, pixel_size,
                    exp_time,
                    sed_integral, sky_integral,
                    instrument.getDarkCurrent());

            peak_pixel_count = ppfc.getFluxInPeakPixelUSB(SFcalc.getSourceFraction(), SFcalc.getNPix());
        }

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        int binFactor;
        int number_exposures = _obsDetailParameters.getNumExposures();
        double spec_source_frac = 0;
        double frac_with_source = _obsDetailParameters.getSourceFraction();
        double dark_current = instrument.getDarkCurrent();
        double exposure_time = _obsDetailParameters.getExposureTime();
        double read_noise = instrument.getReadNoise();
        // report error if this does not come out to be an integer
        checkSourceFraction(number_exposures, frac_with_source);

        //ObservationMode Imaging or spectroscopy


        if (_obsDetailParameters.getMethod().isSpectroscopy()) {

            SlitThroughput st;
            if (!_obsDetailParameters.isAutoAperture()) {
                st = new SlitThroughput(im_qual,
                        _obsDetailParameters.getApertureDiameter(),
                        pixel_size, _michelleParameters.getFPMask());
                _println("software aperture extent along slit = " + device.toString(_obsDetailParameters.getApertureDiameter()) +
                        " arcsec");
            } else {

                st = new SlitThroughput(im_qual, pixel_size, _michelleParameters.getFPMask());

                switch (_sdParameters.getProfileType()) {
                    case UNIFORM:
                        _println("software aperture extent along slit = " + device.toString(1 / _michelleParameters.getFPMask()) + " arcsec");
                        break;
                    case POINT:
                        _println("software aperture extent along slit = " + device.toString(1.4 * im_qual) + " arcsec");
                        break;
                }


            }

            if (!_sdParameters.isUniform()) {
                _println("fraction of source flux in aperture = " +
                        device.toString(st.getSlitThroughput()));
            }

            _println("derived image size(FWHM) for a point source = " + device.toString(im_qual) + "arcsec\n");

            _println("Sky subtraction aperture = " +
                    _obsDetailParameters.getSkyApertureDiameter()
                    + " times the software aperture.");

            _println("");
            if (_michelleParameters.polarimetryIsUsed()) {
                //Michelle polarimetry uses 4 waveplate positions so a single observation takes 4 times as long.
                //To the user it should appear as though the time used by the ITC matches thier requested time.
                //hence the x4 factor
                _println("Requested total integration time = " +
                        device.toString(exposure_time * 4 * number_exposures) +
                        " secs, of which " + device.toString(exposure_time * 4 *
                        number_exposures *
                        frac_with_source) +
                        " secs is on source.");
            } else {
                _println("Requested total integration time = " +
                        device.toString(exposure_time * number_exposures) +
                        " secs, of which " + device.toString(exposure_time *
                        number_exposures *
                        frac_with_source) +
                        " secs is on source.");
            }

            _print("<HR align=left SIZE=3>");

            ap_diam = st.getSpatialPix(); // ap_diam really Spec_Npix on Phil's Mathcad change later
            spec_source_frac = st.getSlitThroughput();

            //_println("Spec_source_frac: " + st.getSlitThroughput()+ "  Spec_npix: "+ ap_diam);


            //For the usb case we want the resolution to be determined by the
            //slit width and not the image quality for a point source.
            if (_sdParameters.isUniform()) {
                im_qual = 10000;
                if (_obsDetailParameters.isAutoAperture()) {
                    ap_diam = new Double(1 / (_michelleParameters.getFPMask() * pixel_size) + 0.5).intValue();
                    spec_source_frac = 1;
                } else {
                    spec_source_frac = _michelleParameters.getFPMask() * ap_diam * pixel_size;  //ap_diam = Spec_NPix
                }
            }
            //_println("Spec_source_frac: " + spec_source_frac+ "  Spec_npix: "+ ap_diam);
            specS2N =
                    new SpecS2NLargeSlitVisitor(_michelleParameters.getFPMask(), pixel_size,
                            instrument.getSpectralPixelWidth(),
                            instrument.getObservingStart(),
                            instrument.getObservingEnd(),
                            instrument.getGratingDispersion_nm(),
                            instrument.getGratingDispersion_nmppix(),
                            instrument.getGratingResolution(),
                            spec_source_frac, im_qual,
                            ap_diam, number_exposures,
                            frac_with_source, exposure_time,
                            dark_current * instrument.getSpatialBinning() *
                                    instrument.getSpectralBinning(), read_noise,
                            _obsDetailParameters.getSkyApertureDiameter(),
                            instrument.getSpectralBinning());
            specS2N.setDetectorTransmission(instrument.getDetectorTransmision());
            specS2N.setSourceSpectrum(sed);
            specS2N.setBackgroundSpectrum(sky);
            sed.accept(specS2N);

            _println("<p style=\"page-break-inside: never\">");
            /*
            MichelleChart.setSeriesName("Signal  ");
            MichelleChart.setName("Signal and Background ");
            MichelleChart.setYaxisTitle("e- per exposure per spectral pixel");
            MichelleChart.setSpectrum(specS2N.getSignalSpectrum());
             
            MichelleChart.setSeriesName("SQRT(Background)  ");
            MichelleChart.addSpectrum(specS2N.getBackgroundSpectrum());
            specS2N.getBackgroundSpectrum().accept(MichelleChart);
             
            _println(MichelleChart.getImage(), "SigAndBack");
             
            MichelleChart.setSeriesName("Single Exp S/N  ");
            MichelleChart.setName("Intermediate Single Exp and Final S/N");
            MichelleChart.setYaxisTitle("Signal / Noise per spectral pixel");
             
            MichelleChart.setSpectrum(specS2N.getExpS2NSpectrum());
             
            MichelleChart.setSeriesName("Final S/N  ");
            MichelleChart.addSpectrum(specS2N.getFinalS2NSpectrum());
             
            specS2N.getFinalS2NSpectrum().accept(MichelleChart);
            _println(MichelleChart.getImage(), "Sig2N");
             */
            final ITCChart chart1 = new ITCChart("Signal and Background ", "Wavelength (nm)", "e- per exposure per spectral pixel", _plotParameters);
            chart1.addArray(specS2N.getSignalSpectrum().getData(), "Signal ");
            chart1.addArray(specS2N.getBackgroundSpectrum().getData(), "SQRT(Background)  ");
            _println(chart1.getBufferedImage(), "SigAndBack");
            _println("");

            sigSpec = _printSpecTag("ASCII signal spectrum");
            backSpec = _printSpecTag("ASCII background spectrum");

            final ITCChart chart2 = new ITCChart("Intermediate Single Exp and Final S/N", "Wavelength (nm)", "Signal / Noise per spectral pixel", _plotParameters);
            chart2.addArray(specS2N.getExpS2NSpectrum().getData(), "Single Exp S/N");
            chart2.addArray(specS2N.getFinalS2NSpectrum().getData(), "Final S/N  ");
            _println(chart2.getBufferedImage(), "Sig2N");
            _println("");

            singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
            finalS2N = _printSpecTag("Final S/N ASCII data");

            binFactor = instrument.getSpatialBinning() *
                    instrument.getSpectralBinning();

            //THis was used for TED to output the data might be useful later.
            /**  double [][] temp = specS2N.getSignalSpectrum().getData();
             * for (int i=0; i< specS2N.getSignalSpectrum().getLength()-2; i++)
             * {
             * System.out.print(" " +temp[0][i]+ "  ");
             * System.out.println(temp[1][i]);
             * }
             * System.out.println("END");
             * double [][] temp2 = specS2N.getFinalS2NSpectrum().getData();
             * for (int i=0; i< specS2N.getFinalS2NSpectrum().getLength()-2; i++)
             * {
             * System.out.print(" " +temp2[0][i]+ "  ");
             * System.out.println(temp2[1][i]);
             * }
             * System.out.println("END");
             *
             **/

        } else {
            
            
/*
    //****************THIS IS NOW METHOD C.. MAY BE USED IN THE FUTURE******
//else perform calculation of inttime given S/N
//    }else {
//       // define variables that will be used. the rest were defined above.
//       double req_s2n=_obsDetailParameters.getSNRatio();
//       double partial_equation;
//       double derived_exposure_time;
//       double derived_int_time;
//       double read_noise = instrument.getReadNoise();
//       double dark_current = instrument.getDarkCurrent();
//       double summed_source=sed_integral;
//       double summed_background=sky_integral;
 
// if (_sdParameters.getSourceGeometry().equals(
// 		   SourceDefinitionParameters.EXTENDED_SOURCE))
//     {
// 	if (_sdParameters.getExtendedSourceType().
// 	             equals(SourceDefinitionParameters.UNIFORM))
// 	{
// 	    req_s2n = req_s2n/Math.sqrt(pix_per_sq_arcsec);
// 	}
//     }
 
//   //equation directly from math cad.
//       partial_equation =(req_s2n*source_fraction*summed_source+
//           (2*req_s2n*summed_background*Math.pow(pixel_size,2)*
// 	  Npix+2*req_s2n*dark_current*Npix)+Math.sqrt(Math.pow(req_s2n,2)*
// 	  Math.pow(source_fraction,2)*Math.pow(summed_source,2)+
// 	  4*Math.pow(req_s2n,2)*source_fraction*summed_source*
// 	  summed_background*Math.pow(pixel_size,2)*Npix+
// 	  4*Math.pow(req_s2n,2)*source_fraction*summed_source*
// 	  dark_current*Npix+4*Math.pow(req_s2n,2)*
//           Math.pow(summed_background,2)*Math.pow(pixel_size,4)*
// 	  Math.pow(Npix,2)+8*Math.pow(req_s2n,2)*summed_background*
//           Math.pow(pixel_size,2)*Math.pow(Npix,2)*dark_current+4*
//           Math.pow(req_s2n,2)*Math.pow(dark_current,2)*
// 	  Math.pow(Npix,2)+8*Math.pow(source_fraction,2)*
//           Math.pow(summed_source,2)*number_source_exposures*
//           Math.pow(read_noise,2)*Npix));
 
//       derived_exposure_time=req_s2n*partial_equation/(2*
// 			    Math.pow(source_fraction,2)*
//  	                    Math.pow(summed_source,2)*
//                             number_source_exposures);
 
//       derived_int_time = number_exposures*derived_exposure_time;
 
//       _println("");
//       _println("Derived Exposure time = " +
// 	       device.toString(derived_exposure_time));
//       _println("");
//       _println("Derived Integration time = " +
// 	       device.toString(derived_int_time));
 
 
//    }
 */
            // for testing
            /*
            if (_sdParameters.getSourceGeometry().
                equals(SourceDefinitionParameters.UNIFORM)) {
               _println("Final Uniform Surface Brightness S2N: " + usb_final_s2n);
            }
             */

            final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc);
            IS2Ncalc.setSedIntegral(sed_integral);
            IS2Ncalc.setSkyIntegral(sky_integral);
            IS2Ncalc.setSkyAperture(_obsDetailParameters.getSkyApertureDiameter());
            IS2Ncalc.setDarkCurrent(instrument.getDarkCurrent() * instrument.getSpatialBinning() * instrument.getSpatialBinning());
            IS2Ncalc.calculate();

            // Michelle polarimetry calculations include a x4 overhead of observing into the calculation
            // the following code applies this factor to all the needed values
            if (!_michelleParameters.polarimetryIsUsed()) {
                _println(IS2Ncalc.getTextResult(device));
            } else {
                String result = IS2Ncalc.getTextResult(device);
                String delims = "[ ]+";
                String[] tokens = result.split(delims);
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i].contains("Derived")) {
                        tokens[i + 5] = device.toString((new Double(tokens[i + 5]).doubleValue() * 4));
                        tokens[i + 9] = device.toString((new Double(tokens[i + 9]).doubleValue() * 4));
                    }
                    if (tokens[i].contains("Taking")) {
                        tokens[i + 1] = device.toString((new Double(tokens[i + 1]).doubleValue() * 4));
                    }
                    if (tokens[i].contains("Requested") || tokens[i].contains("Required")) {
                        tokens[i + 5] = device.toString((new Double(tokens[i + 5]).doubleValue() * 4));
                        tokens[i + 9] = device.toString((new Double(tokens[i + 9]).doubleValue() * 4));
                    }
                    _print(tokens[i] + " ");

                }

            }
            //_println(IS2Ncalc.getBackgroundLimitResult());
            device.setPrecision(0);  // NO decimal places
            device.clear();
            binFactor = instrument.getSpatialBinning() *
                    instrument.getSpatialBinning();

            _println("");
            _println("The peak pixel signal + background is " + device.toString(peak_pixel_count) + ". ");//This is " +
            //	device.toString(peak_pixel_count/instrument.getWellDepth()*100) +
            //	"% of the full well depth of "+device.toString(instrument.getWellDepth())+".");

//	if (peak_pixel_count > (.95*instrument.getWellDepth()*binFactor))
//	       	_println("Warning: peak pixel may be saturating the (binned) CCD full well of "+
//	       					.95*instrument.getWellDepth()*binFactor);

            if (peak_pixel_count > (instrument.getWellDepth()))
                _println("Warning: peak pixel may be saturating the imaging deep well setting of " +
                        instrument.getWellDepth());

        }

        _println("");
        device.setPrecision(2);  // TWO decimal places
        device.clear();


        //_println("");
        _print("<HR align=left SIZE=3>");
        //_println("");
        /*
        _println( instrument.toString() );
        _println("");
         
              device.setPrecision(0);  // NO decimal places
               device.clear();
         
        _println("Observing Conditions:");
        _println("<LI> Image Quality: " +
                 device.toString(_obsConditionParameters.getImageQualityPercentile()
         * 100) + "%");
        _println("<LI> Sky Transparency (cloud cover): " +
                 device.toString(_obsConditionParameters.
                            getSkyTransparencyCloudPercentile() * 100)
                 + "%");
        _println("<LI> Sky transparency (water vapour): " +
                 device.toString(_obsConditionParameters.
                            getSkyTransparencyWaterPercentile() * 100)
                 + "%");
        _println("<LI> Sky background: " +
                 device.toString(_obsConditionParameters.
                            getSkyBackgroundPercentile() * 100)
                 + "%");
            device.setPrecision(1);  // Two decimal places
                     device.clear();
         
        _println("Frequency of occurrence of these conditions: " +
                 device.toString(
                 _obsConditionParameters.getImageQualityPercentile() *
                 _obsConditionParameters.getSkyTransparencyCloudPercentile() *
                 _obsConditionParameters.getSkyTransparencyWaterPercentile() *
                 _obsConditionParameters.getSkyBackgroundPercentile()*100
                 ) + "%"
                 );
                    device.setPrecision(2);  // Two decimal places
                     device.clear();
         
        _println("");
                                                  //getSpectrumResource
            if (_sdParameters.getDistributionType().equals(_sdParameters.ELINE)){
                          device.setPrecision(4);
                          device.clear();
                   _print("The Source is an emission line, at a wavelength of "
                                           +device.toString(_sdParameters.getELineWavelength()));
                           device.setPrecision(2);
                           device.clear();
                           _print(" microns, and with a width of "
                                          +device.toString(_sdParameters.getELineWidth())+
                                          " km/s.\n  It's total flux is "+
                                        device.toString(_sdParameters.getELineFlux())+
                                        " " + _sdParameters.getELineFluxUnits()+
                                          " on a flat continuum of flux density " +
                                          device.toString(_sdParameters.getELineContinuumFlux())+
                                        " " + _sdParameters.getELineContinuumFluxUnits()+
                                        ".");
            }else if (_sdParameters.getDistributionType().equals(_sdParameters.BBODY)){
                            _print("The Source is a "
                                            + _sdParameters.getBBTemp() + "K Blackbody, at "
                                            + _sdParameters.getSourceNormalization() +
                                            " " + _sdParameters.getUnits() +" in the "+
                                            _sdParameters.getNormBand()+ " band.");
            }else if (_sdParameters.getDistributionType().equals(_sdParameters.LIBRARY_STAR)){
                            _print("The Source is a "+_sdParameters.getSourceNormalization() +
                                            " " + _sdParameters.getUnits() + " " + _sdParameters.getSpecType() +
                                            " star at " + _sdParameters.getNormBand()+ ".");
            }else if (_sdParameters.getDistributionType().equals(_sdParameters.LIBRARY_NON_STAR)){
                            _print("The Source is a "+_sdParameters.getSourceNormalization() +
                                            " " + _sdParameters.getUnits() + " " + _sdParameters.getSpecType() +
                                            " at " + _sdParameters.getNormBand()+ ".");
         
            }
         */
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(_sdParameters));
        _println(instrument.toString());
        _println(HtmlPrinter.printParameterSummary(_telescope));
        _println(HtmlPrinter.printParameterSummary(_obsConditionParameters));

        // Michelle polarimetry calculations include a x4 overhead of observing into the calculation
        // the following code applies this factor to all the needed values
        if (!_michelleParameters.polarimetryIsUsed()) {
            _println(HtmlPrinter.printParameterSummary(_obsDetailParameters));
        } else {
            String result = HtmlPrinter.printParameterSummary(_obsDetailParameters);
            String delims = "[ ]+";
            String[] tokens = result.split(delims);
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].contains("<LI>Calculation") && tokens[i + 2].contains("S/N")) {
                    tokens[i + 5] = device.toString((new Double(tokens[i + 5]).doubleValue() * 4));
                }
                _print(tokens[i] + " ");

            }
        }

        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            _println(HtmlPrinter.printParameterSummary(_plotParameters));
            _println(specS2N.getSignalSpectrum(), _header, sigSpec);
            _println(specS2N.getBackgroundSpectrum(), _header, backSpec);
            _println(specS2N.getExpS2NSpectrum(), _header, singleS2N);
            _println(specS2N.getFinalS2NSpectrum(), _header, finalS2N);
        }

    }
}
