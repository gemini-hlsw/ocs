package edu.gemini.ags.gems;

import edu.gemini.ags.api.DefaultMagnitudeTable;
import edu.gemini.catalog.api.MagnitudeLimits;
import edu.gemini.catalog.api.RadiusLimits;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.spModel.gemini.gems.GemsInstrument;
import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import edu.gemini.spModel.gems.GemsGuideStarType;
import edu.gemini.spModel.gems.GemsTipTiltMode;
import edu.gemini.spModel.obs.context.ObsContext;

import java.util.*;

/**
 * An immutable class specifying the Gems guide star search options.
 * An instance of this class will be created by the UI or other client
 * and used to control the search process.
 *
 * See OT-25
 */
public class GemsGuideStarSearchOptions {

    public static enum CatalogChoice {
        PPMXL_CDS("PPMXL@CDS", "PPMXL at CDS"),
        PPMXL_CADC("PPMXL@CADC", "PPMXL at CADC"),
        UCAC3_CDS("UCAC3@CDS", "UCAC3 at CDS"),
        UCAC3_CADC("UCAC3@CADC", "UCAC3 at CADC"),
        NOMAD1_CDS("NOMAD1@CDS", "NOMAD1 at CDS"),
        NOMAD1_CADC("NOMAD1@CADC", "NOMAD1 at CADC"),
        USER_CATALOG("user", "User Catalog"),
        ;

//        public static CatalogChoice DEFAULT = NOMAD1_CADC;
        public static CatalogChoice DEFAULT = UCAC3_CADC;

        private String _displayValue;
        private String _catalogName;

        private CatalogChoice(String catalogName, String displayValue) {
            _displayValue = displayValue;
            _catalogName = catalogName;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String catalogName() {
            return _catalogName;
        }

        public String toString() {
            return displayValue();
        }
    }


    public static enum NirBandChoice {
        J(Magnitude.Band.J),
        H(Magnitude.Band.H),
        K(Magnitude.Band.K),
        ;

        public static NirBandChoice DEFAULT = H;

        private Magnitude.Band _band;

        private NirBandChoice(Magnitude.Band band) {
            _band = band;
        }

        public Magnitude.Band getBand() {
            return _band;
        }

        public String displayValue() {
            return _band.name();
        }

        public String toString() {
            return displayValue();
        }
    }


    public static enum AnalyseChoice {
        BOTH("Canopus and GSAOI", GemsTipTiltMode.both),
        CANOPUS("Canopus", GemsTipTiltMode.canopus),
        GSAOI("GSAOI", GemsTipTiltMode.instrument),
        ;

//        public static AnalyseChoice DEFAULT = BOTH;
        public static AnalyseChoice DEFAULT = CANOPUS; // REL-604

        private String _displayValue;
        private GemsTipTiltMode _gemsTipTiltMode;

        private AnalyseChoice(String name, GemsTipTiltMode gemsTipTiltMode) {
            _displayValue = name;
            _gemsTipTiltMode = gemsTipTiltMode;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String toString() {
            return displayValue();
        }

        public GemsTipTiltMode getGemsTipTiltMode() {
            return _gemsTipTiltMode;
        }
    }


    public static final String DEFAULT_CATALOG = CatalogChoice.DEFAULT.catalogName();

    private String opticalCatalog = DEFAULT_CATALOG;
    private String nirCatalog = DEFAULT_CATALOG;
    private GemsInstrument instrument;
    private GemsTipTiltMode tipTiltMode;
    private Magnitude.Band nirBand = NirBandChoice.DEFAULT.getBand();
    private Set<Angle> posAngles = new HashSet<Angle>();


    public GemsGuideStarSearchOptions() {
    }

    public GemsGuideStarSearchOptions(String opticalCatalog, String nirCatalog, GemsInstrument instrument,
                                      GemsTipTiltMode tipTiltMode, Set<Angle> posAngles) {
        this.opticalCatalog = opticalCatalog;
        this.nirCatalog = nirCatalog;
        this.instrument = instrument;
        if (instrument == GemsInstrument.flamingos2) {
            // Flamingos 2 OIWFS can only ever be used for the flexure star.
            this.tipTiltMode = GemsTipTiltMode.canopus;
        } else {
            this.tipTiltMode = tipTiltMode;
        }
//        this.nirMagLimits = nirMagLimits;
        this.posAngles = posAngles;
    }

    public String getOpticalCatalog() {
        return opticalCatalog;
    }

    public String getNirCatalog() {
        return nirCatalog;
    }

    public GemsInstrument getInstrument() {
        return instrument;
    }

    public GemsTipTiltMode getTipTiltMode() {
        return tipTiltMode;
    }

    public Set<Angle> getPosAngles() {
        return posAngles;
    }

    public Magnitude.Band getNirBand() {
        return nirBand;
    }

    /**
     * @return a copy of this instance
     */
    public GemsGuideStarSearchOptions copy() {
        return new GemsGuideStarSearchOptions(opticalCatalog, nirCatalog, instrument,
                                      tipTiltMode, posAngles);
    }

    /**
     * @param opticalCatalog
     * @return a copy of this instance with the given opticalCatalog
     */
    public GemsGuideStarSearchOptions setOpticalCatalog(String opticalCatalog) {
        GemsGuideStarSearchOptions o = copy();
        o.opticalCatalog = opticalCatalog;
        return o;
    }

    /**
     *
     * @param nirCatalog
     * @return a copy of this instance with the given nirCatalog
     */
    public GemsGuideStarSearchOptions setNirCatalog(String nirCatalog) {
        GemsGuideStarSearchOptions o = copy();
        o.nirCatalog = nirCatalog;
        return o;
    }

    /**
     *
     * @param instrument
     * @return a copy of this instance with the given instrument
     */
    public GemsGuideStarSearchOptions setInstrument(GemsInstrument instrument) {
        GemsGuideStarSearchOptions o = copy();
        o.instrument = instrument;
        return o;
    }

    /**
     *
     * @param tipTiltMode
     * @return a copy of this instance with the given tipTiltMode
     */
    public GemsGuideStarSearchOptions setTipTiltMode(GemsTipTiltMode tipTiltMode) {
        GemsGuideStarSearchOptions o = copy();
        o.tipTiltMode = tipTiltMode;
        return o;
    }

    /**
     *
     * @param posAngles
     * @return a copy of this instance with the given posAngles
     */
    public GemsGuideStarSearchOptions setPosAngles(Set<Angle> posAngles) {
        GemsGuideStarSearchOptions o = copy();
        o.posAngles = posAngles;
        return o;
    }

    /**
     *
     * @param nirBand
     * @return a copy of this instance with the given NIR band
     */
    public GemsGuideStarSearchOptions setNirBand(Magnitude.Band nirBand) {
        GemsGuideStarSearchOptions o = copy();
        this.nirBand = nirBand;
        return o;
    }

    /**
     * @param nirBand      optional NIR magnitude band (default is H)
     * @return all relevant CatalogSearchCriterion instances
     */
    public List<GemsCatalogSearchCriterion> searchCriteria(ObsContext obsContext, Option<Magnitude.Band> nirBand) {
        switch(tipTiltMode) {
            case canopus: return Arrays.asList(
                    canopusCriterion(obsContext, GemsGuideStarType.tiptilt),
                    instrumentCriterion(obsContext, GemsGuideStarType.flexure, nirBand));
            case instrument: return Arrays.asList(
                    instrumentCriterion(obsContext, GemsGuideStarType.tiptilt, nirBand),
                    canopusCriterion(obsContext, GemsGuideStarType.flexure));
            default:
            case both: return Arrays.asList(
                    canopusCriterion(obsContext, GemsGuideStarType.tiptilt),
                    instrumentCriterion(obsContext, GemsGuideStarType.flexure, nirBand),
                    instrumentCriterion(obsContext, GemsGuideStarType.tiptilt, nirBand),
                    canopusCriterion(obsContext, GemsGuideStarType.flexure)
            );
        }
    }

    public GemsCatalogSearchCriterion canopusCriterion(ObsContext obsContext, GemsGuideStarType ggst) {
        DefaultMagnitudeTable.GemsMagnitudeLimitsCalculator calculator = new DefaultMagnitudeTable(obsContext).CanopusWfsMagnitudeLimitsCalculator();
        return searchCriterion(obsContext, Canopus.Wfs.Group.instance, calculator, ggst, None.<Magnitude.Band>instance());
    }

    public GemsCatalogSearchCriterion instrumentCriterion(ObsContext obsContext, GemsGuideStarType ggst, Option<Magnitude.Band> nirBand) {
        DefaultMagnitudeTable.GemsMagnitudeLimitsCalculator calculator = new DefaultMagnitudeTable(obsContext).GemsInstrumentToMagnitudeLimitsCalculator().apply(instrument);
        return searchCriterion(obsContext, instrument.getGuiders(), calculator, ggst, nirBand);
    }

    public GemsCatalogSearchCriterion searchCriterion(ObsContext obsContext,
                                                      GemsGuideProbeGroup gGroup,
                                                      DefaultMagnitudeTable.GemsMagnitudeLimitsCalculator calculator,
                                                      GemsGuideStarType gType,
                                                      Option<Magnitude.Band> nirBand) {
        String name = "%s %s".format(gGroup.getDisplayName(), gType.name());

        // Adjust the mag limits for the worst conditions (as is done in the ags servlet)
        MagnitudeLimits magLimits = calculator.getGemsMagnitudeLimitsForJava(gType, nirBand).mapMagnitudes(obsContext.getConditions().magAdjustOp());

        //MagnitudeLimits magLimits = gGroup.getMagLimits(gType, nirBand).mapMagnitudes(obsContext.getConditions().magAdjustOp());
        RadiusLimits radiusLimits = new RadiusLimits(gGroup.getRadiusLimits());
        Option<Offset> searchOffset = instrument.getOffset();
        Option<Angle> searchPA = (posAngles.size() == 1) ? new Some<Angle>(posAngles.iterator().next()) : None.<Angle>instance();
        CatalogSearchCriterion criterion = new CatalogSearchCriterion(name, magLimits, radiusLimits, searchOffset, searchPA);
        GemsCatalogSearchKey key = new GemsCatalogSearchKey(gType, gGroup);
        return new GemsCatalogSearchCriterion(key, criterion);
    }

    public Set<String> getCatalogs() {
        Set<String> catalogs = new HashSet<String>(2);
        catalogs.add(nirCatalog);
        catalogs.add(opticalCatalog);
        return catalogs;
    }
}
