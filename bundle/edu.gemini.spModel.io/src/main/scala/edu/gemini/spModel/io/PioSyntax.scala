package edu.gemini.spModel.io

import edu.gemini.pot.sp.{SPComponentType, SPNodeKey}
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.pio.{Container, ContainerParent, ParamSet}

import scala.collection.JavaConverters._
import scala.util.Try


/** Syntax to simplify PIO manipulation.
 */
object PioSyntax {

  implicit class ContainerParentOps(p: ContainerParent) {

    def containers: List[Container] =
      p.getContainers.asInstanceOf[java.util.List[Container]].asScala.toList

    def allContainers: List[Container] = {
      val cs = containers
      cs ++ cs.flatMap(_.allContainers)
    }

    def findContainers(spc: SPComponentType): List[Container] =
      allContainers.filter(_.componentType.contains(spc))
  }

  implicit class ContainerOps(c: Container) {

    def nodeKey: Option[SPNodeKey] =
      Try(new SPNodeKey(c.getKey)).toOption

    def componentType: Option[SPComponentType] =
      Try(SPComponentType.getInstance(c.getType, c.getSubtype)).toOption

    def paramSets: List[ParamSet] =
      c.getParamSets.asInstanceOf[java.util.List[ParamSet]].asScala.toList

    def dataObject: Option[ParamSet] =
      paramSets.find(_.getKind == ISPDataObject.PARAM_SET_KIND)

    def allParamSets: List[ParamSet] = {
      val ps = paramSets
      ps ++ ps.flatMap(_.allParamSets)
    }

  }

  implicit class ParamSetOps(p: ParamSet) {

    def paramSets: List[ParamSet] =
      p.getParamSets.asScala.toList

    def paramSets(n: String): List[ParamSet] =
      paramSets.filter(_.getName == n)

    def paramSet(n: String): Option[ParamSet] =
      paramSets.find(_.getName == n)

    def allParamSets: List[ParamSet] = {
      val ps = paramSets
      ps ++ ps.flatMap(_.allParamSets)
    }

    def value(key: String): Option[String] =
      Option(p.getParam(key)).map(_.getValue)

    def long(key: String): Option[Long] =
      Option(p.getParam(key)).map(_.getValue.toLong)

    def double(key: String): Option[Double] =
      Option(p.getParam(key)).map(_.getValue.toDouble)

  }

}
