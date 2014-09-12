// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ISPObsComponent.java 46866 2012-07-20 19:35:51Z swalker $
//

package edu.gemini.pot.sp;


/**
 * This is the interface for a Science Program Observation Component node.
 */
public interface ISPObsComponent extends ISPProgramNode {
    /**
     * Returns the type of this observation component.
     */
    SPComponentType getType();
}

