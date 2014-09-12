package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

object Investigators {
  
  // Lenses
  val pi:Lens[Investigators,PrincipalInvestigator] = Lens.lensu((a, b) => a.copy(pi = b), _.pi)
  val cois:Lens[Investigators,List[CoInvestigator]] = Lens.lensu((a, b) => a.copy(cois = b), _.cois)
  val all:Lens[Investigators, List[Investigator]] = Lens.lensu((a, b) => sys.error("this lens is read-only"), _.all)
  
  def empty = Investigators(PrincipalInvestigator.empty, Nil)

  def apply(p: M.Proposal) = {
    new Investigators(p.getInvestigators)
  }

}

case class Investigators(pi: PrincipalInvestigator, cois: List[CoInvestigator]) {

  private def this(m: M.Investigators) = this(
    PrincipalInvestigator(m.getPi),
    m.getCoi.asScala.map(CoInvestigator(_)).toList)

  def mutable(n:Namer) = {
    val m = Factory.createInvestigators()
    m.setPi(pi.mutable(n))
    m.getCoi.addAll(cois.map(_.mutable(n)).asJava)
    m
  }

  def all:List[Investigator] = pi :: cois

}