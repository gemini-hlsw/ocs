package jsky.app.ot.visitlog

import edu.gemini.pot.spdb.IDBQueryRunner
import edu.gemini.spModel.obsrecord.{ObsVisitFunctor, ObsVisit}
import edu.gemini.util.trpc.client.TrpcClient

import java.io.IOException
import java.util.logging.{Level, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.{Swing, Publisher}
import scala.util.{Success, Failure}
import scala.swing.event.Event
import edu.gemini.spModel.core.Peer
import edu.gemini.skycalc.ObservingNight
import edu.gemini.util.security.auth.keychain.KeyChain

object VisitLogClient {
  val Log = Logger.getLogger(classOf[VisitLogClient].getName)

  sealed trait State extends Event {
    def night: ObservingNight
    def visits: List[ObsVisit] = Nil
  }

  case class Loading(night: ObservingNight) extends State
  case class Loaded(night: ObservingNight, override val visits: List[ObsVisit]) extends State
  case class IoProblem(night: ObservingNight, ex: IOException) extends State
  case class Error(night: ObservingNight, ex: Throwable) extends State
}

import VisitLogClient._

case class VisitLogClient(kc: KeyChain, peer: Peer) extends Publisher {
  def load(night: ObservingNight): Unit = {
    publish(Loading(night))

    def publishOnEdt(s: State): Unit = Swing.onEDT(publish(s))

    TrpcClient(peer).withKeyChain(kc) future { r =>
      r[IDBQueryRunner].execute(new ObsVisitFunctor(night), null).visits
    } onComplete {
      case Failure(e: IOException) =>
        Log.log(Level.WARNING, "connection failure", e)
        publishOnEdt(IoProblem(night, e))
      case Failure(t)              =>
        Log.log(Level.SEVERE, t.getMessage, t)
        publishOnEdt(Error(night, t))
      case Success(visits)         =>
        publishOnEdt(Loaded(night, visits))
    }
  }
}
