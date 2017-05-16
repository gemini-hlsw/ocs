package edu.gemini.spModel.io.impl.migration.to2015B

import java.io.{StringReader, StringWriter}

import edu.gemini.pot.sp.{ISPObservation, ISPProgram, ISPTemplateFolder, ISPTemplateGroup, SPComponentType}
import edu.gemini.shared.util.immutable.{None => JNone}
import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.io.impl.{PioSpXmlParser, PioSpXmlWriter}
import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.template.{TemplateGroup, TemplateParameters}
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

// a rudimentary test to make sure it doesn't blow up

class TargetConversionTest extends MigrationTest {

  // Simple conversion from 2015A model.
  @Test
  def testTemplateConversion(): Unit =
    withTestProgram("GS-2015B-T-1.xml", { (_,p) => validateProgram(p) })

  // Read 2015A model, write 2015B model, read back 2015B model, validate.
  @Test
  def roundTrip(): Unit =
    withTestProgram("GS-2015B-T-1.xml", { (odb, p0) =>
      val sw = new StringWriter()
      new PioSpXmlWriter(sw).printDocument(p0)

      val parser = new PioSpXmlParser(odb.getFactory)
      parser.parseDocument(new StringReader(sw.toString)) match {
        case p1: ISPProgram => validateProgram(p1)
        case _              => fail("expecting a science program")
      }
    })

  private def validateProgram(p: ISPProgram): Unit = {
    validateTemplateFolder(p.getTemplateFolder)
    validateB1950(p.getObservations.asScala.find(_.getDataObject.getTitle == "Rigel").get)
  }

  private def validateTemplateFolder(tf: ISPTemplateFolder): Unit = {
    def validateGroup(g: ISPTemplateGroup): Unit = {
      g.getDataObject match {
        case tg: TemplateGroup =>
          assertEquals("blueprint-0", tg.getBlueprintId)
      }

      val tpList = g.getTemplateParameters.asScala.toList.map(_.getDataObject.asInstanceOf[TemplateParameters])
      assertEquals(2, tpList.size)

      // A sidereal and a non-sidereal target
      tpList.map(_.getTarget) match {
        case List(tSidereal, tNonSidereal) =>

          assertTrue(tSidereal.isSidereal)
          assertEquals("Some Sidereal", tSidereal.getName)

          assertTrue(tNonSidereal.isNonSidereal)
          assertEquals("S123456", tNonSidereal.getName)

        case _ =>
          fail("expecting Sidereal, NonSidereal")
      }

      g.getObsComponents.asScala.toList match {
        case List(note) =>
          val txt = note.getDataObject.asInstanceOf[SPNote].getNote
          assertTrue(txt.contains("S123456 failed"))

        case _ =>
          fail("expecting a conversion note")
      }
    }

    tf.getTemplateGroups.asScala.toList match {
      case List(g1, g2) => validateGroup(g2)
      case _            => fail("expecting two template groups")
    }
  }

  def validateB1950(obs: ISPObservation): Unit = {
    // unsafe extravaganza!
    val targetComp = obs.getObsComponents.asScala.find(_.getType == SPComponentType.TELESCOPE_TARGETENV).get
    val toc        = targetComp.getDataObject.asInstanceOf[TargetObsComp]
    val rigel      = toc.getAsterism.targets match { case -\/(t: SiderealTarget) => t ; case _ => sys.error("unpossible") }

    val when  = JNone.instance[java.lang.Long]

    val ra    = rigel.coordinates.ra.toDegrees
    val dec   = rigel.coordinates.dec.toDegrees
    val dra   = rigel.properMotion.map(_.deltaRA.velocity.masPerYear).get
    val ddec  = rigel.properMotion.map(_.deltaDec.velocity.masPerYear).get
    val epoch = rigel.properMotion.map(_.epoch.year).get

//    assertEquals("05:14:32.269", (new HMSFormat).format(ra))
//    assertEquals("-08:12:05.86", (new DMSFormat).format(dec))
    assertEquals("1.30", f"$dra%.2f")
    assertEquals("0.50", f"$ddec%.2f")
    assertEquals(2000.0000, epoch, 0.0000001)

    // Check for the note.
    val noteComp = obs.getObsComponents.asScala.find(_.getType == SPComponentType.INFO_NOTE).get
    val text     = noteComp.getDataObject.asInstanceOf[SPNote].getNote
    assertTrue(text.contains("Rigel"))
  }
}
