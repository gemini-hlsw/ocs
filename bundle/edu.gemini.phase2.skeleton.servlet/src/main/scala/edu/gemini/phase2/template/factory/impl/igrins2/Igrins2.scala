package edu.gemini.phase2.template.factory.impl
package igrins2

import edu.gemini.pot.sp.{ISPGroup, SPComponentType}
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.igrins2.NoddingOption
import edu.gemini.spModel.gemini.igrins2.{Igrins2 => InstIgrins2}
import edu.gemini.spModel.gemini.igrins2.SeqConfigIgrins2
import edu.gemini.spModel.gemini.igrins2.blueprint.SpIgrins2Blueprint

case class Igrins2(blueprint: SpIgrins2Blueprint) extends GroupInitializer[SpIgrins2Blueprint] with TemplateDsl2[InstIgrins2] {
  override val program = "IGRINS-2 INSTRUMENT PHASE I/II MAPPING BPS"

  override def instCompType: SPComponentType =
    InstIgrins2.SP_TYPE

  override def seqConfigCompType: SPComponentType =
    SeqConfigIgrins2.SP_TYPE

  var db: Option[TemplateDb] = None
  override def initialize(db: TemplateDb, pid: SPProgramID): Maybe[ISPGroup] =
    try {
      this.db = Some(db)
      super.initialize(db, pid)
    } finally {
      this.db = None
    }

  // Blueprint library:
  // {1} Before telluric observation
  // {2} Point source (ABBA) science observation
  // {3} Extended source (On-off) science observation
  // {4} After telluric observation
  // {5} SVC image
  //
  // The instantiated scheduling group should look like
  // Observer Instructions note
  // Before Telluric
  // SVC Image
  // Science
  // After Telluric
  //
  // INCLUDE note "Phase II Checklist" at the program level.
  //
  // INCLUDE {1}, {5} IN target-specific Scheduling Group
  //
  // If NODDING OPTION == Keep target in slit:
  //   INCLUDE {2}
  //
  // If NODDING OPTION == Nod to sky:
  //   INCLUDE {3}
  //
  // INCLUDE {4} IN target-specific Scheduling Group
  //
  // INCLUDE the note "Observer Instructions" IN the target-specific Scheduling Group

  val n = blueprint.getNoddingOption match {
    case NoddingOption.KEEP_TARGET_IN_SLIT => 2
    case NoddingOption.NOD_TO_SKY          => 3
  }
  include(1, 5, n, 4) in TargetGroup

  addNote("Observer Instructions") in TargetGroup
}
