// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ISPObservingLog.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.pot.sp;




/**
 * The shell for the automatically updating part of the observing log.
 */
public interface ISPObsExecLog extends ISPProgramNode {
    SPComponentType getType();
}

