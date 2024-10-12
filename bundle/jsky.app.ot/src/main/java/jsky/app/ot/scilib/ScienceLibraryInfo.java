package jsky.app.ot.scilib;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.ghost.Ghost;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.igrins2.Igrins2;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.phoenix.InstPhoenix;
import edu.gemini.spModel.gemini.texes.InstTexes;
import edu.gemini.spModel.gemini.trecs.InstTReCS;

import java.util.*;

/**
 * Holder of the library information associated to each instrument.
 * Library information includes names of the library programs, database where
 * they are found, username/role needed to fetch them
 */
final class ScienceLibraryInfo {

    private static final Map<SPComponentType, ScienceLibraryInfo> _libInfo = new HashMap<>();
    private static final Set<String> _allLibraries = Collections.synchronizedSet(new HashSet<String>());

    private final Site _site;
    private final List<SPProgramID> _libraries = new ArrayList<>();

    //the data
    static {

        //NORTH
        _libInfo.put(InstGmosNorth.SP_TYPE, new ScienceLibraryInfo(Site.GN, "GN-GMOS-library"));
        _libInfo.put(InstGNIRS.SP_TYPE, new ScienceLibraryInfo(Site.GN, "GN-GNIRS-library"));
        _libInfo.put(InstMichelle.SP_TYPE, new ScienceLibraryInfo(Site.GN, "GN-CAL-MICHELLE-Standards",
                "GN-CAL-MICHELLE-COHENall", "GN-CAL-MICHELLE-Asteroids", "GN-MICHELLE-library"));
        _libInfo.put(InstNIFS.SP_TYPE, new ScienceLibraryInfo(Site.GN, "GN-NIFS-library"));
        _libInfo.put(InstNIRI.SP_TYPE, new ScienceLibraryInfo(Site.GN, "GN-NIRI-library"));
        _libInfo.put(InstTexes.SP_TYPE, new ScienceLibraryInfo(Site.GN, "GN-TEXES-library"));
        _libInfo.put(Igrins2.SP_TYPE, new ScienceLibraryInfo(Site.GN, "GN-IGRINS2-library"));

        //SOUTH
        _libInfo.put(Flamingos2.SP_TYPE, new ScienceLibraryInfo(Site.GS, "GS-Flamingos2-library"));
        _libInfo.put(InstGmosSouth.SP_TYPE, new ScienceLibraryInfo(Site.GS, "GS-GMOS-library"));
        _libInfo.put(Ghost.SP_TYPE, new ScienceLibraryInfo(Site.GS, "GS-GHOST-library"));
        _libInfo.put(Gpi.SP_TYPE, new ScienceLibraryInfo(Site.GS, "GS-GPI-library"));
        _libInfo.put(Gsaoi.SP_TYPE, new ScienceLibraryInfo(Site.GS, "GS-GSAOI-library"));
        _libInfo.put(InstNICI.SP_TYPE, new ScienceLibraryInfo(Site.GS, "GS-NICI-library"));
        _libInfo.put(InstPhoenix.SP_TYPE, new ScienceLibraryInfo(Site.GS, "GS-PHOENIX-library"));
        _libInfo.put(InstTReCS.SP_TYPE, new ScienceLibraryInfo(Site.GS, "GS-TReCS-library"));

    }

    private ScienceLibraryInfo(final Site site, final String... libraries) {
        _site = site;
        for (final String lib : libraries) {
            try {
                _allLibraries.add(lib);
                _libraries.add(SPProgramID.toProgramID(lib));
            } catch (SPBadIDException bie) {
                // If this happens it's a programming error.
                throw new RuntimeException(bie);
            }
        }
    }

    /**
     * Return an unmodifiable map associating instruments to ScienceLibraryInfo objects
     */
    static Map<SPComponentType, ScienceLibraryInfo> getLibraryInfoMap() {
        return Collections.unmodifiableMap(_libInfo);
    }

    /**
     * Return an unmodifiable list of the libraries associated to this instrument
     */
    Collection<SPProgramID> getLibraries() {
        return Collections.unmodifiableList(_libraries);
    }

    /**
     * Return the site where these libraries are found.
     */
    public Site getSite() {
        return _site;
    }
}
