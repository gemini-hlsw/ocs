package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.target.env.*;

import java.util.Collections;
import java.util.List;

/**
 * An implementation of Row that represents a group header, as in a guide group.
 */
public final class GroupRow extends Row {
    private final List<Row> children;
    private final IndexedGuideGroup indexedGuideGroup;

    GroupRow(final boolean enabled,
             final boolean editable,
             final int index,
             final GuideGroup group,
             final List<Row> children) {
        super(enabled,
                editable,
                "",
                extractGroupName(group),
                null,
                None.instance());
        this.children = Collections.unmodifiableList(children);
        this.indexedGuideGroup = IndexedGuideGroup$.MODULE$.apply(index, group);
    }

    public List<Row> children() {
        return children;
    }

    public IndexedGuideGroup indexedGuideGroup() {
        return indexedGuideGroup;
    }

    @Override
    public boolean movable() {
        return false;
    }

    private static String extractGroupName(final GuideGroup group) {
        final GuideGrp grp = group.grp();
        final String name;
        if (grp.isAutomatic()) {
            if (grp instanceof AutomaticGroup.Disabled$)
                name = "Auto (Disabled)";
            else
                name = "Auto";
        } else
            name = group.getName().getOrElse("Manual");
        return name;
    }
}