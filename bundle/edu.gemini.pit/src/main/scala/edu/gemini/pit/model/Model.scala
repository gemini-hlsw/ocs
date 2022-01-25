package edu.gemini.pit.model

import java.io.File
import edu.gemini.model.p1.immutable._

import scalaz._
import Scalaz._

// Each shell has a model, and this is the level where we track undo/redo. So anything that needs to participate in
// edits in that way should be part of the model. Other things (like file association) are not part of the model. So
// for now it just wraps a proposal.
case class Model(proposal: Proposal, conversion: ModelConversion) {
  def save(f: File) = ProposalIo.write(proposal, f)

  def resetObservationMeta:Model = this.copy(proposal = proposal.resetObservationMeta)
}

case class ModelConversion(transformed: Boolean, from: Semester, changes: Seq[String])

object Model {

  // Lenses
  val proposal: Lens[Model, Proposal]           = Lens.lensu((a, b) => a.copy(proposal = b), _.proposal)
  val conversion: Lens[Model, ModelConversion]  = Lens.lensu((a, b) => a.copy(conversion = b), _.conversion)
  val transformed: Lens[Model, Boolean]              = conversion >=> Lens.lensu((a, b) => a.copy(transformed = b), _.transformed)
  val fromSemester: Lens[Model, Semester]       = conversion >=> Lens.lensu((a, b) => a.copy(from = b), _.from)
  val schemaVersion: Lens[Model, String]        = proposal >=> Lens.lensu((a, b) => a.copy(schemaVersion = b), _.schemaVersion)

  // The empty model
  val empty = Model(Proposal.empty, ModelConversion(transformed = false, Semester.current, Nil))

  // Try to read from XML
  def fromFile(f: File):Validation[Either[Exception, NonEmptyList[String]], Model] =
    ProposalIo.readAndConvert(f).map(p => Model(p.proposal, ModelConversion(p.transformed, p.from, p.changes)))

}

