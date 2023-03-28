/**
 * $Id: NiriReadoutTime.java 21139 2009-07-19 04:25:39Z swalker $
 */

package edu.gemini.spModel.gemini.niri;

import edu.gemini.pot.sp.Instrument;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.gemini.niri.Niri.BuiltinROI;
import edu.gemini.spModel.gemini.niri.Niri.ReadMode;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.shared.util.immutable.None;

/**
 * Local class used to map NIRI ROI and ReadMode settings to readout times
 *
 */
public final class NiriReadoutTime {

    // OT-470:
    // NIRI readout overheads for each fits image updated from the following table:
    //
    // ArrayGeometry    Readmode    Overhead (seconds)
    // -------------    --------    ---------------------
    // 1024             high        2.78 + 0.18 * Ncoadds
    // 1024             med         2.78 + 0.70 * Ncoadds
    // 1024             low         2.78 + 11.15 * Ncoadds
    //
    // 768              high        2.50 + 0.12 * Ncoadds
    // 768              med         2.50 + 0.41 * Ncoadds
    // 768              low         2.50 + 6.49 * Ncoadds
    //
    // 512              high        2.38 + 0.07 * Ncoadds
    // 512              med         2.38 + 0.20 * Ncoadds
    // 512              low         2.38 + 2.9 * Ncoadds
    //
    // 256              high        2.96 + 0.03 * Ncoadds
    // 256              med         2.96 + 0.07 * Ncoadds
    // 256              low         2.96 + 0.8 * Ncoadds
    //
    // 1024x512         high        2.5 + 0.1 * Ncoadds
    // 1024x512         med         2.5 + 0.4 * Ncoadds
    // 1024x512         low         2.5 + 5.6 * Ncoadds
    //
    // As an example, the TOTAL time required for a
    // repeat 3 of 5 offsets of 30sec exposures with 4 coadds
    // using 1024 low RN mode would be:
    // 3 * 5 * (2.78 + 4 * (11.15 + 30)) = 2510.7 seconds
    // (= 3 * 5 * ((2.78 + 11.15 * 4) + (30 * 4)) seconds)
    //
    // This does not include telescope offsetting overheads.

    // 2017-12-15: Modified according to the latest read time measurements.

    private static final double DHS_WRITE_TIME =
        SPInstObsComp.WRITE_TIMES.get(Instrument.Niri).timeSeconds();

    private static final NiriReadoutTime[] _readoutTimes = new NiriReadoutTime[]{
        new NiriReadoutTime(BuiltinROI.FULL_FRAME, ReadMode.IMAG_SPEC_3TO5, 0.20),
        new NiriReadoutTime(BuiltinROI.FULL_FRAME, ReadMode.IMAG_1TO25, 0.71),
        new NiriReadoutTime(BuiltinROI.FULL_FRAME, ReadMode.IMAG_SPEC_NB, 11.16),

        new NiriReadoutTime(BuiltinROI.CENTRAL_768, ReadMode.IMAG_SPEC_3TO5, 0.11),
        new NiriReadoutTime(BuiltinROI.CENTRAL_768, ReadMode.IMAG_1TO25, 0.41),
        new NiriReadoutTime(BuiltinROI.CENTRAL_768, ReadMode.IMAG_SPEC_NB, 6.48),

        new NiriReadoutTime(BuiltinROI.CENTRAL_512, ReadMode.IMAG_SPEC_3TO5, 0.056),
        new NiriReadoutTime(BuiltinROI.CENTRAL_512, ReadMode.IMAG_1TO25, 0.21),
        new NiriReadoutTime(BuiltinROI.CENTRAL_512, ReadMode.IMAG_SPEC_NB, 3.10),

        new NiriReadoutTime(BuiltinROI.CENTRAL_256, ReadMode.IMAG_SPEC_3TO5, 0.044),
        new NiriReadoutTime(BuiltinROI.CENTRAL_256, ReadMode.IMAG_1TO25, 0.078),
        new NiriReadoutTime(BuiltinROI.CENTRAL_256, ReadMode.IMAG_SPEC_NB, 0.98),

        new NiriReadoutTime(BuiltinROI.SPEC_1024_512, ReadMode.IMAG_SPEC_3TO5, 0.1),
        new NiriReadoutTime(BuiltinROI.SPEC_1024_512, ReadMode.IMAG_1TO25, 0.4),
        new NiriReadoutTime(BuiltinROI.SPEC_1024_512, ReadMode.IMAG_SPEC_NB, 5.6),
    };

    private final BuiltinROI _roi;
    private final ReadMode _readMode;
    private final double _n; // multiply this by coadds and add to overhead

    private NiriReadoutTime(Niri.BuiltinROI builtinROI, Niri.ReadMode readMode,
                            double n) {
        _roi          = builtinROI;
        _readMode     = readMode;
        _n            = n;
    }

    public static double getDhsWriteTime () {
        return DHS_WRITE_TIME;
    }

    public double getReadout(int coadds) {
        return _n * coadds;
    }

    public static Option<NiriReadoutTime> lookup(Niri.BuiltinROI roi, Niri.ReadMode readMode) {
        for (NiriReadoutTime r : _readoutTimes) {
            if (r._readMode == readMode && r._roi == roi) {
                return new Some<NiriReadoutTime>(r);
            }
        }
        return None.instance();
    }

    public static Option<NiriReadoutTime> lookup(InstNIRI inst, IConfig config) {
        ReadMode readMode = inst.getReadMode();
        BuiltinROI roi = inst.getBuiltinROI();

        if (config != null) {
            String s = SeqConfigNames.INSTRUMENT_CONFIG_NAME;

            // See CachedConfig.getParameterValue() for why we don't use the ISysConfig here
            readMode = (ReadMode) config.getParameterValue(s, InstNIRI.READ_MODE_PROP.getName());

            // ROI is sequenced as of SCT-265/OT-709
            roi = (BuiltinROI) config.getParameterValue(s, InstNIRI.BUILTIN_ROI_PROP.getName());
        }

        return lookup(roi, readMode);
    }
}
