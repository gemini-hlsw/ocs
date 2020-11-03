package edu.gemini.phase2.template.factory.impl

import edu.gemini.pot.sp._
import edu.gemini.pot.spdb.{DBLocalDatabase, IDBDatabaseService}
import edu.gemini.shared.util.immutable.{None => JNone}
import edu.gemini.spModel.io.SpImportService
import edu.gemini.spModel.rich.pot.spdb._
import edu.gemini.spModel.rich.pot.sp._

import java.net.URL
import java.util.logging.Logger
import java.io.InputStreamReader
import scala.util.{Failure, Success}
import java.security.Principal

import edu.gemini.spModel.obs.SPObservation

import scala.collection.JavaConverters._

object TemplateDb {
  val LOG = Logger.getLogger(getClass.getName)

  // Path from root to the xml directory
  val PATH = {
    val ss = classOf[TemplateDb].getName.split("\\.")
    ss.reverse.drop(2).reverse.mkString("/", "/", "/xml/")
  }

  // URLs of xml files, filtered by name
  def xmls(f: String => Boolean) = List(
    "F2_BP.xml",
    "GMOS_N_BP.xml",
    "GMOS_S_BP.xml",
    "GNIRS_BP.xml",
    "GPI_BP.xml",
    "GRACES_BP.xml",
    "GSAOI_BP.xml",
    "MICHELLE_BP.xml",
    "NICI_BP.xml",
    "NIFS_BP.xml",
    "NIRI_BP.xml",
    "PHOENIX_BP.xml",
    "TEXES_BP.xml",
    "TRECS_BP.xml",
    "VISITOR_BP.xml"
  ).filter(f).map(PATH + _).map(classOf[TemplateDb].getResource)

  def load(user: java.util.Set[Principal]):Either[String, TemplateDb] =
    loadWithFilter(user, _ => true)

  def loadWithFilter(user: java.util.Set[Principal], filter: String => Boolean):Either[String, TemplateDb] = {
    val odb = DBLocalDatabase.createTransient
    val res = xmls(filter).mapM { url =>
      LOG.fine(s"Loading $url")
      parse(odb)(url)
    }

    for {
      l <- res.right
      p <- l
      o <- p.getAllObservations.asScala
    } o.getDataObject.asInstanceOf[SPObservation].setSchedulingBlock(JNone.instance())
    res.right foreach { _.foreach(odb.put) }
    res.right map { _ => new TemplateDb(odb, user) }
  }

  //  private def loadTemplates(odb:IDBDatabaseService):Either[String, List[ISPProgram]] = {
  //    val empty:Either[String, List[ISPProgram]] = Right(Nil)
  //    (empty /: XMLS) {
  //      (e, url) =>
  //        e.right flatMap {
  //          progList =>
  //            parse(odb)(url).right map {
  //              _ :: progList
  //            }
  //        }
  //    }
  //  }

  //  private def loadTemplates(odb:IDBDatabaseService) = XMLS.mapM(parse(odb))


  //  private val XML_NAME_FILTER = new FilenameFilter() {
  //    def accept(dir:File, name:String) = name.endsWith(".xml")
  //  }
  //
  //  private def programXmls(dir:File):Either[String, List[File]] =
  //    try {
  //      Option(dir.listFiles(XML_NAME_FILTER)).toRight("There was an i/o problem reading the template program directory: " + dir.getPath).right map {
  //        _.toList
  //      }
  //    } catch {
  //      case ex:Exception => Left("Could not read the template program directory: " + dir.getPath)
  //    }

  private def parse(odb:IDBDatabaseService)(url:URL):Either[String, ISPProgram] = {

    def parseError(description: Throwable): String =
      "Problem reading template program: %s%s".format(url, description.getMessage)

    def closing[T](f: java.io.Reader => T): T = {
      val rdr = new InputStreamReader(url.openStream(), "UTF-8")
      try {
        f(rdr)
      } finally {
        rdr.close()
      }
    }

    closing { rdr =>
      new SpImportService(odb).importProgramXml(rdr) match {
        case Success(prog) => Right(prog)
        case Failure(ex)   => Left(parseError(ex))
      }
    }
  }

  //  private[factory] def instrumentGroupMap(odb:IDBDatabaseService):Map[SPComponentType, List[ISPGroup]] =
  //    (grpList(odb).groupBy(instrumentType) collect {
  //      case (Some(compType), lst) => (compType, lst)
  //    }).toMap
  //
  //  private def grpList(odb:IDBDatabaseService) = (odb query {
  //    progList =>
  //      progList flatMap {
  //        prog => prog.getGroups.asScala.toList.asInstanceOf[List[ISPGroup]]
  //      }
  //  }).right.getOrElse(Nil)
  //
  //  private def instrumentType(grp:ISPGroup):Option[SPComponentType] =
  //    grp.getObservations.asScala collectFirst {
  //      case obs if instrumentType(obs).isDefined => instrumentType(obs).get
  //    }
  //
  //  private def instrumentType(obs:ISPObservation):Option[SPComponentType] =
  //    obs.findObsComponent(SPInstComponentType.INST_BROAD_TYPE == _.getType.getBroadType) map {
  //      _.getType
  //    }

  // Map of program title to program
  private def progMap(odb:IDBDatabaseService, user: java.util.Set[Principal]):Map[String, ISPProgram] =
    (odb.query(user) { ps =>
      val tups = ps.map(p => p.spProgram.map(d => (d.getTitle, p)))
      for { Some(t) <- tups } yield t
    }).right.toOption.map(_.toMap).getOrElse(Map.empty)

}

class TemplateDb private (val odb:IDBDatabaseService, user: java.util.Set[Principal]) {

  //  val instrumentMap = TemplateDb.instrumentGroupMap(odb)
  //
  //  def group(inst:SPComponentType, id:String):Option[ISPGroup] =
  //    for {
  //      lst <- instrumentMap.get(inst)
  //      grp <- lst.find(Option(id) == _.libraryId)
  //    } yield copy(grp)

  val progMap = TemplateDb.progMap(odb, user)

//  private def copy(grp:ISPGroup):ISPGroup =
//    odb.getFactory.createGroupCopy(grp.getProgram, grp, false)

  def groups(programTitle:String, obsLibIds:Seq[String], noteTitles:Seq[String]):Either[String, ISPGroup] =
    for {
      p <- progMap.get(programTitle).toRight("Program '%s' not found.".format(programTitle)).right
      os <- p.obsByLibraryIds(obsLibIds).right
    } yield {
      // Create our group
      val targetGroup = odb.getFactory.createGroup(p, null)
      os.foreach {o =>
        val o1 = odb.getFactory.createObservationCopy(p, o, false)
        targetGroup.addObservation(o1)
      }

      targetGroup
    }
}
