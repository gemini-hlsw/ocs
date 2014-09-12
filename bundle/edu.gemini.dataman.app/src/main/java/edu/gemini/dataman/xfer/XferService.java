//
// $Id: XferService.java 163 2005-10-01 23:26:20Z shane $
//

package edu.gemini.dataman.xfer;

import edu.gemini.dataman.context.DatamanContext;
import edu.gemini.dataman.context.GsaXferConfig;
import edu.gemini.dataman.context.XferConfig;
import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spModel.core.ProgramType$;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.dataflow.GsaAspect;

import java.io.File;

/**
 * A service that handles transferring datasets from the working storage area
 * to the base facility test area and/or the GSA transfer directory.
 */
public final class XferService {
    private XferService() {
    }

    /**
     * Returns <code>true</code> if a dataset with the given label should be
     * sent to the GSA.  <code>false</code> otherwise.
     */
    public static boolean shouldXferToGsa(DatasetLabel label) {
        if (label == null) return false;
        SPObservationID obsId = label.getObservationId();
        if (obsId == null) return false;
        SPProgramID progId = obsId.getProgramID();
        if (progId == null) return false;

        final ProgramType pt = ProgramType$.MODULE$.readOrNull(progId);
        return GsaAspect.getDefaultAspect(pt).isSendToGsa();
    }

    /**
     * Schedules an <em>initial</em> file transfer to the GSA transfer
     * directory, assuming the {@link ProgramType} warrants it.  Only certain
     * program types (such as queue and classical program) are actually
     * transfered.   See {@link ProgramType} for details.
     *
     * <p>This method applies to new datasets that still have an unset QA State.
     * An initial attempt is made to transfer the file to the GSA, but the
     * GSA state is not updated during the transfer, and failures are only
     * logged.
     */
    public static void initialXferToGsa(DatamanContext ctx, DatasetLabel label, File dataset) {
        if (!shouldXferToGsa(label)) return;
        GsaXferConfig xferConf = ctx.getConfig().getGsaXferConfig();

        AbstractXferCommand gsaXfer;
        gsaXfer = new InitialGsaXferCommand(ctx, label, dataset, xferConf);

        gsaXfer.scheduleXfer();
    }

    /**
     * Schedules a file transfer to the GSA transfer directory, assuming the
     * {@link ProgramType} warrants it.  Only certain program types (such as
     * queue and classical program) are actually transfered.   See
     * {@link ProgramType} for details.
     */
    public static void xferToGsa(DatamanContext ctx, DatasetLabel label, File dataset) {
        if (!shouldXferToGsa(label)) return;
        GsaXferConfig xferConf = ctx.getConfig().getGsaXferConfig();

        AbstractXferCommand gsaXfer;
        gsaXfer = new GsaXferCommand(ctx, label, dataset, xferConf);

        gsaXfer.scheduleXfer();
    }

    /**
     * Schedules an unconditional file transfer to the base facility test area.
     */
    public static void xferToBase(DatamanContext ctx, DatasetLabel label, File dataset) {
        // Schedule a transfer to the base.
        BaseCopyXferCommand baseXfer;
        XferConfig xferConf = ctx.getConfig().getBaseXferConfig();

        //REL-182: transfer to base should be optional
        if (xferConf != null) {
            baseXfer = new BaseCopyXferCommand(ctx, label, dataset, xferConf);
            baseXfer.scheduleXfer();
        }
    }
}
