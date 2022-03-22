package jsky.app.ot.shared.progstate;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.sp.version.LifespanId;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.VersionVector;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.obslog.ObsExecLog;

import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fetch information about any leaf nodes with a key in the provided collection
 * of keys.
 */
public final class LeafNodeFetchFunctor extends DBAbstractFunctor {
    private static final Logger Log = Logger.getLogger(LeafNodeFetchFunctor.class.getName());

    private final SPProgramID progId;
    private final Collection<SPNodeKey> leafKeys = new ArrayList<>();
    private Map<SPNodeKey, LeafNodeData> result  = Collections.emptyMap();

    public LeafNodeFetchFunctor(SPProgramID progId, Collection<SPNodeKey> leafKeys) {
        if (progId == null) throw new NullPointerException("progId is null");
        this.progId = progId;
        this.leafKeys.addAll(leafKeys);
    }

    private LeafNodeData leafNodeData(ISPNode node) {
        final SPNodeKey key = node.getNodeKey();

        node.getProgramReadLock();
        try {
            final ISPDataObject dataObject              = node.getDataObject();
            final VersionVector<LifespanId, Integer> vv = node.getProgram().getVersions(key);
            return new LeafNodeData(key, dataObject, vv);
        } finally {
            node.returnProgramReadLock();
        }
    }

    private void search(ISPNode node, Set<SPNodeKey> remaining, Map<SPNodeKey, LeafNodeData> result) {
        if (!remaining.isEmpty()) {
            if (node instanceof ISPContainerNode) {
                final ISPContainerNode parent = (ISPContainerNode) node;
                for (ISPNode child : parent.getChildren()) {
                    search(child, remaining, result);
                }
            } else if (remaining.remove(node.getNodeKey())) {
                final LeafNodeData data = leafNodeData(node);
                result.put(node.getNodeKey(), data);

                // REL-4013: Logging for missing start/end visit events
                if (data.dataObject instanceof ObsExecLog) {
                    final ObsExecLog log = (ObsExecLog) data.dataObject;

                    final StringBuilder buf = new StringBuilder();
                    buf.append(String.format("OT requested update for: %s\n", node.getContextObservationId().map(SPObservationID::stringValue).getOrElse("<unknown>")));
                    buf.append(String.format("Versions: %s\n", data.versions));
                    buf.append(log.getFormattedVisitEvents());
                    Log.log(Level.INFO, buf.toString());
                }
            }
        }
    }

    @Override public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        final Map<SPNodeKey, LeafNodeData> result = new HashMap<>();
        final Set<SPNodeKey> keys = new HashSet<>(leafKeys);
        final ISPProgram p = db.lookupProgramByID(progId);
        if (p != null) search(p, keys, result);
        this.result = result;
    }

    public Map<SPNodeKey, LeafNodeData> getResult() { return result; }
}
