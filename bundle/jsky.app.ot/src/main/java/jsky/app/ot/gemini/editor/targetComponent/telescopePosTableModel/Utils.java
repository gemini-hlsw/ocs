package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.gemini.ghost.GhostAsterism;
import edu.gemini.spModel.target.SPCoordinates;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.Asterism;

import java.time.Instant;

final class Utils {
    private Utils() {}

    // Return the world coordinates for the given target.
    static Option<Coordinates> getCoordinates(final SPTarget t,
                                                final Option<Long> when) {
        return t.getRaDegrees(when).flatMap(ra ->
                t.getDecDegrees(when).flatMap(dec ->
                        ImOption.fromScalaOpt(Coordinates.fromDegrees(ra, dec))));
    }

    // Extracting coordinates from GHOST asterisms is obnoxious because the types are
    // entirely different, so this method is getCoordinates for entire GHOST asterisms.
    static Option<Coordinates> getCoordinates(final GhostAsterism a,
                                              final Option<Long> when) {
        // Convert the Option<Long> to an Option<Instant>.
        final Option<Instant> instWhen = when.map(Instant::ofEpochMilli);
        return ImOption.fromScalaOpt(a.basePosition(ImOption.toScalaOpt(instWhen)));
    }

    // Get the base coordinates, regardless of asterism type.
    static Option<Coordinates> getBaseCoordinates(final Asterism a,
                                                  final Option<Long> when) {
        switch (a.asterismType()) {
            case Single:
                return getCoordinates(((Asterism.Single) a).t(), when);
            case GhostSingleTarget:
            case GhostDualTarget:
            case GhostTargetPlusSky:
            case GhostSkyPlusTarget:
            case GhostHighResolutionTargetPlusSky:
                return getCoordinates((GhostAsterism) a, when);
            default:
                return None.instance();
        }
    }
}
