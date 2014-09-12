//
// $
//

package edu.gemini.spModel.telescope;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.data.ISPDataObject;

import java.util.*;
import java.util.logging.Logger;

/**
 * An event monitor that keeps {@link IssPort} {@link IssPortSensitiveComponent
 * sensitive components} up-to-date when the port is changed.  It watches for
 * {@link IssPortProvider}s whose port is updated, as well as the addition of
 * new IssPortProviders.
 *
 * <p>This code is added in the node initializer for the observation and
 * runs in the database when changes are made.  The API for dealing with the
 * science programs declares RemoteExceptions for each method, but these will
 * not be encountered in this code since it is running in the database.
 */
public enum IssPortSync implements ISPEventMonitor {
    instance;

    private static final long serialVersionUID = 1l;

    private static final Logger LOG = Logger.getLogger(IssPortSync.class.getName());

    public void structureChanged(SPStructureChange change) {
//        try {
            Collection<ISPNode> newPortProviders;
            newPortProviders = getNewPortProviders(change);
            if (newPortProviders.size() == 0) return;

            ISPObservation obs = getObservation(change.getModifiedNode());
            for (ISPNode node : newPortProviders) {
                IssPortProvider prov = (IssPortProvider) node.getDataObject();
                update(obs, node, null, prov.getIssPort());
            }
//        } catch (RemoteException ex) {
//            LOG.log(Level.SEVERE, "RemoteException in local code", ex);
//            throw new RuntimeException(ex);
//        }
    }

    public void propertyChanged(SPCompositeChange change) {
        if (!SPUtil.getDataObjectPropertyName().equals(change.getPropertyName())) return;

        ISPNode node = change.getModifiedNode();
//        try {
            // Ignore updates to objects that aren't to a port provider.
            Object dataObj = node.getDataObject();
            if (!(dataObj instanceof IssPortProvider)) return;

            IssPortProvider oldPP = (IssPortProvider) change.getOldValue();
            IssPortProvider newPP = (IssPortProvider) change.getNewValue();

            // Ignore updates that don't change the ISS port.
            IssPort oldPort = oldPP.getIssPort();
            IssPort newPort = newPP.getIssPort();
            if (oldPort == newPort) return;

            // Update all the port sensitive nodes in the observation.
            update((ISPObservation) node.getParent(), node, oldPort, newPort);
//        } catch (RemoteException ex) {
//            LOG.log(Level.SEVERE, "Remote exception in local method call.", ex);
//            throw new RuntimeException(ex);
//        }
    }

    private Set<ISPNode> extractChangedNodes(Object val) {
        Set<ISPNode> res = new HashSet<ISPNode>();

        if (val instanceof ISPNode) {
            res.add((ISPNode) val);
        } else if (val instanceof Collection) {
            //noinspection unchecked
            res.addAll((Collection<ISPNode>) val);
        }
        return res;
    }

    private Collection<ISPNode> getNewPortProviders(SPStructureChange change)  {
        ISPNode node = change.getModifiedNode();

        Set<ISPNode> oldChildren = extractChangedNodes(change.getOldValue());
        Set<ISPNode> newChildren = extractChangedNodes(change.getNewValue());

        // Figure out which children are new.
        newChildren.removeAll(oldChildren);

        // Filter out those that are not IssPortProviders.
        Iterator<ISPNode> it = newChildren.iterator();
        while (it.hasNext()) {
            Object dataObj = it.next().getDataObject();
            if (!(dataObj instanceof IssPortProvider)) it.remove();
        }

        return newChildren;
    }

    private void update(ISPObservation obs, ISPNode portProvider, IssPort oldPort, IssPort newPort)  {
        Collection<ISPNode> nodes = getSensitiveNodes(obs);
        if (nodes.size() == 0) return; // nothing to do

        for (ISPNode node : nodes) {
            IssPortSensitiveComponent sen;
            sen = (IssPortSensitiveComponent) node.getDataObject();
            sen.handleIssPortUpdate(portProvider, oldPort, newPort);
            node.setDataObject((ISPDataObject) sen);
        }
    }

    private Collection<ISPNode> getSensitiveNodes(ISPObservation obs)  {
        Collection<ISPNode> res = new ArrayList<ISPNode>();
        addSensitiveObsComponents(obs, res);

        ISPSeqComponent seqRoot = obs.getSeqComponent();
        if (seqRoot != null) addSensitiveSeqComponents(seqRoot, res);

        return res;
    }

    private void addSensitiveObsComponents(ISPObservation obs, Collection<ISPNode> comps)  {
        for (ISPObsComponent obsComp : obs.getObsComponents()) {
            Object dObj = obsComp.getDataObject();
            if (dObj instanceof IssPortSensitiveComponent) {
                comps.add(obsComp);
            }
        }
    }

    private void addSensitiveSeqComponents(ISPSeqComponent root, Collection<ISPNode> comps)  {
        Object dObj = root.getDataObject();
        if (dObj instanceof IssPortSensitiveComponent) {
            comps.add(root);
        }

        for (ISPSeqComponent child : root.getSeqComponents()) {
            addSensitiveSeqComponents(child, comps);
        }
    }

    private ISPObservation getObservation(ISPNode node)  {
        if (node instanceof ISPObservation) return (ISPObservation) node;
        return getObservation(node.getParent());
    }
}
