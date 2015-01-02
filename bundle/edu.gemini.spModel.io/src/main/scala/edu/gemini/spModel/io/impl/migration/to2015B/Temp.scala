package edu.gemini.spModel.io.impl.migration.to2015B

import edu.gemini.pot.sp.SPComponentType

import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.pio.{Pio, Document, ParamSet}

// just to prevent a mergec conflict; this will move into To2015B

object Temp {
  import PioSyntax._

  private val PioFactory = new PioXmlFactory()

  val SYS_ASA_MAJOR_PLANET = "AsA major planet"
  val SYS_ASA_MINOR_PLANET = "AsA minor planet"
  val SYS_ASA_COMET        = "AsA comet"
  val SYS_JPL_MAJOR_PLANET = "JPL major planet"
  val SYS_JPL_MINOR_BODY   = "JPL minor body"
  val SYS_MPC_MINOR_PLANET = "MPC minor planet"
  val SYS_MPC_COMET        = "MPC comet"

  val SYS_SOLAR_OBJECT     = "Solar system object"

  val conversions: List[Document => Unit] = List(
    convertComet,
    convertMinorPlanet,
    convertSolar
  )

  private val convertComet       = convertSystem(SYS_JPL_MINOR_BODY)(SYS_ASA_COMET, SYS_MPC_COMET)()
  private val convertMinorPlanet = convertSystem(SYS_MPC_MINOR_PLANET)(SYS_ASA_MINOR_PLANET)()

  private val convertSolar =
    convertSystem(SYS_SOLAR_OBJECT)(SYS_ASA_MAJOR_PLANET, SYS_JPL_MAJOR_PLANET) { ps =>
      val n = ps.getParam("name")
      Pio.addParam(PioFactory, ps, "object", n.getValue.toUpperCase)
    }

  private def convertSystem(to: String)(from: String*)(f: ParamSet => Unit = _ => ()): Document => Unit = { d =>
    val systems = Set(from: _*)
    val names = Set("base", "spTarget")
    for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets if names(ps.getName)
      p   <- Option(ps.getParam("system")).toList if systems(p.getValue)
    } p.setValue(to)
  }

}


