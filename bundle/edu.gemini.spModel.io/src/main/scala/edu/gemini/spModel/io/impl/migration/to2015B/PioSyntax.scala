package edu.gemini.spModel.io.impl.migration.to2015B

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.pio.{ ParamSet, Container, ContainerParent}

import scala.collection.JavaConverters._
import scala.util.Try

object PioSyntax {

  implicit class ContainerParentOps(p: ContainerParent) {

    def containers: List[Container] =
      p.getContainers.asInstanceOf[java.util.List[Container]].asScala.toList

    def allContainers: List[Container] = {
      val cs = containers
      cs ++ cs.flatMap(_.allContainers)
    }

    def findContainers(spc: SPComponentType): List[Container] =
      allContainers.filter(_.componentType.exists(_ == spc))

  }

  implicit class ContainerOps(c: Container) {

    def componentType: Option[SPComponentType] =
      Try(SPComponentType.getInstance(c.getType, c.getSubtype)).toOption

    def paramSets: List[ParamSet] =
      c.getParamSets.asInstanceOf[java.util.List[ParamSet]].asScala.toList

    def allParamSets: List[ParamSet] = {
      val ps = paramSets
      ps ++ ps.flatMap(_.allParamSets)
    }

  }

  implicit class ParamSetOps(p: ParamSet) {

    def paramSets: List[ParamSet] =
      p.getParamSets.asScala.toList

    def allParamSets: List[ParamSet] = {
      val ps = paramSets
      ps ++ ps.flatMap(_.allParamSets)
    }

    def value(key: String): Option[String] =
      Option(p.getParam(key)).map(_.getValue)

  }

}
