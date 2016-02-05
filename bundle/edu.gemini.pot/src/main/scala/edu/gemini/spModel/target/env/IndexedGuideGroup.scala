package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.Tuple2

case class IndexedGuideGroup(index: Integer, group: GuideGroup)

object IndexedGuideGroup {
  def fromTuple(t: Tuple2[Integer,GuideGroup])        = IndexedGuideGroup(t._1(), t._2())
  def fromReverseTuple(t: Tuple2[GuideGroup,Integer]) = fromTuple(t.swap())
}