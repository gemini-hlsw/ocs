package edu.gemini.pit.ui.robot

import edu.gemini.ags.client.api._
import edu.gemini.ags.client.api.AgsResult.Success
import edu.gemini.model.p1.immutable._
import java.net.URL
import scalaz.Lens
import scala.swing.Swing
import edu.gemini.pit.model.Model

/**
 * A background processor that performs AGS checks when the model seems to need them.
 */
object AgsRobot extends ObservationMetaRobot[URL, GuidingEstimation] {

  // The AGS service can come and go and is injected from outside
  private val _agsLock = new Object
  private var _ags: Option[AgsClient] = None
  def ags = _ags
  def ags_=(client: Option[AgsClient]): Unit = {
    _agsLock.synchronized {
      logger.info("AGS Client is now " + client)
      _ags = client
    }
    Swing.onEDT(refresh(model))
  }

  protected val valueLens: Lens[ObservationMeta, Option[GuidingEstimation]] =
    Lens.lensu((a, b) => a.copy(guiding = b), _.guiding)

  private def midPoint: Option[Long] = model map {
    m => m.proposal.semester.midPoint
  }

  protected def key(o: Observation): Option[URL] =
    for {
      mid <- midPoint
      a <- ags
      u <- a.url(o, mid)
    } yield u

  protected def query(o: Observation): Option[GuidingEstimation] =
    for {
      m <- model
      a <- ags
      e <- query(m, a, o)
    } yield e

  private def query(m: Model, a: AgsClient, o: Observation): Option[GuidingEstimation] =
    a.estimateNow(o, m.proposal.semester.midPoint) match {
      case Success(perc) => Some(GuidingEstimation((perc * 100).round.toInt))
      case _ => None
    }
}
