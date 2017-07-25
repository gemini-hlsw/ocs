package edu.gemini.pit.ui.util;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Component;
import java.awt.Graphics;
import java.net.URL;

public interface SharedIcons {

    @SuppressWarnings("serial")
    class SharedIcon extends ImageIcon {
        private final String name;

        SharedIcon(String name) {
            super(SharedIcons.class.getResource("icons/" + name));
            this.name = name;
        }
    }

    @SuppressWarnings("serial")
    class SharedOffsetIcon extends OffsetImageIcon {
        SharedOffsetIcon(String name, int x, int y) {
            super(SharedIcons.class.getResource("icons/" + name), x, y);
        }
    }

    Icon FLOWCHART  = new SharedIcon("flowchart.png");
    ImageIcon PIT  = new ImageIcon(SharedIcons.class.getResource("icons/pit_icon.png"));

    Icon BULLET_GREEN  = new SharedIcon("bullet_green.png");
    Icon BULLET_RED    = new SharedIcon("bullet_red.png");
    Icon BULLET_YELLOW = new SharedIcon("bullet_yellow.png");
    Icon BULLET_ORANGE = new SharedIcon("bullet_orange.png");
    Icon BULLET_GREY   = new SharedIcon("bullet_grey.png");
    Icon ICON_BLANK    = new Icon() {
        public void paintIcon(Component c, Graphics g, int x, int y) { }
        public int getIconWidth() { return 16; }
        public int getIconHeight() { return 16; }
    };

    Icon FLAG_AR = new SharedIcon("ar.png");
    Icon FLAG_AU = new SharedIcon("au.png");
    Icon FLAG_BR = new SharedIcon("br.png");
    Icon FLAG_CA = new SharedIcon("ca.png");
    Icon FLAG_CFH = new SharedIcon("cfht.png");
    Icon FLAG_CL = new SharedIcon("cl.png");
    Icon FLAG_KR = new SharedIcon("kr.png");
    Icon FLAG_GEMINI = new SharedIcon("gemini.png");
    Icon FLAG_JP = new SharedIcon("jp.png");
    Icon FLAG_KECK = new SharedIcon("keck.png");
    Icon FLAG_UH = new SharedIcon("uh.png");
    Icon FLAG_UK = new SharedIcon("uk.png");
    Icon FLAG_US = new SharedIcon("us.png");

    Icon ICON_SPINNER = new SharedIcon("spinner.gif");
    Icon ICON_SPINNER_BLUE = new SharedIcon("spinner_blue_gray.gif");

    Icon ICON_SIDEREAL = new SharedIcon("sidereal.png");
    Icon ICON_SIDEREAL_DIS = new SharedIcon("sidereal_dis.png");
    Icon ICON_NONSIDEREAL = new SharedIcon("nonsidereal.png");
    Icon ICON_TOO = new SharedIcon("too.png");

    Icon ICON_DEVICE = new SharedIcon("device.png");
    Icon ICON_DEVICE_DIS = new SharedIcon("device_dis.png");
    Icon ICON_CONDS = new SharedIcon("conds.png");
    Icon ICON_CONDS_DIS = new SharedIcon("conds_dis.png");

    Icon ICON_GSA = new SharedIcon("gsa.png");
    Icon ICON_GSA_DIS = new SharedIcon("gsa_dis.png");

    Icon ICON_SELECT = new SharedIcon("select.png");
    Icon ICON_ATTACH = new SharedIcon("attach.png");
    Icon ICON_ATTACH_DIS = new SharedIcon("attach_dis.png");

    Icon ICON_IMPORT = new SharedIcon("import.gif");
    Icon ICON_EXPORT = new SharedIcon("export.gif");
    Icon ICON_IMPORT_DIS = new SharedIcon("import_dis.gif");
    Icon ICON_EXPORT_DIS = new SharedIcon("export_dis.gif");

    Icon ICON_USER = new SharedIcon("user-icon.png");
    Icon ICON_USER_DIS = new SharedIcon("user-icon-dis.png");

    Icon ICON_PUBLISH = new SharedIcon("publish.gif");

    Icon ICON_BOGUS = new SharedOffsetIcon("bogus.gif", 0, 2);

    Icon ICON_CALIB = new SharedOffsetIcon("class_obj.gif", 0, 2);
    Icon ICON_SCIENCE = new SharedOffsetIcon("int_obj.gif", 0, 2);
    Icon ICON_DAYCAL = new SharedOffsetIcon("daycal.gif", 0, 2);
    Icon ICON_DAYENG = new SharedOffsetIcon("dayeng.gif", 0, 2);

    Icon ICON_CALIB_DIS = new SharedOffsetIcon("class_obj_dis.gif", 0, 2);
    Icon ICON_SCIENCE_DIS = new SharedOffsetIcon("int_obj_dis.gif", 0, 2);
    Icon ICON_DAYCAL_DIS = new SharedOffsetIcon("daycal_dis.gif", 0, 2);

    Icon ICON_CALIB_DIM = new SharedOffsetIcon("class_obj_dim.gif", 0, 2);
    Icon ICON_SCIENCE_DIM = new SharedOffsetIcon("int_obj_dim.gif", 0, 2);
    Icon ICON_DAYCAL_DIM = new SharedOffsetIcon("daycal_dim.gif", 0, 2);
    Icon ICON_DAYENG_DIM = new SharedOffsetIcon("dayeng_dim.gif", 0, 2);

    Icon ICON_ACQ = new SharedOffsetIcon("annotation_obj.gif", 0, 1);
    Icon ICON_VARIANT = new SharedIcon("package_obj.gif");
    Icon ICON_ERROR = new SharedIcon("error_tsk.gif");
    Icon ICON_INFO = new SharedIcon("info_tsk.gif");
    Icon ICON_WARN = new SharedIcon("warn_tsk.gif");
    Icon ICON_TEMPLATES = new SharedIcon("download_templates.gif");
    Icon ICON_HELP = new SharedIcon("help.gif");

    Icon PROGRAM_CLOSED = new SharedIcon("cprj_obj.gif");
    Icon PROGRAM_OPEN = new SharedIcon("prj_obj.gif");

    Icon GROUP_OPEN = new SharedIcon("fldr_obj.gif");
    Icon GROUP_CLOSED = new SharedIcon("fldr_obj.gif");

    Icon ICON_CLOCK = new SharedIcon("clock.png");
    Icon ICON_CLOCK_DIS = new SharedIcon("clock_dis.png");

    Icon NOTE = new SharedIcon("file_obj.gif");
    Icon NOTE_DIS = new SharedIcon("file_obj_dis.gif");

    Icon OVL_ERROR = new SharedIcon("error_co.gif");
    Icon OVL_WARN = new SharedIcon("warning_co.gif");
    Icon OVL_IN_PROGRESS = new SharedIcon("run_co.gif");
    Icon OVL_CROSSED_OUT = new SharedOffsetIcon("deprecated.gif", 0, 3);
    Icon OVL_SCHEDULED = new SharedOffsetIcon("synch_co.gif", -8, -6);
    Icon OVL_TIMING = new SharedOffsetIcon("t.gif", 0, -9);

    Icon CHECK_SELECTED = new SharedIcon("check_selected.gif");
    Icon CHECK_UNSELECTED = new SharedIcon("check_unselected.gif");
    Icon CHECK_INDETERMINATE = new SharedIcon("check_indefinite.gif");

    Icon ARROW_NEXT = new SharedIcon("forward_nav.gif");
    Icon ARROW_PREV = new SharedIcon("backward_nav.gif");

    Icon ARROW_UP = new SharedIcon("up.gif");
    Icon ARROW_UP_DISABLED = new SharedIcon("up_dis.gif");

    Icon ARROW_DOWN = new SharedIcon("down.gif");
    Icon ARROW_DOWN_DISABLED = new SharedIcon("down_dis.gif");

    Icon ADD = new SharedIcon("add.gif");
    Icon REMOVE = new SharedIcon("delete_obj.gif");

    Icon ADD_DISABLED = new SharedIcon("add_exc.gif");
    Icon REMOVE_DISABLED = new SharedIcon("remove.gif");

    Icon ADD_SEMESTER = new SharedIcon("semester_add.gif");
    Icon REMOVE_SEMESTER = new SharedIcon("semester_remove.gif");

    Icon DUP_VARIANT = new SharedIcon("dup_variant.gif");
    Icon DUP_VARIANT_DIS = new SharedIcon("dup_variant_dis.gif");

}
