package edu.gemini.spModel.io.impl.migration.to2015A

import java.io.{StringReader, StringWriter}

import edu.gemini.pot.sp.{ISPProgram, ISPTemplateFolder, ISPTemplateGroup}
import edu.gemini.spModel.gemini.gmos.InstGmosSouth
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.io.impl.{PioSpXmlParser, PioSpXmlWriter}
import edu.gemini.spModel.template.{TemplateFolder, TemplateGroup, TemplateParameters}
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._

// At least rudimentary evidence that the conversion works.

class TemplateConversionTest extends MigrationTest {

  // Simple conversion from 2014B model.
  @Test
  def testTemplateConversion(): Unit =
    withTestProgram("GS-2014A-Q-999.xml", { (_,p) => validateProgram(p) })

  // Read 2014B model, write 2015A model, read back 2015A model, validate.
  @Test
  def roundTrip(): Unit =
    withTestProgram("GS-2014A-Q-999.xml", { (odb, p0) =>
      val sw = new StringWriter()
      new PioSpXmlWriter(sw).printDocument(p0)

      val parser = new PioSpXmlParser(odb.getFactory)
      parser.parseDocument(new StringReader(sw.toString)) match {
        case p1: ISPProgram => validateProgram(p1)
        case _              => fail("expecting a science program")
      }
    })

  private def validateProgram(p: ISPProgram): Unit = {
    def validateGroup(g: ISPTemplateGroup): Unit = {
      g.getDataObject match {
        case tg: TemplateGroup =>
          assertEquals("blueprint-0", tg.getBlueprintId)
      }

      val tpList = g.getTemplateParameters.asScala.toList.map(_.getDataObject.asInstanceOf[TemplateParameters])
      assertEquals(4, tpList.size)

      // One site quality
      val sq = new SPSiteQuality()
      sq.setCloudCover(SPSiteQuality.CloudCover.PERCENT_50)
      sq.setImageQuality(SPSiteQuality.ImageQuality.PERCENT_70)
      sq.setSkyBackground(SPSiteQuality.SkyBackground.PERCENT_50)
      assertTrue(tpList.forall(_.getSiteQuality.conditions() == sq.conditions()))

      // Different targets
      assertEquals(List("target_D", "target_C", "target_B", "target_A"), tpList.map(_.getTarget.getTarget.getName))
    }

    def validateFolder(f: ISPTemplateFolder): Unit = {
      f.getTemplateGroups.asScala.toList match {
        case List(g) => validateGroup(g)
        case _       => fail("expecting a single template group")
      }

      f.getDataObject match {
        case tf: TemplateFolder =>
          assertEquals(1, tf.getBlueprints.size())
          val blueprint = tf.getBlueprints.get("blueprint-0")
          assertNotNull(blueprint)
          assertEquals(blueprint.instrumentType(), InstGmosSouth.SP_TYPE)
      }
    }

    validateFolder(p.getTemplateFolder)
  }
}
