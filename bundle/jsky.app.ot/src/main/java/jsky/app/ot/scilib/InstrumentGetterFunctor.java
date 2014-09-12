package jsky.app.ot.scilib;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.GeminiRuntimeException;
import edu.gemini.spModel.util.SPTreeUtil;
import jsky.app.ot.OT;


import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Functor to retrieve the instruments used by a given program.
 */
public class InstrumentGetterFunctor extends DBAbstractFunctor {
    private static final Logger LOG = Logger.getLogger(InstrumentGetterFunctor.class.getName());

    private final Set<SPComponentType> _instruments = new HashSet<SPComponentType>();

    public void execute(final IDBDatabaseService db, final ISPNode node, Set<Principal> principals) {
        if (node instanceof ISPProgram) {
            final ISPProgram prog = (ISPProgram)node;
//            try {
                addTemplateInstruments(prog);
                addInstruments(prog);
//            } catch (RemoteException e) {
//                 throw GeminiRuntimeException.newException("got a remote exception in local mode", e);
//            }
        }
    }

    private void addInstruments(final ISPObservationContainer c)  {
        for (final ISPObservation obs : c.getAllObservations()) {
            final ISPObsComponent instrument = SPTreeUtil.findInstrument(obs);
            if (instrument != null) {
                _instruments.add(instrument.getType());
            }
        }
    }

    private void addTemplateInstruments(final ISPProgram program)  {
        final ISPTemplateFolder f = program.getTemplateFolder();
        if (f == null) return;
        for (final ISPTemplateGroup grp : f.getTemplateGroups()) addInstruments(grp);
    }

    private List<SPComponentType> getInstruments() {
        return new ArrayList<SPComponentType>(_instruments);
    }

    /**
     * Returns the instruments (no duplicates) used by the observations of the given program
     * @param node The program remote node
     * @return a list of {@code SPComponentType} with the types of the instruments
     */
    public static List<SPComponentType> getInstruments(final ISPNode node)  {
        final IDBDatabaseService db = SPDB.get();
        InstrumentGetterFunctor functor = new InstrumentGetterFunctor();
        try {
            functor = db.getQueryRunner(OT.getUser()).execute(functor, node);

            if (functor.getException() != null) {
                LOG.log(Level.WARNING, "Problem getting instruments for library fetch", functor.getException());
            }

            return functor.getInstruments();
        } catch (SPNodeNotLocalException e) {
            throw GeminiRuntimeException.newException(e);
        }
    }

}