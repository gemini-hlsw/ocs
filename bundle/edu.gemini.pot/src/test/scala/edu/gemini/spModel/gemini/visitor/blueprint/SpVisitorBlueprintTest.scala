package edu.gemini.spModel.gemini.visitor.blueprint

import edu.gemini.spModel.gemini.visitor.VisitorConfig
import org.junit.Test
import org.junit.Assert._
import edu.gemini.spModel.pio.xml.PioXmlFactory

import SpVisitorBlueprint.{NAME_PARAM_NAME, CONFIG_PARAM_NAME}

final class SpVisitorBlueprintTest {

  val fact = new PioXmlFactory

  @Test
  def genericSanityTests(): Unit = {
    val generic = new SpVisitorBlueprint("name", VisitorConfig.GenericVisitor)
    assertEquals("name", generic.name)
    assertEquals(VisitorConfig.GenericVisitor, generic.visitorConfig)

    val ps = generic.toParamSet(fact)

    assertEquals(1, ps.getParams(NAME_PARAM_NAME).size())
    assertEquals("name", ps.getParam(NAME_PARAM_NAME).getValue)

    assertEquals(1, ps.getParams(CONFIG_PARAM_NAME).size())
    assertEquals(VisitorConfig.GenericVisitor.name, ps.getParam(CONFIG_PARAM_NAME).getValue)

  }

  @Test
  def igrinsSanityTests(): Unit = {
    val igrins = new SpVisitorBlueprint("igrins", VisitorConfig.Igrins)
    assertEquals("igrins", igrins.name)
    assertEquals(VisitorConfig.Igrins, igrins.visitorConfig)

    val ps = igrins.toParamSet(fact)

    assertEquals(1, ps.getParams(NAME_PARAM_NAME).size())
    assertEquals("igrins", ps.getParam(NAME_PARAM_NAME).getValue)

    assertEquals(1, ps.getParams(CONFIG_PARAM_NAME).size())
    assertEquals(VisitorConfig.Igrins.name, ps.getParam(CONFIG_PARAM_NAME).getValue)
  }

  @Test
  def igrinsRoundtripTest(): Unit = {
    val igrins = new SpVisitorBlueprint("igrins", VisitorConfig.Igrins)
    assertEquals(igrins, new SpVisitorBlueprint(igrins.toParamSet(fact)))
  }

}
