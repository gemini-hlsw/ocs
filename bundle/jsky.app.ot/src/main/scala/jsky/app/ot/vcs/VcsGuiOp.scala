package jsky.app.ot.vcs

import edu.gemini.pot.client.SPDB
import edu.gemini.spModel.core.{VersionException, SPProgramID}
import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.sp.version._
import edu.gemini.sp.vcs._
import edu.gemini.sp.vcs.VcsFailure._
import scalaz.{-\/, \/-}
import java.util.logging.Logger
import edu.gemini.pot.spdb.ProgramSummoner.{IdNotFound, LookupOrFail}
import jsky.app.ot.OT
import scala.collection.JavaConverters._


object VcsGuiOp {
  type Result = Option[TryVcs[(VersionMap, ISPProgram)]]

  val Log = Logger.getLogger(this.getClass.getName)
  def currentUser = OT.getKeyChain.subject.getPrincipals.asScala.toSet

  /**
   * A trait that VcsGuiOp implementations can use to perform tasks that update
   * the GUI or find out whether an operation has been cancelled.
   */
  trait Ui {
    def report(msg: String)
    def isCancelled: Boolean
  }


  // For testing purposes, seconds to delay between steps.
  private val defaultArtificialDelay = 0

  /**
   * Temporary method to add a bit of delay to the operation for demonstration
   * and testing purposes.  Achieves the delay by sleeping in one second
   * intervals until either cancelled or <code>secs</code> seconds have passed.
   *
   * @param secs number of seconds to delay
   *
   * @param gui access to the gui to check for cancellation
   */
  def artificialDelay(secs: Int, gui: Ui) {
    if (secs > 0) {
      for (i <- secs to 0 by -1) {
        if (!gui.isCancelled) Thread.sleep(1000)
      }
    }
  }

  /**
   * Fetches the program with the given id from the given server.
   *
   * @param id id of the program to fetch
   * @param gui UI to maintain informed of the progress
   * @param server server from which to fetch the program
   *
   * @return None if the operation is cancelled by the user, Some fetch
   *         result which, if successful, contains a reference to the remote
   *         program
   */
  def fetchRemote(id: SPProgramID, gui: Ui, server: VcsServer): Option[TryVcs[ISPProgram]] = {
    gui.report("Retrieving program %s from server...".format(id))
    artificialDelay(defaultArtificialDelay, gui)
    val fetchResult = server.fetch(id)
    if (gui.isCancelled) None
    else Some(fetchResult)
  }

  /**
   * Merges the given program with the existing program in the OT's database.
   * @param prog version of the program to be merged with the local one
   *
   * @return the updated local program
   */
  def mergeUpdates(gui: Ui, prog: ISPProgram): TryVcs[ISPProgram] = {
    gui.report("Applying changes to your program...")
    artificialDelay(defaultArtificialDelay, gui)
    VcsLocking(SPDB.get()).merge(LookupOrFail, prog, currentUser)(Update)
  }

  def storeRemote(id: SPProgramID, gui: Ui, server: VcsServer): TryVcs[(VersionMap, ISPProgram)] = {
    gui.report("Sending program %s to server...".format(id))
    artificialDelay(defaultArtificialDelay, gui)

    // Makes a copy of the local program to commit so that it is free to be
    // subsequently updated while sending it to the server.
    for {
      prog <- VcsLocking(SPDB.get()).copy(id)
      jvm  <- server.store(prog)
    } yield (jvm, prog)
  }
}

import VcsGuiOp._

/**
 * Base trait for VCS operations performed in a background thread.
 */
trait VcsGuiOp {
  def name: String

  /**
   * Perform the VCS GUI operation in a background thread.
   * @param id operate on the program associated with the given program id
   * @param gui provides access to the GUI in a thread-safe way
   * @param server remote server with which the operation should work
   *
   * @return None if cancelled, otherwise fetched or updated program depending
   *         on what the operation implementation does
   */
  def apply(id: SPProgramID, gui: Ui, server: VcsServer): Result

  def explanation: PartialFunction[VcsFailure, String] = { case _ if false => "" }
}


/**
 * Performs a VCS update operation in the GUI.
 */
object VcsUpdateOp extends VcsGuiOp {
  val instance = this // For easy access from Java.
  val name     = "update"

  def apply(id: SPProgramID, gui: Ui, server: VcsServer): Result =
    fetchRemote(id, gui, server) map { _ flatMap { remoteProg =>
      mergeUpdates(gui, remoteProg) map { localProg =>
        (remoteProg.getVersions, localProg)
      }
    }}
}

/**
 * Performs a VCS commit operation in the GUI.
 */
object VcsCommitOp extends VcsGuiOp {
  val instance = this // For easy access from Java.
  val name     = "commit"

  def apply(id: SPProgramID, gui: Ui, server: VcsServer): Result =
    Some(storeRemote(id, gui, server))
}

/**
 * Performs a VCS update and commit.
 */
object VcsSyncOp extends VcsGuiOp {
  val instance = this // For easy access form Java.
  val name     = "sync"

  def apply(id: SPProgramID, gui: Ui, server: VcsServer): Result = apply(id, gui, server, 0)

  private val MaxCommitTry = 10

  def apply(id: SPProgramID, gui: Ui, server: VcsServer, count: Int): Result =
    VcsUpdateOp(id, gui, server) flatMap {
      case \/-(_) => VcsCommitOp(id, gui, server) flatMap {
        // If the commit fails because the client needs an update during a sync,
        // it means the program has received an exec event (or other remote
        // update) since the update succeeded.  If that is the case, just retry
        // until it works or fails for some other reason.
        case -\/(up: VcsFailure.NeedsUpdate.type) =>
          if (count < MaxCommitTry) apply(id, gui, server, count + 1)
          else Some(-\/(VcsFailure.Unexpected(s"Your version of $id appears to be incompatible.")))
        case otherResult => Some(otherResult)
      }
      case -\/(SummonFailure(IdNotFound(_))) =>
        Log.warning(s"$id not found in remote database, adding")
        VcsCommitOp(id, gui, server)
      case failure => Some(failure)
    }

  override val explanation: PartialFunction[VcsFailure, String] = {
    case VcsException(ex: VersionException) => ex.getLongMessage // :-\
    case HasConflict => "Your program has been updated but you must resolve conflicting edits before storing changes."
  }
}