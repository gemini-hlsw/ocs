package edu.gemini.qpt.ui.util;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public interface SharedIcons {

	@SuppressWarnings("serial")
	class SharedIcon extends ImageIcon {
		SharedIcon(String name) {
			super(SharedIcons.class.getResource(name));
		}
	}

	@SuppressWarnings("serial")
	class SharedOffsetIcon extends OffsetImageIcon {
		SharedOffsetIcon(String name, int x, int y) {
			super(SharedIcons.class.getResource(name), x, y);
		}
	}
	
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
	Icon ICON_WARN = new SharedIcon("warn_tsk_red.gif");
	Icon ICON_NOTICE = new SharedIcon("warn_tsk_blue.gif");
	Icon ICON_INFO = new SharedIcon("info_tsk.gif");

	Icon PROGRAM_CLOSED = new SharedIcon("cprj_obj.gif");
	Icon PROGRAM_OPEN = new SharedIcon("prj_obj.gif");
	
	Icon GROUP_OPEN = new SharedIcon("fldr_obj.gif");
	Icon GROUP_CLOSED = new SharedIcon("fldr_obj.gif");

	Icon NOTE = new SharedIcon("file_obj.gif");
	Icon NOTE_DIS = new SharedIcon("file_obj_dis.gif");
	
	Icon OVL_ERROR = new SharedIcon("error_co.gif");
	Icon OVL_WARN = new SharedIcon("warning_co.gif");
	Icon OVL_IN_PROGRESS = new SharedIcon("run_co.gif");
	Icon OVL_CROSSED_OUT = new SharedOffsetIcon("deprecated.gif", 0, 3);
	Icon OVL_SCHEDULED = new SharedOffsetIcon("synch_co.gif", -8, -6);
    Icon OVL_TIMING = new SharedOffsetIcon("t.gif", 0, -9);
    Icon OVL_LGS = new SharedOffsetIcon("l.gif", -8, 0);

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
