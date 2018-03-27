package edu.gemini.spModel.gemini.spModel.template

import java.security.Principal

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.memImpl.{MemTemplateGroup, MemProgram, MemFactory}
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth
import edu.gemini.spModel.gemini.gmos.InstGmosNorth
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.gemini.security.UserRolePrivileges
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.template.ReapplicationFunctor
import org.junit._
import org.junit.Assert._
import java.util.UUID

class ReapplicationFunctorTest {
  val PROG_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d47")
  val TEMPLATE_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d45")
  val TEMPLATE_FOLDER_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d48")
  val TEMPLATE_GROUP_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d49")
  val TEMPLATE_GMOS_OBS_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d46")
  val TEMPLATE_F2_OBS_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d43")
  val GMOS_OBSERVATION_KEY = new SPNodeKey("2f7a8b79-1d10-416a-baf3-9b982f77da53")
  val F2_OBSERVATION_KEY = new SPNodeKey("2f7a8b79-1d10-416a-baf3-9b982f77da51")

  val dbUuid = UUID.randomUUID

  val db = DBLocalDatabase.createTransient()
  val fact = db.getFactory.asInstanceOf[MemFactory]
  val id = "GS-2015A-Q-1"
  val templateProgram = MemProgram.create(TEMPLATE_KEY, SPProgramID.toProgramID(id), dbUuid)

  /**
   * Verify that changes to PA are now overwritten on re-apply
   */
  @Before
  def before() {
    // Build the template
    val templateProg = new SPProgram
    templateProgram.setDataObject(templateProg)

    val templateFolder = fact.createTemplateFolder(templateProgram, TEMPLATE_FOLDER_KEY)
    templateProgram.setTemplateFolder(templateFolder)

    val templateGroup = new MemTemplateGroup(templateProgram, TEMPLATE_GROUP_KEY)

    // The template observation with GMOS-N
    val templateGmosObs = fact.createObservation(templateProgram, Instrument.none, TEMPLATE_GMOS_OBS_KEY)

    // Add GMOS Observation Template
    val tGmosObscomps = new java.util.ArrayList[ISPObsComponent]
    val tGmos = new InstGmosNorth
    // The template sets the position angle to 120
    tGmos.setPosAngle(120)
    // Disperser on template is mirror
    tGmos.setDisperser(DisperserNorth.MIRROR)
    // Set a custom MDF name
    tGmos.setFPUnitCustomMask("Custom name on the template")

    val tGmosObscomp = fact.createObsComponent(templateProgram, tGmos.getType, TEMPLATE_GMOS_OBS_KEY)
    tGmosObscomp.setDataObject(tGmos)

    tGmosObscomps.add(tGmosObscomp)
    templateGmosObs.setObsComponents(tGmosObscomps)
    templateGroup.addObservation(templateGmosObs)

    // Add F2 Observation Template
    val templatef2Obs = fact.createObservation(templateProgram, Instrument.none, TEMPLATE_F2_OBS_KEY)

    val tf2Obscomps = new java.util.ArrayList[ISPObsComponent]
    val tf2 = new Flamingos2
    // The template sets the position angle to 120
    tf2.setPosAngle(150)
    // Set a custom fpu mask name
    tf2.setFpuCustomMask("Custom mask on the template")

    val tf2Obscomp = fact.createObsComponent(templateProgram, tf2.getType, TEMPLATE_F2_OBS_KEY)
    tf2Obscomp.setDataObject(tf2)

    tf2Obscomps.add(tf2Obscomp)
    templatef2Obs.setObsComponents(tf2Obscomps)
    templateGroup.addObservation(templatef2Obs)

    templateFolder.addTemplateGroup(templateGroup)

    // Build an observation with GMOS
    val gmosObs = fact.createObservation(templateProgram, Instrument.none, GMOS_OBSERVATION_KEY)

    val gmosSpObs = new SPObservation
    gmosSpObs.setTitle("Test Observation")
    // Simulate it was created out of the template
    gmosSpObs.setOriginatingTemplate(TEMPLATE_GMOS_OBS_KEY)

    gmosObs.setDataObject(gmosSpObs)
    gmosObs.setSeqComponent(fact.createSeqComponent(templateProgram, SPComponentType.OBSERVER_OBSERVE, GMOS_OBSERVATION_KEY))

    val gmosObscomps = new java.util.ArrayList[ISPObsComponent]
    val gmos = new InstGmosNorth
    // The observation position angle is different
    gmos.setPosAngle(99)
    // Disperser is different too
    gmos.setDisperser(DisperserNorth.B1200_G5301)
    // Override the custom MDF name
    gmos.setFPUnitCustomMask("Custom name on the observation")

    val gmosObscomp = fact.createObsComponent(templateProgram, gmos.getType, GMOS_OBSERVATION_KEY)
    gmosObscomp.setDataObject(gmos)

    gmosObscomps.add(gmosObscomp)
    gmosObs.setObsComponents(gmosObscomps)

    // Build an observation with F2
    val f2Obs = fact.createObservation(templateProgram, Instrument.none, F2_OBSERVATION_KEY)

    val f2SpObs = new SPObservation
    f2SpObs.setTitle("Test Observation")
    // Simulate it was created out of the template
    f2SpObs.setOriginatingTemplate(TEMPLATE_F2_OBS_KEY)

    f2Obs.setDataObject(f2SpObs)
    f2Obs.setSeqComponent(fact.createSeqComponent(templateProgram, SPComponentType.OBSERVER_OBSERVE, F2_OBSERVATION_KEY))

    val f2Obscomps = new java.util.ArrayList[ISPObsComponent]
    val f2 = new Flamingos2
    // The observation position angle is different
    f2.setPosAngle(99)
    // Override the custom MDF name
    f2.setFpuCustomMask("Custom name on the f2 observation")

    val f2Obscomp = fact.createObsComponent(templateProgram, f2.getType, F2_OBSERVATION_KEY)
    f2Obscomp.setDataObject(f2)

    f2Obscomps.add(f2Obscomp)
    f2Obs.setObsComponents(f2Obscomps)

    templateProgram.addObservation(gmosObs)
    templateProgram.addObservation(f2Obs)

    // Put the program on the DB
    db.put(templateProgram)
  }

  /**
   * Verify that changes to PA are now overwritten on re-apply
   */
  @Test
  def testReApplyPA() {
    // Run the reapply
    val functor = new ReapplicationFunctor(UserRolePrivileges.STAFF)
    functor.add(templateProgram.getAllObservations.get(0))
    functor.add(templateProgram.getAllObservations.get(1))
    functor.execute(db, null, new java.util.HashSet[Principal])

    val gmosAfterReaaply = db.lookupObservationByID(new SPObservationID("GS-2015A-Q-1-3")).getObsComponents.get(0).getDataObject.asInstanceOf[InstGmosNorth]
    // Check that the Position Angle was preserved
    assertEquals(99, gmosAfterReaaply.getPosAngle, 0)
    // But the Disperser was reset
    assertEquals(DisperserNorth.MIRROR, gmosAfterReaaply.getDisperser)
  }

  /**
   * REL-814 Verify that changes to custom MDF Mask are now overwritten on re-apply
   */
  @Test
  def testReApplyCustomMDF() {
    // Run the reapply
    val functor = new ReapplicationFunctor(UserRolePrivileges.STAFF)
    functor.add(templateProgram.getAllObservations.get(0))
    functor.add(templateProgram.getAllObservations.get(1))
    functor.execute(db, null, new java.util.HashSet[Principal])

    val gmosAfterReapply = db.lookupObservationByID(new SPObservationID("GS-2015A-Q-1-3")).getObsComponents.get(0).getDataObject.asInstanceOf[InstGmosNorth]
    // Check that the Custom Mask name is preserved
    assertEquals("Custom name on the observation", gmosAfterReapply.getFPUnitCustomMask)
  }

  /**
   * REL-814 Verify that changes to custom MDF Mask are now overwritten on re-apply
   */
  @Test
  def testReApplyCustomMaskNameF2() {
    // Run the reapply
    val functor = new ReapplicationFunctor(UserRolePrivileges.STAFF)
    functor.add(templateProgram.getAllObservations.get(0))
    functor.add(templateProgram.getAllObservations.get(1))
    functor.execute(db, null, new java.util.HashSet[Principal])

    val f2AfterReaaply = db.lookupObservationByID(new SPObservationID("GS-2015A-Q-1-4")).getObsComponents.get(0).getDataObject.asInstanceOf[Flamingos2]
    // Check that the Custom Mask name is preserved
    assertEquals("Custom name on the f2 observation", f2AfterReaaply.getFpuCustomMask)
  }
}