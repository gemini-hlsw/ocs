package jsky.app.ot.shared.progstate;

import edu.gemini.pot.sp.ISPContainerNode;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.sp.version.LifespanId;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.VersionVector;
import edu.gemini.spModel.core.SPProgramID;

import java.security.Principal;
import java.util.*;

/**
 * Fetch information about any leaf nodes with a key in the provided collection
 * of keys.
 */
public final class LeafNodeFetchFunctor extends DBAbstractFunctor {
    private final SPProgramID progId;
    private final Collection<SPNodeKey> leafKeys = new ArrayList<SPNodeKey>();
    private Map<SPNodeKey, LeafNodeData> result  = Collections.emptyMap();

    public LeafNodeFetchFunctor(SPProgramID progId, Collection<SPNodeKey> leafKeys) {
        if (progId == null) throw new NullPointerException("progId is null");
        this.progId = progId;
        this.leafKeys.addAll(leafKeys);
    }

    private void search(ISPNode node, Set<SPNodeKey> remaining, Map<SPNodeKey, LeafNodeData> result) {
        if (!remaining.isEmpty()) {
            if (node instanceof ISPContainerNode) {
                final ISPContainerNode parent = (ISPContainerNode) node;
                for (ISPNode child : parent.getChildren()) {
                    search(child, remaining, result);
                }
            } else if (remaining.remove(node.getNodeKey())) {
                final SPNodeKey key = node.getNodeKey();
                final VersionVector<LifespanId, Integer> vv = node.getProgram().getVersions(key);
                result.put(key, new LeafNodeData(key, node.getDataObject(), vv));
            }
        }
    }

    @Override public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        final Map<SPNodeKey, LeafNodeData> result = new HashMap<SPNodeKey, LeafNodeData>();
        final Set<SPNodeKey> keys = new HashSet<SPNodeKey>(leafKeys);
        final ISPProgram p = db.lookupProgramByID(progId);
        if (p != null) search(p, keys, result);
        this.result = result;
    }

    public Map<SPNodeKey, LeafNodeData> getResult() { return result; }
}