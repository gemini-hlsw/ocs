package jsky.app.ot.ui.util;

import javax.swing.*;

import edu.gemini.spModel.obs.ObservationStatus;

import java.util.Map;
import java.util.HashMap;

/**
 *Constants for UI usage
 */
public final class UIConstants {
    private UIConstants() {}

    public static final Icon UNKNOWN_ICON = getIcon("qmark.gif");

    private static final class StatusIcons {
        public final Icon obsIcon;
        public final Icon folderIcon;
        public final Icon groupIcon;

        private StatusIcons(Icon obsIcon, Icon folderIcon, Icon groupIcon) {
            this.obsIcon = obsIcon;
            this.folderIcon = folderIcon;
            this.groupIcon = groupIcon;
        }
    }

    private static final Map<ObservationStatus, StatusIcons> ICON_MAP =
            new HashMap<ObservationStatus, StatusIcons>();

    static {
        ICON_MAP.put(ObservationStatus.PHASE2, new StatusIcons(
                getIcon("observation.gif"),
                getIcon("libFolderDefault.gif"),
                getIcon("obsGroup.gif")
        ));

        ICON_MAP.put(ObservationStatus.FOR_REVIEW, new StatusIcons(
                getIcon("observationForReview.gif"),
                getIcon("libFolderForReview.gif"),
                getIcon("obsGroupForReview.gif")
        ));

        ICON_MAP.put(ObservationStatus.IN_REVIEW, new StatusIcons(
                getIcon("observationInReview.gif"),
                getIcon("libFolderInReview.gif"),
                getIcon("obsGroupInReview.gif")
        ));

        ICON_MAP.put(ObservationStatus.FOR_ACTIVATION, new StatusIcons(
                getIcon("observationForActivation.gif"),
                getIcon("libFolderActivation.gif"),
                getIcon("obsGroupForActivation.gif")
        ));

        ICON_MAP.put(ObservationStatus.READY, new StatusIcons(
                getIcon("observationReady.gif"),
                getIcon("libFolderReady.gif"),
                getIcon("obsGroupReady.gif")
        ));

        ICON_MAP.put(ObservationStatus.ON_HOLD, new StatusIcons(
                getIcon("observationOnHold.gif"),
                getIcon("libFolderOnHold.gif"),
                getIcon("obsGroupOnHold.gif")
        ));

        ICON_MAP.put(ObservationStatus.ONGOING, new StatusIcons(
                getIcon("observationOngoing.gif"),
                getIcon("libFolderOnGoing.gif"),
                getIcon("obsGroupOngoing.gif")
        ));

        ICON_MAP.put(ObservationStatus.INACTIVE, new StatusIcons(
                getIcon("observationInactive.gif"),
                getIcon("libFolderInactive.gif"),
                getIcon("obsGroupInactive.gif")
        ));

        ICON_MAP.put(ObservationStatus.OBSERVED, new StatusIcons(
                getIcon("observationDone.gif"),
                getIcon("libFolderDone.gif"),
                getIcon("obsGroupDone.gif")
        ));
    }

    private static Icon getIcon(String name) {
        return new ImageIcon(UIConstants.class.getResource("/resources/images/"+name));
    }

    public static Icon getObsIcon(ObservationStatus status) {
        StatusIcons si = ICON_MAP.get(status);
        if (si == null) return getObsIcon(ObservationStatus.PHASE2);
        return si.obsIcon;
    }

    public static Icon getFolderIcon(ObservationStatus status) {
        StatusIcons si = ICON_MAP.get(status);
        if (si == null) return getFolderIcon(ObservationStatus.PHASE2);
        return si.folderIcon;
    }

    public static Icon getGroupIcon(ObservationStatus status) {
        StatusIcons si = ICON_MAP.get(status);
        if (si == null) return getGroupIcon(ObservationStatus.PHASE2);
        return si.groupIcon;
    }

    //Image files for icons used to represent group states
    public static final Icon GROUP_ICON = getIcon("obsGroup.gif");

    //Image files for icons used to represent folder states
    public static final Icon FOLDER_ICON = getIcon("libFolderDefault.gif");

    //Icons for diferent observation status
    public static final Icon OBS_ICON = getIcon("observation.gif");

    //icons for decorations of tree nodes
    public static final Icon ERROR_DECORATION = getIcon("error_co.gif");
    public static final Icon WARNING_DECORATION = getIcon("warning_co.gif");

    //icons for decorations of tree nodes with ignored errors or warnings
    public static final Icon IGNORED_ERROR_DECORATION = getIcon("error_co_ignored.gif");
    public static final Icon IGNORED_WARNING_DECORATION = getIcon("warning_co_ignored.gif");

    //icons for problem status
    public static final Icon WARNING_ICON = getIcon("warn_tsk.gif");
    public static final Icon ERROR_ICON = getIcon("error_tsk.gif");

    //greyed out icons for ignored problem status
    public static final Icon IGNORED_WARNING_ICON = getIcon("warn_tsk_ignored.gif");
    public static final Icon IGNORED_ERROR_ICON = getIcon("error_tsk_ignored.gif");



}
