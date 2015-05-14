package edu.gemini.spModel.io.impl.migration.to2015B

import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.spModel.target.system.{NonSiderealTarget, HmsDegTarget}

import java.io.{StringReader, StringWriter, InputStreamReader}

import edu.gemini.pot.sp.{ISPTemplateFolder, ISPTemplateGroup, ISPProgram}
import edu.gemini.pot.spdb.{IDBDatabaseService, DBLocalDatabase}
import edu.gemini.spModel.io.impl.{PioSpXmlWriter, PioSpXmlParser}
import edu.gemini.spModel.template.{TemplateParameters, TemplateGroup}

import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConverters._

// a rudimentary test to make sure it doesn't blow up

class TemplateTargetConversionTest {
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
    parser.parseDocument(new InputStreamReader(getClass.getResourceAsStream("AsAcomet.xml"))) match {
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

    def validateFolder(f: ISPTemplateFolder): Unit =
      f.getTemplateGroups.asScala.toList match {
        case List(g1, g2) => validateGroup(g2)
        case _            => fail("expecting two template groups")
      }

    validateFolder(p.getTemplateFolder)
  }
}
