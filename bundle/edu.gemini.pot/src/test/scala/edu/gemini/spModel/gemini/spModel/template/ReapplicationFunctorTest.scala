package edu.gemini.spModel.gemini.spModel.template

import java.security.Principal

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.memImpl.{MemObsComponent, MemTemplateGroup, MemProgram, MemFactory}
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.spModel.config.IConfigBuilder
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth
import edu.gemini.spModel.gemini.gmos.{InstGMOSCB, InstGmosNorth}
import edu.gemini.spModel.gemini.obscomp.{SPProgram, SPSiteQuality}
import edu.gemini.spModel.gemini.security.UserRolePrivileges
import edu.gemini.spModel.obs.{ObsPhase2Status, SPObservation}
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.template.ReapplicationFunctor
import edu.gemini.spModel.too.{TooType, Too}
import org.junit._
import org.junit.Assert._
import java.util.UUID

class ReapplicationFunctorTest {
  val PROG_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d47")
  val TEMPLATE_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d45")
  val TEMPLATE_FOLDER_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d48")
  val TEMPLATE_GROUP_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d49")
  val TEMPLATE_OBS_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d46")
  val KEY = new SPNodeKey("2f7a8b79-1d10-416a-baf3-9b982f77da53")

  val dbUuid = UUID.randomUUID

  val db = DBLocalDatabase.createTransient()
  val fact = db.getFactory.asInstanceOf[MemFactory]
  val id = "GS-2015A-Q-1"

  /**
   * Verify that changes to PA are now overwritten on re-apply
   */
  @Test
  def testReApplyPA() {
    // Build the template
    val templateProg = new SPProgram
    val templateProgram = MemProgram.create(TEMPLATE_KEY, SPProgramID.toProgramID(id), dbUuid)
    templateProgram.setDataObject(templateProg)

    val templateFolder = fact.createTemplateFolder(templateProgram, TEMPLATE_FOLDER_KEY)
    templateProgram.setTemplateFolder(templateFolder)

    val templateGroup = new MemTemplateGroup(templateProgram, TEMPLATE_GROUP_KEY)

    // The template observation with GMOS-N
    val templateObs = fact.createObservation(templateProgram, TEMPLATE_OBS_KEY)

    val tObscomps = new java.util.ArrayList[ISPObsComponent]
    val tGmos = new InstGmosNorth
    // The template sets the position angle to 120
    tGmos.setPosAngle(120)
    // Disperser on template is mirror
    tGmos.setDisperser(DisperserNorth.MIRROR)

    val tObscomp = fact.doCreateObsComponent(templateProgram, tGmos.getType, KEY)
    tObscomp.setDataObject(tGmos)

    tObscomps.add(tObscomp)
    templateObs.setObsComponents(tObscomps)

    templateGroup.addObservation(templateObs)
    templateFolder.addTemplateGroup(templateGroup)

    // Build an observation
    val obs = fact.createObservation(templateProgram, KEY)

    val spObs = new SPObservation
    spObs.setTitle("Test Observation")
    // Simulate it was created out of the template
    spObs.setOriginatingTemplate(TEMPLATE_OBS_KEY)

    obs.setDataObject(spObs)
    obs.setSeqComponent(fact.createSeqComponent(templateProgram, SPComponentType.OBSERVER_OBSERVE, KEY))
    templateProgram.addObservation(obs)

    val obscomps = new java.util.ArrayList[ISPObsComponent]
    val gmos = new InstGmosNorth
    // The observation position angle is different
    gmos.setPosAngle(99)
    // Disperser is different too
    gmos.setDisperser(DisperserNorth.B1200_G5301)

    val obscomp = fact.doCreateObsComponent(templateProgram, gmos.getType, KEY)
    obscomp.setDataObject(gmos)

    obscomps.add(obscomp)
    obs.setObsComponents(obscomps)

    // Put the program on the DB
    db.put(templateProgram)

    // Run the reapply
    val functor = new ReapplicationFunctor(UserRolePrivileges.STAFF)
    functor.add(templateProgram.getAllObservations.get(0))
    functor.execute(db, null, new java.util.HashSet[Principal])

    val gmosAfterReaaply = db.lookupObservationByID(new SPObservationID(SPProgramID.toProgramID("GS-2015A-Q-1"), 2)).getObsComponents.get(0).getDataObject.asInstanceOf[InstGmosNorth]
    // Check that the Position Angle was preserved
    assertEquals(99, gmosAfterReaaply.getPosAngle, 0)
    // But the Disperser was reset
    assertEquals(DisperserNorth.MIRROR, gmosAfterReaaply.getDisperser)
  }
}