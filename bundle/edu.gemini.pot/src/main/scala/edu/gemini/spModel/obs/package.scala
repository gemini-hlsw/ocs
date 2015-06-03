package edu.gemini.spModel

import scalaz.Order

package object obs {

  implicit val ObservationStatusOrder: Order[ObservationStatus] =
    Order.orderBy(_.ordinal())

}
