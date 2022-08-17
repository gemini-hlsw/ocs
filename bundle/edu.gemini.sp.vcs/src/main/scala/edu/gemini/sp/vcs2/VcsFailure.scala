package edu.gemini.sp.vcs2

import java.io.IOException
import java.util.logging.{Level, Logger}

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.pot.spdb.DBIDClashException
import edu.gemini.spModel.core.{VersionException, Peer, SPProgramID}

import scalaz._
import Scalaz._

sealed trait VcsFailure

object VcsFailure {
  /** Indicates that the program has no program id. */
  case object MissingId extends VcsFailure

  /** Indicates that the given program already exists in the database. */
  case class IdAlreadyExists(id: SPProgramID) extends VcsFailure

  /** Indicates that the given program already exists in the database. */
  case class KeyAlreadyExists(id: SPProgramID, key: SPNodeKey) extends VcsFailure

  /** Two distinct programs share the same program id. */
  case class IdClash(id: SPProgramID, key0: SPNodeKey, key1: SPNodeKey) extends VcsFailure

  /** The program associated with the given id could not be found. */
  case class NotFound(id: SPProgramID) extends VcsFailure

  /** Indicates that the user tried to do something for which he doesn't have
    * permission. */
  case class Forbidden(why: String) extends VcsFailure

  /** Indicates that the program you're trying to commit is out of date with
    * respect to the server's version.  Commit only works when the incoming
    * program is strictly newer than the existing version. */
  case object NeedsUpdate extends VcsFailure

  /** Indicates that the program you're trying to commit has conflicts which
    * must be resolved before committing. */
  case object HasConflict extends VcsFailure

  /** Indicates that the local program cannot be merged with the remote
    * program.  For example, because it contains executed observations that
    * would be renumbered. */
  case class Unmergeable(msg: String) extends VcsFailure

  /** Indicates an unexpected problem while performing a vcs operation. */
  case class Unexpected(msg: String) extends VcsFailure

  /** Exception thrown while performing a vcs operation. */
  case class VcsException(ex: Throwable) extends VcsFailure

  /** User cancelled a vcs operation. */
  case object Cancelled extends VcsFailure

  def idClash(ex: DBIDClashException): VcsFailure =
    IdClash(ex.id, ex.existingKey, ex.newKey)

  def explain(f: VcsFailure, id: SPProgramID, op: String, peer: Option[Peer]): String = {

    val peerName = peer.map { p => s"${p.host}:${p.port}" } | "remote host"
    val msg = f match {
      case IdClash(i,_,_)                     =>
        s"There is another program in the database with ID '$i'."

      case NotFound(i)                        =>
        s"$i is not in the database."

      case Forbidden(why)                     =>
        s"Denied permission to $op $id: $why"

      case MissingId                          =>
        "Give your program an id and try again."

      case KeyAlreadyExists(i,k)              =>
        s"Program $i cannot be added because a program with the same internal key ($k) already exists in the database."

      case IdAlreadyExists(i)                 =>
        s"Program $i cannot be added because a program with the same ID already exists in the database."

      case NeedsUpdate                        =>
        "You have to update your version of the program before you can commit changes."

      case HasConflict                        =>
        "You have to resolve all conflicts in your program before you can commit changes."

      case Unmergeable(m)                     =>
        s"Your program could not be merged: $m"

      case Unexpected(m)                      =>
        s"Internal error. The changes in the database could not be merged with your version of the program: $m"

      case VcsException(ex: VersionException) =>
        ex.getLongMessage(peerName)

      case VcsException(io: IOException)      =>
        val m = Option(io.getMessage) | "unknown network issue"
        s"There was a problem communicating with the server: $m.  Try again later."

      case VcsException(ex)                   =>
        val m = Option(ex.getMessage) | "unknown error"
        s"Internal error. Something went wrong in the database server: $m"

      case Cancelled                          =>
        "The action was cancelled."
    }

    val exOpt = f match {
      case VcsException(ex) => Some(ex)
      case _                => None
    }

    val log = Logger.getLogger(VcsFailure.getClass.getName)
    log.log(Level.WARNING, s"VcsFailure $op $peerName: $msg", exOpt.orNull)
    msg
  }
}
