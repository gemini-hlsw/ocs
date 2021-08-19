// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.test;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import junit.framework.TestCase;

import java.util.Objects;
import java.util.Optional;

public abstract class OdbTestBase extends TestCase {

    private IDBDatabaseService odb;

    private SPNodeKey progKey;
    private ISPProgram prog;
    private ISPObservation obs;

    private ISPObsComponent target;

    public void setUp(SPProgramID progId) throws Exception {
        super.setUp();

        odb     = DBLocalDatabase.createTransient();
        prog    = odb.getFactory().createProgram(new SPNodeKey(), progId);
        odb.put(prog);
        progKey = prog.getProgramKey();

        obs     = odb.getFactory().createObservation(prog, Instrument.none, null);

        prog.addObservation(obs);

        target  = getObsComponent(obs, TargetObsComp.SP_TYPE);
    }

    public void setUp() throws Exception {
        setUp(null);
    }

    public void tearDown() throws Exception {
        odb.getDBAdmin().shutdown();
        super.tearDown();
    }

    protected IDBDatabaseService getOdb() { return odb; }
    protected ISPFactory getFactory() { return odb.getFactory(); }

    protected ISPObsComponent addObsComponent(SPComponentType type) throws Exception {
        ISPObsComponent comp = odb.getFactory().createObsComponent(prog, type, null);
        obs.addObsComponent(comp);
        return comp;
    }

    protected void removeObsComponent(SPComponentType type) {
        Optional.ofNullable(getObsComponent(type))
                .ifPresent(c -> obs.removeObsComponent(c));
    }

    protected SPNodeKey getProgKey() {
        return progKey;
    }

    protected ISPProgram getProgram() {
        return prog;
    }

    protected ISPObservation getObs() {
        return obs;
    }

    protected ISPObsComponent getObsComponent(SPComponentType type) {
        return getObsComponent(obs, type);
    }

    protected static ISPObsComponent getObsComponent(ISPObservation obs, final SPComponentType type) {
        return obs.getObsComponents()
                .stream()
                .filter(c -> type.equals(c.getType()))
                .findFirst()
                .orElse(null);
    }

    protected ISPObsComponent getTarget() {
        return target;
    }

    protected TargetEnvironment getTargetEnvironment() {
        return Optional
            .ofNullable((TargetObsComp) target.getDataObject())
            .map(TargetObsComp::getTargetEnvironment)
            .orElse(null);
    }

    protected ISPSeqComponent addSeqComponent(ISPSeqComponent parent, SPComponentType type) throws Exception {
        ISPSeqComponent comp = odb.getFactory().createSeqComponent(prog, type, null);
        parent.addSeqComponent(comp);
        return comp;
    }

    protected ISPSeqComponent getSeqComponent(SPComponentType type) {
        return getSeqComponent(obs, type);
    }

    protected static ISPSeqComponent getSeqComponent(ISPSeqComponent root, SPComponentType type) {
        if (type.equals(root.getType())) return root;

        return root.getSeqComponents()
                .stream()
                .map(c -> getSeqComponent(c, type))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    protected static ISPSeqComponent getSeqComponent(ISPObservation obs, SPComponentType type) {
        return getSeqComponent(obs.getSeqComponent(), type);
    }

}
