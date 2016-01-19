package edu.gemini.spModel

import scalaz.Order
import scalaz.std.anyVal._

package object obs {

  implicit val ObservationStatusOrder: Order[ObservationStatus] =
    Order.orderBy(_.ordinal())

}
