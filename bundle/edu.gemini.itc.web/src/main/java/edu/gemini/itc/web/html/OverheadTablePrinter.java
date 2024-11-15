package edu.gemini.itc.web.html;

import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.Result;
import edu.gemini.itc.shared.*;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.obs.plannedtime.OffsetOverheadCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator;
import edu.gemini.spModel.obscomp.ItcOverheadProvider;
import edu.gemini.spModel.time.TimeAmountFormatter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.Map;
import java.util.StringJoiner;


public class OverheadTablePrinter {
    private static final Logger Log = Logger.getLogger(OverheadTablePrinter.class.getName());
    private final ItcParameters p;
    private final ConfigCreator.ConfigCreatorResult ccResult;
    private final double readoutTimePerCoadd;
    private final PlannedTime pta;
    private final ItcSpectroscopyResult r;
    private final SPComponentType instrumentName;
    private final int numOfExposures;

    private final double visit_time;

    private final double recenterInterval;

    private PlannedTime.Step s;
    private Map<PlannedTime.Category, ImList<PlannedTime.CategorizedTime>> m;
    private ImList<PlannedTime.CategorizedTime> cts;

    private class OverheadTablePrinterException extends Exception {
        public OverheadTablePrinterException(String message) {
            super(message);
        }
    }

    public interface PrinterWithOverhead {
        ConfigCreator.ConfigCreatorResult createInstConfig(int numberExposures);
        ItcOverheadProvider getInst();
        double getReadoutTimePerCoadd(); // this should return "0" for instruments with no coadds

        double getVisitTime();

        double getRecenterInterval();

        int getNumberExposures();
    }

    public static String print (PrinterWithOverhead printer, final ItcParameters params,
                                final double readoutTimePerCoadd, Result result, ItcSpectroscopyResult sResult) {
        try {
            OverheadTablePrinter otp = new OverheadTablePrinter(printer, params, readoutTimePerCoadd, result, sResult);

            return otp.printOverheadTable();
        }
        catch (OverheadTablePrinterException exc) {
            return exc.getMessage();
        }
    }

    // instruments with no coadds, case of spectroscopy for instruments with IFU
    public static String print (PrinterWithOverhead printer, ItcParameters p, Result r, ItcSpectroscopyResult sr) {
        return print(printer, p, 0, r, sr);
    }

    // instruments with coadds, case of imaging, or case of spectroscopy for instruments with no IFU
    public static String print(PrinterWithOverhead printer, ItcParameters p,  double readoutTimePerCoadd, Result r) {
        return print(printer, p, readoutTimePerCoadd, r, null);
    }

    // instruments with no coadds, case of imaging, or case of spectroscopy for instruments with no IFU
    public static String print(PrinterWithOverhead printer, ItcParameters p, Result r) {
        return print(printer, p, 0, r, null);
    }

    private OverheadTablePrinter(PrinterWithOverhead printer,
                                 final ItcParameters params,
                                 final double readoutTimePerCoadd,
                                 Result result,
                                 ItcSpectroscopyResult sResult)
            throws OverheadTablePrinterException
    {
        final ObservationDetails obs = result.observation();
        final CalculationMethod calcMethod = obs.calculationMethod();

        if (calcMethod instanceof ImagingInt) {
            int coadds = params.observation().calculationMethod().coaddsOrElse(1);
            Log.fine("Number of coadds = " + coadds);
            numOfExposures = (int)(((ImagingResult) result).is2nCalc().numberSourceExposures() / coadds / obs.sourceFraction());
        } else if (calcMethod instanceof ImagingS2N) {
            numOfExposures = ((ImagingS2N) calcMethod).exposures();
        } else if (calcMethod instanceof SpectroscopyS2N) {
            numOfExposures = ((SpectroscopyS2N) calcMethod).exposures();
        } else if (calcMethod instanceof ImagingExp) {
            numOfExposures = ((GmosPrinter) printer).recipe.getNumberExposures();
        } else if (calcMethod instanceof SpectroscopyInt) {
            numOfExposures = printer.getNumberExposures();
        } else {
            numOfExposures = 1;
        }
        Log.fine("Number of exposures = " + numOfExposures);
        this.visit_time = printer.getVisitTime();
        this.recenterInterval = printer.getRecenterInterval();

        if (numOfExposures > 0) {
            this.r = sResult;
            this.p = params;
            this.ccResult = printer.createInstConfig(numOfExposures);
            this.readoutTimePerCoadd = readoutTimePerCoadd;
            this.pta = PlannedTimeMath.calc(this.ccResult.getConfig(), printer.getInst());
            this.instrumentName = (SPComponentType) ccResult.getConfig()[0].getItemValue(ConfigCreator.InstInstrumentKey);
        } else {
            throw new OverheadTablePrinterException("<b>Observation Overheads</b><br> Warning: Observation overheads cannot be calculated for the number of exposures = 0.");
        }
    }

    private int getGsaoiLgsReacqNum() {
        int gsaoiLgsReacqNum = 0;

        for (int i = 0; i < ccResult.getConfig().length; i++) {
            s = pta.steps.get(i);
            m = s.times.groupTimes();
            cts = m.get(PlannedTime.Category.CONFIG_CHANGE);

            if (cts != null) {
                for (PlannedTime.CategorizedTime lct : cts) {
                    if (lct.detail.contains("LGS Reacquisition")) {
                        gsaoiLgsReacqNum++;
                    }
                }
            }
        }
        return gsaoiLgsReacqNum;
    }


    /**
     *  get number of reacquisitions
     */
    private int getNumRecenter() {
        int numReacq = 0;
        if (p.observation().calculationMethod() instanceof Spectroscopy) {
            numReacq = PlannedTimeMath.numRecenter(pta, ccResult.getConfig()[0], visit_time, recenterInterval);
            // for IFU spectroscopy recentering is needed only for faint targets (with SNR in individual images < 5)
            if (isIFU()) {
                if (r.maxSingleSNRatio() > 5) {
                    numReacq = 0;
                }
            }
            if (instrumentName.equals(SPComponentType.INSTRUMENT_IGRINS2)) {
                numReacq = 0;  // REL-4602: IGRINS-2 does not need re-acquisitions
            }
        }
        return numReacq;
    }

    private boolean isIFU() {
        if (instrumentName.equals(SPComponentType.INSTRUMENT_GMOS) ||
                instrumentName.equals(SPComponentType.INSTRUMENT_GMOSSOUTH)) {
            GmosCommonType.FPUnit fpu = (GmosCommonType.FPUnit) ccResult.getConfig()[0].getItemValue(ConfigCreator.FPUKey);
            if (fpu.isIFU()) {
                return true;
            }
        } else if (instrumentName.equals(SPComponentType.INSTRUMENT_NIFS)) {
            return true;
        }
        return false;
    }

    private Map<Double, Integer> getOffsetsByOverhead() {
        Map<Double, Integer> offsetsByOverhead = new HashMap<>();

        for (int i = 0; i < ccResult.getConfig().length; i++) {
            s = pta.steps.get(i);
            m = s.times.groupTimes();
            cts = m.get(PlannedTime.Category.CONFIG_CHANGE);

            if (cts != null) {
                for (PlannedTime.CategorizedTime lct : cts) {
                    Double time = lct.time / 1000.0;
                    if (lct.detail.contains(OffsetOverheadCalculator.DETAIL) && lct.time != 0) {
                        offsetsByOverhead.put(time, offsetsByOverhead.getOrDefault(time, 0) + 1);
                    }
                }
            }
        }
        return offsetsByOverhead;
    }

    private String printOverheadTable() {
        StringBuilder buf = new StringBuilder("<html><body>");
        buf.append("<hr><br><table><tr><th>Observation Overheads</th></tr>");

        if (ccResult.hasWarnings()) {
            buf.append("</table>");
            buf.append("<div>");
            for (String warning : ccResult.getWarnings()) {
                buf.append("<p>").append(warning).append("</p>");
            }
            buf.append("</body></html>");
            return buf.toString();
        }


        // print setup overheads, counting one full setup per every two hours of science
        String setupStr = "";
        int numAcq = PlannedTimeMath.numAcq(pta, visit_time);
        if (numAcq == 1) {
            setupStr = String.format("%.1f s", pta.setup.time.fullSetupTime.toMillis() / 1000.0);
        } else if (numAcq > 1) {
            setupStr = String.format("%d acq x %.1f s", numAcq, pta.setup.time.fullSetupTime.toMillis() / 1000.0);
        }
        buf.append("<tr>");
        buf.append("<td>").append("Setup ").append("</td>");
        buf.append("<td align=\"right\"> ").append(setupStr).append("</td>");
        buf.append("</tr>");

        /**  print reacquisition overheads:
         *    - as target "Recentering" on the slit for all instruments but GSAOI
         *    - as "LGS Reacquisition" for GSAOI after large unguided sky offsets
         */
        int numReacq = getNumRecenter();
        int numLgsReacq = getGsaoiLgsReacqNum();
        String  reacqStr;
        if (numReacq > 0) {
            reacqStr = String.format("%d x %.1f s", numReacq, pta.setup.time.reacquisitionOnlyTime.toMillis() / 1000.0);
            buf.append("<tr>");
            buf.append("<td>").append("Re-centering ").append("</td>");
            buf.append("<td align=\"right\"> ").append(reacqStr).append("</td>");
            buf.append("</tr>");
        }
        if (numLgsReacq > 0) {
            reacqStr = String.format("%d x %.1f s", numLgsReacq, pta.setup.time.reacquisitionOnlyTime.toMillis() / 1000.0);
            buf.append("<tr>");
            buf.append("<td>").append("LGS reacquisition ").append("</td>");
            buf.append("<td align=\"right\"> ").append(reacqStr).append("</td>");
            buf.append("</tr>");
        }


        // print offset overheads
        if (!getOffsetsByOverhead().isEmpty()) {
            StringJoiner joiner = new StringJoiner(" + ");
            for (Map.Entry<Double, Integer> entry: getOffsetsByOverhead().entrySet()) {
                joiner.add(String.format("%d x %.1f s", entry.getValue(), entry.getKey()));
            }
            String offsetStr = joiner.toString();

            buf.append("<tr>");
            buf.append("<td>").append("Telescope offset ").append("</td>");
            buf.append("<td align=\"right\"> ").append(offsetStr).append("</td>");
            buf.append("<td align=\"right\"> ").append("&ensp; assuming " + ccResult.getOffsetMessage()).append("</td>");
            buf.append("</tr>");
        }

        // print the rest of categories (for which the times are the same for all steps, so using just first step)
        String secStr = "";
        s = pta.steps.get(0);
        m = s.times.groupTimes();

        for (PlannedTime.Category c : PlannedTime.Category.values()) {
            cts = m.get(c);
            if (cts == null) continue;
            PlannedTime.CategorizedTime ct = cts.max(Comparator.naturalOrder());
            int coadds = p.observation().calculationMethod().coaddsOrElse(1);
            String category = ct.category.display;

            buf.append("<tr>");
            buf.append("<td>").append(category).append("</td>");

            if (category.equals("Exposure") && (coadds !=1 )) {
                secStr = String.format("%d exp x (%d coadds x %.1f s)", numOfExposures, coadds, ct.time/1000.0/coadds);
            } else if ((category.equals("Readout") && (coadds!=1) && (readoutTimePerCoadd != 0) )) {
                secStr = String.format("%d exp x (%d coadds x %.1f s)", numOfExposures, coadds, readoutTimePerCoadd, ct.time/1000.0);
            } else {
                secStr = String.format("%d x %.1f s", numOfExposures, ct.time/1000.0);
            }

            buf.append("<td align=\"right\"> ").append(secStr).append("</td>");
            buf.append("</tr>");
        }

        long totalTime = PlannedTimeMath.totalTimeWithReacq(pta, numReacq, (int) visit_time);
        buf.append("<tr><td><b>Program time</b></td><td align=\"right\"><b>").append(String.format("%s", TimeAmountFormatter.getDescriptiveFormat(totalTime))).append("</b></td></tr>");
        buf.append("</table>");

        buf.append("</body></html>");

        return buf.toString();
    }
}
