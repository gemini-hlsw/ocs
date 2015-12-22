package edu.gemini.pit.ui.robot

import scalaz.Lens
import edu.gemini.gsa.client.api.{GsaResult, GsaParams}
import edu.gemini.gsa.client.impl.GsaClientImpl
import edu.gemini.model.p1.immutable.{ObservationMeta, Observation}

object GsaRobot extends ObservationMetaRobot[GsaParams, Int] {

  protected val valueLens: Lens[ObservationMeta, Option[Int]] =
    Lens.lensu((a, b) => a.copy(gsa = b), _.gsa)

  protected def key(o: Observation): Option[GsaParams] = GsaParams.get(o)

  protected def query(o: Observation): Option[Int] =
    key(o).flatMap { k =>
      GsaClientImpl.query(k) match {
        case GsaResult.Success(_, datasets) => Some(datasets.size)
        case _                              => None
      }
    }
}