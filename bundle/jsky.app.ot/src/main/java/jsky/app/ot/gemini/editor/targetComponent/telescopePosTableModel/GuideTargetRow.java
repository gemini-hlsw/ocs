package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.ags.api.AgsGuideQuality;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import jsky.app.ot.gemini.editor.targetComponent.GuidingIcon;
import jsky.util.gui.Resources;

import javax.swing.*;
import javax.swing.border.Border;

final class GuideTargetRow extends TargetRow {
    private final boolean isActiveGuideProbe;
    private final Option<AgsGuideQuality> quality;
    private final boolean movable;

    private static final Icon errorIcon = Resources.getIcon("eclipse/error.gif");

    GuideTargetRow(final boolean isActiveGuideProbe,
                   final Option<AgsGuideQuality> quality,
                   final boolean enabled,
                   final boolean editable,
                   final boolean movable,
                   final GuideProbe probe,
                   final int index,
                   final SPTarget target,
                   final Option<Coordinates> baseCoords,
                   final Option<Long> when) {
        super(enabled,
                editable,
                String.format("%s (%d)", probe.getKey(), index),
                target,
                baseCoords,
                when);
        this.isActiveGuideProbe = isActiveGuideProbe;
        this.quality = quality;
        this.movable = movable;
    }

    public Option<AgsGuideQuality> quality() {
        return quality;
    }

    @Override
    public boolean movable() {
        return movable;
    }

    @Override
    public Border border(int col)     {
        return col == 0 ? BorderFactory.createEmptyBorder(0, 16, 0, 0) : null;
    }

    @Override
    public Icon icon() {
        return isActiveGuideProbe ?
                GuidingIcon.apply(quality.getOrElse(AgsGuideQuality.Unusable$.MODULE$), enabled()) :
                errorIcon;
    }
}
