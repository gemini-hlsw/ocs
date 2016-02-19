package edu.gemini

import java.util.concurrent.atomic.AtomicReference
import java.util.logging.{Logger, Level}

import edu.gemini.dataman.core.{DmanFailure, DmanAction}
import edu.gemini.gsa.query.GsaResponse

package object dataman {
  val DatamanLogger = Logger.getLogger("edu.gemini.dataman")

  private val DetailLevelRef = new AtomicReference(Level.FINE)
  private val JsonLevelRef   = new AtomicReference(Level.FINE)

  def DetailLevel: Level                = DetailLevelRef.get
  def DetailLevel_=(level: Level): Unit = DetailLevelRef.set(level)

  def JsonLevel: Level                = JsonLevelRef.get
  def JsonLevel_=(level: Level): Unit = JsonLevelRef.set(level)

  implicit class DmanOps[A](r: => GsaResponse[A]) {
    def liftDman: DmanAction[A] =
      r.leftMap(DmanFailure.QueryFailure).liftDman
  }

}
