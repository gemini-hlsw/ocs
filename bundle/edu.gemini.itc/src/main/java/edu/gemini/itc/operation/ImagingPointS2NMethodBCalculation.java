// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.operation;

import edu.gemini.itc.shared.FormatStringWriter;

public class ImagingPointS2NMethodBCalculation extends ImagingS2NCalculation {

    int number_exposures, int_req_source_exposures;
    double frac_with_source, exp_s2n, final_s2n, number_source_exposures,
            req_s2n, req_source_exposures, req_number_exposures,
            effective_s2n;

    public ImagingPointS2NMethodBCalculation(int number_exposures,
                                             double frac_with_source,
                                             double exposure_time, double read_noise,
                                             double req_s2n,
                                             double pixel_size) {

        this.number_exposures = number_exposures;
        this.frac_with_source = frac_with_source;
        this.exposure_time = exposure_time;
        this.read_noise = read_noise;
        this.pixel_size = pixel_size;
        this.req_s2n = req_s2n;

    }

    public void calculate() throws Exception {
        super.calculate();
        req_source_exposures = (req_s2n / signal) * (req_s2n / signal) *
                (signal + noiseFactor * sourceless_noise * sourceless_noise);

        int_req_source_exposures =
                new Double(Math.ceil(req_source_exposures)).intValue();

        req_number_exposures =
                int_req_source_exposures / frac_with_source;

        effective_s2n =
                (Math.sqrt(int_req_source_exposures) * signal) /
                        Math.sqrt(signal + noiseFactor * sourceless_noise * sourceless_noise);


    }

    public String getTextResult(FormatStringWriter device) {
        StringBuffer sb = new StringBuffer(super.getTextResult(device));
        device.setPrecision(0);
        device.clear();

        sb.append("Derived number of exposures = " +
                device.toString(req_number_exposures) +
                " , of which " + device.toString(req_number_exposures
                * frac_with_source));
        if (req_number_exposures == 1)
            sb.append(" is on source.\n");
        else
            sb.append(" are on source.\n");

        sb.append("Taking " +
                device.toString(Math.ceil(req_number_exposures)));
        if (Math.ceil(req_number_exposures) == 1)
            sb.append(" exposure");
        else
            sb.append(" exposures");
        sb.append(", the effective S/N for the whole" +
                " observation is ");

        device.setPrecision(2);
        device.clear();

        sb.append(device.toString(effective_s2n) +
                " (including sky subtraction)\n\n");


        sb.append("Required total integration time is " +
                device.toString(req_number_exposures *
                        exposure_time) +
                " secs, of which " +
                device.toString(req_number_exposures *
                        exposure_time *
                        frac_with_source)
                + " secs is on source.\n");

        return sb.toString();
    }


}

