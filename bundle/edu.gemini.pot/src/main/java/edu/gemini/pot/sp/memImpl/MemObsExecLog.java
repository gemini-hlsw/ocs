package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.event.EndVisitEvent;
import edu.gemini.spModel.event.StartVisitEvent;
import edu.gemini.spModel.obslog.ObsExecLog;

import java.util.logging.Logger;

import static edu.gemini.pot.sp.SPComponentType.OBS_EXEC_LOG;

public final class MemObsExecLog extends MemProgramNodeBase implements ISPObsExecLog {
    private static final Logger LOG = Logger.getLogger(MemObsExecLog.class.getName());

    private final MemProgram program;

    MemObsExecLog(MemProgram prog, SPNodeKey key) {
        super(prog.getDocumentData(), key);
        program = prog;
    }

    MemObsExecLog(MemProgram prog, ISPObsExecLog log, boolean preserveKeys) {
        super(prog.getDocumentData(), log, preserveKeys);
        program = prog;
    }

    @Override public SPComponentType getType() { return OBS_EXEC_LOG; }

    @Override public void accept(ISPProgramVisitor visitor) {
        visitor.visitObsExecLog(this);
    }

    @Override public MemProgram getProgram() { return program; }

    // Start REL-3986 ------ logging to debug an issue with missing events
    private String formatEvents(ObsExecLog log) {
        final StringBuilder buf = new StringBuilder();
        log.getRecord().getAllEventList().forEach(e -> {
            if ((e instanceof StartVisitEvent) || (e instanceof EndVisitEvent)) {
                buf.append(String.format("\t%s\n", e));
            }
        });
        return buf.toString();
    }

    private Option<String> formatEvents(String name, Object obj) {

        return (DATA_OBJECT_KEY.equals(name) && (obj instanceof ObsExecLog)) ?
                new Some<>(formatEvents((ObsExecLog) obj))                   :
                ImOption.empty();

    }

    private Option<String> getObsId() {
        return ImOption.apply(getContextObservation())
                       .flatMap(o -> ImOption.apply(o.getObservationID()))
                       .map(SPObservationID::stringValue);
    }

    @Override public PropagationId putClientData(String name, Object obj) {
        getProgramWriteLock();

        try {
            final Option<String> oid = getObsId();
            final StringBuilder  buf = new StringBuilder();

            oid.foreach(id -> {
                buf.append(String.format("Updating ObsExecRecord for %s\n", id));
                buf.append(String.format("LifespanId: %s\n", getDocumentData().getLifespanId()));
                buf.append(String.format("Before....: %s\n", getDocumentData().versionVector(this.getNodeKey())));
                buf.append(formatEvents(name, getDataObject()).getOrElse("\n"));
            });

            final PropagationId propid = super.putClientData(name, obj);

            oid.foreach(id -> {
                buf.append(String.format("After.....: %s\n", getDocumentData().versionVector(this.getNodeKey())));
                buf.append(formatEvents(name, obj).getOrElse("\n"));

                LOG.info(buf.toString());
            });

            return propid;
        } finally {
            returnProgramWriteLock();
        }
    }

    // End REL-3986

}
