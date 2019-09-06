package edu.gemini.ags.gems;

import edu.gemini.catalog.api.CatalogName;
import edu.gemini.catalog.api.CatalogName.Gaia$;
import edu.gemini.catalog.api.CatalogName.PPMXL$;
import edu.gemini.catalog.api.CatalogName.UCAC4$;
import edu.gemini.catalog.api.MagnitudeConstraints;
import edu.gemini.spModel.core.Angle;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.gemini.gems.CanopusWfs;
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
public final class GemsGuideStarSearchOptions {

    public enum CatalogChoice {
        GAIA_ESA(    Gaia$.MODULE$,  "Gaia at ESA"),
        PPMXL_GEMINI(PPMXL$.MODULE$, "PPMXL at Gemini"),
        UCAC4_GEMINI(UCAC4$.MODULE$, "UCAC4 at Gemini"),
        ;

        public static final CatalogChoice DEFAULT = GAIA_ESA;

        private final CatalogName _catalogName;
        private final String      _displayValue;

        CatalogChoice(CatalogName catalogName, String displayValue) {
            _catalogName  = catalogName;
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public CatalogName catalog() {
            return _catalogName;
        }

        public String toString() {
            return displayValue();
        }
    }


    public enum NirBandChoice {
        J(MagnitudeBand.J$.MODULE$),
        H(MagnitudeBand.H$.MODULE$),
        K(MagnitudeBand.K$.MODULE$),
        ;

        public static final NirBandChoice DEFAULT = H;

        private final MagnitudeBand _band;

        NirBandChoice(MagnitudeBand band) {
            _band = band;
        }

        public MagnitudeBand getBand() {
            return _band;
        }

        public String displayValue() {
            return _band.name();
        }

        public String toString() {
            return displayValue();
        }
    }

    private final GemsInstrument instrument;
    private final Set<Angle>     posAngles;

    public static final CatalogChoice DEFAULT = CatalogChoice.DEFAULT;

    public GemsGuideStarSearchOptions(
        final GemsInstrument instrument,
        final Set<Angle>     posAngles
    ) {
        this.instrument = instrument;
        this.posAngles  = posAngles;
    }

    public GemsInstrument getInstrument() {
        return instrument;
    }

    /**
     * @param nirBand      optional NIR magnitude band (default is H)
     * @return all relevant CatalogSearchCriterion instances
     */
    public List<GemsCatalogSearchCriterion> searchCriteria(
        final ObsContext                  obsContext,
        final scala.Option<MagnitudeBand> nirBand
    ) {
        return Arrays.asList(
                    canopusCriterion(obsContext),
                    instrumentCriterion(obsContext, nirBand));
    }

    private GemsCatalogSearchCriterion canopusCriterion(
        final ObsContext obsContext
    ) {
        final GemsMagnitudeTable.LimitsCalculator calculator =
            GemsMagnitudeTable.CanopusWfsMagnitudeLimitsCalculator();

        return searchCriterion(
            obsContext,
            CanopusWfs.Group.instance,
            calculator,
            GemsGuideStarType.tiptilt,
            scala.Option.empty()
        );
    }

    private GemsCatalogSearchCriterion instrumentCriterion(
        final ObsContext                  obsContext,
        final scala.Option<MagnitudeBand> nirBand
    ) {
        final GemsMagnitudeTable.LimitsCalculator calculator =
            GemsMagnitudeTable.GemsInstrumentToMagnitudeLimitsCalculator().apply(instrument);

        return searchCriterion(
            obsContext,
            instrument.getGuiders(),
            calculator,
            GemsGuideStarType.flexure,
            nirBand
        );
    }

    private GemsCatalogSearchCriterion searchCriterion(
        final ObsContext                          obsContext,
        final GemsGuideProbeGroup                 gGroup,
        final GemsMagnitudeTable.LimitsCalculator calculator,
        final GemsGuideStarType                   gType,
        final scala.Option<MagnitudeBand>         nirBand
    ) {
        final String name = String.format("%s %s", gGroup.getDisplayName(), gType.name());

        // Adjust the mag limits for the worst conditions (as is done in the ags servlet)
        final MagnitudeConstraints magConstraints =
                calculator.adjustGemsMagnitudeConstraintForJava(gType, nirBand, obsContext.getConditions());

        final CatalogSearchCriterion criterion =
            calculator.searchCriterionBuilder(
                name,
                gGroup.getRadiusLimits(),
                instrument,
                magConstraints,
                posAngles
            );

        return new GemsCatalogSearchCriterion(
            new GemsCatalogSearchKey(gType, gGroup),
            criterion
        );
    }

}
