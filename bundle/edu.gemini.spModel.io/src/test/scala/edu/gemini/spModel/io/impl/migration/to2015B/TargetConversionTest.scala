package edu.gemini.spModel.io.impl.migration.to2015B

import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.target.system.{DMSFormat, HMSFormat, CoordinateParam, NonSiderealTarget, HmsDegTarget}

import java.io.{StringReader, StringWriter, InputStreamReader}

import edu.gemini.pot.sp.{SPComponentType, ISPObservation, ISPTemplateFolder, ISPTemplateGroup, ISPProgram}
import edu.gemini.pot.spdb.{IDBDatabaseService, DBLocalDatabase}
import edu.gemini.spModel.io.impl.{PioSpXmlWriter, PioSpXmlParser}
import edu.gemini.spModel.template.{TemplateParameters, TemplateGroup}

import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConverters._

// a rudimentary test to make sure it doesn't blow up

class TargetConversionTest {
  private def withTestOdb(block: IDBDatabaseService => Unit): Unit = {
    val odb = DBLocalDatabase.createTransient()
    try {
      block(odb)
    } finally {
      odb.getDBAdmin.shutdown()
    }
  }

  private def withTestProgram(block: (IDBDatabaseService, ISPProgram) => Unit): Unit = withTestOdb { odb =>
    val parser = new PioSpXmlParser(odb.getFactory)
    parser.parseDocument(new InputStreamReader(getClass.getResourceAsStream("GS-2015B-T-1.xml"))) match {
      case p: ISPProgram => block(odb, p)
      case _             => fail("Expecting a science program")
    }
  }

  // Simple conversion from 2015A model.
  @Test
  def testTemplateConversion(): Unit =
    withTestProgram { (_,p) => validateProgram(p) }

  // Read 2015A model, write 2015B model, read back 2015B model, validate.
  @Test
  def roundTrip(): Unit =
    withTestProgram { (odb, p0) =>
      val sw = new StringWriter()
      new PioSpXmlWriter(sw).printDocument(p0)

      val parser = new PioSpXmlParser(odb.getFactory)
      parser.parseDocument(new StringReader(sw.toString)) match {
        case p1: ISPProgram => validateProgram(p1)
        case _              => fail("expecting a science program")
      }
    }

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
          assertTrue(tSidereal.getTarget.isInstanceOf[HmsDegTarget])
          assertEquals("Some Sidereal", tSidereal.getTarget.getName)

          assertTrue(tNonSidereal.getTarget.isInstanceOf[NonSiderealTarget])
          assertEquals("S123456", tNonSidereal.getTarget.getName)

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
    val rigel      = toc.getBase.getTarget.asInstanceOf[HmsDegTarget]

    val ra    = rigel.getRa.getAs(CoordinateParam.Units.DEGREES)
    val dec   = rigel.getDec.getAs(CoordinateParam.Units.DEGREES)
    val dra   = rigel.getPropMotionRA
    val ddec  = rigel.getPropMotionDec
    val epoch = rigel.getEpoch.getValue
    
    assertEquals("05:14:32.269", (new HMSFormat).format(ra))
    assertEquals("-08:12:05.86", (new DMSFormat).format(dec))
    assertEquals("1.30", f"$dra%.2f")
    assertEquals("0.50", f"$ddec%.2f")
    assertEquals(2000.0000, epoch, 0.0000001)
  }
}
