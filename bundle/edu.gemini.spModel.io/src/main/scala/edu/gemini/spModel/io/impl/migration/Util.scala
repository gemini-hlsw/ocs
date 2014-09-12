package edu.gemini.spModel.io.impl.migration

import edu.gemini.spModel.pio.{Param, ParamSet, Container}
import edu.gemini.pot.sp.SPComponentType

import scala.collection.JavaConverters._
import edu.gemini.spModel.io.impl.SpIOTags
import edu.gemini.spModel.data.ISPDataObject

/**
 * Utilities for working with the Pio structures.
 */
object Util {
  implicit def pimpContainer(c: Container) = new Object {
    // awkward because Container.getContainers is untyped
    def containers: List[Container] =
      c.getContainers.asInstanceOf[java.util.List[Container]].asScala.toList

    def paramSets: List[ParamSet] =
      c.getParamSets.asInstanceOf[java.util.List[ParamSet]].asScala.toList

    def isSpType(t: SPComponentType): Boolean =
      c.getType == t.broadType.value && c.getSubtype == t.narrowType

    def dataObject: Option[ParamSet] =
      paramSets.find { ps => ps.getKind == ISPDataObject.PARAM_SET_KIND }

    def params(names: Set[String]): Map[String, String] =
      dataObject.map {
        _.getParams.asScala.collect {
          case p: Param if names.contains(p.getName) => p.getName -> p.getValue
        }.toMap
      }.getOrElse(Map.empty)
  }

  class ObsContainer(obs: Container) {
    def instrument(spType: SPComponentType): Option[Container] =
      obs.containers.find { c => c.getKind == SpIOTags.OBSCOMP && c.isSpType(spType) }

    def topLevelSequence: Option[Container] =
      obs.containers.find { c => c.getKind == SpIOTags.SEQCOMP }

    // Find the first iterator of the matching type, if any
    def findIterator(spType: SPComponentType): Option[Container] = {
      def findSubIterator(iterators: List[Container]): Option[Container] =
        iterators match {
          case Nil    => None
          case h :: t => if (h.isSpType(spType)) Some(h)
                         else findSubIterator(h.containers ::: t)
        }

      topLevelSequence.flatMap { tls => findSubIterator(tls.containers) }
    }
  }
}
