package edu.gemini.util.security.policy

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.gemini.security.UserRolePrivileges
import edu.gemini.spModel.gemini.security.UserRolePrivileges._
import edu.gemini.util.security.permission._
import edu.gemini.util.security.principal._

import java.security.{AccessControlException, Principal, Permission}
import javax.security.auth.Subject
import java.util.logging.{Level, Logger}

import scala.util.Try
import scalaz._
import Scalaz._
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.util.security.principal.AffiliatePrincipal
import edu.gemini.util.security.permission.PiPermission
import edu.gemini.util.security.permission.NgoPermission
import scala.Some
import edu.gemini.util.security.principal.ProgramPrincipal
import edu.gemini.util.security.permission.StaffPermission

/** A policy that implies permissions based on a set of Principals. */
class ImplicitPolicy private (db: IDBDatabaseService, ps: Set[Principal]) {
  val Log = Logger.getLogger(classOf[ImplicitPolicy].getName)

  def implies(p: Permission): Boolean = {
    val b = p match {

      // VisitorPermission
      case VisitorPermission(None)      => ps.exists { case VisitorPrincipal(_) => true; case _ => false }
      case VisitorPermission(Some(pid)) => ps.contains(VisitorPrincipal(pid))

      // Staff permission for a given program
      case StaffPermission(Some(pid)) =>
        ps.contains(StaffPrincipal.Gemini) || // Gemini staff
        ps.contains(VisitorPrincipal(pid)) || // Visitor for this program
        hasAny(staffUserPrincipals(pid))      // Staff contact for this program

      // Staff permission generally
      case StaffPermission(None) =>
        ps.contains(StaffPrincipal.Gemini) // Gemini staff

      // NGO Permission is implied by the presence of a correct NGO principal
      case NgoPermission(oid) =>
        hasAny(~oid.map(ngoPrincipals)) || hasAny(~oid.map(ngoUserPrincipals))

      // PI permission if there's no ID is always true (!)
      // PI Permission for an ID is implied by the presence of a program key OR a user key
      case PiPermission(None) => true // !
      case PiPermission(Some(id)) =>
        ps.contains(ProgramPrincipal(id)) || // program key
        hasAny(piUserPrincipals(id))         // user principals

      // Read permission can be defined in terms of implied roles
      case ProgramPermission.Read(id) =>
        isStaff(id) || isNGO(id) || isPI(id) || isLibraryProgram(id)

      // Otherwise no, but we don't expect this so log it
      case _ =>
        Log.log(Level.WARNING, "Checking unexpected permission " + p, new Exception)
        false

    }
    // println("*** checked " + p + " with principals: " + ps + " => " + b)
    b
  }

  def hasAny(others: List[Principal]): Boolean =
    ps.exists(others.contains)

  /** Retrieve the NGO principals associated with the specified program, if any. */
  def ngoPrincipals(id: SPProgramID): List[Principal] =
    for {
      p <- spProg(id).toList
      n <- Option(p.getPIAffiliate)
    } yield AffiliatePrincipal(n)

  /** Retrieve the PI user principal(s) associated with the specified program, if any. */
  def piUserPrincipals(id: SPProgramID): List[Principal] =
    ~spProg(id).map(_.getPIInfo.getEmail).map(userPrincipalsForEmails)

  /** Retrieve the NGO user principal(s) associated with the specified program, if any. */
  def ngoUserPrincipals(id: SPProgramID): List[Principal] =
    ~spProg(id).map(_.getPrimaryContactEmail).map(userPrincipalsForEmails)

  /** Retrieve the Staff user principal(s) associated with the specified program, if any. */
  def staffUserPrincipals(id: SPProgramID): List[Principal] =
    ~spProg(id).map(_.getContactPerson).map(userPrincipalsForEmails)

  /** Calculate old-style privileges for the given program. */
  def userRolePrivileges(id: SPProgramID): UserRolePrivileges =
    if (isStaff(id)) STAFF
    else if (isNGO(id)) NGO
    else if (isPI(id)) PI
    else NOUSER

  def isStaff(id: SPProgramID): Boolean =
    implies(StaffPermission(Some(id)))

  def isNGO(id: SPProgramID): Boolean =
    implies(NgoPermission(Some(id)))

  def isPI(id: SPProgramID): Boolean =
    implies(PiPermission(Some(id)))

  def isLibraryProgram(id: SPProgramID):Boolean =
    spProg(id).map(_.isLibrary).getOrElse(false)

  def spProg(id: SPProgramID):Option[SPProgram] =
    Option(db.lookupProgramByID(id)).map(_.getDataObject.asInstanceOf[SPProgram])

  def userPrincipalsForEmails(s:String):List[UserPrincipal] =
    splitEmails(s).map(UserPrincipal(_))

  def splitEmails(s:String):List[String] =
    ~Option(s).map(_.split("""[^\w@.\-]+""").toList)

}


object ImplicitPolicy {

  import java.awt.{ AWTEvent, EventQueue }
  import scalaz.effect.IO
  import edu.gemini.util.security.auth.keychain._, Action._
  import scala.collection.JavaConverters._

  // This is a hack that says "don't check the policy more than once for a given permission during
  // processing of the same AWT event" ... this is not entirely ethical since you might try again
  // with another set of principles, or the db might have changed. However it is a critical
  // optimization in the OT; everything drags if we don't do this.
  // TODO: sort this out
  private object EventCache {

    private var previousEvent: Option[AWTEvent] = None
    private val cache: collection.mutable.Map[Permission, Boolean] = collection.mutable.Map()

    // EventQueue.getCurrentEvent sometimes throws a NPE, at least in headless
    // mode, so we'll wrap it here.

    def check(p: Permission)(a: => Boolean): Boolean =
      Option(Try(EventQueue.getCurrentEvent).toOption.orNull) match {
        case None => a
        case o =>
          if (o != previousEvent) {
            previousEvent = o
            cache.clear()
          }
          cache.getOrElseUpdate(p, a)
      }

  }

  def hasPermission(db: IDBDatabaseService, ps: Set[Principal], p: Permission): IO[Boolean] =
    IO(EventCache.check(p)(new ImplicitPolicy(db, ps).implies(p)))

  def hasPermission(db: IDBDatabaseService, kc: KeyChain, p: Permission): Action[Boolean] =
    kc.selection.flatMap {
      case Some((peer, key)) => hasPermission(db, Set[Principal](key.get._1), p).liftIO[Action]
      case None =>              hasPermission(db, Set[Principal](),           p).liftIO[Action]
    }

  val forJava = ImplicitPolicyForJava

}

// Java code should use this one. It's more java-y
object ImplicitPolicyForJava {

  import edu.gemini.util.security.auth.keychain._, Action._
  import scala.collection.JavaConverters._

  def hasPermission(db: IDBDatabaseService, ps: java.util.Collection[Principal], p: Permission): Boolean =
    ImplicitPolicy.hasPermission(db, ps.asScala.toSet, p).unsafePerformIO

  def hasPermission(db: IDBDatabaseService, principal: Principal, p: Permission): Boolean =
    ImplicitPolicy.hasPermission(db, Set(principal), p).unsafePerformIO

  // N.B. this swallows KeyFailure. May or may not be a problem.
  def hasPermission(db: IDBDatabaseService, kc: KeyChain, p: Permission): Boolean =
    ImplicitPolicy.hasPermission(db, kc, p).run.unsafePerformIO.fold({
      case KeyFailure.KeychainLocked => false
      case f                         => throw f.toException
    }, identity)

  def checkPermission(db: IDBDatabaseService, ps: java.util.Collection[Principal], p: Permission): Unit =
    if (hasPermission(db, ps, p)) () else fail(p)

  def checkPermission(db: IDBDatabaseService, principal: Principal, p: Permission): Unit =
    if (hasPermission(db, principal, p)) () else fail(p)

  def checkPermission(db: IDBDatabaseService, kc: KeyChain, p: Permission): Unit =
    if (hasPermission(db, kc, p)) () else fail(p)

  private def fail(p: Permission): Nothing =
    throw new AccessControlException("permission denied: " + p, p)

}











