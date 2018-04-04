package edu.gemini.spModel

import scalaz._
import Scalaz._

package object ictd {

  //
  // Availability Monoid where a0 |+| a1 results in the least available of the
  // two.  For example Installed |+| SummitCabinet = SummitCabinet.
  //
  implicit val MonoidAvailability: Monoid[Availability] =
    new Monoid[Availability] {
      def zero: Availability =
        Availability.Zero

      def append(a0: Availability, a1: => Availability): Availability =
        a0.plus(a1)
    }

}
