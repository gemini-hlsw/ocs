package edu.gemini.spModel.core

import scalaz._, Scalaz._

/** Epoch in Gregorian (?) years. */
final case class Epoch(year: Double)


object Epoch {

  val year: Epoch @> Double = Lens.lensu((a, b) => a.copy(year = b), _.year)

  val J2000 = Epoch(2000.0)

}



