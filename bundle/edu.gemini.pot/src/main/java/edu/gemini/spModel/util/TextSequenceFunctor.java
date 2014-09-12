//
// $Id: TextSequenceFunctor.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.util;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.config.ObservationCB;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.type.DisplayableSpType;


import java.security.Principal;
import java.util.*;


/**
 * Creates a tabular text description of a given observation sequence,
 * suitable for display in a JTable. The result of executing this
 * functor on an observation is a data vector containing the rows to
 * be displayed in the table.
 */
public class TextSequenceFunctor extends DBAbstractFunctor {

    // Contains 2 column table data for the sequence, suitable for display in a JTable
    private Vector _dataVector;

    // If there is a remote exception , it is saved here
//    private RemoteException _exception;

    // Count of observes, starting with 1
    private int _count = 0;

    // For formatting
    private static final String _indentSize = "    ";

    // Set to true if on site
    private boolean _isOnSite;


    public TextSequenceFunctor(boolean isOnSite) {
        _isOnSite = isOnSite;
    }

    public void execute(IDBDatabaseService database, ISPNode node, Set<Principal> principals) {
//        try {
            _makeDataVector((ISPObservation) node);
//        } catch (RemoteException ex) {
//            _exception = ex;
//            _dataVector = null;
//        }
    }

    // Generate the data vector result from the observation's config data
    private void _makeDataVector(ISPObservation obs)  {
        _dataVector = new Vector();
        IConfigBuilder cb = (IConfigBuilder) obs.getClientData(IConfigBuilder.USER_OBJ_KEY);

        IConfig full = new DefaultConfig();
        ISysConfig cache = new DefaultSysConfig("");
        List l = new ArrayList();
        cb.reset(ObservationCB.getDefaultSequenceOptions(null));
        while (cb.hasNext()) {
            IConfig config = new CachedConfig(cache);
            cb.applyNext(config, full);
            full.mergeSysConfigs(config);
            l.add(config);
        }

        // OT-516: This delays adding the step, allowing the status parameter value to be
        // modified first, if needed. See GemObservationCB.ObsContext._checkStatus().
        Iterator it = l.iterator();
        while (it.hasNext()) {
            _addSteps((IConfig)it.next());
        }
    }


    /**
     * Add steps to the table for the given top level IConfig object
     */
    private void _addSteps(IConfig config) {
        Iterator<ISysConfig> it = config.getSysConfigs().iterator();
        while (it.hasNext()) {
            ISysConfig sc = it.next();
            if (sc.isMetadata()) continue;
            _addStep(sc);
        }
    }

    /**
     * Add a step to the table for the given ISysConfig object
     */
    private void _addStep(ISysConfig sysConfig) {
        String name = sysConfig.getSystemName();
        if (name.equals("observe")) {
            _count++;
            name = name + "(" + _count + ")";

        } else if (name.equals("instrument")) {
            String instName = (String) sysConfig.getParameterValue("instrument");
            if (instName != null) {
                name = name + "(" + instName + ")";
            }
        }
        // Add a row with just the system name
        Vector row = new Vector(2);
        row.add(name);
        row.add("");
        _addRow(row);

        _addParameters("", sysConfig);
    }

    // This method adds all the parameters in an ISysConfig
    private void _addParameters(String indent, ISysConfig sc) {
        // Add the parameters for this system config recurse if IParameter is an IConfigParameter
        Iterator i = sc.getParameters().iterator();
        while (i.hasNext()) {
            IParameter ip = (IParameter) i.next();
            if (ip instanceof IConfigParameter) {
                _addParameter(indent, (IConfigParameter) ip);
            } else {
                _addParameter(indent, ip);
            }
        }
    }

    private void _addParameter(String indent, IConfigParameter cp) {
        // First create the special Config Name row, then recurse
        Vector row = new Vector(2);
        row.add("");
        row.add(indent + cp.getName());
        _addRow(row);
        indent += _indentSize;
        ISysConfig sc = (ISysConfig) cp.getValue();
        _addParameters(indent, sc);
    }

    private void _addParameter(String indent, IParameter p) {
        String name = p.getName();
        Object val  = p.getValue();
        if (val == null) return;

        // ignore observe status parameter unless onsite
        // (XXX could cause problems if other "status" params are later added XXX)
        if (!_isOnSite && "status".equals(name)) return;

        String strVal;
        if (val instanceof DisplayableSpType) {
            strVal = ((DisplayableSpType) val).displayValue();
        } else {
            strVal = val.toString();
        }
        if (strVal.length() == 0) return; // not sure this is needed
            Vector<String> row = new Vector<String>(2);
            row.add("");
            row.add(indent + name + " = " + strVal);
            _addRow(row);
    }

    // Add a row to the table.
    private void _addRow(Vector v) {
        _dataVector.add(v);
    }


    /**
     * Return a data vector suitable for use in a JTable
     */
    public Vector getDataVector() {
        return _dataVector;
    }

//    /**
//     * Gets any RemoteException that was thrown during the execution of the
//     * functor.
//     */
//    public RemoteException getRemoteException() {
//        return _exception;
//    }
}
