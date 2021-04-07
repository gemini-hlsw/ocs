package edu.gemini.qpt.shared.sp;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBQueryRunner;
import edu.gemini.spModel.core.*;
import edu.gemini.qpt.shared.sp.ServerExclusion.*;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.util.security.auth.keychain.KeyChain;
import edu.gemini.util.trpc.client.TrpcClient$;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * The mini model is a condensed snapshot of the ODB program and observation data and can be used
 * by clients that want to be shielded from all the details of the SP model and don't need to know
 * about changes either. The QPT and the QV tool both use this model.
 * Note: The date passed down denotes the date on which the night ends (sunrise), e.g. 2014-01-15 for the night
 * starting on 2014-01-14 (sunset) and ending on 2014-01-15 (sunrise).
 */
public class MiniModel {

    /** Set of observation classes that are considered to be relevant by default (QPT). */
    private static final Set<ObsClass> RELEVANT_OBS_CLASSES = new HashSet<>();
    static {
        RELEVANT_OBS_CLASSES.add(ObsClass.PARTNER_CAL);
        RELEVANT_OBS_CLASSES.add(ObsClass.PROG_CAL);
        RELEVANT_OBS_CLASSES.add(ObsClass.DAY_CAL);
        RELEVANT_OBS_CLASSES.add(ObsClass.SCIENCE);
    }
    /** Set of observation statuses that are considered to be relevant by default (QPT). */
    private static final Set<ObservationStatus> RELEVANT_OBS_STATUSES = new HashSet<>();
    static {
        RELEVANT_OBS_STATUSES.add(ObservationStatus.READY);
        RELEVANT_OBS_STATUSES.add(ObservationStatus.ONGOING);
    }

    private final SortedSet<Prog> programs;
    private final SortedSet<Obs> allObservations;
    private final SortedSet<String> misconfiguredObservations;
    private final SortedSet<String> allSemesters;
    private final Map<String, Obs> obsMap = new TreeMap<>();
    private final Site site;
    private final long timestamp = System.currentTimeMillis();
    private final Map<SPProgramID, ProgramExclusion> programExclusions;
    private final Map<SPObservationID, ObsExclusion> obsExclusions;

    private MiniModel(Site site,
                      SortedSet<Prog> programs,
                      SortedSet<String> misconfiguredObservations,
                      SortedSet<String> allSemesters,
                      Map<SPProgramID, ProgramExclusion> programExclusions,
                      Map<SPObservationID, ObsExclusion> obsExclusions) {
        this.site = site;
        this.programs = Collections.unmodifiableSortedSet(new TreeSet<>(programs));
        this.misconfiguredObservations = Collections.unmodifiableSortedSet(new TreeSet<>(misconfiguredObservations));
        this.allSemesters = Collections.unmodifiableSortedSet(new TreeSet<>(allSemesters));
        this.programExclusions = Collections.unmodifiableMap(programExclusions);
        this.obsExclusions = Collections.unmodifiableMap(obsExclusions);
        SortedSet<Obs> accum = new TreeSet<>();
        for (Prog prog: programs) accum.addAll(prog.getFullObsSet());
        allObservations = Collections.unmodifiableSortedSet(accum);
        for (Obs obs: allObservations) obsMap.put(obs.getObsId(), obs);
    }

    public static MiniModel empty(Site site) {
        return new MiniModel(
                site,
                Collections.emptySortedSet(),
                Collections.emptySortedSet(),
                Collections.emptySortedSet(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    public SortedSet<Prog> getPrograms() {
        return programs;
    }

    public Site getSite() {
        return site;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public SortedSet<Obs> getAllObservations() {
        return allObservations;
    }

    public Obs getObs(String obsId) {
        return obsMap.get(obsId);
    }

    public SortedSet<String> getMisconfiguredObservations() {
        return misconfiguredObservations;
    }

    public SortedSet<String> getAllSemesters() {
        return allSemesters;
    }

    /**
     * Creates a mini model for the given peer and date.
     * This is a convenience method used by QPT.
     * @param peer
     * @param date the date on which the night ends / sunrise
     * @return
     * @throws IOException
     * @throws TimeoutException
     */
    public static MiniModel newInstance(
            KeyChain kc,
            Peer peer,
            long date,
            AgsMagnitude.MagnitudeTable magTable) throws IOException, TimeoutException {

        return newInstance(kc, peer, new Date(date), Collections.<Semester>emptySet(), ProgramType$.MODULE$.AllAsJava(), RELEVANT_OBS_CLASSES, RELEVANT_OBS_STATUSES, magTable);

    }

    /**
     * Creates a mini model for the given peer, date and some additional semesters.
     * This is a convenience method used by QPT.
     * @param peer
     * @param date the date on which the night ends / sunrise
     * @param extraSemesters
     * @return
     * @throws IOException
     * @throws TimeoutException
     */
    public static MiniModel newInstance(
            KeyChain kc,
            Peer peer,
            long date,
            Set<String> extraSemesters,
            AgsMagnitude.MagnitudeTable magTable) throws IOException, TimeoutException {

        Set<Semester> semesters = new HashSet<Semester>();
        for (String s : extraSemesters) {
            try {
                semesters.add(Semester.parse(s));
            } catch (ParseException e) {
                throw new RuntimeException("can not parse semester: " + s);
            }
        }
        return newInstance(kc, peer, new Date(date), semesters, ProgramType$.MODULE$.AllAsJava(), RELEVANT_OBS_CLASSES, RELEVANT_OBS_STATUSES, magTable);
    }

    /**
     * Creates a mini model for a given peer, date and additional semesters containing all observations
     * that match the given observation classes and statuses.
     * This is a convenience method for QPT. The mini model returned by this function will only contain
     * ENG and CAL observations for the specified date and rollovers for the past semester.
     * @param peer
     * @param date the date on which the night ends / sunrise
     * @param extraSemesters
     * @param progTypes
     * @param obsClasses
     * @param obsStatuses
     * @return
     * @throws IOException
     * @throws TimeoutException
     */
    public static MiniModel newInstance(
            KeyChain kc,
            Peer peer,
            Date date,
            Set<Semester> extraSemesters,
            List<ProgramType> progTypes,
            Set<ObsClass> obsClasses,
            Set<ObservationStatus> obsStatuses,
            AgsMagnitude.MagnitudeTable magTable) throws IOException, TimeoutException {

        ObsQueryFunctor func = new ObsQueryFunctor(peer.site, date, extraSemesters, progTypes, obsClasses, obsStatuses, magTable);
        return newInstance(kc, peer, func);

    }

    /**
     * Creates a mini model from an already executed functor.
     * @param peer
     * @param result
     * @return
     */
    public static MiniModel newInstanceFromExecuted(Peer peer, ObsQueryFunctor result) {
        return new MiniModel(
                peer.site,
                result.getProgramSet(),
                result.getMisconfiguredObservations(),
                result.getAllSemesters(),
                result.getProgramExclusions(),
                result.getObsExclusions());
    }

    private static MiniModel newInstance(KeyChain kc, Peer peer, ObsQueryFunctor functor) throws IOException, TimeoutException {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {

            final ClassLoader classLoader = MiniModel.class.getClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);

            // Ensure that the provided peer has a Site
            if (peer.site == null) {
                throw new IOException("The selected peer is not associated with a site.\n" +
                        "This generally indicates a configuration problem with your application.");
            }

            final IDBQueryRunner qr = TrpcClient$.MODULE$.apply(peer.host, peer.port).withKeyChain(kc).proxy(IDBQueryRunner.class);
            final ObsQueryFunctor result = qr.queryPrograms(functor);
            return newInstanceFromExecuted(peer, result);

        } catch (UndeclaredThrowableException ute) {
            try {
                throw ute.getCause();
            } catch (IOException | RuntimeException ioe) {
                throw ioe;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }


    public Map<SPObservationID, ObsExclusion> getObsExclusions() {
        return obsExclusions;
    }

    public Map<SPProgramID, ProgramExclusion> getProgramExclusions() {
        return programExclusions;
    }
}
