package edu.gemini.spModel.dataset;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

/**
 */
public enum DatasetDisposition implements Serializable {
    NONE("None", false),
    UNDEFINED("QA Evaluation", true),
    BAD("QA Fix (Bad Dataset)", true),
    MISSING("QA Fix (Missing Dataset)", true),
    CHECK("CS Evaluation", true),
    COPY_FAILED("HLPG Fix (Copy Failed)", true),
    VERIFY_FAILED("QA Fix (Verify Failed)", true),
    TRANSFER_ERROR("HLPG Fix (Transfer Error)", true),
    REJECTED("QA Investigate (Rejected)", true),

    PENDING("Pending Transfer", false),
    QUEUED("Queued", false),
    TRANSFERING("In Transfer Process", false),
    ACCEPTED("Accepted", false);

    private String _displayString;
    private boolean _attentionNeeded;

    private static final Map<GsaState, DatasetDisposition> DISPO_MAP =
            new HashMap<GsaState, DatasetDisposition>();

    static {
        // could just put these in the GsaStates themselves I suppose
        DISPO_MAP.put(GsaState.NONE,           NONE);
        DISPO_MAP.put(GsaState.PENDING,        PENDING);
        DISPO_MAP.put(GsaState.COPYING,        TRANSFERING);
        DISPO_MAP.put(GsaState.COPY_FAILED,    COPY_FAILED);
        DISPO_MAP.put(GsaState.VERIFYING,      TRANSFERING);
        DISPO_MAP.put(GsaState.VERIFY_FAILED,  VERIFY_FAILED);
        DISPO_MAP.put(GsaState.QUEUED,         QUEUED);
        DISPO_MAP.put(GsaState.TRANSFERRING,   TRANSFERING);
        DISPO_MAP.put(GsaState.TRANSFER_ERROR, TRANSFER_ERROR);
        DISPO_MAP.put(GsaState.REJECTED,       REJECTED);
        DISPO_MAP.put(GsaState.ACCEPTED,       ACCEPTED);

        // Make sure we've covered them all.
        for (GsaState state : GsaState.values()) {
            if (DISPO_MAP.get(state) == null) {
                throw new RuntimeException("Missing mapping for GsaState: " + state);
            }
        }
    }


    private DatasetDisposition(String display, boolean attentionNeeded) {
        _displayString   = display;
        _attentionNeeded = attentionNeeded;
    }

    public String getDisplayString() {
        return _displayString;
    }

    public boolean isAttentionNeeded() {
        return _attentionNeeded;
    }

    public static DatasetDisposition derive(DatasetRecord rec) {
        final DatasetFileState fs = rec.exec.fileState;
        final GsaState gs = rec.exec.gsaState;
        final DatasetQaState qs = rec.qa.qaState;

        if ((fs == DatasetFileState.MISSING) && (gs == GsaState.NONE)) {
            return MISSING;
        }

        if (fs == DatasetFileState.BAD) return BAD;

        if ((fs == DatasetFileState.OK) && (qs == DatasetQaState.UNDEFINED)) {
            return UNDEFINED;
        }

        if ((fs == DatasetFileState.OK) && (qs == DatasetQaState.CHECK)) {
            return CHECK;
        }

        return DISPO_MAP.get(gs);
    }

    public static DatasetDisposition rollUp(Collection<DatasetRecord> recs) {
        if (recs.size() == 0) return NONE;

        DatasetDisposition min = ACCEPTED;
        for (DatasetRecord rec : recs) {
            final DatasetDisposition cur = derive(rec);
            if (cur.compareTo(min) < 0) {
                min = cur;
                if (min == NONE) break;
            }
        }
        return min;
    }
}
