package edu.gemini.spModel.core

/** Point in time, used as the basis for proper motion calculations. */
sealed trait CoordinatesEpoch { 

  /** This `CoordinatesEpoch` in Unix time. */
  def toUnixTime: Long

}

object CoordinatesEpoch {

  case object J2000 extends CoordinatesEpoch {
    def toUnixTime = 0L // TODO
  }

  case object B1959 extends CoordinatesEpoch {
    def toUnixTime = 0L // TODO
  }
  
}

