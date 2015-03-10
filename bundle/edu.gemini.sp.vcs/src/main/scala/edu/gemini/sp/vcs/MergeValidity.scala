package edu.gemini.sp.vcs

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version._
import edu.gemini.pot.sp.validator._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.sp.vcs.OldMergePlan.Zipper

//
// Post-process a merge plan (i.e, MergePlan) to handle validity checking and
// any required fixes to make the merged program valid.
//

case class MergeValidity(prog: ISPProgram, odb: IDBDatabaseService) {
  val lifespanId = prog.getLifespanId

  /**
   * Process the MergePlan to transform it into a MergePlan that will generate
   * a valid science program (if not already valid).
   *
   * <p>If a constraint violation is detected from which we cannot recover, a
   * Left with a message describing why is returned.  This indicates a
   * programming error somewhere and is not expected ....</p>
   *
   * @return a potentially modified merge plan that is guaranteed to produce
   *         a valid science program (if Right), or else a message describing
   *         why that cannot be done (if Left)
   */
  def process(mp: OldMergePlan): Either[String, OldMergePlan] =
    Validator.validate(mp.toTypeTree).fold(
      v => fix(mp, v).right.flatMap(process),
      _ => Right(mp)
    )

  private def violationKey(v: Violation): Either[String, SPNodeKey] = v match {
    case DuplicateKeyViolation(k)      => Left("Duplicate node keys detected: " + k)
    case CardinalityViolation(_, k, _) => k.toRight("Cardinality violation with unknown key.")
  }

  private def fix(mp: OldMergePlan, v: Violation): Either[String, OldMergePlan] =
    for {
      k <- violationKey(v).right
      z <- Zipper(mp).find(_.sp.getNodeKey == k).toRight("Validity constraint violation for node not in merged result: " + k).right
      updatedPlan <- fix(z).right
    } yield updatedPlan

  private def addConflictFolder(z: Zipper): Zipper = {
    val cf = odb.getFactory.createConflictFolder(prog, null)
    val vv = EmptyNodeVersions.incr(lifespanId)
    val dj = cf.getDataObject
    z.prependChild(OldMergePlan(cf, vv, dj, cf.getConflicts, Nil)).down.get
  }

//  private def fixWithMerge(zip: Zipper): Either[String, MergePlan] = {
//    type Mergeable = ISPMergeable[ISPDataObject]
//
//    def doMerge(d0: ISPDataObject, d1: ISPDataObject): Option[ISPDataObject] =
//      Option(d0.asInstanceOf[Mergeable].mergeOrNull(d1))
//
//    for {
//      n0  <- Some(zip.focus.sp).filter(_.getDataObject.isInstanceOf[Mergeable]).toRight("Node not mergeable").right
//      z   <- zip.delete.flatMap(_.findChild(c => NodeType(n0).matches(c.sp))).toRight("Could not find matching node type for merge").right
//      obj <- doMerge(n0.getDataObject, z.focus.sp.getDataObject).toRight("Could not merge data objects").right
//    } yield z.setDataObj(obj).top.focus
//  }

  private def fix(zip: Zipper): Either[String, OldMergePlan] = {
    // Add a conflict note to the problem node and get the resulting merge plan
    val problemNode = zip.addNote(new Conflict.ConstraintViolation(zip.focus.sp.getNodeKey)).focus

    val isConflictFolder: OldMergePlan => Boolean = _.sp match {
      case cf: ISPConflictFolder => true
      case _ => false
    }

    // Delete the problem node (incrementing the node versions for the parent),
    // find or add a conflict folder for the parent, and add the node back to
    // that conflict folder.
    for {
      p0 <- zip.delete.toRight("Validity constraint violation for root program node.").right
      p1 <- Right(p0.incr(lifespanId)).right
      cf <- Right(p1.findChild(isConflictFolder).getOrElse(addConflictFolder(p1))).right
    } yield cf.appendChild(problemNode).incr(lifespanId).top.focus
  }

//  private def fix(zip: Zipper): Either[String, MergePlan] =
//    fixWithMerge(zip).left.flatMap(_ => fixWithConflict(zip))
}
