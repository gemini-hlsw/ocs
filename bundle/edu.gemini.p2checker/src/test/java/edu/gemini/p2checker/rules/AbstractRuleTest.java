package edu.gemini.p2checker.rules;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBIDClashException;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.ghost.Ghost;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatFlatObs;
import edu.gemini.spModel.seqcomp.SeqRepeatDarkObs;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.List;

/**
 * Class AbstractRuleTest
 *
 * @author Nicolas A. Barriga
 *         Date: 4/14/11
 */
@Ignore
public class AbstractRuleTest {
    private final SPNodeKey PROG_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d47");
    protected IDBDatabaseService db;
    protected ISPFactory fact;
    protected ISPProgram prog;
    private List<ISPObsComponent> obscomps;
    protected ISPObservation obs;


    @Before
    public void setUp() throws Exception {
        db = DBLocalDatabase.createTransient();
        fact = db.getFactory();
        prog = createBasicProgram();
    }

    @After
    public void tearDown() throws Exception {
        db.getDBAdmin().shutdown();
    }

    private ISPObsComponent createObsComp(AbstractDataObject dataObj) throws SPUnknownIDException {
        return createObsComp(prog, dataObj);
    }

    private ISPObsComponent createObsComp(ISPProgram prog, AbstractDataObject dataObj) throws SPUnknownIDException {
        ISPObsComponent obscomp = fact.createObsComponent(prog, dataObj.getType(), null);
        obscomp.setDataObject(dataObj);
        return obscomp;
    }

    protected ISPProgram createBasicProgram() throws SPException, DBIDClashException {
        return createBasicProgram(null);
    }

    protected ISPProgram createBasicProgram(String programIdString) throws SPException, DBIDClashException {
        final SPProgramID pid;
        try {
            pid = (programIdString == null) ? null : SPProgramID.toProgramID(programIdString);
        } catch (SPBadIDException e) {
            throw new RuntimeException(e);
        }
        ISPProgram prog = fact.createProgram(PROG_KEY, pid);
        db.put(prog);

        obs = fact.createObservation(prog, Instrument.none, null);

        List<ISPObservation> observations = new ArrayList<ISPObservation>();
        observations.add(obs);
        prog.setObservations(observations);

        obscomps = new ArrayList<ISPObsComponent>();
        obs.setObsComponents(obscomps);

        ISPSeqComponent sequenceRoot = fact.createSeqComponent(prog, SPComponentType.ITERATOR_BASE, null);
        obs.setSeqComponent(sequenceRoot);

        return prog;
    }

    /**
     * Add an Altair component of the specified type
     *
     * @param mode (LGS or NGS)
     * @throws edu.gemini.pot.sp.SPUnknownIDException
     * @throws java.rmi.RemoteException
     * @throws edu.gemini.pot.sp.SPTreeStateException
     * @throws edu.gemini.pot.sp.SPNodeNotLocalException
     */
    protected ISPObsComponent addAltair(AltairParams.Mode mode) throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        InstAltair altair = new InstAltair();
        altair.setMode(mode);
        ISPObsComponent altairObsComp = createObsComp(altair);
        obscomps.add(altairObsComp);
//        obs.putClientData(IConfigBuilder.USER_OBJ_KEY, new InstAltairCB(altairObsComp));
        obs.setObsComponents(obscomps);
        return altairObsComp;
    }

    /**
     * Add a GMOS north component
     *
     * @throws edu.gemini.pot.sp.SPUnknownIDException
     * @throws java.rmi.RemoteException
     * @throws edu.gemini.pot.sp.SPTreeStateException
     * @throws edu.gemini.pot.sp.SPNodeNotLocalException
     */
    protected ISPObsComponent addGmosNorth() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        final ISPObsComponent gmosComponent = fact.createObsComponent(prog, InstGmosNorth.SP_TYPE, null);
        obscomps.add(gmosComponent);
        obs.setObsComponents(obscomps);
        return gmosComponent;
    }

    protected ISPObsComponent addGmosSouth() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        final ISPObsComponent gmosComponent = fact.createObsComponent(prog, InstGmosSouth.SP_TYPE, null);
        obscomps.add(gmosComponent);
        obs.setObsComponents(obscomps);
        return gmosComponent;
    }

    protected ISPObsComponent addNiri() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        ISPObsComponent obsComp = createObsComp(new InstNIRI());
        obscomps.add(obsComp);
        obs.setObsComponents(obscomps);
        return obsComp;
    }

    protected ISPObsComponent addMichelle() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        final ISPObsComponent obsComp = createObsComp(new InstMichelle());
        obscomps.add(obsComp);
        obs.setObsComponents(obscomps);
        return obsComp;
    }

    protected ISPObsComponent addGhost() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        final ISPObsComponent obsComp = createObsComp(new Ghost());
        obscomps.add(obsComp);
        obs.setObsComponents(obscomps);
        return obsComp;
    }

    protected void addSimpleScienceObserve() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        addSimpleScienceObserve(1);
    }

    protected void addSimpleScienceObserve(int count) throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        assert (obs != null);
        ISPSeqComponent observeSeqComponent = fact.createSeqComponent(prog, SeqRepeatObserve.SP_TYPE, null);
        SeqRepeatObserve seqObserve = (SeqRepeatObserve) observeSeqComponent.getDataObject();
        seqObserve.setStepCount(count);
        observeSeqComponent.setDataObject(seqObserve);
        obs.getSeqComponent().addSeqComponent(observeSeqComponent);
    }

    protected void addSiteQuality() throws SPUnknownIDException, SPNodeNotLocalException, SPTreeStateException {
        obscomps.add(fact.createObsComponent(prog, SPSiteQuality.SP_TYPE, null));
        obs.setObsComponents(obscomps);
    }

    protected void addSiteQuality(SPSiteQuality.ImageQuality imageQ) throws SPUnknownIDException, SPNodeNotLocalException, SPTreeStateException {
        addSiteQuality(imageQ, SPSiteQuality.CloudCover.DEFAULT, SPSiteQuality.SkyBackground.DEFAULT, SPSiteQuality.WaterVapor.DEFAULT);
    }

    protected void addSiteQuality(SPSiteQuality.ImageQuality imageQ,
                                  SPSiteQuality.CloudCover cloudCover,
                                  SPSiteQuality.SkyBackground skyBG,
                                  SPSiteQuality.WaterVapor waterVapor) throws SPUnknownIDException, SPNodeNotLocalException, SPTreeStateException {

        SPSiteQuality sq = new SPSiteQuality();
        sq.setImageQuality(imageQ);
        sq.setCloudCover(cloudCover);
        sq.setSkyBackground(skyBG);
        sq.setWaterVapor(waterVapor);
        ISPObsComponent siteQualityComponent = createObsComp(sq);
        obscomps.add(siteQualityComponent);
        obs.setObsComponents(obscomps);
    }

    protected void addSimpleDarkObserve(int count, double exposureTime, int coadds) throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        assert (obs != null);
        ISPSeqComponent observeSeqComponent = fact.createSeqComponent(prog, SeqRepeatDarkObs.SP_TYPE, null);
        SeqRepeatDarkObs seqObserve = (SeqRepeatDarkObs) observeSeqComponent.getDataObject();
        seqObserve.setStepCount(count);
        seqObserve.setExposureTime(exposureTime);
        seqObserve.setCoaddsCount(coadds);
        observeSeqComponent.setDataObject(seqObserve);
        obs.getSeqComponent().addSeqComponent(observeSeqComponent);
    }

    protected void addSimpleFlatObserve(int count, double exposureTime, int coadds) throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        assert (obs != null);
        ISPSeqComponent observeSeqComponent = fact.createSeqComponent(prog, SeqRepeatFlatObs.SP_TYPE, null);
        SeqRepeatFlatObs seqObserve = (SeqRepeatFlatObs) observeSeqComponent.getDataObject();
        seqObserve.setStepCount(count);
        seqObserve.setExposureTime(exposureTime);
        seqObserve.setCoaddsCount(coadds);
        observeSeqComponent.setDataObject(seqObserve);
        obs.getSeqComponent().addSeqComponent(observeSeqComponent);

    }

    /**
     * Add a TargetObsComp with an AOWFS and a P1WFS target
     *
     * @throws edu.gemini.pot.sp.SPUnknownIDException
     * @throws java.rmi.RemoteException
     * @throws edu.gemini.pot.sp.SPTreeStateException
     * @throws edu.gemini.pot.sp.SPNodeNotLocalException
     */
    protected void addTargetObsCompAOP1() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        TargetObsComp target = new TargetObsComp();
        SPTarget sptarget = new SPTarget();
        sptarget.setName("name");

        GuideEnvironment guide = GuideEnvironment.create(
                OptionsListImpl.create(
                        GuideGroup.create(
                                "group",
                                GuideProbeTargets.create(
                                        PwfsGuideProbe.pwfs1,
                                        sptarget)
                        )
                )
        );
        final TargetEnvironment env = TargetEnvironment.create(sptarget, guide, ImCollections.<UserTarget>emptyList());

        target.setTargetEnvironment(env);
        ISPObsComponent targetObsComp = createObsComp(target);
        obscomps.add(targetObsComp);
        //obs.putClientData(IConfigBuilder.USER_OBJ_KEY, new TargetObsCompCB(targetObsComp));
        obs.setObsComponents(obscomps);
    }

    /**
     * Add a TargetObsComp with an AOWFS target
     *
     * @throws edu.gemini.pot.sp.SPUnknownIDException
     * @throws java.rmi.RemoteException
     * @throws edu.gemini.pot.sp.SPTreeStateException
     * @throws edu.gemini.pot.sp.SPNodeNotLocalException
     */
    protected void addTargetObsCompAO() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        TargetObsComp target = new TargetObsComp();
        SPTarget sptarget = new SPTarget();
        sptarget.setName("name");

        GuideEnvironment guide = GuideEnvironment.create(
                OptionsListImpl.create(
                        GuideGroup.create(
                                "group",
                                GuideProbeTargets.create(
                                        AltairAowfsGuider.instance,
                                        sptarget)
                        )
                )
        );
        final TargetEnvironment env = TargetEnvironment.create(sptarget, guide, ImCollections.<UserTarget>emptyList());

        target.setTargetEnvironment(env);
        ISPObsComponent targetObsComp = createObsComp(target);
        obscomps.add(targetObsComp);
        //obs.putClientData(IConfigBuilder.USER_OBJ_KEY, new TargetObsCompCB(targetObsComp));
        obs.setObsComponents(obscomps);
    }

    /**
     * Add a TargetObsComp with no targets
     *
     * @throws edu.gemini.pot.sp.SPUnknownIDException
     * @throws java.rmi.RemoteException
     * @throws edu.gemini.pot.sp.SPTreeStateException
     * @throws edu.gemini.pot.sp.SPNodeNotLocalException
     */
    protected void addTargetObsCompEmpty() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        TargetObsComp target = new TargetObsComp();

        ISPObsComponent targetObsComp = createObsComp(target);
        obscomps.add(targetObsComp);
        //obs.putClientData(IConfigBuilder.USER_OBJ_KEY, new TargetObsCompCB(targetObsComp));
        obs.setObsComponents(obscomps);


    }

    private ISPSeqComponent addGmosIteratorFor(SPComponentType ct) throws Exception {
        final ISPSeqComponent sq = fact.createSeqComponent(prog, ct, null);
        obs.getSeqComponent().addSeqComponent(sq);
        return sq;
    }

    protected ISPSeqComponent addGmosIterator() throws Exception {
        return addGmosIteratorFor(SPComponentType.ITERATOR_GMOS);
    }

    protected ISPSeqComponent addGmosSouthIterator() throws Exception {
        return addGmosIteratorFor(SPComponentType.ITERATOR_GMOSSOUTH);
    }

    protected ISPSeqComponent addOffsetIterator() throws Exception {
        final ISPSeqComponent sq = fact.createSeqComponent(prog, SPComponentType.ITERATOR_OFFSET, null);
        obs.getSeqComponent().addSeqComponent(sq);
        return sq;
    }
}
