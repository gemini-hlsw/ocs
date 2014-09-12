package edu.gemini.sp.vcs

import edu.gemini.pot.spdb.ProgramSummoner
import edu.gemini.pot.spdb.ProgramSummoner._
import edu.gemini.spModel.core.{Peer, VersionException, SPProgramID}

import java.io.IOException
import java.util.logging.{Level, Logger}

import scalaz._

sealed trait VcsFailure

object VcsFailure {
  type TryVcs[T] = VcsFailure \/ T

  /**
   * One of several issues occurred while looking up a program in a database.
   */
  case class SummonFailure(f: ProgramSummoner.Failure) extends VcsFailure

  /**
   * Indicates that the user tried to do something for which he doesn't have
   * permission.
   */
  case class Forbidden(why: String) extends VcsFailure

  /**
   * Some exception occurred trying to work with the version control system.
   * If it is an IOException, it is likely a network problem or the server is
   * down. Otherwise, it's probably a server error.
   */
  case class VcsException(ex: Exception) extends VcsFailure

  /**
   * Indicates that the program you're trying to commit is out of date with
   * respect to the server's version.  Commit only works when the incoming
   * program is strictly newer than the existing version.
   */
  case object NeedsUpdate extends VcsFailure

  /**
   * Indicates that the program you're trying to commit has conflicts which
   * must be resolved before committing.
   */
  case object HasConflict extends VcsFailure

  /**
   * An unexpected problem performing a Vcs operation.
   * @param why what happened
   */
  case class Unexpected(why: String) extends VcsFailure


  def explain(f: ProgramSummoner.Failure, opName: String): String =
    f match {
      case IdNotFound(i)               =>
        s"Cound not find $i in the database."
      case IdClash((k0,i0), (k1,i1))   =>
        assert((k0 != k1) || (i0 != i1))
        if (k0 == k1) s"Another program ($i1) with the same internal key as $i0 already exists in the database."
        else s"There is another program in the database with ID '$i0'.  Give your program a new ID and try again."
      case KeyAlreadyExists((k,e),i)   =>
        if (e == i) s"Could not $opName $e, it already exists."
        else s"Could not $opName $e, another program ($i) with the same internal key already exists."
      case IdAlreadyExists((k0, e),k1) =>
        if (k0 == k1) s"Could not $opName $e, it already exists."
        else s"Could not $opName $e, a distinct program with the same ID already exists."
    }

  def explain(f: VcsFailure, id: SPProgramID, opName: String, peer: Option[Peer]): String = {
    val Log = Logger.getLogger(VcsFailure.getClass.getName)
    val peerName = peer.map { p => s"${p.host}:${p.port}" }.getOrElse("remote host")
    val (msg, ex) = f match {
      case SummonFailure(sf)             =>
        (explain(sf, opName), None)
      case Forbidden(why)                =>
        ("Denied permission to %s %s: %s".format(opName, id, why), None)
      case VcsException(ex: VersionException) =>
        (ex.getLongMessage(peerName), Some(ex))
      case VcsException(io: IOException) =>
        ("There was a problem communicating with the server: %s.  Try again later.".format(Option(io.getMessage).getOrElse("unknown network issue")), Some(io))
      case VcsException(ex)              =>
        ("Something went wrong in the database server: %s".format(Option(ex.getMessage).getOrElse("unknown internal server failure")), Some(ex))
      case NeedsUpdate                   =>
        ("You have to update your version of the program before you can commit changes.", None)
      case HasConflict                   =>
        ("You have to resolve all conflicts in your program before you can commit changes.", None)
      case Unexpected(msg)               =>
        ("The changes in the database could not be merged with your version of the program: " + msg, None)
    }
    Log.log(Level.WARNING, s"Trouble with $opName from/to $peerName: $msg", ex.orNull)
    msg
  }
}

