package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.{ISPFactory, SPNodeKeyLocks, ISPProgram, SPNodeKey}
import edu.gemini.pot.sp.version.VersionMap
import edu.gemini.pot.spdb.{DBIDClashException, IDBDatabaseService}
import edu.gemini.shared.util.VersionComparison.{Same, Newer}
import edu.gemini.sp.vcs2.VcsAction._
import edu.gemini.sp.vcs2.VcsFailure._
import edu.gemini.sp.vcs.log.{OpStore, OpFetch, VcsEventSet, VcsLog}
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.util.security.permission.ProgramPermission
import edu.gemini.util.security.policy.ImplicitPolicy

import java.security.{Permission, Principal}

import edu.gemini.util.security.principal.GeminiPrincipal

import java.util.logging.Logger

import scalaz._
import Scalaz._

/** VcsServer provides access-controlled locked read/write methods to generate
  * actions that read from and write to programs in the database. It also
  * implements the server side of the [[edu.gemini.sp.vcs2.VcsService]]
  * interface. */
class VcsServer(odb: IDBDatabaseService) { vs =>

  import SPNodeKeyLocks.instance

  def hasPermission(p: Permission, user: Set[Principal]): VcsAction[Boolean] =
    VcsAction(ImplicitPolicy.hasPermission(odb, user, p).unsafePerformIO())

  def lookup(id: SPProgramID): VcsAction[ISPProgram] =
    (Option(odb.lookupProgramByID(id)) \/> NotFound(id)).liftVcs


  /** Creates an action that reads from a program with a read lock held
    * provided the caller has permission. */
  def read[A](id: SPProgramID, user: Set[Principal])(body: ISPProgram => A): VcsAction[A] =
    managed(id, user, instance.readLock, instance.readUnlock)(p => body(p).point[VcsAction])

  /** Creates an action that potentially writes to a program with a write lock
    * held provided the caller has permission.  Writing is done on a copy of the
    * associated program and only when successful the copy replaces the current
    * version of the program in the database.
    *
    * The caller provides three functions, `evaluate`, `filter`, and `update`.
    * Evaluate should not modify anything in the progrm but rather calculate
    * a value.  The value is handed to `filter` to decide whether to continue
    * with the write operation (and make the program copy).  In either case,
    * the value is returned as the result of the action.  The `update` takes a
    * copy of the program and the value returned by `evaluate` and mutates the
    * copy.  If successful, the copy replaces the current version of the program
    * in the database.
    *
    * @param id program id
    * @param user authentication
    * @param evaluate a function to compute a value which is used to determine
    *                 whether to continue the write operation
    * @param filter a function that takes the value returned by the `evaluate`
    *               function and returns `true` if the write should continue
    * @param update a function that performs the side effect of updating the
    *               program copy
    * @return the value produced by the `evaluate` function
    */
  def write[A](id:       SPProgramID,
               user:     Set[Principal],
               evaluate: ISPProgram => VcsAction[A],
               filter:   A => Boolean,
               update:   (ISPFactory, ISPProgram, A) => VcsAction[Unit]): VcsAction[A] =
    managed(id, user, instance.writeLock, instance.writeUnlock) { prog =>
      evaluate(prog) >>= { a =>
        if (filter(a)) {
          val cp = odb.getFactory.copyWithSameKeys(prog)
          update(odb.getFactory, cp, a) >> putProg(cp).as(a).liftVcs
        } else {
          VcsAction(a)
        }
      }
    }

  /** Adds the given program to the database, provided it doesn't share the
    * same key or id with an existing program in the database and the user has
    * appropriate keys.
    */
  def add(p: ISPProgram): VcsAction[Unit] = {
    def failIfExists(id: SPProgramID, key: SPNodeKey): VcsAction[Unit] = {
      val alreadyExists = Option(odb.lookupProgramByID(id)).as(IdAlreadyExists(id)).orElse {
        Option(odb.lookupProgram(key)).as(KeyAlreadyExists(id, key))
      }

      alreadyExists.fold(VcsAction.unit)(f => VcsAction.fail(f))
    }

    (Option(p.getProgramID) \/> MissingId).liftVcs >>= { id =>
      locked(p.getProgramKey, instance.writeLock, instance.writeUnlock) {
        failIfExists(id, p.getProgramKey) >> putProg(odb.getFactory.copyWithNewLifespanId(p)).liftVcs
      }
    }
  }

  /** Replaces the given program in the database. */
  def replace(p: ISPProgram): VcsAction[Unit] =
    (Option(p.getProgramID) \/> MissingId).liftVcs >>= { id =>
      locked(p.getProgramKey, instance.writeLock, instance.writeUnlock) {
        putProg(odb.getFactory.copyWithNewLifespanId(p)).liftVcs
      }
    }

  /** Server implementation of `VcsService`. */
  final class SecureVcsService(user: Set[Principal], vcsLog: VcsLog) extends VcsService {
    def geminiPrincipals: Set[GeminiPrincipal] =
      user.collect { case p: GeminiPrincipal => p }

    override def version(id: SPProgramID): TryVcs[VersionMap] =
      vs.read(id, user)(_.getVersions).unsafeRun

    override def add(p: ISPProgram): TryVcs[Unit] =
      (for {
        id <- (Option(p.getProgramID) \/> MissingId).liftVcs
        _  <- accessControlled(id, user) { vs.add(p) }
      } yield ()).unsafeRun

    override def checkout(id: SPProgramID): TryVcs[ISPProgram] =
      vs.read(id, user)(identity).unsafeRun

    override def diffState(id: SPProgramID): TryVcs[DiffState] =
      vs.read(id, user)(DiffState.apply).unsafeRun

    override def fetchDiffs(id: SPProgramID, state: DiffState): TryVcs[ProgramDiff.Transport] =
      vs.read(id, user) { p =>
        vcsLog.log(OpFetch, id, geminiPrincipals)
        ProgramDiff.compare(p, state)
      }.map(_.encode).unsafeRun

    override def storeDiffs(id: SPProgramID, mpt: MergePlan.Transport): TryVcs[Boolean] = {
      def versionCheck(p: ISPProgram, mp: MergePlan): VcsAction[Boolean] =
        mp.compare(p.getVersions) match {
          case Newer => VcsAction(true)
          case Same  => VcsAction(false)
          case _     => VcsAction.fail(NeedsUpdate)
        }

      def conflictCheck(mp: MergePlan): VcsAction[Boolean] =
        if (mp.hasConflicts) VcsAction.fail(HasConflict) else VcsAction(true)

      val mp = mpt.decode
      vs.write[Boolean](id, user,
        p => for {
          vc <- versionCheck(p, mp)
          cc <- conflictCheck(mp)
        } yield vc && cc,
        identity,
        (f, p, _) => (mp.merge(f, p) >> VcsAction(vcsLog.log(OpStore, id, geminiPrincipals))).as(())
      ).unsafeRun
    }

    override def log(id: SPProgramID, offset:Int, length:Int): TryVcs[(List[VcsEventSet], Boolean)] =
      try {
        vcsLog.selectByProgram(id, offset, length).right
      } catch {
        case ex: Exception => VcsException(ex).left
      }
  }

  private def managed[A](id: SPProgramID, user: Set[Principal], lock: SPNodeKey => Unit, unlock: SPNodeKey => Unit)(body: ISPProgram => VcsAction[A]): VcsAction[A] =
    accessControlled(id, user) {
      lookup(id) >>= { p => locked(p.getProgramKey, lock, unlock)(body(p)) }
    }

  private def accessControlled[A](id: SPProgramID, user: Set[Principal])(body: => VcsAction[A]): VcsAction[A] =
    hasPermission(new ProgramPermission.Read(id), user) >>= { hp =>
      if (hp) body
      else {
        VcsServer.Log.info(s"VCS op forbidden: pid=$id, user=[${user.toList.mkString(", ")}]")
        VcsAction.fail(Forbidden(s"You don't have permission to access program '$id'"))
      }
    }

  private def locked[A](k: SPNodeKey, lock: SPNodeKey => Unit, unlock: SPNodeKey => Unit)(body: => VcsAction[A]): VcsAction[A] =
    VcsAction(lock(k)) >> { try { body} finally { unlock(k) } }

  private  def putProg(p: ISPProgram): TryVcs[Unit] =
    \/.fromTryCatchNonFatal(odb.put(p)).leftMap {
      case clash: DBIDClashException => VcsFailure.idClash(clash)
      case ex                        => VcsException(ex)
    }.as(())
}

object VcsServer {
  private val Log = Logger.getLogger(VcsServer.getClass.getName)
}