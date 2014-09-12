package jsky.app.ot.shared.spModel.util

import edu.gemini.pot.spdb.{IDBDatabaseService, IDBFunctor, IDBQueryRunner}
import edu.gemini.pot.sp.{ISPRootNode, ISPNode}
import edu.gemini.pot.spdb.IDBFunctor.Priority
import edu.gemini.spModel.core.{Peer, SPProgramID}
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.util.trpc.client.TrpcClient
import java.util.logging.Logger
import java.security.Principal
import scalaz._
import Scalaz._
import edu.gemini.util.security.auth.keychain.KeyChain

/** Functor that creates a program with the specified id if no such program exists. */
case class CreateOrVerifyFunctor private(pid: SPProgramID) extends IDBFunctor {

  private var exception: Option[Exception] =
    None

  def getPriority: Priority =
    Priority.medium

  def execute(db: IDBDatabaseService, node: ISPNode, ps: java.util.Set[Principal]): Unit =
    CreateOrVerifyFunctor.synchronized {

      // Import some stuff we need
      val factory = db.getFactory
      import db._
      import factory._

      // Look up a plan or program
      def lookup: SPProgramID => Option[ISPRootNode] = id =>
        Option(lookupProgramByID(id))

      // Set the title of an ISPRootNode to be the same as its pid
      def init: ISPRootNode => Unit = { n =>
        val o = n.getDataObject.asInstanceOf[ISPDataObject]
        o.setTitle(pid.stringValue)
        n.setDataObject(o)
      }

      // Create a plan or program and add it to the database
      def create: SPProgramID => ISPRootNode =
        createProgram(null, _) <| put

      // Look up, and create/init if it's missing
      lookup(pid).fold(init(create(pid)))(_ => ())

    }

  def setException(ex: Exception): Unit =
    exception = Some(ex)

}

object CreateOrVerifyFunctor {

  val Log = Logger.getLogger(classOf[CreateOrVerifyFunctor].getName)

  /** On successful return the specified plan is guaranteed to exist in the database. */
  def execute(kc: KeyChain, pid: SPProgramID, peer: Peer): Unit = {

    // Called from Java, so ...
    require(pid != null, "pid cannot be null")
    require(peer != null, "peer cannot be null")

    // Ok, try it
    TrpcClient(peer).withKeyChain(kc) { r =>
      r[IDBQueryRunner]
        .execute(CreateOrVerifyFunctor(pid), null)
        .exception
        .foreach(throw _)
    }.fold(throw _, identity)

  }

}

