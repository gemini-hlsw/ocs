package edu.gemini.spModel.gemini.seqcomp

import edu.gemini.pot.sp.ISPNode
import edu.gemini.spModel.core.Semester
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.rich.core._
import edu.gemini.spModel.rich.pot.sp._

// REL-4411: The default ObsClass for arcs changed in 24B.  Before it would
// default to PARTNER_CAL but now should default to PROG_CAL.

object DefaultArcObsClass {

  private val Semester24B: Semester =
    Semester.parseOptional("2024B").get

  private def uses24BDefault(c: ISPNode): Boolean =
    Option(c)
      .flatMap(_.semesterOption)
      .forall(_ >= Semester24B)

  def forNode(c: ISPNode): ObsClass =
    if (uses24BDefault(c)) ObsClass.PARTNER_CAL else ObsClass.PROG_CAL;

}