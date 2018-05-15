package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.core.Magnitude;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.target.SPTarget;

import java.text.DecimalFormat;

/**
 * A row representing a science, user, or guide target.
 */
public abstract class TargetRow extends Row {
    private static final DecimalFormat MAG_FORMAT = new DecimalFormat("0.0##");

    private final SPTarget target;
    private final Option<Double> distance;

    public SPTarget target() {
        return target;
    }

    public synchronized String formatMagnitude(final MagnitudeBand band) {
        final Option<Magnitude> mag = target.getMagnitudeJava(band);
        return mag.map(m -> MAG_FORMAT.format(m.value())).getOrElse("");
    }

    @Override
    public Option<Double> distance() {
        return distance;
    }

    TargetRow(final boolean enabled,
              final boolean editable,
              final String tag,
              final SPTarget target,
              final Option<Coordinates> baseCoords,
              final Option<Long> when) {
        super(RowType.TARGET,
                enabled,
                editable,
                tag,
                target.getName(),
                null,
                when);
        this.target = target;

        final Option<Coordinates> coords = Utils.getCoordinates(target, when);
        this.distance = baseCoords.flatMap(bc ->
                coords.map(c ->
                        bc.angularDistance(c).toArcmins()
                )
        );
    }
}

