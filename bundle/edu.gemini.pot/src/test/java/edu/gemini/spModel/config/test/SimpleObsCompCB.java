// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SimpleObsCompCB.java 4726 2004-05-14 16:50:12Z brighton $
//

package edu.gemini.spModel.config.test;

import edu.gemini.spModel.config.HelperObsCompCB;

import edu.gemini.pot.sp.ISPObsComponent;


public class SimpleObsCompCB extends HelperObsCompCB {

    public SimpleObsCompCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    // All functionality handled by HelperObsCompCB

}
