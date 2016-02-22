package edu.gemini.spModel

import scalaz._
import Scalaz._

package object guide {

  implicit val ComparatorGuideProbe: java.util.Comparator[GuideProbe] =
    GuideProbe.KeyComparator.instance

//  implicit val OrderingGuideGroup: scala.Ordering[GuideProbe] =


  implicit val OrderGuideGroup: Order[GuideProbe] = Order.fromScalaOrdering

}
