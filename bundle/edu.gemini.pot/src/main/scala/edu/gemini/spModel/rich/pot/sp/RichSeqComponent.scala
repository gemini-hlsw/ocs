package edu.gemini.spModel.rich.pot.sp

import edu.gemini.pot.sp.ISPSeqComponent

import scala.collection.JavaConverters._
import collection.immutable.Stream

/**
 * Adds convenience methods to ISPSeqComponent.
 */
class RichSeqComponent(seq: ISPSeqComponent) {
  /**
   * Finds the first sequence component (either this sequence component or a
   * child) which matches the provided predicate (if any).
   */
  def find(pred: ISPSeqComponent => Boolean): Option[ISPSeqComponent] = find(pred, seq)

  private def find(pred: ISPSeqComponent => Boolean, s: ISPSeqComponent): Option[ISPSeqComponent] =
    if (pred(s))
      Some(s)
    else
      for {
        children <- Option(s.getSeqComponents)
        comp     <- find(pred, children.asScala.toList)
      } yield comp

  private def find(pred: ISPSeqComponent => Boolean, lst: List[ISPSeqComponent]): Option[ISPSeqComponent] =
    for {
      h <- lst.headOption
      c <- find(pred, h) orElse find(pred, lst.tail)
    } yield c


  /** A depth-first expansion of this sequence component. */
  def flatten = {
    def flatten(s:ISPSeqComponent):List[ISPSeqComponent] =
      s :: s.getSeqComponents.asScala.toList.flatMap(flatten)
    flatten(seq)
  }
  

}
