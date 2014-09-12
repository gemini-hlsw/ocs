package edu.gemini.spdb.shell.misc;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obs.ObservationStatus;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This functor gets all the programs from one specific semester, and
 * generates a report that contains the following format:
 * <p/>
 * Gemini ID   Gemini Contact Scientist e-mail     NGO contact e-mail
 */
public class InfoDumpFunctor extends DBAbstractQueryFunctor {

    private final List<String> _messages = new ArrayList<String>();
    private final String _semester;

    private InfoDumpFunctor() {
        _semester = "2011A";
        _messages.add(" GEMINI-ID#Instrument(s)#PI Name#PI email");
    }

    public void execute(final IDBDatabaseService db, final ISPNode node, Set<Principal> principals) {
        final ISPProgram iprog = (ISPProgram) node;

            // only consider programs related to the semester we are interest
            if (iprog.getProgramID() == null) return;

            if (!iprog.getProgramID().toString().contains(_semester)) {
                return;
            }

            final SPProgram prog = (SPProgram) iprog.getDataObject();
            if (prog == null) {
                return;
            }
            final SPProgram.PIInfo pi = prog.getPIInfo();
            final String piName = pi.getLastName() + ", " + pi.getFirstName();

            String piEmail = pi.getEmail();
            if (piEmail == null) {
                piEmail = "undefined";
            }
            final Set<String> inst = new HashSet<String>();
            for (final ISPObservation iobs : iprog.getAllObservations()) {
                if (ObservationStatus.computeFor(iobs) == ObservationStatus.READY) {
                    for (final ISPObsComponent obsComp : iobs.getObsComponents()) {
                        final SPComponentType type = obsComp.getType();
                        if (type.broadType == SPComponentBroadType.INSTRUMENT) {
                            inst.add(type.readableStr);
                        }
                    }
                }
            }
            _messages.add(iprog.getProgramID().toString() + "#" + inst.toString() + "#" + piName + "#" + piEmail);

    }

}
