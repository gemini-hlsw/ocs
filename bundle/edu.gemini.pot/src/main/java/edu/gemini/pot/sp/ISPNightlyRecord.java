// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ISPNightlyRecord.java 47000 2012-07-26 19:15:10Z swalker $
//

package edu.gemini.pot.sp;


/**
 * This is the interface for a Nightly Record.
 */
public interface ISPNightlyRecord extends ISPRootNode {

    /**
     * Names the property change event fired when the program id is modified.
     */
    String NIGHTLY_PLAN_ID_PROP = "NightlyPlanID";

}

