package edu.gemini.sp.vcs

import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.pot.sp.{SPNodeKey, ISPNode, ISPProgram}
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.util.security.permission.{VisitorPermission, StaffPermission}
import edu.gemini.util.security.policy.ImplicitPolicy
import edu.gemini.pot.spdb.IDBDatabaseService

import java.security.{Principal, Permission}
import scala.util.Try

/**
 * Used to detects and correct changes made to staff-only fields.  Should
 * probably be generalized for any GeminiPermission and not just
 * StaffPermission.
 */
private[vcs] object StaffOnlyFieldProtect {

  private def hasPermission(db: IDBDatabaseService, p: Permission, user: Set[Principal]): Boolean =
    ImplicitPolicy.hasPermission(db, user, p).unsafePerformIO()

  private def isStaff(db: IDBDatabaseService, pid: SPProgramID, user: Set[Principal]): Boolean =
    hasPermission(db, new StaffPermission(pid), user) || hasPermission(db, new VisitorPermission(pid), user)

  private def isSuperStaff(db: IDBDatabaseService, user: Set[Principal]): Boolean =
    hasPermission(db, new StaffPermission(), user) || hasPermission(db, new VisitorPermission(), user)

  def canChangeStaffOnlyFields(db: IDBDatabaseService, pending: ISPProgram, committed: ISPProgram, user: Set[Principal]): Boolean = {
    // Don't want them to be able to change the gem contact to their own
    // user key and then update since we would think that you have staff
    // permission when in fact in the database you wouldn't.
    def contact(p: ISPProgram): Option[String] =
      Option(p.getDataObject.asInstanceOf[SPProgram].getContactPerson).map(_.trim.toLowerCase)

    def gemContactChanged: Boolean = contact(committed) != contact(pending)

    isSuperStaff(db, user) || (!gemContactChanged && isStaff(db, committed.getProgramID, user))
  }

  // Read as pending node, matching committed node (if any)
  type NodePair = (ISPNode, Option[ISPNode])

  // Matches up all protected nodes in the committed and pending programs.
  // The committed program is the one that is in the ODB (remote peer) while
  // the pending program pending commit (e.g., in a user's OT)
  def protectedNodes(pending: ISPProgram, committed: ISPProgram): Iterable[NodePair] = {
    def initWithPending(m: Map[SPNodeKey, NodePair], n: ISPNode): Map[SPNodeKey, NodePair] = {
      val m0 = if (n.hasStaffOnlyFields) m + (n.getNodeKey -> ((n, None))) else m
      (m0/:n.children) { initWithPending }
    }

    def matchWithCommitted(m: Map[SPNodeKey, NodePair], n: ISPNode): Map[SPNodeKey, NodePair] = {
      val m0 = m.get(n.getNodeKey).fold(m) { case (exNode, _) =>
        m.updated(n.getNodeKey, (exNode, Some(n)))
      }
      (m0/:n.children) { matchWithCommitted }
    }

    matchWithCommitted(initWithPending(Map.empty[SPNodeKey, NodePair], pending), committed).values
  }
}