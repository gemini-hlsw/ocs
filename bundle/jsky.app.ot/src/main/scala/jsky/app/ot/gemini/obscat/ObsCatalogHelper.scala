package jsky.app.ot.gemini.obscat

import jsky.app.ot.OT
import jsky.catalog.{ArraySearchCondition, QueryResult}
import edu.gemini.pot.spdb.IDBQueryRunner
import edu.gemini.pot.client.SPDB
import jsky.app.ot.shared.gemini.obscat.{ObsCatalogInfo, ObsQueryFunctor}
import jsky.util.gui.ProgressPanel
import jsky.catalog.skycat.SkycatConfigEntry
import scala.collection.JavaConverters._
import scala.actors.Futures.{future, awaitAll}
import scala.actors.Future
import java.util.logging.{Level, Logger}
import edu.gemini.util.trpc.client.TrpcClient
import edu.gemini.util.security.auth.keychain.Action._
import java.net.{SocketTimeoutException, ConnectException}
import scalaz._
import Scalaz._
import scala.util.control.Exception._
import scala.swing.Dialog
import edu.gemini.spModel.core.{Peer, SPProgramID}

// A representation of our databases
sealed trait DB {
  def isLocal: Boolean = false
}
case object Local extends DB {
  override def isLocal: Boolean = true
}
case class Remote(peer:Peer) extends DB

object ObsCatalogHelper {

  private val LOG = Logger.getLogger(ObsCatalogHelper.getClass.getName)
  private val TIMEOUT_MS = 3 * 60 * 1000

  // Some awful crap so we can deal with bare java.util.Vector
  type JVec = java.util.Vector[Object]
  object JVec {
    def apply[A <: AnyRef](ts: Traversable[A]): JVec = {
      val v = new JVec
      ts.foreach(v.add)
      v
    }
  }

  // Result "types" ... :-/
  type DataRow = JVec
  type IdRow = JVec
  case class ResultRow(data: DataRow, ids: IdRow, db: DB)
  type Result = Traversable[ResultRow]

  // All the databases we know about
  private def databases(includeRemote: Boolean): List[DB] = {
    val remote = if (includeRemote) OT.getKeyChain.peers.map(_.toList.map(Remote.apply)).unsafeRun.fold(e => List(), identity)
                 else List.empty[DB]
    Local :: remote
  }

  // Run a future in the current auth context
  private def authFuture[A](a: => A): Future[A] =
    future(a)

  // Run a computation with a progress panel in view
  private def withProgressPanel[A](f: => A): A = {
    val p = ProgressPanel.makeProgressPanel("Searching the science program database ...")
    p.start()
    p.getStopButton.setVisible(false)
    try f finally p.stop()
  }

  // Construct a functor for a given set of query args. It turns out we can do some simplifications
  // that speed things up enough that it's worth the trouble.
  private def functorFor(queryArgs: ObsCatalogQueryArgs): ObsQueryFunctor = {

    // Optimization: remove empty array search conditions ... DUH
    val conds = queryArgs.getConditions.filter {
      case ac: ArraySearchCondition => Option(ac.getValues).exists(_.nonEmpty)
      case _ => true
    }

    new ObsQueryFunctor(conds, queryArgs.getInstruments, queryArgs.getInstConditions)
  }

  // N.B this is ridiculously unsafe, but so is everything else the catalog does. It will blow up with
  // an exception if it stops working (as opposed to doing something wrong)
  private def pid(data: DataRow) = data.get(0).asInstanceOf[SPProgramID]

  // Entry point. Run the specified query against all databases.
  def query(queryArgs: ObsCatalogQueryArgs, configEntry: SkycatConfigEntry, includeRemote: Boolean): QueryResult = {

    // Run the requested query on the given runner. DB is used for error reporting.
    def run(db: DB, qr: IDBQueryRunner): Result = {
      LOG.info("Querying " + db)
      val f0 = qr.queryPrograms(functorFor(queryArgs))
      val ds = f0.getResult.asScala.map(_.asInstanceOf[DataRow])
      val is = f0.getIds.asScala.map(_.asInstanceOf[IdRow])
      assert(ds.length == is.length)
      (ds, is).zipped.map((d, i) => ResultRow(d, i, db))
    }

    // Show the progress panel
    withProgressPanel {

      // Start our queries running
      lazy val fs: List[Future[Validation[QueryFailure, Result]]] = databases(includeRemote).map {

        // Local queries run, um, locally
        case Local => authFuture {
          Validation.fromEither(allCatch.either(run(Local, SPDB.get.getQueryRunner(OT.getUser))).left.map(e => QueryFailure(Local, e)))
        }

        // Remote ones via TRPC. Result type ends up the same
        case db@Remote(peer) => authFuture {
          TrpcClient(peer, 10 * 1000, TIMEOUT_MS).withKeyChain(OT.getKeyChain).apply(r => run(db, r[IDBQueryRunner])).fold(QueryFailure(db, _).fail, _.success)
        }

      }

      // Await results and warn if any didn't finish. If this is the case we just need to make this timeout
      // longer; underlying socket timeouts should happen first.
      val all = awaitAll(TIMEOUT_MS, fs: _*).asInstanceOf[List[Option[Validation[QueryFailure, Result]]]]
      val noAnswerDbs = all.zip(databases(includeRemote)).filter(_._1.isEmpty).map(_._2)
      if (noAnswerDbs.size > 0) {
        val noAnswerString = noAnswerDbs.map {
          case Local     => "Local"
          case Remote(p) => p.displayName()
        }.mkString(", ")

        LOG.warning("Got NONE from rows query attempts; awaitAll timeout is not long enough: " + noAnswerString)
        Dialog.showMessage(
          message = s"Timeout. Results from $noAnswerString are not included.",
          title = "Query Timeout",
          messageType = Dialog.Message.Warning
        )
      }

      // Collapse rows and filter out remote obs in programs that exist locally
      val allRows: List[ResultRow] = all.flatten.map(_.fold(_ => Nil, identity)).flatten
      val localPids: Set[SPProgramID] = allRows.filter(_.db == Local).map(r => pid(r.data)).toSet
      val filtered = allRows.filterNot(r => r.db != Local && localPids.contains(pid(r.data)))

      // Collapse our failures and show them to the user
      val failures: List[QueryFailure] = all.flatten.map(_.fail.toOption).flatten
      if (failures.nonEmpty) {
        Dialog.showMessage(
          message = "Some results are unavailable:\n\n" + failures.map(_.msg).mkString("\n\n"),
          title = "Query Failure",
          messageType = Dialog.Message.Warning
        )
      }

      // Return the results
      new ObsCatalogQueryResult(
        configEntry,
        JVec(filtered.map(_.data)),
        JVec(filtered.map(_.ids)),
        filtered.map(_.db).toBuffer.asJava, // needs to be mutable :-(
        ObsCatalogInfo.getFieldDescr)

    }

  }


  case class QueryFailure(db: DB, e: Throwable) {
    val msg = {

      val desc = db match {
        case Local => "local database"
        case Remote(peer) => "database on %s:%d".format(peer.host, peer.port)
      }

      e match {

        case _: ConnectException =>
          """|Could not connect to the %s.
            |The database may be down for maintainance, or you may have a network issue.""".stripMargin.format(desc)

        case _: NoSuchElementException =>
          """|The %s is not accepting query requests right now.
            |Please try again in a few minutes.""".stripMargin.format(desc)

        case _: SocketTimeoutException =>
          """|The %s did not return results in a timely manner.
            |Please narrow your search and try again.""".stripMargin.format(desc)

        case _ =>
          LOG.log(Level.WARNING, "Query failed on %s".format(desc), e)
          """|Error querying the %s.
            |  %s""".stripMargin.format(desc, e.getMessage)

      }
    }
  }


}
